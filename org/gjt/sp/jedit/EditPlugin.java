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

import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.menu.EnhancedMenu;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.io.*;
import java.util.Vector;

/**
 * The abstract base class that every plugin must implement.
 * Alternatively, instead of extending this class, a plugin core class can
 * extend {@link EBPlugin} to automatically receive EditBus messages.
 *
 * <h3>Basic plugin information properties</h3>
 *
 * Note that in all cases above where a <i>className</i> is needed, the fully
 * qualified class name, including the package name, if any, must be used.<p>
 *
 * The following properties are required for jEdit to load the plugin:
 *
 * <ul>
 * <li><code>plugin.<i>className</i>.activate</code> - set this to
 * <code>defer</code> if your plugin only needs to be loaded when it is first
 * invoked; set it to <code>startup</code> if your plugin must be loaded at
 * startup regardless; set it to a whitespace-separated list of property names
 * if your plugin should be loaded if at least one of these properties is set.
 * Note that if this property is <b>not</b> set, the plugin will not work with
 * jEdit 4.3final.
 * </li>
 * <li><code>plugin.<i>className</i>.name</code></li>
 * <li><code>plugin.<i>className</i>.version</code></li>
 * <li><code>plugin.<i>className</i>.jars</code> - only needed if your plugin
 * bundles external JAR files. Contains a whitespace-separated list of JAR
 * file names. Without this property, the plugin manager will leave behind the
 * external JAR files when removing the plugin.</li>
 * <li><code>plugin.<i>className</i>.files</code> - only needed if your plugin
 * bundles external files like libraries which MUST reside in the local
 * filesystem. Contains a whitespace-separated list of file names.
 * Without this property, the plugin manager will leave behind the
 * external files when removing the plugin.</li>
 * <li><code>plugin.<i>className</i>.description</code> - the short description
 * associated with the plugin.  The short description is used by the Plugin
 * Manager and on the list pages on Plugin Central. </li>
 * </ul>
 *
 * The following properties are optional but recommended:
 *
 * <ul>
 * <li><code>plugin.<i>className</i>.author</code></li>
 * <li><code>plugin.<i>className</i>.usePluginHome</code> - whether
 * the plugin uses the EditPlugin.getPluginHome API or not. Even
 * if the plugin doesn't store any data, this property should be set
 * so that the plugin manager can tell that there is no data stored.</li>
 * <li><code>plugin.<i>className</i>.docs</code> - the path to plugin
 * documentation in HTML format. </li>
 * <li><code>plugin.<i>className</i>.longdescription</code> - the path to
 * the long description in XHTML (no fancy stuff here, please - just proper
 * XHTML subset with the basic tags: <tt>html, h1, h2, p, li, ul, ol, a href,b ,i, u, br/ </tt>)
 * <p> The long description is extracted from the plugin at various times,
 * primarily at plugin packaging time to update the data on the
 * plugin detail pages of Plugin Central. </p>
 * <p>
 * If this property is left out, the default will be to look in a file
 * called &lt;description.html&gt;. </p>
 *</li>
 *</ul>
 *<p>
 * For the previous two properties, if a relative path is supplied,
 * it should be both </p>
 * <ol>
 * <li> relative to the location of the .props file (when it is in the source tree) </li>
 * <li> relative to the root of the JAR (when it is packaged in the JAR file) </li>
 *</ol>
 *
 *<p> Both conditions are easily satisfied if the .props file as well as
 * description.html are both located in the root directory of the plugin,
 * as well as the generated JAR. </p>
 *
 * <h3>Plugin dependency properties</h3>
 *
 * <p>Plugin dependencies are also specified using properties.
 * Each dependency is defined in a property named with
 * <code>plugin.<i>className</i>.depend.</code> followed by a number.
 * Dependencies must be numbered in order, starting from zero.
 * This determines the order that dependent plugins get loaded and activated,
 * so order is very important. </p>
*
 * <p> The value of a dependency property has one of the following forms: </p>
 *
 * <ul>
 * <li> <code>jdk <i>minimumJavaVersion</i></code> </li>
 * <li> <code>jedit <i>minimumjEditVersion</i></code> - note that this must be
 * a version number in the form returned by {@link jEdit#getBuild()},
 * not {@link jEdit#getVersion()}. Note that the documentation here describes
 * the jEdit 4.2 plugin API, so this dependency must be set to at least
 * <code>04.02.99.00</code> (4.2final).</li>
 * <li><code><i>pluginClassName pluginVersion</i></code> - the fully quailified
 * plugin class name with package must be specified.</li>
 * <li><code>optional plugin <i>pluginClassName pluginVersion</i></code> -
 * an optional dependency, indicating that the plugin will work without it,
 * but that the dependency should be loaded before this plugin. </li>
</ul>

 <p>In this example, the ProjectViewer plugin is an optional dependency of
 the Console, beacause the Console only listens to events from the ProjectViewer.
 It requires Jedit 4.2 final. </p>

<pre>
plugin.console.ConsolePlugin.depend.0=jedit 04.02.99.00
plugin.console.ConsolePlugin.depend.1=jdk 1.5
plugin.console.ConsolePlugin.depend.2=plugin errorlist.ErrorListPlugin 1.4
plugin.console.ConsolePlugin.depend.3=optional plugin projectviewer.ProjectPlugin 2.1.0.92
</pre>

 * <h3>Plugin menu item properties</h3>
 *
 *<p> To add your plugin to the view's <b>Plugins</b> menu, define one of these two
 * properties: </p>
 *
 * <ul>
 * <li><code>plugin.<i>className</i>.menu-item</code> - if this is defined,
 * the action named by this property is added to the <b>Plugins</b> menu.</li>
 * <li><code>plugin.<i>className</i>.menu</code> - if this is defined,
 * a sub-menu is added to the <b>Plugins</b> menu whose content is the
 * whitespace-separated list of action names in this property. A separator may
 * be added to the sub-menu by listing <code>-</code> in the property.</li>
 * </ul>
 *
 * <p>If you want the plugin's menu items to be determined at runtime, define a
 * property <code>plugin.<i>className</i>.menu.code</code> to be BeanShell
 * code that evaluates to an implementation of
 * {@link org.gjt.sp.jedit.menu.DynamicMenuProvider}.</p>
 *<p>
 * To add your plugin to the file system browser's <b>Plugins</b> menu, define
 * one of these two properties:
 *</p>
 * <ul>
 * <li><code>plugin.<i>className</i>.browser-menu-item</code> - if this is
 * defined, the action named by this property is added to the <b>Plugins</b>
 * menu.</li>
 * <li><code>plugin.<i>className</i>.browser-menu</code> - if this is defined,
 * a sub-menu is added to the <b>Plugins</b> menu whose content is the
 * whitespace-separated list of action names in this property. A separator may
 * be added to the sub-menu by listing <code>-</code> in the property.</li>
 * </ul>
 *
 *<p> In all cases, each action's
 * menu item label is taken from the <code><i>actionName</i>.label</code>
 * property. View actions are defined in an <code>actions.xml</code>
 * file, file system browser actions are defined in a
 * <code>browser.actions.xml</code> file; see {@link ActionSet}.
 *</p>
 * <h3>Plugin option pane properties</h3>
 *
 * <p>To add your plugin to the <b>Plugin Options</b> dialog box, define one of
 * these two properties:
 *</p>
 * <ul>
 * <li><code>plugin.<i>className</i>.option-pane=<i>paneName</i></code> - if this is defined,
 * a single option pane with this name is added to the <b>Plugin Options</b>
 * menu.</li>
 * <li><code>plugin.<i>className</i>.option-group=<i>paneName1</i> [<i>paneName2 paneName3</i> ...]</code> - if this is defined,
 * a branch node is added to the <b>Plugin Options</b> dialog box whose content
 * is the whitespace-separated list of <i>paneNames</i> in this property.</li>
 * </ul>
 *
 * Then for each option <i>paneName</i>, define these two properties:
 *
 * <ul>
 * <li><code>options.<i>paneName</i>.label</code> - the label to show
 * for the pane in the dialog box.</li>
 * <li><code>options.<i>paneName</i>.code</code> - BeanShell code that
 * evaluates to an instance of the {@link OptionPane} class.</li>
 *
 * <h3>Example</h3>
 *
 * Here is an example set of plugin properties:
 *
 * <pre>plugin.QuickNotepadPlugin.activate=defer
 *plugin.QuickNotepadPlugin.name=QuickNotepad
 *plugin.QuickNotepadPlugin.author=John Gellene
 *plugin.QuickNotepadPlugin.version=4.2
 *plugin.QuickNotepadPlugin.docs=QuickNotepad.html
 *plugin.QuickNotepadPlugin.depend.0=jedit 04.02.01.00
 *plugin.QuickNotepadPlugin.menu=quicknotepad \
 *    - \
 *    quicknotepad.choose-file \
 *    quicknotepad.save-file \
 *    quicknotepad.copy-to-buffer
 *plugin.QuickNotepadPlugin.option-pane=quicknotepad
 *
 * plugin.QuickNotepadPlugin.option-pane=quicknotepad
 * plugin.QuickNotepadPlugin.usePluginHome=false
 * options.quicknotepad.code=new QuickNotepadOptionPane();
 * options.quicknotepad.label=QuickNotepad
 * options.quicknotepad.file=File:
 * options.quicknotepad.choose-file=Choose
 * options.quicknotepad.choose-file.title=Choose a notepad file
 * options.quicknotepad.choose-font=Font:
 * options.quicknotepad.show-filepath.title=Display notepad file path
</pre>
 *
 * Note that action and option pane labels are not shown in the above example.
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
 * @author John Gellene (API documentation)
 * @author Alan Ezust (API documentation)
 * @since jEdit 2.1pre1
 */
