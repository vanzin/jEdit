package org.gjt.sp.jedit.gui;

import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.util.Log;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
public abstract class DockableWindowManagerBase extends JPanel implements EBComponent
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
	abstract public void adjust(View view, ViewConfig config);
	abstract public void setDockingLayout(DockingLayout docking);
	abstract public void showDockableWindow(String name);
	abstract public void hideDockableWindow(String name);
	abstract public JComponent floatDockableWindow(String name);
	abstract public void setTopToolbars(JPanel toolbars);
	abstract public void setBottomToolbars(JPanel toolbars);
	public JComponent getDockable(String name)
	{
		return windows.get(name);
	}
	abstract public String getDockableTitle(String name);
	abstract public void setDockableTitle(String dockable, String title);
	abstract public boolean isDockableWindowDocked(String name);
	abstract public boolean isDockableWindowVisible(String name);
	abstract public void closeCurrentArea();
	abstract public void close();
	abstract public DockingLayout getDockingLayout(ViewConfig config);
	abstract public KeyListener closeListener(String dockableName);

	/*
	 * Data members
	 */
	protected View view;
	protected DockableWindowFactory factory;
	protected Map<String, JComponent> windows = new HashMap<String, JComponent>();
	
	/*
	 * Base class methods
	 */
	public void handleMessage(EBMessage msg) {
		
	}
	private void propertiesChanged() {
		
	}
	public DockableWindowManagerBase(View view, DockableWindowFactory instance,
			ViewConfig config)
	{
		this.view = view;
		this.factory = instance;
	}	
	public void init()
	{
		EditBus.addToBus(this);
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
	/*
	 * Derived methods
	 */
	public void addDockableWindow(String name) {
		showDockableWindow(name);
	}
	public void removeDockableWindow(String name) {
		hideDockableWindow(name);
	}
	public void toggleDockableWindow(String name) {
		if (isDockableWindowVisible(name))
			hideDockableWindow(name);
		else
			showDockableWindow(name);
	}
	public JComponent getDockableWindow(String name) {
		return getDockable(name);
	}
}
