/*
 * DockableWindow.java - a window that can either float, or be inside a view
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.gui;

import java.awt.Component;

/**
 * @deprecated There is no need to implement this interface anymore. Just write
 * a <code>dockables.xml</code> file instead, for example:
 * <pre>
 * <?xml version="1.0"?>
 *
 * <!DOCTYPE DOCKABLES SYSTEM "dockables.xml">
 *
 * <DOCKABLES>
 *   <DOCKABLE NAME="quicknotepad">
 *     new QuickNotepad(view);
 *   </DOCKABLE>
 * </DOCKABLES>
 * </pre>
 */
public interface DockableWindow
{
	/**
	 * @deprecated Write a <code>dockables.xml</code> to add dockable
	 * windows instead
	 */
	String DOCKABLE_WINDOW_LIST = "DOCKABLE_WINDOWS";

	/**
	 * Returns the name of this dockable window. This is used to load/save
	 * geometry, and obtain the <code>dockable.<i>name</i>.label</code>
	 * property.
	 * @since jEdit 2.6pre3
	 */
	String getName();

	/**
	 * Returns the actual component.
	 * @since jEdit 2.6pre3
	 */
	Component getComponent();
}
