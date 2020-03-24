/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2012 jEdit contributors
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

package org.jedit.io;

//{{{ Imports
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.gjt.sp.jedit.io.CharsetEncoding;
import org.gjt.sp.jedit.io.Encoding;
//}}}

/**
 * ISO-8859-1 with unicode escapes as e. g. needed for http://download.oracle.com/javase/6/docs/api/java/util/Properties.html
 *
 * @author Björn "Vampire" Kautler
 * @since 5.1pre1
 */
public class Native2ASCIIEncoding implements Encoding
{
	//{{{ implements Encoding
	@Override
	@Nonnull
	public Reader getTextReader(@Nonnull InputStream in) throws IOException
	{
		return new Native2ASCIIReader(in, false);
	}

	@Override
	@Nonnull
	public Writer getTextWriter(@Nonnull OutputStream out) throws IOException
	{
		return new FilterWriter(asciiEncoding.getTextWriter(out))
		{
			@Override
			@Nonnull
			public Writer append(@Nullable CharSequence csq) throws IOException
			{
				write((csq == null) ? "null" : csq.toString());
				return this;
			}

			@Override
			@Nonnull
			public Writer append(@Nullable CharSequence csq, int start, int end) throws IOException
			{
				CharSequence cs = (csq == null ? "null" : csq);
				write(cs.subSequence(start, end).toString());
				return this;
			}

			@Override
			@Nonnull
			public Writer append(char c) throws IOException
			{
				write(c);
				return this;
			}

			@Override
			public void write(@Nonnull String str) throws IOException
			{
				write(str, 0, str.length());
			}

			@Override
			public void write(@Nonnull char[] cbuf) throws IOException
			{
				write(cbuf, 0, cbuf.length);
			}

			@Override
			public void write(@Nonnull String str, int off, int len) throws IOException
			{
				write(str.substring(off, off + len).toCharArray());
			}

			@Override
			public void write(@Nonnull char[] cbuf, int off, int len) throws IOException
			{
				char[] buf = new char[len * 6];
				int i = 0;
				for (int j = off, j2 = off + len; j < j2; j++)
				{
					char c = cbuf[j];
					if (asciiEncoder.canEncode(c))
					{
						buf[i++] = c;
					} else
					{
						System.arraycopy(String.format("\\u%04X", (int) c).toCharArray(), 0,
								 buf, i, 6);
						i += 6;
					}
				}
				super.write(buf, 0, i);
			}

			@Override
			public void write(int c) throws IOException
			{
				if (asciiEncoder.canEncode((char) c))
				{
					super.write(c);
				} else
				{
					write(String.format("\\u%04X", c));
				}
			}
		};
	}

	@Override
	@Nonnull
	public Reader getPermissiveTextReader(@Nonnull InputStream in) throws IOException
	{
		return new Native2ASCIIReader(in, true);
	}
	//}}}

