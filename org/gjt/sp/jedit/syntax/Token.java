/*
 * Token.java - Syntax token
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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
 * A linked list of syntax tokens.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Token
{
	//{{{ stringToToken() method
	/**
	 * Converts a token type string to a token type constant.
	 * @param value The token type
	 * @since jEdit 4.1pre1
	 */
	public static byte stringToToken(String value)
	{
		value = value.intern();

		if (value == "NULL")
			return Token.NULL;
		else if (value == "COMMENT1")
			return Token.COMMENT1;
		else if (value == "COMMENT2")
			return Token.COMMENT2;
		else if (value == "LITERAL1")
			return Token.LITERAL1;
		else if (value == "LITERAL2")
			return Token.LITERAL2;
		else if (value == "LABEL")
			return Token.LABEL;
		else if (value == "KEYWORD1")
			return Token.KEYWORD1;
		else if (value == "KEYWORD2")
			return Token.KEYWORD2;
		else if (value == "KEYWORD3")
			return Token.KEYWORD3;
		else if (value == "FUNCTION")
			return Token.FUNCTION;
		else if (value == "MARKUP")
			return Token.MARKUP;
		else if (value == "OPERATOR")
			return Token.OPERATOR;
		else if (value == "DIGIT")
			return Token.DIGIT;
		else if (value == "INVALID")
			return Token.INVALID;
		else
			return -1;
	} //}}}

	//{{{ Token types
	public static final byte NULL = 0;
	public static final byte COMMENT1 = 1;
	public static final byte COMMENT2 = 2;
	public static final byte LITERAL1 = 3;
	public static final byte LITERAL2 = 4;
	public static final byte LABEL = 5;
	public static final byte KEYWORD1 = 6;
	public static final byte KEYWORD2 = 7;
	public static final byte KEYWORD3 = 8;
	public static final byte FUNCTION = 9;
	public static final byte MARKUP = 10;
	public static final byte OPERATOR = 11;
	public static final byte DIGIT = 12;
	public static final byte INVALID = 13; //}}}

	public static final byte ID_COUNT = 14;

	// Special:
	public static final byte WHITESPACE = 125;
	public static final byte TAB = 126;
	public static final byte END = 127;

	//{{{ Instance variables
	/**
	 * The length of this token.
	 */
	public int length;

	/**
	 * The id of this token.
	 */
	public byte id;

	/**
	 * The rule set of this token.
	 */
	public ParserRuleSet rules;

	/**
	 * The previous token in the linked list.
	 * @since jEdit 2.6pre1
	 */
	public Token prev;

	/**
	 * The next token in the linked list.
	 */
	public Token next;
	//}}}

	//{{{ Token constructor
	/**
	 * Creates a new token.
	 * @param length The length of the token
	 * @param id The id of the token
	 * @param rules The parser rule set that generated this token
	 */
	public Token(int length, byte id, ParserRuleSet rules)
	{
		this.length = length;
		this.id = id;
		this.rules = rules;
	} //}}}

	//{{{ toString() method
	/**
	 * Returns a string representation of this token.
	 */
	public String toString()
	{
		return "[id=" + id + ",length=" + length + "]";
	} //}}}
}
