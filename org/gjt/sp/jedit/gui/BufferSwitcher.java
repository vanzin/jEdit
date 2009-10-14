/*
 * BufferSwitcher.java - Status bar
 * Copyright (C) 2000, 2004 Slava Pestov
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

import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.bufferset.BufferSet;


/** BufferSwitcher class
   @version $Id$
*/
public class BufferSwitcher extends JComboBox
{
    // private members
	private EditPane editPane;
	private boolean updating;

	public BufferSwitcher(final EditPane editPane)
	{
		this.editPane = editPane;

		//setFont(new Font("Dialog",Font.BOLD,10));
		setRenderer(new BufferCellRenderer());
		setMaximumRowCount(jEdit.getIntegerProperty("bufferSwitcher.maxRowCount",10));
		addActionListener(new ActionHandler());
		addPopupMenuListener(new PopupMenuListener()
		{
			public void popupMenuWillBecomeVisible(
				PopupMenuEvent e) {}

			public void popupMenuWillBecomeInvisible(
				PopupMenuEvent e)
			{
				editPane.getTextArea().requestFocus();
			}

			public void popupMenuCanceled(PopupMenuEvent e)
			{
				editPane.getTextArea().requestFocus();
			}
		});
	}

	public void updateBufferList()
	{
		// if the buffer count becomes 0, then it is guaranteed to
		// become 1 very soon, so don't do anything in that case.
		BufferSet bufferSet = editPane.getBufferSet();
		if(bufferSet.size() == 0)
			return;

		updating = true;
		setMaximumRowCount(jEdit.getIntegerProperty("bufferSwitcher.maxRowCount",10));
		setModel(new DefaultComboBoxModel(bufferSet.getAllBuffers()));
		setSelectedItem(editPane.getBuffer());
		setToolTipText(editPane.getBuffer().getPath(true));
		updating = false;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(!updating)
			{
				Buffer buffer = (Buffer)getSelectedItem();
				if(buffer != null) 
					editPane.setBuffer(buffer);
			}
		}
	}

	class BufferCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(
			JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);
			Buffer buffer = (Buffer)value;
			
			if(buffer == null)
				setIcon(null);
			else
			{
				setIcon(buffer.getIcon());
				setToolTipText(buffer.getPath());
			}
			return this;
		}
	}
}
