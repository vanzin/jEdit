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
import java.util.*;
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
	//{{{ Static part

	//{{{ getDisplayManager() method
	static DisplayManager getDisplayManager(Buffer buffer,
		JEditTextArea textArea)
	{
		List l = (List)bufferMap.get(buffer);
		if(l == null || l.size() == 0)
		{
			if(l == null)
			{
				l = new LinkedList();
				bufferMap.put(buffer,l);
			}
			return new DisplayManager(buffer,textArea);
		}
		else
			return (DisplayManager)l.remove(0);
	} //}}}

	//{{{ releaseDisplayManager() method
	static void releaseDisplayManager(DisplayManager dmgr)
	{
		List l = (List)bufferMap.get(dmgr.buffer);
		if(l == null)
			/* buffer closed! */;
		else
			l.add(dmgr);
	} //}}}

	//{{{ bufferClosed() method
	public static void bufferClosed(Buffer buffer)
	{
		bufferMap.remove(buffer);
	} //}}}

	//{{{ textAreaDisposed() method
	static void textAreaDisposed(JEditTextArea textArea)
	{
		Iterator biter = bufferMap.values().iterator();
		while(biter.hasNext())
		{
			List l = (List)biter.next();
			Iterator liter = l.iterator();
			while(liter.hasNext())
			{
				DisplayManager dmgr = (DisplayManager)
					liter.next();
				if(dmgr.textArea == textArea)
				{
					dmgr.dispose();
					liter.remove();
				}
			}
		}
	} //}}}

	//{{{ _setScreenLineCount() method
	private static void _setScreenLineCount(Buffer buffer, int line,
		int oldCount, int count)
	{
		Iterator iter = ((List)bufferMap.get(buffer)).iterator();
		while(iter.hasNext())
		{
			((DisplayManager)iter.next())._setScreenLineCount(line,oldCount,count);
		}
	} //}}}

	//{{{ _notifyScreenLineChanges() method
	private static void _notifyScreenLineChanges(Buffer buffer)
	{
		Iterator iter = ((List)bufferMap.get(buffer)).iterator();
		while(iter.hasNext())
		{
			((DisplayManager)iter.next())._notifyScreenLineChanges();
		}
	} //}}}

	private static Map bufferMap = new HashMap();
	//}}}

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

		_notifyScreenLineChanges();
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

		_notifyScreenLineChanges();
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

			showLineRange(0,buffer.getLineCount() - 1);
		}
		finally
		{
			buffer.writeUnlock();
		}

		_notifyScreenLineChanges();
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

			if(buffer.getFoldHandler() instanceof IndentFoldHandler)
				foldLevel = (foldLevel - 1) * buffer.getIndentSize() + 1;

			showLineRange(0,buffer.getLineCount() - 1);

			/* this ensures that the first line is always visible */
			boolean seenVisibleLine = false;

			int firstInvisible = 0;

			for(int i = 0; i < buffer.getLineCount(); i++)
			{
				if(!seenVisibleLine || buffer.getFoldLevel(i) < foldLevel)
				{
					if(firstInvisible != i)
					{
						hideLineRange(firstInvisible,
							i - 1);
					}
					firstInvisible = i + 1;
					seenVisibleLine = true;
				}
			}

			if(firstInvisible != buffer.getLineCount())
				hideLineRange(firstInvisible,buffer.getLineCount() - 1);
		}
		finally
		{
			buffer.writeUnlock();
		}

		_notifyScreenLineChanges();
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

		// Hack... need a more direct way of obtaining a view?
		// JEditTextArea.getView() method?
		GUIUtilities.getView(textArea).getStatus().setMessageAndClear(
			jEdit.getProperty("view.status.narrow"));

		_notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ Package-private members
	boolean softWrap;
	int wrapMargin;
	FirstLine firstLine;
	ScrollLineCount scrollLineCount;

	//{{{ init() method
	void init()
	{
		if(!initialized)
		{
			initialized = true;
			if(buffer.isLoaded())
				bufferChangeHandler.foldHandlerChanged(buffer);
		}
		else
		{
			textArea.updateScrollBars();
			textArea.recalculateLastPhysicalLine();
		}
	} //}}}

	//{{{ setScreenLineCount() method
	/**
	 * Sets the number of screen lines that the specified physical line
	 * is split into.
	 * @since jEdit 4.2pre1
	 */
	void setScreenLineCount(int line, int count)
	{
		try
		{
			buffer.writeLock();
			int oldCount = offsetMgr.getScreenLineCount(line);
			offsetMgr.setScreenLineCount(line,count);
			// this notifies each display manager editing this
			// buffer of the screen line count change
			if(count != oldCount)
				_setScreenLineCount(buffer,line,oldCount,count);
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
	private boolean initialized;
	private Buffer buffer;
	private OffsetManager offsetMgr;
	private JEditTextArea textArea;
	private int index;
	private BufferChangeHandler bufferChangeHandler;

	//{{{ DisplayManager constructor
	private DisplayManager(Buffer buffer, JEditTextArea textArea)
	{
		this.buffer = buffer;
		this.offsetMgr = buffer._getOffsetManager();
		this.textArea = textArea;
		this.index = buffer._displayLock();

		scrollLineCount = new ScrollLineCount();
		firstLine = new FirstLine();

		bufferChangeHandler = new BufferChangeHandler();
		buffer.addBufferChangeListener(bufferChangeHandler);
	} //}}}

	//{{{ dispose() method
	private void dispose()
	{
		buffer._displayUnlock(index);
		buffer.removeBufferChangeListener(bufferChangeHandler);
	} //}}}

	//{{{ showLineRange() method
	private void showLineRange(int start, int end)
	{
		for(int i = start; i <= end; i++)
		{
			if(!offsetMgr.isLineVisible(i,index))
			{
				int screenLines = offsetMgr
					.getScreenLineCount(i);
				if(firstLine.physicalLine >= i)
				{
					firstLine.scrollLine += screenLines;
					firstLine.callChanged = true;
				}
				scrollLineCount.scrollLine += screenLines;
				scrollLineCount.callChanged = true;
			}
			offsetMgr.setLineVisible(i,index,true);
		}
	} //}}}

	//{{{ hideLineRange() method
	private void hideLineRange(int start, int end)
	{
		for(int i = start; i <= end; i++)
		{
			if(offsetMgr.isLineVisible(i,index))
			{
				int screenLines = offsetMgr
					.getScreenLineCount(i);
				if(firstLine.physicalLine >= i)
				{
					firstLine.physicalLine -= screenLines;
					firstLine.callChanged = true;
				}
				scrollLineCount.scrollLine -= screenLines;
				scrollLineCount.callChanged = true;
			}
			offsetMgr.setLineVisible(i,index,false);
		}
	} //}}}

	//{{{ _setScreenLineCount() method
	private void _setScreenLineCount(int line, int oldCount, int count)
	{
		if(!isLineVisible(line))
			return;

		if(firstLine.physicalLine >= line)
		{
			if(firstLine.physicalLine == line)
				firstLine.callChanged = true;
			else
			{
				firstLine.scrollLine += (count - oldCount);
				firstLine.callChanged = true;
			}
		}

		scrollLineCount.scrollLine += (count - oldCount);
		scrollLineCount.callChanged = true;
	} //}}}

	//{{{ _notifyScreenLineChanges() method
	private void _notifyScreenLineChanges()
	{
		if(firstLine.callChanged)
		{
			firstLine.callChanged = false;
			firstLine.changed();
		}

		if(scrollLineCount.callChanged)
		{
			scrollLineCount.callChanged = false;
			scrollLineCount.changed();
		}
	} //}}}

	//}}}

	//{{{ Anchor class
	static abstract class Anchor
	{
		int physicalLine;
		int scrollLine;
		boolean callChanged;

		abstract void reset();
		abstract void changed();
	} //}}}

	//{{{ ScrollLineCount class
	class ScrollLineCount extends Anchor
	{
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

			scrollLine = 0;
			for(int i = 0; i < offsetMgr.getLineCount(); i++)
			{
				if(isLineVisible(i))
					scrollLine += getScreenLineCount(i);
			}
			physicalLine = offsetMgr.getLineCount();

			firstLine.ensurePhysicalLineIsVisible();

			textArea.recalculateLastPhysicalLine();
			textArea.updateScrollBars();
		} //}}}
	} //}}}

	//{{{ FirstLine class
	class FirstLine extends Anchor
	{
		int skew;

		//{{{ changed() method
		public void changed()
		{
			//{{{ Debug code
			if(Debug.SCROLL_DEBUG)
			{
				Log.log(Log.DEBUG,this,"changed() before: "
					+ physicalLine + ":" + scrollLine);
			} //}}}

			ensurePhysicalLineIsVisible();

			int screenLines = getScreenLineCount(physicalLine);
			if(skew >= screenLines)
				skew = screenLines - 1;

			//{{{ Debug code
			if(Debug.VERIFY_FIRST_LINE)
			{
				int verifyScrollLine = 0;

				for(int i = 0; i < buffer.getLineCount(); i++)
				{
					if(!isLineVisible(i))
						continue;
	
					if(i >= physicalLine)
						break;
	
					verifyScrollLine += getScreenLineCount(i);
				}

				if(verifyScrollLine != scrollLine)
				{
					Exception ex = new Exception(scrollLine + ":" + verifyScrollLine);
					Log.log(Log.ERROR,this,ex);
					new org.gjt.sp.jedit.gui.BeanShellErrorDialog(null,ex);
				}
			}

			if(Debug.SCROLL_DEBUG)
			{
				Log.log(Log.DEBUG,this,"changed() after: "
					+ physicalLine + ":" + scrollLine);
			} //}}}

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
			{
				Log.log(Log.DEBUG,this,"physDown() start: "
					+ physicalLine + ":" + scrollLine);
			}

			skew = 0;

			if(!isLineVisible(physicalLine))
			{
				int lastVisibleLine = getLastVisibleLine();
				if(physicalLine > lastVisibleLine)
					physicalLine = lastVisibleLine;
				else
				{
					int nextPhysicalLine = getNextVisibleLine(physicalLine);
					amount -= (nextPhysicalLine - physicalLine);
					physicalLine = nextPhysicalLine;
					scrollLine += getScreenLineCount(physicalLine);
				}
			}

			for(;;)
			{
				int nextPhysicalLine = getNextVisibleLine(
					physicalLine);
				if(nextPhysicalLine == -1)
					break;
				else if(nextPhysicalLine > physicalLine + amount)
					break;
				else
				{
					scrollLine += getScreenLineCount(physicalLine);
					amount -= (nextPhysicalLine - physicalLine);
					physicalLine = nextPhysicalLine;
				}
			}

			if(Debug.SCROLL_DEBUG)
			{
				Log.log(Log.DEBUG,this,"physDown() end: "
					+ physicalLine + ":" + scrollLine);
			}

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
			{
				Log.log(Log.DEBUG,this,"physUp() start: "
					+ physicalLine + ":" + scrollLine);
			}

			skew = 0;

			if(!isLineVisible(physicalLine))
			{
				int firstVisibleLine = getFirstVisibleLine();
				if(physicalLine < firstVisibleLine)
					physicalLine = firstVisibleLine;
				else
				{
					int prevPhysicalLine = getPrevVisibleLine(physicalLine);
					amount -= (physicalLine - prevPhysicalLine);
				}
			}

			for(;;)
			{
				int prevPhysicalLine = getPrevVisibleLine(
					physicalLine);
				if(prevPhysicalLine == -1)
					break;
				else if(prevPhysicalLine < physicalLine - amount)
					break;
				else
				{
					scrollLine -= getScreenLineCount(
						prevPhysicalLine);
					amount -= (physicalLine - prevPhysicalLine);
					physicalLine = prevPhysicalLine;
				}
			}

			if(Debug.SCROLL_DEBUG)
			{
				Log.log(Log.DEBUG,this,"physUp() end: "
					+ physicalLine + ":" + scrollLine);
			}

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
		} //}}}

		//{{{ scrollUp() method
		// scroll up by screen line amount
		void scrollUp(int amount)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"scrollUp()");

			ensurePhysicalLineIsVisible();

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

	//{{{ BufferChangeHandler class
	class BufferChangeHandler extends BufferChangeAdapter
	{
		//{{{ foldHandlerChanged() method
		public void foldHandlerChanged(Buffer buffer)
		{
			System.err.println("f here in event: " + buffer);
			firstLine.reset();
			scrollLineCount.reset();
			int collapseFolds = buffer.getIntegerProperty(
				"collapseFolds",0);
			if(collapseFolds == 0)
			{
				expandAllFolds();
			}
			else
			{
				expandFolds(collapseFolds);
			}
		} //}}}

		//{{{ wrapModeChanged() method
		public void wrapModeChanged(Buffer buffer)
		{
			System.err.println("w here in event: " + buffer);
			firstLine.reset();
			scrollLineCount.reset();
		} //}}}

		//{{{ contentInserted() method
		public void contentInserted(Buffer buffer, int startLine,
			int offset, int numLines, int length)
		{
			if(numLines != 0 && buffer.isLoaded())
			{
				contentInserted(firstLine,startLine,numLines);
				contentInserted(scrollLineCount,startLine,numLines);
			}
		} //}}}

		//{{{ preContentRemoved() method
		public void preContentRemoved(Buffer buffer, int startLine,
			int offset, int numLines, int length)
		{
			if(numLines != 0 && buffer.isLoaded())
			{
				preContentRemoved(firstLine,startLine,numLines);
				preContentRemoved(scrollLineCount,startLine,numLines);
			}
		} //}}}

		//{{{ contentInserted() method
		private void contentInserted(Anchor anchor, int startLine,
			int numLines)
		{
			if(anchor.physicalLine >= startLine)
			{
				if(anchor.physicalLine != startLine)
					anchor.physicalLine += numLines;
				anchor.callChanged = true;
			}
		} //}}}

		//{{{ preContentRemoved() method
		public void preContentRemoved(Anchor anchor, int startLine,
			int numLines)
		{
			if(anchor.physicalLine >= startLine)
			{
				if(anchor.physicalLine == startLine)
					anchor.callChanged = true;
				else
				{
					int end = Math.min(startLine + numLines,
						anchor.physicalLine);
					for(int i = startLine; i < end; i++)
					{
						if(isLineVisible(i))
						{
							anchor.scrollLine -=
								offsetMgr
								.getScreenLineCount(i);
						}
						anchor.physicalLine--;
						anchor.callChanged = true;
					}
				}
			}
		} //}}}
	} //}}}
}
