/*
 * TextRenderer.java - Draws text
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import javax.swing.text.TabExpander;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.Hashtable;
import org.gjt.sp.util.Log;
//}}}

public class TextRenderer
{
	//{{{ setupGraphics() method
	public void setupGraphics(Graphics g)
	{
		((Graphics2D)g).setRenderingHints(renderingHints);
	} //}}}

	//{{{ configure() method
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
	} //}}}

	//{{{ drawChars() method
	public float drawChars(char[] text, int off, int len, Graphics _g,
		float x, float y, TabExpander e, Color foreground,
		Color tokenBackground, Color componentBackground)
	{
		Graphics2D g = (Graphics2D)_g;

		//{{{ this probably should be moved elsewhere
		if(tokenBackground != null)
		{
			float width = charsWidth(text,off,len,g.getFont(),x,e);

			FontMetrics fm = g.getFontMetrics();
			float height = fm.getHeight();
			float descent = fm.getDescent();
			float leading = fm.getLeading();

			g.setXORMode(componentBackground);
			g.setColor(tokenBackground);
			g.fillRect((int)x,(int)(y - height + descent + leading),
				(int)width,(int)height);

			g.setPaintMode();
		} //}}}

		g.setColor(foreground);

		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					x += _drawChars(text,flushIndex,
						flushLen,g,x,y);
					flushLen = 0;
				}

				flushIndex = i + 1;

				x = e.nextTabStop(x,i - off);
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
			x += _drawChars(text,flushIndex,flushLen,g,x,y);

		return x;
	} //}}}

	//{{{ charsWidth() method
	public float charsWidth(char[] text, int off, int len, Font font, float x,
		TabExpander e)
	{
		float newX = x;

		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					newX += _getWidth(text,flushIndex,flushLen,font);
					flushLen = 0;
				}

				flushIndex = i + 1;

				newX = e.nextTabStop(newX,i - off);
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
			newX += _getWidth(text,flushIndex,flushLen,font);

		return newX - x;
	} //}}}

	//{{{ xToOffset() method
	public int xToOffset(char[] text, int off, int len, Font font, float x,
		TabExpander e, boolean round, float[] widthArray)
	{
		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		float width = widthArray[0];

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					float newWidth = _getWidth(text,flushIndex,
						flushLen,font);
					if(x <= width + newWidth)
					{
						return _xToOffset(text,flushIndex,
							flushLen,font,x - width,
							round) + flushIndex;
					}
					else
						width += newWidth;

					flushLen = 0;
				}

				flushIndex = i + 1;

				float newWidth = e.nextTabStop(width,i - off) - width;
				if(x <= width + newWidth)
				{
					if(round && (x - width) < (width + newWidth - x))
						return i;
					else
						return i + 1;
				}
				else
					width += newWidth;
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
		{
			float newWidth = _getWidth(text,flushIndex,flushLen,font);
			if(x <= width + newWidth)
			{
				return _xToOffset(text,flushIndex,flushLen,font,
					x - width,round) + flushIndex;
			}
			else
				width += newWidth;
		}

		widthArray[0] = width;
		return -1;
	} //}}}

	//{{{ Private members
	private RenderingHints renderingHints;
	private FontRenderContext fontRenderContext;

	//{{{ _drawChars() method
	private float _drawChars(char[] text, int start, int len, Graphics2D g,
		float x, float y)
	{
		Font font = g.getFont();

		// update it just in case
		fontRenderContext = g.getFontRenderContext();

		GlyphVector glyphs = font.createGlyphVector(fontRenderContext,
			new String(text,start,len));

		g.drawGlyphVector(glyphs,x,y);

		return (float)glyphs.getLogicalBounds().getWidth();
	} //}}}

	//{{{ _getWidth() method
	private float _getWidth(char[] text, int start, int len, Font font)
	{
		GlyphVector glyphs = font.createGlyphVector(fontRenderContext,
			new String(text,start,len));

		return (float)glyphs.getLogicalBounds().getWidth();
	} //}}}

	//{{{ _xToOffset() method
	private int _xToOffset(char[] text, int start, int len, Font font,
		float x, boolean round)
	{
		// this is slow!
		TextLayout layout = new TextLayout(new String(text,start,len),font,
			fontRenderContext);

		TextHitInfo info = layout.hitTestChar(x,0);
		return (round ? info.getInsertionIndex() : info.getCharIndex());
	} //}}}

	//}}}
}
