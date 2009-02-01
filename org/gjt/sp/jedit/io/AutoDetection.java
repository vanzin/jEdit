/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Kazutoshi Satoda
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

//{{{ Imports
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.bufferio.BufferIORequest;
import org.gjt.sp.util.Log;
//}}}

/**
 * Some functions for auto detection of I/O stream properties.
 */
public class AutoDetection
{
	//{{{ getMarkedStream() method
	/**
	 * Returns a marked, rewindable stream.
	 * Calling reset() method rewinds the stream to its beginning.
	 * But reset() can fail if too long bytes were read.
	 */
	public static BufferedInputStream getMarkedStream(InputStream in)
	{
		int bufferSize = BufferIORequest.getByteIOBufferSize();
		BufferedInputStream markable
			= new BufferedInputStream(in, bufferSize);
		assert(markable.markSupported());
		markable.mark(bufferSize);
		return markable;
	} //}}}

	//{{{ isGzipped() method
	/**
	 * Returns wheather the stream is gzipped.
	 * This method reads a few bytes from the sample. So a caller
	 * must take care of mark() to reuse the contents. Wraping the
	 * stream by getMarkedStream() is suitable.
	 */
	public static boolean isGzipped(InputStream sample)
		throws IOException
	{
		int magic1 = GZIPInputStream.GZIP_MAGIC & 0xff;
		int magic2 = (GZIPInputStream.GZIP_MAGIC >> 8) & 0xff;
		return sample.read() == magic1
			&& sample.read() == magic2;
	} //}}}

	//{{{ getEncodingDetectors() method
	/**
	 * Returns the user configured ordered list of encoding detectors.
	 * This method reads property "encodingDetectors".
	 */
	public static List<EncodingDetector> getEncodingDetectors()
	{
		List<EncodingDetector> detectors
			= new ArrayList<EncodingDetector>();
		String propName = "encodingDetectors";
		String selectedDetectors
			= jEdit.getProperty(propName, "BOM XML-PI");
		if (selectedDetectors != null
			&& selectedDetectors.length() > 0)
		{
			for (String name: selectedDetectors.split("\\s+"))
			{
				EncodingDetector service
					= getEncodingDetectorService(name);
				if (service != null)
				{
					detectors.add(service);
				}
				else
				{
					Log.log(Log.ERROR, AutoDetection.class
						, "getEncodingDetectors():"
							+ " No EncodingDetector for the name"
							+ " \"" + name + "\"");
				}
			}
		}
		return detectors;
	} //}}}

	//{{{ getDetectedEncoding() method
	/**
	 * Returns an auto detected encoding from content of markedStream.
	 * This method assumes that markedStream is wrapped by
	 * getMarkedStream() method.
	 */
	public static String getDetectedEncoding(BufferedInputStream markedStream)
		throws IOException
	{
		List<EncodingDetector> detectors = getEncodingDetectors();
		for (EncodingDetector detector: detectors)
		{
			// FIXME: Here the method reset() can fail if the
			// previous detector read more than buffer size of
			// markedStream.
			markedStream.reset();
			// Wrap once more so that calling mark()
			// or reset() in detectEncoding() don't
			// alter the mark position of markedStream.
			String detected = detector.detectEncoding(
				new BufferedInputStream(markedStream));
			if (detected != null)
			{
				return detected;
			}
		}
		return null;
	} //}}}

	//{{{ class Result
	/**
	 * An utility class to hold the result of some auto detections.
	 */
	public static class Result
	{
		//{{{ Constructor
		/**
		 * Do some auto detection for a stream and hold the
		 * result in this instance.
		 * @param in the stream
		 */
		public Result(InputStream in) throws IOException
		{
			BufferedInputStream marked = getMarkedStream(in);

			gzipped = isGzipped(marked);
			if (gzipped)
			{
				marked.reset();
				marked = getMarkedStream(
					new GZIPInputStream(marked));
			}

			marked.reset();
			encoding = AutoDetection.getDetectedEncoding(marked);

			markedStream = marked;
		} //}}}

		//{{{ getRewindedStream()
		/**
		 * Returns the stream which can be read the contents of
		 * the original stream.
		 * Some bytes ware read from original stream for auto
		 * detections. But they are rewinded at this method.
		 */
		public BufferedInputStream getRewindedStream()
			throws IOException
		{
			markedStream.reset();
			return markedStream;
		} //}}}

		//{{{ streamIsGzipped()
		/**
		 * Returns true if the stream is gzipped.
		 */
		public boolean streamIsGzipped()
		{
			return gzipped;
		} //}}}

		//{{{ getDetectedEncoding()
		/**
		 * Returns the auto detected encoding.
		 * Returns null if no encoding was detected.
		 */
		public String getDetectedEncoding()
		{
			return encoding;
		} //}}}

		//{{{ Private members
		private final BufferedInputStream markedStream;
		private final boolean gzipped;
		private final String encoding;
		//}}}
	} //}}}

	//{{{ Private members
	/**
	 * Returns a service of EncodingDetector for name.
	 */
	private static EncodingDetector getEncodingDetectorService(String name)
	{
		String serviceClass = "org.gjt.sp.jedit.io.EncodingDetector";
		Object service = ServiceManager.getService(serviceClass, name);
		if (service != null && service instanceof EncodingDetector)
		{
			return (EncodingDetector)service;
		}
		else
		{
			return null;
		}
	}
	//}}}
}
