/*
 * DynamicMenuProvider.java - API for dynamic plugin menus
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.menu;

import javax.swing.JMenu;

/**
 * Interface for a pull-down menu whose contents are determined at runtime.<p>
 *
 * See {@link org.gjt.sp.jedit.EditPlugin} for properties you need to define to
 * have your plugin provide a dynamic menu.
 *
 * @since jEdit 4.2pre2
 * @author Slava Pestov
 * @version $Id$
 */
public interface DynamicMenuProvider
{
	/**
	 * Returns true if the menu should be updated each time it is shown.
	 * Otherwise, it will only be updated when the menu is first created,
	 * and if the menu receives a {@link
	 * org.gjt.sp.jedit.msg.DynamicMenuChanged} message.
	 */
	boolean updateEveryTime();

	/**
	 * Adds the menu items to the given menu.
	 * @param menu The menu
	 */
	void update(JMenu menu);
}
