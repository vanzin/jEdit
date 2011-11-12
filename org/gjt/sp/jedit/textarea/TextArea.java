/*
 * TextArea.java - Abstract jEdit Text Area component
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
 * Portions copyright (C) 2006 Matthieu Casanova
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
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TooManyListenersException;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.im.InputMethodRequests;

import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;

import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.IPropertyManager;
import org.gjt.sp.jedit.JEditActionContext;
import org.gjt.sp.jedit.JEditActionSet;
import org.gjt.sp.jedit.JEditBeanShellAction;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.input.AbstractInputHandler;
import org.gjt.sp.jedit.input.DefaultInputHandlerProvider;
import org.gjt.sp.jedit.input.InputHandlerProvider;
import org.gjt.sp.jedit.input.TextAreaInputHandler;
import org.gjt.sp.jedit.syntax.Chunk;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/** Abstract TextArea component.
 *
 * The concrete instance used by jEdit itself is called the JEditTextArea.
 *
 * This class uses a minimal set of jEdit APIs because it is the base class of the
 * JEditEmbeddedTextArea and StandaloneTextArea, so it needs to be embeddable and separable.
 *
 * @author Slava Pestov
 * @author kpouer (rafactoring into standalone text area)
 * @version $Id$
 */
public abstract class TextArea extends JComponent
{
	//{{{ TextArea constructor
	/**
	 * Creates a new JEditTextArea.
	 * @param propertyManager the property manager that contains informations like shortcut bindings
	 * @param inputHandlerProvider the inputHandlerProvider
	 */
	protected TextArea(IPropertyManager propertyManager, InputHandlerProvider inputHandlerProvider)
	{
		this.inputHandlerProvider = inputHandlerProvider;
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		//{{{ Initialize some misc. stuff
		selectionManager = new SelectionManager(this);
		chunkCache = new ChunkCache(this);
		painter = new TextAreaPainter(this);
		gutter = new Gutter(this);
		gutter.setMouseActionsProvider(new MouseActions(propertyManager, "gutter"));
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		blink = true;
		offsetXY = new Point();
		structureMatchers = new LinkedList<StructureMatcher>();
		structureMatchers.add(new StructureMatcher.BracketMatcher());
		//}}}

		//{{{ Initialize the GUI
		setLayout(new ScrollLayout());
		add(ScrollLayout.CENTER,painter);
		add(ScrollLayout.LEFT,gutter);

		// some plugins add stuff in a "right-hand" gutter
		verticalBox = new Box(BoxLayout.X_AXIS);
		verticalBox.add(vertical = new JScrollBar(Adjustable.VERTICAL));
		vertical.setRequestFocusEnabled(false);
		add(ScrollLayout.RIGHT,verticalBox);
		add(ScrollLayout.BOTTOM,
			horizontal = new JScrollBar(Adjustable.HORIZONTAL));
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


		addFocusListener(new FocusHandler());
		addMouseWheelListener(new MouseWheelHandler());

		//}}}

		// This doesn't seem very correct, but it fixes a problem
		// when setting the initial caret position for a buffer
		// (eg, from the recent file list)
		focusedComponent = this;

		popupEnabled = true;
	} //}}}

	//{{{ getFoldPainter() method
	public FoldPainter getFoldPainter()
	{
		return new TriangleFoldPainter();
	} //}}}

	//{{{ initInputHandler() method
	/**
	 * Creates an actionContext and initializes the input
	 * handler for this textarea. Called when creating
	 * a standalone textarea from within jEdit.
	 */
	public void initInputHandler()
	{
		actionContext = new JEditActionContext<JEditBeanShellAction, JEditActionSet<JEditBeanShellAction>>()
		{
			@Override
			public void invokeAction(EventObject evt, JEditBeanShellAction action)
			{
				action.invoke(TextArea.this);
			}
		};

		setMouseHandler(new TextAreaMouseHandler(this));
		inputHandlerProvider = new DefaultInputHandlerProvider(new TextAreaInputHandler(this)
		{
			@Override
			protected JEditBeanShellAction getAction(String action)
			{
				return actionContext.getAction(action);
			}
		});
	} //}}}

	//{{{ getActionContext() method
	public JEditActionContext<JEditBeanShellAction,JEditActionSet<JEditBeanShellAction>> getActionContext()
	{
		return actionContext;
	} //}}}

	//{{{ setMouseHandler() method
	public void setMouseHandler(MouseInputAdapter mouseInputAdapter)
	{
		mouseHandler = mouseInputAdapter;
		painter.addMouseListener(mouseHandler);
		painter.addMouseMotionListener(mouseHandler);
	} //}}}

	//{{{ setTransferHandler() method
	@Override
	public void setTransferHandler(TransferHandler newHandler)
	{
		super.setTransferHandler(newHandler);
		try
		{
			getDropTarget().addDropTargetListener(
				new TextAreaDropHandler(this));
		}
		catch(TooManyListenersException e)
		{
			Log.log(Log.ERROR,this,e);
		}
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		String baseVersion = super.toString();
		int len = baseVersion.length() - 1;
		builder.append(baseVersion);
		builder.setLength(len); // chop off the last ]
		builder.append(",caret=").append(caret);
		builder.append(",caretLine=").append(caretLine);
		builder.append(",caretScreenLine=").append(caretScreenLine);
		builder.append(",electricScroll=").append(electricScroll);
		builder.append(",horizontalOffset=").append(horizontalOffset);
		builder.append(",magicCaret=").append(magicCaret);
		builder.append(",offsetXY=").append(offsetXY.toString());
		builder.append(",oldCaretLine=").append(oldCaretLine);
		builder.append(",screenLastLine=").append(screenLastLine);
		builder.append(",visibleLines=").append(visibleLines);
		builder.append(",firstPhysicalLine=").append(getFirstPhysicalLine());
		builder.append(",physLastLine=").append(physLastLine).append("]");
		return builder.toString();
	} //}}}

	//{{{ dispose() method
	/**
	 * Plugins and macros should not call this method.
	 * @since jEdit 4.2pre1
	 */
	public void dispose()
	{
		DisplayManager.textAreaDisposed(this);
		gutter.dispose();
	} //}}}

	//{{{ getInputHandler() method
	/**
	 * @since jEdit 4.3pre1
	 */
	public AbstractInputHandler getInputHandler()
	{

		return inputHandlerProvider.getInputHandler();
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
	 * @return the display manager used by this text area.
	 * @since jEdit 4.2pre1
	 */
	public DisplayManager getDisplayManager()
	{
		return displayManager;
	} //}}}

	//{{{ isCaretBlinkEnabled() method
	/**
	 * @return true if the caret is blinking, false otherwise.
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
	 * @return the minimum distance (in number of lines)
	 * from the caret to the nearest edge of the screen
	 * (top or bottom edge).
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
	 * @since jedit 4.3pre3
	 *
	 *  Prior to 4.3pre3, this function returned a "Buffer" type.
	 *  If this causes your code to break, try calling view.getBuffer() instead of
	 *  view.getTextArea().getBuffer().
	 *
	 */
	public final JEditBuffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ setBuffer() method
	/**
	 * Sets the buffer this text area is editing.
	 * If you don't run a standalone textarea in jEdit please do not call this method -
	 * use {@link org.gjt.sp.jedit.EditPane#setBuffer(org.gjt.sp.jedit.Buffer)} instead.
	 * @param buffer The buffer
	 */
	public void setBuffer(JEditBuffer buffer)
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

				if(!this.buffer.isLoading())
					selectNone();
				caretLine = caret = caretScreenLine = 0;
				match = null;
			}
			boolean inCompoundEdit = false;
			if (this.buffer != null)
				inCompoundEdit = this.buffer.insideCompoundEdit();
			if (inCompoundEdit)
				this.buffer.endCompoundEdit();
			this.buffer = buffer;
			if (inCompoundEdit)
				this.buffer.beginCompoundEdit();

			chunkCache.setBuffer(buffer);
			gutter.setBuffer(buffer);
			propertiesChanged();

			if(displayManager != null)
			{
				displayManager.release();
			}

			displayManager = DisplayManager.getDisplayManager(
				buffer,this);

			displayManager.init();

			if(buffer.isLoading())
				updateScrollBar();

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

	//{{{ getJoinNonWordChars() method
	/**
	 * If set, double clicking will join non-word characters to form one "word".
	 * @since jEdit 4.3pre2
	 */
	public boolean getJoinNonWordChars()
	{
		return joinNonWordChars;
	} //}}}

	//{{{ setJoinNonWordChars() method
	/**
	 * If set, double clicking will join non-word characters to form one "word".
	 * @since jEdit 4.3pre2
	 */
	public void setJoinNonWordChars(boolean joinNonWordChars)
	{
		this.joinNonWordChars = joinNonWordChars;
	} //}}}

	//{{{ getCtrlForRectangularSelection() method
	/**
	 * If set, CTRL enables rectangular selection mode while pressed.
	 * @since jEdit 4.3pre10
	 */
	public boolean isCtrlForRectangularSelection()
	{
		return ctrlForRectangularSelection;
	} //}}}

	//{{{ setCtrlForRectangularSelection() method
	/**
	 * If set, CTRL enables rectangular selection mode while pressed.
	 * @since jEdit 4.3pre10
	 */
	public void setCtrlForRectangularSelection(boolean ctrlForRectangularSelection)
	{
		this.ctrlForRectangularSelection = ctrlForRectangularSelection;
	} //}}}

