/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//}}}

/**
 * A Statusbar widget that monitor the task manager.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class TaskMonitorWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
    public Widget getWidget(View view)
	{
		Widget widget = new TaskMonitorWidget(view);
		widget.getComponent().setToolTipText(jEdit.getProperty("statusbar.task-monitor.tooltip"));
		return widget;
	} //}}}

    private static class TaskMonitorWidget extends JLabel implements Widget, TaskListener
    {
        private TaskMonitorWidget(final View view)
        {
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isLeftMouseButton(e))
                    {
                        view.getDockableWindowManager().showDockableWindow("task-monitor");
                    }
                }
            });
        }

        @Override
        public void addNotify()
        {
            super.addNotify();
            TaskManager.instance.addTaskListener(this);
            update();
        }

        @Override
        public void removeNotify()
        {
            super.removeNotify();
            TaskManager.instance.removeTaskListener(this);
        }

        @Override
        public JComponent getComponent()
        {
            return this;
        }

        @Override
        public void propertiesChanged()
        {
        }

        @Override
        public void update()
        {
            int count = TaskManager.instance.countTasks();
            if (count == 0)
            {
                setText(null);
            }
            else
            {
                setText(jEdit.getProperty("statusbar.task-monitor.template", new Object[]{Integer.toString(count)}));
            }
        }

        @Override
        public void waiting(Task task)
        {
            update();
        }

        @Override
        public void running(Task task)
        {
            update();
        }

        @Override
        public void done(Task task)
        {
            update();
        }

        @Override
        public void statusUpdated(Task task)
        {
            update();
        }

        @Override
        public void maximumUpdated(Task task)
        {
            update();
        }

        @Override
        public void valueUpdated(Task task)
        {
            update();
        }
	} //}}}
}