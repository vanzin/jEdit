package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;


/**
   jEdit's classic dockable window manager, turned into a "provider" service.
   
   @author Shlomy Reinstein
   @since jEdit 4.3pre16
*/   
public class DockableWindowManagerProvider implements DockingFrameworkProvider
{
	public DockableWindowManager create(View view,
			DockableWindowFactory instance, ViewConfig config)
	{
		return new DockableWindowManagerImpl(view, instance, config);
	}

	public DockingLayout createDockingLayout()
	{
		return new DockableWindowManagerImpl.DockableWindowConfig();
	}

}
