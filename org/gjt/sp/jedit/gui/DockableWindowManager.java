/*
 * DockableWindowManager.java - manages dockable windows
 * Copyright (C) 2000, 2001 Slava Pestov
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

import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.msg.CreateDockableWindow;
import org.gjt.sp.jedit.search.HyperSearchResults;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
 * Manages dockable windows.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public class DockableWindowManager extends JPanel
{
	/**
	 * Floating position.
	 * @since jEdit 2.6pre3
	 */
	public static final String FLOATING = "floating";

	/**
	 * Top position.
	 * @since jEdit 2.6pre3
	 */
	public static final String TOP = "top";

	/**
	 * Left position.
	 * @since jEdit 2.6pre3
	 */
	public static final String LEFT = "left";

	/**
	 * Bottom position.
	 * @since jEdit 2.6pre3
	 */
	public static final String BOTTOM = "bottom";

	/**
	 * Right position.
	 * @since jEdit 2.6pre3
	 */
	public static final String RIGHT = "right";

	/**
	 * Creates a new dockable window manager.
	 * @param view The view
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager(View view)
	{
		setLayout(new DockableLayout());
		this.view = view;
		windows = new Hashtable();

		top = new PanelWindowContainer(this,TOP);
		left = new PanelWindowContainer(this,LEFT);
		bottom = new PanelWindowContainer(this,BOTTOM);
		right = new PanelWindowContainer(this,RIGHT);

		add(BorderLayout.NORTH,top);
		add(BorderLayout.WEST,left);
		add(BorderLayout.SOUTH,bottom);
		add(BorderLayout.EAST,right);
	}

	/**
	 * Adds any dockables set to auto-open.
	 * @since jEdit 2.6pre3
	 */
	public void init()
	{
		Object[] dockables = EditBus.getNamedList(DockableWindow
			.DOCKABLE_WINDOW_LIST);
		if(dockables != null)
		{
			for(int i = 0; i < dockables.length; i++)
			{
				String name = (String)dockables[i];
				Entry entry = new Entry(name);
				windows.put(name,entry);
			}
		}

		String lastTop = jEdit.getProperty("view.docking.last-top");
		if(lastTop != null)
			showDockableWindow(lastTop);

		String lastLeft = jEdit.getProperty("view.docking.last-left");
		if(lastLeft != null)
			showDockableWindow(lastLeft);

		String lastBottom = jEdit.getProperty("view.docking.last-bottom");
		if(lastBottom != null)
			showDockableWindow(lastBottom);

		String lastRight = jEdit.getProperty("view.docking.last-right");
		if(lastRight != null)
			showDockableWindow(lastRight);
	}

	/**
	 * Opens the specified dockable window.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void showDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}

		if(entry.win == null)
			entry.open();

		entry.container.show(entry);
	}

	/**
	 * Opens the specified dockable window. As of version 4.0pre1, has the same
	 * effect as calling showDockableWindow().
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(String name)
	{
		showDockableWindow(name);
	}

	/**
	 * Removes the specified dockable window.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void removeDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"This DockableWindowManager"
				+ " does not have a window named " + name);
			return;
		}

		if(entry.win == null)
			return;

		if(entry.container instanceof FloatingWindowContainer)
			entry.container.remove(entry);
		else
			entry.container.show(null);
	}

	/**
	 * Toggles the visibility of the specified dockable window.
	 * @param name The dockable window name
	 */
	public void toggleDockableWindow(String name)
	{
		if(isDockableWindowVisible(name))
			removeDockableWindow(name);
		else
			addDockableWindow(name);
	}

	/**
	 * Returns the specified dockable window.
	 * @param name The dockable window name.
	 */
	public DockableWindow getDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return null;
		else
			return entry.win;
	}

	/**
	 * Returns if the specified dockable window is visible.
	 * @param name The dockable window name
	 */
	public boolean isDockableWindowVisible(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return false;
		else
			return entry.container.isVisible(entry);
	}

	/**
	 * Called when the view is being closed.
	 * @since jEdit 2.6pre3
	 */
	public void close()
	{
		top.saveDimension();
		if(top.getCurrent() != null)
			jEdit.setProperty("view.docking.last-top",top.getCurrent().name);
		left.saveDimension();
		if(left.getCurrent() != null)
			jEdit.setProperty("view.docking.last-left",left.getCurrent().name);
		bottom.saveDimension();
		if(bottom.getCurrent() != null)
			jEdit.setProperty("view.docking.last-bottom",bottom.getCurrent().name);
		right.saveDimension();
		if(right.getCurrent() != null)
			jEdit.setProperty("view.docking.last-right",right.getCurrent().name);

		Enumeration enum = windows.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(entry.win != null)
				entry.remove();
		}

		windows = null;
		top = left = bottom = right = null;
	}

	public PanelWindowContainer getTopDockingArea()
	{
		return top;
	}

	public PanelWindowContainer getLeftDockingArea()
	{
		return left;
	}

	public PanelWindowContainer getBottomDockingArea()
	{
		return bottom;
	}

	public PanelWindowContainer getRightDockingArea()
	{
		return right;
	}

	/**
	 * Called by the view when properties change.
	 * @since jEdit 2.6pre3
	 */
	public void propertiesChanged()
	{
		alternateLayout = jEdit.getBooleanProperty("view.docking.alternateLayout");

		Enumeration enum = windows.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(entry.container instanceof FloatingWindowContainer)
			{
				SwingUtilities.updateComponentTreeUI(((JFrame)entry.container)
					.getRootPane());
			}
		}

		revalidate();
	}

	// private members
	private View view;
	private Hashtable windows;
	private boolean alternateLayout;
	private PanelWindowContainer left;
	private PanelWindowContainer right;
	private PanelWindowContainer top;
	private PanelWindowContainer bottom;

	static
	{
		EditBus.addToBus(new DefaultFactory());
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"vfs.browser");
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"hypersearch-results");
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"log-viewer");
	}

	class DockableLayout implements LayoutManager2
	{
		Component center;

		public void addLayoutComponent(String name, Component comp)
		{
			addLayoutComponent(comp,name);
		}

		public void addLayoutComponent(Component comp, Object cons)
		{
			if(cons == null || BorderLayout.CENTER.equals(cons))
				center = comp;
		}

		public void removeLayoutComponent(Component comp)
		{
			if(center == comp)
				center = null;
		}

		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension prefSize = new Dimension(0,0);
			Dimension _top = top.getPreferredSize();
			Dimension _left = left.getPreferredSize();
			Dimension _bottom = bottom.getPreferredSize();
			Dimension _right = right.getPreferredSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getPreferredSize());

			prefSize.height = _top.height + _bottom.height + _center.height;
			prefSize.width = _left.width + _right.width + _center.width;

			return prefSize;
		}

		public Dimension minimumLayoutSize(Container parent)
		{
			return preferredLayoutSize(parent);
		}

		public Dimension maximumLayoutSize(Container parent)
		{
			return new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
		}

		public void layoutContainer(Container parent)
		{
			Dimension size = parent.getSize();

			Dimension prefSize = new Dimension(0,0);
			Dimension _top = top.getPreferredSize();
			Dimension _left = left.getPreferredSize();
			Dimension _bottom = bottom.getPreferredSize();
			Dimension _right = right.getPreferredSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getPreferredSize());

			if(_left.width + _right.width > size.width)
			{
				left.show(null);
				right.show(null);
				_left = left.getPreferredSize();
				_right = right.getPreferredSize();
			}

			if(_top.height + _bottom.height > size.height)
			{
				top.show(null);
				bottom.show(null);
				_top = top.getPreferredSize();
				_bottom = bottom.getPreferredSize();
			}

			int _width = size.width - _left.width - _right.width;
			int _height = size.height - _top.height - _bottom.height;

			if(alternateLayout)
			{
				top.setBounds(0,0,size.width,_top.height);
				bottom.setBounds(0,size.height - _bottom.height,
					size.width,_bottom.height);

				left.setBounds(0,_top.height,_left.width,_height);
				right.setBounds(size.width - _right.width,
					_top.height,_right.width,_height);
			}
			else
			{
				left.setBounds(0,0,_left.width,size.height);
				right.setBounds(size.width - _right.width,0,
					_right.width,size.height);

				top.setBounds(_left.width,0,_width,_top.height);
				bottom.setBounds(_left.width,size.height - _bottom.height,
					_width,_bottom.height);
			}

			if(center != null)
				center.setBounds(_left.width,_top.height,_width,_height);
		}

		public float getLayoutAlignmentX(Container target)
		{
			return 0.5f;
		}

		public float getLayoutAlignmentY(Container target)
		{
			return 0.5f;
		}

		public void invalidateLayout(Container target) {}
	}

	public class Entry
	{
		String name;
		String position;
		String title;
		DockableWindowContainer container;

		// only set if open
		DockableWindow win;

		Entry(String name)
		{
			this.name = name;
			this.position = jEdit.getProperty(name + ".dock-position",
				FLOATING);
			title = jEdit.getProperty(name + ".title");

			if(position.equals(FLOATING))
				/* do nothing */;
			else
			{
				if(position.equals(TOP))
					container = top;
				else if(position.equals(LEFT))
					container = left;
				else if(position.equals(BOTTOM))
					container = bottom;
				else if(position.equals(RIGHT))
					container = right;
				else
					throw new InternalError("Unknown position: " + position);

				container.register(this);
			}
		}

		void open()
		{
			CreateDockableWindow msg = new CreateDockableWindow(view,name,
				position);
			EditBus.send(msg);

			win = msg.getDockableWindow();
			if(win == null)
			{
				Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
				return;
			}

			Log.log(Log.DEBUG,this,"Adding " + name + " with position " + position);

			if(position.equals(FLOATING))
			{
				container = new FloatingWindowContainer(
					DockableWindowManager.this);
				container.register(this);
			}

			container.add(this);
		}

		void remove()
		{
			Log.log(Log.DEBUG,this,"Removing " + name + " from "
				+ container);

			container.save(this);
			container.remove(this);

			if(container instanceof FloatingWindowContainer)
				container = null;

			win = null;
		}
	}

	// factory for creating the dockables built into the jEdit core
	// (VFS browser, HyperSearch results)
	static class DefaultFactory implements EBComponent
	{
		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof CreateDockableWindow)
			{
				CreateDockableWindow cmsg = (CreateDockableWindow)msg;
				String name = cmsg.getDockableWindowName();
				if(name.equals("vfs.browser"))
				{
					cmsg.setDockableWindow(new VFSBrowser(
						cmsg.getView(),null));
				}
				else if(name.equals("hypersearch-results"))
				{
					cmsg.setDockableWindow(new HyperSearchResults(
						cmsg.getView()));
				}
				else if(name.equals("log-viewer"))
				{
					cmsg.setDockableWindow(new LogViewer());
				}
			}
		}
	}
}
