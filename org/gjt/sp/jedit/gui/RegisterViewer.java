/*
 * RegisterViewer.java - Dockable view of register contents
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2005 Nicholas O'Leary
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

//{{{ Imports
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.Registers.Register;
import org.gjt.sp.jedit.msg.RegisterChanged;
import org.gjt.sp.jedit.msg.PropertiesChanged;
//}}}

public class RegisterViewer extends JPanel implements ActionListener,
	DockableWindow
{
	//{{{ RegisterViewer constructor
	public RegisterViewer(View view, String position)
	{
		super(new BorderLayout());
		this.view = view;
		Box toolBar = new Box(BoxLayout.X_AXIS);
		JLabel label = new JLabel(
			jEdit.getProperty("view-registers.title"));
		label.setBorder(new EmptyBorder(0,0,3,0));
		toolBar.add(label);
		
		toolBar.add(Box.createGlue());

		RolloverButton pasteRegister = new RolloverButton(
			GUIUtilities.loadIcon("Paste.png"));
		pasteRegister.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("paste-string-register.label")));
		pasteRegister.addActionListener(this);
		pasteRegister.setActionCommand("paste-string-register");
		toolBar.add(pasteRegister);

		RolloverButton clearRegister = new RolloverButton(
			GUIUtilities.loadIcon("Clear.png"));
		clearRegister.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("clear-string-register.label")));
		clearRegister.addActionListener(this);
		clearRegister.setActionCommand("clear-string-register");
		toolBar.add(clearRegister);

		
		add(BorderLayout.NORTH,toolBar);

		DefaultListModel registerModel = new DefaultListModel();
		registerList = new JList(registerModel);
		registerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		registerList.setCellRenderer(new Renderer());
		registerList.addListSelectionListener(new ListHandler());
		registerList.addMouseListener(new MouseHandler());

		contentTextArea = new JTextArea(10,20);
		contentTextArea.setEditable(true);
		documentHandler = new DocumentHandler();
		//contentTextArea.getDocument().addDocumentListener(documentHandler);
		contentTextArea.addFocusListener(new FocusHandler());

		int orientation = JSplitPane.HORIZONTAL_SPLIT;
		if (position.equals(DockableWindowManager.LEFT) ||
			position.equals(DockableWindowManager.RIGHT))
			orientation = JSplitPane.VERTICAL_SPLIT;

		add(BorderLayout.CENTER,splitPane = new JSplitPane(orientation,
			new JScrollPane(registerList),
			new JScrollPane(contentTextArea)));

		refreshList();
	} //}}}
	
	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent evt)
	{
		String cmd = evt.getActionCommand();
		if (cmd.equals("paste-string-register"))
			insertRegister();
		else if (cmd.equals("clear-string-register"))
			clearSelectedIndex();
	} //}}}

	//{{{ handleRegisterChanged() method
	@EBHandler
	public void handleRegisterChanged(RegisterChanged msg)
	{
		if (msg.getRegisterName() != '%')
			refreshList();
	} //}}}

	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ move() method
	public void move(String newPosition)
	{
		int orientation = JSplitPane.HORIZONTAL_SPLIT;
		if (newPosition.equals(DockableWindowManager.LEFT) ||
			newPosition.equals(DockableWindowManager.RIGHT))
			orientation = JSplitPane.VERTICAL_SPLIT;
		splitPane.setOrientation(orientation);
		revalidate();
	} //}}}

	//{{{ Private members

	//{{{ clearSelectedIndex() method
	private void clearSelectedIndex()
	{
		Object o = registerList.getSelectedValue();
		if (o != null && o instanceof Character)
		{
			Registers.clearRegister(((Character)o).charValue());
			refreshList();
		}
	} //}}}

	//{{{ Instance variables
	private JList registerList;
	private JTextArea contentTextArea;
	private DocumentHandler documentHandler;
	private View view;
	private boolean editing;
	private JSplitPane splitPane;
	private JPopupMenu popup;
	//}}}

	//{{{ refreshList
	private void refreshList()
	{
		DefaultListModel registerModel = (DefaultListModel)registerList.getModel();
		Object o = registerList.getSelectedValue();
		int selected = -1;
		if (o != null && o instanceof Character)
			selected = ((Character)o).charValue();

		registerModel.removeAllElements();
		Registers.Register[] registers = Registers.getRegisters();

		int index = 0;
		for(int i = 0; i < registers.length; i++)
		{
			Registers.Register reg = registers[i];
			if(reg == null)
				continue;
			if (i == '%')
				continue;

			String value = reg.toString();
			if(value == null) // || value.length() == 0)
				continue;
			if (i == selected)
				index = registerModel.size();
			registerModel.addElement(Character.valueOf((char)i));
		}

		if(registerModel.getSize() == 0)
		{
			registerModel.addElement(jEdit.getProperty("view-registers.none"));
			registerList.setEnabled(false);
		}
		else
			registerList.setEnabled(true);
		registerList.setSelectedIndex(index);
	} //}}}

	//{{{ insertRegister
	private void insertRegister()
	{
		Object o = registerList.getSelectedValue();
		if (o == null || !(o instanceof Character))
			return;
		Registers.Register reg = Registers.getRegister(((Character)o).charValue());
		view.getTextArea().setSelectedText(reg.toString());
		view.getTextArea().requestFocus();
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Renderer Class
	static class Renderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(
			JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,
			index,isSelected,cellHasFocus);

			if(value instanceof Character)
			{
				char name = ((Character)value).charValue();

				String label;

				if(name == '\n')
					label = "\n";
				else if(name == '\t')
					label = "\t";
				else if(name == '$')
					label = jEdit.getProperty("view-registers.clipboard");
				else if(name == '%')
					label = jEdit.getProperty("view-registers.selection");
				else
					label = String.valueOf(name);
				Register register = Registers.getRegister(name);
				String registerValue;
				if (register == null)
				{
					// The register is not defined anymore, it has been removed before
					// the painting event
					registerValue = jEdit.getProperty("view-registers.undefined");
				}
				else
				{
					registerValue = register.toString();
					if (registerValue.length() > 100)
						registerValue = registerValue.substring(0,100)+"...";
					registerValue = registerValue.replaceAll("\n"," ");
					registerValue = registerValue.replaceAll("\t"," ");
				}
				setText(label + " : " + registerValue);
			}

			return this;

		}
	} //}}}

	//{{{ ListHandler Class
	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{			
			Object value = registerList.getSelectedValue();
			if(!(value instanceof Character))
			{
				if (!editing)
				{
					contentTextArea.setText("");
					contentTextArea.setEditable(false);
				}
				return;
			}

			char name = ((Character)value).charValue();

			Registers.Register reg = Registers.getRegister(name);
			if(reg == null)
			{
				if (!editing)
				{
					contentTextArea.setText("");
					contentTextArea.setEditable(false);
				}	
				return;
			}
			
			
			if (!editing)
			{
				contentTextArea.setText(reg.toString());
				contentTextArea.setEditable(true);
				contentTextArea.setCaretPosition(0);
			}
		}
	} //}}}

	//{{{ MouseHandler Class
	class MouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			int i = registerList.locationToIndex(evt.getPoint());
			if (i != -1)
				registerList.setSelectedIndex(i);
			if (GUIUtilities.isPopupTrigger(evt))
			{
				if (popup == null)
				{
					popup = new JPopupMenu();
					JMenuItem item = GUIUtilities.loadMenuItem("paste");
					popup.add(item);
					item = new JMenuItem(jEdit.getProperty("clear-string-register.label"));
					item.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							clearSelectedIndex();
						}
					});
					popup.add(item);
				}
				GUIUtilities.showPopupMenu(popup, registerList, evt.getX(), evt.getY(), false);
			}
			else if (evt.getClickCount() % 2 == 0)
				insertRegister();
		}
	} //}}}

	//{{{ DocumentHandler Class
	class DocumentHandler implements DocumentListener
	{
		public void changedUpdate(DocumentEvent e)
		{
			updateRegisterSafely();
		}

		public void insertUpdate(DocumentEvent e)
		{
			updateRegisterSafely();
		}

		public void removeUpdate(DocumentEvent e)
		{
			updateRegisterSafely();
		}

		private void updateRegisterSafely()
		{
			try
			{
				editing = true;
				updateRegister();
			}
			finally
			{
				editing = false;
			}
		}
		
		private void updateRegister()
		{
			Object value = registerList.getSelectedValue();
			if(!(value instanceof Character))
				return;
			char name = ((Character)value).charValue();
			Registers.setRegister(name,contentTextArea.getText());
		}
	} //}}}

	//{{{ FocusHandler Class
	class FocusHandler implements FocusListener
	{
		public void focusGained(FocusEvent e)
		{
			contentTextArea.getDocument().addDocumentListener(documentHandler);
		}
		public void focusLost(FocusEvent e)
		{
			contentTextArea.getDocument().removeDocumentListener(documentHandler);
		}
	}//}}}

	//}}}
}
