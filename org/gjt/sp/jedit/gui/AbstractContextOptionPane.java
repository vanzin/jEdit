/*
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;

/**
 * An abstract base class for context menu editors. Provides the base
 * UI and functionality for creating a context menu populated with
 * jEdit actions.
 *
 * @author Slava Pestov
 * @author Marcelo Vanzin
 * @version $Id$
 * @since jEdit 4.3pre13
 */
public abstract class AbstractContextOptionPane extends AbstractOptionPane
{

    /**
     * Constructor that takes a name as an argument, for use by
     * subclasses.
     *
     * @param name Name of the option pane.
     * @param caption String to use as the caption of the context menu
     *                configuration list.
     *
     * @since jEdit 4.3pre13
     */
    protected AbstractContextOptionPane(String name, String caption)
    {
        super(name);
        this.caption = new JLabel(caption);
    }

    /**
     * Initializes the pane's UI.
     */
    protected void _init()
    {
        setLayout(new BorderLayout());

        add(BorderLayout.NORTH,caption);

		listModel = new DefaultListModel();
		reloadContextList(getContextMenu());
		
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListHandler());

        add(BorderLayout.CENTER,new JScrollPane(list));

        buttons = new JPanel();
        buttons.setBorder(new EmptyBorder(3,0,0,0));
        buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
        ActionHandler actionHandler = new ActionHandler();
        add = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.add.icon")));
        add.setToolTipText(jEdit.getProperty("common.add"));
        add.addActionListener(actionHandler);
        buttons.add(add);
        buttons.add(Box.createHorizontalStrut(6));
        remove = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.remove.icon")));
        remove.setToolTipText(jEdit.getProperty("common.remove"));
        remove.addActionListener(actionHandler);
        buttons.add(remove);
        buttons.add(Box.createHorizontalStrut(6));
        moveUp = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveUp.icon")));
        moveUp.setToolTipText(jEdit.getProperty("common.moveUp"));
        moveUp.addActionListener(actionHandler);
        buttons.add(moveUp);
        buttons.add(Box.createHorizontalStrut(6));
        moveDown = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveDown.icon")));
        moveDown.setToolTipText(jEdit.getProperty("common.moveDown"));
        moveDown.addActionListener(actionHandler);
        buttons.add(moveDown);
        buttons.add(Box.createGlue());

		// add "reset to defaults" button
		reset = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.reset.icon")));
		reset.setToolTipText(jEdit.getProperty("options.context.reset"));
		reset.addActionListener(actionHandler);
		buttons.add(reset);
		
