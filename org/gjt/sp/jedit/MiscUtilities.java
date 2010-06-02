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
import java.nio.charset.MalformedInputException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.IOUtilities;

import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.jedit.menu.MenuItemTextComparator;
import org.gjt.sp.jedit.buffer.JEditBuffer;
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
	 * @deprecated
	 *   Extended encodings are now supported as services.
	 *   This value is no longer used.
	 */
	@Deprecated public static final String UTF_8_Y = "UTF-8Y";

	//{{{ Path name methods

	//{{{ canonPath() method
	/**
	 * @return the canonical form of the specified path name. Currently
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

				if (path.charAt(trim - 1) == '\\')
					while (trim > 1 && path.charAt(trim - 2) == '\\')
					{
						trim--;
					}
				path = path.substring(0,trim);
		}

		if(path.startsWith('~' + File.separator))
		{
			path = path.substring(2);
			String home = System.getProperty("user.home");

			if(home.endsWith(File.separator))
				return home + path;
			else
				return home + File.separator + path;
		}
		else if("~".equals(path))
			return System.getProperty("user.home");
		else if ("-".equals(path))
			return getParentOfPath(jEdit.getActiveView().getBuffer().getPath());
		else
			return path;
	} //}}}

	//{{{ expandVariables() method
	static final String varPatternString = "(\\$([a-zA-Z0-9_]+))";
	static final String varPatternString2 = "(\\$\\{([^}]+)\\})";
	static final Pattern varPattern = Pattern.compile(varPatternString);
	static final Pattern varPattern2 = Pattern.compile(varPatternString2);

	/** Accepts a string from the user which may contain variables of various syntaxes.
	 *  The goal is to support the following:
	 *     $varname
	 *     ${varname}
	 *     And expand each of these by looking at the system environment variables for possible
	 *     expansions.
	 *     @return a string which is either the unchanged input string, or one with expanded variables.
	 *     @since 4.3pre7
	 *     @author ezust
	 */
	public static String expandVariables(String arg)
	{
		Pattern p = varPattern;
		Matcher m = p.matcher(arg);
		if (!m.find())
		{
			p = varPattern2;
			m = p.matcher(arg);
			if (!m.find()) // no variables to substitute
				return arg;
		}
		String varName = m.group(2);
		String expansion = System.getenv(varName);
		if (expansion == null)
		{ // try everything uppercase?
			varName = varName.toUpperCase();
			String uparg = arg.toUpperCase();
			m = p.matcher(uparg);
			expansion = System.getenv(varName);
		}
		if (expansion != null)
		{
			expansion = expansion.replace("\\", "\\\\");
			return m.replaceFirst(expansion);
		}
		return arg;
	} //}}}

	//{{{ abbreviate() method
	/** returns an abbreviated path, replacing
	 *  values with variables, if a prefix exists.
	 *  @since jEdit 4.3pre16
	 */
	public static String abbreviate(String path)
	{
		if (svc == null)
			svc = new VarCompressor();
		return svc.compress(path);
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
		else if(path.startsWith("~/") || path.startsWith('~' + File.separator) || "~".equals(path))
			return true;
		else if ("-".equals(path))
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

	//{{{ constructPath() methods
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

		if (parent == null)
			parent = System.getProperty("user.dir");

		if (path == null || path.length() == 0)
			return parent;

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
		String d = '.' + File.separator;

		for(;;)
		{
			if(".".equals(path))
				return parent;
			else if("..".equals(path))
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
		if(path.length() == 0)
			return parent;

		if(OperatingSystem.isDOSDerived()
			&& !isURL(parent)
		&& path.charAt(0) == '\\')
			parent = parent.substring(0,2);

		VFS vfs = VFSManager.getVFSForPath(parent);

		return canonPath(vfs.constructPath(parent,path));
	}

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
	 * @param parent the parent path
	 * @param path the path to append to the parent
	 */
	public static String concatPath(String parent, String path)
	{
		parent = canonPath(parent);
		path = canonPath(path);

		// Make all child paths relative.
		if (path.startsWith(File.separator))
			path = path.substring(1);
		else if (path.length() >= 3 && path.charAt(1) == ':')
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
		int index = Math.max(
			path.lastIndexOf('/'), path.lastIndexOf(File.separatorChar));
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
		int index = path.lastIndexOf('.');
		// there could be a dot in the path and no file extension
		if(index == -1 || index < fsIndex )
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
	@Deprecated
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
	@Deprecated
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

	//{{{ saveBackup() methods
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
	}

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
				Log.log(Log.DEBUG,MiscUtilities.class,
					"Saving backup of file \"" +
					file.getAbsolutePath() + "\" to \"" +
					backupFile.getAbsolutePath() + '"');
				backupFile.delete();
				if (!file.renameTo(backupFile))
					IOUtilities.moveFile(file, backupFile);
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
				+ '1' + backupSuffix);
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

					backup.renameTo(new File(backupDirectory,
						backupPrefix + name
						+ backupSuffix + (i + 1)
						+ backupSuffix));
				}

				File backupFile = new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ '1' + backupSuffix);
				Log.log(Log.DEBUG,MiscUtilities.class,
					"Saving backup of file \"" +
					file.getAbsolutePath() + "\" to \"" +
					backupFile.getAbsolutePath() + '"');
				if (!file.renameTo(backupFile))
					IOUtilities.moveFile(file, backupFile);
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
	 * @deprecated use {@link org.gjt.sp.util.IOUtilities#moveFile(java.io.File, java.io.File)}
	 */
	@Deprecated
	public static boolean moveFile(File source, File dest)
	{
		return IOUtilities.moveFile(source, dest);
	} //}}}

	//{{{ copyStream() methods
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
	 * @deprecated use {@link IOUtilities#copyStream(int, org.gjt.sp.util.ProgressObserver, java.io.InputStream, java.io.OutputStream, boolean)}
	 */
	@Deprecated
	public static boolean copyStream(int bufferSize, ProgressObserver progress,
		InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return IOUtilities.copyStream(bufferSize, progress, in, out, canStop);
	}

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
	 * @deprecated use {@link IOUtilities#copyStream(org.gjt.sp.util.ProgressObserver, java.io.InputStream, java.io.OutputStream, boolean)}
	 */
	@Deprecated
	public static boolean copyStream(ProgressObserver progress, InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return IOUtilities.copyStream(4096,progress, in, out, canStop);
	} //}}}

	//{{{ isBinary() methods
	/**
	 * Check if a Reader is binary.
	 * @deprecated
	 *   Use isBinary(InputStream) instead.
	 */
	@Deprecated
	public static boolean isBinary(Reader reader) throws IOException
	{
		return containsNullCharacter(reader);
	}

	/**
	 * Check if an InputStream is binary.
	 * First this tries encoding auto detection. If an encoding is
	 * detected, the stream should be a text stream. Otherwise, this
	 * will check the first characters 100
	 * (jEdit property vfs.binaryCheck.length) in the system default
	 * encoding. If more than 1 (jEdit property vfs.binaryCheck.count)
	 * NUL(\u0000) was found, the stream is declared binary.
	 *
	 * This is not 100% because sometimes the autodetection could fail.
	 *
	 * This method will not close the stream. You have to do it yourself
	 *
	 * @param in the stream
	 * @return <code>true</code> if the stream was detected as binary
	 * @throws IOException IOException If an I/O error occurs
	 * @since jEdit 4.3pre10
	 */
	public static boolean isBinary(InputStream in) throws IOException
	{
		AutoDetection.Result detection = new AutoDetection.Result(in);
		// If an encoding is detected, this is a text stream
		if (detection.getDetectedEncoding() != null)
		{
			return false;
		}
		// Read the stream in system default encoding. The encoding
		// might be wrong. But enough for binary detection.
		try
		{
			return containsNullCharacter(
				new InputStreamReader(detection.getRewindedStream()));
		}
		catch (MalformedInputException mie)
		{
			// This error probably means the input is binary.
			return true;
		}
	} //}}}

	//{{{ isBackup() method
	/**
	 * Check if the filename is a backup file.
	 * @param filename the filename to check
	 * @return true if this is a backup file.
	 * @since jEdit 4.3pre5
	 */
	public static boolean isBackup(String filename)
	{
		if (filename.startsWith("#")) return true;
		if (filename.endsWith("~")) return true;
		if (filename.endsWith(".bak")) return true;
		return false;
	} //}}}


	//{{{ autodetect() method
	/**
	 * Tries to detect if the stream is gzipped, and if it has an encoding
	 * specified with an XML PI.
	 *
	 * @param in the input stream reader that must be autodetected
	 * @param buffer a buffer. It can be null if you only want to autodetect the encoding of a file
	 * @return a Reader using the detected encoding
	 * @throws IOException io exception during read
	 * @since jEdit 4.3pre5
	 */
	public static Reader autodetect(InputStream in, Buffer buffer) throws IOException
	{
		String encoding;
		if (buffer == null)
			encoding = System.getProperty("file.encoding");
		else
			encoding = buffer.getStringProperty(JEditBuffer.ENCODING);
		boolean gzipped = false;

		if (buffer == null || buffer.getBooleanProperty(Buffer.ENCODING_AUTODETECT))
		{
			AutoDetection.Result detection = new AutoDetection.Result(in);
			gzipped = detection.streamIsGzipped();
			if (gzipped)
			{
				Log.log(Log.DEBUG, MiscUtilities.class
					, "Stream is Gzipped");
			}
			String detected = detection.getDetectedEncoding();
			if (detected != null)
			{
				encoding = detected;
				Log.log(Log.DEBUG, MiscUtilities.class
					, "Stream encoding detected is " + detected);
			}
			in = detection.getRewindedStream();
		}
		else
		{
			// Make the stream buffered in the same way.
			in = AutoDetection.getMarkedStream(in);
		}

		Reader result = EncodingServer.getTextReader(in, encoding);
		if (buffer != null)
		{
			// Store the successful properties.
			if (gzipped)
			{
				buffer.setBooleanProperty(Buffer.GZIPPED,true);
			}
			buffer.setProperty(JEditBuffer.ENCODING, encoding);
		}
		return result;
	} //}}}

	//{{{ closeQuietly() method
	/**
	 * Method that will close an {@link InputStream} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param in the InputStream to close.
	 * @since jEdit 4.3pre3
	 * @deprecated use {@link IOUtilities#closeQuietly(java.io.InputStream)}
	 */
	@Deprecated
	public static void closeQuietly(InputStream in)
	{
		IOUtilities.closeQuietly(in);
	} //}}}

	//{{{ copyStream() method
	/**
	 * Method that will close an {@link OutputStream} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param out the OutputStream to close.
	 * @since jEdit 4.3pre3
	 * @deprecated use {@link IOUtilities#closeQuietly(java.io.OutputStream)}
	 */
	@Deprecated
	public static void closeQuietly(OutputStream out)
	{
		IOUtilities.closeQuietly(out);
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
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#getLeadingWhiteSpace(String)}
	 */
	@Deprecated
	public static int getLeadingWhiteSpace(String str)
	{
		return StandardUtilities.getLeadingWhiteSpace(str);
	} //}}}

	//{{{ getTrailingWhiteSpace() method
	/**
	 * Returns the number of trailing whitespace characters in the
	 * specified string.
	 * @param str The string
	 * @since jEdit 2.5pre5
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#getTrailingWhiteSpace(String)}
	 */
	@Deprecated
	public static int getTrailingWhiteSpace(String str)
	{
		return StandardUtilities.getTrailingWhiteSpace(str);
	} //}}}

	//{{{ getLeadingWhiteSpaceWidth() method
	/**
	 * Returns the width of the leading white space in the specified
	 * string.
	 * @param str The string
	 * @param tabSize The tab size
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#getLeadingWhiteSpace(String)}
	 */
	@Deprecated
	public static int getLeadingWhiteSpaceWidth(String str, int tabSize)
	{
		return StandardUtilities.getLeadingWhiteSpaceWidth(str, tabSize);
	} //}}}

	//{{{ getVirtualWidth() method
	/**
	 * Returns the virtual column number (taking tabs into account) of the
	 * specified offset in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 * @since jEdit 4.1pre1
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#getVirtualWidth(javax.swing.text.Segment, int)}
	 */
	@Deprecated
	public static int getVirtualWidth(Segment seg, int tabSize)
	{
		return StandardUtilities.getVirtualWidth(seg, tabSize);
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
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#getVirtualWidth(javax.swing.text.Segment, int)}
	 */
	@Deprecated
	public static int getOffsetOfVirtualColumn(Segment seg, int tabSize,
					    int column, int[] totalVirtualWidth)
	{
		return StandardUtilities.getOffsetOfVirtualColumn(seg, tabSize, column, totalVirtualWidth);
	} //}}}

	//{{{ createWhiteSpace() methods
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
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#createWhiteSpace(int, int)}
	 */
	@Deprecated
	public static String createWhiteSpace(int len, int tabSize)
	{
		return StandardUtilities.createWhiteSpace(len,tabSize,0);
	}

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
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#createWhiteSpace(int, int, int)}
	 */
	@Deprecated
	public static String createWhiteSpace(int len, int tabSize, int start)
	{
		return StandardUtilities.createWhiteSpace(len, tabSize, start);
	} //}}}

	//{{{ globToRE() method
	/**
	 * Converts a Unix-style glob to a regular expression.<p>
	 *
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 * @deprecated Use {@link org.gjt.sp.util.StandardUtilities#globToRE(String)}.
	 */
	@Deprecated
	public static String globToRE(String glob)
	{
		return StandardUtilities.globToRE(glob);
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
		StringBuilder buf = new StringBuilder();
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

	//{{{ charsToEscapes() methods
	/**
	 * Escapes newlines, tabs, backslashes, and quotes in the specified
	 * string.
	 * @param str The string
	 * @since jEdit 2.3pre1
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#charsToEscapes(String)}
	 */
	@Deprecated
	public static String charsToEscapes(String str)
	{
		return StandardUtilities.charsToEscapes(str);
	}

	/**
	 * Escapes the specified characters in the specified string.
	 * @param str The string
	 * @param toEscape Any characters that require escaping
	 * @since jEdit 4.1pre3
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#charsToEscapes(String)}
	 */
	@Deprecated
	public static String charsToEscapes(String str, String toEscape)
	{
		return StandardUtilities.charsToEscapes(str, toEscape);
	} //}}}

	//{{{ compareVersions() method
	/**
	 * @deprecated Call {@link StandardUtilities#compareStrings(String, String, boolean)} instead
	 */
	@Deprecated
	public static int compareVersions(String v1, String v2)
	{
		return StandardUtilities.compareStrings(v1,v2,false);
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
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#compareStrings(String, String, boolean)}
	 */
	@Deprecated
	public static int compareStrings(String str1, String str2, boolean ignoreCase)
	{
		return StandardUtilities.compareStrings(str1, str2, ignoreCase);
	} //}}}

	//{{{ stringsEqual() method
	/**
	 * @deprecated Call {@link StandardUtilities#objectsEqual(Object, Object)} instead.
	 */
	@Deprecated
	public static boolean stringsEqual(String s1, String s2)
	{
		return StandardUtilities.objectsEqual(s1,s2);
	} //}}}

	//{{{ objectsEqual() method
	/**
	 * Returns if two strings are equal. This correctly handles null pointers,
	 * as opposed to calling <code>o1.equals(o2)</code>.
	 * @since jEdit 4.2pre1
	 * @deprecated use {@link StandardUtilities#objectsEqual(Object, Object)}
	 */
	@Deprecated
	public static boolean objectsEqual(Object o1, Object o2)
	{
		return StandardUtilities.objectsEqual(o1, o2);
	} //}}}

	//{{{ charsToEntities() method
	/**
	 * Converts &lt;, &gt;, &amp; in the string to their HTML entity
	 * equivalents.
	 * @param str The string
	 * @since jEdit 4.2pre1
	 * @deprecated	Use {@link org.gjt.sp.util.XMLUtilities#charsToEntities(String, boolean)}.
	 */
	@Deprecated
	public static String charsToEntities(String str)
	{
		return XMLUtilities.charsToEntities(str,false);
	} //}}}

	//{{{ formatFileSize() method
	@Deprecated
	public static final DecimalFormat KB_FORMAT = new DecimalFormat("#.# kB");
	@Deprecated
	public static final DecimalFormat MB_FORMAT = new DecimalFormat("#.# MB");

	/**
	 * Formats the given file size into a nice string (123 Bytes, 10.6 kB,
	 * 1.2 MB).
	 * @param length The size
	 * @since jEdit 4.2pre1
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities#formatFileSize(long)} 
	 */
	@Deprecated
	public static String formatFileSize(long length)
	{
		return StandardUtilities.formatFileSize(length);
	} //}}}

	//{{{ getLongestPrefix() methods
	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(List<String> str, boolean ignoreCase)
	{
		if(str.isEmpty())
			return "";

		int prefixLength = 0;

loop:		for(;;)
		{
			String s = str.get(0);
			if(prefixLength >= s.length())
				break loop;
			char ch = s.charAt(prefixLength);
			for(int i = 1; i < str.size(); i++)
			{
				s = str.get(i);
				if(prefixLength >= s.length())
					break loop;
				if(!compareChars(s.charAt(prefixLength),ch,ignoreCase))
					break loop;
			}
			prefixLength++;
		}

		return str.get(0).substring(0,prefixLength);
	}

	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(String[] str, boolean ignoreCase)
	{
		return getLongestPrefix((Object[])str,ignoreCase);
	}

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

	//{{{ quicksort() deprecated methods
	/**
	 * Sorts the specified array. Equivalent to calling
	 * <code>Arrays.sort()</code>.
	 * @param obj The array
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 * @deprecated use <code>Arrays.sort()</code>
	 */
	@Deprecated
	public static void quicksort(Object[] obj, Comparator compare)
	{
		Arrays.sort(obj,compare);
	}


	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 * @deprecated <code>Collections.sort()</code>
	 */
	@Deprecated
	public static void quicksort(Vector vector, Comparator compare)
	{
		Collections.sort(vector,compare);
	}

	/**
	 * Sorts the specified list.
	 * @param list The list
	 * @param compare Compares the objects
	 * @since jEdit 4.0pre4
	 * @deprecated <code>Collections.sort()</code>
	 */
	@Deprecated
	public static void quicksort(List list, Comparator compare)
	{
		Collections.sort(list,compare);
	}

	/**
	 * Sorts the specified array. Equivalent to calling
	 * <code>Arrays.sort()</code>.
	 * @param obj The array
	 * @param compare Compares the objects
	 * @deprecated use <code>Arrays.sort()</code>
	 */
	@Deprecated
	public static void quicksort(Object[] obj, Compare compare)
	{
		Arrays.sort(obj,compare);
	}

	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 * @deprecated <code>Collections.sort()</code>
	 */
	@Deprecated
	public static void quicksort(Vector vector, Compare compare)
	{
		Collections.sort(vector,compare);
	} //}}}

	//{{{ Compare deprecated interface
	/**
	 * An interface for comparing objects. This is a hold-over from
	 * they days when jEdit had its own sorting API due to JDK 1.1
	 * compatibility requirements. Use <code>java.util.Comparator</code>
	 * instead.
	 * @deprecated
	 */
	@Deprecated
	public interface Compare extends Comparator
	{
		int compare(Object obj1, Object obj2);
	} //}}}

	//{{{ StringCompare deprecated class
	/**
	 * Compares strings.
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities.StringCompare}
	 */
	@Deprecated
	public static class StringCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return StandardUtilities.compareStrings(obj1.toString(),
				obj2.toString(),false);
		}
	} //}}}

	//{{{ StringICaseCompare deprecated class
	/**
	 * Compares strings ignoring case.
	 * @deprecated use {@link org.gjt.sp.util.StandardUtilities.StringCompare}
	 */
	@Deprecated
	public static class StringICaseCompare implements Comparator<Object>
	{
		public int compare(Object obj1, Object obj2)
		{
			return StandardUtilities.compareStrings(obj1.toString(), obj2.toString(), true);
		}
	} //}}}

	//{{{ MenuItemCompare deprecated class
	/**
	 * Compares menu item labels.
	 * @deprecated Replaced with {@link org.gjt.sp.jedit.menu.MenuItemTextComparator}
	 */
	@Deprecated
	public static class MenuItemCompare implements Compare
	{
		private MenuItemTextComparator comparator = new MenuItemTextComparator();

		public int compare(Object obj1, Object obj2)
		{
			return comparator.compare((JMenuItem)obj1, (JMenuItem)obj2);
		}
	} //}}}

	//{{{ buildToVersion() method
	/**
	 * Converts an internal version number (build) into a
	 * `human-readable' form.
	 * @param build The build
	 */
	public static String buildToVersion(String build)
	{
		if(build.length() != 11)
			return "<unknown version: " + build + '>';
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
			(bugfix != 0 ? "." + bugfix : ""));
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

		Collection<String> paths = new LinkedList<String>();

		//{{{ 1. Check whether tools.jar is in the system classpath:
		paths.add("System classpath: "
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
			paths.add(toolsPath);
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
			paths.add(toolsPath);
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
		paths.add(toolsPath);

		if(!new File(toolsPath).exists())
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

	//{{{ getEncodings() methods
	/**
	 * Returns a list of supported character encodings.
	 * @since jEdit 4.2pre5
	 * @deprecated See #getEncodings(boolean)
	 */
	@Deprecated
	public static String[] getEncodings()
	{
		return getEncodings(false);
	}

	/**
	 * Returns a list of supported character encodings.
	 * @since jEdit 4.3pre5
	 * @param getSelected Whether to return just the selected encodings or all.
	 */
	public static String[] getEncodings(boolean getSelected)
	{
		Set<String> set;
		if (getSelected)
		{
			set = EncodingServer.getSelectedNames();
		}
		else
		{
			set = EncodingServer.getAvailableNames();
		}
		return set.toArray(new String[set.size()]);
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

	//{{{ parseXML() method
	/**
	 * Convenience method for parsing an XML file.
	 *
	 * @return Whether any error occured during parsing.
	 * @since jEdit 4.3pre5
	 * @deprecated Use {@link XMLUtilities#parseXML(InputStream,DefaultHandler)}.
	 */
	@Deprecated
	public static boolean parseXML(InputStream in, DefaultHandler handler)
		throws IOException
	{
		return XMLUtilities.parseXML(in, handler);
	} //}}}

	//{{{ resolveEntity() method
	/**
	 * Tries to find the given systemId in the context of the given
	 * class.
	 *
	 * @deprecated Use {@link XMLUtilities#findEntity(String,String,Class)}.
	 */
	@Deprecated
	public static InputSource findEntity(String systemId, String test, Class where)
	{
		return XMLUtilities.findEntity(systemId, test, where);
	} //}}}

	//{{{ Private members
	private MiscUtilities() {}

	//{{{ compareChars() method
	/**
	 * Compares two chars.
	 * should this be public?
	 * @param ch1 the first char
	 * @param ch2 the second char
	 * @param ignoreCase true if you want to ignore case
	 */
	private static boolean compareChars(char ch1, char ch2, boolean ignoreCase)
	{
		if(ignoreCase)
			return Character.toUpperCase(ch1) == Character.toUpperCase(ch2);
		else
			return ch1 == ch2;
	} //}}}

	//{{{ getPathStart() method
	private static int getPathStart(String path)
	{
		if(path.startsWith("/"))
			return 0;
		else if(OperatingSystem.isDOSDerived()
			&& path.length() >= 3
			&& path.charAt(1) == ':'
			&& (path.charAt(2) == '/'
			|| path.charAt(2) == '\\'))
			return 3;
		else
			return 0;
	} //}}}

	//{{{ containsNullCharacter() method
	private static boolean containsNullCharacter(Reader reader)
		throws IOException
	{
		int nbChars = jEdit.getIntegerProperty("vfs.binaryCheck.length",100);
		int authorized = jEdit.getIntegerProperty("vfs.binaryCheck.count",1);
		for (long i = 0L;i < nbChars;i++)
		{
			int c = reader.read();
			if (c == -1)
				return false;
			if (c == 0)
			{
				authorized--;
				if (authorized == 0)
					return true;
			}
		}
		return false;
	} //}}}

	//}}}

	static VarCompressor svc = null;

	//{{{ VarCompressor class
	/**
	 * Singleton class for quickly "compressing" paths into variable-prefixed values.
	 * @author alan ezust
	 */
	static class VarCompressor
	{
		/** a reverse mapping of values to environment variable names */
		final Map<String, String> prefixMap = new HashMap<String, String>();
		/** previously compressed strings saved for quick access later */
		final Map<String, String> previous = new HashMap<String, String>();

		//{{{ VarCompressor constructor
		VarCompressor()
		{
			ProcessBuilder pb = new ProcessBuilder();
			Map<String, String> env = pb.environment();
			if (OperatingSystem.isUnix())
				prefixMap.put(System.getProperty("user.home"), "~");
			for (Map.Entry<String, String> entry: env.entrySet())
			{
				String k = entry.getKey();
				if (k.equalsIgnoreCase("pwd") || k.equalsIgnoreCase("oldpwd")) continue;
				if (!Character.isLetter(k.charAt(0))) continue;
				String v = entry.getValue();
				// only add possible candidates to the prefix map
				if (!canBePathPrefix(v)) continue;
				// no need for trailing file separator
				if (v.endsWith(File.separator))
					v = v.substring(0, v.length()-1);
				// check if it is actually shorter
				if (OperatingSystem.isWindows())
					if (k.length()+2 > v.length()) continue; // gets replaced by %FOO%
				else
					if (k.length()+1 > v.length()) continue; // gets replaced by $FOO
				if (OperatingSystem.isWindows())
				{
					// no case sensitivity, might as well convert to lower case
					v = v.toLowerCase();
					k = k.toLowerCase();
				}
				if (prefixMap.containsKey(v))
				{
					String otherKey = prefixMap.get(v);
					if (otherKey.length() < k.length()) continue;
				}
				prefixMap.put(v, k);
			}
		} //}}}

		//{{{ compress() method
		String compress(String path)
		{
			String original = path;
			if (previous.containsKey(path))
			{
				return previous.get(path);
			}
			String bestPrefix = "/";
			String verifiedPrefix = bestPrefix;
			for (String tryPrefix : prefixMap.keySet())
			{
				if (tryPrefix.length() < bestPrefix.length()) continue;
				if (OperatingSystem.isWindows() &&
				    path.toLowerCase().startsWith(tryPrefix))
					bestPrefix = tryPrefix;
				else if (path.startsWith(tryPrefix))
				{
					bestPrefix = tryPrefix;
				}
				// Only use prefix if it is a directory-prefix of the path
				if (!bestPrefix.equals(verifiedPrefix))
				{
					String remainder = original.substring(bestPrefix.length());
					if (remainder.length() < 1 || remainder.startsWith(File.separator))
						verifiedPrefix = bestPrefix;
					else bestPrefix = verifiedPrefix;
				}
			}
			if (bestPrefix.length() > 1)
			{
				String remainder = original.substring(bestPrefix.length());
				String envvar = prefixMap.get(bestPrefix);
				if (envvar.equals("~"))
					path = envvar + remainder;
				else if (OperatingSystem.isWindows())
					path = '%' + envvar.toUpperCase() + '%' + remainder;
				else
					path = '$' + envvar + remainder;
			}
			previous.put(original, path);
			return path;
		} //}}}

		//{{{ canBePathPrefix() method
		// Returns true if the argument may absolutely point a directory.
		// For speed, no access to file system or network should happen.
		private boolean canBePathPrefix(String s)
		{
			// Do not use File#isDirectory() since it causes
			// access to file system or network to check if
			// the directory is actually exists.
			return !s.contains(File.pathSeparator)
				&& new File(s).isAbsolute();
		} //}}}
	} //}}}

}
