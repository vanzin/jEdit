/*
 * ModeOptionPane.java - Mode-specific options panel
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

public class ModeOptionPane extends AbstractOptionPane
{
	public ModeOptionPane()
	{
		super("mode");
	}

	// protected members
	protected void _init()
	{
		Mode[] modes = jEdit.getModes();
		String[] modeNames = new String[modes.length];
		modeProps = new ModeProperties[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			modeProps[i] = new ModeProperties(modes[i]);
			modeNames[i] = modes[i].getName();
		}
		mode = new JComboBox(modeNames);
		mode.addActionListener(new ActionHandler());

		addComponent(jEdit.getProperty("options.mode.mode"),mode);

		useDefaults = new JCheckBox(jEdit.getProperty("options.mode.useDefaults"));
		useDefaults.addActionListener(new ActionHandler());
		addComponent(useDefaults);

		addComponent(jEdit.getProperty("options.mode.filenameGlob"),
			filenameGlob = new JTextField());

		addComponent(jEdit.getProperty("options.mode.firstlineGlob"),
			firstlineGlob = new JTextField());

		String[] tabSizes = { "2", "4", "8" };
		addComponent(jEdit.getProperty("options.editing.tabSize"),
			tabSize = new JComboBox(tabSizes));
		tabSize.setEditable(true);

		addComponent(jEdit.getProperty("options.editing.indentSize"),
			indentSize = new JComboBox(tabSizes));
		indentSize.setEditable(true);

		String[] lineLens = { "0", "72", "76", "80" };
		addComponent(jEdit.getProperty("options.editing.maxLineLen"),
			maxLineLen = new JComboBox(lineLens));
		maxLineLen.setEditable(true);

		addComponent(jEdit.getProperty("options.editing.wordBreakChars"),
			wordBreakChars = new JTextField());

		addComponent(jEdit.getProperty("options.mode.commentStart"),
			commentStart = new JTextField());

		addComponent(jEdit.getProperty("options.mode.commentEnd"),
			commentEnd = new JTextField());

		addComponent(jEdit.getProperty("options.mode.lineComment"),
			lineComment = new JTextField());

		addComponent(jEdit.getProperty("options.mode.noWordSep"),
			noWordSep = new JTextField());

		addComponent(jEdit.getProperty("options.editing.collapseFolds"),
			collapseFolds = new JTextField());

		addComponent(syntax = new JCheckBox(jEdit.getProperty(
			"options.editing.syntax")));

		addComponent(indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab")));

		addComponent(indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter")));

		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs")));

		selectMode();
	}

	protected void _save()
	{
		saveMode();

		for(int i = 0; i < modeProps.length; i++)
		{
			modeProps[i].save();
		}
	}

	// private members
	private ModeProperties[] modeProps;
	private ModeProperties current;
	private JComboBox mode;
	private JCheckBox useDefaults;
	private JTextField filenameGlob;
	private JTextField firstlineGlob;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JComboBox maxLineLen;
	private JTextField wordBreakChars;
	private JTextField commentStart;
	private JTextField commentEnd;
	private JTextField lineComment;
	private JTextField noWordSep;
	private JTextField collapseFolds;
	private JCheckBox noTabs;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox syntax;

	private void saveMode()
	{
		current.useDefaults = useDefaults.isSelected();
		current.filenameGlob = filenameGlob.getText();
		current.firstlineGlob = firstlineGlob.getText();
		current.tabSize = (String)tabSize.getSelectedItem();
		current.indentSize = (String)indentSize.getSelectedItem();
		current.maxLineLen = (String)maxLineLen.getSelectedItem();
		current.wordBreakChars = wordBreakChars.getText();
		current.commentStart = commentStart.getText();
		current.commentEnd = commentEnd.getText();
		current.lineComment = lineComment.getText();
		current.noWordSep = noWordSep.getText();
		current.collapseFolds = collapseFolds.getText();
		current.noTabs = noTabs.isSelected();
		current.indentOnEnter = indentOnEnter.isSelected();
		current.indentOnTab = indentOnTab.isSelected();
		current.syntax = syntax.isSelected();
	}

	private void selectMode()
	{
		current = modeProps[mode.getSelectedIndex()];
		current.edited = true;
		current.load();

		useDefaults.setSelected(current.useDefaults);
		filenameGlob.setText(current.filenameGlob);
		firstlineGlob.setText(current.firstlineGlob);
		tabSize.setSelectedItem(current.tabSize);
		indentSize.setSelectedItem(current.indentSize);
		maxLineLen.setSelectedItem(current.maxLineLen);
		wordBreakChars.setText(current.wordBreakChars);
		commentStart.setText(current.commentStart);
		commentEnd.setText(current.commentEnd);
		lineComment.setText(current.lineComment);
		noWordSep.setText(current.noWordSep);
		collapseFolds.setText(current.collapseFolds);
		noTabs.setSelected(current.noTabs);
		indentOnTab.setSelected(current.indentOnTab);
		indentOnEnter.setSelected(current.indentOnEnter);
		syntax.setSelected(current.syntax);

		updateEnabled();
	}

	private void updateEnabled()
	{
		boolean enabled = !modeProps[mode.getSelectedIndex()].useDefaults;
		filenameGlob.setEnabled(enabled);
		firstlineGlob.setEnabled(enabled);
		tabSize.setEnabled(enabled);
		indentSize.setEnabled(enabled);
		maxLineLen.setEnabled(enabled);
		wordBreakChars.setEnabled(enabled);
		commentStart.setEnabled(enabled);
		commentEnd.setEnabled(enabled);
		lineComment.setEnabled(enabled);
		noWordSep.setEnabled(enabled);
		collapseFolds.setEnabled(enabled);
		noTabs.setEnabled(enabled);
		indentOnTab.setEnabled(enabled);
		indentOnEnter.setEnabled(enabled);
		syntax.setEnabled(enabled);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == mode)
			{
				saveMode();
				selectMode();
			}
			else if(evt.getSource() == useDefaults)
			{
				modeProps[mode.getSelectedIndex()].useDefaults =
					useDefaults.isSelected();
				updateEnabled();
			}
		}
	}

	class ModeProperties
	{
		Mode mode;
		boolean edited;
		boolean loaded;

		boolean useDefaults;
		String filenameGlob;
		String firstlineGlob;
		String tabSize;
		String indentSize;
		String maxLineLen;
		String wordBreakChars;
		String commentStart;
		String commentEnd;
		String lineComment;
		String noWordSep;
		String collapseFolds;
		boolean noTabs;
		boolean indentOnTab;
		boolean indentOnEnter;
		boolean syntax;

		ModeProperties(Mode mode)
		{
			this.mode = mode;
		}

		void load()
		{
			if(loaded)
				return;

			loaded = true;

			mode.loadIfNecessary();

			useDefaults = !jEdit.getBooleanProperty("mode."
				+ mode.getName() + ".customSettings");
			filenameGlob = (String)mode.getProperty("filenameGlob");
			firstlineGlob = (String)mode.getProperty("firstlineGlob");
			tabSize = mode.getProperty("tabSize").toString();
			indentSize = mode.getProperty("indentSize").toString();
			maxLineLen = mode.getProperty("maxLineLen").toString();
			wordBreakChars = (String)mode.getProperty("wordBreakChars");
			commentStart = (String)mode.getProperty("commentStart");
			commentEnd = (String)mode.getProperty("commentEnd");
			lineComment = (String)mode.getProperty("lineComment");
			noWordSep = (String)mode.getProperty("noWordSep");
			collapseFolds = mode.getProperty("collapseFolds").toString();
			noTabs = mode.getBooleanProperty("noTabs");
			indentOnTab = mode.getBooleanProperty("indentOnTab");
			indentOnEnter = mode.getBooleanProperty("indentOnEnter");
			syntax = mode.getBooleanProperty("syntax");
		}

		void save()
		{
			// don't do anything if the user didn't change
			// any settings
			if(!edited)
				return;

			String prefix = "mode." + mode.getName() + ".";
			jEdit.setBooleanProperty(prefix + "customSettings",!useDefaults);

			if(useDefaults)
			{
				jEdit.resetProperty(prefix + "filenameGlob");
				jEdit.resetProperty(prefix + "firstlineGlob");
				jEdit.resetProperty(prefix + "tabSize");
				jEdit.resetProperty(prefix + "indentSize");
				jEdit.resetProperty(prefix + "maxLineLen");
				jEdit.resetProperty(prefix + "wordBreakChars");
				jEdit.resetProperty(prefix + "commentStart");
				jEdit.resetProperty(prefix + "commentEnd");
				jEdit.resetProperty(prefix + "lineComment");
				jEdit.resetProperty(prefix + "noWordSep");
				jEdit.resetProperty(prefix + "collapseFolds");
				jEdit.resetProperty(prefix + "noTabs");
				jEdit.resetProperty(prefix + "indentOnTab");
				jEdit.resetProperty(prefix + "indentOnEnter");
				jEdit.resetProperty(prefix + "syntax");
			}
			else
			{
				jEdit.setProperty(prefix + "filenameGlob",filenameGlob);
				jEdit.setProperty(prefix + "firstlineGlob",firstlineGlob);
				jEdit.setProperty(prefix + "tabSize",tabSize);
				jEdit.setProperty(prefix + "indentSize",indentSize);
				jEdit.setProperty(prefix + "maxLineLen",maxLineLen);
				jEdit.setProperty(prefix + "wordBreakChars",wordBreakChars);
				jEdit.setProperty(prefix + "commentStart",commentStart);
				jEdit.setProperty(prefix + "commentEnd",commentEnd);
				jEdit.setProperty(prefix + "lineComment",lineComment);
				jEdit.setProperty(prefix + "noWordSep",noWordSep);
				jEdit.setProperty(prefix + "collapseFolds",collapseFolds);
				jEdit.setBooleanProperty(prefix + "noTabs",noTabs);
				jEdit.setBooleanProperty(prefix + "indentOnTab",indentOnTab);
				jEdit.setBooleanProperty(prefix + "indentOnEnter",indentOnEnter);
				jEdit.setBooleanProperty(prefix + "syntax",syntax);
			}
		}
	}
}
