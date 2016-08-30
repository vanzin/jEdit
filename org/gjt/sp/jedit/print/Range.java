/*
 * Range.java
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

/**
 * Defines a range of integers from <code>start</code> to <code>end</code>,
 * inclusive.
 */
public class Range
{

    private int start = 0;
    private int end = 0;


    public Range( int start, int end )
    {
        this.start = start;
        this.end = end;
    }


    public int getStart()
    {
        return start;
    }


    public int getEnd()
    {
        return end;
    }


    public boolean contains( int i )
    {
        return i >= start && i <= end;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Range[" ).append( start ).append( ", " ).append( end ).append( ']' );
        return sb.toString();
    }
}
