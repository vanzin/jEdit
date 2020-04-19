/*
 * PluginListHandler.java - XML handler for the plugin list
 * Copyright (C) 2001 Slava Pestov
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
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
//}}}

/**
 * @version $Id$
 */
class PluginListHandler extends DefaultHandler
{
	//{{{ PluginListHandler constructor
	PluginListHandler(PluginList pluginList)
	{
		this.pluginList = pluginList;

		author = new StringBuilder();
		description = new StringBuilder();
		pluginSetEntry = new StringBuilder();
		download = new StringBuilder();
		downloadSource = new StringBuilder();
	} //}}}

	//{{{ resolveEntity() method
	@Override
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return XMLUtilities.findEntity(systemId, "plugins.dtd", getClass());
	} //}}}

	//{{{ attribute() method
	public void attribute(String aname, String value, boolean isSpecified)
	{
		switch (aname)
		{
			case "NAME":
				name = value;
				break;
			case "JAR":
				jar = value;
				break;
			case "VERSION":
				version = value;
				break;
			case "DATE":
				date = value;
				break;
			case "OBSOLETE":
				obsolete = ("TRUE".equals(value));
				break;
			case "WHAT":
				depWhat = value;
				break;
			case "FROM":
				depFrom = value;
				break;
			case "TO":
				depTo = value;
				break;
			case "PLUGIN":
				depPlugin = value;
				break;
			case "SIZE":
				size = Integer.parseInt(value);
				if (size == 0)
					Log.log(Log.WARNING, this, "SIZE = 0");
				break;
		}
	} //}}}

	//{{{ characters() method
	@Override
	public void characters(char[] c, int off, int len)
	{
		String tag = peekElement();

		switch (tag)
		{
			case "DESCRIPTION":
				description.append(c, off, len);
				break;
			case "PLUGIN_SET_ENTRY":
				pluginSetEntry.append(c, off, len);
				break;
			case "AUTHOR":
				if (author.length() != 0)
					author.append(", ");
				author.append(c, off, len);
				break;
			case "DOWNLOAD":
				download.append(c, off, len);
				break;
			case "DOWNLOAD_SOURCE":
				downloadSource.append(c, off, len);
				break;
		}
	} //}}}

	//{{{ startElement() method
	@Override
	public void startElement(String uri, String localName,
				 String tag, Attributes attrs)
	{
		for (int i = 0; i < attrs.getLength(); i++)
		{
			String aName = attrs.getQName(i);
			String aValue = attrs.getValue(i);
			attribute(aName, aValue, true);
		}


		tag = pushElement(tag);

		switch (tag)
		{
			case "PLUGIN_SET":
				description.setLength(0);
				pluginSet = new PluginList.PluginSet();
				pluginSet.name = name;
				break;
			case "PLUGIN":
				description.setLength(0);
				author.setLength(0);
				branch = null;
				plugin = new PluginList.Plugin();
				break;
			case "BRANCH":
				download.setLength(0);
				branch = new PluginList.Branch();
				break;
			case "DOWNLOAD":
				downloadSize = size;
				break;
			case "DOWNLOAD_SOURCE":
				downloadSourceSize = size;
				break;
		}
	} //}}}

	//{{{ endElement() method
	@Override
	public void endElement(String uri, String localName, String tag)
	{
		popElement();

		switch (tag)
		{
			case "PLUGIN_SET":
				pluginList.addPluginSet(pluginSet);
				pluginSet = null;
				pluginSetEntry.setLength(0);
				break;
			case "PLUGIN_SET_ENTRY":
				pluginSet.plugins.add(pluginSetEntry.toString());
				pluginSetEntry.setLength(0);
				break;
			case "PLUGIN":
				plugin.jar = jar;
				plugin.name = name;
				plugin.author = author.toString();
				plugin.description = description.toString();
				pluginList.addPlugin(plugin);
				jar = null;
				name = null;
				author.setLength(0);
				description.setLength(0);
				break;
			case "BRANCH":
				branch.version = version;
				branch.date = date;
				branch.download = download.toString();
				branch.downloadSize = downloadSize;
				branch.downloadSource = downloadSource.toString();
				branch.downloadSourceSize = downloadSourceSize;
				branch.obsolete = obsolete;
				plugin.branches.add(branch);
				version = null;
				download.setLength(0);
				downloadSource.setLength(0);
				obsolete = false;
				break;
			case "DEPEND":
				PluginList.Dependency dep = new PluginList.Dependency(
					depWhat, depFrom, depTo, depPlugin);
				branch.deps.add(dep);
				depWhat = null;
				depFrom = null;
				depTo = null;
				depPlugin = null;
				break;
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
		pluginList.finished();
	} //}}}

	// end HandlerBase implementation

	//{{{ private members
	
	//{{{ Instance variables
	private final PluginList pluginList;

	private PluginList.PluginSet pluginSet;
	private final StringBuilder pluginSetEntry;

	private PluginList.Plugin plugin;
	private String jar;
	private final StringBuilder author;

	private PluginList.Branch branch;
	private boolean obsolete;
	private String version;
	private String date;
	private final StringBuilder download;
	private int downloadSize;
	private final StringBuilder downloadSource;
	private int downloadSourceSize;
	private int size;
	private String depWhat;
	private String depFrom;
	private String depTo;
	private String depPlugin;

	private String name;
	private final StringBuilder description;

	private final Stack<String> stateStack = new Stack<>();
	//}}}

	//{{{ pushElement() method
	private String pushElement(String name)
	{
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
