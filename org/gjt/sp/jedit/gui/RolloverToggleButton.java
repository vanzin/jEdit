/*
 * RolloverToggleButton.java - Class for buttons that implement rollovers
 * :tabSize=4:indentSize=4:noTabs=false:
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

//}}}

/** Class for buttons that implement rollovers
 *
 * If you wish to have rollovers on your buttons, use this class.
 *
 * Unlike the Swing rollover support, this class works outside of
 * <code>JToolBar</code>s, and does not require undocumented client
 * property hacks or JDK1.4-specific API calls.<p>
 *
 * Note: You should not call <code>setBorder()</code> on your buttons,
 * as they probably won't work properly.
 * @version $Id: RolloverButton.java 21831 2012-06-18 22:54:17Z ezust $
 */
public class RolloverToggleButton extends JToggleButton
{
	
	private Border originalBorder;
	private Border rolloverBorder;
	
	//{{{ RolloverButton constructor
	/**
	 * Setup the border
	 */
	public RolloverToggleButton()
	{
		setBorderPainted(true);
		Color originalColor = UIManager.getColor("Button.darkShadow");
		Color rolloverColor = UIManager.getColor("Button.foreground");
		originalBorder = BorderFactory.createLineBorder(originalColor, 1);
		rolloverBorder = BorderFactory.createLineBorder(rolloverColor, 1);
		setBorder(originalBorder);
		setContentAreaFilled(false);
		addMouseListener(new MouseOverHandler());
	} //}}}

	//{{{ RolloverToggleButton constructor
	/**
	 * Setup the border (invisible initially)
	 *
	 * @param icon the icon of this button
	 */
	public RolloverToggleButton(Icon icon)
	{
		this();

		setIcon(icon);
	} //}}}

	//{{{ updateUI() method
	public void updateUI()
	{
		super.updateUI();
		setBorder(originalBorder);
		setRequestFocusEnabled(false);
	} //}}}

	//{{{ setEnabled() method
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		setBorderPainted(true);
		repaint();
	} //}}}

	//{{{ setBorderPainted() method
	public void setBorderPainted(boolean b)
	{
		try
		{
			revalidateBlocked = true;
			super.setBorderPainted(b);
			setContentAreaFilled(false);
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
			setBorder(rolloverBorder);
			setContentAreaFilled(false);
			setBorderPainted(true);
		}

		public void mouseExited(MouseEvent e)
		{
			setBorder(originalBorder);
			setContentAreaFilled(false);
			setBorderPainted(true);
		}
	} //}}}
	//}}}
}
