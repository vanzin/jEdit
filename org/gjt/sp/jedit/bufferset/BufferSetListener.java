/*
 * BufferSetListener.java - the listener for buffersets change
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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
package org.gjt.sp.jedit.bufferset;

import org.gjt.sp.jedit.Buffer;

import java.util.EventListener;

/**
 * This is the listener for the BufferSet changes.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public interface BufferSetListener extends EventListener
{
	/**
	 * A buffer was added in the bufferSet.
	 *
	 * @param buffer the buffer
	 * @param index the position where it was added
	 */
	void bufferAdded(Buffer buffer, int index);

	/**
	 * A buffer was removed from the bufferSet.
	 *
	 * @param buffer the removed buffer
	 * @param index the position where the buffer was
	 */
	void bufferRemoved(Buffer buffer, int index);

	/**
	 * A buffer was moved in the BufferSet.
	 *
	 * @param buffer the moved buffer
	 * @param oldIndex the old index
	 * @param newIndex the new index
	 */
	void bufferMoved(Buffer buffer, int oldIndex, int newIndex);

	/**
	 * The bufferSet was sorted.
	 */
	void bufferSetSorted();

}
