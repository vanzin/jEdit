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

import org.gjt.sp.jedit.buffer.JEditBuffer;

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
		this.regexp = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
		this.collapse = collapse;
	} //}}}

	//{{{ apply() method
	public void apply(IndentContext ctx)
	{
		if(thisAction != null
			&& isMatch(ctx.getLineText(0)))
		{
			ctx.addAction(thisAction);
		}
		if(prevAction != null
			&& isMatch(ctx.getLineText(-1)))
		{
			ctx.addAction(prevAction);
			if (collapse)
				ctx.addAction(IndentAction.PrevCollapse);
		}
		if(prevPrevAction != null
			&& isMatch(ctx.getLineText(-2)))
		{
			ctx.addAction(prevPrevAction);
			if (collapse)
				ctx.addAction(IndentAction.PrevPrevCollapse);
		}
	} //}}}

	//{{{ isMatch() method
	public boolean isMatch(CharSequence line)
	{
		if (line == null)
			return false;
		Matcher m = regexp.matcher(line);
		return m.matches();
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + '[' + regexp + ']';
	} //}}}

	private IndentAction prevPrevAction, prevAction, thisAction;
	private Pattern regexp;
	private boolean collapse;
}
