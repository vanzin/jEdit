/*
 * Registers.java - Register manager
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Portions Copyright (C) 2010 Matthieu Casanova
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
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.*;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.datatransfer.JEditDataFlavor;
import org.gjt.sp.jedit.datatransfer.JEditRichText;
import org.gjt.sp.jedit.datatransfer.TransferHandler;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's registers are an extension of the clipboard metaphor.<p>
 *
 * A {@link Registers.Register} is string of text indexed by a
 * single character. Typically the text is taken from selected buffer text
 * and the index character is a keyboard character selected by the user.<p>
 *
 * This class defines a number of static methods
 * that give each register the properties of a virtual clipboard.<p>
 *
 * Two classes implement the {@link Registers.Register} interface. A
 * {@link Registers.ClipboardRegister} is tied to the contents of the
 * system clipboard. jEdit assigns a
 * {@link Registers.ClipboardRegister} to the register indexed under
 * the character <code>$</code>. A
 * {@link Registers.DefaultRegister} is created for registers assigned
 * by the user. In addition, jEdit assigns <code>%</code> to
 * the last text segment selected in the text area. On Windows this is a
 * {@link Registers.DefaultRegister}, on Unix under Java 2 version 1.4, a
 * {@link Registers.ClipboardRegister}.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class Registers
{
	//{{{ copy() method
	/**
	 * Copies the text selected in the text area into the specified register.
	 * This will replace the existing contents of the designated register.
	 *
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void copy(TextArea textArea, char register)
	{
		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		Transferable transferable = TransferHandler.getInstance().getTransferable(textArea, selection);
		setRegister(register, transferable);
		HistoryModel.getModel("clipboard").addItem(selection);

	} //}}}

	//{{{ cut() method
	/**
	 * Copies the text selected in the text area into the specified
	 * register, and then removes it from the buffer.
	 *
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void cut(TextArea textArea, char register)
	{
		if(textArea.isEditable())
		{
			String selection = textArea.getSelectedText();
			if(selection == null)
				return;

			Transferable transferable = TransferHandler.getInstance().getTransferable(textArea, selection);
			setRegister(register,transferable);
			HistoryModel.getModel("clipboard").addItem(selection);

			textArea.setSelectedText("");
		}
		else
			textArea.getToolkit().beep();
	} //}}}

	//{{{ append() methods
	/**
	 * Appends the text selected in the text area to the specified register,
	 * with a newline between the old and new text.
	 * @param textArea The text area
	 * @param register The register
	 */
	public static void append(TextArea textArea, char register)
	{
		append(textArea,register,"\n",false);
	}

	/**
	 * Appends the text selected in the text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The separator to insert between the old and new text
	 */
	public static void append(TextArea textArea, char register,
		String separator)
	{
		append(textArea,register,separator,false);
	}

	/**
	 * Appends the text selected in the  text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The text to insert between the old and new text
	 * @param cut Should the current selection be removed?
	 * @since jEdit 3.2pre1
	 */
	public static void append(TextArea textArea, char register,
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

		if(reg != null)
		{
			Transferable transferable = reg.getTransferable();
			if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				try
				{
					String registerContents = (String) transferable.getTransferData(DataFlavor.stringFlavor);
					if(registerContents != null)
					{
						if(registerContents.endsWith(separator))
							selection = registerContents + selection;
						else
							selection = registerContents + separator + selection;
					}
				}
				catch (UnsupportedFlavorException e)
				{
				}
				catch (IOException e)
				{
					Log.log(Log.ERROR, Registers.class, e);
				}
			}
		}
		Transferable transferable = TransferHandler.getInstance().getTransferable(textArea, selection);
		setRegister(register,transferable);
		HistoryModel.getModel("clipboard").addItem(selection);

		if(cut)
			textArea.setSelectedText("");
	} //}}}

	//{{{ paste() methods
	/**
	 * Insets the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void paste(TextArea textArea, char register)
	{
		paste(textArea,register,false);
	}

	/**
	 * Insets the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @param preferredDataFlavor the preferred dataflavor. If not available
	 * <tt>DataFlavor.stringFlavor</tt> will be used
	 * @since jEdit 4.4pre1
	 */
	public static void paste(TextArea textArea, char register, DataFlavor preferredDataFlavor)
	{
		paste(textArea,register,false, preferredDataFlavor);
	}

	/**
	 * Inserts the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @param vertical Vertical (columnar) paste
	 * @since jEdit 4.1pre1
	 */
	public static void paste(TextArea textArea, char register,
		boolean vertical)
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
		Transferable transferable = reg.getTransferable();
		Mode mode = null;
		String selection = null;
		if (transferable.isDataFlavorSupported(JEditDataFlavor.jEditRichTextDataFlavor))
		{
			try
			{
				JEditRichText data = (JEditRichText) transferable.getTransferData(JEditDataFlavor.jEditRichTextDataFlavor);
				mode = data.getMode();
				selection = data.getText();
			}
			catch (UnsupportedFlavorException e)
			{
				Log.log(Log.ERROR, Registers.class, e);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, Registers.class, e);
			}
		}
		else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			selection = getTextFromTransferable(transferable, DataFlavor.stringFlavor);
		}
		if(selection == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		JEditBuffer buffer = textArea.getBuffer();
		applyMode(mode, buffer);
		_paste(textArea, vertical, selection, buffer);
	}

	/**
	 * Inserts the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @param vertical Vertical (columnar) paste
	 * @param preferredDataFlavor the preferred dataflavor. If not available
	 * <tt>DataFlavor.stringFlavor</tt> will be used
	 * @since jEdit 4.4pre1
	 */
	public static void paste(TextArea textArea, char register,
		boolean vertical, DataFlavor preferredDataFlavor)
	{
		if (JEditDataFlavor.jEditRichTextDataFlavor.equals(preferredDataFlavor))
		{
			paste(textArea,register,vertical);
			return;
		}
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
		Transferable transferable = reg.getTransferable();
		String selection = null;
		if (transferable.isDataFlavorSupported(preferredDataFlavor))
		{
			selection = getTextFromTransferable(transferable, preferredDataFlavor);
		}
		else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			selection = getTextFromTransferable(transferable, DataFlavor.stringFlavor);
		}
		if(selection == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		JEditBuffer buffer = textArea.getBuffer();
		 /*
		 Commented because it must not use jEdit class.
		 Need to rewrite a property manager that is independant
		String mime = preferredDataFlavor.getMimeType();
		int i = mime.indexOf(';');
		if (i != -1)
		{
			mime = mime.substring(0,i);
		}
		String mode = jEdit.getProperty("mime2mode."+mime);
		if (mode != null)
		{
			Mode _mode = ModeProvider.instance.getMode(mode);
			if (_mode != null)
			{
				applyMode(_mode, buffer);
			}
		}     */
		_paste(textArea, vertical, selection, buffer);
	}

	private static void _paste(TextArea textArea, boolean vertical, String selection, JEditBuffer buffer)
	{
		try
		{
			buffer.beginCompoundEdit();

			/* vertical paste */
			if(vertical && textArea.getSelectionCount() == 0)
			{
				int caret = textArea.getCaretPosition();
				int caretLine = textArea.getCaretLine();
				Selection.Rect rect = new Selection.Rect(
					caretLine,caret,caretLine,caret);
				textArea.setSelectedText(rect,selection);
				caretLine = textArea.getCaretLine();

				if(caretLine != textArea.getLineCount() - 1)
				{

					int startColumn = rect.getStartColumn(
						buffer);
					int offset = buffer
						.getOffsetOfVirtualColumn(
						caretLine + 1,startColumn,null);
					if(offset == -1)
					{
						buffer.insertAtColumn(caretLine + 1,startColumn,"");
						textArea.setCaretPosition(
							buffer.getLineEndOffset(
							caretLine + 1) - 1);
					}
					else
					{
						textArea.setCaretPosition(
							buffer.getLineStartOffset(
							caretLine + 1) + offset);
					}
				}
			}
			else /* Regular paste */
			{
				textArea.replaceSelection(selection);
			}
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		HistoryModel.getModel("clipboard").addItem(selection);
	} //}}}

	//{{{ applyMode() method
	private static void applyMode(Mode mode, JEditBuffer buffer)
	{
		if (mode != null &&
			"text".equals(buffer.getMode().getName()) &&
		!mode.equals(buffer.getMode()) &&
		buffer.getLength() == 0)
		{
			buffer.setMode(mode);
		}
	} //}}}

	//{{{ getTextFromTransferable() method
	private static String getTextFromTransferable(Transferable transferable, DataFlavor dataFlavor)
	{
		try
		{
			Object data = transferable.getTransferData(dataFlavor);
			return stripEOLChars(data.toString());
		}
		catch (UnsupportedFlavorException e)
		{
			Log.log(Log.ERROR, Registers.class, e);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, Registers.class, e);
		}
		return null;
	} //}}}

	//{{{ getRegister() method
	/**
	 * Returns the specified register.
	 * @param name The name
	 */
	public static Register getRegister(char name)
	{
		if(name != '$' && name != '%')
		{
			if(!loaded)
				loadRegisters();
		}

		if(registers == null || name >= registers.length)
			return null;
		else
			return registers[name];
	} //}}}

	//{{{ setRegister() methods
	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param newRegister The new value
	 */
	public static void setRegister(char name, Register newRegister)
	{
		touchRegister(name);

		if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16, name<<1)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
		}

		registers[name] = newRegister;
		if (listener != null)
			listener.registerChanged(name);
	}

	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param value The new value
	 */
	public static void setRegister(char name, String value)
	{
		setRegister(name, new StringSelection(value));
	}

	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param transferable the transferable
	 */
	public static void setRegister(char name, Transferable transferable)
	{
		touchRegister(name);
		Register register = getRegister(name);
		if(register != null)
		{
			register.setTransferable(transferable);
			if (listener != null)
				listener.registerChanged(name);
		}
		else
		{
			Register defaultRegister = new DefaultRegister();
			defaultRegister.setTransferable(transferable);
			setRegister(name, defaultRegister);
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
			register.setTransferable(new StringSelection(""));
		else
		{
			registers[name] = null;
			modified = true;
			if (listener != null)
				listener.registerChanged(name);
		}
	} //}}}

	//{{{ getRegisters() method
	/**
	 * Returns an array of all available registers. Some of the elements
	 * of this array might be <code>null</code>.
	 */
	public static Register[] getRegisters()
	{
		if(!loaded)
			loadRegisters();
		return registers;
	} //}}}

	//{{{ getRegisterNameString() method
	/**
	 * Returns a string of all defined registers, used by the status bar
	 * (eg, "a b $ % ^").
	 * @since jEdit 4.2pre2
	 */
	public static String getRegisterNameString()
	{
		if(!loaded)
			loadRegisters();

		StringBuilder buf = new StringBuilder(registers.length << 1);
		for(int i = 0; i < registers.length; i++)
		{
			if(registers[i] != null)
			{
				if(buf.length() != 0)
					buf.append(' ');
				buf.append((char)i);
			}
		}

		if(buf.length() == 0)
			return null;
		else
			return buf.toString();
	} //}}}

	//{{{ saveRegisters() method
	public static void saveRegisters()
	{
		if(!loaded || !modified)
			return;

		if (saver != null)
		{
			saver.saveRegisters();
			modified = false;
		}
	} //}}}

	//{{{ setListener() method
	public static void setListener(RegistersListener listener)
	{
		Registers.listener = listener;
	} //}}}

	//{{{ setSaver() method
	public static void setSaver(RegisterSaver saver)
	{
		Registers.saver = saver;
	} //}}}

	//{{{ isLoading() method
	public static boolean isLoading()
	{
		return loading;
	} //}}}

	//{{{ setLoading() method
	public static void setLoading(boolean loading)
	{
		Registers.loading = loading;
	} //}}}

	//{{{ Private members
	private static Register[] registers;
	private static boolean loaded, loading;
	private static RegisterSaver saver;
	private static RegistersListener listener;
	/**
	 * Flag that tell if a register has been modified (except for '%' and '$' registers that aren't
	 * saved to the xml file).
	 */
	private static boolean modified;

	private Registers() {}

	static
	{
		registers = new Register[256];
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		registers['$'] = new ClipboardRegister(
			toolkit.getSystemClipboard());
		Clipboard selection = toolkit.getSystemSelection();
		if(selection != null)
			registers['%'] = new ClipboardRegister(selection);
	}

	//{{{ touchRegister() method
	private static void touchRegister(char name)
	{
		if(name == '%' || name == '$')
			return;

		if(!loaded)
			loadRegisters();

		if(!loading)
			modified = true;
	} //}}}

	//{{{ loadRegisters() method
	private static void loadRegisters()
	{
		if (saver != null)
		{
			loaded = true;
			saver.loadRegisters();
		}
	} //}}}

	//{{{ loadRegisters() method
	private static String stripEOLChars(String selection) throws IOException
	{
		boolean trailingEOL = selection.endsWith("\n")
				      || selection.endsWith(System.getProperty(
			"line.separator"));

		// Some Java versions return the clipboard
		// contents using the native line separator,
		// so have to convert it here
		BufferedReader in = new BufferedReader(
			new StringReader(selection));
		StringBuilder buf = new StringBuilder(selection.length());
		String line;
		while((line = in.readLine()) != null)
		{
			// broken Eclipse workaround!
			// 24 Febuary 2004
			if(line.endsWith("\0"))
			{
				line = line.substring(0,
						      line.length() - 1);
			}
			buf.append(line);
			buf.append('\n');
		}
		// remove trailing \n
		if(!trailingEOL && buf.length() != 0)
			buf.setLength(buf.length() - 1);
		return buf.toString();
	}  //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Register interface
	/**
	 * A register.
	 */
	public interface Register
	{
		/**
		 * Converts to a string.
		 */
		@Deprecated
		String toString();

		/**
		 * Sets the register contents.
		 * @deprecated use {@link #setTransferable(java.awt.datatransfer.Transferable)}
		 * instead, for example
		 * <code>setTransferable(new StringSelection(""))</code>
		 */
		@Deprecated
		void setValue(String value);

		Transferable getTransferable();

		void setTransferable(Transferable transferable);
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
		@Override
		public void setValue(String value)
		{
			Transferable selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		/**
		 * Returns the clipboard contents.
		 */
		@Override
		public String toString()
		{
			try
			{

				if (false)
				{
					/*
						This is to debug clipboard problems.

						Apparently, jEdit is unable to copy text from clipbard into the current
						text buffer if the clipboard was filles using the command
							echo test | xselection CLIPBOARD -
						under Linux. However, it seems that Java does not offer any
						data flavor for this clipboard content (under J2RE 1.5.0_06-b05)
						Thus, copying from clipboard seems to be plainly impossible.
					*/
					Log.log(Log.DEBUG,this,"clipboard.getContents(this)="+clipboard.getContents(this)+'.');
					debugListDataFlavors(clipboard.getContents(this));
				}

				String selection = (String)clipboard
					.getContents(this).getTransferData(
					DataFlavor.stringFlavor);

				return stripEOLChars(selection);
			}
			catch(Exception e)
			{
				Log.log(Log.NOTICE,this,e);
				return null;
			}
		}

		@Override
		public Transferable getTransferable()
		{
			return clipboard.getContents(this);
		}

		@Override
		public void setTransferable(Transferable transferable)
		{
			clipboard.setContents(transferable, null);
		}
	} //}}}

	//{{{ debugListDataFlavors() method
	protected static void debugListDataFlavors(Transferable transferable)
	{
		DataFlavor[] dataFlavors = transferable.getTransferDataFlavors();

		for (int i = 0;i<dataFlavors.length;i++)
		{
			DataFlavor dataFlavor = dataFlavors[i];
			Log.log(Log.DEBUG,Registers.class,
				"debugListDataFlavors(): dataFlavor="+
				dataFlavor+'.');
		}

		if (dataFlavors.length == 0)
		{
			Log.log(Log.DEBUG,Registers.class,
				"debugListDataFlavors(): no dataFlavor supported.");
		}
	} //}}}

	//{{{ DefaultRegister class
	private static class DefaultRegister implements Register
	{
		private Transferable transferable;

		@Override
		public void setValue(String value)
		{
			transferable = new StringSelection(value);
		}

		@Override
		public String toString()
		{
			if (transferable == null)
				return null;
			if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				try
				{
					return transferable.getTransferData(DataFlavor.stringFlavor).toString();
				}
				catch (UnsupportedFlavorException e)
				{
					Log.log(Log.ERROR, this, e);
				}
				catch (IOException e)
				{
					Log.log(Log.ERROR, this, e);
				}
			}
			return transferable.toString();
		}

		@Override
		public Transferable getTransferable()
		{
			return transferable;
		}

		@Override
		public void setTransferable(Transferable transferable)
		{
			this.transferable = transferable;
		}
	} //}}}

	//}}}
}
