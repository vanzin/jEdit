/*
 * SearchBar.java - Search & replace toolbar
 * Portions copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.search;
import java.awt.event.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.HistoryTextField;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

public class SearchBar extends JPanel
{
	public SearchBar(View view)
	{
		super(new BorderLayout());

		this.view = view;

		//Font boldFont = new Font("Dialog",Font.BOLD,12);
		//Font plainFont = new Font("Dialog",Font.PLAIN,12);

		JLabel label = new JLabel(jEdit.getProperty("view.search.find"));
		//label.setFont(boldFont);
		label.setBorder(new EmptyBorder(0,2,0,12));
		add(label,BorderLayout.WEST);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(find = new HistoryTextField("find"));
		//find.setFont(plainFont);
		Dimension min = find.getPreferredSize();
		min.width = Integer.MAX_VALUE;
		find.setMaximumSize(min);
		ActionHandler actionHandler = new ActionHandler();
		find.addKeyListener(new KeyHandler());
		find.addActionListener(actionHandler);
		find.getDocument().addDocumentListener(new DocumentHandler());
		box.add(Box.createGlue());
		add(box,BorderLayout.CENTER);

		Insets margin = new Insets(1,1,1,1);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createHorizontalStrut(12));
		buttons.add(ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.case")));
		//ignoreCase.setFont(boldFont);
		ignoreCase.addActionListener(actionHandler);
		ignoreCase.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp")));
		//regexp.setFont(boldFont);
		regexp.addActionListener(actionHandler);
		regexp.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(hyperSearch = new JCheckBox(jEdit.getProperty(
			"search.hypersearch")));
		//hyperSearch.setFont(boldFont);
		hyperSearch.addActionListener(actionHandler);
		hyperSearch.setMargin(margin);

		update();

		add(buttons,BorderLayout.EAST);
	}

	public HistoryTextField getField()
	{
		return find;
	}

	public void setHyperSearch(boolean hyperSearch)
	{
		jEdit.setBooleanProperty("view.search.hypersearch.toggle",hyperSearch);
		this.hyperSearch.setSelected(hyperSearch);
		find.setModel(this.hyperSearch.isSelected() ? "find" : null);
	}

	public void update()
	{
		ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
		regexp.setSelected(SearchAndReplace.getRegexp());
		hyperSearch.setSelected(jEdit.getBooleanProperty(
			"view.search.hypersearch.toggle"));
		find.setModel(hyperSearch.isSelected() ? "find" : null);
	}

	// private members
	private View view;
	private HistoryTextField find;
	private JCheckBox ignoreCase, regexp, hyperSearch;

	private void find(boolean reverse)
	{
		String text = find.getText();
		if(text.length() == 0)
		{
			jEdit.setBooleanProperty("search.hypersearch.toggle",
				hyperSearch.isSelected());
			new SearchDialog(view,null);
		}
		else if(hyperSearch.isSelected())
		{
			find.setText(null);
			SearchAndReplace.setSearchString(text);
			SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
			SearchAndReplace.hyperSearch(view);
		}
		else
		{
			// on enter, start search from end
			// of current match to find next one
			int start;
			JEditTextArea textArea = view.getTextArea();
			Selection s = textArea.getSelectionAtOffset(
				textArea.getCaretPosition());
			if(s == null)
				start = textArea.getCaretPosition();
			else if(reverse)
				start = s.getStart();
			else 
				start = s.getEnd();

			if(!incrementalSearch(start,reverse))
			{
				// not found. start from
				// beginning
				if(!incrementalSearch(reverse
					? view.getBuffer().getLength()
					: 0,reverse))
				{
					// not found at all. beep.
					getToolkit().beep();
				}
			}
		}
	}
	private boolean incrementalSearch(int start, boolean reverse)
	{
		/* For example, if the current fileset is a directory,
		 * C+g will find the next match within that fileset.
		 * This can be annoying if you have just done an
		 * incremental search and want the next occurrence
		 * in the current buffer. */
		SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
		SearchAndReplace.setSearchString(find.getText());
		SearchAndReplace.setReverseSearch(reverse);

		try
		{
			if(SearchAndReplace.find(view,view.getBuffer(),start))
				return true;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(Exception e)
		{
			Log.log(Log.DEBUG,this,e);

			// invalid regexp, ignore
			// return true to avoid annoying beeping while
			// typing a re
			return true;
		}

		return false;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(evt.getSource() == find)
				find(false);
			else if(evt.getSource() == hyperSearch)
			{
				jEdit.setBooleanProperty("view.search.hypersearch.toggle",
					hyperSearch.isSelected());
				update();
			}
			else if(evt.getSource() == ignoreCase)
			{
				SearchAndReplace.setIgnoreCase(ignoreCase
					.isSelected());
			}
			else if(evt.getSource() == regexp)
			{
				SearchAndReplace.setRegexp(regexp
					.isSelected());
			}
		}
	}

	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			// on insert, start search from beginning of
			// current match. This will continue to highlight
			// the current match until another match is found
			if(!hyperSearch.isSelected())
			{
				int start;
				JEditTextArea textArea = view.getTextArea();
				Selection s = textArea.getSelectionAtOffset(
					textArea.getCaretPosition());
				if(s == null)
					start = textArea.getCaretPosition();
				else 
					start = s.getStart();

				if(!incrementalSearch(start,false))
				{
					if(!incrementalSearch(0,false))
					{
						// not found at all. beep.
						getToolkit().beep();
					}
				}
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			// on backspace, restart from beginning
			if(!hyperSearch.isSelected())
			{
				String text = find.getText();
				if(text.length() != 0)
				{
					// don't beep if not found.
					// subsequent beeps are very
					// annoying when backspacing an
					// invalid search string.
					if(regexp.isSelected())
					{
						// reverse regexp search
						// not supported yet, so
						// 'sumulate' with restart
						incrementalSearch(0,false);
					}
					else
					{
						int start;
						JEditTextArea textArea = view.getTextArea();
						Selection s = textArea.getSelectionAtOffset(
							textArea.getCaretPosition());
						if(s == null)
							start = textArea.getCaretPosition();
						else 
							start = s.getStart();
						incrementalSearch(start,true);
					}
				}
			}
		}

		public void changedUpdate(DocumentEvent evt)
		{
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				if(!hyperSearch.isSelected())
				{
					evt.consume();
					view.getEditPane().focusOnTextArea();
					view.getEditPane().getTextArea()
						.processKeyEvent(evt);
				}
				break;
			case KeyEvent.VK_ESCAPE:
				evt.consume();
				view.getEditPane().focusOnTextArea();
				break;
			case KeyEvent.VK_ENTER:
				if(evt.isShiftDown())
				{
					evt.consume();
					find(true);
				}
				break;
			}
		}
	}
}
