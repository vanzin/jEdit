/*
 * ResourceCache.java - Caches actions.xml, dockables.xml and other stuff
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

import java.net.URL;
import java.util.*;
import org.gjt.sp.jedit.gui.DockableWindowManager;

/**
 * Caches various built-in and plugin resources so we don't have to load them
 * each time.
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
class ResourceCache
{
	//{{{ loadCache() method
	static boolean loadCache()
	{
		return false;
	} //}}}

	//{{{ generateCache() method
	static void generateCache()
	{
		ActionSet builtInActionSet = new ActionSet(null,
			ResourceCache.class.getResource("actions.xml"),
			null);
		builtInActionSet.load();

		jEdit.addActionSet(builtInActionSet);
		jEdit.setBuiltInActionSet(builtInActionSet);

		DockableWindowManager.loadDockableWindows(null,
			ResourceCache.class.getResource("dockables.xml"));
	} //}}}

	//{{{ PluginCacheEntry class
	static class PluginCacheEntry
	{
		URL       actionsURI;
		String[]  cachedActionNames;
		URL       dockablesURI;
		String[]  cachedDockableNames;
		boolean[] cachedDockableActionFlags;
	} //}}}
}
