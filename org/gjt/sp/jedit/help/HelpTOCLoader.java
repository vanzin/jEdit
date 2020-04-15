/*
 * HelpTOCLoader.java - Help table of contents loader
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
 * Copyright (C) 2016 Eric Le Lay
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
//}}}

/**
 * Help table-of-contents loader.
 *
 * <p>Code for loading and constructing the table of contents (TOC).
 * Doesn't refresh when plugins are (un)loaded: you'll have to call
 * {@link #createTOC()} again yourself.</p>
 *
 * <p>Don't keep {@link HelpTOCLoader} instances between loads:
 * use it and forget it.</p>
 **/
public class HelpTOCLoader {

	//{{{ HelpTOCLoader constructor
	public HelpTOCLoader(Map<String, DefaultMutableTreeNode> nodes, String baseURL) {
		this.nodes = nodes;
		this.baseURL = baseURL;
	} //}}}

	//{{{ HelpNode class
	/**
	 * a TOC item: href and title.
	 */
	public static class HelpNode
	{
		public final String href, title;

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

	//{{{ createTOC() method
	/**
	 * Load the table of contents.
	 * Performs synchronous IO, so you don't want to call it from the GUI thread.
	 * @return the TOC tree model as a {@link DefaultMutableTreeNode}.
	 *         User objects are {@link HelpNode} instances.
	 */
	public DefaultMutableTreeNode createTOC()
	{
		EditPlugin[] plugins = jEdit.getPlugins();
		Arrays.sort(plugins,new PluginCompare());
		DefaultMutableTreeNode tocRoot = new DefaultMutableTreeNode();

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

		loadTOC(tocRoot,"whatsnew/toc.xml");
		loadTOC(tocRoot,"users-guide/toc.xml");
		loadTOC(tocRoot,"FAQ/toc.xml");


		DefaultMutableTreeNode pluginTree = new DefaultMutableTreeNode(
			jEdit.getProperty("helpviewer.toc.plugins"),true);

		for (EditPlugin plugin : plugins)
		{
			String name = plugin.getClassName();

			String docs = jEdit.getProperty("plugin." + name + ".docs");
			String label = jEdit.getProperty("plugin." + name + ".name");
			if (label != null && docs != null)
			{
				String path = plugin.getPluginJAR().getClassLoader().getResourceAsPath(docs);
				pluginTree.add(createNode(path, label));
			}
		}

		if(pluginTree.getChildCount() != 0)
		{
			tocRoot.add(pluginTree);
		}

		loadTOC(tocRoot,"api/toc.xml");
		return tocRoot;
	} //}}}

	//{{{ createNode() method
	private DefaultMutableTreeNode createNode(String href, String title)
	{
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
			new HelpTOCLoader.HelpNode(href,title),true);
		if(nodes!=null)
		{
			nodes.put(href,node);
		}
		return node;
	} //}}}

	//{{{ loadTOC() method
	private void loadTOC(DefaultMutableTreeNode root, String path)
	{
		TOCHandler h = new TOCHandler(root,MiscUtilities.getParentOfPath(path));
		try
		{
			XMLUtilities.parseXML(
				new URL(baseURL + '/' + path).openStream(), h);
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

	//{{{ Private members
	private final Map<String, DefaultMutableTreeNode> nodes;
	private final String baseURL;
	//}}}

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
		@Override
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
		@Override
		public void startElement(String uri, String localName,
					 String name, Attributes attrs)
		{
			tag = name;
			if (name.equals("ENTRY"))
				href = attrs.getValue("HREF");
		} //}}}

		//{{{ endElement() method
		@Override
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
		private final StringBuilder title = new StringBuilder();
		private String href;
		private DefaultMutableTreeNode node;
		private final Stack<DefaultMutableTreeNode> nodes;
		//}}}
	} //}}}

	//{{{ PluginCompare class
	static class PluginCompare implements Comparator<EditPlugin>
	{
		@Override
		public int compare(EditPlugin p1, EditPlugin p2)
		{
			return StandardUtilities.compareStrings(
				jEdit.getProperty("plugin." + p1.getClassName() + ".name"),
				jEdit.getProperty("plugin." + p2.getClassName() + ".name"),
				true);
		}
	} //}}}
}
