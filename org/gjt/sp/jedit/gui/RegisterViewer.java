/*
 * RegisterViewer.java - Dockable view of register contents
 * :tabSize=4:indentSize=4:noTabs=false:
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
import org.gjt.sp.util.GenericGUIUtilities;
//}}}

/** Dockable view of register contents */
public class RegisterViewer extends JPanel
	implements DockableWindow, DefaultFocusComponent
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
		pasteRegister.setToolTipText(GenericGUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("paste-string-register.label")));
		pasteRegister.addActionListener(new InsertHandler());
		pasteRegister.setActionCommand("paste-string-register");
		toolBar.add(pasteRegister);

		RolloverButton clearRegister = new RolloverButton(
			GUIUtilities.loadIcon("Clear.png"));
		clearRegister.setToolTipText(GenericGUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("clear-string-register.label")));
		clearRegister.addActionListener(new ClearHandler());
		clearRegister.setActionCommand("clear-string-register");
		toolBar.add(clearRegister);

		add(BorderLayout.NORTH,toolBar);

		DefaultListModel<String> registerModel = new DefaultListModel<String>();
		registerList = new JList<String>(registerModel);
		registerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		registerList.setCellRenderer(new Renderer());
		registerList.addListSelectionListener(new ListHandler());
		registerList.addMouseListener(new MouseHandler());
		contentTextArea = new JTextArea(10,20);
		contentTextArea.setEditable(true);
		documentHandler = new DocumentHandler();
		//contentTextArea.getDocument().addDocumentListener(documentHandler);
		contentTextArea.addFocusListener(new FocusHandler());
		//key bindings
		this.registerKeyboardAction(new EscapeHandler(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		registerList.registerKeyboardAction(new InsertHandler(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
			WHEN_FOCUSED);
		registerList.registerKeyboardAction(new InsertHandler(),
			KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
			WHEN_FOCUSED);
		registerList.registerKeyboardAction(new ClearHandler(),
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
			WHEN_FOCUSED);
		contentTextArea.registerKeyboardAction(new TabHandler(),
			KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK),
			WHEN_FOCUSED);
		int orientation = JSplitPane.HORIZONTAL_SPLIT;
		if (position.equals(DockableWindowManager.LEFT) ||
			position.equals(DockableWindowManager.RIGHT))
			orientation = JSplitPane.VERTICAL_SPLIT;

		add(BorderLayout.CENTER,splitPane = new JSplitPane(orientation,
			new JScrollPane(registerList),
			new JScrollPane(contentTextArea)));

		refreshList();
	} //}}}

	//{{{ focusOnDefaultComponent() method
	@Override
	public void focusOnDefaultComponent()
	{
		registerList.requestFocusInWindow();
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

	//{{{ Instance variables
	/** contains either a
	 *  - String object (no Register is registered yet,
	 *                   "view-registers.none") or
	 *  - Character objects ("name" of the register; char value must be between 0 and 255,
	 *                       see Registers.java)
	 * changed so this can use generics, the registerList now can contain only Strings,
	 * either "view-registers.none" or a single character string, where the single
	 * char must have a value between 0 and 255. (More realisticly, it should be a
	 * character than can actually be typed from a keyboard.)
	 */
	private JList<String> registerList;
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
		DefaultListModel<String> registerModel = (DefaultListModel<String>)registerList.getModel();
		String o = registerList.getSelectedValue();
		int selected = -1;
		if (o != null && o.length() == 1)
			selected = o.charAt(0);

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
			registerModel.addElement(String.valueOf((char)i));
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
		String o = registerList.getSelectedValue();
		if (o == null || o.length() > 1)
			return;
		Registers.Register reg = Registers.getRegister(o.charAt(0));
		view.getTextArea().setSelectedText(reg.toString());
		// can't use requestFocusInWindow() here, otherwise we'll stay
		// in RegisterViewer when it is a floating window
		view.getTextArea().requestFocus();
		
		// close the window if we are floating
		DockableWindowManager dm = view.getDockableWindowManager();
		if (!dm.isDockableWindowDocked("view-registers")) {
			dm.hideDockableWindow("view-registers");
		}
	} //}}}

	//{{{ clearSelectedIndex() method
	private void clearSelectedIndex()
	{
		String o = registerList.getSelectedValue();
		if (o != null && o.length() == 1)
		{
			Registers.clearRegister(o.charAt(0));
			refreshList();
		}
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

			if(!jEdit.getProperty("view-registers.none").equals(value))
			{
				char name = value.toString().charAt(0);

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
			String value = registerList.getSelectedValue();
			if (value == null || value.length() < 1)
			{
				if (!editing)
				{
					contentTextArea.setText("");
					contentTextArea.setEditable(false);
				}
				return;
			}

			char name = value.charAt(0);

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
			if (GenericGUIUtilities.isPopupTrigger(evt))
			{
				if (popup == null)
				{
					popup = new JPopupMenu();
					JMenuItem item = GUIUtilities.loadMenuItem("paste");
					popup.add(item);
					item = new JMenuItem(jEdit.getProperty("clear-string-register.label"));
					item.addActionListener(new ClearHandler());
					popup.add(item);
				}
				GenericGUIUtilities.showPopupMenu(popup, registerList, evt.getX(), evt.getY(), false);
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
			String value = registerList.getSelectedValue();
			if(value == null || value.length() < 1)
				return;
			char name = value.charAt(0);
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

	//{{{ EscapeHandler Class
	class EscapeHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			view.getTextArea().requestFocus();
			view.toFront();
		}
	}//}}}

	//{{{ TabHandler Class
	class TabHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			registerList.requestFocusInWindow();
		}
	}//}}}

	//{{{ InsertHandler Class
	class InsertHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			insertRegister();
		}
	}//}}}

	//{{{ ClearHandler Class
	class ClearHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			clearSelectedIndex();

		}
	}//}}}
}
