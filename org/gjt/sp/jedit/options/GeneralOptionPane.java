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
import javax.swing.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.*;
//}}}

public class GeneralOptionPane extends AbstractOptionPane
{
	//{{{ GeneralOptionPane constructor
	public GeneralOptionPane()
	{
		super("general");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
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
		addComponent(jEdit.getProperty("options.general.lineSeparator"),
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
		addComponent(jEdit.getProperty("options.general.encoding"),encoding);

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
			}
		});

		addComponent(restore);
		restoreCLI = new JCheckBox(jEdit.getProperty(
			"options.general.restore.cli"));
		restoreCLI.setSelected(jEdit.getBooleanProperty("restore.cli"));
		restoreCLI.setEnabled(restore.isSelected());
		addComponent(restoreCLI);

		/* Sort buffers */
		sortBuffers = new JCheckBox(jEdit.getProperty(
			"options.general.sortBuffers"));
		sortBuffers.setSelected(jEdit.getBooleanProperty("sortBuffers"));
		sortBuffers.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				sortByName.setEnabled(sortBuffers.isSelected());
			}
		});

		addComponent(sortBuffers);

		/* Sort buffers by names */
		sortByName = new JCheckBox(jEdit.getProperty(
			"options.general.sortByName"));
		sortByName.setSelected(jEdit.getBooleanProperty("sortByName"));
		sortByName.setEnabled(sortBuffers.isSelected());
		addComponent(sortByName);

		/* Sort recent file list */
		sortRecent = new JCheckBox(jEdit.getProperty(
			"options.general.sortRecent"));
		sortRecent.setSelected(jEdit.getBooleanProperty("sortRecent"));
		addComponent(sortRecent);

		/* Check mod status on focus */
		checkModStatus = new JCheckBox(jEdit.getProperty(
			"options.general.checkModStatus"));
		checkModStatus.setSelected(jEdit.getBooleanProperty(
			"view.checkModStatus"));
		addComponent(checkModStatus);

		/* Two-stage save */
		twoStageSave = new JCheckBox(jEdit.getProperty(
			"options.general.twoStageSave"));
		twoStageSave.setSelected(jEdit.getBooleanProperty(
			"twoStageSave"));
		addComponent(twoStageSave);

		/* Strip trailing EOL */
		stripTrailingEOL = new JCheckBox(jEdit.getProperty(
			"options.general.stripTrailingEOL"));
		stripTrailingEOL.setSelected(jEdit.getBooleanProperty("stripTrailingEOL"));
		addComponent(stripTrailingEOL);

	} //}}}

	//{{{ _save() method
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
		jEdit.setProperty("buffer.lineSeparator",lineSep);
		jEdit.setProperty("buffer.encoding",(String)
			encoding.getSelectedItem());
		jEdit.setBooleanProperty("saveCaret",saveCaret.isSelected());
		jEdit.setBooleanProperty("persistentMarkers",
			persistentMarkers.isSelected());
		jEdit.setBooleanProperty("restore",restore.isSelected());
		jEdit.setBooleanProperty("restore.cli",restoreCLI.isSelected());
		jEdit.setBooleanProperty("sortBuffers",sortBuffers.isSelected());
		jEdit.setBooleanProperty("sortByName",sortByName.isSelected());
		jEdit.setBooleanProperty("sortRecent",sortRecent.isSelected());
		jEdit.setBooleanProperty("view.checkModStatus",checkModStatus.isSelected());
		jEdit.setBooleanProperty("twoStageSave",twoStageSave.isSelected());
		jEdit.setBooleanProperty("stripTrailingEOL", stripTrailingEOL.isSelected());
	} //}}}

	//{{{ Private members
	private JComboBox lineSeparator;
	private JComboBox encoding;
	private JCheckBox saveCaret;
	private JCheckBox persistentMarkers;
	private JCheckBox restore;
	private JCheckBox restoreCLI;
	private JCheckBox sortBuffers;
	private JCheckBox sortByName;
	private JCheckBox sortRecent;
	private JCheckBox checkModStatus;
	private JCheckBox twoStageSave;
	private JCheckBox stripTrailingEOL;
	//}}}
}
