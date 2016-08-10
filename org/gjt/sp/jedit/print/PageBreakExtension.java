
/*
 * PageBreakExtension.java
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


import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.TextAreaPainter;


/**
 * Draws a line across the text area indicating where a printing page break
 * would be.
 */
public class PageBreakExtension extends TextAreaExtension implements EBComponent
{

    private JEditTextArea textArea;
    private boolean showPageBreak;
    private Color pageBreakColor;
    private HashMap<Integer, Range> pages = null;


    public PageBreakExtension( JEditTextArea textArea )
    {
        this.textArea = textArea;
        textArea.getPainter().addExtension( TextAreaPainter.WRAP_GUIDE_LAYER, this );
        showPageBreak = jEdit.getBooleanProperty( "view.pageBreaks" );
        pageBreakColor = jEdit.getColorProperty( "view.pageBreaks.color" );
        EditBus.addToBus( this );
    }


    private HashMap<Integer, Range> getPageRanges()
    {
        View view = textArea.getView();
        Buffer buffer = ( Buffer )textArea.getBuffer();
        return BufferPrinter1_7.getPageRanges( view, buffer );
    }


    public void handleMessage( EBMessage msg )
    {
        if ( msg instanceof PropertiesChanged )
        {
            showPageBreak = jEdit.getBooleanProperty( "view.pageBreaks" );
            pageBreakColor = jEdit.getColorProperty( "view.pageBreaks.color" );
        }
        else
        if ( msg instanceof EditPaneUpdate )
        {
            EditPaneUpdate epu = ( EditPaneUpdate )msg;
            if ( EditPaneUpdate.BUFFER_CHANGED.equals( epu.getWhat() ) )
            {
                pages = getPageRanges();
            }
        }
        else
        if ( msg instanceof BufferUpdate )
        {
            BufferUpdate bu = ( BufferUpdate )msg;
            if (BufferUpdate.SAVED.equals(bu.getWhat()) || BufferUpdate.LOADED.equals(bu.getWhat()))
            {
                pages = getPageRanges();        
            }
        }
    }


    public Color getPageBreakColor()
    {
        return pageBreakColor;
    }


    public void setPageBreakColor( Color pageBreakColor )
    {
        this.pageBreakColor = pageBreakColor;
    }


    public boolean isPageBreakEnabled()
    {
        return showPageBreak;
    }


    public void setPageBreakEnabled( boolean pageBreak )
    {
        showPageBreak = pageBreak;
    }


    @Override
    public void paintValidLine( Graphics2D gfx, int screenLine, int physicalLine, int start, int end, int y )
    {
        if ( isPageBreakEnabled() )
        {
            if ( pages == null )
            {
                pages = getPageRanges();
            }


            for ( Integer page : pages.keySet() )
            {
                Range range = pages.get( page );
                if ( range.getEnd() == physicalLine + 1 )
                {
                    gfx.drawLine( 0, y, textArea.getPainter().getWidth(), y );
                }
            }
        }
    }
}
