/*
 * Margins.java
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

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import static javax.print.attribute.Size2DSyntax.*;


/**
 * Custom printing attribute to represent page margins.
 */
public class Margins implements DocAttribute, PrintRequestAttribute, PrintJobAttribute
{

    // margins are stored in micromillimeters
    private float top;
    private float left;
    private float right;
    private float bottom;

    // need serial version since this is serialized
    private static final long serialVersionUID = 5343792322705104289L;

    public Margins( float top, float left, float right, float bottom )
    {
        if ( top < 0.0 || left < 0.0 || right < 0.0 || bottom < 0.0 )
        {

            // this shouldn't happen since the printer dialog margin text fields
            // only accept positive numbers
            throw new IllegalArgumentException( "Invalid margin." );
        }

        float u = Integer.valueOf( getUnits() ).floatValue();
        this.top = top * u;
        this.left = left * u;
        this.right = right * u;
        this.bottom = bottom * u;
    }


    // returns INCH or MM depending on Locale
    // note that while Canada is mostly metric, Canadian paper sizes
    // are essentially US ANSI sizes rounded to the nearest 5 mm
    private int getUnits()
    {
        String country = Locale.getDefault().getCountry();
        if ( "".equals( country ) || Locale.US.getCountry().equals( country ) || Locale.CANADA.getCountry().equals( country ) )
        {
            return INCH;
        }


        return MM;
    }


    /**
     * Get the margins as an array of 4 values in the order
     * top, left, right, bottom. The values returned are in the given units.
     * @param  units Unit conversion factor, either INCH or MM.
     *
     * @return margins as array of top, left, right, bottom in the specified units.
     *
     * @exception  IllegalArgumentException on invalid units.
     */
    public float[] getMargins( int units )
    {
        switch ( units )

        {
            case INCH:
            case MM:
                break;
            default:
                throw new IllegalArgumentException( "Invalid units." );
        }

        return new float[] {getTop( units ), getLeft( units ), getRight( units ), getBottom( units )};
    }


    public float getTop( int units )
    {
        return convertFromMicrometers( top, units );
    }


    public float getLeft( int units )
    {
        return convertFromMicrometers( left, units );
    }


    public float getRight( int units )
    {
        return convertFromMicrometers( right, units );
    }


    public float getBottom( int units )
    {
        return convertFromMicrometers( bottom, units );
    }


    public final Class<? extends Attribute> getCategory()
    {
        return Margins.class;
    }


    public final String getName()
    {
        return "margins";
    }


    private float convertFromMicrometers( float margin, int units )
    {
        return margin / Integer.valueOf( units ).floatValue();
    }

    public String toString()
    {
        return toString(INCH);   
    }

    public String toString( int units )
    {
        String uom = "";
        switch ( units )

        {
            case INCH:
                uom = "in";
                break;
            case MM:
                uom = "mm";
                break;
            default:
                throw new IllegalArgumentException( "Invalid units." );
        }


        float[] margins = getMargins( units );
        StringBuilder sb = new StringBuilder(128);
        sb.append( "Margins(" ).append( uom ).append( ")[top:" ).append( margins[0] ).append( ", left:" );
        sb.append( margins[1] ).append( ", right:" ).append( margins[2] ).append( ", bottom:" ).append( margins[3] ).append( ']' );
        return sb.toString();
    }


    public boolean equals( Object object )
    {
        boolean toReturn = false;
        if ( object instanceof Margins )
        {
            Margins margins = ( Margins )object;
            if ( top == margins.top && left == margins.left && bottom == margins.bottom && right == margins.right )
            {
                toReturn = true;
            }
        }


        return toReturn;
    }


    public int hashCode()
    {
        return Float.valueOf(top).intValue() + 37 * Float.valueOf(left).intValue() + 43 * Float.valueOf(right).intValue() + 47 * Float.valueOf(bottom).intValue();
    }
}
