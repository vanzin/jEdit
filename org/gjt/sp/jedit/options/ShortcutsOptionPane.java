/*
 * ShortcutsOptionPane.java - Shortcuts options panel
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.gui.GrabKeyDialog;
import org.gjt.sp.jedit.*;

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
		allBindings = new Vector();

		setLayout(new BorderLayout(12,12));

		initModels();

		selectModel = new JComboBox(models);
		selectModel.addActionListener(new ActionHandler());

		Box north = Box.createHorizontalBox();
		north.add(new JLabel(jEdit.getProperty(
			"options.shortcuts.select.label")));
		north.add(Box.createHorizontalStrut(12));
		north.add(selectModel);

		keyTable = new JTable(currentModel);
		keyTable.getTableHeader().setReorderingAllowed(false);
		keyTable.getTableHeader().addMouseListener(new HeaderMouseHandler());
		keyTable.addMouseListener(new TableMouseHandler());
		Dimension d = keyTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(keyTable);
		scroller.setPreferredSize(d);

		add(BorderLayout.NORTH,north);
		add(BorderLayout.CENTER,scroller);
	}

	protected void _save()
	{
		if(keyTable.getCellEditor() != null)
			keyTable.getCellEditor().stopCellEditing();

		Enumeration e = models.elements();
		while(e.hasMoreElements())
			((ShortcutsModel)e.nextElement()).save();

		Macros.loadMacros();
	}

	private void initModels()
	{
		models = new Vector();
		models.addElement(currentModel = createModel("commands",false));
		models.addElement(createModel("plugins",true));
		models.addElement(createMacrosModel());
	}

	private ShortcutsModel createModel(String id, boolean pluginActions)
	{
		EditAction[] actions = jEdit.getActions();
		Vector bindings = new Vector(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			EditAction action = actions[i];
			if(action.isPluginAction() != pluginActions)
				continue;

			String name = action.getName();
			String label = jEdit.getProperty(name + ".label");
			// Skip certain actions this way (ENTER, TAB)
			if(label == null)
				continue;

			label = GUIUtilities.prettifyMenuLabel(label);
			addBindings(name,label,bindings);
		}

		return new ShortcutsModel(id,bindings);
	}

	private ShortcutsModel createMacrosModel()
	{
		Vector bindings = new Vector();
		Vector macroList = Macros.getMacroList();

		for(int i = 0; i < macroList.size(); i++)
		{
			String name = macroList.elementAt(i).toString();
			addBindings(name,name,bindings);
		}

		return new ShortcutsModel("macros",bindings);
	}

	private void addBindings(String name, String label, Vector bindings)
	{
		GrabKeyDialog.KeyBinding b[] = new GrabKeyDialog.KeyBinding[2];

		b[0] = createBinding(name,label,
			jEdit.getProperty(name + ".shortcut"));
		b[1] = createBinding(name,label,
			jEdit.getProperty(name + ".shortcut2"));

		bindings.addElement(b);
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
	private Vector models;
	private ShortcutsModel currentModel;
	private JComboBox selectModel;
	private Vector allBindings;

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
					ShortcutsOptionPane.this,
					currentModel.getBindingAt(row,col-1),
					allBindings);
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
				currentModel = newModel;
				keyTable.setModel(currentModel);
			}
		}
	}

	class ShortcutsModel extends AbstractTableModel
	{
		private Vector bindings;
		private String name;

		ShortcutsModel(String name, Vector bindings)
		{
			this.name = name;
			this.bindings = bindings;
			sort(0);
		}

		public void sort(int col)
		{
			MiscUtilities.quicksort(bindings,new KeyCompare(col));
			fireTableDataChanged();
		}

		public int getColumnCount()
		{
			return 3;
		}

		public int getRowCount()
		{
			return bindings.size();
		}

		public Object getValueAt(int row, int col)
		{
			switch(col)
			{
			case 0:
				return getBindingAt(row,0).label;
			case 1:
				return getBindingAt(row,0).shortcut;
			case 2:
				return getBindingAt(row,1).shortcut;
			default:
				return null;
			}
		}

		public void setValueAt(Object value, int row, int col)
		{
			if(col == 0)
				return;

			getBindingAt(row,col-1).shortcut = (String)value;

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
			Enumeration enum = bindings.elements();
			while(enum.hasMoreElements())
			{
				GrabKeyDialog.KeyBinding binding[]
					= (GrabKeyDialog.KeyBinding[])
						enum.nextElement();
				jEdit.setProperty(
					binding[0].name + ".shortcut",
					binding[0].shortcut);
				jEdit.setProperty(
					binding[1].name + ".shortcut2",
					binding[1].shortcut);
			}
		}

		public GrabKeyDialog.KeyBinding getBindingAt(int row, int nr)
		{
			GrabKeyDialog.KeyBinding binding[]
				= (GrabKeyDialog.KeyBinding[])
					bindings.elementAt(row);
			return binding[nr];
		}

		public String toString()
		{
			return jEdit.getProperty(
				"options.shortcuts.select." + name);
		}

		class KeyCompare implements MiscUtilities.Compare
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
					return label1.compareTo(label2);
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
					else if(shortcut1 == null && shortcut2 == null)
						return label1.compareTo(label2);
					else
						return shortcut1.compareTo(shortcut2);
				}
			}
		}
	}
}
