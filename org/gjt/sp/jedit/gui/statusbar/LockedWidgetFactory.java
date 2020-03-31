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
    private static class LockedWidget implements Widget
    {
        private final JLabel cmp;
        private final View view;
        LockedWidget(final View view)
        {
            cmp = new ToolTipLabel();
            cmp.setHorizontalAlignment(SwingConstants.CENTER);

            this.view = view;
            cmp.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent evt)
                {
                    view.getBuffer().toggleLocked(view);
                }
            });
        }

        @Override
        public JComponent getComponent()
        {
            return cmp;
        }

        @Override
        public void update()
        {
            Buffer buffer = view.getBuffer();
            Boolean locked = buffer.isLocked();

            cmp.setText(locked ? "L" : "l");
            cmp.setEnabled(locked);

            cmp.setToolTipText(jEdit.getProperty("view.status.locked-tooltip",
                    new Integer[] { locked ? 1 : 0 }));
        }

        @Override
        public void propertiesChanged()
        {
            // retarded GTK look and feel!
            Font font = new JLabel().getFont();
            //UIManager.getFont("Label.font");
            FontMetrics fm = cmp.getFontMetrics(font);
            Dimension dim = new Dimension(
                    Math.max(fm.charWidth('r'),fm.charWidth('R')) + 1,
                    fm.getHeight());
            cmp.setPreferredSize(dim);
            cmp.setMaximumSize(dim);

        }
    } //}}}
}
