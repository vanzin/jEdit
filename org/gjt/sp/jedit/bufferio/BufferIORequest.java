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

package org.gjt.sp.jedit.bufferio;

//{{{ Imports
import java.io.BufferedOutputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;

import javax.swing.text.Segment;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.Encoding;
import org.gjt.sp.jedit.io.EncodingServer;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.SegmentBuffer;
import org.gjt.sp.util.WorkRequest;
//}}}

/**
 * A buffer I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class BufferIORequest extends WorkRequest
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

	// These are no longer used but still here only for compatibility.
	@Deprecated public static final int GZIP_MAGIC_1 = 0x1f;
	@Deprecated public static final int GZIP_MAGIC_2 = 0x8b;

	//}}}

	//{{{ Instance variables
	protected final View view;
	protected final Buffer buffer;
	protected final Object session;
	protected final VFS vfs;
	protected String path;
	protected final String markersPath;
	//}}}

	//{{{ BufferIORequest constructor
	/**
	 * Creates a new buffer I/O request.
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 */
	protected BufferIORequest(View view, Buffer buffer,
		Object session, VFS vfs, String path)
	{
		this.view = view;
		this.buffer = buffer;
		this.session = session;
		this.vfs = vfs;
		this.path = path;

		markersPath = Buffer.getMarkersPath(vfs, path);
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + '[' + buffer + ']';
	} //}}}

	//{{{ getCharIOBufferSize() method
	/**
	 * Size of character I/O buffers.
	 */
	public static int getCharIOBufferSize()
	{
		return IOBUFSIZE;
	} //}}}

	//{{{ getByteIOBufferSize() method
	/**
	 * Size of byte I/O buffers.
	 */
	public static int getByteIOBufferSize()
	{
		// 2 is sizeof char in byte;
		return IOBUFSIZE * 2;
	} //}}}

	//{{{ autodetect() method
	/**
	 * Tries to detect if the stream is gzipped, and if it has an encoding
	 * specified with an XML PI.
	 */
	protected Reader autodetect(InputStream in) throws IOException
	{
		return MiscUtilities.autodetect(in, buffer);
	} //}}}

	//{{{ read() method
	protected SegmentBuffer read(Reader in, long length,
		boolean insert) throws IOException
	{
		/* we guess an initial size for the array */
		IntegerArray endOffsets = new IntegerArray(
			Math.max(1,(int)(length / 50)));

		// only true if the file size is known
		boolean trackProgress = !buffer.isTemporary() && length != 0;

		if(trackProgress)
		{
			setMaximum(length);
			setValue(0);
		}

		// if the file size is not known, start with a resonable
		// default buffer size
		if(length == 0)
			length = IOBUFSIZE;

		SegmentBuffer seg = new SegmentBuffer((int)length + 1);

		char[] buf = new char[IOBUFSIZE];

		/* Number of characters in 'buf' array.
		 InputStream.read() doesn't always fill the
		 array (eg, the file size is not a multiple of
		 IOBUFSIZE, or it is a GZipped file, etc) */
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
						setValue(seg.count);

					// This is i+1 to take the
					// trailing \n into account
					lastLine = i + 1;
					break;
				case '\n':
					/* If lastWasCR is true, we just read a \r followed
					 by a \n. We specify that this is a Windows file,
					 but take no further action and just ignore the \r. */
					if(lastWasCR)
					{
						CROnly = false;
						CRLF = true;
						lastWasCR = false;
						/* Bump lastLine so that the next line doesn't erronously
						  pick up the \r */
						lastLine = i + 1;
					}
					/* Otherwise, we found a \n that follows some other
					 *  character, hence we have a Unix file */
					else
					{
						CROnly = false;
						CRLF = false;
						seg.append(buf,lastLine,
							i - lastLine);
						seg.append('\n');
						endOffsets.add(seg.count);
						if(trackProgress && lineCount++ % PROGRESS_INTERVAL == 0)
							setValue(seg.count);
						lastLine = i + 1;
					}
					break;
				default:
					/*  If we find some other character that follows
					 a \r, so it is not a Windows file, and probably
					 a Mac file */
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
				setValue(seg.count);

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
				buffer.setProperty(JEditBuffer.LINESEP,lineSeparator);
		}

		// used in insert()
		return seg;
	} //}}}

	//{{{ write() method
	protected void write(Buffer buffer, OutputStream out)
		throws IOException
	{
		String encodingName
			= buffer.getStringProperty(JEditBuffer.ENCODING);
		Encoding encoding = EncodingServer.getEncoding(encodingName);
		Writer writer = encoding.getTextWriter(
			new BufferedOutputStream(out, getByteIOBufferSize()));

		Segment lineSegment = new Segment();
		String newline = buffer.getStringProperty(JEditBuffer.LINESEP);
		if(newline == null)
			newline = System.getProperty("line.separator");

		final int bufferLineCount = buffer.getLineCount();
		setMaximum(bufferLineCount / PROGRESS_INTERVAL);
		setValue(0);

		int i = 0;
		while(i < bufferLineCount)
		{
			buffer.getLineText(i,lineSegment);
			try
			{
				writer.write(lineSegment.array,
					lineSegment.offset,
					lineSegment.count);
				if(i < bufferLineCount - 1
					|| (jEdit.getBooleanProperty("stripTrailingEOL")
						&& buffer.getBooleanProperty(Buffer.TRAILING_EOL)))
				{
					writer.write(newline);
				}
			}
			catch(CharacterCodingException e)
			{
				String message = getWriteEncodingErrorMessage(
					encodingName, encoding,
					lineSegment, i);
				IOException wrapping = new CharConversionException(message);
				wrapping.initCause(e);
				throw wrapping;
			}

			if(++i % PROGRESS_INTERVAL == 0)
				setValue(i / PROGRESS_INTERVAL);
		}
		writer.flush();
	} //}}}

	//{{{ Private members

	//{{{ createEncodingErrorMessage() method
	private static String getWriteEncodingErrorMessage(
		String encodingName, Encoding encoding,
		Segment line, int lineIndex)
	{
		String args[] = {
			encodingName,
			Integer.toString(lineIndex + 1),
			"UNKNOWN", // column
			"UNKNOWN"  // the character
		};
		try
		{
			int charIndex = getFirstGuiltyCharacterIndex(encoding, line);
			if(0 <= charIndex && charIndex < line.count)
			{
				char c = line.array[line.offset + charIndex];
				args[2] = Integer.toString(charIndex + 1);
				args[3] = "'" + c + "' (U+" + Integer.toHexString(c).toUpperCase() + ")";
			}
		}
		catch(Exception e)
		{
			// Ignore.
		}
		return jEdit.getProperty("ioerror.write-encoding-error", args);
	} //}}}

	//{{{ getFirstGuiltyCharacterIndex() method
	// Look for the first character which causes encoding error.
	private static int getFirstGuiltyCharacterIndex(Encoding encoding,
		Segment line) throws IOException
	{
		if(line.count < 1)
		{
			return -1;
		}
		else if(line.count == 1)
		{
			return 0;
		}

		Writer tester = encoding.getTextWriter(
			new OutputStream()
			{
				public void write(int b) {}
			});
		for(int i = 0; i < line.count; ++i)
		{
			try
			{
				tester.write(line.array[line.offset + i]);
			}
			catch(CharacterCodingException e)
			{
				return i;
			}
		}
		return -1;
	} //}}}

	//}}}
}
