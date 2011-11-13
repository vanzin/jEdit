/*
 * ViewOptionPane.java - Editor window options
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
 * Portions Copyright (C) 2009 Matthieu Casanova
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
import javax.swing.border.*;
import javax.swing.*;

import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.util.ComboKeyListener;
import org.gjt.sp.util.Log;
//}}}

public class ViewOptionPane extends AbstractOptionPane
{
    //{{{ ViewOptionPane constructor
	public ViewOptionPane()
	{
		super("view");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		/* View dock layout */
		layoutIcon1 = GUIUtilities.loadIcon("dock_layout1.png");
		layoutIcon2 = GUIUtilities.loadIcon("dock_layout2.png");
		layoutIcon3 = GUIUtilities.loadIcon("dock_layout3.png");
		layoutIcon4 = GUIUtilities.loadIcon("dock_layout4.png");

		JPanel layoutPanel = new JPanel(new BorderLayout(12,12));

		if(jEdit.getBooleanProperty("view.docking.alternateLayout"))
		{
			layout = new JLabel(jEdit.getBooleanProperty(
				"view.toolbar.alternateLayout")
				? layoutIcon4 : layoutIcon2);
		}
		else
		{
			layout = new JLabel(jEdit.getBooleanProperty(
				"view.toolbar.alternateLayout")
				? layoutIcon3 : layoutIcon1);
		}

		layout.setBorder(new EmptyBorder(12,12,12,12));
		layoutPanel.add(BorderLayout.CENTER,layout);

		JPanel buttons = new JPanel(new GridLayout(2,1,12,12));
		buttons.setBorder(new EmptyBorder(0,12,12,12));
		buttons.add(alternateDockingLayout = new JButton(jEdit.getProperty(
			"options.view.alternateDockingLayout")));
		ActionHandler actionHandler = new ActionHandler();
		alternateDockingLayout.addActionListener(actionHandler);
		buttons.add(alternateToolBarLayout = new JButton(jEdit.getProperty(
			"options.view.alternateToolBarLayout")));
		alternateToolBarLayout.addActionListener(actionHandler);
		layoutPanel.add(BorderLayout.SOUTH,buttons);

		TitledBorder border = new TitledBorder(jEdit.getProperty(
			"options.view.viewLayout"));
		border.setTitleJustification(TitledBorder.CENTER);
		layoutPanel.setBorder(border);

		addComponent(layoutPanel);

		/* Floatable Toolbars */
		floatableToolbars = new JCheckBox(jEdit.getProperty(
			"options.view.floatableToolbars"));
		floatableToolbars.setSelected(jEdit.getBooleanProperty(
			"view.toolbar.floatable"));
		addComponent(floatableToolbars);
		
		/* Show full path */
		showFullPath = new JCheckBox(jEdit.getProperty(
			"options.view.showFullPath"));
		showFullPath.setSelected(jEdit.getBooleanProperty(
			"view.showFullPath"));
		addComponent(showFullPath);

		/* Show search bar */
		showSearchbar = new JCheckBox(jEdit.getProperty(
			"options.view.showSearchbar"));
		showSearchbar.setSelected(jEdit.getBooleanProperty(
			"view.showSearchbar"));
		addComponent(showSearchbar);

		/* Beep on search auto wrap */
		beepOnSearchAutoWrap = new JCheckBox(jEdit.getProperty(
			"options.view.beepOnSearchAutoWrap"));
		beepOnSearchAutoWrap.setSelected(jEdit.getBooleanProperty(
			"search.beepOnSearchAutoWrap"));
		addComponent(beepOnSearchAutoWrap);

		/* Show buffer switcher */
		showBufferSwitcher = new JCheckBox(jEdit.getProperty(
			"options.view.showBufferSwitcher"));

		showBufferSwitcher.setSelected(jEdit.getBooleanProperty(
			"view.showBufferSwitcher"));
		addComponent(showBufferSwitcher);
		showBufferSwitcher.addActionListener(actionHandler);


		/* Buffer switcher max row count */
		bufferSwitcherMaxRowCount = new JTextField(jEdit.getProperty("bufferSwitcher.maxRowCount"));
		addComponent(jEdit.getProperty("options.view.bufferSwitcherMaxRowsCount"),
			bufferSwitcherMaxRowCount);
		bufferSwitcherMaxRowCount.setEditable(showBufferSwitcher.isSelected());

		buffersetScope = new JComboBox();
		buffersetScope.addKeyListener(new ComboKeyListener(buffersetScope));
		buffersetScope.addItem(BufferSet.Scope.global);
		buffersetScope.addItem(BufferSet.Scope.view);
		buffersetScope.addItem(BufferSet.Scope.editpane);
		BufferSet.Scope scope = null;
		try
		{
			scope = BufferSet.Scope.valueOf(jEdit.getProperty("bufferset.scope"));
		}
		catch (IllegalArgumentException e)
		{
			Log.log(Log.ERROR, this, e);
			scope = BufferSet.Scope.global;
		}
		buffersetScope.setSelectedItem(scope);
		addComponent(jEdit.getProperty("options.bufferset.scope"), buffersetScope);

		/* Sort buffers */
		sortBuffers = new JCheckBox(jEdit.getProperty(
			"options.view.sortBuffers"));
		sortBuffers.setSelected(jEdit.getBooleanProperty("sortBuffers"));
		sortBuffers.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				sortByName.setEnabled(sortBuffers.isSelected());
			}
		});

		addComponent(sortBuffers);

		/* Sort buffers by names */
		sortByName = new JCheckBox(jEdit.getProperty(
			"options.view.sortByName"));
		sortByName.setSelected(jEdit.getBooleanProperty("sortByName"));
		sortByName.setEnabled(sortBuffers.isSelected());
		addComponent(sortByName);

		fullScreenIncludesMenu = new JCheckBox(jEdit.getProperty(
			"options.view.fullScreenIncludesMenu"));
		fullScreenIncludesMenu.setSelected(
			jEdit.getBooleanProperty("fullScreenIncludesMenu"));
		addComponent(fullScreenIncludesMenu);

		fullScreenIncludesToolbar = new JCheckBox(jEdit.getProperty(
			"options.view.fullScreenIncludesToolbar"));
		fullScreenIncludesToolbar.setSelected(
			jEdit.getBooleanProperty("fullScreenIncludesToolbar"));
		addComponent(fullScreenIncludesToolbar);

		fullScreenIncludesStatus = new JCheckBox(jEdit.getProperty(
				"options.view.fullScreenIncludesStatus"));
		fullScreenIncludesStatus.setSelected(
				jEdit.getBooleanProperty("fullScreenIncludesStatus"));
		addComponent(fullScreenIncludesStatus);

	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setBooleanProperty("view.docking.alternateLayout",
			layout.getIcon() == layoutIcon2
			|| layout.getIcon() == layoutIcon4);
		jEdit.setBooleanProperty("view.toolbar.alternateLayout",
			layout.getIcon() == layoutIcon3
			|| layout.getIcon() == layoutIcon4);
		jEdit.setBooleanProperty("view.showFullPath",showFullPath
			.isSelected());
		jEdit.setBooleanProperty("view.toolbar.floatable", floatableToolbars.isSelected());
		
		jEdit.setBooleanProperty("view.showSearchbar",showSearchbar
			.isSelected());
		jEdit.setBooleanProperty("search.beepOnSearchAutoWrap",beepOnSearchAutoWrap
			.isSelected());
		jEdit.setBooleanProperty("view.showBufferSwitcher",
			showBufferSwitcher.isSelected());
		jEdit.setProperty("bufferSwitcher.maxRowCount",
			bufferSwitcherMaxRowCount.getText());
		jEdit.setProperty("bufferset.scope", buffersetScope.getSelectedItem().toString());
		jEdit.setBooleanProperty("sortBuffers",sortBuffers.isSelected());
		jEdit.setBooleanProperty("sortByName",sortByName.isSelected());
		jEdit.setBooleanProperty("fullScreenIncludesMenu",fullScreenIncludesMenu.isSelected());
		jEdit.setBooleanProperty("fullScreenIncludesToolbar",fullScreenIncludesToolbar.isSelected());
		jEdit.setBooleanProperty("fullScreenIncludesStatus",fullScreenIncludesStatus.isSelected());

	} //}}}

	//{{{ Private members
	private JLabel layout;
	private Icon layoutIcon1, layoutIcon2, layoutIcon3, layoutIcon4;
	private JButton alternateDockingLayout, alternateToolBarLayout;
	private JCheckBox showFullPath;
	private JCheckBox floatableToolbars;
	private JCheckBox showSearchbar;
	private JCheckBox beepOnSearchAutoWrap;
	private JCheckBox showBufferSwitcher;
	private JTextField bufferSwitcherMaxRowCount;
	private JComboBox buffersetScope;
	private JCheckBox sortBuffers;
	private JCheckBox sortByName;
	private JCheckBox fullScreenIncludesMenu;
	private JCheckBox fullScreenIncludesToolbar;
	private JCheckBox fullScreenIncludesStatus;

	//}}}

	//{{{ ActionHandler class
	private class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == alternateDockingLayout)
			{
				if(layout.getIcon() == layoutIcon1)
					layout.setIcon(layoutIcon2);
				else if(layout.getIcon() == layoutIcon2)
					layout.setIcon(layoutIcon1);
				else if(layout.getIcon() == layoutIcon3)
					layout.setIcon(layoutIcon4);
				else if(layout.getIcon() == layoutIcon4)
					layout.setIcon(layoutIcon3);
			}
			else if(evt.getSource() == alternateToolBarLayout)
			{
				if(layout.getIcon() == layoutIcon1)
					layout.setIcon(layoutIcon3);
				else if(layout.getIcon() == layoutIcon3)
					layout.setIcon(layoutIcon1);
				else if(layout.getIcon() == layoutIcon2)
					layout.setIcon(layoutIcon4);
				else if(layout.getIcon() == layoutIcon4)
					layout.setIcon(layoutIcon2);
			}
			else if (evt.getSource() == showBufferSwitcher)
			{
				bufferSwitcherMaxRowCount.setEditable(showBufferSwitcher.isSelected());
			}
		}
	} //}}}
}
