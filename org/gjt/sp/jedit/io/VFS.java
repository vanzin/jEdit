/*
 * VFS.java - Virtual filesystem implementation
 * Copyright (C) 2000 Slava Pestov
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
import java.io.*;
import org.gjt.sp.jedit.*;

/**
 * A virtual filesystem implementation. Note tha methods whose names are
 * prefixed with "_" are called from the I/O thread.
 * @param author Slava Pestov
 * @author $Id$
 */
public abstract class VFS
{
	/**
	 * Read capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int READ_CAP = 1 << 0;

	/**
	 * Write capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int WRITE_CAP = 1 << 1;

	/**
	 * If set, a menu item for this VFS will appear in the browser's
	 * 'More' menu. If not set, it will still be possible to type in
	 * URLs in this VFS in the browser, but there won't be a user-visible
	 * way of doing this.
	 * @since jEdit 2.6pre2
	 */
	public static final int BROWSE_CAP = 1 << 2;

	/**
	 * Delete file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int DELETE_CAP = 1 << 3;

	/**
	 * Rename file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int RENAME_CAP = 1 << 4;

	/**
	 * Make directory file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int MKDIR_CAP = 1 << 5;

	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 */
	public VFS(String name)
	{
		this.name = name;
	}

	/**
	 * Returns this VFS's name. The name is used to obtain the
	 * label stored in the <code>vfs.<i>name</i>.label</code>
	 * property.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the capabilities of this VFS.
	 * @since jEdit 2.6pre2
	 */
	public abstract int getCapabilities();

	/**
	 * Displays a dialog box that should set up a session and return
	 * the initial URL to browse.
	 * @param session Where the VFS session will be stored
	 * @param comp The component that will parent error dialog boxes
	 * @return The URL
	 * @since jEdit 2.7pre1
	 */
	public String showBrowseDialog(Object[] session, Component comp)
	{
		return null;
	}

	/**
	 * Returns the file name component of the specified path. The
	 * default implementation calls
	 * <code>MiscUtilities.getFileName()</code>
	 * @param path The path
	 * @since jEdit 3.1pre4
	 */
	public String getFileName(String path)
	{
		return MiscUtilities.getFileName(path);
	}

	/**
	 * Returns the parent of the specified path. This must be
	 * overridden to return a non-null value for browsing of this
	 * filesystem to work.
	 * @param path The path
	 * @since jEdit 2.6pre5
	 */
	public String getParentOfPath(String path)
	{
		return null;
	}

	/**
	 * Constructs a path from the specified directory and
	 * file name component. This must be overridden to return a
	 * non-null value, otherwise browsing this filesystem will
	 * not work.
	 * @param parent The parent directory
	 * @param path The path
	 * @since jEdit 2.6pre2
	 */
	public String constructPath(String parent, String path)
	{
		return parent + path;
	}

	/**
	 * Returns the file separator used by this VFS.
	 * @since jEdit 2.6pre9
	 */
	public char getFileSeparator()
	{
		return '/';
	}

	/**
	 * Creates a VFS session. This method is called from the AWT thread,
	 * so it should not do any I/O. It could, however, prompt for
	 * a login name and password, for example.
	 * @param path The path in question
	 * @param comp The component that will parent error dialog boxes
	 * @return The session
	 * @since jEdit 2.6pre3
	 */
	public Object createVFSSession(String path, Component comp)
	{
		return new Object();
	}

	/**
	 * Loads the specified buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean load(View view, Buffer buffer, String path)
	{
		if((getCapabilities() & READ_CAP) == 0)
		{
			VFSManager.error(view,"vfs.not-supported.load",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		BufferIORequest request = new BufferIORequest(
			BufferIORequest.LOAD,view,buffer,session,this,path);
		if(buffer.isTemporary())
			// this makes HyperSearch much faster
			request.run();
		else
			VFSManager.runInWorkThread(request);

		return true;
	}

	/**
	 * Saves the specifies buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean save(View view, Buffer buffer, String path)
	{
		if((getCapabilities() & WRITE_CAP) == 0)
		{
			VFSManager.error(view,"vfs.not-supported.save",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		/* When doing a 'save as', the path to save to (path)
		 * will not be the same as the buffer's previous path
		 * (buffer.getPath()). In that case, we want to create
		 * a backup of the new path, even if the old path was
		 * backed up as well (BACKED_UP property set) */
		if(!path.equals(buffer.getPath()))
			buffer.getDocumentProperties().remove(Buffer.BACKED_UP);

