/*
 * HyperSearchRequest.java - HyperSearch request, run in I/O thread
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

//{{{ Importa
import javax.swing.text.Segment;
import javax.swing.tree.*;
import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.*;
//}}}

public class HyperSearchRequest extends WorkRequest
{
	//{{{ HyperSearchRequest constructor
	public HyperSearchRequest(View view, SearchMatcher matcher,
		HyperSearchResults results, Selection[] selection)
	{
		this.view = view;
		this.matcher = matcher;

		this.results = results;
		this.resultTreeModel = results.getTreeModel();
		this.resultTreeRoot = (DefaultMutableTreeNode)resultTreeModel
			.getRoot();

		this.selection = selection;
	} //}}}

	//{{{ run() method
	public void run()
	{
		SearchFileSet fileset = SearchAndReplace.getSearchFileSet();
		setProgressMaximum(fileset.getBufferCount());
		setStatus(jEdit.getProperty("hypersearch.status"));

		int resultCount = 0;
		int bufferCount = 0;

		try
		{
			Buffer buffer = fileset.getFirstBuffer(view);

			if(selection != null)
				searchInSelection(buffer);
			else
			{
				int current = 0;

				if(buffer != null)
				{
					do
					{
						setProgressValue(++current);
						int thisResultCount = doHyperSearch(buffer,
							0,buffer.getLength());
						if(thisResultCount != 0)
						{
							bufferCount++;
							resultCount += thisResultCount;
						}
					}
					while((buffer = fileset.getNextBuffer(view,buffer)) != null);
				}
			}
		}
		catch(final Exception e)
		{
			Log.log(Log.ERROR,this,e);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					GUIUtilities.error(view,"searcherror",
						new String[] { e.toString() });
				}
			});
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
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private View view;
	private SearchMatcher matcher;
	private HyperSearchResults results;
	private DefaultTreeModel resultTreeModel;
	private DefaultMutableTreeNode resultTreeRoot;
	private Selection[] selection;
	//}}}

	//{{{ searchInSelection() method
	private int searchInSelection(Buffer buffer) throws Exception
	{
		setAbortable(false);

		final DefaultMutableTreeNode bufferNode = new DefaultMutableTreeNode(
			buffer.getPath());

		int resultCount = 0;

		for(int i = 0; i < selection.length; i++)
		{
			Selection s = selection[i];
			resultCount += doHyperSearch(buffer,s.getStart(),s.getEnd());
		}

		setAbortable(true);

		return resultCount;
	} //}}}

	//{{{ doHyperSearch() method
	private int doHyperSearch(Buffer buffer, int start, int end)
		throws Exception
	{
		setAbortable(false);

		final DefaultMutableTreeNode bufferNode = new DefaultMutableTreeNode(
			buffer.getPath());

		int resultCount = doHyperSearch(buffer,start,end,bufferNode);

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
	} //}}}

	//{{{ doHyperSearch() method
	private int doHyperSearch(Buffer buffer, int start, int end,
		DefaultMutableTreeNode bufferNode)
	{
		int resultCount = 0;

		try
		{
			buffer.readLock();

			Segment text = new Segment();
			int offset = start;
			int length = end;
			int line = -1;

loop:			for(;;)
			{
				buffer.getText(offset,length - offset,text);
				int[] match = matcher.nextMatch(text,offset == 0,
					length == buffer.getLength());
				if(match == null)
					break loop;

				int matchStart = offset + match[0];
				int matchEnd = offset + match[1];

				offset += match[1];
				if(match[0] - match[1] == 0)
					offset++;

				int newLine = buffer.getLineOfOffset(offset);
				if(line == newLine)
				{
					// already had a result on this
					// line, skip
					continue loop;
				}

				line = newLine;

				resultCount++;

				bufferNode.add(new DefaultMutableTreeNode(
					new HyperSearchResult(buffer,line,
					matchStart,matchEnd),false));
			}
		}
		finally
		{
			buffer.readUnlock();
		}

		return resultCount;
	} //}}}

	//}}}
}
