/*
 * ModeSettingsPane.java - Mode-specific options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2002 Slava Pestov
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

import java.util.Arrays;
import java.util.Objects;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.buffer.WordWrap;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * Option pane to change the settings of an edit mode.
 * @author Slava Pestov
 * @version $Id: ModeSettingsPane.java 24012 2015-08-12 08:48:07Z kpouer $
 */
public class ModeSettingsPane extends AbstractOptionPane
{
	//{{{ ModeSettingsPane constructor
	public ModeSettingsPane()
	{
		super("modesettings");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		Mode[] modes = reloadModes();
		mode = new JComboBox<>(modeNames);
		mode.addActionListener(e ->
		{
			saveMode();
			selectMode();
		});

		captionBox = new Box(BoxLayout.X_AXIS);
		addComponent(captionBox);

		addComponent(jEdit.getProperty("options.editing.mode"),mode);

		useDefaults = new JCheckBox(jEdit.getProperty("options.editing.useDefaults"));
		useDefaults.addActionListener(e ->
		{
			modeProps[mode.getSelectedIndex() - 1].useDefaults = useDefaults.isSelected();
			updateEnabled();
		});
		addComponent(useDefaults);

		addComponent(jEdit.getProperty("options.editing.noWordSep"),
			noWordSep = new JTextField());

		addComponent(camelCasedWords = new JCheckBox(jEdit.getProperty(
			"options.editing.camelCasedWords")));

		String[] foldModes = FoldHandler.getFoldModes();
		addComponent(jEdit.getProperty("options.editing.folding"),
			folding = new JComboBox<>(foldModes));

		addComponent(jEdit.getProperty("options.editing.collapseFolds"),
			collapseFolds = new JTextField());

		addComponent(jEdit.getProperty("options.editing.wrap"),
			wrap = new JComboBox<>(WordWrap.values()));

		String[] lineLens = { "0", "72", "76", "80" };
		maxLineLen = new JComboBox<>(lineLens);
		maxLineLen.setToolTipText(jEdit.getProperty("options.editing.maxLineLen.tooltip"));
		addComponent(jEdit.getProperty("options.editing.maxLineLen"), maxLineLen);
		maxLineLen.setEditable(true);

		String[] indentModes = {
			"none",
			"simple",
			"full"
		};
		addComponent(jEdit.getProperty("options.editing.autoIndent"),
			autoIndent = new JComboBox<>(indentModes));


		String[] tabSizes = { "2", "4", "8" };
		addComponent(jEdit.getProperty("options.editing.tabSize"),
			tabSize = new JComboBox<>(tabSizes));

		tabSize.setEditable(true);

		addComponent(jEdit.getProperty("options.editing.indentSize"),
			indentSize = new JComboBox<>(tabSizes));
		indentSize.setEditable(true);


		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editing.noTabs")));

		addComponent(elasticTabstops = new JCheckBox(jEdit.getProperty(
		"options.editing.elasticTabstops")));
		elasticTabstops.setToolTipText(jEdit.getProperty("options.editing.elasticTabstops.tooltip"));

		addComponent(deepIndent = new JCheckBox(jEdit.getProperty(
			"options.editing.deepIndent")));

		filenameGlob = new JTextField();
		filenameGlob.setToolTipText(jEdit.getProperty("glob.tooltip"));
		addComponent(jEdit.getProperty("options.editing.filenameGlob"),
			filenameGlob);

		addComponent(jEdit.getProperty("options.editing.firstlineGlob"),
			firstlineGlob = new JTextField());
		
		selectMode();

	} //}}}
	
	private Mode[] reloadModes()
	{
		Mode[] modes = jEdit.getModes();
		Arrays.sort(modes,new StandardUtilities.StringCompare<>(true));

		global = new ModeProperties();
		modeProps = new ModeProperties[modes.length];

		modeNames = new String[modes.length + 1];
		modeNames[0] = jEdit.getProperty("options.editing.global");

		for(int i = 0; i < modes.length; i++)
		{
			modeProps[i] = new ModeProperties(modes[i]);
			modeNames[i + 1] = modes[i].getName();
		}
		return modes;
	}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		saveMode();

		global.save();

