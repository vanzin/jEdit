/*
 * MiscUtilities.java - Various miscallaneous utility functions
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 2000 Richard S. Hall
 * Portions copyright (C) 2001 Dirk Moebius
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
import org.gjt.sp.util.Log;

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
	 * A more intelligent version of String.compareTo() that handles
	 * numbers specially. For example, it places "My file 2" before
	 * "My file 10".
	 * @param str1 The first string
	 * @param str2 The second string
	 * @param ignoreCase If true, case will be ignored
	 * @return negative If str1 &lt; str2, 0 if both are the same,
	 * positive if str1 &gt; str2
	 * @since jEdit 4.0pre1
	 */
	public static int compareStrings(String str1, String str2, boolean ignoreCase)
	{
		char[] char1 = str1.toCharArray();
		char[] char2 = str2.toCharArray();

		int len = Math.min(char1.length,char2.length);

		for(int i = 0, j = 0; i < len && j < len; i++, j++)
		{
			char ch1 = char1[i];
			char ch2 = char2[j];
			if(Character.isDigit(ch1) && Character.isDigit(ch2)
				&& ch1 != '0' && ch2 != '0')
			{
				int _i = i + 1;
				int _j = j + 1;

				for(; _i < char1.length; _i++)
				{
					if(!Character.isDigit(char1[_i]))
					{
						//_i--;
						break;
					}
				}

				for(; _j < char2.length; _j++)
				{
					if(!Character.isDigit(char2[_j]))
					{
						//_j--;
						break;
					}
				}

				int num1 = Integer.parseInt(new String(char1,
					i,_i - i));
				int num2 = Integer.parseInt(new String(char2,
					j,_j - j));

				if(num1 != num2)
					return num1 - num2;

				i = _i - 1;
				j = _j - 1;
			}
			else
			{
				if(ignoreCase)
				{
					ch1 = Character.toLowerCase(ch1);
					ch2 = Character.toLowerCase(ch2);
				}

				if(ch1 != ch2)
					return ch1 - ch2;
			}
		}

		return char1.length - char2.length;
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
			return compareStrings(obj1.toString(),
				obj2.toString(),false);
		}
	}

	/**
	 * Compares strings ignoring case.
	 */
	public static class StringICaseCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return compareStrings(obj1.toString(),
				obj2.toString(),true);
		}
	}

	public static class MenuItemCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return compareStrings(((JMenuItem)obj1).getText(),
				((JMenuItem)obj2).getText(),true);
		}
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

	/**
	 * If on JDK 1.2 or higher, make sure that tools.jar is available.
	 * This method should be called by plugins requiring the classes
	 * in this library.
	 * <p>
	 * tools.jar is searched for in the following places:
	 * <ol>
	 *   <li>the classpath that was used when jEdit was started,
	 *   <li>jEdit's jars folder in the user's home,
	 *   <li>jEdit's system jars folder,
	 *   <li><i>java.home</i>/lib/. In this case, tools.jar is added to
	 *       jEdit's list of known jars using jEdit.addPluginJAR(),
	 *       so that it gets loaded through JARClassLoader.
	 * </ol><p>
	 *
	 * On older JDK's this method does not perform any checks, and returns
	 * <code>true</code> (even though there is no tools.jar).
	 *
	 * @return <code>false</code> if and only if on JDK 1.2 and tools.jar
	 *    could not be found. In this case it prints some warnings on Log,
	 *    too, about the places where it was searched for.
	 * @since jEdit 3.2.2
	 */
	public static boolean isToolsJarAvailable()
	{
		String javaVersion = System.getProperty("java.version");
		if(compareStrings(javaVersion,"1.2",false) < 0)
			return true;

		Log.log(Log.DEBUG, MiscUtilities.class, "JDK 1.2 or higher "
			+ "detected, searching for tools.jar...");

		Vector paths = new Vector();

		// 1. Check whether tools.jar is in the system classpath:
		paths.addElement("System classpath: "
			+ System.getProperty("java.class.path"));

		try
		{
			// Either class sun.tools.javac.Main or
			// com.sun.tools.javac.Main must be there:
			try
			{
				Class.forName("sun.tools.javac.Main");
			}
			catch(ClassNotFoundException e1)
			{
				Class.forName("com.sun.tools.javac.Main");
			}
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- is in classpath. Fine.");
			return true;
		}
		catch(ClassNotFoundException e)
		{
			//Log.log(Log.DEBUG, MiscUtilities.class,
			//	"- is not in system classpath.");
		}

		// 2. Check whether it is in the jEdit user settings jars folder:
		String settingsDir = jEdit.getSettingsDirectory();
		if(settingsDir != null)
		{
			String toolsPath = constructPath(settingsDir, "jars",
				"tools.jar");
			paths.addElement(toolsPath);
			if(new File(toolsPath).exists())
			{
				Log.log(Log.DEBUG, MiscUtilities.class,
					"- is in the user's jars folder. Fine.");
				// jEdit will load it automatically
				return true;
			}
		}

		// 3. Check whether it is in jEdit's system jars folder:
		String jEditDir = jEdit.getJEditHome();
		String toolsPath = constructPath(jEditDir, "jars", "tools.jar");
		paths.addElement(toolsPath);
		if(new File(toolsPath).exists())
		{
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- is in jEdit's system jars folder. Fine.");
			// jEdit will load it automatically
			return true;
		}

		// 4. Check whether it is in <java.home>/lib:
		toolsPath = System.getProperty("java.home");
		if(toolsPath.toLowerCase().endsWith(File.separator + "jre"))
			toolsPath = toolsPath.substring(0, toolsPath.length() - 4);
		toolsPath = constructPath(toolsPath, "lib", "tools.jar");
		paths.addElement(toolsPath);

		if(!(new File(toolsPath).exists()))
		{
			Log.log(Log.WARNING, MiscUtilities.class,
				"Could not find tools.jar.\n"
				+ "I checked the following locations:\n"
				+ paths.toString());
			return false;
		}

		// Load it, if not yet done:
		EditPlugin.JAR jar = jEdit.getPluginJAR(toolsPath);
		if(jar == null)
		{
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- adding " + toolsPath + " to jEdit plugins.");
			try
			{
				jEdit.addPluginJAR(new EditPlugin.JAR(toolsPath,
					new JARClassLoader(toolsPath)));
			}
			catch(IOException ioex)
			{
				Log.log(Log.ERROR, MiscUtilities.class,
					"- I/O error loading " + toolsPath);
				Log.log(Log.ERROR, MiscUtilities.class, ioex);
				return false;
			}
		}
		else
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- has been loaded before.");
		return true;
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
