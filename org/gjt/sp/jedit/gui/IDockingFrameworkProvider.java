package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.xml.sax.helpers.DefaultHandler;

public interface IDockingFrameworkProvider {
	DockableWindowManagerBase create(View view, DockableWindowFactory instance,
			ViewConfig config);
	DefaultHandler getPerpsectiveHandler();
}
