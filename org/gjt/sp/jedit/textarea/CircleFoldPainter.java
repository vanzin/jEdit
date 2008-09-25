package org.gjt.sp.jedit.textarea;

import java.awt.Graphics2D;

import org.gjt.sp.jedit.buffer.JEditBuffer;

public class CircleFoldPainter implements FoldPainter {

	@Override
	public void paintFoldEnd(Gutter gutter, Graphics2D gfx, int screenLine,
			int bufferLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.setColor(gutter.getFoldColor());
		int _y = y + lineHeight / 2;
		gfx.drawLine(5,y,5,_y + 3);
		gfx.drawLine(5,_y + 3,9,_y + 3);
	}

	@Override
	public void paintFoldMiddle(Gutter gutter, Graphics2D gfx, int screenLine,
			int bufferLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.setColor(gutter.getFoldColor());
		gfx.drawLine(5,y,5,y+lineHeight-1);
	}

	@Override
	public void paintFoldStart(Gutter gutter, Graphics2D gfx, int screenLine,
			int bufferLine, boolean nextLineVisible, int y, int lineHeight,
			JEditBuffer buffer)
	{
		int _y = y + lineHeight / 2;
		int _x = 5;
		gfx.setColor(gutter.getFoldColor());
		gfx.drawArc(_x-4,_y-4,8,8,0,360);
		gfx.drawLine(_x-2,_y,_x+2,_y);
		if (nextLineVisible)
			gfx.drawLine(_x,_y+5,_x,y+lineHeight-1);
		else
			gfx.drawLine(_x,_y-2,_x,_y+2);
	}

}
