package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;


/** Base interface for Docking Framework Provider services.
    Plugins can offer alternate docking frameworks by offering a service
    that creates an instance of one of these.
    For an example, see jEdit's own services.xml, which provides jEdit's classic
    docking framework via the class DockableWindowManagerProvider.

    @since jEdit 4.3pre16
    @author Shlomy Reinstein
*/
public interface DockingFrameworkProvider {
	DockableWindowManager create(View view, DockableWindowFactory instance,
			ViewConfig config);
	DockingLayout createDockingLayout();
}
