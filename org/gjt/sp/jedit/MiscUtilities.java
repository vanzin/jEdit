/*
 * MiscUtilities.java - Various miscallaneous utility functions
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 2000 Richard S. Hall
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

package org.gjt.sp.jedit;

import javax.swing.JMenuItem;
import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Class with several useful miscellaneous functions.<p>
 *
 * It provides methods for converting file names to class names, for
 * constructing path names, and for various indentation calculations.
 * A quicksort implementation is also available.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class MiscUtilities
{
	/**
	 * Converts a file name to a class name. All slash characters are
	 * replaced with periods and the trailing '.class' is removed.
	 * @param name The file name
	 */
	public static String fileToClass(String name)
	{
		char[] clsName = name.toCharArray();
		for(int i = clsName.length - 6; i >= 0; i--)
			if(clsName[i] == '/')
				clsName[i] = '.';
		return new String(clsName,0,clsName.length - 6);
	}

	/**
	 * Converts a class name to a file name. All periods are replaced
	 * with slashes and the '.class' extension is added.
	 * @param name The class name
	 */
	public static String classToFile(String name)
	{
		return name.replace('.','/').concat(".class");
	}

	/**
	 * Constructs an absolute path name from a directory and another
	 * path name.
	 * @param parent The directory
	 * @param path The path name
	 */
	public static String constructPath(String parent, String path)
	{
		if(new File(path).isAbsolute())
			return canonPath(path);

		if(parent == null)
			parent = System.getProperty("user.dir");

		// have to handle these cases specially on windows.
		if(File.separatorChar == '\\')
		{
			if(path.length() == 2 && path.charAt(1) == ':')
				return path;
			if(path.startsWith("/") || path.startsWith("\\"))
				parent = parent.substring(0,2);
		}

		if(parent.endsWith(File.separator) || path.endsWith("/"))
			return canonPath(parent + path);
		else
			return canonPath(parent + File.separator + path);
	}

	/**
	 * Constructs an absolute path name from three path components.
	 * @param parent The parent directory
	 * @param path1 The first path
	 * @param path2 The second path
	 */
	public static String constructPath(String parent,
		String path1, String path2)
	{
		return constructPath(constructPath(parent,path1),path2);
	}

	/**
	 * Like constructPath(), except <code>path</code> will be
	 * appended to <code>parent</code> even if it is absolute.
	 * @param path
	 * @param parent
	 */
	public static String concatPath(String parent, String path)
	{
		// Make all child paths relative.
		if (path.startsWith(File.separator))
			path = path.substring(1);
		else if ((path.length() >= 3) && (path.charAt(1) == ':'))
			path = path.replace(':', File.separatorChar);

		if (parent == null)
			parent = System.getProperty("user.dir");

		if (parent.endsWith(File.separator))
			return parent + path;
		else
			return parent + File.separator + path;
	}
	
	/**
	 * Returns the extension of the specified filename, or an empty
	 * string if there is none.
	 * @param name The file name
	 */
	public static String getFileExtension(String name)
	{
		int index = name.indexOf('.');
		if(index == -1)
			return "";
		else
			return name.substring(index);
	}

	/**
	 * For use with local files only - returns the last component
	 * of the specified path.
	 * @param path The path name
	 */
	public static String getFileName(String path)
	{
		int count = Math.max(0,path.length() - 2);
		int index1 = path.lastIndexOf(File.separatorChar,count);
		int index2 = path.lastIndexOf('/',count);

		return path.substring(Math.max(index1,index2) + 1);
	}

	/**
	 * @deprecated Call getParentOfPath() instead
	 */
	public static String getFileParent(String path)
	{
		return getParentOfPath(path);
	}

	/**
	 * For use with local files only - returns the parent of the
	 * specified path.
	 * @param path The path name
	 * @since jEdit 2.6pre5
	 */
	public static String getParentOfPath(String path)
	{
		// ignore last character of path to properly handle
		// paths like /foo/bar/
		int count = Math.max(0,path.length() - 2);
		int index = path.lastIndexOf(File.separatorChar,count);
		if(index == -1)
			index = path.lastIndexOf('/',count);
		if(index == -1)
		{
			// this ensures that getFileParent("protocol:"), for
			// example, is "protocol:" and not "".
			index = path.lastIndexOf(':');
		}

		return path.substring(0,index + 1);
	}

	/**
	 * @deprecated Call getProtocolOfURL() instead
	 */
	public static String getFileProtocol(String url)
	{
		return getProtocolOfURL(url);
	}

	/**
	 * Returns the protocol specified by a URL.
	 * @param url The URL
	 * @since jEdit 2.6pre5
	 */
	public static String getProtocolOfURL(String url)
	{
		return url.substring(0,url.indexOf(':'));
	}

	/**
	 * Checks if the specified string is a URL.
	 * @param str The string to check
	 * @return True if the string is a URL, false otherwise
	 */
	public static boolean isURL(String str)
	{
		int fsIndex = Math.max(str.indexOf(File.separatorChar),
			str.indexOf('/'));
		if(fsIndex == 0) // /etc/passwd
			return false;
		else if(fsIndex == 2) // C:\AUTOEXEC.BAT
			return false;

		int cIndex = str.indexOf(':');
		if(cIndex <= 1) // D:\WINDOWS
			return false;
		else if(fsIndex != -1 && cIndex > fsIndex) // /tmp/RTF::read.pm
			return false;

		return true;
	}

	/**
	 * Returns the number of leading white space characters in the
	 * specified string.
	 * @param str The string
	 */
	public static int getLeadingWhiteSpace(String str)
	{
		int whitespace = 0;
loop:		for(;whitespace < str.length();)
		{
			switch(str.charAt(whitespace))
			{
			case ' ': case '\t':
				whitespace++;
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	}

	/**
	 * Returns the number of trailing whitespace characters in the
	 * specified string.
	 * @param str The string
	 * @since jEdit 2.5pre5
	 */
	public static int getTrailingWhiteSpace(String str)
	{
		int whitespace = 0;
loop:		for(int i = str.length() - 1; i >= 0; i--)
		{
			switch(str.charAt(i))
			{
			case ' ': case '\t':
				whitespace++;
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	}

	/**
	 * Returns the width of the leading white space in the specified
	 * string.
	 * @param str The string
	 * @param tabSize The tab size
	 */
	public static int getLeadingWhiteSpaceWidth(String str, int tabSize)
	{
		int whitespace = 0;
loop:		for(int i = 0; i < str.length(); i++)
		{
			switch(str.charAt(i))
			{
			case ' ':
				whitespace++;
				break;
			case '\t':
				whitespace += (tabSize - whitespace % tabSize);
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	}

	/**
	 * Creates a string of white space with the specified length.
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 */
	public static String createWhiteSpace(int len, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		if(tabSize == 0)
		{
			while(len-- > 0)
				buf.append(' ');
		}
		else		
		{
			int count = len / tabSize;
			while(count-- > 0)
				buf.append('\t');
			count = len % tabSize;
			while(count-- > 0)
				buf.append(' ');
		}
		return buf.toString();
	}

	/**
	 * Converts a Unix-style glob to a regular expression.
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 */
	public static String globToRE(String glob)
	{
		StringBuffer buf = new StringBuffer();
		boolean backslash = false;
		boolean insideGroup = false;

		for(int i = 0; i < glob.length(); i++)
		{
			char c = glob.charAt(i);
			if(backslash)
			{
				buf.append('\\');
				buf.append(c);
				backslash = false;
				continue;
			}

			switch(c)
			{
			case '\\':
				backslash = true;
				break;
			case '?':
				buf.append('.');
				break;
			case '.':
				buf.append("\\.");
				break;
			case '*':
				buf.append(".*");
				break;
			case '{':
				buf.append('(');
				insideGroup = true;
				break;
			case ',':
				if(insideGroup)
					buf.append('|');
				else
					buf.append(',');
				break;
			case '}':
				buf.append(')');
				insideGroup = false;
				break;
			default:
				buf.append(c);
			}
		}

		return buf.toString();
	}

	/**
	 * Converts "\n" and "\t" escapes in the specified string to
	 * newlines and tabs.
	 * @param str The string
	 * @since jEdit 2.3pre1
	 */
	public static String escapesToChars(String str)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch(c)
			{
			case '\\':
				if(i == str.length() - 1)
				{
					buf.append('\\');
					break;
				}
				c = str.charAt(++i);
				switch(c)
				{
				case 'n':
					buf.append('\n');
					break;
				case 't':
					buf.append('\t');
					break;
				default:
					buf.append(c);
					break;
				}
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

	/**
	 * Escapes newlines, tabs, backslashes, quotes in the specified
	 * string.
	 * @param str The string
	 * @since jEdit 2.3pre1
	 */
	public static String charsToEscapes(String str)
	{
		return charsToEscapes(str,false);
	}

	/**
	 * Escapes newlines, tabs, backslashes, quotes in the specified
	 * string.
	 * @param str The string
	 * @param history jEdit history files require additional escaping
	 * @since jEdit 2.7pre2
	 */
	public static String charsToEscapes(String str, boolean history)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch(c)
			{
			case '\n':
				buf.append("\\n");
				break;
			case '\t':
				buf.append("\\t");
				break;
			case '[':
				if(history)
					buf.append("\\[");
				else
					buf.append(c);
				break;
			case ']':
				if(history)
					buf.append("\\]");
				else
					buf.append(c);
				break;
			case '"':
				if(history)
					buf.append(c);
				else
					buf.append("\\\"");
				break;
			case '\'':
				if(history)
					buf.append(c);
				else
					buf.append("\\\'");
				break;
			case '\\':
				buf.append("\\\\");
				break;
			default:
				buf.append(c);
				break;
			}
		}
		return buf.toString();
	}

	/**
	 * Sorts the specified array.
	 * @param obj The array
	 * @param compare Compares the objects
	 */
	public static void quicksort(Object[] obj, Compare compare)
	{
		if(obj.length == 0)
			return;

		quicksort(obj,0,obj.length - 1,compare);
	}

	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 */
	public static void quicksort(Vector vector, Compare compare)
	{
		if(vector.size() == 0)
			return;

		quicksort(vector,0,vector.size() - 1,compare);
	}

	/**
	 * An interface for comparing objects.
	 */
	public interface Compare
	{
		int compare(Object obj1, Object obj2);
	}

	/**
	 * Compares strings.
	 */
	public static class StringCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return obj1.toString().compareTo(obj2.toString());
		}
	}

	/**
	 * Compares strings ignoring case.
	 */
	public static class StringICaseCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return obj1.toString().toLowerCase()
				.compareTo(obj2.toString()
				.toLowerCase());
		}
	}

	public static class MenuItemCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((JMenuItem)obj1).getText().compareTo(
				((JMenuItem)obj2).getText());
		}
	}

	/**
	 * Compares two version strings formatted like 'xxx.xx.xxx'.
	 * The version string are tokenized at '.' and the substrings
	 * of both strings are compared with one after another.
	 * For each substring at first they are compared as Integers
	 * and if that fails, as Strings. The comparison ends with
	 * the first difference.
	 * Note, that "1.2.0" < "1.2.0pre1", because "0" < "0pre1".
	 * Therefore you should avoid mixing numbers and text.
	 * Case is <i>not</i> ignored.
	 */
	public static class VersionCompare implements Compare
	{
		/**
		 * compare two version strings 
		 * @param obj1 first version. Should be a String.
		 * @param obj2 secons version. Should be a String.
		 * @return a negative value, if <code>obj1 < obj2</code>, 
		 *         a positive value, if <code>obj1 > obj2</code>,
		 *         0, if <code>obj1.equals(obj2)</code>.
		 */
		public int compare(Object obj1, Object obj2)
		{
			String v1 = obj1.toString();
			String v2 = obj2.toString();
			StringTokenizer vt1 = new StringTokenizer(v1,".");
			StringTokenizer vt2 = new StringTokenizer(v2,".");
			int comp = 0;
			
			while(vt1.hasMoreTokens() && vt2.hasMoreTokens()) {
				String vt1tok = vt1.nextToken();
				String vt2tok = vt2.nextToken();
				try
				{
					int i1 = Integer.parseInt(vt1tok);
					int i2 = Integer.parseInt(vt2tok);
					comp = i1 < i2 ? -1 : i1 > i2 ? 1 : 0;
				}
				catch(NumberFormatException e)
				{	
					comp = vt1tok.compareTo(vt2tok);
				}
				if(comp != 0)
					return comp;
			}
			
			return vt1.hasMoreTokens() ? 1 
				: vt2.hasMoreTokens() ? -1 : 0;
		}
	}

	/**
	 * Helper function to compare two version strings, using the 
	 * VersionCompare class.
	 * @param version1 the first version string
	 * @param version2 the second version string
	 * @return a negative value, if <code>version1 &lt; version2</code>, 
	 *         a positive value, if <code>version1 &gt; version2</code>,
	 *         0, if <code>version1.equals(version2)</code>.
	 */
	public static int compareVersions(String version1, String version2)
	{
		VersionCompare comparator = new VersionCompare();
		return comparator.compare(version1,version2);
	}
	
	/**
	 * Converts an internal version number (build) into a
	 * `human-readable' form.
	 * @param build The build
	 */
	public static String buildToVersion(String build)
	{
		if(build.length() != 11)
			return "<unknown version: " + build + ">";
		// First 2 chars are the major version number
		int major = Integer.parseInt(build.substring(0,2));
		// Second 2 are the minor number
		int minor = Integer.parseInt(build.substring(3,5));
		// Then the pre-release status
		int beta = Integer.parseInt(build.substring(6,8));
		// Finally the bug fix release
		int bugfix = Integer.parseInt(build.substring(9,11));

		return "" + major + "." + minor
			+ (beta != 99 ? "pre" + beta :
			(bugfix != 0 ? "." + bugfix : "final"));
	}

	// private members
	private MiscUtilities() {}

	private static String canonPath(String path)
	{
		if(File.separatorChar == '\\')
		{
			// get rid of mixed paths on Windows
			path = path.replace('/','\\');
		}

		try
		{
			return new File(path).getCanonicalPath();
		}
		catch(Exception e)
		{
			return path;
		}
	}

	private static void quicksort(Object[] obj, int _start, int _end,
		Compare compare)
	{
		int start = _start;
		int end = _end;

		Object mid = obj[(_start + _end) / 2];

		if(_start > _end)
			return;

		while(start <= end)
		{
			while((start < _end) && (compare.compare(obj[start],mid) < 0))
				start++;

			while((end > _start) && (compare.compare(obj[end],mid) > 0))
				end--;

			if(start <= end)
			{
				Object o = obj[start];
				obj[start] = obj[end];
				obj[end] = o;

				start++;
				end--;
			}
		}

		if(_start < end)
			quicksort(obj,_start,end,compare);

		if(start < _end)
			quicksort(obj,start,_end,compare);
	}

	private static void quicksort(Vector obj, int _start, int _end,
		Compare compare)
	{
		int start = _start;
		int end = _end;

		Object mid = obj.elementAt((_start + _end) / 2);

		if(_start > _end)
			return;

		while(start <= end)
		{
			while((start < _end) && (compare.compare(obj.elementAt(start),mid) < 0))
				start++;

			while((end > _start) && (compare.compare(obj.elementAt(end),mid) > 0))
				end--;

			if(start <= end)
			{
				Object o = obj.elementAt(start);
				obj.setElementAt(obj.elementAt(end),start);
				obj.setElementAt(o,end);

				start++;
				end--;
			}
		}

		if(_start < end)
			quicksort(obj,_start,end,compare);

		if(start < _end)
			quicksort(obj,start,_end,compare);
	}
}
