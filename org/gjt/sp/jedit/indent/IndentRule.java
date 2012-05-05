/*
 * IndentRule.java
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

/** Abstract Indentation Rule. Before jEdit 5.0pre1 it was an interface,
 *  then it became an abstract class.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class IndentRule
{
	/**
	 * Apply the indent rule to this line, and return an indent action.
	 *
	 * @param buffer the buffer
	 * @param thisLineIndex the line index
	 * @param prevLineIndex the prior non empty line index
	 * (or -1 if there is no prior non empty line)
	 * @param prevPrevLineIndex the prior non empty line index before the prevLineIndex
	 * (or -1 if there is no prior non empty line)
	 * @param indentActions the indent actions list. The rule can add an action in it if
	 * it is necessary
	 */
	public abstract void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List<IndentAction> indentActions);

	/**
	 * Gets the name of the rule. Defaults to the last component of
	 * the class name.
	 * @since 5.0pre1
	 */
	public String getRuleName()
	{
		String ruleName = this.getClass().getName();
		ruleName = ruleName.replaceFirst("^.*\\.", "");
		return ruleName;
	}
}
