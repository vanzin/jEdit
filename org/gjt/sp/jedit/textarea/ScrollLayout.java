/*
 * ScrollLayout.java
 * :tabSize=4:indentSize=4:noTabs=false:
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

/**
 * Similar to a javax.swing.ScrollPaneLayout, but not as restrictive on the
 * components that can be added. This layout is essentially a 3 x 3 grid layout,
 * with the intent that the bottom and right will hold scroll bars. When installed
 * a TextArea, the bottom has a scroll bar, the right has a vertical box containing
 * a scroll bar, the left has a Gutter, and the center holds a TextAreaPainter.
 * The corners and top are not used by the TextArea.
 *
 * The corners are intended to be a place to put a button or other small component.
 * The corner dimensions are constrained by the left, right, top, and bottom
 * components, so, for example, the width of the top left corner is the width
 * of the left component and the height of the top left corner is the height of
 * the top component.
 */
public class ScrollLayout implements LayoutManager
{
	public static final String CENTER = "center";
	public static final String RIGHT = "right";
	public static final String LEFT = "left";
	public static final String BOTTOM = "bottom";
	public static final String TOP = "top";
	public static final String TOP_LEFT = "topLeft";
	public static final String TOP_RIGHT = "topRight";
	public static final String BOTTOM_LEFT = "bottomLeft";
	public static final String BOTTOM_RIGHT = "bottomRight";

	//{{{ addLayoutComponent() method
	/**
 	 * Adds a component to the layout using the <code>name</code> parameter to
 	 * position the component.
 	 * @param name One of CENTER, RIGHT, LEFT, BOTTOM, TOP, TOP_LEFT, TOP_RIGHT, 
 	 * BOTTOM_LEFT, BOTTOM_RIGHT.
 	 * @param comp The component to add at the given position. If <code>null</code>, the 
 	 * component will be removed from that position.
 	 */
	@Override
	public void addLayoutComponent(String name, Component comp)
	{
		switch(name) {
			case CENTER:
				center = comp;
				break;
			case RIGHT:
				right = comp;
				break;
			case LEFT:
				left = comp;
				break;
			case BOTTOM:
				bottom = comp;
				break;
			case TOP:
				top = comp;
				break;
			case TOP_LEFT:
				topLeft = comp;
				break;
			case TOP_RIGHT:
				topRight = comp;
				break;
			case BOTTOM_LEFT:
				bottomLeft = comp;
				break;
			case BOTTOM_RIGHT:
				bottomRight = comp;
				break;
		}
	} //}}}

