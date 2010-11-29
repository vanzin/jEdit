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
import java.io.IOException;
import java.util.*;

import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * VFS browser tree view.
 * @author Slava Pestov
 * @version $Id$
 */
class BrowserView extends JPanel
{
	//{{{ BrowserView constructor
	BrowserView(final VFSBrowser browser)
	{
		this.browser = browser;

		tmpExpanded = new HashSet<String>();
		DockableWindowManager dwm = jEdit.getActiveView().getDockableWindowManager();
		KeyListener keyListener = dwm.closeListener(VFSBrowser.NAME);

		parentDirectories = new ParentDirectoryList();
		parentDirectories.addKeyListener(keyListener);
		parentDirectories.setName("parent");
		
		parentDirectories.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		parentDirectories.setCellRenderer(new ParentDirectoryRenderer());
		parentDirectories.setVisibleRowCount(5);
		parentDirectories.addMouseListener(new ParentMouseHandler());

		final JScrollPane parentScroller = new JScrollPane(parentDirectories);
		parentScroller.setMinimumSize(new Dimension(0,0));

		table = new VFSDirectoryEntryTable(this);
		table.addMouseListener(new TableMouseHandler());
		table.setName("file");
		JScrollPane tableScroller = new JScrollPane(table);
		tableScroller.setMinimumSize(new Dimension(0,0));
		tableScroller.getViewport().setBackground(table.getBackground());
		tableScroller.getViewport().addMouseListener(new TableMouseHandler());
		splitPane = new JSplitPane(
			browser.isHorizontalLayout()
			? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT,
			jEdit.getBooleanProperty("appearance.continuousLayout"),
			parentScroller, tableScroller);
		splitPane.setOneTouchExpandable(true);

		EventQueue.invokeLater(new Runnable()
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
	@Override
	public void removeNotify()
	{
		String prop = browser.isHorizontalLayout() ? "vfs.browser.horizontalSplitter" : "vfs.browser.splitter";
		jEdit.setIntegerProperty(prop,splitPane.getDividerLocation());

		super.removeNotify();
	} //}}}

	//{{{ getSelectedFiles() method
	public VFSFile[] getSelectedFiles()
	{
		return table.getSelectedFiles();
	} //}}}

	//{{{ selectNone() method
	public void selectNone()
	{
		table.clearSelection();
	} //}}}

	//{{{ saveExpansionState() method
	public void saveExpansionState()
	{
		tmpExpanded.clear();
		table.getExpandedDirectories(tmpExpanded);
	} //}}}

	//{{{ clearExpansionState() method
	public void clearExpansionState()
	{
		tmpExpanded.clear();
	} //}}}

	//{{{ loadDirectory() method
	public void loadDirectory(Object node, String path,
		boolean addToHistory)
	{
		loadDirectory(node, path, addToHistory, null);
	} //}}}


	//{{{ loadDirectory() method
	public void loadDirectory(final Object node, String path,
		final boolean addToHistory, final Runnable delayedAWTTask)
	{
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

		final Object[] loadInfo = new Object[2];
		Runnable awtRunnable = new Runnable()
		{
			public void run()
			{
				browser.directoryLoaded(node,loadInfo,addToHistory);
				if (delayedAWTTask != null)
					delayedAWTTask.run();
			}
		};
		ThreadUtilities.runInBackground(new ListDirectoryBrowserTask(browser,
			session, vfs, path, null, loadInfo, awtRunnable));
	} //}}}

	//{{{ directoryLoaded() method
	/**
	 * Rebuild the parent view after a directory has been loaded.
	 *
	 * @param node
	 * @param path
	 * @param directory  
	 */
	public void directoryLoaded(Object node, String path, java.util.List<VFSFile> directory)
	{
		//{{{ If reloading root, update parent directory list
		if(node == null)
		{
			DefaultListModel parentList = new DefaultListModel();

			String parent = path;

			for(;;)
			{
				VFS _vfs = VFSManager.getVFSForPath(parent);
				VFSFile file = null;
				if (_vfs instanceof FileVFS)
				{
					Object session = _vfs.createVFSSession(path, browser);
					try
					{
						file = _vfs._getFile(session, parent, browser);
						if (file != null)
						{
							file.setName(_vfs.getFileName(parent));
						}
					}
					catch (IOException e)
					{
						Log.log(Log.ERROR, this, e, e);
					}
				}
				if (file == null)
				{
					// create a DirectoryEntry manually
					// instead of using _vfs._getFile()
					// since so many VFS's have broken
					// implementations of this method
					file = new VFSFile(
							_vfs.getFileName(parent),
							parent,parent,
							VFSFile.DIRECTORY,
							0L,false);
				}


				/*parentList.insertElementAt(new VFSFile(
					_vfs.getFileName(parent),
					parent,parent,
					VFSFile.DIRECTORY,
					0L,false),0);*/
				parentList.insertElementAt(file,0);
				String newParent = _vfs.getParentOfPath(parent);

				if(newParent == null ||
					MiscUtilities.pathsEqual(parent,newParent))
					break;
				else
					parent = newParent;
			}

			parentDirectories.setModel(parentList);
			int index = parentList.getSize() - 1;
			parentDirectories.setSelectedIndex(index);
			parentDirectories.ensureIndexIsVisible(index);
		} //}}}

		table.setDirectory(VFSManager.getVFSForPath(path),
			node,directory,tmpExpanded);
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
		String symlinkBrowserDir;
		if(MiscUtilities.isURL(browserDir))
		{
			symlinkBrowserDir = browserDir;
		}
		else
		{
			symlinkBrowserDir = MiscUtilities.resolveSymlinks(
				browserDir);
		}

		if(MiscUtilities.pathsEqual(path,symlinkBrowserDir))
		{
			saveExpansionState();
			loadDirectory(null,browserDir,false);
		}

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
			&& !path.startsWith(symlinkBrowserDir))
			return;

		if(browserDir.startsWith(FileRootsVFS.PROTOCOL)
			&& MiscUtilities.isURL(path)
			&& !"file".equals(MiscUtilities.getProtocolOfURL(path)))
			return;

		table.maybeReloadDirectory(path);
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
		table.propertiesChanged();
		GUIUtilities.initContinuousLayout(splitPane);
		splitPane.setBorder(null);
	} //}}}

	//{{{ getBrowser() method
	/**
	 * Returns the associated <code>VFSBrowser</code> instance.
	 * @since jEdit 4.2pre1
	 */
	public VFSBrowser getBrowser()
	{
		return browser;
	} //}}}

	//{{{ getTable() method
	public VFSDirectoryEntryTable getTable()
	{
		return table;
	} //}}}

	//{{{ getParentDirectoryList() method
	public JList getParentDirectoryList()
	{
		return parentDirectories;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final VFSBrowser browser;

	private final JSplitPane splitPane;
	private final JList parentDirectories;
	private final VFSDirectoryEntryTable table;
	private final Set<String> tmpExpanded;
	private BrowserCommandsMenu popup;
	private boolean showIcons;
	//}}}

	//{{{ showFilePopup() method
	private void showFilePopup(VFSFile[] files, Component comp,
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
				// we use SwingUtilities.invokeLater()
				// so that the action is executed before
				// the popup is hidden.
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						int index = parentDirectories
							.getModel()
							.getSize() - 1;
						parentDirectories.setSelectedIndex(index);
					}
				});
			}
		});
		GUIUtilities.showPopupMenu(popup,comp,point.x,point.y);
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ParentDirectoryRenderer class
	class ParentDirectoryRenderer extends DefaultListCellRenderer
	{
		private Font plainFont;
		private final Font boldFont;

		ParentDirectoryRenderer()
		{
			plainFont = UIManager.getFont("Tree.font");
			if(plainFont == null)
				plainFont = jEdit.getFontProperty("metal.secondary.font");
			boldFont = new Font(plainFont.getName(),Font.BOLD,plainFont.getSize());
		}

		@Override
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
			else if(value instanceof VFSFile)
			{
				VFSFile dirEntry = (VFSFile)value;
				ParentDirectoryRenderer.this.setFont(boldFont);

				setIcon(showIcons ? FileCellRenderer.getIconForFile(dirEntry,true)
					: null);
				setText(dirEntry.getName());
			}
			else if(value == null)
				setText("VFS does not follow VFS API");

			return this;
		}
	} //}}}

	//{{{ ParentMouseHandler class
	private class ParentMouseHandler extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent evt)
		{
			int row = parentDirectories.locationToIndex(evt.getPoint());
			if(row != -1)
			{
				Object obj = parentDirectories.getModel()
					.getElementAt(row);
				if(obj instanceof VFSFile)
				{
					VFSFile dirEntry = (VFSFile)obj;
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
							showFilePopup(new VFSFile[] {
								dirEntry },parentDirectories,
								evt.getPoint());
						}
					}
				}
			}
		}

		@Override
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
				if(obj instanceof VFSFile)
				{
					VFSFile dirEntry = (VFSFile)obj;
					if(!GUIUtilities.isPopupTrigger(evt))
					{
						browser.setDirectory(dirEntry.getPath());
						if(browser.getMode() == VFSBrowser.BROWSER)
						focusOnFileView();
					}
				}
			}
		}
	} //}}}

	//{{{ TableMouseHandler class
	private class TableMouseHandler extends MouseAdapter
	{
		//{{{ mouseClicked() method
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			Point p = evt.getPoint();
			int row = table.rowAtPoint(p);
			int column = table.columnAtPoint(p);
			if(row == -1)
				return;
			if(column == 0)
			{
				VFSDirectoryEntryTableModel.Entry entry
					= (VFSDirectoryEntryTableModel.Entry)
					table.getModel().getValueAt(row,0);
				if(FileCellRenderer.ExpansionToggleBorder
					.isExpansionToggle(entry.level,p.x))
				{
					return;
				}
			}

			if((evt.getModifiers() & InputEvent.BUTTON1_MASK) != 0
				&& evt.getClickCount() % 2 == 0)
			{
				browser.filesActivated(evt.isShiftDown()
					? VFSBrowser.M_OPEN_NEW_VIEW
					: VFSBrowser.M_OPEN,true);
			}
			else if(GUIUtilities.isMiddleButton(evt.getModifiers()))
			{
				if(evt.isShiftDown())
					table.getSelectionModel().addSelectionInterval(row,row);
				else
					table.getSelectionModel().setSelectionInterval(row,row);
				browser.filesActivated(evt.isShiftDown()
					? VFSBrowser.M_OPEN_NEW_VIEW
					: VFSBrowser.M_OPEN,true);
			}
		} //}}}

		//{{{ mousePressed() method
		@Override
		public void mousePressed(MouseEvent evt)
		{
			Point p = evt.getPoint();
			if(evt.getSource() != table)
			{
				p.x -= table.getX();
				p.y -= table.getY();
			}

			int row = table.rowAtPoint(p);
			int column = table.columnAtPoint(p);
			if(column == 0 && row != -1)
			{
				VFSDirectoryEntryTableModel.Entry entry
					= (VFSDirectoryEntryTableModel.Entry)
					table.getModel().getValueAt(row,0);
				if(FileCellRenderer.ExpansionToggleBorder
					.isExpansionToggle(entry.level,p.x))
				{
					table.toggleExpanded(row);
					return;
				}
			}

			if(GUIUtilities.isMiddleButton(evt.getModifiers()))
			{
				if(row == -1)
					/* nothing */;
				else if(evt.isShiftDown())
					table.getSelectionModel().addSelectionInterval(row,row);
				else
					table.getSelectionModel().setSelectionInterval(row,row);
			}
			else if(GUIUtilities.isPopupTrigger(evt))
			{
				if(popup != null && popup.isVisible())
				{
					popup.setVisible(false);
					popup = null;
					return;
				}

				if(row == -1)
					showFilePopup(null,table,evt.getPoint());
				else
				{
					if(!table.getSelectionModel().isSelectedIndex(row))
						table.getSelectionModel().setSelectionInterval(row,row);
					showFilePopup(getSelectedFiles(),table,evt.getPoint());
				}
			}
		} //}}}

		//{{{ mouseReleased() method
		@Override
		public void mouseReleased(MouseEvent evt)
		{
			if(!GUIUtilities.isPopupTrigger(evt)
				&& table.getSelectedRow() != -1)
			{
				browser.filesSelected();
			}
		} //}}}
	} //}}}

	private static class LoadingPlaceholder {}
	//}}}
	
	class ParentDirectoryList extends JList
	{

		public String getPath(int row)
		{
			Collection<String> components = new LinkedList<String>();
			for (int i=1; i<=row; ++i)
				components.add(getModel().getElementAt(i).toString());
			return getModel().getElementAt(0) + TextUtilities.join(components, File.separator);
		}

		@Override
		protected void processKeyEvent(KeyEvent evt)
		{
			if (evt.getID() == KeyEvent.KEY_PRESSED)
			{
				ActionContext ac = VFSBrowser.getActionContext();
				int row = parentDirectories.getSelectedIndex();
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_DOWN:
					evt.consume();			
					if (row < parentDirectories.getSize().height-1) 
						parentDirectories.setSelectedIndex(++row);
					break;
				case KeyEvent.VK_LEFT:
					if ((evt.getModifiers() & InputEvent.ALT_MASK)>0)
					{
						evt.consume();
						browser.previousDirectory();
					}
					else super.processEvent(evt);
					break;
				case KeyEvent.VK_RIGHT:
					if ((evt.getModifiers() & InputEvent.ALT_MASK)>0)
					{
						evt.consume();
						browser.nextDirectory();
					}
					else super.processEvent(evt);
					break;
				case KeyEvent.VK_TAB:
					evt.consume();
					if ((evt.getModifiers() & InputEvent.SHIFT_MASK) > 0)
						browser.focusOnDefaultComponent();
					else
						table.requestFocus();
					break;
				case KeyEvent.VK_UP :
					evt.consume();
					if (row > 0)
					{
						parentDirectories.setSelectedIndex(--row);
					}
					break;
				case KeyEvent.VK_BACK_SPACE:
					evt.consume();
					EditAction up = ac.getAction("vfs.browser.up");
					ac.invokeAction(evt, up);
					break;
				case KeyEvent.VK_F5: 
					evt.consume();
					EditAction reload = ac.getAction("vfs.browser.reload");
					ac.invokeAction(evt, reload);
					break;
				case KeyEvent.VK_ENTER: 
					evt.consume();
					String path = getPath(row);
					getBrowser().setDirectory(path);
					table.requestFocus();
					break;
/* These actions don't work because they look at the EntryTable for the current selected
 * 	item. We need actions that look at the parentDirectoryList item instead.
 * 					
				case KeyEvent.VK_DELETE:
					evt.consume();
					ea = ac.getAction("vfs.browser.delete");
					ac.invokeAction(evt, ea);
					break; 
				case KeyEvent.CTRL_MASK | KeyEvent.VK_N:  
					evt.consume();
					ea = ac.getAction("vfs.browser.new-file");
					ac.invokeAction(evt, ea);
					break;
				case KeyEvent.VK_INSERT:
					evt.consume();
					ea = ac.getAction("vfs.browser.new-directory");
					ac.invokeAction(evt, ea);
					break; */					
				}
			}
			else if(evt.getID() == KeyEvent.KEY_TYPED)
			{
				if(evt.isControlDown() || evt.isAltDown()
					|| evt.isMetaDown())
				{
					evt.consume();
					return;
				}
				switch(evt.getKeyChar())
				{
				case '~':
					evt.consume();
					if(browser.getMode() == VFSBrowser.BROWSER)
						browser.setDirectory(System.getProperty(
							"user.home"));
					break;
				case '/':
					evt.consume();
					if(browser.getMode() == VFSBrowser.BROWSER)
						browser.rootDirectory();
					break;
				case '-':
					evt.consume();
					if(browser.getMode() == VFSBrowser.BROWSER)
					{
						browser.setDirectory(
							browser.getView().getBuffer()
							.getDirectory());
					}
					break;
				}
			}
			if (!evt.isConsumed())
				super.processKeyEvent(evt);
		}	
	}
	
}
