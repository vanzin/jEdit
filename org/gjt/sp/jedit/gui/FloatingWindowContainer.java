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
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class FloatingWindowContainer extends JFrame implements DockableWindowContainer,
	PropertyChangeListener
{
	String dockableName = null;
	//{{{ FloatingWindowContainer constructor
	public FloatingWindowContainer(DockableWindowManagerImpl dockableWindowManager,
		boolean clone)
	{
		this.dockableWindowManager = dockableWindowManager;

		dockableWindowManager.addPropertyChangeListener(this);
		this.clone = clone;
		setIconImage(GUIUtilities.getPluginIcon());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Box caption = new Box(BoxLayout.X_AXIS);
		caption.add(menu = new RolloverButton(GUIUtilities
			.loadIcon(jEdit.getProperty("dropdown-arrow.icon"))));
		menu.addMouseListener(new MouseHandler());
		menu.setToolTipText(jEdit.getProperty("docking.menu.label"));
		Box separatorBox = new Box(BoxLayout.Y_AXIS);
		separatorBox.add(Box.createVerticalStrut(3));
		separatorBox.add(new JSeparator(JSeparator.HORIZONTAL));
		separatorBox.add(Box.createVerticalStrut(3));
		caption.add(separatorBox);
		getContentPane().add(BorderLayout.NORTH,caption);
	
		
	} //}}}

	//{{{ register() method
	public void register(DockableWindowManagerImpl.Entry entry)
	{
		this.entry = entry;
		dockableName = entry.factory.name;
		
		setTitle(entry.shortTitle());

		getContentPane().add(BorderLayout.CENTER,entry.win);

		pack();
		Container parent = dockableWindowManager.getView();
		GUIUtilities.loadGeometry(this, parent, dockableName);
		GUIUtilities.addSizeSaver(this, parent, dockableName);
		KeyListener listener = dockableWindowManager.closeListener(dockableName);
		addKeyListener(listener);
		getContentPane().addKeyListener(listener);
		menu.addKeyListener(listener);
		entry.win.addKeyListener(listener);
		setVisible(true);
		if (! entry.win.isVisible())
			entry.win.setVisible(true);
	} //}}}

	//{{{ remove() method
	public void remove(DockableWindowManagerImpl.Entry entry)
	{
		dispose();
	} //}}}

	//{{{ unregister() method
	public void unregister(DockableWindowManagerImpl.Entry entry)
	{
		this.entry = null;
		entry.btn = null;
		entry.container = null;
		// Note: entry.win must not be reset, to enable the dockable
		// instance to be moved instead of recreated if it uses the
		// MOVABLE attribute.
		super.dispose();
	} //}}}

	//{{{ show() method
	public void show(final DockableWindowManagerImpl.Entry entry)
	{
		if(entry == null)
			dispose();
		else
		{
			setTitle(entry.longTitle());
			toFront();
			requestFocus();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if(entry.win instanceof DefaultFocusComponent)
					{
						((DefaultFocusComponent)entry.win)
							.focusOnDefaultComponent();
					}
					else
					{
						entry.win.requestFocus();
					}
				}
			});
		}
	} //}}}

	//{{{ isVisible() method
	public boolean isVisible(DockableWindowManagerImpl.Entry entry)
	{
		return true;
	} //}}}

	//{{{ dispose() method
	@Override
	public void dispose()
	{
		entry.container = null;
		entry.win = null;
		super.dispose();
	} //}}}

	//{{{ getDockableWindowManager() method
	public DockableWindowManagerImpl getDockableWindowManager()
	{
		return dockableWindowManager;
	} //}}}

	//{{{ getMinimumSize() method
	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ Private members
	private final DockableWindowManagerImpl dockableWindowManager;
	private final boolean clone;
	private DockableWindowManagerImpl.Entry entry;
	private final JButton menu;
	//}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		JPopupMenu popup;

		@Override
		public void mousePressed(MouseEvent evt)
		{
			if(popup != null && popup.isVisible())
				popup.setVisible(false);
			else
			{
				popup = dockableWindowManager.createPopupMenu(
					FloatingWindowContainer.this,
					entry.factory.name,clone);
				GUIUtilities.showPopupMenu(popup,
					menu,menu.getX(),menu.getY() + menu.getHeight(),
					false);
			}
		}
	} //}}}
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (dockableName == null) return;
		String pn = evt.getPropertyName();
		if (pn.startsWith(dockableName) && pn.endsWith("title"))
			setTitle(evt.getNewValue().toString());
	}
	
}

