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

//{{{ Imports
import org.gjt.sp.jedit.View;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//}}}

/**
 * @author Matthieu Casanova
 */
abstract class AbstractLabelWidget implements Widget
{
	protected final JLabel label;
	protected final View view;

	protected AbstractLabelWidget(View view)
	{
		this.view = view;
		label = new ToolTipLabel();
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent evt)
			{
				if (evt.getClickCount() == 1)
					singleClick(evt);
				else if (evt.getClickCount() == 2)
					doubleClick(evt);
			}
		});
	}

	@Override
	public JComponent getComponent()
	{
		return label;
	}

	protected void singleClick(MouseEvent e)
	{
	}

	protected void doubleClick(MouseEvent e)
	{
	}
}
