/*
 * FileRootsVFS.java - Local root filesystems VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 * Portions copyright (C) 2002 Kris Kopicki
 * Portions copyright (C) 2002 Carmine Lucarelli
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
import org.gjt.sp.jedit.MiscUtilities;
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
		super("roots",LOW_LATENCY_CAP,new String[] {
			EA_TYPE });

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
				isFloppyDrive = FileSystemView.class.getMethod("isFloppyDrive",
					new Class[] { java.io.File.class });
				isDrive = FileSystemView.class.getMethod("isDrive",
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
			rootDE[i] = new RootsEntry(roots[i]);
		}

		return rootDE;
	} //}}}

	//{{{ _getDirectoryEntry() method
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		return new RootsEntry(new File(path));
	} //}}}

	//{{{ Private members
	private static FileSystemView fsView;
	private static Method getSystemDisplayName;
	private static Method getRoots;
	private static Method isFileSystemRoot;
	private static Method isFloppyDrive;
	private static Method isDrive;

	//{{{ listRoots() method
	private static File[] listRoots()
	{
		if (OperatingSystem.isMacOS())
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
		{
			File[] roots = File.listRoots();
			File[] desktop = null;

			if(getRoots != null)
			{
				try
				{
					desktop = (File[])getRoots.invoke(fsView,
						new Object[0]);
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR, FileRootsVFS.class, "Error getting Desktop: " + e.getMessage());
					desktop = null;
				}
			}

			if(desktop == null)
				return roots;

			File[] rootsPlus = new File[roots.length + desktop.length];
			System.arraycopy(desktop, 0, rootsPlus, 0, desktop.length);
			System.arraycopy(roots, 0, rootsPlus, 1, roots.length);
			return rootsPlus;
		}
	} //}}}

	//}}}

	//{{{ RootsEntry class
	static class RootsEntry extends VFS.DirectoryEntry
	{
		RootsEntry(File file)
		{
			// REMIND: calling isDirectory() on a floppy drive
			// displays stupid I/O error dialog box on Windows

			this.path = this.deletePath = file.getPath();

			if(isFloppy(file))
			{
				type = VFS.DirectoryEntry.FILESYSTEM;
				name = path;
			}
			else if(isDrive(file))
			{
				type = VFS.DirectoryEntry.FILESYSTEM;

				if(getSystemDisplayName != null)
				{
					try
					{
						name = path + " " + (String)getSystemDisplayName
							.invoke(fsView,new Object[] { file });
					}
					catch(Exception e)
					{
						Log.log(Log.ERROR,this,e);
						name = path;
					}
				}
			}
			else if(file.isDirectory())
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

				if(OperatingSystem.isMacOS())
					name = MiscUtilities.getFileName(path);
				else
					name = path;
			}
			else
				type = VFS.DirectoryEntry.FILE;
		}

		public String getExtendedAttribute(String name)
		{
			if(name.equals(EA_TYPE))
				return super.getExtendedAttribute(name);
			else
			{
				// don't want it to show "0 bytes" for size,
				// etc.
				return null;
			}
		}

		private boolean isFloppy(File file)
		{
			// to prevent windows looking for a disk in the floppy drive
			if(isFloppyDrive != null)
			{
				try
				{
					return Boolean.TRUE.equals(isFloppyDrive.
						invoke(fsView, new Object[] { file }));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,e);
					return false;
				}
			}
			else
				return path.startsWith("A:") || path.startsWith("B:");
		}

		private boolean isDrive(File file)
		{
			// so an empty cd drive is not reported as a file
			if(isDrive != null)
			{
				try
				{
					return Boolean.TRUE.equals(isDrive.
						invoke(fsView, new Object[] { file }));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,e);
					return false;
				}
			}
			else
				return true;
		}
	} //}}}
}
