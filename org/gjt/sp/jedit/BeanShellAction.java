/*
 * BeanShellAction.java - BeanShell action
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

import java.awt.event.ActionEvent;
import java.awt.*;

public class BeanShellAction extends EditAction
{
	public BeanShellAction(String name, boolean plugin, String code,
		String isSelected, boolean noRepeat, boolean noRecord)
	{
		super(name,plugin);

		this.code = code;
		this.isSelected = isSelected;
		this.noRepeat = noRepeat;
		this.noRecord = noRecord;

		/* Some characters that we like to use in action names
		 * ('.', '-') are not allowed in BeanShell identifiers. */
		sanitizedName = name.replace('.','_').replace('-','_');
	}

	public void invoke(View view)
	{
		if(cachedCode == null)
		{
			String cachedCodeName = "action_" + sanitizedName;
			cachedCode = BeanShell.cacheBlock(cachedCodeName,code,true);
		}
		BeanShell.runCachedBlock(cachedCode,view,null);
	}

	public boolean isToggle()
	{
		return isSelected != null;
	}

	public boolean isSelected(View view)
	{
		if(isSelected == null)
			return false;

		if(cachedIsSelected == null)
		{
			String cachedIsSelectedName = "selected_" + sanitizedName;
			cachedIsSelected = BeanShell.cacheBlock(cachedIsSelectedName,
				isSelected,true);
		}

		return Boolean.TRUE.equals(BeanShell.runCachedBlock(cachedIsSelected,
			view,null));
	}

	public boolean noRepeat()
	{
		return noRepeat;
	}

	public boolean noRecord()
	{
		return noRecord;
	}

	public String getCode()
	{
		return code.trim();
	}

	// private members
	private boolean noRepeat;
	private boolean noRecord;
	private String code;
	private String isSelected;
	private String cachedCode;
	private String cachedIsSelected;
	private String sanitizedName;
}
