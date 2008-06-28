package org.gjt.sp.jedit.gui;

import javax.swing.JMenuItem;

import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 * A service that can be offered by plugins when a context menu item needs to be offered
 * that is sensitive to the state of the editpane it was requested from.
 * @author ezust
 * @since jEdit 4.3pre15
 */
abstract public class DynamicContextMenuService {
	/**
	 * 
	 * @param pane the TextArea where the context menu was requested.
	 * 	   Use this to determine the location of the caret, or the edit mode of the buffer, etc.
	 * @return a dynamic menu item (or JMenu) dependent on state of pane
	 *         or null if there is no appropriate action to be added at this time
	 */
	public abstract JMenuItem createMenu(JEditTextArea ta); 

}
