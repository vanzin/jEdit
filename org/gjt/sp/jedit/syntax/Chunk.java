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
import org.gjt.sp.jedit.syntax.*;
//}}}

/**
 * A syntax token with extra information required for painting it
 * on screen.
 * @since jEdit 4.1pre1
 */
public class Chunk extends Token
{
	public static boolean DEBUG = false;

	//{{{ paintChunkList() method
	/**
	 * Paints a chunk list.
	 * @param lineText The line text
	 * @param chunks The chunk list
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param background The background color of the painting area,
	 * used for the background color hack
	 * @return The width of the painted text
	 * @since jEdit 4.1pre1
	 */
	public static float paintChunkList(Segment lineText, Chunk chunks,
		Graphics2D gfx, float x, float y, Color background,
		boolean glyphVector)
	{
		FontMetrics forBackground = gfx.getFontMetrics();

		float _x = 0.0f;

		for(;;)
		{
			if(chunks == null)
				return _x;

			//{{{ find run of chunks with the same token type
			Chunk start = chunks;
			float width = 0.0f;
			int length = 0;
			while(chunks != null
				&& start.style == chunks.style
				&& (start.visible == chunks.visible)
				&& (start.accessable == chunks.accessable))
			{
				length += chunks.length;
				width += chunks.width;
				chunks = (Chunk)chunks.next;
			} //}}}

			// Useful for debugging purposes
			if(DEBUG)
			{
				gfx.draw(new Rectangle2D.Float(x + _x,y - 10,
					width,10));
			}

			if(start.accessable)
			{
				//{{{ Paint token background color if necessary
				Color bgColor = start.style.getBackgroundColor();
				if(bgColor != null)
				{
					// Workaround for bug in Graphics2D in
					// JDK1.4 under Windows; calling
					// setPaintMode() does not reset
					// graphics mode.
					Graphics2D xorGfx = (Graphics2D)gfx.create();
					xorGfx.setXORMode(background);
					xorGfx.setColor(bgColor);

					xorGfx.fill(new Rectangle2D.Float(
						x + _x,y - forBackground.getAscent(),
						_x + width - _x,forBackground.getHeight()));

					xorGfx.dispose();
				} //}}}

				//{{{ If there is text in this chunk, paint it
				if(start.visible)
				{
					gfx.setFont(start.style.getFont());
					gfx.setColor(start.style.getForegroundColor());

					if(glyphVector && start.gv != null
						&& start.next == chunks)
						gfx.drawGlyphVector(start.gv,x + _x,y);
					else
					{
						gfx.drawChars(lineText.array,
							lineText.offset
							+ start.offset,length,
							(int)(x + _x),(int)y);
					}
				} //}}}
			}

			_x += width;
		}

		// for return statement see top of for() loop...
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
			if(chunks.accessable && offset < chunks.offset + chunks.length)
				return x + chunks.offsetToX(offset - chunks.offset);

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
			if(chunks.accessable && x < _x + chunks.width)
				return chunks.xToOffset(x - _x,round);

			_x += chunks.width;
			chunks = (Chunk)chunks.next;
		}

		return -1;
	} //}}}

	//{{{ Instance variables
	public boolean accessable;
	public boolean visible;

	public boolean monospaced;
	public float charWidth;

	// set up after init()
	public SyntaxStyle style;
	public float width;
	public GlyphVector gv;
	//}}}

	//{{{ Chunk constructor
	public Chunk(float width, int offset, ParserRuleSet rules)
	{
		super(Token.NULL,offset,0,rules);
		this.width = width;
	} //}}}

	//{{{ Chunk constructor
	public Chunk(byte id, int offset, int length, ParserRuleSet rules)
	{
		super(id,offset,length,rules);
		accessable = true;
	} //}}}

	//{{{ getPositions() method
	public final float[] getPositions()
	{
		if(gv == null)
			return null;

		if(positions == null)
			positions = gv.getGlyphPositions(0,length,null);

		return positions;
	} //}}}

	//{{{ offsetToX() method
	public final float offsetToX(int offset)
	{
		if(!visible)
			return 0.0f;
		else if(monospaced)
			return offset * charWidth;
		else
			return getPositions()[offset * 2];
	} //}}}

	//{{{ xToOffset() method
	public final int xToOffset(float x, boolean round)
	{
		if(!visible)
		{
			if(round && width - x < x)
				return offset + length;
			else
				return offset;
		}
		else if(monospaced)
		{
			x = Math.max(0,x);
			float remainder = x % charWidth;
			int i = (int)(x / charWidth);
			if(round && remainder > charWidth / 2)
				return offset + i + 1;
			else
				return offset + i;
		}
		else
		{
			float[] pos = getPositions();

			for(int i = 0; i < length; i++)
			{
				float glyphX = pos[i*2];
				float nextX = (i == length - 1
					? width : pos[i*2+2]);

				if(nextX > x)
				{
					if(!round || nextX - x > x - glyphX)
						return offset + i;
					else
						return offset + i + 1;
				}
			}
		}

		// wtf?
		return -1;
	} //}}}

	//{{{ init() method
	public void init(Segment seg, TabExpander expander, float x,
		SyntaxStyle[] styles, FontRenderContext fontRenderContext,
		byte defaultID, float charWidth)
	{
		style = styles[(id == Token.WHITESPACE || id == Token.TAB)
			? defaultID : id];

		if(length == 1 && seg.array[seg.offset + offset] == '\t')
		{
			visible = false;
			float newX = expander.nextTabStop(x,offset + length);
			width = newX - x;
		}
		else if(charWidth != 0.0f)
		{
			visible = monospaced = true;
			this.charWidth = charWidth;
			width = charWidth * length;
		}
		else
		{
			visible = true;
			String str = new String(seg.array,seg.offset + offset,length);
			gv = style.getFont().createGlyphVector(
				fontRenderContext,str);
			width = (float)gv.getLogicalBounds().getWidth();
		}
	} //}}}

	//{{{ Private members
	private float[] positions;
	//}}}
}
