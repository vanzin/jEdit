/*
 * MarkerHighlight.java - Paints marker highlights in the gutter
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class MarkerHighlight implements TextAreaHighlight
{
	public void init(JEditTextArea textArea, TextAreaHighlight next)
	{
		this.textArea = textArea;
		this.next = next;
	}

	public void paintHighlight(Graphics gfx, int line, int y)
	{
		if(textArea.getBuffer().isLoaded() && highlightEnabled)
		{
			Buffer buffer = textArea.getBuffer();
			if(buffer.getMarkerAtLine(buffer.virtualToPhysical(line)) != null)
			{
				int firstLine = textArea.getFirstLine();
				line -= firstLine;

				FontMetrics fm = textArea.getPainter().getFontMetrics();
				gfx.setColor(markerHighlightColor);
				gfx.fillRect(0,line * fm.getHeight(),textArea.getGutter()
					.getWidth(),fm.getHeight());
			}
		}

		if(next != null)
			next.paintHighlight(gfx,line,y);
	}

	public String getToolTipText(MouseEvent evt)
	{
		if(textArea.getBuffer().isLoaded() && highlightEnabled)
		{
			FontMetrics fm = textArea.getPainter().getFontMetrics();
			int line = textArea.getFirstLine() + evt.getY() / fm.getHeight();

			Buffer buffer = textArea.getBuffer();
			Marker marker = buffer.getMarkerAtLine(buffer.virtualToPhysical(line));
			if(marker != null)
			{
				char shortcut = marker.getShortcut();
				if(shortcut == '\0')
					return jEdit.getProperty("view.gutter.marker.no-name");
				else
				{
					String[] args = { String.valueOf(shortcut) };
					return jEdit.getProperty("view.gutter.marker",args);
				}
			}
		}

		if(next != null)
			return next.getToolTipText(evt);
		else
			return null;
	}

	public Color getMarkerHighlightColor()
	{
		return markerHighlightColor;
	}

	public void setMarkerHighlightColor(Color markerHighlightColor)
	{
		this.markerHighlightColor = markerHighlightColor;
	}

	public boolean isHighlightEnabled()
	{
		return highlightEnabled;
	}

	public void setHighlightEnabled(boolean highlightEnabled)
	{
		this.highlightEnabled = highlightEnabled;
	}

	// private members
	private JEditTextArea textArea;
	private TextAreaHighlight next;
	private boolean highlightEnabled;
	private Color markerHighlightColor;
}
