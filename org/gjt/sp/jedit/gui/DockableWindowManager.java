package org.gjt.sp.jedit.gui;

import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.msg.DockableWindowUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
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

	public static abstract class DockingLayout {
		public DockingLayout() {
		}
		abstract public void setPlainView(boolean plain);
		abstract public DefaultHandler getPerspectiveHandler();
		abstract public void savePerspective(SettingsXML.Saver out, String lineSep) throws IOException;
		abstract public void move(int dx, int dy);
	}

	/*
	 * Docking framework interface methods
	 */
	abstract public void setMainPanel(JPanel panel);
	abstract public void adjust(View view, ViewConfig config);
	public void setDockingLayout(DockingLayout docking)
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
	}
	abstract public void showDockableWindow(String name);
	abstract public void hideDockableWindow(String name);
	abstract public JComponent floatDockableWindow(String name);
	abstract public boolean isDockableWindowDocked(String name);
	abstract public boolean isDockableWindowVisible(String name);
	abstract public void closeCurrentArea();
	abstract public void close();
	abstract public DockingLayout getDockingLayout(ViewConfig config);
	abstract public KeyListener closeListener(String dockableName);
	protected void dockingPositionChanged(String dockableName,
		String oldPosition, String newPosition)
	{
	}
	
	/*
	 * Data members
	 */

	protected View view;
	protected DockableWindowFactory factory;
	protected Map<String, JComponent> windows = new HashMap<String, JComponent>();
	
	/*
	 * Base class methods
	 */

	//{{{ getView() method
	/**
	 * Returns this dockable window manager's view.
	 * @since jEdit 4.0pre2
	 */
	public View getView()
	{
		return view;
	} //}}}

	//{{{ getRegisteredDockableWindows() method
	/**
	 * @since jEdit 4.3pre2
	 */
	public JComponent getDockable(String name)
	{
		return windows.get(name);
	}
	
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
	 * @param dockableName the name of the dockable, as specified in the dockables.xml
	 * @param newTitle the new .longtitle you want to see above it.
	 * @since 4.3pre5
	 * 
	 */
	public void setDockableTitle(String dockable, String title)
	{
		String propName = getLongTitlePropertyName(dockable);
		String oldTitle = jEdit.getProperty(propName);
		jEdit.setProperty(propName, title);
		firePropertyChange(propName, oldTitle, title);		
	}
	// }}}
	
	//{{{ getDockableTitle() method
	public static String[] getRegisteredDockableWindows()
	{
		return DockableWindowFactory.getInstance()
			.getRegisteredDockableWindows();
	} //}}}
	
	public void handleMessage(EBMessage msg) {
		if (msg instanceof DockableWindowUpdate)
		{
			if(((DockableWindowUpdate)msg).getWhat() ==	DockableWindowUpdate.PROPERTIES_CHANGED)
				propertiesChanged();
		}
		else if (msg instanceof PropertiesChanged)
			propertiesChanged();
	}
	
	private Map<String, String> positions = new HashMap<String, String>();
	
	private void propertiesChanged()
	{
		if(view.isPlainView())
			return;

		String[] dockables = factory.getRegisteredDockableWindows();
		for(int i = 0; i < dockables.length; i++)
		{
			String dockable = dockables[i];
			String oldPosition = positions.get(dockable);
			String newPosition = getDockablePosition(dockable);
			if ((oldPosition == null) || (! newPosition.equals(oldPosition)))
			{
				positions.put(dockable, newPosition);
				dockingPositionChanged(dockable, oldPosition, newPosition);
			}
		}
		
	}
	public DockableWindowManager(View view, DockableWindowFactory instance,
			ViewConfig config)
	{
		this.view = view;
		this.factory = instance;
	}	
	public void init()
	{
		EditBus.addToBus(this);

		Iterator<DockableWindowFactory.Window> entries = factory.getDockableWindowIterator();
		while(entries.hasNext())
		{
			DockableWindowFactory.Window window = entries.next();
			String dockable = window.name;
			positions.put(dockable, getDockablePosition(dockable));
		}
	}
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
	}
	protected String getDockablePosition(String name)
	{
		return jEdit.getProperty(name + ".dock-position", FLOATING);
	}
	private String getLongTitlePropertyName(String dockableName)
	{
		return dockableName + ".longtitle";
	}
	public String longTitle(String name) 
	{
		String title = jEdit.getProperty(getLongTitlePropertyName(name));
		if (title == null)
			return shortTitle(name);
		return title;
	}
	public String shortTitle(String name)
	{		
		String title = jEdit.getProperty(name + ".title");
		if(title == null)
			return "NO TITLE PROPERTY: " + name;
		return title;
	}

	/*
	 * Derived methods
	 */
	
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

}
