/*
 * View.java - jEdit view
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2004 Slava Pestov
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
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.MenuSelectionManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.bufferset.BufferSetManager;
import org.gjt.sp.jedit.gui.ActionBar;
import org.gjt.sp.jedit.gui.CloseDialog;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.gui.DockableWindowFactory;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.gui.DockingFrameworkProvider;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.StatusBar;
import org.gjt.sp.jedit.gui.ToolBarManager;
import org.gjt.sp.jedit.gui.VariableGridLayout;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;
import org.gjt.sp.jedit.input.InputHandlerProvider;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.SearchSettingsChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.options.GeneralOptionPane;
import org.gjt.sp.jedit.search.CurrentBufferSet;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.search.SearchBar;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.ScrollListener;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.visitors.JEditVisitor;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
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
 * iterates through the collection of loaded plugins and constructs the
 * <b>Plugins</b> menu using the properties as specified in the
 * {@link EditPlugin} class.</li>
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
public class View extends JFrame implements InputHandlerProvider
{
	//{{{ User interface

	//{{{ ToolBar-related constants

	public static final String VIEW_DOCKING_FRAMEWORK_PROPERTY = "view.docking.framework";
	private static final String ORIGINAL_DOCKING_FRAMEWORK = "Original";
	public static final String DOCKING_FRAMEWORK_PROVIDER_SERVICE =
		"org.gjt.sp.jedit.gui.DockingFrameworkProvider";
	private static DockingFrameworkProvider dockingFrameworkProvider;

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

	//{{{ getDockingFrameworkName() method
	public static String getDockingFrameworkName()
	{
		String framework = jEdit.getProperty(
				VIEW_DOCKING_FRAMEWORK_PROPERTY, ORIGINAL_DOCKING_FRAMEWORK);
		return framework;
	} //}}}

	//{{{ getDockingFrameworkProvider() method
	public static DockingFrameworkProvider getDockingFrameworkProvider()
	{
		if (dockingFrameworkProvider == null)
		{
			String framework = getDockingFrameworkName();
			dockingFrameworkProvider = (DockingFrameworkProvider)
				ServiceManager.getService(
					DOCKING_FRAMEWORK_PROVIDER_SERVICE, framework);

			if (dockingFrameworkProvider == null)
			{
				Log.log(Log.ERROR, View.class, "No docking framework " + framework +
							       " available, using the original one");
				dockingFrameworkProvider = (DockingFrameworkProvider)
				ServiceManager.getService(
					DOCKING_FRAMEWORK_PROVIDER_SERVICE, ORIGINAL_DOCKING_FRAMEWORK);
			}
		}
		return dockingFrameworkProvider;
	} //}}}

	//{{{ getToolBar() method
	/**
	 * Returns the view's tool bar.
	 * @since jEdit 4.2pre1
	 */
	public Container getToolBar()
	{
		return toolBar;
	} //}}}

	//{{{ addToolBar() methods
	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		addToolBar(DEFAULT_GROUP, DEFAULT_LAYER, toolBar);
	}

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
	}
	
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
		if (toolBarManager == null) return;
		if (toolBar == null) return;
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
			visit(new SetCursorVisitor(cursor));
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

			visit(new SetCursorVisitor(cursor));
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

	//{{{ getActionBar() method
	/**
	 * Returns the action bar.
	 * @since jEdit 4.2pre3
	 */
	public final ActionBar getActionBar()
	{
		return actionBar;
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

		searchBar.setHyperSearch(false);

		JEditTextArea textArea = getTextArea();

		if(word)
		{
			String text = textArea.getSelectedText();
			if(text == null)
			{
				textArea.selectWord();
				text = textArea.getSelectedText();
			}
			else if(text.indexOf('\n') != -1)
				text = null;

			if(text != null && SearchAndReplace.getRegexp())
				text = SearchAndReplace.escapeRegexp(text,false);

			searchBar.getField().setText(text);
		}

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

		if(word)
		{
			String text = textArea.getSelectedText();
			if(text == null)
			{
				textArea.selectWord();
				text = textArea.getSelectedText();
			}

			if(text != null && text.indexOf('\n') == -1)
			{
				if(SearchAndReplace.getRegexp())
				{
					text = SearchAndReplace.escapeRegexp(
						text,false);
				}

				HistoryModel.getModel("find").addItem(text);
				SearchAndReplace.setSearchString(text);
				SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
				SearchAndReplace.hyperSearch(this);

				return;
			}
		}

		if(searchBar == null)
			searchBar = new SearchBar(this,true);
		if(searchBar.getParent() == null)
			addToolBar(TOP_GROUP,SEARCH_BAR_LAYER,searchBar);

		searchBar.setHyperSearch(true);
		searchBar.getField().setText(null);
		searchBar.getField().requestFocus();
		searchBar.getField().selectAll();
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

	//}}}

	//{{{ Input handling

	//{{{ getKeyEventInterceptor() method
	/**
	 * Returns the listener that will handle all key events in this
	 * view, if any.
	 * @return the key event interceptor or null
	 */
	public KeyListener getKeyEventInterceptor()
	{
		return inputHandler.getKeyEventInterceptor();
	} //}}}

	//{{{ setKeyEventInterceptor() method
	/**
	 * Sets the listener that will handle all key events in this
	 * view. For example, the complete word command uses this so
	 * that all key events are passed to the word list popup while
	 * it is visible.
	 * @param listener The key event interceptor.
	 */
	public void setKeyEventInterceptor(KeyListener listener)
	{
		inputHandler.setKeyEventInterceptor(listener);
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
	@Override
	public void processKeyEvent(KeyEvent evt)
	{
		inputHandler.processKeyEvent(evt,VIEW, false);
		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	public void processKeyEvent(KeyEvent evt, boolean calledFromTextArea)
	{
		processKeyEvent(evt,calledFromTextArea
			? TEXT_AREA
			: VIEW);
	} //}}}

	//{{{ processKeyEvent() method
	public static final int VIEW = 0;
	public static final int TEXT_AREA = 1;
	public static final int ACTION_BAR = 2;
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	public void processKeyEvent(KeyEvent evt, int from)
	{
		inputHandler.processKeyEvent(evt, from, false);
		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	}
	//}}}

	//{{{ Buffers, edit panes, split panes

	//{{{ splitHorizontally() method
	/**
	 * Splits the view horizontally.
	 * @return the new editPane
	 * @since jEdit 4.1pre2
	 */
	public EditPane splitHorizontally()
	{
		return split(JSplitPane.VERTICAL_SPLIT);
	} //}}}

	//{{{ splitVertically() method
	/**
	 * Splits the view vertically.
	 * @return the new editPane
	 * @since jEdit 4.1pre2
	 */
	public EditPane splitVertically()
	{
		return split(JSplitPane.HORIZONTAL_SPLIT);
	} //}}}

	//{{{ split() method
	/**
	 * Splits the view.
	 * @param orientation the orientation {@link javax.swing.JSplitPane#HORIZONTAL_SPLIT} or
	 * {@link javax.swing.JSplitPane#VERTICAL_SPLIT}
	 * @return the new editPane
	 * @since jEdit 4.1pre2
	 */
	public EditPane split(int orientation)
	{
		PerspectiveManager.setPerspectiveDirty(true);

		editPane.saveCaretInfo();
		EditPane oldEditPane = editPane;
		EditPane newEditPane = createEditPane(oldEditPane);
//		setEditPane(newEditPane);
		newEditPane.loadCaretInfo();

		JComponent oldParent = (JComponent)oldEditPane.getParent();

		final JSplitPane newSplitPane = new JSplitPane(orientation,
							       jEdit.getBooleanProperty("appearance.continuousLayout"));
		newSplitPane.setOneTouchExpandable(true);
		newSplitPane.setBorder(null);
		newSplitPane.setMinimumSize(new Dimension(0,0));
		newSplitPane.setResizeWeight(0.5);

		int parentSize = orientation == JSplitPane.VERTICAL_SPLIT
			? oldEditPane.getHeight() : oldEditPane.getWidth();
		final int dividerPosition = (int)((parentSize
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
			newSplitPane.setRightComponent(newEditPane);

			oldSplitPane.setDividerLocation(dividerPos);
		}
		else
		{
			splitPane = newSplitPane;

			newSplitPane.setLeftComponent(oldEditPane);
			newSplitPane.setRightComponent(newEditPane);

			setMainContent(newSplitPane);

		}

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				newSplitPane.setDividerLocation(dividerPosition);
			}
		});

		newEditPane.focusOnTextArea();

		return newEditPane;
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
			lastSplitConfig = getSplitConfig();

			PerspectiveManager.setPerspectiveDirty(true);
			BufferSet.Scope scope = jEdit.getBufferSetManager().getScope();
			for(EditPane _editPane: getEditPanes())
			{
				if(editPane != _editPane)
				{
					if (scope == BufferSet.Scope.editpane)
						mergeBufferSets(editPane, _editPane);
					_editPane.close();
				}
			}

			setMainContent(editPane);

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
			lastSplitConfig = getSplitConfig();

			PerspectiveManager.setPerspectiveDirty(true);

			// find first split pane parenting current edit pane
			Component comp = editPane;
			while(!(comp instanceof JSplitPane) && comp != null)
			{
				comp = comp.getParent();
			}

			BufferSet.Scope scope = jEdit.getBufferSetManager().getScope();
			// get rid of any edit pane that is a child
			// of the current edit pane's parent splitter
			for(EditPane _editPane: getEditPanes())
			{
				if(GUIUtilities.isAncestorOf(comp,_editPane)
					&& _editPane != editPane)
				{
					if (scope == BufferSet.Scope.editpane)
						mergeBufferSets(editPane, _editPane);
					_editPane.close();
				}
			}

			JComponent parent = comp == null ? null : (JComponent)comp.getParent();

			if(parent instanceof JSplitPane)
			{
				JSplitPane parentSplit = (JSplitPane)parent;
				int pos = parentSplit.getDividerLocation();
				if(parentSplit.getLeftComponent() == comp)
					parentSplit.setLeftComponent(editPane);
				else
					parentSplit.setRightComponent(editPane);
				parentSplit.setDividerLocation(pos);
				parent.revalidate();
			}
			else
			{
				setMainContent(editPane);
				splitPane = null;
			}

			updateTitle();

			editPane.focusOnTextArea();
		}
		else
			getToolkit().beep();
	} //}}}

	//{{{ resplit() method
	/**
	 * Restore the split configuration as it was before unsplitting.
	 *
	 * @since jEdit 4.3pre1
	 */
	public void resplit()
	{
		if(lastSplitConfig == null)
			getToolkit().beep();
		else
			setSplitConfig(null,lastSplitConfig);
	} //}}}

	//{{{ getSplitConfig() method
	/**
	*   Split configurations are recorded in a simple RPN "language".
	*   @return The split configuration, describing where splitpanes
	*           are, which buffers are open in each EditPane, etc.
	*
	*/
	public String getSplitConfig()
	{
		StringBuilder splitConfig = new StringBuilder();

		if(splitPane != null)
			getSplitConfig(splitPane,splitConfig);
		else
		{
			appendToSplitConfig(splitConfig, editPane);
		}

		return splitConfig.toString();
	} //}}}

	//{{{ setSplitConfig() method
	/**
	 * sets the split configuration as per the splitConfig.
	 *
	 * @param buffer if null, checks all buffers to restore View's split config.
	 * @param splitConfig the split config, as returned by getSplitConfig()
	 */
	public void setSplitConfig(Buffer buffer, String splitConfig)
	{
		try
		{
			Component comp = restoreSplitConfig(buffer,splitConfig);
			setMainContent(comp);
			updateTitle();
		}
		catch(IOException e)
		{
			// this should never throw an exception.
			throw new InternalError();
		}
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
	 * @return the top JSplitPane if any.
	 * @since jEdit 2.3pre2
	 */
	public JSplitPane getSplitPane()
	{
		return splitPane;
	} //}}}

	//{{{ getBuffer() method
	/**
	 * Returns the current edit pane's buffer.
	 * @return the current edit pane's buffer, it can be null
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
	 * @param buffer The buffer
	 */
	public void setBuffer(Buffer buffer)
	{
		setBuffer(buffer,false);
	} //}}}

	//{{{ setBuffer() method
	/**
	 * Sets the current edit pane's buffer.
	 * @param buffer The buffer
	 * @param disableFileStatusCheck Disables file status checking
	 * regardless of the state of the checkFileStatus property
	 */
	public void setBuffer(Buffer buffer, boolean disableFileStatusCheck)
	{
		setBuffer(buffer, disableFileStatusCheck, true);
	} //}}}

	//{{{ setBuffer() method
	/**
	 * Sets the current edit pane's buffer.
	 * @param buffer The buffer
	 * @param disableFileStatusCheck Disables file status checking
	 * regardless of the state of the checkFileStatus property
	 * @param focus Whether the textarea should request focus
	 * @since jEdit 4.3pre13
	 */
	public void setBuffer(Buffer buffer, boolean disableFileStatusCheck, boolean focus)
	{
		editPane.setBuffer(buffer, focus);
		int check = jEdit.getIntegerProperty("checkFileStatus");
		if(!disableFileStatusCheck && (check == GeneralOptionPane.checkFileStatus_all ||
						  check == GeneralOptionPane.checkFileStatus_operations ||
						  check == GeneralOptionPane.checkFileStatus_focusBuffer))
			jEdit.checkBufferStatus(this, true);
	} //}}}

	//{{{ goToBuffer() method
	/**
	 * If this buffer is open in one of the view's edit panes, sets focus
	 * to that edit pane. Otherwise, opens the buffer in the currently
	 * active edit pane.
	 * @param buffer The buffer
	 * @return the current edit pane
	 * @since jEdit 4.2pre1
	 */
	public EditPane goToBuffer(Buffer buffer)
	{
		return showBuffer(buffer, true);
	} //}}}

	//{{{ showBuffer() method
	/**
	 * If this buffer is open in one of the view's edit panes, activates
	 * that edit pane. Otherwise, opens the buffer in the currently
	 * active edit pane. But the focus is not moved.
	 * @param buffer The buffer to show
	 * @return the current edit pane
	 * @since jEdit 4.3pre13
	 */
	public EditPane showBuffer(Buffer buffer)
	{
		return showBuffer(buffer, false);
	} //}}}

	//{{{ getTextArea() method
	/**
	 * Returns the current edit pane's text area.
	 * @return the current edit pane's text area, or <b>null</b> if there is no edit pane yet
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
	 * @return the current edit pane
	 * @since jEdit 2.5pre2
	 */
	public EditPane getEditPane()
	{
		return editPane;
	} //}}}

	//{{{ getEditPanes() method
	/**
	 * Returns all edit panes.
	 * @return an array of all edit panes in the view
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
			List<EditPane> vec = new ArrayList<EditPane>();
			getEditPanes(vec,splitPane);
			EditPane[] ep = new EditPane[vec.size()];
			vec.toArray(ep);
			return ep;
		}
	} //}}}

	//{{{ getViewConfig() method
	/**
	 * @return a ViewConfig instance for the current view
	 * @since jEdit 4.2pre1
	 */
	public ViewConfig getViewConfig()
	{
		ViewConfig config = new ViewConfig();
		config.plainView = isPlainView();
		config.splitConfig = getSplitConfig();
		config.extState = getExtendedState();
		config.docking = dockableWindowManager.getDockingLayout(config);
		config.title = userTitle;
		String prefix = config.plainView ? "plain-view" : "view";
		switch (config.extState)
		{
			case Frame.MAXIMIZED_BOTH:
			case Frame.ICONIFIED:
				config.x = jEdit.getIntegerProperty(prefix + ".x",getX());
				config.y = jEdit.getIntegerProperty(prefix + ".y",getY());
				config.width = jEdit.getIntegerProperty(prefix + ".width",getWidth());
				config.height = jEdit.getIntegerProperty(prefix + ".height",getHeight());
				break;

			case Frame.MAXIMIZED_VERT:
				config.x = getX();
				config.y = jEdit.getIntegerProperty(prefix + ".y",getY());
				config.width = getWidth();
				config.height = jEdit.getIntegerProperty(prefix + ".height",getHeight());
				break;

			case Frame.MAXIMIZED_HORIZ:
				config.x = jEdit.getIntegerProperty(prefix + ".x",getX());
				config.y = getY();
				config.width = jEdit.getIntegerProperty(prefix + ".width",getWidth());
				config.height = getHeight();
				break;

			case Frame.NORMAL:
			default:
				config.x = getX();
				config.y = getY();
				config.width = getWidth();
				config.height = getHeight();
				break;
		}
		return config;
	} //}}}

	//}}}

	//{{{ isClosed() method
	/**
	 * Returns true if this view has been closed with
	 * {@link jEdit#closeView(View)}.
	 * @return true if the view is closed
	 */
	public boolean isClosed()
	{
		return closed;
	} //}}}

	//{{{ isPlainView() method
	/**
	 * Returns true if this is an auxilliary view with no dockable windows.
	 * @return true if the view is plain
	 * @since jEdit 4.1pre2
	 */
	public boolean isPlainView()
	{
		return plainView;
	} //}}}

	//{{{ getNext() method
	/**
	 * Returns the next view in the list.
	 * @return the next view
	 */
	public View getNext()
	{
		return next;
	} //}}}

	//{{{ getPrev() method
	/**
	 * Returns the previous view in the list.
	 * @return the preview view
	 */
	public View getPrev()
	{
		return prev;
	} //}}}

	//{{{ handlePropertiesChanged()
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		propertiesChanged();
	} //}}}

	//{{{ handleSearchSettingsChanged() method
	@EBHandler
	public void handleSearchSettingsChanged(SearchSettingsChanged msg)
	{
		if(searchBar != null)
			searchBar.update();
	} //}}}

	//{{{ getMinimumSize() method
	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ setWaitSocket() method
	/**
	 * This socket is closed when the buffer is closed.
	 */
	public void setWaitSocket(Socket waitSocket)
	{
		this.waitSocket = waitSocket;
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return getClass().getName() + '['
			+ (jEdit.getActiveView() == this
			? "active" : "inactive")
			+ ']';
	} //}}}

	//{{{ updateTitle() method
	/**
	 * Updates the title bar.
	 */
	public void updateTitle()
	{
		List<Buffer> buffers = new ArrayList<Buffer>();
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			Buffer buffer = editPanes[i].getBuffer();
			if(!buffers.contains(buffer))
				buffers.add(buffer);
		}

		StringBuilder title = new StringBuilder();

		/* On Mac OS X, apps are not supposed to show their name in the
		title bar. */
		if(!OperatingSystem.isMacOS())
		{
			if (userTitle != null)
				title.append(userTitle);
			else
				title.append(jEdit.getProperty("view.title"));
		}

		for(int i = 0; i < buffers.size(); i++)
		{
			if(i != 0)
				title.append(", ");

			Buffer buffer = buffers.get(i);
			title.append(showFullPath && !buffer.isNewFile()
				? buffer.getPath(true) : buffer.getName());
			if(buffer.isDirty())
				title.append(jEdit.getProperty("view.title.dirty"));
		}

		setTitle(title.toString());
	} //}}}

	//{{{ setUserTitle() method
	/**
	 * Sets a user-defined title for this view instead of the "view.title" property.
	 */
	public void setUserTitle(String title)
	{
		userTitle = title + " - ";
		updateTitle();
	} //}}}

	//{{{ showUserTitleDialog() method
	/**
	 * Shows a dialog for selecting a user-defined title for this view.
	 */
	public void showUserTitleDialog()
	{
		String title = JOptionPane.showInputDialog(this, jEdit.getProperty(
			"view.title.select"));
		if (title == null)
			return;
		setUserTitle(title);
	} //}}}

	//{{{ getPrefixFocusOwner() method
	public Component getPrefixFocusOwner()
	{
		return prefixFocusOwner;
	} //}}}

	//{{{ setPrefixFocusOwner() method
	public void setPrefixFocusOwner(Component prefixFocusOwner)
	{
		this.prefixFocusOwner = prefixFocusOwner;
	} //}}}

	//{{{ visit() method
	/**
	 * Visit the the editpanes and textareas of the view
	 * @param visitor the visitor
	 * @since jEdit 4.3pre13
	 */
	public void visit(JEditVisitor visitor)
	{
		EditPane[] panes = getEditPanes();
		for (int i = 0; i < panes.length; i++)
		{
			EditPane editPane = panes[i];
			visitor.visit(editPane);
			visitor.visit(editPane.getTextArea());
		}
	} //}}}

	// {{{ closeAllMenus()
	/** closes any popup menus that may have been opened 
	    @since jEdit 4.4pre1
	*/
	public void closeAllMenus()
	{
		MenuSelectionManager.defaultManager().clearSelectedPath();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
	} // }}}
	
	//{{{ Package-private members
	View prev;
	View next;

	//{{{ View constructor
	View(Buffer buffer, ViewConfig config)
	{
		fullScreenMode = false;
		menuBar = null;
		plainView = config.plainView;

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		setIconImage(GUIUtilities.getEditorIcon());

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		dockableWindowManager = getDockingFrameworkProvider().create(this,
			DockableWindowFactory.getInstance(), config);
		userTitle = config.title;
		dockableWindowManager.setMainPanel(mainPanel);

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

		setSplitConfig(buffer,config.splitConfig);

		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);

		dockableWindowManager.init();

		// tool bar and status bar gets added in propertiesChanged()
		// depending in the 'tool bar alternate layout' setting.
		propertiesChanged();

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		setFocusTraversalPolicy(new MyFocusTraversalPolicy());

		EditBus.addToBus(this);

		GUIUtilities.addSizeSaver(this, null, plainView ? "plain-view" : "view");
	} //}}}

	//{{{ updateFullScreenProps() method
	public void updateFullScreenProps()
	{
		boolean alternateLayout = jEdit.getBooleanProperty(
			"view.toolbar.alternateLayout");
		boolean showMenu = jEdit.getBooleanProperty("fullScreenIncludesMenu");
		boolean showToolbars = jEdit.getBooleanProperty("fullScreenIncludesToolbar");
		boolean showStatus = jEdit.getBooleanProperty("fullScreenIncludesStatus");
		if (! showMenu)
		{
			menuBar = getJMenuBar();
			setJMenuBar(null);
		}
		else if (menuBar != null)
			setJMenuBar(menuBar);
		// Note: Bottom toolbar is the action bar, which is always enabled
		loadToolBars();
		if (alternateLayout)
		{
			if (! showStatus)
				removeToolBar(status);
			else
				addToolBar(BOTTOM_GROUP,STATUS_BAR_LAYER,status);
		}
		else
		{
			if (! showStatus)
				getContentPane().remove(status);
			else
				getContentPane().add(BorderLayout.SOUTH,status);
		}
	} //}}}

	//{{{ toggleFullScreen() method
	public void toggleFullScreen()
	{
		fullScreenMode = (! fullScreenMode);
		GraphicsDevice sd = getGraphicsConfiguration().getDevice();
		dispose();
		if (fullScreenMode)
		{
			updateFullScreenProps();
			windowedBounds = getBounds();
			setUndecorated(true);
			setBounds(sd.getDefaultConfiguration().getBounds());
			validate();
		}
		else
		{
			boolean showStatus = plainView ? jEdit.getBooleanProperty("view.status.plainview.visible") :
				jEdit.getBooleanProperty("view.status.visible");
			if ((menuBar != null) && (getJMenuBar() != menuBar))
				setJMenuBar(menuBar);
			boolean alternateLayout = jEdit.getBooleanProperty(
				"view.toolbar.alternateLayout");
			loadToolBars();
			if (showStatus)
			{
				if (alternateLayout)
					addToolBar(BOTTOM_GROUP,STATUS_BAR_LAYER,status);
				else
					getContentPane().add(BorderLayout.SOUTH,status);
			}
			setUndecorated(false);
			setBounds(windowedBounds);
		}
		setVisible(true);
		toFront();
		closeAllMenus();
		// so you can keep typing in your editpane afterwards...
		editPane.getTextArea().requestFocus();
	} //}}}

	//{{{ confirmToCloseDirty() methods
	/**
	 * If the view contains dirty buffers which will be closed on
	 * closing the view, show the confirmation dialog for user.
	 * @return
	 * 	true if there are no such buffers or user select OK
	 * 	to close the view; false if user select Cancel
	 */
	boolean confirmToCloseDirty()
	{
		Set<Buffer> checkingBuffers = getOpenBuffers();
		for (View view: jEdit.getViews())
		{
			if (view != this)
			{
				checkingBuffers.removeAll(
					view.getOpenBuffers());
			}
		}
		for (Buffer buffer: checkingBuffers)
		{
			if (buffer.isDirty())
			{
				return new CloseDialog(this, checkingBuffers).isOK();
			}
		}
		return true;
	} //}}}

	//{{{ close() method
	void close()
	{
		EditBus.send(new ViewUpdate(this,ViewUpdate.CLOSED));
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

		// notify clients with -wait
		if(waitSocket != null)
		{
			try
			{
				waitSocket.getOutputStream().write('\0');
				waitSocket.getOutputStream().flush();
				waitSocket.getInputStream().close();
				waitSocket.getOutputStream().close();
				waitSocket.close();
			}
			catch(IOException io)
			{
				//Log.log(Log.ERROR,this,io);
			}
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private boolean closed;

	private DockableWindowManager dockableWindowManager;
	private JPanel mainPanel;

	private JPanel topToolBars;
	private JPanel bottomToolBars;
	private ToolBarManager toolBarManager;

	private Container toolBar;
	private SearchBar searchBar;
	private ActionBar actionBar;

	private EditPane editPane;
	private JSplitPane splitPane;
	private String lastSplitConfig;

	private StatusBar status;

	private InputHandler inputHandler;
	private Macros.Recorder recorder;
	private Component prefixFocusOwner;

	private int waitCount;

	private boolean showFullPath;

	private boolean plainView;

	private Socket waitSocket;
	private Component mainContent;

	private boolean fullScreenMode;
	private Rectangle windowedBounds;
	private JMenuBar menuBar;
	private String userTitle;
	//}}}

	//{{{ setMainContent() method
	private void setMainContent(Component c)
	{
		if (mainContent != null)
			mainPanel.remove(mainContent);
		mainContent = c;
		mainPanel.add(mainContent, BorderLayout.CENTER);
		if (c instanceof JSplitPane)
		{
			splitPane = (JSplitPane)c;	
		}
		else
		{
			splitPane = null;
			editPane = (EditPane)c;
		}
		mainPanel.revalidate();
		mainPanel.repaint();
	} //}}}

	//{{{ getEditPanes() method
	private static void getEditPanes(List<EditPane> vec, Component comp)
	{
		if(comp instanceof EditPane)
			vec.add((EditPane) comp);
		else if(comp instanceof JSplitPane)
		{
			JSplitPane split = (JSplitPane)comp;
			getEditPanes(vec,split.getLeftComponent());
			getEditPanes(vec,split.getRightComponent());
		}
	} //}}}

	//{{{ showBuffer() method
	private EditPane showBuffer(Buffer buffer, boolean focus)
	{
		if(editPane.getBuffer() == buffer
			&& editPane.getTextArea().getVisibleLines() > 1)
		{
			if (focus)
				editPane.focusOnTextArea();
			return editPane;
		}

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			EditPane ep = editPanes[i];
			if(ep.getBuffer() == buffer
				/* ignore zero-height splits, etc */
				&& ep.getTextArea().getVisibleLines() > 1)
			{
				setEditPane(ep);
				if (focus)
					ep.focusOnTextArea();
				return ep;
			}
		}

		setBuffer(buffer,false, focus);
		return editPane;
	} //}}}

	//{{{ getSplitConfig() method
	/*
	 * The split config is recorded in a simple RPN "language".
	 */
	private static void getSplitConfig(JSplitPane splitPane,
		StringBuilder splitConfig)
	{
		Component right = splitPane.getRightComponent();
		appendToSplitConfig(splitConfig, right);

		splitConfig.append(' ');

		Component left = splitPane.getLeftComponent();
		appendToSplitConfig(splitConfig, left);

		splitConfig.append(' ');
		splitConfig.append(splitPane.getDividerLocation());
		splitConfig.append(' ');
		splitConfig.append(splitPane.getOrientation()
			== JSplitPane.VERTICAL_SPLIT ? "vertical" : "horizontal");
	} //}}}

	//{{{ appendToSplitConfig() method
	/**
	 * Append the Component to the split config.
	 * The component must be a JSplitPane or an EditPane
	 *
	 * @param splitConfig the split config
	 * @param component the component
	 */
	private static void appendToSplitConfig(StringBuilder splitConfig, Component component)
	{
		if(component instanceof JSplitPane)
		{
			// the component is a JSplitPane
			getSplitConfig((JSplitPane)component,splitConfig);
		}
		else
		{
			// the component is an editPane
			EditPane editPane = (EditPane) component;
			splitConfig.append('"');
			splitConfig.append(StandardUtilities.charsToEscapes(
				editPane.getBuffer().getPath()));
			splitConfig.append("\" buffer");
			BufferSet bufferSet = editPane.getBufferSet();
			Buffer[] buffers = bufferSet.getAllBuffers();
			for (Buffer buffer : buffers)
			{
				if (!buffer.isNewFile())
				{
					splitConfig.append(" \"");
					splitConfig.append(StandardUtilities.charsToEscapes(
						buffer.getPath()));
					splitConfig.append("\" buff");
				}
			}
			splitConfig.append(" \"");
			splitConfig.append(jEdit.getBufferSetManager().getScope());
			splitConfig.append("\" bufferset");
		}
	} //}}}

	//{{{ restoreSplitConfig() method
	private Component restoreSplitConfig(Buffer buffer, String splitConfig)
		throws IOException
	// this is where checked exceptions piss me off. this method only uses
	// a StringReader which can never throw an exception...
	{
		if(buffer != null)
		{
			return editPane = createEditPane(buffer);
		}
		else if(splitConfig == null)
		{

			Buffer buf = jEdit.getFirstBuffer();
			if (buf == null)
			{
				buf = BufferSetManager.createUntitledBuffer();
			}
			return editPane = createEditPane(buf);
		}
		Buffer[] buffers = jEdit.getBuffers();

		Stack<Object> stack = new Stack<Object>();

		// we create a stream tokenizer for parsing a simple
		// stack-based language
		StreamTokenizer st = new StreamTokenizer(new StringReader(
			splitConfig));
		st.whitespaceChars(0,' ');
		/* all printable ASCII characters */
		st.wordChars('#','~');
		st.commentChar('!');
		st.quoteChar('"');
		st.eolIsSignificant(false);
		boolean continuousLayout = jEdit.getBooleanProperty("appearance.continuousLayout");
		List<Buffer> editPaneBuffers = new ArrayList<Buffer>();
loop:		while (true)
		{
			switch(st.nextToken())
			{
			case StreamTokenizer.TT_EOF:
				break loop;
			case StreamTokenizer.TT_WORD:
				if(st.sval.equals("vertical") ||
					st.sval.equals("horizontal"))
				{
					int orientation
						= st.sval.equals("vertical")
						? JSplitPane.VERTICAL_SPLIT
						: JSplitPane.HORIZONTAL_SPLIT;
					int divider = ((Integer)stack.pop())
						.intValue();
					Object obj1 = stack.pop();
					Object obj2 = stack.pop();
					// Backward compatibility with pre-bufferset versions
					if (obj1 instanceof Buffer)
					{
						Buffer b1 = buffer = (Buffer) obj1;
						obj1 = editPane = createEditPane(b1);
					}
					if (obj2 instanceof Buffer)
					{
						Buffer b2 = (Buffer) obj2;
						obj2 = createEditPane(b2);
					}
					stack.push(splitPane = new JSplitPane(
						orientation,
						continuousLayout,
						(Component)obj1,
						(Component)obj2));
					splitPane.setOneTouchExpandable(true);
					splitPane.setBorder(null);
					splitPane.setMinimumSize(
						new Dimension(0,0));
					splitPane.setDividerLocation(divider);
				}
				else if(st.sval.equals("buffer"))
				{
					Object obj = stack.pop();
					if(obj instanceof Integer)
					{
						int index = ((Integer)obj).intValue();
						if(index >= 0 && index < buffers.length)
							buffer = buffers[index];
					}
					else if(obj instanceof String)
					{
						String path = (String)obj;
						buffer = jEdit.getBuffer(path);
						if (buffer == null)
						{
							buffer = jEdit.openTemporary(jEdit.getActiveView(), null,
											    path, true, null);
							jEdit.commitTemporary(buffer);
						}
					}

					if(buffer == null)
						buffer = jEdit.getFirstBuffer();
					stack.push(buffer);
					editPaneBuffers.add(buffer);
				}
				else if (st.sval.equals("buff"))
				{
					String path = (String)stack.pop();
					buffer = jEdit.getBuffer(path);
					if (buffer == null)
					{
						Log.log(Log.WARNING, this, "Buffer " + path + " doesn't exist");
					}
					else
					{
						editPaneBuffers.add(buffer);
					}
				}
				else if (st.sval.equals("bufferset"))
				{
					// pop the bufferset scope. Not used anymore but still here for compatibility
					// with old perspectives
					stack.pop();
					buffer = (Buffer) stack.pop();
					editPane = createEditPane(buffer);
					stack.push(editPane);
					BufferSet bufferSet = editPane.getBufferSet();
					int i = 0;
					for (Buffer buff : editPaneBuffers)
					{
						bufferSet.addBufferAt(buff,i);
						i++;
					}
					editPaneBuffers.clear();
				}
				break;
			case StreamTokenizer.TT_NUMBER:
				stack.push((int)st.nval);
				break;
			case '"':
				stack.push(st.sval);
				break;
			}
		}

		// Backward compatibility with pre-bufferset versions
		Object obj = stack.peek();
		if (obj instanceof Buffer)
		{
			obj = editPane = createEditPane((Buffer)obj);
		}

		updateGutterBorders();

		return (Component)obj;
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

		status.propertiesChanged();

		removeToolBar(status);
		getContentPane().remove(status);

		boolean showStatus = plainView ? jEdit.getBooleanProperty("view.status.plainview.visible") :
				    jEdit.getBooleanProperty("view.status.visible");
		if (jEdit.getBooleanProperty("view.toolbar.alternateLayout"))
		{
			getContentPane().add(BorderLayout.NORTH,topToolBars);
			getContentPane().add(BorderLayout.SOUTH,bottomToolBars);
			if (showStatus)
				addToolBar(BOTTOM_GROUP,STATUS_BAR_LAYER,status);
		}
		else
		{
			mainPanel.add(topToolBars, BorderLayout.NORTH);
			mainPanel.add(bottomToolBars, BorderLayout.SOUTH);
			if (showStatus)
				getContentPane().add(BorderLayout.SOUTH,status);
		}
		updateBufferSwitcherStates();

		getRootPane().revalidate();

		if (splitPane != null)
			GUIUtilities.initContinuousLayout(splitPane);
		//SwingUtilities.updateComponentTreeUI(getRootPane());

		if (fullScreenMode)
			updateFullScreenProps();
	} //}}}

	//{{{ updateBufferSwitcherStates() method
	/**
	 * Enables or Disables the "Focus Buffer Switcher" menu item in the View menu
	 * depending on the visible state of the buffer switcher.  The menu item
	 * is intended to have the same effect as clicking on the buffer switcher
	 * combo box, and it doesn't make sense to have this action available if
	 * the buffer switcher isn't visible.
	 * Also shows or hides the Buffer Switcher itself, since this can be invoked after
	 * the toggle buffer switcher action.
	 */
	public void updateBufferSwitcherStates()
	{
		boolean show = jEdit.getBooleanProperty("view.showBufferSwitcher");
		JMenuBar menubar = getJMenuBar();
		if (menubar == null)
		{
			return;
		}
		String viewmenu_label = jEdit.getProperty("view.label");
		viewmenu_label = viewmenu_label.replace("$", "");
		String sbs_label = jEdit.getProperty("focus-buffer-switcher.label");
		sbs_label = sbs_label.replace("$", "");
		JMenu viewmenu = null;
		for (int i = 0; i < menubar.getMenuCount(); i++)
		{
			JMenu menu = menubar.getMenu(i);
			if (menu.getText().equals(viewmenu_label))
			{
				viewmenu = menu;
				break;
			}
		}
		if (viewmenu != null)
		{
			for (int i = 0; i < viewmenu.getMenuComponentCount(); i++)
			{
				Component item = viewmenu.getMenuComponent(i);
				if (item instanceof JMenuItem && ((JMenuItem)item).getText().equals(sbs_label))
				{
					item.setEnabled(show);
					// viewmenu.invalidate();
				}
			}
		}
		// Toggle the visibility of the BufferSwitcher itself
		for (View v: jEdit.getViews())
			for (EditPane ep: v.getEditPanes())
				ep.loadBufferSwitcher();
	} //}}}


	//{{{ loadToolBars() method
	private void loadToolBars()
	{
		if((! plainView) && (fullScreenMode ?
			jEdit.getBooleanProperty("fullScreenIncludesToolbar") :
			jEdit.getBooleanProperty("view.showToolbar")))
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

		if(searchBar != null)
		{
			searchBar.propertiesChanged();
			removeToolBar(searchBar);
		}

		if(jEdit.getBooleanProperty("view.showSearchbar") && !plainView)
		{
			if(searchBar == null)
				searchBar = new SearchBar(this,false);
			addToolBar(TOP_GROUP,SEARCH_BAR_LAYER,searchBar);
		}
	} //}}}

	//{{{ createEditPane() methods
	private EditPane createEditPane(Buffer buffer)
	{
		EditPane editPane = new EditPane(this, null, buffer);
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addFocusListener(new FocusHandler());
		textArea.addCaretListener(new CaretHandler());
		textArea.addScrollListener(new ScrollHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
		return editPane;
	}

	private EditPane createEditPane(EditPane oldEditPane)
	{
		EditPane editPane = new EditPane(this, oldEditPane.getBufferSet(), oldEditPane.getBuffer());
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
	@EBHandler
	public void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
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
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		EditPane editPane = msg.getEditPane();
		if(editPane !=  null &&
			editPane.getView() == this
			&& msg.getWhat() == EditPaneUpdate.BUFFER_CHANGED
			&& editPane.getBuffer().isLoaded())
		{
			closeDuplicateBuffers(msg);
			status.updateCaretStatus();
			status.updateBufferStatus();
			status.updateMiscStatus();
		}
	} //}}}

	//{{{ closeDuplicateBuffers() method
	private void closeDuplicateBuffers(EditPaneUpdate epu)
	{
		if (!jEdit.getBooleanProperty("buffersets.exclusive"))
			return;
		final BufferSet.Scope scope = jEdit.getBufferSetManager().getScope();
		if (scope == BufferSet.Scope.global)
			return;
		final EditPane ep = epu.getEditPane();
		/* Only one view needs to handle this message, since
		   we iterate through all the other views */
		final View view = ep.getView();
		if (view != this)
			return;
		final Buffer b = ep.getBuffer();

		jEdit.visit(new JEditVisitorAdapter()
		{
			@Override
			public void visit(EditPane editPane)
			{
				if (editPane == ep ||
					(scope == BufferSet.Scope.view && editPane.getView() == view))
					return;
				if (editPane.getBufferSet().indexOf(b) < 0)
					return;
				jEdit.getBufferSetManager().removeBuffer(editPane, b);
			}
		});
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

	//{{{ getOpenBuffers() method
	private Set<Buffer> getOpenBuffers()
	{
		Set<Buffer> openBuffers = new HashSet<Buffer>();
		for (EditPane editPane: getEditPanes())
		{
			openBuffers.addAll(Arrays.asList(
				editPane.getBufferSet().getAllBuffers()));
		}
		return openBuffers;
	} //}}}

	//{{{ mergeBufferSets() method
	/**
	 * Merge a EditPane's BufferSet into another one.
	 * This is used on unsplitting panes not to close buffers.
	 * @param target the target bufferSet where we will merge buffers from source
	 * @param source the source bufferSet
	 */
	private static void mergeBufferSets(EditPane target, EditPane source)
	{
		BufferSetManager manager = jEdit.getBufferSetManager();
		for (Buffer buffer: source.getBufferSet().getAllBuffers())
		{
			manager.addBuffer(target, buffer);
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ CaretHandler class
	private class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			if(evt.getSource() == getTextArea())
				status.updateCaretStatus();
		}
	} //}}}

	//{{{ FocusHandler class
	private class FocusHandler extends FocusAdapter
	{
		@Override
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
	private class ScrollHandler implements ScrollListener
	{
		public void scrolledVertically(TextArea textArea)
		{
			if(getTextArea() == textArea)
				status.updateCaretStatus();
		}

		public void scrolledHorizontally(TextArea textArea) {}
	} //}}}

	//{{{ WindowHandler class
	private class WindowHandler extends WindowAdapter
	{
		@Override
		public void windowActivated(WindowEvent evt)
		{
			boolean editPaneChanged =
				jEdit.getActiveViewInternal() != View.this;
			jEdit.setActiveView(View.this);

			// People have reported hangs with JDK 1.4; might be
			// caused by modal dialogs being displayed from
			// windowActivated()
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					int check = jEdit.getIntegerProperty("checkFileStatus");
					if(check == GeneralOptionPane.checkFileStatus_focus ||
					   check == GeneralOptionPane.checkFileStatus_all)
						jEdit.checkBufferStatus(View.this,false);
					else if(check == GeneralOptionPane.checkFileStatus_focusBuffer)
						jEdit.checkBufferStatus(View.this,true);
				}
			});

			if (editPaneChanged)
			{
				EditBus.send(new ViewUpdate(View.this,ViewUpdate
					.ACTIVATED));
			}
		}

		@Override
		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	} //}}}

	//{{{ ViewConfig class
	public static class ViewConfig
	{
		public int x, y, width, height, extState;
		public boolean plainView;
		public String splitConfig;
		public DockingLayout docking;
		public String title;

		public ViewConfig()
		{
		}

		public ViewConfig(boolean plainView)
		{
			this.plainView = plainView;
			String prefix = plainView ? "plain-view" : "view";
			x = jEdit.getIntegerProperty(prefix + ".x",0);
			y = jEdit.getIntegerProperty(prefix + ".y",0);
			width = jEdit.getIntegerProperty(prefix + ".width",0);
			height = jEdit.getIntegerProperty(prefix + ".height",0);
			extState = jEdit.getIntegerProperty(prefix + ".extendedState", Frame.NORMAL);
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

	//{{{ isInsideScreen() method
	private static boolean isInsideScreen(View parent, Rectangle r)
	{
		Rectangle bounds;
		if (parent == null)
			bounds = GUIUtilities.getScreenBounds();
		else
			bounds = parent.getGraphicsConfiguration().getBounds();
		int minWidth = jEdit.getIntegerProperty("view.minStartupWidth");
		int minHeight = jEdit.getIntegerProperty("view.minStartupHeight");
		return  r.x + r.width	> bounds.x + minWidth &&		// right edge at minWidth pixels on the right of the left bound
			r.x		< bounds.x + bounds.width - minWidth &&	// left edge at minWidth pixels on the left of the right bound
			r.y + r.height	> bounds.y + minHeight &&		// bottom edge at minHeight pixels under the top bound
			r.y		< bounds.y + bounds.height - minHeight;	// top edge at minHeight pixels on the top of the bottom bound
	} //}}}

	public void adjust(View parent, ViewConfig config)
	{
		if(config.width != 0 && config.height != 0)
		{
			Rectangle desired = new Rectangle(
					config.x, config.y, config.width, config.height);
			if (! isInsideScreen(parent, desired))
				setLocationRelativeTo(parent);
			else
			{
				if(OperatingSystem.isX11() && Debug.GEOMETRY_WORKAROUND)
					new GUIUtilities.UnixWorkaround(this,"view",desired,config.extState);
				else
				{
					setBounds(desired);
					setExtendedState(config.extState);
				}
			}
		}
		else
			setLocationRelativeTo(parent);
	}

	//{{{ MyFocusTraversalPolicy class
	private static class MyFocusTraversalPolicy extends LayoutFocusTraversalPolicy
	{
		@Override
		public Component getDefaultComponent(Container focusCycleRoot)
		{
			return GUIUtilities.getView(focusCycleRoot).getTextArea();
		}
	} //}}}

	//{{{ SetCursorVisitor class
	private static class SetCursorVisitor extends JEditVisitorAdapter
	{
		private final Cursor cursor;

		SetCursorVisitor(Cursor cursor)
		{
			this.cursor = cursor;
		}

		@Override
		public void visit(EditPane editPane)
		{
			editPane.setCursor(cursor);
		}
	}//}}}
	 //}}}
	
}
