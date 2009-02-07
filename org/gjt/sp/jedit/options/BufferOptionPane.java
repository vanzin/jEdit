/*
 * BufferOptionPane.java -
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.StandardUtilities;


public class BufferOptionPane extends AbstractOptionPane
{
	private JComboBox encoding;
	private JComboBox lineSeparator;
	private JCheckBox gzipped;
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox folding;
	private JComboBox wrap;
	private JComboBox maxLineLen;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox checkModStatus;
	private JCheckBox noTabs;
	private Buffer buffer;


	public BufferOptionPane()
	{
		super("Buffer Options");
		init();
	}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		buffer = jEdit.getActiveView().getBuffer();
		String filename = buffer.getName();
		setName("Buffer: " + filename);
		addComponent(GUIUtilities.createMultilineLabel(
			jEdit.getProperty("buffer-options.caption")));

		addSeparator("buffer-options.loading-saving");

		//{{{ Line separator
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = buffer.getStringProperty(JEditBuffer.LINESEP);
		if(lineSep == null)
			lineSep = System.getProperty("line.separator");
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		addComponent(jEdit.getProperty("buffer-options.lineSeparator"),
			lineSeparator);
		//}}}

		//{{{ Encoding
		String[] encodings = MiscUtilities.getEncodings(true);
		Arrays.sort(encodings,new StandardUtilities.StringCompare<String>(true));
		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(buffer.getStringProperty(JEditBuffer.ENCODING));
		addComponent(jEdit.getProperty("buffer-options.encoding"),
			encoding);
		//}}}

		//{{{ GZipped setting
		gzipped = new JCheckBox(jEdit.getProperty(
			"buffer-options.gzipped"));
		gzipped.setSelected(buffer.getBooleanProperty(Buffer.GZIPPED));
		addComponent(gzipped);
		//}}}

		//{{{ Autoreload settings
		/* Check mod status on focus */
		String[] modCheckOptions = {
			jEdit.getProperty("options.general.checkModStatus.nothing"),
			jEdit.getProperty("options.general.checkModStatus.prompt"),
			jEdit.getProperty("options.general.checkModStatus.reload"),
			jEdit.getProperty("options.general.checkModStatus.silentReload")
		};
		checkModStatus = new JComboBox(modCheckOptions);
		if(buffer.getAutoReload())
		{
			if(buffer.getAutoReloadDialog())
				// reload and notify
				checkModStatus.setSelectedIndex(2);
			else	// reload silently
				checkModStatus.setSelectedIndex(3);
		}
		else
		{
			if(buffer.getAutoReloadDialog())
				// prompt
				checkModStatus.setSelectedIndex(1);
			else	// do nothing
				checkModStatus.setSelectedIndex(0);
		}
		addComponent(jEdit.getProperty("options.general.checkModStatus"),
			checkModStatus);

		// }}}

		addSeparator("buffer-options.editing");

		//{{{ Edit mode
		modes = jEdit.getModes();
		Arrays.sort(modes,new StandardUtilities.StringCompare<Mode>(true));
		mode = new JComboBox(modes);
		mode.setSelectedItem(buffer.getMode());
		ActionHandler actionListener = new ActionHandler();
		mode.addActionListener(actionListener);
		addComponent(jEdit.getProperty("buffer-options.mode"),mode);
		//}}}

		//{{{ Fold mode
		String[] foldModes = FoldHandler.getFoldModes();

		folding = new JComboBox(foldModes);
		folding.setSelectedItem(buffer.getStringProperty("folding"));
		addComponent(jEdit.getProperty("options.editing.folding"),
			folding);
		//}}}

		//{{{ Wrap mode
		String[] wrapModes = {
			"none",
			"soft",
			"hard"
		};

		wrap = new JComboBox(wrapModes);
		wrap.setSelectedItem(buffer.getStringProperty("wrap"));
		addComponent(jEdit.getProperty("options.editing.wrap"),
			wrap);
		//}}}

		//{{{ Max line length
		String[] lineLengths = { "0", "72", "76", "80" };

		maxLineLen = new JComboBox(lineLengths);
		maxLineLen.setEditable(true);
		maxLineLen.setSelectedItem(buffer.getStringProperty("maxLineLen"));
		addComponent(jEdit.getProperty("options.editing.maxLineLen"),
			maxLineLen);
		//}}}

		//{{{ Tab size
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(buffer.getStringProperty("tabSize"));
		addComponent(jEdit.getProperty("options.editing.tabSize"),tabSize);
		//}}}

		//{{{ Indent size
		indentSize = new JComboBox(tabSizes);
		indentSize.setEditable(true);
		indentSize.setSelectedItem(buffer.getStringProperty("indentSize"));
		addComponent(jEdit.getProperty("options.editing.indentSize"),
			indentSize);
		//}}}

		//{{{ Soft tabs
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs"));
		noTabs.setSelected(buffer.getBooleanProperty("noTabs"));
		addComponent(noTabs);
		//}}}
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		int index = lineSeparator.getSelectedIndex();
		String lineSep;
		if(index == 0)
			lineSep = "\n";
		else if(index == 1)
			lineSep = "\r\n";
		else if(index == 2)
			lineSep = "\r";
		else
			throw new InternalError();

		String oldLineSep = buffer.getStringProperty(JEditBuffer.LINESEP);
		if(oldLineSep == null)
			oldLineSep = System.getProperty("line.separator");
		if(!oldLineSep.equals(lineSep))
		{
			buffer.setStringProperty(JEditBuffer.LINESEP, lineSep);
			buffer.setDirty(true);
		}

		String encoding = (String)this.encoding.getSelectedItem();
		String oldEncoding = buffer.getStringProperty(JEditBuffer.ENCODING);
		if(!oldEncoding.equals(encoding))
		{
			buffer.setStringProperty(JEditBuffer.ENCODING,encoding);
			buffer.setDirty(true);
			// Disable auto-detect because user explicitly
			// specify an encoding.
			buffer.setBooleanProperty(Buffer.ENCODING_AUTODETECT,false);
		}

		boolean gzippedValue = gzipped.isSelected();
		boolean oldGzipped = buffer.getBooleanProperty(
			Buffer.GZIPPED);
		if(gzippedValue != oldGzipped)
		{
			buffer.setBooleanProperty(Buffer.GZIPPED,gzippedValue);
			buffer.setDirty(true);
		}

		buffer.setStringProperty("folding",(String)folding.getSelectedItem());

		buffer.setStringProperty("wrap",(String)wrap.getSelectedItem());

		try
		{
			buffer.setProperty("maxLineLen",new Integer(
				maxLineLen.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
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

		buffer.setBooleanProperty("noTabs",noTabs.isSelected());

		index = mode.getSelectedIndex();
		buffer.setMode(modes[index]);
		switch(checkModStatus.getSelectedIndex())
		{
		case 0:
			buffer.setAutoReloadDialog(false);
			buffer.setAutoReload(false);
			break;
		case 1:
			buffer.setAutoReloadDialog(true);
			buffer.setAutoReload(false);
			break;
		case 2:
			buffer.setAutoReloadDialog(true);
			buffer.setAutoReload(true);
			break;
		case 3:
			buffer.setAutoReloadDialog(false);
			buffer.setAutoReload(true);
			break;
		}
	} //}}}

	//{{{ ActionHandler() class
	private class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == mode)
			{
				Mode _mode = (Mode)mode.getSelectedItem();
				folding.setSelectedItem(_mode.getProperty(
					"folding"));
				wrap.setSelectedItem(_mode.getProperty(
					"wrap"));
				maxLineLen.setSelectedItem(_mode.getProperty(
					"maxLineLen"));
				tabSize.setSelectedItem(_mode.getProperty(
					"tabSize"));
				indentSize.setSelectedItem(_mode.getProperty(
					"indentSize"));
				noTabs.setSelected(_mode.getBooleanProperty(
					"noTabs"));
			}
		} //}}}
	} //}}}

}
