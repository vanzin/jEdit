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

import java.io.*;
import java.net.*;
import java.util.*;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

import org.gjt.sp.jedit.*;

public class MirrorList
{
	public ArrayList mirrors;

	//{{{ MirrorList constructor
	public MirrorList() throws Exception
	{
		mirrors = new ArrayList();

		Mirror none = new Mirror();
		none.id = Mirror.NONE;
		none.description = none.location = none.country = none.continent = "";
		mirrors.add(none);

		String path = jEdit.getProperty("plugin-manager.mirror-url");
		MirrorListHandler handler = new MirrorListHandler(this,path);

		XMLReader parser = XMLReaderFactory.createXMLReader();
		Reader in = new BufferedReader(new InputStreamReader(
			new URL(path).openStream()));
		InputSource isrc = new InputSource(in);
		isrc.setSystemId("jedit.jar");
		parser.setContentHandler(handler);
		parser.setDTDHandler(handler);
		parser.setEntityResolver(handler);
		parser.setErrorHandler(handler);
		try
		{
			parser.parse(isrc);
		}
		finally
		{
			in.close();
		}
	} //}}}

	//{{{ Private members

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
	class MirrorCompare implements Comparator
	{
		public int compare(Object o1,Object o2)
		{
			Mirror m1 = (Mirror)o1;
			Mirror m2 = (Mirror)o2;

			int result;
			if ((result = m1.continent.compareToIgnoreCase(m2.continent)) == 0)
				if ((result = m1.country.compareToIgnoreCase(m2.country)) == 0)
					if ((result = m1.location.compareToIgnoreCase(m2.location)) == 0)
						return m1.description.compareToIgnoreCase(m2.description);
			return result;
		}

		public boolean equals(Object obj)
		{
			return (obj instanceof MirrorCompare);
		}
	} //}}}

	//}}}
}
