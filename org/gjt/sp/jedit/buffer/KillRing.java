/*
 * KillRing.java - Stores deleted text
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import javax.swing.ListModel;
import org.gjt.sp.jedit.jEdit;

public class KillRing
{
	//{{{ propertiesChanged() method
	public static void propertiesChanged()
	{
		UndoManager.Remove[] newRing = new UndoManager.Remove[
			jEdit.getIntegerProperty("history",25)];
		if(ring != null)
		{
			/* System.arraycopy(ring,0,newRing,0,
				Math.min(ring.length */
		}
		ring = newRing;
		//XXX
		count = 0;
		wrap = false;
	} //}}}

	//{{{ getListModel() method
	public static ListModel getListModel()
	{
		return new RingListModel();
	} //}}}

	//{{{ Package-private members
	static UndoManager.Remove[] ring;
	static int count;
	static boolean wrap;

	//{{{ changed() method
	static void changed(UndoManager.Remove rem)
	{
		if(rem.inKillRing)
		{
			// compare existing entries' hashcode with this
			int length = (wrap ? ring.length : count);
			int kill = -1;
			boolean duplicate = false;

			for(int i = 0; i < length; i++)
			{
				if(ring[i] != rem
					&& ring[i].hashcode == rem.hashcode
					&& ring[i].str.equals(rem.str))
				{
					// we don't want duplicate
					// entries in the kill ring
					kill = i;
					break;
				}
			}

			if(kill != -1)
				remove(kill);
		}
		else
			add(rem);
	} //}}}

	//{{{ add() method
	static void add(UndoManager.Remove rem)
	{
		// compare existing entries' hashcode with this
		int length = (wrap ? ring.length : count);
		for(int i = 0; i < length; i++)
		{
			if(ring[i].hashcode == rem.hashcode)
			{
				// strings might be equal!
				if(ring[i].str.equals(rem.str))
				{
					// we don't want duplicate entries
					// in the kill ring
					return;
				}
			}
		}

		// no duplicates, check for all-whitespace string
		boolean allWhitespace = true;
		for(int i = 0; i < rem.str.length(); i++)
		{
			if(!Character.isWhitespace(rem.str.charAt(i)))
			{
				allWhitespace = false;
				break;
			}
		}

		if(allWhitespace)
			return;

		rem.inKillRing = true;

		if(ring[count] != null)
			ring[count].inKillRing = false;

		ring[count] = rem;
		if(++count >= ring.length)
		{
			wrap = true;
			count = 0;
		}
	} //}}}

	//{{{ remove() method
	static void remove(int i)
	{
		if(wrap)
		{
			UndoManager.Remove[] newRing = new UndoManager.Remove[
				ring.length];
			int newCount = 0;
			for(int j = 0; j < ring.length; j++)
			{
				int index;
				if(j < count)
					index = count - j - 1;
				else
					index = count + ring.length - j - 1;

				if(i == index)
				{
					ring[index].inKillRing = false;
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

	//{{{ RingListModel class
	static class RingListModel implements ListModel
	{
		public void addListDataListener(ListDataListener listener)
		{
		}

		public void removeListDataListener(ListDataListener listener)
		{
		}

		public Object getElementAt(int index)
		{
			UndoManager.Remove rem;

			if(wrap)
			{
				if(index < count)
					rem = ring[count - index - 1];
				else
					rem = ring[count + ring.length - index - 1];
			}
			else
				rem = ring[count - index - 1];

			return rem.str;
		}

		public int getSize()
		{
			if(wrap)
				return ring.length;
			else
				return count;
		}
	} //}}}

	//}}}
}
