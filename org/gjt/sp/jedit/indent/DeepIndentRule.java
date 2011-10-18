/*
 * DeepIndentRule.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Matthieu Casanova
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

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;

import java.util.List;
import java.util.Stack;
import javax.swing.text.Segment;

/**
 * Deep indent rule.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public class DeepIndentRule implements IndentRule
{
	private final char openChar;
	private final char closeChar;

	//{{{ DeepIndentRule constructor
	public DeepIndentRule(char openChar, char closeChar)
	{
		this.openChar = openChar;
		this.closeChar = closeChar;
	} //}}}

	//{{{ apply() method
	public void apply(JEditBuffer buffer, int thisLineIndex,
			  int prevLineIndex, int prevPrevLineIndex,
			  List<IndentAction> indentActions)
	{
		if (prevLineIndex == -1)
			return;

		int lineIndex = prevLineIndex;
		int oldLineIndex = lineIndex;
		CharSequence lineText = buffer.getLineSegment(lineIndex);
		int searchPos = -1;
		while (true)
		{
			if (lineIndex != oldLineIndex)
			{
				lineText = buffer.getLineSegment(lineIndex);
				oldLineIndex = lineIndex;
			}
			Parens parens = new Parens(buffer, lineIndex, searchPos);

			// No unmatched parens on prev line.
			if (parens.openOffset == -1 && parens.closeOffset == -1)
			{
				// Try prev-prev line if present.
				if (prevPrevLineIndex != -1) {
					searchPos = -1;
					lineIndex = prevPrevLineIndex;
					prevPrevLineIndex = -1;
					continue;
				}
				return;
			}

			// There's an unmatched open parenthesis - we want to
			// align according to its position.
			if (parens.closeOffset == -1)
			{
				// recalculate column (when using tabs instead of spaces)
				int indent = parens.openOffset + getIndent(lineText, buffer.getTabSize()) - lineText.length();
				indentActions.clear();
				indentActions.add(new IndentAction.AlignParameter(indent));
				return;
			}

			// There's an unmatched closed parenthesis - find the
			// matching parenthesis and start looking from there again.
			int openParenOffset = TextUtilities.findMatchingBracket(buffer, lineIndex, parens.closeOffset);
			if (openParenOffset >= 0)
			{
				// Avoid falling back to the prev-prev line in this case.
				prevPrevLineIndex = -1;
				lineIndex = buffer.getLineOfOffset(openParenOffset);
				searchPos = openParenOffset - buffer.getLineStartOffset(lineIndex) - 1;
				if (searchPos < 0)
					break;
			}
			else
				break;
		}
	} //}}}

	//{{{ getIndent() method
	/**
	 * Returns the length of the string as if it were indented with
	 * spaces instead of tabs.
	 */
	private int getIndent(CharSequence line, int tabSize)
	{
		int cnt = 0;
		for (int i = 0;  i < line.length(); i++)
		{
			if (line.charAt(i) == '\t')
			{
				cnt += tabSize;
			}
			else
			{
				if (!Character.isWhitespace(line.charAt(i)))
				{
					cnt += (line.length() - i);
					break;
				}
				cnt++;
			}
		}
		return cnt;
	} //}}}

	//{{{ Parens class
	/**
	 * A token filter that looks for the position of the open and
	 * close characters in the line being parsed. Characters inside
	 * literals and comments are ignored.
	 */
	private class Parens implements TokenHandler
	{
		int openOffset;
		int closeOffset;

		private int searchPos;
		private Stack<Integer> open;
		private Stack<Integer> close;

		Parens(JEditBuffer b, int line, int pos)
		{
			this.searchPos = pos;
			this.open = new Stack<Integer>();
			this.close = new Stack<Integer>();
			b.markTokens(line, this);
			openOffset = (open.isEmpty()) ? -1 : open.pop();
			closeOffset = (close.isEmpty()) ? -1 : close.pop();
		}

		public void handleToken(Segment seg,
					byte id,
					int offset,
					int length,
					TokenMarker.LineContext context)
		{
			if (length <= 0 ||
			    (searchPos != -1 && searchPos < offset))
			{
				return;
			}

			if (searchPos != -1 && offset + length > searchPos)
			{
				length = searchPos - offset + 1;
			}

			switch (id)
			{
			case Token.COMMENT1:
			case Token.COMMENT2:
			case Token.COMMENT3:
			case Token.COMMENT4:
			case Token.LITERAL1:
			case Token.LITERAL2:
			case Token.LITERAL3:
			case Token.LITERAL4:
				/* Ignore comments and literals. */
				break;
			default:
				for (int i = offset; i < offset + length; i++)
				{
					if (seg.array[seg.offset + i] == openChar)
					{
						if (open.isEmpty() && !close.isEmpty())
							close.pop();
						else
							open.push(i);
					}
					else if (seg.array[seg.offset + i] == closeChar)
					{
						if (close.isEmpty() && !open.isEmpty())
							open.pop();
						else
							close.push(i);
					}
				}
				break;
			}
		}

		public void setLineContext(TokenMarker.LineContext lineContext)
		{
			/* Do nothing. */
		}

		@Override
		public String toString()
		{
			return "Parens(" + openOffset + ',' + closeOffset + ')';
		}
	} //}}}
}

