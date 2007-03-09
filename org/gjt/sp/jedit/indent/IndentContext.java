/* IndentContext.java
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

import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.StandardUtilities;

/**
 * Holds information about the context where auto-indentation is being
 * applied. The same instance is used by all the rules, so they can
 * re-use existing objects and even "talk" to one another if needed.
 *
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre10
 */
public class IndentContext
{

	public IndentContext(JEditBuffer buffer, int line)
	{
		this.buffer = buffer;
		this.line = line;
		this.prevLineIndex = -1;
		this.prevPrevLineIndex = -1;
		this.oldIndent = -1;
	}

	//{{{ getBuffer() method
	public JEditBuffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ getLineIndex() method
	public int getLineIndex(int offset)
	{
		assert (offset <= 0) : "positive offsets are not handled";
		int lIdx = line;
		for (int i = -1; i >= offset && lIdx >= 0; i--)
		{
			if (i == -1)
			{
				if (prevLineIndex == -1)
				{
					prevLineIndex = buffer.getPriorNonEmptyLine(lIdx);
				}
				lIdx = prevLineIndex;
			}
			else if (i == -2)
			{
				if (prevPrevLineIndex == -1)
				{
					prevPrevLineIndex = buffer.getPriorNonEmptyLine(lIdx);
				}
				lIdx = prevPrevLineIndex;
			}
			else
			{
				lIdx = buffer.getPriorNonEmptyLine(lIdx);
			}
		}
		return lIdx;
	} //}}}

	//{{{ getLineText() method
	/**
	 * Returns the text from the line at the given offset from the
	 * current line. This will ignore empty lines unless the mode
	 * specifies empty lines shouldn't be ignored.
	 *
	 * @param offset Negative offset from current line
	 *		 (e.g., -1 = previous line).
	 */
	public CharSequence getLineText(int offset)
	{
		int lIdx = getLineIndex(offset);
		if (lIdx < 0)
			return null;

		if (lIdx == line)
		{
			if (thisLine == null)
				thisLine = buffer.getLineSegment(line);
			return thisLine;
		}
		else if (lIdx == prevLineIndex)
		{
			if (prevLine == null)
				prevLine = buffer.getLineSegment(prevLineIndex);
			return prevLine;
		}
		else if (lIdx == prevPrevLineIndex)
		{
			if (prevPrevLine == null)
				prevPrevLine = buffer.getLineSegment(prevPrevLineIndex);
			return prevPrevLine;
		}
		else
		{
			return buffer.getLineSegment(lIdx);
		}
	} //}}}

	//{{{ getBrackets() method
	public Brackets getBrackets(int offset, char open, char close)
	{
		return getBrackets(getLineText(offset), open, close);
	} //}}}

	//{{{ getBrackets() method
	public Brackets getBrackets(CharSequence line, char open, char close)
	{
		Brackets brackets = new Brackets();

		for(int i = 0; i < line.length(); i++)
		{
			char ch = line.charAt(i);
			if(ch == open)
			{
				/* Don't increase indent when we see
				an explicit fold. */
				if(open == '{' && line.length() - i >= 3)
				{
					if(line.subSequence(i,i+3).equals("{{{")) /* }}} */
					{
						i += 2;
						continue;
					}
				}
				brackets.openCount++;
			}
			else if(ch == close)
			{
				if(brackets.openCount != 0)
					brackets.openCount--;
				else
					brackets.closeCount++;
			}
		}

		return brackets;
	} //}}}

	//{{{ getOldIndent() methods
	public int getOldIndent()
	{
		if (oldIndent == -1)
		{
			CharSequence prev = getLineText(-1);
			oldIndent = (prev == null) ? 0 :
				StandardUtilities.getLeadingWhiteSpaceWidth(
					prev, buffer.getTabSize());
		}
		return oldIndent;
	} //}}}

	//{{{ Action-related methods
	/**
	 * Returns the current list of indent actions. Can return null.
	 */
	public List<IndentAction> getActions()
	{
		return actions;
	}

	/**
	 * Adds a new action to the list.
	 */
	public void addAction(IndentAction action)
	{
		if (actions == null)
		{
			actions = new LinkedList<IndentAction>();
		}
		actions.add(action);
	}
	//}}}

	//{{{ Brackets class
	public static class Brackets
	{
		int openCount;
		int closeCount;
	} //}}}

	//{{{ Private members
	private int line;
	private JEditBuffer buffer;
	private List<IndentAction> actions;

	// indent values
	private int oldIndent;

	// line text cache
	private CharSequence	thisLine;
	private int 		prevLineIndex;
	private CharSequence 	prevLine;
	private int 		prevPrevLineIndex;
	private CharSequence 	prevPrevLine;
	//}}}

}

