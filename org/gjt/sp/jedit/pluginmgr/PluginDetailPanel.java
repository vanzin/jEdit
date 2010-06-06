/*
 * PluginDetailPanel.java - Displays the details of a plugin
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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

package org.gjt.sp.jedit.pluginmgr;

//{{{ Imports
import java.awt.BorderLayout;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.pluginmgr.ManagePanel.Entry;
//}}}

/**
 * @author Matthieu Casanova
 * 
 */
class PluginDetailPanel extends JPanel
{
	private final JEditorPane pluginDetail;
	private final JLabel title;
	
	/** The current entry. */
	private Entry entry;
	
	//{{{ PluginDetailPanel constructor
	PluginDetailPanel()
	{
		setLayout(new BorderLayout());
		pluginDetail = new JEditorPane();
		pluginDetail.setEditable(false);
		pluginDetail.setContentType("text/html");
		pluginDetail.setBackground(jEdit.getColorProperty("view.bgColor"));
		pluginDetail.setForeground(jEdit.getColorProperty("view.fgColor"));
		pluginDetail.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		title = new JLabel();
		add(title, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(pluginDetail);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll);
	} //}}}

	//{{{ setPlugin() method
	void setPlugin(Entry entry)
	{
		if (entry != this.entry)
		{
			if (entry.status.equals(Entry.LOADED))
			{
				if (entry.name == null)
					title.setText("<html><b>"+entry.jar+"</b></html>");
				else
					title.setText("<html><b>"+entry.name+"</b></html>");
				
				StringBuilder builder = new StringBuilder();
				
				if (entry.version != null)
					builder.append("<b>Version</b>: ").append(entry.version).append("<br>");
				if (entry.author != null)
					builder.append("<b>Author</b>: ").append(entry.author).append("<br>");

				if (entry.description != null)
				{
					builder.append("<br>").append(entry.description);
				}
				pluginDetail.setText(builder.toString());
			}
			else
			{
				title.setText("<html><b>"+entry.jar+"</b></html>");
				pluginDetail.setText(null);
			}
			this.entry = entry;
		}
	} //}}}

}
