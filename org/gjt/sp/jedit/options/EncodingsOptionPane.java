/*
 * EncodingsOptionPane.java - Encodings options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Björn Kautler
 * Portions copyright (C) 2010 Matthieu Casanova
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.PingPongList;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import org.gjt.sp.util.StandardUtilities;
import static java.awt.GridBagConstraints.BOTH;
import static java.util.Arrays.sort;
import static javax.swing.Box.createHorizontalBox;
import static javax.swing.Box.createHorizontalStrut;
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
 * @author Matthieu Casanova
 * @since jEdit 4.3pre6
 * @version $Id$
 */
public class EncodingsOptionPane extends AbstractOptionPane
{
	//{{{ Instance variables
	private JComboBox defaultEncoding;
	private JCheckBox encodingAutodetect;
	private JTextField encodingDetectors;
	private JTextField fallbackEncodings;
	private JComboBox lineSeparator;
	private JButton selectAllButton;
	private JButton selectNoneButton;
	private PingPongList<String> pingPongList;
	//}}}

	//{{{ EncodingsOptionPane constructor
	public EncodingsOptionPane()
	{
		super("encodings");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{

		/* Line separator */
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);

		String lineSep = jEdit.getProperty("buffer."+ JEditBuffer.LINESEP,
			System.getProperty("line.separator"));
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.general.lineSeparator"),
			lineSeparator);


		// Default file encoding
		String[] encodings = getEncodings(true);
		sort(encodings,new StandardUtilities.StringCompare<String>(true));
		defaultEncoding = new JComboBox(encodings);
		defaultEncoding.setEditable(true);
		defaultEncoding.setSelectedItem(jEdit.getProperty("buffer."+JEditBuffer.ENCODING,
			System.getProperty("file.encoding")));
		addComponent(jEdit.getProperty("options.general.encoding"),defaultEncoding);

		// Auto detect encoding
		encodingAutodetect = new JCheckBox(jEdit.getProperty(
			"options.general.encodingAutodetect"));
		encodingAutodetect.setSelected(jEdit.getBooleanProperty(
			"buffer.encodingAutodetect"));
		addComponent(encodingAutodetect,BOTH);

		// Encoding detectors
		encodingDetectors = new JTextField(jEdit.getProperty(
			"encodingDetectors",""));
		addComponent(jEdit.getProperty("options.general.encodingDetectors"),encodingDetectors);

		// Fallback Encodings
		fallbackEncodings = new JTextField(jEdit.getProperty(
			"fallbackEncodings",""));
		fallbackEncodings.setToolTipText(jEdit.getProperty(
			"options.general.fallbackEncodings.tooltip"));
		addComponent(jEdit.getProperty("options.general.fallbackEncodings"),fallbackEncodings);

		// Encodings to display
		encodings = getEncodings(false);
		sort(encodings,new StandardUtilities.StringCompare<String>(true));
		List<String> availableEncodings = new ArrayList<String>();
		List<String> selectedEncodings = new ArrayList<String>();
		for (String encoding : encodings)
		{
			boolean selected = !getBooleanProperty("encoding.opt-out."+encoding,false);
			if (selected)
				selectedEncodings.add(encoding);
			else
				availableEncodings.add(encoding);
		}
		pingPongList = new PingPongList<String>(availableEncodings, selectedEncodings);
		pingPongList.setLeftTitle(getProperty("options.encodings.available"));
		pingPongList.setRightTitle(getProperty("options.encodings.selected"));
		pingPongList.setLeftTooltip(getProperty("options.encodings.available.tooltip"));
		pingPongList.setRightTooltip(getProperty("options.encodings.selected.tooltip"));
		addComponent(pingPongList,BOTH);

		// Select All/None Buttons
		Box buttonsBox = createHorizontalBox();
		buttonsBox.add(createHorizontalStrut(12));

		ActionHandler actionHandler = new ActionHandler();
		selectAllButton = new JButton(getProperty("options.encodings.selectAll"));
		selectAllButton.addActionListener(actionHandler);
		selectAllButton.setEnabled(pingPongList.getLeftSize() != 0);
		buttonsBox.add(selectAllButton);
		buttonsBox.add(createHorizontalStrut(12));

		selectNoneButton = new JButton(getProperty("options.encodings.selectNone"));
		selectNoneButton.addActionListener(actionHandler);
		selectNoneButton.setEnabled(pingPongList.getRightSize() != 0);
		buttonsBox.add(selectNoneButton);
		buttonsBox.add(createHorizontalStrut(12));

		addComponent(buttonsBox);
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		String lineSep = null;
		switch(lineSeparator.getSelectedIndex())
		{
		case 0:
			lineSep = "\n";
			break;
		case 1:
			lineSep = "\r\n";
			break;
		case 2:
			lineSep = "\r";
			break;
		}
		jEdit.setProperty("buffer."+ JEditBuffer.LINESEP,lineSep);

		jEdit.setProperty("buffer."+ JEditBuffer.ENCODING,(String)
			defaultEncoding.getSelectedItem());
		jEdit.setBooleanProperty("buffer.encodingAutodetect",
			encodingAutodetect.isSelected());
		jEdit.setProperty("encodingDetectors",encodingDetectors.getText());
		jEdit.setProperty("fallbackEncodings",fallbackEncodings.getText());
		Iterator<String> available = pingPongList.getLeftDataIterator();
		while (available.hasNext())
		{
			String encoding = available.next();
			setBooleanProperty("encoding.opt-out."+encoding,true);

		}
		Iterator<String> selected = pingPongList.getRightDataIterator();
		while (selected.hasNext())
		{
			String encoding = selected.next();
			unsetProperty("encoding.opt-out."+encoding);
		}
	} //}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	private class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			Object source = ae.getSource();
			if (source == selectAllButton)
			{
				pingPongList.moveAllToRight();
			}
			else if (source == selectNoneButton)
			{
				pingPongList.moveAllToLeft();
			}
		}
	} //}}}
	//}}}
} //}}}
