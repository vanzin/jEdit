/*
 * LogViewer.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

public class LogViewer extends JPanel
{
	//{{{ LogViewer constructor
	public LogViewer()
	{
		super(new BorderLayout());

		Box captionBox = Box.createHorizontalBox();

		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			String[] args = { MiscUtilities.constructPath(
				settingsDirectory, "activity.log") };
			JLabel label = new JLabel(jEdit.getProperty(
				"log-viewer.caption",args));
			captionBox.add(label);
		}

		captionBox.add(Box.createHorizontalGlue());

		tailIsOn = jEdit.getBooleanProperty("log-viewer.tail", false);
		tail = new JCheckBox(
			jEdit.getProperty("log-viewer.tail.label"),tailIsOn);
		tail.addActionListener(new ActionHandler());
		captionBox.add(tail);

		ListModel model = Log.getLogListModel();
		model.addListDataListener(new ListHandler());
		list = new JList(model);
		list.setVisibleRowCount(24);
		list.setFont(jEdit.getFontProperty("view.font"));

		add(BorderLayout.NORTH,captionBox);
		JScrollPane scroller = new JScrollPane(list);
		Dimension dim = scroller.getPreferredSize();
		dim.width = Math.min(300,dim.width);
		scroller.setPreferredSize(dim);
		add(BorderLayout.CENTER,scroller);
	} //}}}

	//{{{ requestDefaultFocus() method
	public boolean requestDefaultFocus()
	{
		list.requestFocus();
		return true;
	} //}}}

	//{{{ Private members
	private JList list;
	private JCheckBox tail;
	private boolean tailIsOn;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			tailIsOn = !tailIsOn;
			jEdit.setBooleanProperty("log-viewer.tail",tailIsOn);
			if(tailIsOn)
			{
				int index = list.getModel().getSize();
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			}
		}
	} //}}}

	//{{{ ListHandler class
	class ListHandler implements ListDataListener
	{
		public void intervalAdded(ListDataEvent e)
		{
			contentsChanged(e);
		}

		public void intervalRemoved(ListDataEvent e)
		{
			contentsChanged(e);
		}

		public void contentsChanged(ListDataEvent e)
		{
			if(tailIsOn)
			{
				int index = list.getModel().getSize() - 1;
				list.ensureIndexIsVisible(index);
			}
		}
	} //}}}
}
