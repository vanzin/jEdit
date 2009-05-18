/*
 * PropertyManager.java - Manages property files
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Slava Pestov
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

import java.io.*;
import java.util.*;

class PropertyManager
{
	//{{{ getProperties() method
	Properties getProperties()
	{
		Properties total = new Properties();
		total.putAll(system);
		for (Properties plugin : plugins)
			total.putAll(plugin);
		total.putAll(site);
		total.putAll(user);
		return total;
	} //}}}

	//{{{ loadSystemProps() method
	void loadSystemProps(InputStream in)
		throws IOException
	{
		loadProps(system,in);
	} //}}}

	//{{{ loadSiteProps() method
	void loadSiteProps(InputStream in)
		throws IOException
	{
		loadProps(site,in);
	} //}}}

	//{{{ loadUserProps() method
	void loadUserProps(InputStream in)
		throws IOException
	{
		loadProps(user,in);
	} //}}}

	//{{{ saveUserProps() method
	void saveUserProps(OutputStream out)
		throws IOException
	{
		user.store(out,"jEdit properties");
	} //}}}

	//{{{ loadPluginProps() method
	Properties loadPluginProps(InputStream in)
		throws IOException
	{
		Properties plugin = new Properties();
		loadProps(plugin,in);
		plugins.add(plugin);
		return plugin;
	} //}}}

	//{{{ addPluginProps() method
	void addPluginProps(Properties props)
	{
		plugins.add(props);
	} //}}}

	//{{{ removePluginProps() method
	void removePluginProps(Properties props)
	{
		plugins.remove(props);
	} //}}}

	//{{{ getProperty() method
	String getProperty(String name)
	{
		String value = user.getProperty(name);
		if(value != null)
			return value;
		else
			return getDefaultProperty(name);
	} //}}}

	//{{{ setProperty() method
	void setProperty(String name, String value)
	{
		String prop = getDefaultProperty(name);

		/* if value is null:
		 * - if default is null, unset user prop
		 * - else set user prop to ""
		 * else
		 * - if default equals value, ignore
		 * - if default doesn't equal value, set user
		 */
		if(value == null)
		{
			if(prop == null || prop.length() == 0)
				user.remove(name);
			else
				user.setProperty(name,"");
		}
		else
		{
			if(value.equals(prop))
				user.remove(name);
			else
				user.setProperty(name,value);
		}
	} //}}}

	//{{{ setTemporaryProperty() method
	public void setTemporaryProperty(String name, String value)
	{
		user.remove(name);
		system.setProperty(name,value);
	} //}}}

	//{{{ unsetProperty() method
	void unsetProperty(String name)
	{
		if(getDefaultProperty(name) != null)
			user.setProperty(name,"");
		else
			user.remove(name);
	} //}}}

	//{{{ resetProperty() method
	public void resetProperty(String name)
	{
		user.remove(name);
	} //}}}

	//{{{ Private members
	private Properties system = new Properties();
	private List<Properties> plugins = new LinkedList<Properties>();
	private Properties site = new Properties();
	private Properties user = new Properties();

	//{{{ getDefaultProperty() method
	private String getDefaultProperty(String name)
	{
		String value = site.getProperty(name);
		if(value != null)
			return value;

		for (Properties plugin : plugins)
		{
			value = plugin.getProperty(name);
			if (value != null)
				return value;
		}

		return system.getProperty(name);
	} //}}}

	//{{{ loadProps() method
	private static void loadProps(Properties into, InputStream in)
		throws IOException
	{
		try
		{
			into.load(in);
		}
		finally
		{
			in.close();
		}
	} //}}}

	//}}}
}
