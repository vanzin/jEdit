/*
 * AnimatedIcon.java - Animated version of ImageIcon
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
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
import java.awt.event.*;
import javax.swing.*;
//}}}

/**
 * A animated version of ImageIcon. It can be used anywhere an ImageIcon can be.
 */
public class AnimatedIcon extends ImageIcon
{
	//{{{ AnimatedIcon constructor
	/**
	 * @param frames The frames to be used in the animation
	 * @param rate The frame rate of the animation, in frames per second
	 * @param host The container that the animation is used in
	 */
	public AnimatedIcon(Image icon, Image[] frames, int rate, Component host)
	{
		super(icon);
		this.icon = icon;
		this.frames = frames;
		delay = 1000/rate;
		this.host = host;
	} //}}}

	//{{{ getFrames() method
	public Image[] getFrames()
	{
		return frames;
	} //}}}

	//{{{ getIcon() method
	public Image getIcon()
	{
		return icon;
	} //}}}

	//{{{ getRate() method
	public int getRate()
	{
		return 1000/delay;
	} //}}}

	//{{{ setFrames() method
	public void setFrames(Image[] frames)
	{
		this.frames = frames;
	} //}}}

	//{{{ setIcon() method
	public void setIcon(Image icon)
	{
		this.icon = icon;
	} //}}}

	//{{{ setRate() method
	public void setRate(int rate)
	{
		delay = 1000/rate;
	} //}}}

	//{{{ start() method
	/**
	 * Starts the animation rolling
	 */
	public void start()
	{
		if(timer != null)
			return;

		timer = new Timer(delay,new Animator());
		timer.start();
	} //}}}

	//{{{ stop() method
	/**
	 * Stops the animation, and resets to frame 0
	 */
	public void stop()
	{
		current = 0;
		if(timer != null)
		{
			timer.stop();
			timer = null;
		}

		setImage(icon);
		host.repaint();
	} //}}}

	//{{{ Private members
	private Image[] frames;
	private int current;
	private int delay;
	private Timer timer;
	private Component host;
	private Image icon;
	//}}}

	//{{{ Animator class
	class Animator implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			current = (current + 1) % frames.length;
			setImage(frames[current]);
			host.repaint();
		}
	} //}}}
}
