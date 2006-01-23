package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;

/** An EBMessage sent by the 
 * EditPane just before the buffer changes.
 * 
 * @author ezust
 * @since jEdit 4.3pre4
 */
public class BufferChanging extends EditPaneUpdate
{
	/**
	 * 
	 * @param what the editPane that sent the message
	 * @param newBuffer the buffer that will soon be displayed.
	 */
	public BufferChanging(EditPane what, Buffer newBuffer) {
		super(what, BUFFER_CHANGING);
		m_buffer = newBuffer;
	}
	
	public Buffer getBuffer() {
		return m_buffer;
	}

	private Buffer m_buffer;
}