		for (ModeProperties modeProp : modeProps)
		{
			modeProp.save();
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private ModeProperties global;
	private ModeProperties[] modeProps;
	private ModeProperties current;
	private Box captionBox;
	private JComboBox<String> mode;
	private JCheckBox useDefaults;
	private JTextField filenameGlob;
	private JTextField firstlineGlob;
	private JTextField noWordSep;
	private JCheckBox camelCasedWords;
	private JComboBox<String> folding;
	private JTextField collapseFolds;
	private JComboBox<WordWrap> wrap;
	private JComboBox<String> maxLineLen;
	private JComboBox<String> tabSize;
	private JComboBox<String> indentSize;
	private JCheckBox noTabs;
	private JCheckBox elasticTabstops;
	private JComboBox<String> autoIndent;
	private JCheckBox deepIndent;
	private String[] modeNames;
	//}}}

	//{{{ saveMode() method
	private void saveMode()
	{
		current.useDefaults = useDefaults.isSelected();
		current.filenameGlob = filenameGlob.getText();
		current.firstlineGlob = firstlineGlob.getText();
		current.noWordSep = noWordSep.getText();
		current.camelCasedWords = camelCasedWords.isSelected();
		current.folding = (String)folding.getSelectedItem();
		current.collapseFolds = collapseFolds.getText();
		current.wrap = (WordWrap) wrap.getSelectedItem();
		current.maxLineLen = (String)maxLineLen.getSelectedItem();
		current.tabSize = (String)tabSize.getSelectedItem();
		current.indentSize = (String)indentSize.getSelectedItem();
		current.noTabs = noTabs.isSelected();
		current.elasticTabstops = elasticTabstops.isSelected();
		current.autoIndent = (String)autoIndent.getSelectedItem();
		current.deepIndent = deepIndent.isSelected();
	} //}}}

	//{{{ selectMode() method
	private void selectMode()
	{
		int index = Math.max(mode.getSelectedIndex(), 0);
		current = index == 0 ? global : modeProps[index - 1];
		current.edited = true;
		current.load();

		captionBox.removeAll();
		captionBox.add(GUIUtilities.createMultilineLabel(
			jEdit.getProperty("options.editing.caption-"
			+ (index == 0 ? "0" : "1"))));

		useDefaults.setSelected(current.useDefaults);
		filenameGlob.setText(current.filenameGlob);
		firstlineGlob.setText(current.firstlineGlob);
		noWordSep.setText(current.noWordSep);
		camelCasedWords.setSelected(current.camelCasedWords);
		folding.setSelectedItem(current.folding);
		collapseFolds.setText(current.collapseFolds);
		wrap.setSelectedItem(current.wrap);
		maxLineLen.setSelectedItem(current.maxLineLen);
		tabSize.setSelectedItem(current.tabSize);
		indentSize.setSelectedItem(current.indentSize);
		noTabs.setSelected(current.noTabs);
		elasticTabstops.setSelected(current.elasticTabstops);
		autoIndent.setSelectedItem(current.autoIndent);
		deepIndent.setSelected(current.deepIndent);

		updateEnabled();
		revalidate();
	} //}}}

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		boolean enabled;
		if(current == global)
		{
			enabled = true;
			useDefaults.setEnabled(false);
			filenameGlob.setEnabled(false);
			firstlineGlob.setEnabled(false);
		}
		else
		{
			enabled = !modeProps[mode.getSelectedIndex() - 1]
				.useDefaults;
			useDefaults.setEnabled(true);
			filenameGlob.setEnabled(enabled);
			firstlineGlob.setEnabled(enabled);
		}

		noWordSep.setEnabled(enabled);
		camelCasedWords.setEnabled(enabled);
		folding.setEnabled(enabled);
		collapseFolds.setEnabled(enabled);
		wrap.setEnabled(enabled);
		maxLineLen.setEnabled(enabled);
		tabSize.setEnabled(enabled);
		indentSize.setEnabled(enabled);
		noTabs.setEnabled(enabled);
		elasticTabstops.setEnabled(enabled);
		autoIndent.setEnabled(enabled);
		deepIndent.setEnabled(enabled);
	} //}}}

	//}}}

	//{{{ ModeProperties class
	private static class ModeProperties
	{
		//{{{ Instance variables
		Mode mode;
		boolean edited;
		boolean loaded;

		boolean useDefaults;
		String filenameGlob;
		String firstlineGlob;
		String noWordSep;
		boolean camelCasedWords;
		String folding;
		String collapseFolds;
		WordWrap wrap;
		String maxLineLen;
		String tabSize;
		String indentSize;
		boolean noTabs;
		boolean elasticTabstops;
		String autoIndent;
		boolean deepIndent;
		boolean isUserMode;
		//}}}

		//{{{ ModeProperties constructor
		ModeProperties()
		{
		} //}}}

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

