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

import com.microstar.xml.*;
import java.io.*;
import java.util.Stack;
import org.gjt.sp.util.Log;

class PluginListHandler extends HandlerBase
{
	PluginListHandler(PluginList pluginList, String path)
	{
		this.pluginList = pluginList;
		this.path = path;
		stateStack = new Stack();
	}

	public Object resolveEntity(String publicId, String systemId)
	{
		if("plugins.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");

			/* try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream(
					"/org/gjt/sp/jedit/pluginmgr/plugins.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " plugins.dtd:");
				Log.log(Log.ERROR,this,e);
			} */
		}

		return null;
	}

	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();
		value = (value == null) ? null : value.intern();

		if(aname == "NAME")
			name = value;
		else if(aname == "JAR")
			jar = value;
		else if(aname == "VERSION")
			version = value;
		else if(aname == "DATE")
			date = value;
		else if(aname == "OBSOLETE")
			obsolete = ("TRUE".equals(value));
		else if(aname == "WHAT")
			depWhat = value;
		else if(aname == "FROM")
			depFrom = value;
		else if(aname == "TO")
			depTo = value;
		else if(aname == "PLUGIN")
			depPlugin = value;
		else if(aname == "SIZE")
		{
			size = Integer.parseInt(value);
			if(size == 0)
				Log.log(Log.WARNING,this,"SIZE = 0");
		}
	}

	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("PLUGINS".equals(name))
			return;

		Log.log(Log.ERROR,this,path + ": DOCTYPE must be PLUGINS");
	}

	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if(tag == "DESCRIPTION")
		{
			description = text;
		}
		else if(tag == "PLUGIN_SET_ENTRY")
			pluginSetEntry = text;
		else if(tag == "AUTHOR")
		{
			if(author != null && author.length() != 0)
				author = author + ", " + text;
			else
				author = text;
		}
		else if(tag == "DOWNLOAD")
			download = text;
		else if(tag == "DOWNLOAD_SOURCE")
			downloadSource = text;
	}

	public void startElement(String tag)
	{
		tag = pushElement(tag);

		if(tag == "PLUGIN_SET")
		{
			description = null;
			pluginSet = new PluginList.PluginSet();
			pluginSet.name = name;
		}
		else if(tag == "PLUGIN")
		{
			description = null;
			author = null;
			branch = null;
			plugin = new PluginList.Plugin();
		}
		else if(tag == "BRANCH")
		{
			download = null;
			branch = new PluginList.Branch();
		}
		else if(tag == "DOWNLOAD")
			downloadSize = size;
		else if(tag == "DOWNLOAD_SOURCE")
			downloadSourceSize = size;
	}

	public void endElement(String tag)
	{
		if(tag == null)
			return;
		else
			tag = tag.intern();

		popElement();

		if(tag == "PLUGIN_SET")
		{
			pluginList.addPluginSet(pluginSet);
			pluginSet = null;
			pluginSetEntry = null;
		}
		else if(tag == "PLUGIN_SET_ENTRY")
		{
			pluginSet.plugins.addElement(pluginSetEntry);
			pluginSetEntry = null;
		}
		else if(tag == "PLUGIN")
		{
			plugin.jar = jar;
			plugin.name = name;
			plugin.author = author;
			plugin.description = description;
			pluginList.addPlugin(plugin);
			jar = null;
			name = null;
			author = null;
		}
		else if(tag == "BRANCH")
		{
			branch.version = version;
			branch.date = date;
			branch.download = download;
			branch.downloadSize = downloadSize;
			branch.downloadSource = downloadSource;
			branch.downloadSourceSize = downloadSourceSize;
			branch.obsolete = obsolete;
			plugin.branches.addElement(branch);
			version = null;
			download = null;
			obsolete = false;
		}
		else if(tag == "DEPEND")
		{
			PluginList.Dependency dep = new PluginList.Dependency(
				depWhat,depFrom,depTo,depPlugin);
			branch.deps.addElement(dep);
			depWhat = null;
			depFrom = null;
			depTo = null;
			depPlugin = null;
		}
	}

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
	}

	public void endDocument()
	{
		pluginList.finished();
	}
	// end HandlerBase implementation

	// private members
	private String path;

	private PluginList pluginList;

	private PluginList.PluginSet pluginSet;
	private String pluginSetEntry;

	private PluginList.Plugin plugin;
	private String jar;
	private String author;

	private PluginList.Branch branch;
	private boolean obsolete;
	private String version;
	private String date;
	private String download;
	private int downloadSize;
	private String downloadSource;
	private int downloadSourceSize;
	private int size;
	private String depWhat;
	private String depFrom;
	private String depTo;
	private String depPlugin;

	private String name;
	private String description;

	private Stack stateStack;

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
}
