/*
 * WrapWidgetFactory.java - The wrap widget service
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public class WrapWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view)
	{
		Widget wrap = new WrapWidget(view);
		return wrap;
	} //}}}

	//{{{ WrapWidget class
	private static class WrapWidget implements Widget
	{
		private final JLabel wrap;
		private final View view;
		public WrapWidget(final View view)
		{
			wrap = new ToolTipLabel();
			wrap.setHorizontalAlignment(SwingConstants.CENTER);

			this.view = view;
			wrap.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent evt)
				{
					view.getBuffer().toggleWordWrap(view);
				}
			});
		}

		public JComponent getComponent()
		{
			return wrap;
		}

		public void update()
		{
			Buffer buffer = view.getBuffer();
			String wrap = buffer.getStringProperty("wrap");
			if (largeBufferDeactivateWrap() && "soft".equals(wrap))
			{
				wrap = "none";
			}
			this.wrap.setToolTipText(jEdit.getProperty("view.status.wrap-tooltip",
								   new String[]{jEdit.getProperty("wrap." + wrap)}));
			if("none".equals(wrap))
			{
				this.wrap.setEnabled(false);
				this.wrap.setText("N");
			}
			else
			{
				this.wrap.setEnabled(true);
				if ("hard".equals(wrap))
					this.wrap.setText("H");
				else if ("soft".equals(wrap))
					this.wrap.setText("S");
			}
		}

		public void propertiesChanged()
		{
			// retarded GTK look and feel!
			Font font = new JLabel().getFont();
			//UIManager.getFont("Label.font");
			FontMetrics fm = wrap.getFontMetrics(font);
			Dimension dim = new Dimension(Math.max(Math.max(fm.charWidth('N'),
									fm.charWidth('H')),
					fm.charWidth('S')) + 1,
				fm.getHeight());
			wrap.setPreferredSize(dim);
			wrap.setMaximumSize(dim);
		}

		private boolean largeBufferDeactivateWrap()
		{
			Buffer buffer = view.getBuffer();
			String largeFileMode = buffer.getStringProperty("largefilemode");
			return "limited".equals(largeFileMode) || "nohighlight".equals(largeFileMode);
		}
	} //}}}

}
