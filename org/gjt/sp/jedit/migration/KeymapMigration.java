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

package org.gjt.sp.jedit.migration;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.keymap.Keymap;
import org.gjt.sp.jedit.keymap.KeymapManager;

/**
 * @author Matthieu Casanova
 */
public class KeymapMigration implements MigrationService
{
	@Override
	public void migrate()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if (settingsDirectory == null)
			return;
		File keymapFolder = new File(settingsDirectory, "keymaps");
		if (keymapFolder.exists())
			return;

		KeymapManager keymapManager = jEdit.getKeymapManager();
		keymapManager.copyKeymap(KeymapManager.DEFAULT_KEYMAP_NAME, "imported");
		Keymap imported = keymapManager.getKeymap("imported");
		Properties properties = jEdit.getProperties();
		Set<Map.Entry<Object,Object>> entries = properties.entrySet();
		for (Map.Entry<Object, Object> entry:entries)
		{
		        String key = entry.getKey().toString();
			if (key.endsWith(".shortcut") || key.endsWith(".shortcut2"))
			{
				imported.setShortcut(key, entry.getValue().toString());
				jEdit.resetProperty(key);
			}
		}
		imported.save();
		jEdit.setProperty("keymap.current", "imported");
		keymapManager.reload();
	}
}
