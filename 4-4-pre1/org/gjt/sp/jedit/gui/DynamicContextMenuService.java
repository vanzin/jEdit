package org.gjt.sp.jedit.gui;

import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;

import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 * A service that can be offered by plugins when a text area context menu item 
 * needs to be offered  that is sensitive to the state of the TextArea it was requested from.

 * Note: this service should only be used by certain plugins that need context information at the time
 * that the context menu is requested. For all other actions, it is already possible for users to
 * add menu items to the context menu, so please do not use this service from Plugins
 * to add non-dynamic actions to the context menu.
 *  
 * @author ezust
 * @since jEdit 4.3pre15
 */
abstract public class DynamicContextMenuService {
	/**
	 * 
	 * @param ta the TextArea where the context menu was requested.
	 * 	   Use this to determine the location of the caret, or the edit mode of the buffer, etc.
	 * @param evt a mouseEvent that triggered this menu request, or null
	 * @return an array of menu items
	 *         or null if there are no appropriate actions to be added at this time
	 */
	public abstract JMenuItem[] createMenu(JEditTextArea ta, MouseEvent evt); 

}