	//{{{ Scrolling

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
	/**
	 * Sets the vertical scroll bar position
	 *
	 * @param firstLine The scroll bar position
	 */
	public void setFirstLine(int firstLine)
	{
		//{{{ ensure we don't have empty space at the bottom or top, etc
		int max = displayManager.getScrollLineCount() - visibleLines
			+ (lastLinePartial ? 1 : 0);
		if(firstLine > max)
			firstLine = max;
		if(firstLine < 0)
			firstLine = 0;
		//}}}

		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"setFirstLine() from "
				+ getFirstLine() + " to " + firstLine);
		}

		int oldFirstLine = getFirstLine();
		if(firstLine == oldFirstLine)
			return;

		displayManager.setFirstLine(oldFirstLine,firstLine);

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

	//{{{ setFirstPhysicalLine() methods
	/**
	 * Sets the vertical scroll bar position.
	 * @param physFirstLine The first physical line to display
	 * @since jEdit 4.2pre1
	 */
	public void setFirstPhysicalLine(int physFirstLine)
	{
		setFirstPhysicalLine(physFirstLine,0);
	}

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
				+ physFirstLine + ',' + skew + ')');
		}

		int amount = physFirstLine - displayManager.firstLine.physicalLine;

		displayManager.setFirstPhysicalLine(amount,skew);

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

	//{{{ getLastScreenLine() method
	/**
	 * Returns the last screen line index, it is different from
	 * {@link #getVisibleLines()} because the buffer can have less lines than
	 * the visible lines
	 * @return the last screen line index.
	 * @since jEdit 4.3pre1
	 */
	public int getLastScreenLine()
	{
		return screenLastLine;
	} //}}}

	//{{{ getVisibleLines() method
	/**
	 * Returns the number of lines visible in this text area.
	 * @return the number of visible lines in the textarea
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

	//{{{ scrollTo() methods
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
	}

	/**
	 * Ensures that the specified location in the buffer is visible.
	 * @param line The line number
	 * @param offset The offset from the start of the line
	 * @param doElectricScroll If true, electric scrolling will be performed
	 * @since jEdit 4.0pre6
	 */
	public void scrollTo(int line, int offset, boolean doElectricScroll)
	{
		if (buffer.isLoading())
			return;
		if(Debug.SCROLL_TO_DEBUG)
			Log.log(Log.DEBUG,this,"scrollTo(), lineCount="
				+ getLineCount());

		if(visibleLines <= 1)
		{
			if(Debug.SCROLL_TO_DEBUG)
			Log.log(Log.DEBUG,this,"visibleLines <= 0");
			// Fix the case when the line is wrapped
			// it was not possible to see the second (or next)
			// subregion of a line
			ChunkCache.LineInfo[] infos = chunkCache
				.getLineInfosForPhysicalLine(line);
			int subregion = ChunkCache.getSubregionOfOffset(
				offset,infos);
			setFirstPhysicalLine(line,subregion);
			return;
		}

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

		int _electricScroll = doElectricScroll
			&& visibleLines - 1 > (electricScroll << 1)
				      ? electricScroll : 0;
		//}}}

		//{{{ Scroll vertically
		int screenLine = chunkCache.getScreenLineOfOffset(line,offset);
		int visibleLines = getVisibleLines();
		if(screenLine == -1)
		{
			// We are scrolling to a position that is not on the screen.
			if(Debug.SCROLL_TO_DEBUG)
				Log.log(Log.DEBUG,this,"screenLine == -1");
			ChunkCache.LineInfo[] infos = chunkCache
				.getLineInfosForPhysicalLine(line);
			int subregion = ChunkCache.getSubregionOfOffset(
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
					subregion + _electricScroll
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
					- (visibleLines >> 1));
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

		Point point = offsetToXY(line,offset,offsetXY);

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

	//{{{ xyToOffset() methods
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
	}

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
		int height = painter.getLineHeight();
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

	//{{{ offsetToXY() methods
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
	}

	/**
	 * Converts an offset into a point in the text area painter's
	 * co-ordinate space.
	 * @param line The line
	 * @param offset The offset
	 * @return The location of the offset on screen, or <code>null</code>
	 * if the specified offset is not visible
	 */
	public Point offsetToXY(int line, int offset)
	{
		return offsetToXY(line,offset,new Point());
	}

	/**
	 * Converts a line,offset pair into an x,y (pixel) point relative to the
	 * upper left corner (0,0) of the text area.
	 *
	 * @param line The physical line number (from top of document)
	 * @param offset The offset in characters, from the start of the line
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

		retVal.y = screenLine * painter.getLineHeight();

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
		if(buffer.isLoading())
			return;

		if(start > end)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		if(chunkCache.needFullRepaint())
			end = visibleLines;

		int y = start * painter.getLineHeight();
		int height = (end - start + 1) * painter.getLineHeight();
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
			|| buffer.isLoading()
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
		if(!isShowing() || buffer.isLoading())
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
	 * @param line The line (physical line)
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
	 * @param line The line (physical line)
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

	//{{{ getText() methods
	/**
	 * Returns the specified substring of the buffer.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring
	 */
	public final String getText(int start, int len)
	{
		return buffer.getText(start,len);
	}

	/**
	 * Copies the specified substring of the buffer into a segment.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		buffer.getText(start,len,segment);
	}

	/**
	 * Returns the entire text of this text area.
	 */
	public String getText()
	{
		return buffer.getText(0,buffer.getLength());
	} //}}}

	//{{{ getLineText() methods
	/**
	 * Returns the text on the specified line.
	 * @param lineIndex the line number
	 * @return The text, or null if the lineIndex is invalid
	 */
	public final String getLineText(int lineIndex)
	{
		return buffer.getLineText(lineIndex);
	}

	/**
	 * Copies the text on the specified line into a Segment. If lineIndex
	 * is invalid, the segment will contain a null string.
	 * @param lineIndex The line number (physical line)
	 * @param segment the segment into which the data will be stored.
	 */
	public final void getLineText(int lineIndex, Segment segment)
	{
		buffer.getLineText(lineIndex,segment);
	} //}}}

	//{{{ getVisibleLineText() methods
	/**
	 * Returns the visible part of the given line
	 * @param screenLine the screenLine
	 * @return the visible text
	 * @since 4.5pre1
	 */
	public String getVisibleLineText(int screenLine)
	{
		int offset = -getHorizontalOffset();
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(screenLine);
		int lineStartOffset = getLineStartOffset(lineInfo.physicalLine);
		Point point = offsetToXY(lineStartOffset + lineInfo.offset);
		int begin = xyToOffset(offset + point.x, point.y);
		int end = xyToOffset(getPainter().getWidth(), point.y);
		return buffer.getText(begin, end - begin);
	}

	/**
	 * Returns the visible part of the given line
	 * @param screenLine the screenLine
	 * @param segment the segment into which the data will be stored.
	 * @since 4.5pre1
	 */
	public void getVisibleLineText(int screenLine, Segment segment)
	{
		int offset = -getHorizontalOffset();
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(screenLine);
		int lineStartOffset = getLineStartOffset(lineInfo.physicalLine);
		Point point = offsetToXY(lineStartOffset + lineInfo.offset);
		int begin = xyToOffset(offset + point.x, point.y);
		int end = xyToOffset(getPainter().getWidth(), point.y);
		buffer.getText(begin, end - begin, segment);
	}//}}}

	/**
	 * Returns the visible part of the given line in a CharSequence.
	 * The buffer data are not copied. so this should be used in EDT
	 * thread
	 * @param screenLine the screenLine
	 * @return the visible text
	 * @since 4.5pre1
	 */
	public CharSequence getVisibleLineSegment(int screenLine)
	{
		int offset = -getHorizontalOffset();
		ChunkCache.LineInfo lineInfo = chunkCache.getLineInfo(screenLine);
		int lineStartOffset = getLineStartOffset(lineInfo.physicalLine);
		Point point = offsetToXY(lineStartOffset + lineInfo.offset);
		int begin = xyToOffset(offset + point.x, point.y);
		int end = xyToOffset(getPainter().getWidth(), point.y);
		return buffer.getSegment(begin, end - begin);
	}

	//{{{ setText() method
	/**
	 * Sets the entire text of this text area.
	 * @param text the new content of the buffer
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
	 * Selects all text in the buffer. Preserves the scroll position.
	 */
	public final void selectAll()
	{
		int firstLine = getFirstLine();
		int horizOffset = getHorizontalOffset();

		setSelection(new Selection.Range(0,buffer.getLength()));
		moveCaretPosition(buffer.getLength(),true);

		setFirstLine(firstLine);
		setHorizontalOffset(horizOffset);
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

		int wordStart = TextUtilities.findWordStart(lineText,offset,
					noWordSep,true,false,false);
		int wordEnd = TextUtilities.findWordEnd(lineText,offset+1,
					noWordSep,true,false,false);

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
	}

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

		// We can't do the backward scan if start == 0
		if(start == 0)
		{
			getToolkit().beep();
			return;
		}

		// Scan backwards, trying to find a bracket
		String openBrackets = "([{";
		String closeBrackets = ")]}";
		int count = 1;
		char openBracket = '\0';
		char closeBracket = '\0';

