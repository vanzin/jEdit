/*
 * PasteFromListDialog.java - Paste previous/paste deleted dialog
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.GenericGUIUtilities;
//}}}

/** Paste previous/paste deleted dialog */
public class PasteFromListDialog extends EnhancedDialog
{
	//{{{ PasteFromListDialog constructor
	public PasteFromListDialog(String name, View view, MutableListModel<String> model)
	{
		super(view,jEdit.getProperty(name + ".title"),true);
		this.view = view;
		listModel = model;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
		setContentPane(content);
		JPanel center = new JPanel(new GridLayout(2, 1, 2, 12));

		int maxItemLength =
			jEdit.getIntegerProperty("paste-from-list.max-item-length", 1000);
		clips = new JList<>(model);
		clips.setCellRenderer(new Renderer(maxItemLength));
		clips.setVisibleRowCount(12);

		clips.addMouseListener(new MouseHandler());
		clips.addListSelectionListener(e ->
		{
			showClipText();
			updateButtons();
		});

		insert = new JButton(jEdit.getProperty("common.insert"));
		JButton cancel = new JButton(jEdit.getProperty("common.cancel"));

		JLabel label = new JLabel(jEdit.getProperty(name + ".caption"));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		content.add(BorderLayout.NORTH,label);

		JScrollPane scroller = new JScrollPane(clips);
		scroller.setPreferredSize(new Dimension(500, 150));
		center.add(scroller);

		clipText = new JTextArea();
		clipText.setEditable(false);
		scroller = new JScrollPane(clipText);
		scroller.setPreferredSize(new Dimension(500, 150));
		center.add(scroller);

		content.add(center, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(17, 0, 0, 0));
		panel.add(Box.createGlue());
		panel.add(insert);
		panel.add(Box.createHorizontalStrut(6));
		panel.add(cancel);
		
		GenericGUIUtilities.makeSameSize(insert, cancel);

		content.add(panel, BorderLayout.SOUTH);

		if(model.getSize() >= 1)
			clips.setSelectedIndex(0);
		updateButtons();

		getRootPane().setDefaultButton(insert);
		insert.addActionListener(e -> ok());
		cancel.addActionListener(e -> cancel());

		GenericGUIUtilities.requestFocus(this,clips);

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		java.util.List<String> selected = clips.getSelectedValuesList();
		if(selected == null || selected.isEmpty())
		{
			javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
			return;
		}

		String text = getSelectedClipText();

		/*
		 * For each selected clip, we remove it, then add it back
		 * to the model. This has the effect of moving it to the
		 * top of the list.
		 */
		for (String sel : selected)
		{
			listModel.removeElement(sel);
			listModel.insertElementAt(sel, 0);
		}

		view.getTextArea().setSelectedText(text);

		cleanup();
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		cleanup();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final View view;
	private final MutableListModel<String> listModel;
	private final JList<String> clips;
	private final JTextArea clipText;
	private final JButton insert;
	//}}}

	//{{{ cleanup()
	private void cleanup()
	{
		// Remove the reference to the JList from the history model so that the
		// list doesn't keep getting updated after the dialog is gone
		String[] nothing = {};
		clips.setListData(nothing);
		dispose();
	} //}}}

	//{{{ getSelectedClipText()
	private String getSelectedClipText()
	{
		java.util.List<String> selected = clips.getSelectedValuesList();
		if (selected == null || selected.isEmpty())
			return "";
		if (selected.size() == 1)
		{
			// These strings may be very large, so if we can just return the same string
			// instead of making a copy, do so
			return selected.get(0);
		}
		else
		{
			StringBuilder clip = new StringBuilder();
			for(int i = 0; i < selected.size(); i++)
			{
				if(i != 0)
					clip.append('\n');
				clip.append(selected.get(i));
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
		java.util.List<String> selected = clips.getSelectedValuesList();
		if(selected == null || selected.isEmpty())
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
				clipText.setText(text.substring(0, maxPreviewLength) + '<' + (text.length() - maxPreviewLength) + " more bytes>");
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
	private static class Renderer extends DefaultListCellRenderer
	{
		Renderer(int maxItemLength)
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
					if(!ws)
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

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			setText(shorten(value.toString()));

			return this;
		}

		private final int maxItemLength;
	} //}}}

	//{{{ MouseHandler class
	private class MouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			if(evt.getClickCount() == 2)
				ok();
		}
	} //}}}
}
