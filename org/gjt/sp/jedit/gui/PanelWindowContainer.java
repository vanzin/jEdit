/*
 * PanelWindowContainer.java - holds dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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
	public PanelWindowContainer(DockableWindowManager wm, String position,
		int dimension)
	{
		this.wm = wm;
		this.position = position;

		//{{{ Button box setup
		buttons = new JPanel(new ButtonLayout());
		buttons.setBorder(new EmptyBorder(1,1,1,1));

		closeBox = new JButton(GUIUtilities.loadIcon("closebox.gif"));
		closeBox.setRequestFocusEnabled(false);
		closeBox.setToolTipText(jEdit.getProperty("view.docking.close-tooltip"));
		if(OperatingSystem.isMacOSLF())
			closeBox.putClientProperty("JButton.buttonType","toolbar");

		closeBox.setMargin(new Insets(0,0,0,0));
		buttons.add(closeBox);

		closeBox.addActionListener(new ActionHandler());

		menuBtn = new JButton(GUIUtilities.loadIcon("ToolbarMenu.gif"));
		menuBtn.setRequestFocusEnabled(false);
		menuBtn.setToolTipText(jEdit.getProperty("view.docking.menu-tooltip"));
		if(OperatingSystem.isMacOSLF())
			closeBox.putClientProperty("JButton.buttonType","toolbar");

		menuBtn.setMargin(new Insets(0,0,0,0));
		buttons.add(menuBtn);

		menuBtn.addMouseListener(new MenuMouseHandler());

		buttonGroup = new ButtonGroup();
		// JDK 1.4 workaround
		buttonGroup.add(nullButton = new JToggleButton());
		//}}}

		dockables = new Vector();
		dockablePanel = new DockablePanel();

		this.dimension = dimension;
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
		button.addMouseListener(new MenuMouseHandler());
		if(OperatingSystem.isMacOSLF())
			button.putClientProperty("JButton.buttonType","toolbar");
		//}}}

		buttonGroup.add(button);
		buttons.add(button);

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
		if(entry.factory.name.equals(mostRecent))
			mostRecent = null;

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

	//{{{ showMostRecent() method
	public void showMostRecent()
	{
		if(dockables.size() == 0)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		if(mostRecent == null)
		{
			mostRecent = ((DockableWindowManager.Entry)
				dockables.get(0)).factory.name;
		}

		wm.showDockableWindow(mostRecent);
	} //}}}

	//{{{ show() method
	public void show(final DockableWindowManager.Entry entry)
	{
		if(current == entry)
		{
			if(entry != null)
			{
				entry.win.requestFocus();
				entry.win.requestDefaultFocus();
			}
			return;
		}

		if(current == null)
		{
			// we didn't have a component previously, so create a border
			dockablePanel.setBorder(new DockBorder(position));
		}

		if(entry != null)
		{
			mostRecent = entry.factory.name;
			this.current = entry;

			dockablePanel.showDockable(entry.factory.name);

			int index = dockables.indexOf(entry);
			((JToggleButton)buttons.getComponent(index + 2)).setSelected(true);

			entry.win.requestFocus();
			entry.win.requestDefaultFocus();
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
	/**
	 * Returns the name of the dockable in this container.
	 * @since jEdit 4.2pre1
	 */
	public String getCurrent()
	{
		if(current == null)
			return null;
		else
			return current.factory.name;
	} //}}}

	//{{{ getDimension() method
	/**
	 * Returns the width or height (depending on position) of the dockable
	 * window container.
	 * @since jEdit 4.2pre1
	 */
	public int getDimension()
	{
		return dimension;
	} //}}}

	//{{{ getDockables() method
	public String[] getDockables()
	{
		String[] retVal = new String[dockables.size()];
		for(int i = 0; i < dockables.size(); i++)
		{
			DockableWindowManager.Entry entry =
				(DockableWindowManager.Entry) dockables.elementAt(i);
			retVal[i] = entry.factory.name;
		}
		return retVal;
	} //}}}

	//{{{ Package-private members
	static final int SPLITTER_WIDTH = 10;
	DockablePanel dockablePanel;
	JPanel buttons;

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
			this.dimension = dimension - SPLITTER_WIDTH;
	} //}}}

	//}}}

	//{{{ Private members
	private DockableWindowManager wm;
	private String position;
	private JButton closeBox;
	private JButton menuBtn;
	private ButtonGroup buttonGroup;
	private JToggleButton nullButton;
	private int dimension;
	private Vector dockables;
	private DockableWindowManager.Entry current;
	private JPopupMenu popup;

	// remember the most recent dockable
	private String mostRecent;
	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(popup != null && popup.isVisible())
				popup.setVisible(false);

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

	//{{{ MenuMouseHandler class
	class MenuMouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if(popup != null && popup.isVisible())
			{
				popup.setVisible(false);
				return;
			}

			Component comp = (Component)evt.getSource();
			String dockable;
			if(comp instanceof JToggleButton)
				dockable = ((JToggleButton)comp).getActionCommand();
			else
				dockable = getCurrent();

			if(comp == menuBtn || GUIUtilities.isPopupTrigger(evt))
			{
				if(dockable == null)
				{
					popup = wm.createPopupMenu(PanelWindowContainer.this,null,false);
				}
				else
				{
					popup = wm.createPopupMenu(PanelWindowContainer.this,dockable,false);
				}

				int x, y;
				boolean point;
				if(comp == menuBtn)
				{
					x = 0;
					y = menuBtn.getHeight();
					point = false;
				}
				else
				{
					x = evt.getX();
					y = evt.getY();
					point = true;
				}
				GUIUtilities.showPopupMenu(popup,
					comp,x,y,point);
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
				// nothing 'cept close box
				return new Dimension(0,0);
			}

			Dimension dim = comp[2].getPreferredSize();

			if(position.equals(DockableWindowManager.TOP)
				|| position.equals(DockableWindowManager.BOTTOM))
			{
				int width = parent.getWidth() - insets.right;
				int rowHeight = Math.max(dim.height,closeBox.getPreferredSize().width);
				int x = rowHeight * 2 + insets.left;
				Dimension returnValue = new Dimension(0,rowHeight
					+ insets.top + insets.bottom);

				for(int i = 2; i < comp.length; i++)
				{
					int btnWidth = comp[i].getPreferredSize().width;
					if(btnWidth + x > width)
					{
						x = btnWidth + insets.left;
						returnValue.height += rowHeight;
					}
					else
						x += btnWidth;
				}
				return returnValue;
			}
			else
			{
				int height = parent.getHeight() - insets.bottom;
				int colWidth = Math.max(dim.width,closeBox.getPreferredSize().height);
				int y = colWidth * 2 + insets.top;
				Dimension returnValue = new Dimension(colWidth
					+ insets.left + insets.right,0);

				for(int i = 2; i < comp.length; i++)
				{
					int btnHeight = comp[i].getPreferredSize().height;
					if(btnHeight + y > height)
					{
						returnValue.width += colWidth;
						y = insets.top;
					}

					y += btnHeight;
				}
				return returnValue;
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
			if(comp.length == 2)
				return;

			Dimension dim = comp[2].getPreferredSize();

			if(position.equals(DockableWindowManager.TOP)
				|| position.equals(DockableWindowManager.BOTTOM))
			{
				int width = parent.getWidth() - insets.right;
				int rowHeight = Math.max(dim.height,closeBox.getPreferredSize().width);
				int x = rowHeight * 2 + insets.left;
				int y = insets.top;
				closeBox.setBounds(insets.left,insets.top,rowHeight,rowHeight);
				menuBtn.setBounds(insets.left + rowHeight,insets.top,rowHeight,rowHeight);

				for(int i = 2; i < comp.length; i++)
				{
					int btnWidth = comp[i].getPreferredSize().width;
					if(btnWidth + x > width)
					{
						x = insets.left;
						y += rowHeight;
					}
					comp[i].setBounds(x,y,btnWidth,rowHeight);
					x += btnWidth;
				}
			}
			else
			{
				int height = parent.getHeight() - insets.bottom;
				int colWidth = Math.max(dim.width,closeBox.getPreferredSize().height);
				int x = insets.left;
				int y = colWidth * 2 + insets.top;
				closeBox.setBounds(insets.left,insets.top,colWidth,colWidth);
				menuBtn.setBounds(insets.left,insets.top + colWidth,colWidth,colWidth);

				for(int i = 2; i < comp.length; i++)
				{
					int btnHeight = comp[i].getPreferredSize().height;
					if(btnHeight + y > height)
					{
						x += colWidth;
						y = insets.top;
					}
					comp[i].setBounds(x,y,colWidth,btnHeight);
					y += btnHeight;
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

		//{{{ getWindowContainer() method
		PanelWindowContainer getWindowContainer()
		{
			return PanelWindowContainer.this;
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
					dimension = width - SPLITTER_WIDTH;
				}

				if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
				{
					return new Dimension(0,
						dimension + SPLITTER_WIDTH);
				}
				else
				{
					return new Dimension(dimension + SPLITTER_WIDTH,
						0);
				}
			}
		} //}}}

		//{{{ ResizeMouseHandler class
		class ResizeMouseHandler extends MouseAdapter implements MouseMotionListener
		{
			boolean canDrag;
			Point dragStart;
			int resizePos;

			//{{{ mousePressed() method
			public void mousePressed(MouseEvent evt)
			{
				if(canDrag)
				{
					wm.setResizePos(PanelWindowContainer.this,dimension);
					resizePos = dimension;
					dragStart = evt.getPoint();
				}
			} //}}}

			//{{{ mouseReleased() method
			public void mouseReleased(MouseEvent evt)
			{
				if(canDrag)
				{
					dimension = resizePos;
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

				Insets insets = border.getBorderInsets(DockablePanel.this);
				int cursor = Cursor.DEFAULT_CURSOR;
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

				setCursor(Cursor.getPredefinedCursor(
					getAppropriateCursor()));
			} //}}}

			//{{{ mouseDragged() method
			public void mouseDragged(MouseEvent evt)
			{
				if(!canDrag)
					return;

				if(dragStart == null) // can't happen?
					return;

				setCursor(Cursor.getPredefinedCursor(
					getAppropriateCursor()));

				//{{{ Top...
				if(position.equals(DockableWindowManager.TOP))
				{
					resizePos = evt.getY() - dragStart.y + dimension;
					wm.setResizePos(PanelWindowContainer.this,resizePos);
				} //}}}
				//{{{ Left...
				else if(position.equals(DockableWindowManager.LEFT))
				{
					resizePos = evt.getX() - dragStart.x + dimension;
					wm.setResizePos(PanelWindowContainer.this,resizePos);
				} //}}}
				//{{{ Bottom...
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					resizePos = (dimension - evt.getY() + dragStart.y);
					wm.setResizePos(PanelWindowContainer.this,resizePos);
				} //}}}
				//{{{ Right...
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					resizePos = (dimension - evt.getX() + dragStart.x);
					wm.setResizePos(PanelWindowContainer.this,resizePos);
				} //}}}
			} //}}}

			//{{{ mouseExited() method
			public void mouseExited(MouseEvent evt)
			{
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			} //}}}

			//{{{ getCursor() method
			private int getAppropriateCursor()
			{
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
		} //}}}
	} //}}}

	//}}}
}
