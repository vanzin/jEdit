/*
 * MarkersProvider.java - Markers menu
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
import java.util.List;

import org.gjt.sp.jedit.*;
//}}}

public class MarkersProvider implements DynamicMenuProvider
{
	//{{{ updateEveryTime() method
	@Override
	public boolean updateEveryTime()
	{
		return true;
	} //}}}

	//{{{ update() method
	@Override
	public void update(JMenu menu)
	{
		final View view = GUIUtilities.getView(menu);
		Buffer buffer = view.getBuffer();

		List<Marker> markers = buffer.getMarkers();

		if(markers.isEmpty())
		{
			JMenuItem mi = new JMenuItem(jEdit.getProperty(
				"no-markers.label"));
			mi.setEnabled(false);
			menu.add(mi);
			return;
		}

		int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

		JMenu current = menu;

		for(int i = 0; i < markers.size(); i++)
		{
			final Marker marker = markers.get(i);
			int lineNo = buffer.getLineOfOffset(marker.getPosition());

			if(current.getItemCount() >= maxItems && i != markers.size() - 1)
			{
				//current.addSeparator();
				JMenu newCurrent = new JMenu(
					jEdit.getProperty(
					"common.more"));
				current.add(newCurrent);
				current = newCurrent;
			}

			JMenuItem mi = new MarkersMenuItem(buffer, lineNo, marker.getShortcut());
			mi.addActionListener(evt -> view.getTextArea().setCaretPosition(marker.getPosition()));
			current.add(mi);
		}
	} //}}}

	//{{{ MarkersMenuItem class
	private static class MarkersMenuItem extends JMenuItem
	{
		//{{{ MarkersMenuItem constructor
		MarkersMenuItem(Buffer buffer, int lineNo, char shortcut)
		{
			String text = buffer.getLineText(lineNo).trim();
			if(text.isEmpty())
				text = jEdit.getProperty("markers.blank-line");
			setText((lineNo + 1) + ": " + text);

			shortcutProp = "goto-marker.shortcut";
			this.shortcut = shortcut;
		} //}}}

		//{{{ getPreferredSize() method
		@Override
		public Dimension getPreferredSize()
		{
			Dimension d = super.getPreferredSize();

			String shortcut = getShortcut();

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

			String shortcut = getShortcut();

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

		//{{{ Private members
		private final String shortcutProp;
		private final char shortcut;
		private static final Font acceleratorFont;
		private static final Color acceleratorForeground;
		private static final Color acceleratorSelectionForeground;

		//{{{ getShortcut() method
		private String getShortcut()
		{
			if(shortcut == '\0')
				return null;
			else
			{
				String shortcutPrefix = jEdit.getProperty(shortcutProp);

				if(shortcutPrefix == null)
					return null;
				else
				{
					return shortcutPrefix + ' ' + shortcut;
				}
			}
		} //}}}

		//{{{ Class initializer
		static
		{
			acceleratorFont = GUIUtilities.menuAcceleratorFont();
			acceleratorForeground = UIManager
				.getColor("MenuItem.acceleratorForeground");
			acceleratorSelectionForeground = UIManager
				.getColor("MenuItem.acceleratorSelectionForeground");
		} //}}}

		//}}}
	} //}}}
}
