/*
 * BufferLoadRequest.java - I/O request
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.*;
//}}}

/**
 * A buffer load request.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferLoadRequest extends BufferIORequest
{
	//{{{ BufferLoadRequest constructor
	/**
	 * Creates a new buffer I/O request.
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 */
	public BufferLoadRequest(View view, Buffer buffer,
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
			try
			{
				String[] args = { vfs.getFileName(path) };
				setAbortable(true);
				if(!buffer.isTemporary())
				{
					setStatus(jEdit.getProperty("vfs.status.load",args));
					setValue(0L);
				}

				path = vfs._canonPath(session,path,view);

				VFSFile entry = vfs._getFile(
					session,path,view);
				long length;
				if(entry != null)
					length = entry.getLength();
				else
					length = 0L;

				in = vfs._createInputStream(session,path,
					false,view);
				if(in == null)
					return;

				read(autodetect(in),length,false);
				buffer.setNewFile(false);
			}
			catch(CharConversionException ch)
			{
				Log.log(Log.ERROR,this,ch);
				Object[] pp = { buffer.getProperty(JEditBuffer.ENCODING),
					ch.toString() };
				VFSManager.error(view,path,"ioerror.encoding-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(UnsupportedEncodingException uu)
			{
				Log.log(Log.ERROR,this,uu);
				Object[] pp = { buffer.getProperty(JEditBuffer.ENCODING),
					uu.toString() };
				VFSManager.error(view,path,"ioerror.encoding-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				Object[] pp = { io.toString() };
				VFSManager.error(view,path,"ioerror.read-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(OutOfMemoryError oom)
			{
				Log.log(Log.ERROR,this,oom);
				VFSManager.error(view,path,"out-of-memory-error",null);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}

			if(jEdit.getBooleanProperty("persistentMarkers"))
			{
				try
				{
					String[] args = { vfs.getFileName(path) };
					if(!buffer.isTemporary())
						setStatus(jEdit.getProperty("vfs.status.load-markers",args));
					setAbortable(true);

					in = vfs._createInputStream(session,markersPath,true,view);
					if(in != null)
						readMarkers(buffer,in);
				}
				catch(IOException io)
				{
					// ignore
				}
			}
		}
		catch(WorkThread.Abort a)
		{
			IOUtilities.closeQuietly(in);
			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,view);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(view,path,"ioerror.read-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(WorkThread.Abort a)
			{
				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
		}
	} //}}}

	//{{{ readMarkers() method
	private static void readMarkers(Buffer buffer, InputStream _in)
		throws IOException
	{
		// For `reload' command
		buffer.removeAllMarkers();

		BufferedReader in = new BufferedReader(new InputStreamReader(_in));

		try
		{
			String line;
			while((line = in.readLine()) != null)
			{
				// malformed marks file?
				if(line.length() == 0)
					continue;
				
				// compatibility kludge for jEdit 3.1 and earlier
				if(line.charAt(0) != '!')
					continue;


				char shortcut = line.charAt(1);
				int start = line.indexOf(';');
				int end = line.indexOf(';',start + 1);
				int position = Integer.parseInt(line.substring(start + 1,end));
				buffer.addMarker(shortcut,position);
			}
			buffer.setMarkersChanged(false);
		}
		finally
		{
			in.close();
		}
	} //}}}
}
