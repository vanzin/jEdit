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
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
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
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		blink = true;
		lineSegment = new Segment();
		returnValue = new Point();
		structureMatchers = new LinkedList();
		structureMatchers.add(new StructureMatcher.BracketMatcher());
		//}}}

		//{{{ Initialize the GUI
		setLayout(new ScrollLayout());
		add(CENTER,painter);
		add(LEFT,gutter);

		// some plugins add stuff in a "right-hand" gutter
		verticalBox = new Box(BoxLayout.X_AXIS);
		verticalBox.add(vertical = new JScrollBar(JScrollBar.VERTICAL));
		vertical.setRequestFocusEnabled(false);
		add(RIGHT,verticalBox);
		add(BOTTOM,horizontal = new JScrollBar(JScrollBar.HORIZONTAL));
		horizontal.setRequestFocusEnabled(false);

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

	//{{{ dispose() method
	/**
	 * Plugins and macros should not call this method.
	 * @since jEdit 4.2pre1
	 */
	public void dispose()
	{
		DisplayManager.textAreaDisposed(this);
	} //}}}

	//{{{ Getters and setters

	//{{{ getView() method
	/**
	 * Returns this text area's view.
	 * @since jEdit 4.2pre5
	 */
	public View getView()
	{
		return view;
	} //}}}

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

	//{{{ getDisplayManager() method
	/**
	 * Returns the display manager used by this text area.
	 * @since jEdit 4.2pre1
	 */
	public DisplayManager getDisplayManager()
	{
		return displayManager;
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
				match = null;
			}
			this.buffer = buffer;

			chunkCache.setBuffer(buffer);
			propertiesChanged();

			if(displayManager != null)
			{
				DisplayManager.releaseDisplayManager(
					displayManager);
			}

			displayManager = DisplayManager.getDisplayManager(
				buffer,this);

			displayManager.init();

			maxHorizontalScrollWidth = 0;

			if(!buffer.isLoaded())
				updateScrollBars();

			repaint();

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

	//{{{ getDragAndDropCallback() method
	/**
	 * Drag and drop of text in jEdit is implementing using jEdit 1.4 APIs,
	 * however since jEdit must run with Java 1.3, this class only has the
	 * necessary support to call a hook method via reflection. The method
	 * is provided by the {@link org.gjt.sp.jedit.Java14} class and handles
	 * the drag and drop API calls themselves.
	 * @since jEdit 4.2pre5
	 */
	public Method getDragAndDropCallback()
	{
		return dndCallback;
	} //}}}

	//{{{ setDragAndDropCallback() method
	/**
	 * Drag and drop of text in jEdit is implementing using jEdit 1.4 APIs,
	 * however since jEdit must run with Java 1.3, this class only has the
	 * necessary support to call a hook method via reflection. The method
	 * is provided by the {@link org.gjt.sp.jedit.Java14} class and handles
	 * the drag and drop API calls themselves.
	 * @since jEdit 4.2pre5
	 */
	public void setDragAndDropCallback(Method meth)
	{
		dndCallback = meth;
	} //}}}

	//{{{ isDragInProgress() method
	/**
	 * Drag and drop of text in jEdit is implementing using jEdit 1.4 APIs,
	 * however since jEdit must run with Java 1.3, this class only has the
	 * necessary support to call a hook method via reflection. This method
	 * is called by the {@link org.gjt.sp.jedit.Java14} class to signal that
	 * a drag is in progress.
	 * @since jEdit 4.2pre5
	 */
	public boolean isDragInProgress()
	{
		return dndInProgress;
	} //}}}

	//{{{ setDragInProgress() method
	/**
	 * Drag and drop of text in jEdit is implementing using jEdit 1.4 APIs,
	 * however since jEdit must run with Java 1.3, this class only has the
	 * necessary support to call a hook method via reflection. This method
	 * is called by the {@link org.gjt.sp.jedit.Java14} class to signal that
	 * a drag is in progress.
	 * @since jEdit 4.2pre5
	 */
	public void setDragInProgress(boolean dndInProgress)
	{
		this.dndInProgress = dndInProgress;
	} //}}}

	//{{{ isDragEnabled() method
	/**
	 * Returns if drag and drop of text is enabled.
	 * @since jEdit 4.2pre5
	 */
	public boolean isDragEnabled()
	{
		return dndEnabled;
	} //}}}

	//{{{ setDragEnabled() method
	/**
	 * Sets if drag and drop of text is enabled.
	 * @since jEdit 4.2pre5
	 */
	public void setDragEnabled(boolean dndEnabled)
	{
		this.dndEnabled = dndEnabled;
	} //}}}

	//}}}

	//{{{ Scrolling

	//{{{ Debugging code
	/* public void scrollTest(boolean paint)
	{
		Image im = painter.createImage(painter.getWidth(),painter.getHeight());
		Graphics gfx = im.getGraphics();
		gfx.setFont(painter.getFont());
		gfx.setColor(painter.getForeground());
		gfx.clipRect(0,0,painter.getWidth(),painter.getHeight());
		long start = System.currentTimeMillis();
		for(int i = 0; i < displayManager.getScrollLineCount(); i++)
		{
			setFirstLine(i);
			if(!paint)
				chunkCache.getLineInfo(visibleLines - 1);
			else
				painter.paintComponent(gfx);
		}
		System.err.println(System.currentTimeMillis() - start);
	} */ //}}}

	//{{{ getFirstLine() method
	/**
	 * Returns the vertical scroll bar position.
	 * @since jEdit 4.2pre1
	 */
	public final int getFirstLine()
	{
		return displayManager.firstLine.scrollLine
			+ displayManager.firstLine.skew;
	} //}}}

	//{{{ setFirstLine() method
	public Exception trace;
	/**
	 * Sets the vertical scroll bar position
	 *
	 * @param firstLine The scroll bar position
	 */
	public void setFirstLine(int firstLine)
	{
		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"setFirstLine() from "
				+ getFirstLine() + " to " + firstLine);
		}

		//{{{ ensure we don't have empty space at the bottom or top, etc
		int max = displayManager.getScrollLineCount() - visibleLines
			+ (lastLinePartial ? 1 : 0);
		if(firstLine > max)
			firstLine = max;
		if(firstLine < 0)
			firstLine = 0;
		//}}}

		int oldFirstLine = getFirstLine();
		if(firstLine == oldFirstLine)
			return;

		trace = new Exception();

		if(firstLine >= oldFirstLine + visibleLines)
		{
			displayManager.firstLine.scrollDown(firstLine - oldFirstLine);
			chunkCache.invalidateAll();
		}
		else if(firstLine <= oldFirstLine - visibleLines)
		{
			displayManager.firstLine.scrollUp(oldFirstLine - firstLine);
			chunkCache.invalidateAll();
		}
		else if(firstLine > oldFirstLine)
		{
			displayManager.firstLine.scrollDown(firstLine - oldFirstLine);
			chunkCache.scrollDown(firstLine - oldFirstLine);
		}
		else if(firstLine < oldFirstLine)
		{
			displayManager.firstLine.scrollUp(oldFirstLine - firstLine);
			chunkCache.scrollUp(oldFirstLine - firstLine);
		}

		// we have to be careful
		displayManager._notifyScreenLineChanges();

		maxHorizontalScrollWidth = 0;

		//if(buffer.isLoaded())
		//	recalculateLastPhysicalLine();

		repaint();

		fireScrollEvent(true);
	} //}}}

	//{{{ getFirstPhysicalLine() method
	/**
	 * Returns the first visible physical line index.
	 * @since jEdit 4.0pre4
	 */
	public final int getFirstPhysicalLine()
	{
		return displayManager.firstLine.physicalLine;
	} //}}}

	//{{{ setFirstPhysicalLine() method
	/**
	 * Sets the vertical scroll bar position.
	 * @param physFirstLine The first physical line to display
	 * @param skew A local screen line delta
	 * @since jEdit 4.2pre1
	 */
	public void setFirstPhysicalLine(int physFirstLine)
	{
		setFirstPhysicalLine(physFirstLine,0);
	} //}}}

	//{{{ setFirstPhysicalLine() method
	/**
	 * Sets the vertical scroll bar position.
	 * @param physFirstLine The first physical line to display
	 * @param skew A local screen line delta
	 * @since jEdit 4.2pre1
	 */
	public void setFirstPhysicalLine(int physFirstLine, int skew)
	{
		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"setFirstPhysicalLine("
				+ physFirstLine + "," + skew + ")");
		}

		//{{{ ensure we don't have empty space at the bottom or top, etc
		/* int screenLineCount = 0;
		int physicalLine = displayManager.getLastVisibleLine();
		int visibleLines = this.visibleLines - (lastLinePartial ? 1 : 0);
		for(;;)
		{
			screenLineCount += displayManager.getScreenLineCount(physicalLine);
			if(screenLineCount >= visibleLines)
				break;
			int prevLine = displayManager.getPrevVisibleLine(physicalLine);
			if(prevLine == -1)
				break;
			physicalLine = prevLine;
		}

		if(physFirstLine > physicalLine)
			physFirstLine = physicalLine; */
		//}}}

		int amount = (physFirstLine - displayManager.firstLine.physicalLine);

		int oldFirstLine = getFirstLine();

		if(amount == 0)
		{
			skew -= displayManager.firstLine.skew;

			// JEditTextArea.scrollTo() needs this to simplify
			// its code
			if(skew < 0)
				displayManager.firstLine.scrollUp(-skew);
			else if(skew > 0)
				displayManager.firstLine.scrollDown(skew);
			else
			{
				// nothing to do
				return;
			}
		}
		else if(amount > 0)
			displayManager.firstLine.physDown(amount,skew);
		else if(amount < 0)
			displayManager.firstLine.physUp(-amount,skew);

		int firstLine = getFirstLine();

		if(firstLine == oldFirstLine)
			/* do nothing */;
		else if(firstLine >= oldFirstLine + visibleLines
			|| firstLine <= oldFirstLine - visibleLines)
		{
			chunkCache.invalidateAll();
		}
		else if(firstLine > oldFirstLine)
		{
			chunkCache.scrollDown(firstLine - oldFirstLine);
		}
		else if(firstLine < oldFirstLine)
		{
			chunkCache.scrollUp(oldFirstLine - firstLine);
		}

		// we have to be careful
		displayManager._notifyScreenLineChanges();

		maxHorizontalScrollWidth = 0;

		//if(buffer.isLoaded())
		//	recalculateLastPhysicalLine();

		repaint();

		fireScrollEvent(true);
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

	//{{{ getVisibleLines() method
	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines()
	{
		return visibleLines;
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
		if(horizontalOffset > 0)
			horizontalOffset = 0;

		if(horizontalOffset == this.horizontalOffset)
			return;

		this.horizontalOffset = horizontalOffset;
		if(horizontalOffset != horizontal.getValue())
			updateScrollBars();
		painter.repaint();

		fireScrollEvent(false);
	} //}}}

	//{{{ scrollUpLine() method
	/**
	 * Scrolls up by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpLine()
	{
		setFirstLine(getFirstLine() - 1);
	} //}}}

	//{{{ scrollUpPage() method
	/**
	 * Scrolls up by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpPage()
	{
		setFirstLine(getFirstLine() - getVisibleLines()
			+ (lastLinePartial ? 1 : 0));
	} //}}}

	//{{{ scrollDownLine() method
	/**
	 * Scrolls down by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownLine()
	{
		setFirstLine(getFirstLine() + 1);
	} //}}}

	//{{{ scrollDownPage() method
	/**
	 * Scrolls down by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownPage()
	{
		setFirstLine(getFirstLine() + getVisibleLines()
			- (lastLinePartial ? 1 : 0));
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
	 * @param offset The offset from the start of the buffer
	 * @param doElectricScroll If true, electric scrolling will be performed
	 * @since jEdit 4.2pre3
	 */
	public void scrollTo(int offset, boolean doElectricScroll)
	{
		int line = buffer.getLineOfOffset(offset);
		scrollTo(line,offset - buffer.getLineStartOffset(line),
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
		if(Debug.SCROLL_TO_DEBUG)
			Log.log(Log.DEBUG,this,"scrollTo(), lineCount="
				+ getLineCount());

		//{{{ Get ready
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
			&& visibleLines - 1 > electricScroll * 2
			? electricScroll : 0); //}}}

		if(visibleLines == 0)
		{
			if(Debug.SCROLL_TO_DEBUG)
				Log.log(Log.DEBUG,this,"visibleLines == 0");
			setFirstPhysicalLine(line,_electricScroll);
			// it will figure itself out after being added...
			return;
		}

		//{{{ Scroll vertically
		int firstLine = getFirstLine();
		int screenLine = chunkCache.getScreenLineOfOffset(line,offset);
		int visibleLines = getVisibleLines();
		if(screenLine == -1)
		{
			if(Debug.SCROLL_TO_DEBUG)
				Log.log(Log.DEBUG,this,"screenLine == -1");
			ChunkCache.LineInfo[] infos = chunkCache
				.getLineInfosForPhysicalLine(line);
			int subregion = chunkCache.getSubregionOfOffset(
				offset,infos);
			int prevLine = displayManager.getPrevVisibleLine(getFirstPhysicalLine());
			int nextLine = displayManager.getNextVisibleLine(getLastPhysicalLine());
			if(line == getFirstPhysicalLine())
			{
				if(Debug.SCROLL_TO_DEBUG)
					Log.log(Log.DEBUG,this,line + " == " + getFirstPhysicalLine());
				setFirstPhysicalLine(line,subregion
					- _electricScroll);
			}
			else if(line == prevLine)
			{
				if(Debug.SCROLL_TO_DEBUG)
					Log.log(Log.DEBUG,this,line + " == " + prevLine);
				setFirstPhysicalLine(prevLine,subregion
					- _electricScroll);
			}
			else if(line == getLastPhysicalLine())
			{
				if(Debug.SCROLL_TO_DEBUG)
					Log.log(Log.DEBUG,this,line + " == " + getLastPhysicalLine());
				setFirstPhysicalLine(line,
					subregion + _electricScroll
					- visibleLines
					+ (lastLinePartial ? 2 : 1));
			}
			else if(line == nextLine)
			{
				if(Debug.SCROLL_TO_DEBUG)
					Log.log(Log.DEBUG,this,line + " == " + nextLine);
				setFirstPhysicalLine(nextLine,
					subregion + electricScroll
					- visibleLines
					+ (lastLinePartial ? 2 : 1));
			}
			else
			{
				if(Debug.SCROLL_TO_DEBUG)
				{
					Log.log(Log.DEBUG,this,"neither");
					Log.log(Log.DEBUG,this,"Last physical line is " + getLastPhysicalLine());
				}
				setFirstPhysicalLine(line,subregion
					- visibleLines / 2);
				if(Debug.SCROLL_TO_DEBUG)
				{
					Log.log(Log.DEBUG,this,"Last physical line is " + getLastPhysicalLine());
				}
			}
		}
		else if(screenLine < _electricScroll)
		{
			if(Debug.SCROLL_TO_DEBUG)
				Log.log(Log.DEBUG,this,"electric up");
			setFirstLine(getFirstLine() - _electricScroll + screenLine);
		}
		else if(screenLine > visibleLines - _electricScroll
			- (lastLinePartial ? 2 : 1))
		{
			if(Debug.SCROLL_TO_DEBUG)
				Log.log(Log.DEBUG,this,"electric down");
			setFirstLine(getFirstLine() + _electricScroll - visibleLines + screenLine + (lastLinePartial ? 2 : 1));
		} //}}}

		//{{{ Scroll horizontally
		if(!displayManager.isLineVisible(line))
			return;

		Point point = offsetToXY(line,offset,returnValue);
		if(point == null)
		{
			Log.log(Log.ERROR,this,"BUG: screenLine=" + screenLine
				+ ",visibleLines=" + visibleLines
				+ ",physicalLine=" + line
				+ ",firstPhysicalLine=" + getFirstPhysicalLine()
				+ ",lastPhysicalLine=" + getLastPhysicalLine());
		}

		point.x += extraEndVirt;

		if(point.x < 0)
		{
			setHorizontalOffset(horizontalOffset
				- point.x + charWidth + 5);
		}
		else if(point.x >= painter.getWidth() - charWidth - 5)
		{
			setHorizontalOffset(horizontalOffset +
				(painter.getWidth() - point.x)
				- charWidth - 5);
		} //}}}
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
		return chunkCache.getLineInfo(screenLine).physicalLine;
	} //}}}

	//{{{ getScreenLineOfOffset() method
	/**
	 * Returns the screen (wrapped) line containing the specified offset.
	 * Returns -1 if the line is not currently visible on the screen.
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

		if(line < 0 || line >= visibleLines)
			return -1;

		return xToScreenLineOffset(line,x,round);
	} //}}}

	//{{{ xToScreenLineOffset() method
	/**
	 * Converts a point in a given screen line to an offset.
	 * Note that unlike in previous jEdit versions, this method now returns
	 * -1 if the y co-ordinate is out of bounds.
	 *
	 * @param x The x co-ordinate of the point
	 * @param screenLine The screen line
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 3.2pre6
	 */
	public int xToScreenLineOffset(int screenLine, int x, boolean round)
	{
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(screenLine);
		if(lineInfo.physicalLine == -1)
		{
			return getLineEndOffset(displayManager
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
		if(!displayManager.isLineVisible(line))
			return null;
		int screenLine = chunkCache.getScreenLineOfOffset(line,offset);
		if(screenLine == -1)
			return null;

		FontMetrics fm = painter.getFontMetrics();

		retVal.y = screenLine * fm.getHeight();

		ChunkCache.LineInfo info = chunkCache.getLineInfo(screenLine);

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
		if(!buffer.isLoaded())
			return;

		//if(start != end)
		//	System.err.println(start + ":" + end + ":" + chunkCache.needFullRepaint());

		if(start > end)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		if(chunkCache.needFullRepaint())
			end = visibleLines;

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
		if(!isShowing()
			|| !buffer.isLoaded()
			|| line < getFirstPhysicalLine()
			|| line > physLastLine
			|| !displayManager.isLineVisible(line))
			return;

		int startLine = -1;
		int endLine = -1;

		for(int i = 0; i < visibleLines; i++)
		{
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

		if(chunkCache.needFullRepaint() || endLine == -1)
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
		if(!isShowing() || !buffer.isLoaded())
			return;

		if(end < start)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		if(end < getFirstPhysicalLine() || start > getLastPhysicalLine())
			return;

		int startScreenLine = -1;
		int endScreenLine = -1;

		for(int i = 0; i < visibleLines; i++)
		{
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

		if(chunkCache.needFullRepaint() || endScreenLine == -1)
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
			getToolkit().beep();
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
	 * corresponding bracket.
	 * @since jEdit 4.2pre1
	 */
	public Selection selectToMatchingBracket(int position,
		boolean quickCopy)
	{
		int positionLine = buffer.getLineOfOffset(position);
		int lineOffset = position - buffer.getLineStartOffset(positionLine);
		if(getLineLength(positionLine) != 0)
		{
			int bracket = TextUtilities.findMatchingBracket(buffer,
				positionLine,Math.max(0,lineOffset - 1));

			if(bracket != -1)
			{
				Selection s;

				if(bracket < position)
				{
					if(!quickCopy)
						moveCaretPosition(position,false);
					s = new Selection.Range(bracket,position);
				}
				else
				{
					if(!quickCopy)
						moveCaretPosition(bracket + 1,false);
					s = new Selection.Range(position - 1,bracket + 1);
				}

				if(!multi && !quickCopy)
					selectNone();

				addToSelection(s);
				return s;
			}
		}

		return null;
	} //}}}

	//{{{ selectToMatchingBracket() method
	/**
	 * Selects from the bracket at the caret position to the corresponding
	 * bracket.
	 * @since jEdit 4.0pre2
	 */
	public void selectToMatchingBracket()
	{
		selectToMatchingBracket(caret,false);
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
			getToolkit().beep();
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

	//{{{ lineInStructureScope() method
	/**
	 * Returns if the specified line is contained in the currently
	 * matched structure's scope.
	 * @since jEdit 4.2pre3
	 */
	public boolean lineInStructureScope(int line)
	{
		if(match == null)
			return false;

		if(match.startLine < caretLine)
			return (line >= match.startLine && line <= caretLine);
		else
			return (line <= match.endLine && line >= caretLine);
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

	//{{{ removeFromSelection() method
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
	} //}}}

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
	 * @since jEdit 3.2pre1
	 */
	public void extendSelection(int offset, int end)
	{
		extendSelection(offset,end,0,0);
	} //}}}

	//{{{ extendSelection() method
	/**
	 * Extends the selection at the specified offset, or creates a new
	 * one if there is no selection at the specified offset. This is
	 * different from resizing in that the new chunk is added to the
	 * selection in question, instead of replacing it.
	 * @param offset The offset
	 * @param end The new selection end
	 * @param extraStartVirt Extra virtual space at the start
	 * @param extraEndVirt Extra virtual space at the end
	 * @since jEdit 4.2pre1
	 */
	public void extendSelection(int offset, int end,
		int extraStartVirt, int extraEndVirt)
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

		if(rectangularSelectionMode)
		{
			s = new Selection.Rect(offset,end);
			((Selection.Rect)s).extraStartVirt = extraStartVirt;
			((Selection.Rect)s).extraEndVirt = extraEndVirt;
		}
		else
			s = new Selection.Range(offset,end);

		_addToSelection(s);
		fireCaretEvent();

		if(rectangularSelectionMode && extraEndVirt != 0)
		{
			int line = getLineOfOffset(end);
			scrollTo(line,getLineLength(line) + extraEndVirt,false);
		}
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
		setSelectedText(selectedText,true);
	} //}}}

	//{{{ setSelectedText() method
	/**
	 * Replaces the selection at the caret with the specified text.
	 * If there is no selection at the caret, the text is inserted at
	 * the caret position.
	 * @param selectedText The new selection
	 * @param moveCaret Move caret to insertion location if necessary
	 * @since jEdit 4.2pre5
	 */
	public void setSelectedText(String selectedText, boolean moveCaret)
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

				if(moveCaret)
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

		Set set = new TreeSet();
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			int endLine = (s.end == getLineStartOffset(s.endLine)
				? s.endLine - 1
				: s.endLine);

			for(int j = s.startLine; j <= endLine; j++)
			{
				line = new Integer(j);
				set.add(line);
			}
		}

		int[] returnValue = new int[set.size()];
		int i = 0;

		Iterator iter = set.iterator();
		while(iter.hasNext())
		{
			line = (Integer)iter.next();
			returnValue[i++] = line.intValue();
		}

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

	//{{{ addStructureMatcher() method
	/**
	 * Adds a structure matcher.
	 * @since jEdit 4.2pre3
	 */
	public void addStructureMatcher(StructureMatcher matcher)
	{
		structureMatchers.add(matcher);
	} //}}}

	//{{{ removeStructureMatcher() method
	/**
	 * Removes a structure matcher.
	 * @since jEdit 4.2pre3
	 */
	public void removeStructureMatcher(StructureMatcher matcher)
	{
		structureMatchers.remove(matcher);
	} //}}}

	//{{{ getStructureMatch() method
	/**
	 * Returns the structure element (bracket, or XML tag, etc) matching the
	 * one before the caret.
	 * @since jEdit 4.2pre3
	 */
	public StructureMatcher.Match getStructureMatch()
	{
		return match;
	} //}}}

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
		moveCaretPosition(newCaret,doElectricScroll ? ELECTRIC_SCROLL
			: NORMAL_SCROLL);
	} //}}}

	//{{{ moveCaretPosition() method
	public static int NO_SCROLL = 0;
	public static int NORMAL_SCROLL = 1;
	public static int ELECTRIC_SCROLL = 2;
	/**
	 * Sets the caret position without deactivating the selection.
	 * @param caret The caret position
	 * @param scrollMode The scroll mode (NO_SCROLL, NORMAL_SCROLL, or
	 * ELECTRIC_SCROLL).
	 * @since jEdit 4.2pre1
	 */
	public void moveCaretPosition(int newCaret, int scrollMode)
	{
		if(newCaret < 0 || newCaret > buffer.getLength())
		{
			throw new IllegalArgumentException("caret out of bounds: "
				+ newCaret);
		}

		if(match != null)
		{
			if(caretLine < match.startLine)
				invalidateLineRange(caretLine,match.endLine);
			else
				invalidateLineRange(match.startLine,caretLine);
			match = null;
		}

		if(caret == newCaret)
		{
			if(scrollMode == NORMAL_SCROLL)
				finishCaretUpdate(false,false);
			else if(scrollMode == ELECTRIC_SCROLL)
				finishCaretUpdate(true,false);
		}
		else
		{
			int newCaretLine = getLineOfOffset(newCaret);

			magicCaret = -1;

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

			if(scrollMode == NORMAL_SCROLL)
				finishCaretUpdate(false,true);
			else if(scrollMode == ELECTRIC_SCROLL)
				finishCaretUpdate(true,true);
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
	 * Returns an internal position used to keep the caret in one
	 * column while moving around lines of varying lengths.
	 * @since jEdit 4.2pre1
	 */
	public int getMagicCaretPosition()
	{
		if(magicCaret == -1)
		{
			magicCaret = chunkCache.subregionOffsetToX(
				caretLine,caret - getLineStartOffset(caretLine));
		}

		return magicCaret;
	} //}}}

	//{{{ setMagicCaretPosition() method
	/**
	 * Sets the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 * @param magicCaret The magic caret position
	 * @since jEdit 4.2pre1
	 */
	public void setMagicCaretPosition(int magicCaret)
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
		Selection s = getSelectionAtOffset(caret);

		if(!select && s instanceof Selection.Range)
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

		int extraStartVirt, extraEndVirt;
		if(s instanceof Selection.Rect)
		{
			extraStartVirt = ((Selection.Rect)s).extraStartVirt;
			extraEndVirt = ((Selection.Rect)s).extraEndVirt;
		}
		else
		{
			extraStartVirt = 0;
			extraEndVirt = 0;
		}

		int newCaret = caret;

		if(caret == buffer.getLength())
		{
			if(select && (rectangularSelectionMode || s instanceof Selection.Rect))
			{
				if(s != null && caret == s.start)
					extraStartVirt++;
				else
					extraEndVirt++;
			}
			else
			{
				getToolkit().beep();
				return;
			}
		}
		else if(caret == getLineEndOffset(caretLine) - 1)
		{
			if(select && (rectangularSelectionMode || s instanceof Selection.Rect))
			{
				if(s != null && caret == s.start)
					extraStartVirt++;
				else
					extraEndVirt++;
			}
			else
			{
				int line = displayManager.getNextVisibleLine(caretLine);
				if(line == -1)
				{
					getToolkit().beep();
					return;
				}
				else
					newCaret = getLineStartOffset(line);
			}
		}
		else
			newCaret = caret + 1;

		if(select)
			extendSelection(caret,newCaret,extraStartVirt,extraEndVirt);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToNextLine() method
	/**
	 * Move the caret to the next line.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		boolean rectSelect = (s == null ? rectangularSelectionMode
			: s instanceof Selection.Rect);
		int magic = getMagicCaretPosition();
		int newCaret = chunkCache.getBelowPosition(caretLine,
			caret - buffer.getLineStartOffset(caretLine),magic + 1,
			rectSelect && select);
		if(newCaret == -1)
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

		if(select)
		{
			RectParams params = getRectParams(caret,newCaret);
			int extraStartVirt;
			int extraEndVirt;
			if(params == null)
			{
				extraStartVirt = 0;
				extraEndVirt = 0;
			}
			else
			{
				extraStartVirt = params.extraStartVirt;
				extraEndVirt = params.extraEndVirt;
				newCaret = params.newCaret;
			}
			extendSelection(caret,newCaret,extraStartVirt,extraEndVirt);
		}
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);

		setMagicCaretPosition(magic);
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
		scrollToCaret(false);
		int magic = getMagicCaretPosition();
		if(caretLine < displayManager.getFirstVisibleLine())
		{
			caretLine = displayManager.getNextVisibleLine(
				caretLine);
		}

		int caretScreenLine = getScreenLineOfOffset(caret);

		scrollDownPage();

		int newCaret = xToScreenLineOffset(caretScreenLine,magic,true);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret,false);

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
			if(!displayManager.isLineVisible(i))
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
	 * Note that if the "view.eatWhitespace" boolean propery is false,
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
	 * @since jEdit 4.1pre5
	 */
	public void goToNextWord(boolean select, boolean eatWhitespace)
	{
		int lineStart = getLineStartOffset(caretLine);
		int newCaret = caret - lineStart;
		String lineText = getLineText(caretLine);

		if(newCaret == lineText.length())
		{
			int nextLine = displayManager.getNextVisibleLine(caretLine);
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
			newCaret = TextUtilities.findWordEnd(lineText,
				newCaret + 1,noWordSep,true,eatWhitespace);

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
		Selection s = getSelectionAtOffset(caret);

		if(!select && s instanceof Selection.Range)
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

		int extraStartVirt = 0;
		int extraEndVirt = 0;
		int newCaret = caret;

		if(select && caret == getLineEndOffset(caretLine) - 1)
		{
			if(s instanceof Selection.Rect)
			{
				extraStartVirt = ((Selection.Rect)s).extraStartVirt;
				extraEndVirt = ((Selection.Rect)s).extraEndVirt;
				if(caret == s.start)
				{
					if(extraStartVirt == 0)
						newCaret = caret - 1;
					else
						extraStartVirt--;
				}
				else
				{
					if(extraEndVirt == 0)
						newCaret = caret - 1;
					else
						extraEndVirt--;
				}
			}
			else
				newCaret = caret - 1;
		}
		else if(caret == getLineStartOffset(caretLine))
		{
			int line = displayManager.getPrevVisibleLine(caretLine);
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
			extendSelection(caret,newCaret,extraStartVirt,extraEndVirt);
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
		Selection s = getSelectionAtOffset(caret);
		boolean rectSelect = (s == null ? rectangularSelectionMode
			: s instanceof Selection.Rect);
		int magic = getMagicCaretPosition();

		int newCaret = chunkCache.getAbovePosition(caretLine,
			caret - buffer.getLineStartOffset(caretLine),magic + 1,
			rectSelect && select);
		if(newCaret == -1)
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

		if(select)
		{
			RectParams params = getRectParams(caret,newCaret);
			int extraStartVirt;
			int extraEndVirt;
			if(params == null)
			{
				extraStartVirt = 0;
				extraEndVirt = 0;
			}
			else
			{
				extraStartVirt = params.extraStartVirt;
				extraEndVirt = params.extraEndVirt;
				newCaret = params.newCaret;
			}
			extendSelection(caret,newCaret,extraStartVirt,extraEndVirt);
		}
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);

		setMagicCaretPosition(magic);
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
		scrollToCaret(false);
		int magic = getMagicCaretPosition();

		if(caretLine < displayManager.getFirstVisibleLine())
		{
			caretLine = displayManager.getNextVisibleLine(
				caretLine);
		}

		int caretScreenLine = getScreenLineOfOffset(caret);

		scrollUpPage();

		int newCaret = xToScreenLineOffset(caretScreenLine,magic,true);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret,false);

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
			if(!displayManager.isLineVisible(i))
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
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevWord(boolean select)
	{
		goToPrevWord(select,false);
	} //}}}

	//{{{ goToPrevWord() method
	/**
	 * Moves the caret to the start of the previous word.
	 * @since jEdit 4.1pre5
	 */
	public void goToPrevWord(boolean select, boolean eatWhitespace)
	{
		int lineStart = getLineStartOffset(caretLine);
		int newCaret = caret - lineStart;
		String lineText = getLineText(caretLine);

		if(newCaret == 0)
		{
			if(lineStart == 0)
			{
				getToolkit().beep();
				return;
			}
			else
			{
				int prevLine = displayManager.getPrevVisibleLine(caretLine);
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
			newCaret = TextUtilities.findWordStart(lineText,
				newCaret - 1,noWordSep,true,eatWhitespace);

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

		int firstIndent = chunkCache.getSubregionStartOffset(line,offset);
		if(firstIndent == getLineStartOffset(line))
		{
			firstIndent = MiscUtilities.getLeadingWhiteSpace(getLineText(line));
			if(firstIndent == getLineLength(line))
				firstIndent = 0;
			firstIndent += getLineStartOffset(line);
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

		int lastIndent = chunkCache.getSubregionEndOffset(line,offset);

		if(lastIndent == getLineEndOffset(line))
		{
			lastIndent = getLineLength(line) - MiscUtilities.getTrailingWhiteSpace(getLineText(line));
			if(lastIndent == 0)
				lastIndent = getLineLength(line);
			lastIndent += getLineStartOffset(line);
		}
		else
		{
			lastIndent--;
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
		int firstVisibleLine = getFirstLine() == 0 ? 0 : electricScroll;
		int firstVisible = getScreenLineStartOffset(firstVisibleLine);
		if(firstVisible == -1)
		{
			firstVisible = getLineStartOffset(displayManager
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

		if(getFirstLine() + visibleLines >=
			displayManager.getScrollLineCount())
		{
			lastVisible = getLineEndOffset(displayManager
				.getLastVisibleLine()) - 1;
		}
		else
		{
			lastVisible = visibleLines - electricScroll - 1;
			if(lastLinePartial)
				lastVisible--;
			if(lastVisible < 0)
				lastVisible = 0;
			lastVisible = getScreenLineEndOffset(lastVisible) - 1;
			if(lastVisible == -1)
			{
				lastVisible = getLineEndOffset(displayManager
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
			displayManager.getFirstVisibleLine());
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
			displayManager.getLastVisibleLine()) - 1;
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

			int bracket = TextUtilities.findMatchingBracket(
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
				if(sel instanceof Selection.Rect ||
					(sel.startLine == sel.endLine
					&& (sel.start != buffer.getLineStartOffset(sel.startLine)
					|| sel.end != buffer.getLineEndOffset(sel.startLine) - 1)))
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
			Selection[] selection = getSelection();
			if(selection.length != 0)
			{
				for(int i = 0; i < selection.length; i++)
				{
					Selection s = selection[i];
					setSelectedText(s,str);
					/* if(s instanceof Selection.Rect)
					{
						addToSelection(new Selection.Rect(
							s.start + 1,s.end + 1));
					} */
				}
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
					buffer.indentLine(caretLine,true);
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
		backspaceWord(false);
	} //}}}

	//{{{ backspaceWord() method
	/**
	 * Deletes the word before the caret.
	 * @param eatWhitespace If true, will eat whitespace
	 * @since jEdit 4.2pre5
	 */
	public void backspaceWord(boolean eatWhitespace)
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
			_caret = TextUtilities.findWordStart(lineText,_caret-1,
				noWordSep,true,eatWhitespace);
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

		int x = chunkCache.subregionOffsetToX(caretLine,caret - start);

		// otherwise a bunch of consecutive C+d's would be merged
		// into one edit
		try
		{
			if(end > buffer.getLength())
			{
				if(start != 0)
					start--;
				end--;
			}
			buffer.beginCompoundEdit();
			buffer.remove(start,end - start);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		int lastLine = displayManager.getLastVisibleLine();
		if(caretLine == lastLine)
		{
			int offset = chunkCache.xToSubregionOffset(lastLine,0,x,true);
			setCaretPosition(buffer.getLineStartOffset(lastLine)
				+ offset);
		}
		else
		{
			int offset = chunkCache.xToSubregionOffset(caretLine,0,x,true);
			setCaretPosition(start + offset);
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
			//if(!displayManager.isLineVisible(i))
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
			//if(!displayManager.isLineVisible(i))
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
		deleteWord(false);
	} //}}}

	//{{{ deleteWord() method
	/**
	 * Deletes the word in front of the caret
.	 * @param eatWhitespace If true, will eat whitespace
	 * @since jEdit 4.2pre5
	 */
	public void deleteWord(boolean eatWhitespace)
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
				_caret+1,noWordSep,true,eatWhitespace);
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
		painter.repaint();
	} //}}}

	//{{{ isRectangularSelectionEnabled() method
	/**
	 * Returns if rectangular selection is enabled.
	 * @since jEdit 4.2pre1
	 */
	public final boolean isRectangularSelectionEnabled()
	{
		return rectangularSelectionMode;
	} //}}}

	//{{{ toggleRectangularSelectionEnabled() method
	/**
	 * Toggles rectangular selection.
	 * @since jEdit 4.2pre1
	 */
	public final void toggleRectangularSelectionEnabled()
	{
		setRectangularSelectionEnabled(!rectangularSelectionMode);
		if(view.getStatus() != null)
		{
			view.getStatus().setMessageAndClear(
				jEdit.getProperty("view.status.rect-select-changed",
				new Integer[] { new Integer(rectangularSelectionMode ? 1 : 0) }));
		}
	} //}}}

	//{{{ setRectangularSelectionEnabled() method
	/**
	 * Set rectangular selection on or off according to the value of
	 * <code>rectangularSelectionMode</code>. This only affects the ability
	 * to make multiple selections from the keyboard. A rectangular selection
	 * can always be created by dragging with the mouse by holding down
	 * <b>Control</b>, regardless of the state of this flag.
	 *
	 * @param multi Should rectangular selection be enabled?
	 * @since jEdit 4.2pre1
	 */
	public final void setRectangularSelectionEnabled(boolean rectangularSelectionMode)
	{
		this.rectangularSelectionMode = rectangularSelectionMode;
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
		painter.repaint();
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
			+ chunkCache.xToSubregionOffset(line,0,magic + 1,true);
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
				&& displayManager.isLineVisible(i))
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
			+ chunkCache.xToSubregionOffset(nextFold,0,magic + 1,true);
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
				&& displayManager.isLineVisible(i))
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
			+ chunkCache.xToSubregionOffset(prevFold,0,magic + 1,true);
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	} //}}}

	//{{{ collapseFold() method
	/**
	 * Like {@link DisplayManager#collapseFold(int)}, but
	 * also moves the caret to the first line of the fold.
	 * @since jEdit 4.0pre3
	 */
	public void collapseFold()
	{
		int x = chunkCache.subregionOffsetToX(caretLine,
			caret - getLineStartOffset(caretLine));

		displayManager.collapseFold(caretLine);

		if(displayManager.isLineVisible(caretLine))
			return;

		int line = displayManager.getPrevVisibleLine(caretLine);

		if(!multi)
			selectNone();
		moveCaretPosition(buffer.getLineStartOffset(line)
			+ chunkCache.xToSubregionOffset(line,0,x,true));
	} //}}}

	//{{{ expandFold() method
	/**
	 * Like {@link DisplayManager#expandFold(int,boolean)}, but
	 * also moves the caret to the first sub-fold.
	 * @since jEdit 4.0pre3
	 */
	public void expandFold(boolean fully)
	{
		int x = chunkCache.subregionOffsetToX(caretLine,
			caret - getLineStartOffset(caretLine));

		int line = displayManager.expandFold(caretLine,fully);

		if(!fully && line != -1)
		{
			if(!multi)
				selectNone();
			moveCaretPosition(getLineStartOffset(line)
				+ chunkCache.xToSubregionOffset(line,0,x,true));
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
			displayManager.narrow(lines[0],lines[1]);
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
		displayManager.narrow(sel.getStartLine(),sel.getEndLine());

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
					if(s.end == buffer.getLineStartOffset(
						s.endLine))
					{
						buffer.insert(s.end,end);
					}
					else
					{
						buffer.insert(s.end," " + end);
					}
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
			GUIUtilities.error(view,"format-maxlinelen",null);
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
				buffer.indentLine(caretLine,true);
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
				&& buffer.indentLine(caretLine,false))
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
		if(!buffer.isEditable() || end > buffer.getLength())
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

	//{{{ addLeftOfScrollBar() method
	/**
	 * Adds a component to the box left of the vertical scroll bar. The
	 * ErrorList plugin uses this to show a global error overview, for
	 * example.
	 *
	 * @param comp The component
	 * @since jEdit 4.2pre1
	 */
	public void addLeftOfScrollBar(Component comp)
	{
		verticalBox.add(comp,verticalBox.getComponentCount() - 1);
	} //}}}

	//{{{ removeLeftOfScrollBar() method
	/**
	 * Removes a component from the box left of the vertical scroll bar.
	 *
	 * @param comp The component
	 * @since jEdit 4.2pre1
	 */
	public void removeLeftOfScrollBar(Component comp)
	{
		verticalBox.remove(comp);
	} //}}}

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

		recalculateVisibleLines();
		if(buffer.isLoaded())
			recalculateLastPhysicalLine();
		propertiesChanged();
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

	//{{{ getFocusCycleRoot() method
	/**
	 * Java 1.4 compatibility fix to make Tab traversal work in a sane
	 * manner.
	 * @since jEdit 4.2pre3
	 */
	public boolean getFocusCycleRoot()
	{
		return true;
	} //}}}

	//{{{ processKeyEvent() method
	public void processKeyEvent(KeyEvent evt)
	{
		view.processKeyEvent(evt,View.TEXT_AREA);

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	} //}}}

	//{{{ addTopComponent() method
	/**
	 * Adds a component above the gutter, text area, and vertical scroll bar.
	 *
	 * @since jEdit 4.2pre3
	 */
	public void addTopComponent(Component comp)
	{
		add(TOP,comp);
	} //}}}

	//{{{ removeTopComponent() method
	/**
	 * Removes a component from above the gutter, text area, and vertical scroll bar.
	 *
	 * @since jEdit 4.2pre3
	 */
	public void removeTopComponent(Component comp)
	{
		remove(comp);
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

		boolean invalidateCachedScreenLineCounts = false;

		String wrap = buffer.getStringProperty("wrap");
		if(!wrap.equals(this.wrap))
		{
			this.wrap = wrap;
			hardWrap = wrap.equals("hard");
			if(displayManager != null && !bufferChanging)
			{
				displayManager.firstLine.callReset = true;
				displayManager.scrollLineCount.callReset = true;
			}
			invalidateCachedScreenLineCounts = true;
		}

		int maxLineLen = buffer.getIntegerProperty("maxLineLen",0);
		if(maxLineLen != this.maxLineLen)
		{
			this.maxLineLen = maxLineLen;
			if(displayManager != null && !bufferChanging)
			{
				displayManager.firstLine.callReset = true;
				displayManager.scrollLineCount.callReset = true;
			}
			invalidateCachedScreenLineCounts = true;
		}

		maxHorizontalScrollWidth = 0;

		if(invalidateCachedScreenLineCounts)
			buffer.invalidateCachedScreenLineCounts();

		chunkCache.invalidateAll();

		if(displayManager != null && !bufferChanging)
		{
			displayManager.updateWrapSettings();
			displayManager._notifyScreenLineChanges();
		}

		gutter.repaint();
		painter.repaint();
	} //}}}

	//{{{ Deprecated methods

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
	DisplayManager displayManager;
	boolean bufferChanging;

	int maxHorizontalScrollWidth;
	boolean updateMaxHorizontalScrollWidth;

	String wrap;
	boolean hardWrap;
	float tabSize;
	int charWidth;
	int maxLineLen;

	boolean scrollBarsInitialized;

	// this is package-private so that the painter can use it without
	// having to call getSelection() (which involves an array copy)
	Vector selection;

	// used to store offsetToXY() results
	Point returnValue;

	boolean lastLinePartial;
	//}}}

	//{{{ isCaretVisible() method
	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	final boolean isCaretVisible()
	{
		return blink && hasFocus();
	} //}}}

	//{{{ isStructureHighlightVisible() method
	/**
	 * Returns true if the structure highlight is visible, false otherwise.
	 * @since jEdit 4.2pre3
	 */
	final boolean isStructureHighlightVisible()
	{
		return match != null
			&& hasFocus()
			&& displayManager.isLineVisible(match.startLine)
			&& displayManager.isLineVisible(match.endLine);
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
		if(lastLinePartial)
			visibleLines++;

		chunkCache.recalculateVisibleLines();
		maxHorizontalScrollWidth = 0;

		// this does the "trick" to eliminate blank space at the end
		if(displayManager != null && buffer != null && buffer.isLoaded())
			setFirstLine(getFirstLine());

		updateScrollBars();
	} //}}}

	//{{{ foldStructureChanged() method
	void foldStructureChanged()
	{
		chunkCache.invalidateAll();
		recalculateLastPhysicalLine();
		repaint();
	} //}}}

	//{{{ updateScrollBars() method
	/**
	 * Updates the state of the scroll bars. This should be called
	 * if the number of lines in the buffer changes, or when the
	 * size of the text are changes.
	 */
	void updateScrollBars()
	{
		if(buffer == null)
			return;

		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"updateScrollBars(), slc="
				+ displayManager.getScrollLineCount());

		if(vertical != null && visibleLines != 0)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"Vertical ok");
			int lineCount = displayManager.getScrollLineCount();
			int firstLine = getFirstLine();
			int visible = visibleLines - (lastLinePartial ? 1 : 0);

			vertical.setValues(firstLine,visible,0,lineCount);
			vertical.setUnitIncrement(2);
			vertical.setBlockIncrement(visible);
		}

		int width = painter.getWidth();
		if(horizontal != null && width != 0)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"Horizontal ok");
			maxHorizontalScrollWidth = 0;
			painter.repaint();

			horizontal.setUnitIncrement(painter.getFontMetrics()
				.charWidth('w'));
			horizontal.setBlockIncrement(width / 2);
		}
	} //}}}

	//{{{ _finishCaretUpdate() method
	/* called by DisplayManager.BufferChangeHandler.transactionComplete() */
	void _finishCaretUpdate()
	{
		if(!queuedCaretUpdate)
			return;

		try
		{
			// When the user is typing, etc, we don't want the caret
			// to blink
			blink = true;
			caretTimer.restart();

			if(!displayManager.isLineVisible(caretLine))
			{
				if(caretLine < displayManager.getFirstVisibleLine()
					|| caretLine > displayManager.getLastVisibleLine())
				{
					int collapseFolds = buffer.getIntegerProperty(
						"collapseFolds",0);
					if(collapseFolds != 0)
					{
						displayManager.expandFolds(collapseFolds);
						displayManager.expandFold(caretLine,false);
					}
					else
						displayManager.expandAllFolds();
				}
				else
					displayManager.expandFold(caretLine,false);
			}

			scrollToCaret(queuedScrollToElectric);
			updateBracketHighlightWithDelay();
			if(queuedFireCaretEvent)
				fireCaretEvent();
		}
		// in case one of the above fails, we still want to
		// clear these flags.
		finally
		{
			queuedCaretUpdate = queuedScrollToElectric
				= queuedFireCaretEvent = false;
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static final String CENTER = "center";
	private static final String RIGHT = "right";
	private static final String LEFT = "left";
	private static final String BOTTOM = "bottom";
	private static final String TOP = "top";

	private static Timer caretTimer;
	private static Timer structureTimer;
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

	private int physLastLine;
	private int screenLastLine;

	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;

	private boolean quickCopy;

	// JDiff, error list add stuff here
	private Box verticalBox;
	private JScrollBar vertical;
	private JScrollBar horizontal;

	private Buffer buffer;

	private int caret;
	private int caretLine;
	private int caretScreenLine;

	private List structureMatchers;
	private StructureMatcher.Match match;

	private int magicCaret;

	private boolean multi;
	private boolean overwrite;
	private boolean rectangularSelectionMode;

	/* on JDK 1.4, this is set to a method by Java14. The method must take
	* these parameters:
	* - a JEditTextArea
	* - an InputEvent
	* - a boolean (copy text or move, depending on modifier user held down)
	*/
	private boolean dndEnabled;
	private Method dndCallback;
	private boolean dndInProgress;

	// see finishCaretUpdate() & _finishCaretUpdate()
	private boolean queuedCaretUpdate;
	private boolean queuedScrollToElectric;
	private boolean queuedFireCaretEvent;

	//}}}

	//{{{ startDragAndDrop() method
	// calls dndCallback via reflection
	private void startDragAndDrop(InputEvent evt, boolean copy)
	{
		try
		{
			dndCallback.invoke(null,new Object[] { this, evt,
				new Boolean(copy) });
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	} //}}}

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
		this.queuedScrollToElectric |= doElectricScroll;
		this.queuedFireCaretEvent |= fireCaretEvent;

		if(queuedCaretUpdate)
			return;

		queuedCaretUpdate = true;

		if(!buffer.isTransactionInProgress())
			_finishCaretUpdate();
		/* otherwise DisplayManager.BufferChangeHandler calls */
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
				buffer.indentLine(caretLine,true);
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

	//{{{ updateStructureHighlightWithDelay() method
	private void updateBracketHighlightWithDelay()
	{
		structureTimer.stop();
		structureTimer.start();
	} //}}}

	//{{{ updateStructureHighlight() method
	private void updateStructureHighlight()
	{
		if(!painter.isStructureHighlightEnabled()
			&& !gutter.isStructureHighlightEnabled())
			return;

		Iterator iter = structureMatchers.iterator();
		while(iter.hasNext())
		{
			StructureMatcher matcher = (StructureMatcher)
				iter.next();
			match = matcher.getMatch(this);
			if(match != null)
				break;
		}

		if(match != null)
		{
			if(caretLine < match.startLine)
				invalidateLineRange(caretLine,match.endLine);
			else
				invalidateLineRange(match.startLine,caretLine);

			if(!displayManager.isLineVisible(match.startLine)
				|| chunkCache.getScreenLineOfOffset(
				match.startLine,match.start - getLineStartOffset(match.startLine))
				== -1)
			{
				showStructureStatusMessage(match.startLine < caretLine);
			}
		}
	} //}}}

	//{{{ showStructureStatusMessage() method
	private void showStructureStatusMessage(boolean backward)
	{
		String text = buffer.getLineText(match.startLine).trim();
		if(backward && match.startLine != 0 && text.length() == 1)
		{
			switch(text.charAt(0))
			{
			case '{': case '}':
			case '[': case ']':
			case '(': case ')':
				text = buffer.getLineText(match.startLine - 1)
					.trim() + " " + text;
				break;
			}
		}

		// get rid of embedded tabs not removed by trim()
		text = text.replace('\t',' ');

		view.getStatus().setMessageAndClear(jEdit.getProperty(
			"view.status.bracket",new Object[] { 
			new Integer(match.startLine + 1), text }));
	} //}}}

	//{{{ recalculateLastPhysicalLine() method
	void recalculateLastPhysicalLine()
	{
		int oldScreenLastLine = screenLastLine;
		for(int i = visibleLines - 1; i >= 0; i--)
		{
			ChunkCache.LineInfo info = chunkCache.getLineInfo(i);
			if(info.physicalLine != -1)
			{
				physLastLine = info.physicalLine;
				screenLastLine = i;
				break;
			}
		}
		invalidateScreenLineRange(oldScreenLastLine,screenLastLine);
	} //}}}

	//{{{ getRectParams() method
	static class RectParams
	{
		int extraStartVirt;
		int extraEndVirt;
		int newCaret;

		RectParams(int extraStartVirt, int extraEndVirt, int newCaret)
		{
			this.extraStartVirt = extraStartVirt;
			this.extraEndVirt = extraEndVirt;
			this.newCaret = newCaret;
		}
	}

	/**
	 * Used when doing S+UP/DOWN to simplify dealing with virtual space.
	 */
	private RectParams getRectParams(int caret, int newCaret)
	{
		Selection s = getSelectionAtOffset(caret);
		int virtualWidth;
		if(s instanceof Selection.Rect)
		{
			if(caret == s.end)
			{
				virtualWidth = buffer.getVirtualWidth(
					s.endLine,s.end - getLineStartOffset(
					s.endLine)) + ((Selection.Rect)s).extraEndVirt;
			}
			else
			{
				virtualWidth = buffer.getVirtualWidth(
					s.startLine,s.start - getLineStartOffset(
					s.startLine)) + ((Selection.Rect)s).extraStartVirt;
			}
		}
		else if(rectangularSelectionMode)
		{
			virtualWidth = buffer.getVirtualWidth(
				caretLine,caret - buffer.getLineStartOffset(caretLine));
		}
		else
			return null;

		int newLine = getLineOfOffset(newCaret);
		int[] totalVirtualWidth = new int[1];
		int newOffset = buffer.getOffsetOfVirtualColumn(newLine,
			virtualWidth,totalVirtualWidth);
		if(newOffset == -1)
		{
			int extraVirt = virtualWidth - totalVirtualWidth[0];
			newCaret = getLineEndOffset(newLine) - 1;
			RectParams returnValue;

			boolean bias;
			if(s == null)
				bias = (newCaret < caret);
			else if(s.start == caret)
				bias = (newCaret <= s.end);
			else if(s.end == caret)
				bias = (newCaret <= s.start);
			else
				bias = false;

			if(bias)
				returnValue = new RectParams(extraVirt,0,newCaret);
			else
				returnValue = new RectParams(0,extraVirt,newCaret);
			return returnValue;
		}
		else
		{
			return new RectParams(0,0,getLineStartOffset(newLine)
				+ newOffset);
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ TextAreaBorder class
	static class TextAreaBorder extends AbstractBorder
	{
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
			else if(name.equals(TOP))
				top = comp;
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
			else if(top == comp)
				top = null;
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
			if(top != null)
			{
				Dimension topPref = top.getPreferredSize();
				dim.height += topPref.height;
			}

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
			if(top != null)
			{
				Dimension topPref = top.getMinimumSize();
				dim.height += topPref.height;
			}
			
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
			int topHeight;
			if(top != null)
			{
				topHeight = top.getPreferredSize().height;
			}
			else
			{
				topHeight = 0;
			}
			int bottomHeight = bottom.getPreferredSize().height;
			int centerWidth = Math.max(0,size.width - leftWidth
				- rightWidth - ileft - iright);
			int centerHeight = Math.max(0,size.height - topHeight
				- bottomHeight - itop - ibottom);
				
			left.setBounds(
				ileft,
				itop+topHeight,
				leftWidth,
				centerHeight);

			center.setBounds(
				ileft + leftWidth,
				itop+topHeight,
				centerWidth,
				centerHeight);

			right.setBounds(
				ileft + leftWidth + centerWidth,
				itop+topHeight,
				rightWidth,
				centerHeight);

			bottom.setBounds(
				ileft,
				itop + topHeight + centerHeight,
				/* silly that we reference the vertical
				   scroll bar here directly. we do this so
				   that the horizontal scroll bar is flush
				   with the vertical scroll bar */
				Math.max(0,size.width - vertical.getWidth()
					- ileft - iright),
				bottomHeight);
			if(top != null)
			{
				top.setBounds(
					ileft,
					itop,
					leftWidth+centerWidth+rightWidth,
					topHeight);
			}
		} //}}}

		Component center;
		Component left;
		Component right;
		Component bottom;
		Component top;
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

	//{{{ FocusHandler class
	class FocusHandler implements FocusListener
	{
		//{{{ focusGained() method
		public void focusGained(FocusEvent evt)
		{
			if(bufferChanging)
				return;

			if(match != null)
			{
				if(caretLine < match.startLine)
					invalidateLineRange(caretLine,match.endLine);
				else
					invalidateLineRange(match.startLine,caretLine);
			}
			else
				invalidateLine(caretLine);

			focusedComponent = JEditTextArea.this;
		} //}}}

		//{{{ focusLost() method
		public void focusLost(FocusEvent evt)
		{
			if(!isShowing())
				return;

			if(match != null)
			{
				if(caretLine < match.startLine)
					invalidateLineRange(caretLine,match.endLine);
				else
					invalidateLineRange(match.startLine,caretLine);
			}
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
		/* with drag and drop on, a mouse down in a selection does not
		immediately deselect */
		private boolean maybeDragAndDrop;

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
			control = (OperatingSystem.isMacOS() && evt.isMetaDown())
				|| (!OperatingSystem.isMacOS() && evt.isControlDown());

			// so that Home <mouse click> Home is not the same
			// as pressing Home twice in a row
			view.getInputHandler().resetLastActionCount();

			quickCopyDrag = (isQuickCopyEnabled() &&
				GUIUtilities.isMiddleButton(evt.getModifiers()));

			if(!quickCopyDrag)
			{
				requestFocus();
				focusedComponent = JEditTextArea.this;
			}

			if(!buffer.isLoaded())
				return;

			int x = evt.getX();
			int y = evt.getY();

			dragStart = xyToOffset(x,y,!(painter.isBlockCaretEnabled()
				|| isOverwriteEnabled()));
			dragStartLine = getLineOfOffset(dragStart);
			dragStartOffset = dragStart - getLineStartOffset(dragStartLine);

			if(GUIUtilities.isPopupTrigger(evt) && popup != null)
			{
				if(popup.isVisible())
					popup.setVisible(false);
				else
				{
					if(getSelectionCount() == 0 || multi)
						moveCaretPosition(dragStart,false);
					GUIUtilities.showPopupMenu(popup,painter,
						evt.getX(),evt.getY());
				}
				return;
			}

			dragged = false;

			blink = true;
			invalidateLine(caretLine);

			clickCount = evt.getClickCount();

			if(isDragEnabled() && getDragAndDropCallback() != null
				&& getSelectionAtOffset(dragStart) != null
				&& clickCount == 1 && !evt.isShiftDown())
			{
				maybeDragAndDrop = true;
				moveCaretPosition(dragStart,false);
				return;
			}
			else
				maybeDragAndDrop = false;

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
			int x = evt.getX();

			int extraEndVirt = 0;
			if(chunkCache.getLineInfo(screenLastLine).lastSubregion)
			{
				float dragStartLineWidth = offsetToXY(
					dragStartLine,getLineLength(dragStartLine),
					returnValue).x;
				if(x > dragStartLineWidth)
				{
					extraEndVirt = (int)(
						(x - dragStartLineWidth)
						/ charWidth);
					if(!getPainter().isBlockCaretEnabled()
						&& !isOverwriteEnabled()
						&& (x - getHorizontalOffset()) % charWidth > charWidth / 2)
					{
						extraEndVirt++;
					}
				}
			}

			if(control || isRectangularSelectionEnabled())
			{
				int screenLine = (evt.getY() / getPainter()
					.getFontMetrics().getHeight());
				if(screenLine > screenLastLine)
					screenLine = screenLastLine;
				ChunkCache.LineInfo info = chunkCache.getLineInfo(screenLine);
				if(info.lastSubregion)
				{
					// control-click in virtual space inserts
					// whitespace and moves caret
					String whitespace = MiscUtilities
						.createWhiteSpace(extraEndVirt,0);
					buffer.insert(dragStart,whitespace);

					dragStart += whitespace.length();
				}
			}

			if(evt.isShiftDown())
			{
				// XXX: getMarkPosition() deprecated!
				resizeSelection(getMarkPosition(),dragStart,extraEndVirt,
					isRectangularSelectionEnabled()
					|| control);

				if(!quickCopyDrag)
					moveCaretPosition(dragStart,false);

				// so that shift-click-drag works
				dragStartLine = getMarkLine();
				dragStart = getMarkPosition();
				dragStartOffset = dragStart
					- getLineStartOffset(dragStartLine);

				// so that quick copy works
				dragged = true;

				return;
			}

			if(!quickCopyDrag)
				moveCaretPosition(dragStart,false);

			if(!(multi || quickCopyDrag))
				selectNone();
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
			Selection sel = new Selection.Range(
				lineStart + wordStart,
				lineStart + wordEnd);
			if(isMultipleSelectionEnabled())
				addToSelection(sel);
			else
				setSelection(sel);

			if(quickCopyDrag)
				quickCopyDrag = false;

			moveCaretPosition(lineStart + wordEnd,false);

			dragged = true;
		} //}}}

		//{{{ doTripleClick() method
		private void doTripleClick(MouseEvent evt)
		{
			int newCaret = getLineEndOffset(dragStartLine);
			if(dragStartLine == buffer.getLineCount() - 1)
				newCaret--;

			Selection sel = new Selection.Range(
				getLineStartOffset(dragStartLine),
				newCaret);
			if(isMultipleSelectionEnabled())
				addToSelection(sel);
			else
				setSelection(sel);

			if(quickCopyDrag)
				quickCopyDrag = false;

			moveCaretPosition(newCaret,false);

			dragged = true;
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent evt)
		{
			if(maybeDragAndDrop)
			{
				startDragAndDrop(evt,control);
				return;
			}

			if(dndInProgress)
				return;

			if(GUIUtilities.isPopupTrigger(evt)
				|| (popup != null && popup.isVisible()))
				return;

			if(!buffer.isLoaded())
				return;

			if(evt.getY() < 0)
			{
				int delta = Math.min(-1,evt.getY()
					/ painter.getFontMetrics()
					.getHeight());
				setFirstLine(getFirstLine() + delta);
			}
			else if(evt.getY() >= painter.getHeight())
			{
				int delta = Math.max(1,(evt.getY()
					- painter.getHeight()) /
					painter.getFontMetrics()
					.getHeight());
				if(lastLinePartial)
					delta--;
				setFirstLine(getFirstLine() + delta);
			}

			if(quickCopyDrag)
			{
				view.getStatus().setMessage(jEdit.getProperty(
					"view.status.rect-quick-copy"));
				clearStatus = true;
			}

			switch(clickCount)
			{
			case 1:
				doSingleDrag(evt);
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
		private void doSingleDrag(MouseEvent evt)
		{
			dragged = true;

			int x = evt.getX();
			int y = evt.getY();
			if(y < 0)
				y = 0;
			else if(y >= painter.getHeight())
				y = painter.getHeight() - 1;

			int dot = xyToOffset(x,y,
				(!painter.isBlockCaretEnabled()
				&& !isOverwriteEnabled())
				|| quickCopyDrag);
			int dotLine = buffer.getLineOfOffset(dot);
			int extraEndVirt = 0;

			if(chunkCache.getLineInfo(screenLastLine).lastSubregion)
			{
				float dotLineWidth = offsetToXY(dotLine,getLineLength(dotLine),
					returnValue).x;
				if(x > dotLineWidth)
				{
					extraEndVirt = (int)((x - dotLineWidth) / charWidth);
					if(!getPainter().isBlockCaretEnabled()
						&& !isOverwriteEnabled()
						&& (x - getHorizontalOffset()) % charWidth > charWidth / 2)
						extraEndVirt++;
				}
			}

			resizeSelection(dragStart,dot,extraEndVirt,
				isRectangularSelectionEnabled()
				|| control);

			if(quickCopyDrag)
			{
				// just scroll to the dragged location
				scrollTo(dotLine,dot - buffer.getLineStartOffset(dotLine),false);
			}
			else
			{
				if(dot != caret)
					moveCaretPosition(dot,false);
				if(isRectangularSelectionEnabled()
					&& extraEndVirt != 0)
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
			moveCaretPosition(lineStart + offset,false);

			dragged = true;
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

			resizeSelection(mark,mouse,0,false);
			moveCaretPosition(mouse,false);

			dragged = true;
		} //}}}

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent evt)
		{
			// middle mouse button drag inserts selection
			// at caret position
			Selection sel = getSelectionAtOffset(dragStart);
			if(dragged && sel != null)
			{
				Registers.setRegister('%',getSelectedText(sel));
				if(quickCopyDrag)
				{
					removeFromSelection(sel);
					Registers.paste(focusedComponent,
						'%',sel instanceof Selection.Rect);

					focusedComponent.requestFocus();
				}
			}
			else if(!dragged && isQuickCopyEnabled() &&
				GUIUtilities.isMiddleButton(evt.getModifiers()))
			{
				JEditTextArea.this.requestFocus();
				focusedComponent = JEditTextArea.this;

				setCaretPosition(dragStart,false);
				if(!isEditable())
					getToolkit().beep();
				else
					Registers.paste(JEditTextArea.this,'%',control);
			}
			else if(maybeDragAndDrop && !isMultipleSelectionEnabled())
			{
				selectNone();
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

		structureTimer = new Timer(100,new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if(focusedComponent != null)
					focusedComponent.updateStructureHighlight();
			}
		});
		structureTimer.setInitialDelay(100);
		structureTimer.setRepeats(false);
	} //}}}
}
