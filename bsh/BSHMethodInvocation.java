/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  Sun Public License Notice:                                               *
 *                                                                           *
 *  The contents of this file are subject to the Sun Public License Version  *
 *  1.0 (the "License"); you may not use this file except in compliance with *
 *  the License. A copy of the License is available at http://www.sun.com    * 
 *                                                                           *
 *  The Original Code is BeanShell. The Initial Developer of the Original    *
 *  Code is Pat Niemeyer. Portions created by Pat Niemeyer are Copyright     *
 *  (C) 2000.  All Rights Reserved.                                          *
 *                                                                           *
 *  GNU Public License Notice:                                               *
 *                                                                           *
 *  Alternatively, the contents of this file may be used under the terms of  *
 *  the GNU Lesser General Public License (the "LGPL"), in which case the    *
 *  provisions of LGPL are applicable instead of those above. If you wish to *
 *  allow use of your version of this file only under the  terms of the LGPL *
 *  and not to allow others to use your version of this file under the SPL,  *
 *  indicate your decision by deleting the provisions above and replace      *
 *  them with the notice and other provisions required by the LGPL.  If you  *
 *  do not delete the provisions above, a recipient may use your version of  *
 *  this file under either the SPL or the LGPL.                              *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Learning Java, O'Reilly & Associates                           *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/


package bsh;

class BSHMethodInvocation extends SimpleNode
{
	BSHMethodInvocation (int id) { super(id); }

	/**
		Evaluate the method invocation with the specified callstack and 
		interpreter
	*/
	public Object eval( CallStack callstack, Interpreter interpreter)
		throws EvalError
	{
		NameSpace namespace = callstack.top();
		Name name = ((BSHAmbiguousName)jjtGetChild(0)).getName(namespace);
		Object[] args = 
			((BSHArguments)jjtGetChild(1)).getArguments(callstack, interpreter);
		try {
			return name.invokeMethod(interpreter, args, callstack, this);
		} catch (ReflectError e) {
			throw new EvalError(
				"Error in method invocation: " + e.getMessage(), this);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw new TargetError(e.getTargetException(), this);
		} catch ( EvalError ee ) {
			ee.reThrow( this );
			throw new Error("should be unreachable...");
		}

	}
}

