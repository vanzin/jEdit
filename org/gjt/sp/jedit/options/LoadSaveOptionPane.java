/*
 * LoadSaveOptionPane.java - Loading and saving options panel
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
import java.util.StringTokenizer;
import org.gjt.sp.jedit.*;

public class LoadSaveOptionPane extends AbstractOptionPane
{
	public LoadSaveOptionPane()
	{
		super("loadsave");
	}

	public void _init()
	{
		/* Autosave interval */
		autosave = new JTextField(jEdit.getProperty("autosave"));
		addComponent(jEdit.getProperty("options.loadsave.autosave"),autosave);

		/* Backup count */
		backups = new JTextField(jEdit.getProperty("backups"));
		addComponent(jEdit.getProperty("options.loadsave.backups"),backups);

		/* Backup directory */
		backupDirectory = new JTextField(jEdit.getProperty(
			"backup.directory"));
		addComponent(jEdit.getProperty("options.loadsave.backupDirectory"),
			backupDirectory);

		/* Backup filename prefix */
		backupPrefix = new JTextField(jEdit.getProperty("backup.prefix"));
		addComponent(jEdit.getProperty("options.loadsave.backupPrefix"),
			backupPrefix);

		/* Backup suffix */
		backupSuffix = new JTextField(jEdit.getProperty(
			"backup.suffix"));
		addComponent(jEdit.getProperty("options.loadsave.backupSuffix"),
			backupSuffix);

		/* Line separator */
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = jEdit.getProperty("buffer.lineSeparator",
			System.getProperty("line.separator"));
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.loadsave.lineSeparator"),
			lineSeparator);

		/* Default file encoding */
		DefaultComboBoxModel encodings = new DefaultComboBoxModel();
		StringTokenizer st = new StringTokenizer(jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			encodings.addElement(st.nextToken());
		}

		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(jEdit.getProperty("buffer.encoding",
			System.getProperty("file.encoding")));
		addComponent(jEdit.getProperty("options.loadsave.encoding"),encoding);

		/* Number of I/O threads to start */
		ioThreadCount = new JTextField(jEdit.getProperty("ioThreadCount"));
		addComponent(jEdit.getProperty("options.loadsave.ioThreadCount"),
			ioThreadCount);

		/* Session management */
		restore = new JCheckBox(jEdit.getProperty(
			"options.loadsave.restore"));
		restore.setSelected(jEdit.getBooleanProperty("restore"));
		restore.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				restoreCLI.setEnabled(restore.isSelected());
			}
		});

		addComponent(restore);
		restoreCLI = new JCheckBox(jEdit.getProperty(
			"options.loadsave.restore.cli"));
		restoreCLI.setSelected(jEdit.getBooleanProperty("restore.cli"));
		restoreCLI.setEnabled(restore.isSelected());
		addComponent(restoreCLI);

		/* Clients open files in new view */
		newView = new JCheckBox(jEdit.getProperty(
			"options.loadsave.newView"));
		newView.setSelected(jEdit.getBooleanProperty("client.newView"));
		addComponent(newView);

		/* Persistent markers */
		persistentMarkers = new JCheckBox(jEdit.getProperty(
			"options.loadsave.persistentMarkers"));
		persistentMarkers.setSelected(jEdit.getBooleanProperty(
			"persistentMarkers"));
		addComponent(persistentMarkers);

		/* Parse fully */
		parseFully = new JCheckBox(jEdit.getProperty(
			"options.loadsave.parseFully"));
		parseFully.setSelected(jEdit.getBooleanProperty("parseFully"));
		addComponent(parseFully);
	}

	public void _save()
	{
		jEdit.setProperty("autosave",autosave.getText());
		jEdit.setProperty("backups",backups.getText());
		jEdit.setProperty("backup.directory",backupDirectory.getText());
		jEdit.setProperty("backup.prefix",backupPrefix.getText());
		jEdit.setProperty("backup.suffix",backupSuffix.getText());
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
		jEdit.setProperty("buffer.lineSeparator",lineSep);
		jEdit.setProperty("buffer.encoding",(String)
			encoding.getSelectedItem());
		jEdit.setProperty("ioThreadCount",ioThreadCount.getText());
		jEdit.setBooleanProperty("restore",restore.isSelected());
		jEdit.setBooleanProperty("restore.cli",restoreCLI.isSelected());
		jEdit.setBooleanProperty("client.newView",newView.isSelected());
		jEdit.setBooleanProperty("persistentMarkers",
			persistentMarkers.isSelected());
		jEdit.setBooleanProperty("parseFully",parseFully.isSelected());
	}

	// private members
	private JTextField autosave;
	private JTextField backups;
	private JTextField backupDirectory;
	private JTextField backupPrefix;
	private JTextField backupSuffix;
	private JComboBox lineSeparator;
	private JComboBox encoding;
	private JTextField ioThreadCount;
	private JCheckBox restore;
	private JCheckBox restoreCLI;
	private JCheckBox newView;
	private JCheckBox persistentMarkers;
	private JCheckBox parseFully;
}
