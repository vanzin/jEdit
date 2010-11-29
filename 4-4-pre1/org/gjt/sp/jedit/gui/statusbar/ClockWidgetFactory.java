/*
 * ClockWidgetFactory.java - The clock widget service
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
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
public class ClockWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view) 
	{
		Widget clock = new ClockWidget();
		return clock;
	} //}}}

	//{{{ ClockWidget class
	private static class ClockWidget implements Widget
	{
		private final Clock clock;
		ClockWidget()
		{
			clock = new Clock();
		}
		
		public JComponent getComponent() 
		{
			return clock;
		}
		
		public void update() 
		{
		}
		
		public void propertiesChanged()
		{
		}
	} //}}}

	//{{{ Clock class
	private static class Clock extends JLabel implements ActionListener
	{
		//{{{ Clock constructor
		Clock()
		{
			setForeground(jEdit.getColorProperty("view.status.foreground"));
			setBackground(jEdit.getColorProperty("view.status.background"));
		} //}}}

		//{{{ addNotify() method
		@Override
		public void addNotify()
		{
			super.addNotify();
			update();

			int millisecondsPerMinute = 1000 * 60;

			timer = new Timer(millisecondsPerMinute,this);
			timer.setInitialDelay((int)(
				millisecondsPerMinute
				- System.currentTimeMillis()
				% millisecondsPerMinute) + 500);
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
			return new Date().toString();
		} //}}}

		//{{{ getToolTipLocation() method
		@Override
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent e)
		{
			update();
		} //}}}

		//{{{ Private members
		private Timer timer;

		//{{{ getTime() method
		private static String getTime()
		{
			return DateFormat.getTimeInstance(
				DateFormat.SHORT).format(new Date());
		} //}}}

		//{{{ update() method
		private void update()
		{
			setText(getTime());
		} //}}}

		//}}}
	} //}}}
}
