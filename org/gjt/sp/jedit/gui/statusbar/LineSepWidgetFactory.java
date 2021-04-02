/*
 * LineSepWidgetFactory.java - The line separator widget service
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008-2021 Matthieu Casanova
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
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.LineSepListCellRenderer;
import org.gjt.sp.jedit.jEdit;
import org.jedit.misc.LineSepType;

import javax.swing.*;
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
			EventQueue.invokeLater(() ->
			{
				JList<LineSepType> lineSeparatorList = new JList<>(new LineSepType[] {LineSepType.LF, LineSepType.CRLF, LineSepType.CR});
				lineSeparatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				lineSeparatorList.setCellRenderer(new LineSepListCellRenderer());
				Buffer buffer = view.getBuffer();
				String property = buffer.getStringProperty(JEditBuffer.LINESEP);
				LineSepType currentSeparator = LineSepType.fromSeparator(property);
				lineSeparatorList.setSelectedValue(currentSeparator, true);

				lineSeparatorList.setBorder(BorderFactory.createEtchedBorder());
				lineSeparatorList.setVisibleRowCount(3);
				JDialog window = new JDialog();
				window.setUndecorated(true);
				window.addWindowListener(new WindowAdapter()
				{
					@Override
					public void windowDeactivated(WindowEvent e)
					{
						window.dispose();
					}
				});
				lineSeparatorList.addListSelectionListener(e1 ->
				{
					if (!e1.getValueIsAdjusting())
					{
						LineSepType selectedValue = lineSeparatorList.getSelectedValue();
						if (selectedValue != null)
							buffer.setLineSeparator(selectedValue.getSeparator());
						window.dispose();
					}
				});
				window.getContentPane().add(lineSeparatorList);
				window.pack();
				window.setLocationRelativeTo(label);
				window.setLocation(window.getX(), window.getY() - 20);
				window.setVisible(true);
				EventQueue.invokeLater(lineSeparatorList::requestFocus);
			});
		}

		//{{{ update() method
		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			String lineSep = buffer.getStringProperty(JEditBuffer.LINESEP);
			LineSepType lineSepType = LineSepType.fromSeparator(lineSep);
			label.setText(lineSepType.name());
		} //}}}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}

}
