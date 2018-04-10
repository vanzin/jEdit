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
import static java.awt.RenderingHints.*;
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

	private PrintRequestAttributeSet attributes;
	private boolean firstCall;

	private View view;
	private Buffer buffer;
	private boolean reverse;
	private PrintRangeType printRangeType = PrintRangeType.ALL;
	private Font font;
	private SyntaxStyle[] styles;
	private boolean header;
	private boolean footer;
	private boolean lineNumbers;

	private HashMap<Integer, Range> pages = null;
	private int currentPhysicalLine;
	private int[] printingLineNumbers = null;

	private LineMetrics lm;
	private final List<Chunk> lineList;

	private FontRenderContext frc;

	private DisplayTokenHandler tokenHandler;
	
	BufferPrintable1_7(PrintRequestAttributeSet attributes, View view, Buffer buffer)
	{
		this.attributes = attributes;
		this.view = view;
		this.buffer = buffer;
		firstCall = true;		// pages and page ranges are calculated only once
		reverse = attributes.containsKey(Reverse.class);
		if (attributes.containsKey(PrintRangeType.class))
		{
			printRangeType = (PrintRangeType)attributes.get(PrintRangeType.class);	
		}
		
		// the buffer might have a buffer property for the line numbers, if so, then
		// the buffer is a temporary buffer representing selected text and the line
		// numbers correspond with the selected lines.
		printingLineNumbers = (int[])buffer.getProperty("printingLineNumbers");
		
		header = jEdit.getBooleanProperty("print.header");
		footer = jEdit.getBooleanProperty("print.footer");
		lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		font = jEdit.getFontProperty("print.font");
		boolean color = Chromaticity.COLOR.equals(attributes.get(Chromaticity.class));
		//Log.log(Log.DEBUG, this, "color is " + color);
		//Log.log(Log.DEBUG, this, "chromaticity is " + attributes.get(Chromaticity.class));

		styles = org.gjt.sp.util.SyntaxUtilities.loadStyles(jEdit.getProperty("print.font"), jEdit.getIntegerProperty("print.fontsize", 10), color);
		styles[Token.NULL] = new SyntaxStyle(textColor, null, font);

		// assume the paper is white, so change any white text to black
		for(int i = 0; i < styles.length; i++)
		{
			SyntaxStyle s = styles[i];
			if(s.getForegroundColor().equals(Color.WHITE) && s.getBackgroundColor() == null)
			{
				styles[i] = new SyntaxStyle(Color.BLACK, s.getBackgroundColor(), s.getFont());
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
	
	// useful to avoid having to recalculate the page ranges if they are already known
	public void setPages(HashMap<Integer, Range> pages)
	{
		this.pages = pages;	
	}
	
	// this can be called multiple times by the print system for the same page, and
	// all calls must be handled for the page to print properly. 
	public int print(Graphics _gfx, PageFormat pageFormat, int pageIndex) throws PrinterException
	{
		pageIndex += 1;		// pageIndex is 0-based, but pages are 1-based
		//Log.log(Log.DEBUG, this, "Asked to print page " + pageIndex);
		if (firstCall && pages == null)
		{
			pages = calculatePages(_gfx, pageFormat);
			if (pages == null || pages.isEmpty())
			{
				throw new PrinterException("Unable to determine page ranges.");		
			}
			firstCall = false;
		}
		
		// adjust the page index for reverse printing
		if (reverse && !PrintRangeType.CURRENT_PAGE.equals(printRangeType))
		{
			pageIndex = pages.size() - 1 - pageIndex;
			//Log.log(Log.DEBUG, this, "Reverse is on, changing page index to " + pageIndex);
		}
		
		// go ahead and print the page
		Range range = pages.get(pageIndex);
		//Log.log(Log.DEBUG, this, "range = " + range);
		if ( (range == null || !inRange(pageIndex)) && !PrintRangeType.CURRENT_PAGE.equals(printRangeType)  )
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

	
	/**
 	 * Parses the file to determine what lines belong to which page.
 	 * @param _gfx The graphics context to use for the calculations.
 	 * @param pageFormat The page format to use for the calculations.
 	 * @param force If true, force the calculation regardless of large file and long
 	 * line limits.
 	 * @return A hashmap of page number = line range for that page
 	 *
 	 * NOTE: This handles large files and long lines poorly, if the buffer size
 	 * is larger than the "largeBufferSize" property, a printer exception is thrown.
 	 * Same for long lines, if any line in the buffer is longer than the 
 	 * "longLineLimit" property, a printer exception is thrown. This seems to be
 	 * an adequate way to handle the large files, but is probably a poor way to
 	 * handle files that aren't particularly large but only have one line.
 	 */
	protected HashMap<Integer, Range> calculatePages(Graphics _gfx, PageFormat pageFormat) throws PrinterException
	{
		//Log.log(Log.DEBUG, this, "calculatePages for " + buffer.getName());
		//Log.log(Log.DEBUG, this, "graphics.getClip = " + _gfx.getClip());

		pages = new HashMap<Integer, Range>();
		
		// check large file settings
		String largeFileMode = buffer.getStringProperty("largefilemode");
		if (!"full".equals(largeFileMode))
		{
			int largeBufferSize = jEdit.getIntegerProperty("largeBufferSize", 4000000);
			if (buffer.getLength() > largeBufferSize)
			{
				throw new PrinterException("Buffer is too large to print.");
			}
		}
		
		
		// ensure graphics and font rendering context are valid
		if (_gfx == null)
		{
			// this can happen on startup when the graphics is not yet valid
			return pages;	
		}
		
		// use the rendering hints set in the text area option pane. The affine
		// transform is basic and seems to work well in all cases. I've found
		// that it's necessary to turn on the print spacing workaround in the
		// text area option pane to not get character overlap. I think this
		// setting should be on by default since it causes graphics.drawGlyphVector
		// to be used to draw the characters and the javadoc says, "This is the 
		// fastest way to render a set of characters to the screen."
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setRenderingHint(KEY_TEXT_ANTIALIASING, view.getTextArea().getPainter().getAntiAlias().renderHint());
		boolean useFractionalFontMetrics = jEdit.getBooleanProperty("view.fracFontMetrics");
		gfx.setRenderingHint(KEY_FRACTIONALMETRICS, (useFractionalFontMetrics ? VALUE_FRACTIONALMETRICS_ON : VALUE_FRACTIONALMETRICS_OFF));
		gfx.setFont(font);
		gfx.setTransform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		frc = gfx.getFontRenderContext();
		//Log.log(Log.DEBUG, this, "Font render context is " + frc);

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
		int pageCount = 1;
		int startLine = 0;
		double y = 0.0;
		
		// measure each line
		int longLineLimit = jEdit.getIntegerProperty("longLineLimit", 4000);
		int bufferLineCount = buffer.getLineCount();
		while (currentPhysicalLine <= bufferLineCount)
		{
			// line might be too long. The default long line limit is 4000 characters,
			// which is about the number of characters that fit on a single letter
			// size page, roughly 80 characters wide and 50 lines tall.
			if (currentPhysicalLine < bufferLineCount && buffer.getLineLength(currentPhysicalLine) > longLineLimit)
			{
				throw new PrinterException("Line " + (currentPhysicalLine + 1) + " is too long to print.");	
			}
			
			if (currentPhysicalLine == bufferLineCount)
			{
				// last page
				Range range = new Range(startLine, currentPhysicalLine);
				pages.put(Integer.valueOf(pageCount), range);
				//Log.log(Log.DEBUG, this, "calculatePages, page " + pageCount + " has " + range);
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
			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), 0);
			buffer.markTokens(currentPhysicalLine, tokenHandler);
			
			// check that these lines will fit on the page
			if(y + (lineHeight * (lineList.isEmpty() ? 1 : lineList.size())) > pageHeight)
			{
				Range range = new Range(startLine, Math.max(0, currentPhysicalLine - 1));
				pages.put(Integer.valueOf(pageCount), range);
				//Log.log(Log.DEBUG, this, "calculatePages, page " + pageCount + " has " + range);
				++ pageCount;
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
		PageRanges ranges = (PageRanges)attributes.get(PageRanges.class);
		//Log.log(Log.DEBUG, this, "inRange, ranges = " + ranges);
		boolean answer = false;
		if (ranges == null)
		{
			answer = true;
		}
		else 
		{
			answer = ranges.contains(pageNumber);	
		}
		//Log.log(Log.DEBUG, this, "inRange(" + pageNumber + ") returning " + answer);
		return answer;
	}
	
	// actually print the page to the graphics context
	// pageIndex is 1-based
	private void printPage(Graphics _gfx, PageFormat pageFormat, int pageIndex, boolean actuallyPaint)
	{
		//Log.log(Log.DEBUG, this, "printPage(" + pageIndex + ", " + actuallyPaint + ')');
		Graphics2D gfx = (Graphics2D)_gfx;
		float zoomLevel = 1.0f;
		if (pageFormat instanceof PrintPreviewModel)
		{
			PrintPreviewModel model = (PrintPreviewModel)pageFormat;
			zoomLevel = model.getZoomLevel();
			font = font.deriveFont(font.getSize() * zoomLevel);
			gfx.setRenderingHint(KEY_TEXT_ANTIALIASING, view.getTextArea().getPainter().getAntiAlias().renderHint());
			boolean useFractionalFontMetrics = jEdit.getBooleanProperty("view.fracFontMetrics");
			gfx.setRenderingHint(KEY_FRACTIONALMETRICS, (useFractionalFontMetrics ? VALUE_FRACTIONALMETRICS_ON : VALUE_FRACTIONALMETRICS_OFF));
			
			// TODO: why did I need this next line? Leaving it in causes the print preview 
			// to show the page with the wrong top and bottom margins, leaving it out doesn't seem
			// to cause any problems
			//gfx.setTransform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
			
			for(int i = 0; i < styles.length; i++)
			{
				SyntaxStyle s = styles[i];
				styles[i] = new SyntaxStyle(s.getForegroundColor(), s.getBackgroundColor(), font);
			}
		}
		gfx.setFont(font);
		if (frc == null)
		{
			frc = gfx.getFontRenderContext();	
		}
		
		// printable dimensions
		double pageX = pageFormat.getImageableX();
		double pageY = pageFormat.getImageableY();
		double pageWidth = pageFormat.getImageableWidth();
		double pageHeight = pageFormat.getImageableHeight();
		//Log.log(Log.DEBUG, this, "#1 - Page dimensions: (" + pageX + ", " + pageY + ") " + pageWidth + 'x' + pageHeight);

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


		// determine line number width
		double lineNumberWidth = 0.0;
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
		//Log.log(Log.DEBUG, this, "printing range for page " + pageIndex + ": " + range);
		int start = printingLineNumbers == null ? range.getStart() : 0;
		int end = printingLineNumbers == null ? range.getEnd() : printingLineNumbers.length - 1;
		
		// print each line
		for (currentPhysicalLine = start; currentPhysicalLine <= end; currentPhysicalLine++)
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
			
			// fill the line list
			lineList.clear();
			tokenHandler.init(styles, frc, tabExpander, lineList, (float)(pageWidth - lineNumberWidth), -1);
			buffer.markTokens(currentPhysicalLine, tokenHandler);

			if(lineNumbers && actuallyPaint)
			{
				gfx.setFont(font);
				gfx.setColor(lineNumberColor);
				int lineNo = currentPhysicalLine + 1;
				if (printingLineNumbers != null && currentPhysicalLine < printingLineNumbers.length) 
				{
					lineNo = printingLineNumbers[currentPhysicalLine] + 1;	
				}
				gfx.drawString(String.valueOf(lineNo), (float)pageX, (float)(pageY + y + lineHeight));
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
						Chunk.paintChunkList(chunks, gfx, (float) (pageX + lineNumberWidth), (float) (pageY + y), true);
					}
				}
			}
			
			if (currentPhysicalLine == end)
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
		lm = font.getLineMetrics(headerText, frc);

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
		String footerText = jEdit.getProperty("print.footerText", new Object[] { new Date(), Integer.valueOf(pageIndex)});
		FontRenderContext frc = gfx.getFontRenderContext();
		lm = font.getLineMetrics(footerText, frc);

		Rectangle2D bounds = font.getStringBounds(footerText, frc);
		Rectangle2D footerBounds = new Rectangle2D.Double(pageX, pageY + pageHeight - bounds.getHeight(), pageWidth, bounds.getHeight());
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
