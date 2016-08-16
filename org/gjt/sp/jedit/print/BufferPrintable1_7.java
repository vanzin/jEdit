/*
 * BufferPrintable.java - Printable implementation
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2016 Dale Anson
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
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;

import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;


/**
 * A new buffer printable that does a lot more than the old one, like properly
 * printing ranges of pages, reverse page printing, printing just a selection,
 * and so on.
 * @version $Id: BufferPrintable.java 24442 2016-06-29 23:29:25Z daleanson $
 */
class BufferPrintable1_7 implements Printable
{
	private static Color headerColor = Color.lightGray;
	private static Color headerTextColor = Color.black;
	private static Color footerColor = Color.lightGray;
	private static Color footerTextColor = Color.black;
	private static Color lineNumberColor = Color.gray;
	private static Color textColor = Color.black;

	private PrintRequestAttributeSet format;
	private boolean firstCall;

	private View view;
	private Buffer buffer;
	private boolean selection;
	private int[] selectedLines;
	private boolean reverse;
	private int printRangeType = PrinterDialog.ALL;
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
	
	BufferPrintable1_7(PrintRequestAttributeSet format, View view, Buffer buffer)
	{
		this.format = format;
		this.view = view;
		this.buffer = buffer;
		firstCall = true;		// pages and page ranges are calculated only once
		
		header = jEdit.getBooleanProperty("print.header");
		footer = jEdit.getBooleanProperty("print.footer");
		lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		font = jEdit.getFontProperty("print.font");
		boolean color = Chromaticity.COLOR.equals(format.get(Chromaticity.class));

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
	}
	
	public void setFont(Font font)
	{
		if (font != null)
		{
			this.font = font;
		}
	}
	
	/**
 	 * Set the line numbers that are selected in the text area.	
 	 * @param lines An array of lines that are selected in the text area.	
 	 */
	public void setSelectedLines(int[] lines)
	{
		selectedLines = Arrays.copyOf(lines, lines.length);	
		Arrays.sort(selectedLines);
	}
	
	/**
 	 * Set to <code>true</code> to print the pages in reverse order, that is, print
 	 * the last page first and the first page last.
 	 * @param b Whether to print in reverse or not.
 	 */
	public void setReverse(boolean b)
	{
		reverse = b;	
	}
	
	/**
 	 * Set the print range type.
 	 * @param printRangeType One of PrinterDialog.ALL, RANGE, CURRENT_PAGE, or SELECTION.
 	 */
	public void setPrintRangeType(int printRangeType)
	{
		this.printRangeType = printRangeType;
		selection = PrinterDialog.SELECTION == printRangeType;
	}
	
