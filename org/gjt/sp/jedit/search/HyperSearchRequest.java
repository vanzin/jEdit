/*
 * HyperSearchRequest.java - HyperSearch request, run in I/O thread
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.util.concurrent.Callable;
import javax.swing.tree.*;
import javax.swing.*;

import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.*;
//}}}

/**
 * HyperSearch results window.
 * @author Slava Pestov
 * @version $Id$
 */
class HyperSearchRequest extends Task
{
	//{{{ HyperSearchRequest constructor
	HyperSearchRequest(View view, SearchMatcher matcher,
		HyperSearchResults results, Selection[] selection)
	{
		this.view = view;
		this.matcher = matcher;

		this.results = results;
		searchString = SearchAndReplace.getSearchString();
		rootSearchNode = new DefaultMutableTreeNode(new HyperSearchOperationNode(searchString, matcher));

		this.selection = selection;
	} //}}}

	//{{{ run() method
	@Override
	public void _run()
	{
		setStatus(jEdit.getProperty("hypersearch-status"));

		SearchFileSet fileset = SearchAndReplace.getSearchFileSet();
		String[] files = fileset.getFiles(view);
		if(files == null || files.length == 0)
		{
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					GUIUtilities.error(view,"empty-fileset",null);
					results.searchDone(rootSearchNode);
				}
			});
			return;
		}

		setMaximum(fileset.getFileCount(view));

		// to minimize synchronization and stuff like that, we only
		// show a status message at most twice a second

		// initially zero, so that we always show the first message
		String searchingCaption = jEdit.getProperty("hypersearch-results.searching",
				new String[] { SearchAndReplace.getSearchString() }) + ' ';
		try
		{
			if(selection != null)
			{
				Buffer buffer = view.getBuffer();

				searchInSelection(buffer);
			}
			else
			{
				int current = 0;

				long lastStatusTime = 0L;
				int resultCount = 0;
				boolean asked = false;
				int maxResults = jEdit.getIntegerProperty("hypersearch.maxWarningResults");
				for(int i = 0; i < files.length; i++)
				{
					if(Thread.currentThread().isInterrupted())
					{
						Log.log(Log.MESSAGE, this, "Search stopped by user action (stop button)");
						break;
					}
					if (!asked && resultCount > maxResults && maxResults != 0)
					{
						Log.log(Log.DEBUG, this, "Search in progress, " + resultCount +
									 " occurrences found, asking the user to stop");
						asked = true;
						int ret = GUIUtilities.confirm(view, "hypersearch.tooManyResults",
									       new Object[]{resultCount},
									       JOptionPane.YES_NO_OPTION,
									       JOptionPane.QUESTION_MESSAGE);
						if (ret == JOptionPane.YES_OPTION)
						{
							Log.log(Log.MESSAGE, this, "Search stopped by user action");
							break;
						}
					}
					final String file = files[i];
					current++;

					long currentTime = System.currentTimeMillis();
					if(currentTime - lastStatusTime > 250L)
					{
						setValue(current);
						lastStatusTime = currentTime;
						results.setSearchStatus(searchingCaption + file);
					}

					Buffer buffer = ThreadUtilities.callInDispatchThread(
						new Callable<Buffer>()
						{
							@Override
							public Buffer call() {
								return jEdit.openTemporary(null,null,file,false);
							}
						});
					if(buffer != null)
					{
						// Wait for the buffer to load
						if(!buffer.isLoaded())
							TaskManager.instance.waitForIoTasks();

						resultCount += doHyperSearch(buffer, 0, buffer.getLength());
					}
				}
				Log.log(Log.MESSAGE, this, resultCount +" OCCURENCES");
			}
		}
		catch(final Exception e)
		{
			Log.log(Log.ERROR,this,e);
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					SearchAndReplace.handleError(view,e);
				}
			});
		}
		finally
		{
			ThreadUtilities.runInDispatchThread(new Runnable()
			{
				public void run()
				{
					results.searchDone(rootSearchNode, selectNode);
				}
			});
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final View view;
	private final SearchMatcher matcher;
	private final HyperSearchResults results;
	private final DefaultMutableTreeNode rootSearchNode;
	private final Selection[] selection;
	private final String searchString;
	private DefaultMutableTreeNode selectNode;
	//}}}

	//{{{ searchInSelection() method
	private int searchInSelection(Buffer buffer) throws Exception
	{
		setCancellable(false);

		int resultCount = 0;

		try
		{
			buffer.readLock();

			for (Selection s : selection)
			{
				if (s instanceof Selection.Rect)
				{
					for (int j = s.getStartLine(); j <= s.getEndLine(); j++)
					{
						resultCount += doHyperSearch(buffer, s.getStart(buffer, j),
									     s.getEnd(buffer, j));
					}
				}
				else
				{
					resultCount += doHyperSearch(buffer, s.getStart(), s.getEnd());
				}
			}
		}
		finally
		{
			buffer.readUnlock();
		}
		setCancellable(true);

		return resultCount;
	} //}}}

	//{{{ doHyperSearch() method
	private int doHyperSearch(Buffer buffer, int start, int end)
		throws Exception
	{
		if(matcher instanceof BoyerMooreSearchMatcher)
			setCancellable(true);
		else
			setCancellable(false);

		HyperSearchFileNode hyperSearchFileNode = new HyperSearchFileNode(buffer.getPath());
		DefaultMutableTreeNode bufferNode = new DefaultMutableTreeNode(hyperSearchFileNode);

		int resultCount = doHyperSearch(buffer,start,end,bufferNode);
		hyperSearchFileNode.setCount(resultCount);
		if(resultCount != 0)
			rootSearchNode.insert(bufferNode,rootSearchNode.getChildCount());

		setCancellable(true);

		return resultCount;
	} //}}}

	//{{{ doHyperSearch() method
	private int doHyperSearch(Buffer buffer, int start, int end,
		DefaultMutableTreeNode bufferNode)
	{
		if(matcher.wholeWord)
		{
			buffer.setMode();
			String noWordSep = buffer.getStringProperty("noWordSep");
			matcher.setNoWordSep(noWordSep);
		}
		int resultCount = 0;
		JEditTextArea textArea = jEdit.getActiveView().getTextArea();
		int caretLine = textArea.getBuffer() == buffer ? textArea.getCaretLine() : -1;
		try
		{
			buffer.readLock();

			boolean endOfLine = buffer.getLineEndOffset(
				buffer.getLineOfOffset(end)) - 1 == end;

			int offset = start;

			HyperSearchResult lastResult = null;
			for(int counter = 0; ; counter++)
			{
				boolean startOfLine = buffer.getLineStartOffset(
					buffer.getLineOfOffset(offset)) == offset;

				SearchMatcher.Match match = null;
				try {
					match = matcher.nextMatch(
						buffer.getSegment(offset, end - offset),
						startOfLine,endOfLine,counter == 0,
						false);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if(match == null)
					break;

				int newLine = buffer.getLineOfOffset(
					offset + match.start);
				if(lastResult == null || lastResult.line != newLine)
				{
					lastResult = new HyperSearchResult(
						buffer,newLine);
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(
						lastResult, false);
					if (lastResult.line == caretLine)
						selectNode = child;
					bufferNode.add(child);
				}

				lastResult.addOccur(offset + match.start,
					offset + match.end);

				offset += match.end;
				resultCount++;
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
