
package org.gjt.sp.jedit.print;


import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class PrintPreviewPane extends JComponent
{

    private static final String uiClassID = "PrintPreviewPaneUI";
    private PrintPreviewModel printPreviewModel = null;
    private Set<ChangeListener> changeListeners = new HashSet<ChangeListener>();


    public PrintPreviewPane()
    {
        updateUI();
    }


    public void updateUI()
    {
        if ( UIManager.get( getUIClassID() ) != null )
        {
            setUI( ( PrintPreviewPaneUI )UIManager.getUI( this ) );
        }
        else
        {
            setUI( new BasicPrintPreviewPaneUI() );
        }


        fireStateChanged();
    }


    public PrintPreviewPaneUI getUI()
    {
        return ( PrintPreviewPaneUI )ui;
    }


    public String getUIClassID()
    {
        return uiClassID;
    }


    public void addChangeListener( ChangeListener cl )
    {
        if ( cl != null )
        {
            changeListeners.add( cl );
        }
    }


    public void removeChangeListener( ChangeListener cl )
    {
        if ( cl != null )
        {
            changeListeners.remove( cl );
        }
    }


    public void fireStateChanged()
    {
        if ( changeListeners.size() > 0 )
        {
            ChangeEvent event = new ChangeEvent( this );
            for ( ChangeListener cl : changeListeners )
            {
                cl.stateChanged( event );
            }
        }
    }


    public void setModel( PrintPreviewModel model )
    {
        printPreviewModel = model;
        fireStateChanged();
    }


    public PrintPreviewModel getModel()
    {
        return printPreviewModel;
    }


    public void clear()
    {
        setModel( null );
    }


    public void reset()
    {
        fireStateChanged();
    }
}
