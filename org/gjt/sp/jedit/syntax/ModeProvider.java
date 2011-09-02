/*
 * ModeProvider.java - An edit mode provider.
 * :tabSize=8:indentSize=8:noTabs=false:
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
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
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

	private LinkedHashMap<String, Mode> modes = new LinkedHashMap<String, Mode>(180);

	// any mode that is added that is already in the 'modes' map is added here. These
	// 'override' modes are modes loaded from outside of the standard issue catalog files.
	private LinkedHashMap<String, Mode> overrideModes = new LinkedHashMap<String, Mode>(20);

	//{{{ removeAll() method
	public void removeAll()
	{
		modes.clear();
		overrideModes.clear();
	} //}}}

	//{{{ getMode() method
	/**
	 * Returns the edit mode with the specified name.
	 * @param name The edit mode
	 * @since jEdit 4.3pre10
	 */
	public Mode getMode(String name)
	{
		Mode mode = overrideModes.get(name);
		if (mode == null)
		{
			mode = modes.get(name);
		}
		return mode;
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
		if ((filepath != null) && filepath.endsWith(".gz"))
			filepath = filepath.substring(0, filepath.length() - 3);
		if ((filename != null) && filename.endsWith(".gz"))
			filename = filename.substring(0, filename.length() - 3);

		List<Mode> acceptable = new ArrayList<Mode>();

		// First check overrideModes as these are user supplied modes.
		// User modes have priority.
		for(Mode mode : overrideModes.values())
		{
			if(mode.accept(filepath, filename, firstLine))
			{
				acceptable.add(mode);
			}
		}
		if (acceptable.size() == 0)
		{
			// no user modes were acceptable, so check standard modes.
			for(Mode mode : modes.values())
			{
				if(mode.accept(filepath, filename, firstLine))
				{
					acceptable.add(mode);
				}
			}
		}
		if (acceptable.size() == 1)
		{
			return acceptable.get(0);
		}
		if (acceptable.size() > 1)
		{
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
			// next best is filepath match
			for (Mode mode : acceptable)
			{
				if (mode.acceptFile(filepath, filename)) {
					return mode;
				}
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
		Mode[] array = new Mode[modes.size() + overrideModes.size()];
		Mode[] standard = modes.values().toArray(new Mode[0]);
		Mode[] override = overrideModes.values().toArray(new Mode[0]);
		System.arraycopy(standard, 0, array, 0, standard.length);
		System.arraycopy(override, 0, array, standard.length, override.length);
		return array;
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
		if (modes.get(mode.getName()) != null)
		{
			overrideModes.put(mode.getName(), mode);
			modes.remove(mode.getName());
		}
		else
		{
			modes.put(mode.getName(), mode);
		}
	} //}}}

	//{{{ loadMode() method
	public void loadMode(Mode mode, XModeHandler xmh)
	{
		String fileName = (String)mode.getProperty("file");

		Log.log(Log.NOTICE,this,"Loading edit mode " + fileName);

		XMLReader parser;
		try
		{
			parser = XMLReaderFactory.createXMLReader();
		} catch (SAXException saxe)
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
		catch (Throwable e)
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
