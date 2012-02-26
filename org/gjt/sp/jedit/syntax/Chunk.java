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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.ref.SoftReference;

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

				if(chunks.isAccessible() && chunks.glyphs != null)
				{
					gfx.setFont(chunks.style.getFont());
					gfx.setColor(chunks.style.getForegroundColor());
					if (glyphVector)
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
				if(chunks.isAccessible())
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
			if(chunks.isAccessible() && offset < chunks.offset + chunks.length)
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
			if(chunks.isAccessible() && x < _x + chunks.width)
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
			fontSubstSystemFontsEnabled = true;
			preferredFonts = null;
		}
		else
		{
			fontSubstEnabled = Boolean.parseBoolean(props.getProperty("view.enableFontSubst"));
			fontSubstSystemFontsEnabled = Boolean.parseBoolean(props.getProperty("view.enableFontSubstSystemFonts"));
		}


		List<Font> userFonts = new ArrayList<Font>();

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

		// Clear cache, not to hold reference to old fonts which
		// might become unused after properties changed.
		glyphCache = null;
	} //}}}

	//{{{ Package private members

	//{{{ Instance variables
	SyntaxStyle style;
	// set up after init()
	float width;
	//}}}

	//{{{ Chunk constructor
	/**
	 * Constructs a virtual indent appears at the beggining of a wrapped line.
	 */
	Chunk(float width, int offset, ParserRuleSet rules)
	{
		super(Token.NULL,offset,0,rules);
		this.width = width;
		assert !isAccessible();
		assert isInitialized();
	} //}}}

	//{{{ Chunk constructor
	Chunk(byte id, int offset, int length, ParserRuleSet rules,
		SyntaxStyle[] styles, byte defaultID)
	{
		super(id,offset,length,rules);
		style = styles[id];
		background = style.getBackgroundColor();
		if(background == null)
			background = styles[defaultID].getBackgroundColor();
		assert isAccessible();
		assert !isInitialized();
	} //}}}

	//{{{ Chunk constructor
	Chunk(byte id, int offset, int length, ParserRuleSet rules,
		SyntaxStyle style, Color background)
	{
		super(id,offset,length,rules);
		this.style = style;
		this.background = background;
		assert isAccessible();
		assert !isInitialized();
	} //}}}

	//{{{ isAccessible() method
	/**
	 * Returns true if this chunk has accesible text.
	 */
	final boolean isAccessible()
	{
		return length > 0;
	} //}}}

	//{{{ isInitialized() method
	/**
	 * Returns true if this chunk is ready for painting.
	 */
	final boolean isInitialized()
	{
		return !isAccessible()	// virtual indent
			|| (glyphs != null)	// normal text
			|| (width > 0);	// tab
	} //}}}

	//{{{ isTab() method
	/**
	 * Returns true if this chunk represents a tab.
	 */
	final boolean isTab(Segment lineText)
	{
		return length == 1
			&& lineText.array[lineText.offset + offset] == '\t';
	} //}}}

	//{{{ snippetBefore() method
	/**
	 * Returns a shorten uninitialized chunk before specific offset.
	 */
	final Chunk snippetBefore(int snipOffset)
	{
		assert 0 <= snipOffset && snipOffset < length;
		return new Chunk(id, offset, snipOffset,
			rules, style, background);
	} //}}}

	//{{{ snippetAfter() method
	/**
	 * Returns a shorten uninitialized chunk after specific offset.
	 */
	final Chunk snippetAfter(int snipOffset)
	{
		assert 0 <= snipOffset && snipOffset < length;
		return new Chunk(id, offset + snipOffset, length - snipOffset,
			rules, style, background);
	} //}}}

	//{{{ snippetBeforeLineOffset() method
	/**
	 * Returns a shorten uninitialized chunk before specific offset.
	 * The offset is it in the line text, instead of in chunk.
	 */
	final Chunk snippetBeforeLineOffset(int lineOffset)
	{
		return snippetBefore(lineOffset - this.offset);
	} //}}}

	//{{{ offsetToX() method
	final float offsetToX(int offset)
	{
		if(glyphs == null)
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
	final int xToOffset(float x, boolean round)
	{
		if (glyphs == null)
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
	void init(Segment lineText, TabExpander expander, float x,
		FontRenderContext fontRenderContext, int physicalLineOffset)
	{
		if(!isAccessible())
		{
			// do nothing
		}
		else if(isTab(lineText))
		{
			float newX = expander.nextTabStop(x,physicalLineOffset+offset);
			width = newX - x;
		}
		else
		{
			str = new String(lineText.array,lineText.offset + offset,length);
			GlyphKey cacheKey = new GlyphKey(str,
				style.getFont(), fontRenderContext);
			GlyphCache cache = getGlyphCache();
			GlyphVector[] cachedGlyphs = cache.get(cacheKey);
			if (cachedGlyphs != null)
			{
				glyphs = cachedGlyphs;
				float w = 0.0f;
				for (GlyphVector gv: glyphs)
				{
					w += (float)gv.getLogicalBounds().getWidth();
				}
				width = w;
			}
			else
			{
				char[] textArray = lineText.array;
				int textStart = lineText.offset + offset;
				width = layoutGlyphs(fontRenderContext,
					textArray, textStart, textStart + length);
				cache.put(cacheKey, glyphs);
			}
		}
		assert isInitialized();
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static final char[] EMPTY_TEXT = new char[0];

	private static boolean fontSubstEnabled;
	private static boolean fontSubstSystemFontsEnabled;
	private static Font[] preferredFonts;
	private static Font[] fontSubstList;

	// This cache is meant to reduce calls of layoutGlyphVector(),
	// which was an outclassing CPU bottleneck (profiled on jProfiler,
	// Sun JDK 6, Windows XP).
	//
	// The capacity is roughly tuned so that the effect is clearly
	// noticeable on very large random int table in C mode; like
	// following:
	//   int table000[100] = { 232, 190, 69, ..., 80, 246, 78 };
	//   int table000[100] = { 69, 84, 206, ..., 160, 197, 161 };
	//   ...
	//   int table099[100] = { 219, 100, 60, ..., 100, 203, 8 };
	//   int table100[100] = { 159, 189, 159, ..., 76, 9, 239, };
	// and the additional heap usage is lower than 1 MB.
	//
	// Heap usage was measured as about 400 KB / 256 entries (JRE 7u3,
	// Windows XP).
	private static int glyphCacheCapacity = 256;
	private static SoftReference<GlyphCache> glyphCache;

	//}}}

	//{{{ Instance variables
	// this is either style.getBackgroundColor() or
	// styles[defaultID].getBackgroundColor()
	private Color background;
	private String str;
	private GlyphVector[] glyphs;
	//}}}

	//{{{ getFontSubstList() method
	private static Font[] getFontSubstList()
	{
		if (fontSubstList == null)
		{
			if (fontSubstSystemFontsEnabled)
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
			else
			{
				fontSubstList = new Font[preferredFonts.length];

				System.arraycopy(preferredFonts, 0, fontSubstList, 0,
						 preferredFonts.length);
			}
		}
		return fontSubstList;
	} //}}}

	//{{{ getSubstFont() method
	/**
	 * Returns the first font which can display a character from
	 * configured substitution candidates, or null if there is no
	 * such font.
	 */
	private static Font getSubstFont(int codepoint)
	{
		// Workaround for a problem reported in SF.net patch #3480246
		// > If font substitution with system fonts is enabled,
		// > I get for inserted control characters strange mathematical
		// > symbols from a non-unicode font in my system.
		if (Character.isISOControl(codepoint))
			return null;

		for (Font candidate: getFontSubstList())
		{
			if (candidate.canDisplay(codepoint))
			{
				return candidate;
			}
		}
		return null;
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
	 * and adds it to the list.
	 *
	 * @param	glyphs	list to add
	 * @param	font	Font to use for rendering.
	 * @param	frc	Font rendering context.
	 * @param	text	Char array with text to render.
	 * @param	start	Start index of text to render.
	 * @param	end	End index of text to render.
	 *
	 * @return Width of the rendered text.
	 */
	private static float addGlyphVector(ArrayList<GlyphVector> glyphs,
		Font font, FontRenderContext frc,
		char[] text, int start, int end)
	{
		// FIXME: Need BiDi support.
		int layoutFlags = Font.LAYOUT_LEFT_TO_RIGHT
			| Font.LAYOUT_NO_START_CONTEXT
			| Font.LAYOUT_NO_LIMIT_CONTEXT;

		GlyphVector gv = font.layoutGlyphVector(frc,
						     text,
						     start,
						     end,
						     layoutFlags);

		// This is necessary to work around a memory leak in Sun Java 6 where
		// the sun.font.GlyphLayout is cached and reused while holding an
		// instance to the char array.
		font.layoutGlyphVector(frc, EMPTY_TEXT, 0, 0, layoutFlags);

		glyphs.add(gv);
		return (float) gv.getLogicalBounds().getWidth();
	} // }}}

	//{{{ layoutGlyphs() method
	/**
	 * Layout the glyphs to render the given text, applying font
	 * substitution if configured. GlyphVectors are created and
	 * set to the internal glyph list.
	 *
	 * Font substitution works in the following manner:
	 *	- All characters that can be rendered with the main
	 *	  font will be.
	 *	- For characters that can't be handled by the main
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
		Font mainFont = style.getFont();
		ArrayList<GlyphVector> glyphs_ = new ArrayList<GlyphVector>();
		while (start < end)
		{
			int substStart = !fontSubstEnabled ? -1
				: mainFont.canDisplayUpTo(text, start, end);
			if (substStart == -1)
			{
				width += addGlyphVector(glyphs_,
					mainFont, frc, text, start, end);
				break;
			}
			if (substStart > start)
			{
				width += addGlyphVector(glyphs_,
					mainFont, frc, text, start, substStart);
			}
			int nextChar = Character.codePointAt(text, substStart);
			Font substFont = getSubstFont(nextChar);
			int substEnd = substStart + Character.charCount(nextChar);
			if (substFont != null)
			{
				while (substEnd < end &&
				      substFont == getSubstFont(nextChar = Character.codePointAt(text, substEnd)))
				{
					substEnd += Character.charCount(nextChar);
				}
				width += addGlyphVector(glyphs_,
					substFont.deriveFont(mainFont.getStyle(),
						mainFont.getSize()), frc,
					text, substStart, substEnd);
			}
			else
			{
				width += addGlyphVector(glyphs_,
					mainFont, frc, text, substStart, substEnd);
			}
			start = substEnd;
		}
		glyphs = glyphs_.toArray(new GlyphVector[glyphs_.size()]);
		return width;
	} //}}}

	//{{{ getGlyphCache() method
	private static GlyphCache getGlyphCache()
	{
		if (glyphCache != null)
		{
			GlyphCache cache = glyphCache.get();
			if (cache != null)
			{
				return cache;
			}
		}
		GlyphCache newOne = new GlyphCache(glyphCacheCapacity);
		glyphCache = new SoftReference<GlyphCache>(newOne);
		return newOne;
	} //}}}

	//{{{ class GlyphKey
	private static class GlyphKey
	{
		public final String token;
		public final Font font;
		public final FontRenderContext context;

		GlyphKey(String token, Font font, FontRenderContext context)
		{
			assert token != null;
			assert font != null;
			assert context != null;
			this.token = token;
			this.font = font;
			this.context = context;
		}

		@Override
		public final int hashCode()
		{
			return token.hashCode()
				+ font.hashCode()
				+ context.hashCode();
		}

		@Override
		public final boolean equals(Object otherObject)
		{
			// should be called only from GlyphCache to
			// compare with other keys, then explicit type
			// checking and null checking are not necessary.
			GlyphKey other = (GlyphKey)otherObject;
			return token.equals(other.token)
				&& font.equals(other.font)
				&& context.equals(other.context);
		}

		@Override
		public final String toString()
		{
			return token;
		}
	} //}}}

	//{{{ class GlyphCache
	private static class GlyphCache extends LinkedHashMap<GlyphKey, GlyphVector[]>
	{
		public GlyphCache(int capacity)
		{
			// Avoid rehashing with known limit.
			super(capacity + 1, 1.0f, true/*accessOrder*/);
			this.capacity = capacity;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<GlyphKey, GlyphVector[]> eldest)
		{
			return size() > capacity;
		}

		private final int capacity;
	} //}}}

	//}}}
}
