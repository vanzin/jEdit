/*
 * BeanShellAction.java - BeanShell action
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001 Slava Pestov
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

import bsh.BshMethod;
import org.gjt.sp.jedit.gui.BeanShellErrorDialog;
import org.gjt.sp.util.Log;

/**
 * An action that evaluates BeanShell code when invoked.
 * @author Slava Pestov
 * @version $Id$
 */
public class BeanShellAction extends EditAction
{
	//{{{ BeanShellAction constructor
	public BeanShellAction(String name, String code, String isSelected,
		boolean noRepeat, boolean noRecord)
	{
		super(name);

		this.code = code;
		this.isSelected = isSelected;
		this.noRepeat = noRepeat;
		this.noRecord = noRecord;

		/* Some characters that we like to use in action names
		 * ('.', '-') are not allowed in BeanShell identifiers. */
		sanitizedName = name.replace('.','_').replace('-','_');
	} //}}}

	//{{{ invoke() method
	public void invoke(View view)
	{
		try
		{
			if(cachedCode == null)
			{
				String cachedCodeName = "action_" + sanitizedName;
				cachedCode = BeanShell.cacheBlock(cachedCodeName,code,false);
			}

			BeanShell.runCachedBlock(cachedCode,view,null);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,this,e);

			new BeanShellErrorDialog(view,e);
		}
	} //}}}

	//{{{ isToggle() method
	public boolean isToggle()
	{
		return isSelected != null;
	} //}}}

	//{{{ isSelected() method
	public boolean isSelected(View view)
	{
		if(isSelected == null)
			return false;

		try
		{
			if(cachedIsSelected == null)
			{
				String cachedIsSelectedName = "selected_" + sanitizedName;
				cachedIsSelected = BeanShell.cacheBlock(cachedIsSelectedName,
					isSelected,false);
			}
			return Boolean.TRUE.equals(BeanShell.runCachedBlock(
				cachedIsSelected,view,null));
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,this,e);

			new BeanShellErrorDialog(view,e);

			return false;
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

	//{{{ getCode() method
	public String getCode()
	{
		return code.trim();
	} //}}}

	//{{{ Private members
	private boolean noRepeat;
	private boolean noRecord;
	private String code;
	private String isSelected;
	private BshMethod cachedCode;
	private BshMethod cachedIsSelected;
	private String sanitizedName;
	//}}}
}
