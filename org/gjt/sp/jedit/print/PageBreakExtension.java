
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

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
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
        showPageBreak = jEdit.getBooleanProperty( "view.pageBreaks", false );
        pageBreakColor = jEdit.getColorProperty( "view.pageBreaksColor" );
        EditBus.addToBus( this );
    }


    private void loadPageRanges()
    {
        if ( showPageBreak )
        {
            View view = textArea.getView();
            Buffer buffer = ( Buffer )textArea.getBuffer();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(new PageRanges("1-1000"));
            pages = BufferPrinter1_7.getPageRanges( view, buffer, attributes );
        }
        else
        {
            pages = null;
        }
    }


    public void handleMessage( EBMessage msg )
    {
        if ( msg instanceof PropertiesChanged )
        {
            showPageBreak = jEdit.getBooleanProperty( "view.pageBreaks" );
            pageBreakColor = jEdit.getColorProperty( "view.pageBreaksColor" );
            loadPageRanges();
        }
        else
        if ( msg instanceof EditPaneUpdate )
        {
            EditPaneUpdate epu = ( EditPaneUpdate )msg;
            if ( EditPaneUpdate.BUFFER_CHANGED.equals( epu.getWhat() ) )
            {

                // prevent NPE in Buffer#markToken() when edit mode is not loaded
                if ( epu.getEditPane().getBuffer().isLoaded() )
                {
                    loadPageRanges();
                }
            }
        }
        else
        if ( msg instanceof BufferUpdate )
        {
            BufferUpdate bu = ( BufferUpdate )msg;
            if ( BufferUpdate.SAVED.equals( bu.getWhat() ) || BufferUpdate.LOADED.equals( bu.getWhat() ) )
            {
                loadPageRanges();
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
        if ( showPageBreak )
        {

            if ( pages == null || pages.isEmpty() )
            {
                loadPageRanges();
                if ( pages == null || pages.isEmpty() )
                {
                    return;
                }
            }


            gfx.setColor( pageBreakColor );

            // - 1 so last page break isn't drawn
            for ( int page = 1; page < pages.size(); page++ )
            {
                Range range = pages.get( page );

                // 2nd part of 'if' handles soft wrap so if the last line of the page
                // is wrapped, only the last screen line of the wrapped line will get
                // the page break line drawn on it.
                if ( range != null && range.getEnd() == physicalLine && textArea.getLineEndOffset( physicalLine ) == end )
                {
                    y += gfx.getFontMetrics().getHeight();
                    gfx.drawLine( 0, y, textArea.getPainter().getWidth(), y );
                }
            }
        }
    }
}
