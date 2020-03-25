/*
 * TaskMonitor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010-2012 Matthieu Casanova
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;
import org.gjt.sp.util.ThreadUtilities;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
//}}}

/** Task Monitor dockable panel, for showing progress on active tasks.
 * @author Matthieu Casanova
 * @since jEdit 4.4
 */
public class TaskMonitor extends JPanel implements TaskListener
{
	private final TaskTableModel model;
	private final JLabel remainingCount;

	//{{{ TaskMonitor constructor
	public TaskMonitor()
	{
		super(new BorderLayout());
		JPanel panel = new JPanel(new BorderLayout());
		remainingCount = new JLabel();
		panel.add(remainingCount, BorderLayout.NORTH);

		model = new TaskTableModel();
		model.addTableModelListener(e ->
		{
			if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) updateTasksCount();
		});
		JTable table = new JTable(model);
		table.setRowHeight(GenericGUIUtilities.defaultRowHeight());
		table.setDefaultRenderer(Object.class, new TaskCellRenderer());
		table.getTableHeader().setVisible(false);
		table.setDefaultEditor(Object.class, new TaskTableEditor());
		table.getColumnModel().getColumn(1).setMaxWidth(16);
		table.getColumnModel().getColumn(1).setMinWidth(16);
		JScrollPane scroll = new JScrollPane(table);
		panel.add(scroll);
		updateTasksCount();

		add(panel);
	} //}}}

	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		TaskManager.instance.visit(model::addTask);
		TaskManager.instance.addTaskListener(this);
		super.addNotify();
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		TaskManager.instance.removeTaskListener(this);
		super.removeNotify();
		model.removeAll();
	} //}}}

	//{{{ waiting() method
	@Override
	public void waiting(Task task)
	{
		model.addTask(task);
	} //}}}

	//{{{ running() method
	@Override
	public void running(Task task)
	{
		repaint();
	} //}}}

	//{{{ done() method
	@Override
	public void done(Task task)
	{
		model.removeTask(task);
	} //}}}

	//{{{ statusUpdated() method
	@Override
	public void statusUpdated(Task task)
	{
		repaint();
	} //}}}

	//{{{ maximumUpdated() method
	@Override
	public void maximumUpdated(Task task)
	{
		repaint();
	} //}}}

	//{{{ valueUpdated() method
	@Override
	public void valueUpdated(Task task)
	{
		repaint();
	} //}}}

	//{{{ updateTasksCount() method
	private void updateTasksCount()
	{
		remainingCount.setText(jEdit.getProperty("taskmanager.remainingtasks.label",
						new Object[]{model.getRowCount()}));
	} //}}}

	//{{{ TaskCellRenderer class
	private static class TaskCellRenderer implements TableCellRenderer
	{
		private final JProgressBar progress;
		private final JButton button;

		//{{{ TaskCellRenderer constructor
		private TaskCellRenderer()
		{
			progress = new JProgressBar();
			button = new JButton(GUIUtilities.loadIcon(jEdit.getProperty("close-buffer.icon")));
			progress.setStringPainted(true);
		} //}}}

		//{{{ getTableCellRendererComponent
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
							       boolean hasFocus, int row, int column)
		{
			Task task = (Task) value;
			if (column == 0)
			{
				if (task.getMaximum() == 0L)
				{
					progress.setIndeterminate(true);
				}
				else
				{
					progress.setIndeterminate(false);
					long max = task.getMaximum();
					long val = task.getValue();
					if (max > Integer.MAX_VALUE)
					{
						max >>= 10L;
						val >>= 10L;

					}
					progress.setMaximum((int) max);
					progress.setValue((int) val);
				}
				progress.setToolTipText(task.getLabel());
				progress.setString(task.getStatus());
				return progress;
			}
			button.setEnabled(task.isCancellable());
			return button;
		} //}}}
	} //}}}

	//{{{ TaskTableEditor class
	private static class TaskTableEditor extends AbstractCellEditor implements TableCellEditor
	{
		private final JButton button;

		private Task task;

		//{{{ TaskTableEditor constructor
		private TaskTableEditor()
		{
			button = new JButton(GUIUtilities.loadIcon(jEdit.getProperty("close-buffer.icon")));
			button.addActionListener(e ->
			{
				task.cancel();
				stopCellEditing();
			});
		} //}}}

		//{{{ getCellEditorValue() method
		@Override
		public Object getCellEditorValue()
		{
			return null;
		} //}}}

		//{{{ getTableCellEditorComponent() method
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			task = (Task) value;
			return button;
		} //}}}
	} //}}}

	//{{{ TaskTableModel class
	private static class TaskTableModel extends AbstractTableModel
	{
		private final java.util.List<Task> tasks;

		//{{{ TaskTableModel constructor
		private TaskTableModel()
		{
			tasks = new ArrayList<Task>();
		} //}}}

		//{{{ getRowCount() method
		@Override
		public int getRowCount()
		{
			return tasks.size();
		} //}}}

		//{{{ getColumnCount() method
		@Override
		public int getColumnCount()
		{
			return 2;
		} //}}}

		//{{{ isCellEditable() method
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 1;
		} //}}}

		//{{{ getValueAt() method
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			return tasks.get(rowIndex);
		} //}}}

		//{{{ addTask() method
		void addTask(Task task)
		{
			ThreadUtilities.runInDispatchThread(() ->
			{
				tasks.add(task);
				fireTableRowsInserted(tasks.size()-1, tasks.size()-1);
			});
		} //}}}

		//{{{ removeTask() method
		void removeTask(Task task)
		{
			ThreadUtilities.runInDispatchThread(() ->
			{
				int index = tasks.indexOf(task);
				if (index != -1)
				{
					tasks.remove(index);
					fireTableRowsDeleted(index,index);
				}
			});
		} //}}}

		//{{{ removeAll() method
		public void removeAll()
		{
			tasks.clear();
			fireTableDataChanged();
		} //}}}
	} //}}}
}