backward_scan:	while(--start >= 0)
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
forward_scan:	do
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
			return line >= match.startLine && line <= caretLine;
		else
			return line <= match.endLine && line >= caretLine;
	} //}}}

	//{{{ invertSelection() method
	/**
	 * Inverts the selection.
	 * @since jEdit 4.0pre1
	 */
	public final void invertSelection()
	{
		selectionManager.invertSelection();
	} //}}}

	//{{{ getSelectionCount() method
	/**
	 * Returns the number of selections. This can be used to test
	 * for the existence of selections.
	 * @since jEdit 3.2pre2
	 */
	public int getSelectionCount()
	{
		return selectionManager.getSelectionCount();
	} //}}}

	//{{{ getSelection() methods
	/**
	 * Returns the current selection.
	 * @since jEdit 3.2pre1
	 */
	public Selection[] getSelection()
	{
		return selectionManager.getSelection();
	}

	/**
	 * Returns the selection with the specified index. This must be
	 * between 0 and the return value of <code>getSelectionCount()</code>.
	 * @since jEdit 4.3pre1
	 * @param index the index of the selection you want
	 */
	public Selection getSelection(int index)
	{
		return selectionManager.selection.get(index);
	} //}}}

	//{{{ getSelectionIterator() method
	/**
	 * Returns the current selection.
	 * @since jEdit 4.3pre1
	 */
	public Iterator<Selection> getSelectionIterator()
	{
		return selectionManager.selection.iterator();
	} //}}}

	//{{{ selectNone() method
	/**
	 * Deselects everything.
	 */
	public void selectNone()
	{
		invalidateSelectedLines();
		setSelection((Selection)null);
	} //}}}

	//{{{ setSelection() methods
	/**
	 * Sets the selection. Nested and overlapping selections are merged
	 * where possible. Null elements of the array are ignored.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void setSelection(Selection[] selection)
	{
		// invalidate the old selection
		invalidateSelectedLines();
		selectionManager.setSelection(selection);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
	}

	/**
	 * Sets the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void setSelection(Selection selection)
	{
		invalidateSelectedLines();
		selectionManager.setSelection(selection);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
	} //}}}

	//{{{ addToSelection() methods
	/**
	 * Adds to the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection[] selection)
	{
		invalidateSelectedLines();
		selectionManager.addToSelection(selection);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
	}

	/**
	 * Adds to the selection. Nested and overlapping selections are merged
	 * where possible.
	 * @param selection The new selection
	 * since jEdit 3.2pre1
	 */
	public void addToSelection(Selection selection)
	{
		invalidateSelectedLines();
		selectionManager.addToSelection(selection);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
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
		return selectionManager.getSelectionAtOffset(offset);
	} //}}}

	//{{{ removeFromSelection() methods
	/**
	 * Deactivates the specified selection.
	 * @param sel The selection
	 * @since jEdit 3.2pre1
	 */
	public void removeFromSelection(Selection sel)
	{
		invalidateSelectedLines();
		selectionManager.removeFromSelection(sel);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
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

		invalidateSelectedLines();
		selectionManager.removeFromSelection(sel);
		finishCaretUpdate(caretLine,NO_SCROLL,true);
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
		Selection s = selectionManager.getSelectionAtOffset(offset);
		if(s != null)
		{
			invalidateLineRange(s.startLine,s.endLine);
			selectionManager.removeFromSelection(s);
		}

		selectionManager.resizeSelection(offset,end,extraEndVirt,rect);
		fireCaretEvent();
	} //}}}

	//{{{ extendSelection() methods
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
	}

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
			selectionManager.removeFromSelection(s);

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

		selectionManager.addToSelection(s);
		fireCaretEvent();

		if(rectangularSelectionMode && extraEndVirt != 0)
		{
			int line = getLineOfOffset(end);
			scrollTo(line,getLineLength(line) + extraEndVirt,false);
		}
	} //}}}

	//{{{ getSelectedText() methods
	/**
	 * Returns the text in the specified selection.
	 * @param s The selection
	 * @since jEdit 3.2pre1
	 */
	public String getSelectedText(Selection s)
	{
		StringBuilder buf = new StringBuilder(s.end - s.start);
		s.getText(buffer,buf);
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
		Selection[] sel = selectionManager.getSelection();
		if(sel.length == 0)
			return null;

		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < sel.length; i++)
		{
			if(i != 0)
				buf.append(separator);

			sel[i].getText(buffer,buf);
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
	} //}}}

	//{{{ setSelectedText() methods
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
	}

	/**
	 * Replaces the selection at the caret with the specified text.
	 * If there is no selection at the caret, the text is inserted at
	 * the caret position.
	 */
	public void setSelectedText(String selectedText)
	{
		int newCaret = replaceSelection(selectedText);
		if(newCaret != -1)
			moveCaretPosition(newCaret);
		selectNone();
	}

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
		int newCaret = replaceSelection(selectedText);
		if(moveCaret && newCaret != -1)
			moveCaretPosition(newCaret);
		selectNone();
	} //}}}

	//{{{ replaceSelection() method
	/**
	 * Set the selection, but does not deactivate it, and does not move the
	 * caret.
	 *
	 * Please use {@link #setSelectedText(String)} instead.
	 *
	 * @param selectedText The new selection
	 * @return The new caret position
	 * @since 4.3pre1
	 */
	public int replaceSelection(String selectedText)
	{
		if(!isEditable())
			throw new RuntimeException("Text component read only");

		int newCaret = -1;
		if(getSelectionCount() == 0)
		{
			// for compatibility with older jEdit versions
			buffer.insert(caret,selectedText);
		}
		else
		{
			try
			{
				buffer.beginCompoundEdit();

				Selection[] selection = getSelection();
				for(int i = 0; i < selection.length; i++)
					newCaret = selection[i].setText(buffer,selectedText);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}

		return newCaret;
	} //}}}

	//{{{ getSelectedLines() method
	/**
	 * Returns a sorted array of line numbers on which a selection or
	 * selections are present.<p>
	 *
	 * This method is the most convenient way to iterate through selected
	 * lines in a buffer. The line numbers in the array returned by this
	 * method can be passed as a parameter to such methods as
	 * {@link JEditBuffer#getLineText(int)}.
	 *
	 * @since jEdit 3.2pre1
	 */
	public int[] getSelectedLines()
	{
		if(selectionManager.getSelectionCount() == 0)
			return new int[] { caretLine };

		return selectionManager.getSelectedLines();
	} //}}}

	//}}}

	//{{{ Caret

	//{{{ caretAutoScroll() method
	/**
	 * Return if change in buffer should scroll this text area.
	 * @since jEdit 4.3pre2
	 */
	public boolean caretAutoScroll()
	{
		return focusedComponent == this;
	} //}}}

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

	//{{{ getStructureMatchStart() method
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
		int offset = getScreenLineStartOffset(visibleLines >> 1);
		if(offset == -1)
			getToolkit().beep();
		else
			setCaretPosition(offset);
	} //}}}

	// {{{ scrollAndCenterCaret() method
	/**
	 * Tries to scroll the textArea so that the caret is centered on the screen.
	 * Sometimes gets confused by folds but at least makes the caret visible and
	 * guesses better on subsequent attempts.
	 *
	 * @since jEdit 4.3pre15
	 */
	public void scrollAndCenterCaret()
	{
		if (!getDisplayManager().isLineVisible(getCaretLine()))
			getDisplayManager().expandFold(getCaretLine(),true);
		int physicalLine = getCaretLine();
		int midPhysicalLine = getPhysicalLineOfScreenLine(visibleLines >> 1);
		int diff = physicalLine -  midPhysicalLine;
		setFirstLine(getFirstLine() + diff);
		requestFocus();
	} // }}}

	//{{{ setCaretPosition() methods
	/**
	 * Sets the caret position and deactivates the selection.
	 * @param newCaret The caret position
	 */
	public void setCaretPosition(int newCaret)
	{
		selectNone();
		moveCaretPosition(newCaret,true);
	}

	/**
	 * Sets the caret position and deactivates the selection.
	 * @param newCaret The caret position
	 * @param doElectricScroll Do electric scrolling?
	 */
	public void setCaretPosition(int newCaret, boolean doElectricScroll)
	{
		selectNone();
		moveCaretPosition(newCaret,doElectricScroll);
	} //}}}

	//{{{ moveCaretPosition() methods
	/**
	 * Sets the caret position without deactivating the selection.
	 * @param newCaret The caret position
	 */
	public void moveCaretPosition(int newCaret)
	{
		moveCaretPosition(newCaret,true);
	}

	/**
	 * Sets the caret position without deactivating the selection.
	 * @param newCaret The caret position
	 * @param doElectricScroll Do electric scrolling?
	 */
	public void moveCaretPosition(int newCaret, boolean doElectricScroll)
	{
		moveCaretPosition(newCaret,doElectricScroll ? ELECTRIC_SCROLL
			: NORMAL_SCROLL);
	}

	public static final int NO_SCROLL = 0;
	public static final int NORMAL_SCROLL = 1;
	public static final int ELECTRIC_SCROLL = 2;

	/**
	 * Sets the caret position without deactivating the selection.
	 * @param newCaret The caret position
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
		int oldCaretLine = caretLine;

		if(caret == newCaret)
			finishCaretUpdate(oldCaretLine,scrollMode,false);
		else
		{
			caret = newCaret;
			caretLine = getLineOfOffset(newCaret);

			magicCaret = -1;

			finishCaretUpdate(oldCaretLine,scrollMode,true);
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextBracket(boolean select)
	{
		int newCaret = -1;

		if(caret != buffer.getLength())
		{
			String text = getText(caret,buffer.getLength()
				- caret - 1);

loop:			for(int i = 0; i < text.length(); i++)
			{
				switch(text.charAt(i))
				{
				case ')': case ']': case '}':
					newCaret = caret + i + 1;
					break loop;
				}
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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToNextLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		boolean rectSelect = s == null ? rectangularSelectionMode
			: s instanceof Selection.Rect;
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

		_changeLine(select, newCaret);

		setMagicCaretPosition(magic);
	}//}}}

	//{{{ goToNextPage() method
	/**
	 * Moves the caret to the next screenful.
	 * @param select true if you want to extend selection
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

		int newCaret;

		if(getFirstLine() + getVisibleLines() >= displayManager
			.getScrollLineCount())
		{
			int lastVisibleLine = displayManager
				.getLastVisibleLine();
			newCaret = getLineEndOffset(lastVisibleLine) - 1;
		}
		else
		{
			int caretScreenLine = getScreenLineOfOffset(caret);

			scrollDownPage();

			newCaret = xToScreenLineOffset(caretScreenLine,
				magic,true);
		}

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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToNextParagraph(boolean select)
	{
		int lineNo = getCaretLine();

		int newCaret = getBufferLength();

		boolean foundBlank = false;

		final Segment lineSegment = new Segment();
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

	//{{{ goToNextWord() methods
	/**
	 * Moves the caret to the start of the next word.
	 * Note that if the "view.eatWhitespace" boolean propery is false,
	 * this method moves the caret to the end of the current word instead.
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToNextWord(boolean select)
	{
		goToNextWord(select,false);
	}

	/**
	 * Moves the caret to the start of the next word.
	 * @since jEdit 4.1pre5
	 */
	public void goToNextWord(boolean select, boolean eatWhitespace)
	{
		if (buffer.isLoading())
			return;
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
			boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
			newCaret = TextUtilities.findWordEnd(lineText,
				newCaret + 1,noWordSep,true,camelCasedWords,
				eatWhitespace);

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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
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

		if(caret == 0)
		{
			getToolkit().beep();
			return;
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		boolean rectSelect = s == null ? rectangularSelectionMode
			: s instanceof Selection.Rect;
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

		_changeLine(select, newCaret);

		setMagicCaretPosition(magic);
	} //}}}

	//{{{ goToPrevPage() method
	/**
	 * Moves the caret to the previous screenful.
	 * @param select true if you want to extend selection
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

		int newCaret;

		if(getFirstLine() == 0)
		{
			int firstVisibleLine = displayManager
				.getFirstVisibleLine();
			newCaret = getLineStartOffset(firstVisibleLine);
		}
		else
		{
			int caretScreenLine = getScreenLineOfOffset(caret);

			scrollUpPage();

			newCaret = xToScreenLineOffset(caretScreenLine,
				magic,true);
		}

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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevParagraph(boolean select)
	{
		int lineNo = caretLine;
		int newCaret = 0;

		boolean foundBlank = false;

		final Segment lineSegment = new Segment();
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevWord(boolean select)
	{
		goToPrevWord(select,false);
	} //}}}

	//{{{ goToPrevWord() method
	/**
	 * Moves the caret to the start of the previous word.
	 * @param eatWhitespace If true, will eat whitespace
	 * @since jEdit 4.1pre5
	 */
	public void goToPrevWord(boolean select, boolean eatWhitespace)
	{
		goToPrevWord(select,eatWhitespace,false);
	} //}}}

	//{{{ goToPrevWord() method
	/**
	 * Moves the caret to the start of the previous word.
	 * @param eatWhitespace If true, will eat whitespace
	 * @param eatOnlyAfterWord Eat only whitespace after a word,
	 * in effect this goes to actual word starts even if eating
	 * @since jEdit 4.4pre1
	 */
	public void goToPrevWord(boolean select, boolean eatWhitespace, boolean eatOnlyAfterWord)
	{
		if (buffer.isLoading())
			return;
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
			boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
			newCaret = TextUtilities.findWordStart(lineText,
				newCaret - 1,noWordSep,true,camelCasedWords,eatWhitespace,
				eatOnlyAfterWord);

			newCaret += lineStart;
		}

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ home() method
	/**
	 * A "dumb home" action which only has 2 states:
	 *     start of the whitespace or start of line
	 *     @param select true if we also want to select from the cursor
	 * @since jedit 4.3pre18
	 */
	public void home(boolean select)
	{
		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			goToStartOfWhiteSpace(select);
			break;
		default:
			goToStartOfLine(select);
			break;
		}
	} //}}}

	//{{{ end() method
	/**
	 * a dumb end action which only has 2 states:
	 * 	end of whitespace or end of line
	 * @param select true if we also want to select from the cursor
	 * @since jedit 4.3pre18
	 */
	public void end(boolean select)
	{
		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			goToEndOfWhiteSpace(select);
			break;
		default:
			goToEndOfLine(select);
			break;
		}
	} //}}}

	//{{{ smartHome() method
	/**
	 * On subsequent invocations, first moves the caret to the first
	 * non-whitespace character of the line, then the beginning of the
	 * line, then to the first visible line.
	 * @param select true if you want to extend selection
	 * @since jEdit 4.3pre7
	 */
	public void smartHome(boolean select)
	{
		switch(getInputHandler().getLastActionCount())
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
	} //}}}

	//{{{ smartEnd() method
	/**
	 * Has 4 states based on # of invocations:
	 *   1. last character of code (before inline comment)
	 *   2. last non whitespace character of the line
	 *   3. end of line
	 *   4. end of last visible line
	 * @param select true if you want to extend selection
	 * @since jEdit 4.3pre18
	 */
	public void smartEnd(boolean select)
	{
		switch(getInputHandler().getLastActionCount())
		{
		case 1:
			int pos = getCaretPosition();
			goToEndOfCode(select);
			int npos = getCaretPosition();
			if (npos == pos) goToEndOfWhiteSpace(select);
			break;
		case 2:
			goToEndOfWhiteSpace(select);
			break;
		case 3:
			goToEndOfLine(select);
			break;
		default: //case 4:
			goToLastVisibleLine(select);
			break;
		}
	} //}}}

	//{{{ goToStartOfLine() method
	/**
	 * Moves the caret to the beginning of the current line.
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line = select || s == null ? caretLine : s.startLine;
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfLine(boolean select)
	{
		Selection s = getSelectionAtOffset(caret);
		int line = select || s == null ? caretLine : s.endLine;
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

	//{{{ goToEndOfCode() method
	/**
	 * Moves the caret to the end of the code present on the current line, before the comments and whitespace.
	 * @param select true if you want to extend selection
	 * @since jEdit 4.3pre18
	 */
	public void goToEndOfCode(boolean select)
	{
		int line = getCaretLine();

		DefaultTokenHandler tokenHandler = new DefaultTokenHandler();
		buffer.markTokens(line,tokenHandler);
		Token token = tokenHandler.getTokens();

		char[] txt = getLineText(line).toCharArray();

		// replace comments with whitespace to find endOfCode:
		while(true)
		{
			if( token.id == Token.COMMENT1 ||
				token.id == Token.COMMENT2 ||
				token.id == Token.COMMENT3 ||
				token.id == Token.COMMENT4)
			{
				for(int i=token.offset; i<token.offset+token.length; i++)
				{
					txt[i] = ' ';
				}
			}

			if(token.next == null)
				break;
			token = token.next;
		}

		int newCaret = getLineLength(line) - StandardUtilities.getTrailingWhiteSpace( new String(txt) );
		newCaret += getLineStartOffset(line);

		if(select)
			extendSelection(caret,newCaret);
		else if(!multi)
			selectNone();
		moveCaretPosition(newCaret);
	} //}}}

	//{{{ goToStartOfWhiteSpace() method
	/**
	 * Moves the caret to the first non-whitespace character of the current
	 * line.
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfWhiteSpace(boolean select)
	{
		if (buffer.isLoading())
			return;
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
			firstIndent = StandardUtilities.getLeadingWhiteSpace(getLineText(line));
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
	 * @param select true if you want to extend selection
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfWhiteSpace(boolean select)
	{
		if (buffer.isLoading())
			return;
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
			lastIndent = getLineLength(line) - StandardUtilities.getTrailingWhiteSpace(getLineText(line));
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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
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

	//}}}

	//{{{ User input

	//{{{ userInput() method
	/**
	 * Handles the insertion of the specified character. It performs the
	 * following operations above and beyond simply inserting the text:
	 * <ul>
	 * <li>Inserting a TAB with a selection will shift to the right
	 * <li>Inserting a BACK_SPACE or a DELETE will remove a character
	 * <li>Inserting an indent open/close bracket will re-indent the current
	 * line as necessary
	 * </ul>
	 *
	 * @param ch The character
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 * @since jEdit 4.3pre7
	 */
	public void userInput(char ch)
	{
		if(!isEditable())
		{
			getToolkit().beep();
			return;
		}

		getPainter().hideCursor();

		switch(ch)
		{
		case '\t':
			userInputTab();
			break;
		case '\b':
			backspace();
			break;
		case '\u007F':
			delete();
			break;
		default:
			boolean indent = buffer.isElectricKey(ch, caretLine) &&
				buffer.getBooleanProperty("autoIndent");
			String str = String.valueOf(ch);
			if(getSelectionCount() == 0)
			{
				if(!doWordWrap(ch == ' '))
					insert(str,indent);
			}
			else
				replaceSelection(str);
			break;
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
		fireStatusChanged(StatusListener.OVERWRITE_CHANGED,overwrite);
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
		delete(false);
	} //}}}

	//{{{ backspaceWord() methods
	/**
	 * Deletes the word before the caret.
	 * @since jEdit 2.7pre2
	 */
	public void backspaceWord()
	{
		backspaceWord(false);
	}

	/**
	 * Deletes the word before the caret.
	 * @param eatWhitespace If true, will eat whitespace
	 * @since jEdit 4.2pre5
	 */
	public void backspaceWord(boolean eatWhitespace)
	{
		backspaceWord(eatWhitespace,false);
	} //}}}

	//{{{ backspaceWord() method
	/**
	 * Deletes the word before the caret.
	 * @param eatWhitespace If true, will eat whitespace
	 * @param eatOnlyAfterWord Eat only whitespace after a word,
	 * in effect this goes to actual word starts even if eating
	 * @since jEdit 4.4pre1
	 */
	public void backspaceWord(boolean eatWhitespace, boolean eatOnlyAfterWord)
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(getSelectionCount() != 0)
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
			boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
			_caret = TextUtilities.findWordStart(lineText,_caret-1,
				noWordSep,true,camelCasedWords,eatWhitespace,
				eatOnlyAfterWord);
		}

		buffer.remove(_caret + lineStart, caret - (_caret + lineStart));
	} //}}}

	//{{{ delete() method
	/**
	 * Deletes the character after the caret.
	 * @since jEdit 2.7pre2
	 */
	public void delete()
	{
		delete(true);
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

		int x = chunkCache.subregionOffsetToX(caretLine,caret - getLineStartOffset(caretLine));
		int[] lines = getSelectedLines();

		try
		{
			buffer.beginCompoundEdit();

			for (int i = lines.length - 1; i >= 0; i--)
			{
				int start = getLineStartOffset(lines[i]);
				int end = getLineEndOffset(lines[i]);
				if (end > buffer.getLength())
				{
					if (start != 0)
						start--;
					end--;
				}
				buffer.remove(start,end - start);
			}
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
			setCaretPosition(getLineStartOffset(caretLine) + offset);
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

		// find the beginning of the paragraph.
		int start = 0;
		for(int i = caretLine - 1; i >= 0; i--)
		{
			if (lineContainsSpaceAndTabs(i))
			{
				start = getLineStartOffset(i);
				break;
			}
		}

		// Find the end of the paragraph
		int end = buffer.getLength();
		for(int i = caretLine + 1; i < getLineCount(); i++)
		{
			//if(!displayManager.isLineVisible(i))
			//	continue loop;

			if (lineContainsSpaceAndTabs(i))
			{
				end = getLineEndOffset(i) - 1;
				break;
			}
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

	//{{{ deleteWord() methods
	/**
	 * Deletes the word in front of the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteWord()
	{
		deleteWord(false);
	}

	/**
	 * Deletes the word in front of the caret.
	 *
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

		if(getSelectionCount() != 0)
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
			boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
			_caret = TextUtilities.findWordEnd(lineText,
				_caret+1,noWordSep,true,camelCasedWords,eatWhitespace);
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
		fireStatusChanged(StatusListener.MULTI_SELECT_CHANGED,multi);
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

		if(getSelectionCount() == 1)
		{
			Selection s = getSelection(0);
			removeFromSelection(s);
			if(rectangularSelectionMode)
			{
				addToSelection(new Selection.Rect(
					s.getStart(),s.getEnd()));
			}
			else
			{
				addToSelection(new Selection.Range(
					s.getStart(),s.getEnd()));
			}
		}
	} //}}}

	//{{{ setRectangularSelectionEnabled() method
	/**
	 * Set rectangular selection on or off according to the value of
	 * <code>rectangularSelectionMode</code>. This only affects the ability
	 * to make multiple selections from the keyboard. A rectangular
	 * selection can always be created by dragging with the mouse by holding
	 * down <b>Control</b>, regardless of the state of this flag.
	 *
	 * @param rectangularSelectionMode Should rectangular selection be
	 * enabled?
	 * @since jEdit 4.2pre1
	 */
	public final void setRectangularSelectionEnabled(
		boolean rectangularSelectionMode)
	{
		this.rectangularSelectionMode = rectangularSelectionMode;
		fireStatusChanged(StatusListener.RECT_SELECT_CHANGED,
			rectangularSelectionMode);
		painter.repaint();
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
	 * @param select true if you want to extend selection
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
	 * @param select true if you want to extend selection
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

	//{{{ collapseFold() methods
	/**
	 * Like {@link DisplayManager#collapseFold(int)}, but
	 * also moves the caret to the first line of the fold.
	 * @since jEdit 4.0pre3
	 */
	public void collapseFold()
	{
		collapseFold(caretLine);
	}

	/**
	 * Like {@link DisplayManager#collapseFold(int)}, but
	 * also moves the caret to the first line of the fold.
	 * @param line the physical line index of the fold that we want to collapse
	 * @since jEdit 4.3pre7
	 */
	public void collapseFold(int line)
	{
		displayManager.collapseFold(line);
	} //}}}

	//{{{ expandFold() method
	/**
	 * Like {@link DisplayManager#expandFold(int,boolean)}, but
	 * also moves the caret to the first sub-fold.
	 * @param fully If true, all subfolds will also be expanded
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

	//{{{ selectFold() methods
	/**
	 * Selects the fold that contains the caret line number.
	 * @since jEdit 3.1pre3
	 */
	public void selectFold()
	{
		selectFold(caretLine);
	}

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
		if(getSelectionCount() != 1)
		{
			getToolkit().beep();
			return;
		}

		Selection sel = getSelection(0);
		displayManager.narrow(sel.getStartLine(),sel.getEndLine());

		selectNone();
	} //}}}

	//{{{ addExplicitFold() method
	/**
	 * Surrounds the selection with explicit fold markers.
	 * @throws TextAreaException an exception thrown if the folding mode is
	 * not explicit
	 * @since jEdit 4.0pre3
	 */
	public void addExplicitFold() throws TextAreaException
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}
		if(!"explicit".equals(buffer.getStringProperty("folding")))
		{
			throw new TextAreaException("folding-not-explicit");
		}

		try
		{
			buffer.beginCompoundEdit();

			if (getSelectionCount() == 0)
			{
				int caretBack = addExplicitFold(caret, caret, caretLine, caretLine);
				setCaretPosition(caret - caretBack);
			}
			else
			{
				Selection[] selections = getSelection();
				Selection selection = null;
				int caretBack = 0;
				for (int i = 0; i < selections.length; i++)
				{
					selection = selections[i];
					caretBack = addExplicitFold(selection.start, selection.end, selection.startLine,selection.endLine);
				}
				// Selection cannot be null because there is at least 1 selection
				assert selection != null;
				setCaretPosition(selection.start - caretBack, false);
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
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}
		String comment = buffer.getContextSensitiveProperty(caret,"lineComment");
		if(comment == null || comment.length() == 0)
		{
			rangeLineComment();
			return;
		}

		comment += ' ';

		buffer.beginCompoundEdit();

		int[] lines = getSelectedLines();

		try
		{
			for(int i = 0; i < lines.length; i++)
			{
				String text = getLineText(lines[i]);
				buffer.insert(getLineStartOffset(lines[i])
					+ StandardUtilities.getLeadingWhiteSpace(text),
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

		commentStart += ' ';
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
	public void formatParagraph() throws TextAreaException
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(maxLineLen <= 0)
		{
			throw new TextAreaException("format-maxlinelen");
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

			for(int i = lineNo - 1; i >= 0; i--)
			{
				if (lineContainsSpaceAndTabs(i))
				{
					start = getLineEndOffset(i);
					break;
				}
			}

			for(int i = lineNo + 1; i < getLineCount(); i++)
			{
				if (lineContainsSpaceAndTabs(i))
				{
					end = getLineStartOffset(i) - 1;
					break;
				}
			}

			try
			{
				buffer.beginCompoundEdit();

				String text = buffer.getText(start,end - start);
				int offset = getCaretPosition() - start;
				int noSpaceOffset = TextUtilities.indexIgnoringWhitespace(
					text,offset);
				buffer.remove(start,end - start);
				text = TextUtilities.format(
					text,maxLineLen,buffer.getTabSize());
				buffer.insert(start,text);
				int caretPos = start;
				if (text.length() != 0)
				{
					caretPos += Math.min(text.length(),
					TextUtilities.ignoringWhitespaceIndex(
					text,noSpaceOffset));
				}
				moveCaretPosition(caretPos);
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
				setSelectedText(s, TextUtilities.tabsToSpaces(
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
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		Selection[] selection = getSelection();
		int caret = -1;
		if (selection.length == 0)
		{
			caret = getCaretPosition();
			selectWord();
			selection = getSelection();
		}
		if (selection.length == 0)
		{
			if (caret != -1)
				setCaretPosition(caret);
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
		if (caret != -1)
			setCaretPosition(caret);
	} //}}}

	//{{{ toLowerCase() method
	/**
	 * Converts the selected text to lower case.
	 * @since jEdit 2.7pre2
	 */
	public void toLowerCase()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		Selection[] selection = getSelection();
		int caret = -1;
		if (selection.length == 0)
		{
			caret = getCaretPosition();
			selectWord();
			selection = getSelection();
		}
		if (selection.length == 0)
		{
			if (caret != -1)
				setCaretPosition(caret);
			getToolkit().beep();
			return;
		}

		buffer.beginCompoundEdit();

		for (int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			setSelectedText(s,getSelectedText(s).toLowerCase());
		}

		buffer.endCompoundEdit();
		if (caret != -1)
			setCaretPosition(caret);
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
			boolean indent = buffer.getBooleanProperty("autoIndent");
			if (indent && buffer.isElectricKey('\n', caretLine))
			{
				buffer.indentLine(caretLine, true);
			}
			
			try
			{
				buffer.beginCompoundEdit();
				setSelectedText("\n");
				
				if (indent)
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

		boolean indent = buffer.getBooleanProperty("autoIndent");
		if(indent && getSelectionCount() == 0)
		{
			// if caret is inside leading whitespace, indent.
			CharSequence text = buffer.getLineSegment(caretLine);
			int start = buffer.getLineStartOffset(caretLine);
			int whiteSpace = StandardUtilities.getLeadingWhiteSpace(text);

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
 	
	//{{{ turnOnElasticTabstops() method
	/**
	 * Turn ON elastic tab stops.
	 */
	public void turnOnElasticTabstops()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{	
			buffer.indentUsingElasticTabstops();
			buffer.elasticTabstopsOn = true;
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
	 * Joins the current and the next line, or joins all lines in
	 * selections.
	 * @since jEdit 2.7pre2
	 */
	public void joinLines()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		try
		{
			buffer.beginCompoundEdit();
			boolean doneForSelection = false;
			for (Selection selection: selectionManager.getSelection())
			{
				while (selection.startLine < selection.endLine)
				{
					// Edit from end of selection to
					// minimize invalidations and
					// recaluculations of cached line info
					// such as indent level or fold level.
					joinLineAt(selection.endLine - 1);
					doneForSelection = true;
				}
			}
			// If nothing selected or all selections span only
			// one line, join the line at the caret.
			if (!doneForSelection)
			{
				int end = getLineEndOffset(caretLine);

				// Nothing to do if the caret is on the last line.
				if (end > buffer.getLength())
				{
					getToolkit().beep();
					return;
				}

				joinLineAt(caretLine);
				if(!multi)
					selectNone();
				moveCaretPosition(end - 1);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}
	//}}}

	//{{{ AWT stuff

	//{{{ addLeftOfScrollBar() method
	/**
	 * Adds a component to the left side of the box left of the vertical
	 * scroll bar. The ErrorList plugin uses this to show a global error
	 * overview, for example.  It is possible for more than one component
	 * to be added, each is added to the left side of the box in turn.
	 * Adding to the left ensures the scrollbar is always right of all added
	 * components.
	 *
	 * @param comp The component
	 * @since jEdit 4.2pre1
	 */
	public void addLeftOfScrollBar(Component comp)
	{
		verticalBox.add(comp, 0);
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
	@Override
	public void addNotify()
	{
		super.addNotify();

		ToolTipManager.sharedInstance().registerComponent(painter);
		ToolTipManager.sharedInstance().registerComponent(gutter);

		recalculateVisibleLines();
		if(!buffer.isLoading())
			recalculateLastPhysicalLine();
		propertiesChanged();
	} //}}}

	//{{{ removeNotify() method
	/**
	 * Called by the AWT when this component is removed from it's parent.
	 * This clears the pointer to the currently focused component.
	 * Also removes document listener.
	 */
	@Override
	public void removeNotify()
	{
		super.removeNotify();

		ToolTipManager.sharedInstance().unregisterComponent(painter);
		ToolTipManager.sharedInstance().unregisterComponent(gutter);

		if(focusedComponent == this)
			focusedComponent = null;

		caretTimer.stop();
	} //}}}

	//{{{ getFocusTraversalKeysEnabled() method
	/**
	 * Java 1.4 compatibility fix to make Tab key work.
	 * @since jEdit 3.2pre4
	 */
	@Override
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
	@Override
	public void processKeyEvent(KeyEvent evt)
	{
		getInputHandler().processKeyEvent(evt, 1 /* source=TEXTAREA (1) */, false);
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
		add(ScrollLayout.TOP,comp);
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

	//{{{ getInputMethodRequests() method
	@Override
	public InputMethodRequests getInputMethodRequests()
	{
		if(inputMethodSupport == null)
		{
			inputMethodSupport = new InputMethodSupport(this);
			Log.log(Log.DEBUG, this, "InputMethodSupport is activated");
		}
		return inputMethodSupport;
	} //}}}
	//}}}

	//{{{ addStatusListener() method
	/**
	 * Adds a scroll listener to this text area.
	 * @param listener The listener
	 * @since jEdit 4.3pre2
	 */
	public final void addStatusListener(StatusListener listener)
	{
		listenerList.add(StatusListener.class,listener);
	} //}}}

	//{{{ removeStatusListener() method
	/**
	 * Removes a scroll listener from this text area.
	 * @param listener The listener
	 * @since jEdit 4.3pre2
	 */
	public final void removeStatusListener(StatusListener listener)
	{
		listenerList.remove(StatusListener.class,listener);
	} //}}}

	//{{{ propertiesChanged() method
	/**
	 * Called by jEdit when necessary. Plugins should not call this method.
	 */
	public void propertiesChanged()
	{
		if(buffer == null)
			return;
		
		if(buffer.getBooleanProperty("elasticTabstops"))
		{
			//call this only if it was previously off
			if(!buffer.elasticTabstopsOn)
			{	
				turnOnElasticTabstops();
			}	
			if(buffer.getColumnBlock()!=null)
			{	
				buffer.getColumnBlock().setTabSizeDirtyStatus(true, true);
			}	
		}
		else
		{
			buffer.elasticTabstopsOn = false;
		}
		
		int _tabSize = buffer.getTabSize();
		char[] foo = new char[_tabSize];
		for(int i = 0; i < foo.length; i++)
			foo[i] = ' ';

		tabSize = painter.getStringWidth(new String(foo));

		charWidth = (int)Math.round(
			painter.getFont().getStringBounds(foo,0,1,
			painter.getFontRenderContext()).getWidth());

		String oldWrap = wrap;
		wrap = buffer.getStringProperty("wrap");
		hardWrap = "hard".equals(wrap);
		softWrap = "soft".equals(wrap);
		boolean oldWrapToWidth = wrapToWidth;
		int oldWrapMargin = wrapMargin;
		setMaxLineLength(buffer.getIntegerProperty("maxLineLen",0));

		boolean wrapSettingsChanged = !(wrap.equals(oldWrap)
			&& oldWrapToWidth == wrapToWidth
			&& oldWrapMargin == wrapMargin);

		if(displayManager != null && !bufferChanging
			&& !buffer.isLoading() && wrapSettingsChanged)
		{
			displayManager.invalidateScreenLineCounts();
			displayManager.notifyScreenLineChanges();
		}
		chunkCache.invalidateAll();
		gutter.repaint();
		painter.repaint();
	} //}}}

	//{{{ addActionSet() method
	/**
	 * Adds a new action set to the textarea's list of ActionSets.
	 * Call this only on standalone textarea
	 *
	 * @param actionSet the actionSet to add
	 * @since jEdit 4.3pre13
	 */
	public void addActionSet(JEditActionSet<JEditBeanShellAction> actionSet)
	{
		actionContext.addActionSet(actionSet);
	} //}}}

	//{{{ getMarkPosition() method
	/**
	 * @deprecated Do not use.
	 */
	@Deprecated
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

	//{{{ Package-private members

	static TextArea focusedComponent;

	//{{{ Instance variables
	MouseInputAdapter mouseHandler;
	final ChunkCache chunkCache;
	DisplayManager displayManager;
	final SelectionManager selectionManager;
	/**
	 * The action context.
	 * It is used only when the textarea is standalone
	 */
	private JEditActionContext<JEditBeanShellAction,JEditActionSet<JEditBeanShellAction>> actionContext;
	boolean bufferChanging;

	int maxHorizontalScrollWidth;

	String wrap;
	boolean hardWrap;
	boolean softWrap;
	boolean wrapToWidth;
	int maxLineLen;
	int wrapMargin;
	float tabSize;
	int charWidth;

	boolean scrollBarsInitialized;

	/**
	 * Cursor location, measured as an offset (in pixels) from upper left corner
	 * of the TextArea.
	 */
	final Point offsetXY;

	boolean lastLinePartial;

	boolean blink;
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
			horizontal.setUnitIncrement(10);
			horizontal.setBlockIncrement(painter.getWidth());
		}
		else if (horizontal.getValue() != -horizontalOffset)
		{
			horizontal.setValue(-horizontalOffset);
		}
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getLineHeight();
		if(lineHeight == 0)
			visibleLines = 0;
		else if(height <= 0)
		{
			visibleLines = 0;
			lastLinePartial = false;
		}
		else
		{
			visibleLines = height / lineHeight;
			lastLinePartial = height % lineHeight != 0;
			if(lastLinePartial)
				visibleLines++;
		}

		chunkCache.recalculateVisibleLines();

		// this does the "trick" to eliminate blank space at the end
		if(displayManager != null && buffer != null && !buffer.isLoading())
			setFirstLine(getFirstLine());

		updateScrollBar();
	} //}}}

	//{{{ foldStructureChanged() method
	void foldStructureChanged()
	{
		chunkCache.invalidateAll();
		recalculateLastPhysicalLine();

		if(!displayManager.isLineVisible(caretLine))
		{
			int x = chunkCache.subregionOffsetToX(caretLine,
				caret - getLineStartOffset(caretLine));
			int line = displayManager.getPrevVisibleLine(caretLine);

			if(!multi)
			{
				// cannot use selectNone() because the finishCaretUpdate method will reopen the fold
				invalidateSelectedLines();
				selectionManager.setSelection((Selection) null);
			}
			moveCaretPosition(buffer.getLineStartOffset(line)
				+ chunkCache.xToSubregionOffset(line,0,x,true));
		}
		repaint();
	} //}}}

	//{{{ updateScrollBar() method
	/**
	 * Updates the state of the scroll bars. This should be called
	 * if the number of lines in the buffer changes, or when the
	 * size of the text are changes.
	 */
	void updateScrollBar()
	{
		if(buffer == null)
			return;

		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"updateScrollBar(), slc="
				+ displayManager.getScrollLineCount());

		if(vertical != null && visibleLines != 0)
		{
			if(Debug.SCROLL_DEBUG)
				Log.log(Log.DEBUG,this,"Vertical ok");
			final int lineCount = displayManager.getScrollLineCount();
			final int firstLine = getFirstLine();
			final int visible = visibleLines - (lastLinePartial ? 1 : 0);

			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					vertical.setValues(firstLine,visible,0,lineCount);
					vertical.setUnitIncrement(2);
					vertical.setBlockIncrement(visible);
				}
			};
			ThreadUtilities.runInDispatchThread(runnable);
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
			if(match != null)
			{
				if(oldCaretLine < match.startLine)
					invalidateLineRange(oldCaretLine,match.endLine);
				else
					invalidateLineRange(match.startLine,oldCaretLine);
				match = null;
			}

			int newCaretScreenLine = chunkCache.getScreenLineOfOffset(caretLine,
				caret - buffer.getLineStartOffset(caretLine));
			if(caretScreenLine == -1)
				invalidateScreenLineRange(newCaretScreenLine,newCaretScreenLine);
			else
				invalidateScreenLineRange(caretScreenLine,newCaretScreenLine);
			caretScreenLine = newCaretScreenLine;

			invalidateSelectedLines();

			// When the user is typing, etc, we don't want the caret
			// to blink
			blink = true;
			caretTimer.restart();

			if(!displayManager.isLineVisible(caretLine))
			{			
				// If we've jumped outside of a narrowed display, just reset all
				// folds to their default level, so that we don't get disconnected
				// islands of visible lines.
				if(displayManager.isOutsideNarrowing(caretLine))
				{
					int collapseFolds = buffer.getIntegerProperty(
						"collapseFolds",0);
					if(collapseFolds != 0)
					{
						displayManager.expandFolds(collapseFolds, false);
						displayManager.expandFold(caretLine, false);
						foldStructureChanged();
					}
					else
						displayManager.expandAllFolds();
				}
				else
					displayManager.expandFold(caretLine,false);
			}

			if(queuedScrollMode == ELECTRIC_SCROLL)
				scrollToCaret(true);
			else if(queuedScrollMode == NORMAL_SCROLL)
				scrollToCaret(false);

			updateBracketHighlightWithDelay();
			if(queuedFireCaretEvent)
				fireCaretEvent();
		}
		// in case one of the above fails, we still want to
		// clear these flags.
		finally
		{
			queuedCaretUpdate = queuedFireCaretEvent = false;
			queuedScrollMode = NO_SCROLL;
		}
	} //}}}

	//{{{ invalidateStructureMatch() method
	void invalidateStructureMatch()
	{
		if(match != null)
			invalidateLineRange(match.startLine,match.endLine);
	} //}}}

	//{{{ startDragAndDrop() method
	void startDragAndDrop(InputEvent evt, boolean copy)
	{
		TransferHandler transferHandler = getTransferHandler();
		if (transferHandler != null)
		{
			Log.log(Log.DEBUG,this,"Drag and drop callback");
			transferHandler.exportAsDrag(this,evt,
				copy ? TransferHandler.COPY
				: TransferHandler.MOVE);
		}
	} //}}}

	//{{{ fireNarrowActive() method
	void fireNarrowActive()
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == StatusListener.class)
			{
				try
				{
					((StatusListener)listeners[i+1])
						.narrowActive(this);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,t);
				}
			}
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static final Timer caretTimer;
	private static final Timer structureTimer;
	//}}}

	//{{{ Instance variables
	protected JPopupMenu popup;

	private boolean popupEnabled;

	private final Gutter gutter;
	protected final TextAreaPainter painter;

	private final EventListenerList listenerList;
	private final MutableCaretEvent caretEvent;

	private boolean caretBlinks;
	private final ElasticTabstopsTabExpander elasticTabstopsExpander = new ElasticTabstopsTabExpander(this);
	protected InputHandlerProvider inputHandlerProvider;

	private InputMethodSupport inputMethodSupport;

	/** The last visible physical line index. */
	private int physLastLine;

	/**
	 * The last screen line index.
	 */
	private int screenLastLine;

	/** The visible lines count. */
	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;

	private boolean quickCopy;

	// JDiff, error list add stuff here
	private final Box verticalBox;
	private final JScrollBar vertical;
	private final JScrollBar horizontal;

	protected JEditBuffer buffer;

	protected int caret;
	protected int caretLine;
	private int caretScreenLine;

	private final java.util.List<StructureMatcher> structureMatchers;
	private StructureMatcher.Match match;

	private int magicCaret;
	/** Flag that tells if multiple selection is on. */
	protected boolean multi;
	private boolean overwrite;
	private boolean rectangularSelectionMode;

	private boolean dndEnabled;

	// see finishCaretUpdate() & _finishCaretUpdate()
	private boolean queuedCaretUpdate;
	private int queuedScrollMode;
	private boolean queuedFireCaretEvent;
	private int oldCaretLine;

	private boolean joinNonWordChars;
	private boolean ctrlForRectangularSelection;
	//}}}

	//{{{ _setHorizontalOffset() method
	/**
	 * Sets the horizontal offset of drawn lines. This method will
	 * check if the offset do not go too far after the last character
	 * @param horizontalOffset offset The new horizontal offset
	 */
	private void _setHorizontalOffset(int horizontalOffset)
	{
		if(horizontalOffset > 0)
			horizontalOffset = 0;

		if(horizontalOffset == this.horizontalOffset)
			return;

		// Scrolling with trackpad or other device should be kept inside bounds
		int min = Math.min(-(maxHorizontalScrollWidth + charWidth - painter.getWidth()), 0);
		if(horizontalOffset < min)
			horizontalOffset = min;

		setHorizontalOffset(horizontalOffset);
	} //}}}

	//{{{ invalidateSelectedLines() method
	/**
	 * Repaints the lines containing the selection.
	 */
	private void invalidateSelectedLines()
	{
		// to hide line highlight if selections are being added later on
		invalidateLine(caretLine);

		for (Selection s : selectionManager.selection)
			invalidateLineRange(s.startLine,s.endLine);
	} //}}}

	//{{{ finishCaretUpdate() method
	/**
	 * the collapsing of scrolling/event firing inside compound edits
	 * greatly speeds up replace-all.
	 */
	private void finishCaretUpdate(int oldCaretLine,
		int scrollMode, boolean fireCaretEvent)
	{
		queuedFireCaretEvent |= fireCaretEvent;
		queuedScrollMode = Math.max(scrollMode,queuedScrollMode);

		if(queuedCaretUpdate)
			return;

		this.oldCaretLine = oldCaretLine;
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

	//{{{ fireStatusChanged() method
	private void fireStatusChanged(int flag, boolean value)
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == StatusListener.class)
			{
				try
				{
					((StatusListener)listeners[i+1])
						.statusChanged(this,flag,value);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,t);
				}
			}
		}
	} //}}}

	//{{{ fireBracketSelected() method
	private void fireBracketSelected(int line, String text)
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == StatusListener.class)
			{
				try
				{
					((StatusListener)listeners[i+1])
						.bracketSelected(this,line,text);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,this,t);
				}
			}
		}
	} //}}}

	//{{{ _changeLine() method
	private void _changeLine(boolean select, int newCaret)
	{
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
	}//}}}

	//{{{ lineContainsSpaceAndTabs() method
	/**
	 * Check if the line contains only spaces and tabs.
	 *
	 * @param lineIndex the line index
	 * @return <code>true</code> if the line contains only spaces and tabs
	 */
	private boolean lineContainsSpaceAndTabs(int lineIndex)
	{
		final Segment lineSegment = new Segment();
		getLineText(lineIndex,lineSegment);

		for(int j = 0; j < lineSegment.count; j++)
		{
			switch(lineSegment.array[lineSegment.offset + j])
			{
			case ' ':
			case '\t':
				break;
			default:
				return false;
			}
		}
		return true;
	} //}}}

	//{{{ insert() method
	protected void insert(String str, boolean indent)
	{
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

			replaceSelection(StandardUtilities.createWhiteSpace(
				tabSize - pos,0));
		}
		else
			replaceSelection("\t");
	} //}}}

	//{{{ userInputTab() method
	protected void userInputTab()
	{
		if(getSelectionCount() == 1)
		{
			Selection sel = getSelection(0);
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
		else if(getSelectionCount() != 0)
			shiftIndentRight();
		else
			insertTab();
	} //}}}

	//{{{ doWordWrap() method
	/**
	 * Does hard wrap.
	 */
	protected boolean doWordWrap(boolean spaceInserted)
	{
		if(!hardWrap || maxLineLen <= 0)
			return false;

		final Segment lineSegment = new Segment();
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

		int tabSize = buffer.getTabSize();

		String wordBreakChars = buffer.getStringProperty("wordBreakChars");

		int lastInLine = 0; // last character before wrap
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
					lastInLine = i;
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(ch == ' ')
			{
				logicalLength++;
				if(!lastWasSpace &&
					logicalLength <= maxLineLen + 1)
				{
					lastInLine = i;
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(wordBreakChars != null && wordBreakChars.indexOf(ch) != -1)
			{
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastInLine = i;
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else
			{
				lastInLine = i;
				logicalLength++;
				lastWasSpace = false;
			}
		}

		boolean returnValue;

		int insertNewLineAt;
		if(spaceInserted && logicalLength == maxLineLen
			&& lastInLine == caretPos - 1)
		{
			insertNewLineAt = caretPos;
			returnValue = true;
		}
		else if(logicalLength >= maxLineLen && lastWordOffset != -1)
		{
			insertNewLineAt = lastWordOffset;
			returnValue = false;
		}
		else
			return false;

		boolean indent = buffer.getBooleanProperty("autoIndent");
		try
		{
			buffer.beginCompoundEdit();
			buffer.insert(start + insertNewLineAt,"\n");
			// caretLine would have been incremented
			// since insertNewLineAt <= caretPos
			if (indent)
				buffer.indentLine(caretLine,true);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		/* only ever return true if space was pressed
		 * with logicalLength == maxLineLen */
		return returnValue;
	} //}}}

	//{{{ updateStructureHighlightWithDelay() method
	private static void updateBracketHighlightWithDelay()
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

		for (StructureMatcher matcher : structureMatchers)
		{
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
					.trim() + ' ' + text;
				break;
			}
		}

		// get rid of embedded tabs not removed by trim()
		fireBracketSelected(match.startLine + 1,text.replace('\t',' '));
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
	private static class RectParams
	{
		final int extraStartVirt;
		final int extraEndVirt;
		final int newCaret;

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

			boolean bias;
			if(s == null)
				bias = newCaret < caret;
			else if(s.start == caret)
				bias = newCaret <= s.end;
			else if(s.end == caret)
				bias = newCaret <= s.start;
			else
				bias = false;

			RectParams returnValue;
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

	//{{{ delete() method
	private void delete(boolean forward)
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(getSelectionCount() != 0)
		{
			Selection[] selections = getSelection();
			for (Selection s : selections)
			{
				if(s instanceof Selection.Rect)
				{
					Selection.Rect r = (Selection.Rect)s;
					int startColumn = r.getStartColumn(buffer);
					if(startColumn == r.getEndColumn(buffer))
					{
						if(!forward && startColumn == 0)
							getToolkit().beep();
						else
							tallCaretDelete(r,forward);
					}
					else
						setSelectedText(s,null);
				}
				else
					setSelectedText(s,null);
			}
		}
		else if(forward)
		{
			if(caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}

			buffer.remove(caret,1);
		}
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

	//{{{ tallCaretDelete() method
	private void tallCaretDelete(Selection.Rect s, boolean forward)
	{
		try
		{
			buffer.beginCompoundEdit();

			int[] width = new int[1];

			int startCol = s.getStartColumn(buffer);
			int startLine = s.startLine;
			int endLine = s.endLine;
			for(int i = startLine; i <= endLine; i++)
			{
				int offset = buffer.getOffsetOfVirtualColumn(
					i,startCol,width);
				if(offset == -1)
				{
					if(width[0] == startCol)
						offset = getLineLength(i);
					else
					{
						if(i == startLine && !forward)
							shiftTallCaretLeft(s);
						continue;
					}
				}
				offset += buffer.getLineStartOffset(i);
				if(forward)
				{
					if(offset != buffer.getLineEndOffset(i) - 1)
						buffer.remove(offset,1);
				}
				else
					buffer.remove(offset-1,1);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}

	//{{{ shiftTallCaretLeft() method
	private void shiftTallCaretLeft(Selection.Rect s)
	{
		removeFromSelection(s);
		addToSelection(new Selection.Rect(
			buffer,
			s.getStartLine(),s.getStartColumn(buffer) - 1,
			s.getEndLine(),s.getEndColumn(buffer) - 1));
	} //}}}

	//{{{ setMaxLineLength() method
	private void setMaxLineLength(int maxLineLen)
	{
		this.maxLineLen = maxLineLen;

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
			char[] foo = new char[maxLineLen];
			for(int i = 0; i < foo.length; i++)
			{
				foo[i] = ' ';
			}
			int maxRenderedLineLen = (int)painter.getFont().getStringBounds(
				foo,0,foo.length,
				painter.getFontRenderContext())
				.getWidth();

			if (softWrap && painter.getWidth() < maxRenderedLineLen)
			{
				wrapToWidth = true;
				wrapMargin = painter.getWidth() - charWidth * 3;
			}
			else
			{
				wrapToWidth = false;
				wrapMargin = maxRenderedLineLen;
			}
		}
	} //}}}

	//{{{ addExplicitFold() method
	/**
	 * Add an explicit fold.
	 * You should call this method inside a compoundEdit in the buffer.
	 * You must also check if the buffer fold mode is explicit before
	 * calling this method.
	 *
	 * @param caretStart the starting offset
	 * @param caretEnd   the end offset
	 * @param lineStart  the start line
	 * @param lineEnd    the end line
	 * @since jEdit 4.3pre3
	 */
	protected int addExplicitFold(int caretStart, int caretEnd, int lineStart, int lineEnd)
	{
		// need to "fix" the caret position so that we get the right rule.
		// taking the start offset one char ahead and the end offset one char
		// behing makes sure we get the right rule for the text being
		// wrapped (tricky around mode boundaries, e.g., php code embedded
		// in HTML code)
		int startCaret = caretStart < buffer.getLength() ? caretStart + 1 : caretStart;
		int endCaret = caretEnd > 0 ? caretEnd - 1 : caretEnd;

		String startLineComment = buffer.getContextSensitiveProperty(startCaret,"lineComment");
		String startCommentStart = buffer.getContextSensitiveProperty(startCaret,"commentStart");
		String startCommentEnd = buffer.getContextSensitiveProperty(startCaret,"commentEnd");
		String endLineComment = buffer.getContextSensitiveProperty(endCaret,"lineComment");
		String endCommentStart = buffer.getContextSensitiveProperty(endCaret,"commentStart");
		String endCommentEnd = buffer.getContextSensitiveProperty(endCaret,"commentEnd");

		String start;
		int caretBack = 1;
		if(startLineComment != null)
			start = startLineComment + "{{{ ";
		else if(startCommentStart != null && startCommentEnd != null)
		{
			start = startCommentStart + "{{{  " + startCommentEnd;
			caretBack = 2 + startCommentEnd.length();
		}
		else
			start = "{{{ ";

		if (startLineComment != null)
		{
			// add a new line if there's text after the comment
			// we're inserting
			if (buffer.getLineLength(lineStart) != caretStart)
			{
				start += '\n';
			}
		}
		else
		{
			// always insert a new line if there's no comment character.
			start += "\n";
		}

		String end;
		if(endLineComment != null)
			end = endLineComment + "}}}";
		else if(endCommentStart != null && endCommentEnd != null)
			end = endCommentStart + "}}}" + endCommentEnd;
		else
			end = "}}}";

		String line = buffer.getLineText(lineStart);
		String whitespace = line.substring(0,
			StandardUtilities.getLeadingWhiteSpace(line));
		caretBack += whitespace.length();
		if (caretStart == caretEnd)
		{
			caretBack += end.length() + 1;
			int lineStartOffset = buffer.getLineStartOffset(lineStart);
			if (lineStartOffset  + whitespace.length() != caretStart)
			{
				caretBack++;
			}
		}

		if (endLineComment != null)
		{
			// if we're inserting a line comment into a non-empty
			// line, we'll need to add a line break so we don't
			// comment out existing code.
			if (buffer.getLineLength(lineEnd) != caretEnd)
			{
				end += '\n';
			}
		}
		else
		{
			// always insert a new line if there's no comment character.
			end += "\n";
		}

		if(caretEnd == buffer.getLineStartOffset(lineEnd))
			buffer.insert(caretEnd,end);
		else
		{
			CharSequence lineText = buffer.getSegment(caretEnd - 1, 1);
			if (Character.isWhitespace(lineText.charAt(0)))
				buffer.insert(caretEnd, end);
			else
				buffer.insert(caretEnd,' ' + end);
		}

		buffer.insert(caretStart,start + whitespace);

		return caretBack;
	} //}}}

	//{{{ rangeLineComment() method
	/**
	 * This method will surround each selected line with a range comment.
	 * This is used when calling line comment if the edit mode doesn't have
	 * a line comment property
	 * @since jEdit 4.3pre10
	 */
	private void rangeLineComment()
	{
		String commentStart = buffer.getContextSensitiveProperty(caret,"commentStart");
		String commentEnd = buffer.getContextSensitiveProperty(caret,"commentEnd");
		if(!buffer.isEditable() || commentStart == null || commentEnd == null
			|| commentStart.length() == 0 || commentEnd.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		commentStart += ' ';
		commentEnd = ' ' + commentEnd;


		try
		{
			buffer.beginCompoundEdit();
			int[] lines = getSelectedLines();
			for(int i = 0; i < lines.length; i++)
			{
				String text = getLineText(lines[i]);
				if (text.trim().length() == 0)
					continue;
				buffer.insert(getLineEndOffset(lines[i]) - 1,
					commentEnd);
				buffer.insert(getLineStartOffset(lines[i])
					+ StandardUtilities.getLeadingWhiteSpace(text),
					commentStart);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	} //}}}

	//{{{ joinLine() method
	/**
	 * Join a line with the next line.
	 * If you use this method you have to lock the buffer in compound edit mode.
	 * @param line the line number that will be joined with the next line
	 */
	private void joinLineAt(int line)
	{
		if (line >= buffer.getLineCount() - 1)
			return;
		int end = getLineEndOffset(line);
		CharSequence nextLineText = buffer.getLineSegment(line + 1);
		buffer.remove(end - 1,StandardUtilities.getLeadingWhiteSpace(
			nextLineText) + 1);
		if (nextLineText.length() != 0)
			buffer.insert(end - 1, " ");
	} //}}}
	//}}}

	//{{{ isRightClickPopupEnabled() method
	/**
	 * Returns if the right click popup menu is enabled. The Gestures
	 * plugin uses this API.
	 * @since jEdit 4.2pre13
	 */
	public boolean isRightClickPopupEnabled()
	{
		return popupEnabled;
	} //}}}

	//{{{ setRightClickPopupEnabled() method
	/**
	 * Sets if the right click popup menu is enabled. The Gestures
	 * plugin uses this API.
	 * @since jEdit 4.2pre13
	 */
	public void setRightClickPopupEnabled(boolean popupEnabled)
	{
		this.popupEnabled = popupEnabled;
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

	//{{{ handlePopupTrigger() method
	/**
	 * Do the same thing as right-clicking on the text area. The Gestures
	 * plugin uses this API.
	 * @since jEdit 4.2pre13
	 */
	public void handlePopupTrigger(MouseEvent evt)
	{
		// Rebuild popup menu every time the menu is requested.
		createPopupMenu(evt);

		int x = evt.getX();
		int y = evt.getY();

		int dragStart = xyToOffset(x,y,
			!(painter.isBlockCaretEnabled()
			|| isOverwriteEnabled()));

		if(getSelectionCount() == 0 || multi)
			moveCaretPosition(dragStart,false);
		showPopupMenu(popup,this,x,y,false);
	} //}}}

	//{{{ createPopupMenu() method
	/**
	 * Creates the popup menu.
	 * @since 4.3pre15
	 */
	public void createPopupMenu(MouseEvent evt)
	{
		if (popup == null)
			popup = new JPopupMenu();
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the popup menu below the current caret position.
	 * @since 4.3pre10
	 */
	public void showPopupMenu()
	{
		if (!popup.isVisible() && hasFocus())
		{
			Point caretPos = offsetToXY(getCaretPosition());
			if (caretPos != null)
			{
				// Open the context menu below the caret
				int charHeight = getPainter().getLineHeight();
				showPopupMenu(popup,
					painter,caretPos.x,caretPos.y + charHeight,true);
			}
		}
	} //}}}

	//{{{ showPopupMenu() method - copied from GUIUtilities
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param point If true, then the popup originates from a single point;
	 * otherwise it will originate from the component itself. This affects
	 * positioning in the case where the popup does not fit onscreen.
	 *
	 * @since jEdit 4.1pre1
	 */
	private static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y, boolean point)
	{
		int offsetX = 0;
		int offsetY = 0;

		int extraOffset = point ? 1 : 0;

		Component win = comp;
		while(!(win instanceof Window || win == null))
		{
			offsetX += win.getX();
			offsetY += win.getY();
			win = win.getParent();
		}

		if(win != null)
		{
			Dimension size = popup.getPreferredSize();

			Rectangle screenSize = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getMaximumWindowBounds();

			if(x + offsetX + size.width + win.getX() > screenSize.width
				&& x + offsetX + win.getX() >= size.width)
			{
				//System.err.println("x overflow");
				if(point)
					x -= size.width + extraOffset;
				else
					x = win.getWidth() - size.width - offsetX + extraOffset;
			}
			else
			{
				x += extraOffset;
			}

			//System.err.println("y=" + y + ",offsetY=" + offsetY
			//	+ ",size.height=" + size.height
			//	+ ",win.height=" + win.getHeight());
			if(y + offsetY + size.height + win.getY() > screenSize.height
				&& y + offsetY + win.getY() >= size.height)
			{
				if(point)
					y = win.getHeight() - size.height - offsetY + extraOffset;
				else
					y = -size.height - 1;
			}
			else
			{
				y += extraOffset;
			}

			popup.show(comp,x,y);
		}
		else
			popup.show(comp,x + extraOffset,y + extraOffset);

	} //}}}

	//{{{ Inner classes

	//{{{ CaretBlinker class
	private static class CaretBlinker implements ActionListener
	{
		//{{{ actionPerformed() method
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			if(focusedComponent != null && focusedComponent.hasFocus())
				focusedComponent.blinkCaret();
		} //}}}
	} //}}}

	//{{{ MutableCaretEvent class
	private class MutableCaretEvent extends CaretEvent
	{
		//{{{ MutableCaretEvent constructor
		MutableCaretEvent()
		{
			super(TextArea.this);
		} //}}}

		//{{{ getDot() method
		@Override
		public int getDot()
		{
			return getCaretPosition();
		} //}}}

		//{{{ getMark() method
		@Override
		public int getMark()
		{
			return getMarkPosition();
		} //}}}
	} //}}}

	//{{{ AdjustHandler class
	private class AdjustHandler implements AdjustmentListener
	{
		//{{{ adjustmentValueChanged() method
		@Override
		public void adjustmentValueChanged(AdjustmentEvent evt)
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
	private class FocusHandler implements FocusListener
	{
		//{{{ focusGained() method
		@Override
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

			focusedComponent = TextArea.this;
		} //}}}

		//{{{ focusLost() method
		@Override
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

	//{{{ MouseWheelHandler class
	private class MouseWheelHandler implements MouseWheelListener
	{
		@Override
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			/****************************************************
			 * move caret depending on pressed control-keys:
			 * - Alt: move cursor, do not select
			 * - Alt+(shift or control): move cursor, select
			 * - shift: scroll horizontally
			 * - control: scroll single line
			 * - <else>: scroll 3 lines
			 ****************************************************/
			if(e.isAltDown())
			{
				boolean select = e.isShiftDown()
					|| e.isControlDown();
				if(e.getWheelRotation() < 0)
					goToPrevLine(select);
				else
					goToNextLine(select);
			}
			else if(e.getScrollType()
				== MouseWheelEvent.WHEEL_BLOCK_SCROLL)
			{
				if(e.isShiftDown())
				{
					// Wheel orientation is reversed so we negate the charwidth
					_setHorizontalOffset(getHorizontalOffset()
						+ (e.getWheelRotation() > 0 ? 1 : -1) * painter.getWidth());
				}
				else
				{
					if(e.getWheelRotation() > 0)
						scrollDownPage();
					else
						scrollUpPage();
				}
			}
			else if(e.isControlDown() && e.isShiftDown())
			{
				if(e.getWheelRotation() > 0)
					scrollDownPage();
				else
					scrollUpPage();
			}
			else if(e.isControlDown())
			{
				setFirstLine(getFirstLine()
					+ e.getWheelRotation());
			}
			else if(e.getScrollType()
				== MouseWheelEvent.WHEEL_UNIT_SCROLL)
			{
				if(e.isShiftDown())
				{
					_setHorizontalOffset(getHorizontalOffset()
						+ (-charWidth * e.getUnitsToScroll()));
				}
				else
				{
					setFirstLine(getFirstLine()
						+ e.getUnitsToScroll());
				}
			}
			else
			{
				if(e.isShiftDown())
				{
					_setHorizontalOffset(getHorizontalOffset()
						+ (-charWidth * e.getWheelRotation()));
				}
				else
				{
					setFirstLine(getFirstLine()
						+ 3 * e.getWheelRotation());
				}
			}
		}
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
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				if(focusedComponent != null)
					focusedComponent.updateStructureHighlight();
			}
		});
		structureTimer.setInitialDelay(100);
		structureTimer.setRepeats(false);
	} //}}}

	public TabExpander getTabExpander() 
	{
		if(buffer.getBooleanProperty("elasticTabstops"))
		{
			return elasticTabstopsExpander;
		}
		else
		{
			return painter;
		}
	}
}
