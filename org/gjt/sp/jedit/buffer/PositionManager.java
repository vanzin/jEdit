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
		if(root == null)
		{
			gapWidth = 0;
			return;
		}

		if(gapWidth != 0)
		{
			if(gapOffset < offset)
				root.contentInserted(gapOffset,offset,gapWidth);
			else
				root.contentInserted(offset,gapOffset,-gapWidth);
		}

		gapOffset = offset;
		gapWidth += length;
	} //}}}

	//{{{ contentRemoved() method
	public synchronized void contentRemoved(int offset, int length)
	{
		if(root == null)
		{
			gapWidth = 0;
			return;
		}

		// p1 --------------------- p2 ---------------------- p3
		// kill if bias:   ^---- false        ^----- true
		// update if bias: ^---- true         ^----- false
		boolean bias;
		int p1, p2, p3;
		if(gapWidth == 0)
		{
			p1 = offset;
			p2 = p3 = offset + length;
			bias = true;
		}
		else
		{
			if(gapOffset < offset)
			{
				p1 = gapOffset;
				p2 = offset;
				p3 = offset + length;
				bias = true;
			}
			else if(gapOffset > offset + length)
			{
				p1 = offset;
				p2 = offset + length;
				p3 = gapOffset;
				bias = false;
			}
			else
			{
				p1 = offset;
				p2 = p3 = offset + length;
				bias = false;
			}
		}

		root.contentRemoved(p1,p2,p3,gapWidth,bias);

		gapOffset = offset;
		gapWidth -= length;

	} //}}}

	//{{{ Package-private members
	/* so that PosBottomHalf can access without access$ methods */
	int gapOffset;
	int gapWidth;

	//{{{ removePosition() method
	private void removePosition(PosBottomHalf bh)
	{
		if(Debug.POSITION_DEBUG)
			Log.log(Log.DEBUG,this,"killing " + bh);
		PosBottomHalf r, x = bh.parent;

		// if one of the siblings is null, make &this=non null sibling
		if(bh.left == null)
		{
			r = bh.right;
			if(bh.parent == null)
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"simple/left: setting root to " + bh.right);
				root = bh.right;
			}
			else
			{
				if(bh == bh.parent.left)
					bh.parent.left = bh.right;
				else
					bh.parent.right = bh.right;
			}
			if(bh.right != null)
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"134: " + bh.right+":"+bh.parent);
				bh.right.parent = bh.parent;
			}
		}
		else if(bh.right == null)
		{
			r = bh.left;
			if(bh.parent == null)
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"simple/right: setting root " + bh.left);
				root = bh.left;
			}
			else
			{
				if(bh == bh.parent.left)
					bh.parent.left = bh.left;
				else
					bh.parent.right = bh.left;
			}
			if(bh.left != null)
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"155: "+bh.left+":" + bh.parent);
				bh.left.parent = bh.parent;
			}
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
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"nextInorder: setting root to " + nextInorder);
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
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"182: "+nextInorder.right+":" + nextInorder.parent);
					nextInorder.right.parent = nextInorder.parent;
				}
				nextInorder.right = bh.right;
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"186: "+nextInorder.right+":" + nextInorder);
				nextInorder.right.parent = nextInorder;
			}
			x = nextInorder.parent;
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"189: "+nextInorder+":" + bh.parent);
			nextInorder.parent = bh.parent;
			nextInorder.left = bh.left;
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"192: "+nextInorder.left+":" + nextInorder);
			nextInorder.left.parent = nextInorder;
		}

		if(!bh.red)
		{
			if(r != null && r.red)
				r.red = false;
			else if(x != null && r != null)
			{
				if(!Debug.DISABLE_POSITION_BALANCE)
					rbalance(r,x);
			}
		}

		if(Debug.POSITION_DEBUG && root != null)
			root.dump(0);
	} //}}}

	//}}}

	//{{{ Private members
	private PosBottomHalf root;

	//{{{ ibalance() method
	private void ibalance(PosBottomHalf bh)
	{
		if(bh.parent.red)
		{
			PosBottomHalf u = bh.parent.parent;
			PosBottomHalf w = bh.parent.sibling();
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
	private void rbalance(PosBottomHalf r, PosBottomHalf x)
	{
		PosBottomHalf y = r.sibling();
		PosBottomHalf z;

		if(y.red)
		{
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"case 3");
			if(x.right == y)
				z = y.right;
			else if(x.left == y)
				z = y.left;
			else
				throw new InternalError();
			PosBottomHalf b = z.restructure();
			y.red = false;
			x.red = true;

			// different meaning of x and y
			rbalance(r,r.sibling());
		}
		else
		{
			r.red = false;
			if(y.left != null && y.left.red)
				z = y.left;
			else if(y.right != null && y.right.red)
				z = y.right;
			else
			{
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"case 2");
				y.red = true;
				if(x.red)
					x.red = false;
				else if(x.parent != null)
					rbalance(x,x.parent);
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
			return bh.getOffset();
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
			if(offset >= gapOffset)
				this.offset = offset - gapWidth;
			else
				this.offset = offset;
		} //}}}

		//{{{ getOffset() method
		int getOffset()
		{
			if(offset >= gapOffset)
				return offset + gapWidth;
			else
				return offset;
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
			Log.log(Log.DEBUG,this,ws + red + ":" + getOffset());
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
			if(pos.getOffset() < getOffset())
			{
				if(left == null)
				{
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"382: "+pos+":" + this);
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
					if(Debug.POSITION_DEBUG)
						Log.log(Log.DEBUG,this,"393: "+pos+":" + this);
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
			if(getOffset() == offset)
				return this;
			else if(getOffset() < offset)
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

		//{{{ sibling() method
		PosBottomHalf sibling()
		{
			if(parent.left == this)
				return parent.right;
			else if(parent.right == this)
				return parent.left;
			else
				throw new InternalError();
		} //}}}

		//{{{ contentInserted() method
		/* update all nodes between start and end by length */
		void contentInserted(int start, int end, int length)
		{
			if(getOffset() < start)
			{
				if(right != null)
					right.contentInserted(start,end,length);
			}
			else if(getOffset() > end)
			{
				if(left != null)
					left.contentInserted(start,end,length);
			}
			else
			{
				offset += length;
				if(left != null)
					left.contentInserted(start,end,length);
				if(right != null)
					right.contentInserted(start,end,length);
			}
		} //}}}

		//{{{ contentRemoved() method
		/* if bias: kill from p1 to p2, update from p2 to p3,
		*  if !bias: update from p1 to p2, kill from p2 to p3 */
		void contentRemoved(int p1, int p2, int p3, int length,
			boolean bias)
		{
			if(getOffset() < p1)
			{
				if(right != null)
					right.contentRemoved(p1,p2,p3,length,bias);
			}
			else if(getOffset() > p3)
			{
				if(left != null)
					left.contentRemoved(p1,p2,p3,length,bias);
			}
			else
			{
				// recall from above:
				// p1 --------------------- p2 ---------------------- p3
				// kill if bias:   ^---- false        ^----- true
				// update if bias: ^---- true         ^----- false
				if(bias)
				{
					if(getOffset() > p2)
					{
						if(p2 == p3)
							offset = p2 + length;
						else
							offset = p2;
					}
					else
					{
						offset += length;
					}
				}
				else
				{
					if(getOffset() < p2)
						offset = p1;
					else
						offset -= length;
				}

				if(left != null)
					left.contentRemoved(p1,p2,p3,length,bias);
				if(right != null)
					right.contentRemoved(p1,p2,p3,length,bias);
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
				if(Debug.POSITION_DEBUG)
					Log.log(Log.DEBUG,this,"restructure: setting root to " + nodes[3]);
				root = nodes[3];
			}

			// Write-only code for constructing a meaningful tree
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"583: "+nodes[1]+":" + nodes[3]);
			nodes[1].parent = nodes[3];
			nodes[1].left   = nodes[0];
			nodes[1].right  = nodes[2];

			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"setting parent to " + t);
			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"590: " + nodes[3]+":" + t);
			nodes[3].parent = t;
			nodes[3].left   = nodes[1];
			nodes[3].right  = nodes[5];

			if(Debug.POSITION_DEBUG)
				Log.log(Log.DEBUG,this,"595: "+nodes[5]+":" + nodes[3]);
			nodes[5].parent = nodes[3];
			nodes[5].left   = nodes[4];
			nodes[5].right  = nodes[6];

			return nodes[3];
		} //}}}

		//{{{ toString() method
		public String toString()
		{
			return red + ":" + getOffset();
		} //}}}
	} //}}}

	//}}}
}
