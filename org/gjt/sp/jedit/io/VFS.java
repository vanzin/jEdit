/*
 * VFS.java - Virtual filesystem implementation
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import gnu.regexp.*;
import java.awt.Color;
import java.awt.Component;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * A virtual filesystem implementation. Note tha methods whose names are
 * prefixed with "_" are called from the I/O thread.
 * @param author Slava Pestov
 * @author $Id$
 */
public abstract class VFS
{
	//{{{ Capabilities

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

	//}}}

	//{{{ VFS constructor
	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 */
	public VFS(String name)
	{
		this.name = name;
	} //}}}

	//{{{ VFS constructor
	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 * @param caps The capabilities
	 */
	public VFS(String name, int caps)
	{
		this.name = name;
		this.caps = caps;
	} //}}}

	//{{{ getName() method
	/**
	 * Returns this VFS's name. The name is used to obtain the
	 * label stored in the <code>vfs.<i>name</i>.label</code>
	 * property.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getCapabilities() method
	/**
	 * Returns the capabilities of this VFS.
	 * @since jEdit 2.6pre2
	 */
	public int getCapabilities()
	{
		return caps;
	} //}}}

	//{{{ showBrowseDialog() method
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
	} //}}}

	//{{{ getFileName() method
	/**
	 * Returns the file name component of the specified path.
	 * @param path The path
	 * @since jEdit 3.1pre4
	 */
	public String getFileName(String path)
	{
		if(path.equals("/"))
			return path;

		int count = Math.max(0,path.length() - 2);
		int index1 = path.lastIndexOf(File.separatorChar,count);
		int index2 = path.lastIndexOf('/',count);

		return path.substring(Math.max(index1,index2) + 1);
	} //}}}

	//{{{ getParentOfPath() method
	/**
	 * Returns the parent of the specified path. This must be
	 * overridden to return a non-null value for browsing of this
	 * filesystem to work.
	 * @param path The path
	 * @since jEdit 2.6pre5
	 */
	public String getParentOfPath(String path)
	{
		// ignore last character of path to properly handle
		// paths like /foo/bar/
		int count = Math.max(0,path.length() - 2);
		int index = path.lastIndexOf(File.separatorChar,count);
		if(index == -1)
			index = path.lastIndexOf('/',count);
		if(index == -1)
		{
			// this ensures that getFileParent("protocol:"), for
			// example, is "protocol:" and not "".
			index = path.lastIndexOf(':');
		}

		return path.substring(0,index + 1);
	} //}}}

	//{{{ constructPath() method
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
	} //}}}

	//{{{ getFileSeparator() method
	/**
	 * Returns the file separator used by this VFS.
	 * @since jEdit 2.6pre9
	 */
	public char getFileSeparator()
	{
		return '/';
	} //}}}

	//{{{ createVFSSession() method
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
	} //}}}

	//{{{ load() method
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
	} //}}}

	//{{{ save() method
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
			buffer.unsetProperty(Buffer.BACKED_UP);

		VFSManager.runInWorkThread(new BufferIORequest(
			BufferIORequest.SAVE,view,buffer,session,this,path));
		return true;
	} //}}}

	//{{{ insert() method
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
	} //}}}

	// the remaining methods are only called from the I/O thread

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
		return path;
	} //}}}

	//{{{ _listDirectory() method
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
	} //}}}

	//{{{ _getDirectoryEntry() method
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
	} //}}}

	//{{{ DirectoryEntry class
	/**
	 * A directory entry.
	 * @since jEdit 2.6pre2
	 */
	public static class DirectoryEntry implements Serializable
	{
		//{{{ File types
		public static final int FILE = 0;
		public static final int DIRECTORY = 1;
		public static final int FILESYSTEM = 2;
		//}}}

		//{{{ Instance variables
		public String name;
		public String path;
		public String deletePath;
		public int type;
		public long length;
		public boolean hidden;
		//}}}

		//{{{ DirectoryEntry constructor
		public DirectoryEntry(String name, String path, String deletePath,
			int type, long length, boolean hidden)
		{
			this.name = name;
			this.path = path;
			this.deletePath = deletePath;
			this.type = type;
			this.length = length;
			this.hidden = hidden;
		} //}}}

		protected boolean colorCalculated;
		protected Color color;

		//{{{ getColor() method
		public Color getColor()
		{
			if(!colorCalculated)
			{
				colorCalculated = true;
				color = getDefaultColorFor(name);
			}

			return color;
		} //}}}

		//{{{ toString() method
		public String toString()
		{
			return name;
		} //}}}
	} //}}}

	//{{{ _delete() method
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
	} //}}}

	//{{{ _rename() method
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
	} //}}}

	//{{{ _mkdir() method
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
	} //}}}

	//{{{ _backup() method
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
	} //}}}

	//{{{ _createInputStream() method
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
	} //}}}

	//{{{ _createOutputStream() method
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
	} //}}}

	//{{{ _saveComplete() method
	/**
	 * Called after a file has been saved.
	 * @param session The VFS session
	 * @param buffer The buffer
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 3.1pre1
	 */
	public void _saveComplete(Object session, Buffer buffer, Component comp)
		throws IOException {} //}}}

	//{{{ _endVFSSession() method
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
	} //}}}

	//{{{ getDefaultColorFor() method
	/**
	 * Returns color of the specified file name, by matching it against
	 * user-specified regular expressions.
	 * @since jEdit 4.0pre1
	 */
	public static Color getDefaultColorFor(String name)
	{
		synchronized(lock)
		{
			if(colors == null)
				loadColors();

			for(int i = 0; i < colors.size(); i++)
			{
				ColorEntry entry = (ColorEntry)colors.elementAt(i);
				if(entry.re.isMatch(name))
					return entry.color;
			}

			return null;
		}
	} //}}}

	//{{{ Private members
	private String name;
	private int caps;
	private static Vector colors;
	private static Object lock = new Object();

	//{{{ Class initializer
	static
	{
		EditBus.addToBus(new EBComponent()
		{
			public void handleMessage(EBMessage msg)
			{
				if(msg instanceof PropertiesChanged)
				{
					synchronized(lock)
					{
						colors = null;
					}
				}
			}
		});
	} //}}}

	//{{{ loadColors() method
	private static void loadColors()
	{
		synchronized(lock)
		{
			colors = new Vector();

			if(!jEdit.getBooleanProperty("vfs.browser.colorize"))
				return;

			try
			{
				String glob;
				int i = 0;
				while((glob = jEdit.getProperty("vfs.browser.colors." + i + ".glob")) != null)
				{
					colors.addElement(new ColorEntry(
						new RE(MiscUtilities.globToRE(glob)),
						jEdit.getColorProperty(
						"vfs.browser.colors." + i + ".color",
						Color.black)));
					i++;
				}
			}
			catch(REException e)
			{
				Log.log(Log.ERROR,VFS.class,"Error loading file list colors:");
				Log.log(Log.ERROR,VFS.class,e);
			}
		}
	} //}}}

	//{{{ ColorEntry class
	static class ColorEntry
	{
		RE re;
		Color color;

		ColorEntry(RE re, Color color)
		{
			this.re = re;
			this.color = color;
		}
	} //}}}

	//}}}
}
