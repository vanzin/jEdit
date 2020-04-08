/*
 * ServiceManager.java - Handles services.xml files in plugins
 * :tabSize=4:indentSize=4:noTabs=false:
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

package org.gjt.sp.jedit;

//{{{ Imports
import java.net.URL;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
class ServiceListHandler extends DefaultHandler
{
	//{{{ ServiceListHandler constructor
	ServiceListHandler(PluginJAR plugin, URL uri)
	{
		this.plugin = plugin;
		this.uri = uri;
		code = new StringBuilder();
		stateStack = new Stack<>();
		cachedServices = new LinkedList<>();
	} //}}}

	//{{{ resolveEntity() method
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return XMLUtilities.findEntity(systemId, "services.dtd", getClass());
	} //}}}

	//{{{ characters() method
	@Override
	public void characters(char[] c, int off, int len)
	{
		String tag = peekElement();
		if (tag == "SERVICE")
			code.append(c, off, len);
	} //}}}

	//{{{ startElement() method
	@Override
	public void startElement(String uri, String localName,
				 String tag, Attributes attrs)
	{
		pushElement(tag);
		serviceName = attrs.getValue("NAME");
		serviceClass = attrs.getValue("CLASS");
	} //}}}

	//{{{ endElement() method
	@Override
	public void endElement(String uri, String localName, String name)
	{
		String tag = peekElement();

		if(name.equals(tag))
		{
			if (tag.equals("SERVICE"))
			{
				ServiceManager.Descriptor d =
					new ServiceManager.Descriptor(
					serviceClass,serviceName,code.toString(),plugin);
				ServiceManager.registerService(d);
				cachedServices.add(d);
				code.setLength(0);
			}

			popElement();
		}
		else
		{
			// can't happen
			throw new InternalError();
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
			Log.log(Log.ERROR, e, e);
		}
	} //}}}

	//{{{ getCachedServices() method
	public ServiceManager.Descriptor[] getCachedServices()
	{
		return cachedServices.toArray(new ServiceManager.Descriptor[0]);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final PluginJAR plugin;
	private URL uri;

	private String serviceName;
	private String serviceClass;
	private final StringBuilder code;

	private final Stack<String> stateStack;

	private final List<ServiceManager.Descriptor> cachedServices;
	//}}}

	//{{{ pushElement() method
	private String pushElement(String name)
	{
		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	} //}}}

	//{{{ peekElement() method
	private String peekElement()
	{
		return stateStack.peek();
	} //}}}

	//{{{ popElement() method
	private String popElement()
	{
		return stateStack.pop();
	} //}}}

	//}}}
}