public abstract class EditPlugin
{
	//{{{ start() method
	/**
	 * jEdit calls this method when the plugin is being activated, either
	 * during startup or at any other time. A plugin can get activated for
	 * a number of reasons:
	 *
	 * <ul>
	 * <li>The plugin is written for jEdit 4.1 or older, in which case it
	 * will always be loaded at startup.</li>
	 * <li>The plugin has its <code>activate</code> property set to
	 * <code>startup</code>, in which case it will always be loaded at
	 * startup.</li>
	 * <li>One of the properties listed in the plugin's
	 * <code>activate</code> property is set to <code>true</code>,
	 * in which case it will always be loaded at startup.</li>
	 * <li>One of the plugin's classes is being accessed by another plugin,
	 * a macro, or a BeanShell snippet in a plugin API XML file.</li>
	 * </ul>
	 *
	 * Note that this method is always called from the event dispatch
	 * thread, even if the activation resulted from a class being loaded
	 * from another thread. A side effect of this is that some of your
	 * plugin's code might get executed before this method finishes
	 * running.<p>
	 *
	 * When this method is being called for plugins written for jEdit 4.1
	 * and below, no views or buffers are open. However, this is not the
	 * case for plugins using the new API. For example, if your plugin adds
	 * tool bars to views, make sure you correctly handle the case where
	 * views are already open when the plugin is loaded.<p>
	 *
	 * If your plugin must be loaded on startup, take care to have this
	 * method return as quickly as possible.<p>
	 *
	 * The default implementation of this method does nothing.
	 *
	 * @since jEdit 2.1pre1
	 */
	public void start() {}
	//}}}

