/*
 * MacrosProvider.java - Macros menu
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Portions copyright (C) 2011 Matthieu Casanova
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

package org.gjt.sp.jedit.menu;

//{{{ Imports
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.gjt.sp.jedit.*;
//}}}

public class MacrosProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	@Override
	public boolean updateEveryTime()
	{
		return false;
	} //}}}

	//{{{ update() method
	@Override
	public void update(JMenu menu)
	{
		List macroList = Macros.getMacroHierarchy();

		int count = menu.getMenuComponentCount();

		createMacrosMenu(menu,macroList,0);

		if(count == menu.getMenuComponentCount())
		{
			JMenuItem mi = new JMenuItem(jEdit.getProperty(
				"no-macros.label"));
			mi.setEnabled(false);
			menu.add(mi);
		}
	} //}}}

	//{{{ createMacrosMenu() method
	private void createMacrosMenu(JMenu menu, List list, int start)
	{
		List<JMenuItem> menuItems = new ArrayList<>();
		int maxItems = jEdit.getIntegerProperty("menu.spillover", 20);
		JMenu subMenu = null;
		for(int i = start; i < list.size(); i++)
		{
			if (i != start && i % maxItems == 0)
			{
				subMenu = new JMenu(jEdit.getProperty("common.more"));
				createMacrosMenu(subMenu, list, i);
				break;
			}
			Object obj = list.get(i);
			if(obj instanceof String)
			{
				menuItems.add(new EnhancedMenuItem(
					jEdit.getProperty(obj + ".label"),
					(String)obj,jEdit.getActionContext()));
			}
			else if(obj instanceof List)
			{
				List subList = (List)obj;
				String name = (String)subList.get(0);
				JMenu submenu = new JMenu(jEdit.getProperty("macros.folder."+ name + ".label", name));
				createMacrosMenu(submenu,subList,1);
				if(submenu.getMenuComponentCount() != 0)
					menuItems.add(submenu);
			}
		}

		menuItems.sort(new MenuItemTextComparator());

		if (subMenu != null)
			menuItems.add(subMenu);

		for (JMenuItem menuItem : menuItems)
			menu.add(menuItem);
	} //}}}
}
