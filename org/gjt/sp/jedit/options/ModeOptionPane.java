/*
 * ModeOptionPane.java - Mode-specific options panel
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

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

public class ModeOptionPane extends AbstractOptionPane
{
	//{{{ ModeOptionPane constructor
	public ModeOptionPane()
	{
		super("mode");
	} //}}}

	//{{{ _init() method
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

		addComponent(jEdit.getProperty("options.editing.noWordSep"),
			noWordSep = new JTextField());

		String[] foldModes = {
			"none",
			"indent",
			"explicit"
		};
		addComponent(jEdit.getProperty("options.editing.folding"),
			folding = new JComboBox(foldModes));

		addComponent(jEdit.getProperty("options.editing.collapseFolds"),
			collapseFolds = new JTextField());

		String[] lineLens = { "0", "72", "76", "80" };
		addComponent(jEdit.getProperty("options.editing.maxLineLen"),
			maxLineLen = new JComboBox(lineLens));
		maxLineLen.setEditable(true);

		addComponent(softWrap = new JCheckBox(jEdit.getProperty(
			"options.editing.softWrap")));

		addComponent(indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab")));

		addComponent(indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter")));

		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs")));

		selectMode();
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		saveMode();

		for(int i = 0; i < modeProps.length; i++)
		{
			modeProps[i].save();
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private ModeProperties[] modeProps;
	private ModeProperties current;
	private JComboBox mode;
	private JCheckBox useDefaults;
	private JTextField filenameGlob;
	private JTextField firstlineGlob;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JTextField noWordSep;
	private JComboBox folding;
	private JTextField collapseFolds;
	private JComboBox maxLineLen;
	private JCheckBox softWrap;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox noTabs;
	//}}}

	//{{{ saveMode() method
	private void saveMode()
	{
		current.useDefaults = useDefaults.isSelected();
		current.filenameGlob = filenameGlob.getText();
		current.firstlineGlob = firstlineGlob.getText();
		current.tabSize = (String)tabSize.getSelectedItem();
		current.indentSize = (String)indentSize.getSelectedItem();
		current.noWordSep = noWordSep.getText();
		current.folding = (String)folding.getSelectedItem();
		current.collapseFolds = collapseFolds.getText();
		current.maxLineLen = (String)maxLineLen.getSelectedItem();
		current.softWrap = softWrap.isSelected();
		current.indentOnEnter = indentOnEnter.isSelected();
		current.indentOnTab = indentOnTab.isSelected();
		current.noTabs = noTabs.isSelected();
	} //}}}

	//{{{ selectMode() method
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
		noWordSep.setText(current.noWordSep);
		folding.setSelectedItem(current.folding);
		collapseFolds.setText(current.collapseFolds);
		maxLineLen.setSelectedItem(current.maxLineLen);
		softWrap.setSelected(current.softWrap);
		indentOnTab.setSelected(current.indentOnTab);
		indentOnEnter.setSelected(current.indentOnEnter);
		noTabs.setSelected(current.noTabs);

		updateEnabled();
	} //}}}

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		boolean enabled = !modeProps[mode.getSelectedIndex()].useDefaults;
		filenameGlob.setEnabled(enabled);
		firstlineGlob.setEnabled(enabled);
		tabSize.setEnabled(enabled);
		indentSize.setEnabled(enabled);
		noWordSep.setEnabled(enabled);
		folding.setEnabled(enabled);
		collapseFolds.setEnabled(enabled);
		maxLineLen.setEnabled(enabled);
		softWrap.setEnabled(enabled);
		indentOnTab.setEnabled(enabled);
		indentOnEnter.setEnabled(enabled);
		noTabs.setEnabled(enabled);
	} //}}}

	//}}}

	//{{{ ActionHandler class
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
	} //}}}

	//{{{ ModeProperties class
	class ModeProperties
	{
		//{{{ Instance variables
		Mode mode;
		boolean edited;
		boolean loaded;

		boolean useDefaults;
		String filenameGlob;
		String firstlineGlob;
		String tabSize;
		String indentSize;
		String noWordSep;
		String folding;
		String collapseFolds;
		String maxLineLen;
		boolean softWrap;
		boolean indentOnTab;
		boolean indentOnEnter;
		boolean noTabs;
		//}}}

		//{{{ ModeProperties constructor
		ModeProperties(Mode mode)
		{
			this.mode = mode;
		} //}}}

		//{{{ load() method
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
			noWordSep = (String)mode.getProperty("noWordSep");
			folding = mode.getProperty("folding").toString();
			collapseFolds = mode.getProperty("collapseFolds").toString();
			maxLineLen = mode.getProperty("maxLineLen").toString();
			softWrap = mode.getBooleanProperty("softWrap");
			indentOnTab = mode.getBooleanProperty("indentOnTab");
			indentOnEnter = mode.getBooleanProperty("indentOnEnter");
			noTabs = mode.getBooleanProperty("noTabs");
		} //}}}

		//{{{ save() method
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
				jEdit.resetProperty(prefix + "noWordSep");
				jEdit.resetProperty(prefix + "folding");
				jEdit.resetProperty(prefix + "collapseFolds");
				jEdit.resetProperty(prefix + "maxLineLen");
				jEdit.resetProperty(prefix + "softWrap");
				jEdit.resetProperty(prefix + "indentOnTab");
				jEdit.resetProperty(prefix + "indentOnEnter");
				jEdit.resetProperty(prefix + "noTabs");
			}
			else
			{
				jEdit.setProperty(prefix + "filenameGlob",filenameGlob);
				jEdit.setProperty(prefix + "firstlineGlob",firstlineGlob);
				jEdit.setProperty(prefix + "tabSize",tabSize);
				jEdit.setProperty(prefix + "indentSize",indentSize);
				jEdit.setProperty(prefix + "noWordSep",noWordSep);
				jEdit.setProperty(prefix + "folding",folding);
				jEdit.setProperty(prefix + "collapseFolds",collapseFolds);
				jEdit.setProperty(prefix + "maxLineLen",maxLineLen);
				jEdit.setBooleanProperty(prefix + "softWrap",softWrap);
				jEdit.setBooleanProperty(prefix + "indentOnTab",indentOnTab);
				jEdit.setBooleanProperty(prefix + "indentOnEnter",indentOnEnter);
				jEdit.setBooleanProperty(prefix + "noTabs",noTabs);
			}
		} //}}}
	} //}}}
}
