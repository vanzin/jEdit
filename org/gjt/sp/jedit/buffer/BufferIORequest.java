/*
 * BufferIORequest.java - I/O request
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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

package org.gjt.sp.jedit.buffer;

//{{{ Imports
import javax.swing.text.Segment;
import java.io.*;
import java.util.zip.*;
import java.util.Vector;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * A buffer I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferIORequest extends WorkRequest
{
	//{{{ Constants
	/**
	 * Size of I/O buffers.
	 */
	public static final int IOBUFSIZE = 32768;

	/**
	 * Number of lines per progress increment.
	 */
	public static final int PROGRESS_INTERVAL = 300;

	public static final String LOAD_DATA = "BufferIORequest__loadData";
	public static final String END_OFFSETS = "BufferIORequest__endOffsets";
	public static final String NEW_PATH = "BufferIORequest__newPath";

	/**
	 * Buffer boolean property set when an error occurs.
	 */
	public static final String ERROR_OCCURRED = "BufferIORequest__error";

	/**
	 * A file load request.
	 */
	public static final int LOAD = 0;

	/**
	 * A file save request.
	 */
	public static final int SAVE = 1;

	/**
	 * An autosave request. Only supported for local files.
	 */
	public static final int AUTOSAVE = 2;

	/**
	 * An insert file request.
	 */
	public static final int INSERT = 3;

	/**
	 * Magic numbers used for auto-detecting Unicode and GZIP files.
	 */
	public static final int GZIP_MAGIC_1 = 0x1f;
	public static final int GZIP_MAGIC_2 = 0x8b;
	public static final int UNICODE_MAGIC_1 = 0xfe;
	public static final int UNICODE_MAGIC_2 = 0xff;
	//}}}

	//{{{ BufferIORequest constructor
	/**
	 * Creates a new buffer I/O request.
	 * @param type The request type
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 */
	public BufferIORequest(int type, View view, Buffer buffer,
		Object session, VFS vfs, String path)
	{
		this.type = type;
		this.view = view;
		this.buffer = buffer;
		this.session = session;
		this.vfs = vfs;
		this.path = path;

		markersPath = vfs.getParentOfPath(path)
			+ '.' + vfs.getFileName(path)
			+ ".marks";
	} //}}}

	//{{{ run() method
	public void run()
	{
		switch(type)
		{
		case LOAD:
			load();
			break;
		case SAVE:
			save();
			break;
		case AUTOSAVE:
			autosave();
			break;
		case INSERT:
			insert();
			break;
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		String typeString;
		switch(type)
		{
		case LOAD:
			typeString = "LOAD";
			break;
		case SAVE:
			typeString = "SAVE";
			break;
		case AUTOSAVE:
			typeString = "AUTOSAVE";
			break;
		default:
			typeString = "UNKNOWN!!!";
		}

		return getClass().getName() + "[type=" + typeString
			+ ",buffer=" + buffer + "]";
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private int type;
	private View view;
	private Buffer buffer;
	private Object session;
	private VFS vfs;
	private String path;
	private String markersPath;
	//}}}

	//{{{ load() method
	private void load()
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
					setProgressValue(0);
				}

				path = vfs._canonPath(session,path,view);

				VFS.DirectoryEntry entry = vfs._getDirectoryEntry(
					session,path,view);
				long length;
				if(entry != null)
					length = entry.length;
				else
					length = 0L;

				in = vfs._createInputStream(session,path,false,view);
				if(in == null)
					return;

				in = new BufferedInputStream(in);

				if(in.markSupported())
				{
					in.mark(2);
					int b1 = in.read();
					int b2 = in.read();
					in.reset();

					if(b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2)
					{
						in = new GZIPInputStream(in);
						buffer.setBooleanProperty(Buffer.GZIPPED,true);
					}
					else if((b1 == UNICODE_MAGIC_1 && b2 == UNICODE_MAGIC_2)
						|| (b1 == UNICODE_MAGIC_2 && b2 == UNICODE_MAGIC_1))
					{
						buffer.setProperty(Buffer.ENCODING,"Unicode");
					}
				}
				else if(path.toLowerCase().endsWith(".gz"))
					in = new GZIPInputStream(in);

				read(buffer,in,length);
				buffer.setNewFile(false);
			}
			catch(CharConversionException ch)
			{
				Log.log(Log.ERROR,this,ch);
				Object[] pp = { buffer.getProperty(Buffer.ENCODING),
					ch.toString() };
				VFSManager.error(view,path,"ioerror.encoding-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(UnsupportedEncodingException uu)
			{
				Log.log(Log.ERROR,this,uu);
				Object[] pp = { buffer.getProperty(Buffer.ENCODING),
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
			if(in != null)
			{
				try
				{
					in.close();
				}
				catch(IOException io)
				{
				}
			}

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

	//{{{ read() method
	private void read(Buffer buffer, InputStream _in, long length)
		throws IOException
	{
		LongArray endOffsets = new LongArray();
		// we construct line information in the same format as
		// the offset manager expects it in
		long visible = (0xffL << OffsetManager.VISIBLE_SHIFT);

		// only true if the file size is known
		boolean trackProgress = (!buffer.isTemporary() && length != 0);

		if(trackProgress)
		{
			setProgressValue(0);
			setProgressMaximum((int)length);
		}

		// if the file size is not known, start with a resonable
		// default buffer size
		if(length == 0)
			length = IOBUFSIZE;

		SegmentBuffer seg = new SegmentBuffer((int)length + 1);

		InputStreamReader in = new InputStreamReader(_in,
			buffer.getStringProperty(Buffer.ENCODING));
		char[] buf = new char[IOBUFSIZE];

		// Number of characters in 'buf' array.
		// InputStream.read() doesn't always fill the
		// array (eg, the file size is not a multiple of
		// IOBUFSIZE, or it is a GZipped file, etc)
		int len;

		// True if a \n was read after a \r. Usually
		// means this is a DOS/Windows file
		boolean CRLF = false;

		// A \r was read, hence a MacOS file
		boolean CROnly = false;

		// Was the previous read character a \r?
		// If we read a \n and this is true, we assume
		// we have a DOS/Windows file
		boolean lastWasCR = false;

		// Number of lines read. Every 100 lines, we update the
		// progress bar
		int lineCount = 0;

		while((len = in.read(buf,0,buf.length)) != -1)
		{
			// Offset of previous line, relative to
			// the start of the I/O buffer (NOT
			// relative to the start of the document)
			int lastLine = 0;

			for(int i = 0; i < len; i++)
			{
				// Look for line endings.
				switch(buf[i])
				{
				case '\r':
					// If we read a \r and
					// lastWasCR is also true,
					// it is probably a Mac file
					// (\r\r in stream)
					if(lastWasCR)
					{
						CROnly = true;
						CRLF = false;
					}
					// Otherwise set a flag,
					// so that \n knows that last
					// was a \r
					else
					{
						lastWasCR = true;
					}

					// Insert a line
					seg.append(buf,lastLine,i -
						lastLine);
					seg.append('\n');
					endOffsets.add(seg.count | visible);
					if(trackProgress && lineCount++ % PROGRESS_INTERVAL == 0)
						setProgressValue(seg.count);

					// This is i+1 to take the
					// trailing \n into account
					lastLine = i + 1;
					break;
				case '\n':
					// If lastWasCR is true,
					// we just read a \r followed
					// by a \n. We specify that
					// this is a Windows file,
					// but take no further
					// action and just ignore
					// the \r.
					if(lastWasCR)
					{
						CROnly = false;
						CRLF = true;
						lastWasCR = false;
						// Bump lastLine so
						// that the next line
						// doesn't erronously
						// pick up the \r
						lastLine = i + 1;
					}
					// Otherwise, we found a \n
					// that follows some other
					// character, hence we have
					// a Unix file
					else
					{
						CROnly = false;
						CRLF = false;
						seg.append(buf,lastLine,
							i - lastLine);
						seg.append('\n');
						endOffsets.add(seg.count | visible);
						if(trackProgress && lineCount++ % PROGRESS_INTERVAL == 0)
							setProgressValue(seg.count);
						lastLine = i + 1;
					}
					break;
				default:
					// If we find some other
					// character that follows
					// a \r, so it is not a
					// Windows file, and probably
					// a Mac file
					if(lastWasCR)
					{
						CROnly = true;
						CRLF = false;
						lastWasCR = false;
					}
					break;
				}
			}

			if(trackProgress)
				setProgressValue(seg.count);

			// Add remaining stuff from buffer
			seg.append(buf,lastLine,len - lastLine);
		}

		setAbortable(false);

		String lineSeparator;
		if(CRLF)
			lineSeparator = "\r\n";
		else if(CROnly)
			lineSeparator = "\r";
		else
			lineSeparator = "\n";

		in.close();

		// Chop trailing newline and/or ^Z (if any)
		int bufferLength = seg.count;
		if(bufferLength != 0)
		{
			char ch = seg.array[bufferLength - 1];
			if(ch == 0x1a /* DOS ^Z */)
				seg.count--;
		}

		buffer.setBooleanProperty(Buffer.TRAILING_EOL,false);
		if(bufferLength != 0 && jEdit.getBooleanProperty("stripTrailingEOL"))
		{
			char ch = seg.array[bufferLength - 1];
			if(ch == '\n')
			{
				buffer.setBooleanProperty(Buffer.TRAILING_EOL,true);
				seg.count--;
				endOffsets.setSize(endOffsets.getSize() - 1);
			}
		}

		// add a line marker at the end for proper offset manager
		// operation
		endOffsets.add(seg.count + 1 | visible);

		// to avoid having to deal with read/write locks and such,
		// we insert the loaded data into the buffer in the
		// post-load cleanup runnable, which runs in the AWT thread.
		buffer.setProperty(LOAD_DATA,seg);
		buffer.setProperty(END_OFFSETS,endOffsets);
		buffer.setProperty(NEW_PATH,path);
		buffer.setProperty(Buffer.LINESEP,lineSeparator);
	} //}}}

	//{{{ readMarkers() method
	private void readMarkers(Buffer buffer, InputStream _in)
		throws IOException
	{
		// For `reload' command
		buffer.removeAllMarkers();

		BufferedReader in = new BufferedReader(new InputStreamReader(_in));

		String line;
		while((line = in.readLine()) != null)
		{
			// compatibility kludge for jEdit 3.1 and earlier
			if(!line.startsWith("!"))
				continue;

			char shortcut = line.charAt(1);
			int start = line.indexOf(';');
			int end = line.indexOf(';',start + 1);
			int position = Integer.parseInt(line.substring(start + 1,end));
			buffer.addMarker(shortcut,position);
		}

		in.close();
	} //}}}

	//{{{ save() method
	private void save()
	{
		OutputStream out = null;

		try
		{
			String[] args = { vfs.getFileName(path) };
			setStatus(jEdit.getProperty("vfs.status.save",args));

			// the entire save operation can be aborted...
			setAbortable(true);

			try
			{
				path = vfs._canonPath(session,path,view);

				buffer.readLock();

				// Only backup once per session
				if(buffer.getProperty(Buffer.BACKED_UP) == null
					|| jEdit.getBooleanProperty("backupEverySave"))
				{
					vfs._backup(session,path,view);
					buffer.setBooleanProperty(Buffer.BACKED_UP,true);
				}

				/* if the VFS supports renaming files, we first
				 * save to #<filename>#save#, then rename that
				 * to <filename>, so that if the save fails,
				 * data will not be lost.
				 *
				 * as of 4.1pre7 we now call vfs.getTwoStageSaveName()
				 * instead of constructing the path directly
				 * since some VFS's might not allow # in filenames.
				 */
				String savePath;

				boolean twoStageSave = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0
					&& jEdit.getBooleanProperty("twoStageSave");
				if(twoStageSave)
					savePath = vfs.getTwoStageSaveName(path);
				else
					savePath = path;

				out = vfs._createOutputStream(session,savePath,view);
				if(out != null)
				{
					// Can't use buffer.getName() here because
					// it is not changed until the save is
					// complete
					if(savePath.endsWith(".gz"))
						buffer.setBooleanProperty(Buffer.GZIPPED,true);

					if(buffer.getBooleanProperty(Buffer.GZIPPED))
						out = new GZIPOutputStream(out);

					write(buffer,out);

					if(twoStageSave)
					{
						if(!vfs._rename(session,savePath,path,view))
							throw new IOException(savePath);
					}

					// We only save markers to VFS's that support deletion.
					// Otherwise, we will accumilate stale marks files.
					if((vfs.getCapabilities() & VFS.DELETE_CAP) != 0)
					{
						if(jEdit.getBooleanProperty("persistentMarkers")
							&& buffer.getMarkers().size() != 0)
						{
							setStatus(jEdit.getProperty("vfs.status.save-markers",args));
							setProgressValue(0);
							out = vfs._createOutputStream(session,markersPath,view);
							if(out != null)
								writeMarkers(buffer,out);
						}
						else
							vfs._delete(session,markersPath,view);
					}
				}
				else
					buffer.setBooleanProperty(ERROR_OCCURRED,true);

				if(!twoStageSave)
					VFSManager.sendVFSUpdate(vfs,path,true);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(view,path,"ioerror.write-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			finally
			{
				buffer.readUnlock();
			}
		}
		catch(WorkThread.Abort a)
		{
			if(out != null)
			{
				try
				{
					out.close();
				}
				catch(IOException io)
				{
				}
			}

			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		finally
		{
			try
			{
				vfs._saveComplete(session,buffer,path,view);
				vfs._endVFSSession(session,view);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(view,path,"ioerror.write-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
			catch(WorkThread.Abort a)
			{
				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
		}
	} //}}}

	//{{{ autosave() method
	private void autosave()
	{
		OutputStream out = null;

		try
		{
			String[] args = { vfs.getFileName(path) };
			setStatus(jEdit.getProperty("vfs.status.autosave",args));

			// the entire save operation can be aborted...
			setAbortable(true);

			try
			{
				//buffer.readLock();

				if(!buffer.isDirty())
				{
					// buffer has been saved while we
					// were waiting.
					return;
				}

				out = vfs._createOutputStream(session,path,view);
				if(out == null)
					return;

				write(buffer,out);
			}
			catch(Exception e)
			{
			}
			finally
			{
				//buffer.readUnlock();
			}
		}
		catch(WorkThread.Abort a)
		{
			if(out != null)
			{
				try
				{
					out.close();
				}
				catch(IOException io)
				{
				}
			}
		}
	} //}}}

	//{{{ write() method
	private void write(Buffer buffer, OutputStream _out)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(_out,
				buffer.getStringProperty(Buffer.ENCODING)),
				IOBUFSIZE);
		Segment lineSegment = new Segment();
		String newline = buffer.getStringProperty(Buffer.LINESEP);
		if(newline == null)
			newline = System.getProperty("line.separator");

		setProgressMaximum(buffer.getLineCount() / PROGRESS_INTERVAL);
		setProgressValue(0);

		int i = 0;
		while(i < buffer.getLineCount())
		{
			buffer.getLineText(i,lineSegment);
			out.write(lineSegment.array,lineSegment.offset,
				lineSegment.count);

			if(i != buffer.getLineCount() - 1)
			{
				out.write(newline);
			}

			if(++i % PROGRESS_INTERVAL == 0)
				setProgressValue(i / PROGRESS_INTERVAL);
		}

		if(jEdit.getBooleanProperty("stripTrailingEOL")
			&& buffer.getBooleanProperty(Buffer.TRAILING_EOL))
		{
			out.write(newline);
		}

		out.close();
	} //}}}

	//{{{ writeMarkers() method
	private void writeMarkers(Buffer buffer, OutputStream out)
		throws IOException
	{
		Writer o = new BufferedWriter(new OutputStreamWriter(out));
		Vector markers = buffer.getMarkers();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			o.write('!');
			o.write(marker.getShortcut());
			o.write(';');

			String pos = String.valueOf(marker.getPosition());
			o.write(pos);
			o.write(';');
			o.write(pos);
			o.write('\n');
		}
		o.close();
	} //}}}

	//{{{ insert() method
	private void insert()
	{
		InputStream in = null;

		try
		{
			try
			{
				String[] args = { vfs.getFileName(path) };
				setStatus(jEdit.getProperty("vfs.status.load",args));
				setAbortable(true);

				path = vfs._canonPath(session,path,view);

				VFS.DirectoryEntry entry = vfs._getDirectoryEntry(
					session,path,view);
				long length;
				if(entry != null)
					length = entry.length;
				else
					length = 0L;

				in = vfs._createInputStream(session,path,false,view);
				if(in == null)
					return;

				if(path.endsWith(".gz"))
					in = new GZIPInputStream(in);

				read(buffer,in,length);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { io.toString() };
				VFSManager.error(view,path,"ioerror.read-error",pp);

				buffer.setBooleanProperty(ERROR_OCCURRED,true);
			}
		}
		catch(WorkThread.Abort a)
		{
			if(in != null)
			{
				try
				{
					in.close();
				}
				catch(IOException io)
				{
				}
			}

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

	//}}}
}
