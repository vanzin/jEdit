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

import bsh.EvalError;
import com.microstar.xml.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.msg.CreateDockableWindow;
import org.gjt.sp.jedit.search.HyperSearchResults;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Manages dockable windows.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public class DockableWindowManager extends JPanel
{
	// static part of class

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
	 * Plugins shouldn't need to call this method.
	 * @since jEdit 4.0pre1
	 */
	public static boolean loadDockableWindows(String path, Reader in, ActionSet actionSet)
	{
		try
		{
			Log.log(Log.DEBUG,jEdit.class,"Loading dockables from " + path);

			DockableListHandler dh = new DockableListHandler(path,actionSet);
			XmlParser parser = new XmlParser();
			parser.setHandler(dh);
			parser.parse(null, null, in);
			return true;
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,jEdit.class,path + ":" + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,e);
		}

		return false;
	}

	public static void registerDockableWindow(String name, String code,
		boolean actions, ActionSet actionSet)
	{
		dockableWindowFactories.addElement(new Factory(name,code,
			actions,actionSet));
	}

	static class DockableListHandler extends HandlerBase
	{
		DockableListHandler(String path, ActionSet actionSet)
		{
			this.path = path;
			this.actionSet = actionSet;
			stateStack = new Stack();
			actions = true;
		}

		public Object resolveEntity(String publicId, String systemId)
		{
			if("dockables.dtd".equals(systemId))
			{
				try
				{
					return new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream
						("/org/gjt/sp/jedit/dockables.dtd")));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,"Error while opening"
						+ " dockables.dtd:");
					Log.log(Log.ERROR,this,e);
				}
			}

			return null;
		}

		public void attribute(String aname, String value, boolean isSpecified)
		{
			aname = (aname == null) ? null : aname.intern();
			value = (value == null) ? null : value.intern();

			if(aname == "NAME")
				dockableName = value;
			else if(aname == "NO_ACTIONS")
				actions = (value == "FALSE");
		}

		public void doctypeDecl(String name, String publicId,
			String systemId) throws Exception
		{
			if("DOCKABLES".equals(name))
				return;

			Log.log(Log.ERROR,this,path + ": DOCTYPE must be DOCKABLES");
		}

		public void charData(char[] c, int off, int len)
		{
			String tag = peekElement();
			String text = new String(c, off, len);

			if (tag == "DOCKABLE")
			{
				code = text;
			}
		}

		public void startElement(String tag)
		{
			tag = pushElement(tag);
		}

		public void endElement(String name)
		{
			if(name == null)
				return;

			String tag = peekElement();

			if(name.equals(tag))
			{
				if(tag == "DOCKABLE")
				{
					registerDockableWindow(dockableName,
						code,actions,actionSet);
					// make default be true for the next
					// action
					actions = true;
				}

				popElement();
			}
			else
			{
				// can't happen
				throw new InternalError();
			}
		}

		public void startDocument()
		{
			try
			{
				pushElement(null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		// end HandlerBase implementation

		// private members
		private String path;
		private ActionSet actionSet;

		private String dockableName;
		private String code;
		private boolean actions;

		private Stack stateStack;

		private String pushElement(String name)
		{
			name = (name == null) ? null : name.intern();

			stateStack.push(name);

			return name;
		}

		private String peekElement()
		{
			return (String) stateStack.peek();
		}

		private String popElement()
		{
			return (String) stateStack.pop();
		}
	}

	static class Factory
	{
		String name;
		String sanitizedName;
		String code;
		String cachedCode;

		Factory(String name, String code, boolean actions, ActionSet actionSet)
		{
			this.name = name;
			this.code = code;
			if(actions)
			{
				actionSet.addAction(new OpenAction());
				actionSet.addAction(new ToggleAction());
			}

			/* Some characters that we like to use in action names
			 * ('.', '-') are not allowed in BeanShell identifiers. */
			sanitizedName = name.replace('.','_').replace('-','_');
		}

		Component createDockableWindow(View view, String position)
		{
			// BACKWARDS COMPATIBILITY with jEdit 2.6-3.2 docking APIs
			if(code == null)
			{
				CreateDockableWindow msg = new CreateDockableWindow(view,name,
					position);
				EditBus.send(msg);

				DockableWindow win = msg.getDockableWindow();
				if(win == null)
				{
					Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
					return null;
				}
				return win.getComponent();
			}
			// END BACKWARDS COMPATIBILITY
			else
			{
				if(cachedCode == null)
				{
					cachedCode = BeanShell.cacheBlock(
						sanitizedName,code,true);
				}
				try
				{
					BeanShell.getNameSpace().setVariable(
						"position",position);
				}
				catch(EvalError e)
				{
					Log.log(Log.ERROR,this,e);
				}
				Component win = (Component)
					BeanShell.runCachedBlock(
					cachedCode,view,null);
				return win;
			}
		}

		class OpenAction extends EditAction
		{
			OpenAction()
			{
				super(name);
			}

			public void invoke(View view)
			{
				view.getDockableWindowManager()
					.showDockableWindow(name);
			}
		}

		class ToggleAction extends EditAction
		{
			ToggleAction()
			{
				super(name + "-toggle");
			}

			public void invoke(View view)
			{
				view.getDockableWindowManager()
					.toggleDockableWindow(name);
			}
		}
	}

	private static Vector dockableWindowFactories;

	static
	{
		dockableWindowFactories = new Vector();
	}

	// instance part of class

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

		add(DockableLayout.TOP_BUTTONS,top.getButtonBox());
		add(DockableLayout.LEFT_BUTTONS,left.getButtonBox());
		add(DockableLayout.BOTTOM_BUTTONS,bottom.getButtonBox());
		add(DockableLayout.RIGHT_BUTTONS,right.getButtonBox());

		add(TOP,top.getDockablePanel());
		add(LEFT,left.getDockablePanel());
		add(BOTTOM,bottom.getDockablePanel());
		add(RIGHT,right.getDockablePanel());
	}

	/**
	 * Adds any dockables set to auto-open.
	 * @since jEdit 2.6pre3
	 */
	public void init()
	{
		for(int i = 0; i < dockableWindowFactories.size(); i++)
		{
			Factory factory = (Factory)
				dockableWindowFactories.elementAt(i);
			Entry entry = new Entry(factory);
			windows.put(factory.name,entry);
		}

		String lastTop = jEdit.getProperty("view.dock.top.last");
		if(lastTop != null)
			showDockableWindow(lastTop);

		String lastLeft = jEdit.getProperty("view.dock.left.last");
		if(lastLeft != null)
			showDockableWindow(lastLeft);

		String lastBottom = jEdit.getProperty("view.dock.bottom.last");
		if(lastBottom != null)
			showDockableWindow(lastBottom);

		String lastRight = jEdit.getProperty("view.dock.right.last");
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
		{
			entry.container.remove(entry);
			entry.container = null;
			entry.win = null;
		}
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
	 * @deprecated Call getDockable(String) instead
	 */
	public DockableWindow getDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return null;
		else if(entry.win instanceof DockableWindow)
			return (DockableWindow)entry.win;
		else
			return entry;
	}

	/**
	 * This method returns a Component instance, unlike the
	 * deprecated <code>getDockableWindow()</code> method,
	 * which returns an instance of the DockableWindow interface.
	 * That interface should no longer be used.
	 * @param name The name of the dockable window
	 * @since jEdit 4.0pre1
	 */
	public Component getDockable(String name)
	{
		return getDockableWindow(name).getComponent();
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
		top.save();
		left.save();
		bottom.save();
		right.save();

		Enumeration enum = windows.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(entry.win != null)
				entry.remove();
		}
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
			String position = entry.position;
			String newPosition = jEdit.getProperty(entry.name
				+ ".dock-position");
			if(newPosition != null /* ??? */
				&& !position.equals(newPosition))
			{
				entry.position = newPosition;
				if(entry.container != null)
				{
					entry.container.remove(entry);
					entry.container = null;
					entry.win = null;
				}

				if(position.equals(FLOATING))
					/* do nothing */;
				else
				{
					if(newPosition.equals(TOP))
						entry.container = top;
					else if(newPosition.equals(LEFT))
						entry.container = left;
					else if(newPosition.equals(BOTTOM))
						entry.container = bottom;
					else if(newPosition.equals(RIGHT))
						entry.container = right;
					else
						throw new InternalError("Unknown position: " + position);

					entry.container.register(entry);
				}
			}

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

	class DockableLayout implements LayoutManager2
	{
		// for backwards compatibility with plugins that fiddle with
		// jEdit's UI layout
		static final String CENTER = BorderLayout.CENTER;

		static final String TOP_BUTTONS = "top-buttons";
		static final String LEFT_BUTTONS = "left-buttons";
		static final String BOTTOM_BUTTONS = "bottom-buttons";
		static final String RIGHT_BUTTONS = "right-buttons";

		Component center;
		Component top, left, bottom, right;
		Component topButtons, leftButtons, bottomButtons, rightButtons;

		public void addLayoutComponent(String name, Component comp)
		{
			addLayoutComponent(comp,name);
		}

		public void addLayoutComponent(Component comp, Object cons)
		{
			if(cons == null || CENTER.equals(cons))
				center = comp;
			else if(TOP.equals(cons))
				top = comp;
			else if(LEFT.equals(cons))
				left = comp;
			else if(BOTTOM.equals(cons))
				bottom = comp;
			else if(RIGHT.equals(cons))
				right = comp;
			else if(TOP_BUTTONS.equals(cons))
				topButtons = comp;
			else if(LEFT_BUTTONS.equals(cons))
				leftButtons = comp;
			else if(BOTTOM_BUTTONS.equals(cons))
				bottomButtons = comp;
			else if(RIGHT_BUTTONS.equals(cons))
				rightButtons = comp;
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
			Dimension _topButtons = topButtons.getPreferredSize();
			Dimension _leftButtons = leftButtons.getPreferredSize();
			Dimension _bottomButtons = bottomButtons.getPreferredSize();
			Dimension _rightButtons = rightButtons.getPreferredSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getPreferredSize());

			prefSize.height = _top.height + _bottom.height + _center.height
				+ _topButtons.height + _bottomButtons.height;
			prefSize.width = _left.width + _right.width + _center.width
				+ _leftButtons.width + _rightButtons.width;

			return prefSize;
		}

		public Dimension minimumLayoutSize(Container parent)
		{
			Dimension minSize = new Dimension(0,0);
			Dimension _top = top.getMinimumSize();
			Dimension _left = left.getMinimumSize();
			Dimension _bottom = bottom.getMinimumSize();
			Dimension _right = right.getMinimumSize();
			Dimension _topButtons = topButtons.getMinimumSize();
			Dimension _leftButtons = leftButtons.getMinimumSize();
			Dimension _bottomButtons = bottomButtons.getMinimumSize();
			Dimension _rightButtons = rightButtons.getMinimumSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getMinimumSize());

			minSize.height = _top.height + _bottom.height + _center.height
				+ _topButtons.height + _bottomButtons.height;
			minSize.width = _left.width + _right.width + _center.width
				+ _leftButtons.width + _rightButtons.width;

			return minSize;
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
			Dimension _topButtons = topButtons.getPreferredSize();
			Dimension _leftButtons = leftButtons.getPreferredSize();
			Dimension _bottomButtons = bottomButtons.getPreferredSize();
			Dimension _rightButtons = rightButtons.getPreferredSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getPreferredSize());

			int _width = size.width - _leftButtons.width - _rightButtons.width;
			int _height = size.height - _topButtons.height - _bottomButtons.height;

			if(alternateLayout)
			{
				topButtons.setBounds(
					_leftButtons.width,
					0,
					_width,
					_topButtons.height);

				leftButtons.setBounds(
					0,
					_topButtons.height + _top.height,
					_leftButtons.width,
					_height - _top.height - _bottom.height);

				bottomButtons.setBounds(
					_leftButtons.width,
					size.height - _bottomButtons.height,
					_width,
					_bottomButtons.height);

				rightButtons.setBounds(
					size.width - _rightButtons.width,
					_topButtons.height + _top.height,
					_rightButtons.width,
					_height - _top.height - _bottom.height);

				top.setBounds(
					_leftButtons.width,
					_topButtons.height,
					_width,
					_top.height);

				bottom.setBounds(
					_leftButtons.width,
					size.height - _bottom.height - _bottomButtons.height,
					_width,
					_bottom.height);

				left.setBounds(
					_leftButtons.width,
					_topButtons.height + _top.height,
					_left.width,
					_height - _top.height - _bottom.height);

				right.setBounds(
					size.width - _right.width - _rightButtons.width,
					_topButtons.height + _top.height,
					_right.width,
					_height - _top.height - _bottom.height);
			}
			else
			{
				topButtons.setBounds(
					_leftButtons.width + _left.width,
					0,
					_width - _left.width - _right.width,
					_topButtons.height);

				leftButtons.setBounds(
					0,
					_topButtons.height,
					_leftButtons.width,
					_height);

				bottomButtons.setBounds(
					_leftButtons.width + _left.width,
					size.height - _bottomButtons.height,
					_width - _left.width - _right.width,
					_bottomButtons.height);

				rightButtons.setBounds(
					size.width - _rightButtons.width,
					_topButtons.height,
					_rightButtons.width,
					_height);

				top.setBounds(
					_leftButtons.width + _left.width,
					_topButtons.height,
					_width - _left.width - _right.width,
					_top.height);
				bottom.setBounds(
					_leftButtons.width + _left.width,
					size.height - _bottom.height - _bottomButtons.height,
					_width - _left.width - _right.width,
					_bottom.height);

				left.setBounds(
					_leftButtons.width,
					_topButtons.height,
					_left.width,
					_height);

				right.setBounds(
					size.width - _right.width - _rightButtons.width,
					_topButtons.height,
					_right.width,
					_height);
			}

			if(center != null)
			{
				center.setBounds(
					_leftButtons.width + _left.width,
					_topButtons.height + _top.height,
					_width - _left.width - _right.width,
					_height - _top.height - _bottom.height);
			}
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

	class Entry implements DockableWindow
	{
		Factory factory;
		String name;
		String position;
		String title;

		DockableWindowContainer container;

		// only set if open
		Component win;

		public String getName()
		{
			return name;
		}

		public Component getComponent()
		{
			return win;
		}

		Entry(Factory factory)
		{
			this.factory = factory;
			this.name = factory.name;
			this.position = jEdit.getProperty(name + ".dock-position",
				FLOATING);
			title = jEdit.getProperty(name + ".title");

			if(position == null)
				position = FLOATING;

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
			win = factory.createDockableWindow(view,position);

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
}
