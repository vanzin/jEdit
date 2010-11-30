/*
 * BufferAdapter.java - Buffer listener adapter
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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
 * An adapter you can subclass to avoid having to implement all the methods
 * of the {@link BufferListener} interface.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.3pre3
 */
public abstract class BufferAdapter implements BufferListener
{
	//{{{ foldLevelChanged() method
	/**
	 * Called when line fold levels change.
	 * @param buffer The buffer in question
	 * @param start The start line number
	 * @param end The end line number
	 * @since jEdit 4.3pre3
	 */
	public void foldLevelChanged(JEditBuffer buffer, int start, int end)
	{
	} //}}}

	//{{{ contentInserted() method
	/**
	 * Called when text is inserted into the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines inserted
	 * @param length The number of characters inserted
	 * @since jEdit 4.3pre3
	 */
	public void contentInserted(JEditBuffer buffer, int startLine, int offset,
		int numLines, int length) {}
	//}}}

	//{{{ preContentInserted() method
	/**
	 * Called when text is about to be inserted in the buffer.
	 *
	 * @param buffer    The buffer in question
	 * @param startLine The first line
	 * @param offset    The start offset, from the beginning of the buffer
	 * @param numLines  The number of lines inserted
	 * @param length    The number of characters inserted
	 * @since jEdit 4.3pre11
	 */
	public void preContentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length)
	{
	} //}}}

	//{{{ preContentRemoved() method
	/**
	 * Called when text is about to be removed from the buffer, but is
	 * still present.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines to be removed
	 * @param length The number of characters to be removed
	 * @since jEdit 4.3pre3
	 */
	public void preContentRemoved(JEditBuffer buffer, int startLine, int offset,
		int numLines, int length) {}
	//}}}

	//{{{ contentRemoved() method
	/**
	 * Called when text is removed from the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines removed
	 * @param length The number of characters removed
	 * @since jEdit 4.3pre3
	 */
	public void contentRemoved(JEditBuffer buffer, int startLine, int offset,
		int numLines, int length) {}
	//}}}

	//{{{ transactionComplete() method
	/**
	 * Called after an undo or compound edit has finished. The text area
	 * uses this event to queue up and collapse cleanup operations so they
	 * are only run once during a long transaction (such as a "Replace All"
	 * operation.)
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.3pre3
	 */
	public void transactionComplete(JEditBuffer buffer) {}
	//}}}

	//{{{ foldHandlerChanged() method
	/**
	 * Called to notify the text area that folds need to be collapsed if
	 * the "collapseFolds" property is set. This method is called after the
	 * buffer has been loaded, and also if the user changes the fold
	 * handler.
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.3pre3
	 */
	public void foldHandlerChanged(JEditBuffer buffer) {}
	//}}}

	//{{{ foldHandlerChanged() method
	/**
	 * Called to notify the text area that the buffer has been reloaded.
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.3pre3
	 */
	public void bufferLoaded(JEditBuffer buffer) {}
	//}}}
}
