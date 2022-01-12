/*
 * SegmentBuffer.java - A Segment you can append stuff to
 * :tabSize=4:indentSize=4:noTabs=false:
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

package org.gjt.sp.util;

import javax.swing.text.Segment;

/**
 * An extended segment that you can append text to.
 */
public class SegmentBuffer extends Segment
{
	//{{{ SegmentBuffer constructor
	public SegmentBuffer(int capacity)
	{
		ensureCapacity(capacity);
	} //}}}

	//{{{ append() methods
	public void append(char ch)
	{
		ensureCapacity(count + 1);
		array[offset + count] = ch;
		count++;
	}

	/**
	 * @param text the text to append
	 * @since jEdit 5.7pre1
	 */
	public void append(char[] text)
	{
		append(text, 0, text.length);
	}

	public void append(char[] text, int off, int len)
	{
		ensureCapacity(count + len);
		System.arraycopy(text,off,array,count,len);
		count += len;
	} //}}}

	//{{{ insert() methods
	/**
	 * Insert some text
	 * @param index the position where the text will be inserted
	 * @param str the text to insert
	 * @since jEdit 5.7pre1
	 */
	public void insert(int index, char[] str)
	{
		insert(index, str, 0, str.length);
	}

	/**
	 * Insert some text
	 * @param index the position where the text will be inserted
	 * @param str the text to insert
	 * @param offset the start position in the inserted array
	 * @param len the length to be copied
	 * @since jEdit 5.7pre1
	 */
	public void insert(int index, char[] str, int offset, int len)
	{
		ensureCapacity(count + len);
		// insert gap
		System.arraycopy(array, index, array, index + len, count - index);
		System.arraycopy(str, offset, array, index, len);
		count += len;
	} //}}}

	//{{{ Private members

	//{{{ ensureCapacity() method
	private void ensureCapacity(int capacity)
	{
		if(array == null)
			array = new char[capacity];
		else if(capacity >= array.length)
		{
			char[] arrayN = new char[capacity * 2];
			System.arraycopy(array,0,arrayN,0,count);
			array = arrayN;
		}
	} //}}}

	//}}}
}
