/*
 * FloatingWindowContainer.java - holds dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class FloatingWindowContainer extends JFrame implements DockableWindowContainer
{
	//{{{ FloatingWindowContainer constructor
	public FloatingWindowContainer(DockableWindowManager dockableWindowManager,
		boolean clone)
	{
		this.dockableWindowManager = dockableWindowManager;
		this.clone = clone;
		setIconImage(GUIUtilities.getPluginIcon());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Box caption = new Box(BoxLayout.X_AXIS);
		caption.add(menu = new RolloverButton(GUIUtilities
			.loadIcon("ToolbarMenu.gif")));
		menu.addMouseListener(new MouseHandler());
		Box separatorBox = new Box(BoxLayout.Y_AXIS);
		separatorBox.add(Box.createVerticalStrut(3));
		separatorBox.add(new JSeparator(JSeparator.HORIZONTAL));
		separatorBox.add(Box.createVerticalStrut(3));
		caption.add(separatorBox);
		getContentPane().add(BorderLayout.NORTH,caption);
	} //}}}

	//{{{ register() method
	public void register(DockableWindowManager.Entry entry)
	{
		this.entry = entry;
		setTitle(entry.title);

		getContentPane().add(BorderLayout.CENTER,entry.win);

		pack();
		GUIUtilities.loadGeometry(this,entry.factory.name);
		show();
	} //}}}

	//{{{ add() method
	public void add(DockableWindowManager.Entry entry)
	{
	} //}}}

	//{{{ save() method
	public void save(DockableWindowManager.Entry entry)
	{
		GUIUtilities.saveGeometry(this,entry.factory.name);
	} //}}}

	//{{{ remove() method
	public void remove(DockableWindowManager.Entry entry)
	{
		super.dispose();
	} //}}}

	//{{{ show() method
	public void show(final DockableWindowManager.Entry entry)
	{
		toFront();
		requestFocus();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				entry.win.requestDefaultFocus();
			}
		});
	} //}}}

	//{{{ isVisible() method
	public boolean isVisible(DockableWindowManager.Entry entry)
	{
		return true;
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		save(entry);
		entry.container = null;
		entry.win = null;
		super.dispose();
	} //}}}

	//{{{ getMinimumSize() method
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ Private members
	private DockableWindowManager dockableWindowManager;
	private boolean clone;
	private DockableWindowManager.Entry entry;
	private JButton menu;
	//}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		JPopupMenu popup;

		public void mousePressed(MouseEvent evt)
		{
			if(popup != null && popup.isVisible())
				popup.setVisible(false);
			else
			{
				popup = dockableWindowManager.createPopupMenu(
					entry.factory.name,clone);
				GUIUtilities.showPopupMenu(popup,
					menu,menu.getX(),menu.getY() + menu.getHeight(),
					false);
			}
		}
	} //}}}
}
