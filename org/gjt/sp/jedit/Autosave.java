/*
 * Autosave.java - Autosave manager
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

package org.gjt.sp.jedit;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Slava Pestov
 * @version $Id$
 */
class Autosave implements ActionListener
{
	public static void setInterval(int interval)
	{
		if(interval == 0)
		{
			if(timer != null)
			{
				timer.stop();
				timer = null;
			}

			return;
		}

		interval *= 1000;

		if(timer == null)
		{
			timer = new Timer(interval,new Autosave());
			timer.start();
		}
		else
			timer.setDelay(interval);
	}

	public static void stop()
	{
		if(timer != null)
			timer.stop();
	}

	public void actionPerformed(ActionEvent evt)
	{
		// save list of open files
		if(jEdit.getFirstView() != null)
			jEdit.saveOpenFiles(jEdit.getFirstView());

		Buffer[] bufferArray = jEdit.getBuffers();
		for(int i = 0; i < bufferArray.length; i++)
			bufferArray[i].autosave();
	}

	// private members
	private static Timer timer;

	private Autosave() {}
}
