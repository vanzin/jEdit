/*
 * ErrorsWidgetFactory.java - The error widget service
 * :tabSize=4:indentSize=4:noTabs=false:
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
import org.gjt.sp.jedit.JEditActionSet;
import org.gjt.sp.jedit.JEditBeanShellAction;
import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.textarea.JEditEmbeddedTextArea;
import org.gjt.sp.jedit.textarea.StandaloneTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.SyntaxUtilities;
import org.jedit.keymap.Keymap;

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
			SyntaxStyle invalid = SyntaxUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"),
				defaultFont,defaultFontSize, true);
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
				if (GenericGUIUtilities.isRightButton(e))
				{
					JPopupMenu menu = GUIUtilities.loadPopupMenu("errorwidget.popupmenu");
					GenericGUIUtilities.showPopupMenu(menu, ErrorHighlight.this, e.getX(), e.getY());

				}
				else if (e.getClickCount() == 2)
					new ErrorDialog(view);

			}
		} //}}}

	} //}}}

	//{{{ ErrorDialog class
	private static class ErrorDialog extends EnhancedDialog
	{
		private final TextArea textArea;
		private final ByteArrayOutputStream byteArrayOutputStream;
		private final PrintStream printStream;
		private final JButton removeThisError;
		private final JButton removeAllErrors;
		private final Throwable[] throwables;
		private final JComboBox<Throwable> combo;

		//{{{ ErrorDialog constructor
		private ErrorDialog(Frame view)
		{
			super(view, "Errors", false);
			byteArrayOutputStream = new ByteArrayOutputStream();
			printStream = new PrintStream(byteArrayOutputStream);
			throwables = Log.throwables.toArray(new Throwable[0]);
			textArea = new JEditEmbeddedTextArea();
			JEditActionSet<JEditBeanShellAction> actionSet = new StandaloneTextArea.StandaloneActionSet(jEdit.getPropertyManager(),
																										textArea,
																										TextArea.class.getResource("textarea.actions.xml"));
			textArea.addActionSet(actionSet);
			actionSet.load();
			actionSet.initKeyBindings();
			Keymap keymap = jEdit.getKeymapManager().getKeymap();
			String shortcut = keymap.getShortcut("copy.shortcut");
			if (shortcut != null)
				textArea.getInputHandler().addKeyBinding(shortcut, "copy");
			String shortcut2 = keymap.getShortcut("copy.shortcut2");
			if (shortcut2 != null)
				textArea.getInputHandler().addKeyBinding(shortcut2, "copy");

			JPopupMenu menu = new JPopupMenu();
			JMenuItem copy = new JMenuItem(jEdit.getProperty("copy.label").replace("$",""));
			copy.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Registers.copy(textArea, '$');
				}
			});
			menu.add(copy);
			textArea.setRightClickPopup(menu);
			textArea.setRightClickPopupEnabled(true);

			textArea.getBuffer().setMode(jEdit.getMode("logs"));
			if (throwables.length != 0)
			{
				Throwable throwable = (Throwable) throwables[0];
				setThrowable(throwable);
			}
			combo = new JComboBox<Throwable>(throwables);
			combo.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent e)
				{
					setThrowable((Throwable) combo.getSelectedItem());
				}
			});
			getContentPane().add(combo, BorderLayout.NORTH);
			getContentPane().add(new JScrollPane(textArea));



			Box buttons = new Box(BoxLayout.X_AXIS);
			buttons.add(Box.createGlue());

			buttons.add(removeThisError = new JButton(jEdit.getProperty("common.removeCurrent")));
			buttons.add(Box.createHorizontalStrut(6));
			buttons.add(removeAllErrors = new JButton(jEdit.getProperty("common.clearAll")));

			ActionListener actionListener = new MyActionListener();
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
			textArea.getBuffer().setReadOnly(false);
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
			textArea.getBuffer().setReadOnly(true);
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
