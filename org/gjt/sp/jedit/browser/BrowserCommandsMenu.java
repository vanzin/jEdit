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
	public BrowserCommandsMenu(VFSBrowser browser, VFS.DirectoryEntry file)
	{
		this.browser = browser;

		if(file != null)
		{
			this.file = file;

			VFS vfs = VFSManager.getVFSForPath(file.path);

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

		JCheckBoxMenuItem showHiddenFiles = new JCheckBoxMenuItem(
			jEdit.getProperty("vfs.browser.commands.show-hidden-files.label"));
		showHiddenFiles.setActionCommand("show-hidden-files");
		showHiddenFiles.setSelected(browser.getShowHiddenFiles());
		showHiddenFiles.addActionListener(new ActionHandler());
		add(showHiddenFiles);
	} //}}}

	//{{{ Private members
	private VFSBrowser browser;
	private VFS.DirectoryEntry file;
	private VFS vfs;

	//{{{ createMenuItem() method
	private JMenuItem createMenuItem(String name)
	{
		String label = jEdit.getProperty("vfs.browser.commands." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	} //}}}

	//{{{ createOpenEncodingMenu() method
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
	} //}}}

	//}}}

	//{{{ ActionHandler class
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
				view.getBuffer().insertFile(view,file.path);
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
			else if(actionCommand.equals("rename"))
				browser.rename(file.path);
			else if(actionCommand.equals("delete"))
				browser.delete(file.deletePath);
			else if(actionCommand.equals("up"))
			{
				String path = browser.getDirectory();
				VFS vfs = VFSManager.getVFSForPath(path);
				browser.setDirectory(vfs.getParentOfPath(path));
			}
			else if(actionCommand.equals("reload"))
				browser.reloadDirectory();
			else if(actionCommand.equals("roots"))
				browser.setDirectory(FileRootsVFS.PROTOCOL + ":");
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
