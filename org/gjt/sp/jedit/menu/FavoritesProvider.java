/*
 * FavoritesProvider.java - Favorites list menu
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
//}}}

public class FavoritesProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return false;
	} //}}}

	//{{{ update() method
	public void update(JMenu menu)
	{
		final View view = GUIUtilities.getView(menu);

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

		VFS.DirectoryEntry[] favorites
			= FavoritesVFS.getFavorites();
		if(favorites.length == 0)
		{
			JMenuItem mi = new JMenuItem(
				jEdit.getProperty(
				"vfs.browser.favorites"
				+ ".no-favorites.label"));
			mi.setEnabled(false);
			menu.add(mi);
		}
		else
		{
			MiscUtilities.quicksort(favorites,
				new VFS.DirectoryEntryCompare(
				jEdit.getBooleanProperty("vfs.browser.sortMixFilesAndDirs"),
				jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase")));
			for(int i = 0; i < favorites.length; i++)
			{
				VFS.DirectoryEntry favorite
					= favorites[i];
				JMenuItem mi = new JMenuItem(favorite.path);
				mi.setIcon(FileCellRenderer
					.getIconForFile(
					favorite,false));
				if(favorite.type ==
					VFS.DirectoryEntry.FILE)
				{
					mi.addActionListener(fileListener);
				}
				else
				{
					mi.addActionListener(dirListener);
				}
				menu.add(mi);
			}
		}
	} //}}}
}
