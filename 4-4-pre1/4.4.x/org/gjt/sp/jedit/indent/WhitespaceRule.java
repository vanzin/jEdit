/*
 * WhitespaceRule.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Marcelo Vanzin
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

/**
 * Whitespace rule. This rule cancels all indent actions in the
 * following case:
 *
 * <ul>
 * <li>The previous line is all whitespace</li>
 * <li>The current line is not empty</li>
 * </ul>
 *
 * <p>The result is that this rule won't allow the indentation to be
 * increased, only decreased (by rules triggered by unindentThisLine).
 * If the requirements above do not apply, this rule does nothing.</p>
 *
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre10
 */
public class WhitespaceRule implements IndentRule
{

	public void apply(JEditBuffer buffer, int thisLineIndex,
			  int prevLineIndex, int prevPrevLineIndex,
			  List<IndentAction> indentActions)
	{
		/* Don't apply this rule if the current line is empty. */
		CharSequence current = buffer.getLineSegment(thisLineIndex);
		boolean found = false;
		for (int i = 0; i < current.length(); i++)
		{
			if (!Character.isWhitespace(current.charAt(i)))
			{
				found = true;
				break;
			}
		}
		if (!found)
			return;

		/* Check if the previous line is empty. */
		if (prevLineIndex >= 0) {
			CharSequence previous = buffer.getLineSegment(prevLineIndex);
			for (int i = 0; i < previous.length(); i++)
			{
				if (!Character.isWhitespace(previous.charAt(i)))
					return;
			}
		}
		indentActions.add(new IndentAction.NoIncrease());
	}

}

