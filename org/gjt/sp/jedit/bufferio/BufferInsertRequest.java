/*
 * BufferInsertRequest.java - I/O request
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

package org.gjt.sp.jedit.bufferio;

//{{{ Imports
import java.io.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.notification.NotificationManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * A buffer insert request.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferInsertRequest extends BufferIORequest
{
	//{{{ BufferInsertRequest constructor
	/**
	 * Creates a new buffer I/O request.
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 */
	public BufferInsertRequest(View view, Buffer buffer,
		Object session, VFS vfs, String path)
	{
		super(view,buffer,session,vfs,path);
	} //}}}

	//{{{ run() method
	public void run()
	{
		InputStream in = null;
		try
		{
			String[] args = { vfs.getFileName(path) };
			setStatus(jEdit.getProperty("vfs.status.load",args));
			setAbortable(true);

			path = vfs._canonPath(session,path,view);

			VFSFile entry = vfs._getFile(
				session,path,view);
			long length;
			if(entry != null)
				length = entry.getLength();
			else
				length = 0L;

			in = vfs._createInputStream(session,path,false,view);
			if(in == null)
				return;

			final SegmentBuffer seg = read(
				autodetect(in),length,true);

			/* we don't do this in Buffer.insert() so that
			   we can insert multiple files at once */
			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					view.getTextArea().setSelectedText(
						seg.toString());
				}
			});
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			String[] pp = { e.toString() };
			NotificationManager.error(view,path,"ioerror.read-error",pp);

			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		catch(WorkThread.Abort a)
		{
			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		finally
		{
			IOUtilities.closeQuietly(in);
			try
			{
				vfs._endVFSSession(session,view);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
				String[] pp = { e.toString() };
				NotificationManager.error(view,path,"ioerror.read-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(WorkThread.Abort a)
			{
				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
		}
	} //}}}
}
