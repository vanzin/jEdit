/*
 * JEditTextArea.java - jEdit's text component
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

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.undo.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * jEdit's text component.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class JEditTextArea extends JComponent
{
	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea(View view)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		this.view = view;

		// Initialize some misc. stuff
		selection = new Vector();
		renderer = TextRenderer.createTextRenderer();
		painter = new TextAreaPainter(this);
		gutter = new Gutter(view,this);
		documentHandler = new DocumentHandler();
		foldHandler = new FoldHandler();
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		bracketLine = bracketPosition = -1;
		blink = true;
		lineSegment = new Segment();

		// Initialize the GUI
		setLayout(new ScrollLayout());
		add(LEFT,gutter);
		add(CENTER,painter);
		add(RIGHT,vertical = new JScrollBar(JScrollBar.VERTICAL));
		add(BOTTOM,horizontal = new JScrollBar(JScrollBar.HORIZONTAL));

		horizontal.setValues(0,0,0,0);

		// this ensures that the text area's look is slightly
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

		// Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());
		painter.addComponentListener(new ComponentHandler());

		mouseHandler = new MouseHandler();
		painter.addMouseListener(mouseHandler);
		painter.addMouseMotionListener(mouseHandler);

		addFocusListener(new FocusHandler());

		// This doesn't seem very correct, but it fixes a problem
		// when setting the initial caret position for a buffer
		// (eg, from the recent file list)
		focusedComponent = this;
	}

	/**
	 * Returns the object responsible for painting this text area.
	 */
	public final TextAreaPainter getPainter()
	{
		return painter;
	}

 	/**
	 * Returns the gutter to the left of the text area or null if the gutter
	 * is disabled
	 */
	public final Gutter getGutter()
	{
		return gutter;
	}

	/**
	 * Returns true if the caret is blinking, false otherwise.
	 */
	public final boolean isCaretBlinkEnabled()
	{
		return caretBlinks;
	}

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
	}

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
	}

	/**
	 * Returns the number of lines from the top and button of the
	 * text area that are always visible.
	 */
	public final int getElectricScroll()
	{
		return electricScroll;
	}

	/**
	 * Sets the number of lines from the top and bottom of the text
	 * area that are always visible
	 * @param electricScroll The number of lines always visible from
	 * the top or bottom
	 */
	public final void setElectricScroll(int electricScroll)
	{
		this.electricScroll = electricScroll;
	}

	/**
	 * Returns if clicking the middle mouse button pastes the most
	 * recent selection (% register).
	 */
	public final boolean isMiddleMousePasteEnabled()
	{
		return middleMousePaste;
	}

	/**
	 * Sets if clicking the middle mouse button pastes the most
	 * recent selection (% register).
	 * @param middleMousePaste A boolean flag
	 */
	public final void setMiddleMousePasteEnabled(boolean middleMousePaste)
	{
		this.middleMousePaste = middleMousePaste;
	}

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
	}

	/**
	 * Returns the line displayed at the text area's origin. This is
	 * a virtual, not a physical, line number.
	 */
	public final int getFirstLine()
	{
		return firstLine;
	}

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
	}

	public void _setFirstLine(int firstLine)
	{
		this.firstLine = Math.max(0,firstLine);
		physFirstLine = buffer.virtualToPhysical(this.firstLine);

		maxHorizontalScrollWidth = 0;

		// hack so that if we scroll and the matching bracket
		// comes into view, it is highlighted

		// 3.2pre9 update: I am commenting this out once again because
		// I have changed the location of the documentChanged() call
		// in the DocumentHandler, so this is called before the caret
		// position is updated, which can be potentially tricky.

		//if(bracketPosition == -1)
		//	updateBracketHighlight();

		if(this.firstLine != vertical.getValue())
			updateScrollBars();

		painter.repaint();
		gutter.repaint();

		fireScrollEvent(true);
	}

	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines()
	{
		return visibleLines;
	}

	/**
	 * Returns the horizontal offset of drawn lines.
	 */
	public final int getHorizontalOffset()
	{
		return horizontalOffset;
	}

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
	}

	public void _setHorizontalOffset(int horizontalOffset)
	{
		this.horizontalOffset = horizontalOffset;
		if(horizontalOffset != horizontal.getValue())
			updateScrollBars();
		painter.repaint();

		fireScrollEvent(false);
	}

	/**
	 * @deprecated Use setFirstLine() and setHorizontalOffset() instead
	 */
	public boolean setOrigin(int firstLine, int horizontalOffset)
	{
		setFirstLine(firstLine);
		setHorizontalOffset(horizontalOffset);
		return true;
	}

	/**
	 * Centers the caret on the screen.
	 * @since jEdit 2.7pre2
	 */
	public void centerCaret()
	{
		Element map = buffer.getDefaultRootElement();

		int gotoLine = buffer.virtualToPhysical(firstLine + visibleLines / 2);

		if(gotoLine < 0 || gotoLine >= map.getElementCount())
		{
			getToolkit().beep();
			return;
		}

		Element element = map.getElement(gotoLine);
		setCaretPosition(element.getStartOffset());
	}

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
	}

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
	}

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
	}

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
	}

	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 * @param doElectricScroll If true, electric scrolling will be performed
	 */
	public void scrollToCaret(boolean doElectricScroll)
	{
		if(!buffer.isLineVisible(caretLine))
			buffer.expandFoldAt(caretLine,true,this);

		int offset = caret - getLineStartOffset(caretLine);
		int virtualCaretLine = buffer.physicalToVirtual(caretLine);

		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if(visibleLines == 0)
		{
			setFirstLine(caretLine - electricScroll);
			return;
		}

		int lineCount = getVirtualLineCount();
		int _lastLine = firstLine + visibleLines;

		int electricScroll;

		if(doElectricScroll && visibleLines > this.electricScroll * 2)
			electricScroll = this.electricScroll;
		else
			electricScroll = 0;

		boolean changed = false;

		int _firstLine = (firstLine == 0 ? 0 : firstLine + electricScroll);
		if(_lastLine >= lineCount - 1)
			_lastLine = lineCount - 1;
		else
			_lastLine -= electricScroll;
		if(virtualCaretLine > _firstLine && virtualCaretLine < _lastLine)
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
				startLine = buffer.physicalToVirtual(s.startLine);
				endLine = buffer.physicalToVirtual(s.endLine);
			}

			if(endLine - startLine <= visibleLines)
				firstLine = (startLine + endLine - visibleLines) / 2;
			else
				firstLine = buffer.physicalToVirtual(caretLine) - visibleLines / 2;

			firstLine = Math.min(firstLine,buffer.getVirtualLineCount()
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

			physFirstLine = buffer.virtualToPhysical(firstLine);

			updateScrollBars();
			painter.repaint();
			gutter.repaint();

			view.synchroScrollVertical(this,firstLine);
			view.synchroScrollHorizontal(this,horizontalOffset);

			// fire events for both a horizontal and vertical scroll
			fireScrollEvent(true);
			fireScrollEvent(false);
		}
	}

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
	}

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
	}

	/**
	 * Returns the text renderer instance. This method is going away in
	 * the next major release, so do not use it.
	 * @since jEdit 3.2pre6
	 */
	public TextRenderer getTextRenderer()
	{
		return renderer;
	}

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
	}

	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		return xToOffset(line,x,true);
	}

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
	}

	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		return xyToOffset(x,y,true);
	}

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
			return getLineEndOffset(buffer.virtualToPhysical(
				buffer.getVirtualLineCount() - 1)) - 1;
		}
		else
		{
			line = buffer.virtualToPhysical(line);
			return getLineStartOffset(line) + xToOffset(line,x);
		}
	}

	/**
	 * Marks a line as needing a repaint.
	 * @param line The line to invalidate
	 */
	public final void invalidateLine(int line)
	{
		line = buffer.physicalToVirtual(line);

		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(line) + fm.getDescent() + fm.getLeading();
		painter.repaint(0,y,painter.getWidth(),fm.getHeight());
		gutter.repaint(0,y,gutter.getWidth(),fm.getHeight());
	}

	/**
	 * Marks a range of lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public final void invalidateLineRange(int firstLine, int lastLine)
	{
		firstLine = buffer.physicalToVirtual(firstLine);

		// all your bugs are belong to us
		if(lastLine > buffer.virtualToPhysical(
			buffer.getVirtualLineCount() - 1))
		{
			lastLine = (lastLine - buffer.getLineCount())
				+ buffer.getVirtualLineCount();
		}
		else
			lastLine = buffer.physicalToVirtual(lastLine);

		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(firstLine) + fm.getDescent() + fm.getLeading();
		int height = (lastLine - firstLine + 1) * fm.getHeight();
		painter.repaint(0,y,painter.getWidth(),height);
		gutter.repaint(0,y,gutter.getWidth(),height);
	}

	/**
	 * Repaints the lines containing the selection.
	 */
	public final void invalidateSelectedLines()
	{
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			invalidateLineRange(s.startLine,s.endLine);
		}
	}

	/**
	 * Returns the buffer this text area is editing.
	 */
	public final Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Sets the buffer this text area is editing.
	 * @param buffer The buffer
	 */
	public void setBuffer(Buffer buffer)
	{
		if(this.buffer == buffer)
			return;
		if(this.buffer != null)
		{
			this.buffer.removeDocumentListener(documentHandler);
			this.buffer.removeFoldListener(foldHandler);
		}
		this.buffer = buffer;

		buffer.addDocumentListener(documentHandler);
		buffer.addFoldListener(foldHandler);
		documentHandlerInstalled = true;

		maxHorizontalScrollWidth = 0;

		painter.updateTabSize();

		setCaretPosition(0);

		updateScrollBars();
		painter.repaint();
		gutter.repaint();
	}

	/**
	 * Returns the length of the buffer. Equivalent to calling
	 * <code>getBuffer().getLength()</code>.
	 */
	public final int getBufferLength()
	{
		return buffer.getLength();
	}

	/**
	 * Returns the number of lines in the document.
	 */
	public final int getLineCount()
	{
		return buffer.getLineCount();
	}

	/**
	 * Returns the number of visible lines in the document (which may
	 * be less than the total due to folding).
	 * @since jEdit 3.1pre1
	 */
	public final int getVirtualLineCount()
	{
		return buffer.getVirtualLineCount();
	}

	/**
	 * Returns the line containing the specified offset.
	 * @param offset The offset
	 */
	public final int getLineOfOffset(int offset)
	{
		return buffer.getDefaultRootElement().getElementIndex(offset);
	}

	/**
	 * Returns the start offset of the specified line.
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is
	 * invalid
	 */
	public int getLineStartOffset(int line)
	{
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getStartOffset();
	}

	/**
	 * Returns the end offset of the specified line.
	 * @param line The line
	 * @return The end offset of the specified line, or -1 if the line is
	 * invalid.
	 */
	public int getLineEndOffset(int line)
	{
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getEndOffset();
	}

	/**
	 * Returns the length of the specified line.
	 * @param line The line
	 */
	public int getLineLength(int line)
	{
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getEndOffset()
				- lineElement.getStartOffset() - 1;
	}

	/**
	 * Returns the entire text of this text area.
	 */
	public String getText()
	{
		try
		{
			return buffer.getText(0,buffer.getLength());
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the entire text of this text area.
	 */
	public void setText(String text)
	{
		try
		{
			buffer.beginCompoundEdit();
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,text,null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	}

	/**
	 * Returns the specified substring of the buffer.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring, or null if the offsets are invalid
	 */
	public final String getText(int start, int len)
	{
		try
		{
			return buffer.getText(start,len);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Copies the specified substring of the buffer into a segment.
	 * If the offsets are invalid, the segment will contain a null string.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		try
		{
			buffer.getText(start,len,segment);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			segment.offset = segment.count = 0;
		}
	}

	/**
	 * Returns the text on the specified line.
	 * @param lineIndex The line
	 * @return The text, or null if the line is invalid
	 */
	public final String getLineText(int lineIndex)
	{
		int start = getLineStartOffset(lineIndex);
		return getText(start,getLineEndOffset(lineIndex) - start - 1);
	}

	/**
	 * Copies the text on the specified line into a segment. If the line
	 * is invalid, the segment will contain a null string.
	 * @param lineIndex The line
	 */
	public final void getLineText(int lineIndex, Segment segment)
	{
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(lineIndex);
		int start = lineElement.getStartOffset();
		getText(start,lineElement.getEndOffset() - start - 1,segment);
	}

	/**
	 * Selects all text in the buffer.
	 */
	public final void selectAll()
	{
		setSelection(new Selection.Range(0,buffer.getLength()));
		moveCaretPosition(buffer.getLength(),true);
	}

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
	}

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
	}

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
	}

	// OLD (NON-MULTI AWARE) SELECTION API
		/**
		 * @deprecated Instead, obtain a Selection instance using
		 * any means, and call its <code>getStart()</code> method
		 */
		public final int getSelectionStart()
		{
			if(selection.size() != 1)
				return caret;

			return ((Selection)selection.elementAt(0)).getStart();
		}

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
		}

		/**
		 * @deprecated Instead, obtain a Selection instance using
		 * any means, and call its <code>getStartLine()</code> method
		 */
		public final int getSelectionStartLine()
		{
			if(selection.size() != 1)
				return caret;

			return ((Selection)selection.elementAt(0)).getStartLine();
		}

		/**
		 * @deprecated Do not use.
		 */
		public final void setSelectionStart(int selectionStart)
		{
			select(selectionStart,getSelectionEnd(),true);
		}

		/**
		 * @deprecated Instead, obtain a Selection instance using
		 * any means, and call its <code>getEnd()</code> method
		 */
		public final int getSelectionEnd()
		{
			if(selection.size() != 1)
				return caret;

			return ((Selection)selection.elementAt(0)).getEnd();
		}

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
		}

		/**
		 * @deprecated Instead, obtain a Selection instance using
		 * any means, and call its <code>getEndLine()</code> method
		 */
		public final int getSelectionEndLine()
		{
			if(selection.size() != 1)
				return caret;

			return ((Selection)selection.elementAt(0)).getEndLine();
		}

		/**
		 * @deprecated Do not use.
		 */
		public final void setSelectionEnd(int selectionEnd)
		{
			select(getSelectionStart(),selectionEnd,true);
		}

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
		}

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
		}

		/**
		 * @deprecated Instead, call either <code>addToSelection()</code>,
		 * or <code>setSelection()</code> with a new Selection instance.
		 */
		public void select(int start, int end)
		{
			select(start,end,true);
		}

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
		}

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
		}
	// OLD SELECTION API ENDS HERE

	/**
	 * Sets the caret position and deactivates the selection.
	 * @param caret The caret position
	 */
	public void setCaretPosition(int newCaret)
	{
		invalidateSelectedLines();
		selection.removeAllElements();
		moveCaretPosition(newCaret,true);
	}

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
	}

	/**
	 * Sets the caret position without deactivating the selection.
	 * @param caret The caret position
	 */
	public void moveCaretPosition(int newCaret)
	{
		moveCaretPosition(newCaret,true);
	}

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

		magicCaret = offsetToX(newCaretLine,newCaret
			- getLineStartOffset(newCaretLine));

		// call invalidateLine() twice, as opposed to calling
		// invalidateLineRange(), because invalidateLineRange()
		// doesn't handle start > end
		invalidateLine(caretLine);
		invalidateLine(newCaretLine);

		buffer.addUndoableEdit(new CaretUndo(caret));

		caret = newCaret;
		caretLine = newCaretLine;

		if(focusedComponent == this)
			scrollToCaret(doElectricScroll);

		updateBracketHighlight();

		fireCaretEvent();
	}

	/**
	 * Returns the caret position.
	 */
	public int getCaretPosition()
	{
		return caret;
	}

	/**
	 * Returns the line number containing the caret.
	 */
	public int getCaretLine()
	{
		return caretLine;
	}

	/**
	 * Returns the number of selections. This is primarily for use by the
	 * the status bar.
	 * @since jEdit 3.2pre2
	 */
	public int getSelectionCount()
	{
		return selection.size();
	}

	/**
	 * Returns the current selection.
	 * @since jEdit 3.2pre1
	 */
	public Selection[] getSelection()
	{
		Selection[] sel = new Selection[selection.size()];
		selection.copyInto(sel);
		return sel;
	}

	/**
	 * Deselects everything.
	 */
	public void selectNone()
	{
		setSelection((Selection)null);
	}

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
	}

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
	}

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
	}

	/**
	 * Adds to the selection.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection selection)
	{
		_addToSelection(selection);
		fireCaretEvent();
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

	/**
	 * Returns the text in all active selections, with a newline
	 * between each text chunk.
	 */
	public String getSelectedText()
	{
		return getSelectedText("\n");
	}

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
				Element map = buffer.getDefaultRootElement();

				int start = s.start - map.getElement(s.startLine)
					.getStartOffset();
				int end = s.end - map.getElement(s.endLine)
					.getStartOffset();

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
					Element lineElement = map.getElement(i);
					int lineStart = lineElement.getStartOffset();
					int lineEnd = lineElement.getEndOffset() - 1;
					int rectStart = Math.min(lineEnd,lineStart + start);

					buffer.remove(rectStart,Math.min(lineEnd - rectStart,
						end - start));

					if(selectedText == null)
						continue;

					currNewline = selectedText.indexOf('\n',lastNewline);
					if(currNewline == -1)
						currNewline = selectedText.length();

					buffer.insertString(rectStart,selectedText
						.substring(lastNewline,currNewline),null);

					lastNewline = Math.min(selectedText.length(),
						currNewline + 1);
				}

				if(selectedText != null &&
					currNewline != selectedText.length())
				{
					int offset = map.getElement(s.endLine)
						.getEndOffset() - 1;
					buffer.insertString(offset,"\n",null);
					buffer.insertString(offset + 1,selectedText
						.substring(currNewline + 1),null);
				}
			}
			else
			{
				buffer.remove(s.start,s.end - s.start);
				if(selectedText != null && selectedText.length() != 0)
				{
					buffer.insertString(s.start,
						selectedText,null);
				}
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		// No matter what happends... stops us from leaving buffer
		// in a bad state
		finally
		{
			buffer.endCompoundEdit();
		}

		// no no no!!!!
		//selectNone();
	}

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
			try
			{
				buffer.insertString(caret,selectedText,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
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
	}

	/**
	 * Returns an array of all line numbers that contain a selection.
	 * This array will also include the line number containing the
	 * caret, for convinience.
	 * @since jEdit 3.2pre1
	 */
	public int[] getSelectedLines()
	{
		Integer line;

		// this algorithm sucks
		Hashtable hash = new Hashtable();
		for(int i = 0; i < selection.size(); i++)
		{
			Selection s = (Selection)selection.elementAt(i);
			for(int j = s.startLine; j <= s.endLine; j++)
			{
				line = new Integer(j);
				hash.put(line,line);
			}
		}

		line = new Integer(caretLine);
		hash.put(line,line);

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
	}

	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable()
	{
		return buffer.isEditable();
	}

	/**
	 * Returns the right click popup menu.
	 */
	public final JPopupMenu getRightClickPopup()
	{
		return popup;
	}

	/**
	 * Sets the right click popup menu.
	 * @param popup The popup
	 */
	public final void setRightClickPopup(JPopupMenu popup)
	{
		this.popup = popup;
	}

	/**
	 * Returns the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 */
	public final int getMagicCaretPosition()
	{
		return (magicCaret == -1
			? offsetToX(caretLine,caret - getLineStartOffset(caretLine))
			: magicCaret);
	}

	/**
	 * Sets the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 * @param magicCaret The magic caret position
	 */
	public final void setMagicCaretPosition(int magicCaret)
	{
		this.magicCaret = magicCaret;
	}

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
			if(buffer.getBooleanProperty("indentOnTab")
				&& selection.size() == 0
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

			try
			{
				if(ch == ' ')
				{
					if(doWordWrap(caretLine,true))
						return;
				}
				else
					doWordWrap(caretLine,false);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}

			try
			{
				buffer.beginCompoundEdit();

				// Don't overstrike if we're on the end of
				// the line
				if(overwrite)
				{
					int caretLineEnd = getLineEndOffset(caretLine);
					if(caretLineEnd - caret > 1)
						buffer.remove(caret,1);
				}

				buffer.insertString(caret,str,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			finally
			{
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
	}

	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public final boolean isOverwriteEnabled()
	{
		return overwrite;
	}

	/**
	 * Sets overwrite mode.
	 */
	public final void setOverwriteEnabled(boolean overwrite)
	{
		this.overwrite = overwrite;
		invalidateLine(caretLine);
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	}

	/**
	 * Toggles overwrite mode.
	 * @since jEdit 2.7pre2
	 */
	public final void toggleOverwriteEnabled()
	{
		overwrite = !overwrite;
		invalidateLine(caretLine);
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	}

	/**
	 * Returns the position of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketPosition()
	{
		return bracketPosition;
	}

	/**
	 * Returns the line of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketLine()
	{
		return bracketLine;
	}

	/**
	 * Adds a caret change listener to this text area.
	 * @param listener The listener
	 */
	public final void addCaretListener(CaretListener listener)
	{
		listenerList.add(CaretListener.class,listener);
	}

	/**
	 * Removes a caret change listener from this text area.
	 * @param listener The listener
	 */
	public final void removeCaretListener(CaretListener listener)
	{
		listenerList.remove(CaretListener.class,listener);
	}

	/**
	 * Adds a scroll listener to this text area.
	 * @param listener The listener
	 * @since jEdit 3.2pre2
	 */
	public final void addScrollListener(ScrollListener listener)
	{
		listenerList.add(ScrollListener.class,listener);
	}

	/**
	 * Removes a scroll listener from this text area.
	 * @param listener The listener
	 * @since jEdit 3.2pre2
	 */
	public final void removeScrollListener(ScrollListener listener)
	{
		listenerList.remove(ScrollListener.class,listener);
	}

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
			try
			{
				buffer.remove(caret - 1,1);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}
	}

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

		try
		{
			buffer.remove(_caret + lineStart,
				caret - (_caret + lineStart));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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
			try
			{
				buffer.remove(caret,1);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}
	}

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

		try
		{
			buffer.remove(caret,getLineEndOffset(caretLine)
				- caret - 1);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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

		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(caretLine);
		try
		{
			int start = lineElement.getStartOffset();
			int end = lineElement.getEndOffset();
			if(end > buffer.getLength())
			{
				if(start != 0)
					start--;
				end--;
			}
			buffer.remove(start,end - start);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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
			//if(!buffer.isLineVisible(i))
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
			//if(!buffer.isLineVisible(i))
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

		try
		{
			buffer.remove(start,end - start);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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

		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(caretLine);

		try
		{
			buffer.remove(lineElement.getStartOffset(),
				caret - lineElement.getStartOffset());
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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

		try
		{
			buffer.remove(caret,(_caret + lineStart) - caret);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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
	}

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
			int line = buffer.getNextVisibleLine(caretLine);
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
	}

	/**
	 * Movse the caret to the next line.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextLine(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();

		int magic = getMagicCaretPosition();

		int nextLine = buffer.getNextVisibleLine(line);

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
	}

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
	}

	/**
	 * Moves the caret to the next screenful.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextPage(boolean select)
	{
		int lineCount = buffer.getVirtualLineCount();

		int magic = getMagicCaretPosition();

		if(firstLine + visibleLines * 2 >= lineCount - 1)
			setFirstLine(lineCount - visibleLines);
		else
			setFirstLine(firstLine + visibleLines);

		int newLine = buffer.virtualToPhysical(Math.min(lineCount - 1,
			buffer.physicalToVirtual(caretLine) + visibleLines));
		int newCaret = getLineStartOffset(newLine)
			+ xToOffset(newLine,magic + 1);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);

		setMagicCaretPosition(magic);
	}

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
			if(!buffer.isLineVisible(i))
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
	}

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
			int nextLine = buffer.getNextVisibleLine(caretLine);
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
	}

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
	}

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
			int line = buffer.getPrevVisibleLine(caretLine);
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
	}

	/**
	 * Moves the caret to the previous line.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevLine(boolean select)
	{
		int magic = getMagicCaretPosition();

		int prevLine = buffer.getPrevVisibleLine(caretLine);
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
	}

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
	}

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

		int newLine = buffer.virtualToPhysical(Math.max(0,
			buffer.physicalToVirtual(caretLine) - visibleLines));
		int newCaret = getLineStartOffset(newLine)
			+ xToOffset(newLine,magic + 1);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();

		moveCaretPosition(newCaret);
		setMagicCaretPosition(magic);
	}

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
			if(!buffer.isLineVisible(i))
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
	}

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
				int prevLine = buffer.getPrevVisibleLine(caretLine);
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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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

		firstVisibleLine = buffer.virtualToPhysical(firstVisibleLine);

		int firstVisible = getLineEndOffset(firstVisibleLine) - 1;

		if(select)
			extendSelection(caret,firstVisible);
		else if(!multi)
			selectNone();
		moveCaretPosition(firstVisible);
	}

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

		lastVisibleLine = buffer.virtualToPhysical(lastVisibleLine);

		int lastVisible = getLineEndOffset(lastVisibleLine) - 1;

		if(select)
			extendSelection(caret,lastVisible);
		else if(!multi)
			selectNone();
		moveCaretPosition(lastVisible);
	}

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
	}

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
	}

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
	}

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
				buffer.insertString(getLineStartOffset(lines[i]),
					comment,null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		selectNone();
	}

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
				buffer.insertString(caret,
					commentStart,null);
				buffer.insertString(caret,
					commentEnd,null);
				setCaretPosition(oldCaret + commentStart.length());
			}

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				if(s instanceof Selection.Range)
				{
					buffer.insertString(s.start,
						commentStart,null);
					buffer.insertString(s.end,
						commentEnd,null);
				}
				else if(s instanceof Selection.Rect)
				{
					for(int j = s.startLine; j <= s.endLine; j++)
					{
						buffer.insertString(s.getStart(buffer,j),
							commentStart,null);
						int end = s.getEnd(buffer,j)
							+ (j == s.endLine
							? 0
							: commentStart.length());
						buffer.insertString(end,commentEnd,null);
					}
				}
			}

			selectNone();
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	}

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
				buffer.insertString(start,TextUtilities.format(
					text,maxLineLength),null);
			}
			catch(BadLocationException bl)
			{
				return;
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

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
	}

	/**
	 * Joins the current and the next line.
	 * @since jEdit 2.7pre2
	 */
	public void joinLines()
	{
		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(caretLine);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		if(end > buffer.getLength())
		{
			getToolkit().beep();
			return;
		}
		Element nextLineElement = map.getElement(caretLine + 1);
		int nextStart = nextLineElement.getStartOffset();
		int nextEnd = nextLineElement.getEndOffset();
		try
		{
			buffer.remove(end - 1,MiscUtilities.getLeadingWhiteSpace(
				buffer.getText(nextStart,nextEnd - nextStart)) + 1);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * @since jEdit 2.7pre3
	 */
	public void goToMatchingBracket()
	{
		int dot = caret - getLineStartOffset(caretLine);

		try
		{
			int bracket = TextUtilities.findMatchingBracket(
				buffer,caretLine,Math.max(0,dot - 1));
			if(bracket != -1)
			{
				setCaretPosition(bracket + 1);
				return;
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		getToolkit().beep();
	}

	// Eliminates lots of switch() statements
	private final String openBrackets = "([{";
	private final String closeBrackets = ")]}";

	/**
	 * Selects the code block surrounding the caret.
	 * @since jEdit 2.7pre2
	 */
	public void selectBlock()
	{
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
	}

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
	}

	/**
	 * Displays the 'select line range' dialog box, and selects the
	 * specified range of lines.
	 * @since jEdit 2.7pre2
	 */
	public void showSelectLineRangeDialog()
	{
		new SelectLineRange(view);
	}

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
		try
		{
			doWordCount(view,buffer.getText(0,buffer.getLength()));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

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
				* (buffer.physicalToVirtual(caretLine)
				- firstLine + 1));
			SwingUtilities.convertPointToScreen(location,painter);
			new CompleteWord(view,word,completions,location);
		}
	}

	/**
	 * Selects the fold that contains the caret line number.
	 * @since jEdit 3.1pre3
	 */
	public void selectFold()
	{
		selectFoldAt(caretLine);
	}

	/**
	 * Selects the fold that contains the specified line number.
	 * @param line The line number
	 * @since jEdit 3.1pre3
	 */
	public void selectFoldAt(int line)
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
	}

	/**
	 * Called by the AWT when this component is added to a parent.
	 * Adds document listener.
	 */
	public void addNotify()
	{
		super.addNotify();

		ToolTipManager.sharedInstance().registerComponent(painter);
		ToolTipManager.sharedInstance().registerComponent(gutter);

		if(!documentHandlerInstalled)
		{
			documentHandlerInstalled = true;
			buffer.addDocumentListener(documentHandler);
			buffer.addFoldListener(foldHandler);
		}

		recalculateVisibleLines();
	}

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

		if(documentHandlerInstalled)
		{
			buffer.removeDocumentListener(documentHandler);
			buffer.removeFoldListener(foldHandler);
			documentHandlerInstalled = false;
		}
	}

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
	}

	/**
	 * Bug workarounds.
	 * @since jEdit 2.7pre1
	 */
	public void grabFocus()
	{
		super.grabFocus();
		// ensure that focusedComponent is set correctly
		hasFocus();
	}

	/**
	 * Java 1.4 compatibility fix to make Tab key work.
	 * @since jEdit 3.2pre4
	 */
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	}

	/**
	 * Returns if multiple selection is enabled.
	 * @since jEdit 3.2pre1
	 */
	public final boolean isMultipleSelectionEnabled()
	{
		return multi;
	}

	/**
	 * Toggles multiple selection.
	 * @since jEdit 3.2pre1
	 */
	public final void toggleMultipleSelectionEnabled()
	{
		multi = !multi;
		if(view.getStatus() != null)
			view.getStatus().updateMiscStatus();
	}

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

	// protected members
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
	}

	// package-private members
	Segment lineSegment;
	MouseHandler mouseHandler;
	int maxHorizontalScrollWidth;

	// this is package-private so that the painter can use it without
	// having to call getSelection() (which involves an array copy)
	Vector selection;

	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	final boolean isCaretVisible()
	{
		return blink && hasFocus();
	}

	/**
	 * Returns true if the line and bracket is visible, false otherwise.
	 */
	final boolean isHighlightVisible()
	{
		return hasFocus();
	}

	/**
	 * Recalculates the number of visible lines. This should not
	 * be called directly.
	 */
	void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		updateScrollBars();
	}

	void updateMaxHorizontalScrollWidth()
	{
		int _maxHorizontalScrollWidth = buffer.getMaxLineWidth(
			physFirstLine,visibleLines);
		if(_maxHorizontalScrollWidth != maxHorizontalScrollWidth)
		{
			maxHorizontalScrollWidth = _maxHorizontalScrollWidth;
			horizontal.setValues(-horizontalOffset,painter.getWidth(),
				0,maxHorizontalScrollWidth
				+ painter.getFontMetrics().charWidth('w'));
		}
	}

	// private members
	private static String CENTER = "center";
	private static String RIGHT = "right";
	private static String LEFT = "left";
	private static String BOTTOM = "bottom";

	private static Timer caretTimer;
	private static JEditTextArea focusedComponent;

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

	private Buffer buffer;
	private DocumentHandler documentHandler;
	private FoldHandler foldHandler;
	private boolean documentHandlerInstalled;

	private int caret;
	private int caretLine;

	private int bracketPosition;
	private int bracketLine;

	private int magicCaret;

	private static boolean multi;
	private boolean overwrite;

	private TextRenderer renderer;

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
	}

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

		selection.addElement(addMe);

		invalidateLineRange(addMe.startLine,addMe.endLine);
	}

	private boolean _selectionsOverlap(Selection s1, Selection s2)
	{
		if((s1.start >= s2.start && s1.start <= s2.end)
			|| (s1.end >= s2.start && s1.end <= s2.end))
			return true;
		else
			return false;
	}

	private void getSelectedText(Selection s, StringBuffer buf)
	{
		if(s instanceof Selection.Rect)
		{
			// Return each row of the selection on a new line
			Element map = buffer.getDefaultRootElement();

			int start = s.start - map.getElement(s.startLine)
				.getStartOffset();
			int end = s.end - map.getElement(s.endLine)
				.getStartOffset();

			// Certain rectangles satisfy this condition...
			if(end < start)
			{
				int tmp = end;
				end = start;
				start = tmp;
			}

			for(int i = s.startLine; i <= s.endLine; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				int lineEnd = lineElement.getEndOffset() - 1;
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
	}

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
	}

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
	}

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
	}

	private boolean doWordWrap(int line, boolean spaceInserted)
		throws BadLocationException
	{
		int maxLineLen = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();

		if(maxLineLen <= 0)
			return false;

		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		int len = end - start - 1;

		// don't wrap unless we're at the end of the line
		if(getCaretPosition() != end - 1)
			return false;

		boolean returnValue = false;

		int tabSize = buffer.getTabSize();

		String wordBreakChars = (String)buffer.getProperty("wordBreakChars");

		buffer.getText(start,len,lineSegment);

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
				buffer.insertString(insertNewLineAt,"\n",null);
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
	}

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
	}

	// return word that starts at 'offset'
	private String completeWord(String line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.substring(offset,wordEnd);
	}

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
			endLine = Math.min(buffer.getLineCount(),
				buffer.virtualToPhysical(
				firstLine + visibleLines));
		}

		int beginLine = Math.min(line,physFirstLine);

		try
		{
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
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		bracketLine = bracketPosition = -1;
	}

	private void documentChanged(DocumentEvent evt)
	{
		DocumentEvent.ElementChange ch = evt.getChange(
			buffer.getDefaultRootElement());

		int count;
		if(ch == null)
			count = 0;
		else
			count = ch.getChildrenAdded().length -
				ch.getChildrenRemoved().length;

		int line = getLineOfOffset(evt.getOffset());
		if(count == 0)
			invalidateLine(line);
		// do magic stuff
		else if(line < firstLine)
		{
			setFirstLine(firstLine + count);
			// calls updateScrollBars()
		}
		// end of magic stuff
		else
		{
			updateScrollBars();
			invalidateLineRange(line,buffer.virtualToPhysical(
				firstLine + visibleLines));
		}
	}

	static class TextAreaBorder extends AbstractBorder
	{
		private static final Insets insets = new Insets(1, 1, 2, 2);

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
		}

		public Insets getBorderInsets(Component c)
		{
			return new Insets(1,1,2,2);
		}
	}

	class ScrollLayout implements LayoutManager
	{
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
		}

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
		}

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
		}

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
		}

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
		}

		Component center;
		Component left;
		Component right;
		Component bottom;
	}

	static class CaretBlinker implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(focusedComponent != null && focusedComponent.hasFocus())
				focusedComponent.blinkCaret();
		}
	}

	class MutableCaretEvent extends CaretEvent
	{
		MutableCaretEvent()
		{
			super(JEditTextArea.this);
		}

		public int getDot()
		{
			return getCaretPosition();
		}

		public int getMark()
		{
			return getMarkPosition();
		}
	}

	class AdjustHandler implements AdjustmentListener
	{
		public void adjustmentValueChanged(final AdjustmentEvent evt)
		{
			if(!scrollBarsInitialized)
				return;

			if(evt.getAdjustable() == vertical)
				setFirstLine(vertical.getValue());
			else
				setHorizontalOffset(-horizontal.getValue());
		}
	}

	class ComponentHandler extends ComponentAdapter
	{
		public void componentResized(ComponentEvent evt)
		{
			recalculateVisibleLines();
			scrollBarsInitialized = true;
		}
	}

	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			if(!buffer.isLoaded())
				return;

			documentChanged(evt);

			int offset = evt.getOffset();
			int length = evt.getLength();

			if(caret >= offset)
				moveCaretPosition(caret + length,true);
			else
				updateBracketHighlight();

			// loop through all selections, resizing them if
			// necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if(s.start >= offset)
				{
					s.start += length;
					s.startLine = getLineOfOffset(s.start);
					changed = true;
				}

				if(s.end >= offset)
				{
					s.end += length;
					s.endLine = getLineOfOffset(s.end);
					changed = true;
				}

				if(changed)
					invalidateLineRange(s.startLine,s.endLine);
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			if(!buffer.isLoaded())
				return;

			documentChanged(evt);

			int offset = evt.getOffset();
			int length = evt.getLength();
			int end = offset + length;

			boolean caretEvent = false;

			// loop through all selections, resizing them if
			// necessary
			for(int i = 0; i < selection.size(); i++)
			{
				Selection s = (Selection)selection.elementAt(i);

				boolean changed = false;

				if(s.start > offset && s.start <= end)
				{
					s.start = offset;
					changed = caretEvent = true;
				}
				else if(s.start > end)
				{
					s.start -= length;
					changed = caretEvent = true;
				}

				if(s.end > offset && s.end <= end)
				{
					s.end = offset;
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

			if(caret > offset && caret <= end)
				moveCaretPosition(offset,false);
			else if(caret > end)
				moveCaretPosition(caret - length,false);
			else
			{
				updateBracketHighlight();

				if(caretEvent)
					fireCaretEvent();
			}
		}

		public void changedUpdate(DocumentEvent evt) {}
	}

	class FoldHandler implements Buffer.FoldListener
	{
		public void foldLevelsChanged(int firstLine, int lastLine)
		{
			invalidateLineRange(firstLine,lastLine);
		}

		public void foldStructureChanged()
		{
			// recalculate first line
			setFirstLine(buffer.physicalToVirtual(physFirstLine));

			// update scroll bars because the number of
			// virtual lines might have changed
			updateScrollBars();

			// repaint gutter and painter
			gutter.repaint();

			// this should really go elsewhere!!!
			if(view.getTextArea() == JEditTextArea.this)
				view.getStatus().updateFoldStatus();
		}
	}

	class FocusHandler implements FocusListener
	{
		public void focusGained(FocusEvent evt)
		{
			invalidateLine(caretLine);

			// repaint the gutter so that the border color
			// reflects the focus state
			view.updateGutterBorders();
		}

		public void focusLost(FocusEvent evt)
		{
			invalidateLine(caretLine);
		}
	}

	class MouseHandler extends MouseAdapter implements MouseMotionListener
	{
		private int dragStartLine;
		private int dragStartOffset;
		private int dragStart;
		private int clickCount;

		public void mousePressed(MouseEvent evt)
		{
			buffer.endCompoundEdit();

			grabFocus();

			if(GUIUtilities.isPopupTrigger(evt) && popup != null)
			{
				if(popup.isVisible())
					popup.setVisible(false);
				else
					popup.show(painter,evt.getX()+1,evt.getY()+1);
				return;
			}

			blink = true;
			invalidateLine(caretLine);

			int x = evt.getX();
			int y = evt.getY();

			dragStartLine = buffer.virtualToPhysical(yToLine(y));
			dragStartOffset = xToOffset(dragStartLine,x);
			dragStart = xyToOffset(x,y,!painter.isBlockCaretEnabled());

			clickCount = evt.getClickCount();
			switch(clickCount)
			{
			case 1:
				doSingleClick(evt);
				break;
			case 2:
				// It uses the bracket matching stuff, so
				// it can throw a BLE
				try
				{
					doDoubleClick(evt);
				}
				catch(BadLocationException bl)
				{
					bl.printStackTrace();
				}
				break;
			default: //case 3:
				doTripleClick(evt);
				break;
			}
		}

		public void mouseReleased(MouseEvent evt)
		{
			if(getSelectionCount() != 0)
				Registers.setRegister('%',getSelectedText());
		}

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
		}

		private void doDoubleClick(MouseEvent evt)
			throws BadLocationException
		{
			// Ignore empty lines
			if(getLineLength(dragStartLine) == 0)
				return;

			try
			{
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
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
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
		}

		private void doTripleClick(MouseEvent evt)
		{
			int newCaret = getLineEndOffset(dragStartLine);
			addToSelection(new Selection.Range(
				getLineStartOffset(dragStartLine),
				newCaret));
			moveCaretPosition(newCaret);
		}

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
		}

		public void mouseMoved(MouseEvent evt) {}

		private void doSingleDrag(MouseEvent evt, boolean rect)
		{
			int dot = xyToOffset(evt.getX(),evt.getY(),
				!painter.isBlockCaretEnabled());
			if(dot == caret)
				return;

			resizeSelection(dragStart,dot,rect);
			moveCaretPosition(dot,false);
		}

		private void doDoubleDrag(MouseEvent evt, boolean rect)
		{
			int markLineStart = getLineStartOffset(dragStartLine);
			int markLineLength = getLineLength(dragStartLine);
			int mark = dragStartOffset;

			int line = buffer.virtualToPhysical(yToLine(evt.getY()));
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
		}

		private void doTripleDrag(MouseEvent evt, boolean rect)
		{
			int mouseLine = buffer.virtualToPhysical(yToLine(evt.getY()));
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
		}
	}

	static class CaretUndo extends AbstractUndoableEdit
	{
		private int caret;

		CaretUndo(int caret)
		{
			this.caret = caret;
		}

		public boolean isSignificant()
		{
			return false;
		}

		public String getPresentationName()
		{
			return "caret move";
		}

		public void undo() throws CannotUndoException
		{
			super.undo();

			if(focusedComponent != null)
			{
				int length = focusedComponent
					.getBuffer().getLength();
				if(caret <= length)
				{
					focusedComponent.selectNone();
					focusedComponent.setCaretPosition(caret);
				}
				else
					Log.log(Log.WARNING,this,
						caret + " > " + length + "??!!");
			}
		}

		public boolean addEdit(UndoableEdit edit)
		{
			if(edit instanceof CaretUndo)
			{
				edit.die();

				return true;
			}
			else
				return false;
		}

		public String toString()
		{
			return getPresentationName() + "[caret=" + caret + "]";
		}
	}

	static
	{
		caretTimer = new Timer(500,new CaretBlinker());
		caretTimer.setInitialDelay(500);
		caretTimer.start();
	}
}
