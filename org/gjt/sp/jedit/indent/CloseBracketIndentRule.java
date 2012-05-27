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
import org.gjt.sp.util.StandardUtilities;

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

		CharSequence lineText = buffer.getLineSegment(index);
		int offset;
		for (offset = lineText.length() - 1; offset >= 0; offset--)
		{
			if (lineText.charAt(offset) == closeBracket)
				break;
		}
		if(offset == -1)
			return;

		int closeCount = getBrackets(buffer, index).closeCount;
		if(closeCount != 0)
		{
			AlignBracket alignBracket
				= new AlignBracket(buffer,index,offset);
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
			int openLine = alignBracket.getOpenBracketLine();
			if(openLine != -1)
			{
				int column = alignBracket.getOpenBracketColumn();
				alignBracket.setExtraIndent(
					getBrackets(buffer, openLine,
						0, column).openCount);
			}

			indentActions.add(alignBracket);
		}
	} //}}}

	private boolean aligned;

	//{{{ AlignBracket class
	private static class AlignBracket implements IndentAction
	{
		private int line, offset;
		private int openBracketLine;
		private int openBracketColumn;
		private CharSequence openBracketLineText;
		private int extraIndent;

		public AlignBracket(JEditBuffer buffer, int line, int offset)
		{
			this.line = line;
			this.offset = offset;

			int openBracketIndex = TextUtilities.findMatchingBracket(
				buffer,this.line,this.offset);
			if(openBracketIndex == -1)
				openBracketLine = -1;
			else
			{
				openBracketLine = buffer.getLineOfOffset(openBracketIndex);
				openBracketColumn = openBracketIndex -
					buffer.getLineStartOffset(openBracketLine);
				openBracketLineText = buffer.getLineSegment(openBracketLine);
			}
		}

		public int getExtraIndent()
		{
			return extraIndent;
		}

		public void setExtraIndent(int extraIndent)
		{
			this.extraIndent = extraIndent;
		}

		public int getOpenBracketColumn()
		{
			return openBracketColumn;
		}

		public int getOpenBracketLine()
		{
			return openBracketLine;
		}

		public int calculateIndent(JEditBuffer buffer, int line, int oldIndent,
			int newIndent)
		{
			if(openBracketLineText == null)
				return newIndent;
			else
			{
				return StandardUtilities.getLeadingWhiteSpaceWidth(
					openBracketLineText,buffer.getTabSize())
					+ (extraIndent * buffer.getIndentSize());
			}
		}

		public boolean keepChecking()
		{
			return false;
		}
	} //}}}
}
