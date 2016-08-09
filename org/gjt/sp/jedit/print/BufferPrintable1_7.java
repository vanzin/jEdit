/*
 * BufferPrintable.java - Printable implementation
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
 * Portions copyright (C) 2002 Thomas Dilts
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
import javax.swing.text.TabExpander;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;

import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * @version $Id: BufferPrintable.java 24442 2016-06-29 23:29:25Z daleanson $
 */
class BufferPrintable1_7 implements Printable
{
	//{{{ BufferPrintable1_7 constructor
	BufferPrintable1_7(PrintRequestAttributeSet format, View view, Buffer buffer, boolean selection)
	{
		this.format = format;
		this.view = view;
		this.buffer = buffer;
		this.selection = selection;
		firstCall = true;
		
		
		header = jEdit.getBooleanProperty("print.header");
		footer = jEdit.getBooleanProperty("print.footer");
		lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		font = jEdit.getFontProperty("print.font");
		boolean color = jEdit.getBooleanProperty("print.color");

		styles = org.gjt.sp.util.SyntaxUtilities.loadStyles(jEdit.getProperty("print.font"), jEdit.getIntegerProperty("print.fontsize", 10), color);
		styles[Token.NULL] = new SyntaxStyle(textColor, null, font);

		// assume the paper is white, so change any white text to black
		for(int i = 0; i < styles.length; i++)
		{
			SyntaxStyle s = styles[i];
			if(s.getForegroundColor().equals(Color.WHITE) && s.getBackgroundColor() == null)
			{
				styles[i] = new SyntaxStyle(Color.BLACK, styles[i].getBackgroundColor(), styles[i].getFont());
			}
		}

		lineList = new ArrayList<Chunk>();

		tokenHandler = new DisplayTokenHandler();
	} //}}}
	
	//{{{ print() method
	// this can be called multiple times by the print system for the same page
	public int print(Graphics _gfx, PageFormat pageFormat, int pageIndex) throws PrinterException
	{
		if (firstCall)
		{
			// we keep the first non-null frc we get, since sometimes
			// we get invalid ones on subsequent pages on Windows
			if(frc == null)
			{
				frc = ((Graphics2D)_gfx).getFontRenderContext();
				Log.log(Log.DEBUG, this, "Font render context is " + frc);
			}

			calculatePages(_gfx, pageFormat);
			firstCall = false;
		}
		
		Log.log(Log.DEBUG,this,"Asked to print page " + pageIndex);
		
		Range range = pages.get(pageIndex);
		if (range == null || !inRange(pageIndex))
		{
			return NO_SUCH_PAGE;	
		}
		else {
			printPage(_gfx, pageFormat, pageIndex, true);
		}

		return PAGE_EXISTS;
	} //}}}

	//{{{ Private members

	//{{{ Static variables
	private static Color headerColor = Color.lightGray;
	private static Color headerTextColor = Color.black;
	private static Color footerColor = Color.lightGray;
	private static Color footerTextColor = Color.black;
	private static Color lineNumberColor = Color.gray;
	private static Color textColor = Color.black;
	//}}}

	//{{{ Instance variables
	private PrintRequestAttributeSet format;
	private boolean firstCall;

	private View view;
	private Buffer buffer;
	private boolean selection;
	private Font font;
	private SyntaxStyle[] styles;
	private boolean header;
	private boolean footer;
	private boolean lineNumbers;

	private HashMap<Integer, Range> pages;
	private int currentPhysicalLine;

	private LineMetrics lm;
	private final List<Chunk> lineList;

	private FontRenderContext frc;

	private DisplayTokenHandler tokenHandler;
	//}}}
	
