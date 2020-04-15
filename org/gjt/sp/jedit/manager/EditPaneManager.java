/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.manager;

import org.gjt.sp.jedit.EditPane;

import java.util.List;
import java.util.function.Consumer;

/**
 * The EditPane manager {@link org.gjt.sp.jedit.EditPane} provide
 * useful tools to list and iterate over EditPanes
 *
 * @author Matthieu Casanova
 * @since jEdit 5.6pre1
 * @version $Id: jEdit.java 25120 2020-04-03 14:58:39Z kpouer $
 */
public interface EditPaneManager
{
	List<EditPane> getEditPanes();

	void forEach(Consumer<? super EditPane> action);
}
