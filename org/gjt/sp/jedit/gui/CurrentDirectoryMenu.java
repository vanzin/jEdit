/*
 * CurrentDirectoryMenu.java - File list menu
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import org.gjt.sp.jedit.*;

public class CurrentDirectoryMenu extends EnhancedMenu
{
	public CurrentDirectoryMenu()
	{
		super("current-directory");
	}

	public void setPopupMenuVisible(boolean b)
	{
		if(b)
		{
			final View view = EditAction.getView(this);

			if(getMenuComponentCount() != 0)
				removeAll();

			File file = view.getBuffer().getFile();
			if(file == null)
			{
				JMenuItem mi = new JMenuItem(jEdit.getProperty(
					"current-directory.not-local"));
				mi.setEnabled(false);
				add(mi);
				super.setPopupMenuVisible(b);
				return;
			}

			File dir = new File(file.getParent());

			JMenuItem mi = new JMenuItem(dir.getPath());
			mi.setEnabled(false);
			add(mi);
			addSeparator();

			JMenu current = this;
			ActionListener listener = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					jEdit.openFile(view,evt.getActionCommand());
				}
			};

			// for filtering out backups
			String backupPrefix = jEdit.getProperty("backup.prefix");
			String backupSuffix = jEdit.getProperty("backup.suffix");

			String[] list = dir.list();
			if(list != null)
			{
				MiscUtilities.quicksort(list,
					new MiscUtilities.StringICaseCompare());
				for(int i = 0; i < list.length; i++)
				{
					String name = list[i];

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
					file = new File(dir,name);
					if(file.isDirectory())
						continue;

					mi = new JMenuItem(name);
					mi.setActionCommand(file.getPath());
					mi.addActionListener(listener);

					if(current.getItemCount() >= 20)
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

		}

		super.setPopupMenuVisible(b);
	}
}
