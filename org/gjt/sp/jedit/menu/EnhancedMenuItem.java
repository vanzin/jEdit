/*
 * EnhancedMenuItem.java - Menu item with user-specified accelerator string
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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

package org.gjt.sp.jedit.menu;

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.gui.StatusBar;
import org.gjt.sp.jedit.keymap.Keymap;
//}}}

/**
 * jEdit's custom menu item. It adds support for multi-key shortcuts.
 */
public class EnhancedMenuItem extends JMenuItem
{
	//{{{ EnhancedMenuItem constructor
	/**
	 * Creates a new menu item. Most plugins should call
	 * GUIUtilities.loadMenuItem() instead.
	 * @param label The menu item label
	 * @param action The edit action
	 * @param context An action context
	 * @since jEdit 4.2pre1
	 */
	public EnhancedMenuItem(String label, String action, ActionContext context)
	{
		this.action = action;
		this.shortcut = GUIUtilities.getShortcutLabel(action);
		String toolTip = jEdit.getProperty(action+ ".tooltip");
		if (toolTip != null) {
			setToolTipText(toolTip);
		}

		if (OperatingSystem.hasScreenMenuBar() && shortcut != null)
		{
			if (jEdit.getBooleanProperty("menu.multiShortcut", false))
			{
				setText(label + " ( " + shortcut + " )");
			}
			else
			{
				setText(label);
				
				Keymap keymap = jEdit.getKeymapManager().getKeymap();
				String rawShortcut = keymap.getShortcut(action + ".shortcut");
				
				KeyStroke key = KeyEventTranslator.parseKeyStroke(rawShortcut);
				if (key != null)
					setAccelerator(key);
			}
			shortcut = null;
		}
		else
			setText(label);

		if(action != null)
		{
			setEnabled(true);
			addActionListener(new EditAction.Wrapper(context,action));
			addMouseListener(new MouseHandler());
		}
		else
			setEnabled(false);
	} //}}}

	//{{{ getPreferredSize() method
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();

		if(shortcut != null)
		{
			d.width += (getFontMetrics(acceleratorFont)
				.stringWidth(shortcut) + 15);
		}
		return d;
	} //}}}

	//{{{ paint() method
	public void paint(Graphics g)
	{
		super.paint(g);

		if(shortcut != null)
		{
			g.setFont(acceleratorFont);
			g.setColor(getModel().isArmed() ?
				acceleratorSelectionForeground :
				acceleratorForeground);
			FontMetrics fm = g.getFontMetrics();
			Insets insets = getInsets();
			g.drawString(shortcut,getWidth() - (fm.stringWidth(
				shortcut) + insets.right + insets.left + 5),
				getFont().getSize() + (insets.top - 
				(OperatingSystem.isMacOSLF() ? 0 : 1))
				/* XXX magic number */);
		}
	} //}}}

	//{{{ Package-private members
	static Font acceleratorFont;
	static Color acceleratorForeground;
	static Color acceleratorSelectionForeground;
	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private String shortcut;
	private String action;
	//}}}

	//{{{ Class initializer
	static
	{
		String shortcutFont;
		if (OperatingSystem.isMacOSLF())
		{
			shortcutFont = "Lucida Grande";
		}
		else
		{
			shortcutFont = "Monospaced";
		}
		
		acceleratorFont = UIManager.getFont("MenuItem.acceleratorFont");
		if(acceleratorFont == null)
		{
			acceleratorFont = new Font(shortcutFont,Font.PLAIN,12);
		}
		acceleratorForeground = UIManager
			.getColor("MenuItem.acceleratorForeground");
		if(acceleratorForeground == null)
		{
			acceleratorForeground = Color.black;
		}

		acceleratorSelectionForeground = UIManager
			.getColor("MenuItem.acceleratorSelectionForeground");
		if(acceleratorSelectionForeground == null)
		{
			acceleratorSelectionForeground = Color.black;
		}
	} //}}}

	//}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		boolean msgSet = false;
		private String msg;

		public void mouseReleased(MouseEvent evt)
		{
			cleanupStatusBar(evt);
		}

		public void mouseEntered(MouseEvent evt)
		{
			msg = jEdit.getProperty(action + ".mouse-over");
			if(msg != null)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(msg);
				msgSet = true;
			}
		}

		public void mouseExited(MouseEvent evt)
		{
			cleanupStatusBar(evt);
		}

		private void cleanupStatusBar(MouseEvent evt)
		{
			if(msgSet)
			{
				StatusBar statusBar = GUIUtilities.getView((Component) evt.getSource())
					.getStatus();
				if (msg == statusBar.getMessage())
				{
					statusBar.setMessage(null);
				}
				msgSet = false;
				msg = null;
			}
		}
	} //}}}
}
