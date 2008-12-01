/*
 * DirectoryProvider.java - File list menu
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

package org.gjt.sp.jedit.menu;

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;

import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class DirectoryProvider implements DynamicMenuProvider
{
	//{{{ DirectoryProvider constructor
	public DirectoryProvider(String dir)
	{
		this.dir = dir;
	} //}}}

	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return true;
	} //}}}

	//{{{ update() method
	public void update(JMenu menu)
	{
		final View view = GUIUtilities.getView(menu);

		String path;
		if(dir == null)
		{
			path = view.getBuffer().getDirectory();
		}
		else
			path = dir;

		JMenuItem mi = new JMenuItem(path + ':');
		mi.setActionCommand(path);
		mi.setIcon(FileCellRenderer.openDirIcon);

		//{{{ ActionListeners
		ActionListener fileListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				jEdit.openFile(view,evt.getActionCommand());
			}
		};

		ActionListener dirListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				VFSBrowser.browseDirectory(view,
					evt.getActionCommand());
			}
		}; //}}}

		mi.addActionListener(dirListener);

		menu.add(mi);
		menu.addSeparator();

		if(dir == null && !(view.getBuffer().getVFS() instanceof FileVFS))
		{
			mi = new JMenuItem(jEdit.getProperty(
				"directory.not-local"));
			mi.setEnabled(false);
			menu.add(mi);
			return;
		}

		File directory = new File(path);

		JMenu current = menu;

		// for filtering out backups
		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");

		File[] list = directory.listFiles();
		if(list == null || list.length == 0)
		{
			mi = new JMenuItem(jEdit.getProperty(
				"directory.no-files"));
			mi.setEnabled(false);
			menu.add(mi);
		}
		else
		{
			int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

			Arrays.sort(list,
				new StandardUtilities.StringCompare<File>(true));
			for(int i = 0; i < list.length; i++)
			{
				File file = list[i];

				String name = file.getName();

				// skip marker files
				if(name.endsWith(".marks"))
					continue;

				// skip autosave files
				if(name.startsWith("#") && name.endsWith("#"))
					continue;

				// skip backup files
				if((backupPrefix.length() != 0
					&& name.startsWith(backupPrefix))
					|| (backupSuffix.length() != 0
					&& name.endsWith(backupSuffix)))
					continue;

				// skip directories
				//if(file.isDirectory())
				//	continue;

				mi = new JMenuItem(name);
				mi.setActionCommand(file.getPath());
				mi.addActionListener(file.isDirectory()
					? dirListener
					: fileListener);
				mi.setIcon(file.isDirectory()
					? FileCellRenderer.dirIcon
					: FileCellRenderer.fileIcon);

				if(current.getItemCount() >= maxItems && i != list.length - 1)
				{
					//current.addSeparator();
					JMenu newCurrent = new JMenu(
						jEdit.getProperty(
						"common.more"));
					current.add(newCurrent);
					current = newCurrent;
				}
				current.add(mi);
			}
		}
	} //}}}

	//{{{ Private members
	private final String dir;
	//}}}
}
