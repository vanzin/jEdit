/*
 * DirectoryListSet.java - Directory list matcher
 * Copyright (C) 1999, 2000 Slava Pestov
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

import gnu.regexp.RE;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Recursive directory search.
 * @author Slava Pestov
 * @version $Id$
 */
public class DirectoryListSet extends BufferListSet
{
	public DirectoryListSet(String directory, String glob, boolean recurse)
	{
		super(listFiles(directory,glob,recurse));

		this.directory = directory;
		this.glob = glob;
		this.recurse = recurse;
	}

	public String getDirectory()
	{
		return directory;
	}

	public String getFileFilter()
	{
		return glob;
	}

	public boolean isRecursive()
	{
		return recurse;
	}

	/**
	 * Returns the BeanShell code that will recreate this file set.
	 * @since jEdit 2.7pre3
	 */
	public String getCode()
	{
		return "new DirectoryListSet(\"" + MiscUtilities.charsToEscapes(directory)
			+ "\",\"" + MiscUtilities.charsToEscapes(glob) + "\","
			+ recurse + ")";
	}

	// private members
	private String directory;
	private String glob;
	private boolean recurse;

	/**
	 * One day this might become public and move to MiscUtilities...
	 */
	private static Vector listFiles(String directory,
		String glob, boolean recurse)
	{
		Log.log(Log.DEBUG,DirectoryListSet.class,"Searching in "
			+ directory);
		Vector files = new Vector(50);

		RE filter;
		try
		{
			filter = new RE(MiscUtilities.globToRE(glob));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,DirectoryListSet.class,e);
			return files;
		}

		listFiles(new Vector(),files,new File(directory),filter,recurse);

		return files;
	}

	private static void listFiles(Vector stack, Vector files,
		File directory, RE filter, boolean recurse)
	{
		if(stack.contains(directory))
		{
			Log.log(Log.ERROR,DirectoryListSet.class,
				"Recursion in DirectoryListSet: "
				+ directory.getPath());
			return;
		}
		else
			stack.addElement(directory);
		
		String[] _files = directory.list();
		if(_files == null)
			return;

		MiscUtilities.quicksort(_files,new MiscUtilities.StringICaseCompare());

		for(int i = 0; i < _files.length; i++)
		{
			String name = _files[i];

			File file = new File(directory,name);
			if(file.isDirectory())
			{
				if(recurse)
					listFiles(stack,files,file,filter,recurse);
			}
			else
			{
				if(!filter.isMatch(name))
					continue;

				Log.log(Log.DEBUG,DirectoryListSet.class,file.getPath());
				String canonPath;
				try
				{
					canonPath = file.getCanonicalPath();
				}
				catch(IOException io)
				{
					canonPath = file.getPath();
				}
				files.addElement(canonPath);
			}
		}
	}
}
