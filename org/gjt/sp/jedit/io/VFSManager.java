/*
 * VFSManager.java - Main class of virtual filesystem
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

import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.Vector;
import org.gjt.sp.jedit.msg.VFSUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkThreadPool;

/**
 * jEdit's virtual filesystem allows it to transparently edit files
 * stored elsewhere than the local filesystem, for example on an FTP
 * site.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSManager
{
	/**
	 * Do not call.
	 */
	public static void start()
	{
		ioThreadPool.start();
	}

	/**
	 * Returns the I/O thread pool.
	 */
	public static WorkThreadPool getIOThreadPool()
	{
		return ioThreadPool;
	}

	/**
	 * Returns the local filesystem VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getFileVFS()
	{
		return fileVFS;
	}

	/**
	 * Returns the URL VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getUrlVFS()
	{
		return urlVFS;
	}

	/**
	 * Returns the VFS for the specified name.
	 * @param name The VFS name
	 * @since jEdit 2.6pre4
	 */
	public static VFS getVFSByName(String name)
	{
		return (VFS)vfsHash.get(name);
	}

	/**
	 * Returns the VFS for the specified protocol.
	 * @param protocol The protocol
	 * @since jEdit 2.5pre1
	 */
	public static VFS getVFSForProtocol(String protocol)
	{
		if(protocol.equals("file"))
			return fileVFS;
		else
		{
			VFS vfs = (VFS)protocolHash.get(protocol);
			if(vfs != null)
				return vfs;
			else
				return urlVFS;
		}
	}

	/**
	 * Returns the VFS for the specified path.
	 * @param path The path
	 * @since jEdit 2.6pre4
	 */
	public static VFS getVFSForPath(String path)
	{
		if(MiscUtilities.isURL(path))
			return getVFSForProtocol(MiscUtilities.getProtocolOfURL(path));
		else
			return fileVFS;
	}

	/**
	 * Registers a virtual filesystem.
	 * @param protocol The protocol
	 * @param vfs The VFS
	 * @since jEdit 2.5pre1
	 */
	public static void registerVFS(String protocol, VFS vfs)
	{
		Log.log(Log.DEBUG,VFSManager.class,"Registered "
			+ vfs.getName() + " filesystem for "
			+ protocol + " protocol");
		vfsHash.put(vfs.getName(),vfs);
		protocolHash.put(protocol,vfs);
	}

	/**
	 * Returns an enumeration of all registered filesystems.
	 * @since jEdit 2.5pre1
	 */
	public static Enumeration getFilesystems()
	{
		return vfsHash.elements();
	}

	/**
	 * Returns when all pending requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void waitForRequests()
	{
		ioThreadPool.waitForRequests();
	}

	/**
	 * Returns if the last request caused an error.
	 */
	public static boolean errorOccurred()
	{
		return error;
	}

	/**
	 * Returns the number of pending I/O requests.
	 */
	public static int getRequestCount()
	{
		return ioThreadPool.getRequestCount();
	}

	/**
	 * Executes the specified runnable in the AWT thread once all
	 * pending I/O requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void runInAWTThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,true);
	}

	/**
	 * Executes the specified runnable in one of the I/O threads.
	 * @since jEdit 2.6pre2
	 */
	public static void runInWorkThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,false);
	}

	/**
	 * For use by VFS implementations and IO requests. Displays the
	 * specified error in the AWT thread.
	 * @since jEdit 2.6pre1
	 */
	public static void error(final Component comp, final String error, final Object[] args)
	{
		// if we are already in the AWT thread, take a shortcut
		if(SwingUtilities.isEventDispatchThread())
		{
			GUIUtilities.error(comp,error,args);
			return;
		}

		// the 'error' chicanery ensures that stuff like:
		// VFSManager.waitForRequests()
		// if(VFSManager.errorOccurred())
		//         ...
		// will work (because the below runnable will only be
		// executed in the next event)
		VFSManager.error = true;

		runInAWTThread(new Runnable()
		{
			public void run()
			{
				VFSManager.error = false;

				if(comp == null || !comp.isShowing())
					GUIUtilities.error(null,error,args);
				else
					GUIUtilities.error(comp,error,args);
			}
		});
	}

	/**
	 * Sends a VFS update message.
	 * @param vfs The VFS
	 * @param path The path that changed
	 * @param parent True if an update should be sent for the path's
	 * parent too
	 * @since jEdit 2.6pre4
	 */
	public static void sendVFSUpdate(VFS vfs, String path, boolean parent)
	{
		if(parent)
		{
			sendVFSUpdate(vfs,vfs.getParentOfPath(path),false);
			sendVFSUpdate(vfs,path,false);
		}
		else
		{
			// have to do this hack until VFSPath class is written
			if(path.length() != 1 && (path.endsWith("/")
				|| path.endsWith(java.io.File.separator)))
				path = path.substring(0,path.length() - 1);

			synchronized(vfsUpdateLock)
			{
				for(int i = 0; i < vfsUpdates.size(); i++)
				{
					VFSUpdate msg = (VFSUpdate)vfsUpdates
						.elementAt(i);
					if(msg.getPath().equals(path))
					{
						// don't send two updates
						// for the same path
						return;
					}
				}

				vfsUpdates.addElement(new VFSUpdate(path));

				if(vfsUpdates.size() == 1)
				{
					// we were the first to add an update;
					// add update sending runnable to AWT
					// thread
					VFSManager.runInAWTThread(new SendVFSUpdatesSafely());
				}
			}
		}
	}

	static class SendVFSUpdatesSafely implements Runnable
	{
		public void run()
		{
			synchronized(vfsUpdateLock)
			{
				for(int i = 0; i < vfsUpdates.size(); i++)
				{
					EditBus.send((VFSUpdate)vfsUpdates.elementAt(i));
				}

				vfsUpdates.removeAllElements();
			}
		}
	}

	// private members
	private static WorkThreadPool ioThreadPool;
	private static VFS fileVFS;
	private static VFS urlVFS;
	private static Hashtable vfsHash;
	private static Hashtable protocolHash;
	private static boolean error;
	private static Object vfsUpdateLock;
	private static Vector vfsUpdates;

	static
	{
		int count;
		try
		{
			count = Integer.parseInt(jEdit.getProperty("ioThreadCount"));
		}
		catch(NumberFormatException nf)
		{
			count = 4;
		}
		ioThreadPool = new WorkThreadPool("jEdit I/O",count);
		fileVFS = new FileVFS();
		urlVFS = new UrlVFS();
		vfsHash = new Hashtable();
		protocolHash = new Hashtable();
		vfsUpdateLock = new Object();
		vfsUpdates = new Vector();
		registerVFS(FavoritesVFS.PROTOCOL,new FavoritesVFS());
		registerVFS(FileRootsVFS.PROTOCOL,new FileRootsVFS());
	}

	private VFSManager() {}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:16  spestov
 * Initial revision
 *
 * Revision 1.23  2001/03/01 11:03:27  sp
 * Improved folding, vfs bug fix
 *
 * Revision 1.22  2000/11/11 02:59:31  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.21  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 * Revision 1.20  2000/08/29 07:47:13  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.19  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.18  2000/08/20 07:29:31  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.17  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.16  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.15  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 * Revision 1.14  2000/07/26 07:48:45  sp
 * stuff
 *
 * Revision 1.13  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.12  2000/07/21 10:23:49  sp
 * Multiple work threads
 *
 * Revision 1.11  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 */
