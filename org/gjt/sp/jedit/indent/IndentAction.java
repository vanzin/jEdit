/*
 * IndentAction.java
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

import org.gjt.sp.jedit.*;

public interface IndentAction
{
	/**
	 * @param buffer The buffer
	 * @param line The line number that matched the rule; not necessarily
	 * the line being indented.
	 * @param oldIndent Original indent.
	 * @param newIndent The new indent -- ie, indent returned by previous
	 * indent action.
	 */
	public int calculateIndent(Buffer buffer, int line, int oldIndent,
		int newIndent);

	public class Reset implements IndentAction
	{
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return oldIndent;
		}
	}

	public class Increase implements IndentAction
	{
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			/* this is intentional -- we never want to
			increase indent > 1 */
			return oldIndent + buffer.getIndentSize();
		}
	}

	public class Decrease implements IndentAction
	{
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return newIndent - buffer.getIndentSize();
		}
	}

	public class AlignBracket implements IndentAction
	{
		private int line, offset;

		public AlignBracket(int line, int offset)
		{
			this.line = line;
			this.offset = offset;
		}

		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			int openBracketIndex = TextUtilities.findMatchingBracket(
				buffer,this.line,this.offset);
			if(openBracketIndex == -1)
				return newIndent;

			int openLineIndex = buffer.getLineOfOffset(openBracketIndex);
			String openLine = buffer.getLineText(openLineIndex);

			return MiscUtilities.getLeadingWhiteSpaceWidth(
				openLine,buffer.getTabSize());
		}
	}
}
