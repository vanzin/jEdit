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

		int collapseFolds = buffer.getIntegerProperty("collapseFolds",0);
		if(collapseFolds != 0)
			expandFolds(collapseFolds);
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
		try
		{
			buffer.readLock();
			return buffer._isLineVisible(line,index);
		}
		finally
		{
			buffer.readUnlock();
		}
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		try
		{
			buffer.readLock();

			if(line == buffer.getLineCount() - 1)
				return -1;

			for(int i = line + 1; i < buffer.getLineCount(); i++)
			{
				if(buffer._isLineVisible(i,index))
					return i;
			}
			return -1;
		}
		finally
		{
			buffer.readUnlock();
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
		try
		{
			buffer.readLock();

			if(line == 0)
				return -1;

			for(int i = line - 1; i >= 0; i--)
			{
				if(buffer._isLineVisible(i,index))
					return i;
			}
			return -1;
		}
		finally
		{
			buffer.readUnlock();
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
		try
		{
			buffer.readLock();

			if(line < 0 || line >= buffer.getLineCount())
				throw new ArrayIndexOutOfBoundsException(String.valueOf(line));

			while(!buffer._isLineVisible(line,index) && line > 0)
				line--;

			if(lastPhysical == line)
			{
				if(lastVirtual < 0 || lastVirtual >= virtualLineCount)
				{
					throw new ArrayIndexOutOfBoundsException(
						"cached: " + lastVirtual);
				}
			}
			else if(line > lastPhysical)
			{
				for(;;)
				{
					if(lastPhysical == line)
						break;

					if(buffer._isLineVisible(lastPhysical,index))
						lastVirtual++;

					if(lastPhysical == buffer.getLineCount() - 1)
						break;
					else
						lastPhysical++;
				}

				if(lastVirtual < 0 || lastVirtual >= virtualLineCount)
				{
					throw new ArrayIndexOutOfBoundsException(
						"fwd scan: " + lastVirtual);
				}
			}
			else if(line < lastPhysical && lastPhysical - line > line)
			{
				for(;;)
				{
					if(lastPhysical == line)
						break;

					if(buffer._isLineVisible(lastPhysical,index))
						lastVirtual--;

					if(lastPhysical == 0)
						break;
					else
						lastPhysical--;
				}

				if(lastVirtual < 0 || lastVirtual >= virtualLineCount)
				{
					throw new ArrayIndexOutOfBoundsException(
						"back scan: " + lastVirtual);
				}
			}
			else
			{
				lastPhysical = lastVirtual = 0;
				for(;;)
				{
					if(lastPhysical == line)
						break;

					if(buffer._isLineVisible(lastPhysical,index))
						lastVirtual++;

					if(lastPhysical == buffer.getLineCount() - 1)
						break;
					else
						lastPhysical++;
				}

				if(lastVirtual < 0 || lastVirtual >= virtualLineCount)
				{
					throw new ArrayIndexOutOfBoundsException(
						"zero scan: " + lastVirtual);
				}
			}

			return lastVirtual;
		}
		finally
		{
			buffer.readUnlock();
		}
	} //}}}

	//{{{ virtualToPhysical() method
	/**
	 * Converts a virtual line number to a physical line number.
	 * @param line A virtual line index
	 * @since jEdit 4.0pre1
	 */
	public int virtualToPhysical(int line)
	{
		try
		{
			buffer.readLock();

			if(line < 0 || line >= virtualLineCount)
				throw new ArrayIndexOutOfBoundsException(String.valueOf(line));

			if(lastVirtual == line)
			{
				if(lastPhysical < 0 || lastPhysical >= buffer.getLineCount())
				{
					throw new ArrayIndexOutOfBoundsException(
						"cached: " + lastPhysical);
				}
			}
			else if(line > lastVirtual)
			{
				for(;;)
				{
					if(buffer._isLineVisible(lastPhysical,index))
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

				if(lastPhysical < 0 || lastPhysical >= buffer.getLineCount())
				{
					throw new ArrayIndexOutOfBoundsException(
						"fwd scan: " + lastPhysical);
				}
			}
			else if(line < lastVirtual && lastVirtual - line > line)
			{
				for(;;)
				{
					if(buffer._isLineVisible(lastPhysical,index))
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

				if(lastPhysical < 0 || lastPhysical >= buffer.getLineCount())
				{
					throw new ArrayIndexOutOfBoundsException(
						"back scan: " + lastPhysical);
				}
			}
			else
			{
				lastPhysical = lastVirtual = 0;
				for(;;)
				{
					if(buffer._isLineVisible(lastPhysical,index))
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

				if(lastPhysical < 0 || lastPhysical >= buffer.getLineCount())
				{
					throw new ArrayIndexOutOfBoundsException(
						"zero scan: " + lastPhysical);
				}
			}

			return lastPhysical;
		}
		finally
		{
			buffer.readUnlock();
		}
	} //}}}

	//{{{ collapseFold() method
	/**
	 * Collapses the fold at the specified physical line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public void collapseFold(int line)
	{
		int lineCount = buffer.getLineCount();
		int start = 0;
		int end = lineCount - 1;

		try
		{
			buffer.writeLock();

			int initialFoldLevel = buffer.getFoldLevel(line);

			//{{{ Find fold start and end...
			if(line != lineCount - 1
				&& buffer.getFoldLevel(line + 1) > initialFoldLevel)
			{
				// this line is the start of a fold
				start = line + 1;

				for(int i = line + 1; i < lineCount; i++)
				{
					if(buffer.getFoldLevel(i) <= initialFoldLevel)
					{
						end = i - 1;
						break;
					}
				}
			}
			else
			{
				boolean ok = false;

				// scan backwards looking for the start
				for(int i = line - 1; i >= 0; i--)
				{
					if(buffer.getFoldLevel(i) < initialFoldLevel)
					{
						start = i + 1;
						ok = true;
						break;
					}
				}

				if(!ok)
				{
					// no folds in buffer
					return;
				}

				for(int i = line + 1; i < lineCount; i++)
				{
					if(buffer.getFoldLevel(i) < initialFoldLevel)
					{
						end = i - 1;
						break;
					}
				}
			} //}}}

			//{{{ Collapse the fold...
			int delta = (end - start + 1);

			for(int i = start; i <= end; i++)
			{
				if(buffer._isLineVisible(i,index))
					buffer._setLineVisible(i,index,false);
				else
					delta--;
			}

			if(delta == 0)
			{
				// user probably pressed A+BACK_SPACE twice
				return;
			}

			virtualLineCount -= delta;
			//}}}
		}
		finally
		{
			buffer.writeUnlock();
		}

		foldStructureChanged();

		int virtualLine = physicalToVirtual(start);
		if(textArea.getFirstLine() > virtualLine)
			textArea.setFirstLine(virtualLine - textArea.getElectricScroll());
	} //}}}

	//{{{ expandFold() method
	/**
	 * Expands the fold at the specified physical line index.
	 * @param line A physical line index
	 * @param fully If true, all subfolds will also be expanded
	 * @since jEdit 4.0pre1
	 */
	public void expandFold(int line, boolean fully)
	{
		int lineCount = buffer.getLineCount();
		int start = 0;
		int end = lineCount - 1;
		int delta = 0;

		try
		{
			buffer.writeLock();

			int initialFoldLevel = buffer.getFoldLevel(line);

			//{{{ Find fold start and fold end...
			if(line != lineCount - 1
				&& buffer._isLineVisible(line,index)
				&& !buffer._isLineVisible(line + 1,index)
				&& buffer.getFoldLevel(line + 1) > initialFoldLevel)
			{
				// this line is the start of a fold
				start = line + 1;

				for(int i = line + 1; i < lineCount; i++)
				{
					if(buffer._isLineVisible(i,index) && buffer.getFoldLevel(i) <= initialFoldLevel)
					{
						end = i - 1;
						break;
					}
				}
			}
			else
			{
				boolean ok = false;

				// scan backwards looking for the start
				for(int i = line - 1; i >= 0; i--)
				{
					if(buffer._isLineVisible(i,index) && buffer.getFoldLevel(i) < initialFoldLevel)
					{
						start = i + 1;
						ok = true;
						break;
					}
				}

				if(!ok)
				{
					// no folds in buffer
					return;
				}

				for(int i = line + 1; i < lineCount; i++)
				{
					if(buffer._isLineVisible(i,index) && buffer.getFoldLevel(i) < initialFoldLevel)
					{
						end = i - 1;
						break;
					}
				}
			} //}}}

			//{{{ Expand the fold...

			// we need a different value of initialFoldLevel here!
			initialFoldLevel = buffer.getFoldLevel(start);

			for(int i = start; i <= end; i++)
			{
				if(buffer._isLineVisible(i,index))
				{
					// do nothing
				}
				else if(!fully && buffer.getFoldLevel(i) > initialFoldLevel)
				{
					// don't expand lines with higher fold
					// levels
				}
				else
				{
					delta++;
					buffer._setLineVisible(i,index,true);
				}
			}

			virtualLineCount += delta;
			//}}}

			if(!fully && !buffer._isLineVisible(line,index))
			{
				// this is a hack, and really needs to be done better.
				expandFold(line,false);
				return;
			}
		}
		finally
		{
			buffer.writeUnlock();
		}

		foldStructureChanged();

		int virtualLine = physicalToVirtual(start);
		int firstLine = textArea.getFirstLine();
		int visibleLines = textArea.getVisibleLines();
		if(virtualLine + delta >= firstLine + visibleLines
			&& delta < visibleLines - 1)
		{
			textArea.setFirstLine(virtualLine + delta - visibleLines + 1);
		}
	} //}}}

	//{{{ expandAllFolds() method
	/**
	 * Expands all folds.
	 * @since jEdit 4.0pre1
	 */
	public void expandAllFolds()
	{
		try
		{
			buffer.writeLock();

			virtualLineCount = buffer.getLineCount();
			for(int i = 0; i < virtualLineCount; i++)
			{
				buffer._setLineVisible(i,index,true);
			}
			foldStructureChanged();
		}
		finally
		{
			buffer.writeUnlock();
		}
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
		try
		{
			buffer.writeLock();

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
					buffer._setLineVisible(i,index,true);
					newVirtualLineCount++;
				}
				else
					buffer._setLineVisible(i,index,false);
			}

			virtualLineCount = newVirtualLineCount;
		}
		finally
		{
			buffer.writeUnlock();
		}

		foldStructureChanged();
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

		if(buffer.getLineCount() == 1)
			buffer._setLineVisible(0,index,true);

		virtualLineCount = 0;
		for(int i = 0; i < buffer.getLineCount(); i++)
		{
			if(buffer._isLineVisible(i,index))
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
		//{{{ Find fold start of this line
		int foldLevel = buffer.getFoldLevel(startLine);
		boolean visible = true;
		if(startLine != 0)
		{
			for(int i = startLine; i > 0; i--)
			{
				if(buffer.isFoldStart(i - 1) && buffer.getFoldLevel(i) <= foldLevel)
				{
					visible = buffer._isLineVisible(i,index);
					break;
				}
			}
		} //}}}

		int collapseFolds = buffer.getIntegerProperty("collapseFolds",0);
		if(collapseFolds != 0)
			collapseFolds = (collapseFolds - 1) * buffer.getIndentSize() + 1;

		int newVirtualLineCount = virtualLineCount;

		boolean seenVisibleLine = (startLine != 0);
		int threshold = Math.max(foldLevel + 1,collapseFolds);
		for(int i = startLine; i < startLine + numLines; i++)
		{
			boolean _visible;
			if(collapseFolds == 0)
				_visible = visible;
			else if(buffer.getFoldLevel(i) >= threshold)
				_visible = false;
			else
				_visible = visible;

			if(!seenVisibleLine && !_visible)
				_visible = true;

			if(_visible)
			{
				seenVisibleLine = true;
				newVirtualLineCount++;
			}

			buffer._setLineVisible(i,index,_visible);
		}

		virtualLineCount = newVirtualLineCount;

		if(lastPhysical >= startLine)
			lastPhysical = lastVirtual = 0;
	} //}}}

	//{{{ _linesRemoved() method
	/**
	 * Do not call this method. The only reason it is public is so
	 * that the <code>Buffer</code> class can call it.
	 */
	public void _linesRemoved(int startLine, int numLines)
	{
		for(int i = startLine; i < startLine + numLines; i++)
		{
			// note that Buffer calls linesRemoved()
			// of FoldVisibilityManagers before the
			// line info array is shrunk.
			if(buffer._isLineVisible(i,index))
				virtualLineCount--;
		}

		if(lastPhysical >= startLine)
			lastPhysical = lastVirtual = 0;
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

	//{{{ foldStructureChanged() method
	private void foldStructureChanged()
	{
		lastPhysical = lastVirtual = 0;
		textArea.foldStructureChanged();
	} //}}}

	//}}}
}
