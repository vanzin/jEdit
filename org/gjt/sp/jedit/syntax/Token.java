/*
 * Token.java - Generic token
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
 * A linked list of tokens. Each token has four fields - a token
 * identifier, which can be mapped to a color or font style for
 * painting, a length value which is the length of the token in the
 * text, and pointers to the previous and next tokens in the list,
 * respectively.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Token
{
	public static int COUNT;
	public static int COUNT_GC;

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
	public static final byte INVALID = 13;

	public static final byte ID_COUNT = 14;

	public static final byte END = 127;

	/**
	 * The length of this token.
	 */
	public int length;

	/**
	 * The id of this token.
	 */
	public byte id;

	/**
	 * The previous token in the linked list.
	 * @since jEdit 2.6pre1
	 */
	public Token prev;

	/**
	 * The next token in the linked list.
	 */
	public Token next;

	/**
	 * Creates a new token.
	 * @param length The length of the token
	 * @param id The id of the token
	 */
	public Token(int length, byte id)
	{
		COUNT++;
		COUNT_GC++;
		this.length = length;
		this.id = id;
	}

	/**
	 * Returns a string representation of this token.
	 */
	public String toString()
	{
		return "[id=" + id + ",length=" + length + "]";
	}

	public void finalize()
	{
		COUNT_GC--;
	}
}
