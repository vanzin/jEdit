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
	public IndentAction apply(Buffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex)
	{
		if(aligned && isMatch(buffer.getLineText(thisLineIndex)))
			return new IndentAction.Reset();
		else if(prevLineIndex != -1 && isMatch(
			buffer.getLineText(prevLineIndex)))
			return new IndentAction.Increase();
		else
			return null;
	} //}}}

	//{{{ isMatch() method
	public boolean isMatch(String line)
	{
		return getBrackets(line).openCount != 0;
	} //}}}
	
	private boolean aligned;
}
