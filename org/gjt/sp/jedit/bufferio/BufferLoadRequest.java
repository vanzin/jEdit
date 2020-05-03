/*
 * BufferLoadRequest.java - I/O request
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.nio.charset.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
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
	private final boolean untitled;
	
	//{{{ BufferLoadRequest constructor
	/**
	 * Creates a new buffer I/O request.
	 * @param view The view
	 * @param buffer The buffer
	 * @param session The VFS session
	 * @param vfs The VFS
	 * @param path The path
	 * @param untitled is the buffer untitled
	 */
	public BufferLoadRequest(View view, Buffer buffer,
		Object session, VFS vfs, String path, boolean untitled)
	{
		super(view,buffer,session,vfs,path);
		this.untitled = untitled;
	} //}}}

	//{{{ run() method
	@Override
	public void _run()
	{
		try
		{
			setCancellable(true);
			if(!buffer.isTemporary())
			{
				String[] args = { vfs.getFileName(path) };
				setStatus(jEdit.getProperty("vfs.status.load",args));
				setValue(0L);
			}

			path = vfs._canonPath(session,path,view);

			readContents();
			buffer.setNewFile(untitled);

			if (jEdit.getBooleanProperty("persistentMarkers") &&
			    (vfs.isMarkersFileSupported()))
			{
				InputStream markers = null;
				try
				{
					String[] args = { vfs.getFileName(path) };
					if(!buffer.isTemporary())
						setStatus(jEdit.getProperty("vfs.status.load-markers",args));
					setCancellable(true);

					markers = vfs._createInputStream(session,markersPath,true,view);
					if(markers != null)
						readMarkers(buffer,markers);
				}
				catch(Exception e)
				{
					// ignore
				}
				finally
				{
					IOUtilities.closeQuietly(markers);
				}
			}
		}
		catch(InterruptedException e)
		{
			buffer.setBooleanProperty(ERROR_OCCURRED,true);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			Object[] pp = { e.toString() };
			VFSManager.error(view,path,"ioerror.read-error",pp);

			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		catch(OutOfMemoryError oom)
		{
			Log.log(Log.ERROR,this,oom);
			VFSManager.error(view,path,"out-of-memory-error",null);

			buffer.setBooleanProperty(ERROR_OCCURRED,true);
		}
		finally
		{
			endSessionQuietly();
		}
	} //}}}

	//{{{ getNakedStream() method
	/**
	 * Returns the raw contents stream for this load request.
	 * This stream is not buffered or unzipped.
	 */
	private InputStream getNakedStream() throws IOException
	{
		InputStream in = vfs._createInputStream(session,path,false,view);
		if(in != null)
		{
			return in;
		}
		throw new IOException("Unable to get a Stream for " + path);
	} //}}}

	//{{{ getContentLength() method
	/**
	 * Returns content length of this load request.
	 */
	private long getContentLength() throws IOException
	{
		VFSFile entry = vfs._getFile(session,path,view);
		if(entry != null)
			return entry.getLength();
		else
			return 0L;
	} //}}}

	//{{{ rewindContentsStream() method
	/**
	 * Returns rewinded contents stream.
	 * This method assumes the marked stream was made by
	 * getMarkedStream() method. The stream may be reopened if reset()
	 * failed.
	 */
	private BufferedInputStream rewindContentsStream(BufferedInputStream markedStream, boolean gzipped)
		throws IOException
	{
		try
		{
			markedStream.reset();
			return markedStream;
		}
		catch(IOException e)
		{
			Log.log(Log.NOTICE, this
				, path + ": Reopening to rewind the stream");
			// Reopen the stream because the mark has been
			// invalidated while previous reading.
			markedStream.close();
			InputStream in = getNakedStream();
			try
			{
				if(gzipped)
				{
					in = new GZIPInputStream(in);
				}
				BufferedInputStream result
					= AutoDetection.getMarkedStream(in);
				in = null;
				return result;
			}
			finally
			{
				IOUtilities.closeQuietly((Closeable)in);
			}
		}
	} //}}}

	//{{{ readContents() method
	/**
	 * Read the contents of this load request.
	 * Some auto detection is performed if enabled.
	 *   - GZIPed file
	 *   - The encoding
	 * If fallback encodings are specified, they are used on
	 * encoding errors.
	 * @throws InterruptedException
	 */
	private void readContents() throws IOException, InterruptedException
	{
		long length = getContentLength();

		BufferedInputStream markedStream
			= AutoDetection.getMarkedStream(getNakedStream());
		try
		{
			boolean gzipped;
			// encodingProviders is consist of given
			// encodings as String or contents-aware
			// detectors as EncodingDetector.
			List<Object> encodingProviders = new ArrayList<>();

			boolean autodetect = buffer.getBooleanProperty(Buffer.ENCODING_AUTODETECT);
			if(autodetect)
			{
				gzipped = AutoDetection.isGzipped(markedStream);
				markedStream.reset();

				encodingProviders.addAll(AutoDetection.getEncodingDetectors());
				// If the detected encoding fail, fallback to
				// the original encoding.
				encodingProviders.add(buffer.getStringProperty(JEditBuffer.ENCODING));

				String fallbackEncodings = jEdit.getProperty("fallbackEncodings");
				if(fallbackEncodings != null && !fallbackEncodings.isEmpty())
					Collections.addAll(encodingProviders, fallbackEncodings.split("\\s+"));
			}
			else
			{
				gzipped = buffer.getBooleanProperty(Buffer.GZIPPED);
				encodingProviders.add(buffer.getStringProperty(JEditBuffer.ENCODING));
			}

			if(gzipped)
			{
				Log.log(Log.DEBUG, this, path + ": Stream is gzipped.");
				markedStream = AutoDetection.getMarkedStream(
					new GZIPInputStream(markedStream));
			}

			Collection<String> failedEncodings = new HashSet<>();
			Exception encodingError = null;
			for(Object encodingProvider: encodingProviders)
			{
				String encoding = null;
				if (encodingProvider instanceof String)
				{
					encoding = (String)encodingProvider;
				}
				else if(encodingProvider instanceof EncodingDetector)
				{
					markedStream = rewindContentsStream(markedStream, gzipped);
					encoding = ((EncodingDetector)encodingProvider).detectEncoding(new BufferedInputStream(markedStream));
				}
				else
				{
					Log.log(Log.DEBUG, this, "Strange encodingProvider: " + encodingProvider);
				}

				if(encoding == null || encoding.length() <= 0
					|| failedEncodings.contains(encoding))
				{
					continue;
				}

				markedStream = rewindContentsStream(markedStream, gzipped);
				try
				{
					read(EncodingServer.getTextReader(markedStream, encoding)
						, length, false);
					if(autodetect)
					{
						// Store the successful properties.
						if(gzipped)
						{
							buffer.setBooleanProperty(Buffer.GZIPPED,true);
						}
						buffer.setProperty(JEditBuffer.ENCODING, encoding);
					}
					return;
				}
				catch(CharConversionException | CharacterCodingException | UnsupportedEncodingException | UnsupportedCharsetException e)
				{
					encodingError = e;
				}
				Log.log(Log.NOTICE, this, path + ": " + encoding
					+ ": " + encodingError);
				failedEncodings.add(encoding);
			}
			// All possible detectors and encodings failed.
			Object[] pp = { String.join(",", failedEncodings), "" };
			if(failedEncodings.size() < 2)
			{
				pp[1] = encodingError.toString();
			}
			else
			{
				pp[1] = "See details in Activity Log";
			}
			VFSManager.error(view,path,"ioerror.encoding-error",pp,Log.NOTICE);
			markedStream = rewindContentsStream(markedStream, gzipped);
			read(EncodingServer.getEncoding(
				buffer.getStringProperty(JEditBuffer.ENCODING)
				).getPermissiveTextReader(markedStream)
				, length, false);
			if(autodetect && gzipped)
			{
				buffer.setBooleanProperty(Buffer.GZIPPED,true);
			}
		}
		finally
		{
			markedStream.close();
		}
	} //}}}

	//{{{ readMarkers() method
	private static void readMarkers(Buffer buffer, InputStream _in)
		throws IOException, InterruptedException
	{
		// For `reload' command
		buffer.removeAllMarkers();


		try (BufferedReader in = new BufferedReader(new InputStreamReader(_in)))
		{
			String line;
			while((line = in.readLine()) != null)
			{
				if(Thread.interrupted())
					throw new InterruptedException();

				// malformed marks file?
				if(line.isEmpty())
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
	} //}}}
}
