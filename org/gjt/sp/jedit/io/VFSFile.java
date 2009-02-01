/*
 * VFSFile.java - A file residing on a virtual file system
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2005 Slava Pestov
 * Portions copyright (C) 2007 Matthieu Casanova
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
import java.awt.Color;
import java.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.browser.FileCellRenderer;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import javax.swing.*;
//}}}

/**
 * A directory entry returned from a file listing.
 * @since jEdit 4.3pre2
 */
public class VFSFile implements Serializable
{
	//{{{ findCompletion() method
	/**
	 * Return the index of a file whose name matches the given string,
	 * in a case-insensitive manner. Exact matches are preferred.
	 * @param files The list of files
	 * @param start The start index, inclusive
	 * @param end The end index, exclusive
	 * @param str The string to match
	 * @param dirsOnly Only match directories?
	 * @since jEdit 4.3pre3
	 */
	public static int findCompletion(VFSFile[] files, int start, int end,
		String str, boolean dirsOnly)
	{
		for(int i = start; i < end; i++)
		{
			VFSFile file = files[i];
			String matchAgainst = (MiscUtilities.isAbsolutePath(str)
				? file.getPath() : file.getName());

			if(dirsOnly && file.getType() == FILE)
				continue;
			/* try exact match first */
			else if(matchAgainst.equals(str))
				return i;
			else if(matchAgainst.regionMatches(true,0,str,0,str.length()))
				return i;
		}

		return -1;
	} //}}}

	//{{{ findCompletion() method
	public static String findCompletion(String path, String complete,
		VFSBrowser browser, boolean dirsOnly)
	{
		Log.log(Log.DEBUG,VFSFile.class,"findCompletion(" + path + ',' + complete
			+ ',' + dirsOnly + ')');

		if(complete.equals("~"))
			return System.getProperty("user.home");
		else if(complete.equals("-"))
			return browser.getView().getBuffer().getDirectory();
		else if(complete.equals(".."))
			return MiscUtilities.getParentOfPath(path);

		if(MiscUtilities.isAbsolutePath(complete))
		{
			if(MiscUtilities.isURL(complete))
				return complete;
			else
				path = "roots:";
		}

		VFS vfs = VFSManager.getVFSForPath(path);
		if((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) == 0)
			return null;
		Object session = vfs.createVFSSession(path,browser);
		if(session == null)
			return null;

		try
		{
			VFSFile[] files = vfs._listFiles(session,path,browser);
			int index = findCompletion(files,0,files.length,complete,dirsOnly);
			if(index != -1)
				return files[index].path;
		}
		catch(IOException e)
		{
			VFSManager.error(e,path,browser);
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException e)
			{
				VFSManager.error(e,path,browser);
			}
		}
		
