/*
 * GlobalOptions.java - Global options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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

package org.gjt.sp.jedit.options;

//{{{ Imports
import java.awt.Dialog;
import java.awt.Frame;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.PropertiesChanging;
import org.gjt.sp.jedit.*;
//}}}
/**
   @deprecated - use CombinedOptinos
*/
public class GlobalOptions extends OptionsDialog
{
	//{{{ GlobalOptions constructor
	public GlobalOptions(Frame frame)
	{
		super(frame,"options",jEdit.getProperty("options.last"));
	} //}}}

	//{{{ GlobalOptions constructor
	public GlobalOptions(Frame frame, String pane)
	{
		super(frame,"options",pane);
	} //}}}

	//{{{ GlobalOptions constructor
	public GlobalOptions(Dialog dialog)
	{
		super(dialog,"options",jEdit.getProperty("options.last"));
	} //}}}

	//{{{ GlobalOptions constructor
	public GlobalOptions(Dialog dialog, String pane)
	{
		super(dialog,"options",pane);
	} //}}}

	//{{{ createOptionTreeModel() method
	@Override
	protected OptionTreeModel createOptionTreeModel()
	{
		OptionTreeModel paneTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) paneTreeModel.getRoot();

		String optionGroups = jEdit.getProperty("options.groups");
		String[] groups = optionGroups.split(" ");
		for (String group : groups)
		{
			OptionGroup optionGroup = new OptionGroup(group);
			String optionPanes = jEdit.getProperty("options.group." + group);
			String[] panes = optionPanes.split(" ");
			for (String pane : panes)
			{
				optionGroup.addOptionPane(pane);
			}
			rootGroup.addOptionGroup(optionGroup);
		}
		return paneTreeModel;
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		EditBus.send(
			new PropertiesChanging(null,
				PropertiesChanging.State.CANCELED));
		super.cancel();
	} //}}}

	//{{{ init() method
	@Override
	protected void init(String name, String pane)
	{
		EditBus.send(
			new PropertiesChanging(null,
				PropertiesChanging.State.LOADING));
		super.init(name, pane);
	} //}}}

	//{{{ getDefaultGroup() method
	@Override
	protected OptionGroup getDefaultGroup()
	{
		return null;
	} //}}}
}
