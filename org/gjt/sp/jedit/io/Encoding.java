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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

/**
 * An interface to represent an encoding.
 * An encoding is a mapping between a character stream and a byte
 * stream. It is like java.nio.charset.Charset but has slightly
 * different form. This can represents some extended encodings like
 * UTF-8Y which drops (inserts) the BOM bytes before actual decoding
 * (encoding). This also enables to add some extended encodings such
 * as ASCII representation used by Java property files.
 *
 * @since 4.3pre10
 * @author Kazutoshi Satoda
 */
public interface Encoding
{
	/**
	* Map an InputStream to a Reader.
	* Decode-error while reading from this Reader should be reported
	* by throwing an IOException.
	*/
	public Reader getTextReader(InputStream in) throws IOException;
	
	/**
	* Map an OutputStream to a Writer.
	* Encode-error while writing to this Writer should be reported
	* by throwing an IOException.
	*/
	public Writer getTextWriter(OutputStream out) throws IOException;

	/**
	* Map an InputStream to a Reader.
	* Decode-error while reading from this Reader should be ignored
	* or replaced.
	*/
	public Reader getPermissiveTextReader(InputStream in)
		throws IOException;
}
