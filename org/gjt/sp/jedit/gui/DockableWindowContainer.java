/*
 * DockableWindowContainer.java - holds dockable windows
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public interface DockableWindowContainer
{
	void add(DockableWindowManager.Entry entry);
	void save(DockableWindowManager.Entry entry);
	void remove(DockableWindowManager.Entry entry);
	void show(DockableWindowManager.Entry entry);
	boolean isVisible(DockableWindowManager.Entry entry);
}
