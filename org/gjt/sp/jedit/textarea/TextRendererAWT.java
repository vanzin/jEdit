/*
 * TextRendererAWT.java - Uses old AWT methods to draw text
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

import java.awt.*;

class TextRendererAWT extends TextRenderer
{
	float _drawChars(char[] text, int start, int len, Graphics g,
		float x, float y)
	{
		g.drawChars(text,start,len,(int)x,(int)y);
		return (float)g.getFontMetrics().charsWidth(text,start,len);
	}

	float _getWidth(char[] text, int start, int len, Font font)
	{
		return (float)Toolkit.getDefaultToolkit()
			.getFontMetrics(font).charsWidth(text,start,len);
	}

	int _xToOffset(char[] text, int start, int len, Font font, float x,
		boolean round)
	{
		int end = start + len;

		FontMetrics fm = Toolkit.getDefaultToolkit()
			.getFontMetrics(font);

		int width = 0;

		for(int i = start; i < end; i++)
		{
			int newWidth = fm.charWidth(text[i]);
			if(x <= width + newWidth)
			{
				if(round && (x - width) < (width + newWidth - x))
					return i - start;
				else
					return i + 1 - start;
			}
			else
				width += newWidth;
		}

		return -1;
	}
}
