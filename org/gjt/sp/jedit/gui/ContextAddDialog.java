/*
 * ContextAddDialog.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Marcelo Vancin
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.AbstractContextOptionPane.MenuItem;
//}}}


/**
 * Dialog for showing ActionSets and adding actions to context menus
 * Was package private and located in AbstractContextOptionPane.java
 * until 4.3pre16
 *
 */
public class ContextAddDialog extends EnhancedDialog
{
	private static final String CONTEXT_ADD_DIALOG_LAST_SELECTION = "contextAddDialog.lastSelection";

	//{{{ ContextAddDialog constructor
	public ContextAddDialog(Component comp, ActionContext actionContext)
	{
		super(GUIUtilities.getParentDialog(comp),
		      jEdit.getProperty("options.context.add.title"),
		      true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		JPanel typePanel = new JPanel(new GridLayout(3,1,6,6));
		typePanel.setBorder(new EmptyBorder(0,0,6,0));
		typePanel.add(new JLabel(
					 jEdit.getProperty("options.context.add.caption")));

		separator = new JRadioButton(jEdit.getProperty("options.context"
							       + ".add.separator"));
		separator.addActionListener(actionHandler);
		grp.add(separator);
		typePanel.add(separator);

		action = new JRadioButton(jEdit.getProperty("options.context"
							    + ".add.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		action.setSelected(true);
		typePanel.add(action);

		content.add(BorderLayout.NORTH,typePanel);

		JPanel actionPanel = new JPanel(new BorderLayout(6,6));

		ActionSet[] actionsList = actionContext.getActionSets();

		Collection<ActionSet> actionSets = new TreeSet<ActionSet>();
		String lastSelectionLabel = jEdit.getProperty(CONTEXT_ADD_DIALOG_LAST_SELECTION);
		for (ActionSet actionSet : actionsList)
		{
			if (actionSet.getActionCount() != 0)
			{
				actionSets.add(actionSet);
			}
		}
		int selectionIndex = 0;
		int i = 0;
		for (ActionSet actionSet : actionSets)
		{
			if (actionSet.getLabel().equals(lastSelectionLabel))
			{
				selectionIndex = i;
				break;
			}
			i++;
		}
		combo = new JComboBox(actionSets.toArray());
		combo.setSelectedIndex(selectionIndex);
		combo.addActionListener(actionHandler);
		actionPanel.add(BorderLayout.NORTH,combo);

		list = new JList();
		list.setVisibleRowCount(8);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		actionPanel.add(BorderLayout.CENTER,new JScrollPane(list));

		content.add(BorderLayout.CENTER,actionPanel);

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

		updateList();

		pack();
		setLocationRelativeTo(GUIUtilities.getParentDialog(comp));
		setVisible(true);
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		isOK = true;
		dispose();
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getSelection() method
	public String getSelection()
	{
		if(!isOK)
			return null;

		if(separator.isSelected())
			return "-";
		else if(action.isSelected())
		{
			AbstractContextOptionPane.MenuItem selectedValue =
				(AbstractContextOptionPane.MenuItem) list.getSelectedValue();
			return selectedValue == null ? null : selectedValue.actionName;
		}
		else
			throw new InternalError();
	} //}}}


	//{{{ private members
	private boolean isOK;
	private final JRadioButton separator;
	private final JRadioButton action;
	private final JComboBox combo;
	private final JList list;
	private final JButton ok;
	private final JButton cancel;

	//{{{ updateList() method
	private void updateList()
	{
		ActionSet actionSet = (ActionSet)combo.getSelectedItem();
		jEdit.setProperty(CONTEXT_ADD_DIALOG_LAST_SELECTION, actionSet.getLabel());

		EditAction[] actions = actionSet.getActions();
		Vector<MenuItem> listModel = new Vector<MenuItem>(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			EditAction action = actions[i];
			String label = action.getLabel();
			if(label == null)
				continue;

			listModel.addElement(new MenuItem(action.getName(),label));
		}

		Collections.sort(listModel,new AbstractContextOptionPane.MenuItemCompare());

		list.setListData(listModel);
	} //}}}
	//}}}


	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source instanceof JRadioButton)
			{
				combo.setEnabled(action.isSelected());
				list.setEnabled(action.isSelected());
			}
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == combo)
				updateList();
		}
	} //}}}

}

