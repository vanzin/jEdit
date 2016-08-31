/*
 * PluginDetailPanel.java - Displays the details of a plugin
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008-2013 Matthieu Casanova, Dale Anson, Alan Ezust 
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
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.PluginJAR;
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
				
				StringBuilder sb = new StringBuilder(256);

				// <br> instead of <br/> because Sun's Java 5 HTML parser can't digest them.
				// No problem on Sun's Java 6 JVM.
				if (entry.version != null)
					sb.append("<b>").append(jEdit.getProperty("install-plugins.info.version", "Version")).append("</b>: ").append(entry.version).append("<br>");
				if (entry.author != null)
					sb.append("<b>").append(jEdit.getProperty("install-plugins.info.author", "Author")).append("</b>: ").append(entry.author).append("<br>");
				if (entry.description != null)
				{
					sb.append("<br>").append(entry.description);
				}
				sb.append(getDepends(entry));
				pluginDetail.setText(sb.toString());
			}
			else
			{
				title.setText("<html><b>"+entry.jar+"</b></html>");
				
				PluginJAR pluginJar = new PluginJAR(new File(entry.jar));
				pluginJar.init();
				entry.plugin = pluginJar.getPlugin();
				String clazz = pluginJar.getPlugin().getClassName();
				
				StringBuilder sb = new StringBuilder(256);
				sb.append("<b>").append(jEdit.getProperty("install-plugin.info.version", "Version")).append("</b>: ").append(jEdit.getProperty("plugin."+clazz+".version", ""));
				sb.append("<br><b>").append(jEdit.getProperty("install-plugin.info.author", "Author")).append("</b>: ").append(jEdit.getProperty("plugin."+clazz+".author", ""));
				sb.append("<br><br>").append(jEdit.getProperty("plugin."+clazz+".description", ""));
				sb.append(getDepends(entry));
				pluginDetail.setText(sb.toString());
				
				pluginJar.uninit(false);
			}
			pluginDetail.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

			this.entry = entry;
		}
	} //}}}
	
	//{{{ getDepends() method
	private String getDepends(Entry entry) 
	{
		StringBuilder builder = new StringBuilder();
		Set<String> dependencies = entry.getDependencies();
		if (dependencies != null && !dependencies.isEmpty()) 
		{
			builder.append("<br><br><b>").append(jEdit.getProperty("install-plugins.info.depends", "Depends on")).append("</b>:");
			List<String> depends = new ArrayList<String>(dependencies);
			Collections.sort(depends);
			int i = 0;
			for (String dep : depends) 
			{
				if (i > 0) builder.append(',');
				builder.append(' ').append(dep);
				++i;				
			}
		}
		return builder.toString();
	} //}}}
}
