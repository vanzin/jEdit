/*
 * RegexpIndentRule.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov
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

package org.gjt.sp.jedit.indent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.Segment;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class RegexpIndentRule implements IndentRule
{
	//{{{ RegexpIndentRule constructor
	/**
	 * @param collapse If true, then if the next indent rule is
	 * an opening bracket, this rule will not increase indent.
	 */
	public RegexpIndentRule(String regexp, IndentAction prevPrev,
		IndentAction prev, IndentAction thisLine, boolean collapse)
	throws PatternSyntaxException
	{
		prevPrevAction = prevPrev;
		prevAction = prev;
		thisAction = thisLine;
		this.regexp = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE );
		this.collapse = collapse;
	} //}}}

	//{{{ apply() method
	public void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List<IndentAction> indentActions)
	{
		if(thisAction != null
			&& lineMatches(buffer, thisLineIndex))
		{
			indentActions.add(thisAction);
		}
		if(prevAction != null
			&& prevLineIndex != -1
			&& lineMatches(buffer, prevLineIndex))
		{
			indentActions.add(prevAction);
			if (collapse)
				indentActions.add(IndentAction.PrevCollapse);
		}
		if(prevPrevAction != null
			&& prevPrevLineIndex != -1
			&& lineMatches(buffer, prevPrevLineIndex))
		{
			indentActions.add(prevPrevAction);
			if (collapse)
				indentActions.add(IndentAction.PrevPrevCollapse);
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + '[' + regexp + ']';
	} //}}}

	private IndentAction prevPrevAction, prevAction, thisAction;
	private Pattern regexp;
	private boolean collapse;

	//{{{ class TokenFilter
	/**
	 * A filter which removes non syntactic characters in comments
	 * or literals which might confuse regexp matchings for indent.
	 */
	private static class TokenFilter implements TokenHandler
	{
		public StringBuilder result;

		public TokenFilter(int originalLength)
		{
			result = new StringBuilder(originalLength);
		}

		public void handleToken(Segment seg
			, byte id, int offset, int length
			, TokenMarker.LineContext context)
		{
			// Avoid replacing an empty token into a non empty
			// string.
			if (length <= 0)
			{
				return;
			}
			
			switch (id)
			{
			case Token.COMMENT1:
			case Token.COMMENT2:
			case Token.COMMENT3:
			case Token.COMMENT4:
				// Replace any comments to a white space
				// so that they are simply ignored.
				result.append(' ');
				break;
			case Token.LITERAL1:
			case Token.LITERAL2:
			case Token.LITERAL3:
			case Token.LITERAL4:
				// Replace any literals to a '0' which means
				// a simple integer literal in most programming
				// languages.
				result.append('0');
				break;
			default:
				result.append(seg.array
					, seg.offset + offset
					, length);
				break;
			}
		}

		public void setLineContext(TokenMarker.LineContext lineContext)
		{
		}
	} //}}}

	//{{{ lineMatches() method
	private boolean lineMatches(JEditBuffer buffer, int lineIndex)
	{
		TokenFilter filter
			= new TokenFilter(buffer.getLineLength(lineIndex));
		buffer.markTokens(lineIndex, filter);
		return regexp.matcher(filter.result).matches();
	} //}}}
}
