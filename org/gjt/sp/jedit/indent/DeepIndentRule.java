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
import org.gjt.sp.util.StandardUtilities;

import java.util.List;

/**
 * Deep indent rule.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public class DeepIndentRule implements IndentRule
{
	//{{{ getLastParens() method
	/**
	 * Return the indexes of the last closing and the last opening parens in a line
	 *
	 * @param s   the line text
	 * @param pos the starting pos from the end (or -1 for entire line)
	 *
	 * @return the last pos of the parens in the line
	 */
	private static Parens getLastParens(CharSequence s, int pos)
	{
		int lastClose;
		int lastOpen;
		if (pos == -1)
		{
			lastClose = StandardUtilities.getLastIndexOf(s, ')');
			lastOpen = StandardUtilities.getLastIndexOf(s, '(');
		}
		else
		{
			lastClose = StandardUtilities.getLastIndexOf(s, ')', pos);
			lastOpen = StandardUtilities.getLastIndexOf(s, '(', pos);
		}
		return new Parens(lastOpen, lastClose);
	} //}}}

	//{{{ apply() method
	public void apply(IndentContext ctx)
	{
		int lineIndex = ctx.getLineIndex(-1);
		if (lineIndex == -1)
			return;

		int oldLineIndex = lineIndex;
		CharSequence lineText = ctx.getLineText(-1);

		int searchPos = -1;
		while (true)
		{
			if (lineIndex != oldLineIndex)
			{
				lineText = ctx.getBuffer().getLineText(lineIndex);
				oldLineIndex = lineIndex;
			}
			Parens parens = getLastParens(lineText, searchPos);
			if (parens.openOffset > parens.closeOffset)
			{
				// recalculate column (when using tabs instead of spaces)
				int indent = parens.openOffset + TextUtilities.tabsToSpaces(lineText, ctx.getBuffer().getTabSize()).length() - lineText.length();
				ctx.addAction(new IndentAction.AlignParameter(indent, lineText));
				return;
			}

			// No parens on prev line
			if (parens.openOffset == -1 && parens.closeOffset == -1)
			{
				return;
			}
			int openParenOffset = TextUtilities.findMatchingBracket(ctx.getBuffer(), lineIndex, parens.closeOffset);
			if (openParenOffset >= 0)
			{
				lineIndex = ctx.getBuffer().getLineOfOffset(openParenOffset);
				searchPos = openParenOffset - ctx.getBuffer().getLineStartOffset(lineIndex) - 1;
				if (searchPos < 0)
				 	break;
			}
			else
				break;
		}
	} //}}}

	//{{{ Parens() class
	private static class Parens
	{
		final int openOffset;
		final int closeOffset;

		Parens(int openOffset, int closeOffset)
		{
			this.openOffset = openOffset;
			this.closeOffset = closeOffset;
		}

		public String toString()
		{
			return "Parens(" + openOffset + ',' + closeOffset + ')';
		}
	} //}}}
}

