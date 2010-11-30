/*
 * FoldPainter.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Shlomy Reinstein
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

import org.gjt.sp.jedit.buffer.JEditBuffer;

/**
 * FoldPainter defines the interface for fold painters in the gutter.
 *
 * @since jEdit 4.3pre16
 * @author Shlomy Reinstein
 * @version $Id$
 */
public interface FoldPainter
{
	/**
	 * Paints the beginning of a fold in the gutter.
	 * @param gutter The gutter in which the fold is drawn.
	 * @param gfx The graphics object to use for the painting.
	 * @param screenLine The index of the line on the screen (e.g. 5th from top).
	 * @param physicalLine The index of the line in the buffer.
	 * @param nextLineVisible Whether the next buffer line is visible on screen.
	 * @param y The y coordinate of the top of the line on the screen.
	 * @param lineHeight The line height in pixels.
	 * @param buffer The buffer to which the line belongs.
	 */
	void paintFoldStart(Gutter gutter, Graphics2D gfx, int screenLine,
		int physicalLine, boolean nextLineVisible, int y, int lineHeight,
		JEditBuffer buffer);
	
	/**
	 * Paints the end of a fold in the gutter.
	 * @param gutter The gutter in which the fold is drawn.
	 * @param gfx The graphics object to use for the painting.
	 * @param screenLine The index of the line on the screen (e.g. 5th from top).
	 * @param physicalLine The index of the line in the buffer.
	 * @param y The y coordinate of the top of the line on the screen.
	 * @param lineHeight The line height in pixels.
	 * @param buffer The buffer to which the line belongs.
	 */
	void paintFoldEnd(Gutter gutter, Graphics2D gfx, int screenLine,
		int physicalLine, int y, int lineHeight, JEditBuffer buffer);
	
	/**
	 * Paints the middle of a fold (single line) in the gutter.
	 * @param gutter The gutter in which the fold is drawn.
	 * @param gfx The graphics object to use for the painting.
	 * @param screenLine The index of the line on the screen (e.g. 5th from top).
	 * @param physicalLine The index of the line in the buffer.
	 * @param y The y coordinate of the top of the line on the screen.
	 * @param lineHeight The line height in pixels.
	 * @param buffer The buffer to which the line belongs.
	 */
	void paintFoldMiddle(Gutter gutter, Graphics2D gfx, int screenLine,
		int physicalLine, int y, int lineHeight, JEditBuffer buffer);
}
