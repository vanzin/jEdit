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

//{{{ Imports
import javax.swing.SwingUtilities;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Loads and unloads plugins.<p>
 *
 * <h3>JAR file contents</h3>
 *
 * When loading a plugin, jEdit looks for the following resources:
 *
 * <ul>
 * <li>A file named <code>actions.xml</code> defining plugin actions.
 * Only one such file per plugin is allowed. See {@link ActionSet} for
 * syntax.</li>
 * <li>A file named <code>browser.actions.xml</code> defining file system
 * browser actions.
 * Only one such file per plugin is allowed. See {@link ActionSet} for
 * syntax.</li>
 * <li>A file named <code>dockables.xml</code> defining dockable windows.
 * Only one such file per plugin is allowed. See {@link
 * org.gjt.sp.jedit.gui.DockableWindowManager} for
 * syntax.</li>
 * <li>A file named <code>services.xml</code> defining additional services
 * offered by the plugin, such as virtual file systems.
 * Only one such file per plugin is allowed. See {@link
 * org.gjt.sp.jedit.ServiceManager} for
 * syntax.</li>
 * <li>File with extension <code>.props</code> containing name/value pairs
 * separated by an equals sign.
 * A plugin can supply any number of property files. Property files are used
 * to define plugin men items, plugin option panes, as well as arbitriary
 * settings and strings used by the plugin. See {@link EditPlugin} for
 * information about properties used by jEdit. See
 * <code>java.util.Properties</code> for property file syntax.</li>
 * </ul>
 *
 * For a plugin to actually do something once it is resident in memory,
 * it must contain a class whose name ends with <code>Plugin</code>.
 * This class, known as the <i>plugin core class</i> must extend
 * {@link EditPlugin} and define a few required properties, otherwise it is
 * ignored.
 *
 * <h3>Dynamic and deferred loading</h3>
 *
 * Unlike in prior jEdit versions, jEdit 4.2 and later allow
 * plugins to be added and removed to the resident set at any time using
 * the {@link jEdit#addPluginJAR(String)} and
 * {@link jEdit#removePluginJAR(PluginJAR,boolean)} methods. Furthermore, the
 *  plugin core class might not be loaded until the plugin is first used. See
 * {@link EditPlugin#start()} for a full description.
 *
 * @see org.gjt.sp.jedit.jEdit#getProperty(String)
 * @see org.gjt.sp.jedit.jEdit#getPlugin(String)
 * @see org.gjt.sp.jedit.jEdit#getPlugins()
 * @see org.gjt.sp.jedit.jEdit#getPluginJAR(String)
 * @see org.gjt.sp.jedit.jEdit#getPluginJARs()
 * @see org.gjt.sp.jedit.jEdit#addPluginJAR(String)
 * @see org.gjt.sp.jedit.jEdit#removePluginJAR(PluginJAR,boolean)
 * @see org.gjt.sp.jedit.ActionSet
 * @see org.gjt.sp.jedit.gui.DockableWindowManager
 * @see org.gjt.sp.jedit.OptionPane
 * @see org.gjt.sp.jedit.PluginJAR
 * @see org.gjt.sp.jedit.ServiceManager
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1
 */
public class PluginJAR
{
	//{{{ getPath() method
	/**
	 * Returns the full path name of this plugin's JAR file.
	 */
	public String getPath()
	{
		return path;
	} //}}}

	//{{{ getCachePath() method
	/**
	 * Returns the full path name of this plugin's summary file.
	 * The summary file is used to store certain information which allows
	 * loading of the plugin's resources and core class to be deferred
	 * until the plugin is first used. As long as a plugin is using the
	 * jEdit 4.2 plugin API, no extra effort is required to take advantage
	 * of the summary cache.
	 */
	public String getCachePath()
	{
		return cachePath;
	} //}}}

	//{{{ getFile() method
	/**
	 * Returns a file pointing to the plugin JAR.
	 */
	public File getFile()
	{
		return file;
	} //}}}

	//{{{ getClassLoader() method
	/**
	 * Returns the plugin's class loader.
	 */
	public JARClassLoader getClassLoader()
	{
		return classLoader;
	} //}}}

	//{{{ getZipFile() method
	/**
	 * Returns the plugin's JAR file, opening it if necessary.
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
	 * Returns the plugin's action set for the jEdit action context
	 * {@link jEdit#getActionContext()}. These actions are loaded from
	 * the <code>actions.xml</code> file; see {@link ActionSet}.
	 *.
	 * @since jEdit 4.2pre1
	 */
	public ActionSet getActionSet()
	{
		return actions;
	} //}}}

	//{{{ getBrowserActionSet() method
	/**
	 * Returns the plugin's action set for the file system browser action
	 * context {@link
	 * org.gjt.sp.jedit.browser.VFSBrowser#getActionContext()}.
	 * These actions are loaded from
	 * the <code>browser.actions.xml</code> file; see {@link ActionSet}.
	 *.
	 * @since jEdit 4.2pre1
	 */
	public ActionSet getBrowserActionSet()
	{
		return browserActions;
	} //}}}

	//{{{ checkDependencies() method
	/**
	 * Returns true if all dependencies are satisified, false otherwise.
	 * Also if dependencies are not satisfied, the plugin is marked as
	 * "broken".
	 */
	public boolean checkDependencies()
	{
		if(plugin == null)
			return true;

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
				ok = false;
				continue;
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
					ok = false;
					continue;
				}

				String pluginName = arg.substring(0,index2);
				String needVersion = arg.substring(index2 + 1);
				String currVersion = jEdit.getProperty("plugin." 
					+ pluginName + ".version");

				EditPlugin plugin = jEdit.getPlugin(pluginName);
				if(plugin == null)
				{
					String[] args = { needVersion,
						pluginName };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin.no-version",
						args);
					ok = false;
				}
				else if(MiscUtilities.compareStrings(
					currVersion,needVersion,false) < 0)
				{
					String[] args = { needVersion,
						pluginName, currVersion };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin",args);
					ok = false;
				}
				else if(plugin instanceof EditPlugin.Broken)
				{
					String[] args = { pluginName };
					jEdit.pluginError(path,
						"plugin-error.dep-plugin.broken",args);
					ok = false;
				}
				else
				{
					plugin.getPluginJAR().theseRequireMe.add(path);
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
				ok = false;
			}
		}

		if(!ok)
			breakPlugin();

		return ok;
	} //}}}

	//{{{ getDependentPlugins() method
	/**
	 * Returns an array of all plugins that depend on this one.
	 * @since jEdit 4.2pre2
	 */
	public String[] getDependentPlugins()
	{
		return (String[])theseRequireMe.toArray(
			new String[theseRequireMe.size()]);
	} //}}}

	//{{{ getPlugin() method
	/**
	 * Returns the plugin core class for this JAR file. Note that if the
	 * plugin has not been activated, this will return an instance of
	 * {@link EditPlugin.Deferred}. If you need the actual plugin core
	 * class instance, call {@link #activatePlugin()} first.
	 *
	 * @since jEdit 4.2pre1
	 */
	public EditPlugin getPlugin()
	{
		return plugin;
	} //}}}

	//{{{ activatePlugin() method
	/**
	 * Loads the plugin core class. Does nothing if the plugin core class
	 * has already been loaded. This method might be called on startup,
	 * depending on what properties are set. See {@link EditPlugin#start()}.
	 * This method is thread-safe.
	 *
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

			String className = plugin.getClassName();

			try
			{
				Class clazz = classLoader.loadClass(className,false);
				int modifiers = clazz.getModifiers();
				if(Modifier.isInterface(modifiers)
					|| Modifier.isAbstract(modifiers)
					|| !EditPlugin.class.isAssignableFrom(clazz))
				{
					Log.log(Log.ERROR,this,"Plugin has properties but does not extend EditPlugin: "
						+ className);
					breakPlugin();
					return;
				}

				plugin = (EditPlugin)clazz.newInstance();
				plugin.jar = (EditPlugin.JAR)this;
			}
			catch(Throwable t)
			{
				breakPlugin();
	
				Log.log(Log.ERROR,this,"Error while starting plugin " + className);
				Log.log(Log.ERROR,this,t);
				String[] args = { t.toString() };
				jEdit.pluginError(path,"plugin-error.start-error",args);

				return;
			}
		}

		if(jEdit.isMainThread()
			|| SwingUtilities.isEventDispatchThread())
		{
			startPlugin();
		}
		else
		{
			// for thread safety
			startPluginLater();
		}

		EditBus.send(new PluginUpdate(this,PluginUpdate.ACTIVATED));
	} //}}}

	//{{{ activateIfNecessary() method
	/**
	 * Should be called after a new plugin is installed.
	 * @since jEdit 4.2pre2
	 */
	public void activatePluginIfNecessary()
	{
		if(!(plugin instanceof EditPlugin.Deferred && plugin != null))
			return;

		String className = plugin.getClassName();

		// default for plugins that don't specify this property (ie,
		// 4.1-style plugins) is to load them on startup
		String activate = jEdit.getProperty("plugin."
			+ className + ".activate");

		if(activate == null)
		{
			// 4.1 plugin
			if(!jEdit.isMainThread())
			{
				breakPlugin();

				jEdit.pluginError(path,"plugin-error.not-42",null);
			}
			else
				activatePlugin();
		}
		else
		{
			// 4.2 plugin

			// if at least one property listed here is true,
			// load the plugin
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
		}
	} //}}}

	//{{{ deactivatePlugin() method
	/**
	 * Unloads the plugin core class. Does nothing if the plugin core class
	 * has not been loaded.
	 * This method can only be called from the AWT event dispatch thread!
	 * @see EditPlugin#stop()
	 *
	 * @since jEdit 4.2pre2
	 */
	public void deactivatePlugin()
	{
		synchronized(this)
		{
			if(!activated)
				return;

			activated = false;

			// buffers retain a reference to the fold handler in
			// question... and the easiest way to handle fold
			// handler unloading is this...
			Buffer buffer = jEdit.getFirstBuffer();
			while(buffer != null)
			{
				if(buffer.getFoldHandler() != null
					&& buffer.getFoldHandler().getClass()
					.getClassLoader() == classLoader)
				{
					buffer.setFoldHandler(
						new DummyFoldHandler());
				}
				buffer = buffer.getNext();
			}

			if(plugin != null)
			{
				if(plugin instanceof EBPlugin)
					EditBus.removeFromBus((EBPlugin)plugin);

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

				plugin = new EditPlugin.Deferred(
					plugin.getClassName());
				plugin.jar = (EditPlugin.JAR)this;

				EditBus.send(new PluginUpdate(this,
					PluginUpdate.DEACTIVATED));
			}
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

		classLoader.activate();
	} //}}}

	//{{{ uninit() method
	void uninit(boolean exit)
	{
		deactivatePlugin();

		if(!exit)
		{
			classLoader.deactivate();
			BeanShell.resetClassManager();

			if(actions != null)
				jEdit.getActionContext().removeActionSet(actions);
			if(browserActions != null)
				VFSBrowser.getActionContext().removeActionSet(browserActions);

			DockableWindowManager.unloadDockableWindows(this);
			ServiceManager.unloadServices(this);

			try
			{
				if(zipFile != null)
				{
					zipFile.close();
					zipFile = null;
				}
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
			}
		}
	} //}}}

	//{{{ getClasses() method
	String[] getClasses()
	{
		return classes;
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private String path;
	private String cachePath;
	private File file;

	private JARClassLoader classLoader;
	private ZipFile zipFile;
	private String[] classes;
	private ActionSet actions;
	private ActionSet browserActions;

	private EditPlugin plugin;

	private URL dockablesURI;
	private URL servicesURI;

	private boolean activated;

	private List theseRequireMe = new LinkedList();
	//}}}

	//{{{ loadCache() method
	private void loadCache(PluginCacheEntry cache)
	{
		classes = cache.classes;

		if(cache.actionsURI != null
			&& cache.cachedActionNames != null)
		{
			actions = new ActionSet(this,
				cache.cachedActionNames,
				cache.cachedActionToggleFlags,
				cache.actionsURI);
			jEdit.addActionSet(actions);
		}
		else
			actions = new ActionSet();

		if(cache.browserActionsURI != null
			&& cache.cachedBrowserActionNames != null)
		{
			browserActions = new ActionSet(this,
				cache.cachedBrowserActionNames,
				cache.cachedBrowserActionToggleFlags,
				cache.browserActionsURI);
			VFSBrowser.getActionContext().addActionSet(browserActions);
		}

		if(cache.dockablesURI != null
			&& cache.cachedDockableNames != null
			&& cache.cachedDockableActionFlags != null)
		{
			dockablesURI = cache.dockablesURI;
			DockableWindowManager.cacheDockableWindows(this,
				cache.cachedDockableNames,
				cache.cachedDockableActionFlags);
		}

		if(cache.servicesURI != null
			&& cache.cachedServices != null)
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

		if(cache.cachedProperties != null)
			jEdit.addProperties(cache.cachedProperties);

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
		Properties properties = new Properties();

		LinkedList classes = new LinkedList();

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
			{
				InputStream in = classLoader.getResourceAsStream(name);
				properties.load(in);
				in.close();
			}
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

		cache.cachedProperties = properties;
		jEdit.addProperties(properties);

		this.classes = cache.classes =
			(String[])classes.toArray(
			new String[classes.size()]);

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
				label = _label;
				break;
			}
		}

		if(cache.actionsURI != null)
		{
			actions = new ActionSet(this,null,null,
				cache.actionsURI);
			actions.setLabel(jEdit.getProperty(
				"action-set.plugin",
				new String[] { label }));
			actions.load();
			jEdit.addActionSet(actions);
			cache.cachedActionNames =
				actions.getCacheableActionNames();
			cache.cachedActionToggleFlags = new boolean[
				cache.cachedActionNames.length];
			for(int i = 0; i < cache.cachedActionNames.length; i++)
			{
				 cache.cachedActionToggleFlags[i]
				 	= jEdit.getBooleanProperty(
					cache.cachedActionNames[i]
					+ ".toggle");
			}
		}

		if(cache.browserActionsURI != null)
		{
			browserActions = new ActionSet(this,null,null,
				cache.browserActionsURI);
			browserActions.load();
			VFSBrowser.getActionContext().addActionSet(browserActions);
			cache.cachedBrowserActionNames =
				browserActions.getCacheableActionNames();
			cache.cachedBrowserActionToggleFlags = new boolean[
				cache.cachedBrowserActionNames.length];
			for(int i = 0;
				i < cache.cachedBrowserActionNames.length;
				i++)
			{
				 cache.cachedBrowserActionNames[i]
				 	= jEdit.getProperty(
					cache.cachedBrowserActionNames[i]
					+ ".toggle");
			}
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

	//{{{ startPlugin() method
	private void startPlugin()
	{
		try
		{
			plugin.start();
		}
		catch(Throwable t)
		{
			breakPlugin();

			Log.log(Log.ERROR,PluginJAR.this,
				"Error while starting plugin " + plugin.getClassName());
			Log.log(Log.ERROR,PluginJAR.this,t);
			String[] args = { t.toString() };
			jEdit.pluginError(path,"plugin-error.start-error",args);
		}

		if(plugin instanceof EBPlugin)
		{
			if(jEdit.getProperty("plugin."
				+ plugin.getClassName() + ".activate")
				== null)
			{
				// old plugins expected jEdit 4.1-style
				// behavior, where a PropertiesChanged
				// was sent after plugins were started
				((EBComponent)plugin).handleMessage(
					new org.gjt.sp.jedit.msg.PropertiesChanged(null));
			}
			EditBus.addToBus((EBPlugin)plugin);
		}

		// buffers retain a reference to the fold handler in
		// question... and the easiest way to handle fold
		// handler loading is this...
		Buffer buffer = jEdit.getFirstBuffer();
		while(buffer != null)
		{
			FoldHandler handler =
				FoldHandler.getFoldHandler(
				buffer.getStringProperty("folding"));
			// == null before loaded
			if(null != buffer.getFoldHandler()
				&& handler != buffer.getFoldHandler())
			{
				buffer.setFoldHandler(handler);
			}
			buffer = buffer.getNext();
		}
	} //}}}

	//{{{ startPluginLater() method
	private void startPluginLater()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(!activated)
					return;

				startPlugin();
			}
		});
	} //}}}

	//{{{ breakPlugin() method
	private void breakPlugin()
	{
		plugin = new EditPlugin.Broken(plugin.getClassName());
		plugin.jar = (EditPlugin.JAR)this;

		// remove action sets, dockables, etc so that user doesn't
		// see the broken plugin
		uninit(false);
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
		public static final int MAGIC = 0xB7A2E420;

		//{{{ Instance variables
		public PluginJAR plugin;
		public long modTime;

		public String[] classes;
		public URL actionsURI;
		public String[] cachedActionNames;
		public boolean[] cachedActionToggleFlags;
		public URL browserActionsURI;
		public String[] cachedBrowserActionNames;
		public boolean[] cachedBrowserActionToggleFlags;
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
				return false;

			String cacheBuild = readString(din);
			if(!cacheBuild.equals(jEdit.getBuild()))
				return false;

			long cacheModTime = din.readLong();
			if(cacheModTime != modTime)
				return false;

			actionsURI = readURI(din);
			cachedActionNames = readStringArray(din);
			cachedActionToggleFlags = readBooleanArray(din);

			browserActionsURI = readURI(din);
			cachedBrowserActionNames = readStringArray(din);
			cachedBrowserActionToggleFlags = readBooleanArray(din);

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
			writeBooleanArray(dout,cachedActionToggleFlags);

			writeString(dout,browserActionsURI);
			writeStringArray(dout,cachedBrowserActionNames);
			writeBooleanArray(dout,cachedBrowserActionToggleFlags);

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
				String key = readString(din);
				String value = readString(din);
				if(value == null)
					value = "";
				returnValue.put(key,value);
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
