/*
 * VFSManager.java - Main class of virtual filesystem
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
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.Vector;
import org.gjt.sp.jedit.msg.VFSUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkThreadPool;
//}}}

/**
 * jEdit's virtual filesystem allows it to transparently edit files
 * stored elsewhere than the local filesystem, for example on an FTP
 * site.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSManager
{
	//{{{ init() method
	/**
	 * Do not call.
	 */
	public static void init()
	{
		int count = jEdit.getIntegerProperty("ioThreadCount",4);
		ioThreadPool = new WorkThreadPool("jEdit I/O",count);
		registerVFS(FavoritesVFS.PROTOCOL,new FavoritesVFS());
		registerVFS(FileRootsVFS.PROTOCOL,new FileRootsVFS());
	} //}}}

	//{{{ start() method
	/**
	 * Do not call.
	 */
	public static void start()
	{
		ioThreadPool.start();
	} //}}}

	//{{{ VFS methods

	//{{{ getFileVFS() method
	/**
	 * Returns the local filesystem VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getFileVFS()
	{
		return fileVFS;
	} //}}}

	//{{{ getUrlVFS() method
	/**
	 * Returns the URL VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getUrlVFS()
	{
		return urlVFS;
	} //}}}

	//{{{ getVFSByName() method
	/**
	 * Returns the VFS for the specified name.
	 * @param name The VFS name
	 * @since jEdit 2.6pre4
	 */
	public static VFS getVFSByName(String name)
	{
		return (VFS)vfsHash.get(name);
	} //}}}

	//{{{ getVFSForProtocol() method
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
	} //}}}

	//{{{ getVFSForPath() method
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
	} //}}}

	//{{{ registerVFS() method
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
	} //}}}

	//{{{ getFilesystems() method
	/**
	 * Returns an enumeration of all registered filesystems.
	 * @since jEdit 2.5pre1
	 */
	public static Enumeration getFilesystems()
	{
		return vfsHash.elements();
	} //}}}

	//}}}

	//{{{ I/O request methods

	//{{{ getIOThreadPool() method
	/**
	 * Returns the I/O thread pool.
	 */
	public static WorkThreadPool getIOThreadPool()
	{
		return ioThreadPool;
	} //}}}

	//{{{ waitForRequests() method
	/**
	 * Returns when all pending requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void waitForRequests()
	{
		ioThreadPool.waitForRequests();
	} //}}}

	//{{{ errorOccurred() method
	/**
	 * Returns if the last request caused an error.
	 */
	public static boolean errorOccurred()
	{
		return error;
	} //}}}

	//{{{ getRequestCount() method
	/**
	 * Returns the number of pending I/O requests.
	 */
	public static int getRequestCount()
	{
		return ioThreadPool.getRequestCount();
	} //}}}

	//{{{ runInAWTThread() method
	/**
	 * Executes the specified runnable in the AWT thread once all
	 * pending I/O requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void runInAWTThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,true);
	} //}}}

	//{{{ runInWorkThread() method
	/**
	 * Executes the specified runnable in one of the I/O threads.
	 * @since jEdit 2.6pre2
	 */
	public static void runInWorkThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,false);
	} //}}}

	//}}}

	//{{{ error() method
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
	} //}}}

	//{{{ sendVFSUpdate() method
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
	} //}}}

	//{{{ SendVFSUpdatesSafely class
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
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private static WorkThreadPool ioThreadPool;
	private static VFS fileVFS;
	private static VFS urlVFS;
	private static Hashtable vfsHash;
	private static Hashtable protocolHash;
	private static boolean error;
	private static Object vfsUpdateLock;
	private static Vector vfsUpdates;
	//}}}

	//{{{ Class initializer
	static
	{
		fileVFS = new FileVFS();
		urlVFS = new UrlVFS();
		vfsHash = new Hashtable();
		protocolHash = new Hashtable();
		vfsUpdateLock = new Object();
		vfsUpdates = new Vector();
	} //}}}

	private VFSManager() {}
	//}}}
}
