/*
 * PositionManager.java - Manages positions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
import javax.swing.text.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.Log;
//}}}

/**
 * A class internal to jEdit's document model. You should not use it
 * directly. To improve performance, none of the methods in this class
 * check for out of bounds access, nor are they thread-safe. The
 * <code>Buffer</code> class, through which these methods must be
 * called through, implements such protection.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre3
 */
public class PositionManager
{
	//{{{ createPosition() method

	// note: Buffer.createPosition() grabs a read lock, so the buffer
	// will not change during this method. however, if two stops call
	// it, there can be contention issues unless this method is
	// synchronized.

	// I could make Buffer.createPosition() grab a write lock, but then
	// it would be necessary to implement grabbing write locks within
	// read locks, since HyperSearch for example does everything inside
	// a read lock.
	public synchronized Position createPosition(int offset)
	{
		PosBottomHalf bh = null;

		for(int i = 0; i < positionCount; i++)
		{
			PosBottomHalf _bh = positions[i];
			if(_bh.offset == offset)
			{
				bh = _bh;
				break;
			}
			else if(_bh.offset > offset)
			{
				bh = new PosBottomHalf(offset);
				growPositionArray();
				System.arraycopy(positions,i,positions,i+1,
					positionCount - i);
				positionCount++;
				positions[i] = bh;
				break;
			}
		}

		if(bh == null)
		{
			bh = new PosBottomHalf(offset);
			growPositionArray();
			positions[positionCount++] = bh;
		}

		return new PosTopHalf(bh);
	} //}}}

	//{{{ Private members
	//{{{ growPositionArray() method
	private void growPositionArray()
	{
		if(positions.length < positionCount + 1)
		{
			PosBottomHalf[] newPositions = new PosBottomHalf[
				(positionCount + 1) * 2];
			System.arraycopy(positions,0,newPositions,0,positionCount);
			positions = newPositions;
		}
	} //}}}

	//{{{ removePosition() method
	private synchronized void removePosition(PosBottomHalf bh)
	{
		int index = -1;

		for(int i = 0; i < positionCount; i++)
		{
			if(positions[i] == bh)
			{
				index = i;
				break;
			}
		}

		System.arraycopy(positions,index + 1,positions,index,
			positionCount - index - 1);
		positions[--positionCount] = null;
	} //}}}

	//{{{ updatePositionsForInsert() method
	private void updatePositionsForInsert(int offset, int length)
	{
		if(positionCount == 0)
			return;

		int start = getPositionAtOffset(offset);

		for(int i = start; i < positionCount; i++)
		{
			PosBottomHalf bh = positions[i];
			if(bh.offset < offset)
				Log.log(Log.ERROR,this,"Screwed up: " + bh.offset);
			else
				bh.offset += length;
		}
	} //}}}

	//{{{ updatePositionsForRemove() method
	private void updatePositionsForRemove(int offset, int length)
	{
		if(positionCount == 0)
			return;

		int start = getPositionAtOffset(offset);

		for(int i = start; i < positionCount; i++)
		{
			PosBottomHalf bh = positions[i];
			if(bh.offset < offset)
				Log.log(Log.ERROR,this,"Screwed up: " + bh.offset);
			else if(bh.offset < offset + length)
				bh.offset = offset;
			else
				bh.offset -= length;
		}
	} //}}}

	//{{{ getPositionAtOffset() method
	private int getPositionAtOffset(int offset)
	{
		int start = 0;
		int end = positionCount - 1;

		PosBottomHalf bh;

loop:		for(;;)
		{
			switch(end - start)
			{
			case 0:
				bh = positions[start];
				if(bh.offset < offset)
					start++;
				break loop;
			case 1:
				bh = positions[end];
				if(bh.offset < offset)
				{
					start = end + 1;
				}
				else
				{
					bh = positions[start];
					if(bh.offset < offset)
					{
						start++;
					}
				}
				break loop;
			default:
				int pivot = (start + end) / 2;
				bh = positions[pivot];
				if(bh.offset > offset)
					end = pivot - 1;
				else
					start = pivot + 1;
				break;
			}
		}

		return start;
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ PosTopHalf class
	static class PosTopHalf implements Position
	{
		PosBottomHalf bh;

		//{{{ PosTopHalf constructor
		PosTopHalf(PosBottomHalf bh)
		{
			this.bh = bh;
			bh.ref();
		} //}}}

		//{{{ getOffset() method
		public int getOffset()
		{
			return bh.offset;
		} //}}}

		//{{{ finalize() method
		public void finalize()
		{
			bh.unref();
		} //}}}
	} //}}}

	//{{{ PosBottomHalf class
	class PosBottomHalf
	{
		int offset;
		int ref;

		//{{{ PosBottomHalf constructor
		PosBottomHalf(int offset)
		{
			this.offset = offset;
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
				removePosition(this);
		} //}}}
	} //}}}

	//}}}
}
