/*
 * EncodingsOptionPane.java - Encodings options panel
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.MiscUtilities.StringICaseCompare;
//}}}

//{{{ EncodingsOptionPane class
/**
 * Encodings editor.
 * @author Björn Kautler
 * @since 4.3pre5
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

		add(new JLabel(jEdit.getProperty("options.encodings.selectEncodings")),BorderLayout.NORTH);

		Box encodingsBox = Box.createVerticalBox();
		String[] encodings = MiscUtilities.getEncodings(false);
		Arrays.sort(encodings,new MiscUtilities.StringICaseCompare());
		int encodingsAmount = encodings.length;
		encodingCheckBoxArray = new JCheckBox[encodingsAmount];
		for (int i=0 ; i<encodingsAmount ; i++) {
			String encoding = encodings[i];
			JCheckBox encodingCheckBox = new JCheckBox(encoding,!jEdit.getBooleanProperty("encoding.opt-out."+encoding,false));
			encodingCheckBoxArray[i] = encodingCheckBox;
			encodingsBox.add(encodingCheckBox);
		}
		JScrollPane encodingsScrollPane = new JScrollPane(encodingsBox);
		Dimension d = encodingsBox.getPreferredSize();
		d.height = Math.min(d.height,200);
		encodingsScrollPane.setPreferredSize(d);
		add(encodingsScrollPane,BorderLayout.CENTER);

		ActionHandler actionHandler = new ActionHandler();
		Box buttonsBox = Box.createHorizontalBox();
		selectAllButton = new JButton(jEdit.getProperty("options.encodings.selectAll"));
		selectAllButton.addActionListener(actionHandler);
		buttonsBox.add(selectAllButton);
		selectNoneButton = new JButton(jEdit.getProperty("options.encodings.selectNone"));
		selectNoneButton.addActionListener(actionHandler);
		buttonsBox.add(selectNoneButton);
		add(buttonsBox,BorderLayout.SOUTH);
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		for (int i=0, c=encodingCheckBoxArray.length ; i<c ; i++)
		{
			JCheckBox encodingCheckBox = encodingCheckBoxArray[i];
			jEdit.setBooleanProperty("encoding.opt-out."+encodingCheckBox.getText(),!encodingCheckBox.isSelected());
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JCheckBox[] encodingCheckBoxArray;
	private JButton selectAllButton;
	private JButton selectNoneButton;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			boolean select;
			Object source = e.getSource();
			if (source == selectAllButton)
			{
				select = true;
			}
			else if (source == selectNoneButton)
			{
				select = false;
			}
			else
			{
				return;
			}
			for (int i=0, c=encodingCheckBoxArray.length ; i<c ; i++)
			{
				encodingCheckBoxArray[i].setSelected(select);
			}
		}
	} //}}}
} //}}}
