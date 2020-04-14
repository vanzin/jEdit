/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.manager;

import org.gjt.sp.jedit.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * This class contains view management code, those methods are not public and must only be used by jEdit.
 * The public interface is {@link ViewManager}
 *
 * @author Matthieu Casanova
 * @since 5.6pre1
 * @version $Id: jEdit.java 25120 2020-04-03 14:58:39Z kpouer $
 */
public class ViewManagerImpl implements ViewManager
{
	public static final View[] EMPTY_VIEW_ARRAY = new View[0];

	// view link list
	private int viewCount;
	private View viewsFirst;
	private View viewsLast;
	private View activeView;

	//{{{ getViews() method
	/**
	 * Returns an array of all open views.
	 */
	@Override
	public List<View> getViews()
	{
		List<View> buffers = new ArrayList<>(viewCount);
		forEach(buffers::add);
		return buffers;
	} //}}}

	//{{{ forEach() method
	/**
	 * Performs the given action for each view.
	 *
	 * @param action The action to be performed for each element
	 * @throws NullPointerException if the specified action is null
	 */
	@Override
	public void forEach(Consumer<? super View> action)
	{
		View view = viewsFirst;
		for(int i = 0; i < viewCount; i++)
		{
			action.accept(view);
			view = view.getNext();
		}
	} //}}}

	//{{{ size() method
	/**
	 * Returns the number of open views.
	 */
	@Override
	public int size()
	{
		return viewCount;
	} //}}}

	//{{{ getFirst() method
	@Override
	public View getFirst()
	{
		return viewsFirst;
	} //}}}

	//{{{ getLast() method
	@Override
	public View getLast()
	{
		return viewsLast;
	} //}}}

	//{{{ getActiveView() method
	/**
	 * Returns the currently focused view.
	 */
	@Override
	public View getActiveView()
	{
		if(activeView == null)
		{
			// eg user just closed a view and didn't focus another
			return viewsFirst;
		}
		else
			return activeView;
	} //}}}

	@Override
	public void setActiveView(View view)
	{
		activeView = view;
	}

	//{{{ getActiveViewInternal() method
	/**
	 * Returns the internal active view, which might be null.
	 */
	@Override
	@Nullable
	public View getActiveViewInternal()
	{
		return activeView;
	} //}}}

	//{{{ addViewToList() method
	public void addViewToList(View view)
	{
		viewCount++;

		if(viewsFirst == null)
			viewsFirst = viewsLast = view;
		else
		{
			view.setPrev(viewsLast);
			viewsLast.setNext(view);
			viewsLast = view;
		}
	} //}}}

	//{{{ removeViewFromList() method
	public void remove(View view)
	{
		viewCount--;

		if(viewsFirst == viewsLast)
		{
			viewsFirst = viewsLast = null;
			return;
		}

		if(view == viewsFirst)
		{
			viewsFirst = view.getNext();
			view.getNext().setPrev(null);
		}
		else
		{
			view.getPrev().setNext(view.getNext());
		}

		if(view == viewsLast)
		{
			viewsLast = viewsLast.getPrev();
			view.getPrev().setNext(null);
		}
		else
		{
			view.getNext().setPrev(view.getPrev());
		}
	} //}}}
}