			if(mode != null)
			{
				mode.loadIfNecessary();

				useDefaults = !jEdit.getBooleanProperty("mode."
					+ mode.getName() + ".customSettings");
				filenameGlob = (String)mode.getProperty("filenameGlob");
				firstlineGlob = (String)mode.getProperty("firstlineGlob");
				noWordSep = (String)mode.getProperty("noWordSep");
				camelCasedWords = mode.getBooleanProperty("camelCasedWords");
				folding = mode.getProperty("folding").toString();
				collapseFolds = mode.getProperty("collapseFolds").toString();
				wrap = WordWrap.valueOf(mode.getProperty(JEditBuffer.WRAP).toString());
				maxLineLen = mode.getProperty("maxLineLen").toString();
				tabSize = mode.getProperty("tabSize").toString();
				indentSize = mode.getProperty("indentSize").toString();
				noTabs = mode.getBooleanProperty("noTabs");
				elasticTabstops = mode.getBooleanProperty("elasticTabstops");
				autoIndent = mode.getProperty("autoIndent").toString();
				deepIndent = mode.getBooleanProperty("deepIndent");
				isUserMode = mode.isUserMode();
			}
			else
			{
				noWordSep = jEdit.getProperty("buffer.noWordSep");
				camelCasedWords = jEdit.getBooleanProperty("buffer.camelCasedWords");
				folding = jEdit.getProperty("buffer.folding");
				collapseFolds = jEdit.getProperty("buffer.collapseFolds");
				wrap = WordWrap.valueOf(jEdit.getProperty("buffer.wrap"));
				maxLineLen = jEdit.getProperty("buffer.maxLineLen");
				tabSize = jEdit.getProperty("buffer.tabSize");
				indentSize = jEdit.getProperty("buffer.indentSize");
				noTabs = jEdit.getBooleanProperty("buffer.noTabs");
				elasticTabstops= jEdit.getBooleanProperty("buffer.elasticTabstops");
				autoIndent = jEdit.getProperty("buffer.autoIndent");
				deepIndent = jEdit.getBooleanProperty("buffer.deepIndent");
			}
		} //}}}

		//{{{ save() method
		void save()
		{
			// don't do anything if the user didn't change
			// any settings
			if(!edited)
				return;

			String prefix;
			if(mode != null)
			{
				prefix = "mode." + mode.getName() + '.';
				jEdit.setBooleanProperty(prefix + "customSettings",!useDefaults);

				// need to call Mode.init() if the file name or first line
				// globs change
				String oldFilenameGlob = (String)mode.getProperty("filenameGlob");
				String oldFirstlineGlob = (String)mode.getProperty("firstlineGlob");
				if(useDefaults)
				{
					jEdit.resetProperty(prefix + "filenameGlob");
					jEdit.resetProperty(prefix + "firstlineGlob");
					jEdit.resetProperty(prefix + "noWordSep");
					jEdit.resetProperty(prefix + "camelCasedWords");
					jEdit.resetProperty(prefix + "folding");
					jEdit.resetProperty(prefix + "collapseFolds");
					jEdit.resetProperty(prefix + JEditBuffer.WRAP);
					jEdit.resetProperty(prefix + "maxLineLen");
					jEdit.resetProperty(prefix + "tabSize");
					jEdit.resetProperty(prefix + "indentSize");
					jEdit.resetProperty(prefix + "noTabs");
					jEdit.resetProperty(prefix + "elasticTabstops");
					jEdit.resetProperty(prefix + "autoIndent");
					jEdit.resetProperty(prefix + "deepIndent");

					if(!(Objects.equals(oldFilenameGlob, mode.getProperty("filenameGlob"))
						&& Objects.equals(oldFirstlineGlob, mode.getProperty("firstlineGlob"))))
					{
						mode.init();
					}

					return;
				}
				else
				{
					jEdit.setProperty(prefix + "filenameGlob",filenameGlob);
					jEdit.setProperty(prefix + "firstlineGlob",firstlineGlob);

					if(!(Objects.equals(oldFilenameGlob, filenameGlob) &&
						 Objects.equals(oldFirstlineGlob, firstlineGlob)))
					{
						mode.init();
					}
				}
			}
			else
			{
				prefix = "buffer.";
			}

			jEdit.setProperty(prefix + "noWordSep",noWordSep);
			jEdit.setBooleanProperty(prefix + "camelCasedWords",camelCasedWords);
			jEdit.setProperty(prefix + "folding",folding);
			jEdit.setProperty(prefix + "collapseFolds",collapseFolds);
			jEdit.setProperty(prefix + "wrap", wrap.name());
			jEdit.setProperty(prefix + "maxLineLen",maxLineLen);
			jEdit.setProperty(prefix + "tabSize",tabSize);
			jEdit.setProperty(prefix + "indentSize",indentSize);
			jEdit.setProperty(prefix + "autoIndent", autoIndent);
			jEdit.setBooleanProperty(prefix + "noTabs",noTabs);
			jEdit.setBooleanProperty(prefix + "elasticTabstops",elasticTabstops);
			jEdit.setBooleanProperty(prefix + "deepIndent",deepIndent);
		} //}}}
	} //}}}
}
