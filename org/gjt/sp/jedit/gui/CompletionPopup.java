/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 KazutoshiSatoda
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
//}}}

/**
 * Popup window for word completion in text area.
 * This class provides basic UI of completion popup.
 *
 * @since jEdit 4.3pre11
 */
public class CompletionPopup extends JWindow
{
	//{{{ interface Candidates
	/**
	 * Candidates of completion.
	 */
	public interface Candidates
	{
		/**
		 * Returns the number of candidates.
		 */
		public int getSize();

		/**
		 * Returns whether this completion is still valid.
		 */
		public boolean isValid();

		/**
		 * Do the completion.
		 */
		public void complete(int index);
	
		/**
		 * Returns a component to render a cell for the index
		 * in the popup.
		 */
		public Component getCellRenderer(JList list, int index,
			boolean isSelected, boolean cellHasFocus);

		/**
		 * Returns a description text shown when the index is
		 * selected in the popup, or null if no description is
		 * available.
		 */
		public String getDescription(int index);
	} //}}}

	//{{{ CompletionPopup constructor
	/**
	 * Create a completion popup.
	 * It is not shown until reset() method is called with valid
	 * candidates. All key events for the view are intercepted by
	 * this popup untill end of completion.
	 * @since jEdit 4.3pre13
	 */ 
	public CompletionPopup(View view)
	{
		super(view);
		this.view = view;
		this.keyHandler = new KeyHandler();
		this.candidates = null;
		this.list = new JList();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new CellRenderer());
		list.addKeyListener(keyHandler);
		list.addMouseListener(new MouseHandler());

		JPanel content = new JPanel(new BorderLayout());
		content.setFocusTraversalKeysEnabled(false);
		// stupid scrollbar policy is an attempt to work around
		// bugs people have been seeing with IBM's JDK -- 7 Sep 2000
		JScrollPane scroller = new JScrollPane(list,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		content.add(scroller, BorderLayout.CENTER);
		setContentPane(content);
		addWindowFocusListener(new WindowFocusHandler());
	}

	public CompletionPopup(View view, Point location)
	{
		this(view);
		if (location != null)
		{
			setLocation(location);
		}
	} //}}}

