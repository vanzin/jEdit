
/*
 * PrintPreviewModel.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2016 Dale Anson
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
package org.gjt.sp.jedit.print;


import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.util.HashMap;

import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;


/**
 * Data model for the print preview pane, contains setters and getters for the
 * print preview display.
 */
public class PrintPreviewModel extends PageFormat
{

	private View view;
	private Buffer buffer;
	private PrintService printService;
	private PrintRequestAttributeSet attributes;
	private HashMap<Integer, Range> pageRanges;
	private int pageNumber = 1;
	private PrintRangeType printRangeType = PrintRangeType.ALL;
	private Graphics gfx;
	public static enum Zoom { NONE,  IN,  OUT,  WIDTH,  PAGE };
	private Zoom zoom = Zoom.NONE;
	private float zoomLevel = 1.0f;


	public PrintPreviewModel()
	{
		super();
	}


	public PrintPreviewModel( View view, Buffer buffer, PrintService printService, PrintRequestAttributeSet attributes, HashMap<Integer, Range> pageRanges )
	{
		super();
		this.view = view;
		this.buffer = buffer;
		this.printService = printService;
		this.attributes = attributes;
		this.pageRanges = pageRanges;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("PrintPreviewModel[");
		sb.append("view=").append(view);
		sb.append(", buffer=").append(buffer);
		sb.append(", ps=").append(printService);
		sb.append(", page=").append(pageNumber);
		sb.append(", range=").append(pageRanges);
		sb.append(", gfx=").append(gfx);
		sb.append(", zoom=").append(zoom);
		sb.append(", zoomLevel=").append(zoomLevel);
		sb.append(']');
		return sb.toString();
	}


	public int getPageNumber()
	{
		return pageNumber;
	}


	public void setPageNumber( int number )
	{
		pageNumber = number;
	}


	public HashMap<Integer, Range> getPageRanges()
	{
		return pageRanges;
	}


	public void setPageRanges( HashMap<Integer, Range> pageRanges )
	{
		this.pageRanges = pageRanges;
	}
	
	public void setPrintRangeType(PrintRangeType type)
	{
		printRangeType = type;
	}
	
	public PrintRangeType getPrintRangeType()
	{
		return printRangeType;	
	}


	/**
	 * Returns the value of printService.
	 */
	public PrintService getPrintService()
	{
		return printService;
	}


	/**
	 * Sets the value of printService.
	 * @param printService The value to assign printService.
	 */
	public void setPrintService( PrintService printService )
	{
		this.printService = printService;
	}


	/**
	 * Returns the value of attributes.
	 */
	public PrintRequestAttributeSet getAttributes()
	{
		return attributes;
	}


	/**
	 * Sets the value of attributes.
	 * @param attributes The value to assign attributes.
	 */
	public void setAttributes( PrintRequestAttributeSet attributes )
	{
		this.attributes = attributes;
	}


	/**
	 * Returns the value of view.
	 */
	public View getView()
	{
		return view;
	}


	/**
	 * Sets the value of view.
	 * @param view The value to assign view.
	 */
	public void setView( View view )
	{
		this.view = view;
	}


	/**
	 * Returns the value of buffer.
	 */
	public Buffer getBuffer()
	{
		return buffer;
	}


	/**
	 * Sets the value of buffer.
	 * @param buffer The value to assign buffer.
	 */
	public void setBuffer( Buffer buffer )
	{
		this.buffer = buffer;
	}


	public void setGraphics( Graphics g )
	{
		gfx = g;
	}


	public Graphics getGraphics()
	{
		return gfx;
	}


	public Zoom getZoom()
	{
		return zoom;
	}


	public void setZoom( Zoom zoom )
	{
		this.zoom = zoom;
	}


	public void setZoomLevel( float level )
	{
		if ( zoomLevel <= 0 )
		{
			return;
		}

		zoomLevel = level;
	}


	public float getZoomLevel()
	{
		return zoomLevel;
	}
}
