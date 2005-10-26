/*
 * OptionsDialog.java - Tree options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

import org.gjt.sp.jedit.OptionGroup;

/**
 * A model for all of the Global Options.
 * 
 */

public class GlobalOptionGroup extends OptionGroup
{
	public GlobalOptionGroup()
	{
		this(null);
	}

	public GlobalOptionGroup(OptionGroup rootGroup)
	{
		super("options");
		addOptionPane("abbrevs");
		addOptionPane("appearance");
		addOptionPane("context");
		addOptionPane("docking");
		addOptionPane("editing");
		addOptionPane("general");
		addOptionPane("gutter");
		addOptionPane("mouse");
		addOptionPane("print");
		addOptionPane("plugin-manager");
		addOptionPane("firewall");
		addOptionPane("save-back");
		addOptionPane("shortcuts");
		addOptionPane("status");
		addOptionPane("syntax");
		addOptionPane("textarea");
		addOptionPane("toolbar");
		addOptionPane("view");
		OptionGroup browserGroup = new OptionGroup("browser");
		browserGroup.addOptionPane("browser.general");
		browserGroup.addOptionPane("browser.colors");
		if (rootGroup != null)
		{
			rootGroup.addOptionGroup(this);
			rootGroup.addOptionGroup(browserGroup);
		}
		else
		{
			addOptionGroup(browserGroup);
		}
	}
}
