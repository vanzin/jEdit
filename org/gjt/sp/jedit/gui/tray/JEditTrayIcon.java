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

package org.gjt.sp.jedit.gui.tray;

import java.awt.Image;

/**
 * The mother class of the tray icon service.
 * If you want to replace the tray icon of jEdit, you must extend it
 * and declare a service "org.gjt.sp.jedit.gui.tray.JEditTrayIcon"
 * @see org.gjt.sp.jedit.ServiceManager
 * @author Matthieu Casanova
 */
public abstract class JEditTrayIcon extends JTrayIcon
{
	protected JEditTrayIcon(Image image, String tooltip)
	{
		super(image, tooltip);
	}

	abstract void setTrayIconArgs(boolean restore, String userDir, String[] args);
}
