/*
 * HelpTOCPanel.java - Help table of contents
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.gjt.sp.util.ThreadUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.browser.FileCellRenderer; // for icons
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.XMLUtilities;

import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;
//}}}

public class HelpTOCPanel extends JPanel
{
	//{{{ HelpTOCPanel constructor
	public HelpTOCPanel(HelpViewerInterface helpViewer)
	{
		super(new BorderLayout());

		this.helpViewer = helpViewer;
		nodes = new HashMap<String,DefaultMutableTreeNode>();

		toc = new TOCTree();

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			toc.putClientProperty("JTree.lineStyle", "Angled");

		toc.setCellRenderer(new TOCCellRenderer());
		toc.setEditable(false);
		toc.setShowsRootHandles(true);

		add(BorderLayout.CENTER,new JScrollPane(toc));

		load();
	} //}}}

	//{{{ selectNode() method
	public void selectNode(String shortURL)
	{
		if(tocModel == null)
			return;

		final DefaultMutableTreeNode node =
			nodes.get(shortURL);

		if(node == null)
			return;

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				TreePath path = new TreePath(tocModel.getPathToRoot(node));
				toc.expandPath(path);
				toc.setSelectionPath(path);
				toc.scrollPathToVisible(path);
			}
		});
	} //}}}

	//{{{ load() method
	public void load()
	{
		DefaultTreeModel empty = new DefaultTreeModel(
			new DefaultMutableTreeNode(
			jEdit.getProperty("helpviewer.toc.loading")));
		toc.setModel(empty);
		toc.setRootVisible(true);

		ThreadUtilities.runInBackground(new Runnable()
		{
			public void run()
			{
				createTOC();
				tocModel.reload(tocRoot);
				toc.setModel(tocModel);
				toc.setRootVisible(false);
				for(int i = 0; i <tocRoot.getChildCount(); i++)
				{
					DefaultMutableTreeNode node =
						(DefaultMutableTreeNode)
						tocRoot.getChildAt(i);
					toc.expandPath(new TreePath(
						node.getPath()));
				}
				if(helpViewer.getShortURL() != null)
					selectNode(helpViewer.getShortURL());
			}
		});
	} //}}}

	//{{{ Private members
	private HelpViewerInterface helpViewer;
	private DefaultTreeModel tocModel;
	private DefaultMutableTreeNode tocRoot;
	private JTree toc;
	private Map<String, DefaultMutableTreeNode> nodes;

	//{{{ createNode() method
	private DefaultMutableTreeNode createNode(String href, String title)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
			new HelpNode(href,title),true);
		nodes.put(href,node);
		return node;
	} //}}}

	//{{{ createTOC() method
	private void createTOC()
	{
		EditPlugin[] plugins = jEdit.getPlugins();
		Arrays.sort(plugins,new PluginCompare());
		tocRoot = new DefaultMutableTreeNode();

		tocRoot.add(createNode("welcome.html",
			jEdit.getProperty("helpviewer.toc.welcome")));

		tocRoot.add(createNode("README.txt",
			jEdit.getProperty("helpviewer.toc.readme")));
		tocRoot.add(createNode("CHANGES.txt",
			jEdit.getProperty("helpviewer.toc.changes")));
		tocRoot.add(createNode("TODO.txt",
			jEdit.getProperty("helpviewer.toc.todo")));
		tocRoot.add(createNode("COPYING.txt",
			jEdit.getProperty("helpviewer.toc.copying")));
		tocRoot.add(createNode("COPYING.DOC.txt",
			jEdit.getProperty("helpviewer.toc.copying-doc")));
		tocRoot.add(createNode("Apache.LICENSE.txt",
			jEdit.getProperty("helpviewer.toc.copying-apache")));
		tocRoot.add(createNode("COPYING.PLUGINS.txt",
			jEdit.getProperty("helpviewer.toc.copying-plugins")));

		loadTOC(tocRoot,"news45/toc.xml");
		loadTOC(tocRoot,"users-guide/toc.xml");
		loadTOC(tocRoot,"FAQ/toc.xml");


		DefaultMutableTreeNode pluginTree = new DefaultMutableTreeNode(
			jEdit.getProperty("helpviewer.toc.plugins"),true);

		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];

			String name = plugin.getClassName();

			String docs = jEdit.getProperty("plugin." + name + ".docs");
			String label = jEdit.getProperty("plugin." + name + ".name");
			if(label != null && docs != null)
			{
				String path = plugin.getPluginJAR()
					.getClassLoader()
					.getResourceAsPath(docs);
				pluginTree.add(createNode(
					path,label));
			}
		}

		if(pluginTree.getChildCount() != 0)
			tocRoot.add(pluginTree);
		else
		{
			// so that HelpViewer constructor doesn't try to expand
			pluginTree = null;
		}
		loadTOC(tocRoot,"api/toc.xml");
		tocModel = new DefaultTreeModel(tocRoot);
	} //}}}

	//{{{ loadTOC() method
	private void loadTOC(DefaultMutableTreeNode root, String path)
	{
		TOCHandler h = new TOCHandler(root,MiscUtilities.getParentOfPath(path));
		try
		{
			XMLUtilities.parseXML(
				new URL(helpViewer.getBaseURL()
					+ '/' + path).openStream(), h);
		}
		catch(FileNotFoundException e)
		{
			/* it is acceptable only for the API TOC :
			   the user can choose not to install them
			 */
			if("api/toc.xml".equals(path))
			{
				Log.log(Log.NOTICE,this,
					"The API docs for jEdit will not be available (reinstall jEdit if you want them)");
				root.add(
					createNode("http://www.jedit.org/api/overview-summary.html",
						jEdit.getProperty("helpviewer.toc.online-apidocs")));
			}
			else
			{
				Log.log(Log.ERROR,this,e);
			}
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,this,e);
		}
	} //}}}

	//}}}

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
	class TOCHandler extends DefaultHandler
	{
		String dir;

		//{{{ TOCHandler constructor
		TOCHandler(DefaultMutableTreeNode root, String dir)
		{
			nodes = new Stack<DefaultMutableTreeNode>();
			node = root;
			this.dir = dir;
		} //}}}

		//{{{ characters() method
		public void characters(char[] c, int off, int len)
		{
			if(tag.equals("TITLE"))
			{
				boolean firstNonWhitespace = false;
				for(int i = 0; i < len; i++)
				{
					char ch = c[off + i];
					if (!firstNonWhitespace && Character.isWhitespace(ch)) continue;
					firstNonWhitespace = true;
					title.append(ch);
				}
			}


		} //}}}

		//{{{ startElement() method
		public void startElement(String uri, String localName,
					 String name, Attributes attrs)
		{
			tag = name;
			if (name.equals("ENTRY"))
				href = attrs.getValue("HREF");
		} //}}}

		//{{{ endElement() method
		public void endElement(String uri, String localName, String name)
		{
			if(name == null)
				return;

			if(name.equals("TITLE"))
			{
				DefaultMutableTreeNode newNode = createNode(
					dir + href,title.toString());
				node.add(newNode);
				nodes.push(node);
				node = newNode;
				title.setLength(0);
			}
			else if(name.equals("ENTRY"))
			{
				node = nodes.pop();
				href = null;
			}
		} //}}}

		//{{{ Private members
		private String tag;
		private StringBuilder title = new StringBuilder();
		private String href;
		private DefaultMutableTreeNode node;
		private Stack<DefaultMutableTreeNode> nodes;
		//}}}
	} //}}}

	//{{{ TOCTree class
	class TOCTree extends JTree
	{
		//{{{ TOCTree constructor
		TOCTree()
		{
			ToolTipManager.sharedInstance().registerComponent(this);
			selectionModel.setSelectionMode(SINGLE_TREE_SELECTION);
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
		/* public final Point getToolTipLocation(MouseEvent evt)
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
		} */ //}}}

		//{{{ processKeyEvent() method
		public void processKeyEvent(KeyEvent evt)
		{
			if ((KeyEvent.KEY_PRESSED == evt.getID()) &&
			    (KeyEvent.VK_ENTER == evt.getKeyCode()))
			{
				TreePath path = getSelectionPath();
				if(path != null)
				{
					Object obj = ((DefaultMutableTreeNode)
						path.getLastPathComponent())
						.getUserObject();
					if(!(obj instanceof HelpNode))
					{
						this.expandPath(path);
						return;
					}

					HelpNode node = (HelpNode)obj;
					helpViewer.gotoURL(node.href,true,0);
				}
				evt.consume();
			}
			else
			{
				super.processKeyEvent(evt);
			}
		} //}}}

		//{{{ processMouseEvent() method
		protected void processMouseEvent(MouseEvent evt)
		{
			//ToolTipManager ttm = ToolTipManager.sharedInstance();

			switch(evt.getID())
			{
			/* case MouseEvent.MOUSE_ENTERED:
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
				break; */
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
						this.expandPath(path);
						return;
					}

					HelpNode node = (HelpNode)obj;

					helpViewer.gotoURL(node.href,true,0);
				}

				super.processMouseEvent(evt);
				break;
			default:
				super.processMouseEvent(evt);
				break;
			}
		} //}}}

		//{{{ cellRectIsVisible() method
		private boolean cellRectIsVisible(Rectangle cellRect)
		{
			Rectangle vr = TOCTree.this.getVisibleRect();
			return vr.contains(cellRect.x,cellRect.y) &&
				vr.contains(cellRect.x + cellRect.width,
				cellRect.y + cellRect.height);
		} //}}}
	} //}}}

	//{{{ TOCCellRenderer class
	static class TOCCellRenderer extends DefaultTreeCellRenderer
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

	//{{{ PluginCompare class
	static class PluginCompare implements Comparator<EditPlugin>
	{
		public int compare(EditPlugin p1, EditPlugin p2)
		{
			return StandardUtilities.compareStrings(
				jEdit.getProperty("plugin." + p1.getClassName() + ".name"),
				jEdit.getProperty("plugin." + p2.getClassName() + ".name"),
				true);
		}
	} //}}}
}