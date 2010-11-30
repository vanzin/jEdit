/*
 * ScrollLayout.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Slava Pestov
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

//{{{ Imports
import java.awt.*;
import javax.swing.border.Border;
import javax.swing.JComponent;
//}}}

public class ScrollLayout implements LayoutManager
{
	public static final String CENTER = "center";
	public static final String RIGHT = "right";
	public static final String LEFT = "left";
	public static final String BOTTOM = "bottom";
	public static final String TOP = "top";

	//{{{ addLayoutComponent() method
	public void addLayoutComponent(String name, Component comp)
	{
		if(name.equals(CENTER))
			center = comp;
		else if(name.equals(RIGHT))
			right = comp;
		else if(name.equals(LEFT))
			left = comp;
		else if(name.equals(BOTTOM))
			bottom = comp;
		else if(name.equals(TOP))
			top = comp;
	} //}}}

	//{{{ removeLayoutComponent() method
	public void removeLayoutComponent(Component comp)
	{
		if(center == comp)
			center = null;
		else if(right == comp)
			right = null;
		else if(left == comp)
			left = null;
		else if(bottom == comp)
			bottom = null;
		else if(top == comp)
			top = null;
	} //}}}

	//{{{ preferredLayoutSize() method
	public Dimension preferredLayoutSize(Container parent)
	{
		Dimension dim = new Dimension();
		Insets insets = getInsets(parent);

		dim.width = insets.left + insets.right;
		dim.height = insets.top + insets.bottom;

		Dimension leftPref = left.getPreferredSize();
		dim.width += leftPref.width;
		Dimension centerPref = center.getPreferredSize();
		dim.width += centerPref.width;
		dim.height += centerPref.height;
		Dimension rightPref = right.getPreferredSize();
		dim.width += rightPref.width;
		Dimension bottomPref = bottom.getPreferredSize();
		dim.height += bottomPref.height;
		if(top != null)
		{
			Dimension topPref = top.getPreferredSize();
			dim.height += topPref.height;
		}

		return dim;
	} //}}}

	//{{{ minimumLayoutSize() method
	public Dimension minimumLayoutSize(Container parent)
	{
		Dimension dim = new Dimension();
		Insets insets = getInsets(parent);

		dim.width = insets.left + insets.right;
		dim.height = insets.top + insets.bottom;

		Dimension leftPref = left.getMinimumSize();
		dim.width += leftPref.width;
		Dimension centerPref = center.getMinimumSize();
		dim.width += centerPref.width; 
		dim.height += centerPref.height;
		Dimension rightPref = right.getMinimumSize();
		dim.width += rightPref.width;
		Dimension bottomPref = bottom.getMinimumSize();
		dim.height += bottomPref.height;
		if(top != null)
		{
			Dimension topPref = top.getMinimumSize();
			dim.height += topPref.height;
		}
		
		return dim;
	} //}}}

	//{{{ layoutContainer() method
	public void layoutContainer(Container parent)
	{
		Dimension size = parent.getSize();
		Insets insets = getInsets(parent);

		int itop = insets.top;
		int ileft = insets.left;
		int ibottom = insets.bottom;
		int iright = insets.right;

		int rightWidth = right.getPreferredSize().width;
		int leftWidth = left.getPreferredSize().width;
		int topHeight;
		if(top != null)
		{
			topHeight = top.getPreferredSize().height;
		}
		else
		{
			topHeight = 0;
		}
		int bottomHeight = bottom.getPreferredSize().height;
		int centerWidth = Math.max(0,size.width - leftWidth
			- rightWidth - ileft - iright);
		int centerHeight = Math.max(0,size.height - topHeight
			- bottomHeight - itop - ibottom);
			
		left.setBounds(
			ileft,
			itop+topHeight,
			leftWidth,
			centerHeight);

		center.setBounds(
			ileft + leftWidth,
			itop+topHeight,
			centerWidth,
			centerHeight);

		right.setBounds(
			ileft + leftWidth + centerWidth,
			itop+topHeight,
			rightWidth,
			centerHeight);

		bottom.setBounds(
			ileft,
			itop + topHeight + centerHeight,
			Math.max(0,size.width - bottom.getHeight()
				- ileft - iright),
			bottomHeight);
		if(top != null)
		{
			top.setBounds(
				ileft,
				itop,
				leftWidth+centerWidth+rightWidth,
				topHeight);
		}
	} //}}}

	//{{{ Private members
	private Component center;
	private Component left;
	private Component right;
	private Component bottom;
	private Component top;

	//{{{ getInsets() method
	private Insets getInsets(Component parent)
	{
		Border border = ((JComponent)parent).getBorder();
		if(border == null)
			return new Insets(0,0,0,0);
		else
			return border.getBorderInsets(parent);
	} //}}}
	
	//}}}
}
