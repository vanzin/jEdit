/*
 * VFSManager.java - Main class of virtual filesystem
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.util.*;

import org.gjt.sp.jedit.gui.ErrorListDialog;
import org.gjt.sp.jedit.msg.VFSUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkThreadPool;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * jEdit's virtual filesystem allows it to transparently edit files
 * stored elsewhere than the local filesystem, for example on an FTP
 * site. See the {@link VFS} class for implementation details.<p>
 *
 * Note that most of the jEdit API is not thread-safe, so special care
 * must be taken when making jEdit API calls. Also, it is not safe to
 * call <code>SwingUtilities.invokeAndWait()</code> from a work request;
 * it can cause a deadlock if the given runnable then later calls
 * {@link #waitForRequests()}.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSManager
{
	/**
	 * The service type. See {@link org.gjt.sp.jedit.ServiceManager}.
	 * @since jEdit 4.2pre1
	 */
	public static final String SERVICE = "org.gjt.sp.jedit.io.VFS";

	//{{{ init() method
	/**
	 * Do not call.
	 */
	public static void init()
	{
		int count = jEdit.getIntegerProperty("ioThreadCount",4);
		ioThreadPool = new WorkThreadPool("jEdit I/O",count);
		JARClassLoader classLoader = new JARClassLoader();
		for(int i = 0; i < ioThreadPool.getThreadCount(); i++)
		{
			ioThreadPool.getThread(i).setContextClassLoader(
				classLoader);
		}
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
	 * @deprecated Use <code>getVFSForProtocol()</code> instead.
	 */
	@Deprecated
	public static VFS getVFSByName(String name)
	{
		// in new api, protocol always equals name
		VFS vfs = (VFS)ServiceManager.getService(SERVICE,name);
		if(vfs == null)
			return vfsHash.get(name);
		else
			return vfs;
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
			VFS vfs = (VFS)ServiceManager.getService(SERVICE,protocol);
			if(vfs == null)
				vfs = protocolHash.get(protocol);

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
	 * @deprecated Write a <code>services.xml</code> file instead;
	 * see {@link org.gjt.sp.jedit.ServiceManager}.
	 */
	@Deprecated
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
	 * @deprecated Use <code>getVFSs()</code> instead.
	 */
	@Deprecated
	public static Enumeration<VFS> getFilesystems()
	{
		return vfsHash.elements();
	} //}}}

	//{{{ getVFSs() method
	/**
	 * Returns a list of all registered filesystems.
	 * @since jEdit 4.2pre1
	 */
	public static String[] getVFSs()
	{
		// the sooner ppl move to the new api, the less we'll need
		// crap like this
		List<String> returnValue = new LinkedList<String>();
		String[] newAPI = ServiceManager.getServiceNames(SERVICE);
		if(newAPI != null)
		{
			for(int i = 0; i < newAPI.length; i++)
			{
				returnValue.add(newAPI[i]);
			}
		}
		Enumeration<String> oldAPI = vfsHash.keys();
		while(oldAPI.hasMoreElements())
			returnValue.add(oldAPI.nextElement());
		return returnValue.toArray(new String[returnValue.size()]);
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
	@Deprecated
	public static void runInAWTThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,true);
	} //}}}

	//{{{ runInWorkThread() method
	/**
	 * Executes the specified runnable in one of the I/O threads.
	 * @since jEdit 2.6pre2
	 */
	@Deprecated
	public static void runInWorkThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,false);
	} //}}}

	//}}}

	//{{{ error() method
	/**
	 * Handle an I/O error.
	 * @since jEdit 4.3pre3
	 */
	public static void error(IOException e, String path, Component comp)
	{
		Log.log(Log.ERROR,VFSManager.class,e);
		VFSManager.error(comp,path,"ioerror",new String[] { e.toString() });
	} //}}}

	//{{{ error() method
	/**
	 * @deprecated Call the other <code>error()</code> method instead.
	 */
	@Deprecated
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

	//{{{ error() method
	/**
	 * Reports an I/O error.
	 *
	 * @param comp The component
	 * @param path The path name that caused the error
	 * @param messageProp The error message property name
	 * @param args Positional parameters
	 * @since jEdit 4.0pre3
	 */
	public static void error(Component comp,
		final String path,
		String messageProp,
		Object[] args)
	{
		final Frame frame = JOptionPane.getFrameForComponent(comp);

		synchronized(errorLock)
		{
			error = true;

			errors.add(new ErrorListDialog.ErrorEntry(
				path,messageProp,args));

			if(errors.size() == 1)
			{
				

				VFSManager.runInAWTThread(new Runnable()
				{
					public void run()
					{
						String caption = jEdit.getProperty(
							"ioerror.caption" + (errors.size() == 1
							? "-1" : ""),new Integer[] {
							Integer.valueOf(errors.size())});
						new ErrorListDialog(
							frame.isShowing()
							? frame
							: jEdit.getFirstView(),
							jEdit.getProperty("ioerror.title"),
							caption,errors,false);
						errors.clear();
						error = false;
					}
				});
			}
		}
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
					VFSUpdate msg = vfsUpdates.get(i);
					if(msg.getPath().equals(path))
					{
						// don't send two updates
						// for the same path
						return;
					}
				}

				vfsUpdates.add(new VFSUpdate(path));

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
				// the vfs browser has what you might call
				// a design flaw, it doesn't update properly
				// unless the vfs update for a parent arrives
				// before any updates for the children. sorting
				// the list alphanumerically guarantees this.
				Collections.sort(vfsUpdates,
					new StandardUtilities.StringCompare<VFSUpdate>()
				);
				for(int i = 0; i < vfsUpdates.size(); i++)
				{
					EditBus.send(vfsUpdates.get(i));
				}

				vfsUpdates.clear();
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Static variables
	private static WorkThreadPool ioThreadPool;
	private static VFS fileVFS;
	private static VFS urlVFS;
	private static final Hashtable<String, VFS> vfsHash;
	private static final Map<String, VFS> protocolHash;
	private static boolean error;
	private static final Object errorLock = new Object();
	private static final Vector<ErrorListDialog.ErrorEntry> errors;
	private static final Object vfsUpdateLock = new Object();
	private static final List<VFSUpdate> vfsUpdates;
	//}}}

	//{{{ Class initializer
	static
	{
		errors = new Vector<ErrorListDialog.ErrorEntry>();
		fileVFS = new FileVFS();
		urlVFS = new UrlVFS();
		vfsHash = new Hashtable<String, VFS>();
		protocolHash = new Hashtable<String, VFS>();
		vfsUpdates = new ArrayList<VFSUpdate>(10);
	} //}}}

	private VFSManager() {}
	//}}}
}
