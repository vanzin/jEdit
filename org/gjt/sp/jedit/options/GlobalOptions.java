/*
 * GlobalOptions.java - Global options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
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

package org.gjt.sp.jedit.options;

import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.options.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class GlobalOptions extends OptionsDialog
{
	public GlobalOptions(View view)
	{
		super(view,"options",jEdit.getProperty("options.last"));
	}

	public GlobalOptions(View view, String pane)
	{
		super(view,"options",pane);
	}

	protected OptionTreeModel createOptionTreeModel()
	{
		OptionTreeModel paneTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) paneTreeModel.getRoot();

		// initialize the jEdit branch of the options tree
		jEditGroup = new OptionGroup("jedit");

		addOptionPane(new GeneralOptionPane(), jEditGroup);
		addOptionPane(new AppearanceOptionPane(), jEditGroup);
		addOptionPane(new TextAreaOptionPane(), jEditGroup);
		addOptionPane(new GutterOptionPane(), jEditGroup);
		addOptionPane(new SyntaxHiliteOptionPane(), jEditGroup);
		addOptionPane(new LoadSaveOptionPane(), jEditGroup);
		addOptionPane(new EditingOptionPane(), jEditGroup);
		addOptionPane(new ModeOptionPane(), jEditGroup);
		addOptionPane(new AbbrevsOptionPane(), jEditGroup);
		addOptionPane(new ShortcutsOptionPane(), jEditGroup);
		addOptionPane(new DockingOptionPane(), jEditGroup);
		addOptionPane(new ContextOptionPane(), jEditGroup);
		addOptionPane(new ToolBarOptionPane(), jEditGroup);
		addOptionPane(new StatusBarOptionPane(), jEditGroup);
		addOptionPane(new PrintOptionPane(), jEditGroup);
		addOptionPane(new FirewallOptionPane(), jEditGroup);
		addOptionGroup(jEditGroup, rootGroup);

		browserGroup = new OptionGroup("browser");
		addOptionPane(new BrowserOptionPane(), browserGroup);
		addOptionPane(new BrowserColorsOptionPane(), browserGroup);
		addOptionGroup(browserGroup, rootGroup);

		// initialize the Plugins branch of the options tree
		pluginsGroup = new OptionGroup("plugins");

		// Query plugins for option panes
		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin ep = plugins[i];
			try
			{
				ep.createOptionPanes(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR, ep,
					"Error creating option pane");
				Log.log(Log.ERROR, ep, t);
			}
		}

		// only add the Plugins branch if there are OptionPanes
		if (pluginsGroup.getMemberCount() > 0)
		{
			addOptionGroup(pluginsGroup, rootGroup);
		}

		return paneTreeModel;
	}

	protected OptionGroup getDefaultGroup()
	{
		return pluginsGroup;
	}

	private OptionGroup jEditGroup;
	private OptionGroup browserGroup;
	private OptionGroup pluginsGroup;
}
