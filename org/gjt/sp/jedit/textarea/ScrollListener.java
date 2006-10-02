/*
 * ScrollListener.java - Text area scroll listener
 * Copyright (C) 2001 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

/**
 * A scroll listener will be notified when the text area is scrolled, either
 * horizontally or vertically.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 3.2pre2
 */
public interface ScrollListener extends java.util.EventListener
{
	void scrolledVertically(TextArea textArea);
	void scrolledHorizontally(TextArea textArea);
}
