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

import gnu.regexp.*;
import java.util.List;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.Buffer;

public class RegexpIndentRule implements IndentRule
{
	//{{{ RegexpIndentRule constructor
	public RegexpIndentRule(String regexp, IndentAction prevPrev,
		IndentAction prev, IndentAction thisLine)
		throws REException
	{
		this.prevPrevAction = prevPrev;
		this.prevAction = prev;
		this.thisAction = thisLine;
		this.regexp = new RE(regexp,RE.REG_ICASE,
			RESearchMatcher.RE_SYNTAX_JEDIT);
	} //}}}

	//{{{ apply() method
	public void apply(Buffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List indentActions)
	{
		if(thisAction != null
			&& isMatch(buffer.getLineText(thisLineIndex)))
		{
			indentActions.add(thisAction);
			indentActions.add(new IndentAction.Collapse());
		}
		if(prevAction != null
			&& prevLineIndex != -1
			&& isMatch(buffer.getLineText(prevLineIndex)))
		{
			indentActions.add(prevAction);
			indentActions.add(new IndentAction.Collapse());
		}
		if(prevPrevAction != null
			&& prevPrevLineIndex != -1
			&& isMatch(buffer.getLineText(prevPrevLineIndex)))
		{
			indentActions.add(prevPrevAction);
			indentActions.add(new IndentAction.Collapse());
		}
	} //}}}

	//{{{ isMatch() method
	public boolean isMatch(String line)
	{
		return regexp.isMatch(line);
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[" + regexp + "]";
	} //}}}

	private IndentAction prevPrevAction, prevAction, thisAction;
	private RE regexp;
}
