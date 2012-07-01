/*
 * CloseBracketIndentRule.java
 * :tabSize=4:indentSize=4:noTabs=false:
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
				= new AlignBracket(buffer,index,offset,aligned);
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
			if(openLine != -1 && alignBracket.getExtraIndent() == 0)
			{
				int column = alignBracket.getOpenBracketColumn();
				alignBracket.setExtraIndent(
					getBrackets(buffer, openLine,
						0, column).openCount);
			}

			if (aligned)
				indentActions.add(alignBracket);
			else
				indentActions.add(0, alignBracket);
		}
	} //}}}

	private boolean aligned;

	//{{{ AlignBracket class
	private static class AlignBracket implements IndentAction
	{
		private final boolean aligned;
		private int line, offset;
		private int openBracketLine;
		private int openBracketColumn;
		private CharSequence openBracketLineText;
		private int extraIndent;

		public AlignBracket(JEditBuffer buffer, int line, int offset, boolean aligned)
		{
			this.aligned = aligned;
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

			if (aligned)
				findUnalignedBracket(buffer);
			else
				findAlignedBracket(buffer);
		}

		/**
		 * When matching aligned brackets, the actual start of
		 * the statement which defines the indent of the closing
		 * bracket may be on a different line, because of
		 * multi-line conditions. So, if the mode defines
		 * unaligned brackets, used those to try to find the
		 * actual start line from which to base the indent.
		 */
		private void findUnalignedBracket(JEditBuffer buffer)
		{
			int bracketIndex = -1;
			String unaligned = (String) buffer.getMode()
				.getProperty("unalignedCloseBrackets");
			if (unaligned == null)
				return;
			for (int i = 0; i < unaligned.length(); i++)
			{
				char c = unaligned.charAt(i);
				int cIdx = StandardUtilities.lastIndexOf(openBracketLineText, c);
				bracketIndex = Math.max(bracketIndex, cIdx);
			}

			if (bracketIndex > -1)
			{
				int startIndex = TextUtilities.findMatchingBracket(
					buffer, openBracketLine, bracketIndex);
				if (startIndex > -1)
				{
					openBracketLine = buffer.getLineOfOffset(startIndex);
					openBracketLineText = buffer.getLineSegment(openBracketLine);
				}
			}
		}

		/**
		 * When matching unaligned brackets, we may need to
		 * increase the indent the following line if there is no
		 * aligned bracket. Think of conditional constructs in
		 * C/Java where the conditional block is not enclosed in
		 * curly braces.
		 */
		private void findAlignedBracket(JEditBuffer buffer)
		{
			String aligned = (String) buffer.getMode()
				.getProperty("indentOpenBrackets");
			if (aligned == null)
				return;

			CharSequence lineText = buffer.getLineSegment(line);
			for (int i = 0; i < aligned.length(); i++)
			{
				char c = aligned.charAt(i);
				int cIdx = StandardUtilities.lastIndexOf(lineText, c);
				if (cIdx > -1)
				{
					return;
				}
			}
			extraIndent = 1;
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
			else if (aligned)
			{
				return StandardUtilities.getLeadingWhiteSpaceWidth(
					openBracketLineText,buffer.getTabSize())
					+ (extraIndent * buffer.getIndentSize());
			}
			else
				return buffer.getCurrentIndentForLine(openBracketLine, null)
					+ (extraIndent * buffer.getIndentSize());
		}

		public boolean keepChecking()
		{
			return !aligned;
		}
	} //}}}
}
