/*
 * SelectionLengthWidgetFactory.java - A status bar widget that displays
 * the length of the selection at caret
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.Selection;

import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
//}}}

/**
 * @author Matthieu Casanova 
 * @since jEdit 4.3pre15
 */
public class SelectionLengthWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view)
	{
		Widget selectionLengthWidget = new SelectionLengthWidget(view);
		return selectionLengthWidget;
	} //}}}

	//{{{ SelectionLengthWidget class
	public static class SelectionLengthWidget implements Widget
	{
		private final SelectionLength selectionLength;
		private final View view;
		private TextArea textArea;

		SelectionLengthWidget(View view)
		{
			this.view = view;
			textArea = view.getTextArea();
			selectionLength = new SelectionLength();
			selectionLength.setForeground(jEdit.getColorProperty("view.status.foreground"));
			selectionLength.setBackground(jEdit.getColorProperty("view.status.background"));
			EditBus.addToBus(this);
		}

		public JComponent getComponent()
		{
			return selectionLength;
		}

		public void update()
		{
		}

		public void propertiesChanged()
		{
		}

		@EBHandler
		public void handleViewUpdate(ViewUpdate viewUpdate)
		{
			if (viewUpdate.getView() == view && viewUpdate.getWhat() == ViewUpdate.EDIT_PANE_CHANGED)
			{
				if (textArea != null)
				{
					textArea.removeCaretListener(selectionLength);
				}
				textArea = view.getTextArea();
				if (selectionLength.visible)
					textArea.addCaretListener(selectionLength);
			}
		}

		private class SelectionLength extends JLabel implements CaretListener
		{
			boolean visible;
			//{{{ addNotify() method
			@Override
			public void addNotify()
			{
				super.addNotify();
				visible = true;
				textArea.addCaretListener(this);
			} //}}}


			//{{{ removeNotify() method
			@Override
			public void removeNotify()
			{
				visible = false;
				textArea.removeCaretListener(this);
				super.removeNotify();
			} //}}}

			public void caretUpdate(CaretEvent e)
			{
				Selection selection = textArea.getSelectionAtOffset(textArea.getCaretPosition());
				if (selection == null)
				{
					setText("");
				}
				else
				{
					int selectionEnd = selection.getEnd();
					int selectionStart = selection.getStart();
					int len;
					if (selection instanceof Selection.Rect)
					{
						int startLine = selection.getStartLine();
						int endLine = selection.getEndLine();
						JEditTextArea textArea = view.getTextArea();
						int startLineOffset = textArea.getLineStartOffset(startLine);
						int endLineOffset = textArea.getLineStartOffset(endLine);
						int lines = endLine - startLine + 1;
						int columns = (selectionEnd - endLineOffset) -
							(selectionStart - startLineOffset);
						len = lines * columns;
					}
					else
						len = selectionEnd - selectionStart;
					setText(Integer.toString(len));
				}
			}
		}
	} //}}}
}