/*
 * HelpSearchPanel.java - Help search GUI
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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

package org.gjt.sp.jedit.help;

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

public class HelpSearchPanel extends JPanel
{
	//{{{ HelpSearchPanel constructor
	public HelpSearchPanel(HelpViewer helpViewer)
	{
		super(new BorderLayout(6,6));

		this.helpViewer = helpViewer;

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(jEdit.getProperty("helpviewer.search.caption")));
		box.add(Box.createHorizontalStrut(6));
		box.add(searchField = new HistoryTextField("helpviewer.search"));
		searchField.addActionListener(new ActionHandler());

		add(BorderLayout.NORTH,box);

		results = new JList();
		results.addMouseListener(new MouseHandler());
		results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		results.setCellRenderer(new ResultRenderer());
		add(BorderLayout.CENTER,new JScrollPane(results));
	} //}}}

	//{{{ Private members
	private HelpViewer helpViewer;
	private HistoryTextField searchField;
	private JList results;
	private HelpIndex index;

	private HelpIndex getHelpIndex()
	{
		if(index == null)
		{
			index = new HelpIndex();
			try
			{
				index.indexEditorHelp();
			}
			catch(Exception e)
			{
				index = null;
				Log.log(Log.ERROR,this,e);
				GUIUtilities.error(helpViewer,"helpviewer.search.error",
					new String[] { e.toString() });
			}
		}

		return index;
	} //}}}

	//}}}

	//{{{ Result class
	class Result
	{
		int rank;
		String title;
		String file;

		Result(String title, String file)
		{
			this.title = title;
			this.file = file;
		}

		public String toString()
		{
			return rank + ":" + title + ":" + file;
		}

		public boolean equals(Object o)
		{
			if(o instanceof Result)
				return ((Result)o).file.equals(file);
			else
				return false;
		}
	} //}}}

	//{{{ ResultRenderer class
	class ResultRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,null,index,
				isSelected,cellHasFocus);

			Result result = (Result)value;
			setText(result.title);

			return this;
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			HelpIndex index = getHelpIndex();
			if(index == null)
				return;

			StringTokenizer st = new StringTokenizer(
				searchField.getText(),",.;:- ");

			DefaultListModel resultModel = new DefaultListModel();

			while(st.hasMoreTokens())
			{
				String word = st.nextToken().toLowerCase();
				HelpIndex.Word lookup = index.getWord(word);
				if(lookup != null)
				{
					for(int i = 0; i < lookup.fileCount; i++)
					{
						Result result = new Result(
							MiscUtilities.getFileName(lookup.files[i]),
							lookup.files[i]);
						int idx = resultModel.indexOf(result);

						// if not in list, add; otherwise increment
						// rank
						if(idx == -1)
							resultModel.addElement(result);
						else
						{
							((Result)resultModel.getElementAt(idx))
								.rank += 1;
						}
					}
				}
			}

			results.setModel(resultModel);

			if(resultModel.getSize() == 0)
				getToolkit().beep();
		}
	} //}}}

	//{{{ MouseHandler class
	public class MouseHandler extends MouseAdapter
	{
		public void mouseReleased(MouseEvent evt)
		{
			int row = results.locationToIndex(evt.getPoint());
			if(row != -1)
			{
				Result result = (Result)results.getModel()
					.getElementAt(row);
				helpViewer.gotoURL(result.file,true);
			}
		}
	} //}}}
}
