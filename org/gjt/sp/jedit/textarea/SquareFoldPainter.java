package org.gjt.sp.jedit.textarea;

import java.awt.Graphics2D;

import org.gjt.sp.jedit.buffer.JEditBuffer;

public class SquareFoldPainter implements FoldPainter {

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
	public void paintFoldStart(Gutter gutter, Graphics2D gfx, int screenLine,
			int bufferLine, boolean nextLineVisible, int y, int lineHeight,
			JEditBuffer buffer)
	{
		int _y = y + lineHeight / 2;
		gfx.setColor(gutter.getFoldColor());
		gfx.drawRect(1,_y-5,9,10);
		gfx.drawLine(3,_y,8,_y);
		if (nextLineVisible)
			gfx.drawLine(5,_y+5,5,_y+lineHeight-1);
		else
			gfx.drawLine(5,_y-3,5,_y+3);
	}

	@Override
	public void paintFoldMiddle(Gutter gutter, Graphics2D gfx, int screenLine,
			int bufferLine, int y, int lineHeight, JEditBuffer buffer)
	{
		gfx.drawLine(5,y,5,y+lineHeight-1);
	}

}
