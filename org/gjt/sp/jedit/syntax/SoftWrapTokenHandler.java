/*
 * SoftWrapTokenHandler.java - converts tokens to chunks
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
 * Splits token lists to fit within a specified margin.
 */
public class SoftWrapTokenHandler extends DisplayTokenHandler
{
	//{{{ init() method
	public void init(Segment seg, SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander, List out,
		float wrapMargin)
	{
		super.init(seg,styles,fontRenderContext,expander);

		// SILLY: allow for anti-aliased characters' "fuzz"
		this.wrapMargin = wrapMargin += 2.0f;

		this.out = out;
		initialSize = out.size();

		seenNonWhitespace = addedNonWhitespace = false;
		endX = endOfWhitespace = 0.0f;
		end = null;
	} //}}}

	//{{{ getChunks() method
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
			else if(x > wrapMargin && end != null
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
	private List out;
	private float wrapMargin;
	private float endX;
	private Token end;

	private boolean seenNonWhitespace;
	private boolean addedNonWhitespace;
	private float endOfWhitespace;

	private int initialSize;
	//}}}
}
