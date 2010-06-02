/*
 * HistoryButton.java - History Button
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Nicholas O'Leary
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.RolloverButton;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
//}}}

/**
 * History Button
 * @author Nicholas O'Leary
 * @version $Id$
 */
public class HistoryButton extends JPanel implements ActionListener
{
	public static final int BACK    = 0;
	public static final int FORWARD = 1;

	//{{{ Private Members
	private int type;
	private HelpHistoryModel history;
	private RolloverButton arrow_button;
	private RolloverButton drop_button;
	private JPopupMenu historyList;
	private ActionListener arrowActionListener;
	//}}}

	//{{{ HistoryButton constructor
	public HistoryButton(int type, HelpHistoryModel model)
	{
		super();
		arrow_button = new RolloverButton(GUIUtilities.loadIcon(
			jEdit.getProperty(type==BACK
						? "helpviewer.back.icon"
						: "helpviewer.forward.icon")));
		arrow_button.setToolTipText(
			jEdit.getProperty(type==BACK
						? "helpviewer.back.label"
						: "helpviewer.forward.label"));
		Box box = new Box(BoxLayout.X_AXIS);
		drop_button = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("dropdown-arrow.icon")));
		drop_button.addActionListener(new DropActionHandler());
		box.add(arrow_button);
		box.add(drop_button);
		this.setMaximumSize(new Dimension(
			drop_button.getPreferredSize().width +
			arrow_button.getPreferredSize().width +
			5,
			arrow_button.getPreferredSize().height + 10)
			);
		this.add(box);
		this.type = type;
		this.history = model;
	} //}}}

	//{{{ setEnabled() method
	public void setEnabled(boolean state)
	{
		super.setEnabled(state);
		drop_button.setEnabled(state);
		arrow_button.setEnabled(state);
	} //}}}

	//{{{ addActionListener() method
	public void addActionListener(ActionListener al)
	{
		arrow_button.addActionListener(this);
		arrowActionListener = al;
	} //}}}

	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent evt)
	{
		arrowActionListener.actionPerformed(
			new ActionEvent(this,
				ActionEvent.ACTION_PERFORMED,
				evt.getActionCommand(),
				evt.getWhen(),
				evt.getModifiers()
				)
			);
	} //}}}

	//{{{ getParentHistoryButton() method
	private HistoryButton getParentHistoryButton()
	{
		return this;
	} //}}}

	//{{{ Inner Classes

	//{{{ DropActionHandler class
	class DropActionHandler implements ActionListener
	{
		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			historyList = new JPopupMenu();
			HelpHistoryModel.HistoryEntry[] urls;
			if (type == BACK)
			{
				urls = history.getPreviousURLs();
			}
			else
			{
				urls = history.getNextURLs();
			}
			if (urls != null)
			{
				if (type == BACK) {
					for (int i=urls.length-1 ; i>=0 ; i--)
					{
						if (urls[i] != null)
						{
							historyList.add(new HistoryListActionHandler(urls[i]));
						}
					}
				}
				else
				{
					for (int i=0 ; i<urls.length ; i++)
					{
						if (urls[i] != null)
						{
							historyList.add(new HistoryListActionHandler(urls[i]));
						}
					}
				}

				historyList.show((JComponent)evt.getSource(),0,0);
			}
		} //}}}
	} //}}}

	//{{{ HistoryListActionHandler class
	class HistoryListActionHandler extends AbstractAction
	{
		HelpHistoryModel.HistoryEntry entry;

		//{{{ HistoryListActionHandler constructor
		HistoryListActionHandler(HelpHistoryModel.HistoryEntry entry)
		{
			super(entry.title);
			this.entry = entry;
			this.putValue(Action.ACTION_COMMAND_KEY,entry.url + ':' + entry.scrollPosition);
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent ae)
		{
			getParentHistoryButton().actionPerformed(ae);
			history.setCurrentEntry(entry);
		} //}}}
	} //}}}

	//}}}
}