        updateButtons();
        add(BorderLayout.SOUTH,buttons);
    }

    /**
     * Returns the context menu to be edited. The default implementation
     * returns jEdit's context menu. Subclasses overriding this method
     * should also override {@link #saveContextMenu(String menu) saveContextMenu}.
     *
     * @since jEdit 4.3pre13
     */
    protected abstract String getContextMenu();

    /**
     * Saves the context menu configuration.
     *
     * @since jEdit 4.3pre13
     */
    protected abstract void saveContextMenu(String menu);

    /**
     * Adds a widget to the "buttons" panel at the bottom. The component
     * will be added at the very right of the button row (separated from
     * the normal buttons).
     *
     * @since jEdit 4.3pre13
     */
    protected void addButton(JComponent c)
    {
        buttons.add(c);
    }

    static class MenuItemCompare implements Comparator
    {
        public int compare(Object obj1, Object obj2)
        {
            return StandardUtilities.compareStrings(
                ((MenuItem)obj1).label,
                ((MenuItem)obj2).label,
                true);
        }
    }

    protected void _save()
    {
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < listModel.getSize(); i++)
        {
            if(i != 0)
                buf.append(' ');
            buf.append(((MenuItem)listModel.elementAt(i)).actionName);
        }
        saveContextMenu(buf.toString());
    }

    // private members
    private DefaultListModel listModel;
    private JList list;
    private JButton add;
    private JButton remove;
    private JButton moveUp, moveDown;
    private JButton reset;
    private JLabel caption;
    private JPanel buttons;

    private void updateButtons()
    {
        int index = list.getSelectedIndex();
        remove.setEnabled(index != -1 && listModel.getSize() != 0);
        moveUp.setEnabled(index > 0);
        moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
    }
	
	private void reloadContextList(String contextMenu)
	{
		listModel.clear();
		StringTokenizer st = new StringTokenizer(contextMenu);
		while(st.hasMoreTokens())
		{
			String actionName = st.nextToken();
			if(actionName.equals("-"))
				listModel.addElement(new AbstractContextOptionPane.MenuItem("-","-"));
			else
			{
				EditAction action = jEdit.getAction(actionName);
				if(action == null)
					continue;
				String label = action.getLabel();
				if(label == null)
					continue;
				listModel.addElement(new AbstractContextOptionPane.MenuItem(actionName,label));
			}
		}
	}

    static class MenuItem
    {
        String actionName;
        String label;

        MenuItem(String actionName, String label)
        {
            this.actionName = actionName;
            this.label = GUIUtilities.prettifyMenuLabel(label);
        }

        public String toString()
        {
            return label;
        }
    }

    class ActionHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            Object source = evt.getSource();

            if(source == add)
            {
                ContextAddDialog dialog = new ContextAddDialog(
                    AbstractContextOptionPane.this);
                String selection = dialog.getSelection();
                if(selection == null)
                    return;

                int index = list.getSelectedIndex();
                if(index == -1)
                    index = listModel.getSize();
                else
                    index++;

                MenuItem menuItem;
                if(selection.equals("-"))
                    menuItem = new AbstractContextOptionPane.MenuItem("-","-");
                else
                {
                    menuItem = new AbstractContextOptionPane.MenuItem(selection,
                        jEdit.getAction(selection)
                        .getLabel());
                }

                listModel.insertElementAt(menuItem,index);
                list.setSelectedIndex(index);
                list.ensureIndexIsVisible(index);
            }
            else if(source == remove)
            {
                int index = list.getSelectedIndex();
                listModel.removeElementAt(index);
                if(listModel.getSize() != 0)
                {
                    list.setSelectedIndex(
                        Math.min(listModel.getSize()-1,
                        index));
                }
                updateButtons();
            }
            else if(source == moveUp)
            {
                int index = list.getSelectedIndex();
                Object selected = list.getSelectedValue();
                listModel.removeElementAt(index);
                listModel.insertElementAt(selected,index-1);
                list.setSelectedIndex(index-1);
                list.ensureIndexIsVisible(index - 1);
            }
            else if(source == moveDown)
            {
                int index = list.getSelectedIndex();
                Object selected = list.getSelectedValue();
                listModel.removeElementAt(index);
                listModel.insertElementAt(selected,index+1);
                list.setSelectedIndex(index+1);
                list.ensureIndexIsVisible(index+1);
            }
			else if(source == reset)
			{
				String dialogType = "options.context.reset.dialog";
				int result = GUIUtilities.confirm(list,dialogType,null,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				
				if(result == JOptionPane.YES_OPTION)
				{
					// the user should be able to cancel the options dialog 
					// so we need to modify the list, not the actual property
					// since the default value is not available, 
					// we reset, fetch default value and re-set to original
					String orgContext = jEdit.getProperty("view.context");
					jEdit.resetProperty("view.context");
					String defaultContext = jEdit.getProperty("view.context");
					jEdit.setProperty("view.context", orgContext);
					reloadContextList(defaultContext);
					
					// reset selection if user had more buttons than default
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					updateButtons();
				}
			}
        }
    }

    class ListHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent evt)
        {
            updateButtons();
        }
    }
}

