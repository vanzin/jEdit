/*
 * BrowserView.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * VFS browser tree view.
 * @author Slava Pestov
 * @version $Id$
 */
class BrowserView extends JPanel
{
	//{{{ BrowserView constructor
	public BrowserView(final VFSBrowser browser)
	{
		this.browser = browser;

		parentDirectories = new JList();

		parentDirectories.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);

		parentDirectories.setCellRenderer(new ParentDirectoryRenderer());
		parentDirectories.setVisibleRowCount(5);
		parentDirectories.addMouseListener(new MouseHandler());

		final JScrollPane parentScroller = new JScrollPane(parentDirectories);
		parentScroller.setMinimumSize(new Dimension(0,0));

		table = new VFSDirectoryEntryTable(this);
		JScrollPane tableScroller = new JScrollPane(table);
		tableScroller.setMinimumSize(new Dimension(0,0));
		splitPane = new JSplitPane(
			browser.isHorizontalLayout()
			? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT,
			parentScroller,tableScroller);
		splitPane.setOneTouchExpandable(true);

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				String prop = browser.isHorizontalLayout() ? "vfs.browser.horizontalSplitter" : "vfs.browser.splitter";
				int loc = jEdit.getIntegerProperty(prop,-1);
				if(loc == -1)
					loc = parentScroller.getPreferredSize().height;

