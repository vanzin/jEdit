/*
 * View.java - jEdit view
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.textarea.*;
//}}}

/**
 * A <code>View</code> is jEdit's top-level frame window.<p>
 *
 * In a BeanShell script, you can obtain the current view instance from the
 * <code>view</code> variable.<p>
 *
 * The largest component it contains is an {@link EditPane} that in turn
 * contains a {@link org.gjt.sp.jedit.textarea.JEditTextArea} that displays a
 * {@link Buffer}.
 * A view can have more than one edit pane in a split window configuration.
 * A view also contains a menu bar, an optional toolbar and other window
 * decorations, as well as docked windows.<p>
 *
 * The <b>View</b> class performs two important operations
 * dealing with plugins: creating plugin menu items, and managing dockable
 * windows.
 *
 * <ul>
 * <li>When a view is being created, its initialization routine
 * iterates through the collection of loaded plugins and calls
 * the {@link EditPlugin#createMenuItems(Vector)} method of
 * each plugin core class.</li>
 * <li>The view also creates and initializes a
 * {@link org.gjt.sp.jedit.gui.DockableWindowManager}
 * object.  This object is
 * responsible for creating, closing and managing dockable windows.</li>
 * </ul>
 *
 * This class does not have a public constructor.
 * Views can be opened and closed using methods in the <code>jEdit</code>
 * class.
 *
 * @see org.gjt.sp.jedit.jEdit#newView(View)
 * @see org.gjt.sp.jedit.jEdit#newView(View,Buffer)
 * @see org.gjt.sp.jedit.jEdit#newView(View,Buffer,boolean)
 * @see org.gjt.sp.jedit.jEdit#closeView(View)
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class View extends JFrame implements EBComponent
{
	//{{{ User interface

	//{{{ ToolBar-related constants

	//{{{ Groups
	/**
	 * The group of tool bars above the DockableWindowManager
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int TOP_GROUP = 0;

	/**
	 * The group of tool bars below the DockableWindowManager
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int BOTTOM_GROUP = 1;
	public static final int DEFAULT_GROUP = TOP_GROUP;
	//}}}

	//{{{ Layers

	// Common layers
	/**
	 * The highest possible layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int TOP_LAYER = Integer.MAX_VALUE;

	/**
	 * The default layer for tool bars with no preference.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int DEFAULT_LAYER = 0;

	/**
	 * The lowest possible layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int BOTTOM_LAYER = Integer.MIN_VALUE;

	// Layers for top group
	/**
	 * Above system tool bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int ABOVE_SYSTEM_BAR_LAYER = 150;

	/**
	 * System tool bar layer.
	 * jEdit uses this for the main tool bar.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int SYSTEM_BAR_LAYER = 100;

	/**
	 * Below system tool bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int BELOW_SYSTEM_BAR_LAYER = 75;

	/**
	 * Search bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int SEARCH_BAR_LAYER = 75;

	/**
	 * Below search bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.0pre7
	 */
	public static final int BELOW_SEARCH_BAR_LAYER = 50;

	// Layers for bottom group
	/**
	 * @deprecated Status bar no longer added as a tool bar.
	 */
	public static final int ABOVE_ACTION_BAR_LAYER = -50;

	/**
	 * Action bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.2pre1
	 */
	public static final int ACTION_BAR_LAYER = -75;

	/**
	 * Status bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.2pre1
	 */
	public static final int STATUS_BAR_LAYER = -100;

	/**
	 * Status bar layer.
	 * @see #addToolBar(int,int,java.awt.Component)
	 * @since jEdit 4.2pre1
	 */
	public static final int BELOW_STATUS_BAR_LAYER = -150;
	//}}}

	//}}}

	//{{{ getDockableWindowManager() method
	/**
	 * Returns the dockable window manager associated with this view.
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager getDockableWindowManager()
	{
		return dockableWindowManager;
	} //}}}

	//{{{ getToolBar() method
	/**
	 * Returns the view's tool bar.
	 * @since jEdit 3.2.1
	 */
	public JToolBar getToolBar()
	{
		return toolBar;
	} //}}}

	//{{{ addToolBar() method
	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		addToolBar(DEFAULT_GROUP, DEFAULT_LAYER, toolBar);
	} //}}}

	//{{{ addToolBar() method
	/**
	 * Adds a tool bar to this view.
	 * @param group The tool bar group to add to
	 * @param toolBar The tool bar
	 * @see org.gjt.sp.jedit.gui.ToolBarManager
	 * @since jEdit 4.0pre7
	 */
	public void addToolBar(int group, Component toolBar)
	{
		addToolBar(group, DEFAULT_LAYER, toolBar);
	} //}}}

	//{{{ addToolBar() method
	/**
	 * Adds a tool bar to this view.
	 * @param group The tool bar group to add to
	 * @param layer The layer of the group to add to
	 * @param toolBar The tool bar
	 * @see org.gjt.sp.jedit.gui.ToolBarManager
	 * @since jEdit 4.0pre7
	 */
	public void addToolBar(int group, int layer, Component toolBar)
	{
		toolBarManager.addToolBar(group, layer, toolBar);
		getRootPane().revalidate();
	} //}}}

	//{{{ removeToolBar() method
	/**
	 * Removes a tool bar from this view.
	 * @param toolBar The tool bar
	 */
	public void removeToolBar(Component toolBar)
	{
		toolBarManager.removeToolBar(toolBar);
		getRootPane().revalidate();
	} //}}}

	//{{{ showWaitCursor() method
	/**
	 * Shows the wait cursor. This method and
	 * {@link #hideWaitCursor()} are implemented using a reference
	 * count of requests for wait cursors, so that nested calls work
	 * correctly; however, you should be careful to use these methods in
	 * tandem.<p>
	 *
	 * To ensure that {@link #hideWaitCursor()} is always called
	 * after a {@link #showWaitCursor()}, use a
	 * <code>try</code>/<code>finally</code> block, like this:
	 * <pre>try
	 *{
	 *    view.showWaitCursor();
	 *    // ...
	 *}
	 *finally
	 *{
	 *    view.hideWaitCursor();
	 *}</pre>
	 */
	public synchronized void showWaitCursor()
	{
		if(waitCount++ == 0)
		{
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
	} //}}}

	//{{{ hideWaitCursor() method
	/**
	 * Hides the wait cursor.
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
	} //}}}

	//{{{ getSearchBar() method
	/**
	 * Returns the search bar.
	 * @since jEdit 2.4pre4
	 */
	public final SearchBar getSearchBar()
	{
		return searchBar;
	} //}}}

	//{{{ getStatus() method
	/**
	 * Returns the status bar. The
	 * {@link org.gjt.sp.jedit.gui.StatusBar#setMessage(String)} and
	 * {@link org.gjt.sp.jedit.gui.StatusBar#setMessageAndClear(String)} methods can
	 * be called on the return value of this method to display status
	 * information to the user.
	 * @since jEdit 3.2pre2
	 */
	public StatusBar getStatus()
	{
		return status;
	} //}}}

	//}}}

	//{{{ Input handling

	//{{{ getKeyEventInterceptor() method
	/**
	 * Returns the listener that will handle all key events in this
	 * view, if any.
	 */
	public KeyListener getKeyEventInterceptor()
	{
		return keyEventInterceptor;
	} //}}}

	//{{{ setKeyEventInterceptor() method
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
	} //}}}

	//{{{ getInputHandler() method
	/**
	 * Returns the input handler.
	 */
	public InputHandler getInputHandler()
	{
		return inputHandler;
	} //}}}

	//{{{ setInputHandler() method
	/**
	 * Sets the input handler.
	 * @param inputHandler The new input handler
	 */
	public void setInputHandler(InputHandler inputHandler)
	{
		this.inputHandler = inputHandler;
	} //}}}

	//{{{ getMacroRecorder() method
	/**
	 * Returns the macro recorder.
	 */
	public Macros.Recorder getMacroRecorder()
	{
		return recorder;
	} //}}}

	//{{{ setMacroRecorder() method
	/**
	 * Sets the macro recorder.
	 * @param recorder The macro recorder
	 */
	public void setMacroRecorder(Macros.Recorder recorder)
	{
		this.recorder = recorder;
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	public void processKeyEvent(KeyEvent evt)
	{
		if(isClosed())
			return;

		if(getFocusOwner() instanceof JComponent)
		{
			JComponent comp = (JComponent)getFocusOwner();
			InputMap map = comp.getInputMap();
			ActionMap am = comp.getActionMap();

			if(map != null && am != null && comp.isEnabled())
			{
				Object binding = map.get(KeyStroke.getKeyStrokeForEvent(evt));
				if(binding != null && am.get(binding) != null)
				{
					return;
				}
			}
		}

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
		}

		if(evt.isConsumed())
			return;

		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			// Handled in text area
			if(keyEventInterceptor != null)
				/* keyEventInterceptor.keyTyped(evt) */;
			else if(inputHandler.isPrefixActive()
				&& !getTextArea().hasFocus())
				inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyPressed(evt);
			else
			{
				inputHandler.keyPressed(evt);
				// this is a weird hack.
				// we don't want C+e a to insert 'a' in the
				// search bar if the search bar has focus...
				if(inputHandler.isPrefixActive()
					&& getFocusOwner() instanceof JTextComponent)
					getTextArea().requestFocus();
			}
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

	//{{{ Buffers, edit panes, split panes

	//{{{ splitHorizontally() method
	/**
	 * Splits the view horizontally.
	 * @since jEdit 4.1pre2
	 */
	public EditPane splitHorizontally()
	{
		return split(JSplitPane.VERTICAL_SPLIT);
	} //}}}

	//{{{ splitVertically() method
	/**
	 * Splits the view vertically.
	 * @since jEdit 4.1pre2
	 */
	public EditPane splitVertically()
	{
		return split(JSplitPane.HORIZONTAL_SPLIT);
	} //}}}

	//{{{ split() method
	/**
	 * Splits the view.
	 * @since jEdit 4.1pre2
	 */
	public EditPane split(int orientation)
	{
		editPane.saveCaretInfo();
		EditPane oldEditPane = editPane;
		setEditPane(createEditPane(oldEditPane.getBuffer()));
		editPane.loadCaretInfo();

		JComponent oldParent = (JComponent)oldEditPane.getParent();

		final JSplitPane newSplitPane = new JSplitPane(orientation);
		newSplitPane.setOneTouchExpandable(true);
		newSplitPane.setBorder(null);
		newSplitPane.setMinimumSize(new Dimension(0,0));

		int parentSize = (orientation == JSplitPane.VERTICAL_SPLIT
			? oldEditPane.getHeight() : oldEditPane.getWidth());
		final int dividerPosition = (int)((double)(parentSize
			- newSplitPane.getDividerSize()) * 0.5);
		newSplitPane.setDividerLocation(dividerPosition);

		if(oldParent instanceof JSplitPane)
		{
			JSplitPane oldSplitPane = (JSplitPane)oldParent;
			int dividerPos = oldSplitPane.getDividerLocation();

			Component left = oldSplitPane.getLeftComponent();

			if(left == oldEditPane)
				oldSplitPane.setLeftComponent(newSplitPane);
			else
				oldSplitPane.setRightComponent(newSplitPane);

			newSplitPane.setLeftComponent(oldEditPane);
			newSplitPane.setRightComponent(editPane);

			oldSplitPane.setDividerLocation(dividerPos);
		}
		else
		{
			this.splitPane = newSplitPane;

			newSplitPane.setLeftComponent(oldEditPane);
			newSplitPane.setRightComponent(editPane);

			oldParent.add(newSplitPane);
			oldParent.revalidate();
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				newSplitPane.setDividerLocation(dividerPosition);
			}
		});

		editPane.focusOnTextArea();

		return editPane;
	} //}}}

	//{{{ unsplit() method
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

			editPane.focusOnTextArea();
		}
		else
			getToolkit().beep();
	} //}}}

	//{{{ unsplitCurrent() method
	/**
	 * Removes the current split.
	 * @since jEdit 2.3pre2
	 */
	public void unsplitCurrent()
	{
		if(splitPane != null)
		{
			// find first split pane parenting current edit pane
			Component comp = editPane;
			while(!(comp instanceof JSplitPane))
			{
				comp = comp.getParent();
			}

			// get rid of any edit pane that is a child
			// of the current edit pane's parent splitter
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane _editPane = editPanes[i];
				if(GUIUtilities.isAncestorOf(comp,_editPane)
					&& _editPane != editPane)
					_editPane.close();
			}

			JComponent parent = (JComponent)comp.getParent();

			if(parent instanceof JSplitPane)
			{
				JSplitPane parentSplit = (JSplitPane)parent;
				int pos = parentSplit.getDividerLocation();
				if(parentSplit.getLeftComponent() == comp)
					parentSplit.setLeftComponent(editPane);
				else
					parentSplit.setRightComponent(editPane);
				parentSplit.setDividerLocation(pos);
			}
			else
			{
				parent.remove(comp);
				parent.add(editPane);
				splitPane = null;
			}

			parent.revalidate();

			updateTitle();

			editPane.focusOnTextArea();
		}
		else
			getToolkit().beep();
	} //}}}

	//{{{ nextTextArea() method
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
	} //}}}

	//{{{ prevTextArea() method
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
	} //}}}

	//{{{ getSplitPane() method
	/**
	 * Returns the top-level split pane, if any.
	 * @since jEdit 2.3pre2
	 */
	public JSplitPane getSplitPane()
	{
		return splitPane;
	} //}}}

	//{{{ getBuffer() method
	/**
	 * Returns the current edit pane's buffer.
	 */
	public Buffer getBuffer()
	{
		if(editPane == null)
			return null;
		else
			return editPane.getBuffer();
	} //}}}

	//{{{ setBuffer() method
	/**
	 * Sets the current edit pane's buffer.
	 */
	public void setBuffer(Buffer buffer)
	{
		editPane.setBuffer(buffer);
	} //}}}

	//{{{ goToBuffer() method
	/**
	 * If this buffer is open in one of the view's edit panes, sets focus
	 * to that edit pane. Otherwise, opens the buffer in the currently
	 * active edit pane.
	 * @param buffer The buffer
	 * @since jEdit 4.2pre1
	 */
	public EditPane goToBuffer(Buffer buffer)
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			EditPane ep = editPanes[i];
			if(ep.getBuffer() == buffer)
			{
				ep.focusOnTextArea();
				return ep;
			}
		}
		setBuffer(buffer);
		return editPane;
	} //}}}

	//{{{ getTextArea() method
	/**
	 * Returns the current edit pane's text area.
	 */
	public JEditTextArea getTextArea()
	{
		if(editPane == null)
			return null;
		else
			return editPane.getTextArea();
	} //}}}

	//{{{ getEditPane() method
	/**
	 * Returns the current edit pane.
	 * @since jEdit 2.5pre2
	 */
	public EditPane getEditPane()
	{
		return editPane;
	} //}}}

	//{{{ getEditPanes() method
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
	} //}}}

	//{{{ getViewConfig() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public ViewConfig getViewConfig()
	{
		StringBuffer splitConfig = new StringBuffer();
		if(splitPane != null)
			getSplitConfig(splitPane,splitConfig);
		else
		{
			splitConfig.append(getBuffer().getIndex());
			splitConfig.append(" buffer");
		}

		ViewConfig config = new ViewConfig();
		config.plainView = isPlainView();
		config.splitConfig = splitConfig.toString();
		config.x = getX();
		config.y = getY();
		config.width = getWidth();
		config.height = getHeight();
		config.extState = GUIUtilities.getExtendedState(this);

		config.top = dockableWindowManager.getTopDockingArea().getCurrent();
		config.left = dockableWindowManager.getLeftDockingArea().getCurrent();
		config.bottom = dockableWindowManager.getBottomDockingArea().getCurrent();
		config.right = dockableWindowManager.getRightDockingArea().getCurrent();

		config.topPos = dockableWindowManager.getTopDockingArea().getDimension();
		config.leftPos = dockableWindowManager.getLeftDockingArea().getDimension();
		config.bottomPos = dockableWindowManager.getBottomDockingArea().getDimension();
		config.rightPos = dockableWindowManager.getRightDockingArea().getDimension();

		return config;
	} //}}}

	//}}}

	//{{{ Synchronized scrolling

	//{{{ isSynchroScrollEnabled() method
	/**
	 * Returns if synchronized scrolling is enabled.
	 * @since jEdit 2.7pre1
	 */
	public boolean isSynchroScrollEnabled()
	{
		return synchroScroll;
	} //}}}

	//{{{ toggleSynchroScrollEnabled() method
	/**
	 * Toggles synchronized scrolling.
	 * @since jEdit 2.7pre2
	 */
	public void toggleSynchroScrollEnabled()
	{
		setSynchroScrollEnabled(!synchroScroll);
	} //}}}

	//{{{ setSynchroScrollEnabled() method
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
	} //}}}

	//{{{ synchroScrollVertical() method
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
				editPanes[i].getTextArea().setFirstLine(firstLine);
		}
	} //}}}

	//{{{ synchroScrollHorizontal() method
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
				editPanes[i].getTextArea().setHorizontalOffset(horizontalOffset);
		}
	} //}}}

	//}}}

	//{{{ quickIncrementalSearch() method
	/**
	 * Quick search.
	 * @since jEdit 4.0pre3
	 */
	public void quickIncrementalSearch(boolean word)
	{
		if(searchBar == null)
			searchBar = new SearchBar(this,true);
		if(searchBar.getParent() == null)
			addToolBar(TOP_GROUP,SEARCH_BAR_LAYER,searchBar);

		JEditTextArea textArea = getTextArea();

		String text = textArea.getSelectedText();
		if(text == null && word)
		{
			textArea.selectWord();
			text = textArea.getSelectedText();
		}
		else if(text != null && text.indexOf('\n') != -1)
			text = null;

		searchBar.setHyperSearch(false);
		searchBar.getField().setText(text);
		searchBar.getField().requestFocus();
		searchBar.getField().selectAll();
	} //}}}

	//{{{ quickHyperSearch() method
	/**
	 * Quick HyperSearch.
	 * @since jEdit 4.0pre3
	 */
	public void quickHyperSearch(boolean word)
	{
		JEditTextArea textArea = getTextArea();

		String text = textArea.getSelectedText();
		if(text == null && word)
		{
			textArea.selectWord();
			text = textArea.getSelectedText();
		}

		if(text != null && text.indexOf('\n') == -1)
		{
			HistoryModel.getModel("find").addItem(text);
			SearchAndReplace.setSearchString(text);
			SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
			SearchAndReplace.hyperSearch(this);
		}
		else
		{
			if(searchBar == null)
				searchBar = new SearchBar(this,true);
			if(searchBar.getParent() == null)
				addToolBar(TOP_GROUP,SEARCH_BAR_LAYER,searchBar);

			searchBar.setHyperSearch(true);
			searchBar.getField().setText(null);
			searchBar.getField().requestFocus();
			searchBar.getField().selectAll();
		}
	} //}}}

	//{{{ actionBar() method
	/**
	 * Shows the action bar if needed, and sends keyboard focus there.
	 * @since jEdit 4.2pre1
	 */
	public void actionBar()
	{
		if(actionBar == null)
			actionBar = new ActionBar(this,true);
		if(actionBar.getParent() == null)
			addToolBar(BOTTOM_GROUP,ACTION_BAR_LAYER,actionBar);

		actionBar.goToActionBar();
	} //}}}

	//{{{ isClosed() method
	/**
	 * Returns true if this view has been closed with
	 * {@link jEdit#closeView(View)}.
	 */
	public boolean isClosed()
	{
		return closed;
	} //}}}

	//{{{ isPlainView() method
	/**
	 * Returns true if this is an auxilliary view with no dockable windows.
	 * @since jEdit 4.1pre2
	 */
	public boolean isPlainView()
	{
		return plainView;
	} //}}}

	//{{{ getNext() method
	/**
	 * Returns the next view in the list.
	 */
	public View getNext()
	{
		return next;
	} //}}}

	//{{{ getPrev() method
	/**
	 * Returns the previous view in the list.
	 */
	public View getPrev()
	{
		return prev;
	} //}}}

	//{{{ handleMessage() method
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
	} //}}}

	//{{{ getMinimumSize() method
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ Package-private members
	View prev;
	View next;

	//{{{ View constructor
	View(Buffer buffer, ViewConfig config)
	{
		this.plainView = config.plainView;

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		setIconImage(GUIUtilities.getEditorIcon());

		dockableWindowManager = new DockableWindowManager(this,config);

		topToolBars = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,
			1));
		bottomToolBars = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,
			1));

		toolBarManager = new ToolBarManager(topToolBars, bottomToolBars);

		status = new StatusBar(this);

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		Component comp = restoreSplitConfig(buffer,config.splitConfig);
		dockableWindowManager.add(comp);

		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);

		dockableWindowManager.init();

		// tool bar and status bar gets added in propertiesChanged()
		// depending in the 'tool bar alternate layout' setting.
		propertiesChanged();

		if(!plainView)
		{
			if(config.top != null && config.top.length() != 0)
				dockableWindowManager.showDockableWindow(config.top);

			if(config.left != null && config.left.length() != 0)
				dockableWindowManager.showDockableWindow(config.left);

			if(config.bottom != null && config.bottom.length() != 0)
				dockableWindowManager.showDockableWindow(config.bottom);

			if(config.right != null && config.right.length() != 0)
				dockableWindowManager.showDockableWindow(config.right);
		}

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		pack();

		EditBus.addToBus(this);
	} //}}}

	//{{{ close() method
	void close()
	{
		GUIUtilities.saveGeometry(this,plainView ? "plain-view" : "view");
		closed = true;

		// save dockable window geometry, and close 'em
		dockableWindowManager.close();

		EditBus.removeFromBus(this);
		dispose();

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].close();

		// null some variables so that retaining references
		// to closed views won't hurt as much.
		toolBarManager = null;
		toolBar = null;
		searchBar = null;
		splitPane = null;
		inputHandler = null;
		recorder = null;

		getContentPane().removeAll();
	} //}}}

	//{{{ updateTitle() method
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
			if(buffer.isDirty())
				title.append(jEdit.getProperty("view.title.dirty"));
		}
		setTitle(title.toString());
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private boolean closed;

	private DockableWindowManager dockableWindowManager;

	private JPanel topToolBars;
	private JPanel bottomToolBars;
	private ToolBarManager toolBarManager;

	private JToolBar toolBar;
	private SearchBar searchBar;
	private ActionBar actionBar;

	private boolean synchroScroll;

	private EditPane editPane;
	private JSplitPane splitPane;

	private StatusBar status;

	private KeyListener keyEventInterceptor;
	private InputHandler inputHandler;
	private Macros.Recorder recorder;

	private int waitCount;

	private boolean showFullPath;

	private boolean plainView;
	//}}}

	//{{{ getEditPanes() method
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
	} //}}}

	//{{{ getSplitConfig() method
	/*
	 * The split config is recorded in a simple RPN "language".
	 */
	private void getSplitConfig(JSplitPane splitPane,
		StringBuffer splitConfig)
	{
		Component right = splitPane.getRightComponent();
		if(right instanceof JSplitPane)
			getSplitConfig((JSplitPane)right,splitConfig);
		else
		{
			splitConfig.append(((EditPane)right).getBuffer().getIndex());
			splitConfig.append(" buffer");
		}

		splitConfig.append(' ');

		Component left = splitPane.getLeftComponent();
		if(left instanceof JSplitPane)
			getSplitConfig((JSplitPane)left,splitConfig);
		else
		{
			splitConfig.append(((EditPane)left).getBuffer().getIndex());
			splitConfig.append(" buffer");
		}

		splitConfig.append(' ');
		splitConfig.append(splitPane.getDividerLocation());
		splitConfig.append(' ');
		splitConfig.append(splitPane.getOrientation()
			== JSplitPane.VERTICAL_SPLIT ? "vertical" : "horizontal");
	} //}}}

	//{{{ restoreSplitConfig() method
	private Component restoreSplitConfig(Buffer buffer, String splitConfig)
	{
		if(buffer != null)
			return (editPane = createEditPane(buffer));
		else if(splitConfig == null)
			return (editPane = createEditPane(jEdit.getFirstBuffer()));

		Buffer[] buffers = jEdit.getBuffers();

		Stack stack = new Stack();

		StringTokenizer st = new StringTokenizer(splitConfig);

		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			if(token.equals("vertical") || token.equals("horizontal"))
			{
				int orientation = (token.equals("vertical")
					? JSplitPane.VERTICAL_SPLIT
					: JSplitPane.HORIZONTAL_SPLIT);
				int divider = Integer.parseInt((String)stack.pop());
				stack.push(splitPane = new JSplitPane(
					orientation,
					(Component)stack.pop(),
					(Component)stack.pop()));
				splitPane.setOneTouchExpandable(true);
				splitPane.setBorder(null);
				splitPane.setMinimumSize(new Dimension(0,0));
				splitPane.setDividerLocation(divider);
			}
			else if(token.equals("buffer"))
			{
				int index = Integer.parseInt((String)stack.pop());
				if(index < buffers.length && index > 0)
					buffer = buffers[index];
				if(buffer == null)
					buffer = jEdit.getFirstBuffer();

				stack.push(editPane = createEditPane(buffer));
			}
			else
			{
				stack.push(token);
			}
		}

		return (Component)stack.peek();
	} //}}}

	//{{{ propertiesChanged() method
	/**
	 * Reloads various settings from the properties.
	 */
	private void propertiesChanged()
	{
		setJMenuBar(GUIUtilities.loadMenuBar("view.mbar"));

		loadToolBars();

		showFullPath = jEdit.getBooleanProperty("view.showFullPath");
		updateTitle();

		dockableWindowManager.propertiesChanged();
		status.propertiesChanged();

		removeToolBar(status);
		getContentPane().remove(status);

		if(jEdit.getBooleanProperty("view.toolbar.alternateLayout"))
		{
			getContentPane().add(BorderLayout.NORTH,topToolBars);
			getContentPane().add(BorderLayout.SOUTH,bottomToolBars);
			if(!plainView && jEdit.getBooleanProperty("view.status.visible"))
				addToolBar(BOTTOM_GROUP,STATUS_BAR_LAYER,status);
		}
		else
		{
			dockableWindowManager.add(DockableWindowManager.DockableLayout
				.TOP_TOOLBARS,topToolBars);
			dockableWindowManager.add(DockableWindowManager.DockableLayout
				.BOTTOM_TOOLBARS,bottomToolBars);
			if(!plainView && jEdit.getBooleanProperty("view.status.visible"))
				getContentPane().add(BorderLayout.SOUTH,status);
		}

		getRootPane().revalidate();

		//SwingUtilities.updateComponentTreeUI(getRootPane());
	} //}}}

	//{{{ loadToolBars() method
	private void loadToolBars()
	{
		if(jEdit.getBooleanProperty("view.showToolbar") && !plainView)
		{
			if(toolBar != null)
				toolBarManager.removeToolBar(toolBar);

			toolBar = GUIUtilities.loadToolBar("view.toolbar");

			addToolBar(TOP_GROUP, SYSTEM_BAR_LAYER, toolBar);
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}

		if(jEdit.getBooleanProperty("view.showSearchbar") && !plainView)
		{
			if(searchBar != null)
				removeToolBar(searchBar);

			addToolBar(TOP_GROUP,SEARCH_BAR_LAYER,
				searchBar = new SearchBar(this,false));
		}
		else if(searchBar != null)
		{
			removeToolBar(searchBar);
			searchBar = null;
		}
	} //}}}

	//{{{ createEditPane() method
	private EditPane createEditPane(Buffer buffer)
	{
		EditPane editPane = new EditPane(this,buffer);
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addFocusListener(new FocusHandler());
		textArea.addCaretListener(new CaretHandler());
		textArea.addScrollListener(new ScrollHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
		return editPane;
	} //}}}

	//{{{ setEditPane() method
	private void setEditPane(EditPane editPane)
	{
		this.editPane = editPane;
		status.updateCaretStatus();
		status.updateBufferStatus();
		status.updateMiscStatus();

		// repaint the gutter so that the border color
		// reflects the focus state
		updateGutterBorders();

		EditBus.send(new ViewUpdate(this,ViewUpdate.EDIT_PANE_CHANGED));
	} //}}}

	//{{{ handleBufferUpdate() method
	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
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
	} //}}}

	//{{{ handleEditPaneUpdate() method
	private void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		EditPane editPane = msg.getEditPane();
		if(editPane.getView() == this
			&& msg.getWhat() == EditPaneUpdate.BUFFER_CHANGED
			&& editPane.getBuffer().isLoaded())
		{
			status.updateCaretStatus();
			status.updateBufferStatus();
			status.updateMiscStatus();
		}
	} //}}}

	//{{{ updateGutterBorders() method
	/**
	 * Updates the borders of all gutters in this view to reflect the
	 * currently focused text area.
	 * @since jEdit 2.6final
	 */
	private void updateGutterBorders()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].getTextArea().getGutter().updateBorder();
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ CaretHandler class
	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			if(evt.getSource() == getTextArea())
				status.updateCaretStatus();
		}
	} //}}}

	//{{{ FocusHandler class
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

			if(comp != editPane)
				setEditPane((EditPane)comp);
			else
				updateGutterBorders();
		}
	} //}}}

	//{{{ ScrollHandler class
	class ScrollHandler implements ScrollListener
	{
		public void scrolledVertically(JEditTextArea textArea)
		{
			if(getTextArea() == textArea)
				status.updateCaretStatus();
		}

		public void scrolledHorizontally(JEditTextArea textArea) {}
	} //}}}

	//{{{ WindowHandler class
	class WindowHandler extends WindowAdapter
	{
		public void windowActivated(WindowEvent evt)
		{
			jEdit.setActiveView(View.this);

			// People have reported hangs with JDK 1.4; might be
			// caused by modal dialogs being displayed from
			// windowActivated()
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					jEdit.checkBufferStatus(View.this);
				}
			});
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	} //}}}

	//{{{ ViewConfig class
	public static class ViewConfig
	{
		public boolean plainView;
		public String splitConfig;
		public int x, y, width, height, extState;

		// dockables
		public String top, left, bottom, right;
		public int topPos, leftPos, bottomPos, rightPos;

		public ViewConfig()
		{
		}

		public ViewConfig(boolean plainView)
		{
			String prefix = (plainView ? "plain-view" : "view");
			x = jEdit.getIntegerProperty(prefix + ".x",0);
			y = jEdit.getIntegerProperty(prefix + ".y",0);
			width = jEdit.getIntegerProperty(prefix + ".width",0);
			height = jEdit.getIntegerProperty(prefix + ".height",0);
		}

		public ViewConfig(boolean plainView, String splitConfig,
			int x, int y, int width, int height, int extState)
		{
			this.plainView = plainView;
			this.splitConfig = splitConfig;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.extState = extState;
		}
	} //}}}

	//}}}
}
