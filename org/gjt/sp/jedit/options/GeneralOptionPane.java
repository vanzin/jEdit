/*
 * GeneralOptionPane.java - General options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//}}}

/**
 * jEdit's General Options Pane
 */

public class GeneralOptionPane extends AbstractOptionPane
{
	//{{{ checkFileStatus options
	// Check the buffer status when the view got the focus
	public static final int checkFileStatus_focus = 0;
	// Check the file status when the view got the status
	// or when saving or visiting the buffer
	public static final int checkFileStatus_all = 1;
	// Check the file status when visiting or saving <b>any</b> buffer
	public static final int checkFileStatus_operations = 2;
	// Check the file status when visiting or saving the buffer
	public static final int checkFileStatus_focusBuffer = 3;
	// Check the file status when saving the buffer
	public static final int checkFileStatus_none = 4;
	//}}}

	//{{{ Private members
	private JComboBox lineSeparator;
	private JComboBox checkModStatus;
	private JComboBox checkModStatusUpon;
	private JTextField recentFiles;
	private JTextField hypersearchResultsWarning;
	private JCheckBox saveCaret;
	private JCheckBox sortRecent;
	private JCheckBox persistentMarkers;
	private JCheckBox restore;
	private JCheckBox restoreRemote;
	private JCheckBox restoreCLI;
	private JCheckBox restoreSplits;
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

		/* Line separator */
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = jEdit.getProperty("buffer."+ JEditBuffer.LINESEP,
			System.getProperty("line.separator"));
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.general.lineSeparator"),
			lineSeparator);


		/* Check mod status */
		String[] modCheckOptions = {
			jEdit.getProperty("options.general.checkModStatus.nothing"),
			jEdit.getProperty("options.general.checkModStatus.prompt"),
			jEdit.getProperty("options.general.checkModStatus.reload"),
			jEdit.getProperty("options.general.checkModStatus.silentReload")
		};
		checkModStatus = new JComboBox(modCheckOptions);
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
			jEdit.getProperty("options.general.checkModStatusUpon.focus"),
			jEdit.getProperty("options.general.checkModStatusUpon.all"),
			jEdit.getProperty("options.general.checkModStatusUpon.operations"),
			jEdit.getProperty("options.general.checkModStatusUpon.focusBuffer"),
			jEdit.getProperty("options.general.checkModStatusUpon.none")
		};
		checkModStatusUpon = new JComboBox(modCheckUponOptions);
		checkModStatusUpon.setSelectedIndex(jEdit.getIntegerProperty("checkFileStatus"));
		addComponent(jEdit.getProperty("options.general.checkModStatusUpon"),
			checkModStatusUpon);

		/* Recent file list size */
		recentFiles = new JTextField(jEdit.getProperty(
			"options.general.recentFiles"));
		recentFiles.setText(jEdit.getProperty("recentFiles"));
		addComponent(jEdit.getProperty("options.general.recentFiles"),
			recentFiles);

		/* Sort recent file list */
		sortRecent = new JCheckBox(jEdit.getProperty(
			"options.general.sortRecent"));
		sortRecent.setSelected(jEdit.getBooleanProperty("sortRecent"));
		addComponent(sortRecent);

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

		hypersearchResultsWarning = new JTextField(jEdit.getProperty("hypersearch.maxWarningResults"));
		addComponent(jEdit.getProperty("options.general.hypersearch.maxWarningResults"),
			hypersearchResultsWarning);



	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{

		String lineSep = null;
		switch(lineSeparator.getSelectedIndex())
		{
		case 0:
			lineSep = "\n";
			break;
		case 1:
			lineSep = "\r\n";
			break;
		case 2:
			lineSep = "\r";
			break;
		}
		jEdit.setProperty("buffer."+ JEditBuffer.LINESEP,lineSep);
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
		jEdit.setIntegerProperty("checkFileStatus",checkModStatusUpon.getSelectedIndex());
		jEdit.setProperty("recentFiles",recentFiles.getText());
		jEdit.setBooleanProperty("sortRecent",sortRecent.isSelected());
		jEdit.setBooleanProperty("saveCaret",saveCaret.isSelected());
		jEdit.setBooleanProperty("persistentMarkers",
			persistentMarkers.isSelected());
		jEdit.setBooleanProperty("restore",restore.isSelected());
		jEdit.setBooleanProperty("restore.cli",restoreCLI.isSelected());
		jEdit.setBooleanProperty("restore.remote", restoreRemote.isSelected());
		jEdit.setBooleanProperty("restore.splits", restoreSplits.isSelected());
		try
		{
			jEdit.setIntegerProperty("hypersearch.maxWarningResults", Integer.parseInt(hypersearchResultsWarning.getText()));
		}
		catch (NumberFormatException e)
		{
			Log.log(Log.WARNING, this, "hypersearchResultsWarning: " + hypersearchResultsWarning.getText() + " is not a valid value for this option");
		}
	} //}}}


}
