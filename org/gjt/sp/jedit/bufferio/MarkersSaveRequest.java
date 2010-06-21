/* {{{ MarkersSaveRequest.java - I/O request
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * based on jEdit.buffer.BufferSaveRequest (Copyright (C) 2000, 2005 Slava Pestov)
 * Copyright (C) 2005 Martin Raspe
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
 * }}} */

package org.gjt.sp.jedit.bufferio;

//{{{ Imports
import java.io.*;
import java.util.List;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.*;
//}}}

/**
 * A save request for markers. Factored out from BufferSaveRequest.java
 *
 * @author     Martin Raspe
 * created    May 20, 2005
 * modified   $Date: 2006/03/10 12:49:17 $ by $Author: hertzhaft $
 */
public class MarkersSaveRequest extends Task
{
	//{{{ Constants
	public static final String ERROR_OCCURRED = "MarkersSaveRequest__error";
	//}}}

	//{{{ MarkersSaveRequest constructor
	/**
	 * Creates a new I/O request for markers.
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 */
	public MarkersSaveRequest(View view, Buffer buffer,
		Object session, VFS vfs, String path)
	{
		this.view = view;
		this.buffer = buffer;
		this.session = session;
		this.vfs = vfs;
		this.path = path;
		this.markersPath = Buffer.getMarkersPath(vfs, path);

	} //}}}

	//{{{ run() method
	@Override
	public void _run()
	{
		OutputStream out = null;

		try
		{
			// the entire save operation can be aborted...
//			setAbortable(true);
			// We only save markers to VFS's that support deletion.
			// Otherwise, we will accumilate stale marks files.
			if((vfs.getCapabilities() & VFS.DELETE_CAP) != 0)
			{
				if(buffer.getMarkers().isEmpty())
					vfs._delete(session,markersPath,view);
				else
				{
					String[] args = { vfs.getFileName(path) };
					setStatus(jEdit.getProperty("vfs.status.save-markers",args));
					out = vfs._createOutputStream(session,markersPath,view);
					if(out != null)
						writeMarkers(out);
				}
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}
	} //}}}

	//{{{ writeMarkers() method
	private void writeMarkers(OutputStream out)
		throws IOException
	{
		Writer o = new BufferedWriter(new OutputStreamWriter(out));
		try
		{
			List<Marker> markers = buffer.getMarkers();
			synchronized (markers)
			{
				setMaximum(markers.size());
				for(int i = 0; i < markers.size(); i++)
				{
					setValue(i+1);
					Marker marker = markers.get(i);
					o.write('!');
					o.write(marker.getShortcut());
					o.write(';');

					String pos = String.valueOf(marker.getPosition());
					o.write(pos);
					o.write(';');
					o.write(pos);
					o.write('\n');
				}
			}
		}
		finally
		{
			o.close();
		}
	} //}}}

	//{{{ Instance variables
	protected View view;
	protected Buffer buffer;
	protected Object session;
	protected VFS vfs;
	protected String path;
	protected String markersPath;
	//}}}

}
