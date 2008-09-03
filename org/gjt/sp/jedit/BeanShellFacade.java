/*
 * BeanShellFacade.java - A BeanShell facade
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Matthieu Casanova
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
import java.lang.reflect.InvocationTargetException;
import org.gjt.sp.jedit.bsh.BshClassManager;
import org.gjt.sp.jedit.bsh.BshMethod;
import org.gjt.sp.jedit.bsh.CallStack;
import org.gjt.sp.jedit.bsh.Interpreter;
import org.gjt.sp.jedit.bsh.NameSpace;
import org.gjt.sp.jedit.bsh.Primitive;
import org.gjt.sp.jedit.bsh.TargetError;
import org.gjt.sp.jedit.bsh.UtilEvalError;
import org.gjt.sp.jedit.bsh.classpath.ClassManagerImpl;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;
//}}}

/**
 * This class will be the interface for beanshell interaction.
 * In jEdit it will be used with the static methods of {@link BeanShell}
 * @author Matthieu Casanova
 * @since jEdit 4.3pre13
 */
public abstract class BeanShellFacade<T>
{
	//{{{ BeanShellFacade constructor
	protected BeanShellFacade()
	{
		classManager = new ClassManagerImpl();
		global = new NameSpace(classManager,
			"jEdit embedded BeanShell interpreter");

		interpForMethods = createInterpreter(global);
		init();
	} //}}}

	//{{{ init() method
	/**
	 * Initialize things. It is called by the constructor.
	 * You can override it to import other packages
	 */
	protected void init()
	{
		global.importPackage("org.gjt.sp.jedit");
		global.importPackage("org.gjt.sp.jedit.buffer");
		global.importPackage("org.gjt.sp.jedit.syntax");
		global.importPackage("org.gjt.sp.jedit.textarea");
		global.importPackage("org.gjt.sp.util");
	} //}}}

	//{{{ evalSelection() method
	/**
	 * Evaluates the text selected in the specified text area.
	 */
	public void evalSelection(T param, TextArea textArea)
	{
		String command = textArea.getSelectedText();
		if(command == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		Object returnValue = eval(param,global,command);
		if(returnValue != null)
			textArea.setSelectedText(returnValue.toString());
	} //}}}

	//{{{ eval() method
	/**
	 * Evaluates the specified BeanShell expression with the global namespace
	 * @param param The parameter
	 * @param command The expression
	 */
	public Object eval(T param, String command)
	{
		return eval(param, global, command);
	} //}}}

	//{{{ eval() method
	/**
	 * Evaluates the specified BeanShell expression. Errors are reported in
	 * a dialog box.
	 * @param param The parameter
	 * @param namespace The namespace
	 * @param command The expression
	 */
	public Object eval(T param, NameSpace namespace, String command)
	{
		try
		{
			return _eval(param,namespace,command);
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,BeanShellFacade.class,e);

			handleException(param,null,e);
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
	 */
	public Object _eval(T view, NameSpace namespace, String command)
		throws Exception
	{
		Interpreter interp = createInterpreter(namespace);

		try
		{
			setupDefaultVariables(namespace,view);
			if(Debug.BEANSHELL_DEBUG)
				Log.log(Log.DEBUG,BeanShellFacade.class,command);
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
	 * @param id An identifier.
	 * @param code The code
	 * @param namespace If true, the namespace will be set
	 * @exception Exception instances are thrown when various BeanShell errors
	 * occur
	 */
	public BshMethod cacheBlock(String id, String code, boolean namespace)
		throws Exception
	{
		// Make local namespace so that the method could be GCed
		// if it becomes unnecessary.
		NameSpace local = new NameSpace(global, "__internal_" + id);
		// This name should be unique enough not to shadow any outer
		// identifier.
		String name = "__runCachedMethod";
		if(namespace)
		{
			_eval(null,local,name + "(ns) {\nthis.callstack.set(0,ns);\n" + code + "\n}");
			return local.getMethod(name,new Class[] { NameSpace.class });
		}
		else
		{
			_eval(null,local,name + "() {\n" + code + "\n}");
			return local.getMethod(name,new Class[0]);
		}
	} //}}}

	//{{{ runCachedBlock() method
	/**
	 * Runs a cached block of code in the specified namespace. Faster than
	 * evaluating the block each time.
	 * @param method The method instance returned by cacheBlock()
	 * @param namespace The namespace to run the code in
	 * @exception Exception instances are thrown when various BeanShell
	 * errors occur
	 */
	public Object runCachedBlock(BshMethod method, T param,
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
			setupDefaultVariables(namespace,param);

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

	//{{{ getNameSpace() method
	/**
	 * Returns the global namespace.
	 */
	public NameSpace getNameSpace()
	{
		return global;
	} //}}}

	//{{{ resetClassManager() method
	/**
	 * Causes BeanShell internal structures to drop references to cached
	 * Class instances.
	 */
	void resetClassManager()
	{
		classManager.reset();
	} //}}}

	//{{{ setupDefaultVariables() method
	protected abstract void setupDefaultVariables(NameSpace namespace, T param)
		throws UtilEvalError;
	//}}}

	//{{{ resetDefaultVariables() method
	protected abstract void resetDefaultVariables(NameSpace namespace)
		throws UtilEvalError;
	//}}}

	//{{{ handleException() method
	protected abstract void handleException(T param, String path, Throwable t);
	//}}}

	//{{{ createInterpreter() method
	protected static Interpreter createInterpreter(NameSpace nameSpace)
	{
		return new Interpreter(null,System.out,System.err,false,nameSpace);
	} //}}}

	//{{{ unwrapException() method
	/**
	 * This extracts an exception from a 'wrapping' exception, as BeanShell
	 * sometimes throws. This gives the user a more accurate error traceback
	 */
	protected static void unwrapException(Exception e) throws Exception
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

	//{{{ Static variables
	protected NameSpace global;
	protected BshClassManager classManager;
	private static Interpreter interpForMethods;
	private static final Object[] NO_ARGS = new Object[0];
	//}}}
}
