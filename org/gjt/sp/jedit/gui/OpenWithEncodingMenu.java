/*
 * OpenWithEncodingMenu.java - 'Open With Encoding' menu
 * Copyright (C) 2001 Slava Pestov
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

import javax.swing.*;
import java.awt.event.*;
import java.awt.Component;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.*;

public class OpenWithEncodingMenu extends EnhancedMenu
{
	public OpenWithEncodingMenu()
	{
		super("open-encoding");

		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Hashtable props = new Hashtable();
				props.put("encoding",evt.getActionCommand());
				jEdit.showOpenFileDialog(EditAction.getView(
					(Component)evt.getSource()),props);
			}
		};

		// used twice...
		String systemEncoding = System.getProperty("file.encoding");

		JMenuItem mi = new JMenuItem(jEdit.getProperty("os-encoding"));
		mi.setActionCommand(systemEncoding);
		mi.addActionListener(listener);
		add(mi);

		mi = new JMenuItem(jEdit.getProperty("jedit-encoding"));
		mi.setActionCommand(jEdit.getProperty("buffer.encoding",systemEncoding));
		mi.addActionListener(listener);
		add(mi);

		addSeparator();

		StringTokenizer st = new StringTokenizer(jEdit.getProperty("encodings"));
		while(st.hasMoreTokens())
		{
			mi = new JMenuItem(st.nextToken());
			mi.addActionListener(listener);
			add(mi);
		}

		addSeparator();

		add(GUIUtilities.loadMenuItem("other-encoding"));
	}
}
