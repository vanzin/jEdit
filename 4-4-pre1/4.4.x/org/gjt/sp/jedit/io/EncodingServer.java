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
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.ServiceManager;
//}}}

/**
 * A class for some static methods to deal with encodings.
 *
 * @since 4.3pre10
 * @author Kazutoshi Satoda
 */
public class EncodingServer
{
	//{{{ getEncoding() method
	/**
	 * Returns an instance of Encoding for specified name.
	 * The name is used for search the following domains in the
	 * listed order.
	 *   - java.nio.charset.Charset
	 *   - jEdit ServiceManager
	 */
	public static Encoding getEncoding(String name)
	{
		try
		{
			return new CharsetEncoding(name);
		}
		catch (IllegalCharsetNameException e)
		{
			// just failed
		}
		catch (UnsupportedCharsetException e)
		{
			// just failed
		}

		Object namedService = ServiceManager.getService(serviceClass, name);
		if (namedService != null && namedService instanceof Encoding)
		{
			return (Encoding)namedService;
		}

		// UnsupportedCharsetException is for java.nio.charset,
		// but throw this here too so that this can be caught as
		// an encoding error by catch clause for general I/O code.
		throw new UnsupportedCharsetException("No such encoding: \"" + name + "\"");
	} //}}}

	//{{{ getAvailableNames() method
	/**
	 * Returns the set of all available encoding names.
	 */
	public static Set<String> getAvailableNames()
	{
		Set<String> set = new HashSet<String>();
		set.addAll(Charset.availableCharsets().keySet());
		set.addAll(Arrays.asList(ServiceManager.getServiceNames(serviceClass)));
		return set;
	} //}}}

	//{{{ getSelectedNames() method
	/**
	 * Returns the set of user selected encoding names.
	 */
	public static Set<String> getSelectedNames()
	{
		Set<String> set = getAvailableNames();
		Iterator<String> i = set.iterator();
		while (i.hasNext())
		{
			String name = i.next();
			if (jEdit.getBooleanProperty("encoding.opt-out." + name, false))
			{
				i.remove();
			}
		}
		return set;
	} //}}}

	//{{{ getTextReader() method
	/**
	 * Returns a Reader object that reads the InputStream with
	 * the encoding. This method is same with
	 * "getEncoding(encoding).getTextReader(in)".
	 */
	public static Reader getTextReader(InputStream in, String encoding)
		throws IOException
	{
		return getEncoding(encoding).getTextReader(in);
	} //}}}

	//{{{ getTextWriter() method
	/**
	 * Returns a Writer object that writes to the OutputStream with
	 * the encoding. This method is same with
	 * "getEncoding(encoding).getTextWriter(out)".
	 */
	public static Writer getTextWriter(OutputStream out, String encoding)
		throws IOException
	{
		return getEncoding(encoding).getTextWriter(out);
	} //}}}

	//{{{ hasEncoding() method
	/**
	 * Returns if the specified name is supported as a name for an Encoding.
	 */
	public static boolean hasEncoding(String name)
	{
		try
		{
			if (Charset.isSupported(name))
			{
				return true;
			}
		}
		catch (IllegalCharsetNameException e)
		{
			// The name is illegal for java.nio.charset.Charset.
			// But it may be legal for service name.
		}

		return Arrays.asList(ServiceManager.getServiceNames(serviceClass)).contains(name);
	} //}}}

	//{{{ Private members
	private static final String serviceClass = "org.gjt.sp.jedit.io.Encoding";
	//}}}
}
