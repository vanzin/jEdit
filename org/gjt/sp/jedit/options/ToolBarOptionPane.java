/*
 * ToolBarOptionPane.java - Tool bar options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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

//{{{ Imports
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
//}}}

//{{{ ToolBarOptionPane class
/**
 * Tool bar editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ToolBarOptionPane extends AbstractOptionPane
{
	//{{{ ToolBarOptionPane constructor
	public ToolBarOptionPane()
	{
		super("toolbar");
	} //}}}

	//{{{ _init() method
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

		listModel = new DefaultListModel();
		reloadButtonList(jEdit.getProperty("view.toolbar"));
		
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());
		list.setCellRenderer(new ButtonCellRenderer());

		add(BorderLayout.CENTER,new JScrollPane(list));

		//{{{ Create buttons
		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		ActionHandler actionHandler = new ActionHandler();
		add = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.add.icon")));
		add.setToolTipText(jEdit.getProperty("options.toolbar.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(Box.createHorizontalStrut(6));
		remove = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.remove.icon")));
		remove.setToolTipText(jEdit.getProperty("options.toolbar.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.moveUp.icon")));
		moveUp.setToolTipText(jEdit.getProperty("options.toolbar.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.moveDown.icon")));
		moveDown.setToolTipText(jEdit.getProperty("options.toolbar.moveDown"));
		moveDown.addActionListener(actionHandler);
		buttons.add(moveDown);
		buttons.add(Box.createHorizontalStrut(6));
		edit = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.edit.icon")));
		edit.setToolTipText(jEdit.getProperty("options.toolbar.edit"));
		edit.addActionListener(actionHandler);
		buttons.add(edit);
		buttons.add(Box.createGlue());
		
		// add "reset to defaults" button
		reset = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.toolbar.reset.icon")));
		reset.setToolTipText(jEdit.getProperty("options.toolbar.reset"));
		reset.addActionListener(actionHandler);
		buttons.add(reset);
		//}}}

		updateButtons();
		add(BorderLayout.SOUTH,buttons);

		//{{{ Ceate icons list
		iconList = new DefaultComboBoxModel();
		StringTokenizer st = new StringTokenizer(jEdit.getProperty("icons"));
		while(st.hasMoreElements())
		{
			String icon = st.nextToken();
			iconList.addElement(new IconListEntry(
				GUIUtilities.loadIcon(icon),icon));
		} //}}}
	} ///}}}

	//{{{ _save() method
	protected void _save()
	{
		jEdit.setBooleanProperty("view.showToolbar",showToolbar
			.isSelected());

		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			Button button = (Button)listModel.elementAt(i);
			buf.append(button.actionName);
			jEdit.setProperty(button.actionName + ".icon",button.iconName);
		}
		jEdit.setProperty("view.toolbar",buf.toString());
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JCheckBox showToolbar;
	private DefaultListModel listModel;
	private JList list;
	private RolloverButton add;
	private RolloverButton remove;
	private RolloverButton moveUp, moveDown;
	private RolloverButton edit;
	private RolloverButton reset;

	private DefaultComboBoxModel iconList;
	//}}}

	//{{{ updateButtons() method
	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1 && listModel.getSize() != 0);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
		edit.setEnabled(index != -1);
	} //}}}
	
	//{{{ reloadButtonList() method
	private void reloadButtonList(String toolbar)
	{
		StringTokenizer st = new StringTokenizer(toolbar);
		listModel.clear();
		
		while(st.hasMoreTokens())
		{
			String actionName = st.nextToken();
			if(actionName.equals("-"))
				listModel.addElement(new ToolBarOptionPane.Button("-",null,null,"-"));
			else
			{
				EditAction action = jEdit.getAction(actionName);
				if(action == null)
					continue;
				String label = action.getLabel();
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
						icon = GUIUtilities.loadIcon(jEdit.getProperty("broken-image.icon"));
					else
					{
						icon = GUIUtilities.loadIcon(iconName);
						if(icon == null)
							icon = GUIUtilities.loadIcon(jEdit.getProperty("broken-image.icon"));
					}
				}
				listModel.addElement(new Button(actionName,iconName,icon,label));
			}
		}
	}
	//}}}
	
	//}}}

	//{{{ Inner classes

	//{{{ ButtonCompare class
	static class ButtonCompare implements Comparator<Button>
	{
		public int compare(Button button1, Button button2)
		{
			return StandardUtilities.compareStrings(
				button1.label,
				button2.label,
				true);
		}
	} //}}}

	//{{{ Button class
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
			this.label = GUIUtilities.prettifyMenuLabel(label);
		}

		public String toString()
		{
			return label;
		}

		public boolean equals(Object o)
		{
			if(o instanceof Button)
				return ((Button)o).actionName.equals(actionName);
			else
				return false;
		}
	} //}}}

	//{{{ IconListEntry class
	static class IconListEntry
	{
		Icon icon;
		String name;

		IconListEntry(Icon icon, String name)
		{
			this.icon = icon;
			this.name = name;
		}

		public String toString()
		{
			return name;
		}
	} //}}}

	//{{{ ButtonCellRenderer class
	static class ButtonCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			Button button = (Button)value;
			setIcon(button.icon);

			return this;
		}
	} //}}}

	//{{{ IconCellRenderer class
	static class IconCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			IconListEntry icon = (IconListEntry)value;
			setIcon(icon.icon);

			return this;
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == add)
			{
				ToolBarEditDialog dialog = new ToolBarEditDialog(
					ToolBarOptionPane.this,iconList,null);
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
				list.ensureIndexIsVisible(index);
			}
			else if(source == remove)
			{
				int index = list.getSelectedIndex();
				listModel.removeElementAt(index);
				if(listModel.getSize() != 0)
				{
					if(listModel.getSize() == index)
						list.setSelectedIndex(index-1);
					else
						list.setSelectedIndex(index);
				}
				updateButtons();
			}
			else if(source == moveUp)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index-1);
				list.setSelectedIndex(index-1);
				list.ensureIndexIsVisible(index-1);
			}
			else if(source == moveDown)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index+1);
				list.setSelectedIndex(index+1);
				list.ensureIndexIsVisible(index+1);
			}
			else if(source == edit)
			{
				ToolBarEditDialog dialog = new ToolBarEditDialog(
					ToolBarOptionPane.this,iconList,
					(Button)list.getSelectedValue());
				Button selection = dialog.getSelection();
				if(selection == null)
					return;

				int index = list.getSelectedIndex();

				listModel.setElementAt(selection,index);
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			}
			else if(source == reset)
			{
				String dialogType = "options.toolbar.reset.dialog";
				int result = GUIUtilities.confirm(list,dialogType,null,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				
				if(result == JOptionPane.YES_OPTION)
				{
					// the user should be able to cancel the options dialog 
					// so we need to modify the list, not the actual property
					// since the default value is not available, 
					// we reset, fetch default value and re-set to original
					String orgToolbar = jEdit.getProperty("view.toolbar");
					jEdit.resetProperty("view.toolbar");
					String defaultToolbar = jEdit.getProperty("view.toolbar");
					jEdit.setProperty("view.toolbar", orgToolbar);
					reloadButtonList(defaultToolbar);
					
					// reset selection if user had more buttons than default
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					updateButtons();
				}
			}
		}
	} //}}}

	//{{{ ListHandler class
	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			updateButtons();
		}
	} //}}}

	//}}}
} //}}}

//{{{ ToolBarEditDialog class
class ToolBarEditDialog extends EnhancedDialog
{
	//{{{ ToolBarEditDialog constructor
	public ToolBarEditDialog(Component comp,
		DefaultComboBoxModel iconListModel,
		ToolBarOptionPane.Button current)
	{
		super(GUIUtilities.getParentDialog(comp),
			jEdit.getProperty("options.toolbar.edit.title"),
			true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		JPanel typePanel = new JPanel(new GridLayout(3,1,6,6));
		typePanel.setBorder(new EmptyBorder(0,0,6,0));
		typePanel.add(new JLabel(
			jEdit.getProperty("options.toolbar.edit.caption")));

		separator = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".edit.separator"));
		separator.addActionListener(actionHandler);
		grp.add(separator);
		typePanel.add(separator);

		action = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".edit.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		typePanel.add(action);

		content.add(BorderLayout.NORTH,typePanel);

		JPanel actionPanel = new JPanel(new BorderLayout(6,6));

		ActionSet[] actionsList = jEdit.getActionSets();
		String selectedActionSet = jEdit.getProperty("options.toolbar.selectedActionSet");
		ActionSet selectedItem = null;
		Vector<ActionSet> vec = new Vector<ActionSet>(actionsList.length);
		for(int i = 0; i < actionsList.length; i++)
		{
			ActionSet actionSet = actionsList[i];
			if(actionSet.getActionCount() != 0)
			{
				vec.add(actionSet);
				if (actionSet.getLabel().equals(selectedActionSet))
				{
					selectedItem = actionSet;
				}
			}
		}
		combo = new JComboBox(vec);
		if (selectedItem != null)
			combo.setSelectedItem(selectedItem);
		else
			jEdit.unsetProperty("options.toolbar.selectedActionSet");
		combo.addActionListener(actionHandler);

		actionPanel.add(BorderLayout.NORTH,combo);

		list = new JList();
		list.setVisibleRowCount(8);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		actionPanel.add(BorderLayout.CENTER,new JScrollPane(list));

		// Icon selection
		JPanel iconPanel = new JPanel(new BorderLayout(0,3));
		JPanel labelPanel = new JPanel(new GridLayout(2,1));
		labelPanel.setBorder(new EmptyBorder(0,0,0,12));
		JPanel compPanel = new JPanel(new GridLayout(2,1));
		grp = new ButtonGroup();
		labelPanel.add(builtin = new JRadioButton(jEdit.getProperty(
			"options.toolbar.edit.builtin")));
		builtin.addActionListener(actionHandler);
		grp.add(builtin);
		labelPanel.add(file = new JRadioButton(jEdit.getProperty(
			"options.toolbar.edit.file")));
		grp.add(file);
		file.addActionListener(actionHandler);
		iconPanel.add(BorderLayout.WEST,labelPanel);
		builtinCombo = new JComboBox(iconListModel);
		builtinCombo.setRenderer(new ToolBarOptionPane.IconCellRenderer());
		compPanel.add(builtinCombo);

		fileButton = new JButton(jEdit.getProperty("options.toolbar.edit.no-icon"));
		fileButton.setMargin(new Insets(1,1,1,1));
		fileButton.setIcon(GUIUtilities.loadIcon("Blank24.gif"));
		fileButton.setHorizontalAlignment(SwingConstants.LEFT);
		fileButton.addActionListener(actionHandler);
		compPanel.add(fileButton);
		iconPanel.add(BorderLayout.CENTER,compPanel);
		actionPanel.add(BorderLayout.SOUTH,iconPanel);

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

		if(current == null)
		{
			action.setSelected(true);
			builtin.setSelected(true);
			updateList();
		}
		else
		{
			if(current.actionName.equals("-"))
			{
				separator.setSelected(true);
				builtin.setSelected(true);
			}
			else
			{
				action.setSelected(true);
				ActionSet set = jEdit.getActionSetForAction(
					current.actionName);
				combo.setSelectedItem(set);
				updateList();
				list.setSelectedValue(current,true);

				if(MiscUtilities.isURL(current.iconName))
				{
					file.setSelected(true);
					fileIcon = current.iconName;
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
				else
				{
					String iconName = MiscUtilities.getFileName(current.iconName);
					builtin.setSelected(true);
					ListModel model = builtinCombo.getModel();
					for(int i = 0; i < model.getSize(); i++)
					{
						ToolBarOptionPane.IconListEntry entry
							= (ToolBarOptionPane.IconListEntry)
							model.getElementAt(i);
						if(entry.name.equals(iconName))
						{
							builtinCombo.setSelectedIndex(i);
							break;
						}
					}
				}
			}
		}

		updateEnabled();

		pack();
		setLocationRelativeTo(GUIUtilities.getParentDialog(comp));
		setVisible(true);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		isOK = true;
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getSelection() method
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
					(ToolBarOptionPane.Button)list
					.getSelectedValue();
				label = button.label;
				actionName = button.actionName;
			}
			else
				throw new InternalError();

			return new ToolBarOptionPane.Button(actionName,
				iconName,icon,label);
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private boolean isOK;
	private JRadioButton separator, action;
	private JComboBox combo;
	private JList list;
	private JRadioButton builtin;
	private JComboBox builtinCombo;
	private JRadioButton file;
	private JButton fileButton;
	private String fileIcon;
	private JButton ok, cancel;
	//}}}

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		combo.setEnabled(action.isSelected());
		list.setEnabled(action.isSelected());

		boolean iconControlsEnabled = !separator.isSelected();
		builtin.setEnabled(iconControlsEnabled);
		file.setEnabled(iconControlsEnabled);
		builtinCombo.setEnabled(iconControlsEnabled && builtin.isSelected());
		fileButton.setEnabled(iconControlsEnabled && file.isSelected());
	} //}}}

	//{{{ updateList() method
	private void updateList()
	{
		ActionSet actionSet = (ActionSet)combo.getSelectedItem();
		String actionSetLabel = actionSet.getLabel();
		jEdit.setProperty("options.toolbar.selectedActionSet", actionSetLabel);
		EditAction[] actions = actionSet.getActions();
		Vector<ToolBarOptionPane.Button> listModel = new Vector<ToolBarOptionPane.Button>(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			EditAction action = actions[i];
			String label = action.getLabel();
			if(label == null)
				continue;

			listModel.add(new ToolBarOptionPane.Button(
				action.getName(),null,null,label));
		}

		Collections.sort(listModel,new ToolBarOptionPane.ButtonCompare());
		list.setListData(listModel);
	} //}}}

	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source instanceof JRadioButton)
				updateEnabled();
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == combo)
				updateList();
			else if(source == fileButton)
			{
				String directory;
				if(fileIcon == null)
					directory = null;
				else
					directory = MiscUtilities.getParentOfPath(fileIcon);
				String[] paths = GUIUtilities.showVFSFileDialog(null,directory,
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
	} //}}}
} //}}}
