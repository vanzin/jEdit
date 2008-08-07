/*
 * PanelWindowContainer.java - holds dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
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
import org.gjt.sp.jedit.jEdit;

import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.border.Border;
//}}}

/**
 * @version $Id$
 */
class DockablePanel extends JPanel
{
	private PanelWindowContainer panel;
	private DockableWindowManagerImpl wm;

	//{{{ DockablePanel constructor
	DockablePanel(PanelWindowContainer panel)
	{
		super(new CardLayout());

		this.panel = panel;
		this.wm = panel.getDockableWindowManager();

		ResizeMouseHandler resizeMouseHandler = new ResizeMouseHandler();
		addMouseListener(resizeMouseHandler);
		addMouseMotionListener(resizeMouseHandler);
	} //}}}

	//{{{ getWindowContainer() method
	PanelWindowContainer getWindowContainer()
	{
		return panel;
	} //}}}

	//{{{ showDockable() method
	void showDockable(String name)
	{
		((CardLayout)getLayout()).show(this,name);
	} //}}}

	//{{{ getMinimumSize() method
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	} //}}}

	//{{{ getPreferredSize() method
	public Dimension getPreferredSize()
	{
		final String position = panel.getPosition();
		final int dimension = panel.getDimension();

		if(panel.getCurrent() == null)
			return new Dimension(0,0);
		else
		{
			if(position.equals(DockableWindowManager.TOP)
				|| position.equals(DockableWindowManager.BOTTOM))
			{
				if(dimension <= 0)
				{
					int height = super.getPreferredSize().height;
					panel.setDimension(height);
				}
				return new Dimension(0,
					dimension + PanelWindowContainer
					.SPLITTER_WIDTH);
			}
			else
			{
				if(dimension <= 0)
				{
					int width = super.getPreferredSize().width;
					panel.setDimension(width);
				}
				return new Dimension(dimension +
					PanelWindowContainer.SPLITTER_WIDTH,
					0);
			}
		}
	} //}}}

	//{{{ setBounds() method
	public void setBounds(int x, int y, int width, int height)
	{
		final String position = panel.getPosition();
		final int dimension = panel.getDimension();

		if(position.equals(DockableWindowManager.TOP) ||
			position.equals(DockableWindowManager.BOTTOM))
		{
			if(dimension != 0 && height <= PanelWindowContainer.SPLITTER_WIDTH)
				panel.show((DockableWindowManagerImpl.Entry) null);
			else
				panel.setDimension(height);
		}
		else
		{
			if(dimension != 0 && width <= PanelWindowContainer.SPLITTER_WIDTH)
				panel.show((DockableWindowManagerImpl.Entry) null);
			else
				panel.setDimension(width);
		}

		super.setBounds(x,y,width,height);
	} //}}}

	/** This belong to ResizeMouseHandler but requires to be static. */
	static Point dragStart;
	
	//{{{ ResizeMouseHandler class
	class ResizeMouseHandler extends MouseAdapter implements MouseMotionListener
	{
		/** This is true if the mouse is on the split bar. */
		boolean canDrag;

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
			if(canDrag)
			{
				continuousLayout = jEdit.getBooleanProperty("appearance.continuousLayout");
				wm.setResizePos(panel.getDimension(),panel);
				dragStart = evt.getPoint();
			}
		} //}}}

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent evt)
		{
			if(canDrag)
			{
				if (!continuousLayout)
				{
					panel.setDimension(wm.resizePos
							   + PanelWindowContainer
						.SPLITTER_WIDTH);
				}
				wm.finishResizing();
				dragStart = null;
				wm.revalidate();
			}
		} //}}}

		//{{{ mouseMoved() method
		public void mouseMoved(MouseEvent evt)
		{
			Border border = getBorder();
			if(border == null)
			{
				// collapsed
				return;
			}

			String position = panel.getPosition();

			Insets insets = border.getBorderInsets(DockablePanel.this);
			canDrag = false;
			//{{{ Top...
			if(position.equals(DockableWindowManager.TOP))
			{
				if(evt.getY() >= getHeight() - insets.bottom)
					canDrag = true;
			} //}}}
			//{{{ Left...
			else if(position.equals(DockableWindowManager.LEFT))
			{
				if(evt.getX() >= getWidth() - insets.right)
					canDrag = true;
			} //}}}
			//{{{ Bottom...
			else if(position.equals(DockableWindowManager.BOTTOM))
			{
				if(evt.getY() <= insets.top)
					canDrag = true;
			} //}}}
			//{{{ Right...
			else if(position.equals(DockableWindowManager.RIGHT))
			{
				if(evt.getX() <= insets.left)
					canDrag = true;
			} //}}}

			if (dragStart == null)
			{
				if(canDrag)
				{
					wm.setCursor(Cursor.getPredefinedCursor(
						getAppropriateCursor()));
				}
				else
				{
					wm.setCursor(Cursor.getPredefinedCursor(
						Cursor.DEFAULT_CURSOR));
				}
			}
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent evt)
		{
			if(!canDrag)
				return;

			if(dragStart == null) // can't happen?
				return;

			int dimension = panel.getDimension();

			String position = panel.getPosition();

			int newSize = 0;
			//{{{ Top...
			if(position.equals(DockableWindowManager.TOP))
			{
				newSize = evt.getY();
				wm.setResizePos(
					evt.getY() - dragStart.y
					+ dimension,
					panel);
			} //}}}
			//{{{ Left...
			else if(position.equals(DockableWindowManager.LEFT))
			{
				newSize = evt.getX();
				wm.setResizePos(evt.getX() - dragStart.x
					+ dimension,
					panel);
			} //}}}
			//{{{ Bottom...
			else if(position.equals(DockableWindowManager.BOTTOM))
			{
				newSize = dimension - evt.getY();
				wm.setResizePos(dimension - evt.getY()
					+ dragStart.y,
					panel);
			} //}}}
			//{{{ Right...
			else if(position.equals(DockableWindowManager.RIGHT))
			{
				newSize = dimension - evt.getX();
				wm.setResizePos(dimension - evt.getX()
					+ dragStart.x,
					panel);
			} //}}}

			if (continuousLayout)
			{
				panel.setDimension(newSize
						   + PanelWindowContainer.SPLITTER_WIDTH);
				wm.revalidate();
			}
		} //}}}

		//{{{ mouseExited() method
		public void mouseExited(MouseEvent evt)
		{
			if (dragStart == null)
			{
				wm.setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			}
		} //}}}

		//{{{ getCursor() method
		private int getAppropriateCursor()
		{
			String position = panel.getPosition();

			if(position.equals(DockableWindowManager.TOP))
				return Cursor.N_RESIZE_CURSOR;
			else if(position.equals(DockableWindowManager.LEFT))
				return Cursor.W_RESIZE_CURSOR;
			else if(position.equals(DockableWindowManager.BOTTOM))
				return Cursor.S_RESIZE_CURSOR;
			else if(position.equals(DockableWindowManager.RIGHT))
				return Cursor.E_RESIZE_CURSOR;
			else
				throw new InternalError();
		} //}}}

		private boolean continuousLayout;
	} //}}}
}
