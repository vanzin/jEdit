/*
 * EditPlugin.java - Abstract class all plugins must implement
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

import javax.swing.JMenuItem;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.util.Log;

/**
 * The abstract base class that every plugin must implement.<p>
 *
 * Each plugin must have the following properties defined in its property file:
 *
 * <ul>
 * <li><code>plugin.<i>class name</i>.name</code></li>
 * <li><code>plugin.<i>class name</i>.version</code></li>
 * <li><code>plugin.<i>class name</i>.jars</code> - only needed if your plugin
 * bundles external JAR files. Contains a whitespace-separated list of JAR
 * file names. Without this property, the plugin manager will leave behind the
 * external JAR files when removing the plugin.</li>
 * </ul>
 *
 * The following properties are optional but recommended:
 *
 * <ul>
 * <li><code>plugin.<i>class name</i>.author</code></li>
 * <li><code>plugin.<i>class name</i>.docs</code> - the path to plugin
 * documentation in HTML format within the JAR file.</li>
 * </ul>
 *
 * Plugin dependencies are also specified using properties.
 * Each dependency is defined in a property named with
 * <code>plugin.<i>class name</i>.depend.</code> followed by a number.
 * Dependencies must be numbered in order, starting from zero.<p>
 *
 * The value of a dependency property has one of the following forms:
 *
 * <ul>
 * <li><code>jdk <i>minimum Java version</i></code></li>
 * <li><code>jedit <i>minimum jEdit version</i></code> - note that this must be a
 * version number in the form returned by {@link jEdit#getBuild()},
 * not {@link jEdit#getVersion()}.</li>
 * <li><code>plugin <i>plugin</i> <i>version</i></code> - the fully quailified
 * plugin class name must be specified.</li>
 * </ul>
 *
 * Here is an example set of plugin properties:
 *
 * <pre>plugin.QuickNotepadPlugin.name=QuickNotepad
 *plugin.QuickNotepadPlugin.author=John Gellene
 *plugin.QuickNotepadPlugin.version=4.1
 *plugin.QuickNotepadPlugin.docs=QuickNotepad.html
 *plugin.QuickNotepadPlugin.depend.0=jedit 04.01.01.00</pre>
 *
 * Note that in all cases above where a class name is needed, the fully
 * qualified class name, including the package name, if any, must be used.<p>
 *
 * Alternatively, instead of extending this class, a plugin core class can
 * extend {@link EBPlugin} to automatically receive EditBus messages.
 *
 * @see org.gjt.sp.jedit.jEdit#getProperty(String)
 * @see org.gjt.sp.jedit.jEdit#getPlugin(String)
 * @see org.gjt.sp.jedit.jEdit#getPlugins()
 * @see org.gjt.sp.jedit.jEdit#getPluginJAR(String)
 * @see org.gjt.sp.jedit.jEdit#getPluginJARs()
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 * @since jEdit 2.1pre1
 */
public abstract class EditPlugin
{
	//{{{ start() method
	/**
	 * The jEdit startup routine calls this method for each loaded
	 * plugin.
	 *
	 * This method should return as quickly as possible to avoid
	 * slowing down jEdit startup.<p>
	 *
	 * The default implementation does nothing.
	 *
	 * @since jEdit 2.1pre1
	 */
	public void start() {}
	//}}}

	//{{{ stop() method
	/**
	 * The jEdit exit routine calls this method fore ach loaded plugin.
	 *
	 * If a plugin uses state information or other persistent data
	 * that should be stored in a special format, this would be a good place
	 * to write the data to storage.  If the plugin uses jEdit's properties
	 * API to hold settings, no special processing is needed for them on
	 * exit, since they will be saved automatically.<p>
	 *
	 * The default implementation does nothing.
	 *
	 * @since jEdit 2.1pre1
	 */
	public void stop() {} //}}}