	//{{{ stop() method
	/**
	 * jEdit calls this method when the plugin is being unloaded. This can
	 * be when the program is exiting, or at any other time.<p>
	 *
	 * If a plugin uses state information or other persistent data
	 * that should be stored in a special format, this would be a good place
	 * to write the data to storage.  If the plugin uses jEdit's properties
	 * API to hold settings, no special processing is needed for them on
	 * exit, since they will be saved automatically.<p>
	 *
	 * With plugins written for jEdit 4.1 and below, this method is only
	 * called when the program is exiting. However, this is not the case
	 * for plugins using the new API. For example, if your plugin adds
	 * tool bars to views, make sure you correctly handle the case where
	 * views are still open when the plugin is unloaded.<p>
	 *
	 * To avoid memory leaks, this method should ensure that no references
	 * to any objects created by this plugin remain in the heap. In the
	 * case of actions, dockable windows and services, jEdit ensures this
	 * automatically. For other objects, your plugin must clean up maually.
	 * <p>
	 *
	 * The default implementation of this method does nothing.
	 *
	 * @since jEdit 2.1pre1
	 */
	public void stop() {} //}}}

	//{{{ getPluginHome() method
	/**
	 * Returns the home of your plugin.
	 *
	 * @return the plugin home. It can be null if there is no 
	 *	   settings directory
	 * @since 4.3pre10
	 * @see #getResourceAsStream
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	public File getPluginHome()
	{
		return getPluginHome(getClassName());
	} //}}}

	//{{{ getPluginHome() method
	/**
	 * <p>Returns the home of the specified plugin.</p>
	 *
	 * <p>Since the first parameter is a reference to the
	 * {@code Class} instance for the plugin,
	 * this method requires the plugin to be activated.</p>
	 *
	 * <p>See {@link #getPluginHome(EditPlugin)} method, as
	 * an alternate, for when the plugin doesn't need
	 * to be activated, or when you do not have the
	 * {@code Class} instance available.</p>
	 *
	 * @param clazz the class of the plugin
	 * @return the plugin home. It can be null if there is no
	 * 	   settings directory
	 * @since 4.3pre10
	 * @see #getPluginHome(EditPlugin)
	 * @see #getResourceAsStream
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	public static File getPluginHome(Class<? extends EditPlugin> clazz)
	{
		return getPluginHome(clazz.getName());
	} //}}}

	//{{{ getPluginHome() method
	/**
	 * <p>Returns the home of the specified plugin.</p>
	 * 
	 * <p>This method doesn't need the plugin to be activated. You can pass
	 * an {@code EditPlugin.Deferred} instance that you get from
	 * {@code jEdit.getPlugin(String)} or {@code jEdit.getPlugins()} if
	 * the plugin in question is not activated yet and this method doesn't
	 * cause the plugin to get activated. If you have a reference to the
	 * plugins {@code Class} instance available, consider using the
	 * {@code Class} method.</p>
	 *
	 * @param plugin the plugin
	 * @return the plugin home. It can be null if there is no settings directory
	 * @since 4.3pre10
	 * @see #getPluginHome(Class)
	 * @see #getResourceAsStream
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	public static File getPluginHome(EditPlugin plugin)
	{
		return getPluginHome(plugin.getClassName());
	} //}}}

	//{{{ getPluginHome() method
	/**
	 * Returns the home of the specified plugin.
	 *
	 * @param pluginClassName the plugin class name (fully qualified)
	 * @return the plugin home. It can be null if there is no settings directory
	 * @since 4.3pre10
	 * @see #getResourceAsStream
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	private static File getPluginHome(String pluginClassName)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if (settingsDirectory == null)
			return null;

		File file = new File(settingsDirectory, "plugins");
		if (!file.isDirectory()) 
		{
			if (!file.mkdir()) 
			{
				Log.log(Log.ERROR, EditPlugin.class, "Can't create directory:" + file.getAbsolutePath());
			}
		}
		return new File(file, pluginClassName);
	} //}}}

	//{{{ getResourceAsStream() method
	/**
	 * <p>Returns an input stream to the specified resource, or {@code null}
	 * if none is found.</p>
	 *
	 * <p>Since the first parameter is a reference to the
	 * {@code Class} instance for the plugin,
	 * this method requires the plugin to be activated.</p>
	 *
	 * <p>See {@link #getResourceAsStream(EditPlugin,String)} method, as
	 * an alternate, for when the plugin doesn't need
	 * to be activated, or when you do not have the
	 * {@code Class} instance available.</p>
	 *
	 * @param clazz the plugin class
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An input stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsStream(EditPlugin,String)
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	public static InputStream getResourceAsStream(Class<? extends EditPlugin> clazz, String path)
	{
		return getResourceAsStream(clazz.getName(), path);
	} //}}}

	//{{{ getResourceAsStream() method
	/**
	 * <p>Returns an input stream to the specified resource, or <code>null</code>
	 * if none is found.</p>
	 * 
	 * <p>This method doesn't need the plugin to be activated. You can pass
	 * an {@code EditPlugin.Deferred} instance that you get from
	 * {@code jEdit.getPlugin(String)} or {@code jEdit.getPlugins()} if
	 * the plugin in question is not activated yet and this method doesn't
	 * cause the plugin to get activated. If you have a reference to the
	 * plugins {@code Class} instance available, consider using the
	 * {@code Class} method.</p>
	 *
	 * @param plugin the plugin
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An input stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsStream(Class,String)
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	public static InputStream getResourceAsStream(EditPlugin plugin, String path)
	{
		return getResourceAsStream(plugin.getClassName(), path);
	} //}}}

	//{{{ getResourceAsStream() method
	/**
	 * Returns an input stream to the specified resource, or <code>null</code>
	 * if none is found.
	 * 
	 * @param pluginClassName the plugin class name (fully qualified)
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An input stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream
	 * @see #getResourcePath
	 */
	private static InputStream getResourceAsStream(String pluginClassName, String path)
	{
		try 
		{
			File file = getResourcePath(pluginClassName, path);
			if (file == null || !file.exists())
				return null;
			return new FileInputStream(file);
		} 
		catch (IOException e)
		{
			return null;
		}
	} //}}}

