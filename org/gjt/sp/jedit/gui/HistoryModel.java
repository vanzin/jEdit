/*
 * HistoryModel.java - History list model
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Slava Pestov
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
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * A history list. One history list can be used by several history text
 * fields.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryModel
{
	//{{{ HistoryModel constructor
	/**
	 * Creates a new history list. Calling this is normally not
	 * necessary.
	 */
	public HistoryModel(String name)
	{
		this.name = name;

		max = jEdit.getIntegerProperty("history",25);
		data = new Vector(max);
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

		int index = data.indexOf(text);
		if(index != -1)
			data.removeElementAt(index);

		data.insertElementAt(text,0);

		while(getSize() > max)
			data.removeElementAt(data.size() - 1);
	} //}}}

	//{{{ getItem() method
	/**
	 * Returns an item from the history list.
	 * @param index The index
	 */
	public String getItem(int index)
	{
		return (String)data.elementAt(index);
	} //}}}

	//{{{ getSize() method
	/**
	 * Returns the number of elements in this history list.
	 */
	public int getSize()
	{
		return data.size();
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
	/**
	 * Loads the history from the specified file. jEdit calls this
	 * on startup.
	 * @param The file
	 */
	public static void loadHistory(File file)
	{
		if(models == null)
			models = new Hashtable();

		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));

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
					currentModel = new HistoryModel(line
						.substring(1,line.length() - 1));
				}
				else if(currentModel == null)
				{
					throw new IOException("History data starts"
						+ " before model name");
				}
				else
				{
					currentModel.data.addElement(MiscUtilities
						.escapesToChars(line));
				}
			}

			if(currentModel != null)
			{
				models.put(currentModel.getName(),currentModel);
			}

			in.close();
		}
		catch(FileNotFoundException fnf)
		{
			Log.log(Log.DEBUG,HistoryModel.class,fnf);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
	} //}}}

	//{{{ saveHistory() method
	/**
	 * Saves the history to the specified file. jEdit calls this when
	 * it is exiting.
	 * @param file The file
	 */
	public static void saveHistory(File file)
	{
		String lineSep = System.getProperty("line.separator");
		try
		{
			BufferedWriter out = new BufferedWriter(
				new FileWriter(file));

			if(models == null)
			{
				out.close();
				return;
			}

			Enumeration modelEnum = models.elements();
			while(modelEnum.hasMoreElements())
			{
				HistoryModel model = (HistoryModel)modelEnum
					.nextElement();

				out.write('[');
				out.write(model.getName());
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

			out.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
	} //}}}

	//{{{ Private members
	private static final String TO_ESCAPE = "\n\t\\\"'[]";
	private String name;
	private int max;
	private Vector data;
	private static Hashtable models;
	//}}}
}
