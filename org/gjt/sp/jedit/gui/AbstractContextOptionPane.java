/*
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;

/**
 * An abstract base class for context menu editors. Provides the base
 * UI and functionality for creating a context menu populated with
 * jEdit actions.
 *
 * @author Slava Pestov
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre13
 */
public abstract class AbstractContextOptionPane extends AbstractOptionPane
{
	private final ActionContext actionContext;

	/**
	 * Constructor that takes a name as an argument, for use by
	 * subclasses.
	 *
	 * @param name    Name of the option pane.
	 * @param caption String to use as the caption of the context menu
	 *                configuration list.
	 * @since jEdit 4.3pre13
	 */
	protected AbstractContextOptionPane(String name, String caption)
	{
		this(name, caption, jEdit.getActionContext());
	}

	/**
	 * Constructor that takes a name as an argument, for use by
	 * subclasses.
	 *
	 * @param name    Name of the option pane.
	 * @param caption String to use as the caption of the context menu
	 *                configuration list.
	 * @param actionContext the actionContext
	 * @since jEdit 4.5pre1
	 */
	protected AbstractContextOptionPane(String name, String caption, ActionContext actionContext)
	{
		super(name);
		this.actionContext = actionContext;
		this.caption = new JLabel(caption);
	}

	/**
	 * Initializes the pane's UI.
	 */
	@Override
	protected void _init()
	{
		setLayout(new BorderLayout());

		add(BorderLayout.NORTH, caption);

		listModel = new DefaultListModel();
		reloadContextList(getContextMenu());

		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());

		add(BorderLayout.CENTER, new JScrollPane(list));

		buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3, 0, 0, 0));
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		ActionHandler actionHandler = new ActionHandler();
		add = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.add.icon")));
		add.setToolTipText(jEdit.getProperty("common.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(Box.createHorizontalStrut(6));
		remove = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.remove.icon")));
		remove.setToolTipText(jEdit.getProperty("common.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveUp.icon")));
		moveUp.setToolTipText(jEdit.getProperty("common.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveDown.icon")));
		moveDown.setToolTipText(jEdit.getProperty("common.moveDown"));
		moveDown.addActionListener(actionHandler);
		buttons.add(moveDown);
		buttons.add(Box.createGlue());

		// add "reset to defaults" button
		reset = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.reset.icon")));
		reset.setToolTipText(jEdit.getProperty("options.context.reset"));
		reset.addActionListener(actionHandler);
		buttons.add(reset);

		updateButtons();
		add(BorderLayout.SOUTH, buttons);
	}

	/**
	 * Returns the context menu to be edited. The default implementation
	 * returns jEdit's context menu. Subclasses overriding this method
	 * should also override {@link #saveContextMenu(String menu) saveContextMenu}.
	 *
	 * @since jEdit 4.3pre13
	 */
	protected abstract String getContextMenu();

	/**
	 * Saves the context menu configuration.
	 *
	 * @since jEdit 4.3pre13
	 */
	protected abstract void saveContextMenu(String menu);

	/**
	 * Adds a widget to the "buttons" panel at the bottom. The component
	 * will be added at the very right of the button row (separated from
	 * the normal buttons).
	 *
	 * @since jEdit 4.3pre13
	 */
	protected void addButton(JComponent c)
	{
		buttons.add(c);
	}

	static class MenuItemCompare implements Comparator<MenuItem>
	{
		@Override
		public int compare(MenuItem obj1, MenuItem obj2)
		{
			return StandardUtilities.compareStrings(obj1.label, obj2.label, false);
		}
	}

	@Override
	protected void _save()
	{
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < listModel.getSize(); i++)
		{
			if (i != 0)
				buf.append(' ');
			buf.append(((MenuItem) listModel.elementAt(i)).actionName);
		}
		saveContextMenu(buf.toString());
	}

	// private members
	private DefaultListModel listModel;
	private JList list;
	private JButton add;
	private JButton remove;
	private JButton moveUp, moveDown;
	private JButton reset;
	private JLabel caption;
	private JPanel buttons;

	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1 && listModel.getSize() != 0);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
	}

	private void reloadContextList(String contextMenu)
	{
		listModel.clear();
		StringTokenizer st = new StringTokenizer(contextMenu);
		while (st.hasMoreTokens())
		{
			String actionName = st.nextToken();
			if (actionName.equals("-"))
				listModel.addElement(new AbstractContextOptionPane.MenuItem("-", "-"));
			else
			{
				EditAction action = actionContext.getAction(actionName);

				if (action == null)
					continue;
				String label = action.getLabel();
				if (label == null)
					continue;
				listModel.addElement(new AbstractContextOptionPane.MenuItem(actionName, label));
			}
		}
	}

	static class MenuItem
	{
		String actionName;
		String label;

		MenuItem(String actionName, String label)
		{
			this.actionName = actionName;
			this.label = GUIUtilities.prettifyMenuLabel(label);
		}

		public String toString()
		{
			return label;
		}
	}

	class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if (source == add)
			{
				ContextAddDialog dialog = new ContextAddDialog(
					AbstractContextOptionPane.this, actionContext);
				String selection = dialog.getSelection();
				if (selection == null)
					return;

				int index = list.getSelectedIndex();
				if (index == -1)
					index = listModel.getSize();
				else
					index++;

				MenuItem menuItem;
				if (selection.equals("-"))
					menuItem = new AbstractContextOptionPane.MenuItem("-", "-");
				else
				{
					EditAction action = actionContext.getAction(selection);
					String label = action
						.getLabel();
					menuItem = new AbstractContextOptionPane.MenuItem(selection,
						label);
				}

				listModel.insertElementAt(menuItem, index);
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			} else if (source == remove)
			{
				int index = list.getSelectedIndex();
				listModel.removeElementAt(index);
				if (listModel.getSize() != 0)
				{
					list.setSelectedIndex(
						Math.min(listModel.getSize() - 1,
							index));
				}
				updateButtons();
			} else if (source == moveUp)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected, index - 1);
				list.setSelectedIndex(index - 1);
				list.ensureIndexIsVisible(index - 1);
			} else if (source == moveDown)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected, index + 1);
				list.setSelectedIndex(index + 1);
				list.ensureIndexIsVisible(index + 1);
			} else if (source == reset)
			{
				String dialogType = "options.context.reset.dialog";
				int result = GUIUtilities.confirm(list, dialogType, null,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);

				if (result == JOptionPane.YES_OPTION)
				{
					// the user should be able to cancel the options dialog 
					// so we need to modify the list, not the actual property
					// since the default value is not available, 
					// we reset, fetch default value and re-set to original
					String orgContext = jEdit.getProperty("view.context");
					jEdit.resetProperty("view.context");
					String defaultContext = jEdit.getProperty("view.context");
					jEdit.setProperty("view.context", orgContext);
					reloadContextList(defaultContext);

					// reset selection if user had more buttons than default
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					updateButtons();
				}
			}
		}
	}

	class ListHandler implements ListSelectionListener
	{
		@Override
		public void valueChanged(ListSelectionEvent evt)
		{
			updateButtons();
		}
	}
}

