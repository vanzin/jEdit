/*
 * HelpViewer.java - HTML Help viewer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import com.microstar.xml.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import javax.swing.text.Document;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.browser.FileCellRenderer; // for icons
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's HTML viewer. It uses a Swing JEditorPane to display the HTML,
 * and implements a URL history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HelpViewer extends JFrame implements EBComponent
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

		history = new String[25];
		nodes = new Hashtable();

		ActionHandler actionListener = new ActionHandler();

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.putClientProperty("JToolBar.isRollover",Boolean.TRUE);

		JLabel label = new JLabel(jEdit.getProperty("helpviewer.url"));
		label.setBorder(new EmptyBorder(0,12,0,12));
		toolBar.add(label);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		urlField = new JTextField();
		urlField.addKeyListener(new KeyHandler());
		Dimension dim = urlField.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		urlField.setMaximumSize(dim);
		box.add(urlField);
		box.add(Box.createGlue());
		toolBar.add(box);

		toolBar.add(Box.createHorizontalStrut(6));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(0,12,0,0));
		back = new JButton(GUIUtilities.loadIcon("Back24.gif"));
		back.setToolTipText(jEdit.getProperty("helpviewer.back"));
		back.addActionListener(actionListener);
		back.setRequestFocusEnabled(false);
		toolBar.add(back);
		forward = new JButton(GUIUtilities.loadIcon("Forward24.gif"));
		forward.addActionListener(actionListener);
		forward.setToolTipText(jEdit.getProperty("helpviewer.forward"));
		forward.setRequestFocusEnabled(false);
		toolBar.add(forward);
		back.setPreferredSize(forward.getPreferredSize());

		getContentPane().add(BorderLayout.NORTH,toolBar);

		createTOC();

		toc = new TOCTree(tocModel);

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			toc.putClientProperty("JTree.lineStyle", "Angled");

		toc.setCellRenderer(new TOCCellRenderer());
		toc.setEditable(false);
		toc.setRootVisible(false);
		toc.setShowsRootHandles(true);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
			nodes.get("users-guide/using-jedit-part.html");
		if(node != null)
			toc.expandPath(new TreePath(node.getPath()));

		viewer = new JEditorPane();
		viewer.setEditable(false);
		viewer.addHyperlinkListener(new LinkHandler());
		viewer.setFont(new Font("Monospaced",Font.PLAIN,12));

		final JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(toc),new JScrollPane(viewer));
		splitter.setBorder(null);

		getContentPane().add(BorderLayout.CENTER,splitter);

		gotoURL(url,true);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		getRootPane().setPreferredSize(new Dimension(800,500));

		pack();
		GUIUtilities.loadGeometry(this,"helpviewer");

		EditBus.addToBus(this);

		show();

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				splitter.setDividerLocation(250);
			}
		});
	} //}}}

	//{{{ gotoURL() method
	/**
	 * Displays the specified URL in the HTML component.
	 * @param url The URL
	 * @param addToHistory Should the URL be added to the back/forward
	 * history?
	 */
	public void gotoURL(String url, boolean addToHistory)
	{
		String shortURL;
		if(MiscUtilities.isURL(url))
		{
			if(url.startsWith(baseURL))
			{
				shortURL = url.substring(baseURL.length());
				if(shortURL.startsWith("/"))
					shortURL = shortURL.substring(1);
			}
			else
			{
				shortURL = null;
			}
		}
		else
		{
			shortURL = url;
			if(baseURL.endsWith("/"))
				url = baseURL + url;
			else
				url = baseURL + '/' + url;
		}

		// reset default cursor so that the hand cursor doesn't
		// stick around
		viewer.setCursor(Cursor.getDefaultCursor());

		int index = url.indexOf('#');

		URL _url = null;
		try
		{
			_url = new URL(url);

			urlField.setText(_url.toString());
			viewer.setPage(_url);
			if(addToHistory)
			{
				history[historyPos] = url;
				if(historyPos + 1 == history.length)
				{
					System.arraycopy(history,1,history,
						0,history.length - 1);
					history[historyPos] = null;
				}
				else
					historyPos++;
			}
		}
		catch(MalformedURLException mf)
		{
			Log.log(Log.ERROR,this,mf);
			String[] args = { url, mf.getMessage() };
			GUIUtilities.error(this,"badurl",args);
			return;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			String[] args = { url, io.toString() };
			GUIUtilities.error(this,"read-error",args);
			return;
		}

		// select the appropriate tree node.
		if(shortURL != null)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodes.get(shortURL);

			if(node == null)
				return;

			TreePath path = new TreePath(tocModel.getPathToRoot(node));
			toc.expandPath(path);
			toc.setSelectionPath(path);
			toc.scrollPathToVisible(path);
		}
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		EditBus.removeFromBus(this);
		GUIUtilities.saveGeometry(this,"helpviewer");
		super.dispose();
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			SwingUtilities.updateComponentTreeUI(getRootPane());
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private String baseURL;
	private JButton back;
	private JButton forward;
	private DefaultTreeModel tocModel;
	private JTree toc;
	// this makes gotoURL()'s tree updating simpler
	private Hashtable nodes;
	private JEditorPane viewer;
	private JTextField urlField;
	private String[] history;
	private int historyPos;
	//}}}

	//{{{ createTOC() method
	private void createTOC()
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();

		root.add(createNode("welcome.html",
			jEdit.getProperty("helpviewer.toc.welcome")));

		root.add(createNode("README.txt",
			jEdit.getProperty("helpviewer.toc.readme")));
		root.add(createNode("NEWS.txt",
			jEdit.getProperty("helpviewer.toc.news")));
		root.add(createNode("TODO.txt",
			jEdit.getProperty("helpviewer.toc.todo")));
		root.add(createNode("CHANGES.txt",
			jEdit.getProperty("helpviewer.toc.changes")));
		root.add(createNode("COPYING.txt",
			jEdit.getProperty("helpviewer.toc.copying")));
		root.add(createNode("COPYING.DOC.txt",
			jEdit.getProperty("helpviewer.toc.copying-doc")));

		loadTOC(root,"users-guide/toc.xml");
		loadTOC(root,"FAQ/toc.xml");

		DefaultMutableTreeNode pluginDocs = new DefaultMutableTreeNode(
			jEdit.getProperty("helpviewer.toc.plugins"),true);

		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			EditPlugin.JAR jar = plugin.getJAR();
			if(jar == null)
				continue;

			String name = plugin.getClassName();

			String docs = jEdit.getProperty("plugin." + name + ".docs");
			String label = jEdit.getProperty("plugin." + name + ".name");
			if(docs != null)
			{
				if(label != null && docs != null)
				{
					URL url = jar.getClassLoader()
						.getResource(docs);
					if(url != null)
					{
						pluginDocs.add(createNode(
							url.toString(),label));
					}
				}
			}
		}

		if(pluginDocs.getChildCount() != 0)
			root.add(pluginDocs);

		tocModel = new DefaultTreeModel(root);
	} //}}}

	//{{{ loadTOC() method
	private void loadTOC(DefaultMutableTreeNode root, String path)
	{
		TOCHandler h = new TOCHandler(root,MiscUtilities.getParentOfPath(path));
		XmlParser parser = new XmlParser();
		parser.setHandler(h);

		try
		{
			parser.parse(null, null, new InputStreamReader(
				new URL(baseURL + '/' + path).openStream()));
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,this,path + ':' + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.NOTICE,this,e);
		}
	} //}}}

	//{{{ createNode() method
	private DefaultMutableTreeNode createNode(String href, String title)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
			new HelpNode(href,title),true);
		nodes.put(href,node);
		return node;
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ HelpNode class
	static class HelpNode
	{
		String href, title;

		//{{{ HelpNode constructor
		HelpNode(String href, String title)
		{
			this.href = href;
			this.title = title;
		} //}}}

		//{{{ toString() method
		public String toString()
		{
			return title;
		} //}}}
	} //}}}

	//{{{ TOCHandler class
	class TOCHandler extends HandlerBase
	{
		String dir;

		//{{{ TOCHandler constructor
		TOCHandler(DefaultMutableTreeNode root, String dir)
		{
			nodes = new Stack();
			node = root;
			this.dir = dir;
		} //}}}

		//{{{ attribute() method
		public void attribute(String aname, String value, boolean isSpecified)
		{
			if(aname.equals("HREF"))
				href = value;
		} //}}}

		//{{{ charData() method
		public void charData(char[] c, int off, int len)
		{
			if(tag.equals("TITLE"))
			{
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < len; i++)
				{
					char ch = c[off + i];
					if(ch == ' ' || !Character.isWhitespace(ch))
						buf.append(ch);
				}
				title = buf.toString();
			}
		} //}}}

		//{{{ startElement() method
		public void startElement(String name)
		{
			tag = name;
		} //}}}

		//{{{ endElement() method
		public void endElement(String name)
		{
			if(name == null)
				return;

			if(name.equals("TITLE"))
			{
				DefaultMutableTreeNode newNode = createNode(
					dir + href,title);
				node.add(newNode);
				nodes.push(node);
				node = newNode;
			}
			else if(name.equals("ENTRY"))
				node = (DefaultMutableTreeNode)nodes.pop();
		} //}}}

		//{{{ Private members
		private String tag;
		private String title;
		private String href;
		private DefaultMutableTreeNode node;
		private Stack nodes;
		//}}}
	} //}}}

	//{{{ TOCTree class
	class TOCTree extends JTree
	{
		//{{{ TOCTree constructor
		TOCTree(TreeModel model)
		{
			super(model);
			ToolTipManager.sharedInstance().registerComponent(this);
		} //}}}

		//{{{ getToolTipText() method
		public final String getToolTipText(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
					return path.getLastPathComponent().toString();
			}
			return null;
		} //}}}

		//{{{ getToolTipLocation() method
		public final Point getToolTipLocation(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
				{
					return new Point(cellRect.x + 14, cellRect.y);
				}
			}
			return null;
		} //}}}

		//{{{ processMouseEvent() method
		protected void processMouseEvent(MouseEvent evt)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();

			switch(evt.getID())
			{
			case MouseEvent.MOUSE_ENTERED:
				toolTipInitialDelay = ttm.getInitialDelay();
				toolTipReshowDelay = ttm.getReshowDelay();
				ttm.setInitialDelay(200);
				ttm.setReshowDelay(0);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_EXITED:
				ttm.setInitialDelay(toolTipInitialDelay);
				ttm.setReshowDelay(toolTipReshowDelay);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_CLICKED:
				TreePath path = getPathForLocation(evt.getX(),evt.getY());
				if(path != null)
				{
					if(!isPathSelected(path))
						setSelectionPath(path);

					Object obj = ((DefaultMutableTreeNode)
						path.getLastPathComponent())
						.getUserObject();
					if(!(obj instanceof HelpNode))
					{
						toc.expandPath(path);
						return;
					}

					HelpNode node = (HelpNode)obj;

					gotoURL(node.href,true);
				}

				super.processMouseEvent(evt);
				break;
			default:
				super.processMouseEvent(evt);
				break;
			}
		} //}}}

		//{{{ Private members
		private int toolTipInitialDelay = -1;
		private int toolTipReshowDelay = -1;

		//{{{ cellRectIsVisible() method
		private boolean cellRectIsVisible(Rectangle cellRect)
		{
			Rectangle vr = TOCTree.this.getVisibleRect();
			return vr.contains(cellRect.x,cellRect.y) &&
				vr.contains(cellRect.x + cellRect.width,
				cellRect.y + cellRect.height);
		} //}}}

		//}}}
	} //}}}

	//{{{ TOCCellRenderer class
	class TOCCellRenderer extends DefaultTreeCellRenderer
	{
		EmptyBorder border = new EmptyBorder(1,0,1,1);

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean focus)
		{
			super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,focus);
			setIcon(leaf ? FileCellRenderer.fileIcon
				: (expanded ? FileCellRenderer.openDirIcon
				: FileCellRenderer.dirIcon));
			setBorder(border);

			return this;
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() class
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == back)
			{
				if(historyPos <= 1)
					getToolkit().beep();
				else
				{
					String url = history[--historyPos - 1];
					gotoURL(url,false);
				}
			}
			else if(source == forward)
			{
				if(history.length - historyPos <= 1)
					getToolkit().beep();
				else
				{
					String url = history[historyPos];
					if(url == null)
						getToolkit().beep();
					else
					{
						historyPos++;
						gotoURL(url,false);
					}
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
				}
				else
				{
					URL url = evt.getURL();
					if(url != null)
						gotoURL(url.toString(),true);
				}
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				viewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				viewer.setCursor(Cursor.getDefaultCursor());
			}
		} //}}}
	} //}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		//{{{ keyPressed() method
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				gotoURL(urlField.getText(),true);
			}
		} //}}}
	} //}}}

	//}}}
}
