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
import java.awt.Toolkit;
import java.util.*;
import org.gjt.sp.jedit.buffer.*;
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

	public static long scanCount, scannedLines;

	//{{{ getDisplayManager() method
	static DisplayManager getDisplayManager(Buffer buffer,
		JEditTextArea textArea)
	{
		List l = (List)bufferMap.get(buffer);
		DisplayManager dmgr;
		if(l == null)
		{
			l = new LinkedList();
			bufferMap.put(buffer,l);
		}

		Iterator liter = l.iterator();
		while(liter.hasNext())
		{
			dmgr = (DisplayManager)liter.next();
			if(!dmgr.inUse && dmgr.textArea == textArea)
			{
				dmgr.inUse = true;
				return dmgr;
			}
		}

		// if we got here, no unused display manager in list
		dmgr = new DisplayManager(buffer,textArea);
		dmgr.inUse = true;
		l.add(dmgr);

		return dmgr;
	} //}}}

	//{{{ releaseDisplayManager() method
	static void releaseDisplayManager(DisplayManager dmgr)
	{
		dmgr.inUse = false;
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
	/* static void _notifyScreenLineChanges(Buffer buffer)
	{
		Iterator iter = ((List)bufferMap.get(buffer)).iterator();
		while(iter.hasNext())
		{
			((DisplayManager)iter.next())._notifyScreenLineChanges();
		}
	} */ //}}}

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
		return fvmget(line) % 2 == 0;
	} //}}}

	//{{{ getFirstVisibleLine() method
	/**
	 * Returns the physical line number of the first visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getFirstVisibleLine()
	{
		return fvm[0];
	} //}}}

	//{{{ getLastVisibleLine() method
	/**
	 * Returns the physical line number of the last visible line.
	 * @since jEdit 4.2pre1
	 */
	public int getLastVisibleLine()
	{
		return fvm[fvmcount - 1] - 1;
	} //}}}

	//{{{ getNextVisibleLine() method
	/**
	 * Returns the next visible line after the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getNextVisibleLine(int line)
	{
		int index = fvmget(line);
		/* in collapsed range */
		if(index % 2 != 0)
		{
			/* beyond last visible line */
			if(fvmcount == index + 1)
				return - 1;
			/* start of next expanded range */
			else
				return fvm[index + 1];
		}
		/* last in expanded range */
		else if(line == fvm[index + 1] - 1)
		{
			/* equal to last visible line */
			if(fvmcount == index + 2)
				return -1;
			/* start of next expanded range */
			else
				return fvm[index + 2];
		}
		/* next in expanded range */
		else
			return line + 1;
	} //}}}

	//{{{ getPrevVisibleLine() method
	/**
	 * Returns the previous visible line before the specified line index.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int getPrevVisibleLine(int line)
	{
		int index = fvmget(line);
		/* before first visible line */
		if(index == -1)
			return -1;
		/* in collapsed range */
		else if(index % 2 == 1)
		{
			/* end of prev expanded range */
			return fvm[index] - 1;
		}
		/* first in expanded range */
		else if(line == fvm[index])
		{
			/* equal to first visible line */
			if(index == 0)
				return -1;
			/* end of prev expanded range */
			else
				return fvm[index - 1] - 1;
		}
		/* prev in expanded range */
		else
			return line - 1;
	} //}}}

	//{{{ getScreenLineCount() method
	public final int getScreenLineCount(int line)
	{
		if(lineMgr.isScreenLineCountValid(line))
			return lineMgr.getScreenLineCount(line);
		else
		{
			int newCount = textArea.chunkCache
				.getLineSubregionCount(line);

			setScreenLineCount(line,newCount);
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
				//XXX
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
				//XXX
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
		showLineRange(0,buffer.getLineCount() - 1);
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
		if(start != buffer.getLineCount() - 1
			&& !isLineVisible(start + 1))
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
			fvm = new int[2];
			if(buffer.isLoaded())
				bufferChangeHandler.foldHandlerChanged(buffer);
			else
				fvmreset();
			_notifyScreenLineChanges();
		}
		else
		{
			_notifyScreenLineChanges();
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
		int oldCount = lineMgr.getScreenLineCount(line);
		// still have to call this even if it equals the
		// old one so that the offset manager sets the
		// validity flag!
		lineMgr.setScreenLineCount(line,count);
		// this notifies each display manager editing this
		// buffer of the screen line count change
		if(count != oldCount)
			_setScreenLineCount(buffer,line,oldCount,count);
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

	//{{{ _notifyScreenLineChanges() method
	void _notifyScreenLineChanges()
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"_notifyScreenLineChanges()");

		// when the text area switches to us, it will do
		// a reset anyway
		if(textArea.getDisplayManager() == this)
		{
			if(firstLine.callReset)
				firstLine.reset();
			else if(firstLine.callChanged)
				firstLine.changed();

			if(scrollLineCount.callReset)
				scrollLineCount.reset();
			else if(scrollLineCount.callChanged)
				scrollLineCount.changed();

			firstLine.callReset = firstLine.callChanged = false;
			scrollLineCount.callReset = scrollLineCount.callChanged = false;
		}
	} //}}}

	//}}}

	//{{{ Private members
	private boolean initialized;
	private boolean inUse;
	private Buffer buffer;
	private LineManager lineMgr;
	private JEditTextArea textArea;
	private BufferChangeHandler bufferChangeHandler;

	/**
	 * The fold visibility map.
	 *
	 * All lines from fvm[2*n] to fvm[2*n+1]-1 inclusive are visible.
	 * All lines from position fvm[2*n+1] to fvm[2*n+2]-1 inclusive are
	 * invisible.
	 *
	 * Examples:
	 * ---------
	 * All lines visible: { 0, buffer.getLineCount() }
	 * Narrow from a to b: { a, b + 1 }
	 * Collapsed fold from a to b: { 0, a + 1, b, buffer.getLineCount() }
	 *
	 * Note: length is always even.
	 */
	private int[] fvm;
	private int fvmcount;

	private int lastfvmget = -1;

	//{{{ DisplayManager constructor
	private DisplayManager(Buffer buffer, JEditTextArea textArea)
	{
		this.buffer = buffer;
		this.lineMgr = buffer._getLineManager();
		this.textArea = textArea;

		scrollLineCount = new ScrollLineCount();
		firstLine = new FirstLine();

		bufferChangeHandler = new BufferChangeHandler();
		// this listener priority thing is a bad hack...
		buffer.addBufferChangeListener(bufferChangeHandler,
			Buffer.HIGH_PRIORITY);
	} //}}}

	//{{{ dispose() method
	private void dispose()
	{
		buffer.removeBufferChangeListener(bufferChangeHandler);
	} //}}}

	//{{{ fvmreset() method
	private void fvmreset()
	{
		lastfvmget = -1;
		fvmcount = 2;
		fvm[0] = 0;
		fvm[1] = buffer.getLineCount();
	} //}}}

	//{{{ fvmget() method
	/**
	 * Returns the fold visibility map index for the given line.
	 */
	private int fvmget(int line)
	{
		scanCount++;

		if(line < fvm[0])
			return -1;
		if(line >= fvm[fvmcount - 1])
			return fvmcount - 1;

		if(lastfvmget != -1)
		{
			if(line >= fvm[lastfvmget])
			{
				if(lastfvmget == fvmcount - 1
					|| line < fvm[lastfvmget + 1])
				{
					return lastfvmget;
				}
			}
		}

		int start = 0;
		int end = fvmcount - 1;

loop:		for(;;)
		{
			scannedLines++;
			switch(end - start)
			{
			case 0:
				lastfvmget = start;
				break loop;
			case 1:
				int value = fvm[end];
				if(value <= line)
					lastfvmget = end;
				else
					lastfvmget = start;
				break loop;
			default:
				int pivot = (end + start) / 2;
				value = fvm[pivot];
				if(value == line)
				{
					lastfvmget = pivot;
					break loop;
				}
				else if(value < line)
					start = pivot;
				else
					end = pivot - 1;
				break;
			}
		}

		return lastfvmget;
	} //}}}

	//{{{ fvmput() method
	/**
	 * Replaces from <code>start</code> to <code>end-1</code> inclusive with
	 * <code>put</code>. Update <code>fvmcount</code>.
	 */
	private void fvmput(int start, int end, int[] put)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			StringBuffer buf = new StringBuffer("{");
			if(put != null)
			{
				for(int i = 0; i < put.length; i++)
				{
					if(i != 0)
						buf.append(',');
					buf.append(put[i]);
				}
			}
			buf.append("}");
			Log.log(Log.DEBUG,this,"fvmput(" + start + ","
				+ end + "," + buf + ")");
		}
		int putl = (put == null ? 0 : put.length);

		int delta = putl - (end - start);
		if(fvmcount + delta > fvm.length)
		{
			int[] newfvm = new int[fvm.length * 2 + 1];
			System.arraycopy(fvm,0,newfvm,0,fvmcount);
			fvm = newfvm;
		}

		if(delta != 0)
		{
			System.arraycopy(fvm,end,fvm,start + putl,
				fvmcount - end);
		}

		if(putl != 0)
		{
			System.arraycopy(put,0,fvm,start,put.length);
		}

		fvmcount += delta;

		fvmdump();

		if(fvmcount == 0)
			throw new InternalError();
	} //}}}

	//{{{ fvmput2() method
	/**
	 * Merge previous and next entry if necessary.
	 */
	private void fvmput2(int starti, int endi, int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"*fvmput2(" + starti + ","
				+ endi + "," + start + "," + end + ")");
		}
		if(starti != -1 && fvm[starti] == start)
		{
			if(endi <= fvmcount - 2 && fvm[endi + 1]
				== end + 1)
			{
				fvmput(starti,endi + 2,null);
			}
			else
			{
				fvmput(starti,endi + 1,
					new int[] { end + 1 });
			}
		}
		else
		{
			if(endi != fvmcount - 1 && fvm[endi + 1]
				== end + 1)
			{
				fvmput(starti + 1,endi + 2,
					new int[] { start });
			}
			else
			{
				fvmput(starti + 1,endi + 1,
					new int[] { start,
					end + 1 });
			}
		}
	} //}}}

	//{{{ fvmdump() method
	private void fvmdump()
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			StringBuffer buf = new StringBuffer("{");
			for(int i = 0; i < fvmcount; i++)
			{
				if(i != 0)
					buf.append(',');
				buf.append(fvm[i]);
			}
			buf.append("}");
			Log.log(Log.DEBUG,this,"fvm = " + buf);
		}
	} //}}}

	//{{{ showLineRange() method
	private void showLineRange(int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"showLineRange(" + start
				+ "," + end + ")");
		}

		for(int i = start; i <= end; i++)
		{
			//XXX
			if(!isLineVisible(i))
			{
				// important: not lineMgr.getScreenLineCount()
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
		int starti = fvmget(start);
		int endi = fvmget(end);

		if(starti % 2 == 0)
		{
			if(endi % 2 == 0)
				fvmput(starti + 1,endi + 1,null);
			else
			{
				if(endi != fvmcount - 1
					&& fvm[endi + 1] == end + 1)
					fvmput(starti + 1,endi + 2,null);
				else
				{
					fvmput(starti + 1,endi,null);
					fvm[starti + 1] = end + 1;
				}
			}
		}
		else
		{
			if(endi % 2 == 0)
			{
				if(starti != -1 && fvm[starti] == start)
					fvmput(starti,endi + 1,null);
				else
				{
					fvmput(starti + 1,endi,null);
					fvm[starti + 1] = start;
				}
			}
			else
				fvmput2(starti,endi,start,end);
		}

		lastfvmget = -1;
	} //}}}

	//{{{ hideLineRange() method
	private void hideLineRange(int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"hideLineRange(" + start
				+ "," + end + ")");
		}

		for(int i = start; i <= end; i++)
		{
			//XXX
			if(isLineVisible(i))
			{
				int screenLines = lineMgr
					.getScreenLineCount(i);
				if(firstLine.physicalLine >= i)
				{
					firstLine.scrollLine -= screenLines;
					firstLine.callChanged = true;
				}
				scrollLineCount.scrollLine -= screenLines;
				scrollLineCount.callChanged = true;
			}
		}

		/* update fold visibility map. */
		int starti = fvmget(start);
		int endi = fvmget(end);

		if(starti % 2 == 0)
		{
			if(endi % 2 == 0)
				fvmput2(starti,endi,start,end);
			else
			{
				fvmput(starti + 1,endi,null);
				fvm[starti + 1] = start;
			}
		}
		else
		{
			if(endi % 2 == 0)
			{
				fvmput(starti + 1,endi,null);
				fvm[starti + 1] = end + 1;
			}
			else
				fvmput(starti + 1,endi + 1,null);
		}

		lastfvmget = -1;
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

	//}}}

	//{{{ Anchor class
	static abstract class Anchor
	{
		int physicalLine;
		int scrollLine;
		boolean callChanged;
		boolean callReset;

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

			physicalLine = getFirstVisibleLine();
			scrollLine = 0;
			while(physicalLine != -1)
			{
				scrollLine += getScreenLineCount(physicalLine);
				physicalLine = getNextVisibleLine(physicalLine);
			}

			physicalLine = buffer.getLineCount();

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

			int i = getFirstVisibleLine();

			for(;;)
			{
				if(i >= physicalLine)
					break;

				scrollLine += getScreenLineCount(i);

				int nextLine = getNextVisibleLine(i);
				if(nextLine == -1)
					break;
				else
					i = nextLine;
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
					scrollLine += getScreenLineCount(physicalLine);
					physicalLine = nextPhysicalLine;
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
			if(screenAmount < 0)
				scrollUp(-screenAmount);
			else if(screenAmount > 0)
				scrollDown(screenAmount);
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
					amount -= (physicalLine - prevPhysicalLine);
					physicalLine = prevPhysicalLine;
					scrollLine -= getScreenLineCount(
						prevPhysicalLine);
				}
			}

			if(Debug.SCROLL_DEBUG)
			{
				Log.log(Log.DEBUG,this,"physUp() end: "
					+ physicalLine + ":" + scrollLine);
			}

			// JEditTextArea.scrollTo() needs this to simplify
			// its code
			if(screenAmount < 0)
				scrollUp(-screenAmount);
			else if(screenAmount > 0)
				scrollDown(screenAmount);
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
					//scrollLine -= getScreenLineCount(physicalLine);
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
		boolean delayedUpdate;
		int delayedUpdateStart;
		int delayedUpdateEnd;

		//{{{ foldHandlerChanged() method
		public void foldHandlerChanged(Buffer buffer)
		{
			fvmreset();

			firstLine.callReset = true;
			scrollLineCount.callReset = true;

			int collapseFolds = buffer.getIntegerProperty(
				"collapseFolds",0);
			if(collapseFolds != 0)
				expandFolds(collapseFolds);

			_notifyScreenLineChanges();
		} //}}}

		//{{{ wrapModeChanged() method
		public void wrapModeChanged(Buffer buffer)
		{
			firstLine.reset();
			scrollLineCount.reset();
		} //}}}

		//{{{ contentInserted() method
		public void contentInserted(Buffer buffer, int startLine,
			int offset, int numLines, int length)
		{
			if(!buffer.isLoaded())
				return;

			delayedUpdate(startLine,startLine + numLines);

			if(numLines != 0)
			{
				contentInserted(firstLine,startLine,numLines);
				contentInserted(scrollLineCount,startLine,numLines);

				/* this is a sloppy hack to fix bug
				   "[ 677902 ] hitting return after collapsed
				   fold"

				   the idea is that if we extend the range then
				   the problem described in the bug happends, so
				   if the insert is at the very end of the range
				   we don't extend it, instead we push the
				   insert into the next range, however for this
				   to work properly we also have to mess with
				   screen line counts. */
				int index = fvmget(startLine);
				int start = index + 1;
				if(start + 1 < fvmcount && fvm[start]
					== startLine + 1)
				{
					if(index % 2 == 0)
					{
						scrollLineCount.scrollLine -=
							lineMgr.getScreenLineCount(
							startLine + 1);
					}
					else
					{
						scrollLineCount.scrollLine +=
							lineMgr.getScreenLineCount(
							startLine + 1);
					}
					start++;
				}

				for(int i = start; i < fvmcount; i++)
				{
					fvm[i] += numLines;
				}

				lastfvmget = -1;
			}

			if(!buffer.isTransactionInProgress())
				transactionComplete(buffer);
		} //}}}

		//{{{ preContentRemoved() method
		public void preContentRemoved(Buffer buffer, int startLine,
			int offset, int numLines, int length)
		{
			if(!buffer.isLoaded())
				return;

			if(numLines != 0)
			{
				preContentRemoved(firstLine,startLine,numLines);
				preContentRemoved(scrollLineCount,startLine,numLines);

				/* update fold visibility map. */
				int starti = fvmget(startLine);
				int endi = fvmget(startLine + numLines);
				System.err.println(starti + "," + endi);

				/* both have same visibility; just remove
				 * anything in between. */
				if(Math.abs(starti % 2) == Math.abs(endi % 2))
				{
					if(endi - starti == fvmcount)
					{
						// we're removing from before
						// the first visible to after
						// the last visible
						fvmreset();
						firstLine.callReset = true;
						scrollLineCount.callReset = true;
					}
					else
					{
						fvmput(starti + 1,endi + 1,null);
						starti++;
					}
				}
				else if(Math.abs(starti % 2) == 1)
				{
					fvmput(starti + 1,endi + 2,null);
					fvm[starti + 1] = startLine;
					starti += 2;
				}
				else /*if(Math.abs(endi % 2) == 1)*/
				{
					fvmput(starti + 1,endi,null);
					fvm[starti + 1] = startLine;
					starti += 2;
				}

				for(int i = starti; i < fvmcount; i++)
					fvm[i] -= numLines;

				lastfvmget = -1;
				fvmdump();
			}
		} //}}}

		//{{{ contentRemoved() method
		public void contentRemoved(Buffer buffer, int startLine,
			int offset, int numLines, int length)
		{
			if(!buffer.isLoaded())
				return;

			delayedUpdate(startLine,startLine);

			if(!buffer.isTransactionInProgress())
				transactionComplete(buffer);
		} //}}}

		//{{{ transactionComplete() method
		public void transactionComplete(Buffer buffer)
		{
			if(delayedUpdate)
			{
				if(textArea.getDisplayManager() == DisplayManager.this)
				{
					int firstLine = textArea.getFirstPhysicalLine();
					int lastLine = textArea.getLastPhysicalLine();

					int line = delayedUpdateStart;
					if(!isLineVisible(line))
						line = getNextVisibleLine(line);
					while(line != -1 && line <= delayedUpdateEnd)
					{
						if(line < firstLine
							|| line > lastLine)
						{
							getScreenLineCount(line);
						}
						line = getNextVisibleLine(line);
					}
				}
				_notifyScreenLineChanges();

				delayedUpdate = false;
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
		private void preContentRemoved(Anchor anchor, int startLine,
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
						//XXX
						if(isLineVisible(i))
						{
							anchor.scrollLine -=
								lineMgr
								.getScreenLineCount(i);
						}
					}
					anchor.physicalLine -= (end - startLine);
					anchor.callChanged = true;
				}
			}
		} //}}}

		//{{{ delayedUpdate() method
		private void delayedUpdate(int startLine, int endLine)
		{
			textArea.chunkCache.invalidateChunksFromPhys(startLine);
			if(!delayedUpdate)
			{
				delayedUpdateStart = startLine;
				delayedUpdateEnd = endLine;
				delayedUpdate = true;
			}
			else
			{
				delayedUpdateStart = Math.min(
					delayedUpdateStart,
					startLine);
				delayedUpdateEnd = Math.max(
					delayedUpdateEnd,
					endLine);
			}
		} //}}}
	} //}}}
}
