/*
 * ChunkCache.java - Intermediate layer between token lists from a TokenMarker
 * and what you see on screen
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
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
import javax.swing.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.ArrayList;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;
//}}}

/**
 * A "chunk" is a run of text with a specified font style and color. This class
 * contains various static methods for manipulating chunks and chunk lists. It
 * also has a number of package-private instance methods used internally by the
 * text area for painting text.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class ChunkCache
{
	//{{{ lineToChunkList() method
	/**
	 * Converts a line of text into one or more chunk lists. There will be
	 * one chunk list if soft wrap is disabled, more than one otherwise.
	 * @param seg The segment containing line text
	 * @param tokens The line's syntax tokens
	 * @param styles The styles to highlight the line with
	 * @param fontRenderContext Text transform, anti-alias, fractional font
	 * metrics
	 * @param e Used for calculating tab offsets
	 * @param wrapMargin The wrap margin width, in pixels. 0 disables
	 * @param out All resulting chunk lists will be appended to this list
	 * @since jEdit 4.0pre4
	 */
	public static void lineToChunkList(Segment seg, Token tokens,
		SyntaxStyle[] styles, FontRenderContext fontRenderContext,
		TabExpander e, float wrapMargin, java.util.List out)
	{
		// SILLY: allow for anti-aliased characters' "fuzz"
		if(wrapMargin != 0.0)
			wrapMargin += 2.0f;

		float x = 0.0f;
		boolean seenNonWhiteSpace = false;
		boolean addedNonWhiteSpace = false;
		float firstNonWhiteSpace = 0.0f;

		Chunk first = null;
		Chunk current = null;

		int tokenListOffset = 0;

		while(tokens.id != Token.END)
		{
			int flushIndex = tokenListOffset;

			for(int i = tokenListOffset; i < tokenListOffset + tokens.length; i++)
			{
				char ch = seg.array[seg.offset + i];

				if(ch == ' ' || ch == '\t')
				{
					if(i != flushIndex)
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,flushIndex,
							i,styles,fontRenderContext);
						if(addedNonWhiteSpace
							&& wrapMargin != 0
							&& x + newChunk.width
							> wrapMargin)
						{
							if(first != null)
								out.add(first);
							first = null;
							newChunk.x = firstNonWhiteSpace;
							x = firstNonWhiteSpace
								+ newChunk.width;
						}
						else
						{
							newChunk.x = x;
							x += newChunk.width;
						}

						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						seenNonWhiteSpace = true;
						addedNonWhiteSpace = true;
					}

					if(ch == ' ')
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,i,i + 1,
							styles,fontRenderContext);
						newChunk.x = x;
						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						x += current.width;
					}
					else if(ch == '\t')
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,i,i,
							styles,fontRenderContext);
						newChunk.x = x;
						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						x = e.nextTabStop(x,i + tokenListOffset);
						current.width = x - current.x;
						current.length = 1;
					}

					if(!seenNonWhiteSpace)
						firstNonWhiteSpace = x;

					if(first == null)
						first = current;

					flushIndex = i + 1;
				}
				else if(i == tokenListOffset + tokens.length - 1)
				{
					Chunk newChunk = new Chunk(
						tokens.id,seg,flushIndex,
						i + 1,styles,fontRenderContext);

					if(/* i == seg.count - 1 && */ wrapMargin != 0
						&& x + newChunk.width > wrapMargin
						&& addedNonWhiteSpace)
					{
						if(first != null)
							out.add(first);
						first = null;
						newChunk.x = firstNonWhiteSpace;
						x = firstNonWhiteSpace
							+ newChunk.width;
					}
					else
					{
						newChunk.x = x;
						x += newChunk.width;
					}

					if(first == null)
						first = current = newChunk;
					else
					{
						current.next = newChunk;
						current = newChunk;
					}

					seenNonWhiteSpace = true;
					addedNonWhiteSpace = true;
				}
			}

			tokenListOffset += tokens.length;
			tokens = tokens.next;
		}

		if(first != null)
			out.add(first);
	} //}}}

	//{{{ paintChunkList() method
	/**
	 * Paints a chunk list.
	 * @param chunks The chunk list
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param width The width of the painting area, used for a token
	 * background color hack
	 * @param background The background color of the painting area,
	 * used for background color hack
	 * @return The width of the painted text
	 * @since jEdit 4.0pre4
	 */
	public static float paintChunkList(Chunk chunks, Graphics2D gfx,
		float x, float y, float width, Color background)
	{
		FontMetrics forBackground = gfx.getFontMetrics();

		float _x = 0.0f;

		Font lastFont = null;
		Color lastColor = null;

		while(chunks != null)
		{
			if(chunks.text != null)
			{
				Font font = chunks.style.getFont();
				Color bgColor = chunks.style.getBackgroundColor();
				if(bgColor != null)
				{
					float x1 = (_x == 0.0f ? x : x + chunks.x);
					float x2;
					if(chunks.next == null)
						x2 = width;
					else if(chunks.next.style == chunks.style)
						x2 = x + chunks.next.x;
					else
						x2 = x + chunks.width;

					//LineMetrics lm = font.getLineMetrics(
					//	chunks.text,gfx.getFontRenderContext());
					gfx.setXORMode(background);
					gfx.setColor(bgColor);

					gfx.fill(new Rectangle2D.Float(
						x1,y - forBackground.getAscent(),
						x2 - x1,forBackground.getHeight()));

					gfx.setPaintMode();
				}

				gfx.setFont(font);
				gfx.setColor(chunks.style.getForegroundColor());

				gfx.drawGlyphVector(chunks.text,x + chunks.x,y);
				//gfx.drawString(chunks.str,x + chunks.x,y);

				// Useful for debugging purposes
				//gfx.draw(new Rectangle2D.Float(x + chunks.x,y - 10,
				//	chunks.width,10));
			}

			_x = chunks.x + chunks.width;
			chunks = chunks.next;
		}

		return _x;
	} //}}}

	//{{{ offsetToX() method
	/**
	 * Converts an offset in a chunk list into an x co-ordinate.
	 * @param chunks The chunk list
	 * @param offset The offset
	 * @since jEdit 4.0pre4
	 */
	public static float offsetToX(Chunk chunks, int offset)
	{
		if(offset < 0)
			throw new ArrayIndexOutOfBoundsException(offset + " < 0");

		float x = 0.0f;

		while(chunks != null)
		{
			if(offset < chunks.offset + chunks.length)
			{
				if(chunks.text == null)
					return chunks.x;
				else
				{
					return chunks.x + chunks.positions[
						(offset - chunks.offset) * 2];
				}
			}

			x = chunks.x + chunks.width;
			chunks = chunks.next;
		}

		return x;
	} //}}}

	//{{{ xToOffset() method
	/**
	 * Converts an x co-ordinate in a chunk list into an offset.
	 * @param chunks The chunk list
	 * @param x The x co-ordinate
	 * @param round Round up to next letter if past the middle of a letter?
	 * @return The offset within the line, or -1 if the x co-ordinate is too
	 * far to the right
	 * @since jEdit 4.0pre4
	 */
	public static int xToOffset(Chunk chunks, float x, boolean round)
	{
		while(chunks != null)
		{
			if(x < chunks.x + chunks.width)
			{
				if(chunks.text == null)
				{
					if(round && chunks.x + chunks.width - x < x - chunks.x)
						return chunks.offset + chunks.length;
					else
						return chunks.offset;
				}
				else
				{
					float _x = x - chunks.x;

					for(int i = 0; i < chunks.length; i++)
					{
						float glyphX = chunks.positions[i*2];
						float nextX = (i == chunks.length - 1
							? chunks.width
							: chunks.positions[i*2+2]);

						if(nextX > _x)
						{
							if(round && nextX - _x > _x - glyphX)
								return chunks.offset + i;
							else
								return chunks.offset + i + 1;
						}
					}
				}
			}

			chunks = chunks.next;
		}

		return -1;
	} //}}}

	//{{{ Chunk class
	/**
	 * A linked-list useful for painting syntax highlighted text and
	 * calculating offsets.
	 * @since jEdit 4.0pre4
	 */
	public static class Chunk
	{
		public float x;
		public float width;
		public SyntaxStyle style;
		public int offset;
		public int length;
		public String str;
		public GlyphVector text;
		public float[] positions;

		public Chunk next;

		Chunk(int tokenType, Segment seg, int offset, int end,
			SyntaxStyle[] styles, FontRenderContext fontRenderContext)
		{
			style = styles[tokenType];

			if(offset != end)
			{
				length = end - offset;
				str = new String(seg.array,seg.offset + offset,length);

				text = style.getFont().createGlyphVector(
					fontRenderContext,str);
				width = (float)text.getLogicalBounds().getWidth();
				positions = text.getGlyphPositions(0,length,null);
			}

			this.offset = offset;
		}
	} //}}}

	//{{{ ChunkCache constructor
	ChunkCache(JEditTextArea textArea)
	{
		this.textArea = textArea;
		out = new ArrayList();
	} //}}}

	//{{{ getMaxHorizontalScrollWidth() method
	int getMaxHorizontalScrollWidth()
	{
		int max = 0;
		for(int i = 0; i < lineInfo.length; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.chunksValid && info.width > max)
				max = info.width;
		}
		return max;
	} //}}}

	//{{{ getScreenLineOfOffset() method
	int getScreenLineOfOffset(int line, int offset)
	{
		int screenLine;

		if(line == lastScreenLineP)
		{
			LineInfo last = lineInfo[lastScreenLine];

			if(offset >= last.offset
				&& offset < last.offset + last.length)
			{
				updateChunksUpTo(lastScreenLine);
				return lastScreenLine;
			}
		}

		if(line < textArea.getFirstPhysicalLine())
		{
			return -1;
		}
		else if(line > textArea.getLastPhysicalLine())
		{
			return -1;
		}
		else
		{
			screenLine = -1;

			// Find the screen line containing this offset
			for(int i = 0; i < lineInfo.length - 1; i++)
			{
				updateChunksUpTo(i);

				LineInfo info = lineInfo[i];
				if(info.physicalLine > line)
				{
					// line is invisible?
					if(i == 0)
						screenLine = 0;
					else
						screenLine = i - 1;
					break;
				}
				else if(info.physicalLine == line)
				{
					if(offset >= info.offset
						&& offset < info.offset + info.length)
					{
						screenLine = i;
						break;
					}
				}
			}

			if(screenLine == -1)
				screenLine = lineInfo.length - 1;
		}

		lastScreenLineP = line;
		lastScreenLine = screenLine;

		return screenLine;
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		lineInfo = new LineInfo[textArea.getVisibleLines() + 1];
		for(int i = 0; i < lineInfo.length; i++)
			lineInfo[i] = new LineInfo();

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ setFirstLine() method
	void setFirstLine(int firstLine)
	{
		if(textArea.softWrap || Math.abs(firstLine - this.firstLine) >= lineInfo.length)
		{
			for(int i = 0; i < lineInfo.length; i++)
			{
				lineInfo[i].chunksValid = false;
			}
		}
		else if(firstLine > this.firstLine)
		{
			System.arraycopy(lineInfo,firstLine - this.firstLine,
				lineInfo,0,lineInfo.length - firstLine
				+ this.firstLine);

			for(int i = lineInfo.length - firstLine
				+ this.firstLine; i < lineInfo.length; i++)
			{
				lineInfo[i] = new LineInfo();
			}
		}
		else if(this.firstLine > firstLine)
		{
			System.arraycopy(lineInfo,0,lineInfo,this.firstLine - firstLine,
				lineInfo.length - this.firstLine + firstLine);

			for(int i = 0; i < this.firstLine - firstLine; i++)
			{
				lineInfo[i] = new LineInfo();
			}
		}

		lastScreenLine = lastScreenLineP = -1;
		this.firstLine = firstLine;
	} //}}}

	//{{{ invalidateAll() method
	void invalidateAll()
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			lineInfo[i].chunksValid = false;
		}

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFrom() method
	void invalidateChunksFrom(int screenLine)
	{
		for(int i = screenLine; i < lineInfo.length; i++)
		{
			lineInfo[i].chunksValid = false;
		}

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFromPhys() method
	void invalidateChunksFromPhys(int physicalLine)
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			if(lineInfo[i].physicalLine >= physicalLine)
			{
				invalidateChunksFrom(i);
				break;
			}
		}
	} //}}}

	//{{{ updateChunksUpTo() method
	void updateChunksUpTo(int lastScreenLine)
	{
		// TODO
		if(lastScreenLine >= lineInfo.length)
			textArea.recalculateVisibleLines();

		if(lineInfo[lastScreenLine].chunksValid)
			return;

		int firstScreenLine = 0;

		for(int i = lastScreenLine; i >= 0; i--)
		{
			if(lineInfo[i].chunksValid)
			{
				firstScreenLine = i + 1;
				break;
			}
		}

		int physicalLine;

		if(firstScreenLine == 0)
		{
			physicalLine = textArea.getFirstPhysicalLine();
		}
		else
		{
			int prevPhysLine = lineInfo[
				firstScreenLine - 1]
				.physicalLine;
			if(prevPhysLine == -1)
				physicalLine = -1;
			else
			{
				physicalLine = textArea
					.getFoldVisibilityManager()
					.getNextVisibleLine(prevPhysLine);
			}
		}

		// Note that we rely on the fact that when a physical line is
		// invalidated, all screen lines/subregions of that line are
		// invalidated as well. See below comment for code that tries
		// to uphold this assumption.

		Buffer buffer = textArea.getBuffer();
		TextAreaPainter painter = textArea.getPainter();

		out.clear();

		int offset = 0;
		int length = 0;

		for(int i = firstScreenLine; i <= lastScreenLine; i++)
		{
			LineInfo info = lineInfo[i];

			Chunk chunks;

			if(out.size() == 0)
			{
				if(physicalLine != -1 && i != firstScreenLine)
				{
					physicalLine = textArea.getFoldVisibilityManager()
						.getNextVisibleLine(physicalLine);
				}

				if(physicalLine == -1)
				{
					info.chunks = null;
					info.chunksValid = true;
					info.physicalLine = -1;
					continue;
				}

				buffer.getLineText(physicalLine,textArea.lineSegment);

				lineToChunkList(textArea.lineSegment,
					buffer.markTokens(physicalLine).getFirstToken(),
					painter.getStyles(),painter.getFontRenderContext(),
					painter,textArea.softWrap
					? textArea.wrapMargin
					: 0.0f,out);

				info.firstSubregion = true;

				if(out.size() == 0)
				{
					chunks = null;
					offset = 0;
					length = 1;
				}
				else
				{
					chunks = (Chunk)out.get(0);
					out.remove(0);
					offset = 0;
					if(out.size() != 0)
						length = ((Chunk)out.get(0)).offset - offset;
					else
						length = buffer.getLineLength(physicalLine) - offset + 1;
				}
			}
			else
			{
				chunks = (Chunk)out.get(0);
				out.remove(0);
				offset = chunks.offset;
				if(out.size() != 0)
					length = ((Chunk)out.get(0)).offset - offset;
				else
					length = buffer.getLineLength(physicalLine) - offset + 1;
			}

			if(i == lastScreenLine
				&& lastScreenLine != lineInfo.length - 1)
			{
				/* If this line has become longer or shorter
				 * (in which case the new physical line number
				 * is different from the cached one) we need to:
				 * - continue updating past the last line
				 * - advise the text area to repaint
				 * On the other hand, if the line wraps beyond
				 * lastScreenLine, we need to keep updating the
				 * chunk list to ensure proper alignment of
				 * invalidation flags (see start of method) */
				if(info.physicalLine != physicalLine)
				{
					lastScreenLine++;
					needFullRepaint = true;
				}
				else if(out.size() != 0)
				{
					lastScreenLine++;
				}
			}

			info.physicalLine = physicalLine;
			info.lastSubregion = (out.size() == 0);
			info.offset = offset;
			info.length = length;
			info.chunks = chunks;
			info.chunksValid = true;
		}
	} //}}}

	//{{{ getLineInfo() method
	LineInfo getLineInfo(int screenLine)
	{
		//if(!lineInfo[screenLine].chunksValid)
		//	Log.log(Log.ERROR,this,"Not up-to-date: " + screenLine);
		return lineInfo[screenLine];
	} //}}}

	//{{{ needFullRepaint() method
	/**
	 * The needFullRepaint variable becomes true when the number of screen
	 * lines in a physical line changes.
	 */
	boolean needFullRepaint()
	{
		boolean retVal = needFullRepaint;
		needFullRepaint = false;
		return retVal;
	} //}}}

	//{{{ getLineInfoBackwardsCompatibility() method
	LineInfo getLineInfoBackwardsCompatibility(int physicalLineIndex)
	{
		LineInfo info = new LineInfo();

		out.clear();
		Buffer buffer = textArea.getBuffer();
		buffer.getLineText(physicalLineIndex,textArea.lineSegment);

		TextAreaPainter painter = textArea.getPainter();
		lineToChunkList(textArea.lineSegment,
			buffer.markTokens(physicalLineIndex).getFirstToken(),
			painter.getStyles(),painter.getFontRenderContext(),
			painter,0.0f,out);

		if(out.size() == 0)
			info.chunks = null;
		else
			info.chunks = (Chunk)out.get(0);

		info.physicalLine = physicalLineIndex;
		info.chunksValid = true;

		return info;
	} //}}}

	//{{{ Private members
	private JEditTextArea textArea;
	private int firstLine;
	private LineInfo[] lineInfo;
	private ArrayList out;

	private int lastScreenLineP;
	private int lastScreenLine;

	private boolean needFullRepaint;
	//}}}

	//{{{ LineInfo class
	static class LineInfo
	{
		int physicalLine;
		int offset;
		int length;
		boolean firstSubregion;
		boolean lastSubregion;
		boolean chunksValid;
		Chunk chunks;
		int width;
	} //}}}
}