	//{{{ createMenuItems() method
	/**
	 * When a {@link View} object is created, it calls this
	 * method on each plugin class to obtain entries to be displayed
	 * in the view's <b>Plugins</b> menu.
	 *
	 * The <code>menuItems</code> vector accumulates menu items and
	 * menus as it is passed from plugin to plugin.<p>
	 *
	 * The easiest way to provide menu items is to
	 * package them as entries in the plugin's property
	 * file and implement <code>createMenuItems()</code> with a
	 * call to the {@link GUIUtilities#loadMenu(String)}
	 * method:
	 * <pre>public void createMenuItems(Vector menuItems)
	 *{
	 *    menuItems.addElement(GUIUtilities.loadMenu(
	 *        "myplugin.menu"));
	 *}</pre>
	 *
	 * Alternatively, {@link GUIUtilities#loadMenuItem(String)} can
	 * be used if your plugin only defines one menu item.<p>
	 *
	 * The default implementation does nothing.
	 *
	 * @param menuItems Add menus and menu items here.
	 *
	 * @see GUIUtilities#loadMenu(String)
	 * @see GUIUtilities#loadMenuItem(String)
	 *
	 * @since jEdit 2.6pre5
	 */
	public void createMenuItems(Vector menuItems) {} //}}}

	//{{{ createOptionPanes() method
	/**
	 * When the <b>Global Options</b> dialog is opened, this method is
	 * called for each plugin in turn.
	 *
	 * To show an option pane, the plugin should define an
	 * option pane class and implement <code>createOptionPane()</code>
	 * as follows:
	 *
	 * <pre>public void createOptionPanes(OptionsDialog optionsDialog)
	 *{
	 *    dialog.addOptionPane(new MyPluginOptionPane());
	 *}</pre>
	 *
	 * Plugins can also define more than one option pane, grouped in an
	 * "option group". See the documentation for the {@link OptionGroup}
	 * class for information.<p>
	 *
	 * The default implementation does nothing.
	 *
	 * @param optionsDialog The plugin options dialog box
	 *
	 * @see OptionPane
	 * @see AbstractOptionPane
	 * @see OptionsDialog#addOptionPane(OptionPane)
	 * @see OptionGroup
	 * @see OptionsDialog#addOptionGroup(OptionGroup)
	 *
	 * @since jEdit 2.1pre1
	 */
	public void createOptionPanes(OptionsDialog optionsDialog) {} //}}}

	//{{{ getClassName() method
	/**
	 * Returns the plugin's class name.
	 *
	 * @since jEdit 2.5pre3
	 */
	public String getClassName()
	{
		return getClass().getName();
	} //}}}

	//{{{ getJAR() method
	/**
	 * Returns the JAR file containing this plugin.
	 * @since jEdit 3.1pre5
	 */
	public EditPlugin.JAR getJAR()
	{
		return jar;
	} //}}}

	//{{{ createMenuItems() method
	/**
	 * Loads menu items from the
	 * <code>plugin.<i>class name</i>.menu</code> property.<p>
	 *
	 * If this
	 * property only lists one menu item, then this item is returned;
	 * otherwise the multiple items are placed inside a single menu
	 * with the plugin's name as the label, and this menu is returned.<p>
	 *
	 * If the property is not defined, this method returns null.<p>
	 *
	 * Do not override this method; define the above mentioned property
	 * instead.
	 *
	 * @since jEdit 4.2pre1
	 */
	public final JMenuItem createMenuItems()
	{
		if(this instanceof Broken)
			return null;

		String menuItemName = jEdit.getProperty("plugin." +
			getClassName() + ".menu-item");
		if(menuItemName != null)
			return GUIUtilities.loadMenuItem(menuItemName);

		String menuItemNames = jEdit.getProperty("plugin." +
			getClassName() + ".menu");
		if(menuItemNames != null)
		{
			String pluginName = jEdit.getProperty("plugin." +
				getClassName() + ".name");
			return new EnhancedMenu(menuItemNames,pluginName);
		}

		return null;
	} //}}}

	//{{{ Broken class
	/**
	 * A placeholder for a plugin that didn't load.
	 */
	public static class Broken extends EditPlugin
	{
		public String getClassName()
		{
			return clazz;
		}

		// package-private members
		Broken(String clazz)
		{
			this.clazz = clazz;
		}

		// private members
		private String clazz;
	} //}}}

