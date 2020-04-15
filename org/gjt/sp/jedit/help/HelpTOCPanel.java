/*
 * HelpTOCPanel.java - Help table of contents
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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

package org.gjt.sp.jedit.help;

//{{{ Imports
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.gjt.sp.util.ThreadUtilities;

import org.gjt.sp.jedit.browser.FileCellRenderer; // for icons
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.EnhancedTreeCellRenderer;

import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;
//}}}

public class HelpTOCPanel extends JPanel
{
	//{{{ HelpTOCPanel constructor
	public HelpTOCPanel(HelpViewerInterface helpViewer)
	{
		super(new BorderLayout());

		this.helpViewer = helpViewer;
		nodes = new HashMap<>();

		toc = new TOCTree();

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			toc.putClientProperty("JTree.lineStyle", "Angled");

		toc.setRowHeight(0);
		toc.setCellRenderer(new TOCCellRenderer());
		toc.setEditable(false);
		toc.setShowsRootHandles(true);

		add(BorderLayout.CENTER,new JScrollPane(toc));

		load();
	} //}}}

	//{{{ selectNode() method
	public void selectNode(String shortURL)
	{
		if(tocModel == null)
			return;

		final DefaultMutableTreeNode node =
			nodes.get(shortURL);

		if(node == null)
			return;

		EventQueue.invokeLater(() ->
		{
			TreePath path = new TreePath(tocModel.getPathToRoot(node));
			toc.expandPath(path);
			toc.setSelectionPath(path);
			toc.scrollPathToVisible(path);
		});
	} //}}}

	//{{{ load() method
	public void load()
	{
		DefaultTreeModel empty = new DefaultTreeModel(
			new DefaultMutableTreeNode(
			jEdit.getProperty("helpviewer.toc.loading")));
		toc.setModel(empty);
		toc.setRootVisible(true);

		ThreadUtilities.runInBackground(() ->
		{
			DefaultMutableTreeNode tocRoot = new HelpTOCLoader(nodes, helpViewer.getBaseURL()).createTOC();
			tocModel = new DefaultTreeModel(tocRoot);
			toc.setModel(tocModel);
			toc.setRootVisible(false);
			for(int i = 0; i <tocRoot.getChildCount(); i++)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tocRoot.getChildAt(i);
				toc.expandPath(new TreePath(node.getPath()));
			}
			if(helpViewer.getShortURL() != null)
				selectNode(helpViewer.getShortURL());
		});
	} //}}}

	//{{{ Private members
	private final HelpViewerInterface helpViewer;
	private DefaultTreeModel tocModel;
	private final JTree toc;
	private final Map<String, DefaultMutableTreeNode> nodes;
	//}}}

	//{{{ TOCTree class
	class TOCTree extends JTree
	{
		//{{{ TOCTree constructor
		TOCTree()
		{
			ToolTipManager.sharedInstance().registerComponent(this);
			selectionModel.setSelectionMode(SINGLE_TREE_SELECTION);
		} //}}}

		//{{{ getToolTipText() method
		@Override
		public final String getToolTipText(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
					return path.getLastPathComponent().toString();
			}
			return null;
		} //}}}

		//{{{ processKeyEvent() method
		@Override
		public void processKeyEvent(KeyEvent evt)
		{
			if ((KeyEvent.KEY_PRESSED == evt.getID()) &&
			    (KeyEvent.VK_ENTER == evt.getKeyCode()))
			{
				TreePath path = getSelectionPath();
				expandOrGotoPath(path);

				evt.consume();
			}
			else
			{
				super.processKeyEvent(evt);
			}
		} //}}}

		//{{{ processMouseEvent() method
		@Override
		protected void processMouseEvent(MouseEvent evt)
		{
			switch(evt.getID())
			{
			case MouseEvent.MOUSE_CLICKED:
				TreePath path = getPathForLocation(evt.getX(),evt.getY());
				
				expandOrGotoPath(path);
				
				super.processMouseEvent(evt);
				break;
			default:
				super.processMouseEvent(evt);
				break;
			}
		} //}}}

		//{{{ cellRectIsVisible() method
		private boolean cellRectIsVisible(Rectangle cellRect)
		{
			Rectangle vr = TOCTree.this.getVisibleRect();
			return vr.contains(cellRect.x,cellRect.y) &&
				vr.contains(cellRect.x + cellRect.width,
				cellRect.y + cellRect.height);
		} //}}}
		
		//{{{ expandOrGotoPath() method
		private void expandOrGotoPath(TreePath path)
		{
			if(path != null)
			{
				if(!isPathSelected(path)) setSelectionPath(path);

				Object obj = ((DefaultMutableTreeNode)
					path.getLastPathComponent()).getUserObject();

				if(obj instanceof HelpTOCLoader.HelpNode)
				{
					HelpTOCLoader.HelpNode node = (HelpTOCLoader.HelpNode)obj;
					helpViewer.gotoURL(node.href,true,0);
				}
				else
				{
					this.expandPath(path);
				}
			}
		} //}}}


	} //}}}

	//{{{ TOCCellRenderer class
	static class TOCCellRenderer extends EnhancedTreeCellRenderer
	{
		@Override
		protected TreeCellRenderer newInstance()
		{
			return new TOCCellRenderer();
		}

		@Override
		protected void configureTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean focus)
		{
			setIcon(leaf ? FileCellRenderer.fileIcon
				: (expanded ? FileCellRenderer.openDirIcon
				: FileCellRenderer.dirIcon));
			setBorder(border);
		}

		EmptyBorder border = new EmptyBorder(1,0,1,1);
	} //}}}

}
