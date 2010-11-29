/*
 * BufferSetAdapter.java - the default implementation for the bufferSet listener
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

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSetAdapter implements BufferSetListener
{
	public void bufferAdded(Buffer buffer, int index)
	{
	}

	public void bufferRemoved(Buffer buffer, int index)
	{
	}

	public void bufferMoved(Buffer buffer, int oldIndex, int newIndex)
	{
	}

	public void bufferSetSorted()
	{
	}

}
