/*
 * PatternSearchMatcher.java - Regular expression matcher
 * :noTabs=false:
 *
 * Copyright (C) 2006 Marcelo Vanzin
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gjt.sp.util.ReverseCharSequence;

/**
 * A regular expression string matcher using java.util.regex.
 * @see java.util.regex.Pattern
 *
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre5
 */
public class PatternSearchMatcher extends SearchMatcher
{
	//{{{ PatternSearchMatcher constructors
	/**
	 * Creates a new regular expression string matcher.
	 * @see java.util.regex.Pattern
	 * @param search the search pattern
	 * @param ignoreCase <code>true</code> if you want to ignore case
	 * @since jEdit 4.3pre5
	 */
	public PatternSearchMatcher(String search, boolean ignoreCase)
	{
		pattern = search;
		flags = getFlag(ignoreCase);
	}

	/**
	 * Creates a new regular expression string matcher.
	 * @see java.util.regex.Pattern
	 * @param re the compiled regex
	 * @param ignoreCase <code>true</code> if you want to ignore case
	 * @param wholeWord <code>true</code> to search for whole word only
	 * @since jEdit 4.5pre1
	 */
	public PatternSearchMatcher(Pattern re, boolean ignoreCase, boolean wholeWord)
	{
		this(re.pattern(), ignoreCase);
		this.re = re;
		this.wholeWord = wholeWord;
	}
	
	/**
	 * Creates a new regular expression already compiled.
	 * @see java.util.regex.Pattern
	 * @param re the compiled regex
	 * @param ignoreCase <code>true</code> if you want to ignore case
	 * @since jEdit 4.3pre13
	 */
	public PatternSearchMatcher(Pattern re, boolean ignoreCase)
	{
		this(re, ignoreCase, false);
	} //}}}

	//{{{ nextMatch() method
	/**
	 * {@inheritDoc}
	 * <p>Reverse regex search is done by searching from the beginning to
	 * just prior to the current match, so will be inefficient for large
	 * buffers.</p>
	 */
	@Override
	public SearchMatcher.Match nextMatch(CharSequence text, boolean start,
		boolean end, boolean firstTime, boolean reverse)
	{
		// "For the mean time, there is no way to automatically generate a sexeger"
		//
		// http://japhy.perlmonk.org/sexeger/sexeger.html
		//
		// So ... for reverse regex searches we will search 
		// the string in the forward direction and 
		// return the last match.
		
		// Since we search the String in the forward direction,
		// (even for reverse searches) un-reverse the ReverseCharSequence.
		if (text instanceof ReverseCharSequence)
			text = ((ReverseCharSequence)text).baseSequence();

		if (re == null)
			re = Pattern.compile(pattern, flags);

		// if the pattern begins with "^", avoid spurious match at the
		// start of input sequence which is not a start of line.
		int matchStart = 0;
		if (!start && re.pattern().charAt(0) == '^')
		{
			Matcher sol = Pattern.compile("^", flags).matcher(text);
			// Ignore the first match since it is not a start of line.
			sol.find();
			// If the second match is not found, the real pattern also
			// can't match.
			if (!sol.find())
				return null;
			// Skip the text to the second match, which can be the first
			// match for the real pattern.
			matchStart = sol.start();
		}

		Matcher match = re.matcher(text);
		if (!match.find(matchStart))
			return null;

		// Special care for zero width matches. Without this care,
		// the caller will fall into an infinite loop, for non-reverse
		// search.
		if (!reverse && !firstTime && match.start() == 0 && match.end() == 0)
		{
			if (!match.find())
				return null;
		}

		Match previous = null;
		while (true)
		{
			// if we're not at the end of the buffer and we
			// match the end of the text, and the pattern ends with a "$",
			// ignore the match.
			// The match at the end the buffer which immediately follows
			// the final newline is also ignored because it is generally
			// not expected as an EOL.
			if ((!end || (text.charAt(text.length() - 1) == '\n'))
				&& match.end() == text.length()
				&& pattern.charAt(pattern.length() - 1) == '$')
			{
				if (previous != null)
				{
					returnValue.start = previous.start;
					returnValue.end = previous.end;
					returnValue.substitutions = previous.substitutions;
					break;
				}
				else
				{
					return null;
				}
			}

			returnValue.substitutions = new String[match.groupCount() + 1];
			for(int i = 0; i < returnValue.substitutions.length; i++)
			{
				returnValue.substitutions[i] = match.group(i);
			}
	
			int _start = match.start();
			int _end = match.end();
	
			returnValue.start = _start;
			returnValue.end = _end;

			if (wholeWord && !isWholeWord(text, _start, _end))
			{
				if (!match.find())
					return null;
				continue;
			}

			// For non-reversed searches, we break immediately
			// to return the first match.  For reversed searches,
			// we continue until no more matches are found
			if (!reverse || !match.find())
			{
				// For reverse search, check for zero width match at
				// the end of text.
				if (reverse && !firstTime && returnValue.start == text.length()
					&& returnValue.end == text.length())
				{
					if (previous != null)
					{
						returnValue.start = previous.start;
						returnValue.end = previous.end;
						returnValue.substitutions = previous.substitutions;
					}
					else
					{
						return null;
					}
				}
				break;
			}
			// Save the result for reverse zero width match.
			if (previous == null)
			{
				previous = new Match();
			}
			previous.start = returnValue.start;
			previous.end = returnValue.end;
			previous.substitutions = returnValue.substitutions;
		}

		if (reverse)
		{
			// The caller assumes we are searching a reversed
			// CharSegment, so we need to reverse the indices
			// before returning
			int len = returnValue.end - returnValue.start;
			returnValue.start = text.length() - returnValue.end;
			returnValue.end = returnValue.start + len;
		}

		return returnValue;
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		boolean ignoreCase = (flags & Pattern.CASE_INSENSITIVE) != 0;
		return "PatternSearchMatcher[" + pattern + ',' + ignoreCase + ']';
	} //}}}
	
	static int getFlag(boolean ignoreCase)
	{
		int flags = Pattern.MULTILINE;
		if (ignoreCase)
			flags |= Pattern.CASE_INSENSITIVE;
		return flags;
	}

	//{{{ Private members
	private int flags;
	private Pattern	re;
	private final String pattern;
	//}}}
}

