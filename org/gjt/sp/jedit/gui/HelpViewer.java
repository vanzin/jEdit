/*
 * HelpViewer.java - HTML Help viewer
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import javax.help.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * A wrapper around JavaHelp.
 * @author Slava Pestov
 * @version $Id$
 */
public class HelpViewer extends JFrame implements EBComponent
{
	/**
	 * Creates a new help viewer for the specified URL.
	 * @param url The URL
	 */
	public HelpViewer(String url)
	{
		super(jEdit.getProperty("helpviewer.title"));

		setIconImage(GUIUtilities.getEditorIcon());

		try
		{
			helpSet = new HelpSet(HelpViewer.class.getClassLoader(),
				new URL("jeditresource:/doc/jhelpset.hs"));
			helpSet.add(new HelpSet(HelpViewer.class.getClassLoader(),
				new URL("jeditresource:/doc/users-guide/jhelpset.hs")));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}

		help = new JHelp(helpSet);

		try
		{
			help.setCurrentURL(new URL(url));
		}
		catch(MalformedURLException mf)
		{
			Log.log(Log.ERROR,this,mf);
		}

		getContentPane().add(BorderLayout.CENTER,help);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setSize(800,400);
		GUIUtilities.loadGeometry(this,"helpviewer");

		EditBus.addToBus(this);

		show();
	}

	public void dispose()
	{
		EditBus.removeFromBus(this);
		GUIUtilities.saveGeometry(this,"helpviewer");
		super.dispose();
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			SwingUtilities.updateComponentTreeUI(getRootPane());
	}

	// private members
	private HelpSet helpSet;
	private JHelp help;
}
