/*
 * HyperSearchResults.java - HyperSearch results
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * HyperSearch results window.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearchResults extends JPanel implements EBComponent
{
	public static final String NAME = "hypersearch-results";

	//{{{ HyperSearchResults constructor
	public HyperSearchResults(View view)
	{
		super(new BorderLayout());

		this.view = view;

		caption = new JLabel();
		updateCaption(0,0);
		add(BorderLayout.NORTH, caption);

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

		resultTree.addTreeSelectionListener(new TreeSelectionHandler());
		resultTree.addKeyListener(new KeyHandler());
		resultTree.addMouseListener(new MouseHandler());

		JScrollPane scrollPane = new JScrollPane(resultTree);
		Dimension dim = scrollPane.getPreferredSize();
		dim.width = 400;
		scrollPane.setPreferredSize(dim);
		add(BorderLayout.CENTER, scrollPane);
	} //}}}

	//{{{ requestDefaultFocus() method
	public boolean requestDefaultFocus()
	{
		resultTree.grabFocus();
		return true;
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			Buffer buffer = bmsg.getBuffer();
			if(bmsg.getWhat() == BufferUpdate.LOADED)
			{
				for(int i = resultTreeRoot.getChildCount() - 1; i >= 0; i--)
				{
					DefaultMutableTreeNode bufferNode = (DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i);

					for(int j = bufferNode.getChildCount() - 1;
						j >= 0; j--)
					{
						HyperSearchResult result = (HyperSearchResult)
							((DefaultMutableTreeNode)bufferNode
							.getChildAt(j)).getUserObject();
						if(buffer.getPath().equals(result.path))
							result.bufferOpened(buffer);
					}
				}
			}
			else if(bmsg.getWhat() == BufferUpdate.CLOSED)
			{
				for(int i = resultTreeRoot.getChildCount() - 1; i >= 0; i--)
				{
					DefaultMutableTreeNode bufferNode = (DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i);

					for(int j = bufferNode.getChildCount() - 1;
						j >= 0; j--)
					{
						HyperSearchResult result = (HyperSearchResult)
							((DefaultMutableTreeNode)bufferNode
							.getChildAt(j)).getUserObject();
						if(buffer.getPath().equals(result.path))
							result.bufferClosed();
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

	//{{{ searchStarted() method
	public void searchStarted()
	{
		caption.setText(jEdit.getProperty("hypersearch-results.searching"));
		resultTreeRoot.removeAllChildren();
		resultTreeModel.reload(resultTreeRoot);
	} //}}}

	//{{{ searchDone() method
	public void searchDone(int resultCount, int bufferCount)
	{
		updateCaption(resultCount,bufferCount);

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				for(int i = 0; i < resultTreeRoot.getChildCount(); i++)
				{
					resultTree.expandPath(new TreePath(
						((DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i))
						.getPath()));
				}
			}
		});
	} //}}}

	//{{{ Private members
	private View view;

	private JLabel caption;
	private JTree resultTree;
	private DefaultMutableTreeNode resultTreeRoot;
	private DefaultTreeModel resultTreeModel;

	//{{{ goToSelectedNode() method
	private void goToSelectedNode()
	{
		TreePath path = resultTree.getSelectionPath();
		if(path == null)
			return;

		Object value = ((DefaultMutableTreeNode)path
			.getLastPathComponent()).getUserObject();

		if(value instanceof String)
		{
			Buffer buffer = jEdit.openFile(view,(String)value);
			if(buffer == null)
				return;

			view.setBuffer(buffer);

			// fuck me dead
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					resultTree.requestFocus();
				}
			});
		}
		else
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
					view.setBuffer(buffer);
					JEditTextArea textArea = view.getTextArea();
					if(textArea.isMultipleSelectionEnabled())
						textArea.addToSelection(s);
					else
						textArea.setSelection(s);

					textArea.moveCaretPosition(end);
				}
			});
		}
	} //}}}

	//{{{ updateCaption() method
	private void updateCaption(int resultCount, int bufferCount)
	{
		Object[] pp = { new Integer(resultCount), new Integer(bufferCount) };
		caption.setText(jEdit.getProperty("hypersearch-results.caption",pp));
	} //}}}

	//}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
				goToSelectedNode();
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			TreePath path1 = resultTree.getPathForLocation(
				evt.getX(),evt.getY());
			if(path1 == null)
				return;

			resultTree.setSelectionPath(path1);
			goToSelectedNode();

			view.toFront();
			view.requestFocus();
			view.getTextArea().requestFocus();
		}
	} //}}}

	//{{{ TreeSelectionHandler class
	class TreeSelectionHandler implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent evt)
		{
			goToSelectedNode();
		}
	} //}}}

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
			if(node.getUserObject() instanceof String)
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
}
