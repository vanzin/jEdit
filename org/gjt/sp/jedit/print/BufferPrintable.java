/*
 * BufferPrintable.java - Printable implementation
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

package org.gjt.sp.jedit.print;

//{{{ Imports
import javax.swing.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.awt.*;
import java.util.Date;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
//}}}

class BufferPrintable implements Printable
{
	//{{{ BufferPrintable constructor
	BufferPrintable(Buffer buffer, Font font, boolean header, boolean footer,
		boolean lineNumbers, boolean color)
	{
		this.buffer = buffer;
		this.font = font;
		this.header = header;
		this.footer = footer;
		this.lineNumbers = lineNumbers;

		styles = GUIUtilities.loadStyles(jEdit.getProperty("print.font"),
			jEdit.getIntegerProperty("print.fontsize",10),color);
	} //}}}

	//{{{ print() method
	public int print(Graphics _gfx, PageFormat pageFormat, int pageIndex)
		throws PrinterException
	{
		if(end)
			return NO_SUCH_PAGE;

		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();

		Graphics2D gfx = (Graphics2D)_gfx;

		gfx.setFont(font);

		if(header)
		{
			double headerHeight = paintHeader(gfx,pageX,pageY,pageWidth);
			pageY += headerHeight * 2;
			pageHeight -= headerHeight * 2;
		}

		if(footer)
		{
			double footerHeight = paintFooter(gfx,pageX,pageY,pageWidth,
				pageHeight,pageIndex);
			pageHeight -= footerHeight * 2;
		}

		FontRenderContext frc = gfx.getFontRenderContext();

		// as anyone with any maths knowledge knows, this computes log_2(x)
		int lineNumberDigits = (int)(Math.log(buffer.getLineCount())
			/ Math.log(2) + 1);

		// now that we know how many chars there are, get the width.
		char[] chars = new char[lineNumberDigits];
		for(int i = 0; i < chars.length; i++)
			chars[i] = ' ';
		double lineNumberWidth = font.getStringBounds(chars,
			0,lineNumberDigits,frc).getWidth();

		Segment seg = new Segment();
		double y = 0.0;

print_loop:	for(;;)
		{
			buffer.getLineText(currentLine,seg);
			LineMetrics lm = font.getLineMetrics(seg.array,seg.offset,seg.count,
				frc);
			Token tokens = buffer.markTokens(currentLine).getFirstToken();

			y += lm.getHeight();
			if(y >= pageHeight)
				break print_loop;

			if(lineNumbers)
			{
				gfx.setColor(lineNumberColor);
				String lineNumberString = String.valueOf(currentLine + 1);
				gfx.drawString(lineNumberString,
					(float)(pageX + lineNumberWidth - font.getStringBounds(
					lineNumberString,frc).getWidth()),(float)(pageY + y));
			}

			currentLine++;
			if(currentLine == buffer.getLineCount())
			{
				end = true;
				break print_loop;
			}
		}

		return PAGE_EXISTS;
	} //}}}

	//{{{ Private members

	private static Color headerColor = Color.lightGray;
	private static Color headerTextColor = Color.black;
	private static Color footerColor = Color.lightGray;
	private static Color footerTextColor = Color.black;
	private static Color lineNumberColor = Color.black;
	private static Color textColor = Color.black;

	private static int lineNumberPadding = 10;

	//{{{ Instance variables
	private Buffer buffer;
	private Font font;
	private SyntaxStyle[] styles;
	private boolean header;
	private boolean footer;
	private boolean lineNumbers;

	private int currentLine;
	private boolean end;
	//}}}

	//{{{ paintHeader() method
	private double paintHeader(Graphics2D gfx, double pageX, double pageY,
		double pageWidth)
	{
		String headerText = jEdit.getProperty("print.headerText",
			new String[] { buffer.getPath() });
		FontRenderContext frc = gfx.getFontRenderContext();

		gfx.setColor(headerColor);

		Rectangle2D bounds = font.getStringBounds(headerText,frc);
		Rectangle2D headerBounds = new Rectangle2D.Double(
			pageX,pageY,pageWidth,bounds.getHeight());
		gfx.fill(headerBounds);

		gfx.setColor(headerTextColor);

		LineMetrics lm = font.getLineMetrics(headerText,frc);
		gfx.drawString(headerText,
			(float)(pageX + (pageWidth - bounds.getWidth()) / 2),
			(float)(pageY + lm.getAscent()));

		return headerBounds.getHeight();
	}
	//}}}

	//{{{ paintFooter() method
	private double paintFooter(Graphics2D gfx, double pageX, double pageY,
		double pageWidth, double pageHeight, int pageIndex)
	{
		String footerText = jEdit.getProperty("print.footerText",
			new Object[] { new Date(), new Integer(pageIndex + 1) });
		FontRenderContext frc = gfx.getFontRenderContext();

		gfx.setColor(footerColor);

		Rectangle2D bounds = font.getStringBounds(footerText,frc);
		Rectangle2D footerBounds = new Rectangle2D.Double(
			pageX,pageY + pageHeight - bounds.getHeight(),
			pageWidth,bounds.getHeight());
		gfx.fill(footerBounds);

		gfx.setColor(footerTextColor);

		LineMetrics lm = font.getLineMetrics(footerText,frc);
		gfx.drawString(footerText,
			(float)(pageX + (pageWidth - bounds.getWidth()) / 2),
			(float)(pageY + pageHeight - bounds.getHeight()
			+ lm.getAscent()));

		return footerBounds.getHeight();
	}
	//}}}
}
