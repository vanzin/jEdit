/*
 * NoWrapTokenHandler.java - converts tokens to chunks
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

class NoWrapTokenHandler implements TokenHandler
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

	//{{{ getFirstChunk() method
	/**
	 * Returns the first chunk.
	 * @since jEdit 4.1pre1
	 */
	public Chunk getFirstChunk()
	{
		return firstChunk;
	} //}}}

	//{{{ getLastChunk() method
	/**
	 * Returns the last chunk.
	 * @since jEdit 4.1pre1
	 */
	public Chunk getLastChunk()
	{
		return lastChunk;
	} //}}}

	//{{{ handleToken() method
	/**
	 * Called by the token marker when a syntax token has been parsed.
	 * @param length The number of characters in the token
	 * @param id The token type (one of the constants in the
	 * <code>Token</code> class).
	 * @param rules The parser rule set that generated this token
	 * @since jEdit 4.1pre1
	 */
	public void handleToken(int length, byte id, ParserRuleSet rules)
	{
		if(id == Token.END)
			return;

		if(firstChunk == null)
		{
			firstChunk = createChunk(length,id);
			lastChunk = firstChunk;
		}
		else
		{
			lastChunk.next = createChunk(length,id);
			lastChunk = lastChunk.next;
		}
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
	protected Chunk createChunk(int length, byte id)
	{
		Chunk newChunk;

		if(id == Token.WHITESPACE)
		{
			char ch = seg.array[seg.offset + totalLength];
			//{{{ Create ' ' chunk
			if(ch == ' ')
			{
				newChunk = new Chunk(Token.WHITESPACE,
					seg,totalLength,length,styles,
					fontRenderContext);
			} //}}}
			//{{{ Create '\t' chunk
			else if(ch == '\t')
			{
				// XXX: why here {offset,offset}, and up
				// there {offset,offset + 1}? this is a
				// bit silly, especially considering the
				// below 'current.length = 1'.
				newChunk = new Chunk(Token.WHITESPACE,
					seg,totalLength,0,styles,fontRenderContext);
				newChunk.length = length;

				float newX = expander.nextTabStop(x,totalLength);
				newChunk.width = newX - x;
			} //}}}
			else
			{
				// Can't happen
				throw new InternalError("Token.WHITESPACE not"
					+ " space or tab, but " + ch);
			}
		}
		//{{{ Create text chunk
		else
		{
			newChunk = new Chunk(id,seg,totalLength,length,
				styles,fontRenderContext);
		} //}}}

		totalLength += length;
		x += newChunk.width;
		return newChunk;
	} //}}}

	//}}}
}
