/*
 * BrowserPopupMenu.java - provides popup actions for rename, del, etc.
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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

/**
 * @version $Id$
 * @author Slava Pestov and Jason Ginchereau
 */
public class BrowserPopupMenu extends JPopupMenu
{
	public BrowserPopupMenu(VFSBrowser browser, VFS.DirectoryEntry file)
	{
		this.browser = browser;

		if(file != null)
		{
			this.file = file;
			this.vfs = VFSManager.getVFSForPath(file.path);

			boolean delete = (vfs.getCapabilities() & VFS.DELETE_CAP) != 0;
			boolean rename = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;

			if(jEdit.getBuffer(file.path) != null)
			{
				if(browser.getMode() == VFSBrowser.BROWSER)
				{
					add(createMenuItem("open"));
					add(createMenuItem("insert"));
					add(createMenuItem("close"));
				}
				else
					add(createMenuItem("choose"));
			}
			else
			{
				if(file.type == VFS.DirectoryEntry.DIRECTORY
					|| file.type == VFS.DirectoryEntry.FILESYSTEM)
				{
					add(createMenuItem("browse"));
				}
				else if(browser.getMode() != VFSBrowser.BROWSER)
				{
					add(createMenuItem("choose"));
				}
				// else if in browser mode
				else
				{
					add(createMenuItem("open"));
					add(createOpenEncodingMenu());
					add(createMenuItem("insert"));
				}
	
				if(rename)
					add(createMenuItem("rename"));
				if(delete)
					add(createMenuItem("delete"));
			}

			addSeparator();
		}
		else
			vfs = VFSManager.getVFSForPath(browser.getDirectory());

		JCheckBoxMenuItem showHiddenFiles = new JCheckBoxMenuItem(
			jEdit.getProperty("vfs.browser.menu.show-hidden-files.label"));
		showHiddenFiles.setActionCommand("show-hidden-files");
		showHiddenFiles.setSelected(browser.getShowHiddenFiles());
		showHiddenFiles.addActionListener(new ActionHandler());
		add(showHiddenFiles);

		addSeparator();
		add(createMenuItem("new-file"));
		add(createMenuItem("new-directory"));

		addSeparator();

		// note that we don't display the search in directory command
		// in open and save dialog boxes
		if(browser.getMode() == VFSBrowser.BROWSER
			&& vfs instanceof FileVFS)
		{
			add(createMenuItem("search-in-directory"));
			addSeparator();
		}

		add(createMenuItem("add-to-favorites"));
		add(createMenuItem("go-to-favorites"));

		// put them in a vector for sorting
		Vector vec = new Vector();
		Enumeration enum = VFSManager.getFilesystems();

		while(enum.hasMoreElements())
		{
			VFS vfs = (VFS)enum.nextElement();
			if((vfs.getCapabilities() & VFS.BROWSE_CAP) == 0)
				continue;

			JMenuItem menuItem = new JMenuItem(jEdit.getProperty(
				"vfs." + vfs.getName() + ".label"));
			menuItem.setActionCommand("vfs." + vfs.getName());
			menuItem.addActionListener(new ActionHandler());
			vec.addElement(menuItem);
		}

		if(vec.size() != 0)
		{
			addSeparator();

			MiscUtilities.quicksort(vec,new MiscUtilities.MenuItemCompare());
			for(int i = 0; i < vec.size(); i++)
				add((JMenuItem)vec.elementAt(i));
		}
	}

	// private members
	private VFSBrowser browser;
	private VFS.DirectoryEntry file;
	private VFS vfs;

