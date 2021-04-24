/*
 * PositionManager.java - Manages positions
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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

//{{{ Imports
import javax.annotation.Nonnull;
import javax.swing.text.Position;
import java.lang.ref.Cleaner;
import java.util.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * A class internal to jEdit's document model. You should not use it
 * directly.
 * <p>Positions are created explicitly and removed implicitly, when
 * there are no more references to it. For this implicit removal to work
 * a top (referenced outside) and a bottom half (referenced internally)
 * of the position are implemented separately.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre3
 */
class PositionManager
{
	//{{{ PositionManager constructor
	PositionManager(JEditBuffer buffer)
	{
		this.buffer = buffer;
		cleaner = Cleaner.create();
	} //}}}
	
	//{{{ createPosition() method
	/** No explicit removal is required. Unreferencing is enough. */
	public Position createPosition(int offset)
	{
		PosBottomHalf existing;
		Position posTopHalf;
		synchronized (this)
		{
			PosBottomHalf bh = new PosBottomHalf(offset);
			existing = positions.get(bh);
			if(existing == null)
			{
				positions.put(bh,bh);
				existing = bh;
			}

			posTopHalf = new PosTopHalf(existing);
			existing.ref();
		}
		PosBottomHalf finalExisting = existing;
		cleaner.register(posTopHalf, () -> unref(finalExisting));
		return posTopHalf;
	} //}}}

	//{{{ contentInserted() method
	public synchronized void contentInserted(int offset, int length)
	{
		if(positions.isEmpty())
			return;

		/* get all positions from offset to the end, inclusive */
		Iterator<PosBottomHalf> iter = positions.tailMap(new PosBottomHalf(offset))
			.keySet().iterator();

		iteration = true;
		while(iter.hasNext())
		{
			iter.next().contentInserted(offset,length);
		}
		iteration = false;
	} //}}}

	//{{{ contentRemoved() method
	public synchronized void contentRemoved(int offset, int length)
	{
		if(positions.isEmpty())
			return;

		/* get all positions from offset to the end, inclusive */
		Iterator<PosBottomHalf> iter = positions.tailMap(new PosBottomHalf(offset))
			.keySet().iterator();

		iteration = true;
		while(iter.hasNext())
		{
			iter.next().contentRemoved(offset,length);
		}
		iteration = false;

	} //}}}

	private void unref(PosBottomHalf posBottomHalf)
	{
		synchronized (this)
		{
			posBottomHalf.unref();
		}
	}

	boolean iteration;

	//{{{ Private members
	private final Cleaner cleaner;
	private final JEditBuffer buffer;
	private final SortedMap<PosBottomHalf, PosBottomHalf> positions = new TreeMap<>();
	//}}}

	//{{{ Inner classes

	//{{{ PosTopHalf class
	/** A wrapper for real position handling done by
	  * <code>PosBottomHalf</code>, so Top means the part that is
	  * visible. When there are no more references
	  * to <code>PosTopHalf</code> and garbage
	  * collector eats it, the position is removed together with its
	  * bottom half. */
	private static class PosTopHalf implements Position
	{
		private final PosBottomHalf bh;

		//{{{ PosTopHalf constructor
		PosTopHalf(PosBottomHalf bh)
		{
			this.bh = bh;
		} //}}}

		//{{{ getOffset() method
		@Override
		public int getOffset()
		{
			return bh.getOffset();
		} //}}}
	} //}}}

	//{{{ PosBottomHalf class
	/** 'bottom' means the part
	  * that is not visible outside and stays only here in
	  * <code>positions</code> map.*/
	class PosBottomHalf implements Comparable<PosBottomHalf>
	{
		private int offset;
		private int ref;

		//{{{ PosBottomHalf constructor
		PosBottomHalf(int offset)
		{
			this.offset = offset;
		} //}}}

		//{{{ getOffset() method
		public int getOffset()
		{
			return offset;
		} //}}}

		//{{{ ref() method
		void ref()
		{
			ref++;
		} //}}}

		//{{{ unref() method
		void unref()
		{
			if(--ref == 0)
				positions.remove(this);
		} //}}}

		//{{{ contentInserted() method
		void contentInserted(int offset, int length)
		{
			if(offset > this.offset)
				throw new ArrayIndexOutOfBoundsException(offset);
			this.offset += length;
			checkInvariants();
		} //}}}

		//{{{ contentRemoved() method
		void contentRemoved(int offset, int length)
		{
			if(offset > this.offset)
				throw new ArrayIndexOutOfBoundsException(offset);
			if(this.offset <= offset + length)
				this.offset = offset;
			else
				this.offset -= length;
			checkInvariants();
		} //}}}

		//{{{ equals() method
		@Override
		public boolean equals(Object o)
		{
			if(!(o instanceof PosBottomHalf))
				return false;

			return ((PosBottomHalf)o).offset == offset;
		} //}}}

		//{{{ compareTo() method
		@Override
		public int compareTo(@Nonnull PosBottomHalf posBottomHalf)
		{
			if(iteration)
				Log.log(Log.ERROR,this,"Consistency failure");
			return offset - posBottomHalf.offset;
		} //}}}
		
		//{{{ checkInvariants() method
		private void checkInvariants()
		{
			if(offset < 0 || offset > buffer.getLength())
				throw new ArrayIndexOutOfBoundsException(offset);
		} //}}}
	} //}}}

	//}}}
}
