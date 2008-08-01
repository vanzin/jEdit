package org.gjt.sp.jedit.gui;

import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.xml.sax.helpers.DefaultHandler;

public abstract class DockableWindowManagerBase extends JPanel implements EBComponent {

	public static abstract class DockingLayout {
		abstract public DefaultHandler getPerspectiveHandler();
		abstract public void savePerspective(SettingsXML.Saver out, String lineSep) throws IOException;
		abstract public void move(int x, int y);
		abstract public int getX();
		abstract public int getY();
		abstract public int getWidth();
		abstract public int getHeight();
	}

	/*
	 * Docking framework interface methods
	 */
	public void setDockingLayout(DockingLayout docking) {
		
	}
	public void showDockableWindow(String name) {
		
	}
	public void hideDockableWindow(String name) {
		
	}
	public JComponent getDockable(String name) {
		return null;
	}
	public String getDockableTitle(String name) {
		return null;
	}
	public void setDockableTitle(String dockable, String title) {
		
	}
	public boolean isDockableWindowDocked(String name) {
		return false;
	}
	public boolean isDockableWindowVisible(String name) {
		return false;
	}
	public void closeCurrentArea() {
		
	}
	public void close() {
		
	}
	public DockingLayout getDockingLayout(ViewConfig config) {
		return null;
	}
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
