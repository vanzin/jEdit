/*
 * Macros.java - Macro manager
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;

/**
 * This class records and runs macros.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Macros
{
	/**
	 * Utility method that can be used to display a message dialog in a macro.
	 * @param view The view
	 * @param message The message
	 * @since jEdit 2.7pre2
	 */
	public static void message(View view, String message)
	{
		JOptionPane.showMessageDialog(view,message,
			jEdit.getProperty("macro-message.title"),
			JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Utility method that can be used to display an error dialog in a macro.
	 * @param view The view
	 * @param message The message
	 * @since jEdit 2.7pre2
	 */
	public static void error(View view, String message)
	{
		JOptionPane.showMessageDialog(view,message,
			jEdit.getProperty("macro-message.title"),
			JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Utility method that can be used to prompt for input in a macro.
	 * @param view The view
	 * @param prompt The prompt string
	 * @since jEdit 2.7pre2
	 */
	public static String input(View view, String prompt)
	{
		return input(view,prompt,null);
	}

	/**
	 * Utility method that can be used to prompt for input in a macro.
	 * @param view The view
	 * @param prompt The prompt string
	 * @since jEdit 3.1final
	 */
	public static String input(View view, String prompt, String defaultValue)
	{
		return (String)JOptionPane.showInputDialog(view,prompt,
			jEdit.getProperty("macro-input.title"),
			JOptionPane.QUESTION_MESSAGE,null,null,defaultValue);
	}

	/**
	 * Opens the system macro directory in a VFS browser.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void browseSystemMacros(View view)
	{
		if(userMacroPath == null)
		{
			GUIUtilities.error(view,"no-webstart",null);
			return;
		}

		DockableWindowManager dockableWindowManager
			= view.getDockableWindowManager();

		dockableWindowManager.addDockableWindow(VFSBrowser.NAME);
		final VFSBrowser browser = (VFSBrowser)dockableWindowManager
			.getDockableWindow(VFSBrowser.NAME);

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				browser.setDirectory(systemMacroPath);
			}
		});
	}

	/**
	 * Opens the user macro directory in a VFS browser.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void browseUserMacros(View view)
	{
		if(userMacroPath == null)
		{
			GUIUtilities.error(view,"no-settings",null);
			return;
		}

		DockableWindowManager dockableWindowManager
			= view.getDockableWindowManager();

		dockableWindowManager.addDockableWindow(VFSBrowser.NAME);
		final VFSBrowser browser = (VFSBrowser)dockableWindowManager
			.getDockableWindow(VFSBrowser.NAME);

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				browser.setDirectory(userMacroPath);
			}
		});
	}

	/**
	 * Rebuilds the macros list, and sends a MacrosChanged message
	 * (views update their Macros menu upon receiving it)
	 * @since jEdit 2.2pre4
	 */
	public static void loadMacros()
	{
		macroList = new Vector();
		macroHierarchy = new Vector();
		macroHash = new Hashtable();

		if(jEdit.getJEditHome() != null)
		{
			systemMacroPath = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"macros");
			loadMacros(macroHierarchy,"",new File(systemMacroPath));
		}

		String settings = jEdit.getSettingsDirectory();

		if(settings != null)
		{
			userMacroPath = MiscUtilities.constructPath(
				settings,"macros");
			loadMacros(macroHierarchy,"",new File(userMacroPath));
		}

		// sort macro list
		MiscUtilities.quicksort(macroList,new MiscUtilities.StringICaseCompare());

		EditBus.send(new MacrosChanged(null));
	}

	/**
	 * Returns a vector hierarchy with all known macros in it.
	 * Each element of this vector is either a macro name string,
	 * or another vector. If it is a vector, the first element is a
	 * string label, the rest are again, either macro name strings
	 * or vectors.
	 * @since jEdit 2.6pre1
	 */
	public static Vector getMacroHierarchy()
	{
		return macroHierarchy;
	}

	/**
	 * Returns a single vector with all known macros in it.
	 * @since jEdit 3.1pre3
	 */
	public static Vector getMacroList()
	{
		return macroList;
	}

	/**
	 * Returns the macro with the specified name.
	 * @param macro The macro's name
	 * @since jEdit 2.6pre1
	 */
	public static Macro getMacro(String macro)
	{
		return (Macro)macroHash.get(macro);
	}

	/**
	 * Encapsulates the macro's label, name and path.
	 * @since jEdit 2.2pre4
	 */
	public static class Macro
	{
		public String name;
		public String path;
		public EditAction action;

		public Macro(String name, final String path)
		{
			this.name = name;
			this.path = path;

			action = new EditAction(name,false)
			{
				public void invoke(View view)
				{
					lastMacro = path;
					Buffer buffer = view.getBuffer();

					try
					{
						buffer.beginCompoundEdit();

						BeanShell.runScript(view,path,
							true,false);
					}
					finally
					{
						buffer.endCompoundEdit();
					}
				}
			};
		}

		public String toString()
		{
			return name;
		}
	}

	/**
	 * Starts recording a temporary macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void recordTemporaryMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}
		if(view.getMacroRecorder() != null)
		{
			GUIUtilities.error(view,"already-recording",new String[0]);
			return;
		}

		Buffer buffer = jEdit.openFile(null,settings + File.separator
			+ "macros","Temporary_Macro.bsh",true,null);

		if(buffer == null)
			return;

		try
		{
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,jEdit.getProperty("macro.temp.header"),null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,Macros.class,bl);
		}

		recordMacro(view,buffer,true);
	}

	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void recordMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}

		if(view.getMacroRecorder() != null)
		{
			GUIUtilities.error(view,"already-recording",new String[0]);
			return;
		}

		String name = GUIUtilities.input(view,"record",null);
		if(name == null)
			return;

		name = name.replace(' ','_');

		Buffer buffer = jEdit.openFile(null,null,
			MiscUtilities.constructPath(settings,"macros",
			name + ".bsh"),true,null);

		if(buffer == null)
			return;

		try
		{
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,jEdit.getProperty("macro.header"),null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,Macros.class,bl);
		}

		recordMacro(view,buffer,false);
	}

	/**
	 * Stops a recording currently in progress.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void stopRecording(View view)
	{
		InputHandler inputHandler = view.getInputHandler();
		Recorder recorder = view.getMacroRecorder();

		if(recorder == null)
			GUIUtilities.error(view,"macro-not-recording",null);
		else
		{
			view.setMacroRecorder(null);
			if(!recorder.temporary)
				view.setBuffer(recorder.buffer);
			recorder.dispose();
		}
	}

	/**
	 * Runs the temporary macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void runTemporaryMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}

		lastMacro = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"macros",
			"Temporary_Macro.bsh");

		Buffer buffer = view.getBuffer();

		try
		{
			buffer.beginCompoundEdit();
			BeanShell.runScript(view,lastMacro,true,false);
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	}

	/**
	 * Runs the most recently run or recorded macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void runLastMacro(View view)
	{
		if(lastMacro == null)
			view.getToolkit().beep();
		else
			BeanShell.runScript(view,lastMacro,true,false);
	}

	// private members
	private static String systemMacroPath;
	private static String userMacroPath;

	private static Vector macroList;
	private static Vector macroHierarchy;
	private static Hashtable macroHash;
	private static String lastMacro;

	static
	{
		EditBus.addToBus(new MacrosEBComponent());
	}

	private static void loadMacros(Vector vector, String path, File directory)
	{
		String[] macroFiles = directory.list();
		if(macroFiles == null)
			return;

		MiscUtilities.quicksort(macroFiles,new MiscUtilities.StringICaseCompare());

		for(int i = 0; i < macroFiles.length; i++)
		{
			String fileName = macroFiles[i];
			File file = new File(directory,fileName);
			if(fileName.toLowerCase().endsWith(".bsh"))
			{
				String label = fileName.substring(0,fileName.length() - 4);
				String name = path + label;
				Macro newMacro = new Macro(name,file.getPath());
				vector.addElement(newMacro);
				macroList.addElement(newMacro);
				macroHash.put(name,newMacro);
			}
			else if(file.isDirectory())
			{
				Vector submenu = new Vector();
				submenu.addElement(fileName.replace('_',' '));
				loadMacros(submenu,path + fileName + '/',file);
				vector.addElement(submenu);
			}
		}
	}

	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @param buffer The buffer to record to
	 * @param temporary True if this is a temporary macro
	 * @since jEdit 3.0pre5
	 */
	private static void recordMacro(View view, Buffer buffer, boolean temporary)
	{
		lastMacro = buffer.getPath();

		view.setMacroRecorder(new Recorder(view,buffer,temporary));

		// setting the message to 'null' causes the status bar to check
		// if a recording is in progress
		view.getStatus().setMessage(null);
	}

	static class MacrosEBComponent implements EBComponent
	{
		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
			{
				BufferUpdate bmsg = (BufferUpdate)msg;
				if(bmsg.getWhat() == BufferUpdate.DIRTY_CHANGED
					&& !bmsg.getBuffer().isDirty())
					maybeReloadMacros(bmsg.getBuffer().getPath());
			}
			else if(msg instanceof VFSUpdate)
			{
				maybeReloadMacros(((VFSUpdate)msg).getPath());
			}
		}

		private void maybeReloadMacros(String path)
		{
			// On Windows and MacOS, path names are case insensitive
			if(File.separatorChar == '\\' || File.separatorChar == ':')
			{
				path = path.toLowerCase();
				if(systemMacroPath != null && path.startsWith(
					systemMacroPath.toLowerCase()))
					loadMacros();

				if(userMacroPath != null && path.startsWith(
					userMacroPath.toLowerCase()))
					loadMacros();
			}
			else
			{
				if(systemMacroPath != null && path.startsWith(systemMacroPath))
					loadMacros();

				if(userMacroPath != null && path.startsWith(userMacroPath))
					loadMacros();
			}
		}
	}

	public static class Recorder implements EBComponent
	{
		View view;
		Buffer buffer;
		boolean temporary;

		boolean lastWasInput;

		public Recorder(View view, Buffer buffer, boolean temporary)
		{
			this.view = view;
			this.buffer = buffer;
			this.temporary = temporary;
			EditBus.addToBus(this);
		}

		public void record(String code)
		{
			if(lastWasInput)
			{
				lastWasInput = false;
				append("\");");
			}

			append("\n");
			append(code);
		}

		public void record(int repeat, String code)
		{
			if(repeat == 1)
				record(code);
			else
			{
				record("for(int i = 1; i <= " + repeat + "; i++)\n"
					+ "{\n"
					+ code + "\n"
					+ "}");
			}
		}

		public void record(int repeat, char ch)
		{
			// record \n and \t on lines specially so that auto indent
			// can take place
			if(ch == '\n')
				record(repeat,"textArea.userInput(\'\\n\');");
			else if(ch == '\t')
				record(repeat,"textArea.userInput(\'\\t\');");
			else
			{
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < repeat; i++)
					buf.append(ch);
				String charStr = MiscUtilities.charsToEscapes(buf.toString());

				if(lastWasInput)
					append(charStr);
				else
				{
					append("\ntextArea.setSelectedText(\"" + charStr);
					lastWasInput = true;
				}
			}
		}

		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
			{
				BufferUpdate bmsg = (BufferUpdate)msg;
				if(bmsg.getWhat() == BufferUpdate.CLOSED)
				{
					if(bmsg.getBuffer() == buffer)
						stopRecording(view);
				}
			}
		}

		private void append(String str)
		{
			try
			{
				buffer.insertString(buffer.getLength(),str,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}

		private void dispose()
		{
			if(lastWasInput)
			{
				lastWasInput = false;
				append("\");");
			}

			int lineCount = buffer.getDefaultRootElement()
				.getElementCount();
			for(int i = 0; i < lineCount; i++)
			{
				buffer.indentLine(i,true,true);
			}

			EditBus.removeFromBus(this);

			// setting the message to 'null' causes the status bar to
			// check if a recording is in progress
			view.getStatus().setMessage(null);
		}
	}
}
