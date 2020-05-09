/*
 * EnhancedMenuItem.java - Menu item with user-specified accelerator string
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.gui.statusbar.HoverSetStatusMouseHandler;
import org.jedit.keymap.Keymap;
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
		shortcut = GUIUtilities.getShortcutLabel(action, true);
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
			addMouseListener(new HoverSetStatusMouseHandler(action));
		}
		else
			setEnabled(false);
	} //}}}

	//{{{ getPreferredSize() method
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();

		if(shortcut != null)
		{
			FontMetrics fm = getFontMetrics(acceleratorFont);
			d.width += (fm.stringWidth(shortcut) + fm.stringWidth("AAAA"));
		}
		return d;
	} //}}}

	//{{{ paint() method
	@Override
	public void paint(Graphics g)
	{
		super.paint(g);

		if(shortcut != null)
		{
			Graphics2D g2 = (Graphics2D)g;
			g.setFont(acceleratorFont);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getModel().isArmed() ?
				acceleratorSelectionForeground :
				acceleratorForeground);
			FontMetrics fm = g.getFontMetrics();
			Insets insets = getInsets();
			g.drawString(shortcut,getWidth() - (fm.stringWidth(
				shortcut) + insets.right + insets.left + 5),
				fm.getAscent() + insets.top);
		}
	} //}}}

	//{{{ Package-private members
	static Font acceleratorFont;
	static Color acceleratorForeground;
	static Color acceleratorSelectionForeground;
	//}}}

	//{{{ Private members

	//{{{ Instance variables
	@Nullable
	private String shortcut;
	//}}}

	//{{{ Class initializer
	static
	{
		acceleratorFont = GUIUtilities.menuAcceleratorFont();

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
}
