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

package org.gjt.sp.jedit.options;

import org.gjt.sp.jedit.gui.AbstractContextOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 * Right-click context menu editor.
 *
 * @author Matthieu Casanova
 * @version $Id: ContextOptionPane.java 12504 2008-04-22 23:12:43Z ezust $
 */
public class BrowserContextOptionPane extends AbstractContextOptionPane
{

	public BrowserContextOptionPane()
	{
		super("browser.custom.context", jEdit.getProperty("options.browser.context.caption"));
	}

	/**
	 * Returns jEdit's context menu configuration.
	 *
	 * @since jEdit 4.3pre13
	 */
	@Override
	protected String getContextMenu()
	{
		return jEdit.getProperty("browser.custom.context");
	}

	/**
	 * Saves jEdit's context menu configuration.
	 *
	 * @since jEdit 4.3pre13
	 */
	@Override
	protected void saveContextMenu(String menu)
	{
		jEdit.setProperty("browser.custom.context", menu);
	}

}
