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
		Buffer[] bufferArray = jEdit.getBuffers();
		for(int i = 0; i < bufferArray.length; i++)
			bufferArray[i].autosave();
	}

	// private members
	private static Timer timer;

	private Autosave() {}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:37:01  spestov
 * Initial revision
 *
 * Revision 1.8  2000/08/03 07:43:41  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.7  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.6  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.5  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 */