	// this can be called multiple times by the print system for the same page, and
	// all calls must be handled for the page to print properly. 
	public int print(Graphics _gfx, PageFormat pageFormat, int pageIndex) throws PrinterException
	{
		if (firstCall)
		{
			calculatePages(_gfx, pageFormat);
			firstCall = false;
		}
		
		// figure out the current page if that is what is requested. I'm using
		// the page that contains the caret as the current page.
		// QUESTION: use the text area first physical line instead?
		if (printRangeType == PrinterDialog.CURRENT_PAGE)
		{
			int caretLine = view.getTextArea().getCaretLine();
			for (Integer i : pages.keySet())
			{
				Range range = pages.get(i);
				if (range.contains(caretLine))
				{
					pageIndex = i;
					break;
				}
			}
		}
		
		//Log.log(Log.DEBUG, this, "Asked to print page " + pageIndex);
		
		// adjust the page index for reverse printing
		if (reverse && printRangeType != PrinterDialog.CURRENT_PAGE)
		{
			pageIndex = pages.size() - 1 - pageIndex;
			//Log.log(Log.DEBUG, this, "Reverse is on, changing page index to " + pageIndex);
		}
		
		// go ahead and print the page
		Range range = pages.get(pageIndex);
		if ( (range == null || !inRange(pageIndex)) && printRangeType != PrinterDialog.CURRENT_PAGE  )
		{
			//Log.log(Log.DEBUG, this, "Returning NO_SUCH_PAGE for page " + pageIndex);
			return NO_SUCH_PAGE;	
		}
		else {
			printPage(_gfx, pageFormat, pageIndex, true);
		}

		//Log.log(Log.DEBUG, this, "Returning PAGE_EXISTS for page " + pageIndex);
		return PAGE_EXISTS;
	} 

	
	// parses the file to determine what lines belong to which page
	protected HashMap<Integer, Range> calculatePages(Graphics _gfx, PageFormat pageFormat) 
	{
		//Log.log(Log.DEBUG, this, "calculatePages for " + buffer.getName());
		pages = new HashMap<Integer, Range>();
		
		// ensure graphics and font rendering context are valid
		if (_gfx == null)
		{
			// this can happen on startup when the graphics is not yet valid
			return pages;	
		}
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setFont(font);
		if(frc == null)
		{
			frc = ((Graphics2D)_gfx).getFontRenderContext();
			//Log.log(Log.DEBUG, this, "Font render context is " + frc);
		}

		// maximum printable area
		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();
		//Log.log(Log.DEBUG, this, "calculatePages, total imageable: x=" + pageX + ", y=" + pageY + ", w=" + pageWidth + ", h=" + pageHeight);

		// calculate header height
		if(header)
		{
			double headerHeight = paintHeader(gfx, pageX, pageY, pageWidth, false);
			pageY += headerHeight;
			pageHeight -= headerHeight;
			//Log.log(Log.DEBUG, this, "calculatePages, w/header imageable: x=" + pageX + ", y=" + pageY + ", w=" + pageWidth + ", h=" + pageHeight);
		}

		// calculate footer height
		if(footer)
		{
			double footerHeight = paintFooter(gfx, pageX, pageY, pageWidth, pageHeight, 0, false);
			pageHeight -= footerHeight;
			//Log.log(Log.DEBUG, this, "calculatePages, w/footer imageable: x=" + pageX + ", y=" + pageY + ", w=" + pageWidth + ", h=" + pageHeight);
		}

		double lineNumberWidth = 0.0;

		// determine line number width
		if(lineNumbers)
		{
			String lineNumberDigits = String.valueOf(buffer.getLineCount());
			StringBuilder digits = new StringBuilder();
			for (int i = 0; i < lineNumberDigits.length(); i++)
			{
				digits.append('0');	
			}
			lineNumberWidth = font.getStringBounds(digits.toString(), frc).getWidth();
		}
		
		// calculate tab size
		int tabSize = jEdit.getIntegerProperty("print.tabSize", 4);
		StringBuilder tabs = new StringBuilder();
		char[] chars = new char[tabSize];
		for(int i = 0; i < tabSize; i++)
		{
			tabs.append(' ');
		}
		double tabWidth = font.getStringBounds(tabs.toString(), frc).getWidth();
		PrintTabExpander tabExpander = new PrintTabExpander(tabWidth);
		
		// prep for calculations
		lm = font.getLineMetrics("gGyYX", frc);
		float lineHeight = lm.getHeight();
		boolean printFolds = jEdit.getBooleanProperty("print.folds", true);
		currentPhysicalLine = 0;
		int pageCount = 0;
		int startLine = 0;
		double y = 0.0;
		
		// measure each line
		while (currentPhysicalLine <= buffer.getLineCount())
		{
			if (currentPhysicalLine == buffer.getLineCount())
			{
				// last page
				Range range = new Range(startLine, currentPhysicalLine);
				pages.put(new Integer(pageCount), range);
				Log.log(Log.DEBUG, this, "calculatePages, page " + pageCount + " has " + range);
				break;
			}
			
			// skip folded lines
			if (!printFolds && !view.getTextArea().getDisplayManager().isLineVisible(currentPhysicalLine))
			{
				++ currentPhysicalLine;
				continue;
			}
				
			// fill the line list
			lineList.clear();
			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), -1);
			buffer.markTokens(currentPhysicalLine, tokenHandler);
			
			if(y + (lineHeight * (lineList.isEmpty() ? 1 : lineList.size())) > pageHeight)
			{
				Range range = new Range(startLine, -- currentPhysicalLine);
				pages.put(new Integer(pageCount), range);
				Log.log(Log.DEBUG, this, "calculatePages, page " + pageCount + " has " + range);
				++ pageCount;
				++ currentPhysicalLine;
				startLine = currentPhysicalLine;
				y = 0.0;
				continue;
			}

