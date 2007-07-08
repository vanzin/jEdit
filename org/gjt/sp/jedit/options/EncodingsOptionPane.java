/*
 * EncodingsOptionPane.java - Encodings options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Björn Kautler
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities.StringICaseCompare;
import org.gjt.sp.jedit.gui.JCheckBoxList;
import org.gjt.sp.jedit.gui.JCheckBoxList.Entry;
import static java.util.Arrays.sort;
import static javax.swing.Box.createHorizontalBox;
import static org.gjt.sp.jedit.jEdit.getBooleanProperty;
import static org.gjt.sp.jedit.jEdit.getProperty;
import static org.gjt.sp.jedit.jEdit.setBooleanProperty;
import static org.gjt.sp.jedit.jEdit.unsetProperty;
import static org.gjt.sp.jedit.MiscUtilities.getEncodings;
//}}}

//{{{ EncodingsOptionPane class
/**
 * Encodings options.
 * 
 * @author Björn Kautler
 * @since jEdit 4.3pre6
 * @version $Id$
 */
public class EncodingsOptionPane extends AbstractOptionPane
{
	//{{{ Instance variables
	private JPanel optionsPanel;
	private JComboBox encoding;
	private JCheckBox encodingAutodetect;
	private JTextField encodingDetectors;
	private JTextField fallbackEncodings;
	
	private JCheckBoxList encodingsList;
	private JButton selectAllButton;
	private JButton selectNoneButton;
	//}}}

	
	//{{{ EncodingsOptionPane constructor
	public EncodingsOptionPane()
	{
		super("encodings");
		optionsPanel = new JPanel();
		optionsPanel.setLayout(gridBag = new GridBagLayout());
	} //}}}

	//{{{ addComponent() method
	/**
	 * Adds a labeled component to the option pane. Components are
	 * added in a vertical fashion, one per row. The label is
	 * displayed to the left of the component.
	 * @param comp1 The label
	 * @param comp2 The component
	 * @param fill Fill parameter to GridBagConstraints for the right
	 * component
	 *
	 * 
	 */
	public void addComponent(Component comp1, Component comp2, int fill)
	{
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		cons.weightx = 0.0f;
		cons.insets = new Insets(1,0,1,0);
		cons.fill = GridBagConstraints.BOTH;

		gridBag.setConstraints(comp1,cons);
		optionsPanel.add(comp1);

		cons.fill = fill;
		cons.gridx = 1;
		cons.weightx = 1.0f;
		gridBag.setConstraints(comp2,cons);
		optionsPanel.add(comp2);
	} //}}}

	
	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());
//		add(new JLabel(getProperty("options.encodings.selectEncodings")),BorderLayout.NORTH);
		
		/* Default file encoding */
		String[] encodings = MiscUtilities.getEncodings(true);
		Arrays.sort(encodings,new MiscUtilities.StringICaseCompare());
		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(jEdit.getProperty("buffer."+Buffer.ENCODING,
			System.getProperty("file.encoding")));
		addComponent(jEdit.getProperty("options.general.encoding"),encoding);

		/* Auto detect encoding */
		encodingAutodetect = new JCheckBox(jEdit.getProperty(
			"options.general.encodingAutodetect"));
		encodingAutodetect.setSelected(jEdit.getBooleanProperty("buffer.encodingAutodetect"));
		addComponent(encodingAutodetect);
		
		encodingDetectors = new JTextField(jEdit.getProperty(
			"options.general.encodingDetectors"));
		encodingDetectors.setText(jEdit.getProperty("encodingDetectors",
			"BOM XML-PI"));
		addComponent(jEdit.getProperty("options.general.encodingDetectors"),
			encodingDetectors);

		fallbackEncodings = new JTextField(jEdit.getProperty(
			"options.general.fallbackEncodings"));
		fallbackEncodings.setText(jEdit.getProperty("fallbackEncodings",
			""));
		fallbackEncodings.setToolTipText(jEdit.getProperty(
			"options.general.fallbackEncodings.tooltip"));
		addComponent(jEdit.getProperty("options.general.fallbackEncodings"),
			fallbackEncodings);

		encodings = getEncodings(false);
		sort(encodings,new StringICaseCompare());
		Vector<Entry> encodingEntriesVector = new Vector<Entry>();
		boolean enableSelectAll = false;
		boolean enableSelectNone = false;
		for (String encodstr : encodings) {
			boolean selected = !getBooleanProperty("encoding.opt-out."+encodstr,false);
			enableSelectAll = enableSelectAll || !selected;
			enableSelectNone = enableSelectNone || selected;
			encodingEntriesVector.add(new Entry(selected,encodstr));
		}
		encodingsList = new JCheckBoxList(encodingEntriesVector);
		encodingsList.getModel().addTableModelListener(new TableModelHandler());
		JScrollPane encodingsScrollPane = new JScrollPane(encodingsList);
		encodingsScrollPane.setBorder(
			new TitledBorder(getProperty("options.encodings.selectEncodings")));
		Dimension d = encodingsList.getPreferredSize();
		d.height = Math.min(d.height,200);
		encodingsScrollPane.setPreferredSize(d);

		add(optionsPanel, BorderLayout.NORTH);
		add(encodingsScrollPane,BorderLayout.CENTER);

		ActionHandler actionHandler = new ActionHandler();
		Box buttonsBox = createHorizontalBox();
		selectAllButton = new JButton(getProperty("options.encodings.selectAll"));
		selectAllButton.addActionListener(actionHandler);
		selectAllButton.setEnabled(enableSelectAll);
		buttonsBox.add(selectAllButton);
		selectNoneButton = new JButton(getProperty("options.encodings.selectNone"));
		selectNoneButton.addActionListener(actionHandler);
		selectNoneButton.setEnabled(enableSelectNone);
		buttonsBox.add(selectNoneButton);
		add(buttonsBox,BorderLayout.SOUTH);
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		
		jEdit.setProperty("buffer."+Buffer.ENCODING,(String)
			encoding.getSelectedItem());
		jEdit.setBooleanProperty("buffer.encodingAutodetect",
			encodingAutodetect.isSelected());
		jEdit.setProperty("encodingDetectors",encodingDetectors.getText());
		jEdit.setProperty("fallbackEncodings",fallbackEncodings.getText());
		
		for (Entry entry : encodingsList.getValues())
		{
			if (entry.isChecked())
			{
				unsetProperty("encoding.opt-out."+entry.getValue());
			}
			else
			{
				setBooleanProperty("encoding.opt-out."+entry.getValue(),true);
			}
		}
	} //}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent ae)
		{
			Object source = ae.getSource();
			if (source == selectAllButton)
			{
				encodingsList.selectAll();
			}
			else if (source == selectNoneButton)
			{
				for (int i=0, c=encodingsList.getRowCount() ; i<c ; i++)
				{
					encodingsList.setValueAt(false,i,0);
				}
			}
		}
	} //}}}

	//{{{ TableModelHandler class
	class TableModelHandler implements TableModelListener
	{
		public void tableChanged(TableModelEvent tme)
		{
			int checkedAmount = encodingsList.getCheckedValues().length;
			if (0 == checkedAmount)
			{
				selectNoneButton.setEnabled(false);
			}
			else
			{
				selectNoneButton.setEnabled(true);
			}
			if (encodingsList.getValues().length == checkedAmount)
			{
				selectAllButton.setEnabled(false);
			}
			else
			{
				selectAllButton.setEnabled(true);
			}
		}
	} //}}}

	//}}}

} //}}}
