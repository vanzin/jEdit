/*
 * HelpHistoryModel.java - History Model for Help GUI
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Nicholas O'Leary
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

import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * History model used by the help browser 
 * @author Nicholas O'Leary
 * @version $Id$
 */
public class HelpHistoryModel
{
	//{{{ HelpHistoryModel constructor
	public HelpHistoryModel(int size)
	{
		history = new HistoryEntry[size];
		listeners = new ArrayList<HelpHistoryModelListener>();
	} //}}}

	//{{{ forward() method
	HistoryEntry forward(HelpViewer helpViewer)
	{
		if(history.length - historyPos <= 1)
		{
			return null;
		}
		if (history[historyPos] == null)
		{
			return null;
		}
		setCurrentScrollPosition(helpViewer.getCurrentPage(),helpViewer.getCurrentScrollPosition());
		HistoryEntry result = new HistoryEntry(history[historyPos]);
		historyPos++;
		fireUpdate();
		return result;
	} //}}}

	//{{{ hasNext() method
	public boolean hasNext()
	{
		return !((history.length - historyPos <= 1) ||
			 (history[historyPos] == null));
	} //}}}

	//{{{ back() method
	HistoryEntry back(HelpViewer helpViewer)
	{
		if (historyPos <= 1)
		{
			return null;
		}
		setCurrentScrollPosition(helpViewer.getCurrentPage(),helpViewer.getCurrentScrollPosition());
		HistoryEntry result = new HistoryEntry(history[--historyPos - 1]);
		fireUpdate();
		return result;
	} //}}}

	//{{{ hasPrevious() method
	public boolean hasPrevious()
	{
		return (historyPos>1);
	} //}}}

	//{{{ addToHistory() method
	public void addToHistory(String url)
	{
		history[historyPos] = new HistoryEntry(url,url,0);
		if(historyPos + 1 == history.length)
		{
			System.arraycopy(history,1,history,
					 0,history.length - 1);
			history[historyPos] = null;
		}
		else
		{
			historyPos++;
			for (int i = historyPos ; i<history.length ; i++)
			{
				history[i] = null;
			}
		}
		fireUpdate();
	} //}}}

	//{{{ setCurrentScrollPosition() method
	public void setCurrentScrollPosition(URL currentPage, int scrollPosition)
	{
		if ((null != currentPage) && (historyPos >= 1) &&
		    (currentPage.toString().equals(history[historyPos-1].url)))
		{
			history[historyPos-1].scrollPosition = scrollPosition;
		}
	} //}}}

	//{{{ setCurrentEntry() method
	public void setCurrentEntry(HistoryEntry entry)
	{
		for (int i=0 ; i<history.length ; i++)
		{
			if ((history[i] != null) && (history[i].equals(entry)))
			{
				historyPos = i+1;
				fireUpdate();
				break;
			}
		}
		// Do nothing?
	} //}}}

	//{{{ updateTitle() method
	public void updateTitle(String url, String title)
	{
		for (int i=0;i<history.length;i++)
		{
			if ((history[i] != null) && history[i].url.equals(url))
			{
				history[i].title = title;
			}
		}
		fireUpdate();
	}//}}}

	//{{{ getPreviousURLs() method
	HistoryEntry[] getPreviousURLs()
	{
		if (historyPos<=1)
		{
			return new HelpHistoryModel.HistoryEntry[0];
		}
		HistoryEntry[] previous = new HistoryEntry[historyPos-1];
		System.arraycopy(history,0,previous,0,historyPos-1);
		return previous;
	} //}}}

	//{{{ getNextURLs() method
	HistoryEntry[] getNextURLs()
	{
		if (history.length - historyPos <= 1)
		{
			return new HelpHistoryModel.HistoryEntry[0];
		}
		if (history[historyPos] == null)
		{
			return new HelpHistoryModel.HistoryEntry[0];
		}
		HistoryEntry[] next = new HistoryEntry[history.length-historyPos];
		System.arraycopy(history,historyPos,next,0,history.length-historyPos);
		return next;
	} //}}}

	//{{{ addHelpHistoryModelListener() method
	public void addHelpHistoryModelListener(HelpHistoryModelListener hhml)
	{
		listeners.add(hhml);
	} //}}}

	//{{{ removeHelpHistoryModelListener() method
	public void removeHelpHistoryModelListener(HelpHistoryModelListener hhml)
	{
		listeners.remove(hhml);
	} //}}}

	//{{{ fireUpdate() method
	public void fireUpdate()
	{
		for (int i=0 ; i<listeners.size() ; i++)
		{
			listeners.get(i).historyUpdated();
		}
	} //}}}

	//{{{ Private members
	private int historyPos;
	private HistoryEntry[] history;
	private List<HelpHistoryModelListener> listeners;
	//}}}

	//{{{ Inner Classes

	//{{{ HistoryEntry class
	static class HistoryEntry
	{
		String url;
		String title;
		int scrollPosition;

		//{{{ HistoryEntry constructor
		HistoryEntry(String url, String title)
		{
			this(url,title,0);
		} //}}}

		//{{{ HistoryEntry constructor
		HistoryEntry(HistoryEntry original)
		{
			this(original.url,original.title,original.scrollPosition);
		} //}}}

		//{{{ HistoryEntry constructor
		HistoryEntry(String url, String title, int scrollPosition)
		{
			this.url = url;
			this.title = title;
			this.scrollPosition = scrollPosition;
		} //}}}

		//{{{ equals() method
		public boolean equals(HistoryEntry he)
		{
			return he.url.equals(this.url) &&
			       he.title.equals(this.title) &&
			       (he.scrollPosition == scrollPosition);
		} //}}}

		//{{{ toString() method
		public String toString()
		{
			return getClass().getName() + "[url=" + url + ",title=" + title
			+ ",scrollPosition=" + scrollPosition + ']';
		} //}}}
	} //}}}

	//}}}
}
