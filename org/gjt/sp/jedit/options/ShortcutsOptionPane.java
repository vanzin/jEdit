/*
 * ShortcutsOptionPane.java - Shortcuts options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
 * Copyright (C) 2001 Dirk Moebius
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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import org.gjt.sp.jedit.gui.GrabKeyDialog;
import org.gjt.sp.jedit.gui.GrabKeyDialog.KeyBinding;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

/**
 * Key binding editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ShortcutsOptionPane extends AbstractOptionPane
{
	public ShortcutsOptionPane()
	{
		super("shortcuts");
	}

	// protected members
	protected void _init()
	{
		allBindings = new Vector<KeyBinding>();

		setLayout(new BorderLayout(12,12));

		initModels();

		selectModel = new JComboBox(models);
		selectModel.addActionListener(new ActionHandler());
		selectModel.setToolTipText(jEdit.getProperty("options.shortcuts.select.tooltip"));
		Box north = Box.createHorizontalBox();
		north.add(new JLabel(jEdit.getProperty(
			"options.shortcuts.select.label")));
		north.add(Box.createHorizontalStrut(6));
		north.add(selectModel);

		filterTF = new JTextField(40);
		filterTF.setToolTipText(jEdit.getProperty("options.shortcuts.filter.tooltip"));
		filterTF.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				setFilter();
			}
			public void insertUpdate(DocumentEvent e) {
				setFilter();
			}
			public void removeUpdate(DocumentEvent e) {
				setFilter();
			}
		});
		JButton clearButton = new JButton(jEdit.getProperty(
				"options.shortcuts.clear.label"));
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				filterTF.setText("");
				filterTF.requestFocus();
			}
		});

		JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filterPanel.add(new JLabel(jEdit.getProperty("options.shortcuts.filter.label")));
		filterPanel.add(filterTF);
		filterPanel.add(clearButton);

		keyTable = new JTable(currentModel);
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
		
		add(BorderLayout.NORTH,north);
		add(BorderLayout.CENTER,tableFilterPanel);
		try {
			selectModel.setSelectedIndex(jEdit.getIntegerProperty("options.shortcuts.select.index", 0));
		}
		catch (IllegalArgumentException eae) {}
	}

	private void setFilter() {
		currentModel.setFilter(filterTF.getText());
	}

	protected void _save()
	{
		if(keyTable.getCellEditor() != null)
			keyTable.getCellEditor().stopCellEditing();

		for (ShortcutsModel model : models)
			model.save();

		Macros.loadMacros();
	}

	private void initModels()
	{
		Vector<KeyBinding[]> allBindings = new Vector<KeyBinding[]>();
		models = new Vector<ShortcutsModel>();
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
				models.addElement(model);
				allBindings.addAll(model.getBindings());
			}
		}
		if (models.size() > 1)
			models.addElement(new ShortcutsModel("All", allBindings));
		Collections.sort(models,new MiscUtilities.StringICaseCompare());
		currentModel = models.elementAt(0);
	}

	private ShortcutsModel createModel(String modelLabel, String[] actions)
	{
		Vector<GrabKeyDialog.KeyBinding[]> bindings = new Vector<GrabKeyDialog.KeyBinding[]>(actions.length);

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
	}

	private void addBindings(String name, String label, List<GrabKeyDialog.KeyBinding[]> bindings)
	{
		GrabKeyDialog.KeyBinding[] b = new GrabKeyDialog.KeyBinding[2];

		b[0] = createBinding(name,label,
			jEdit.getProperty(name + ".shortcut"));
		b[1] = createBinding(name,label,
			jEdit.getProperty(name + ".shortcut2"));

		bindings.add(b);
	}

	private GrabKeyDialog.KeyBinding createBinding(String name,
		String label, String shortcut)
	{
		if(shortcut != null && shortcut.length() == 0)
			shortcut = null;

		GrabKeyDialog.KeyBinding binding
			= new GrabKeyDialog.KeyBinding(name,label,shortcut,false);

		allBindings.addElement(binding);
		return binding;
	}

	// private members
	private JTable keyTable;
	private Vector<ShortcutsModel> models;
	private ShortcutsModel currentModel;
	private JComboBox selectModel;
	private Vector<GrabKeyDialog.KeyBinding> allBindings;
	private JTextField filterTF;

	class HeaderMouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			switch(keyTable.getTableHeader().columnAtPoint(evt.getPoint()))
			{
			case 0:
				currentModel.sort(0);
				break;
			case 1:
				currentModel.sort(1);
				break;
			case 2:
				currentModel.sort(2);
				break;
			}
		}
	}

	class TableMouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			int row = keyTable.getSelectedRow();
			int col = keyTable.getSelectedColumn();
			if(col != 0 && row != -1)
			{
				 GrabKeyDialog gkd = new GrabKeyDialog(
					GUIUtilities.getParentDialog(
					ShortcutsOptionPane.this),
					currentModel.getFilteredBindingAt(row,col-1),
					allBindings,null);
				if(gkd.isOK())
					currentModel.setValueAt(
						gkd.getShortcut(),row,col);
			}
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			ShortcutsModel newModel
				= (ShortcutsModel)selectModel.getSelectedItem();
			if(currentModel != newModel)
			{
				jEdit.setIntegerProperty("options.shortcuts.select.index", selectModel.getSelectedIndex());
				currentModel = newModel;
				keyTable.setModel(currentModel);
			}
		}
	}

	class ShortcutsModel extends AbstractTableModel
	{
		private Vector<GrabKeyDialog.KeyBinding[]> bindings;
		private String name;
		private Vector<Integer> filteredIndices = null;
		
		ShortcutsModel(String name, Vector<GrabKeyDialog.KeyBinding[]> bindings)
		{
			this.name = name;
			this.bindings = bindings;
			sort(0);
			resetFilter();
		}

		private void resetFilter()
		{
			filteredIndices = new Vector<Integer>();
			for (int i = 0; i < bindings.size(); i++)
				filteredIndices.add(new Integer(i));
		}
		
		public void setFilter(String filter)
		{
			if (filter != null && filter.length() > 0)
			{
				filter = filter.toLowerCase();
				Vector<Integer> indices = new Vector<Integer>();
				for (int i = 0; i < bindings.size(); i++)
				{
					String name = getBindingAt(i, 0).label.toLowerCase();
					if (name.contains(filter))
						indices.add(new Integer(i));
				}
				filteredIndices = indices;
			}
			else
				resetFilter();
			fireTableDataChanged();
		}
		
		public Vector<GrabKeyDialog.KeyBinding[]> getBindings()
		{
			return bindings;
		}
		
		public void sort(int col)
		{
			Collections.sort(bindings,new KeyCompare(col));
			fireTableDataChanged();
		}

		public int getColumnCount()
		{
			return 3;
		}

		public int getRowCount()
		{
			return filteredIndices.size();
		}

		public Object getValueAt(int row, int col)
		{
			switch(col)
			{
			case 0:
				return getFilteredBindingAt(row,0).label;
			case 1:
				return getFilteredBindingAt(row,0).shortcut;
			case 2:
				return getFilteredBindingAt(row,1).shortcut;
			default:
				return null;
			}
		}

		public void setValueAt(Object value, int row, int col)
		{
			if(col == 0)
				return;

			getFilteredBindingAt(row,col-1).shortcut = (String)value;

			// redraw the whole table because a second shortcut
			// might have changed, too
			fireTableDataChanged();
		}

		public String getColumnName(int index)
		{
			switch(index)
			{
			case 0:
				return jEdit.getProperty("options.shortcuts.name");
			case 1:
				return jEdit.getProperty("options.shortcuts.shortcut1");
			case 2:
				return jEdit.getProperty("options.shortcuts.shortcut2");
			default:
				return null;
			}
		}

		public void save()
		{
			for (GrabKeyDialog.KeyBinding[] binding : bindings)
			{
				jEdit.setProperty(
					binding[0].name + ".shortcut",
					binding[0].shortcut);
				jEdit.setProperty(
					binding[1].name + ".shortcut2",
					binding[1].shortcut);
			}
		}

		public GrabKeyDialog.KeyBinding getFilteredBindingAt(int row, int nr)
		{
			row = filteredIndices.get(row).intValue();
			return getBindingAt(row, nr);
		}
		
		public GrabKeyDialog.KeyBinding getBindingAt(int row, int nr)
		{
			GrabKeyDialog.KeyBinding[] binding = bindings.elementAt(row);
			return binding[nr];
		}

		public String toString()
		{
			return name;
		}

		class KeyCompare implements Comparator
		{
			int col;

			KeyCompare(int col)
			{
				this.col = col;
			}

			public int compare(Object obj1, Object obj2)
			{
				GrabKeyDialog.KeyBinding[] k1
					= (GrabKeyDialog.KeyBinding[])obj1;
				GrabKeyDialog.KeyBinding[] k2
					= (GrabKeyDialog.KeyBinding[])obj2;

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
	}
}
