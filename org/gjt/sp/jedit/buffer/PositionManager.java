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
import org.gjt.sp.jedit.MiscUtilities;
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
	public synchronized Position createPosition(int offset)
	{
		PosBottomHalf bh;

		if(root == null)
			root = bh = new PosBottomHalf(offset);
		else
		{
			bh = root.find(offset);

			if(bh == null)
			{
				bh = new PosBottomHalf(offset);
				bh.red = true;
				root.insert(bh);
				if(!Debug.DISABLE_POSITION_BALANCE)
					ibalance(bh);
			}
		}

		if(Debug.POSITION_DEBUG)
			root.dump(0);

		return new PosTopHalf(bh);
	} //}}}

	//{{{ contentInserted() method
	public synchronized void contentInserted(int offset, int length)
	{
		if(root != null)
			root.contentInserted(offset,length);
	} //}}}

	//{{{ contentRemoved() method
	public synchronized void contentRemoved(int offset, int length)
	{
		if(root != null)
			root.contentRemoved(offset,length);
	} //}}}

	//{{{ Private members
	private PosBottomHalf root;

	//{{{ removePosition() method
	private synchronized void removePosition(PosBottomHalf bh)
	{
		PosBottomHalf r, x = bh.parent;

		// if one of the siblings is null, make &this=non null sibling
		if(bh.left == null)
		{
			r = bh.right;
			if(bh.parent == null)
				root = bh.right;
			else
			{
				if(bh == bh.parent.left)
					bh.parent.left = bh.right;
				else
					bh.parent.right = bh.right;
			}
			if(bh.right != null)
				bh.right.parent = bh.parent;
		}
		else if(bh.right == null)
		{
			r = bh.left;
			if(bh.parent == null)
				root = bh.left;
			else
			{
				if(bh == bh.parent.left)
					bh.parent.left = bh.left;
				else
					bh.parent.right = bh.left;
			}
			if(bh.left != null)
				bh.left.parent = bh.parent;
		}
		// neither is null
		else
		{
			PosBottomHalf nextInorder = bh.right;
			r = nextInorder;
			while(nextInorder.left != null)
			{
				nextInorder = nextInorder.left;
				r = nextInorder.right;
			}
			// removing the root?
			if(bh.parent == null)
			{
				root = nextInorder;
			}
			else
			{
				if(bh.parent.left == bh)
					bh.parent.left = nextInorder;
				else
					bh.parent.right = nextInorder;
			}

			if(nextInorder != bh.right)
			{
				nextInorder.parent.left = nextInorder.right;
				if(nextInorder.right != null)
				{
					nextInorder.right.parent = nextInorder.parent;
				}
				nextInorder.right = bh.right;
				nextInorder.right.parent = nextInorder;
			}
			nextInorder.parent = bh.parent;
			nextInorder.left = bh.left;
			nextInorder.left.parent = nextInorder;
			x = nextInorder.parent;
		}

		if(!bh.red)
		{
			if(r != null && r.red)
				r.red = false;
			else
			{
				PosBottomHalf y;
				if(x == null)
					y = null;
				else if(x.left == r)
					y = x.right;
				else
					y = x.left;

				if(!Debug.DISABLE_POSITION_BALANCE)
					rbalance(r,x,y);
			}
		}

		if(Debug.POSITION_DEBUG && root != null)
			root.dump(0);
	} //}}}

	//{{{ ibalance() method
	private void ibalance(PosBottomHalf bh)
	{
		if(bh.parent.red)
		{
			PosBottomHalf u = bh.parent.parent;
			PosBottomHalf w;
			if(u.left == bh.parent)
				w = u.right;
			else
				w = u.left;
			if(w != null && w.red)
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"case 2");
				bh.parent.red = false;
				w.red = false;
				if(u.parent == null)
					u.red = false;
				else
				{
					u.red = true;
					ibalance(u);
				}
			}
			else
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"case 1");
				PosBottomHalf b = bh.restructure();
				b.red = false;
				b.left.red = true;
				b.right.red = true;
			}
		}
	} //}}}

	//{{{ rbalance() method
	private void rbalance(PosBottomHalf r, PosBottomHalf x,
		PosBottomHalf y)
	{
		PosBottomHalf z;

		if(y != null && !y.red)
		{
			if(y.left != null && y.left.red)
				z = y.left;
			else if(y.right != null && y.right.red)
				z = y.right;
			else
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"case 3");
				if(x.left == y)
					z = y.right;
				else
					z = y.left;
				PosBottomHalf b = z.restructure();
				y.red = false;
				x.red = true;

				// different meaning of x and y
				rbalance(r,b.right,b);
				return;
			}

			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"case 1");

			boolean oldXRed = x.red;
			PosBottomHalf b = z.restructure();
			b.left.red = false;
			b.right.red = false;
			b.red = oldXRed;
		}
		else if((y.left == null || !y.left.red)
			&& (y.right != null && y.right.red))
		{
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"case 2");
			y.red = true;
			if(x.red)
				x.red = false;
			else
				rbalance(r,x,x.parent);
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ PosTopHalf class
	class PosTopHalf implements Position
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
			synchronized(PositionManager.this)
			{
				bh.unref();
			}
		} //}}}
	} //}}}

	//{{{ PosBottomHalf class
	class PosBottomHalf
	{
		int offset;
		int ref;
		PosBottomHalf parent;
		PosBottomHalf left, right;
		boolean red;

		//{{{ PosBottomHalf constructor
		PosBottomHalf(int offset)
		{
			this.offset = offset;
		} //}}}

		//{{{ dump() method
		void dump(int level)
		{
			String ws = MiscUtilities.createWhiteSpace(level,0);
			if(left != null)
				left.dump(level+1);
			else
			{
				Log.log(Log.DEBUG,this,ws + " /]");
			}
			Log.log(Log.DEBUG,this,ws + red + ":" + offset);
			if(right != null)
				right.dump(level+1);
			else
			{
				Log.log(Log.DEBUG,this,ws + " \\]");
			}
		} //}}}

		//{{{ insert() method
		void insert(PosBottomHalf pos)
		{
			if(pos.offset < offset)
			{
				if(left == null)
				{
					pos.parent = this;
					left = pos;
				}
				else
					left.insert(pos);
			}
			else
			{
				if(right == null)
				{
					pos.parent = this;
					right = pos;
				}
				else
					right.insert(pos);
			}
		} //}}}

		//{{{ find() method
		PosBottomHalf find(int offset)
		{
			if(this.offset == offset)
				return this;
			else if(this.offset < offset)
			{
				if(right == null)
					return null;
				else
					return right.find(offset);
			}
			else
			{
				if(left == null)
					return null;
				else
					return left.find(offset);
			}
		} //}}}

		//{{{ contentInserted() method
		void contentInserted(int offset, int length)
		{
			if(offset <= this.offset)
			{
				this.offset += length;
				if(left != null)
					left.contentInserted(offset,length);
				if(right != null)
					right.contentInserted(offset,length);
			}
			else if(right != null)
				right.contentInserted(offset,length);
		} //}}}

		//{{{ contentRemoved() method
		void contentRemoved(int offset, int length)
		{
			if(offset >= this.offset)
			{
				if(right != null)
					right.contentRemoved(offset,length);
			}
			else if(offset + length <= this.offset)
			{
				this.offset -= length;
				if(left != null)
					left.contentRemoved(offset,length);
				if(right != null)
					right.contentRemoved(offset,length);
			}
			else
			{
				this.offset = offset;
				if(left != null)
					left.contentRemoved(offset,length);
				if(right != null)
					right.contentRemoved(offset,length);
			}
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

		//{{{ restructure() method
		private PosBottomHalf restructure()
		{
			// this method looks incomprehensible but really
			// its quite simple. this node, its parent and its
			// grandparent are rearranged such that in-ordering
			// of the tree is preserved, and so that the tree
			// looks like so:
			//
			//           (b)
			//         /     \
			//       (a)     (c)
			//
			// Where a/b/c are the first, second and third in an
			// in-order traversal of this node, the parent and the
			// grandparent.

			// Temporary storage: { al, a, ar, b, cl, c, cr }
			PosBottomHalf[] nodes;

			// For clarity
			PosBottomHalf u = parent.parent;

			if(u.left == parent)
			{
				if(parent.left == this)
				{
					//     u
					//    /
					//   v
					//  /
					// z
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"restructure 1");
					// zl, z, zr, v, vr, u, ur
					nodes = new PosBottomHalf[] {
						left, this, right,
						parent, parent.right,
						u, u.right
					};
				}
				else
				{
					//   u
					//  /
					// v
					//  \
					//   z
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"restructure 2");
					// vl, v, zl, z, zr, u, ur
					nodes = new PosBottomHalf[] {
						parent.left, parent, left,
						this, right, u, u.right
					};
				}
			}
			else
			{
				if(parent.right == this)
				{
					// u
					//  \
					//   v
					//    \
					//     z
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"restructure 3");
					// ul, u, vl, v, zl, z, zr
					nodes = new PosBottomHalf[] {
						u.left, u, parent.left,
						parent, left, this, right
					};
				}
				else
				{
					// u
					//  \
					//   v
					//  /
					// z
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"restructure 4");
					// ul, u, zl, z, zr, v, vr
					nodes = new PosBottomHalf[] {
						u.left, u, left, this, right,
						parent, parent.right
					};
				}
			}

			PosBottomHalf t = u.parent;
			if(t != null)
			{
				if(t.left == u)
					t.left = nodes[3];
				else
					t.right = nodes[3];
			}
			else
			{
				System.err.println("setting root to " + nodes[3]);
				root = nodes[3];
			}

			// Write-only code for constructing a meaningful tree
			nodes[1].parent = nodes[3];
			nodes[1].left   = nodes[0];
			nodes[1].right  = nodes[2];

			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"setting parent to " + t);
			nodes[3].parent = t;
			nodes[3].left   = nodes[1];
			nodes[3].right  = nodes[5];

			nodes[5].parent = nodes[3];
			nodes[5].left   = nodes[4];
			nodes[5].right  = nodes[6];

			return nodes[3];
		} //}}}

		//{{{ toString() method
		public String toString()
		{
			return red + ":" + offset;
		} //}}}
	} //}}}

	//}}}
}
