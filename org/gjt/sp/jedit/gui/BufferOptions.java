/*
 * BufferOptions.java - Buffer-specific options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001, 2002 Slava Pestov
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
		AbstractOptionPane panel = new AbstractOptionPane(null)
		{
			public void addComponent(Component comp)
			{
				super.addComponent(comp);
			}

			public void addComponent(String label, Component comp)
			{
				super.addComponent(label,comp);
			}

			public void addSeparator(String separator)
			{
				super.addSeparator(separator);
			}
		};

		panel.addSeparator("buffer-options.loading-saving");

		//{{{ Line separator
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
		panel.addComponent(jEdit.getProperty("buffer-options.lineSeparator"),
			lineSeparator);
		//}}}

		//{{{ Encoding
		DefaultComboBoxModel encodings = new DefaultComboBoxModel();
		StringTokenizer st = new StringTokenizer(jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			encodings.addElement(st.nextToken());
		}

		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(buffer.getStringProperty(Buffer.ENCODING));
		panel.addComponent(jEdit.getProperty("buffer-options.encoding"),
			encoding);
		//}}}

		//{{{ GZipped setting
		gzipped = new JCheckBox(jEdit.getProperty(
			"buffer-options.gzipped"));
		gzipped.setSelected(buffer.getBooleanProperty(Buffer.GZIPPED));
		gzipped.addActionListener(actionListener);
		panel.addComponent(gzipped);
		//}}}

		//{{{ Trailing EOL setting
		trailingEOL = new JCheckBox(jEdit.getProperty(
			"buffer-options.trailingEOL"));
		trailingEOL.setSelected(buffer.getBooleanProperty(Buffer.TRAILING_EOL));
		trailingEOL.addActionListener(actionListener);
		panel.addComponent(trailingEOL);
		//}}}

		panel.addSeparator("buffer-options.editing");

		//{{{ Edit mode
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
		panel.addComponent(jEdit.getProperty("buffer-options.mode"),mode);
		//}}}

		//{{{ Tab size
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(buffer.getStringProperty("tabSize"));
		tabSize.addActionListener(actionListener);
		panel.addComponent(jEdit.getProperty("options.editing.tabSize"),tabSize);
		//}}}

		//{{{ Indent size
		indentSize = new JComboBox(tabSizes);
		indentSize.setEditable(true);
		indentSize.setSelectedItem(buffer.getStringProperty("indentSize"));
		indentSize.addActionListener(actionListener);
		panel.addComponent(jEdit.getProperty("options.editing.indentSize"),
			indentSize);
		//}}}

		//{{{ Fold mode
		String[] foldModes = {
			"none",
			"indent",
			"explicit"
		};

		folding = new JComboBox(foldModes);
		String foldMode = buffer.getStringProperty("folding");

		if("indent".equals(foldMode))
			folding.setSelectedIndex(1);
		else if("explicit".equals(foldMode))
			folding.setSelectedIndex(2);
		else
			folding.setSelectedIndex(0);
		folding.addActionListener(actionListener);
		panel.addComponent(jEdit.getProperty("options.editing.folding"),
			folding);
		//}}}

		//{{{ Max line length
		String[] lineLengths = { "0", "72", "76", "80" };

		maxLineLen = new JComboBox(lineLengths);
		maxLineLen.setEditable(true);
		maxLineLen.setSelectedItem(buffer.getStringProperty("maxLineLen"));
		maxLineLen.addActionListener(actionListener);
		panel.addComponent(jEdit.getProperty("options.editing.maxLineLen"),
			maxLineLen);
		//}}}

		//{{{ Soft wrap
		softWrap = new JCheckBox(jEdit.getProperty(
			"options.editing.softWrap"));
		softWrap.setSelected(buffer.getBooleanProperty("softWrap"));
		softWrap.addActionListener(actionListener);
		panel.addComponent(softWrap);
		//}}}

		//{{{ Indent on tab
		indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab"));
		indentOnTab.setSelected(buffer.getBooleanProperty("indentOnTab"));
		indentOnTab.addActionListener(actionListener);
		panel.addComponent(indentOnTab);
		//}}}

		//{{{ Indent on enter
		indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter"));
		indentOnEnter.setSelected(buffer.getBooleanProperty("indentOnEnter"));
		indentOnEnter.addActionListener(actionListener);
		panel.addComponent(indentOnEnter);
		//}}}

		//{{{ Soft tabs
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs"));
		noTabs.setSelected(buffer.getBooleanProperty("noTabs"));
		noTabs.addActionListener(actionListener);
		panel.addComponent(noTabs);
		//}}}

		//{{{ Props label
		JLabel label = new JLabel(jEdit.getProperty("buffer-options.props"));
		label.setBorder(new EmptyBorder(6,0,6,0));
		panel.addComponent(label);
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
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionListener);
		buttons.add(cancel);
		buttons.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttons);
		//}}}

		pack();
		setLocationRelativeTo(view);
		show();
	} //}}}

	//{{{ ok() method
	public void ok()
	{
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

		String foldMode = (String)folding.getSelectedItem();
		String oldFoldMode = buffer.getStringProperty("folding");
		buffer.setStringProperty("folding",foldMode);
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

		try
		{
			buffer.setProperty("maxLineLen",new Integer(
				maxLineLen.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		buffer.setBooleanProperty("softWrap",softWrap.isSelected());
		buffer.setBooleanProperty("indentOnTab",indentOnTab.isSelected());
		buffer.setBooleanProperty("indentOnEnter",indentOnEnter.isSelected());
		buffer.setBooleanProperty("noTabs",noTabs.isSelected());

		buffer.propertiesChanged();

		dispose();
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
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox lineSeparator;
	private JComboBox encoding;
	private JCheckBox gzipped;
	private JCheckBox trailingEOL;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox folding;
	private JComboBox maxLineLen;
	private JCheckBox softWrap;
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
			+ ":softWrap=" + softWrap.isSelected()
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
				softWrap.setSelected(_mode.getBooleanProperty(
					"softWrap"));
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
