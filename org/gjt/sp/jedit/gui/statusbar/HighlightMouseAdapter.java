/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2021 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui.statusbar;

import org.gjt.sp.jedit.jEdit;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A simple mouse adapter that will highlight the status bar with the defined background
 * @author Matthieu Casanova
 * @since jEdit 5.7pre1
 */
public class HighlightMouseAdapter extends MouseAdapter
{
	private final JComponent component;

	public HighlightMouseAdapter(JComponent component)
	{
		this.component = component;
		component.setOpaque(true);
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		component.setBackground(jEdit.getColorProperty("view.status.highlight"));
		component.repaint();
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		component.setBackground(null);
		component.repaint();
	}
}
