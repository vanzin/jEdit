/*
 * VFSBrowser.java - VFS browser
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.browser;

import gnu.regexp.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * The main class of the VFS browser.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowser extends JPanel implements EBComponent
{
	public static final String NAME = "vfs.browser";

	/**
	 * Open file dialog mode. Equals JFileChooser.OPEN_DIALOG for
	 * backwards compatibility.
	 */
	public static final int OPEN_DIALOG = 0;

	/**
	 * Save file dialog mode. Equals JFileChooser.SAVE_DIALOG for
	 * backwards compatibility.
	 */
	public static final int SAVE_DIALOG = 1;

	/**
	 * Stand-alone browser mode.
	 */
	public static final int BROWSER = 2;

	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 * @param path The path to display
	 */
	public VFSBrowser(View view, String path)
	{
		this(view,path,BROWSER,false);
	}

	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 * @param path The path to display
	 * @param mode The browser mode
	 * @param multipleSelection True if multiple selection should be allowed
	 */
	public VFSBrowser(View view, String path, int mode, boolean multipleSelection)
	{
		super(new BorderLayout());

		listenerList = new EventListenerList();

		this.mode = mode;
		this.multipleSelection = multipleSelection;
		this.view = view;

		ActionHandler actionHandler = new ActionHandler();

		Box topBox = new Box(BoxLayout.Y_AXIS);
		topBox.add(createToolBar());

		GridBagLayout layout = new GridBagLayout();
		JPanel pathAndFilterPanel = new JPanel(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridwidth = cons.gridheight = 1;
		cons.gridx = cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		cons.anchor = GridBagConstraints.EAST;
		JLabel label = new JLabel(jEdit.getProperty("vfs.browser.path"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		layout.setConstraints(label,cons);
		pathAndFilterPanel.add(label);

		pathField = new HistoryTextField("vfs.browser.path",true,false);

		// because its preferred size can be quite wide, we
		// don't want it to make the browser way too big,
		// so set the preferred width to 0.
		Dimension prefSize = pathField.getPreferredSize();
		prefSize.width = 0;
		pathField.setPreferredSize(prefSize);

		pathField.addActionListener(actionHandler);
		cons.gridx = 1;
		cons.weightx = 1.0f;

		layout.setConstraints(pathField,cons);
		pathAndFilterPanel.add(pathField);

		filterCheckbox = new JCheckBox(jEdit.getProperty("vfs.browser.filter"));
		filterCheckbox.setMargin(new Insets(0,0,0,0));
		filterCheckbox.setRequestFocusEnabled(false);
		filterCheckbox.setBorder(new EmptyBorder(0,0,0,12));
		filterCheckbox.setSelected(mode != BROWSER ||
			jEdit.getBooleanProperty("vfs.browser.filter-enabled"));

		filterCheckbox.addActionListener(actionHandler);
		cons.gridx = 0;
		cons.weightx = 0.0f;
		cons.gridy = 1;
		layout.setConstraints(filterCheckbox,cons);
		pathAndFilterPanel.add(filterCheckbox);

		filterField = new HistoryTextField("vfs.browser.filter",true);
		filterField.addActionListener(actionHandler);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		layout.setConstraints(filterField,cons);
		pathAndFilterPanel.add(filterField);

		topBox.add(pathAndFilterPanel);
		add(BorderLayout.NORTH,topBox);

		add(BorderLayout.CENTER,browserView = new BrowserView(this));

		propertiesChanged();

		HistoryModel filterModel = HistoryModel.getModel("vfs.browser.filter");
		String filter;
		if(mode == BROWSER || view == null || !jEdit.getBooleanProperty(
			"vfs.browser.currentBufferFilter"))
		{
			filter = jEdit.getProperty("vfs.browser.last-filter");
			if(filter == null)
				filter = jEdit.getProperty("vfs.browser.default-filter");
		}
		else
		{
			String name = view.getBuffer().getName();
			int index = name.lastIndexOf('.');

			if(index == -1)
				filter = jEdit.getProperty("vfs.browser.default-filter");
			else
			{
				String ext = name.substring(index);
				filter = "*" + ext;
			}
		}

		filterField.setText(filter);
		filterField.addCurrentToHistory();

		updateFilterEnabled();

		if(path == null)
		{
			String defaultPath = jEdit.getProperty("vfs.browser.defaultPath");
			String userHome = System.getProperty("user.home");
			if(defaultPath.equals("home"))
				path = userHome;
			else if(defaultPath.equals("buffer"))
			{
				if(view != null)
				{
					Buffer buffer = view.getBuffer();
					path = buffer.getVFS().getParentOfPath(
						buffer.getPath());
				}
				else
					path = userHome;
			}
			else if(defaultPath.equals("last"))
			{
				HistoryModel pathModel = HistoryModel.getModel("vfs.browser.path");
				if(pathModel.getSize() == 0)
					path = userHome;
				else
					path = pathModel.getItem(0);
			}
			else if(defaultPath.equals("favorites"))
				path = "favorites:";
			else
			{
				// unknown value??!!!
				path = userHome;
			}
		}

		setDirectory(path);
	}

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		jEdit.setBooleanProperty("vfs.browser.filter-enabled",
			filterCheckbox.isSelected());
		if(mode == BROWSER || !jEdit.getBooleanProperty(
			"vfs.browser.currentBufferFilter"))
		{
			jEdit.setProperty("vfs.browser.last-filter",
				filterField.getText());
		}
		EditBus.removeFromBus(this);
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof ViewUpdate)
			handleViewUpdate((ViewUpdate)msg);
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
		else if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof VFSUpdate)
		{
			// this is a dirty hack and it relies on the fact
			// that updates for parents are sent before updates
			// for the changed nodes themselves (if this was not
			// the case, the browser wouldn't be updated properly
			// on delete, etc).
			//
			// to avoid causing '> 1 request' errors, don't reload
			// directory if request already active
			if(requestRunning)
				return;

			browserView.reloadDirectory(((VFSUpdate)msg).getPath());
		}
	}

	public String getDirectory()
	{
		return path;
	}

	public void setDirectory(String path)
	{
		// have to do this hack until VFSPath class is written
		if(path.length() != 1 && (path.endsWith("/")
			|| path.endsWith(java.io.File.separator)))
			path = path.substring(0,path.length() - 1);

		if(path.startsWith("file:"))
			path = path.substring(5);

		this.path = path;

		pathField.setText(path);
		pathField.addCurrentToHistory();

		reloadDirectory();
	}

	public void reloadDirectory()
	{
		try
		{
			String filter = filterField.getText();
			if(filter.length() == 0)
				filter = "*";
			filenameFilter = new RE(MiscUtilities.globToRE(filter),RE.REG_ICASE);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,VFSBrowser.this,e);
			String[] args = { filterField.getText(),
				e.getMessage() };
			GUIUtilities.error(VFSBrowser.this,"vfs.browser.bad-filter",args);
		}

		loadDirectory(path);
	}

	public void loadDirectory(String path)
	{
		VFS vfs = VFSManager.getVFSForPath(path);

		Object session = vfs.createVFSSession(path,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.LIST_DIRECTORY,this,
			session,vfs,path,null));
	}

	public void delete(String path)
	{
		if(MiscUtilities.isURL(path) && FavoritesVFS.PROTOCOL.equals(
			MiscUtilities.getProtocolOfURL(path)))
		{
			Object[] args = { path.substring(FavoritesVFS.PROTOCOL.length() + 1) };
			int result = GUIUtilities.confirm(this,
				"vfs.browser.delete-favorites",args,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}
		else
		{
			Object[] args = { path };
			int result = GUIUtilities.confirm(this,
				"vfs.browser.delete-confirm",args,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		VFS vfs = VFSManager.getVFSForPath(path);

		Object session = vfs.createVFSSession(path,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.DELETE,this,
			session,vfs,path,null));
	}

	public void rename(String from)
	{
		VFS vfs = VFSManager.getVFSForPath(from);

		String filename = vfs.getFileName(from);
		String[] args = { filename };
		String to = GUIUtilities.input(this,"vfs.browser.rename",
			args,filename);
		if(to == null)
			return;

		to = vfs.constructPath(vfs.getParentOfPath(from),to);

		Object session = vfs.createVFSSession(from,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.RENAME,this,
			session,vfs,from,to));
	}

	public void mkdir()
	{
		String newDirectory = GUIUtilities.input(this,"vfs.browser.mkdir",null);
		if(newDirectory == null)
			return;

		// if a directory is selected, create new dir in there.
		// if a file is selected, create new dir inside its parent.
		VFS.DirectoryEntry[] selected = getSelectedFiles();
		String parent;
		if(selected.length == 0)
			parent = path;
		else if(selected[0].type == VFS.DirectoryEntry.FILE)
		{
			parent = selected[0].path;
			parent = VFSManager.getVFSForPath(parent)
				.getParentOfPath(parent);
		}
		else
			parent = selected[0].path;

		VFS vfs = VFSManager.getVFSForPath(parent);

		// path is the currently viewed directory in the browser
		newDirectory = vfs.constructPath(parent,newDirectory);

		Object session = vfs.createVFSSession(newDirectory,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.MKDIR,this,
			session,vfs,newDirectory,null));
	}

	public View getView()
	{
		return view;
	}

	public int getMode()
	{
		return mode;
	}

	public boolean isMultipleSelectionEnabled()
	{
		return multipleSelection;
	}

	public boolean getShowHiddenFiles()
	{
		return showHiddenFiles;
	}

	public void setShowHiddenFiles(boolean showHiddenFiles)
	{
		this.showHiddenFiles = showHiddenFiles;
	}

	/**
	 * Returns the file name filter glob.
	 * @since jEdit 3.2pre2
	 */
	public String getFilenameFilter()
	{
		if(filterCheckbox.isSelected())
		{
			String filter = filterField.getText();
			if(filter.length() == 0)
				return "*";
			else
				return filter;
		}
		else
			return "*";
	}

	public void setFilenameFilter(String filter)
	{
		if(filter == null || filter.length() == 0 || filter.equals("*"))
			filterCheckbox.setSelected(false);
		else
		{
			filterCheckbox.setSelected(true);
			filterField.setText(filter);
		}
	}

	public BrowserView getBrowserView()
	{
		return browserView;
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		return browserView.getSelectedFiles();
	}

	public void addBrowserListener(BrowserListener l)
	{
		listenerList.add(BrowserListener.class,l);
	}

	public void removeBrowserListener(BrowserListener l)
	{
		listenerList.remove(BrowserListener.class,l);
	}

	// package-private members
	void directoryLoaded(final VFS.DirectoryEntry[] list)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				boolean filterEnabled = filterCheckbox.isSelected();

				Vector directoryVector = new Vector();

				if(list == null)
				{
					browserView.directoryLoaded(directoryVector);
					return;
				}

				for(int i = 0; i < list.length; i++)
				{
					VFS.DirectoryEntry file = list[i];
					if(file.hidden && !showHiddenFiles)
						continue;
					if(file.type == VFS.DirectoryEntry.FILE
						&& filterEnabled
						&& filenameFilter != null
						&& !filenameFilter.isMatch(file.name))
						continue;
					directoryVector.addElement(file);
				}

				if(sortFiles)
				{
					MiscUtilities.quicksort(directoryVector,
						new FileCompare());
				}

				browserView.directoryLoaded(directoryVector);
			}
		});
	}

	class FileCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			VFS.DirectoryEntry file1 = (VFS.DirectoryEntry)obj1;
			VFS.DirectoryEntry file2 = (VFS.DirectoryEntry)obj2;

			if(!sortMixFilesAndDirs)
			{
				if(file1.type != file2.type)
					return file2.type - file1.type;
			}

			if(sortIgnoreCase)
			{
				return file1.name.toLowerCase().compareTo(
					file2.name.toLowerCase());
			}
			else
			{
				return file1.name.compareTo(file2.name);
			}
		}
	}

	void filesSelected()
	{
		VFS.DirectoryEntry[] selectedFiles = browserView.getSelectedFiles();

		if(mode == BROWSER)
		{
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFS.DirectoryEntry file = selectedFiles[i];
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null && view != null)
					view.setBuffer(buffer);
			}
		}

		Object[] listeners = listenerList.getListenerList();
		for(int i = 0; i < listeners.length; i++)
		{
			if(listeners[i] == BrowserListener.class)
			{
				BrowserListener l = (BrowserListener)listeners[i+1];
				l.filesSelected(this,selectedFiles);
			}
		}

	}

	void filesActivated()
	{
		VFS.DirectoryEntry[] selectedFiles = browserView.getSelectedFiles();

		for(int i = 0; i < selectedFiles.length; i++)
		{
			VFS.DirectoryEntry file = selectedFiles[i];

			if(file.type == VFS.DirectoryEntry.DIRECTORY
				|| file.type == VFS.DirectoryEntry.FILESYSTEM)
				setDirectory(file.path);
			else if(mode == BROWSER)
			{
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer == null)
					buffer = jEdit.openFile(null,file.path);
				else if(doubleClickClose)
				{
					jEdit.closeBuffer(view,buffer);
					break;
				}

				if(buffer != null)
				{
					if(view == null)
						view = jEdit.newView(null,buffer);
					else
						view.setBuffer(buffer);
				}
			}
			else
			{
				// if a file is selected in OPEN_DIALOG or
				// SAVE_DIALOG mode, just let the listener(s)
				// handle it
			}
		}

		Object[] listeners = listenerList.getListenerList();
		for(int i = 0; i < listeners.length; i++)
		{
			if(listeners[i] == BrowserListener.class)
			{
				BrowserListener l = (BrowserListener)listeners[i+1];
				l.filesActivated(this,selectedFiles);
			}
		}
	}

	// has to be package-private so that BrowserIORequest can call it
	void endRequest()
	{
		requestRunning = false;
	}

	// private members
	private EventListenerList listenerList;
	private View view;
	private String path;
	private HistoryTextField pathField;
	private JCheckBox filterCheckbox;
	private HistoryTextField filterField;
	private JButton up, reload, roots, home, synchronize;
	private BrowserView browserView;
	private RE filenameFilter;
	private int mode;
	private boolean multipleSelection;

	private boolean showHiddenFiles;
	private boolean sortFiles;
	private boolean sortMixFilesAndDirs;
	private boolean sortIgnoreCase;
	private boolean doubleClickClose;

	private boolean requestRunning;

	private JToolBar createToolBar()
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.putClientProperty("JToolBar.isRollover",Boolean.TRUE);

		toolBar.add(up = createToolButton("up"));
		// see comment in UpMenuButton class to find out why we
		// pass it the up button
		toolBar.add(new UpMenuButton(up));
		toolBar.add(reload = createToolButton("reload"));
		toolBar.addSeparator();
		toolBar.add(roots = createToolButton("roots"));
		toolBar.add(home = createToolButton("home"));
		toolBar.add(synchronize = createToolButton("synchronize"));
		toolBar.addSeparator();

		toolBar.add(new MoreMenuButton());

		toolBar.add(Box.createGlue());
		return toolBar;
	}

	private JButton createToolButton(String name)
	{
		JButton button = new JButton();
		String prefix = "vfs.browser.";

		button.setIcon(GUIUtilities.loadIcon(jEdit.getProperty(
			prefix + name + ".icon")));
		button.setToolTipText(jEdit.getProperty(prefix + name + ".label"));

		button.setRequestFocusEnabled(false);
		button.setMargin(new Insets(0,0,0,0));
		button.addActionListener(new ActionHandler());

		return button;
	}

	private void handleViewUpdate(ViewUpdate vmsg)
	{
		if(vmsg.getWhat() == ViewUpdate.CLOSED
			&& vmsg.getView() == view)
			view = null;
	}

	private void handleBufferUpdate(BufferUpdate bmsg)
	{
		if(bmsg.getWhat() == BufferUpdate.CREATED
			|| bmsg.getWhat() == BufferUpdate.CLOSED)
			browserView.updateFileView();
		else if(bmsg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			// if a buffer becomes clean, it means it was
			// saved. So we repaint the browser view, in
			// case it was a 'save as'
			if(!bmsg.getBuffer().isDirty())
				browserView.updateFileView();
		}
	}

	private void propertiesChanged()
	{
		showHiddenFiles = jEdit.getBooleanProperty("vfs.browser.showHiddenFiles");
		sortFiles = jEdit.getBooleanProperty("vfs.browser.sortFiles");
		sortMixFilesAndDirs = jEdit.getBooleanProperty("vfs.browser.sortMixFilesAndDirs");
		sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		doubleClickClose = jEdit.getBooleanProperty("vfs.browser.doubleClickClose");

		browserView.propertiesChanged();

		if(path != null)
			reloadDirectory();
	}

	/* We do this stuff because the browser is not able to handle
	 * more than one request yet */
	private boolean startRequest()
	{
		if(requestRunning)
		{
			// dump stack trace for debugging purposes
			Log.log(Log.DEBUG,this,new Throwable("For debugging purposes"));

			GUIUtilities.error(this,"browser-multiple-io",null);
			return false;
		}
		else
		{
			requestRunning = true;
			return true;
		}
	}

	private void updateFilterEnabled()
	{
		filterField.setEnabled(filterCheckbox.isSelected());
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == pathField || source == filterField
				|| source == filterCheckbox)
			{
				updateFilterEnabled();

				String path = pathField.getText();
				if(path != null)
					setDirectory(path);
			}
			else if(source == up)
			{
				VFS vfs = VFSManager.getVFSForPath(path);
				setDirectory(vfs.getParentOfPath(path));
			}
			else if(source == reload)
				reloadDirectory();
			else if(source == roots)
				setDirectory(FileRootsVFS.PROTOCOL + ":");
			else if(source == home)
				setDirectory(System.getProperty("user.home"));
			else if(source == synchronize)
			{
				Buffer buffer = view.getBuffer();
				setDirectory(buffer.getVFS().getParentOfPath(
					buffer.getPath()));
			}
		}
	}

	class UpMenuButton extends JButton
	{
		UpMenuButton(JButton upButton)
		{
			// for a better-looking GUI, we display the popup
			// as if it is from the 'up' button, not the arrow
			// to the right of it
			this.upButton = upButton;

			setIcon(GUIUtilities.loadIcon(
				jEdit.getProperty("vfs.browser.up-menu.icon")));
			UpMenuButton.this.setToolTipText(jEdit.getProperty(
				"vfs.browser.up-menu.label"));

			UpMenuButton.this.setRequestFocusEnabled(false);
			setMargin(new Insets(0,0,0,0));
			UpMenuButton.this.addMouseListener(new MouseHandler());
		}

		// private members
		private JButton upButton;
		private JPopupMenu popup;

		private void createPopup()
		{
			popup = new JPopupMenu();
			ActionHandler actionHandler = new ActionHandler();

			VFS vfs = VFSManager.getVFSForPath(path);
			String dir = vfs.getParentOfPath(path);
			for(;;)
			{
				JMenuItem menuItem = new JMenuItem(dir);
				menuItem.addActionListener(actionHandler);
				popup.add(menuItem);
				String parentDir = vfs.getParentOfPath(dir);
				if(parentDir.equals(dir))
					break;
				else
					dir = parentDir;
			}
		}

		class ActionHandler implements ActionListener
		{
			public void actionPerformed(ActionEvent evt)
			{
				setDirectory(evt.getActionCommand());
			}
		}

		class MouseHandler extends MouseAdapter
		{
			public void mousePressed(MouseEvent evt)
			{
				if(popup == null || !popup.isVisible())
				{
					createPopup();
					popup.show(upButton,0,upButton.getHeight());
				}
				else
				{
					popup.setVisible(false);
					popup = null;
				}
			}
		}
	}

	class MoreMenuButton extends JButton
	{
		MoreMenuButton()
		{
			setText(jEdit.getProperty("vfs.browser.more.label"));
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty(
				"vfs.browser.more.icon")));
			setHorizontalTextPosition(SwingConstants.LEFT);

			MoreMenuButton.this.setRequestFocusEnabled(false);
			setMargin(new Insets(0,0,0,0));
			MoreMenuButton.this.addMouseListener(new MouseHandler());
		}

		// private members
		JPopupMenu popup;

		class MouseHandler extends MouseAdapter
		{
			public void mousePressed(MouseEvent evt)
			{
				if(popup == null || !popup.isVisible())
				{
					popup = new BrowserPopupMenu(VFSBrowser.this,null);
					popup.show(MoreMenuButton.this,0,MoreMenuButton.this.getHeight());
				}
				else
				{
					popup.setVisible(false);
					popup = null;
				}
			}
		}
	}
}
