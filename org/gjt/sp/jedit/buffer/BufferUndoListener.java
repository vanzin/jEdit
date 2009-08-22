/*
 * BufferUndoListener.java - Buffer undo listener interface
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2009 Shlomy Reinstein
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

package org.gjt.sp.jedit.buffer;

/**
 * A interface for notification of buffer undo/redo actions.
 *
 * This interface makes it easier for undo-aware plugins to process
 * undo/redo actions in a buffer.
 *
 * Buffer undo listeners are added and removed from a buffer using
 * <code>JEditBuffer.addBufferUndoListener<code> and
 * <code>JEditBuffer.removeBufferUndoListener<code>, respectively.
 *
 * @author Shlomy Reinstein
 * @version $Id$
 * @since jEdit 4.3pre18
 */
public interface BufferUndoListener
{
	//{{{ undo() method
	/**
	 * Called when undo is called on the buffer.
	 * @param buffer The buffer in question
	 */
	void undo(JEditBuffer buffer);
	//}}}

	//{{{ redo() method
	/**
	 * Called when redo is called on the buffer.
	 * @param buffer The buffer in question
	 */
	void redo(JEditBuffer buffer);
	//}}}
}
