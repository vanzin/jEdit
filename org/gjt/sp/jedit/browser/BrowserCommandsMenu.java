/*
 * BrowserCommandsMenu.java - provides various commands
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2000, 2001 Slava Pestov
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * @version $Id$
 * @author Slava Pestov and Jason Ginchereau
 */
public class BrowserCommandsMenu extends JPopupMenu
{
	//{{{ BrowserCommandsMenu constructor
	public BrowserCommandsMenu(VFSBrowser browser, VFS.DirectoryEntry[] files)
	{
		this.browser = browser;

		if(files != null)
		{
			this.files = files;

			VFS vfs = VFSManager.getVFSForPath(files[0].deletePath);
			int type = files[0].type;
			boolean delete = (vfs.getCapabilities() & VFS.DELETE_CAP) != 0;
			boolean rename = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;
			boolean canClose = (jEdit.getBuffer(files[0].path) != null);

			for(int i = 1; i < files.length; i++)
			{
				VFS.DirectoryEntry file = files[i];

				VFS _vfs = VFSManager.getVFSForPath(file.deletePath);
				delete &= (vfs == _vfs) && (_vfs.getCapabilities()
					& VFS.DELETE_CAP) != 0;

				if(type == file.type)
					/* all good */;
				else
				{
					// this will disable most operations if
					// files of multiple types are selected
					type = -1;
				}

				// set rename to false if > 1 file selected
				rename = false;

				// show 'close' item if at least one selected
				// file is currently open
				if(jEdit.getBuffer(file.path) != null)
					canClose = true;
			}

			if(type == VFS.DirectoryEntry.DIRECTORY
				|| type == VFS.DirectoryEntry.FILESYSTEM)
			{
				if(files.length == 1)
					add(createMenuItem("browse"));
				if(browser.getMode() == VFSBrowser.BROWSER)
					add(createMenuItem("browse-window"));
			}
			else if(type == VFS.DirectoryEntry.FILE
				&& (browser.getMode() == VFSBrowser.BROWSER
				|| browser.getMode() == VFSBrowser.BROWSER_DIALOG))
			{
				add(createMenuItem("open"));
				JMenu openIn = new JMenu(jEdit.getProperty(
					"vfs.browser.commands.open-in.label"));
				openIn.add(createMenuItem("open-view"));
				openIn.add(createMenuItem("open-plain-view"));
				openIn.add(createMenuItem("open-split"));
				add(openIn);
				add(createMenuItem("insert"));

				if(canClose)
					add(createMenuItem("close"));
			}
			else if(type != -1)
				add(createMenuItem("choose"));

			if(rename)
				add(createMenuItem("rename"));
			if(delete)
				add(createMenuItem("delete"));

			addSeparator();
		}

		add(createMenuItem("up"));
		add(createMenuItem("reload"));
		add(createMenuItem("roots"));
		add(createMenuItem("home"));
		add(createMenuItem("synchronize"));
		addSeparator();

		if(browser.getMode() == VFSBrowser.BROWSER)
			add(createMenuItem("new-file"));

		add(createMenuItem("new-directory"));

		if(browser.getMode() == VFSBrowser.BROWSER)
		{
			addSeparator();
			add(createMenuItem("search-in-directory"));
		}

		addSeparator();

		showHiddenFiles = new JCheckBoxMenuItem(
			jEdit.getProperty("vfs.browser.commands.show-hidden-files.label"));
		showHiddenFiles.setActionCommand("show-hidden-files");
		showHiddenFiles.setSelected(browser.getShowHiddenFiles());
		showHiddenFiles.addActionListener(new ActionHandler());
		add(showHiddenFiles);
	} //}}}

	//{{{ update() method
	public void update()
	{
		showHiddenFiles.setSelected(browser.getShowHiddenFiles());
	} //}}}

	//{{{ Private members
	private VFSBrowser browser;
	private VFS.DirectoryEntry[] files;
	private VFS vfs;
	private JCheckBoxMenuItem showHiddenFiles;

	//{{{ createMenuItem() method
	private JMenuItem createMenuItem(String name)
	{
		String label = jEdit.getProperty("vfs.browser.commands." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	} //}}}

	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			View view = browser.getView();
			String actionCommand = evt.getActionCommand();

			if(actionCommand.equals("other-encoding"))
			{
				String encoding = GUIUtilities.input(browser,
					"encoding-prompt",null,
					jEdit.getProperty("buffer.encoding",
					System.getProperty("file.encoding")));
				if(encoding == null)
					return;
				Hashtable props = new Hashtable();
				props.put(Buffer.ENCODING,encoding);
				jEdit.openFile(view,null,files[0].path,false,props);
			}
			else if(actionCommand.equals("open"))
				browser.filesActivated(VFSBrowser.M_OPEN,false);
			else if(actionCommand.equals("open-view"))
				browser.filesActivated(VFSBrowser.M_OPEN_NEW_VIEW,false);
			else if(actionCommand.equals("open-plain-view"))
				browser.filesActivated(VFSBrowser.M_OPEN_NEW_PLAIN_VIEW,false);
			else if(actionCommand.equals("open-split"))
				browser.filesActivated(VFSBrowser.M_OPEN_NEW_SPLIT,false);
			else if(actionCommand.equals("insert"))
			{
				for(int i = 0; i < files.length; i++)
				{
					view.getBuffer().insertFile(view,files[i].path);
				}
			}
			else if(actionCommand.equals("choose"))
				browser.filesActivated(VFSBrowser.M_OPEN,false);
			else if(actionCommand.equals("close"))
			{
				for(int i = 0; i < files.length; i++)
				{
					Buffer buffer = jEdit.getBuffer(files[i].path);
					if(buffer != null)
						jEdit.closeBuffer(view,buffer);
				}
			}
			else if(actionCommand.equals("browse"))
				browser.setDirectory(files[0].path);
			else if(actionCommand.equals("browse-window"))
			{
				for(int i = 0; i < files.length; i++)
				{
					VFSBrowser.browseDirectoryInNewWindow(view,
						files[i].path);
				}
			}
			else if(actionCommand.equals("rename"))
				browser.rename(files[0].path);
			else if(actionCommand.equals("delete"))
				browser.delete(files);
			else if(actionCommand.equals("up"))
			{
				String path = browser.getDirectory();
				VFS vfs = VFSManager.getVFSForPath(path);
				browser.setDirectory(vfs.getParentOfPath(path));
			}
			else if(actionCommand.equals("reload"))
				browser.reloadDirectory();
			else if(actionCommand.equals("roots"))
				browser.rootDirectory();
			else if(actionCommand.equals("home"))
				browser.setDirectory(System.getProperty("user.home"));
			else if(actionCommand.equals("synchronize"))
			{
				Buffer buffer = browser.getView().getBuffer();
				browser.setDirectory(buffer.getVFS().getParentOfPath(
					buffer.getPath()));
			}
			else if(actionCommand.equals("new-file"))
				browser.newFile();
			else if(actionCommand.equals("new-directory"))
				browser.mkdir();
			else if(actionCommand.equals("search-in-directory"))
				browser.searchInDirectory();
			else if(actionCommand.equals("show-hidden-files"))
			{
				browser.setShowHiddenFiles(!browser.getShowHiddenFiles());
				browser.reloadDirectory();
			}
		}
	} //}}}
}
