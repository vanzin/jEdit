package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EditPane;


/**
 * An EBMessage associated with an EditPane that is sent just before its caret 
 * position changes in a "major way". 
 * These messages are tracked by the Navigator plugin, 
 * and other interested plugins.
 * 
 * @see org.gjt.sp.jedit.msg.BufferChanging
 * @author ezust
 * @since jEdit 4.3pre15
 *
 */
public class PositionChanging extends EditPaneUpdate {
	
	protected PositionChanging(EditPane editPane, Object whatt) {
		super(editPane, whatt);
	}
	
	public PositionChanging(EditPane editPane) {
		super (editPane, EditPaneUpdate.POSITION_CHANGING);
	}
}
