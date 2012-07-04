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
 * Computes the correct indent for the line after a multi-line comment.
 * <p>
 * The main goal is to do the right thing for javadoc-style multi-line
 * comments, where the closing line is not aligned with the opening
 * line. The next line after the comment should be at the same indent
 * level as the opening line of the comment, not the closing line.
 *
 * @author Marcelo Vanzin
 * @version $Id$
 */
public class CommentIndentRule implements IndentRule
{

	public CommentIndentRule(String start, String end)
	{
		this.start = start;
		this.end = end;
	}

	public void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List<IndentAction> indentActions)
	{
		if (prevLineIndex < 0)
			return;

		Searcher s = new Searcher(end);
		buffer.markTokens(prevLineIndex, s);
		if (!s.isFound())
			return;

		// We found a comment end on the previous line. Look for the
		// comment start and, if found, use that line as the baseline
		// for the indent.
		s = new Searcher(start);
		for (int lineIndex = prevLineIndex; lineIndex >= 0; lineIndex--)
		{
			buffer.markTokens(lineIndex, s);
			if (s.isFound())
			{
				indentActions.add(new IndentAction.SetBaseIndent(lineIndex));
				break;
			}
		}
	}

	private final String start;
	private final String end;

	private static class Searcher implements TokenHandler
	{

		Searcher(String needle)
		{
			this.needle = needle;
		}

		public void handleToken(Segment seg,
			byte id, int offset, int length,
			TokenMarker.LineContext context)
		{
			if (found)
				return;

			switch (id) {
			case Token.LITERAL1:
			case Token.LITERAL2:
			case Token.LITERAL3:
			case Token.LITERAL4:
				return;
			}

			CharSequence seq = new SegmentCharSequence(seg, offset, length);
			found = StandardUtilities.indexOf(seq, needle) >= 0;
		}

		public void setLineContext(TokenMarker.LineContext lineContext)
		{
		}

		boolean isFound()
		{
			return found;
		}

		private boolean found;
		private final String needle;
	}
}

