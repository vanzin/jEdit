/*
 * CombinedOptions.java - Combined options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:
 *
 * Copyright (C) 2011 Alan Ezust
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
import java.awt.Frame;
import org.gjt.sp.jedit.jEdit;
// import org.gjt.sp.jedit.options.BufferOptionPane;

// {{{ class CombinedOptions
/**
 * 
 * An OptionDialog which combines Global and Plugin options 
 * into 2 tabs on a single dialog.
 * 
 * @author Alan Ezust
 * @since jEdit 5.0pre1 
 */

public class CombinedOptions extends TabbedOptionDialog
{

	// {{{ Members

	GlobalOptionGroup globalOptions;
	PluginOptionGroup pluginOptions;
	// BufferOptionPane bufferOptions;

	int startingIndex = 0;
	// }}}
	
	// {{{ CombinedOptions Constructors	
	/**
	 * Static constructor that remembers the previously used tab.
	 */
	public static CombinedOptions combinedOptions(Frame parent) 
	{
		int startingIndex = jEdit.getIntegerProperty("optional.last.tab", 0);
		return new CombinedOptions(parent, startingIndex);
	}
	

	public CombinedOptions(Frame parent, int tabIndex)
	{
		super(parent, "Combined Options");
		startingIndex = tabIndex;
		_init();

	}

	public CombinedOptions(Frame parent)
	{
		this(parent, 0);
	} // }}}

	// {{{ _init() method
	public void _init()
	{
		//String title = jEdit.getProperty("options.title");
		//setTitle(title);
		addOptionGroup(new GlobalOptionGroup());
		addOptionGroup(new PluginOptionGroup());
		// addOptionPane(new BufferOptionPane());
		setSelectedIndex(startingIndex);
		OptionGroupPane p = (OptionGroupPane) (tabs.getSelectedComponent());
		setTitle(p.getTitle());
		setVisible(true);
	}// }}}

}// }}}

