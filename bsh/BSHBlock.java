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

class BSHBlock extends SimpleNode
{
	BSHBlock(int id) { super(id); }

	public Object eval( CallStack callstack, Interpreter interpreter) 
		throws EvalError
	{
		return eval( callstack, interpreter, false );
	}

	/**
		@param overrideNamespace if set to true *no* new BlockNamespace will
		be swapped onto the stack and the eval will happen in the current
		top namespace.  This is used by ForStatement, etc.  which must 
		intialize the block with the for-init and also for those that 
		perform multiple passes in the same block.
	*/
	public Object eval( 
		CallStack callstack, Interpreter interpreter, 
		boolean overrideNamespace ) 
		throws EvalError
	{
		Object ret = Primitive.VOID;
		int statements = jjtGetNumChildren();

		NameSpace enclosingNameSpace = null;
		if ( !overrideNamespace ) 
		{
			enclosingNameSpace= callstack.top();
			BlockNameSpace bodyNameSpace = 
				new BlockNameSpace( enclosingNameSpace );
			callstack.swap( bodyNameSpace );
		}

		for(int i=0; i<statements; i++)
		{
			SimpleNode node = ((SimpleNode)jjtGetChild(i));
			ret = node.eval( callstack, interpreter );

			// some statement or embedded block evaluated a return statement
			if (ret instanceof ReturnControl)
				break;
		}

		if ( !overrideNamespace ) 
			callstack.swap( enclosingNameSpace );

		return ret;
	}
}

