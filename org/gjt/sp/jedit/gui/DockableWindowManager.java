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

		top = new DockableWindowContainer.TabbedPane(this,TOP);
		left = new DockableWindowContainer.TabbedPane(this,LEFT);
		bottom = new DockableWindowContainer.TabbedPane(this,BOTTOM);
		right = new DockableWindowContainer.TabbedPane(this,RIGHT);

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
				if(jEdit.getBooleanProperty(name + ".auto-open"))
					addDockableWindow(name);
			}
		}

		// do this after adding dockables because addDockableWindow()
		// sets 'collapsed' to false
		top.setCollapsed(jEdit.getBooleanProperty("view.dock.top.collapsed"));
		left.setCollapsed(jEdit.getBooleanProperty("view.dock.left.collapsed"));
		bottom.setCollapsed(jEdit.getBooleanProperty("view.dock.bottom.collapsed"));
		right.setCollapsed(jEdit.getBooleanProperty("view.dock.right.collapsed"));
	}

	/**
	 * Focuses the specified dockable window.
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

		entry.container.showDockableWindow(entry.win);
	}

	/**
	 * Adds the dockable window with the specified name to this dockable
	 * window manager.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry != null)
		{
			entry.container.showDockableWindow(entry.win);
			return;
		}

		String position = jEdit.getProperty(name + ".dock-position",
			FLOATING);

		CreateDockableWindow msg = new CreateDockableWindow(view,name,
			position);
		EditBus.send(msg);

		DockableWindow win = msg.getDockableWindow();
		if(win == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}

		addDockableWindow(win,position);
	}

	/**
	 * Adds the specified dockable window to this dockable window manager.
	 * The position will be loaded from the properties.
	 * @param win The dockable window
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(DockableWindow win)
	{
		String name = win.getName();
		String position = jEdit.getProperty(name + ".dock-position",
			FLOATING);

		addDockableWindow(win,position);
	}

	/**
	 * Adds the specified dockable window to this dockable window manager.
	 * @param win The dockable window
	 * @param pos The window position
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(DockableWindow win, String position)
	{
		String name = win.getName();
		if(windows.get(name) != null)
		{
			throw new IllegalArgumentException("This DockableWindowManager"
				+ " already has a window named " + name);
		}

		DockableWindowContainer container;
		if(position.equals(FLOATING))
			container = new DockableWindowContainer.Floating(this);
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
		}

		Log.log(Log.DEBUG,this,"Adding " + name + " with position " + position);

		container.addDockableWindow(win);
		Entry entry = new Entry(win,position,container);
		windows.put(name,entry);
	}

	/**
	 * Removes the specified dockable window from this dockable window manager.
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

		Log.log(Log.DEBUG,this,"Removing " + name + " from "
			+ entry.container);

		jEdit.setBooleanProperty(name + ".auto-open",false);
		entry.container.saveDockableWindow(entry.win);
		entry.container.removeDockableWindow(entry.win);
		windows.remove(name);
	}

	/**
	 * Removes the specified dockable window from this dockable window manager.
	 * @param comp The dockable window's component
	 * @since jEdit 4.0pre1
	 */
	public void removeDockableWindow(Component comp)
	{
		Enumeration enum = windows.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(entry.win.getComponent() == comp)
			{
				String name = entry.win.getName();
				Log.log(Log.DEBUG,this,"Removing " + name + " from "
					+ entry.container);

				jEdit.setBooleanProperty(name + ".auto-open",false);
				entry.container.saveDockableWindow(entry.win);
				entry.container.removeDockableWindow(entry.win);
				windows.remove(name);
				return;
			}
		}

		Log.log(Log.ERROR,this,"This DockableWindowManager"
			+ " does not have a window " + comp);
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
		if(entry == null)
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
		if(entry == null)
			return false;
		else
			return entry.container.isDockableWindowVisible(entry.win);
	}

	/**
	 * Called when the view is being closed.
	 * @since jEdit 2.6pre3
	 */
	public void close()
	{
		Enumeration enum = windows.keys();
		while(enum.hasMoreElements())
		{
			String name = (String)enum.nextElement();
			Entry entry = (Entry)windows.get(name);
			jEdit.setBooleanProperty(name + ".auto-open",true);
			entry.container.saveDockableWindow(entry.win);
			entry.container.removeDockableWindow(entry.win);
		}

		top.saveDimension();
		left.saveDimension();
		bottom.saveDimension();
		right.saveDimension();
	}

	public DockableWindowContainer.TabbedPane getTopDockingArea()
	{
		return top;
	}

	public DockableWindowContainer.TabbedPane getLeftDockingArea()
	{
		return left;
	}

	public DockableWindowContainer.TabbedPane getBottomDockingArea()
	{
		return bottom;
	}

	public DockableWindowContainer.TabbedPane getRightDockingArea()
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

		left.propertiesChanged();
		right.propertiesChanged();
		top.propertiesChanged();
		bottom.propertiesChanged();

		Enumeration enum = windows.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(entry.container instanceof DockableWindowContainer.Floating)
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
	private DockableWindowContainer.TabbedPane left;
	private DockableWindowContainer.TabbedPane right;
	private DockableWindowContainer.TabbedPane top;
	private DockableWindowContainer.TabbedPane bottom;

	static
	{
		EditBus.addToBus(new DefaultFactory());
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"vfs.browser");
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"hypersearch-results");
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,"log-viewer");
	}

	class DockableLayout implements LayoutManager2
	{
		// these are Containers so that we can call getComponentCount()
		Container top, left, bottom, right;
		Component center;

		public void addLayoutComponent(String name, Component comp)
		{
			addLayoutComponent(comp,name);
		}

		public void addLayoutComponent(Component comp, Object cons)
		{
			if(BorderLayout.NORTH.equals(cons))
				top = (Container)comp;
			else if(BorderLayout.WEST.equals(cons))
				left = (Container)comp;
			else if(BorderLayout.SOUTH.equals(cons))
				bottom = (Container)comp;
			else if(BorderLayout.EAST.equals(cons))
				right = (Container)comp;
			else
				center = comp;
		}

		public void removeLayoutComponent(Component comp)
		{
			if(top == comp)
				top = null;
			else if(left == comp)
				left = null;
			else if(bottom == comp)
				bottom = null;
			else if(right == comp)
				right = null;
			else if(center == comp)
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
				if(left.getComponentCount() == 0)
					_left.width = 0;
				else
				{
					_left.width = DockableWindowContainer
						.TabbedPane.SPLITTER_WIDTH;
				}

				if(right.getComponentCount() == 0)
					_right.width = 0;
				else
				{
					_right.width = DockableWindowContainer
						.TabbedPane.SPLITTER_WIDTH;
				}
			}

			if(_top.height + _bottom.height > size.height)
			{
				if(top.getComponentCount() == 0)
					_top.height = 0;
				else
				{
					_top.height = DockableWindowContainer
						.TabbedPane.SPLITTER_WIDTH;
				}

				if(bottom.getComponentCount() == 0)
					_bottom.height = 0;
				else
				{
					_bottom.height = DockableWindowContainer
						.TabbedPane.SPLITTER_WIDTH;
				}
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

	static class Entry
	{
		DockableWindow win;
		String position;
		DockableWindowContainer container;

		Entry(DockableWindow win, String position,
			DockableWindowContainer container)
		{
			this.win = win;
			this.position = position;
			this.container = container;
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
