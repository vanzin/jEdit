/*
 * GeneralOptionPane.java - General options panel
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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
import java.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class GeneralOptionPane extends AbstractOptionPane
{
	public GeneralOptionPane()
	{
		super("general");
	}

	// protected members
	protected void _init()
	{
		/* History count */
		history = new JTextField(jEdit.getProperty("history"));
		addComponent(jEdit.getProperty("options.general.history"),history);

		/* Save caret positions */
		saveCaret = new JCheckBox(jEdit.getProperty(
			"options.general.saveCaret"));
		saveCaret.setSelected(jEdit.getBooleanProperty("saveCaret"));
		addComponent(saveCaret);

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

		/* Show full path */
		showFullPath = new JCheckBox(jEdit.getProperty(
			"options.general.showFullPath"));
		showFullPath.setSelected(jEdit.getBooleanProperty(
			"view.showFullPath"));
		addComponent(showFullPath);

		/* Show search bar */
		showSearchbar = new JCheckBox(jEdit.getProperty(
			"options.general.showSearchbar"));
		showSearchbar.setSelected(jEdit.getBooleanProperty(
			"view.showSearchbar"));
		addComponent(showSearchbar);

		/* Beep on search auto wrap */
		beepOnSearchAutoWrap = new JCheckBox(jEdit.getProperty(
			"options.general.beepOnSearchAutoWrap"));
		beepOnSearchAutoWrap.setSelected(jEdit.getBooleanProperty(
			"search.beepOnSearchAutoWrap"));
		addComponent(beepOnSearchAutoWrap);

		/* Show buffer switcher */
		showBufferSwitcher = new JCheckBox(jEdit.getProperty(
			"options.general.showBufferSwitcher"));
		showBufferSwitcher.setSelected(jEdit.getBooleanProperty(
			"view.showBufferSwitcher"));
		addComponent(showBufferSwitcher);

		/* Show tip of the day */
		showTips = new JCheckBox(jEdit.getProperty(
			"options.general.showTips"));
		showTips.setSelected(jEdit.getBooleanProperty("tip.show"));
		addComponent(showTips);

		/* Show splash screen */
		showSplash = new JCheckBox(jEdit.getProperty(
			"options.general.showSplash"));
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			showSplash.setSelected(true);
		else
			showSplash.setSelected(!new File(settingsDirectory,"nosplash").exists());
		addComponent(showSplash);
	}

	protected void _save()
	{
		jEdit.setProperty("history",history.getText());
		jEdit.setBooleanProperty("saveCaret",saveCaret.isSelected());
		jEdit.setBooleanProperty("sortBuffers",sortBuffers.isSelected());
		jEdit.setBooleanProperty("sortByName",sortByName.isSelected());
		jEdit.setBooleanProperty("sortRecent",sortRecent.isSelected());
		jEdit.setBooleanProperty("view.checkModStatus",checkModStatus
			.isSelected());
		jEdit.setBooleanProperty("view.showFullPath",showFullPath
			.isSelected());
		jEdit.setBooleanProperty("view.showSearchbar",showSearchbar
			.isSelected());
		jEdit.setBooleanProperty("search.beepOnSearchAutoWrap",beepOnSearchAutoWrap
			.isSelected());
		jEdit.setBooleanProperty("view.showBufferSwitcher",
			showBufferSwitcher.isSelected());
		jEdit.setBooleanProperty("tip.show",showTips.isSelected());

		// this is handled a little differently from other jEdit settings
		// as the splash screen flag needs to be known very early in the
		// startup sequence, before the user properties have been loaded
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			File file = new File(settingsDirectory,"nosplash");
			if(showSplash.isSelected())
				file.delete();
			else
			{
				try
				{
					FileOutputStream out = new FileOutputStream(file);
					out.write('\n');
					out.close();
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,this,io);
				}
			}
		}
	}

	// private members
	private JTextField history;
	private JCheckBox saveCaret;
	private JCheckBox sortBuffers;
	private JCheckBox sortByName;
	private JCheckBox sortRecent;
	private JCheckBox checkModStatus;
	private JCheckBox showFullPath;
	private JCheckBox showSearchbar;
  private JCheckBox beepOnSearchAutoWrap;
	private JCheckBox showBufferSwitcher;
	private JCheckBox showTips;
	private JCheckBox showSplash;
}
