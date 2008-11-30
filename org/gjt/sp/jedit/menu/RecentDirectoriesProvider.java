/*
 * RecentDirectoriesProvider.java - Recent directory list menu
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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
import java.awt.event.*;
import java.util.Vector;
import java.util.Collections;

import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.*;
//}}}

public class RecentDirectoriesProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return true;
	} //}}}

	//{{{ update() method
	public void update(JMenu menu)
	{
		final View view = GUIUtilities.getView(menu);

		//{{{ ActionListener...
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				VFSBrowser.browseDirectory(view,evt.getActionCommand());

				view.getStatus().setMessage(null);
			}
		}; //}}}

		//{{{ MouseListener...
		MouseListener mouseListener = new MouseAdapter()
		{
			public void mouseEntered(MouseEvent evt)
			{
				view.getStatus().setMessage(
					((JMenuItem)evt.getSource())
					.getActionCommand());
			}

			public void mouseExited(MouseEvent evt)
			{
				view.getStatus().setMessage(null);
			}
		}; //}}}

		HistoryModel model = HistoryModel.getModel("vfs.browser.path");
		if(model.getSize() == 0)
		{
			JMenuItem menuItem = new JMenuItem(
				jEdit.getProperty("no-recent-dirs.label"));
			menuItem.setEnabled(false);
			menu.add(menuItem);
			return;
		}

		boolean sort = jEdit.getBooleanProperty("sortRecent");

		int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

		Vector<JMenuItem> menuItems = new Vector<JMenuItem>();

		for(int i = 0; i < model.getSize(); i++)
		{
			String path = model.getItem(i);
			JMenuItem menuItem = new JMenuItem(MiscUtilities.getFileName(path));
			menuItem.setActionCommand(path);
			menuItem.addActionListener(actionListener);
			menuItem.addMouseListener(mouseListener);
			menuItem.setIcon(FileCellRenderer.dirIcon);

			if(sort)
				menuItems.addElement(menuItem);
			else
			{
				if(menu.getMenuComponentCount() >= maxItems
					&& i != model.getSize() - 1)
				{
					JMenu newMenu = new JMenu(
						jEdit.getProperty("common.more"));
					menu.add(newMenu);
					menu = newMenu;
				}

				menu.add(menuItem);
			}
		}

		if(sort)
		{
			Collections.sort(menuItems,
					new MenuItemTextComparator());
			for(int i = 0; i < menuItems.size(); i++)
			{
				if(menu.getMenuComponentCount() >= maxItems
					&& i != 0)
				{
					JMenu newMenu = new JMenu(
						jEdit.getProperty("common.more"));
					menu.add(newMenu);
					menu = newMenu;
				}

				menu.add(menuItems.elementAt(i));
			}
		}
	} //}}}
}
