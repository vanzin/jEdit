/*
 * HistoryModel.java - History list model
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import javax.swing.DefaultListModel;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * A history list. One history list can be used by several history text
 * fields. Note that the list model implementation is incomplete; no events
 * are fired when the history model changes.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryModel extends DefaultListModel
	implements MutableListModel
{
	//{{{ HistoryModel constructor
	/**
	 * Creates a new history list. Calling this is normally not
	 * necessary.
	 */
	public HistoryModel(String name)
	{
		this.name = name;
	} //}}}

	//{{{ addItem() method
	/**
	 * Adds an item to the end of this history list, trimming the list
	 * to the maximum number of items if necessary.
	 * @param text The item
	 */
	public void addItem(String text)
	{
		if(text == null || text.length() == 0)
			return;

		int index = indexOf(text);
		if(index != -1)
			removeElementAt(index);

		insertElementAt(text,0);

		while(getSize() > max)
			removeElementAt(getSize() - 1);
	} //}}}

	//{{{ insertElementAt() method
	public void insertElementAt(Object obj, int index)
	{
		modified = true;
		super.insertElementAt(obj,index);
	} //}}}

	//{{{ getItem() method
	/**
	 * Returns an item from the history list.
	 * @param index The index
	 */
	public String getItem(int index)
	{
		return (String)elementAt(index);
	} //}}}

	//{{{ removeElement() method
	public boolean removeElement(Object obj)
	{
		modified = true;
		return super.removeElement(obj);
	} //}}}

	//{{{ clear() method
	/**
	 * @deprecated Call <code>removeAllElements()</code> instead.
	 */
	public void clear()
	{
		removeAllElements();
	} //}}}

	//{{{ removeAllElements() method
	public void removeAllElements()
	{
		modified = true;
		super.removeAllElements();
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the name of this history list. This can be passed
	 * to the HistoryTextField constructor.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getModel() method
	/**
	 * Returns a named model. If the specified model does not
	 * already exist, it will be created.
	 * @param name The model name
	 */
	public static HistoryModel getModel(String name)
	{
		if(models == null)
			models = new Hashtable();

		HistoryModel model = (HistoryModel)models.get(name);
		if(model == null)
		{
			model = new HistoryModel(name);
			models.put(name,model);
		}

		return model;
	} //}}}

	//{{{ loadHistory() method
	public static void loadHistory()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		history = new File(MiscUtilities.constructPath(
			settingsDirectory,"history"));
		if(!history.exists())
			return;

		historyModTime = history.lastModified();

		Log.log(Log.MESSAGE,HistoryModel.class,"Loading history");

		if(models == null)
			models = new Hashtable();

		BufferedReader in = null;

		try
		{
			in = new BufferedReader(new FileReader(history));

			HistoryModel currentModel = null;
			String line;

			while((line = in.readLine()) != null)
			{
				if(line.startsWith("[") && line.endsWith("]"))
				{
					if(currentModel != null)
					{
						models.put(currentModel.getName(),
							currentModel);
					}

					String modelName = MiscUtilities
						.escapesToChars(line.substring(
						1,line.length() - 1));
					currentModel = new HistoryModel(
						modelName);
				}
				else if(currentModel == null)
				{
					throw new IOException("History data starts"
						+ " before model name");
				}
				else
				{
					currentModel.addElement(MiscUtilities
						.escapesToChars(line));
				}
			}

			if(currentModel != null)
			{
				models.put(currentModel.getName(),currentModel);
			}
		}
		catch(FileNotFoundException fnf)
		{
			//Log.log(Log.DEBUG,HistoryModel.class,fnf);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
		finally
		{
			try
			{
				if(in != null)
					in.close();
			}
			catch(IOException io)
			{
			}
		}
	} //}}}

	//{{{ saveHistory() method
	public static void saveHistory()
	{
		if(!modified)
			return;

		Log.log(Log.MESSAGE,HistoryModel.class,"Saving history");
		File file1 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "#history#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "history"));
		if(file2.exists() && file2.lastModified() != historyModTime)
		{
			Log.log(Log.WARNING,HistoryModel.class,file2
				+ " changed on disk; will not save history");
			return;
		}

		jEdit.backupSettingsFile(file2);

		String lineSep = System.getProperty("line.separator");

		BufferedWriter out = null;

		try
		{
			out = new BufferedWriter(new FileWriter(file1));

			if(models != null)
			{
				Enumeration modelEnum = models.elements();
				while(modelEnum.hasMoreElements())
				{
					HistoryModel model = (HistoryModel)modelEnum
						.nextElement();
					if(model.getSize() == 0)
						continue;
	
					out.write('[');
					out.write(MiscUtilities.charsToEscapes(
						model.getName(),TO_ESCAPE));
					out.write(']');
					out.write(lineSep);
	
					for(int i = 0; i < model.getSize(); i++)
					{
						out.write(MiscUtilities.charsToEscapes(
							model.getItem(i),
							TO_ESCAPE));
						out.write(lineSep);
					}
				}
			}

			out.close();

			/* to avoid data loss, only do this if the above
			 * completed successfully */
			file2.delete();
			file1.renameTo(file2);
			modified = false;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
		finally
		{
			try
			{
				if(out != null)
					out.close();
			}
			catch(IOException e)
			{
			}
		}

		historyModTime = file2.lastModified();
	} //}}}

	//{{{ propertiesChanged() method
	public static void propertiesChanged()
	{
		max = jEdit.getIntegerProperty("history",25);
	} //}}}

	//{{{ Private members
	private static final String TO_ESCAPE = "\r\n\t\\\"'[]";
	private static int max;

	private String name;
	private static Hashtable models;

	private static boolean modified;
	private static File history;
	private static long historyModTime;
	//}}}
}
