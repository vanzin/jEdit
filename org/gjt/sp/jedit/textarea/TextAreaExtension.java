/*
 * TextAreaExtension.java - Custom painter and tool tip handler
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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

import java.awt.Graphics2D;

/**
 * Subclasses of this class can perform custom painting and tool tip
 * handling in the text area and gutter.
 *
 * @see TextAreaPainter#addExtension(TextAreaExtension)
 * @see TextAreaPainter#removeExtension(TextAreaExtension)
 * @see Gutter#addExtension(TextAreaExtension)
 * @see Gutter#removeExtension(TextAreaExtension)
 *
 * @since jEdit 4.0pre4
 *
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class TextAreaExtension
{
	//{{{ paintScreenLineRange() method
	/**
	 * Paints a range of screen lines. The default implementation calls
	 * {@link #paintValidLine(Graphics2D,int,int,int,int,int)} and
	 * {@link #paintInvalidLine(Graphics2D,int,int)}.
	 * @param gfx A graphics context
	 * @param firstLine The first screen line
	 * @param lastLine The last screen line
	 * @param physicalLines The list of physical line numbers. Entries are
	 * -1 if the screen line is out of range.
	 * @param start An array of screen line start offsets.
	 * @param end An array of screen line end offsets
	 * @param y The y co-ordinate
	 * @param lineHeight The line height
	 * @since jEdit 4.2pre2
	 */
	public void paintScreenLineRange(Graphics2D gfx, int firstLine,
		int lastLine, int[] physicalLines, int[] start, int[] end,
		int y, int lineHeight)
	{
		for(int i = 0; i < physicalLines.length; i++)
		{
			int screenLine = i + firstLine;
			if(physicalLines[i] == -1)
				paintInvalidLine(gfx,screenLine,y);
			else
			{
				paintValidLine(gfx,screenLine,physicalLines[i],
					start[i],end[i],y);
			}

			y += lineHeight;
		}
	} //}}}

	//{{{ paintValidLine() method
	/**
	 * Called by the text area when the extension is to paint a
	 * screen line which has an associated physical line number in
	 * the buffer. Note that since one physical line may consist of
	 * several screen lines due to soft wrap, the start and end
	 * offsets of the screen line are passed in as well.
	 *
	 * @param gfx The graphics context
	 * @param screenLine The screen line number
	 * @param physicalLine The physical line number
	 * @param start The offset where the screen line begins, from
	 * the start of the buffer
	 * @param end The offset where the screen line ends, from the
	 * start of the buffer
	 * @param y The y co-ordinate of the top of the line's
	 * bounding box
	 * @since jEdit 4.0pre4
	 */
	public void paintValidLine(Graphics2D gfx, int screenLine,
		int physicalLine, int start, int end, int y) {} //}}}

	//{{{ paintInvalidLine() method
	/**
	 * Called by the text area when the extension is to paint a
	 * screen line which is not part of the buffer. This can happen
	 * if the buffer is shorter than the height of the text area,
	 * for example.
	 *
	 * @param gfx The graphics context
	 * @param screenLine The screen line number
	 * @param y The y co-ordinate of the top of the line's
	 * bounding box
	 * @since jEdit 4.0pre4
	 */
	public void paintInvalidLine(Graphics2D gfx, int screenLine,
		int y) {} //}}}

	//{{{ getToolTipText() method
	/**
	 * Called by the text area when the mouse hovers over the
	 * location specified in the mouse event.
	 *
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @since jEdit 4.0pre4
	 */
	public String getToolTipText(int x, int y)
	{
		return null;
	} //}}}
}
