/*
 * BufferPrintable.java - Printable implementation
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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
 *
 *
 * Changed 20020428/Thomas Dilts rewrote to allow non-continuous printing
 */

package org.gjt.sp.jedit.print;

//{{{ Imports
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

class BufferPrintable implements Printable
{
	//{{{ BufferPrintable constructor
	BufferPrintable(View view, Buffer buffer, Font font, boolean header,
		boolean footer, boolean lineNumbers, boolean color)
	{
		this.view = view;
		this.buffer = buffer;
		this.font = font;
		this.header = header;
		this.footer = footer;
		this.lineNumbers = lineNumbers;

		styles = GUIUtilities.loadStyles(jEdit.getProperty("print.font"),
		                                 jEdit.getIntegerProperty("print.fontsize",10),color);
		styles[Token.NULL] = new SyntaxStyle(textColor,null,font);

		lineList = new ArrayList();
		LastPagelineList = new ArrayList();

		// On some JDKs, we have to paint a glyph vector instead of a
		// string, otherwise the string will 
		glyphVector = jEdit.getBooleanProperty("print.workaround");
	} //}}}

	//{{{ print() method
	public int print(Graphics _gfx, PageFormat pageFormat, int pageIndex)
		throws PrinterException
	{

		//{{{ get/calculate all the printing constants
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

		lm = font.getLineMetrics("ABCDEgWZyqp",frc);
		int linesPerPage=(int)(pageHeight/lm.getHeight() );


		// the +1's ensure that 99 gets 3 digits, 103 gets 4 digits,
		// and so on.
		int lineNumberDigits = (int)Math.ceil(Math.log(buffer.getLineCount() + 1)
		                                      / Math.log(10)) + 1;

		//{{{ now that we know how many chars there are, get the width.
		char[] chars = new char[lineNumberDigits];
		for(int i = 0; i < chars.length; i++)
			chars[i] = ' ';
		double lineNumberWidth = font.getStringBounds(chars,
		                         0,lineNumberDigits,frc).getWidth();
		//}}}

		//{{{ calculate tab size
		int tabSize = jEdit.getIntegerProperty("print.tabSize",8);
		chars = new char[tabSize];
		for(int i = 0; i < chars.length; i++)
			chars[i] = ' ';
		double tabWidth = font.getStringBounds(chars,
		                                       0,tabSize,frc).getWidth();
		PrintTabExpander e = new PrintTabExpander(pageX,tabWidth);
		//}}}

		//}}}

		//{{{ get the correct line number in the buffer for this page number

		if(pageIndex == lastPrintedPage )
		{
			//we are redoing the last page
			ArrayList lineListForNextPage=new ArrayList();
			for (int i=0;i<lineList.size();i++)
				lineListForNextPage.add(lineList.get(i));
			lineList.clear();
			for (int i=0;i<LastPagelineList.size();i++)
				lineList.add(LastPagelineList.get(i));
			int returnValue= renderPage(pageIndex,gfx,frc,false,linesPerPage,pageWidth,
				lineNumberWidth,e,pageX,pageY);
			
			//restore the continuing lines for the next page
			if (pageIndex != 0)
			{
				lineList.clear();
				for (int i=0;i<lineListForNextPage.size();i++)
					lineList.add(lineListForNextPage.get(i));
			}
			return returnValue;
		}
		else if(pageIndex == (lastPrintedPage+1))
		{
			//we are continueing printing to the next page
			if(end)
			{
				//we hit the end of the file on the last printing.
				//there are no more pages!
				return NO_SUCH_PAGE;
			}
			//all the pointers and counters should be correct from the last page
			//no need to set the value of anything
		}
		else 
		{
			//non continuous printing, we must start reading the
			//buffer from the begining until we get to the page to print.

			//'end' is now invalid, reset it
			end = false;
			lineList=new ArrayList();
			currentPhysicalLine=0;
			for(int i = 0 ; i < pageIndex && !end ; i++)
			{
				renderPage(i,gfx,frc,true,linesPerPage,pageWidth,lineNumberWidth,e,pageX,pageY);
			}
			if(end)
			{
				//we hit the end of the file 
				//the requested page does not exist!!!
				return NO_SUCH_PAGE;
			}
		}
		//}}}

		return renderPage(pageIndex,gfx,frc,false,linesPerPage,pageWidth,lineNumberWidth,e,pageX,pageY);
	}//}}}

