/*
 * AbstractOptionPane.java - Abstract option pane
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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

//{{{ Imports
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
//}}}

/**
 * The default implementation of the option pane interface. It lays out
 * components in a vertical fashion.
 *
 * @see org.gjt.sp.jedit.OptionPane
 */
public abstract class AbstractOptionPane extends JPanel implements OptionPane
{
	//{{{ AbstractOptionPane constructor
	/**
	 * Creates a new option pane.
	 * @param name The internal name. The option pane's label is set to the
	 * value of the property named <code>options.<i>name</i>.label</code>.
	 */
	public AbstractOptionPane(String name)
	{
		this.name = name;
		setLayout(gridBag = new GridBagLayout());
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the internal name of this option pane. The option pane's label
	 * is set to the value of the property named
	 * <code>options.<i>name</i>.label</code>.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getComponent() method
	/**
	 * Returns the component that should be displayed for this option pane.
	 * Because this class extends Component, it simply returns "this".
	 */
	public Component getComponent()
	{
		return this;
	} //}}}

	//{{{ init() method
	public void init()
	{
		if(!initialized)
		{
			initialized = true;
			_init();
		}
	} //}}}

	//{{{ save() method
	public void save()
	{
		if(initialized)
			_save();
	} //}}}

	//{{{ Protected members

	//{{{ Instance variables
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
	//}}}

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

	//{{{ addComponent() method
	/**
	 * Adds a labeled component to the option pane. Components are
	 * added in a vertical fashion, one per row. The label is
	 * displayed to the left of the component.
	 * @param label The label
	 * @param comp The component
	 */
	public void addComponent(String label, Component comp)
	{
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		cons.weightx = 0.0f;
		cons.insets = new Insets(1,0,1,0);
		cons.fill = GridBagConstraints.BOTH;

		JLabel l = new JLabel(label,SwingConstants.RIGHT);
		l.setBorder(new EmptyBorder(0,0,0,12));
		gridBag.setConstraints(l,cons);
		add(l);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		gridBag.setConstraints(comp,cons);
		add(comp);
	} //}}}

	//{{{ addComponent() method
	/**
	 * Adds a component to the option pane. Components are
	 * added in a vertical fashion, one per row.
	 * @param comp The component
	 */
	public void addComponent(Component comp)
	{
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = 1;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		cons.weightx = 1.0f;
		cons.insets = new Insets(1,0,1,0);

		gridBag.setConstraints(comp,cons);
		add(comp);
	} //}}}

	//{{{ addSeparator() method
	/**
	 * Adds a separator component.
	 * @param label The separator label property
	 * @since jEdit 2.6pre2
	 */
	public void addSeparator(String label)
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
		cons.insets = new Insets(1,0,1,0);

		gridBag.setConstraints(box,cons);
		add(box);
	} //}}}

	//}}}

	//{{{ Private members
	private String name;
	//}}}
}
