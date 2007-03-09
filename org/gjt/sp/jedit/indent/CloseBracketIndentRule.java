/*
 * CloseBracketIndentRule.java
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
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.TextUtilities;

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class CloseBracketIndentRule extends BracketIndentRule
{
	//{{{ CloseBracketIndentRule constructor
	public CloseBracketIndentRule(char closeBracket, boolean aligned)
	{
		super(TextUtilities.getComplementaryBracket(closeBracket,null),
			closeBracket);
		this.aligned = aligned;
	} //}}}

	//{{{ apply() method
	public void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List<IndentAction> indentActions)
	{
		int index;
		if(aligned)
			index = thisLineIndex;
		else
			index = prevLineIndex;

		if(index == -1)
			return;

		String line = buffer.getLineText(index);

		int offset = line.lastIndexOf(closeBracket);
		if(offset == -1)
			return;

		int closeCount = getBrackets(line).closeCount;
		if(closeCount != 0)
		{
			IndentAction.AlignBracket alignBracket
				= new IndentAction.AlignBracket(
				buffer,index,offset);
			/*
			Consider the following Common Lisp code (with one more opening
			bracket than closing):

			(defun emit-push-long (arg)
			  (cond ((eql arg 0)
			      (emit 'lconst_0))
			    ((eql arg 1)
			      (emit 'lconst_1)))

			even though we have a closing bracket match on line 3,
			the next line must be indented relative to the
			corresponding opening bracket from line 1.
			*/
			String openLine = alignBracket.getOpenBracketLine();
			int column = alignBracket.getOpenBracketColumn();
			if(openLine != null)
			{
				String leadingBrackets = openLine.substring(0,column);
				alignBracket.setExtraIndent(getBrackets(leadingBrackets)
					.openCount);
			}

			indentActions.add(alignBracket);
		}
	} //}}}

	//{{{ isMatch() method
	public boolean isMatch(String line)
	{
		return getBrackets(line).closeCount != 0;
	} //}}}

	private boolean aligned;
}
