/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 jEdit contributors
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

package org.gjt.sp.jedit.gui.tray;

//{{{ Imports
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import javax.swing.JPanel;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.ImageObserver.HEIGHT;
import static java.awt.image.ImageObserver.WIDTH;
import static java.lang.Math.round;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class JTrayIconManager
{
	private static JEditTrayIcon trayIcon;
	private static Image originalTrayIconImage;
	private static boolean restore;
	private static String userDir;
	private static String[] args;

	//{{{ setTrayIconArgs() method
	public static void setTrayIconArgs(boolean restore, String userDir, String[] args)
	{
		JTrayIconManager.restore = restore;
		JTrayIconManager.userDir = userDir;
		JTrayIconManager.args = args;
	} //}}}

	//{{{ addTrayIcon() method
	public static void addTrayIcon()
	{
		if (trayIcon == null && SystemTray.isSupported())
		{
			String trayIconName = jEdit.getProperty("systrayicon.service", "swing");
			trayIcon = ServiceManager.getService(JEditTrayIcon.class, trayIconName);
			if (trayIcon == null)
			{
				if ("swing".equals(trayIconName))
				{
					Log.log(Log.ERROR, JTrayIconManager.class, "No service " +
						JEditTrayIcon.class.getName() + " with name swing");
					return;
				}
				Log.log(Log.WARNING, JTrayIconManager.class, "No service " +
					JEditTrayIcon.class.getName() + " with name "+ trayIconName);
				trayIcon = ServiceManager.getService(JEditTrayIcon.class, "swing");
			}
			if (trayIcon == null)
			{
				Log.log(Log.ERROR, JTrayIconManager.class, "No service " +
					JEditTrayIcon.class.getName() + " with name swing");
				return;
			}
			trayIcon.setTrayIconArgs(restore, userDir, args);
			originalTrayIconImage = trayIcon.getImage();

			SystemTray systemTray = SystemTray.getSystemTray();

			boolean backgroundDefault = false;
			boolean backgroundFixed = false;
			boolean backgroundPicked = false;
			boolean backgroundAutodetect = false;
			switch (jEdit
					.getProperty("systrayicon.background", "autodetect")
					.toLowerCase())
			{
				case "default":
					backgroundDefault = true;
					break;
				case "fixed":
					backgroundFixed = true;
					break;
				case "picked":
					backgroundPicked = true;
					break;
				default:
					backgroundAutodetect = true;
			}

			if (OperatingSystem.isX11() && !backgroundDefault)
			{
				try
				{
					// fail fast if robot creation is necessary but not possible
					Robot robot = null;
					if (backgroundAutodetect)
						robot = new Robot();

					// scale the image to the tray icon size
					Dimension trayIconSize = systemTray.getTrayIconSize();
					BufferedImage trayIconImage = new BufferedImage(
							trayIconSize.width, trayIconSize.height, TYPE_INT_ARGB);
					Graphics2D g = trayIconImage.createGraphics();
					CompletableFuture<Integer> widthFuture = new CompletableFuture<>();
					CompletableFuture<Integer> heightFuture = new CompletableFuture<>();
					int width = originalTrayIconImage.getWidth((img, infoflags, x, y, w, h) ->
					{
						if ((infoflags & WIDTH) == WIDTH)
							widthFuture.complete(w);
						return !widthFuture.isDone();
					});
					if (width != -1)
						widthFuture.complete(width);
					int height = originalTrayIconImage.getHeight((img, infoflags, x, y, w, h) ->
					{
						if ((infoflags & HEIGHT) == HEIGHT)
							heightFuture.complete(h);
						return !heightFuture.isDone();
					});
					if (height != -1)
						heightFuture.complete(height);
					try
					{
						g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
						double scaleFactorX = trayIconSize.getWidth() / widthFuture.join();
						double scaleFactorY = trayIconSize.getHeight() / heightFuture.join();
						g.drawImage(originalTrayIconImage,
								AffineTransform.getScaleInstance(scaleFactorX, scaleFactorY), null);
					}
					finally
					{
						g.dispose();
					}

					// incorporate the configured background
					Color[][] backgroundColorMatrix = null;
					int backgroundColorMatrixWidth = 0;
					int backgroundColorMatrixHeight = 0;
					if (backgroundPicked)
					{
						backgroundColorMatrix =
								jEdit.getColorMatrixProperty("systrayicon.bgPixel");
						if (backgroundColorMatrix != null)
						{
							backgroundColorMatrixWidth = backgroundColorMatrix[0].length;
							backgroundColorMatrixHeight = backgroundColorMatrix.length;
						}
					}

					Color backgroundColor = null;

					// use the same background color for all pixel
					if (backgroundFixed)
						backgroundColor = jEdit.getColorProperty("systrayicon.bgColor");

					Color defaultBackgroundColor = null;
					if (backgroundPicked)
						defaultBackgroundColor = new JPanel().getBackground();

					for (int y = 0; y < trayIconSize.height; y++)
					{
						// autodetect the background color from screenshot
						if (backgroundAutodetect)
							backgroundColor = robot.getPixelColor(1, y);

						for (int x = 0; x < trayIconSize.width; x++)
						{
							// use configured background matrix
							if (backgroundPicked)
								if ((x >= backgroundColorMatrixWidth)
										|| (y >= backgroundColorMatrixHeight))
									backgroundColor = defaultBackgroundColor;
								else
									backgroundColor = backgroundColorMatrix[y][x];

							int imageRGB = trayIconImage.getRGB(x, y);
							Color imageColor = new Color(imageRGB);
							float alpha = ((imageRGB >> 24) & 0xFF) / 255f;
							float antiAlpha = 1 - alpha;
							int red = round((alpha * imageColor.getRed())
									+ (antiAlpha * backgroundColor.getRed()));
							int green = round((alpha * imageColor.getGreen())
									+ (antiAlpha * backgroundColor.getGreen()));
							int blue = round((alpha * imageColor.getBlue())
									+ (antiAlpha * backgroundColor.getBlue()));
							trayIconImage.setRGB(x, y, new Color(red, green, blue).getRGB());
						}
					}

					trayIcon.setImage(trayIconImage);
				}
				catch (AWTException ignore)
				{
					// if robot is needed but cannot be created
					// we live with the potentially ugly icon background
				}
			}

			try
			{
				systemTray.add(trayIcon);
			}
			catch (AWTException e)
			{
				Log.log(Log.ERROR, JEditSwingTrayIcon.class, "Unable to add Tray icon", e);
				trayIcon = null;
				return;
			}
			EditBus.addToBus(trayIcon);
		}
	} //}}}

	//{{{ removeTrayIcon() method
	public static void removeTrayIcon()
	{
		if (trayIcon != null)
		{
			SystemTray.getSystemTray().remove(trayIcon);
			EditBus.removeFromBus(trayIcon);
			trayIcon.setImage(originalTrayIconImage);
			trayIcon = null;
		}
	} //}}}

}
