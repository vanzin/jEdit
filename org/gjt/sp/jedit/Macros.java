/*
 * Macros.java - Macro manager
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Portions copyright (C) 2002 mike dillon
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
import gnu.regexp.RE;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * This class records and runs macros.<p>
 *
 * It also contains a few methods useful for displaying output messages
 * or obtaining input from a macro:
 *
 * <ul>
 * <li>{@link #confirm(Component,String,int)}</li>
 * <li>{@link #confirm(Component,String,int,int)}</li>
 * <li>{@link #error(Component,String)}</li>
 * <li>{@link #input(Component,String)}</li>
 * <li>{@link #input(Component,String,String)}</li>
 * <li>{@link #message(Component,String)}</li>
 * </ul>
 *
 * Note that plugins should not use the above methods. Call
 * the methods in the {@link GUIUtilities} class instead.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Macros
{
	//{{{ showRunScriptDialog() method
	/**
	 * Prompts for one or more files to run as macros
	 * @param view The view
	 * @since jEdit 4.0pre7
	 */
	public static void showRunScriptDialog(View view)
	{
		String[] paths = GUIUtilities.showVFSFileDialog(view,
			null,JFileChooser.OPEN_DIALOG,true);
		if(paths != null)
		{
			Buffer buffer = view.getBuffer();
			try
			{
				buffer.beginCompoundEdit();

file_loop:			for(int i = 0; i < paths.length; i++)
					runScript(view,paths[i],false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	} //}}}

	//{{{ runScript() method
	/**
	 * Runs the specified script.
	 * Unlike the {@link BeanShell#runScript(View,String,Reader,boolean)}
	 * method, this method can run scripts supported
	 * by any registered macro handler.
	 * @param view The view
	 * @param path The VFS path of the script
	 * @param ignoreUnknown If true, then unknown file types will be
	 * ignored; otherwise, a warning message will be printed and they will
	 * be evaluated as BeanShell scripts.
	 *
	 * @since jEdit 4.1pre2
	 */
	public static void runScript(View view, String path, boolean ignoreUnknown)
	{
		Handler handler = getHandlerForPathName(path);
		if(handler != null)
		{
			try
			{
				Macro newMacro = handler.createMacro(
					MiscUtilities.getFileName(path), path);
				newMacro.invoke(view);
			}
			catch (Exception e)
			{
				Log.log(Log.ERROR, Macros.class, e);
				return;
			}
			return;
		}

		// only executed if above loop falls
		// through, ie there is no handler for
		// this file
		if(ignoreUnknown)
		{
			Log.log(Log.NOTICE,Macros.class,path +
				": Cannot find a suitable macro handler");
		}
		else
		{
			Log.log(Log.ERROR,Macros.class,path +
				": Cannot find a suitable macro handler, "
				+ "assuming BeanShell");
			getHandler("beanshell").createMacro(
				path,path).invoke(view);
		}
	} //}}}

	//{{{ message() method
	/**
	 * Utility method that can be used to display a message dialog in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param message The message
	 * @since jEdit 2.7pre2
	 */
	public static void message(Component comp, String message)
	{
		GUIUtilities.hideSplashScreen();

		JOptionPane.showMessageDialog(comp,message,
			jEdit.getProperty("macro-message.title"),
			JOptionPane.INFORMATION_MESSAGE);
	} //}}}

	//{{{ error() method
	/**
	 * Utility method that can be used to display an error dialog in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param message The message
	 * @since jEdit 2.7pre2
	 */
	public static void error(Component comp, String message)
	{
		GUIUtilities.hideSplashScreen();

		JOptionPane.showMessageDialog(comp,message,
			jEdit.getProperty("macro-message.title"),
			JOptionPane.ERROR_MESSAGE);
	} //}}}

	//{{{ input() method
	/**
	 * Utility method that can be used to prompt for input in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param prompt The prompt string
	 * @since jEdit 2.7pre2
	 */
	public static String input(Component comp, String prompt)
	{
		GUIUtilities.hideSplashScreen();

		return input(comp,prompt,null);
	} //}}}

	//{{{ input() method
	/**
	 * Utility method that can be used to prompt for input in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param prompt The prompt string
	 * @since jEdit 3.1final
	 */
	public static String input(Component comp, String prompt, String defaultValue)
	{
		GUIUtilities.hideSplashScreen();

		return (String)JOptionPane.showInputDialog(comp,prompt,
			jEdit.getProperty("macro-input.title"),
			JOptionPane.QUESTION_MESSAGE,null,null,defaultValue);
	} //}}}

	//{{{ confirm() method
	/**
	 * Utility method that can be used to ask for confirmation in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param prompt The prompt string
	 * @param buttons The buttons to display - for example,
	 * JOptionPane.YES_NO_CANCEL_OPTION
	 * @since jEdit 4.0pre2
	 */
	public static int confirm(Component comp, String prompt, int buttons)
	{
		GUIUtilities.hideSplashScreen();

		return JOptionPane.showConfirmDialog(comp,prompt,
			jEdit.getProperty("macro-confirm.title"),buttons,
			JOptionPane.QUESTION_MESSAGE);
	} //}}}

	//{{{ confirm() method
	/**
	 * Utility method that can be used to ask for confirmation in a macro.
	 * @param comp The component to show the dialog on behalf of, this
	 * will usually be a view instance
	 * @param prompt The prompt string
	 * @param buttons The buttons to display - for example,
	 * JOptionPane.YES_NO_CANCEL_OPTION
	 * @param type The dialog type - for example,
	 * JOptionPane.WARNING_MESSAGE
	 */
	public static int confirm(Component comp, String prompt, int buttons, int type)
	{
		GUIUtilities.hideSplashScreen();

		return JOptionPane.showConfirmDialog(comp,prompt,
			jEdit.getProperty("macro-confirm.title"),buttons,type);
	} //}}}

	//{{{ loadMacros() method
	/**
	 * Rebuilds the macros list, and sends a MacrosChanged message
	 * (views update their Macros menu upon receiving it)
	 * @since jEdit 2.2pre4
	 */
	public static void loadMacros()
	{
		macroActionSet.removeAllActions();
		macroHierarchy.removeAllElements();
		macroHash.clear();

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

		EditBus.send(new DynamicMenuChanged("macros"));
	} //}}}

	//{{{ registerHandler() method
	/**
	 * Adds a macro handler to the handlers list
	 * @since jEdit 4.0pre6
	 */
	public static void registerHandler(Handler handler)
	{
		if (getHandler(handler.getName()) != null)
		{
			Log.log(Log.ERROR, Macros.class, "Cannot register more than one macro handler with the same name");
			return;
		}

		Log.log(Log.DEBUG,Macros.class,"Registered " + handler.getName()
			+ " macro handler");
		macroHandlers.add(handler);
	} //}}}

	//{{{ getHandlers() method
	/**
	 * Returns an array containing the list of registered macro handlers
	 * @since jEdit 4.0pre6
	 */
	public static Handler[] getHandlers()
	{
		Handler[] handlers = new Handler[macroHandlers.size()];
		return (Handler[])macroHandlers.toArray(handlers);
	} //}}}

	//{{{ getHandlerForFileName() method
	/**
	 * Returns the macro handler suitable for running the specified file
	 * name, or null if there is no suitable handler.
	 * @since jEdit 4.1pre3
	 */
	public static Handler getHandlerForPathName(String pathName)
	{
		for (int i = 0; i < macroHandlers.size(); i++)
		{
			Handler handler = (Handler)macroHandlers.get(i);
			if (handler.accept(pathName))
				return handler;
		}

		return null;
	} //}}}

	//{{{ getHandler() method
	/**
	 * Returns the macro handler with the specified name, or null if
	 * there is no registered handler with that name.
	 * @since jEdit 4.0pre6
	 */
	public static Handler getHandler(String name)
	{
		Handler handler = null;
		for (int i = 0; i < macroHandlers.size(); i++)
		{
			handler = (Handler)macroHandlers.get(i);
			if (handler.getName().equals(name)) return handler;
		}

		return null;
	}
	//}}}

	//{{{ getMacroHierarchy() method
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
	} //}}}

	//{{{ getMacroActionSet() method
	/**
	 * Returns an action set with all known macros in it.
	 * @since jEdit 4.0pre1
	 */
	public static ActionSet getMacroActionSet()
	{
		return macroActionSet;
	} //}}}

	//{{{ getMacro() method
	/**
	 * Returns the macro with the specified name.
	 * @param macro The macro's name
	 * @since jEdit 2.6pre1
	 */
	public static Macro getMacro(String macro)
	{
		return (Macro)macroHash.get(macro);
	} //}}}

	//{{{ Macro class
	/**
	 * Encapsulates the macro's label, name and path.
	 * @since jEdit 2.2pre4
	 */
	public static class Macro extends EditAction
	{
		//{{{ Macro constructor
		public Macro(Handler handler, String name, String label, String path)
		{
			// in case macro file name has a space in it.
			// spaces break the view.toolBar property, for instance,
			// since it uses spaces to delimit action names.
			super(name.replace(' ','_'));
			this.handler = handler;
			this.label = label;
			this.path = path;

			jEdit.setTemporaryProperty(getName() + ".label",label);
			jEdit.setTemporaryProperty(getName() + ".mouse-over",
				handler.getLabel() + " - " + path);
		} //}}}

		//{{{ getHandler() method
		public Handler getHandler()
		{
			return handler;
		}
		//}}}

		//{{{ getPath() method
		public String getPath()
		{
			return path;
		} //}}}

		//{{{ invoke() method
		public void invoke(View view)
		{
			if(view == null)
				handler.runMacro(null,this);
			else
			{
				Buffer buffer = view.getBuffer();

				try
				{
					buffer.beginCompoundEdit();
					handler.runMacro(view,this);
				}
				finally
				{
					buffer.endCompoundEdit();
				}
			}
		} //}}}

		//{{{ getCode() method
		public String getCode()
		{
			return "Macros.getMacro(\"" + getName() + "\").invoke(view);";
		} //}}}

		//{{{ macroNameToLabel() method
		public static String macroNameToLabel(String macroName)
		{
			int index = macroName.lastIndexOf('/');
			return macroName.substring(index + 1).replace('_', ' ');
		}
		//}}}

		//{{{ Private members
		private Handler handler;
		private String path;
		private String label;
		//}}}
	} //}}}

	//{{{ recordTemporaryMacro() method
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

		buffer.remove(0,buffer.getLength());
		buffer.insert(0,jEdit.getProperty("macro.temp.header"));

		recordMacro(view,buffer,true);
	} //}}}

	//{{{ recordMacro() method
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

		buffer.remove(0,buffer.getLength());
		buffer.insert(0,jEdit.getProperty("macro.header"));

		recordMacro(view,buffer,false);
	} //}}}

	//{{{ stopRecording() method
	/**
	 * Stops a recording currently in progress.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void stopRecording(View view)
	{
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
	} //}}}

	//{{{ runTemporaryMacro() method
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

		String path = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"macros",
			"Temporary_Macro.bsh");

		Handler handler = getHandler("beanshell");
		Macro temp = handler.createMacro(path,path);

		Buffer buffer = view.getBuffer();

		try
		{
			buffer.beginCompoundEdit();
			temp.invoke(view);
		}
		finally
		{
			/* I already wrote a comment expaining this in
			 * Macro.invoke(). */
			if(buffer.insideCompoundEdit())
				buffer.endCompoundEdit();
		}
	} //}}}

	//{{{ Private members

	//{{{ Static variables
	private static String systemMacroPath;
	private static String userMacroPath;

	private static ArrayList macroHandlers;

	private static ActionSet macroActionSet;
	private static Vector macroHierarchy;
	private static Hashtable macroHash;
	//}}}

	//{{{ Class initializer
	static
	{
		macroHandlers = new ArrayList();
		registerHandler(new BeanShellHandler());
		macroActionSet = new ActionSet(jEdit.getProperty("action-set.macros"));
		jEdit.addActionSet(macroActionSet);
		macroHierarchy = new Vector();
		macroHash = new Hashtable();
	} //}}}

	//{{{ loadMacros() method
	private static void loadMacros(Vector vector, String path, File directory)
	{
		File[] macroFiles = directory.listFiles();
		if(macroFiles == null || macroFiles.length == 0)
			return;

		for(int i = 0; i < macroFiles.length; i++)
		{
			File file = macroFiles[i];
			String fileName = file.getName();
			if(file.isHidden())
			{
				/* do nothing! */
				continue;
			}
			else if(file.isDirectory())
			{
				String submenuName = fileName.replace('_',' ');
				Vector submenu = null;
				//{{{ try to merge with an existing menu first
				for(int j = 0; j < vector.size(); j++)
				{
					Object obj = vector.get(j);
					if(obj instanceof Vector)
					{
						Vector vec = (Vector)obj;
						if(((String)vec.get(0)).equals(submenuName))
						{
							submenu = vec;
							break;
						}
					}
				} //}}}
				if(submenu == null)
				{
					submenu = new Vector();
					submenu.addElement(submenuName);
					vector.addElement(submenu);
				}

				loadMacros(submenu,path + fileName + '/',file);
			}
			else
			{
				Handler handler = getHandlerForPathName(file.getPath());

				if(handler == null)
					continue;

				try
				{
					Macro newMacro = handler.createMacro(
						path + fileName, file.getPath());
					vector.addElement(newMacro.getName());
					macroActionSet.addAction(newMacro);
					macroHash.put(newMacro.getName(),newMacro);
				}
				catch (Exception e)
				{
					Log.log(Log.ERROR, Macros.class, e);
					macroHandlers.remove(handler);
				}
			}
		}
	} //}}}

	//{{{ recordMacro() method
	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @param buffer The buffer to record to
	 * @param temporary True if this is a temporary macro
	 * @since jEdit 3.0pre5
	 */
	private static void recordMacro(View view, Buffer buffer, boolean temporary)
	{
		Handler handler = getHandler("beanshell");
		String path = buffer.getPath();

		view.setMacroRecorder(new Recorder(view,buffer,temporary));

		// setting the message to 'null' causes the status bar to check
		// if a recording is in progress
		view.getStatus().setMessage(null);
	} //}}}

	//}}}

	//{{{ Recorder class
	/**
	 * Handles macro recording.
	 */
	public static class Recorder implements EBComponent
	{
		View view;
		Buffer buffer;
		boolean temporary;

		boolean lastWasInput;

		//{{{ Recorder constructor
		public Recorder(View view, Buffer buffer, boolean temporary)
		{
			this.view = view;
			this.buffer = buffer;
			this.temporary = temporary;
			EditBus.addToBus(this);
		} //}}}

		//{{{ record() method
		public void record(String code)
		{
			if(lastWasInput)
			{
				lastWasInput = false;
				append("\");");
			}

			append("\n");
			append(code);
		} //}}}

		//{{{ record() method
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
		} //}}}

		//{{{ record() method
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
		} //}}}

		//{{{ handleMessage() method
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
		} //}}}

		//{{{ append() method
		private void append(String str)
		{
			buffer.insert(buffer.getLength(),str);
		} //}}}

		//{{{ dispose() method
		private void dispose()
		{
			if(lastWasInput)
			{
				lastWasInput = false;
				append("\");");
			}

			for(int i = 0; i < buffer.getLineCount(); i++)
			{
				buffer.indentLine(i);
			}

			EditBus.removeFromBus(this);

			// setting the message to 'null' causes the status bar to
			// check if a recording is in progress
			view.getStatus().setMessage(null);
		} //}}}
	} //}}}

	//{{{ Handler class
	/**
	 * Encapsulates creating and invoking macros in arbitrary scripting languages
	 * @since jEdit 4.0pre6
	 */
	public static abstract class Handler
	{
		//{{{ getName() method
		public String getName()
		{
			return name;
		} //}}}

		//{{{ getLabel() method
		public String getLabel()
		{
			return label;
		} //}}}

		//{{{ accept() method
		public boolean accept(String path)
		{
			return filter.isMatch(MiscUtilities.getFileName(path));
		} //}}}

		//{{{ createMacro() method
		public abstract Macro createMacro(String macroName, String path);
		//}}}

		//{{{ runMacro() method
		/**
		 * Runs the specified macro.
		 * @param view The view - may be null.
		 * @param macro The macro.
		 */
		public abstract void runMacro(View view, Macro macro);
		//}}}

		//{{{ runMacro() method
		/**
		 * Runs the specified macro. This method is optional; it is
		 * called if the specified macro is a startup script. The
		 * default behavior is to simply call {@link #runMacro(View,Macros.Macro)}.
		 *
		 * @param view The view - may be null.
		 * @param macro The macro.
		 * @param ownNamespace  A hint indicating whenever functions and
		 * variables defined in the script are to be self-contained, or
		 * made available to other scripts. The macro handler may ignore
		 * this parameter.
		 * @since jEdit 4.1pre3
		 */
		public void runMacro(View view, Macro macro, boolean ownNamespace)
		{
			runMacro(view,macro);
		} //}}}

		//{{{ Handler constructor
		protected Handler(String name)
		{
			this.name = name;
			label = jEdit.getProperty("macro-handler."
				+ name + ".label", name);
			try
			{
				filter = new RE(MiscUtilities.globToRE(
					jEdit.getProperty(
					"macro-handler." + name + ".glob")));
			}
			catch (Exception e)
			{
				throw new InternalError("Missing or invalid glob for handler " + name);
			}
		} //}}}

		//{{{ Private members
		private String name;
		private String label;
		private RE filter;
		//}}}
	} //}}}

	//{{{ BeanShellHandler class
	static class BeanShellHandler extends Handler
	{
		//{{{ BeanShellHandler constructor
		BeanShellHandler()
		{
			super("beanshell");
		} //}}}

		//{{{ createMacro() method
		public Macro createMacro(String macroName, String path)
		{
			// Remove '.bsh'
			macroName = macroName.substring(0, macroName.length() - 4);

			return new Macro(this, macroName,
				Macro.macroNameToLabel(macroName), path);
		} //}}}

		//{{{ runMacro() method
		public void runMacro(View view, Macro macro)
		{
			BeanShell.runScript(view,macro.getPath(),null,true);
		} //}}}

		//{{{ runMacro() method
		public void runMacro(View view, Macro macro, boolean ownNamespace)
		{
			BeanShell.runScript(view,macro.getPath(),null,ownNamespace);
		} //}}}
	} //}}}
}
