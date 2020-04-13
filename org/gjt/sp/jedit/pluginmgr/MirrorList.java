/*
 * MirrorList.java - Mirrors list
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;


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
	//{{{ mirrorListFromDisk method
	public static MirrorList mirrorListFromDisk(ProgressObserver progressObserver) throws IOException, SAXException, ParserConfigurationException
	{
		Log.log(Log.NOTICE, MirrorList.class, "Loading mirror list from cache");
		String xml = readXml().orElseThrow(() -> new IOException("Unable to read local mirror cache"));
		return new MirrorList(xml, progressObserver);
	} //}}}

	//{{{ mirrorListFromInternet method
	public static MirrorList mirrorListFromInternet(ProgressObserver progressObserver) throws IOException, ParserConfigurationException, SAXException
	{
		Log.log(Log.NOTICE, MirrorList.class, "Loading mirror list from internet");
		String path = jEdit.getProperty("plugin-manager.mirror-url");
		String xml = downloadXml(path).orElseThrow(() -> new IOException("Unable to load remote mirror cache"));
		return new MirrorList(xml, progressObserver);
	} //}}}

	//{{{ MirrorList constructor
	public MirrorList(String xml, ProgressObserver observer) throws ParserConfigurationException, SAXException, IOException
	{
		this.xml = xml;
		mirrors = new ArrayList<>();

		Mirror none = new Mirror();
		none.id = Mirror.NONE;
		none.description = none.location = none.country = none.continent = "";
		mirrors.add(none);


		MirrorListHandler handler = new MirrorListHandler(this);

		observer.setValue(1L);
		Reader in = new BufferedReader(new StringReader(xml));

		InputSource isrc = new InputSource(in);
		isrc.setSystemId("jedit.jar");
		XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
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
	private final String xml;
	private final List<Mirror> mirrors;


	//{{{ readXml() method
	/**
	 * Read the mirror list xml.
	 * @return
	 */
	private static Optional<String> readXml()
	{
		return getMirrorListFile()
			.filter(file -> Files.exists(file))
			.flatMap(MirrorList::readFile);
	} //}}}

	public void saveXml()
	{
		getMirrorListFile().ifPresent(this::saveFile);
	}

	private static Optional<String> readFile(Path path)
	{
		try
		{
			return Optional.of(Files.readString(path));
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, MirrorList.class, "Unable to read path " + path, e);
		}
		return Optional.empty();
	}

	private void saveFile(Path path)
	{
		try
		{
			Files.writeString(path, xml);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, "Unable to write cached mirror list : " + xml, e);
		}
	}

	/**
	 * @return the local mirror list file
	 */
	private static Optional<Path> getMirrorListFile()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return Optional.empty();

		Path mirrorList = Path.of(MiscUtilities.constructPath(settingsDirectory,"mirrorList.xml"));
		return Optional.of(mirrorList);
	}

	//{{{ downloadXml() method
	/**
	 * Read and store the mirror list xml.
	 *
	 * @param path the url
	 * @throws IOException exception if it was not possible to read the
	 * xml or if the url was invalid
	 * @return
	 */
	private static Optional<String> downloadXml(String path) throws IOException
	{
		try (InputStream inputStream = new URL(path).openStream())
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtilities.copyStream(null,inputStream,out, false);
			return Optional.of(out.toString());
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
		mirrors.sort(new MirrorCompare());
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
		@Override
		public int compare(Mirror m1, Mirror m2)
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
