/*
 * CloseDialog.java - Close all buffers dialog
 * Copyright (C) 1999, 2000 Slava Pestov
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

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;

public class CloseDialog extends EnhancedDialog
{
	public CloseDialog(View view)
	{
		super(view,jEdit.getProperty("close.title"),true);

		this.view = view;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty("close.caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		content.add(BorderLayout.NORTH,label);

		bufferList = new JList(bufferModel = new DefaultListModel());
		bufferList.setVisibleRowCount(10);
		bufferList.addListSelectionListener(new ListHandler());

		Buffer[] buffers = jEdit.getBuffers();
		for(int i = 0; i < buffers.length; i++)
		{
			Buffer buffer = buffers[i];
			if(buffer.isDirty())
			{
				bufferModel.addElement(buffer.getPath());
			}
		}

		content.add(BorderLayout.CENTER,new JScrollPane(bufferList));

		ActionHandler actionListener = new ActionHandler();

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());
		buttons.add(save = new JButton(jEdit.getProperty("close.save")));
		save.setMnemonic(jEdit.getProperty("close.save.mnemonic").charAt(0));
		save.addActionListener(actionListener);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(discard = new JButton(jEdit.getProperty("close.discard")));
		discard.setMnemonic(jEdit.getProperty("close.discard.mnemonic").charAt(0));
		discard.addActionListener(actionListener);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(cancel = new JButton(jEdit.getProperty("common.cancel")));
		cancel.addActionListener(actionListener);
		buttons.add(Box.createGlue());

		bufferList.setSelectedIndex(0);

		content.add(BorderLayout.SOUTH,buttons);

		GUIUtilities.requestFocus(this,bufferList);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public boolean isOK()
	{
		return ok;
	}

	// EnhancedDialog implementation
	public void ok()
	{
		// do nothing
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	// private members
	private View view;
	private JList bufferList;
	private DefaultListModel bufferModel;
	private JButton save;
	private JButton discard;
	private JButton cancel;

	private boolean ok; // only set if all buffers saved/closed

	private void updateButtons()
	{
		int index = bufferList.getSelectedIndex();
		save.getModel().setEnabled(index != -1);
		discard.getModel().setEnabled(index != -1);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == save)
			{
				Object[] paths = bufferList.getSelectedValues();

				for(int i = 0; i < paths.length; i++)
				{
					String path = (String)paths[i];
					Buffer buffer = jEdit.getBuffer(path);
					if(!buffer.save(view,null,true))
						return;
					VFSManager.waitForRequests();
					jEdit._closeBuffer(view,buffer);
					bufferModel.removeElement(path);
				}

				updateButtons();

				if(bufferModel.getSize() == 0)
				{
					ok = true;
					dispose();
				}
			}
			else if(source == discard)
			{
				Object[] paths = bufferList.getSelectedValues();

				for(int i = 0; i < paths.length; i++)
				{
					String path = (String)paths[i];
					Buffer buffer = jEdit.getBuffer(path);
					jEdit._closeBuffer(view,buffer);
					bufferModel.removeElement(path);
				}

				updateButtons();

				if(bufferModel.getSize() == 0)
				{
					ok = true;
					dispose();
				}
			}
			else if(source == cancel)
				cancel();
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			int index = bufferList.getSelectedIndex();
			if(index != -1)
				view.setBuffer(jEdit.getBuffer((String)
					bufferModel.getElementAt(index)));

			updateButtons();
		}
	}
}
