package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EditPane;


/**
 * An EBMessage associated with an EditPane that is sent just before its caret 
 * position changes in a "major way" to another location in the same Buffer.
 * These messages are tracked by the Navigator plugin, 
 * and other interested plugins.
 * 
 * jEdit plugins such as SideKick, Tags, Jump, CscopeFinder, etc, should 
 * emit this message whenever the user wants to jump from one position
 * to another in the same buffer.
 * 
 * For jumps to a different buffer entirely, it is not necessary to send an
 * EBMessage, since a BufferChanging message will be sent by jEdit core.
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
