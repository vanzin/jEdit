/*
 * Marker.java - Named location in a buffer
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import org.gjt.sp.util.Log;

/**
 * A named location in a buffer.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Marker
{
	/**
	 * Returns the marker's shortcut.
	 * @since jEdit 3.2pre1
	 */
	public char getShortcut()
	{
		return shortcut;
	}

	/**
	 * Sets the marker's shortcut.
	 * @param shortcut The new shortcut
	 * @since jEdit 3.2pre1
	 */
	public void setShortcut(char shortcut)
	{
		this.shortcut = shortcut;
	}

	/**
	 * Returns the position of this marker.
	 * @since jEdit 3.2pre1
	 */
	public int getPosition()
	{
		return (position == null ? pos : position.getOffset());
	}

	// package-private members
	Marker(Buffer buffer, char shortcut, int position)
	{
		this.buffer = buffer;
		this.shortcut = shortcut;
		this.pos = position;
	}

	void createPosition()
	{
		try
		{
			position = buffer.createPosition(pos);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	void removePosition()
	{
		// forget the cached Position instance
		position = null;
	}

	// private members
	private Buffer buffer;
	private char shortcut;
	private int pos;
	private Position position;
}
