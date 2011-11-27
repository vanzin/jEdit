/*
 * FileVFS.java - Local filesystem VFS
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2005 Slava Pestov
 * Portions copyright (C) 1998, 1999, 2000 Peter Graves
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
import javax.swing.filechooser.FileSystemView;
import javax.swing.*;
import java.awt.Component;
import java.io.*;
import java.text.*;
import java.util.Date;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.IOUtilities;
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

	//{{{ FileVFS constructor
	public FileVFS()
	{
		super("file",READ_CAP | WRITE_CAP | BROWSE_CAP | DELETE_CAP
			| RENAME_CAP | MKDIR_CAP | LOW_LATENCY_CAP
			| (OperatingSystem.isCaseInsensitiveFS()
			? CASE_INSENSITIVE_CAP : 0),
			new String[] { EA_TYPE, EA_SIZE, EA_STATUS,
			EA_MODIFIED });
	} //}}}

	//{{{ getParentOfPath() method
	@Override
	public String getParentOfPath(String path)
	{
		if(OperatingSystem.isWindows())
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				return FileRootsVFS.PROTOCOL + ':';
			else if(path.length() == 3 && path.endsWith(":\\"))
				return FileRootsVFS.PROTOCOL + ':';
			else if(path.startsWith("\\\\") && path.indexOf('\\',2) == -1)
				return path;
		}

		return super.getParentOfPath(path);
	} //}}}

	//{{{ constructPath() method
	@Override
	public String constructPath(String parent, String path)
	{
		if(parent.endsWith(File.separator)
			|| parent.endsWith("/"))
			return parent + path;
		else
			return parent + File.separator + path;
	} //}}}

	//{{{ getFileSeparator() method
	@Override
	public char getFileSeparator()
	{
		return File.separatorChar;
	} //}}}

	//{{{ getTwoStageSaveName() method
	/**
	 * Returns a temporary file name based on the given path.
	 *
	 * <p>If the directory where the file would be created cannot be
	 * written (i.e., no new files can be created in that directory),
	 * this method returns <code>null</code>.</p>
	 *
	 * @param path The path name
	 */
	@Override
	public String getTwoStageSaveName(String path)
	{
		File parent = new File(getParentOfPath(path));
		// the ignorance of the canWrite() method for windows
		// is, because the read-only flag on windows has
		// not the effect of preventing the creation of new files.
		// The only way to make a directory read-only in this means
		// the ACL of the directory has to be set to read-only,
		// which is not checkable by java.
		// The " || OperatingSystem.isWindows()" can be removed
		// if the canWrite() method gives back the right value.
		return (parent.canWrite() || OperatingSystem.isWindows())
			? super.getTwoStageSaveName(path)
			: null;
	} //}}}

	//{{{ save() method
	@Override
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
	@Override
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

	//{{{ recursiveDelete() method
	/**
	 * #
	 * @param path the directory path to recursive delete
	 * @return true if successful, else false
	 */
	public static boolean recursiveDelete(File path)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					recursiveDelete(files[i]);
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return path.delete();
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
	@Override
	public String _canonPath(Object session, String path, Component comp)
		throws IOException
	{
		return MiscUtilities.canonPath(path);
	} //}}}

	//{{{ LocalFile class
	public static class LocalFile extends VFSFile
	{
		private File file;

		// use system default short format
		public static DateFormat DATE_FORMAT
			= DateFormat.getInstance();

		private long modified;

		//{{{ LocalFile() class
		public LocalFile(File file)
		{
			this.file = file;

			/* These attributes are fetched relatively
			quickly. The rest are lazily filled in. */
			setName(file.getName());
			String path = file.getPath();
			setPath(path);
			setDeletePath(path);
			setHidden(file.isHidden());
			setType(file.isDirectory()
				? VFSFile.DIRECTORY
				: VFSFile.FILE);
		} //}}}

		//{{{ getExtendedAttribute() method
		@Override
		public String getExtendedAttribute(String name)
		{
			fetchAttrs();
			if (name.equals(EA_MODIFIED))
			{
				return DATE_FORMAT.format(new Date(modified));
			}
			else
			{
				return super.getExtendedAttribute(name);
			}
		} //}}}

		//{{{ fetchAttrs() method
		/** Fetch the attributes of the local file. */
		@Override
		protected void fetchAttrs()
		{
			if(fetchedAttrs())
				return;

			super.fetchAttrs();

			setSymlinkPath(MiscUtilities.resolveSymlinks(
				file.getPath()));
			setReadable(file.canRead());
			setWriteable(file.canWrite());
			setLength(file.length());
			setModified(file.lastModified());
		} //}}}

		//{{{ getIcon() method
		/**
		 * Returns the file system icon for the file.
		 *
		 * @param expanded not used here
		 * @param openBuffer not used here
		 * @return the file system icon
		 * @since 4.3pre9
		 */
		@Override
		public Icon getIcon(boolean expanded, boolean openBuffer)
		{
			if (icon == null)
			{
				if (fsView == null)
					fsView = FileSystemView.getFileSystemView();

				icon = fsView.getSystemIcon(file);
			}
			return icon;
		} //}}}

		//{{{ getSymlinkPath() method
		@Override
		public String getSymlinkPath()
		{
			fetchAttrs();
			return super.getSymlinkPath();
		} //}}}

		//{{{ getLength() method
		@Override
		public long getLength()
		{
			fetchAttrs();
			return super.getLength();
		} //}}}

		//{{{ isReadable() method
		@Override
		public boolean isReadable()
		{
			fetchAttrs();
			return super.isReadable();
		} //}}}

		//{{{ isWriteable() method
		@Override
		public boolean isWriteable()
		{
			fetchAttrs();
			return super.isWriteable();
		} //}}}

		//{{{ getModified() method
		public long getModified()
		{
			fetchAttrs();
			return modified;
		} //}}}

		//{{{ setModified() method
		public void setModified(long modified)
		{
			this.modified = modified;
		} //}}}

		private transient FileSystemView fsView;
		private transient Icon icon;
	} //}}}

	//{{{ _listFiles() method
	@Override
	public VFSFile[] _listFiles(Object session, String path,
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
		File[] list = null;
		if(directory.exists())
			list = fsView.getFiles(directory,false);

		if(list == null)
		{
			VFSManager.error(comp,path,"ioerror.directory-error-nomsg",null);
			return null;
		}

		VFSFile[] list2 = new VFSFile[list.length];
		for(int i = 0; i < list.length; i++)
			list2[i] = new LocalFile(list[i]);

		return list2;
	} //}}}

	//{{{ _getFile() method
	@Override
	public VFSFile _getFile(Object session, String path,
		Component comp)
	{
		if(path.equals("/") && OperatingSystem.isUnix())
		{
			return new VFSFile(path,path,path,
				VFSFile.DIRECTORY,0L,false);
		}

		File file = new File(path);
		if(!file.exists())
			return null;

		return new LocalFile(file);
	} //}}}

	//{{{ _delete() method
	@Override
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
		// if directory, do recursive delete
		boolean retVal;
		if (!file.isDirectory())
		{
			retVal = file.delete();
		}
		else
		{
			retVal = recursiveDelete(file);
		}
		if(retVal)
			VFSManager.sendVFSUpdate(this,canonPath,true);
		return retVal;
	} //}}}

	//{{{ _rename() method
	@Override
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
	@Override
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
	@Override
	public void _backup(Object session, String path, Component comp)
		throws IOException
	{
		// Fetch properties

		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");

		String backupDirectory = jEdit.getProperty("backup.directory");

		int backupTimeDistance = jEdit.getIntegerProperty("backup.minTime",0);
		File file = new File(path);

		if (!file.exists())
			return;

		// Check for backup.directory, and create that
		// directory if it doesn't exist
		if(backupDirectory == null || backupDirectory.length() == 0)
			backupDirectory = file.getParent();
		else
		{
			backupDirectory = MiscUtilities.constructPath(
				System.getProperty("user.home"), backupDirectory);

			// Perhaps here we would want to guard with
			// a property for parallel backups or not.
			backupDirectory = MiscUtilities.concatPath(
				backupDirectory,file.getParent());

			File dir = new File(backupDirectory);

			if (!dir.exists())
				dir.mkdirs();
		}

		MiscUtilities.saveBackup(file, jEdit.getIntegerProperty("backups",1), backupPrefix,
			backupSuffix,backupDirectory,backupTimeDistance);
	} //}}}

	//{{{ _createInputStream() method
	@Override
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
	@Override
	public OutputStream _createOutputStream(Object session, String path,
		Component comp) throws IOException
	{
		return new FileOutputStream(path);
	} //}}}

	//{{{ _saveComplete() method
	@Override
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
			String[] cmdarray = { "ls", "-lLd", path };

			InputStreamReader isr = null;
			BufferedReader reader = null;
			try
			{
				Process process = Runtime.getRuntime().exec(cmdarray);
				isr = new InputStreamReader(process.getInputStream());
				reader = new BufferedReader(isr);

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
			finally
			{
				IOUtilities.closeQuietly(reader);
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
					// Jun 9 2004 12:40 PM
					// waitFor() hangs on some Java
					// implementations.
					/* int exitCode = process.waitFor();
					if(exitCode != 0)
						Log.log(Log.NOTICE,FileVFS.class,"chmod exited with code " + exitCode); */
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

	//{{{ Private members
	private static final FileSystemView fsView = FileSystemView.getFileSystemView();
	//}}}
}
