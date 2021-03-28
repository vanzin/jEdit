/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2017 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
//}}}

/**
 * @author Roman Tsourick
 */
public class LockedWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() class
	@Override
	public Widget getWidget(View view)
	{
		Widget widget = new LockedWidget(view);
		return widget;
	} //}}}

	//{{{ LockedWidget class
	private static class LockedWidget extends AbstractLabelWidget
	{
		private static Icon lockClosed;
		private static Icon lockOpened;

		LockedWidget(View view)
		{
			super(view);
			initIcons();
			Dimension dim = new Dimension(16, 16);
			label.setMinimumSize(dim);
			label.setPreferredSize(dim);
			label.setMaximumSize(dim);
		}

		@Override
		protected void singleClick(MouseEvent e)
		{
			view.getBuffer().toggleLocked(view);
		}

		private void initIcons()
		{
			if (lockClosed == null)
			{
				try
				{
					lockClosed = new ImageIcon(
						new ImageIcon(new URL("jeditresource:/org/gjt/sp/jedit/icons/themes/lock-rounded.png"))
							.getImage()
							.getScaledInstance(14, 14, Image.SCALE_SMOOTH));
					lockOpened = new ImageIcon(
						new ImageIcon(new URL("jeditresource:/org/gjt/sp/jedit/icons/themes/lock-rounded-open.png"))
							.getImage()
							.getScaledInstance(14, 14, Image.SCALE_SMOOTH));
				}
				catch (MalformedURLException e)
				{
					Log.log(Log.ERROR, this, "Unable to load icon");
				}
			}
		}

		@Override
		public void update()
		{
			Buffer buffer = view.getBuffer();
			boolean locked = buffer.isLocked();
			label.setIcon(locked ? lockClosed : lockOpened);

			label.setToolTipText(jEdit.getProperty("view.status.locked-tooltip",
				new Integer[]{locked ? 1 : 0}));
		}

		@Override
		public boolean test(StatusBarEventType statusBarEventType)
		{
			return statusBarEventType == StatusBarEventType.Buffer;
		}
	} //}}}
}
