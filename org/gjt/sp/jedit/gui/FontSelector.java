/*
 * FontSelector.java - Font selector
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 * Portions copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2003 mike dillon
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
import java.awt.event.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

//{{{ FontSelector class
/**
 * A font chooser widget.
 * @author Slava Pestov
 * @version $Id$
 */
public class FontSelector extends JButton
{
	//{{{ FontSelector constructor
	/**
	 * Creates a new font selector control.
	 * @param font The font
	 */
	public FontSelector(Font font)
	{
		this(font,false);
	} //}}}

	//{{{ FontSelector constructor
	/**
	 * Creates a new font selector control.
	 * @param font The font
	 * @param antiAlias Is anti-aliasing enabled?
	 * @since jEdit 4.2pre7
	 */
	public FontSelector(Font font, boolean antiAlias)
	{
		setFont(font);
		this.antiAlias = antiAlias;

		updateText();

		setRequestFocusEnabled(false);

		addActionListener(new ActionHandler());
	} //}}}

	//{{{ paintComponent() method
	public void paintComponent(Graphics g)
	{
		setAntiAliasEnabled(g);
		super.paintComponent(g);
	} //}}}

	//{{{ isAntiAliasEnabled() method
	public boolean isAntiAliasEnabled()
	{
		return antiAlias;
	} //}}}

	//{{{ setAntiAliasEnabled() method
	public void setAntiAliasEnabled(boolean antiAlias)
	{
		this.antiAlias = antiAlias;
	} //}}}

	//{{{ updateText() method
	private void updateText()
	{
		Font font = getFont();
		String styleString;
		switch(font.getStyle())
		{
		case Font.PLAIN:
			styleString = jEdit.getProperty("font-selector.plain");
			break;
		case Font.BOLD:
			styleString = jEdit.getProperty("font-selector.bold");
			break;
		case Font.ITALIC:
			styleString = jEdit.getProperty("font-selector.italic");
			break;
		case Font.BOLD | Font.ITALIC:
			styleString = jEdit.getProperty("font-selector.bolditalic");
			break;
		default:
			styleString = "UNKNOWN!!!???";
			break;
		}

		setText(font.getName() + ' ' + font.getSize() + ' ' + styleString);
	} //}}}

	//{{{ setAntiAliasEnabled() method
	void setAntiAliasEnabled(Graphics g)
	{
		if (antiAlias)
		{
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		}
	} //}}}

	private boolean antiAlias;

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Font font;

			JDialog dialog = GUIUtilities.getParentDialog(FontSelector.this);
			if(dialog == null)
			{
				font = new FontSelectorDialog(
					JOptionPane.getFrameForComponent(
					FontSelector.this),getFont(),
					FontSelector.this)
					.getSelectedFont();
			}
			else
			{
				font = new FontSelectorDialog(dialog,getFont(),
					FontSelector.this)
					.getSelectedFont();
			}

			if(font != null)
			{
				setFont(font);
				updateText();
			}
		}
	} //}}}
} //}}}

