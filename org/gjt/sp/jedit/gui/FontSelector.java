/*
 * FontSelector.java - Font selector
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 * Portions copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2003 mike dillon
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
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

//{{{ FontSelector class
/**
 * A font chooser widget.
 * @author Slava Pestov
 * @version $Id$
 */
public class FontSelector extends JButton
{
	//{{{ FontSelector constructor
	/**
	 * Creates a new font selector control.
	 * @param font The font
	 */
	public FontSelector(Font font)
	{
		this(font,false);
	} //}}}

	//{{{ FontSelector constructor
	/**
	 * Creates a new font selector control.
	 * @param font The font
	 * @param antiAlias Is anti-aliasing enabled?
	 * @since jEdit 4.2pre7
	 */
	public FontSelector(Font font, boolean antiAlias)
	{
		setFont(font);
		this.antiAlias = antiAlias;

		updateText();

		setRequestFocusEnabled(false);

		addActionListener(new ActionHandler());
	} //}}}

	//{{{ paintComponent() method
	public void paintComponent(Graphics g)
	{
		setAntiAliasEnabled(g);
		super.paintComponent(g);
	} //}}}

	//{{{ isAntiAliasEnabled() method
	public boolean isAntiAliasEnabled()
	{
		return antiAlias;
	} //}}}

	//{{{ setAntiAliasEnabled() method
	public void setAntiAliasEnabled(boolean antiAlias)
	{
		this.antiAlias = antiAlias;
	} //}}}

	//{{{ updateText() method
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

		setText(font.getName() + " " + font.getSize() + " " + styleString);
	} //}}}

	//{{{ setAntiAliasEnabled() method
	void setAntiAliasEnabled(Graphics g)
	{
		if (antiAlias)
		{
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		}
	} //}}}

	private boolean antiAlias;

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Font font;

			JDialog dialog = GUIUtilities.getParentDialog(FontSelector.this);
			if(dialog == null)
			{
				font = new FontSelectorDialog(
					JOptionPane.getFrameForComponent(
					FontSelector.this),getFont(),
					FontSelector.this)
					.getSelectedFont();
			}
			else
			{
				font = new FontSelectorDialog(dialog,getFont(),
					FontSelector.this)
					.getSelectedFont();
			}

			if(font != null)
			{
				setFont(font);
				updateText();
			}
		}
	} //}}}
} //}}}

//{{{ FontSelectorDialog class
class FontSelectorDialog extends EnhancedDialog
{
	//{{{ FontSelectorDialog constructor
	public FontSelectorDialog(Frame parent, Font font)
	{
		super(parent,jEdit.getProperty("font-selector.title"),true);
		init(font);
	} //}}}

	//{{{ FontSelectorDialog constructor
	public FontSelectorDialog(Dialog parent, Font font)
	{
		super(parent,jEdit.getProperty("font-selector.title"),true);
		init(font);
	} //}}}

	//{{{ FontSelectorDialog constructor
	public FontSelectorDialog(Frame parent, Font font,
		FontSelector fontSelector)
	{
		super(parent,jEdit.getProperty("font-selector.title"),true);
		this.fontSelector = fontSelector;
		init(font);
	} //}}}

	//{{{ FontSelectorDialog constructor
	public FontSelectorDialog(Dialog parent, Font font,
		FontSelector fontSelector)
	{
		super(parent,jEdit.getProperty("font-selector.title"),true);
		this.fontSelector = fontSelector;
		init(font);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		isOK = true;
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getSelectedFont() method
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
			size = 12;
		}

		return new Font(familyField.getText(),styleList
			.getSelectedIndex(),size);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private FontSelector fontSelector;
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
	//}}}

	/**
	 * For some reason the default Java fonts show up in the
	 * list with .bold, .bolditalic, and .italic extensions.
	 */
	private static final String[] HIDEFONTS = {
		".bold",
		".italic"
	};

	//{{{ init() method
	private void init(Font font)
	{
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel listPanel = new JPanel(new GridLayout(1,3,6,6));

		String[] fonts;
		try
		{
			fonts = getFontList();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Broken Java implementation!");
			/* Log.log(Log.ERROR,this,"Using deprecated Toolkit.getFontList()"); */
			Log.log(Log.ERROR,this,e);

			/* fonts = getToolkit().getFontList(); */
			fonts = new String[] { "Broken Java implementation!" };
		}

		JPanel familyPanel = createTextFieldAndListPanel(
			"font-selector.family",
			familyField = new JTextField(),
			familyList = new JList(fonts));
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

		preview = new JLabel(jEdit.getProperty("font-selector.long-text")) {
			public void paintComponent(Graphics g)
			{
				if(fontSelector != null)
					fontSelector.setAntiAliasEnabled(g);
				super.paintComponent(g);
			}
		};
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
		setLocationRelativeTo(getParent());
		setVisible(true);
	} //}}}

	//{{{ getFontList() method
	private String[] getFontList()
	{
		String[] nameArray = GraphicsEnvironment
			.getLocalGraphicsEnvironment()
			.getAvailableFontFamilyNames();
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
	} //}}}

	//{{{ createTextFieldAndListPanel() method
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
	} //}}}

	//{{{ updatePreview() method
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
			size = 12;
		}
		int style = styleList.getSelectedIndex();

		preview.setFont(new Font(family,style,size));
	} //}}}

	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else if(evt.getSource() == cancel)
				cancel();
		}
	} //}}}

	//{{{ ListHandler class
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
	} //}}}
} //}}}
