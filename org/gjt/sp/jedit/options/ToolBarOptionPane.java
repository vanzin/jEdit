/*
 * ToolBarOptionPane.java - Tool bar options panel
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
import java.io.File;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Tool bar editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ToolBarOptionPane extends AbstractOptionPane
{
	public ToolBarOptionPane()
	{
		super("toolbar");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(2,1));

		/* Show toolbar */
		showToolbar = new JCheckBox(jEdit.getProperty(
			"options.toolbar.showToolbar"));
		showToolbar.setSelected(jEdit.getBooleanProperty("view.showToolbar"));
		panel.add(showToolbar);

		panel.add(new JLabel(jEdit.getProperty(
			"options.toolbar.caption")));

		add(BorderLayout.NORTH,panel);

		String toolbar = jEdit.getProperty("view.toolbar");
		StringTokenizer st = new StringTokenizer(toolbar);
		listModel = new DefaultListModel();
		while(st.hasMoreTokens())
		{
			String actionName = (String)st.nextToken();

			String label = ContextOptionPane.getActionLabel(actionName);
			if(label == null)
				continue;

			Icon icon;
			String iconName;
			if(actionName.equals("-"))
			{
				iconName = null;
				icon = null;
			}
			else
			{
				iconName = jEdit.getProperty(actionName + ".icon");
				if(iconName == null)
					continue;

				icon = GUIUtilities.loadIcon(iconName);
			}
			listModel.addElement(new Button(actionName,iconName,icon,label));
		}

		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());
		list.setCellRenderer(new ButtonCellRenderer());

		add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.add(Box.createGlue());
		ActionHandler actionHandler = new ActionHandler();
		add = new JButton(jEdit.getProperty("options.toolbar.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(Box.createHorizontalStrut(6));
		remove = new JButton(jEdit.getProperty("options.toolbar.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new JButton(jEdit.getProperty("options.toolbar.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new JButton(jEdit.getProperty("options.toolbar.moveDown"));
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
			vector.addElement(new Button(actionName,null,null,label));
		}
		MiscUtilities.quicksort(vector,new ButtonCompare());

		actionsList = new DefaultListModel();
		actionsList.ensureCapacity(vector.size());
		for(int i = 0; i < vector.size(); i++)
		{
			actionsList.addElement(vector.elementAt(i));
		}

		// create icons list
		iconList = new DefaultComboBoxModel();
		st = new StringTokenizer(jEdit.getProperty("icons"));
		while(st.hasMoreElements())
		{
			String icon = st.nextToken();
			iconList.addElement(new IconListEntry(
				GUIUtilities.loadIcon(icon),icon));
		}
	}

	class ButtonCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((Button)obj1).label.toLowerCase().compareTo(
				((Button)obj2).label.toLowerCase());
		}
	}

	protected void _save()
	{
		jEdit.setBooleanProperty("view.showToolbar",showToolbar
			.isSelected());

		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			Button button = (Button)listModel.elementAt(i);
			buf.append(button.actionName);
			jEdit.setProperty(button.actionName + ".icon",button.iconName);
		}
		jEdit.setProperty("view.toolbar",buf.toString());
	}

	// private members
	private JCheckBox showToolbar;
	private DefaultListModel listModel;
	private JList list;
	private JButton add;
	private JButton remove;
	private JButton moveUp, moveDown;

	private DefaultListModel actionsList;
	private DefaultComboBoxModel iconList;

	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1 && listModel.getSize() != 0);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
	}

	static class Button
	{
		String actionName;
		String iconName;
		Icon icon;
		String label;

		Button(String actionName, String iconName, Icon icon, String label)
		{
			this.actionName = actionName;
			this.iconName = iconName;
			this.icon = icon;
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

	static class IconListEntry
	{
		Icon icon;
		String name;

		IconListEntry(Icon icon, String name)
		{
			this.icon = icon;
			this.name = name;
		}
	}

	static class ButtonCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			Button button = (Button)value;
			setText(button.label);
			setIcon(button.icon);

			return this;
		}
	}

	static class IconCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			IconListEntry icon = (IconListEntry)value;
			setText(icon.name);
			setIcon(icon.icon);

			return this;
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == add)
			{
				ToolBarAddDialog dialog = new ToolBarAddDialog(
					ToolBarOptionPane.this,
					actionsList,iconList);
				Button selection = dialog.getSelection();
				if(selection == null)
					return;

				int index = list.getSelectedIndex();
				if(index == -1)
					index = listModel.getSize();
				else
					index++;

				listModel.insertElementAt(selection,index);
				list.setSelectedIndex(index);
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

class ToolBarAddDialog extends EnhancedDialog
{
	public ToolBarAddDialog(Component comp, ListModel actionsListModel,
		ComboBoxModel iconListModel)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("options.toolbar.add.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("options.toolbar.add.caption")));

		JPanel mainPanel = new JPanel(new BorderLayout(0,6));
		JPanel radioPanel = new JPanel(new GridLayout(2,1));

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		// Add separator
		separator = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.separator"));
		separator.setSelected(true);
		separator.addActionListener(actionHandler);
		grp.add(separator);
		radioPanel.add(separator);

		// Add action
		action = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		radioPanel.add(action);

		mainPanel.add(BorderLayout.NORTH,radioPanel);

		actionsList = new JList(actionsListModel);
		actionsList.setVisibleRowCount(8);

		JPanel centerPanel = new JPanel(new BorderLayout(0,3));
		centerPanel.add(BorderLayout.NORTH,new JScrollPane(actionsList));

		// Add macro
		macro = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.macro"));
		macro.addActionListener(actionHandler);
		grp.add(macro);
		centerPanel.add(BorderLayout.CENTER,macro);

		macrosList = new JList(Macros.getMacroList());
		macrosList.setVisibleRowCount(8);
		centerPanel.add(BorderLayout.SOUTH,new JScrollPane(macrosList));

		mainPanel.add(BorderLayout.CENTER,centerPanel);

		// Icon selection
		JPanel iconPanel = new JPanel(new BorderLayout(0,3));
		JPanel labelPanel = new JPanel(new GridLayout(2,1));
		labelPanel.setBorder(new EmptyBorder(0,0,0,12));
		JPanel compPanel = new JPanel(new GridLayout(2,1));
		grp = new ButtonGroup();
		labelPanel.add(builtin = new JRadioButton(jEdit.getProperty(
			"options.toolbar.add.builtin")));
		builtin.addActionListener(actionHandler);
		builtin.setSelected(true);
		grp.add(builtin);
		labelPanel.add(file = new JRadioButton(jEdit.getProperty(
			"options.toolbar.add.file")));
		grp.add(file);
		file.addActionListener(actionHandler);
		iconPanel.add(BorderLayout.WEST,labelPanel);
		builtinCombo = new JComboBox(iconListModel);
		builtinCombo.setRenderer(new ToolBarOptionPane.IconCellRenderer());
		compPanel.add(builtinCombo);

		fileButton = new JButton(jEdit.getProperty("options.toolbar.add.no-icon"));
		fileButton.setMargin(new Insets(1,1,1,1));
		fileButton.setIcon(GUIUtilities.loadIcon("Blank24.gif"));
		fileButton.setHorizontalAlignment(SwingConstants.LEFT);
		fileButton.addActionListener(actionHandler);
		compPanel.add(fileButton);
		iconPanel.add(BorderLayout.CENTER,compPanel);
		mainPanel.add(BorderLayout.SOUTH,iconPanel);

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

		updateEnabled();

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

	public ToolBarOptionPane.Button getSelection()
	{
		if(!isOK)
			return null;

		if(separator.isSelected())
			return new ToolBarOptionPane.Button("-",null,null,"-");
		else
		{
			Icon icon;
			String iconName;
			if(builtin.isSelected())
			{
				ToolBarOptionPane.IconListEntry selectedIcon =
					(ToolBarOptionPane.IconListEntry)
					builtinCombo.getSelectedItem();
				icon = selectedIcon.icon;
				iconName = selectedIcon.name;
			}
			else
			{
				icon = fileButton.getIcon();
				iconName = fileIcon;
				if(iconName == null)
					iconName = "Blank24.gif";
			}

			String label;
			String actionName;
			if(action.isSelected())
			{
				ToolBarOptionPane.Button button =
					(ToolBarOptionPane.Button)actionsList
					.getSelectedValue();
				label = button.label;
				actionName = button.actionName;
			}
			else if(macro.isSelected())
			{
				actionName = "play-macro@" + macrosList.getSelectedValue();
				label = ContextOptionPane.getActionLabel(actionName);
			}
			else
				throw new InternalError();

			return new ToolBarOptionPane.Button(actionName,
				iconName,icon,label);
		}
	}

	// private members
	private boolean isOK;
	private JRadioButton separator, action, macro;
	private JList actionsList, macrosList;
	private JRadioButton builtin;
	private JComboBox builtinCombo;
	private JRadioButton file;
	private JButton fileButton;
	private String fileIcon;
	private JButton ok, cancel;

	private void updateEnabled()
	{
		actionsList.setEnabled(action.isSelected());
		macrosList.setEnabled(macro.isSelected());

		boolean iconControlsEnabled = !separator.isSelected();
		builtin.setEnabled(iconControlsEnabled);
		file.setEnabled(iconControlsEnabled);
		builtinCombo.setEnabled(iconControlsEnabled && builtin.isSelected());
		fileButton.setEnabled(iconControlsEnabled && file.isSelected());
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source instanceof JRadioButton)
				updateEnabled();
			else if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == builtin || source == file)
				updateEnabled();
			else if(source == fileButton)
			{
				String directory;
				if(fileIcon == null)
					directory = null;
				else
					directory = MiscUtilities.getParentOfPath(fileIcon);
				String paths[] = GUIUtilities.showVFSFileDialog(null,directory,
					VFSBrowser.OPEN_DIALOG,false);
				if(paths == null)
					return;

				fileIcon = "file:" + paths[0];

				try
				{
					fileButton.setIcon(new ImageIcon(new URL(
						fileIcon)));
				}
				catch(MalformedURLException mf)
				{
					Log.log(Log.ERROR,this,mf);
				}
				fileButton.setText(MiscUtilities.getFileName(fileIcon));
			}
		}
	}
}
