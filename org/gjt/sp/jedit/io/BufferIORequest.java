/*
 * BufferIORequest.java - I/O request
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import java.io.*;
import java.util.zip.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

/**
 * A buffer I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferIORequest extends WorkRequest
{
	/**
	 * Size of I/O buffers.
	 */
	public static final int IOBUFSIZE = 32768;

	/**
	 * Number of lines per progress increment.
	 */
	public static final int PROGRESS_INTERVAL = 300;

	/**
	 * Property loaded data is stored in.
	 */
	public static final String LOAD_DATA = "IORequest__loadData";

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
	}

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
	}

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
	}

	// private members
	private int type;
	private View view;
	private Buffer buffer;
	private Object session;
	private VFS vfs;
	private String path;
	private String markersPath;

	private void load()
	{
		InputStream in = null;

		try
		{
			try
			{
				String[] args = { vfs.getFileName(path) };
				setStatus(jEdit.getProperty("vfs.status.load",args));
				setAbortable(true);
				setProgressValue(0);

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

				String lineSeparator = read(buffer,in,length);
				buffer.putProperty(Buffer.LINESEP,lineSeparator);
				buffer.setNewFile(false);
			}
			catch(CharConversionException ch)
			{
				Log.log(Log.ERROR,this,ch);
				Object[] pp = { path,
					buffer.getProperty(Buffer.ENCODING),
					ch.toString() };
				VFSManager.error(view,"encoding-error",pp);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				Object[] pp = { path, io.toString() };
				VFSManager.error(view,"read-error",pp);
			}

			if(jEdit.getBooleanProperty("persistentMarkers"))
			{
				try
				{
					String[] args = { vfs.getFileName(path) };
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
				String[] pp = { path, io.toString() };
				VFSManager.error(view,"read-error",pp);
			}
			catch(WorkThread.Abort a)
			{
			}
		}
	}

	/**
	 * Reads the buffer from the specified input stream. Read and
	 * understand all these notes if you want to snarf this code for
	 * your own app; it has a number of subtle behaviours which are
	 * not entirely obvious.<p>
	 *
	 * Some notes that will help future hackers:
	 * <ul>
	 * <li>
	 * We use a StringBuffer because there is no way to pre-allocate
	 * in the GapContent - and adding text each time to the GapContent
	 * would be slow because it would require array enlarging, etc.
	 * Better to do as few gap inserts as possible.
	 *
	 * <li>The StringBuffer is pre-allocated to the file's size (obtained
	 * from the VFS). If the file size is not known, we default to
	 * IOBUFSIZE.
	 *
	 * <li>We read the stream in IOBUFSIZE (= 32k) blocks, and loop over
	 * the read characters looking for line breaks.
	 * <ul>
	 * <li>a \r or \n causes a line to be added to the model, and appended
	 * to the string buffer
	 * <li>a \n immediately following an \r is ignored; so that Windows
	 * line endings are handled
	 * </ul>
	 *
	 * <li>This method remembers the line separator used in the file, and
	 * stores it in the lineSeparator buffer-local property. However,
	 * if the file contains, say, hello\rworld\n, lineSeparator will
	 * be set to \n, and the file will be saved as hello\nworld\n.
	 * Hence jEdit is not really appropriate for editing binary files.
	 *
	 * <li>To make reloading a bit easier, this method automatically
	 * removes all data from the model before inserting it. This
	 * shouldn't cause any problems, as most documents will be
	 * empty before being loaded into anyway.
	 *
	 * <li>If the last character read from the file is a line separator,
	 * it is not added to the model! There are two reasons:
	 * <ul>
	 * <li>On Unix, all text files have a line separator at the end,
	 * there is no point wasting an empty screen line on that
	 * <li>Because save() appends a line separator after *every* line,
	 * it prevents the blank line count at the end from growing
	 * </ul>
	 * 
	 * </ul>
	 */
	private String read(Buffer buffer, InputStream _in, long length)
		throws IOException
	{
		// only true if the file size is known
		boolean trackProgress = (length != 0);
		File file = buffer.getFile();

		setProgressValue(0);
		setProgressMaximum((int)length);

		// if the file size is not known, start with a resonable
		// default buffer size
		if(length == 0)
			length = IOBUFSIZE;

		StringBuffer sbuf = new StringBuffer((int)length);

		InputStreamReader in = new InputStreamReader(_in,
			(String)buffer.getProperty(Buffer.ENCODING));
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
					sbuf.append(buf,lastLine,i -
						lastLine);
					sbuf.append('\n');
					if(trackProgress && lineCount++ % PROGRESS_INTERVAL == 0)
						setProgressValue(sbuf.length());

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
						sbuf.append(buf,lastLine,
							i - lastLine);
						sbuf.append('\n');
						if(trackProgress && lineCount++ % PROGRESS_INTERVAL == 0)
							setProgressValue(sbuf.length());
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
				setProgressValue(sbuf.length());

			// Add remaining stuff from buffer
			sbuf.append(buf,lastLine,len - lastLine);
		}

		setAbortable(false);

		String returnValue;
		if(CRLF)
			returnValue = "\r\n";
		else if(CROnly)
			returnValue = "\r";
		else
			returnValue = "\n";

		in.close();

		// Chop trailing newline and/or ^Z (if any)
		int bufferLength = sbuf.length();
		if(bufferLength != 0)
		{
			char ch = sbuf.charAt(bufferLength - 1);
			if(length >= 2 && ch == 0x1a /* DOS ^Z */
				&& sbuf.charAt(bufferLength - 2) == '\n')
				sbuf.setLength(bufferLength - 2);
			else if(ch == '\n')
				sbuf.setLength(bufferLength - 1);
		}

		// to avoid having to deal with read/write locks and such,
		// we insert the loaded data into the buffer in the
		// post-load cleanup runnable, which runs in the AWT thread.
		buffer.putProperty(LOAD_DATA,sbuf);

		return returnValue;
	}

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
	}

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
				buffer.readLock();

				/* if the VFS supports renaming files, we first
				 * save to #<filename>#save#, then rename that
				 * to <filename>, so that if the save fails,
				 * data will not be lost */
				String savePath;

				if((vfs.getCapabilities() & VFS.RENAME_CAP) != 0)
				{
					savePath = vfs.getParentOfPath(path)
						+ '#' + vfs.getFileName(path)
						+ "#save#";
				}
				else
					savePath = path;

				out = vfs._createOutputStream(session,savePath,view);
				if(out != null)
				{
					if(path.endsWith(".gz"))
						out = new GZIPOutputStream(out);

					write(buffer,out);
				}

				// Only backup once per session
				if(buffer.getProperty(Buffer.BACKED_UP) == null)
				{
					vfs._backup(session,path,view);
					buffer.putProperty(Buffer.BACKED_UP,Boolean.TRUE);
				}

				if((vfs.getCapabilities() & VFS.RENAME_CAP) != 0)
					vfs._rename(session,savePath,path,view);

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
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { path, io.toString() };
				VFSManager.error(view,"write-error",pp);
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
		}
		finally
		{
			try
			{
				vfs._saveComplete(session,buffer,view);
				vfs._endVFSSession(session,view);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] pp = { path, io.toString() };
				VFSManager.error(view,"write-error",pp);
			}
			catch(WorkThread.Abort a)
			{
			}
		}
	}

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
				buffer.readLock();

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
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			catch(IOException io)
			{
				/* Log.log(Log.ERROR,this,io);
				args[0] = io.toString();
				VFSManager.error(view,"ioerror",args); */
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
		}
	}

	private void write(Buffer buffer, OutputStream _out)
		throws IOException, BadLocationException
	{
		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(_out,
				(String)buffer.getProperty(Buffer.ENCODING)),
				IOBUFSIZE);
		Segment lineSegment = new Segment();
		String newline = (String)buffer.getProperty(Buffer.LINESEP);
		if(newline == null)
			newline = System.getProperty("line.separator");
		Element map = buffer.getDefaultRootElement();

		setProgressMaximum(map.getElementCount() / PROGRESS_INTERVAL);
		setProgressValue(0);

		int i = 0;
		while(i < map.getElementCount())
		{
			Element line = map.getElement(i);
			int start = line.getStartOffset();
			buffer.getText(start,line.getEndOffset() - start - 1,
				lineSegment);
			out.write(lineSegment.array,lineSegment.offset,
				lineSegment.count);
			out.write(newline);

			if(++i % PROGRESS_INTERVAL == 0)
				setProgressValue(i / PROGRESS_INTERVAL);
		}
		out.close();
	}

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
	}

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
				String[] pp = { path, io.toString() };
				VFSManager.error(view,"read-error",pp);
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
				String[] pp = { path, io.toString() };
				VFSManager.error(view,"read-error",pp);
			}
			catch(WorkThread.Abort a)
			{
			}
		}
	}
}
