/*
 * HelpViewerDialog.java - HTML Help viewer
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

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;

import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import org.gjt.sp.util.Log;

import static org.gjt.sp.jedit.help.HelpHistoryModel.HistoryEntry;
//}}}

/**
 * jEdit's searchable help viewer. It uses a Swing JEditorPane to display the HTML,
 * and implements a URL history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HelpViewer extends JFrame implements HelpViewerInterface, HelpHistoryModelListener
{
	//{{{ HelpViewer constructor
	/**
	 * Creates a new help viewer with the default help page.
	 * @since jEdit 4.0pre4
	 */
	public HelpViewer()
	{
		this("welcome.html");
	} //}}}

	//{{{ HelpViewer constructor
	/**
	 * Creates a new help viewer for the specified URL.
	 * @param url The URL
	 */
	public HelpViewer(URL url)
	{
		this(url.toString());
	} //}}}

	//{{{ HelpViewer constructor
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
			baseURL = new File(MiscUtilities.constructPath(
				jEdit.getJEditHome(),"doc")).toURL().toString();
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			// what to do?
		}

		ActionHandler actionListener = new ActionHandler();

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(jEdit.getProperty("helpviewer.toc.label"),
			toc = new HelpTOCPanel(this));
		tabs.addTab(jEdit.getProperty("helpviewer.search.label"),
			new HelpSearchPanel(this));
		tabs.setMinimumSize(new Dimension(0,0));

		JPanel rightPanel = new JPanel(new BorderLayout());

		Box toolBar = new Box(BoxLayout.X_AXIS);
		//toolBar.setFloatable(false);

		toolBar.add(title = new JLabel());
		toolBar.add(Box.createGlue());
		historyModel = new HelpHistoryModel(25);
		back = new HistoryButton(HistoryButton.BACK,historyModel);
		back.addActionListener(actionListener);
		toolBar.add(back);
		forward = new HistoryButton(HistoryButton.FORWARD,historyModel);
		forward.addActionListener(actionListener);
		toolBar.add(forward);
		back.setPreferredSize(forward.getPreferredSize());
		rightPanel.add(BorderLayout.NORTH,toolBar);

		viewer = new JEditorPane();
		viewer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
			Boolean.TRUE);
		
		viewer.setEditable(false);
		viewer.addHyperlinkListener(new LinkHandler());
			 
		viewer.setFont(jEdit.getFontProperty("helpviewer.font"));
		viewer.addPropertyChangeListener(new PropertyChangeHandler());
		viewer.addKeyListener(new KeyHandler());

		viewerScrollPane = new JScrollPane(viewer);

		rightPanel.add(BorderLayout.CENTER,viewerScrollPane);

		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					  jEdit.getBooleanProperty("appearance.continuousLayout"),
					  tabs,
					  rightPanel);
		splitter.setBorder(null);


		getContentPane().add(BorderLayout.CENTER,splitter);

		historyModel.addHelpHistoryModelListener(this);
		historyUpdated();

		gotoURL(url,true,0);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		getRootPane().setPreferredSize(new Dimension(750,500));

		pack();
		GUIUtilities.loadGeometry(this,"helpviewer");
		GUIUtilities.addSizeSaver(this,"helpviewer");

		EditBus.addToBus(this);

		setVisible(true);

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				splitter.setDividerLocation(jEdit.getIntegerProperty(
					"helpviewer.splitter",250));
				viewer.requestFocus();
			}
		});
	} //}}}

	//{{{ gotoURL() method
	/**
	 * Displays the specified URL in the HTML component.
	 * 
	 * @param url 		 The URL
	 * @param addToHistory   Should the URL be added to the back/forward
	 * 			 history?
	 * @param scrollPosition The vertical scrollPosition
	 */
	public void gotoURL(String url, final boolean addToHistory, final int scrollPosition)
	{
		// the TOC pane looks up user's guide URLs relative to the
		// doc directory...
		String shortURL;
		if (MiscUtilities.isURL(url))
		{
			if (url.startsWith(baseURL))
			{
				shortURL = url.substring(baseURL.length());
				if(shortURL.startsWith("/"))
				{
					shortURL = shortURL.substring(1);
				}
			}
			else
			{
				shortURL = url;
			}
		}
		else
		{
			shortURL = url;
			if(baseURL.endsWith("/"))
			{
				url = baseURL + url;
			}
			else
			{
				url = baseURL + '/' + url;
			}
		}

		// reset default cursor so that the hand cursor doesn't
		// stick around
		viewer.setCursor(Cursor.getDefaultCursor());

		try
		{
			final URL _url = new URL(url);
			final String _shortURL = shortURL;
			if(!_url.equals(viewer.getPage()))
			{
				title.setText(jEdit.getProperty("helpviewer.loading"));
			}
			else
			{
				/* don't show loading msg because we won't
				   receive a propertyChanged */
			}

			historyModel.setCurrentScrollPosition(viewer.getPage(),getCurrentScrollPosition());
			
			/* call setPage asynchronously, because it can block when
			   one can't connect to host.
			   Calling setPage outside from the EDT violates
			   the single-tread rule of Swing, but it's an experienced workaround
			   (see merge request #2984022 - fix blocking HelpViewer
			   https://sourceforge.net/tracker/?func=detail&aid=2984022&group_id=588&atid=1235750
			   for discussion).
			   Once jEdit sets JDK 7 as dependency, all this should be
			   reverted to synchronous code.
			 */
			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						viewer.setPage(_url);
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								if (0 != scrollPosition)
								{
									viewerScrollPane.getVerticalScrollBar().setValue(scrollPosition);
								}
								if(addToHistory)
								{
									historyModel.addToHistory(_url.toString());
								}
		
								HelpViewer.this.shortURL = _shortURL;
						
								// select the appropriate tree node.
								if(_shortURL != null)
								{
									toc.selectNode(_shortURL);
								}
								
								viewer.requestFocus();
							}
						});
					}
					catch(IOException io)
					{
						Log.log(Log.ERROR,this,io);
						String[] args = { _url.toString(), io.toString() };
						GUIUtilities.error(HelpViewer.this,"read-error",args);
						return;
					}
				}
			};
			t.start();
		}
		catch(MalformedURLException mf)
		{
			Log.log(Log.ERROR,this,mf);
			String[] args = { url, mf.getMessage() };
			GUIUtilities.error(this,"badurl",args);
			return;
		}
	} //}}}

	//{{{ getCurrentScrollPosition() method
	int getCurrentScrollPosition() {
		return viewerScrollPane.getVerticalScrollBar().getValue();
	} //}}}

	//{{{ getCurrentPage() method
	URL getCurrentPage() {
		return viewer.getPage();
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		EditBus.removeFromBus(this);
		jEdit.setIntegerProperty("helpviewer.splitter",
			splitter.getDividerLocation());
		super.dispose();
	} //}}}

	//{{{ handlePluginUpdate() method
	@EBHandler
	public void handlePluginUpdate(PluginUpdate pmsg)
	{
		if(pmsg.getWhat() == PluginUpdate.LOADED
				|| pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				if(!pmsg.isExiting())
				{
					if(!queuedTOCReload)
						queueTOCReload();
					queuedTOCReload = true;
				}
			}
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		GUIUtilities.initContinuousLayout(splitter);
	} //}}}

	//{{{ getBaseURL() method
	public String getBaseURL()
	{
		return baseURL;
	} //}}}

	//{{{ getShortURL() method
	public String getShortURL()
	{
		return shortURL;
	} //}}}

	//{{{ historyUpdated() method
	public void historyUpdated()
	{
		back.setEnabled(historyModel.hasPrevious());
		forward.setEnabled(historyModel.hasNext());
	} //}}}

	//{{{ getComponent method
	public Component getComponent()
	{
		return getRootPane();
	} //}}}

	//{{{ Private members

	//{{{ Instance members
	private String baseURL;
	private String shortURL;
	private HistoryButton back;
	private HistoryButton forward;
	private JEditorPane viewer;
	private JScrollPane viewerScrollPane;
	private JLabel title;
	private JSplitPane splitter;
	private HelpHistoryModel historyModel;
	private HelpTOCPanel toc;
	private boolean queuedTOCReload;
	//}}}

	//{{{ queueTOCReload() method
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
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() class
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			String actionCommand = evt.getActionCommand();
			int separatorPosition = actionCommand.lastIndexOf(':');
			String url;
			int scrollPosition;
			if (-1 == separatorPosition)
			{
				url = actionCommand;
				scrollPosition = 0;
			}
			else
			{
				url = actionCommand.substring(0,separatorPosition);
				scrollPosition = Integer.parseInt(actionCommand.substring(separatorPosition+1));
			}
			if (url.length() != 0)
			{
				gotoURL(url,false,scrollPosition);
				return;
			}

			if(source == back)
			{
				HistoryEntry entry = historyModel.back(HelpViewer.this);
				if(entry == null)
				{
					getToolkit().beep();
				}
				else
				{
					gotoURL(entry.url,false,entry.scrollPosition);
				}
			}
			else if(source == forward)
			{
				HistoryEntry entry = historyModel.forward(HelpViewer.this);
				if(entry == null)
				{
					getToolkit().beep();
				}
				else
				{
					gotoURL(entry.url,false,entry.scrollPosition);
				}
			}
		} //}}}
	} //}}}

	//{{{ LinkHandler class
	class LinkHandler implements HyperlinkListener
	{
		//{{{ hyperlinkUpdate() method
		public void hyperlinkUpdate(HyperlinkEvent evt)
		{
			if(evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				if(evt instanceof HTMLFrameHyperlinkEvent)
				{
					((HTMLDocument)viewer.getDocument())
						.processHTMLFrameHyperlinkEvent(
						(HTMLFrameHyperlinkEvent)evt);
					historyUpdated();
				}
				else
				{
					URL url = evt.getURL();
					if(url != null)
					{
						gotoURL(url.toString(),true,0);
					}
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
		} //}}}
	} //}}}

	//{{{ PropertyChangeHandler class
	class PropertyChangeHandler implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			if("page".equals(evt.getPropertyName()))
			{
				String titleStr = (String)viewer.getDocument()
					.getProperty("title");
				if(titleStr == null)
				{
					titleStr = MiscUtilities.getFileName(
						viewer.getPage().toString());
				}
				title.setText(titleStr);
				historyModel.updateTitle(viewer.getPage().toString(),
					titleStr);
			}
		}
	} //}}}

	//{{{ KeyHandler class
	private class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent ke)
		{
			switch (ke.getKeyCode())
			{
			case KeyEvent.VK_UP:
				JScrollBar scrollBar = viewerScrollPane.getVerticalScrollBar();
				scrollBar.setValue(scrollBar.getValue()-scrollBar.getUnitIncrement(-1));
				ke.consume();
				break;
			case KeyEvent.VK_DOWN:
				scrollBar = viewerScrollPane.getVerticalScrollBar();
				scrollBar.setValue(scrollBar.getValue()+scrollBar.getUnitIncrement(1));
				ke.consume();
				break;
			case KeyEvent.VK_LEFT:
				scrollBar = viewerScrollPane.getHorizontalScrollBar();
				scrollBar.setValue(scrollBar.getValue()-scrollBar.getUnitIncrement(-1));
				ke.consume();
				break;
			case KeyEvent.VK_RIGHT:
				scrollBar = viewerScrollPane.getHorizontalScrollBar();
				scrollBar.setValue(scrollBar.getValue()+scrollBar.getUnitIncrement(1));
				ke.consume();
				break;
			}
		}
	} //}}}

	//}}}
}
