/*
 * View.java - jEdit view
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.search.SearchBar;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * A window that edits buffers. There is no public constructor in the
 * View class. Views are created and destroyed by the <code>jEdit</code>
 * class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class View extends JFrame implements EBComponent
{
	/**
	 * Returns the dockable window manager associated with this view.
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager getDockableWindowManager()
	{
		return dockableWindowManager;
	}

	/**
	 * Returns the view's tool bar.
	 * @since jEdit 3.2.1
	 */
	public JToolBar getToolBar()
	{
		return toolBar;
	}

	/**
	 * Quick search.
	 * @since jEdit 2.7pre2
	 */
	public void quickIncrementalSearch()
	{
		if(searchBar == null)
		{
			getToolkit().beep();
			return;
		}

		String text = getTextArea().getSelectedText();
		if(text != null && text.indexOf('\n') != -1)
			text = null;

		searchBar.setHyperSearch(false);
		searchBar.getField().setText(text);
		searchBar.getField().selectAll();
		searchBar.getField().requestFocus();
	}

	/**
	 * Quick HyperSearch.
	 * @since jEdit 2.7pre2
	 */
	public void quickHyperSearch()
	{
		if(searchBar == null)
		{
			getToolkit().beep();
			return;
		}

		String text = getTextArea().getSelectedText();
		if(text != null && text.indexOf('\n') != -1)
			text = null;

		searchBar.setHyperSearch(true);
		searchBar.getField().setText(text);
		searchBar.getField().selectAll();
		searchBar.getField().requestFocus();
	}

	/**
	 * Returns the search bar.
	 * @since jEdit 2.4pre4
	 */
	public final SearchBar getSearchBar()
	{
		return searchBar;
	}

	/**
	 * Returns the listener that will handle all key events in this
	 * view, if any.
	 */
	public KeyListener getKeyEventInterceptor()
	{
		return keyEventInterceptor;
	}

	/**
	 * Sets the listener that will handle all key events in this
	 * view. For example, the complete word command uses this so
	 * that all key events are passed to the word list popup while
	 * it is visible.
	 * @param comp The component
	 */
	public void setKeyEventInterceptor(KeyListener listener)
	{
		this.keyEventInterceptor = listener;
	}

	/**
	 * Returns the input handler.
	 */
	public InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Sets the input handler.
	 * @param inputHandler The new input handler
	 */
	public void setInputHandler(InputHandler inputHandler)
	{
		this.inputHandler = inputHandler;
	}

	/**
	 * Returns the macro recorder.
	 */
	public Macros.Recorder getMacroRecorder()
	{
		return recorder;
	}

	/**
	 * Sets the macro recorder.
	 * @param recorder The macro recorder
	 */
	public void setMacroRecorder(Macros.Recorder recorder)
	{
		this.recorder = recorder;
	}

	/**
	 * Returns the status bar.
	 * @since jEdit 3.2pre2
	 */
	public StatusBar getStatus()
	{
		return status;
	}

	/**
	 * Splits the view horizontally.
	 * @since jEdit 2.7pre2
	 */
	public void splitHorizontally()
	{
		split(JSplitPane.VERTICAL_SPLIT);
	}

	/**
	 * Splits the view vertically.
	 * @since jEdit 2.7pre2
	 */
	public void splitVertically()
	{
		split(JSplitPane.HORIZONTAL_SPLIT);
	}

	/**
	 * Splits the view.
	 * @since jEdit 2.3pre2
	 */
	public void split(int orientation)
	{
		editPane.saveCaretInfo();
		EditPane oldEditPane = editPane;
		setEditPane(createEditPane(oldEditPane.getBuffer()));
		editPane.loadCaretInfo();

		JComponent oldParent = (JComponent)oldEditPane.getParent();

		if(oldParent instanceof JSplitPane)
		{
			JSplitPane oldSplitPane = (JSplitPane)oldParent;
			int dividerPos = oldSplitPane.getDividerLocation();

			Component left = oldSplitPane.getLeftComponent();
			final JSplitPane newSplitPane = new JSplitPane(orientation,
				oldEditPane,editPane);
			newSplitPane.setBorder(null);

			if(left == oldEditPane)
				oldSplitPane.setLeftComponent(newSplitPane);
			else
				oldSplitPane.setRightComponent(newSplitPane);

			oldSplitPane.setDividerLocation(dividerPos);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					newSplitPane.setDividerLocation(0.5);
					editPane.focusOnTextArea();
				}
			});
		}
		else
		{
			JSplitPane newSplitPane = splitPane = new JSplitPane(orientation,
				oldEditPane,editPane);
			newSplitPane.setBorder(null);
			oldParent.add(splitPane);
			oldParent.revalidate();

			Dimension size;
			if(oldParent instanceof JSplitPane)
				size = oldParent.getSize();
			else
				size = oldEditPane.getSize();
			newSplitPane.setDividerLocation(((orientation
				== JSplitPane.VERTICAL_SPLIT) ? size.height
				: size.width) / 2);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					editPane.focusOnTextArea();
				}
			});
		}
	}

	/**
	 * Unsplits the view.
	 * @since jEdit 2.3pre2
	 */
	public void unsplit()
	{
		if(splitPane != null)
		{
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane _editPane = editPanes[i];
				if(editPane != _editPane)
					_editPane.close();
			}

			JComponent parent = (JComponent)splitPane.getParent();

			parent.remove(splitPane);
			parent.add(editPane);
			parent.revalidate();

			splitPane = null;
			updateTitle();
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				editPane.focusOnTextArea();
			}
		});
	}

	/**
	 * Moves keyboard focus to the next text area.
	 * @since jEdit 2.7pre4
	 */
	public void nextTextArea()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPane == editPanes[i])
			{
				if(i == editPanes.length - 1)
					editPanes[0].focusOnTextArea();
				else
					editPanes[i+1].focusOnTextArea();
				break;
			}
		}
	}

	/**
	 * Moves keyboard focus to the previous text area.
	 * @since jEdit 2.7pre4
	 */
	public void prevTextArea()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPane == editPanes[i])
			{
				if(i == 0)
					editPanes[editPanes.length - 1].focusOnTextArea();
				else
					editPanes[i-1].focusOnTextArea();
				break;
			}
		}
	}

	/**
	 * Returns the top-level split pane, if any.
	 * @since jEdit 2.3pre2
	 */
	public JSplitPane getSplitPane()
	{
		return splitPane;
	}

	/**
	 * Returns the current edit pane's buffer.
	 */
	public Buffer getBuffer()
	{
		return editPane.getBuffer();
	}

	/**
	 * Sets the current edit pane's buffer.
	 */
	public void setBuffer(Buffer buffer)
	{
		editPane.setBuffer(buffer);
	}

	/**
	 * Returns the current edit pane's text area.
	 */
	public JEditTextArea getTextArea()
	{
		return editPane.getTextArea();
	}

	/**
	 * Returns the current edit pane.
	 * @since jEdit 2.5pre2
	 */
	public EditPane getEditPane()
	{
		return editPane;
	}

	/**
	 * Returns all edit panes.
	 * @since jEdit 2.5pre2
	 */
	public EditPane[] getEditPanes()
	{
		if(splitPane == null)
		{
			EditPane[] ep = { editPane };
			return ep;
		}
		else
		{
			Vector vec = new Vector();
			getEditPanes(vec,splitPane);
			EditPane[] ep = new EditPane[vec.size()];
			vec.copyInto(ep);
			return ep;
		}
	}

	/**
	 * Returns a string that can be passed to the view constructor to
	 * recreate the current split configuration in a new view.
	 * @since jEdit 3.2pre2
	 */
	public String getSplitConfig()
	{
		// this code isn't finished yet

		StringBuffer splitConfig = new StringBuffer();
		//if(splitPane != null)
		//	getSplitConfig(splitPane,splitConfig);
		//else
			splitConfig.append(getBuffer().getPath());
		return splitConfig.toString();
	}

	/**
	 * Updates the borders of all gutters in this view to reflect the
	 * currently focused text area.
	 * @since jEdit 2.6final
	 */
	public void updateGutterBorders()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].getTextArea().getGutter().updateBorder();
	}

	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		toolBars.add(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Removes a tool bar from this view.
	 * @param toolBar The tool bar
	 */
	public void removeToolBar(Component toolBar)
	{
		toolBars.remove(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Returns true if this view has been closed with
	 * <code>jEdit.closeView()</code>.
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Shows the wait cursor and glass pane.
	 */
	public synchronized void showWaitCursor()
	{
		if(waitCount++ == 0)
		{
			// still needed even though glass pane
			// has a wait cursor
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			setCursor(cursor);
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane editPane = editPanes[i];
				editPane.getTextArea().getPainter()
					.setCursor(cursor);
			}
		}
	}

	/**
	 * Hides the wait cursor and glass pane.
	 */
	public synchronized void hideWaitCursor()
	{
		if(waitCount > 0)
			waitCount--;

		if(waitCount == 0)
		{
			// still needed even though glass pane
			// has a wait cursor
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
			setCursor(cursor);
			cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane editPane = editPanes[i];
				editPane.getTextArea().getPainter()
					.setCursor(cursor);
			}
		}
	}

	/**
	 * Returns if synchronized scrolling is enabled.
	 * @since jEdit 2.7pre1
	 */
	public boolean isSynchroScrollEnabled()
	{
		return synchroScroll;
	}

	/**
	 * Toggles synchronized scrolling.
	 * @since jEdit 2.7pre2
	 */
	public void toggleSynchroScrollEnabled()
	{
		setSynchroScrollEnabled(!synchroScroll);
	}

	/**
	 * Sets synchronized scrolling.
	 * @since jEdit 2.7pre1
	 */
	public void setSynchroScrollEnabled(boolean synchroScroll)
	{
		this.synchroScroll = synchroScroll;
		JEditTextArea textArea = getTextArea();
		int firstLine = textArea.getFirstLine();
		int horizontalOffset = textArea.getHorizontalOffset();
		synchroScrollVertical(textArea,firstLine);
		synchroScrollHorizontal(textArea,horizontalOffset);
	}

	/**
	 * Sets the first line of all text areas.
	 * @param textArea The text area that is propagating this change
	 * @param firstLine The first line
	 * @since jEdit 2.7pre1
	 */
	public void synchroScrollVertical(JEditTextArea textArea, int firstLine)
	{
		if(!synchroScroll)
			return;

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPanes[i].getTextArea() != textArea)
				editPanes[i].getTextArea()._setFirstLine(firstLine);
		}
	}

	/**
	 * Sets the horizontal offset of all text areas.
	 * @param textArea The text area that is propagating this change
	 * @param horizontalOffset The horizontal offset
	 * @since jEdit 2.7pre1
	 */
	public void synchroScrollHorizontal(JEditTextArea textArea, int horizontalOffset)
	{
		if(!synchroScroll)
			return;

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPanes[i].getTextArea() != textArea)
				editPanes[i].getTextArea()._setHorizontalOffset(horizontalOffset);
		}
	}

	/**
	 * Returns the next view in the list.
	 */
	public View getNext()
	{
		return next;
	}

	/**
	 * Returns the previous view in the list.
	 */
	public View getPrev()
	{
		return prev;
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof SearchSettingsChanged)
		{
			if(searchBar != null)
				searchBar.update();
		}
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
		else if(msg instanceof EditPaneUpdate)
			handleEditPaneUpdate((EditPaneUpdate)msg);
	}

	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	public void processKeyEvent(KeyEvent evt)
	{
		if(isClosed())
			return;

		// JTextComponents don't consume events...
		if(getFocusOwner() instanceof JTextComponent)
		{
			// fix for the bug where key events in JTextComponents
			// inside views are also handled by the input handler
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_TAB:
				case KeyEvent.VK_ENTER:
					return;
				}
			}

			Keymap keymap = ((JTextComponent)getFocusOwner())
				.getKeymap();
			if(keymap.getAction(KeyStroke.getKeyStrokeForEvent(evt)) != null)
				return;
		}

		if(evt.isConsumed())
			return;

		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else if(inputHandler.isPrefixActive())
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
	View prev;
	View next;

	View(Buffer buffer, String splitConfig)
	{
		setIconImage(GUIUtilities.getEditorIcon());

		dockableWindowManager = new DockableWindowManager(this);

		Component comp = restoreSplitConfig(buffer,splitConfig);
		dockableWindowManager.add(comp);

		EditBus.addToBus(this);

		setJMenuBar(GUIUtilities.loadMenuBar("view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		propertiesChanged();

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);
		getContentPane().add(BorderLayout.SOUTH,status = new StatusBar(this));

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		dockableWindowManager.init();
	}

	void close()
	{
		closed = true;

		// save dockable window geometry, and close 'em
		dockableWindowManager.close();

		GUIUtilities.saveGeometry(this,"view");
		EditBus.removeFromBus(this);
		dispose();

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].close();

		// null some variables so that retaining references
		// to closed views won't hurt as much.
		toolBars = null;
		toolBar = null;
		searchBar = null;
		splitPane = null;
		inputHandler = null;
		recorder = null;

		setContentPane(new JPanel());
	}

	/**
	 * Updates the title bar.
	 */
	void updateTitle()
	{
		Vector buffers = new Vector();
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			Buffer buffer = editPanes[i].getBuffer();
			if(buffers.indexOf(buffer) == -1)
				buffers.addElement(buffer);
		}

		StringBuffer title = new StringBuffer(jEdit.getProperty("view.title"));
		for(int i = 0; i < buffers.size(); i++)
		{
			if(i != 0)
				title.append(", ");

			Buffer buffer = (Buffer)buffers.elementAt(i);
			title.append((showFullPath && !buffer.isNewFile())
				? buffer.getPath() : buffer.getName());
		}
		setTitle(title.toString());
	}

	// private members
	private boolean closed;

	private DockableWindowManager dockableWindowManager;

	private Box toolBars;
	private JToolBar toolBar;
	private SearchBar searchBar;

	private boolean synchroScroll;

	private EditPane editPane;
	private JSplitPane splitPane;

	private StatusBar status;

	private KeyListener keyEventInterceptor;
	private InputHandler inputHandler;
	private Macros.Recorder recorder;

	private int waitCount;

	private boolean showFullPath;

	private void getEditPanes(Vector vec, Component comp)
	{
		if(comp instanceof EditPane)
			vec.addElement(comp);
		else if(comp instanceof JSplitPane)
		{
			JSplitPane split = (JSplitPane)comp;
			getEditPanes(vec,split.getLeftComponent());
			getEditPanes(vec,split.getRightComponent());
		}
	}

	/*
	 * The split config is recorded in a simple RPN "language":
	 * "vertical" pops the two topmost elements off the stack, creates a
	 * vertical split
	 * "horizontal" pops the two topmost elements off the stack, creates a
	 * horizontal split
	 * A path name creates an edit pane editing that buffer
	 */
	private void getSplitConfig(JSplitPane splitPane,
		StringBuffer splitConfig)
	{
		Component left = splitPane.getLeftComponent();
		if(left instanceof JSplitPane)
			getSplitConfig((JSplitPane)left,splitConfig);
		else
		{
			splitConfig.append('\t');
			splitConfig.append(((EditPane)left).getBuffer().getPath());
		}

		Component right = splitPane.getRightComponent();
		if(right instanceof JSplitPane)
			getSplitConfig((JSplitPane)right,splitConfig);
		else
		{
			splitConfig.append('\t');
			splitConfig.append(((EditPane)right).getBuffer().getPath());
		}

		splitConfig.append(splitPane.getOrientation()
			== JSplitPane.VERTICAL_SPLIT ? "\tvertical" : "\thorizontal");
	}

	private Component restoreSplitConfig(Buffer buffer, String splitConfig)
	{
		if(buffer != null)
			return (editPane = createEditPane(buffer));
		else if(splitConfig == null)
			return (editPane = createEditPane(jEdit.getFirstBuffer()));

		Stack stack = new Stack();

		StringTokenizer st = new StringTokenizer(splitConfig,"\t");

		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			if(token.equals("vertical"))
			{
				stack.push(splitPane = new JSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					(Component)stack.pop(),
					(Component)stack.pop()));
				splitPane.setBorder(null);
				splitPane.setDividerLocation(0.5);
			}
			else if(token.equals("horizontal"))
			{
				stack.push(splitPane = new JSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					(Component)stack.pop(),
					(Component)stack.pop()));
				splitPane.setBorder(null);
				splitPane.setDividerLocation(0.5);
			}
			else
			{
				buffer = jEdit.getBuffer(token);
				if(buffer == null)
					buffer = jEdit.getFirstBuffer();

				stack.push(editPane = createEditPane(buffer));
			}
		}

		return (Component)stack.peek();
	}

	/**
	 * Reloads various settings from the properties.
	 */
	private void propertiesChanged()
	{
		loadToolBars();

		showFullPath = jEdit.getBooleanProperty("view.showFullPath");
		updateTitle();

		dockableWindowManager.propertiesChanged();

		SwingUtilities.updateComponentTreeUI(getRootPane());
	}

	private void loadToolBars()
	{
		if(jEdit.getBooleanProperty("view.showToolbar"))
		{
			if(toolBar != null)
				toolBars.remove(toolBar);

			toolBar = GUIUtilities.loadToolBar("view.toolbar");
			toolBar.add(Box.createGlue());

			toolBars.add(toolBar,0);
			getRootPane().revalidate();
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}

		if(jEdit.getBooleanProperty("view.showSearchbar"))
		{
			if(searchBar == null)
			{
				searchBar = new SearchBar(this);
				addToolBar(searchBar);
			}
		}
		else if(searchBar != null)
		{
			removeToolBar(searchBar);
			searchBar = null;
		}
	}

	private EditPane createEditPane(Buffer buffer)
	{
		EditPane editPane = new EditPane(this,buffer);
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addFocusListener(new FocusHandler());
		textArea.addCaretListener(new CaretHandler());
		textArea.addScrollListener(new ScrollHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
		return editPane;
	}

	private void setEditPane(EditPane editPane)
	{
		this.editPane = editPane;
		status.repaintCaretStatus();
		status.updateBufferStatus();
		status.updateMiscStatus();
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			if(!buffer.isDirty())
			{
				// have to update title after each save
				// in case it was a 'save as'
				EditPane[] editPanes = getEditPanes();
				for(int i = 0; i < editPanes.length; i++)
				{
					if(editPanes[i].getBuffer() == buffer)
					{
						updateTitle();
						break;
					}
				}
			}
		}
	}

	private void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		if(msg.getEditPane().getView() == this
			&& msg.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
		{
			status.repaintCaretStatus();
			status.updateBufferStatus();
			status.updateMiscStatus();
			status.updateFoldStatus();
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			status.repaintCaretStatus();
			status.updateMiscStatus();
		}
	}

	class FocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt)
		{
			// walk up hierarchy, looking for an EditPane
			Component comp = (Component)evt.getSource();
			while(!(comp instanceof EditPane))
			{
				if(comp == null)
					return;

				comp = comp.getParent();
			}

			setEditPane((EditPane)comp);
		}
	}

	class ScrollHandler implements ScrollListener
	{
		public void scrolledVertically(JEditTextArea textArea)
		{
			if(getTextArea() == textArea)
				status.repaintCaretStatus();
		}

		public void scrolledHorizontally(JEditTextArea textArea) {}
	}

	class WindowHandler extends WindowAdapter
	{
		boolean gotFocus;

		public void windowActivated(WindowEvent evt)
		{
			if(!gotFocus)
			{
				editPane.focusOnTextArea();
				gotFocus = true;
			}

			final Vector buffers = new Vector();
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				Buffer buffer = ((EditPane)editPanes[i])
					.getBuffer();
				if(buffers.contains(buffer))
					continue;
				else
					buffers.addElement(buffer);
			}

			// People have reported hangs with JDK 1.4; might be
			// caused by modal dialogs being displayed from
			// windowActivated()
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					for(int i = 0; i < buffers.size(); i++)
					{
						((Buffer)buffers.elementAt(i))
							.checkModTime(View.this);
					}
				}
			});
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	}
}
