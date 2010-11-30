/*
 * TokenHandler.java - Token markers send tokens to implementations of
 * this class
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

import javax.swing.text.Segment;

/**
 * Token markers send tokens to implementations of this interface.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.1pre1
 */
public interface TokenHandler
{
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
		TokenMarker.LineContext context);

	/**
	 * The token handler can compare this object with the object
	 * previously given for this line to see if the token type at the end
	 * of the line has changed (meaning subsequent lines might need to be
	 * retokenized).
	 * @since jEdit 4.2pre6
	 */
	public void setLineContext(TokenMarker.LineContext lineContext);
}
