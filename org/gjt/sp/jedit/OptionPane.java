/*
 * OptionPane.java - Option pane interface
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit;

import java.awt.Component;

/**
 * The interface all option panes must implement.  Internally, jEdit uses
 * option panes to implement the tabs in the "Global Options" dialog box.
 * Plugins can also create option panes for the "Plugin Options" dialog
 * box.<p>
 *
 * The <i>name</i> of an option pane is returned by the <code>getName()</code>
 * method. The label displayed in the option pane's tab is obtained from the
 * <code>options.<i>name</i>.label</code> property.
 *
 * @see org.gjt.sp.jedit.AbstractOptionPane
 */
public interface OptionPane
{
	/**
	 * Returns the internal name of this option pane.
	 */
	String getName();

	/**
	 * Returns the component that should be displayed for this option pane.
	 */
	Component getComponent();

	/**
	 * This method should create the option pane's GUI.
	 */
	void init();

	/**
	 * Called when the options dialog's "ok" button is clicked.
	 * This should save any properties being edited in this option
	 * pane.
	 */
	void save();
}
