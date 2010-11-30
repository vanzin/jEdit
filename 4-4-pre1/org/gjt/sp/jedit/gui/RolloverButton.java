/*
 * RolloverButton.java - Class for buttons that implement rollovers
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
 * Portions copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicBorders.ButtonBorder;
import org.gjt.sp.jedit.OperatingSystem;

//}}}

/**
 * If you wish to have rollovers on your buttons, use this class.
 *
 * Unlike the Swing rollover support, this class works outside of
 * <code>JToolBar</code>s, and does not require undocumented client
 * property hacks or JDK1.4-specific API calls.<p>
 *
 * Note: You should not call <code>setBorder()</code> on your buttons,
 * as they probably won't work properly.
 * @version $Id$
 */
public class RolloverButton extends JButton
{
	//{{{ RolloverButton constructor
	/**
	 * Setup the border (invisible initially)
	 */
	public RolloverButton()
	{
		//setContentAreaFilled(true);
		addMouseListener(new MouseOverHandler());
	} //}}}

	//{{{ RolloverButton constructor
	/**
	 * Setup the border (invisible initially)
	 *
	 * @param icon the icon of this button
	 */
	public RolloverButton(Icon icon)
	{
		this();

		setIcon(icon);
	} //}}}

	//{{{ updateUI() method
	public void updateUI()
	{
		super.updateUI();
		//setBorder(originalBorder);
		setBorderPainted(false);
		setRequestFocusEnabled(false);
		setMargin(new Insets(1,1,1,1));
	} //}}}

	//{{{ setEnabled() method
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		setBorderPainted(false);
		repaint();
	} //}}}

	//{{{ setBorderPainted() method
	public void setBorderPainted(boolean b)
	{
		try
		{
			revalidateBlocked = true;
			super.setBorderPainted(b);
			setContentAreaFilled(b);
		}
		finally
		{
			revalidateBlocked = false;
		}
	} //}}}

	//{{{ revalidate() method
	/**
	 * We block calls to revalidate() from a setBorderPainted(), for
	 * performance reasons.
	 */
	public void revalidate()
	{
		if(!revalidateBlocked)
			super.revalidate();
	} //}}}

	//{{{ paint() method
	public void paint(Graphics g)
	{
		if(isEnabled())
			super.paint(g);
		else
		{
			Graphics2D g2 = (Graphics2D)g;
			g2.setComposite(c);
			super.paint(g2);
		}
	} //}}}

	//{{{ Private members
	private static final AlphaComposite c = AlphaComposite.getInstance(
		AlphaComposite.SRC_OVER, 0.5f);

	private boolean revalidateBlocked;

	//{{{ MouseHandler class
	/**
	 * Make the border visible/invisible on rollovers
	 */
	class MouseOverHandler extends MouseAdapter
	{
		public void mouseEntered(MouseEvent e)
		{
			setContentAreaFilled(true);
			setBorderPainted(isEnabled());
		}

		public void mouseExited(MouseEvent e)
		{
			setContentAreaFilled(false);
			setBorderPainted(false);
		}
	} //}}}
	//}}}
}
