/*
 * ParserRuleFactory.java - Factory object for creating ParserRules
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
 * Creates parser rules.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRuleFactory
{
	// public members
	public static final ParserRule createSpanRule(String begin, String end,
		byte id, boolean noLineBreak, boolean atLineStart,
		boolean excludeMatch, boolean noWordBreak)
	{
		int ruleAction = TokenMarker.SPAN |
			((noLineBreak) ? TokenMarker.NO_LINE_BREAK : 0) |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0) |
			((noWordBreak) ? TokenMarker.NO_WORD_BREAK : 0);

		String[] strings = new String[2];
		strings[0] = begin;
		strings[1] = end;

		int[] ruleSeqLengths = getStringLengthArray(strings);
		char[] ruleChars = getCharArray(strings, ruleSeqLengths);

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createDelegateSpanRule(String begin, String end,
		String delegateSet, byte id, boolean noLineBreak, boolean atLineStart,
		boolean excludeMatch, boolean noWordBreak)
	{
		int ruleAction = TokenMarker.SPAN |
			TokenMarker.DELEGATE |
			((noLineBreak) ? TokenMarker.NO_LINE_BREAK : 0) |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0) |
			((noWordBreak) ? TokenMarker.NO_WORD_BREAK : 0);

		String[] strings = new String[3];
		strings[0] = begin;
		strings[1] = end;
		strings[2] = delegateSet;

		int[] ruleSeqLengths = getStringLengthArray(strings);
		char[] ruleChars = getCharArray(strings, ruleSeqLengths);

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createEOLSpanRule(String seq, byte id,
		boolean atLineStart, boolean excludeMatch)
	{
		int ruleAction = TokenMarker.EOL_SPAN |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0);

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createMarkPreviousRule(String seq, byte id,
		boolean atLineStart, boolean excludeMatch)
	{
		int ruleAction = TokenMarker.MARK_PREVIOUS |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0);

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createMarkFollowingRule(String seq, byte id,
		boolean atLineStart, boolean excludeMatch)
	{
		int ruleAction = TokenMarker.MARK_FOLLOWING |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0);

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createSequenceRule(String seq, byte id, boolean atLineStart)
	{
		int ruleAction = ((atLineStart) ? TokenMarker.AT_LINE_START : 0);

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, id);
	}

	public static final ParserRule createEscapeRule(String seq)
	{
		int ruleAction = TokenMarker.IS_ESCAPE;

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, Token.NULL);
	}

	public static final ParserRule createWhitespaceRule(String seq)
	{
		int ruleAction = TokenMarker.WHITESPACE;

		String[] strings = new String[1];
		strings[0] = seq;

		int[] ruleSeqLengths = new int[1];
		char[] ruleChars;
		if (seq != null)
		{
			ruleSeqLengths[0] = seq.length();
			ruleChars = seq.toCharArray();
		}
		else
		{
			ruleChars = new char[0];
		}

		return new ParserRule(ruleChars, ruleSeqLengths, ruleAction, Token.NULL);
	}

	// private members
	private static char[] getCharArray(String[] strings, int[] lengthArray)
	{
		if (lengthArray == null || lengthArray.length == 0) return new char[0];

		char[] chars;
	
		int charArrayLength = 0;

		for (int i = 0; i < lengthArray.length; i++)
		{
			charArrayLength += lengthArray[i];
		}

		chars = new char[charArrayLength];

		int copyOffset = 0;

		for (int i = 0; i < strings.length; i++)
		{
			if (strings[i] != null)
			{
				System.arraycopy(strings[i].toCharArray(),0,chars,copyOffset,lengthArray[i]);
				copyOffset += lengthArray[i];
			}
		}

		return chars;
	}

	private static int[] getStringLengthArray(String[] strings)
	{
		int[] stringLengthArray;

		if (strings == null) return new int[0];

		stringLengthArray = new int[strings.length];

		for (int i = 0; i < strings.length; i++)
		{
			if (strings[i] != null)
			{
				stringLengthArray[i] = strings[i].length();
			}
		}

		return stringLengthArray;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:02  spestov
 * Initial revision
 *
 * Revision 1.4  2000/04/09 10:41:26  sp
 * NO_WORD_BREAK SPANs fixed, action tokens removed
 *
 * Revision 1.3  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.2  2000/04/01 08:40:55  sp
 * Streamlined syntax highlighting, Perl mode rewritten in XML
 *
 */
