/*
 * DockableWindowManager.java - manages dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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
import bsh.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * The <code>DockableWindowManager</code> keeps track of dockable windows.
 * Each {@link org.gjt.sp.jedit.View} has an instance of this class.<p>
 *
 * <b>dockables.xml:</b><p>
 *
 * Dockable window definitions are read from <code>dockables.xml</code> files
 * contained inside plugin JARs. A dockable definition file has the following
 * form:
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE DOCKABLES SYSTEM "dockables.dtd"&gt;
 *&lt;DOCKABLES&gt;
 *    &lt;DOCKABLE NAME="name"&gt;
 *        // Code to create the dockable
 *    &lt;/DOCKABLE&gt;
 *&lt;/DOCKABLES&gt;</pre>
 *
 * More than one <code>&lt;DOCKABLE&gt;<code> tag may be present. The code that
 * creates the dockable can reference any BeanShell built-in variable
 * (see {@link org.gjt.sp.jedit.BeanShell}), along with a variable
 * <code>position</code> whose value is one of
 * {@link #FLOATING}, {@link #TOP}, {@link #LEFT}, {@link #BOTTOM},
 * and {@link #RIGHT}.<p>
 *
 * The following properties must be defined for each dockable window:
 *
 * <ul>
 * <li><code><i>name</i>.title</code> - the string to show in the title bar
 * of the dockable.</li>
 * <li><code><i>name</i>.label</code> - the dockable's menu item label.</li>
 * </ul>
 *
 * A number of actions are automatically created for each dockable window:
 *
 * <ul>
 * <li><code><i>name</i></code> - opens the dockable window.</li>
 * <li><code><i>name</i>-toggle</code> - toggles the dockable window's visibility.</li>
 * <li><code><i>name</i>-float</code> - opens the dockable window in a new
 * floating window.</li>
 * </ul>
 *
 * Note that only the first action needs a <code>label</code> property, the
 * rest have automatically-generated labels.
 *
 * <b>Implementation details:</b><p>
 *
 * When an instance of this class is initialized by the {@link org.gjt.sp.jedit.View}
 * class, it
 * iterates through the list of registered dockable windows (from jEdit itself,
 * and any loaded plugins) and
 * examines options supplied by the user in the <b>Global
 * Options</b> dialog box. Any plugins designated for one of the
 * four docking positions are displayed.<p>
 *
 * To create an instance of a dockable window, the <code>DockableWindowManager</code>
 * finds and executes the BeanShell code extracted from the appropriate
 * <code>dockables.xml</code> file. This code will typically consist of a call
 * to the constructor of the dockable window component. The result of the
 * BeanShell expression, typically a newly constructed component, is placed
 * in a window managed by this class.
 *
 * @see org.gjt.sp.jedit.View#getDockableWindowManager()
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public class DockableWindowManager extends JPanel implements EBComponent
{
	//{{{ Constants
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
	//}}}

	//{{{ getRegisteredDockableWindows() method
	/**
	 * @since jEdit 4.3pre2
	 */
	public static String[] getRegisteredDockableWindows()
	{
		return DockableWindowFactory.getInstance()
			.getRegisteredDockableWindows();
	} //}}}

	//{{{ DockableWindowManager constructor
	/**
	 * Creates a new dockable window manager.
	 * @param view The view
	 * @param factory A {@link DockableWindowFactory}, usually
	 * <code>DockableWindowFactory.getInstance()</code>.
	 * @param config A docking configuration
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager(View view, DockableWindowFactory factory,
		View.ViewConfig config)
	{
		setLayout(new DockableLayout());
		this.view = view;
		this.factory = factory;

		windows = new Hashtable();
		clones = new ArrayList();

		top = new PanelWindowContainer(this,TOP,config.topPos);
		left = new PanelWindowContainer(this,LEFT,config.leftPos);
		bottom = new PanelWindowContainer(this,BOTTOM,config.bottomPos);
		right = new PanelWindowContainer(this,RIGHT,config.rightPos);

		add(DockableLayout.TOP_BUTTONS,top.buttonPanel);
		add(DockableLayout.LEFT_BUTTONS,left.buttonPanel);
		add(DockableLayout.BOTTOM_BUTTONS,bottom.buttonPanel);
		add(DockableLayout.RIGHT_BUTTONS,right.buttonPanel);

		add(TOP,top.dockablePanel);
		add(LEFT,left.dockablePanel);
		add(BOTTOM,bottom.dockablePanel);
		add(RIGHT,right.dockablePanel);
	} //}}}

	//{{{ init() method
	/**
	 * Initialises dockable window manager. Do not call this method directly.
	 */
	public void init()
	{
		EditBus.addToBus(this);

		Iterator entries = factory.getDockableWindowIterator();

		while(entries.hasNext())
			addEntry((DockableWindowFactory.Window)entries.next());

		propertiesChanged();
	} //}}}

	//{{{ getView() method
	/**
	 * Returns this dockable window manager's view.
	 * @since jEdit 4.0pre2
	 */
	public View getView()
	{
		return view;
	} //}}}

	//{{{ floatDockableWindow() method
	/**
	 * Opens a new instance of the specified dockable window in a floating
	 * container.
	 * @param name The dockable window name
	 * @return The new dockable window instance
	 * @since jEdit 4.1pre2
	 */
	public JComponent floatDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return null;
		}

		// create a copy of this dockable window and float it
		Entry newEntry = new Entry(entry.factory,FLOATING);
		newEntry.win = newEntry.factory.createDockableWindow(view,FLOATING);
		if(newEntry.win != null)
		{
			newEntry.container = new FloatingWindowContainer(this,true);
			newEntry.container.register(newEntry);
			newEntry.container.show(newEntry);
		}

		clones.add(newEntry);
		return newEntry.win;
	} //}}}

	//{{{ showDockableWindow() method
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
		{
			entry.win = entry.factory.createDockableWindow(
				view,entry.position);
		}

		if(entry.win != null)
		{
			if(entry.position.equals(FLOATING)
				&& entry.container == null)
			{
				entry.container = new FloatingWindowContainer(
					this,view.isPlainView());
				entry.container.register(entry);
			}

			entry.container.show(entry);
			Object reason = DockableWindowUpdate.ACTIVATED;
			EditBus.send(new DockableWindowUpdate(this, reason, name));
		}
		else
			/* an error occurred */;
	} //}}}

	//{{{ addDockableWindow() method
	/**
	 * Opens the specified dockable window. As of jEdit 4.0pre1, has the
	 * same effect as calling showDockableWindow().
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(String name)
	{
		showDockableWindow(name);
	} //}}}

	//{{{ hideDockableWindow() method
	/**
	 * Hides the specified dockable window.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void hideDockableWindow(String name)
	{

		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}



		if(entry.win == null)
			return;
		Object reason = DockableWindowUpdate.DEACTIVATED;
		EditBus.send(new DockableWindowUpdate(this, reason, name));

		entry.container.show(null);
	} //}}}

	//{{{ removeDockableWindow() method
	/**
	 * Hides the specified dockable window. As of jEdit 4.2pre1, has the
	 * same effect as calling hideDockableWindow().
	 * @param name The dockable window name
	 * @since jEdit 4.2pre1
	 */
	public void removeDockableWindow(String name)
	{
		hideDockableWindow(name);
	} //}}}

	//{{{ toggleDockableWindow() method
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
	} //}}}

	//{{{ getDockableWindow() method
	/**
	 * Returns the specified dockable window.
	 *
	 * Note that this method
	 * will return null if the dockable has not been added yet.
	 * Make sure you call {@link #addDockableWindow(String)} first.
	 *
	 * @param name The name of the dockable window
	 * @since jEdit 4.1pre2
	 */
	public JComponent getDockableWindow(String name)
	{
		return getDockable(name);
	} //}}}

	//{{{ getDockable() method
	/**
	 * Returns the specified dockable window.
	 *
	 * Note that this method
	 * will return null if the dockable has not been added yet.
	 * Make sure you call {@link #addDockableWindow(String)} first.
	 *
	 * For historical reasons, this
	 * does the same thing as {@link #getDockableWindow(String)}.
	 *
	 * @param name The name of the dockable window
	 * @since jEdit 4.0pre1
	 */
	public JComponent getDockable(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return null;
		else
			return entry.win;
	} //}}}

	//{{{ getDockableTitle() method
	/**
	 * Returns the title of the specified dockable window.
	 * @param name The name of the dockable window.
	 * @since jEdit 4.1pre5
	 */
	public String getDockableTitle(String name)
	{
		String title = jEdit.getProperty(name + ".title");
		if(title == null)
			return "NO TITLE PROPERTY: " + name;
		else
			return title;
	} //}}}

	//{{{ isDockableWindowVisible() method
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
	} //}}}

	//{{{ isDockableWindowDocked() method
	/**
	 * Returns if the specified dockable window is docked into the
	 * view.
	 * @param name The dockable's name
	 * @since jEdit 4.0pre2
	 */
	public boolean isDockableWindowDocked(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
			return false;
		else
			return !entry.position.equals(FLOATING);
	} //}}}

	//{{{ closeCurrentArea() method
	/**
	 * Closes the currently focused docking area.
	 * @since jEdit 4.1pre3
	 */
	public void closeCurrentArea()
	{
		// I don't know of any other way to fix this, since invoking this
		// command from a menu results in the focus owner being the menu
		// until the menu goes away.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Component comp = view.getFocusOwner();
				while(comp != null)
				{
					//System.err.println(comp.getClass());
					if(comp instanceof DockablePanel)
					{
						PanelWindowContainer container =
							((DockablePanel)comp)
							.getWindowContainer();
						container.show(null);
						return;
					}

					comp = comp.getParent();
				}

				getToolkit().beep();
			}
		});
	} //}}}

	//{{{ close() method
	/**
	 * Called when the view is being closed.
	 * @since jEdit 2.6pre3
	 */
	public void close()
	{
		EditBus.removeFromBus(this);

		Iterator iter = windows.values().iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.win != null)
			{
				entry.container.unregister(entry);
			}
		}

		iter = clones.iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.win != null)
			{
				entry.container.unregister(entry);
			}
		}
	} //}}}

	//{{{ getTopDockingArea() method
	public PanelWindowContainer getTopDockingArea()
	{
		return top;
	} //}}}

	//{{{ getLeftDockingArea() method
	public PanelWindowContainer getLeftDockingArea()
	{
		return left;
	} //}}}

	//{{{ getBottomDockingArea() method
	public PanelWindowContainer getBottomDockingArea()
	{
		return bottom;
	} //}}}

	//{{{ getRightDockingArea() method
	public PanelWindowContainer getRightDockingArea()
	{
		return right;
	} //}}}

	//{{{ createPopupMenu() method
	public JPopupMenu createPopupMenu(
		final DockableWindowContainer container,
		final String dockable,
		final boolean clone)
	{
		JPopupMenu popup = new JPopupMenu();
		if(dockable == null && container instanceof PanelWindowContainer)
		{
			ActionListener listener = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					showDockableWindow(evt.getActionCommand());
				}
			};

			String[] dockables = ((PanelWindowContainer)
				container).getDockables();
			for(int i = 0; i < dockables.length; i++)
			{
				String name = dockables[i];
				JMenuItem item = new JMenuItem(getDockableTitle(name));
				item.setActionCommand(name);
				item.addActionListener(listener);
				popup.add(item);
			}
		}
		else
		{
			JMenuItem caption = new JMenuItem(getDockableTitle(dockable));
			caption.setEnabled(false);
			popup.add(caption);
			popup.addSeparator();
			String currentPos = jEdit.getProperty(dockable + ".dock-position",FLOATING);
			if(!clone)
			{
				String[] positions = { FLOATING, TOP, LEFT, BOTTOM, RIGHT };
				for(int i = 0; i < positions.length; i++)
				{
					final String pos = positions[i];
					if(pos.equals(currentPos))
						continue;

					JMenuItem moveMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-"
						+ pos));

					moveMenuItem.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent evt)
						{
							jEdit.setProperty(dockable + ".dock-position",pos);
							EditBus.send(new DockableWindowUpdate(
								DockableWindowManager.this,
								DockableWindowUpdate.PROPERTIES_CHANGED,
								null
							));
							showDockableWindow(dockable);
						}
					});
					popup.add(moveMenuItem);
				}

				popup.addSeparator();
			}

			JMenuItem cloneMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-clone"));

			cloneMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					floatDockableWindow(dockable);
				}
			});
			popup.add(cloneMenuItem);

			popup.addSeparator();

			JMenuItem closeMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-close"));

			closeMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					if(clone)
						((FloatingWindowContainer)container).dispose();
					else
						removeDockableWindow(dockable);
				}
			});
			popup.add(closeMenuItem);

			if(!(clone || currentPos.equals(FLOATING)))
			{
				JMenuItem undockMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-undock"));

				undockMenuItem.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						jEdit.setProperty(dockable + ".dock-position",FLOATING);
						EditBus.send(new DockableWindowUpdate(
							DockableWindowManager.this,
							DockableWindowUpdate.PROPERTIES_CHANGED,
							null
						));
					}
				});
				popup.add(undockMenuItem);
			}
		}

		return popup;
	} //}}}

	//{{{ paintChildren() method
	public void paintChildren(Graphics g)
	{
		super.paintChildren(g);

		if(resizeRect != null)
		{
			g.setColor(Color.darkGray);
			g.fillRect(resizeRect.x,resizeRect.y,
				resizeRect.width,resizeRect.height);
		}
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof DockableWindowUpdate)
		{
			if(((DockableWindowUpdate)msg).getWhat()
				== DockableWindowUpdate.PROPERTIES_CHANGED)
				propertiesChanged();
		}
		else if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof PluginUpdate)
		{
			PluginUpdate pmsg = (PluginUpdate)msg;
			if(pmsg.getWhat() == PluginUpdate.LOADED)
			{
				Iterator iter = factory.getDockableWindowIterator();

				while(iter.hasNext())
				{
					DockableWindowFactory.Window w = (DockableWindowFactory.Window)iter.next();
					if(w.plugin == pmsg.getPluginJAR())
						addEntry(w);
				}

				propertiesChanged();
			}
			else if(pmsg.isExiting())
			{
				// we don't care
			}
			else if(pmsg.getWhat() == PluginUpdate.DEACTIVATED)
			{
				Iterator iter = getAllPluginEntries(
					pmsg.getPluginJAR(),false);
				while(iter.hasNext())
				{
					Entry entry = (Entry)iter.next();
					if(entry.container != null)
						entry.container.remove(entry);
				}
			}
			else if(pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				Iterator iter = getAllPluginEntries(
					pmsg.getPluginJAR(),true);
				while(iter.hasNext())
				{
					Entry entry = (Entry)iter.next();
					if(entry.container != null)
					{
						entry.container.unregister(entry);
						entry.win = null;
						entry.container = null;
					}
				}
			}
		}
	} //}}}

	//{{{ Package-private members
	int resizePos;
	Rectangle resizeRect;

	//{{{ setResizePos() method
	void setResizePos(int resizePos, PanelWindowContainer resizing)
	{
		this.resizePos = resizePos;

		if(resizePos < 0)
			resizePos = 0;

		Rectangle newResizeRect = new Rectangle(0,0,
			PanelWindowContainer.SPLITTER_WIDTH - 2,
			PanelWindowContainer.SPLITTER_WIDTH - 2);
		if(resizing == top)
		{
			resizePos = Math.min(resizePos,getHeight()
				- top.buttonPanel.getHeight()
				- bottom.dockablePanel.getHeight()
				- bottom.buttonPanel.getHeight()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = top.dockablePanel.getX() + 1;
			newResizeRect.y = resizePos + top.buttonPanel.getHeight() + 1;
			newResizeRect.width = top.dockablePanel.getWidth() - 2;
		}
		else if(resizing == left)
		{
			resizePos = Math.min(resizePos,getWidth()
				- left.buttonPanel.getWidth()
				- right.dockablePanel.getWidth()
				- right.buttonPanel.getWidth()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = resizePos + left.buttonPanel.getWidth() + 1;
			newResizeRect.y = left.dockablePanel.getY() + 1;
			newResizeRect.height = left.dockablePanel.getHeight() - 2;
		}
		else if(resizing == bottom)
		{
			resizePos = Math.min(resizePos,getHeight()
				- bottom.buttonPanel.getHeight()
				- top.dockablePanel.getHeight()
				- top.buttonPanel.getHeight()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = bottom.dockablePanel.getX() + 1;
			newResizeRect.y = getHeight() - bottom.buttonPanel.getHeight() - resizePos
				- PanelWindowContainer.SPLITTER_WIDTH + 2;
			newResizeRect.width = bottom.dockablePanel.getWidth() - 2;
		}
		else if(resizing == right)
		{
			resizePos = Math.min(resizePos,getWidth()
				- right.buttonPanel.getWidth()
				- left.dockablePanel.getWidth()
				- left.buttonPanel.getWidth()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = getWidth() - right.buttonPanel.getWidth() - resizePos
				- PanelWindowContainer.SPLITTER_WIDTH + 1;
			newResizeRect.y = right.dockablePanel.getY() + 1;
			newResizeRect.height = right.dockablePanel.getHeight() - 2;
		}

		Rectangle toRepaint;
		if(resizeRect == null)
			toRepaint = newResizeRect;
		else
			toRepaint = resizeRect.union(newResizeRect);
		resizeRect = newResizeRect;
		repaint(toRepaint);
	} //}}}

	//{{{ finishResizing() method
	void finishResizing()
	{
		resizeRect = null;
		repaint();
	} //}}}

	//}}}

	//{{{ Private members
	private View view;
	private DockableWindowFactory factory;
	private Hashtable windows;
	private PanelWindowContainer left;
	private PanelWindowContainer right;
	private PanelWindowContainer top;
	private PanelWindowContainer bottom;
	private ArrayList clones;

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		if(view.isPlainView())
			return;

		((DockableLayout)getLayout()).setAlternateLayout(
			jEdit.getBooleanProperty("view.docking.alternateLayout"));

		String[] windowList = factory.getRegisteredDockableWindows();

		for(int i = 0; i < windowList.length; i++)
		{
			String dockable = windowList[i];
			Entry entry = (Entry)windows.get(dockable);

			String newPosition = jEdit.getProperty(dockable
				+ ".dock-position",FLOATING);
			if(newPosition.equals(entry.position))
			{
				continue;
			}

			entry.position = newPosition;
			if(entry.container != null)
			{
				entry.container.unregister(entry);
				entry.container = null;
				entry.win = null;
			}

			if(newPosition.equals(FLOATING))
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
				{
					Log.log(Log.WARNING,this,
						"Unknown position: "
						+ newPosition);
					continue;
				}

				entry.container.register(entry);
			}
		}

		top.sortDockables();
		left.sortDockables();
		bottom.sortDockables();
		right.sortDockables();

		revalidate();
		repaint();
	} //}}}

	//{{{ addEntry() method
	private void addEntry(DockableWindowFactory.Window factory)
	{
		Entry e;
		if(view.isPlainView())
		{
			// don't show menu items to dock into a plain view
			e = new Entry(factory,FLOATING);
		}
		else
		{
			e = new Entry(factory);
			if(e.position.equals(FLOATING))
				/* nothing to do */;
			else if(e.position.equals(TOP))
				e.container = top;
			else if(e.position.equals(LEFT))
				e.container = left;
			else if(e.position.equals(BOTTOM))
				e.container = bottom;
			else if(e.position.equals(RIGHT))
				e.container = right;
			else
			{
				Log.log(Log.WARNING,this,
					"Unknown position: "
					+ e.position);
			}

			if(e.container != null)
				e.container.register(e);
		}
		windows.put(factory.name,e);
	} //}}}

	//{{{ getAllPluginEntries() method
	/**
	 * If remove is false, only remove from clones list, otherwise remove
	 * from both entries and clones.
	 */
	private Iterator getAllPluginEntries(PluginJAR plugin, boolean remove)
	{
		java.util.List returnValue = new LinkedList();
		Iterator iter = windows.values().iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.factory.plugin == plugin)
			{
				returnValue.add(entry);
				if(remove)
					iter.remove();
			}
		}

		iter = clones.iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.factory.plugin == plugin)
			{
				returnValue.add(entry);
				iter.remove();
			}
		}

		return returnValue.iterator();
	} //}}}

	//}}}

	//{{{ Entry class
	class Entry
	{
		DockableWindowFactory.Window factory;

		String title;
		String position;
		DockableWindowContainer container;

		// only set if open
		JComponent win;

		// only for docked
		AbstractButton btn;

		//{{{ Entry constructor
		Entry(DockableWindowFactory.Window factory)
		{
			this(factory,jEdit.getProperty(factory.name
				+ ".dock-position",FLOATING));
		} //}}}

		//{{{ Entry constructor
		Entry(DockableWindowFactory.Window factory, String position)
		{
			this.factory = factory;
			this.position = position;

			// get the title here, not in the factory constructor,
			// since the factory might be created before a plugin's
			// props are loaded
			title = getDockableTitle(factory.name);
		} //}}}
	} //}}}
}
