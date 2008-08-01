package org.gjt.sp.jedit.gui;

import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
public abstract class DockableWindowManagerBase extends JPanel implements EBComponent {

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
	abstract public JComponent getDockable(String name);
	abstract public String getDockableTitle(String name);
	abstract public void setDockableTitle(String dockable, String title);
	abstract public boolean isDockableWindowDocked(String name);
	abstract public boolean isDockableWindowVisible(String name);
	abstract public void closeCurrentArea();
	abstract public void close();
	abstract public DockingLayout getDockingLayout(ViewConfig config);
	abstract public KeyListener closeListener(String dockableName);

	/*
	 * Base class methods
	 */
	public void handleMessage(EBMessage msg) {
		
	}
	private void propertiesChanged() {
		
	}
	public DockableWindowManagerBase(View view, DockableWindowFactory instance,
			ViewConfig config) {
	}	
	public void init() {
		
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
