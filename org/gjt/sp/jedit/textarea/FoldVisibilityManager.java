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
public class FoldVisibilityManager
{
	//{{{ FoldVisibilityManager constructor
	public FoldVisibilityManager(Buffer buffer, JEditTextArea textArea)
	{
		this.buffer = buffer;
		this.textArea = textArea;
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
	public final boolean isLineVisible(int line)
	{
		return buffer.getLineInfo(line).isVisible(index);
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		for(int i = line + 1; i < buffer.getLineCount(); i++)
		{
			if(isLineVisible(i))
				return i;
		}
		return -1;
	} //}}}

	//{{{ getPrevVisibleLine() method
	/**
	 * Returns the previous visible line before the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getPrevVisibleLine(int line)
	{
		for(int i = line - 1; i >= 0; i--)
		{
			if(isLineVisible(i))
				return i;
		}
		return -1;
	} //}}}

	//{{{ Debugging code
	boolean counting;
	int counter;

	public void startCounting()
	{
		counter = 0;
		counting = true;
	}

	public void stopCounting()
	{
		counting = false;
	}

	public int getCount()
	{
		return counter;
	}

	public void test()
	{
		java.util.Random random = new java.util.Random();
		long start = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++)
		{
			int line = Math.abs(random.nextInt() % virtualLineCount);
			int v2p = virtualToPhysical(line);
			int p2v = physicalToVirtual(v2p);
			if(p2v != line)
			{
				System.err.println(line + ":" + v2p + ":" + p2v);
				return;
			}
			//System.err.println("--- passed: " + line);
		}
		System.err.println(System.currentTimeMillis() - start);
	}
	//}}}

	//{{{ physicalToVirtual() method
	/**
	 * Converts a physical line number to a virtual line number.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int physicalToVirtual(int line)
	{
		//counter++;

		if(line < 0 || line >= buffer.getLineCount())
			throw new ArrayIndexOutOfBoundsException(String.valueOf(line));

		while(!isLineVisible(line))
			line--;

		if(lastPhysical == line)
			/* do nothing */;
		else if(line > lastPhysical)
		{
			for(;;)
			{
				if(lastPhysical == line)
					break;

				if(isLineVisible(lastPhysical))
					lastVirtual++;

				if(lastPhysical == buffer.getLineCount() - 1)
					break;
				else
					lastPhysical++;
			}
		}
		else if(line < lastPhysical && lastPhysical - line > line)
		{
			for(;;)
			{
				if(lastPhysical == line)
					break;

				if(isLineVisible(lastPhysical))
					lastVirtual--;

				if(lastPhysical == 0)
					break;
				else
					lastPhysical--;
			}

		}
		else
		{
			lastPhysical = lastVirtual = 0;
			for(;;)
			{
				if(lastPhysical == line)
					break;

				if(isLineVisible(lastPhysical))
					lastVirtual++;

				if(lastPhysical == buffer.getLineCount() - 1)
					break;
				else
					lastPhysical++;
			}
		}

		if(lastVirtual >= virtualLineCount)
			System.err.println("P2V: " + lastPhysical
				+ ":" + lastVirtual + ":" + virtualLineCount);
		return lastVirtual;
	} //}}}

	//{{{ virtualToPhysical() method
	/**
	 * Converts a virtual line number to a physical line number.
	 * @param line A virtual line index
	 * @since jEdit 4.0pre1
	 */
	public int virtualToPhysical(int line)
	{
		//counter++;

		if(line < 0 || line >= virtualLineCount)
			throw new ArrayIndexOutOfBoundsException(String.valueOf(line));

		if(lastVirtual == line)
			/* do nothing */;
		else if(line > lastVirtual)
		{
			for(;;)
			{
				if(isLineVisible(lastPhysical))
				{
					if(lastVirtual == line)
						break;
					else
						lastVirtual++;
				}

				if(lastPhysical == buffer.getLineCount() - 1)
					break;
				else
					lastPhysical++;
			}
		}
		else if(line < lastVirtual && lastVirtual - line > line)
		{
			for(;;)
			{
				if(isLineVisible(lastPhysical))
				{
					if(lastVirtual == line)
						break;
					else
						lastVirtual--;
				}

				if(lastPhysical == 0)
					break;
				else
					lastPhysical--;
			}

		}
		else
		{
			lastPhysical = lastVirtual = 0;
			for(;;)
			{
				if(isLineVisible(lastPhysical))
				{
					if(lastVirtual == line)
						break;
					else
						lastVirtual++;
				}

				if(lastPhysical == buffer.getLineCount() - 1)
					break;
				else
					lastPhysical++;
			}
		}

		if(lastPhysical >= buffer.getLineCount())
			System.err.println("V2P: " + lastVirtual
				+ ":" + lastPhysical + ":" + buffer.getLineCount());
		return lastPhysical;
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
		virtualLineCount = buffer.getLineCount();
		for(int i = 0; i < virtualLineCount; i++)
		{
			buffer.getLineInfo(i).setVisible(index,true);
		}
		textArea.foldStructureChanged();
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
		// so that getFoldLevel() calling fireFoldLevelsChanged()
		// won't break
		virtualLineCount = buffer.getLineCount() - 1;

		int newVirtualLineCount = 0;
		foldLevel = (foldLevel - 1) * buffer.getIndentSize() + 1;

		/* this ensures that the first line is always visible */
		boolean seenVisibleLine = false;

		for(int i = 0; i < buffer.getLineCount(); i++)
		{
			if(!seenVisibleLine || buffer.getFoldLevel(i) < foldLevel)
			{
				seenVisibleLine = true;
				buffer.getLineInfo(i).setVisible(index,true);
				newVirtualLineCount++;
			}
			else
				buffer.getLineInfo(i).setVisible(index,false);
		}

		virtualLineCount = newVirtualLineCount;
		textArea.foldStructureChanged();
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

	//{{{ Methods for Buffer class to call

	//{{{ _grab() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public final void _grab(int index)
	{
		this.index = index;
		virtualLineCount = 0;
		for(int i = 0; i < buffer.getLineCount(); i++)
		{
			if(isLineVisible(i))
				virtualLineCount++;
		}
		lastPhysical = lastVirtual = 0;
	} //}}}

	//{{{ _release() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public final void _release()
	{
		index = -1;
	} //}}}

	//{{{ _getIndex() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public final int _getIndex()
	{
		return index;
	} //}}}

	//{{{ _linesInserted() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public void _linesInserted(int startLine, int numLines)
	{
		if(numLines != 0)
		{
			for(int i = 0; i < numLines; i++)
			{
				if(isLineVisible(startLine + i))
					virtualLineCount++;
			}
		}
	} //}}}

	//{{{ _linesRemoved() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public void _linesRemoved(int startLine, int numLines)
	{
		if(numLines != 0)
		{
			for(int i = 0; i < numLines; i++)
			{
				// note that Buffer calls linesRemoved()
				// of FoldVisibilityManagers before the
				// line info array is shrunk.
				if(isLineVisible(startLine + i))
					virtualLineCount--;
			}
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private Buffer buffer;
	private JEditTextArea textArea;
	private int index;
	private int virtualLineCount;
	private int lastPhysical;
	private int lastVirtual;
	//}}}

	//}}}
}
