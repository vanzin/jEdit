/*
 * BoyerMooreSearchMatcher.java - Literal pattern String matcher utilizing the
 *         Boyer-Moore algorithm
 * Copyright (C) 1999, 2000 mike dillon
 * Portions copyright (C) 2001 Tom Locke
 * Portions copyright (C) 2001 Slava Pestov
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

package org.gjt.sp.jedit.search;

import bsh.NameSpace;
import javax.swing.text.Segment;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.util.Log;

public class BoyerMooreSearchMatcher implements SearchMatcher
{
	/**
	 * Creates a new string literal matcher.
	 */
	public BoyerMooreSearchMatcher(String pattern, String replace,
		boolean ignoreCase, boolean reverseSearch,
		boolean beanshell, String replaceMethod)
	{
		if (ignoreCase)
		{
			this.pattern = pattern.toUpperCase().toCharArray();
		}
		else
		{
			this.pattern = pattern.toCharArray();
		}

		if (reverseSearch)
		{
			char[] tmp = new char[this.pattern.length];
			for (int i = 0; i < tmp.length; i++)
			{
				tmp[i] = this.pattern[this.pattern.length - (i + 1)];
			}
			this.pattern = tmp;
		}

		this.replace = replace;
		this.ignoreCase = ignoreCase;
		this.reverseSearch = reverseSearch;
		this.beanshell = beanshell;

		if(beanshell)
		{
			this.replaceMethod = replaceMethod;
			replaceNS = new NameSpace(BeanShell.getNameSpace(),
				"search and replace");
		}

		generateSkipArray();
		generateSuffixArray();
	}

	/**
	 * Returns the offset of the first match of the specified text
	 * within this matcher.
	 * @param text The text to search in
	 * @return an array where the first element is the start offset
	 * of the match, and the second element is the end offset of
	 * the match
	 */
	public int[] nextMatch(Segment text)
	{
		int pos = match(text.array, text.offset, text.offset + text.count);

		if (pos == -1)
		{
			return null;
		}
		else
		{
			return new int[] { pos - text.offset, pos + pattern.length
				- text.offset };
		}
	}

	/**
	 * Returns the specified text, with any substitution specified
	 * within this matcher performed.
	 * @param text The text
	 */
	public String substitute(String text) throws Exception
	{
		if(beanshell)
		{
			replaceNS.setVariable("_0",text);
			Object obj = BeanShell.runCachedBlock(replaceMethod,
				null,replaceNS);
			if(obj == null)
				return null;
			else
				return obj.toString();
		}
		else
			return replace;
	}

	/*
	 *  a good introduction to the Boyer-Moore fast string matching
	 *  algorithm may be found on Moore's website at:
	 *
	 *   http://www.cs.utexas.edu/users/moore/best-ideas/string-searching/
	 *
	 */
	public int match(char[] text, int offset, int length)
	{
		// position variable for pattern start
		int anchor = reverseSearch ? length - 1 : offset;

		// position variable for pattern test position
		int pos;

		// last possible start position of a match with this pattern;
		// this is negative if the pattern is longer than the text
		// causing the search loop below to immediately fail
		int last_anchor = reverseSearch
			? offset + pattern.length - 1
			: length - pattern.length;

		// each time the pattern is checked, we start this many
		// characters ahead of 'anchor'
		int pattern_end = pattern.length - 1;

		char ch = 0;

		int bad_char;
		int good_suffix;

		// the search works by starting the anchor (first character
		// of the pattern) at the initial offset. as long as the
		// anchor is far enough from the enough of the text for the
		// pattern to match, and until the pattern matches, we
		// compare the pattern to the text from the last character
		// to the first character in reverse order. where a character
		// in the pattern mismatches, we use the two heuristics
		// based on the mismatch character and its position in the
		// pattern to determine the furthest we can move the anchor
		// without missing any potential pattern matches.
SEARCH:
		while (reverseSearch ? anchor >= last_anchor : anchor <= last_anchor)
		{
			for (pos = pattern_end; pos >= 0; --pos)
			{
				int idx = reverseSearch ? anchor - pos : anchor + pos;
				ch = ignoreCase
					? Character.toUpperCase(text[idx])
					: text[idx];

				// pattern test
				if (ch != pattern[pos])
				{
					// character mismatch, determine how many characters to skip

					// heuristic #1
					bad_char = pos - skip[getSkipIndex(ch)];

					// heuristic #2
					good_suffix = suffix[pos];

					// skip the greater of the two distances provided by the
					// heuristics
					int skip = (bad_char > good_suffix) ? bad_char : good_suffix;
					anchor += reverseSearch ? -skip : skip;

					// go back to the while loop
					continue SEARCH;
				}
			}

			// MATCH: return the position of its first character
			return (reverseSearch ? anchor - pattern_end : anchor);
		}

		// MISMATCH: return -1 as defined by API
		return -1;
	}

	// private members
	private char[] pattern;
	private String replace;
	private boolean ignoreCase;
	private boolean reverseSearch;
	private boolean beanshell;
	private String replaceMethod;
	private NameSpace replaceNS;

	// Boyer-Moore member fields
	private int[] skip;
	private int[] suffix;

	// Boyer-Moore helper methods

	/*
	 *  the 'skip' array is used to determine for each index in the
	 *  hashed alphabet how many characters can be skipped if
	 *  a mismatch occurs on a characater hashing to that index.
	 */
	private void generateSkipArray()
	{
		// initialize the skip array to all zeros
		skip = new int[256];

		// leave the table cleanly-initialized for an empty pattern
		if (pattern.length == 0) return;

		int pos = 0;

		do
		{
			skip[getSkipIndex(pattern[pos])] = pos;
		}
		while (++pos < pattern.length);
	}

	/*
	 *  to avoid our skip table having a length of 2 ^ 16, we hash each
	 *  character of the input into a character in the alphabet [\x00-\xFF]
	 *  using the lower 8 bits of the character's value (resulting in
	 *  a more reasonable skip table of length 2 ^ 8).
	 *
	 *  the result of this is that more than one character can hash to the
	 *  same index, but since the skip table encodes the position of
	 *  occurence of the character furthest into the string with a particular
	 *  index (whether or not it is the only character with that index), an
	 *  index collision only means that that this heuristic will give a
	 *  sub-optimal skip (i.e. a complete skip table could use the differences
	 *  between colliding characters to maximal effect, at the expense of
	 *  building a table that is over 2 orders of magnitude larger and very
	 *  sparse).
	 */
	private static final int getSkipIndex(char ch)
	{
		return ((int) ch) & 0x000000FF;
	}

	/*
	 *  XXX: hairy code that is basically just a functional(?) port of some
	 *  other code i barely understood
	 */
	private void generateSuffixArray()
	{
		int m = pattern.length;

		int j = m + 1;

		suffix = new int[j];
		int[] tmp = new int[j];
		tmp[m] = j;

		for (int i = m; i > 0; --i)
		{
			while (j <= m && pattern[i - 1] != pattern[j - 1])
			{
				if (suffix[j] == 0)
				{
					suffix[j] = j - i;
				}

				j = tmp[j];
			}

			tmp[i - 1] = --j;
		}

		int k = tmp[0];

		for (j = 0; j <= m; j++)
		{
			// the code above builds a 1-indexed suffix array,
			// but we shift it to be 0-indexed, ignoring the
			// original 0-th element
			if (j > 0)
			{
				suffix[j - 1] = (suffix[j] == 0) ? k : suffix[j];
			}

			if (j == k)
			{
				k = tmp[k];
			}
		}
	}
}
