/*
 * BufferChangeListener.java - Buffer listener adapter
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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
 * An adapter you can subclass to avoid having to implement all the methods
 * of the <code>BufferChangeListener</code> interface.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public abstract class BufferChangeAdapter implements BufferChangeListener
{
	//{{{ foldLevelChanged() method
	/**
	 * Called when the fold level of a line changes.
	 * @param buffer The buffer in question
	 * @param line The line number
	 * @since jEdit 4.0pre1
	 */
	public void foldLevelChanged(Buffer buffer, int line)
	{
	} //}}}

	//{{{ contentInserted() method
	/**
	 * Called when text is inserted into the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param start The start offset, from the beginning of the buffer
	 * @param numLines The number of lines inserted
	 * @param length The number of characters inserted
	 * @since jEdit 4.0pre1
	 */
	public void contentInserted(Buffer buffer, int startLine, int start,
		int numLines, int length) {}
	//}}}

	//{{{ contentRemoved() method
	/**
	 * Called when text is removed from the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param start The start offset, from the beginning of the buffer
	 * @param numLines The number of lines removed
	 * @param length The number of characters removed
	 * @since jEdit 4.0pre1
	 */
	public void contentRemoved(Buffer buffer, int startLine, int start,
		int numLines, int length) {}
	//}}}
}
