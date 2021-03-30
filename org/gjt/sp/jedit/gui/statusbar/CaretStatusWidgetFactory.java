/*
 * IndentWidgetFactory.java - The indent widget service
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2021 Matthieu Casanova
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
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.StandardUtilities;

import javax.swing.text.Segment;
import java.awt.event.MouseEvent;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 5.7pre1
 */
public class CaretStatusWidgetFactory implements StatusWidgetFactory
{
	@Override
	public Widget getWidget(View view)
	{
		return new CaretStatusWidgetFactory.CaretStatus(view);
	}

	//{{{ IndentWidget class
	private static class CaretStatus extends AbstractLabelWidget
	{
		private final StringBuilder buf;
		private final Segment seg;
		private boolean lineNumberOption;
		private boolean dotOption;
		private boolean virtualOption;
		private boolean offsetOption;
		private boolean bufferlength;

		CaretStatus(View view)
		{
			super(view);
			buf = new StringBuilder();
			seg = new Segment();
			label.setToolTipText(jEdit.getProperty("view.status.caret-tooltip"));
		}

		@Override
		protected void doubleClick(MouseEvent e)
		{
			view.getTextArea().showGoToLineDialog();
		}

		@Override
		public void propertiesChanged()
		{
			lineNumberOption = jEdit.getBooleanProperty("view.status.show-caret-linenumber", true);
			dotOption = jEdit.getBooleanProperty("view.status.show-caret-dot", true);
			virtualOption = jEdit.getBooleanProperty("view.status.show-caret-virtual", true);
			offsetOption = jEdit.getBooleanProperty("view.status.show-caret-offset", true);
			bufferlength = jEdit.getBooleanProperty("view.status.show-caret-bufferlength", true);
		}

		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();

			if(!buffer.isLoaded() ||
				/* can happen when switching buffers sometimes */
				buffer != view.getTextArea().getBuffer())
			{
				label.setText(" ");
				return;
			}

			JEditTextArea textArea = view.getTextArea();

			int caretPosition = textArea.getCaretPosition();
			int currLine = textArea.getCaretLine();

			// there must be a better way of fixing this...
			// the problem is that this method can sometimes
			// be called as a result of a text area scroll
			// event, in which case the caret position has
			// not been updated yet.
			if(currLine >= buffer.getLineCount())
				return; // hopefully another caret update will come?

			int start = textArea.getLineStartOffset(currLine);
			int dot = caretPosition - start;

			if(dot < 0)
				return;

			int bufferLength = buffer.getLength();

			buffer.getText(start,dot,seg);
			int virtualPosition = StandardUtilities.getVirtualWidth(seg,
				buffer.getTabSize());
			// for GC
			seg.array = null;
			seg.count = 0;

			if (lineNumberOption)
			{
				buf.append(currLine + 1);
				buf.append(',');
			}
			if (dotOption)
			{
				buf.append(dot + 1);
			}

			if (virtualOption && virtualPosition != dot)
			{
				buf.append('-');
				buf.append(virtualPosition + 1);
			}
			if (buf.length() > 0)
			{
				buf.append(' ');
			}


			if (offsetOption && bufferlength)
			{
				buf.append('(');
				buf.append(caretPosition);
				buf.append('/');
				buf.append(bufferLength);
				buf.append(')');
			}
			else if (offsetOption)
			{
				buf.append('(');
				buf.append(caretPosition);
				buf.append(')');
			}
			else if (bufferlength)
			{
				buf.append('(');
				buf.append(bufferLength);
				buf.append(')');
			}

			label.setText(buf.toString());
			buf.setLength(0);
		}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Caret;
		}
	} //}}}
}
