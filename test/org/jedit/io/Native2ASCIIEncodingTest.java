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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.gjt.sp.util.IOUtilities.closeQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

public class Native2ASCIIEncodingTest
{
	@BeforeClass
	public static void oneTimeSetUp()
	{
		native2ASCIIEncoding = new Native2ASCIIEncoding();
		iso_8859_1 = Charset.forName("ISO-8859-1");
		bufferArray = new char[1024];
		buffer = CharBuffer.wrap(bufferArray);
	}

	@AfterClass
	public static void oneTimeTearDown()
	{
		native2ASCIIEncoding = null;
		iso_8859_1 = null;
		bufferArray = null;
		buffer = null;
	}

	@Before
	public void setUp()
	{
		buffer.clear();
	}

	@After
	public void tearDown()
	{
		closeQuietly((Closeable)reader);
		closeQuietly((Closeable)writer);
	}

	private Reader getReader(String input) throws IOException
	{
		InputStream inputStream = new ByteArrayInputStream(input.getBytes(iso_8859_1));
		reader = native2ASCIIEncoding.getTextReader(inputStream);
		return reader;
	}

	private Reader getThrottledReader(String input)
		throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
		       InstantiationException
	{
		InputStream inputStream = new ByteArrayInputStream(input.getBytes(iso_8859_1));
		reader = native2ASCIIEncoding.getTextReader(inputStream, ThrottledPushbackReader.class);
		return reader;
	}

	private Reader getPermissiveReader(String input) throws IOException
	{
		InputStream inputStream = new ByteArrayInputStream(input.getBytes(iso_8859_1));
		reader = native2ASCIIEncoding.getPermissiveTextReader(inputStream);
		return reader;
	}

	private Reader getThrottledPermissiveReader(String input)
		throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
		       InstantiationException
	{
		InputStream inputStream = new ByteArrayInputStream(input.getBytes(iso_8859_1));
		reader = native2ASCIIEncoding.getPermissiveTextReader(inputStream, ThrottledPushbackReader.class);
		return reader;
	}

	@Test
	public void readShouldConvertEscapeSequence() throws IOException
	{
		int c = getReader("\\u21aF").read();
		assertThat((char) c, is(equalTo('\u21aF')));
	}

	@Test
	public void read_charArray_ShouldConvertEscapeSequence() throws IOException
	{
		int c = getReader("\\u21aF").read(bufferArray);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\u21aF')));
	}

	@Test
	public void read_charArray_int_int_ShouldConvertEscapeSequence() throws IOException
	{
		int c = getReader("\\u21aF").read(bufferArray, 0, 1);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\u21aF')));
	}

