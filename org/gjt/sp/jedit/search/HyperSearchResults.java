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
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.*;
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

		clear = new RolloverButton(GUIUtilities.loadIcon("Clear.png"));
		clear.setToolTipText(jEdit.getProperty(
			"hypersearch-results.clear.label"));
		clear.addActionListener(ah);
		toolBar.add(clear);

		multi = new RolloverButton();
		multi.setToolTipText(jEdit.getProperty(
			"hypersearch-results.multi.label"));
		multi.addActionListener(ah);
		toolBar.add(multi);

		add(BorderLayout.NORTH, toolBar);

		resultTreeRoot = new DefaultMutableTreeNode();
		resultTreeModel = new DefaultTreeModel(resultTreeRoot);
		resultTree = new JTree(resultTreeModel);
		resultTree.setToolTipText("");
		resultTree.setCellRenderer(new ResultCellRenderer());
		resultTree.setVisibleRowCount(16);
		resultTree.setRootVisible(false);
		resultTree.setShowsRootHandles(true);

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
		traverseNodes(resultTreeRoot, new TreeNodeCallbackAdapter() {
			public boolean processNode(DefaultMutableTreeNode node) {
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
	 * @since jEdit 4.1pre9
	 */
	public JTree getTree()
	{
		return resultTree;
	} //}}}

	//{{{ searchStarted() method
	public void searchStarted()
	{
		caption.setText(jEdit.getProperty("hypersearch-results.searching"));
	} //}}}

	//{{{ setSearchStatus() method
	public void setSearchStatus(String status)
	{
		caption.setText(status);
	} //}}}

	//{{{ searchFailed() method
	public void searchFailed()
	{
		caption.setText(jEdit.getProperty("hypersearch-results.no-results"));

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
	public void searchDone(final DefaultMutableTreeNode searchNode)
	{
		final int nodeCount = searchNode.getChildCount();
		if (nodeCount < 1)
		{
			searchFailed();
			return;
		}

		caption.setText(jEdit.getProperty("hypersearch-results.done"));

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

				TreePath lastNode = null;

				for(int i = 0; i < nodeCount; i++)
				{
					lastNode = new TreePath(
						((DefaultMutableTreeNode)
						searchNode.getChildAt(i))
						.getPath());

					resultTree.expandPath(lastNode);
				}

				resultTree.scrollPathToVisible(
					new TreePath(new Object[] {
					resultTreeRoot,searchNode }));
			}
		});
	} //}}}

	//{{{ Private members
	private View view;

	private JLabel caption;
	private JTree resultTree;
	private DefaultMutableTreeNode resultTreeRoot;
	private DefaultTreeModel resultTreeModel;

	private RolloverButton clear;
	private RolloverButton multi;
	private boolean multiStatus;

	//{{{ updateMultiStatus() method
	private void updateMultiStatus()
	{
		if(multiStatus)
			multi.setIcon(GUIUtilities.loadIcon("MultipleResults.png"));
		else
			multi.setIcon(GUIUtilities.loadIcon("SingleResult.png"));
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
			Buffer buffer = n.getBuffer();
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

			((HyperSearchNode)value).goTo(pane);
		}
	} //}}}

	//}}}

	//{{{ ActionHandler class
	public class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == clear)
			{
				resultTreeRoot.removeAllChildren();
				resultTreeModel.reload(resultTreeRoot);
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
		}
	} //}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
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

				view.toFront();
				view.requestFocus();
				view.getTextArea().requestFocus();
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
			}

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
			TreePath path = resultTree.getSelectionPath();
			if(path == null)
				return;

			MutableTreeNode value = (MutableTreeNode)path
				.getLastPathComponent();
			HyperSearchOperationNode.removeNodeFromCache(value);
			resultTreeModel.removeNodeFromParent(value);
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
			resultTreeRoot = new DefaultMutableTreeNode();
			resultTreeModel = new DefaultTreeModel(resultTreeRoot);
			resultTree.setModel(resultTreeModel);
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

	//{{{ TreeDisplayAction class
	class TreeDisplayAction extends AbstractAction
	{
		//{{{ actionPerformed method
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
	
	public void expandAllNodes(DefaultMutableTreeNode node)
	{
		
		traverseNodes(node, new TreeNodeCallbackAdapter()
		{
			public boolean processNode(DefaultMutableTreeNode node) {
				resultTree.expandPath(new TreePath(node.getPath()));
				return true;
			}
		});
	}
	
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

				String property = "hypersearch-results.result-caption";
				if (countNodes.bufferCount == 1)
				{
					property += countNodes.resultCount == 1 ? "1" : "2";
				}
				
				Object[] pp = { node.toString(), Integer.valueOf(countNodes.resultCount),
						Integer.valueOf(countNodes.bufferCount)};
				setText(jEdit.getProperty(property,pp));
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
				int count = node.getChildCount();
				HyperSearchFileNode hyperSearchFileNode = (HyperSearchFileNode) node.getUserObject();
				if(count == 1)
				{
					if (hyperSearchFileNode.getCount() == 1)
					{
						setText(jEdit.getProperty("hypersearch-results"
									  + ".file-caption1",new Object[] {
							hyperSearchFileNode
						}));
					}
					else
					{
						setText(jEdit.getProperty("hypersearch-results"
									  + ".file-caption2",new Object[] {
							hyperSearchFileNode,
							Integer.valueOf(hyperSearchFileNode.getCount())
						}));
					}
				}
				else
				{
					setText(jEdit.getProperty("hypersearch-results"
						+ ".file-caption",new Object[] {
						hyperSearchFileNode,
						Integer.valueOf(hyperSearchFileNode.getCount()),
						Integer.valueOf(count)
					}));
				}
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
		public boolean processNode(DefaultMutableTreeNode node) {
			return false;
		}
		
	} //}}}
}
