/*
 * AbbrevEditor.java - Panel for editing abbreviations
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

public class AbbrevEditor extends JPanel
{
	//{{{ AbbrevEditor constructor
	public AbbrevEditor()
	{
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.anchor = cons.WEST;
		cons.fill = cons.BOTH;
		cons.weightx = 0.0f;
		cons.gridx = 1;
		cons.gridy = 1;

		JLabel label = new JLabel(jEdit.getProperty("abbrev-editor.abbrev"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		layout.setConstraints(label,cons);
		add(label);
		cons.gridx++;
		cons.weightx = 1.0f;
		abbrev = new JTextField();
		layout.setConstraints(abbrev,cons);
		add(abbrev);

		cons.gridx = 1;
		cons.weightx = 0.0f;
		cons.gridwidth = 2;

		cons.gridy++;
		label = new JLabel(jEdit.getProperty("abbrev-editor.before"));
		label.setBorder(new EmptyBorder(6,0,3,0));
		layout.setConstraints(label,cons);
		add(label);

		cons.gridy++;
		cons.weighty = 1.0f;
		beforeCaret = new JTextArea(4,40);
		JScrollPane scroller = new JScrollPane(beforeCaret);
		layout.setConstraints(scroller,cons);
		add(scroller);

		cons.gridy++;
		cons.weighty = 0.0f;
		label = new JLabel(jEdit.getProperty("abbrev-editor.after"));
		label.setBorder(new EmptyBorder(6,0,3,0));
		layout.setConstraints(label,cons);
		add(label);

		cons.gridy++;
		cons.weighty = 1.0f;
		afterCaret = new JTextArea(4,40);
		scroller = new JScrollPane(afterCaret);
		layout.setConstraints(scroller,cons);
		add(scroller);
	} //}}}

	//{{{ getAbbrev() method
	public String getAbbrev()
	{
		return abbrev.getText();
	} //}}}

	//{{{ setAbbrev() method
	public void setAbbrev(String abbrev)
	{
		this.abbrev.setText(abbrev);
	} //}}}

	//{{{ getExpansion() method
	public String getExpansion()
	{
		StringBuilder buf = new StringBuilder();

		String beforeCaretText = beforeCaret.getText();
		String afterCaretText = afterCaret.getText();

		for(int i = 0; i < beforeCaretText.length(); i++)
		{
			char ch = beforeCaretText.charAt(i);
			switch(ch)
			{
			case '\n':
				buf.append("\\n");
				break;
			case '\t':
				buf.append("\\t");
				break;
			case '\\':
				buf.append("\\\\");
				break;
			default:
				buf.append(ch);
				break;
			}
		}

		if(afterCaretText.length() != 0)
		{
			buf.append("\\|");

			for(int i = 0; i < afterCaretText.length(); i++)
			{
				char ch = afterCaretText.charAt(i);
				switch(ch)
				{
				case '\n':
					buf.append("\\n");
					break;
				case '\t':
					buf.append("\\t");
					break;
				case '\\':
					buf.append("\\\\");
					break;
				default:
					buf.append(ch);
					break;
				}
			}
		}

		return buf.toString();
	} //}}}

	//{{{ setExpansion() method
	public void setExpansion(String expansion)
	{
		if(expansion == null)
		{
			beforeCaret.setText(null);
			afterCaret.setText(null);
			return;
		}

		String beforeCaretText = null;
		String afterCaretText = null;
		StringBuilder buf = new StringBuilder();

		for(int i = 0; i < expansion.length(); i++)
		{
			char ch = expansion.charAt(i);

			if(ch == '\\' && i != expansion.length() - 1)
			{
				ch = expansion.charAt(++i);
				switch(ch)
				{
				case 't':
					buf.append('\t');
					break;
				case 'n':
					buf.append('\n');
					break;
				case '|':
					beforeCaretText = buf.toString();
					buf.setLength(0);
					break;
				default:
					buf.append(ch);
					break;
				}
			}
			else
				buf.append(ch);
		}

		if(beforeCaretText == null)
			beforeCaretText = buf.toString();
		else
			afterCaretText = buf.toString();

		beforeCaret.setText(beforeCaretText);
		afterCaret.setText(afterCaretText);
	} //}}}

	//{{{ getAbbrevField() method
	public JTextField getAbbrevField()
	{
		return abbrev;
	} //}}}

	//{{{ getBeforeCaretTextArea() method
	public JTextArea getBeforeCaretTextArea()
	{
		return beforeCaret;
	} //}}}

	//{{{ getAfterCaretTextArea() method
	public JTextArea getAfterCaretTextArea()
	{
		return afterCaret;
	} //}}}

	//{{{ Private members
	private JTextField abbrev;
	private JTextArea beforeCaret, afterCaret;
	//}}}
}