	@Test
	public void read_CharBuffer_ShouldConvertEscapeSequence() throws IOException
	{
		int c = getReader("\\u21aF").read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(1)));
		assertThat(buffer.length(), is(equalTo(1)));
		assertThat(buffer.toString(), is(equalTo("\u21aF")));
	}

	@Test(expected = MalformedInputException.class)
	public void readShouldThrowExceptionOnIncompleteEscapeSequence() throws IOException
	{
		getReader("\\u21a").read();
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_ShouldThrowExceptionOnIncompleteEscapeSequence() throws IOException
	{
		getReader("\\u21a").read(bufferArray);
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_int_int_ShouldThrowExceptionOnIncompleteEscapeSequence() throws IOException
	{
		getReader("\\u21a").read(bufferArray, 0, 1);
	}

	@Test(expected = MalformedInputException.class)
	public void read_CharBuffer_ShouldThrowExceptionOnIncompleteEscapeSequence() throws IOException
	{
		getReader("\\u21a").read(buffer);
	}

	@Test(expected = MalformedInputException.class)
	public void readShouldThrowExceptionOnMissingInputAfterU() throws IOException
	{
		getReader("\\u").read();
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_ShouldThrowExceptionOnMissingInputAfterU() throws IOException
	{
		getReader("\\u").read(bufferArray);
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_int_int_ShouldThrowExceptionOnMissingInputAfterU() throws IOException
	{
		getReader("\\u").read(bufferArray, 0, 1);
	}

	@Test(expected = MalformedInputException.class)
	public void read_CharBuffer_ShouldThrowExceptionOnMissingInputAfterU() throws IOException
	{
		getReader("\\u").read(buffer);
	}

	@Test(expected = MalformedInputException.class)
	public void readShouldThrowExceptionOnMalformedInput() throws IOException
	{
		Reader reader = getReader("asdf\\: \\u21alasdf");
		while (reader.read() != -1)
		{
		}
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_ShouldThrowExceptionOnMalformedInput() throws IOException
	{
		getReader("asdf\\: \\u21alasdf").read(bufferArray);
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_int_int_ShouldThrowExceptionOnMalformedInput() throws IOException
	{
		getReader("asdf\\: \\u21alasdf").read(bufferArray, 0, 15);
	}

	@Test(expected = MalformedInputException.class)
	public void read_CharBuffer_ShouldThrowExceptionOnMalformedInput() throws IOException
	{
		getReader("asdf\\: \\u21alasdf").read(buffer);
	}

	@Test
	public void permissiveReadShouldAcceptIncompleteEscapeSequence() throws IOException
	{
		Reader reader = getPermissiveReader("\\u21a");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void permissiveRead_charArray_ShouldAcceptIncompleteEscapeSequence() throws IOException
	{
		Reader reader = getPermissiveReader("\\u21a");
		int c = reader.read(bufferArray);
		assertThat(c, is(equalTo(5)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
	}

	@Test
	public void permissiveRead_charArray_int_int_ShouldAcceptIncompleteEscapeSequence() throws IOException
	{
		Reader reader = getPermissiveReader("\\u21a");
		int c = reader.read(bufferArray, 0, 5);
		assertThat(c, is(equalTo(5)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
	}

	@Test
	public void permissiveRead_CharBuffer_ShouldAcceptIncompleteEscapeSequence() throws IOException
	{
		String input = "\\u21a";
		Reader reader = getPermissiveReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(5)));
		assertThat(buffer.length(), is(equalTo(5)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void permissiveReadShouldAcceptMissingInputAfterU() throws IOException
	{
		Reader reader = getPermissiveReader("\\u");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void permissiveRead_charArray_ShouldAcceptMissingInputAfterU() throws IOException
	{
		Reader reader = getPermissiveReader("\\u");
		int c = reader.read(bufferArray);
		assertThat(c, is(equalTo(2)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
	}

	@Test
	public void permissiveRead_charArray_int_int_ShouldAcceptMissingInputAfterU() throws IOException
	{
		Reader reader = getPermissiveReader("\\u");
		int c = reader.read(bufferArray, 0, 2);
		assertThat(c, is(equalTo(2)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
	}

	@Test
	public void permissiveRead_CharBuffer_ShouldAcceptMissingInputAfterU() throws IOException
	{
		String input = "\\u";
		Reader reader = getPermissiveReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(2)));
		assertThat(buffer.length(), is(equalTo(2)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void permissiveReadShouldAcceptMalformedInput() throws IOException
	{
		Reader reader = getPermissiveReader("asdf\\: \\u21a/asdf");
		int c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));
		c = reader.read();
		assertThat((char) c, is(equalTo('f')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo(':')));
		c = reader.read();
		assertThat((char) c, is(equalTo(' ')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('/')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));
		c = reader.read();
		assertThat((char) c, is(equalTo('f')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void permissiveRead_charArray_ShouldAcceptMalformedInput() throws IOException
	{
		int c = getPermissiveReader("asdf\\: \\u21a/asdf").read(bufferArray);
		assertThat(c, is(equalTo(17)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('/')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void permissiveRead_charArray_int_int_ShouldAcceptMalformedInput() throws IOException
	{
		int c = getPermissiveReader("asdf\\: \\u21a/asdf").read(bufferArray, 0, 17);
		assertThat(c, is(equalTo(17)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('/')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void permissiveRead_CharBuffer_ShouldAcceptMalformedInput() throws IOException
	{
		String input = "asdf\\: \\u21a/asdf";
		Reader reader = getPermissiveReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(17)));
		assertThat(buffer.length(), is(equalTo(17)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void readShouldCorrectlyHandleEOF() throws IOException
	{
		int c = getReader("").read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldCorrectlyHandleEOF() throws IOException
	{
		int c = getReader("").read(bufferArray);
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_int_int_ShouldCorrectlyHandleEOF() throws IOException
	{
		int c = getReader("").read(bufferArray, 0, 1);
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_CharBuffer_ShouldCorrectlyHandleEOF() throws IOException
	{
		int c = getReader("").read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(-1)));
		assertThat(buffer.length(), is(equalTo(0)));
	}

	@Test
	public void readShouldCorrectlyHandleEOFAfterBackslash() throws IOException
	{
		Reader reader = getReader("\\");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldCorrectlyHandleEOFAfterBackslash() throws IOException
	{
		int c = getReader("\\").read(bufferArray);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\\')));
	}

	@Test
	public void read_charArray_int_int_ShouldCorrectlyHandleEOFAfterBackslash() throws IOException
	{
		int c = getReader("\\").read(bufferArray, 0, 1);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\\')));
	}

	@Test
	public void read_CharBuffer_ShouldCorrectlyHandleEOFAfterBackslash() throws IOException
	{
		String input = "\\";
		int c = getReader(input).read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(1)));
		assertThat(buffer.length(), is(equalTo(1)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void readShouldCorrectlyHandleLessThan5NonEscapeCharactersAfterBackslash() throws IOException
	{
		Reader reader = getReader("\\asdf");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));
		c = reader.read();
		assertThat((char) c, is(equalTo('f')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldCorrectlyHandleLessThan5NonEscapeCharactersAfterBackslash() throws IOException
	{
		int c = getReader("\\asdf").read(bufferArray);
		assertThat(c, is(equalTo(5)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_charArray_int_int_ShouldCorrectlyHandleLessThan5NonEscapeCharactersAfterBackslash()
		throws IOException
	{
		int c = getReader("\\asdf").read(bufferArray, 0, 5);
		assertThat(c, is(equalTo(5)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_CharBuffer_ShouldCorrectlyHandleLessThan5NonEscapeCharactersAfterBackslash() throws IOException
	{
		String input = "\\asdf";
		int c = getReader(input).read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(5)));
		assertThat(buffer.length(), is(equalTo(5)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test(expected = MalformedInputException.class)
	public void readShouldThrowExceptionOnMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		getThrottledReader("\\u21aL").read();
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_ShouldThrowExceptionOnMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		getThrottledReader("\\u21aL").read(bufferArray);
	}

	@Test(expected = MalformedInputException.class)
	public void read_charArray_int_int_ShouldThrowExceptionOnMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		getThrottledReader("\\u21aL").read(bufferArray, 0, 1);
	}

	@Test(expected = MalformedInputException.class)
	public void read_CharBuffer_ShouldThrowExceptionOnMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		getThrottledReader("\\u21aL").read(buffer);
	}

	@Test
	public void readShouldConvertEscapeSequenceWithThrottledInputStream()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledReader("\\u21aF").read();
		assertThat((char) c, is(equalTo('\u21aF')));
	}

	@Test
	public void read_charArray_ShouldConvertEscapeSequenceWithThrottledInputStream()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledReader("\\u21aF").read(bufferArray);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\u21aF')));
	}

	@Test
	public void read_charArray_int_int_ShouldConvertEscapeSequenceWithThrottledInputStream()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledReader("\\u21aF").read(bufferArray, 0, 1);
		assertThat(c, is(equalTo(1)));
		assertThat(bufferArray[0], is(equalTo('\u21aF')));
	}

	@Test
	public void read_CharBuffer_ShouldConvertEscapeSequenceWithThrottledInputStream()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledReader("\\u21aF").read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(1)));
		assertThat(buffer.toString(), is(equalTo("\u21aF")));
	}

	@Test
	public void readShouldReadBackslashWithoutFollowingUAsBackslash() throws IOException
	{
		Reader reader = getReader("\\nu21aF");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('n')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('F')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldReadBackslashWithoutFollowingUAsBackslash() throws IOException
	{
		int c = getReader("\\nu21aF").read(bufferArray);
		assertThat(c, is(equalTo(7)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('n')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_charArray_int_int_ShouldReadBackslashWithoutFollowingUAsBackslash() throws IOException
	{
		int c = getReader("\\nu21aF").read(bufferArray, 0, 7);
		assertThat(c, is(equalTo(7)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('n')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_CharBuffer_ShouldReadBackslashWithoutFollowingUAsBackslash() throws IOException
	{
		String input = "\\nu21aF";
		Reader reader = getReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(7)));
		assertThat(buffer.length(), is(equalTo(7)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void permissiveReadShouldAcceptMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		Reader reader = getThrottledPermissiveReader("\\u21a;");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo(';')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void permissiveRead_charArray_ShouldAcceptMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledPermissiveReader("\\u21a;").read(bufferArray);
		assertThat(c, is(equalTo(6)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo(';')));
	}

	@Test
	public void permissiveRead_charArray_int_int_ShouldAcceptMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		int c = getThrottledPermissiveReader("\\u21a;").read(bufferArray, 0, 6);
		assertThat(c, is(equalTo(6)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo(';')));
	}

	@Test
	public void permissiveRead_CharBuffer_ShouldAcceptMalformedInputWithThrottledReader()
		throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
		       IllegalAccessException
	{
		String input = "\\u21a;";
		Reader reader = getThrottledPermissiveReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(6)));
		assertThat(buffer.length(), is(equalTo(6)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void read_charArray_shouldReadOnAfterCollapsingEscapeSequences() throws IOException
	{
		int c = getReader("asdf\\: \\u21aFasdf").read(bufferArray);
		assertThat(c, is(equalTo(12)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\u21aF')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_charArray_int_int_shouldReadOnAfterCollapsingEscapeSequences() throws IOException
	{
		int c = getReader("asdf\\: \\u21aFasdf").read(bufferArray, 0, 12);
		assertThat(c, is(equalTo(12)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\u21aF')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_CharBuffer_shouldReadOnAfterCollapsingEscapeSequences() throws IOException
	{
		int c = getReader("asdf\\: \\u21aFasdf").read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(12)));
		assertThat(buffer.length(), is(equalTo(12)));
		assertThat(buffer.toString(), is(equalTo("asdf\\: \u21aFasdf")));
	}

	@Test
	public void readShouldIgnoreEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		Reader reader = getReader("\\\\u21aF");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('F')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldIgnoreEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		int c = getReader("\\\\u21aF").read(bufferArray);
		assertThat(c, is(equalTo(7)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_charArray_int_int_ShouldIgnoreEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		int c = getReader("\\\\u21aF").read(bufferArray, 0, 7);
		assertThat(c, is(equalTo(7)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_CharBuffer_ShouldIgnoreEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		String input = "\\\\u21aF";
		Reader reader = getReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(7)));
		assertThat(buffer.length(), is(equalTo(7)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void readShouldConvertEscapeSequenceThatFollowsTwoBackslashes() throws IOException
	{
		Reader reader = getReader("\\\\\\u21aF");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\u21aF')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldConvertEscapeSequenceThatFollowsTwoBackslashes() throws IOException
	{
		int c = getReader("\\\\\\u21aF").read(bufferArray);
		assertThat(c, is(equalTo(3)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\u21aF')));
	}

	@Test
	public void read_charArray_int_int_ShouldConvertEscapeSequenceThatFollowsTwoBackslashes() throws IOException
	{
		int c = getReader("\\\\\\u21aF").read(bufferArray, 0, 3);
		assertThat(c, is(equalTo(3)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\u21af')));
	}

	@Test
	public void read_CharBuffer_ShouldConvertEscapeSequenceThatFollowsTwoBackslashes() throws IOException
	{
		Reader reader = getReader("\\\\\\u21aF");
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(3)));
		assertThat(buffer.length(), is(equalTo(3)));
		assertThat(buffer.toString(), is(equalTo("\\\\\u21aF")));
	}

	@Test
	public void differentReadMethodsShouldBeUsableOnTheSameStream() throws IOException
	{
		Reader reader = getReader("asdf\\: \\u21aFasdf");
		int c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));

		c = reader.read(bufferArray, 0, 3);
		assertThat(c, is(equalTo(3)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));

		c = reader.read();
		assertThat((char) c, is(equalTo(' ')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\u21aF')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));

		c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(3)));
		assertThat(buffer.length(), is(equalTo(3)));
		assertThat(buffer.toString(), is(equalTo("sdf")));
	}

	@Test
	public void readShouldIgnoreEscapeSequenceThatFollowsThreeBackslashes() throws IOException
	{
		Reader reader = getReader("\\\\\\\\u21aF");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('F')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldIgnoreEscapeSequenceThatFollowsThreeBackslashes() throws IOException
	{
		int c = getReader("\\\\\\\\u21aF").read(bufferArray);
		assertThat(c, is(equalTo(9)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_charArray_int_int_ShouldIgnoreEscapeSequenceThatFollowsThreeBackslashes() throws IOException
	{
		int c = getReader("\\\\\\\\u21aF").read(bufferArray, 0, 9);
		assertThat(c, is(equalTo(9)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('F')));
	}

	@Test
	public void read_CharBuffer_ShouldIgnoreEscapeSequenceThatFollowsThreeBackslashes() throws IOException
	{
		String input = "\\\\\\\\u21aF";
		Reader reader = getReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(9)));
		assertThat(buffer.length(), is(equalTo(9)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void readShouldIgnoreIncompleteEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		Reader reader = getReader("\\\\u21a");
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldIgnoreIncompleteEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		int c = getReader("\\\\u21a").read(bufferArray);
		assertThat(c, is(equalTo(6)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
	}

	@Test
	public void read_charArray_int_int_ShouldIgnoreIncompleteEscapeSequenceThatFollowsOneBackslash()
		throws IOException
	{
		int c = getReader("\\\\u21a").read(bufferArray, 0, 6);
		assertThat(c, is(equalTo(6)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
	}

	@Test
	public void read_CharBuffer_ShouldIgnoreIncompleteEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		String input = "\\\\u21a";
		Reader reader = getReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(6)));
		assertThat(buffer.length(), is(equalTo(6)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void readShouldIgnoreMalformedEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		Reader reader = getReader("asdf\\: \\\\u21alasdf");
		int c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));
		c = reader.read();
		assertThat((char) c, is(equalTo('f')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo(':')));
		c = reader.read();
		assertThat((char) c, is(equalTo(' ')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('l')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));
		c = reader.read();
		assertThat((char) c, is(equalTo('s')));
		c = reader.read();
		assertThat((char) c, is(equalTo('d')));
		c = reader.read();
		assertThat((char) c, is(equalTo('f')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void read_charArray_ShouldIgnoreMalformedEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		int c = getReader("asdf\\: \\\\u21alasdf").read(bufferArray);
		assertThat(c, is(equalTo(18)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('l')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_charArray_int_int_ShouldIgnoreMalformedEscapeSequenceThatFollowsOneBackslash()
		throws IOException
	{
		int c = getReader("asdf\\: \\\\u21alasdf").read(bufferArray, 0, 18);
		assertThat(c, is(equalTo(18)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo('u')));
		assertThat(bufferArray[i++], is(equalTo('2')));
		assertThat(bufferArray[i++], is(equalTo('1')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('l')));
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
	}

	@Test
	public void read_CharBuffer_ShouldIgnoreMalformedEscapeSequenceThatFollowsOneBackslash() throws IOException
	{
		String input = "asdf\\: \\\\u21alasdf";
		Reader reader = getReader(input);
		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(18)));
		assertThat(buffer.length(), is(equalTo(18)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void differentReadMethodsShouldHaveACommonEscapeSequenceHandling() throws IOException
	{
		Reader reader = getReader("asdf\\: \\\\u21aFasdf");
		int c = reader.read(bufferArray, 0, 8);
		assertThat(c, is(equalTo(8)));
		int i = 0;
		assertThat(bufferArray[i++], is(equalTo('a')));
		assertThat(bufferArray[i++], is(equalTo('s')));
		assertThat(bufferArray[i++], is(equalTo('d')));
		assertThat(bufferArray[i++], is(equalTo('f')));
		assertThat(bufferArray[i++], is(equalTo('\\')));
		assertThat(bufferArray[i++], is(equalTo(':')));
		assertThat(bufferArray[i++], is(equalTo(' ')));
		assertThat(bufferArray[i++], is(equalTo('\\')));

		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('a')));

		c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(5)));
		assertThat(buffer.length(), is(equalTo(5)));
		assertThat(buffer.toString(), is(equalTo("Fasdf")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void skipShouldThrowExceptionIfToSkipIsNegative() throws IOException
	{
		getReader("asdf\\: \\\\u21alasdf").skip(-1);
	}

	@Test
	public void skipShouldNotDoAnythingIfToSkipIsZero() throws IOException
	{
		String input = "asdf\\: \\\\u21alasdf";
		Reader reader = getReader(input);
		long skipped = reader.skip(0);
		assertThat(skipped, is(equalTo(0L)));

		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(18)));
		assertThat(buffer.length(), is(equalTo(18)));
		assertThat(buffer.toString(), is(equalTo(input)));
	}

	@Test
	public void skipShouldSkipGivenAmountIfAvailable() throws IOException
	{
		Reader reader = getReader("asdf\\: \\\\u21alasdf");
		long skipped = reader.skip(8);
		assertThat(skipped, is(equalTo(8L)));

		int c = reader.read(buffer);
		buffer.flip();
		assertThat(c, is(equalTo(10)));
		assertThat(buffer.length(), is(equalTo(10)));
		assertThat(buffer.toString(), is(equalTo("\\u21alasdf")));
	}

	@Test
	public void skipShouldSkipAllIfToSkipIsGreaterThanInputLength() throws IOException
	{
		Reader reader = getReader("asdf\\: \\\\u21alasdf");
		long skipped = reader.skip(20);
		assertThat(skipped, is(equalTo(18L)));

		int c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void multipleSkipCallsShouldWork() throws IOException
	{
		Reader reader = getReader("asdf\\: \\\\u21alasdf");
		long skipped = reader.skip(5);
		assertThat(skipped, is(equalTo(5L)));
		skipped = reader.skip(10);
		assertThat(skipped, is(equalTo(10L)));
		skipped = reader.skip(5);
		assertThat(skipped, is(equalTo(3L)));

		int c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void textReaderWithNullAsClassParameterShouldWork()
		throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
		       InstantiationException
	{
		InputStream inputStream = new ByteArrayInputStream("asdf\\: \\\\u21alasdf".getBytes(iso_8859_1));
		reader = native2ASCIIEncoding.getTextReader(inputStream, null);
		long skipped = reader.skip(18);
		assertThat(skipped, is(equalTo(18L)));

		int c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void readerShouldBeAbleToDecodeWhatWriterHasEncoded() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write('\u21AF');
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		reader = native2ASCIIEncoding.getTextReader(bais);
		int c = reader.read();
		assertThat((char) c, is(equalTo('\u21AF')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void readerShouldReadSingleCharactersIfWriterAddedAnEscapingBackslash() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write('\\');
		writer.write('\u21AF');
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\\\u21AF")));
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		reader = native2ASCIIEncoding.getTextReader(bais);
		int c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('\\')));
		c = reader.read();
		assertThat((char) c, is(equalTo('u')));
		c = reader.read();
		assertThat((char) c, is(equalTo('2')));
		c = reader.read();
		assertThat((char) c, is(equalTo('1')));
		c = reader.read();
		assertThat((char) c, is(equalTo('A')));
		c = reader.read();
		assertThat((char) c, is(equalTo('F')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void readerShouldDecodeIfWriterHasWrittenEscapeSequenceAsSingleCharacters() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write('\\');
		writer.write('u');
		writer.write('2');
		writer.write('1');
		writer.write('a');
		writer.write('F');
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		reader = native2ASCIIEncoding.getTextReader(bais);
		int c = reader.read();
		assertThat((char) c, is(equalTo('\u21aF')));
		c = reader.read();
		assertThat(c, is(equalTo(-1)));
	}

	@Test
	public void write_int_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write('\\');
		writer.write('u');
		writer.write('2');
		writer.write('1');
		writer.write('a');
		writer.write('F');
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void write_charArray_int_int_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write(new char[] { '\\', 'u', '2', '1', 'a', 'F' }, 0, 6);
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void write_String_int_int_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write("\\u21aF", 0, 6);
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void write_charArray_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write(new char[] { '\\', 'u', '2', '1', 'a', 'F' });
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void write_String_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write("\\u21aF");
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void append_char_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append('\\').append('u').append('2').append('1').append('a').append('F');
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void append_CharSequence_int_int_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append("\\u21aF", 0, 6);
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void append_CharSequence_ShouldEncodeASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append("\\u21aF");
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21aF")));
	}

	@Test
	public void write_int_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write('\u21AF');
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void write_charArray_int_int_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write(new char[] { '\u21AF' }, 0, 1);
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void write_String_int_int_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write("\u21AF", 0, 1);
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void write_charArray_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write(new char[] { '\u21AF' });
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void write_String_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		writer.write("\u21AF");
		writer.flush();
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void append_char_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append('\u21AF');
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void append_CharSequence_int_int_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append("\u21AF", 0, 1);
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void append_CharSequence_ShouldEncodeNonASCIICharactersCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append("\u21AF");
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("\\u21AF")));
	}

	@Test
	public void append_CharSequence_int_int_ShouldEncodeNullCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append(null, 0, 4);
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("null")));
	}

	@Test
	public void append_CharSequence_ShouldEncodeNullCorrectly() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = native2ASCIIEncoding.getTextWriter(baos);
		Writer returnedWriter = writer.append(null);
		writer.flush();
		assertThat(returnedWriter, is(sameInstance(writer)));
		assertThat(baos.toString("ISO-8859-1"), is(equalTo("null")));
	}

	private Reader reader;
	private Writer writer;

	private static Native2ASCIIEncoding native2ASCIIEncoding;
	private static Charset iso_8859_1;
	private static char[] bufferArray;
	private static CharBuffer buffer;

	private static class ThrottledPushbackReader extends PushbackReader
	{
		public ThrottledPushbackReader(Reader in, int size)
		{
			super(in, size);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException
		{
			if (cbuf == null)
			{
				throw new NullPointerException();
			} else if (off < 0 || len < 0 || len > cbuf.length - off)
			{
				throw new IndexOutOfBoundsException();
			}
			int readChar = read();
			if (readChar == -1)
			{
				return -1;
			}
			if (len == 0)
			{
				return 0;
			}
			cbuf[off] = (char) readChar;
			return 1;
		}
	}
}
