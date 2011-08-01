/*
 * BeanShell.java - BeanShell scripting support
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
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
import org.gjt.sp.jedit.bsh.*;

import java.io.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.notification.NotificationManager;
import org.gjt.sp.jedit.gui.BeanShellErrorDialog;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * BeanShell is jEdit's extension language.<p>
 *
 * When run from jEdit, BeanShell code has access to the following predefined
 * variables:
 *
 * <ul>
 * <li><code>view</code> - the currently active {@link View}.</li>
 * <li><code>editPane</code> - the currently active {@link EditPane}.</li>
 * <li><code>textArea</code> - the edit pane's {@link JEditTextArea}.</li>
 * <li><code>buffer</code> - the edit pane's {@link Buffer}.</li>
 * <li><code>wm</code> - the view's {@link
 * org.gjt.sp.jedit.gui.DockableWindowManager}.</li>
 * <li><code>scriptPath</code> - the path name of the currently executing
 * BeanShell script.</li>
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class BeanShell
{
	private static final BeanShellFacade<View> bsh = new MyBeanShellFacade();
	
	static void init()
	{
		Log.log(Log.MESSAGE, BeanShell.class, "Beanshell Init");
	}

	//{{{ evalSelection() method
	/**
	 * Evaluates the text selected in the specified text area.
	 * @since jEdit 2.7pre2
	 */
	public static void evalSelection(View view, JEditTextArea textArea)
	{
		bsh.evalSelection(view, textArea);
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
					returnValue = bsh._eval(view,bsh.getNameSpace(),command);
				}
			}
			catch(Throwable e)
			{
				Log.log(Log.ERROR,BeanShell.class,e);

				bsh.handleException(view,null,e);
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

		if(command == null || command.length() == 0)
			return;

		Selection[] selection = textArea.getSelection();
		if(selection.length == 0)
		{
			view.getToolkit().beep();
			return;
		}

		if(!command.endsWith(";"))
			command = command + ";";

		String script = "int[] lines = textArea.getSelectedLines();\n"
			+ "for(int i = 0; i < lines.length; i++)\n"
			+ "{\n"
				+ "line = lines[i];\n"
				+ "index = line - lines[0];\n"
				+ "start = buffer.getLineStartOffset(line);\n"
				+ "end = buffer.getLineEndOffset(line);\n"
				+ "text = buffer.getText(start,end - start - 1);\n"
				+ "newText = " + command + "\n"
				+ "if(newText != null)\n"
				+ "{\n"
					+ "buffer.remove(start,end - start - 1);\n"
					+ "buffer.insert(start,String.valueOf(newText));\n"
				+ "}\n"
			+ "}\n";

		if(view.getMacroRecorder() != null)
			view.getMacroRecorder().record(1,script);

		try
		{
			buffer.beginCompoundEdit();

			bsh.eval(view,script);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		textArea.selectNone();
	} //}}}

	//{{{ runScript() method
	/**
	 * Runs a BeanShell script. Errors are shown in a dialog box.<p>
	 *
	 * If the <code>in</code> parameter is non-null, the script is
	 * read from that stream; otherwise it is read from the file identified
	 * by <code>path</code>.<p>
	 *
	 * The <code>scriptPath</code> BeanShell variable is set to the path
	 * name of the script.
	 *
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param path The script file's VFS path.
	 * @param in The reader to read the script from, or <code>null</code>.
	 * @param ownNamespace If set to <code>false</code>, methods and
	 * variables defined in the script will be available to all future
	 * uses of BeanShell; if set to <code>true</code>, they will be lost as
	 * soon as the script finishes executing. jEdit uses a value of
	 * <code>false</code> when running startup scripts, and a value of
	 * <code>true</code> when running all other macros.
	 *
	 * @since jEdit 4.0pre7
	 */
	public static void runScript(View view, String path, Reader in,
		boolean ownNamespace)
	{
		try
		{
			_runScript(view,path,in,ownNamespace);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,BeanShell.class,e);

			bsh.handleException(view,path,e);
		}
	} //}}}

	//{{{ runScript() method
	/**
	 * Runs a BeanShell script. Errors are shown in a dialog box.<p>
	 *
	 * If the <code>in</code> parameter is non-null, the script is
	 * read from that stream; otherwise it is read from the file identified
	 * by <code>path</code>.<p>
	 *
	 * The <code>scriptPath</code> BeanShell variable is set to the path
	 * name of the script.
	 *
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param path The script file's VFS path.
	 * @param in The reader to read the script from, or <code>null</code>.
	 * @param namespace The namespace to run the script in.
	 *
	 * @since jEdit 4.2pre5
	 */
	public static void runScript(View view, String path, Reader in,
		NameSpace namespace)
	{
		try
		{
			_runScript(view,path,in,namespace);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,BeanShell.class,e);

			bsh.handleException(view,path,e);
		}
	} //}}}

	//{{{ _runScript() method
	/**
	 * Runs a BeanShell script. Errors are passed to the caller.<p>
	 *
	 * If the <code>in</code> parameter is non-null, the script is
	 * read from that stream; otherwise it is read from the file identified
	 * by <code>path</code>.<p>
	 *
	 * The <code>scriptPath</code> BeanShell variable is set to the path
	 * name of the script.
	 *
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param path The script file's VFS path.
	 * @param in The reader to read the script from, or <code>null</code>.
	 * @param ownNamespace If set to <code>false</code>, methods and
	 * variables defined in the script will be available to all future
	 * uses of BeanShell; if set to <code>true</code>, they will be lost as
	 * soon as the script finishes executing. jEdit uses a value of
	 * <code>false</code> when running startup scripts, and a value of
	 * <code>true</code> when running all other macros.
	 * @exception Exception instances are thrown when various BeanShell errors
	 * occur
	 * @since jEdit 4.0pre7
	 */
	public static void _runScript(View view, String path, Reader in,
		boolean ownNamespace) throws Exception
	{
		_runScript(view,path,in,ownNamespace
			? new NameSpace(bsh.getNameSpace(),"namespace")
			: bsh.getNameSpace());
	} //}}}

	//{{{ _runScript() method
	/**
	 * Runs a BeanShell script. Errors are passed to the caller.<p>
	 *
	 * If the <code>in</code> parameter is non-null, the script is
	 * read from that stream; otherwise it is read from the file identified
	 * by <code>path</code>.<p>
	 *
	 * The <code>scriptPath</code> BeanShell variable is set to the path
	 * name of the script.
	 *
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param path The script file's VFS path.
	 * @param in The reader to read the script from, or <code>null</code>.
	 * @param namespace The namespace to run the script in.
	 * @exception Exception instances are thrown when various BeanShell errors
	 * occur
	 * @since jEdit 4.2pre5
	 */
	public static void _runScript(View view, String path, Reader in,
		NameSpace namespace) throws Exception
	{
		Log.log(Log.MESSAGE,BeanShell.class,"Running script " + path);

		Interpreter interp = BeanShellFacade.createInterpreter(namespace);

		try
		{
			if(in == null)
			{
				Buffer buffer = jEdit.openTemporary(null,
					null,path,false);

				if(!buffer.isLoaded())
					VFSManager.waitForRequests();

				in = new StringReader(buffer.getText(0,
					buffer.getLength()));
			}

			bsh.setupDefaultVariables(namespace,view);
			interp.set("scriptPath",path);

			running = true;

			interp.eval(in,namespace,path);
		}
		catch(Exception e)
		{
			BeanShellFacade.unwrapException(e);
		}
		finally
		{
			running = false;
			try
			{
				// no need to do this for macros!
				if(namespace == bsh.getNameSpace())
				{
					bsh.resetDefaultVariables(namespace);
					interp.unset("scriptPath");
				}
			}
			catch(EvalError e)
			{
				// do nothing
			}
		}
	} //}}}

	//{{{ eval() method
	/**
	 * Evaluates the specified BeanShell expression. Errors are reported in
	 * a dialog box.
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param namespace The namespace
	 * @param command The expression
	 * @since jEdit 4.0pre8
	 */
	public static Object eval(View view, NameSpace namespace, String command)
	{
		return bsh.eval(view, namespace, command);
	} //}}}

	//{{{ _eval() method
	/**
	 * Evaluates the specified BeanShell expression. Unlike
	 * <code>eval()</code>, this method passes any exceptions to the caller.
	 *
	 * @param view The view. Within the script, references to
	 * <code>buffer</code>, <code>textArea</code> and <code>editPane</code>
	 * are determined with reference to this parameter.
	 * @param namespace The namespace
	 * @param command The expression
	 * @exception Exception instances are thrown when various BeanShell
	 * errors occur
	 * @since jEdit 3.2pre7
	 */
	public static Object _eval(View view, NameSpace namespace, String command)
		throws Exception
	{
		return bsh._eval(view, namespace, command);
	} //}}}

	//{{{ cacheBlock() method
	/**
	 * Caches a block of code, returning a handle that can be passed to
	 * runCachedBlock().
	 * @param id An identifier. If null, a unique identifier is generated
	 * @param code The code
	 * @param namespace If true, the namespace will be set
	 * @exception Exception instances are thrown when various BeanShell errors
	 * occur
	 * @since jEdit 4.1pre1
	 */
	public static BshMethod cacheBlock(String id, String code, boolean namespace)
		throws Exception
	{
		return bsh.cacheBlock(id, code, namespace);
	} //}}}

	//{{{ runCachedBlock() method
	/**
	 * Runs a cached block of code in the specified namespace. Faster than
	 * evaluating the block each time.
	 * @param method The method instance returned by cacheBlock()
	 * @param view The view
	 * @param namespace The namespace to run the code in
	 * @exception Exception instances are thrown when various BeanShell
	 * errors occur
	 * @since jEdit 4.1pre1
	 */
	public static Object runCachedBlock(BshMethod method, View view,
		NameSpace namespace) throws Exception
	{
		return bsh.runCachedBlock(method, view, namespace);
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
		return bsh.getNameSpace();
	} //}}}

	//{{{ Package-private members

	//{{{ resetClassManager() method
	/**
	 * Causes BeanShell internal structures to drop references to cached
	 * Class instances.
	 */
	static void resetClassManager()
	{
		bsh.resetClassManager();
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static boolean running;
	//}}}

	//}}}

	/**
	 * The BeanshellFacade that is used by jEdit.
	 */
	private static class MyBeanShellFacade extends BeanShellFacade<View>
	{
		private MyBeanShellFacade()
		{
			classManager.setClassLoader(new JARClassLoader());
		}

		@Override
		protected void init()
		{
			super.init();
			global.importPackage("org.gjt.sp.jedit.browser");
			global.importPackage("org.gjt.sp.jedit.bufferset");
			global.importPackage("org.gjt.sp.jedit.statusbar");
			global.importPackage("org.gjt.sp.jedit.gui");
			global.importPackage("org.gjt.sp.jedit.help");
			global.importPackage("org.gjt.sp.jedit.io");
			global.importPackage("org.gjt.sp.jedit.menu");
			global.importPackage("org.gjt.sp.jedit.msg");
			global.importPackage("org.gjt.sp.jedit.options");
			global.importPackage("org.gjt.sp.jedit.pluginmgr");
			global.importPackage("org.gjt.sp.jedit.print");
			global.importPackage("org.gjt.sp.jedit.search");
		}
		
		@Override
		protected void setupDefaultVariables(NameSpace namespace, View view) throws UtilEvalError 
		{
			if(view != null)
			{
				EditPane editPane = view.getEditPane();
				setVariable(namespace, "view", view);
				setVariable(namespace, "editPane",editPane);
				setVariable(namespace, "buffer",editPane.getBuffer());
				setVariable(namespace, "textArea",editPane.getTextArea());
				setVariable(namespace, "wm",view.getDockableWindowManager());
			}
		}

		@Override
		protected void resetDefaultVariables(NameSpace namespace) throws UtilEvalError
		{
			namespace.setVariable("view",null, false);
			namespace.setVariable("editPane",null, false);
			namespace.setVariable("buffer",null, false);
			namespace.setVariable("textArea",null, false);
			namespace.setVariable("wm",null, false);
		}

		@Override
		protected void handleException(View view, String path, Throwable t)
		{
			if(t instanceof IOException)
			{
				NotificationManager.error(view,path,"ioerror.read-error",
					new String[] { t.toString() });
			}
			else
				new BeanShellErrorDialog(view,t);
		}
		
	}
}
