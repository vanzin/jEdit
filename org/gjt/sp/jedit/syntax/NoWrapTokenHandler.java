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

package org.gjt.sp.jedit.syntax;

//{{{ Imports
import javax.swing.text.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

public class NoWrapTokenHandler extends DisplayTokenHandler
{
	//{{{ getChunks() method
	/**
	 * Returns the first chunk.
	 * @since jEdit 4.1pre1
	 */
	public Chunk getChunks()
	{
		return (Chunk)firstToken;
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
		Token token = createToken(id,offset,length,context);
		if(token != null)
		{
			Token oldLastToken = lastToken;
			if(addToken(token,context,true))
			{
				Chunk oldLastChunk = (Chunk)oldLastToken;
				oldLastChunk.init(seg,expander,x,styles,
					fontRenderContext,context.rules.getDefault());
			}
		}
	} //}}}
}
