/*
 * SplashScreen.java - Splash screen
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Random;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

public class SplashScreen extends JWindow
{
	public SplashScreen()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JPanel splash = new JPanel(new BorderLayout(12,12));
		splash.setBorder(new CompoundBorder(
			new MatteBorder(1,1,1,1,Color.black),
			new EmptyBorder(12,12,12,12)));
		splash.setBackground(Color.white);
		URL url = getClass().getResource("/org/gjt/sp/jedit/jedit_logo.gif");
		if(url != null)
		{
			JLabel label = new JLabel(new ImageIcon(url));
			//label.setBorder(new MatteBorder(1,1,1,1,Color.black));
			splash.add(label,BorderLayout.CENTER);
		}

		progress = new JProgressBar(0,6);
		progress.setStringPainted(true);
		//progress.setBorderPainted(false);
		progress.setString("jEdit version: " + jEdit.getVersion());
		//progress.setBackground(Color.white);
		splash.add(BorderLayout.SOUTH,progress);

		setContentPane(splash);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void advance()
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run()
				{
					progress.setValue(progress.getValue() + 1);
				}
			});
			Thread.yield();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	// private members
	private JProgressBar progress;
}
