/*
 * Marker.java - Named location in a buffer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

import javax.swing.text.Position;

/**
 * Buffers may contain one or more <i>markers</i> which serve
 * as textual bookmarks.<p>
 *
 * A <code>Marker</code> has three key attributes: the
 * <code>Buffer</code> to which it relates, the line number to which
 * the marker refers, and an optional shortcut character. The shortcut
 * identifies the the key that can be pressed with the
 * <b>Markers</b>&gt;<b>Go To Marker</b> command.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class Marker
{
	//{{{ getShortcut() method
	/**
	 * Returns the marker's shortcut character.
	 * @since jEdit 3.2pre1
	 */
	public char getShortcut()
	{
		return shortcut;
	} //}}}

	//{{{ getPosition() method
	/**
	 * Returns the position of this marker.
	 * @since jEdit 3.2pre1
	 */
	public int getPosition()
	{
		return (position == null ? pos : position.getOffset());
	} //}}}

	//{{{ Package-private members

	//{{{ Marker constructor
	Marker(Buffer buffer, char shortcut, int position)
	{
		this.buffer = buffer;
		this.shortcut = shortcut;
		this.pos = position;
	} //}}}

	//{{{ setShortcut() method
	/**
	 * Sets the marker's shortcut.
	 * @param shortcut The new shortcut
	 * @since jEdit 3.2pre1
	 */
	void setShortcut(char shortcut)
	{
		this.shortcut = shortcut;
	} //}}}

	//{{{ createPosition() method
	void createPosition()
	{
		position = buffer.createPosition(pos);
	} //}}}

	//{{{ removePosition() method
	void removePosition()
	{
		// forget the cached Position instance
		if(position != null)
		{
			pos = position.getOffset();
			position = null;
		}
	} //}}}

	//{{{ setPosition() method
	/**
	 * Sets the position of this marker.
	 * @since jEdit 4.0pre5
	 */
	void setPosition(int pos)
	{
		this.pos = pos;
	} //}}}

	//}}}

	//{{{ Private members
	private Buffer buffer;
	private char shortcut;
	private int pos;
	private Position position;
	//}}}
}
