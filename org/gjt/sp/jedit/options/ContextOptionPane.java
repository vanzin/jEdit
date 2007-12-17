/*
 * ContextOptionPane.java - Context menu options panel
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.options;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;

/**
 * Right-click context menu editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ContextOptionPane extends AbstractContextOptionPane
{

	public ContextOptionPane()
	{
		super("context", jEdit.getProperty("options.context.caption"));
	}

    /**
	 * Returns jEdit's context menu configuration.
	 *
	 * @since jEdit 4.3pre13
	 */
    protected String getContextMenu()
    {
		return jEdit.getProperty("view.context");
	}

    /**
	 * Saves jEdit's context menu configuration.
	 *
	 * @since jEdit 4.3pre13
	 */
    protected void saveContextMenu(String menu)
    {
		jEdit.setProperty("view.context", menu);
    }

}
