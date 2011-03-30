/*
 * JEditBuffer.java - jEdit buffer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2005 Slava Pestov
 * Portions copyright (C) 1999, 2000 mike dillon
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
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.indent.IndentAction;
import org.gjt.sp.jedit.indent.IndentRule;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.ColumnBlock;
import org.gjt.sp.jedit.textarea.ColumnBlockLine;
import org.gjt.sp.jedit.textarea.Node;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import javax.swing.text.Position;
import javax.swing.text.Segment;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
//}}}

/**
 * A <code>JEditBuffer</code> represents the contents of an open text
 * file as it is maintained in the computer's memory (as opposed to
 * how it may be stored on a disk).<p>
 *
 * This class is partially thread-safe, however you must pay attention to two
 * very important guidelines:
 * <ul>
 * <li>Operations such as insert() and remove(),
 * undo(), change Buffer data in a writeLock(), and must
 * be called from the AWT thread.
 * <li>When accessing the buffer from another thread, you must
 * call readLock() before and readUnLock() after,  if you plan on performing
 * more than one read, to ensure that  the buffer contents are not changed by
 * the AWT thread for the duration of the lock. Only methods whose descriptions
 * specify thread safety can be invoked from other threads.
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 4.3pre3
 */
public class JEditBuffer
{
	/**
	 * Line separator property.
	 */
	public static final String LINESEP = "lineSeparator";

	/**
	 * Character encoding used when loading and saving.
	 * @since jEdit 3.2pre4
	 */
	public static final String ENCODING = "encoding";

	//{{{ JEditBuffer constructors
	public JEditBuffer(Map props)
	{
		bufferListeners = new Vector<Listener>();
		lock = new ReentrantReadWriteLock();
		contentMgr = new ContentManager();
		lineMgr = new LineManager();
		positionMgr = new PositionManager(this);
		undoMgr = new UndoManager(this);
		integerArray = new IntegerArray();
		propertyLock = new Object();
		properties = new HashMap<Object, PropValue>();

		//{{{ need to convert entries of 'props' to PropValue instances
		Set<Map.Entry> set = props.entrySet();
		for (Map.Entry entry : set)
		{
			properties.put(entry.getKey(),new PropValue(entry.getValue(),false));
		} //}}}

		// fill in defaults for these from system properties if the
		// corresponding buffer.XXX properties not set
		if(getProperty(ENCODING) == null)
			properties.put(ENCODING,new PropValue(System.getProperty("file.encoding"),false));
		if(getProperty(LINESEP) == null)
			properties.put(LINESEP,new PropValue(System.getProperty("line.separator"),false));
	}

	/**
	 * Create a new JEditBuffer.
	 * It is used by independent textarea only
	 */
	public JEditBuffer()
	{
		bufferListeners = new Vector<Listener>();
		lock = new ReentrantReadWriteLock();
		contentMgr = new ContentManager();
		lineMgr = new LineManager();
		positionMgr = new PositionManager(this);
		undoMgr = new UndoManager(this);
		integerArray = new IntegerArray();
		propertyLock = new Object();
		properties = new HashMap<Object, PropValue>();

		properties.put("wrap",new PropValue("none",false));
		properties.put("folding",new PropValue("none",false));
		tokenMarker = new TokenMarker();
		tokenMarker.addRuleSet(new ParserRuleSet("text","MAIN"));
		setTokenMarker(tokenMarker);

		loadText(null,null);
		// corresponding buffer.XXX properties not set
		if(getProperty(ENCODING) == null)
			properties.put(ENCODING,new PropValue(System.getProperty("file.encoding"),false));
		if(getProperty(LINESEP) == null)
			properties.put(LINESEP,new PropValue(System.getProperty("line.separator"),false));

		setFoldHandler(new DummyFoldHandler());
	} //}}}

	//{{{ Flags

	//{{{ isDirty() method
	/**
	 * Returns whether there have been unsaved changes to this buffer.
	 * This method is thread-safe.
	 */
	public boolean isDirty()
	{
		return dirty;
	} //}}}

	//{{{ isLoading() method
	public boolean isLoading()
	{
		return loading;
	} //}}}

	//{{{ setLoading() method
	public void setLoading(boolean loading)
	{
		this.loading = loading;
	} //}}}

	//{{{ isPerformingIO() method
	/**
	 * Returns true if the buffer is currently performing I/O.
	 * This method is thread-safe.
	 * @since jEdit 2.7pre1
	 */
	public boolean isPerformingIO()
	{
		return isLoading() || io;
	} //}}}

	//{{{ setPerformingIO() method
	/**
	 * Returns true if the buffer is currently performing I/O.
	 * This method is thread-safe.
	 * @since jEdit 2.7pre1
	 */
	public void setPerformingIO(boolean io)
	{
		this.io = io;
	} //}}}

	//{{{ isEditable() method
	/**
	 * Returns true if this file is editable, false otherwise. A file may
	 * become uneditable if it is read only, or if I/O is in progress.
	 * This method is thread-safe.
	 * @since jEdit 2.7pre1
	 */
	public boolean isEditable()
	{
		return !(isReadOnly() || isPerformingIO());
	} //}}}

	//{{{ isReadOnly() method
	/**
	 * Returns true if this file is read only, false otherwise.
	 * This method is thread-safe.
	 */
	public boolean isReadOnly()
	{
		return readOnly || readOnlyOverride;
	} //}}}

	//{{{ setReadOnly() method
	/**
	 * Sets the read only flag.
	 * @param readOnly The read only flag
	 */
	public void setReadOnly(boolean readOnly)
	{
		readOnlyOverride = readOnly;
	} //}}}

	//{{{ setDirty() method
	/**
	 * Sets the 'dirty' (changed since last save) flag of this buffer.
	 */
	public void setDirty(boolean d)
	{
		boolean editable = isEditable();

		if(d)
		{
			if(editable)
				dirty = true;
		}
		else
		{
			dirty = false;

			// fixes dirty flag not being reset on
			// save/insert/undo/redo/undo
			if(!isUndoInProgress())
			{
				// this ensures that undo can clear the dirty flag properly
				// when all edits up to a save are undone
				undoMgr.resetClearDirty();
			}
		}
	} //}}}

	//}}}

	//{{{ Thread safety

	//{{{ readLock() method
	/**
	 * The buffer is guaranteed not to change between calls to
	 * {@link #readLock()} and {@link #readUnlock()}.
	 */
	public void readLock()
	{
		lock.readLock().lock();
	} //}}}

	//{{{ readUnlock() method
	/**
	 * The buffer is guaranteed not to change between calls to
	 * {@link #readLock()} and {@link #readUnlock()}.
	 */
	public void readUnlock()
	{
		lock.readLock().unlock();
	} //}}}

	//{{{ writeLock() method
	/**
	 * Attempting to obtain read lock will block between calls to
	 * {@link #writeLock()} and {@link #writeUnlock()}.
	 */
	public void writeLock()
	{
		lock.writeLock().lock();
	} //}}}

	//{{{ writeUnlock() method
	/**
	 * Attempting to obtain read lock will block between calls to
	 * {@link #writeLock()} and {@link #writeUnlock()}.
	 */
	public void writeUnlock()
	{
		lock.writeLock().unlock();
	} //}}}

	//}}}

	//{{{ Line offset methods

	//{{{ getLength() method
	/**
	 * Returns the number of characters in the buffer. This method is thread-safe.
	 */
	public int getLength()
	{
		// no need to lock since this just returns a value and that's it
		return contentMgr.getLength();
	} //}}}

	//{{{ getLineCount() method
	/**
	 * Returns the number of physical lines in the buffer.
	 * This method is thread-safe.
	 * @since jEdit 3.1pre1
	 */
	public int getLineCount()
	{
		// no need to lock since this just returns a value and that's it
		return lineMgr.getLineCount();
	} //}}}

