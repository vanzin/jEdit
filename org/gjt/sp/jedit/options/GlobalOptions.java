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
import org.gjt.sp.jedit.options.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

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
	protected OptionTreeModel createOptionTreeModel()
	{
		OptionTreeModel paneTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) paneTreeModel.getRoot();

		// initialize the jEdit branch of the options tree
		jEditGroup = new OptionGroup("jedit");

		addOptionPane(new AbbrevsOptionPane(), jEditGroup);
		addOptionPane(new AppearanceOptionPane(), jEditGroup);
		addOptionPane(new ContextOptionPane(), jEditGroup);
		addOptionPane(new EditingOptionPane(), jEditGroup);
		addOptionPane(new GeneralOptionPane(), jEditGroup);
		addOptionPane(new GutterOptionPane(), jEditGroup);
		addOptionPane(new LoadSaveOptionPane(), jEditGroup);
		addOptionPane(new PrintOptionPane(), jEditGroup);
		addOptionPane(new FirewallOptionPane(), jEditGroup);
		addOptionPane(new ShortcutsOptionPane(), jEditGroup);
		addOptionPane(new StatusBarOptionPane(), jEditGroup);
		addOptionPane(new SyntaxHiliteOptionPane(), jEditGroup);
		addOptionPane(new TextAreaOptionPane(), jEditGroup);
		addOptionPane(new ToolBarOptionPane(), jEditGroup);
		addOptionGroup(jEditGroup, rootGroup);

		browserGroup = new OptionGroup("browser");
		addOptionPane(new BrowserColorsOptionPane(), browserGroup);
		addOptionPane(new BrowserOptionPane(), browserGroup);
		addOptionGroup(browserGroup, rootGroup);

		return paneTreeModel;
	} //}}}

	//{{{ getDefaultGroup() method
	protected OptionGroup getDefaultGroup()
	{
		return null;
	} //}}}

	//{{{ Private members
	private OptionGroup jEditGroup;
	private OptionGroup browserGroup;
	//}}}
}
