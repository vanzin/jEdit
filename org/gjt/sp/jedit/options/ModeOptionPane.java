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
import org.gjt.sp.jedit.buffer.FoldHandler;
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

		addComponent(jEdit.getProperty("options.editing.noWordSep"),
			noWordSep = new JTextField());

		String[] foldModes = FoldHandler.getFoldModes();
		addComponent(jEdit.getProperty("options.editing.folding"),
			folding = new JComboBox(foldModes));

		addComponent(jEdit.getProperty("options.editing.collapseFolds"),
			collapseFolds = new JTextField());

		String[] wrapModes = {
			"none",
			"soft",
			"hard"
		};
		addComponent(jEdit.getProperty("options.editing.wrap"),
			wrap = new JComboBox(wrapModes));

		String[] lineLens = { "0", "72", "76", "80" };
		addComponent(jEdit.getProperty("options.editing.maxLineLen"),
			maxLineLen = new JComboBox(lineLens));
		maxLineLen.setEditable(true);

		String[] tabSizes = { "2", "4", "8" };
		addComponent(jEdit.getProperty("options.editing.tabSize"),
			tabSize = new JComboBox(tabSizes));
		tabSize.setEditable(true);

		addComponent(jEdit.getProperty("options.editing.indentSize"),
			indentSize = new JComboBox(tabSizes));
		indentSize.setEditable(true);

		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs")));

		addComponent(indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnTab")));

		addComponent(indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editing.indentOnEnter")));

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
	private JTextField noWordSep;
	private JComboBox folding;
	private JTextField collapseFolds;
	private JComboBox wrap;
	private JComboBox maxLineLen;
	private JComboBox tabSize;
	private JComboBox indentSize;
	private JCheckBox noTabs;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	//}}}

	//{{{ saveMode() method
	private void saveMode()
	{
		current.useDefaults = useDefaults.isSelected();
		current.filenameGlob = filenameGlob.getText();
		current.firstlineGlob = firstlineGlob.getText();
		current.noWordSep = noWordSep.getText();
		current.folding = (String)folding.getSelectedItem();
		current.collapseFolds = collapseFolds.getText();
		current.wrap = (String)wrap.getSelectedItem();
		current.maxLineLen = (String)maxLineLen.getSelectedItem();
		current.tabSize = (String)tabSize.getSelectedItem();
		current.indentSize = (String)indentSize.getSelectedItem();
		current.noTabs = noTabs.isSelected();
		current.indentOnEnter = indentOnEnter.isSelected();
		current.indentOnTab = indentOnTab.isSelected();
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
		noWordSep.setText(current.noWordSep);
		folding.setSelectedItem(current.folding);
		collapseFolds.setText(current.collapseFolds);
		wrap.setSelectedItem(current.wrap);
		maxLineLen.setSelectedItem(current.maxLineLen);
		tabSize.setSelectedItem(current.tabSize);
		indentSize.setSelectedItem(current.indentSize);
		noTabs.setSelected(current.noTabs);
		indentOnTab.setSelected(current.indentOnTab);
		indentOnEnter.setSelected(current.indentOnEnter);

		updateEnabled();
	} //}}}

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		boolean enabled = !modeProps[mode.getSelectedIndex()].useDefaults;
		filenameGlob.setEnabled(enabled);
		firstlineGlob.setEnabled(enabled);
		noWordSep.setEnabled(enabled);
		folding.setEnabled(enabled);
		collapseFolds.setEnabled(enabled);
		wrap.setEnabled(enabled);
		maxLineLen.setEnabled(enabled);
		tabSize.setEnabled(enabled);
		indentSize.setEnabled(enabled);
		noTabs.setEnabled(enabled);
		indentOnTab.setEnabled(enabled);
		indentOnEnter.setEnabled(enabled);
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
		String noWordSep;
		String folding;
		String collapseFolds;
		String wrap;
		String maxLineLen;
		String tabSize;
		String indentSize;
		boolean noTabs;
		boolean indentOnTab;
		boolean indentOnEnter;
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
			noWordSep = (String)mode.getProperty("noWordSep");
			folding = mode.getProperty("folding").toString();
			collapseFolds = mode.getProperty("collapseFolds").toString();
			wrap = mode.getProperty("wrap").toString();
			maxLineLen = mode.getProperty("maxLineLen").toString();
			tabSize = mode.getProperty("tabSize").toString();
			indentSize = mode.getProperty("indentSize").toString();
			noTabs = mode.getBooleanProperty("noTabs");
			indentOnTab = mode.getBooleanProperty("indentOnTab");
			indentOnEnter = mode.getBooleanProperty("indentOnEnter");
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
				jEdit.resetProperty(prefix + "noWordSep");
				jEdit.resetProperty(prefix + "folding");
				jEdit.resetProperty(prefix + "collapseFolds");
				jEdit.resetProperty(prefix + "wrap");
				jEdit.resetProperty(prefix + "maxLineLen");
				jEdit.resetProperty(prefix + "tabSize");
				jEdit.resetProperty(prefix + "indentSize");
				jEdit.resetProperty(prefix + "noTabs");
				jEdit.resetProperty(prefix + "indentOnTab");
				jEdit.resetProperty(prefix + "indentOnEnter");
			}
			else
			{
				jEdit.setProperty(prefix + "filenameGlob",filenameGlob);
				jEdit.setProperty(prefix + "firstlineGlob",firstlineGlob);
				jEdit.setProperty(prefix + "noWordSep",noWordSep);
				jEdit.setProperty(prefix + "folding",folding);
				jEdit.setProperty(prefix + "collapseFolds",collapseFolds);
				jEdit.setProperty(prefix + "wrap",wrap);
				jEdit.setProperty(prefix + "maxLineLen",maxLineLen);
				jEdit.setProperty(prefix + "tabSize",tabSize);
				jEdit.setProperty(prefix + "indentSize",indentSize);
				jEdit.setBooleanProperty(prefix + "noTabs",noTabs);
				jEdit.setBooleanProperty(prefix + "indentOnTab",indentOnTab);
				jEdit.setBooleanProperty(prefix + "indentOnEnter",indentOnEnter);
			}
		} //}}}
	} //}}}
}