	//{{{ getResourceAsOutputStream() method
	/**
	 * <p>Returns an output stream to the specified resource, or {@code null}
	 * if access to that resource is denied.</p>
	 *
	 * <p>Since the first parameter is a reference to the
	 * {@code Class} instance for the plugin,
	 * this method requires the plugin to be activated.</p>
	 *
	 * <p>See {@link #getResourceAsOutputStream(EditPlugin,String)} method, as
	 * an alternate, for when the plugin doesn't need
	 * to be activated, or when you do not have the
	 * {@code Class} instance available.</p>
	 *
	 * @param clazz the plugin class
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An output stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream(EditPlugin,String)
	 * @see #getResourceAsStream
	 * @see #getResourcePath
	 */
	public static OutputStream getResourceAsOutputStream(Class<? extends EditPlugin> clazz, String path)
	{
		return getResourceAsOutputStream(clazz.getName(), path);
	} //}}}

	//{{{ getResourceAsOutputStream() method
	/**
	 * <p>Returns an output stream to the specified resource, or <code>null</node> if access
	 * to that resource is denied.</p>
	 *
	 * <p>This method doesn't need the plugin to be activated. You can pass
	 * an {@code EditPlugin.Deferred} instance that you get from
	 * {@code jEdit.getPlugin(String)} or {@code jEdit.getPlugins()} if
	 * the plugin in question is not activated yet and this method doesn't
	 * cause the plugin to get activated. If you have a reference to the
	 * plugins {@code Class} instance available, consider using the
	 * {@code Class} method.</p>
	 *
	 * @param plugin the plugin
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An output stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream(Class,String)
	 * @see #getResourceAsStream
	 * @see #getResourcePath
	 */
	public static OutputStream getResourceAsOutputStream(EditPlugin plugin, String path)
	{
		return getResourceAsOutputStream(plugin.getClassName(), path);
	} //}}}

