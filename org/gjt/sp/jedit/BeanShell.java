/*
 * BeanShell.java - BeanShell scripting support
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001 Slava Pestov
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
import bsh.*;
import javax.swing.text.Segment;
import javax.swing.JFileChooser;
import java.lang.reflect.InvocationTargetException;
import java.io.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.BeanShellErrorDialog;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;
//}}}

public class BeanShell
{
	//{{{ evalSelection() method
	/**
	 * Evaluates the text selected in the specified text area.
	 * @since jEdit 2.7pre2
	 */
	public static void evalSelection(View view, JEditTextArea textArea)
	{
		String command = textArea.getSelectedText();
		if(command == null)
		{
			view.getToolkit().beep();
			return;
		}
		Object returnValue = eval(view,command,false);
		if(returnValue != null)
			textArea.setSelectedText(returnValue.toString());
	} //}}}

	//{{{ showEvaluateDialog() method
	/**
	 * Prompts for a BeanShell expression to evaluate.
	 * @since jEdit 2.7pre2
	 */
	public static void showEvaluateDialog(View view)
	{
		String command = GUIUtilities.input(view,"beanshell-eval-input",null);
		if(command != null)
		{
			if(!command.endsWith(";"))
				command = command + ";";

			int repeat = view.getInputHandler().getRepeatCount();

			if(view.getMacroRecorder() != null)
			{
				view.getMacroRecorder().record(repeat,command);
			}

			Object returnValue = null;
			try
			{
				for(int i = 0; i < repeat; i++)
				{
					returnValue = eval(view,command,true);
				}
			}
			catch(Throwable t)
			{
				// BeanShell error occurred, abort execution
			}

			if(returnValue != null)
			{
				String[] args = { returnValue.toString() };
				GUIUtilities.message(view,"beanshell-eval",args);
			}
		}
	} //}}}

	//{{{ showEvaluateLinesDialog() method
	/**
	 * Evaluates the specified script for each selected line.
	 * @since jEdit 4.0pre1
	 */
	public static void showEvaluateLinesDialog(View view)
	{
		String command = GUIUtilities.input(view,"beanshell-eval-line",null);

		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();

		Selection[] selection = textArea.getSelection();
		if(selection.length == 0 || command == null || command.length() == 0)
		{
			view.getToolkit().beep();
			return;
		}

		if(!command.endsWith(";"))
			command = command + ";";

		if(view.getMacroRecorder() != null)
			view.getMacroRecorder().record(1,command);

		try
		{
			buffer.beginCompoundEdit();

			

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];
				for(int j = s.getStartLine(); j <= s.getEndLine(); j++)
				{
					// if selection ends on the start of a
					// line, don't filter that line
					if(s.getEnd() == textArea.getLineStartOffset(j))
						break;

					global.setVariable("line",new Integer(j));
					global.setVariable("index",new Integer(
						j - s.getStartLine()));
					int start = s.getStart(buffer,j);
					int end = s.getEnd(buffer,j);
					String text = buffer.getText(start,
						end - start);
					global.setVariable("text",text);

					Object returnValue = eval(view,command,true);
					if(returnValue != null)
					{
						buffer.remove(start,end - start);
						buffer.insert(start,
							returnValue.toString());
					}
				}
			}
		}
		catch(Throwable e)
		{
			// BeanShell error occurred, abort execution
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		textArea.selectNone();
	} //}}}

	//{{{ showRunScriptDialog() method
	/**
	 * Prompts for a BeanShell script to run.
	 * @since jEdit 2.7pre2
	 */
	public static void showRunScriptDialog(View view)
	{
		Macros.showRunScriptDialog(view);
	} //}}}

	//{{{ runScript() method
	/**
	 * Runs a BeanShell script.
	 * @param view The view
	 * @param path The path name of the script. May be a jEdit VFS path
	 * @param ownNamespace Macros are run in their own namespace, startup
	 * scripts are run on the global namespace
	 * @param rethrowBshErrors Rethrow BeanShell errors, in addition to
	 * showing an error dialog box
	 * @since jEdit 2.7pre3
	 */
	public static void runScript(View view, String path,
		boolean ownNamespace, boolean rethrowBshErrors)
	{
		Reader in;
		Buffer buffer = jEdit.getBuffer(path);

		VFS vfs = VFSManager.getVFSForPath(path);
		Object session = vfs.createVFSSession(path,view);
		if(session == null)
		{
			// user cancelled???
			return;
		}

		try
		{
			if(buffer != null)
			{
				if(!buffer.isLoaded())
					VFSManager.waitForRequests();

				in = new StringReader(buffer.getText(0,
					buffer.getLength()));
			}
			else
			{
				in = new BufferedReader(new InputStreamReader(
					vfs._createInputStream(session,path,
					true,view)));
			}

			runScript(view,path,in,ownNamespace,rethrowBshErrors);
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"read-error",
				new String[] { path, e.toString() });
			return;
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,view);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,BeanShell.class,io);
				GUIUtilities.error(view,"read-error",
					new String[] { path, io.toString() });
			}
		}
	} //}}}

	//{{{ runScript() method
	/**
	 * Runs a BeanShell script.
	 * @param view The view
	 * @param path For error reporting only
	 * @param in The reader to read the script from
	 * @param ownNamespace Macros are run in their own namespace, startup
	 * scripts are run on the global namespace
	 * @param rethrowBshErrors Rethrow BeanShell errors, in addition to
	 * showing an error dialog box
	 * @since jEdit 3.2pre4
	 */
	public static void runScript(View view, String path, Reader in,
		boolean ownNamespace, boolean rethrowBshErrors)
	{
		Log.log(Log.MESSAGE,BeanShell.class,"Running script " + path);

		NameSpace namespace;
		if(ownNamespace)
			namespace = new NameSpace(global,"script namespace");
		else
			namespace = global;

		Interpreter interp = createInterpreter(namespace);

		try
		{
			if(view != null)
			{
				EditPane editPane = view.getEditPane();
				interp.set("view",view);
				interp.set("editPane",editPane);
				interp.set("buffer",editPane.getBuffer());
				interp.set("textArea",editPane.getTextArea());
			}

			running = true;

			interp.eval(in,namespace,path);
		}
		catch(Throwable e)
		{
			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);

			new BeanShellErrorDialog(view,e.toString());

			if(e instanceof Error && rethrowBshErrors)
				throw (Error)e;
		}
		finally
		{
			running = false;
		}
	} //}}}

	//{{{ eval() method
	/**
	 * Evaluates the specified BeanShell expression.
	 * @param view The view (may be null)
	 * @param command The expression
	 * @param rethrowBshErrors If true, BeanShell errors will
	 * be re-thrown to the caller
	 * @since jEdit 2.7pre3
	 */
	public static Object eval(View view, String command,
		boolean rethrowBshErrors)
	{
		return eval(view,global,command,rethrowBshErrors);
	} //}}}

	//{{{ eval() method
	/**
	 * Evaluates the specified BeanShell expression.
	 * @param view The view (may be null)
	 * @param namespace The namespace
	 * @param command The expression
	 * @param rethrowBshErrors If true, BeanShell errors will
	 * be re-thrown to the caller
	 * @since jEdit 3.2pre7
	 */
	public static Object eval(View view, NameSpace namespace,
		String command, boolean rethrowBshErrors)
	{
		Interpreter interp = createInterpreter(namespace);

		try
		{
			if(view != null)
			{
				EditPane editPane = view.getEditPane();
				interp.set("view",view);
				interp.set("editPane",editPane);
				interp.set("buffer",editPane.getBuffer());
				interp.set("textArea",editPane.getTextArea());
			}

			return interp.eval(command);
		}
		catch(Throwable e)
		{
			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);

			new BeanShellErrorDialog(view,e.toString());

			if(e instanceof Error && rethrowBshErrors)
				throw (Error)e;
		}

		return null;
	} //}}}

	//{{{ cacheBlock() method
	/**
	 * Caches a block of code, returning a handle that can be passed to
	 * runCachedBlock().
	 * @param id An identifier. If null, a unique identifier is generated
	 * @param code The code
	 * @param childNamespace If the method body should be run in a new
	 * namespace (slightly faster). Note that you must pass a null namespace
	 * to the runCachedBlock() method if you do this
	 * @since jEdit 3.2pre5
	 */
	public static String cacheBlock(String id, String code, boolean childNamespace)
	{
		String name;
		if(id == null)
			name = "b_" + (cachedBlockCounter++);
		else
			name = "b_" + id;

		code = "setNameSpace(__cruft.namespace);\n"
			+ name
			+ "(ns) {\n"
			+ "setNameSpace(ns);"
			+ code
			+ "\n}";

		eval(null,code,false);

		return name;
	} //}}}

	//{{{ runCachedBlock() method
	/**
	 * Runs a cached block of code in the specified namespace. Faster than
	 * evaluating the block each time.
	 * @param id The identifier returned by cacheBlock()
	 * @param view The view
	 * @param namespace The namespace to run the code in. Can only be null if
	 * childNamespace parameter was true in cacheBlock() call
	 * @since jEdit 3.2pre5
	 */
	public static Object runCachedBlock(String id, View view, NameSpace namespace)
	{
		if(namespace == null)
			namespace = global;

		Object[] args = { namespace };

		try
		{
			if(view != null)
			{
				namespace.setVariable("view",view);
				EditPane editPane = view.getEditPane();
				namespace.setVariable("editPane",editPane);
				namespace.setVariable("buffer",editPane.getBuffer());
				namespace.setVariable("textArea",editPane.getTextArea());
			}

			Object retVal = internal.invokeMethod(id,args,interpForMethods);
			if(retVal instanceof Primitive)
			{
				if(retVal == Primitive.VOID)
					return null;
				else
					return ((Primitive)retVal).getValue();
			}
			else
				return retVal;
		}
		catch(Throwable e)
		{
			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);

			new BeanShellErrorDialog(view,e.toString());
		}
		finally
		{
			try
			{
				namespace.setVariable("view",null);
				namespace.setVariable("editPane",null);
				namespace.setVariable("buffer",null);
				namespace.setVariable("textArea",null);
			}
			catch(EvalError e)
			{
				// can't do much
			}
		}

		return null;
	} //}}}

	//{{{ isScriptRunning() method
	/**
	 * Returns if a BeanShell script or macro is currently running.
	 * @since jEdit 2.7pre2
	 */
	public static boolean isScriptRunning()
	{
		return running;
	} //}}}

	//{{{ getNameSpace() method
	/**
	 * Returns the global namespace.
	 * @since jEdit 3.2pre5
	 */
	public static NameSpace getNameSpace()
	{
		return global;
	} //}}}

	//{{{ Package-private members

	//{{{ init() method
	static void init()
	{
		BshClassManager.setClassLoader(new JARClassLoader());

		global = new NameSpace("jEdit embedded BeanShell interpreter");
		global.importPackage("org.gjt.sp.jedit");
		global.importPackage("org.gjt.sp.jedit.browser");
		global.importPackage("org.gjt.sp.jedit.gui");
		global.importPackage("org.gjt.sp.jedit.io");
		global.importPackage("org.gjt.sp.jedit.msg");
		global.importPackage("org.gjt.sp.jedit.options");
		global.importPackage("org.gjt.sp.jedit.pluginmgr");
		global.importPackage("org.gjt.sp.jedit.print");
		global.importPackage("org.gjt.sp.jedit.search");
		global.importPackage("org.gjt.sp.jedit.syntax");
		global.importPackage("org.gjt.sp.jedit.textarea");
		global.importPackage("org.gjt.sp.util");

		interpForMethods = createInterpreter(global);

		internal = (NameSpace)eval(null,"__cruft = object();__cruft.namespace;",false);

		Log.log(Log.DEBUG,BeanShell.class,"BeanShell interpreter version "
			+ Interpreter.VERSION);
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private static Interpreter interpForMethods;
	private static NameSpace global;
	private static NameSpace internal;
	private static boolean running;
	private static int cachedBlockCounter;
	//}}}

	//{{{ createInterpreter() method
	private static Interpreter createInterpreter(NameSpace nameSpace)
	{
		return new Interpreter(null,System.out,System.err,false,nameSpace);
	} //}}}

	//}}}
}
