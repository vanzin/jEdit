/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
//}}}

/**
 * A Statusbar widget that show the time of last save of the current buffer.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class LastModifiedWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view)
	{
		Widget lastModifiedWidget = new LastModifiedWidget(view);
		return lastModifiedWidget;
	} //}}}

	//{{{ BufferSetWidget class
	public static class LastModifiedWidget implements Widget
	{
		private final JLabel lastModifiedLabel;
		private final View view;
		private static final SimpleDateFormat sdf = new SimpleDateFormat();
		private static final Date date = new Date();


		LastModifiedWidget(View view)
		{
			this.view = view;
			lastModifiedLabel = new JLabel()
			{
				@Override
				public void addNotify()
				{
					super.addNotify();
					LastModifiedWidget.this.update();
					EditBus.addToBus(LastModifiedWidget.this);
				}

				@Override
				public void removeNotify()
				{
					super.removeNotify();
					EditBus.removeFromBus(LastModifiedWidget.this);
				}
			};
			lastModifiedLabel.setToolTipText(jEdit.getProperty("fileprop.lastmod"));
			update();
		}

		//{{{ getComponent() method
		public JComponent getComponent()
		{
			return lastModifiedLabel;
		} //}}}

		//{{{ update() method
		public void update()
		{
			Buffer buffer = view.getBuffer();
			String path = buffer.getPath();
			VFS vfs = VFSManager.getVFSForPath(path);
			Object session = vfs.createVFSSession(path, view);
			try
			{
				VFSFile file = vfs._getFile(session, path, view);
				lastModifiedLabel.setText(file.getExtendedAttribute(VFS.EA_MODIFIED));
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, e);
			}
			finally
			{
				try
				{
					vfs._endVFSSession(session, view);
				}
				catch (IOException e)
				{
				}
			}
		} //}}}

		//{{{ propertiesChanged() methods
		@Override
		public void propertiesChanged()
		{

		} //}}}

		//{{{ handleMessage() methods
		@EditBus.EBHandler
		public void handleMessage(EditPaneUpdate message)
		{
			if (message.getWhat() == EditPaneUpdate.BUFFER_CHANGED &&
				message.getEditPane().getView() == view)
			{
				update();
			}
		}

		@EditBus.EBHandler
		public void handleMessage(BufferUpdate message)
		{
			if (message.getBuffer() == view.getBuffer())
			{
				update();
			}
		} //}}}

	} //}}}

}