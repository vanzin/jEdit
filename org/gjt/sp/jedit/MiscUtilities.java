/*
 * MiscUtilities.java - Various miscallaneous utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
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
import javax.swing.text.Segment;
import javax.swing.JMenuItem;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;
import org.gjt.sp.jedit.menu.EnhancedMenuItem;
//}}}

/**
 * Path name manipulation, string manipulation, and more.<p>
 *
 * The most frequently used members of this class are:<p>
 *
 * <b>Some path name methods:</b><p>
 * <ul>
 * <li>{@link #getFileName(String)}</li>
 * <li>{@link #getParentOfPath(String)}</li>
 * <li>{@link #constructPath(String,String)}</li>
 * </ul>
 * <b>String comparison:</b><p>

 * A {@link #compareStrings(String,String,boolean)} method that unlike
 * <function>String.compareTo()</function>, correctly recognizes and handles
 * embedded numbers.<p>
 *
 * This class also defines several inner classes for use with the
 * sorting features of the Java collections API:
 *
 * <ul>
 * <li>{@link MiscUtilities.StringCompare}</li>
 * <li>{@link MiscUtilities.StringICaseCompare}</li>
 * <li>{@link MiscUtilities.MenuItemCompare}</li>
 * </ul>
 *
 * For example, you might call:<p>
 *
 * <code>Arrays.sort(myListOfStrings,
 *     new MiscUtilities.StringICaseCompare());</code>
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class MiscUtilities
{
	/**
	 * This encoding is not supported by Java, yet it is useful.
	 * A UTF-8 file that begins with 0xEFBBBF.
	 */
	public static final String UTF_8_Y = "UTF-8Y";

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
		if(path.length() == 0)
			return path;

		if(path.startsWith("file://"))
			path = path.substring("file://".length());
		else if(path.startsWith("file:"))
			path = path.substring("file:".length());
		else if(isURL(path))
			return path;

		if(File.separatorChar == '\\')
			{
				// get rid of mixed paths on Windows
				path = path.replace('/','\\');
				// also get rid of trailing spaces on Windows
				int trim = path.length();
				while(path.charAt(trim - 1) == ' ')
					trim--;
				path = path.substring(0,trim);
			}
		else if(OperatingSystem.isMacOS())
			{
				// do the same on OS X
				path = path.replace(':','/');
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

	//{{{ resolveSymlinks() method
	/**
	 * Resolves any symbolic links in the path name specified
	 * using <code>File.getCanonicalPath()</code>. <b>For local path
	 * names only.</b>
	 * @since jEdit 4.2pre1
	 */
	public static String resolveSymlinks(String path)
	{
		if(isURL(path))
			return path;

		// 2 aug 2003: OS/2 Java has a broken getCanonicalPath()
		if(OperatingSystem.isOS2())
			return path;
		// 18 nov 2003: calling this on a drive letter on Windows causes
		// drive access
		if(OperatingSystem.isDOSDerived())
			{
				if(path.length() == 2 || path.length() == 3)
					{
						if(path.charAt(1) == ':')
							return path;
					}
			}
		try
			{
				return new File(path).getCanonicalPath();
			}
		catch(IOException io)
			{
				return path;
			}
	} //}}}

	//{{{ isAbsolutePath() method
	/**
	 * Returns if the specified path name is an absolute path or URL.
	 * @since jEdit 4.1pre11
	 */
	public static boolean isAbsolutePath(String path)
	{
		if(isURL(path))
			return true;
		else if(path.startsWith("~/") || path.startsWith("~" + File.separator) || path.equals("~"))
			return true;
		else if(OperatingSystem.isDOSDerived())
			{
				if(path.length() == 2 && path.charAt(1) == ':')
					return true;
				if(path.length() > 2 && path.charAt(1) == ':'
				   && (path.charAt(2) == '\\'
					   || path.charAt(2) == '/'))
					return true;
				if(path.startsWith("\\\\")
				   || path.startsWith("//"))
					return true;
			}
		// not sure if this is correct for OpenVMS.
		else if(OperatingSystem.isUnix()
				|| OperatingSystem.isVMS())
			{
				// nice and simple
				if(path.length() > 0 && path.charAt(0) == '/')
					return true;
			}

		return false;
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
		if(isAbsolutePath(path))
			return canonPath(path);

		// have to handle this case specially on windows.
		// insert \ between, eg A: and myfile.txt.
		if(OperatingSystem.isDOSDerived())
			{
				if(path.length() == 2 && path.charAt(1) == ':')
					return path;
				else if(path.length() > 2 && path.charAt(1) == ':'
						&& path.charAt(2) != '\\')
					{
						path = path.substring(0,2) + '\\'
							+ path.substring(2);
						return canonPath(path);
					}
			}

		String dd = ".." + File.separator;
		String d = "." + File.separator;

		if(parent == null)
			parent = System.getProperty("user.dir");

		for(;;)
			{
				if(path.equals("."))
					return parent;
				else if(path.equals(".."))
					return getParentOfPath(parent);
				else if(path.startsWith(dd) || path.startsWith("../"))
					{
						parent = getParentOfPath(parent);
						path = path.substring(3);
					}
				else if(path.startsWith(d) || path.startsWith("./"))
					path = path.substring(2);
				else
					break;
			}

		if(OperatingSystem.isDOSDerived()
		   && !isURL(parent)
		   && path.startsWith("\\"))
			parent = parent.substring(0,2);

		VFS vfs = VFSManager.getVFSForPath(parent);

		return canonPath(vfs.constructPath(parent,path));
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
	 * Like {@link #constructPath}, except <code>path</code> will be
	 * appended to <code>parent</code> even if it is absolute.
	 * <b>For local path names only.</b>.
	 *
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

	//{{{ getFirstSeparatorIndex() method
	/**
	 * Return the first index of either / or the OS-specific file
	 * separator.
	 * @param path The path
	 * @since jEdit 4.3pre3
	 */
	public static int getFirstSeparatorIndex(String path)
	{
		int start = getPathStart(path);
		int index = path.indexOf('/',start);
		if(index == -1)
			index = path.indexOf(File.separatorChar,start);
		return index;
	} //}}}

	//{{{ getLastSeparatorIndex() method
	/**
	 * Return the last index of either / or the OS-specific file
	 * separator.
	 * @param path The path
	 * @since jEdit 4.3pre3
	 */
	public static int getLastSeparatorIndex(String path)
	{
		int start = getPathStart(path);
		if(start != 0)
			path = path.substring(start);
		int index = Math.max(path.lastIndexOf('/'),
							 path.lastIndexOf(File.separatorChar));
		if(index == -1)
			return index;
		else
			return index + start;
	} //}}}

	//{{{ getFileExtension() method
	/**
	 * Returns the extension of the specified filename, or an empty
	 * string if there is none.
	 * @param path The path
	 */
	public static String getFileExtension(String path)
	{
		int fsIndex = getLastSeparatorIndex(path);
		int index = path.indexOf('.',fsIndex);
		if(index == -1)
			return "";
		else
			return path.substring(index);
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
		int index = name.indexOf('.');
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
		int fsIndex = getLastSeparatorIndex(str);
		if(fsIndex == 0) // /etc/passwd
			return false;
		else if(fsIndex == 2) // C:\AUTOEXEC.BAT
			return false;

		int cIndex = str.indexOf(':');
		if(cIndex <= 1) // D:\WINDOWS, or doesn't contain : at all
			return false;

		String protocol = str.substring(0,cIndex);
		VFS vfs = VFSManager.getVFSForProtocol(protocol);
		if(vfs != null && !(vfs instanceof UrlVFS))
			return true;

		try
			{
				new URL(str);
				return true;
			}
		catch(MalformedURLException mf)
			{
				return false;
			}
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
		saveBackup(file,backups,backupPrefix,backupSuffix,backupDirectory,0);
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
	 * @param backupTimeDistance The minimum time in minutes when a backup
	 * version 1 shall be moved into version 2; if 0, backups are always
	 * moved.
	 * @since jEdit 4.2pre5
	 */
	public static void saveBackup(File file, int backups,
								  String backupPrefix, String backupSuffix,
								  String backupDirectory, int backupTimeDistance)
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
				long modTime = backupFile.lastModified();
				/* if backup file was created less than
				 * 'backupTimeDistance' ago, we do not
				 * create the backup */
				if(System.currentTimeMillis() - modTime
				   >= backupTimeDistance)
					{
						backupFile.delete();
						if (!file.renameTo(backupFile))
							moveFile(file, backupFile);
					}
			}
		// If backups > 1, move old ~n~ files, create ~1~ file
		else
			{
				/* delete a backup created using above method */
				new File(backupDirectory,
						 backupPrefix + name + backupSuffix
						 + backups + backupSuffix).delete();

				File firstBackup = new File(backupDirectory,
											backupPrefix + name + backupSuffix
											+ "1" + backupSuffix);
				long modTime = firstBackup.lastModified();
				/* if backup file was created less than
				 * 'backupTimeDistance' ago, we do not
				 * create the backup */
				if(System.currentTimeMillis() - modTime
				   >= backupTimeDistance)
					{
						for(int i = backups - 1; i > 0; i--)
							{
								File backup = new File(backupDirectory,
													   backupPrefix + name
													   + backupSuffix + i
													   + backupSuffix);

								backup.renameTo(
												new File(backupDirectory,
														 backupPrefix + name
														 + backupSuffix + (i+1)
														 + backupSuffix));
							}

						File backupFile = new File(backupDirectory,
												   backupPrefix + name + backupSuffix
												   + "1" + backupSuffix);
						if (!file.renameTo(backupFile))
							moveFile(file, backupFile);
					}
			}
	} //}}}

	//{{{ moveFile() method
	/**
	 * Moves the source file to the destination.
	 *
	 * If the destination cannot be created or is a read-only file, the
	 * method returns <code>false</code>. Otherwise, the contents of the
	 * source are copied to the destination, the source is deleted,
	 * and <code>true</code> is returned.
	 *
	 * @param source The source file to move.
	 * @param dest   The destination where to move the file.
	 * @return true on success, false otherwise.
	 *
	 * @since jEdit 4.3pre1
	 */
	public static boolean moveFile(File source, File dest)
	{
		boolean ok = false;

		if ((dest.exists() && dest.canWrite())
			|| (!dest.exists() && dest.getParentFile().canWrite()))
			{
				OutputStream fos = null;
				InputStream fis = null;
				try
					{
						fos = new FileOutputStream(dest);
						fis = new FileInputStream(source);
						ok = copyStream(32768,null,fis,fos,false);
					}
				catch (IOException ioe)
					{
						Log.log(Log.WARNING, MiscUtilities.class,
								"Error moving file: " + ioe + " : " + ioe.getMessage());
					}
				finally
					{
						try
							{
								if(fos != null)
									fos.close();
								if(fis != null)
									fis.close();
							}
						catch(Exception e)
							{
								Log.log(Log.ERROR,MiscUtilities.class,e);
							}
					}

				if(ok)
					source.delete();
			}
		return ok;
	} //}}}

	//{{{ copyStream() method
	/**
	 * Copy an input stream to an output stream.
	 *
	 * @param bufferSize the size of the buffer
	 * @param progress the progress observer it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 4.3pre3
	 */
	public static boolean copyStream(int bufferSize, ProgressObserver progress,
									 InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		byte[] buffer = new byte[bufferSize];
		int n;
		long copied = 0;
		while (-1 != (n = in.read(buffer)))
			{
				out.write(buffer, 0, n);
				copied += n;
				if(progress != null)
					progress.setValue(copied);
				if(canStop && Thread.interrupted()) return false;
			}
		return true;
	} //}}}

	//{{{ copyStream() method
	/**
	 * Copy an input stream to an output stream with a buffer of 4096 bytes.
	 *
	 * @param progress the progress observer it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 4.3pre3
	 */
	public static boolean copyStream(ProgressObserver progress,
									 InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return copyStream(4096,progress, in, out, canStop);
	} //}}}

	//{{{ closeQuietly() method
	/**
	 * Method that will close an {@link InputStream} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param in the InputStream to close.
	 * @since jEdit 4.3pre3
	 */
	public static void closeQuietly(InputStream in)
	{
		if(in != null)
			{
				try
					{
						in.close();
					}
				catch (IOException e)
					{
						//ignore
					}
			}
	} //}}}

	//{{{ copyStream() method
	/**
	 * Method that will close an {@link OutputStream} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param out the OutputStream to close.
	 * @since jEdit 4.3pre3
	 */
	public static void closeQuietly(OutputStream out)
	{
		if(out != null)
			{
				try
					{
						out.close();
					}
				catch (IOException e)
					{
						//ignore
					}
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

	//{{{ pathsEqual() method
	/**
	 * @param p1 A path name
	 * @param p2 A path name
	 * @return True if both paths are equal, ignoring trailing slashes, as
	 * well as case insensitivity on Windows.
	 * @since jEdit 4.3pre2
	 */
	public static boolean pathsEqual(String p1, String p2)
	{
		VFS v1 = VFSManager.getVFSForPath(p1);
		VFS v2 = VFSManager.getVFSForPath(p2);

		if(v1 != v2)
			return false;

		if(p1.endsWith("/") || p1.endsWith(File.separator))
			p1 = p1.substring(0,p1.length() - 1);

		if(p2.endsWith("/") || p2.endsWith(File.separator))
			p2 = p2.substring(0,p2.length() - 1);

		if((v1.getCapabilities() & VFS.CASE_INSENSITIVE_CAP) != 0)
			return p1.equalsIgnoreCase(p2);
		else
			return p1.equals(p2);
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
	 * Creates a string of white space with the specified length.<p>
	 *
	 * To get a whitespace string tuned to the current buffer's
	 * settings, call this method as follows:
	 *
	 * <pre>myWhitespace = MiscUtilities.createWhiteSpace(myLength,
	 *     (buffer.getBooleanProperty("noTabs") ? 0
	 *     : buffer.getTabSize()));</pre>
	 *
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 */
	public static String createWhiteSpace(int len, int tabSize)
	{
		return createWhiteSpace(len,tabSize,0);
	} //}}}

	//{{{ createWhiteSpace() method
	/**
	 * Creates a string of white space with the specified length.<p>
	 *
	 * To get a whitespace string tuned to the current buffer's
	 * settings, call this method as follows:
	 *
	 * <pre>myWhitespace = MiscUtilities.createWhiteSpace(myLength,
	 *     (buffer.getBooleanProperty("noTabs") ? 0
	 *     : buffer.getTabSize()));</pre>
	 *
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 * @param start The start offset, for tab alignment
	 * @since jEdit 4.2pre1
	 */
	public static String createWhiteSpace(int len, int tabSize, int start)
	{
		StringBuffer buf = new StringBuffer();
		if(tabSize == 0)
			{
				while(len-- > 0)
					buf.append(' ');
			}
		else if(len == 1)
			buf.append(' ');
		else
			{
				int count = (len + start % tabSize) / tabSize;
				if(count != 0)
					len += start;
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
	 * Converts a Unix-style glob to a regular expression.<p>
	 *
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 */
	public static String globToRE(String glob)
	{
		final Object NEG = new Object();
		final Object GROUP = new Object();
		Stack state = new Stack();

		StringBuffer buf = new StringBuffer();
		boolean backslash = false;

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
			case '+':
			case '(':
			case ')':
				buf.append('\\');
				buf.append(c);
				break;
			case '*':
				buf.append(".*");
				break;
			case '|':
				if(backslash)
					buf.append("\\|");
				else
					buf.append('|');
				break;
			case '{':
				buf.append('(');
				if(i + 1 != glob.length() && glob.charAt(i + 1) == '!')
				{
					buf.append('?');
					state.push(NEG);
				}
				else
					state.push(GROUP);
				break;
			case ',':
				if(!state.isEmpty() && state.peek() == GROUP)
					buf.append('|');
				else
					buf.append(',');
				break;
			case '}':
				if(!state.isEmpty())
				{
					buf.append(")");
					if(state.pop() == NEG)
						buf.append(".*");
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
	 * @param toEscape Any characters that require escaping
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
	 * Compares two strings.<p>
	 *
	 * Unlike <function>String.compareTo()</function>,
	 * this method correctly recognizes and handles embedded numbers.
	 * For example, it places "My file 2" before "My file 10".<p>
	 *
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
	 * @deprecated Call <code>objectsEqual()</code> instead.
	 */
	public static boolean stringsEqual(String s1, String s2)
	{
		return objectsEqual(s1,s2);
	} //}}}

	//{{{ objectsEqual() method
	/**
	 * Returns if two strings are equal. This correctly handles null pointers,
	 * as opposed to calling <code>o1.equals(o2)</code>.
	 * @since jEdit 4.2pre1
	 */
	public static boolean objectsEqual(Object o1, Object o2)
	{
		if(o1 == null)
		{
			if(o2 == null)
				return true;
			else
				return false;
		}
		else if(o2 == null)
			return false;
		else
			return o1.equals(o2);
	} //}}}

	//{{{ charsToEntities() method
	/**
	 * Converts &lt;, &gt;, &amp; in the string to their HTML entity
	 * equivalents.
	 * @param str The string
	 * @since jEdit 4.2pre1
	 */
	public static String charsToEntities(String str)
	{
		StringBuffer buf = new StringBuffer(str.length());
		for(int i = 0; i < str.length(); i++)
		{
			char ch = str.charAt(i);
			switch(ch)
			{
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '&':
				buf.append("&amp;");
				break;
			default:
				buf.append(ch);
				break;
			}
		}
		return buf.toString();
	} //}}}

	//{{{ formatFileSize() method
	public static final DecimalFormat KB_FORMAT = new DecimalFormat("#.# KB");
	public static final DecimalFormat MB_FORMAT = new DecimalFormat("#.# MB");

	/**
	 * Formats the given file size into a nice string (123 bytes, 10.6 KB,
	 * 1.2 MB).
	 * @param length The size
	 * @since jEdit 4.2pre1
	 */
	public static String formatFileSize(long length)
	{
		if(length < 1024)
			return length + " bytes";
		else if(length < 1024*1024)
			return KB_FORMAT.format((double)length / 1024);
		else
			return MB_FORMAT.format((double)length / 1024 / 1024);
	} //}}}

	//{{{ getLongestPrefix() method
	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(List str, boolean ignoreCase)
	{
		if(str.size() == 0)
			return "";

		int prefixLength = 0;

loop:		for(;;)
		{
			String s = str.get(0).toString();
			if(prefixLength >= s.length())
				break loop;
			char ch = s.charAt(prefixLength);
			for(int i = 1; i < str.size(); i++)
			{
				s = str.get(i).toString();
				if(prefixLength >= s.length())
					break loop;
				if(!compareChars(s.charAt(prefixLength),ch,ignoreCase))
					break loop;
			}
			prefixLength++;
		}

		return str.get(0).toString().substring(0,prefixLength);
	} //}}}

	//{{{ getLongestPrefix() method
	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(String[] str, boolean ignoreCase)
	{
		return getLongestPrefix((Object[])str,ignoreCase);
	} //}}}

	//{{{ getLongestPrefix() method
	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings (calls <code>toString()</code> on each object)
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre6
	 */
	public static String getLongestPrefix(Object[] str, boolean ignoreCase)
	{
		if(str.length == 0)
			return "";

		int prefixLength = 0;

		String first = str[0].toString();

loop:		for(;;)
		{
			if(prefixLength >= first.length())
				break loop;
			char ch = first.charAt(prefixLength);
			for(int i = 1; i < str.length; i++)
			{
				String s = str[i].toString();
				if(prefixLength >= s.length())
					break loop;
				if(!compareChars(s.charAt(prefixLength),ch,ignoreCase))
					break loop;
			}
			prefixLength++;
		}

		return first.substring(0,prefixLength);
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
	/**
	 * Compares menu item labels.
	 */
	public static class MenuItemCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			boolean obj1E, obj2E;
			obj1E = obj1 instanceof EnhancedMenuItem;
			obj2E = obj2 instanceof EnhancedMenuItem;
            if(obj1E && !obj2E)
                  return 1;
            else if(obj2E && !obj1E)
                  return -1;
			else
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

		return major + "." + minor
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
		PluginJAR jar = jEdit.getPluginJAR(toolsPath);
		if(jar == null)
		{
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- adding " + toolsPath + " to jEdit plugins.");
			jEdit.addPluginJAR(toolsPath);
		}
		else
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- has been loaded before.");
		//}}}

		return true;
	} //}}}

	//{{{ parsePermissions() method
	/**
	 * Parse a Unix-style permission string (rwxrwxrwx).
	 * @param s The string (must be 9 characters long).
	 * @since jEdit 4.1pre8
	 */
	public static int parsePermissions(String s)
	{
		int permissions = 0;

		if(s.length() == 9)
		{
			if(s.charAt(0) == 'r')
				permissions += 0400;
			if(s.charAt(1) == 'w')
				permissions += 0200;
			if(s.charAt(2) == 'x')
				permissions += 0100;
			else if(s.charAt(2) == 's')
				permissions += 04100;
			else if(s.charAt(2) == 'S')
				permissions += 04000;
			if(s.charAt(3) == 'r')
				permissions += 040;
			if(s.charAt(4) == 'w')
				permissions += 020;
			if(s.charAt(5) == 'x')
				permissions += 010;
			else if(s.charAt(5) == 's')
				permissions += 02010;
			else if(s.charAt(5) == 'S')
				permissions += 02000;
			if(s.charAt(6) == 'r')
				permissions += 04;
			if(s.charAt(7) == 'w')
				permissions += 02;
			if(s.charAt(8) == 'x')
				permissions += 01;
			else if(s.charAt(8) == 't')
				permissions += 01001;
			else if(s.charAt(8) == 'T')
				permissions += 01000;
		}

		return permissions;
	} //}}}

	//{{{ getEncodings() method
	/**
	 * Returns a list of supported character encodings.
	 * @since jEdit 4.2pre5
	 */
	public static String[] getEncodings()
	{
		List returnValue = new ArrayList();

		Map map = Charset.availableCharsets();
		Iterator iter = map.keySet().iterator();

		returnValue.add(UTF_8_Y);

		while(iter.hasNext())
			returnValue.add(iter.next());

		return (String[])returnValue.toArray(
			new String[returnValue.size()]);
	} //}}}

	//{{{ throwableToString() method
	/**
	 * Returns a string containing the stack trace of the given throwable.
	 * @since jEdit 4.2pre6
	 */
	public static String throwableToString(Throwable t)
	{
		StringWriter s = new StringWriter();
		t.printStackTrace(new PrintWriter(s));
		return s.toString();
	} //}}}

	//{{{ Private members
	private MiscUtilities() {}

	//{{{ compareChars()
	/** should this be public? */
	private static boolean compareChars(char ch1, char ch2, boolean ignoreCase)
	{
		if(ignoreCase)
			return Character.toUpperCase(ch1) == Character.toUpperCase(ch2);
		else
			return ch1 == ch2;
	} //}}}

	//{{{ getPathStart()
	private static int getPathStart(String path)
	{
		int start = 0;
		if(path.startsWith("/"))
			return 1;
		else if(OperatingSystem.isDOSDerived()
			&& path.length() >= 3
			&& path.charAt(1) == ':'
			&& (path.charAt(2) == '/'
			|| path.charAt(2) == '\\'))
			return 3;
		else
			return 0;
	} //}}}

	//}}}
}
