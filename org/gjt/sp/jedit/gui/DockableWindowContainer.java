/*
 * DockableWindowContainer.java - holds dockable windows
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.border.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public interface DockableWindowContainer
{
	void addDockableWindow(DockableWindow win);
	void saveDockableWindow(DockableWindow win);
	void removeDockableWindow(DockableWindow win);
	void showDockableWindow(DockableWindow win);
	boolean isDockableWindowVisible(DockableWindow win);

	/**
	 * Tabbed pane container.
	 */
	class TabbedPane extends JTabbedPane implements DockableWindowContainer
	{
		public static final int SPLITTER_WIDTH = 10;

		String position;
		int dimension;
		boolean collapsed;

		public TabbedPane(String position)
		{
			this.position = position;

			dimension = jEdit.getIntegerProperty(
				"view.dock." + position + ".dimension",-1);

			if(dimension <= SPLITTER_WIDTH)
				collapsed = true;

			collapsed = jEdit.getBooleanProperty("view.dock."
				+ position + ".collapsed");

			MouseHandler mouseHandler = new MouseHandler();
			addMouseListener(mouseHandler);
			addMouseMotionListener(mouseHandler);

			propertiesChanged();
		}

		public boolean isCollapsed()
		{
			return getComponentCount() == 0 || collapsed;
		}

		public void setCollapsed(boolean collapsed)
		{
			if(getComponentCount() == 0)
				return;

			if(dimension <= SPLITTER_WIDTH)
				dimension = -1;

			this.collapsed = collapsed;
			revalidate();
		}

		public void toggleCollapsed()
		{
			setCollapsed(!collapsed);
		}

		public void saveDimension()
		{
			jEdit.setIntegerProperty("view.dock." + position + ".dimension",
				dimension);
			jEdit.setBooleanProperty("view.dock." + position
				+ ".collapsed",collapsed);
		}

		public void propertiesChanged()
		{
			setBorder(new DockBorder(position));

			int tabsPos = jEdit.getIntegerProperty(
				"view.docking.tabsPos",0);
			if(tabsPos == 0)
				setTabPlacement(JTabbedPane.TOP);
			else if(tabsPos == 1)
				setTabPlacement(JTabbedPane.BOTTOM);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(0,0);
		}

		public Dimension getPreferredSize()
		{
			if(getComponentCount() == 0)
				return new Dimension(0,0);

			Dimension prefSize = super.getPreferredSize();
			if(collapsed)
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					prefSize.width = SPLITTER_WIDTH;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					prefSize.height = SPLITTER_WIDTH;
			}
			else if(dimension <= SPLITTER_WIDTH)
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					dimension = prefSize.width;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					 dimension = prefSize.height;
			}
			else
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					prefSize.width = dimension;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					prefSize.height = dimension;
			}

			return prefSize;
		}

		public void addDockableWindow(DockableWindow win)
		{
			addTab(jEdit.getProperty(win.getName()
				+ ".title"),win.getComponent());
			setSelectedComponent(win.getComponent());

			collapsed = false;

			revalidate();
		}

		public void saveDockableWindow(DockableWindow win) {}

		public void removeDockableWindow(DockableWindow win)
		{
			remove(win.getComponent());
			revalidate();
		}

		public void showDockableWindow(DockableWindow win)
		{
			setSelectedComponent(win.getComponent());
			if(collapsed)
			{
				collapsed = false;
				revalidate();
			}
			win.getComponent().requestFocus();
		}

		public boolean isDockableWindowVisible(DockableWindow win)
		{
			return !collapsed;
		}

		class MouseHandler extends MouseAdapter implements MouseMotionListener
		{
			boolean canDrag;
			int dragStartDimension;
			Point dragStart;

			public void mousePressed(MouseEvent evt)
			{
				dragStartDimension = dimension;
				dragStart = evt.getPoint();
				dragStart.x = (getWidth() - dragStart.x);
				dragStart.y = (getHeight() - dragStart.y);
			}

			public void mouseClicked(MouseEvent evt)
			{
				if(evt.getClickCount() == 2)
					setCollapsed(!isCollapsed());
			}

			public void mouseMoved(MouseEvent evt)
			{
				Border border = getBorder();
				Insets insets = border.getBorderInsets(TabbedPane.this);
				int cursor = Cursor.DEFAULT_CURSOR;
				canDrag = false;
				if(position.equals(DockableWindowManager.TOP))
				{
					if(evt.getY() >= getHeight() - insets.bottom)
					{
						cursor = Cursor.N_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.LEFT))
				{
					if(evt.getX() >= getWidth() - insets.right)
					{
						cursor = Cursor.W_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					if(evt.getY() <= insets.top)
					{
						cursor = Cursor.S_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					if(evt.getX() <= insets.left)
					{
						cursor = Cursor.E_RESIZE_CURSOR;
						canDrag = true;
					}
				}

				setCursor(Cursor.getPredefinedCursor(cursor));
			}

			public void mouseDragged(MouseEvent evt)
			{
				if(!canDrag)
					return;

				if(dragStart == null) // can't happen?
					return;

				if(position.equals(DockableWindowManager.TOP))
					dimension = evt.getY() + dragStart.y;
				else if(position.equals(DockableWindowManager.LEFT))
					dimension = evt.getX() + dragStart.x;
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					dimension = getHeight() - (/* dragStart.y
						- */ evt.getY());
				}
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					dimension = getWidth() - (/* dragStart.x
						- */ evt.getX());
				}

				dimension = Math.max(SPLITTER_WIDTH,dimension);
				if(dimension == SPLITTER_WIDTH)
				{
					dimension = dragStartDimension;
					collapsed = true;
				}
				else
					collapsed = false;

				revalidate();
			}

			public void mouseExited(MouseEvent evt)
			{
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			}
		}

		static class DockBorder implements Border
		{
			String position;
			Insets insets;
			Color color1;
			Color color2;
			Color color3;

			DockBorder(String position)
			{
				if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
				{
					color1 = MetalLookAndFeel.getControlHighlight();
					color2 = MetalLookAndFeel.getControlDarkShadow();
					color3 = MetalLookAndFeel.getControl();
				}

				this.position = position;
				insets = new Insets(
					position.equals(DockableWindowManager.BOTTOM)
						? SPLITTER_WIDTH : 0,
					position.equals(DockableWindowManager.RIGHT)
						? SPLITTER_WIDTH : 0,
					position.equals(DockableWindowManager.TOP)
						? SPLITTER_WIDTH : 0,
					position.equals(DockableWindowManager.LEFT)
						? SPLITTER_WIDTH : 0);
			}

			public void paintBorder(Component c, Graphics g,
				int x, int y, int width, int height)
			{
				if(color1 == null || color2 == null || color3 == null)
					return;

				if(position.equals(DockableWindowManager.BOTTOM))
					paintHorizBorder(g,x,y,width);
				else if(position.equals(DockableWindowManager.RIGHT))
					paintVertBorder(g,x,y,height);
				else if(position.equals(DockableWindowManager.TOP))
				{
					paintHorizBorder(g,x,y + height
						- SPLITTER_WIDTH,width);
				}
				else if(position.equals(DockableWindowManager.LEFT))
				{
					paintVertBorder(g,x + width
						- SPLITTER_WIDTH,y,height);
				}
			}

			public Insets getBorderInsets(Component c)
			{
				return insets;
			}

			public boolean isBorderOpaque()
			{
				return false;
			}

			private void paintHorizBorder(Graphics g, int x, int y, int width)
			{
				g.setColor(color3);
				g.fillRect(x,y,width,SPLITTER_WIDTH);

				for(int i = 0; i < width / 4 - 1; i++)
				{
					g.setColor(color1);
					g.drawLine(x + i * 4 + 2,y + 3,
						x + i * 4 + 2,y + 3);
					g.setColor(color2);
					g.drawLine(x + i * 4 + 3,y + 4,
						x + i * 4 + 3,y + 4);
					g.setColor(color1);
					g.drawLine(x + i * 4 + 4,y + 5,
						x + i * 4 + 4,y + 5);
					g.setColor(color2);
					g.drawLine(x + i * 4 + 5,y + 6,
						x + i * 4 + 5,y + 6);
				}
			}

			private void paintVertBorder(Graphics g, int x, int y, int height)
			{
				g.setColor(color3);
				g.fillRect(x,y,SPLITTER_WIDTH,height);

				for(int i = 0; i < height / 4 - 1; i++)
				{
					g.setColor(color1);
					g.drawLine(x + 3,y + i * 4 + 2,
						x + 3,y + i * 4 + 2);
					g.setColor(color2);
					g.drawLine(x + 4,y + i * 4 + 3,
						x + 4,y + i * 4 + 3);
					g.setColor(color1);
					g.drawLine(x + 5,y + i * 4 + 4,
						x + 5,y + i * 4 + 4);
					g.setColor(color2);
					g.drawLine(x + 6,y + i * 4 + 5,
						x + 6,y + i * 4 + 5);
				}
			}
		}
	}

	/**
	 * Floating container.
	 */
	class Floating extends JFrame implements DockableWindowContainer
	{
		public Floating(DockableWindowManager dockableWindowManager)
		{
			this.dockableWindowManager = dockableWindowManager;
			setIconImage(GUIUtilities.getPluginIcon());
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		}

		public void addDockableWindow(DockableWindow window)
		{
			this.window = window;
			name = window.getName();
			setTitle(jEdit.getProperty(name + ".title"));

			getContentPane().add(BorderLayout.CENTER,window.getComponent());

			pack();
			GUIUtilities.loadGeometry(this,name);
			show();
		}

		public void saveDockableWindow(DockableWindow window)
		{
			GUIUtilities.saveGeometry(this,name);
		}

		public void removeDockableWindow(DockableWindow window)
		{
			super.dispose();
		}

		public void showDockableWindow(DockableWindow window)
		{
			toFront();
			requestFocus();
		}

		public boolean isDockableWindowVisible(DockableWindow win)
		{
			return true;
		}

		public void dispose()
		{
			dockableWindowManager.removeDockableWindow(name);
		}

		// private members
		private DockableWindowManager dockableWindowManager;
		private DockableWindow window;
		private String name;
	}
}
