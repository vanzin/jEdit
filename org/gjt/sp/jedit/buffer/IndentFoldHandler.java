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
			int offset = seg.offset;
			int count = seg.count;
			int whitespace = 0;
			boolean seenNonWhiteSpace = false;

loop:		for(int i = 0; i < count; i++)
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
					seenNonWhiteSpace = true;
					break loop;
				}
			}

			if(seenNonWhiteSpace)
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
}
