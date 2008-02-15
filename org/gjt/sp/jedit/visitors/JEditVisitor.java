/*
 * JEditVisitor.java - A Visitor pattern for jEdit
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

package org.gjt.sp.jedit.visitors;

//{{{ Imports
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
//}}}

/**
 * A visitor that can visit a View, an EditPane or a JEditTextArea.
 * You can also extends the default implementation JEditVisitorAdapter
 * To use this visitor.
 * @see jEdit#visit(JEditVisitor)
 * @see View#visit(JEditVisitor)
 *
 * @author Matthieu Casanova
 * @version $Id: GUIUtilities.java 11797 2008-02-15 00:07:23Z Vampire0 $
 * @since jEdit 4.3pre13
 */
public interface JEditVisitor
{
	/**
	 * Visit a view.
	 * @param view the visited view
	 */
	void visit(View view);
		   
	/**
	 * Visit an EditPane.
	 * @param editPane the visited edit pane
	 */
	void visit(EditPane editPane);

	/**
	 * Visit a view.
	 * @param textArea the visited textArea
	 */
	void visit(JEditTextArea textArea);
}
