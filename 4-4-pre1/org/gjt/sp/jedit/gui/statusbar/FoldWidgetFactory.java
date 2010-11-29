/*
 * FoldWidgetFactory.java - The fold widget service
 * :tabSize=8:indentSize=8:noTabs=false:
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.BufferOptions;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14 
 */
public class FoldWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view) 
	{
		Widget fold = new FoldWidget(view);
		return fold;
	} //}}}

	//{{{ FoldWidget class
	private static class FoldWidget implements Widget
	{
		private final JLabel fold;
		private final View view;
		public FoldWidget(final View view) 
		{
			fold = new ToolTipLabel();
			this.view = view;
			fold.setToolTipText(jEdit.getProperty("view.status.mode-tooltip"));
			fold.addMouseListener(new MouseAdapter() 
					      {
						      @Override
						      public void mouseClicked(MouseEvent evt)
						      {
							      if(evt.getClickCount() == 2)
								      new BufferOptions(view,view.getBuffer());
						      }
					      });
		}
		
		public JComponent getComponent() 
		{
			return fold;
		}
		
		public void update() 
		{
			Buffer buffer = view.getBuffer();
			if (buffer.isLoaded())
				fold.setText((String)view.getBuffer().getProperty("folding"));
		}
		
		public void propertiesChanged()
		{
		}
	} //}}}
}
