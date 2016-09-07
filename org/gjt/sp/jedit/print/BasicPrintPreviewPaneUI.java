
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
import java.awt.Rectangle;
import java.awt.print.Paper;

import javax.swing.BorderFactory;
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
        printPreviewPane.add( scrollPane, BorderLayout.CENTER );
        printPreviewPane.repaint();
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


    // TODO: not used, remove the change listener stuff?
    public void stateChanged( ChangeEvent event )
    {
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
            setBorder( new DropShadowBorder());
        }


        /**
         * @return current paper size
         */
        public Dimension getPreferredSize()
        {
            PrintPreviewModel model = printPreviewPane.getModel();
            Paper paper = model.getPaper();
            return new Dimension( new Double( paper.getWidth() ).intValue(), new Double( paper.getHeight() ).intValue() );
        }


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


            // set size to paper size
            // TODO: adjust for zoom level, add zoom to model
            Paper paper = model.getPaper();
            int width = new Double( paper.getWidth() ).intValue();
            int height = new Double( paper.getHeight() ).intValue();
            setSize( width, height );
            super.paintComponent( gfx );

            
            // print the page into this panel
            BufferPrinter1_7.printPage( model.getView(), model.getBuffer(), gfx, model.getPrintService(), model.getAttributes(), model.getPageNumber(), model.getPageRanges() );

            scrollPane.revalidate();
            printPreviewPane.revalidate();
        }
    }
}