class ContextAddDialog extends EnhancedDialog
{
    public ContextAddDialog(Component comp)
    {
        super(GUIUtilities.getParentDialog(comp),
            jEdit.getProperty("options.context.add.title"),
            true);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12,12,12,12));
        setContentPane(content);

        ActionHandler actionHandler = new ActionHandler();
        ButtonGroup grp = new ButtonGroup();

        JPanel typePanel = new JPanel(new GridLayout(3,1,6,6));
        typePanel.setBorder(new EmptyBorder(0,0,6,0));
        typePanel.add(new JLabel(
            jEdit.getProperty("options.context.add.caption")));

        separator = new JRadioButton(jEdit.getProperty("options.context"
            + ".add.separator"));
        separator.addActionListener(actionHandler);
        grp.add(separator);
        typePanel.add(separator);

        action = new JRadioButton(jEdit.getProperty("options.context"
            + ".add.action"));
        action.addActionListener(actionHandler);
        grp.add(action);
        action.setSelected(true);
        typePanel.add(action);

        content.add(BorderLayout.NORTH,typePanel);

        JPanel actionPanel = new JPanel(new BorderLayout(6,6));

        JEditActionSet[] actionsList = jEdit.getActionSets();
        Vector vec = new Vector(actionsList.length);
        for(int i = 0; i < actionsList.length; i++)
        {
            ActionSet actionSet = (ActionSet) actionsList[i];
            if(actionSet.getActionCount() != 0)
                vec.addElement(actionSet);
        }
        combo = new JComboBox(vec);
        combo.addActionListener(actionHandler);
        actionPanel.add(BorderLayout.NORTH,combo);

        list = new JList();
        list.setVisibleRowCount(8);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionPanel.add(BorderLayout.CENTER,new JScrollPane(list));

        content.add(BorderLayout.CENTER,actionPanel);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel,BoxLayout.X_AXIS));
        southPanel.setBorder(new EmptyBorder(12,0,0,0));
        southPanel.add(Box.createGlue());
        ok = new JButton(jEdit.getProperty("common.ok"));
        ok.addActionListener(actionHandler);
        getRootPane().setDefaultButton(ok);
        southPanel.add(ok);
        southPanel.add(Box.createHorizontalStrut(6));
        cancel = new JButton(jEdit.getProperty("common.cancel"));
        cancel.addActionListener(actionHandler);
        southPanel.add(cancel);
        southPanel.add(Box.createGlue());

        content.add(BorderLayout.SOUTH,southPanel);

        updateList();

        pack();
        setLocationRelativeTo(GUIUtilities.getParentDialog(comp));
        setVisible(true);
    }

    public void ok()
    {
        isOK = true;
        dispose();
    }

    public void cancel()
    {
        dispose();
    }

    public String getSelection()
    {
        if(!isOK)
            return null;

        if(separator.isSelected())
            return "-";
        else if(action.isSelected())
        {
            return ((AbstractContextOptionPane.MenuItem)list.getSelectedValue())
                .actionName;
        }
        else
            throw new InternalError();
    }

    // private members
    private boolean isOK;
    private JRadioButton separator, action;
    private JComboBox combo;
    private JList list;
    private JButton ok, cancel;

    private void updateList()
    {
        ActionSet actionSet = (ActionSet)combo.getSelectedItem();
        EditAction[] actions = actionSet.getActions();
        Vector listModel = new Vector(actions.length);

        for(int i = 0; i < actions.length; i++)
        {
            EditAction action = actions[i];
            String label = action.getLabel();
            if(label == null)
                continue;

            listModel.addElement(new AbstractContextOptionPane.MenuItem(
                action.getName(),label));
        }

        Collections.sort(listModel,new AbstractContextOptionPane.MenuItemCompare());

        list.setListData(listModel);
    }

    class ActionHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            Object source = evt.getSource();
            if(source instanceof JRadioButton)
            {
                combo.setEnabled(action.isSelected());
                list.setEnabled(action.isSelected());
            }
            if(source == ok)
                ok();
            else if(source == cancel)
                cancel();
            else if(source == combo)
                updateList();
        }
    }
}

