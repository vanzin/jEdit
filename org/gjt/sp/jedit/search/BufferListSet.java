/*
 * BufferListSet.java - Buffer list matcher
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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

package org.gjt.sp.jedit.search;

//{{{ Imports
import java.awt.Component;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * A file set for searching a user-specified list of buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class BufferListSet implements SearchFileSet
{
	//{{{ getFirstFile() method
	public synchronized String getFirstFile(View view)
	{
		if(files == null)
			files = _getFiles(view);

		if(files == null || files.length == 0)
			return null;
		else
			return files[0];
	} //}}}

	//{{{ getNextFile() method
	public synchronized String getNextFile(View view, String path)
	{
		if(files == null)
			files = _getFiles(view);

		if(files == null || files.length == 0)
			return null;

		if(path == null)
		{
			path = view.getBuffer().getSymlinkPath();
			VFS vfs = VFSManager.getVFSForPath(path);
			boolean ignoreCase = ((vfs.getCapabilities()
				& VFS.CASE_INSENSITIVE_CAP) != 0);

			for(int i = 0; i < files.length; i++)
			{
				if(StandardUtilities.compareStrings(
					files[i],path,ignoreCase) == 0)
				{
					return path;
				}
			}

			return getFirstFile(view);
		}
		else
		{
			// -1 so that the last isn't checked
			VFS vfs = VFSManager.getVFSForPath(path);
			boolean ignoreCase = ((vfs.getCapabilities()
				& VFS.CASE_INSENSITIVE_CAP) != 0);

			for(int i = 0; i < files.length - 1; i++)
			{
				if(StandardUtilities.compareStrings(
					files[i],path,ignoreCase) == 0)
				{
					return files[i+1];
				}
			}

			return null;
		}
	} //}}}

	//{{{ getFiles() method
	public synchronized String[] getFiles(View view)
	{
		if(files == null)
			files = _getFiles(view);

		if(files == null || files.length == 0)
			return null;
		else
			return files;
	} //}}}

	//{{{ getFileCount() method
	public synchronized int getFileCount(View view)
	{
		if(files == null)
			files = _getFiles(view);

		if(files == null)
			return 0;
		else
			return files.length;
	} //}}}

	//{{{ getCode() method
	public String getCode()
	{
		// not supported for arbitriary filesets
		return null;
	} //}}}

	//{{{ invalidateCachedList() method
	public void invalidateCachedList()
	{
		files = null;
	} //}}}

	/**
	 * Note that the paths in the returned list must be
	 * fully canonicalized.
	 */
	protected abstract String[] _getFiles(Component comp);

	private String[] files;
}
