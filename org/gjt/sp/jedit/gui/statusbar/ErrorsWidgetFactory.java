/*
 * ErrorsWidgetFactory.java - The error widget service
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008-2011 Matthieu Casanova
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
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
//}}}

/**
 * This widget will show you in the status bar the last errors reported in jEdit.
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class ErrorsWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		Widget errorWidget = new ErrorWidget(view);
		return errorWidget;
	} //}}}

	//{{{ ErrorWidget class
	private static class ErrorWidget implements Widget
	{
		private final ErrorHighlight errorHighlight;

		ErrorWidget(View view)
		{
			errorHighlight = new ErrorHighlight(view);
		}

		@Override
		public JComponent getComponent()
		{
			return errorHighlight;
		}

		@Override
		public void update()
		{
			errorHighlight.update();
		}

		@Override
		public void propertiesChanged()
		{
		}
	} //}}}

	//{{{ ErrorHighlight class
	private static class ErrorHighlight extends JLabel implements ActionListener
	{
		private int currentSize;
		private final Color foregroundColor;

		//{{{ ErrorHighlight constructor
		ErrorHighlight(View view)
		{
			String defaultFont = jEdit.getProperty("view.font");
			int defaultFontSize = jEdit.getIntegerProperty("view.fontsize", 12);
			SyntaxStyle invalid = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"),
				defaultFont,defaultFontSize);
			foregroundColor = invalid.getForegroundColor();
			setForeground(foregroundColor);
			setBackground(jEdit.getColorProperty("view.status.background"));
			addMouseListener(new MyMouseAdapter(view));
		} //}}}

		//{{{ addNotify() method
		@Override
		public void addNotify()
		{
			super.addNotify();
			update();
			int millisecondsPerMinute = 1000;

			timer = new Timer(millisecondsPerMinute, this);
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

		//{{{ getToolTipLocation() method
		@Override
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(), -20);
		} //}}}

		//{{{ actionPerformed() method
		@Override
		public void actionPerformed(ActionEvent e)
		{
			update();
		} //}}}

		private Timer timer;

		//{{{ update() method
		private void update()
		{
			int size = Log.throwables.size();
			if (size != currentSize)
			{
				currentSize = size;
				if (size == 0)
				{
					setText(null);
					setToolTipText(size + " error");
				}
				else
				{
					setForeground(foregroundColor);
					setText(Integer.toString(size) + " error(s)");
					setToolTipText(size + " error(s)");
				}
			}
		} //}}}

		//{{{ MyMouseAdapter class
		private class MyMouseAdapter extends MouseAdapter
		{
			private final View view;

			MyMouseAdapter(View view)
			{
				this.view = view;
			}

			@Override
				public void mouseClicked(MouseEvent e)
			{
				if (Log.throwables.isEmpty())
					return;
				if (GUIUtilities.isRightButton(e.getModifiers()))
				{
					JPopupMenu menu = GUIUtilities.loadPopupMenu("errorwidget.popupmenu");
					GUIUtilities.showPopupMenu(menu, ErrorHighlight.this, e.getX(), e.getY());

				}
				else if (e.getClickCount() == 2)
					new ErrorDialog(view);

			}
		} //}}}

	} //}}}

	//{{{ ErrorDialog class
	private static class ErrorDialog extends EnhancedDialog
	{
		private final JTextArea textArea;
		private final ByteArrayOutputStream byteArrayOutputStream;
		private final PrintStream printStream;
		private final JButton removeThisError;
		private final JButton removeAllErrors;
		private final Object[] throwables;
		private final JComboBox combo;

		//{{{ ErrorDialog constructor
		private ErrorDialog(Frame view)
		{
			super(view, "Errors", false);
			byteArrayOutputStream = new ByteArrayOutputStream();
			printStream = new PrintStream(byteArrayOutputStream);
			throwables = Log.throwables.toArray();
			textArea = new JTextArea();
			textArea.setEditable(false);
			if (throwables.length != 0)
			{
				Throwable throwable = (Throwable) throwables[0];
				setThrowable(throwable);
			}
			combo = new JComboBox(throwables);
			combo.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					setThrowable((Throwable) combo.getSelectedItem());
				}
			});
			getContentPane().add(combo, BorderLayout.NORTH);
			getContentPane().add(new JScrollPane(textArea));



			Box buttons = new Box(BoxLayout.X_AXIS);
			buttons.add(Box.createGlue());

			buttons.add(removeThisError = new JButton(jEdit.getProperty("grab-key.remove")));
			buttons.add(Box.createHorizontalStrut(6));
			buttons.add(removeAllErrors = new JButton(jEdit.getProperty("common.clearAll")));

			ErrorDialog.MyActionListener actionListener = new MyActionListener();
			removeThisError.addActionListener(actionListener);
			removeAllErrors.addActionListener(actionListener);
			buttons.add(Box.createGlue());


			getContentPane().add(buttons, BorderLayout.SOUTH);
			pack();
			GUIUtilities.loadGeometry(this,"status.errorWidget");
			setVisible(true);
		} //}}}

		//{{{ setThrowable() method
		private void setThrowable(Throwable throwable)
		{
			if (throwable == null)
			{
				textArea.setText(null);
			}
			else
			{
				throwable.printStackTrace(printStream);
				textArea.setText(byteArrayOutputStream.toString());
				textArea.setCaretPosition(0);
				byteArrayOutputStream.reset();
			}
		} //}}}

		//{{{ dispose() method
		@Override
		public void dispose()
		{
			GUIUtilities.saveGeometry(this, "status.errorWidget");
			super.dispose();
		} //}}}

		//{{{ ok() method
		@Override
		public void ok()
		{
			dispose();
		} //}}}

		//{{{ cancel() method
		@Override
		public void cancel()
		{
			dispose();
		} //}}}

		//{{{ MyActionListener class
		private class MyActionListener implements ActionListener
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source == removeThisError)
				{
					Throwable throwable = (Throwable) combo.getSelectedItem();
					if (throwable != null)
					{
						Log.throwables.remove(throwable);
						combo.removeItem(throwable);
						if (combo.getItemCount() == 0)
						{
							dispose();
						}
					}
				}
				else if (source == removeAllErrors)
				{
					for (Object throwable : throwables)
					{
						Log.throwables.remove(throwable);
					}
					dispose();
				}
			}
		} //}}}
	} //}}}
}