		VFSManager.runInWorkThread(new BufferIORequest(
			BufferIORequest.SAVE,view,buffer,session,this,path));
		return true;
	}

	/**
	 * Inserts a file into the specified buffer. The default implementation
	 * posts an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean insert(View view, Buffer buffer, String path)
	{
		if((getCapabilities() & READ_CAP) == 0)
		{
			VFSManager.error(view,"vfs.not-supported.load",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		VFSManager.runInWorkThread(new BufferIORequest(
			BufferIORequest.INSERT,view,buffer,session,this,path));
		return true;
	}

	// the remaining methods are only called from the I/O thread

	/**
	 * Lists the specified directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password is obtained from the session.
	 * @param session The session
	 * @param directory The directory
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.7pre1
	 */
	public DirectoryEntry[] _listDirectory(Object session, String directory,
		Component comp)
		throws IOException
	{
		VFSManager.error(comp,"vfs.not-supported.list",new String[] { name });
		return null;
	}

	/**
	 * Returns the specified directory entry.
	 * @param session The session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @return The specified directory entry, or null if it doesn't exist.
	 * @since jEdit 2.7pre1
	 */
	public DirectoryEntry _getDirectoryEntry(Object session, String path,
		Component comp)
		throws IOException
	{
		return null;
	}

	/**
	 * A directory entry.
	 * @since jEdit 2.6pre2
	 */
	public static class DirectoryEntry implements Serializable
	{
		public static final int FILE = 0;
		public static final int DIRECTORY = 1;
		public static final int FILESYSTEM = 2;

		public String name;
		public String path;
		public String deletePath;
		public int type;
		public long length;
		public boolean hidden;

		public DirectoryEntry(String name, String path, String deletePath,
			int type, long length, boolean hidden)
		{
			this.name = name;
			this.path = path;
			this.deletePath = deletePath;
			this.type = type;
			this.length = length;
			this.hidden = hidden;
		}

		public String toString()
		{
			return name;
		}
	}

	/**
	 * Deletes the specified URL.
	 * @param session The VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _delete(Object session, String path, Component comp)
		throws IOException
	{
		return false;
	}

	/**
	 * Renames the specified URL. Some filesystems might support moving
	 * URLs between directories, however others may not. Do not rely on
	 * this behavior.
	 * @param session The VFS session
	 * @param from The old path
	 * @param to The new path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _rename(Object session, String from, String to,
		Component comp) throws IOException
	{
		return false;
	}

	/**
	 * Creates a new directory with the specified URL.
	 * @param session The VFS session
	 * @param directory The directory
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _mkdir(Object session, String directory, Component comp)
		throws IOException
	{
		return false;
	}

	/**
	 * Backs up the specified file. This should only be overriden by
	 * the local filesystem VFS.
	 * @param session The VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 3.2pre2
	 */
	public void _backup(Object session, String path, Component comp)
		throws IOException
	{
	}

	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public InputStream _createInputStream(Object session,
		String path, boolean ignoreErrors, Component comp)
		throws IOException
	{
		VFSManager.error(comp,"vfs.not-supported.load",new String[] { name });
		return null;
	}

	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public OutputStream _createOutputStream(Object session,
		String path, Component comp)
		throws IOException
	{
		VFSManager.error(comp,"vfs.not-supported.save",new String[] { name });
		return null;
	}

	/**
	 * Called after a file has been saved.
	 * @param session The VFS session
	 * @param buffer The buffer
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 3.1pre1
	 */
	public void _saveComplete(Object session, Buffer buffer, Component comp)
		throws IOException {}

	/**
	 * Finishes the specified VFS session. This must be called
	 * after all I/O with this VFS is complete, to avoid leaving
	 * stale network connections and such.
	 * @param session The VFS session
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.7pre1
	 */
	public void _endVFSSession(Object session, Component comp)
		throws IOException
	{
	}

	// private members
	private String name;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:17  spestov
 * Initial revision
 *
 * Revision 1.28  2001/05/29 11:30:05  sp
 * foo
 *
 * Revision 1.27  2001/05/17 05:00:14  sp
 * Status bar, various minor tweaks
 *
 * Revision 1.26  2001/05/13 07:21:27  sp
 * more stuff
 *
 * Revision 1.25  2001/04/27 11:28:46  sp
 * new selection code started
 *
 * Revision 1.24  2001/04/18 03:09:45  sp
 * GJT was down for a long time
 *
 * Revision 1.23  2001/01/23 09:23:48  sp
 * code cleanups, misc tweaks
 *
 * Revision 1.22  2000/11/11 02:59:31  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.21  2000/11/05 00:44:14  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.20  2000/11/02 09:19:33  sp
 * more features
 *
 * Revision 1.19  2000/10/15 04:10:35  sp
 * bug fixes
 *
 * Revision 1.18  2000/08/29 07:47:13  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.17  2000/08/23 09:51:48  sp
 * Documentation updates, abbrev updates, bug fixes
 *
 * Revision 1.16  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.15  2000/08/10 11:55:58  sp
 * VFS browser toolbar improved a little bit, font selector tweaks
 *
 * Revision 1.14  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.13  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.12  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 */
