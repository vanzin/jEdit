/*
 * ContentManager.java - Manages text content
 * :tabSize=4:indentSize=4:noTabs=false:
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
class ContentManager
{
	//{{{ getLength() method
	public final int getLength()
	{
		return length;
	} //}}}

	//{{{ getText() methods
	public String getText(int start, int len)
	{
		if(start >= gapStart)
			return new String(text,start + gapLength(),len);
		else if(start + len <= gapStart)
			return new String(text,start,len);
		else
		{
			return new String(text,start,gapStart - start)
				.concat(new String(text,gapEnd(),start + len - gapStart));
		}
	}

	/**
	 * Returns the specified text range in a <code>Segment</code>.<p>
	 *
	 * Using a <classname>Segment</classname> is generally more
	 * efficient than using a <classname>String</classname> because it
	 * results in less memory allocation and array copying.<p>
	 *
	 *
	 * @param start The start offset
	 * @param len The number of characters to get
	 * @param seg The segment to copy the text to
	 * @see JEditBuffer#getText(int, int, Segment)
	 */
	public void getText(int start, int len, Segment seg)
	{
		if(start >= gapStart)
		{
			seg.array = text;
			seg.offset = start + gapLength();
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
			System.arraycopy(text,gapEnd(),seg.array,gapStart - start,
				len + start - gapStart);

			seg.offset = 0;
			seg.count = len;
		}
	} //}}}

	//{{{ getSegment() method
	/**
	 * Returns a read-only segment of the buffer.
	 * It doesn't copy the text
	 *
	 * @param start The start offset
	 * @param len The number of characters to get
	 *
	 * @return a CharSequence that contains the text wanted text
	 * @since jEdit 4.3pre15
	 */
	public CharSequence getSegment(int start, int len)
	{
		if(start >= gapStart)
			return new BufferSegment(text,start + gapLength(),len);
		else if(start + len <= gapStart)
			return new BufferSegment(text,start,len);
		else
		{
			return new BufferSegment(text,start,gapStart - start,
				new BufferSegment(text,gapEnd(),start + len - gapStart));
		}
	} //}}}

	//{{{ insert() methods
	public void insert(int start, String str)
	{
		int len = str.length();
		prepareGapForInsertion(start, len);
		str.getChars(0,len,text,start);
		gapStart += len;
		length += len;
	}

	/**
	 * Inserts the given data into the buffer.
	 *
	 * @since jEdit 4.3pre15
	 */
	public void insert(int start, CharSequence str)
	{
		int len = str.length();
		prepareGapForInsertion(start, len);
		for (int i = 0; i < len; i++)
		{
			text[start+i] = str.charAt(i);
		}
		gapStart += len;
		length += len;
	}

	public void insert(int start, Segment seg)
	{
		prepareGapForInsertion(start, seg.count);
		System.arraycopy(seg.array,seg.offset,text,start,seg.count);
		gapStart += seg.count;
		length += seg.count;
	} //}}}

	//{{{ _setContent() method
	public void _setContent(char[] text, int length)
	{
		assert text != null;
		assert text.length >= length;
		this.text = text;
		this.gapStart = length;
		this.length = length;
	} //}}}

	//{{{ remove() method
	public void remove(int start, int len)
	{
		moveGapStart(start);
		length -= len;
	} //}}}

	//{{{ Private members
	private static final char[] EMPTY_TEXT = new char[0];
	private char[] text = EMPTY_TEXT;
	private int gapStart;
	private int length;

	//{{{ gapEnd() method
	private int gapEnd()
	{
		return gapStart + gapLength();
	} //}}}

	//{{{ gapLength() method
	private int gapLength()
	{
		return text.length - length;
	} //}}}

	//{{{ moveGapStart() method
	private void moveGapStart(int newStart)
	{
		int gapEnd = gapEnd();
		int newEnd = gapEnd + (newStart - gapStart);

		if(newStart == gapStart)
		{
			// nothing to do
		}
		else if(newStart > gapStart)
		{
			System.arraycopy(text,gapEnd,text,gapStart,
				newStart - gapStart);
		}
		else if(newStart < gapStart)
		{
			System.arraycopy(text,newStart,text,newEnd,
				gapStart - newStart);
		}

		gapStart = newStart;
	} //}}}

	//{{{ ensureCapacity() method
	private void ensureCapacity(int capacity)
	{
		if(capacity >= text.length)
		{
			int gapEndOld = gapEnd();

			char[] textN = new char[capacity * 2];
			System.arraycopy(text,0,textN,0,text.length);
			text = textN;

			int gapEndNew = gapEnd();
			System.arraycopy(text,gapEndOld,text,gapEndNew,text.length - gapEndNew);
		}
	} //}}}

	//{{{ prepareGapForInsertion() method
	private void prepareGapForInsertion(int start, int len)
	{
		moveGapStart(start);
		if(gapLength() < len)
			ensureCapacity(length + len);
	} //}}}

	//}}}
}
