/*
 * BufferChanging.java - Buffer changing (specialized Edit Pane update message)
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Alan Ezust
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
 
package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;

/** An EBMessage sent by the EditPane just before the buffer changes.
 * It is also sent by some plugins just before the caret position is changed. 
 * In hindsight, a better name for this class would be PositionChanging.
 * 
 * Plugins may emit this message just before they perform a 
 * Navigation-Like operation, such as jumping the cursor to 
 * another location (which may be in the same buffer or not). This is mostly 
 * for the benefit of the Navigator plugin, but may be used by other plugins
 * too, such as BufferLocal.
 * 
 * @since jEdit 4.3pre4
 * @version $Id$
 */
public class BufferChanging extends EditPaneUpdate
{
	/**
	 * @param editPane the editPane that sent the message
	 * @param newBuffer the buffer that will soon be displayed, or null if this is
	 * 		a jump to the same buffer. 
	 * 
	 */
	public BufferChanging(EditPane editPane, Buffer newBuffer) {
		super(editPane, BUFFER_CHANGING);
		m_buffer = newBuffer;
	}
	
	/**
	 * @return the new buffer that is about to be displayed. This value can sometimes be null,
	 * in the case where a plugin is changing to another position in the current buffer.
	 */
	public Buffer getBuffer() {
		return m_buffer;
	}

	private Buffer m_buffer;
}
