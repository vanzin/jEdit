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
	 * Creates a new regular expression already compiled.
	 * @see java.util.regex.Pattern
	 * @param re the compiled regex
	 * @param ignoreCase <code>true</code> if you want to ignore case
	 * @since jEdit 4.3pre13
	 */
	public PatternSearchMatcher(Pattern re, boolean ignoreCase)
	{
		this(re.pattern(), ignoreCase);
		this.re = re;
	} //}}}

	//{{{ nextMatch() method
	/**
	 * Returns the offset of the first match of the specified text
	 * within this matcher.
	 *
	 * @param text 		The text to search in
	 * @param start 	True if the start of the segment is the beginning
	 *			of the buffer
	 * @param end 		True if the end of the segment is the end of the
	 *			buffer
	 * @param firstTime 	If false and the search string matched at the
	 *			start offset with length zero, automatically
	 *			find next match
	 * @param reverse 	Unsupported for PatternSearchMatcher. Should
	 *			always be "false".
	 *
	 * @return A {@link SearchMatcher.Match} object.
	 * @since jEdit 4.3pre5
	 */
	public SearchMatcher.Match nextMatch(CharSequence text, boolean start,
		boolean end, boolean firstTime, boolean reverse)
	{
		if (re == null)
			re = Pattern.compile(pattern, flags);

		Matcher match = re.matcher(text);
		if (!match.find())
			return null;

		// if we're not at the start of the buffer, and the pattern
		// begins with "^" and matched the beginning of the region
		// being matched, ignore the match and try the next one.
		if (!start && match.start() == 0
			&& re.pattern().charAt(0) == '^' && !match.find())
			return null;

		// similarly, if we're not at the end of the buffer and we
		// match the end of the text, and the pattern ends with a "$",
		// return null.
		if (!end && match.end() == (text.length() - 1)
			&& pattern.charAt(pattern.length() - 1) == '$')
			return null;

		returnValue.substitutions = new String[match.groupCount() + 1];
		for(int i = 0; i < returnValue.substitutions.length; i++)
		{
			returnValue.substitutions[i] = match.group(i);
		}

		int _start = match.start();
		int _end = match.end();

		returnValue.start = _start;
		returnValue.end = _end;
		return returnValue;
	} //}}}

	//{{{ isMatchingEOL() method
	@Override
	public boolean isMatchingEOL()
	{
		return pattern.charAt(pattern.length() - 1) == '$';
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return "PatternSearchMatcher[" + pattern + ']';
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

