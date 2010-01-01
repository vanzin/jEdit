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
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import java.awt.font.FontRenderContext;
import java.util.List;
//}}}

/**
 * Creates {@link Chunk} objects that can be painted on screen.
 * @version $Id$
 */
public class DisplayTokenHandler extends DefaultTokenHandler
{
	// don't have chunks longer than 100 characters to avoid slowing things down
	public static final int MAX_CHUNK_LEN = 100;

	//{{{ init() method
	/**
	 * Init some variables that will be used when marking tokens.
	 * This is called before {@link JEditBuffer#markTokens(int, TokenHandler)}
	 * to store some data that will be required and that we don't want
	 * to put in the parameters
	 *
	 * @param styles
	 * @param fontRenderContext
	 * @param expander
	 * @param out
	 * @param wrapMargin
	 */
	public void init(SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander, List<Chunk> out,
		float wrapMargin)
	{
		super.init();

		endX = 0.0f;

		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;

		// SILLY: allow for anti-aliased characters' "fuzz"
		if(wrapMargin != 0.0f)
			this.wrapMargin = wrapMargin + 2.0f;
		else
			this.wrapMargin = 0.0f;

		this.out = out;

		seenNonWhitespace = false;
		endOfWhitespace = 0.0f;
		afterTrailingWhitespace = false;
	} //}}}

	//{{{ getChunkList() method
	/**
	 * Returns the list of chunks.
	 * @since jEdit 4.1pre7
	 */
	public List<Chunk> getChunkList()
	{
		return out;
	} //}}}

	//{{{ handleToken() method
	/**
	 * Called by the token marker when a syntax token has been parsed.
	 * @param seg The segment containing the text
	 * @param id The token type (one of the constants in the
	 * {@link Token} class).
	 * @param offset The start offset of the token
	 * @param length The number of characters in the token
	 * @param context The line context
	 * @since jEdit 4.2pre3
	 */
	public void handleToken(Segment seg, byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		if(id == Token.END)
		{
			terminateScreenLine(seg);
			return;
		}

		for(int splitOffset = 0; splitOffset < length; splitOffset += MAX_CHUNK_LEN)
		{
			int splitLength = Math.min(length - splitOffset,MAX_CHUNK_LEN);
			Chunk chunk = createChunk(id,offset + splitOffset,splitLength,context);
			if(wrapMargin != 0.0f)
			{
				initChunk(chunk,seg);
				if(Character.isWhitespace(seg.array[
					seg.offset + chunk.offset]))
				{
					if (seenNonWhitespace)
					{
						afterTrailingWhitespace = true;
					}
				}
				else
				{
					if (!seenNonWhitespace)
					{
						endOfWhitespace = endX;
						seenNonWhitespace = true;
					}
					if(endX + chunk.width > wrapMargin
						&& afterTrailingWhitespace
						&& endX > endOfWhitespace)
					{
						wrapLine(seg, context);
					}
					afterTrailingWhitespace = false;
				}
				endX += chunk.width;
			}
			addToken(chunk,context);
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private SyntaxStyle[] styles;
	private FontRenderContext fontRenderContext;
	private TabExpander expander;
	private float endX;

	private List<Chunk> out;
	private float wrapMargin;

	private boolean seenNonWhitespace;
	private float endOfWhitespace;
	private boolean afterTrailingWhitespace;
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
	private void initChunk(Chunk chunk, Segment seg)
	{
		chunk.init(seg,expander,endX,fontRenderContext);
	} //}}}

	//{{{ merge() method
	/**
	 * Merges each adjucent chunks if possible, to reduce the number
	 * of chunks for rendering performance.
	 */
	private Chunk merge(Chunk first, Segment seg)
	{
		if(first == null)
			return null;

		Chunk chunk = first;
		while(chunk.next != null)
		{
			Chunk next = (Chunk)chunk.next;
			if(canMerge(chunk,next,seg))
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
					initChunk(chunk,seg);
					if(wrapMargin == 0.0f)
						endX += chunk.width;
				}
				chunk = next;
			}
		}

		if(!chunk.initialized)
			initChunk(chunk,seg);

		return first;
	} //}}}

	//{{{ canMerge() method
	private static boolean canMerge(Chunk c1, Chunk c2, Segment seg)
	{
		if(!c1.accessable || !c2.accessable)
			return false;

		char ch1 = seg.array[seg.offset + c1.offset];
		char ch2 = seg.array[seg.offset + c2.offset];

		return ((c1.style == c2.style)
			&& ch1 != '\t' && ch2 != '\t'
			&& (c1.length + c2.length <= MAX_CHUNK_LEN));
	} //}}}

	//{{{ wrapLine() method
	/**
	 * Starts a new screen line.
	 */
	private void wrapLine(Segment seg, TokenMarker.LineContext context)
	{
		// Wrap can't happen at the start of a screen line.
		assert lastToken != null;

		int eolOffset = lastToken.offset + lastToken.length;
		terminateScreenLine(seg);

		Chunk indent = new Chunk(endOfWhitespace, eolOffset,
			getParserRuleSet(context));
		initChunk(indent, seg);
		addToken(indent, context);

		endX = endOfWhitespace;
	} //}}}

	//{{{ terminateScreenLine() method
	/**
	 * Makes the list of tokens into a screen line.
	 * The list becomes empty.
	 */
	private void terminateScreenLine(Segment seg)
	{
		if (firstToken != null)
		{
			out.add(merge((Chunk)firstToken, seg));
			firstToken = lastToken = null;
		}
	} //}}}

	//}}}
}
