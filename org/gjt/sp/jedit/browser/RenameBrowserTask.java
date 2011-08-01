/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
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
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.notification.NotificationManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import java.io.IOException;
//}}}

/**
 * @author Matthieu Casanova
 * @version $Id: ListDirectoryBrowserTask.java 19692 2011-07-22 15:27:56Z kpouer $
 */
class RenameBrowserTask extends AbstractBrowserTask
{
	//{{{ BrowserIORequest constructor
	/**
	 * Creates a new browser I/O request.
	 *
	 * @param browser  The VFS browser instance
	 * @param path1    The first path name to operate on
	 * @param path2    The second path name to operate on
	 */
	RenameBrowserTask(VFSBrowser browser,
			  Object session, VFS vfs, String path1, String path2,
			 Runnable awtRunnable)
	{
		super(browser, session, vfs, path1, awtRunnable);
		this.path2 = path2;
	} //}}}

	//{{{ _run() method
	@Override
	public void _run()
	{
		try
		{
			setCancellable(true);
			String[] args = {path, path2};
			setStatus(jEdit.getProperty("vfs.status.renaming", args));

			path = vfs._canonPath(session, path, browser);
			path2 = vfs._canonPath(session, path2, browser);

			VFSFile file = vfs._getFile(session, path2, browser);
			if (file != null)
			{
				if ((OperatingSystem.isCaseInsensitiveFS())
				    && path.equalsIgnoreCase(path2))
				{
					// allow user to change name
					// case
				}
				else
				{
					NotificationManager.error(browser, path,
					    "ioerror.rename-exists",
					    new String[]{path2});
					return;
				}
			}

			if (!vfs._rename(session, path, path2, browser))
				NotificationManager.error(browser, path, "ioerror.rename-error",
				    new String[]{path2});
		}
		catch (IOException io)
		{
			setCancellable(false);
			Log.log(Log.ERROR, this, io);
			String[] pp = {io.toString()};
			NotificationManager.error(browser, path, "ioerror.directory-error", pp);
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session, browser);
			}
			catch (IOException io)
			{
				setCancellable(false);
				Log.log(Log.ERROR, this, io);
				String[] pp = {io.toString()};
				NotificationManager.error(browser, path, "ioerror.directory-error", pp);
			}
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[type=RENAME"
		    + ",vfs=" + vfs + ",path=" + path
		    + ",path2=" + path2 + ']';
	} //}}}

	//{{{ Private members
	private String path2;
	//}}}
}
