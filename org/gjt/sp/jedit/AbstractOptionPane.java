/*
 * AbstractOptionPane.java - Abstract option pane
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;

/**
 * The default implementation of the option pane interface. It lays out
 * components in a vertical fashion.
 *
 * @see org.gjt.sp.jedit.OptionPane
 */
public abstract class AbstractOptionPane extends JPanel implements OptionPane
{
	/**
	 * Creates a new option pane.
	 * @param name The internal name
	 */
	public AbstractOptionPane(String name)
	{
		this.name = name;
		setLayout(gridBag = new GridBagLayout());
	}

	/**
	 * Returns the internal name of this option pane.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the component that should be displayed for this option pane.
	 * Because this class extends Component, it simply returns "this".
	 */
	public Component getComponent()
	{
		return this;
	}

	public void init()
	{
		if(!initialized)
		{
			initialized = true;
			_init();
		}
	}

	public void save()
	{
		if(initialized)
			_save();
	}

	// protected members
	/**
	 * Has the option pane been initialized?
	 */
	protected boolean initialized;

	/**
	 * The layout manager.
	 */
	protected GridBagLayout gridBag;

	/**
	 * The number of components already added to the layout manager.
	 */
	protected int y;

	/**
	 * This method should create the option pane's GUI.
	 */
	protected void _init() {}

	/**
	 * Called when the options dialog's "ok" button is clicked.
	 * This should save any properties being edited in this option
	 * pane.
	 */
	protected void _save() {}

	/**
	 * Adds a labeled component to the option pane. Components are
	 * added in a vertical fashion, one per row. The label is
	 * displayed to the left of the component.
	 * @param label The label
	 * @param comp The component
	 */
	protected void addComponent(String label, Component comp)
	{
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		cons.weightx = 0.0f;
		cons.fill = GridBagConstraints.BOTH;

		JLabel l = new JLabel(label,SwingConstants.RIGHT);
		l.setBorder(new EmptyBorder(0,0,0,12));
		gridBag.setConstraints(l,cons);
		add(l);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		gridBag.setConstraints(comp,cons);
		add(comp);
	}

	/**
	 * Adds a component to the option pane. Components are
	 * added in a vertical fashion, one per row.
	 * @param comp The component
	 */
	protected void addComponent(Component comp)
	{
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		cons.weightx = 1.0f;

		gridBag.setConstraints(comp,cons);
		add(comp);
	}

	/**
	 * Adds a separator component.
	 * @param label The separator label property
	 * @since jEdit 2.6pre2
	 */
	protected void addSeparator(String label)
	{
		Box box = new Box(BoxLayout.X_AXIS);
		Box box2 = new Box(BoxLayout.Y_AXIS);
		box2.add(Box.createGlue());
		box2.add(new JSeparator(JSeparator.HORIZONTAL));
		box2.add(Box.createGlue());
		box.add(box2);
		JLabel l = new JLabel(jEdit.getProperty(label));
		l.setMaximumSize(l.getPreferredSize());
		box.add(l);
		Box box3 = new Box(BoxLayout.Y_AXIS);
		box3.add(Box.createGlue());
		box3.add(new JSeparator(JSeparator.HORIZONTAL));
		box3.add(Box.createGlue());
		box.add(box3);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.BOTH;
		cons.anchor = GridBagConstraints.WEST;
		cons.weightx = 1.0f;

		gridBag.setConstraints(box,cons);
		add(box);
	}

	// private members
	private String name;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:37:00  spestov
 * Initial revision
 *
 * Revision 1.7  2000/08/10 08:30:40  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.6  2000/08/05 07:16:11  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.5  2000/07/15 10:10:17  sp
 * improved printing
 *
 * Revision 1.4  2000/04/16 08:56:24  sp
 * Option pane updates
 *
 * Revision 1.3  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.2  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.1  1999/10/04 03:20:50  sp
 * Option pane change, minor tweaks and bug fixes
 *
 */
