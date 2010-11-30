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
import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.IPropertyManager;
//}}}

/**
 * A syntax token with extra information required for painting it
 * on screen.
 * @since jEdit 4.1pre1
 */
public class Chunk extends Token
{
	//{{{ Static variables
	private static final char[] EMPTY_TEXT = new char[0];
	//}}}

	//{{{ paintChunkList() method
	/**
	 * Paints a chunk list.
	 * @param chunks The chunk list
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param glyphVector true if we want to use glyphVector, false if we
	 * want to use drawString
	 * @return The width of the painted text
	 * @since jEdit 4.2pre1
	 */
	public static float paintChunkList(Chunk chunks,
		Graphics2D gfx, float x, float y, boolean glyphVector)
	{
		Rectangle clipRect = gfx.getClipBounds();

		float _x = 0.0f;

		while(chunks != null)
		{
			// only paint visible chunks
			if(x + _x + chunks.width > clipRect.x
				&& x + _x < clipRect.x + clipRect.width)
			{
				// Useful for debugging purposes
				if(Debug.CHUNK_PAINT_DEBUG)
				{
					gfx.draw(new Rectangle2D.Float(x + _x,y - 10,
						chunks.width,10));
				}

				if(chunks.accessable && chunks.visible)
				{
					gfx.setFont(chunks.style.getFont());
					gfx.setColor(chunks.style.getForegroundColor());

					if (glyphVector && chunks.glyphs != null)
						chunks.drawGlyphs(gfx, x + _x, y);
					else if(chunks.str != null)
					{
						gfx.drawString(chunks.str,
							(int)(x + _x),(int)y);
					}
				}
			}

			_x += chunks.width;
			chunks = (Chunk)chunks.next;
		}

		return _x;
	} //}}}