	//{{{ dispose() method
	/**
	 * Quit completion.
	 */
	public void dispose()
	{
		if (isDisplayable())
		{
			if (view.getKeyEventInterceptor() == keyHandler)
			{
				view.setKeyEventInterceptor(null);
			}
			super.dispose();

			// This is a workaround to ensure setting the
			// focus back to the textArea. Without this, the
			// focus gets lost after closing the popup in
			// some environments. It seems to be a bug in
			// J2SE 1.4 or 5.0. Probably it relates to the
			// following one.
			// "Frame does not receives focus after closing
			// of the owned window"
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4810575
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					view.getTextArea().requestFocus();
				}
			});
		}
	} //}}}

	//{{{ reset() method
	/**
	 * Start completion.
	 * @param candidates The candidates of this completion
	 * @param active Set focus to the popup
	 */
	public void reset(Candidates candidates, boolean active)
	{
		if(candidates == null || !candidates.isValid()
			|| candidates.getSize() <= 0)
		{
			dispose();
			return;
		}

		this.candidates = candidates;
		list.setModel(new CandidateListModel());
		list.setVisibleRowCount(Math.min(candidates.getSize(),8));
		pack();
		setLocation(fitInScreen(getLocation(null),this,
			view.getTextArea().getPainter().getFontMetrics().getHeight()));
		if (active)
		{
			setSelectedIndex(0);
			GUIUtilities.requestFocus(this,list);
		}
		setVisible(true);
		view.setKeyEventInterceptor(keyHandler);
	} //}}}

	//{{{ getCandidates() method
	/**
	 * Current candidates of completion.
	 */
	public Candidates getCandidates()
	{
		return candidates;
	} //}}}

	//{{{ getSelectedIndex() method
	/**
	 * Returns index of current selection.
	 * Returns -1 if nothing is selected.
	 */
	public int getSelectedIndex()
	{
		return list.getSelectedIndex();
	} //}}}

	//{{{ setSelectedIndex() method
	/**
	 * Set selection.
	 */
	public void setSelectedIndex(int index)
	{
		if (candidates != null
			&& 0 <= index && index < candidates.getSize())
		{
			list.setSelectedIndex(index);
			list.ensureIndexIsVisible(index);
			String description = candidates.getDescription(index);
			if (description != null)
			{
				view.getStatus().setMessageAndClear(description);
			}
		}
	} //}}}

	//{{{ doSelectedCompletion() method
	/**
	 * Do completion with current selection and quit.
	 */
	public boolean doSelectedCompletion()
	{
		int selected = list.getSelectedIndex();
		if (candidates != null &&
			0 <= selected && selected < candidates.getSize())
		{
			candidates.complete(selected);
			dispose();
			return true;
		}
		return false;
	} //}}}

	//{{{ keyPressed() medhod
	/**
	 * Handle key pressed events.
	 * Override this method to make additional key handing.
	 */
	protected void keyPressed(KeyEvent e)
	{
	} //}}}

	//{{{ keyTyped() medhod
	/**
	 * Handle key typed events.
	 * Override this method to make additional key handing.
	 */
	protected void keyTyped(KeyEvent e)
	{
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final View view;
	private final KeyHandler keyHandler;
	private Candidates candidates;
	private final JList list;
	//}}}

	//{{{ fitInScreen() method
	private static Point fitInScreen(Point p, Window w, int lineHeight)
	{
		Rectangle screenSize = w.getGraphicsConfiguration().getBounds();
		if(p.y + w.getHeight() >= screenSize.height)
			p.y = p.y - w.getHeight() - lineHeight;
		return p;
	} //}}}

	//{{{ moveRelative method()
	private void moveRelative(int n)
	{
		int selected = list.getSelectedIndex();

		int newSelect = selected + n;
		if (newSelect < 0)
		{
			newSelect = 0;
		}
		else
		{
			int numItems = list.getModel().getSize();
			if(numItems < 1)
			{
				return;
			}
			if(newSelect >= numItems)
			{
				newSelect = numItems - 1;
			}
		}

		if(newSelect != selected)
		{
			setSelectedIndex(newSelect);
		}
	} //}}}

	//{{{ moveRelativePages() method
	private void moveRelativePages(int n)
	{
		int pageSize = list.getVisibleRowCount() - 1;
		moveRelative(pageSize * n);
	} //}}}

	//{{{ passKeyEventToView() method
	private void passKeyEventToView(KeyEvent e)
	{
		// Remove intercepter to avoid infinite recursion.
		assert (view.getKeyEventInterceptor() == keyHandler);
		view.setKeyEventInterceptor(null);

		// Here depends on an implementation detail.
		// Use ACTION_BAR to force processing KEY_TYPED event in
		// the implementation of gui.InputHandler.processKeyEvent().
		view.getInputHandler().processKeyEvent(e, View.ACTION_BAR, false);

		// Restore keyHandler only if this popup is still alive.
		// The key event might trigger dispose() of this popup.
		if (this.isDisplayable())
		{
			view.setKeyEventInterceptor(keyHandler);
		}
	} //}}}

	//{{{ CandidateListModel class
	private class CandidateListModel extends AbstractListModel
	{
		public int getSize()
		{
			return candidates.getSize();
		}

		public Object getElementAt(int index)
		{
			// This value is not used.
			// The list is only rendered by components
			// returned by getCellRenderer().
			return candidates;
		}
	} //}}}

	//{{{ CellRenderer class
	private class CellRenderer implements ListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			return candidates.getCellRenderer(list, index,
				isSelected, cellHasFocus);
		}
	} //}}}

	//{{{ KeyHandler class
	private class KeyHandler extends KeyAdapter
	{
		//{{{ keyPressed() method
		public void keyPressed(KeyEvent e)
		{
			CompletionPopup.this.keyPressed(e);

			if (candidates == null || !candidates.isValid())
			{
				dispose();
			}
			else if (!e.isConsumed())
			{
				switch(e.getKeyCode())
				{
				case KeyEvent.VK_TAB:
				case KeyEvent.VK_ENTER:
					if (doSelectedCompletion())
					{
						e.consume();
					}
					else
					{
						dispose();
					}
					break;
				case KeyEvent.VK_ESCAPE:
					dispose();
					e.consume();
					break;
				case KeyEvent.VK_UP:
					moveRelative(-1);
					e.consume();
					break;
				case KeyEvent.VK_DOWN:
					moveRelative(1);
					e.consume();
					break;
				case KeyEvent.VK_PAGE_UP:
					moveRelativePages(-1);
					e.consume();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					moveRelativePages(1);
					e.consume();
					break;
				default:
					if(e.isActionKey()
						|| e.isControlDown()
						|| e.isAltDown()
						|| e.isMetaDown())
					{
						dispose();
					}
					break;
				}
			}

			if (!e.isConsumed())
			{
				passKeyEventToView(e);
			}
		} //}}}

		//{{{ keyTyped() method
		public void keyTyped(KeyEvent e)
		{
			CompletionPopup.this.keyTyped(e);

			if (candidates == null || !candidates.isValid())
			{
				dispose();
			}

			if (!e.isConsumed())
			{
				passKeyEventToView(e);
			}
		} //}}}
	} //}}}

	//{{{ MouseHandler class
	private class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent e)
		{
			if (doSelectedCompletion())
			{
				e.consume();
			}
			else
			{
				dispose();
			}
		}
	} //}}}

	//{{{ WindowFocusHandler class
	private class WindowFocusHandler implements WindowFocusListener
	{
		public void windowGainedFocus(WindowEvent e)
		{
		}

		public void windowLostFocus(WindowEvent e)
		{
			dispose();
		}
	} //}}}

	//}}}
}
