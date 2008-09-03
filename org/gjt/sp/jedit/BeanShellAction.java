/*
 * BeanShellAction.java - BeanShell action
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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

import org.gjt.sp.jedit.bsh.*;
import java.lang.ref.SoftReference;
import java.awt.Component;
import org.gjt.sp.jedit.gui.BeanShellErrorDialog;
import org.gjt.sp.util.Log;

/**
 * An action that evaluates BeanShell code when invoked. BeanShell actions are
 * usually loaded from <code>actions.xml</code> and
 * <code>browser.actions.xml</code> files; see {@link ActionSet} for syntax
 * information.
 *
 * @see jEdit#getAction(String)
 * @see jEdit#getActionNames()
 * @see ActionSet
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class BeanShellAction extends EditAction
{
	//{{{ BeanShellAction constructor
	public BeanShellAction(String name, String code, String isSelected,
		boolean noRepeat, boolean noRecord, boolean noRememberLast)
	{
		super(name);

		/* Some characters that we like to use in action names
		 * ('.', '-') are not allowed in BeanShell identifiers. */
		String sanitizedName = name.replace('.','_').replace('-','_');
		this.code = new CachedBshMethod("action_" + sanitizedName, code);
		if (isSelected != null)
		{
			this.isSelected = new CachedBshMethod("selected_" + sanitizedName, isSelected);
		}
		this.noRepeat = noRepeat;
		this.noRecord = noRecord;
		this.noRememberLast = noRememberLast;

		jEdit.setTemporaryProperty(name + ".toggle",
			isSelected != null ? "true" : "false");
	} //}}}

	//{{{ invoke() method
	public void invoke(View view)
	{
		try
		{
			BeanShell.runCachedBlock(code.get(),view,
				new NameSpace(BeanShell.getNameSpace(),
				"BeanShellAction.invoke()"));
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,this,e);

			new BeanShellErrorDialog(view,e);
		}
	} //}}}

	//{{{ isSelected() method
	public boolean isSelected(Component comp)
	{
		if(isSelected == null)
			return false;

		NameSpace global = BeanShell.getNameSpace();

		try
		{
			View view = GUIUtilities.getView(comp);

			// undocumented hack to allow browser actions to work.
			// XXX - clean up in 4.3
			global.setVariable("_comp",comp);

			return Boolean.TRUE.equals(BeanShell.runCachedBlock(
				isSelected.get(),view,
				new NameSpace(BeanShell.getNameSpace(),
				"BeanShellAction.isSelected()")));
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,this,e);

			// dialogs fuck things up if a menu is visible, etc!
			//new BeanShellErrorDialog(view,e);

			// so that in the future we don't see streams of
			// exceptions
			isSelected = null;

			return false;
		}
		finally
		{
			try
			{
				global.setVariable("_comp",null);
			}
			catch(UtilEvalError err)
			{
				Log.log(Log.ERROR,this,err);
			}
		}
	} //}}}

	//{{{ noRepeat() method
	public boolean noRepeat()
	{
		return noRepeat;
	} //}}}

	//{{{ noRecord() method
	public boolean noRecord()
	{
		return noRecord;
	} //}}}

	//{{{ noRememberLast() method
	/**
	 * Returns if this edit action should not be remembered as the most
	 * recently invoked action.
	 * @since jEdit 4.2pre1
	 */
	public boolean noRememberLast()
	{
		return noRememberLast;
	} //}}}

	//{{{ getCode() method
	public String getCode()
	{
		return code.getSource().trim();
	} //}}}

	//{{{ Private members
	private boolean noRepeat;
	private boolean noRecord;
	private boolean noRememberLast;
	private CachedBshMethod code;
	private CachedBshMethod isSelected;

	//{{{ CachedBshMethod class
	private static class CachedBshMethod
	{
		private final String name;
		private final String source;
		private SoftReference<BshMethod> cache;

		public CachedBshMethod(String name, String source)
		{
			this.name = name;
			this.source = source;
			this.cache = null;
		}

		public BshMethod get() throws java.lang.Exception
		{
			if (cache != null)
			{
				BshMethod cached = cache.get();
				if (cached != null)
				{
					return cached;
				}
			}
			BshMethod newOne = BeanShell.cacheBlock(name, source, true);
			cache = new SoftReference<BshMethod>(newOne);
			return newOne;
		}

		public String getSource()
		{
			return source;
		}
	}//}}}

	//}}}
}
