/*
 * HelpViewerComponent.java - HTML Help viewer
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
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

// }}}

/**
 * jEdit's searchable help viewer. It uses a Swing JEditorPane to display the
 * HTML, and implements a URL history.
 * 
 * @author Slava Pestov
 * @version $Id$
 */
public class HelpViewerComponent extends JPanel implements 
	EBComponent, HelpViewer, HelpHistoryModelListener
{

	// {{{ HelpViewer()
	/**
	 * Not meant to be called publicly - only by derived classes.
	 * 
	 * @see HelpViewer.create() to create new instances.
	 */
	public HelpViewerComponent()
	{
		
		ActionHandler actionListener = new ActionHandler();
		setLayout(new BorderLayout());
//		setLayout(new FlowLayout());
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(jEdit.getProperty("helpviewer.toc.label"), 
			               toc = new HelpTOCPanel(this));
		tabs.addTab(jEdit.getProperty("helpviewer.search.label"), 
					new HelpSearchPanel(this));
		tabs.setMinimumSize(new Dimension(0, 20));

		JPanel rightPanel = new JPanel(new BorderLayout());

		Box toolBar = new Box(BoxLayout.X_AXIS);
		// toolBar.setFloatable(false);

		toolBar.add(title = new JLabel());
		toolBar.add(Box.createGlue());
		historyModel = new HelpHistoryModel(25);
		back = new HistoryButton(HistoryButton.BACK, historyModel);
		back.addActionListener(actionListener);
		toolBar.add(back);
		forward = new HistoryButton(HistoryButton.FORWARD, historyModel);
		forward.addActionListener(actionListener);
		toolBar.add(forward);
		back.setPreferredSize(forward.getPreferredSize());
		rightPanel.add(BorderLayout.NORTH, toolBar);

		viewer = new JEditorPane();
		viewer.setEditable(false);
		viewer.addHyperlinkListener(new LinkHandler());
		viewer.setFont(new Font("Monospaced", Font.PLAIN, 12));
		viewer.addPropertyChangeListener(new PropertyChangeHandler());

		rightPanel.add(BorderLayout.CENTER, new JScrollPane(viewer));

		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, rightPanel);
		splitter.setBorder(null);

		// add(splitter);
		add(BorderLayout.CENTER, splitter);
		
		historyModel.addHelpHistoryModelListener(this);
		historyUpdated();
		queueTOCReload();
		EditBus.addToBus(this);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				splitter.setDividerLocation(jEdit.getIntegerProperty(
					"helpviewer.splitter", 250));
				viewer.requestFocus();
			}
		});

	} // }}}

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
		// the TOC pane looks up user's guide URLs relative to the
		// doc directory...
		String shortURL;
		setTitle(jEdit.getProperty("helpviewer.title") + ": " + url);

		try
		{
			baseURL = new File(MiscUtilities.constructPath(jEdit.getJEditHome(), "doc"))
				.toURL().toString();
		}
		catch (MalformedURLException mu)
		{
			Log.log(Log.ERROR, this, mu);
			// what to do?
		}

		if (MiscUtilities.isURL(url))
		{
			if (url.startsWith(baseURL))
			{
				shortURL = url.substring(baseURL.length());
				if (shortURL.startsWith("/"))
					shortURL = shortURL.substring(1);
			}
			else
			{
				shortURL = url;
			}
		}
		else
		{
			shortURL = url;
			if (baseURL.endsWith("/"))
				url = baseURL + url;
			else
				url = baseURL + '/' + url;
		}

		// reset default cursor so that the hand cursor doesn't
		// stick around
		viewer.setCursor(Cursor.getDefaultCursor());

		URL _url = null;
		try
		{
			_url = new URL(url);

			if (!_url.equals(viewer.getPage()))
				title.setText(jEdit.getProperty("helpviewer.loading"));
			else
			{
				/*
				 * don't show loading msg because we won't
				 * receive a propertyChanged
				 */
			}

			viewer.setPage(_url);
			if (addToHistory)
				historyModel.addToHistory(url);
		}
		catch (MalformedURLException mf)
		{
			Log.log(Log.ERROR, this, mf);
			String[] args = { url, mf.getMessage() };
			GUIUtilities.error(this, "badurl", args);
			return;
		}
		catch (IOException io)
		{
			Log.log(Log.ERROR, this, io);
			String[] args = { url, io.toString() };
			GUIUtilities.error(this, "read-error", args);
			return;
		}

		this.shortURL = shortURL;

		// select the appropriate tree node.
		if (shortURL != null)
			toc.selectNode(shortURL);
	} // }}}

	// {{{ dispose() method
	public void dispose()
	{
		EditBus.removeFromBus(this);
		jEdit.setIntegerProperty("helpviewer.splitter", splitter.getDividerLocation());
	} // }}}

	// {{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		
		
		if (msg instanceof PluginUpdate)
		{
			PluginUpdate pmsg = (PluginUpdate) msg;
			if (pmsg.getWhat() == PluginUpdate.LOADED
				|| pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				if (!pmsg.isExiting())
				{
					if (!queuedTOCReload)
						queueTOCReload();
					queuedTOCReload = true;
				}
			}
		}
	} // }}}

	// {{{ getBaseURL() method
	public String getBaseURL()
	{
		return baseURL;
	} // }}}

	// {{{ getShortURL() method
	public String getShortURL()
	{
		return shortURL;
	} // }}}

	// {{{ historyUpdated() method
	public void historyUpdated()
	{
		back.setEnabled(historyModel.hasPrevious());
		forward.setEnabled(historyModel.hasNext());
	} // }}}

	// {{{ Private members

	// {{{ Instance members
	private String baseURL;

	private String shortURL;

	private HistoryButton back;

	private HistoryButton forward;

	private JEditorPane viewer;

	private JLabel title;

	private JSplitPane splitter;

	private HelpHistoryModel historyModel;

	private HelpTOCPanel toc;

	private boolean queuedTOCReload;

	// }}}

	// {{{ queueTOCReload() method
	public void queueTOCReload()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				queuedTOCReload = false;
				toc.load();
			}
		});
	} // }}}

	// }}}

	// {{{ Inner classes

	// {{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		// {{{ actionPerformed() class
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			String url = evt.getActionCommand();
			if (!url.equals(""))
			{
				gotoURL(url, false);
				return;
			}

			if (source == back)
			{
				url = historyModel.back();
				if (url == null)
					getToolkit().beep();
				else
				{
					gotoURL(url, false);
				}
			}
			else if (source == forward)
			{
				url = historyModel.forward();
				if (url == null)
					getToolkit().beep();
				else
				{
					gotoURL(url, false);
				}
			}
		} // }}}
	} // }}}

	// {{{ LinkHandler class
	class LinkHandler implements HyperlinkListener
	{
		// {{{ hyperlinkUpdate() method
		public void hyperlinkUpdate(HyperlinkEvent evt)
		{
			if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				if (evt instanceof HTMLFrameHyperlinkEvent)
				{
					((HTMLDocument) viewer.getDocument())
						.processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) evt);
					historyUpdated();
				}
				else
				{
					URL url = evt.getURL();
					if (url != null)
						gotoURL(url.toString(), true);
				}
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED)
			{
				viewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED)
			{
				viewer.setCursor(Cursor.getDefaultCursor());
			}
		} // }}}
	} // }}}

	// {{{ PropertyChangeHandler class
	class PropertyChangeHandler implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			if ("page".equals(evt.getPropertyName()))
			{
				String titleStr = (String) viewer.getDocument()
					.getProperty("title");
				if (titleStr == null)
				{
					titleStr = MiscUtilities.getFileName(viewer.getPage()
						.toString());
				}
				title.setText(titleStr);
				historyModel.updateTitle(viewer.getPage().toString(), titleStr);
			}
		}
	} // }}}

	public void setTitle(String newTitle)
	{
		firePropertyChange("title", null, newTitle);
	}

	public Component getComponent()
	{
		return this;
	}

	// }}}
}
