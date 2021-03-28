/*
 * MultiSelectWidgetFactory.java - The multiple selection widget service
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.event.MouseEvent;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public class MultiSelectWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		return new MultiSelectWidget(view);
	} //}}}

	//{{{ MultiSelectWidget class
	private static class MultiSelectWidget extends AbstractLabelWidget
	{
		MultiSelectWidget(View view)
		{
			super(view);
			label.setToolTipText(jEdit.getProperty("view.status.multi-tooltip"));
		}

		@Override
		protected void singleClick(MouseEvent e)
		{
			JEditTextArea textArea = view.getTextArea();
			if (textArea != null)
				textArea.toggleMultipleSelectionEnabled();
		}

		//{{{ update() method
		@Override
		public void update()
		{
			JEditTextArea textArea = view.getTextArea();
			if (textArea != null)
			{
				label.setEnabled(textArea.isMultipleSelectionEnabled());
				label.setText("Multi sel: " + textArea.isMultipleSelectionEnabled());
			}
		} //}}}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Misc;
		}
	} //}}}

}
