/*
 * BufferPrinter1_4.java - Main class that controls printing
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.io.*;
import javax.print.*;

import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.print.event.*;

import javax.swing.JOptionPane;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

public class BufferPrinter1_7
{
	//{{{ pageSetup() method
	/**
 	 * Shows the printer dialog with the page setup tab active, other tabs inactive.
 	 * @param view The parent view for the dialog.
 	 */
	public static void pageSetup(View view)
	{
		loadPrintSpec();
		PrinterDialog printerDialog = new PrinterDialog(view, format, true);
		if (!printerDialog.isCanceled()) 
		{
			format = printerDialog.getAttributes();
			savePrintSpec();
		}
	} //}}}

	//{{{ print() method
	public static void print(final View view, final Buffer buffer, boolean selection)
	{
		loadPrintSpec();
		String jobName = MiscUtilities.abbreviateView(buffer.getPath());
		format.add(new JobName(jobName, null));
		
		PrinterDialog printerDialog = new PrinterDialog(view, format, false);
		if (printerDialog.isCanceled())
		{
			return;
		}
		
		PrintService printService = printerDialog.getPrintService();
		OutputStream outputStream = null;
		if (printService != null) 
		{
			try 
			{
				job = printService.createPrintJob();
				job.addPrintJobListener(new BufferPrinter1_7.JobListener(view));
				format = printerDialog.getAttributes();
				savePrintSpec();
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(view, jEdit.getProperty("print-error.message", new String[]{e.getMessage()}), jEdit.getProperty("print-error.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		else
		{
			JOptionPane.showMessageDialog(view, jEdit.getProperty("print-error.message", new String[]{"Invalid print service."}), jEdit.getProperty("print-error.title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		BufferPrintable1_7 printable = new BufferPrintable1_7(format, view, buffer);
		printable.setReverse(printerDialog.getReverse());
		printable.setPrintRangeType(printerDialog.getPrintRangeType());
		Doc doc = new SimpleDoc(printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
		try {
			job.print(doc, format);
		}
		catch(PrintException e) {
			JOptionPane.showMessageDialog(view, jEdit.getProperty("print-error.message", new String[]{e.getMessage()}), jEdit.getProperty("print-error.title"), JOptionPane.ERROR_MESSAGE);
		}
		
	} //}}}

	//{{{ savePrintSpec() method
	private static void loadPrintSpec() 
	{
		format = new HashPrintRequestAttributeSet();

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			String printSpecPath = MiscUtilities.constructPath(
				settings, "printspec");
			File filePrintSpec = new File(printSpecPath);

			if (filePrintSpec.exists())
			{
				FileInputStream fileIn;
				ObjectInputStream obIn = null;
				try
				{
					fileIn = new FileInputStream(filePrintSpec);
					obIn = new ObjectInputStream(fileIn);
					format = (HashPrintRequestAttributeSet)obIn.readObject();
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,BufferPrinter1_4.class,e);
				} finally
				{
					try
					{
						if (obIn != null)
							obIn.close();
					} catch (IOException e) {}
				}
			}
		}
	} //}}}

	//{{{ savePrintSpec() method
	private static void savePrintSpec()
	{
		String settings = jEdit.getSettingsDirectory();
		if(settings == null)
			return;

		String printSpecPath = MiscUtilities.constructPath(settings, "printspec");
		File filePrintSpec = new File(printSpecPath);

		FileOutputStream fileOut;
		ObjectOutputStream obOut = null;
		try
		{
			fileOut = new FileOutputStream(filePrintSpec);
			obOut = new ObjectOutputStream(fileOut);
			obOut.writeObject(format);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		} finally {
			if(obOut != null)
				try
				{
					obOut.close();
				} catch (IOException e) {}
		}
	}
	//}}}
	
	static class JobListener extends PrintJobAdapter
	{
		
		private View view;
		
		public JobListener(View view)
		{
			this.view = view;	
		}
		
		@Override
		public void printJobCompleted(PrintJobEvent pje)
		{
			// if the print service is a "print to file" service, then need to 
			// flush and close the output stream.
			PrintService printService = pje.getPrintJob().getPrintService();
			if (printService instanceof StreamPrintService)
			{
				StreamPrintService streamService = (StreamPrintService)printService;
				OutputStream outputStream =	streamService.getOutputStream();
				try {
					outputStream.flush();
				}
				catch(Exception e) {
				}
				try {
					outputStream.close();
				}
				catch(Exception e) {
				}
			}
			view.getStatus().setMessageAndClear("Printing complete.");
		}
		
		@Override
		public void printJobFailed(PrintJobEvent pje)
		{
			JOptionPane.showMessageDialog(view, jEdit.getProperty("print-error.message", new String[]{"Print job failed."}), jEdit.getProperty("print-error.title"), JOptionPane.ERROR_MESSAGE);
		}
		
		@Override
		public void printJobRequiresAttention(PrintJobEvent pje)
		{
			JOptionPane.showMessageDialog(view, jEdit.getProperty("print-error.message", new String[]{"Check the printer."}), jEdit.getProperty("print-error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	//{{{ Private members
	private static PrintRequestAttributeSet format;
	private static DocPrintJob job;
	//}}}
}

