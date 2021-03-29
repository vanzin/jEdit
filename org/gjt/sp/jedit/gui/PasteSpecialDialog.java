/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 jEdit contributors
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

import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.datatransfer.JEditDataFlavor;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.GenericGUIUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.List;

/**
 * "Paste Special" dialog.
 *
 * @author Matthieu Casanova
 */
public class PasteSpecialDialog extends EnhancedDialog
{
	private static final DataFlavor[] FLAVORS = {
		DataFlavor.stringFlavor,
		JEditDataFlavor.jEditRichTextDataFlavor,
		JEditDataFlavor.html};

	private final TextArea textArea;
	private final JList<DataFlavor> flavorList;

	public PasteSpecialDialog(View view, TextArea textArea)
	{
		super(view, jEdit.getProperty("paste-special.title"), true);
		this.textArea = textArea;
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		setContentPane(content);
		Registers.Register register = Registers.getRegister('$');
		Transferable transferable = register.getTransferable();
		DataFlavor[] flavors = transferable.getTransferDataFlavors();
		List<DataFlavor> flavorList = Arrays.asList(flavors);
		List<DataFlavor> supportedFlavors = new ArrayList<>(FLAVORS.length);
		for (DataFlavor flavor : FLAVORS)
		{
			if (flavorList.contains(flavor))
			{
				supportedFlavors.add(flavor);
			}
		}
		this.flavorList = new JList<>(supportedFlavors.toArray(new DataFlavor[0]));
		this.flavorList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value.equals(DataFlavor.stringFlavor))
				{
					setText("Plain text");
				}
				else if (value.equals(JEditDataFlavor.jEditRichTextDataFlavor))
				{
					setText("jEdit rich text");
				}
				else if (value.equals(JEditDataFlavor.html))
				{
					setText("html");
				}
				return this;
			}
		});
		getContentPane().add(new JScrollPane(this.flavorList));

		//{{{ Buttons

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(17, 0, 0, 0));
		buttons.add(Box.createGlue());

		JButton ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(e -> ok());
		getRootPane().setDefaultButton(ok);
		buttons.add(ok);

		buttons.add(Box.createHorizontalStrut(6));

		JButton cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(e -> cancel());
		buttons.add(cancel);

		GenericGUIUtilities.makeSameSize(ok, cancel);

		content.add(BorderLayout.SOUTH, buttons);
		//}}}

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	}

	@Override
	public void ok()
	{
		DataFlavor flavor = flavorList.getSelectedValue();
		if (flavor == null)
		{
			flavor = DataFlavor.stringFlavor;
		}
		Registers.paste(textArea, '$', flavor);
		dispose();
	}

	@Override
	public void cancel()
	{
		dispose();
	}
}
