/*
 * DefaultTokenHandler.java - Builds a linked list of Token objects
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

/**
 * An implementation of the <code>TokenHandler</code> interface that
 * builds a linked list of tokens.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.1pre1
 */
public class DefaultTokenHandler implements TokenHandler
{
	//{{{ reset() method
	/**
	 * Clears the list of tokens.
	 */
	public void reset()
	{
		lastToken = null;
	} //}}}

	//{{{ getFirstToken() method
	/**
	 * Returns the first syntax token.
	 * @since jEdit 4.1pre1
	 */
	public Token getFirstToken()
	{
		return firstToken;
	} //}}}

	//{{{ getLastToken() method
	/**
	 * Returns the last syntax token.
	 * @since jEdit 4.1pre1
	 */
	public Token getLastToken()
	{
		return lastToken;
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
		if(length == 0 && id != Token.END)
			return;
		else if(id == Token.WHITESPACE)
		{
			if(lastToken == null)
				id = Token.NULL;
			else
			{
				lastToken.length += length;
				return;
			}
		}

		if(firstToken == null)
		{
			firstToken = new Token(length,id,rules);
			lastToken = firstToken;
		}
		else if(lastToken == null)
		{
			lastToken = firstToken;
			firstToken.length = length;
			firstToken.id = id;
			firstToken.rules = rules;
		}
		else if(lastToken.id == id && lastToken.rules == rules)
		{
			lastToken.length += length;
		}
		else if(lastToken.next == null)
		{
			lastToken.next = new Token(length,id,rules);
			lastToken.next.prev = lastToken;
			lastToken = lastToken.next;
		}
		else
		{
			lastToken = lastToken.next;
			lastToken.length = length;
			lastToken.id = id;
			lastToken.rules = rules;
		}
	} //}}}

	//{{{ Private members
	private Token firstToken, lastToken;
	//}}}
}
