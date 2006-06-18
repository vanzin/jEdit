/*
 * SearchMatcher.java - Abstract string matcher interface
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2001, 2002 Slava Pestov
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

/**
 * An abstract class for matching strings.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class SearchMatcher
{
	public SearchMatcher()
	{
		returnValue = new Match();
	}

	/**
	 * Returns the offset of the first match of the specified text
	 * within this matcher.
	 * @param text The text to search in
	 * @param start True if the start of the segment is the beginning of the
	 * buffer
	 * @param end True if the end of the segment is the end of the buffer
	 * @param firstTime If false and the search string matched at the start
	 * offset with length zero, automatically find next match
	 * @param reverse If true, searching will be performed in a backward
	 * direction.
	 * @return an array where the first element is the start offset
	 * of the match, and the second element is the end offset of
	 * the match
	 * @since jEdit 4.3pre5
	 */
	public abstract Match nextMatch(CharSequence text, boolean start,
		boolean end, boolean firstTime, boolean reverse);

	protected Match returnValue;

	//{{{ Match class
	public static class Match
	{
		public int start;
		public int end;
		public String[] substitutions;
	} //}}}
}
