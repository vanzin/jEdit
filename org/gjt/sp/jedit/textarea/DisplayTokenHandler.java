/*
 * DisplayTokenHandler.java - converts tokens to chunks
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

abstract class DisplayTokenHandler implements TokenHandler
{
	//{{{ init() method
	public void init(Segment seg, SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander)
	{
		lastChunk = firstChunk = null;
		totalLength = 0;
		x = 0.0f;

		this.seg = seg;
		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;
	} //}}}

	//{{{ Protected members
	protected Chunk firstChunk, lastChunk;
	protected Segment seg;
	protected SyntaxStyle[] styles;
	protected FontRenderContext fontRenderContext;
	protected TabExpander expander;
	protected int totalLength;
	protected float x;

	//{{{ createChunk() method
	protected Chunk createChunk(int length, byte id, byte defaultID)
	{
		if(id == Token.END)
		{
			if(lastChunk != null)
			{
				lastChunk.init(seg,expander,x,styles,
					fontRenderContext,defaultID);
				x += lastChunk.width;
			}

			return null;
		}
		else
		{
			return new Chunk(id,totalLength,length);
		}
	} //}}}

	public static long merge;
	public static long add;

	//{{{ addChunk() method
	protected void addChunk(Chunk chunk, byte defaultID)
	{
		totalLength += chunk.length;

		if(firstChunk == null)
		{
			firstChunk = chunk;
			lastChunk = firstChunk;
		}
		else
		{
			if((lastChunk.id == defaultID
				&& chunk.id == Token.WHITESPACE)
				|| lastChunk.id == chunk.id)
			{
				if(chunk.id != Token.TAB)
				{
					merge++;
					lastChunk.length += chunk.length;
					return;
				}
			}

			add++;
			lastChunk.init(seg,expander,x,styles,
				fontRenderContext,defaultID);
			x += lastChunk.width;

			lastChunk.next = chunk;
			lastChunk = lastChunk.next;
		}
	} //}}}
}
