/*
 * MarkerHighlight.java - Paints marker highlights in the gutter
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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

//{{{ Imports
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Vector;
import org.gjt.sp.jedit.*;
//}}}

/**
 * A text area extension that highlights marker locations in the gutter.
 * Macros and plugins should not create instances of this class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class MarkerHighlight extends TextAreaExtension
{
	//{{{ MarkerHighlight constructor
	public MarkerHighlight(JEditTextArea textArea)
	{
		this.textArea = textArea;
	} //}}}

	//{{{ paintValidLine() method
	public void paintValidLine(Graphics2D gfx, int physicalLine,
		int start, int end, int y)
	{
		if(textArea.getBuffer().isLoaded() && highlightEnabled)
		{
			Buffer buffer = textArea.getBuffer();
			if(buffer.getMarkerInRange(start,end) != null)
			{
				gfx.setColor(markerHighlightColor);
				FontMetrics fm = textArea.getPainter().getFontMetrics();
				gfx.fillRect(0,y,textArea.getGutter()
					.getWidth(),fm.getHeight());
			}
		}
	} //}}}

	//{{{ getToolTipText() method
	public String getToolTipText(int x, int y)
	{
		if(textArea.getBuffer().isLoaded() && highlightEnabled)
		{
			int start = textArea.xyToOffset(0,y);
			int end = textArea.getScreenLineEndOffset(
				textArea.getScreenLineOfOffset(start));

			Marker marker = textArea.getBuffer().getMarkerInRange(start,end);
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

		return null;
	} //}}}

	//{{{ getMarkerHighlightColor() method
	public Color getMarkerHighlightColor()
	{
		return markerHighlightColor;
	} //}}}

	//{{{ setMarkerHighlightColor() method
	public void setMarkerHighlightColor(Color markerHighlightColor)
	{
		this.markerHighlightColor = markerHighlightColor;
	} //}}}

	//{{{ isHighlightEnabled() method
	public boolean isHighlightEnabled()
	{
		return highlightEnabled;
	} //}}}

	//{{{ setHighlightEnabled()
	public void setHighlightEnabled(boolean highlightEnabled)
	{
		this.highlightEnabled = highlightEnabled;
	} //}}}

	//{{{ Private members
	private JEditTextArea textArea;
	private boolean highlightEnabled;
	private Color markerHighlightColor;
	//}}}
}
