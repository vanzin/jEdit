/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2019 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.GenericGUIUtilities;

import static java.awt.Color.RED;
import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.round;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createRaisedBevelBorder;
import static javax.swing.Box.createRigidArea;
import static javax.swing.BoxLayout.PAGE_AXIS;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
//}}}

public class ScreenRectangleSelectionButton extends JButton
{
	//{{{ ColorWellButton constructor
	public ScreenRectangleSelectionButton(BufferedImage image)
	{
		setIcon(new ScreenRectangle(image));
		setMargin(new Insets(2, 2, 2, 2));
		addActionListener(new ActionHandler());
	} //}}}

	//{{{ getSelectedImage() method
	public BufferedImage getSelectedImage()
	{
		return ((ScreenRectangle)getIcon()).image;
	} //}}}

	//{{{ setSelectedImage() method
	public void setSelectedImage(BufferedImage image)
	{
		((ScreenRectangle)getIcon()).image = image;
		repaint();
		fireStateChanged();
	} //}}}

	//{{{ setRectangleDimension() method
	public void setRectangleDimension(Dimension dimension)
	{
		((ScreenRectangle)getIcon()).dimension = dimension;
		repaint();
		fireStateChanged();
	} //}}}

	//{{{ ScreenRectangle class
	static class ScreenRectangle implements Icon
	{
		Dimension dimension;
		BufferedImage image;

		ScreenRectangle(BufferedImage image)
		{
			this.image = image;
		}

		public int getIconWidth()
		{
			return dimension.width;
		}

		public int getIconHeight()
		{
			return dimension.height;
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			g = g.create();
			g.setColor(c.getBackground());
			g.fillRect(x, y, getIconWidth(), getIconHeight());

			if (image != null)
			{
				g.setClip(x, y, getIconWidth(), getIconHeight());
				g.drawImage(image, x, y, null);
			}
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			int iconWidth = getIcon().getIconWidth();
			int iconHeight = getIcon().getIconHeight();
			int zoomedWidth = iconWidth * 12;
			int zoomedHeight = iconHeight * 12;

			JLabel directionsLabel = new JLabel(
					jEdit.getProperty("screen-rectangle-selection.directions"));

			ImageIcon rectangleIcon = new ImageIcon();
			JLabel rectangleLabel = new JLabel(rectangleIcon) {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					g = g.create();
					g.setColor(RED);
					g.drawRect(
							iconWidth * 2 - 1, iconHeight * 2 - 1,
							iconWidth * 8 + 1, iconHeight * 8 + 1);
				}
			};
			rectangleLabel.setMinimumSize(new Dimension(zoomedWidth, zoomedHeight));
			rectangleLabel.setPreferredSize(new Dimension(zoomedWidth, zoomedHeight));
			rectangleLabel.setMaximumSize(new Dimension(zoomedWidth, zoomedHeight));
			Robot[] robot = new Robot[1];
			try {
				robot[0] = new Robot();
			} catch (AWTException ignore) {
			}

			int rectangleUpperLeftDeltaX = round(iconWidth * 0.75F);
			int rectangleUpperLeftDeltaY = round(iconHeight * 0.75F);
			Rectangle rectangle = new Rectangle(
					round(iconWidth * 1.5F), round(iconHeight * 1.5F));

			BufferedImage[] rectangleImage = new BufferedImage[1];

			Timer timer = new Timer(100, e -> {
				Point mousePointerLocation = MouseInfo.getPointerInfo().getLocation();
				rectangle.x = mousePointerLocation.x - rectangleUpperLeftDeltaX;
				rectangle.y = mousePointerLocation.y - rectangleUpperLeftDeltaY;

				rectangleImage[0] = robot[0].createScreenCapture(rectangle);
				rectangleIcon.setImage(rectangleImage[0].getScaledInstance(
						zoomedWidth, zoomedHeight, SCALE_SMOOTH));
				rectangleLabel.repaint();
			});
			timer.setInitialDelay(0);

			// if we couldn't get a robot, we cannot take screen pixels
			if (robot[0] != null)
				timer.start();

			JPanel rectangleSelectorPanel = new JPanel();
			rectangleSelectorPanel.setLayout(
					new BoxLayout(rectangleSelectorPanel, PAGE_AXIS));
			rectangleSelectorPanel.setBorder(new CompoundBorder(
					createRaisedBevelBorder(),
					createEmptyBorder(10, 10, 10, 10)));
			directionsLabel.setAlignmentX(CENTER_ALIGNMENT);
			rectangleSelectorPanel.add(directionsLabel);
			rectangleSelectorPanel.add(createRigidArea(new Dimension(0, 10)));
			rectangleLabel.setAlignmentX(CENTER_ALIGNMENT);
			rectangleSelectorPanel.add(rectangleLabel);

			JDialog parent = GenericGUIUtilities
					.getParentDialog(ScreenRectangleSelectionButton.this);
			JDialog rectangleSelector = new JDialog(parent, APPLICATION_MODAL);
			rectangleSelector.setContentPane(rectangleSelectorPanel);
			rectangleSelector.setUndecorated(true);
			rectangleSelector.setAlwaysOnTop(true);
			rectangleSelector.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			rectangleSelector.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e)
				{
					switch (e.getKeyCode())
					{
						case VK_ENTER:
							timer.stop();
							rectangleSelector.dispose();
							BufferedImage selectedRectangleImage =
									new BufferedImage(iconWidth, iconHeight, TYPE_INT_RGB);
							Graphics2D g = selectedRectangleImage.createGraphics();
							try
							{
								int sx1 = round(iconWidth * 0.25F);
								int sy1 = round(iconHeight * 0.25F);
								int sx2 = iconWidth + sx1;
								int sy2 = iconHeight + sy1;
								g.drawImage(
										rectangleImage[0], 0, 0, iconWidth, iconHeight,
										sx1, sy1, sx2, sy2, null);
							}
							finally
							{
								g.dispose();
							}
							setSelectedImage(selectedRectangleImage);
							break;

						case VK_ESCAPE:
							timer.stop();
							rectangleSelector.dispose();
							break;

						default:
							break;
					}
				}
			});
			rectangleSelector.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					rectangleSelector.pack();
					rectangleSelector.setLocationRelativeTo(
							ScreenRectangleSelectionButton.this);
				}
			});
			rectangleSelector.pack();
			rectangleSelector.setLocationRelativeTo(
					ScreenRectangleSelectionButton.this);
			rectangleSelector.setVisible(true);
		}
	} //}}}
}
