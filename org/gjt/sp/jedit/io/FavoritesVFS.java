/*
 * FavoritesVFS.java - Stores frequently-visited directory locations
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.io;

import java.awt.Component;
import java.util.Vector;
import org.gjt.sp.jedit.jEdit;

/**
 * A VFS used for remembering frequently-visited directories. Listing it
 * returns the favorites list. The deletePath of each entry is the
 * directory prefixed with "favorites:" so that right-clicking on a
 * favorite and clicking 'delete' in the browser just deletes the
 * favorite, and not the directory itself.
 * @author Slava Pestov
 * @version $Id$
 */
public class FavoritesVFS extends VFS
{
	public static final String PROTOCOL = "favorites";

	public FavoritesVFS()
	{
		super("favorites");

		/* addToFavorites(), which is a static method
		 * (for convinience) needs an instance of the
		 * VFS to pass to VFSManager.sendVFSUpdate(),
		 * hence this hack. */
		instance = this;
	}

	public int getCapabilities()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create the default 'favorites' button on the tool bar
		return /* BROWSE_CAP | */ DELETE_CAP;
	}

	public String getParentOfPath(String path)
	{
		return PROTOCOL + ":";
	}

	public VFS.DirectoryEntry[] _listDirectory(Object session, String url,
		Component comp)
	{
		synchronized(lock)
		{
			VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[favorites.size()];
			for(int i = 0; i < retVal.length; i++)
			{
				String favorite = (String)favorites.elementAt(i);
				retVal[i] = _getDirectoryEntry(session,favorite,comp);
			}
			return retVal;
		}
	}

	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		return new VFS.DirectoryEntry(path,path,"favorites:" + path,
					VFS.DirectoryEntry.DIRECTORY,
					0L,false);
	}

	public boolean _delete(Object session, String path, Component comp)
	{
		synchronized(lock)
		{
			path = path.substring(PROTOCOL.length() + 1);
			favorites.removeElement(path);

			VFSManager.sendVFSUpdate(this,PROTOCOL + ":",false);
		}

		return true;
	}

	public static void loadFavorites()
	{
		synchronized(lock)
		{
			String favorite;
			int i = 0;
			while((favorite = jEdit.getProperty("vfs.favorite." + i)) != null)
			{
				favorites.addElement(favorite);
				i++;
			}
		}
	}

	public static void addToFavorites(String path)
	{
		synchronized(lock)
		{
			favorites.addElement(path);

			VFSManager.sendVFSUpdate(instance,PROTOCOL + ":",false);
		}
	}

	public static void saveFavorites()
	{
		synchronized(lock)
		{
			for(int i = 0; i < favorites.size(); i++)
			{
				jEdit.setProperty("vfs.favorite." + i,
					(String)favorites.elementAt(i));
			}
			jEdit.unsetProperty("vfs.favorite." + favorites.size());
		}
	}

	// private members
	private static FavoritesVFS instance;
	private static Object lock = new Object();
	private static Vector favorites = new Vector();
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:16  spestov
 * Initial revision
 *
 * Revision 1.7  2000/11/11 02:59:30  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.6  2000/08/29 07:47:13  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.5  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.4  2000/08/20 07:29:31  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.3  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.2  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.1  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 */
