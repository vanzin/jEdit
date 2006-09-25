/*
 * KillRing.java - Stores deleted text
 * :tabSize=8:indentSize=8:noTabs=false:
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
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.MutableListModel;
import org.gjt.sp.util.XMLUtilities;

/**
 * The kill ring retains deleted text. This class is a singleton -- only one
 * kill ring is used for all of jEdit. Nothing prevents plugins from making their
 * own kill rings for whatever reason, though.
 */
public class KillRing implements MutableListModel
{
	//{{{ getInstance() method
	public static KillRing getInstance()
	{
		return killRing;
	} //}}}

	//{{{ getInstance() method
	public static void setInstance(KillRing killRing)
	{
		KillRing.killRing = killRing;
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		int newSize = Math.max(1,jEdit.getIntegerProperty("history",25));
		if(ring == null)
			ring = new UndoManager.Remove[newSize];
		else if(newSize != ring.length)
		{
			UndoManager.Remove[] newRing = new UndoManager.Remove[
				newSize];
			int newCount = Math.min(getSize(),newSize);
			for(int i = 0; i < newCount; i++)
			{
				newRing[i] = (UndoManager.Remove)getElementAt(i);
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

	//{{{ MutableListModel implementation
	public void addListDataListener(ListDataListener listener) {}

	public void removeListDataListener(ListDataListener listener) {}

	//{{{ getElementAt() method
	public Object getElementAt(int index)
	{
		return ring[virtualToPhysicalIndex(index)];
	} //}}}

	//{{{ getSize() method
	public int getSize()
	{
		if(wrap)
			return ring.length;
		else
			return count;
	} //}}}

	//{{{ removeElement() method
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
	public void insertElementAt(Object value, int index)
	{
		/* This is not terribly efficient, but this method is only
		called by the 'Paste Deleted' dialog where the performance
		is not exactly vital */
		remove(index);
		add((UndoManager.Remove)value);
	} //}}}

	//}}}

	//{{{ Package-private members

	//{{{ changed() method
	void changed(UndoManager.Remove rem)
	{
		if(rem.inKillRing)
		{
			// compare existing entries' hashcode with this
			int length = (wrap ? ring.length : count);
			int kill = -1;

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
	void add(UndoManager.Remove rem)
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
	void remove(int i)
	{
		if(wrap)
		{
			UndoManager.Remove[] newRing = new UndoManager.Remove[
				ring.length];
			int newCount = 0;
			for(int j = 0; j < ring.length; j++)
			{
				int index = virtualToPhysicalIndex(j);

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

	//}}}

	//{{{ Private members
	protected UndoManager.Remove[] ring;
	protected int count;
	protected boolean wrap;
	protected long killRingModTime;
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

	//}}}

	//{{{ KillRingHandler class
	public static class KillRingHandler extends DefaultHandler
	{
		public List<UndoManager.Remove> list = new LinkedList<UndoManager.Remove>();

		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "killring.dtd", getClass());
		}

		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			inEntry = qName.equals("ENTRY");
		}

		public void endElement(String uri, String localName, String name)
		{
			if(name.equals("ENTRY"))
			{
				list.add(new UndoManager.Remove(null,0,0,charData.toString()));
				inEntry = false;
				charData.setLength(0);
			}
		}

		public void characters(char[] ch, int start, int length)
		{
			if (inEntry)
				charData.append(ch, start, length);
		}

		private StringBuffer charData = new StringBuffer();
		private boolean inEntry;
	} //}}}
}
