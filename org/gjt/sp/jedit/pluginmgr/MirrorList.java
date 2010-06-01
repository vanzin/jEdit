/*
 * MirrorList.java - Mirrors list
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
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

package org.gjt.sp.jedit.pluginmgr;

//{{{ Imports
import java.io.*;
import java.net.*;
import java.util.*;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.ProgressObserver;
import org.gjt.sp.util.Log;
//}}}

/**
 * @version $Id$
 */
public class MirrorList
{
	//{{{ MirrorList constructor
	public MirrorList(boolean download, ProgressObserver observer) throws Exception
	{
		mirrors = new ArrayList<Mirror>();

		Mirror none = new Mirror();
		none.id = Mirror.NONE;
		none.description = none.location = none.country = none.continent = "";
		mirrors.add(none);

		String path = jEdit.getProperty("plugin-manager.mirror-url");
		MirrorListHandler handler = new MirrorListHandler(this,path);
		if (download)
		{
			Log.log(Log.NOTICE, this, "Loading mirror list from internet");
			downloadXml(path);
		}
		else
		{
			Log.log(Log.NOTICE, this, "Loading mirror list from cache");
			readXml();
		}
		if (xml == null)
			return;
		observer.setValue(1L);
		Reader in = new BufferedReader(new StringReader(xml));

		InputSource isrc = new InputSource(in);
		isrc.setSystemId("jedit.jar");
		XMLReader parser = XMLReaderFactory.createXMLReader();
		parser.setContentHandler(handler);
		parser.setDTDHandler(handler);
		parser.setEntityResolver(handler);
		parser.setErrorHandler(handler);
		parser.parse(isrc);
		observer.setValue(2L);
	} //}}}

	//{{{ getXml() method
	public String getXml()
	{
		return xml;
	} //}}}

	//{{{ getMirrors() method
	public List<Mirror> getMirrors()
	{
		return mirrors;
	} //}}}

	//{{{ Private members

	/** The xml mirror list. */
	private String xml;
	private final List<Mirror> mirrors;


	//{{{ readXml() method
	/**
	 * Read and store the mirror list xml.
	 * @throws IOException exception if it was not possible to read the
	 * xml or if the url was invalid
	 */
	private void readXml() throws IOException
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File mirrorList = new File(MiscUtilities.constructPath(
			settingsDirectory,"mirrorList.xml"));
		if(!mirrorList.exists())
			return;
		InputStream inputStream = null;
		try
		{
			inputStream = new BufferedInputStream(new FileInputStream(mirrorList));

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtilities.copyStream(null,inputStream,out, false);
			xml = out.toString();
		}
		finally
		{
			IOUtilities.closeQuietly(inputStream);
		}
	} //}}}

	//{{{ downloadXml() method
	/**
	 * Read and store the mirror list xml.
	 *
	 * @param path the url
	 * @throws IOException exception if it was not possible to read the
	 * xml or if the url was invalid
	 */
	private void downloadXml(String path) throws IOException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = new URL(path).openStream();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtilities.copyStream(null,inputStream,out, false);
			xml = out.toString();
		}
		finally
		{
			IOUtilities.closeQuietly(inputStream);
		}
	} //}}}

	//{{{ add() method
	void add(Mirror mirror)
	{
		mirrors.add(mirror);
	} //}}}

	//{{{ finished() method
	void finished()
	{
		Collections.sort(mirrors,new MirrorCompare());
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Mirror class
	public static class Mirror
	{
		public static final String NONE = "NONE";

		public String id;
		public String description;
		public String location;
		public String country;
		public String continent;
	} //}}}

	//{{{ MirrorCompare class
	private static class MirrorCompare implements Comparator<Mirror>
	{
		public int compare(Mirror m1,Mirror m2)
		{
			int result;
			if ((result = m1.continent.compareToIgnoreCase(m2.continent)) == 0)
				if ((result = m1.country.compareToIgnoreCase(m2.country)) == 0)
					if ((result = m1.location.compareToIgnoreCase(m2.location)) == 0)
						return m1.description.compareToIgnoreCase(m2.description);
			return result;
		}

		public boolean equals(Object obj)
		{
			return obj instanceof MirrorCompare;
		}
	} //}}}

	//}}}
}
