/*
 * BufferSetWidgetFactory.java - The bufferSet widget service
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui.statusbar;

//{{{ Imports
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.bufferset.BufferSet;
//}}}

/**
 * A Statusbar widget that show the bufferSet's scope of the current edit pane.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSetWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view)
	{
		Widget bufferSetWidget = new BufferSetWidget(view);
		return bufferSetWidget;
	} //}}}

	//{{{ BufferSetWidget class
	private static class BufferSetWidget implements Widget, EBComponent
	{
		private final JLabel bufferSetLabel;
		private final View view;
		private BufferSet.Scope currentScope;

		BufferSetWidget(final View view)
		{
			bufferSetLabel = new ToolTipLabel()
			{
				@Override
				public void addNotify()
				{
					super.addNotify();
					EditBus.addToBus(BufferSetWidget.this);
				}

				@Override
				public void removeNotify()
				{
					super.removeNotify();
					EditBus.removeFromBus(BufferSetWidget.this);
				}
			};
			this.view = view;
			update();
			bufferSetLabel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent evt)
				{
					if (evt.getClickCount() == 2)
					{
						EditPane editPane = view.getEditPane();
						BufferSet.Scope scope = editPane.getBufferSetScope();
						switch (scope)
						{
							case global:
								scope = BufferSet.Scope.view;
								break;
							case view:
								scope = BufferSet.Scope.editpane;
								break;
							case editpane:
								scope = BufferSet.Scope.global;
								break;
						}
						editPane.setBufferSetScope(scope);
					}
				}
			});
		}

		//{{{ getComponent() method
		public JComponent getComponent()
		{
			return bufferSetLabel;
		} //}}}

		//{{{ update() method
		public void update()
		{
			BufferSet.Scope scope = view.getEditPane().getBufferSetScope();
			if (currentScope == null || !currentScope.equals(scope))
			{
				bufferSetLabel.setText(scope.toString().substring(0,1).toUpperCase());
				bufferSetLabel.setToolTipText(jEdit.getProperty("view.status.bufferset-tooltip", new Object[] {scope}));
				currentScope = scope;
			}
		} //}}}

		//{{{ propertiesChanged() method
		public void propertiesChanged()
		{
			// retarded GTK look and feel!
			Font font = new JLabel().getFont();
			//UIManager.getFont("Label.font");
			FontMetrics fm = bufferSetLabel.getFontMetrics(font);
			Dimension dim = new Dimension(Math.max(fm.charWidth('E'),Math.max(fm.charWidth('V'),
								fm.charWidth('G'))),
								fm.getHeight());
			bufferSetLabel.setPreferredSize(dim);
			bufferSetLabel.setMaximumSize(dim);
		} //}}}

		//{{{ handleMessage() method
		public void handleMessage(EBMessage message)
		{
			if (message instanceof ViewUpdate)
			{
				ViewUpdate viewUpdate = (ViewUpdate) message;
				if (viewUpdate.getWhat() == ViewUpdate.EDIT_PANE_CHANGED)
				{
					update();
				}
			}
			else if (message instanceof EditPaneUpdate)
			{
				EditPaneUpdate editPaneUpdate = (EditPaneUpdate) message;
				if (editPaneUpdate.getEditPane() == view.getEditPane() &&
					editPaneUpdate.getWhat() == EditPaneUpdate.BUFFERSET_CHANGED)
				{
					update();
				}
			}
		} //}}}

	} //}}}

}