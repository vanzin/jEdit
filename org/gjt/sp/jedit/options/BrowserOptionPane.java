/*
 * BrowserOptionPane.java - Browser options panel
 * Copyright (C) 2000, 2001 Slava Pestov
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

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class BrowserOptionPane extends AbstractOptionPane
{
	public BrowserOptionPane()
	{
		super("browser");
	}

	public void _init()
	{
		/* Default directory */
		String[] dirs = {
			jEdit.getProperty("options.browser.defaultPath.buffer"),
			jEdit.getProperty("options.browser.defaultPath.home"),
			jEdit.getProperty("options.browser.defaultPath.favorites"),
			jEdit.getProperty("options.browser.defaultPath.last")
		};

		defaultDirectory = new JComboBox(dirs);
		String defaultDir = jEdit.getProperty("vfs.browser.defaultPath");
		if("buffer".equals(defaultDir))
			defaultDirectory.setSelectedIndex(0);
		else if("home".equals(defaultDir))
			defaultDirectory.setSelectedIndex(1);
		else if("favorites".equals(defaultDir))
			defaultDirectory.setSelectedIndex(2);
		else if("last".equals(defaultDir))
			defaultDirectory.setSelectedIndex(3);
		addComponent(jEdit.getProperty("options.browser.defaultPath"),
			defaultDirectory);

		/* Show icons */
		showIcons = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".showIcons"));
		showIcons.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".showIcons"));
		addComponent(showIcons);

		/* Show hidden files */
		showHiddenFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".showHiddenFiles"));
		showHiddenFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".showHiddenFiles"));
		addComponent(showHiddenFiles);

		/* Sort file list */
		sortFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortFiles"));
		sortFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortFiles"));
		addComponent(sortFiles);

		/* Ignore case when sorting */
		sortIgnoreCase = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortIgnoreCase"));
		sortIgnoreCase.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortIgnoreCase"));
		addComponent(sortIgnoreCase);

		/* Mix files and directories */
		sortMixFilesAndDirs = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortMixFilesAndDirs"));
		sortMixFilesAndDirs.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortMixFilesAndDirs"));
		addComponent(sortMixFilesAndDirs);

		/* Double-click close */
		doubleClickClose = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".doubleClickClose"));
		doubleClickClose.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".doubleClickClose"));
		addComponent(doubleClickClose);

		/* Base filter in open/save dialogs on current buffer name */
		currentBufferFilter = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".currentBufferFilter"));
		currentBufferFilter.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".currentBufferFilter"));
		addComponent(currentBufferFilter);
	}

	public void _save()
	{
		String[] dirs = { "buffer", "home", "favorites", "last" };
		jEdit.setProperty("vfs.browser.defaultPath",dirs[defaultDirectory
			.getSelectedIndex()]);
		jEdit.setBooleanProperty("vfs.browser.showIcons",
			showIcons.isSelected());
		jEdit.setBooleanProperty("vfs.browser.showHiddenFiles",
			showHiddenFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortFiles",
			sortFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortIgnoreCase",
			sortIgnoreCase.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortMixFilesAndDirs",
			sortMixFilesAndDirs.isSelected());
		jEdit.setBooleanProperty("vfs.browser.doubleClickClose",
			doubleClickClose.isSelected());
		jEdit.setBooleanProperty("vfs.browser.currentBufferFilter",
			currentBufferFilter.isSelected());
	}

	// private members
	private JComboBox defaultDirectory;
	private JCheckBox showIcons;
	private JCheckBox showHiddenFiles;
	private JCheckBox sortFiles;
	private JCheckBox sortIgnoreCase;
	private JCheckBox sortMixFilesAndDirs;
	private JCheckBox doubleClickClose;
	private JCheckBox currentBufferFilter;
}
