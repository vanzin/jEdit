/*
 * Registers.java - Register manager
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import javax.swing.text.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's registers are an extension of the clipboard metaphor.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Registers
{
	//{{{ copy() method
	/**
	 * Convenience method that copies the text selected in the specified
	 * text area into the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void copy(JEditTextArea textArea, char register)
	{
		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);
	} //}}}

	//{{{ append() method
	/**
	 * Convinience method that appends the text selected in the specified
	 * text area to the specified register, with a newline between the old
	 * and new text.
	 * @param textArea The text area
	 * @param register The register
	 */
	public static void append(JEditTextArea textArea, char register)
	{
		append(textArea,register,"\n",false);
	} //}}}

	//{{{ append() method
	/**
	 * Convinience method that appends the text selected in the specified
	 * text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The text to insert between the old and new text
	 */
	public static void append(JEditTextArea textArea, char register,
		String separator)
	{
		append(textArea,register,separator,false);
	} //}}}

	//{{{ append() method
	/**
	 * Convinience method that appends the text selected in the specified
	 * text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The text to insert between the old and new text
	 * @param cut Should the current selection be removed?
	 * @since jEdit 3.2pre1
	 */
	public static void append(JEditTextArea textArea, char register,
		String separator, boolean cut)
	{
		if(cut && !textArea.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		Register reg = getRegister(register);

		String registerContents = reg.toString();
		if(reg != null && registerContents != null)
		{
			if(registerContents.endsWith(separator))
				selection = registerContents + selection;
			else
				selection = registerContents + separator + selection;
		}

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);

		if(cut)
			textArea.setSelectedText("");
	} //}}}

	//{{{ cut() method
	/**
	 * Convinience method that copies the text selected in the specified
	 * text area into the specified register, and then removes it from the
	 * text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void cut(JEditTextArea textArea, char register)
	{
		if(textArea.isEditable())
		{
			String selection = textArea.getSelectedText();
			if(selection == null)
				return;

			setRegister(register,selection);
			HistoryModel.getModel("clipboard").addItem(selection);

			textArea.setSelectedText("");
		}
		else
			textArea.getToolkit().beep();
	} //}}}

	//{{{ paste() method
	/**
	 * Convinience method that pastes the contents of the specified
	 * register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void paste(JEditTextArea textArea, char register)
	{
		if(!textArea.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		Register reg = getRegister(register);

		if(reg == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		else
		{
			String selection = reg.toString();
			if(selection == null)
			{
				textArea.getToolkit().beep();
				return;
			}

			// preserve magic pos for easy insertion of the
			// same string at the start of multiple lines

			// XXX: getMagicCaretPosition() is deprecated
			int magic = textArea.getMagicCaretPosition();
			textArea.setSelectedText(selection);

			if(textArea.getCaretPosition()
				!= textArea.getLineEndOffset(textArea.getCaretLine()) - 1)
			{
				textArea.setMagicCaretPosition(magic);
			}
			else
			{
				// if user is pasting at end of line, chances are
				// they want the caret to go the the end of the
				// line again when they move it up or down
			}

			HistoryModel.getModel("clipboard").addItem(selection);
		}
	} //}}}

	//{{{ getRegister() method
	/**
	 * Returns the specified register.
	 * @param name The name
	 */
	public static Register getRegister(char name)
	{
		if(registers == null || name >= registers.length)
			return null;
		else
			return registers[name];
	} //}}}

	//{{{ setRegister() method
	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param newRegister The new value
	 */
	public static void setRegister(char name, Register newRegister)
	{
		if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16,name * 2)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
		}

		registers[name] = newRegister;
	} //}}}

	//{{{ setRegister() method
	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param value The new value
	 */
	public static void setRegister(char name, String value)
	{
		if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16,name * 2)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
			registers[name] = new StringRegister(value);
		}
		else
		{
			Register register = registers[name];

			if(register != null)
				register.setValue(value);
			else
				registers[name] = new StringRegister(value);
		}
	} //}}}

	//{{{ clearRegister() method
	/**
	 * Sets the value of the specified register to <code>null</code>.
	 * @param name The register name
	 */
	public static void clearRegister(char name)
	{
		if(name >= registers.length)
			return;

		Register register = registers[name];
		if(name == '$' || name == '%')
			register.setValue("");
		else
			registers[name] = null;
	} //}}}

	//{{{ getRegister() method
	/**
	 * Returns an array of all available registers. Some of the elements
	 * of this array might be null.
	 */
	public static Register[] getRegisters()
	{
		return registers;
	} //}}}

	//{{{ Register interface
	/**
	 * A register.
	 */
	public interface Register
	{
		/**
		 * Converts to a string.
		 */
		String toString();

		/**
		 * Sets the register contents.
		 */
		void setValue(String value);
	} //}}}

	//{{{ ClipboardRegister class
	/**
	 * A clipboard register. Register "$" should always be an
	 * instance of this.
	 */
	public static class ClipboardRegister implements Register
	{
		Clipboard clipboard;

		public ClipboardRegister(Clipboard clipboard)
		{
			this.clipboard = clipboard;
		}

		/**
		 * Sets the clipboard contents.
		 */
		public void setValue(String value)
		{
			StringSelection selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		/**
		 * Returns the clipboard contents.
		 */
		public String toString()
		{
			try
			{
				String selection = (String)(clipboard
					.getContents(this).getTransferData(
					DataFlavor.stringFlavor));

				boolean trailingEOL = (selection.endsWith("\n")
					|| selection.endsWith(System.getProperty(
					"line.separator")));

				// Some Java versions return the clipboard
				// contents using the native line separator,
				// so have to convert it here
				BufferedReader in = new BufferedReader(
					new StringReader(selection));
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					buf.append(line);
					buf.append('\n');
				}
				// remove trailing \n
				if(!trailingEOL)
					buf.setLength(buf.length() - 1);
				return buf.toString();
			}
			catch(Exception e)
			{
				Log.log(Log.NOTICE,this,e);
				return null;
			}
		}
	} //}}}

	//{{{ StringRegister class
	/**
	 * Register that stores a string.
	 */
	public static class StringRegister implements Register
	{
		private String value;

		/**
		 * Creates a new string register.
		 * @param value The contents
		 */
		public StringRegister(String value)
		{
			this.value = value;
		}

		/**
		 * Sets the register contents.
		 */
		public void setValue(String value)
		{
			this.value = value;
		}

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			return value;
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
		public void dispose() {}
	} //}}}

	//{{{ Private members
	private static Register[] registers;

	private Registers() {}

	static
	{
		registers = new Register[256];
		registers['$'] = new ClipboardRegister(Toolkit
			.getDefaultToolkit().getSystemClipboard());
	} //}}}
}
