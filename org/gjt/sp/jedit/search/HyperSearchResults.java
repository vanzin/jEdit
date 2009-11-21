/*
 * HyperSearchResults.java - HyperSearch results
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 2002 Peter Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.search;

//{{{ Imports
import javax.swing.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.gui.StyleEditor;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.search.SearchMatcher.Match;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.SyntaxUtilities;
//}}}

/**
 * HyperSearch results window.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearchResults extends JPanel implements EBComponent,
	DefaultFocusComponent
{
	public static final String NAME = "hypersearch-results";
	public static final String HIGHLIGHT_PROP = "hypersearch.results.highlight";

	//{{{ HyperSearchResults constructor
	public HyperSearchResults(View view)
	{
		super(new BorderLayout());

		this.view = view;

		caption = new JLabel();

		Box toolBar = new Box(BoxLayout.X_AXIS);
		toolBar.add(caption);
		toolBar.add(Box.createGlue());

		ActionHandler ah = new ActionHandler();

		highlight = new RolloverButton();
		highlight.setToolTipText(jEdit.getProperty(
			"hypersearch-results.highlight.label"));
		highlight.addActionListener(ah);
		toolBar.add(highlight);
		
		clear = new RolloverButton(GUIUtilities.loadIcon(
			jEdit.getProperty("hypersearch-results.clear.icon")));
		clear.setToolTipText(jEdit.getProperty(
			"hypersearch-results.clear.label"));
		clear.addActionListener(ah);
		toolBar.add(clear);

		multi = new RolloverButton();
		multi.setToolTipText(jEdit.getProperty(
			"hypersearch-results.multi.label"));
		multi.addActionListener(ah);
		toolBar.add(multi);

		stop = new RolloverButton(GUIUtilities.loadIcon(
			jEdit.getProperty("hypersearch-results.stop.icon")));
		stop.setToolTipText(jEdit.getProperty(
			"hypersearch-results.stop.label"));
		stop.addActionListener(ah);
		toolBar.add(stop);
		stop.setEnabled(false);

		add(BorderLayout.NORTH, toolBar);

		resultTreeRoot = new DefaultMutableTreeNode();
		resultTreeModel = new DefaultTreeModel(resultTreeRoot);
		resultTree = new HighlightingTree(resultTreeModel);
		resultTree.setToolTipText(null);
		resultTree.setCellRenderer(new ResultCellRenderer());
		resultTree.setVisibleRowCount(16);
		resultTree.setRootVisible(false);
		resultTree.setShowsRootHandles(true);
		//the ESCAPE keystroke is assigned to hideTip action by swing
		//it breaks the action usually assigned to close-docking-area by jEdit,
		//so we remove this keystroke binding bug #1955140
		KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		resultTree.getInputMap().remove(keyStroke);
		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			resultTree.putClientProperty("JTree.lineStyle", "Angled");

		resultTree.setEditable(false);

		resultTree.addKeyListener(new KeyHandler());
		resultTree.addMouseListener(new MouseHandler());

		JScrollPane scrollPane = new JScrollPane(resultTree);
		Dimension dim = scrollPane.getPreferredSize();
		dim.width = 400;
		scrollPane.setPreferredSize(dim);
		add(BorderLayout.CENTER, scrollPane);
		resultTree.setTransferHandler(new ResultTreeTransferHandler());
	} //}}}

	//{{{ focusOnDefaultComponent() method
	public void focusOnDefaultComponent()
	{
		resultTree.requestFocus();
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
		multiStatus = jEdit.getBooleanProperty(
			"hypersearch-results.multi");
		updateHighlightStatus();
		updateMultiStatus();
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
		jEdit.setBooleanProperty("hypersearch-results.multi",multiStatus);
	} //}}}

	//{{{ visitBuffers() method
	private void visitBuffers(final ResultVisitor visitor, final Buffer buffer)
	{
		// impl note: since multi-level hierarchies now allowed,
		// use traverseNodes to process HyperSearchResult nodes
		traverseNodes(resultTreeRoot, new TreeNodeCallbackAdapter()
		{
			public boolean processNode(DefaultMutableTreeNode node)
			{
				Object userObject = node.getUserObject();
				if (!(userObject instanceof HyperSearchResult))
					return true;
				HyperSearchResult result = (HyperSearchResult) userObject;
				if (result.pathEquals(buffer.getSymlinkPath()))
					visitor.visit(buffer, result);
				return true;
			}
		});
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			Buffer buffer = bmsg.getBuffer();
			Object what = bmsg.getWhat();
			if(what == BufferUpdate.LOADED)
				visitBuffers(new BufferLoadedVisitor(),buffer);
			else if(what == BufferUpdate.CLOSED)
				visitBuffers(new BufferClosedVisitor(),buffer);
		}
	} //}}}

	//{{{ traverseNodes() method
	public static boolean traverseNodes(DefaultMutableTreeNode node, 
			HyperSearchTreeNodeCallback callbackInterface)
	{
		if (!callbackInterface.processNode(node))
			return false;
		for (Enumeration e = node.children(); e.hasMoreElements();)
		{
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)e.nextElement();
			if (!traverseNodes(childNode, callbackInterface))
				return false;
		}
		return true;
	} //}}}

	//{{{ getTreeModel() method
	public DefaultTreeModel getTreeModel()
	{
		return resultTreeModel;
	} //}}}

	//{{{ getTree() method
	/**
	 * Returns the result tree.
	 *
	 * @return the result tree
	 * @since jEdit 4.1pre9
	 */
	public JTree getTree()
	{
		return resultTree;
	} //}}}

	//{{{ searchStarted() method
	public void searchStarted()
	{
		stop.setEnabled(true);
		caption.setText(jEdit.getProperty("hypersearch-results.searching",
				new String[] { SearchAndReplace.getSearchString() }));
	} //}}}

	//{{{ setSearchStatus() method
	public void setSearchStatus(String status)
	{
		caption.setText(status);
	} //}}}

	//{{{ searchFailed() method
	public void searchFailed()
	{
		caption.setText(jEdit.getProperty("hypersearch-results.no-results",
				new String[] { SearchAndReplace.getSearchString() }));

		// collapse all nodes, as suggested on user mailing list...
		for(int i = 0; i < resultTreeRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				resultTreeRoot.getChildAt(i);
			resultTree.collapsePath(new TreePath(new Object[] {
				resultTreeRoot, node }));
		}
	} //}}}

	//{{{ searchDone() method
	/**
	 * @param searchNode the result node
	 * @param selectNode the node that must be selected, or null
	 * @since jEdit 4.3pre12
	 */
	public void searchDone(final DefaultMutableTreeNode searchNode, final DefaultMutableTreeNode selectNode)
	{
		stop.setEnabled(false);
		final int nodeCount = searchNode.getChildCount();
		if (nodeCount < 1)
		{
			searchFailed();
			return;
		}

		caption.setText(jEdit.getProperty("hypersearch-results.done",
				new String [] { SearchAndReplace.getSearchString() }));

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(!multiStatus)
				{
					for(int i = 0; i < resultTreeRoot.getChildCount(); i++)
					{
						resultTreeRoot.remove(0);
					}
				}

				resultTreeRoot.add(searchNode);
				resultTreeModel.reload(resultTreeRoot);


				for (int i = 0; i < nodeCount; i++)
				{
					TreePath lastNode = new TreePath(
						((DefaultMutableTreeNode)
						searchNode.getChildAt(i))
						.getPath());

					resultTree.expandPath(lastNode);
				}
				TreePath treePath;
				if (selectNode == null)
				{
					treePath = new TreePath(new Object[]{
						resultTreeRoot, searchNode});
				}
				else
				{
					treePath = new TreePath(selectNode.getPath());
				}
				resultTree.setSelectionPath(treePath);
				resultTree.scrollPathToVisible(
					treePath);
			}
		});
	} //}}}

	//{{{ searchDone() method
	public void searchDone(final DefaultMutableTreeNode searchNode)
	{
		searchDone(searchNode, null);
	} //}}}

	//{{{ Private members
	private View view;

	private JLabel caption;
	private final JTree resultTree;
	private DefaultMutableTreeNode resultTreeRoot;
	private DefaultTreeModel resultTreeModel;

	private RolloverButton highlight;
	private RolloverButton clear;
	private RolloverButton multi;
	private RolloverButton stop;
	private boolean multiStatus;

	//{{{ updateHighlightStatus() method
	private void updateHighlightStatus()
	{
		String prop = jEdit.getProperty(HIGHLIGHT_PROP);
		if (prop != null && prop.length() > 0)
			highlight.setIcon(GUIUtilities.loadIcon(jEdit.getProperty("hypersearch-results.match.highlight.icon")));
		else
			highlight.setIcon(GUIUtilities.loadIcon(jEdit.getProperty("hypersearch-results.match.normal.icon")));
		resultTree.repaint();
	} //}}}
	
	//{{{ updateMultiStatus() method
	private void updateMultiStatus()
	{
		if(multiStatus)
			multi.setIcon(GUIUtilities.loadIcon(jEdit.getProperty("hypersearch-results.multi.multiple.icon")));
		else
			multi.setIcon(GUIUtilities.loadIcon(jEdit.getProperty("hypersearch-results.multi.single.icon")));
	} //}}}
	
	//{{{ goToSelectedNode() method
	public static final int M_OPEN = 0;
	public static final int M_OPEN_NEW_VIEW = 1;
	public static final int M_OPEN_NEW_PLAIN_VIEW = 2;
	public static final int M_OPEN_NEW_SPLIT = 3;

	private void goToSelectedNode(int mode)
	{
		TreePath path = resultTree.getSelectionPath();
		if(path == null)
			return;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path
			.getLastPathComponent();
		Object value = node.getUserObject();

		// do nothing if clicked "foo (showing n occurrences in m files)"
		if(node.getParent() != resultTreeRoot && value instanceof HyperSearchNode)
		{
			HyperSearchNode n = (HyperSearchNode)value;
			Buffer buffer = n.getBuffer(view);
			if(buffer == null)
				return;

			EditPane pane;

			switch(mode)
			{
			case M_OPEN:
				pane = view.goToBuffer(buffer);
				break;
			case M_OPEN_NEW_VIEW:
				pane = jEdit.newView(view,buffer,false).getEditPane();
				break;
			case M_OPEN_NEW_PLAIN_VIEW:
				pane = jEdit.newView(view,buffer,true).getEditPane();
				break;
			case M_OPEN_NEW_SPLIT:
				pane = view.splitHorizontally();
				break;
			default:
				throw new IllegalArgumentException("Bad mode: " + mode);
			}

			n.goTo(pane);
		}
	} //}}}

	//{{{ removeSelectedNode() method
	private void removeSelectedNode()
	{
		TreePath path = resultTree.getSelectionPath();
		if(path == null)
			return;

		MutableTreeNode value = (MutableTreeNode)path
			.getLastPathComponent();

		if(path.getPathCount() > 1)
		{
			// Adjust selection so that repeating some removals
			// behave naturally.
			TreePath parentPath = path.getParentPath();
			MutableTreeNode parent = (MutableTreeNode)parentPath
				.getLastPathComponent();
			int removingIndex = parent.getIndex(value);
			int nextIndex = removingIndex + 1;
			if(nextIndex < parent.getChildCount())
			{
				TreeNode next = parent.getChildAt(nextIndex);
				resultTree.setSelectionPath(
					parentPath.pathByAddingChild(next));
			}
			else
			{
				resultTree.setSelectionPath(parentPath);
			}

			resultTreeModel.removeNodeFromParent(value);
		}

		HyperSearchOperationNode.removeNodeFromCache(value);
		if (resultTreeRoot.getChildCount() == 0)
		{
			hideDockable();
		}
	} //}}}

	//{{{ removeAllNodes() method
	private void removeAllNodes()
	{
		resultTreeRoot.removeAllChildren();
		resultTreeModel.reload(resultTreeRoot);
		setSearchStatus(null);
		hideDockable();
	} //}}}

	//{{{ hideDockable() method
	private void hideDockable()
	{
		view.getDockableWindowManager().hideDockableWindow(NAME);
	} //}}}

	//{{{ parseHighlightStyle()
	SyntaxStyle parseHighlightStyle(String style)
	{
		Font f = (resultTree != null) ? resultTree.getFont() :
			UIManager.getFont("Tree.font");
		SyntaxStyle s;
		try
		{
			s = SyntaxUtilities.parseStyle(style, f.getFamily(), f.getSize(), true, null);
		}
		catch (Exception e)
		{
			style = "color:#000000";
			s = SyntaxUtilities.parseStyle(style, f.getFamily(), f.getSize(), true);
		}
		return s;
	}
	//}}}
	
	//}}}

	//{{{ ActionHandler class
	public class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == highlight)
			{
				String prop = jEdit.getProperty(HIGHLIGHT_PROP);
				SyntaxStyle style = new StyleEditor(jEdit.getActiveView(), parseHighlightStyle(prop),
					"hypersearch").getStyle();
				if (style != null)
					jEdit.setProperty(HIGHLIGHT_PROP, GUIUtilities.getStyleString(style));
				updateHighlightStatus();
			}
			else if(source == clear)
			{
				removeAllNodes();
			}
			else if(source == multi)
			{
				multiStatus = !multiStatus;
				updateMultiStatus();

				if(!multiStatus)
				{
					for(int i = resultTreeRoot.getChildCount() - 2; i >= 0; i--)
					{
						resultTreeModel.removeNodeFromParent(
							(MutableTreeNode)resultTreeRoot
							.getChildAt(i));
					}
				}
			}
			else if(source == stop)
			{
				jEdit.setTemporaryProperty("hyperSearch-stopButton", "true");
			}
		}
	} //}}}

	//{{{ HighlightingTree class
	class HighlightingTree extends JTree
	{
		private String prop;
		private String styleTag;
		
		public HighlightingTree(DefaultTreeModel model)
		{
			super(model);
			prop = jEdit.getProperty(HIGHLIGHT_PROP);
			if (prop != null && prop.length() > 0)
				styleTag = style2html(prop);
		}

		@Override
		public String convertValueToText(Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			String s = super.convertValueToText(value, selected, expanded, leaf,
				row, hasFocus);
			String newProp = jEdit.getProperty(HIGHLIGHT_PROP);
			if (newProp == null || newProp.length() == 0)
				return s;
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			while ((node != null) &&
				   (! (node.getUserObject() instanceof HyperSearchOperationNode)))
			{
				node = (DefaultMutableTreeNode) node.getParent();
			}
			if (node == null)
				return s;
			if (! newProp.equals(prop))
			{
				prop = newProp;
				styleTag = style2html(prop);
			}
			SearchMatcher matcher =
				((HyperSearchOperationNode) node.getUserObject()).getSearchMatcher();
			StringBuilder sb = new StringBuilder("<html><style>.highlight {");
			sb.append(styleTag);
			sb.append("}</style><body>");
			int lineTextIndex = s.indexOf(": ");
			if (lineTextIndex > 0)
			{
				lineTextIndex += 2;
				appendString2html(sb, s.substring(0, lineTextIndex));
				s = s.substring(lineTextIndex);
			}
			int i = 0;
			Match m;
			while ((m = matcher.nextMatch(s.substring(i), true, true, true, false)) != null)
			{
				appendString2html(sb, s.substring(i, i + m.start));
				sb.append("<span class=\"highlight\">");
				appendString2html(sb, s.substring(i + m.start, i + m.end));
				sb.append("</span>");
				i += m.end;
			}
			appendString2html(sb, s.substring(i));
			sb.append("</body></html>");
			return sb.toString();
		}

		private String color2html(Color c)
		{
			StringBuilder cs = new StringBuilder("rgb(");
			cs.append(c.getRed());
			cs.append(",");
			cs.append(c.getGreen());
			cs.append(",");
			cs.append(c.getBlue());
			cs.append(");");
			return cs.toString();
		}
		
		private String style2html(String prop)
		{
			StringBuilder tag = new StringBuilder();
			SyntaxStyle style = parseHighlightStyle(prop);
			Font f = style.getFont();
			Color c = style.getForegroundColor();
			if (c != null)
				tag.append("color:").append(color2html(c));
			c = style.getBackgroundColor();
			if (c != null)
				tag.append("background:").append(color2html(c));
			if (f.isBold())
				tag.append("font-weight:bold;");
			if (f.isItalic())
				tag.append("font-style: italic;");
			return tag.toString();
		}
		
		private void appendString2html(StringBuilder sb, String s)
		{
			for (int i = 0; i < s.length(); i++)
			{
				char c = s.charAt(i);
				String r;
				switch (c)
				{
				case '"':
					r = "&quot;";
					break;
				// case '\'': r = "&apos;"; break;
				case '&':
					r = "&amp;";
					break;
				case '<':
					r = "&lt;";
					break;
				case '>':
					r = "&gt;";
					break;
				default:
					r = String.valueOf(c);
					break;
				}
				sb.append(r);
			}
		}
	} //}}}
	
	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_SPACE:
				goToSelectedNode(M_OPEN);

				// fuck me dead
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						resultTree.requestFocus();
					}
				});

				evt.consume();
				break;
			case KeyEvent.VK_ENTER:
				goToSelectedNode(M_OPEN);
				evt.consume();
				break;
			case KeyEvent.VK_DELETE:
				removeSelectedNode();
				evt.consume();
				break;
			default:
				break;
			}
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
			if(evt.isConsumed())
				return;

			TreePath path1 = resultTree.getPathForLocation(
				evt.getX(),evt.getY());
			if(path1 == null)
				return;

			resultTree.setSelectionPath(path1);
			if (GUIUtilities.isPopupTrigger(evt))
				showPopupMenu(evt);
			else
			{
				goToSelectedNode(M_OPEN);
			}
		} //}}}

		//{{{ Private members
		private JPopupMenu popupMenu;

		//{{{ showPopupMenu method
		private void showPopupMenu(MouseEvent evt)
		{
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			
			popupMenu = new JPopupMenu();
			Object userObj = node.getUserObject();
			if (userObj instanceof HyperSearchFileNode
					|| userObj instanceof HyperSearchResult)
			{
				popupMenu.add(new GoToNodeAction(
					"hypersearch-results.open",
					M_OPEN));
				popupMenu.add(new GoToNodeAction(
					"hypersearch-results.open-view",
					M_OPEN_NEW_VIEW));
				popupMenu.add(new GoToNodeAction(
					"hypersearch-results.open-plain-view",
					M_OPEN_NEW_PLAIN_VIEW));
				popupMenu.add(new GoToNodeAction(
					"hypersearch-results.open-split",
					M_OPEN_NEW_SPLIT));
			}
			if (!(userObj instanceof HyperSearchFolderNode))
				popupMenu.add(new RemoveTreeNodeAction());
			popupMenu.add(new ExpandChildTreeNodesAction());
			if (userObj instanceof HyperSearchFolderNode
					|| userObj instanceof HyperSearchOperationNode)
			{
				popupMenu.add(new CollapseChildTreeNodesAction());
				if (userObj instanceof HyperSearchFolderNode)
					popupMenu.add(new NewSearchAction());
			}
			if (userObj instanceof HyperSearchOperationNode)
			{
				popupMenu.add(new JPopupMenu.Separator());
				HyperSearchOperationNode resultNode = (HyperSearchOperationNode)userObj;
				JCheckBoxMenuItem chkItem = 
					new JCheckBoxMenuItem(jEdit.getProperty("hypersearch-results.tree-view"),
							resultNode.isTreeViewDisplayed());
				chkItem.addActionListener(new TreeDisplayAction());
				popupMenu.add(chkItem);

				popupMenu.add(new RedoSearchAction((HyperSearchOperationNode)userObj));
			}
			popupMenu.add(new CopyToClipboardAction());

			GUIUtilities.showPopupMenu(popupMenu,evt.getComponent(),
				evt.getX(),evt.getY());
			evt.consume();
		} //}}}

		//}}}
	} //}}}

	//{{{ RemoveTreeNodeAction class
	class RemoveTreeNodeAction extends AbstractAction
	{
		RemoveTreeNodeAction()
		{
			super(jEdit.getProperty("hypersearch-results.remove-node"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			removeSelectedNode();
		}
	}//}}}

	//{{{ RemoveAllTreeNodesAction class
	class RemoveAllTreeNodesAction extends AbstractAction
	{
		RemoveAllTreeNodesAction()
		{
			super(jEdit.getProperty("hypersearch-results.remove-all-nodes"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			removeAllNodes();
		}
	}//}}}
	
	//{{{ NewSearchAction class
	class NewSearchAction extends AbstractAction
	{
		NewSearchAction()
		{
			super(jEdit.getProperty("hypersearch-results.new-search"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			HyperSearchFolderNode nodeObj = (HyperSearchFolderNode)operNode.getUserObject();
			
			String glob = "*";
			SearchFileSet dirList = SearchAndReplace.getSearchFileSet();
			if (dirList instanceof DirectoryListSet)
				glob = ((DirectoryListSet)dirList).getFileFilter();
			SearchAndReplace.setSearchFileSet(new DirectoryListSet(
					nodeObj.getNodeFile().getAbsolutePath(),glob,true));
			SearchDialog.showSearchDialog(view,null,SearchDialog.DIRECTORY);
		}
	}//}}}

	//{{{ ExpandChildTreeNodesAction class
	class ExpandChildTreeNodesAction extends AbstractAction
	{
		ExpandChildTreeNodesAction()
		{
			super(jEdit.getProperty("hypersearch-results.expand-child-nodes"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			expandAllNodes(operNode);
		}
	}//}}}

	//{{{ CopyToClipboardAction class
	class CopyToClipboardAction extends AbstractAction
	{
		CopyToClipboardAction()
		{
			super(jEdit.getProperty("hypersearch-results.copy-to-clipboard"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			ToStringNodes toStringNodes = new ToStringNodes();
			traverseNodes(operNode, toStringNodes);
			StringSelection selection = new StringSelection(
				toStringNodes.nodesString.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, null);
		}
	}//}}}

	//{{{ ToStringNodes class
	class ToStringNodes implements HyperSearchTreeNodeCallback
	{
		StringBuilder nodesString = new StringBuilder();

		public boolean processNode(DefaultMutableTreeNode node)
		{
			Object userObject = node.getUserObject();
			if (userObject instanceof HyperSearchFileNode)
				nodesString.append(((HyperSearchFileNode)userObject).path);
			else if (userObject instanceof HyperSearchResult)
			{
				HyperSearchResult hsr = (HyperSearchResult)userObject;
				// Copy the ORIGINAL line from the buffer!
				nodesString.append(hsr.buffer == null ? hsr.toString() : hsr.buffer.getLineText(hsr.line));
			}
			else
				nodesString.append(userObject.toString());
			nodesString.append(System.getProperty("line.separator"));
			return true;
		}
	}//}}}

	//{{{ CollapseChildTreeNodesAction class
	class CollapseChildTreeNodesAction extends AbstractAction
	{
		CollapseChildTreeNodesAction()
		{
			super(jEdit.getProperty("hypersearch-results.collapse-child-nodes"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			for (Enumeration e = operNode.children(); e.hasMoreElements();)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
				resultTree.collapsePath(new TreePath(node.getPath()));
			}
			resultTree.scrollPathToVisible(
					new TreePath(operNode.getPath()));
		}
	}//}}}

	//{{{ RedoSearchAction class
	class RedoSearchAction extends AbstractAction
	{
		private HyperSearchOperationNode hyperSearchOperationNode;
		public RedoSearchAction(HyperSearchOperationNode hyperSearchOperationNode)
		{
			super(jEdit.getProperty("hypersearch-results.redo"));
			this.hyperSearchOperationNode = hyperSearchOperationNode;
		}

		/**
		 * Invoked when an action occurs.
		 */
		public void actionPerformed(ActionEvent e)
		{
			SearchAndReplace.setSearchString(hyperSearchOperationNode.getSearchString());
			SearchAndReplace.setSearchMatcher(hyperSearchOperationNode.getSearchMatcher());
			removeSelectedNode();
			SearchAndReplace.hyperSearch(view, false);
		}
	} //}}}

	//{{{ TreeDisplayAction class
	class TreeDisplayAction extends AbstractAction
	{
		public void actionPerformed(ActionEvent evt)
		{
			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) evt.getSource();
			boolean curState = menuItem.isSelected();
			
			TreePath path = resultTree.getSelectionPath();
			DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			
			HyperSearchOperationNode operNodeObj = (HyperSearchOperationNode)operNode.getUserObject();
			if (curState)
				operNodeObj.cacheResultNodes(operNode);
			operNode.removeAllChildren();
			Exception excp = null;
			if (curState)
			{
				try
				{
					operNodeObj.insertTreeNodes(resultTree, operNode);
				} 
				catch (Exception ex)
				{
					operNodeObj.restoreFlatNodes(resultTree, operNode);
					menuItem.setSelected(false);
					excp = ex;
				}
				finally
				{
					((DefaultTreeModel)resultTree.getModel()).nodeStructureChanged(operNode);
					expandAllNodes(operNode);
					resultTree.scrollPathToVisible(
							new TreePath(operNode.getPath()));					
				}
				if (excp != null)
					throw new RuntimeException(excp);
			}
			else
				operNodeObj.restoreFlatNodes(resultTree, operNode);
			
			operNodeObj.setTreeViewDisplayed(menuItem.isSelected());
		}
	}//}}}
	
	//{{{ expandAllNodes() method
	public void expandAllNodes(DefaultMutableTreeNode node)
	{
		
		traverseNodes(node, new TreeNodeCallbackAdapter()
		{
			public boolean processNode(DefaultMutableTreeNode node)
			{
				resultTree.expandPath(new TreePath(node.getPath()));
				return true;
			}
		});
	} //}}}
	
	//{{{ GoToNodeAction class
	class GoToNodeAction extends AbstractAction
	{
		private int mode;

		GoToNodeAction(String labelProp, int mode)
		{
			super(jEdit.getProperty(labelProp));
			this.mode = mode;
		}

		public void actionPerformed(ActionEvent evt)
		{
			goToSelectedNode(mode);
		}
	}//}}}

	//{{{ ResultCellRenderer class
	class ResultCellRenderer extends DefaultTreeCellRenderer
	{
		Font plainFont, boldFont;
		
		//{{{ ResultCellRenderer constructor
		ResultCellRenderer()
		{
			plainFont = UIManager.getFont("Tree.font");
			if(plainFont == null)
				plainFont = jEdit.getFontProperty("metal.secondary.font");
			boldFont = new Font(plainFont.getName(),Font.BOLD,
				plainFont.getSize());
		} //}}}

		//{{{ getTreeCellRendererComponent() method
		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,hasFocus);
			setIcon(null);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

			if (node.getUserObject() instanceof HyperSearchOperationNode)
			{
				setFont(boldFont);

				CountNodes countNodes = new CountNodes();
				traverseNodes(node, countNodes);

				setText(jEdit.getProperty("hypersearch-results.result-caption",
							  new Object[] {
							  node.toString(),
							  Integer.valueOf(countNodes.resultCount),
							  Integer.valueOf(countNodes.bufferCount)
				}));
			}
			else if(node.getUserObject() instanceof HyperSearchFolderNode)
			{
				setFont(plainFont);
				setText(node.toString() + " (" + node.getChildCount() + " files/folders)");
			}
			else if(node.getUserObject() instanceof HyperSearchFileNode)
			{
				// file name
				setFont(boldFont);
				HyperSearchFileNode hyperSearchFileNode = (HyperSearchFileNode) node.getUserObject();
				setText(jEdit.getProperty("hypersearch-results.file-caption",
							  new Object[] {
							  hyperSearchFileNode,
							  Integer.valueOf(hyperSearchFileNode.getCount()),
							  Integer.valueOf(node.getChildCount())
				}));
			}
			else
			{
				setFont(plainFont);
			}

			return this;
		} //}}}

		//{{{ CountNodes class
		class CountNodes implements HyperSearchTreeNodeCallback
		{
			int bufferCount;
			int resultCount;
			public boolean processNode(DefaultMutableTreeNode node)
			{
				Object userObject = node.getUserObject();
				if (userObject instanceof HyperSearchFileNode)
				{
					resultCount += ((HyperSearchFileNode)userObject).getCount();
					bufferCount++;
				}
				return true;
			}
		}//}}}
	} //}}}

	//{{{
	class ResultTreeTransferHandler extends TransferHandler
	{
		@Override
		public void exportToClipboard(JComponent comp, Clipboard clip,
				int action) throws IllegalStateException
		{
			TreePath [] paths = resultTree.getSelectionPaths();
			ToStringNodes toStringNodes = new ToStringNodes();
			for (TreePath path: paths)
			{
				DefaultMutableTreeNode operNode = (DefaultMutableTreeNode)
					path.getLastPathComponent();
				toStringNodes.processNode(operNode);
			}
			StringSelection selection = new StringSelection(
				toStringNodes.nodesString.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, null);
		}
	} //}}}
	
	// these are used to eliminate code duplication. i don't normally use
	// the visitor or "template method" pattern, but this code was contributed
	// by Peter Cox and i don't feel like changing it around too much.

	//{{{ ResultVisitor interface
	interface ResultVisitor
	{
		void visit(Buffer buffer, HyperSearchResult result);
	} //}}}

	//{{{ BufferLoadedVisitor class
	static class BufferLoadedVisitor implements ResultVisitor
	{
		public void visit(Buffer buffer, HyperSearchResult result)
		{
			result.bufferOpened(buffer);
		}
	} //}}}

	//{{{ BufferClosedVisitor class
	static class BufferClosedVisitor implements ResultVisitor
	{
		public void visit(Buffer buffer, HyperSearchResult result)
		{
			result.bufferClosed();
		}
	} //}}}
	
	//{{{ TreeNodeCallbackAdapter class
	static class TreeNodeCallbackAdapter implements HyperSearchTreeNodeCallback
	{
		public boolean processNode(DefaultMutableTreeNode node)
		{
			return false;
		}
		
	} //}}}
}
