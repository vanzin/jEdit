package org.gjt.sp.jedit.options;

import java.awt.Dialog;
import java.awt.Frame;


import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionDialog;

/**
 * 
 * An OptionDialog which combines all of jEdit's options into 3 tabs on a single dialog.
 * 
 * @author ezust
 *
 */
public class CombinedOptions extends OptionDialog 
{
	GlobalOptionGroup globalOptions;
	PluginOptionGroup pluginOptions;
	BufferOptionPane bufferOptions;
	
	public CombinedOptions(Dialog parent) {
		super(parent, "options");
		init();
	}
	public CombinedOptions(Frame parent) {
		super(parent, "options");
		init();
	}
	
	public void init() {
		String title = jEdit.getProperty("options.title");
		setTitle(title);
		addOptionGroup(new GlobalOptionGroup());
		addOptionGroup(new PluginOptionGroup());
		addOptionPane(new BufferOptionPane());
		show();
	}
	
}
