/*
 * Selection.java - Selected text
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

package org.gjt.sp.jedit.textarea;

import org.gjt.sp.jedit.Buffer;

/**
 * An interface representing a portion of the current selection.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 3.2pre1
 */
public abstract class Selection
{
	//{{{ getStart() method
	/**
	 * Returns the start offset of this selection.
	 */
	public int getStart()
	{
		return start;
	} //}}}

	//{{{ getEnd() method
	/**
	 * Returns the end offset of this selection.
	 */
	public int getEnd()
	{
		return end;
	} //}}}

	//{{{ getStartLine() method
	/**
	 * Returns the starting line number of this selection.
	 */
	public int getStartLine()
	{
		return startLine;
	} //}}}

	//{{{ getEndLine() method
	/**
	 * Returns the ending line number of this selection.
	 */
	public int getEndLine()
	{
		return endLine;
	} //}}}

	//{{{ getStart() method
	/**
	 * Returns the start offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 */
	public abstract int getStart(Buffer buffer, int line);
	//}}}

	//{{{ getEnd() method
	/**
	 * Returns the end offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 */
	public abstract int getEnd(Buffer buffer, int line);
	//}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[start=" + start
			+ ",end=" + end + ",startLine=" + startLine
			+ ",endLine=" + endLine + "]";
	} //}}}

	//{{{ Package-private members
	int start, end, startLine, endLine;
	//}}}

	//{{{ Protected members

	//{{{ Selection constructor
	protected Selection()
	{
	} //}}}

	//{{{ Selection constructor
	protected Selection(Selection copy)
	{
		start = copy.start;
		end = copy.end;
	} //}}}

	//{{{ Selection constructor
	protected Selection(int start, int end)
	{
		this.start = start;
		this.end = end;

		// setting these is handled by textArea._addToSelection();
		//this.startLine = startLine;
		//this.endLine = endLine;
	} //}}}

	//}}}

	//{{{ Range class
	/**
	 * An ordinary range selection.
	 * @since jEdit 3.2pre1
	 */
	public static class Range extends Selection
	{
		//{{{ Range constructor
		public Range()
		{
			super();
		} //}}}

		//{{{ Range constructor
		public Range(Selection sel)
		{
			super(sel);
		} //}}}

		//{{{ Range constructor
		public Range(int start, int end)
		{
			super(start,end);
		} //}}}

		//{{{ getStart() method
		public int getStart(Buffer buffer, int line)
		{
			if(line == startLine)
				return start;
			else
				return buffer.getLineStartOffset(line);
		} //}}}

		//{{{ getEnd() method
		public int getEnd(Buffer buffer, int line)
		{
			if(line == endLine)
				return end;
			else
				return buffer.getLineEndOffset(line) - 1;
		} //}}}
	} //}}}

	//{{{ Rect class
	/**
	 * A rectangular selection.
	 * @since jEdit 3.2pre1
	 */
	public static class Rect extends Selection
	{
		//{{{ Rect constructor
		public Rect()
		{
			super();
		} //}}}

		//{{{ Rect constructor
		public Rect(Selection sel)
		{
			super(sel);
		} //}}}

		//{{{ Rect constructor
		public Rect(int start, int end)
		{
			super(start,end);
		} //}}}

		//{{{ getStart() method
		public int getStart(Buffer buffer, int line)
		{
			if(line == startLine)
				return start;
			else
			{
				int _start = start - buffer.getLineStartOffset(startLine);
				int _end = end - buffer.getLineStartOffset(endLine);

				return Math.min(buffer.getLineEndOffset(line) - 1,
					buffer.getLineStartOffset(line)
					+ Math.min(_start,_end));
			}
		} //}}}

		//{{{ getEnd() method
		public int getEnd(Buffer buffer, int line)
		{
			if(line == endLine)
				return end;
			else
			{
				int _start = start - buffer.getLineStartOffset(startLine);
				int _end = end - buffer.getLineEndOffset(endLine);

				return Math.min(buffer.getLineEndOffset(line) - 1,
					buffer.getLineStartOffset(line)
					+ Math.max(_start,_end));
			}
		} //}}}
	} //}}}
}
