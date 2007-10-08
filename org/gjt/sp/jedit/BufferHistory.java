/*
 * BufferHistory.java - Remembers caret positions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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

package org.gjt.sp.jedit;

//{{{ Imports
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.msg.DynamicMenuChanged;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.IOUtilities;
//}}}

/**
 * Recent file list.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferHistory
{
	//{{{ Entry class
	/**
	 * Recent file list entry.
	 */
	public static class Entry
	{
		public String path;
		public int caret;
		public String selection;
		public String encoding;
		public String mode;

		public Selection[] getSelection()
		{
			return stringToSelection(selection);
		}

		public Entry(String path, int caret, String selection, String encoding, String mode)
		{
			this.path = path;
			this.caret = caret;
			this.selection = selection;
			this.encoding = encoding;
			this.mode = mode;
		}

		public String toString()
		{
			return path + ": " + caret;
		}
	} //}}}

	//{{{ getEntry() method
	public static Entry getEntry(String path)
	{
		historyLock.readLock().lock();
		try
		{
			for (Entry entry : history)
			{
				if(MiscUtilities.pathsEqual(entry.path,path))
					return entry;
			}
		}
		finally
		{
			historyLock.readLock().unlock();
		}

		return null;
	} //}}}

	//{{{ setEntry() method
	public static void setEntry(String path, int caret, Selection[] selection,
		String encoding, String mode)
	{
		Entry entry = new Entry(path,caret,
			selectionToString(selection), encoding, mode);
		historyLock.writeLock().lock();
		try
		{
			removeEntry(path);
			addEntry(entry);
		}
		finally
		{
			historyLock.writeLock().unlock();
		}
		notifyChange();
	} //}}}

	//{{{ clear() method
	/**
	 * Clear the BufferHistory.
	 * @since 4.3pre6
	 */
	public static void clear()
	{
		historyLock.writeLock().lock();
		try
		{
			history.clear();
		}
		finally
		{
			historyLock.writeLock().unlock();
		}
		notifyChange();
	} //}}}

	//{{{ getHistory() method
	/**
	 * Returns the Buffer list.
	 * @return the buffer history list
	 * @since jEdit 4.2pre2
	 */
	public static List<Entry> getHistory()
	{
		// Returns a snapshot to avoid concurrent access to the
		// history. This requires O(n) time, but it should be ok
		// because this method should be used only by external
		// O(n) operation.

		historyLock.readLock().lock();
		try
		{
			return (List<Entry>)history.clone();
		}
		finally
		{
			historyLock.readLock().unlock();
		}
	} //}}}

	//{{{ load() method
	public static void load()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File recent = new File(MiscUtilities.constructPath(
			settingsDirectory,"recent.xml"));
		if(!recent.exists())
			return;

		recentModTime = recent.lastModified();

		Log.log(Log.MESSAGE,BufferHistory.class,"Loading recent.xml");

		RecentHandler handler = new RecentHandler();
		try
		{
			XMLUtilities.parseXML(new FileInputStream(recent), handler);
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,BufferHistory.class,e);
		}
		trimToLimit(handler.result);
		history = handler.result;
	} //}}}

	//{{{ save() method
	public static void save()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File file1 = new File(MiscUtilities.constructPath(
			settingsDirectory, "#recent.xml#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			settingsDirectory, "recent.xml"));
		if(file2.exists() && file2.lastModified() != recentModTime)
		{
			Log.log(Log.WARNING,BufferHistory.class,file2
				+ " changed on disk; will not save recent"
				+ " files");
			return;
		}

		jEdit.backupSettingsFile(file2);

		Log.log(Log.MESSAGE,BufferHistory.class,"Saving " + file1);

		String lineSep = System.getProperty("line.separator");

		boolean ok = false;

		BufferedWriter out = null;

		try
		{
			out = new BufferedWriter(new FileWriter(file1));

			out.write("<?xml version=\"1.0\"?>");
			out.write(lineSep);
			out.write("<!DOCTYPE RECENT SYSTEM \"recent.dtd\">");
			out.write(lineSep);
			out.write("<RECENT>");
			out.write(lineSep);

			// Make a snapshot to avoid long locking period
			// which may be required by file I/O.
			List<Entry> snapshot = getHistory();

			for (Entry entry : snapshot)
			{
				out.write("<ENTRY>");
				out.write(lineSep);

				out.write("<PATH>");
				out.write(XMLUtilities.charsToEntities(entry.path,false));
				out.write("</PATH>");
				out.write(lineSep);

				out.write("<CARET>");
				out.write(String.valueOf(entry.caret));
				out.write("</CARET>");
				out.write(lineSep);

				if(entry.selection != null
					&& entry.selection.length() > 0)
				{
					out.write("<SELECTION>");
					out.write(entry.selection);
					out.write("</SELECTION>");
					out.write(lineSep);
				}

				if(entry.encoding != null)
				{
					out.write("<ENCODING>");
					out.write(entry.encoding);
					out.write("</ENCODING>");
					out.write(lineSep);
				}

				if (entry.mode != null)
				{
					out.write("<MODE>");
					out.write(entry.mode);
					out.write("</MODE>");
					out.write(lineSep);
				}

				out.write("</ENTRY>");
				out.write(lineSep);
			}

			out.write("</RECENT>");
			out.write(lineSep);

			ok = true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,BufferHistory.class,e);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}

		if(ok)
		{
			/* to avoid data loss, only do this if the above
			 * completed successfully */
			file2.delete();
			file1.renameTo(file2);
		}

		recentModTime = file2.lastModified();
	} //}}}

	//{{{ Private members
	private static LinkedList<Entry> history;
	private static ReentrantReadWriteLock historyLock;
	private static long recentModTime;

	//{{{ Class initializer
	static
	{
		history = new LinkedList<Entry>();
		historyLock = new ReentrantReadWriteLock();
	} //}}}

	//{{{ addEntry() method
	private static void addEntry(Entry entry)
	{
		historyLock.writeLock().lock();
		try
		{
			history.addFirst(entry);
			trimToLimit(history);
		}
		finally
		{
			historyLock.writeLock().unlock();
		}
	} //}}}

	//{{{ removeEntry() method
	private static void removeEntry(String path)
	{
		historyLock.writeLock().lock();
		try
		{
			Iterator<Entry> iter = history.iterator();
			while(iter.hasNext())
			{
				Entry entry = iter.next();
				if(MiscUtilities.pathsEqual(path,entry.path))
				{
					iter.remove();
					return;
				}
			}
		}
		finally
		{
			historyLock.writeLock().unlock();
		}
	} //}}}

	//{{{ selectionToString() method
	private static String selectionToString(Selection[] s)
	{
		if(s == null)
			return null;

		StringBuffer buf = new StringBuffer();

		for(int i = 0; i < s.length; i++)
		{
			if(i != 0)
				buf.append(' ');

			Selection sel = s[i];
			if(sel instanceof Selection.Range)
				buf.append("range ");
			else //if(sel instanceof Selection.Rect)
				buf.append("rect ");
			buf.append(sel.getStart());
			buf.append(' ');
			buf.append(sel.getEnd());
		}

		return buf.toString();
	} //}}}

	//{{{ stringToSelection() method
	private static Selection[] stringToSelection(String s)
	{
		if(s == null)
			return null;

		Vector<Selection> selection = new Vector<Selection>();
		StringTokenizer st = new StringTokenizer(s);

		while(st.hasMoreTokens())
		{
			String type = st.nextToken();
			int start = Integer.parseInt(st.nextToken());
			int end = Integer.parseInt(st.nextToken());
			if(end < start)
			{
				// I'm not sure when this can happen,
				// but it does sometimes, witness the
				// jEdit bug tracker.
				continue;
			}

			Selection sel;
			if(type.equals("range"))
				sel = new Selection.Range(start,end);
			else //if(type.equals("rect"))
				sel = new Selection.Rect(start,end);

			selection.add(sel);
		}

		Selection[] returnValue = new Selection[selection.size()];
		selection.copyInto(returnValue);
		return returnValue;
	} //}}}

	//{{{ trimToLimit() method
	private static void trimToLimit(LinkedList<Entry> list)
	{
		int max = jEdit.getIntegerProperty("recentFiles",50);
		while(list.size() > max)
			list.removeLast();
	} //}}}

	//{{{ notifyChange() method
	private static void notifyChange()
	{
		EditBus.send(new DynamicMenuChanged("recent-files"));
	} //}}}

	//{{{ RecentHandler class
	private static class RecentHandler extends DefaultHandler
	{
		public LinkedList<Entry> result = new LinkedList<Entry>();

		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "recent.dtd", getClass());
		}

		public void endElement(String uri, String localName, String name)
		{
			if(name.equals("ENTRY"))
			{
				result.addLast(new Entry(
					path,caret,selection,
					encoding,
					mode));
				path = null;
				caret = 0;
				selection = null;
				encoding = null;
				mode = null;
			}
			else if(name.equals("PATH"))
				path = charData.toString();
			else if(name.equals("CARET"))
				caret = Integer.parseInt(charData.toString());
			else if(name.equals("SELECTION"))
				selection = charData.toString();
			else if(name.equals("ENCODING"))
				encoding = charData.toString();
			else if(name.equals("MODE"))
				mode = charData.toString();
			charData.setLength(0);
		}

		public void characters(char[] ch, int start, int length)
		{
			charData.append(ch,start,length);
		}

		// end HandlerBase implementation

		// private members
		private String path;
		private int caret;
		private String selection;
		private String encoding;
		private String mode;
		private StringBuffer charData = new StringBuffer();
	} //}}}

	//}}}
}