	//{{{ Package private members
	@Nonnull
	Reader getTextReader(@Nonnull InputStream in, @Nullable Class<? extends PushbackReader> clazz)
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		return new Native2ASCIIReader(in, false, clazz);
	}

	@Nonnull
	Reader getPermissiveTextReader(@Nonnull InputStream in, @Nullable Class<? extends PushbackReader> clazz)
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		return new Native2ASCIIReader(in, true, clazz);
	}
	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
	private final CharsetEncoding asciiEncoding = new CharsetEncoding(StandardCharsets.US_ASCII);
	//}}}

	private static class Native2ASCIIReader extends FilterReader
	{
		private Native2ASCIIReader(@Nonnull InputStream in, boolean permissive) throws IOException
		{
			super(new PushbackReader(iso_8859_1Encoding.getTextReader(in), 5));
			this.in = (PushbackReader) super.in;
			this.permissive = permissive;
		}

		private Native2ASCIIReader(@Nonnull InputStream in, boolean permissive,
					   @Nullable Class<? extends PushbackReader> clazz)
			throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
			       InstantiationException
		{
			super(clazz == null
			      ? new PushbackReader(iso_8859_1Encoding.getTextReader(in), 5)
			      : clazz.getConstructor(Reader.class, int.class)
				     .newInstance(iso_8859_1Encoding.getTextReader(in), 5));
			this.in = (PushbackReader) super.in;
			this.permissive = permissive;
		}

		@Override
		public int read() throws IOException
		{
			// delegate read to the ISO-8859-1
			int result = in.read();
			// does not start escape sequence or is escaped
			// (EOF - which is signalled by -1 - is also captured)
			if ((result != '\\') || escaped)
			{
				escaped = false;
				return result;
			}
			// check for following character
			int read = in.read();
			// EOF reached
			if (read == -1)
			{
				return result;
			}
			// not an escape sequence
			if (read != 'u')
			{
				escaped = true;
				in.unread(read);
				return result;
			}
			// read in remainder of possible escape sequence
			char[] escape = { 'u', '\0', '\0', '\0', '\0' };
			read = 1 + in.read(escape, 1, 4);
			// EOF reached during escape sequence
			if (read == 0)
			{
				if (permissive)
				{
					escaped = true;
					in.unread('u');
					return result;
				} else
				{
					throw new MalformedInputException(1);
				}
			}
			// read < 5 doesn't necessarily mean EOF but could also
			// mean no more input available currently, so try to read on
			while (read < 5)
			{
				int read2 = in.read(escape, read, 5 - read);
				// we have really hit EOF, so there is not
				// enough input for an escape sequence
				if (read2 == -1)
				{
					if (permissive)
					{
						escaped = true;
						in.unread(escape, 0, read);
						return result;
					} else
					{
						throw new MalformedInputException(1);
					}
				}
				read += read2;
			}
			// no unicode escape with non-hex characters in positions 3-6
			for (int i = 1; i < 5; i++)
			{
				char e = escape[i];
				if (!(((e >= '0') && (e <= '9')) || ((e >= 'a') && (e <= 'f')) || ((e >= 'A') && (e
														  <= 'F'))))
				{
					if (permissive)
					{
						escaped = true;
						in.unread(escape, 0, read);
						return result;
					} else
					{
						throw new MalformedInputException(1);
					}
				}
			}
			// valid unicode escape
			escaped = false;
			return Integer.parseInt(new String(escape, 1, 4), 16);
		}

		@Override
		public int read(CharBuffer target) throws IOException
		{
			int len = target.remaining();
			char[] cbuf = new char[len];
			int n = read(cbuf, 0, len);
			if (n > 0)
			{
				target.put(cbuf, 0, n);
			}
			return n;
		}

		@Override
		public int read(char[] cbuf) throws IOException
		{
			return read(cbuf, 0, cbuf.length);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException
		{
			return readn(cbuf, off, len);
		}

		private int readn(char[] cbuf, int off, int len) throws IOException
		{
			// read 5 chars more than requested to have
			// more input if last character is a '\'
			int bufLen = len + 5;
			char[] buf = new char[bufLen];
			// delegate read to the ISO-8859-1
			int read = in.read(buf);
			// EOF reached
			if (read == -1)
			{
				return read;
			}
			// read = read from underlying stream
			// result = read after conversion
			int result = 0;
			// how many additional characters need to be read
			// because of collapsed escape sequences
			int needed = 0;
			// iterate read chars but maximum len ones
			int i;
outer:
			for (i = 0; (i < read) && (i < len); i++)
			{
				// character under consideration
				char c = buf[i];
				// does not start escape sequence
				if ((c != '\\') || escaped)
				{
					// add to result buffer and
					// continue to next character
					escaped = false;
					cbuf[off + result++] = c;
					continue;
				}
				// less than 5 characters left after current
				// position because either there was no more
				// input available or because of EOF
				if (read - i - 1 < 5)
				{
					// try to read in more characters to
					// complete the escape sequence
					while (read < i + 1 + 5)
					{
						// read in missing characters
						int read2 = in.read(buf, read, i + 1 + 5 - read);
						// EOF reached
						if (read2 == -1)
						{
							// add to result buffer and continue to next character if
							// - permissive or
							// - EOF reached after backslash or
							// - not an escape sequence,
							// otherwise throw exception
							if (permissive || (read - i - 1 == 0) || (buf[i + 1] != 'u'))
							{
								escaped = true;
								cbuf[off + result++] = c;
								continue outer;
							} else
							{
								throw new MalformedInputException(1);
							}
						}
						read += read2;
					}
				}
				// no unicode escape without 'u' at second position
				if (buf[i + 1] != 'u')
				{
					// add to result buffer and
					// continue to next character
					escaped = true;
					cbuf[off + result++] = c;
					continue;
				}
				// no unicode escape with non-hex characters in positions 3-6
				for (int j = i + 2, j2 = i + 6; j < j2; j++)
				{
					char e = buf[j];
					if (!(((e >= '0') && (e <= '9')) || ((e >= 'a') && (e <= 'f')) || ((e >= 'A')
													   && (e
													       <= 'F'))))
					{
						// add to result buffer and continue to next character
						// if permissive, otherwise throw exception
						if (permissive)
						{
							escaped = true;
							cbuf[off + result++] = c;
							continue outer;
						} else
						{
							throw new MalformedInputException(1);
						}
					}
				}
				// valid unicode escape
				escaped = false;
				cbuf[off + result++] = (char) Integer.parseInt(new String(buf, i + 2, 4), 16);
				// need 5 more chars that were consumed for escape collapsing,
				// but only if the escape sequence was not in the additional space
				needed += Math.min(len - i - 1, 5);
				// advance pointer
				i += 5;
			}
			in.unread(buf, i, read - i);
			// nothing was collapsed
			if (needed == 0)
			{
				return result;
			}
			// read more chars due to escape collapsing
			read = readn(cbuf, off + result, needed);
			// EOF reached
			if (read == -1)
			{
				return result;
			}
			return result + read;
		}

		@Override
		public long skip(long toSkip) throws IOException
		{
			if (toSkip < 0)
			{
				throw new IllegalArgumentException("skip value is negative");
			}
			int skipBufferSize = (int) Math.min(toSkip, MAX_SKIP_BUFFER_SIZE);
			if ((skipBuffer == null) || (skipBuffer.length < skipBufferSize))
			{
				skipBuffer = new char[skipBufferSize];
			}
			long remaining = toSkip;
			synchronized (in)
			{
				while (remaining > 0)
				{
					int skipped = read(skipBuffer, 0, (int) Math.min(remaining, skipBufferSize));
					if (skipped == -1)
					{
						break;
					}
					remaining -= skipped;
				}
			}
			return toSkip - remaining;
		}

		//{{{ Private members

		private static final int MAX_SKIP_BUFFER_SIZE = 8192;
		private static final Encoding iso_8859_1Encoding = new CharsetEncoding(StandardCharsets.ISO_8859_1);

		//{{{ Instance variables
		private final PushbackReader in;
		private final boolean permissive;
		private char[] skipBuffer;
		private boolean escaped;
		//}}}

		//}}}
	}

	//}}}
}