		return null;
	} //}}}

	//{{{ getIcon() method
	/**
	 * Returns the icon for the file.
	 *
	 * @since jEdit 4.3pre9
	 */
	public final Icon getIcon(boolean expanded)
	{
		return getIcon(expanded, jEdit._getBuffer(getSymlinkPath()) != null);
	} //}}}

	//{{{ getIcon() method
	/**
	 * Returns the icon for the file.
	 * Implementations of File system browsers can override this method
	 *  
	 * @since jEdit 4.3pre9
	 */
	public Icon getIcon(boolean expanded, boolean openBuffer)
	{
		return getDefaultIcon(expanded, openBuffer);
	} //}}}

	//{{{ getDefaultIcon() method
	/**
	 * Returns the default icon for the file.
	 *
	 * @since jEdit 4.3pre9
	 */
	public final Icon getDefaultIcon(boolean expanded, boolean openBuffer)
	{
		if(getType() == DIRECTORY)
			return expanded ? FileCellRenderer.openDirIcon : FileCellRenderer.dirIcon;
		else if(getType() == FILESYSTEM)
			return FileCellRenderer.filesystemIcon;
		else if(openBuffer)
			return FileCellRenderer.openFileIcon;
		else
			return FileCellRenderer.fileIcon;
	} //}}}

	//{{{ getDefaultIcon() method
	/**
	 * Returns the default icon of the file.
	 *
	 * @return the default icon of the file
	 * @since jEdit 4.3pre9
	 */
	public final Icon getDefaultIcon(boolean expanded)
	{
		return getDefaultIcon(expanded, jEdit._getBuffer(getSymlinkPath()) != null);
	} //}}}

	//{{{ File types
	public static final int FILE = 0;
	public static final int DIRECTORY = 1;
	public static final int FILESYSTEM = 2;
	//}}}

	//{{{ Instance variables
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public String name;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public String path;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public String symlinkPath;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public String deletePath;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public int type;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public long length;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public boolean hidden;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public boolean canRead;
	/**
	 * @deprecated Use the accessor/mutator methods instead.
	 */
	public boolean canWrite;
	//}}}

	//{{{ VFSFile constructor
	/**
	 * @since jEdit 4.3pre2
	 */
	public VFSFile()
	{
	} //}}}

	//{{{ VFSFile constructor
	public VFSFile(String name, String path, String deletePath,
		int type, long length, boolean hidden)
	{
		this.name = name;
		this.path = path;
		this.deletePath = deletePath;
		this.symlinkPath = path;
		this.type = type;
		this.length = length;
		this.hidden = hidden;
		if(path != null)
		{
			// maintain backwards compatibility
			VFS vfs = VFSManager.getVFSForPath(path);
			canRead = ((vfs.getCapabilities() & VFS.READ_CAP) != 0);
			canWrite = ((vfs.getCapabilities() & VFS.WRITE_CAP) != 0);
		}
	} //}}}

	//{{{ getVFS() method
	/**
	 * @return The originating virtual file system of this file.
	 */
	public VFS getVFS()
	{
		return VFSManager.getVFSForPath(path);
	} //}}}
	
	//{{{ getName() method
	public String getName()
	{
		return name;
	} //}}}

	//{{{ setName() method
	public void setName(String name)
	{
		this.name = name;
	} //}}}

	//{{{ isBinary() method
	/**
	 * Check if a file is binary file.
	 *
	 * @param session the VFS session
	 * @return <code>true</code> if the file was detected as binary
	 * @throws IOException IOException If an I/O error occurs
	 * @since jEdit 4.3pre5
	 */
	public boolean isBinary(Object session)
		throws IOException
	{
		InputStream in = getVFS()._createInputStream(session,getPath(),
			false,jEdit.getActiveView());
		if(in == null)
			throw new IOException("Unable to get a Stream for this file "+this);

		try
		{
			return MiscUtilities.isBinary(in);
		}
		finally
		{
			IOUtilities.closeQuietly(in);
		}
	} //}}}

	//{{{ getPath() method
	public String getPath()
	{
		return path;
	} //}}}

	//{{{ setPath() method
	public void setPath(String path)
	{
		this.path = path;
	} //}}}

	//{{{ getSymlinkPath() method
	public String getSymlinkPath()
	{
		return symlinkPath;
	} //}}}

	//{{{ setSymlinkPath() method
	public void setSymlinkPath(String symlinkPath)
	{
		this.symlinkPath = symlinkPath;
	} //}}}

	//{{{ getDeletePath() method
	public String getDeletePath()
	{
		return deletePath;
	} //}}}

	//{{{ setDeletePath() method
	public void setDeletePath(String deletePath)
	{
		this.deletePath = deletePath;
	} //}}}

	//{{{ getType() method
	public int getType()
	{
		return type;
	} //}}}

	//{{{ setType() method
	public void setType(int type)
	{
		this.type = type;
	} //}}}

	//{{{ getLength() method
	public long getLength()
	{
		return length;
	} //}}}

	//{{{ setLength() method
	public void setLength(long length)
	{
		this.length = length;
	} //}}}

	//{{{ isHidden() method
	public boolean isHidden()
	{
		return hidden;
	} //}}}

	//{{{ setHidden() method
	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	} //}}}

	//{{{ isReadable() method
	public boolean isReadable()
	{
		return canRead;
	} //}}}

	//{{{ setReadable() method
	public void setReadable(boolean canRead)
	{
		this.canRead = canRead;
	} //}}}

	//{{{ isWriteable() method
	public boolean isWriteable()
	{
		return canWrite;
	} //}}}

	//{{{ setWriteable() method
	public void setWriteable(boolean canWrite)
	{
		this.canWrite = canWrite;
	} //}}}

	protected boolean colorCalculated;
	protected Color color;

	//{{{ getExtendedAttribute() method
	/**
	 * Returns the value of an extended attribute. Note that this
	 * returns formatted strings (eg, "10 Mb" for a file size of
	 * 1048576 bytes). If you need access to the raw data, access
	 * fields and methods of this class.
	 * @param name The extended attribute name
	 * @since jEdit 4.2pre1
	 */
	public String getExtendedAttribute(String name)
	{
		if(name.equals(VFS.EA_TYPE))
		{
			switch(getType())
			{
			case FILE:
				return jEdit.getProperty("vfs.browser.type.file");
			case DIRECTORY:
				return jEdit.getProperty("vfs.browser.type.directory");
			case FILESYSTEM:
				return jEdit.getProperty("vfs.browser.type.filesystem");
			default:
				throw new IllegalArgumentException();
			}
		}
		else if(name.equals(VFS.EA_STATUS))
		{
			if(isReadable())
			{
				if(isWriteable())
					return jEdit.getProperty("vfs.browser.status.rw");
				else
					return jEdit.getProperty("vfs.browser.status.ro");
			}
			else
			{
				if(isWriteable())
					return jEdit.getProperty("vfs.browser.status.append");
				else
					return jEdit.getProperty("vfs.browser.status.no");
			}
		}
		else if(name.equals(VFS.EA_SIZE))
		{
			if(getType() != FILE)
				return null;
			else
				return MiscUtilities.formatFileSize(getLength());
		}
		else
			return null;
	} //}}}

	//{{{ getColor() method
	/**
	 * Returns the color that will be used to display the file.
	 *
	 * @return the color of the file
	 */
	public Color getColor()
	{
		if(!colorCalculated)
		{
			colorCalculated = true;
			color = VFS.getDefaultColorFor(name);
		}

		return color;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return name;
	} //}}}
	
	//{{{ fetchedAttrs() method
	/**
	 * Returns true if the attributes are already fetched.
	 *
	 * @see #fetchAttrs()
	 * @return <code>true</code> if the attributes are already fetched
	 */
	protected boolean fetchedAttrs()
	{
		return fetchedAttrs;
	} //}}}
	
	//{{{ fetchAttrs() method
	/**
	 * Fetch some attributes of the file.
	 * Some attributes are not fetched during
	 * file initialization because it takes time.
	 * They are fetched here.
	 * VFS implementation should overwrite this
	 */
	protected void fetchAttrs()
	{
		fetchedAttrs = true;
	} //}}}

	/** This is true if the attributes are already fetched. */
	private boolean fetchedAttrs;
}
