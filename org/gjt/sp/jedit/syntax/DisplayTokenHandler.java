/*
 * DisplayTokenHandler.java - converts tokens to chunks
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import java.util.List;
import org.gjt.sp.jedit.syntax.*;
//}}}

/**
 * Creates {@link Chunk} objects that can be painted on screen.
 */
public class DisplayTokenHandler extends DefaultTokenHandler
{
	// don't have chunks longer than 100 characters to avoid slowing things down
	public static final int MAX_CHUNK_LEN = 100;

	//{{{ init() method
	public void init(Segment seg, SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander, List out,
		float wrapMargin)
	{
		super.init();

		x = 0.0f;

		this.seg = seg;
		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;

		// SILLY: allow for anti-aliased characters' "fuzz"
		if(wrapMargin != 0.0f)
			this.wrapMargin = wrapMargin += 2.0f;
		else
			this.wrapMargin = 0.0f;

		this.out = out;
		initialSize = out.size();

		seenNonWhitespace = false;
		endX = endOfWhitespace = 0.0f;
		end = null;
	} //}}}

	//{{{ getChunkList() method
	/**
	 * Returns the list of chunks.
	 * @since jEdit 4.1pre7
	 */
	public List getChunkList()
	{
		return out;
	} //}}}

	//{{{ handleToken() method
	/**
	 * Called by the token marker when a syntax token has been parsed.
	 * @param id The token type (one of the constants in the
	 * {@link Token} class).
	 * @param offset The start offset of the token
	 * @param length The number of characters in the token
	 * @param context The line context
	 * @since jEdit 4.1pre1
	 */
	public void handleToken(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		if(id == Token.END)
		{
			if(firstToken != null)
				out.add(merge((Chunk)firstToken));
			return;
		}

		for(int splitOffset = 0; splitOffset < length; splitOffset += MAX_CHUNK_LEN)
		{
			int splitLength = Math.min(length - splitOffset,MAX_CHUNK_LEN);
			Chunk chunk = createChunk(id,offset + splitOffset,splitLength,context);
			addToken(chunk,context);

			if(wrapMargin != 0.0f)
			{
				initChunk(chunk);
				x += chunk.width;

				if(Character.isWhitespace(seg.array[
					seg.offset + chunk.offset]))
				{
					if(seenNonWhitespace)
					{
						end = lastToken;
						endX = x;
					}
					else
						endOfWhitespace = x;
				}
				else
				{
					if(x > wrapMargin
						&& end != null
						&& seenNonWhitespace)
					{
						Chunk nextLine = new Chunk(endOfWhitespace,
							end.offset + end.length,
							getParserRuleSet(context));
						initChunk(nextLine);

						nextLine.next = end.next;
						end.next = null;

						if(firstToken != null)
							out.add(merge((Chunk)firstToken));

						firstToken = nextLine;

						x = x - endX + endOfWhitespace;

						end = null;
						endX = x;
					}

					seenNonWhitespace = true;
				}
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Segment seg;
	private SyntaxStyle[] styles;
	private FontRenderContext fontRenderContext;
	private TabExpander expander;
	private float x;

	private List out;
	private float wrapMargin;
	private float endX;
	private Token end;

	private boolean seenNonWhitespace;
	private float endOfWhitespace;

	private int initialSize;
	//}}}

	//{{{ createChunk() method
	private Chunk createChunk(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		return new Chunk(id,offset,length,
			getParserRuleSet(context),styles,
			context.rules.getDefault());
	} //}}}

	//{{{ initChunk() method
	protected void initChunk(Chunk chunk)
	{
		chunk.init(seg,expander,x,fontRenderContext);
	} //}}}

	//{{{ merge() method
	private Chunk merge(Chunk first)
	{
		if(first == null)
			return null;

		Chunk chunk = first;
		while(chunk.next != null)
		{
			Chunk next = (Chunk)chunk.next;
			if(canMerge(chunk,next))
			{
				// in case already initialized; un-initialize it
				chunk.initialized = false;
				chunk.length += next.length;
				chunk.width += next.width;
				chunk.next = next.next;
			}
			else
			{
				if(!chunk.initialized)
				{
					initChunk(chunk);
					if(wrapMargin == 0.0f)
						x += chunk.width;
				}
				chunk = next;
			}
		}

		if(!chunk.initialized)
			initChunk(chunk);

		return first;
	} //}}}

	//{{{ canMerge() method
	private boolean canMerge(Chunk c1, Chunk c2)
	{
		if(!c1.accessable || !c2.accessable)
			return false;

		char ch1 = seg.array[seg.offset + c1.offset];
		char ch2 = seg.array[seg.offset + c2.offset];

		return ((c1.style == c2.style)
			&& ch1 != '\t' && ch2 != '\t'
			&& (c1.length + c2.length <= MAX_CHUNK_LEN));
	} //}}}

	//}}}
}
