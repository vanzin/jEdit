/*
 * FavoritesVFS.java - Stores frequently-visited directory locations
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import java.awt.Component;
import java.util.ArrayList;
import org.gjt.sp.jedit.jEdit;
//}}}

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

	//{{{ FavoritesVFS constructor
	public FavoritesVFS()
	{
		super("favorites");

		/* addToFavorites(), which is a static method
		 * (for convinience) needs an instance of the
		 * VFS to pass to VFSManager.sendVFSUpdate(),
		 * hence this hack. */
		instance = this;
	} //}}}

	//{{{ getCapabilities() method
	public int getCapabilities()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create the default 'favorites' button on the tool bar
		return /* BROWSE_CAP | */ DELETE_CAP;
	} //}}}

	//{{{ getParentOfPath() method
	public String getParentOfPath(String path)
	{
		return PROTOCOL + ":";
	} //}}}

	//{{{ _listDirectory() method
	public VFS.DirectoryEntry[] _listDirectory(Object session, String url,
		Component comp)
	{
		synchronized(lock)
		{
			if(favorites == null)
				loadFavorites();

			VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[favorites.size()];
			for(int i = 0; i < retVal.length; i++)
			{
				String favorite = (String)favorites.get(i);
				retVal[i] = _getDirectoryEntry(session,favorite,comp);
			}
			return retVal;
		}
	} //}}}

	//{{{ _getDirectoryEntry() method
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		return new VFS.DirectoryEntry(path,path,"favorites:" + path,
					VFS.DirectoryEntry.DIRECTORY,
					0L,false);
	} //}}}

	//{{{ _delete() method
	public boolean _delete(Object session, String path, Component comp)
	{
		synchronized(lock)
		{
			path = path.substring(PROTOCOL.length() + 1);
			favorites.remove(path);

			VFSManager.sendVFSUpdate(this,PROTOCOL + ":",false);
		}

		return true;
	} //}}}

	//{{{ loadFavorites() method
	public static void loadFavorites()
	{
		favorites = new ArrayList();

		synchronized(lock)
		{
			String favorite;
			int i = 0;
			while((favorite = jEdit.getProperty("vfs.favorite." + i)) != null)
			{
				favorites.add(favorite);
				i++;
			}
		}
	} //}}}

	//{{{ addToFavorites() method
	public static void addToFavorites(String path)
	{
		synchronized(lock)
		{
			if(favorites == null)
				loadFavorites();

			if(!favorites.contains(path))
				favorites.add(path);

			VFSManager.sendVFSUpdate(instance,PROTOCOL + ":",false);
		}
	} //}}}

	//{{{ saveFavorites() method
	public static void saveFavorites()
	{
		synchronized(lock)
		{
			if(favorites == null)
				return;

			for(int i = 0; i < favorites.size(); i++)
			{
				jEdit.setProperty("vfs.favorite." + i,
					(String)favorites.get(i));
			}
			jEdit.unsetProperty("vfs.favorite." + favorites.size());
		}
	} //}}}

	//{{{ getFavorites() method
	public static Object[] getFavorites()
	{
		synchronized(lock)
		{
			if(favorites == null)
				loadFavorites();

			return favorites.toArray();
		}
	} //}}}

	//{{{ Private members
	private static FavoritesVFS instance;
	private static Object lock = new Object();
	private static ArrayList favorites;
	//}}}
}
