/*
 * IndentRule.java
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

import org.gjt.sp.jedit.Buffer;

public abstract class IndentRule
{
	//{{{ IndentRule constructor
	public IndentRule(IndentAction prevPrev, IndentAction prev,
		IndentAction thisLine)
	{
		this.prevPrevAction = prevPrev;
		this.prevAction = prev;
		this.thisAction = thisLine;
	} //}}}

	//{{{ apply() method
	/**
	 * Apply the indent rule to this line, and return an indent action.
	 */
	public IndentAction apply(Buffer buffer, int line)
	{
		String thisLine = buffer.getLineText(line);
		String prev = (line == 0 ? null : buffer.getLineText(line - 1));
		String prevPrev = (line <= 1 ? null : buffer.getLineText(line - 2));
		return apply(buffer,thisLine,prev,prevPrev,line);
	} //}}}

	//{{{ apply() method
	public IndentAction apply(Buffer buffer, String thisLine,
		String prev, String prevPrev, int line)
	{
		if(isMatch(thisLine))
			return thisAction;
		else if(prev != null && isMatch(prev))
			return prevAction;
		else if(prevPrev != null && isMatch(prevPrev))
			return prevPrevAction;
		else
			return null;
	} //}}}

	public abstract boolean isMatch(String line);

	//{{{ Protected members
	protected IndentAction prevPrevAction, prevAction, thisAction;
	//}}}
}
