/*
 * ContentManager.java - Manages text content
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

import javax.swing.text.Segment;

/**
 * A class internal to jEdit's document model. You should not use it
 * directly. To improve performance, none of the methods in this class
 * check for out of bounds access, nor are they thread-safe. The
 * <code>Buffer</code> class, through which these methods must be
 * called through, implements such protection.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class ContentManager
{
	//{{{ ContentManager constructor
	public ContentManager()
	{
		text = new char[1024];
	} //}}}

	//{{{ getLength() method
	public final int getLength()
	{
		return length;
	} //}}}

	//{{{ getText() method
	public void getText(int start, int len, Segment seg)
	{
		if(start > gapStart)
			start += (gapEnd - gapStart);

		// optimization
		if(start + length <= gapStart || start > gapStart)
		{
			seg.array = text;
			seg.offset = start;
			seg.count = len;
		}
		else
		{
			if(seg.array == null || seg.array.length < len)
				seg.array = new char[len];
			System.arraycopy(text,start,seg.array,0,len);
			seg.offset = 0;
			seg.count = len;
		}
	} //}}}

	//{{{ insert() method
	public void insert(int start, Segment str)
	{
		close(start,start + str.count * 2);
		System.arraycopy(str.array,str.offset,text,start,str.count);
	} //}}}

	//{{{ remove() method
	public void remove(int start, int len)
	{
		close(start,start);
		gapEnd += len;
		length -= len;
	} //}}}

	//{{{ Private members
	private char[] text;
	private int gapStart;
	private int gapEnd;
	private int length;

	//{{{ close() method
	private void close(int newStart, int newEnd)
	{
		if(gapStart != gapEnd)
		{
			System.arraycopy(text,gapEnd,text,gapStart,
				length - gapEnd);
		}

		if(newStart != newEnd)
		{
			ensureCapacity(length + (newEnd - newStart));
			System.arraycopy(text,newStart,text,newEnd,
				length - newStart);
		}

		gapStart = newStart;
		gapEnd = newEnd;
	} //}}}

	//{{{ ensureCapacity() method
	private void ensureCapacity(int capacity)
	{
		if(text.length > capacity)
		{
			char[] textN = new char[capacity * 2];
			System.arraycopy(text,0,textN,0,length);
			text = textN;
		}
	} //}}}

	//}}}
}
