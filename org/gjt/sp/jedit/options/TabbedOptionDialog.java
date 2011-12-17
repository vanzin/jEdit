/*
 * TabbedOptionDialog.java - Plugin options model
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov, Alan Ezust
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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.EnhancedDialog;

// {{{ TabbedOptionDialog class
/**
 * Replacement for OptionsDialog. It uses the OptionGroupPane instead of
 * managing its own optons.
 * 
 * This class should eventually replace
 * 
 * @ref OptionsDialog
 * 
 * @author ezust
 * 
 */

public class TabbedOptionDialog extends EnhancedDialog implements ActionListener, ChangeListener
{
	// {{{ Members
	JTabbedPane tabs;

	LinkedList<OptionPane> panes;
	Set<OptionPane> shownPanes;

	private JButton ok;

	private JButton cancel;

	private JButton apply;
	// }}}
	
	// {{{ TabbedOptionDialog constructor
	public TabbedOptionDialog(Frame frame, String name)
	{
		super(frame, jEdit.getProperty(name + ".title"), true);
		setName(name);
		setupTabs();

	} // }}}

	// {{{ OptionsDialog constructor
	public TabbedOptionDialog(Dialog dialog, String name)
	{
		super(dialog, jEdit.getProperty(name + ".title"), true);
		setName(name);
		setupTabs();

	} // }}}

	// {{{ setSelectedIndex()
	void setSelectedIndex(int index)
	{
		tabs.setSelectedIndex(index);
		
	} // }}}
	
	// {{{ setupTabs()
	void setupTabs()
	{
		panes = new LinkedList<OptionPane>();
		shownPanes = new HashSet<OptionPane>();
		tabs = new JTabbedPane();
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12, 12, 12, 12));

		content.add(tabs, BorderLayout.CENTER);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(6));
		getRootPane().setDefaultButton(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		buttons.add(Box.createHorizontalStrut(6));
		apply = new JButton(jEdit.getProperty("common.apply"));
		apply.addActionListener(this);
		buttons.add(apply);

		buttons.add(Box.createGlue());

		content.add(buttons, BorderLayout.SOUTH);
		setContentPane(content);
		GUIUtilities.loadGeometry(this, getName());
		tabs.addChangeListener(this);

	} // }}}

	// {{{ actionPerformed()
	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();

		if (source == ok)
		{
			ok();
		}
		else if (source == cancel)
		{
			cancel();
		}
		else if (source == apply)
		{
			ok(false);
		}
	} // }}}

	// {{{ addOptionPane()
	public void addOptionPane(OptionPane pane)
	{
		panes.add(pane);
		JPanel panel = (JPanel) pane;
		tabs.addTab(pane.getName(), panel);

	} // }}}

	// {{{ addOptionGroup()
	public void addOptionGroup(OptionGroup group)
	{
		OptionGroupPane pane = new OptionGroupPane(group);
		pane.addTextListener(new TitleChanger());
		addOptionPane(pane);
	} // }}}

	// {{{ ok() 
	public void ok()
	{
		ok(true);
	}

	public void ok(boolean dispose)
	{
		GUIUtilities.saveGeometry(this, getName());

		Iterator<OptionPane> itr = panes.iterator();
		while (itr.hasNext())
		{
			OptionPane op = itr.next();
			if (shownPanes.contains(op))
				op.save();
		}
		Point p = getLocation();
		Dimension d = getSize();

		/* This will fire the PROPERTIES_CHANGED event */
		jEdit.propertiesChanged();

		// Save settings to disk
		jEdit.saveSettings();

		// get rid of this dialog if necessary
		if (dispose)
			dispose();
	} // }}}

	// {{{ cancel()
	public void cancel()
	{
		GUIUtilities.saveGeometry(this, getName());

		dispose();
	} // }}}

	// {{{ TitleChanger class
	class TitleChanger implements TextListener
	{

		public void textValueChanged(TextEvent e)
		{
			setTitle(e.getSource().toString());
		}

	}

	// {{{ stateChanged()
	public void stateChanged(ChangeEvent e)
	{

		OptionPane op = (OptionPane) tabs.getSelectedComponent();
		shownPanes.add(op);
		jEdit.setIntegerProperty("optional.last.tab", tabs.getSelectedIndex());
		setTitle(op.getName());
	} // }}}

} // }}}
