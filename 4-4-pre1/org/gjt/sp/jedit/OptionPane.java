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
 * The interface all option panes must implement.<p>
 *
 * See {@link EditPlugin} for information on how jEdit obtains and constructs
 * option pane instances.<p>
 *
 * Note that in most cases it is much easier to extend
 * {@link AbstractOptionPane} instead.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public interface OptionPane
{
	/**
	 * Returns the internal name of this option pane. The option pane's label
	 * is set to the value of the property named
	 * <code>options.<i>name</i>.label</code>.
	 * @see jEdit#getProperty(String)
	 */
	String getName();

	/**
	 * Returns the component that should be displayed for this option pane.
	 */
	Component getComponent();

	/**
	 * This method is called every time the option pane is displayed.
	 */
	void init();

	/**
	 * Called when the options dialog's "ok" button is clicked.
	 * This should save any properties being edited in this option
	 * pane.
	 */
	void save();
}
