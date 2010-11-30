/*
 * HyperSearchResult.java - HyperSearch result
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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
import javax.swing.text.Position;

import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * A set of occurrences of the search string on a given line in a buffer.
 * @author Slava Pestov
 */
public class HyperSearchResult implements HyperSearchNode
{
	public String path;
	public Buffer buffer;
	public int line;
	public String str; // cached for speed
	public Occur occur;
	public int occurCount;

	//{{{ getBuffer() method
	public Buffer getBuffer(View view)
	{
		if(buffer == null)
			buffer = jEdit.openFile(view,path);
		return buffer;
	} //}}}

	//{{{ getSelection() method
	/**
	 * Returns an array of selection objects pointing to the occurrences
	 * of the search term on the current line. The buffer must be opened
	 * first.
	 * @since jEdit 4.2pre5
	 */
	public Selection[] getSelection()
	{
		if(buffer == null)
			return null;

		Selection[] returnValue = new Selection[occurCount];
		Occur o = occur;
		int i = 0;
		while(o != null)
		{
			int start = o.startPos.getOffset();
			int end = o.endPos.getOffset();
			Selection.Range s = new Selection.Range(
				start,
				end
			);
			returnValue[i++] = s;
			o = o.next;
		}
		return returnValue;
	} //}}}

	//{{{ goTo() method
	public void goTo(final EditPane editPane)
	{
		new GotoDelayed(editPane);
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return str;
	} //}}}

	//{{{ Package-private members

	//{{{ HyperSearchResult constructor
	HyperSearchResult(Buffer buffer, int line)
	{
		path = buffer.getPath();

		if(!buffer.isTemporary())
			bufferOpened(buffer);

		this.line = line;

		str = (line + 1) + ": " + buffer.getLineText(line)
			.replace('\t',' ').trim();
	} //}}}

	//{{{ bufferOpened() method
	void bufferOpened(Buffer buffer)
	{
		this.buffer = buffer;
		Occur o = occur;
		while(o != null)
		{
			o.bufferOpened();
			o = o.next;
		}
	} //}}}

	//{{{ bufferClosed() method
	void bufferClosed()
	{
		buffer = null;
		Occur o = occur;
		while(o != null)
		{
			o.bufferClosed();
			o = o.next;
		}
	} //}}}

	//{{{ addOccur() method
	void addOccur(int start, int end)
	{
		Occur o = new Occur(start,end);
		o.next = occur;
		occur = o;
		occurCount++;
	} //}}}

	//{{{ pathEquals() method
	/**
	 * @param path A canonical path
	 */
	boolean pathEquals(String path)
	{
		return path.equals(MiscUtilities.resolveSymlinks(this.path));
	} //}}}
	
	//{{{ equals() method
	public boolean equals(Object compareObj)
	{
		if (!(compareObj instanceof HyperSearchResult))
			return false;
		HyperSearchResult otherResult = (HyperSearchResult)compareObj;
		return pathEquals(otherResult.path) && line == otherResult.line
			&& buffer.equals(otherResult.buffer);		
	}//}}}

	//}}}

	//{{{ Occur class
	public class Occur
	{
		public int start, end;
		public Position startPos, endPos;
		public Occur next;

		//{{{ Occur constructor
		Occur(int start, int end)
		{
			this.start = start;
			this.end = end;

			if(buffer != null && !buffer.isTemporary())
				bufferOpened();
		} //}}}

		//{{{ bufferOpened() method
		void bufferOpened()
		{
			startPos = buffer.createPosition(Math.min(
				buffer.getLength(),start));
			endPos = buffer.createPosition(Math.min(
				buffer.getLength(),end));
		} //}}}

		//{{{ bufferClosed() method
		void bufferClosed()
		{
			start = startPos.getOffset();
			end = endPos.getOffset();
			startPos = endPos = null;
		} //}}}
	} //}}}

	public class GotoDelayed implements Runnable
	{
		private final EditPane editPane;
		private volatile boolean loadedEventReceived = false;

		private GotoDelayed(EditPane editPane)
		{
			this.editPane = editPane;
			EditBus.addToBus(this);
			buffer = getBuffer(editPane.getView());
			if(buffer == null)
			{
				EditBus.removeFromBus(this);
				return;
			}
			editPane.setBuffer(buffer);
			synchronized (this)
			{
				if (!loadedEventReceived && buffer.isLoaded())
				{
					bufferLoaded();
				}
			}

		}

		public void run()
		{
			Selection[] s = getSelection();
			if(s == null)
				return;

			JEditTextArea textArea = editPane.getTextArea();
			if(textArea.isMultipleSelectionEnabled())
				textArea.addToSelection(s);
			else
				textArea.setSelection(s);

			textArea.moveCaretPosition(occur.endPos.getOffset());
		}

		private void bufferLoaded()
		{
			synchronized (this)
			{
				if (!loadedEventReceived)
				{
					EditBus.removeFromBus(this);
					loadedEventReceived = true;
					ThreadUtilities.runInDispatchThread(this);
				}
			}
		}

		@EBHandler
		public void handleBufferUpdate(BufferUpdate msg)
		{
			if (msg.getWhat() == BufferUpdate.LOADED &&
				msg.getBuffer() == buffer)
			{
				bufferLoaded();
			}
		}
	}
}
