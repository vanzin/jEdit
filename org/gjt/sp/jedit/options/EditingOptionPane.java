/*
 * EditingOptionPane.java - Editing options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.FoldHandler;

public class EditingOptionPane extends AbstractOptionPane
{
	//{{{ EditingOptionPane constructor
	public EditingOptionPane()
	{
		super("editing");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		//{{{ Modes
		Mode[] modes = jEdit.getModes();
		String defaultModeString = jEdit.getProperty("buffer.defaultMode");
		String[] modeNames = new String[modes.length];
		int index = 0;
		for(int i = 0; i < modes.length; i++)
		{
			Mode _mode = modes[i];
			modeNames[i] = _mode.getName();
			if(defaultModeString.equals(_mode.getName()))
				index = i;
		}
		defaultMode = new JComboBox(modeNames);
		defaultMode.setSelectedIndex(index);
		addComponent(jEdit.getProperty("options.editing.defaultMode"),
			defaultMode);
		//}}}

		//{{{ Undo queue size
		undoCount = new JTextField(jEdit.getProperty("buffer.undoCount"));
		addComponent(jEdit.getProperty("options.editing.undoCount"),undoCount);
		//}}}

		//{{{ Extra word characters
		defaultNoWordSep = new JTextField(jEdit.getProperty("buffer.noWordSep"));
		addComponent(jEdit.getProperty("options.editing.noWordSep"),defaultNoWordSep);
		//}}}

		//{{{ Default fold mode
		String[] foldModes = FoldHandler.getFoldModes();
		addComponent(jEdit.getProperty("options.editing.folding"),
			defaultFolding = new JComboBox(foldModes));

		defaultFolding.setSelectedItem(jEdit.getProperty("buffer.folding"));
		//}}}

		//{{{ Default fold level
		defaultCollapseFolds = new JTextField(jEdit.getProperty("buffer.collapseFolds"));
		addComponent(jEdit.getProperty("options.editing.collapseFolds"),defaultCollapseFolds);
		//}}}

		//{{{ Default wrap mode
		String[] wrapModes = {
			"none",
			"soft",
			"hard"
		};
		addComponent(jEdit.getProperty("options.editing.wrap"),
			defaultWrap = new JComboBox(wrapModes));

		defaultWrap.setSelectedItem(jEdit.getProperty("buffer.wrap"));
		//}}}

		//{{{ Max line length
		String[] lineLens = { "0", "72", "76", "80" };
		defaultMaxLineLen = new JComboBox(lineLens);
		defaultMaxLineLen.setEditable(true);
		defaultMaxLineLen.setSelectedItem(jEdit.getProperty("buffer.maxLineLen"));
		addComponent(jEdit.getProperty("options.editing.maxLineLen"),defaultMaxLineLen);
		//}}}

		//{{{ Tab size
		String[] tabSizes = { "2", "4", "8" };
		defaultTabSize = new JComboBox(tabSizes);
		defaultTabSize.setEditable(true);
		defaultTabSize.setSelectedItem(jEdit.getProperty("buffer.tabSize"));
		addComponent(jEdit.getProperty("options.editing.tabSize"),defaultTabSize);

		//}}}

		//{{{ Indent size
		defaultIndentSize = new JComboBox(tabSizes);
		defaultIndentSize.setEditable(true);
		defaultIndentSize.setSelectedItem(jEdit.getProperty("buffer.indentSize"));
		addComponent(jEdit.getProperty("options.editing.indentSize"),defaultIndentSize);
		//}}}

		//{{{ Soft tabs
		defaultNoTabs = new JCheckBox(jEdit.getProperty("options.editing"
			+ ".noTabs"));
		defaultNoTabs.setSelected(jEdit.getBooleanProperty("buffer.noTabs"));
		addComponent(defaultNoTabs);
		//}}}

		//{{{ Indent on tab
		defaultIndentOnTab = new JCheckBox(jEdit.getProperty("options.editing"
			+ ".indentOnTab"));
		defaultIndentOnTab.setSelected(jEdit.getBooleanProperty("buffer.indentOnTab"));
		addComponent(defaultIndentOnTab);
		//}}}

		//{{{ Indent on enter
		defaultIndentOnEnter = new JCheckBox(jEdit.getProperty("options.editing"
			+ ".indentOnEnter"));
		defaultIndentOnEnter.setSelected(jEdit.getBooleanProperty("buffer.indentOnEnter"));
		addComponent(defaultIndentOnEnter);
		//}}}
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		jEdit.setProperty("buffer.defaultMode",
			(String)defaultMode.getSelectedItem());
		jEdit.setProperty("buffer.undoCount",undoCount.getText());
		jEdit.setProperty("buffer.noWordSep",defaultNoWordSep.getText());
		jEdit.setProperty("buffer.folding",(String)defaultFolding.getSelectedItem());
		jEdit.setProperty("buffer.collapseFolds",defaultCollapseFolds.getText());
		jEdit.setProperty("buffer.wrap",(String)defaultWrap.getSelectedItem());
		jEdit.setProperty("buffer.maxLineLen",(String)defaultMaxLineLen.getSelectedItem());
		jEdit.setProperty("buffer.tabSize",(String)defaultTabSize
			.getSelectedItem());
		jEdit.setProperty("buffer.indentSize",(String)defaultIndentSize
			.getSelectedItem());
		jEdit.setBooleanProperty("buffer.noTabs",defaultNoTabs.isSelected());
		jEdit.setBooleanProperty("buffer.indentOnTab",defaultIndentOnTab
			.isSelected());
		jEdit.setBooleanProperty("buffer.indentOnEnter",defaultIndentOnEnter
			.isSelected());
	} //}}}

	//{{{ Private members
	private JComboBox defaultMode;
	private JTextField undoCount;
	private JTextField defaultNoWordSep;
	private JComboBox defaultFolding;
	private JTextField defaultCollapseFolds;
	private JComboBox defaultWrap;
	private JComboBox defaultMaxLineLen;
	private JComboBox defaultTabSize;
	private JComboBox defaultIndentSize;
	private JCheckBox defaultNoTabs;
	private JCheckBox defaultIndentOnTab;
	private JCheckBox defaultIndentOnEnter;
	//}}}
}
