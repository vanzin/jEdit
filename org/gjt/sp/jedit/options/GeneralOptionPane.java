/*
 * GeneralOptionPane.java - General options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
 *               2013 Thomas Meyer
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
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//}}}

/**
 * jEdit's General Options Pane
 */

public class GeneralOptionPane extends AbstractOptionPane
{
	//{{{ checkFileStatus bit flags:
	public static final int checkFileStatus_none = 0;

	/** Check the buffer status when the view gets focus (low bit) */
	public static final int checkFileStatus_focus = 1;

	/** Check the file status when visiting the buffer (second bit) */
	public static final int checkFileStatus_focusBuffer = 2;

	/** This is actually a bitwise OR: (view focus | buffer focus) */ 
	public static final int checkFileStatus_all = 3;	
	//}}}

	//{{{ Private members

	private JComboBox<String> checkModStatus;
	private JComboBox<String> checkModStatusUpon;
	private JSpinner recentFiles;
	private JSpinner hypersearchResultsWarning;
	private JCheckBox saveCaret;
	private JCheckBox sortRecent;
	private JCheckBox hideOpen;
	private JCheckBox persistentMarkers;
	private JCheckBox restore;
	private JCheckBox restoreRemote;
	private JCheckBox restoreCLI;
	private JCheckBox restoreSplits;

	private JCheckBox useDefaultLocale;
	private JComboBox<String> lang;
	
	private JCheckBox closeAllConfirm;
	//}}}

