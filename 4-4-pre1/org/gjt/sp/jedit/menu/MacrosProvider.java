/*
 * MacrosProvider.java - Macros menu
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

package org.gjt.sp.jedit.menu;

//{{{ Imports
import javax.swing.*;
import java.util.Collections;
import java.util.Vector;
import org.gjt.sp.jedit.*;
//}}}

public class MacrosProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return false;
	} //}}}

	//{{{ update() method
	public void update(JMenu menu)
	{
		Vector macroVector = Macros.getMacroHierarchy();

		int count = menu.getMenuComponentCount();

		createMacrosMenu(menu,macroVector,0);

		if(count == menu.getMenuComponentCount())
		{
			JMenuItem mi = new JMenuItem(jEdit.getProperty(
				"no-macros.label"));
			mi.setEnabled(false);
			menu.add(mi);
		}
	} //}}}

	//{{{ createMacrosMenu() method
	private void createMacrosMenu(JMenu menu, Vector vector, int start)
	{
		Vector<JMenuItem> menuItems = new Vector<JMenuItem>();

		for(int i = start; i < vector.size(); i++)
		{
			Object obj = vector.elementAt(i);
			if(obj instanceof String)
			{
				menuItems.add(new EnhancedMenuItem(
					jEdit.getProperty(obj + ".label"),
					(String)obj,jEdit.getActionContext()));
			}
			else if(obj instanceof Vector)
			{
				Vector subvector = (Vector)obj;
				String name = (String)subvector.elementAt(0);
				JMenu submenu = new JMenu(name);
				createMacrosMenu(submenu,subvector,1);
				if(submenu.getMenuComponentCount() != 0)
					menuItems.add(submenu);
			}
		}

		Collections.sort(menuItems, new MenuItemTextComparator());
		for(int i = 0; i < menuItems.size(); i++)
		{
			menu.add(menuItems.get(i));
		}
	} //}}}
}
