/*
 * PrintPreviewPane.java
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


import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Custom component to display the current page of the current buffer.
 */
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
