/*
 * FontSelector.java - Font selector
 * Copyright (C) 2000, 2001 Slava Pestov
 * Portions copyright (C) 1999 Jason Ginchereau
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

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.jEdit;

/**
 * A font chooser widget.
 * @author Slava Pestov
 * @version $Id$
 */
public class FontSelector extends JButton
{
	public FontSelector(Font font)
	{
		setFont(font);

		updateText();

		setRequestFocusEnabled(false);

		addActionListener(new ActionHandler());
	}

	// private members
	private void updateText()
	{
		Font font = getFont();
		String styleString;
		switch(font.getStyle())
		{
		case Font.PLAIN:
			styleString = jEdit.getProperty("font-selector.plain");
			break;
		case Font.BOLD:
			styleString = jEdit.getProperty("font-selector.bold");
			break;
		case Font.ITALIC:
			styleString = jEdit.getProperty("font-selector.italic");
			break;
		case Font.BOLD | Font.ITALIC:
			styleString = jEdit.getProperty("font-selector.bolditalic");
			break;
		default:
			styleString = "UNKNOWN!!!???";
			break;
		}

		setText(font.getFamily() + " " + font.getSize() + " " + styleString);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Font font = new FontSelectorDialog(FontSelector.this,getFont())
				.getSelectedFont();
			if(font != null)
			{
				setFont(font);
				updateText();
			}
		}
	}
}

class FontSelectorDialog extends EnhancedDialog
{
	public FontSelectorDialog(Component comp, Font font)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("font-selector.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel listPanel = new JPanel(new GridLayout(1,3,6,6));

		JPanel familyPanel = createTextFieldAndListPanel(
			"font-selector.family",
			familyField = new JTextField(),
			familyList = new JList(getFontList()));
		listPanel.add(familyPanel);

		String[] sizes = { "9", "10", "12", "14", "16", "18", "24" };
		JPanel sizePanel = createTextFieldAndListPanel(
			"font-selector.size",
			sizeField = new JTextField(),
			sizeList = new JList(sizes));
		listPanel.add(sizePanel);

		String[] styles = {
			jEdit.getProperty("font-selector.plain"),
			jEdit.getProperty("font-selector.bold"),
			jEdit.getProperty("font-selector.italic"),
			jEdit.getProperty("font-selector.bolditalic")
		};

		JPanel stylePanel = createTextFieldAndListPanel(
			"font-selector.style",
			styleField = new JTextField(),
			styleList = new JList(styles));
		styleField.setEditable(false);
		listPanel.add(stylePanel);

		familyList.setSelectedValue(font.getFamily(),true);
		familyField.setText(font.getFamily());
		sizeList.setSelectedValue(String.valueOf(font.getSize()),true);
		sizeField.setText(String.valueOf(font.getSize()));
		styleList.setSelectedIndex(font.getStyle());
		styleField.setText((String)styleList.getSelectedValue());

		ListHandler listHandler = new ListHandler();
		familyList.addListSelectionListener(listHandler);
		sizeList.addListSelectionListener(listHandler);
		styleList.addListSelectionListener(listHandler);

		content.add(BorderLayout.NORTH,listPanel);

		preview = new JLabel(jEdit.getProperty("font-selector.long-text"));
		preview.setBorder(new TitledBorder(jEdit.getProperty(
			"font-selector.preview")));

		updatePreview();

		Dimension prefSize = preview.getPreferredSize();
		prefSize.height = 50;
		preview.setPreferredSize(prefSize);

		content.add(BorderLayout.CENTER,preview);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		buttons.add(ok);

		buttons.add(Box.createHorizontalStrut(6));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);

		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
		show();
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

	public Font getSelectedFont()
	{
		if(!isOK)
			return null;

		int size;
		try
		{
			size = Integer.parseInt(sizeField.getText());
		}
		catch(Exception e)
		{
			size = 14;
		}

		return new Font(familyField.getText(),styleList
			.getSelectedIndex(),size);
	}

	// private members
	private boolean isOK;
	private JTextField familyField;
	private JList familyList;
	private JTextField sizeField;
	private JList sizeList;
	private JTextField styleField;
	private JList styleList;
	private JLabel preview;
	private JButton ok;
	private JButton cancel;

	/**
	 * For some reason the default Java fonts show up in the
	 * list with .bold, .bolditalic, and .italic extensions.
	 */
	private static final String[] HIDEFONTS = {
		".bold",
		".italic"
	};

	private String[] getFontList()
	{
		try
		{
			Class GEClass = Class.forName("java.awt.GraphicsEnvironment");
			Object GEInstance = GEClass.getMethod("getLocalGraphicsEnvironment", null).invoke(null, null);

			String[] nameArray = (String[])
			GEClass.getMethod("getAvailableFontFamilyNames", null).invoke(GEInstance, null);
			Vector nameVector = new Vector(nameArray.length);

			for(int i = 0, j; i < nameArray.length; i++)
			{
				for(j = 0; j < HIDEFONTS.length; j++)
				{
					if(nameArray[i].indexOf(HIDEFONTS[j]) >= 0)
						break;
				}

				if(j == HIDEFONTS.length)
					nameVector.addElement(nameArray[i]);
			}

			String[] _array = new String[nameVector.size()];
			nameVector.copyInto(_array);
			return _array;
		}
		catch(Exception ex)
		{
			return Toolkit.getDefaultToolkit().getFontList();
		}
	}

	private JPanel createTextFieldAndListPanel(String label,
		JTextField textField, JList list)
	{
		GridBagLayout layout = new GridBagLayout();
		JPanel panel = new JPanel(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;

		JLabel _label = new JLabel(jEdit.getProperty(label));
		layout.setConstraints(_label,cons);
		panel.add(_label);

		cons.gridy = 1;
		Component vs = Box.createVerticalStrut(6);
		layout.setConstraints(vs,cons);
		panel.add(vs);

		cons.gridy = 2;
		layout.setConstraints(textField,cons);
		panel.add(textField);

		cons.gridy = 3;
		vs = Box.createVerticalStrut(6);
		layout.setConstraints(vs,cons);
		panel.add(vs);

		cons.gridy = 4;
		cons.gridheight = GridBagConstraints.REMAINDER;
		cons.weighty = 1.0f;
		JScrollPane scroller = new JScrollPane(list);
		layout.setConstraints(scroller,cons);
		panel.add(scroller);

		return panel;
	}

	private void updatePreview()
	{
		String family = familyField.getText();
		int size;
		try
		{
			size = Integer.parseInt(sizeField.getText());
		}
		catch(Exception e)
		{
			size = 14;
		}
		int style = styleList.getSelectedIndex();

		preview.setFont(new Font(family,style,size));
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else if(evt.getSource() == cancel)
				cancel();
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			Object source = evt.getSource();
			if(source == familyList)
			{
				String family = (String)familyList.getSelectedValue();
				if(family != null)
					familyField.setText(family);
			}
			else if(source == sizeList)
			{
				String size = (String)sizeList.getSelectedValue();
				if(size != null)
					sizeField.setText(size);
			}
			else if(source == styleList)
			{
				String style = (String)styleList.getSelectedValue();
				if(style != null)
					styleField.setText(style);
			}

			updatePreview();
		}
	}
}
