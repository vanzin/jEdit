/*
 * OpenBracketIndentRule.java
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
import org.gjt.sp.jedit.TextUtilities;

public class OpenBracketIndentRule extends BracketIndentRule
{
	//{{{ OpenBracketIndentRule constructor
	public OpenBracketIndentRule(char openBracket, boolean aligned)
	{
		super(openBracket,
			TextUtilities.getComplementaryBracket(openBracket,
			new boolean[1]));
		this.aligned = aligned;
	} //}}}

	//{{{ apply() method
	public void apply(Buffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List indentActions)
	{
		int prevOpenBracketCount = getOpenBracketCount(buffer,prevLineIndex);
		if(prevOpenBracketCount != 0)
		{
			if(indentActions.contains(new IndentAction.Collapse()))
				indentActions.add(new IndentAction.Reset());
			indentActions.add(new IndentAction.Increase(prevOpenBracketCount));
		}
		else if(getOpenBracketCount(buffer,thisLineIndex) != 0)
		{
			if(indentActions.contains(new IndentAction.Collapse()))
				indentActions.add(new IndentAction.Reset());
		}
	} //}}}

	//{{{ getOpenBracketCount() method
	private int getOpenBracketCount(Buffer buffer, int line)
	{
		if(line == -1)
			return 0;
		else
			return getBrackets(buffer.getLineText(line)).openCount;
	} //}}}
	
	private boolean aligned;
}
