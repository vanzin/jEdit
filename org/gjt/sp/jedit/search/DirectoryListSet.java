/*
 * DirectoryListSet.java - Directory list matcher
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import gnu.regexp.RE;
import java.awt.Component;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Recursive directory search.
 * @author Slava Pestov
 * @version $Id$
 */
public class DirectoryListSet extends BufferListSet
{
	//{{{ DirectoryListSet constructor
	public DirectoryListSet(String directory, String glob, boolean recurse)
	{
		this.directory = directory;
		this.glob = glob;
		this.recurse = recurse;
	} //}}}

	//{{{ getDirectory() method
	public String getDirectory()
	{
		return directory;
	} //}}}

	//{{{ getFileFilter() method
	public String getFileFilter()
	{
		return glob;
	} //}}}

	//{{{ isRecursive() method
	public boolean isRecursive()
	{
		return recurse;
	} //}}}

	//{{{ getCode() method
	public String getCode()
	{
		return "new DirectoryListSet(\"" + MiscUtilities.charsToEscapes(directory)
			+ "\",\"" + MiscUtilities.charsToEscapes(glob) + "\","
			+ recurse + ")";
	} //}}}

	//{{{ _getFiles() method
	protected String[] _getFiles(Component comp)
	{
		VFS vfs = VFSManager.getVFSForPath(directory);
		Object session = vfs.createVFSSession(directory,comp);
		if(session == null)
			return null;

		try
		{
			return vfs._listDirectory(session,directory,glob,recurse,comp);
		}
		catch(IOException io)
		{
			VFSManager.error(comp,directory,"ioerror",new String[]
				{ io.toString() });
			return null;
		}
	} //}}}

	//{{{ Private members
	private String directory;
	private String glob;
	private boolean recurse;
	//}}}
}
