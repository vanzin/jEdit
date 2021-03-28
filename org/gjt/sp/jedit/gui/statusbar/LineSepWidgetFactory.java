/*
 * LineSepWidgetFactory.java - The line separator widget service
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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public class LineSepWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		return new LineSepWidget(view);
	} //}}}
	
	//{{{ LineSepWidget class
	private static class LineSepWidget extends AbstractLabelWidget
	{
		//{{{ LineSepWidget constructor
		LineSepWidget(View view)
		{
			super(view);
			label.setToolTipText(jEdit.getProperty("view.status.linesep-tooltip"));
		} //}}}

		@Override
		protected void singleClick(MouseEvent e)
		{
			view.getBuffer().toggleLineSeparator(view);
		}

		//{{{ update() method
		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			String lineSep = buffer.getStringProperty(JEditBuffer.LINESEP);
			if("\n".equals(lineSep))
				label.setText("CR");
			else if("\r\n".equals(lineSep))
				label.setText("CRLF");
			else if("\r".equals(lineSep))
				label.setText("LF");
		} //}}}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}

}
