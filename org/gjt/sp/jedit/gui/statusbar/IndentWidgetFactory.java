/*
 * IndentWidgetFactory.java - The indent widget service
 * :tabSize=8:indentSize=8:noTabs=false:
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
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
	public Widget getWidget(View view)
	{
		Widget indent = new IndentWidget(view);
		return indent;
	}

	//{{{ IndentWidget class
	private static class IndentWidget implements Widget
	{
		private final JLabel indent;
		private final View view;
		public IndentWidget(final View view)
		{
			indent = new ToolTipLabel();
			indent.setHorizontalAlignment(SwingConstants.CENTER);

			this.view = view;
			indent.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent evt)
				{	
					Buffer buffer = view.getBuffer();
					buffer.toggleAutoIndent(view);
					update();
				}
			});
		}

		public JComponent getComponent()
		{
			return indent;
		}

		public void update()
		{
			Buffer buffer = view.getBuffer();
			String indent = buffer.getStringProperty("autoIndent");
			this.indent.setToolTipText(jEdit.getProperty("view.status.indent-tooltip"));
			
			if ("full".equals(indent))
			{
				this.indent.setEnabled(true);
				this.indent.setText("F");
			}
			else if ("simple".equals(indent))
			{
				this.indent.setEnabled(true);
				this.indent.setText("S");
			}
			else
			{
				this.indent.setEnabled(false);
				this.indent.setText("n");
			}
		}
		
		public void propertiesChanged()
		{
		}
	} //}}}

}
