/*
 * PanelWindowContainer.java - holds dockable windows
 * Copyright (C) 2000, 2001 Slava Pestov
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
import javax.swing.plaf.metal.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class PanelWindowContainer implements DockableWindowContainer
{
	public PanelWindowContainer(DockableWindowManager wm, String position)
	{
		this.wm = wm;
		this.position = position;

		buttons = new ButtonBox();

		// the close box must be the same size as the other buttons to look good.
		// there are two ways to achieve this:
		// a) write a custom layout manager
		// b) when the first button is added, give the close box the proper size
		// I'm lazy so I chose "b". See register() for details.

		closeBox = new JButton(CLOSE_BOX);
		closeBox.setRequestFocusEnabled(false);
		closeBox.setToolTipText(jEdit.getProperty("view.docking.close-tooltip"));
		buttons.add(closeBox);

		closeBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				show(null);
			}
		});

		buttonGroup = new ButtonGroup();
		dockables = new Vector();
		dockablePanel = new DockablePanel();

		dimension = jEdit.getIntegerProperty(
			"view.dock." + position + ".dimension",0);
	}

	public void register(final DockableWindowManager.Entry entry)
	{
		dockables.addElement(entry);

		int rotation;
		if(position.equals(DockableWindowManager.TOP)
			|| position.equals(DockableWindowManager.BOTTOM))
			rotation = CustomButton.NONE;
		else if(position.equals(DockableWindowManager.LEFT))
			rotation = CustomButton.CCW;
		else if(position.equals(DockableWindowManager.RIGHT))
			rotation = CustomButton.CW;
		else
			throw new InternalError("Invalid position: " + position);

		CustomButton button = new CustomButton(rotation,entry.title);
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				wm.showDockableWindow(entry.name);
			}
		});

		// this is a hack so that the first button added gives the close box
		// it's proper size
		if(buttons.getComponentCount() == 1)
		{
			Dimension size = button.getPreferredSize();
			Dimension dim = new Dimension(
				Math.min(size.width,size.height),
				Math.min(size.width,size.height));
			closeBox.setMinimumSize(dim);
			closeBox.setPreferredSize(dim);
			closeBox.setMaximumSize(dim);
		}

		buttonGroup.add(button);
		buttons.add(button);

		wm.revalidate();
	}

	public void add(DockableWindowManager.Entry entry)
	{
		dockablePanel.add(entry.name,entry.win);
	}

	public void remove(DockableWindowManager.Entry entry)
	{
		int index = dockables.indexOf(entry);
		buttons.remove(index + 1);

		dockables.removeElement(entry);
		if(entry.win != null)
			dockablePanel.remove(entry.win);

		if(current == entry)
		{
			current = null;
			show(null);
		}
		else
			wm.revalidate();
	}

	public void save(DockableWindowManager.Entry entry) {}

	public void show(DockableWindowManager.Entry entry)
	{
		if(current == entry)
		{
			if(entry != null)
				entry.win.requestDefaultFocus();
			return;
		}

		if(current == null)
		{
			// we didn't have a component previously, so create a border
			dockablePanel.setBorder(new DockBorder(position));
		}

		if(entry != null)
		{
			this.current = entry;

			dockablePanel.showDockable(entry.name);

			int index = dockables.indexOf(entry);
			((JToggleButton)buttons.getComponent(index + 1)).setSelected(true);

			entry.win.requestDefaultFocus();
		}
		else
		{
			current = null;
			buttonGroup.setSelected(null,true);
			// removing last component, so remove border
			dockablePanel.setBorder(null);
		}

		wm.revalidate();
		dockablePanel.repaint();
	}

	public boolean isVisible(DockableWindowManager.Entry entry)
	{
		return current == entry;
	}

	public DockableWindowManager.Entry getCurrent()
	{
		return current;
	}

	// package-private members
	void save()
	{
		jEdit.setIntegerProperty("view.dock." + position + ".dimension",
			dimension);
		if(current == null)
			jEdit.unsetProperty("view.dock." + position + ".last");
		else
		{
			jEdit.setProperty("view.dock." + position + ".last",
				current.name);
		}
	}

	ButtonBox getButtonBox()
	{
		return buttons;
	}

	DockablePanel getDockablePanel()
	{
		return dockablePanel;
	}

	// private members
	private static final Icon CLOSE_BOX
		 = GUIUtilities.loadIcon("closebox.gif");
	private static final int SPLITTER_WIDTH = 10;

	private DockableWindowManager wm;
	private String position;
	private ButtonBox buttons;
	private JButton closeBox;
	private ButtonGroup buttonGroup;
	private int dimension;
	private Vector dockables;
	private DockablePanel dockablePanel;
	private DockableWindowManager.Entry current;

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

	static class CustomButton extends JToggleButton
	{
		static final int NONE = 0;
		static final int CW = 1;
		static final int CCW = 2;

		public CustomButton(int rotate, String text)
		{
			setMargin(new Insets(0,0,0,0));
			setRequestFocusEnabled(false);
			if(MiscUtilities.compareStrings(
				System.getProperty("java.version"),
				"1.2",false) >= 0)
			{
				setIcon(new RotatedTextIcon2D(rotate,text));
			}
			else
			{
				setIcon(new RotatedTextIcon(rotate,text));
			}
		}

		class RotatedTextIcon implements Icon
		{
			int rotate;
			String text;
			int width;
			int height;
			Image rotated;

			RotatedTextIcon(int rotate, String text)
			{
				this.rotate = rotate;
				this.text = text;
				FontMetrics fm = getFontMetrics(getFont());
				width = fm.stringWidth(text);
				height = fm.getHeight();
			}

			public int getIconWidth()
			{
				return (rotate == CW || rotate == CCW
					? height : width);
			}

			public int getIconHeight()
			{
				return (rotate == CW || rotate == CCW
					? width : height);
			}

			public void paintIcon(Component c, Graphics g, int x, int y)
			{
				FontMetrics fm = g.getFontMetrics();

				if(rotate == NONE)
				{
					g.setColor(getForeground());
					g.drawString(text,x,y + fm.getAscent());
					return;
				}

				if(rotated == null)
				{
					Image rotImg = c.createImage(width,height);
					Graphics rotGfx = rotImg.getGraphics();
					rotGfx.setColor(getBackground());
					rotGfx.fillRect(0,0,getWidth(),getHeight());
					rotGfx.setColor(Color.black);

					rotGfx.drawString(text,0,fm.getAscent());

					ImageFilter filter = new RotationFilter(rotate);
					rotated = createImage(new FilteredImageSource(
						rotImg.getSource(),filter));
				}

				g.drawImage(rotated,x,y,c);
			}
		}

		static class RotationFilter extends ImageFilter
		{
			int rotation;
			int width;
			int height;
			IndexColorModel indexModel;

			RotationFilter(int rotation)
			{
				this.rotation = rotation;
				indexModel = new IndexColorModel(8,2,
					new byte[] { 0, 0 },
					new byte[] { 0, 0 },
					new byte[] { 0, 0 },
					1);
			}

			public void setDimensions(int width, int height)
			{
				this.width = height;
				this.height = width;
				super.setDimensions(height,width);
			}

			public void setColorModel(ColorModel model)
			{
				super.setColorModel(indexModel);
			}

			// Fuck all the retards at Sun who made it impossible to
			// write polymorphic array-access code.

			public void setPixels(int x, int y, int w, int h,
				ColorModel model, byte pixels[], int off,
				int scansize)
			{
				byte[] retVal = new byte[h * w];

				for(int i = 0; i < w; i++)
				{
					for(int j = 0; j < h; j++)
					{
						byte value = pixels[j * scansize
							+ (rotation == CW
							? i : scansize - i - 1) + off];

						retVal[i * h + (h - j - 1)]
							= (byte)(value == 0 ? 0 : 1);
					}
				}

				super.setPixels((rotation == CCW
					? y : width - y - 1),
					x,h,w,indexModel,retVal,0,h);
			}

			public void setPixels(int x, int y, int w, int h,
				ColorModel model, int pixels[], int off,
				int scansize)
			{
				byte[] retVal = new byte[h * w];

				for(int i = 0; i < w; i++)
				{
					for(int j = 0; j < h; j++)
					{
						int value = pixels[j * scansize
							+ (rotation == CW
							? i : scansize - i - 1) + off];

						retVal[i * h + (h - j - 1)]
							= (byte)(value == 0 ? 0 : 1);
					}
				}

				super.setPixels((rotation == CCW
					? y : width - y - h),
					(rotation == CW
					? x : height - x - w),h,w,
					indexModel,retVal,0,h);
			}
		}

		class RotatedTextIcon2D implements Icon
		{
			int rotate;
			String text;
			int width;
			int height;

			RotatedTextIcon2D(int rotate, String text)
			{
				this.rotate = rotate;
				this.text = text;
				FontMetrics fm = getFontMetrics(getFont());
				width = fm.stringWidth(text);
				height = fm.getHeight();
			}

			public int getIconWidth()
			{
				return (rotate == CW || rotate == CCW
					? height : width);
			}

			public int getIconHeight()
			{
				return (rotate == CW || rotate == CCW
					? width : height);
			}

			public void paintIcon(Component c, Graphics g, int x, int y)
			{
				FontMetrics fm = g.getFontMetrics();

				Graphics2D g2d = (Graphics2D)g;
				AffineTransform oldTransform = g2d.getTransform();

				g2d.setColor(getForeground());

				if(rotate == NONE)
				{
					g2d.setTransform(AffineTransform
						.getRotateInstance(
						0,x,y));
					g2d.drawString(text,x,y + fm.getAscent());
				}
				else if(rotate == CW)
				{
					g2d.setTransform(AffineTransform
						.getRotateInstance(
						Math.PI / 3,x,y));
					g2d.fillRect(0,0,1000,1000);
					g2d.drawString(text,y,x + fm.getAscent());
				}
				else if(rotate == CCW)
				{
					g2d.setTransform(AffineTransform
						.getRotateInstance(
						-Math.PI / 3,x,y));
					g2d.drawString(text,y,x + fm.getAscent());
				}

				g2d.setTransform(oldTransform);
			}
		}
	}

	class ButtonBox extends Box
	{
		ButtonBox()
		{
			super((position.equals(DockableWindowManager.TOP)
				|| position.equals(DockableWindowManager.BOTTOM))
				? BoxLayout.X_AXIS
				: BoxLayout.Y_AXIS);
		}

		public Dimension getPreferredSize()
		{
			if(dockables.size() == 0)
				return new Dimension(0,0);
			else
				return super.getPreferredSize();
		}
	}

	class DockablePanel extends JPanel
	{
		DockablePanel()
		{
			super(new CardLayout());

			ResizeMouseHandler resizeMouseHandler = new ResizeMouseHandler();
			addMouseListener(resizeMouseHandler);
			addMouseMotionListener(resizeMouseHandler);
		}

		void showDockable(String name)
		{
			((CardLayout)getLayout()).show(this,name);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(0,0);
		}

		public Dimension getPreferredSize()
		{
			if(current == null)
				return new Dimension(0,0);
			else
			{
				if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
				{
					return new Dimension(0,
						dimension + SPLITTER_WIDTH + 3);
				}
				else
				{
					return new Dimension(dimension + SPLITTER_WIDTH + 3,
						0);
				}
			}
		}

		class ResizeMouseHandler extends MouseAdapter implements MouseMotionListener
		{
			boolean canDrag;
			int dragStartDimension;
			Point dragStart;

			public void mousePressed(MouseEvent evt)
			{
				dragStartDimension = dimension;
				dragStart = evt.getPoint();
			}

			public void mouseMoved(MouseEvent evt)
			{
				Border border = getBorder();
				if(border == null)
				{
					// collapsed
					return;
				}

				Insets insets = border.getBorderInsets(DockablePanel.this);
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
				{
					dimension = evt.getY()
						+ dragStartDimension
						- dragStart.y;
				}
				else if(position.equals(DockableWindowManager.LEFT))
				{
					dimension = evt.getX()
						+ dragStartDimension
						- dragStart.x;
				}
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					dimension += (dragStart.y - evt.getY());
				}
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					dimension += (dragStart.x - evt.getX());
				}

				if(dimension <= 0)
					dimension = dragStartDimension;

				wm.invalidate();
				wm.validate();
			}

			public void mouseExited(MouseEvent evt)
			{
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			}
		}
	}
}
