/*
 * Selection.java - Selected text
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

package org.gjt.sp.jedit.textarea;

import javax.swing.text.Segment;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;

/**
 * An interface representing a portion of the current selection.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 3.2pre1
 */
public abstract class Selection implements Cloneable
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

	//{{{ getStart() method
	/**
	 * Returns the start offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 * @since jEdit 4.1pre1
	 */
	public abstract int getStart(Buffer buffer, int line);
	//}}}

	//{{{ getEnd() method
	/**
	 * Returns the end offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 * @since jEdit 4.1pre1
	 */
	public abstract int getEnd(Buffer buffer, int line);
	//}}}

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

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[start=" + start
			+ ",end=" + end + ",startLine=" + startLine
			+ ",endLine=" + endLine + "]";
	} //}}}

	//{{{ clone() method
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError("I just drank a whole "
				+ "bottle of cough syrup and I feel "
				+ "funny!");
		}
	} //}}}

	//{{{ Package-private members
	int start, end;
	int startLine, endLine;

	//{{{ Selection constructor
	Selection()
	{
	} //}}}

	//{{{ Range constructor
	Selection(Selection sel)
	{
		this.start = sel.start;
		this.end = sel.end;
	} //}}}

	//{{{ Selection constructor
	Selection(int start, int end)
	{
		this.start = start;
		this.end = end;
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
	// this class is not very fast...
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

		//{{{ Rect constructor
		/**
		 * Special constructor for "Vertical Paste" command.
		 */
		public Rect(int start, int end, int startLine, int endLine)
		{
			super(start,end);
			this.startLine = startLine;
			this.endLine = endLine;
		} //}}}

		//{{{ getStartColumn() method
		public int getStartColumn(Buffer buffer)
		{
			int virtColStart = buffer.getVirtualWidth(startLine,
				start - buffer.getLineStartOffset(startLine));
			int virtColEnd = buffer.getVirtualWidth(endLine,
				end - buffer.getLineStartOffset(endLine));
			return Math.min(virtColStart,virtColEnd) + extraStartVirt;
		} //}}}

		//{{{ getEndColumn() method
		public int getEndColumn(Buffer buffer)
		{
			int virtColStart = buffer.getVirtualWidth(startLine,
				start - buffer.getLineStartOffset(startLine));
			int virtColEnd = buffer.getVirtualWidth(endLine,
				end - buffer.getLineStartOffset(endLine));
			return Math.max(virtColStart,virtColEnd) + extraEndVirt;
		} //}}}

		//{{{ getStart() method
		public int getStart(Buffer buffer, int line)
		{
			return getColumnOnOtherLine(buffer,startLine,line,
				getStartColumn(buffer));
		} //}}}

		//{{{ getEnd() method
		public int getEnd(Buffer buffer, int line)
		{
			return getColumnOnOtherLine(buffer,startLine,line,
				getEndColumn(buffer));
		} //}}}

		//{{{ Package-private members
		int extraStartVirt;
		int extraEndVirt;

		//{{{ Private members

		//{{{ getColumnOnOtherLine() method
		private int getColumnOnOtherLine(Buffer buffer, int line1,
			int line2, int col)
		{
			int returnValue = buffer.getOffsetOfVirtualColumn(
				line2,col,null);
			if(returnValue == -1)
				return buffer.getLineEndOffset(line2) - 1;
			else
				return buffer.getLineStartOffset(line2) + returnValue;
		} //}}}

		//}}}
	} //}}}
}
