package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.gui.DockableWindowManagerBase.DockingLayout;

public interface IDockingFrameworkProvider {
	DockableWindowManagerBase create(View view, DockableWindowFactory instance,
			ViewConfig config);
	DockingLayout createDockingLayout();
}
