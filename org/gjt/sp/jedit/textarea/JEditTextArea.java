/*
 * JEditTextArea.java - jEdit's text component
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
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
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's text component.<p>
 *
 * Unlike most other text editors, the selection API permits selection and
 * concurrent manipulation of multiple, non-contiguous regions of text.
 * Methods in this class that deal with selecting text rely upon classes derived
 * the {@link Selection} class.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class JEditTextArea extends JComponent
{
	//{{{ JEditTextArea constructor
	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea(View view)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		this.view = view;

		//{{{ Initialize some misc. stuff
		selection = new Vector();
		chunkCache = new ChunkCache(this);
		painter = new TextAreaPainter(this);
		gutter = new Gutter(view,this);
		bufferHandler = new BufferChangeHandler();
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		bracketLine = bracketPosition = -1;
		blink = true;
		lineSegment = new Segment();
		returnValue = new Point();
		runnables = new ArrayList();
		//}}}

		//{{{ Initialize the GUI
		setLayout(new ScrollLayout());
		add(LEFT,gutter);
		add(CENTER,painter);
		add(RIGHT,vertical = new JScrollBar(JScrollBar.VERTICAL));
		add(BOTTOM,horizontal = new JScrollBar(JScrollBar.HORIZONTAL));

		horizontal.setValues(0,0,0,0);
		//}}}

		//{{{ this ensures that the text area's look is slightly
		// more consistent with the rest of the metal l&f.
		// while it depends on not-so-well-documented portions
		// of Swing, it only affects appearance, so future
		// breakage shouldn't matter
		if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
		{
			setBorder(new TextAreaBorder());
			vertical.putClientProperty("JScrollBar.isFreeStanding",
				Boolean.FALSE);
			horizontal.putClientProperty("JScrollBar.isFreeStanding",
				Boolean.FALSE);
			//horizontal.setBorder(null);
		}
		//}}}

		//{{{ Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());

		mouseHandler = new MouseHandler();
		painter.addMouseListener(mouseHandler);
		painter.addMouseMotionListener(mouseHandler);

		addFocusListener(new FocusHandler());
		//}}}

		// This doesn't seem very correct, but it fixes a problem
		// when setting the initial caret position for a buffer
		// (eg, from the recent file list)
		focusedComponent = this;
	} //}}}

	//{{{ Getters and setters

	//{{{ getPainter() method
	/**
	 * Returns the object responsible for painting this text area.
	 */
	public final TextAreaPainter getPainter()
	{
		return painter;
	} //}}}

	//{{{ getGutter() method
 	/**
	 * Returns the gutter to the left of the text area or null if the gutter
	 * is disabled
	 */
	public final Gutter getGutter()
	{
		return gutter;
	} //}}}

	//{{{ getFoldVisibilityManager() method
	/**
	 * Returns the fold visibility manager used by this text area.
	 * @since jEdit 4.0pre1
	 */
	public FoldVisibilityManager getFoldVisibilityManager()
	{
		return foldVisibilityManager;
	} //}}}

	//{{{ isCaretBlinkEnabled() method
	/**
	 * Returns true if the caret is blinking, false otherwise.
	 */
	public final boolean isCaretBlinkEnabled()
	{
		return caretBlinks;
	} //}}}

	//{{{ setCaretBlinkEnabled() method
	/**
	 * Toggles caret blinking.
	 * @param caretBlinks True if the caret should blink, false otherwise
	 */
	public void setCaretBlinkEnabled(boolean caretBlinks)
	{
		this.caretBlinks = caretBlinks;
		if(!caretBlinks)
			blink = false;

		if(buffer != null)
			invalidateLine(caretLine);
	} //}}}

	//{{{ getElectricScroll() method
	/**
	 * Returns the number of lines from the top and button of the
	 * text area that are always visible.
	 */
	public final int getElectricScroll()
	{
		return electricScroll;
	} //}}}

	//{{{ setElectricScroll() method
	/**
	 * Sets the number of lines from the top and bottom of the text
	 * area that are always visible
	 * @param electricScroll The number of lines always visible from
	 * the top or bottom
	 */
	public final void setElectricScroll(int electricScroll)
	{
		this.electricScroll = electricScroll;
	} //}}}

	//{{{ isQuickCopyEnabled() method
	/**
	 * Returns if clicking the middle mouse button pastes the most
	 * recent selection (% register), and if Control-dragging inserts
	 * the selection at the caret.
	 */
	public final boolean isQuickCopyEnabled()
	{
		return quickCopy;
	} //}}}

	//{{{ setQuickCopyEnabled() method
	/**
	 * Sets if clicking the middle mouse button pastes the most
	 * recent selection (% register), and if Control-dragging inserts
	 * the selection at the caret.
	 * @param quickCopy A boolean flag
	 */
	public final void setQuickCopyEnabled(boolean quickCopy)
	{
		this.quickCopy = quickCopy;
	} //}}}

	//{{{ getBuffer() method
	/**
	 * Returns the buffer this text area is editing.
	 */
	public final Buffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ setBuffer() method
	/**
	 * Sets the buffer this text area is editing. Do not call this method -
	 * use {@link org.gjt.sp.jedit.EditPane#setBuffer(Buffer)} instead.
	 * @param buffer The buffer
	 */
	public void setBuffer(Buffer buffer)
	{
		if(this.buffer == buffer)
			return;

		try
		{
			bufferChanging = true;

			if(this.buffer != null)
			{
				// dubious?
				//setFirstLine(0);

				selectNone();
				caretLine = caret = caretScreenLine = 0;
				bracketLine = bracketPosition = -1;

				this.buffer._releaseFoldVisibilityManager(foldVisibilityManager);
				this.buffer.removeBufferChangeListener(bufferHandler);
			}
			this.buffer = buffer;

			foldVisibilityManager = buffer._getFoldVisibilityManager(this);

			buffer.addBufferChangeListener(bufferHandler);
			bufferHandlerInstalled = true;

			firstLine = 0;
			maxHorizontalScrollWidth = 0;
			physFirstLine = foldVisibilityManager.getFirstVisibleLine();
			chunkCache.setFirstLine(0,physFirstLine,true);

			propertiesChanged();

			recalculateLastPhysicalLine();

			painter.repaint();
			gutter.repaint();

			updateScrollBars();
			fireScrollEvent(true);
		}
		finally
		{
			bufferChanging = false;
		}
	} //}}}

	//{{{ isEditable() method
	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable()
	{
		return buffer.isEditable();
	} //}}}

	//{{{ getRightClickPopup() method
	/**
	 * Returns the right click popup menu.
	 */
	public final JPopupMenu getRightClickPopup()
	{
		return popup;
	} //}}}

	//{{{ setRightClickPopup() method
	/**
	 * Sets the right click popup menu.
	 * @param popup The popup
	 */
	public final void setRightClickPopup(JPopupMenu popup)
	{
		this.popup = popup;
	} //}}}

	//}}}

	//{{{ Scrolling

	//{{{ getFirstLine() method
	/**
	 * Returns the line displayed at the text area's origin. This is
	 * a virtual, not a physical, line number. See
	 * {@link FoldVisibilityManager#virtualToPhysical(int)}.
	 */
	public final int getFirstLine()
	{
		return firstLine;
	} //}}}

	//{{{ setFirstLine() method
	/**
	 * Sets the line displayed at the text area's origin.
	 *
	 * @param firstLine A virtual, not a physical, line number. See
	 * {@link FoldVisibilityManager#physicalToVirtual(int)}.
	 */
	public void setFirstLine(int firstLine)
	{
		if(firstLine == this.firstLine)
			return;

		_setFirstLine(firstLine);

		view.synchroScrollVertical(this,firstLine);
	} //}}}

	//{{{ _setFirstLine() method
	public void _setFirstLine(int firstLine)
	{
		firstLine = Math.max(0,Math.min(getVirtualLineCount() - 1,firstLine));
		this.firstLine = firstLine;

		physFirstLine = virtualToPhysical(firstLine);

		maxHorizontalScrollWidth = 0;

		chunkCache.setFirstLine(firstLine,physFirstLine,false);

		recalculateLastPhysicalLine();

		painter.repaint();
		gutter.repaint();

		if(this.firstLine != vertical.getValue())
			updateScrollBars();

		fireScrollEvent(true);
	} //}}}

	//{{{ getVisibleLines() method
	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines()
	{
		return visibleLines;
	} //}}}

	//{{{ getFirstPhysicalLine() method
	/**
	 * Returns the first visible physical line index.
	 * @since jEdit 4.0pre4
	 */
	public final int getFirstPhysicalLine()
	{
		return physFirstLine;
	} //}}}

	//{{{ getLastPhysicalLine() method
	/**
	 * Returns the last visible physical line index.
	 * @since jEdit 4.0pre4
	 */
	public final int getLastPhysicalLine()
	{
		return physLastLine;
	} //}}}

	//{{{ getHorizontalOffset() method
	/**
	 * Returns the horizontal offset of drawn lines.
	 */
	public final int getHorizontalOffset()
	{
		return horizontalOffset;
	} //}}}

	//{{{ setHorizontalOffset() method
	/**
	 * Sets the horizontal offset of drawn lines. This can be used to
	 * implement horizontal scrolling.
	 * @param horizontalOffset offset The new horizontal offset
	 */
	public void setHorizontalOffset(int horizontalOffset)
	{
		if(horizontalOffset == this.horizontalOffset)
			return;
		_setHorizontalOffset(horizontalOffset);

		view.synchroScrollHorizontal(this,horizontalOffset);
	} //}}}

	//{{{ _setHorizontalOffset() method
	public void _setHorizontalOffset(int horizontalOffset)
	{
		this.horizontalOffset = horizontalOffset;
		if(horizontalOffset != horizontal.getValue())
			updateScrollBars();
		painter.repaint();

		fireScrollEvent(false);
	} //}}}

	//{{{ updateScrollBars() method
	/**
	 * Updates the state of the scroll bars. This should be called
	 * if the number of lines in the buffer changes, or when the
	 * size of the text are changes.
	 */
	public void updateScrollBars()
	{
		if(vertical != null && visibleLines != 0)
		{
			// don't display stuff past the end of the buffer if
			// we can help it
			int lineCount = getVirtualLineCount();

			// very stupid but proper fix will go in later
			if(softWrap)
				lineCount += visibleLines - 1;

			if(lineCount < firstLine + visibleLines)
			{
				// this will call updateScrollBars(), so
				// just return...
				int newFirstLine = Math.max(0,lineCount - visibleLines);
				if(newFirstLine != firstLine)
				{
					setFirstLine(newFirstLine);
					return;
				}
			}

			vertical.setValues(firstLine,visibleLines,0,lineCount);
			vertical.setUnitIncrement(2);
			vertical.setBlockIncrement(visibleLines);
		}

		int width = painter.getWidth();
		if(horizontal != null && width != 0)
		{
			maxHorizontalScrollWidth = 0;
			painter.repaint();

			horizontal.setUnitIncrement(painter.getFontMetrics()
				.charWidth('w'));
			horizontal.setBlockIncrement(width / 2);
		}
	} //}}}

	//{{{ scrollUpLine() method
	/**
	 * Scrolls up by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpLine()
	{
		if(firstLine > 0)
			setFirstLine(firstLine - 1);
		//else
		//	getToolkit().beep();
	} //}}}

	//{{{ scrollUpPage() method
	/**
	 * Scrolls up by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpPage()
	{
		if(firstLine > 0)
		{
			int newFirstLine;
			if(softWrap)
			{
				newFirstLine = firstLine;
				int screenLineCount = 0;
				while(--newFirstLine >= 0)
				{
					int lines = chunkCache
						.getLineInfosForPhysicalLine(
						newFirstLine).length;
					if(screenLineCount + lines >= visibleLines)
						break;
					else
						screenLineCount += lines;
				}
			}
			else
			{
				newFirstLine = firstLine - visibleLines;
			}
			setFirstLine(newFirstLine);
		}
		/* else
		{
			getToolkit().beep();
		} */
	} //}}}

	//{{{ scrollDownLine() method
	/**
	 * Scrolls down by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownLine()
	{
		int numLines = getVirtualLineCount();

		if((softWrap && firstLine + 1 < numLines) ||
			(firstLine + visibleLines < numLines))
			setFirstLine(firstLine + 1);
		//else
		//	getToolkit().beep();
	} //}}}

	//{{{ scrollDownPage() method
	/**
	 * Scrolls down by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownPage()
	{
		int numLines = getVirtualLineCount();

		if((softWrap && firstLine + 1 < numLines) ||
			(firstLine + visibleLines < numLines))
		{
			setFirstLine(physicalToVirtual(physLastLine));
		}
		//else
		//	getToolkit().beep();
	} //}}}

	//{{{ scrollToCaret() method
	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 * @param doElectricScroll If true, electric scrolling will be performed
	 */
	public void scrollToCaret(boolean doElectricScroll)
	{
		scrollTo(caretLine,caret - buffer.getLineStartOffset(caretLine),
			doElectricScroll);
	} //}}}

	//{{{ scrollTo() method
	/**
	 * Ensures that the specified location in the buffer is visible.
	 * @param line The line number
	 * @param offset The offset from the start of the line
	 * @param doElectricScroll If true, electric scrolling will be performed
	 * @since jEdit 4.0pre6
	 */
	public void scrollTo(int line, int offset, boolean doElectricScroll)
	{
		int extraEndVirt;
		int lineLength = buffer.getLineLength(line);
		if(offset > lineLength)
		{
			extraEndVirt = charWidth * (offset - lineLength);
			offset = lineLength;
		}
		else
			extraEndVirt = 0;

		int _electricScroll = (doElectricScroll
			&& visibleLines > electricScroll * 2
			? electricScroll : 0);

		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if(visibleLines == 0)
		{
			setFirstLine(physicalToVirtual(
				Math.max(0,line - _electricScroll)));
			return;
		}

		//{{{ STAGE 1 -- determine if the caret is visible.
		int screenLine = getScreenLineOfOffset(buffer.getLineStartOffset(line) + offset);
		Point point;
		if(screenLine != -1)
		{
			// It's visible, but is it too close to the borders?
			int height = painter.getFontMetrics().getHeight();

			int y1 = (firstLine == 0 ? 0 : height * _electricScroll);
			int y2 = (visibleLines + firstLine == getVirtualLineCount()
				? 0 : height * _electricScroll);

			Rectangle rect = new Rectangle(0,y1,
				painter.getWidth() - 5,visibleLines * height
				- y1 - y2);

			point = offsetToXY(line,offset,returnValue);
			point.x += extraEndVirt;
			if(rect.contains(point))
				return;
		}
		else
			point = null;
		//}}}

		//{{{ STAGE 2 -- scroll vertically
		if(line == physLastLine + 1)
		{
			int count = chunkCache.getLineInfosForPhysicalLine(physLastLine).length
				+ chunkCache.getLineInfosForPhysicalLine(physLastLine + 1).length
				+ _electricScroll;
			while(count > 0)
			{
				count -= chunkCache.getLineInfosForPhysicalLine(physFirstLine).length;
				firstLine++;
				physFirstLine = foldVisibilityManager.getNextVisibleLine(physFirstLine);
			}
		}
		else if(screenLine == -1)
		{
			if(line == physLastLine)
			{
				int count = chunkCache.getLineInfosForPhysicalLine(physLastLine).length
					+ _electricScroll;
				while(count > 0)
				{
					count -= chunkCache.getLineInfosForPhysicalLine(physFirstLine).length;
					firstLine++;
					int nextLine = foldVisibilityManager.getNextVisibleLine(physFirstLine);
					if(nextLine == -1)
						break;
					else
						physFirstLine = nextLine;
				}
			}

			int virtualLine = foldVisibilityManager.physicalToVirtual(line);
			if(virtualLine == firstLine - 1)
			{
				firstLine = Math.max(0,firstLine - _electricScroll - 1);
				physFirstLine = foldVisibilityManager.virtualToPhysical(firstLine);
			}
			else
			{
				// keep chunking lines until we have visibleLines / 2
				if(!softWrap && virtualLine >= foldVisibilityManager.getVirtualLineCount()
					- visibleLines / 2)
				{
					firstLine = foldVisibilityManager.getVirtualLineCount()
						- visibleLines;
					physFirstLine = foldVisibilityManager
						.virtualToPhysical(firstLine);
				}
				else
				{
					physFirstLine = line;

					int count = 0;

					for(;;)
					{
						if(foldVisibilityManager.isLineVisible(physFirstLine))
						{
							int incr = chunkCache.getLineInfosForPhysicalLine(physFirstLine).length;
							if(count + incr > visibleLines / 2)
								break;
							else
								count += incr;
						}

						int prevLine = foldVisibilityManager
							.getPrevVisibleLine(physFirstLine);
						if(prevLine == -1)
							break;
						else
							physFirstLine = prevLine;
					}

					firstLine = physicalToVirtual(physFirstLine);
				}
			}
		}
		else if(screenLine < _electricScroll && firstLine != 0)
		{
			int count = _electricScroll - screenLine;
			while(count > 0 && firstLine > 0)
			{
				count -= chunkCache.getLineInfosForPhysicalLine(physFirstLine).length;
				firstLine--;
				physFirstLine = foldVisibilityManager.getPrevVisibleLine(physFirstLine);
			}
		}
		else if(screenLine >= visibleLines - _electricScroll)
		{
			int count = _electricScroll - visibleLines + screenLine + 1;
			while(count > 0 && firstLine <= getVirtualLineCount())
			{
				count -= chunkCache.getLineInfosForPhysicalLine(physFirstLine).length;
				firstLine++;
				physFirstLine = foldVisibilityManager.getNextVisibleLine(physFirstLine);
			}
		}

		chunkCache.setFirstLine(firstLine,physFirstLine,false);

		recalculateLastPhysicalLine();

		if(point == null)
		{
			point = offsetToXY(line,offset,returnValue);
			if(point == null)
			{
				// a soft wrapped line has more screen lines
				// than the number of visible lines
				return;
			}
			else
				point.x += extraEndVirt;
		} //}}}

		//{{{ STAGE 3 -- scroll horizontally
		if(point.x < 0)
		{
			horizontalOffset = Math.min(0,horizontalOffset
				- point.x + charWidth + 5);
		}
		else if(point.x >= painter.getWidth() - charWidth - 5)
		{
			horizontalOffset = horizontalOffset +
				(painter.getWidth() - point.x)
				- charWidth - 5;
		} //}}}

		//{{{ STAGE 4 -- update some stuff
		updateScrollBars();
		painter.repaint();
		gutter.repaint();

		view.synchroScrollVertical(this,firstLine);
		view.synchroScrollHorizontal(this,horizontalOffset);

		// fire events for both a horizontal and vertical scroll
		fireScrollEvent(true);
		fireScrollEvent(false);
		//}}}
	} //}}}

	//{{{ addScrollListener() method
	/**
	 * Adds a scroll listener to this text area.
	 * @param listener The listener
	 * @since jEdit 3.2pre2
	 */
	public final void addScrollListener(ScrollListener listener)
	{
		listenerList.add(ScrollListener.class,listener);
	} //}}}

	//{{{ removeScrollListener() method
	/**
	 * Removes a scroll listener from this text area.
	 * @param listener The listener
	 * @since jEdit 3.2pre2
	 */
	public final void removeScrollListener(ScrollListener listener)
	{
		listenerList.remove(ScrollListener.class,listener);
	} //}}}

	//}}}

	//{{{ Screen line stuff

	//{{{ getPhysicalLineOfScreenLine() method
	/**
	 * Returns the physical line number that contains the specified screen
	 * line.
	 * @param screenLine The screen line
	 * @since jEdit 4.0pre6
	 */
	public int getPhysicalLineOfScreenLine(int screenLine)
	{
		chunkCache.updateChunksUpTo(screenLine);
		return chunkCache.getLineInfo(screenLine).physicalLine;
	} //}}}

	//{{{ getScreenLineOfOffset() method
	/**
	 * Returns the screen (wrapped) line containing the specified offset.
	 * @param offset The offset
	 * @since jEdit 4.0pre4
	 */
	public int getScreenLineOfOffset(int offset)
	{
		int line = buffer.getLineOfOffset(offset);
		offset -= buffer.getLineStartOffset(line);
		return chunkCache.getScreenLineOfOffset(line,offset);
	} //}}}

	//{{{ getScreenLineStartOffset() method
	/**
	 * Returns the start offset of the specified screen (wrapped) line.
	 * @param line The line
	 * @since jEdit 4.0pre4
	 */
	public int getScreenLineStartOffset(int line)
	{
		chunkCache.updateChunksUpTo(line);
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(line);
		if(lineInfo.physicalLine == -1)
			return -1;

		return buffer.getLineStartOffset(lineInfo.physicalLine)
			+ lineInfo.offset;
	} //}}}

	//{{{ getScreenLineEndOffset() method
	/**
	 * Returns the end offset of the specified screen (wrapped) line.
	 * @param line The line
	 * @since jEdit 4.0pre4
	 */
	public int getScreenLineEndOffset(int line)
	{
		chunkCache.updateChunksUpTo(line);
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(line);
		if(lineInfo.physicalLine == -1)
			return -1;

		return buffer.getLineStartOffset(lineInfo.physicalLine)
			+ lineInfo.offset + lineInfo.length;
	} //}}}

	//}}}

	//{{{ Offset conversion

	//{{{ xyToOffset() method
	/**
	 * Converts a point to an offset.
	 * Note that unlike in previous jEdit versions, this method now returns
	 * -1 if the y co-ordinate is out of bounds.
	 *
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		return xyToOffset(x,y,true);
	} //}}}

	//{{{ xyToOffset() method
	/**
	 * Converts a point to an offset.
	 * Note that unlike in previous jEdit versions, this method now returns
	 * -1 if the y co-ordinate is out of bounds.
	 *
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 3.2pre6
	 */
	public int xyToOffset(int x, int y, boolean round)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		int line = y / height;

		if(line < 0 || line > visibleLines)
			return -1;

		chunkCache.updateChunksUpTo(line);

		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(line);
		if(!lineInfo.chunksValid)
			System.err.println("xy to offset: not valid");

		if(lineInfo.physicalLine == -1)
		{
			return getLineEndOffset(foldVisibilityManager
				.getLastVisibleLine()) - 1;
		}
		else
		{
			int offset = Chunk.xToOffset(lineInfo.chunks,
				x - horizontalOffset,round);
			if(offset == -1 || offset == lineInfo.offset + lineInfo.length)
				offset = lineInfo.offset + lineInfo.length - 1;

			return getLineStartOffset(lineInfo.physicalLine) + offset;
		}
	} //}}}

	//{{{ offsetToXY() method
	/**
	 * Converts an offset into a point in the text area painter's
	 * co-ordinate space.
	 * @param offset The offset
	 * @return The location of the offset on screen, or <code>null</code>
	 * if the specified offset is not visible
	 */
	public Point offsetToXY(int offset)
	{
		int line = buffer.getLineOfOffset(offset);
		offset -= buffer.getLineStartOffset(line);
		Point retVal = new Point();
		return offsetToXY(line,offset,retVal);
	} //}}}

	//{{{ offsetToXY() method
	/**
	 * Converts an offset into a point in the text area painter's
	 * co-ordinate space.
	 * @param line The physical line number
	 * @param offset The offset, from the start of the line
	 * @param retVal The point to store the return value in
	 * @return <code>retVal</code> for convenience, or <code>null</code>
	 * if the specified offset is not visible
	 * @since jEdit 4.0pre4
	 */
	public Point offsetToXY(int line, int offset, Point retVal)
	{
		int screenLine = chunkCache.getScreenLineOfOffset(line,offset);
		if(screenLine == -1)
		{
			if(line < physFirstLine)
				return null;
			// must have >= here because the last physical line
			// might only be partially visible (some offsets would
			// have a screen line, others would return -1 and hence
			// this code would be executed)
			else if(line >= physLastLine)
				return null;
			else
			{
				throw new InternalError("line=" + line
					+ ",offset=" + offset
					+ ",screenLine=" + screenLine
					+ ",physFirstLine=" + physFirstLine
					+ ",physLastLine=" + physLastLine);
			}
		}

		FontMetrics fm = painter.getFontMetrics();

		retVal.y = screenLine * fm.getHeight();

		chunkCache.updateChunksUpTo(screenLine);
		ChunkCache.LineInfo info = chunkCache.getLineInfo(screenLine);
		if(!info.chunksValid)
			System.err.println("offset to xy: not valid");

		retVal.x = (int)(horizontalOffset + Chunk.offsetToX(
			info.chunks,offset));

		return retVal;
	} //}}}

	//}}}

	//{{{ Painting

	//{{{ invalidateScreenLineRange() method
	/**
	 * Marks a range of screen lines as needing a repaint.
	 * @param start The first line
	 * @param end The last line
	 * @since jEdit 4.0pre4
	 */
	public void invalidateScreenLineRange(int start, int end)
	{
		//if(start != end)
		//	System.err.println(start + ":" + end + ":" + chunkCache.needFullRepaint());
		if(chunkCache.needFullRepaint())
		{
			recalculateLastPhysicalLine();
			gutter.repaint();
			painter.repaint();
			return;
		}

		if(start > end)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		FontMetrics fm = painter.getFontMetrics();
		int y = start * fm.getHeight();
		int height = (end - start + 1) * fm.getHeight();
		painter.repaint(0,y,painter.getWidth(),height);
		gutter.repaint(0,y,gutter.getWidth(),height);
	} //}}}

	//{{{ invalidateLine() method
	/**
	 * Marks a line as needing a repaint.
	 * @param line The physical line to invalidate
	 */
	public void invalidateLine(int line)
	{
		if(line < physFirstLine || line > physLastLine
			|| !foldVisibilityManager.isLineVisible(line))
			return;

		int startLine = -1;
		int endLine = -1;

		for(int i = 0; i <= visibleLines; i++)
		{
			chunkCache.updateChunksUpTo(i);
			ChunkCache.LineInfo info = chunkCache.getLineInfo(i);

			if((info.physicalLine >= line || info.physicalLine == -1)
				&& startLine == -1)
			{
				startLine = i;
			}

			if((info.physicalLine >= line && info.lastSubregion)
				|| info.physicalLine == -1)
			{
				endLine = i;
				break;
			}
		}

		if(chunkCache.needFullRepaint())
		{
			recalculateLastPhysicalLine();
			endLine = visibleLines;
		}
		else if(endLine == -1)
			endLine = visibleLines;

		//if(startLine != endLine)
		//	System.err.println(startLine + ":" + endLine);

		invalidateScreenLineRange(startLine,endLine);
	} //}}}

	//{{{ invalidateLineRange() method
	/**
	 * Marks a range of physical lines as needing a repaint.
	 * @param start The first line to invalidate
	 * @param end The last line to invalidate
	 */
	public void invalidateLineRange(int start, int end)
	{
		if(end < start)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		if(end < physFirstLine || start > physLastLine)
			return;

		int startScreenLine = -1;
		int endScreenLine = -1;

		for(int i = 0; i <= visibleLines; i++)
		{
			chunkCache.updateChunksUpTo(i);
			ChunkCache.LineInfo info = chunkCache.getLineInfo(i);

			if((info.physicalLine >= start || info.physicalLine == -1)
				&& startScreenLine == -1)
			{
				startScreenLine = i;
			}

			if((info.physicalLine >= end && info.lastSubregion)
				|| info.physicalLine == -1)
			{
				endScreenLine = i;
				break;
			}
		}

		if(startScreenLine == -1)
			startScreenLine = 0;

		if(chunkCache.needFullRepaint())
		{
			recalculateLastPhysicalLine();
			endScreenLine = visibleLines;
		}
		else if(endScreenLine == -1)
			endScreenLine = visibleLines;

		invalidateScreenLineRange(startScreenLine,endScreenLine);
	} //}}}

	//{{{ invalidateSelectedLines() method
	/**
	 * Repaints the lines containing the selection.
	 */
	public void invalidateSelectedLines()
	{
		// to hide line highlight if selections are being added later on
		invalidateLine(caretLine);

		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			invalidateLineRange(s.startLine,s.endLine);
		}
	} //}}}

	//}}}

	//{{{ Convenience methods

	//{{{ physicalToVirtual() method
	/**
	 * Converts a physical line number to a virtual line number.
	 * See {@link FoldVisibilityManager} for details.
	 * @param line A physical line index
	 * @since jEdit 4.0pre1
	 */
	public int physicalToVirtual(int line)
	{
		return foldVisibilityManager.physicalToVirtual(line);
	} //}}}

	//{{{ virtualToPhysical() method
	/**
	 * Converts a virtual line number to a physical line number.
	 * See {@link FoldVisibilityManager} for details.
	 * @param line A virtual line index
	 * @since jEdit 4.0pre1
	 */
	public int virtualToPhysical(int line)
	{
		return foldVisibilityManager.virtualToPhysical(line);
	} //}}}

	//{{{ getBufferLength() method
	/**
	 * Returns the length of the buffer.
	 */
	public final int getBufferLength()
	{
		return buffer.getLength();
	} //}}}

	//{{{ getLineCount() method
	/**
	 * Returns the number of physical lines in the buffer.
	 */
	public final int getLineCount()
	{
		return buffer.getLineCount();
	} //}}}

	//{{{ getVirtualLineCount() method
	/**
	 * Returns the number of virtual lines in the buffer.
	 */
	public final int getVirtualLineCount()
	{
		return foldVisibilityManager.getVirtualLineCount();
	} //}}}

	//{{{ getLineOfOffset() method
	/**
	 * Returns the line containing the specified offset.
	 * @param offset The offset
	 */
	public final int getLineOfOffset(int offset)
	{
		return buffer.getLineOfOffset(offset);
	} //}}}

	//{{{ getLineStartOffset() method
	/**
	 * Returns the start offset of the specified line.
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is
	 * invalid
	 */
	public int getLineStartOffset(int line)
	{
		return buffer.getLineStartOffset(line);
	} //}}}

	//{{{ getLineEndOffset() method
	/**
	 * Returns the end offset of the specified line.
	 * @param line The line
	 * @return The end offset of the specified line, or -1 if the line is
	 * invalid.
	 */
	public int getLineEndOffset(int line)
	{
		return buffer.getLineEndOffset(line);
	} //}}}

	//{{{ getLineLength() method
	/**
	 * Returns the length of the specified line.
	 * @param line The line
	 */
	public int getLineLength(int line)
	{
		return buffer.getLineLength(line);
	} //}}}

	//{{{ getText() method
	/**
	 * Returns the specified substring of the buffer.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring
	 */
	public final String getText(int start, int len)
	{
		return buffer.getText(start,len);
	} //}}}

	//{{{ getText() method
	/**
	 * Copies the specified substring of the buffer into a segment.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		buffer.getText(start,len,segment);
	} //}}}

	//{{{ getLineText() method
	/**
	 * Returns the text on the specified line.
	 * @param lineIndex The line
	 * @return The text, or null if the line is invalid
	 */
	public final String getLineText(int lineIndex)
	{
		return buffer.getLineText(lineIndex);
	} //}}}

	//{{{ getLineText() method
	/**
	 * Copies the text on the specified line into a segment. If the line
	 * is invalid, the segment will contain a null string.
	 * @param lineIndex The line
	 */
	public final void getLineText(int lineIndex, Segment segment)
	{
		buffer.getLineText(lineIndex,segment);
	} //}}}

	//{{{ getText() method
	/**
	 * Returns the entire text of this text area.
	 */
	public String getText()
	{
		return buffer.getText(0,buffer.getLength());
	} //}}}

	//{{{ setText() method
	/**
	 * Sets the entire text of this text area.
	 */
	public void setText(String text)
	{
		try
		{
			buffer.beginCompoundEdit();
			buffer.remove(0,buffer.getLength());
			buffer.insert(0,text);
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}

	//}}}

	//{{{ Selection

	//{{{ selectAll() method
	/**
	 * Selects all text in the buffer.
	 */
	public final void selectAll()
	{
		setSelection(new Selection.Range(0,buffer.getLength()));
		moveCaretPosition(buffer.getLength(),true);
	} //}}}

	//{{{ selectLine() method
	/**
	 * Selects the current line.
	 * @since jEdit 2.7pre2
	 */
	public void selectLine()
	{
		int caretLine = getCaretLine();
		int start = getLineStartOffset(caretLine);
		int end = getLineEndOffset(caretLine) - 1;
		Selection s = new Selection.Range(start,end);
		if(multi)
			addToSelection(s);
		else
			setSelection(s);
		moveCaretPosition(end);
	} //}}}

	//{{{ selectParagraph() method
	/**
	 * Selects the paragraph at the caret position.
	 * @since jEdit 2.7pre2
	 */
	public void selectParagraph()
	{
		int caretLine = getCaretLine();

		if(getLineLength(caretLine) == 0)
		{
			view.getToolkit().beep();
			return;
		}

		int start = caretLine;
		int end = caretLine;

		while(start >= 0)
		{
			if(getLineLength(start) == 0)
				break;
			else
				start--;
		}

		while(end < getLineCount())
		{
			if(getLineLength(end) == 0)
				break;
			else
				end++;
		}

		int selectionStart = getLineStartOffset(start + 1);
		int selectionEnd = getLineEndOffset(end - 1) - 1;
		Selection s = new Selection.Range(selectionStart,selectionEnd);
		if(multi)
			addToSelection(s);
		else
			setSelection(s);
		moveCaretPosition(selectionEnd);
	} //}}}

	//{{{ selectWord() method
	
	
	/**
	 * Selects the word at the caret position.
	 * @since jEdit 2.7pre2
	 */
	public void selectWord()
	{
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int offset = getCaretPosition() - lineStart;

		if(getLineLength(line) == 0)
			return;

		String lineText = getLineText(line);
		String noWordSep = buffer.getStringProperty("noWordSep");

		if(offset == getLineLength(line))
			offset--;

		int wordStart = TextUtilities.findWordStart(lineText,offset,noWordSep);
		int wordEnd = TextUtilities.findWordEnd(lineText,offset+1,noWordSep);

		Selection s = new Selection.Range(lineStart + wordStart,
			lineStart + wordEnd);
		if(multi)
			addToSelection(s);
		else
			setSelection(s);
		moveCaretPosition(lineStart + wordEnd);
	} //}}}

	//{{{ selectToMatchingBracket() method
	/**
	 * Selects from the bracket at the specified position to the 
	 * corresponding bracket. Returns <code>true</code> if successful (i.e.
	 * there's a bracket at the specified position and there's a matching 
	 * bracket for it), <code>false</code> otherwise.
	 * @since jEdit 4.1pre1
	 */
	public boolean selectToMatchingBracket(int position)
	{
		int positionLine = buffer.getLineOfOffset(position);
		int lineOffset = position - buffer.getLineStartOffset(positionLine);

		int bracket = TextUtilities.findMatchingBracket(buffer,positionLine,lineOffset);
		
		if(bracket != -1)
		{
			Selection s;

			if(bracket < position)
			{
				moveCaretPosition(position,false);
				s = new Selection.Range(++bracket,position);
			}
			else
			{
				moveCaretPosition(position + 1,false);
				s = new Selection.Range(position + 1,bracket);
			}

			if(!multi)
				selectNone();

			addToSelection(s);
			return true;
		}
		
		return false;
	} //}}}

	//{{{ selectToMatchingBracket() method
	/**
	 * Selects from the bracket at the caret position to the corresponding
	 * bracket.
	 * @since jEdit 4.0pre2
	 */
	public void selectToMatchingBracket()
	{
		// since we might change it below
		int caret = this.caret;

		int offset = caret - buffer.getLineStartOffset(caretLine);

		if(buffer.getLineLength(caretLine) == 0)
			return;

		if(offset == buffer.getLineLength(caretLine))
			caret--;
		
		selectToMatchingBracket(caret);
	} //}}}

	//{{{ selectBlock() method
	/**
	 * Selects the code block surrounding the caret.
	 * @since jEdit 2.7pre2
	 */
	public void selectBlock()
	{
		String openBrackets = "([{";
		String closeBrackets = ")]}";

		Selection s = getSelectionAtOffset(caret);
		int start, end;
		if(s == null)
			start = end = caret;
		else
		{
			start = s.start;
			end = s.end;
		}

		String text = getText(0,buffer.getLength());

		// Scan backwards, trying to find a bracket
		int count = 1;
		char openBracket = '\0';
		char closeBracket = '\0';

		// We can't do the backward scan if start == 0
		if(start == 0)
		{
			view.getToolkit().beep();
			return;
		}

backward_scan:	while(--start > 0)
		{
			char c = text.charAt(start);
			int index = openBrackets.indexOf(c);
			if(index != -1)
			{
				if(--count == 0)
				{
					openBracket = c;
					closeBracket = closeBrackets.charAt(index);
					break backward_scan;
				}
			}
			else if(closeBrackets.indexOf(c) != -1)
				count++;
		}

		// Reset count
		count = 1;

		// Scan forward, matching that bracket
		if(openBracket == '\0')
		{
			getToolkit().beep();
			return;
		}
		else
		{
forward_scan:		do
			{
				char c = text.charAt(end);
				if(c == closeBracket)
				{
					if(--count == 0)
					{
						end++;
						break forward_scan;
					}
				}
				else if(c == openBracket)
					count++;
			}
			while(++end < buffer.getLength());
		}

		s = new Selection.Range(start,end);
		if(multi)
			addToSelection(s);
		else
			setSelection(s);
		moveCaretPosition(end);
	} //}}}

	//{{{ invertSelection() method
	/**
	 * Inverts the selection.
	 * @since jEdit 4.0pre1
	 */
	public final void invertSelection()
	{
		Selection[] newSelection = new Selection[selection.size() + 1];
		int lastOffset = 0;
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			newSelection[i] = new Selection.Range(lastOffset,
				s.getStart());
			lastOffset = s.getEnd();
		}
		newSelection[selection.size()] = new Selection.Range(
			lastOffset,buffer.getLength());
		setSelection(newSelection);
	} //}}}

	//{{{ getSelectionCount() method
	/**
	 * Returns the number of selections. This can be used to test
	 * for the existence of selections.
	 * @since jEdit 3.2pre2
	 */
	public int getSelectionCount()
	{
		return selection.size();
	} //}}}

	//{{{ getSelection() method
	/**
	 * Returns the current selection.
	 * @since jEdit 3.2pre1
	 */
	public Selection[] getSelection()
	{
		Selection[] sel = new Selection[selection.size()];
		selection.copyInto(sel);
		return sel;
	} //}}}

	//{{{ selectNone() method
	/**
	 * Deselects everything.
	 */
	public void selectNone()
	{
		setSelection((Selection)null);
	} //}}}

	//{{{ setSelection() method
	/**
	 * Sets the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void setSelection(Selection[] selection)
	{
		// invalidate the old selection
		invalidateSelectedLines();

		this.selection.removeAllElements();

		if(selection != null)
		{
			for(int i = 0; i < selection.length; i++)
				_addToSelection(selection[i]);
		}

		fireCaretEvent();
	} //}}}

	//{{{ setSelection() method
	/**
	 * Sets the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void setSelection(Selection selection)
	{
		invalidateSelectedLines();
		this.selection.removeAllElements();

		if(selection != null)
			_addToSelection(selection);

		fireCaretEvent();
	} //}}}

	//{{{ addToSelection() method
	/**
	 * Adds to the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection[] selection)
	{
		if(selection != null)
		{
			for(int i = 0; i < selection.length; i++)
				_addToSelection(selection[i]);
		}

		// to hide current line highlight
		invalidateLine(caretLine);

		fireCaretEvent();
	} //}}}

	//{{{ addToSelection() method
	/**
	 * Adds to the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection selection)
	{
		_addToSelection(selection);

		// to hide current line highlight
		invalidateLine(caretLine);

		fireCaretEvent();
	} //}}}

	//{{{ getSelectionAtOffset() method
	/**
	 * Returns the selection containing the specific offset, or <code>null</code>
	 * if there is no selection at that offset.
	 * @param offset The offset
	 * @since jEdit 3.2pre1
	 */
	public Selection getSelectionAtOffset(int offset)
	{
		if(selection != null)
		{
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);
				if(offset >= s.start && offset <= s.end)
					return s;
			}
		}

		return null;
	} //}}}

	//{{{ removeFromSelection() method
	/**
	 * Deactivates the specified selection.
	 * @param s The selection
	 * @since jEdit 3.2pre1
	 */
	public void removeFromSelection(Selection sel)
	{
		selection.removeElement(sel);
		invalidateLineRange(sel.startLine,sel.endLine);

		// to hide current line highlight
		invalidateLine(caretLine);

		fireCaretEvent();
	} //}}}

	/* //{{{ removeFromSelection() method
	/**
	 * Deactivates the selection at the specified offset. If there is
	 * no selection at that offset, does nothing.
	 * @param offset The offset
	 * @since jEdit 3.2pre1
	 */
	public void removeFromSelection(int offset)
	{
		Selection sel = getSelectionAtOffset(offset);
		if(sel == null)
			return;

		selection.removeElement(sel);
		invalidateLineRange(sel.startLine,sel.endLine);

		// to hide current line highlight
		invalidateLine(caretLine);

		fireCaretEvent();
	} //}}} */

	//{{{ resizeSelection() method
	/**
	 * Resizes the selection at the specified offset, or creates a new
	 * one if there is no selection at the specified offset. This is a
	 * utility method that is mainly useful in the mouse event handler
	 * because it handles the case of end being before offset gracefully
	 * (unlike the rest of the selection API).
	 * @param offset The offset
	 * @param end The new selection end
	 * @param extraEndVirt Only for rectangular selections - specifies how
	 * far it extends into virtual space.
	 * @param rect Make the selection rectangular?
	 * @since jEdit 3.2pre1
	 */
	public void resizeSelection(int offset, int end, int extraEndVirt,
		boolean rect)
	{
		Selection s = getSelectionAtOffset(offset);
		if(s != null)
		{
			invalidateLineRange(s.startLine,s.endLine);
			selection.removeElement(s);
		}

		boolean reversed = false;
		if(end < offset)
		{
			int tmp = offset;
			offset = end;
			end = tmp;
			reversed = true;
		}

		Selection newSel;
		if(rect)
		{
			Selection.Rect rectSel = new Selection.Rect(offset,end);
			if(reversed)
				rectSel.extraStartVirt = extraEndVirt;
			else
				rectSel.extraEndVirt = extraEndVirt;
			newSel = rectSel;
		}
		else
			newSel = new Selection.Range(offset,end);

		_addToSelection(newSel);
		fireCaretEvent();
	} //}}}

	//{{{ extendSelection() method
	/**
	 * Extends the selection at the specified offset, or creates a new
	 * one if there is no selection at the specified offset. This is
	 * different from resizing in that the new chunk is added to the
	 * selection in question, instead of replacing it.
	 * @param offset The offset
	 * @param end The new selection end
	 * @param rect Make the selection rectangular?
	 * @since jEdit 3.2pre1
	 */
	public void extendSelection(int offset, int end)
	{
		Selection s = getSelectionAtOffset(offset);
		if(s != null)
		{
			invalidateLineRange(s.startLine,s.endLine);
			selection.removeElement(s);

			if(offset == s.start)
			{
				offset = end;
				end = s.end;
			}
			else if(offset == s.end)
			{
				offset = s.start;
			}
		}

		if(end < offset)
		{
			int tmp = end;
			end = offset;
			offset = tmp;
		}

		_addToSelection(new Selection.Range(offset,end));
		fireCaretEvent();
	} //}}}

	//{{{ getSelectedText() method
	/**
	 * Returns the text in the specified selection.
	 * @param s The selection
	 * @since jEdit 3.2pre1
	 */
	public String getSelectedText(Selection s)
	{
		StringBuffer buf = new StringBuffer();
		s.getText(buffer,buf);
		return buf.toString();
	} //}}}

	//{{{ getSelectedText() method
	/**
	 * Returns the text in all active selections.
	 * @param separator The string to insert between each text chunk
	 * (for example, a newline)
	 * @since jEdit 3.2pre1
	 */
	public String getSelectedText(String separator)
	{
		if(selection.size() == 0)
			return null;

		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < selection.size(); i++)
		{
			if(i != 0)
				buf.append(separator);

			((Selection)selection.elementAt(i)).getText(buffer,buf);
		}

		return buf.toString();
	} //}}}

	//{{{ getSelectedText() method
	/**
	 * Returns the text in all active selections, with a newline
	 * between each text chunk.
	 */
	public String getSelectedText()
	{
		return getSelectedText("\n");
	} //}}}

	//{{{ setSelectedText() method
	/**
	 * Replaces the selection with the specified text.
	 * @param s The selection
	 * @param selectedText The new text
	 * @since jEdit 3.2pre1
	 */
	public void setSelectedText(Selection s, String selectedText)
	{
		if(!isEditable())
		{
			throw new InternalError("Text component"
				+ " read only");
		}

		try
		{
			buffer.beginCompoundEdit();

			moveCaretPosition(s.setText(buffer,selectedText));
		}
		// No matter what happends... stops us from leaving buffer
		// in a bad state
		finally
		{
			buffer.endCompoundEdit();
		}

		// no no no!!!!
		//selectNone();
	} //}}}

	//{{{ setSelectedText() method
	/**
	 * Replaces the selection at the caret with the specified text.
	 * If there is no selection at the caret, the text is inserted at
	 * the caret position.
	 */
	public void setSelectedText(String selectedText)
	{
		if(!isEditable())
		{
			throw new InternalError("Text component"
				+ " read only");
		}

		Selection[] selection = getSelection();
		if(selection.length == 0)
		{
			// for compatibility with older jEdit versions
			buffer.insert(caret,selectedText);
		}
		else
		{
			try
			{
				int newCaret = -1;

				buffer.beginCompoundEdit();

				for(int i = 0; i < selection.length; i++)
				{
					newCaret = selection[i].setText(buffer,selectedText);
				}

				moveCaretPosition(newCaret);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}

		selectNone();
	} //}}}

	//{{{ getSelectedLines() method
	/**
	 * Returns a sorted array of line numbers on which a selection or
	 * selections are present.<p>
	 *
	 * This method is the most convenient way to iterate through selected
	 * lines in a buffer. The line numbers in the array returned by this
	 * method can be passed as a parameter to such methods as
	 * {@link org.gjt.sp.jedit.Buffer#getLineText(int)}.
	 *
	 * @since jEdit 3.2pre1
	 */
	public int[] getSelectedLines()
	{
		if(selection.size() == 0)
			return new int[] { caretLine };

		Integer line;

		Hashtable hash = new Hashtable();
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			int endLine = (s.end == getLineStartOffset(s.endLine)
				? s.endLine - 1
				: s.endLine);

			for(int j = s.startLine; j <= endLine; j++)
			{
				line = new Integer(j);
				hash.put(line,line);
			}
		}

		int[] returnValue = new int[hash.size()];
		int i = 0;

		Enumeration keys = hash.keys();
		while(keys.hasMoreElements())
		{
			line = (Integer)keys.nextElement();
			returnValue[i++] = line.intValue();
		}

		Arrays.sort(returnValue);

		return returnValue;
	} //}}}

	//{{{ showSelectLineRangeDialog() method
	/**
	 * Displays the 'select line range' dialog box, and selects the
	 * specified range of lines.
	 * @since jEdit 2.7pre2
	 */
	public void showSelectLineRangeDialog()
	{
		new SelectLineRange(view);
	} //}}}

	//}}}

	//{{{ Caret

	//{{{ blinkCaret() method
	/**
	 * Blinks the caret.
	 */
	public final void blinkCaret()
	{
		if(caretBlinks)
		{
			blink = !blink;
			invalidateLine(caretLine);
		}
		else
			blink = true;
	} //}}}

	//{{{ centerCaret() method
	/**
	 * Centers the caret on the screen.
	 * @since jEdit 2.7pre2
	 */
	public void centerCaret()
	{
		int offset = getScreenLineStartOffset(visibleLines / 2);
		if(offset == -1)
			getToolkit().beep();
		else
			setCaretPosition(offset);
	} //}}}

	//{{{ setCaretPosition() method
	/**
	 * Sets the caret position and deactivates the selection.
	 * @param caret The caret position
	 */
	public void setCaretPosition(int newCaret)
	{
		invalidateSelectedLines();
		selection.removeAllElements();
		moveCaretPosition(newCaret,true);
	} //}}}

	//{{{ setCaretPosition() method
	/**
	 * Sets the caret position and deactivates the selection.
	 * @param caret The caret position
	 * @param doElectricScroll Do electric scrolling?
	 */
	public void setCaretPosition(int newCaret, boolean doElectricScroll)
	{
		invalidateSelectedLines();
		selection.removeAllElements();
		moveCaretPosition(newCaret,doElectricScroll);
	} //}}}

	//{{{ moveCaretPosition() method
	/**
	 * Sets the caret position without deactivating the selection.
	 * @param caret The caret position
	 */
	public void moveCaretPosition(int newCaret)
	{
		moveCaretPosition(newCaret,true);
	} //}}}

	//{{{ moveCaretPosition() method
	/**
	 * Sets the caret position without deactivating the selection.
	 * @param caret The caret position
	 * @param doElectricScroll Do electric scrolling?
	 */
	public void moveCaretPosition(int newCaret, boolean doElectricScroll)
	{
		if(newCaret < 0 || newCaret > buffer.getLength())
		{
			throw new IllegalArgumentException("caret out of bounds: "
				+ newCaret);
		}

		if(caret == newCaret)
		{
			if(view.getTextArea() == this)
				finishCaretUpdate(doElectricScroll,false);
		}
		else
		{
			int newCaretLine = getLineOfOffset(newCaret);

			magicCaret = -1;

			if(!foldVisibilityManager.isLineVisible(newCaretLine))
			{
				if(foldVisibilityManager.isNarrowed())
				{
					int collapseFolds = buffer.getIntegerProperty(
						"collapseFolds",0);
					if(collapseFolds != 0)
					{
						foldVisibilityManager.expandFolds(collapseFolds);
						foldVisibilityManager.expandFold(newCaretLine,false);
					}
					else
						foldVisibilityManager.expandAllFolds();
				}
				else
					foldVisibilityManager.expandFold(newCaretLine,false);
			}

			if(caretLine == newCaretLine)
			{
				if(caretScreenLine != -1)
					invalidateScreenLineRange(caretScreenLine,caretScreenLine);
			}
			else
			{
				int newCaretScreenLine = chunkCache.getScreenLineOfOffset(newCaretLine,
					newCaret - buffer.getLineStartOffset(newCaretLine));
				if(caretScreenLine == -1)
					invalidateScreenLineRange(newCaretScreenLine,newCaretScreenLine);
				else
					invalidateScreenLineRange(caretScreenLine,newCaretScreenLine);
				caretScreenLine = newCaretScreenLine;
			}

			caret = newCaret;
			caretLine = newCaretLine;

			if(view.getTextArea() == this)
				finishCaretUpdate(doElectricScroll,true);
		}
	} //}}}

	//{{{ getCaretPosition() method
	/**
	 * Returns a zero-based index of the caret position.
	 */
	public int getCaretPosition()
	{
		return caret;
	} //}}}

	//{{{ getCaretLine() method
	/**
	 * Returns the line number containing the caret.
	 */
	public int getCaretLine()
	{
		return caretLine;
	} //}}}

	//{{{ getMagicCaretPosition() method
	/**
	 * @deprecated Do not call this method.
	 */
	public final int getMagicCaretPosition()
	{
		if(magicCaret == -1)
		{
			magicCaret = offsetToX(caretLine,caret
				- getLineStartOffset(caretLine));
		}

		return magicCaret;
	} //}}}

	//{{{ setMagicCaretPosition() method
	/**
	 * Sets the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 * @param magicCaret The magic caret position
	 */
	public final void setMagicCaretPosition(int magicCaret)
	{
		this.magicCaret = magicCaret;
	} //}}}

	//{{{ addCaretListener() method
	/**
	 * Adds a caret change listener to this text area.
	 * @param listener The listener
	 */
	public final void addCaretListener(CaretListener listener)
	{
		listenerList.add(CaretListener.class,listener);
	} //}}}

	//{{{ removeCaretListener() method
	/**
	 * Removes a caret change listener from this text area.
	 * @param listener The listener
	 */
	public final void removeCaretListener(CaretListener listener)
	{
		listenerList.remove(CaretListener.class,listener);
	} //}}}

	//{{{ getBracketPosition() method
	/**
	 * Returns the position of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketPosition()
	{
		return bracketPosition;
	} //}}}

	//{{{ getBracketLine() method
	/**
	 * Returns the line of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketLine()
	{
		return bracketLine;
	} //}}}

	//{{{ goToNextBracket() method
	/**
	 * Moves the caret to the next closing bracket.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextBracket(boolean select)
	{
		String text = getText(caret,buffer.getLength() - caret - 1);

		int newCaret = -1;

loop:		for(int i = 0; i < text.length(); i++)
		{
			switch(text.charAt(i))
			{
			case ')': case ']': case '}':
				newCaret = caret + i + 1;
				break loop;
			}
		}

		if(newCaret == -1)
			getToolkit().beep();
		else
		{
			if(select)
				extendSelection(caret,newCaret);
			else if(!multi)
				selectNone();
			moveCaretPosition(newCaret);
		}
	} //}}}

	//{{{ goToNextCharacter() method
	/**
	 * Moves the caret to the next character.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextCharacter(boolean select)
	{
		if(!select && selection.size() != 0)
		{
			Selection s = getSelectionAtOffset(caret);
			if(s != null)
			{
				if(multi)
				{
					if(caret != s.end)
					{
						moveCaretPosition(s.end);
						return;
					}
				}
				else
				{
					setCaretPosition(s.end);
					return;
				}
			}
		}

		if(caret == buffer.getLength())
			getToolkit().beep();

		int newCaret;

		if(caret == getLineEndOffset(caretLine) - 1)
		{
			int line = foldVisibilityManager.getNextVisibleLine(caretLine);
			if(line == -1)
			{
				getToolkit().beep();
				return;
			}

			newCaret = getLineStartOffset(line);
		}
		else
			newCaret = caret + 1;

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToNextLine() method
	/**
	 * Movse the caret to the next line.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextLine(boolean select)
	{
		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(caretLine);

		int caretFromStartOfLine = caret - buffer.getLineStartOffset(caretLine);
		int subregion = getSubregionOfOffset(caretFromStartOfLine,
			lineInfos);

		int magic = this.magicCaret;
		if(magic == -1)
		{
			magic = subregionOffsetToX(lineInfos[subregion],
				caretFromStartOfLine);
		}

		int newCaret;

		if(subregion != lineInfos.length - 1)
		{
			newCaret = buffer.getLineStartOffset(caretLine)
				+ xToSubregionOffset(lineInfos[subregion + 1],magic,
				true);
		}
		else
		{
			int nextLine = foldVisibilityManager.getNextVisibleLine(caretLine);

			if(nextLine == -1)
			{
				int end = getLineEndOffset(caretLine) - 1;
				if(caret == end)
				{
					getToolkit().beep();
					return;
				}
				else
					newCaret = end;
			}
			else
			{
				lineInfos = chunkCache.getLineInfosForPhysicalLine(nextLine);
				newCaret = getLineStartOffset(nextLine)
					+ xToSubregionOffset(lineInfos[0],magic + 1,
					true);
			}
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);

		this.magicCaret = magic;
	} //}}}

	//{{{ goToNextMarker() method
	/**
	 * Moves the caret to the next marker.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextMarker(boolean select)
	{
		Vector markers = buffer.getMarkers();
		if(markers.size() == 0)
		{
			getToolkit().beep();
			return;
		}

		Marker marker = null;

		for(int i = 0; i < markers.size(); i++)
		{
			Marker _marker = (Marker)markers.get(i);
			if(_marker.getPosition() > caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			marker = (Marker)markers.get(0);

		if(select)
			extendSelection(caret,marker.getPosition());
		else if(!multi)
			selectNone();
		moveCaretPosition(marker.getPosition());
	} //}}}

	//{{{ goToNextPage() method
	/**
	 * Moves the caret to the next screenful.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextPage(boolean select)
	{
		int lineCount = getVirtualLineCount();

		int magic = getMagicCaretPosition();

		if(firstLine + visibleLines * 2 >= lineCount - 1)
			setFirstLine(lineCount - visibleLines);
		else
			setFirstLine(firstLine + visibleLines);

		int newLine = virtualToPhysical(Math.min(lineCount - 1,
			physicalToVirtual(caretLine) + visibleLines));
		int newCaret = getLineStartOffset(newLine)
			+ xToOffset(newLine,magic + 1);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);

		setMagicCaretPosition(magic);
	} //}}}

	//{{{ goToNextParagraph() method
	/**
	 * Moves the caret to the start of the next paragraph.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextParagraph(boolean select)
	{
		int lineNo = getCaretLine();

		int newCaret = getBufferLength();

		boolean foundBlank = false;

loop:		for(int i = lineNo + 1; i < getLineCount(); i++)
		{
			if(!foldVisibilityManager.isLineVisible(i))
				continue;

			getLineText(i,lineSegment);

			for(int j = 0; j < lineSegment.count; j++)
			{
				switch(lineSegment.array[lineSegment.offset + j])
				{
				case ' ':
				case '\t':
					break;
				default:
					if(foundBlank)
					{
						newCaret = getLineStartOffset(i);
						break loop;
					}
					else
						continue loop;
				}
			}

			foundBlank = true;
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToNextWord() method
	/**
	 * Moves the caret to the start of the next word.
	 * Note that if the "view.stdNextPrevWord" boolean propery is false,
	 * this method moves the caret to the end of the current word instead.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextWord(boolean select)
	{
		goToNextWord(select,false);
	} //}}}

	//{{{ goToNextWord() method
	/**
	 * Moves the caret to the start of the next word.
	 * Note that if the "view.stdNextPrevWord" boolean propery is false,
	 * this method moves the caret to the end of the current word instead.
	 * @since jEdit 4.1pre5
	 */
	public void goToNextWord(boolean select, boolean stdNextPrevWord)
	{
		int lineStart = getLineStartOffset(caretLine);
		int newCaret = caret - lineStart;
		String lineText = getLineText(caretLine);

		if(newCaret == lineText.length())
		{
			int nextLine = foldVisibilityManager.getNextVisibleLine(caretLine);
			if(nextLine == -1)
			{
				getToolkit().beep();
				return;
			}

			newCaret = getLineStartOffset(nextLine);
		}
		else
		{
			String noWordSep = buffer.getStringProperty("noWordSep");
			newCaret = TextUtilities.findWordEnd(lineText,newCaret + 1,
				noWordSep);
			if(stdNextPrevWord)
			{
				while((newCaret < lineText.length()) && Character.isWhitespace(lineText.charAt(newCaret)))
					newCaret = TextUtilities.findWordEnd(lineText,newCaret + 1,
						noWordSep);
			}

			newCaret += lineStart;
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToPrevBracket() method
	/**
	 * Moves the caret to the previous bracket.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevBracket(boolean select)
	{
		String text = getText(0,caret);

		int newCaret = -1;

loop:		for(int i = getCaretPosition() - 1; i >= 0; i--)
		{
			switch(text.charAt(i))
			{
			case '(': case '[': case '{':
				newCaret = i;
				break loop;
			}
		}

		if(newCaret == -1)
			getToolkit().beep();
		else
		{
			if(select)
				extendSelection(caret,newCaret);
			else if(!multi)
				selectNone();
			moveCaretPosition(newCaret);
		}
	} //}}}

	//{{{ goToPrevCharacter() method
	/**
	 * Moves the caret to the previous character.
	 * @since jEdit 2.7pre2.
	 */
	public void goToPrevCharacter(boolean select)
	{
		if(!select && selection.size() != 0)
		{
			Selection s = getSelectionAtOffset(caret);
			if(s != null)
			{
				if(multi)
				{
					if(caret != s.start)
					{
						moveCaretPosition(s.start);
						return;
					}
				}
				else
				{
					setCaretPosition(s.start);
					return;
				}
			}
		}

		int newCaret;

		if(caret == getLineStartOffset(caretLine))
		{
			int line = foldVisibilityManager.getPrevVisibleLine(caretLine);
			if(line == -1)
			{
				getToolkit().beep();
				return;
			}
			newCaret = getLineEndOffset(line) - 1;
		}
		else
			newCaret = caret - 1;

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToPrevLine() method
	/**
	 * Moves the caret to the previous line.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevLine(boolean select)
	{
		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(caretLine);

		int caretFromStartOfLine = caret - buffer.getLineStartOffset(caretLine);
		int subregion = getSubregionOfOffset(caretFromStartOfLine,
			lineInfos);

		int magic = this.magicCaret;
		if(magic == -1)
		{
			magic = subregionOffsetToX(lineInfos[subregion],
				caretFromStartOfLine);
		}

		int newCaret;

		if(subregion != 0)
		{
			newCaret = buffer.getLineStartOffset(caretLine)
				+ xToSubregionOffset(lineInfos[subregion - 1],magic,
				true);
		}
		else
		{
			int prevLine = foldVisibilityManager.getPrevVisibleLine(caretLine);

			if(prevLine == -1)
			{
				int start = getLineStartOffset(caretLine);
				if(caret == start)
				{
					getToolkit().beep();
					return;
				}
				else
					newCaret = start;
			}
			else
			{
				lineInfos = chunkCache.getLineInfosForPhysicalLine(prevLine);
				newCaret = getLineStartOffset(prevLine)
					+ xToSubregionOffset(lineInfos[
					lineInfos.length - 1],magic + 1,
					true);
			}
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);

		this.magicCaret = magic;
	} //}}}

	//{{{ goToPrevMarker() method
	/**
	 * Moves the caret to the previous marker.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevMarker(boolean select)
	{
		Vector markers = buffer.getMarkers();
		if(markers.size() == 0)
		{
			getToolkit().beep();
			return;
		}

		Marker marker = null;
		for(int i = markers.size() - 1; i >= 0; i--)
		{
			Marker _marker = (Marker)markers.elementAt(i);
			if(_marker.getPosition() < caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			marker = (Marker)markers.get(markers.size() - 1);

		if(select)
			extendSelection(caret,marker.getPosition());
		else if(!multi)
			selectNone();
		moveCaretPosition(marker.getPosition());
	} //}}}

	//{{{ goToPrevPage() method
	/**
	 * Moves the caret to the previous screenful.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevPage(boolean select)
	{
		if(firstLine < visibleLines)
			setFirstLine(0);
		else
			setFirstLine(firstLine - visibleLines);

		int magic = getMagicCaretPosition();

		int newLine = virtualToPhysical(Math.max(0,
			physicalToVirtual(caretLine) - visibleLines));
		int newCaret = getLineStartOffset(newLine)
			+ xToOffset(newLine,magic + 1);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	} //}}}

	//{{{ goToPrevParagraph() method
	/**
	 * Moves the caret to the start of the previous paragraph.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevParagraph(boolean select)
	{
		int lineNo = caretLine;
		int newCaret = 0;

		boolean foundBlank = false;

loop:		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(!foldVisibilityManager.isLineVisible(i))
				continue;

			getLineText(i,lineSegment);

			for(int j = 0; j < lineSegment.count; j++)
			{
				switch(lineSegment.array[lineSegment.offset + j])
				{
				case ' ':
				case '\t':
					break;
				default:
					if(foundBlank)
					{
						newCaret = getLineEndOffset(i) - 1;
						break loop;
					}
					else
						continue loop;
				}
			}

			foundBlank = true;
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToPrevWord() method
	/**
	 * Moves the caret to the start of the previous word.
	 * Note that if the "view.stdNextPrevWord" boolean propery is false,
	 * this method moves the caret to the start of the current word instead.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevWord(boolean select)
	{
		goToPrevWord(select,false);
	} //}}}

	//{{{ goToPrevWord() method
	/**
	 * Moves the caret to the start of the previous word.
	 * Note that if the "view.stdNextPrevWord" boolean propery is false,
	 * this method moves the caret to the start of the current word instead.
	 * @since jEdit 4.1pre5
	 */
	public void goToPrevWord(boolean select, boolean stdNextPrevWord)
	{
		int lineStart = getLineStartOffset(caretLine);
		int newCaret = caret - lineStart;
		String lineText = getLineText(caretLine);

		if(newCaret == 0)
		{
			if(lineStart == 0)
			{
				view.getToolkit().beep();
				return;
			}
			else
			{
				int prevLine = foldVisibilityManager.getPrevVisibleLine(caretLine);
				if(prevLine == -1)
				{
					getToolkit().beep();
					return;
				}

				newCaret = getLineEndOffset(prevLine) - 1;
			}
		}
		else
		{
			String noWordSep = buffer.getStringProperty("noWordSep");
			newCaret = TextUtilities.findWordStart(lineText,newCaret - 1,
				noWordSep);
			if(stdNextPrevWord)
			{
				while((newCaret > 0) && Character.isWhitespace(lineText.charAt(newCaret)))
					newCaret = TextUtilities.findWordStart(lineText,newCaret - 1,
						noWordSep);
			}
			
			newCaret += lineStart;
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ smartHome() method
	/**
	 * On subsequent invocations, first moves the caret to the first
	 * non-whitespace character of the line, then the beginning of the
	 * line, then to the first visible line.
	 * @since jEdit 2.7pre2
	 */
	public void smartHome(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");

			goToStartOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToStartOfLine(" + select + ");");

			goToStartOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToFirstVisibleLine(" + select + ");");

			goToFirstVisibleLine(select);
			break;
		}
	} //}}}

	//{{{ smartEnd() method
	/**
	 * On subsequent invocations, first moves the caret to the last
	 * non-whitespace character of the line, then the end of the
	 * line, then to the last visible line.
	 * @since jEdit 2.7pre2
	 */
	public void smartEnd(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");

			goToEndOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToEndOfLine(" + select + ");");

			goToEndOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToLastVisibleLine(" + select + ");");
			goToLastVisibleLine(select);
			break;
		}
	} //}}}

	//{{{ goToStartOfLine() method
	/**
	 * Moves the caret to the beginning of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line = (select || s == null
			? caretLine : s.startLine);
		int newCaret = getLineStartOffset(line);
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToEndOfLine() method
	/**
	 * Moves the caret to the end of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line = (select || s == null
			? caretLine : s.endLine);
		int newCaret = getLineEndOffset(line) - 1;
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);

		// so that end followed by up arrow will always put caret at
		// the end of the previous line, for example
		//setMagicCaretPosition(Integer.MAX_VALUE);
	} //}}}

	//{{{ goToStartOfWhiteSpace() method
	/**
	 * Moves the caret to the first non-whitespace character of the current
	 * line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfWhiteSpace(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line, offset;
		if(select || s == null)
		{
			line = caretLine;
			offset = caret - buffer.getLineStartOffset(line);
		}
		else
		{
			line = s.startLine;
			offset = s.start - buffer.getLineStartOffset(line);
		}

		int firstIndent;

		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(line);
		int subregion = getSubregionOfOffset(offset,lineInfos);
		if(subregion == 0)
		{
			firstIndent = MiscUtilities.getLeadingWhiteSpace(getLineText(line));
			if(firstIndent == getLineLength(line))
				firstIndent = 0;
			firstIndent += getLineStartOffset(line);
		}
		else
		{
			firstIndent = getLineStartOffset(line)
				+ lineInfos[subregion].offset;
		}

		if(select)
			extendSelection(caret,firstIndent);
		else if(!multi)
			selectNone();
		moveCaretPosition(firstIndent);
	} //}}}

	//{{{ goToEndOfWhiteSpace() method
	/**
	 * Moves the caret to the last non-whitespace character of the current
	 * line.
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfWhiteSpace(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line, offset;
		if(select || s == null)
		{
			line = caretLine;
			offset = caret - getLineStartOffset(line);
		}
		else
		{
			line = s.endLine;
			offset = s.end - getLineStartOffset(line);
		}

		int lastIndent;

		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(line);
		int subregion = getSubregionOfOffset(offset,lineInfos);
		if(subregion == lineInfos.length - 1)
		{
			lastIndent = getLineLength(line) - MiscUtilities.getTrailingWhiteSpace(getLineText(line));
			if(lastIndent == 0)
				lastIndent = getLineLength(line);
			lastIndent += getLineStartOffset(line);
		}
		else
		{
			lastIndent = getLineStartOffset(line)
				+ lineInfos[subregion].offset
				+ lineInfos[subregion].length - 1;
		}

		if(select)
			extendSelection(caret,lastIndent);
		else if(!multi)
			selectNone();
		moveCaretPosition(lastIndent);
	} //}}}

	//{{{ goToFirstVisibleLine() method
	/**
	 * Moves the caret to the first visible line.
	 * @since jEdit 2.7pre2
	 */
	public void goToFirstVisibleLine(boolean select)
	{
		int firstVisibleLine = firstLine == 0 ? 0 : electricScroll;
		int firstVisible = getScreenLineStartOffset(firstVisibleLine);
		if(firstVisible == -1)
		{
			firstVisible = getLineStartOffset(foldVisibilityManager
				.getFirstVisibleLine());
		}

		if(select)
			extendSelection(caret,firstVisible);
		else if(!multi)
			selectNone();
		moveCaretPosition(firstVisible);
	} //}}}

	//{{{ goToLastVisibleLine() method
	/**
	 * Moves the caret to the last visible line.
	 * @since jEdit 2.7pre2
	 */
	public void goToLastVisibleLine(boolean select)
	{
		int lastVisible;

		if(firstLine + visibleLines >= getVirtualLineCount())
		{
			lastVisible = getLineEndOffset(foldVisibilityManager
				.getLastVisibleLine()) - 1;
		}
		else
		{
			lastVisible = getScreenLineEndOffset(visibleLines
				- electricScroll - 1) - 1;
			if(lastVisible == -1)
			{
				lastVisible = getLineEndOffset(foldVisibilityManager
					.getLastVisibleLine()) - 1;
			}
		}

		if(select)
			extendSelection(caret,lastVisible);
		else if(!multi)
			selectNone();
		moveCaretPosition(lastVisible);
	} //}}}

	//{{{ goToBufferStart() method
	/**
	 * Moves the caret to the beginning of the buffer.
	 * @since jEdit 4.0pre3
	 */
	public void goToBufferStart(boolean select)
	{
		int start = buffer.getLineStartOffset(
			foldVisibilityManager.getFirstVisibleLine());
		if(select)
			extendSelection(caret,start);
		else if(!multi)
			selectNone();
		moveCaretPosition(start);
	} //}}}

	//{{{ goToBufferEnd() method
	/**
	 * Moves the caret to the end of the buffer.
	 * @since jEdit 4.0pre3
	 */
	public void goToBufferEnd(boolean select)
	{
		int end = buffer.getLineEndOffset(
			foldVisibilityManager.getLastVisibleLine()) - 1;
		if(select)
			extendSelection(caret,end);
		else if(!multi)
			selectNone();
		moveCaretPosition(end);
	} //}}}

	//{{{ goToMatchingBracket() method
	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * @since jEdit 2.7pre3
	 */
	public void goToMatchingBracket()
	{
		if(getLineLength(caretLine) != 0)
		{
			int dot = caret - getLineStartOffset(caretLine);

			int bracket = TextUtilities.findMatchingBracketFuzzy(
				buffer,caretLine,Math.max(0,dot - 1));
			if(bracket != -1)
			{
				selectNone();
				moveCaretPosition(bracket + 1,false);
				return;
			}
		}

		getToolkit().beep();
	} //}}}

	//{{{ showGoToLineDialog() method
	/**
	 * Displays the 'go to line' dialog box, and moves the caret to the
	 * specified line number.
	 * @since jEdit 2.7pre2
	 */
	public void showGoToLineDialog()
	{
		String line = GUIUtilities.input(view,"goto-line",null);
		if(line == null)
			return;

		try
		{
			int lineNumber = Integer.parseInt(line) - 1;
			setCaretPosition(getLineStartOffset(lineNumber));
		}
		catch(Exception e)
		{
			getToolkit().beep();
		}
	} //}}}

	//}}}

	//{{{ User input

	//{{{ userInput() method
	/**
	 * Handles the insertion of the specified character. Performs
	 * auto indent, expands abbreviations, does word wrap, etc.
	 * @param ch The character
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 * @since jEdit 2.7pre3
	 */
	public void userInput(char ch)
	{
		if(!isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(ch == ' ' && Abbrevs.getExpandOnInput()
			&& Abbrevs.expandAbbrev(view,false))
			return;
		else if(ch == '\t')
		{
			if(selection.size() == 1)
			{
				Selection sel = (Selection)selection.elementAt(0);
				if(sel.startLine == sel.endLine
					&& (sel.start != buffer.getLineStartOffset(sel.startLine)
					|| sel.end != buffer.getLineEndOffset(sel.startLine) - 1))
				{
					insertTab();
				}
				else
					shiftIndentRight();
			}
			else if(selection.size() != 0)
				shiftIndentRight();
			else
				insertTab();
			return;
		}
		else
		{
			boolean indent;

			// check if the user entered a bracket
			String indentOpenBrackets = (String)buffer
				.getProperty("indentOpenBrackets");
			String indentCloseBrackets = (String)buffer
				.getProperty("indentCloseBrackets");
			if((indentCloseBrackets != null
				&& indentCloseBrackets.indexOf(ch) != -1)
				|| (indentOpenBrackets != null
				&& indentOpenBrackets.indexOf(ch) != -1))
			{
				indent = true;
			}
			else
			{
				indent = false;
			}

			String str = String.valueOf(ch);
			if(selection.size() != 0)
			{
				setSelectedText(str);
				return;
			}

			if(ch == ' ')
			{
				if(doWordWrap(true))
					return;
			}
			else
				doWordWrap(false);

			try
			{
				// Don't overstrike if we're on the end of
				// the line
				if(overwrite || indent)
					buffer.beginCompoundEdit();

				if(overwrite)
				{
					int caretLineEnd = getLineEndOffset(caretLine);
					if(caretLineEnd - caret > 1)
						buffer.remove(caret,1);
				}

				buffer.insert(caret,str);

				if(indent)
					buffer.indentLine(caretLine,false,true);
			}
			finally
			{
				if(overwrite || indent)
					buffer.endCompoundEdit();
			}

		}
	} //}}}

	//{{{ isOverwriteEnabled() method
	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public final boolean isOverwriteEnabled()
	{
		return overwrite;
	} //}}}

	//{{{ setOverwriteEnabled() method
	/**
	 * Sets overwrite mode.
	 */
	public final void setOverwriteEnabled(boolean overwrite)
	{
		blink = true;
		caretTimer.restart();

		this.overwrite = overwrite;
		invalidateLine(caretLine);
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	} //}}}

	//{{{ toggleOverwriteEnabled() method
	/**
	 * Toggles overwrite mode.
	 * @since jEdit 2.7pre2
	 */
	public final void toggleOverwriteEnabled()
	{
		setOverwriteEnabled(!overwrite);
		if(view.getStatus() != null)
		{
			view.getStatus().setMessageAndClear(
				jEdit.getProperty("view.status.overwrite-changed",
				new Integer[] { new Integer(overwrite ? 1 : 0) }));
		}
	} //}}}

	//{{{ backspace() method
	/**
	 * Deletes the character before the caret, or the selection, if one is
	 * active.
	 * @since jEdit 2.7pre2
	 */
	public void backspace()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selection.size() != 0)
			setSelectedText(null);
		else
		{
			if(caret == 0)
			{
				getToolkit().beep();
				return;
			}

			buffer.remove(caret - 1,1);
		}
	} //}}}

	//{{{ backspaceWord() method
	/**
	 * Deletes the word before the caret.
	 * @since jEdit 2.7pre2
	 */
	public void backspaceWord()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selection.size() != 0)
		{
			setSelectedText("");
			return;
		}

		int lineStart = getLineStartOffset(caretLine);
		int _caret = caret - lineStart;

		String lineText = getLineText(caretLine);

		if(_caret == 0)
		{
			if(lineStart == 0)
			{
				getToolkit().beep();
				return;
			}
			_caret--;
		}
		else
		{
			String noWordSep = buffer.getStringProperty("noWordSep");
			_caret = TextUtilities.findWordStart(lineText,_caret-1,noWordSep);
		}

		buffer.remove(_caret + lineStart,
			caret - (_caret + lineStart));
	} //}}}

	//{{{ delete() method
	/**
	 * Deletes the character after the caret.
	 * @since jEdit 2.7pre2
	 */
	public void delete()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selection.size() != 0)
			setSelectedText(null);
		else
		{
			if(caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}

			buffer.remove(caret,1);
		}
	} //}}}

	//{{{ deleteToEndOfLine() method
	/**
	 * Deletes from the caret to the end of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void deleteToEndOfLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		buffer.remove(caret,getLineEndOffset(caretLine)
			- caret - 1);
	} //}}}

	//{{{ deleteLine() method
	/**
	 * Deletes the line containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		int start = getLineStartOffset(caretLine);
		int end = getLineEndOffset(caretLine);
		if(end > buffer.getLength())
		{
			if(start != 0)
				start--;
			end--;
		}
		int x = offsetToX(caretLine,caret - start);

		// otherwise a bunch of consecutive C+d's would be merged
		// into one edit
		try
		{
			buffer.beginCompoundEdit();
			buffer.remove(start,end - start);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		int lastLine = buffer.getLineCount() - 1;
		if(caretLine == lastLine)
		{
			setCaretPosition(buffer.getLineStartOffset(lastLine)
				+ xToOffset(caretLine,x));
		}
		else
		{
			setCaretPosition(start + xToOffset(caretLine,x));
		}
	} //}}}

	//{{{ deleteParagraph() method
	/**
	 * Deletes the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteParagraph()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		int start = 0, end = buffer.getLength();

loop:		for(int i = caretLine - 1; i >= 0; i--)
		{
			//if(!foldVisibilityManager.isLineVisible(i))
			//	continue loop;

			getLineText(i,lineSegment);

			for(int j = 0; j < lineSegment.count; j++)
			{
				switch(lineSegment.array[lineSegment.offset + j])
				{
				case ' ':
				case '\t':
					break;
				default:
					continue loop;
				}
			}

			start = getLineStartOffset(i);
			break loop;
		}

loop:		for(int i = caretLine + 1; i < getLineCount(); i++)
		{
			//if(!foldVisibilityManager.isLineVisible(i))
			//	continue loop;

			getLineText(i,lineSegment);

			for(int j = 0; j < lineSegment.count; j++)
			{
				switch(lineSegment.array[lineSegment.offset + j])
				{
				case ' ':
				case '\t':
					break;
				default:
					continue loop;
				}
			}

			end = getLineEndOffset(i) - 1;
			break loop;
		}

		buffer.remove(start,end - start);
	} //}}}

	//{{{ deleteToStartOfLine() method
	/**
	 * Deletes from the caret to the beginning of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void deleteToStartOfLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		buffer.remove(getLineStartOffset(caretLine),
			caret - getLineStartOffset(caretLine));
	} //}}}

	//{{{ deleteWord() method
	/**
	 * Deletes the word in front of the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteWord()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selection.size() != 0)
		{
			setSelectedText("");
			return;
		}

		int lineStart = getLineStartOffset(caretLine);
		int _caret = caret - lineStart;

		String lineText = getLineText(caretLine);

		if(_caret == lineText.length())
		{
			if(lineStart + _caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}
			_caret++;
		}
		else
		{
			String noWordSep = buffer.getStringProperty("noWordSep");
			_caret = TextUtilities.findWordEnd(lineText,
				_caret+1,noWordSep);
		}

		buffer.remove(caret,(_caret + lineStart) - caret);
	} //}}}

	//{{{ isMultipleSelectionEnabled() method
	/**
	 * Returns if multiple selection is enabled.
	 * @since jEdit 3.2pre1
	 */
	public final boolean isMultipleSelectionEnabled()
	{
		return multi;
	} //}}}

	//{{{ toggleMultipleSelectionEnabled() method
	/**
	 * Toggles multiple selection.
	 * @since jEdit 3.2pre1
	 */
	public final void toggleMultipleSelectionEnabled()
	{
		setMultipleSelectionEnabled(!multi);
		if(view.getStatus() != null)
		{
			view.getStatus().setMessageAndClear(
				jEdit.getProperty("view.status.multi-changed",
				new Integer[] { new Integer(multi ? 1 : 0) }));
		}
	} //}}}

	//{{{ setMultipleSelectionEnabled() method
	/**
	 * Set multiple selection on or off according to the value of
	 * <code>multi</code>. This only affects the ability to
	 * make multiple selections in the user interface; macros and plugins
	 * can manipulate them regardless of the setting of this flag. In fact,
	 * in most cases, calling this method should not be necessary.
	 *
	 * @param multi Should multiple selection be enabled?
	 * @since jEdit 3.2pre1
	 */
	public final void setMultipleSelectionEnabled(boolean multi)
	{
		this.multi = multi;
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	} //}}}

	//}}}

	//{{{ Markers

	//{{{ goToMarker() method
	/**
	 * Moves the caret to the marker with the specified shortcut.
	 * @param shortcut The shortcut
	 * @param select True if the selection should be extended,
	 * false otherwise
	 * @since jEdit 3.2pre2
	 */
	public void goToMarker(char shortcut, boolean select)
	{
		Marker marker = buffer.getMarker(shortcut);
		if(marker == null)
		{
			getToolkit().beep();
			return;
		}

		int pos = marker.getPosition();

		if(select)
			extendSelection(caret,pos);
		else if(!multi)
			selectNone();
		moveCaretPosition(pos);
	} //}}}

	//{{{ addMarker() method
	/**
	 * Adds a marker at the caret position.
	 * @since jEdit 3.2pre1
	 */
	public void addMarker()
	{
		// always add markers on selected lines
		Selection[] selection = getSelection();
		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			if(s.startLine != s.endLine)
			{
				if(s.startLine != caretLine)
					buffer.addMarker('\0',s.start);
			}

			if(s.endLine != caretLine)
				buffer.addMarker('\0',s.end);
		}

		// toggle marker on caret line
		buffer.addOrRemoveMarker('\0',caret);
	} //}}}

	//{{{ swapMarkerAndCaret() method
	/**
	 * Moves the caret to the marker with the specified shortcut,
	 * then sets the marker position to the former caret position.
	 * @param shortcut The shortcut
	 * @since jEdit 3.2pre2
	 */
	public void swapMarkerAndCaret(char shortcut)
	{
		Marker marker = buffer.getMarker(shortcut);
		if(marker == null)
		{
			getToolkit().beep();
			return;
		}

		int caret = getCaretPosition();

		setCaretPosition(marker.getPosition());
		buffer.addMarker(shortcut,caret);
	} //}}}

	//}}}

	//{{{ Folding

	//{{{ goToParentFold() method
	/**
	 * Moves the caret to the fold containing the one at the caret
	 * position.
	 * @since jEdit 4.0pre3
	 */
	public void goToParentFold()
	{
		int line = -1;
		int level = buffer.getFoldLevel(caretLine);
		for(int i = caretLine - 1; i >= 0; i--)
		{
			if(buffer.getFoldLevel(i) < level)
			{
				line = i;
				break;
			}
		}

		if(line == -1)
		{
			getToolkit().beep();
			return;
		}

		int magic = getMagicCaretPosition();

		int newCaret = buffer.getLineStartOffset(line)
			+ xToOffset(line,magic + 1);
		if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	} //}}}

	//{{{ goToNextFold() method
	/**
	 * Moves the caret to the next fold.
	 * @since jEdit 4.0pre3
	 */
	public void goToNextFold(boolean select)
	{
		int nextFold = -1;
		for(int i = caretLine + 1; i < buffer.getLineCount(); i++)
		{
			if(buffer.isFoldStart(i)
				&& foldVisibilityManager.isLineVisible(i))
			{
				nextFold = i;
				break;
			}
		}

		if(nextFold == -1)
		{
			getToolkit().beep();
			return;
		}

		int magic = getMagicCaretPosition();

		int newCaret = buffer.getLineStartOffset(nextFold)
			+ xToOffset(nextFold,magic + 1);
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	} //}}}

	//{{{ goToPrevFold() method
	/**
	 * Moves the caret to the previous fold.
	 * @since jEdit 4.0pre3
	 */
	public void goToPrevFold(boolean select)
	{
		int prevFold = -1;
		for(int i = caretLine - 1; i >= 0; i--)
		{
			if(buffer.isFoldStart(i)
				&& foldVisibilityManager.isLineVisible(i))
			{
				prevFold = i;
				break;
			}
		}

		if(prevFold == -1)
		{
			getToolkit().beep();
			return;
		}

		int magic = getMagicCaretPosition();

		int newCaret = buffer.getLineStartOffset(prevFold)
			+ xToOffset(prevFold,magic + 1);
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	} //}}}

	//{{{ collapseFold() method
	/**
	 * Like {@link FoldVisibilityManager#collapseFold(int)}, but
	 * also moves the caret to the first line of the fold.
	 * @since jEdit 4.0pre3
	 */
	public void collapseFold()
	{
		int x = offsetToX(caretLine,caret - getLineStartOffset(caretLine));

		foldVisibilityManager.collapseFold(caretLine);

		if(foldVisibilityManager.isLineVisible(caretLine))
			return;

		int line = foldVisibilityManager.getPrevVisibleLine(caretLine);

		if(!multi)
			selectNone();
		moveCaretPosition(buffer.getLineStartOffset(line) + xToOffset(line,x));
	} //}}}

	//{{{ expandFold() method
	/**
	 * Like {@link FoldVisibilityManager#expandFold(int,boolean)}, but
	 * also moves the caret to the first sub-fold.
	 * @since jEdit 4.0pre3
	 */
	public void expandFold(boolean fully)
	{
		int x = offsetToX(caretLine,caret - getLineStartOffset(caretLine));

		int line = foldVisibilityManager.expandFold(caretLine,fully);

		if(!fully && line != -1)
		{
			if(!multi)
				selectNone();
			moveCaretPosition(getLineStartOffset(line) + xToOffset(line,x));
		}
	} //}}}

	//{{{ selectFold() method
	/**
	 * Selects the fold that contains the caret line number.
	 * @since jEdit 3.1pre3
	 */
	public void selectFold()
	{
		selectFold(caretLine);
	} //}}}

	//{{{ selectFold() method
	/**
	 * Selects the fold that contains the specified line number.
	 * @param line The line number
	 * @since jEdit 4.0pre1
	 */
	public void selectFold(int line)
	{
		int[] lines = buffer.getFoldAtLine(line);

		int newCaret = getLineEndOffset(lines[1]) - 1;
		Selection s = new Selection.Range(getLineStartOffset(lines[0]),newCaret);
		if(multi)
			addToSelection(s);
		else
			setSelection(s);
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ narrowToFold() method
	/**
	 * Hides all lines except those in the fold containing the caret.
	 * @since jEdit 4.0pre1
	 */
	public void narrowToFold()
	{
		int[] lines = buffer.getFoldAtLine(caretLine);
		if(lines[0] == 0 && lines[1] == buffer.getLineCount() - 1)
			getToolkit().beep();
		else
			foldVisibilityManager.narrow(lines[0],lines[1]);
	} //}}}

	//{{{ narrowToSelection() method
	/**
	 * Hides all lines except those in the selection.
	 * @since jEdit 4.0pre1
	 */
	public void narrowToSelection()
	{
		if(selection.size() != 1)
		{
			getToolkit().beep();
			return;
		}

		Selection sel = (Selection)selection.elementAt(0);
		foldVisibilityManager.narrow(sel.getStartLine(),sel.getEndLine());

		selectNone();
	} //}}}

	//{{{ addExplicitFold() method
	/**
	 * Surrounds the selection with explicit fold markers.
	 * @since jEdit 4.0pre3
	 */
	public void addExplicitFold()
	{
		if(!buffer.getStringProperty("folding").equals("explicit"))
		{
			GUIUtilities.error(view,"folding-not-explicit",null);
			return;
		}

		// BUG: if there are multiple selections in different
		// contexts, the wrong comment strings will be inserted.
		String lineComment = buffer.getContextSensitiveProperty(caret,"lineComment");
		String commentStart = buffer.getContextSensitiveProperty(caret,"commentStart");
		String commentEnd = buffer.getContextSensitiveProperty(caret,"commentEnd");

		String start, end;
		if(lineComment != null)
		{
			start = lineComment + "{{{ \n";
			end = lineComment + "}}}";
		}
		else if(commentStart != null && commentEnd != null)
		{
			start = commentStart + "{{{  " + commentEnd + "\n";
			end = commentStart + "}}}" + commentEnd;
		}
		else
		{
			start = "{{{ \n";
			end = "}}}";
		}

		try
		{
			buffer.beginCompoundEdit();

			if(selection.size() == 0)
			{
				String line = buffer.getLineText(caretLine);
				String whitespace = line.substring(0,
					MiscUtilities.getLeadingWhiteSpace(line));
				int loc = caret + start.length() - 1;
				start = start + whitespace;
				buffer.insert(caret,start);
				// stupid: caret will automatically be incremented
				buffer.insert(caret,end);
				moveCaretPosition(loc,false);
			}
			else
			{
				int loc = -1;

				for(int i = 0; i < selection.size(); i++)
				{
					Selection s = (Selection)selection.elementAt(i);
					String line = buffer.getLineText(s.startLine);
					String whitespace = line.substring(0,
						MiscUtilities.getLeadingWhiteSpace(line));
					loc = s.start + start.length() - 1;
					buffer.insert(s.start,start + whitespace);
					buffer.insert(s.end," " + end);
				}

				setCaretPosition(loc,false);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}

	//}}}

	//{{{ Text editing

	//{{{ lineComment() method
	/**
	 * Prepends each line of the selection with the line comment string.
	 * @since jEdit 3.2pre1
	 */
	public void lineComment()
	{
		String comment = buffer.getContextSensitiveProperty(caret,"lineComment");
		if(!buffer.isEditable() || comment == null || comment.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		comment = comment + ' ';

		buffer.beginCompoundEdit();

		int[] lines = getSelectedLines();

		try
		{
			for(int i = 0; i < lines.length; i++)
			{
				String text = getLineText(lines[i]);
				buffer.insert(getLineStartOffset(lines[i])
					+ MiscUtilities.getLeadingWhiteSpace(text),
					comment);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		selectNone();
	} //}}}

	//{{{ rangeComment() method
	/**
	 * Adds comment start and end strings to the beginning and end of the
	 * selection.
	 * @since jEdit 3.2pre1
	 */
	public void rangeComment()
	{
		String commentStart = buffer.getContextSensitiveProperty(caret,"commentStart");
		String commentEnd = buffer.getContextSensitiveProperty(caret,"commentEnd");
		if(!buffer.isEditable() || commentStart == null || commentEnd == null
			|| commentStart.length() == 0 || commentEnd.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		commentStart = commentStart + ' ';
		commentEnd = ' ' + commentEnd;

		try
		{
			buffer.beginCompoundEdit();

			Selection[] selection = getSelection();

			if(selection.length == 0)
			{
				int oldCaret = caret;
				buffer.insert(caret,commentStart);
				buffer.insert(caret,commentEnd);
				setCaretPosition(oldCaret + commentStart.length());
			}

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				if(s instanceof Selection.Range)
				{
					buffer.insert(s.start,commentStart);
					buffer.insert(s.end,commentEnd);
				}
				else if(s instanceof Selection.Rect)
				{
					Selection.Rect rect = (Selection.Rect)s;
					int start = rect.getStartColumn(buffer);
					int end = rect.getEndColumn(buffer);

					for(int j = s.startLine; j <= s.endLine; j++)
					{
						buffer.insertAtColumn(j,end,
							commentEnd);
						buffer.insertAtColumn(j,start,
							commentStart);
					}
				}
			}

			selectNone();
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}

	//{{{ formatParagraph() method
	/**
	 * Formats the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void formatParagraph()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(maxLineLen <= 0)
		{
			getToolkit().beep();
			return;
		}

		Selection[] selection = getSelection();
		if(selection.length != 0)
		{
			buffer.beginCompoundEdit();

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				setSelectedText(s,TextUtilities.format(
					getSelectedText(s),maxLineLen,
					buffer.getTabSize()));
			}

			buffer.endCompoundEdit();
		}
		else
		{
			int lineNo = getCaretLine();

			int start = 0, end = buffer.getLength();

loop:			for(int i = lineNo - 1; i >= 0; i--)
			{
				getLineText(i,lineSegment);

				for(int j = 0; j < lineSegment.count; j++)
				{
					switch(lineSegment.array[lineSegment.offset + j])
					{
					case ' ':
					case '\t':
						break;
					default:
						continue loop;
					}
				}

				start = getLineEndOffset(i);
				break loop;
			}

loop:			for(int i = lineNo + 1; i < getLineCount(); i++)
			{
				getLineText(i,lineSegment);

				for(int j = 0; j < lineSegment.count; j++)
				{
					switch(lineSegment.array[lineSegment.offset + j])
					{
					case ' ':
					case '\t':
						break;
					default:
						continue loop;
					}
				}

				end = getLineStartOffset(i) - 1;
				break loop;
			}

			try
			{
				buffer.beginCompoundEdit();

				String text = buffer.getText(start,end - start);
				buffer.remove(start,end - start);
				buffer.insert(start,TextUtilities.format(
					text,maxLineLen,buffer.getTabSize()));
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	} //}}}

	//{{{ spacesToTabs() method
	/**
	 * Converts spaces to tabs in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void spacesToTabs()
	{
		Selection[] selection = getSelection();

		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		buffer.beginCompoundEdit();

		if(selection.length == 0)
		{
			setText(TextUtilities.spacesToTabs(
				getText(), buffer.getTabSize()));
		}
		else
		{
			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				setSelectedText(s,TextUtilities.spacesToTabs(
					getSelectedText(s),buffer.getTabSize()));
			}
		}

		buffer.endCompoundEdit();
	} //}}}

	//{{{ tabsToSpaces() method
	/**
	 * Converts tabs to spaces in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void tabsToSpaces()
	{
		Selection[] selection = getSelection();

		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		buffer.beginCompoundEdit();

		if(selection.length == 0)
		{
			setText(TextUtilities.tabsToSpaces(
				getText(), buffer.getTabSize()));
		}
		else
		{
			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				setSelectedText(s,TextUtilities.tabsToSpaces(
					getSelectedText(s),buffer.getTabSize()));
			}
		}

		buffer.endCompoundEdit();
	} //}}}

	//{{{ toUpperCase() method
	/**
	 * Converts the selected text to upper case.
	 * @since jEdit 2.7pre2
	 */
	public void toUpperCase()
	{
		Selection[] selection = getSelection();

		if(!buffer.isEditable() || selection.length == 0)
                {
                	getToolkit().beep();
                	return;
                }

		buffer.beginCompoundEdit();

		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			setSelectedText(s,getSelectedText(s).toUpperCase());
		}

		buffer.endCompoundEdit();
	} //}}}

	//{{{ toLowerCase() method
	/**
	 * Converts the selected text to lower case.
	 * @since jEdit 2.7pre2
	 */
	public void toLowerCase()
	{
		Selection[] selection = getSelection();

		if(!buffer.isEditable() || selection.length == 0)
                {
                	getToolkit().beep();
                	return;
                }

		buffer.beginCompoundEdit();

		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			setSelectedText(s,getSelectedText(s).toLowerCase());
		}

		buffer.endCompoundEdit();
	} //}}}

	//{{{ removeTrailingWhiteSpace() method
	/**
	 * Removes trailing whitespace from all lines in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void removeTrailingWhiteSpace()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.removeTrailingWhiteSpace(getSelectedLines());
		}
	} //}}}

	//{{{ insertEnterAndIndent() method
	public void insertEnterAndIndent()
	{
		if(!isEditable())
			getToolkit().beep();
		else
		{
			try
			{
				buffer.beginCompoundEdit();
				setSelectedText("\n");
				buffer.indentLine(caretLine,true,false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	} //}}}

	//{{{ insertTabAndIndent() method
	public void insertTabAndIndent()
	{
		if(!isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selection.size() == 0)
		{
			// if caret is inside leading whitespace, indent.
			String text = buffer.getLineText(caretLine);
			int start = buffer.getLineStartOffset(caretLine);
			int whiteSpace = MiscUtilities.getLeadingWhiteSpace(text);

			if(caret - start <= whiteSpace
				&& buffer.indentLine(caretLine,true,false))
				return;
		}

		userInput('\t');
	} //}}}

	//{{{ indentSelectedLines() method
	/**
	 * Indents all selected lines.
	 * @since jEdit 3.1pre3
	 */
	public void indentSelectedLines()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.indentLines(getSelectedLines());
			selectNone();
		}
	} //}}}

	//{{{ shiftIndentLeft() method
	/**
	 * Shifts the indent to the left.
	 * @since jEdit 2.7pre2
	 */
	public void shiftIndentLeft()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.shiftIndentLeft(getSelectedLines());
		}
	} //}}}

	//{{{ shiftIndentRight() method
	/**
	 * Shifts the indent to the right.
	 * @since jEdit 2.7pre2
	 */
	public void shiftIndentRight()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
			buffer.shiftIndentRight(getSelectedLines());
	} //}}}

	//{{{ joinLines() method
	/**
	 * Joins the current and the next line.
	 * @since jEdit 2.7pre2
	 */
	public void joinLines()
	{
		int end = getLineEndOffset(caretLine);
		if(end > buffer.getLength())
		{
			getToolkit().beep();
			return;
		}
		buffer.remove(end - 1,MiscUtilities.getLeadingWhiteSpace(
			buffer.getLineText(caretLine + 1)) + 1);

		setCaretPosition(end - 1);
	} //}}}

	//{{{ showWordCountDialog() method
	/**
	 * Displays the 'word count' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showWordCountDialog()
	{
		String selection = getSelectedText();
		if(selection != null)
		{
			doWordCount(view,selection);
			return;
		}

		doWordCount(view,buffer.getText(0,buffer.getLength()));
	} //}}}

	//}}}

	//{{{ AWT stuff

	//{{{ addNotify() method
	/**
	 * Called by the AWT when this component is added to a parent.
	 * Adds document listener.
	 */
	public void addNotify()
	{
		super.addNotify();

		ToolTipManager.sharedInstance().registerComponent(painter);
		ToolTipManager.sharedInstance().registerComponent(gutter);

		if(!bufferHandlerInstalled)
		{
			bufferHandlerInstalled = true;
			buffer.addBufferChangeListener(bufferHandler);
		}

		recalculateVisibleLines();
		recalculateLastPhysicalLine();
	} //}}}

	//{{{ removeNotify() method
	/**
	 * Called by the AWT when this component is removed from it's parent.
	 * This clears the pointer to the currently focused component.
	 * Also removes document listener.
	 */
	public void removeNotify()
	{
		super.removeNotify();

		ToolTipManager.sharedInstance().unregisterComponent(painter);
		ToolTipManager.sharedInstance().unregisterComponent(gutter);

		if(focusedComponent == this)
			focusedComponent = null;

		if(bufferHandlerInstalled)
		{
			buffer.removeBufferChangeListener(bufferHandler);
			bufferHandlerInstalled = false;
		}
	} //}}}

	//{{{ hasFocus() method
	/**
	 * Bug workarounds.
	 * @since jEdit 2.7pre1
	 */
	public boolean hasFocus()
	{
		Component c = this;
		while(!(c instanceof Window))
		{
			if(c == null)
				return false;
			c = c.getParent();
		}

		Component focusOwner = ((Window)c).getFocusOwner();
		boolean hasFocus = (focusOwner == this);
		if(hasFocus)
			focusedComponent = this;
		return hasFocus;
	} //}}}

	//{{{ grabFocus() method
	/**
	 * Bug workarounds.
	 * @since jEdit 2.7pre1
	 */
	public void grabFocus()
	{
		super.grabFocus();
		// ensure that focusedComponent is set correctly
		hasFocus();
	} //}}}

	//{{{ getFocusTraversalKeysEnabled() method
	/**
	 * Java 1.4 compatibility fix to make Tab key work.
	 * @since jEdit 3.2pre4
	 */
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	} //}}}

	//{{{ processKeyEvent() method
	// debug code to measure key delay
	long time;
	boolean timing;
	public void processKeyEvent(KeyEvent evt)
	{
		time = System.currentTimeMillis();

		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		// Ignore
		if(view.isClosed())
			return;

		InputHandler inputHandler = view.getInputHandler();
		KeyListener keyEventInterceptor = view.getKeyEventInterceptor();
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			//timing = true;
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else
				inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyPressed(evt);
			else
				inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyReleased(evt);
			else
				inputHandler.keyReleased(evt);
			break;
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	} //}}}

	//}}}

	//{{{ propertiesChanged() method
	/**
	 * Called by jEdit when necessary. Plugins should not call this method.
	 */
	public void propertiesChanged()
	{
		if(buffer == null)
			return;

		int _tabSize = buffer.getTabSize();
		char[] foo = new char[_tabSize];
		for(int i = 0; i < foo.length; i++)
		{
			foo[i] = ' ';
		}

		tabSize = (float)painter.getFont().getStringBounds(foo,0,_tabSize,
			painter.getFontRenderContext()).getWidth();

		charWidth = (int)Math.round(painter.getFont().getStringBounds(foo,0,1,
			painter.getFontRenderContext()).getWidth());

		String wrap = buffer.getStringProperty("wrap");
		softWrap = wrap.equals("soft");
		hardWrap = wrap.equals("hard");

		maxLineLen = buffer.getIntegerProperty("maxLineLen",0);

		if(maxLineLen <= 0)
		{
			if(softWrap)
			{
				wrapToWidth = true;
				wrapMargin = painter.getWidth() - charWidth * 3;
			}
			else
			{
				wrapToWidth = false;
				wrapMargin = 0;
			}
		}
		else
		{
			// stupidity
			foo = new char[maxLineLen];
			for(int i = 0; i < foo.length; i++)
			{
				foo[i] = ' ';
			}
			wrapToWidth = false;
			wrapMargin = (int)painter.getFont().getStringBounds(
				foo,0,maxLineLen,painter.getFontRenderContext())
				.getWidth();
		}

		monospacedHack = jEdit.getBooleanProperty("view.monospacedHack");

		maxHorizontalScrollWidth = 0;
		updateScrollBars();

		chunkCache.invalidateAll();
		gutter.repaint();
		painter.repaint();
	} //}}}

	//{{{ Deprecated methods

	//{{{ offsetToX() method
	/**
	 * @deprecated Call <code>offsetToXY()</code> instead.
	 *
	 * Converts an offset in a line into an x co-ordinate.
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public int offsetToX(int line, int offset)
	{
		Chunk chunks = chunkCache.lineToChunkList(line);
		return (int)(horizontalOffset + Chunk.offsetToX(chunks,offset));
	} //}}}

	//{{{ xToOffset() method
	/**
	 * @deprecated Call <code>xyToOffset()</code> instead.
	 *
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The physical line index
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		x -= horizontalOffset;
		Chunk chunks = chunkCache.lineToChunkList(line);
		int offset = Chunk.xToOffset(chunks,x,true);
		if(offset == -1)
			offset = getLineLength(line);
		return offset;
	} //}}}

	//{{{ xToOffset() method
	/**
	 * @deprecated Call <code>xyToOffset()</code> instead.
	 *
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The physical line index
	 * @param x The x co-ordinate
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 3.2pre6
	 */
	public int xToOffset(int line, int x, boolean round)
	{
		x -= horizontalOffset;
		Chunk chunks = chunkCache.lineToChunkList(line);
		int offset = Chunk.xToOffset(chunks,x,round);
		if(offset == -1)
			offset = getLineLength(line);
		return offset;
	} //}}}

	//{{{ getSelectionStart() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStart()</code> method
	 */
	public final int getSelectionStart()
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getStart();
	} //}}}

	//{{{ getSelectionStart() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStart(int)</code> method
	 */
	public int getSelectionStart(int line)
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getStart(
			buffer,line);
	} //}}}

	//{{{ getSelectionStartLine() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStartLine()</code> method
	 */
	public final int getSelectionStartLine()
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getStartLine();
	} //}}}

	//{{{ setSelectionStart() method
	/**
	 * @deprecated Do not use.
	 */
	public final void setSelectionStart(int selectionStart)
	{
		select(selectionStart,getSelectionEnd(),true);
	} //}}}

	//{{{ getSelectionEnd() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEnd()</code> method
	 */
	public final int getSelectionEnd()
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getEnd();
	} //}}}

	//{{{ getSelectionEnd() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEnd(int)</code> method
	 */
	public int getSelectionEnd(int line)
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getEnd(
			buffer,line);
	} //}}}

	//{{{ getSelectionEndLine() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEndLine()</code> method
	 */
	public final int getSelectionEndLine()
	{
		if(selection.size() != 1)
			return caret;

		return ((Selection)selection.elementAt(0)).getEndLine();
	} //}}}

	//{{{ setSelectionEnd() method
	/**
	 * @deprecated Do not use.
	 */
	public final void setSelectionEnd(int selectionEnd)
	{
		select(getSelectionStart(),selectionEnd,true);
	} //}}}

	//{{{ getMarkPosition() method
	/**
	 * @deprecated Do not use.
	 */
	public final int getMarkPosition()
	{
		Selection s = getSelectionAtOffset(caret);
		if(s == null)
			return caret;

		if(s.start == caret)
			return s.end;
		else if(s.end == caret)
			return s.start;
		else
			return caret;
	} //}}}

	//{{{ getMarkLine() method
	/**
	 * @deprecated Do not use.
	 */
	public final int getMarkLine()
	{
		if(selection.size() != 1)
			return caretLine;

		Selection s = (Selection)selection.elementAt(0);
		if(s.start == caret)
			return s.endLine;
		else if(s.end == caret)
			return s.startLine;
		else
			return caretLine;
	} //}}}

	//{{{ select() method
	/**
	 * @deprecated Instead, call either <code>addToSelection()</code>,
	 * or <code>setSelection()</code> with a new Selection instance.
	 */
	public void select(int start, int end)
	{
		select(start,end,true);
	} //}}}

	//{{{ select() method
	/**
	 * @deprecated Instead, call either <code>addToSelection()</code>,
	 * or <code>setSelection()</code> with a new Selection instance.
	 */
	public void select(int start, int end, boolean doElectricScroll)
	{
		selectNone();

		int newStart, newEnd;
		if(start < end)
		{
			newStart = start;
			newEnd = end;
		}
		else
		{
			newStart = end;
			newEnd = start;
		}

		setSelection(new Selection.Range(newStart,newEnd));
		moveCaretPosition(end,doElectricScroll);
	} //}}}

	//{{{ isSelectionRectangular() method
	/**
	 * @deprecated Instead, check if the appropriate Selection
	 * is an instance of the Selection.Rect class.
	 */
	public boolean isSelectionRectangular()
	{
		Selection s = getSelectionAtOffset(caret);
		if(s == null)
			return false;
		else
			return (s instanceof Selection.Rect);
	} //}}}

	//}}}

	//{{{ Package-private members

	//{{{ Instance variables
	Segment lineSegment;
	MouseHandler mouseHandler;
	ChunkCache chunkCache;

	int maxHorizontalScrollWidth;

	boolean softWrap;
	boolean hardWrap;
	float tabSize;
	int wrapMargin;
	boolean wrapToWidth;
	int charWidth;
	boolean monospacedHack;

	boolean scrollBarsInitialized;

	// not private so that inner classes can use without access$ methods.
	// see moveCaretPosition() and BufferChangeHandler.content{Inserted,Removed}()
	// for details of how these are used.
	//
	// maybe should do it some other way since in fact the only runnable that
	// ever goes here is from finishCaretUpdate().
	boolean queuedScrollTo;
	boolean queuedScrollToElectric;
	boolean queuedFireCaretEvent;
	ArrayList runnables;

	// this is package-private so that the painter can use it without
	// having to call getSelection() (which involves an array copy)
	Vector selection;

	// used to store offsetToXY() results
	Point returnValue;
	//}}}

	//{{{ isCaretVisible() method
	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	final boolean isCaretVisible()
	{
		return blink && hasFocus();
	} //}}}

	//{{{ isHighlightVisible() method
	/**
	 * Returns true if the bracket highlight is visible, false otherwise.
	 */
	final boolean isBracketHighlightVisible()
	{
		return bracketLine != -1
			&& hasFocus()
			&& foldVisibilityManager.isLineVisible(bracketLine)
			&& foldVisibilityManager.isLineVisible(caretLine);
	} //}}}

	//{{{ updateMaxHorizontalScrollWidth() method
	void updateMaxHorizontalScrollWidth()
	{
		int max = chunkCache.getMaxHorizontalScrollWidth();

		if(max != maxHorizontalScrollWidth)
		{
			maxHorizontalScrollWidth = max;
			horizontal.setValues(Math.max(0,
				Math.min(maxHorizontalScrollWidth + charWidth
				- painter.getWidth(),
				-horizontalOffset)),
				painter.getWidth(),
				0,maxHorizontalScrollWidth
				+ charWidth);
		}
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		lastLinePartial = (height % lineHeight != 0);

		chunkCache.recalculateVisibleLines();
		propertiesChanged();
	} //}}}

	//{{{ foldStructureChanged() method
	void foldStructureChanged()
	{
		chunkCache.invalidateAll();

		/* // physFirstLine should not be invisible
		while(!foldVisibilityManager.isLineVisible(physFirstLine)
			&& physFirstLine != 0)
		{
			physFirstLine--;
		} */

		physFirstLine = virtualToPhysical(firstLine);
		setFirstLine(physicalToVirtual(physFirstLine));

		// update scroll bars because the number of virtual lines might
		// have changed even if first line didn't change
		updateScrollBars();

		recalculateLastPhysicalLine();

		// repaint gutter and painter
		gutter.repaint();
		painter.repaint();
	} //}}}

	//{{{ getSubregionOfOffset() method
	/**
	 * Returns the subregion containing the specified offset. A subregion
	 * is a subset of a physical line. Each screen line corresponds to one
	 * subregion. Unlike the {@link #getScreenLineOfOffset()} method,
	 * this method works with non-visible lines too.
	 * @since jEdit 4.0pre6
	 */
	// not public yet
	/* public */ int getSubregionOfOffset(int offset, ChunkCache.LineInfo[] lineInfos)
	{
		//ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(line);
		for(int i = 0; i < lineInfos.length; i++)
		{
			ChunkCache.LineInfo info = lineInfos[i];
			if(offset >= info.offset && offset < info.offset + info.length)
				return i;
		}

		return -1;
	} //}}}

	//{{{ xToSubregionOffset() method
	/**
	 * Converts an x co-ordinate within a subregion into an offset from the
	 * start of that subregion.
	 * @param info The line info object
	 * @param x The x co-ordinate
	 * @param round Round up to next character if x is past the middle of a
	 * character?
	 * @since jEdit 4.0pre6
	 */
	// not public yet
	/* public */ int xToSubregionOffset(ChunkCache.LineInfo info, float x,
		boolean round)
	{
		int offset = Chunk.xToOffset(info.chunks,
			x - horizontalOffset,round);
		if(offset == -1 || offset == info.offset + info.length)
			offset = info.offset + info.length - 1;

		return offset;
	} //}}}

	//{{{ subregionOffsetToX() method
	/**
	 * Converts an offset within a subregion into an x co-ordinate.
	 * @param info The line info object
	 * @param offset The offset
	 * @since jEdit 4.0pre6
	 */
	// not public yet
	/* public */ int subregionOffsetToX(ChunkCache.LineInfo info, int offset)
	{
		return (int)(horizontalOffset + Chunk.offsetToX(
			info.chunks,offset));
	} //}}}

	//{{{ getSubregionStartOffset() method
	/**
	 * Returns the start offset of the specified subregion of the specified
	 * physical line.
	 * @param line The physical line number
	 * @param subregion The subregion
	 * @since jEdit 4.0pre6
	 */
	// not public yet
	/* public int getSubregionStartOffset(int line, int subregion)
	{
		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(line);
		return buffer.getLineStartOffset(lineInfos[subregion].physicalLine)
			+ lineInfos[subregion].offset;
	} */ //}}}

	//{{{ getSubregionEndOffset() method
	/**
	 * Returns the end offset of the specified subregion of the specified
	 * physical line.
	 * @param line The physical line number
	 * @param subregion The subregion
	 * @since jEdit 4.0pre6
	 */
	// not public yet
	/* public int getSubregionEndOffset(int line, int subregion)
	{
		ChunkCache.LineInfo[] lineInfos = chunkCache.getLineInfosForPhysicalLine(line);
		ChunkCache.LineInfo info = lineInfos[subregion];
		return buffer.getLineStartOffset(info.physicalLine)
			+ info.offset + info.length;
	} */ //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static String CENTER = "center";
	private static String RIGHT = "right";
	private static String LEFT = "left";
	private static String BOTTOM = "bottom";

	private static Timer caretTimer;
	private static JEditTextArea focusedComponent;
	//}}}

	//{{{ Instance variables
	private View view;
	private Gutter gutter;
	private TextAreaPainter painter;

	private JPopupMenu popup;

	private EventListenerList listenerList;
	private MutableCaretEvent caretEvent;

	private boolean caretBlinks;
	private boolean blink;

	private int firstLine;
	private int physFirstLine;
	private int physLastLine;
	private int screenLastLine;
	private boolean lastLinePartial;

	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;

	private boolean quickCopy;

	private JScrollBar vertical;
	private JScrollBar horizontal;

	private boolean bufferChanging;
	private Buffer buffer;
	private FoldVisibilityManager foldVisibilityManager;
	private BufferChangeHandler bufferHandler;
	private boolean bufferHandlerInstalled;

	private int caret;
	private int caretLine;
	private int caretScreenLine;

	private int bracketPosition;
	private int bracketLine;

	private int magicCaret;

	private boolean multi;
	private boolean overwrite;

	private int maxLineLen;
	//}}}

	//{{{ _addToSelection() method
	private void _addToSelection(Selection addMe)
	{
		if(addMe.start > addMe.end)
		{
			throw new IllegalArgumentException(addMe.start
				+ " > " + addMe.end);
		}
		else if(addMe.start == addMe.end)
		{
			if(addMe instanceof Selection.Range)
				return;
			else if(addMe instanceof Selection.Rect)
			{
				if(((Selection.Rect)addMe).extraEndVirt == 0)
					return;
			}
		}

		for(int i = 0; i < selection.size(); i++)
		{
			// try and merge existing selections one by
			// one with the new selection
			Selection s = (Selection)selection.elementAt(i);
			if(s.overlaps(addMe))
			{
				addMe.start = Math.min(s.start,addMe.start);
				addMe.end = Math.max(s.end,addMe.end);

				selection.removeElement(s);
				i--;
			}
		}

		addMe.startLine = getLineOfOffset(addMe.start);
		addMe.endLine = getLineOfOffset(addMe.end);

		boolean added = false;

		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			if(addMe.start < s.start)
			{
				selection.insertElementAt(addMe,i);
				added = true;
				break;
			}
		}

		if(!added)
			selection.addElement(addMe);

		invalidateLineRange(addMe.startLine,addMe.endLine);
	} //}}}

	//{{{ finishCaretUpdate() method
	/**
	 * the collapsing of scrolling/event firing inside compound edits
	 * greatly speeds up replace-all.
	 */
	private void finishCaretUpdate(boolean doElectricScroll,
		boolean fireCaretEvent)
	{
		if(queuedScrollTo)
			return;

		this.queuedScrollToElectric |= doElectricScroll;
		this.queuedFireCaretEvent |= fireCaretEvent;

		Runnable r = new Runnable()
		{
			public void run()
			{
				// When the user is typing, etc, we don't want the caret
				// to blink
				blink = true;
				caretTimer.restart();

				scrollToCaret(queuedScrollToElectric);
				updateBracketHighlight();
				if(queuedFireCaretEvent)
					fireCaretEvent();
				queuedScrollTo = queuedScrollToElectric
					= queuedFireCaretEvent = false;
			}
		};

		if(buffer.isTransactionInProgress())
		{
			queuedScrollTo = true;
			runnables.add(r);
		}
		else
			r.run();
	} //}}}

	//{{{ fireCaretEvent() method
	private void fireCaretEvent()
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == CaretListener.class)
			{
				try
				{
					((CaretListener)listeners[i+1]).caretUpdate(caretEvent);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,t);
				}
			}
		}
	} //}}}

	//{{{ fireScrollEvent() method
	private void fireScrollEvent(boolean vertical)
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == ScrollListener.class)
			{
				try
				{
					if(vertical)
						((ScrollListener)listeners[i+1]).scrolledVertically(this);
					else
						((ScrollListener)listeners[i+1]).scrolledHorizontally(this);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,t);
				}
			}
		}
	} //}}}

	//{{{ insertTab() method
	private void insertTab()
	{
		int tabSize = buffer.getTabSize();
		if(buffer.getBooleanProperty("noTabs"))
		{
			int lineStart = getLineStartOffset(caretLine);

			String line = getText(lineStart,caret - lineStart);

			int pos = 0;

			for(int i = 0; i < line.length(); i++)
			{
				switch(line.charAt(pos))
				{
				case '\t':
					pos = 0;
					break;
				default:
					if(++pos >= tabSize)
						pos = 0;
					break;
				}
			}

			setSelectedText(MiscUtilities.createWhiteSpace(
				tabSize - pos,0));
		}
		else
			setSelectedText("\t");
	} //}}}

	//{{{ doWordWrap() method
	private boolean doWordWrap(boolean spaceInserted)
	{
		if(!hardWrap || maxLineLen <= 0)
			return false;

		buffer.getLineText(caretLine,lineSegment);

		int start = getLineStartOffset(caretLine);
		int end = getLineEndOffset(caretLine);
		int len = end - start - 1;

		int caretPos = caret - start;

		// only wrap if we're at the end of a line, or the rest of the
		// line text is whitespace
		for(int i = caretPos; i < len; i++)
		{
			char ch = lineSegment.array[lineSegment.offset + i];
			if(ch != ' ' && ch != '\t')
				return false;
		}

		boolean returnValue = false;

		int tabSize = buffer.getTabSize();

		String wordBreakChars = buffer.getStringProperty("wordBreakChars");

		int logicalLength = 0; // length with tabs expanded
		int lastWordOffset = -1;
		boolean lastWasSpace = true;
		for(int i = 0; i < caretPos; i++)
		{
			char ch = lineSegment.array[lineSegment.offset + i];
			if(ch == '\t')
			{
				logicalLength += tabSize - (logicalLength % tabSize);
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(ch == ' ')
			{
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(wordBreakChars != null && wordBreakChars.indexOf(ch) != -1)
			{
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else
			{
				logicalLength++;
				lastWasSpace = false;
			}

			int insertNewLineAt;
			if(spaceInserted && logicalLength == maxLineLen
				&& i == caretPos - 1)
			{
				insertNewLineAt = caretPos;
				returnValue = true;
			}
			else if(logicalLength >= maxLineLen && lastWordOffset != -1)
				insertNewLineAt = lastWordOffset;
			else
				continue;

			try
			{
				buffer.beginCompoundEdit();
				buffer.insert(start + insertNewLineAt,"\n");
				// caretLine would have been incremented
				// since insertNewLineAt <= caretPos
				buffer.indentLine(caretLine,true,true);
			}
			finally
			{
				buffer.endCompoundEdit();
			}

			/* only ever return true if space was pressed
			 * with logicalLength == maxLineLen */
			return returnValue;
		}

		return false;
	} //}}}

	//{{{ doWordCount() method
	private void doWordCount(View view, String text)
	{
		char[] chars = text.toCharArray();
		int characters = chars.length;
		int words;
		if(characters == 0)
			words = 0;
		else
			words = 1;
		int lines = 1;
		boolean word = false;
		for(int i = 0; i < chars.length; i++)
		{
			switch(chars[i])
			{
			case '\r': case '\n':
				lines++;
			case ' ': case '\t':
				if(word)
				{
					words++;
					word = false;
				}
				break;
			default:
				word = true;
				break;
			}
		}
		Object[] args = { new Integer(characters), new Integer(words),
			new Integer(lines) };
		GUIUtilities.message(view,"wordcount",args);
	} //}}}

	//{{{ updateBracketHighlight() method
	private void updateBracketHighlight()
	{
		if(!painter.isBracketHighlightEnabled())
			return;

		if(bracketLine != -1)
			invalidateLineRange(bracketLine,caretLine);

		int offset = caret - getLineStartOffset(caretLine);

		if(offset != 0)
		{
			int bracketOffset = TextUtilities.findMatchingBracketFuzzy(
				buffer,caretLine,offset - 1);
			if(bracketOffset != -1)
			{
				bracketLine = getLineOfOffset(bracketOffset);
				bracketPosition = bracketOffset
					- getLineStartOffset(bracketLine);
				invalidateLineRange(bracketLine,caretLine);

				if(bracketLine < physFirstLine
					|| bracketLine > physLastLine)
				{
					showBracketStatusMessage(bracketLine < caretLine);
				}
				return;
			}
		}

		bracketLine = bracketPosition = -1;
	} //}}}

	//{{{ showBracketStatusMessage() method
	private void showBracketStatusMessage(boolean backward)
	{
		String text = buffer.getLineText(bracketLine).trim();
		if(backward && bracketLine != 0 && text.length() == 1)
		{
			switch(text.charAt(0))
			{
			case '{': case '}':
			case '[': case ']':
			case '(': case ')':
				text = buffer.getLineText(bracketLine - 1).trim()
					+ " " + text;
				break;
			}
		}

		// get rid of embedded tabs not removed by trim()
		text = text.replace('\t',' ');

		view.getStatus().setMessageAndClear(jEdit.getProperty(
			"view.status.bracket",new String[] { text }));
	} //}}}

	//{{{ recalculateLastPhysicalLine() method
	void recalculateLastPhysicalLine()
	{
		if(softWrap)
		{
			chunkCache.updateChunksUpTo(visibleLines);
			for(int i = visibleLines; i >= 0; i--)
			{
				ChunkCache.LineInfo info = chunkCache.getLineInfo(i);
				if(info.physicalLine != -1)
				{
					physLastLine = info.physicalLine;
					screenLastLine = i;
					break;
				}
			}
		}
		else
		{
			// this is faster
			int virtLastLine = Math.min(foldVisibilityManager
				.getVirtualLineCount() - 1,
				firstLine + visibleLines);
			screenLastLine = virtLastLine - firstLine;
			physLastLine = foldVisibilityManager.virtualToPhysical(
				virtLastLine);
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ TextAreaBorder class
	static class TextAreaBorder extends AbstractBorder
	{
		private static final Insets insets = new Insets(1, 1, 2, 2);

		//{{{ paintBorder() method
		public void paintBorder(Component c, Graphics g, int x, int y,
			int width, int height)
		{
			g.translate(x,y);

			g.setColor(MetalLookAndFeel.getControlDarkShadow());
			g.drawRect(0,0,width-2,height-2);

			g.setColor(MetalLookAndFeel.getControlHighlight());
			g.drawLine(width-1,1,width-1,height-1);
			g.drawLine(1,height-1,width-1,height-1);

			g.setColor(MetalLookAndFeel.getControl());
			g.drawLine(width-2,2,width-2,2);
			g.drawLine(1,height-2,1,height-2);

			g.translate(-x,-y);
		} //}}}

		//{{{ getBorderInsets() method
		public Insets getBorderInsets(Component c)
		{
			return new Insets(1,1,2,2);
		} //}}}
	} //}}}

	//{{{ ScrollLayout class
	class ScrollLayout implements LayoutManager
	{
		//{{{ addLayoutComponent() method
		public void addLayoutComponent(String name, Component comp)
		{
			if(name.equals(CENTER))
				center = comp;
			else if(name.equals(RIGHT))
				right = comp;
			else if(name.equals(LEFT))
				left = comp;
			else if(name.equals(BOTTOM))
				bottom = comp;
		} //}}}

		//{{{ removeLayoutComponent() method
		public void removeLayoutComponent(Component comp)
		{
			if(center == comp)
				center = null;
			else if(right == comp)
				right = null;
			else if(left == comp)
				left = null;
			else if(bottom == comp)
				bottom = null;
		} //}}}

		//{{{ preferredLayoutSize() method
		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			Dimension leftPref = left.getPreferredSize();
			dim.width += leftPref.width;
			Dimension centerPref = center.getPreferredSize();
			dim.width += centerPref.width;
			dim.height += centerPref.height;
			Dimension rightPref = right.getPreferredSize();
			dim.width += rightPref.width;
			Dimension bottomPref = bottom.getPreferredSize();
			dim.height += bottomPref.height;

			return dim;
		} //}}}

		//{{{ minimumLayoutSize() method
		public Dimension minimumLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			Dimension leftPref = left.getMinimumSize();
			dim.width += leftPref.width;
			Dimension centerPref = center.getMinimumSize();
			dim.width += centerPref.width; 
			dim.height += centerPref.height;
			Dimension rightPref = right.getMinimumSize();
			dim.width += rightPref.width;
			Dimension bottomPref = bottom.getMinimumSize();
			dim.height += bottomPref.height;

			return dim;
		} //}}}

		//{{{ layoutContainer() method
		public void layoutContainer(Container parent)
		{
			Dimension size = parent.getSize();
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

			int itop = insets.top;
			int ileft = insets.left;
			int ibottom = insets.bottom;
			int iright = insets.right;

			int rightWidth = right.getPreferredSize().width;
			int leftWidth = left.getPreferredSize().width;
			int bottomHeight = bottom.getPreferredSize().height;
			int centerWidth = Math.max(0,size.width - leftWidth
				- rightWidth - ileft - iright);
			int centerHeight = Math.max(0,size.height
				- bottomHeight - itop - ibottom);

			left.setBounds(
				ileft,
				itop,
				leftWidth,
				centerHeight);

			center.setBounds(
				ileft + leftWidth,
				itop,
				centerWidth,
				centerHeight);

			right.setBounds(
				ileft + leftWidth + centerWidth,
				itop,
				rightWidth,
				centerHeight);

			bottom.setBounds(
				ileft,
				itop + centerHeight,
				Math.max(0,size.width - rightWidth - ileft - iright),
				bottomHeight);
		} //}}}

		Component center;
		Component left;
		Component right;
		Component bottom;
	} //}}}

	//{{{ CaretBlinker class
	static class CaretBlinker implements ActionListener
	{
		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			if(focusedComponent != null && focusedComponent.hasFocus())
				focusedComponent.blinkCaret();
		} //}}}
	} //}}}

	//{{{ MutableCaretEvent class
	class MutableCaretEvent extends CaretEvent
	{
		//{{{ MutableCaretEvent constructor
		MutableCaretEvent()
		{
			super(JEditTextArea.this);
		} //}}}

		//{{{ getDot() method
		public int getDot()
		{
			return getCaretPosition();
		} //}}}

		//{{{ getMark() method
		public int getMark()
		{
			return getMarkPosition();
		} //}}}
	} //}}}

	//{{{ AdjustHandler class
	class AdjustHandler implements AdjustmentListener
	{
		//{{{ adjustmentValueChanged() method
		public void adjustmentValueChanged(final AdjustmentEvent evt)
		{
			if(!scrollBarsInitialized)
				return;

			if(evt.getAdjustable() == vertical)
				setFirstLine(vertical.getValue());
			else
				setHorizontalOffset(-horizontal.getValue());
		} //}}}
	} //}}}

	//{{{ BufferChangeHandler class
	/**
	 * Note that in this class we take great care to defer complicated
	 * calculations to the end of the current transaction if the buffer
	 * informs us a compound edit is in progress
	 * (<code>isTransactionInProgress()</code>).
	 *
	 * This greatly speeds up replace all for example, by only doing certain
	 * things once, particularly in <code>moveCaretPosition()</code>.
	 *
	 * Try doing a replace all in a large file, for example. It is very slow
	 * in 3.2, faster in 4.0 (where the transaction optimization was
	 * introduced) and faster still in 4.1 (where it was further improved).
	 *
	 * There is still work to do; see TODO.txt.
	 */
	class BufferChangeHandler implements BufferChangeListener
	{
		boolean delayedUpdate;
		boolean delayedMultilineUpdate;
		int delayedRepaintStart;
		int delayedRepaintEnd;
		boolean delayedRecalculateLastPhysicalLine;
		// if changes are being made above the first line, we don't want
		// them to scroll us down or up. so we store the first line
		// position at the start of the transaction.
		Position holdPosition;

		//{{{ foldLevelChanged() method
		public void foldLevelChanged(Buffer buffer, int start, int end)
		{
			if(!bufferChanging && end != 0 && buffer.isLoaded())
			{
				invalidateLineRange(start - 1,end - 1);
			}
		} //}}}

		//{{{ contentInserted() method
		public void contentInserted(Buffer buffer, int startLine, int start,
			int numLines, int length)
		{
			// ... otherwise,buffer will call transactionComplete()
			// later on
			//if(!buffer.isTransactionInProgress())
			chunkCache.invalidateChunksFromPhys(startLine);

			_recalculateLastPhysicalLine(numLines);

			if(!buffer.isLoaded())
				return;

			repaintAndScroll(startLine,numLines);

			//{{{ resize selections if necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if((s instanceof Selection.Rect && s.start > start)
					|| (s instanceof Selection.Range && s.start >= start))
				{
					s.start += length;
					if(numLines != 0)
						s.startLine = getLineOfOffset(s.start);
					changed = true;
				}

				if(s.end >= start)
				{
					s.end += length;
					if(numLines != 0)
						s.endLine = getLineOfOffset(s.end);
					changed = true;
				}

				if(changed)
				{
					delayedRepaintStart = Math.min(
						delayedRepaintStart,
						s.startLine);
					delayedRepaintEnd = Math.max(
						delayedRepaintEnd,
						s.endLine);
				}
			} //}}}

			// ... otherwise,buffer will call transactionComplete()
			// later on
			if(!buffer.isTransactionInProgress())
				transactionComplete(buffer);

			if(caret >= start)
				moveCaretPosition(caret + length,true);
			else
			{
				// will update bracket highlight
				moveCaretPosition(caret);
			}
		}
		//}}}

		//{{{ contentRemoved() method
		public void contentRemoved(Buffer buffer, int startLine, int start,
			int numLines, int length)
		{
			// ... otherwise,buffer will call transactionComplete()
			// later on
			//if(!buffer.isTransactionInProgress())
			chunkCache.invalidateChunksFromPhys(startLine);

			if(!buffer.isLoaded())
				return;

			// -numLines because they are removed.
			repaintAndScroll(startLine,-numLines);
			_recalculateLastPhysicalLine(-numLines);

			int end = start + length;

			//{{{ resize selections if necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if(s.start > start && s.start <= end)
				{
					s.start = start;
					changed = true;
				}
				else if(s.start > end)
				{
					s.start -= length;
					changed = true;
				}

				if(s.end > start && s.end <= end)
				{
					s.end = start;
					changed = true;
				}
				else if(s.end > end)
				{
					s.end -= length;
					changed = true;
				}

				if(s.start == s.end)
				{
					selection.removeElement(s);
					delayedRepaintStart = Math.min(
						delayedRepaintStart,
						s.startLine);
					delayedRepaintEnd = Math.max(
						delayedRepaintEnd,
						s.endLine);
					i--;
				}
				else if(changed)
				{
					if(numLines != 0)
					{
						s.startLine = getLineOfOffset(s.start);
						s.endLine = getLineOfOffset(s.end);
					}
					delayedRepaintStart = Math.min(
						delayedRepaintStart,
						s.startLine);
					delayedRepaintEnd = Math.max(
						delayedRepaintEnd,
						s.endLine);
				}
			} //}}}

			// this is a rough workaround but it ensures that
			// scroll listeners never receive an event while the
			// text area caret position is invalid.
			int oldFirstLine = getFirstLine();

			if(caret > start && caret <= end)
				moveCaretPosition(start,false);
			else if(caret > end)
				moveCaretPosition(caret - length,false);
			else
			{
				// will update bracket highlight
				moveCaretPosition(caret);
			}

			if(getFirstLine() != oldFirstLine)
			{
				// so that transactionComplete() doesn't scroll
				holdPosition = null;
			}

			// ... otherwise, it will be called by the buffer
			if(!buffer.isTransactionInProgress())
				transactionComplete(buffer);
		}
		//}}}

		//{{{ transactionComplete() method
		public void transactionComplete(Buffer buffer)
		{
			if(delayedUpdate)
			{
				//chunkCache.invalidateChunksFromPhys(delayedRepaintStart);

				if(holdPosition != null)
				{
					setFirstLine(physicalToVirtual(
						getBuffer().getLineOfOffset(
						holdPosition.getOffset())));
					holdPosition = null;
				}

				if(delayedMultilineUpdate)
				{
					updateScrollBars();
					invalidateScreenLineRange(chunkCache
						.getScreenLineOfOffset(
						delayedRepaintStart,0),
						screenLastLine);
					delayedMultilineUpdate = false;
				}
				else
				{
					invalidateLineRange(delayedRepaintStart,
						delayedRepaintEnd);
				}

				delayedUpdate = false;
			}

			if(delayedRecalculateLastPhysicalLine)
			{
				int oldScreenLastLine = screenLastLine;
				recalculateLastPhysicalLine();
				invalidateScreenLineRange(oldScreenLastLine,
					screenLastLine);
				delayedRecalculateLastPhysicalLine = false;
			}

			for(int i = 0; i < runnables.size(); i++)
				((Runnable)runnables.get(i)).run();
			runnables.clear();
		} //}}}

		//{{{ _recalculateLastPhysicalLine() method
		private void _recalculateLastPhysicalLine(int numLines)
		{
			// Inserting multiple lines can change the last physical
			// line due to folds being pushed down and so on
			if(numLines != 0 || (softWrap && getFoldVisibilityManager()
				.getLastVisibleLine() - numLines <= getLastPhysicalLine()))
			{
				delayedRecalculateLastPhysicalLine = true;
			}
		} //}}}

		//{{{ repaintAndScroll() method
		private void repaintAndScroll(int startLine, int numLines)
		{
			if(numLines != 0)
				delayedMultilineUpdate = true;

			if(!delayedUpdate)
			{
				delayedRepaintStart = startLine;
				delayedRepaintEnd = startLine;
				delayedUpdate = true;
			}
			else
			{
				delayedRepaintStart = Math.min(
					delayedRepaintStart,
					startLine);
				delayedRepaintEnd = Math.max(
					delayedRepaintEnd,
					startLine);
			}

			// this is less than ideal... but it fixes the issue
			// mentioned in the holdPosition declaration comment
			// much better than the old fix, which didn't work
			// with folding.
			if(holdPosition != null)
				return;
			if(startLine < getFirstPhysicalLine())
			{
				if(startLine < getFoldVisibilityManager().getFirstVisibleLine())
				{
					// need to update these two!
					physFirstLine = virtualToPhysical(firstLine);
					delayedRecalculateLastPhysicalLine = true;
					return;
				}

				int virtStartLine = physicalToVirtual(startLine);

				int pos;

				int virtLine = getFirstLine() + physicalToVirtual(
					Math.max(0,startLine + numLines))
					- virtStartLine;
				if(virtLine < 0)
					pos = 0;
				else if(virtLine >= getVirtualLineCount())
					pos = getBufferLength();
				else
				{
					pos = buffer.getLineStartOffset(
						virtualToPhysical(virtLine));
				}

				holdPosition = buffer.createPosition(pos);
			}
		} //}}}
	} //}}}

	//{{{ FocusHandler class
	class FocusHandler implements FocusListener
	{
		//{{{ focusGained() method
		public void focusGained(FocusEvent evt)
		{
			if(bufferChanging)
				return;

			if(bracketLine != -1)
				invalidateLineRange(bracketLine,caretLine);
			else
				invalidateLine(caretLine);

			// repaint the gutter so that the border color
			// reflects the focus state
			view.updateGutterBorders();
		} //}}}

		//{{{ focusLost() method
		public void focusLost(FocusEvent evt)
		{
			if(!isShowing())
				return;

			if(bracketLine != -1)
				invalidateLineRange(bracketLine,caretLine);
			else
				invalidateLine(caretLine);
		} //}}}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseInputAdapter
	{
		private int dragStartLine;
		private int dragStartOffset;
		private int dragStart;
		private int clickCount;
		private boolean dragged;
		private boolean quickCopyDrag;
		private boolean clearStatus;
		private boolean control;

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
			control = (OperatingSystem.isMacOS() && evt.isMetaDown())
				|| (!OperatingSystem.isMacOS() && evt.isControlDown());

			// so that Home <mouse click> Home is not the same
			// as pressing Home twice in a row
			view.getInputHandler().resetLastActionCount();

			grabFocus();

			if(GUIUtilities.isPopupTrigger(evt) && popup != null)
			{
				if(popup.isVisible())
					popup.setVisible(false);
				else
				{
					GUIUtilities.showPopupMenu(popup,painter,
						evt.getX(),evt.getY());
				}
				return;
			}

			quickCopyDrag = (isQuickCopyEnabled()
				&& GUIUtilities.isMiddleButton(evt.getModifiers()));
			blink = true;
			invalidateLine(caretLine);

			int x = evt.getX();
			int y = evt.getY();

			dragStart = xyToOffset(x,y,!(painter.isBlockCaretEnabled()
				|| isOverwriteEnabled()));
			dragStartLine = getLineOfOffset(dragStart);
			dragStartOffset = dragStart - getLineStartOffset(dragStartLine);

			dragged = false;

			clickCount = evt.getClickCount();

			switch(clickCount)
			{
			case 1:
				doSingleClick(evt);
				break;
			case 2:
				doDoubleClick(evt);
				break;
			default: //case 3:
				doTripleClick(evt);
				break;
			}
		} //}}}

		//{{{ doSingleClick() method
		private void doSingleClick(MouseEvent evt)
		{
			/* if(buffer.insideCompoundEdit())
				buffer.endCompoundEdit(); */

			if(evt.isShiftDown())
			{
				float dragStartLineWidth = offsetToXY(dragStartLine,
					getLineLength(dragStartLine),returnValue).x;
				int extraEndVirt;
				int x = evt.getX();
				if(x > dragStartLineWidth)
				{
					extraEndVirt = (int)((x - dragStartLineWidth)
						/ charWidth);
					if(x % charWidth  > charWidth / 2)
						extraEndVirt++;
				}
				else
					extraEndVirt = 0;

				// XXX: getMarkPosition() deprecated!
				resizeSelection(getMarkPosition(),dragStart,extraEndVirt,control);

				moveCaretPosition(dragStart,false);

				// so that shift-click-drag works
				dragStartLine = getMarkLine();
				dragStart = getMarkPosition();
				dragStartOffset = dragStart
					- getLineStartOffset(dragStartLine);
			}
			else
			{
				if(!(multi || quickCopyDrag))
					selectNone();

				if(!quickCopyDrag)
					moveCaretPosition(dragStart,false);
			}
		} //}}}

		//{{{ doDoubleClick() method
		private void doDoubleClick(MouseEvent evt)
		{
			// Ignore empty lines
			if(getLineLength(dragStartLine) == 0)
				return;

			String lineText = getLineText(dragStartLine);
			String noWordSep = buffer.getStringProperty("noWordSep");
			if(dragStartOffset == getLineLength(dragStartLine))
				dragStartOffset--;

			boolean joinNonWordChars =
				jEdit.getBooleanProperty("view.joinNonWordChars");
			int wordStart = TextUtilities.findWordStart(lineText,
				dragStartOffset,noWordSep,joinNonWordChars);
			int wordEnd = TextUtilities.findWordEnd(lineText,
				dragStartOffset+1,noWordSep,joinNonWordChars);

			int lineStart = getLineStartOffset(dragStartLine);
			addToSelection(new Selection.Range(lineStart + wordStart,
				lineStart + wordEnd));

			if(quickCopyDrag)
				quickCopyDrag = false;

			moveCaretPosition(lineStart + wordEnd,false);

			// with double clicks, even if nothing extra
			// selected, still activate quick copy drag code
			dragged = true;
		} //}}}

		//{{{ doTripleClick() method
		private void doTripleClick(MouseEvent evt)
		{
			int newCaret = getLineEndOffset(dragStartLine);
			if(dragStartLine == buffer.getLineCount() - 1)
				newCaret--;

			addToSelection(new Selection.Range(
				getLineStartOffset(dragStartLine),
				newCaret));

			if(quickCopyDrag)
				quickCopyDrag = false;

			moveCaretPosition(newCaret);
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent evt)
		{
			if(GUIUtilities.isPopupTrigger(evt)
				|| (popup != null && popup.isVisible()))
				return;

			if(evt.getY() < 0)
			{
				setFirstLine(getFirstLine() - 2);
			}
			else if(evt.getY() >= getHeight())
			{
				setFirstLine(getFirstLine() + 1);
			}

			if(quickCopyDrag)
			{
				view.getStatus().setMessage(jEdit.getProperty(
					"view.status.quick-copy"));
				clearStatus = true;
			}
			else if(control)
			{
				view.getStatus().setMessage(jEdit.getProperty(
					"view.status.rect-select"));
				clearStatus = true;
			}

			switch(clickCount)
			{
			case 1:
				doSingleDrag(evt,control);
				break;
			case 2:
				doDoubleDrag(evt);
				break;
			default: //case 3:
				doTripleDrag(evt);
				break;
			}
		} //}}}

		//{{{ doSingleDrag() method
		private void doSingleDrag(MouseEvent evt, boolean rect)
		{
			dragged = true;

			int x = evt.getX();
			int dot = xyToOffset(x,
				Math.max(0,Math.min(painter.getHeight(),evt.getY())),
				(!painter.isBlockCaretEnabled()
				&& !isOverwriteEnabled())
				|| quickCopyDrag);
			int dotLine = buffer.getLineOfOffset(dot);
			int extraEndVirt = 0;

			if(dotLine != physLastLine || chunkCache.getLineInfo(
				screenLastLine).lastSubregion)
			{
				float dotLineWidth = offsetToXY(dotLine,getLineLength(dotLine),
					returnValue).x;
				if(x > dotLineWidth)
				{
					extraEndVirt = (int)((x - dotLineWidth) / charWidth);
					if(x % charWidth  > charWidth / 2)
						extraEndVirt++;
				}
			}

			resizeSelection(dragStart,dot,extraEndVirt,rect);

			if(quickCopyDrag)
			{
				// just scroll to the dragged location
				scrollTo(dotLine,dot - buffer.getLineStartOffset(dotLine),false);
			}
			else
			{
				if(dot != caret)
					moveCaretPosition(dot,false);
				if(rect && extraEndVirt != 0)
				{
					scrollTo(dotLine,dot - buffer.getLineStartOffset(dotLine)
						+ extraEndVirt,false);
				}
			}
		} //}}}

		//{{{ doDoubleDrag() method
		private void doDoubleDrag(MouseEvent evt)
		{
			int markLineStart = getLineStartOffset(dragStartLine);
			int markLineLength = getLineLength(dragStartLine);
			int mark = dragStartOffset;

			int pos = xyToOffset(evt.getX(),
				Math.max(0,Math.min(painter.getHeight(),evt.getY())),
				!(painter.isBlockCaretEnabled() || isOverwriteEnabled()));
			int line = getLineOfOffset(pos);
			int lineStart = getLineStartOffset(line);
			int lineLength = getLineLength(line);
			int offset = pos - lineStart;

			String lineText = getLineText(line);
			String markLineText = getLineText(dragStartLine);
			String noWordSep = buffer.getStringProperty("noWordSep");
			boolean joinNonWordChars =
				jEdit.getBooleanProperty("view.joinNonWordChars");

			if(markLineStart + dragStartOffset > lineStart + offset)
			{
				if(offset != 0 && offset != lineLength)
				{
					offset = TextUtilities.findWordStart(
						lineText,offset,noWordSep,
						joinNonWordChars);
				}

				if(markLineLength != 0)
				{
					mark = TextUtilities.findWordEnd(
						markLineText,mark,noWordSep,
						joinNonWordChars);
				}
			}
			else
			{
				if(offset != 0 && lineLength != 0)
				{
					offset = TextUtilities.findWordEnd(
						lineText,offset,noWordSep,
						joinNonWordChars);
				}

				if(mark != 0 && mark != markLineLength)
				{
					mark = TextUtilities.findWordStart(
						markLineText,mark,noWordSep,
						joinNonWordChars);
				}
			}

			if(lineStart + offset == caret)
				return;

			resizeSelection(markLineStart + mark,lineStart + offset,
				0,false);
			if(!quickCopyDrag)
				moveCaretPosition(lineStart + offset,false);
		} //}}}

		//{{{ doTripleDrag() method
		private void doTripleDrag(MouseEvent evt)
		{
			int offset = xyToOffset(evt.getX(),
				Math.max(0,Math.min(painter.getHeight(),evt.getY())),
				false);
			int mouseLine = getLineOfOffset(offset);
			int mark;
			int mouse;
			if(dragStartLine > mouseLine)
			{
				mark = getLineEndOffset(dragStartLine) - 1;
				if(offset == getLineEndOffset(mouseLine) - 1)
					mouse = offset;
				else
					mouse = getLineStartOffset(mouseLine);
			}
			else
			{
				mark = getLineStartOffset(dragStartLine);
				if(offset == getLineStartOffset(mouseLine))
					mouse = offset;
				else if(offset == getLineEndOffset(mouseLine) - 1
					&& mouseLine != getBuffer().getLineCount() - 1)
					mouse = getLineEndOffset(mouseLine);
				else
					mouse = getLineEndOffset(mouseLine) - 1;
			}

			mouse = Math.min(getBuffer().getLength(),mouse);

			if(mouse == caret)
				return;

			dragged = true;

			resizeSelection(mark,mouse,0,false);
			moveCaretPosition(mouse,false);
		} //}}}

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent evt)
		{
			if(control && !dragged)
			{
				int offset = xyToOffset(evt.getX(),evt.getY(),false);
				if(offset != buffer.getLength())
				{
					buffer.getText(offset,1,lineSegment);
					switch(lineSegment.array[lineSegment.offset])
					{
					case '(': case '[': case '{':
					case ')': case ']': case '}':
						moveCaretPosition(offset,false);
						selectToMatchingBracket();
						return;
					}
				}
			}

			// middle mouse button drag inserts selection
			// at caret position
			Selection sel = getSelectionAtOffset(dragStart);
			if(sel != null)
				Registers.setRegister('%',getSelectedText(sel));

			if(dragged && sel != null)
			{
				if(quickCopyDrag)
				{
					removeFromSelection(sel);
					Registers.paste(JEditTextArea.this,'%',
						sel instanceof Selection.Rect);
				}
				else
					Registers.setRegister('%',getSelectedText());
			}
			else if(isQuickCopyEnabled()
				&& GUIUtilities.isMiddleButton(evt.getModifiers()))
			{
				setCaretPosition(dragStart,false);
				if(!isEditable())
					getToolkit().beep();
				else
					Registers.paste(JEditTextArea.this,'%',control);
			}

			dragged = false;

			if(clearStatus)
			{
				clearStatus = false;
				view.getStatus().setMessage(null);
			}
		} //}}}
	} //}}}

	//}}}

	//{{{ Class initializer
	static
	{
		caretTimer = new Timer(500,new CaretBlinker());
		caretTimer.setInitialDelay(500);
		caretTimer.start();
	} //}}}
}
