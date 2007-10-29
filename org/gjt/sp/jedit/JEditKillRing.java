/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
 * Portions copyright (C) 2006 Matthieu Casanova
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
import java.io.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.buffer.KillRing;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.IOUtilities;

/**
 * The basic KillRing of jEdit.
 * @author Matthieu Casanova
 * @version $Id: ParserRuleSet.java 5471 2006-06-22 06:31:23Z kpouer $
 */
class JEditKillRing extends KillRing
{
	//{{{ load() method
	public void load()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File killRing = new File(MiscUtilities.constructPath(
			settingsDirectory,"killring.xml"));
		if(!killRing.exists())
			return;

		killRingModTime = killRing.lastModified();
		Log.log(Log.MESSAGE,KillRing.class,"Loading killring.xml");

		KillRingHandler handler = new KillRingHandler();
		try
		{
			XMLUtilities.parseXML(new FileInputStream(killRing), handler);
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, this, ioe);
		}
		reset(handler.list);
	} //}}}

	//{{{ save() method
	public void save()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File file1 = new File(MiscUtilities.constructPath(
			settingsDirectory, "#killring.xml#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			settingsDirectory, "killring.xml"));
		if(file2.exists() && file2.lastModified() != killRingModTime)
		{
			Log.log(Log.WARNING,KillRing.class,file2
				+ " changed on disk; will not save killring"
				+ " files");
			return;
		}

		jEdit.backupSettingsFile(file2);

		Log.log(Log.MESSAGE,KillRing.class,"Saving killring.xml");

		String lineSep = System.getProperty("line.separator");
		String encoding = "UTF-8";

		BufferedWriter out = null;

		try
		{
			out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file1), encoding));

			out.write("<?xml version=\"1.1\""
				+ " encoding=\"" + encoding + "\"?>");
			out.write(lineSep);
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

			out.close();

			/* to avoid data loss, only do this if the above
			 * completed successfully */
			file2.delete();
			file1.renameTo(file2);
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
	private long killRingModTime;

	//{{{ KillRingHandler class
	private static class KillRingHandler extends DefaultHandler
	{
		public List<String> list = new LinkedList<String>();

		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "killring.dtd", getClass());
		}

		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			inEntry = qName.equals("ENTRY");
		}

		public void endElement(String uri, String localName, String name)
		{
			if(name.equals("ENTRY"))
			{
				list.add(charData.toString());
				inEntry = false;
				charData.setLength(0);
			}
		}

		public void characters(char[] ch, int start, int length)
		{
			if (inEntry)
				charData.append(ch, start, length);
		}

		private StringBuffer charData = new StringBuffer();
		private boolean inEntry;
	} //}}}
	//}}}
}
