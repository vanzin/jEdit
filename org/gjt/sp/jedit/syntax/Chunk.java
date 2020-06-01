/*
 * Chunk.java - A syntax token with extra information required for painting it
 * on screen
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.*;
import java.lang.ref.SoftReference;
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
	public static final Font[] EMPTY_FONT_ARRAY = new Font[0];
	public static final GlyphVector[] EMPTY_GLYPH_VECTOR_ARRAY = new GlyphVector[0];

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

				if(chunks.isAccessible() && chunks.glyphData != null)
				{
					gfx.setFont(chunks.style.getFont());
					gfx.setColor(chunks.style.getForegroundColor());
					if (glyphVector)
						chunks.drawGlyphs(gfx, x + _x, y);
					else if(chunks.chars != null)
					{
						if (chunks.str == null) // lazy init
							chunks.str = new String(chunks.chars);

						gfx.drawString(chunks.str, x + _x, y);
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
		Graphics2D gfx, float x, float y, float lineHeight)
	{
		Rectangle clipRect = gfx.getClipBounds();

		float _x = 0.0f;

		FontMetrics forBackground = gfx.getFontMetrics();

		int ascent = forBackground.getAscent();
		float height = lineHeight;

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
							x + _x,
							y - ascent,
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
		lastSubstFont = null;
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


		List<Font> userFonts = new ArrayList<>();

		String family;
		int i = 0;
		if (props != null)
		{
			while ((family = props.getProperty("view.fontSubstList." + i)) != null)
			{
				/*
				 * The default font is Font.DIALOG if the family
				 * doesn't match any installed fonts. The following
				 * check skips fonts that don't exist.
				 */
				Font f = Font.decode(props.getProperty("view.fontSubstList." + i));
				if (!"dialog".equalsIgnoreCase(f.getFamily()) || "dialog".equalsIgnoreCase(family))
					userFonts.add(f);
				i++;
			}
		}

		preferredFonts = userFonts.toArray(EMPTY_FONT_ARRAY);

		// Clear cache, not to hold reference to old fonts which
		// might become unused after properties changed.
		glyphCache = null;
	} //}}}

	//{{{ getSubstFont() method
	/**
	 * Returns the first font which can display a character from
	 * configured substitution candidates, or null if there is no
	 * such font.
	 */
	public static Font getSubstFont(int codepoint)
	{
		// Workaround for a problem reported in SF.net patch #3480246
		// > If font substitution with system fonts is enabled,
		// > I get for inserted control characters strange mathematical
		// > symbols from a non-unicode font in my system.
		if (Character.isISOControl(codepoint))
			return null;

		if (lastSubstFont != null && lastSubstFont.canDisplay(codepoint))
			return lastSubstFont;

		for (Font candidate: getFontSubstList())
		{
			if (candidate.canDisplay(codepoint))
			{
				lastSubstFont = candidate;
				return candidate;
			}
		}
		return null;
	} //}}}

	//{{{ deriveSubstFont() method
	/**
	 * Derives a font to match the main font for purposes of
	 * font substitution.
	 * Preserves any transformations from main font.
	 * For system-fallback fonts, derives size and style from main font.
	 *
	 * @param mainFont Font to derive from
	 * @param candidateFont Font to transform
	 */
	public static Font deriveSubstFont(Font mainFont, Font candidateFont)
	{
		// adopt subst font family and size, but preserve any transformations
		// i.e. if font is squashed/sheared, subst font glyphs should be squashed
		Font substFont = candidateFont.deriveFont(mainFont.getTransform());

		// scale up system fonts (point size 1) to size of main font
		if (substFont.getSize() == 1)
			substFont = substFont.deriveFont(mainFont.getStyle(),
				mainFont.getSize());

		return substFont;
	} //}}}

	//{{{ usedFontSubstitution() method
	/**
	 * Returns true if font substitution was used in the layout of this chunk.
	 * If substitution was not used, the chunk may be assumed to be composed
	 * of one glyph using a single font.
	 */
	public boolean usedFontSubstitution()
	{
		return (fontSubstEnabled && glyphData != null &&
				(glyphData.getGlyphVectorData().length > 1 ||
				(glyphData.getGlyphVectorData().length == 1 && glyphData.getGlyphVectorData()[0].getGlyphVector().getFont() != style.getFont())));
	}
	//}}}

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
			|| (glyphData != null)	// normal text
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
		return snippetBefore(lineOffset - offset);
	} //}}}

	//{{{ offsetToX() method
	final float offsetToX(int offset)
	{
		if(glyphData == null)
			return 0.0f;

		float x = 0.0f;
		for (GlyphVectorData glyphVectorData : glyphData.getGlyphVectorData())
		{
			GlyphVector gv = glyphVectorData.getGlyphVector();
			if (offset < gv.getNumGlyphs())
			{
				x += (float) gv.getGlyphPosition(offset).getX();
				return x;
			}
			x += glyphVectorData.getWidth();
			offset -= gv.getNumGlyphs();
		}

		/* Shouldn't reach this. */
		assert false : "Shouldn't reach this.";
		return -1;
	} //}}}

	//{{{ xToOffset() method
	final int xToOffset(float x, boolean round)
	{
		if (glyphData == null)
		{
			if (round && width - x < x)
				return offset + length;
			else
				return offset;
		}

		int off = offset;
		float myx = 0.0f;
		for (GlyphVectorData glyphVectorData : glyphData.getGlyphVectorData())
		{
			GlyphVector gv = glyphVectorData.getGlyphVector();
			float gwidth = glyphVectorData.getWidth();
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
			// since Java 11, calling font.layoutGlyphVector() performances is directly linked with the
			// char array length regardless the start and end offset.
			// Since we usually have the entire buffer in that char array, a buffer of 100MB is about
			// 100 time slower to display compared to a 1MB buffer.
			// The problem is in sun.font.SunLayoutEngine class
			// private static native boolean shape(Font2D font, FontStrike strike, float ptSize, float[] mat,
			//              long pNativeFont, long pFace, boolean aat,
			//              char[] chars, GVData data,
			//              int script, int offset, int limit,
			//              int baseIndex, Point2D.Float pt, int typo_flags, int slot);
			// the reason for that is that Java is now processing the entire text to get a better bidi
			// support. As we tokenize the text, and don't support bidi (yet), we don't care.
			// So I copy the necessary chars to a temporary char array
			chars = new char[length];
			System.arraycopy(lineText.array, lineText.offset + offset, chars, 0, length);
			GlyphKey cacheKey = new GlyphKey(chars, style.getFont(), fontRenderContext);
			GlyphCache cache = getGlyphCache();
			glyphData = cache.computeIfAbsent(cacheKey, key -> buildGlyphInfo(chars, fontRenderContext));
			width = glyphData.getWidth();
		}
		assert isInitialized();
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static boolean fontSubstEnabled;
	private static boolean fontSubstSystemFontsEnabled;
	private static Font[] preferredFonts;
	@Nullable
	private static Font[] fontSubstList;
	/**
	 * lastSubstFont contains the last font that was used in Font substitution.
	 * It is set there to make searching subst font faster as when one font was found
	 * there are great chances that it matches the other chars of the same textarea.
	 */
	@Nullable
	private static Font lastSubstFont;

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
	private char[] chars;
	private String str;
	private GlyphData glyphData;
	//}}}

	//{{{ init() method
	private GlyphData buildGlyphInfo(char[] chars, FontRenderContext fontRenderContext)
	{
		GlyphVector[] glyphs = layoutGlyphs(style.getFont(), fontRenderContext, chars, 0, chars.length);
		return new GlyphData(glyphs);
	} //}}}

	//{{{ getFontSubstList() method
	/**
	 * Obtain a list of preferred fallback fonts as specified by the user
	 * (see Text Area in Global Options), as well as a list of all fonts
	 * specified in the system.
	 * Note that preferred fonts are returned with sizes as specified by the
	 * user, but system fonts all have a point size of 1. These should be
	 * scaled up once the main font is known (see layoutGlyphs()).
	 */
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
		for (GlyphVectorData vectorData : glyphData.getGlyphVectorData())
		{
			gfx.drawGlyphVector(vectorData.getGlyphVector(), x, y);
			x += vectorData.getWidth();
		}
	} //}}}

	//{{{ layoutGlyphVector() methods
	/**
	 * A wrapper of Font.layoutGlyphVector() to simplify the calls.
	 */
	@Deprecated
	private static GlyphVector layoutGlyphVector(Font font,
		FontRenderContext frc,
		char[] text, int start, int end)
	{
		int length = end - start;
		char[] tmpChars;
		if (text.length > length || start != 0)
		{
			tmpChars = new char[length];
			System.arraycopy(text, start, tmpChars, 0, length);
		}
		else
			tmpChars = text;

		return layoutGlyphVector(font, frc, tmpChars);
	}

	private static GlyphVector layoutGlyphVector(Font font, FontRenderContext frc, char[] text)
	{
		// FIXME: Need BiDi support.
		int flags = Font.LAYOUT_LEFT_TO_RIGHT
			| Font.LAYOUT_NO_START_CONTEXT
			| Font.LAYOUT_NO_LIMIT_CONTEXT;

		GlyphVector result = font.layoutGlyphVector(frc, text, 0, text.length, flags);

		// This is necessary to work around a memory leak in Sun Java 6 where
		// the sun.font.GlyphLayout is cached and reused while holding an
		// instance to the char array.
		// Since we now give small char array, and it is replaced at every call, we don't
		// need to reset it anymore but just in case it can be done by calling this
		// font.layoutGlyphVector(frc, EMPTY_TEXT, 0, 0, flags);

		if ((result.getLayoutFlags() & GlyphVector.FLAG_COMPLEX_GLYPHS) != 0)
		{
			result = font.createGlyphVector(frc, text);
		}

		return result;
	} // }}}

	//{{{ layoutGlyphs() method
	/**
	 * Layout the glyphs to render the given text, applying font
	 * substitution if configured.
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
	 */
	private static GlyphVector[] layoutGlyphs(Font mainFont,
		FontRenderContext frc,
		char[] text, int start, int end)
	{
		int substStart = !fontSubstEnabled ? -1
			: mainFont.canDisplayUpTo(text, start, end);
		if (substStart == -1)
		{
			GlyphVector gv = layoutGlyphVector(mainFont, frc,
				text, start, end);
			return new GlyphVector[] {gv};
		}
		else
		{
			FontSubstitution subst = new FontSubstitution(
				mainFont, frc, text, start);
			subst.addNonSubstRange(substStart - start);
			doFontSubstitution(subst, mainFont,
				text, substStart, end);
			subst.finish();
			return subst.getGlyphs();
		}
	} //}}}

	//{{{ doFontSubstitution() method
	private static void doFontSubstitution(FontSubstitution subst,
		Font mainFont,
		char[] text, int start, int end)
	{
		for (;;)
		{
			assert start < end;
			int nextChar = Character.codePointAt(text, start);
			int charCount = Character.charCount(nextChar);
			assert !mainFont.canDisplay(nextChar);
			Font substFont = getSubstFont(nextChar);

			if (substFont != null)
			{
				substFont = deriveSubstFont(mainFont, substFont);
				subst.addRange(substFont, charCount);
			}
			else
			{
				subst.addNonSubstRange(charCount);
			}
			start += charCount;
			if (start >= end)
			{
				break;
			}
			int nextSubstStart =
				mainFont.canDisplayUpTo(text, start, end);
			if (nextSubstStart == -1)
			{
				subst.addNonSubstRange(end - start);
				break;
			}
			subst.addNonSubstRange(nextSubstStart - start);
			start = nextSubstStart;
		}
	} //}}}

	//{{{ class FontSubstitution
	// A helper class to build GlyphVector[] with least calls to
	// layoutGlyphVector() no matter how many the font substitution
	// logic find intermediate boundaries.
	private static class FontSubstitution
	{
		FontSubstitution(Font mainFont, FontRenderContext frc,
			char[] text, int start)
		{
			this.mainFont = mainFont;
			this.frc = frc;
			this.text = text;
			rangeStart = start;
			rangeFont = null;
			rangeLength = 0;
			glyphs = new ArrayList<>();
		}

		public void addNonSubstRange(int length)
		{
			addRange(null, length);
		}

		private void addRange(Font font, int length)
		{
			assert length >= 0;
			if (length == 0)
			{
				return;
			}
			if (font == rangeFont)
			{
				rangeLength += length;
			}
			else
			{
				addGlyphVectorOfLastRange();
				rangeFont = font;
				rangeStart += rangeLength;
				rangeLength = length;
			}
		}

		public void finish()
		{
			addGlyphVectorOfLastRange();
			rangeFont = null;
			rangeStart += rangeLength;
			rangeLength = 0;
		}

		public GlyphVector[] getGlyphs()
		{
			return glyphs.toArray(EMPTY_GLYPH_VECTOR_ARRAY);
		}

		private final Font mainFont;
		private final FontRenderContext frc;
		private final char[] text;
		private int rangeStart;
		@Nullable
		private Font rangeFont;
		private int rangeLength;
		private final List<GlyphVector> glyphs;

		private void addGlyphVectorOfLastRange()
		{
			if (rangeLength == 0)
			{
				return;
			}

			Font font = (rangeFont == null) ? mainFont : rangeFont;

			GlyphVector gv = layoutGlyphVector(font, frc,
				text, rangeStart, rangeStart + rangeLength);
			glyphs.add(gv);
		}
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
		glyphCache = new SoftReference<>(newOne);
		return newOne;
	} //}}}

	//{{{ class GlyphKey
	private static class GlyphKey
	{
		public final char[] chars;
		public final Font font;
		public final FontRenderContext context;
		private final int hashCode;

		GlyphKey(@Nonnull char[] chars, @Nonnull Font font, @Nonnull FontRenderContext context)
		{
			this.chars = chars;
			this.font = font;
			this.context = context;
			hashCode =  31 * (31 * Arrays.hashCode(chars) + font.hashCode()) + context.hashCode();
		}

		@Override
		public final int hashCode()
		{
			return hashCode;
		}

		@Override
		public final boolean equals(Object otherObject)
		{
			// should be called only from GlyphCache to
			// compare with other keys, then explicit type
			// checking and null checking are not necessary.
			GlyphKey other = (GlyphKey)otherObject;
			return Arrays.equals(chars, other.chars)
				&& font.equals(other.font)
				&& context.equals(other.context);
		}

		@Override
		public final String toString()
		{
			return new String(chars);
		}
	} //}}}

	//{{{ class GlyphCache
	private static class GlyphCache extends LinkedHashMap<GlyphKey, GlyphData>
	{
		GlyphCache(int capacity)
		{
			// Avoid rehashing with known limit.
			super(capacity + 1, 1.0f, true/*accessOrder*/);
			this.capacity = capacity;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<GlyphKey, GlyphData> eldest)
		{
			return size() > capacity;
		}

		private final int capacity;
	} //}}}

	//{{{ class GlyphCache
	private static class GlyphData
	{
		private final GlyphVectorData[] glyphVectorData;
		private final float             width;

		GlyphData(GlyphVector[] glyphs)
		{
			glyphVectorData = new GlyphVectorData[glyphs.length];
			float w = 0.0f;
			for (int i = 0; i < glyphs.length; i++)
			{
				GlyphVectorData glyphVectorData = new GlyphVectorData(glyphs[i]);
				this.glyphVectorData[i] = glyphVectorData;
				w += glyphVectorData.getWidth();
			}
			width = w;
		}

		public GlyphVectorData[] getGlyphVectorData()
		{
			return glyphVectorData;
		}

		public float getWidth()
		{
			return width;
		}
	} //}}}

	//{{{ class GlyphVectorData
	private static class GlyphVectorData
	{
		private final GlyphVector glyphVector;
		private final float       width;

		private GlyphVectorData(GlyphVector glyphVector)
		{
			this.glyphVector = glyphVector;
			width = (float) glyphVector.getLogicalBounds().getWidth();
		}

		public GlyphVector getGlyphVector()
		{
			return glyphVector;
		}

		public float getWidth()
		{
			return width;
		}
	} //}}}

	//}}}
}
