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

	/**
	 * @return true if the indent engine should keep processing after
	 * this rule.
	 */
	public boolean keepChecking();

	/**
	 * This handles the following Java code:
	 * if(something)
	 * { // no indentation on this line, even though previous matches a rule
	 */
	public class Collapse implements IndentAction
	{
		/**
		 * This does nothing; it is merely a sentinel for the
		 * <code>OpenBracketIndentRule</code>.
		 */
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return newIndent;
		}
		
		public boolean keepChecking()
		{
			return true;
		}
		
		public boolean equals(Object o)
		{
			return (o instanceof Collapse);
		}
	}

	public class Reset implements IndentAction
	{
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return oldIndent;
		}
		
		public boolean keepChecking()
		{
			return false;
		}
	}

	public class Increase implements IndentAction
	{
		private int amount;
		
		public Increase()
		{
			amount = 1;
		}
		
		public Increase(int amount)
		{
			this.amount = amount;
		}
		
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return newIndent + buffer.getIndentSize() * amount;
		}
		
		public boolean keepChecking()
		{
			return true;
		}
	}

	public class Decrease implements IndentAction
	{
		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			return newIndent - buffer.getIndentSize();
		}
		
		public boolean keepChecking()
		{
			return true;
		}
	}

	public class AlignBracket implements IndentAction
	{
		private int line, offset;
		private int openLineIndex;
		private String openLine;
		private boolean extraIndent;

		public AlignBracket(Buffer buffer, int line, int offset)
		{
			this.line = line;
			this.offset = offset;
			
			int openBracketIndex = TextUtilities.findMatchingBracket(
				buffer,this.line,this.offset);
			if(openBracketIndex == -1)
				openLineIndex = -1;
			else
			{
				openLineIndex = buffer.getLineOfOffset(openBracketIndex);
				openLine = buffer.getLineText(openLineIndex);
			}
		}

		public boolean getExtraIndent()
		{
			return extraIndent;
		}
		
		public void setExtraIndent(boolean extraIndent)
		{
			this.extraIndent = extraIndent;
		}
		
		public String getOpenBracketLine()
		{
			return openLine;
		}

		public int calculateIndent(Buffer buffer, int line, int oldIndent,
			int newIndent)
		{
			if(openLine == null)
				return newIndent;
			else
			{
				return MiscUtilities.getLeadingWhiteSpaceWidth(
					openLine,buffer.getTabSize())
					+ (extraIndent ? buffer.getIndentSize() : 0);
			}
		}
		
		public boolean keepChecking()
		{
			return false;
		}
	}
}
