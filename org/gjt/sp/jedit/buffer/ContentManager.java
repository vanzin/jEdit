/*
 * ContentManager.java - Manages text content
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
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
	public String getText(int start, int len)
	{
		if(start >= gapStart)
			return new String(text,start + gapEnd - gapStart,len);
		else if(start + len <= gapStart)
			return new String(text,start,len);
		else
		{
			return new String(text,start,gapStart - start)
				.concat(new String(text,gapEnd,start + len - gapStart));
		}
	} //}}}

	//{{{ getText() method
	public void getText(int start, int len, Segment seg)
	{
		if(start >= gapStart)
		{
			seg.array = text;
			seg.offset = start + gapEnd - gapStart;
			seg.count = len;
		}
		else if(start + len <= gapStart)
		{
			seg.array = text;
			seg.offset = start;
			seg.count = len;
		}
		else
		{
			seg.array = new char[len];

			// copy text before gap
			System.arraycopy(text,start,seg.array,0,gapStart - start);

			// copy text after gap
			System.arraycopy(text,gapEnd,seg.array,gapStart - start,
				len + start - gapStart);

			seg.offset = 0;
			seg.count = len;
		}
	} //}}}

	//{{{ insert() method
	public void insert(int start, String str)
	{
		int len = str.length();
		if(gapStart != start || gapEnd - gapStart < len)
		{
			ensureCapacity(length + len + 200);
			close(start,start + len + 200);
		}
		str.getChars(0,len,text,start);
		gapStart += len;
		length += len;
	} //}}}

	//{{{ insert() method
	public void insert(int start, Segment seg)
	{
		if(gapStart != start || gapEnd - gapStart < seg.count)
		{
			ensureCapacity(length + seg.count + 200);
			close(start,start + seg.count + 200);
		}
		System.arraycopy(seg.array,seg.offset,text,start,seg.count);
		gapStart += seg.count;
		length += seg.count;
	} //}}}

	//{{{ _setContent() method
	public void _setContent(char[] text, int length)
	{
		this.text = text;
		this.gapStart = this.gapEnd = 0;
		this.length = length;
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
		// Optimization
		if(newStart == gapStart)
		{
			System.arraycopy(text,gapEnd,text,newEnd,length - gapStart);
		}
		else
		{
			if(gapStart != gapEnd && gapStart != length)
			{
				System.arraycopy(text,gapEnd,text,gapStart,
					length - gapStart);
			}

			if(newStart != newEnd && newStart != length)
			{
				System.arraycopy(text,newStart,text,newEnd,
					length - newStart);
			}
		}

		gapStart = newStart;
		gapEnd = newEnd;
	} //}}}

	//{{{ ensureCapacity() method
	private void ensureCapacity(int capacity)
	{
		if(capacity >= text.length)
		{
			char[] textN = new char[capacity * 2];
			System.arraycopy(text,0,textN,0,length + (gapEnd - gapStart));
			text = textN;
		}
	} //}}}

	//}}}
}