	//{{{ getResourceAsOutputStream() method
	/**
	 * Returns an output stream to the specified resource, or <code>null</node> if access
	 * to that resource is denied.
	 * 
	 * @param pluginClassName the plugin class name (fully qualified)
	 * @param path The path to the resource to be returned, relative to
	 * the plugin's resource path.
	 * @return An output stream for the resource, or <code>null</code>.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsStream
	 * @see #getResourcePath
	 */
	private static OutputStream getResourceAsOutputStream(String pluginClassName, String path)
	{
		try 
		{
			File file = getResourcePath(pluginClassName, path);
			if (file == null)
				return null;
			File parentFile = file.getParentFile();
			if (!parentFile.exists())
			{
				if (!parentFile.mkdirs())
				{
					Log.log(Log.ERROR, EditPlugin.class, "Unable to create folder " + parentFile.getPath());
					return null;
				}
			}
			return new FileOutputStream(file);
		}
		catch (IOException e)
		{
			return null;
		}
	} //}}}

	//{{{ getResourcePath() method
	/**
	 * <p>Returns the full path of the specified plugin resource.</p>
	 *
	 * <p>Since the first parameter is a reference to the
	 * {@code Class} instance for the plugin,
	 * this method requires the plugin to be activated.</p>
	 *
	 * <p>See {@link #getResourcePath(EditPlugin,String)} method, as
	 * an alternate, for when the plugin doesn't need
	 * to be activated, or when you do not have the
	 * {@code Class} instance available.</p>
	 *
	 * @param clazz the plugin class
	 * @param path The relative path to the resource from the plugin's
	 * resource path.
	 * @return The absolute path to the resource or null if there is no plugin home.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream
	 * @see #getResourceAsStream
	 * @see #getResourcePath(EditPlugin,String)
	 */
	public static File getResourcePath(Class<? extends EditPlugin> clazz, String path)
	{
		return getResourcePath(clazz.getName(), path);
	} //}}}

