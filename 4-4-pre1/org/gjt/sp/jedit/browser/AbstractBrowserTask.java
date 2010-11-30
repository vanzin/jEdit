/*
 * AbstractBrowserTask
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010 Matthieu Casanova
 * Portions Copyright (C) 2000, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * @author Matthieu Casanova
 * @version $Id$
 */
abstract class AbstractBrowserTask extends Task
{
	//{{{ BrowserIORequest constructors
	/**
	 * Creates a new browser I/O request.
	 * @param browser The VFS browser instance
	 * @param path1 The first path name to operate on
	 * @param path2 The second path name to operate on
	 * @param loadInfo A two-element array filled out by the request;
	 * element 1 is the canonical path, element 2 is the file list.
	 */
	AbstractBrowserTask(VFSBrowser browser,
		Object session, VFS vfs, String path1, String path2,
		Object[] loadInfo)
	{
		this(browser, session, vfs, path1, path2, loadInfo, null);
	}

	/**
	 * Creates a new browser I/O request.
	 * @param browser The VFS browser instance
	 * @param path1 The first path name to operate on
	 * @param path2 The second path name to operate on
	 * @param loadInfo A two-element array filled out by the request;
	 * element 1 is the canonical path, element 2 is the file list.
	 */
	AbstractBrowserTask(VFSBrowser browser,
		Object session, VFS vfs, String path1, String path2,
		Object[] loadInfo, Runnable awtTask)
	{
		this.browser = browser;
		this.session = session;
		this.vfs = vfs;
		this.path1 = path1;
		this.path2 = path2;
		this.loadInfo = loadInfo;
		if (awtTask != null)
		{
			MyTaskListener listener = new MyTaskListener(awtTask);
			TaskManager.instance.addTaskListener(listener);
		}
	} //}}}

	//{{{ Instance variables
	protected VFSBrowser browser;
	protected Object session;
	protected VFS vfs;
	protected String path1;
	protected String path2;
	protected Object[] loadInfo;
	//}}}

	private class MyTaskListener implements TaskListener
	{
		private final Runnable runnable;

		private MyTaskListener(Runnable runnable)
		{
			this.runnable = runnable;
		}

		public void waiting(Task task)
		{
		}

		public void running(Task task)
		{
		}

		public void done(Task task)
		{
			if (task == AbstractBrowserTask.this)
			{
				TaskManager.instance.removeTaskListener(this);
				ThreadUtilities.runInDispatchThread(runnable);
			}
		}

		public void statusUpdated(Task task)
		{
		}

		public void maximumUpdated(Task task)
		{
		}

		public void valueUpdated(Task task)
		{
		}
	}
}