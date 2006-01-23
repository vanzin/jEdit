package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;

/** An EBMessage sent by the 
 * EditPane just before the buffer changes.
 * 
 * @since jEdit 4.3pre4
 * @version $Id$
 */
public class BufferChanging extends EditPaneUpdate
{
	/**
	 * 
	 * @param editPane the editPane that sent the message
	 * @param newBuffer the buffer that will soon be displayed.
	 */
	public BufferChanging(EditPane editPane, Buffer newBuffer) {
		super(editPane, BUFFER_CHANGING);
		m_buffer = newBuffer;
	}
	
	public Buffer getBuffer() {
		return m_buffer;
	}

	private Buffer m_buffer;
}
