
package org.gjt.sp.jedit.print;


import java.awt.print.PageFormat;
import java.util.HashMap;

import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;


public class PrintPreviewModel extends PageFormat
{

    private View view;
    private Buffer buffer;
    private PrintService printService;
    private PrintRequestAttributeSet attributes;
    private HashMap<Integer, Range> pageRanges;
    private int pageNumber = 1;


    public PrintPreviewModel()
    {
    }


    public PrintPreviewModel( View view, Buffer buffer, PrintService printService, PrintRequestAttributeSet attributes, HashMap<Integer, Range> pageRanges )
    {
        this.view = view;
        this.buffer = buffer;
        this.printService = printService;
        this.attributes = attributes;
        this.pageRanges = pageRanges;
    }
    
    public int getPageNumber()
    {
        return pageNumber;   
    }
    
    public void setPageNumber(int number)
    {
        pageNumber = number;   
    }


    public HashMap<Integer, Range> getPageRanges()
    {
        return pageRanges;
    }


    public void setPageRanges( HashMap<Integer, Range> pageRanges )
    {
        this.pageRanges = pageRanges;
    }


    /**
     * Returns the value of printService.
     */
    public PrintService getPrintService()
    {
        return printService;
    }


    /**
     * Sets the value of printService.
     * @param printService The value to assign printService.
     */
    public void setPrintService( PrintService printService )
    {
        this.printService = printService;
    }


    /**
     * Returns the value of attributes.
     */
    public PrintRequestAttributeSet getAttributes()
    {
        return attributes;
    }


    /**
     * Sets the value of attributes.
     * @param attributes The value to assign attributes.
     */
    public void setAttributes( PrintRequestAttributeSet attributes )
    {
        this.attributes = attributes;
    }


    /**
     * Returns the value of view.
     */
    public View getView()
    {
        return view;
    }


    /**
     * Sets the value of view.
     * @param view The value to assign view.
     */
    public void setView( View view )
    {
        this.view = view;
    }


    /**
     * Returns the value of buffer.
     */
    public Buffer getBuffer()
    {
        return buffer;
    }


    /**
     * Sets the value of buffer.
     * @param buffer The value to assign buffer.
     */
    public void setBuffer( Buffer buffer )
    {
        this.buffer = buffer;
    }
}
