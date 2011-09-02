/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
 * Portions copyright (C) 2006, 2011 Matthieu Casanova
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

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.buffer.KillRing;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.IOUtilities;

import javax.swing.*;

/**
 * The basic KillRing of jEdit.
 * @author Matthieu Casanova
 * @version $Id: ParserRuleSet.java 5471 2006-06-22 06:31:23Z kpouer $
 */
class JEditKillRing extends KillRing
{
	//{{{ Constructor
	JEditKillRing()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			killringXML = new SettingsXML(settingsDirectory, "killring");
		}
	} //}}}

	//{{{ load() method
	@Override
	public void load()
	{
		if(killringXML == null)
			return;

		if(!killringXML.fileExists())
			return;

		Log.log(Log.MESSAGE,KillRing.class,"Loading " + killringXML);

		KillRingHandler handler = new KillRingHandler();
		try
		{
			killringXML.load(handler);
		}
		catch (OutOfMemoryError oem)
		{
			Log.log(Log.ERROR, this, "Unable to load entire Killring, too low memory, increase your jvm max heap size");
			int confirm = GUIUtilities.confirm(null, "killring.load-memoryerror", null, JOptionPane.YES_NO_OPTION,
					JOptionPane.ERROR_MESSAGE);
			if (confirm == JOptionPane.NO_OPTION)
			{
				System.exit(-1);
			}
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, this, ioe);
		}
		reset(handler.list);
	} //}}}

	//{{{ save() method
	@Override
	public void save()
	{
		if(killringXML == null)
			return;

		if(killringXML.hasChangedOnDisk())
		{
			Log.log(Log.WARNING,KillRing.class,killringXML
				+ " changed on disk; will not save killring"
				+ " files");
			return;
		}

		Log.log(Log.MESSAGE,KillRing.class,"Saving " + killringXML);

		String lineSep = System.getProperty("line.separator");

		SettingsXML.Saver out = null;

		try
		{
			out = killringXML.openSaver();
			out.writeXMLDeclaration("1.1");

			out.write("<!DOCTYPE KILLRING SYSTEM \"killring.dtd\">");
			out.write(lineSep);
			out.write("<KILLRING>");
			out.write(lineSep);

			int size = getSize();
			for(int i = size - 1; i >=0; i--)
			{
				out.write("<ENTRY>");
				out.write(XMLUtilities.charsToEntities(
					getElementAt(i).toString(),true));
				out.write("</ENTRY>");
				out.write(lineSep);
			}

			out.write("</KILLRING>");
			out.write(lineSep);

			out.finish();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,KillRing.class,e);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}
	} //}}}

	//{{{ Private members
	private SettingsXML killringXML;

	//{{{ KillRingHandler class
	private static class KillRingHandler extends DefaultHandler
	{
		public List<String> list = new LinkedList<String>();

		@Override
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "killring.dtd", getClass());
		}

		@Override
		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			inEntry = qName.equals("ENTRY");
		}

		@Override
		public void endElement(String uri, String localName, String name)
		{
			if(name.equals("ENTRY"))
			{
				list.add(charData.toString());
				inEntry = false;
				charData.setLength(0);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			if (inEntry)
				charData.append(ch, start, length);
		}

		@Override
		public void processingInstruction(String target, String data)
		{
			if ("illegal-xml-character".equals(target))
			{
				char ch;
				try
				{
					ch = (char)Integer.parseInt(data.trim());
				}
				catch (Exception e)
				{
					Log.log(Log.ERROR, this,
						"Failed to get character from PI"
							+ "\"" + target + "\""
							+ " with \"" + data + "\""
							+ ": " + e);
					return;
				}
				characters(new char[] {ch}, 0, 1);
			}
		}

		private final StringBuilder charData = new StringBuilder();
		private boolean inEntry;
	} //}}}
	//}}}
}
