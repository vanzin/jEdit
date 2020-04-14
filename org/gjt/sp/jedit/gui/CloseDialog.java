/*
 * CloseDialog.java - Close all buffers dialog
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import java.util.Collection;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;

import org.gjt.sp.jedit.bufferio.BufferIORequest;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.manager.BufferManager;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.TaskManager;
//}}}

/** Close all buffers dialog
 * @author Slava Pestov
 */
public class CloseDialog extends EnhancedDialog
{
	//{{{ CloseDialog constructor
	public CloseDialog(View view)
	{
		this(view, jEdit.getBufferManager().getBuffers());
	}

	public CloseDialog(View view, Collection<Buffer> buffers)
	{
		super(view,jEdit.getProperty("close.title"),true);

		this.view = view;

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(UIManager.getIcon("OptionPane.warningIcon")));
		iconBox.add(Box.createGlue());
		content.add(BorderLayout.WEST,iconBox);

		JPanel centerPanel = new JPanel(new BorderLayout());

		JLabel label = new JLabel(jEdit.getProperty("close.caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		centerPanel.add(BorderLayout.NORTH,label);

		bufferList = new JList<String>(bufferModel = new DefaultListModel<String>());
		bufferList.setVisibleRowCount(10);
		bufferList.addListSelectionListener(new ListHandler());

		boolean suppressNotSavedConfirmUntitled = jEdit.getBooleanProperty("suppressNotSavedConfirmUntitled");
		suppressNotSavedConfirmUntitled = suppressNotSavedConfirmUntitled || jEdit.getBooleanProperty("autosaveUntitled");

		for(Buffer buffer: buffers)
		{
			if(buffer.isDirty() && !( buffer.isUntitled() && suppressNotSavedConfirmUntitled ))
				bufferModel.addElement(buffer.getPath());
		}

		centerPanel.add(BorderLayout.CENTER,new JScrollPane(bufferList));

		content.add(BorderLayout.CENTER,centerPanel);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());
		JButton selectAll;
		buttons.add(selectAll = new JButton(jEdit.getProperty("close.selectAll")));
		selectAll.setMnemonic(jEdit.getProperty("close.selectAll.mnemonic").charAt(0));
		selectAll.addActionListener((e) -> selectAll());
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(save = new JButton(jEdit.getProperty("close.save")));
		save.setMnemonic(jEdit.getProperty("close.save.mnemonic").charAt(0));
		save.addActionListener(e -> save());
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(discard = new JButton(jEdit.getProperty("close.discard")));
		discard.setMnemonic(jEdit.getProperty("close.discard.mnemonic").charAt(0));
		discard.addActionListener(e -> discard());
		buttons.add(Box.createHorizontalStrut(6));
		JButton cancel;
		buttons.add(cancel = new JButton(jEdit.getProperty("common.cancel")));
		cancel.addActionListener(e -> cancel());
		buttons.add(Box.createGlue());
		bufferList.setSelectedIndex(0);
		content.add(BorderLayout.SOUTH,buttons);
		content.getRootPane().setDefaultButton(cancel);
		GenericGUIUtilities.requestFocus(this,bufferList);
		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ isOK() method
	public boolean isOK()
	{
		return ok;
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		// do nothing
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members
	private final View view;
	private final JList<String> bufferList;
	private final DefaultListModel<String> bufferModel;
	private final JButton save;
	private final JButton discard;

	private boolean ok; // only set if all buffers saved/closed

	boolean selectAllFlag;

	private void updateButtons()
	{
		int index = bufferList.getSelectedIndex();
		save.getModel().setEnabled(index != -1);
		discard.getModel().setEnabled(index != -1);
	} //}}}

	private void discard()
	{
		java.util.List<String> paths = bufferList.getSelectedValuesList();

		BufferManager bufferManager = jEdit.getBufferManager();
		for (String path : paths)
		{
			bufferManager.getBuffer(path)
				.ifPresent(buffer ->
				{
					jEdit._closeBuffer(view, buffer);
					bufferModel.removeElement(path);
				});
		}

		if(bufferModel.getSize() == 0)
		{
			ok = true;
			dispose();
		}
		else
		{
			bufferList.setSelectedIndex(0);
			bufferList.requestFocus();
		}
	}

	private void save()
	{
		java.util.List<String> paths = bufferList.getSelectedValuesList();

		BufferManager bufferManager = jEdit.getBufferManager();
		for (String path : paths)
		{
			bufferManager.getBuffer(path)
				.filter(buffer -> buffer.save(view, null, true, true))
				.ifPresent(buffer ->
				{
					TaskManager.instance.waitForIoTasks();
					if (buffer.getBooleanProperty(BufferIORequest.ERROR_OCCURRED)) return;
					jEdit._closeBuffer(view, buffer);
					bufferModel.removeElement(path);
				});
		}

		if(bufferModel.getSize() == 0)
		{
			ok = true;
			dispose();
		}
		else
		{
			bufferList.setSelectedIndex(0);
			bufferList.requestFocus();
		}
	}

	private void selectAll()
	{
		// I'm too tired to think of a better way
		// to handle this right now.
		try
		{
			selectAllFlag = true;

			bufferList.setSelectionInterval(0,
				bufferModel.getSize() - 1);
		}
		finally
		{
			selectAllFlag = false;
		}
		bufferList.requestFocus();
	}

	//{{{ ListHandler class
	private class ListHandler implements ListSelectionListener
	{
		@Override
		public void valueChanged(ListSelectionEvent evt)
		{
			if(selectAllFlag)
				return;

			int index = bufferList.getSelectedIndex();
			if(index != -1)
			{
				String path = bufferModel.getElementAt(index);
				jEdit.getBufferManager()
					.getBuffer(path)
					.ifPresentOrElse(view::showBuffer, () -> removeIndex(index, path));
			}

			updateButtons();
		}

		private void removeIndex(int index, String path)
		{
			// it seems this buffer was already closed
			Log.log(Log.DEBUG, this, "Buffer " + path + " is already closed");
			bufferModel.removeElementAt(index);
		}
	} //}}}
}
