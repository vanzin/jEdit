/*
 * BrowserIORequest.java - VFS browser I/O request
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.io.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * A browser I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
class BrowserIORequest extends WorkRequest
{
	//{{{ Request types
	/**
	 * Directory listing I/O request.
	 */
	public static final int LIST_DIRECTORY = 0;

	/**
	 * Delete file I/O request.
	 */
	public static final int DELETE = 1;

	/**
	 * Rename file I/O request.
	 */
	public static final int RENAME = 2;

	/**
	 * Make directory I/O request.
	 */
	public static final int MKDIR = 3;
	//}}}

	//{{{ BrowserIORequest constructor
	/**
	 * Creates a new browser I/O request.
	 * @param type The request type
	 * @param browser The VFS browser instance
	 * @param path1 The first path name to operate on
	 * @param path2 The second path name to operate on
	 * @param loadInfo A two-element array filled out by the request;
	 * element 1 is the canonical path, element 2 is the file list.
	 */
	BrowserIORequest(int type, VFSBrowser browser,
		Object session, VFS vfs, String path1, String path2,
		Object[] loadInfo)
	{
		this.type = type;
		this.browser = browser;
		this.session = session;
		this.vfs = vfs;
		this.path1 = path1;
		this.path2 = path2;
		this.loadInfo = loadInfo;
	} //}}}

	//{{{ run() method
	public void run()
	{
		switch(type)
		{
		case LIST_DIRECTORY:
			listDirectory();
			break;
		case DELETE:
			delete();
			break;
		case RENAME:
			rename();
			break;
		case MKDIR:
			mkdir();
			break;
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		String typeString;
		switch(type)
		{
		case LIST_DIRECTORY:
			typeString = "LIST_DIRECTORY";
			break;
		case DELETE:
			typeString = "DELETE";
			break;
		case RENAME:
			typeString = "RENAME";
			break;
		case MKDIR:
			typeString = "MKDIR";
			break;
		default:
			typeString = "UNKNOWN!!!";
			break;
		}

		return getClass().getName() + "[type=" + typeString
			+ ",vfs=" + vfs + ",path1=" + path1
			+ ",path2=" + path2 + "]";
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private int type;
	private VFSBrowser browser;
	private Object session;
	private VFS vfs;
	private String path1;
	private String path2;
	private Object[] loadInfo;
	//}}}

	//{{{ listDirectory() method
	private void listDirectory()
	{
		VFSFile[] directory = null;

		String[] args = { path1 };
		setStatus(jEdit.getProperty("vfs.status.listing-directory",args));

		String canonPath = path1;

		try
		{
			setAbortable(true);

			canonPath = vfs._canonPath(session,path1,browser);
			directory = vfs._listFiles(session,canonPath,browser);
		}
		catch(IOException io)
		{
			setAbortable(false);
			Log.log(Log.ERROR,this,io);
			String[] pp = { io.toString() };
			VFSManager.error(browser,path1,"ioerror.directory-error",pp);
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(browser,path1,"ioerror.directory-error",pp);
			}
		}

		setAbortable(false);

		loadInfo[0] = canonPath;
		loadInfo[1] = directory;
	} //}}}

	//{{{ delete() method
	private void delete()
	{
		try
		{
			setAbortable(true);
			String[] args = { path1 };
			setStatus(jEdit.getProperty("vfs.status.deleting",args));

			try
			{
				path1 = vfs._canonPath(session,path1,browser);


				if(!vfs._delete(session,path1,browser))
					VFSManager.error(browser,path1,"ioerror.delete-error",null);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(browser,path1,"ioerror.directory-error",pp);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(browser,path1,"ioerror.directory-error",pp);
			}
		}
	} //}}}

	//{{{ rename() method
	private void rename()
	{
		try
		{
			setAbortable(true);
			String[] args = { path1, path2 };
			setStatus(jEdit.getProperty("vfs.status.renaming",args));

			try
			{
				path1 = vfs._canonPath(session,path1,browser);
				path2 = vfs._canonPath(session,path2,browser);

				VFSFile file = vfs._getFile(session,path2,browser);
				if(file != null)
				{
					if((OperatingSystem.isCaseInsensitiveFS())
						&& path1.equalsIgnoreCase(path2))
					{
						// allow user to change name
						// case
					}
					else
					{
						VFSManager.error(browser,path1,
							"ioerror.rename-exists",
							new String[] { path2 });
						return;
					}
				}

				if(!vfs._rename(session,path1,path2,browser))
					VFSManager.error(browser,path1,"ioerror.rename-error",
						new String[] { path2 });
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(browser,path1,"ioerror.directory-error",pp);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(browser,path1,"ioerror.directory-error",pp);
			}
		}
	} //}}}

	//{{{ mkdir() method
	private void mkdir()
	{
		try
		{
			setAbortable(true);
			String[] args = { path1 };
			setStatus(jEdit.getProperty("vfs.status.mkdir",args));

			try
			{
				path1 = vfs._canonPath(session,path1,browser);

				if(!vfs._mkdir(session,path1,browser))
					VFSManager.error(browser,path1,"ioerror.mkdir-error",null);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				args[0] = io.toString();
				VFSManager.error(browser,path1,"ioerror",args);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				setAbortable(false);
				Log.log(Log.ERROR,this,io);
				String[] args = { io.toString() };
				VFSManager.error(browser,path1,"ioerror",args);
			}
		}
	} //}}}

	//}}}
}
