/*
 * QuickNotepadPlugin.java
 * part of the QuickNotepad plugin for the jEdit text editor
 * Copyright (C) 2001 John Gellene
 * jgellene@nyc.rr.com
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
 *
 * $Id$
 */

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.gui.*;


/**
 * The QuickNotepad plugin
 *
 * @author John Gellene
 */
public class QuickNotepadPlugin extends EditPlugin
{
    public static final String NAME = "quicknotepad";
	public static final String MENU = "quicknotepad.menu";
    public static final String PROPERTY_PREFIX = "plugin.QuickNotepadPlugin.";
    public static final String OPTION_PREFIX = "options.quicknotepad.";

    public void start()
	{
    }


	public void stop()
	{
	}


    public void createMenuItems(Vector menuItems)
	{
        menuItems.addElement(GUIUtilities.loadMenu(MENU));
    }


    public void createOptionPanes(OptionsDialog od)
	{
        od.addOptionPane(new QuickNotepadOptionPane());
    }

}

