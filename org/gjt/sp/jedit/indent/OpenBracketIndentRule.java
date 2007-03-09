/*
 * OpenBracketIndentRule.java
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

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class OpenBracketIndentRule extends BracketIndentRule
{
	//{{{ OpenBracketIndentRule constructor
	public OpenBracketIndentRule(char openBracket, boolean aligned)
	{
		super(openBracket,
			TextUtilities.getComplementaryBracket(openBracket,null));
		this.aligned = aligned;
	} //}}}

	//{{{ apply() method
	public void apply(IndentContext ctx)
	{
		int prevOpenBracketCount =
			getOpenBracketCount(ctx, -1);
		if(prevOpenBracketCount != 0)
		{
			handleCollapse(ctx.getActions(), true);
			boolean multiple = ctx.getBuffer().getBooleanProperty(
				"multipleBracketIndent");
			IndentAction increase = new IndentAction.Increase(
				multiple ? prevOpenBracketCount : 1);
			ctx.addAction(increase);
		}
		else if(getOpenBracketCount(ctx, 0) != 0)
		{
			handleCollapse(ctx.getActions(), false);
		}
	} //}}}

	//{{{ getOpenBracketCount() method
	private int getOpenBracketCount(IndentContext ctx, int offset)
	{
		CharSequence line = ctx.getLineText(offset);
		return (line != null) ? getBrackets(ctx, line).openCount : 0;
	} //}}}

	//{{{ handleCollapse() method
	private static void handleCollapse(List<IndentAction> indentActions,
					   boolean delPrevPrevCollapse)
	{
		if (indentActions == null)
			return;
		if (indentActions.contains(IndentAction.PrevCollapse))
		{
			indentActions.clear();
			return;
		}

		if (delPrevPrevCollapse && indentActions.contains(IndentAction.PrevPrevCollapse))
		{
			indentActions.clear();
			return;
		}
	} //}}}

	private boolean aligned;
}

