
/*
 * BasicPrintPreviewPaneUI.java
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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;

import org.gjt.sp.jedit.gui.DropShadowBorder;


/**
 * Concrete implementation of a PrintPreviewPaneUI.
 */
public class BasicPrintPreviewPaneUI extends PrintPreviewPaneUI implements ChangeListener
{

	private PrintPreviewPane printPreviewPane = null;
	private JScrollPane scrollPane = null;
	private PrintPreviewRenderer printPreviewRenderer = null;


	/**
	 * Required by super class.
	 * @param c not used
	 * @return one of these
	 */
	public static ComponentUI createUI( JComponent c )
	{
		return new BasicPrintPreviewPaneUI();
	}


	/**
	 * Configures the specified component appropriate for the look and feel.
	 * This method is invoked when the <code>ComponentUI</code> instance is being installed
	 * as the UI delegate on the specified component.  This method should
	 * completely configure the component for the look and feel,
	 * including the following:
	 * <ol>
	 * <li>Install any default property values for color, fonts, borders,
	 *     icons, opacity, etc. on the component.  Whenever possible,
	 *     property values initialized by the client program should <i>not</i>
	 *     be overridden.
	 * </li><li>Install a <code>LayoutManager</code> on the component if necessary.
	 * </li><li>Create/add any required sub-components to the component.
	 * </li><li>Create/install event listeners on the component.
	 * </li><li>Create/install a <code>PropertyChangeListener</code> on the component in order
	 *     to detect and respond to component property changes appropriately.
	 * </li><li>Install keyboard UI (mnemonics, traversal, etc.) on the component.
	 * </li><li>Initialize any appropriate instance data.
	 * </li></ol>
	 * @param c The actual component.
	 */
	public void installUI( JComponent c )
	{
		printPreviewPane = ( PrintPreviewPane )c;

		installDefaults();
		installComponents();
		installListeners();
	}


	/**
	 * Install default values for colors, fonts, borders, etc.
	 */
	public void installDefaults()
	{
		printPreviewPane.setLayout( createLayoutManager() );
	}


	/**
	 * Create and install any sub-components.
	 */
	public void installComponents()
	{
		printPreviewRenderer = new PrintPreviewRenderer();
		scrollPane = new JScrollPane( printPreviewRenderer );
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		printPreviewPane.add( scrollPane, BorderLayout.CENTER );
	}


	/**
	 * Install any action listeners, mouse listeners, etc.
	 */
	public void installListeners()
	{
		printPreviewPane.addChangeListener( this );
	}


	/**
	 * Tear down and clean up.
	 */
	public void uninstallUI( JComponent c )
	{
		c.setLayout( null );
		uninstallListeners();
		uninstallComponents();
		uninstallDefaults();

		printPreviewPane = null;
	}


	public void uninstallDefaults()
	{
	}


	/**
	 * Tear down and clean up.
	 */
	public void uninstallComponents()
	{
		printPreviewRenderer = null;
	}


	public void uninstallListeners()
	{
		printPreviewPane.removeChangeListener( this );
	}


	public void stateChanged( ChangeEvent event )
	{
		if ( printPreviewRenderer != null )
		{
			printPreviewRenderer.setSize( printPreviewRenderer.getPreferredSize() );
			printPreviewRenderer.repaint();
		}
	}


	/**
	 * @return a BorderLayout
	 */
	protected LayoutManager createLayoutManager()
	{
		return new BorderLayout();
	}



	/**
	 * Panel to display the print preview.
	 */
	public class PrintPreviewRenderer extends JPanel
	{

		public PrintPreviewRenderer()
		{
			//setBorder( new DropShadowBorder() );
		}


