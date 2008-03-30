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

	public DeepIndentRule(char openChar, char closeChar)
	{
		this.openChar = openChar;
		this.closeChar = closeChar;
	}

	//{{{ apply() method
	public void apply(JEditBuffer buffer, int thisLineIndex,
			  int prevLineIndex, int prevPrevLineIndex,
			  List<IndentAction> indentActions)
	{
		if (prevLineIndex == -1)
			return;

		int lineIndex = prevLineIndex;
		int oldLineIndex = lineIndex;
		String lineText = buffer.getLineText(lineIndex);
		int searchPos = -1;
		while (true)
		{
			if (lineIndex != oldLineIndex)
			{
				lineText = buffer.getLineText(lineIndex);
				oldLineIndex = lineIndex;
			}
			Parens parens = new Parens(buffer, lineIndex, searchPos);
			if (parens.openOffset > parens.closeOffset)
			{
				// recalculate column (when using tabs instead of spaces)
				int indent = parens.openOffset + TextUtilities.tabsToSpaces(lineText, buffer.getTabSize()).length() - lineText.length();
				indentActions.add(new IndentAction.AlignParameter(indent));
				return;
			}

			// No parens on prev line
			if (parens.openOffset == -1 && parens.closeOffset == -1)
			{
				return;
			}
			int openParenOffset = TextUtilities.findMatchingBracket(buffer, lineIndex, parens.closeOffset);
			if (openParenOffset >= 0)
			{
				lineIndex = buffer.getLineOfOffset(openParenOffset);
				searchPos = openParenOffset - buffer.getLineStartOffset(lineIndex) - 1;
				if (searchPos < 0)
				 	break;
			}
			else
				break;
		}
	} //}}}


	/**
	 * A token filter that looks for the position of the open and
	 * close characters in the line being parsed. Characters inside
	 * literals and comments are ignored.
	 */
	private class Parens implements TokenHandler
	{
		int openOffset;
		int closeOffset;
		int searchPos;

		Parens(JEditBuffer b, int line, int pos)
		{
			this.openOffset = -1;
			this.closeOffset = -1;
			this.searchPos = pos;
			b.markTokens(line, this);
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
				for (int i = offset + length - 1;
				     i >= offset;
				     i--)
				{
					if (seg.array[seg.offset + i] == openChar)
					{
						openOffset = i;
					}
					else if (seg.array[seg.offset + i] == closeChar)
					{
						closeOffset = i;
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

