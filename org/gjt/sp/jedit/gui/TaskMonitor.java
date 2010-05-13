/*
 * TaskMonitor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010 Matthieu Casanova
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

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;
import org.gjt.sp.util.ThreadUtilities;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * @author Matthieu Casanova
 */
public class TaskMonitor extends JPanel implements TaskListener
{
	private final TaskTableModel model;
	private final JTable table;
	private final JLabel remainingCount;

	public TaskMonitor()
	{
		super(new BorderLayout());
		remainingCount = new JLabel();
		add(remainingCount, BorderLayout.NORTH);

		model = new TaskTableModel();
		model.addTableModelListener(new TableModelListener()
		{
			public void tableChanged(TableModelEvent e)
			{
				if (e.getType() == TableModelEvent.INSERT ||
					e.getType() == TableModelEvent.DELETE)
				{
					updateTasksCount();
				}
			}
		});
		table = new JTable(model);
		table.setDefaultRenderer(Object.class, new TaskCellRenderer());
		table.getTableHeader().setVisible(false);
		table.setDefaultEditor(Object.class, new TaskTableEditor());
		table.getColumnModel().getColumn(1).setMaxWidth(16);
		table.getColumnModel().getColumn(1).setMinWidth(16);
		JScrollPane scroll = new JScrollPane(table);
		add(scroll);
		updateTasksCount();
	}

	@Override
	public void addNotify()
	{
		TaskManager.instance.visit(new TaskManager.TaskVisitor() 
		{
			public void visit(Task task)
			{
				model.addTask(task);
			}
		});
		TaskManager.instance.addTaskListener(this);
		super.addNotify();
	}

	@Override
	public void removeNotify()
	{
		TaskManager.instance.removeTaskListener(this);
		super.removeNotify();
		model.removeAll();
	}

	public void waiting(Task task)
	{
		model.addTask(task);
	}

	public void running(Task task)
	{
		repaint();
	}

	public void done(Task task)
	{
		model.removeTask(task);
	}

	public void statusUpdated(Task task)
	{
		repaint();
	}

	public void maximumUpdated(Task task)
	{
		repaint();
	}

	public void valueUpdated(Task task)
	{
		repaint();
	}

	private void updateTasksCount()
	{
		remainingCount.setText(jEdit.getProperty("taskmanager.remainingtasks.label",
						new Object[]{model.getRowCount()}));
	}

	private static class TaskCellRenderer implements TableCellRenderer
	{
		private final JProgressBar progress;
		private final JButton button;
		private TaskCellRenderer()
		{
			progress = new JProgressBar();
			button = new JButton(GUIUtilities.loadIcon(jEdit.getProperty("close-buffer.icon")));
			progress.setStringPainted(true);
		}

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

			return button;
		}
	}

	private class TaskTableEditor extends AbstractCellEditor implements TableCellEditor
	{
		private final JButton button;

		private Task task;

		private TaskTableEditor()
		{
			button = new JButton(GUIUtilities.loadIcon(jEdit.getProperty("close-buffer.icon")));
			button.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					task.cancel();
					stopCellEditing();
				}
			});
		}

		public Object getCellEditorValue()
		{
			return null;
		}

		//{{{ getTableCellEditorComponent() method
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			task = (Task) value;
			return button;
		} //}}}
	}

	private static class TaskTableModel extends AbstractTableModel
	{
		private final java.util.List<Task> tasks;

		private TaskTableModel()
		{
			tasks = new ArrayList<Task>();
		}

		public int getRowCount()
		{
			return tasks.size();
		}

		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 1;
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			return tasks.get(rowIndex);
		}

		void addTask(final Task task)
		{
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					tasks.add(task);
					fireTableRowsInserted(tasks.size()-1, tasks.size()-1);
				}
			});
		}

		void removeTask(final Task task)
		{
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					int index = tasks.indexOf(task);
					if (index != -1)
					{
						tasks.remove(index);
						fireTableRowsDeleted(index,index);
					}
				}
			});
		}

		public void removeAll()
		{
			tasks.clear();
			fireTableDataChanged();
		}
	}
}
