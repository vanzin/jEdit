/*
 * PluginsMenu.java - Plugins menu
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.util.*;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class PluginsMenu extends EnhancedMenu implements EBComponent
{
	//{{{ PluginsMenu constructor
	public PluginsMenu()
	{
		super("plugins");
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PluginUpdate)
		{
			Object what = ((PluginUpdate)msg).getWhat();
			if(what == PluginUpdate.LOADED
				|| what == PluginUpdate.UNLOADED)
			{
				initialized = false;
			}
		}
	} //}}}

	//{{{ init() method
	public void init()
	{
		super.init();

		// Because the plugins menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(getMenuComponent(i) instanceof JSeparator)
				break;
			else
				remove(i);
		}

		// We build a set of lists, each list contains plugin menu
		// items that begin with a given letter.
		int count = 0;

		List[] letters = new List[26];
		for(int i = 0; i < letters.length; i++)
		{
			letters[i] = new ArrayList();
		}

		Vector pluginMenuItems = new Vector();

		PluginJAR[] pluginArray = jEdit.getPluginJARs();
		for(int i = 0; i < pluginArray.length; i++)
		{
			PluginJAR jar = pluginArray[i];
			EditPlugin plugin = jar.getPlugin();
			if(plugin == null)
				continue;

			JMenuItem menuItem = plugin.createMenuItems();
			if(menuItem != null)
			{
				addToLetterMap(letters,menuItem);
				count++;
			}
			//{{{ old API
			else if(jEdit.getProperty("plugin."
				+ plugin.getClassName()
				+ ".activate") == null)
			{
				try
				{
					pluginMenuItems.clear();
					plugin.createMenuItems(pluginMenuItems);

					Iterator iter = pluginMenuItems.iterator();
					while(iter.hasNext())
					{
						addToLetterMap(letters,
							(JMenuItem)iter.next());
						count++;
					}
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,
						"Error creating menu items"
						+ " for plugin");
					Log.log(Log.ERROR,this,t);
				}
			} //}}}
		}

		if(count == 0)
		{
			JMenuItem menuItem = new JMenuItem(
				jEdit.getProperty("no-plugins.label"));
			menuItem.setEnabled(false);
			add(menuItem);
			return;
		}

		// Sort each letter
		for(int i = 0; i < letters.length; i++)
		{
			List list = letters[i];
			Collections.sort(list,new MiscUtilities
				.MenuItemCompare());
		}

		int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

		// if less than 20 items, put them directly in the menu
		if(count <= maxItems)
		{
			for(int i = 0; i < letters.length; i++)
			{
				Iterator iter = letters[i].iterator();
				while(iter.hasNext())
				{
					add((JMenuItem)iter.next());
				}
			}

			return;
		}

		// Collect blocks of up to maxItems of consecutive letters
		count = 0;
		char first = 'A';
		JMenu submenu = new JMenu();
		add(submenu);

		for(int i = 0; i < letters.length; i++)
		{
			List letter = letters[i];

			if(count + letter.size() > maxItems && count != 0)
			{
				char last = (char)(i + 'A' - 1);
				if(last == first)
					submenu.setText(String.valueOf(first));
				else
					submenu.setText(first + " - " + last);
				first = (char)(char)(i + 'A');
				count = 0;
				submenu = null;
			}

			Iterator iter = letter.iterator();
			while(iter.hasNext())
			{
				if(submenu == null)
				{
					submenu = new JMenu();
					add(submenu);
				}
				submenu.add((JMenuItem)iter.next());
			}

			count += letter.size();
		}

		if(submenu != null)
		{
			char last = 'Z';
			if(last == first)
				submenu.setText(String.valueOf(first));
			else
				submenu.setText(first + " - " + last);
		}
	} //}}}

	//{{{ addToLetterMap() method
	private void addToLetterMap(List[] letters, JMenuItem item)
	{
		char ch = item.getText().charAt(0);
		ch = Character.toUpperCase(ch);
		if(ch < 'A' || ch > 'Z')
		{
			Log.log(Log.ERROR,this,"Plugin menu item label must "
				+ "begin with A - Z, or a - z: "
				+ item.getText());
		}
		else
			letters[ch - 'A'].add(item);
	} //}}}
}
