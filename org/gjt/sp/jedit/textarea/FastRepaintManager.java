/*
 * FastRepaintManager.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov
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

//{{{ Imports
import java.awt.*;
//}}}

/**
 * Manages blitting the offscreen graphics context to speed up scrolling.
 * The text area does not use Swing's built-in double buffering, so that
 * we have access to the graphics context for fast scrolling.
 * @author Slava Pestov
 * @version $Id$
 */
class FastRepaintManager
{
	//{{{ FastRepaintManager constructor
	FastRepaintManager(TextArea textArea,
		TextAreaPainter painter)
	{
		this.textArea = textArea;
		this.painter = painter;
	} //}}}

	//{{{ updateGraphics() method
	/**
	 * Prepare the graphics environment.
	 * This is only called by the TextAreaPainter when it is resized
	 */
	void updateGraphics()
	{
		if(gfx != null)
			gfx.dispose();

		int width = painter.getWidth();
		int height = painter.getHeight();
		/* A little hack */
		if(width <= 0)
			width = 1;
		if(height <= 0)
			height = 1;
		img = painter.getGraphicsConfiguration()
			.createCompatibleImage(width,height,
			Transparency.OPAQUE);
		gfx = (Graphics2D)img.getGraphics();
		gfx.clipRect(0,0,painter.getWidth(),painter.getHeight());
		fastScroll = false;
	} //}}}

	//{{{ getGraphics() method
	Graphics2D getGraphics()
	{
		return gfx;
	} //}}}

	//{{{ RepaintLines class
	static class RepaintLines
	{
		final int first;
		final int last;

		RepaintLines(int first, int last)
		{
			this.first = first;
			this.last = last;
		}
		
		@Override
		public String toString()
		{
			return "RepaintLines[" + first + ',' + last + ']';
		}
	} //}}}

	//{{{ prepareGraphics() method
	RepaintLines prepareGraphics(Rectangle clipRect, int firstLine,
		Graphics2D gfx)
	{
		gfx.setFont(painter.getFont());
		gfx.setColor(painter.getBackground());

		int lineHeight = gfx.getFontMetrics().getHeight();

		if(fastScroll)
		{
			int lineDelta = this.firstLine - firstLine;
			int visibleLines = textArea.getVisibleLines();

			if(lineDelta > -visibleLines
				&& lineDelta < visibleLines)
			{
				int yDelta = lineDelta * lineHeight;
				if(lineDelta < 0)
				{
					gfx.copyArea(0,-yDelta,painter.getWidth(),
						painter.getHeight() + yDelta,0,yDelta);
					return new RepaintLines(
						visibleLines + this.firstLine
						- firstLine - 1,
						visibleLines - 1);
				}
				else if(lineDelta > 0)
				{
					gfx.copyArea(0,0,painter.getWidth(),
						painter.getHeight() - yDelta,0,yDelta);
					return new RepaintLines(0,
						this.firstLine - firstLine);
				}
			}
		}

		// Because the clipRect's height is usually an even multiple
		// of the font height, we subtract 1 from it, otherwise one
		// too many lines will always be painted.
		return new RepaintLines(
			clipRect.y / lineHeight,
			(clipRect.y + clipRect.height - 1) / lineHeight);
	} //}}}

	//{{{ paint() method
	void paint(Graphics g)
	{
		firstLine = textArea.getFirstLine();
		g.drawImage(img,0,0,null);
	} //}}}

	//{{{ setFastScroll() method
	void setFastScroll(boolean fastScroll)
	{
		this.fastScroll = fastScroll;
	} //}}}

	//{{{ Private members
	private final TextArea textArea;
	private final TextAreaPainter painter;
	private Graphics2D gfx;
	private Image img;
	private boolean fastScroll;
	/* Most recently rendered first line */
	private int firstLine;
	//}}}
}
