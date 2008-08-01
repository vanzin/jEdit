package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.xml.sax.helpers.DefaultHandler;

public class DockableWindowManagerProvider implements IDockingFrameworkProvider {

	@Override
	public DockableWindowManagerBase create(View view,
			DockableWindowFactory instance, ViewConfig config) {
		return new DockableWindowManager(view, instance, config);
	}

	@Override
	public DefaultHandler getPerpsectiveHandler() {
		// TODO Auto-generated method stub
		return null;
	}

}
