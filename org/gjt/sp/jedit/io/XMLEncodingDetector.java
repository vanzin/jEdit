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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import org.gjt.sp.util.Log;
//}}}

/**
 * An encoding detector which extracts encoding from XML declaration.
 *
 * @since 4.3pre10
 * @author Kazutoshi Satoda
 */
public class XMLEncodingDetector implements EncodingDetector
{
	//{{{ implements EncodingDetector
	public String detectEncoding(InputStream sample) throws IOException
	{
		// Length of longest XML PI used for encoding detection.
		// <?xml version="1.0" encoding="................"?>
		final int XML_PI_LENGTH = 50;
		
		byte[] _xmlPI = new byte[XML_PI_LENGTH];
		int offset = 0;
		int count;
		while((count = sample.read(_xmlPI,offset,
			XML_PI_LENGTH - offset)) != -1)
		{
			offset += count;
			if(offset == XML_PI_LENGTH)
				break;
		}
		return getXMLEncoding(new String(_xmlPI,0,offset,"ASCII"));
	} //}}}

	//{{{ Private members
	/**
	 * Extract XML encoding name from PI.
	 */
	private static String getXMLEncoding(String xmlPI)
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

		try
		{
			if(Charset.isSupported(encoding))
			{
				return encoding;
			}
			else
			{
				Log.log(Log.WARNING, XMLEncodingDetector.class,
					"XML PI specifies unsupported encoding: " + encoding);
			}
		}
		catch(IllegalCharsetNameException e)
		{
			Log.log(Log.WARNING, XMLEncodingDetector.class,
				"XML PI specifies illegal encoding: " + encoding,
				e);
		}
		return null;
	}
	//}}}
}
