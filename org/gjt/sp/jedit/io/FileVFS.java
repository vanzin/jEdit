/*
 * FileVFS.java - Local filesystem VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
 * Portions copyright (C) 1998, 1999, 2000 Peter Graves
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
import java.awt.Component;
import java.io.*;
import java.text.*;
import java.util.Date;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Local filesystem VFS.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileVFS extends VFS
{
	public static final String PERMISSIONS_PROPERTY = "FileVFS__perms";

	//{{{ FileVFS method
	public FileVFS()
	{
		super("file",READ_CAP | WRITE_CAP | DELETE_CAP
			| RENAME_CAP | MKDIR_CAP | LOW_LATENCY_CAP
			| ((OperatingSystem.isMacOS()
			|| OperatingSystem.isDOSDerived())
			? CASE_INSENSITIVE_CAP : 0),
			new String[] { EA_TYPE, EA_SIZE, EA_STATUS,
			EA_MODIFIED });
	} //}}}

	//{{{ getParentOfPath() method
	public String getParentOfPath(String path)
	{
		if(OperatingSystem.isDOSDerived())
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				return FileRootsVFS.PROTOCOL + ":";
			else if(path.length() == 3 && path.endsWith(":\\"))
				return FileRootsVFS.PROTOCOL + ":";
			else if(path.startsWith("\\\\") && path.indexOf('\\',2) == -1)
				return path;
		}

		return super.getParentOfPath(path);
	} //}}}

	//{{{ constructPath() method
	public String constructPath(String parent, String path)
	{
		if(parent.endsWith(File.separator)
			|| parent.endsWith("/"))
			return parent + path;
		else
			return parent + File.separator + path;
	} //}}}

	//{{{ getFileSeparator() method
	public char getFileSeparator()
	{
		return File.separatorChar;
	} //}}}

	//{{{ save() method
	public boolean save(View view, Buffer buffer, String path)
	{
		if(OperatingSystem.isUnix())
		{
			int permissions = getPermissions(buffer.getPath());
			Log.log(Log.DEBUG,this,buffer.getPath() + " has permissions 0"
				+ Integer.toString(permissions,8));
			buffer.setIntegerProperty(PERMISSIONS_PROPERTY,permissions);
		}

		return super.save(view,buffer,path);
	} //}}}

	//{{{ insert() method
	public boolean insert(View view, Buffer buffer, String path)
	{
		File file = new File(path);

		//{{{ Check if file is valid
		if(!file.exists())
			return false;

		if(file.isDirectory())
		{
			VFSManager.error(view,file.getPath(),
				"ioerror.open-directory",null);
			return false;
		}

		if(!file.canRead())
		{
			VFSManager.error(view,file.getPath(),
				"ioerror.no-read",null);
			return false;
		} //}}}

		return super.insert(view,buffer,path);
	} //}}}

	//{{{ _canonPath() method
	/**
	 * Returns the canonical form if the specified path name. For example,
	 * <code>~</code> might be expanded to the user's home directory.
	 * @param session The session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 4.0pre2
	 */
	public String _canonPath(Object session, String path, Component comp)
		throws IOException
	{
		return MiscUtilities.canonPath(path);
	} //}}}

	//{{{ LocalDirectoryEntry class
	public static class LocalDirectoryEntry extends VFS.DirectoryEntry
	{
		public static DateFormat DATE_FORMAT
			= new SimpleDateFormat("dd/MM/yyyy hh:mm a");

		public long modified;

		public LocalDirectoryEntry(File file)
		{
			super(file.getName(),file.getPath(),
				file.getPath(),file.isDirectory() ? DIRECTORY : FILE,file.length(),file.isHidden());
			this.modified = file.lastModified();
			this.canRead = file.canRead();
			this.canWrite = file.canWrite();
			this.symlinkPath = MiscUtilities.resolveSymlinks(path);
		}

		public String getExtendedAttribute(String name)
		{
			if(name.equals(EA_MODIFIED))
				return DATE_FORMAT.format(new Date(modified));
			else
				return super.getExtendedAttribute(name);
		}
	} //}}}

	//{{{ _listDirectory() method
	public VFS.DirectoryEntry[] _listDirectory(Object session, String path,
		Component comp)
	{
		//{{{ Windows work around
		/* On Windows, paths of the form X: list the last *working
		 * directory* on that drive. To list the root of the drive,
		 * you must use X:\.
		 *
		 * However, the VFS browser and friends strip off trailing
		 * path separators, for various reasons. So to work around
		 * that, we add a '\' to drive letter paths on Windows.
		 */
		if(OperatingSystem.isWindows())
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				path = path.concat(File.separator);
		} //}}}

		File directory = new File(path);
		File[] list = directory.listFiles();
		if(list == null)
		{
			VFSManager.error(comp,path,"ioerror.directory-error-nomsg",null);
			return null;
		}

		VFS.DirectoryEntry[] list2 = new VFS.DirectoryEntry[list.length];
		for(int i = 0; i < list.length; i++)
			list2[i] = new LocalDirectoryEntry(list[i]);

		return list2;
	} //}}}

	//{{{ _getDirectoryEntry() method
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		if(path.equals("/") && OperatingSystem.isUnix())
		{
			return new VFS.DirectoryEntry(path,path,path,
				VFS.DirectoryEntry.DIRECTORY,0L,false);
		}

		File file = new File(path);
		if(!file.exists())
			return null;

		return new LocalDirectoryEntry(file);
	} //}}}

	//{{{ _delete() method
	public boolean _delete(Object session, String path, Component comp)
	{
		File file = new File(path);
		// do some platforms throw exceptions if the file does not exist
		// when we ask for the canonical path?
		String canonPath;
		try
		{
			canonPath = file.getCanonicalPath();
		}
		catch(IOException io)
		{
			canonPath = path;
		}

		boolean retVal = file.delete();
		if(retVal)
			VFSManager.sendVFSUpdate(this,canonPath,true);
		return retVal;
	} //}}}

	//{{{ _rename() method
	public boolean _rename(Object session, String from, String to,
		Component comp)
	{
		File _to = new File(to);

		String toCanonPath;
		try
		{
			toCanonPath = _to.getCanonicalPath();
		}
		catch(IOException io)
		{
			toCanonPath = to;
		}

		// this is needed because on OS X renaming to a non-existent
		// directory causes problems
		File parent = new File(_to.getParent());
		if(parent.exists())
		{
			if(!parent.isDirectory())
				return false;
		}
		else
		{
			parent.mkdirs();
			if(!parent.exists())
				return false;
		}

		File _from = new File(from);

		String fromCanonPath;
		try
		{
			fromCanonPath = _from.getCanonicalPath();
		}
		catch(IOException io)
		{
			fromCanonPath = from;
		}

		// Case-insensitive fs workaround
		if(!fromCanonPath.equalsIgnoreCase(toCanonPath))
			_to.delete();

		boolean retVal = _from.renameTo(_to);
		VFSManager.sendVFSUpdate(this,fromCanonPath,true);
		VFSManager.sendVFSUpdate(this,toCanonPath,true);
		return retVal;
	} //}}}

	//{{{ _mkdir() method
	public boolean _mkdir(Object session, String directory, Component comp)
	{
		String parent = getParentOfPath(directory);
		if(!new File(parent).exists())
		{
			if(!_mkdir(session,parent,comp))
				return false;
		}

		File file = new File(directory);

		boolean retVal = file.mkdir();
		String canonPath;
		try
		{
			canonPath = file.getCanonicalPath();
		}
		catch(IOException io)
		{
			canonPath = directory;
		}
		VFSManager.sendVFSUpdate(this,canonPath,true);
		return retVal;
	} //}}}

	//{{{ _backup() method
	public void _backup(Object session, String path, Component comp)
		throws IOException
	{
		// Fetch properties
		int backups = jEdit.getIntegerProperty("backups",1);

		if(backups == 0)
			return;

		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");

		String backupDirectory = jEdit.getProperty("backup.directory");

		int backupTimeDistance = jEdit.getIntegerProperty("backup.minTime",0);
		File file = new File(path);

		// Check for backup.directory, and create that
		// directory if it doesn't exist
		if(backupDirectory == null || backupDirectory.length() == 0)
			backupDirectory = file.getParent();
		else
		{
			backupDirectory = MiscUtilities.constructPath(
				System.getProperty("user.home"),backupDirectory);

			// Perhaps here we would want to guard with
			// a property for parallel backups or not.
			backupDirectory = MiscUtilities.concatPath(
				backupDirectory,file.getParent());

			File dir = new File(backupDirectory);

			if (!dir.exists())
				dir.mkdirs();
		}

		MiscUtilities.saveBackup(file,backups,backupPrefix,
			backupSuffix,backupDirectory,backupTimeDistance);
	} //}}}

	//{{{ _createInputStream() method
	public InputStream _createInputStream(Object session, String path,
		boolean ignoreErrors, Component comp) throws IOException
	{
		try
		{
			return new FileInputStream(path);
		}
		catch(IOException io)
		{
			if(ignoreErrors)
				return null;
			else
				throw io;
		}
	} //}}}

	//{{{ _createOutputStream() method
	public OutputStream _createOutputStream(Object session, String path,
		Component comp) throws IOException
	{
		return new FileOutputStream(path);
	} //}}}

	//{{{ _saveComplete() method
	public void _saveComplete(Object session, Buffer buffer, String path,
		Component comp)
	{
		int permissions = buffer.getIntegerProperty(PERMISSIONS_PROPERTY,0);
		setPermissions(path,permissions);
	} //}}}

	//{{{ Permission preservation code

	/** Code borrowed from j text editor (http://www.armedbear.org) */
	/** I made some changes to make it support suid, sgid and sticky files */

	//{{{ getPermissions() method
	/**
	 * Returns numeric permissions of a file. On non-Unix systems, always
	 * returns zero.
	 * @since jEdit 3.2pre9
	 */
	public static int getPermissions(String path)
	{
		int permissions = 0;

		if(jEdit.getBooleanProperty("chmodDisabled"))
			return permissions;

		if(OperatingSystem.isUnix())
		{
			String[] cmdarray = { "ls", "-ld", path };

			try
			{
				Process process = Runtime.getRuntime().exec(cmdarray);

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String output = reader.readLine();

				if(output != null)
				{
					String s = output.substring(1, 10);

					permissions = MiscUtilities
						.parsePermissions(s);
				}
			}

			// Feb 4 2000 5:30 PM
			// Catch Throwable here rather than Exception.
			// Kaffe's implementation of Runtime.exec throws java.lang.InternalError.
			catch (Throwable t)
			{
			}
		}

		return permissions;
	} //}}}

	//{{{ setPermissions() method
	/**
	 * Sets numeric permissions of a file. On non-Unix platforms,
	 * does nothing.
	 * @since jEdit 3.2pre9
	 */
	public static void setPermissions(String path, int permissions)
	{
		if(jEdit.getBooleanProperty("chmodDisabled"))
			return;

		if(permissions != 0)
		{
			if(OperatingSystem.isUnix())
			{
				String[] cmdarray = { "chmod", Integer.toString(permissions, 8), path };

				try
				{
					Process process = Runtime.getRuntime().exec(cmdarray);
					process.getInputStream().close();
					process.getOutputStream().close();
					process.getErrorStream().close();
					int exitCode = process.waitFor();
					if(exitCode != 0)
						Log.log(Log.NOTICE,FileVFS.class,"chmod exited with code " + exitCode);
				}

				// Feb 4 2000 5:30 PM
				// Catch Throwable here rather than Exception.
				// Kaffe's implementation of Runtime.exec throws java.lang.InternalError.
				catch (Throwable t)
				{
				}
			}
		}
	} //}}}

	//}}}
}
