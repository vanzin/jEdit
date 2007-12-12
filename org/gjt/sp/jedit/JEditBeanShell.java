/*
 * BeanShell.java - BeanShell scripting support
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
 * Portions Copyright (C) 2007 Matthieu Casanova
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
 * @version $Id: BeanShell.java 10803 2007-10-04 20:45:31Z kpouer $
 */
public class JEditBeanShell
{
	//{{{ static initializer
	static
	{
		// Initialize JEditBeanShell only if needed
		// It is used mostly for the standalone textarea
		init();
	} //}}}

	
	//{{{ evalSelection() method
	/**
	 * Evaluates the text selected in the specified text area.
	 * @since jEdit 2.7pre2
	 */
	public static void evalSelection(TextArea textArea)
	{
		String command = textArea.getSelectedText();
		if(command == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		Object returnValue = eval(textArea,global,command);
		if(returnValue != null)
			textArea.setSelectedText(returnValue.toString());
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
	public static Object eval(TextArea textArea, NameSpace namespace, String command)
	{
		try
		{
			return _eval(textArea,namespace,command);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,JEditBeanShell.class,e);

			handleException(textArea,null,e);
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
	public static Object _eval(TextArea textArea, NameSpace namespace, String command)
		throws Exception
	{
		Interpreter interp = createInterpreter(namespace);

		try
		{
			setupDefaultVariables(namespace,textArea);
			if(Debug.BEANSHELL_DEBUG)
				Log.log(Log.DEBUG,JEditBeanShell.class,command);
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
	public static Object runCachedBlock(BshMethod method, TextArea textArea,
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
			setupDefaultVariables(namespace,textArea);

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

	//{{{ Package-private members

	//{{{ init() method
	static void init()
	{
		classManager = new ClassManagerImpl();
		classManager.setClassLoader(new JARClassLoader());

		global = new NameSpace(classManager,
			"jEdit embedded BeanShell interpreter");
		global.importPackage("org.gjt.sp.jedit");
		global.importPackage("org.gjt.sp.jedit.buffer");
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
	private static void setupDefaultVariables(NameSpace namespace, TextArea textArea)
		throws UtilEvalError
	{
		if(textArea != null)
		{
			namespace.setVariable("buffer",textArea.getBuffer(), false);
			namespace.setVariable("textArea",textArea, false);
		}
	} //}}}

	//{{{ resetDefaultVariables() method
	private static void resetDefaultVariables(NameSpace namespace)
		throws UtilEvalError
	{
		namespace.setVariable("buffer",null, false);
		namespace.setVariable("textArea",null, false);
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
	private static void handleException(TextArea textArea, String path, Throwable t)
	{
		new BeanShellErrorDialog(null,t);
	} //}}}

	//{{{ createInterpreter() method
	private static Interpreter createInterpreter(NameSpace nameSpace)
	{
		return new Interpreter(null,System.out,System.err,false,nameSpace);
	} //}}}

	//}}}

	//{{{ CustomClassManager class
	static class CustomClassManager extends ClassManagerImpl
	{
		private LinkedList listeners = new LinkedList();
		private ReferenceQueue refQueue = new ReferenceQueue();

		// copy and paste from bsh/classpath/ClassManagerImpl.java...
		@Override
		public synchronized void addListener(Listener l)
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

		@Override
		public void removeListener( Listener l )
		{
			throw new Error("unimplemented");
		}

		@Override
		public void reset()
		{
			classLoaderChanged();
		}

		@Override
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
