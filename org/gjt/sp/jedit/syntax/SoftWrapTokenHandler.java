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
import java.awt.geom.*;
import java.awt.*;
import java.util.ArrayList;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

public class SoftWrapTokenHandler extends DisplayTokenHandler
{
	//{{{ init() method
	public void init(Segment seg, SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander, ArrayList out,
		float wrapMargin)
	{
		super.init(seg,styles,fontRenderContext,expander);

		// SILLY: allow for anti-aliased characters' "fuzz"
		this.wrapMargin = wrapMargin += 2.0f;

		this.out = out;
	} //}}}

	//{{{ getChunks() method
	/**
	 * Returns the list of chunks.
	 * @since jEdit 4.1pre1
	 */
	public ArrayList getChunks()
	{
		return out;
	} //}}}

	//{{{ handleToken() method
	/**
	 * Called by the token marker when a syntax token has been parsed.
	 * @param id The token type (one of the constants in the
	 * <code>Token</code> class).
	 * @param offset The start offset of the token
	 * @param length The number of characters in the token
	 * @param context The line context
	 * @since jEdit 4.1pre1
	 */
	public void handleToken(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		Chunk chunk = (Chunk)createToken(id,offset,length,context);
		if(chunk != null)
		{
			chunk.init(seg,expander,x,styles,
				fontRenderContext,
				context.rules.getDefault());

			if(out.size() == 0)
			{
				addToken(chunk,context);
				out.add(chunk);
			}
			else
			{
				if(id == Token.WHITESPACE && x + chunk.width > wrapMargin)
				{
					if(!addToken(chunk,context))
					{
						// re-init widened chunk
						((Chunk)lastToken).init(seg,expander,x,styles,
							fontRenderContext,
							context.rules.getDefault());
					}

					Chunk blankSpace = new Chunk(0.0f,offset + length,
						getParserRuleSet(context));

					firstToken = lastToken = blankSpace;
					x = 0.0f;

					out.add(blankSpace);
				}
				else
				{
					if(!addToken(chunk,context))
					{
						// re-init widened chunk
						((Chunk)lastToken).init(seg,expander,x,styles,
							fontRenderContext,
							context.rules.getDefault());
					}
					x += chunk.width;
				}
			}
		}
	} //}}}

	//{{{ Private members
	private ArrayList out;
	private float wrapMargin;
	private float endX;
	private Token end;
	//}}}
}
