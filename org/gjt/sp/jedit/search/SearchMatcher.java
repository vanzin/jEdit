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

import org.gjt.sp.jedit.TextUtilities;

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
	 * @param start True if the start of the text is the beginning of a line
	 * @param end True if the end of the text is the end of a line
	 * @param firstTime If false and the search string matched at the start
	 * offset with length zero, automatically find next match
	 * @param reverse If true, searching will be performed in a backward
	 * direction.
	 * @return A {@link Match} object.
	 * @since jEdit 4.3pre5
	 */
	public abstract Match nextMatch(CharSequence text, boolean start,
		boolean end, boolean firstTime, boolean reverse);

	/**
	 * @param noWordSep the chars that are considered as word chars for this search
	 * @since jEdit 4.5pre1
	 */
	public void setNoWordSep(String noWordSep)
	{
		if (noWordSep == null)
			this.noWordSep = "_";
		else
			this.noWordSep = noWordSep;
	}

	/**
	 * Returns the noWordSep that should be used.
	 * This is used by the HyperSearchOperationNode that
	 * needs to remember this property since it can have
	 * to restore it.
	 * @return the noWordSep property
	 */
	String getNoWordSep()
	{
		return noWordSep;
	}

	/**
	 * Check if the result is a whole word
	 * @param text the full text search
	 * @param start the start match
	 * @param end the end match
	 * @return true if the word is a whole word
	 */
	protected boolean isWholeWord(CharSequence text, int start, int end)
	{
		if (start != 0)
		{
			char firstChar = text.charAt(start);
			char prevChar = text.charAt(start - 1);
			if (!isEndWord(firstChar, prevChar))
			{
				return false;
			}
		}
		if (end < text.length())
		{
			char lastChar = text.charAt(end - 1);
			char nextChar = text.charAt(end);
			if (!isEndWord(lastChar, nextChar))
			{
				return false;
			}
		}
		return true;
	}

	private boolean isEndWord(char current, char next)
	{
		int currentCharType = TextUtilities.getCharType(current, noWordSep);
		if (currentCharType != TextUtilities.WORD_CHAR)
			return true;

		int nextCharType = TextUtilities.getCharType(next, noWordSep);
		return nextCharType != TextUtilities.WORD_CHAR;
	}

	protected Match returnValue;
	/**
	 * true if this SearchMatcher search for whole words only.
	 */
	protected boolean wholeWord;
	/**
	 * This should contains the noWordSep property of the edit mode of your buffer.
	 * It contains a list of chars that should be considered as word chars
	 */
	protected String noWordSep;

	//{{{ Match class
	public static class Match
	{
		public int start;
		public int end;
		public String[] substitutions;

		@Override
		public String toString()
		{
			return "Match[" + start + ',' + end + ']';
		}
	} //}}}
}
