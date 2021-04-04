/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2021 jEdit contributors
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Matthieu Casanova
 * @since jEdit 5.7pre1
 */
public class DialogChooser
{
	/**
	 * Show an undecorated modal dialog presenting one button per choice.
	 *
	 * @param parent the pârent frame
	 * @param choices the choices
	 * @return the index of the selected choice
	 * @since jEdit 5.7pre1
	 */
	public static int openChooseWindow(JFrame parent, String... choices)
	{
		JDialog dialog = new JDialog(parent);
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		AtomicInteger atomicInteger = new AtomicInteger(-1);
		for (int i = 0; i < choices.length; i++)
		{
			String choice = choices[i];
			JButton comp = new JButton(choice);
			int finalI = i;
			comp.addActionListener(e ->
			{
				atomicInteger.set(finalI);
				dialog.dispose();
			});
			panel.add(comp);
		}
		dialog.setUndecorated(true);
		dialog.setModal(true);
		dialog.setContentPane(panel);
		dialog.setLocationRelativeTo(panel);
		dialog.pack();
		dialog.setVisible(true);
		return atomicInteger.get();
	}
}
