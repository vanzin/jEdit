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

import org.gjt.sp.jedit.EditServer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class JEditTrayIcon extends JTrayIcon
{
	private static JEditTrayIcon trayIcon;
	private static boolean restore;
	private static String userDir;
	private static String[] args;

	private JEditTrayIcon()
	{
		super(GUIUtilities.getEditorIcon(), "jEdit");
		setImageAutoSize(true);
		JPopupMenu popup = new JPopupMenu();
		JMenuItem newViewItem = new JMenuItem(jEdit.getProperty("tray.newView.label"));
		JMenuItem newPlainViewItem = new JMenuItem(jEdit.getProperty("tray.newPlainView.label"));
		JMenuItem exitItem = new JMenuItem(jEdit.getProperty("tray.exit.label"));

		popup.add(newViewItem);
		popup.add(newPlainViewItem);
		popup.addSeparator();
		popup.add(exitItem);
		ActionListener actionListener = new MyActionListener(newViewItem, newPlainViewItem, exitItem);
		newViewItem.addActionListener(actionListener);
		newPlainViewItem.addActionListener(actionListener);
		exitItem.addActionListener(actionListener);
		setMenu(popup);

		addMouseListener(new MyMouseAdapter());
	}

	public static void setTrayIconArgs(boolean restore, String userDir, String[] args)
	{
		JEditTrayIcon.restore = restore;
		JEditTrayIcon.userDir = userDir;
		JEditTrayIcon.args = args;
	}

	public static void addTrayIcon()
	{
		if (trayIcon == null && SystemTray.isSupported())
		{

			SystemTray systemTray = SystemTray.getSystemTray();

			trayIcon = new JEditTrayIcon();

			try
			{
				systemTray.add(trayIcon);
			}
			catch (AWTException e)
			{
				Log.log(Log.ERROR, JEditTrayIcon.class, "Unable to add Tray icon", e);
			}
		}
	}

	public static void removeTrayIcon()
	{
		if (trayIcon != null)
		{
			SystemTray.getSystemTray().remove(trayIcon);
			trayIcon = null;
		}
	}

	private static class MyMouseAdapter extends MouseAdapter
	{
		private final Map<Window,Boolean> windowState = new HashMap<Window, Boolean>();

		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			if (jEdit.getViewCount() == 0)
			{
				EditServer.handleClient(restore, true, false, userDir, args);
			}
			else
			{
				boolean newVisibilityState = !jEdit.getActiveView().isVisible();
				if (newVisibilityState)
				{
					for (Window window : Window.getWindows())
					{
						Boolean previousState = windowState.get(window);
						if (previousState == null)
							window.setVisible(true);
						else if (previousState)
							window.setVisible(previousState);
					}
					windowState.clear();
					if (jEdit.getActiveView().getState() == Frame.ICONIFIED)
						jEdit.getActiveView().setState(Frame.NORMAL);
					jEdit.getActiveView().toFront();
				}
				else
				{
					for (Window window : Window.getWindows())
					{
						windowState.put(window, window.isVisible());
						window.setVisible(false);
					}
				}
			}
		}
	}

	private static class MyActionListener implements ActionListener
	{
		private final JMenuItem newViewItem;
		private final JMenuItem newPlainViewItem;
		private final JMenuItem exitItem;

		MyActionListener(JMenuItem newViewItem, JMenuItem newPlainViewItem, JMenuItem exitItem)
		{
			this.newViewItem = newViewItem;
			this.newPlainViewItem = newPlainViewItem;
			this.exitItem = exitItem;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == newViewItem)
			{
				jEdit.newView(null);
			} else if (e.getSource() == newPlainViewItem)
			{
				jEdit.newView(null, null, true);
			} else if (e.getSource() == exitItem)
			{
				jEdit.exit(null, true);
			}
		}
	}
}
