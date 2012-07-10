/*
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2012 Marcelo Vanzin
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
import javax.swing.text.Segment;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.util.SegmentCharSequence;
import org.gjt.sp.util.StandardUtilities;

/**
 * Computes the indent for the line based on the previous statement.
 * <p>
 * Consider the following fragment of pseudo-Java code, with line
 * numbers:
 *
 * <blockquote><tt>
 *   1.   String thisIsStatement1;
 *   2.
 *   3.   String thisIsStatement2 =
 *   4.       initializerForStatement2;
 *   5.
 * </tt></blockquote>
 *
 * When calculating the indent for line 5, it should be aligned with
 * line 3, not line 4. Since jEdit's syntax parsing does not contain
 * enough semantic information to know where the previous statement
 * starts, this rule implements some basic parsing of the buffer to
 * try to figure that out.
 * <p>
 * It works like the following: if the previous line ends with a
 * statement terminator (e.g., line 4), the code will parse previous
 * lines in the buffer until it finds the end of a previous statement
 * (in the example above, line 1) or the start or end of a code block,
 * identified by the "indentOpenBrackets" and "indentCloseBrackets"
 * properties of the mode. The indent of the next line will then be the
 * indent level of the next non-empty line. or, in the example above,
 * line 3.
 * <p>
 * For this to work the mode should declare a property named
 * "statementSeparator" so that this rule can identify the end of
 * statements.
 *
 * @author Marcelo Vanzin
 * @version $Id$
 */
public class PreviousStatementIndentRule implements IndentRule
{

	public PreviousStatementIndentRule(String statementEnd,
									   String openBrackets,
									   String closeBrackets)
	{
		this.statementEnd = statementEnd;

		if (openBrackets != null && closeBrackets != null)
			this.brackets = openBrackets + closeBrackets;
		else
			this.brackets = null;
	}

	public void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List<IndentAction> indentActions)
	{
		if (prevLineIndex < 0)
			return;

		Scanner s = new Scanner(false);
		buffer.markTokens(prevLineIndex, s);
		if (!s.isFound())
			return;

		// We found the end of a statement. Start backtracking to find
		// the previous statement.
		s = new Scanner(true);
		int baseLine = prevLineIndex;
		for (int lineIndex = buffer.getPriorNonEmptyLine(prevLineIndex);
			 lineIndex >= 0;
			 lineIndex = buffer.getPriorNonEmptyLine(lineIndex))
		{
			buffer.markTokens(lineIndex, s);
			if (s.isFound())
			{
				break;
			}
			baseLine = lineIndex;
		}

		indentActions.add(new IndentAction.SetBaseIndent(baseLine));
	}

	private final String brackets;
	private final String statementEnd;

	private class Scanner implements TokenHandler
	{

		Scanner(boolean searchBrackets)
		{
			this.index = -1;
			this.searchBrackets = searchBrackets;
		}

		public void handleToken(Segment seg,
			byte id, int offset, int length,
			TokenMarker.LineContext context)
		{
			switch (id) {
			case Token.COMMENT1:
			case Token.COMMENT2:
			case Token.COMMENT3:
			case Token.COMMENT4:
			case Token.LITERAL1:
			case Token.LITERAL2:
			case Token.LITERAL3:
			case Token.LITERAL4:
				return;
			}

			CharSequence seq = new SegmentCharSequence(seg, offset, length);

			// Ignore whitespaces.
			boolean isEmpty = true;
			for (int i = index + 1; i < seq.length(); i++)
			{
				if (!Character.isWhitespace(seq.charAt(i)))
				{
					isEmpty = false;
					break;
				}
			}
			if (isEmpty)
				return;

			index = StandardUtilities.indexOf(seq, statementEnd);
			if (index >= 0)
			{
				// The statement end should be the last thing on the line.
				for (int i = index + 1; i < seq.length(); i++)
				{
					if (!Character.isWhitespace(seq.charAt(i)))
					{
						index = -1;
						break;
					}
				}
			}

			if (index == -1 && searchBrackets && brackets != null)
			{
				int idx2 = -1;
				for (int i = 0; idx2 == -1 && i < brackets.length(); i++)
				{
					char c = brackets.charAt(i);
					idx2 = Math.max(idx2, StandardUtilities.lastIndexOf(seq, c));
				}

				index = idx2;
			}
		}

		public void setLineContext(TokenMarker.LineContext lineContext)
		{
		}

		boolean isFound()
		{
			return index >= 0;
		}

		private final boolean searchBrackets;
		private int index;

	}
}

