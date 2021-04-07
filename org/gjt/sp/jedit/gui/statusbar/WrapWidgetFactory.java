/*
 * WrapWidgetFactory.java - The wrap widget service
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
import java.awt.*;
import java.awt.event.MouseEvent;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.WordWrap;
import org.gjt.sp.jedit.gui.DialogChooser;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferUpdate;

import javax.swing.*;

import static org.gjt.sp.jedit.Buffer.LARGE_MODE_FILE;
import static org.gjt.sp.jedit.buffer.WordWrap.none;
import static org.gjt.sp.jedit.buffer.WordWrap.soft;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public class WrapWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		return new WrapWidget(view);
	} //}}}

	//{{{ WrapWidget class
	private static class WrapWidget extends AbstractLabelWidget
	{
		//{{{ WrapWidget constructor
		WrapWidget(View view)
		{
			super(view);
		} //}}}

		@Override
		protected void singleClick(MouseEvent e)
		{
			Buffer buffer = view.getBuffer();
			WordWrap currentFoldingMode = buffer.getWordWrap();
			DialogChooser.openListChooserWindow(label,
				currentFoldingMode,
				listSelectionEvent -> EventQueue.invokeLater(() ->
				{
					JList<WordWrap> list = (JList<WordWrap>) listSelectionEvent.getSource();
					WordWrap selectedValue = list.getSelectedValue();
					buffer.setWordWrap(selectedValue);
					EditBus.send(new BufferUpdate(buffer,null,BufferUpdate.PROPERTIES_CHANGED));

				}),
				WordWrap.values());
		}

		//{{{ update() method
		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			WordWrap wrap = buffer.getWordWrap();
			if (largeBufferDeactivateWrap() && wrap == soft)
			{
				wrap = none;
			}
			label.setToolTipText(jEdit.getProperty("view.status.wrap-tooltip",
				new String[]{jEdit.getProperty("wrap." + wrap)}));
			label.setText("Wrap: " + wrap);
		} //}}}

		//{{{ largeBufferDeactivateWrap() method
		private boolean largeBufferDeactivateWrap()
		{
			Buffer buffer = view.getBuffer();
			String largeFileMode = buffer.getStringProperty(LARGE_MODE_FILE);
			return "limited".equals(largeFileMode) || "nohighlight".equals(largeFileMode);
		} //}}}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}

}
