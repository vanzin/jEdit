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

		seenNonWhitespace = addedNonWhitespace = false;
		endX = endOfWhitespace = 0.0f;
		end = null;
	} //}}}

	//{{{ setMonospacedCharWidth() method
	public void setMonospacedCharWidth(float charWidth)
	{
		this.charWidth = charWidth;
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
		Token token = createToken(id,offset,length,context);
		if(token != null)
		{
			addToken(token,context);

			if(id == Token.WHITESPACE
				|| id == Token.TAB)
			{
				if(!seenNonWhitespace)
				{
					endOfWhitespace = x;
				}
			}
			else
				seenNonWhitespace = true;

			if(out.size() == initialSize)
				out.add(firstToken);
			else if(id == Token.WHITESPACE
				|| id == Token.TAB)
			{
				if(out.size() != initialSize)
				{
					end = lastToken;
					endX = x;
				}
			}
			else if(wrapMargin != 0.0f
				&& x > wrapMargin
				&& end != null
				&& addedNonWhitespace)
			{
				Chunk blankSpace = new Chunk(endOfWhitespace,
					end.offset + end.length,
					getParserRuleSet(context));

				blankSpace.next = end.next;
				end.next = null;

				x = x - endX + endOfWhitespace;

				out.add(blankSpace);

				end = null;
				endX = x;
			}

			addedNonWhitespace = seenNonWhitespace;
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Segment seg;
	private SyntaxStyle[] styles;
	private FontRenderContext fontRenderContext;
	private TabExpander expander;
	private float x;
	private float charWidth;

	private List out;
	private float wrapMargin;
	private float endX;
	private Token end;

	private boolean seenNonWhitespace;
	private boolean addedNonWhitespace;
	private float endOfWhitespace;

	private int initialSize;
	//}}}

	//{{{ createToken() method
	protected Token createToken(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		if(id == Token.END)
			return null;

		Chunk chunk = new Chunk(id,offset,length,getParserRuleSet(context));
		chunk.init(seg,expander,x,styles,fontRenderContext,
			context.rules.getDefault(),charWidth);

		x += chunk.width;

		return chunk;
	} //}}}

	//}}}
}
