/*
 * BufferPrinter.java - Main class that controls printing
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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

public class BufferPrinter
{
	//{{{ pageSetup() method
	public static void pageSetup(View view)
	{
		PrinterJob job = PrinterJob.getPrinterJob();
		if(format == null)
			format = job.defaultPage();

		PageFormat newFormat = job.pageDialog(format);
		if(newFormat != null)
			format = newFormat;
	} //}}}

	//{{{ print() method
	public static void print(View view, Buffer buffer, boolean selection)
	{
		PrinterJob job = PrinterJob.getPrinterJob();
		if(format == null)
			format = job.defaultPage();

		Book book = new Book();
		job.setPageable(book);

		if(!job.printDialog())
			return;

		job.print();
	} //}}}

	//{{{ Private members
	private static PageFormat format;
	//}}}
}
