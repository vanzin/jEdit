/*
 * BufferChangeListener.java - Buffer listener interface
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
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

import org.gjt.sp.jedit.Buffer;

/**
 * A interface for notification of changes to buffer text. While the
 * {@link org.gjt.sp.jedit.msg.BufferUpdate} EditBus message is used for
 * general buffer state changes, this interface is used for events which are
 * fired frequently, or for which performance is essential.<p>
 *
 * Because this interface is subject to change in the future, you
 * should subclass <code>BufferChangeAdapter</code> instead of
 * implementing it directly.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public interface BufferChangeListener
{
	//{{{ foldLevelChanged() method
	/**
	 * Called when line fold levels change.
	 * @param buffer The buffer in question
	 * @param start The start line number
	 * @param end The end line number
	 * @since jEdit 4.0pre1
	 */
	void foldLevelChanged(Buffer buffer, int startLine, int endLine);
	//}}}

	//{{{ contentInserted() method
	/**
	 * Called when text is inserted into the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines inserted
	 * @param length The number of characters inserted
	 * @since jEdit 4.0pre1
	 */
	void contentInserted(Buffer buffer, int startLine, int offset,
		int numLines, int length);
	//}}}

	//{{{ contentRemoved() method
	/**
	 * Called when text is removed from the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines removed
	 * @param length The number of characters removed
	 * @since jEdit 4.0pre1
	 */
	void contentRemoved(Buffer buffer, int startLine, int offset,
		int numLines, int length);
	//}}}

	//{{{ transactionComplete() method
	/**
	 * Called after an undo or compound edit has finished. The text area
	 * uses this event to queue up and collapse cleanup operations so they
	 * are only run once during a long transaction (such as a "Replace All"
	 * operation.)
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.0pre6
	 */
	void transactionComplete(Buffer buffer);
	//}}}
}