			for (int i = 0; i < (lineList.isEmpty() ? 1 : lineList.size()); i++)
			{
				y += lineHeight;
			}
			++ currentPhysicalLine;
		}
		return pages;
	}

	// returns true if the given page number is one of the pages requested to
	// be printed
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
	}
	
	// actually print the page to the graphics context
	private void printPage(Graphics _gfx, PageFormat pageFormat, int pageIndex, boolean actuallyPaint)
	{
		//Log.log(Log.DEBUG, this, "printPage(" + pageIndex + ", " + actuallyPaint + ')');
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setFont(font);

		// printable dimensions
		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();
		//Log.log(Log.DEBUG, this, "#1 - Page dimensions: " + pageWidth + 'x' + pageHeight);

		// print header
		if(header)
		{
			double headerHeight = paintHeader(gfx, pageX, pageY, pageWidth, actuallyPaint);
			pageY += headerHeight;
			pageHeight -= headerHeight;
		}

		// print footer
		if(footer)
		{
			double footerHeight = paintFooter(gfx, pageX, pageY, pageWidth, pageHeight, pageIndex, actuallyPaint);
			pageHeight -= footerHeight;
		}

		// spacing workaround
		boolean glyphVector = jEdit.getBooleanProperty("print.glyphVector");
		double lineNumberWidth = 0.0;

		// determine line number width
		if(lineNumbers)
		{
			String lineNumberDigits = String.valueOf(buffer.getLineCount());
			StringBuilder digits = new StringBuilder();
			for (int i = 0; i < lineNumberDigits.length(); i++)
			{
				digits.append('0');	
			}
			lineNumberWidth = font.getStringBounds(digits.toString(), frc).getWidth();
		}

		//Log.log(Log.DEBUG,this,"#2 - Page dimensions: " + (pageWidth - lineNumberWidth) + 'x' + pageHeight);

		// calculate tab size
		int tabSize = jEdit.getIntegerProperty("print.tabSize", 4);
		StringBuilder tabs = new StringBuilder();
		for(int i = 0; i < tabSize; i++)
		{
			tabs.append(' ');
		}
		double tabWidth = font.getStringBounds(tabs.toString(), frc).getWidth();
		PrintTabExpander tabExpander = new PrintTabExpander(tabWidth);

		// prep for printing lines
		lm = font.getLineMetrics("gGyYX", frc);
		float lineHeight = lm.getHeight();
		//Log.log(Log.DEBUG, this, "Line height is " + lineHeight);
		double y = 0.0;
		Range range = pages.get(pageIndex);
		//Log.log(Log.DEBUG, this, "printing range: " + range);
		
		// print each line
		for (currentPhysicalLine = range.getStart(); currentPhysicalLine <= range.getEnd(); currentPhysicalLine++)
		{
			if(currentPhysicalLine == buffer.getLineCount())
			{
				//Log.log(Log.DEBUG, this, "Finished buffer");
				//Log.log(Log.DEBUG, this, "The end");
				break;
			}
			if (!jEdit.getBooleanProperty("print.folds",true) && !view.getTextArea().getDisplayManager().isLineVisible(currentPhysicalLine))
			{
				//Log.log(Log.DEBUG, this, "Skipping invisible line");
				continue;
			}
			
			// print only selected lines if printing selection
			if (selection && Arrays.binarySearch(selectedLines, currentPhysicalLine) < 0)
			{
				//Log.log(Log.DEBUG, this, "Skipping non-selected line: " + currentPhysicalLine); 
				continue;
			}
				
			
			// fill the line list
			lineList.clear();
			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), -1);
			buffer.markTokens(currentPhysicalLine, tokenHandler);

			if(lineNumbers && actuallyPaint)
			{
				gfx.setFont(font);
				gfx.setColor(lineNumberColor);
				gfx.drawString(String.valueOf(currentPhysicalLine + 1), (float)pageX, (float)(pageY + y + lineHeight));
			}

			if (lineList.isEmpty())
			{
				// handle blank line
				y += lineHeight;	
			}
			else
			{
				for (Chunk chunk : lineList)
				{
					y += lineHeight;
					Chunk chunks = chunk;
					if (chunks != null && actuallyPaint)
					{
						Chunk.paintChunkBackgrounds(chunks, gfx, (float) (pageX + lineNumberWidth), (float) (pageY + y), lineHeight);
						Chunk.paintChunkList(chunks, gfx, (float) (pageX + lineNumberWidth), (float) (pageY + y), glyphVector);
					}
				}
			}
			
			if (currentPhysicalLine == range.getEnd())
			{
				//Log.log(Log.DEBUG,this,"Finished page");
				break;
			}
		}
	} 

	private double paintHeader(Graphics2D gfx, double pageX, double pageY, double pageWidth, boolean actuallyPaint)
	{
		String headerText = jEdit.getProperty("print.headerText", new String[] { buffer.getName() });
		FontRenderContext frc = gfx.getFontRenderContext();
		lm = font.getLineMetrics(headerText,frc);

		Rectangle2D bounds = font.getStringBounds(headerText, frc);
		Rectangle2D headerBounds = new Rectangle2D.Double(pageX, pageY, pageWidth, bounds.getHeight());

		if(actuallyPaint)
		{
			gfx.setColor(headerColor);
			gfx.fill(headerBounds);
			gfx.setColor(headerTextColor);
			gfx.drawString(headerText, (float)(pageX + (pageWidth - bounds.getWidth()) / 2), (float)(pageY + lm.getAscent()));
		}

		return headerBounds.getHeight();
	}
	

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
	} 

	
	
	static class PrintTabExpander implements TabExpander
	{
		private double tabWidth;

		PrintTabExpander(double tabWidth)
		{
			this.tabWidth = tabWidth;
		} 

		public float nextTabStop(float x, int tabOffset)
		{
			int ntabs = (int)((x + 1) / tabWidth);
			return (float)((ntabs + 1) * tabWidth);
		} 
	} 
}
