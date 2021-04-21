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
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
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
	 * @param parent the parent frame
	 * @param choices the choices
	 * @return the index of the selected choice
	 * @since jEdit 5.7pre1
	 */
	public static int openChooserWindow(JFrame parent, String... choices)
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

	/**
	 * Show an undecorated dialog showing a list of items to choose.
	 *
	 * @param parent the parent component
	 * @param initialValue the initial choosen value
	 * @param listSelectionListener the callback to call when an item is choosen
	 * @param choices the choices
	 * @since jEdit 5.7pre1
	 */
	public static void openListChooserWindow(JComponent parent,
						 Object initialValue,
						 ListSelectionListener listSelectionListener,
						 Object[] choices)
	{
		JList<?> list = new JList<>(choices);
		list.setVisibleRowCount(Math.min(30, choices.length));
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedValue(initialValue, true);
		list.setBorder(BorderFactory.createEtchedBorder());
		JDialog window = new JDialog();
		window.setUndecorated(true);
		window.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowDeactivated(WindowEvent e)
			{
				window.dispose();
			}
		});
		list.addListSelectionListener(e1 ->
		{
			if (!e1.getValueIsAdjusting())
			{
				Object selectedValue = list.getSelectedValue();
				if (selectedValue != null && !Objects.equals(selectedValue, initialValue))
					listSelectionListener.valueChanged(e1);
				window.dispose();
			}
		});
		window.getContentPane().add(new JScrollPane(list));
		window.pack();
		window.setLocationRelativeTo(parent);
		window.setLocation(window.getX(), window.getY() - 60);
		window.setVisible(true);
		list.ensureIndexIsVisible(list.getSelectedIndex());
		EventQueue.invokeLater(list::requestFocus);
	}
}
