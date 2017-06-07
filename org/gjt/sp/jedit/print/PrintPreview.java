
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
import org.gjt.sp.util.Log;


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
	private JButton cancelButton;
	private HashMap<Integer, Range> pageRanges;
	private PrintRequestAttributeSet attributes;
	private PrintService printService;
	private PrintPreviewModel model;
	private float zoomLevel = 1.0f;


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
		// TODO: this repaint shouldn't be necessary, but sometimes the first
		// page isn't drawn when the preview is first displayed. This fixes that
		// problem, but this feels like the wrong way to do it.
		repaint();
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

		// TODO: these need to be finished
		// toolbar.add( zoomIn );
		// toolbar.add( zoomOut );
		// toolbar.add( fullWidth );
		// toolbar.add( fullPage );
		// main area to see print preview
		printPreviewPane = new PrintPreviewPane();

		// print/close buttons
		cancelButton = new JButton( jEdit.getProperty( "common.cancel" ) );

		// create bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout( new FlowLayout( FlowLayout.RIGHT, 6, 11 ) );
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
		cancelButton.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					PrintPreview.this.cancel();
				}
			} );
		
		// as suggested by Alan, set the keystrokes so that up, down, left, right,
		// and page up and down go to the next or previous page as appropriate.
		// First remove the default key map:
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_DOWN, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_UP, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_RIGHT, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_LEFT, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_DOWN, 0, false ), "none" );
		pages.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_UP, 0, false ), "none" );
		
		// then handle the keystrokes:
		pages.addKeyListener( new KeyAdapter()
		{

				public void keyPressed( KeyEvent ke )
				{

					int selectedIndex = pages.getSelectedIndex();
					switch ( ke.getKeyCode() )
					{
						case KeyEvent.VK_DOWN:
						case KeyEvent.VK_KP_DOWN:
						case KeyEvent.VK_RIGHT:
						case KeyEvent.VK_KP_RIGHT:
						case KeyEvent.VK_PAGE_DOWN:
							selectedIndex += 1;
							selectedIndex = selectedIndex >= pages.getItemCount() ? 0 : selectedIndex;
							break;
						case KeyEvent.VK_UP:
						case KeyEvent.VK_KP_UP:
						case KeyEvent.VK_LEFT:
						case KeyEvent.VK_KP_LEFT:
						case KeyEvent.VK_PAGE_UP:
							selectedIndex -= 1;
							selectedIndex = selectedIndex < 0 ? pages.getItemCount() - 1 : selectedIndex;
							break;
						// pressing the 1 thru 9 keys jumps directly to that page
						case KeyEvent.VK_1:
							selectedIndex = 0;
							break;
						case KeyEvent.VK_2:
							selectedIndex = 1;
							break;
						case KeyEvent.VK_3:
							selectedIndex = 2;
							break;
						case KeyEvent.VK_4:
							selectedIndex = 3;
							break;
						case KeyEvent.VK_5:
							selectedIndex = 4;
							break;
						case KeyEvent.VK_6:
							selectedIndex = 5;
							break;
						case KeyEvent.VK_7:
							selectedIndex = 6;
							break;
						case KeyEvent.VK_8:
							selectedIndex = 7;
							break;
						case KeyEvent.VK_9:
							selectedIndex = 8;
							break;
							
						default:
							return;
					}
					

					pages.setSelectedIndex( selectedIndex );
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setPageNumber( selectedPage - 1 );
					model.setPageRanges( pageRanges );
					model.setZoomLevel( zoomLevel );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		
		pages.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent me)
				{
					if (model != null) 
					{
						int selectedPage = ( Integer )pages.getSelectedItem();
						model.setPageNumber( selectedPage - 1 );
						model.setPageRanges( pageRanges );
						model.setZoomLevel( zoomLevel );
						attributes.add( new PageRanges( selectedPage ) );
						printPreviewPane.setModel( model );
					}
				}
			});

		prevPage.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedIndex = pages.getSelectedIndex();
					if ( selectedIndex <= 0 )
					{
						selectedIndex = pages.getItemCount() - 1;
					}
					else
					{
						selectedIndex = selectedIndex - 1;
					}


					pages.setSelectedIndex( selectedIndex );
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setPageNumber( selectedPage - 1 );
					model.setPageRanges( pageRanges );
					model.setZoomLevel( zoomLevel );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		nextPage.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedIndex = pages.getSelectedIndex();
					if ( selectedIndex + 1 == pages.getItemCount() )
					{
						selectedIndex = 0;
					}
					else
					{
						selectedIndex = selectedIndex + 1;
					}


					pages.setSelectedIndex( selectedIndex );
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setPageNumber( selectedPage - 1 );
					model.setPageRanges( pageRanges );
					model.setZoomLevel( zoomLevel );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		zoomIn.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					zoomLevel += 0.1f;
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setZoomLevel( zoomLevel );
					model.setPageNumber( selectedPage - 1 );
					model.setPageRanges( pageRanges );
					model.setZoom( PrintPreviewModel.Zoom.IN );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		zoomOut.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					zoomLevel -= 0.1f;
					if ( zoomLevel <= 0.0f )
					{
						zoomLevel = 0.1f;
					}


					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setZoomLevel( zoomLevel );
					model.setPageNumber( selectedPage - 1 );
					model.setPageRanges( pageRanges );
					model.setZoom( PrintPreviewModel.Zoom.OUT );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		fullWidth.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setPageNumber( selectedPage );
					model.setPageRanges( pageRanges );
					model.setZoom( PrintPreviewModel.Zoom.WIDTH );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
		fullPage.addActionListener( new ActionListener()
		{

				public void actionPerformed( ActionEvent ae )
				{
					int selectedPage = ( Integer )pages.getSelectedItem();
					model.setPageNumber( selectedPage );
					model.setPageRanges( pageRanges );
					model.setZoom( PrintPreviewModel.Zoom.PAGE );
					attributes.add( new PageRanges( selectedPage ) );
					printPreviewPane.setModel( model );
				}
			}
		);
	}
	

	private void init()
	{
		pageRanges = BufferPrinter1_7.getPageRanges( view, buffer, attributes );
		DefaultComboBoxModel<Integer> pagesModel = new DefaultComboBoxModel<Integer>();
		boolean reverse = attributes.containsKey(Reverse.class);
		StringBuilder pr = new StringBuilder();
		for ( Integer i : pageRanges.keySet() )
		{
			Integer pageNo = reverse ? pageRanges.size() - i  + 1: i;
			pagesModel.addElement( pageNo );
			//Log.log(Log.DEBUG, this, "init, i = " + i + ", range = " + pageRanges.get(i));
			pr.append(i).append(',');
		}
		pr.deleteCharAt(pr.length() - 1);
		pages.setModel( pagesModel );
		pages.setSelectedIndex( 0 );

		nextPage.setEnabled( pagesModel.getSize() > 1 );
		prevPage.setEnabled( pagesModel.getSize() > 1 );

		model = new PrintPreviewModel( view, buffer, printService, attributes, pageRanges );
		int firstPage = ( Integer )pages.getSelectedItem();
		model.setPageNumber( firstPage - 1 );
		model.setPageRanges( pageRanges );
		model.setZoomLevel( zoomLevel );
		attributes.add( new PageRanges( firstPage ) );
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
