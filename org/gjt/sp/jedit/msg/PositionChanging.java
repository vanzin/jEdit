package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;


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
 * For jumps to a different buffer entirely, it is not necessary for plugins
 * to send any message, since BufferChanging is sent by jEdit whenever 
 * EditPane.setBuffer() is called, and it serves as a PositionChanging message
 * also.
 * 
 *
 * @see org.gjt.sp.jedit.msg.BufferChanging
 * @author ezust
 * @since jEdit 4.3pre15
 *
 */
public class PositionChanging extends EditPaneUpdate
{
	
	protected PositionChanging(EditPane editPane, Object whatt)
	{
		super(editPane, whatt);
	}

	public PositionChanging(TextArea textArea)
	{
		super(EditPane.get(textArea), EditPaneUpdate.POSITION_CHANGING);
	}
	
	public PositionChanging(EditPane editPane)
	{
		super (editPane, EditPaneUpdate.POSITION_CHANGING);
	}
}
