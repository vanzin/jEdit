/*
 * BufferOptions.java - Buffer-specific options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.textarea.FoldVisibilityManager;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Buffer-specific options dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferOptions extends EnhancedDialog
{
	//{{{ BufferOptions constructor
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

		//{{{ Edit mode
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
		//}}}

		//{{{ Tab size
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
		tabSize.setSelectedItem(buffer.getStringProperty("tabSize"));
		tabSize.addActionListener(actionListener);
		layout.setConstraints(tabSize,cons);
		panel.add(tabSize);
		//}}}

		//{{{ Indent size
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
		indentSize.setSelectedItem(buffer.getStringProperty("indentSize"));
		indentSize.addActionListener(actionListener);
		layout.setConstraints(indentSize,cons);
		panel.add(indentSize);
		//}}}

		//{{{ Max line length
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
		maxLineLen.setSelectedItem(buffer.getStringProperty("maxLineLen"));
		maxLineLen.addActionListener(actionListener);
		layout.setConstraints(maxLineLen,cons);
		panel.add(maxLineLen);
		//}}}

		//{{{ Fold mode
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.insets = labelInsets;
		label = new JLabel(jEdit.getProperty(
			"options.editing.folding"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		String[] foldModes = {
			"none",
			"indent",
			"explicit"
		};

		cons.gridx = 1;
		cons.weightx = 1.0f;
		cons.insets = nullInsets;
		folding = new JComboBox(foldModes);
		String foldMode = buffer.getStringProperty("folding");

		if("indent".equals(foldMode))
			folding.setSelectedIndex(1);
		else if("explicit".equals(foldMode))
			folding.setSelectedIndex(2);
		else
			folding.setSelectedIndex(0);
		folding.addActionListener(actionListener);
		layout.setConstraints(folding,cons);
		panel.add(folding);
		//}}}

		//{{{ Line separator
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
		String lineSep = buffer.getStringProperty(Buffer.LINESEP);
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
		//}}}

		//{{{ Encoding
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
		encoding.setSelectedItem(buffer.getStringProperty(Buffer.ENCODING));
		layout.setConstraints(encoding,cons);
		panel.add(encoding);
		//}}}

		//{{{ GZipped setting
		cons.gridx = 0;
		cons.gridy++;
		cons.weightx = 0.0f;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		gzipped = new JCheckBox(jEdit.getProperty(
			"buffer-options.gzipped"));
		gzipped.setSelected(buffer.getBooleanProperty(Buffer.GZIPPED));
		gzipped.addActionListener(actionListener);
		layout.setConstraints(gzipped,cons);
		panel.add(gzipped);
		//}}}

		//{{{ Trailing EOL setting
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
		//}}}

		//{{{ Indent on tab
		cons.gridy++;
		indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab"));
		indentOnTab.setSelected(buffer.getBooleanProperty("indentOnTab"));
		indentOnTab.addActionListener(actionListener);
		layout.setConstraints(indentOnTab,cons);
		panel.add(indentOnTab);
		//}}}

		//{{{ Indent on enter
		cons.gridy++;
		indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter"));
		indentOnEnter.setSelected(buffer.getBooleanProperty("indentOnEnter"));
		indentOnEnter.addActionListener(actionListener);
		layout.setConstraints(indentOnEnter,cons);
		panel.add(indentOnEnter);
		//}}}

		//{{{ Soft tabs
		cons.gridy++;
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs"));
		noTabs.setSelected(buffer.getBooleanProperty("noTabs"));
		noTabs.addActionListener(actionListener);
		layout.setConstraints(noTabs,cons);
		panel.add(noTabs);
		//}}}

		//{{{ Props label
		cons.gridy++;
		cons.insets = new Insets(6,0,6,0);
		label = new JLabel(jEdit.getProperty("buffer-options.props"));
		layout.setConstraints(label,cons);
		panel.add(label);
		//}}}

		content.add(BorderLayout.NORTH,panel);

		//{{{ Properties text area
		props = new JTextArea(4,4);
		props.setLineWrap(true);
		props.setWrapStyleWord(false);
		content.add(BorderLayout.CENTER,new JScrollPane(props));
		updatePropsField();
		//}}}

		//{{{ Buttons
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
		//}}}

		pack();
		setLocationRelativeTo(view);
		show();
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		try
		{
			buffer.setProperty("tabSize",new Integer(
				tabSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.setProperty("indentSize",new Integer(
				indentSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		try
		{
			buffer.setProperty("maxLineLen",new Integer(
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

		String oldLineSep = buffer.getStringProperty(Buffer.LINESEP);
		if(oldLineSep == null)
			oldLineSep = System.getProperty("line.separator");
		if(!oldLineSep.equals(lineSep))
		{
			buffer.setStringProperty("lineSeparator",lineSep);
			buffer.setDirty(true);
		}

		String encoding = (String)this.encoding.getSelectedItem();
		String oldEncoding = buffer.getStringProperty(Buffer.ENCODING);
		if(!oldEncoding.equals(encoding))
		{
			buffer.setStringProperty(Buffer.ENCODING,encoding);
			buffer.setDirty(true);

			// XXX this should be moved elsewhere!!!
			EditBus.send(new BufferUpdate(buffer,view,BufferUpdate.ENCODING_CHANGED));
		}

		String foldMode = (String)folding.getSelectedItem();
		String oldFoldMode = buffer.getStringProperty("folding");
		buffer.setStringProperty("folding",foldMode);

		boolean gzippedValue = gzipped.isSelected();
		boolean oldGzipped = buffer.getBooleanProperty(
			Buffer.GZIPPED);
		if(gzippedValue != oldGzipped)
		{
			buffer.setBooleanProperty(Buffer.GZIPPED,gzippedValue);
			buffer.setDirty(true);
		}

		boolean trailingEOLValue = trailingEOL.isSelected();
		boolean oldTrailingEOL = buffer.getBooleanProperty(
			Buffer.TRAILING_EOL);
		if(trailingEOLValue != oldTrailingEOL)
		{
			buffer.setBooleanProperty(Buffer.TRAILING_EOL,trailingEOLValue);
			buffer.setDirty(true);
		}

		buffer.setBooleanProperty("indentOnTab",indentOnTab.isSelected());
		buffer.setBooleanProperty("indentOnEnter",indentOnEnter.isSelected());
		buffer.setBooleanProperty("noTabs",noTabs.isSelected());

		buffer.propertiesChanged();
		if(!oldFoldMode.equals(foldMode))
		{
			FoldVisibilityManager foldVisibilityManager
				 = view.getTextArea().getFoldVisibilityManager();
			int collapseFolds = buffer.getIntegerProperty(
				"collapseFolds",0);
			if(collapseFolds != 0)
				foldVisibilityManager.expandFolds(collapseFolds);
			else
				foldVisibilityManager.expandAllFolds();
		}

		dispose();

		// Update text area
		view.getTextArea().getPainter().repaint();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

        //{{{ Private members

	//{{{ Instance variables
	private View view;
	private Buffer buffer;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox maxLineLen;
	private JComboBox folding;
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox lineSeparator;
	private JComboBox encoding;
	private JCheckBox gzipped;
	private JCheckBox trailingEOL;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox noTabs;
	private JTextArea props;
	private JButton ok;
	private JButton cancel;
	//}}}

	//{{{ updatePropsField() method
	private void updatePropsField()
	{
		props.setText(":mode=" + modes[mode.getSelectedIndex()].getName()
			+ ":tabSize=" + tabSize.getSelectedItem()
			+ ":indentSize=" + indentSize.getSelectedItem()
			+ ":maxLineLen=" + maxLineLen.getSelectedItem()
			+ ":noTabs=" + noTabs.isSelected()
			+ ":indentOnTab=" + indentOnTab.isSelected()
			+ ":indentOnEnter=" + indentOnEnter.isSelected()
			+ ":");
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() method
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
				noTabs.setSelected(_mode.getBooleanProperty(
					"noTabs"));
				updatePropsField();
			}
			else
				updatePropsField();
		} //}}}
	} //}}}

	//}}}
}