				splitPane.setDividerLocation(loc);
				parentDirectories.ensureIndexIsVisible(
					parentDirectories.getModel()
					.getSize());
			}
		});

		if(browser.isMultipleSelectionEnabled())
			table.getSelectionModel().setSelectionMode(
				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		else
			table.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);

		setLayout(new BorderLayout());

		add(BorderLayout.CENTER,splitPane);

		propertiesChanged();
	} //}}}

	//{{{ focusOnFileView() method
	public void focusOnFileView()
	{
		table.requestFocus();
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		String prop = browser.isHorizontalLayout() ? "vfs.browser.horizontalSplitter" : "vfs.browser.splitter";
		jEdit.setIntegerProperty(prop,splitPane.getDividerLocation());

		super.removeNotify();
	} //}}}

	//{{{ getSelectedFiles() method
	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		return table.getSelectedFiles();
	} //}}}

	//{{{ selectNone() method
	public void selectNone()
	{
		table.clearSelection();
	} //}}}

	//{{{ loadDirectory() method
	public void loadDirectory(Object node, String path)
	{
		tmpExpanded = table.getExpandedDirectories();

		path = MiscUtilities.constructPath(browser.getDirectory(),path);
		VFS vfs = VFSManager.getVFSForPath(path);

		Object session = vfs.createVFSSession(path,this);
		if(session == null)
			return;

		if(node == null)
		{
			parentDirectories.setListData(new Object[] {
				new LoadingPlaceholder() });
		}

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.LIST_DIRECTORY,browser,
			session,vfs,path,null,node));
	} //}}}

	//{{{ directoryLoaded() method
	public void directoryLoaded(Object node, String path, ArrayList directory)
	{
		//{{{ If reloading root, update parent directory list
		if(node == null)
		{
			DefaultListModel parentList = new DefaultListModel();

			String parent = path;

			if(parent.length() != 1 && (parent.endsWith("/")
				|| parent.endsWith(File.separator)))
				parent = parent.substring(0,parent.length() - 1);

			for(;;)
			{
				VFS _vfs = VFSManager.getVFSForPath(
					parent);
				// create a DirectoryEntry manually
				// instead of using _vfs._getDirectoryEntry()
				// since so many VFS's have broken
				// implementations of this method
				parentList.insertElementAt(new VFS.DirectoryEntry(
					_vfs.getFileName(parent),
					parent,parent,
					VFS.DirectoryEntry.DIRECTORY,
					0L,false),0);
				String newParent = _vfs.getParentOfPath(parent);
				if(newParent.length() != 1 && (newParent.endsWith("/")
					|| newParent.endsWith(File.separator)))
					newParent = newParent.substring(0,newParent.length() - 1);

				if(newParent == null || parent.equals(newParent))
					break;
				else
					parent = newParent;
			}

			parentDirectories.setModel(parentList);
			int index = parentList.getSize() - 1;
			parentDirectories.setSelectedIndex(index);
			parentDirectories.ensureIndexIsVisible(index);
		} //}}}

		LinkedList toExpand = new LinkedList();

		table.setDirectory(node,directory);

		if(directory != null)
		{
			for(int i = 0; i < directory.size(); i++)
			{
				VFS.DirectoryEntry file = (VFS.DirectoryEntry)
					directory.get(i);
				boolean allowsChildren = (file.type != VFS.DirectoryEntry.FILE);
				if(tmpExpanded != null && tmpExpanded.contains(file.path))
					loadDirectory(null,file.path);
			}
		}
	} //}}}

	//{{{ updateFileView() method
	public void updateFileView()
	{
		table.repaint();
	} //}}}

	//{{{ maybeReloadDirectory() method
	public void maybeReloadDirectory(String path)
	{
		String browserDir = browser.getDirectory();

		if(path.equals(browserDir))
			loadDirectory(null,path);

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

		if(!browserDir.startsWith(FavoritesVFS.PROTOCOL)
			&& !browserDir.startsWith(FileRootsVFS.PROTOCOL)
			&& !path.startsWith(browserDir))
			return;

		if(browserDir.startsWith(FileRootsVFS.PROTOCOL)
			&& MiscUtilities.isURL(path)
			&& !MiscUtilities.getProtocolOfURL(path)
			.equals("file"))
			return;

		table.maybeReloadDirectory(path);
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
		table.propertiesChanged();

		splitPane.setBorder(null);
	} //}}}

	//{{{ getTable() method
	public VFSDirectoryEntryTable getTable()
	{
		return table;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private VFSBrowser browser;

	private JSplitPane splitPane;
	private JList parentDirectories;
	private VFSDirectoryEntryTable table;
	private Set tmpExpanded;
	private BrowserCommandsMenu popup;
	private boolean showIcons;
	//}}}

	//{{{ showFilePopup() method
	private void showFilePopup(VFS.DirectoryEntry[] files, Component comp,
		Point point)
	{
		popup = new BrowserCommandsMenu(browser,files);
		// for the parent directory right-click; on the click we select
		// the clicked item, but when the popup goes away we select the
		// currently showing directory.
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			public void popupMenuCanceled(PopupMenuEvent e) {}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				int index = parentDirectories.getModel().getSize() - 1;
				parentDirectories.setSelectedIndex(index);
			}
		});
		GUIUtilities.showPopupMenu(popup,comp,point.x,point.y);
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ParentDirectoryRenderer class
	class ParentDirectoryRenderer extends DefaultListCellRenderer
	{
		Font plainFont, boldFont;

		ParentDirectoryRenderer()
		{
			plainFont = UIManager.getFont("Tree.font");
			if(plainFont == null)
				plainFont = jEdit.getFontProperty("metal.secondary.font");
			boldFont = new Font(plainFont.getName(),Font.BOLD,plainFont.getSize());
		}

		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			ParentDirectoryRenderer.this.setBorder(new EmptyBorder(
				1,index * 5 + 1,1,1));

			if(value instanceof LoadingPlaceholder)
			{
				ParentDirectoryRenderer.this.setFont(plainFont);

				setIcon(showIcons ? FileCellRenderer.loadingIcon : null);
				setText(jEdit.getProperty("vfs.browser.tree.loading"));
			}
			else if(value instanceof VFS.DirectoryEntry)
			{
				VFS.DirectoryEntry dirEntry = (VFS.DirectoryEntry)value;
				ParentDirectoryRenderer.this.setFont(boldFont);

				setIcon(showIcons ? FileCellRenderer.getIconForFile(dirEntry,true)
					: null);
				setText(dirEntry.name);
			}
			else if(value == null)
				setText("VFS does not follow VFS API");

			return this;
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			int row = parentDirectories.locationToIndex(evt.getPoint());
			if(row != -1)
			{
				Object obj = parentDirectories.getModel()
					.getElementAt(row);
				if(obj instanceof VFS.DirectoryEntry)
				{
					VFS.DirectoryEntry dirEntry = ((VFS.DirectoryEntry)obj);
					if(GUIUtilities.isPopupTrigger(evt))
					{
						if(popup != null && popup.isVisible())
						{
							popup.setVisible(false);
							popup = null;
						}
						else
						{
							parentDirectories.setSelectedIndex(row);
							showFilePopup(new VFS.DirectoryEntry[] {
								dirEntry },parentDirectories,
								evt.getPoint());
						}
					}
				}
			}
		}

		public void mouseReleased(MouseEvent evt)
		{
			if(evt.getClickCount() % 2 != 0 &&
				!GUIUtilities.isMiddleButton(evt.getModifiers()))
				return;

			int row = parentDirectories.locationToIndex(evt.getPoint());
			if(row != -1)
			{
				Object obj = parentDirectories.getModel()
					.getElementAt(row);
				if(obj instanceof VFS.DirectoryEntry)
				{
					VFS.DirectoryEntry dirEntry = ((VFS.DirectoryEntry)obj);
					if(!GUIUtilities.isPopupTrigger(evt))
					{
						browser.setDirectory(dirEntry.path);
						focusOnFileView();
					}
				}
			}
		}
	} //}}}

	//{{{ BrowserJTree class
	/*class BrowserJTree extends JTree
	{
		//{{{ processKeyEvent() method
		public void processKeyEvent(KeyEvent evt)
		{
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
					super.processKeyEvent(evt);
					if(browser.getMode() != VFSBrowser.BROWSER)
						browser.filesSelected();
					break;
				case KeyEvent.VK_ENTER:
					browser.filesActivated((evt.isShiftDown()
						? VFSBrowser.M_OPEN_NEW_VIEW
						: VFSBrowser.M_OPEN),false);
					evt.consume();
					break;
				case KeyEvent.VK_LEFT:
					String directory = browser.getDirectory();
					browser.setDirectory(MiscUtilities
						.getParentOfPath(directory));
					evt.consume();
					break;
				}
			}
			else if(evt.getID() == KeyEvent.KEY_TYPED)
			{
				if(evt.isControlDown() || evt.isAltDown()
					|| evt.isMetaDown())
				{
					return;
				}

				// hack...
				if(evt.isShiftDown() && evt.getKeyChar() == '\n')
					return;

				switch(evt.getKeyChar())
				{
				case '~':
					if(browser.getMode() == VFSBrowser.BROWSER)
					{
						browser.setDirectory(System.getProperty("user.home"));
						return;
					}
					else
						break;
				case '/':
					if(browser.getMode() == VFSBrowser.BROWSER)
					{
						browser.rootDirectory();
						return;
					}
					else
						break;
				case '-':
					if(browser.getMode() == VFSBrowser.BROWSER)
					{
						Buffer buffer = browser.getView().getBuffer();
						browser.setDirectory(buffer.getDirectory());
						return;
					}
					else
						break;
				}
			}

			if(!evt.isConsumed())
				super.processKeyEvent(evt);
		} //}}}

		//{{{ processMouseEvent() method
		protected void processMouseEvent(MouseEvent evt)
		{
			//ToolTipManager ttm = ToolTipManager.sharedInstance();

			TreePath path = getPathForLocation(evt.getX(),evt.getY());

			switch(evt.getID())
			{
			/* //{{{ MOUSE_ENTERED...
			case MouseEvent.MOUSE_ENTERED:
				toolTipInitialDelay = ttm.getInitialDelay();
				toolTipReshowDelay = ttm.getReshowDelay();
				ttm.setInitialDelay(200);
				ttm.setReshowDelay(0);
				super.processMouseEvent(evt);
				break; //}}}
			//{{{ MOUSE_EXITED...
			case MouseEvent.MOUSE_EXITED:
				ttm.setInitialDelay(toolTipInitialDelay);
				ttm.setReshowDelay(toolTipReshowDelay);
				super.processMouseEvent(evt);
				break; //}}}
			//{{{ MOUSE_CLICKED...
			case MouseEvent.MOUSE_CLICKED:
				if(path != null)
				{
					// A double click is not only when clickCount == 2
					// because every other click can open a new directory
					if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0
						&& evt.getClickCount() % 2 == 0)
					{
						setSelectionPath(path);

						// don't pass double-clicks to tree, otherwise
						// directory nodes will be expanded and we don't
						// want that
						browser.filesActivated((evt.isShiftDown()
							? VFSBrowser.M_OPEN_NEW_VIEW
							: VFSBrowser.M_OPEN),true);
						break;
					}
					else if(GUIUtilities.isMiddleButton(evt.getModifiers()))
					{
						browser.filesActivated((evt.isShiftDown()
							? VFSBrowser.M_OPEN_NEW_VIEW
							: VFSBrowser.M_OPEN),true);
					}
					else if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
					{
						if(!isPathSelected(path))
							setSelectionPath(path);
					}

					super.processMouseEvent(evt);
					break;
				}
				else if(GUIUtilities.isPopupTrigger(evt))
					break;
			//}}}
			//{{{ MOUSE_PRESSED...
			case MouseEvent.MOUSE_PRESSED:
				if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
				{
					if(evt.getClickCount() % 2 == 0)
						break;
				}

				if(GUIUtilities.isMiddleButton(evt.getModifiers()))
				{
					if(!isPathSelected(path))
						setSelectionPath(path);
				}
				else if(GUIUtilities.isPopupTrigger(evt))
				{
					if(popup != null && popup.isVisible())
					{
						popup.setVisible(false);
						popup = null;
						break;
					}

					if(path == null)
						showFilePopup(null,this,evt.getPoint());
					else
					{
						if(!isPathSelected(path))
							setSelectionPath(path);

						showFilePopup(getSelectedFiles(),this,evt.getPoint());
					}

					break;
				}

				super.processMouseEvent(evt);
				break;
			//}}}
			//{{{ MOUSE_RELEASED...
			case MouseEvent.MOUSE_RELEASED:
				if(!GUIUtilities.isPopupTrigger(evt)
					&& path != null)
				{
					browser.filesSelected();
				}

				if(evt.getClickCount() % 2 != 0)
					super.processMouseEvent(evt);

				break;
			//}}}
			default:
				super.processMouseEvent(evt);
				break;
			}
		} //}}}

		//}}}
	}*/ //}}}

	//{{{ TreeHandler class
	/*class TreeHandler implements TreeExpansionListener
	{
		//{{{ treeExpanded() method
		public void treeExpanded(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			Object userObject = treeNode.getUserObject();
			if(userObject instanceof VFS.DirectoryEntry)
			{
				loadDirectory(treeNode,((VFS.DirectoryEntry)
					userObject).path,true);
			}
		} //}}}
	}*/ //}}}

	static class LoadingPlaceholder {}
	//}}}
}
