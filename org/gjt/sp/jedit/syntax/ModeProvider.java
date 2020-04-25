/*
 * ModeProvider.java - An edit mode provider.
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
package org.gjt.sp.jedit.syntax;

//{{{ Imports
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.regex.*;
//}}}

/**
 * This class works like a singleton, the instance is initialized by jEdit.
 *
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre10
 */
public class ModeProvider
{
	public static ModeProvider instance = new ModeProvider();

	private final LinkedHashMap<String, Mode> modes = new LinkedHashMap<>(250);

	//{{{ removeAll() method
	public void removeAll()
	{
		modes.clear();
	} //}}}

	//{{{ removeMode() method
	/**
	 * Will only remove user modes.
	 * @return true if the mode was removed, false otherwise.
	 */
	public boolean removeMode(String name) throws IOException
	{
		Mode mode = modes.get(name);
		if (mode.isUserMode())
		{
			// check that this mode is in the map
			Mode oldMode = modes.remove(name);
			if (oldMode == null)
				return false;

			// delete mode file from disk and remove the entry from the catalog file.
			// Actually, just rename the mode file by adding "_unused" to the end of the file name
			// and comment out the line in the catalog file. This way it is possible to undo
			// these changes manually without too much work.
			String modeFilename = (String)mode.getProperty("file");
			File modeFile = new File(modeFilename);
			if (modeFile.exists())
			{
				Path path = FileSystems.getDefault().getPath(modeFilename);
				Files.move(path, path.resolveSibling(modeFilename + "_unused"), StandardCopyOption.REPLACE_EXISTING);
			}

			// The mode file may not be present and still referenced in the catalog, so carry on.
			// delete entry from mode catalog, catalog is in the same directory as the mode file
			File catalogFile = new File(modeFile.getParent(),"catalog");
			if (catalogFile.exists())
			{
				StringBuilder contents = new StringBuilder();
				try
				{
					// read in the catalog file
					BufferedReader br = new BufferedReader(new FileReader(catalogFile));
					String line = null;
					while((line = br.readLine()) != null)
					{
						contents.append(line).append('\n');
					}
					br.close();
				}
				catch(IOException ioe)
				{
					// unable to read the catalog file
					modes.put(oldMode.getName(), oldMode);
					throw ioe;
				}

				if (contents.length() == 0)
				{
					// empty catalog file, how did that happen?
					modes.put(oldMode.getName(), oldMode);
					return false;
				}

				// remove the catalog entry for this mode
				Pattern p = Pattern.compile("(?m)(^\\s*[<]MODE.*?NAME=\"" + name + "\".*?[>])");
				Matcher m = p.matcher(contents);
				String newContents = m.replaceFirst("<!--$1-->");

				try
				{
					// rewrite the catalog file
					BufferedWriter bw = new BufferedWriter(new FileWriter(catalogFile));
					bw.write(newContents, 0, newContents.length());
					bw.flush();
					bw.close();
				}
				catch(IOException ioe)
				{
					// unable to write the catalog file
					modes.put(oldMode.getName(), oldMode);
					throw ioe;
				}
			}
		}
		return true;
	} //}}}

	//{{{ getMode() method
	/**
	 * Returns the edit mode with the specified name.
	 * @param name The edit mode
	 * @since jEdit 4.3pre10
	 */
	public Mode getMode(String name)
	{
		return modes.get(name);
	} //}}}

	//{{{ getModeForFile() method
	/**
	 * Get the appropriate mode that must be used for the file
	 * @param filename the filename
	 * @param firstLine the first line of the file
	 * @return the edit mode, or null if no mode match the file
	 * @since jEdit 4.3pre12
	 */
	public Mode getModeForFile(String filename, String firstLine)
	{
		return getModeForFile(null, filename, firstLine);
	} //}}}

