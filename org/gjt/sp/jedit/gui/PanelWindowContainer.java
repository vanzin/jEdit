/*
 * PanelWindowContainer.java - holds dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
//}}}

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class PanelWindowContainer implements DockableWindowContainer
{
	//{{{ PanelWindowContainer constructor
	public PanelWindowContainer(DockableWindowManager wm, String position)
	{
		this.wm = wm;
		this.position = position;

		//{{{ Button box setup
		buttons = new JPanel(new ButtonLayout());
		buttons.setBorder(new EmptyBorder(1,1,1,1));

		// the close box must be the same size as the other buttons to look good.
		// there are two ways to achieve this:
		// a) write a custom layout manager
		// b) when the first button is added, give the close box the proper size
		// I'm lazy so I chose "b". See register() for details.

		closeBox = new JButton(GUIUtilities.loadIcon("closebox.gif"));
		closeBox.setRequestFocusEnabled(false);
		closeBox.setToolTipText(jEdit.getProperty("view.docking.close-tooltip"));
		if(OperatingSystem.isMacOSLF())
			closeBox.putClientProperty("JButton.buttonType","toolbar");

		// makes it look a bit better
		int left;
		if(position.equals(DockableWindowManager.RIGHT)
			|| position.equals(DockableWindowManager.LEFT))
			left = 1;
		else
			left = 0;

		closeBox.setMargin(new Insets(0,left,0,0));
		buttons.add(closeBox);

		closeBox.addActionListener(new ActionHandler());
		closeBox.addMouseListener(new MouseHandler());

		popupButton = new JButton(GUIUtilities.loadIcon("ToolbarMenu.gif"));
		popupButton.setRequestFocusEnabled(false);
		popupButton.setToolTipText(jEdit.getProperty("view.docking.menu-tooltip"));
		if(OperatingSystem.isMacOSLF())
			popupButton.putClientProperty("JButton.buttonType","toolbar");
		buttons.add(popupButton);

		popupButton.addMouseListener(new MouseHandler());
		popup = new JPopupMenu();

		buttonGroup = new ButtonGroup();
		// JDK 1.4 workaround
		buttonGroup.add(nullButton = new JToggleButton());
		//}}}

		dockables = new Vector();
		dockablePanel = new DockablePanel();

		dimension = jEdit.getIntegerProperty(
			"view.dock." + position + ".dimension",0);

		buttons.addMouseListener(new MouseHandler());
	} //}}}

	//{{{ register() method
	public void register(final DockableWindowManager.Entry entry)
	{
		dockables.addElement(entry);

		//{{{ Create button
		int rotation;
		if(position.equals(DockableWindowManager.TOP)
			|| position.equals(DockableWindowManager.BOTTOM))
			rotation = RotatedTextIcon.NONE;
		else if(position.equals(DockableWindowManager.LEFT))
			rotation = RotatedTextIcon.CCW;
		else if(position.equals(DockableWindowManager.RIGHT))
			rotation = RotatedTextIcon.CW;
		else
			throw new InternalError("Invalid position: " + position);

		JToggleButton button = new JToggleButton();
		button.setMargin(new Insets(0,0,0,0));
		button.setRequestFocusEnabled(false);
		button.setIcon(new RotatedTextIcon(rotation,button.getFont(),
			entry.title));
		button.setActionCommand(entry.factory.name);
		button.addActionListener(new ActionHandler());
		//}}}

		buttonGroup.add(button);
		buttons.add(button);

		button.addMouseListener(new MouseHandler());

		//{{{ Create menu item
		JMenuItem menuItem = new JMenuItem(entry.title);

		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				wm.showDockableWindow(entry.factory.name);
			}
		}); //}}}

		popup.add(menuItem);

		wm.revalidate();
	} //}}}

	//{{{ add() method
	public void add(DockableWindowManager.Entry entry)
	{
		dockablePanel.add(entry.factory.name,entry.win);
	} //}}}

	//{{{ remove() method
	public void remove(DockableWindowManager.Entry entry)
	{
		int index = dockables.indexOf(entry);
		buttons.remove(index + 2);

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
	} //}}}

	//{{{ save() method
	public void save(DockableWindowManager.Entry entry) {}
	//}}}

	//{{{ show() method
	public void show(final DockableWindowManager.Entry entry)
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

			dockablePanel.showDockable(entry.factory.name);

			int index = dockables.indexOf(entry);
			((JToggleButton)buttons.getComponent(index + 2)).setSelected(true);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					entry.win.requestDefaultFocus();
				}
			});
		}
		else
		{
			current = null;
			nullButton.setSelected(true);
			// removing last component, so remove border
			dockablePanel.setBorder(null);

			wm.getView().getTextArea().requestFocus();
		}

		wm.revalidate();
		dockablePanel.repaint();
	} //}}}

	//{{{ isVisible() method
	public boolean isVisible(DockableWindowManager.Entry entry)
	{
		return current == entry;
	} //}}}

	//{{{ getCurrent() method
	public DockableWindowManager.Entry getCurrent()
	{
		return current;
	} //}}}

	//{{{ Package-private members

	//{{{ save() method
	void save()
	{
		jEdit.setIntegerProperty("view.dock." + position + ".dimension",
			dimension);
		if(current == null)
			jEdit.unsetProperty("view.dock." + position + ".last");
		else
		{
			jEdit.setProperty("view.dock." + position + ".last",
				current.factory.name);
		}
	} //}}}

	//{{{ getButtonBox() method
	JPanel getButtonBox()
	{
		return buttons;
	} //}}}

	//{{{ getDockablePanel() method
	DockablePanel getDockablePanel()
	{
		return dockablePanel;
	} //}}}

	//{{{ setDimension() method
	void setDimension(int dimension)
	{
		if(dimension != 0)
			this.dimension = dimension - SPLITTER_WIDTH - 3;
	} //}}}

	//}}}

	//{{{ Private members
	private static final int SPLITTER_WIDTH = 10;

	private DockableWindowManager wm;
	private String position;
	private JPanel buttons;
	private JButton closeBox;
	private JButton popupButton;
	private ButtonGroup buttonGroup;
	private JToggleButton nullButton;
	private int dimension;
	private Vector dockables;
	private DockablePanel dockablePanel;
	private DockableWindowManager.Entry current;
	private JPopupMenu popup;
	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == closeBox)
				show(null);
			else
			{
				if(wm.isDockableWindowVisible(evt.getActionCommand()))
					show(null);
				else
					wm.showDockableWindow(evt.getActionCommand());
			}
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if(evt.getSource() == popupButton
				|| GUIUtilities.isPopupTrigger(evt))
			{
				if(popup.isVisible())
					popup.setVisible(false);
				else
				{
					GUIUtilities.showPopupMenu(popup,
						(Component)evt.getSource(),
						evt.getX(),evt.getY());
				}
			}
		}
	} //}}}

	//{{{ DockBorder class
	static class DockBorder implements Border
	{
		String position;
		Insets insets;
		Color color1;
		Color color2;
		Color color3;

		//{{{ DockBorder constructor
		DockBorder(String position)
		{
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
		} //}}}

		//{{{ paintBorder() method
		public void paintBorder(Component c, Graphics g,
			int x, int y, int width, int height)
		{
			updateColors();

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
		} //}}}

		//{{{ getBorderInsets() method
		public Insets getBorderInsets(Component c)
		{
			return insets;
		} //}}}

		//{{{ isBorderOpaque() method
		public boolean isBorderOpaque()
		{
			return false;
		} //}}}

		//{{{ paintHorizBorder() method
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
		} //}}}

		//{{{ paintVertBorder() method
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
		} //}}}

		//{{{ updateColors() method
		private void updateColors()
		{
			if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
			{
				color1 = MetalLookAndFeel.getControlHighlight();
				color2 = MetalLookAndFeel.getControlDarkShadow();
				color3 = MetalLookAndFeel.getControl();
			}
			else
			{
				color1 = color2 = color3 = null;
			}
		} //}}}
	} //}}}

	//{{{ RotatedTextIcon class
	public static class RotatedTextIcon implements Icon
	{
		public static final int NONE = 0;
		public static final int CW = 1;
		public static final int CCW = 2;

		//{{{ RotatedTextIcon constructor
		public RotatedTextIcon(int rotate, Font font, String text)
		{
			this.rotate = rotate;
			this.font = font;

			FontRenderContext fontRenderContext
				= new FontRenderContext(null,true,true);
			this.text = text;
			glyphs = font.createGlyphVector(fontRenderContext,text);
			width = (int)glyphs.getLogicalBounds().getWidth() + 4;
			//height = (int)glyphs.getLogicalBounds().getHeight();

			LineMetrics lineMetrics = font.getLineMetrics(text,fontRenderContext);
			ascent = lineMetrics.getAscent();
			height = (int)lineMetrics.getHeight();

			renderHints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			renderHints.put(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		} //}}}

		//{{{ getIconWidth() method
		public int getIconWidth()
		{
			return (int)(rotate == RotatedTextIcon.CW
				|| rotate == RotatedTextIcon.CCW
				? height : width);
		} //}}}

		//{{{ getIconHeight() method
		public int getIconHeight()
		{
			return (int)(rotate == RotatedTextIcon.CW
				|| rotate == RotatedTextIcon.CCW
				? width : height);
		} //}}}

		//{{{ paintIcon() method
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2d = (Graphics2D)g;
			g2d.setFont(font);
			AffineTransform oldTransform = g2d.getTransform();
			RenderingHints oldHints = g2d.getRenderingHints();

			g2d.setRenderingHints(renderHints);
			g2d.setColor(c.getForeground());

			//{{{ No rotation
			if(rotate == RotatedTextIcon.NONE)
			{
				g2d.drawGlyphVector(glyphs,x + 2,y + ascent);
			} //}}}
			//{{{ Clockwise rotation
			else if(rotate == RotatedTextIcon.CW)
			{
				AffineTransform trans = new AffineTransform();
				trans.concatenate(oldTransform);
				trans.translate(x,y + 2);
				trans.rotate(Math.PI / 2,
					height / 2, width / 2);
				g2d.setTransform(trans);
				g2d.drawGlyphVector(glyphs,(height - width) / 2,
					(width - height) / 2
					+ ascent);
			} //}}}
			//{{{ Counterclockwise rotation
			else if(rotate == RotatedTextIcon.CCW)
			{
				AffineTransform trans = new AffineTransform();
				trans.concatenate(oldTransform);
				trans.translate(x,y - 2);
				trans.rotate(Math.PI * 3 / 2,
					height / 2, width / 2);
				g2d.setTransform(trans);
				g2d.drawGlyphVector(glyphs,(height - width) / 2,
					(width - height) / 2
					+ ascent);
			} //}}}

			g2d.setTransform(oldTransform);
			g2d.setRenderingHints(oldHints);
		} //}}}

		//{{{ Private members
		private int rotate;
		private Font font;
		private String text;
		private GlyphVector glyphs;
		private float width;
		private float height;
		private float ascent;
		private RenderingHints renderHints;
		//}}}
	} //}}}

	//{{{ ButtonLayout class
	class ButtonLayout implements LayoutManager
	{
		//{{{ addLayoutComponent() method
		public void addLayoutComponent(String name, Component comp) {} //}}}

		//{{{ removeLayoutComponent() method
		public void removeLayoutComponent(Component comp) {} //}}}

		//{{{ preferredLayoutSize() method
		public Dimension preferredLayoutSize(Container parent)
		{
			Insets insets = ((JComponent)parent).getBorder()
				.getBorderInsets((JComponent)parent);

			Component[] comp = parent.getComponents();
			if(comp.length == 2)
			{
				// nothing 'cept close box and popup button
				return new Dimension(0,0);
			}
			else
			{
				if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
				{
					return new Dimension(0,
						comp[2].getPreferredSize().height
						+ insets.top
						+ insets.bottom);
				}
				else
				{
					return new Dimension(
						comp[2].getPreferredSize().width
						+ insets.left + insets.right,0);
				}
			}
		} //}}}

		//{{{ minimumLayoutSize() method
		public Dimension minimumLayoutSize(Container parent)
		{
			return preferredLayoutSize(parent);
		} //}}}

		//{{{ layoutContainer() method
		public void layoutContainer(Container parent)
		{
			Insets insets = ((JComponent)parent).getBorder()
				.getBorderInsets((JComponent)parent);

			Component[] comp = parent.getComponents();
			if(comp.length != 2)
			{
				boolean closeBoxSizeSet = false;
				boolean noMore = false;
				popupButton.setVisible(false);

				Dimension parentSize = parent.getSize();
				int pos = (position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM)
					) ? 0 : insets.left;

				for(int i = 2; i < comp.length; i++)
				{
					Dimension size = comp[i].getPreferredSize();
					if(position.equals(DockableWindowManager.TOP)
						|| position.equals(DockableWindowManager.BOTTOM))
					{
						if(!closeBoxSizeSet)
						{
							closeBox.setBounds(pos,
								insets.top,
								size.height,size.height);
							pos += size.height;
							closeBoxSizeSet = true;
						}

						if(noMore || pos + size.width > parentSize.width
							- (i == comp.length - 1
							? 0 : closeBox.getWidth()))
						{
							popupButton.setBounds(
								parentSize.width - size.height
								- insets.right,
								insets.top,size.height,
								size.height);
							popupButton.setVisible(true);
							comp[i].setVisible(false);
							noMore = true;
						}
						else
						{
							comp[i].setBounds(pos,insets.top,
								size.width,size.height);
							comp[i].setVisible(true);
							pos += size.width;
						}
					}
					else
					{
						if(!closeBoxSizeSet)
						{
							closeBox.setBounds(insets.left,
								insets.top,size.width,size.width);
							pos += size.width;
							closeBoxSizeSet = true;
						}

						if(noMore || pos + size.height > parentSize.height
							- (i == comp.length - 1
							? 0 : closeBox.getHeight()))
						{
							popupButton.setBounds(
								insets.top,
								parentSize.height - size.width,
								size.width,size.width);
							popupButton.setVisible(true);
							comp[i].setVisible(false);
							noMore = true;
						}
						else
						{
							comp[i].setBounds(insets.left,
								pos,size.width,size.height);
							comp[i].setVisible(true);
							pos += size.height;
						}
					}
				}
			}
		} //}}}
	} //}}}

	//{{{ DockablePanel class
	class DockablePanel extends JPanel
	{
		//{{{ DockablePanel constructor
		DockablePanel()
		{
			super(new CardLayout());

			ResizeMouseHandler resizeMouseHandler = new ResizeMouseHandler();
			addMouseListener(resizeMouseHandler);
			addMouseMotionListener(resizeMouseHandler);
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
			if(current == null)
				return new Dimension(0,0);
			else
			{
				if(dimension <= 0)
				{
					int width = super.getPreferredSize().width;
					dimension = width - SPLITTER_WIDTH - 3;
				}

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
		} //}}}

		//{{{ ResizeMouseHandler class
		class ResizeMouseHandler extends MouseAdapter implements MouseMotionListener
		{
			boolean canDrag;
			int dragStartDimension;
			Point dragStart;

			//{{{ mousePressed() method
			public void mousePressed(MouseEvent evt)
			{
				dragStartDimension = dimension;
				dragStart = evt.getPoint();
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

				Insets insets = border.getBorderInsets(DockablePanel.this);
				int cursor = Cursor.DEFAULT_CURSOR;
				canDrag = false;
				//{{{ Top...
				if(position.equals(DockableWindowManager.TOP))
				{
					if(evt.getY() >= getHeight() - insets.bottom)
					{
						cursor = Cursor.N_RESIZE_CURSOR;
						canDrag = true;
					}
				} //}}}
				//{{{ Left...
				else if(position.equals(DockableWindowManager.LEFT))
				{
					if(evt.getX() >= getWidth() - insets.right)
					{
						cursor = Cursor.W_RESIZE_CURSOR;
						canDrag = true;
					}
				} //}}}
				//{{{ Bottom...
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					if(evt.getY() <= insets.top)
					{
						cursor = Cursor.S_RESIZE_CURSOR;
						canDrag = true;
					}
				} //}}}
				//{{{ Right...
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					if(evt.getX() <= insets.left)
					{
						cursor = Cursor.E_RESIZE_CURSOR;
						canDrag = true;
					}
				} //}}}

				setCursor(Cursor.getPredefinedCursor(cursor));
			} //}}}

			//{{{ mouseDragged() method
			public void mouseDragged(MouseEvent evt)
			{
				if(!canDrag)
					return;

				if(dragStart == null) // can't happen?
					return;

				//{{{ Top...
				if(position.equals(DockableWindowManager.TOP))
				{
					dimension = evt.getY()
						+ dragStartDimension
						- dragStart.y;
				} //}}}
				//{{{ Left...
				else if(position.equals(DockableWindowManager.LEFT))
				{
					dimension = evt.getX()
						+ dragStartDimension
						- dragStart.x;
				} //}}}
				//{{{ Bottom...
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					dimension += (dragStart.y - evt.getY());
				} //}}}
				//{{{ Right...
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					dimension += (dragStart.x - evt.getX());
				} //}}}

				if(dimension <= 0)
					dimension = dragStartDimension;

				wm.invalidate();
				wm.validate();
			} //}}}

			//{{{ mouseExited() method
			public void mouseExited(MouseEvent evt)
			{
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			} //}}}
		} //}}}
	} //}}}

	//}}}
}
