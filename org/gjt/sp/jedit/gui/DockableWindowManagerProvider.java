package org.gjt.sp.jedit.gui;

import javax.swing.JPanel;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;
import org.xml.sax.helpers.DefaultHandler;

public class DockableWindowManagerProvider implements IDockingFrameworkProvider {

	public DockableWindowManager create(View view,
			DockableWindowFactory instance, ViewConfig config) {
		return new DockableWindowManagerImpl(view, instance, config);
	}

	public DockingLayout createDockingLayout() {
		return new DockableWindowManagerImpl.DockableWindowConfig();
	}

}
