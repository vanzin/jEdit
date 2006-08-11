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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.jedit.msg.DockableWindowUpdate;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
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
	
	//{{{ Data members
	private View view;
	private DockableWindowFactory factory;

	/** A mapping from Strings to Entry objects. */
	private Map<String, Entry> windows;
	private PanelWindowContainer left;
	private PanelWindowContainer right;
	private PanelWindowContainer top;
	private PanelWindowContainer bottom;
	private List<Entry> clones;
	
	private Entry lastEntry;
	public Stack showStack = new Stack();
	// }}}
	
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

		windows = new HashMap<String, Entry>();
		clones = new ArrayList<Entry>();

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

		Iterator<DockableWindowFactory.Window> entries = factory.getDockableWindowIterator();

		while(entries.hasNext())
			addEntry(entries.next());

		propertiesChanged();
	} //}}}

	// {{{ closeListener() method
	/**
	 * 
	 * The actionEvent "close-docking-area" by default only works on 
	 * windows that are docked. If you want your floatable plugins to also
	 * respond to this event, you need to add key listeners to each component
	 * in your plugin that usually has keyboard focus. 
	 * This function returns a key listener which does exactly that.
	 * 
	 * @param dockableName the name of your dockable
	 * @return a KeyListener you can add to that plugin's component.
	 * @since jEdit 4.3pre6
	 * 
	 */
	public KeyListener closeListener(String dockableName) {
		return new KeyHandler(dockableName);
	}
	// }}}
	
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
		Entry entry = windows.get(name);
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
			FloatingWindowContainer fwc = new FloatingWindowContainer(this,true); 
			newEntry.container = fwc;
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
		lastEntry = windows.get(name);
		if(lastEntry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}

		if(lastEntry.win == null)
		{
			lastEntry.win = lastEntry.factory.createDockableWindow(
				view,lastEntry.position);
		}

		if(lastEntry.win != null)
		{
			if(lastEntry.position.equals(FLOATING)
				&& lastEntry.container == null)
			{
				FloatingWindowContainer fwc = new FloatingWindowContainer(
					this,view.isPlainView()); 
				lastEntry.container = fwc;
				lastEntry.container.register(lastEntry);
			}
			showStack.push(name);
			lastEntry.container.show(lastEntry);
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

		Entry entry = windows.get(name);
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
		
		Entry entry = windows.get(name);
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
		Entry e = windows.get(name);
		return e.longTitle();
	} //}}}

	//{{{ setDockableTitle() method
	/**
	 * Changes the .longtitle property of a dockable window, which corresponds to the 
	 * title shown when it is floating (not docked). Fires a change event that makes sure
	 * all floating dockables change their title.
	 * 
	 * @param dockableName the name of the dockable, as specified in the dockables.xml
	 * @param newTitle the new .longtitle you want to see above it.
	 * @since 4.3pre5
	 * 
	 */
	public void  setDockableTitle(String dockableName, String newTitle) {
		Entry entry = windows.get(dockableName);
		String propName = entry.factory.name + ".longtitle";
		String oldTitle = jEdit.getProperty(propName);
		jEdit.setProperty(propName, newTitle);
		firePropertyChange(propName, oldTitle, newTitle);
	}
	// }}}
	
	//{{{ isDockableWindowVisible() method
	/**
	 * Returns if the specified dockable window is visible.
	 * @param name The dockable window name
	 */
	public boolean isDockableWindowVisible(String name)
	{
		Entry entry = windows.get(name);
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
		Entry entry = windows.get(name);
		if(entry == null)
			return false;
		else
			return !entry.position.equals(FLOATING);
	} //}}}

	//{{{ closeCurrentArea() method
	/**
	 * Closes the most recently focused dockable. 
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
				/* Try to hide the last entry that was shown */
				try {
					String dockableName = showStack.pop().toString(); 
					hideDockableWindow(dockableName);
					return;
				}
				catch (Exception e) {}
				
				Component comp = view.getFocusOwner();
				while(comp != null)
				{
					//System.err.println(comp.getClass());
					if(comp instanceof DockablePanel)
					{
						DockablePanel panel = (DockablePanel) comp;
						
						PanelWindowContainer container = panel.getWindowContainer();
						
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

		for (Entry entry : windows.values())
		{
			if (entry.win != null)
				entry.container.unregister(entry);
		}

		for (Entry clone : clones)
		{
			if (clone.win != null)
				clone.container.unregister(clone);
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
				dockables[i] = getDockableTitle(name);
			}
			Arrays.sort(dockables);
			for(int i = 0; i < dockables.length; i++)
			{
				String name = dockables[i];
				JMenuItem item = new JMenuItem(name);
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
				Iterator<DockableWindowFactory.Window> iter = factory.getDockableWindowIterator();

				while(iter.hasNext())
				{
					DockableWindowFactory.Window w = iter.next();
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
				Iterator<Entry> iter = getAllPluginEntries(
					pmsg.getPluginJAR(),false);
				while(iter.hasNext())
				{
					Entry entry = iter.next();
					if(entry.container != null)
						entry.container.remove(entry);
				}
			}
			else if(pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				Iterator<Entry> iter = getAllPluginEntries(
					pmsg.getPluginJAR(),true);
				while(iter.hasNext())
				{
					Entry entry = iter.next();
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
			Entry entry = windows.get(dockable);

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
			{
			}
				
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
	private Iterator<Entry> getAllPluginEntries(PluginJAR plugin, boolean remove)
	{
		List<Entry> returnValue = new LinkedList<Entry>();
		Iterator<Entry> iter = windows.values().iterator();
		while(iter.hasNext())
		{
			Entry entry = iter.next();
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
			Entry entry = iter.next();
			if(entry.factory.plugin == plugin)
			{
				returnValue.add(entry);
				iter.remove();
			}
		}

		return returnValue.iterator();
	} //}}}

	//{{{ Entry class
	class Entry
	{
		DockableWindowFactory.Window factory;

//		String title;
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

		
		/**
		 * @return the long title for the dockable floating window.
		 */
		public String longTitle() 
		{
			String title = jEdit.getProperty(factory.name + ".longtitle");
			if (title == null) return shortTitle();
			else return title;
			
		}
		
		/**
		 * @return The short title, for the dockable button text
		 */
		public String shortTitle() 
		{
			
			String title = jEdit.getProperty(factory.name + ".title");
			if(title == null)
				return "NO TITLE PROPERTY: " + factory.name;
			else
				return title;
		}
		/**
		 * @return A label appropriate for the title on the dock buttons.
		 */
		public String label() {
			String retval = jEdit.getProperty(factory.name + ".label");
			retval = retval.replaceAll("\\$", "");
			return retval; 
		}
		//{{{ Entry constructor
		Entry(DockableWindowFactory.Window factory, String position)
		{
			this.factory = factory;
			this.position = position;

			// get the title here, not in the factory constructor,
			// since the factory might be created before a plugin's
			// props are loaded
			
		} //}}}
	} //}}}

	/**
	 * This keyhandler responds to only two key events - those corresponding to
	 * the close-docking-area action event. 
	 * 
	 * @author ezust
	 *
	 */
	class KeyHandler extends KeyAdapter {
		static final String action = "close-docking-area";  
		Key b1, b2;
		String name;
		
		public KeyHandler(String dockableName) {
			String shortcut1=jEdit.getProperty(action + ".shortcut");
			String shortcut2=jEdit.getProperty(action + ".shortcut2");
			if (shortcut1 != null)
				b1 = KeyEventTranslator.parseKey(shortcut1);
			if (shortcut2 != null)
				b2 = KeyEventTranslator.parseKey(shortcut2);
			name = dockableName;
		}
		public void keyPressed(KeyEvent e)
		{
			if ((b1 != null && e.getKeyChar() == b1.key) ||
			    (b2 != null && e.getKeyChar() == b2.key)) 
				hideDockableWindow(name);
		}


	}
	
}
