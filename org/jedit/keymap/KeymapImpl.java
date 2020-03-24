/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
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

package org.jedit.keymap;

//{{{ Imports
import java.io.*;
import java.util.Properties;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * The default keymap implementation.
 * @author Matthieu Casanova
 * @since jEdit 5.0
 */
class KeymapImpl implements Keymap
{
	protected Properties props;

	protected final String name;
	private final File file;
	private boolean modified;

	//{{{ KeymapImpl() constructor
	KeymapImpl(String name, File file)
	{
		this.name = name;
		this.file = file;
		loadProperties();
	} //}}}

	//{{{ getInputStream() method
	protected InputStream getInputStream()
	{
		return Keymap.class.getResourceAsStream(name + "_keys.props");
	} //}}}

	//{{{ loadProperties() method
	private void loadProperties()
	{
		props = new Properties();
		try (InputStream in = new BufferedInputStream(new FileInputStream(file)))
		{
			props.load(in);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, "Unable to load properties", e);
		}
	} //}}}

	//{{{ getShortcut() method
	@Override
	public String getShortcut(String name)
	{
		String property = props.getProperty(name);
		return property;
	} //}}}

	//{{{ setShortcut() method
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
		String oldShortcut = props.getProperty(name);
		if (!shortcut.equals(oldShortcut))
		{
			modified = true;
			props.setProperty(name, shortcut);
		}
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return name;
	} //}}}

	//{{{ hashCode() method
	@Override
	public int hashCode()
	{
		return name.hashCode();
	} //}}}

	//{{{ equals() method
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Keymap))
			return false;

		Keymap keymap = (Keymap) obj;
		return name.equals(keymap.toString());
	} //}}}

	//{{{ save() method
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
				MiscUtilities.storeProperties(props, out, "jEdit's keymap " + name);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, "Unable to save properties", e);
			}
			finally
			{
				IOUtilities.closeQuietly((Closeable)out);
			}
		}

	} //}}}
}
