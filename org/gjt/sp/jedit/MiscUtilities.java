/*
 * MiscUtilities.java - Various miscallaneous utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import gnu.regexp.RE;
import javax.swing.text.Segment;
import javax.swing.JMenuItem;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Class with several useful miscellaneous functions.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class MiscUtilities
{
	//{{{ Path name methods

	//{{{ canonPath() method
	/**
	 * Returns the canonical form of the specified path name. Currently
	 * only expands a leading <code>~</code>. <b>For local path names
	 * only.</b>
	 * @param path The path name
	 * @since jEdit 4.0pre2
	 */
	public static String canonPath(String path)
	{
		if(File.separatorChar == '\\')
		{
			// get rid of mixed paths on Windows
			path = path.replace('/','\\');
		}

		if(path.startsWith("~" + File.separator))
		{
			path = path.substring(2);
			String home = System.getProperty("user.home");

			if(home.endsWith(File.separator))
				return home + path;
			else
				return home + File.separator + path;
		}
		else if(path.equals("~"))
			return System.getProperty("user.home");
		else
			return path;
	} //}}}

	//{{{ constructPath() method
	/**
	 * Constructs an absolute path name from a directory and another
	 * path name. This method is VFS-aware.
	 * @param parent The directory
	 * @param path The path name
	 */
	public static String constructPath(String parent, String path)
	{
		if(MiscUtilities.isURL(path))
			return path;
		else if(path.startsWith("~"))
			return path;
		else
		{
			// have to handle these cases specially on windows.
			if(OperatingSystem.isDOSDerived())
			{
				if(path.length() == 2 && path.charAt(1) == ':')
					return path;
				else if(path.length() > 2 && path.charAt(1) == ':')
				{
					if(path.charAt(2) != '\\')
					{
						path = path.substring(0,2) + '\\'
							+ path.substring(2);
					}

					return resolveSymlinks(path);
				}
				else if(path.startsWith("\\\\"))
					return resolveSymlinks(path);
			}
			else if(OperatingSystem.isUnix())
			{
				// nice and simple
				if(path.length() > 0 && path.charAt(0) == '/')
					return resolveSymlinks(path);
			}
		}

		if(parent == null)
			parent = System.getProperty("user.dir");

		if(OperatingSystem.isDOSDerived() && path.startsWith("\\"))
			parent = parent.substring(0,2);
		
		VFS vfs = VFSManager.getVFSForPath(parent);
		return vfs.constructPath(parent,path);
	} //}}}

	//{{{ constructPath() method
	/**
	 * Constructs an absolute path name from three path components.
	 * This method is VFS-aware.
	 * @param parent The parent directory
	 * @param path1 The first path
	 * @param path2 The second path
	 */
	public static String constructPath(String parent,
		String path1, String path2)
	{
		return constructPath(constructPath(parent,path1),path2);
	} //}}}

	//{{{ concatPath() method
	/**
	 * Like constructPath(), except <code>path</code> will be
	 * appended to <code>parent</code> even if it is absolute.
	 * @param path
	 * @param parent
	 */
	public static String concatPath(String parent, String path)
	{
		parent = canonPath(parent);
		path = canonPath(path);

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
	} //}}}

	//{{{ getFileExtension() method
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
	} //}}}

	//{{{ getFileName() method
	/**
	 * Returns the last component of the specified path.
	 * This method is VFS-aware.
	 * @param path The path name
	 */
	public static String getFileName(String path)
	{
		return VFSManager.getVFSForPath(path).getFileName(path);
	} //}}}

	//{{{ getFileNameNoExtension() method
	/**
	 * Returns the last component of the specified path name without the
	 * trailing extension (if there is one).
	 * @param path The path name
	 * @since jEdit 4.0pre8
	 */
	public static String getFileNameNoExtension(String path)
	{
		String name = getFileName(path);
		int index = name.lastIndexOf('.');
		if(index == -1)
			return name;
		else
			return name.substring(0,index);
	} //}}}

	//{{{ getFileParent() method
	/**
	 * @deprecated Call getParentOfPath() instead
	 */
	public static String getFileParent(String path)
	{
		return getParentOfPath(path);
	} //}}}

	//{{{ getParentOfPath() method
	/**
	 * Returns the parent of the specified path. This method is VFS-aware.
	 * @param path The path name
	 * @since jEdit 2.6pre5
	 */
	public static String getParentOfPath(String path)
	{
		return VFSManager.getVFSForPath(path).getParentOfPath(path);
	} //}}}

	//{{{ getFileProtocol() method
	/**
	 * @deprecated Call getProtocolOfURL() instead
	 */
	public static String getFileProtocol(String url)
	{
		return getProtocolOfURL(url);
	} //}}}

	//{{{ getProtocolOfURL() method
	/**
	 * Returns the protocol specified by a URL.
	 * @param url The URL
	 * @since jEdit 2.6pre5
	 */
	public static String getProtocolOfURL(String url)
	{
		return url.substring(0,url.indexOf(':'));
	} //}}}

	//{{{ isURL() method
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
	} //}}}

	//{{{ saveBackup() method
	/**
	 * Saves a backup (optionally numbered) of a file.
	 * @param file A local file
	 * @param backups The number of backups. Must be >= 1. If > 1, backup
	 * files will be numbered.
	 * @param backupPrefix The backup file name prefix
	 * @param backupSuffix The backup file name suffix
	 * @param backupDirectory The directory where to save backups; if null,
	 * they will be saved in the same directory as the file itself.
	 * @since jEdit 4.0pre1
	 */
	public static void saveBackup(File file, int backups,
		String backupPrefix, String backupSuffix,
		String backupDirectory)
	{
		if(backupPrefix == null)
			backupPrefix = "";
		if(backupSuffix == null)
			backupSuffix = "";

		String name = file.getName();

		// If backups is 1, create ~ file
		if(backups == 1)
		{
			File backupFile = new File(backupDirectory,
				backupPrefix + name + backupSuffix);
			backupFile.delete();
			file.renameTo(backupFile);
		}
		// If backups > 1, move old ~n~ files, create ~1~ file
		else
		{
			new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ backups + backupSuffix).delete();

			for(int i = backups - 1; i > 0; i--)
			{
				File backup = new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ i + backupSuffix);

				backup.renameTo(new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ (i+1) + backupSuffix));
			}

			file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ "1" + backupSuffix));
		}
	} //}}}

	//{{{ fileToClass() method
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
	} //}}}

	//{{{ classToFile() method
	/**
	 * Converts a class name to a file name. All periods are replaced
	 * with slashes and the '.class' extension is added.
	 * @param name The class name
	 */
	public static String classToFile(String name)
	{
		return name.replace('.','/').concat(".class");
	} //}}}

	//}}}

	//{{{ Text methods

	//{{{ getLeadingWhiteSpace() method
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
	} //}}}

	//{{{ getTrailingWhiteSpace() method
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
	} //}}}

	//{{{ getLeadingWhiteSpaceWidth() method
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
	} //}}}

	//{{{ getVirtualWidth() method
	/**
	 * Returns the virtual column number (taking tabs into account) of the
	 * specified offset in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 * @since jEdit 4.1pre1
	 */
	public static int getVirtualWidth(Segment seg, int tabSize)
	{
		int virtualPosition = 0;

		for (int i = 0; i < seg.count; i++)
		{
			char ch = seg.array[seg.offset + i];

			if (ch == '\t')
			{
				virtualPosition += tabSize
					- (virtualPosition % tabSize);
			}
			else
			{
				++virtualPosition;
			}
		}

		return virtualPosition;
	} //}}}

	//{{{ getOffsetOfVirtualColumn() method
	/**
	 * Returns the array offset of a virtual column number (taking tabs
	 * into account) in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 * @param column The virtual column number
	 * @param totalVirtualWidth If this array is non-null, the total
	 * virtual width will be stored in its first location if this method
	 * returns -1.
	 *
	 * @return -1 if the column is out of bounds
	 *
	 * @since jEdit 4.1pre1
	 */
	public static int getOffsetOfVirtualColumn(Segment seg, int tabSize,
		int column, int[] totalVirtualWidth)
	{
		int virtualPosition = 0;

		for (int i = 0; i < seg.count; i++)
		{
			char ch = seg.array[seg.offset + i];

			if (ch == '\t')
			{
				int tabWidth = tabSize
					- (virtualPosition % tabSize);
				if(virtualPosition >= column)
					return i;
				else
					virtualPosition += tabWidth;
			}
			else
			{
				if(virtualPosition >= column)
					return i;
				else
					++virtualPosition;
			}
		}

		if(totalVirtualWidth != null)
			totalVirtualWidth[0] = virtualPosition;
		return -1;
	} //}}}

	//{{{ createWhiteSpace() method
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
	} //}}}

	//{{{ globToRE() method
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
		boolean insideNegativeLookahead = false;

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
				if(i + 1 != glob.length() && glob.charAt(i + 1) == '!')
				{
					buf.append('?');
					insideNegativeLookahead = true;
				}
				else
					insideGroup = true;
				break;
			case ',':
				if(insideGroup)
				{
					if(insideNegativeLookahead)
					{
						buf.append(").*");
						insideNegativeLookahead = false;
					}
					buf.append('|');
				}
				else
					buf.append(',');
				break;
			case '}':
				if(insideNegativeLookahead)
				{
					buf.append(").*");
					insideNegativeLookahead = false;
				}
				else if(insideGroup)
				{
					buf.append(')');
					insideGroup = false;
				}
				else
					buf.append('}');
				break;
			default:
				buf.append(c);
			}
		}

		return buf.toString();
	} //}}}

	//{{{ escapesToChars() method
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
	} //}}}

	//{{{ charsToEscapes() method
	/**
	 * Escapes newlines, tabs, backslashes, and quotes in the specified
	 * string.
	 * @param str The string
	 * @since jEdit 2.3pre1
	 */
	public static String charsToEscapes(String str)
	{
		return charsToEscapes(str,"\n\t\\\"'");
	} //}}}

	//{{{ charsToEscapes() method
	/**
	 * Escapes the specified characters in the specified string.
	 * @param str The string
	 * @param extra Any characters that require escaping
	 * @since jEdit 4.1pre3
	 */
	public static String charsToEscapes(String str, String toEscape)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if(toEscape.indexOf(c) != -1)
			{
				if(c == '\n')
					buf.append("\\n");
				else if(c == '\t')
					buf.append("\\t");
				else
				{
					buf.append('\\');
					buf.append(c);
				}
			}
			else
				buf.append(c);
		}
		return buf.toString();
	} //}}}

	//{{{ compareVersions() method
	/**
	 * @deprecated Call <code>compareStrings()</code> instead
	 */
	public static int compareVersions(String v1, String v2)
	{
		return compareStrings(v1,v2,false);
	} //}}}

	//{{{ compareStrings() method
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

				int len1 = _i - i;
				int len2 = _j - j;
				if(len1 > len2)
					return 1;
				else if(len1 < len2)
					return -1;
				else
				{
					for(int k = 0; k < len1; k++)
					{
						ch1 = char1[i + k];
						ch2 = char2[j + k];
						if(ch1 != ch2)
							return ch1 - ch2;
					}
				}

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
	} //}}}

	//{{{ stringsEqual() method
	/**
	 * Returns if two strings are equal. This correctly handles null pointers,
	 * as opposed to calling <code>s1.equals(s2)</code>.
	 * @since jEdit 4.1pre5
	 */
	public static boolean stringsEqual(String s1, String s2)
	{
		if(s1 == null)
		{
			if(s2 == null)
				return true;
			else
				return false;
		}
		else if(s2 == null)
			return false;
		else
			return s1.equals(s2);
	} //}}}

	//}}}

	//{{{ Sorting methods

	//{{{ quicksort() method
	/**
	 * Sorts the specified array. Equivalent to calling
	 * <code>Arrays.sort()</code>.
	 * @param obj The array
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 */
	public static void quicksort(Object[] obj, Comparator compare)
	{
		Arrays.sort(obj,compare);
	} //}}}

	//{{{ quicksort() method
	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 */
	public static void quicksort(Vector vector, Comparator compare)
	{
		Collections.sort(vector,compare);
	} //}}}

	//{{{ quicksort() method
	/**
	 * Sorts the specified list.
	 * @param list The list
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 */
	public static void quicksort(List list, Comparator compare)
	{
		Collections.sort(list,compare);
	} //}}}

	//{{{ quicksort() method
	/**
	 * Sorts the specified array. Equivalent to calling
	 * <code>Arrays.sort()</code>.
	 * @param obj The array
	 * @param compare Compares the objects
	 */
	public static void quicksort(Object[] obj, Compare compare)
	{
		Arrays.sort(obj,compare);
	} //}}}

	//{{{ quicksort() method
	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 */
	public static void quicksort(Vector vector, Compare compare)
	{
		Collections.sort(vector,compare);
	} //}}}

	//{{{ Compare interface
	/**
	 * An interface for comparing objects. This is a hold-over from
	 * they days when jEdit had its own sorting API due to JDK 1.1
	 * compatibility requirements. Use <code>java.util.Comparable</code>
	 * instead.
	 */
	public interface Compare extends Comparator
	{
		int compare(Object obj1, Object obj2);
	} //}}}

	//{{{ StringCompare class
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
	} //}}}

	//{{{ StringICaseCompare class
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
	} //}}}

	//{{{ MenuItemCompare class
	public static class MenuItemCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return compareStrings(((JMenuItem)obj1).getText(),
				((JMenuItem)obj2).getText(),true);
		}
	} //}}}

	//}}}

	//{{{ buildToVersion() method
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
	} //}}}

	//{{{ isToolsJarAvailable() method
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
		Log.log(Log.DEBUG, MiscUtilities.class,"Searching for tools.jar...");

		Vector paths = new Vector();

		//{{{ 1. Check whether tools.jar is in the system classpath:
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
		} //}}}

		//{{{ 2. Check whether it is in the jEdit user settings jars folder:
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
		} //}}}

		//{{{ 3. Check whether it is in jEdit's system jars folder:
		String jEditDir = jEdit.getJEditHome();
		if(jEditDir != null)
		{
			String toolsPath = constructPath(jEditDir, "jars", "tools.jar");
			paths.addElement(toolsPath);
			if(new File(toolsPath).exists())
			{
				Log.log(Log.DEBUG, MiscUtilities.class,
					"- is in jEdit's system jars folder. Fine.");
				// jEdit will load it automatically
				return true;
			}
		} //}}}

		//{{{ 4. Check whether it is in <java.home>/lib:
		String toolsPath = System.getProperty("java.home");
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
		} //}}}

		//{{{ Load it, if not yet done:
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
		//}}}

		return true;
	} //}}}

	//{{{ Private members
	private MiscUtilities() {}

	//{{{ resolveSymlinks() method
	private static String resolveSymlinks(String path)
	{
		try
		{
			return new File(path).getCanonicalPath();
		}
		catch(IOException io)
		{
			return path;
		}
	} //}}}

	//}}}
}
