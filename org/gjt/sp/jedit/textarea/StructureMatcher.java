/*
 * StructureMatcher.java - Abstract interface for bracket matching, etc.
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

//{{{ Imports
import java.awt.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
//}}}

/**
 * An interface for matching parts of a source file's stucture. The default
 * implementation matches brackets. The XML plugin provides an implementation
 * for matching XML tags.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre3
 */
public interface StructureMatcher
{
	//{{{ getMatch() method
	/**
	 * Returns the element matching the one at the given text area's
	 * caret position, or null.
	 * @since jEdit 4.2pre3
	 */
	Match getMatch(JEditTextArea textArea);
	//}}}

	//{{{ BracketMatcher class
	static class BracketMatcher implements StructureMatcher
	{
		public Match getMatch(JEditTextArea textArea)
		{
			int offset = textArea.getCaretPosition()
				- textArea.getLineStartOffset(
				textArea.getCaretLine());

			if(offset != 0)
			{
				int bracketOffset = TextUtilities.findMatchingBracket(
					textArea.getBuffer(),
					textArea.getCaretLine(),
					offset - 1);
				if(bracketOffset != -1)
				{
					int bracketLine = textArea
						.getLineOfOffset(
						bracketOffset);
					return new Match(bracketLine,
						bracketOffset,
						bracketLine,
						bracketOffset + 1);
				}
			}

			return null;
		}
	} //}}}

	//{{{ Match class
	/**
	 * A structure match, denoted by a start and end position.
	 * @since jEdit 4.2pre3
	 */
	public static class Match
	{
		public int startLine;
		public int start;
		public int endLine;
		public int end;

		public Match()
		{
		}

		public Match(int startLine, int start, int endLine, int end)
		{
			this.startLine = startLine;
			this.start = start;
			this.endLine = endLine;
			this.end = end;
		}
	} //}}}

	//{{{ Highlight class
	/**
	 * Paints the structure match highlight.
	 */
	static class Highlight extends TextAreaExtension
	{
		Highlight(JEditTextArea textArea)
		{
			this.textArea = textArea;
			returnValue = new Point();
		}

		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(!textArea.getPainter().isStructureHighlightEnabled())
				return;

			Match match = textArea.getStructureMatch();
			if(match != null)
			{
				paintHighlight(gfx,screenLine,physicalLine,
					start,end,y,match);
			}
		}

		private void paintHighlight(Graphics gfx, int screenLine,
			int physicalLine, int start, int end, int y,
			Match match)
		{
			if(!textArea.isStructureHighlightVisible())
				return;

			if(match.start >= end || match.end < start)
			{
				return;
			}

			int matchStartLine = textArea.getScreenLineOfOffset(
				match.start);
			int matchEndLine = textArea.getScreenLineOfOffset(
				match.end);

			FontMetrics fm = textArea.getPainter().getFontMetrics();
			int height = fm.getHeight();

			int x1, x2;

			if(matchStartLine == screenLine)
			{
				x1 = match.start - textArea.getLineStartOffset(
					match.startLine);
			}
			else
				x1 = 0;

			if(matchEndLine == screenLine)
			{
				x2 = match.end - textArea.getLineStartOffset(
					match.endLine);
			}
			else
			{
				x2 = textArea.getScreenLineEndOffset(screenLine)
					- textArea.getScreenLineStartOffset(screenLine);
			}

			x1 = textArea.offsetToXY(physicalLine,x1,returnValue).x;
			x2 = textArea.offsetToXY(physicalLine,x2,returnValue).x;

			gfx.setColor(textArea.getPainter().getStructureHighlightColor());

			gfx.drawLine(x1,y,x1,y + height - 1);
			gfx.drawLine(x2,y,x2,y + height - 1);

			if(matchStartLine == screenLine || screenLine == 0)
				gfx.drawLine(x1,y,x2,y);
			else
			{
				int prevX1, prevX2;

				if(matchStartLine == screenLine - 1)
				{
					prevX1 = match.start - textArea.getLineStartOffset(
						match.startLine);
				}
				else
					prevX1 = 0;

				prevX2 = textArea.getScreenLineEndOffset(screenLine - 1)
					- textArea.getScreenLineStartOffset(screenLine - 1);

				prevX1 = textArea.offsetToXY(physicalLine - 1,prevX1,returnValue).x;
				prevX2 = textArea.offsetToXY(physicalLine - 1,prevX2,returnValue).x;

				gfx.drawLine(Math.min(x1,prevX1),y,
					Math.max(x1,prevX1),y);
				gfx.drawLine(Math.min(x2,prevX2),y,
					Math.max(x2,prevX2),y);
			}

			if(matchEndLine == screenLine)
				gfx.drawLine(x1,y + height - 1,x2,y + height - 1);
		}

		private JEditTextArea textArea;
		private Point returnValue;
	} //}}}
}
