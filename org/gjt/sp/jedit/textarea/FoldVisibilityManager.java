/*
 * FoldVisibilityManager.java - Controls fold visiblity
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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

import java.awt.Toolkit;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.*;

/**
 * Controls fold visibility.
 * Note that a "physical" line number is a line index, numbered from the
 * start of the buffer. A "virtual" line number is a visible line index;
 * lines after a collapsed fold have a virtual line number that is less
 * than their physical line number, for example.<p>
 *
 * You can use the <code>physicalToVirtual()</code> and
 * <code>virtualToPhysical()</code> methods to convert one type of line
 * number to another.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class FoldVisibilityManager implements BufferChangeListener
{
	//{{{ FoldVisibilityManager constructor
	public FoldVisibilityManager(Buffer buffer, JEditTextArea textArea)
	{
		this.buffer = buffer;
		this.textArea = textArea;
		map = new FoldVisibilityMap();
		virtualLineCount = buffer.getLineCount();
	} //}}}

	//{{{ getVirtualLineCount() method
	/**
	 * Returns the number of virtual lines in the buffer.
	 * @since jEdit 4.0pre1
	 */
	public int getVirtualLineCount()
	{
		return virtualLineCount;
	} //}}}

	//{{{ isLineVisible() method
	/**
	 * Returns if the specified line is visible.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public boolean isLineVisible(int line)
	{
		int foldStart = getFoldForLine(line);
		if(foldStart == -1)
			return true;
		else
			return map.getp(foldStart).visible;
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		if(line >= buffer.getLineCount() - 1)
			return -1;
		else
		{
			int foldStart = getFoldForLine(line);
			if(foldStart == -1 || (map.getp(foldStart).visible
				&& !isHashLine(line + 1)))
				return line + 1;
			else
			{
				for(int i = line + 1; i < buffer.getLineCount(); i++)
				{
					if(isHashLine(i) && map.getp(i).visible)
						return i;
				}

				// it was the last visible line
				return -1;
			}
		}
	} //}}}

	//{{{ getPrevVisibleLine() method
	/**
	 * Returns the previous visible line before the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getPrevVisibleLine(int line)
	{
		if(line == 0)
			return -1;
		else
		{
			int foldStart = getFoldForLine(line);
			if(foldStart == -1 || (map.getp(foldStart).visible
				&& foldStart != line))
				return line - 1;
			else
			{
				for(int i = line - 1; i >= 0; /* nothing */)
				{
					foldStart = getFoldForLine(i);
					if(foldStart == -1)
						return -1;

					if(map.getp(foldStart).visible)
						return i;
					else
						i = foldStart - 1;
				}

				// it was the first visible line
				return -1;
			}
		}
	} //}}}

	//{{{ physicalToVirtual() method
	/**
	 * Converts a physical line number to a virtual line number.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int physicalToVirtual(int line)
	{
		return line;
	} //}}}

	//{{{ virtualToPhysical() method
	/**
	 * Converts a virtual line number to a physical line number.
	 * @param line A virtual line index
	 * @since jEdit 4.0pre1
	 */
	public int virtualToPhysical(int line)
	{
		return line;
	} //}}}

	//{{{ collapseFold() method
	/**
	 * Collapses the fold at the specified physical line index.
	 * @param line A virtual line index
	 * @since jEdit 4.0pre1
	 */
	public void collapseFold(int line)
	{
	} //}}}

	//{{{ expandFold() method
	/**
	 * Expands the fold at the specified physical line index.
	 * @param line A virtual line index
	 * @param fully If true, all subfolds will also be expanded
	 * @since jEdit 4.0pre1
	 */
	public void expandFold(int line, boolean fully)
	{
	} //}}}

	//{{{ expandAllFolds() method
	/**
	 * Expands all folds.
	 * @since jEdit 4.0pre1
	 */
	public void expandAllFolds()
	{
	} //}}}

	//{{{ expandFolds() method
	/**
	 * This method should only be called from <code>actions.xml</code>.
	 * @since jEdit 4.0pre1
	 */
	public void expandFolds(char digit)
	{
		if(digit < '1' || digit > '9')
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		else
			expandFolds((int)(digit - '1') + 1);
	} //}}}

	//{{{ expandFolds() method
	/**
	 * Expands all folds with the specified fold level.
	 * @param foldLevel The fold level
	 * @since jEdit 4.0pre1
	 */
	public void expandFolds(int foldLevel)
	{
	} //}}}

	//{{{ narrow() method
	/**
	 * Narrows the visible portion of the buffer to the specified
	 * line range.
	 * @param start The first line
	 * @param end The last line
	 * @since jEdit 4.0pre1
	 */
	public void narrow(int start, int end)
	{
	} //}}}

	//{{{ Buffer change handlers

	//{{{ foldLevelChanged() method
	/**
	 * Called when the fold level of a line changes.
	 * @param buffer The buffer in question
	 * @param line The line number
	 * @since jEdit 4.0pre1
	 */
	public void foldLevelChanged(Buffer buffer, int line)
	{
		if(line == 0)
			return;

		int level = buffer.getFoldLevel(line);

		if(isHashLine(line))
		{
			int virtualLine = 0;
			boolean visible = true;

			int counter = 0;
			for(int i = line - 1; i >= 0; /* nothing */)
			{
				int foldStart = getFoldForLine(i);
				if(foldStart == -1)
				{
					virtualLine = counter + i;
					visible = true;
					break;
				}

				visible = map.getp(foldStart).visible;
				if(visible)
					counter += (i - foldStart);

				if(buffer.getFoldLevel(i) < level)
				{
					virtualLine = counter;
					break;
				}

				i = foldStart - 1;
			}

			map.put(line,virtualLine,visible);
		}
		else
			map.removep(line);
	} //}}}

	//{{{ contentInserted() method
	/**
	 * Called when text is inserted into the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param start The start offset, from the beginning of the buffer
	 * @param numLines The number of lines inserted
	 * @param length The number of characters inserted
	 * @since jEdit 4.0pre1
	 */
	public void contentInserted(Buffer buffer, int startLine, int start,
		int numLines, int length)
	{
		if(numLines != 0)
		{
			virtualLineCount += numLines;
			map.insertLines(startLine,numLines);
		}
	} //}}}

	//{{{ contentRemoved() method
	/**
	 * Called when text is removed from the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param start The start offset, from the beginning of the buffer
	 * @param numLines The number of lines removed
	 * @param length The number of characters removed
	 * @since jEdit 4.0pre1
	 */
	public void contentRemoved(Buffer buffer, int startLine, int start,
		int numLines, int length)
	{
		if(numLines != 0)
		{
			virtualLineCount -= numLines;
			map.removeLines(startLine,numLines);
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private Buffer buffer;
	private JEditTextArea textArea;
	private FoldVisibilityMap map;
	private int virtualLineCount;
	//}}}

	//{{{ isHashLine() method
	private boolean isHashLine(int line)
	{
		if(line == 0)
			return false;
		else
			return buffer.getFoldLevel(line) != buffer.getFoldLevel(line - 1);
	}

	//{{{ getFoldForLine() method
	private int getFoldForLine(int line)
	{
		for(int i = line; i >= 1; i--)
		{
			if(isHashLine(i))
				return i;
		}

		return -1;
	} //}}}

	//}}}
}
