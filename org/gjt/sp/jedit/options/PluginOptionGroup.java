/*
 * PluginOptionGroup.java - Plugin options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov, Alan Ezust
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
package org.gjt.sp.jedit.options;

import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionsDialog.OptionTreeModel;
import org.gjt.sp.jedit.options.PluginOptions.NoPluginsPane;
import org.gjt.sp.util.Log;

/**
  * NOTE: This version no longer shows options from plugins that
  * use the deprecated APIs.  
  * @since jedit4.3pre3
  * 
*/
// {{{

/**
*  Refactored from PluginOptions.java - this class contains only the OptionGroup
*  and none of the GUI code.
*/

public class PluginOptionGroup extends OptionGroup 
{
	public PluginOptionGroup() {
		super("plugins");
		createOptionTreeModel();
	}
	
	public OptionTreeModel createOptionTreeModel() {
		OptionTreeModel paneTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) paneTreeModel.getRoot();

		// initialize the Plugins branch of the options tree
		setSort(true);

		// Query plugins for option panes
		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin ep = plugins[i];
			if(ep instanceof EditPlugin.Broken)
				continue;

			String className = ep.getClassName();
			String optionPane = jEdit.getProperty(
				"plugin." + className + ".option-pane");
			if(optionPane != null)
				addOptionPane(optionPane);
			else
			{
				String options = jEdit.getProperty(
					"plugin." + className
					+ ".option-group");
				if(options != null)
				{
					addOptionGroup(
						new OptionGroup(
						"plugin." + className,
						jEdit.getProperty("plugin."
						+ className + ".name"),
						options)
					);
				}
			}

		}

		// only add the Plugins branch if there are OptionPanes
		if (getMemberCount() == 0)
			addOptionPane(new NoPluginsPane());

		rootGroup.addOptionGroup(this);

		return paneTreeModel;
	} //}}}		
		
}

