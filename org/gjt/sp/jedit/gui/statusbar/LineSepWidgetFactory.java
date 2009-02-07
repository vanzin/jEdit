/*
 * LineSepWidgetFactory.java - The line separator widget service
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
import org.gjt.sp.jedit.buffer.JEditBuffer;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public class LineSepWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	public Widget getWidget(View view) 
	{
		Widget lineSep = new LineSepWidget(view);
		return lineSep;
	} //}}}
	
	//{{{ LineSepWidget class
	private static class LineSepWidget implements Widget
	{
		private final JLabel lineSep;
		private final View view;
		
		//{{{ LineSepWidget constructor
		LineSepWidget(final View view) 
		{
			lineSep = new ToolTipLabel();
			lineSep.setHorizontalAlignment(SwingConstants.CENTER);
			lineSep.setToolTipText(jEdit.getProperty("view.status.linesep-tooltip"));
			this.view = view;
			lineSep.addMouseListener(new MouseAdapter() 
						 {
							 @Override
							 public void mouseClicked(MouseEvent evt)
							 {
								 view.getBuffer().toggleLineSeparator(view);
							 }
						 });
		} //}}}

		
		//{{{ getComponent() method
		public JComponent getComponent() 
		{
			return lineSep;
		} //}}}

		
		//{{{ update() method
		public void update() 
		{
			Buffer buffer = view.getBuffer();
			String lineSep = buffer.getStringProperty(JEditBuffer.LINESEP);
			if("\n".equals(lineSep))
				this.lineSep.setText("U");
			else if("\r\n".equals(lineSep))
				this.lineSep.setText("W");
			else if("\r".equals(lineSep))
				this.lineSep.setText("M");
		} //}}}

		
	        //{{{ propertiesChanged() method
	        public void propertiesChanged()
		{
			// retarded GTK look and feel!
			Font font = new JLabel().getFont();
			//UIManager.getFont("Label.font");
			FontMetrics fm = lineSep.getFontMetrics(font);
			Dimension dim = new Dimension(Math.max(
							       Math.max(fm.charWidth('U'),
									fm.charWidth('W')),
							       fm.charWidth('M')) + 1,
				fm.getHeight());
			lineSep.setPreferredSize(dim);
			lineSep.setMaximumSize(dim);
		} //}}}
	} //}}}

}
