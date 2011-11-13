/*
 * ShortcutsOptionPane.java - Shortcuts options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
 * Copyright (C) 2001 Dirk Moebius
 * Copyright (C) 2011 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.gjt.sp.jedit.options;

//{{{ Imports
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.FilteredTableModel;
import org.gjt.sp.jedit.gui.GrabKeyDialog;
import org.gjt.sp.jedit.gui.GrabKeyDialog.KeyBinding;
import org.gjt.sp.jedit.keymap.Keymap;
import org.gjt.sp.jedit.keymap.KeymapManager;
import org.gjt.sp.util.ComboKeyListener;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
//}}}

/**
 * Key binding editor.
 * @author Slava Pestov
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ShortcutsOptionPane extends AbstractOptionPane
{
	//{{{ ShortcutsOptionPane constructor
	public ShortcutsOptionPane()
	{
		super("shortcuts");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		allBindings = new Vector<KeyBinding>();

		setLayout(new BorderLayout(12, 12));

		KeymapManager keymapManager = jEdit.getKeymapManager();

		String keymapName = jEdit.getProperty("keymap.current");
		selectedKeymap = keymapManager.getKeymap(keymapName);
		if (selectedKeymap == null)
			selectedKeymap = keymapManager.getKeymap(KeymapManager.DEFAULT_KEYMAP_NAME);
		initModels();

		duplicateKeymap = new JButton(jEdit.getProperty("options.shortcuts.duplicatekeymap.label"));
		resetKeymap = new JButton(jEdit.getProperty("options.shortcuts.resetkeymap.label"));
		deleteKeymap = new JButton(jEdit.getProperty("options.shortcuts.deletekeymap.label"));
		resetButtons();

		ActionListener actionHandler = new ActionHandler();

		ComboBoxModel model = new KeymapsModel();
		keymaps = new JComboBox(model);
		keymaps.addKeyListener(new ComboKeyListener(keymaps));
		keymaps.setRenderer(new KeymapCellRenderer());
		keymaps.setSelectedItem(keymapName);
		duplicateKeymap.addActionListener(actionHandler);
		resetKeymap.addActionListener(actionHandler);
		deleteKeymap.addActionListener(actionHandler);
		keymaps.addActionListener(actionHandler);
		keymaps.setSelectedItem(selectedKeymap);

		Box keymapBox = Box.createHorizontalBox();
		keymapBox.add(new JLabel(jEdit.getProperty(
			"options.shortcuts.keymap.label")));
		keymapBox.add(Box.createHorizontalStrut(6));
		keymapBox.add(keymaps);
		keymapBox.add(Box.createHorizontalStrut(6));
		keymapBox.add(duplicateKeymap);
		keymapBox.add(resetKeymap);
		keymapBox.add(deleteKeymap);
		keymapBox.add(Box.createHorizontalGlue());

		// combobox to choose action set
		selectModel = new JComboBox(models);
		selectModel.addKeyListener(new ComboKeyListener(selectModel));
		selectModel.addActionListener(actionHandler);
		selectModel.setToolTipText(jEdit.getProperty("options.shortcuts.select.tooltip"));
		Box north = Box.createHorizontalBox();
		north.add(new JLabel(jEdit.getProperty(
			"options.shortcuts.select.label")));
		north.add(Box.createHorizontalStrut(6));
		north.add(selectModel);

		filterTF = new JTextField(40);
		filterTF.setToolTipText(jEdit.getProperty("options.shortcuts.filter.tooltip"));
		filterTF.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				setFilter();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				setFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				setFilter();
			}
		});
		JButton clearButton = new JButton(jEdit.getProperty(
				"options.shortcuts.clear.label"));
		clearButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				filterTF.setText("");
				filterTF.requestFocus();
			}
		});

		JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filterPanel.add(new JLabel(jEdit.getProperty("options.shortcuts.filter.label")));
		filterPanel.add(filterTF);
		filterPanel.add(clearButton);

		keyTable = new JTable(filteredModel);
		filteredModel.setTable(keyTable);
		keyTable.getTableHeader().setReorderingAllowed(false);
		keyTable.getTableHeader().addMouseListener(new HeaderMouseHandler());
		keyTable.addMouseListener(new TableMouseHandler());
		Dimension d = keyTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(keyTable);
		scroller.setPreferredSize(d);
		JPanel tableFilterPanel = new JPanel(new BorderLayout());
		tableFilterPanel.add(BorderLayout.NORTH,filterPanel);
		tableFilterPanel.add(BorderLayout.CENTER,scroller);

		Box northBox = Box.createVerticalBox();
		northBox.add(keymapBox);
		northBox.add(Box.createVerticalGlue());
		northBox.add(north);

		add(BorderLayout.NORTH,northBox);
		add(BorderLayout.CENTER,tableFilterPanel);
		try
		{
			selectModel.setSelectedIndex(jEdit.getIntegerProperty("options.shortcuts.select.index", 0));
		}
		catch (IllegalArgumentException eae) {}
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setProperty("keymap.current", selectedKeymap.toString());
		if(keyTable.getCellEditor() != null)
			keyTable.getCellEditor().stopCellEditing();

		for (ShortcutsModel model : models)
			model.save();

		Macros.loadMacros();
		selectedKeymap.save();
	} //}}}

	//{{{ Private members
	/** The selected keymap. It is a copy of the current keymap in the beginning, but it may be another one */
	private Keymap selectedKeymap;
	private JButton duplicateKeymap;
	private JButton resetKeymap;
	private JButton deleteKeymap;
	private JComboBox keymaps;
	private JTable keyTable;
	private Vector<ShortcutsModel> models;
	private FilteredTableModel<ShortcutsModel> filteredModel;
	private JComboBox selectModel;
	private List<KeyBinding> allBindings;
	private JTextField filterTF;

	//{{{ setFilter() method
	private void setFilter()
	{
		filteredModel.setFilter(filterTF.getText());
	} //}}}

	//{{{ initModels() method
	private void initModels()
	{
		filteredModel = new FilteredTableModel<ShortcutsModel>()
		{
			@Override
			public String prepareFilter(String filter)
			{
				return filter.toLowerCase();
			}

			@Override
			public boolean passFilter(int row, String filter)
			{
				String name = delegated.getBindingAt(row, 0).label.toLowerCase();
				return name.contains(filter);
			}
		};
		models = new Vector<ShortcutsModel>();
		reloadModels();
	} //}}}

	//{{{ reloadModels() method
	private void reloadModels()
	{
		models.clear();
		List<KeyBinding[]> allBindings = new ArrayList<KeyBinding[]>();
		Collection<String> knownBindings = new HashSet<String>();
		ActionSet[] actionSets = jEdit.getActionSets();
		for(int i = 0; i < actionSets.length; i++)
		{
			ActionSet actionSet = actionSets[i];
			if(actionSet.getActionCount() != 0)
			{
				String modelLabel = actionSet.getLabel();
				if(modelLabel == null)
				{
					Log.log(Log.ERROR,this,"Empty action set: "
						+ actionSet.getPluginJAR());
				}
				ShortcutsModel model = createModel(modelLabel,
						actionSet.getActionNames());
				models.add(model);
				List<KeyBinding[]> bindings = model.getBindings();
				for (KeyBinding[] binding : bindings)
				{
					String name = binding[0].name;
					if (!knownBindings.contains(name))
					{
						knownBindings.add(name);
						allBindings.add(binding);
					}
				}
			}
		}
		if (models.size() > 1)
			models.add(new ShortcutsModel("All", allBindings));
		ShortcutsModel delegated = filteredModel.getDelegated();
		Collections.sort(models,new StandardUtilities.StringCompare<ShortcutsModel>(true));
		if (delegated == null)
		{
			delegated = models.get(0);
		}
		else
		{
			for (ShortcutsModel model : models)
			{
				// Find the model with the same name
				if (model.toString().equals(delegated.toString()))
				{
					delegated = model;
					break;
				}
			}
		}
		filteredModel.setDelegated(delegated);
		filteredModel.fireTableDataChanged();
	} //}}}

	//{{{ createModel() method
	private ShortcutsModel createModel(String modelLabel, String[] actions)
	{
		List<GrabKeyDialog.KeyBinding[]> bindings = new ArrayList<GrabKeyDialog.KeyBinding[]>(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			String name = actions[i];
			EditAction ea = jEdit.getAction(name);
			String label = ea.getLabel();
			// Skip certain actions this way
			if(label == null)
				continue;

			label = GUIUtilities.prettifyMenuLabel(label);
			addBindings(name,label,bindings);
		}

		return new ShortcutsModel(modelLabel,bindings);
	} //}}}

	//{{{ addBindings() method
	private void addBindings(String name, String label, Collection<KeyBinding[]> bindings)
	{
		GrabKeyDialog.KeyBinding[] b = new GrabKeyDialog.KeyBinding[2];

		b[0] = createBinding(name,label,
			selectedKeymap.getShortcut(name + ".shortcut"));
		b[1] = createBinding(name,label,
			selectedKeymap.getShortcut(name + ".shortcut2"));

		bindings.add(b);
	} //}}}

	//{{{ createBinding() method
	private GrabKeyDialog.KeyBinding createBinding(String name,
		String label, String shortcut)
	{
		if(shortcut != null && shortcut.length() == 0)
			shortcut = null;

		GrabKeyDialog.KeyBinding binding
			= new GrabKeyDialog.KeyBinding(name,label,shortcut,false);

		allBindings.add(binding);
		return binding;
	} //}}}

	// {{{ resetButtons() methods
	private void resetButtons()
	{
		KeymapManager keymapManager = jEdit.getKeymapManager();
		KeymapManager.State state = keymapManager.getKeymapState(selectedKeymap.toString());
		resetKeymap.setEnabled(state == KeymapManager.State.SystemModified);
		deleteKeymap.setEnabled(state == KeymapManager.State.User);
	} //}}}

	//{{{ Inner classes

		
	//{{{ HeaderMouseHandler class
	private class HeaderMouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			ShortcutsModel shortcutsModel = filteredModel.getDelegated();
			switch(keyTable.getTableHeader().columnAtPoint(evt.getPoint()))
			{
			case 0:
				shortcutsModel.sort(0);
				break;
			case 1:
				shortcutsModel.sort(1);
				break;
			case 2:
				shortcutsModel.sort(2);
				break;
			}
			setFilter();
		}
	} //}}}

	//{{{ TableMouseHandler class
	private class TableMouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			int row = keyTable.getSelectedRow();
			int col = keyTable.getSelectedColumn();
			if(col != 0 && row != -1)
			{
				 GrabKeyDialog gkd = new GrabKeyDialog(
					GUIUtilities.getParentDialog(
					ShortcutsOptionPane.this),
					filteredModel.getDelegated().getBindingAt(filteredModel.getTrueRow(row), col - 1),
					allBindings,null);
				if(gkd.isOK())
					filteredModel.setValueAt(
						gkd.getShortcut(),row,col);
			}
		}
	} //}}}

	//{{{ ActionHandler class
	private class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			if (evt.getSource() == selectModel)
			{
				ShortcutsModel newModel
					= (ShortcutsModel)selectModel.getSelectedItem();
				if(filteredModel.getDelegated() != newModel)
				{
					jEdit.setIntegerProperty("options.shortcuts.select.index", selectModel.getSelectedIndex());
					filteredModel.setDelegated(newModel);
					setFilter();
				}
			}
			else if (evt.getSource() == keymaps)
			{
				String selectedKeymapName = (String) keymaps.getSelectedItem();
				KeymapManager keymapManager = jEdit.getKeymapManager();
				selectedKeymap = keymapManager.getKeymap(selectedKeymapName);
				resetButtons();
				reloadModels();
			}
			else if (evt.getSource() == duplicateKeymap)
			{
				String newName = JOptionPane.showInputDialog(ShortcutsOptionPane.this,
									     jEdit.getProperty(
										     "options.shortcuts.duplicatekeymap.dialog.label"),
									     jEdit.getProperty(
										     "options.shortcuts.duplicatekeymap.dialog.title"),
									     JOptionPane.OK_CANCEL_OPTION);
				KeymapManager manager = jEdit.getKeymapManager();
				Collection<String> keymapNames = manager.getKeymapNames();
				while (newName != null)
				{
					if (!keymapNames.contains(newName))
						break;
					newName = JOptionPane.showInputDialog(ShortcutsOptionPane.this,
										     jEdit.getProperty(
											     "options.shortcuts.keymapalreadyexists.dialog.label"),
										     jEdit.getProperty(
											     "options.shortcuts.duplicatekeymap.dialog.title"),
										     JOptionPane.OK_CANCEL_OPTION);

				}
				if (manager.copyKeymap(selectedKeymap.toString(), newName))
				{
					KeymapsModel model = (KeymapsModel) keymaps.getModel();
					model.reset();
					keymaps.setSelectedItem(newName);
				}
			}
			else if (evt.getSource() == resetKeymap)
			{
				int ret = JOptionPane.showConfirmDialog(ShortcutsOptionPane.this, jEdit.getProperty(
					"options.shortcuts.resetkeymap.dialog.label"), jEdit.getProperty(
					"options.shortcuts.resetkeymap.dialog.title"), JOptionPane.YES_NO_OPTION,
								      JOptionPane.QUESTION_MESSAGE);
				if (ret == JOptionPane.YES_OPTION)
				{
					String name = selectedKeymap.toString();
					KeymapManager manager = jEdit.getKeymapManager();
					manager.resetKeymap(name);
					selectedKeymap = manager.getKeymap(name);
					resetButtons();
					reloadModels();
				}
			}
			else if (evt.getSource() == deleteKeymap)
			{
				int ret = JOptionPane.showConfirmDialog(ShortcutsOptionPane.this, jEdit.getProperty(
					"options.shortcuts.deletekeymap.dialog.label"), jEdit.getProperty(
					"options.shortcuts.deletekeymap.dialog.title"), JOptionPane.YES_NO_OPTION,
								      JOptionPane.QUESTION_MESSAGE);
				if (ret == JOptionPane.YES_OPTION)
				{
					KeymapManager manager = jEdit.getKeymapManager();
					KeymapManager.State keymapState = manager.getKeymapState(selectedKeymap.toString());
					if (keymapState == KeymapManager.State.User)
					{
						manager.deleteUserKeymap(selectedKeymap.toString());
						KeymapsModel model = (KeymapsModel) keymaps.getModel();
						model.reset();
					}
				}
			}
		}
	} //}}}

	//{{{ ShortcutsModel class
	private class ShortcutsModel extends AbstractTableModel
	{
		private final List<GrabKeyDialog.KeyBinding[]> bindings;
		private final String name;

		ShortcutsModel(String name, List<GrabKeyDialog.KeyBinding[]> bindings)
		{
			this.name = name;
			this.bindings = bindings;
			sort(0);
		}

		public List<GrabKeyDialog.KeyBinding[]> getBindings()
		{
			return bindings;
		}

		public void sort(int col)
		{
			Collections.sort(bindings,new KeyCompare(col));
		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public int getRowCount()
		{
			return bindings.size();
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			// The only place this gets used is in JTable's own display code, so
			// we translate the shortcut to platform-specific form for display here.
			switch(col)
			{
			case 0:
				return getBindingAt(row,0).label;
			case 1:
				return GUIUtilities.getPlatformShortcutLabel(getBindingAt(row,0).shortcut);
			case 2:
				return GUIUtilities.getPlatformShortcutLabel(getBindingAt(row,1).shortcut);
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col)
		{
			if(col == 0)
				return;

			getBindingAt(row,col-1).shortcut = (String)value;

			// redraw the whole table because a second shortcut
			// might have changed, too
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int index)
		{
			switch(index)
			{
			case 0:
				return jEdit.getProperty("options.shortcuts.name");
			case 1:
				return selectedKeymap.getShortcut("options.shortcuts.shortcut1");
			case 2:
				return selectedKeymap.getShortcut("options.shortcuts.shortcut2");
			default:
				return null;
			}
		}

		public void save()
		{
			for (GrabKeyDialog.KeyBinding[] binding : bindings)
			{
				selectedKeymap.setShortcut(
					binding[0].name + ".shortcut",
					binding[0].shortcut);
				selectedKeymap.setShortcut(
					binding[1].name + ".shortcut2",
					binding[1].shortcut);
			}
		}

		public GrabKeyDialog.KeyBinding getBindingAt(int row, int nr)
		{
			GrabKeyDialog.KeyBinding[] binding = bindings.get(row);
			return binding[nr];
		}

		@Override
		public String toString()
		{
			return name;
		}

		private class KeyCompare implements Comparator<GrabKeyDialog.KeyBinding[]>
		{
			private final int col;

			KeyCompare(int col)
			{
				this.col = col;
			}

			@Override
			public int compare(GrabKeyDialog.KeyBinding[] k1, GrabKeyDialog.KeyBinding[] k2)
			{
				String label1 = k1[0].label.toLowerCase();
				String label2 = k2[0].label.toLowerCase();

				if(col == 0)
					return StandardUtilities.compareStrings(
						label1,label2,true);
				else
				{
					String shortcut1, shortcut2;
					if(col == 1)
					{
						shortcut1 = k1[0].shortcut;
						shortcut2 = k2[0].shortcut;
					}
					else
					{
						shortcut1 = k1[1].shortcut;
						shortcut2 = k2[1].shortcut;
					}

					if(shortcut1 == null && shortcut2 != null)
						return 1;
					else if(shortcut2 == null && shortcut1 != null)
						return -1;
					else if(shortcut1 == null)
						return StandardUtilities.compareStrings(label1,label2,true);
					else
						return StandardUtilities.compareStrings(shortcut1,shortcut2,true);
				}
			}
		}
	} //}}}

	//{{{ KeymapsModel class 
	private static class KeymapsModel extends AbstractListModel implements ComboBoxModel
	{
		private String[] keymaps;

		private Object selectedItem;

		//{{{ KeymapsModel() constructor
		private KeymapsModel()
		{
			reset();
		} //}}}

		//{{{ reset() method
		void reset()
		{
			KeymapManager keymapManager = jEdit.getKeymapManager();
			Collection<String> keymapNames = keymapManager.getKeymapNames();
			keymaps = keymapNames.toArray(new String[keymapNames.size()]);
			if (!isValidName(selectedItem))
				selectedItem = keymaps[0];
			fireContentsChanged(this, 0, keymaps.length-1);
		} //}}}

		//{{{ getSize() method
		@Override
		public int getSize()
		{
			return keymaps.length;
		} //}}}

		//{{{ getElementAt() method
		@Override
		public Object getElementAt(int index)
		{
			return keymaps[index];
		} //}}}

		//{{{ setSelectedItem() method
		@Override
		public void setSelectedItem(Object anItem)
		{
			if (isValidName(anItem))
				selectedItem = anItem;
			else
				selectedItem = keymaps[0];
		} //}}}

		//{{{ getSelectedItem() method
		@Override
		public Object getSelectedItem()
		{
			return selectedItem;
		} //}}}

		//{{{ isValidName() method
		private boolean isValidName(Object name)
		{
			for (String keymap : keymaps)
			{
				if (keymap.equals(name))
					return true;
			}
			return false;
		} //}}}
	} //}}}

	//{{{ KeymapsModel class 
	private static class KeymapCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index,
							      boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
								  cellHasFocus);
			String label = jEdit.getProperty("keymaps." + value + ".label", String.valueOf(value));
			setText(label);
			return this;
		}
	} //}}}
	
	//}}}
	//}}}
}
