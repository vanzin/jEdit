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
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.*;
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

		multiStatus = jEdit.getBooleanProperty(
			"hypersearch-results.multi");
		updateMultiStatus();

		add(BorderLayout.NORTH, toolBar);

		resultTreeRoot = new DefaultMutableTreeNode();
		resultTreeModel = new DefaultTreeModel(resultTreeRoot);
		resultTree = new JTree(resultTreeModel);
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
		multi.setSelected(jEdit.getBooleanProperty("hypersearch-results.multi-toggle"));
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
		jEdit.setBooleanProperty("hypersearch-results.multi-toggle",multi.isSelected());
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			Buffer buffer = bmsg.getBuffer();
			Object what = bmsg.getWhat();
			if(what == BufferUpdate.LOADED ||
				what == BufferUpdate.CLOSED)
			{
				ResultVisitor visitor = null;
				if (what == BufferUpdate.LOADED)
				{
					visitor = new BufferLoadedVisitor();
				}
				else // BufferUpdate.CLOSED
				{
					visitor = new BufferClosedVisitor();
				}
				// impl note: since multiple searches now allowed,
				// extra level in hierarchy
				for(int i = resultTreeRoot.getChildCount() - 1; i >= 0; i--)
				{
					DefaultMutableTreeNode searchNode = (DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i);
					for(int j = searchNode.getChildCount() - 1;
						j >= 0; j--)
					{

						DefaultMutableTreeNode bufferNode = (DefaultMutableTreeNode)
							searchNode.getChildAt(j);

						for(int k = bufferNode.getChildCount() - 1;
							k >= 0; k--)
						{
							Object userObject =
								((DefaultMutableTreeNode)bufferNode
								.getChildAt(k)).getUserObject();
							HyperSearchResult result = (HyperSearchResult)
									userObject;

							if(buffer.getPath().equals(result.path))
								visitor.visit(buffer,result);
						}
					}
				}
			}
		}
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
				if(!multi.isSelected())
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
			multi.setIcon(GUIUtilities.loadIcon("SingleResult.png"));
		else
			multi.setIcon(GUIUtilities.loadIcon("MultipleResults.png"));
	} //}}}

	//{{{ goToSelectedNode() method
	private void goToSelectedNode()
	{
		TreePath path = resultTree.getSelectionPath();
		if(path == null)
			return;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path
			.getLastPathComponent();
		Object value = node.getUserObject();

		if(node.getParent() == resultTreeRoot)
		{
			// do nothing if clicked "foo (showing n occurrences in m files)"
		}
		else if(value instanceof String)
		{
			Buffer buffer = jEdit.openFile(view,(String)value);
			if(buffer == null)
				return;

			view.goToBuffer(buffer);

			// fuck me dead
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					resultTree.requestFocus();
				}
			});
		}
		else if (value instanceof HyperSearchResult)
		{
			final HyperSearchResult result = (HyperSearchResult)value;
			final Buffer buffer = result.getBuffer();

			if(buffer == null)
				return;

			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					int start = result.startPos.getOffset();
					int end = result.endPos.getOffset();
					Selection s = new Selection.Range(start,end);
					EditPane pane = view.goToBuffer(buffer);
					JEditTextArea textArea = pane.getTextArea();
					if(textArea.isMultipleSelectionEnabled())
						textArea.addToSelection(s);
					else
						textArea.setSelection(s);

					textArea.moveCaretPosition(end);
				}
			});
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
				goToSelectedNode();

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
				goToSelectedNode();

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
			if (popupMenu == null)
			{
				popupMenu = new JPopupMenu();
				popupMenu.add(new RemoveTreeNodeAction());
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
		public RemoveTreeNodeAction()
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
			resultTreeModel.removeNodeFromParent(value);
		}
	}//}}}

	//{{{ RemoveAllTreeNodesAction class
	class RemoveAllTreeNodesAction extends AbstractAction
	{
		public RemoveAllTreeNodesAction()
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

	//{{{ ResultCellRenderer class
	class ResultCellRenderer extends DefaultTreeCellRenderer
	{
		Font plainFont, boldFont;

		//{{{ ResultCellRenderer constructor
		ResultCellRenderer()
		{
			plainFont = UIManager.getFont("Tree.font");
			boldFont = new Font(plainFont.getName(),Font.BOLD,
				plainFont.getSize());
		} //}}}

		//{{{ getTreeCellRendererComponent() method
		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			Component comp = super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,hasFocus);
			setIcon(null);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

			if (node.getParent() == resultTreeRoot)
			{
				ResultCellRenderer.this.setFont(boldFont);
				int bufferCount = node.getChildCount();
				int resultCount = 0;
				for (int i = 0; i < bufferCount; i++)
				{
					resultCount += node.getChildAt(i).getChildCount();
				}
				Object[] pp = { node.toString(), new Integer(resultCount), new Integer(bufferCount) };
				setText(jEdit.getProperty("hypersearch-results.result-caption",pp));
			}
			else if(node.getUserObject() instanceof String)
			{
				// file name
				ResultCellRenderer.this.setFont(boldFont);
				int count = node.getChildCount();
				if(count == 1)
				{
					setText(jEdit.getProperty("hypersearch-results"
						+ ".file-caption1",new Object[] {
						node.getUserObject()
						}));
				}
				else
				{
					setText(jEdit.getProperty("hypersearch-results"
						+ ".file-caption",new Object[] {
						node.getUserObject(),
						new Integer(count)
						}));
				}
			}
			else
			{
				ResultCellRenderer.this.setFont(plainFont);
			}

			return this;
		} //}}}
	} //}}}

	// these are used to eliminate code duplication. i don't normally use
	// the visitor or "template method" pattern, but this code was contributed
	// by Peter Cox and i don't feel like changing it around too much.

	//{{{ ResultVisitor interface
	interface ResultVisitor
	{
		public void visit(Buffer buffer, HyperSearchResult result);
	} //}}}

	//{{{ BufferLoadedVisitor class
	class BufferLoadedVisitor implements ResultVisitor
	{
		public void visit(Buffer buffer, HyperSearchResult result)
		{
			result.bufferOpened(buffer);
		}
	} //}}}

	//{{{ BufferClosedVisitor class
	class BufferClosedVisitor implements ResultVisitor
	{
		public void visit(Buffer buffer, HyperSearchResult result)
		{
			result.bufferClosed();
		}
	} //}}}
}