	//{{{ getModeForFile() method
	/**
	 * Get the appropriate mode that must be used for the file
	 * @param filepath the filepath, can be {@code null}
	 * @param filename the filename, can be {@code null}
	 * @param firstLine the first line of the file
	 * @return the edit mode, or null if no mode match the file
	 * @since jEdit 4.5pre1
	 */
	public Mode getModeForFile(String filepath, String filename, String firstLine)
	{
		if (filepath != null && filepath.endsWith(".gz"))
			filepath = filepath.substring(0, filepath.length() - 3);
		if (filename != null && filename.endsWith(".gz"))
			filename = filename.substring(0, filename.length() - 3);

		List<Mode> acceptable = new ArrayList<>(1);
		for(Mode mode : modes.values())
		{
			if(mode.accept(filepath, filename, firstLine))
			{
				acceptable.add(mode);
			}
		}
		if (acceptable.size() == 1)
		{
			return acceptable.get(0);
		}
		if (acceptable.size() > 1)
		{
			// The check should be in reverse order so that
			// modes from the user catalog get checked first!
			Collections.reverse(acceptable);

			// the very most acceptable mode is one whose file
			// name doesn't only match the file name as regular
			// expression but which is identical
			for (Mode mode : acceptable)
			{
				if (mode.acceptIdentical(filepath, filename))
				{
					return mode;
				}
			}

			// most acceptable is a mode that matches both the
			// filepath and the first line glob
			for (Mode mode : acceptable)
			{
				if (mode.acceptFile(filepath, filename) &&
					mode.acceptFirstLine(firstLine))
				{
					return mode;
				}
			}
			// next best is filepath match, there could be multiple matches,
			// need to choose the best one
			List<Mode> filepathMatch = new ArrayList<Mode>();
			for (Mode mode : acceptable)
			{
				if (mode.acceptFile(filepath, filename))
				{
					filepathMatch.add(mode);
				}
			}
			if (filepathMatch.size() == 1)
			{
				return filepathMatch.get(0);
			}
			else if (filepathMatch.size() > 1)
			{
				// return the one with the longest glob pattern since that one
				// is most likely to be more specific and hence the best choice
				Mode longest = filepathMatch.get(0);
				for (Mode mode : filepathMatch)
				{
					if (((String)mode.getProperty("filenameGlob")).length() > ((String)longest.getProperty("filenameGlob")).length())
					{
						longest = mode;
					}
				}
				return longest;
			}
			// all acceptable choices are by first line glob, and
			// they all match, so just return the first one.
			return acceptable.get(0);
		}
		// no matching mode found for this file
		return null;
	} //}}}

	//{{{ getModes() method
	/**
	 * Returns an array of installed edit modes.
	 * @since jEdit 4.3pre10
	 */
	public Mode[] getModes()
	{
		return modes.values().toArray(new Mode[0]);
	} //}}}

	//{{{ addMode() method
	/**
	 * Do not call this method. It is only public so that classes
	 * in the org.gjt.sp.jedit.syntax package can access it.
	 * @since jEdit 4.3pre10
	 * @see org.gjt.sp.jedit.jEdit#reloadModes reloadModes
	 * @param mode The edit mode
	 */
	public void addMode(Mode mode)
	{
		String name = mode.getName();

		// The removal makes the "insertion order" in modes
		// (LinkedHashMap) follow the order of addMode() calls.
		modes.remove(name);

		modes.put(name, mode);
	} //}}}

