/*
 * BufferSegment.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Marcelo Vanzin
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
 * A read-only text segment from a buffer. Allows concatenation using a
 * "linked list" approach.
 *
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre10
 */
public class BufferSegment implements CharSequence
{

	public BufferSegment(char[] data, int offset, int len)
	{
		this(data,offset,len,null);
	}

	private BufferSegment(char[] data, int offset, int len,
				BufferSegment next)
	{
		this.data = data;
		this.offset = offset;
		this.len = len;
		this.next = next;
	}

	public char charAt(int index)
	{
		if (index < len)
			return data[offset+index];
		else if (next != null)
			return next.charAt(index-len);
		else
			throw new ArrayIndexOutOfBoundsException(index);
	}

	public int length()
	{
		return len + ((next != null) ? next.length() : 0);
	}

	public CharSequence subSequence(int start, int end)
	{
		if (start >= 0 && end - start < len)
			return new BufferSegment(data,offset+start,end-start);
		else
			throw new ArrayIndexOutOfBoundsException();
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		toString(sb);
		if (next != null)
			next.toString(sb);
		return sb.toString();
	}

	protected BufferSegment concat(BufferSegment other)
	{
		BufferSegment clone = new BufferSegment(data,offset,len,next);
		BufferSegment last = clone;
		while (last.next != null)
			last = last.next;
		last.next = other;
		return clone;
	}

	private void toString(StringBuilder sb)
	{
		sb.append(data,offset,len);
	}

	private char[] data;
	private int offset;
	private int len;
	private BufferSegment next;
}

