/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Kazutoshi Satoda
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

package org.gjt.sp.jedit.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.CharBuffer;

/**
 * An encoding detector which finds regex pattern.
 *
 * This reads the sample in the system default encoding for first some
 * lines and look for a regex pattern. This can fail if the
 * stream cannot be read in the system default encoding or the
 * pattern is not found at near the top of the stream.
 *
 * @since 4.3pre16
 * @author Kazutoshi Satoda
 */
public class RegexEncodingDetector implements EncodingDetector
{
	/**
	 * A regex pattern matches to "Charset names" specified for
	 * java.nio.charset.Charset.
	 * @see <a href="http://download.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html#names">Charset names</a>
	 */
	public static final String VALID_ENCODING_PATTERN
		= "\\p{Alnum}[\\p{Alnum}\\-.:_]*";

	private final Pattern pattern;
	private final String replacement;

	public RegexEncodingDetector(String pattern, String replacement)
	{
		this.pattern = Pattern.compile(pattern);
		this.replacement = replacement;
	}

	public String detectEncoding(InputStream sample) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(sample);
		final int bufferSize = 1024;
		char[] buffer = new char[bufferSize];
		int readSize = reader.read(buffer, 0, bufferSize);
		if (readSize > 0)
		{
			Matcher matcher = pattern.matcher(
				CharBuffer.wrap(buffer, 0, readSize));

			// Tracking of this implicit state within Matcher
			// is required to know where is the start of
			// replacement after calling appendReplacement().
			int appendPosition = 0;

			while (matcher.find())
			{
				String extracted = extractReplacement(
					matcher, appendPosition, replacement);
				if (EncodingServer.hasEncoding(extracted))
				{
					return extracted;
				}
				appendPosition = matcher.end();
			}
		}
		return null;
	}

	/**
	 * Returns a replaced string for a Matcher which has been matched
	 * by find() method.
	 */
	private static String extractReplacement(
		Matcher found, int appendPosition, String replacement)
	{
		/*
		 * It doesn't make sense to read before start, but
		 * appendReplacement() requires to to it.
		 */
		int found_start = found.start();
		int found_end = found.end();
		int source_length = found_end - found_start;
		int length_before_match = found_start - appendPosition;
		StringBuffer replaced = new StringBuffer(
				length_before_match + (source_length * 2));
		found.appendReplacement(replaced, replacement);
		return replaced.substring(length_before_match);
	}
}
