/*
 * Chunk.java - A syntax token with extra information required for painting it
 * on screen
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

package org.gjt.sp.jedit.syntax;

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
 * A syntax token with extra information required for painting it
 * on screen.
 * @since jEdit 4.1pre1
 */
public class Chunk extends Token
{
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
	 * used for the background color hack
	 * @return The width of the painted text
	 * @since jEdit 4.1pre1
	 */
	public static float paintChunkList(Chunk chunks, Graphics2D gfx,
		float x, float y, Color background, boolean glyphVector)
	{
		FontMetrics forBackground = gfx.getFontMetrics();

		float _x = 0.0f;

		Chunk first = chunks;

		Font lastFont = null;
		Color lastColor = null;

		while(chunks != null)
		{
			if(!chunks.inaccessable)
			{
				Font font = chunks.style.getFont();
				Color bgColor = chunks.style.getBackgroundColor();
				if(bgColor != null)
				{
					float x2 = _x + chunks.width;

					// Workaround for bug in Graphics2D in
					// JDK1.4 under Windows; calling
					// setPaintMode() does not reset
					// graphics mode.
					Graphics2D xorGfx = (Graphics2D)gfx.create();
					xorGfx.setXORMode(background);
					xorGfx.setColor(bgColor);

					xorGfx.fill(new Rectangle2D.Float(
						x + _x,y - forBackground.getAscent(),
						x2 - _x,forBackground.getHeight()));

					xorGfx.dispose();
				}

				if(chunks.str != null)
				{
					gfx.setFont(font);
					gfx.setColor(chunks.style.getForegroundColor());

					if(glyphVector)
						gfx.drawGlyphVector(chunks.gv,x + _x,y);
					else
						gfx.drawString(chunks.str,x + _x,y);

					// Useful for debugging purposes
					//gfx.draw(new Rectangle2D.Float(x + chunks.x,y - 10,
					//	chunks.width,10));
				}
			}

			_x += chunks.width;
			chunks = (Chunk)chunks.next;
		}

		return _x;
	} //}}}

	//{{{ offsetToX() method
	/**
	 * Converts an offset in a chunk list into an x co-ordinate.
	 * @param chunks The chunk list
	 * @param offset The offset
	 * @since jEdit 4.1pre1
	 */
	public static float offsetToX(Chunk chunks, int offset)
	{
		if(chunks != null && offset < chunks.offset)
		{
			throw new ArrayIndexOutOfBoundsException(offset + " < "
				+ chunks.offset);
		}

		float x = 0.0f;

		while(chunks != null)
		{
			if(!chunks.inaccessable && offset < chunks.offset + chunks.length)
			{
				if(chunks.gv == null)
					break;
				else
				{
					return x + chunks.positions[
						(offset - chunks.offset) * 2];
				}
			}

			x += chunks.width;
			chunks = (Chunk)chunks.next;
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
	 * @since jEdit 4.1pre1
	 */
	public static int xToOffset(Chunk chunks, float x, boolean round)
	{
		float _x = 0.0f;

		while(chunks != null)
		{
			if(!chunks.inaccessable && x < _x + chunks.width)
			{
				if(chunks.gv == null)
				{
					if(round && _x + chunks.width - x < x - _x)
						return chunks.offset + chunks.length;
					else
						return chunks.offset;
				}
				else
				{
					float xInChunk = x - _x;

					for(int i = 0; i < chunks.length; i++)
					{
						float glyphX = chunks.positions[i*2];
						float nextX = (i == chunks.length - 1
							? chunks.width
							: chunks.positions[i*2+2]);

						if(nextX > xInChunk)
						{
							if(!round || nextX - xInChunk > xInChunk - glyphX)
								return chunks.offset + i;
							else
								return chunks.offset + i + 1;
						}
					}
				}
			}

			_x += chunks.width;
			chunks = (Chunk)chunks.next;
		}

		return -1;
	} //}}}

	//{{{ Instance variables

	// should xToOffset() ignore this chunk?
	public boolean inaccessable;

	public boolean initialized;

	// set up after init()
	public SyntaxStyle style;
	public float width;
	public String str;
	public GlyphVector gv;
	public float[] positions;
	//}}}

	//{{{ Chunk constructor
	public Chunk(float width, int offset, ParserRuleSet rules)
	{
		super(Token.NULL,offset,0,rules);

		inaccessable = true;
		this.width = width;
	} //}}}

	//{{{ Chunk constructor
	public Chunk(byte id, int offset, int length, ParserRuleSet rules)
	{
		super(id,offset,length,rules);
	} //}}}

	//{{{ init() method
	public void init(Segment seg, TabExpander expander, float x,
		SyntaxStyle[] styles, FontRenderContext fontRenderContext,
		byte defaultID)
	{
		initialized = true;

		style = styles[(id == Token.WHITESPACE || id == Token.TAB)
			? defaultID : id];

		if(length == 1 && seg.array[seg.offset + offset] == '\t')
		{
			float newX = expander.nextTabStop(x,offset + length);
			width = newX - x;
		}
		else
		{
			str = new String(seg.array,seg.offset + offset,length);

			gv = style.getFont().createGlyphVector(
				fontRenderContext,str);
			width = (float)gv.getLogicalBounds().getWidth();
			positions = gv.getGlyphPositions(0,length,null);
		}
	} //}}}
}
