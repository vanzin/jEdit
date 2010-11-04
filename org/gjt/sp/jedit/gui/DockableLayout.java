/*
 * DockableLayout.java -- a more flexible BorderLayout
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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

//{{{ Imports
import java.awt.*;
//}}}

public class DockableLayout implements LayoutManager2
{
	// for backwards compatibility with plugins that fiddle with
	// jEdit's UI layout
	static final String CENTER = BorderLayout.CENTER;

	static final String TOP_BUTTONS = "top-buttons";
	static final String LEFT_BUTTONS = "left-buttons";
	static final String BOTTOM_BUTTONS = "bottom-buttons";
	static final String RIGHT_BUTTONS = "right-buttons";

	private boolean alternateLayout;
	private Component center;

	/* No good */
	private DockablePanel top;
	private DockablePanel left;
	private DockablePanel bottom;
	private DockablePanel right;

	private Component topButtons, leftButtons, bottomButtons, rightButtons;

	//{{{ isAlternateLayout() method
	/**
	 * jEdit View option: wide horizontal docking areas versus tall vertical docking areas 
	 * @return true if using the "alternate layout"
	 */
	public boolean isAlternateLayout()
	{
		return alternateLayout;
	} //}}}

	//{{{ setAlternateLayout() method
	public void setAlternateLayout(boolean alternateLayout)
	{
		this.alternateLayout = alternateLayout;
	} //}}}

	//{{{ addLayoutComponent() method
	public void addLayoutComponent(String name, Component comp)
	{
		addLayoutComponent(comp, name);
	} //}}}

	//{{{ addLayoutComponent() method
	public void addLayoutComponent(Component comp, Object cons)
	{
		if(cons == null || CENTER.equals(cons))
			center = comp;
		else if(DockableWindowManager.TOP.equals(cons))
			top = (DockablePanel)comp;
		else if(DockableWindowManager.LEFT.equals(cons))
			left = (DockablePanel)comp;
		else if(DockableWindowManager.BOTTOM.equals(cons))
			bottom = (DockablePanel)comp;
		else if(DockableWindowManager.RIGHT.equals(cons))
			right = (DockablePanel)comp;
		else if(TOP_BUTTONS.equals(cons))
			topButtons = comp;
		else if(LEFT_BUTTONS.equals(cons))
			leftButtons = comp;
		else if(BOTTOM_BUTTONS.equals(cons))
			bottomButtons = comp;
		else if(RIGHT_BUTTONS.equals(cons))
			rightButtons = comp;
	} //}}}

	//{{{ removeLayoutComponent() method
	public void removeLayoutComponent(Component comp)
	{
		if(center == comp)
			center = null;
		else if(comp == top)
			top = null;
		else if(comp == left)
			left = null;
		else if(comp == bottom)
			bottom = null;
		else if(comp == right)
			right = null;
	} //}}}

	//{{{ preferredLayoutSize() method
	public Dimension preferredLayoutSize(Container parent)
	{
		Dimension prefSize = new Dimension(0,0);
		Dimension _top = top.getPreferredSize();
		Dimension _left = left.getPreferredSize();
		Dimension _bottom = bottom.getPreferredSize();
		Dimension _right = right.getPreferredSize();
		Dimension _topButtons = topButtons.getPreferredSize();
		Dimension _leftButtons = leftButtons.getPreferredSize();
		Dimension _bottomButtons = bottomButtons.getPreferredSize();
		Dimension _rightButtons = rightButtons.getPreferredSize();
		Dimension _center = (center == null
			? new Dimension(0,0)
			: center.getPreferredSize());
		Dimension _topToolbars = new Dimension(0,0);
		Dimension _bottomToolbars = new Dimension(0,0);

		prefSize.height = _top.height + _bottom.height + _center.height
			+ _topButtons.height + _bottomButtons.height
			+ _topToolbars.height + _bottomToolbars.height;
		prefSize.width = _left.width + _right.width
			+ Math.max(_center.width,
			Math.max(_topToolbars.width,_bottomToolbars.width))
			+ _leftButtons.width + _rightButtons.width;

		return prefSize;
	} //}}}

	//{{{ minimumLayoutSize() method
	public Dimension minimumLayoutSize(Container parent)
	{
		// I'm lazy
		return preferredLayoutSize(parent);
	} //}}}

	//{{{ maximumLayoutSize() method
	public Dimension maximumLayoutSize(Container parent)
	{
		return new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
	} //}}}

	//{{{ layoutContainer() method
	public void layoutContainer(Container parent)
	{
		Dimension size = parent.getSize();

		Dimension _topToolbars = new Dimension(0,0);
		Dimension _bottomToolbars = new Dimension(0,0);

		int topButtonHeight = -1;
		int bottomButtonHeight = -1;
		int leftButtonWidth = -1;
		int rightButtonWidth = -1;

		Dimension _top = top.getPreferredSize();
		Dimension _left = left.getPreferredSize();
		Dimension _bottom = bottom.getPreferredSize();
		Dimension _right = right.getPreferredSize();

		int topHeight = _top.height;
		int bottomHeight = _bottom.height;
		int leftWidth = _left.width;
		int rightWidth = _right.width;

		boolean topEmpty = ((Container)topButtons)
			.getComponentCount() <= 2;
		boolean leftEmpty = ((Container)leftButtons)
			.getComponentCount() <= 2;
		boolean bottomEmpty = ((Container)bottomButtons)
			.getComponentCount() <= 2;
		boolean rightEmpty = ((Container)rightButtons)
			.getComponentCount() <= 2;

		Dimension closeBoxSize;
		if(((Container)topButtons).getComponentCount() == 0)
			closeBoxSize = new Dimension(0,0);
		else
		{
			closeBoxSize = ((Container)topButtons)
				.getComponent(0).getPreferredSize();
		}

		int closeBoxWidth = Math.max(closeBoxSize.width,
			closeBoxSize.height) + 1;

		if(alternateLayout)
		{
			//{{{ Lay out independent buttons
			int _width = size.width;

			int padding = (leftEmpty&&rightEmpty)
				? 0 : closeBoxWidth;

			topButtonHeight = top.getWindowContainer()
				.getWrappedDimension(_width
				- closeBoxWidth * 2);
			topButtons.setBounds(
				padding,
				0,
				size.width - padding * 2,
				topButtonHeight);

			bottomButtonHeight = bottom.getWindowContainer()
				.getWrappedDimension(_width);
			bottomButtons.setBounds(
				padding,
				size.height - bottomButtonHeight,
				size.width - padding * 2,
				bottomButtonHeight);

			int _height = size.height
				- topButtonHeight
				- bottomButtonHeight;
			//}}}

			//{{{ Lay out dependent buttons
			leftButtonWidth = left.getWindowContainer()
				.getWrappedDimension(_height);
			leftButtons.setBounds(
				0,
				topHeight + topButtonHeight,
				leftButtonWidth,
				_height - topHeight - bottomHeight);

			rightButtonWidth = right.getWindowContainer()
				.getWrappedDimension(_height);
			rightButtons.setBounds(
				size.width - rightButtonWidth,
				topHeight + topButtonHeight,
				rightButtonWidth,
				_height - topHeight - bottomHeight);
			//}}}

			int[] dimensions = adjustDockingAreasToFit(
				size,
				topHeight,
				leftWidth,
				bottomHeight,
				rightWidth,
				topButtonHeight,
				leftButtonWidth,
				bottomButtonHeight,
				rightButtonWidth,
				_topToolbars,
				_bottomToolbars);

			topHeight = dimensions[0];
			leftWidth = dimensions[1];
			bottomHeight = dimensions[2];
			rightWidth = dimensions[3];

			//{{{ Lay out docking areas
			top.setBounds(
				0,
				topButtonHeight,
				size.width,
				topHeight);

			bottom.setBounds(
				0,
				size.height
				- bottomHeight
				- bottomButtonHeight,
				size.width,
				bottomHeight);

			left.setBounds(
				leftButtonWidth,
				topButtonHeight + topHeight,
				leftWidth,
				_height - topHeight - bottomHeight);

			right.setBounds(
				_width - rightButtonWidth - rightWidth,
				topButtonHeight + topHeight,
				rightWidth,
				_height - topHeight - bottomHeight); //}}}
		}
		else
		{
			//{{{ Lay out independent buttons
			int _height = size.height;

			int padding = (topEmpty && bottomEmpty
				? 0 : closeBoxWidth);

			leftButtonWidth = left.getWindowContainer()
				.getWrappedDimension(_height
				- closeBoxWidth * 2);
			leftButtons.setBounds(
				0,
				padding,
				leftButtonWidth,
				_height - padding * 2);

			rightButtonWidth = right.getWindowContainer()
				.getWrappedDimension(_height);
			rightButtons.setBounds(
				size.width - rightButtonWidth,
				padding,
				rightButtonWidth,
				_height - padding * 2);

			int _width = size.width
				- leftButtonWidth
				- rightButtonWidth;
			//}}}

			//{{{ Lay out dependent buttons
			topButtonHeight = top.getWindowContainer()
				.getWrappedDimension(_width);
			topButtons.setBounds(
				leftButtonWidth + leftWidth,
				0,
				_width - leftWidth - rightWidth,
				topButtonHeight);

			bottomButtonHeight = bottom.getWindowContainer()
				.getWrappedDimension(_width);
			bottomButtons.setBounds(
				leftButtonWidth + leftWidth,
				_height - bottomButtonHeight,
				_width - leftWidth - rightWidth,
				bottomButtonHeight); //}}}

			int[] dimensions = adjustDockingAreasToFit(
				size,
				topHeight,
				leftWidth,
				bottomHeight,
				rightWidth,
				topButtonHeight,
				leftButtonWidth,
				bottomButtonHeight,
				rightButtonWidth,
				_topToolbars,
				_bottomToolbars);

			topHeight = dimensions[0];
			leftWidth = dimensions[1];
			bottomHeight = dimensions[2];
			rightWidth = dimensions[3];

			//{{{ Lay out docking areas
			top.setBounds(
				leftButtonWidth + leftWidth,
				topButtonHeight,
				_width - leftWidth - rightWidth,
				topHeight);

			bottom.setBounds(
				leftButtonWidth + leftWidth,
				size.height - bottomHeight - bottomButtonHeight,
				_width - leftWidth - rightWidth,
				bottomHeight);

			left.setBounds(
				leftButtonWidth,
				0,
				leftWidth,
				_height);

			right.setBounds(
				size.width - rightWidth - rightButtonWidth,
				0,
				rightWidth,
				_height); //}}}
		}

		//{{{ Position center (edit pane, or split pane)
		if(center != null)
		{
			center.setBounds(
				leftButtonWidth + leftWidth,
				topButtonHeight + topHeight
				+ _topToolbars.height,
				size.width
				- leftWidth
				- rightWidth
				- leftButtonWidth
				- rightButtonWidth,
				size.height
				- topHeight
				- topButtonHeight
				- bottomHeight
				- bottomButtonHeight
				- _topToolbars.height
				- _bottomToolbars.height);
		} //}}}
	} //}}}

	//{{{ adjustDockingAreasToFit() method
	private int[] adjustDockingAreasToFit(
		Dimension size,
		int topHeight,
		int leftWidth,
		int bottomHeight,
		int rightWidth,
		int topButtonHeight,
		int leftButtonWidth,
		int bottomButtonHeight,
		int rightButtonWidth,
		Dimension _topToolbars,
		Dimension _bottomToolbars)
	{
		int maxTopHeight = size.height - bottomHeight
			- topButtonHeight - bottomButtonHeight
			- _topToolbars.height - _bottomToolbars.height;
		topHeight = Math.min(Math.max(0,maxTopHeight),
			topHeight);
		leftWidth = Math.min(Math.max(0,
			size.width - leftButtonWidth
			- rightButtonWidth - rightWidth),leftWidth);
		int maxBottomHeight = size.height - topHeight
			- topButtonHeight - bottomButtonHeight
			- _topToolbars.height - _bottomToolbars.height;
		bottomHeight = Math.min(Math.max(0,maxBottomHeight),
			bottomHeight);
		rightWidth = Math.min(Math.max(0,
			size.width - leftButtonWidth
			- rightButtonWidth - leftWidth),rightWidth);

		top.getWindowContainer().setDimension(topHeight);
		left.getWindowContainer().setDimension(leftWidth);
		bottom.getWindowContainer().setDimension(bottomHeight);
		right.getWindowContainer().setDimension(rightWidth);

		return new int[] {
			topHeight,
			leftWidth,
			bottomHeight,
			rightWidth
		};
	} //}}}

	//{{{ getLayoutAlignmentX() method
	public float getLayoutAlignmentX(Container target)
	{
		return 0.5f;
	} //}}}

	//{{{ getLayoutAlignmentY() method
	public float getLayoutAlignmentY(Container target)
	{
		return 0.5f;
	} //}}}

	//{{{ invalidateLayout() method
	public void invalidateLayout(Container target) {}
	//}}}
}
