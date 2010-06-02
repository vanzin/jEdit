/*
 * AutosaveBackupOptionPane.java - Autosave & backup options
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import org.gjt.sp.jedit.gui.NumericTextField;
import org.gjt.sp.jedit.browser.VFSBrowser;
//}}}

/**
 * The Save and Backup option panel.
 *
 * @author Slava Pestov
 * @author $Id$
 */
public class SaveBackupOptionPane extends AbstractOptionPane
{
	//{{{ SaveBackupOptionPane constructor
	public SaveBackupOptionPane()
	{
		super("save-back");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		/* Two-stage save */
		twoStageSave = new JCheckBox(jEdit.getProperty(
			"options.save-back.twoStageSave"));
		twoStageSave.setSelected(jEdit.getBooleanProperty(
			"twoStageSave"));
		twoStageSave.setToolTipText(jEdit.getProperty(
			"options.save-back.twoStageSave.tooltip"));
		addComponent(twoStageSave);

		/* Confirm save all */
		confirmSaveAll = new JCheckBox(jEdit.getProperty(
			"options.save-back.confirmSaveAll"));
		confirmSaveAll.setSelected(jEdit.getBooleanProperty(
			"confirmSaveAll"));
		addComponent(confirmSaveAll);

		/* Autosave interval */
		autosave = new NumericTextField(jEdit.getProperty("autosave"), true);
		addComponent(jEdit.getProperty("options.save-back.autosave"),autosave);

		/* Autosave untitled buffers */
		autosaveUntitled = new JCheckBox(jEdit.getProperty(
			"options.save-back.autosaveUntitled"));
		autosaveUntitled.setSelected(jEdit.getBooleanProperty("autosaveUntitled"));
		addComponent(autosaveUntitled);

		suppressNotSavedConfirmUntitled = new JCheckBox(jEdit.getProperty(
			"options.save-back.suppressNotSavedConfirmUntitled"));
		suppressNotSavedConfirmUntitled.setSelected(
			jEdit.getBooleanProperty("suppressNotSavedConfirmUntitled"));
		addComponent(suppressNotSavedConfirmUntitled);

		useMD5forDirtyCalculation = new JCheckBox(jEdit.getProperty(
			"options.save-back.useMD5forDirtyCalculation"));
		useMD5forDirtyCalculation.setToolTipText(jEdit.getProperty(
			"options.save-back.useMD5forDirtyCalculation.tooltip"));
		useMD5forDirtyCalculation.setSelected(
			jEdit.getBooleanProperty("useMD5forDirtyCalculation"));
		addComponent(useMD5forDirtyCalculation);




		/* Backup count */
		backups = new NumericTextField(jEdit.getProperty("backups"), true);
		addComponent(jEdit.getProperty("options.save-back.backups"),backups);

		/* Backup directory */
		backupDirectory = new JTextField(jEdit.getProperty(
			"backup.directory"));
		JButton browseBackupDirectory = new JButton("...");
		browseBackupDirectory.addActionListener(new MyActionListener());
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(backupDirectory);
		panel.add(browseBackupDirectory, BorderLayout.EAST);
		addComponent(jEdit.getProperty("options.save-back.backupDirectory"),
			panel);

		/* Backup filename prefix */
		backupPrefix = new JTextField(jEdit.getProperty("backup.prefix"));
		addComponent(jEdit.getProperty("options.save-back.backupPrefix"),
			backupPrefix);

		/* Backup suffix */
		backupSuffix = new JTextField(jEdit.getProperty(
			"backup.suffix"));
		addComponent(jEdit.getProperty("options.save-back.backupSuffix"),
			backupSuffix);

		/* Backup on every save */
		backupEverySave = new JCheckBox(jEdit.getProperty(
			"options.save-back.backupEverySave"));
		backupEverySave.setSelected(jEdit.getBooleanProperty("backupEverySave"));
		addComponent(backupEverySave);
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setBooleanProperty("twoStageSave",twoStageSave.isSelected());
		jEdit.setBooleanProperty("confirmSaveAll",confirmSaveAll.isSelected());
		jEdit.setProperty("autosave", this.autosave.getText());
		jEdit.setProperty("backups",backups.getText());
		jEdit.setProperty("backup.directory",backupDirectory.getText());
		jEdit.setProperty("backup.prefix",backupPrefix.getText());
		jEdit.setProperty("backup.suffix",backupSuffix.getText());
		jEdit.setBooleanProperty("backupEverySave", backupEverySave.isSelected());
		boolean newAutosave = autosaveUntitled.isSelected();
		boolean oldAutosave = jEdit.getBooleanProperty("autosaveUntitled");
		jEdit.setBooleanProperty("autosaveUntitled", newAutosave);
		jEdit.setBooleanProperty("suppressNotSavedConfirmUntitled",
				suppressNotSavedConfirmUntitled.isSelected());
		jEdit.setBooleanProperty("useMD5forDirtyCalculation",
				useMD5forDirtyCalculation.isSelected());
		if ((!newAutosave || jEdit.getIntegerProperty("autosave",0) == 0) && oldAutosave)
		{
			Buffer[] buffers = jEdit.getBuffers();
			for (Buffer buffer : buffers)
			{
				if (buffer.isUntitled())
				{
					buffer.removeAutosaveFile();
				}
			}
		}
	} //}}}

	//{{{ Private members
	private JCheckBox twoStageSave;
	private JCheckBox confirmSaveAll;
	private JTextField autosave;
	private JCheckBox autosaveUntitled;
	private JCheckBox suppressNotSavedConfirmUntitled;
	private JCheckBox useMD5forDirtyCalculation;
	private JTextField backups;
	private JTextField backupDirectory;
	private JTextField backupPrefix;
	private JTextField backupSuffix;
	private JCheckBox backupEverySave;
	//}}}

	//{{{ MyActionListener class
	private class MyActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String[] choosenFolder =
				GUIUtilities.showVFSFileDialog(null,
				   			       backupDirectory.getText(),
				   			       VFSBrowser.CHOOSE_DIRECTORY_DIALOG,
				   			       false);
			if (choosenFolder != null)
				backupDirectory.setText(choosenFolder[0]);
		}
	} //}}}

}
