/*
 * MacrosMenu.java - Macros menu
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import org.gjt.sp.jedit.msg.MacrosChanged;
import org.gjt.sp.jedit.*;

public class MacrosMenu extends EnhancedMenu implements EBComponent
{
	public MacrosMenu()
	{
		super("macros");
		updateMacrosMenu();
	}

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof MacrosChanged)
			updateMacrosMenu();
	}

	private void updateMacrosMenu()
	{
		// Because the macros menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(getMenuComponent(i) instanceof JSeparator)
				break;
			else
				remove(i);
		}

		int count = getMenuComponentCount();

		Vector macroVector = Macros.getMacroHierarchy();
		createMacrosMenu(this,macroVector,0);

		if(count == getMenuComponentCount())
			add(GUIUtilities.loadMenuItem("no-macros"));
	}

	private void createMacrosMenu(JMenu menu, Vector vector, int start)
	{
		for(int i = start; i < vector.size(); i++)
		{
			Object obj = vector.elementAt(i);
			if(obj instanceof Macros.Macro)
			{
				Macros.Macro macro = (Macros.Macro)obj;
				String label = macro.name;
				int index = label.lastIndexOf('/');
				label = label.substring(index + 1)
					.replace('_',' ');

				menu.add(new EnhancedMenuItem(label,
					macro.action));
			}
			else if(obj instanceof Vector)
			{
				Vector subvector = (Vector)obj;
				String name = (String)subvector.elementAt(0);
				JMenu submenu = new JMenu(name);
				createMacrosMenu(submenu,subvector,1);
				if(submenu.getMenuComponentCount() == 0)
				{
					submenu.add(GUIUtilities.loadMenuItem(
						"no-macros"));
				}
				menu.add(submenu);
			}
		}
	}
}
