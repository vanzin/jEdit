/*
 * EditPlugin.java - Abstract class all plugins must implement
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000 Slava Pestov
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

import java.util.Vector;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.util.Log;

/**
 * The interface between jEdit and a plugin.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.1pre1
 */
public abstract class EditPlugin
{
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

	//{{{ start() method
	/**
	 * Method called by jEdit to initialize the plugin.
	 * Actions and edit modes should be registered here, along
	 * with any EditBus paraphinalia.
	 * @since jEdit 2.1pre1
	 */
	public void start() {} //}}}

	//{{{ stop() method
	/**
	 * Method called by jEdit before exiting. Usually, nothing
	 * needs to be done here.
	 * @since jEdit 2.1pre1
	 */
	public void stop() {} //}}}

	//{{{ createMenuItems() method
	/**
	 * Method called every time a view is created to set up the
	 * Plugins menu. Menus and menu items should be loaded using the
	 * methods in the GUIUtilities class, and added to the vector.
	 * @param menuItems Add menus and menu items here
	 *
	 * @see GUIUtilities#loadMenu(String)
	 * @see GUIUtilities#loadMenuItem(String)
	 *
	 * @since jEdit 2.6pre5
	 */
	public void createMenuItems(Vector menuItems) {} //}}}

	//{{{ createOptionPanes() method
	/**
	 * Method called every time the plugin options dialog box is
	 * displayed. Any option panes created by the plugin should be
	 * added here.
	 * @param optionsDialog The plugin options dialog box
	 *
	 * @see OptionPane
	 * @see OptionsDialog#addOptionPane(OptionPane)
	 *
	 * @since jEdit 2.1pre1
	 */
	public void createOptionPanes(OptionsDialog optionsDialog) {} //}}}

	//{{{ getJAR() method
	/**
	 * Returns the JAR file containing this plugin.
	 * @since jEdit 3.1pre5
	 */
	public EditPlugin.JAR getJAR()
	{
		return jar;
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
		public String getPath()
		{
			return path;
		}

		public JARClassLoader getClassLoader()
		{
			return classLoader;
		}

		public ActionSet getActions()
		{
			return actions;
		}

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

			plugins.addElement(plugin);
		}

		public EditPlugin[] getPlugins()
		{
			EditPlugin[] array = new EditPlugin[plugins.size()];
			plugins.copyInto(array);
			return array;
		}

		public JAR(String path, JARClassLoader classLoader)
		{
			this.path = path;
			this.classLoader = classLoader;
			plugins = new Vector();
			actions = new ActionSet();
		}

		// package-private members
		void getPlugins(Vector vector)
		{
			for(int i = 0; i < plugins.size(); i++)
			{
				vector.addElement(plugins.elementAt(i));
			}
		}

		// private members
		private String path;
		private JARClassLoader classLoader;
		private Vector plugins;
		private ActionSet actions;
	} //}}}

	//{{{ Private members
	private EditPlugin.JAR jar;
	//}}}
}
