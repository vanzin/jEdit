/*
 * PluginJAR.java - Controls JAR loading and unloading
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.util.Log;

/**
 * A JAR file.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1
 */
public class PluginJAR
{
	//{{{ getPath() method
	public String getPath()
	{
		return path;
	} //}}}

	//{{{ getClassLoader() method
	/**
	 * Returns the plugin's class loader.
	 * @since jEdit 4.2pre1
	 */
	public JARClassLoader getClassLoader()
	{
		return classLoader;
	} //}}}

	//{{{ getZipFile() method
	/**
	 * Returns the plugin's JAR file, opening it first if necessary.
	 * @since jEdit 4.2pre1
	 */
	public ZipFile getZipFile() throws IOException
	{
		if(zipFile == null)
			zipFile = new ZipFile(path);
		return zipFile;
	} //}}}

	//{{{ getActions() method
	/**
	 * @deprecated Call getActionSet() instead
	 */
	public ActionSet getActions()
	{
		return getActionSet();
	} //}}}

	//{{{ getActionSet() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public ActionSet getActionSet()
	{
		return actions;
	} //}}}

	//{{{ getBrowserActionSet() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public ActionSet getBrowserActionSet()
	{
		return browserActions;
	} //}}}

	//{{{ getPlugin() method
	/**
	 * Returns the plugin core class for this JAR file.
	 * @since jEdit 4.2pre1
	 */
	public EditPlugin getPlugin()
	{
		return plugin;
	} //}}}

	//{{{ activatePlugin() method
	/**
	 * Loads the plugin core class if it is not already loaded.
	 * @since jEdit 4.2pre1
	 */
	public void activatePlugin()
	{
		synchronized(this)
		{
			if(activated)
			{
				// recursive call
				return;
			}

			activated = true;

			if(!(plugin instanceof EditPlugin.Deferred && plugin != null))
				return;
		}

		String className = plugin.getClassName();

		if(!checkDependencies())
		{
			plugin = new EditPlugin.Broken(className);
			plugin.jar = (EditPlugin.JAR)this;
			return;
		}

		try
		{
			Class clazz = classLoader.loadClass(className,false);
			int modifiers = clazz.getModifiers();
			if(Modifier.isInterface(modifiers)
				|| Modifier.isAbstract(modifiers)
				|| !EditPlugin.class.isAssignableFrom(clazz))
			{
				// not a real plugin core class
				plugin = null;
				return;
			}

			plugin = (EditPlugin)clazz.newInstance();
			plugin.jar = (EditPlugin.JAR)this;

			plugin.start();

			if(plugin instanceof EBPlugin)
				EditBus.addToBus((EBPlugin)plugin);

			EditBus.send(new PluginUpdate(this,PluginUpdate.ACTIVATED));
		}
		catch(Throwable t)
		{
			plugin = new EditPlugin.Broken(className);
			plugin.jar = (EditPlugin.JAR)this;

			Log.log(Log.ERROR,this,"Error while starting plugin " + className);
			Log.log(Log.ERROR,this,t);
			String[] args = { t.toString() };
			jEdit.pluginError(path,"plugin-error.start-error",args);
		}
	} //}}}

	//{{{ getDockablesURI() method
	/**
	 * Returns the location of the plugin's
	 * <code>dockables.xml</code> file.
	 * @since jEdit 4.2pre1
	 */
	public URL getDockablesURI()
	{
		return dockablesURI;
	} //}}}

	//{{{ getServicesURI() method
	/**
	 * Returns the location of the plugin's
	 * <code>services.xml</code> file.
	 * @since jEdit 4.2pre1
	 */
	public URL getServicesURI()
	{
		return servicesURI;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		if(plugin == null)
			return path;
		else
			return path + ",class=" + plugin.getClassName();
	} //}}}

	//{{{ Package-private members

	//{{{ PluginJAR constructor
	PluginJAR(String path)
	{
		this.path = path;
		classLoader = new JARClassLoader(this);
		actions = new ActionSet();
	} //}}}

