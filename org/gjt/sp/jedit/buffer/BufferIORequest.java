/*
 * BufferIORequest.java - I/O request
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
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
import java.nio.charset.Charset;
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
public abstract class BufferIORequest extends WorkRequest
{
	//{{{ Constants
	public static final int UTF8_MAGIC_1 = 0xef;
	public static final int UTF8_MAGIC_2 = 0xbb;
	public static final int UTF8_MAGIC_3 = 0xbf;

	/**
	 * Magic numbers used for auto-detecting Unicode and GZIP files.
	 */
	public static final int GZIP_MAGIC_1 = 0x1f;
	public static final int GZIP_MAGIC_2 = 0x8b;
	public static final int UNICODE_MAGIC_1 = 0xfe;
	public static final int UNICODE_MAGIC_2 = 0xff;

	/**
	 * Length of longest XML PI used for encoding detection.<p>
	 * &lt;?xml version="1.0" encoding="................"?&gt;
	 */
	public static final int XML_PI_LENGTH = 50;

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
	public BufferIORequest(View view, Buffer buffer,
		Object session, VFS vfs, String path)
	{
		this.view = view;
		this.buffer = buffer;
		this.session = session;
		this.vfs = vfs;
		this.path = path;

		markersPath = vfs.getParentOfPath(path)
			+ '.' + vfs.getFileName(path)
			+ ".marks";
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[" + buffer + "]";
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	protected View view;
	protected Buffer buffer;
	protected Object session;
	protected VFS vfs;
	protected String path;
	protected String markersPath;
	//}}}

	//{{{ getXMLEncoding() method
	/**
	 * Extract XML encoding name from PI.
	 */
	private String getXMLEncoding(String xmlPI)
	{
		if(!xmlPI.startsWith("<?xml"))
			return null;

		int index = xmlPI.indexOf("encoding=");
		if(index == -1 || index + 9 == xmlPI.length())
			return null;

		char ch = xmlPI.charAt(index + 9);
		int endIndex = xmlPI.indexOf(ch,index + 10);
		if(endIndex == -1)
			return null;

		String encoding = xmlPI.substring(index + 10,endIndex);

		if(Charset.isSupported(encoding))
			return encoding;
		else
		{
			Log.log(Log.WARNING,this,"XML PI specifies "
				+ "unsupported encoding: " + encoding);
			return null;
		}
	} //}}}

	//{{{ autodetect() method
	/**
	 * Tries to detect if the stream is gzipped, and if it has an encoding
	 * specified with an XML PI.
	 */
	protected Reader autodetect(InputStream in) throws IOException
	{
		in = new BufferedInputStream(in);

		String encoding = buffer.getStringProperty(Buffer.ENCODING);
		if(!in.markSupported())
			Log.log(Log.WARNING,this,"Mark not supported: " + in);
		else if(buffer.getBooleanProperty(Buffer.ENCODING_AUTODETECT))
		{
			in.mark(XML_PI_LENGTH);
			int b1 = in.read();
			int b2 = in.read();
			int b3 = in.read();

			if(b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2)
			{
				in.reset();
				in = new GZIPInputStream(in);
				buffer.setBooleanProperty(Buffer.GZIPPED,true);
				// auto-detect encoding within the gzip stream.
				return autodetect(in);
			}
			else if((b1 == UNICODE_MAGIC_1
				&& b2 == UNICODE_MAGIC_2)
				|| (b1 == UNICODE_MAGIC_2
				&& b2 == UNICODE_MAGIC_1))
			{
				in.reset();
				encoding = "UTF-16";
				buffer.setProperty(Buffer.ENCODING,encoding);
			}
			else if(b1 == UTF8_MAGIC_1 && b2 == UTF8_MAGIC_2
				&& b3 == UTF8_MAGIC_3)
			{
				// do not reset the stream and just treat it
				// like a normal UTF-8 file.
				buffer.setProperty(Buffer.ENCODING,
					MiscUtilities.UTF_8_Y);

				encoding = "UTF-8";
			}
			else
			{
				in.reset();

				byte[] _xmlPI = new byte[XML_PI_LENGTH];
				int offset = 0;
				int count;
				while((count = in.read(_xmlPI,offset,
					XML_PI_LENGTH - offset)) != -1)
				{
					offset += count;
					if(offset == XML_PI_LENGTH)
						break;
				}

				String xmlEncoding = getXMLEncoding(new String(
					_xmlPI,0,offset,"ASCII"));
				if(xmlEncoding != null)
				{
					encoding = xmlEncoding;
					buffer.setProperty(Buffer.ENCODING,encoding);
				}

				if(encoding.equals(MiscUtilities.UTF_8_Y))
					encoding = "UTF-8";

				in.reset();
			}
		}

		return new InputStreamReader(in,encoding);
	} //}}}

	//{{{ read() method
	protected SegmentBuffer read(Reader in, long length,
		boolean insert) throws IOException
	{
		/* we guess an initial size for the array */
		IntegerArray endOffsets = new IntegerArray(
			Math.max(1,(int)(length / 50)));

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
					endOffsets.add(seg.count);
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
						endOffsets.add(seg.count);
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
		if(seg.count == 0)
		{
			// fix for "[ 865589 ] 0-byte files should open using
			// the default line seperator"
			lineSeparator = jEdit.getProperty(
				"buffer.lineSeparator",
				System.getProperty("line.separator"));
		}
		else if(CRLF)
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
		endOffsets.add(seg.count + 1);

		// to avoid having to deal with read/write locks and such,
		// we insert the loaded data into the buffer in the
		// post-load cleanup runnable, which runs in the AWT thread.
		if(!insert)
		{
			buffer.setProperty(LOAD_DATA,seg);
			buffer.setProperty(END_OFFSETS,endOffsets);
			buffer.setProperty(NEW_PATH,path);
			if(lineSeparator != null)
				buffer.setProperty(Buffer.LINESEP,lineSeparator);
		}

		// used in insert()
		return seg;
	} //}}}

	//{{{ write() method
	protected void write(Buffer buffer, OutputStream _out)
		throws IOException
	{
		BufferedWriter out = null;

		try
		{
			String encoding = buffer.getStringProperty(Buffer.ENCODING);
			if(encoding.equals(MiscUtilities.UTF_8_Y))
			{
				// not supported by Java...
				_out.write(UTF8_MAGIC_1);
				_out.write(UTF8_MAGIC_2);
				_out.write(UTF8_MAGIC_3);
				_out.flush();
				encoding = "UTF-8";
			}

			out = new BufferedWriter(
				new OutputStreamWriter(_out,encoding),
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
		}
		finally
		{
			if(out != null)
				out.close();
			else
				_out.close();
		}
	} //}}}

	//}}}
}
