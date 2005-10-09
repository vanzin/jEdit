/*
 * HelpViewer.java - HTML Help viewer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov, Nicholas O'Leary
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

package org.gjt.sp.jedit.help;

// {{{ Imports
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;

// }}}

/**
 * A simple frame around the HelpViewerComponent
 */

public class HelpViewerFrame extends JFrame implements HelpViewer, PropertyChangeListener
{
	protected HelpViewerFrame()
	{
		component = new HelpViewerComponent();
		init();
	}

	protected HelpViewerFrame(HelpViewer hv)
	{
		component = hv;
		init();
	}

	protected void init()
	{
		
		setTitle(jEdit.getProperty("helpviewer.title"));
		// setLayout(new FlowLayout());

		component.addPropertyChangeListener(this);
		getContentPane().add((Component) component);

		setIconImage(GUIUtilities.getEditorIcon());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GUIUtilities.loadGeometry(this, "helpviewer");

		// getRootPane().setPreferredSize(new Dimension(750,500));
//		pack(); // FRAME thing
		setVisible(true);
	}

	// {{{ gotoURL() method
	/**
	 * Displays the specified URL in the HTML component.
	 * 
	 * @param url
	 *                The URL
	 * @param addToHistory
	 *                Should the URL be added to the back/forward history?
	 */
	public void gotoURL(String url, boolean addToHistory)
	{
		component.gotoURL(url, addToHistory);
	} // }}}

	// {{{ dispose() method
	public void dispose()
	{
		component.dispose();

		GUIUtilities.saveGeometry(this, "helpviewer");
		super.dispose();
	} // }}}

	// {{{ getBaseURL() method
	public String getBaseURL()
	{
		return component.getBaseURL();
	} // }}}

	// {{{ getShortURL() method
	public String getShortURL()
	{
		return component.getShortURL();
	} // }}}

	public Component getComponent()
	{
		return (Component) component;
	}

	public void queueTOCReload()
	{
		component.queueTOCReload();
	}

	private HelpViewer component;

	public void propertyChange(PropertyChangeEvent evt)
	{
		String propName = evt.getPropertyName();
		if (propName == null)
			return;
		if (propName.equals("title"))
		{
			String value = evt.getNewValue().toString();
			setTitle(value);
		}
	}

}
