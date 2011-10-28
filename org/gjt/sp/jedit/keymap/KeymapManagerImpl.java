/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.keymap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.gjt.sp.jedit.IPropertyManager;
import org.gjt.sp.jedit.jEdit;

/**
 * @author Matthieu Casanova
 * @since jEdit 5.0
 */
public class KeymapManagerImpl implements KeymapManager
{
	private Keymap currentKeymap;
	private final IPropertyManager propertyManager;

	public KeymapManagerImpl(IPropertyManager propertyManager)
	{
		this.propertyManager = propertyManager;
	}

	@Override
	public Collection<String> getKeymapNames()
	{
		Collection<String> systemKeymapNames = getSystemKeymapNames();
		Collection<String> userKeymapNames = getUserKeymapNames();
		systemKeymapNames.addAll(userKeymapNames);
		Set<String> keyMaps = new HashSet<String>(systemKeymapNames.size() + userKeymapNames.size());
		keyMaps.addAll(systemKeymapNames);
		keyMaps.addAll(userKeymapNames);
		return keyMaps;
	}

	@Override
	public Keymap getKeymap(String name)
	{
		File keymapFile = getKeymapFile(name);
		Keymap keymap = null;
		if (keymapFile.isFile())
			keymap = new KeymapImpl(name, keymapFile);
		return keymap;
	}

	@Override
	public State getKeymapState(String name)
	{
		File systemKeymapFile = getSystemKeymapFile(name);
		File userKeymapFile = getUserKeymapFile(name);
		if (userKeymapFile.isFile())
		{
			if (systemKeymapFile.isFile())
				return State.SystemModified;
			return State.User;
		}
		if (systemKeymapFile.isFile())
			return State.System;
		return State.Unknown;
	}

	private static File getKeymapFile(String name)
	{
		File file = getUserKeymapFile(name);
		if (!file.isFile())
			file = getSystemKeymapFile(name);
		return file;
	}

	static File getUserKeymapFile(String name)
	{
		return new File(getUserKeymapFolder(), name + "_keys.props");
	}

	private static File getSystemKeymapFile(String name)
	{
		return new File("keymaps/" + name + "_keys.props");
	}

	private static Collection<String> getSystemKeymapNames()
	{
		File keymapFolder = new File("keymaps");
		return getKeymapsFromFolder(keymapFolder);
	}

	private static Collection<String> getUserKeymapNames()
	{
		File keymapFolder = getUserKeymapFolder();
		return getKeymapsFromFolder(keymapFolder);
	}

	private static Collection<String> getKeymapsFromFolder(File folder)
	{
		Collection<String> names = new ArrayList<String>();
		File[] files = folder.listFiles(new KeymapFileFilter());
		if (files != null)
		{
			for (File file : files)
			{
				String filename = file.getName();
				String name = filename.substring(0, filename.length() - 11);
				names.add(name);
			}
		}
		return names;
	}

	private static File getUserKeymapFolder()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		return new File(settingsDirectory, "keymap");
	}

	@Override
	public void reload()
	{
		String name = propertyManager.getProperty("keymap.current");
		currentKeymap = getKeymap(name);
		if (currentKeymap == null)
			currentKeymap = getKeymap("jedit");
	}


	@Override
	public Keymap getKeymap()
	{
		return currentKeymap;
	}
}