	//{{{ getLineOfOffset() method
	/**
	 * Returns the line containing the specified offset.
	 * This method is thread-safe.
	 * @param offset The offset
	 * @since jEdit 4.0pre1
	 */
	public int getLineOfOffset(int offset)
	{
		try
		{
			readLock();

			if(offset < 0 || offset > getLength())
				throw new ArrayIndexOutOfBoundsException(offset);

			return lineMgr.getLineOfOffset(offset);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getLineStartOffset() method
	/**
	 * Returns the start offset of the specified line.
	 * This method is thread-safe.
	 * @param line The line
	 * @return The start offset of the specified line
	 * @since jEdit 4.0pre1
	 */
	public int getLineStartOffset(int line)
	{
		try
		{
			readLock();

			if(line < 0 || line >= lineMgr.getLineCount())
				throw new ArrayIndexOutOfBoundsException(line);
			else if(line == 0)
				return 0;

			return lineMgr.getLineEndOffset(line - 1);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getLineEndOffset() method
	/**
	 * Returns the end offset of the specified line.
	 * This method is thread-safe.
	 * @param line The line
	 * @return The end offset of the specified line
	 * invalid.
	 * @since jEdit 4.0pre1
	 */
	public int getLineEndOffset(int line)
	{
		try
		{
			readLock();

			if(line < 0 || line >= lineMgr.getLineCount())
				throw new ArrayIndexOutOfBoundsException(line);

			return lineMgr.getLineEndOffset(line);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getLineLength() method
	/**
	 * Returns the length of the specified line.
	 * This method is thread-safe.
	 * @param line The line
	 * @since jEdit 4.0pre1
	 */
	public int getLineLength(int line)
	{
		try
		{
			readLock();

			return getLineEndOffset(line)
				- getLineStartOffset(line) - 1;
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getPriorNonEmptyLine() method
	/**
	 * Auto indent needs this.
	 */
	public int getPriorNonEmptyLine(int lineIndex)
	{
		if (!mode.getIgnoreWhitespace())
		{
			return lineIndex - 1;
		}

		int returnValue = -1;
		for(int i = lineIndex - 1; i >= 0; i--)
		{
			Segment seg = new Segment();
			getLineText(i,seg);
			if(seg.count != 0)
				returnValue = i;
			for(int j = 0; j < seg.count; j++)
			{
				char ch = seg.array[seg.offset + j];
				if(!Character.isWhitespace(ch))
					return i;
			}
		}

		// didn't find a line that contains non-whitespace chars
		// so return index of prior whitespace line
		return returnValue;
	} //}}}

	//}}}

	//{{{ Text getters and setters

	//{{{ getLineText() methods
	/**
	 * Returns the text on the specified line.
	 * This method is thread-safe.
	 * @param line The line
	 * @return The text, or null if the line is invalid
	 * @since jEdit 4.0pre1
	 */
	public String getLineText(int line)
	{
		if(line < 0 || line >= lineMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		try
		{
			readLock();

			int start = line == 0 ? 0 : lineMgr.getLineEndOffset(line - 1);
			int end = lineMgr.getLineEndOffset(line);

			return getText(start,end - start - 1);
		}
		finally
		{
			readUnlock();
		}
	}

	/**
	 * Returns the specified line in a <code>Segment</code>.<p>
	 *
	 * Using a <classname>Segment</classname> is generally more
	 * efficient than using a <classname>String</classname> because it
	 * results in less memory allocation and array copying.<p>
	 *
	 * This method is thread-safe.
	 *
	 * @param line The line
	 * @since jEdit 4.0pre1
	 */
	public void getLineText(int line, Segment segment)
	{
		getLineText(line, 0, segment);
	}

	/**
	 * Returns the specified line from the starting point passed in relativeStartOffset  in a <code>Segment</code>.<p>
	 *
	 * Using a <classname>Segment</classname> is generally more
	 * efficient than using a <classname>String</classname> because it
	 * results in less memory allocation and array copying.<p>
	 *
	 * This method is thread-safe.
	 *
	 * @param line The line
	 * @since jEdit 4.0pre1
	 */
	public void getLineText(int line,int relativeStartOffset, Segment segment)
	{
		if(line < 0 || line >= lineMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		try
		{
			readLock();

			int start = (line == 0 ? 0 : lineMgr.getLineEndOffset(line - 1)); 
			int end = lineMgr.getLineEndOffset(line);
			if((start+relativeStartOffset)>end)
			{
				throw new IllegalArgumentException("This index is outside the line length (start+relativeOffset):"+start+" + "+relativeStartOffset+" > "+"endffset:"+end);
			}
			else
			{	
				getText(start+relativeStartOffset,end - start -relativeStartOffset- 1,segment);
			}	
		}
		finally
		{
			readUnlock();
		}
	} //}}}
	
	//}}}
	//{{{ getLineSegment() method
	/**
	 * Returns the text on the specified line.
	 * This method is thread-safe.
	 *
	 * @param line The line index.
	 * @return The text, or null if the line is invalid
	 *
	 * @since jEdit 4.3pre15
	 */
	public CharSequence getLineSegment(int line)
	{
		if(line < 0 || line >= lineMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		try
		{
			readLock();

			int start = line == 0 ? 0 : lineMgr.getLineEndOffset(line - 1);
			int end = lineMgr.getLineEndOffset(line);

			return getSegment(start,end - start - 1);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getText() methods
	/**
	 * Returns the specified text range. This method is thread-safe.
	 * @param start The start offset
	 * @param length The number of characters to get
	 */
	public String getText(int start, int length)
	{
		try
		{
			readLock();

			if(start < 0 || length < 0
				|| start + length > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);

			return contentMgr.getText(start,length);
		}
		finally
		{
			readUnlock();
		}
	}

	/**
	 * Returns the full buffer content. This method is thread-safe
	 * @since 4.4.1
	 */
	public String getText()
	{
		try
		{
			readLock();
			return contentMgr.getText(0, getLength());
		}
		finally
		{
			readUnlock();
		}
	}

	/**
	 * Returns the specified text range in a <code>Segment</code>.<p>
	 *
	 * Using a <classname>Segment</classname> is generally more
	 * efficient than using a <classname>String</classname> because it
	 * results in less memory allocation and array copying.<p>
	 *
	 * This method is thread-safe.
	 *
	 * @param start The start offset
	 * @param length The number of characters to get
	 * @param seg The segment to copy the text to
	 */
	public void getText(int start, int length, Segment seg)
	{
		try
		{
			readLock();

			if(start < 0 || length < 0
				|| start + length > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);

			contentMgr.getText(start,length,seg);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getSegment() method
	/**
	 * Returns the specified text range. This method is thread-safe.
	 * It doesn't copy the text
	 *
	 * @param start The start offset
	 * @param length The number of characters to get
	 *
	 * @return a CharSequence that contains the text wanted text
	 * @since jEdit 4.3pre15
	 */
	public CharSequence getSegment(int start, int length)
	{
		try
		{
			readLock();

			if(start < 0 || length < 0
				|| start + length > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);

			return contentMgr.getSegment(start,length);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ insert() methods
	/**
	 * Inserts a string into the buffer.
	 * @param offset The offset
	 * @param str The string
	 * @since jEdit 4.0pre1
	 */
	public void insert(int offset, String str)
	{
		if(str == null)
			return;

		int len = str.length();

		if(len == 0)
			return;

		if(isReadOnly())
			throw new RuntimeException("buffer read-only");

		try
		{
			writeLock();

			if(offset < 0 || offset > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(offset);

			contentMgr.insert(offset,str);

			integerArray.clear();

			for(int i = 0; i < len; i++)
			{
				if(str.charAt(i) == '\n')
					integerArray.add(i + 1);
			}

			if(!undoInProgress)
			{
				undoMgr.contentInserted(offset,len,str,!dirty);
			}

			contentInserted(offset,len,integerArray);
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Inserts a string into the buffer.
	 * @param offset The offset
	 * @param seg The segment
	 * @since jEdit 4.0pre1
	 */
	public void insert(int offset, Segment seg)
	{
		if(seg.count == 0)
			return;

		if(isReadOnly())
			throw new RuntimeException("buffer read-only");

		try
		{
			writeLock();

			if(offset < 0 || offset > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(offset);

			contentMgr.insert(offset,seg);

			integerArray.clear();

			for(int i = 0; i < seg.count; i++)
			{
				if(seg.array[seg.offset + i] == '\n')
					integerArray.add(i + 1);
			}

			if(!undoInProgress)
			{
				undoMgr.contentInserted(offset,seg.count,
					seg.toString(),!dirty);
			}

			contentInserted(offset,seg.count,integerArray);
		}
		finally
		{
			writeUnlock();
		}
	} //}}}

	//{{{ remove() method
	/**
	 * Removes the specified rang efrom the buffer.
	 * @param offset The start offset
	 * @param length The number of characters to remove
	 */
	public void remove(int offset, int length)
	{
		if(length == 0)
			return;

		if(isReadOnly())
			throw new RuntimeException("buffer read-only");

		try
		{
			transaction = true;

			writeLock();

			if(offset < 0 || length < 0
				|| offset + length > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(offset + ":" + length);

			int startLine = lineMgr.getLineOfOffset(offset);
			int endLine = lineMgr.getLineOfOffset(offset + length);

			int numLines = endLine - startLine;

			if(!undoInProgress && !loading)
			{
				undoMgr.contentRemoved(offset,length,
					getText(offset,length),
					!dirty);
			}

			firePreContentRemoved(startLine,offset,numLines,length);

			contentMgr.remove(offset,length);
			lineMgr.contentRemoved(startLine,offset,numLines,length);
			positionMgr.contentRemoved(offset,length);

			setDirty(true);

			fireContentRemoved(startLine,offset,numLines,length);

			/* otherwise it will be delivered later */
			if(!undoInProgress && !insideCompoundEdit())
				fireTransactionComplete();

		}
		finally
		{
			transaction = false;

			writeUnlock();
		}
	} //}}}

	//}}}

	//{{{ Indentation

	//{{{ removeTrailingWhiteSpace() method
	/**
	 * Removes trailing whitespace from all lines in the specified list.
	 * @param lines The line numbers
	 * @since jEdit 3.2pre1
	 */
	public void removeTrailingWhiteSpace(int[] lines)
	{
		try
		{
			beginCompoundEdit();

			for(int i = 0; i < lines.length; i++)
			{
				Segment seg = new Segment();
				getLineText(lines[i],seg);

				// blank line
				if (seg.count == 0) continue;

				int lineStart = seg.offset;
				int lineEnd = seg.offset + seg.count - 1;

				int pos;
				for (pos = lineEnd; pos >= lineStart; pos--)
				{
					if (!Character.isWhitespace(seg.array[pos]))
						break;
				}

				int tail = lineEnd - pos;

				// no whitespace
				if (tail == 0) continue;

				remove(getLineEndOffset(lines[i]) - 1 - tail,tail);
			}
		}
		finally
		{
			endCompoundEdit();
		}
	} //}}}

	//{{{ shiftIndentLeft() method
	/**
	 * Shifts the indent of each line in the specified list to the left.
	 * @param lines The line numbers
	 * @since jEdit 3.2pre1
	 */
	public void shiftIndentLeft(int[] lines)
	{
		int tabSize = getTabSize();
		int indentSize = getIndentSize();
		boolean noTabs = getBooleanProperty("noTabs");

		try
		{
			beginCompoundEdit();

			for(int i = 0; i < lines.length; i++)
			{
				int lineStart = getLineStartOffset(lines[i]);
				CharSequence line = getLineSegment(lines[i]);
				int whiteSpace = StandardUtilities
					.getLeadingWhiteSpace(line);
				if(whiteSpace == 0)
					continue;
				int whiteSpaceWidth = Math.max(0,StandardUtilities
					.getLeadingWhiteSpaceWidth(line,tabSize)
					- indentSize);

				insert(lineStart + whiteSpace,StandardUtilities
					.createWhiteSpace(whiteSpaceWidth,
					noTabs ? 0 : tabSize));
				remove(lineStart,whiteSpace);
			}

		}
		finally
		{
			endCompoundEdit();
		}
	} //}}}

	//{{{ shiftIndentRight() method
	/**
	 * Shifts the indent of each line in the specified list to the right.
	 * @param lines The line numbers
	 * @since jEdit 3.2pre1
	 */
	public void shiftIndentRight(int[] lines)
	{
		try
		{
			beginCompoundEdit();

			int tabSize = getTabSize();
			int indentSize = getIndentSize();
			boolean noTabs = getBooleanProperty("noTabs");
			for(int i = 0; i < lines.length; i++)
			{
				int lineStart = getLineStartOffset(lines[i]);
				CharSequence line = getLineSegment(lines[i]);
				int whiteSpace = StandardUtilities
					.getLeadingWhiteSpace(line);

				// silly usability hack
				//if(lines.length != 1 && whiteSpace == 0)
				//	continue;

				int whiteSpaceWidth = StandardUtilities
					.getLeadingWhiteSpaceWidth(
					line,tabSize) + indentSize;
				insert(lineStart + whiteSpace,StandardUtilities
					.createWhiteSpace(whiteSpaceWidth,
					noTabs ? 0 : tabSize));
				remove(lineStart,whiteSpace);
			}
		}
		finally
		{
			endCompoundEdit();
		}
	} //}}}

	//{{{ indentLines() methods
	/**
	 * Indents all specified lines.
	 * @param start The first line to indent
	 * @param end The last line to indent
	 * @since jEdit 3.1pre3
	 */
	public void indentLines(int start, int end)
	{
		try
		{
			beginCompoundEdit();
			for(int i = start; i <= end; i++)
				indentLine(i,true);
		}
		finally
		{
			endCompoundEdit();
		}
	}

	/**
	 * Indents all specified lines.
	 * @param lines The line numbers
	 * @since jEdit 3.2pre1
	 */
	public void indentLines(int[] lines)
	{
		try
		{
			beginCompoundEdit();
			for(int i = 0; i < lines.length; i++)
				indentLine(lines[i],true);
		}
		finally
		{
			endCompoundEdit();
		}
	} //}}}

	//{{{ indentLine() methods
	/**
	 * Indents the specified line.
	 * @param lineIndex The line number to indent
	 * @param canDecreaseIndent If true, the indent can be decreased as a
	 * result of this. Set this to false for Tab key.
	 * @return true If indentation took place, false otherwise.
	 * @since jEdit 4.2pre2
	 */
	public boolean indentLine(int lineIndex, boolean canDecreaseIndent)
	{
		int[] whitespaceChars = new int[1];
		int currentIndent = getCurrentIndentForLine(lineIndex,
			whitespaceChars);
		int prevLineIndex = getPriorNonEmptyLine(lineIndex);
		int prevLineIndent = (prevLineIndex == -1) ? 0 :
			StandardUtilities.getLeadingWhiteSpaceWidth(getLineSegment(
				prevLineIndex), getTabSize());
		int idealIndent = getIdealIndentForLine(lineIndex, prevLineIndex,
			prevLineIndent);

		if (idealIndent == -1 || idealIndent == currentIndent ||
			(!canDecreaseIndent && idealIndent < currentIndent))
			return false;

		// Do it
		try
		{
			beginCompoundEdit();

			int start = getLineStartOffset(lineIndex);

			remove(start,whitespaceChars[0]);
			String prevIndentString = (prevLineIndex >= 0) ?
				StandardUtilities.getIndentString(getLineText(
					prevLineIndex)) : null;
			String indentString;
			if (prevIndentString == null)
			{
				indentString = StandardUtilities.createWhiteSpace(
					idealIndent,
					getBooleanProperty("noTabs") ? 0 : getTabSize());
			}
			else if (idealIndent == prevLineIndent)
				indentString = prevIndentString;
			else if (idealIndent < prevLineIndent)
				indentString = StandardUtilities.truncateWhiteSpace(
					idealIndent, getTabSize(), prevIndentString);
			else
				indentString = prevIndentString +
					StandardUtilities.createWhiteSpace(
						idealIndent - prevLineIndent,
						getBooleanProperty("noTabs") ? 0 : getTabSize(),
						prevLineIndent);
			insert(start, indentString);
		}
		finally
		{
			endCompoundEdit();
		}

		return true;
	} //}}}

	//{{{ getCurrentIndentForLine() method
	/**
	 * Returns the line's current leading indent.
	 * @param lineIndex The line number
	 * @param whitespaceChars If this is non-null, the number of whitespace
	 * characters is stored at the 0 index
	 * @since jEdit 4.2pre2
	 */
	public int getCurrentIndentForLine(int lineIndex, int[] whitespaceChars)
	{
		Segment seg = new Segment();
		getLineText(lineIndex,seg);

		int tabSize = getTabSize();

		int currentIndent = 0;
loop:		for(int i = 0; i < seg.count; i++)
		{
			char c = seg.array[seg.offset + i];
			switch(c)
			{
			case ' ':
				currentIndent++;
				if(whitespaceChars != null)
					whitespaceChars[0]++;
				break;
			case '\t':
				currentIndent += tabSize - (currentIndent
					% tabSize);
				if(whitespaceChars != null)
					whitespaceChars[0]++;
				break;
			default:
				break loop;
			}
		}

		return currentIndent;
	} //}}}

	//{{{ getIdealIndentForLine() method
	/**
	 * Returns the ideal leading indent for the specified line.
	 * This will apply the various auto-indent rules.
	 * @param lineIndex The line number
	 */
	public int getIdealIndentForLine(int lineIndex)
	{
		int prevLineIndex = getPriorNonEmptyLine(lineIndex);
		int oldIndent = prevLineIndex == -1 ? 0 :
			StandardUtilities.getLeadingWhiteSpaceWidth(
			getLineSegment(prevLineIndex),
			getTabSize());
		return getIdealIndentForLine(lineIndex, prevLineIndex,
			oldIndent);
	} //}}}

	//{{{ getIdealIndentForLine() method
	/**
	 * Returns the ideal leading indent for the specified line.
	 * This will apply the various auto-indent rules.
	 * @param lineIndex The line number
	 * @param prevLineIndex The index of the previous non-empty line
	 * @param oldIndent The indent width of the previous line (or 0)
	 */
	private int getIdealIndentForLine(int lineIndex, int prevLineIndex,
		int oldIndent)
	{
		int prevPrevLineIndex = prevLineIndex < 0 ? -1
			: getPriorNonEmptyLine(prevLineIndex);
		int newIndent = oldIndent;

		List<IndentRule> indentRules = getIndentRules(lineIndex);
		List<IndentAction> actions = new LinkedList<IndentAction>();
		for (int i = 0;i<indentRules.size();i++)
		{
			IndentRule rule = indentRules.get(i);
			rule.apply(this,lineIndex,prevLineIndex,
				prevPrevLineIndex,actions);
		}


		for (IndentAction action : actions)
		{
			newIndent = action.calculateIndent(this, lineIndex,
					oldIndent, newIndent);
			if (!action.keepChecking())
				break;
		}
		if (newIndent < 0)
			newIndent = 0;

		return newIndent;
	} //}}}

	//{{{ getVirtualWidth() method
	/**
	 * Returns the virtual column number (taking tabs into account) of the
	 * specified position.
	 *
	 * @param line The line number
	 * @param column The column number
	 * @since jEdit 4.1pre1
	 */
	public int getVirtualWidth(int line, int column)
	{
		try
		{
			readLock();

			int start = getLineStartOffset(line);
			Segment seg = new Segment();
			getText(start,column,seg);

			return StandardUtilities.getVirtualWidth(seg,getTabSize());
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ getOffsetOfVirtualColumn() method
	/**
	 * Returns the offset of a virtual column number (taking tabs
	 * into account) relative to the start of the line in question.
	 *
	 * @param line The line number
	 * @param column The virtual column number
	 * @param totalVirtualWidth If this array is non-null, the total
	 * virtual width will be stored in its first location if this method
	 * returns -1.
	 *
	 * @return -1 if the column is out of bounds
	 *
	 * @since jEdit 4.1pre1
	 */
	public int getOffsetOfVirtualColumn(int line, int column,
		int[] totalVirtualWidth)
	{
		try
		{
			readLock();

			Segment seg = new Segment();
			getLineText(line,seg);

			return StandardUtilities.getOffsetOfVirtualColumn(seg,
				getTabSize(),column,totalVirtualWidth);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//{{{ insertAtColumn() method
	/**
	 * Like the {@link #insert(int,String)} method, but inserts the string at
	 * the specified virtual column. Inserts spaces as appropriate if
	 * the line is shorter than the column.
	 * @param line The line number
	 * @param col The virtual column number
	 * @param str The string
	 */
	public void insertAtColumn(int line, int col, String str)
	{
		try
		{
			writeLock();

			int[] total = new int[1];
			int offset = getOffsetOfVirtualColumn(line,col,total);
			if(offset == -1)
			{
				offset = getLineEndOffset(line) - 1;
				str = StandardUtilities.createWhiteSpace(col - total[0],0) + str;
			}
			else
				offset += getLineStartOffset(line);

			insert(offset,str);
		}
		finally
		{
			writeUnlock();
		}
	} //}}}

	//{{{ insertIndented() method
	/**
	 * Inserts a string into the buffer, indenting each line of the string
	 * to match the indent of the first line.
	 *
	 * @param offset The offset
	 * @param text The text
	 *
	 * @return The number of characters of indent inserted on each new
	 * line. This is used by the abbreviations code.
	 *
	 * @since jEdit 4.2pre14
	 */
	public int insertIndented(int offset, String text)
	{
		try
		{
			beginCompoundEdit();

			// obtain the leading indent for later use
			int firstLine = getLineOfOffset(offset);
			CharSequence lineText = getLineSegment(firstLine);
			int leadingIndent
				= StandardUtilities.getLeadingWhiteSpaceWidth(
				lineText,getTabSize());

			String whiteSpace = StandardUtilities.createWhiteSpace(
				leadingIndent,getBooleanProperty("noTabs")
				? 0 : getTabSize());

			insert(offset,text);

			int lastLine = getLineOfOffset(offset + text.length());

			// note that if firstLine == lastLine, loop does not
			// execute
			for(int i = firstLine + 1; i <= lastLine; i++)
			{
				insert(getLineStartOffset(i),whiteSpace);
			}

			return whiteSpace.length();
		}
		finally
		{
			endCompoundEdit();
		}
	} //}}}

	//{{{ isElectricKey() methods
	/**
	 * Should inserting this character trigger a re-indent of
	 * the current line?
	 * @since jEdit 4.3pre9
	 */
	public boolean isElectricKey(char ch, int line)
	{
		TokenMarker.LineContext ctx = lineMgr.getLineContext(line);
		Mode mode = ModeProvider.instance.getMode(ctx.rules.getModeName());

		// mode can be null, though that's probably an error "further up":
		if (mode == null)
			return false;
		return mode.isElectricKey(ch);
	} //}}}

	//}}}

	//{{{ Syntax highlighting

	//{{{ markTokens() method
	/**
	 * Returns the syntax tokens for the specified line.
	 * @param lineIndex The line number
	 * @param tokenHandler The token handler that will receive the syntax
	 * tokens
	 * @since jEdit 4.1pre1
	 */
	public void markTokens(int lineIndex, TokenHandler tokenHandler)
	{
		Segment seg = new Segment();

		if(lineIndex < 0 || lineIndex >= lineMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(lineIndex);

		int firstInvalidLineContext = lineMgr.getFirstInvalidLineContext();
		int start;
		if(contextInsensitive || firstInvalidLineContext == -1)
		{
			start = lineIndex;
		}
		else
		{
			start = Math.min(firstInvalidLineContext,
				lineIndex);
		}

		if(Debug.TOKEN_MARKER_DEBUG)
			Log.log(Log.DEBUG,this,"tokenize from " + start + " to " + lineIndex);
		TokenMarker.LineContext oldContext = null;
		TokenMarker.LineContext context = null;
		for(int i = start; i <= lineIndex; i++)
		{
			getLineText(i,seg);

			oldContext = lineMgr.getLineContext(i);

			TokenMarker.LineContext prevContext = (
				(i == 0 || contextInsensitive) ? null
				: lineMgr.getLineContext(i - 1)
			);

			context = tokenMarker.markTokens(prevContext,
				(i == lineIndex ? tokenHandler
				: DummyTokenHandler.INSTANCE), seg);
			lineMgr.setLineContext(i,context);
		}

		int lineCount = lineMgr.getLineCount();
		if(lineCount - 1 == lineIndex)
			lineMgr.setFirstInvalidLineContext(-1);
		else if(oldContext != context)
			lineMgr.setFirstInvalidLineContext(lineIndex + 1);
		else if(firstInvalidLineContext == -1)
			/* do nothing */;
		else
		{
			lineMgr.setFirstInvalidLineContext(Math.max(
				firstInvalidLineContext,lineIndex + 1));
		}
	} //}}}

	//{{{ getTokenMarker() method
	public TokenMarker getTokenMarker()
	{
		return tokenMarker;
	} //}}}

	//{{{ setTokenMarker() method
	public void setTokenMarker(TokenMarker tokenMarker)
	{
		TokenMarker oldTokenMarker = this.tokenMarker;

		this.tokenMarker = tokenMarker;

		// don't do this on initial token marker
		if(oldTokenMarker != null && tokenMarker != oldTokenMarker)
		{
			lineMgr.setFirstInvalidLineContext(0);
		}
	} //}}}

	//{{{ createPosition() method
	/**
	 * Creates a floating position.
	 * @param offset The offset
	 */
	public Position createPosition(int offset)
	{
		try
		{
			readLock();

			if(offset < 0 || offset > contentMgr.getLength())
				throw new ArrayIndexOutOfBoundsException(offset);

			return positionMgr.createPosition(offset);
		}
		finally
		{
			readUnlock();
		}
	} //}}}

	//}}}

	//{{{ Property methods

	//{{{ propertiesChanged() method
	/**
	 * Reloads settings from the properties. This should be called
	 * after the <code>syntax</code> or <code>folding</code>
	 * buffer-local properties are changed.
	 */
	public void propertiesChanged()
	{
		String folding = getStringProperty("folding");
		FoldHandler handler = FoldHandler.getFoldHandler(folding);

		if(handler != null)
		{
			setFoldHandler(handler);
		}
		else
		{
			setFoldHandler(new DummyFoldHandler());
		}
	} //}}}

	//{{{ getTabSize() method
	/**
	 * Returns the tab size used in this buffer. This is equivalent
	 * to calling <code>getProperty("tabSize")</code>.
	 * This method is thread-safe.
	 */
	public int getTabSize()
	{
		int tabSize = getIntegerProperty("tabSize",8);
		if(tabSize <= 0)
			return 8;
		else
			return tabSize;
	} //}}}

	//{{{ getIndentSize() method
	/**
	 * Returns the indent size used in this buffer. This is equivalent
	 * to calling <code>getProperty("indentSize")</code>.
	 * This method is thread-safe.
	 * @since jEdit 2.7pre1
	 */
	public int getIndentSize()
	{
		int indentSize = getIntegerProperty("indentSize",8);
		if(indentSize <= 0)
			return 8;
		else
			return indentSize;
	} //}}}

	//{{{ getProperty() method
	/**
	 * Returns the value of a buffer-local property.<p>
	 *
	 * Using this method is generally discouraged, because it returns an
	 * <code>Object</code> which must be cast to another type
	 * in order to be useful, and this can cause problems if the object
	 * is of a different type than what the caller expects.<p>
	 *
	 * The following methods should be used instead:
	 * <ul>
	 * <li>{@link #getStringProperty(String)}</li>
	 * <li>{@link #getBooleanProperty(String)}</li>
	 * <li>{@link #getIntegerProperty(String,int)}</li>
	 * </ul>
	 *
	 * This method is thread-safe.
	 *
	 * @param name The property name. For backwards compatibility, this
	 * is an <code>Object</code>, not a <code>String</code>.
	 */
	public Object getProperty(Object name)
	{
		synchronized(propertyLock)
		{
			// First try the buffer-local properties
			PropValue o = properties.get(name);
			if(o != null)
				return o.value;

			// For backwards compatibility
			if(!(name instanceof String))
				return null;

			Object retVal = getDefaultProperty((String)name);

			if(retVal == null)
				return null;
			else
			{
				properties.put(name,new PropValue(retVal,true));
				return retVal;
			}
		}
	} //}}}

	//{{{ getDefaultProperty() method
	public Object getDefaultProperty(String key)
	{
		return null;
	} //}}}

	//{{{ setProperty() method
	/**
	 * Sets the value of a buffer-local property.
	 * @param name The property name
	 * @param value The property value
	 * @since jEdit 4.0pre1
	 */
	public void setProperty(String name, Object value)
	{
		if(value == null)
			properties.remove(name);
		else
		{
			PropValue test = properties.get(name);
			if(test == null)
				properties.put(name,new PropValue(value,false));
			else if(test.value.equals(value))
			{
				// do nothing
			}
			else
			{
				test.value = value;
				test.defaultValue = false;
			}
		}
	} //}}}

	//{{{ setDefaultProperty() method
	public void setDefaultProperty(String name, Object value)
	{
		properties.put(name,new PropValue(value,true));
	} //}}}

	//{{{ unsetProperty() method
	/**
	 * Clears the value of a buffer-local property.
	 * @param name The property name
	 * @since jEdit 4.0pre1
	 */
	public void unsetProperty(String name)
	{
		properties.remove(name);
	} //}}}

	//{{{ resetCachedProperties() method
	public void resetCachedProperties()
	{
		// Need to reset properties that were cached defaults,
		// since the defaults might have changed.
		Iterator<PropValue> iter = properties.values().iterator();
		while(iter.hasNext())
		{
			PropValue value = iter.next();
			if(value.defaultValue)
				iter.remove();
		}
	} //}}}

	//{{{ getStringProperty() method
	/**
	 * Returns the value of a string property. This method is thread-safe.
	 * @param name The property name
	 * @since jEdit 4.0pre1
	 */
	public String getStringProperty(String name)
	{
		Object obj = getProperty(name);
		if(obj != null)
			return obj.toString();
		else
			return null;
	} //}}}

	//{{{ setStringProperty() method
	/**
	 * Sets a string property.
	 * @param name The property name
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public void setStringProperty(String name, String value)
	{
		setProperty(name,value);
	} //}}}

	//{{{ getBooleanProperty() methods
	/**
	 * Returns the value of a boolean property. This method is thread-safe.
	 * @param name The property name
	 * @since jEdit 4.0pre1
	 */
	public boolean getBooleanProperty(String name)
	{
		return getBooleanProperty(name, false);
	}

	/**
	 * Returns the value of a boolean property. This method is thread-safe.
	 * @param name The property name
	 * @param def The default value
	 * @since jEdit 4.3pre17
	 */
	public boolean getBooleanProperty(String name, boolean def)
	{
		Object obj = getProperty(name);
		return StandardUtilities.getBoolean(obj, def);
	} //}}}

	//{{{ setBooleanProperty() method
	/**
	 * Sets a boolean property.
	 * @param name The property name
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public void setBooleanProperty(String name, boolean value)
	{
		setProperty(name,value ? Boolean.TRUE : Boolean.FALSE);
	} //}}}

	//{{{ getIntegerProperty() method
	/**
	 * Returns the value of an integer property. This method is thread-safe.
	 * @param name The property name
	 * @since jEdit 4.0pre1
	 */
	public int getIntegerProperty(String name, int defaultValue)
	{
		boolean defaultValueFlag;
		Object obj;
		PropValue value = properties.get(name);
		if(value != null)
		{
			obj = value.value;
			defaultValueFlag = value.defaultValue;
		}
		else
		{
			obj = getProperty(name);
			// will be cached from now on...
			defaultValueFlag = true;
		}

		if(obj == null)
			return defaultValue;
		else if(obj instanceof Number)
			return ((Number)obj).intValue();
		else
		{
			try
			{
				int returnValue = Integer.parseInt(
					obj.toString().trim());
				properties.put(name,new PropValue(
					returnValue,
					defaultValueFlag));
				return returnValue;
			}
			catch(Exception e)
			{
				return defaultValue;
			}
		}
	} //}}}

	//{{{ setIntegerProperty() method
	/**
	 * Sets an integer property.
	 * @param name The property name
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public void setIntegerProperty(String name, int value)
	{
		setProperty(name,value);
	} //}}}

	//{{{ getPatternProperty()
	/**
	 * Returns the value of a property as a regular expression.
	 * This method is thread-safe.
	 * @param name The property name
	 * @param flags Regular expression compilation flags
	 * @since jEdit 4.3pre5
	 */
	public Pattern getPatternProperty(String name, int flags)
	{
		synchronized(propertyLock)
		{
			boolean defaultValueFlag;
			Object obj;
			PropValue value = properties.get(name);
			if(value != null)
			{
				obj = value.value;
				defaultValueFlag = value.defaultValue;
			}
			else
			{
				obj = getProperty(name);
				// will be cached from now on...
				defaultValueFlag = true;
			}

			if(obj == null)
				return null;
			else if (obj instanceof Pattern)
				return (Pattern) obj;
			else
			{
				Pattern re = Pattern.compile(obj.toString(),flags);
				properties.put(name,new PropValue(re,
					defaultValueFlag));
				return re;
			}
		}
	} //}}}

	//{{{ getRuleSetAtOffset() method
	/**
	 * Returns the syntax highlighting ruleset at the specified offset.
	 * @since jEdit 4.1pre1
	 */
	public ParserRuleSet getRuleSetAtOffset(int offset)
	{
		int line = getLineOfOffset(offset);
		offset -= getLineStartOffset(line);
		if(offset != 0)
			offset--;

		DefaultTokenHandler tokens = new DefaultTokenHandler();
		markTokens(line,tokens);
		Token token = TextUtilities.getTokenAtOffset(tokens.getTokens(),offset);
		return token.rules;
	} //}}}

	//{{{ getKeywordMapAtOffset() method
	/**
	 * Returns the syntax highlighting keyword map in effect at the
	 * specified offset. Used by the <b>Complete Word</b> command to
	 * complete keywords.
	 * @param offset The offset
	 * @since jEdit 4.0pre3
	 */
	public KeywordMap getKeywordMapAtOffset(int offset)
	{
		return getRuleSetAtOffset(offset).getKeywords();
	} //}}}

	//{{{ getContextSensitiveProperty() method
	/**
	 * Some settings, like comment start and end strings, can
	 * vary between different parts of a buffer (HTML text and inline
	 * JavaScript, for example).
	 * @param offset The offset
	 * @param name The property name
	 * @since jEdit 4.0pre3
	 */
	public String getContextSensitiveProperty(int offset, String name)
	{
		ParserRuleSet rules = getRuleSetAtOffset(offset);

		Object value = null;

		Map<String, String> rulesetProps = rules.getProperties();
		if(rulesetProps != null)
			value = rulesetProps.get(name);

		if(value == null)
			return null;
		else
			return String.valueOf(value);
	} //}}}

	//{{{ getMode() method
	/**
	 * Returns this buffer's edit mode. This method is thread-safe.
	 */
	public Mode getMode()
	{
		return mode;
	} //}}}

	//{{{ setMode() methods
	/**
	 * Sets this buffer's edit mode. Note that calling this before a buffer
	 * is loaded will have no effect; in that case, set the "mode" property
	 * to the name of the mode. A bit inelegant, I know...
	 * @param mode The mode name
	 * @since jEdit 4.2pre1
	 */
	public void setMode(String mode)
	{
		setMode(ModeProvider.instance.getMode(mode));
	}

	/**
	 * Sets this buffer's edit mode. Note that calling this before a buffer
	 * is loaded will have no effect; in that case, set the "mode" property
	 * to the name of the mode. A bit inelegant, I know...
	 * @param mode The mode
	 */
	public void setMode(Mode mode)
	{
		setMode(mode, false);
	}

	/**
	 * Sets this buffer's edit mode. Note that calling this before a buffer
	 * is loaded will have no effect; in that case, set the "mode" property
	 * to the name of the mode. A bit inelegant, I know...
	 * @param mode The mode
	 * @param forceContextInsensitive true if you want to force the buffer to be
	 * insensitive to the context. Careful it can break syntax highlight. Default
	 * value is false
	 * @since jEdit 4.5pre1
	 */
	public void setMode(Mode mode, boolean forceContextInsensitive)
	{
		/* This protects against stupid people (like me)
		 * doing stuff like buffer.setMode(jEdit.getMode(...)); */
		if(mode == null)
			throw new NullPointerException("Mode must be non-null");

		this.mode = mode;

		contextInsensitive = forceContextInsensitive ||
			mode.getBooleanProperty("contextInsensitive");

		setTokenMarker(mode.getTokenMarker());

		resetCachedProperties();
		propertiesChanged();
	}//}}}

	//}}}

	//{{{ Folding methods

	//{{{ isFoldStart() method
	/**
	 * Returns if the specified line begins a fold.
	 * @since jEdit 3.1pre1
	 */
	public boolean isFoldStart(int line)
	{
		return line != getLineCount() - 1
			&& getFoldLevel(line) < getFoldLevel(line + 1);
	} //}}}

	//{{{ isFoldEnd() method
	/**
	 * Returns if the specified line ends a fold.
	 * @since jEdit 4.2pre5
	 */
	public boolean isFoldEnd(int line)
	{
		return line != getLineCount() - 1
			&& getFoldLevel(line) > getFoldLevel(line + 1);
	} //}}}

	//{{{ invalidateCachedFoldLevels() method
	/**
	 * Invalidates all cached fold level information.
	 * @since jEdit 4.1pre11
	 */
	public void invalidateCachedFoldLevels()
	{
		lineMgr.setFirstInvalidFoldLevel(0);
		fireFoldLevelChanged(0,getLineCount());
	} //}}}

	//{{{ getFoldLevel() method
	/**
	 * Returns the fold level of the specified line.
	 * @param line A physical line index
	 * @since jEdit 3.1pre1
	 */
	public int getFoldLevel(int line)
	{
		if(line < 0 || line >= lineMgr.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);

		if(foldHandler instanceof DummyFoldHandler)
			return 0;

		int firstInvalidFoldLevel = lineMgr.getFirstInvalidFoldLevel();
		if(firstInvalidFoldLevel == -1 || line < firstInvalidFoldLevel)
		{
			return lineMgr.getFoldLevel(line);
		}
		else
		{
			if(Debug.FOLD_DEBUG)
				Log.log(Log.DEBUG,this,"Invalid fold levels from " + firstInvalidFoldLevel + " to " + line);

			int newFoldLevel = 0;
			boolean changed = false;
			int firstUpdatedFoldLevel = firstInvalidFoldLevel;

			for(int i = firstInvalidFoldLevel; i <= line; i++)
			{
				Segment seg = new Segment();
				newFoldLevel = foldHandler.getFoldLevel(this,i,seg);
				if(newFoldLevel != lineMgr.getFoldLevel(i))
				{
					if(Debug.FOLD_DEBUG)
						Log.log(Log.DEBUG,this,i + " fold level changed");
					changed = true;
					// Update preceding fold levels if necessary
					if (i == firstInvalidFoldLevel)
					{
						List<Integer> precedingFoldLevels =
							foldHandler.getPrecedingFoldLevels(
								this,i,seg,newFoldLevel);
						if (precedingFoldLevels != null)
						{
							int j = i;
							for (Integer foldLevel: precedingFoldLevels)
							{
								j--;
								lineMgr.setFoldLevel(j,foldLevel.intValue());
							}
							if (j < firstUpdatedFoldLevel)
								firstUpdatedFoldLevel = j;
						}
					}
				}
				lineMgr.setFoldLevel(i,newFoldLevel);
			}

			if(line == lineMgr.getLineCount() - 1)
				lineMgr.setFirstInvalidFoldLevel(-1);
			else
				lineMgr.setFirstInvalidFoldLevel(line + 1);

			if(changed)
			{
				if(Debug.FOLD_DEBUG)
					Log.log(Log.DEBUG,this,"fold level changed: " + firstUpdatedFoldLevel + ',' + line);
				fireFoldLevelChanged(firstUpdatedFoldLevel,line);
			}

			return newFoldLevel;
		}
	} //}}}

	//{{{ getFoldAtLine() method
	/**
	 * Returns an array. The first element is the start line, the
	 * second element is the end line, of the fold containing the
	 * specified line number.
	 * @param line The line number
	 * @since jEdit 4.0pre3
	 */
	public int[] getFoldAtLine(int line)
	{
		int start, end;

		if(isFoldStart(line))
		{
			start = line;
			int foldLevel = getFoldLevel(line);

			line++;

			while(getFoldLevel(line) > foldLevel)
			{
				line++;

				if(line == getLineCount())
					break;
			}

			end = line - 1;
		}
		else
		{
			start = line;
			int foldLevel = getFoldLevel(line);
			while(getFoldLevel(start) >= foldLevel)
			{
				if(start == 0)
					break;
				else
					start--;
			}

			end = line;
			while(getFoldLevel(end) >= foldLevel)
			{
				end++;

				if(end == getLineCount())
					break;
			}

			end--;
		}

		while(getLineLength(end) == 0 && end > start)
			end--;

		return new int[] { start, end };
	} //}}}

	//{{{ getFoldHandler() method
	/**
	 * Returns the current buffer's fold handler.
	 * @since jEdit 4.2pre1
	 */
	public FoldHandler getFoldHandler()
	{
		return foldHandler;
	} //}}}

	//{{{ setFoldHandler() method
	/**
	 * Sets the buffer's fold handler.
	 * @since jEdit 4.2pre2
	 */
	public void setFoldHandler(FoldHandler foldHandler)
	{
		FoldHandler oldFoldHandler = this.foldHandler;

		if(foldHandler.equals(oldFoldHandler))
			return;

		this.foldHandler = foldHandler;

		lineMgr.setFirstInvalidFoldLevel(0);

		fireFoldHandlerChanged();
	} //}}}

	//}}}

	//{{{ Undo

	//{{{ undo() method
	/**
	 * Undoes the most recent edit.
	 *
	 * @since jEdit 4.0pre1
	 */
	public void undo(TextArea textArea)
	{
		if(undoMgr == null)
			return;

		if(!isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		try
		{
			writeLock();

			undoInProgress = true;
			fireBeginUndo();
			int caret = undoMgr.undo();
			if(caret == -1)
				textArea.getToolkit().beep();
			else
				textArea.setCaretPosition(caret);

			fireEndUndo();
			fireTransactionComplete();
		}
		finally
		{
			undoInProgress = false;

			writeUnlock();
		}
	} //}}}

	//{{{ redo() method
	/**
	 * Redoes the most recently undone edit.
	 *
	 * @since jEdit 2.7pre2
	 */
	public void redo(TextArea textArea)
	{
		if(undoMgr == null)
			return;

		if(!isEditable())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		try
		{
			writeLock();

			undoInProgress = true;
			fireBeginRedo();
			int caret = undoMgr.redo();
			if(caret == -1)
				textArea.getToolkit().beep();
			else
				textArea.setCaretPosition(caret);

			fireEndRedo();
			fireTransactionComplete();
		}
		finally
		{
			undoInProgress = false;

			writeUnlock();
		}
	} //}}}

	//{{{ isTransactionInProgress() method
	/**
	 * Returns if an undo or compound edit is currently in progress. If this
	 * method returns true, then eventually a
	 * {@link org.gjt.sp.jedit.buffer.BufferListener#transactionComplete(JEditBuffer)}
	 * buffer event will get fired.
	 * @since jEdit 4.0pre6
	 */
	public boolean isTransactionInProgress()
	{
		return transaction || undoInProgress || insideCompoundEdit();
	} //}}}

	//{{{ beginCompoundEdit() method
	/**
	 * Starts a compound edit. All edits from now on until
	 * {@link #endCompoundEdit()} are called will be merged
	 * into one. This can be used to make a complex operation
	 * undoable in one step. Nested calls to
	 * {@link #beginCompoundEdit()} behave as expected,
	 * requiring the same number of {@link #endCompoundEdit()}
	 * calls to end the edit.
	 * @see #endCompoundEdit()
	 */
	public void beginCompoundEdit()
	{
		try
		{
			writeLock();

			undoMgr.beginCompoundEdit();
		}
		finally
		{
			writeUnlock();
		}
	} //}}}

	//{{{ endCompoundEdit() method
	/**
	 * Ends a compound edit. All edits performed since
	 * {@link #beginCompoundEdit()} was called can now
	 * be undone in one step by calling {@link #undo(TextArea)}.
	 * @see #beginCompoundEdit()
	 */
	public void endCompoundEdit()
	{
		try
		{
			writeLock();

			undoMgr.endCompoundEdit();

			if(!insideCompoundEdit())
				fireTransactionComplete();
		}
		finally
		{
			writeUnlock();
		}
	}//}}}

	//{{{ insideCompoundEdit() method
	/**
	 * Returns if a compound edit is currently active.
	 * @since jEdit 3.1pre1
	 */
	public boolean insideCompoundEdit()
	{
		return undoMgr.insideCompoundEdit();
	} //}}}

	//{{{ isUndoInProgress() method
	/**
	 * Returns if an undo or redo is currently being performed.
	 * @since jEdit 4.3pre3
	 */
	public boolean isUndoInProgress()
	{
		return undoInProgress;
	} //}}}

	//{{{ getUndoId() method
	/**
	 * Returns an object that identifies the undo operation to which the
	 * current content change belongs. This method can be used by buffer
	 * listeners during content changes (contentInserted/contentRemoved)
	 * to find out which content changes belong to the same "undo" operation.
	 * The same undoId object will be returned for all content changes
	 * belonging to the same undo operation. Only the identity of the
	 * undoId can be used, by comparing it with a previously-returned undoId
	 * using "==".
	 * @since jEdit 4.3pre18
	 */
	public Object getUndoId()
	{
		return undoMgr.getUndoId();
	} //}}}

	//}}}

	//{{{ Buffer events
	public static final int NORMAL_PRIORITY = 0;
	public static final int HIGH_PRIORITY = 1;

	static class Listener
	{
		BufferListener listener;
		int priority;

		Listener(BufferListener listener, int priority)
		{
			this.listener = listener;
			this.priority = priority;
		}
	}

	//{{{ addBufferListener() methods
	/**
	 * Adds a buffer change listener.
	 * @param listener The listener
	 * @param priority Listeners with HIGH_PRIORITY get the event before
	 * listeners with NORMAL_PRIORITY
	 * @since jEdit 4.3pre3
	 */
	public void addBufferListener(BufferListener listener,
		int priority)
	{
		Listener l = new Listener(listener,priority);
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			Listener _l = bufferListeners.get(i);
			if(_l.priority < priority)
			{
				bufferListeners.add(i,l);
				return;
			}
		}
		bufferListeners.add(l);
	}

	/**
	 * Adds a buffer change listener.
	 * @param listener The listener
	 * @since jEdit 4.3pre3
	 */
	public void addBufferListener(BufferListener listener)
	{
		addBufferListener(listener,NORMAL_PRIORITY);
	} //}}}

	//{{{ removeBufferListener() method
	/**
	 * Removes a buffer change listener.
	 * @param listener The listener
	 * @since jEdit 4.3pre3
	 */
	public void removeBufferListener(BufferListener listener)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			if(bufferListeners.get(i).listener == listener)
			{
				bufferListeners.remove(i);
				return;
			}
		}
	} //}}}

	//{{{ getBufferListeners() method
	/**
	 * Returns an array of registered buffer change listeners.
	 * @since jEdit 4.3pre3
	 */
	public BufferListener[] getBufferListeners()
	{
		BufferListener[] returnValue
			= new BufferListener[
			bufferListeners.size()];
		for(int i = 0; i < returnValue.length; i++)
		{
			returnValue[i] = bufferListeners.get(i).listener;
		}
		return returnValue;
	} //}}}

	//{{{ setUndoLimit() method
	/**
	 * Set the undo limit of the Undo Manager.
	 *
	 * @param limit the new limit
	 * @since jEdit 4.3pre16
	 */
	public void setUndoLimit(int limit)
	{
		if (undoMgr != null)
			undoMgr.setLimit(limit);
	} //}}}

	//{{{ canUndo() method
	/**
	 * Returns true if an undo operation can be performed.
	 * @since jEdit 4.3pre18
	 */
	public boolean canUndo()
	{
		if (undoMgr == null)
			return false;
		return undoMgr.canUndo();
	} //}}}

	//{{{ canRedo() method
	/**
	 * Returns true if a redo operation can be performed.
	 * @since jEdit 4.3pre18
	 */
	public boolean canRedo()
	{
		if (undoMgr == null)
			return false;
		return undoMgr.canRedo();
	} //}}}

	//{{{ isContextInsensitive() method
	/**
	 * Returns true if the buffer highlight is
	 * not sensitive to the context.
	 * @return true if the highlight is insensitive to
	 * the context
	 * @since jEdit 4.5pre1
	 */
	public boolean isContextInsensitive()
	{
		return contextInsensitive;
	}//}}}

	//{{{ setContextInsensitive() method
	/**
	 * Set the buffer to be insensitive to the context during
	 * highlight.
	 * @param contextInsensitive the new contextInsensitive value
	 * the context
	 * @since jEdit 4.5pre1
	 */
	public void setContextInsensitive(boolean contextInsensitive)
	{
		this.contextInsensitive = contextInsensitive;
	}//}}}

	//}}}

	//{{{ Protected members
	/**
	 * The edit mode of the buffer.
	 */
	protected Mode mode;
	/**
	 * If true the syntax highlight is context insensitive.
	 * To highlight a line we don't keed the context of the previous line.
	 */
	protected boolean contextInsensitive;
	protected UndoManager undoMgr;

	//{{{ Event firing methods

	//{{{ fireFoldLevelChanged() method
	protected void fireFoldLevelChanged(int start, int end)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.foldLevelChanged(this,start,end);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireContentInserted() method
	protected void fireContentInserted(int startLine, int offset,
		int numLines, int length)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.contentInserted(this,startLine,
					offset,numLines,length);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireContentRemoved() method
	protected void fireContentRemoved(int startLine, int offset,
		int numLines, int length)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.contentRemoved(this,startLine,
					offset,numLines,length);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ firePreContentInserted() method
	protected void firePreContentInserted(int startLine, int offset,
		int numLines, int length)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.preContentInserted(this,startLine,
					offset,numLines,length);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ firePreContentRemoved() method
	protected void firePreContentRemoved(int startLine, int offset,
		int numLines, int length)
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.preContentRemoved(this,startLine,
					offset,numLines,length);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireBeginUndo() method
	protected void fireBeginUndo()
	{
	} //}}}

	//{{{ fireEndUndo() method
	protected void fireEndUndo()
	{
	} //}}}

	//{{{ fireBeginRedo() method
	protected void fireBeginRedo()
	{
	} //}}}

	//{{{ fireEndRedo() method
	protected void fireEndRedo()
	{
	} //}}}

	//{{{ fireTransactionComplete() method
	protected void fireTransactionComplete()
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.transactionComplete(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireFoldHandlerChanged() method
	protected void fireFoldHandlerChanged()
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.foldHandlerChanged(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireBufferLoaded() method
	protected void fireBufferLoaded()
	{
		for(int i = 0; i < bufferListeners.size(); i++)
		{
			BufferListener listener = getListener(i);
			try
			{
				listener.bufferLoaded(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//}}}

	//{{{ isFileReadOnly() method
	protected boolean isFileReadOnly()
	{
		return readOnly;
	} //}}}

	//{{{ setFileReadOnly() method
	protected void setFileReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
	} //}}}

	//{{{ loadText() method
	protected void loadText(Segment seg, IntegerArray endOffsets)
	{
		if(seg == null)
			seg = new Segment(new char[1024],0,0);

		if(endOffsets == null)
		{
			endOffsets = new IntegerArray();
			endOffsets.add(1);
		}

		try
		{
			writeLock();

			// For `reload' command
			// contentMgr.remove() changes this!
			int length = getLength();

			firePreContentRemoved(0,0,getLineCount()
				- 1,length);

			contentMgr.remove(0,length);
			lineMgr.contentRemoved(0,0,getLineCount()
				- 1,length);
			positionMgr.contentRemoved(0,length);
			fireContentRemoved(0,0,getLineCount()
				- 1,length);

			firePreContentInserted(0, 0, endOffsets.getSize() - 1, seg.count - 1);
			// theoretically a segment could
			// have seg.offset != 0 but
			// SegmentBuffer never does that
			contentMgr._setContent(seg.array,seg.count);

			lineMgr._contentInserted(endOffsets);
			positionMgr.contentInserted(0,seg.count);

			fireContentInserted(0,0,
				endOffsets.getSize() - 1,
				seg.count - 1);
		}
		finally
		{
			writeUnlock();
		}
	} //}}}

	//{{{ invalidateFoldLevels() method
	protected void invalidateFoldLevels()
	{
		lineMgr.setFirstInvalidFoldLevel(0);
	} //}}}

	//{{{ parseBufferLocalProperties() method
	protected void parseBufferLocalProperties()
	{
		int maxRead = 10000;
		int lineCount = getLineCount();
		int lastLine = Math.min(9, lineCount - 1);
		int max = Math.min(maxRead, getLineEndOffset(lastLine) - 1);
		parseBufferLocalProperties(getSegment(0, max));

		// first line for last 10 lines, make sure not to overlap
		// with the first 10
		int firstLine = Math.max(lastLine + 1, lineCount - 10);
		if(firstLine < lineCount)
		{
			int firstLineStartOffset = getLineStartOffset(firstLine);
			int length = getLineEndOffset(lineCount - 1)
				- (firstLineStartOffset + 1);
			if (length > maxRead)
			{
				firstLineStartOffset += length - maxRead;
				length = maxRead;
			}
			parseBufferLocalProperties(getSegment(firstLineStartOffset,length));
		}
	} //}}}

	//{{{ Used to store property values
	protected static class PropValue
	{
		PropValue(Object value, boolean defaultValue)
		{
			if(value == null)
				throw new NullPointerException();
			this.value = value;
			this.defaultValue = defaultValue;
		}

		Object value;

		/**
		 * If this is true, then this value is cached from the mode
		 * or global defaults, so when the defaults change this property
		 * value must be reset.
		 */
		boolean defaultValue;

		/**
		 * For debugging purposes.
		 */
		public String toString()
		{
			return value.toString();
		}
	} //}}}

	//}}}

	//{{{ Private members
	private final List<Listener> bufferListeners;
	private final ReentrantReadWriteLock lock;
	private final ContentManager contentMgr;
	private final LineManager lineMgr;
	private final PositionManager positionMgr;
	private FoldHandler foldHandler;
	private final IntegerArray integerArray;
	private TokenMarker tokenMarker;
	private boolean undoInProgress;
	private boolean dirty;
	private boolean readOnly;
	private boolean readOnlyOverride;
	private boolean transaction;
	private boolean loading;
	private boolean io;
	private final Map<Object, PropValue> properties;
	private final Object propertyLock;
	public boolean elasticTabstopsOn = false; 
	private ColumnBlock columnBlock;

	//{{{ getListener() method
	private BufferListener getListener(int index)
	{
		return bufferListeners.get(index).listener;
	} //}}}

	//{{{ contentInserted() method
	private void contentInserted(int offset, int length,
		IntegerArray endOffsets)
	{
		try
		{
			transaction = true;

			int startLine = lineMgr.getLineOfOffset(offset);
			int numLines = endOffsets.getSize();

			if (!loading)
			{
				firePreContentInserted(startLine, offset, numLines, length);
			}

			lineMgr.contentInserted(startLine,offset,numLines,length,
				endOffsets);
			positionMgr.contentInserted(offset,length);

			setDirty(true);

			if(!loading)
			{
				fireContentInserted(startLine,offset,numLines,length);

				if(!undoInProgress && !insideCompoundEdit())
					fireTransactionComplete();
			}

		}
		finally
		{
			transaction = false;
		}
	} //}}}

	//{{{ parseBufferLocalProperties() method
	private void parseBufferLocalProperties(CharSequence prop)
	{
		StringBuilder buf = new StringBuilder();
		String name = null;
		boolean escape = false;
		int length = prop.length();
		for(int i = 0; i < length; i++)
		{
			char c = prop.charAt(i);
			switch(c)
			{
			case ':':
				if(escape)
				{
					escape = false;
					buf.append(':');
					break;
				}
				if(name != null)
				{
					// use the low-level property setting code
					// so that if we have a buffer-local
					// property with the same value as a default,
					// later changes in the default don't affect
					// the buffer-local property
					properties.put(name,new PropValue(buf.toString(),false));
					name = null;
				}
				buf.setLength(0);
				break;
			case '=':
				if(escape)
				{
					escape = false;
					buf.append('=');
					break;
				}
				name = buf.toString();
				buf.setLength(0);
				break;
			case '\\':
				if(escape)
					buf.append('\\');
				escape = !escape;
				break;
			case 'n':
				if(escape)
				{	buf.append('\n');
					escape = false;
					break;
				}
			case 'r':
				if(escape)
				{	buf.append('\r');
					escape = false;
					break;
				}
			case 't':
				if(escape)
				{
					buf.append('\t');
					escape = false;
					break;
				}
			default:
				buf.append(c);
				break;
			}
		}
	} //}}}

	//{{{ getIndentRules() method
	private List<IndentRule> getIndentRules(int line)
	{
		String modeName = null;
		TokenMarker.LineContext ctx = lineMgr.getLineContext(line);
		if (ctx != null && ctx.rules != null)
			modeName = ctx.rules.getModeName();
		if (modeName == null)
			modeName = tokenMarker.getMainRuleSet().getModeName();
		return ModeProvider.instance.getMode(modeName).getIndentRules();
	} //}}}

	//{{{ updateColumnBlocks() method
	public void updateColumnBlocks(int startLine,int endLine,int startColumn,Node parent)
	{
		if((parent!=null)&&(startLine>=0)&&(endLine>=0)&&(startLine<=endLine))
		{	
			int currentLine = startLine;
			int colBlockWidth=0;
			Vector<ColumnBlockLine> columnBlockLines = new Vector<ColumnBlockLine>();
			//while(currentLine<=endLine)
			for(int ik=startLine-((ColumnBlock)parent).getStartLine();currentLine<=endLine;ik++)
			{
				Segment seg = new Segment();
				int actualStart =  startColumn ;
				if(((ColumnBlock)parent).getLines().size()>0)
				{
					ColumnBlockLine line = ((ColumnBlockLine)(((ColumnBlock)parent).getLines().elementAt(ik)));
					if(currentLine!=line.getLine())
					{
						throw new IllegalArgumentException();
					}
					actualStart = line.getColumnEndIndex()+1;
				}
				getLineText(currentLine, actualStart, seg);
				int tabPos = getTabStopPosition(seg);
				if(tabPos>=0)
				{
					columnBlockLines.add(new ColumnBlockLine(currentLine, actualStart, actualStart+tabPos));
					if( tabPos>colBlockWidth)
					{
						colBlockWidth =  tabPos;
					}
				}
				if((( tabPos<0)&&(columnBlockLines.size()>0))||((columnBlockLines.size()>0)&&(currentLine==endLine)))
				{
					ColumnBlock  block = new ColumnBlock(this,((ColumnBlockLine)columnBlockLines.elementAt(0)).getLine(),startColumn+colBlockWidth,((ColumnBlockLine)columnBlockLines.elementAt(columnBlockLines.size()-1)).getLine(),startColumn+colBlockWidth);
					block.setLines(columnBlockLines);
					block.setParent(parent);
					block.setWidth(colBlockWidth);
					block.setTabSizeDirtyStatus(true,false);
					//block.populateTabSizes();
					parent.addChild(block);
					colBlockWidth=0;
					columnBlockLines = new Vector<ColumnBlockLine>();
					updateColumnBlocks(block.getStartLine(), block.getEndLine(), startColumn+block.getColumnWidth()+1, block);
				}
				currentLine++;
			}
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}
	//}}}
	
	//{{{ getTabStopPosition() method
	public int getTabStopPosition(Segment seg )
	{
		for (int i = 0; i < seg.count; i++)
		{
			if(seg.array[i+seg.offset]=='\t')
			{
				return i;
			}
		}
		return -5;
	}
	 //}}}
	
	public final String columnBlockLock = "columnBlockLock";
	
	//{{{ indentUsingElasticTabstops() method
	public void indentUsingElasticTabstops()
	{
		synchronized(columnBlockLock)
		{
			columnBlock = new ColumnBlock(this,0,getLineCount()-1);
			updateColumnBlocks(0, lineMgr.getLineCount()-1, 0, columnBlock);
		}	
	}
	 //}}}
	
	//{{{ getColumnBlock() method
	public ColumnBlock getColumnBlock()
	{
		return columnBlock;
	}
	 //}}}
//}}}	
}
