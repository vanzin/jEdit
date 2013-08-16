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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.buffer.FoldHandlerProvider;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.XMLUtilities;

/**
 * A generic way for plugins (and core) to provide various API extensions.<p>
 *
 * Services are loaded from files named <code>services.xml</code> inside the
 * plugin JAR. A service definition file has the following form:
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE SERVICES SYSTEM "services.dtd"&gt;
 *&lt;SERVICES&gt;
 *    &lt;SERVICE NAME="service name" CLASS="fully qualified class name"&gt;
 *        // BeanShell code evaluated when the sevice is first activated
 *    &lt;/SERVICE&gt;
 *&lt;/SERVICES&gt;</pre>
 *
 * The following elements are valid:
 *
 * <ul>
 * <li>
 * <code>SERVICES</code> is the top-level element and refers
 * to the set of services offered by the plugin.
 * </li>
 * <li>
 * A <code>SERVICE</code> contains the factory method for this
 * service singleton. The ServiceManager manages named singletons
 * created from these factory methods.
 * It has two attributes, both required: <code>NAME</code> and
 * <code>CLASS</code>. The <code>CLASS</code> attribute must be the name of
 * a known sevice type; see below.
 * </li>
 * <li>
 * A <code>SERVICE</code> element should the BeanShell code that returns a
 * new instance of the named class. Note that this code can return
 * <code>null</code>.
 * </li>
 * </ul>
 *
 * To see all of the services offered by jEdit core, see
 * jEdit's <tt>services.xml</tt> file.
 * Some core services are listed below:
 * <ul>
 * <li>{@link org.gjt.sp.jedit.buffer.FoldHandler}</li>
 * <li>{@link org.gjt.sp.jedit.textarea.FoldPainter}</li>
 * <li>{@link org.gjt.sp.jedit.io.VFS}</li>
 * <li>{@link org.gjt.sp.jedit.io.Encoding}</li>
 * <li>{@link org.gjt.sp.jedit.io.EncodingDetector}</li>
 * <li>{@link org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory}</li>
 * <li>{@link org.gjt.sp.jedit.gui.DockingFrameworkProvider}</li>
 * <li>{@link org.gjt.sp.jedit.gui.tray.JEditTrayIcon}</li>
 * </ul>
 *
 * Plugins may define/provide more, so the only way to see a
 * complete list of service types currently in use is by calling
 * {@link #getServiceTypes()}.
 * <br />
 * To use a service from a plugin, add a piece of code somewhere that calls
 * {@link #getServiceNames(String)} and  {@link #getService(String,String)}.
 *
 *
 * @see BeanShell
 * @see PluginJAR
 *
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
		ServiceListHandler dh = new ServiceListHandler(plugin,uri);
		try
		{
			java.io.InputStream in = uri.openStream();
			if(in == null)
			{
				// this happened when calling generateCache() in the context of 'find orphan jars'
				// in org.gjt.sp.jedit.pluginmgr.ManagePanel.FindOrphan.actionPerformed(ActionEvent)
				// because for not loaded plugins, the plugin will not be added to the list of pluginJars
				// so the org.gjt.sp.jedit.proto.jeditresource.PluginResURLConnection will not find the plugin
				// to read the resource from.
				// Better log a small error message than a big stack trace
				Log.log(Log.WARNING, ServiceManager.class, "Unable to open: " + uri);
			}
			else  if (!XMLUtilities.parseXML(uri.openStream(), dh)
				&& cache != null)
			{
				cache.cachedServices = dh.getCachedServices();
			}
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, ServiceManager.class, ioe);
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
		Iterator<Descriptor> descriptors = serviceMap.keySet().iterator();
		while(descriptors.hasNext())
		{
			Descriptor d = descriptors.next();
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
	public static void registerService(@Nonnull String clazz,
					   @Nonnull String name,
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
	 *
	 * @since jEdit 4.2pre1
	 */
	public static void unregisterService(@Nonnull String clazz,
					     @Nonnull String name)
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
		Set<String> returnValue = new HashSet<String>();

		Set<Descriptor> keySet = serviceMap.keySet();
		for (Descriptor d : keySet)
			returnValue.add(d.clazz);

		return returnValue.toArray(
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
		List<String> returnValue = new ArrayList<String>();

		Set<Descriptor> keySet = serviceMap.keySet();
		for (Descriptor d : keySet)
			if(d.clazz.equals(clazz))
				returnValue.add(d.name);


		return returnValue.toArray(
			new String[returnValue.size()]);
	} //}}}


	//{{{ getServiceNames() method
	public static String[] getServiceNames(Class clazz)
	{
		return getServiceNames(clazz.getName());
	} //}}}

	//{{{ getService() methods
	/**
	 * Returns an instance of the given service. The first time this is
	 * called for a given service, the BeanShell code is evaluated. The
	 * result is cached for future invocations, so in effect services are
	 * singletons.
	 *
	 * @param clazz The service class
	 * @param name The service name
	 * @since jEdit 4.2pre1
	 */
	@Nullable
	public static Object getService(@Nonnull String clazz,
					@Nonnull String name)
	{

		Descriptor key = new Descriptor(clazz,name);
		Descriptor value = serviceMap.get(key);
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
				value = serviceMap.get(key);
			}
			return value.getInstance();
		}
	}

    /**
     * Returns an instance of the given service. The first time this is
	 * called for a given service, the BeanShell code is evaluated. The
	 * result is cached for future invocations, so in effect services are
	 * singletons.
     *
     * @param clazz The service class
	 * @param name The service name
     * @return the service instance
     * @since jEdit 4.4pre1
     */
	public static <E> E getService(Class<E> clazz, String name)
	{
		return (E) getService(clazz.getName(), name);
	} //}}}

	//{{{ Package-private members

	//{{{ registerService() method
	/**
	 * Registers a service.
	 *
	 * @param d the service descriptor
	 * @since jEdit 4.2pre1
	 */
	static void registerService(Descriptor d)
	{
		serviceMap.put(d,d);
	} //}}}

	//}}}

	//{{{ Private members
	private static final Map<Descriptor, Descriptor> serviceMap = new HashMap<Descriptor, Descriptor>();
	//}}}

	//{{{ Descriptor class
	static class Descriptor
	{
		final String clazz;
		final String name;
		String code;
		PluginJAR plugin;
		Object instance;
		boolean instanceIsNull;

		// this constructor keys the hash table
		Descriptor(@Nonnull String clazz, @Nonnull String name)
		{
			this.clazz = clazz;
			this.name  = name;
		}

		// this constructor is the value of the hash table
		Descriptor(@Nonnull String clazz, @Nonnull String name,
			   String code, PluginJAR plugin)
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

		@Override
		public int hashCode()
		{
			int result = 31 * clazz.hashCode() + name.hashCode();
			return result;
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

	/**
	 * A FoldHandler based on the ServiceManager
	 * @author Matthieu Casanova
	 * @since jEdit 4.3pre10
	 */
	public static class ServiceFoldHandlerProvider implements FoldHandlerProvider
	{
		/**
		 * The service type. See {@link org.gjt.sp.jedit.ServiceManager}.
		 * @since jEdit 4.3pre10
		 */
		public static final String SERVICE = "org.gjt.sp.jedit.buffer.FoldHandler";

		/**
		 * Returns the fold handler with the specified name, or null if
		 * there is no registered handler with that name.
		 * @param name The name of the desired fold handler
		 * @return the FoldHandler or null if it doesn't exist
		 * @since jEdit 4.3pre10
		 */
		@Override
		public FoldHandler getFoldHandler(String name)
		{
			FoldHandler handler = (FoldHandler) getService(SERVICE,name);
			return handler;
		}

		/**
		 * Returns an array containing the names of all registered fold
		 * handlers.
		 *
		 * @since jEdit 4.3pre10
		 */
		@Override
		public String[] getFoldModes()
		{
			String[] handlers = getServiceNames(SERVICE);
			Arrays.sort(handlers,new StandardUtilities.StringCompare<String>());
			return handlers;
		}
	}
}
