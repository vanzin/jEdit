/*
 * HyperSearchResults.java - HyperSearch results
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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

/**
 * HyperSearch results window.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearchResults extends JPanel implements DockableWindow,
	EBComponent
{
	public static final String NAME = "hypersearch-results";

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
		resultTree.putClientProperty("JTree.lineStyle", "Angled");
		resultTree.setEditable(false);

		resultTree.addMouseListener(new MouseHandler());

		JScrollPane scrollPane = new JScrollPane(resultTree);
		Dimension dim = scrollPane.getPreferredSize();
		dim.width = 400;
		scrollPane.setPreferredSize(dim);
		add(BorderLayout.CENTER, scrollPane);
	}

	public String getName()
	{
		return NAME;
	}

	public Component getComponent()
	{
		return this;
	}

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	}

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
	}

	public DefaultTreeModel getTreeModel()
	{
		return resultTreeModel;
	}

	public void searchStarted()
	{
		caption.setText(jEdit.getProperty("hypersearch-results.searching"));
		resultTreeRoot.removeAllChildren();
		resultTreeModel.reload(resultTreeRoot);
	}

	public void searchDone(int resultCount, int bufferCount)
	{
		updateCaption(resultCount,bufferCount);

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(resultTreeRoot.getChildCount() == 1)
				{
					resultTree.expandPath(new TreePath(
						((DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(0))
						.getPath()));
				}
			}
		});
	}

	// private members
	private View view;

	private JLabel caption;
	private JTree resultTree;
	private DefaultMutableTreeNode resultTreeRoot;
	private DefaultTreeModel resultTreeModel;

	private void updateCaption(int resultCount, int bufferCount)
	{
		Object[] pp = { new Integer(resultCount), new Integer(bufferCount) };
		caption.setText(jEdit.getProperty("hypersearch-results.caption",pp));
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			TreePath path = resultTree.getPathForLocation(
				evt.getX(),evt.getY());
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
				view.toFront();
				view.requestFocus();
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
						int pos = result.linePos.getOffset();
						view.setBuffer(buffer);
						view.getTextArea().setCaretPosition(pos);
						view.toFront();
						view.requestFocus();
					}
				});
			}
		}
	}


	class ResultCellRenderer extends DefaultTreeCellRenderer
	{
		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			Component comp = super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,hasFocus);
			if (!(comp instanceof JLabel))
				return comp;
			JLabel label = (JLabel)comp;
			label.setIcon(null);
			return label;
		}
	}
}
