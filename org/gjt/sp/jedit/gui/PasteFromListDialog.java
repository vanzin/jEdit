/*
 * PasteFromListDialog.java - Paste previous/paste deleted dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;
//}}}

public class PasteFromListDialog extends EnhancedDialog
{
	//{{{ PasteFromListDialog constructor
	public PasteFromListDialog(String name, View view, MutableListModel model)
	{
		super(view,jEdit.getProperty(name + ".title"),true);
		this.view = view;
		this.listModel = model;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);
		JPanel center = new JPanel(new GridLayout(2,1,2,12));

		int maxItemLength =
			jEdit.getIntegerProperty("paste-from-list.max-item-length", 1000);
		clips = new JList(model);
		clips.setCellRenderer(new Renderer(maxItemLength));
		clips.setVisibleRowCount(12);

		clips.addMouseListener(new MouseHandler());
		clips.addListSelectionListener(new ListHandler());

		insert = new JButton(jEdit.getProperty("common.insert"));
		cancel = new JButton(jEdit.getProperty("common.cancel"));

		JLabel label = new JLabel(jEdit.getProperty(name + ".caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		content.add(BorderLayout.NORTH,label);

		JScrollPane scroller = new JScrollPane(clips);
		scroller.setPreferredSize(new Dimension(500,150));
		center.add(scroller);

		clipText = new JTextArea();
		clipText.setEditable(false);
		scroller = new JScrollPane(clipText);
		scroller.setPreferredSize(new Dimension(500,150));
		center.add(scroller);

		content.add(center, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		panel.add(insert);
		panel.add(Box.createHorizontalStrut(6));
		panel.add(cancel);
		panel.add(Box.createGlue());
		content.add(panel, BorderLayout.SOUTH);

		if(model.getSize() >= 1)
			clips.setSelectedIndex(0);
		updateButtons();

		getRootPane().setDefaultButton(insert);
		insert.addActionListener(new ActionHandler());
		cancel.addActionListener(new ActionHandler());

		GUIUtilities.requestFocus(this,clips);

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		Object[] selected = clips.getSelectedValues();
		if(selected == null || selected.length == 0)
		{
			getToolkit().beep();
			return;
		}

		String text = getSelectedClipText();

		/**
		 * For each selected clip, we remove it, then add it back
		 * to the model. This has the effect of moving it to the
		 * top of the list.
		 */
		for(int i = 0; i < selected.length; i++)
		{
			listModel.removeElement(selected[i]);
			listModel.insertElementAt(selected[i],0);
		}

		view.getTextArea().setSelectedText(text);

		cleanup();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		cleanup();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private View view;
	private MutableListModel listModel;
	private JList clips;
	private JTextArea clipText;
	private JButton insert;
	private JButton cancel;
	//}}}
	
	//{{{ cleanup()
	private void cleanup()
	{
		// Remove the reference to the JList from the history model so that the
		// list doesn't keep getting updated after the dialog is gone
		Object[] nothing = {};
		clips.setListData(nothing);
		dispose();
	} //}}}

	//{{{ getSelectedClipText()
	private String getSelectedClipText()
	{
		Object[] selected = clips.getSelectedValues();
		
		if (selected.length == 1)
		{
			// These strings may be very large, so if we can just return the same string
			// instead of making a copy, do so
			return selected[0].toString();
		}
		else
		{
			StringBuilder clip = new StringBuilder();
			for(int i = 0; i < selected.length; i++)
			{
				if(i != 0)
					clip.append('\n');
				clip.append(selected[i]);
			}
			return clip.toString();
		}
	}
	//}}}

	//{{{ updateButtons() method
	private void updateButtons()
	{
		int selected = clips.getSelectedIndex();
		insert.setEnabled(selected != -1);
	} //}}}

	//{{{ showClipText() method
	private void showClipText()
	{
		Object[] selected = clips.getSelectedValues();
		if(selected == null || selected.length == 0)
		{
			clipText.setText("");
		}
		else
		{
			String text = getSelectedClipText();
			int maxPreviewLength = 
				jEdit.getIntegerProperty("paste-from-list.max-preview-length",
					100000);
			
			if (text.length() > maxPreviewLength)
			{
				String showText = text.substring(0, maxPreviewLength);
				showText += "<" + (text.length() - maxPreviewLength) +
					" more bytes>";
				clipText.setText(showText);
			}
			else
			{
				clipText.setText(text);
			}
		}

		clipText.setCaretPosition(0);
	}
	//}}}

	//}}}

	//{{{ Renderer class
	static class Renderer extends DefaultListCellRenderer
	{
		public Renderer(int maxItemLength)
		{
			this.maxItemLength = maxItemLength;
		}
		
		String shorten(String item)
		{
			StringBuilder buf = new StringBuilder();
			// workaround for Swing rendering labels starting
			// with <html> using the HTML engine
			if(item.toLowerCase().startsWith("<html>"))
				buf.append(' ');
			boolean ws = true;
			for(int i = 0; i < item.length(); i++)
			{
				// Don't make the list items too large
				if (buf.length() == maxItemLength)
				{
					buf.append("...");
					break;
				}
				
				char ch = item.charAt(i);
				if(Character.isWhitespace(ch))
				{
					if(ws)
						/* do nothing */;
					else
					{
						buf.append(' ');
						ws = true;
					}
				}
				else
				{
					ws = false;
					buf.append(ch);
				}
			}

			if(buf.length() == 0)
				return jEdit.getProperty("paste-from-list.whitespace");
			return buf.toString();
		}

		public Component getListCellRendererComponent(
			JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			setText(shorten(value.toString()));

			return this;
		}
		
		private int maxItemLength;
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == insert)
				ok();
			else if(source == cancel)
				cancel();
		}
	} //}}}

	//{{{ ListHandler class
	class ListHandler implements ListSelectionListener
	{
		//{{{ valueChanged() method
		public void valueChanged(ListSelectionEvent evt)
		{
			showClipText();
			updateButtons();
		} //}}}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			if(evt.getClickCount() == 2)
				ok();
		}
	} //}}}
}
