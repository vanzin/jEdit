/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:
*/ 
package org.gjt.sp.jedit.options;

import java.awt.Frame;

import org.gjt.sp.jedit.jEdit;
// import org.gjt.sp.jedit.options.BufferOptionPane;

// {{{ class CombinedOptions
/**
 * 
 * An OptionDialog which combines Global and Plugin options 2 tabs on a single
 * dialog
 * 
 * @author Alan Ezust
 * 
 */

public class CombinedOptions extends TabbedOptionDialog
{
    
	// {{{ Members

	GlobalOptionGroup globalOptions;
	PluginOptionGroup pluginOptions;
	// BufferOptionPane bufferOptions;

	int startingIndex = 0;

	// }}}

	/**
	 * Remembers the previously used tab.
	 */
	public static CombinedOptions combinedOptions(Frame parent) {
		int startingIndex = jEdit.getIntegerProperty("optional.last.tab", 0);
		return new CombinedOptions(parent, startingIndex);
	}
	
	// {{{ CombinedOptions Constructors
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

