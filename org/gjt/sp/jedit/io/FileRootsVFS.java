/*
 * FileRootsVFS.java - Local root filesystems VFS
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

package org.gjt.sp.jedit.io;

import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.lang.reflect.*;
import java.io.File;
import org.gjt.sp.util.Log;

/**
 * A VFS that lists local root filesystems.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileRootsVFS extends VFS
{
	public static final String PROTOCOL = "roots";

	public FileRootsVFS()
	{
		super("roots");

		// try using Java 2 method first
		try
		{
			method = File.class.getMethod("listRoots",new Class[0]);
			Log.log(Log.DEBUG,this,"File.listRoots() detected");
		}
		catch(Exception e)
		{
			fsView = FileSystemView.getFileSystemView();
			Log.log(Log.DEBUG,this,"File.listRoots() not detected");
		}
	}

	public int getCapabilities()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create the default 'favorites' item in the 'More' menu
		return 0 /* BROWSE_CAP | */;
	}

	public String getParentOfPath(String path)
	{
		return PROTOCOL + ":";
	}

	public VFS.DirectoryEntry[] _listDirectory(Object session, String url,
		Component comp)
	{
		File[] roots;

		if(method == null)
			roots = fsView.getRoots();
		else
		{
			try
			{
				roots = (File[])method.invoke(null,new Object[0]);
			}
			catch(Exception e)
			{
				roots = null;
				Log.log(Log.ERROR,this,e);
			}
		}

		if(roots == null)
			return null;

		VFS.DirectoryEntry[] rootDE = new VFS.DirectoryEntry[roots.length];
		for(int i = 0; i < roots.length; i++)
		{
			String name = roots[i].getPath();
			rootDE[i] = _getDirectoryEntry(session,name,comp);
		}

		return rootDE;
	}

	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		return new VFS.DirectoryEntry(path,path,path,VFS.DirectoryEntry
			.FILESYSTEM,0L,false);
	}

	// private members
	private FileSystemView fsView;
	private Method method;
}
