/*
 * TextAreaHighlight.java - Paints custom highlights in the text area
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import java.awt.Graphics;
import java.awt.event.MouseEvent;

/**
 * @deprecated Use the new, soft wrap-aware, <code>TextAreaExtension</code>
 * class instead.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public interface TextAreaHighlight
{
	/**
	 * @deprecated se the new, soft wrap-aware,
	 * <code>TextAreaExtension</code> interface instead.
	 */
	void paintHighlight(Graphics gfx, int line, int y);

	/**
	 * @deprecated se the new, soft wrap-aware,
	 * <code>TextAreaExtension</code> class instead.
	 */
	String getToolTipText(MouseEvent evt);
}
