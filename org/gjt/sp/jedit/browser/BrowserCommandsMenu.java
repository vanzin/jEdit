/*
 * BrowserCommandsMenu.java - provides various commands
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 * Portions copyright (C) 1999 Jason Ginchereau
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
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

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
			VFS vfs = VFSManager.getVFSForPath(files[0].deletePath);
			int type = files[0].type;
			boolean fileOpen = (jEdit.getBuffer(files[0].path) != null);
			boolean delete = !fileOpen && (vfs.getCapabilities() & VFS.DELETE_CAP) != 0;
			boolean rename = !fileOpen && (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;

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
					fileOpen = true;
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
				add(GUIUtilities.loadMenu(
					VFSBrowser.getActionContext(),
					"vfs.browser.open-in"));
				add(createMenuItem("insert"));

				if(fileOpen)
					add(createMenuItem("close"));
			}
			else if(type != -1)
				add(createMenuItem("open"));

			if(rename)
				add(createMenuItem("rename"));
			if(delete)
				add(createMenuItem("delete"));

			add(createMenuItem("copy-path"));
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
			add(createMenuItem("search-directory"));
		}

		addSeparator();

		add(createMenuItem("show-hidden-files"));

		if(browser.getMode() == VFSBrowser.BROWSER
			|| browser.getMode() == VFSBrowser.BROWSER_DIALOG)
		{
			addSeparator();
			add(createEncodingMenu());
		}

		update();
	} //}}}

	//{{{ update() method
	public void update()
	{
		if(encodingMenuItems != null)
		{
			JRadioButtonMenuItem mi = (JRadioButtonMenuItem)
				encodingMenuItems.get(browser.currentEncoding);
			if(mi != null)
			{
				mi.setSelected(true);
				otherEncoding.setText(jEdit.getProperty(
					"vfs.browser.other-encoding.label"));
			}
			else
			{
				otherEncoding.setSelected(true);
				otherEncoding.setText(jEdit.getProperty(
					"vfs.browser.other-encoding-2.label",
					new String[] { browser.currentEncoding }));
			}
		}
	} //}}}

	//{{{ Private members
	private VFSBrowser browser;
	private HashMap encodingMenuItems;
	private JRadioButtonMenuItem defaultEncoding;
	private JRadioButtonMenuItem otherEncoding;

	//{{{ createMenuItem() method
	private JMenuItem createMenuItem(String name)
	{
		return GUIUtilities.loadMenuItem(VFSBrowser.getActionContext(),
			"vfs.browser." + name,false);
	} //}}}

	//{{{ createEncodingMenu() method
	private JMenu createEncodingMenu()
	{
		ActionHandler actionHandler = new ActionHandler();

		encodingMenuItems = new HashMap();
		JMenu encodingMenu = new JMenu(jEdit.getProperty(
			"vfs.browser.commands.encoding.label"));

		ButtonGroup grp = new ButtonGroup();

		StringTokenizer st = new StringTokenizer(
			jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			String encoding = st.nextToken();
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem(encoding);
			mi.setActionCommand("encoding@" + encoding);
			mi.addActionListener(actionHandler);
			grp.add(mi);
			encodingMenuItems.put(encoding,mi);
			encodingMenu.add(mi);
		}

		String systemEncoding = System.getProperty("file.encoding");
		if(encodingMenuItems.get(systemEncoding) == null)
		{
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem(
				systemEncoding);
			mi.setActionCommand("encoding@" + systemEncoding);
			mi.addActionListener(actionHandler);
			grp.add(mi);
			encodingMenuItems.put(systemEncoding,mi);
			encodingMenu.add(mi);
		}

		encodingMenu.addSeparator();

		otherEncoding = new JRadioButtonMenuItem();
		otherEncoding.setActionCommand("other-encoding");
		otherEncoding.addActionListener(actionHandler);
		grp.add(otherEncoding);
		encodingMenu.add(otherEncoding);

		return encodingMenu;
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
				browser.currentEncoding = encoding;
			}
			else if(actionCommand.startsWith("encoding@"))
			{
				browser.currentEncoding = actionCommand.substring(9);
			}
		}
	} //}}}
}
