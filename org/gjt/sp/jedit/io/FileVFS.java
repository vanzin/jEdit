/*
 * FileVFS.java - Local filesystem VFS
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

import java.awt.Component;
import java.lang.reflect.Method;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Local filesystem VFS.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileVFS extends VFS
{
	public static final String BACKED_UP_PROPERTY = "FileVFS__backedUp";
	public static final String PERMISSIONS_PROPERTY = "FileVFS__perms";

	public FileVFS()
	{
		super("file");
	}

	public int getCapabilities()
	{
		return READ_CAP | WRITE_CAP | BROWSE_CAP | DELETE_CAP
			| RENAME_CAP | MKDIR_CAP;
	}

	public String getParentOfPath(String path)
	{
		// handle Windows differently
		if(File.separatorChar == '\\')
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				return FileRootsVFS.PROTOCOL + ":";
			else if(path.length() == 3 && path.endsWith(":\\"))
				return FileRootsVFS.PROTOCOL + ":";
		}

		if(path.equals("/"))
			return FileRootsVFS.PROTOCOL + ":";

		return MiscUtilities.getParentOfPath(path);
	}

	public String constructPath(String parent, String path)
	{
		return MiscUtilities.constructPath(parent,path);
	}

	public char getFileSeparator()
	{
		return File.separatorChar;
	}

	public boolean load(View view, Buffer buffer, String path)
	{
		File file = buffer.getFile();

		if(!file.exists())
		{
			buffer.setNewFile(true);
			return false;
		}
		else
			buffer.setReadOnly(!file.canWrite());

		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"open-directory",args);
			buffer.setNewFile(false);
			return false;
		}

		if(!file.canRead())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"no-read",args);
			buffer.setNewFile(false);
			return false;
		}

		return super.load(view,buffer,path);
	}

	public boolean save(View view, Buffer buffer, String path)
	{
		// can't call buffer.getFile() here because this
		// method is called *before* setPath()
		File file = new File(path);

		// Apparently, certain broken OSes (like Micro$oft Windows)
		// can mess up directories if they are write()'n to
		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"save-directory",args);
			return false;
		}

		// Check that we can actually write to the file
		if((file.exists() && !file.canWrite())
			|| (!file.exists() && !new File(file.getParent()).canWrite()))
		{
			String[] args = { path };
			GUIUtilities.error(view,"no-write",args);
			return false;
		}

		// On Unix, preserve permissions
		int permissions = getPermissions(buffer.getPath());
		Log.log(Log.DEBUG,this,buffer.getPath() + " has permissions 0"
			+ Integer.toString(permissions,8));
		buffer.putProperty(PERMISSIONS_PROPERTY,new Integer(permissions));

		return super.save(view,buffer,path);
	}

	public boolean insert(View view, Buffer buffer, String path)
	{
		File file = new File(path);

		if(!file.exists())
			return false;

		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"open-directory",args);
			return false;
		}

		if(!file.canRead())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"no-read",args);
			return false;
		}

		return super.load(view,buffer,path);
	}

	public VFS.DirectoryEntry[] _listDirectory(Object session, String path,
		Component comp)
	{
		/* Fix for the bug where listing a drive letter on Windows
		 * doesn't work. On Windows, paths of the form X: list the
		 * last *working directory* on that drive. To list the root
		 * of the drive, you must use X:\.
		 *
		 * However, the VFS browser and friends strip off trailing
		 * path separators, for various reasons. So to work around
		 * that, we add a '\' to drive letter paths on Windows.
		 */
		if(File.separatorChar == '\\')
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				path = path.concat(File.separator);
		}

		File directory = new File(path);
		String[] list = directory.list();
		if(list == null)
		{
			String[] pp = { path };
			VFSManager.error(comp,"directory-error-nomsg",pp);
			return null;
		}

		Vector list2 = new Vector();
		for(int i = 0; i < list.length; i++)
		{
			String name = list[i];
			String _path;
			if(path.endsWith(File.separator))
				_path = path + name;
			else
				_path = path + File.separatorChar + name;

			VFS.DirectoryEntry entry = _getDirectoryEntry(null,_path,null);

			if(entry != null)
				list2.addElement(entry);
		}

		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[list2.size()];
		list2.copyInto(retVal);
		return retVal;
	}

	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
	{
		// workaround for Java bug where paths with trailing / return
		// null getName()
		if(path.endsWith("/") || path.endsWith(File.separator))
			path = path.substring(0,path.length() - 1);

		File file = new File(path);
		if(!file.exists())
			return null;

		int type;
		if(file.isDirectory())
			type = VFS.DirectoryEntry.DIRECTORY;
		else
			type = VFS.DirectoryEntry.FILE;

		boolean hidden;
		if(isHiddenMethod != null)
		{
			try
			{
				hidden = Boolean.TRUE.equals(isHiddenMethod.invoke(
					file,new Object[0]));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
				hidden = false;
			}
		}
		else if(isUnix)
			hidden = (file.getName().charAt(0) == '.');
		else
			hidden = false;

		return new VFS.DirectoryEntry(file.getName(),path,path,type,
			file.length(),hidden);
	}

	public boolean _delete(Object session, String path, Component comp)
	{
		boolean retVal = new File(path).delete();
		if(retVal)
			VFSManager.sendVFSUpdate(this,path,true);
		return retVal;
	}

	public boolean _rename(Object session, String from, String to,
		Component comp)
	{
		File _to = new File(to);
		_to.delete();
		boolean retVal = new File(from).renameTo(_to);
		VFSManager.sendVFSUpdate(this,from,true);
		VFSManager.sendVFSUpdate(this,to,true);
		return retVal;
	}

	public boolean _mkdir(Object session, String directory, Component comp)
	{
		boolean retVal = new File(directory).mkdirs();
		VFSManager.sendVFSUpdate(this,directory,true);
		return retVal;
	}

	public void _backup(Object session, String path, Component comp)
		throws IOException
	{
		// Fetch properties
		int backups;
		try
		{
			backups = Integer.parseInt(jEdit.getProperty(
				"backups"));
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,this,nf);
			backups = 1;
		}

		if(backups == 0)
			return;

		String backupPrefix = jEdit.getProperty("backup.prefix","");
		String backupSuffix = jEdit.getProperty("backup.suffix","~");

		File file = new File(path);

		// Check for backup.directory property, and create that
		// directory if it doesn't exist
		String backupDirectory = jEdit.getProperty("backup.directory");
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

		String name = file.getName();

		// If backups is 1, create ~ file
		if(backups == 1)
		{
			file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix));
		}
		// If backups > 1, move old ~n~ files, create ~1~ file
		else
		{
			new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ backups + backupSuffix).delete();

			for(int i = backups - 1; i > 0; i--)
			{
				File backup = new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ i + backupSuffix);

				backup.renameTo(new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ (i+1) + backupSuffix));
			}

			file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ "1" + backupSuffix));
		}
	}

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
	}

	public OutputStream _createOutputStream(Object session, String path,
		Component comp) throws IOException
	{
		OutputStream retVal = new FileOutputStream(path);

		// commented out for now, because updating VFS browsers
		// every time file is saved gets annoying
		//VFSManager.sendVFSUpdate(this,path,true);
		return retVal;
	}

	public void _saveComplete(Object session, Buffer buffer, Component comp)
	{
		int permissions = ((Integer)buffer.getProperty(PERMISSIONS_PROPERTY))
			.intValue();
		setPermissions(buffer.getPath(),permissions);
	}

	/** Code borrowed from j text editor (http://www.armedbear.org) */
	/** I made some changes to make it support suid, sgid and sticky files */

	/**
	 * Returns numeric permissions of a file. On non-Unix systems, always
	 * returns zero.
	 * @since jEdit 3.2pre9
	 */
	public static int getPermissions(String path)
	{
		int permissions = 0;

		if(isUnix)
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

					if(s.length() == 9)
					{
						if(s.charAt(0) == 'r')
							permissions += 0400;
						if(s.charAt(1) == 'w')
							permissions += 0200;
						if(s.charAt(2) == 'x')
							permissions += 0100;
						else if(s.charAt(2) == 's')
							permissions += 04100;
						else if(s.charAt(2) == 'S')
							permissions += 04000;
						if(s.charAt(3) == 'r')
							permissions += 040;
						if(s.charAt(4) == 'w')
							permissions += 020;
						if(s.charAt(5) == 'x')
							permissions += 010;
						else if(s.charAt(5) == 's')
							permissions += 02010;
						else if(s.charAt(5) == 'S')
							permissions += 02000;
						if(s.charAt(6) == 'r')
							permissions += 04;
						if(s.charAt(7) == 'w')
							permissions += 02;
						if(s.charAt(8) == 'x')
							permissions += 01;
						else if(s.charAt(8) == 't')
							permissions += 01001;
						else if(s.charAt(8) == 'T')
							permissions += 01000;
					}
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
	}

	/**
	 * Sets numeric permissions of a file. On non-Unix platforms,
	 * does nothing.
	 * @since jEdit 3.2pre9
	 */
	public static void setPermissions(String path, int permissions)
	{
		if(permissions != 0)
		{
			if(isUnix)
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
	}
	
	// private members
	private static boolean isUnix;
	private static Method isHiddenMethod;

	static
	{
		// If the file separator is '/', the OS is either Unix,
		// MacOS X, or MacOS.
		if(File.separatorChar == '/')
		{
			String osName = System.getProperty("os.name");
			if(osName.indexOf("Mac") != -1)
			{
				if(osName.indexOf("X") != -1)
				{
					// MacOS X is Unix.
					isUnix = true;
				}
				else
				{
					// Classic MacOS is definately not Unix.
					isUnix = false;
				}
			}
			else
			{
				// Unix.
				isUnix = true;
			}
		}

		Log.log(Log.DEBUG,FileVFS.class,"Unix operating system "
			+ (isUnix ? "detected; will" : "not detected; will not")
			+ " use permission-preserving code");

		try
		{
			isHiddenMethod = File.class.getMethod("isHidden",new Class[0]);
			Log.log(Log.DEBUG,FileVFS.class,"File.isHidden() method"
				+ " detected");
		}
		catch(Exception e)
		{
			Log.log(Log.DEBUG,FileVFS.class,"File.isHidden() method"
				+ " not detected");
		}
	}
}
