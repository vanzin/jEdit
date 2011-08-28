/*
 * VFSBrowser.java - VFS browser
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.bsh.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.menu.MenuItemTextComparator;
//}}}

/**
 * The main class of the VFS browser.
 * Used as dockable, and also embedded inside the
 * VFSFileChooserDialog.
 * 
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowser extends JPanel implements DefaultFocusComponent,
	DockableWindow
{
	public static final String NAME = "vfs.browser";

	//{{{ Browser modes
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
	 * Choose directory dialog mode.
	 */
	public static final int BROWSER_DIALOG = 4;
	/**
	 * Choose directory dialog mode.
	 */
	public static final int CHOOSE_DIRECTORY_DIALOG = 3;

	/**
	 * Stand-alone dockable browser mode.
	 */
	public static final int BROWSER = 2;
	//}}}

	//{{{ browseDirectoryInNewWindow() method
	/**
	 * Opens the specified directory in a new, floating, file system browser.
	 * @param view The view
	 * @param path The directory's path
	 * @since jEdit 4.1pre2
	 */
	public static void browseDirectoryInNewWindow(View view, String path)
	{
		DockableWindowManager wm = view.getDockableWindowManager();
		if(path != null)
		{
			// this is such a bad way of doing it, but oh well...
			jEdit.setTemporaryProperty("vfs.browser.path.tmp",path);
		}
		wm.floatDockableWindow("vfs.browser");
		jEdit.unsetProperty("vfs.browser.path.tmp");
	} //}}}

	//{{{ browseDirectory() method
	/**
	 * Opens the specified directory in a file system browser.
	 * @param view The view
	 * @param path The directory's path
	 * @since jEdit 4.0pre3
	 */
	public static void browseDirectory(View view, String path)
	{
		DockableWindowManager wm = view.getDockableWindowManager();
		VFSBrowser browser = (VFSBrowser)wm.getDockable(NAME);
		if(browser != null)
		{
			wm.showDockableWindow(NAME);
			browser.setDirectory(path);
		}
		else
		{
			if(path != null)
			{
				// this is such a bad way of doing it, but oh well...
				jEdit.setTemporaryProperty("vfs.browser.path.tmp",path);
			}
			wm.addDockableWindow("vfs.browser");
			jEdit.unsetProperty("vfs.browser.path.tmp");
		}
	} //}}}

	//{{{ getActionContext() method
	/**
	 * Returns the browser action context.
	 * @since jEdit 4.2pre1
	 */
	public static ActionContext getActionContext()
	{
		return actionContext;
	} //}}}

	//{{{ VFSBrowser constructor
	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 */
	public VFSBrowser(View view, String position)
	{
		this(view,null,BROWSER,true,position);
	} //}}}

	//{{{ VFSBrowser constructor
	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 * @param path The path to display
	 * @param mode The browser mode
	 * @param multipleSelection True if multiple selection should be allowed
	 * @param position Where the browser is located
	 * @since jEdit 4.2pre1
	 */
	public VFSBrowser(View view, String path, int mode,
		boolean multipleSelection, String position)
	{
		super(new BorderLayout());

		listenerList = new EventListenerList();

		this.mode = mode;
		this.multipleSelection = multipleSelection;
		this.view = view;

		
		currentEncoding = null;
		autoDetectEncoding = jEdit.getBooleanProperty(
			"buffer.encodingAutodetect");

		ActionHandler actionHandler = new ActionHandler();

		topBox = new Box(BoxLayout.Y_AXIS);
		horizontalLayout = mode != BROWSER
			|| DockableWindowManager.TOP.equals(position)
			|| DockableWindowManager.BOTTOM.equals(position);

		toolbarBox = new Box(horizontalLayout
			? BoxLayout.X_AXIS
			: BoxLayout.Y_AXIS);

		topBox.add(toolbarBox);

		GridBagLayout layout = new GridBagLayout();
		pathAndFilterPanel = new JPanel(layout);
		if(isHorizontalLayout())
			pathAndFilterPanel.setBorder(new EmptyBorder(12,12,12,12));

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

		pathField = new HistoryTextField("vfs.browser.path");
		pathField.setName("path");
		pathField.setInstantPopups(true);
		pathField.setEnterAddsToHistory(false);
		pathField.setSelectAllOnFocus(true);
		

		// because its preferred size can be quite wide, we
		// don't want it to make the browser way too big,
		// so set the preferred width to 0.
		Dimension prefSize = pathField.getPreferredSize();
		prefSize.width = 0;
		pathField.setPreferredSize(prefSize);
		pathField.addActionListener(actionHandler);
		cons.gridx = 1;
		cons.weightx = 1.0;
		cons.gridwidth = GridBagConstraints.REMAINDER;

		layout.setConstraints(pathField,cons);
		pathAndFilterPanel.add(pathField);

		filterCheckbox = new JCheckBox(jEdit.getProperty("vfs.browser.filter"));
		filterCheckbox.setMargin(new Insets(0,0,0,0));
//		filterCheckbox.setRequestFocusEnabled(false);
		filterCheckbox.setBorder(new EmptyBorder(0,0,0,12));
		filterCheckbox.setSelected(jEdit.getBooleanProperty(
			"vfs.browser.filter-enabled"));

		filterCheckbox.addActionListener(actionHandler);
		filterCheckbox.setName("filter-checkbox");
		if(mode != CHOOSE_DIRECTORY_DIALOG)
		{
			cons.gridwidth = 1;
			cons.gridx = 0;
			cons.weightx = 0.0;
			cons.gridy = 1;
			layout.setConstraints(filterCheckbox,cons);
			pathAndFilterPanel.add(filterCheckbox);
		}

		filterField = new JComboBox();
		filterEditor = new HistoryComboBoxEditor("vfs.browser.filter");
		filterEditor.setToolTipText(jEdit.getProperty("glob.tooltip"));
		filterEditor.setInstantPopups(true);
		filterEditor.setSelectAllOnFocus(true);
		filterEditor.addActionListener(actionHandler);
		filterField.setName("filter-field");
		String filter;
		if (mode == BROWSER)
		{
			DockableWindowManager dwm = view.getDockableWindowManager();
			KeyListener keyListener = dwm.closeListener(NAME);
			filterCheckbox.addKeyListener(keyListener);	
			addKeyListener(keyListener);
			filterEditor.addKeyListener(keyListener);
			pathField.addKeyListener(keyListener);
			// save the location on close of dockable.
			pathField.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					{
						pathField.setText(VFSBrowser.this.path);
					}
				}
			});
		}

		if(mode == BROWSER || !jEdit.getBooleanProperty(
			"vfs.browser.currentBufferFilter"))
		{
			filter = jEdit.getProperty("vfs.browser.last-filter");
			if(filter == null)
				filter = jEdit.getProperty("vfs.browser.default-filter");
		}
		else
		{
			String ext = MiscUtilities.getFileExtension(
				view.getBuffer().getName());
			if(ext.length() == 0)
				filter = jEdit.getProperty("vfs.browser.default-filter");
			else
				filter = '*' + ext;
		}

		// filterField.getEditor().setItem(new GlobVFSFileFilter(filter));
		// filterField.addItem(filterField.getEditor().getItem());
		filterEditor.setItem(new GlobVFSFileFilter(filter));
		filterField.addItem(filterEditor.getItem());
		filterField.addItemListener(actionHandler);
		filterField.setRenderer(new VFSFileFilterRenderer());

		// loads the registered VFSFileFilter services.
		String[] _filters = ServiceManager.getServiceNames(VFSFileFilter.SERVICE_NAME);
		for (int i = 0; i < _filters.length; i++)
		{
			VFSFileFilter _filter = (VFSFileFilter)
				ServiceManager.getService(VFSFileFilter.SERVICE_NAME, _filters[i]);
			filterField.addItem(_filter);
		}

		if(mode != CHOOSE_DIRECTORY_DIALOG)
		{
			cons.gridwidth = GridBagConstraints.REMAINDER;
			cons.fill = GridBagConstraints.HORIZONTAL;
			cons.gridx = 1;
			cons.weightx = 1.0;
			if (filterField.getItemCount() > 1)
			{
				filterField.setEditor(filterEditor);
				filterField.setEditable(true);
				layout.setConstraints(filterField,cons);
				pathAndFilterPanel.add(filterField);
			}
			else
			{
				layout.setConstraints(filterEditor,cons);
				pathAndFilterPanel.add(filterEditor);
			}
		}

		topBox.add(pathAndFilterPanel);
		add(BorderLayout.NORTH,topBox);

		add(BorderLayout.CENTER,browserView = new BrowserView(this));
		if(isHorizontalLayout())
			browserView.setBorder(new EmptyBorder(0,12,0,12));
		defaultFocusComponent = browserView.getTable();
		propertiesChanged();

		updateFilterEnabled();

		setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
		// see VFSBrowser.browseDirectory()
		if(path == null)
			path = jEdit.getProperty("vfs.browser.path.tmp");

		if(path == null || path.isEmpty())
		{
			String userHome = System.getProperty("user.home");
			String defaultPath = jEdit.getProperty("vfs.browser.defaultPath");
			if("home".equals(defaultPath))
				path = userHome;
			else if("working".equals(defaultPath))
				path = System.getProperty("user.dir");
			else if("buffer".equals(defaultPath))
			{
				Buffer buffer = view.getBuffer();
                boolean browseable = (buffer.getVFS().getCapabilities() & VFS.BROWSE_CAP) != 0;
                if (browseable)
			    	path = buffer.getDirectory();
			}
			else if("last".equals(defaultPath))
			{
				HistoryModel pathModel = HistoryModel.getModel("vfs.browser.path");
				if(pathModel.getSize() == 0)
					path = "~";
				else
					path = pathModel.getItem(0);
			}
			else if("favorites".equals(defaultPath))
				path = "favorites:";

            if (path == null || path.isEmpty())
			{
				// unknown value??!!!
				path = userHome;
			}
		}

		final String _path = path;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				setDirectory(_path);
			}
		});
	} //}}}

	//{{{ focusOnDefaultComponent() method
	public void focusOnDefaultComponent()
	{
		// pathField.requestFocus();		
		defaultFocusComponent.requestFocus();
	} //}}}

	// {{{ setDefaultFocusComponent()
	/** Only used by VFSFileChooserDialog, since it embeds this in a dialog
	 */
	void setDefaultFocusComponent(JComponent c) 
	{
		defaultFocusComponent = c;
	}// }}}
	
	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		jEdit.setBooleanProperty("vfs.browser.filter-enabled",
			filterCheckbox.isSelected());
		if(mode == BROWSER || !jEdit.getBooleanProperty(
			"vfs.browser.currentBufferFilter"))
		{
			VFSFileFilter selectedFilter =
				(VFSFileFilter) filterField.getSelectedItem();
			if (selectedFilter instanceof GlobVFSFileFilter)
				jEdit.setProperty("vfs.browser.last-filter",
					((GlobVFSFileFilter)selectedFilter).getGlob());
		}
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		propertiesChanged();
	} //}}}

	//{{{ handleBufferUpdate() method
	@EBHandler
	public void handleBufferUpdate(BufferUpdate bmsg)
	{
		if (bmsg.getWhat() == BufferUpdate.CREATED ||
			bmsg.getWhat() == BufferUpdate.CLOSED)
		{
			browserView.updateFileView();
		}
	} //}}}

	//{{{ handlePluginUpdate() method
	@EBHandler
	public void handlePluginUpdate(PluginUpdate pmsg)
	{
		if((pmsg.getWhat() == PluginUpdate.LOADED ||
		   pmsg.getWhat() == PluginUpdate.UNLOADED) &&
		   plugins != null /* plugins can be null if the VFSBrowser menu bar is hidden */)
		{
			plugins.updatePopupMenu();
		}
	} //}}}

	//{{{ handleVFSUpdate() method
	@EBHandler
	public void handleVFSUpdate(VFSUpdate msg)
	{
		maybeReloadDirectory(msg.getPath());
	} //}}}

	//{{{ getView() method
	public View getView()
	{
		return view;
	} //}}}

	//{{{ getMode() method
	public int getMode()
	{
		return mode;
	} //}}}

	//{{{ isMultipleSelectionEnabled() method
	public boolean isMultipleSelectionEnabled()
	{
		return multipleSelection;
	} //}}}

	//{{{ isHorizontalLayout() method
	public boolean isHorizontalLayout()
	{
		return horizontalLayout;
	} //}}}

	//{{{ getShowHiddenFiles() method
	public boolean getShowHiddenFiles()
	{
		return showHiddenFiles;
	} //}}}

	//{{{ setShowHiddenFiles() method
	public void setShowHiddenFiles(boolean showHiddenFiles)
	{
		this.showHiddenFiles = showHiddenFiles;
	} //}}}

	//{{{ getFilenameFilter() method
	/**
	 * Returns the file name filter glob.
	 * @since jEdit 3.2pre2
	 * @deprecated Use {@link #getVFSFileFilter()} instead. This method
	 *             might return wrong information since jEdit 4.3pre6.
	 */
	@Deprecated
	public String getFilenameFilter()
	{
		if(filterCheckbox.isSelected())
		{
			String filter = filterField.getSelectedItem().toString();
			if(filter.length() == 0)
				return "*";
			else
				return filter;
		}
		else
			return "*";
	} //}}}

	//{{{ getVFSFileFilter() method
	/**
	 * Returns the currently active VFSFileFilter.
	 *
	 * @since jEdit 4.3pre7
	 */
	public VFSFileFilter getVFSFileFilter()
	{
		if (mode == CHOOSE_DIRECTORY_DIALOG)
			return new DirectoriesOnlyFilter();
		return 	(VFSFileFilter) filterField.getSelectedItem();
	} //}}}

	//{{{ addVFSFileFilter() method
	/**
	 * Adds a file filter to the browser.
	 *
	 * @since jEdit 4.3pre7
	 */
	public void addVFSFileFilter(VFSFileFilter filter)
	{
		filterField.addItem(filter);
		if (filterField.getItemCount() == 2)
		{
			filterField.setEditor(filterEditor);
			filterField.setEditable(true);

			GridBagLayout layout = (GridBagLayout) pathAndFilterPanel.getLayout();
			GridBagConstraints cons =layout.getConstraints(filterEditor);
			cons.gridwidth = GridBagConstraints.REMAINDER;
			cons.fill = GridBagConstraints.HORIZONTAL;
			cons.gridx = 1;
			cons.weightx = 1;

			pathAndFilterPanel.remove(filterEditor);
			layout.setConstraints(filterField, cons);
			pathAndFilterPanel.add(filterField);
			pathAndFilterPanel.validate();
			pathAndFilterPanel.repaint();
		}
	} //}}}

	//{{{ setFilenameFilter() method
	public void setFilenameFilter(String filter)
	{
		if(filter == null || filter.length() == 0 || "*".equals(filter))
			filterCheckbox.setSelected(false);
		else
		{
			filterCheckbox.setSelected(true);
			filterEditor.setItem(new GlobVFSFileFilter(filter));
		}
	} //}}}

	//{{{ getDirectoryField() method
	public HistoryTextField getDirectoryField()
	{
		return pathField;
	} //}}}

	//{{{ getDirectory() method
	public String getDirectory()
	{
		return path;
	} //}}}

	// {{{ Directory Stack operations
	/**
	 * @since jedit 4.3pre15
	 */
	public void previousDirectory() 
	{
		if (historyStack.size() > 1)
		{
			historyStack.pop();
			nextDirectoryStack.push(path);
			setDirectory(historyStack.peek());
			historyStack.pop();
		}
	}
	
	
	/**
	 * @since jEdit 4.3pre15
	 */
	public void nextDirectory() 
	{
		if (!nextDirectoryStack.isEmpty())
		{
			setDirectory(nextDirectoryStack.pop());
		}
	}
	// }}}	
	
	//{{{ setDirectory() method
	public void setDirectory(String path)
	{
		if(path.startsWith("file:"))
			path = path.substring(5);
		path = MiscUtilities.expandVariables(path);
		pathField.setText(path);

		if(!startRequest())
			return;

		historyStack.push(path);
		browserView.saveExpansionState();
		Runnable delayedAWTRequest = new Runnable()
		{
			public void run()
			{
				endRequest();
			}
		};
		browserView.loadDirectory(null,path,true, delayedAWTRequest);
		this.path = path;
	} //}}}

	//{{{ getRootDirectory() method
	public static String getRootDirectory()
	{
		if(OperatingSystem.isMacOS() || OperatingSystem.isDOSDerived())
			return FileRootsVFS.PROTOCOL + ':';
		else
			return "/";
	} //}}}

	//{{{ rootDirectory() method
	/**
	 * Goes to the local drives directory.
	 * @since jEdit 4.0pre4
	 */
	public void rootDirectory()
	{
		setDirectory(getRootDirectory());
	} //}}}

	//{{{ reloadDirectory() method
	public void reloadDirectory()
	{
		// used by FTP plugin to clear directory cache
		VFSManager.getVFSForPath(path).reloadDirectory(path);

		browserView.saveExpansionState();
		browserView.loadDirectory(null,path,false);
	} //}}}

	//{{{ delete() method
	/**
	 * Note that all files must be on the same VFS.
	 * @since jEdit 4.3pre2
	 */
	public void delete(VFSFile[] files)
	{
		String dialogType;

		if(MiscUtilities.isURL(files[0].getDeletePath())
			&& FavoritesVFS.PROTOCOL.equals(
			MiscUtilities.getProtocolOfURL(files[0].getDeletePath())))
		{
			dialogType = "vfs.browser.delete-favorites";
		}
		else
		{
			dialogType = "vfs.browser.delete-confirm";
		}

		StringBuilder buf = new StringBuilder();
		String typeStr = "files";
		for(int i = 0; i < files.length; i++)
		{
			buf.append(files[i].getPath());
			buf.append('\n');
			if (files[i].getType() == VFSFile.DIRECTORY)
				typeStr = "directories and their contents";
		}

		Object[] args = { buf.toString(), typeStr};
		
		int result = GUIUtilities.confirm(this,dialogType,args,
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(result != JOptionPane.YES_OPTION)
			return;

		VFS vfs = VFSManager.getVFSForPath(files[0].getDeletePath());

		if(!startRequest())
			return;

		for(int i = 0; i < files.length; i++)
		{
			Object session = vfs.createVFSSession(files[i].getDeletePath(),this);
			if(session == null)
				continue;

			VFSManager.runInWorkThread(new BrowserIORequest(
				BrowserIORequest.DELETE,this,
				session,vfs,files[i].getDeletePath(),
				null,null));
		}

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				endRequest();
			}
		});
	} //}}}

	//{{{ rename() method
	public void rename(String from)
	{
		VFS vfs = VFSManager.getVFSForPath(from);

		String filename = vfs.getFileName(from);
		String[] args = { filename };
		String to = GUIUtilities.input(this,"vfs.browser.rename",
			args,filename);
		if(to == null)
			return;

		to = MiscUtilities.constructPath(vfs.getParentOfPath(from),to);

		if (to.equals(from))
			return;

		Object session = vfs.createVFSSession(from,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.RENAME,this,
			session,vfs,from,to,null));

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				endRequest();
			}
		});
	} //}}}

	//{{{ rename() method
	public void rename(String from, String newname)
	{
		VFS vfs = VFSManager.getVFSForPath(from);

		String filename = vfs.getFileName(from);
		String to = newname;
		
		if(to == null || filename.equals(newname))
			return;

		to = MiscUtilities.constructPath(vfs.getParentOfPath(from),to);

		Object session = vfs.createVFSSession(from,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.RENAME,this,
			session,vfs,from,to,null));

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				endRequest();
			}
		});
	} //}}}		

	//{{{ mkdir() method
	public void mkdir()
	{
		String newDirectory = GUIUtilities.input(this,"vfs.browser.mkdir",null);
		if(newDirectory == null)
			return;

		// if a directory is selected, create new dir in there.
		// if a file is selected, create new dir inside its parent.
		final VFSFile[] selected = getSelectedFiles();
		String parent;
		if(selected.length == 0)
			parent = path;
		else if(selected[0].getType() == VFSFile.FILE)
		{
			parent = selected[0].getPath();
			parent = VFSManager.getVFSForPath(parent)
				.getParentOfPath(parent);
		}
		else
			parent = selected[0].getPath();

		VFS vfs = VFSManager.getVFSForPath(parent);

		// path is the currently viewed directory in the browser
		newDirectory = MiscUtilities.constructPath(parent,newDirectory);

		Object session = vfs.createVFSSession(newDirectory,this);
		if(session == null)
			return;

		if(!startRequest())
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.MKDIR,this,
			session,vfs,newDirectory,null,null));

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				endRequest();
				if (selected.length != 0 && selected[0].getType() != VFSFile.FILE)
				{
					VFSDirectoryEntryTable directoryEntryTable = browserView.getTable();
					int selectedRow = directoryEntryTable.getSelectedRow();
					VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel) directoryEntryTable.getModel();
					VFSDirectoryEntryTableModel.Entry entry = model.files[selectedRow];
					if (!entry.expanded)
					{
						browserView.clearExpansionState();
						browserView.loadDirectory(entry,entry.dirEntry.getPath(),
							false);
					}
				}
			}
		});
	} //}}}

	//{{{ newFile() method
	/**
	 * Creates a new file in the current directory.
	 * @since jEdit 4.0pre2
	 */
	public void newFile()
	{
		VFSFile[] selected = getSelectedFiles();
		if(selected.length >= 1)
		{
			VFSFile file = selected[0];
			if(file.getType() == VFSFile.DIRECTORY)
				jEdit.newFile(view,file.getPath());
			else
			{
				VFS vfs = VFSManager.getVFSForPath(file.getPath());
				jEdit.newFile(view,vfs.getParentOfPath(file.getPath()));
			}
		}
		else
			jEdit.newFile(view,path);
	} //}}}

	//{{{ fileProperties() method
	/**
	 * Show selected file's properties.
	 */
	public void fileProperties(VFSFile[] files)
	{
		new FilePropertiesDialog(view, this, files);
	} //}}} 		
		
	//{{{ searchInDirectory() method
	/**
	 * Opens a directory search in the current directory.
	 * @since jEdit 4.0pre2
	 */
	public void searchInDirectory()
	{
		VFSFile[] selected = getSelectedFiles();
		if(selected.length >= 1)
		{
			VFSFile file = selected[0];
			searchInDirectory(file.getPath(),file.getType() != VFSFile.FILE);
		}
		else
		{
			searchInDirectory(path,true);
		}
	} //}}}

	//{{{ searchInDirectory() method
	/**
	 * Opens a directory search in the specified directory.
	 * @param path The path name
	 * @param directory True if the path is a directory, false if it is a file
	 * @since jEdit 4.2pre1
	 */
	public void searchInDirectory(String path, boolean directory)
	{
		String filter;
		VFSFileFilter vfsff = getVFSFileFilter();
		if (vfsff instanceof GlobVFSFileFilter)
			filter = ((GlobVFSFileFilter)vfsff).getGlob();
		else
			filter = "*";

		if (!directory)
		{
			String name = MiscUtilities.getFileName(path);
			String ext = MiscUtilities.getFileExtension(name);
			filter = ext == null || ext.length() == 0
				? filter : '*' + ext;
			path = MiscUtilities.getParentOfPath(path);
		}

		SearchAndReplace.setSearchFileSet(new DirectoryListSet(
			path,filter,true));
		SearchDialog.showSearchDialog(view,null,SearchDialog.DIRECTORY);
	} //}}}

	//{{{ getBrowserView() method
	BrowserView getBrowserView()
	{
		return browserView;
	} //}}}

	//{{{ getSelectedFiles() method
	/**
	 * Return the selected files in the lower browser tree.
	 * @since jEdit 4.3pre2
	 */
	public VFSFile[] getSelectedFiles()
	{
		return browserView.getSelectedFiles();
	} //}}}

	//{{{ getSelectedFiles() method
	/**
	 * Return the selected files from the point of view of the
	 * given component. This may be the selected directory from the
	 * upper tree component of the browser (directory tree) or
	 * the selected files in the bottom tree component.
	 * This method is to be used by code running inside VFSBrowser
	 * such as a DynamicMenuProvider. Use the other method otherwise.
	 * The main difference is this function searches the component
	 * hierarchy for a {@link BrowserView.ParentDirectoryList} to get
	 * the list of currently selected files from there. Otherwise, it
	 * returns what {@link #getSelectedFiles()} would return.
	 * @param source the source component to start from when
	 * 		navigating the component hierarchy
	 * @since jEdit 4.4pre1
	 */
	public VFSFile[] getSelectedFiles(Component source)
	{
		if(GUIUtilities.getComponentParent(source, BrowserView.ParentDirectoryList.class)
			!= null)
		{
			Object[] selected = getBrowserView()
				.getParentDirectoryList()
				.getSelectedValues();
			VFSFile[] returnValue = new VFSFile[
				selected.length];
			System.arraycopy(selected,0,returnValue,0,
				selected.length);
			return returnValue;
		}
		else
		{
			return getSelectedFiles();
		}
	} //}}}

	//{{{ locateFile() method
	/**
	 * Goes to the given file's directory and selects the file in the list.
	 * @param path The file
	 * @since jEdit 4.2pre2
	 */
	public void locateFile(final String path)
	{
		VFSFileFilter filter = getVFSFileFilter();
		if(!filter.accept(MiscUtilities.getFileName(path)))
			setFilenameFilter(null);

		setDirectory(MiscUtilities.getParentOfPath(path));
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				browserView.getTable().selectFile(path);
			}
		});
	} //}}}

	//{{{ createPluginsMenu() method
	public JComponent createPluginsMenu(JComponent pluginMenu, boolean showManagerOptions)
	{
		ActionHandler actionHandler = new ActionHandler();
		if(showManagerOptions && getMode() == BROWSER)
		{
			pluginMenu.add(GUIUtilities.loadMenuItem("plugin-manager",false));
			pluginMenu.add(GUIUtilities.loadMenuItem("plugin-options",false));
			if (pluginMenu instanceof JMenu)
				((JMenu)pluginMenu).addSeparator();
			else if (pluginMenu instanceof JPopupMenu)
				((JPopupMenu)pluginMenu).addSeparator();

		}
		else
			/* we're in a modal dialog */;

		List<JMenuItem> vec = new ArrayList<JMenuItem>();

		//{{{ old API
		Enumeration<VFS> e = VFSManager.getFilesystems();

		while(e.hasMoreElements())
		{
			VFS vfs = e.nextElement();
			if((vfs.getCapabilities() & VFS.BROWSE_CAP) == 0)
				continue;

			JMenuItem menuItem = new JMenuItem(jEdit.getProperty(
					"vfs." + vfs.getName() + ".label"));
			menuItem.setActionCommand(vfs.getName());
			menuItem.addActionListener(actionHandler);
			vec.add(menuItem);
		} //}}}

		//{{{ new API
		EditPlugin[] plugins = jEdit.getPlugins();
		for (int i = 0; i < plugins.length; i++)
		{
			JMenuItem menuItem = plugins[i].createBrowserMenuItems();
			if(menuItem != null)
				vec.add(menuItem);
		} //}}}

		if (!vec.isEmpty())
		{
			Collections.sort(vec,new MenuItemTextComparator());
			for(int i = 0; i < vec.size(); i++)
				pluginMenu.add(vec.get(i));
		}
		else
		{
			JMenuItem mi = new JMenuItem(jEdit.getProperty(
					"vfs.browser.plugins.no-plugins.label"));
			mi.setEnabled(false);
			pluginMenu.add(mi);
		}

		return pluginMenu;
	} //}}}

	//{{{ addBrowserListener() method
	public void addBrowserListener(BrowserListener l)
	{
		listenerList.add(BrowserListener.class,l);
	} //}}}

	//{{{ removeBrowserListener() method
	public void removeBrowserListener(BrowserListener l)
	{
		listenerList.remove(BrowserListener.class,l);
	} //}}}

	//{{{ filesActivated() method
	// canDoubleClickClose set to false when ENTER pressed
	public static final int M_OPEN = 0;
	public static final int M_OPEN_NEW_VIEW = 1;
	public static final int M_OPEN_NEW_PLAIN_VIEW = 2;
	public static final int M_OPEN_NEW_SPLIT = 3;
	public static final int M_INSERT = 4;

	/**
	 * This method does the "double-click" handling. It is public so that
	 * <code>browser.actions.xml</code> can bind to it.
	 * @since jEdit 4.2pre2
	 */
	public void filesActivated(int mode, boolean canDoubleClickClose)
	{
		VFSFile[] selectedFiles = browserView.getSelectedFiles();

		Buffer buffer = null;

check_selected: for(int i = 0; i < selectedFiles.length; i++)
		{
			VFSFile file = selectedFiles[i];

			if(file.getType() == VFSFile.DIRECTORY
				|| file.getType() == VFSFile.FILESYSTEM)
			{
				if(mode == M_OPEN_NEW_VIEW && this.mode == BROWSER)
					browseDirectoryInNewWindow(view,file.getPath());
				else
					setDirectory(file.getPath());
			}
			else if(this.mode == BROWSER || this.mode == BROWSER_DIALOG)
			{
				if(mode == M_INSERT)
				{
					view.getBuffer().insertFile(view,
						file.getPath());
					continue check_selected;
				}

				Buffer _buffer = jEdit.getBuffer(file.getPath());
				if(_buffer == null)
				{
					Hashtable<String, Object> props = new Hashtable<String, Object>();
					if(currentEncoding != null)
					{
						props.put(JEditBuffer.ENCODING,currentEncoding);
					}
					props.put(Buffer.ENCODING_AUTODETECT,
						autoDetectEncoding);
					_buffer = jEdit.openFile(view, null,
						file.getPath(),false,props);
				}
				else if(doubleClickClose && canDoubleClickClose
					&& this.mode != BROWSER_DIALOG
					&& selectedFiles.length == 1)
				{
					// close if this buffer is currently
					// visible in the view.
					EditPane[] editPanes = view.getEditPanes();
					for(int j = 0; j < editPanes.length; j++)
					{
						if(editPanes[j].getBuffer() == _buffer)
						{
							jEdit.closeBuffer(view,_buffer);
							return;
						}
					}
				}

				if(_buffer != null)
					buffer = _buffer;
			}
			else
			{
				// if a file is selected in OPEN_DIALOG or
				// SAVE_DIALOG mode, just let the listener(s)
				// handle it
			}
		}

		if(buffer != null)
		{
			switch(mode)
			{
			case M_OPEN:
				view.setBuffer(buffer);
				break;
			case M_OPEN_NEW_VIEW:
				jEdit.newView(view,buffer,false);
				break;
			case M_OPEN_NEW_PLAIN_VIEW:
				jEdit.newView(view,buffer,true);
				break;
			case M_OPEN_NEW_SPLIT:
				view.splitHorizontally().setBuffer(buffer);
				break;
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
	} //}}}

	//{{{ dispose() method
	/** Disposes the browser, regardless of whether it is a dialog or a dockable
	*/
	public void dispose() {	
		if (this.mode == BROWSER) {
			view.getDockableWindowManager().hideDockableWindow(NAME);			
		}
		else {
			GUIUtilities.getParentDialog(this).dispose();
		}	
	}//}}}

	//{{{ move() method
	public void move(String newPosition)
	{
		boolean horz = mode != BROWSER
				|| DockableWindowManager.TOP.equals(newPosition)
				|| DockableWindowManager.BOTTOM.equals(newPosition);
		if (horz == horizontalLayout)
			return;
		horizontalLayout = horz;
		topBox.remove(toolbarBox);
		toolbarBox = new Box(horizontalLayout
				? BoxLayout.X_AXIS
				: BoxLayout.Y_AXIS);
		topBox.add(toolbarBox, 0);
		propertiesChanged();
	} //}}}
	
	//{{{ Package-private members

	// This can be null untill an user explicitly selects an encoding
	// so that this don't overwrite more accurate encoding information
	// like buffer histories.
	String currentEncoding;

	boolean autoDetectEncoding;

	//{{{ directoryLoaded() method
	void directoryLoaded(Object node, Object[] loadInfo,
		boolean addToHistory)
	{
		String path = (String)loadInfo[0];
		if(path == null)
		{
			// there was an error
			return;
		}

		VFSFile[] list = (VFSFile[])loadInfo[1];

		if(node == null)
		{
			// This is the new, canonical path
			VFSBrowser.this.path = path;
			if(!pathField.getText().equals(path))
				pathField.setText(path);
			if(path.endsWith("/") ||
				path.endsWith(File.separator))
			{
				// ensure consistent history;
				// eg we don't want both
				// foo/ and foo
				path = path.substring(0,
					path.length() - 1);
			}

			if(addToHistory)
			{
				HistoryModel.getModel("vfs.browser.path")
					.addItem(path);
			}
		}

		boolean filterEnabled = filterCheckbox.isSelected();

		List<VFSFile> directoryList = new ArrayList<VFSFile>();

		int directories = 0;
		int files = 0;
		int invisible = 0;

		if(list != null)
		{
			VFSFileFilter filter = getVFSFileFilter();

			for(int i = 0; i < list.length; i++)
			{
				VFSFile file = list[i];
				if(file.isHidden() && !showHiddenFiles)
				{
					invisible++;
					continue;
				}

				if (filter != null && (filterEnabled || filter instanceof DirectoriesOnlyFilter)
				    && !filter.accept(file))
				{
					invisible++;
					continue;
				}

				if(file.getType() == VFSFile.FILE)
					files++;
				else
					directories++;

				directoryList.add(file);
			}

			Collections.sort(directoryList,
				new VFS.DirectoryEntryCompare(
				sortMixFilesAndDirs,
				sortIgnoreCase));
		}

		browserView.directoryLoaded(node,path,
			directoryList);

		// to notify listeners that any existing
		// selection has been deactivated

		// turns out under some circumstances this
		// method can switch the current buffer in
		// BROWSER mode.

		// in any case, this is only needed for the
		// directory chooser (why?), so we add a
		// check. otherwise poor Rick will go insane.
		if(mode == CHOOSE_DIRECTORY_DIALOG)
			filesSelected();
	} //}}}

	//{{{ filesSelected() method
	void filesSelected()
	{
		VFSFile[] selectedFiles = browserView.getSelectedFiles();

		if(mode == BROWSER)
		{
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFSFile file = selectedFiles[i];
				Buffer buffer = jEdit.getBuffer(file.getPath());
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
	} //}}}

	//{{{ endRequest() method
	void endRequest()
	{
		requestRunning = false;
	} //}}}

	//}}}

	//{{{ Private members

	private static final ActionContext actionContext;

	static
	{
		actionContext = new BrowserActionContext();

		ActionSet builtInActionSet = new ActionSet(null,null,null,
			jEdit.class.getResource("browser.actions.xml"));
		builtInActionSet.setLabel(jEdit.getProperty("action-set.browser"));
		builtInActionSet.load();
		actionContext.addActionSet(builtInActionSet);
	}

	//{{{ Instance variables
	private EventListenerList listenerList;
	private View view;
	private boolean horizontalLayout;
	private String path;
	private JPanel pathAndFilterPanel;
	private HistoryTextField pathField;
	private JComponent defaultFocusComponent;
	private JCheckBox filterCheckbox;
	private HistoryComboBoxEditor filterEditor;
	private JComboBox filterField;
	private Box toolbarBox;
	private Box topBox;
	private FavoritesMenuButton favorites;
	private PluginsMenuButton plugins;
	private BrowserView browserView;
	private int mode;
	private boolean multipleSelection;

	private boolean showHiddenFiles;
	private boolean sortMixFilesAndDirs;
	private boolean sortIgnoreCase;
	private boolean doubleClickClose;

	private boolean requestRunning;
	private boolean maybeReloadRequestRunning;
	
	private final Stack<String> historyStack = new Stack<String>();
	private final Stack<String> nextDirectoryStack = new Stack<String>();
	//}}}

	//{{{ createMenuBar() method
	private Container createMenuBar()
	{
		JToolBar menuBar = new JToolBar();
		menuBar.setFloatable(false);

		menuBar.add(new CommandsMenuButton());
		menuBar.add(Box.createHorizontalStrut(3));
		menuBar.add(plugins = new PluginsMenuButton());
		menuBar.add(Box.createHorizontalStrut(3));
		menuBar.add(favorites = new FavoritesMenuButton());

		return menuBar;
	} //}}}

	//{{{ createToolBar() method
	private Container createToolBar()
	{
		if(mode == BROWSER)
			return GUIUtilities.loadToolBar(actionContext,
				"vfs.browser.toolbar-browser");
		else
			return GUIUtilities.loadToolBar(actionContext,
				"vfs.browser.toolbar-dialog");
	} //}}}

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		showHiddenFiles = jEdit.getBooleanProperty("vfs.browser.showHiddenFiles");
		sortMixFilesAndDirs = jEdit.getBooleanProperty("vfs.browser.sortMixFilesAndDirs");
		sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");
		doubleClickClose = jEdit.getBooleanProperty("vfs.browser.doubleClickClose");

		browserView.propertiesChanged();

		toolbarBox.removeAll();

		if(jEdit.getBooleanProperty("vfs.browser.showToolbar"))
		{
			Container toolbar = createToolBar();
			toolbarBox.add(toolbar);
		}

		if(jEdit.getBooleanProperty("vfs.browser.showMenubar"))
		{
			Container menubar = createMenuBar();
			if(horizontalLayout)
			{
				toolbarBox.add(menubar,0);
			}
			else
			{
				menubar.add(Box.createGlue());
				toolbarBox.add(menubar);
			}
		}
		else
		{
			plugins = null;
			favorites = null;
		}

		revalidate();

		if(path != null)
			reloadDirectory();
	} //}}}

	/* We do this stuff because the browser is not able to handle
	 * more than one request yet */

	//{{{ startRequest() method
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
	} //}}}

	//{{{ updateFilterEnabled() method
	private void updateFilterEnabled()
	{
		filterField.setEnabled(filterCheckbox.isSelected());
		filterEditor.setEnabled(filterCheckbox.isSelected());
	} //}}}

	//{{{ maybeReloadDirectory() method
	private void maybeReloadDirectory(String dir)
	{
		if(MiscUtilities.isURL(dir)
			&& MiscUtilities.getProtocolOfURL(dir).equals(
			FavoritesVFS.PROTOCOL))
		{
			if(favorites != null)
				favorites.popup = null;
		}

		// this is a dirty hack and it relies on the fact
		// that updates for parents are sent before updates
		// for the changed nodes themselves (if this was not
		// the case, the browser wouldn't be updated properly
		// on delete, etc).
		//
		// to avoid causing '> 1 request' errors, don't reload
		// directory if request already active
		if(maybeReloadRequestRunning)
		{
			//Log.log(Log.WARNING,this,"VFS update: request already in progress");
			return;
		}

		// save a file -> sends vfs update. if a VFS file dialog box
		// is shown from the same event frame as the save, the
		// VFSUpdate will be delivered before the directory is loaded,
		// and before the path is set.
		if(path != null)
		{
			try
			{
				maybeReloadRequestRunning = true;

				browserView.maybeReloadDirectory(dir);
			}
			finally
			{
				VFSManager.runInAWTThread(new Runnable()
				{
					public void run()
					{
						maybeReloadRequestRunning = false;
					}
				});
			}
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener, ItemListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if (isProcessingEvent)
				return;

			Object source = evt.getSource();

			if (source == pathField
			    || source == filterCheckbox)
			{
				isProcessingEvent = true;
				resetLater();

				updateFilterEnabled();

				String p = pathField.getText();
				
				if(p != null)
					setDirectory(p);
				browserView.focusOnFileView();
			}

			else if (source == filterField.getEditor())
			{
				// force the editor to refresh.
				filterField.getEditor().setItem(
					filterField.getEditor().getItem());
			}

			// depending on Swing look & feel, filterField.getEditor()
			// returns some ComboBoxUI
			else if (source == filterEditor)
			{
				// force the editor to refresh.
				filterEditor.setItem(
					filterEditor.getItem());
				filterField.setSelectedItem(
					filterEditor.getItem());
				// ### ugly: 
				// itemStateChanged does not seem to get fired
				itemStateChanged(new ItemEvent(filterField,
					ItemEvent.ITEM_STATE_CHANGED,
					filterEditor.getItem(),
					ItemEvent.SELECTED));
			}
		}

		public void itemStateChanged(ItemEvent e)
		{
			if (isProcessingEvent)
				return;

			if (e.getStateChange() != ItemEvent.SELECTED)
				return;

			isProcessingEvent = true;
			resetLater();

			filterField.setEditable(e.getItem() instanceof GlobVFSFileFilter);
			updateFilterEnabled();
			String path = pathField.getText();
			if(path != null)
				setDirectory(path);

			browserView.focusOnFileView();
		}

		/**
		 * Why this method exists: since both actionPerformed()
		 * and itemStateChanged() above can change the combo box,
		 * executing one of them can cause a chain reaction causing
		 * the other method to be called. This would cause the
		 * VFS subsystem to be called several times, which would
		 * cause a warning to show up if the first operation is
		 * still in progress, or cause a second operation to happen
		 * which is not really wanted especially if we're talking
		 * about a remove VFS. So the methods set a flag saying
		 * that something is going on, and this method resets
		 * the flag after the AWT thread is done with the
		 * current events.
		 */
		private void resetLater()
		{
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						isProcessingEvent = false;
					}
				}
			);
		}

		private boolean isProcessingEvent;

	} //}}}

	//{{{ CommandsMenuButton class
	class CommandsMenuButton extends RolloverButton
	{
		//{{{ CommandsMenuButton constructor
		CommandsMenuButton()
		{
			setText(jEdit.getProperty("vfs.browser.commands.label"));
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty("dropdown-arrow.icon")));
			setHorizontalTextPosition(SwingConstants.LEADING);
			setName("commands");
			
			popup = new BrowserCommandsMenu(VFSBrowser.this,null);

			CommandsMenuButton.this.setRequestFocusEnabled(false);
			setMargin(new Insets(1,1,1,1));
			CommandsMenuButton.this.addMouseListener(new MouseHandler());

			if(OperatingSystem.isMacOSLF())
				CommandsMenuButton.this.putClientProperty("JButton.buttonType","toolbar");
		} //}}}

		BrowserCommandsMenu popup;

		//{{{ MouseHandler class
		class MouseHandler extends MouseAdapter
		{
			@Override
			public void mousePressed(MouseEvent evt)
			{
				if(!popup.isVisible())
				{
					popup.update();

					GUIUtilities.showPopupMenu(
						popup,CommandsMenuButton.this,0,
						CommandsMenuButton.this.getHeight(),
						false);
				}
				else
				{
					popup.setVisible(false);
				}
			}
		} //}}}
	} //}}}

	//{{{ PluginsMenuButton class
	class PluginsMenuButton extends RolloverButton
	{
		//{{{ PluginsMenuButton constructor
		PluginsMenuButton()
		{
			setText(jEdit.getProperty("vfs.browser.plugins.label"));
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty("dropdown-arrow.icon")));
			setHorizontalTextPosition(SwingConstants.LEADING);
			setName("plugins");
			
			PluginsMenuButton.this.setRequestFocusEnabled(false);
			setMargin(new Insets(1,1,1,1));
			PluginsMenuButton.this.addMouseListener(new MouseHandler());

			if(OperatingSystem.isMacOSLF())
				PluginsMenuButton.this.putClientProperty("JButton.buttonType","toolbar");
		} //}}}

		JPopupMenu popup;

		//{{{ updatePopupMenu() method
		void updatePopupMenu()
		{
			popup = null;
		} //}}}

		//{{{ createPopupMenu() method
		private void createPopupMenu()
		{
			if(popup != null)
				return;

			popup = (JPopupMenu)createPluginsMenu(new JPopupMenu(),true);
		} //}}}

		//{{{ MouseHandler class
		class MouseHandler extends MouseAdapter
		{
			@Override
			public void mousePressed(MouseEvent evt)
			{
				createPopupMenu();

				if(!popup.isVisible())
				{
					GUIUtilities.showPopupMenu(
						popup,PluginsMenuButton.this,0,
						PluginsMenuButton.this.getHeight(),
						false);
				}
				else
				{
					popup.setVisible(false);
				}
			}
		} //}}}
	} //}}}

	//{{{ FavoritesMenuButton class
	class FavoritesMenuButton extends RolloverButton
	{
		//{{{ FavoritesMenuButton constructor
		FavoritesMenuButton()
		{
			setText(jEdit.getProperty("vfs.browser.favorites.label"));
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty("dropdown-arrow.icon")));
			setHorizontalTextPosition(SwingConstants.LEADING);
			setName("favorites");
			
			FavoritesMenuButton.this.setRequestFocusEnabled(false);
			setMargin(new Insets(1,1,1,1));
			FavoritesMenuButton.this.addMouseListener(new MouseHandler());

			if(OperatingSystem.isMacOSLF())
				FavoritesMenuButton.this.putClientProperty("JButton.buttonType","toolbar");
		} //}}}

		JPopupMenu popup;

		//{{{ createPopupMenu() method
		void createPopupMenu()
		{
			popup = new JPopupMenu();
			ActionHandler actionHandler = new ActionHandler();

			JMenuItem mi = new JMenuItem(
				jEdit.getProperty(
				"vfs.browser.favorites"
				+ ".add-to-favorites.label"));
			mi.setActionCommand("add-to-favorites");
			mi.addActionListener(actionHandler);
			popup.add(mi);

			mi = new JMenuItem(
				jEdit.getProperty(
				"vfs.browser.favorites"
				+ ".edit-favorites.label"));
			mi.setActionCommand("dir@favorites:");
			mi.addActionListener(actionHandler);
			popup.add(mi);

			popup.addSeparator();

			VFSFile[] favorites = FavoritesVFS.getFavorites();
			if(favorites.length == 0)
			{
				mi = new JMenuItem(
					jEdit.getProperty(
					"vfs.browser.favorites"
					+ ".no-favorites.label"));
				mi.setEnabled(false);
				popup.add(mi);
			}
			else
			{
				Arrays.sort(favorites,
					new VFS.DirectoryEntryCompare(
					sortMixFilesAndDirs,
					sortIgnoreCase));
				for(int i = 0; i < favorites.length; i++)
				{
					VFSFile favorite = favorites[i];
					mi = new JMenuItem(favorite.getPath());
					mi.setIcon(FileCellRenderer
						.getIconForFile(
						favorite,false));
					String cmd = (favorite.getType() ==
						VFSFile.FILE
						? "file@" : "dir@")
						+ favorite.getPath();
					mi.setActionCommand(cmd);
					mi.addActionListener(actionHandler);
					popup.add(mi);
				}
			}
		} //}}}

		//{{{ ActionHandler class
		class ActionHandler implements ActionListener
		{
			public void actionPerformed(ActionEvent evt)
			{
				String actionCommand = evt.getActionCommand();
				if("add-to-favorites".equals(actionCommand))
				{
					// if any directories are selected, add
					// them, otherwise add current directory
					VFSFile[] selected = getSelectedFiles();
					if(selected == null || selected.length == 0)
					{
						if(path.equals(FavoritesVFS.PROTOCOL + ':'))
						{
							GUIUtilities.error(VFSBrowser.this,
								"vfs.browser.recurse-favorites",
								null);
						}
						else
						{
							FavoritesVFS.addToFavorites(path,
								VFSFile.DIRECTORY);
						}
					}
					else
					{
						for(int i = 0; i < selected.length; i++)
						{
							VFSFile file = selected[i];
							FavoritesVFS.addToFavorites(file.getPath(),
								file.getType());
						}
					}
				}
				else if(actionCommand.startsWith("dir@"))
				{
					setDirectory(actionCommand.substring(4));
				}
				else if(actionCommand.startsWith("file@"))
				{
					switch(getMode())
					{
					case BROWSER:
						jEdit.openFile(view,actionCommand.substring(5));
						break;
					default:
						locateFile(actionCommand.substring(5));
						break;
					}
				}
			}
		} //}}}

		//{{{ MouseHandler class
		class MouseHandler extends MouseAdapter
		{
			@Override
			public void mousePressed(MouseEvent evt)
			{
				if(popup != null && popup.isVisible())
				{
					popup.setVisible(false);
					return;
				}

				if(popup == null)
					createPopupMenu();

				GUIUtilities.showPopupMenu(
					popup,FavoritesMenuButton.this,0,
					FavoritesMenuButton.this.getHeight(),
					false);
			}
		} //}}}
	} //}}}

	//{{{ BrowserActionContext class
	static class BrowserActionContext extends ActionContext
	{
		@Override
		public void invokeAction(EventObject evt, EditAction action)
		{
			Component source = (Component)evt.getSource();
			VFSBrowser browser = (VFSBrowser)
				GUIUtilities.getComponentParent(
				source,
				VFSBrowser.class);

			VFSFile[] files = browser.getSelectedFiles(source);

			// in the future we will want something better,
			// eg. having an 'evt' object passed to
			// EditAction.invoke().

			// for now, since all browser actions are
			// written in beanshell we set the 'browser'
			// variable directly.
			NameSpace global = BeanShell.getNameSpace();
			try
			{
				global.setVariable("browser",browser);
				global.setVariable("files",files);

				View view = browser.getView();
				// I guess ideally all browsers
				// should have views, but since they
				// don't, we just use the active view
				// in that case, since some actions
				// depend on a view being there and
				// I don't want to add checks to
				// them all
				if(view == null)
					view = jEdit.getActiveView();
				action.invoke(view);
			}
			catch(UtilEvalError err)
			{
				Log.log(Log.ERROR,this,err);
			}
			finally
			{
				try
				{
					global.setVariable("browser",null);
					global.setVariable("files",null);
				}
				catch(UtilEvalError err)
				{
					Log.log(Log.ERROR,this,err);
				}
			}
		}
	} //}}}

	//{{{ HistoryComboBoxEditor class
	private static class HistoryComboBoxEditor
				extends HistoryTextField
				implements ComboBoxEditor
	{

		HistoryComboBoxEditor(String key)
		{
			super(key);
		}

		public Object getItem()
		{
			if (current == null)
			{
				current = new GlobVFSFileFilter(getText());
			}

			if (!current.getGlob().equals(getText()))
			{
				current.setGlob(getText());
			}

			return current;
		}

		public void setItem(Object item)
		{
			if (item == current)
			{
				// if we keep the same object, swing
				// will cause an event to be fired
				// on the default button of the dialog,
				// causing a beep since no file is
				// selected...
				if (item != null)
				{
					GlobVFSFileFilter filter = (GlobVFSFileFilter) item;
					current = new GlobVFSFileFilter(filter.getGlob());
					setText(current.getGlob());
				}
				return;
			}

			if (item != null)
			{
				// this happens when changing the selected item
				// in the combo; the combo has not yet fired an
				// itemStateChanged() event, so it's not put into
				// non-editable mode by the handler above.
				if (!(item instanceof GlobVFSFileFilter))
					return;

				GlobVFSFileFilter filter = (GlobVFSFileFilter) item;
				filter = new GlobVFSFileFilter(filter.getGlob());
				setText(filter.getGlob());
				addCurrentToHistory();
				current = filter;
			}
			else
			{
				setText("*");
				current = new GlobVFSFileFilter("*");
			}
		}

		@Override
		protected void processFocusEvent(FocusEvent e)
		{
			// AWT will call setItem() when the editor loses
			// focus; that can cause weird and unwanted things
			// to happen, so ignore lost focus events.
			if (e.getID() != FocusEvent.FOCUS_LOST)
				super.processFocusEvent(e);
			else 
			{
				setCaretPosition(0);
				getCaret().setVisible(false);
			}
		}

		public Component getEditorComponent()
		{
			return this;
		}

		private GlobVFSFileFilter current;

	} //}}}

	//{{{ VFSFileFilterRenderer class
	private static class VFSFileFilterRenderer extends DefaultListCellRenderer
	{

		@Override
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			assert value instanceof VFSFileFilter : "Filter is not a VFSFileFilter";
			super.getListCellRendererComponent(
				list, value, index, isSelected, cellHasFocus);
			setText(((VFSFileFilter)value).getDescription());
			return this;
		}

	} //}}}

	//{{{ DirectoriesOnlyFilter class
	public static class DirectoriesOnlyFilter implements VFSFileFilter
	{

		public boolean accept(VFSFile file)
		{
			return file.getType() == VFSFile.DIRECTORY
				|| file.getType() == VFSFile.FILESYSTEM;
		}

		public boolean accept(String url)
		{
			return false;
		}

		public String getDescription()
		{
			return jEdit.getProperty("vfs.browser.file_filter.dir_only");
		}

	} //}}}

	//}}}
}
