/*
 * TextRenderer2D.java - Uses new Java2D methods to draw text
 * Copyright (C) 2001 Slava Pestov
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

// this is the only file in the jEdit source that will not compile with
// Java 1.1.
import java.awt.font.*;
import java.awt.*;
import java.util.Hashtable;

class TextRenderer2D extends TextRenderer
{
	public void setupGraphics(Graphics g)
	{
		((Graphics2D)g).setRenderingHints(renderingHints);
	}

	public void configure(boolean antiAlias, boolean fracFontMetrics)
	{
		Hashtable hints = new Hashtable();

		if(antiAlias)
		{
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
				fracFontMetrics ?
					RenderingHints.VALUE_FRACTIONALMETRICS_ON
					: RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		}
		else
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		renderingHints = new RenderingHints(hints);
		fontRenderContext = new FontRenderContext(null,antiAlias,
			fracFontMetrics);
	}

	float _drawChars(char[] text, int start, int len, Graphics _g,
		float x, float y)
	{
		Graphics2D g = (Graphics2D)_g;

		Font font = g.getFont();

		// update it just in case
		fontRenderContext = g.getFontRenderContext();

		GlyphVector glyphs = font.createGlyphVector(fontRenderContext,
			new String(text,start,len));

		((Graphics2D)g).drawGlyphVector(glyphs,x,y);

		return (float)glyphs.getLogicalBounds().getWidth();
	}

	float _getWidth(char[] text, int start, int len, Font font)
	{
		GlyphVector glyphs = font.createGlyphVector(fontRenderContext,
			new String(text,start,len));

		return (float)glyphs.getLogicalBounds().getWidth();
	}

	int _xToOffset(char[] text, int start, int len, Font font, float x,
		boolean round)
	{
		// this is slow!
		TextLayout layout = new TextLayout(new String(text,start,len),font,
			fontRenderContext);

		TextHitInfo info = layout.hitTestChar(x,0);
		return (round ? info.getInsertionIndex() : info.getCharIndex());
	}

	// private members
	private RenderingHints renderingHints;
	private FontRenderContext fontRenderContext;
}
