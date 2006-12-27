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
import java.util.*;
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
			models = Collections.synchronizedMap(new HashMap<String, HistoryModel>());

		HistoryModel model = models.get(name);
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
		if (saver != null)
			models = saver.load(models);
	} //}}}

	//{{{ saveHistory() method
	public static void saveHistory()
	{
		if (saver != null && modified && saver.save(models))
			modified = false;
	} //}}}

	//{{{ setMax() method
	public static void setMax(int max)
	{
		HistoryModel.max = max;
	} //}}}

	//{{{ setSaver() method
	public static void setSaver(HistoryModelSaver saver)
	{
		HistoryModel.saver = saver;
	} //}}}

	//{{{ Private members
	private static int max;

	private String name;
	private static Map<String, HistoryModel> models;

	private static boolean modified;
	private static HistoryModelSaver saver;
	//}}}
}
