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

/**
 * History model used by the help browser 
 * @author Nicholas O'Leary
 * @version $Id$
 */
public class HelpHistoryModel
{
	//{{{ HelpHistoryModel
	public HelpHistoryModel(int size)
	{
		int 
		historyPos = 0;
		history = new HistoryEntry[size];
		listeners = new java.util.Vector();
	} //}}}
	
	//{{{ forward
	public String forward()
	{
		if(history.length - historyPos <= 1)
			return null;
		if (history[historyPos] == null)
			return null;
		String url = history[historyPos].url;
		historyPos++;
		fireUpdate();
		return url;
	} //}}}
	
	//{{{ hasNext
	public boolean hasNext()
	{
		return !(history.length - historyPos <= 1 ||
			history[historyPos] == null);
	} //}}}
	
	//{{{ back
	public String back()
	{
		if (historyPos<=1)
			return null;
		String url = history[--historyPos - 1].url;
		fireUpdate();
		return url;
	} //}}}
	
	//{{{ hasPrevious
	public boolean hasPrevious()
	{
		return (historyPos>1);
	} //}}}
	
	//{{{ addToHistory
	public void addToHistory(String url)
	{
		history[historyPos] = new HistoryEntry(url,url);
		if(historyPos + 1 == history.length)
		{
			System.arraycopy(history,1,history,
				0,history.length - 1);
			history[historyPos] = null;
		}
		else
		{
			historyPos++;
			for (int i = historyPos;i<history.length;i++)
			{
				history[i] = null;
			}
		}
		fireUpdate();
	} //}}}
	
	//{{{ setCurrentEntry
	public void setCurrentEntry(HistoryEntry entry)
	{
		for (int i=0;i<history.length;i++)
		{
			if (history[i]!=null && history[i].equals(entry))
			{
				historyPos = i+1;
				fireUpdate();
				break;
			}
		}
		// Do nothing?
	} //}}}
	
	//{{{ updateTitle
	public void updateTitle(String url, String title)
	{
		for (int i=0;i<history.length;i++)
		{
			if (history[i]!=null && history[i].url.equals(url))
			{
				history[i].title = title;
			}
		}
		fireUpdate();
	}//}}}
	
	//{{{ getPreviousURLs
	public HistoryEntry[] getPreviousURLs()
	{
		if (historyPos<=1)
			return new HelpHistoryModel.HistoryEntry[0];
		HistoryEntry[] previous = new HistoryEntry[historyPos-1];
		System.arraycopy(history,0,previous,0,historyPos-1);
		return previous;
	} //}}}
	
	//{{{ getNextURLs
	public HistoryEntry[] getNextURLs()
	{
		if (history.length - historyPos <= 1)
			return new HelpHistoryModel.HistoryEntry[0];
		if (history[historyPos] == null)
			return new HelpHistoryModel.HistoryEntry[0];
		HistoryEntry[] next = new HistoryEntry[history.length-historyPos];
		System.arraycopy(history,historyPos,next,0,history.length-historyPos);
		return next;
	} //}}}
	
	//{{{ addHelpHistoryModelListener
	public void addHelpHistoryModelListener(HelpHistoryModelListener hhml)
	{
		listeners.add(hhml);
	} //}}}
	
	//{{{ removeHelpHistoryModelListener
	public void removeHelpHistoryModelListener(HelpHistoryModelListener hhml)
	{
		listeners.remove(hhml);
	} //}}}
	
	//{{{ fireUpdate
	public void fireUpdate()
	{
		for (int i=0;i<listeners.size();i++)
			((HelpHistoryModelListener)listeners.elementAt(i)).historyUpdated();
	} //}}}
	
	//{{{ Private members
	private int historyPos;
	private HistoryEntry[] history;
	private java.util.Vector listeners;
	//}}}
	
	//{{{ Inner Classes
	class HistoryEntry
	{
		String url;
		String title;
		HistoryEntry(String url,String title)
		{
			this.url = url;
			this.title = title;
		}
		public boolean equals(HistoryEntry he)
		{
			return he.url.equals(this.url) &&
				he.title.equals(this.title);
		}
	}
	//}}}
}


