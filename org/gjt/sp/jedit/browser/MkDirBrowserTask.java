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
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.notification.NotificationManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import java.io.IOException;
//}}}

/**
 * @author Matthieu CAsanova
 * @version $Id: BrowserIORequest.java 12504 2008-04-22 23:12:43Z ezust $
 */
class MkDirBrowserTask extends AbstractBrowserTask
{
	//{{{ MkDirBrowserTask constructor
	/**
	 * Creates a new browser I/O request.
	 * @param browser The VFS browser instance
	 * @param path The first path name to operate on
	 */
	MkDirBrowserTask(VFSBrowser browser,
			 Object session, VFS vfs, String path,
			 Runnable awtRunnable)
	{
		super(browser, session, vfs, path, awtRunnable);
	} //}}}

	//{{{ run() method
	@Override
	public void _run()
	{
		String[] args = {path};
		try
		{
			setCancellable(true);
			setStatus(jEdit.getProperty("vfs.status.mkdir",args));

			path = vfs._canonPath(session, path,browser);

			if(!vfs._mkdir(session, path,browser))
				NotificationManager.error(browser, path,"ioerror.mkdir-error",null);
		}
		catch(IOException io)
		{
			setCancellable(false);
			Log.log(Log.ERROR,this,io);
			args[0] = io.toString();
			NotificationManager.error(browser, path,"ioerror",args);
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				setCancellable(false);
				Log.log(Log.ERROR,this,io);
				args[0] = io.toString();
				NotificationManager.error(browser, path,"ioerror",args);
			}
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[type=MKDIR"
			+ ",vfs=" + vfs + ",path=" + path + ']';
	} //}}}
}
