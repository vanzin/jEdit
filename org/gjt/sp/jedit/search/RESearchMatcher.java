/*
 * RESearchMatcher.java - Regular expression matcher
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import gnu.regexp.*;
import javax.swing.text.Segment;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.MiscUtilities;

/**
 * A regular expression string matcher.
 * @author Slava Pestov
 * @version $Id$
 */
public class RESearchMatcher implements SearchMatcher
{
	/**
	 * Perl5 syntax with character classes enabled.
	 * @since jEdit 3.0pre5
	 */
	public static final RESyntax RE_SYNTAX_JEDIT
		= new RESyntax(RESyntax.RE_SYNTAX_PERL5)
		.set(RESyntax.RE_CHAR_CLASSES)
		.setLineSeparator("\n");

	/**
	 * Creates a new regular expression string matcher.
	 */
	public RESearchMatcher(String search, String replace,
		boolean ignoreCase, boolean beanshell,
		String replaceMethod) throws Exception
	{
		// gnu.regexp doesn't seem to support \n and \t in the replace
		// string, so implement it here
		this.replace = MiscUtilities.escapesToChars(replace);

		this.beanshell = beanshell;
		this.replaceMethod = replaceMethod;
		replaceNS = new NameSpace(BeanShell.getNameSpace(),"search and replace");

		re = new RE(search,(ignoreCase ? RE.REG_ICASE : 0)
			| RE.REG_MULTILINE,RE_SYNTAX_JEDIT);
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
		REMatch match = re.getMatch(new CharIndexedSegment(text,0));
		if(match == null)
			return null;

		int start = match.getStartIndex();
		int end = match.getEndIndex();
		if(start == end)
		{
			// searched for (), or ^, or $, or other 'empty' regexp
			return null;
		}

		int[] result = { start, end };
		return result;
	}

	/**
	 * Returns the specified text, with any substitution specified
	 * within this matcher performed.
	 * @param text The text
	 */
	public String substitute(String text) throws Exception
	{
		REMatch match = re.getMatch(text);
		if(match == null)
			return null;

		if(beanshell)
		{
			int count = re.getNumSubs();
			for(int i = 0; i < count; i++)
				replaceNS.setVariable("_" + i,match.toString(i));

			Object obj = BeanShell.runCachedBlock(replaceMethod,
				null,replaceNS);
			if(obj == null)
				return null;
			else
				return obj.toString();
		}
		else
			return match.substituteInto(replace);
	}

	// private members
	private String replace;
	private RE re;
	private boolean beanshell;
	private String replaceMethod;
	private NameSpace replaceNS;
}
