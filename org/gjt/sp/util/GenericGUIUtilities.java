/*
 * GenericGUIUtilities.java - Various GUI utility functions
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
 * Copyright (C) 2016 Eric Le Lay (move from GUIUtilities.java)
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
package org.gjt.sp.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.textarea.TextAreaMouseHandler;

/** Various GUI utility functions not depending on jEdit, for use in StandaloneTextArea.
*
*
* @author Slava Pestov
* @author Eric Le Lay
* @version $Id$
*/
public class GenericGUIUtilities
{
	//{{{ prettifyMenuLabel() method
	/**
	 * `Prettifies' a menu item label by removing the `$' sign. This
	 * can be used to process the contents of an <i>action</i>.label
	 * property.
	 * @param label the label
	 * @return a pretty label
	 * @since jEdit 5.3.1
	 */
	public static String prettifyMenuLabel(String label)
	{
		int index = label.indexOf('$');
		if(index != -1)
		{
			label = label.substring(0,index)
				.concat(label.substring(index + 1));
		}
		return label;
	} //}}}

	//{{{ setAutoMnemonic() method
	/**
	 * Sets the mnemonic for the given button using jEdit convention,
	 * taking the letter after the dollar.
	 * @param button The button to set the mnemonic for.
	 * @since jEdit 5.3.1
	 */
	public static void setAutoMnemonic(AbstractButton button)
	{
		String label = button.getText();
		char mnemonic;
		int index = label.indexOf('$');
		if (index != -1 && label.length() - index > 1)
		{
			mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0, index).concat(label.substring(++index));
		}
		else
		{
			mnemonic = '\0';
		}
		if (mnemonic != '\0')
		{
			button.setMnemonic(mnemonic);
			button.setText(label);
		}
	} //}}}

	//{{{ adjustForScreenBounds() method
	/**
	 * Gives a rectangle the specified bounds, ensuring it is within the
	 * screen bounds.
	 * @since jEdit 5.3.1
	 */
	public static void adjustForScreenBounds(Rectangle desired)
	{
		// Make sure the window is displayed in visible region
		Rectangle osbounds = OperatingSystem.getScreenBounds(desired);

		if (desired.width > osbounds.width)
		{
			desired.width = osbounds.width;
		}
		if (desired.x < osbounds.x)
		{
			desired.x = osbounds.x;
		}
		if (desired.x + desired.width > osbounds.x + osbounds.width)
		{
			desired.x = osbounds.x + osbounds.width - desired.width;
		}
		if (desired.height > osbounds.height)
		{
			desired.height = osbounds.height;
		}
		if (desired.y < osbounds.y)
		{
			desired.y = osbounds.y;
		}
		if (desired.y + desired.height > osbounds.y + osbounds.height)
		{
			desired.y = osbounds.y + osbounds.height - desired.height;
		}
	} //}}}

	//{{{ requestFocus() method
	/**
	 * Focuses on the specified component as soon as the window becomes
	 * active.
	 * @param win The window
	 * @param comp The component
	 * @since jEdit 5.3.1
	 */
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowFocusListener(new WindowAdapter()
		{
			@Override
			public void windowGainedFocus(WindowEvent evt)
			{
				EventQueue.invokeLater(comp::requestFocusInWindow);
				win.removeWindowFocusListener(this);
			}
		});
	} //}}}

	//{{{ isPopupTrigger() method
	/**
	 * Returns if the specified event is the popup trigger event.
	 * This implements precisely defined behavior, as opposed to
	 * MouseEvent.isPopupTrigger().
	 * @param evt The event
	 * @since jEdit 5.3.1
	 */
	public static boolean isPopupTrigger(MouseEvent evt)
	{
		return TextAreaMouseHandler.isRightButton(evt);
	} //}}}

	//{{{ isLeftButton() method
	/**
	 * @param evt A mouse event
	 * @since jEdit 5.6
	 */
	public static boolean isLeftButton(MouseEvent evt)
	{
		return TextAreaMouseHandler.isLeftButton(evt);
	} //}}}

	//{{{ isMiddleButton() method
	/**
	 * @param modifiers The modifiers flag from a mouse event
	 * @since jEdit 5.3.1
	 * @deprecated use {@link #isMiddleButton(MouseEvent)}
	 */
	@Deprecated
	public static boolean isMiddleButton(int modifiers)
	{
		return TextAreaMouseHandler.isMiddleButton(modifiers);
	}

	/**
	 * @param evt A mouse event
	 * @since jEdit 5.6
	 */
	public static boolean isMiddleButton(MouseEvent evt)
	{
		return TextAreaMouseHandler.isMiddleButton(evt);
	} //}}}

	//{{{ isRightButton() method
	/**
	 * @param modifiers The modifiers flag from a mouse event
	 * @since jEdit 5.3.1
	 * @deprecated use {@link #isRightButton(MouseEvent)}
	 */
	@Deprecated
	public static boolean isRightButton(int modifiers)
	{
		return TextAreaMouseHandler.isRightButton(modifiers);
	}

	/**
	 * @param evt A mouse event
	 * @since jEdit 5.6
	 */
	public static boolean isRightButton(MouseEvent evt)
	{
		return TextAreaMouseHandler.isRightButton(evt);
	} //}}}

	//{{{ getScreenBounds() method
	/**
	 * Returns the screen bounds, taking into account multi-screen
	 * environments.
	 * @since jEdit 5.3.1
	 */
	public static Rectangle getScreenBounds()
	{
		Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().
			getMaximumWindowBounds();
		GraphicsDevice [] devices = GraphicsEnvironment.
			getLocalGraphicsEnvironment().getScreenDevices();
		if (devices.length > 1)
		{
			for (GraphicsDevice device: devices)
			{
				for (GraphicsConfiguration config: device.getConfigurations())
					bounds = bounds.union(config.getBounds());
			}
		}
		return bounds;
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @since jEdit 4.0pre1
	 * @see javax.swing.JComponent#setComponentPopupMenu(javax.swing.JPopupMenu) setComponentPopupMenu
	 * which works better and is simpler to use: you don't have to write the code to
	 * show/hide popups in response to mouse events anymore.
	 * @since jEdit 5.3.1
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y)
	{
		showPopupMenu(popup,comp,x,y,true);
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param point If true, then the popup originates from a single point;
	 * otherwise it will originate from the component itself. This affects
	 * positioning in the case where the popup does not fit onscreen.
	 *
	 * @since jEdit 5.3.1
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y, boolean point)
	{
		int offsetX = 0;
		int offsetY = 0;

		int extraOffset = point ? 1 : 0;

		Component win = comp;
		while(!(win instanceof Window || win == null))
		{
			offsetX += win.getX();
			offsetY += win.getY();
			win = win.getParent();
		}

		if(win != null)
		{
			Dimension size = popup.getPreferredSize();

			Rectangle screenSize = getScreenBounds();

			if(x + offsetX + size.width + win.getX() > screenSize.width
				&& x + offsetX + win.getX() >= size.width)
			{
				//System.err.println("x overflow");
				if(point)
					x -= size.width + extraOffset;
				else
					x = win.getWidth() - size.width - offsetX + extraOffset;
			}
			else
			{
				x += extraOffset;
			}

			//System.err.println("y=" + y + ",offsetY=" + offsetY
			//	+ ",size.height=" + size.height
			//	+ ",win.height=" + win.getHeight());
			if(y + offsetY + size.height + win.getY() > screenSize.height
				&& y + offsetY + win.getY() >= size.height)
			{
				if(point)
					y -= size.height + extraOffset;
				else
					y = win.getHeight() - size.height - offsetY + extraOffset;
			}
			else
			{
				y += extraOffset;
			}

			popup.show(comp,x,y);
		}
		else
			popup.show(comp,x + extraOffset,y + extraOffset);

	} //}}}

	//{{{ isAncestorOf() method
	/**
	 * Returns if the first component is an ancestor of the
	 * second by traversing up the component hierarchy.
	 *
	 * @param comp1 The ancestor
	 * @param comp2 The component to check
	 * @since jEdit 5.3.1
	 */
	public static boolean isAncestorOf(Component comp1, Component comp2)
	{
		while(comp2 != null)
		{
			if(comp1 == comp2)
				return true;
			else
				comp2 = comp2.getParent();
		}

		return false;
	} //}}}

	//{{{ getParentDialog() method
	/**
	 * Traverses the given component's parent tree looking for an
	 * instance of JDialog, and return it. If not found, return null.
	 * @param c The component
	 * @since jEdit 5.3.1
	 */
	public static JDialog getParentDialog(Component c)
	{
		return (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, c);
	} //}}}

	//{{{ setEnabledRecursively() method
	/**
	 * Call setEnabled() recursively on the container and its descendants.
	 * @param c The container
	 * @param enabled The enabled state to set
	 * @since jEdit 5.3.1
	 */
	public static void setEnabledRecursively(Container c, boolean enabled)
	{
		for (Component child: c.getComponents())
		{
			if (child instanceof Container)
				setEnabledRecursively((Container)child, enabled);
			else
				child.setEnabled(enabled);
		}
		c.setEnabled(enabled);
	} //}}}

	//{{{ setButtonContentMargin() method
	/**
	 * Sets the content margin of a button (for Nimbus L&amp;F).
	 * @param button  the button to modify
	 * @param margin  the new margin
	 * @since jEdit 5.3.1
	 */
	public static void setButtonContentMargin(AbstractButton button, Insets margin)
	{
		UIDefaults defaults = new UIDefaults();
		defaults.put("Button.contentMargins", margin);
		defaults.put("ToggleButton.contentMargins", margin);
		button.putClientProperty("Nimbus.Overrides", defaults);
	} //}}}

	//{{{
	/**
 	 * Makes components the same size by finding the largest width and height of the
 	 * given components then setting all components to that width and height. This is
 	 * especially useful for making JButtons the same size.
 	 * @param components The components to make the same size.
	 * @since jEdit 5.3.1
 	 */
	public static void makeSameSize(Component... components)
	{
		if (components == null)
			return;
		int width = 0;
		int height = 0;
		for (Component component : components)
		{
			if (component == null)
				continue;
			width = Math.max(width, component.getPreferredSize().width);
			height = Math.max(height, component.getPreferredSize().height);
		}
		Dimension d = new Dimension(width, height);
		for (Component component : components)
		{
			if (component == null)
				continue;
			component.setPreferredSize(d);
		}
	} //}}}

	//{{{ defaultTableDimension() method
	/**
	 * JTable cell size, based on global defaults.
	 * @since jEdit 5.3.1
	 */
	public static Dimension defaultTableCellSize()
	{
		JLabel label = new JLabel("miniminiminiminiminiminiminiminiminimini");
		UIDefaults defaults = UIManager.getDefaults();
		Object font = defaults.get("Table.font");
		if (font instanceof Font) label.setFont((Font)font);
		return label.getPreferredSize();
	} //}}}

	//{{{ defaultColumnWidth() method
	/**
	 * Column width for JTable, based on global defaults.
	 * @since jEdit 5.3.1
	 */
	public static int defaultColumnWidth()
	{
		return defaultTableCellSize().width;
	} //}}}

	//{{{ defaultRowHeight() method
	/**
	 * Row height for JTable, based on global defaults.
	 * @since jEdit 5.3.1
	 */
	public static int defaultRowHeight()
	{
		return defaultTableCellSize().height;
	} //}}}
}
