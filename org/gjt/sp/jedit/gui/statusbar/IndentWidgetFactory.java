/*
 * IndentWidgetFactory.java - The indent widget service
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2011 Evan Wright
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

package org.gjt.sp.jedit.gui.statusbar;

//{{{ Imports
import java.awt.event.MouseEvent;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 * @author Evan Wright
 * @since jEdit 5.0
 */
public class IndentWidgetFactory implements StatusWidgetFactory
{
	@Override
	public Widget getWidget(View view)
	{
		Widget indent = new IndentWidget(view);
		return indent;
	}

	//{{{ IndentWidget class
	private static class IndentWidget extends AbstractLabelWidget
	{
		IndentWidget(View view)
		{
			super(view);
		}

		@Override
		protected void singleClick(MouseEvent e)
		{
			Buffer buffer = view.getBuffer();
			buffer.toggleAutoIndent(view);
			update();
		}

		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			String indent = buffer.getStringProperty("autoIndent");
			label.setToolTipText(jEdit.getProperty("view.status.indent-tooltip"));
			
			if ("full".equals(indent))
			{
				label.setEnabled(true);
				label.setText("F");
			}
			else if ("simple".equals(indent))
			{
				label.setEnabled(true);
				label.setText("S");
			}
			else
			{
				label.setEnabled(false);
				label.setText("n");
			}
			label.setText(indent);
		}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}
}
