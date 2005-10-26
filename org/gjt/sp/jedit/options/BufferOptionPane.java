package org.gjt.sp.jedit.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.buffer.FoldHandler;


public class BufferOptionPane extends AbstractOptionPane
{
	JComboBox encoding;
	JComboBox lineSeparator;
	JCheckBox gzipped;
	Mode[] modes;
	JComboBox mode;
	JComboBox folding;
	JComboBox wrap;
	JComboBox maxLineLen;
	JComboBox tabSize;
	JComboBox indentSize;
	JCheckBox noTabs;
	Buffer buffer;
	
	
	BufferOptionPane() 
	{
		super("buffer");
		init();
	}
	
	protected void _init() {
		
		
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
		String lineSep = buffer.getStringProperty(Buffer.LINESEP);
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
		String[] encodings = MiscUtilities.getEncodings();
		Arrays.sort(encodings,new MiscUtilities.StringICaseCompare());
		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(buffer.getStringProperty(Buffer.ENCODING));
		addComponent(jEdit.getProperty("buffer-options.encoding"),
			encoding);
		//}}}

		//{{{ GZipped setting
		gzipped = new JCheckBox(jEdit.getProperty(
			"buffer-options.gzipped"));
		gzipped.setSelected(buffer.getBooleanProperty(Buffer.GZIPPED));
		addComponent(gzipped);
		//}}}

		addSeparator("buffer-options.editing");

		//{{{ Edit mode
		modes = jEdit.getModes();
		MiscUtilities.quicksort(modes,new MiscUtilities.StringICaseCompare());
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

	}
	

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
	}

	
	class ActionHandler implements ActionListener
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