	//{{{ getResourcePath() method
	/**
	 * <p>Returns the full path of the specified plugin resource.</p>
	 *
	 * <p>This method doesn't need the plugin to be activated. You can pass
	 * an {@code EditPlugin.Deferred} instance that you get from
	 * {@code jEdit.getPlugin(String)} or {@code jEdit.getPlugins()} if
	 * the plugin in question is not activated yet and this method doesn't
	 * cause the plugin to get activated. If you have a reference to the
	 * plugins {@code Class} instance available, consider using the
	 * {@code Class} method.</p>
	 *
	 * @param plugin the plugin
	 * @param path The relative path to the resource from the plugin's
	 * resource path.
	 * @return The absolute path to the resource or null if there is no plugin home.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream
	 * @see #getResourceAsStream
	 * @see #getResourcePath(Class,String)
	 */
	public static File getResourcePath(EditPlugin plugin, String path)
	{
		return getResourcePath(plugin.getClassName(), path);
	} //}}}

	//{{{ getResourcePath() method
	/**
	 * Returns the full path of the specified plugin resource.
	 *
	 * @param pluginClassName the plugin class name (fully qualified)
	 * @param path The relative path to the resource from the plugin's
	 * resource path.
	 * @return The absolute path to the resource or null if there is no plugin home.
	 * @since 4.3pre10
	 * @see #getPluginHome
	 * @see #getResourceAsOutputStream
	 * @see #getResourceAsStream
	 */
	private static File getResourcePath(String pluginClassName, String path)
	{
		File home = getPluginHome(pluginClassName);
		if (home == null)
			return null;
		return new File(home, path);
	} //}}}

	//{{{ getClassName() method
	/**
	 * Returns the plugin's class name. This might not be the same as
	 * the class of the actual <code>EditPlugin</code> instance, for
	 * example if the plugin is not loaded yet.
	 *
	 * @since jEdit 2.5pre3
	 */
	public String getClassName()
	{
		return getClass().getName();
	} //}}}

