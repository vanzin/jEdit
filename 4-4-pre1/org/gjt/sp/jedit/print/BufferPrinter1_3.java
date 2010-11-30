/*
 * BufferPrinter1_3.java - Main class that controls printing
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
 * Portions copyright (C) 2002 Thomas Dilts
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

//{{{ Imports
import java.awt.print.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

public class BufferPrinter1_3
{
	//{{{ getPrintJob() method
	private static PrinterJob getPrintJob()
	{
		job = PrinterJob.getPrinterJob();

		int orientation = jEdit.getIntegerProperty("print.orientation",PageFormat.PORTRAIT);
		double width = jEdit.getDoubleProperty("print.width",0);
		double height = jEdit.getDoubleProperty("print.height",0);
		double x = jEdit.getDoubleProperty("print.x",0);
		double y = jEdit.getDoubleProperty("print.y",0);
		double pagewidth = jEdit.getDoubleProperty("print.pagewidth",0);
		double pageheight = jEdit.getDoubleProperty("print.pageheight",0);

		format = job.defaultPage();
		//format.setOrientation(PageFormat.PORTRAIT);
		if(width!=0 && height!=0 )
		{
			Paper pap = format.getPaper();
			pap.setImageableArea(x,y,width,height);
			pap.setSize(pagewidth,pageheight);
			format.setPaper(pap);
		}
		format.setOrientation(orientation);
		return job;

	}//}}}

	//{{{ pageSetup() method
	public static void pageSetup(View view)
	{
		job = getPrintJob();

		PageFormat newFormat = job.pageDialog(format);
		if(newFormat != null)
		{
			format = newFormat;
			jEdit.setIntegerProperty("print.orientation",format.getOrientation());
			Paper paper=format.getPaper();

			jEdit.setDoubleProperty("print.width",paper.getImageableWidth());
			jEdit.setDoubleProperty("print.height",paper.getImageableHeight());
			jEdit.setDoubleProperty("print.x",paper.getImageableX());
			jEdit.setDoubleProperty("print.y",paper.getImageableY());
			jEdit.setDoubleProperty("print.pagewidth",paper.getWidth());
			jEdit.setDoubleProperty("print.pageheight",paper.getHeight());
		}
	} //}}}

	//{{{ print() method
	public static void print(final View view, final Buffer buffer, boolean selection)
	{
		job = getPrintJob();
		job.setJobName(buffer.getPath());
		boolean header = jEdit.getBooleanProperty("print.header");
		boolean footer = jEdit.getBooleanProperty("print.footer");
		boolean lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		boolean color = jEdit.getBooleanProperty("print.color");
		Font font = jEdit.getFontProperty("print.font");

		BufferPrintable printable = new BufferPrintable(job,null,view,
			buffer,font,header,footer,lineNumbers,color);
		job.setPrintable(printable,format);

		if(!job.printDialog())
			return;

		printable.print();
	} //}}}

	//{{{ getPageFormat() method
	public static PageFormat getPageFormat()
	{
		return format;
	} //}}}

	//{{{ Private members
	private static PageFormat format;
	private static PrinterJob job;
	//}}}
}
