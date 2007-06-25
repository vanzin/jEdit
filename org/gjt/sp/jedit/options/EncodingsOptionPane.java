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
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.gjt.sp.jedit.AbstractOptionPane;

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
 * Encodings editor.
 * @author Björn Kautler
 * @since jEdit 4.3pre6
 * @version $Id$
 */
public class EncodingsOptionPane extends AbstractOptionPane
{
	//{{{ EncodingsOptionPane constructor
	public EncodingsOptionPane()
	{
		super("encodings");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());

		add(new JLabel(getProperty("options.encodings.selectEncodings")),BorderLayout.NORTH);

		String[] encodings = getEncodings(false);
		sort(encodings,new StringICaseCompare());
		Vector<Entry> encodingEntriesVector = new Vector<Entry>();
		boolean enableSelectAll = false;
		boolean enableSelectNone = false;
		for (String encoding : encodings) {
			boolean selected = !getBooleanProperty("encoding.opt-out."+encoding,false);
			enableSelectAll = enableSelectAll || !selected;
			enableSelectNone = enableSelectNone || selected;
			encodingEntriesVector.add(new Entry(selected,encoding));
		}
		encodingsList = new JCheckBoxList(encodingEntriesVector);
		encodingsList.getModel().addTableModelListener(new TableModelHandler());
		JScrollPane encodingsScrollPane = new JScrollPane(encodingsList);
		Dimension d = encodingsList.getPreferredSize();
		d.height = Math.min(d.height,200);
		encodingsScrollPane.setPreferredSize(d);
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

	//{{{ Private members

	//{{{ Instance variables
	private JCheckBoxList encodingsList;
	private JButton selectAllButton;
	private JButton selectNoneButton;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent ae)
		{
			Object source = ae.getSource();
			if (source == selectAllButton)
			{
				encodingsList.selectAll();
//				selectAllButton.setEnabled(false);
//				selectNoneButton.setEnabled(true);
			}
			else if (source == selectNoneButton)
			{
				for (int i=0, c=encodingsList.getRowCount() ; i<c ; i++)
				{
					encodingsList.setValueAt(false,i,0);
				}
//				selectAllButton.setEnabled(true);
//				selectNoneButton.setEnabled(false);
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
