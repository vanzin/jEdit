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
		TabExpander expander)
	{
		super.init();

		x = 0.0f;

		this.seg = seg;
		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;
	} //}}}

	//{{{ setMonospacedCharWidth() method
	public void setMonospacedCharWidth(float charWidth)
	{
		this.charWidth = charWidth;
	} //}}}

	//{{{ getChunks() method
	/**
	 * Returns the first chunk.
	 * @since jEdit 4.1pre1
	 */
	public Chunk getChunks()
	{
		return (Chunk)firstToken;
	} //}}}

	//{{{ Protected members
	protected Segment seg;
	protected SyntaxStyle[] styles;
	protected FontRenderContext fontRenderContext;
	protected TabExpander expander;
	protected float x;
	protected float charWidth;

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
