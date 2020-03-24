/*
 * OptionGroupPane.java - A Pane (view) for displaying/selecting OptionGroups.
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:
 *
 * Copyright (C) 2005 Slava Pestov
 * Copyright (C) 2011 Alan Ezust 
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
package org.jedit.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.gjt.sp.util.StringModel;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionsDialog.PaneNameRenderer;
import org.gjt.sp.util.Log;


/**
 * An option pane for displaying groups of options. There is code here
 * which was taken from OptionsDialog, but this class is a component which can
 * be embedded in other Dialogs.
 * 
 * Shows a JTree on the left, and an option pane on the right, with a splitter
 * between. 
 * 
 * @see org.gjt.sp.jedit.gui.OptionsDialog OptionsDialog
 * 
 * @author ezust
 * 
 */
public class OptionGroupPane extends AbstractOptionPane implements TreeSelectionListener
{
	// {{{ Members
	OptionGroup optionGroup;
	JSplitPane splitter;
	JTree paneTree;
	OptionPane currentPane;
	OptionTreeModel optionTreeModel;
	Map<Object, OptionPane> deferredOptionPanes;
	JPanel stage;

	StringModel title = new StringModel();
	// }}}

	public OptionGroupPane(OptionGroup group)
	{
		super(group.getName());
		optionGroup = group;

		init();
	}

	void addTextListener(TextListener l)
	{
		title.addTextListener(l);
	}

	public String getTitle() 
	{
		return title.toString();
	}

	void setTitle(String newTitle)
	{
		title.setText(newTitle);
	}

	// {{{ valueChanged() method
	@Override
	public void valueChanged(TreeSelectionEvent evt)
	{
		TreePath path = evt.getPath();

		if (path == null)
			return;

		Object lastPathComponent = path.getLastPathComponent();
		if (!(lastPathComponent instanceof String || lastPathComponent instanceof OptionPane))
		{
			return;
		}

		Object[] nodes = path.getPath();

		StringBuilder sb = new StringBuilder();

		OptionPane optionPane = null;

		int lastIdx = nodes.length - 1;

		for (int i = paneTree.isRootVisible() ? 0 : 1; i <= lastIdx; i++)
		{
			String label;
			Object node = nodes[i];
			if (node instanceof OptionPane)
			{
				optionPane = (OptionPane) node;
				label = jEdit.getProperty("options." + optionPane.getName()
					+ ".label");
			}
			else if (node instanceof OptionGroup)
			{
				label = ((OptionGroup) node).getLabel();
			}
			else if (node instanceof String)
			{
				label = jEdit.getProperty("options." + node + ".label");
				optionPane = (OptionPane) deferredOptionPanes.get((String) node);
				if (optionPane == null)
				{
					String propName = "options." + node + ".code";
					String code = jEdit.getProperty(propName);
					if (code != null)
					{
						optionPane = (OptionPane) BeanShell.eval(jEdit
							.getActiveView(), BeanShell.getNameSpace(),
							code);

						if (optionPane != null)
						{
							deferredOptionPanes.put(node, optionPane);
						}
						else
							continue;
					}
					else
					{
						Log.log(Log.ERROR, this, propName + " not defined");
						continue;
					}
				}
			}
			else
			{
				continue;
			}
			if (label != null)
				sb.append(label);

			if (i > 0 && i < lastIdx)
				sb.append(": ");
		}

		if (optionPane == null)
			return;
		
		String ttext = jEdit.getProperty("optional.title-template", new Object[] {
			optionGroup.getName(), sb.toString() });
		setTitle(ttext);

		try
		{
			optionPane.init();
		}
		catch (Throwable t)
		{
			Log.log(Log.ERROR, this, "Error initializing option pane:");
			Log.log(Log.ERROR, this, t);
		}

		if (currentPane != null)
			stage.remove(currentPane.getComponent());
		currentPane = optionPane;
		stage.add(BorderLayout.CENTER, currentPane.getComponent());
		stage.revalidate();
		stage.repaint();

		currentPane = optionPane;
	} // }}}

        // {{{ selectPane() methods
	private boolean selectPane(OptionGroup node, String name)
	{
		return selectPane(node, name, new ArrayList<>());
	} 