	//{{{ JAR class
	/**
	 * A JAR file.
	 */
	public static class JAR
	{
		//{{{ getPath() method
		public String getPath()
		{
			return path;
		} //}}}

		//{{{ getClassLoader() method
		public JARClassLoader getClassLoader()
		{
			return classLoader;
		} //}}}

		//{{{ getZipFile() method
		public ZipFile getZipFile()
		{
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

		//{{{ addPlugin() method
		public void addPlugin(EditPlugin plugin)
		{
			plugin.jar = JAR.this;

			long start = System.currentTimeMillis();

			try
			{
				// must be before the below two so that if an error
				// occurs during start, the plugin is not listed as
				// being active
				plugin.start();
			}
			finally
			{
				Log.log(Log.DEBUG,this,"-- startup took " +
					(System.currentTimeMillis() - start)
					+ " milliseconds");
			}

			if(plugin instanceof EBPlugin)
				EditBus.addToBus((EBPlugin)plugin);

			plugins.add(plugin);
		} //}}}

		//{{{ getPlugins() method
		public EditPlugin[] getPlugins()
		{
			return (EditPlugin[])plugins.toArray(new EditPlugin[plugins.size()]);
		} //}}}

		//{{{ Package-private members

		//{{{ JAR constructor
		JAR(String path, JARClassLoader classLoader, ZipFile zipFile)
		{
			this.path = path;
			this.classLoader = classLoader;
			this.zipFile = zipFile;
			plugins = new ArrayList();
			actions = new ActionSet();
		} //}}}

		//{{{ JAR constructor
		JAR(String path, JARClassLoader classLoader, ZipFile zipFile,
			ResourceCache.PluginCacheEntry cache)
		{
			this(path,classLoader,zipFile);
			if(cache != null)
			{
				
			}
		} //}}}

		//{{{ getPlugins() method
		void getPlugins(Vector vector)
		{
			for(int i = 0; i < plugins.size(); i++)
			{
				vector.addElement(plugins.get(i));
			}
		} //}}}

		//{{{ loadCache() method
		void loadCache(ResourceCache.PluginCacheEntry cache)
		{
			if(cache.actionsURI != null)
			{
				actions = new ActionSet(this,
					cache.actionsURI,
					cache.cachedActionNames);
			}
			if(cache.dockablesURI != null)
			{
				DockableWindowManager.cacheDockableWindows(this,
					cache.dockablesURI,
					cache.cachedDockableNames,
					cache.cachedDockableActionFlags);
			}
		} //}}}

		//{{{ generateCache() method
		ResourceCache.PluginCacheEntry generateCache()
			throws IOException
		{
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
					URL actionsURI = classLoader.getResource(name);
					actions = new ActionSet(
						this,
						actionsURI,
						null);
					actions.load();
					jEdit.addActionSet(actions);

					cache.actionsURI = actionsURI;
					cache.cachedActionNames = actions.getActionNames();
					//XXX: dockable actions
				}
				else if(lname.equals("dockables.xml"))
				{
					URL dockablesURI = classLoader.getResource(name);
					DockableWindowManager.loadDockableWindows(this,
						dockablesURI);

					cache.dockablesURI = dockablesURI;
					//cache.cachedDockableNames = 
					//cache.cachedDockableActionFlags =
				}
				else if(lname.equals("services.xml"))
				{
					URL servicesURI = classLoader.getResource(name);
					ServiceManager.loadServices(this,servicesURI);
					//XXX: cache
				}
				else if(lname.endsWith(".props"))
					properties.add(classLoader.getResource(name));
				else if(name.endsWith(".class"))
					classes.add(MiscUtilities.fileToClass(name));
			}

			return cache;
		} //}}}

		//{{{ getPropertyFiles() method
		List getPropertyFiles()
		{
			return properties;
		} //}}}

		//{{{ getClassSet() method
		Set getClassSet()
		{
			return classes;
		} //}}}

		//}}}

		//{{{ Private members
		private String path;
		private JARClassLoader classLoader;
		private ZipFile zipFile;
		private List plugins;
		private List properties;
		private Set classes;
		private ActionSet actions;
		//}}}
	} //}}}

	//{{{ Private members
	private EditPlugin.JAR jar;
	//}}}
}
