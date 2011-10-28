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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;

/**
 * @author Matthieu Casanova
 * @since jEdit 5.0
 */
public class KeymapImpl implements Keymap
{
	protected Properties props;

	protected final String name;
	private final File file;
	private boolean modified;

	public KeymapImpl(String name, File file)
	{
		this.name = name;
		this.file = file;
		loadProperties();
	}

	protected InputStream getInputStream()
	{
		return Keymap.class.getResourceAsStream(name + "_keys.props");
	}

	private void loadProperties()
	{
		props = new Properties();
		InputStream in = null;
		try
		{
			in = new BufferedInputStream(new FileInputStream(file));
			props.load(in);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, "Unable to load properties", e);
		}
		finally
		{
			IOUtilities.closeQuietly(in);
		}
	}

	@Override
	public String getShortcut(String name)
	{
		String property = props.getProperty(name);
		return property;
	}

	@Override
	public void setShortcut(String name, String shortcut)
	{
		if (shortcut == null || shortcut.isEmpty())
		{
			if (props.containsKey(name))
			{
				modified = true;
				props.remove(name);
			}
			return;
		}
		modified = true;
		props.setProperty(name, shortcut);
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Keymap))
			return false;

		Keymap keymap = (Keymap) obj;
		return name.equals(keymap.toString());
	}

	@Override
	public void save()
	{
		if (modified)
		{
			modified = false;
			File userKeymapFile = KeymapManagerImpl.getUserKeymapFile(name);
			userKeymapFile.getParentFile().mkdirs();
			BufferedOutputStream out = null;
			try
			{
				out = new BufferedOutputStream(new FileOutputStream(userKeymapFile));
				props.store(out, "jEdit's keymap " + name);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, "Unable to save properties", e);
			}
			finally
			{
				IOUtilities.closeQuietly(out);
			}
		}

	}
}
