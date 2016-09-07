
/*
 * PrintPreview.java
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


import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;

import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.swing.*;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.jEdit;


public class PrintPreview extends EnhancedDialog
{

	private View view;
	private Buffer buffer;
	private JComboBox<Integer> pages;
	private JButton nextPage;
	private JButton prevPage;
	private JButton zoomIn;
	private JButton zoomOut;
	private JButton fullWidth;
	private JButton fullPage;
	private PrintPreviewPane printPreviewPane;
	private JButton printButton;
	private JButton cancelButton;
	private HashMap<Integer, Range> pageRanges;
	private PrintRequestAttributeSet attributes;
	private PrintService printService;


	public PrintPreview( View view, Buffer buffer, PrintService printService, PrintRequestAttributeSet attributes )
	{
		super( view, jEdit.getProperty( "printpreview.dialog.title" ), true );
		this.view = view;
		this.buffer = buffer;
		this.printService = printService;
		this.attributes = attributes;
		installComponents();
		installListeners();
		init();
		pack();
		setLocationRelativeTo( jEdit.getActiveView().getTextArea() );
		setVisible( true );
	}


	private void installComponents()
	{

		// toolbar components
		pages = new JComboBox<Integer>();

		nextPage = new JButton( GUIUtilities.loadIcon( "22x22/actions/go-next.png" ) );
		nextPage.setToolTipText( jEdit.getProperty( "printpreview.dialog.nextPage", "Next Page" ) );

		prevPage = new JButton( GUIUtilities.loadIcon( "22x22/actions/go-previous.png" ) );
		prevPage.setToolTipText( jEdit.getProperty( "printpreview.dialog.prevPage", "Previous Page" ) );

		zoomIn = new JButton( GUIUtilities.loadIcon( "22x22/actions/zoom-in.png" ) );
		zoomIn.setToolTipText( jEdit.getProperty( "printpreview.dialog.zoomin", "Zoom In" ) );

		zoomOut = new JButton( GUIUtilities.loadIcon( "22x22/actions/zoom-out.png" ) );
		zoomOut.setToolTipText( jEdit.getProperty( "printpreview.dialog.zoomout", "Zoom Out" ) );

		// horisontal! yes, that's right
		fullWidth = new JButton( GUIUtilities.loadIcon( "22x22/actions/resize-horisontal.png" ) );
		fullWidth.setToolTipText( jEdit.getProperty( "printpreview.dialog.pageWidth", "Show page full width" ) );

		fullPage = new JButton( GUIUtilities.loadIcon( "22x22/actions/resize-vertical.png" ) );
		fullPage.setToolTipText( jEdit.getProperty( "printpreview.dialog.fullPage", "Show full page" ) );


		// create toolbar
		JPanel toolbar = new JPanel();
		toolbar.setLayout( new FlowLayout( FlowLayout.LEFT, 6, 6 ) );
		toolbar.add( new JLabel( "Page " ) );
		toolbar.add( pages );
		toolbar.add( prevPage );
		toolbar.add( nextPage );
		toolbar.add( zoomIn );
		toolbar.add( zoomOut );
		toolbar.add( fullWidth );
		toolbar.add( fullPage );

		// main area to see print preview
		printPreviewPane = new PrintPreviewPane();

		// print/close buttons
		printButton = new JButton( jEdit.getProperty( "printpreview.dialog.print", "Print" ) );
		cancelButton = new JButton( jEdit.getProperty( "common.cancel" ) );

		// create bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout( new FlowLayout( FlowLayout.RIGHT, 6, 11 ) );
		bottomPanel.add( printButton );
		bottomPanel.add( cancelButton );

		// main content holder
		JPanel content = new JPanel();
		content.setLayout( new BorderLayout() );
		content.setBorder( BorderFactory.createEmptyBorder( 6, 6, 6, 6 ) );

		// add all the pieces
		content.add( toolbar, BorderLayout.NORTH );
		content.add( printPreviewPane, BorderLayout.CENTER );
		content.add( bottomPanel, BorderLayout.SOUTH );

		setContentPane( content );
	}


	private void installListeners()
	{
		printButton.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					PrintPreview.this.ok();
				}
			} );

		cancelButton.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					PrintPreview.this.cancel();
				}
			} );

		pages.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedIndex = pages.getSelectedIndex();
					HashMap<Integer, Range> currentPage = new HashMap<Integer, Range>();
					currentPage.put( selectedIndex, pageRanges.get( selectedIndex ) );
					PrintPreviewModel model = new PrintPreviewModel( view, buffer, printService, attributes, currentPage );
					model.setPageNumber( selectedIndex );
					attributes.add( new PageRanges( (Integer)pages.getSelectedItem() ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		prevPage.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedIndex = pages.getSelectedIndex();
					if (selectedIndex <= 0)
					{
						selectedIndex = pageRanges.size() - 1;	
					}
					else
					{
						selectedIndex = selectedIndex - 1;	
					}
					pages.setSelectedIndex( selectedIndex );
				}
			}
		);
		nextPage.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedIndex = pages.getSelectedIndex();
					if (selectedIndex + 1 == pageRanges.size())
					{
						selectedIndex = 0;	
					}
					else
					{
						selectedIndex = selectedIndex + 1;	
					}
					pages.setSelectedIndex( selectedIndex );
				}
			}
		);
	}


	private void init()
	{
		pageRanges = BufferPrinter1_7.getPageRanges( view, buffer, attributes );
		DefaultComboBoxModel<Integer> pagesModel = new DefaultComboBoxModel<Integer>();
		for ( Integer i : pageRanges.keySet() )
		{
			pagesModel.addElement( i + 1 );
		}
		pages.setModel( pagesModel );

		HashMap<Integer, Range> currentPage = new HashMap<Integer, Range>();
		currentPage.put( 0, pageRanges.get( 0 ) );
		PrintPreviewModel model = new PrintPreviewModel( view, buffer, printService, attributes, currentPage );
		printPreviewPane.setModel( model );
	}


	public void ok()
	{
	}


	public void cancel()
	{
		PrintPreview.this.setVisible( false );
		PrintPreview.this.dispose();
	}
}