		/**
		 * @return current paper size
		 */
		public Dimension getPaperSize()
		{

			// get the paper size set by the user
			PrintPreviewModel model = printPreviewPane.getModel();
			if ( model != null )
			{
				PrintRequestAttributeSet attributes = model.getAttributes();
				float zoomLevel = model.getZoomLevel();

				Media media = ( Media )attributes.get( Media.class );
				MediaSize mediaSize = null;
				if ( media instanceof MediaSizeName )
				{
					MediaSizeName name = ( MediaSizeName )media;
					mediaSize = MediaSize.getMediaSizeForName( name );
					int units = MediaPrintableArea.INCH;
					float dpi = 72 * zoomLevel;
					float paperWidth = mediaSize.getX( units ) * dpi;
					float paperHeight = mediaSize.getY( units ) * dpi;
					Dimension newSize = new Dimension();
					newSize.setSize( paperWidth, paperHeight );
					OrientationRequested orientationRequested = ( OrientationRequested )attributes.get( OrientationRequested.class );
					if ( OrientationRequested.LANDSCAPE.equals( orientationRequested ) || OrientationRequested.REVERSE_LANDSCAPE.equals( orientationRequested ) )
					{
						if ( paperWidth < paperHeight )
						{
							newSize.setSize( newSize.getHeight(), newSize.getWidth() );
						}
					}


					return newSize;
				}


				// otherwise, use the default paper size
				Paper paper = model.getPaper();
				Dimension defaultSize = new Dimension( Double.valueOf( paper.getWidth() * zoomLevel ).intValue(), Double.valueOf( paper.getHeight() * zoomLevel ).intValue() );
				return defaultSize;
			}


			return getSize();
		}


		// make the print preview be no larger than the view size
		@Override
		public Dimension getPreferredSize()
		{
			return getPaperSize();
		}


		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}


		public void paintComponent( Graphics gfx )
		{
			PrintPreviewModel model = printPreviewPane.getModel();
			if ( model == null )
			{
				return;
			}


			super.paintComponent( gfx );

			Dimension currentSize = getPaperSize();
			double width = currentSize.getWidth();
			double height = currentSize.getHeight();

			// paint background white
			gfx.setColor( Color.WHITE );
			gfx.fillRect( 0, 0, Double.valueOf( width ).intValue(), Double.valueOf( height ).intValue() );

			// print the page into this panel
			updateModel();
			model.setGraphics( gfx );
			BufferPrinter1_7.printPage( model );

			scrollPane.revalidate();
			printPreviewPane.revalidate();
			printPreviewPane.repaint();
		}


		private void updateModel()
		{
			PrintPreviewModel pageFormat = printPreviewPane.getModel();
			if ( pageFormat == null )
			{
				return;
			}


			// get the printable area from the attributes
			PrintRequestAttributeSet attributes = pageFormat.getAttributes();
			float zoomLevel = pageFormat.getZoomLevel();
			MediaPrintableArea mpa = ( MediaPrintableArea )attributes.get( MediaPrintableArea.class );
			int units = MediaPrintableArea.INCH;
			double dpi = 72.0 * zoomLevel;	// Paper uses 72 dpi
			double x = ( double )mpa.getX( units ) * dpi;
			double y = ( double )mpa.getY( units ) * dpi;
			double w = ( double )mpa.getWidth( units ) * dpi;
			double h = ( double )mpa.getHeight( units ) * dpi;

			// apply the mpa dimensions to the paper and page format
			Paper paper = new Paper();
			Dimension paperSize = getPaperSize();
			paper.setSize( paperSize.getWidth(), paperSize.getHeight() );
			int orientation = PageFormat.PORTRAIT;
			OrientationRequested or = ( OrientationRequested )attributes.get( OrientationRequested.class );
			if ( OrientationRequested.LANDSCAPE.equals( or ) )
			{
				paper.setSize( paperSize.getHeight(), paperSize.getWidth() );
				orientation = PageFormat.LANDSCAPE;
			}
			else
			if ( OrientationRequested.REVERSE_LANDSCAPE.equals( or ) )
			{
				paper.setSize( paperSize.getHeight(), paperSize.getWidth() );
				orientation = PageFormat.REVERSE_LANDSCAPE;
			}


			paper.setImageableArea( x, y, w, h );
			pageFormat.setPaper( paper );
			pageFormat.setOrientation( orientation );
		}
	}
}
