/*
 * BracketIndentRule.java
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

import javax.swing.text.Segment;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;

/**
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class BracketIndentRule implements IndentRule
{
	//{{{ BracketIndentRule constructor
	BracketIndentRule(char openBracket, char closeBracket)
	{
		this.openBracket = openBracket;
		this.closeBracket = closeBracket;
	} //}}}

	//{{{ Brackets class
	public static class Brackets
	{
		int openCount;
		int closeCount;
	} //}}}

	//{{{ getBrackets() method
	public Brackets getBrackets(JEditBuffer buffer, int lineIndex)
	{
		return getBrackets(buffer, lineIndex,
			0, buffer.getLineLength(lineIndex));
	} //}}}

	//{{{ getBrackets() method
	public Brackets getBrackets(JEditBuffer buffer, int lineIndex,
		int begin, int end)
	{
		LineScanner scanner = new LineScanner(begin, end);
		buffer.markTokens(lineIndex, scanner);
		return scanner.result;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[" + openBracket + ","
			+ closeBracket + "]";
	} //}}}

	protected char openBracket, closeBracket;

	//{{{ class LineScanner
	private class LineScanner implements TokenHandler
	{
		public final Brackets result;

		private int scannedIndex;
		private final int beginIndex;
		private final int endIndex;

		LineScanner(int begin, int end)
		{
			result = new Brackets();
			scannedIndex = 0;
			beginIndex = begin;
			endIndex = end;
		}

		private void scan(Segment seg, int offset, int length)
		{
			int index = scannedIndex;
			if (index >= endIndex)
			{
				return;
			}
			if (index < beginIndex)
			{
				int numToSkip = beginIndex - index;
				if (numToSkip >= length)
				{
					return;
				}
				offset += numToSkip;
				length -= numToSkip;
				index = beginIndex;
			}
			if (index + length > endIndex)
			{
				length = endIndex - index;
			}

			for (int i = 0; i < length; ++i)
			{
				char c = seg.array[seg.offset + offset + i];
				if(c == openBracket)
				{
					result.openCount++;
				}
				else if(c == closeBracket)
				{
					if(result.openCount != 0)
						result.openCount--;
					else
						result.closeCount++;
				}
			}
		}

		@Override
		public void handleToken(Segment seg
			, byte id, int offset, int length
			, TokenMarker.LineContext context)
		{
			// Rejects comments and literals.
			// Accepts all others.
			if (!Token.isCommentOrLiteral(id))
			{
				scan(seg, offset, length);
			}
			scannedIndex += length;
		}

		@Override
		public void setLineContext(TokenMarker.LineContext lineContext)
		{
		}
	} //}}}
}