	//{{{ getPluginJAR() method
	/**
	 * Returns the JAR file containing this plugin.
	 * @since jEdit 4.2pre1
	 */
	public PluginJAR getPluginJAR()
	{
		return jar;
	} //}}}

	//{{{ createMenuItems() method
	/**
	 * Called by the view when constructing its <b>Plugins</b> menu.
	 * See the description of this class for details about how the
	 * menu items are constructed from plugin properties.
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

		String menuProperty = "plugin." + getClassName() + ".menu";
		String codeProperty = "plugin." + getClassName() + ".menu.code";
		if(jEdit.getProperty(menuProperty) != null
			|| jEdit.getProperty(codeProperty) != null)
		{
			String pluginName = jEdit.getProperty("plugin." +
				getClassName() + ".name");
			return new EnhancedMenu(menuProperty,pluginName);
		}

		return null;
	} //}}}

	//{{{ createBrowserMenuItems() method
	/**
	 * Called by the filesystem browser when constructing its
	 * <b>Plugins</b> menu.
	 * See the description of this class for details about how the
	 * menu items are constructed from plugin properties.
	 *
	 * @since jEdit 4.2pre1
	 */
	public final JMenuItem createBrowserMenuItems()
	{
		if(this instanceof Broken)
			return null;

		String menuItemName = jEdit.getProperty("plugin." +
			getClassName() + ".browser-menu-item");
		if(menuItemName != null)
		{
			return GUIUtilities.loadMenuItem(
				VFSBrowser.getActionContext(),
				menuItemName,
				false);
		}

		String menuProperty = "plugin." + getClassName() + ".browser-menu";
		if(jEdit.getProperty(menuProperty) != null)
		{
			String pluginName = jEdit.getProperty("plugin." +
				getClassName() + ".name");
			return new EnhancedMenu(menuProperty,pluginName,
				VFSBrowser.getActionContext());
		}

		return null;
	} //}}}

	//{{{ Deprecated methods

	//{{{ createMenuItems() method
	/**
	 * @deprecated Instead of overriding this method, define properties
	 * as specified in the description of this class.
	 */
	public void createMenuItems(Vector menuItems) {} //}}}

	//{{{ createOptionPanes() method
	/**
	 * @deprecated Instead of overriding this method, define properties
	 * as specified in the description of this class.
	 */
	public void createOptionPanes(OptionsDialog optionsDialog) {} //}}}

	//}}}

	//{{{ Package-private members
	PluginJAR jar;
	//}}}

	//{{{ Broken class
	/**
	 * A placeholder for a plugin that didn't load.
	 * @see jEdit#getPlugin(String)
	 * @see PluginJAR#getPlugin()
	 * @see PluginJAR#activatePlugin()
	 */
	public static class Broken extends EditPlugin
	{
		public String getClassName()
		{
			return clazz;
		}

		// package-private members
		Broken(PluginJAR jar, String clazz)
		{
			this.jar = jar;
			this.clazz = clazz;
		}

		// private members
		private String clazz;
	} //}}}

	//{{{ Deferred class
	/**
	 * A placeholder for a plugin that hasn't been loaded yet.
	 * @see jEdit#getPlugin(String)
	 * @see PluginJAR#getPlugin()
	 * @see PluginJAR#activatePlugin()
	 */
	public static class Deferred extends EditPlugin
	{
		public String getClassName()
		{
			return clazz;
		}

		// package-private members
		Deferred(PluginJAR jar, String clazz)
		{
			this.jar = jar;
			this.clazz = clazz;
		}

		EditPlugin loadPluginClass()
		{
			return null;
		}

		public String toString()
		{
			return "Deferred[" + clazz + ']';
		}

		// private members
		private String clazz;
	} //}}}
}
