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
import java.awt.geom.*;
import java.awt.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

public abstract class DisplayTokenHandler extends DefaultTokenHandler
{
	//{{{ init() method
	public void init(Segment seg, SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander)
	{
		super.init();

		x = 0.0f;

		this.seg = seg;
		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;
	} //}}}

	//{{{ Protected members
	protected Segment seg;
	protected SyntaxStyle[] styles;
	protected FontRenderContext fontRenderContext;
	protected TabExpander expander;
	protected float x;

	//{{{ createToken() method
	protected Token createToken(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		if(id == Token.END)
		{
			if(lastToken != null)
			{
				Chunk lastChunk = (Chunk)lastToken;
				lastChunk.init(seg,expander,x,styles,
					fontRenderContext,
					context.rules.getDefault());
				x += lastChunk.width;
			}

			return null;
		}
		else
		{
			return new Chunk(id,offset,length,
				getParserRuleSet(context));
		}
	} //}}}

	//{{{ addToken() method
	protected boolean addToken(Token token, TokenMarker.LineContext context,
		boolean merge)
	{
		Chunk oldLastChunk = (Chunk)lastToken;
		if(super.addToken(token,context,
			merge && (oldLastChunk == null
			|| !oldLastChunk.inaccessable
			|| oldLastChunk.str != null)))
		{
			oldLastChunk.init(seg,expander,x,styles,
				fontRenderContext,context.rules.getDefault());
			x += oldLastChunk.width;
			return true;
		}
		else
			return false;
	} //}}}

	//}}}
}
