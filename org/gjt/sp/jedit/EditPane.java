/*
 * EditPane.java - Text area and buffer switcher
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit;

import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * A panel containing a text area. Each edit pane can edit one buffer at
 * a time.
 * @author Slava Pestov
 * @version $Id$
 */
public class EditPane extends JPanel implements EBComponent
{
	/**
	 * Returns the view containing this edit pane.
	 * @since jEdit 2.5pre2
	 */
	public View getView()
	{
		return view;
	}

	/**
	 * Returns the current buffer.
	 * @since jEdit 2.5pre2
	 */
	public Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Sets the current buffer.
	 * @param buffer The buffer to edit.
	 * @since jEdit 2.5pre2
	 */
	public void setBuffer(final Buffer buffer)
	{
		if(this.buffer == buffer)
			return;

		if(buffer.isClosed())
			throw new InternalError(buffer + " has been closed");

		buffer.endCompoundEdit();

		recentBuffer = this.buffer;
		if(recentBuffer != null)
			saveCaretInfo();
		this.buffer = buffer;

		textArea.setBuffer(buffer);

		if(!init)
		{
			view.updateTitle();

			if(bufferSwitcher != null)
			{
				if(bufferSwitcher.getSelectedItem() != buffer)
					bufferSwitcher.setSelectedItem(buffer);
			}

			EditBus.send(new EditPaneUpdate(this,EditPaneUpdate
				.BUFFER_CHANGED));
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// only do this if we are the current edit pane
				if(view.getEditPane() == EditPane.this
					&& (bufferSwitcher == null
					|| !bufferSwitcher.isPopupVisible()))
				{
					focusOnTextArea();
				}
			}
		});

		// Only do this after all I/O requests are complete
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				loadCaretInfo();
				buffer.checkModTime(view);
			}
		};

		if(buffer.isPerformingIO())
			VFSManager.runInAWTThread(runnable);
		else
			runnable.run();
	}

	/**
	 * Selects the previous buffer.
	 * @since jEdit 2.7pre2
	 */
	public void prevBuffer()
	{
		Buffer buffer = this.buffer.getPrev();
		if(buffer == null)
			setBuffer(jEdit.getLastBuffer());
		else
			setBuffer(buffer);
	}

	/**
	 * Selects the next buffer.
	 * @since jEdit 2.7pre2
	 */
	public void nextBuffer()
	{
		Buffer buffer = this.buffer.getNext();
		if(buffer == null)
			setBuffer(jEdit.getFirstBuffer());
		else
			setBuffer(buffer);
	}

	/**
	 * Selects the most recently edited buffer.
	 * @since jEdit 2.7pre2
	 */
	public void recentBuffer()
	{
		if(recentBuffer != null)
			setBuffer(recentBuffer);
		else
			getToolkit().beep();
	}

	/**
	 * Sets the focus onto the text area.
	 * @since jEdit 2.5pre2
	 */
	public void focusOnTextArea()
	{
		textArea.grabFocus();
		// trying to work around buggy focus handling in some
		// Java versions
//		if(!textArea.hasFocus())
//		{
//			textArea.processFocusEvent(new FocusEvent(textArea,
//				FocusEvent.FOCUS_GAINED));
//		}
	}

	/**
	 * Returns the view's text area.
	 * @since jEdit 2.5pre2
	 */
	public JEditTextArea getTextArea()
	{
		return textArea;
	}

	/**
	 * Saves the caret information to the current buffer.
	 * @since jEdit 2.5pre2
	 */
	public void saveCaretInfo()
	{
		buffer.putProperty(Buffer.CARET,new Integer(
			textArea.getCaretPosition()));

		Selection[] selection = textArea.getSelection();
		if(selection != null)
			buffer.putProperty(Buffer.SELECTION,selection);

		buffer.putProperty(Buffer.SCROLL_VERT,new Integer(
			buffer.virtualToPhysical(textArea.getFirstLine())));
		buffer.putProperty(Buffer.SCROLL_HORIZ,new Integer(
			textArea.getHorizontalOffset()));
	}

	/**
	 * Loads the caret information from the curret buffer.
	 * @since jEdit 2.5pre2
	 */
	public void loadCaretInfo()
	{
		Integer caret = (Integer)buffer.getProperty(Buffer.CARET);
		Selection[] selection = (Selection[])buffer.getProperty(Buffer.SELECTION);

		Integer firstLine = (Integer)buffer.getProperty(Buffer.SCROLL_VERT);
		Integer horizontalOffset = (Integer)buffer.getProperty(Buffer.SCROLL_HORIZ);

		if(caret != null)
		{
			textArea.setCaretPosition(Math.min(caret.intValue(),
				buffer.getLength()));
		}

		if(selection != null)
			textArea.setSelection(selection);

		if(firstLine != null)
			textArea.setFirstLine(buffer.physicalToVirtual(firstLine.intValue()));

		if(horizontalOffset != null)
			textArea.setHorizontalOffset(horizontalOffset.intValue());
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
		{
			propertiesChanged();
			loadBufferSwitcher();
		}
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	/**
	 * Returns 0,0 for split pane compatibility.
	 */
	public final Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	}

	// package-private members
	EditPane(View view, Buffer buffer)
	{
		super(new BorderLayout());

		init = true;

		this.view = view;

		EditBus.addToBus(this);

		textArea = new JEditTextArea(view);

		add(BorderLayout.CENTER,textArea);
		markerHighlight = new MarkerHighlight();
		textArea.getGutter().addCustomHighlight(markerHighlight);

		propertiesChanged();

		if(buffer == null)
			setBuffer(jEdit.getFirstBuffer());
		else
			setBuffer(buffer);

		loadBufferSwitcher();

		init = false;
	}

	void close()
	{
		saveCaretInfo();
		EditBus.send(new EditPaneUpdate(this,EditPaneUpdate.DESTROYED));
		EditBus.removeFromBus(this);
	}

	// private members
	private boolean init;
	private View view;
	private Buffer buffer;
	private Buffer recentBuffer;
	private BufferSwitcher bufferSwitcher;
	private JEditTextArea textArea;
	private MarkerHighlight markerHighlight;

	private void propertiesChanged()
	{
		TextAreaPainter painter = textArea.getPainter();

		painter.setFont(UIManager.getFont("TextArea.font"));
		painter.setBracketHighlightEnabled(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		painter.setBracketHighlightColor(
			jEdit.getColorProperty("view.bracketHighlightColor"));
		painter.setEOLMarkersPainted(jEdit.getBooleanProperty(
			"view.eolMarkers"));
		painter.setEOLMarkerColor(
			jEdit.getColorProperty("view.eolMarkerColor"));
		painter.setWrapGuidePainted(jEdit.getBooleanProperty(
			"view.wrapGuide"));
		painter.setWrapGuideColor(
			jEdit.getColorProperty("view.wrapGuideColor"));
		painter.setCaretColor(
			jEdit.getColorProperty("view.caretColor"));
		painter.setSelectionColor(
			jEdit.getColorProperty("view.selectionColor"));
		painter.setBackground(
			jEdit.getColorProperty("view.bgColor"));
		painter.setForeground(
			jEdit.getColorProperty("view.fgColor"));
		painter.setBlockCaretEnabled(jEdit.getBooleanProperty(
			"view.blockCaret"));
		painter.setLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.lineHighlight"));
		painter.setLineHighlightColor(
			jEdit.getColorProperty("view.lineHighlightColor"));
		painter.setAntiAliasEnabled(jEdit.getBooleanProperty(
			"view.antiAlias"));
		painter.setFractionalFontMetricsEnabled(jEdit.getBooleanProperty(
			"view.fracFontMetrics"));
		painter.setStyles(GUIUtilities.loadStyles(
			jEdit.getProperty("view.font"),
			jEdit.getIntegerProperty("view.fontsize",12)));

		Gutter gutter = textArea.getGutter();
		gutter.setExpanded(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		int interval = jEdit.getIntegerProperty(
			"view.gutter.highlightInterval",5);
		gutter.setHighlightInterval(interval);
		gutter.setCurrentLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		gutter.setBackground(
			jEdit.getColorProperty("view.gutter.bgColor"));
		gutter.setForeground(
			jEdit.getColorProperty("view.gutter.fgColor"));
		gutter.setHighlightedForeground(
			jEdit.getColorProperty("view.gutter.highlightColor"));
		gutter.setFoldColor(
			jEdit.getColorProperty("view.gutter.foldColor"));
		markerHighlight.setMarkerHighlightColor(
			jEdit.getColorProperty("view.gutter.markerColor"));
		markerHighlight.setHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.markerHighlight"));
		gutter.setCurrentLineForeground(
			jEdit.getColorProperty("view.gutter.currentLineColor"));
		String alignment = jEdit.getProperty(
			"view.gutter.numberAlignment");
		if ("right".equals(alignment))
		{
			gutter.setLineNumberAlignment(Gutter.RIGHT);
		}
		else if ("center".equals(alignment))
		{
			gutter.setLineNumberAlignment(Gutter.CENTER);
		}
		else // left == default case
		{
			gutter.setLineNumberAlignment(Gutter.LEFT);
		}

		gutter.setFont(jEdit.getFontProperty("view.gutter.font"));

		int width = jEdit.getIntegerProperty(
			"view.gutter.borderWidth",3);
		gutter.setBorder(width,
			jEdit.getColorProperty("view.gutter.focusBorderColor"),
			jEdit.getColorProperty("view.gutter.noFocusBorderColor"),
			textArea.getPainter().getBackground());

		textArea.setCaretBlinkEnabled(jEdit.getBooleanProperty(
			"view.caretBlink"));

		textArea.setElectricScroll(jEdit.getIntegerProperty(
			"view.electricBorders",0));

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu("view.context"));

		textArea.setMiddleMousePasteEnabled(jEdit.getBooleanProperty(
			"view.middleMousePaste"));
	}

	private void loadBufferSwitcher()
	{
		if(jEdit.getBooleanProperty("view.showBufferSwitcher"))
		{
			if(bufferSwitcher == null)
			{
				bufferSwitcher = new BufferSwitcher(this);
				add(BorderLayout.NORTH,bufferSwitcher);
				bufferSwitcher.updateBufferList();
				revalidate();
			}
		}
		else if(bufferSwitcher != null)
		{
			remove(bufferSwitcher);
			revalidate();
			bufferSwitcher = null;
		}
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer _buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CREATED)
		{
			if(bufferSwitcher != null)
				bufferSwitcher.updateBufferList();

			/* When closing the last buffer, the BufferUpdate.CLOSED
			 * handler doesn't call setBuffer(), because null buffers
			 * are not supported. Instead, it waits for the subsequent
			 * 'Untitled' file creation. */
			if(buffer.isClosed())
				setBuffer(jEdit.getFirstBuffer());
		}
		else if(msg.getWhat() == BufferUpdate.CLOSED)
		{
			if(bufferSwitcher != null)
				bufferSwitcher.updateBufferList();

			if(_buffer == buffer)
			{
				Buffer newBuffer = (recentBuffer != null ?
					recentBuffer : _buffer.getPrev());
				if(newBuffer != null && !newBuffer.isClosed())
					setBuffer(newBuffer);
				else if(jEdit.getBufferCount() != 0)
					setBuffer(jEdit.getFirstBuffer());

				recentBuffer = null;
			}
			else if(_buffer == recentBuffer)
				recentBuffer = null;
		}
		else if(msg.getWhat() == BufferUpdate.LOAD_STARTED)
		{
			if(_buffer == buffer)
			{
				textArea.setCaretPosition(0);
				textArea.getPainter().repaint();
			}
		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			if(_buffer == buffer)
			{
				if(bufferSwitcher != null)
				{
					if(buffer.isDirty())
						bufferSwitcher.repaint();
					else
						bufferSwitcher.updateBufferList();
				}
			}
		}
		else if(msg.getWhat() == BufferUpdate.LOADED)
		{
			if(_buffer == buffer)
			{
				textArea.repaint();
				textArea.updateScrollBars();
				if(bufferSwitcher != null)
					bufferSwitcher.updateBufferList();

				if(view.getEditPane() == this)
				{
					StatusBar status = view.getStatus();
					status.repaintCaretStatus();
					status.updateBufferStatus();
					status.updateMiscStatus();
				}

				loadCaretInfo();
			}

		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(_buffer == buffer)
				textArea.getGutter().repaint();
		}
		else if(msg.getWhat() == BufferUpdate.MODE_CHANGED)
		{
			if(_buffer == buffer)
			{
				textArea.getPainter().repaint();

				if(view.getEditPane() == this)
					view.getStatus().updateBufferStatus();
			}
		}
		else if(msg.getWhat() == BufferUpdate.ENCODING_CHANGED)
		{
			if(_buffer == buffer)
			{
				if(view.getEditPane() == this)
					view.getStatus().updateBufferStatus();
			}
		}
	}
}
