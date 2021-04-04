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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.BufferOptions;
import org.gjt.sp.jedit.gui.DialogChooser;
import org.gjt.sp.jedit.jEdit;

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
			EventQueue.invokeLater(() ->
			{
				String[] encodings = MiscUtilities.getEncodings(true);
				Arrays.sort(encodings, CASE_INSENSITIVE_ORDER);
				JList<String> list = new JList<>(encodings);
				list.setVisibleRowCount(20);
				list.setLayoutOrientation(JList.VERTICAL_WRAP);
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				Buffer buffer = view.getBuffer();
				String currentEncoding = buffer.getStringProperty(JEditBuffer.ENCODING);
				list.setSelectedValue(currentEncoding, true);
				list.setBorder(BorderFactory.createEtchedBorder());
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
				list.addListSelectionListener(e1 ->
				{
					if (!e1.getValueIsAdjusting())
					{
						String selectedValue = list.getSelectedValue();
						if (selectedValue != null)
						{
							if (!currentEncoding.equals(selectedValue))
							{
								EventQueue.invokeLater(() ->
								{
									int selectedOption = DialogChooser.openChooseWindow(view,
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
											update();
											break;
									}
								});

							}
						}
						window.dispose();
					}
				});
				window.getContentPane().add(new JScrollPane(list));
				window.pack();
				window.setLocationRelativeTo(label);
				window.setLocation(window.getX(), window.getY() - 60);
				window.setVisible(true);
				list.ensureIndexIsVisible(list.getSelectedIndex());
				EventQueue.invokeLater(list::requestFocus);
			});
		}

		@Override
		protected void doubleClick(MouseEvent e)
		{
			new BufferOptions(view,view.getBuffer());
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
