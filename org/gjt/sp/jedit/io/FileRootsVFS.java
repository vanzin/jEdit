/*
 * FileRootsVFS.java - Local root filesystems VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
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
			rootDE[i] = new RootsEntry(roots[i]);

		return rootDE;
	} //}}}

	//{{{ _getDirectoryEntry() method
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		return new RootsEntry(new File(path));
	} //}}}

	//{{{ Private members
	private static FileSystemView fsView = FileSystemView.getFileSystemView();

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
			File[] desktop = fsView.getRoots();

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

			this.path = this.deletePath = this.symlinkPath
				= file.getPath();

			if(fsView.isFloppyDrive(file))
			{
				type = VFS.DirectoryEntry.FILESYSTEM;
				name = path;
			}
			else if(fsView.isDrive(file))
			{
				type = VFS.DirectoryEntry.FILESYSTEM;

				name =  path + " "
					+ fsView.getSystemDisplayName(file);
			}
			else if(file.isDirectory())
			{
				type = VFS.DirectoryEntry.FILESYSTEM;
				if(fsView.isFileSystemRoot(file))
					type = VFS.DirectoryEntry.DIRECTORY;

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
	} //}}}
}
