/*
 * MirrorListHandler.java - XML handler for the mirrors list
 * :tabSize=4:indentSize=4:noTabs=false:
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

package org.gjt.sp.jedit.pluginmgr;

import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.Log;

import org.gjt.sp.jedit.options.PluginOptions;

/**
 * @version $Id$
 */
class MirrorListHandler extends DefaultHandler
{
	//{{{ Constructor
	MirrorListHandler(MirrorList mirrors)
	{
		this.mirrors = mirrors;
	} //}}}

	//{{{ resolveEntity() method
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return XMLUtilities.findEntity(systemId, "mirrors.dtd",
					PluginOptions.class);
	} //}}}

	//{{{ characters() method
	@Override
	public void characters(char[] c, int off, int len)
	{
		String tag = peekElement();

		if("DESCRIPTION".equals(tag))
			description.append(c, off, len);
		else if("LOCATION".equals(tag))
			location.append(c, off, len);
		else if("COUNTRY".equals(tag))
			country.append(c, off, len);
		else if("CONTINENT".equals(tag))
			continent.append(c, off, len);
	} //}}}

	//{{{ startElement() method
	@Override
	public void startElement(String uri, String localName,
				 String tag, Attributes attrs)
	{
		tag = pushElement(tag);

		if (tag.equals("MIRROR"))
		{
			mirror = new MirrorList.Mirror();
			id = attrs.getValue("ID");
		}
	} //}}}

	//{{{ endElement() method
	@Override
	public void endElement(String uri, String localName, String tag)
	{
		popElement();

		if(tag.equals("MIRROR"))
		{
			mirror.id = id;
			mirror.description = description.toString();
			mirror.location = location.toString();
			mirror.country = country.toString();
			mirror.continent = continent.toString();
			mirrors.add(mirror);
			description.setLength(0);
			location.setLength(0);
			country.setLength(0);
			continent.setLength(0);
		}
	} //}}}

	//{{{ startDocument() method
	@Override
	public void startDocument()
	{
		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			Log.log(Log.ERROR, this, e);
		}
	} //}}}

	//{{{ endDocument() method
	@Override
	public void endDocument()
	{
		mirrors.finished();
	} //}}}

	//{{{ Private members

	//{{{ Variables
	private String id;
	private final StringBuilder description = new StringBuilder();		// NOPMD
	private final StringBuilder location = new StringBuilder();			// NOPMD
	private final StringBuilder country = new StringBuilder();			// NOPMD
	private final StringBuilder continent = new StringBuilder();		// NOPMD

	private final MirrorList mirrors;
	private MirrorList.Mirror mirror;

	private final Stack<String> stateStack = new Stack<>();
	//}}}

	private String pushElement(String name)
	{
		stateStack.push(name);
		return name;
	}

	private String peekElement()
	{
		return stateStack.peek();
	}

	private void popElement()
	{
		stateStack.pop();
	}

	//}}}
}
