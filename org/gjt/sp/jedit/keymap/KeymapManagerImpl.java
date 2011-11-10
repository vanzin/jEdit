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

//{{{ Imports
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.gjt.sp.jedit.IPropertyManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * The default keymap manager implementation.
 * @author Matthieu Casanova
 * @since jEdit 5.0
 */
public class KeymapManagerImpl implements KeymapManager
{
	private Keymap currentKeymap;
	private final IPropertyManager propertyManager;

	//{{{ KeymapManagerImpl() constructor
	public KeymapManagerImpl(IPropertyManager propertyManager)
	{
		this.propertyManager = propertyManager;
	} //}}}

	//{{{ getKeymap() method
	@Override
	public Keymap getKeymap()
	{
		return currentKeymap;
	} //}}}

	//{{{ getKeymapNames() method
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
	} //}}}

	//{{{ getKeymap() method
	@Override
	public Keymap getKeymap(String name)
	{
		File keymapFile = getKeymapFile(name);
		Keymap keymap = null;
		if (keymapFile.isFile())
			keymap = new KeymapImpl(name, keymapFile);
		return keymap;
	} //}}}

	//{{{ getKeymapState() method
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
	} //}}}

	//{{{ resetKeymap() method
	@Override
	public void resetKeymap(String name)
	{
		State keymapState = getKeymapState(name);
		if (keymapState == State.SystemModified)
		{
			File userFile = getUserKeymapFile(name);
			userFile.delete();
		}
	} //}}}

	//{{{ deleteUserKeymap() method
	@Override
	public void deleteUserKeymap(String name)
	{
		State keymapState = getKeymapState(name);
		if (keymapState == State.User)
		{
			File userFile = getUserKeymapFile(name);
			userFile.delete();
		}
	} //}}}

	//{{{ copyKeymap() method
	@Override
	public boolean copyKeymap(String name, String newName)
	{
		File keymapFile = getUserKeymapFile(newName);
		if (keymapFile.exists())
			throw new IllegalArgumentException("Keymap " + newName + " already exists");

		File originalKeymap = getKeymapFile(name);
		if (!originalKeymap.isFile())
			throw new IllegalArgumentException("Keymap " + name + " doesn't exist");
		keymapFile.getParentFile().mkdirs();
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try
		{
			in = new BufferedInputStream(new FileInputStream(originalKeymap));
			out = new BufferedOutputStream(new FileOutputStream(keymapFile));
			IOUtilities.copyStream(null, in, out, false);
			return true;
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, e);
		}
		finally
		{
			IOUtilities.closeQuietly(in);
			IOUtilities.closeQuietly(out);
		}
		return false;
	} //}}}

	//{{{ reload() method
	@Override
	public void reload()
	{
		String name = getCurrentKeymapName();
		currentKeymap = getKeymap(name);
		if (currentKeymap == null)
			currentKeymap = getKeymap("jedit");
	} //}}}

	//{{{ getKeymapFile() method
	private static File getKeymapFile(String name)
	{
		File file = getUserKeymapFile(name);
		if (!file.isFile())
			file = getSystemKeymapFile(name);
		return file;
	} //}}}

	//{{{ getUserKeymapFile() method
	static File getUserKeymapFile(String name)
	{
		return new File(getUserKeymapFolder(), name + "_keys.props");
	} //}}}

	//{{{ getSystemKeymapFile() method
	private static File getSystemKeymapFile(String name)
	{
		return new File(getSystemKeymapFolder(), name + "_keys.props");
	} //}}}

	//{{{ getSystemKeymapNames() method
	private static Collection<String> getSystemKeymapNames()
	{
		File keymapFolder = getSystemKeymapFolder();
		return getKeymapsFromFolder(keymapFolder);
	} //}}}

	//{{{ getUserKeymapNames() method
	private static Collection<String> getUserKeymapNames()
	{
		File keymapFolder = getUserKeymapFolder();
		return getKeymapsFromFolder(keymapFolder);
	} //}}}

	//{{{ getKeymapsFromFolder() method
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
	} //}}}

	//{{{ getUserKeymapFolder() method
	private static File getUserKeymapFolder()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		return new File(settingsDirectory, "keymaps");
	} //}}}

	//{{{ getSystemKeymapFolder() method
	private static File getSystemKeymapFolder()
	{
		String homeDirectory = jEdit.getJEditHome();
		return new File(homeDirectory, "keymaps");
	} //}}}

	//{{{ getCurrentKeymapName() method
	private String getCurrentKeymapName()
	{
		return propertyManager.getProperty("keymap.current");
	} //}}}
}
