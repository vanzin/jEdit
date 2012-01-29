/*
 * EnhancedCheckBoxMenuItem.java - Check box menu item
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
import org.gjt.sp.util.Log;
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
	public EnhancedCheckBoxMenuItem(String label, String action,
		ActionContext context)
	{
		this.context = context;
		this.action = action;
		this.shortcut = GUIUtilities.getShortcutLabel(action);
		String toolTip = jEdit.getProperty(action+ ".tooltip");
		if (toolTip != null) {
			setToolTipText(toolTip);
		}
		
		if(OperatingSystem.hasScreenMenuBar() && shortcut != null)
		{
			setText(label + " ( " + shortcut + " )");
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

		setModel(new Model());
	} //}}}

	//{{{ getPreferredSize() method
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();

		if(shortcut != null)
		{
			d.width += (getFontMetrics(EnhancedMenuItem.acceleratorFont)
				.stringWidth(shortcut) + 15);
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
			g.setFont(EnhancedMenuItem.acceleratorFont);
			g.setColor(getModel().isArmed() ?
				EnhancedMenuItem.acceleratorSelectionForeground :
				EnhancedMenuItem.acceleratorForeground);
			FontMetrics fm = g.getFontMetrics();
			Insets insets = getInsets();
			g.drawString(shortcut,getWidth() - (fm.stringWidth(
				shortcut) + insets.right + insets.left + 5),
				getFont().getSize() + (insets.top - 
				(OperatingSystem.isMacOSLF() ? 0 : 1))
				/* XXX magic number */);
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
	class Model extends DefaultButtonModel
	{
		@Override
		public boolean isSelected()
		{
			if(!isVisible())
				return false;

			EditAction a = context.getAction(action);
			if(a == null)
			{
				Log.log(Log.WARNING,this,"Unknown action: "
					+ action);
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

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		boolean msgSet = false;

		@Override
		public void mouseReleased(MouseEvent evt)
		{
			if(msgSet)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(null);
				msgSet = false;
			}
		}

		@Override
		public void mouseEntered(MouseEvent evt)
		{
			String msg = jEdit.getProperty(action + ".mouse-over");
			if(msg != null)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(msg);
				msgSet = true;
			}
		}

		@Override
		public void mouseExited(MouseEvent evt)
		{
			if(msgSet)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(null);
				msgSet = false;
			}
		}
	} //}}}
}
