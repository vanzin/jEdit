/*
 * BufferOptions.java - Buffer-specific options dialog
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.*;

/**
 * Buffer-specific options dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferOptions extends EnhancedDialog
{
	public BufferOptions(View view, Buffer buffer)
	{
		super(view,jEdit.getProperty("buffer-options.title"),true);
		this.view = view;
		this.buffer = buffer;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		ActionHandler actionListener = new ActionHandler();
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		Insets nullInsets = new Insets(0,0,0,0);
		Insets labelInsets = new Insets(0,0,0,12);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;

		// Edit mode
		JLabel label = new JLabel(jEdit.getProperty(
			"buffer-options.mode"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		modes = jEdit.getModes();
		String bufferMode = buffer.getMode().getName();
		int index = 0;
		String[] modeNames = new String[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			Mode mode = modes[i];
			modeNames[i] = mode.getName();
			if(bufferMode.equals(mode.getName()))
				index = i;
		}
		mode = new JComboBox(modeNames);
		mode.setSelectedIndex(index);
		mode.addActionListener(actionListener);
		layout.setConstraints(mode,cons);
		panel.add(mode);

		// Tab size
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.tabSize"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(buffer.getProperty("tabSize"));
		tabSize.addActionListener(actionListener);
		layout.setConstraints(tabSize,cons);
		panel.add(tabSize);

		// Indent size
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.indentSize"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		indentSize = new JComboBox(tabSizes);
		indentSize.setEditable(true);
		indentSize.setSelectedItem(buffer.getProperty("indentSize"));
		indentSize.addActionListener(actionListener);
		layout.setConstraints(indentSize,cons);
		panel.add(indentSize);

		// Max line length
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.maxLineLen"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		String[] lineLengths = { "0", "72", "76", "80" };

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		maxLineLen = new JComboBox(lineLengths);
		maxLineLen.setEditable(true);
		maxLineLen.setSelectedItem(buffer.getProperty("maxLineLen"));
		maxLineLen.addActionListener(actionListener);
		layout.setConstraints(maxLineLen,cons);
		panel.add(maxLineLen);

		// Line separator
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty("buffer-options.lineSeparator"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = (String)buffer.getProperty(Buffer.LINESEP);
		if(lineSep == null)
			lineSep = System.getProperty("line.separator");
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		lineSeparator.addActionListener(actionListener);
		layout.setConstraints(lineSeparator,cons);
		panel.add(lineSeparator);

		// Encoding
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty("buffer-options.encoding"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;

		DefaultComboBoxModel encodings = new DefaultComboBoxModel();
		StringTokenizer st = new StringTokenizer(jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			encodings.addElement(st.nextToken());
		}

		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(buffer.getProperty(Buffer.ENCODING));
		layout.setConstraints(encoding,cons);
		panel.add(encoding);

		// Read only setting
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		readOnly = new JCheckBox(jEdit.getProperty(
			"buffer-options.readOnly"));
		readOnly.setSelected(buffer.isReadOnly());
		readOnly.addActionListener(actionListener);
		layout.setConstraints(readOnly,cons);
		panel.add(readOnly);

		// Trailing EOL setting
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		trailingEOL = new JCheckBox(jEdit.getProperty(
			"buffer-options.trailingEOL"));
		trailingEOL.setSelected(buffer.getBooleanProperty(Buffer.TRAILING_EOL));
		trailingEOL.addActionListener(actionListener);
		layout.setConstraints(trailingEOL,cons);
		panel.add(trailingEOL);

		// Syntax highlighting
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		syntax = new JCheckBox(jEdit.getProperty(
			"options.editing.syntax"));
		syntax.setSelected(buffer.getBooleanProperty("syntax"));
		syntax.addActionListener(actionListener);
		layout.setConstraints(syntax,cons);
		panel.add(syntax);

		// Indent on tab
		cons.gridy++;
		indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab"));
		indentOnTab.setSelected(buffer.getBooleanProperty("indentOnTab"));
		indentOnTab.addActionListener(actionListener);
		layout.setConstraints(indentOnTab,cons);
		panel.add(indentOnTab);

		// Indent on enter
		cons.gridy++;
		indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter"));
		indentOnEnter.setSelected(buffer.getBooleanProperty("indentOnEnter"));
		indentOnEnter.addActionListener(actionListener);
		layout.setConstraints(indentOnEnter,cons);
		panel.add(indentOnEnter);

		// Soft tabs
		cons.gridy++;
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs"));
		noTabs.setSelected(buffer.getBooleanProperty("noTabs"));
		noTabs.addActionListener(actionListener);
		layout.setConstraints(noTabs,cons);
		panel.add(noTabs);

		// Props label
		cons.gridy++;
		cons.insets = new Insets(6,0,6,0);
		label = new JLabel(jEdit.getProperty("buffer-options.props"));
		layout.setConstraints(label,cons);
		panel.add(label);

		content.add(BorderLayout.NORTH,panel);

		props = new JTextArea(4,4);
		props.setLineWrap(true);
		props.setWrapStyleWord(false);
		content.add(BorderLayout.CENTER,new JScrollPane(props));
		updatePropsField();

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionListener);
		panel.add(cancel);
		panel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,panel);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		try
		{
			buffer.putProperty("tabSize",new Integer(
				tabSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.putProperty("indentSize",new Integer(
				indentSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.putProperty("maxLineLen",new Integer(
				maxLineLen.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		int index = mode.getSelectedIndex();
		buffer.setMode(modes[index]);

		index = lineSeparator.getSelectedIndex();
		String lineSep;
		if(index == 0)
			lineSep = "\n";
		else if(index == 1)
			lineSep = "\r\n";
		else if(index == 2)
			lineSep = "\r";
		else
			throw new InternalError();

		String oldLineSep = (String)buffer.getProperty(Buffer.LINESEP);
		if(oldLineSep == null)
			oldLineSep = System.getProperty("line.separator");
		if(!oldLineSep.equals(lineSep))
		{
			buffer.putProperty("lineSeparator",lineSep);
			buffer.setDirty(true);
		}

		String encoding = (String)this.encoding.getSelectedItem();
		String oldEncoding = (String)buffer.getProperty(Buffer.ENCODING);
		if(!oldEncoding.equals(encoding))
		{
			buffer.putProperty(Buffer.ENCODING,encoding);
			buffer.setDirty(true);

			// XXX this should be moved elsewhere!!!
			EditBus.send(new BufferUpdate(buffer,view,BufferUpdate.ENCODING_CHANGED));
		}

		buffer.setReadOnly(readOnly.isSelected());
		buffer.putBooleanProperty(Buffer.TRAILING_EOL,trailingEOL.isSelected());
		buffer.putBooleanProperty("syntax",syntax.isSelected());
		buffer.putBooleanProperty("indentOnTab",indentOnTab.isSelected());
		buffer.putBooleanProperty("indentOnEnter",indentOnEnter.isSelected());
		buffer.putBooleanProperty("noTabs",noTabs.isSelected());

		buffer.propertiesChanged();
		dispose();

		// Update text area
		view.getTextArea().getPainter().repaint();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

        // private members
	private View view;
	private Buffer buffer;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox maxLineLen;
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox lineSeparator;
	private JComboBox encoding;
	private JCheckBox readOnly;
	private JCheckBox trailingEOL;
	private JCheckBox syntax;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox noTabs;
	private JTextArea props;
	private JButton ok;
	private JButton cancel;

	private void updatePropsField()
	{
		props.setText(":mode=" + modes[mode.getSelectedIndex()].getName()
			+ ":tabSize=" + tabSize.getSelectedItem()
			+ ":indentSize=" + indentSize.getSelectedItem()
			+ ":maxLineLen=" + maxLineLen.getSelectedItem()
			+ ":trailingEOL=" + trailingEOL.isSelected()
			+ ":syntax=" + syntax.isSelected()
			+ ":noTabs=" + noTabs.isSelected()
			+ ":indentOnTab=" + indentOnTab.isSelected()
			+ ":indentOnEnter=" + indentOnEnter.isSelected()
			+ ":");
	}

	class ActionHandler implements ActionListener
	{
		boolean doNotShowDialog = false;

		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == mode)
			{
				Mode _mode = jEdit.getMode((String)
					mode.getSelectedItem());
				tabSize.setSelectedItem(_mode.getProperty(
					"tabSize"));
				indentSize.setSelectedItem(_mode.getProperty(
					"indentSize"));
				maxLineLen.setSelectedItem(_mode.getProperty(
					"maxLineLen"));
				indentOnTab.setSelected(_mode.getBooleanProperty(
					"indentOnTab"));
				indentOnEnter.setSelected(_mode.getBooleanProperty(
					"indentOnEnter"));
				syntax.setSelected(_mode.getBooleanProperty(
					"syntax"));
				noTabs.setSelected(_mode.getBooleanProperty(
					"noTabs"));
				updatePropsField();
			}
			else if(!doNotShowDialog && source == readOnly)
			{
				if(readOnly.isSelected()
					&& buffer.isDirty())
				{
					doNotShowDialog = true;
					readOnly.setSelected(false);
					doNotShowDialog = false;
					GUIUtilities.error(BufferOptions.this,
						"buffer-options.readOnly",null);
				}
			}
			else
				updatePropsField();
		}
	}
}
