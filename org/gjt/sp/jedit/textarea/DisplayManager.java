/*
 * DisplayManager.java - Low-level text display
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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

//{{{ Imports
import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Manages low-level text display tasks.
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
public class DisplayManager
{
	//{{{ isNarrowed() method
	/**
	 * Returns if the buffer has been narrowed.
	 * @since jEdit 4.2pre1
	 */
	public boolean isNarrowed()
	{
		return narrowed;
	} //}}}

	//{{{ isLineVisible() method
	/**
	 * Returns if the specified line is visible.
	 * @param line A physical line index
	 * @since jEdit 4.2pre1
	 */
	public final boolean isLineVisible(int line)
	{
		return offsetMgr.isLineVisible(line,index);
	} //}}}

	public static long scanCount, scannedLines;

	//{{{ getFirstVisibleLine() method
	/**
	 * Returns the physical line number of the first visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getFirstVisibleLine()
	{
		scanCount++;
		try
		{
			buffer.readLock();

			for(int i = 0; i < buffer.getLineCount(); i++)
			{
				if(isLineVisible(i))
				{
					scannedLines += i + 1;
					return i;
				}
			}
		}
		finally
		{
			buffer.readUnlock();
		}

		// can't happen?
		return -1;
	} //}}}

	//{{{ getLastVisibleLine() method
	/**
	 * Returns the physical line number of the last visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getLastVisibleLine()
	{
		scanCount++;

		try
		{
			buffer.readLock();

			for(int i = buffer.getLineCount() - 1; i >= 0; i--)
			{
				if(isLineVisible(i))
				{
					scannedLines += (buffer.getLineCount() - i);
					return i;
				}
			}
		}
		finally
		{
			buffer.readUnlock();
		}

		// can't happen?
		return -1;
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		if(line < 0 || line >= offsetMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		scanCount++;

		try
		{
			buffer.readLock();

			if(line == buffer.getLineCount() - 1)
				return -1;

			for(int i = line + 1; i < buffer.getLineCount(); i++)
			{
				if(isLineVisible(i))
				{
					scannedLines += (i - line);
					return i;
				}
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
		if(line < 0 || line >= offsetMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		scanCount++;

		try
		{
			buffer.readLock();

			if(line == 0)
				return -1;

			for(int i = line - 1; i >= 0; i--)
			{
				if(isLineVisible(i))
				{
					scannedLines += (line - i);
					return i;
				}
			}
			return -1;
		}
		finally
		{
			buffer.readUnlock();
		}
	} //}}}

	//{{{ getScreenLineCount() method
	public final int getScreenLineCount(int line)
	{
		if(offsetMgr.isScreenLineCountValid(line))
			return offsetMgr.getScreenLineCount(line);
		else
		{
			int newCount = textArea.chunkCache.getLineInfosForPhysicalLine(line).length;

			offsetMgr.setScreenLineCount(line,newCount);
			return newCount;
		}
	} //}}}

	//{{{ getScrollLineCount() method
	public final int getScrollLineCount()
	{
		return scrollLineCount.scrollLine;
	} //}}}

	//{{{ collapseFold() method
	/**
	 * Collapses the fold at the specified physical line index.
	 * @param line A physical line index
	 * @since jEdit 4.2pre1
	 */
	public void collapseFold(int line)
	{
		int lineCount = buffer.getLineCount();
		int start = 0;
		int end = lineCount - 1;

		try
		{
			buffer.writeLock();

			// if the caret is on a collapsed fold, collapse the
			// parent fold
			if(line != 0
				&& line != buffer.getLineCount() - 1
				&& buffer.isFoldStart(line)
				&& !isLineVisible(line + 1))
			{
				line--;
			}

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

			// Collapse the fold...
			hideLineRange(start,end);
		}
		finally
		{
			buffer.writeUnlock();
		}

		offsetMgr.notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ expandFold() method
	/**
	 * Expands the fold at the specified physical line index.
	 * @param line A physical line index
	 * @param fully If true, all subfolds will also be expanded
	 * @since jEdit 4.2pre1
	 */
	public int expandFold(int line, boolean fully)
	{
		// the first sub-fold. used by JEditTextArea.expandFold().
		int returnValue = -1;

		int lineCount = buffer.getLineCount();
		int start = 0;
		int end = lineCount - 1;

		try
		{
			buffer.writeLock();

			int initialFoldLevel = buffer.getFoldLevel(line);

			//{{{ Find fold start and fold end...
			if(line != lineCount - 1
				&& isLineVisible(line)
				&& !isLineVisible(line + 1)
				&& buffer.getFoldLevel(line + 1) > initialFoldLevel)
			{
				// this line is the start of a fold
				start = line + 1;

				for(int i = line + 1; i < lineCount; i++)
				{
					if(/* isLineVisible(i) && */
						buffer.getFoldLevel(i) <= initialFoldLevel)
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
					if(isLineVisible(i) && buffer.getFoldLevel(i) < initialFoldLevel)
					{
						start = i + 1;
						ok = true;
						break;
					}
				}

				if(!ok)
				{
					// no folds in buffer
					return -1;
				}

				for(int i = line + 1; i < lineCount; i++)
				{
					if((isLineVisible(i) &&
						buffer.getFoldLevel(i) < initialFoldLevel)
						|| i == getLastVisibleLine())
					{
						end = i - 1;
						break;
					}
				}
			} //}}}

			//{{{ Expand the fold...
			if(fully)
			{
				showLineRange(start,end);
			}
			else
			{
				// we need a different value of initialFoldLevel here!
				initialFoldLevel = buffer.getFoldLevel(start);

				int firstVisible = start;

				for(int i = start; i <= end; i++)
				{
					if(buffer.getFoldLevel(i) > initialFoldLevel)
					{
						if(returnValue == -1
							&& i != 0
							&& buffer.isFoldStart(i - 1))
						{
							returnValue = i - 1;
						}

						if(firstVisible != i)
						{
							showLineRange(firstVisible,i - 1);
						}
						firstVisible = i + 1;
					}
				}

				if(firstVisible != end + 1)
					showLineRange(firstVisible,end);

				if(!isLineVisible(line))
				{
					// this is a hack, and really needs to be done better.
					expandFold(line,false);
					return returnValue;
				}
			} //}}}
		}
		finally
		{
			buffer.writeUnlock();
		}

		offsetMgr.notifyScreenLineChanges();
		textArea.foldStructureChanged();

		return returnValue;
	} //}}}

	//{{{ expandAllFolds() method
	/**
	 * Expands all folds.
	 * @since jEdit 4.2pre1
	 */
	public void expandAllFolds()
	{
		try
		{
			buffer.writeLock();

			narrowed = false;

			showLineRange(0,buffer.getLineCount() - 1);
		}
		finally
		{
			buffer.writeUnlock();
		}

		offsetMgr.notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ expandFolds() method
	/**
	 * This method should only be called from <code>actions.xml</code>.
	 * @since jEdit 4.2pre1
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
	 * @since jEdit 4.2pre1
	 */
	public void expandFolds(int foldLevel)
	{
		try
		{
			buffer.writeLock();

			narrowed = false;

			if(buffer.getFoldHandler() instanceof IndentFoldHandler)
				foldLevel = (foldLevel - 1) * buffer.getIndentSize() + 1;

			/* this ensures that the first line is always visible */
			boolean seenVisibleLine = false;

			for(int i = 0; i < buffer.getLineCount(); i++)
			{
				if(!seenVisibleLine || buffer.getFoldLevel(i) < foldLevel)
				{
					seenVisibleLine = true;
					setLineVisible(i,true);
				}
				else
					setLineVisible(i,false);
			}
		}
		finally
		{
			buffer.writeUnlock();
		}

		offsetMgr.notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ narrow() method
	/**
	 * Narrows the visible portion of the buffer to the specified
	 * line range.
	 * @param start The first line
	 * @param end The last line
	 * @since jEdit 4.2pre1
	 */
	public void narrow(int start, int end)
	{
		if(start > end || start < 0 || end >= buffer.getLineCount())
			throw new ArrayIndexOutOfBoundsException(start + ", " + end);

		if(start < getFirstVisibleLine() || end > getLastVisibleLine())
			expandAllFolds();

		hideLineRange(0,start - 1);
		hideLineRange(end + 1,buffer.getLineCount() - 1);

		// if we narrowed to a single collapsed fold
		if(getNextVisibleLine(start) == -1)
			expandFold(start,false);

		narrowed = true;

		// Hack... need a more direct way of obtaining a view?
		// JEditTextArea.getView() method?
		GUIUtilities.getView(textArea).getStatus().setMessageAndClear(
			jEdit.getProperty("view.status.narrow"));

		offsetMgr.notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ Package-private members
	boolean softWrap;
	int wrapMargin;
	FirstLine firstLine;
	ScrollLineCount scrollLineCount;

	//{{{ DisplayManager constructor
	DisplayManager(Buffer buffer, JEditTextArea textArea)
	{
		this.buffer = buffer;
		this.offsetMgr = buffer._getOffsetManager();
		this.textArea = textArea;
		this.index = buffer._displayLock();

		scrollLineCount = new ScrollLineCount(index);
		offsetMgr.addAnchor(scrollLineCount);

		firstLine = new FirstLine(index);
		offsetMgr.addAnchor(firstLine);
	} //}}}

	//{{{ dispose() method
	void dispose()
	{
		offsetMgr.removeAnchor(scrollLineCount);
		offsetMgr.removeAnchor(firstLine);
		offsetMgr = null;

		buffer._displayUnlock(index);
		buffer = null;
	} //}}}

	//{{{ notifyScreenLineChanges() method
	void notifyScreenLineChanges()
	{
		offsetMgr.notifyScreenLineChanges();
	} //}}}

	//{{{ setScreenLineCount() method
	/**
	 * Sets the number of screen lines that the specified physical line
	 * is split into.
	 * @since jEdit 4.2pre1
	 */
	public void setScreenLineCount(int line, int count)
	{
		try
		{
			buffer.writeLock();
			offsetMgr.setScreenLineCount(line,count);
		}
		finally
		{
			buffer.writeUnlock();
		}
	} //}}}

	//{{{ updateWrapSettings() method
	void updateWrapSettings()
	{
		String wrap = buffer.getStringProperty("wrap");
		softWrap = wrap.equals("soft");
		if(textArea.maxLineLen <= 0)
		{
			softWrap = false;
			wrapMargin = 0;
		}
		else
		{
			// stupidity
			char[] foo = new char[textArea.maxLineLen];
			for(int i = 0; i < foo.length; i++)
			{
				foo[i] = ' ';
			}
			TextAreaPainter painter = textArea.getPainter();
			wrapMargin = (int)painter.getFont().getStringBounds(
				foo,0,foo.length,
				painter.getFontRenderContext())
				.getWidth();
		}
	} //}}}

	//}}}

	//{{{ Private members
	private Buffer buffer;
	private OffsetManager offsetMgr;
	private JEditTextArea textArea;
	private int index;
	private boolean narrowed;

	//{{{ setLineVisible() method
	private final void setLineVisible(int line, boolean visible)
	{
		offsetMgr.setLineVisible(line,index,visible);
	} //}}}

	//{{{ showLineRange() method
	private void showLineRange(int start, int end)
	{
		for(int i = start; i <= end; i++)
		{
			offsetMgr.setLineVisible(i,index,true);
		}
	} //}}}

	//{{{ hideLineRange() method
	private void hideLineRange(int start, int end)
	{
		for(int i = start; i <= end; i++)
		{
			offsetMgr.setLineVisible(i,index,false);
		}
	} //}}}

	//}}}

	//{{{ ScrollLineCount class
	class ScrollLineCount extends OffsetManager.Anchor
	{
		//{{{ ScrollLineCount constructor
		ScrollLineCount(int index)
		{
			super(index);
		} //}}}

		//{{{ changed() method
		public void changed()
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"changed()");
			textArea.updateScrollBars();
			textArea.recalculateLastPhysicalLine();
		} //}}}

		//{{{ reset() method
		public void reset()
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"reset()");

			updateWrapSettings();

			offsetMgr.removeAnchor(this);
			physicalLine = offsetMgr.getLineCount();
			scrollLine = 0;
			for(int i = 0; i < physicalLine; i++)
			{
				if(isLineVisible(i))
					scrollLine += getScreenLineCount(i);
			}
			offsetMgr.addAnchor(this);

			firstLine.ensurePhysicalLineIsVisible();

			textArea.recalculateLastPhysicalLine();
			textArea.updateScrollBars();
		} //}}}
	} //}}}

	//{{{ FirstLine class
	class FirstLine extends OffsetManager.Anchor
	{
		int skew;

		//{{{ FirstLine constructor
		FirstLine(int index)
		{
			super(index);
		} //}}}

		//{{{ changed() method
		public void changed()
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"changed()");

			ensurePhysicalLineIsVisible();

			int screenLines = getScreenLineCount(physicalLine);
			if(skew >= screenLines)
				skew = screenLines - 1;

			textArea.updateScrollBars();
			textArea.recalculateLastPhysicalLine();
		} //}}}

		//{{{ reset() method
		public void reset()
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"reset()");

			String wrap = buffer.getStringProperty("wrap");
			softWrap = wrap.equals("soft");
			if(textArea.maxLineLen <= 0)
			{
				softWrap = false;
				wrapMargin = 0;
			}
			else
			{
				// stupidity
				char[] foo = new char[textArea.maxLineLen];
				for(int i = 0; i < foo.length; i++)
				{
					foo[i] = ' ';
				}
				TextAreaPainter painter = textArea.getPainter();
				wrapMargin = (int)painter.getFont().getStringBounds(
					foo,0,foo.length,
					painter.getFontRenderContext())
					.getWidth();
			}

			scrollLine = 0;

			int i = 0;

			for(; i < buffer.getLineCount(); i++)
			{
				if(!isLineVisible(i))
					continue;

				if(i >= physicalLine)
					break;

				scrollLine += getScreenLineCount(i);
			}

			physicalLine = i;

			int screenLines = getScreenLineCount(physicalLine);
			if(skew >= screenLines)
				skew = screenLines - 1;

			textArea.updateScrollBars();
		} //}}}

		//{{{ physDown() method
		// scroll down by physical line amount
		void physDown(int amount, int screenAmount)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"physDown()");

			skew = 0;

			offsetMgr.removeAnchor(this);

			int newScrollLine = scrollLine;
			for(int i = 0; i < amount; i++)
			{
				if(physicalLine + i >= offsetMgr.getLineCount())
				{
					amount = i;
					break;
				}
				if(isLineVisible(physicalLine + i))
					newScrollLine += getScreenLineCount(physicalLine + i);
			}

			if(newScrollLine != scrollLine)
			{
				scrollLine = newScrollLine;
				physicalLine += amount;
			}

			offsetMgr.addAnchor(this);

			// JEditTextArea.scrollTo() needs this to simplify
			// its code
			if(screenAmount != 0)
			{
				if(screenAmount < 0)
					scrollUp(-screenAmount);
				else
					scrollDown(screenAmount);
			}
		} //}}}

		//{{{ physUp() method
		// scroll up by physical line amount
		void physUp(int amount, int screenAmount)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"physUp()");

			skew = 0;

			offsetMgr.removeAnchor(this);

			int newScrollLine = scrollLine;
			for(int i = 0; i < amount; i++)
			{
				if(physicalLine - i == 0)
				{
					amount = i;
					break;
				}
				if(isLineVisible(physicalLine - i))
					newScrollLine -= getScreenLineCount(physicalLine - i - 1);
			}

			if(newScrollLine != scrollLine)
			{
				scrollLine = newScrollLine;
				physicalLine -= amount;
			}

			offsetMgr.addAnchor(this);

			// JEditTextArea.scrollTo() needs this to simplify
			// its code
			if(screenAmount != 0)
			{
				if(screenAmount < 0)
					scrollUp(-screenAmount);
				else
					scrollDown(screenAmount);
			}
		} //}}}

		//{{{ scrollDown() method
		// scroll down by screen line amount
		void scrollDown(int amount)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"scrollDown()");

			ensurePhysicalLineIsVisible();

			offsetMgr.removeAnchor(this);

			amount += skew;

			skew = 0;

			while(amount > 0)
			{
				int screenLines = getScreenLineCount(physicalLine);
				if(amount < screenLines)
				{
					skew = amount;
					break;
				}
				else
				{
					int nextLine = getNextVisibleLine(physicalLine);
					if(nextLine == -1)
						break;
					boolean visible = isLineVisible(physicalLine);
					physicalLine = nextLine;
					if(visible)
					{
						amount -= screenLines;
						scrollLine += screenLines;
					}
				}
			}

			offsetMgr.addAnchor(this);
		} //}}}

		//{{{ scrollUp() method
		// scroll up by screen line amount
		void scrollUp(int amount)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"scrollUp()");

			ensurePhysicalLineIsVisible();

			offsetMgr.removeAnchor(this);

			if(amount <= skew)
			{
				skew -= amount;
			}
			else
			{
				amount -= skew;
				skew = 0;

				while(amount > 0)
				{
					int prevLine = getPrevVisibleLine(physicalLine);
					if(prevLine == -1)
						break;
					physicalLine = prevLine;

					int screenLines = getScreenLineCount(physicalLine);
					scrollLine -= screenLines;
					if(amount < screenLines)
					{
						skew = screenLines - amount;
						break;
					}
					else
						amount -= screenLines;
				}
			}

			offsetMgr.addAnchor(this);
		} //}}}

		//{{{ ensurePhysicalLineIsVisible() method
		private void ensurePhysicalLineIsVisible()
		{
			if(!isLineVisible(physicalLine))
			{
				if(physicalLine > getLastVisibleLine())
				{
					physicalLine = getPrevVisibleLine(physicalLine);
					scrollLine -= getScreenLineCount(physicalLine);
				}
				else
				{
					physicalLine = getNextVisibleLine(physicalLine);
					scrollLine += getScreenLineCount(physicalLine);
				}
			}
		} //}}}
	} //}}}
}
