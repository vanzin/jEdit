/*
 * Selection.java - Selected text
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

import javax.swing.text.*;
import org.gjt.sp.jedit.Buffer;

/**
 * An interface representing a portion of the current selection.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 3.2pre1
 */
public abstract class Selection
{
	/**
	 * Returns the start offset of this selection.
	 */
	public int getStart()
	{
		return start;
	}

	/**
	 * Returns the end offset of this selection.
	 */
	public int getEnd()
	{
		return end;
	}

	/**
	 * Returns the starting line number of this selection.
	 */
	public int getStartLine()
	{
		return startLine;
	}

	/**
	 * Returns the ending line number of this selection.
	 */
	public int getEndLine()
	{
		return endLine;
	}

	/**
	 * Returns the start offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 */
	public abstract int getStart(Buffer buffer, int line);

	/**
	 * Returns the end offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 */
	public abstract int getEnd(Buffer buffer, int line);

	public String toString()
	{
		return getClass().getName() + "[start=" + start
			+ ",end=" + end + ",startLine=" + startLine
			+ ",endLine=" + endLine + "]";
	}

	// package-private members
	int start, end, startLine, endLine;

	// protected members
	protected Selection()
	{
	}

	protected Selection(Selection copy)
	{
		start = copy.start;
		end = copy.end;
	}

	protected Selection(int start, int end)
	{
		this.start = start;
		this.end = end;

		// setting these is handled by textArea._addToSelection();
		//this.startLine = startLine;
		//this.endLine = endLine;
	}

	/**
	 * An ordinary range selection.
	 * @since jEdit 3.2pre1
	 */
	public static class Range extends Selection
	{
		public Range()
		{
			super();
		}

		public Range(Selection sel)
		{
			super(sel);
		}

		public Range(int start, int end)
		{
			super(start,end);
		}

		public int getStart(Buffer buffer, int line)
		{
			if(line == startLine)
				return start;
			else
			{
				Element map = buffer.getDefaultRootElement();
				Element lineElement = map.getElement(line);
				return lineElement.getStartOffset();
			}
		}

		public int getEnd(Buffer buffer, int line)
		{
			if(line == endLine)
				return end;
			else
			{
				Element map = buffer.getDefaultRootElement();
				Element lineElement = map.getElement(line);
				return lineElement.getEndOffset() - 1;
			}
		}
	}

	/**
	 * A rectangular selection.
	 * @since jEdit 3.2pre1
	 */
	public static class Rect extends Selection
	{
		public Rect()
		{
			super();
		}

		public Rect(Selection sel)
		{
			super(sel);
		}

		public Rect(int start, int end)
		{
			super(start,end);
		}

		public int getStart(Buffer buffer, int line)
		{
			if(line == startLine)
				return start;
			else
			{
				Element map = buffer.getDefaultRootElement();
				int _start = start - map.getElement(startLine)
					.getStartOffset();
				int _end = end - map.getElement(endLine)
					.getStartOffset();
	
				Element lineElement = map.getElement(line);
				return Math.min(lineElement.getEndOffset() - 1,
					lineElement.getStartOffset()
					+ Math.min(_start,_end));
			}
		}

		public int getEnd(Buffer buffer, int line)
		{
			if(line == endLine)
				return end;
			else
			{
				Element map = buffer.getDefaultRootElement();
				int _start = start - map.getElement(startLine)
					.getStartOffset();
				int _end = end - map.getElement(endLine)
					.getStartOffset();
	
				Element lineElement = map.getElement(line);
				return Math.min(lineElement.getEndOffset() - 1,
					lineElement.getStartOffset()
					+ Math.max(_start,_end));
			}
		}
	}
}