	private boolean selectPane(OptionGroup node, String name, ArrayList<Object> path)
	{
		path.add(node);

		Enumeration<Object> e = node.getMembers();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof OptionGroup)
			{
				OptionGroup grp = (OptionGroup) obj;
				if (grp.getName().equals(name))
				{
					path.add(grp);
					path.add(grp.getMember(0));
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);

					return true;
				}
				else if (selectPane((OptionGroup) obj, name, path))
				{
					return true;
				}
			}
			else if (obj instanceof OptionPane)
			{
				OptionPane pane = (OptionPane) obj;
				if (pane.getName().equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);

					return true;
				}
			}
			else if (obj instanceof String)
			{
				String pane = (String) obj;
				if (pane.equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					try {
						paneTree.setSelectionPath(treePath);
					}
					catch (NullPointerException npe) {}

					return true;
				}
			}
		}

		path.remove(node);

		return false;
	} // }}}

	// {{{ init() method
	@Override
	protected void _init()
	{
		setLayout(new BorderLayout());
		deferredOptionPanes = new HashMap<Object, OptionPane>();
		optionTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) optionTreeModel.getRoot();

		// #3608324: ignore the root node of the option group as it does not provide
		// a label and only add its children
		for (Enumeration<Object> members = optionGroup.getMembers(); members.hasMoreElements();)
		{
			Object member = members.nextElement();
			if (member instanceof OptionGroup)
				rootGroup.addOptionGroup((OptionGroup)member);
			else if (member instanceof String)
				rootGroup.addOptionPane((String)member);
			// TODO are there any other cases that must handled?
		}

		paneTree = new JTree(optionTreeModel);
		paneTree.setRowHeight(0);
		paneTree.setRootVisible(false);
		paneTree.setCellRenderer(new PaneNameRenderer());
		
		JPanel content = new JPanel(new BorderLayout(12, 12));
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		add(content, BorderLayout.CENTER);

		stage = new JPanel(new BorderLayout());

		// looks bad with the OS X L&F, apparently...
		if (!OperatingSystem.isMacOSLF())
			paneTree.putClientProperty("JTree.lineStyle", "Angled");

		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);
		
		JScrollPane scroller = new JScrollPane(paneTree,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setMinimumSize(new Dimension(120, 0));
		JScrollPane scroll = new JScrollPane(stage);
		scroll.getVerticalScrollBar().setUnitIncrement(10);
		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroller, scroll);
		content.add(splitter, BorderLayout.CENTER);

		// register the Options dialog as a TreeSelectionListener.
		// this is done before the initial selection to ensure that the
		// first selected OptionPane is displayed on startup.
		paneTree.getSelectionModel().addTreeSelectionListener(this);

		OptionGroup rootNode = (OptionGroup) paneTree.getModel().getRoot();
		String name = optionGroup.getName();
		String pane = jEdit.getProperty(name + ".last");
		selectPane(rootNode, pane);
		paneTree.setVisibleRowCount(1);
		
		int dividerLocation = jEdit.getIntegerProperty(name + ".splitter", -1);
		if (dividerLocation != -1)
			splitter.setDividerLocation(dividerLocation);
		else splitter.setDividerLocation(paneTree.getPreferredSize().width
					+ scroller.getVerticalScrollBar().getPreferredSize().width);

	} //}}}

	//{{{ save() methods
	@Override
	protected void _save()
	{
		if (currentPane != null)
			jEdit.setProperty(getName() + ".last", currentPane.getName());
		int dividerPosition = splitter.getDividerLocation();
		jEdit.setIntegerProperty(optionGroup.getName() + ".splitter", dividerPosition);
		save(optionGroup);
	}

	private void save(Object obj)
	{
		if (obj instanceof OptionGroup)
		{
			OptionGroup grp = (OptionGroup) obj;
			Enumeration<Object> members = grp.getMembers();
			while (members.hasMoreElements())
			{
				save(members.nextElement());
			}
		}
		else if (obj instanceof OptionPane)
		{
			try
			{
				((OptionPane) obj).save();
			}
			catch (Throwable t)
			{
				Log.log(Log.ERROR, this, "Error saving options:");
				Log.log(Log.ERROR, this, t);
			}
		}
		else if (obj instanceof String)
		{
			save(deferredOptionPanes.get(obj));
		}
	} // }}}

}