	//{{{ paintChunkBackgrounds() method
	/**
	 * Paints the background highlights of a chunk list.
	 * @param chunks The chunk list
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @return The width of the painted backgrounds
	 * @since jEdit 4.2pre1
	 */
	public static float paintChunkBackgrounds(Chunk chunks,
		Graphics2D gfx, float x, float y, int lineHeight)
	{
		Rectangle clipRect = gfx.getClipBounds();

		float _x = 0.0f;

		FontMetrics forBackground = gfx.getFontMetrics();

		int ascent = forBackground.getAscent();
		int height = lineHeight;

		while(chunks != null)
		{
			// only paint visible chunks
			if(x + _x + chunks.width > clipRect.x
				&& x + _x < clipRect.x + clipRect.width)
			{
				if(chunks.accessable)
				{
					//{{{ Paint token background color if necessary
					Color bgColor = chunks.background;
					if(bgColor != null)
					{
						gfx.setColor(bgColor);

						gfx.fill(new Rectangle2D.Float(
							x + _x,y - ascent,
							_x + chunks.width - _x,
							height));
					} //}}}
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

	//{{{ propertiesChanged() method
	/**
	 * Reload internal configuration based on the given properties.
	 *
	 * @param	props	Configuration properties.
	 *
	 * @since jEdit 4.4pre1
	 */
	public static void propertiesChanged(IPropertyManager props)
	{
		fontSubstList = null;
		if (props == null)
		{
			fontSubstEnabled = false;
			preferredFonts = null;
		}


		List<Font> userFonts = new ArrayList<Font>();

		fontSubstEnabled = Boolean.parseBoolean(props.getProperty("view.enableFontSubst"));

		String family;
		int i = 0;
		while ((family = props.getProperty("view.fontSubstList." + i)) != null)
		{
			/*
			 * The default font is Font.DIALOG if the family
			 * doesn't match any installed fonts. The following
			 * check skips fonts that don't exist.
			 */
			Font f = new Font(family, Font.PLAIN, 12);
			if (!"dialog".equalsIgnoreCase(f.getFamily()) ||
				"dialog".equalsIgnoreCase(family))
				userFonts.add(f);
			i++;
		}

		preferredFonts = userFonts.toArray(new Font[userFonts.size()]);
	} //}}}

	//{{{ Instance variables
	public boolean accessable;
	public boolean initialized;

	// set up after init()
	public SyntaxStyle style;
	public float width;
	//}}}

	//{{{ Chunk constructor
	public Chunk(float width, int offset, ParserRuleSet rules)
	{
		super(Token.NULL,offset,0,rules);
		this.width = width;
	} //}}}

	//{{{ Chunk constructor
	public Chunk(byte id, int offset, int length, ParserRuleSet rules,
		SyntaxStyle[] styles, byte defaultID)
	{
		super(id,offset,length,rules);
		accessable = true;
		style = styles[id];
		background = style.getBackgroundColor();
		if(background == null)
			background = styles[defaultID].getBackgroundColor();
	} //}}}

	//{{{ offsetToX() method
	public final float offsetToX(int offset)
	{
		if(!visible || glyphs == null)
			return 0.0f;

		float x = 0.0f;
		for (GlyphVector gv : glyphs)
		{
			if (offset < gv.getNumGlyphs())
			{
				x += (float) gv.getGlyphPosition(offset).getX();
				return x;
			}
			x += (float) gv.getLogicalBounds().getWidth();
			offset -= gv.getNumGlyphs();
		}

		/* Shouldn't reach this. */
		assert false : "Shouldn't reach this.";
		return -1;
	} //}}}

	//{{{ xToOffset() method
	public final int xToOffset(float x, boolean round)
	{
		if (!visible || glyphs == null)
		{
			if (round && width - x < x)
				return offset + length;
			else
				return offset;
		}

		int off = offset;
		float myx = 0.0f;
		for (GlyphVector gv : glyphs)
		{
			float gwidth = (float) gv.getLogicalBounds().getWidth();
			if (myx + gwidth >= x)
			{
				float[] pos = gv.getGlyphPositions(0, gv.getNumGlyphs(), null);
				for (int i = 0; i < gv.getNumGlyphs(); i++)
				{
					float glyphX = myx + pos[i << 1];
					float nextX = (i == gv.getNumGlyphs() - 1)
					            ? width
					            : myx + pos[(i << 1) + 2];

					if (nextX > x)
					{
						if (!round || nextX - x > x - glyphX)
							return off + i;
						else
							return off + i + 1;
					}
				}
			}
			myx += gwidth;
			off += gv.getNumGlyphs();
		}

		/* Shouldn't reach this. */
		assert false : "Shouldn't reach this.";
		return -1;
	} //}}}

	//{{{ init() method
	public void init(Segment seg, TabExpander expander, float x,
		FontRenderContext fontRenderContext, int physicalLineOffset)
	{
		initialized = true;

		if(!accessable)
		{
			// do nothing
		}
		else if(length == 1 && seg.array[seg.offset + offset] == '\t')
		{
			visible = false;
			float newX = expander.nextTabStop(x,physicalLineOffset+offset);
			width = newX - x;
		}
		else
		{
			visible = true;

			str = new String(seg.array,seg.offset + offset,length);

			char[] textArray = seg.array;
			int textStart = seg.offset + offset;
			width = layoutGlyphs(fontRenderContext,
					     textArray,
					     textStart,
					     textStart + length);
		}
	} //}}}

	//{{{ Private members
	// this is either style.getBackgroundColor() or
	// styles[defaultID].getBackgroundColor()
	private Color background;
	private String str;
	//private GlyphVector gv;
	private List<GlyphVector> glyphs;
	private boolean visible;

	private static boolean fontSubstEnabled;
	private static Font[] preferredFonts;
	private static Font[] fontSubstList;

	//{{{ getFonts() method
	/**
	 * Returns a list of fonts to be searched when applying font
	 * substitution.
	 */
	private static Font[] getFonts()
	{
		if (fontSubstList == null)
		{
			Font[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

			fontSubstList = new Font[preferredFonts.length +
						 systemFonts.length];

			System.arraycopy(preferredFonts, 0, fontSubstList, 0,
					 preferredFonts.length);

			System.arraycopy(systemFonts, 0, fontSubstList,
					 preferredFonts.length,
					 systemFonts.length);
		}
		return fontSubstList;
	} //}}}

	//{{{ drawGlyphs() method
	/**
	 * Draws the internal list of glyph vectors into the given
	 * graphics object.
	 *
	 * @param	gfx	Where to draw the glyphs.
	 * @param	x	Starting horizontal position.
	 * @param	y	Vertical position.
	 */
	private void drawGlyphs(Graphics2D gfx,
				float x,
				float y)
	{
		for (GlyphVector gv : glyphs)
		{
			gfx.drawGlyphVector(gv, x, y);
			x += (float) gv.getLogicalBounds().getWidth();
		}
	} //}}}

	//{{{ addGlyphVector() method
	/**
	 * Creates a glyph vector for the text with the given font,
	 * and adds it to the internal list.
	 *
	 * @param	f	Font to use for rendering.
	 * @param	frc	Font rendering context.
	 * @param	text	Char array with text to render.
	 * @param	start	Start index of text to render.
	 * @param	end	End index of text to render.
	 *
	 * @return Width of the rendered text.
	 */
	private float addGlyphVector(Font f,
				     FontRenderContext frc,
				     char[] text,
				     int start,
				     int end)
	{
		// FIXME: Need BiDi support.
		int layoutFlags = Font.LAYOUT_LEFT_TO_RIGHT
			| Font.LAYOUT_NO_START_CONTEXT
			| Font.LAYOUT_NO_LIMIT_CONTEXT;

		GlyphVector gv = f.layoutGlyphVector(frc,
						     text,
						     start,
						     end,
						     layoutFlags);
		// This is necessary to work around a memory leak in Sun Java 6 where
		// the sun.font.GlyphLayout is cached and reused while holding an
		// instance to the char array.
		f.layoutGlyphVector(frc, EMPTY_TEXT, 0, 0, layoutFlags);
		glyphs.add(gv);
		return (float) gv.getLogicalBounds().getWidth();
	} // }}}

	//{{{ layoutGlyphs() method
	/**
	 * Layout the glyphs to render the given text, applying font
	 * substitution if configured. GlyphVectors are created and
	 * added to the internal glyph list.
	 *
	 * Font substitution works in the following manner:
	 *	- All characters that can be rendered with the default
	 *	  font will be.
	 *	- For characters that can't be handled by the default
	 *	  font, iterate over the list of available fonts to
	 *	  find an appropriate one. The first match is chosen.
	 *
	 * The user can define his list of preferred fonts, which will
	 * be tried before the system fonts.
	 *
	 * @param	frc	Font rendering context.
	 * @param	text	Char array with text to render.
	 * @param	start	Start index of text to render.
	 * @param	end	End index of text to render.
	 *
	 * @return Width of the rendered text.
	 */
	private float layoutGlyphs(FontRenderContext frc,
				   char[] text,
				   int start,
				   int end)
	{
		float width = 0.0f;
		int max = 0;
		Font dflt = style.getFont();

		glyphs = new LinkedList<GlyphVector>();

		while (max != -1 && start < end)
		{
			max = fontSubstEnabled ? dflt.canDisplayUpTo(text, start, end)
			                       : -1;
			if (max == -1)
			{
				width += addGlyphVector(dflt,
							frc,
							text,
							start,
							end);
			}
			else
			{
				/*
				 * Draw as much as we can and update the
				 * current offset.
				 */
				if (max > start)
				{
					width += addGlyphVector(dflt,
								frc,
								text,
								start,
								max);
					start = max;
				}

				/*
				 * Find a font that can display the next
				 * characters.
				 */
				Font f = null;
				for (Font candidate : getFonts())
				{
					 if (candidate.canDisplay(text[start]))
					 {
						 f = candidate;
						 break;
					 }
				}

				if (f != null)
				{
					f = f.deriveFont(dflt.getStyle(), dflt.getSize());

					/*
					 * Find out how many characters
					 * the current font cannot
					 * display, but the chosen one
					 * can.
					 */
					int last = start;
					while (last < end &&
					       f.canDisplay(text[last]) &&
					       !dflt.canDisplay(text[last]))
						last++;

					width += addGlyphVector(f,
								frc,
								text,
								start,
								last);

					start = last;
				}
				else
				{
					width += addGlyphVector(dflt,
								frc,
								text,
								start,
								start + 1);
					start++;
				}
			}
		}
		return width;
	} //}}}

	//}}}
}
