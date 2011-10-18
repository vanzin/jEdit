/*
 * HistoryModel.java - History list model
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
 * Copyright (C) 2010 Eric Le Lay
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
 * The max size of the history is defined globally via setDefaultMax(),
 *  see jEdit.java for instance.
 * It may be locally overriden by calling setMax() on a HistoryModel instance.
 *
 * @author Slava Pestov
 * @author Eric Le Lay
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
		this.max = -1;
		this.maxSize = -1;
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
		
		// Don't add duplicates
		int index = indexOf(text);
		if (index == 0)
			return;
		if (index != -1)
			removeElementAt(index);
		
		// Make room so that adding this new item doesn't cause the history model to run
		// over its maximum size
		int myMaxSize = (maxSize == -1) ? defaultMaxSize : maxSize;
		if (text.length() > myMaxSize)
		{
			return;
		}
		int currentSize = getCurrentSize();
		while (currentSize + text.length() > myMaxSize)
		{
			currentSize -= getItem(getSize() - 1).length();
			removeElementAt(getSize() - 1);
		}

		insertElementAt(text,0);

		// use the local max unless it's not set
		int myMax = max>=0 ? max : defaultMax;
		while(getSize() > myMax)
			removeElementAt(getSize() - 1);
	} //}}}

	//{{{ insertElementAt() method
	@Override
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
	@Override
	public boolean removeElement(Object obj)
	{
		modified = true;
		return super.removeElement(obj);
	} //}}}

	//{{{ removeAllElements() method
	@Override
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
	/**
	 * sets the maximum size of this history
	 * @param max the new maximum size of this history of -1 to restore default
	 */
	public void setMax(int max)
	{
		this.max = max;
	} //}}}

	//{{{ getMax() method
	/**
	 * @return maximum size of this history or -1 is it's the default size
	 */
	public int getMax()
	{
		return max;
	} //}}}
	
	//{{{ setMaxSize() method
	/**
	 * Sets the maximum size (in characters) for this history model
	 * @param max the new maximum size for the history model, or -1 to restore the default
	 * @since jEdit 4.5pre1
	 */
	public void setMaxSize(int max)
	{
		this.maxSize = max;
	} //}}}

	//{{{ getMaxSize() method
	/**
	 * @return the maximum size (in characters) for this history model, or -1 for the default
	 * @since jEdit 4.5pre1
	 */
	public int getMaxSize()
	{
		return maxSize;
	} //}}}

	//{{{ setDefaultMax() method
	/**
	 * Sets the default size of all HistoryModels.
	 * Affects the VFS path history, the hypersearch history, etc..
	 * To change the max size of one history, call setMax() instead.
	 */
	public static void setDefaultMax(int max)
	{
		HistoryModel.defaultMax = max;
	} //}}}

	//{{{ getDefaultMax() method
	/**
	 * Gets the default size of all HistoryModels.
	 * @return default size limit for HistoryModels
	 */
	public static int getDefaultMax()
	{
		return HistoryModel.defaultMax;
	} //}}}
	
	//{{{ setDefaultMaxSize() method
	/**
	 * Sets the default max size (in characters) for all history models. To change the max
	 * size of one history, call {@link #setMaxSize} instead.
	 * @since jEdit 4.5pre1
	 */
	public static void setDefaultMaxSize(int newMax)
	{
		HistoryModel.defaultMaxSize = newMax;
	} //}}}
	
	//{{{ getDefaultMaxSize() method
	/**
	 * Gets the default maximum size (in characters) for all history models.
	 * @since jEdit 4.5pre1
	 */
	public static int getDefaultMaxSize()
	{
		return HistoryModel.defaultMaxSize;
	} //}}}

	//{{{ setSaver() method
	public static void setSaver(HistoryModelSaver saver)
	{
		HistoryModel.saver = saver;
	} //}}}

	//{{{ Private members
	private int max;
	private static int defaultMax;
	
	private int maxSize;
	private static int defaultMaxSize;

	private final String name;
	private static Map<String, HistoryModel> models;

	private static boolean modified;
	private static HistoryModelSaver saver;
	
	//{{{ getCurrentSize() method
	/**
	 * Gets the current size (in characters) of the entire history model.
	 */
	private int getCurrentSize()
	{
		int currentSize = 0;
		for (int i = 0; i < getSize(); i++)
		{
			currentSize += getItem(i).length();
		}
		
		return currentSize;
	}
	//}}}

	//}}}
}
