/*
 * ContextOptionPane.java - Context menu options panel
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.options;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

/**
 * Right-click context menu editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ContextOptionPane extends AbstractOptionPane
{
	public ContextOptionPane()
	{
		super("context");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());

		JLabel caption = new JLabel(jEdit.getProperty(
			"options.context.caption"));
		add(BorderLayout.NORTH,caption);

		String contextMenu = jEdit.getProperty("view.context");
		StringTokenizer st = new StringTokenizer(contextMenu);
		listModel = new DefaultListModel();
		while(st.hasMoreTokens())
		{
			String actionName = (String)st.nextToken();
			String label = getActionLabel(actionName);
			if(label == null)
				continue;
			listModel.addElement(new MenuItem(actionName,label));
		}
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());

		add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.add(Box.createGlue());
		ActionHandler actionHandler = new ActionHandler();
		add = new JButton(jEdit.getProperty("options.context.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(Box.createHorizontalStrut(6));
		remove = new JButton(jEdit.getProperty("options.context.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new JButton(jEdit.getProperty("options.context.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new JButton(jEdit.getProperty("options.context.moveDown"));
		moveDown.addActionListener(actionHandler);
		buttons.add(moveDown);
		buttons.add(Box.createGlue());

		updateButtons();
		add(BorderLayout.SOUTH,buttons);

		// create actions list
		EditAction[] actions = jEdit.getActions();
		Vector vector = new Vector(actions.length);
		for(int i = 0; i < actions.length; i++)
		{
			String actionName = actions[i].getName();
			String label = jEdit.getProperty(actionName + ".label");
			if(label == null)
				continue;
			vector.addElement(new MenuItem(actionName,label));
		}
		MiscUtilities.quicksort(vector,new MenuItemCompare());

		actionsList = new DefaultListModel();
		actionsList.ensureCapacity(vector.size());
		for(int i = 0; i < vector.size(); i++)
		{
			actionsList.addElement(vector.elementAt(i));
		}
	}

	class MenuItemCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((MenuItem)obj1).label.toLowerCase().compareTo(
				((MenuItem)obj2).label.toLowerCase());
		}
	}

	protected void _save()
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			buf.append(((MenuItem)listModel.elementAt(i)).actionName);
		}
		jEdit.setProperty("view.context",buf.toString());
	}

	// package-private members
	static String getActionLabel(String actionName)
	{
		if(actionName.equals("-"))
			return "-";
		else
		{
			if(actionName.startsWith("play-macro@"))
			{
				int index = Math.max(11,actionName
					.indexOf('/') + 1);
				return actionName.substring(index)
					.replace('_',' ');
			}
			else
				return jEdit.getProperty(actionName + ".label");
		}
	}

	// private members
	private DefaultListModel listModel;
	private JList list;
	private JButton add;
	private JButton remove;
	private JButton moveUp, moveDown;

	private DefaultListModel actionsList;

	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1 && listModel.getSize() != 0);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
	}

	static class MenuItem
	{
		String actionName;
		String label;

		MenuItem(String actionName, String label)
		{
			this.actionName = actionName;
			if(label.equals("-"))
				this.label = label;
			else
				this.label = GUIUtilities.prettifyMenuLabel(label);
		}

		public String toString()
		{
			return label;
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == add)
			{
				ContextAddDialog dialog = new ContextAddDialog(
					ContextOptionPane.this,
					actionsList);
				MenuItem selection = dialog.getSelection();
				if(selection == null)
					return;

				int index = list.getSelectedIndex();
				if(index == -1)
					index = listModel.getSize();
				else
					index++;

				listModel.insertElementAt(selection,index);
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			}
			else if(source == remove)
			{
				int index = list.getSelectedIndex();
				listModel.removeElementAt(index);
				updateButtons();
			}
			else if(source == moveUp)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index-1);
				list.setSelectedIndex(index-1);
			}
			else if(source == moveDown)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index+1);
				list.setSelectedIndex(index+1);
			}
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			updateButtons();
		}
	}
}

class ContextAddDialog extends EnhancedDialog
{
	public ContextAddDialog(Component comp, ListModel actionsListModel)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("options.context.add.title"),
			true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("options.context.add.caption")));

		JPanel mainPanel = new JPanel(new BorderLayout(6,0));

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		// Add separator
		separator = new JRadioButton(jEdit.getProperty("options.context"
			+ ".add.separator"));
		separator.setSelected(true);
		separator.addActionListener(actionHandler);
		grp.add(separator);
		mainPanel.add(BorderLayout.NORTH,separator);

		// Add action
		JPanel actionPanel = new JPanel(new BorderLayout(6,0));
		action = new JRadioButton(jEdit.getProperty("options.context"
			+ ".add.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		actionPanel.add(BorderLayout.NORTH,action);

		actionsList = new JList(actionsListModel);
		actionsList.setVisibleRowCount(8);
		actionsList.setEnabled(false);
		actionPanel.add(BorderLayout.CENTER,new JScrollPane(actionsList));

		mainPanel.add(BorderLayout.CENTER,actionPanel);

		// Add macro
		JPanel macroPanel = new JPanel(new BorderLayout(6,0));
		macro = new JRadioButton(jEdit.getProperty("options.context"
			+ ".add.macro"));
		macro.addActionListener(actionHandler);
		grp.add(macro);
		macroPanel.add(BorderLayout.NORTH,macro);

		macrosList = new JList(Macros.getMacroList());
		macrosList.setVisibleRowCount(8);
		macrosList.setEnabled(false);
		macroPanel.add(BorderLayout.CENTER,new JScrollPane(macrosList));

		mainPanel.add(BorderLayout.SOUTH,macroPanel);

		content.add(BorderLayout.CENTER,mainPanel);

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel,BoxLayout.X_AXIS));
		southPanel.setBorder(new EmptyBorder(12,0,0,0));
		southPanel.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionHandler);
		getRootPane().setDefaultButton(ok);
		southPanel.add(ok);
		southPanel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		southPanel.add(cancel);
		southPanel.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,southPanel);

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
		show();
	}

	public void ok()
	{
		isOK = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	public ContextOptionPane.MenuItem getSelection()
	{
		if(!isOK)
			return null;

		if(separator.isSelected())
			return new ContextOptionPane.MenuItem("-","-");
		else if(action.isSelected())
			return (ContextOptionPane.MenuItem)actionsList.getSelectedValue();
		else if(macro.isSelected())
		{
			String selectedMacro = macrosList.getSelectedValue().toString();
			selectedMacro = "play-macro@" + selectedMacro;
			return new ContextOptionPane.MenuItem(selectedMacro,
				ContextOptionPane.getActionLabel(selectedMacro));
		}
		else
			throw new InternalError();
	}

	// private members
	private boolean isOK;
	private JRadioButton separator, action, macro;
	private JList actionsList, macrosList;
	private JButton ok, cancel;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source instanceof JRadioButton)
			{
				actionsList.setEnabled(action.isSelected());
				macrosList.setEnabled(macro.isSelected());
			}
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
		}
	}
}
