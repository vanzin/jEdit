/*
 * EncodingWidgetFactory.java - The encoding widget service
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
import java.util.Arrays;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.BufferOptions;
import org.gjt.sp.jedit.gui.DialogChooser;
import org.gjt.sp.jedit.msg.BufferUpdate;

import javax.swing.*;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14 
 */
public class EncodingWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		return new EncodingWidget(view);
	} //}}}

	//{{{ EncodingWidget class
	private static class EncodingWidget extends AbstractLabelWidget
	{
		EncodingWidget(View view)
		{
			super(view);
			label.setToolTipText(jEdit.getProperty("view.status.encoding-tooltip"));
		}

		@Override
		protected void singleClick(MouseEvent e)
		{
			String[] encodings = MiscUtilities.getEncodings(true);
			Arrays.sort(encodings, CASE_INSENSITIVE_ORDER);
			Buffer buffer = view.getBuffer();
			String currentEncoding = buffer.getStringProperty(JEditBuffer.ENCODING);
			DialogChooser.openListChooserWindow(label,
				currentEncoding,
				listSelectionEvent -> EventQueue.invokeLater(() ->
				{
					JList<String> list = (JList<String>) listSelectionEvent.getSource();
					String selectedValue = list.getSelectedValue();
					int selectedOption = DialogChooser.openChooserWindow(view,
						jEdit.getProperty("buffer.encoding.reload", new String[]{selectedValue}),
						jEdit.getProperty("buffer.encoding.change", new String[]{selectedValue}));
					switch (selectedOption)
					{
						case 0:
							buffer.reloadWithEncoding(view, selectedValue);
							break;
						case 1:
							buffer.setBooleanProperty(Buffer.ENCODING_AUTODETECT,false);
							buffer.setDirty(true);
							EditBus.send(new BufferUpdate(buffer,null,BufferUpdate.PROPERTIES_CHANGED));
							break;
					}
				}),
				encodings);
		}

		@Override
		protected void rightClick(MouseEvent e)
		{
			new BufferOptions(view, view.getBuffer());
		}

		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			if (buffer.isLoaded())
				label.setText(buffer.getStringProperty("encoding"));
		}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}
}
