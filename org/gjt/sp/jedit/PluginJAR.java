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
	//{{{ Static methods

	//{{{ getPluginCache() method
	static PluginCacheEntry getPluginCache(PluginJAR plugin)
	{
		String jarCachePath = plugin.getCachePath();
		if(jarCachePath == null)
			return null;

		DataInputStream din = null;
		try
		{
			PluginCacheEntry cache = new PluginCacheEntry();
			cache.plugin = plugin;
			cache.modTime = plugin.getFile().lastModified();
			din = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream(jarCachePath)));
			if(!cache.read(din))
			{
				// returns false with outdated cache
				return null;
			}
			else
				return cache;
		}
		catch(FileNotFoundException fnf)
		{
			return null;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,PluginJAR.class,io);
			return null;
		}
		finally
		{
			try
			{
				if(din != null)
					din.close();
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,PluginJAR.class,io);
			}
		}
	} //}}}

	//{{{ setPluginCache() method
	static void setPluginCache(PluginJAR plugin, PluginCacheEntry cache)
	{
		String jarCachePath = plugin.getCachePath();
		if(jarCachePath == null)
			return;

		Log.log(Log.DEBUG,PluginJAR.class,"Writing " + jarCachePath);

		DataOutputStream dout = null;
		try
		{
			dout = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream(jarCachePath)));
			cache.write(dout);
			try
			{
				if(dout != null)
					dout.close();
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,PluginJAR.class,io);
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,PluginJAR.class,io);
			try
			{
				dout.close();
			}
			catch(IOException io2)
			{
				Log.log(Log.ERROR,PluginJAR.class,io2);
			}
			new File(jarCachePath).delete();
		}
	} //}}}

	//}}}

	//{{{ getPath() method
	public String getPath()
	{
		return path;
	} //}}}

	//{{{ getCachePath() method
	public String getCachePath()
	{
		return cachePath;
	} //}}}

	//{{{ getFile() method
	public File getFile()
	{
		return file;
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
		{
			Log.log(Log.DEBUG,this,"Opening " + path);
			zipFile = new ZipFile(path);
		}
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

		try
		{
			loadProperties();

			if(!checkDependencies())
			{
				plugin = new EditPlugin.Broken(className);
				plugin.jar = (EditPlugin.JAR)this;
				return;
			}

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
	PluginJAR(File file)
	{
		this.path = file.getPath();
		String jarCacheDir = jEdit.getJARCacheDirectory();
		if(jarCacheDir != null)
		{
			cachePath = MiscUtilities.constructPath(
				jarCacheDir,file.getName() + ".summary");
		}
		this.file = file;
		classLoader = new JARClassLoader(this);
		actions = new ActionSet();
	} //}}}

	//{{{ init() method
	void init()
	{
		PluginCacheEntry cache = getPluginCache(this);
		if(cache != null)
			loadCache(cache);
		else
		{
			try
			{
				cache = generateCache();
				setPluginCache(this,cache);
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

	//{{{ uninit() method
	void uninit(boolean exit)
	{
		if(plugin != null)
		{
			try
			{
				plugin.stop();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error while "
					+ "stopping plugin:");
				Log.log(Log.ERROR,this,t);
			}
		}

		if(exit)
			return;

		if(actions != null)
			jEdit.getActionContext().removeActionSet(actions);
		if(browserActions != null)
			VFSBrowser.getActionContext().removeActionSet(browserActions);

		DockableWindowManager.unloadDockableWindows(this);
		ServiceManager.unloadServices(this);

		EditBus.send(new PluginUpdate(this,PluginUpdate.UNLOADED));
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
		if(propertiesLoaded || properties == null)
			return;

		propertiesLoaded = true;

		for(int i = 0; i < properties.length; i++)
		{
			jEdit.loadProps(
				new URL(properties[i]).openStream(),
				true);
		}
	} //}}}

	//{{{ getPropertyFiles() method
	String[] getPropertyFiles()
	{
		return properties;
	} //}}}

	//{{{ getClasses() method
	String[] getClasses()
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
	private String cachePath;
	private File file;

	private JARClassLoader classLoader;
	private ZipFile zipFile;
	private String[] properties;
	private String[] classes;
	private ActionSet actions;
	private ActionSet browserActions;

	private EditPlugin plugin;

	private URL dockablesURI;
	private URL servicesURI;

	private boolean propertiesLoaded;
	private boolean activated;
	//}}}

	//{{{ loadCache() method
	private void loadCache(PluginCacheEntry cache)
	{
		properties = cache.properties;
		classes = cache.classes;

		if(cache.actionsURI != null)
		{
			actions = new ActionSet(this,
				cache.cachedActionNames,
				cache.actionsURI);
			jEdit.addActionSet(actions);
		}
		else
			actions = new ActionSet();

		if(cache.browserActionsURI != null)
		{
			browserActions = new ActionSet(this,
				cache.cachedBrowserActionNames,
				cache.browserActionsURI);
			VFSBrowser.getActionContext().addActionSet(browserActions);
		}

		if(cache.dockablesURI != null)
		{
			dockablesURI = cache.dockablesURI;
			DockableWindowManager.cacheDockableWindows(this,
				cache.cachedDockableNames,
				cache.cachedDockableActionFlags);
		}

		if(cache.servicesURI != null)
		{
			servicesURI = cache.servicesURI;
			for(int i = 0; i < cache.cachedServices.length;
				i++)
			{
				ServiceManager.Descriptor d
					= cache.cachedServices[i];
				ServiceManager.registerService(d);
			}
		}

		Iterator keys = cache.cachedProperties.keySet().iterator();
		while(keys.hasNext())
		{
			String key = (String)keys.next();
			String value = (String)cache.cachedProperties.get(key);
			jEdit.setTemporaryProperty(key,value);
		}

		if(cache.pluginClass != null)
		{
			if(actions != null)
			{
				String label = jEdit.getProperty("plugin."
					+ cache.pluginClass + ".name");
				actions.setLabel(jEdit.getProperty(
					"action-set.plugin",
					new String[] { label }));
			}
			plugin = new EditPlugin.Deferred(cache.pluginClass);
			plugin.jar = (EditPlugin.JAR)this;
		}
	} //}}}

	//{{{ generateCache() method
	private PluginCacheEntry generateCache() throws IOException
	{
		LinkedList properties = new LinkedList();
		LinkedList classes = new LinkedList();

		//XXX: need to unload action set, dockables, services
		// if plugin core class didn't load.
		ZipFile zipFile = getZipFile();

		List plugins = new LinkedList();

		PluginCacheEntry cache = new PluginCacheEntry();
		cache.modTime = file.lastModified();
		cache.cachedProperties = new HashMap();

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
				properties.add(classLoader.getResourceAsPath(name));
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

		this.properties = cache.properties =
			(String[])properties.toArray(
			new String[properties.size()]);
		this.classes = cache.classes =
			(String[])classes.toArray(
			new String[classes.size()]);

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
				plugin.jar = (EditPlugin.JAR)this;
				cache.pluginClass = className;
				cachePluginProperties(className,
					cache.cachedProperties);
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
			actions.cacheProperties(cache.cachedProperties);
			jEdit.addActionSet(actions);
			cache.cachedActionNames =
				actions.getCacheableActionNames();
		}

		if(cache.browserActionsURI != null)
		{
			browserActions = new ActionSet(this,null,
				cache.browserActionsURI);
			browserActions.load();
			browserActions.cacheProperties(cache.cachedProperties);
			VFSBrowser.getActionContext().addActionSet(browserActions);
			cache.cachedBrowserActionNames =
				browserActions.getCacheableActionNames();
		}

		if(dockablesURI != null)
		{
			DockableWindowManager.loadDockableWindows(this,
				dockablesURI,cache);
		}

		if(servicesURI != null)
		{
			ServiceManager.loadServices(this,servicesURI,cache);
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

	//{{{ cachePluginProperties() method
	public void cachePluginProperties(String className,
		Map cachedProperties)
	{
		// this is a kludge, but it means we don't have to even read
		// the properties file on startup, for 4.2 api aware plugins!

		jEdit.putProperty(cachedProperties,"plugin." + className + ".activate");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".name");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".version");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".author");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".docs");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".jars");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".menu");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".menu-item");
		jEdit.putProperty(cachedProperties,"plugin." + className + ".option-pane");

		String paneProp = "plugin." + className + ".option-pane";
		String pane = jEdit.getProperty(paneProp);
		if(pane != null)
		{
			cachedProperties.put(paneProp,pane);
			jEdit.putProperty(cachedProperties,pane + ".label");
			jEdit.putProperty(cachedProperties,pane + ".code");
		}

		String groupProp = "plugin." + className + ".option-group";
		String group = jEdit.getProperty(groupProp);
		if(group != null)
		{
			cachedProperties.put(groupProp,group);
			StringTokenizer st = new StringTokenizer(group);
			while(st.hasMoreTokens())
			{
				pane = st.nextToken();
				jEdit.putProperty(cachedProperties,
					pane + ".label");
				jEdit.putProperty(cachedProperties,
					pane + ".code");
			}
		}
	} //}}}

	//}}}

	//{{{ PluginCacheEntry class
	/**
	 * Used by the <code>DockableWindowManager</code> and
	 * <code>ServiceManager</code> to handle caching.
	 * @since jEdit 4.2pre1
	 */
	public static class PluginCacheEntry
	{
		public static final int MAGIC = 0xB1A3E420;

		//{{{ Instance variables
		public PluginJAR plugin;
		public long modTime;

		public String[] properties;
		public String[] classes;
		public URL actionsURI;
		public String[] cachedActionNames;
		public URL browserActionsURI;
		public String[] cachedBrowserActionNames;
		public URL dockablesURI;
		public String[] cachedDockableNames;
		public boolean[] cachedDockableActionFlags;
		public URL servicesURI;
		public ServiceManager.Descriptor[] cachedServices;

		public Map cachedProperties;
		public String pluginClass;
		//}}}

		/* read() and write() must be kept perfectly in sync...
		 * its a very simple file format. doing it this way is
		 * faster than serializing since serialization calls
		 * reflection, etc. */

		//{{{ read() method
		public boolean read(DataInputStream din) throws IOException
		{
			int cacheMagic = din.readInt();
			if(cacheMagic != MAGIC)
				throw new IOException("Wrong magic number");

			String cacheBuild = readString(din);
			if(!cacheBuild.equals(jEdit.getBuild()))
				return false;

			long cacheModTime = din.readLong();
			if(cacheModTime != modTime)
				return false;

			actionsURI = readURI(din);
			cachedActionNames = readStringArray(din);

			browserActionsURI = readURI(din);
			cachedBrowserActionNames = readStringArray(din);

			dockablesURI = readURI(din);
			cachedDockableNames = readStringArray(din);
			cachedDockableActionFlags = readBooleanArray(din);

			servicesURI = readURI(din);
			int len = din.readInt();
			if(len == 0)
				cachedServices = null;
			else
			{
				cachedServices = new ServiceManager.Descriptor[len];
				for(int i = 0; i < len; i++)
				{
					ServiceManager.Descriptor d = new
						ServiceManager.Descriptor(
						readString(din),
						readString(din),
						null,
						plugin);
					cachedServices[i] = d;
				}
			}

			classes = readStringArray(din);
			properties = readStringArray(din);

			cachedProperties = readMap(din);

			pluginClass = readString(din);

			return true;
		} //}}}

		//{{{ write() method
		public void write(DataOutputStream dout) throws IOException
		{
			dout.writeInt(MAGIC);
			writeString(dout,jEdit.getBuild());

			dout.writeLong(modTime);

			writeString(dout,actionsURI);
			writeStringArray(dout,cachedActionNames);

			writeString(dout,browserActionsURI);
			writeStringArray(dout,cachedBrowserActionNames);

			writeString(dout,dockablesURI);
			writeStringArray(dout,cachedDockableNames);
			writeBooleanArray(dout,cachedDockableActionFlags);

			writeString(dout,servicesURI);
			if(cachedServices == null)
				dout.writeInt(0);
			else
			{
				dout.writeInt(cachedServices.length);
				for(int i = 0; i < cachedServices.length; i++)
				{
					writeString(dout,cachedServices[i].clazz);
					writeString(dout,cachedServices[i].name);
				}
			}

			writeStringArray(dout,classes);
			writeStringArray(dout,properties);

			writeMap(dout,cachedProperties);

			writeString(dout,pluginClass);
		} //}}}

		//{{{ Private members

		//{{{ readString() method
		private String readString(DataInputStream din)
			throws IOException
		{
			int len = din.readInt();
			if(len == 0)
				return null;
			char[] str = new char[len];
			for(int i = 0; i < len; i++)
				str[i] = din.readChar();
			return new String(str);
		} //}}}

		//{{{ readURI() method
		private URL readURI(DataInputStream din)
			throws IOException
		{
			String str = readString(din);
			if(str == null)
				return null;
			else
				return new URL(str);
		} //}}}

		//{{{ readStringArray() method
		private String[] readStringArray(DataInputStream din)
			throws IOException
		{
			int len = din.readInt();
			if(len == 0)
				return null;
			String[] str = new String[len];
			for(int i = 0; i < len; i++)
			{
				str[i] = readString(din);
			}
			return str;
		} //}}}

		//{{{ readBooleanArray() method
		private boolean[] readBooleanArray(DataInputStream din)
			throws IOException
		{
			int len = din.readInt();
			if(len == 0)
				return null;
			boolean[] bools = new boolean[len];
			for(int i = 0; i < len; i++)
			{
				bools[i] = din.readBoolean();
			}
			return bools;
		} //}}}

		//{{{ readMap() method
		private Map readMap(DataInputStream din) throws IOException
		{
			HashMap returnValue = new HashMap();
			int count = din.readInt();
			for(int i = 0; i < count; i++)
			{
				returnValue.put(readString(din),readString(din));
			}
			return returnValue;
		} //}}}

		//{{{ writeString() method
		private void writeString(DataOutputStream dout,
			Object obj) throws IOException
		{
			if(obj == null)
			{
				dout.writeInt(0);
			}
			else
			{
				String str = obj.toString();
				dout.writeInt(str.length());
				dout.writeChars(str);
			}
		} //}}}

		//{{{ writeStringArray() method
		private void writeStringArray(DataOutputStream dout,
			String[] str) throws IOException
		{
			if(str == null)
			{
				dout.writeInt(0);
			}
			else
			{
				dout.writeInt(str.length);
				for(int i = 0; i < str.length; i++)
				{
					writeString(dout,str[i]);
				}
			}
		} //}}}

		//{{{ writeBooleanArray() method
		private void writeBooleanArray(DataOutputStream dout,
			boolean[] bools) throws IOException
		{
			if(bools == null)
			{
				dout.writeInt(0);
			}
			else
			{
				dout.writeInt(bools.length);
				for(int i = 0; i < bools.length; i++)
				{
					dout.writeBoolean(bools[i]);
				}
			}
		} //}}}

		//{{{ writeMap() method
		private void writeMap(DataOutputStream dout, Map map)
			throws IOException
		{
			dout.writeInt(map.size());
			Iterator iter = map.keySet().iterator();
			while(iter.hasNext())
			{
				String key = (String)iter.next();
				writeString(dout,key);
				writeString(dout,map.get(key));
			}
		} //}}}

		//}}}
	} //}}}
}
