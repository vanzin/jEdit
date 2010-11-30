/*
 * ExplicitFoldHandler.java - Explicit fold handler
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
 * A fold handler that folds lines based on markers ("{{{" and "}}}")
 * embedded in the text.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class ExplicitFoldHandler extends FoldHandler
{
	//{{{ ExplicitFoldHandler constructor
	public ExplicitFoldHandler()
	{
		super("explicit");
	} //}}}

	//{{{ getFoldLevel() method
	/**
	 * Returns the fold level of the specified line.
	 * @param buffer The buffer in question
	 * @param lineIndex The line index
	 * @param seg A segment the fold handler can use to obtain any
	 * text from the buffer, if necessary
	 * @return The fold level of the specified line
	 * @since jEdit 4.0pre1
	 */
	public int getFoldLevel(JEditBuffer buffer, int lineIndex, Segment seg)
	{
		if(lineIndex == 0)
			return 0;
		else
		{
			int foldLevel = buffer.getFoldLevel(lineIndex - 1);

			buffer.getLineText(lineIndex - 1,seg);

			int offset = seg.offset;
			int count = seg.count;

			int openingBrackets = 0, closingBrackets = 0;
			for(int i = 0; i < count; i++)
			{
				switch(seg.array[offset + i])
				{
				case '{':
					closingBrackets = 0;
					openingBrackets++;
					if(openingBrackets == 3)
					{
						foldLevel++;
						openingBrackets = 0;
					}
					break;
				case '}':
					openingBrackets = 0;
					closingBrackets++;
					if(closingBrackets == 3)
					{
						if(foldLevel > 0)
							foldLevel--;
						closingBrackets = 0;
					}
					break;
				default:
					closingBrackets = openingBrackets = 0;
					break;
				}
			}

			return foldLevel;
		}
	} //}}}
}