	//{{{ renderPage() method
	private int renderPage(int pageIndex, Graphics2D gfx,
		FontRenderContext frc, boolean justCounting,
		int linesPerPage, double pageWidth,
		double lineNumberWidth, PrintTabExpander e,
		double pageX, double pageY)
	{
		Segment seg = new Segment();
		double y = 0.0;
		String messageText = jEdit.getProperty("view.status.print",
			new Object[] { new Integer(pageIndex+1) });
		view.getStatus().setMessageAndClear(messageText);

		LastPagelineList=new ArrayList();
		
		// each loop will print one line on the page
		//but perhaps less than one buffer line.
print_loop:	for(int i = 0 ; i < linesPerPage ; i++ )
		{
			//{{{ get line text
			if(lineList.size()==0)
			{
				buffer.getLineText(currentPhysicalLine,seg);
				lm = font.getLineMetrics(seg.array,
					seg.offset,seg.count,frc);

				softWrap.init(seg,styles,frc,e,lineList,
					(float)(pageWidth - lineNumberWidth));

				buffer.markTokens(currentPhysicalLine,softWrap);

				lineList.add(new Integer(++currentPhysicalLine));

				if(lineList.size() == 1)
					lineList.add(null);
			} //}}}
			y += lm.getHeight();
			Object obj = lineList.get(0);
			LastPagelineList.add(obj);
			lineList.remove(0);
			if(obj instanceof Integer)
			{
				//{{{ paint line number
				if(lineNumbers &&  ! justCounting)
				{
					gfx.setFont(font);
					gfx.setColor(lineNumberColor);
					String lineNumberString = String.valueOf(obj);
					gfx.drawString(lineNumberString,
					               (float)pageX,(float)(pageY + y));
				} //}}}

				obj = lineList.get(0);
				LastPagelineList.add(obj);
				lineList.remove(0);
			}

			//{{{ paint the line
			if(obj != null && ! justCounting)
			{
				Chunk line = (Chunk)obj;

				Chunk.paintChunkList(line,gfx,
					(float)(pageX + lineNumberWidth),
					(float)(pageY + y),
					Color.white,glyphVector);
			}
			//}}}

			if(currentPhysicalLine >= buffer.getLineCount()
			                && lineList.size() == 0)
			{
				end = true;
				break print_loop;
			}
		}
		lastPrintedPage=pageIndex;
		return PAGE_EXISTS;
	} //}}}

	//{{{ Private members

	//{{{ Static variables
	private Color headerColor = Color.lightGray;
	private Color headerTextColor = Color.black;
	private Color footerColor = Color.lightGray;
	private Color footerTextColor = Color.black;
	private Color lineNumberColor = Color.gray;
	private Color textColor = Color.black;
	//}}}

	//{{{ Instance variables
	private Buffer buffer;
	private org.gjt.sp.jedit.View view;
	private Font font;
	private SyntaxStyle[] styles;
	private boolean header;
	private boolean footer;
	private boolean lineNumbers;

	private int lastPrintedPage;
	private int currentPhysicalLine;
	private boolean end;

	private LineMetrics lm;
	private ArrayList lineList;
	private ArrayList LastPagelineList;

	private boolean glyphVector;

	private SoftWrapTokenHandler softWrap;
	//}}}

	//{{{ paintHeader() method
	private double paintHeader(Graphics2D gfx, double pageX, double pageY,
	                           double pageWidth)
	{
		String headerText = jEdit.getProperty("print.headerText",
			new String[] { buffer.toString() });
		FontRenderContext frc = gfx.getFontRenderContext();

		gfx.setColor(headerColor);

		Rectangle2D bounds = font.getStringBounds(headerText,frc);

		Rectangle2D headerBounds = new Rectangle2D.Double(
			pageX,pageY,pageWidth,bounds.getHeight());
		gfx.fill(headerBounds);

		gfx.setColor(headerTextColor);

		lm = font.getLineMetrics(headerText,frc);
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

		lm = font.getLineMetrics(footerText,frc);
		gfx.drawString(footerText,
		               (float)(pageX + (pageWidth - bounds.getWidth()) / 2),
		               (float)(pageY + pageHeight - bounds.getHeight()
		                       + lm.getAscent()));

		return footerBounds.getHeight();
	} //}}}

	//}}}

	//{{{ PrintTabExpander class
	static class PrintTabExpander implements TabExpander
	{
		private double pageX;
		private double tabWidth;

		//{{{ PrintTabExpander constructor
		public PrintTabExpander(double pageX, double tabWidth)
		{
			this.pageX = pageX;
			this.tabWidth = tabWidth;
		} //}}}

		//{{{ nextTabStop() method
		public float nextTabStop(float x, int tabOffset)
		{
			int ntabs = (int)((x - pageX) / tabWidth);
			return (float)((ntabs + 1) * tabWidth + pageX);
		} //}}}
	} //}}}
}