	//{{{ removeLayoutComponent() method
	/**
 	 * Removes the specified component from the layout.
 	 * @param comp The component to be removed.
 	 */
	@Override
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
		else if(topLeft == comp)
			topLeft = null;
		else if(topRight == comp)
			topRight = null;
		else if(bottomLeft == comp)
			bottomLeft = null;
		else if(bottomRight == comp)
			bottomRight = null;
	} //}}}

	//{{{ preferredLayoutSize() method
	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		Dimension dim = new Dimension();
		Insets insets = getInsets(parent);

		dim.width = insets.left + insets.right;
		dim.width += getLeftPreferredWidth();
		dim.width += getCenterPreferredWidth();
		dim.width += getRightPreferredWidth();
		
		dim.height = insets.top + insets.bottom;
		dim.height += getTopPreferredHeight();
		dim.height += getCenterPreferredHeight();
		dim.height += getBottomPreferredHeight();

		return dim;
	} //}}}
	
	//{{{ preferred widths
	// constrained by left component preferred width
	private int getLeftPreferredWidth() 
	{
		if (left != null)
		{
			return left.getPreferredSize().width;
		}
		int tlw = topLeft == null ? 0 : topLeft.getPreferredSize().width;
		int lw = left == null ? 0 : left.getPreferredSize().width;
		int blw = bottomLeft == null ? 0 : bottomLeft.getPreferredSize().width;
		return Math.max(lw, Math.max(tlw, blw));
	}

	private int getCenterPreferredWidth()
	{
		int tw = top == null ? 0 : top.getPreferredSize().width;
		int cw = center == null ? 0 : center.getPreferredSize().width;
		int bw = bottom == null ? 0 : bottom.getPreferredSize().width;
		return Math.max(cw, Math.max(tw, bw));
	}
	
	// constrained by right component preferred width
	private int getRightPreferredWidth()
	{
		if (right != null)
		{
			return right.getPreferredSize().width;
		}
		int trw = topRight == null ? 0 : topRight.getPreferredSize().width;
		int rw = right == null ? 0 : right.getPreferredSize().width;
		int brw = bottomRight == null ? 0 : bottomRight.getPreferredSize().width;
		return Math.max(rw, Math.max(trw, brw));
	}
	//}}}
	
	//{{{ preferred heights
	// constrained by top component preferred height
	private int getTopPreferredHeight()
	{
		if (top != null) 
		{
			return top.getPreferredSize().height;	
		}
		int tlh = topLeft == null ? 0 : topLeft.getPreferredSize().height;
		int th = top == null ? 0 : top.getPreferredSize().height;
		int trh = topRight == null ? 0 : topRight.getPreferredSize().height;
		return Math.max(th, Math.max(tlh, trh));
	}
	
	private int getCenterPreferredHeight()
	{
		int lh = left == null ? 0 : left.getPreferredSize().height;
		int ch = center == null ? 0 : center.getPreferredSize().height;
		int rh = right == null ? 0 : right.getPreferredSize().height;
		return Math.max(ch, Math.max(lh, rh));
	}
	
	// constrained by bottom component preferred height
	private int getBottomPreferredHeight()
	{
		if (bottom != null)
		{
			return bottom.getPreferredSize().height;	
		}
		int blh = bottomLeft == null ? 0 : bottomLeft.getPreferredSize().height;
		int bh = bottom == null ? 0 : bottom.getPreferredSize().height;
		int brh = bottomRight == null ? 0 : bottomRight.getPreferredSize().height;
		return Math.max(bh, Math.max(brh, blh));
	}
	//}}}
	
	//{{{ minimumLayoutSize() method
	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		Dimension dim = new Dimension();
		Insets insets = getInsets(parent);

		dim.width = insets.left + insets.right;
		dim.height = insets.top + insets.bottom;

		int tlw = topLeft == null ? 0 : topLeft.getMinimumSize().width;
		int lw = left == null ? 0 : left.getMinimumSize().width;
		int blw = bottomLeft == null ? 0 : bottomLeft.getMinimumSize().width;
		dim.width += Math.max(lw, Math.max(tlw, blw));
		
		int tw = top == null ? 0 : top.getMinimumSize().width;
		int cw = center == null ? 0 : center.getMinimumSize().width;
		int bw = bottom == null ? 0 : bottom.getMinimumSize().width;
		dim.width += Math.max(cw, Math.max(tw, bw));
		
		int trw = topRight == null ? 0 : topRight.getMinimumSize().width;
		int rw = right == null ? 0 : right.getMinimumSize().width;
		int brw = bottomRight == null ? 0 : bottomRight.getMinimumSize().width;
		dim.width += Math.max(rw, Math.max(trw, brw));
		
		int tlh = topLeft == null ? 0 : topLeft.getMinimumSize().height;
		int lh = left == null ? 0 : left.getMinimumSize().height;
		int blh = bottomLeft == null ? 0 : bottomLeft.getMinimumSize().height;
		dim.height += Math.max(lh, Math.max(tlh, blh));
		
		int th = top == null ? 0 : top.getMinimumSize().height;
		int ch = center == null ? 0 : center.getMinimumSize().height;
		int bh = bottom == null ? 0 : bottom.getMinimumSize().height;
		dim.height += Math.max(ch, Math.max(th, bh));
		
		int trh = topRight == null ? 0 : topRight.getMinimumSize().height;
		int rh = right == null ? 0 : right.getMinimumSize().height;
		int brh = bottomRight == null ? 0 : bottomRight.getMinimumSize().height;
		dim.height += Math.max(rh, Math.max(trh, brh));
		
		return dim;
	} //}}}

	//{{{ layoutContainer() method
	@Override
	public void layoutContainer(Container parent)
	{
		Dimension size = parent.getSize();
		Insets insets = getInsets(parent);

		int itop = insets.top;
		int ileft = insets.left;
		int ibottom = insets.bottom;
		int iright = insets.right;


		int leftWidth = getLeftPreferredWidth();
		int rightWidth = getRightPreferredWidth();
		int topHeight = getTopPreferredHeight();
		int bottomHeight = getBottomPreferredHeight();
		int centerWidth = Math.max(0,size.width - leftWidth
			- rightWidth - ileft - iright);
		int centerHeight = Math.max(0,size.height - topHeight
			- bottomHeight - itop - ibottom);
			
		if (left != null) 
		{
			left.setBounds(
				ileft,
				itop+topHeight,
				leftWidth,
				centerHeight);
		}
		if (center != null)
		{
			center.setBounds(
				ileft + leftWidth,
				itop+topHeight,
				centerWidth,
				centerHeight);
		}
		if (right != null)
		{
			right.setBounds(
				ileft + leftWidth + centerWidth,
				itop + topHeight,
				rightWidth,
				centerHeight);
		}
		if (bottom != null)
		{
			bottom.setBounds(
				ileft + leftWidth,
				itop + topHeight + centerHeight,
				centerWidth,
				bottomHeight);
		}
		if(top != null)
		{
			top.setBounds(
				ileft + leftWidth,
				itop,
				centerWidth,
				topHeight);
		}
		if (topLeft != null)
		{
			topLeft.setBounds(
				ileft,
				itop,
				leftWidth,
				topHeight);
		}
		if (topRight != null) 
		{
			topRight.setBounds(
				ileft + leftWidth + centerWidth,
				itop,
				rightWidth,
				topHeight);
		}
		if (bottomLeft != null)
		{
			bottomLeft.setBounds(
				ileft,
				itop + topHeight + centerHeight,
				leftWidth,
				bottomHeight);
		}
		if (bottomRight != null) 
		{
			bottomRight.setBounds(
				ileft + leftWidth + centerWidth,
				itop + topHeight + centerHeight,
				rightWidth,
				bottomHeight);
		}
	} //}}}

	//{{{ Private members
	private Component center;
	private Component left;
	private Component right;
	private Component bottom;
	private Component top;
	private Component topLeft;
	private Component topRight;
	private Component bottomLeft;
	private Component bottomRight;

	//{{{ getInsets() method
	private static Insets getInsets(Component parent)
	{
		Border border = ((JComponent)parent).getBorder();
		if(border == null)
			return new Insets(0,0,0,0);
		else
			return border.getBorderInsets(parent);
	} //}}}
	
	//}}}
}