	//{{{ GeneralOptionPane constructor
	public GeneralOptionPane()
	{
		super("general");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{

		/* Check mod status */
		String[] modCheckOptions = {
				jEdit.getProperty("options.general.checkModStatus.nothing"),
				jEdit.getProperty("options.general.checkModStatus.prompt"),
				jEdit.getProperty("options.general.checkModStatus.reload"),
				jEdit.getProperty("options.general.checkModStatus.silentReload")
		};
		checkModStatus = new JComboBox<String>(modCheckOptions);
		if(jEdit.getBooleanProperty("autoReload"))
		{
			if (jEdit.getBooleanProperty("autoReloadDialog"))
				// reload and notify
				checkModStatus.setSelectedIndex(2);
			else	// reload silently
				checkModStatus.setSelectedIndex(3);
		}
		else
		{
			if (jEdit.getBooleanProperty("autoReloadDialog"))
				// prompt
				checkModStatus.setSelectedIndex(1);
			else	// do nothing
				checkModStatus.setSelectedIndex(0);
		}
		addComponent(jEdit.getProperty("options.general.checkModStatus"),
				checkModStatus);

		/* Check mod status upon */
		String[] modCheckUponOptions = {
				jEdit.getProperty("options.general.checkModStatusUpon.none"),
				jEdit.getProperty("options.general.checkModStatusUpon.focus"),
				jEdit.getProperty("options.general.checkModStatusUpon.visitBuffer"),
				jEdit.getProperty("options.general.checkModStatusUpon.all")
		};
		checkModStatusUpon = new JComboBox<String>(modCheckUponOptions);

		checkModStatusUpon.setSelectedIndex(jEdit.getIntegerProperty("checkFileStatus"));
		addComponent(jEdit.getProperty("options.general.checkModStatusUpon"),
				checkModStatusUpon);

		/* Recent file list size */
		{
			String recentFilesLabel = jEdit.getProperty("options.general.recentFiles");
			int recentFilesValue = jEdit.getIntegerProperty("recentFiles");
			SpinnerModel model = new SpinnerNumberModel(recentFilesValue, 0, Integer.MAX_VALUE, 1);
			recentFiles = new JSpinner(model);
			addComponent(recentFilesLabel, recentFiles);
		}

		/* Sort recent file list */
		sortRecent = new JCheckBox(jEdit.getProperty(
				"options.general.sortRecent"));
		sortRecent.setSelected(jEdit.getBooleanProperty("sortRecent"));
		addComponent(sortRecent);

		/* Hide open buffers recent file list */
		hideOpen = new JCheckBox(jEdit.getProperty(
				"options.general.hideOpen"));
		hideOpen.setSelected(jEdit.getBooleanProperty("hideOpen", true));
		addComponent(hideOpen);
		
		/* Close all confirmation */
		closeAllConfirm = new JCheckBox(jEdit.getProperty(
				"options.general.closeAllConfirm"));
		closeAllConfirm.setSelected(jEdit.getBooleanProperty("closeAllConfirm", false));
		addComponent(closeAllConfirm);

		/* Save caret positions */
		saveCaret = new JCheckBox(jEdit.getProperty(
				"options.general.saveCaret"));
		saveCaret.setSelected(jEdit.getBooleanProperty("saveCaret"));
		addComponent(saveCaret);

		/* Persistent markers */
		persistentMarkers = new JCheckBox(jEdit.getProperty(
				"options.general.persistentMarkers"));
		persistentMarkers.setSelected(jEdit.getBooleanProperty(
				"persistentMarkers"));
		addComponent(persistentMarkers);

		/* Session management */
		restore = new JCheckBox(jEdit.getProperty(
				"options.general.restore"));

		restore.setSelected(jEdit.getBooleanProperty("restore"));
		restore.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				restoreCLI.setEnabled(restore.isSelected());
				restoreRemote.setEnabled(restore.isSelected());
			}
		});

		addComponent(restore);

		restoreRemote = new JCheckBox(jEdit.getProperty(
				"options.general.restore.remote"));
		restoreRemote.setSelected(jEdit.getBooleanProperty("restore.remote", false));
		restoreRemote.setEnabled(restore.isSelected());
		addComponent(restoreRemote);

		restoreCLI = new JCheckBox(jEdit.getProperty(
				"options.general.restore.cli"));
		restoreCLI.setSelected(jEdit.getBooleanProperty("restore.cli"));
		restoreCLI.setEnabled(restore.isSelected());
		addComponent(restoreCLI);

		restoreSplits = new JCheckBox(jEdit.getProperty(
				"options.general.restore.splits", "Restore split configuration"));
		restoreSplits.setSelected(jEdit.getBooleanProperty("restore.splits", true));
		addComponent(restoreSplits);

		/* Maximum hypersearch results to ask for abort */
		{
			String maxWarnLabel = jEdit.getProperty("options.general.hypersearch.maxWarningResults");
			int maxWarnValue = jEdit.getIntegerProperty("hypersearch.maxWarningResults");
			SpinnerModel model = new SpinnerNumberModel(maxWarnValue, 0, Integer.MAX_VALUE, 1);
			hypersearchResultsWarning = new JSpinner(model);
			addComponent(maxWarnLabel, hypersearchResultsWarning);
		}

		String language = jEdit.getCurrentLanguage();

		String availableLanguages = jEdit.getProperty("available.lang", "en");
		String[] languages = availableLanguages.split(" ");

		useDefaultLocale = new JCheckBox(jEdit.getProperty("options.appearance.usedefaultlocale.label"));
		useDefaultLocale.setSelected(jEdit.getBooleanProperty("lang.usedefaultlocale"));
		useDefaultLocale.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				lang.setEnabled(!useDefaultLocale.isSelected());
			}
		});
		lang = new JComboBox<String>(languages);
		lang.setEnabled(!useDefaultLocale.isSelected());
		lang.setSelectedItem(language);

		lang.setRenderer(new LangCellRenderer());
		addSeparator("options.appearance.localization.section.label");
		addComponent(useDefaultLocale);
		addComponent(jEdit.getProperty("options.appearance.lang.label"), lang);
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{

		switch(checkModStatus.getSelectedIndex())
		{
		case 0:
			jEdit.setBooleanProperty("autoReloadDialog",false);
			jEdit.setBooleanProperty("autoReload",false);
			break;
		case 1:
			jEdit.setBooleanProperty("autoReloadDialog",true);
			jEdit.setBooleanProperty("autoReload",false);
			break;
		case 2:
			jEdit.setBooleanProperty("autoReloadDialog",true);
			jEdit.setBooleanProperty("autoReload",true);
			break;
		case 3:
			jEdit.setBooleanProperty("autoReloadDialog",false);
			jEdit.setBooleanProperty("autoReload",true);
			break;
		}
		jEdit.setIntegerProperty("checkFileStatus", checkModStatusUpon.getSelectedIndex());
		jEdit.setIntegerProperty("recentFiles", (Integer) recentFiles.getModel().getValue());
		jEdit.setBooleanProperty("sortRecent",sortRecent.isSelected());
		jEdit.setBooleanProperty("hideOpen", hideOpen.isSelected());
		jEdit.setBooleanProperty("closeAllConfirm", closeAllConfirm.isSelected());
		jEdit.setBooleanProperty("saveCaret",saveCaret.isSelected());
		jEdit.setBooleanProperty("persistentMarkers",
				persistentMarkers.isSelected());
		jEdit.setBooleanProperty("restore",restore.isSelected());
		jEdit.setBooleanProperty("restore.cli",restoreCLI.isSelected());
		jEdit.setBooleanProperty("restore.remote", restoreRemote.isSelected());
		jEdit.setBooleanProperty("restore.splits", restoreSplits.isSelected());
		{
			int maxWarnResults = (Integer) hypersearchResultsWarning.getModel().getValue();
			jEdit.setIntegerProperty("hypersearch.maxWarningResults", maxWarnResults);
		}
		jEdit.setBooleanProperty("lang.usedefaultlocale", useDefaultLocale.isSelected());
		jEdit.setProperty("lang.current", String.valueOf(lang.getSelectedItem()));
	} //}}}

	//{{{ LangCellRenderer class
	private static class LangCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			String label = jEdit.getProperty("options.appearance.lang."+value);
			if (label != null)
				setText(label);
			return this;
		}
	} //}}}
}
