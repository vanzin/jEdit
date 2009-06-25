/*
 * ShapedFoldPainter.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=0:
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

// {{{ ShapedFoldPainter class
/**
 * Fold Painter
 */
public abstract class ShapedFoldPainter implements FoldPainter {

	// {{{ paintFoldEnd()
	public void paintFoldEnd(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.setColor(gutter.getFoldColor());
		int _y = y + lineHeight / 2;
		int _x = 5;
		gfx.drawLine(_x,y,_x,_y+3);
		gfx.drawLine(_x,_y+3,_x+4,_y+3);
		boolean nested = (physicalLine < buffer.getLineCount() - 1 &&
			buffer.getFoldLevel(physicalLine + 1) > 0);
		if (nested)
			gfx.drawLine(_x,y+4,_x,y+lineHeight-1);
	}// }}}

	// {{{ paintFoldMiddle()
	public void paintFoldMiddle(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.setColor(gutter.getFoldColor());
		gfx.drawLine(5,y,5,y+lineHeight-1);
	}// }}}

	// {{{ paintFoldStart()
	public void paintFoldStart(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, boolean nextLineVisible, int y, int lineHeight,
			JEditBuffer buffer)
	{
		int _y = y + lineHeight / 2;
		int _x = 5;
		gfx.setColor(gutter.getFoldColor());
		paintFoldShape(gfx, _y - 4, _y + 4);
		gfx.drawLine(_x-2,_y,_x+2,_y);
		boolean nested = (buffer.getFoldLevel(physicalLine) > 0);
		if (nested)
			gfx.drawLine(_x,y,_x,_y-5);
		if (nextLineVisible)
			gfx.drawLine(_x,_y+5,_x,y+lineHeight-1);
		else
		{
			gfx.drawLine(_x,_y-2,_x,_y+2);
			if (nested)
				gfx.drawLine(_x,_y+4,_x,y+lineHeight-1);
		}
	}// }}}

	abstract protected void paintFoldShape(Graphics2D gfx, int top, int bottom);

} // }}}
