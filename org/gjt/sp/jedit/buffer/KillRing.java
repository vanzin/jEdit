/*
 * KillRing.java - Stores deleted text
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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

package org.gjt.sp.jedit.buffer;

import javax.swing.event.ListDataListener;
import java.util.Collection;

import org.gjt.sp.jedit.gui.MutableListModel;

/**
 * The kill ring retains deleted text. This class is a singleton -- only one
 * kill ring is used for all of jEdit. Nothing prevents plugins from making their
 * own kill rings for whatever reason, though.
 */
public class KillRing implements MutableListModel<String>
{
	//{{{ getInstance() method
	public static KillRing getInstance()
	{
		return killRing;
	} //}}}

	//{{{ setInstance() method
	public static void setInstance(KillRing killRing)
	{
		KillRing.killRing = killRing;
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged(int historySize)
	{
		int newSize = Math.max(1, historySize);
		if(ring == null)
			ring = new String[newSize];
		else if(newSize != ring.length)
		{
			String[] newRing = new String[newSize];
			int newCount = Math.min(getSize(),newSize);
			for(int i = 0; i < newCount; i++)
			{
				newRing[i] = getElementAt(i);
			}
			ring = newRing;
			count = newCount;
			wrap = false;
		}

		if(count == ring.length)
		{
			count = 0;
			wrap = true;
		}
	} //}}}

	public void load() {}

	public void save() {}

	//{{{ reset() method
	/**
	 * This method is made to be used by implementation of load()
	 * method to initialize (or reset) the killring by a loaded
	 * sequence of objects.
	 *
	 * Each element is converted to an element of the killring as
	 * followings:
	 *   - If it is a String, it is converted as if it is a result of
	 *     getElementAt(n).toString().
	 *   - Otherwise, it is converted as if it is a Object which was
	 *     obtained by getElementAt(n).
	 * @param source the loaded killring.
	 * @since jEdit 4.3pre12
	 */
	protected void reset(Collection<String> source)
	{
		String[] newRing = new String[source.size()];
		int i = 0;
		for(String x: source)
		{
			newRing[i++] = x;
		}
		ring = newRing;
		count = 0;
		wrap = true;
	} //}}}

	//{{{ MutableListModel implementation
	@Override
	public void addListDataListener(ListDataListener listener) {}

	@Override
	public void removeListDataListener(ListDataListener listener) {}

	//{{{ getElementAt() method
	@Override
	public String getElementAt(int index)
	{
		return ring[virtualToPhysicalIndex(index)];
	} //}}}

	//{{{ getSize() method
	@Override
	public int getSize()
	{
		if(wrap)
			return ring.length;
		else
			return count;
	} //}}}

	//{{{ removeElement() method
	@Override
	public boolean removeElement(Object value)
	{
		for(int i = 0; i < getSize(); i++)
		{
			if(ring[i].equals(value))
			{
				remove(i);
				return true;
			}
		}
		return false;
	} //}}}

	//{{{ insertElementAt() method
	@Override
	public void insertElementAt(String value, int index)
	{
		/* This is not terribly efficient, but this method is only
		called by the 'Paste Deleted' dialog where the performance
		is not exactly vital */
		remove(index);
		add(value);
	} //}}}

	//}}}

	//{{{ Package-private members

	//{{{ changed() method
	void changed(String oldStr, String newStr)
	{
		int i = indexOf(oldStr);
		if(i != -1)
			ring[i] = newStr;
		else
			add(newStr);
	} //}}}

	//{{{ add() method
	void add(String removed)
	{
		// we don't want duplicate entries
		// in the kill ring
		if(indexOf(removed) != -1)
			return;

		// no duplicates, check for all-whitespace string
		boolean allWhitespace = true;
		for(int i = 0; i < removed.length(); i++)
		{
			if(!Character.isWhitespace(removed.charAt(i)))
			{
				allWhitespace = false;
				break;
			}
		}

		if(allWhitespace)
			return;

		ring[count] = removed;
		if(++count >= ring.length)
		{
			wrap = true;
			count = 0;
		}
	} //}}}

	//{{{ remove() method
	void remove(int i)
	{
		if(wrap)
		{
			String[] newRing = new String[ring.length];
			int newCount = 0;
			for(int j = 0; j < ring.length; j++)
			{
				int index = virtualToPhysicalIndex(j);

				if(i == index)
				{
					continue;
				}

				newRing[newCount++] = ring[index];
			}
			ring = newRing;
			count = newCount;
			wrap = false;
		}
		else
		{
			System.arraycopy(ring,i + 1,ring,i,count - i - 1);
			count--;
		}
	} //}}}

	//}}}

	//{{{ Private members
	private String[] ring;
	private int count;
	private boolean wrap;
	private static KillRing killRing = new KillRing();

	//{{{ virtualToPhysicalIndex() method
	/**
	 * Since the kill ring has a wrap-around representation, we need to
	 * convert user-visible indices to actual indices in the array.
	 */
	private int virtualToPhysicalIndex(int index)
	{
		if(wrap)
		{
			if(index < count)
				return count - index - 1;
			else
				return count + ring.length - index - 1;
		}
		else
			return count - index - 1;
	} //}}}

	//{{{ indexOf() method
	private int indexOf(String str)
	{
		int length = (wrap ? ring.length : count);
		for(int i = length - 1; i >= 0; i--)
		{
			if(ring[i].equals(str))
			{
				return i;
			}
		}
		return -1;
	} //}}}

	//}}}
}
