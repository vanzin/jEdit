/*
 * TriangleFoldPainter.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=0:
 *
 * Copyright (C) 1999, 2000 mike dillon
 * Portions copyright (C) 2001, 2002 Slava Pestov
 * Refactoring copyright (C) 2008 Shlomy Reinstein
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

// {{{ class TriangleFoldHandler
public class TriangleFoldPainter implements FoldPainter
{
	// {{{ paintFoldStart()
	public void paintFoldStart(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, boolean nextLineVisible, int y, int lineHeight,
			JEditBuffer buffer)
	{
		int _y = y + lineHeight / 2;
		gfx.setColor(gutter.getFoldColor());
		if (nextLineVisible)
		{
			gfx.drawLine(1,_y - 3,10,_y - 3);
			gfx.drawLine(2,_y - 2,9,_y - 2);
			gfx.drawLine(3,_y - 1,8,_y - 1);
			gfx.drawLine(4,_y,7,_y);
			gfx.drawLine(5,_y + 1,6,_y + 1);
		}
		else
		{
			gfx.drawLine(4,_y - 5,4,_y + 4);
			gfx.drawLine(5,_y - 4,5,_y + 3);
			gfx.drawLine(6,_y - 3,6,_y + 2);
			gfx.drawLine(7,_y - 2,7,_y + 1);
			gfx.drawLine(8,_y - 1,8,_y);
		}
	} // }}}

	// {{{ paintFoldEnd()
	public void paintFoldEnd(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.setColor(gutter.getFoldColor());
		int _y = y + lineHeight / 2;
		gfx.drawLine(4,_y,4,_y + 3);
		gfx.drawLine(4,_y + 3,7,_y + 3);
	} // }}}

	// {{{ paintFoldMiddle()
	public void paintFoldMiddle(Gutter gutter, Graphics2D gfx, int screenLine,
			int physicalLine, int y, int lineHeight, JEditBuffer buffer)
	{
	} // }}}

}// }}}
