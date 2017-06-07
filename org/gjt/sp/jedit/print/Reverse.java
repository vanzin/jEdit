/*
 * Reverse.java - Print attribute indicating reverse print order
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) Dale Anson
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
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;


/**
 * Custom print attribute indicating the pages should be printed in reverse
 * order. This is just a marker attribute, if present, pages should be printed
 * in reverse order, if not present, then print pages in forward order.
 */
public class Reverse implements DocAttribute, PrintRequestAttribute, PrintJobAttribute
{

    private static final long serialVersionUID = -2823970704630722439L;

    public boolean equals( Object object )
    {
        return object != null && object instanceof Reverse && object.getClass() == this.getClass();
    }

    public final Class< ?  extends Attribute> getCategory()
    {
        return Reverse.class;
    }

    public final String getName()
    {
        return "reverse";
    }
}

