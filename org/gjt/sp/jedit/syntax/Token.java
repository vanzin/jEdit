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
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:03  spestov
 * Initial revision
 *
 * Revision 1.18  2001/05/13 07:21:27  sp
 * more stuff
 *
 * Revision 1.17  2000/07/26 07:48:45  sp
 * stuff
 *
 * Revision 1.16  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.15  2000/04/08 02:39:33  sp
 * New Token.MARKUP type, remove Token.{CONSTANT,VARIABLE,DATATYPE}
 *
 * Revision 1.14  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.13  2000/04/06 13:09:46  sp
 * More token types added
 *
 * Revision 1.12  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.11  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.10  1999/04/22 06:03:26  sp
 * Syntax colorizing change
 *
 * Revision 1.9  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.8  1999/04/01 04:13:00  sp
 * Bug fixing for 1.5final
 *
 * Revision 1.7  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.6  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
