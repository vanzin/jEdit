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

import java.lang.reflect.*;
import java.lang.reflect.InvocationHandler;
import java.io.*;

/**
	XThis is a dynamically loaded extension which extends This.java and adds 
	support for the generalized interface proxy mechanism introduced in 
	JDK1.3.  XThis allows bsh scripted objects to implement arbitrary 
	interfaces (be arbitrary event listener types).

	Note: This module relies on new features of JDK1.3 and will not compile
	with JDK1.2 or lower.  For those environments simply do not compile this
	class.

	Eventually XThis should become simply This, but for backward compatability
	we will maintain This without requiring support for the proxy mechanism.

	XThis stands for "eXtended This" (I had to call it something).
	
	@see JThis	 See also JThis with explicit JFC support for compatability.
	@see This	
*/
class XThis extends This {

	InvocationHandler invocationHandler = new Handler();

	XThis( NameSpace namespace, Interpreter declaringInterp ) { 
		super( namespace, declaringInterp ); 
	}

	public String toString() {
		return "'this' reference (XThis) to Bsh object: " + namespace.name;
	}

	String toStringShowInts( Class [] ints ) {
		StringBuffer sb = new StringBuffer( toString() + "\nimplements:" );
		for(int i=0; i<ints.length; i++)
			sb.append( " "+ ints[i].getName() + ((ints.length > 1)?",":"") );
		return sb.toString();
	}

	/**
		Get dynamic proxy for interface.
	*/
	public Object getInterface( Class clas ) {
		return Proxy.newProxyInstance(
			clas.getClassLoader(), new Class[] { clas }, invocationHandler );
	}

	/**
		Get a proxy interface for the specified XThis reference.
		This is a static utility method because the interpreter doesn't 
		currently allow access to direct methods of This objects.
		
	public static Object getInterface( XThis ths, Class interf ) { 
		return ths.getInterface( interf ); 
	}
	*/

	/**
		Inner class for the invocation handler seems to shield this unavailable
		interface from JDK1.2 VM...  
		
		I don't understand this.  JThis works just fine even if those
		classes aren't there (doesn't it?)  This class shouldn't be loaded
		if an XThis isn't instantiated in NameSpace.java, should it?
	*/
	class Handler implements InvocationHandler, java.io.Serializable {

		public Object invoke( Object proxy, Method method, Object[] args ) 
			throws EvalError 
		{
			Class [] sig = Reflect.getTypes( args );
			BshMethod bmethod = 
				namespace.getMethod( method.getName(), sig );

			if ( bmethod != null )
				return Primitive.unwrap( 
					bmethod.invokeDeclaredMethod( 
					args, declaringInterpreter, callstack, null ) );

			// Look for the default handler
			bmethod = namespace.getMethod( "invoke", 
				new Class [] { null, null } );

			// Call script "invoke( String methodName, Object [] args );
			if ( bmethod != null )
				return Primitive.unwrap( 
					bmethod.invokeDeclaredMethod( 
					new Object [] { method.getName(), args }, 
					declaringInterpreter, callstack, null ) );

			/*
				implement the required part of the Object protocol:
					public int hashCode();
					public boolean equals(java.lang.Object);
					public java.lang.String toString();
				if these were not handled by scripted methods we must provide
				a default impl.
			*/
			// a default toString() that shows the interfaces we implement
			if ( method.getName().equals("toString" ) )
				return toStringShowInts( proxy.getClass().getInterfaces());

			// a default hashCode()
			if ( method.getName().equals("hashCode" ) )
				return new Integer(this.hashCode());

			// a default equals()
			if ( method.getName().equals("equals" ) ) {
				Object obj = args[0];
				return new Boolean( proxy == obj );
			}

			throw new EvalError("Bsh script method: "+ method.getName()
				+ " not found in namespace: "+ namespace.name );
		}
	};

	/**
		For serialization.
		Note: this is copied from superclass... 
		It must be private, but we can probably add an accessor to allow
		us to call the super method explicitly.
		Just testing to see if this is causing a problem.
	*/
    private synchronized void writeObject(ObjectOutputStream s)
        throws IOException {

		// Temporarily prune the namespace.

		NameSpace parent = namespace.getParent();
		// Bind would set the interpreter, but it's possible that the parent
		// is null (it's the root).  So save it...
		Interpreter interpreter = declaringInterpreter;
		namespace.prune();
		s.defaultWriteObject();
		// put it back
		namespace.setParent( parent );
		declaringInterpreter = interpreter;
	}
}



