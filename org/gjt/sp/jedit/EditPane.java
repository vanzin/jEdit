/*
 * EditPane.java - Text area and buffer switcher
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.bufferset.BufferSetListener;
import org.gjt.sp.jedit.gui.BufferSwitcher;
import org.gjt.sp.jedit.gui.StatusBar;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.options.GutterOptionPane;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.textarea.AntiAlias;
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.textarea.GutterPopupHandler;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.MouseHandler;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.StatusListener;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.jedit.textarea.TextAreaTransferHandler;
import org.gjt.sp.util.SyntaxUtilities;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * A panel containing a text area.<p>
 *
 * In a BeanShell script, you can obtain the current edit pane from the
 * <code>editPane</code> variable.<p>
 *
 *
 * Each View can have multiple editPanes, one is active at a time.
 * Each EditPane has a single JEditTextArea, and is operating on single buffer.
 * The EditPane also can switch buffers.
 *
 * This is the "controller" between a JEditTextArea (view) and a buffer (model).
 *
 * This class does not have a public constructor.
 * Edit panes can be created and destroyed using methods in the
 * {@link View} class.<p>
 *
 *
 * @see View#splitHorizontally()
 * @see View#splitVertically()
 * @see View#unsplitCurrent()
 * @see View#unsplit()
 * @see View#getEditPane()
 * @see View#getEditPanes()
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class EditPane extends JPanel implements BufferSetListener
{
	//{{{ getView() method
	/**
	 * Returns the view containing this edit pane.
	 * @return the view that contains this EditPane
	 * @since jEdit 2.5pre2
	 */
	public View getView()
	{
		return view;
	} //}}}

	// {{{ get(TextArea) method
	/**
	 * Returns the EditPane of a TextArea.
	 *
	 * @param ta the textArea
	 * @return the EditPane containing the TextArea.
	 */
	public static EditPane get(TextArea ta)
	{
		if (ta == null) return null;
		return (EditPane)SwingUtilities.getAncestorOfClass(EditPane.class, ta);
	} // }}}

	//{{{ getBuffer() method
	/**
	 * Returns the current buffer.
	 * @return the current buffer
	 * @since jEdit 2.5pre2
	 */
	public Buffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ setBuffer() methods
	/**
	 * Sets the current buffer.
	 * @param buffer The buffer to edit.
	 * @since jEdit 2.5pre2
	 */
	public void setBuffer(Buffer buffer)
	{
		setBuffer(buffer, true);
	}

	/**
	 * Sets the current buffer.
	 * @param buffer The buffer to edit.
	 * @param requestFocus true if the textarea should request focus, false otherwise
	 * @since jEdit 4.3pre6
	 */
	public void setBuffer(final Buffer buffer, boolean requestFocus)
	{
		if(buffer == null)
			throw new NullPointerException();

		if(this.buffer == buffer)
			return;

		if (bufferSet.indexOf(buffer) == -1)
		{
			jEdit.getBufferSetManager().addBuffer(this, buffer);
		}
		//if(buffer.insideCompoundEdit())
		//	buffer.endCompoundEdit();
		EditBus.send(new BufferChanging(this, buffer));
		if (bufferSet.indexOf(this.buffer) != -1)
		{
			// when closing the last buffer of a bufferSet, the current buffer will still be the closed
			// buffer until a new empty buffer is created.
			// So if the current buffer is not anymore in the bufferSet, do not set the recentBuffer
			recentBuffer = this.buffer;
		}
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
				bufferSwitcher.setToolTipText(buffer.getPath());
			}

			EditBus.send(new EditPaneUpdate(this,EditPaneUpdate
				.BUFFER_CHANGED));
		}

		if (requestFocus)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					// only do this if we are the current edit pane
					if(view.getEditPane() == EditPane.this
						&& (bufferSwitcher == null
						|| !bufferSwitcher.isPopupVisible()))
					{
						textArea.requestFocus();
					}
				}
			});
		}

		// If the buffer is loading, the caret info will be loaded on
		// BufferUpdate.LOADED. Otherwise, we don't need to wait for IO.
		if (!buffer.isLoading())
		{
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					// avoid a race condition
					// see bug #834338
					if(buffer == getBuffer())
						loadCaretInfo();
				}
			});
		}
	} //}}}

	//{{{ prevBuffer() method
	/**
	 * Selects the previous buffer.
	 * @since jEdit 2.7pre2
	 */
	public void prevBuffer()
	{
		Buffer buffer = bufferSet.getPreviousBuffer(bufferSet.indexOf(this.buffer));
		setBuffer(buffer);
	} //}}}

	//{{{ nextBuffer() method
	/**
	 * Selects the next buffer.
	 * @since jEdit 2.7pre2
	 */
	public void nextBuffer()
	{
		Buffer buffer = bufferSet.getNextBuffer(bufferSet.indexOf(this.buffer));
		setBuffer(buffer);
	} //}}}

	//{{{ recentBuffer() method
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
	} //}}}

	//{{{ focusOnTextArea() method
	/**
	 * Sets the focus onto the text area.
	 * @since jEdit 2.5pre2
	 */
	public void focusOnTextArea()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				textArea.requestFocus();
			}
		});
	} //}}}

	//{{{ getTextArea() method
	/**
	 * Returns the view's text area.
	 * @return the text area of the edit pane
	 * @since jEdit 2.5pre2
	 */
	public JEditTextArea getTextArea()
	{
		return textArea;
	} //}}}

	//{{{ getBufferSwitcher() method
	/**
	 * Returns the buffer switcher combo box instance.
	 * @return the buffer switcher (it can be null)
	 * @since jEdit 4.1pre8
	 */
	public BufferSwitcher getBufferSwitcher()
	{
		return bufferSwitcher;
	} //}}}

	//{{{ focusBufferSwitcher() method
	/**
	 * Pops up and focuses on the buffer switcher combo box.
	 * @since jEdit 4.3pre18
	 * (previously known as showBufferSwitcher)
	 */
	public void focusBufferSwitcher()
	{
		if(bufferSwitcher == null)
			getToolkit().beep();
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					bufferSwitcher.requestFocus();
					bufferSwitcher.showPopup();
				}

			});
		}
	} //}}}

	//{{{ saveCaretInfo() method
	/**
	 * Saves the caret information to the current buffer.
	 * @since jEdit 2.5pre2
	 */
	public void saveCaretInfo()
	{
		if(!buffer.isLoaded())
			return;

		buffer.setIntegerProperty(Buffer.CARET,
			textArea.getCaretPosition());

		CaretInfo caretInfo = caretsForPath.get(buffer.getPath());
		if (caretInfo == null)
		{
			caretInfo = new CaretInfo();
			caretsForPath.put(buffer.getPath(), caretInfo);
		}
		caretInfo.caret = textArea.getCaretPosition();


		Selection[] selection = textArea.getSelection();
		for(int i = 0; i < selection.length; i++)
			selection[i] = (Selection)selection[i].clone();
		buffer.setProperty(Buffer.SELECTION,selection);
		caretInfo.selection = selection;

		caretInfo.rectangularSelection = textArea.isRectangularSelectionEnabled();
		caretInfo.multipleSelection = textArea.isMultipleSelectionEnabled();

		buffer.setIntegerProperty(Buffer.SCROLL_VERT,
			textArea.getFirstPhysicalLine());
		caretInfo.scrollVert = textArea.getFirstPhysicalLine();
		buffer.setIntegerProperty(Buffer.SCROLL_HORIZ,
			textArea.getHorizontalOffset());
		caretInfo.scrollHoriz = textArea.getHorizontalOffset();
		if (!buffer.isUntitled())
		{
			BufferHistory.setEntry(buffer.getPath(), textArea.getCaretPosition(),
				(Selection[])buffer.getProperty(Buffer.SELECTION),
				buffer.getStringProperty(JEditBuffer.ENCODING),
				buffer.getMode().getName());
		}
	} //}}}

	//{{{ loadCaretInfo() method
	/**
	 * Loads the caret and selection information from this EditPane, fall
	 * back to the information from the current buffer if none is already
	 * in this EditPane.
	 * @since jEdit 2.5pre2
	 */
	public void loadCaretInfo()
	{
		// get our internal map of buffer -> CaretInfo since there might
		// be current info already
		CaretInfo caretInfo = caretsForPath.get(buffer.getPath());
		if (caretInfo == null)
		{
			caretInfo = new CaretInfo();
		}

		// set the position of the caret itself.
		// Caret position could be stored in the internal map already,
		// if so, use that one first.  Otherwise, fall back to any
		// previously saved caret position that was stored in the
		// buffer properties.
		int caret = caretInfo.caret;
		if (caret == -1 || buffer.getBooleanProperty(Buffer.CARET_POSITIONED))
		{
			Integer i = (Integer) buffer.getProperty(Buffer.CARET);
			caret = i == null ? -1 : i;
		}
		buffer.unsetProperty(Buffer.CARET_POSITIONED);


		if(caret != -1)
			textArea.setCaretPosition(Math.min(caret,
				buffer.getLength()));

		// set any selections
		Selection[] selection = caretInfo.selection;
		if ( selection == null )
		{
			selection = (Selection[]) buffer.getProperty(Buffer.SELECTION);
		}
		if(selection != null)
		{
			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				int max = buffer.getLength();
				if(s.getStart() > max || s.getEnd() > max)
					selection[i] = null;
			}
		}
		textArea.setSelection(selection);
		textArea.setRectangularSelectionEnabled(caretInfo.rectangularSelection);
		textArea.setMultipleSelectionEnabled(caretInfo.multipleSelection);
		// set firstLine value
		int firstLine = caretInfo.scrollVert;
		if ( firstLine == -1 )
		{
			Integer i = (Integer) buffer.getProperty(Buffer.SCROLL_VERT);
			firstLine = i == null ? -1 : i;
		}

		if(firstLine != -1)
			textArea.setFirstPhysicalLine(firstLine);

		// set horizontal offset
		int horizontalOffset = caretInfo.scrollHoriz;
		if (horizontalOffset == -1)
		{
			Integer i = (Integer) buffer.getProperty(Buffer.SCROLL_HORIZ);
			horizontalOffset = i == null ? -1 : i;
		}

		if(horizontalOffset != -1)
			textArea.setHorizontalOffset(horizontalOffset);

		/* Silly bug workaround #8694. If you look at the above code,
		 * note that we restore the saved caret position first, then
		 * scroll to the saved location. However, the caret changing
		 * can itself result in scrolling to a different location than
		 * what was saved; and since moveCaretPosition() calls
		 * updateBracketHighlight(), the bracket highlight's out of
		 * bounds calculation will rely on a different set of physical
		 * first/last lines than what we will end up with eventually.
		 * Instead of confusing the user with status messages that
		 * appear at random when switching buffers, we simply hide the
		 * message altogether. */
		view.getStatus().setMessage(null);
	} //}}}

	//{{{ bufferRenamed() method
	/**
	 * This method should be called by the Buffer when the path is changing.
	 * @param oldPath the old path of the buffer
	 * @param newPath the new path of the buffer
	 */
	void bufferRenamed(String oldPath, String newPath)
	{
		CaretInfo caretInfo = caretsForPath.remove(oldPath);
		if (caretInfo != null)
			caretsForPath.put(newPath, caretInfo);

	} //}}}

	//{{{ CaretInfo class
	/**
	 * Need to track this info for each buffer that this EditPane might edit
	 * since a buffer may be open in more than one EditPane at a time.  That
	 * means we need to track this info at the EditPane level rather than
	 * the buffer level.
	 */
	private static class CaretInfo
	{
		public int caret = -1;
		public Selection[] selection;
		public int scrollVert = -1;
		public int scrollHoriz = -1;
		public boolean rectangularSelection;
		public boolean multipleSelection;
	} //}}}

	//{{{ goToNextMarker() method
	/**
	 * Moves the caret to the next marker.
	 * @since jEdit 4.3pre3
	 */
	public void goToNextMarker(boolean select)
	{
		java.util.List<Marker> markers = buffer.getMarkers();
		if(markers.isEmpty())
		{
			getToolkit().beep();
			return;
		}

		Marker marker = null;

		int caret = textArea.getCaretPosition();

		for(int i = 0; i < markers.size(); i++)
		{
			Marker _marker = markers.get(i);
			if(_marker.getPosition() > caret)
			{
				marker = _marker;
				break;
			}
		}
		// the markers list is not empty at this point
		if(marker == null)
			marker = markers.get(0);

		if(select)
			textArea.extendSelection(caret,marker.getPosition());
		else if(!textArea.isMultipleSelectionEnabled())
			textArea.selectNone();
		textArea.moveCaretPosition(marker.getPosition());
	} //}}}

	//{{{ goToPrevMarker() method
	/**
	 * Moves the caret to the previous marker.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevMarker(boolean select)
	{
		java.util.List<Marker> markers = buffer.getMarkers();
		if(markers.isEmpty())
		{
			getToolkit().beep();
			return;
		}

		int caret = textArea.getCaretPosition();

		Marker marker = null;
		for(int i = markers.size() - 1; i >= 0; i--)
		{
			Marker _marker = markers.get(i);
			if(_marker.getPosition() < caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			marker = markers.get(markers.size() - 1);

		if(select)
			textArea.extendSelection(caret,marker.getPosition());
		else if(!textArea.isMultipleSelectionEnabled())
			textArea.selectNone();
		textArea.moveCaretPosition(marker.getPosition());
	} //}}}

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
			textArea.extendSelection(textArea.getCaretPosition(),pos);
		else if(!textArea.isMultipleSelectionEnabled())
			textArea.selectNone();
		textArea.moveCaretPosition(pos);
	} //}}}

	//{{{ addMarker() method
	/**
	 * Adds a marker at the caret position.
	 * @since jEdit 3.2pre1
	 */
	public void addMarker()
	{
		int caretLine = textArea.getCaretLine();

		// always add markers on selected lines
		Selection[] selection = textArea.getSelection();
		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			int startLine = s.getStartLine();
			if(startLine != s.getEndLine() && startLine != caretLine)
			{
				buffer.addMarker('\0',s.getStart());
			}

			if(s.getEndLine() != caretLine)
				buffer.addMarker('\0',s.getEnd());
		}

		// toggle marker on caret line
		buffer.addOrRemoveMarker('\0',textArea.getCaretPosition());
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

		int caret = textArea.getCaretPosition();

		textArea.setCaretPosition(marker.getPosition());
		buffer.addMarker(shortcut,caret);
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		propertiesChanged();
		loadBufferSwitcher();
	} //}}}

	//{{{ getMinimumSize() method
	/**
	 * Returns 0,0 for split pane compatibility.
	 */
	@Override
	public final Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ getBufferSet() method
	/**
	 * Returns the current buffer set.
	 * This can be changed by setBufferSetScope().
	 * @return the buffer set which is currently used by this EditPane
	 * @since jEdit 4.3pre17
	 */
	public BufferSet getBufferSet()
	{
		return bufferSet;
	} //}}}

	//{{{ bufferAdded() method
	/**
	 * A buffer was added in the bufferSet.
	 * @param buffer the added buffer
	 * @param index the position where it was added
	 * @since jEdit 4.3pre15
	 */
	public void bufferAdded(Buffer buffer, int index)
	{
		if (buffer == null)
			return;
		if (bufferSwitcher != null)
			bufferSwitcher.updateBufferList();
		if (bufferSet.indexOf(this.buffer) == -1)
		{
			// it happens when having 1 untitled buffer if I open a file. The untitled buffer
			// is closed but the new buffer is not yet opened
			setBuffer(buffer);
		}
	} //}}}

	//{{{ bufferRemoved() method
	/**
	 * A buffer was removed from the bufferSet.
	 * @param buffer the removed buffer
	 * @param index the position where it was before being removed
	 * @since jEdit 4.3pre15
	 */
	public void bufferRemoved(Buffer buffer, int index)
	{
		if (buffer.isUntitled())
		{
			// the buffer was a new file so I do not need to keep it's informations
			caretsForPath.remove(buffer.getPath());
		}
		if (buffer == this.buffer)
		{
			// The closed buffer is the current buffer
			Buffer newBuffer = recentBuffer != null ?
				recentBuffer : bufferSet.getPreviousBuffer(index);

			if(newBuffer != null && !newBuffer.isClosed())
			{
				setBuffer(newBuffer);
				if (bufferSet.size() > 1)
				{
					recentBuffer = bufferSet.getPreviousBuffer(index -1);
				}
			}
			else if(bufferSet.size() != 0)
			{
				setBuffer(bufferSet.getBuffer(0));
				recentBuffer = null;
			}
		}
		if(buffer == recentBuffer)
			recentBuffer = null;
		if (bufferSwitcher != null)
			bufferSwitcher.updateBufferList();
	} //}}}

	//{{{ bufferMoved() method
	/**
	 * A buffer was moved in the BufferSet.
	 * @param buffer the moved buffer
	 * @param oldIndex the position it was before
	 * @param newIndex the new position
	 * @since jEdit 4.3pre15
	 */
	public void bufferMoved(Buffer buffer, int oldIndex, int newIndex)
	{
		if (bufferSwitcher != null)
			bufferSwitcher.updateBufferList();
	} //}}}

	//{{{ bufferSetSorted() method
	/**
	 * The bufferSet was sorted
	 * @since jEdit 4.3pre16
	 */
	public void bufferSetSorted()
	{
		if (bufferSwitcher != null)
			bufferSwitcher.updateBufferList();
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return getClass().getName() + '['
			+ (view.getEditPane() == this
			? "active]" : "inactive]");
	} //}}}

	//{{{ Package-private members

	//{{{ EditPane constructor
	EditPane(View view, BufferSet bufferSetSource, Buffer buffer)
	{
		super(new BorderLayout());
		BufferSet.Scope scope = jEdit.getBufferSetManager().getScope();
		BufferSet source = bufferSetSource;
		switch (scope)
		{
			case editpane:
				// do nothing
				break;
			case view:
				{
					EditPane editPane = view.getEditPane();
					if (editPane != null)
					{
						// if we have an editpane we copy it
						source = editPane.getBufferSet();
					}
				}
				break;
			case global:
				View activeView = jEdit.getActiveView();
				if (activeView != null)
				{
					EditPane editPane = activeView.getEditPane();
					if (editPane != null)
					{
						source = editPane.getBufferSet();
					}
				}
				break;
		}
		bufferSet = new BufferSet(source);

		init = true;

		this.view = view;


		textArea = new JEditTextArea(view);
		bufferSet.addBufferSetListener(this);
		textArea.getPainter().setAntiAlias(new AntiAlias(jEdit.getProperty("view.antiAlias")));
		textArea.setMouseHandler(new MouseHandler(textArea));
		textArea.setTransferHandler(new TextAreaTransferHandler());
		markerHighlight = new MarkerHighlight();
		Gutter gutter = textArea.getGutter();
		gutter.setGutterEnabled(GutterOptionPane.isGutterEnabled());
		gutter.setMinLineNumberDigitCount(GutterOptionPane.getMinLineNumberDigits());
		gutter.setSelectionAreaEnabled(GutterOptionPane.isSelectionAreaEnabled());
		gutter.addExtension(markerHighlight);
		gutter.setSelectionPopupHandler(
			new GutterPopupHandler()
			{
				public void handlePopup(int x, int y, int line)
				{
					Buffer buffer = getBuffer();
					buffer.addOrRemoveMarker('\0',
						buffer.getLineStartOffset(line));
				}
			});

		textArea.addStatusListener(new StatusHandler());
		add(BorderLayout.CENTER,textArea);

		propertiesChanged();
		setBuffer(buffer);

		// need to add the buffer to the bufferSet.
		// It may not have been done by the setBuffer() because the EditPane is not yet known by jEdit, and for
		// view and global scope it is added through this list
		if (bufferSet.indexOf(buffer) == -1)
			bufferSet.addBuffer(buffer);

		loadBufferSwitcher();

		init = false;
		EditBus.addToBus(this);
	} //}}}

	//{{{ close() method
	void close()
	{
		saveCaretInfo();
		EditBus.send(new EditPaneUpdate(this,EditPaneUpdate.DESTROYED));
		EditBus.removeFromBus(this);
		textArea.dispose();
	} //}}}


	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private boolean init;
	/** The View where the edit pane is. */
	private final View view;

	private final BufferSet bufferSet;

	/** The current buffer. */
	private Buffer buffer;
	private Buffer recentBuffer;
	private BufferSwitcher bufferSwitcher;

	/** The textArea inside the edit pane. */
	private final JEditTextArea textArea;
	private final MarkerHighlight markerHighlight;

	// A map of buffer.getPath() -> CaretInfo. This is necessary for
	// when the same buffer is open in more than one EditPane and the user
	// is switching between buffers.  We want to keep the caret in the
	// right position in each EditPane, which won't be the case if we
	// just use the buffer caret property.
	private final Map<String, CaretInfo> caretsForPath = new HashMap<String, CaretInfo>();

	//}}}

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		TextAreaPainter painter = textArea.getPainter();

		initPainter(painter);
		Gutter gutter = textArea.getGutter();
		gutter.setExpanded(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		int interval = jEdit.getIntegerProperty(
			"view.gutter.highlightInterval",5);
		gutter.setHighlightInterval(interval);
		gutter.setCurrentLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		gutter.setStructureHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.structureHighlight"));
		gutter.setStructureHighlightColor(
			jEdit.getColorProperty("view.gutter.structureHighlightColor"));
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
		markerHighlight.setMarkerHighlightEnabled(jEdit.getBooleanProperty(
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
		gutter.setGutterEnabled(GutterOptionPane.isGutterEnabled());
		gutter.setMinLineNumberDigitCount(
			GutterOptionPane.getMinLineNumberDigits());
		gutter.setSelectionAreaEnabled(
			GutterOptionPane.isSelectionAreaEnabled());
		gutter.setSelectionAreaBackground(
			GutterOptionPane.getSelectionAreaBackground());
		gutter.setSelectionAreaWidth(
				GutterOptionPane.getSelectionAreaWidth());

		int width = jEdit.getIntegerProperty(
			"view.gutter.borderWidth",3);
		gutter.setBorder(width,
			jEdit.getColorProperty("view.gutter.focusBorderColor"),
			jEdit.getColorProperty("view.gutter.noFocusBorderColor"),
			textArea.getPainter().getBackground());
		gutter.setFoldPainter(textArea.getFoldPainter());

		textArea.setCaretBlinkEnabled(jEdit.getBooleanProperty(
			"view.caretBlink"));

		textArea.setElectricScroll(jEdit.getIntegerProperty(
			"view.electricBorders",0));

		// Set up the right-click popup menu
		textArea.createPopupMenu(null);

		// use old property name for backwards compatibility
		textArea.setQuickCopyEnabled(jEdit.getBooleanProperty(
			"view.middleMousePaste"));

		textArea.setDragEnabled(jEdit.getBooleanProperty(
			"view.dragAndDrop"));

		textArea.setJoinNonWordChars(jEdit.getBooleanProperty(
			"view.joinNonWordChars"));

		textArea.setCtrlForRectangularSelection(jEdit.getBooleanProperty(
			"view.ctrlForRectangularSelection"));

		textArea.propertiesChanged();

		if (bufferSwitcher != null)
		{
			bufferSwitcher.setMaximumRowCount(jEdit.getIntegerProperty(
				"bufferSwitcher.maxRowCount",10));
		}
	} //}}}

	//{{{ initPainter() method
	/**
	 * Init the painter of a textarea.
	 *
	 * @param painter the painter of a textarea
	 * @since jEdit 4.3pre12
	 */
	public static void initPainter(TextAreaPainter painter)
	{
		painter.setFont(jEdit.getFontProperty("view.font"));
		painter.setStructureHighlightEnabled(jEdit.getBooleanProperty(
			"view.structureHighlight"));
		painter.setStructureHighlightColor(
			jEdit.getColorProperty("view.structureHighlightColor"));
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
		painter.setMultipleSelectionColor(
			jEdit.getColorProperty("view.multipleSelectionColor"));
		painter.setBackground(
			jEdit.getColorProperty("view.bgColor"));
		painter.setForeground(
			jEdit.getColorProperty("view.fgColor"));
		painter.setBlockCaretEnabled(jEdit.getBooleanProperty(
			"view.blockCaret"));
		painter.setThickCaretEnabled(jEdit.getBooleanProperty(
			"view.thickCaret"));
		painter.setLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.lineHighlight"));
		painter.setLineHighlightColor(
			jEdit.getColorProperty("view.lineHighlightColor"));
		painter.setAntiAlias(new AntiAlias(jEdit.getProperty("view.antiAlias")));
		painter.setFractionalFontMetricsEnabled(jEdit.getBooleanProperty(
			"view.fracFontMetrics"));

		painter.setSelectionFgColor(jEdit.getColorProperty(
			"view.selectionFgColor"));
		painter.setSelectionFgColorEnabled(jEdit.getBooleanProperty(
			"view.selectionFg"));

		String defaultFont = jEdit.getProperty("view.font");
		int defaultFontSize = jEdit.getIntegerProperty("view.fontsize",12);
		painter.setStyles(SyntaxUtilities.loadStyles(defaultFont,defaultFontSize));

		SyntaxStyle[] foldLineStyle = new SyntaxStyle[4];
		for(int i = 0; i <= 3; i++)
		{
			foldLineStyle[i] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.foldLine." + i),
				defaultFont,defaultFontSize);
		}
		painter.setFoldLineStyle(foldLineStyle);
	} //}}}

	//{{{ loadBufferSwitcher() method
	void loadBufferSwitcher()
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
	} //}}}

	//{{{ handleBufferUpdate() method
	@EBHandler
	public void handleBufferUpdate(BufferUpdate msg)
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
			{
				// since recentBuffer will be set to the one that
				// was closed
				recentBuffer = null;
			}
		}
		else if(msg.getWhat() == BufferUpdate.CLOSED)
		{
			if(bufferSwitcher != null)
				bufferSwitcher.updateBufferList();

			if(_buffer == buffer)
			{
				// The closed buffer is the current buffer
				Buffer newBuffer = recentBuffer != null ?
					recentBuffer : _buffer.getPrev();

				if(newBuffer != null && !newBuffer.isClosed())
				{
					setBuffer(newBuffer);
					recentBuffer = newBuffer.getPrev();
				}
			}
			else if(_buffer == recentBuffer)
				recentBuffer = null;

			Buffer closedBuffer = msg.getBuffer();
			if (closedBuffer.isUntitled())
			{
				// the buffer was a new file so I do not need to keep it's informations
				caretsForPath.remove(closedBuffer.getPath());
			}
		}
		else if(msg.getWhat() == BufferUpdate.LOAD_STARTED)
		{
			if(_buffer == buffer)
			{
				textArea.setCaretPosition(0);
				textArea.getPainter().repaint();
			}
		}
		else if(msg.getWhat() == BufferUpdate.LOADED)
		{
			if(_buffer == buffer)
			{
				textArea.repaint();
				if(bufferSwitcher != null)
					bufferSwitcher.updateBufferList();

				if(view.getEditPane() == this)
				{
					StatusBar status = view.getStatus();
					status.updateCaretStatus();
					status.updateBufferStatus();
					status.updateMiscStatus();
				}

				loadCaretInfo();
			}

		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			if(_buffer == buffer && bufferSwitcher != null)
			{
				if(buffer.isDirty())
					bufferSwitcher.repaint();
				else
					bufferSwitcher.updateBufferList();
			}
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(_buffer == buffer)
				textArea.getGutter().repaint();
		}
		else if(msg.getWhat() == BufferUpdate.PROPERTIES_CHANGED)
		{
			if(_buffer == buffer && buffer.isLoaded())
			{
				textArea.propertiesChanged();
				if(view.getEditPane() == this)
					view.getStatus().updateBufferStatus();
			}
		}
		else if(msg.getWhat() == BufferUpdate.SAVED && _buffer == buffer)
		{
			textArea.propertiesChanged();
		}
	} //}}}

	//}}}

	//{{{ StatusHandler class
	class StatusHandler implements StatusListener
	{
		public void statusChanged(org.gjt.sp.jedit.textarea.TextArea textArea, int flag, boolean value)
		{
			StatusBar status = view.getStatus();
			if(status == null)
				return;

			switch(flag)
			{
			case OVERWRITE_CHANGED:
				status.setMessageAndClear(
					jEdit.getProperty("view.status.overwrite-changed",
					new Integer[] { value ? 1 : 0 }));
				break;
			case MULTI_SELECT_CHANGED:
				status.setMessageAndClear(
					jEdit.getProperty("view.status.multi-changed",
					new Integer[] { value ? 1 : 0 }));
				break;
			case RECT_SELECT_CHANGED:
				status.setMessageAndClear(
					jEdit.getProperty("view.status.rect-select-changed",
					new Integer[] { value ? 1 : 0 }));
				break;
			}

			status.updateMiscStatus();
		}

		public void bracketSelected(org.gjt.sp.jedit.textarea.TextArea textArea, int line, String text)
		{
			StatusBar status = view.getStatus();
			if(status == null)
				return;

			status.setMessageAndClear(jEdit.getProperty(
				"view.status.bracket",new Object[] {
				line, text }));
		}

		public void narrowActive(org.gjt.sp.jedit.textarea.TextArea textArea)
		{
			StatusBar status = view.getStatus();
			if(status == null)
				return;

			status.setMessageAndClear(
				jEdit.getProperty("view.status.narrow"));
		}
	} //}}}

	//{{{ MarkerHighlight class
	class MarkerHighlight extends TextAreaExtension
	{
		private boolean markerHighlight;
		private Color markerHighlightColor;

		//{{{ getMarkerHighlightColor() method
		public Color getMarkerHighlightColor()
		{
			return markerHighlightColor;
		} //}}}

		//{{{ setMarkerHighlightColor() method
		public void setMarkerHighlightColor(Color markerHighlightColor)
		{
			this.markerHighlightColor = markerHighlightColor;
		} //}}}

		//{{{ isMarkerHighlightEnabled() method
		public boolean isMarkerHighlightEnabled()
		{
			return markerHighlight;
		} //}}}

		//{{{ isMarkerHighlightEnabled()
		public void setMarkerHighlightEnabled(boolean markerHighlight)
		{
			this.markerHighlight = markerHighlight;
		} //}}}

		//{{{ paintValidLine() method
		@Override
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(isMarkerHighlightEnabled())
			{
				Buffer buffer = (Buffer)textArea.getBuffer();
				if(buffer.getMarkerInRange(start,end) != null)
				{
					gfx.setColor(getMarkerHighlightColor());
					FontMetrics fm = textArea.getPainter().getFontMetrics();
					gfx.fillRect(0,y,textArea.getGutter()
						.getWidth(),fm.getHeight());
				}
			}
		} //}}}

		//{{{ getToolTipText() method
		@Override
		public String getToolTipText(int x, int y)
		{
			if(isMarkerHighlightEnabled())
			{
				int lineHeight = textArea.getPainter().getFontMetrics().getHeight();
				if(lineHeight == 0)
					return null;

				int line = y / lineHeight;
				int start = textArea.getScreenLineStartOffset(line);
				int end = textArea.getScreenLineEndOffset(line);
				if(start == -1 || end == -1)
					return null;

				Buffer buffer = (Buffer)textArea.getBuffer();
				Marker marker = buffer.getMarkerInRange(start,end);
				if(marker != null)
				{
					char shortcut = marker.getShortcut();
					if(shortcut == '\0')
						return jEdit.getProperty("view.gutter.marker.no-name");
					else
					{
						String[] args = { String.valueOf(shortcut) };
						return jEdit.getProperty("view.gutter.marker",args);
					}
				}
			}

			return null;
		} //}}}
	} //}}}
}
