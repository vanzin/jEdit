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

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
//}}}

/**
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre10
 */
public class ModeProvider
{
	public static final ModeProvider instance = new ModeProvider();

	private List<Mode> modes = new ArrayList<Mode>(160);

	//{{{ ModeProvider constructor
	private ModeProvider()
	{
	} //}}}

	//{{{ removeAll() method
	public void removeAll()
	{
		modes = new ArrayList<Mode>(160);
	} //}}}

	//{{{ getMode() method
	/**
	 * Returns the edit mode with the specified name.
	 * @param name The edit mode
	 * @since jEdit 4.3pre10
	 */
	public Mode getMode(String name)
	{
		for(int i = 0; i < modes.size(); i++)
		{
			Mode mode = modes.get(i);
			if(mode.getName().equals(name))
				return mode;
		}
		return null;
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
		String nogzName = filename.substring(0,filename.length() -
			(filename.endsWith(".gz") ? 3 : 0));
		Mode[] modes = getModes();

		// this must be in reverse order so that modes from the user
		// catalog get checked first!
		for(int i = modes.length - 1; i >= 0; i--)
		{
			if(modes[i].accept(nogzName,firstLine))
			{
				return modes[i];
			}
		}
		return null;
	} //}}}

	
	//{{{ getModes() method
	/**
	 * Returns an array of installed edit modes.
	 * @since jEdit 4.3pre10
	 */
	public Mode[] getModes()
	{
		Mode[] array = new Mode[modes.size()];
		modes.toArray(array);
		return array;
	} //}}}

	//{{{ addMode() method
	/**
	 * Do not call this method. It is only public so that classes
	 * in the org.gjt.sp.jedit.syntax package can access it.
	 * @since jEdit 4.3pre10
	 * @param mode The edit mode
	 */
	public void addMode(Mode mode)
	{
		modes.add(mode);
	} //}}}

	//{{{ loadMode() method
	public void loadMode(Mode mode, XModeHandler xmh)
	{
		String fileName = (String)mode.getProperty("file");

		Log.log(Log.NOTICE,jEdit.class,"Loading edit mode " + fileName);

		XMLReader parser = null;
		try {
			parser = XMLReaderFactory.createXMLReader();
		} catch (SAXException saxe) {
			Log.log(Log.ERROR, jEdit.class, saxe);
			return;
		}
		mode.setTokenMarker(xmh.getTokenMarker());

		InputStream grammar = null;

		try
		{
			grammar = new BufferedInputStream(
				new FileInputStream(fileName));

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
			Log.log(Log.ERROR, jEdit.class, e);

			if (e instanceof SAXParseException)
			{
				String message = e.getMessage();
				int line = ((SAXParseException)e).getLineNumber();
				int col = ((SAXParseException)e).getColumnNumber();

				Object[] args = { fileName, line, col, message };
				GUIUtilities.error(null,"xmode-error",args);
			}
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
			public void error(String what, Object subst)
			{
				Log.log(Log.ERROR, this, subst);
			}

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

}
