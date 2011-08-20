/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 jEdit contributors
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

//{{{ Imports
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import java.awt.*;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 * 
 */
public class JTrayIconManager
{
	private static JEditTrayIcon trayIcon;
	private static boolean restore;
	private static String userDir;
	private static String[] args;

	//{{{ setTrayIconArgs() method
	public static void setTrayIconArgs(boolean restore, String userDir, String[] args)
	{
		JTrayIconManager.restore = restore;
		JTrayIconManager.userDir = userDir;
		JTrayIconManager.args = args;
	} //}}}

	//{{{ addTrayIcon() method
	public static void addTrayIcon()
	{
		if (trayIcon == null && SystemTray.isSupported())
		{

			SystemTray systemTray = SystemTray.getSystemTray();
			String trayIconName = jEdit.getProperty("systrayicon.service", "swing");
			trayIcon = ServiceManager.getService(JEditTrayIcon.class, trayIconName);
			if (trayIcon == null)
			{
				if ("swing".equals(trayIconName))
				{
					Log.log(Log.ERROR, JTrayIconManager.class, "No service " +
						JEditTrayIcon.class.getName() + " with name swing");
					return;
				}
				Log.log(Log.WARNING, JTrayIconManager.class, "No service " +
					JEditTrayIcon.class.getName() + " with name "+ trayIconName);
				trayIcon = ServiceManager.getService(JEditTrayIcon.class, "swing");
			}
			if (trayIcon == null)
			{
				Log.log(Log.ERROR, JTrayIconManager.class, "No service " +
					JEditTrayIcon.class.getName() + " with name swing");
				return;
			}
			trayIcon.setTrayIconArgs(restore, userDir, args);
			try
			{
				systemTray.add(trayIcon);
			}
			catch (AWTException e)
			{
				Log.log(Log.ERROR, JEditSwingTrayIcon.class, "Unable to add Tray icon", e);
				return;
			}
			if (trayIcon instanceof EBComponent) {
				EditBus.addToBus(trayIcon);
			}
		}
	} //}}}

	//{{{ removeTrayIcon() method
	public static void removeTrayIcon()
	{
		if (trayIcon != null)
		{
			SystemTray.getSystemTray().remove(trayIcon);
			if (trayIcon instanceof EBComponent) {
				EditBus.removeFromBus(trayIcon);
			}
			trayIcon = null;
		}
	} //}}}

}
