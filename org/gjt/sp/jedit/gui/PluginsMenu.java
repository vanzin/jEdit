/*
 * PluginsMenu.java - Plugins menu
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.event.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class PluginsMenu extends EnhancedMenu
{
	public PluginsMenu()
	{
		super("plugins");

		// Query plugins for menu items
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				EditPlugin plugin = pluginArray[i];

				// call old API
				int count = pluginMenuItems.size();
				plugin.createMenuItems(null,pluginMenuItems,
					pluginMenuItems);
				if(count != pluginMenuItems.size())
				{
					Log.log(Log.WARNING,this,plugin.getClassName()
						+ " is using the obsolete"
						+ " createMenuItems() API.");
				}

				// call new API
				plugin.createMenuItems(pluginMenuItems);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error creating menu items"
					+ " for plugin");
				Log.log(Log.ERROR,this,t);
			}
		}

		if(pluginMenuItems.isEmpty())
		{
			add(GUIUtilities.loadMenuItem("no-plugins"));
			return;
		}

		// Sort them
		MiscUtilities.quicksort(pluginMenuItems,
			new MiscUtilities.MenuItemCompare());

		JMenu menu = this;
		for(int i = 0; i < pluginMenuItems.size(); i++)
		{
			if(menu.getItemCount() >= 20)
			{
				menu.addSeparator();
				JMenu newMenu = new JMenu(jEdit.getProperty(
					"common.more"));
				menu.add(newMenu);
				menu = newMenu;
			}

			menu.add((JMenuItem)pluginMenuItems.elementAt(i));
		}
	}
}
