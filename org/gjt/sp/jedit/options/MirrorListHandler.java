/*
 * MirrorListHandler.java - XML handler for the mirrors list
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki (parts copied from Slava Pestov :) )
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
import java.util.*;
import org.gjt.sp.util.Log;

class MirrorListHandler extends HandlerBase
{
	//{{{ Constructor
	MirrorListHandler(MirrorList mirrors, String path)
	{
		this.mirrors = mirrors;
		this.path = path;
		stateStack = new Stack();
	} //}}}

	//{{{ resolveEntity() method
	public Object resolveEntity(String publicId, String systemId)
	{
		if("mirrors.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");
		}

		return null;
	} //}}}

	//{{{ attribute() method
	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();
		value = (value == null) ? null : value.intern();
		if(aname == "ID")
			id = value;
	} //}}}

	//{{{ doctypeDecl() method
	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("MIRRORS".equals(name))
			return;

		Log.log(Log.ERROR,this,path + ": DOCTYPE must be MIRRORS");
	} //}}}

	//{{{ charData() method
	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);
		
		if(tag == "DESCRIPTION")
			description = text;
		else if(tag == "LOCATION")
			location = text;
		else if(tag == "COUNTRY")
			country = text;
		else if(tag == "CONTINENT")
			continent = text;
	} //}}}

	//{{{ startElement() method
	public void startElement(String tag)
	{
		tag = pushElement(tag);

		if(tag == "MIRROR")
			mirror = new MirrorList.Mirror();
	} //}}}

	//{{{ endElement() method
	public void endElement(String tag)
	{
		if(tag == null)
			return;
		else
			tag = tag.intern();

		popElement();

		if(tag == "MIRROR")
		{
			mirror.id = id;
			mirror.description = description;
			mirror.location = location;
			mirror.country = country;
			mirror.continent = continent;
			mirrors.add(mirror);
		}
	} //}}}

	//{{{ startDocument() method
	public void startDocument()
	{
		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	} //}}}

	//{{{ endDocument() method
	public void endDocument()
	{
		mirrors.finished();
	} //}}}

	//{{{ Private members
	
	//{{{ Variables
	private String id;
	private String description;
	private String location;
	private String country;
	private String continent;
	
	private MirrorList mirrors;
	private MirrorList.Mirror mirror;
	
	private Stack stateStack;
	private String path;
	//}}}
	
	private String pushElement(String name)
	{
		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	}

	private String peekElement()
	{
		return (String) stateStack.peek();
	}

	private String popElement()
	{
		return (String) stateStack.pop();
	}
	
	//}}}
}
