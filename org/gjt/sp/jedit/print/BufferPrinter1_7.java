/*
 * BufferPrinter1_7.java - Main class that controls printing
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


import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.HashMap;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.print.event.*;
import javax.swing.JOptionPane;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ThreadUtilities;


public class BufferPrinter1_7
{

	/**
	 * Shows the printer dialog with the page setup tab active, other tabs inactive.
	 * @param view The parent view for the dialog.
	 */
	public static void pageSetup( View view )
	{
		loadPrintSpec();
		PrinterDialog printerDialog = new PrinterDialog( view, format, true );
		if ( !printerDialog.isCanceled() )
		{
			format = printerDialog.getAttributes();
			savePrintSpec();
			EditBus.send(new PropertiesChanged(null));
		}
	}	

	// print to a printer
	public static void print( final View view, final Buffer buffer )
	{
		Log.log(Log.DEBUG, BufferPrinter1_7.class, "print buffer " + buffer.getPath());
		
		// load any saved printing attributes, these are put into 'format'
		loadPrintSpec();
		String jobName = MiscUtilities.abbreviateView( buffer.getPath() );
		format.add( new JobName( jobName, null ) );

		// show the print dialog so the user can make their printer settings
		PrinterDialog printerDialog = new PrinterDialog( view, format, false );
		if ( printerDialog.isCanceled() )
		{
			Log.log(Log.DEBUG, BufferPrinter1_7.class, "print dialog canceled");
			return;
		}
		
		// set up the print job
		PrintService printService = printerDialog.getPrintService();
		if ( printService != null )
		{
			//Log.log(Log.DEBUG, BufferPrinter1_7.class, "using print service: " + printService);
			try
			{
				job = printService.createPrintJob();
				job.addPrintJobListener( new BufferPrinter1_7.JobListener( view ) );
				format = printerDialog.getAttributes();
				savePrintSpec();
				EditBus.send(new PropertiesChanged(null));
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog( view, jEdit.getProperty( "print-error.message", new String[] {e.getMessage()} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
				return;
			}
		}
		else
		{
			JOptionPane.showMessageDialog( view, jEdit.getProperty( "print-error.message", new String[] {"Invalid print service."} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
			return;
		}


		// check if printing a selection, if so, create a new temporary buffer
		// containing just the selection
		// TODO: I'm not taking even/odd page setting into account here, nor am
		// I considering any page values that may have been set in the page range.
		// I don't think this is important for printing a selection, which is
		// generally just a few lines rather than pages. I could be wrong...
		Buffer tempBuffer = buffer;
		PrintRangeType printRangeType = (PrintRangeType)format.get(PrintRangeType.class);
		if ( PrintRangeType.SELECTION.equals(printRangeType) )
		{
			tempBuffer = getSelectionBuffer(view, buffer);
		}

		// copy the doc attributes from the print format attributes
		//Log.log(Log.DEBUG, BufferPrinter1_7.class, "--- print request attributes ---");
		DocAttributeSet docAttributes = new HashDocAttributeSet();
		Attribute[] attributes = format.toArray();
		for (Attribute attribute : attributes)
		{
			boolean isDocAttr = attribute instanceof DocAttribute;
			//Log.log(Log.DEBUG, BufferPrinter1_7.class, attribute.getName() + " = " + attribute + ", is doc attr? " + isDocAttr);
			if (isDocAttr)
			{
				docAttributes.add(attribute);	
			}
		}
		//Log.log(Log.DEBUG, BufferPrinter1_7.class, "--- end print request attributes ---");

		// set up the printable
		BufferPrintable1_7 printable = new BufferPrintable1_7( format, view, tempBuffer );

		final Doc doc = new SimpleDoc( printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, docAttributes );

		// ready to print
		// run this in a background thread, it can take some time for a large buffer
		Runnable runner = new Runnable()
		{

			public void run()
			{
				try
				{
					//Log.log(Log.DEBUG, this, "sending print job to printer");
					job.print( doc, format );
					//Log.log(Log.DEBUG, this, "printing complete");
				}
				catch ( PrintException e )
				{
					JOptionPane.showMessageDialog( view, jEdit.getProperty( "print-error.message", new String[] {e.getMessage()} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
				}
			}
		};
		ThreadUtilities.runInBackground( runner );
	}	//}}}
	
	// returns a temporary buffer containing only the lines in the current selection.
	// This also stores the line numbers of the selected lines as a buffer property
	// so they can be used for printing and print preview.
	private static Buffer getSelectionBuffer(View view, Buffer buffer)
	{
		int[] selectedLines = view.getTextArea().getSelectedLines();
		String path = buffer.getPath();
		String parent = path.substring(0, path.lastIndexOf(System.getProperty("file.separator")));
		Buffer temp = jEdit.openTemporary(view, parent, path + ".prn", true);
		temp.setMode(buffer.getMode());
		for (int i : selectedLines)
		{
			String line = buffer.getLineText(i) + '\n';
			temp.insert(temp.getLength(), line);
		}
		// save the line numbers of the selected lines so they can be used for 
		// printing and print preview
		temp.setProperty("printingLineNumbers", selectedLines);
		return temp;
	}
	
	/**
 	 * This is intended for use by the PrintPreview dialog.	
 	 */
	protected static void printPage( PrintPreviewModel model )
	{
		String jobName = MiscUtilities.abbreviateView( model.getBuffer().getPath() );
		PrintRequestAttributeSet attrs = model.getAttributes();
		attrs.add( new JobName( jobName, null ) );
		Reverse reverse = (Reverse)attrs.get(Reverse.class);
		if (reverse != null)
		{
			attrs.remove(Reverse.class);	
		}

		// set up the print job
		PrintService printService = model.getPrintService();
		if (printService == null)
		{
			printService = PrintServiceLookup.lookupDefaultPrintService();
		}
		if ( printService != null )
		{
			try
			{
				job = printService.createPrintJob();
			}
			catch ( Exception e )
			{
				JOptionPane.showMessageDialog( model.getView(), jEdit.getProperty( "print-error.message", new String[] {e.getMessage()} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
				return;
			}
		}
		else
		{
			JOptionPane.showMessageDialog( model.getView(), jEdit.getProperty( "print-error.message", new String[] {"Invalid print service."} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
			return;
		}


		// set up the printable to print just the requested pages
		Buffer buffer = model.getBuffer();
		PrintRangeType printRangeType = (PrintRangeType)attrs.get(PrintRangeType.class);
		if ( PrintRangeType.SELECTION.equals(printRangeType) )
		{
			buffer = getSelectionBuffer(model.getView(), buffer);
		}
		BufferPrintable1_7 printable = new BufferPrintable1_7( attrs, model.getView(), buffer );
		printable.setPages(model.getPageRanges());
		int pageNumber = model.getPageNumber(); 
		try 
		{
			printable.print(model.getGraphics(), model, pageNumber);
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
		if (reverse != null)
		{
			attrs.add(reverse);	
		}
	}


	/**
	 * This is intended for use by classes that need to know the page ranges
	 * of the buffer.
	 */
	public static HashMap<Integer, Range> getPageRanges( View view, Buffer buffer, PrintRequestAttributeSet attributes )
	{
		loadPrintSpec();
		format.addAll(attributes);
		BufferPrintable1_7 printable = new BufferPrintable1_7( format, view, buffer );
		return BufferPrinter1_7.getPageRanges( printable, format );
	}


	// have the printable calculate the pages and ranges, the map has the page
	// number as the key, a range containing the start and end line numbers of
	// that page
	private static HashMap<Integer, Range> getPageRanges( BufferPrintable1_7 printable, PrintRequestAttributeSet attributes )
	{
		PageFormat pageFormat = createPageFormat( attributes );
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		BufferedImage image = new BufferedImage(Double.valueOf(pageFormat.getImageableWidth()).intValue(), Double.valueOf(pageFormat.getImageableHeight()).intValue(), BufferedImage.TYPE_INT_RGB);
		Graphics graphics = ge.createGraphics(image);
		Paper paper = pageFormat.getPaper();
		Rectangle2D.Double clipRegion = new Rectangle2D.Double(paper.getImageableX(), paper.getImageableY(), paper.getImageableWidth(), paper.getImageableHeight());
		graphics.setClip(clipRegion);
		try 
		{
			// calculate which lines belong to each page
			HashMap<Integer, Range> pageLineRanges = printable.calculatePages( graphics, pageFormat );
			PageRanges pr = (PageRanges)attributes.get(PageRanges.class);
			if (pr == null) {
				pr = new PageRanges( 1, 1000 );
			}
			// then keep only the pages the user has selected
			HashMap<Integer, Range> newLineRanges = new HashMap<Integer, Range>();
			for (Integer i : pageLineRanges.keySet())
			{
				if (pr.contains(i))
				{
					newLineRanges.put(i, pageLineRanges.get(i));	
				}
			}
			return newLineRanges;
		}
		catch(Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static HashMap<Integer, Range> getCurrentPageRange( View view, Buffer buffer, PrintRequestAttributeSet attributes )
	{
		if (attributes == null)
		{
			loadPrintSpec();
			attributes = format;
		}
		
		BufferPrintable1_7 printable = new BufferPrintable1_7( attributes, view, buffer );
		HashMap<Integer, Range> pages = BufferPrinter1_7.getPageRanges( printable, attributes );
		HashMap<Integer, Range> answer = new HashMap<Integer, Range>();		
		int caretLine = view.getTextArea().getCaretLine();
		for (Integer i : pages.keySet())
		{
			Range range = pages.get(i);
			if (range.contains(caretLine))
			{
				answer.put(i, range);
				break;
			}
		}
		return answer;
	}
	
	public static PageFormat getDefaultPageFormat(PrintRequestAttributeSet attributes)
	{
		return BufferPrinter1_7.createPageFormat(attributes);
	}


	// create a page format using the values from the given attribute set
	private static PageFormat createPageFormat( PrintRequestAttributeSet attributes )
	{
		Paper paper = new Paper();
		MediaPrintableArea mpa = ( MediaPrintableArea )attributes.get( MediaPrintableArea.class );
		int units = MediaPrintableArea.INCH;
		double dpi = 72.0;		// Paper uses 72 dpi
		double x = ( double )mpa.getX( units ) * dpi;
		double y = ( double )mpa.getY( units ) * dpi;
		double w = ( double )mpa.getWidth( units ) * dpi;
		double h = ( double )mpa.getHeight( units ) * dpi;
		paper.setImageableArea( x, y, w, h );

		int orientation = PageFormat.PORTRAIT;
		OrientationRequested or = ( OrientationRequested )attributes.get( OrientationRequested.class );
		if ( OrientationRequested.LANDSCAPE.equals( or ) || OrientationRequested.REVERSE_LANDSCAPE.equals( or ) )
		{
			orientation = PageFormat.LANDSCAPE;
		}


		PageFormat pageFormat = new PageFormat();
		pageFormat.setPaper( paper );
		pageFormat.setOrientation( orientation );

		return pageFormat;
	}


	// {{{ loadPrintSpec() method
	// this finds a previously saved print attribute set in the settings directory,
	// or creates a new, empty attribute set if not found.
	private static void loadPrintSpec()
	{
		format = new HashPrintRequestAttributeSet();

		String settings = jEdit.getSettingsDirectory();
		if ( settings != null )
		{
			String printSpecPath = MiscUtilities.constructPath( settings, "printspec" );
			File filePrintSpec = new File( printSpecPath );

			if ( filePrintSpec.exists() )
			{
				FileInputStream fileIn;
				ObjectInputStream obIn = null;
				try
				{
					fileIn = new FileInputStream( filePrintSpec );
					obIn = new ObjectInputStream( fileIn );
					format = ( HashPrintRequestAttributeSet )obIn.readObject();
				}
				catch ( Exception e )
				{
					Log.log( Log.ERROR, BufferPrinter1_7.class, e );
				}
				finally
				{
					try

					{
						if ( obIn != null )
						{
							obIn.close();
						}
					}
					catch ( IOException e )	// NOPMD
					{
					}	
				}
			}
		}
		MediaPrintableArea mpa = ( MediaPrintableArea )format.get( MediaPrintableArea.class );
		if (mpa == null)
		{
			// assume US Letter size - why? Because I live in the US
			mpa = new MediaPrintableArea(0.5f, 0.5f, 10.0f, 7.5f, MediaPrintableArea.INCH);
			format.add(mpa);
		}
		format.remove(Reverse.class);
	}


	private static void savePrintSpec()
	{
		String settings = jEdit.getSettingsDirectory();
		if ( settings == null )
		{
			return;
		}


		String printSpecPath = MiscUtilities.constructPath( settings, "printspec" );
		File filePrintSpec = new File( printSpecPath );

		FileOutputStream fileOut;
		ObjectOutputStream objectOut = null;
		try
		{
			fileOut = new FileOutputStream( filePrintSpec );
			objectOut = new ObjectOutputStream( fileOut );
			objectOut.writeObject( format );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			if ( objectOut != null )
			{
				try
				{
					objectOut.flush();
				}
				catch ( IOException e )	// NOPMD
				{
				}	
				try
				{
					objectOut.close();
				}
				catch ( IOException e )	// NOPMD
				{
				}	
			}
		}
	}



	// print job listener, does clean up when the print job is complete and shows
	// the user any errors generated by the printing system
	static class JobListener extends PrintJobAdapter
	{

		private View view;


		public JobListener( View view )
		{
			this.view = view;
		}


		@Override
		public void printJobCompleted( PrintJobEvent pje )
		{

			// if the print service is a "print to file" service, then need to
			// flush and close the output stream.
			PrintService printService = pje.getPrintJob().getPrintService();
			if ( printService instanceof StreamPrintService )
			{
				StreamPrintService streamService = ( StreamPrintService )printService;
				OutputStream outputStream = streamService.getOutputStream();
				try
				{
					outputStream.flush();
				}
				catch ( Exception e )	// NOPMD
				{	
				}
				try
				{
					outputStream.close();
				}
				catch ( Exception e ) 	// NOPMD
				{	
				}
			}

			view.getStatus().setMessageAndClear( "Printing complete." );
		}


		@Override
		public void printJobFailed( PrintJobEvent pje )
		{
			JOptionPane.showMessageDialog( view, jEdit.getProperty( "print-error.message", new String[] {"Print job failed."} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
		}


		@Override
		public void printJobRequiresAttention( PrintJobEvent pje )
		{
			JOptionPane.showMessageDialog( view, jEdit.getProperty( "print-error.message", new String[] {"Check the printer."} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
		}
	}

	private static PrintRequestAttributeSet format;
	private static DocPrintJob job;
}
