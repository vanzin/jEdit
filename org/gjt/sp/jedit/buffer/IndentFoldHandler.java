/*
 * IndentFoldHandler.java - Indent-based fold handler
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Segment;

/**
 * A fold handler that folds lines based on their indent level.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class IndentFoldHandler extends FoldHandler
{
	public IndentFoldHandler()
	{
		super("indent");
	}

	// Returns the width of leading whitespace in the given segment
	// if it contains non-whitespace characters, or (-1) otherwise.
	private int getLeadingWhitespaceWidth(Segment seg, int tabSize)
	{
		int offset = seg.offset;
		int count = seg.count;
		int whitespace = 0;

		for(int i = 0; i < count; i++)
		{
			switch(seg.array[offset + i])
			{
			case ' ':
				whitespace++;
				break;
			case '\t':
				whitespace += (tabSize - whitespace % tabSize);
				break;
			default:
				return whitespace;
			}
		}
		return (-1);
	}

	//{{{ getFoldLevel() method
	/**
	 * Returns the fold level of the specified line. For a whitespace-only
	 * line, returns the fold level of the next non-whitespace line, or
	 * the level of the previous line if no non-whitespace line follows or if
	 * the level of the previous line is higher.
	 * @param buffer The buffer in question
	 * @param lineIndex The line index
	 * @param seg A segment the fold handler can use to obtain any
	 * text from the buffer, if necessary
	 * @return The fold level of the specified line
	 * @since jEdit 4.0pre1
	 */
	public int getFoldLevel(JEditBuffer buffer, int lineIndex, Segment seg)
	{
		int tabSize = buffer.getTabSize();
		// Look for first non-whitespace line starting at lineIndex
		int prevLevel = 0;
		for (int index = lineIndex; index < buffer.getLineCount(); index++)
		{
			buffer.getLineText(index,seg);
			int whitespace = getLeadingWhitespaceWidth(seg,tabSize);
			if(whitespace >= 0)	// Non-whitespace found on line
				return (whitespace > prevLevel) ? whitespace : prevLevel;
			if(index == 0)
				return 0;
			if(index == lineIndex)
				prevLevel = buffer.getFoldLevel(lineIndex - 1);
		}
		// All lines from lineIndex are whitespace-only - use fold
		// level of previous line.
		return prevLevel;
	} //}}}

	//{{{ getPrecedingFoldLevels() method
	/**
	 * Returns the fold levels of the lines preceding the specified line,
	 * which depend on the specified line.
	 * @param buffer The buffer in question
	 * @param lineIndex The line index
	 * @param seg A segment the fold handler can use to obtain any
	 * @param lineFoldLevel The fold level of the specified line
	 * @return The fold levels of the preceding lines, in decreasing line
	 * number order (i.e. bottomost line first).
	 * @since jEdit 4.3pre18
	 */
	public List<Integer> getPrecedingFoldLevels(JEditBuffer buffer,
		int lineIndex, Segment seg, int lineFoldLevel)
	{
		List<Integer> precedingFoldLevels = new ArrayList<Integer>();
		int tabSize = buffer.getTabSize();
		int whitespace = 0;
		int index;
		// Find previous non-whitespace-only line
		for (index = lineIndex - 1; index > 0; index--)
		{
			buffer.getLineText(index,seg);
			whitespace = getLeadingWhitespaceWidth(seg,tabSize);
			if (whitespace >= 0)
				break;
		}
		int max = (lineFoldLevel > whitespace) ? lineFoldLevel : whitespace;
		for (index++; index < lineIndex; index++)
			precedingFoldLevels.add(Integer.valueOf(max));
		return precedingFoldLevels;
	}
	//}}}

}
