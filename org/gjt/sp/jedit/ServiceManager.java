/*
 * ServiceManager.java - Handles services.xml files in plugins
 * :tabSize=8:indentSize=8:noTabs=false:
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

import com.microstar.xml.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import org.gjt.sp.util.Log;

/**
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
public class ServiceManager
{
	//{{{ loadServices() method
	/**
	 * Loads a <code>services.xml</code> file.
	 * @since jEdit 4.2pre1
	 */
	public static void loadServices(PluginJAR plugin, URL uri,
		PluginJAR.PluginCacheEntry cache)
	{
		try
		{
			Log.log(Log.DEBUG,jEdit.class,"Loading services from " + uri);

			ServiceListHandler dh = new ServiceListHandler(plugin,uri);
			XmlParser parser = new XmlParser();
			parser.setHandler(dh);
			parser.parse(null, null, new BufferedReader(
				new InputStreamReader(
				uri.openStream())));
			if(cache != null)
				cache.cachedServices = dh.getCachedServices();
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,ServiceManager.class,uri + ":" + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,ServiceManager.class,e);
		}
	} //}}}

	//{{{ unloadServices() method
	/**
	 * Removes all services belonging to the specified plugin.
	 * @param plugin The plugin
	 * @since jEdit 4.2pre1
	 */
	public static void unloadServices(PluginJAR plugin)
	{
		Iterator descriptors = serviceMap.keySet().iterator();
		while(descriptors.hasNext())
		{
			Descriptor d = (Descriptor)descriptors.next();
			if(d.plugin == plugin)
				descriptors.remove();
		}
	} //}}}

	//{{{ registerService() method
	/**
	 * Registers a service. Plugins should provide a 
	 * <code>services.xml</code> file instead of calling this directly.
	 *
	 * @param clazz The service class
	 * @param name The service name
	 * @param code BeanShell code to create an instance of this
	 * @param plugin The plugin JAR, or null if this is a built-in service
	 *
	 * @since jEdit 4.2pre1
	 */
	public static void registerService(String clazz, String name,
		String code, PluginJAR plugin)
	{
		Descriptor d = new Descriptor(clazz,name,code,plugin);
		serviceMap.put(d,d);
	} //}}}

	//{{{ unregisterService() method
	/**
	 * Unregisters a service.
	 *
	 * @param clazz The service class
	 * @param name The service name
	 * @param code BeanShell code to create an instance of this
	 *
	 * @since jEdit 4.2pre1
	 */
	public static void unregisterService(String clazz, String name)
	{
		Descriptor d = new Descriptor(clazz,name);
		serviceMap.remove(d);
	} //}}}

	//{{{ getServiceTypes() method
	/**
	 * Returns all known service class types.
	 *
	 * @since jEdit 4.2pre1
	 */
	public static String[] getServiceTypes()
	{
		HashSet returnValue = new HashSet();

		Iterator descriptors = serviceMap.keySet().iterator();
		while(descriptors.hasNext())
		{
			Descriptor d = (Descriptor)descriptors.next();
			returnValue.add(d.clazz);
		}

		return (String[])returnValue.toArray(
			new String[returnValue.size()]);
	} //}}}

	//{{{ getServiceNames() method
	/**
	 * Returns the names of all registered services with the given
	 * class. For example, calling this with a parameter of
	 * "org.gjt.sp.jedit.io.VFS" returns all known virtual file
	 * systems.
	 *
	 * @param clazz The class name
	 * @since jEdit 4.2pre1
	 */
	public static String[] getServiceNames(String clazz)
	{
		ArrayList returnValue = new ArrayList();

		Iterator descriptors = serviceMap.keySet().iterator();
		while(descriptors.hasNext())
		{
			Descriptor d = (Descriptor)descriptors.next();
			if(d.clazz.equals(clazz))
				returnValue.add(d.name);
		}

		return (String[])returnValue.toArray(
			new String[returnValue.size()]);
	} //}}}

	//{{{ getService() method
	/**
	 * Returns an instance of the given service.
	 *
	 * @param clazz The service class
	 * @param name The service name
	 * @since jEdit 4.2pre1
	 */
	public static Object getService(String clazz, String name)
	{
		// they never taught you this in undergrad computer science
		Descriptor key = new Descriptor(clazz,name);
		Descriptor value = (Descriptor)serviceMap.get(key);
		if(value == null)
		{
			// unknown service - <clazz,name> not in table
			return null;
		}
		else
		{
			if(value.code == null)
			{
				loadServices(value.plugin,
					value.plugin.getServicesURI(),
					null);
				value = (Descriptor)serviceMap.get(key);
			}
			return value.getInstance();
		}
	} //}}}

	//{{{ Package-private members

	//{{{ registerService() method
	/**
	 * Registers a service.
	 *
	 * @since jEdit 4.2pre1
	 */
	static void registerService(Descriptor d)
	{
		serviceMap.put(d,d);
	} //}}}

	//}}}

	//{{{ Private members
	private static Map serviceMap = new HashMap();
	//}}}

	//{{{ Descriptor class
	static class Descriptor
	{
		String clazz;
		String name;
		String code;
		PluginJAR plugin;
		Object instance;
		boolean instanceIsNull;

		// this constructor keys the hash table
		Descriptor(String clazz, String name)
		{
			this.clazz = clazz;
			this.name  = name;
		}

		// this constructor is the value of the hash table
		Descriptor(String clazz, String name, String code,
			PluginJAR plugin)
		{
			this.clazz  = clazz;
			this.name   = name;
			this.code   = code;
			this.plugin = plugin;
		}

		Object getInstance()
		{
			if(instanceIsNull)
				return null;
			else if(instance == null)
			{
				// lazy instantiation
				instance = BeanShell.eval(null,
					BeanShell.getNameSpace(),
					code);
				if(instance == null)
				{
					// avoid re-running script if it gives
					// us null
					instanceIsNull = true;
				}
			}

			return instance;
		}
		public int hashCode()
		{
			return name.hashCode();
		}

		public boolean equals(Object o)
		{
			if(o instanceof Descriptor)
			{
				Descriptor d = (Descriptor)o;
				return d.clazz.equals(clazz)
					&& d.name.equals(name);
			}
			else
				return false;
		}
	} //}}}
}