	//{{{ init() method
	void init()
	{
		ResourceCache.PluginCacheEntry cache
			= ResourceCache.getPluginCache(path);
		if(cache != null)
			loadCache(cache);
		else
		{
			try
			{
				cache = generateCache();
				ResourceCache.setPluginCache(path,cache);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,"Cannot load"
					+ " plugin " + plugin);
				Log.log(Log.ERROR,this,io);

				String[] args = { io.toString() };
				jEdit.pluginError(path,"plugin-error.load-error",args);
			}
		}

		EditBus.send(new PluginUpdate(this,PluginUpdate.LOADED));
	} //}}}

	//{{{ activateIfNecessary() method
	void activatePluginIfNecessary()
	{
		if(!(plugin instanceof EditPlugin.Deferred && plugin != null))
			return;

		String className = plugin.getClassName();

		// default for plugins that don't specify this property (ie,
		// 4.1-style plugins) is to load them on startup
		String activate = jEdit.getProperty("plugin."
			+ className + ".activate","startup");
		// if at least one property listed here is true, load the
		// plugin
		boolean load = false;

		StringTokenizer st = new StringTokenizer(activate);
		while(st.hasMoreTokens())
		{
			String prop = st.nextToken();            
			boolean value = jEdit.getBooleanProperty(prop);
			if(value)
			{
				Log.log(Log.DEBUG,this,"Activating "
					+ className + " because of " + prop);
				load = true;
				break;
			}
		}

		if(load)
			activatePlugin();
	} //}}}

	//{{{ loadProperties() method
	void loadProperties() throws IOException
	{
		if(propertiesLoaded)
			return;

		propertiesLoaded = true;

		Iterator iter = properties.iterator();
		while(iter.hasNext())
		{
			URL propFile = (URL)iter.next();
			jEdit.loadProps(
				propFile.openStream(),
				true);
		}
	} //}}}

	//{{{ getPropertyFiles() method
	List getPropertyFiles()
	{
		return properties;
	} //}}}

	//{{{ getClasses() method
	List getClasses()
	{
		return classes;
	} //}}}

	//{{{ closeZipFile() method
	/**
	 * Closes the ZIP file. This plugin will no longer be usable
	 * after this.
	 * @since jEdit 4.2pre1
	 */
	public void closeZipFile()
	{
		if(zipFile == null)
			return;

		try
		{
			zipFile.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}

		zipFile = null;
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private String path;
	private JARClassLoader classLoader;
	private ZipFile zipFile;
	private List properties;
	private List classes;
	private ActionSet actions;
	private ActionSet browserActions;

	private EditPlugin plugin;

	private URL dockablesURI;
	private URL servicesURI;

	private boolean propertiesLoaded;
	private boolean activated;
	//}}}

	//{{{ loadCache() method
	private void loadCache(ResourceCache.PluginCacheEntry cache)
	{
		properties = cache.properties;
		classes = cache.classes;

		if(cache.actionsURI != null)
		{
			actions = new ActionSet(this,
				cache.cachedActionNames,
				cache.actionsURI);
		}
		if(cache.browserActionsURI != null)
		{
			browserActions = new ActionSet(this,
				cache.cachedBrowserActionNames,
				cache.browserActionsURI);
		}
		if(cache.dockablesURI != null)
		{
			DockableWindowManager.cacheDockableWindows(this,
				cache.cachedDockableNames,
				cache.cachedDockableActionFlags);
		}
		if(cache.servicesURI != null)
		{
			for(int i = 0; i < cache.cachedServices.length;
				i++)
			{
				ServiceManager.Descriptor d
					= cache.cachedServices[i];
				ServiceManager.registerService(d);
			}
		}
	} //}}}

	//{{{ generateCache() method
	private ResourceCache.PluginCacheEntry generateCache()
		throws IOException
	{
		properties = new LinkedList();
		classes = new LinkedList();

		//XXX: need to unload action set, dockables, services
		// if plugin core class didn't load.
		ZipFile zipFile = getZipFile();

		List plugins = new LinkedList();

		ResourceCache.PluginCacheEntry cache
			= new ResourceCache.PluginCacheEntry();

		Enumeration entries = zipFile.entries();
		while(entries.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)
				entries.nextElement();
			String name = entry.getName();
			String lname = name.toLowerCase();
			if(lname.equals("actions.xml"))
			{
				cache.actionsURI = classLoader.getResource(name);
			}
			if(lname.equals("browser.actions.xml"))
			{
				cache.browserActionsURI = classLoader.getResource(name);
			}
			else if(lname.equals("dockables.xml"))
			{
				dockablesURI = classLoader.getResource(name);
				cache.dockablesURI = dockablesURI;
			}
			else if(lname.equals("services.xml"))
			{
				servicesURI = classLoader.getResource(name);
				cache.servicesURI = servicesURI;
			}
			else if(lname.endsWith(".props"))
				properties.add(classLoader.getResource(name));
			else if(name.endsWith(".class"))
			{
				String className = MiscUtilities
					.fileToClass(name);
				if(className.endsWith("Plugin"))
				{
					// Check if a plugin with the same name
					// is already loaded
					if(jEdit.getPlugin(className) != null)
					{
						jEdit.pluginError(path,
							"plugin-error.already-loaded",
							null);
					}
					else
					{
						plugins.add(className);
					}
				}
				classes.add(className);
			}
		}

		cache.properties = properties;
		cache.classes = classes;

		loadProperties();

		String label = null;

		Iterator iter = plugins.iterator();
		while(iter.hasNext())
		{
			String className = (String)iter.next();
			String _label = jEdit.getProperty("plugin."
				+ className + ".name");
			String version = jEdit.getProperty("plugin."
				+ className + ".version");
			if(_label == null || version == null)
			{
				Log.log(Log.NOTICE,this,"Ignoring: " + className);
			}
			else
			{
				plugin = new EditPlugin.Deferred(className);
				generateCacheForPluginCoreClass(className,cache);
				label = _label;
				break;
			}
		}

		if(cache.actionsURI != null)
		{
			actions = new ActionSet(this,null,cache.actionsURI);
			actions.setLabel(jEdit.getProperty(
				"action-set.plugin",
				new String[] { label }));
			actions.load();
			jEdit.addActionSet(actions);
			cache.cachedActionNames =
				actions.getCacheableActionNames();
		}

		if(cache.browserActionsURI != null)
		{
			browserActions = new ActionSet(this,null,
				cache.browserActionsURI);
			browserActions.load();
			VFSBrowser.getActionContext().addActionSet(browserActions);
			cache.cachedBrowserActionNames =
				browserActions.getCacheableActionNames();
		}

		if(dockablesURI != null)
		{
			DockableWindowManager.loadDockableWindows(this,
				dockablesURI);
			//XXX: filling out cache fields
		}

		if(servicesURI != null)
		{
			ServiceManager.loadServices(this,servicesURI);
		}

		return cache;
	} //}}}

	//{{{ checkDependencies() method
	private boolean checkDependencies()
	{
		int i = 0;

		boolean ok = true;

		String name = plugin.getClassName();

		String dep;
		while((dep = jEdit.getProperty("plugin." + name + ".depend." + i++)) != null)
		{
			int index = dep.indexOf(' ');
			if(index == -1)
			{
				Log.log(Log.ERROR,this,name + " has an invalid"
					+ " dependency: " + dep);
				return false;
			}

			String what = dep.substring(0,index);
			String arg = dep.substring(index + 1);

			if(what.equals("jdk"))
			{
				if(MiscUtilities.compareStrings(
					System.getProperty("java.version"),
					arg,false) < 0)
				{
					String[] args = { arg,
						System.getProperty("java.version") };
					jEdit.pluginError(path,"plugin-error.dep-jdk",args);
					ok = false;
				}
			}
			else if(what.equals("jedit"))
			{
				if(arg.length() != 11)
				{
					Log.log(Log.ERROR,this,"Invalid jEdit version"
						+ " number: " + arg);
					ok = false;
				}

				if(MiscUtilities.compareStrings(
					jEdit.getBuild(),arg,false) < 0)
				{
					String needs = MiscUtilities.buildToVersion(arg);
					String[] args = { needs,
						jEdit.getVersion() };
					jEdit.pluginError(path,
						"plugin-error.dep-jedit",args);
					ok = false;
				}
			}
			else if(what.equals("plugin"))
			{
				int index2 = arg.indexOf(' ');
				if(index2 == -1)
				{
					Log.log(Log.ERROR,this,name 
						+ " has an invalid dependency: "
						+ dep + " (version is missing)");
					return false;
				}
				
				String plugin = arg.substring(0,index2);
				String needVersion = arg.substring(index2 + 1);
				String currVersion = jEdit.getProperty("plugin." 
					+ plugin + ".version");

				if(currVersion == null)
				{
					String[] args = { needVersion, plugin };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin.no-version",
						args);
					ok = false;
				}
				else if(MiscUtilities.compareStrings(currVersion,
					needVersion,false) < 0)
				{
					String[] args = { needVersion, plugin, currVersion };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin",args);
					ok = false;
				}
				else if(jEdit.getPlugin(plugin) instanceof EditPlugin.Broken)
				{
					String[] args = { plugin };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin.broken",args);
					ok = false;
				}
			}
			else if(what.equals("class"))
			{
				try
				{
					classLoader.loadClass(arg,false);
				}
				catch(Exception e)
				{
					String[] args = { arg };
					jEdit.pluginError(path,
						"plugin-error.dep-class",args);
					ok = false;
				}
			}
			else
			{
				Log.log(Log.ERROR,this,name + " has unknown"
					+ " dependency: " + dep);
				return false;
			}
		}

		return ok;
	} //}}}

	//{{{ generateCacheForPluginCoreClass() method
	private void generateCacheForPluginCoreClass(String name,
		ResourceCache.PluginCacheEntry cache)
	{
		// Check if a plugin with the same name is already loaded
		/* EditPlugin[] plugins = jEdit.getPlugins();

		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClass().getName().equals(name))
			{
				jEdit.pluginError(path,
					"plugin-error.already-loaded",null);
				return;
			}
		} */

		/* This is a bit silly... but WheelMouse seems to be
		 * unmaintained so the best solution is to add a hack here.
		 */
		/* if(name.equals("WheelMousePlugin")
			&& OperatingSystem.hasJava14())
		{
			plugins.add(new EditPlugin.Broken(name));
			cache.addBrokenPlugin(name);
			jEdit.pluginError(path,"plugin-error.obsolete",null);
			return;
		} */

		// XXX: this should not be part of the cache stage,
		// full stop!

		// XXX: what if failed dependencies fuck this up
		// XXX: right way is to do full dep check in cache
		// creation, and add dependent plugins to another
		// collection in the cache object
		/* Class clazz = classLoader.loadClass(name,false);
		int modifiers = clazz.getModifiers();
		if(Modifier.isInterface(modifiers)
			|| Modifier.isAbstract(modifiers)
			|| !EditPlugin.class.isAssignableFrom(clazz))
		{
			// not a real plugin core class
			return;
		}

		//XXX: store these in instance vars
		String label = jEdit.getProperty("plugin."
			+ name + ".name");
		String version = jEdit.getProperty("plugin."
			+ name + ".version");

		if(name == null || version == null)
		{
			Log.log(Log.ERROR,this,"Plugin " +
				name + " needs"
				+ " 'name' and 'version' properties.");
			plugins.add(new EditPlugin.Broken(name));
			return;
		}

		// XXX: this is no good
		actionSet.setLabel(jEdit.getProperty(
			"action-set.plugin",
			new String[] { label }));

		plugins.add(new EditPlugin.Deferred(name)); */
	} //}}}

	//}}}
}
