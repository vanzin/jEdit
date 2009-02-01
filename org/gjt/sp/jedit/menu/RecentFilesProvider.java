/*
 * RecentFilesProvider.java - Recent file list menu
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

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.FileCellRenderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
//}}}

public class RecentFilesProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return false;
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
				jEdit.openFile(view,evt.getActionCommand());
				view.getStatus().setMessage(null);
			}
		}; //}}}

		//{{{ MouseListener...
		/*
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
		};
		*/
		//}}}
		
		//{{{ ChangeListener...
		ChangeListener changeListener = new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				JMenuItem menuItem = (JMenuItem) e.getSource();
				
				view.getStatus().setMessage(menuItem.isArmed()?menuItem.getActionCommand():null);
			} 
		}; //}}}

		List<BufferHistory.Entry> recentVector = BufferHistory.getHistory();

		if(recentVector.isEmpty())
		{
			JMenuItem menuItem = new JMenuItem(
				jEdit.getProperty("no-recent-files.label"));
			menuItem.setEnabled(false);
			menu.add(menuItem);
			return;
		}

		final List<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		final JTextField text = new JTextField();
		text.setToolTipText(jEdit.getProperty("recent-files.textfield.tooltip"));
		menu.add(text);
		text.addKeyListener(new KeyAdapter()
		{
			public void keyReleased(KeyEvent e)
			{
				String typedText = text.getText();
				for (JMenuItem tempMenuItem : menuItems)
				{
					if (typedText.length() == 0)
					{
						tempMenuItem.setEnabled(true);
					}
					else
					{
						String fileName = tempMenuItem.getText();
						boolean matchesStart = fileName.toLowerCase().startsWith(typedText.toLowerCase());
						tempMenuItem.setEnabled(matchesStart);
					}
				}
			}
		});

		boolean sort = jEdit.getBooleanProperty("sortRecent");

		int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

		Iterator<BufferHistory.Entry> iter = recentVector.iterator();
		while(iter.hasNext())
		{
			String path = iter.next().path;
			JMenuItem menuItem = new JMenuItem(MiscUtilities
				.getFileName(path));
			menuItem.setActionCommand(path);
			menuItem.addActionListener(actionListener);
//			menuItem.addMouseListener(mouseListener);
			menuItem.addChangeListener(changeListener);
			
			menuItem.setIcon(FileCellRenderer.fileIcon);

			menuItems.add(menuItem);
			if (!sort)
			{
				if (menu.getMenuComponentCount() >= maxItems
				    && iter.hasNext())
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
			Collections.sort(menuItems, new MenuItemTextComparator());
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

				menu.add(menuItems.get(i));
			}
		}
		JMenuItem menuItem = new JMenuItem(jEdit.getProperty("clear-recent-files.label"));
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				BufferHistory.clear();
			}
		});
		menu.addSeparator();
		menu.add(menuItem);
	} //}}}
}