	private JMenuItem createMenuItem(String name)
	{
		String label = jEdit.getProperty("vfs.browser.menu." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	}

	private JMenu createOpenEncodingMenu()
	{
		ActionListener listener = new ActionHandler();

		JMenu openEncoding = new JMenu(jEdit.getProperty("open-encoding.label"));

		// used twice...
		String systemEncoding = System.getProperty("file.encoding");

		JMenuItem mi = new JMenuItem(jEdit.getProperty("os-encoding"));
		mi.setActionCommand("open@" + systemEncoding);
		mi.addActionListener(listener);
		openEncoding.add(mi);

		mi = new JMenuItem(jEdit.getProperty("jedit-encoding"));
		mi.setActionCommand("open@" + jEdit.getProperty("buffer.encoding",systemEncoding));
		mi.addActionListener(listener);
		openEncoding.add(mi);

		openEncoding.addSeparator();

		StringTokenizer st = new StringTokenizer(jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			String encoding = st.nextToken();
			mi = new JMenuItem(encoding);
			mi.setActionCommand("open@" + encoding);
			mi.addActionListener(listener);
			openEncoding.add(mi);
		}

		openEncoding.addSeparator();

		mi = new JMenuItem(jEdit.getProperty("other-encoding.label"));
		mi.setActionCommand("other-encoding");
		mi.addActionListener(listener);
		openEncoding.add(mi);

		return openEncoding;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			View view = browser.getView();
			String actionCommand = evt.getActionCommand();

			if(actionCommand.startsWith("open@"))
			{
				// a bit of a hack to support 'Open With Encoding' menu
				Hashtable props = new Hashtable();
				props.put(Buffer.ENCODING,actionCommand.substring(5));
				jEdit.openFile(view,null,file.path,false,props);
			}
			else if(actionCommand.equals("other-encoding"))
			{
				String encoding = GUIUtilities.input(browser,
					"encoding-prompt",null,
					jEdit.getProperty("buffer.encoding",
					System.getProperty("file.encoding")));
				if(encoding == null)
					return;
				Hashtable props = new Hashtable();
				props.put(Buffer.ENCODING,encoding);
				jEdit.openFile(view,null,file.path,false,props);
			}
			else if(actionCommand.equals("open"))
				jEdit.openFile(view,file.path);
			else if(actionCommand.equals("insert"))
				view.getBuffer().insert(view,file.path);
			else if(actionCommand.equals("choose"))
				browser.filesActivated();
			else if(actionCommand.equals("close"))
			{
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null)
					jEdit.closeBuffer(view,buffer);
			}
			else if(actionCommand.equals("browse"))
				browser.setDirectory(file.path);
			else if(evt.getActionCommand().equals("rename"))
				browser.rename(file.path);
			else if(evt.getActionCommand().equals("delete"))
				browser.delete(file.deletePath);
			else if(actionCommand.equals("show-hidden-files"))
			{
				browser.setShowHiddenFiles(!browser.getShowHiddenFiles());
				browser.reloadDirectory();
			}
			else if(actionCommand.equals("new-file"))
			{
				VFS.DirectoryEntry[] selected = browser.getSelectedFiles();
				if(selected.length >= 1)
				{
					VFS.DirectoryEntry file = selected[0];
					if(file.type == VFS.DirectoryEntry.DIRECTORY)
						jEdit.newFile(view,file.path);
					else
					{
						VFS vfs = VFSManager.getVFSForPath(file.path);
						jEdit.newFile(view,vfs.getParentOfPath(file.path));
					}
				}
				else
					jEdit.newFile(view,browser.getDirectory());
			}
			else if(actionCommand.equals("new-directory"))
				browser.mkdir();
			else if(actionCommand.equals("search-in-directory"))
			{
				String path;

				VFS.DirectoryEntry[] selected = browser.getSelectedFiles();
				if(selected.length >= 1)
				{
					VFS.DirectoryEntry file = selected[0];
					if(file.type == VFS.DirectoryEntry.DIRECTORY)
						path = file.path;
					else
					{
						VFS vfs = VFSManager.getVFSForPath(file.path);
						path = vfs.getParentOfPath(file.path);
					}
				}
				else
					path = browser.getDirectory();

				SearchAndReplace.setSearchFileSet(new DirectoryListSet(
					path,browser.getFilenameFilter(),true));
				new SearchDialog(browser.getView(),null,SearchDialog.DIRECTORY);
			}
			else if(actionCommand.equals("add-to-favorites"))
			{
				// if any directories are selected, add
				// them, otherwise add current directory
				Vector toAdd = new Vector();
				VFS.DirectoryEntry[] selected = browser.getSelectedFiles();
				for(int i = 0; i < selected.length; i++)
				{
					VFS.DirectoryEntry file = selected[i];
					if(file.type == VFS.DirectoryEntry.FILE)
					{
						GUIUtilities.error(browser,
							"vfs.browser.files-favorites",
							null);
						return;
					}
					else
						toAdd.addElement(file.path);
				}
	
				if(toAdd.size() != 0)
				{
					for(int i = 0; i < toAdd.size(); i++)
					{
						FavoritesVFS.addToFavorites((String)toAdd.elementAt(i));
					}
				}
				else
				{
					String directory = browser.getDirectory();
					if(directory.equals(FavoritesVFS.PROTOCOL + ":"))
					{
						GUIUtilities.error(browser,
							"vfs.browser.recurse-favorites",
							null);
					}
					else
					{
						FavoritesVFS.addToFavorites(directory);
					}
				}
			}
			else if(actionCommand.equals("go-to-favorites"))
				browser.setDirectory(FavoritesVFS.PROTOCOL + ":");
			else if(actionCommand.startsWith("vfs."))
			{
				String vfsName = actionCommand.substring(4);
				VFS vfs = VFSManager.getVFSByName(vfsName);
				String directory = vfs.showBrowseDialog(null,browser);
				if(directory != null)
					browser.setDirectory(directory);
			}
		}
	}
}
