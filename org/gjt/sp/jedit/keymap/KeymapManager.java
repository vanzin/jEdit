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

import java.util.Collection;

/**
 * @author Matthieu Casanova
 * @since jEdit 5.0
 */
public interface KeymapManager
{
	/**
	 * Returns the current keymap.
	 * @return the current keymap
	 */
	Keymap getKeymap();

	void reload();

	Collection<String> getKeymapNames();

	/**
	 * Returns the keymap with that name.
	 * @param name the keymap name
	 * @return the user keymap of that name, if it exists, or the system keymap if it doesn't.
	 * If none exists <code>null</code> is returned
	 */
	Keymap getKeymap(String name);

	/**
	 * Returns the state of the keymap
	 * @param name the name of the keymap
	 * @return a state.
	 */
	State getKeymapState(String name);

	/**
	 * The states of the keymaps
	 */
	enum State {
		/** User keymap. */
		User,
		/** System keymap. */
		System,
		/** Modified system keymap. */
		SystemModified,
		/** Unknown keymap (doesn't exists). */
		Unknown }
}
