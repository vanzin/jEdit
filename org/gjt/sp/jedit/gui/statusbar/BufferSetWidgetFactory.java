/*
 * BufferSetWidgetFactory.java - The bufferSet widget service
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008, 2009 Matthieu Casanova
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
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.bufferset.BufferSetManager;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.msg.PropertiesChanged;
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
	@Override
	public Widget getWidget(View view)
	{
		Widget bufferSetWidget = new BufferSetWidget();
		return bufferSetWidget;
	} //}}}

	//{{{ BufferSetWidget class
	private static class BufferSetWidget implements Widget, EBComponent
	{
		private final JLabel bufferSetLabel;
		private BufferSet.Scope currentScope;

		BufferSetWidget()
		{
			bufferSetLabel = new ToolTipLabel()
			{
				@Override
				public void addNotify()
				{
					super.addNotify();
					BufferSetWidget.this.update();
					EditBus.addToBus(BufferSetWidget.this);
				}

				@Override
				public void removeNotify()
				{
					super.removeNotify();
					EditBus.removeFromBus(BufferSetWidget.this);
				}
			};
			update();
			bufferSetLabel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent evt)
				{
					if (evt.getClickCount() == 2)
					{
						BufferSetManager bufferSetManager = jEdit.getBufferSetManager();
						BufferSet.Scope scope = bufferSetManager.getScope();
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
						bufferSetManager.setScope(scope);
					}
				}
			});
		}

		//{{{ getComponent() method
		@Override
		public JComponent getComponent()
		{
			return bufferSetLabel;
		} //}}}

		//{{{ update() method
		@Override
		public void update()
		{
			BufferSet.Scope scope = jEdit.getBufferSetManager().getScope();
			if (currentScope == null || currentScope != scope)
			{
				bufferSetLabel.setText(scope.name());
				bufferSetLabel.setToolTipText(jEdit.getProperty("view.status.bufferset-tooltip", new Object[] {scope}));
				currentScope = scope;
			}
		} //}}}

		//{{{ handleMessage() method
		@Override
		public void handleMessage(EBMessage message)
		{
			if (message instanceof PropertiesChanged)
			{
				PropertiesChanged propertiesChanged = (PropertiesChanged) message;
				if (propertiesChanged.getSource() instanceof BufferSetManager)
				{
					update();
				}
			}
		} //}}}
	} //}}}
}