/*
 * Widget.java - The status bar widget interface
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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

package org.gjt.sp.jedit.gui.statusbar;

import javax.swing.JComponent;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.3pre14
 */
public interface Widget 
{
	/** 
	 * Returns the component that will be inserted in the status bar
	 * @return a JComponent
	 */
	JComponent getComponent();
	
	/**
	 * a callback telling that the properties have been changed, the widget
	 * can update itself if needed
	 */
	void propertiesChanged();
	
	/** A refresh is asked to the widget */
	void update();
}
