/*
 * CharIndexedSegment.java
 * Copyright (C) 1998 Wes Biggs
 * Copyright (C) 2000 Slava Pestov
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.gjt.sp.jedit.search;

import java.io.Serializable;
import javax.swing.text.Segment;
import gnu.regexp.*;

public class CharIndexedSegment implements CharIndexed, Serializable
{
	private Segment seg;
	private int m_index;

	CharIndexedSegment(Segment seg, int index)
	{
		this.seg = seg;
		m_index = index;
	}

	public char charAt(int index)
	{
		return ((m_index + index) < seg.count) ? seg.array[seg.offset
			+ m_index + index] : CharIndexed.OUT_OF_BOUNDS;
	}

	public boolean isValid()
	{
		return (m_index < seg.count);
	}

	public boolean move(int index)
	{
		return ((m_index += index) < seg.count);
	}
}
