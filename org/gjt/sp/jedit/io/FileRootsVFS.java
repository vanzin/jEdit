/*
 * FileRootsVFS.java - Local root filesystems VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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

		// JDK 1.4 adds a method to obtain a drive letter label
		try
		{
			method = FileSystemView.class.getMethod("getSystemDisplayName",
				new Class[] { java.io.File.class });
			fsView = FileSystemView.getFileSystemView();
			Log.log(Log.DEBUG,this,"FileSystemView.getSystemDisplayName() detected");
		}
		catch(Exception e)
		{
			Log.log(Log.DEBUG,this,"FileSystemView.getSystemDisplayName() not detected");
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
		String name = (OperatingSystem.isMacOS() ?
			getFileName(path) : path);

		if(method != null && !name.startsWith("A:") && !name.startsWith("B:"))
		{
			try
			{
				name = name + " " + (String)method.invoke(fsView,
					new Object[] { new File(path) });
			}
			catch(Exception e) {}
		}

		return new VFS.DirectoryEntry(name,path,path,VFS.DirectoryEntry
			.FILESYSTEM,0L,false);
	} //}}}

	//{{{ Private members
	private FileSystemView fsView;
	private Method method;

	//{{{ listRoots() method
	private static File[] listRoots()
	{
		if (OperatingSystem.isMacOS())
		{
			// Nasty hardcoded values
			File[] volumes = new File("/Volumes").listFiles();
			File[] roots = new File[volumes.length+1];
			
			roots[0] = new File("/");
			
			for (int i=0; i<volumes.length; i++)
			{
				// Make sure people don't do stupid things like putting files in /Volumes
				if (volumes[i].isDirectory())
					roots[i+1] = volumes[i];
			}
			
			return roots;
		}
		else
			return File.listRoots();
	} //}}}
	
	//}}}
}
