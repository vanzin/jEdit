/*
 * HyperSearchRequest.java - HyperSearch request, run in I/O thread
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

package org.gjt.sp.jedit.search;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.tree.*;
import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.*;

public class HyperSearchRequest extends WorkRequest
{
	public HyperSearchRequest(View view, SearchMatcher matcher,
		HyperSearchResults results)
	{
		this.view = view;
		this.matcher = matcher;

		this.results = results;
		this.resultTreeModel = results.getTreeModel();
		this.resultTreeRoot = (DefaultMutableTreeNode)resultTreeModel
			.getRoot();
	}

	public void run()
	{
		SearchFileSet fileset = SearchAndReplace.getSearchFileSet();
		setProgressMaximum(fileset.getBufferCount());
		setStatus(jEdit.getProperty("hypersearch.status"));

		int resultCount = 0;
		int bufferCount = 0;

		try
		{
			int current = 0;

			Buffer buffer = fileset.getFirstBuffer(view);

			if(buffer != null)
			{
				do
				{
					setProgressValue(++current);
					int thisResultCount = doHyperSearch(buffer,matcher);
					if(thisResultCount != 0)
					{
						bufferCount++;
						resultCount += thisResultCount;
					}
				}
				while((buffer = fileset.getNextBuffer(view,buffer)) != null);
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			VFSManager.error(view,"searcherror",args);
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			final int _resultCount = resultCount;
			final int _bufferCount = bufferCount;
			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					results.searchDone(_resultCount,_bufferCount);
				}
			});
		}
	}

	// private members
	private View view;
	private SearchMatcher matcher;
	private HyperSearchResults results;
	private DefaultTreeModel resultTreeModel;
	private DefaultMutableTreeNode resultTreeRoot;

	private int doHyperSearch(Buffer buffer, SearchMatcher matcher)
		throws Exception
	{
		setAbortable(false);

		int resultCount = 0;

		final DefaultMutableTreeNode bufferNode = new DefaultMutableTreeNode(
			buffer.getPath());

		try
		{
			buffer.readLock();

			Element map = buffer.getDefaultRootElement();
			Segment text = new Segment();
			int offset = 0;
			int length = buffer.getLength();
			int line = -1;

loop:			for(;;)
			{
				buffer.getText(offset,length - offset,text);
				int[] match = matcher.nextMatch(text);
				if(match == null)
					break loop;

				offset += match[1];

				int newLine = map.getElementIndex(offset);
				if(line == newLine)
				{
					// already had a result on this
					// line, skip
					continue loop;
				}

				line = newLine;

				resultCount++;

				bufferNode.insert(new DefaultMutableTreeNode(
					new HyperSearchResult(buffer,line),false),
					bufferNode.getChildCount());
			}
		}
		finally
		{
			buffer.readUnlock();
		}

		if(resultCount != 0)
		{
			resultTreeRoot.insert(bufferNode,resultTreeRoot.getChildCount());

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					resultTreeModel.reload(resultTreeRoot);
				}
			});
		}

		setAbortable(true);

		return resultCount;
	}
}
