/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Kazutoshi Satoda
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

//{{{ Imports
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
//}}}

/**
 * Encodings which are provided by java.nio.charset.Charset.
 *
 * @since 4.3pre10
 * @author Kazutoshi Satoda
 */
public class CharsetEncoding implements Encoding
{
	//{{{ Constructor
	public CharsetEncoding(String name)
	{
		body = Charset.forName(name);
	} //}}}

	//{{{ implements Encoding
	public Reader getTextReader(InputStream in) throws IOException
	{
		// Pass the decoder explicitly to report a decode error
		// as an exception instead of replacing with "\uFFFD".
		// The form "InputStreamReader(in, encoding)" seemed to use
		// CodingErrorAction.REPLACE internally.
		return new InputStreamReader(in, body.newDecoder());
	}

	public Writer getTextWriter(OutputStream out) throws IOException
	{
		// Pass the encoder explicitly because of same reason
		// in getTextReader();
		return new OutputStreamWriter(out, body.newEncoder());
	}

	public Reader getPermissiveTextReader(InputStream in) throws IOException
	{
		// Use REPLACE action to indicate where the coding error
		// happened by the replacement character "\uFFFD".
		CharsetDecoder permissive = body.newDecoder();
		permissive.onMalformedInput(CodingErrorAction.REPLACE);
		permissive.onUnmappableCharacter(CodingErrorAction.REPLACE);
		return new InputStreamReader(in, permissive);
	}
	//}}}

	//{{{ Private members
	private final Charset body;
	//}}}
}
