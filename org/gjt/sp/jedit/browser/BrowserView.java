/*
 * BrowserView.java
 * Copyright (C) 2000 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;

/**
 * VFS browser tree view.
 * @author Slava Pestov
 * @version $Id$
 */
public class BrowserView extends JPanel
{
	public BrowserView(VFSBrowser browser)
	{
		this.browser = browser;

		currentlyLoadingTreeNode = rootNode = new DefaultMutableTreeNode(null,true);
		model = new DefaultTreeModel(rootNode,true);

		tree = new BrowserJTree(model);
		tree.setCellRenderer(renderer);
		tree.setEditable(false);
		tree.addTreeExpansionListener(new TreeHandler());
		tree.putClientProperty("JTree.lineStyle", "Angled");

		if(browser.isMultipleSelectionEnabled())
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		else
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		setLayout(new BorderLayout());

		scroller = new JScrollPane(tree);
		scroller.setPreferredSize(new Dimension(0,200));
		add(BorderLayout.CENTER,scroller);

		propertiesChanged();
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		Vector selected = new Vector(tree.getSelectionCount());
		TreePath[] paths = tree.getSelectionPaths();
		if(paths == null)
			return new VFS.DirectoryEntry[0];

		for(int i = 0; i < paths.length; i++)
		{
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				paths[i].getLastPathComponent();
			Object obj = treeNode.getUserObject();
			if(obj instanceof VFS.DirectoryEntry)
				selected.addElement(obj);
		}

		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[selected.size()];
		selected.copyInto(retVal);
		return retVal;
	}

	public void selectNone()
	{
		tree.setSelectionPaths(new TreePath[0]);
	}

	public void directoryLoaded(Vector directory)
	{
		if(currentlyLoadingTreeNode == rootNode)
			rootNode.setUserObject(browser.getDirectory());

		currentlyLoadingTreeNode.removeAllChildren();

		if(directory != null)
		{
			for(int i = 0; i < directory.size(); i++)
			{
				VFS.DirectoryEntry file = (VFS.DirectoryEntry)
					directory.elementAt(i);
				boolean allowsChildren = (file.type != VFS.DirectoryEntry.FILE);
				currentlyLoadingTreeNode.add(new DefaultMutableTreeNode(file,allowsChildren));
			}
		}

		// fire events
		model.reload(currentlyLoadingTreeNode);

		tree.expandPath(new TreePath(currentlyLoadingTreeNode.getPath()));

		/* If the user expands a tree node manually, the tree
		 * listener sets currentlyLoadingTreeNode to that.
		 * But if VFSBrowser.setDirectory() is called, we want
		 * the root node to be updated.
		 *
		 * Since the browser view receives no prior notification
		 * to a setDirectory(), we set the currentlyLoadingTreeNode
		 * to null here. */
		currentlyLoadingTreeNode = rootNode;

		timer.stop();
		typeSelectBuffer.setLength(0);
	}

	public void updateFileView()
	{
		tree.repaint();
	}

	public void reloadDirectory(String path)
	{
		// because this method is called for *every* VFS update,
		// we don't want to scan the tree all the time. So we
		// use the following algorithm to determine if the path
		// might be part of the tree:
		// - if the path starts with the browser's current directory,
		//   we do the tree scan
		// - if the browser's directory is 'favorites:' -- we have to
		//   do the tree scan, as every path can appear under the
		//   favorites list
		// - if the browser's directory is 'roots:' and path is on
		//   the local filesystem, do a tree scan
		String browserDir = browser.getDirectory();
		if(browserDir.startsWith(FavoritesVFS.PROTOCOL))
			reloadDirectory(rootNode,path);
		else if(browserDir.startsWith(FileRootsVFS.PROTOCOL))
		{
			if(!MiscUtilities.isURL(path) || MiscUtilities.getProtocolOfURL(path)
				.equals("file"))
				reloadDirectory(rootNode,path);
		}
		else if(path.startsWith(browserDir))
			reloadDirectory(rootNode,path);
	}

	public Component getDefaultFocusComponent()
	{
		return tree;
	}

