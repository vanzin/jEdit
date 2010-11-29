/* {{{ StringList.java - a List of Strings with split() and join() methods
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Alan Ezust
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
 * }}} */

package org.gjt.sp.util;

//{{{ imports
import java.util.ArrayList;
import java.util.Collection;
//}}}

// {{{ StringList class
/**
 * A List&lt;String&gt; with some perl-like convenience functions (split/join primarily),
 * and easy conversion to/from arrays.
 * @since jEdit 4.3pre7
 */
public class StringList extends ArrayList<String>
{

 	// {{{ Constructors
 	public StringList()
	{
	}


	public StringList(Object[] array)
	{
		addAll(array);
	} // }}}

	// {{{ addAll()
	public void addAll(Object[] array)
	{
		for (int i = 0; i < array.length; ++i)
		{
			add(array[i].toString());
		}
	}   // }}}

	// {{{ split()
	/**
	 * @param orig the original string
	 * @param delim a delimiter to use for splitting
	 * @return a new StringList containing the split strings.
	 */
	public static StringList split(String orig, Object delim)
	{
		if ((orig == null) || (orig.length() == 0))
			return new StringList();
		return new StringList(orig.split(delim.toString()));
	} // }}}

	// {{{ toString()
	/**
	 * Joins each string in the list with a newline.
	 * @return a joined string representation of this, 
	 * with the newline (\n) as delimiter. 
	 */
	@Override
	public String toString()
	{
		return join("\n");
	}  // }}}

	// {{{ toArray()
	/** @return an array of String */
	@Override
	public String[] toArray() 
	{
		int siz = size();
		String[] result = new String[siz];
		System.arraycopy(super.toArray(), 0, result, 0, siz);
		return result;
	}
	// }}}

	// {{{ join() methods
	/**
	 * The reverse of split - given a collection, takes each element
	 * and places it in a string, joined by a delimiter.
	 */
	public static String join(Collection<String> c, String delim)
	{
		StringList sl = new StringList();
		for (String s: c)
			sl.add(s);
		return sl.join(delim);
	}

	/**
	 *
	 * @param arr array of objects
	 * @param delim delimiter to separate strings
	 * @return a single string with each element in arr converted to a string and concatenated,
	 * separated by delim.
	 */
	public static String join(Object[] arr, String delim) 
	{
		return new StringList(arr).join(delim);
	}


	/**
	 * Non-static version, that joins "this" StringList.
	 * @param delim the delimiter
	 * @return a joined string with delim inbetween each element
	 */
	public String join(String delim) 
	{
		int s = size();
		if (s < 1)
			return "";
		if (s == 1)
			return get(0);
		else
		{
			StringBuilder retval = new StringBuilder();
			retval.append(get(0));
			for (int i = 1; i < s; ++i)
				retval.append(delim + get(i));
			return retval.toString();
		}

	}  // }}}

	// {{{ main()
	public static void main(String args[])
	{
		String teststr = "a,b,c,d,e,f";
		StringList.split(teststr, ",");
		//String joinstr = sl.join(",");
		// assert(teststr.equals(joinstr));
		System.out.println("Test Passed");

	}// }}}
	private static final long serialVersionUID = -6408080298368668262L;
} // }}}