	//{{{ addUserMode() method
	/**
	 * Do not call this method. It is only public so that classes
	 * in the org.gjt.sp.jedit.syntax package can access it.
	 * @since jEdit 4.3pre10
	 * @see org.gjt.sp.jedit.jEdit#reloadModes reloadModes
	 * @param mode The edit mode
	 */
	public void addUserMode(Mode mode, Path target) throws IOException
	{
		mode.setUserMode(true);
		String name = mode.getName();
		String modeFile = (String)mode.getProperty("file");
		String filenameGlob = (String)mode.getProperty("filenameGlob");
		String firstLineGlob = (String)mode.getProperty("firstlineGlob");

		// copy mode file to user mode directory
		Path source = FileSystems.getDefault().getPath(modeFile);
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		// add entry to mode catalog, catalog is in the same directory as the mode file
		File catalogFile = new File(target.toFile().getParent(),"catalog");
		if (catalogFile.exists())
		{
			try
			{
				// read in the catalog file
				BufferedReader br = new BufferedReader(new FileReader(catalogFile));
				String line = null;
				StringBuilder contents = new StringBuilder();
				while((line = br.readLine()) != null)
				{
					contents.append(line).append('\n');
				}
				br.close();

				// remove any existing catalog entry for this mode
				Pattern p = Pattern.compile("(?m)(^\\s*[<]MODE.*?NAME=\"" + name + "\".*?[>])");
				Matcher m = p.matcher(contents);
				String newContents = m.replaceFirst("<!--$1-->");

				// insert the catalog entry for this mode
				p = Pattern.compile("(?m)(</MODES>)");
				m = p.matcher(contents);
				String modeLine = "\t<MODE NAME=\"" + name + "\" FILE=\"" + target.toFile().getName() + "\"" +
					(filenameGlob == null || filenameGlob.isEmpty() ? "" : " FILE_NAME_GLOB=\"" + filenameGlob + "\"") +
					(firstLineGlob == null || firstLineGlob.isEmpty() ? "" : " FIRST_LINE_GLOB=\"" + firstLineGlob + "\"") +
					"/>";
				newContents = m.replaceFirst(modeLine + "\n$1" );

				// rewrite the catalog file
				BufferedWriter bw = new BufferedWriter(new FileWriter(catalogFile));
				bw.write(newContents, 0, newContents.length());
				bw.flush();
				bw.close();
			}
			catch(Exception e)	// NOPMD
			{
				// ignored
			}
		}

		addMode(mode);
		loadMode(mode);
	} //}}}

	//{{{ loadMode() method
	public void loadMode(Mode mode, XModeHandler xmh)
	{
		String fileName = (String)mode.getProperty("file");

		Log.log(Log.NOTICE,this,"Loading edit mode " + fileName);

		XMLReader parser;
		try
		{
			parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		}
		catch (SAXException | ParserConfigurationException saxe)
		{
			Log.log(Log.ERROR, this, saxe);
			return;
		}
		mode.setTokenMarker(xmh.getTokenMarker());

		InputStream grammar;

		try
		{
			grammar = new BufferedInputStream(
					new FileInputStream(fileName));
		}
		catch (FileNotFoundException e1)
		{
			InputStream resource = ModeProvider.class.getResourceAsStream(fileName);
			if (resource == null)
				error(fileName, e1);
			grammar = new BufferedInputStream(resource);
		}

		try
		{
			InputSource isrc = new InputSource(grammar);
			isrc.setSystemId("jedit.jar");
			parser.setContentHandler(xmh);
			parser.setDTDHandler(xmh);
			parser.setEntityResolver(xmh);
			parser.setErrorHandler(xmh);
			parser.parse(isrc);

			mode.setProperties(xmh.getModeProperties());
		}
		catch (Throwable e)	// NOPMD
		{
			error(fileName, e);
		}
		finally
		{
			IOUtilities.closeQuietly(grammar);
		}
	} //}}}

	//{{{ loadMode() method
	public void loadMode(Mode mode)
	{
		XModeHandler xmh = new XModeHandler(mode.getName())
		{
			@Override
			public void error(String what, Object subst)
			{
				Log.log(Log.ERROR, this, subst);
			}

			@Override
			public TokenMarker getTokenMarker(String modeName)
			{
				Mode mode = getMode(modeName);
				if(mode == null)
					return null;
				else
					return mode.getTokenMarker();
			}
		};
		loadMode(mode, xmh);
	} //}}}

	//{{{ error() method
	protected void error(String file, Throwable e)
	{
		Log.log(Log.ERROR, this, e);
	} //}}}
}
