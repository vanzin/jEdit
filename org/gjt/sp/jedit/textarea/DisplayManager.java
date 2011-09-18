/*
 * DisplayManager.java - Low-level text display
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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
import java.awt.Toolkit;
import java.util.*;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.util.Log;
//}}}

/**
 * Manages low-level text display tasks, such as folding.
 * 
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
public class DisplayManager
{
	//{{{ Static part

	//{{{ getDisplayManager() method
	static DisplayManager getDisplayManager(JEditBuffer buffer,
		TextArea textArea)
	{
		List<DisplayManager> l = bufferMap.get(buffer);
		if(l == null)
		{
			l = new LinkedList<DisplayManager>();
			bufferMap.put(buffer,l);
		}

		/* An existing display manager's fold visibility map
		that a new display manager will inherit */
		DisplayManager copy = null;
		Iterator<DisplayManager> liter = l.iterator();
		DisplayManager dmgr;
		while(liter.hasNext())
		{
			dmgr = liter.next();
			copy = dmgr;
			if(!dmgr.inUse && dmgr.textArea == textArea)
			{
				dmgr.inUse = true;
				return dmgr;
			}
		}

		// if we got here, no unused display manager in list
		dmgr = new DisplayManager(buffer,textArea,copy);
		dmgr.inUse = true;
		l.add(dmgr);

		return dmgr;
	} //}}}

	//{{{ release() method
	void release()
	{
		inUse = false;
	} //}}}

	//{{{ bufferClosed() method
	public static void bufferClosed(JEditBuffer buffer)
	{
		bufferMap.remove(buffer);
	} //}}}

	//{{{ textAreaDisposed() method
	static void textAreaDisposed(TextArea textArea)
	{
		for (List<DisplayManager> l : bufferMap.values())
		{
			Iterator<DisplayManager> liter = l.iterator();
			while(liter.hasNext())
			{
				DisplayManager dmgr = liter.next();
				if(dmgr.textArea == textArea)
				{
					dmgr.dispose();
					liter.remove();
				}
			}
		}
	} //}}}

	private static final Map<JEditBuffer, List<DisplayManager>> bufferMap = new HashMap<JEditBuffer, List<DisplayManager>>();
	//}}}

	//{{{ getBuffer() method
	/**
	 * @since jEdit 4.3pre3
	 */
	public JEditBuffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ isLineVisible() method
	/**
	 * Returns if the specified line is visible.
	 * @param line A physical line index
	 * @since jEdit 4.2pre1
	 */
	public final boolean isLineVisible(int line)
	{
		return folds.search(line) % 2 == 0;
	} //}}}

	//{{{ getFirstVisibleLine() method
	/**
	 * Returns the physical line number of the first visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getFirstVisibleLine()
	{
		return folds.first();
	} //}}}

	//{{{ getLastVisibleLine() method
	/**
	 * Returns the physical line number of the last visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getLastVisibleLine()
	{
		return folds.last();
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index,
	 * or (-1) if there is no next visible line.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		if(line < 0 || line >= buffer.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		return folds.next(line);
	} //}}}

	//{{{ getPrevVisibleLine() method
	/**
	 * Returns the previous visible line before the specified line index.
	 * @param line a physical line index
	 * @return the previous visible physical line or -1 if there is no visible line
	 * @since jEdit 4.0pre1
	 */
	public int getPrevVisibleLine(int line)
	{
		if(line < 0 || line >= buffer.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		return folds.prev(line);
	} //}}}

	//{{{ getScreenLineCount() method
	/**
	 * Returns how many screen lines contains the given physical line.
	 * It can be greater than 1 when using soft wrap
	 *
	 * @param line the physical line
	 * @return the screen line count
	 */
	public final int getScreenLineCount(int line)
	{
		updateScreenLineCount(line);
		return screenLineMgr.getScreenLineCount(line);
	} //}}}

	//{{{ getScrollLineCount() method
	/**
	 * Returns the number of displayable lines
	 * It can be greater than the number of lines of the buffer when using
	 * soft wrap (a line can count for n lines), or when using folding, if
	 * the foldings are collapsed
	 * @return the number of displayable lines
	 */
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
		int end = lineCount - 1;

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
		int start = 0;
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

		notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ expandFold() method
	/**
	 * Expands the fold at the specified physical line index.
	 * @param line A physical line index
	 * @param fully If true, all subfolds will also be expanded
	 * @return the line number of the first subfold, or -1 if none
	 * @since jEdit 4.2pre1
	 */
	public int expandFold(int line, boolean fully)
	{
		MutableInteger firstSubfold = new MutableInteger(-1);
		boolean unfolded = _expandFold(line, fully, firstSubfold);
		
		if (unfolded)
			textArea.foldStructureChanged();
		
		return firstSubfold.get();
	} //}}}

	//{{{ expandAllFolds() method
	/**
	 * Expands all folds.
	 * @since jEdit 4.2pre1
	 */
	public void expandAllFolds()
	{
		showLineRange(0,buffer.getLineCount() - 1);
		notifyScreenLineChanges();
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
		}
		else
			expandFolds((digit - '1') + 1);
	} //}}}

	//{{{ expandFolds() method
	/**
	 * Expands all folds with the specified fold level.
	 * @param foldLevel The fold level
	 * @since jEdit 4.2pre1
	 */
	public void expandFolds(int foldLevel)
	{
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

		notifyScreenLineChanges();
		if(textArea.getDisplayManager() == this)
		{
			textArea.foldStructureChanged();
		}
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

		if(start != 0)
			hideLineRange(0,start - 1);
		if(end != buffer.getLineCount() - 1)
			hideLineRange(end + 1,buffer.getLineCount() - 1);

		// if we narrowed to a single collapsed fold
		if(start != buffer.getLineCount() - 1
			&& !isLineVisible(start + 1))
			expandFold(start,false);

		textArea.fireNarrowActive();

		notifyScreenLineChanges();
		textArea.foldStructureChanged();
	} //}}}

	//{{{ Package-private members
	final FirstLine firstLine;
	final ScrollLineCount scrollLineCount;
	final ScreenLineManager screenLineMgr;
	RangeMap folds;

	//{{{ init() method
	void init()
	{
		if(initialized)
		{
			if(!buffer.isLoading())
				resetAnchors();
		}
		else
		{
			initialized = true;
			folds = new RangeMap();
			if(buffer.isLoading())
				folds.reset(buffer.getLineCount());
			else
				bufferHandler.foldHandlerChanged(buffer);
			notifyScreenLineChanges();
		}
	} //}}}

	//{{{ notifyScreenLineChanges() method
	void notifyScreenLineChanges()
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"notifyScreenLineChanges()");

		// when the text area switches to us, it will do
		// a reset anyway
		if(textArea.getDisplayManager() != this)
			return;

		try
		{
			if(firstLine.callReset)
				firstLine.reset();
			else if(firstLine.callChanged)
				firstLine.changed();

			if(scrollLineCount.callReset)
			{
				scrollLineCount.reset();
				firstLine.ensurePhysicalLineIsVisible();
			}
			else if(scrollLineCount.callChanged)
				scrollLineCount.changed();
			
			if(firstLine.callChanged || scrollLineCount.callReset
				|| scrollLineCount.callChanged)
			{
				textArea.updateScrollBar();
				textArea.recalculateLastPhysicalLine();
			}
		}
		finally
		{
			firstLine.callReset = firstLine.callChanged = false;
			scrollLineCount.callReset = scrollLineCount.callChanged = false;
		}
	} //}}}

	//{{{ setFirstLine() method
	void setFirstLine(int oldFirstLine, int firstLine)
	{
		int visibleLines = textArea.getVisibleLines();

		if(firstLine >= oldFirstLine + visibleLines)
		{
			this.firstLine.scrollDown(firstLine - oldFirstLine);
			textArea.chunkCache.invalidateAll();
		}
		else if(firstLine <= oldFirstLine - visibleLines)
		{
			this.firstLine.scrollUp(oldFirstLine - firstLine);
			textArea.chunkCache.invalidateAll();
		}
		else if(firstLine > oldFirstLine)
		{
			this.firstLine.scrollDown(firstLine - oldFirstLine);
			textArea.chunkCache.scrollDown(firstLine - oldFirstLine);
		}
		else if(firstLine < oldFirstLine)
		{
			this.firstLine.scrollUp(oldFirstLine - firstLine);
			textArea.chunkCache.scrollUp(oldFirstLine - firstLine);
		}

		notifyScreenLineChanges();
	} //}}}

	//{{{ setFirstPhysicalLine() method
	/**
	 * Scroll from a given amount of lines.
	 *
	 * @param amount the amount of lines that must be scrolled
	 * @param skew a skew within the given line
	 */
	void setFirstPhysicalLine(int amount, int skew)
	{
		int oldFirstLine = textArea.getFirstLine();

		if(amount == 0)
		{
			skew -= this.firstLine.skew;

			// JEditTextArea.scrollTo() needs this to simplify
			// its code
			if(skew < 0)
				this.firstLine.scrollUp(-skew);
			else if(skew > 0)
				this.firstLine.scrollDown(skew);
			else
			{
				// nothing to do
				return;
			}
		}
		else if(amount > 0)
			this.firstLine.physDown(amount,skew);
		else if(amount < 0)
			this.firstLine.physUp(-amount,skew);

		int firstLine = textArea.getFirstLine();
		int visibleLines = textArea.getVisibleLines();

		if(firstLine == oldFirstLine)
			/* do nothing */;
		else if(firstLine >= oldFirstLine + visibleLines
			|| firstLine <= oldFirstLine - visibleLines)
		{
			textArea.chunkCache.invalidateAll();
		}
		else if(firstLine > oldFirstLine)
		{
			textArea.chunkCache.scrollDown(firstLine - oldFirstLine);
		}
		else if(firstLine < oldFirstLine)
		{
			textArea.chunkCache.scrollUp(oldFirstLine - firstLine);
		}

		// we have to be careful
		notifyScreenLineChanges();
	} //}}}

	//{{{ invalidateScreenLineCounts() method
	void invalidateScreenLineCounts()
	{
		screenLineMgr.invalidateScreenLineCounts();
		firstLine.callReset = true;
		scrollLineCount.callReset = true;
	} //}}}

	//{{{ updateScreenLineCount() method
	void updateScreenLineCount(int line)
	{
		if(!screenLineMgr.isScreenLineCountValid(line))
		{
			int newCount = textArea.chunkCache
				.getLineSubregionCount(line);

			setScreenLineCount(line,newCount);
		}
	} //}}}

	//{{{ bufferLoaded() method
	void bufferLoaded()
	{
		folds.reset(buffer.getLineCount());
		screenLineMgr.reset();

		if(textArea.getDisplayManager() == this)
		{
			textArea.propertiesChanged();
			init();
		}

		int collapseFolds = buffer.getIntegerProperty(
			"collapseFolds",0);
		if(collapseFolds != 0)
			expandFolds(collapseFolds);
	} //}}}

	//{{{ foldHandlerChanged() method
	void foldHandlerChanged()
	{
		if(buffer.isLoading())
			return;

		folds.reset(buffer.getLineCount());
		resetAnchors();

		int collapseFolds = buffer.getIntegerProperty(
			"collapseFolds",0);
		if(collapseFolds != 0)
			expandFolds(collapseFolds);
	} //}}}

	//}}}

	//{{{ Private members
	private boolean initialized;
	private boolean inUse;
	private final JEditBuffer buffer;
	private final TextArea textArea;
	private final BufferHandler bufferHandler;

	//{{{ DisplayManager constructor
	private DisplayManager(JEditBuffer buffer, TextArea textArea,
		DisplayManager copy)
	{
		this.buffer = buffer;
		this.screenLineMgr = new ScreenLineManager(buffer);
		this.textArea = textArea;

		scrollLineCount = new ScrollLineCount(this,textArea);
		firstLine = new FirstLine(this,textArea);
		bufferHandler = new BufferHandler(this,textArea,buffer);
		//TODO:invoke ElasticTabStopBufferListener methods from inside BufferHandler to avoid chunking same line twice
		ElasticTabStopBufferListener listener = new ElasticTabStopBufferListener(textArea);
		buffer.addBufferListener(listener, JEditBuffer.HIGH_PRIORITY);
		// this listener priority thing is a bad hack...
		buffer.addBufferListener(bufferHandler, JEditBuffer.HIGH_PRIORITY);

		if(copy != null)
		{
			folds = new RangeMap(copy.folds);
			initialized = true;
		}
	} //}}}

	//{{{ resetAnchors() method
	private void resetAnchors()
	{
		firstLine.callReset = true;
		scrollLineCount.callReset = true;
		notifyScreenLineChanges();
	} //}}}

	//{{{ dispose() method
	private void dispose()
	{
		buffer.removeBufferListener(bufferHandler);
	} //}}}

	//{{{ showLineRange() method
	private void showLineRange(int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"showLineRange(" + start
				+ ',' + end + ')');
		}

		for(int i = start; i <= end; i++)
		{
			//XXX
			if(!isLineVisible(i))
			{
				// important: not screenLineMgr.getScreenLineCount()
				int screenLines = getScreenLineCount(i);
				if(firstLine.physicalLine >= i)
				{
					firstLine.scrollLine += screenLines;
					firstLine.callChanged = true;
				}
				scrollLineCount.scrollLine += screenLines;
				scrollLineCount.callChanged = true;
			}
		}

		/* update fold visibility map. */
		folds.show(start,end);
	} //}}}

	//{{{ hideLineRange() method
	private void hideLineRange(int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"hideLineRange(" + start
				+ ',' + end + ')');
		}

		int i = start;
		if(!isLineVisible(i))
			i = getNextVisibleLine(i);
		while(i != -1 && i <= end)
		{
			int screenLines = getScreenLineCount(i);
			if(i < firstLine.physicalLine)
			{
				firstLine.scrollLine -= screenLines;
				firstLine.skew = 0;
				firstLine.callChanged = true;
			}

			scrollLineCount.scrollLine -= screenLines;
			scrollLineCount.callChanged = true;

			i = getNextVisibleLine(i);
		}

		/* update fold visibility map. */
		folds.hide(start,end);

		if(!isLineVisible(firstLine.physicalLine))
		{
			int firstVisible = getFirstVisibleLine();
			if(firstLine.physicalLine < firstVisible)
			{
				firstLine.physicalLine = firstVisible;
				firstLine.scrollLine = 0;
			}
			else
			{
				firstLine.physicalLine = getPrevVisibleLine(
					firstLine.physicalLine);
				firstLine.scrollLine -= getScreenLineCount(
					firstLine.physicalLine);
			}
			firstLine.callChanged = true;
		}
	} //}}}

	//{{{ setScreenLineCount() method
	/**
	 * Sets the number of screen lines that the specified physical line
	 * is split into.
	 * @param line the line number
	 * @param count the line count (1 if no wrap)
	 * @since jEdit 4.2pre1
	 */
	private void setScreenLineCount(int line, int count)
	{
		int oldCount = screenLineMgr.getScreenLineCount(line);

		// old one so that the screen line manager sets the
		// validity flag!

		screenLineMgr.setScreenLineCount(line,count);

		if(count == oldCount)
			return;

		if(!isLineVisible(line))
			return;

		if(firstLine.physicalLine >= line)
		{
			if(firstLine.physicalLine == line)
				firstLine.callChanged = true;
			else
			{
				firstLine.scrollLine += count - oldCount;
				firstLine.callChanged = true;
			}
		}

		scrollLineCount.scrollLine += count - oldCount;
		scrollLineCount.callChanged = true;
	} //}}}

	//{{{ _expandFold() method
	/**
	 * Expands the fold at the specified physical line index.
	 * @param line A physical line index
	 * @param fully If true, all subfolds will also be expanded
	 * @param firstSubfold Will be set to the line number of the first
	 * subfold, or -1 if there is none.
	 * @return True if some line was unfolded, false otherwise.
	 */
	public boolean _expandFold(int line, boolean fully, MutableInteger firstSubfold)
	{
		boolean unfolded = false;

		int lineCount = buffer.getLineCount();
		int end = lineCount - 1;

		if (line == lineCount - 1)
		{
			return false;
		}
		while (!isLineVisible(line))
		{
			int prevLine = folds.lookup(folds.search(line)) - 1;
			if (!isLineVisible(prevLine))
			{
				return unfolded;
			}
			
			// If any fold farther down was unfolded, then the text
			// area needs to be updated
			unfolded |= _expandFold(prevLine, fully, firstSubfold);
			
			if (!isLineVisible(prevLine + 1))
			{
				return unfolded;
			}
		}
		if (isLineVisible(line+1) && !fully)
		{
			return unfolded;
		}

		//{{{ Find fold start and fold end...
		int start;
		int initialFoldLevel = buffer.getFoldLevel(line);
		if (buffer.getFoldLevel(line + 1) > initialFoldLevel)
		{
			// this line is the start of a fold
			start = line;
			if (!isLineVisible(line + 1) && folds.search(line + 1) != folds.count() - 1)
			{
				int index = folds.search(line + 1);
				end = folds.lookup(index + 1) - 1;
			}
			else
			{
				for (int i = line + 1; i < lineCount; i++)
				{
					if (buffer.getFoldLevel(i) <= initialFoldLevel)
					{
						end = i - 1;
						break;
					}
				}
			}
		}
		else
		{
			if (!fully)
			{
				return unfolded;
			}
			start = line;
			while (start > 0 && buffer.getFoldLevel(start) >= initialFoldLevel)
			{
				start--;
			}
			initialFoldLevel = buffer.getFoldLevel(start);
			for (int i = line + 1; i < lineCount; i++)
			{
				if (buffer.getFoldLevel(i) <= initialFoldLevel)
				{
					end = i - 1;
					break;
				}
			}
		} // }}}

		//{{{ Expand the fold...
		if(fully)
		{
			showLineRange(start,end);
		}
		else
		{
			boolean foundSubfold = false;
			for (int i = start + 1; i <= end;)
			{
				if (!foundSubfold && buffer.isFoldStart(i))
				{
					firstSubfold.set(i);
					foundSubfold = true;
				}

				showLineRange(i, i);
				int fold = buffer.getFoldLevel(i);
				i++;
				while (i <= end && buffer.getFoldLevel(i) > fold)
				{
					i++;
				}
			}
		}
		
		unfolded = true;
		// }}}
		
		notifyScreenLineChanges();
		return unfolded;
	} //}}}
	
	//{{{ MutableInteger class
	private class MutableInteger
	{
		MutableInteger(int value)
		{
			this.value = value;
		}
		
		public void set(int value)
		{
			this.value = value;
		}
		
		public int get()
		{
			return value;
		}
		
		private int value;
	} //}}}
	
	//}}}
}
