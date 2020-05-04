/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.EditPaneUpdate;

import static org.gjt.sp.jedit.EditBus.*;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class JEditSwingTrayIcon extends JEditTrayIcon
{
	private boolean restore;
	private String userDir;
	private String[] args;

	//{{{ JEditSwingTrayIcon() constructor
	public JEditSwingTrayIcon()
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
		newViewItem.addActionListener(e -> jEdit.newView(null));
		newPlainViewItem.addActionListener(e -> jEdit.newView(null, null, true));
		exitItem.addActionListener(e -> jEdit.exit(null, true));
		setMenu(popup);
		addMouseListener(new MyMouseAdapter());
	} //}}}

	/**
	 * Update tooltip to reflect the window titles currently available.
	 */
	@EBHandler
	public void handleMessage(EditPaneUpdate editPaneUpdate)
	{
		if (editPaneUpdate.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
		{
			Collection<String> sl = new ArrayList<>();
			jEdit.getViewManager().forEach(view -> sl.add(view.getTitle()));
			setToolTip(String.join(" | ", sl));
		}
	}
	
	//{{{ setTrayIconArgs() method
	@Override
	void setTrayIconArgs(boolean restore, String userDir, String[] args)
	{
		this.restore = restore;
		this.userDir = userDir;
		this.args = args;
	} //}}}

	//{{{ MyMouseAdapter class
	private class MyMouseAdapter extends MouseAdapter
	{
		private final Map<Window,Boolean> windowState = new HashMap<>();

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
					for (Window window : Window.getOwnerlessWindows())
					{
						if (skipWindow(window))
							continue;
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
					for (Window window : Window.getOwnerlessWindows())
					{
						if (skipWindow(window))
							continue;
						windowState.put(window, window.isVisible());
						window.setVisible(false);
					}
				}
			}
		}

		//{{{ skipWindow method
		/**
		 * Check if a window is not top level or systray icon
		 * @param window the checked window
		 * @return true if it is not toplevel or systray icon
		 */
		private boolean skipWindow(Window window)
		{
			return window.getClass().getName().contains("Tray");
		} //}}}

	} //}}}
}
