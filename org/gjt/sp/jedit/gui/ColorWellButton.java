/*
 * ColorWellButton.java - Shows color chooser when clicked
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.OperatingSystem;
//}}}

/**
 * A button that, when clicked, shows a color chooser.
 *
 * You can get and set the currently selected color using
 * {@link #getSelectedColor()} and {@link #setSelectedColor(Color)}.
 * @author Slava Pestov
 * @version $Id$
 */
public class ColorWellButton extends JButton
{
	//{{{ ColorWellButton constructor
	public ColorWellButton(Color color)
	{
		setIcon(new ColorWell(color));
		setMargin(new Insets(2,2,2,2));
		addActionListener(new ActionHandler());

		// according to krisk this looks better on OS X...
		if(OperatingSystem.isMacOSLF())
			putClientProperty("JButton.buttonType","toolbar");
	} //}}}

	//{{{ getSelectedColor() method
	public Color getSelectedColor()
	{
		return ((ColorWell)getIcon()).color;
	} //}}}

	//{{{ setSelectedColor() method
	public void setSelectedColor(Color color)
	{
		((ColorWell)getIcon()).color = color;
		repaint();
	} //}}}

	//{{{ ColorWell class
	static class ColorWell implements Icon
	{
		Color color;

		ColorWell(Color color)
		{
			this.color = color;
		}

		public int getIconWidth()
		{
			return 35;
		}

		public int getIconHeight()
		{
			return 10;
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			if(color == null)
				return;

			g.setColor(color);
			g.fillRect(x,y,getIconWidth(),getIconHeight());
			g.setColor(color.darker());
			g.drawRect(x,y,getIconWidth()-1,getIconHeight()-1);
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JDialog parent = GUIUtilities.getParentDialog(ColorWellButton.this);
			JDialog dialog;
			if (parent != null)
			{
				dialog = new ColorPickerDialog(parent,
					jEdit.getProperty("colorChooser.title"),
					true);
			}
			else
			{
				dialog = new ColorPickerDialog(
					JOptionPane.getFrameForComponent(
					ColorWellButton.this),
					jEdit.getProperty("colorChooser.title"),
					true);
			}
			dialog.pack();
			dialog.setVisible(true);
		}
	} //}}}

	//{{{ ColorPickerDialog class
	/**
	 * Replacement for the color picker dialog provided with Swing. This version
	 * supports dialog as well as frame parents.
	 * @since jEdit 4.1pre7
	 */
	private class ColorPickerDialog extends EnhancedDialog implements ActionListener
	{
		public ColorPickerDialog(Frame parent, String title, boolean modal)
		{
			super(parent,title,modal);

			init();
		}

		public ColorPickerDialog(Dialog parent, String title, boolean modal)
		{
			super(parent,title,modal);

			getContentPane().setLayout(new BorderLayout());

			init();
		}

		public void ok()
		{
			Color c = chooser.getColor();
			if (c != null)
				setSelectedColor(c);
			setVisible(false);
		}

		public void cancel()
		{
			setVisible(false);
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (evt.getSource() == ok)
				ok();
			else
				cancel();
		}

		//{{{ Private members
		private JColorChooser chooser;
		private JButton ok;
		private JButton cancel;

		private void init()
		{
			Color c = getSelectedColor();
			if(c == null)
				chooser = new JColorChooser();
			else
				chooser = new JColorChooser(c);

			getContentPane().add(BorderLayout.CENTER, chooser);

			Box buttons = new Box(BoxLayout.X_AXIS);
			buttons.add(Box.createGlue());

			ok = new JButton(jEdit.getProperty("common.ok"));
			ok.addActionListener(this);
			buttons.add(ok);
			buttons.add(Box.createHorizontalStrut(6));
			getRootPane().setDefaultButton(ok);
			cancel = new JButton(jEdit.getProperty("common.cancel"));
			cancel.addActionListener(this);
			buttons.add(cancel);
			buttons.add(Box.createGlue());

			getContentPane().add(BorderLayout.SOUTH, buttons);
			pack();
			setLocationRelativeTo(getParent());
		} //}}}
	} //}}}
}
