
package org.gjt.sp.jedit.print;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.*;
import java.awt.print.PrinterJob;
import java.util.*;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.GenericGUIUtilities;


public class PrinterDialog extends JDialog implements ListSelectionListener
{

    private PrintService selectedPrintService = null;
    private AttributeSet attributes;
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
    private boolean canceled = false;
    private Map<String, String> messageMap;


    public PrinterDialog( Window owner, AttributeSet attributes )
    {
        super( owner, Dialog.ModalityType.APPLICATION_MODAL );
        if ( attributes != null )
        {
            this.attributes = new HashAttributeSet( attributes );
        }
        else
        {
            this.attributes = new HashAttributeSet();
        }


        initMessages();

        JPanel contents = new JPanel( new BorderLayout() );
        contents.setBorder( BorderFactory.createEmptyBorder( 11, 11, 12, 12 ) );

        tabs = new JTabbedPane();
        tabs.setBorder( BorderFactory.createEmptyBorder( 0, 0, 11, 0 ) );
        tabs.add( "General", new GeneralPanel() );
        tabs.add( "Page Setup", new PageSetupPanel() );
        tabs.add( "Job", new JobPanel() );
        tabs.add( "Advanced", new AdvancedPanel() );
        contents.add( tabs, BorderLayout.CENTER );

        JButton okButton = new JButton( jEdit.getProperty( "common.ok" ) );
        okButton.addActionListener( new ActionListener()
        {

                public void actionPerformed( ActionEvent ae )
                {
                    for ( int i = 0; i < tabs.getTabCount(); i++ )
                    {
                        PrinterPanel panel = ( PrinterPanel )tabs.getComponentAt( i );
                        PrinterDialog.this.attributes.addAll( panel.getAttributes() );
                        PrinterDialog.this.setVisible( false );
                        PrinterDialog.this.dispose();
                    }
                    canceled = false;
                }
            }
        );
        JButton cancelButton = new JButton( jEdit.getProperty( "common.cancel" ) );
        cancelButton.addActionListener( new ActionListener()
        {

                public void actionPerformed( ActionEvent ae )
                {
                    PrinterDialog.this.setVisible( false );
                    PrinterDialog.this.dispose();
                    canceled = true;
                }
            }
        );

        JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 6, 6 ) );
        GenericGUIUtilities.makeSameSize( okButton, cancelButton );
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


        pack();
        setLocationRelativeTo( jEdit.getActiveView().getTextArea() );
        setVisible( true );
    }


    public PrintService getPrintService()
    {
        return selectedPrintService;
    }


    public AttributeSet getAttributes()
    {
        return attributes;
    }


    public boolean isCanceled()
    {
        return canceled;
    }


    private PrintService[] getPrintServices()
    {
        return PrinterJob.lookupPrintServices();
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
        String[] paperNames = new String [sizes.length];
        paperSizes = new ArrayList<Media>();
        int index = 0;
        for ( int i = 0; i < sizes.length; i++ )
        {
            MediaSizeName m = sizes[i];
            if ( MediaSizeName.NA_LETTER.equals( m ) )
            {
                index = i;
            }


            paperSizes.add( m );
            paperNames[i] = getMediaName( m.toString() );
        }
        paperSize.setModel( new DefaultComboBoxModel<String>( paperNames ) );
        paperSize.setEnabled( true );
        paperSize.setSelectedIndex( index );

        // finishing
        value = categoryValueMap.get( Finishings.class );
        if ( value == null )
        {
            finishing.setModel( new DefaultComboBoxModel<Finishings>() );
            finishing.setEnabled( false );
        }
        else
        {
            Finishings[] f = ( Finishings[] )value;
            if ( f.length == 0 || ( f.length == 1 && Finishings.NONE.equals( f[0] ) ) )
            {
                finishing.setModel( new DefaultComboBoxModel<Finishings>() );
                finishing.setEnabled( false );
            }
            else
            {
                finishing.setModel( new DefaultComboBoxModel<Finishings>( f ) );
                finishing.setEnabled( true );
            }
        }


        // sides
        value = categoryValueMap.get( Sides.class );
        if ( value == null )
        {
            sides.setEnabled( false );
        }
        else
        {
            sides.setModel( new DefaultComboBoxModel<Sides>( ( Sides[] )value ) );
            sides.setSelectedItem( Sides.ONE_SIDED );
            sides.setEnabled( true );
        }


        // pages per side
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


        // ordering of pages per side
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


        // paper source tray
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
            }
            else
            {
                paperSource.setEnabled( false );
            }
        }


        // orientation, eg. portrait or landscape
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
            orientation.setSelectedItem( OrientationRequested.PORTRAIT );
        }
    }



    private abstract class PrinterPanel extends JPanel
    {

        public PrinterPanel()
        {
            super( new FlowLayout( FlowLayout.LEFT, 11, 11 ) );
        }


        abstract AttributeSet getAttributes()
        ;
    }



    private class GeneralPanel extends PrinterPanel
    {

        JRadioButton allPages;
        JRadioButton pages;
        JCheckBox collate;
        JCheckBox reverse;
        JTextField pagesField;


        public GeneralPanel()
        {
            super();
            JPanel content = new JPanel( new GridLayout( 2, 1 ) );

            printers = new JList<PrintService>( getPrintServices() );
            printers.setCellRenderer( new PSCellRenderer() );
            printers.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
            printers.addListSelectionListener( PrinterDialog.this );
            content.add( new JScrollPane( printers ) );

            JPanel bottom = new JPanel( new GridLayout( 1, 2 ) );

            JPanel rangePanel = new JPanel( new GridLayout( 4, 2, 6, 6 ) );
            rangePanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Range" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            allPages = new JRadioButton( "All pages" );
            allPages.setSelected( true );
            JRadioButton currentPage = new JRadioButton( "Current page" );
            currentPage.setEnabled( false );    // TODO: implement this
            JRadioButton selection = new JRadioButton( "Selection" );
            selection.setEnabled( false );    // TODO: implement this
            pages = new JRadioButton( "Pages:" );
            pagesField = new JTextField();
            pagesField.setEnabled( false );
            pages.addActionListener( new ActionListener()
            {

                    public void actionPerformed( ActionEvent ae )
                    {
                        JRadioButton source = ( JRadioButton )ae.getSource();
                        pagesField.setEnabled( source.isSelected() );
                    }
                }
            );
            new MyButtonGroup( allPages, currentPage, selection, pages );
            Box pagesBox = Box.createHorizontalBox();
            pagesBox.add( pages );
            pagesBox.add( pagesField );
            rangePanel.add( allPages );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( currentPage );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( selection );
            rangePanel.add( Box.createGlue() );
            rangePanel.add( pagesBox );

            bottom.add( rangePanel );

            JPanel copiesPanel = new JPanel( new GridLayout( 3, 2, 6, 6 ) );
            copiesPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Copies" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            JLabel copiesLabel = new JLabel( "Copies:" );
            copies = new JSpinner( new SpinnerNumberModel( 1, 1, 999, 1 ) );
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
            collate = new JCheckBox( "Collate" );
            collate.setSelected( false );
            collate.setEnabled( false );
            reverse = new JCheckBox( "Reverse" );
            reverse.setEnabled( false );    // TODO: need to pass this to BufferPrintable
            copiesPanel.add( copiesLabel );
            copiesPanel.add( copies );
            copiesPanel.add( collate );
            copiesPanel.add( Box.createGlue() );
            copiesPanel.add( reverse );

            bottom.add( copiesPanel );
            content.add( bottom );
            add( content );
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();

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
            }


            if ( collate.isSelected() )
            {
                as.add( SheetCollate.COLLATED );
            }


            return as;
        }





        // print service cell renderer
        class PSCellRenderer extends JLabel implements ListCellRenderer <PrintService>
        {

            public PSCellRenderer()
            {
                setOpaque( true );
            }


            public Component getListCellRendererComponent( JList<? extends PrintService> list,
            PrintService value,
            int index,
            boolean isSelected,
            boolean cellHasFocus )
            {

                setText( value.getName() );

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

                return this;
            }
        }
    }



    private class PageSetupPanel extends PrinterPanel
    {

        private JComboBox<String> onlyPrint;
        private JComboBox<String> outputTray;


        public PageSetupPanel()
        {
            super();
            JPanel content = new JPanel( new BorderLayout() );

            JPanel layoutPanel = new JPanel( new GridLayout( 5, 2, 6, 6 ) );
            layoutPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Layout" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            sides = new JComboBox<Sides>();
            sides.setEnabled( false );

            pagesPerSide = new JComboBox<NumberUp>();
            pagesPerSide.setEnabled( false );
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

            // disable this when pagesPerSide is 1
            pageOrdering = new JComboBox<PresentationDirection>();
            pageOrdering.setEnabled( false );

            // TODO: implement this
            onlyPrint = new JComboBox<String>();
            onlyPrint.addItem( "All sheets" );
            onlyPrint.addItem( "Even sheets" );
            onlyPrint.addItem( "Odd sheets" );
            onlyPrint.setSelectedIndex( 0 );
            onlyPrint.setEnabled( false );

            // TODO: scale?
            layoutPanel.add( new JLabel( "Two-sided:" ) );
            layoutPanel.add( sides );
            layoutPanel.add( new JLabel( "Pages per side:" ) );
            layoutPanel.add( pagesPerSide );
            layoutPanel.add( new JLabel( "Page ordering:" ) );
            layoutPanel.add( pageOrdering );
            layoutPanel.add( new JLabel( "Only print:" ) );
            layoutPanel.add( onlyPrint );

            content.add( layoutPanel, BorderLayout.NORTH );
            content.add( Box.createHorizontalStrut( 11 ) );

            JPanel paperPanel = new JPanel( new GridLayout( 5, 2, 6, 6 ) );
            paperPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Paper" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );

            paperSource = new JComboBox<MediaTray>();
            paperSource.setEnabled( false );

            outputTray = new JComboBox<String>();
            outputTray.setEnabled( false );

            paperSize = new JComboBox<String>();
            paperSize.setEnabled( false );

            orientation = new JComboBox<OrientationRequested>();
            orientation.setEnabled( false );

            paperPanel.add( new JLabel( "Paper source:" ) );
            paperPanel.add( paperSource );
            paperPanel.add( new JLabel( "Output tray:" ) );
            paperPanel.add( outputTray );
            paperPanel.add( new JLabel( "Paper size:" ) );
            paperPanel.add( paperSize );
            paperPanel.add( new JLabel( "Orientation:" ) );
            paperPanel.add( orientation );

            content.add( paperPanel, BorderLayout.SOUTH );
            add( content );
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


            return as;
        }
    }



    private class JobPanel extends PrinterPanel
    {

        // need 3 subpanels, Job Details, Print Document, and Finishing
        public JobPanel()
        {
            super();
            Box content = Box.createHorizontalBox();

            JPanel jobPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 6, 6 ) );
            jobPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Job Details" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            priority = new JComboBox<Priority>();
            priority.addItem( Priority.LOW );
            priority.addItem( Priority.MEDIUM );
            priority.addItem( Priority.HIGH );
            priority.addItem( Priority.URGENT );
            priority.setSelectedItem( Priority.MEDIUM );

            jobPanel.add( new JLabel( "Priority" ) );
            jobPanel.add( Box.createHorizontalStrut( 6 ) );
            jobPanel.add( priority );

            JPanel printPanel = new JPanel( new GridLayout( 3, 2, 6, 6 ) );
            printPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Print Document" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            JRadioButton nowButton = new JRadioButton( "Now" );
            nowButton.setSelected( true );
            JRadioButton atButton = new JRadioButton( "At" );
            atButton.setEnabled( false );

            // JobHoldUntil
            JTextField when = new JTextField();    // need document for Date
            when.setEnabled( false );
            JRadioButton holdButton = new JRadioButton( "On Hold" );
            holdButton.setEnabled( false );
            new MyButtonGroup( nowButton, atButton, holdButton );
            printPanel.add( nowButton );
            printPanel.add( Box.createGlue() );
            printPanel.add( atButton );
            printPanel.add( when );
            printPanel.add( holdButton );
            printPanel.add( Box.createGlue() );

            JPanel finishingPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 6, 6 ) );
            finishingPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Finishing" ),
            BorderFactory.createEmptyBorder( 11, 11, 11, 11 ) ) );
            finishing = new JComboBox<Finishings>();
            finishing.setEnabled( false );
            finishingPanel.add( new JLabel( "Finishing:" ) );
            finishingPanel.add( Box.createHorizontalStrut( 6 ) );
            finishingPanel.add( finishing );

            content.add( jobPanel );
            content.add( Box.createVerticalStrut( 11 ) );
            content.add( printPanel );
            content.add( Box.createVerticalStrut( 11 ) );
            content.add( finishingPanel );
            add( content );
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();
            as.add( new JobPriority( ( ( Priority )priority.getSelectedItem() ).getValue() ) );
            if ( finishing.isEnabled() )
            {
                as.add( ( Finishings )finishing.getSelectedItem() );
            }


            return as;
        }
    }



    private static class Priority
    {

        public static final Priority LOW = new Priority( 1, "Low" );
        public static final Priority MEDIUM = new Priority( 50, "Medium" );
        public static final Priority HIGH = new Priority( 80, "High" );
        public static final Priority URGENT = new Priority( 100, "Urgent" );
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
            JPanel content = new JPanel( new GridLayout( 2, 2, 6, 6 ) );

            quality = new JComboBox<PrintQuality>();
            quality.addItem( PrintQuality.DRAFT );
            quality.addItem( PrintQuality.NORMAL );
            quality.addItem( PrintQuality.HIGH );
            quality.setSelectedItem( PrintQuality.NORMAL );

            chromaticity = new JComboBox<Chromaticity>();
            chromaticity.addItem( Chromaticity.MONOCHROME );
            chromaticity.addItem( Chromaticity.COLOR );
            chromaticity.setSelectedItem( Chromaticity.MONOCHROME );

            content.add( new JLabel( "Quality" ) );
            content.add( quality );
            content.add( new JLabel( "Ink" ) );
            content.add( chromaticity );
            add( content );
        }


        public AttributeSet getAttributes()
        {
            AttributeSet as = new HashAttributeSet();
            as.add( ( Chromaticity )chromaticity.getSelectedItem() );
            as.add( ( PrintQuality )quality.getSelectedItem() );
            return as;
        }
    }





    class MyButtonGroup extends ButtonGroup
    {

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
    // readable names.
    // TODO: put the values in jEdit properties so they can be localized.
    public void initMessages()
    {
        messageMap = new HashMap<String, String>();
        messageMap.put( "Automatic-Feeder", "Automatic Feeder" );
        messageMap.put( "Cassette", "Cassette" );
        messageMap.put( "Form-Source", "Form Source" );
        messageMap.put( "Large-Format", "Large Format" );
        messageMap.put( "Manual-Envelope", "Manual Envelope" );
        messageMap.put( "Small-Format", "Small Format" );
        messageMap.put( "Tractor-Feeder", "Tractor Feeder" );
        messageMap.put( "a", "Engineering A" );
        messageMap.put( "accepting-jobs", "Accepting jobs" );
        messageMap.put( "auto-select", "Automatically Select" );
        messageMap.put( "b", "Engineering B" );
        messageMap.put( "c", "Engineering C" );
        messageMap.put( "d", "Engineering D" );
        messageMap.put( "e", "Engineering E" );
        messageMap.put( "envelope", "Envelope" );
        messageMap.put( "executive", "Executive" );
        messageMap.put( "folio", "Folio" );
        messageMap.put( "invite-envelope", "Invitation Envelope" );
        messageMap.put( "invoice", "Invoice" );
        messageMap.put( "iso-2a0", "2A0 (ISO/DIN & JIS)" );
        messageMap.put( "iso-4a0", "4A0 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a0", "A0 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a1", "A1 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a10", "A10 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a2", "A2 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a3", "A3 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a4", "A4 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a5", "A5 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a6", "A6 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a7", "A7 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a8", "A8 (ISO/DIN & JIS)" );
        messageMap.put( "iso-a9", "A9 (ISO/DIN & JIS)" );
        messageMap.put( "iso-b0", "B0 (ISO/DIN)" );
        messageMap.put( "iso-b1", "B1 (ISO/DIN)" );
        messageMap.put( "iso-b10", "B10 (ISO/DIN)" );
        messageMap.put( "iso-b2", "B2 (ISO/DIN)" );
        messageMap.put( "iso-b3", "B3 (ISO/DIN)" );
        messageMap.put( "iso-b4", "B4 (ISO/DIN)" );
        messageMap.put( "iso-b5", "B5 (ISO/DIN)" );
        messageMap.put( "iso-b6", "B6 (ISO/DIN)" );
        messageMap.put( "iso-b7", "B7 (ISO/DIN)" );
        messageMap.put( "iso-b8", "B8 (ISO/DIN)" );
        messageMap.put( "iso-b9", "B9 (ISO/DIN)" );
        messageMap.put( "iso-c0", "C0 (ISO/DIN)" );
        messageMap.put( "iso-c1", "C1 (ISO/DIN)" );
        messageMap.put( "iso-c10", "C10 (ISO/DIN)" );
        messageMap.put( "iso-c2", "C2 (ISO/DIN)" );
        messageMap.put( "iso-c3", "C3 (ISO/DIN)" );
        messageMap.put( "iso-c4", "C4 (ISO/DIN)" );
        messageMap.put( "iso-c5", "C5 (ISO/DIN)" );
        messageMap.put( "iso-c6", "C6 (ISO/DIN)" );
        messageMap.put( "iso-c7", "C7 (ISO/DIN)" );
        messageMap.put( "iso-c8", "C8 (ISO/DIN)" );
        messageMap.put( "iso-c9", "C9 (ISO/DIN)" );
        messageMap.put( "iso-designated-long", "ISO Designated Long" );
        messageMap.put( "italian-envelope", "Italy Envelope" );
        messageMap.put( "italy-envelope", "Italy Envelope" );
        messageMap.put( "japanese-postcard", "Postcard (JIS)" );
        messageMap.put( "jis-b0", "B0 (JIS)" );
        messageMap.put( "jis-b1", "B1 (JIS)" );
        messageMap.put( "jis-b10", "B10 (JIS)" );
        messageMap.put( "jis-b2", "B2 (JIS)" );
        messageMap.put( "jis-b3", "B3 (JIS)" );
        messageMap.put( "jis-b4", "B4 (JIS)" );
        messageMap.put( "jis-b5", "B5 (JIS)" );
        messageMap.put( "jis-b6", "B6 (JIS)" );
        messageMap.put( "jis-b7", "B7 (JIS)" );
        messageMap.put( "jis-b8", "B8 (JIS)" );
        messageMap.put( "jis-b9", "B9 (JIS)" );
        messageMap.put( "main", "Main" );
        messageMap.put( "manual", "Manual" );
        messageMap.put( "middle", "Middle" );
        messageMap.put( "monarch-envelope", "Monarch Envelope" );
        messageMap.put( "na-10x13-envelope", "10x15 Envelope" );
        messageMap.put( "na-10x14-envelope", "10x15 Envelope" );
        messageMap.put( "na-10x15-envelope", "10x15 Envelope" );
        messageMap.put( "na-5x7", "5\" x 7\" Paper" );
        messageMap.put( "na-6x9-envelope", "6x9 Envelope" );
        messageMap.put( "na-7x9-envelope", "6x7 Envelope" );
        messageMap.put( "na-8x10", "8\" x 10\" Paper" );
        messageMap.put( "na-9x11-envelope", "9x11 Envelope" );
        messageMap.put( "na-9x12-envelope", "9x12 Envelope" );
        messageMap.put( "na-legal", "Legal" );
        messageMap.put( "na-letter", "Letter" );
        messageMap.put( "na-number-10-envelope", "No. 10 Envelope" );
        messageMap.put( "na-number-11-envelope", "No. 11 Envelope" );
        messageMap.put( "na-number-12-envelope", "No. 12 Envelope" );
        messageMap.put( "na-number-14-envelope", "No. 14 Envelope" );
        messageMap.put( "na-number-9-envelope", "No. 9 Envelope" );
        messageMap.put( "not-accepting-jobs", "Not accepting jobs" );
        messageMap.put( "oufuko-postcard", "Double Postcard (JIS)" );
        messageMap.put( "personal-envelope", "Personal Envelope" );
        messageMap.put( "quarto", "Quarto" );
        messageMap.put( "side", "Side" );
        messageMap.put( "tabloid", "Tabloid" );
        messageMap.put( "top", "Top" );
    }


    private String getMediaName( String key )
    {
        String value = messageMap.get( key );
        return value == null ? key : value;
    }


    public static void main( String[] args )
    {
        new PrinterDialog( null, null );
        System.exit( 0 );
    }
}
