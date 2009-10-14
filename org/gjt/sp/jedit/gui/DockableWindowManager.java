package org.gjt.sp.jedit.gui;

// {{{ imports
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.jedit.msg.DockableWindowUpdate;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
// }}}

@SuppressWarnings("serial")
// {{{ abstract class DockableWindowManager
/**
 * <p>Keeps track of all dockable windows for a single View, and provides
 * an API for getting/showing/hiding them. </p>
 *
 * <p>Each {@link org.gjt.sp.jedit.View} has an instance of this class.</p>
 *
 * <p><b>dockables.xml:</b></p>
 *
 * <p>Dockable window definitions are read from <code>dockables.xml</code> files
 * contained inside plugin JARs. A dockable definition file has the following
 * form: </p>
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE DOCKABLES SYSTEM "dockables.dtd"&gt;
 *&lt;DOCKABLES&gt;
 *    &lt;DOCKABLE NAME="<i>dockableName</i>" MOVABLE="TRUE|FALSE"&gt;
 *        // Code to create the dockable
 *    &lt;/DOCKABLE&gt;
 *&lt;/DOCKABLES&gt;</pre>
 *
 * <p>The MOVABLE attribute specifies the behavior when the docking position of
 * the dockable window is changed. If MOVABLE is TRUE, the existing instance of
 * the dockable window is moved to the new docking position, and if the dockable
 * window implements the DockableWindow interface (see {@link DockableWindow}),
 * it is also notified about the change in docking position before it is moved.
 * If MOVABLE is FALSE, the BeanShell code is invoked to get the instance of
 * the dockable window to put in the new docking position. Typically, the
 * BeanShell code returns a new instance of the dockable window, and the state
 * of the existing instance is not preserved after the change. It is therefore
 * recommended to set MOVABLE to TRUE for all dockables in order to make them
 * preserve their state when they are moved. For backward compatibility reasons,
 * this attribute is set to FALSE by default.</p>
 * <p>More than one <code>&lt;DOCKABLE&gt;</code> tag may be present. The code that
 * creates the dockable can reference any BeanShell built-in variable
 * (see {@link org.gjt.sp.jedit.BeanShell}), along with a variable
 * <code>position</code> whose value is one of
 * {@link #FLOATING}, {@link #TOP}, {@link #LEFT}, {@link #BOTTOM},
 * and {@link #RIGHT}. </p>
 *
 * <p>The following properties must be defined for each dockable window: </p>
 *
 * <ul>
 * <li><code><i>dockableName</i>.title</code> - the string to show on the dockable
 * button. </li>
 * <li><code><i>dockableName</i>.label</code> - The string to use for generating
 *    menu items and action names. </li>
 * <li><code><i>dockableName</i>.longtitle</code> - (optional) the string to use
 *      in the dockable's floating window title (when it is floating).
 *       If not specified, the <code><i>dockableName</i>.title</code> property is used. </li>
 * </ul>
 *
 * A number of actions are automatically created for each dockable window:
 *
 * <ul>
 * <li><code><i>dockableName</i></code> - opens the dockable window.</li>
 * <li><code><i>dockableName</i>-toggle</code> - toggles the dockable window's visibility.</li>
 * <li><code><i>dockableName</i>-float</code> - opens the dockable window in a new
 * floating window.</li>
 * </ul>
 *
 * Note that only the first action needs a <code>label</code> property, the
 * rest have automatically-generated labels.
 *
 * <p> <b>Implementation details:</b></p>
 *
 * <p> When an instance of this class is initialized by the {@link org.gjt.sp.jedit.View}
 * class, it
 * iterates through the list of registered dockable windows (from jEdit itself,
 * and any loaded plugins) and
 * examines options supplied by the user in the <b>Global
 * Options</b> dialog box. Any plugins designated for one of the
 * four docking positions are displayed.</p>
 *
 * <p> To create an instance of a dockable window, the <code>DockableWindowManager</code>
 * finds and executes the BeanShell code extracted from the appropriate
 * <code>dockables.xml</code> file. This code will typically consist of a call
 * to the constructor of the dockable window component. The result of the
 * BeanShell expression, typically a newly constructed component, is placed
 * in a window managed by this class. </p>
 *
 * @see org.gjt.sp.jedit.View#getDockableWindowManager()
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @author Shlomy Reinstein (refactoring into a base and an impl)
 * @version $Id$
 * @since jEdit 2.6pre3
 *
 */
 public abstract class DockableWindowManager extends JPanel implements EBComponent
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

	// {{{ data members
	private final Map<PluginJAR, Set<String>> plugins = new HashMap<PluginJAR, Set<String>>(); 
	private final Map<String, String> positions = new HashMap<String, String>();
	protected View view;
	protected DockableWindowFactory factory;
	protected Map<String, JComponent> windows = new HashMap<String, JComponent>();

	// variables for toggling all dock areas
	private boolean tBottom, tTop, tLeft, tRight;
	private boolean closeToggle = true;

	private static final String ALTERNATE_LAYOUT_PROP = "view.docking.alternateLayout";
	private boolean alternateLayout;
	// }}}

	// {{{ DockableWindowManager constructor
	public DockableWindowManager(View view, DockableWindowFactory instance,
			ViewConfig config)
	{
		this.view = view;
		this.factory = instance;
		alternateLayout = jEdit.getBooleanProperty(ALTERNATE_LAYOUT_PROP);
	} // }}}

	// {{{ Abstract methods
	public abstract void setMainPanel(JPanel panel);
	public abstract void showDockableWindow(String name);
	public abstract void hideDockableWindow(String name);

	/** Completely dispose of a dockable - called when a plugin is
	    unloaded, to remove all references to the its dockables. */
	public abstract void disposeDockableWindow(String name);
	public abstract JComponent floatDockableWindow(String name);
	public abstract boolean isDockableWindowDocked(String name);
	public abstract boolean isDockableWindowVisible(String name);
	public abstract void closeCurrentArea();
	public abstract DockingLayout getDockingLayout(ViewConfig config);
	public abstract DockingArea getLeftDockingArea();
	public abstract DockingArea getRightDockingArea();
	public abstract DockingArea getTopDockingArea();
	public abstract DockingArea getBottomDockingArea();
	// }}}

	// {{{ public methods
	// {{{ init()
	public void init()
	{
		EditBus.addToBus(this);

		Iterator<DockableWindowFactory.Window> entries = factory.getDockableWindowIterator();
		while(entries.hasNext())
		{
			DockableWindowFactory.Window window = entries.next();
			String dockable = window.name;
			positions.put(dockable, getDockablePosition(dockable));
			addPluginDockable(window.plugin, dockable);
		}
	} // }}}

	// {{{ close()
	public void close()
	{
		EditBus.removeFromBus(this);
	} // }}}

	// {{{ applyDockingLayout
	public void applyDockingLayout(DockingLayout docking)
	{
		// By default, use the docking positions specified by the jEdit properties
		Iterator<Entry<String, String>> iterator = positions.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<String, String> entry = iterator.next();
			String dockable = entry.getKey();
			String position = entry.getValue();
			if (! position.equals(FLOATING))
				showDockableWindow(dockable);
		}
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

	// {{{ toggleDockAreas()
	/**
	 * Hides all visible dock areas, or shows them again,
	 * if the last time it was a hide.
	 * @since jEdit 4.3pre16
	 *
	 */
	public void toggleDockAreas()
	{
		if (closeToggle)
		{
			tTop = getTopDockingArea().getCurrent() != null;
			tLeft = getLeftDockingArea().getCurrent() != null;
			tRight = getRightDockingArea().getCurrent() != null;
			tBottom = getBottomDockingArea().getCurrent() != null;
			getBottomDockingArea().show(null);
			getTopDockingArea().show(null);
			getRightDockingArea().show(null);
			getLeftDockingArea().show(null);
		}
		else
		{
			if (tBottom) getBottomDockingArea().showMostRecent();
			if (tLeft) getLeftDockingArea().showMostRecent();
			if (tRight) getRightDockingArea().showMostRecent();
			if (tTop) getTopDockingArea().showMostRecent();
		}
		closeToggle = !closeToggle;
		view.getTextArea().requestFocus();
	} // }}}

	// {{{ dockableTitleChanged
	public void dockableTitleChanged(String dockable, String newTitle)
	{
	} // }}}

	// {{{ closeListener() method
	/**
	 *
	 * The actionEvent "close-docking-area" by default only works on
	 * windows that are docked. If you want your floatable plugins to also
	 * respond to this event, you need to add key listeners to each component
	 * in your plugin that usually has keyboard focus.
	 * This function returns a key listener which does exactly that.
	 * You should not need to call this method - it is used by FloatingWindowContainer.
	 *
	 * @param dockableName the name of your dockable
	 * @return a KeyListener you can add to that plugin's component.
	 * @since jEdit 4.3pre6
	 *
	 */
	public KeyListener closeListener(String dockableName)
	{
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

	//{{{ getDockable method
	/**
	 * @since jEdit 4.3pre2
	 */
	public JComponent getDockable(String name)
	{
		return windows.get(name);
	} // }}}

	//{{{ getDockableTitle() method
	/**
	 * Returns the title of the specified dockable window.
	 * @param name The name of the dockable window.
	 * @since jEdit 4.1pre5
	 */
	public String getDockableTitle(String name)
	{
		return longTitle(name);
	}//}}}

	//{{{ setDockableTitle() method
	/**
	 * Changes the .longtitle property of a dockable window, which corresponds to the
	 * title shown when it is floating (not docked). Fires a change event that makes sure
	 * all floating dockables change their title.
	 *
	 * @param dockable the name of the dockable, as specified in the dockables.xml
	 * @param title the new .longtitle you want to see above it.
	 * @since 4.3pre5
	 *
	 */
	public void setDockableTitle(String dockable, String title)
	{
		String propName = getLongTitlePropertyName(dockable);
		String oldTitle = jEdit.getProperty(propName);
		jEdit.setProperty(propName, title);
		firePropertyChange(propName, oldTitle, title);
		dockableTitleChanged(dockable, title);
	}
	// }}}

	//{{{ getRegisteredDockableWindows() method
	public static String[] getRegisteredDockableWindows()
	{
		return DockableWindowFactory.getInstance()
			.getRegisteredDockableWindows();
	} //}}}

	//{{{ getDockableWindowPluginClassName() method
	public static String getDockableWindowPluginName(String name)
	{
		String pluginClass =
			DockableWindowFactory.getInstance().getDockableWindowPluginClass(name);
		if (pluginClass == null)
			return null;
		return jEdit.getProperty("plugin." + pluginClass + ".name");
	} //}}}

	// {{{ setDockingLayout method
	public void setDockingLayout(DockingLayout docking)
	{
		applyDockingLayout(docking);
		applyAlternateLayout(alternateLayout);
	} // }}}

	// {{{ addPluginDockable
	private void addPluginDockable(PluginJAR plugin, String name)
	{
		Set<String> dockables = plugins.get(plugin);
		if (dockables == null)
		{
			dockables = new HashSet<String>();
			plugins.put(plugin, dockables);
		}
		dockables.add(name);
	}
	// }}}
	
	// {{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if (msg instanceof DockableWindowUpdate)
		{
			if(((DockableWindowUpdate)msg).getWhat() == DockableWindowUpdate.PROPERTIES_CHANGED)
				propertiesChanged();
		}
		else if (msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof PluginUpdate)
		{
			PluginUpdate pmsg = (PluginUpdate)msg;
			if (pmsg.getWhat() == PluginUpdate.LOADED)
			{
				Iterator<DockableWindowFactory.Window> iter = factory.getDockableWindowIterator();
				while (iter.hasNext())
				{
					DockableWindowFactory.Window w = iter.next();
					if (w.plugin == pmsg.getPluginJAR())
					{
						String position = getDockablePosition(w.name);
						positions.put(w.name, position);
						addPluginDockable(w.plugin, w.name);
						dockableLoaded(w.name, position);
					}
				}
				propertiesChanged();
			}
			else if(pmsg.isExiting())
			{
				// we don't care
			}
			else if(pmsg.getWhat() == PluginUpdate.DEACTIVATED ||
					pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				Set<String> dockables = plugins.remove(pmsg.getPluginJAR());
				if (dockables != null)
				{
					for (String dockable: dockables)
					{
						disposeDockableWindow(dockable);
						windows.remove(dockable);
					}
				}
			}
		}
	} // }}}

	// {{{ longTitle() method
	public String longTitle(String name)
	{
		String title = jEdit.getProperty(getLongTitlePropertyName(name));
		if (title == null)
			return shortTitle(name);
		return title;
	} // }}}

	// {{{ shortTitle() method
	public String shortTitle(String name)
	{
		String title = jEdit.getProperty(name + ".title");
		if(title == null)
			return "NO TITLE PROPERTY: " + name;
		return title;
	} // }}}

	// }}}

	// {{{ protected methods
	// {{{ applyAlternateLayout
	protected void applyAlternateLayout(boolean alternateLayout)
	{
	} //}}}

	// {{{
	protected void dockableLoaded(String dockableName, String position)
	{
	}
	// }}}
	
	// {{{
	protected void dockingPositionChanged(String dockableName,
		String oldPosition, String newPosition)
	{
	} //}}}

	// {{{ getAlternateLayoutProp()
	protected boolean getAlternateLayoutProp()
	{
		return alternateLayout;
	} // }}}

	// {{{ propertiesChanged
	protected void propertiesChanged()
	{
		if(view.isPlainView())
			return;

		boolean newAlternateLayout = jEdit.getBooleanProperty(ALTERNATE_LAYOUT_PROP);
		if (newAlternateLayout != alternateLayout)
		{
			alternateLayout = newAlternateLayout;
			applyAlternateLayout(newAlternateLayout);
		}

		String[] dockables = factory.getRegisteredDockableWindows();
		for(int i = 0; i < dockables.length; i++)
		{
			String dockable = dockables[i];
			String oldPosition = positions.get(dockable);
			String newPosition = getDockablePosition(dockable);
			if (oldPosition == null || !newPosition.equals(oldPosition))
			{
				positions.put(dockable, newPosition);
				dockingPositionChanged(dockable, oldPosition, newPosition);
			}
		}

	} // }}}

	// {{{ createDockable()
	protected JComponent createDockable(String name)
	{
		DockableWindowFactory.Window wf = factory.getDockableWindowFactory(name);
		if (wf == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return null;
		}
		String position = getDockablePosition(name);
		JComponent window = wf.createDockableWindow(view, position);
		if (window != null)
			windows.put(name, window);
		return window;
	} // }}}

	// {{{ getDockablePosition()
	protected String getDockablePosition(String name)
	{
		return jEdit.getProperty(name + ".dock-position", FLOATING);
	} // }}}

	// {{{ focusDockable
	protected void focusDockable(String name)
	{
		JComponent c = getDockable(name);
		if (c == null)
			return;
		if (c instanceof DefaultFocusComponent)
			((DefaultFocusComponent)c).focusOnDefaultComponent();
		else
			c.requestFocus();
	} // }}}

	// {{{ getLongTitlePropertyName()
	protected String getLongTitlePropertyName(String dockableName)
	{
		return dockableName + ".longtitle";
	} //}}}
	// }}}


	// {{{ Inner classes
	// {{{ DockingArea interface
	public interface DockingArea
	{
		void showMostRecent();
		String getCurrent();
		void show(String name);
		String [] getDockables();
	}
	// }}}

	//{{{ KeyHandler class
	/**
	 * This keyhandler responds to only two key events - those corresponding to
	 * the close-docking-area action event.
	 *
	 * @author ezust
	 */
	class KeyHandler extends KeyAdapter
	{
		static final String action = "close-docking-area";
		Vector<Key> b1, b2;
		String name;
		int match1, match2;

		public KeyHandler(String dockableName)
		{
			String shortcut1=jEdit.getProperty(action + ".shortcut");
			String shortcut2=jEdit.getProperty(action + ".shortcut2");
			if (shortcut1 != null)
				b1 = parseShortcut(shortcut1);
			if (shortcut2 != null)
				b2 = parseShortcut(shortcut2);
			name = dockableName;
			match1 = match2 = 0;
		}

		@Override
		public void keyTyped(KeyEvent e)
		{
			if (b1 != null)
				match1 = match(e, b1, match1);
			if (b2 != null)
				match2 = match(e, b2, match2);
			if ((match1 > 0 && match1 == b1.size()) ||
				(match2 > 0 && match2 == b2.size()))
			{
				hideDockableWindow(name);
				match1 = match2 = 0;
			}
		}

		private int match(KeyEvent e, Vector<Key> shortcut, int index)
		{
			char c = e.getKeyChar();
			if (shortcut != null && c == shortcut.get(index).key)
				return index + 1;
			return 0;
		}

		private Vector<Key> parseShortcut(String shortcut)
		{
			Vector<Key> keys = new Vector<Key>();
			String [] parts = shortcut.split("\\s+");
			for (String part: parts)
			{
				if (part.length() > 0)
					keys.add(KeyEventTranslator.parseKey(part));
			}
			return keys;
		}
	} //}}}

	// {{{ DockingLayout class
	/**
	 * Objects of DockingLayout class describe which dockables are docked where,
	 * which ones are floating, and their sizes/positions for saving/loading perspectives.
	 */
	public abstract static class DockingLayout
	{
		public static final int NO_VIEW_INDEX = -1;
		public abstract boolean loadLayout(String baseName, int viewIndex);
		public abstract boolean saveLayout(String baseName, int viewIndex);
		public abstract String getName();

		public void setPlainView(boolean plain)
		{
		}

		public String [] getSavedLayouts()
		{
			String layoutDir = getLayoutDirectory();
			if (layoutDir == null)
				return null;
			File dir = new File(layoutDir);
			File[] files = dir.listFiles(new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(".xml");
				}
			});
			String[] layouts = new String[files.length];
			for (int i = 0; i < files.length; i++)
				layouts[i] = fileToLayout(files[i].getName());
			return layouts;
		}

		private static String fileToLayout(String filename)
		{
			return filename.replaceFirst(".xml", "");
		}

		private static String layoutToFile(String baseName, int viewIndex)
		{
			StringBuilder name = new StringBuilder(baseName);
			if (viewIndex != NO_VIEW_INDEX)
				name.append("-view").append(viewIndex);
			name.append(".xml");
			return name.toString();
		}

		public String getLayoutFilename(String baseName, int viewIndex)
		{
			String dir = getLayoutDirectory();
			if (dir == null)
				return null;
			return dir + File.separator + layoutToFile(baseName, viewIndex);
		}

		private String getLayoutDirectory()
		{
			String name = getName();
			if (name == null)
				return null;
			String dir = jEdit.getSettingsDirectory();
			if (dir == null)
				return null;
			dir = dir + File.separator + name;
			File d = new File(dir);
			if (!d.exists())
				d.mkdir();
			return dir;
		}
	} // }}}

} // }}}
