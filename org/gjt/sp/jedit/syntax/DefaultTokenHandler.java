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
	public void init()
	{
		lastToken = firstToken = null;
	} //}}}

	//{{{ getTokens() method
	/**
	 * Returns the first syntax token.
	 * @since jEdit 4.1pre1
	 */
	public Token getFirstToken()
	{
		return firstToken;
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
		ParserRuleSet rules = getParserRuleSet(context);

		Token token = createToken(id,offset,length,rules);
		if(token != null)
			addToken(token,rules);
	} //}}}

	//{{{ Protected members
	protected Token firstToken, lastToken;

	//{{{ getParserRuleSet() method
	protected ParserRuleSet getParserRuleSet(TokenMarker.LineContext context)
	{
		while(context != null)
		{
			if(context.rules.getMode() != null)
				return context.rules;

			context = context.parent;
		}

		return null;
	} //}}}

	//{{{ createToken() method
	protected Token createToken(byte id, int offset, int length,
		ParserRuleSet rules)
	{
		return new Token(id,offset,length,rules);
	} //}}}

	//{{{ addToken() method
	/**
	 * @return False if the new token was merged with the last one; true
	 * otherwise.
	 */
	protected boolean addToken(Token token, ParserRuleSet rules)
	{
		if(firstToken == null)
		{
			firstToken = lastToken = token;
			return false;
		}
		else
		{
			if((lastToken.id == rules.getDefault()
				&& token.id == Token.WHITESPACE)
				|| (lastToken.id == token.id
				&& lastToken.rules == token.rules))
			{
				if(token.id != Token.TAB)
				{
					lastToken.length += token.length;
					return false;
				}
			}

			lastToken.next = token;
			lastToken = lastToken.next;
			return true;
		}
	} //}}}

	//}}}
}