	// parses the file to determine what lines go on what page
	private void calculatePages(Graphics _gfx, PageFormat pageFormat) 
	{
		pages = new HashMap<Integer, Range>();
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setFont(font);

		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();

		if(header)
		{
			double headerHeight = paintHeader(gfx, pageX, pageY, pageWidth, false);
			pageY += headerHeight;
			pageHeight -= headerHeight;
		}

		if(footer)
		{
			double footerHeight = paintFooter(gfx, pageX, pageY, pageWidth, pageHeight, 0, false);
			pageHeight -= footerHeight;
		}

		double lineNumberWidth = 0.0;

		// determine line number width
		if(lineNumbers)
		{
			// the +1's ensure that 99 gets 3 digits, 103 gets 4 digits, and so on.
			int lineNumberDigits = (int)Math.ceil(Math.log(buffer.getLineCount() + 1) / Math.log(10)) + 1;

			// now that we know how many chars there are, get the width.
			char[] chars = new char[lineNumberDigits];
			for(int i = 0; i < chars.length; i++)
			{
				chars[i] = ' ';
			}
			lineNumberWidth = font.getStringBounds(chars, 0, lineNumberDigits, frc).getWidth();
		}
		PrintTabExpander tabExpander = new PrintTabExpander(4);
		try {
			// calculate tab size
			int tabSize = jEdit.getIntegerProperty("print.tabSize", 4);
			char[] chars = new char[tabSize];
			for(int i = 0; i < chars.length; i++)
			{
				chars[i] = ' ';
			}
			double tabWidth = font.getStringBounds(chars, 0, tabSize, frc).getWidth();
			tabExpander = new PrintTabExpander(tabWidth);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		lm = font.getLineMetrics("gGyYX", frc);
		boolean printFolds = jEdit.getBooleanProperty("print.folds", true);
		
		currentPhysicalLine = 0;
		int pageCount = 0;
		int startLine = 0;
		double y = 0.0;
		while (currentPhysicalLine <= buffer.getLineCount())
		{
			if (currentPhysicalLine == buffer.getLineCount())
			{
				// last page
				Range range = new Range(startLine, currentPhysicalLine);
				pages.put(new Integer(pageCount), range);
				break;
			}
			if (!printFolds && !view.getTextArea().getDisplayManager().isLineVisible(currentPhysicalLine))
			{
				currentPhysicalLine++;
				continue;
			}
				
			lineList.clear();

			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), -1);
			buffer.markTokens(currentPhysicalLine, tokenHandler);
			
			if(y + (lm.getHeight() * (lineList.isEmpty() ? 1 : lineList.size())) >= pageHeight)
			{
				Range range = new Range(startLine, currentPhysicalLine);
				pages.put(new Integer(pageCount), range);
				++ pageCount;
				++ currentPhysicalLine;
				startLine = currentPhysicalLine;
				y = 0.0;
				continue;
			}

			for (int i = 0; i < lineList.size(); i++)
			{
				y += lm.getHeight();
			}
			currentPhysicalLine++;
		}
	}

	//{{{ inRange() method
	private boolean inRange(int pageNumber)
	{
		PageRanges ranges = (PageRanges)format.get(PageRanges.class);
		boolean answer = false;
		if (ranges == null)
		{
			answer = true;
		}
		else 
		{
			answer = ranges.contains(pageNumber + 1);	
		}
		return answer;
	} //}}}
	

	//{{{ printPage() method
	private void printPage(Graphics _gfx, PageFormat pageFormat, int pageIndex, boolean actuallyPaint)
	{
		Log.log(Log.DEBUG, this, "printPage(" + pageIndex + ", " + actuallyPaint + ')');
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setFont(font);

		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();

		Log.log(Log.DEBUG,this,"#1 - Page dimensions: " + pageWidth + 'x' + pageHeight);

		if(header)
		{
			double headerHeight = paintHeader(gfx, pageX, pageY, pageWidth, actuallyPaint);
			pageY += headerHeight;
			pageHeight -= headerHeight;
		}

		if(footer)
		{
			double footerHeight = paintFooter(gfx, pageX, pageY, pageWidth, pageHeight, pageIndex, actuallyPaint);
			pageHeight -= footerHeight;
		}

		boolean glyphVector = jEdit.getBooleanProperty("print.glyphVector");
		double lineNumberWidth = 0.0;

		// determine line number width
		if(lineNumbers)
		{
			// the +1's ensure that 99 gets 3 digits, 103 gets 4 digits,
			// and so on.
			int lineNumberDigits = (int)Math.ceil(Math.log(buffer.getLineCount() + 1) / Math.log(10)) + 1;

			// now that we know how many chars there are, get the width.
			char[] chars = new char[lineNumberDigits];
			for(int i = 0; i < chars.length; i++)
			{
				chars[i] = ' ';
			}
			lineNumberWidth = font.getStringBounds(chars, 0, lineNumberDigits, frc).getWidth();
		}

		Log.log(Log.DEBUG,this,"#2 - Page dimensions: " + (pageWidth - lineNumberWidth) + 'x' + pageHeight);

		// calculate tab size
		int tabSize = jEdit.getIntegerProperty("print.tabSize", 4);
		char[] chars = new char[tabSize];
		for(int i = 0; i < chars.length; i++)
		{
			chars[i] = ' ';
		}
		double tabWidth = font.getStringBounds(chars, 0, tabSize, frc).getWidth();
		PrintTabExpander tabExpander = new PrintTabExpander(tabWidth);

		lm = font.getLineMetrics("gGyYX", frc);
		Log.log(Log.DEBUG,this,"Line height is " + lm.getHeight());

		double y = 0.0;
		Range range = pages.get(pageIndex);
		for (currentPhysicalLine = range.getStart(); currentPhysicalLine <= range.getEnd(); currentPhysicalLine++)
		{
			if(currentPhysicalLine == buffer.getLineCount())
			{
				Log.log(Log.DEBUG, this, "Finished buffer");
				Log.log(Log.DEBUG, this, "The end");
				break;
			}
			if (!jEdit.getBooleanProperty("print.folds",true) && !view.getTextArea().getDisplayManager().isLineVisible(currentPhysicalLine))
			{
				Log.log(Log.DEBUG,this,"Skipping invisible line");
				continue;
			}
				
			lineList.clear();

			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), -1);
			buffer.markTokens(currentPhysicalLine, tokenHandler);

			if(lineNumbers && actuallyPaint)
			{
				gfx.setFont(font);
				gfx.setColor(lineNumberColor);
				gfx.drawString(String.valueOf(currentPhysicalLine + 1), (float)pageX, (float)(pageY + y + lm.getHeight()));
			}

			for (Chunk chunk : lineList)
			{
				y += lm.getHeight();
				Chunk chunks = chunk;
				if (chunks != null && actuallyPaint)
				{
					// TODO: use lm.getHeight() instead of metrics.getHeight()?
					FontMetrics metrics = gfx.getFontMetrics();
					Chunk.paintChunkBackgrounds(chunks, gfx, (float) (pageX + lineNumberWidth), (float) (pageY + y), metrics.getHeight());
					Chunk.paintChunkList(chunks, gfx, (float) (pageX + lineNumberWidth), (float) (pageY + y), glyphVector);
				}
			}
			if (currentPhysicalLine == range.getEnd())
			{
				Log.log(Log.DEBUG,this,"Finished page");
				break;
			}
		}
	} //}}}

	//{{{ paintHeader() method
	private double paintHeader(Graphics2D gfx, double pageX, double pageY, double pageWidth, boolean actuallyPaint)
	{
		String headerText = jEdit.getProperty("print.headerText", new String[] { buffer.getName() });
		FontRenderContext frc = gfx.getFontRenderContext();
		lm = font.getLineMetrics(headerText,frc);

		Rectangle2D bounds = font.getStringBounds(headerText,frc);
		Rectangle2D headerBounds = new Rectangle2D.Double(pageX,pageY,pageWidth,bounds.getHeight());

		if(actuallyPaint)
		{
			gfx.setColor(headerColor);
			gfx.fill(headerBounds);
			gfx.setColor(headerTextColor);
			gfx.drawString(headerText, (float)(pageX + (pageWidth - bounds.getWidth()) / 2), (float)(pageY + lm.getAscent()));
		}

		return headerBounds.getHeight();
	}
	//}}}

	//{{{ paintFooter() method
	private double paintFooter(Graphics2D gfx, double pageX, double pageY, double pageWidth, double pageHeight, int pageIndex, boolean actuallyPaint)
	{
		String footerText = jEdit.getProperty("print.footerText", new Object[] { new Date(), Integer.valueOf(pageIndex + 1)});
		FontRenderContext frc = gfx.getFontRenderContext();
		lm = font.getLineMetrics(footerText, frc);

		Rectangle2D bounds = font.getStringBounds(footerText,frc);
		Rectangle2D footerBounds = new Rectangle2D.Double(pageX,pageY + pageHeight - bounds.getHeight(), pageWidth,bounds.getHeight());

		if(actuallyPaint)
		{
			gfx.setColor(footerColor);
			gfx.fill(footerBounds);
			gfx.setColor(footerTextColor);
			gfx.drawString(footerText, (float)(pageX + (pageWidth - bounds.getWidth()) / 2), (float)(pageY + pageHeight - bounds.getHeight() + lm.getAscent()));
		}

		return footerBounds.getHeight();
	} //}}}

	//}}}
	
	class Range
	{
		int start = 0;
		int end = 0;
		
		public Range(int start, int end)
		{
			this.start = start;
			this.end = end;
		}
		
		public int getStart()
		{
			return start;	
		}
		
		public int getEnd()
		{
			return end;	
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Range[").append(start).append(", ").append(end).append(']');
			return sb.toString();
		}
	}

	//{{{ PrintTabExpander class
	static class PrintTabExpander implements TabExpander
	{
		private double tabWidth;

		//{{{ PrintTabExpander constructor
		PrintTabExpander(double tabWidth)
		{
			this.tabWidth = tabWidth;
		} //}}}

		//{{{ nextTabStop() method
		public float nextTabStop(float x, int tabOffset)
		{
			int ntabs = (int)((x + 1) / tabWidth);
			return (float)((ntabs + 1) * tabWidth);
		} //}}}
	} //}}}
}
