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
import org.gjt.sp.jedit.bsh.classpath.ClassManagerImpl;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.gjt.sp.jedit.io.*;
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
	private static final String REQUIRED_VERSION = "2.0b1.1-jedit-1";

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
		Object returnValue = eval(view,global,command);
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
					returnValue = _eval(view,global,command);
				}
			}
			catch(Throwable e)
			{
				Log.log(Log.ERROR,BeanShell.class,e);

				handleException(view,null,e);
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

			BeanShell.eval(view,global,script);
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

			handleException(view,path,e);
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

			handleException(view,path,e);
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
			? new NameSpace(global,"namespace")
			: global);
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

		Interpreter interp = createInterpreter(namespace);

		VFS vfs = null;
		Object session = null;

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

			setupDefaultVariables(namespace,view);
			interp.set("scriptPath",path);

			running = true;

			interp.eval(in,namespace,path);
		}
		catch(Exception e)
		{
			unwrapException(e);
		}
		finally
		{
			running = false;

			if(session != null)
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

			try
			{
				// no need to do this for macros!
				if(namespace == global)
				{
					resetDefaultVariables(namespace);
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
		try
		{
			return _eval(view,namespace,command);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,BeanShell.class,e);

			handleException(view,null,e);
		}

		return null;
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
		Interpreter interp = createInterpreter(namespace);

		try
		{
			setupDefaultVariables(namespace,view);
			if(Debug.BEANSHELL_DEBUG)
				Log.log(Log.DEBUG,BeanShell.class,command);
			return interp.eval(command);
		}
		catch(Exception e)
		{
			unwrapException(e);
			// never called
			return null;
		}
		finally
		{
			try
			{
				resetDefaultVariables(namespace);
			}
			catch(UtilEvalError e)
			{
				// do nothing
			}
		}
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
		String name = "__internal_" + id;

		// evaluate a method declaration
		if(namespace)
		{
			_eval(null,global,name + "(ns) {\nthis.callstack.set(0,ns);\n" + code + "\n}");
			return global.getMethod(name,new Class[] { NameSpace.class });
		}
		else
		{
			_eval(null,global,name + "() {\n" + code + "\n}");
			return global.getMethod(name,new Class[0]);
		}
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
		boolean useNamespace;
		if(namespace == null)
		{
			useNamespace = false;
			namespace = global;
		}
		else
			useNamespace = true;

		try
		{
			setupDefaultVariables(namespace,view);

			Object retVal = method.invoke(useNamespace
				? new Object[] { namespace }
				: NO_ARGS,
				interpForMethods,new CallStack(), null);
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
		catch(Exception e)
		{
			unwrapException(e);
			// never called
			return null;
		}
		finally
		{
			resetDefaultVariables(namespace);
		}
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

	//{{{ Deprecated functions

	//{{{ runScript() method
	/**
	 * @deprecated The <code>rethrowBshErrors</code> parameter is now
	 * obsolete; call <code>_runScript()</code> or <code>runScript()</code>
	 * instead.
	 */
	public static void runScript(View view, String path,
		boolean ownNamespace, boolean rethrowBshErrors)
	{
		runScript(view,path,null,ownNamespace);
	} //}}}

	//{{{ runScript() method
	/**
	 * @deprecated The <code>rethrowBshErrors</code> parameter is now
	 * obsolete; call <code>_runScript()</code> or <code>runScript()</code>
	 * instead.
	 */
	public static void runScript(View view, String path, Reader in,
		boolean ownNamespace, boolean rethrowBshErrors)
	{
		runScript(view,path,in,ownNamespace);
	} //}}}

	//{{{ eval() method
	/**
	 * @deprecated The <code>rethrowBshErrors</code> parameter is now
	 * obsolete; call <code>_eval()</code> or <code>eval()</code> instead.
	 */
	public static Object eval(View view, String command,
		boolean rethrowBshErrors)
	{
		return eval(view,global,command);
	} //}}}

	//{{{ eval() method
	/**
	 * @deprecated The <code>rethrowBshErrors</code> parameter is now
	 * obsolete; call <code>_eval()</code> or <code>eval()</code> instead.
	 */
	public static Object eval(View view, NameSpace namespace,
		String command, boolean rethrowBshErrors)
	{
		return eval(view,namespace,command);
	} //}}}

	//}}}

	//{{{ Package-private members

	//{{{ init() method
	static void init()
	{
		/*try
		{
			NameSpace.class.getMethod("addCommandPath",
				new Class[] { String.class, Class.class });
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,BeanShell.class,"You have BeanShell version " + getVersion() + " in your CLASSPATH.");
			Log.log(Log.ERROR,BeanShell.class,"Please remove it from the CLASSPATH since jEdit can only run with the bundled BeanShell version " + REQUIRED_VERSION);
			System.exit(1);
		} */

		classManager = new ClassManagerImpl();
		classManager.setClassLoader(new JARClassLoader());

		global = new NameSpace(classManager,
			"jEdit embedded BeanShell interpreter");
		global.importPackage("org.gjt.sp.jedit");
		global.importPackage("org.gjt.sp.jedit.browser");
		global.importPackage("org.gjt.sp.jedit.buffer");
		global.importPackage("org.gjt.sp.jedit.gui");
		global.importPackage("org.gjt.sp.jedit.help");
		global.importPackage("org.gjt.sp.jedit.io");
		global.importPackage("org.gjt.sp.jedit.menu");
		global.importPackage("org.gjt.sp.jedit.msg");
		global.importPackage("org.gjt.sp.jedit.options");
		global.importPackage("org.gjt.sp.jedit.pluginmgr");
		global.importPackage("org.gjt.sp.jedit.print");
		global.importPackage("org.gjt.sp.jedit.search");
		global.importPackage("org.gjt.sp.jedit.syntax");
		global.importPackage("org.gjt.sp.jedit.textarea");
		global.importPackage("org.gjt.sp.util");

		interpForMethods = createInterpreter(global);
	} //}}}

	//{{{ resetClassManager() method
	/**
	 * Causes BeanShell internal structures to drop references to cached
	 * Class instances.
	 */
	static void resetClassManager()
	{
		classManager.reset();
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static final Object[] NO_ARGS = new Object[0];
	private static BshClassManager classManager;
	private static Interpreter interpForMethods;
	private static NameSpace global;
	private static boolean running;
	//}}}

	//{{{ setupDefaultVariables() method
	private static void setupDefaultVariables(NameSpace namespace, View view)
		throws UtilEvalError
	{
		if(view != null)
		{
			EditPane editPane = view.getEditPane();
			namespace.setVariable("view",view, false);
			namespace.setVariable("editPane",editPane, false);
			namespace.setVariable("buffer",editPane.getBuffer(), false);
			namespace.setVariable("textArea",editPane.getTextArea(), false);
			namespace.setVariable("wm",view.getDockableWindowManager(), false);
		}
	} //}}}

	//{{{ resetDefaultVariables() method
	private static void resetDefaultVariables(NameSpace namespace)
		throws UtilEvalError
	{
		namespace.setVariable("view",null, false);
		namespace.setVariable("editPane",null, false);
		namespace.setVariable("buffer",null, false);
		namespace.setVariable("textArea",null, false);
		namespace.setVariable("wm",null, false);
	} //}}}

	//{{{ unwrapException() method
	/**
	 * This extracts an exception from a 'wrapping' exception, as BeanShell
	 * sometimes throws. This gives the user a more accurate error traceback
	 */
	private static void unwrapException(Exception e) throws Exception
	{
		if(e instanceof TargetError)
		{
			Throwable t = ((TargetError)e).getTarget();
			if(t instanceof Exception)
				throw (Exception)t;
			else if(t instanceof Error)
				throw (Error)t;
		}

		if(e instanceof InvocationTargetException)
		{
			Throwable t = ((InvocationTargetException)e).getTargetException();
			if(t instanceof Exception)
				throw (Exception)t;
			else if(t instanceof Error)
				throw (Error)t;
		}

		throw e;
	} //}}}

	//{{{ handleException() method
	private static void handleException(View view, String path, Throwable t)
	{
		if(t instanceof IOException)
		{
			VFSManager.error(view,path,"ioerror.read-error",
				new String[] { t.toString() });
		}
		else
			new BeanShellErrorDialog(view,t);
	} //}}}

	//{{{ createInterpreter() method
	private static Interpreter createInterpreter(NameSpace nameSpace)
	{
		return new Interpreter(null,System.out,System.err,false,nameSpace);
	} //}}}

	//{{{ getVersion() method
	private static String getVersion()
	{
		try
		{
			return (String)Interpreter.class.getField("VERSION").get(null);
		}
		catch(Exception e)
		{
			return "unknown";
		}
	} //}}}

	//}}}

	//{{{ CustomClassManager class
	static class CustomClassManager extends ClassManagerImpl
	{
		private LinkedList listeners = new LinkedList();
		private ReferenceQueue refQueue = new ReferenceQueue();

		// copy and paste from bsh/classpath/ClassManagerImpl.java...
		public synchronized void addListener( Listener l )
		{
			listeners.add( new WeakReference( l, refQueue) );

			// clean up old listeners
			Reference deadref;
			while ( (deadref = refQueue.poll()) != null )
			{
				boolean ok = listeners.remove( deadref );
				if ( ok )
				{
					//System.err.println("cleaned up weak ref: "+deadref);
				}
				else
				{
					if ( Interpreter.DEBUG ) Interpreter.debug(
						"tried to remove non-existent weak ref: "+deadref);
				}
			}
		}

		public void removeListener( Listener l )
		{
			throw new Error("unimplemented");
		}

		public void reset()
		{
			classLoaderChanged();
		}

		protected synchronized void classLoaderChanged()
		{
			// clear the static caches in BshClassManager
			clearCaches();
			if (listeners != null)
			{

				for (Iterator iter = listeners.iterator();
				     iter.hasNext(); )
				{
					WeakReference wr = (WeakReference)
						iter.next();
					Listener l = (Listener)wr.get();
					if ( l == null )  // garbage collected
						iter.remove();
					else
						l.classLoaderChanged();
				}
			}
		}
	} //}}}
}
