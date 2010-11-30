/*
 * SquareFoldPainter.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=0:
 *
 * Copyright (C) 2008 Shlomy Reinstein
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

import java.awt.Graphics2D;

// {{{ SquareFoldPainter
public class SquareFoldPainter extends ShapedFoldPainter
{
	@Override
	protected void paintFoldShape(Graphics2D gfx, int top, int bottom)
	{
		gfx.drawRect(1,top,8,(bottom-top));
	}

} // }}}
