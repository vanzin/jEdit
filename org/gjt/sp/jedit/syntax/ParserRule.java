/*
 * ParserRule.java - Sequence match rule for the token marker
 * Copyright (C) 1999 mike dillon
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
 * A parser rule.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRule
{
	// public members
	public final char[] searchChars;
	public final int[] sequenceLengths;
	public final int action;
	public final byte token;
	public ParserRule next;

	// package-private members
	ParserRule(char[] searchChars, int[] sequenceLengths, int action, byte token)
	{
		this.searchChars = searchChars;
		this.sequenceLengths = sequenceLengths;
		this.action = action;
		this.token = token;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:02  spestov
 * Initial revision
 *
 * Revision 1.5  2000/04/09 10:41:26  sp
 * NO_WORD_BREAK SPANs fixed, action tokens removed
 *
 * Revision 1.4  2000/04/08 06:57:14  sp
 * Parser rules are now hashed; this dramatically speeds up tokenization
 *
 * Revision 1.3  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.2  2000/04/01 08:40:55  sp
 * Streamlined syntax highlighting, Perl mode rewritten in XML
 *
 */
