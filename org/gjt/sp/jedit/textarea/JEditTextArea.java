/*
 * JEditTextArea.java - jEdit's text component
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.Segment;
import javax.swing.undo.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's text component.
 *
 * @author Slava Pestov
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
		renderer = TextRenderer.createTextRenderer();
		painter = new TextAreaPainter(this);
		gutter = new Gutter(view,this);
		bufferHandler = new BufferChangeHandler();
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		bracketLine = bracketPosition = -1;
		blink = true;
		lineSegment = new Segment();
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
		painter.addComponentListener(new ComponentHandler());

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

	//{{{ getTextRenderer() method
	/**
	 * Returns the text renderer instance.
	 * @since jEdit 3.2pre6
	 */
	public TextRenderer getTextRenderer()
	{
		return renderer;
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

	//{{{ isMiddleMousePasteEnabled() method
	/**
	 * Returns if clicking the middle mouse button pastes the most
	 * recent selection (% register).
	 */
	public final boolean isMiddleMousePasteEnabled()
	{
		return middleMousePaste;
	} //}}}

	//{{{ setMiddleMousePasteEnabled() method
	/**
	 * Sets if clicking the middle mouse button pastes the most
	 * recent selection (% register).
	 * @param middleMousePaste A boolean flag
	 */
	public final void setMiddleMousePasteEnabled(boolean middleMousePaste)
	{
		this.middleMousePaste = middleMousePaste;
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
	 * Sets the buffer this text area is editing.
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
				setCaretPosition(0);
				this.buffer._releaseFoldVisibilityManager(foldVisibilityManager);
				this.buffer.removeBufferChangeListener(bufferHandler);
			}
			this.buffer = buffer;

			buffer.addBufferChangeListener(bufferHandler);
			bufferHandlerInstalled = true;

			foldVisibilityManager = buffer._getFoldVisibilityManager(this);

			painter.updateTabSize();

			for(int i = 0; i < lineWidths.length; i++)
			{
				lineWidths[i] = 0;
			}

			updateScrollBars();
			painter.repaint();
			gutter.repaint();
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
	 * a virtual, not a physical, line number.
	 */
	public final int getFirstLine()
	{
		return firstLine;
	} //}}}

	//{{{ setFirstLine() method
	/**
	 * Sets the line displayed at the text area's origin. This is
	 * a virtual, not a physical, line number.
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
		firstLine = Math.max(0,firstLine);
		this.firstLine = firstLine;

		physFirstLine = virtualToPhysical(firstLine);

		maxHorizontalScrollWidth = 0;

		// hack so that if we scroll and the matching bracket
		// comes into view, it is highlighted

		// 3.2pre9 update: I am commenting this out once again because
		// I have changed the location of the repaintAndScroll() call
		// in the BufferChangeHandler, so this is called before the caret
		// position is updated, which can be potentially tricky.

		//if(bracketPosition == -1)
		//	updateBracketHighlight();

		if(this.firstLine != vertical.getValue())
			updateScrollBars();

		painter.repaint();
		gutter.repaint();

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

	//{{{ setOrigin() method
	/**
	 * @deprecated Use setFirstLine() and setHorizontalOffset() instead
	 */
	public boolean setOrigin(int firstLine, int horizontalOffset)
	{
		setFirstLine(firstLine);
		setHorizontalOffset(horizontalOffset);
		return true;
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
			setFirstLine(firstLine-1);
		else
			getToolkit().beep();
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
			int newFirstLine = firstLine - visibleLines;
			setFirstLine(newFirstLine);
		}
		else
		{
			getToolkit().beep();
		}
	} //}}}

	//{{{ scrollDownLine() method
	/**
	 * Scrolls down by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownLine()
	{
		int numLines = getVirtualLineCount();

		if(firstLine + visibleLines < numLines)
			setFirstLine(firstLine + 1);
		else
			getToolkit().beep();
	} //}}}

	//{{{ scrollDownPage() method
	/**
	 * Scrolls down by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownPage()
	{
		int numLines = getVirtualLineCount();

		if(firstLine + visibleLines < numLines)
		{
			int newFirstLine = firstLine + visibleLines;
			setFirstLine(newFirstLine + visibleLines < numLines
				? newFirstLine : numLines - visibleLines);
		}
		else
		{
			getToolkit().beep();
		}
	} //}}}

	//{{{ scrollToCaret() method
	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 * @param doElectricScroll If true, electric scrolling will be performed
	 */
	public void scrollToCaret(boolean doElectricScroll)
	{
		int offset = caret - getLineStartOffset(caretLine);
		int virtualCaretLine = physicalToVirtual(caretLine);

		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if(visibleLines == 0)
		{
			setFirstLine(physicalToVirtual(
				Math.max(0,caretLine - electricScroll)));
			return;
		}

		int lineCount = getVirtualLineCount();

		int electricScroll;

		if(doElectricScroll && visibleLines > this.electricScroll * 2)
			electricScroll = this.electricScroll;
		else
			electricScroll = 0;

		boolean changed = false;

		int _firstLine = (firstLine == 0 ? 0 : firstLine + electricScroll);
		int _lastLine = firstLine + visibleLines - electricScroll;

		if(virtualCaretLine > _firstLine
			&& (virtualCaretLine < _lastLine
			|| firstLine + visibleLines >= lineCount))
		{
			// vertical scroll position is correct already
		}
		else if(_firstLine - virtualCaretLine > visibleLines
			|| virtualCaretLine - _lastLine > visibleLines)
		{
			int startLine, endLine;
			Selection s = getSelectionAtOffset(caret);
			if(s == null)
			{
				startLine = endLine = virtualCaretLine;
			}
			else
			{
				startLine = physicalToVirtual(s.startLine);
				endLine = physicalToVirtual(s.endLine);
			}

			if(endLine - startLine <= visibleLines)
				firstLine = (startLine + endLine - visibleLines) / 2;
			else
				firstLine = physicalToVirtual(caretLine) - visibleLines / 2;

			firstLine = Math.min(firstLine,getVirtualLineCount()
				- visibleLines);
			firstLine = Math.max(firstLine,0);

			changed = true;
		}
		else if(virtualCaretLine < _firstLine)
		{
			firstLine = Math.max(0,virtualCaretLine - electricScroll);

			changed = true;
		}
		else if(virtualCaretLine >= _lastLine)
		{
			firstLine = (virtualCaretLine - visibleLines)
				+ electricScroll + 1;
			if(firstLine >= getVirtualLineCount() - visibleLines)
				firstLine = getVirtualLineCount() - visibleLines;

			changed = true;
		}

		int x = offsetToX(caretLine,offset);
		int width = painter.getFontMetrics().charWidth('w');

		if(x < 0)
		{
			horizontalOffset = Math.min(0,horizontalOffset
				- x + width + 5);
			changed = true;
		}
		else if(x >= painter.getWidth() - width - 5)
		{
			horizontalOffset = horizontalOffset +
				(painter.getWidth() - x) - width - 5;
			changed = true;
		}

		if(changed)
		{
			if(firstLine < 0)
				firstLine = 0;

			physFirstLine = virtualToPhysical(firstLine);

			updateScrollBars();
			painter.repaint();
			gutter.repaint();

			view.synchroScrollVertical(this,firstLine);
			view.synchroScrollHorizontal(this,horizontalOffset);

			// fire events for both a horizontal and vertical scroll
			fireScrollEvent(true);
			fireScrollEvent(false);
		}
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

	//{{{ Offset conversion

	//{{{ lineToY() method
	/**
	 * Converts a line index to a y co-ordinate. This must be a virtual,
	 * not a physical, line number.
	 * @param line The line
	 */
	public int lineToY(int line)
	{
		FontMetrics fm = painter.getFontMetrics();
		return (line - firstLine) * fm.getHeight()
			- (fm.getLeading() + fm.getDescent());
	} //}}}

	//{{{ yToLine() method
	/**
	 * Converts a y co-ordinate to a virtual line index.
	 * @param y The y co-ordinate
	 */
	public int yToLine(int y)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		return Math.max(0,Math.min(getVirtualLineCount() - 1,
			y / height + firstLine));
	} //}}}

	//{{{ offsetToX() method
	/**
	 * Converts an offset in a line into an x co-ordinate.
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public int offsetToX(int line, int offset)
	{
		Token tokens = buffer.markTokens(line).getFirstToken();

		getLineText(line,lineSegment);

		char[] text = lineSegment.array;
		int off = lineSegment.offset;

		float x = (float)horizontalOffset;

		Toolkit toolkit = painter.getToolkit();
		Font defaultFont = painter.getFont();
		SyntaxStyle[] styles = painter.getStyles();

		for(;;)
		{
			byte id = tokens.id;
			if(id == Token.END)
				return (int)x;

			Font font;
			if(id == Token.NULL)
				font = defaultFont;
			else
				font = styles[id].getFont();

			int len = tokens.length;

			if(offset < len)
			{
				return (int)(x + renderer.charsWidth(
					text,off,offset,font,x,painter));
			}
			else
			{
				x += renderer.charsWidth(
					text,off,len,font,x,painter);
				off += len;
				offset -= len;
			}

			tokens = tokens.next;
		}
	} //}}}

	//{{{ xToOffset() method
	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		return xToOffset(line,x,true);
	} //}}}

	//{{{ xToOffset() method
	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 3.2pre6
	 */
	public int xToOffset(int line, int x, boolean round)
	{
		Token tokens = buffer.markTokens(line).getFirstToken();

		getLineText(line,lineSegment);

		char[] text = lineSegment.array;
		int off = lineSegment.offset;

		Toolkit toolkit = painter.getToolkit();
		Font defaultFont = painter.getFont();
		SyntaxStyle[] styles = painter.getStyles();

		float[] widthArray = new float[] { horizontalOffset };

		for(;;)
		{
			byte id = tokens.id;
			if(id == Token.END)
				return lineSegment.count;

			Font font;
			if(id == Token.NULL)
				font = defaultFont;
			else
				font = styles[id].getFont();

			int len = tokens.length;

			int offset = renderer.xToOffset(text,off,len,font,x,
				painter,round,widthArray);

			if(offset != -1)
				return offset - lineSegment.offset;

			off += len;
			tokens = tokens.next;
		}
	} //}}}

	//{{{ xyToOffset() method
	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		return xyToOffset(x,y,true);
	} //}}}

	//{{{ xyToOffset() method
	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 3.2pre6
	 */
	public int xyToOffset(int x, int y, boolean round)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		int line = y / height + firstLine;

		if(line < 0)
			return 0;
		else if(line >= getVirtualLineCount())
		{
			// WRONG!!!
			// return getBufferLength();
			return getLineEndOffset(virtualToPhysical(
				getVirtualLineCount() - 1)) - 1;
		}
		else
		{
			line = virtualToPhysical(line);
			return getLineStartOffset(line) + xToOffset(line,x);
		}
	} //}}}

	//}}}

	//{{{ Painting

	//{{{ invalidateLine() method
	/**
	 * Marks a line as needing a repaint.
	 * @param line The physical line to invalidate
	 */
	public void invalidateLine(int line)
	{
		if(line < physFirstLine)
			return;

		int lineCount = buffer.getLineCount();
		int virtualLineCount = foldVisibilityManager
			.getVirtualLineCount();

		if(line >= lineCount)
			line = (line - lineCount) + virtualLineCount;
		else
			line = foldVisibilityManager.physicalToVirtual(line);

		if(line > firstLine + visibleLines + 1)
			return;

		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(line) + fm.getDescent() + fm.getLeading();
		painter.repaint(0,y,painter.getWidth(),fm.getHeight());
		gutter.repaint(0,y,gutter.getWidth(),fm.getHeight());
	} //}}}

	//{{{ invalidateLineRange() method
	/**
	 * Marks a range of physical lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public void invalidateLineRange(int firstLine, int lastLine)
	{
		if(lastLine < physFirstLine)
			return;

		int lineCount = buffer.getLineCount();
		int virtualLineCount = foldVisibilityManager
			.getVirtualLineCount();

		if(firstLine >= lineCount)
			firstLine = (firstLine - lineCount) + virtualLineCount;
		else
			firstLine = foldVisibilityManager.physicalToVirtual(firstLine);

		if(firstLine > firstLine + visibleLines + 1)
			return;

		if(lastLine >= lineCount)
			lastLine = (lastLine - lineCount) + virtualLineCount;
		else
			lastLine = foldVisibilityManager.physicalToVirtual(lastLine);

		invalidateVirtualLineRange(firstLine,lastLine);
	} //}}}

	//{{{ invalidateSelectedLines() method
	/**
	 * Repaints the lines containing the selection.
	 */
	public void invalidateSelectedLines()
	{
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			invalidateLineRange(s.startLine,s.endLine);
		}
	} //}}}

	//{{{ invalidateVirtualLineRange() method
	/**
	 * Marks a range of virtual lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public void invalidateVirtualLineRange(int firstLine, int lastLine)
	{
		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(firstLine) + fm.getDescent() + fm.getLeading();
		int height = (lastLine - firstLine + 1) * fm.getHeight();
		painter.repaint(0,y,painter.getWidth(),height);
		gutter.repaint(0,y,gutter.getWidth(),height);
	} //}}}


	//}}}

	//{{{ Convenience methods

	//{{{ physicalToVirtual() method
	/**
	 * Converts a physical line number to a virtual line number.
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
		setSelection(new Selection.Range(start,end));
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
		setSelection(new Selection.Range(selectionStart,
			selectionEnd));
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
		String noWordSep = (String)buffer.getProperty("noWordSep");

		if(offset == getLineLength(line))
			offset--;

		int wordStart = TextUtilities.findWordStart(lineText,offset,noWordSep);
		int wordEnd = TextUtilities.findWordEnd(lineText,offset+1,noWordSep);

		setSelection(new Selection.Range(lineStart + wordStart,
			lineStart + wordEnd));
		moveCaretPosition(lineStart + wordEnd);
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

		setSelection(new Selection.Range(start,end));
		moveCaretPosition(end);
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

	//{{{ selectFoldAt() method
	/**
	 * Selects the fold that contains the specified line number.
	 * @param line The line number
	 * @since jEdit 4.0pre1
	 */
	public void selectFold(int line)
	{
		int start;
		int end;

		if(buffer.isFoldStart(line))
		{
			start = line;
			int foldLevel = buffer.getFoldLevel(line);

			line++;

			while(line < buffer.getLineCount()
				&& buffer.getFoldLevel(line) > foldLevel)
				line++;
			end = line;
		}
		else
		{
			start = line;
			int foldLevel = buffer.getFoldLevel(line);
			while(start >= 0 && buffer.getFoldLevel(start) >= foldLevel)
				start--;
			end = line;
			while(end < buffer.getLineCount()
				&& buffer.getFoldLevel(end) >= foldLevel)
				end++;
		}

		int newCaret = getLineEndOffset(end) - 1;
		extendSelection(getLineStartOffset(start),newCaret);
		moveCaretPosition(newCaret);
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
	 * Returns the number of selections. This is primarily for use by the
	 * the status bar.
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
	 * Sets the selection.
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
	 * Sets the selection.
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
	 * Adds to the selection.
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

		fireCaretEvent();
	} //}}}

	//{{{ addToSelection() method
	/**
	 * Adds to the selection.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection selection)
	{
		_addToSelection(selection);
		fireCaretEvent();
	} //}}}

	//{{{ getSelectionAtOffset() method
	/**
	 * Returns the selection containing the specific offset, or null
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
	 * @param rect Make the selection rectangular?
	 * @since jEdit 3.2pre1
	 */
	public void resizeSelection(int offset, int end, boolean rect)
	{
		Selection s = getSelectionAtOffset(offset);
		if(s != null)
		{
			invalidateLineRange(s.startLine,s.endLine);
			selection.removeElement(s);
		}

		if(end < offset)
		{
			int tmp = offset;
			offset = end;
			end = tmp;
		}

		Selection newSel;
		if(rect)
			newSel = new Selection.Rect(offset,end);
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
		getSelectedText(s,buf);
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

			getSelectedText((Selection)selection.elementAt(i),buf);
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

			if(s instanceof Selection.Rect)
			{
				int start = s.start - getLineStartOffset(s.startLine);
				int end = s.end - getLineStartOffset(s.endLine);

				// Certain rectangles satisfy this condition...
				if(end < start)
				{
					int tmp = end;
					end = start;
					start = tmp;
				}

				int lastNewline = 0;
				int currNewline = 0;

				for(int i = s.startLine; i <= s.endLine; i++)
				{
					int lineStart = getLineStartOffset(i);
					int lineEnd = getLineEndOffset(i) - 1;
					int rectStart = Math.min(lineEnd,lineStart + start);

					buffer.remove(rectStart,Math.min(lineEnd - rectStart,
						end - start));

					if(selectedText == null)
						continue;

					currNewline = selectedText.indexOf('\n',lastNewline);
					if(currNewline == -1)
						currNewline = selectedText.length();

					buffer.insert(rectStart,selectedText
						.substring(lastNewline,currNewline));

					lastNewline = Math.min(selectedText.length(),
						currNewline + 1);
				}

				if(selectedText != null &&
					currNewline != selectedText.length())
				{
					int offset = getLineEndOffset(s.endLine) - 1;
					buffer.insert(offset,"\n");
					buffer.insert(offset + 1,selectedText
						.substring(currNewline + 1));
				}
			}
			else
			{
				buffer.remove(s.start,s.end - s.start);
				if(selectedText != null && selectedText.length() != 0)
				{
					buffer.insert(s.start,selectedText);
				}
			}
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
				buffer.beginCompoundEdit();

				for(int i = 0; i < selection.length; i++)
				{
					setSelectedText(selection[i],selectedText);
				}
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
	 * Returns an array of all line numbers that contain a selection.
	 * This array will also include the line number containing the
	 * caret, for convinience.
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

		quicksort(returnValue,0,returnValue.length - 1);

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

	//{{{ Old selection API

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
		int gotoLine = virtualToPhysical(firstLine + visibleLines / 2);

		if(gotoLine < 0 || gotoLine >= getLineCount())
		{
			getToolkit().beep();
			return;
		}

		setCaretPosition(getLineStartOffset(gotoLine));
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

		// When the user is typing, etc, we don't want the caret
		// to blink
		blink = true;
		caretTimer.restart();

		if(caret == newCaret)
		{
			// so that C+y <marker>, for example, will return
			// to the saved location even if the caret was
			// never moved but the user scrolled instead
			scrollToCaret(doElectricScroll);
			return;
		}

		int newCaretLine = getLineOfOffset(newCaret);

		magicCaret = -1;

		invalidateLine(caretLine);

		if(caretLine != newCaretLine)
			invalidateLine(newCaretLine);

		if(!foldVisibilityManager.isLineVisible(newCaretLine))
			foldVisibilityManager.expandFold(newCaretLine,false);

		caret = newCaret;
		caretLine = newCaretLine;

		if(focusedComponent == this)
			scrollToCaret(doElectricScroll);

		updateBracketHighlight();

		fireCaretEvent();
	} //}}}

	//{{{ getCaretPosition() method
	/**
	 * Returns the caret position.
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
	 * Returns the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
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
		int caret = getCaretPosition();
		int line = getCaretLine();

		int magic = getMagicCaretPosition();

		int nextLine = foldVisibilityManager.getNextVisibleLine(line);

		if(nextLine == -1)
		{
			getToolkit().beep();
			return;
		}

		int newCaret = getLineStartOffset(nextLine)
			+ xToOffset(nextLine,magic + 1);
		if(select)
			extendSelection(caret,newCaret);
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
		Marker marker = null;

		for(int i = 0; i < markers.size(); i++)
		{
			Marker _marker = (Marker)markers.elementAt(i);
			if(_marker.getPosition() > caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			getToolkit().beep();
		else
		{
			if(select)
				extendSelection(caret,marker.getPosition());
			else if(!multi)
				selectNone();
			moveCaretPosition(marker.getPosition());
		}
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
	 * @since jEdit 2.7pre2
	 */
	public void goToNextWord(boolean select)
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
			String noWordSep = (String)buffer.getProperty("noWordSep");
			newCaret = TextUtilities.findWordEnd(lineText,newCaret + 1,noWordSep)
				+ lineStart;
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
		int magic = getMagicCaretPosition();

		int prevLine = foldVisibilityManager.getPrevVisibleLine(caretLine);
		if(prevLine == -1)
		{
			getToolkit().beep();
			return;
		}

		int newCaret = getLineStartOffset(prevLine) + xToOffset(prevLine,magic + 1);
		if(select)
			extendSelection(caret,newCaret);
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
			getToolkit().beep();
		else
		{
			if(select)
				extendSelection(caret,marker.getPosition());
			else if(!multi)
				selectNone();
			moveCaretPosition(marker.getPosition());
		}
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
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevWord(boolean select)
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
			String noWordSep = (String)buffer.getProperty("noWordSep");
			newCaret = TextUtilities.findWordStart(lineText,newCaret - 1,noWordSep)
				+ lineStart;
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
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			goToStartOfLine(select);
		else
		{
			switch(view.getInputHandler().getLastActionCount())
			{
			case 1:
				goToStartOfWhiteSpace(select);
				break;
			case 2:
				goToStartOfLine(select);
				break;
			default: //case 3:
				goToFirstVisibleLine(select);
				break;
			}
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
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			goToEndOfLine(select);
		else
		{
			switch(view.getInputHandler().getLastActionCount())
			{
			case 1:
				goToEndOfWhiteSpace(select);
				break;
			case 2:
				goToEndOfLine(select);
				break;
			default: //case 3:
				goToLastVisibleLine(select);
				break;
			}
		}
	} //}}}

	//{{{ goToStartOfLine() method
	/**
	 * Moves the caret to the beginning of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfLine(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToStartOfLine(" + select + ");");

		int newCaret = getLineStartOffset(getCaretLine());
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
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToEndOfLine(" + select + ");");

		int newCaret = getLineEndOffset(getCaretLine()) - 1;
		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);

		// so that end followed by up arrow will always put caret at
		// the end of the previous line, for example
		setMagicCaretPosition(Integer.MAX_VALUE);
	} //}}}

	//{{{ goToStartOfWhiteSpace() method
	/**
	 * Moves the caret to the first non-whitespace character of the current
	 * line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfWhiteSpace(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");

		int firstIndent = MiscUtilities.getLeadingWhiteSpace(getLineText(caretLine));
		int firstOfLine = getLineStartOffset(caretLine);

		firstIndent = firstOfLine + firstIndent;
		if(firstIndent == getLineEndOffset(caretLine) - 1)
			firstIndent = firstOfLine;

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
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");

		int lastIndent = MiscUtilities.getTrailingWhiteSpace(getLineText(caretLine));
		int lastOfLine = getLineEndOffset(caretLine) - 1;

		lastIndent = lastOfLine - lastIndent;
		if(lastIndent == getLineStartOffset(caretLine))
			lastIndent = lastOfLine;

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
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToFirstVisibleLine(" + select + ");");

		int firstVisibleLine = (firstLine <= electricScroll) ? 0 :
			firstLine + electricScroll;
		if(firstVisibleLine >= getVirtualLineCount())
			firstVisibleLine = getVirtualLineCount() - 1;

		firstVisibleLine = virtualToPhysical(firstVisibleLine);

		int firstVisible = getLineStartOffset(firstVisibleLine);

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
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToLastVisibleLine(" + select + ");");

		int lastVisibleLine = firstLine + visibleLines;

		if(lastVisibleLine >= getVirtualLineCount())
			lastVisibleLine = getVirtualLineCount() - 1;
		else if(lastVisibleLine <= electricScroll)
			lastVisibleLine = 0;
		else
			lastVisibleLine -= (electricScroll + 1);

		lastVisibleLine = virtualToPhysical(lastVisibleLine);

		int lastVisible = getLineEndOffset(lastVisibleLine) - 1;

		if(select)
			extendSelection(caret,lastVisible);
		else if(!multi)
			selectNone();
		moveCaretPosition(lastVisible);
	} //}}}

	//{{{ goToMatchingBracket() method
	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * @since jEdit 2.7pre3
	 */
	public void goToMatchingBracket()
	{
		int dot = caret - getLineStartOffset(caretLine);

		int bracket = TextUtilities.findMatchingBracket(
			buffer,caretLine,Math.max(0,dot - 1));
		if(bracket != -1)
		{
			setCaretPosition(bracket + 1);
			return;
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

	public void userInputTest()
	{
		long start = System.currentTimeMillis();
		for(int i = 0; i < 70; i++)
			userInput('m');
		System.err.println((System.currentTimeMillis() - start) / 70);
	}

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
			if(selection.size() != 0)
				shiftIndentRight();
			else if(buffer.getBooleanProperty("indentOnTab")
				&& buffer.indentLine(caretLine,true,false))
				return;
			else if(buffer.getBooleanProperty("noTabs"))
			{
				int lineStart = getLineStartOffset(caretLine);

				String line = getText(lineStart,caret - lineStart);

				setSelectedText(createSoftTab(line,buffer.getTabSize()));
			}
			else
				setSelectedText("\t");
			return;
		}
		else if(ch == '\n')
		{
			try
			{
				buffer.beginCompoundEdit();
				setSelectedText("\n");
				if(buffer.getBooleanProperty("indentOnEnter"))
					buffer.indentLine(caretLine,true,false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
			return;
		}
		else
		{
			String str = String.valueOf(ch);
			if(selection.size() != 0)
			{
				setSelectedText(str);
				return;
			}

			if(ch == ' ')
			{
				if(doWordWrap(caretLine,true))
					return;
			}
			else
				doWordWrap(caretLine,false);

			try
			{
				// Don't overstrike if we're on the end of
				// the line
				if(overwrite)
				{
					buffer.beginCompoundEdit();

					int caretLineEnd = getLineEndOffset(caretLine);
					if(caretLineEnd - caret > 1)
						buffer.remove(caret,1);
				}

				buffer.insert(caret,str);
			}
			finally
			{
				if(overwrite)
					buffer.endCompoundEdit();
			}
		}

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
			buffer.indentLine(caretLine,false,true);
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
			setSelectedText("");
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
			String noWordSep = (String)buffer.getProperty("noWordSep");
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
		buffer.remove(start,end - start);
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
			String noWordSep = (String)buffer.getProperty("noWordSep");
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

	///{{{ toggleMultipleSelectionEnabled() method
	/**
	 * Toggles multiple selection.
	 * @since jEdit 3.2pre1
	 */
	public final void toggleMultipleSelectionEnabled()
	{
		multi = !multi;
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	} //}}}

	//{{{ setMultipleSelectionEnabled() method
	/**
	 * Sets multiple selection.
	 * @param multi Should multiple selection be enabled?
	 * @since jEdit 3.2pre1
	 */
	public final void setMultipleSelectionEnabled(boolean multi)
	{
		JEditTextArea.multi = multi;
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	}
	//}}}

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

	//{{{ Text editing

	//{{{ lineComment() method
	/**
	 * Prepends each line of the selection with the line comment string.
	 * @since jEdit 3.2pre1
	 */
	public void lineComment()
	{
		String comment = (String)buffer.getProperty("lineComment");
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
		String commentStart = (String)buffer.getProperty("commentStart");
		String commentEnd = (String)buffer.getProperty("commentEnd");
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
				buffer.insert(caret,
					commentStart);
				buffer.insert(caret,
					commentEnd);
				setCaretPosition(oldCaret + commentStart.length());
			}

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				if(s instanceof Selection.Range)
				{
					buffer.insert(s.start,
						commentStart);
					buffer.insert(s.end,
						commentEnd);
				}
				else if(s instanceof Selection.Rect)
				{
					for(int j = s.startLine; j <= s.endLine; j++)
					{
						buffer.insert(s.getStart(buffer,j),
							commentStart);
						int end = s.getEnd(buffer,j)
							+ (j == s.endLine
							? 0
							: commentStart.length());
						buffer.insert(end,commentEnd);
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
		int maxLineLength = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();
		if(maxLineLength <= 0)
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
					getSelectedText(s),maxLineLength));
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

				start = getLineStartOffset(i);
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

				end = getLineEndOffset(i) - 1;
				break loop;
			}

			try
			{
				buffer.beginCompoundEdit();

				String text = buffer.getText(start,end - start);
				buffer.remove(start,end - start);
				buffer.insert(start,TextUtilities.format(
					text,maxLineLength));
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

		if(!buffer.isEditable() || selection.length == 0)
                {
                	getToolkit().beep();
                	return;
                }

		buffer.beginCompoundEdit();

		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			setSelectedText(s,TextUtilities.spacesToTabs(
				getSelectedText(s),buffer.getTabSize()));
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

		if(!buffer.isEditable() || selection.length == 0)
                {
                	getToolkit().beep();
                	return;
                }

		buffer.beginCompoundEdit();

		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			setSelectedText(s,TextUtilities.tabsToSpaces(
				getSelectedText(s),buffer.getTabSize()));
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
		{
			buffer.shiftIndentRight(getSelectedLines());
		}
	} //}}}

	//{{{ joinLines() method
	/**
	 * Joins the current and the next line.
	 * @since jEdit 2.7pre2
	 */
	public void joinLines()
	{
		int start = getLineStartOffset(caretLine);
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

	//{{{ completeWord() method
	/**
	 * Attempts to complete the word at the caret position, by searching
	 * the buffer for words that start with the currently entered text. If
	 * only one completion is found, it is inserted immediately, otherwise
	 * a popup is shown will all possible completions.
	 * @since jEdit 2.7pre2
	 */
	public void completeWord()
	{
		String noWordSep = (String)buffer.getProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		// first, we get the word before the caret

		String line = getLineText(caretLine);
		int dot = caret - getLineStartOffset(caretLine);
		if(dot == 0)
		{
			getToolkit().beep();
			return;
		}

		char ch = line.charAt(dot-1);
		if(!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1)
		{
			// attempting to expand non-word char
			getToolkit().beep();
			return;
		}

		int wordStart = TextUtilities.findWordStart(line,dot-1,noWordSep);
		String word = line.substring(wordStart,dot);
		if(word.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		Vector completions = new Vector();
		int wordLen = word.length();

		// now loop through all lines of current buffer
		for(int i = 0; i < getLineCount(); i++)
		{
			line = getLineText(i);

			// check for match at start of line

			if(line.startsWith(word))
			{
				if(i == caretLine && wordStart == 0)
					continue;

				String _word = completeWord(line,0,noWordSep);
				if(_word.length() != wordLen)
				{
					// remove duplicates
					if(completions.indexOf(_word) == -1)
						completions.addElement(_word);
				}
			}

			// check for match inside line
			int len = line.length() - word.length();
			for(int j = 0; j < len; j++)
			{
				char c = line.charAt(j);
				if(!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1)
				{
					if(i == caretLine && wordStart == (j + 1))
						continue;

					if(line.regionMatches(j + 1,word,0,wordLen))
					{
						String _word = completeWord(line,j + 1,noWordSep);
						if(_word.length() != wordLen)
						{
							// remove duplicates
							if(completions.indexOf(_word) == -1)
								completions.addElement(_word);
						}
					}
				}
			}
		}

		// sort completion list
		MiscUtilities.quicksort(completions,new MiscUtilities.StringICaseCompare());

		if(completions.size() == 0)
			getToolkit().beep();

		// if there is only one competion, insert in buffer
		else if(completions.size() == 1)
		{
			// chop off 'wordLen' because that's what's already
			// in the buffer
			setSelectedText(((String)completions
				.elementAt(0)).substring(wordLen));
		}
		// show dialog box if > 1
		else
		{
			Point location = new Point(offsetToX(caretLine,wordStart),
				painter.getFontMetrics().getHeight()
				* (physicalToVirtual(caretLine)
				- firstLine + 1));
			SwingUtilities.convertPointToScreen(location,painter);
			new CompleteWord(view,word,completions,location);
		}
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
		if(hasFocus && focusedComponent != this)
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
	public void processKeyEvent(KeyEvent evt)
	{
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

	//{{{ Package-private members
	Segment lineSegment;
	MouseHandler mouseHandler;
	int[] lineWidths;
	int maxHorizontalScrollWidth;

	// this is package-private so that the painter can use it without
	// having to call getSelection() (which involves an array copy)
	Vector selection;

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
	final boolean isHighlightVisible()
	{
		return hasFocus();
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		lineWidths = new int[visibleLines + 1];
		maxHorizontalScrollWidth = -1;
		updateScrollBars();
	} //}}}

	//{{{ updateMaxHorizontalScrollWidth() method
	void updateMaxHorizontalScrollWidth()
	{
		int max = 0;
		for(int i = 0; i < lineWidths.length; i++)
		{
			int width = lineWidths[i];
			if(width > max)
				max = width;
		}

		if(max != maxHorizontalScrollWidth)
		{
			maxHorizontalScrollWidth = max;
			horizontal.setValues(-horizontalOffset,painter.getWidth(),
				0,maxHorizontalScrollWidth
				+ painter.getFontMetrics().charWidth('w'));
		}
	} //}}}

	//{{{ foldStructureChanged() method
	void foldStructureChanged()
	{
		// recalculate first line
		setFirstLine(physicalToVirtual(physFirstLine));

		// update scroll bars because the number of
		// virtual lines might have changed
		updateScrollBars();

		// repaint gutter and painter
		gutter.repaint();
	} //}}}

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
	private int physFirstLine; // only used when fold structure changes

	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;

	private boolean middleMousePaste;

	private JScrollBar vertical;
	private JScrollBar horizontal;
	private boolean scrollBarsInitialized;

	private boolean bufferChanging;
	private Buffer buffer;
	private FoldVisibilityManager foldVisibilityManager;
	private BufferChangeHandler bufferHandler;
	private boolean bufferHandlerInstalled;

	private int caret;
	private int caretLine;

	private int bracketPosition;
	private int bracketLine;

	private int magicCaret;

	private static boolean multi;
	private boolean overwrite;

	private TextRenderer renderer;
	//}}}

	//{{{ quicksort() method
	private static void quicksort(int[] obj, int _start, int _end)
	{
		int start = _start;
		int end = _end;

		int mid = obj[(_start + _end) / 2];

		if(_start > _end)
			return;

		while(start <= end)
		{
			while(start < _end && obj[start] < mid)
				start++;

			while(end > _start && obj[end] > mid)
				end--;

			if(start <= end)
			{
				int tmp = obj[start];
				obj[start] = obj[end];
				obj[end] = tmp;

				start++;
				end--;
			}
		}

		if(_start < end)
			quicksort(obj,_start,end);

		if(start < _end)
			quicksort(obj,start,_end);
	} //}}}

	//{{{ _addToSelection() method
	private void _addToSelection(Selection addMe)
	{
		// this is stupid but it makes things much simpler for
		// the EditPane class
		if(addMe.start < 0)
			addMe.start = 0;
		else if(addMe.end > buffer.getLength())
			addMe.end = buffer.getLength();

		if(addMe.start > addMe.end)
		{
			throw new IllegalArgumentException(addMe.start
				+ " > " + addMe.end);
		}
		else if(addMe.start == addMe.end)
			return;

		for(int i = 0; i < selection.size(); i++)
		{
			// try and merge existing selections one by
			// one with the new selection
			Selection s = (Selection)selection.elementAt(i);
			if(_selectionsOverlap(s,addMe))
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

	//{{{ _selectionsOverlap() method
	private boolean _selectionsOverlap(Selection s1, Selection s2)
	{
		if((s1.start >= s2.start && s1.start <= s2.end)
			|| (s1.end >= s2.start && s1.end <= s2.end))
			return true;
		else
			return false;
	} //}}}

	//{{{ getSelectedText() method
	private void getSelectedText(Selection s, StringBuffer buf)
	{
		if(s instanceof Selection.Rect)
		{
			// Return each row of the selection on a new line
			int start = s.start - getLineStartOffset(s.startLine);
			int end = s.end - getLineStartOffset(s.endLine);

			// Certain rectangles satisfy this condition...
			if(end < start)
			{
				int tmp = end;
				end = start;
				start = tmp;
			}

			for(int i = s.startLine; i <= s.endLine; i++)
			{
				int lineStart = getLineStartOffset(i);
				int lineEnd = getLineEndOffset(i) - 1;
				int lineLen = lineEnd - lineStart;

				lineStart = Math.min(lineStart + start,lineEnd);
				lineLen = Math.min(end - start,lineEnd - lineStart);

				getText(lineStart,lineLen,lineSegment);
				buf.append(lineSegment.array,
					lineSegment.offset,
					lineSegment.count);

				if(i != s.endLine)
					buf.append('\n');
			}

		}
		else
		{
			getText(s.start,s.end - s.start,lineSegment);
			buf.append(lineSegment.array,
				lineSegment.offset,
				lineSegment.count);
		}
	} //}}}

	//{{{ fireCaretEvent() method
	private void fireCaretEvent()
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == CaretListener.class)
			{
				((CaretListener)listeners[i+1]).caretUpdate(caretEvent);
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
				if(vertical)
					((ScrollListener)listeners[i+1]).scrolledVertically(this);
				else
					((ScrollListener)listeners[i+1]).scrolledHorizontally(this);
			}
		}
	} //}}}

	//{{{ createSoftTab() method
	private String createSoftTab(String line, int tabSize)
	{
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

		return MiscUtilities.createWhiteSpace(tabSize - pos,0);
	} //}}}

	//{{{ doWordWrap() method
	private boolean doWordWrap(int line, boolean spaceInserted)
	{
		int maxLineLen = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();

		if(maxLineLen <= 0)
			return false;

		int start = getLineStartOffset(line);
		int end = getLineEndOffset(line);
		int len = end - start - 1;

		// don't wrap unless we're at the end of the line
		if(getCaretPosition() != end - 1)
			return false;

		boolean returnValue = false;

		int tabSize = buffer.getTabSize();

		String wordBreakChars = (String)buffer.getProperty("wordBreakChars");

		buffer.getLineText(line,lineSegment);

		int lineStart = lineSegment.offset;
		int logicalLength = 0; // length with tabs expanded
		int lastWordOffset = -1;
		boolean lastWasSpace = true;
		boolean initialWhiteSpace = true;
		int initialWhiteSpaceLength = 0;
		for(int i = 0; i < len; i++)
		{
			char ch = lineSegment.array[lineStart + i];
			if(ch == '\t')
			{
				if(initialWhiteSpace)
					initialWhiteSpaceLength = i + 1;
				logicalLength += tabSize - (logicalLength % tabSize);
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(ch == ' ')
			{
				if(initialWhiteSpace)
					initialWhiteSpaceLength = i + 1;
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(wordBreakChars != null && wordBreakChars.indexOf(ch) != -1)
			{
				initialWhiteSpace = false;
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else
			{
				initialWhiteSpace = false;
				logicalLength++;
				lastWasSpace = false;
			}

			int insertNewLineAt;
			if(spaceInserted && logicalLength == maxLineLen
				&& i == len - 1)
			{
				insertNewLineAt = end - 1;
				returnValue = true;
			}
			else if(logicalLength >= maxLineLen && lastWordOffset != -1)
				insertNewLineAt = lastWordOffset + start;
			else
				continue;

			try
			{
				buffer.beginCompoundEdit();
				buffer.insert(insertNewLineAt,"\n");
				buffer.indentLine(line + 1,true,true);
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

	//{{{ completeWord() method
	private String completeWord(String line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.substring(offset,wordEnd);
	} //}}}

	//{{{ updateBracketHighlight() method
	private void updateBracketHighlight()
	{
		if(!painter.isBracketHighlightEnabled())
			return;

		if(bracketLine != -1)
			invalidateLine(bracketLine);

		int line = getCaretLine();
		int offset = getCaretPosition() - getLineStartOffset(line);

		if(offset == 0)
		{
			bracketPosition = bracketLine = -1;
			return;
		}

		int endLine;
		if(visibleLines == 0)
			endLine = buffer.getLineCount();
		else
		{
			endLine = virtualToPhysical(
				Math.min(foldVisibilityManager
				.getVirtualLineCount() - 1,
				firstLine + visibleLines + 1));
		}

		int beginLine = Math.min(line,physFirstLine);

		int bracketOffset = TextUtilities.findMatchingBracket(
			buffer,line,offset - 1,beginLine,endLine);
		if(bracketOffset != -1)
		{
			bracketLine = getLineOfOffset(bracketOffset);
			bracketPosition = bracketOffset
				- getLineStartOffset(bracketLine);
			invalidateLine(bracketLine);
			return;
		}

		bracketLine = bracketPosition = -1;
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

	//{{{ ComponentHandler class
	class ComponentHandler extends ComponentAdapter
	{
		//{{{ componentResized() method
		public void componentResized(ComponentEvent evt)
		{
			recalculateVisibleLines();
			scrollBarsInitialized = true;
		} //}}}
	} //}}}

	//{{{ BufferChangeHandler class
	class BufferChangeHandler implements BufferChangeListener
	{
		//{{{ foldLevelChanged() method
		public void foldLevelChanged(Buffer buffer, int line, int level)
		{
			if(!bufferChanging && line != 0)
				invalidateLine(line - 1);
		} //}}}

		//{{{ contentInserted() method
		public void contentInserted(Buffer buffer, int startLine, int start,
			int numLines, int length)
		{
			if(!buffer.isLoaded())
				return;

			repaintAndScroll(startLine,numLines);

			if(caret >= start)
				moveCaretPosition(caret + length,true);
			else
				updateBracketHighlight();

			// loop through all selections, resizing them if
			// necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if(s.start >= start)
				{
					s.start += length;
					s.startLine = getLineOfOffset(s.start);
					changed = true;
				}

				if(s.end >= start)
				{
					s.end += length;
					s.endLine = getLineOfOffset(s.end);
					changed = true;
				}

				if(changed)
					invalidateLineRange(s.startLine,s.endLine);
			}
		}
		//}}}

		//{{{ contentRemoved() method
		public void contentRemoved(Buffer buffer, int startLine, int start,
			int numLines, int length)
		{
			if(!buffer.isLoaded())
				return;

			// -lineCount because they are removed
			repaintAndScroll(startLine,-numLines);

			int end = start + length;

			boolean caretEvent = false;

			// loop through all selections, resizing them if
			// necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if(s.start > start && s.start <= end)
				{
					s.start = start;
					changed = caretEvent = true;
				}
				else if(s.start > end)
				{
					s.start -= length;
					changed = caretEvent = true;
				}

				if(s.end > start && s.end <= end)
				{
					s.end = start;
					changed = caretEvent = true;
				}
				else if(s.end > end)
				{
					s.end -= length;
					changed = caretEvent = true;
				}

				if(s.start == s.end)
				{
					selection.removeElement(s);
					invalidateLineRange(s.startLine,s.endLine);
					i--;
				}
				else if(changed)
				{
					s.startLine = getLineOfOffset(s.start);
					s.endLine = getLineOfOffset(s.end);
					invalidateLineRange(s.startLine,s.endLine);
				}
			}

			if(caret > start && caret <= end)
				moveCaretPosition(start,false);
			else if(caret > end)
				moveCaretPosition(caret - length,false);
			else
			{
				updateBracketHighlight();

				if(caretEvent)
					fireCaretEvent();
			}
		}
		//}}}

		//{{{ linesChanged() method
		private void repaintAndScroll(int startLine, int numLines)
		{
			if(numLines == 0)
				invalidateLine(startLine);
			// do magic stuff
			else if(startLine < firstLine)
			{
				setFirstLine(firstLine + numLines);
				// calls updateScrollBars()
			}
			// end of magic stuff
			else
			{
				updateScrollBars();

				invalidateVirtualLineRange(
					foldVisibilityManager
					.physicalToVirtual(startLine),
					firstLine + visibleLines);
			}
		} //}}}
	} //}}}

	//{{{ FocusHandler class
	class FocusHandler implements FocusListener
	{
		//{{{ focusGained() method
		public void focusGained(FocusEvent evt)
		{
			invalidateLine(caretLine);

			if(bracketLine != -1)
				invalidateLine(bracketLine);

			// repaint the gutter so that the border color
			// reflects the focus state
			view.updateGutterBorders();
		} //}}}

		//{{{ focusLost() method
		public void focusLost(FocusEvent evt)
		{
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

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
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
						evt.getX()+1,evt.getY()+1);
				}
				return;
			}

			blink = true;
			invalidateLine(caretLine);

			int x = evt.getX();
			int y = evt.getY();

			dragStartLine = virtualToPhysical(yToLine(y));
			dragStartOffset = xToOffset(dragStartLine,x);
			dragStart = xyToOffset(x,y,!painter.isBlockCaretEnabled());

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

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent evt)
		{
			if(getSelectionCount() != 0)
				Registers.setRegister('%',getSelectedText());
		} //}}}

		//{{{ doSingleClick() method
		private void doSingleClick(MouseEvent evt)
		{
			if(evt.isShiftDown())
			{
				// XXX: getMarkPosition() deprecated!
				resizeSelection(getMarkPosition(),dragStart,
					evt.isControlDown());

				moveCaretPosition(dragStart,false);

				// so that shift-click-drag works
				dragStartLine = getMarkLine();
				dragStart = getMarkPosition();
				dragStartOffset = dragStart
					- getLineStartOffset(dragStartLine);
			}
			else
			{
				if(!multi)
					selectNone();

				moveCaretPosition(dragStart,false);

				if(middleMousePaste
					&& (evt.getModifiers() & InputEvent.BUTTON2_MASK) != 0)
				{
					if(!isEditable())
						getToolkit().beep();
					else
						Registers.paste(JEditTextArea.this,'%');
				}
			}
		} //}}}

		//{{{ doDoubleClick() method
		private void doDoubleClick(MouseEvent evt)
		{
			// Ignore empty lines
			if(getLineLength(dragStartLine) == 0)
				return;

			int bracket = TextUtilities.findMatchingBracket(
				buffer,dragStartLine,
				Math.max(0,dragStartOffset - 1));

			if(bracket != -1)
			{
				// Hack
				if(bracket < caret)
				{
					addToSelection(new Selection.Range(
						bracket,caret));
				}
				else
				{
					addToSelection(new Selection.Range(
						caret - 1,++bracket));
				}

				moveCaretPosition(bracket,false);
				return;
			}

			// Ok, it's not a bracket... select the word
			String lineText = getLineText(dragStartLine);
			String noWordSep = (String)buffer.getProperty("noWordSep");
			if(dragStartOffset == getLineLength(dragStartLine))
				dragStartOffset--;

			int wordStart = TextUtilities.findWordStart(lineText,
				dragStartOffset,noWordSep);
			int wordEnd = TextUtilities.findWordEnd(lineText,
				dragStartOffset+1,noWordSep);

			int lineStart = getLineStartOffset(dragStartLine);
			addToSelection(new Selection.Range(lineStart + wordStart,
				lineStart + wordEnd));
			moveCaretPosition(lineStart + wordEnd,false);
		} //}}}

		//{{{ doTripleClick() method
		private void doTripleClick(MouseEvent evt)
		{
			int newCaret = getLineEndOffset(dragStartLine);
			addToSelection(new Selection.Range(
				getLineStartOffset(dragStartLine),
				newCaret));
			moveCaretPosition(newCaret);
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent evt)
		{
			if(GUIUtilities.isPopupTrigger(evt)
				|| (popup != null && popup.isVisible()))
				return;

			boolean rect = evt.isControlDown();

			switch(clickCount)
			{
			case 1:
				doSingleDrag(evt,rect);
				break;
			case 2:
				doDoubleDrag(evt,rect);
				break;
			default: //case 3:
				doTripleDrag(evt,rect);
				break;
			}
		} //}}}

		//{{{ doSingleDrag() method
		private void doSingleDrag(MouseEvent evt, boolean rect)
		{
			int dot = xyToOffset(evt.getX(),evt.getY(),
				!painter.isBlockCaretEnabled());
			if(dot == caret)
				return;

			resizeSelection(dragStart,dot,rect);
			moveCaretPosition(dot,false);
		} //}}}

		//{{{ doDoubleDrag() method
		private void doDoubleDrag(MouseEvent evt, boolean rect)
		{
			int markLineStart = getLineStartOffset(dragStartLine);
			int markLineLength = getLineLength(dragStartLine);
			int mark = dragStartOffset;

			int line = virtualToPhysical(yToLine(evt.getY()));
			int lineStart = getLineStartOffset(line);
			int lineLength = getLineLength(line);
			int offset = xToOffset(line,evt.getX());

			String lineText = getLineText(line);
			String markLineText = getLineText(dragStartLine);
			String noWordSep = (String)buffer.getProperty("noWordSep");

			if(markLineStart + dragStartOffset > lineStart + offset)
			{
				if(offset != 0 && offset != lineLength)
				{
					offset = TextUtilities.findWordStart(
						lineText,offset,noWordSep);
				}

				if(markLineLength != 0)
				{
					mark = TextUtilities.findWordEnd(
						markLineText,mark,noWordSep);
				}
			}
			else
			{
				if(offset != 0 && lineLength != 0)
				{
					offset = TextUtilities.findWordEnd(
						lineText,offset,noWordSep);
				}

				if(mark != 0 && mark != markLineLength)
				{
					mark = TextUtilities.findWordStart(
						markLineText,mark,noWordSep);
				}
			}

			if(lineStart + offset == caret)
				return;

			resizeSelection(markLineStart + mark,lineStart + offset,rect);
			moveCaretPosition(lineStart + offset,false);
		} //}}}

		//{{{ doTripleDrag() method
		private void doTripleDrag(MouseEvent evt, boolean rect)
		{
			int mouseLine = virtualToPhysical(yToLine(evt.getY()));
			int offset = xToOffset(mouseLine,evt.getX());
			int mark;
			int mouse;
			if(dragStartLine > mouseLine)
			{
				mark = getLineEndOffset(dragStartLine) - 1;
				if(offset == getLineLength(mouseLine))
					mouse = getLineEndOffset(mouseLine) - 1;
				else
					mouse = getLineStartOffset(mouseLine);
			}
			else
			{
				mark = getLineStartOffset(dragStartLine);
				if(offset == 0)
					mouse = getLineStartOffset(mouseLine);
				else
					mouse = getLineEndOffset(mouseLine) - 1;
			}

			if(mouse == caret)
				return;

			resizeSelection(mark,mouse,rect);
			moveCaretPosition(mouse,false);
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
