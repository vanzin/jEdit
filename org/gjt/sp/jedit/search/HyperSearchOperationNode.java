/*
 * HyperSearchOperationNode.java - Top result node of a HyperSearch request
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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

package org.gjt.sp.jedit.search;

//{{{ Imports
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearchOperationNode
{
	private boolean treeViewDisplayed;
	private final String searchString;
	private List<DefaultMutableTreeNode> resultNodes;
	private SearchMatcher searchMatcher;
	private String noWordSep;
	
	//{{{ HyperSearchOperationNode constructor
	public HyperSearchOperationNode(String searchString, SearchMatcher searchMatcher)
	{
		this.searchString = searchString;
		this.searchMatcher = searchMatcher;
		noWordSep = searchMatcher.getNoWordSep();
	}//}}}
	
	//{{{ toString() method
	public String toString() 
	{
		return searchString;
	}//}}}
	
	//{{{ isTreeViewDisplayed() method
	public boolean isTreeViewDisplayed() 
	{
		return treeViewDisplayed;
	}//}}}
	
	//{{{ setTreeViewDisplayed() method
	public void setTreeViewDisplayed(boolean treeViewDisplayed) 
	{
		this.treeViewDisplayed = treeViewDisplayed;
	}//}}}
	
	//{{{ restoreFlatNodes() method
	public void restoreFlatNodes(JTree resultTree, DefaultMutableTreeNode operNode)
	{
		for (int i = 0; i < resultNodes.size(); i++)
		{
			DefaultMutableTreeNode element = resultNodes.get(i);
			if (element.getUserObject() instanceof HyperSearchFileNode)
				((HyperSearchFileNode)element.getUserObject()).showFullPath = true;

			operNode.insert(element, operNode.getChildCount());
		}

		((DefaultTreeModel)resultTree.getModel()).nodeStructureChanged(operNode);
		
		for (Enumeration e = operNode.children(); e.hasMoreElements();)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			resultTree.expandPath(new TreePath(node.getPath()));
		}
		resultTree.scrollPathToVisible(
			new TreePath(operNode.getPath()));
	}//}}}
	
	//{{{ cacheFlatNodes() method
	public void cacheResultNodes(DefaultMutableTreeNode operNode) 
	{
		resultNodes = new ArrayList<DefaultMutableTreeNode>(operNode.getChildCount());
		for (Enumeration e = operNode.children(); e.hasMoreElements();)
			resultNodes.add((DefaultMutableTreeNode) e.nextElement());
	}//}}}
	
	//{{{ removeNodeFromCache() method
	public static void removeNodeFromCache(MutableTreeNode mnode)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)mnode;
		if (node.getUserObject() instanceof HyperSearchOperationNode)
			return;
		
		DefaultMutableTreeNode tmpNode = node;
		while ((tmpNode = (DefaultMutableTreeNode) tmpNode.getParent()) != null)
		{
			if (!(tmpNode.getUserObject() instanceof HyperSearchOperationNode))
				continue;
			HyperSearchOperationNode operNode = (HyperSearchOperationNode) tmpNode.getUserObject();
			if (operNode.resultNodes != null)
			{
				// the nodes aren't cached so no need to remove the node from cache
				operNode.resultNodes.remove(node);
			}
			break;
		}
		
	}//}}}
	
	//{{{ insertTreeNodes() method
	public void insertTreeNodes(JTree resultTree, DefaultMutableTreeNode operNode)
	{
		String fileSep = System.getProperty("file.separator");
		String fileSepRegex = System.getProperty("file.separator");
		if (fileSep.equals("\\"))
			fileSepRegex = "\\\\";
		
		//find the highest level common path
		String[] topPathTmp = null;
		int topPathNdx = -1;

		for (int i = 0;i < resultNodes.size();i++)
		{
			DefaultMutableTreeNode fileTreeNode = resultNodes.get(i);
			Object obj = fileTreeNode.getUserObject();
			if (!(obj instanceof HyperSearchFileNode))
				continue;
			HyperSearchFileNode fileNode = (HyperSearchFileNode)obj;

			int pos = fileNode.path.lastIndexOf(fileSep);
			String pathName = fileNode.path.substring(0, pos);
			String[] paths = pathName.split(fileSepRegex);
			if (topPathNdx == -1)
			{
				topPathNdx = paths.length;
				topPathTmp = paths;
			}
			else if (paths.length < topPathNdx)
			{
				topPathNdx = paths.length;
				topPathTmp = paths;				
			}
			else
			{
				for (int ndx =0 ; ndx < topPathNdx; ndx++)
				{
					if (!paths[ndx].equals(topPathTmp[ndx]))
					{
						topPathNdx = ndx;
						break;
					}
				}
			}
		}
		String[] topPath = new String[topPathNdx];
		String topPathPath = "";
		for (int ndx = 0 ; ndx < topPathNdx; ndx++)
		{
			topPath[ndx] = topPathTmp[ndx];
			topPathPath = topPathPath.concat(topPath[ndx] + fileSep);
		}
		Map<String, DefaultMutableTreeNode> treeNodes = new HashMap<String, DefaultMutableTreeNode>();
		HyperSearchFolderNode folderNode = 
			new HyperSearchFolderNode(new File(topPathPath), true);
		DefaultMutableTreeNode folderTreeNode = new DefaultMutableTreeNode(folderNode);
		operNode.insert(folderTreeNode, operNode.getChildCount());
		treeNodes.put(topPathPath, folderTreeNode);
		
		for (int i = 0;i < resultNodes.size();i++)
		{
			DefaultMutableTreeNode fileTreeNode = resultNodes.get(i);
			Object obj = fileTreeNode.getUserObject();
			if (!(obj instanceof HyperSearchFileNode))
				continue;
			HyperSearchFileNode fileNode = (HyperSearchFileNode)obj;

			fileNode.showFullPath = false;
			int pos = fileNode.path.lastIndexOf(fileSep);
			String pathName = fileNode.path.substring(0, pos);
			String[] paths = pathName.split(fileSepRegex);
			
			DefaultMutableTreeNode insNode = folderTreeNode;
			String partialPath = topPathPath;
			for (int ndx = topPathNdx; ndx < paths.length; ndx++)
			{
				partialPath = partialPath.concat(paths[ndx] + fileSep);
				DefaultMutableTreeNode tmpNode = treeNodes.get(partialPath);
				if (tmpNode == null)
				{
					HyperSearchFolderNode tmpFolderNode = 
						new HyperSearchFolderNode(new File(partialPath), false);
					tmpNode = new DefaultMutableTreeNode(tmpFolderNode);
					insNode.insert(tmpNode, insNode.getChildCount());
					treeNodes.put(partialPath, tmpNode);
				}
				insNode = tmpNode;
			}
			insNode.insert(fileTreeNode, insNode.getChildCount());
			treeNodes.put(fileNode.path, insNode);
		}
		
	}//}}}

	//{{{ getSearchMatcher() method
	public SearchMatcher getSearchMatcher()
	{
		// The searchMatcher has to remember the noWordSep property that was used
		// because in case of HyperSearchOperationNode, the same SearchMatcher
		// is used for several Buffers that can be of different edit modes.
		searchMatcher.setNoWordSep(noWordSep);
		return searchMatcher;
	}//}}}

	//{{{ getSearchString() method
	public String getSearchString()
	{
		return searchString;
	}//}}}

}
