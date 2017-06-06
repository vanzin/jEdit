/*
 * PrintRangeType.java
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


import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Custom print request attribute to represent a page range type, i.e. all pages,
 * current page, range of pages, just a selection of lines, or only odd or even
 * pages.
 */
public class PrintRangeType extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute
{

    private static final long serialVersionUID = -6426631421680023833L;
    public static PrintRangeType ALL = new PrintRangeType( 0 );
    public static PrintRangeType ODD = new PrintRangeType( 1 );
    public static PrintRangeType EVEN = new PrintRangeType( 2 );
    public static PrintRangeType RANGE = new PrintRangeType( 3 );
    public static PrintRangeType CURRENT_PAGE = new PrintRangeType( 4 );
    public static PrintRangeType SELECTION = new PrintRangeType( 5 );

    public PrintRangeType( int value )
    {
        super( value, 0, 5 );
    }

    public boolean equals( Object object )
    {
        return super.equals( object ) && object instanceof PrintRangeType;
    }

    public final Class< ?  extends Attribute> getCategory()
    {
        return PrintRangeType.class;
    }

    public final String getName()
    {
        return "printRangeType";
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder( "PageRangeType: " );
        switch ( getValue() )
        {
            case 0:
                sb.append( "0 ALL" );
                break;
            case 1:
                sb.append( "1 ODD" );
                break;
            case 2:
                sb.append( "2 EVEN" );
                break;
            case 3:
                sb.append( "3 RANGE" );
                break;
            case 4:
                sb.append( "4 CURRENT_PAGE" );
                break;
            case 6:
                sb.append( "5 SELECTION" );
                break;
        }

        return sb.toString();
    }
}
