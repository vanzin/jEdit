/*
 * FileRootsVFS.java - Local root filesystems VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2002 Slava Pestov
 * Portions copyright (C) 2002 Kris Kopicki
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
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.lang.reflect.*;
import java.io.File;
import java.util.LinkedList;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.util.Log;
//}}}

/**
 * A VFS that lists local root filesystems.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileRootsVFS extends VFS
{
	public static final String PROTOCOL = "roots";

	//{{{ FileRootsVFS constructor
	public FileRootsVFS()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create an item for this VFS in its 'Plugins' menu
		super("roots",LOW_LATENCY_CAP);

		// JDK 1.4 adds methods to obtain a drive letter label and
		// list the desktop on Windows
		if(OperatingSystem.hasJava14())
		{
			try
			{
				getSystemDisplayName = FileSystemView.class.getMethod("getSystemDisplayName",
					new Class[] { java.io.File.class });
				getRoots = FileSystemView.class.getMethod("getRoots",
					new Class[0]);
				isFileSystemRoot = FileSystemView.class.getMethod("isFileSystemRoot",
					new Class[] { java.io.File.class });
				fsView = FileSystemView.getFileSystemView();
				Log.log(Log.DEBUG,this,"Java 1.4 FileSystemView detected");
			}
			catch(Exception e)
			{
				Log.log(Log.DEBUG,this,"Java 1.4 FileSystemView not detected");
			}
		}
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
		File[] roots = listRoots();

		if(roots == null)
			return null;

		VFS.DirectoryEntry[] rootDE = new VFS.DirectoryEntry[roots.length];
		for(int i = 0; i < roots.length; i++)
		{
			String name = roots[i].getPath();
			rootDE[i] = _getDirectoryEntry(session,name,comp);
		}

		return rootDE;
	} //}}}

	//{{{ _getDirectoryEntry() method
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{

		File file = new File(path);

		int type;

		if(file.isDirectory())
		{
			type = VFS.DirectoryEntry.FILESYSTEM;

			if(isFileSystemRoot != null)
			{
				try
				{
					if(Boolean.FALSE.equals(isFileSystemRoot
						.invoke(fsView,new Object[] { file })))
					{
						type = VFS.DirectoryEntry.DIRECTORY;
					}
				}
				catch(Exception e) {}
			}
		}
		else
			type = VFS.DirectoryEntry.FILE;

		String name;

		if(getSystemDisplayName != null && !path.startsWith("A:")
			&& !path.startsWith("B:"))
		{
			try
			{
				name = path + " " + (String)getSystemDisplayName
					.invoke(fsView,new Object[] { file });
			}
			catch(Exception e)
			{
				name = path;
			}
		}
		else if(OperatingSystem.isMacOS())
			name = getFileName(path);
		else
			name = path;

		return new VFS.DirectoryEntry(name,path,path,type,0L,false);
	} //}}}

	//{{{ Private members
	private static FileSystemView fsView;
	private static Method getSystemDisplayName;
	private static Method getRoots;
	private static Method isFileSystemRoot;

	//{{{ listRoots() method
	private static File[] listRoots()
	{
		if(getRoots != null)
		{
			try
			{
				File[] roots = (File[])getRoots.invoke(fsView,
					new Object[0]);
				return roots;
			}
			catch(Exception e)
			{
				return null;
			}
		}
		else if (OperatingSystem.isMacOS())
		{
			// Nasty hardcoded values
			File[] volumes = new File("/Volumes").listFiles();
			LinkedList roots = new LinkedList();

			roots.add(new File("/"));

			for (int i=0; i<volumes.length; i++)
			{
				// Make sure people don't do stupid things like putting files in /Volumes
				if (volumes[i].isDirectory())
					roots.add(volumes[i]);
			}

			return (File[])roots.toArray(new File[0]);
		}
		else
			return File.listRoots();
	} //}}}

	//}}}
}
