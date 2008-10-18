/*
 * MemoryStatusWidgetFactory.java - The memory status widget service
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
 * Portions Copyright (C) 2001, 2004 Slava Pestov
 * Portions copyright (C) 2001 Mike Dillon
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14 
 */
public class MemoryStatusWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() constructor
	public Widget getWidget(View view) 
	{
		Widget memory = new MemoryStatusWidget(view);
		return memory;
	} //}}}
	
	//{{{ MemoryStatusWidget class
	private static class MemoryStatusWidget implements Widget
	{
		private final MemoryStatus memoryStatus;
		public MemoryStatusWidget(View view) 
		{
			memoryStatus = new MemoryStatus(view);
		}
		
		public JComponent getComponent() 
		{
			return memoryStatus;
		}
		
		public void update() 
		{
		}
		
		public void propertiesChanged()
		{
		}
	} //}}}

	//{{{ MemoryStatus class
	private static class MemoryStatus extends JComponent implements ActionListener
	{
		private View view;
		//{{{ MemoryStatus constructor
		MemoryStatus(View view)
		{
			this.view = view;
			// fucking GTK look and feel
			Font font = new JLabel().getFont();
			//Font font = UIManager.getFont("Label.font");
			MemoryStatus.this.setFont(font);

			FontRenderContext frc = new FontRenderContext(
				null,false,false);
			Rectangle2D bounds = font.getStringBounds(
				memoryTestStr,frc);
			Dimension dim = new Dimension((int)bounds.getWidth(),
				(int)bounds.getHeight());
			setPreferredSize(dim);
			setMaximumSize(dim);
			lm = font.getLineMetrics(memoryTestStr,frc);

			setForeground(jEdit.getColorProperty("view.status.foreground"));
			setBackground(jEdit.getColorProperty("view.status.background"));

			progressForeground = jEdit.getColorProperty(
				"view.status.memory.foreground");
			progressBackground = jEdit.getColorProperty(
				"view.status.memory.background");

			addMouseListener(new MouseHandler());
		} //}}}

		//{{{ addNotify() method
		@Override
		public void addNotify()
		{
			super.addNotify();
			timer = new Timer(2000,this);
			timer.start();
			ToolTipManager.sharedInstance().registerComponent(this);
		} //}}}

		//{{{ removeNotify() method
		@Override
		public void removeNotify()
		{
			timer.stop();
			ToolTipManager.sharedInstance().unregisterComponent(this);
			super.removeNotify();
		} //}}}

		//{{{ getToolTipText() method
		@Override
		public String getToolTipText()
		{
			Runtime runtime = Runtime.getRuntime();
			long free = runtime.freeMemory();
			long total = runtime.totalMemory();
			long used = total - free;
			args[0] = (int) (used / 1024);
			args[1] = (int) (total / 1024);
			return jEdit.getProperty("view.status.memory-tooltip",args);
		} //}}}

		//{{{ getToolTipLocation() method
		@Override
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			MemoryStatus.this.repaint();
		} //}}}

		//{{{ paintComponent() method
		@Override
		public void paintComponent(Graphics g)
		{
			Insets insets = new Insets(0,0,0,0);//MemoryStatus.this.getBorder().getBorderInsets(this);

			Runtime runtime = Runtime.getRuntime();
			long free = runtime.freeMemory();
			long total = runtime.totalMemory();
			long used = total - free;

			int width = MemoryStatus.this.getWidth()
				- insets.left - insets.right;
			int height = MemoryStatus.this.getHeight()
				- insets.top - insets.bottom - 1;

			float fraction = ((float)used) / total;

			g.setColor(progressBackground);

			g.fillRect(insets.left,insets.top,
				(int)(width * fraction),
				height);

			String str = (used / 1024 / 1024) + "/"
				+ (total / 1024 / 1024) + "Mb";

			FontRenderContext frc = new FontRenderContext(null,false,false);

			Rectangle2D bounds = g.getFont().getStringBounds(str,frc);

			Graphics g2 = g.create();
			g2.setClip(insets.left,insets.top,
				(int)(width * fraction),
				height);

			g2.setColor(progressForeground);

			g2.drawString(str,
				insets.left + ((int) (width - bounds.getWidth()) / 2),
				(int)(insets.top + lm.getAscent()));

			g2.dispose();

			g2 = g.create();

			g2.setClip(insets.left + (int)(width * fraction),
				insets.top,MemoryStatus.this.getWidth()
				- insets.left - (int)(width * fraction),
				height);

			g2.setColor(MemoryStatus.this.getForeground());

			g2.drawString(str,
				insets.left + ((int) (width - bounds.getWidth()) >> 1),
				(int)(insets.top + lm.getAscent()));

			g2.dispose();
		} //}}}

		//{{{ Private members
		private static final String memoryTestStr = "999/999Mb";

		private final LineMetrics lm;
		private final Color progressForeground;
		private final Color progressBackground;

		private final Integer[] args = new Integer[2];


		private Timer timer;
		//}}}

		//{{{ MouseHandler class
		class MouseHandler extends MouseAdapter
		{
			@Override
			public void mousePressed(MouseEvent evt)
			{
				if(evt.getClickCount() == 2)
				{
					jEdit.showMemoryDialog(view);
					repaint();
				}
			}
		} //}}}
	} //}}}
}
