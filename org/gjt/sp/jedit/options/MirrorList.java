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

package org.gjt.sp.jedit.options;

import com.microstar.xml.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.*;

class MirrorList
{
	//{{{ Constructor
	MirrorList() throws Exception
	{
		mirrors = new LinkedList();

		String path = jEdit.getProperty("plugin-manager.mirror-url");
		MirrorListHandler handler = new MirrorListHandler(this,path);
		XmlParser parser = new XmlParser();
		parser.setHandler(handler);

		parser.parse(null,null,new BufferedReader(new InputStreamReader(
			new URL(path).openStream())));
	} //}}}

	//{{{ Private members
	
	List mirrors;
	
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
	
	//{{{ Mirror class
	static class Mirror
	{
		String id;
		String description;
		String location;
		String country;
		String continent;
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
