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
