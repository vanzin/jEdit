
/*
 * PrinterDialog.java
 *
 * Copyright (C) 2016 Dale Anson
 * Portions Copyright 2000-2007 Sun Microsystems, Inc.
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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.gui.NumericTextField;
import org.gjt.sp.jedit.gui.VariableGridLayout;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.util.Log;

// Technical guide on the Java printing system:
// https://docs.oracle.com/javase/7/docs/technotes/guides/jps/spec/JPSTOC.fm.html
public class PrinterDialog extends JDialog implements ListSelectionListener
{

    private View view;
    private String jobName = null;
    private PrintService selectedPrintService = null;
    private PrintRequestAttributeSet attributes;
    private JTabbedPane tabs;
    private JList<PrintService> printers;
    private JSpinner copies = null;
    private JComboBox<String> paperSize;
    private List<Media> paperSizes;
    private JComboBox<Priority> priority;
    private JComboBox<Finishings> finishing;
    private JComboBox<Sides> sides;
    private JComboBox<NumberUp> pagesPerSide;
    private JComboBox<PresentationDirection> pageOrdering;
    private JComboBox<MediaTray> paperSource;
    private JComboBox<OrientationRequested> orientation;
    private boolean pageSetupOnly;
    private boolean canceled = false;
    private Map<String, String> messageMap;
    private PageSetupPanel pageSetupPanel;
    public static int onlyPrintPages = PrintRangeType.ALL.getValue();
    private DocFlavor DOC_FLAVOR = DocFlavor.SERVICE_FORMATTED.PRINTABLE;


    public PrinterDialog( View owner, PrintRequestAttributeSet attributes, boolean pageSetupOnly )
    {
        super( owner, Dialog.ModalityType.APPLICATION_MODAL );
        try
        {
            view = owner;
            this.pageSetupOnly = pageSetupOnly;
            setTitle( pageSetupOnly ? jEdit.getProperty( "print.dialog.pageSetupTitle" ) : jEdit.getProperty( "print.dialog.title" ) );

            if ( attributes != null )
            {
                this.attributes = new HashPrintRequestAttributeSet( attributes );
            }
            else
            {
                this.attributes = new HashPrintRequestAttributeSet();
            }


            Attribute jobNameAttr = this.attributes.get( JobName.class );
            if ( jobNameAttr != null )
            {
                jobName = jobNameAttr.toString();
            }


            this.attributes.remove( Destination.class );
            

            // for debugging
            /* Attribute[] attrs = attributes.toArray();
             * for ( Attribute a : attrs )
             * {
             * Log.log( Log.DEBUG, this, "+++++ before: " + a.getName() + " = " + a );
             * }
             */
            initMessages();

            JPanel contents = new JPanel( new BorderLayout() );
            contents.setBorder( BorderFactory.createEmptyBorder( 11, 11, 12, 12 ) );

            tabs = new JTabbedPane();
            tabs.setBorder( BorderFactory.createEmptyBorder( 0, 0, 11, 0 ) );
            tabs.add( jEdit.getProperty( "print.dialog.General", "General" ), new GeneralPanel() );
            tabs.add( jEdit.getProperty( "print.dialog.Page_Setup", "Page Setup" ), pageSetupPanel = new PageSetupPanel() );
            tabs.add( jEdit.getProperty( "print.dialog.Job", "Job" ), new JobPanel() );
            tabs.add( jEdit.getProperty( "print.dialog.Advanced", "Advanced" ), new AdvancedPanel() );
            tabs.add( jEdit.getProperty( "print.dialog.jEdit", "jEdit" ), new jEditPanel() );
            if ( pageSetupOnly )
            {
                tabs.setSelectedIndex( 1 );
                tabs.setEnabledAt( 0, false );
                tabs.setEnabledAt( 1, true );
                tabs.setEnabledAt( 2, false );
                tabs.setEnabledAt( 3, false );
                tabs.setEnabledAt( 4, false );
            }


            contents.add( tabs, BorderLayout.CENTER );

            JButton previewButton = new JButton( jEdit.getProperty( "print.dialog.preview", "Preview" ) );
            previewButton.addActionListener( getPreviewButtonListener() );

            JButton okButton = new JButton( jEdit.getProperty( "common.ok" ) );
            okButton.addActionListener( getOkButtonListener() );

            JButton cancelButton = new JButton( jEdit.getProperty( "common.cancel" ) );
            cancelButton.addActionListener( getCancelButtonListener() );

            JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 6, 6 ) );
            GenericGUIUtilities.makeSameSize( previewButton, okButton, cancelButton );
            buttonPanel.add( previewButton );
            buttonPanel.add( okButton );
            buttonPanel.add( cancelButton );
            contents.add( buttonPanel, BorderLayout.SOUTH );
            
            setContentPane( contents );

            // auto-select the default printer
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            if ( defaultPrintService != null )
            {
                printers.setSelectedValue( defaultPrintService, true );
            }
            else
            {
                printers.setSelectedIndex( 0 );
            }


            // loads some default values if needed
            valueChanged( null );

            // set margin values, need to do this here after the other values have been set
            pageSetupPanel.setDefaultMargins();

            pack();

            // ESC key closes dialog
            getRootPane().registerKeyboardAction(e -> {
                PrinterDialog.this.setVisible(false);
                PrinterDialog.this.dispose();
                canceled = true;
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

            setLocationRelativeTo( jEdit.getActiveView().getTextArea() );
            setVisible( true );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    private ActionListener getPreviewButtonListener()
    {
        return new ActionListener()
        {

            public void actionPerformed( ActionEvent ae )
            {

                // check margins and so on
                String checkMarginsMessage = pageSetupPanel.recalculate();
                if ( checkMarginsMessage != null )
                {
                    JOptionPane.showMessageDialog( PrinterDialog.this, checkMarginsMessage, jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
                    return;
                }


                // gather all the attributes from the tabs
                for ( int i = 0; i < tabs.getTabCount(); i++ )
                {
                    PrinterPanel panel = ( PrinterPanel )tabs.getComponentAt( i );
                    AttributeSet panelAttributes = panel.getAttributes();
                    if (panelAttributes != null)
                    {
                        PrinterDialog.this.attributes.addAll( panelAttributes );
                    }
                }

                // adjust the print range to filter based on odd/even pages
                PageRanges pr = ( PageRanges )PrinterDialog.this.attributes.get( PageRanges.class );
                try
                {
                    PageRanges mergedRanges = mergeRanges( pr );
                    if (mergedRanges != null) 
                    {
                        PrinterDialog.this.attributes.add( mergedRanges );
                    }
                }
                catch ( PrintException e )
                {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog( PrinterDialog.this, jEdit.getProperty( "print-error.message", new String[] {e.getMessage()} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
                    return;
                }

                new PrintPreview( view, view.getBuffer(), PrinterDialog.this.getPrintService(), PrinterDialog.this.attributes );
            }
        };
    }


    private ActionListener getOkButtonListener()
    {
        return new ActionListener()
        {

            public void actionPerformed( ActionEvent ae )
            {

                // check margins and so on
                String checkMarginsMessage = pageSetupPanel.recalculate();
                if ( checkMarginsMessage != null )
                {
                    JOptionPane.showMessageDialog( PrinterDialog.this, checkMarginsMessage, jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
                    return;
                }


                // gather all the attributes from the tabs
                for ( int i = 0; i < tabs.getTabCount(); i++ )
                {
                    PrinterPanel panel = ( PrinterPanel )tabs.getComponentAt( i );
                    AttributeSet panelAttributes = panel.getAttributes();
                    if (panelAttributes != null)
                    {
                        PrinterDialog.this.attributes.addAll( panelAttributes );
                    }
                }

                // adjust the print range to filter based on odd/even pages
                PageRanges pr = ( PageRanges )PrinterDialog.this.attributes.get( PageRanges.class );
                try
                {
                    PageRanges mergedRanges = mergeRanges( pr );
                    if (mergedRanges != null) 
                    {
                        PrinterDialog.this.attributes.add( mergedRanges );
                    }
                }
                catch ( PrintException e )
                {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog( PrinterDialog.this, jEdit.getProperty( "print-error.message", new String[] {e.getMessage()} ), jEdit.getProperty( "print-error.title" ), JOptionPane.ERROR_MESSAGE );
                    return;
                }

                // if printing to a file, get the filename to use
                if ( !pageSetupOnly && getPrintService() instanceof StreamPrintService )
                {

                    // create default filename
                    String filename = "out";
                    if ( jobName != null )
                    {
                        File f = new File( jobName );
                        filename = f.getName();
                    }


                    filename = new StringBuilder( filename ).append( ".ps" ).toString();

                    File initialFile = new File( System.getProperty( "user.home" ), filename );

                    // show file chooser
                    String[] files = GUIUtilities.showVFSFileDialog( PrinterDialog.this, view, initialFile.getAbsolutePath(), VFSBrowser.SAVE_DIALOG, false );
                    if (files.length > 0)
                    {
                        File file = new File( files[0] );
                        selectedPrintService = getPostscriptPrintService( file );
                    }
                    else
                    {
                        return;
                    }
                }


                // for debugging
                /*
                 * Attribute[] attrs = PrinterDialog.this.attributes.toArray();
                 * for ( Attribute a : attrs )
                 * {
                 * Log.log( Log.DEBUG, this, "+++++ after: " + a.getName() + " = " + a );
                 * }
                 */
                PrinterDialog.this.setVisible( false );
                PrinterDialog.this.dispose();
            }
        };
    }


    private ActionListener getCancelButtonListener()
    {
        return new ActionListener()
        {

            public void actionPerformed( ActionEvent ae )
            {
                PrinterDialog.this.setVisible( false );
                PrinterDialog.this.dispose();
                canceled = true;
            }
        };
    }


    /**
     * @return The print service selected by the user.
     */
    public PrintService getPrintService()
    {
        return selectedPrintService;
    }


    /**
     * @return An attribute set containing all the values as selected by the user.
     */
    public PrintRequestAttributeSet getAttributes()
    {
        return attributes;
    }


    /**
     * @return <code>true</code> if the user clicked the 'cancel' button.
     */
    public boolean isCanceled()
    {
        return canceled;
    }


    private PrintService[] getPrintServices()
    {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices( DOC_FLAVOR, null );
        List<PrintService> services = new ArrayList<PrintService>( Arrays.asList( printServices ) );
        PrintService service = getPostscriptPrintService( null );
        if ( service != null )
        {
            services.add( service );
        }


        printServices = services.toArray( new PrintService [0]  );
        return printServices;
    }


    private StreamPrintService getPostscriptPrintService( File outfile )
    {
        if ( outfile == null )
        {
            outfile = new File( System.getProperty( "user.home" ), "out.ps" );
        }


        String mimetype = "application/postscript";
        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories( DOC_FLAVOR, mimetype );

        FileOutputStream fos;
        StreamPrintService printService = null;
        if ( factories.length > 0 )
        {
            try


            {
                fos = new FileOutputStream( outfile );
                printService = factories[0].getPrintService( fos );
            }
            catch ( Exception e )
            {
                return null;
            }
        }


        return printService;
    }


    // finds the intersection of the pages selected in the General tab (All pages
    // or a page range) with the all, odd, or even setting for the 'only print'
    // setting in the Page Setup tab.
    private PageRanges mergeRanges( PageRanges pr ) throws PrintException
    {
        if ( pr == null || onlyPrintPages == PrintRangeType.ALL.getValue() )
        {
            return pr;
        }


        List<Integer> pages = new ArrayList<Integer>();
        int[][] ranges = pr.getMembers();
        for ( int i = 0; i < ranges.length; i++ )
        {
            int[] range = ranges[i];
            int start = range[0];

            // this limits printing to the first 250 pages. If the user selects 'All pages'
            // from the General tab, then the range is 1 to Integer.MAX_VALUE, so to print just
            // the even or odd numbered pages would need an array of 1073741823 values, which
            // is unreasonable.
            int end = range.length == 1 ? range[0] : Math.min( range[0] + 500, range[1] );
            for ( int pageIndex = start; pageIndex <= end; pageIndex++ )
            {
                if ( pageIndex % 2 == 0 && onlyPrintPages == PrintRangeType.EVEN.getValue() )
                {
                    pages.add( pageIndex );
                }
                else
                if ( pageIndex % 2 == 1 && onlyPrintPages == PrintRangeType.ODD.getValue() )
                {
                    pages.add( pageIndex );
                }
            }
        }
        if ( pages.isEmpty() )
        {
            throw new PrintException( "No pages are selected to print.\nPlease check the 'Range' setting on the General tab and\nthe 'Only print' setting on the Page Setup tab." );
        }


        StringBuilder sb = new StringBuilder();
        for ( Integer page : pages )
        {
            sb.append( page ).append( ',' );
        }
        sb.deleteCharAt( sb.length() - 1 );
        return new PageRanges( sb.toString() );
    }


    @SuppressWarnings( {"unchecked"} )
    public void valueChanged( ListSelectionEvent e )
    {
        selectedPrintService = printers.getSelectedValue();

        // get the supported attribute categories and values
        Map<Class, Object> categoryValueMap = new HashMap<Class, Object>();
        Class<?>[] classes = selectedPrintService.getSupportedAttributeCategories();
        for ( Class c : classes )
        {
            Object values = selectedPrintService.getSupportedAttributeValues( c, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null );
            categoryValueMap.put( c, values );
        }

        // set range for number of copies
        SpinnerNumberModel copiesModel = new SpinnerNumberModel( 1, 1, 999, 1 );
        Object value = categoryValueMap.get( Copies.class );
        if ( value != null )
        {
            String maxCopies = value.toString();
            if ( maxCopies.indexOf( '-' ) > 0 )
            {
                maxCopies = maxCopies.substring( maxCopies.indexOf( '-' ) + 1 );
            }


            copiesModel = new SpinnerNumberModel( 1, 1, ( int )Integer.valueOf( maxCopies ), 1 );
        }


        copies.setModel( copiesModel );

        // paper sizes
        value = categoryValueMap.get( Media.class );
        Set<MediaSizeName> sizeNames = new HashSet<MediaSizeName>();
        for ( Media m : ( Media[] )value )
        {
            if ( m instanceof MediaSizeName )
            {
                sizeNames.add( ( MediaSizeName )m );
            }
        }
        MediaSizeName[] sizes = sizeNames.toArray( new MediaSizeName [sizeNames.size()]  );
        Arrays.sort( sizes, new Comparator<MediaSizeName>()
        {

            public int compare( MediaSizeName a, MediaSizeName b )
            {
                return a.toString().compareTo( b.toString() );
            }
        } );
        Media previousPaper = ( Media )attributes.get( Media.class );
        MediaSizeName previousSize = null;
        if ( previousPaper instanceof MediaSizeName )
        {
            previousSize = ( MediaSizeName )previousPaper;
        }

        if (paperSize != null)
        {
            String[] paperNames = new String [sizes.length];
            paperSizes = new ArrayList<Media>();
            int index = -1;
            int letterSizeIndex = 0;
            for ( int i = 0; i < sizes.length; i++ )
            {
                MediaSizeName m = sizes[i];
                if ( MediaSizeName.NA_LETTER.equals( m ) )
                {
                    letterSizeIndex = i;
                }
                else
                if ( m.equals( previousSize ) )
                {
                    index = i;
                }
    
    
                paperSizes.add( m );
                paperNames[i] = getMessage( m.toString() );
            }
            index = index == -1 ? letterSizeIndex : index;
            paperSize.setModel( new DefaultComboBoxModel<String>( paperNames ) );
            paperSize.setEnabled( true );
            paperSize.setSelectedIndex( index );
        }
        
        // finishing
        if (finishing != null) 
        {
            value = categoryValueMap.get( Finishings.class );
            if ( value == null )
            {
                finishing.setModel( new DefaultComboBoxModel<Finishings>() );
                finishing.setEnabled( false );
            }
            else
            {
                Finishings[] finishings = ( Finishings[] )value;
                if ( finishings.length == 0 || ( finishings.length == 1 && Finishings.NONE.equals( finishings[0] ) ) )
                {
                    finishing.setModel( new DefaultComboBoxModel<Finishings>() );
                    finishing.setEnabled( false );
                }
                else
                {
                    finishing.setModel( new DefaultComboBoxModel<Finishings>( finishings ) );
                    finishing.setEnabled( true );
                }
            }
        }


        // sides
        if (sides != null)
        {
            value = categoryValueMap.get( Sides.class );
            if ( value == null )
            {
                sides.setEnabled( false );
            }
            else
            {
                sides.setModel( new DefaultComboBoxModel<Sides>( ( Sides[] )value ) );
                Sides previousSides = ( Sides )attributes.get( Sides.class );
                sides.setSelectedItem( previousSides == null ? Sides.ONE_SIDED : previousSides );
                sides.setEnabled( true );
            }
        }


        // pages per side
        if (pagesPerSide != null)
        {
            value = categoryValueMap.get( NumberUp.class );
            if ( value == null )
            {
                pagesPerSide.setEnabled( false );
            }
            else
            {
                NumberUp[] numberUp = ( NumberUp[] )value;
                Arrays.sort( numberUp, new Comparator<NumberUp>()
                {
    
                    public int compare( NumberUp a, NumberUp b )
                    {
                        int m = a.getValue();
                        int n = b.getValue();
                        if ( m < n )
                        {
                            return -1;
                        }
                        else
                        if ( m == n )
                        {
                            return 0;
                        }
                        else
                        {
                            return 1;
                        }
                    }
                } );
                pagesPerSide.setModel( new DefaultComboBoxModel<NumberUp>( numberUp ) );
                pagesPerSide.setEnabled( true );
            }
        }


        // ordering of pages per side
        if (pageOrdering != null) 
        {
            value = categoryValueMap.get( PresentationDirection.class );
            if ( value == null )
            {
                pageOrdering.setEnabled( false );
            }
            else
            {
                PresentationDirection[] po = ( PresentationDirection[] )value;
                pageOrdering.setModel( new DefaultComboBoxModel<PresentationDirection>( po ) );
                pageOrdering.setEnabled( true );
            }
        }


        // paper source tray
        if (paperSource != null)
        {
            value = categoryValueMap.get( Media.class );
            if ( value == null )
            {
                paperSource.setEnabled( false );
            }
            else
            {
                Set<MediaTray> trayNames = new HashSet<MediaTray>();
                for ( Media m : ( Media[] )value )
                {
                    if ( m instanceof MediaTray )
                    {
                        trayNames.add( ( MediaTray )m );
                    }
                }
                if ( trayNames.size() > 0 )
                {
                    MediaTray[] trays = trayNames.toArray( new MediaTray [trayNames.size()]  );
                    paperSource.setModel( new DefaultComboBoxModel<MediaTray>( trays ) );
                    paperSource.setEnabled( true );
                    MediaTray lastUsedTray = (MediaTray)attributes.get(MediaTray.class);
                    paperSource.setSelectedItem(lastUsedTray == null ? trays[0] : lastUsedTray);
                }
                else
                {
                    paperSource.setEnabled( false );
                }
            }
        }


        // orientation, eg. portrait or landscape
        if (orientation != null)
        {
            value = categoryValueMap.get( OrientationRequested.class );
            if ( value == null )
            {
                orientation.setEnabled( false );
            }
            else
            {
                OrientationRequested[] or = ( OrientationRequested[] )value;
                orientation.setModel( new DefaultComboBoxModel<OrientationRequested>( or ) );
                orientation.setEnabled( true );
                OrientationRequested previousOrientation = ( OrientationRequested )attributes.get( OrientationRequested.class );
                orientation.setSelectedItem( previousOrientation == null ? OrientationRequested.PORTRAIT : previousOrientation );
            }
        }
    }



    private abstract class PrinterPanel extends JPanel
    {

        public PrinterPanel()
        {
            super( new BorderLayout() );
            setBorder( BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) );
        }


        abstract AttributeSet getAttributes()
        ;
    }



    private class GeneralPanel extends PrinterPanel
    {

        JRadioButton allPages;
        JRadioButton pages;
        JRadioButton currentPage;
        JRadioButton selection;
        JCheckBox collate;
        JCheckBox reverse;
        JTextField pagesField;


        // DONE: current page and selection are not implemented yet. Note there
        // are no standard printer attributes to specify either of these, so I
        // added the PrintRangeType attribute to handle these.
        public GeneralPanel()
        {
            super();
            printers = new JList<PrintService>( getPrintServices() );
            printers.setCellRenderer( new PrintServiceCellRenderer() );
            printers.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

            JPanel rangePanel = new JPanel( new GridLayout( 4, 2, 6, 6 ) );
            rangePanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Range", "Range" ) ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            allPages = new JRadioButton( jEdit.getProperty( "print.dialog.All_pages", "All pages" ) );
            allPages.setSelected( true );

            pages = new JRadioButton( jEdit.getProperty( "print.dialog.Pages", "Pages" ) + ':' );
            pagesField = new JTextField();
            pagesField.setEnabled( false );

            currentPage = new JRadioButton( jEdit.getProperty( "print.dialog.Current_page", "Current page" ) );
            selection = new JRadioButton( jEdit.getProperty( "print.dialog.Selection", "Selection" ) );

            new MyButtonGroup( allPages, pages, currentPage, selection );
            Box pagesBox = Box.createHorizontalBox();
            pagesBox.add( pages );
            pagesBox.add( Box.createHorizontalStrut( 6 ) );
            pagesBox.add( pagesField );
            rangePanel.add( allPages );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( pagesBox );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( currentPage );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( selection );

            JPanel copiesPanel = new JPanel( new GridLayout( 3, 2, 6, 6 ) );
            copiesPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Copies", "Copies" ) ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            JLabel copiesLabel = new JLabel( jEdit.getProperty( "print.dialog.Copies", "Copies" + ':' ) );
            copies = new JSpinner( new SpinnerNumberModel( 1, 1, 999, 1 ) );
            collate = new JCheckBox( jEdit.getProperty( "print.dialog.Collate", "Collate" ) );
            collate.setSelected( false );
            collate.setEnabled( false );

            reverse = new JCheckBox( jEdit.getProperty( "print.dialog.Reverse", "Reverse" ) );
            reverse.setSelected( false );
            reverse.setEnabled( true );

            copiesPanel.add( copiesLabel );
            copiesPanel.add( copies );
            copiesPanel.add( collate );
            copiesPanel.add( Box.createGlue() );
            copiesPanel.add( reverse );

            JPanel content = new JPanel( new BorderLayout() );
            JPanel top = new JPanel( new BorderLayout() );
            JPanel bottom = new JPanel( new GridLayout( 1, 2, 6, 6 ) );

            top.add( new JScrollPane( printers ), BorderLayout.CENTER );
            bottom.add( rangePanel );
            bottom.add( copiesPanel );
            content.add( top, BorderLayout.CENTER );
            content.add( bottom, BorderLayout.SOUTH );
            add( content );
            
            // install listeners
            printers.addListSelectionListener( PrinterDialog.this );
            allPages.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        pagesField.setEnabled( pages.isSelected() );
                    }
                }
            );
            pages.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        pagesField.setEnabled( pages.isSelected() );
                    }
                }
            );
            copies.addChangeListener( new ChangeListener()
            {

                    public void stateChanged( ChangeEvent e )
                    {
                        JSpinner spinner = ( JSpinner )e.getSource();
                        int value = ( int )spinner.getValue();
                        collate.setEnabled( value > 1 );
                        collate.setSelected( value > 1 );
                    }
                } );
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            // choose last used printer first, default printer if no last used, or first
            // item in print service list otherwise
            String lastUsedPrinterName = jEdit.getProperty("print.lastUsedPrinter");
            if (lastUsedPrinterName != null) 
            {
                ListModel<PrintService> lm = printers.getModel();
                for (int i = 0; i < lm.getSize(); i++)
                {
                    PrintService ps = lm.getElementAt(i);
                    if (lastUsedPrinterName.equals(ps.getName()))
                    {
                        printers.setSelectedValue(ps, true);
                        selectedPrintService = ps;
                        break;
                    }
                }
            }
            else if (defaultPrintService != null)
            {
                printers.setSelectedValue(defaultPrintService, true);      
                selectedPrintService = defaultPrintService;
            }
            if (selectedPrintService == null) 
            {
                selectedPrintService = printers.getModel().getElementAt( 0 );
            }
        }


        public AttributeSet getAttributes()
        {
            jEdit.setProperty("print.lastUsedPrinter", printers.getSelectedValue().getName());
            
            AttributeSet as = new HashAttributeSet();

            if ( allPages.isSelected() )
            {
                as.add( new PageRanges( 1, 1000 ) );
                as.add( PrintRangeType.ALL );
            }
            else
            if ( pages.isSelected() )
            {
                String pageRange = pagesField.getText();
                if ( pageRange != null )
                {
                    try
                    {
                        as.add( new PageRanges( pageRange ) );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
                as.add( PrintRangeType.RANGE );
            }
            else
            if ( currentPage.isSelected() )
            {
                PrinterDialog.this.attributes.add(new PageRanges( 1, 1000 ) );
                HashMap<Integer, Range> currentPageRange = BufferPrinter1_7.getCurrentPageRange(view, view.getBuffer(), PrinterDialog.this.attributes);
                int page = 1;
                if (currentPageRange != null && !currentPageRange.isEmpty())
                {
                    page = currentPageRange.keySet().iterator().next();
                }
                
                as.add( new PageRanges( page ) );
                as.add( PrintRangeType.CURRENT_PAGE );
            }
            else
            if ( selection.isSelected() )
            {
                PrinterDialog.this.attributes.add(new PageRanges( 1, 1000 ) );
                as.add( PrintRangeType.SELECTION );
            }


            if ( collate.isSelected() )
            {
                as.add( SheetCollate.COLLATED );
            }


            as.add( new Copies( ( Integer )copies.getValue() ) );
            
            if (reverse.isSelected())
            {
                as.add(new Reverse());   
            }
            else
            {
                attributes.remove(Reverse.class);   
            }

            return as;
        }





        // print service cell renderer
        class PrintServiceCellRenderer extends JLabel implements ListCellRenderer <PrintService>
        {

            public PrintServiceCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends PrintService> list,
            PrintService value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {

                setText( value == null ? "" : value.getName() );

                Color background;
                Color foreground;

                if ( isSelected )
                {
                    background = jEdit.getColorProperty( "view.selectionColor" );
                    foreground = jEdit.getColorProperty( "view.fgColor" );
                }
                else
                {
                    background = jEdit.getColorProperty( "view.bgColor" );
                    foreground = jEdit.getColorProperty( "view.fgColor" );
                }


                setBackground( background );
                setForeground( foreground );

                Dimension d = new Dimension( ( int )getSize().getWidth(), ( int )getSize().getHeight() + 5 );
                setSize( d );

                return this;
            }
        }
    }



    private class PageSetupPanel extends PrinterPanel
    {

        private JComboBox<String> onlyPrint;
        private JComboBox<String> outputTray;
        NumericTextField topMarginField;
        NumericTextField leftMarginField;
        NumericTextField rightMarginField;
        NumericTextField bottomMarginField;


        public PageSetupPanel()
        {
            super();
            JPanel layoutPanel = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            layoutPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Layout", "Layout" ) ), BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            sides = new JComboBox<Sides>();
            sides.setEnabled( false );
            sides.setRenderer( new SidesCellRenderer() );

            pagesPerSide = new JComboBox<NumberUp>();
            pagesPerSide.setEnabled( false );

            // disable this when pagesPerSide is 1
            pageOrdering = new JComboBox<PresentationDirection>();
            pageOrdering.setEnabled( false );

            onlyPrint = new JComboBox<String>();
            onlyPrint.addItem( jEdit.getProperty( "print.dialog.All_sheets", "All sheets" ) );    // ALL
            onlyPrint.addItem( jEdit.getProperty( "print.dialog.Odd_sheets", "Odd sheets" ) );    // ODD
            onlyPrint.addItem( jEdit.getProperty( "print.dialog.Even_sheets", "Even sheets" ) );    // EVEN
            onlyPrint.setSelectedIndex( 0 );
            onlyPrint.setEnabled( true );

            // TODO: scale?
            layoutPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Two-sided", "Two-sided" ) + ':' ) );
            layoutPanel.add( sides );
            layoutPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Pages_per_side", "Pages per side" ) + ':' ) );
            layoutPanel.add( pagesPerSide );
            layoutPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Page_ordering", "Page ordering" ) + ':' ) );
            layoutPanel.add( pageOrdering );
            layoutPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Only_print", "Only print" ) + ':' ) );
            layoutPanel.add( onlyPrint );

            JPanel paperPanel = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            paperPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Paper", "Paper" ) ), BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );

            paperSource = new JComboBox<MediaTray>();
            paperSource.setEnabled( false );

            outputTray = new JComboBox<String>();
            outputTray.setEnabled( false );

            paperSize = new JComboBox<String>();
            paperSize.setEnabled( false );

            orientation = new JComboBox<OrientationRequested>();
            orientation.setEnabled( false );
            orientation.setRenderer( new OrientationCellRenderer() );

            paperPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Paper_source", "Paper source" ) + ':' ) );
            paperPanel.add( paperSource );
            paperPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Output_tray", "Output tray" ) + ':' ) );
            paperPanel.add( outputTray );
            paperPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Paper_size", "Paper size" ) + ':' ) );
            paperPanel.add( paperSize );
            paperPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Orientation", "Orientation" ) + ':' ) );
            paperPanel.add( orientation );

            JPanel marginPanel = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            marginPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Margins", "Margins" ) ), BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            boolean unitIsMM = getUnits() == MediaPrintableArea.MM;
            String topMargin = jEdit.getProperty("print.topMargin", unitIsMM ? "25" : "1.0");
            String leftMargin = jEdit.getProperty("print.leftMargin", unitIsMM ? "25" : "1.0");
            String rightMargin = jEdit.getProperty("print.rightMargin", unitIsMM ? "25" : "1.0");
            String bottomMargin = jEdit.getProperty("print.bottomMargin", unitIsMM ? "25" : "1.0");
            topMarginField = new NumericTextField( topMargin, true, unitIsMM );
            leftMarginField = new NumericTextField( leftMargin, true, unitIsMM );
            rightMarginField = new NumericTextField( rightMargin, true, unitIsMM );
            bottomMarginField = new NumericTextField( bottomMargin, true, unitIsMM );

            String unitsLabel = unitIsMM ? " (mm)" : " (in)";
            marginPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Top", "Top" ) + unitsLabel ) );
            marginPanel.add( topMarginField );
            marginPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Left", "Left" ) + unitsLabel ) );
            marginPanel.add( leftMarginField );
            marginPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Right", "Right" ) + unitsLabel ) );
            marginPanel.add( rightMarginField );
            marginPanel.add( new JLabel( jEdit.getProperty( "print.dialog.Bottom", "Bottom" ) + unitsLabel ) );
            marginPanel.add( bottomMarginField );

            JPanel finishingPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
            finishingPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Finishing", "Finishing" ) ), BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            finishing = new JComboBox<Finishings>();
            finishing.setEnabled( false );
            finishing.setRenderer( new FinishingCellRenderer() );
            Box finishingBox = Box.createHorizontalBox();
            finishingBox.add( new JLabel( jEdit.getProperty( "print.dialog.Finishing", "Finishing" ) + ':' ) );
            finishingBox.add( Box.createHorizontalStrut( 6 ) );
            finishingBox.add( finishing );
            finishingPanel.add( finishingBox );

            JPanel content = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            content.add( layoutPanel );
            content.add( paperPanel );
            content.add( marginPanel );
            content.add( finishingPanel );
            add( content );
            
            // add listeners
            pagesPerSide.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        NumberUp nu = ( NumberUp )pagesPerSide.getSelectedItem();
                        if ( nu != null && nu.getValue() == 1 )
                        {
                            pageOrdering.setEnabled( false );
                        }
                    }
                }
            );
            paperSize.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        PageSetupPanel.this.setDefaultMargins();
                    }
                }
            );
            orientation.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        PageSetupPanel.this.setDefaultMargins();
                    }
                }
            );
            
            
        }



        // sides renderer
        class SidesCellRenderer extends JLabel implements ListCellRenderer <Sides>
        {

            public SidesCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends Sides> list,
            Sides value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {
                setText( value == null ? "" : getMessage( value.toString() ) );
                return this;
            }
        }





        // orientation renderer
        class OrientationCellRenderer extends JLabel implements ListCellRenderer <OrientationRequested>
        {

            public OrientationCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends OrientationRequested> list,
            OrientationRequested value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {
                setText( value == null ? "" : getMessage( value.toString() ) );
                return this;
            }
        }





        // finishing renderer
        class FinishingCellRenderer extends JLabel implements ListCellRenderer <Finishings>
        {

            public FinishingCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends Finishings> list,
            Finishings value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {
                setText( value == null ? "" : getMessage( value.toString() ) );
                return this;
            }
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();
            if ( sides.isEnabled() )
            {
                as.add( ( Sides )sides.getSelectedItem() );
            }


            if ( pagesPerSide.isEnabled() )
            {
                as.add( ( NumberUp )pagesPerSide.getSelectedItem() );
            }


            if ( pageOrdering.isEnabled() )
            {
                as.add( ( PresentationDirection )pageOrdering.getSelectedItem() );
            }


            onlyPrintPages = onlyPrint.getSelectedIndex();

            if ( paperSource.isEnabled() )
            {
                as.add( ( MediaTray )paperSource.getSelectedItem() );
            }


            if ( paperSize.isEnabled() )
            {
                as.add( paperSizes.get( paperSize.getSelectedIndex() ) );
            }


            if ( orientation.isEnabled() )
            {
                as.add( ( OrientationRequested )orientation.getSelectedItem() );
            }


            Number topMargin = topMarginField.getValue();
            Number leftMargin = leftMarginField.getValue();
            Number rightMargin = rightMarginField.getValue();
            Number bottomMargin = bottomMarginField.getValue();
            
            Margins margins = new Margins( topMargin.floatValue(), leftMargin.floatValue(), rightMargin.floatValue(), bottomMargin.floatValue() );
            as.add( margins );
            
            jEdit.setProperty("print.topMargin", topMargin.toString());
            jEdit.setProperty("print.leftMargin", leftMargin.toString());
            jEdit.setProperty("print.rightMargin", rightMargin.toString());
            jEdit.setProperty("print.bottomMargin", bottomMargin.toString());

            return as;
        }


        // recalculates the media printable area when the printer, paper size,
        // margin, or orientation changes
        // returns null on okay, error message otherwise
        protected String recalculate()
        {
            if ( !PrinterDialog.this.isShowing() )
            {
                return null;
            }


            // get the printable area for the selected paper size and orientation
            int units = getUnits();
            MediaPrintableArea supportedArea = getSupportedPrintableArea();

            // Log.log( Log.DEBUG, this, "supportedArea = " + supportedArea.getX( units ) + ", " + supportedArea.getY( units ) + ", " + supportedArea.getWidth( units ) + ", " + supportedArea.getHeight( units ) );
            // get the selected paper size
            Media media = paperSizes.get( paperSize.getSelectedIndex() );

            // get paper width and height
            MediaSize mediaSize = null;
            if ( media instanceof MediaSizeName )
            {
                MediaSizeName name = ( MediaSizeName )media;
                mediaSize = MediaSize.getMediaSizeForName( name );
            }


            float paperWidth = mediaSize.getX( units );
            float paperHeight = mediaSize.getY( units );

            // get the user desired margins
            float topMargin = topMarginField.getValue().floatValue();
            float leftMargin = leftMarginField.getValue().floatValue();
            float rightMargin = rightMarginField.getValue().floatValue();
            float bottomMargin = bottomMarginField.getValue().floatValue();

            // get the selected orientation and adjust the margins and width/height
            OrientationRequested orientationRequested = ( OrientationRequested )orientation.getSelectedItem();
            rotateMargins( topMargin, leftMargin, rightMargin, bottomMargin, orientationRequested );

            // calculate new printable area
            float x = leftMargin;
            float y = topMargin;
            float width = paperWidth - leftMargin - rightMargin;
            float height = paperHeight - topMargin - bottomMargin;

            // check that the new printable area fits inside the supported area
            if ( x < supportedArea.getX( units ) )
            {
                return jEdit.getProperty( "print.dialog.error.Invalid_left_margin", "Invalid left margin." );
            }


            if ( y < supportedArea.getY( units ) )
            {
                return jEdit.getProperty( "print.dialog.error.Invalid_top_margin", "Invalid top margin." );
            }


            if ( width <= 0 || x + width > supportedArea.getX( units ) + supportedArea.getWidth( units ) )
            {
                return jEdit.getProperty( "print.dialog.error.Invalid_left_andor_right_margin.", "Invalid left and/or right margin." );
            }


            if ( height <= 0 || y + height > supportedArea.getY( units ) + supportedArea.getHeight( units ) )
            {
                return jEdit.getProperty( "print.dialog.error.Invalid_top_andor_bottom_margin", "Invalid top and/or bottom margin." );
            }


            // Log.log( Log.DEBUG, this, "new printable area: " + x + ", " + y + ", " + width + ", " + height );
            MediaPrintableArea area = new MediaPrintableArea( x, y, width, height, units );
            attributes.add( area );
            return null;
        }


        private MediaPrintableArea getSupportedPrintableArea()
        {
            MediaPrintableArea supportedArea = null;
            HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            if ( paperSizes != null )
            {
                attrs.add( paperSizes.get( paperSize.getSelectedIndex() ) );
            }

            if ( orientation != null && orientation.getSelectedItem() != null )
            {
                attrs.add( ( OrientationRequested )orientation.getSelectedItem() );
            }

            Object values = getPrintService().getSupportedAttributeValues( MediaPrintableArea.class, DocFlavor.SERVICE_FORMATTED.PRINTABLE, attrs );
            if ( values != null )
            {
                MediaPrintableArea[] mpas = ( MediaPrintableArea[] )values;
                supportedArea = mpas[0];
            }
            else
            {
                supportedArea = ( MediaPrintableArea )getPrintService().getDefaultAttributeValue( MediaPrintableArea.class );
            }

            return supportedArea;
        }


        private void rotateMargins( float topMargin, float leftMargin, float rightMargin, float bottomMargin, OrientationRequested orientationRequested )
        {
            if ( OrientationRequested.REVERSE_PORTRAIT.equals( orientationRequested ) )
            {
                float m = leftMargin;
                leftMargin = rightMargin;
                rightMargin = m;
                m = topMargin;
                topMargin = bottomMargin;
                bottomMargin = m;
            }
            else
            if ( OrientationRequested.LANDSCAPE.equals( orientationRequested ) )
            {
                float m = leftMargin;
                leftMargin = bottomMargin;
                bottomMargin = rightMargin;
                rightMargin = topMargin;
                topMargin = m;
            }
            else
            if ( OrientationRequested.REVERSE_LANDSCAPE.equals( orientationRequested ) )
            {
                float m = leftMargin;
                leftMargin = topMargin;
                topMargin = rightMargin;
                rightMargin = bottomMargin;
                bottomMargin = m;
            }
        }




        // set the values in the margin text fields to be either the minimum
        // supported by the printer or the last used margins or the currently
        // set margins, also sets a range on the text fields so the user can't
        // enter a value too small or too large. Note that it is still possible
        // for the user to enter invalid margin values, e.g.
        // top margin + bottom margin > printable area height.
        void setDefaultMargins()
        {
            int units = getUnits();
            boolean integerOnly = units == MediaPrintableArea.MM;

            // get the last margins the user set
            Margins margins = ( Margins )attributes.get( Margins.class );

            // get the minimum and maximum margins supported by the printer
            float[] minMargins = getMinimumMargins();
            float[] maxMargins = getMaximumMargins();

            // use the printer margins if there are no last used margins
            float[] marginValues = margins == null ? minMargins : margins.getMargins( units );

            // set the margin text area values
            NumericTextField[] numberFields = new NumericTextField[] {topMarginField, leftMarginField, rightMarginField, bottomMarginField};
            for ( int i = 0; i < numberFields.length; i++ )
            {
                NumericTextField field = numberFields[i];
                Float currentUserMargin = null;
                String text = field.getText();
                if ( text != null && !text.isEmpty() )
                {
                    currentUserMargin = Float.valueOf( text );
                }

                Float value = Float.valueOf( marginValues[i] );
                Float minMargin = Float.valueOf( minMargins[i] );
                Float maxMargin = Float.valueOf( maxMargins[i] );
                if ( currentUserMargin == null || currentUserMargin < minMargin || currentUserMargin > maxMargin )
                {

                    // current user margin is invalid
                    field.setText( integerOnly ? String.valueOf( value.intValue() ) : String.valueOf( value ) );
                }

                field.setMinValue( integerOnly ? Integer.valueOf( minMargin.intValue() ) : Float.valueOf( minMargin ) );
                field.setMaxValue( integerOnly ? Integer.valueOf( maxMargin.intValue() ) : Float.valueOf( maxMargin ) );
                field.setToolTipText( "Min: " + minMargin + ", max: " + maxMargin );
            }
        }


        // the default margins are the margins that allow the maximum supported
        // printable area by the currently selected printer, paper, and orientation,
        // returns float[]{topMargin, leftMargin, rightMargin, bottomMargin}
        private float[] getMinimumMargins()
        {

            // get the printable area for the selected paper size and orientation
            int units = getUnits();
            boolean integerOnly = units == MediaPrintableArea.MM;
            MediaPrintableArea supportedArea = getSupportedPrintableArea();

            // Log.log( Log.DEBUG, this, "supportedArea = " + supportedArea.getX( units ) + ", " + supportedArea.getY( units ) + ", " + supportedArea.getWidth( units ) + ", " + supportedArea.getHeight( units ) );
            // get the selected paper size
            Media media = paperSizes.get( paperSize.getSelectedIndex() );

            // get paper width and height
            MediaSize mediaSize = null;
            if ( media instanceof MediaSizeName )
            {
                MediaSizeName name = ( MediaSizeName )media;
                mediaSize = MediaSize.getMediaSizeForName( name );
            }


            float paperWidth = mediaSize.getX( units );
            float paperHeight = mediaSize.getY( units );

            // calculate the default margins
            float topMargin = supportedArea.getY( units );
            topMargin = integerOnly ? Double.valueOf( Math.ceil( topMargin ) ).floatValue() : topMargin;
            float leftMargin = supportedArea.getX( units );
            leftMargin = integerOnly ? Double.valueOf( Math.ceil( leftMargin ) ).floatValue() : leftMargin;
            float rightMargin = Math.max( 0.0f, paperWidth - leftMargin - supportedArea.getWidth( units ) );
            rightMargin = integerOnly ? Double.valueOf( Math.ceil( rightMargin ) ).floatValue() : rightMargin;
            float bottomMargin = Math.max( 0.0f, paperHeight - topMargin - supportedArea.getHeight( units ) );
            bottomMargin = integerOnly ? Double.valueOf( Math.ceil( bottomMargin ) ).floatValue() : bottomMargin;

            // adjust the margins for the paper orientation
            OrientationRequested orientationRequested = ( OrientationRequested )orientation.getSelectedItem();
            rotateMargins( topMargin, leftMargin, rightMargin, bottomMargin, orientationRequested );

            //Log.log( Log.DEBUG, this, "getMinimumMargins returning " + topMargin + ", " + leftMargin + ", " + rightMargin + ", " + bottomMargin);
            return new float[] {topMargin, leftMargin, rightMargin, bottomMargin};
        }


        private float[] getMaximumMargins()
        {

            // get the printable area for the selected paper size and orientation
            int units = getUnits();
            boolean integerOnly = units == MediaPrintableArea.MM;
            MediaPrintableArea supportedArea = getSupportedPrintableArea();

            // Log.log( Log.DEBUG, this, "supportedArea = " + supportedArea.getX( units ) + ", " + supportedArea.getY( units ) + ", " + supportedArea.getWidth( units ) + ", " + supportedArea.getHeight( units ) );
            // get the selected paper size
            Media media = paperSizes.get( paperSize.getSelectedIndex() );

            // get paper width and height
            MediaSize mediaSize = null;
            if ( media instanceof MediaSizeName )
            {
                MediaSizeName name = ( MediaSizeName )media;
                mediaSize = MediaSize.getMediaSizeForName( name );
            }


            float paperWidth = mediaSize.getX( units );
            float paperHeight = mediaSize.getY( units );

            // calculate the maximum margins
            float topMargin = supportedArea.getY( units ) + supportedArea.getHeight( units );
            topMargin = integerOnly ? Double.valueOf( Math.ceil( topMargin ) ).floatValue() : topMargin;
            float leftMargin = supportedArea.getX( units ) + supportedArea.getWidth( units );
            leftMargin = integerOnly ? Double.valueOf( Math.ceil( leftMargin ) ).floatValue() : leftMargin;
            float rightMargin = paperWidth - supportedArea.getX( units );
            rightMargin = integerOnly ? Double.valueOf( Math.ceil( rightMargin ) ).floatValue() : rightMargin;
            float bottomMargin = paperHeight - supportedArea.getY( units );
            bottomMargin = integerOnly ? Double.valueOf( Math.ceil( bottomMargin ) ).floatValue() : bottomMargin;

            // adjust the margins for the paper orientation
            OrientationRequested orientationRequested = ( OrientationRequested )orientation.getSelectedItem();
            rotateMargins( topMargin, leftMargin, rightMargin, bottomMargin, orientationRequested );

            //Log.log( Log.DEBUG, this, "getMaximumMargins returning " + topMargin + ", " + leftMargin + ", " + rightMargin + ", " + bottomMargin);
            return new float[] {topMargin, leftMargin, rightMargin, bottomMargin};
        }


        // returns INCH or MM depending on Locale
        // note that while Canada is mostly metric, Canadian paper sizes
        // are essentially US ANSI sizes rounded to the nearest 5 mm
        private int getUnits()
        {
            String country = Locale.getDefault().getCountry();
            //String country = "Latvia";    // for testing metric
            if ( "".equals( country ) || Locale.US.getCountry().equals( country ) || Locale.CANADA.getCountry().equals( country ) )
            {
                return MediaPrintableArea.INCH;
            }

            return MediaPrintableArea.MM;
        }
    }



    private class JobPanel extends PrinterPanel
    {

        private JRadioButton nowButton;
        private JRadioButton atButton;
        private JRadioButton holdButton;
        private JSpinner when;


        // need 3 subpanels, Job Details, Print Document, and Finishing
        public JobPanel()
        {
            super();
            // job details panel
            JPanel jobPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
            jobPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Job_Details", "Job Details" ) ), BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            priority = new JComboBox<Priority>();
            priority.addItem( Priority.LOW );
            priority.addItem( Priority.MEDIUM );
            priority.addItem( Priority.HIGH );
            priority.addItem( Priority.URGENT );
            priority.setSelectedItem( Priority.MEDIUM );

            Box priorityBox = Box.createHorizontalBox();
            priorityBox.add( new JLabel( jEdit.getProperty( "print.dialog.Priority", "Priority" ) + ':' ) );
            priorityBox.add( Box.createHorizontalStrut( 6 ) );
            priorityBox.add( priority );
            jobPanel.add( priorityBox );

            // when to print panel
            JPanel printPanel = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            printPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), jEdit.getProperty( "print.dialog.Print_Document", "Print Document" ) ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );

            // print now
            nowButton = new JRadioButton( jEdit.getProperty( "print.dialog.Now", "Now" ) );
            nowButton.setSelected( true );

            // print later
            atButton = new JRadioButton( jEdit.getProperty( "print.dialog.At", "At" ) );
            atButton.setEnabled( true );
            Calendar calendar = Calendar.getInstance( Locale.getDefault() );
            Date initialDate = calendar.getTime();
            calendar.add( Calendar.YEAR, 1 );
            Date latestDate = calendar.getTime();
            SpinnerDateModel dateModel = new SpinnerDateModel( initialDate, initialDate, latestDate, Calendar.YEAR );
            when = new JSpinner( dateModel );
            when.setEnabled( true );

            // print hold
            holdButton = new JRadioButton( jEdit.getProperty( "print.dialog.On_Hold", "On Hold" ) );
            holdButton.setEnabled( true );
            new MyButtonGroup( nowButton, atButton, holdButton );
            printPanel.add( nowButton );
            printPanel.add( Box.createGlue() );
            printPanel.add( atButton );
            printPanel.add( when );
            printPanel.add( holdButton );
            printPanel.add( Box.createGlue() );

            JPanel content = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            content.add( jobPanel );
            content.add( printPanel );
            add( content, BorderLayout.NORTH );

            // add listeners
            atButton.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        when.setEnabled( atButton.isSelected() );
                    }
                }
            );
            
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();
            as.add( new JobPriority( ( ( Priority )priority.getSelectedItem() ).getValue() ) );
            if ( finishing.isEnabled() )
            {
                as.add( ( Finishings )finishing.getSelectedItem() );
            }


            Date holdUntil = new Date( 0L );    // print now
            if ( atButton.isSelected() )
            {

                // print later
                holdUntil = ( ( SpinnerDateModel )when.getModel() ).getDate();
            }
            else
            if ( holdButton.isSelected() )
            {

                // hold for a year, seems weird
                Calendar later = Calendar.getInstance();
                later.add( Calendar.YEAR, 1 );
                holdUntil = later.getTime();
            }


            as.add( new JobHoldUntil( holdUntil ) );
            return as;
        }
    }



    private static class Priority
    {

        public static final Priority LOW = new Priority( 1, jEdit.getProperty( "print.dialog.Low", "Low" ) );
        public static final Priority MEDIUM = new Priority( 50, jEdit.getProperty( "print.dialog.Medium", "Medium" ) );
        public static final Priority HIGH = new Priority( 80, jEdit.getProperty( "print.dialog.High", "High" ) );
        public static final Priority URGENT = new Priority( 100, jEdit.getProperty( "print.dialog.Urgent", "Urgent" ) );
        private int value;
        private String name;


        private Priority( int value, String name )
        {
            this.value = value;
            this.name = name;
        }


        @Override
        public String toString()
        {
            return name;
        }


        public int getValue()
        {
            return value;
        }
    }



    private class AdvancedPanel extends PrinterPanel
    {

        private JComboBox<PrintQuality> quality;
        private JComboBox<Chromaticity> chromaticity;


        public AdvancedPanel()
        {
            super();
            quality = new JComboBox<PrintQuality>();
            quality.addItem( PrintQuality.DRAFT );
            quality.addItem( PrintQuality.NORMAL );
            quality.addItem( PrintQuality.HIGH );
            PrintQuality pq = (PrintQuality)attributes.get(PrintQuality.class);
            quality.setSelectedItem( pq == null ? PrintQuality.NORMAL : pq );
            quality.setRenderer( new QualityCellRenderer() );

            chromaticity = new JComboBox<Chromaticity>();
            chromaticity.addItem( Chromaticity.MONOCHROME );
            chromaticity.addItem( Chromaticity.COLOR );
            Chromaticity value = ( Chromaticity )attributes.get( Chromaticity.class );
            chromaticity.setSelectedItem(value == null ? Chromaticity.MONOCHROME : value);
            chromaticity.setRenderer( new ChromaticityCellRenderer() );

            JPanel content = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            content.add( new JLabel( jEdit.getProperty( "print.dialog.Quality", "Quality" ) ) );
            content.add( quality );
            content.add( new JLabel( jEdit.getProperty( "print.dialog.Ink", "Ink" ) ) );
            content.add( chromaticity );
            add( content, BorderLayout.NORTH );
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();
            as.add( ( Chromaticity )chromaticity.getSelectedItem() );
            as.add( ( PrintQuality )quality.getSelectedItem() );
            return as;
        }





        // quality renderer
        class QualityCellRenderer extends JLabel implements ListCellRenderer <PrintQuality>
        {

            public QualityCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends PrintQuality> list,
            PrintQuality value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {
                setText( value == null ? "" : getMessage( value.toString() ) );
                return this;
            }
        }





        // Chromaticity renderer
        class ChromaticityCellRenderer extends JLabel implements ListCellRenderer <Chromaticity>
        {

            public ChromaticityCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends Chromaticity> list,
            Chromaticity value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {
                setText( value == null ? "" : getMessage( value.toString() ) );
                return this;
            }
        }
    }



    private class jEditPanel extends PrinterPanel
    {

        private FontSelector font;
        private JCheckBox printHeader;
        private JCheckBox printFooter;
        private JCheckBox printLineNumbers;
        private JCheckBox printFolds;
        private JComboBox<String> tabSize;


        public jEditPanel()
        {
            super();
            /* Font */
            font = new FontSelector( jEdit.getFontProperty( "print.font" ) );

            /* Header */
            printHeader = new JCheckBox( jEdit.getProperty( "options.print.header" ) );
            printHeader.setSelected( jEdit.getBooleanProperty( "print.header" ) );

            /* Footer */
            printFooter = new JCheckBox( jEdit.getProperty("options.print.footer" ) );
            printFooter.setSelected( jEdit.getBooleanProperty( "print.footer" ) );

            /* Line numbering */
            printLineNumbers = new JCheckBox( jEdit.getProperty( "options.print.lineNumbers" ) );
            printLineNumbers.setSelected( jEdit.getBooleanProperty( "print.lineNumbers" ) );


            /* Tab size */
            String[] tabSizes = {"2", "4", "8"};
            tabSize = new JComboBox<String>( tabSizes );
            tabSize.setEditor( new NumericTextField( "", true, true ) );
            tabSize.setEditable( true );
            tabSize.setSelectedItem( jEdit.getProperty( "print.tabSize" ) );

            /* Print Folds */
            printFolds = new JCheckBox( jEdit.getProperty( "options.print.folds" ) );
            printFolds.setSelected( jEdit.getBooleanProperty( "print.folds", true ) );
            
            JPanel content = new JPanel( new VariableGridLayout( VariableGridLayout.FIXED_NUM_COLUMNS, 2, 6, 6 ) );
            content.add( new JLabel(jEdit.getProperty( "options.print.font" ) ) );
            content.add( font );
            content.add( new JLabel(jEdit.getProperty( "options.print.tabSize" ) ) );
            content.add( tabSize );
            content.add( printHeader );
            content.add( Box.createGlue());
            content.add( printFooter );
            content.add( Box.createGlue());
            content.add( printLineNumbers );
            content.add( Box.createGlue());
            content.add( printFolds );
            content.add( Box.createGlue());
            add( content, BorderLayout.NORTH );
        }


        public AttributeSet getAttributes()
        {
            jEdit.setFontProperty( "print.font", font.getFont() );
            jEdit.setBooleanProperty( "print.header", printHeader.isSelected() );
            jEdit.setBooleanProperty( "print.footer", printFooter.isSelected() );
            jEdit.setBooleanProperty( "print.lineNumbers", printLineNumbers.isSelected() );
            jEdit.setProperty( "print.tabSize", ( String )tabSize.getSelectedItem() );
            jEdit.setBooleanProperty( "print.folds", printFolds.isSelected() );
            return null;
        }
    }





    class MyButtonGroup extends ButtonGroup
    {

        // ButtonGroup should have this
        public MyButtonGroup( AbstractButton... buttons )
        {
            super();
            for ( AbstractButton b : buttons )
            {
                super.add( b );
            }
        }
    }


    // map to lookup the names returned from the print service as more human
    // readable names
    public void initMessages()
    {
        messageMap = new HashMap<String, String>();
        messageMap.put( "Automatic-Feeder", jEdit.getProperty( "print.dialog.Automatic-Feeder", "Automatic Feeder" ) );
        messageMap.put( "Cassette", jEdit.getProperty( "print.dialog.Cassette", "Cassette" ) );
        messageMap.put( "Form-Source", jEdit.getProperty( "print.dialog.Form-Source", "Form Source" ) );
        messageMap.put( "Large-Format", jEdit.getProperty( "print.dialog.Large-Format", "Large Format" ) );
        messageMap.put( "Manual-Envelope", jEdit.getProperty( "print.dialog.Manual-Envelope", "Manual Envelope" ) );
        messageMap.put( "Small-Format", jEdit.getProperty( "print.dialog.Small-Format", "Small Format" ) );
        messageMap.put( "Tractor-Feeder", jEdit.getProperty( "print.dialog.Tractor-Feeder", "Tractor Feeder" ) );
        messageMap.put( "a", jEdit.getProperty( "print.dialog.a", "Engineering A" ) );
        messageMap.put( "accepting-jobs", jEdit.getProperty( "print.dialog.accepting-jobs", "Accepting jobs" ) );
        messageMap.put( "auto-select", jEdit.getProperty( "print.dialog.auto-select", "Automatically Select" ) );
        messageMap.put( "b", jEdit.getProperty( "print.dialog.b", "Engineering B" ) );
        messageMap.put( "c", jEdit.getProperty( "print.dialog.c", "Engineering C" ) );
        messageMap.put( "color", jEdit.getProperty( "print.dialog.Color", "Color" ) );
        messageMap.put( "d", jEdit.getProperty( "print.dialog.d", "Engineering D" ) );
        messageMap.put( "draft", jEdit.getProperty( "print.dialog.Draft", "Draft" ) );
        messageMap.put( "e", jEdit.getProperty( "print.dialog.e", "Engineering E" ) );
        messageMap.put( "envelope", jEdit.getProperty( "print.dialog.envelope", "Envelope" ) );
        messageMap.put( "executive", jEdit.getProperty( "print.dialog.executive", "Executive" ) );
        messageMap.put( "folio", jEdit.getProperty( "print.dialog.folio", "Folio" ) );
        messageMap.put( "high", jEdit.getProperty( "print.dialog.High", "High" ) );
        messageMap.put( "invite-envelope", jEdit.getProperty( "print.dialog.invite-envelope", "Invitation Envelope" ) );
        messageMap.put( "invoice", jEdit.getProperty( "print.dialog.invoice", "Invoice" ) );
        messageMap.put( "iso-2a0", jEdit.getProperty( "print.dialog.iso-2a0", "2A0 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-4a0", jEdit.getProperty( "print.dialog.iso-4a0", "4A0 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a0", jEdit.getProperty( "print.dialog.iso-a0", "A0 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a1", jEdit.getProperty( "print.dialog.iso-a1", "A1 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a2", jEdit.getProperty( "print.dialog.iso-a2", "A2 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a3", jEdit.getProperty( "print.dialog.iso-a3", "A3 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a4", jEdit.getProperty( "print.dialog.iso-a4", "A4 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a5", jEdit.getProperty( "print.dialog.iso-a5", "A5 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a6", jEdit.getProperty( "print.dialog.iso-a6", "A6 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a7", jEdit.getProperty( "print.dialog.iso-a7", "A7 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a8", jEdit.getProperty( "print.dialog.iso-a8", "A8 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a9", jEdit.getProperty( "print.dialog.iso-a9", "A9 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-a10", jEdit.getProperty( "print.dialog.iso-a10", "A10 (ISO/DIN & JIS)" ) );
        messageMap.put( "iso-b0", jEdit.getProperty( "print.dialog.iso-b0", "B0 (ISO/DIN)" ) );
        messageMap.put( "iso-b1", jEdit.getProperty( "print.dialog.iso-b1", "B1 (ISO/DIN)" ) );
        messageMap.put( "iso-b2", jEdit.getProperty( "print.dialog.iso-b2", "B2 (ISO/DIN)" ) );
        messageMap.put( "iso-b3", jEdit.getProperty( "print.dialog.iso-b3", "B3 (ISO/DIN)" ) );
        messageMap.put( "iso-b4", jEdit.getProperty( "print.dialog.iso-b4", "B4 (ISO/DIN)" ) );
        messageMap.put( "iso-b5", jEdit.getProperty( "print.dialog.iso-b5", "B5 (ISO/DIN)" ) );
        messageMap.put( "iso-b6", jEdit.getProperty( "print.dialog.iso-b6", "B6 (ISO/DIN)" ) );
        messageMap.put( "iso-b7", jEdit.getProperty( "print.dialog.iso-b7", "B7 (ISO/DIN)" ) );
        messageMap.put( "iso-b8", jEdit.getProperty( "print.dialog.iso-b8", "B8 (ISO/DIN)" ) );
        messageMap.put( "iso-b9", jEdit.getProperty( "print.dialog.iso-b9", "B9 (ISO/DIN)" ) );
        messageMap.put( "iso-b10", jEdit.getProperty( "print.dialog.iso-b10", "B10 (ISO/DIN)" ) );
        messageMap.put( "iso-c0", jEdit.getProperty( "print.dialog.iso-c0", "C0 (ISO/DIN)" ) );
        messageMap.put( "iso-c1", jEdit.getProperty( "print.dialog.iso-c1", "C1 (ISO/DIN)" ) );
        messageMap.put( "iso-c2", jEdit.getProperty( "print.dialog.iso-c2", "C2 (ISO/DIN)" ) );
        messageMap.put( "iso-c3", jEdit.getProperty( "print.dialog.iso-c3", "C3 (ISO/DIN)" ) );
        messageMap.put( "iso-c4", jEdit.getProperty( "print.dialog.iso-c4", "C4 (ISO/DIN)" ) );
        messageMap.put( "iso-c5", jEdit.getProperty( "print.dialog.iso-c5", "C5 (ISO/DIN)" ) );
        messageMap.put( "iso-c6", jEdit.getProperty( "print.dialog.iso-c6", "C6 (ISO/DIN)" ) );
        messageMap.put( "iso-c7", jEdit.getProperty( "print.dialog.iso-c7", "C7 (ISO/DIN)" ) );
        messageMap.put( "iso-c8", jEdit.getProperty( "print.dialog.iso-c8", "C8 (ISO/DIN)" ) );
        messageMap.put( "iso-c9", jEdit.getProperty( "print.dialog.iso-c9", "C9 (ISO/DIN)" ) );
        messageMap.put( "iso-c10", jEdit.getProperty( "print.dialog.iso-c10", "C10 (ISO/DIN)" ) );
        messageMap.put( "iso-designated-long", jEdit.getProperty( "print.dialog.iso-designated-long", "ISO Designated Long" ) );
        messageMap.put( "italian-envelope", jEdit.getProperty( "print.dialog.italian-envelope", "Italy Envelope" ) );
        messageMap.put( "italy-envelope", jEdit.getProperty( "print.dialog.italy-envelope", "Italy Envelope" ) );
        messageMap.put( "japanese-postcard", jEdit.getProperty( "print.dialog.japanese-postcard", "Postcard (JIS)" ) );
        messageMap.put( "jis-b0", jEdit.getProperty( "print.dialog.jis-b0", "B0 (JIS)" ) );
        messageMap.put( "jis-b1", jEdit.getProperty( "print.dialog.jis-b1", "B1 (JIS)" ) );
        messageMap.put( "jis-b2", jEdit.getProperty( "print.dialog.jis-b2", "B2 (JIS)" ) );
        messageMap.put( "jis-b3", jEdit.getProperty( "print.dialog.jis-b3", "B3 (JIS)" ) );
        messageMap.put( "jis-b4", jEdit.getProperty( "print.dialog.jis-b4", "B4 (JIS)" ) );
        messageMap.put( "jis-b5", jEdit.getProperty( "print.dialog.jis-b5", "B5 (JIS)" ) );
        messageMap.put( "jis-b6", jEdit.getProperty( "print.dialog.jis-b6", "B6 (JIS)" ) );
        messageMap.put( "jis-b7", jEdit.getProperty( "print.dialog.jis-b7", "B7 (JIS)" ) );
        messageMap.put( "jis-b8", jEdit.getProperty( "print.dialog.jis-b8", "B8 (JIS)" ) );
        messageMap.put( "jis-b9", jEdit.getProperty( "print.dialog.jis-b9", "B9 (JIS)" ) );
        messageMap.put( "jis-b10", jEdit.getProperty( "print.dialog.jis-b10", "B10 (JIS)" ) );
        messageMap.put( "landscape", jEdit.getProperty( "print.dialog.landscape", "Landscape" ) );
        messageMap.put( "main", jEdit.getProperty( "print.dialog.main", "Main" ) );
        messageMap.put( "manual", jEdit.getProperty( "print.dialog.manual", "Manual" ) );
        messageMap.put( "middle", jEdit.getProperty( "print.dialog.middle", "Middle" ) );
        messageMap.put( "monarch-envelope", jEdit.getProperty( "print.dialog.monarch-envelope", "Monarch Envelope" ) );
        messageMap.put( "monochrome", jEdit.getProperty( "print.dialog.Monochrome", "Monochrome" ) );
        messageMap.put( "na-5x7", jEdit.getProperty( "print.dialog.na-5x7", "5\") x 7\" Paper" ) );
        messageMap.put( "na-6x9-envelope", jEdit.getProperty( "print.dialog.na-6x9-envelope", "6x9 Envelope" ) );
        messageMap.put( "na-7x9-envelope", jEdit.getProperty( "print.dialog.na-7x9-envelope", "6x7 Envelope" ) );
        messageMap.put( "na-8x10", jEdit.getProperty( "print.dialog.na-8x10", "8\") x 10\" Paper" ) );
        messageMap.put( "na-9x11-envelope", jEdit.getProperty( "print.dialog.na-9x11-envelope", "9x11 Envelope" ) );
        messageMap.put( "na-9x12-envelope", jEdit.getProperty( "print.dialog.na-9x12-envelope", "9x12 Envelope" ) );
        messageMap.put( "na-10x13-envelope", jEdit.getProperty( "print.dialog.na-10x13-envelope", "10x15 Envelope" ) );
        messageMap.put( "na-10x14-envelope", jEdit.getProperty( "print.dialog.na-10x14-envelope", "10x15 Envelope" ) );
        messageMap.put( "na-10x15-envelope", jEdit.getProperty( "print.dialog.na-10x15-envelope", "10x15 Envelope" ) );
        messageMap.put( "na-legal", jEdit.getProperty( "print.dialog.na-legal", "Legal" ) );
        messageMap.put( "na-letter", jEdit.getProperty( "print.dialog.na-letter", "Letter" ) );
        messageMap.put( "na-number-9-envelope", jEdit.getProperty( "print.dialog.na-number-9-envelope", "No. 9 Envelope" ) );
        messageMap.put( "na-number-10-envelope", jEdit.getProperty( "print.dialog.na-number-10-envelope", "No. 10 Envelope" ) );
        messageMap.put( "na-number-11-envelope", jEdit.getProperty( "print.dialog.na-number-11-envelope", "No. 11 Envelope" ) );
        messageMap.put( "na-number-12-envelope", jEdit.getProperty( "print.dialog.na-number-12-envelope", "No. 12 Envelope" ) );
        messageMap.put( "na-number-14-envelope", jEdit.getProperty( "print.dialog.na-number-14-envelope", "No. 14 Envelope" ) );
        messageMap.put( "normal", jEdit.getProperty( "print.dialog.Normal", "Normal" ) );
        messageMap.put( "not-accepting-jobs", jEdit.getProperty( "print.dialog.not-accepting-jobs", "Not accepting jobs" ) );
        messageMap.put( "one-sided", jEdit.getProperty( "print.dialog.one-sided", "One sided" ) );
        messageMap.put( "oufuko-postcard", jEdit.getProperty( "print.dialog.oufuko-postcard", "Double Postcard (JIS)" ) );
        messageMap.put( "personal-envelope", jEdit.getProperty( "print.dialog.personal-envelope", "Personal Envelope" ) );
        messageMap.put( "portrait", jEdit.getProperty( "print.dialog.portrait", "Portrait" ) );
        messageMap.put( "quarto", jEdit.getProperty( "print.dialog.quarto", "Quarto" ) );
        messageMap.put( "reverse-landscape", jEdit.getProperty( "print.dialog.reverse-landscape", "Reverse Landscape" ) );
        messageMap.put( "reverse-portrait", jEdit.getProperty( "print.dialog.reverse-portrait", "Reverse Portrait" ) );
        messageMap.put( "side", jEdit.getProperty( "print.dialog.side", "Side" ) );
        messageMap.put( "tabloid", jEdit.getProperty( "print.dialog.tabloid", "Tabloid" ) );
        messageMap.put( "top", jEdit.getProperty( "print.dialog.top", "Top" ) );
        messageMap.put( "two-sided-long-edge", jEdit.getProperty( "print.dialog.two-sided-long-edge", "Two Sided, Long Edge" ) );
        messageMap.put( "two-sided-short-edge", jEdit.getProperty( "print.dialog.two-sided-short-edge", "Two Sided, Short Edge" ) );
    }


    private String getMessage( String key )
    {
        String value = messageMap.get( key );
        return value == null ? key : value;
    }
}
