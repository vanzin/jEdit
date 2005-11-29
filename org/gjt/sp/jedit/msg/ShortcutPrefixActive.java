/*
 *  ShortcutPrefixActive.java - Message sent when a shortcut prefix is typed
 *  or the prefix is no longer active (e.g., an action was fired or the user
 *  typed ESC
 *
 *  Copyright (C) 2005 Jeffrey Hoyt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.gjt.sp.jedit.msg;

import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;

/**
 *  Description of the Class
 *
 *@author     jchoyt
 *@created    November 7, 2005
 */
public class ShortcutPrefixActive extends EBMessage
{

    /**
     *  Description of the Field
     */
    protected Hashtable bindings;
    /**
     *  Description of the Field
     */
    protected boolean active;

    //{{{
    /**
     *  Creates a new editor exiting message.
     *
     *@param  source  The message source
     */
    public ShortcutPrefixActive( EBComponent source )
    {
        super( source );
        bindings = null;
        active = false;
    }//}}}


    //{{{
    /**
     *  Constructor for the ShortcutPrefixActive object
     *
     *@param  source    Description of the Parameter
     *@param  bindings  Description of the Parameter
     *@param  active    Description of the Parameter
     */
    public ShortcutPrefixActive( EBComponent source, Hashtable bindings, boolean active )
    {
        super( source );
        this.bindings = bindings;
        this.active = active;
    }//}}}


    //{{{
    /**
     *  Gets the bindings attribute of the ShortcutPrefixActive object
     *
     *@return    The bindings value
     */
    public Hashtable getBindings()
    {
        return bindings;
    }//}}}

    //{{{
    /**
     *  Gets the active attribute of the ShortcutPrefixActive object
     *
     *@return    The active value
     */
    public boolean getActive()
    {
        return active;
    }
    //}}}

    //{{{ paramString() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public String paramString()
    {
        return "active=" + active + ",bindings=" + bindings + "," 
		+ super.paramString();
    }//}}}
}
/*
 *  :tabSize=8:indentSize=8:noTabs=false:
 *  :folding=explicit:collapseFolds=1:
 */