	public void propertiesChanged()
	{
		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
		renderer.propertiesChanged();
	}

	// private members
	private VFSBrowser browser;

	private JTree tree;
	private JScrollPane scroller;
	private DefaultTreeModel model;
	private DefaultMutableTreeNode rootNode;
	private DefaultMutableTreeNode currentlyLoadingTreeNode;
	private BrowserPopupMenu popup;

	// used for tool tips
	private boolean showIcons;
	private FileCellRenderer renderer = new FileCellRenderer();

	private StringBuffer typeSelectBuffer = new StringBuffer();
	private Timer timer = new Timer(0,new ClearTypeSelect());

	class ClearTypeSelect implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			typeSelectBuffer.setLength(0);
			browser.filesSelected();
		}
	}

	private boolean reloadDirectory(DefaultMutableTreeNode node, String path)
	{
		// nodes which are not expanded need not be checked
		if(!tree.isExpanded(new TreePath(node.getPath())))
			return false;

		Object userObject = node.getUserObject();
		if(userObject instanceof String)
		{
			if(path.equals(userObject))
			{
				loadDirectoryNode(node,path,false);
				return true;
			}
		}
		else if(userObject instanceof VFS.DirectoryEntry)
		{
			VFS.DirectoryEntry file = (VFS.DirectoryEntry)userObject;

			// we don't need to do anything with files!
			if(file.type == VFS.DirectoryEntry.FILE)
				return false;

			if(path.equals(file.path))
			{
				loadDirectoryNode(node,path,false);
				return true;
			}
		}

		if(node.getChildCount() != 0)
		{
			Enumeration children = node.children();
			while(children.hasMoreElements())
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)
					children.nextElement();
				if(reloadDirectory(child,path))
					return true;
			}
		}

		return false;
	}

	private void loadDirectoryNode(DefaultMutableTreeNode node, String path,
		boolean showLoading)
	{
		currentlyLoadingTreeNode = node;

		if(showLoading)
		{
			node.removeAllChildren();
			node.add(new DefaultMutableTreeNode(new LoadingPlaceholder(),false));
		}

		// fire events
		model.reload(currentlyLoadingTreeNode);

		browser.loadDirectory(path);
	}

	private void showFilePopup(VFS.DirectoryEntry file, Point point)
	{
		popup = new BrowserPopupMenu(browser,file);
		popup.show(tree,point.x+1,point.y+1);
	}

	class BrowserJTree extends JTree
	{
		BrowserJTree(TreeModel model)
		{
			super(model);
			ToolTipManager.sharedInstance().registerComponent(this);
		}

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
		}

		public final Point getToolTipLocation(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
				{
					return new Point(cellRect.x + (showIcons ? 20 : 1),
						cellRect.y + (showIcons ? 1 : -1));
				}
			}
			return null;
		}

		protected void processKeyEvent(KeyEvent evt)
		{
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_ENTER:
					browser.filesActivated();
					evt.consume();
					break;
				case KeyEvent.VK_LEFT:
					if(getMinSelectionRow() == -1
						|| getMinSelectionRow() == 0)
					{
						String directory = browser.getDirectory();
						browser.setDirectory(VFSManager.getVFSForPath(
							directory).getParentOfPath(directory));
						evt.consume();
					}
					break;
				}
			}
			else if(evt.getID() == KeyEvent.KEY_TYPED)
			{
				typeSelectBuffer.append(evt.getKeyChar());
				doTypeSelect(typeSelectBuffer.toString());

				timer.stop();
				timer.setInitialDelay(500);
				timer.setRepeats(false);
				timer.start();
			}

			if(!evt.isConsumed())
				super.processKeyEvent(evt);
		}

		protected void processMouseEvent(MouseEvent evt)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();

			switch(evt.getID())
			{
			case MouseEvent.MOUSE_ENTERED:
				toolTipInitialDelay = ttm.getInitialDelay();
				toolTipReshowDelay = ttm.getReshowDelay();
				ttm.setInitialDelay(200);
				ttm.setReshowDelay(0);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_EXITED:
				ttm.setInitialDelay(toolTipInitialDelay);
				ttm.setReshowDelay(toolTipReshowDelay);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_CLICKED:
				if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
				{
					TreePath path = getPathForLocation(evt.getX(),evt.getY());
					if(path == null)
					{
						super.processMouseEvent(evt);
						break;
					}

					if(!isPathSelected(path))
						setSelectionPath(path);

					if(evt.getClickCount() == 1)
					{
						browser.filesSelected();
						super.processMouseEvent(evt);
					}
					if(evt.getClickCount() == 2)
					{
						// don't pass double-clicks to tree, otherwise
						// directory nodes will be expanded and we don't
						// want that
						browser.filesActivated();
						break;
					}
				}
				else if(GUIUtilities.isPopupTrigger(evt))
					; // do nothing

				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_PRESSED:
				if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
				{
					if(popup != null && popup.isVisible())
						popup.setVisible(false);

					if(evt.getClickCount() == 2)
						break;
				}
				else if(GUIUtilities.isPopupTrigger(evt))
				{
					if(popup != null && popup.isVisible())
					{
						popup.setVisible(false);
						break;
					}

					TreePath path = getPathForLocation(evt.getX(),evt.getY());
					if(path == null)
						showFilePopup(null,evt.getPoint());
					else
					{
						setSelectionPath(path);
						browser.filesSelected();
	
						Object userObject = ((DefaultMutableTreeNode)path
							.getLastPathComponent()).getUserObject();
						if(userObject instanceof VFS.DirectoryEntry)
						{
							VFS.DirectoryEntry file = (VFS.DirectoryEntry)
								userObject;
							showFilePopup(file,evt.getPoint());
						}
						else
							showFilePopup(null,evt.getPoint());
					}

					break;
				}

				super.processMouseEvent(evt);
				break;
			default:
				super.processMouseEvent(evt);
				break;
			}
		}

		// private members
		private int toolTipInitialDelay = -1;
		private int toolTipReshowDelay = -1;

		private boolean cellRectIsVisible(Rectangle cellRect)
		{
			Rectangle vr = BrowserJTree.this.getVisibleRect();
			return vr.contains(cellRect.x,cellRect.y) &&
				vr.contains(cellRect.x + cellRect.width,
				cellRect.y + cellRect.height);
		}

		private void doTypeSelect(String str)
		{
			if(getSelectionCount() == 0)
				doTypeSelect(str,0,getRowCount());
			else
			{
				int start = getMaxSelectionRow();
				boolean retVal = doTypeSelect(str,start,getRowCount());

				if(!retVal)
				{
					// scan from selection to end failed, so
					// scan from start to selection
					doTypeSelect(str,0,start);
				}
			}
		}

		private boolean doTypeSelect(String str, int start, int end)
		{
			for(int i = start; i < end; i++)
			{
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
					getPathForRow(i).getLastPathComponent();
				Object obj = treeNode.getUserObject();
				if(obj instanceof VFS.DirectoryEntry)
				{
					VFS.DirectoryEntry file = (VFS.DirectoryEntry)obj;
					if(file.name.regionMatches(true,0,str,0,str.length()))
					{
						clearSelection();
						setSelectionRow(i);
						scrollRowToVisible(i);
						return true;
					}
				}
			}

			return false;
		}
	}

	class TreeHandler implements TreeExpansionListener
	{
		public void treeExpanded(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			Object userObject = treeNode.getUserObject();
			if(userObject instanceof VFS.DirectoryEntry)
			{
				loadDirectoryNode(treeNode,((VFS.DirectoryEntry)
					userObject).path,true);
			}
		}

		public void treeCollapsed(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			if(treeNode.getUserObject() instanceof VFS.DirectoryEntry)
			{
				// we add the placeholder so that the node has
				// 1 child (otherwise the user won't be able to
				// expand it again)
				treeNode.removeAllChildren();
				treeNode.add(new DefaultMutableTreeNode(new LoadingPlaceholder(),false));
				model.reload(treeNode);
			}
		}
	}

	class LoadingPlaceholder {}
}
