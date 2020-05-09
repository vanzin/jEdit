/*
 * EnhancedCheckBoxMenuItem.java - Check box menu item
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
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.gui.statusbar.HoverSetStatusMouseHandler;
import org.jedit.keymap.Keymap;
//}}}

/**
 * jEdit's custom menu item. It adds support for multi-key shortcuts.
 */
public class EnhancedCheckBoxMenuItem extends JCheckBoxMenuItem
{
	//{{{ EnhancedCheckBoxMenuItem constructor
	/**
	 * Creates a new menu item. Most plugins should call
	 * GUIUtilities.loadMenuItem() instead.
	 * @param label The menu item label
	 * @param action The edit action
	 * @param context An action context
	 * @since jEdit 4.2pre1
	 */
	public EnhancedCheckBoxMenuItem(String label, String action, ActionContext context)
	{
		this.context = context;
		this.action = action;
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

		setModel(new Model());
	} //}}}

	//{{{ getPreferredSize() method
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();

		if(shortcut != null)
		{
			FontMetrics fm = getFontMetrics(EnhancedMenuItem.acceleratorFont);
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
			g.setFont(EnhancedMenuItem.acceleratorFont);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getModel().isArmed() ?
				EnhancedMenuItem.acceleratorSelectionForeground :
				EnhancedMenuItem.acceleratorForeground);
			FontMetrics fm = g.getFontMetrics();
			Insets insets = getInsets();
			g.drawString(shortcut,getWidth() - (fm.stringWidth(
				shortcut) + insets.right + insets.left + 5),
				fm.getAscent() + insets.top);
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final ActionContext context;
	private String shortcut;
	private final String action;
	//}}}

	//}}}

	//{{{ Model class
	private class Model extends DefaultButtonModel
	{
		@Override
		public boolean isSelected()
		{
			if(!isVisible())
				return false;

			EditAction a = context.getAction(action);
			if(a == null)
			{
				Log.log(Log.WARNING,this,"Unknown action: " + action);
				return false;
			}

			try
			{
				return a.isSelected(EnhancedCheckBoxMenuItem.this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,t);
				return false;
			}
		}

		@Override
		public void setSelected(boolean b) {}
	} //}}}

}
