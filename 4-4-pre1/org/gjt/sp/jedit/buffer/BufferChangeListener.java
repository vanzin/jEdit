/*
 * BufferChangeListener.java - Buffer listener interface
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

import org.gjt.sp.jedit.Buffer;

/**
 * A interface for notification of changes to buffer text. While the
 * {@link org.gjt.sp.jedit.msg.BufferUpdate} EditBus message is used for
 * general buffer state changes, this interface is used for events which are
 * fired frequently, or for which performance is essential.<p>
 *
 * Because this interface is subject to change in the future, you
 * should subclass <code>BufferChangeAdapter</code> instead of
 * implementing it directly.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public interface BufferChangeListener
{
	//{{{ foldLevelChanged() method
	/**
	 * Called when line fold levels change.
	 * @param buffer The buffer in question
	 * @param startLine The start line number
	 * @param endLine The end line number
	 * @since jEdit 4.0pre1
	 */
	void foldLevelChanged(Buffer buffer, int startLine, int endLine);
	//}}}

	//{{{ contentInserted() method
	/**
	 * Called when text is inserted into the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines inserted
	 * @param length The number of characters inserted
	 * @since jEdit 4.0pre1
	 */
	void contentInserted(Buffer buffer, int startLine, int offset,
		int numLines, int length);
	//}}}

	//{{{ contentRemoved() method
	/**
	 * Called when text is removed from the buffer.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines removed
	 * @param length The number of characters removed
	 * @since jEdit 4.0pre1
	 */
	void contentRemoved(Buffer buffer, int startLine, int offset,
		int numLines, int length);
	//}}}

	//{{{ preContentRemoved() method
	/**
	 * Called when text is about to be removed from the buffer, but is
	 * still present.
	 * @param buffer The buffer in question
	 * @param startLine The first line
	 * @param offset The start offset, from the beginning of the buffer
	 * @param numLines The number of lines to be removed
	 * @param length The number of characters to be removed
	 * @since jEdit 4.2pre1
	 */
	public void preContentRemoved(Buffer buffer, int startLine, int offset,
		int numLines, int length);
	//}}}

	//{{{ transactionComplete() method
	/**
	 * Called after an undo or compound edit has finished. The text area
	 * uses this event to queue up and collapse cleanup operations so they
	 * are only run once during a long transaction (such as a "Replace All"
	 * operation.)
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.0pre6
	 */
	void transactionComplete(Buffer buffer);
	//}}}

	//{{{ foldHandlerChanged() method
	/**
	 * Called to notify the text area that folds need to be collapsed if
	 * the "collapseFolds" property is set. This method is called after the
	 * buffer has been loaded, and also if the user changes the fold
	 * handler.
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.2pre2
	 */
	void foldHandlerChanged(Buffer buffer);
	//}}}

	//{{{ foldHandlerChanged() method
	/**
	 * Called to notify the text area that the buffer has been reloaded.
	 *
	 * @param buffer The buffer in question
	 * @since jEdit 4.3pre1
	 */
	void bufferLoaded(Buffer buffer);
	//}}}
	
	//{{{ Compatibility with older jEdit plugins
	public class Adapter implements BufferListener
	{
		private BufferChangeListener delegate;

		//{{{ Adapter constructor
		public Adapter(BufferChangeListener delegate)
		{
			this.delegate = delegate;
		} //}}}
	
		//{{{ getDelegate() method
		public BufferChangeListener getDelegate()
		{
			return delegate;
		} //}}}

		//{{{ foldLevelChanged() method
		/**
		 * Called when line fold levels change.
		 * @param buffer The buffer in question
		 * @param startLine The start line number
		 * @param endLine The end line number
		 * @since jEdit 4.3pre3
		 */
		public void foldLevelChanged(JEditBuffer buffer, int startLine, int endLine)
		{
			delegate.foldLevelChanged((Buffer)buffer,startLine,endLine);
		} //}}}
	
		//{{{ contentInserted() method
		/**
		 * Called when text is inserted into the buffer.
		 * @param buffer The buffer in question
		 * @param startLine The first line
		 * @param offset The start offset, from the beginning of the buffer
		 * @param numLines The number of lines inserted
		 * @param length The number of characters inserted
		 * @since jEdit 4.3pre3
		 */
		public void contentInserted(JEditBuffer buffer, int startLine, int offset,
			int numLines, int length)
		{
			delegate.contentInserted((Buffer)buffer,startLine,offset,numLines,length);
		} //}}}
	
		//{{{ contentRemoved() method
		/**
		 * Called when text is removed from the buffer.
		 * @param buffer The buffer in question
		 * @param startLine The first line
		 * @param offset The start offset, from the beginning of the buffer
		 * @param numLines The number of lines removed
		 * @param length The number of characters removed
		 * @since jEdit 4.3pre3
		 */
		public void contentRemoved(JEditBuffer buffer, int startLine, int offset,
			int numLines, int length)
		{
			delegate.contentRemoved((Buffer)buffer,startLine,offset,numLines,length);
		} //}}}

		/**
		 * Called when text is about to be inserted in the buffer.
		 *
		 * @param buffer    The buffer in question
		 * @param startLine The first line
		 * @param offset    The start offset, from the beginning of the buffer
		 * @param numLines  The number of lines inserted
		 * @param length    The number of characters inserted
		 * @since jEdit 4.3pre11
		 */
		public void preContentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length)
		{
		}

		//{{{ preContentRemoved() method
		/**
		 * Called when text is about to be removed from the buffer, but is
		 * still present.
		 * @param buffer The buffer in question
		 * @param startLine The first line
		 * @param offset The start offset, from the beginning of the buffer
		 * @param numLines The number of lines to be removed
		 * @param length The number of characters to be removed
		 * @since jEdit 4.3pre3
		 */
		public void preContentRemoved(JEditBuffer buffer, int startLine, int offset,
			int numLines, int length)
		{
			delegate.preContentRemoved((Buffer)buffer,startLine,offset,numLines,length);
		} //}}}
	
		//{{{ transactionComplete() method
		/**
		 * Called after an undo or compound edit has finished. The text area
		 * uses this event to queue up and collapse cleanup operations so they
		 * are only run once during a long transaction (such as a "Replace All"
		 * operation.)
		 *
		 * @param buffer The buffer in question
		 * @since jEdit 4.3pre3
		 */
		public void transactionComplete(JEditBuffer buffer)
		{
			delegate.transactionComplete((Buffer)buffer);
		} //}}}
	
		//{{{ foldHandlerChanged() method
		/**
		 * Called to notify the text area that folds need to be collapsed if
		 * the "collapseFolds" property is set. This method is called after the
		 * buffer has been loaded, and also if the user changes the fold
		 * handler.
		 *
		 * @param buffer The buffer in question
		 * @since jEdit 4.3pre3
		 */
		public void foldHandlerChanged(JEditBuffer buffer)
		{
			delegate.foldHandlerChanged((Buffer)buffer);
		} //}}}
	
		//{{{ foldHandlerChanged() method
		/**
		 * Called to notify the text area that the buffer has been reloaded.
		 *
		 * @param buffer The buffer in question
		 * @since jEdit 4.3pre3
		 */
		public void bufferLoaded(JEditBuffer buffer)
		{
			delegate.bufferLoaded((Buffer)buffer);
		} //}}}
	} //}}}
}
