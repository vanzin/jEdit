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

import java.util.Vector;
import java.io.*;

/**
	The BeanShell script interpreter.

	An instance of Interpreter can be used to source scripts and evaluate 
	statements or expressions.  
	<p>
	Here are some examples:

	<p><blockquote><pre>
		Interpeter bsh = new Interpreter();

		// Evaluate statements and expressions
		bsh.eval("foo=Math.sin(0.5)");
		bsh.eval("bar=foo*5; bar=Math.cos(bar);");
		bsh.eval("for(i=0; i<10; i++) { print(\"hello\"); }");
		// same as above using java syntax and apis only
		bsh.eval("for(int i=0; i<10; i++) { System.out.println(\"hello\"); }");

		// Source from files or streams
		bsh.source("myscript.bsh");  // or bsh.eval("source(\"myscript.bsh\")");

		// Use set() and get() to pass objects in and out of variables
		bsh.set( "date", new Date() );
		Date date = (Date)bsh.get( "date" );
		// This would also work:
		Date date = (Date)bsh.eval( "date" );

		bsh.eval("year = date.getYear()");
		Integer year = (Integer)bsh.get("year");  // primitives use wrappers

		// With Java1.3+ scripts can implement arbitrary interfaces...
		// Script an awt event handler (or source it from a file, more likely)
		bsh.eval( "actionPerformed( e ) { print( e ); }");
		// Get a reference to the script object (implementing the interface)
		ActionListener scriptedHandler = 
			(ActionListener)bsh.eval("return (ActionListener)this");
		// Use the scripted event handler normally...
		new JButton.addActionListener( script );
	</pre></blockquote>
	<p>

	In the above examples we showed a single interpreter instance, however 
	you may wish to use many instances, depending on the application and how
	you structure your scripts.  Interpreter instances are very light weight
	to create, however if you are going to execute the same script repeatedly
	and require maximum performance you should consider scripting the code as 
	a method and invoking the scripted method each time on the same interpreter
	instance (using eval()). 
	<p>

	See the BeanShell User's Manual for more information.
*/
public class Interpreter 
	implements Runnable, ConsoleInterface /*,Serializable*/ 
{
	/* --- Begin static stuff --- */

	public static final String VERSION = "1.2b1";
	/* 
		Debug utils are static so that they are reachable by code that doesn't
		necessarily have an interpreter reference (e.g. tracing in utils).
		In the future we may want to allow debug/trace to be turned on on
		a per interpreter basis, in which case we'll need to use the parent 
		reference in some way to determine the scope of the command that 
		turns it on or off...
	*/
    public static boolean DEBUG, TRACE;

	// This should be per instance
    static PrintStream debug;
	static { 
		staticInit();
	}

	/** Shared system object visible under bsh.system */
	static This systemObject;

	/** Strict Java mode */
	public static boolean strictJava = false;

	/* --- end static stuff --- */

	/* --- Instance data --- */

	Parser parser;
    NameSpace globalNameSpace;
    Reader in;
    PrintStream out;
    PrintStream err;
    ConsoleInterface console; 

	/** If this interpeter is a child of another, the parent */
	Interpreter parent;

	/** The name of the file or other source that this interpreter is reading */
	String sourceFileInfo;

	/** 
		Do we override exit on EOF as normally done in iteractive mode?
		(This is used by Sessiond)
	*/
	public boolean noExitOnEOF;

    private boolean 
		evalOnly, 		// Interpreter has no input stream, use eval() only
		interactive;	// Interpreter has a user, print prompts, etc.

	/* --- End instance data --- */

	/**
		The main constructor.
		All constructors should now pass through here.

		@param namespace If namespace is non-null then this interpreter's 
		root namespace will be set to the one provided.  If it is null a new 
		one will be created for it.
		@param parent The parent interpreter if this interpreter is a child 
			of another.  May be null.
		@param sourceFileInfo An informative string holding the filename 
		or other description of the source from which this interpreter is
		reading... used for debugging.  May be null.
	*/
    public Interpreter(
		Reader in, PrintStream out, PrintStream err, 
		boolean interactive, NameSpace namespace,
		Interpreter parent, String sourceFileInfo )
    {
		parser = new Parser( in );
		long t1=System.currentTimeMillis();
        this.in = in;
        this.out = out;
        this.err = err;
        this.interactive = interactive;
		debug = err;
		this.parent = parent;
		this.sourceFileInfo = sourceFileInfo;

		if ( namespace == null )
        	this.globalNameSpace = new NameSpace("global");
		else
			this.globalNameSpace = namespace;

		// The classes which are imported by default
		globalNameSpace.loadDefaultImports();

		/* 
			Create the root "bsh" system object if it doesn't exist.
		*/
		if ( ! ( getu("bsh") instanceof bsh.This ) )
			initRootSystemObject();

		if ( interactive )
			loadRCFiles();

		long t2=System.currentTimeMillis();
		Interpreter.debug("Time to initialize interpreter: "+(t2-t1));
    }

    public Interpreter(
		Reader in, PrintStream out, PrintStream err, 
		boolean interactive, NameSpace namespace)
    {
		this( in, out, err, interactive, namespace, null, null );
	}

    public Interpreter(
		Reader in, PrintStream out, PrintStream err, boolean interactive)
    {
        this(in, out, err, interactive, null);
    }

	/**
		Construct a new interactive interpreter attached to the specified 
		console using the specified parent namespace.
	*/
    public Interpreter(ConsoleInterface console, NameSpace globalNameSpace) {

        this( console.getIn(), console.getOut(), console.getErr(), 
			true, globalNameSpace );

		setConsole( console );
    }

	/**
		Construct a new interactive interpreter attached to the specified 
		console.
	*/
    public Interpreter(ConsoleInterface console) {
        this(console, null);
    }

	/**
		Create an interpreter for evaluation only.
	*/
    public Interpreter()
    {
		this( new StringReader(""), 
			System.out, System.err, false, null );
        evalOnly = true;
		setu( "bsh.evalOnly", new Primitive(true) );
    }

	// End constructors

	/**
		Attach the console thusly... ;)
	*/
	public void setConsole( ConsoleInterface console ) {
		this.console = console;
		setu( "bsh.console", console );
	}

	private void initRootSystemObject() 
	{
		// bsh
		setu("bsh", new NameSpace( "Bsh Object" ).getThis( this ) );

		// init the static shared systemObject if it's not there yet
		if ( systemObject == null )
			systemObject = new NameSpace( 
				"Bsh System Object" ).getThis( this );
		// bsh.system
		setu( "bsh.system", systemObject );

		// bsh.help
		This helpText = new NameSpace( 
			"Bsh Command Help Text" ).getThis( this );
		setu( "bsh.help", helpText );

		// bsh.cwd
		try {
			setu( "bsh.cwd", System.getProperty("user.dir") );
		} catch ( SecurityException e ) { 
			// applets can't see sys props
			setu( "bsh.cwd", "." );
		}

		// bsh.interactive
		setu( "bsh.interactive", new Primitive(interactive) );
		// bsh.evalOnly
		setu( "bsh.evalOnly", new Primitive(evalOnly) );
	}

	/**
		Set the global namespace for this interpreter.
		<p>

		Note: This is here for completeness.  If you're using this a lot 
		it may be an indication that you are doing more work than you have 
		to.  For example, caching the interpreter instance rather than the 
		namespace should not add a significant overhead.  No state other 
		than the debug status is stored in the interpreter.
		<p>

		All features of the namespace can also be accessed using the 
		interpreter via eval() and the script variable 'this.namespace'
		(or global.namespace as necessary).
	*/
	public void setNameSpace( NameSpace globalNameSpace ) {
		this.globalNameSpace = globalNameSpace;
	}

	/**
		Get the global namespace of this interpreter.
		<p>

		Note: This is here for completeness.  If you're using this a lot 
		it may be an indication that you are doing more work than you have 
		to.  For example, caching the interpreter instance rather than the 
		namespace should not add a significant overhead.  No state other than 
		the debug status is stored in the interpreter.  
		<p>

		All features of the namespace can also be accessed using the 
		interpreter via eval() and the script variable 'this.namespace'
		(or global.namespace as necessary).
	*/
	public NameSpace getNameSpace() {
		return globalNameSpace;
	}

	/**
		Run the text only interpreter on the command line or specify a file.
	*/
    public static void main( String [] args ) 
	{
        if ( args.length > 0 ) {
			String filename = args[0];

			String [] bshArgs;
			if ( args.length > 1 ) {
				bshArgs = new String [ args.length -1 ];
				System.arraycopy( args, 1, bshArgs, 0, args.length-1 );
			} else
				bshArgs = new String [0];

            Interpreter interpreter = new Interpreter();
			interpreter.setu( "bsh.args", bshArgs );
			try {
				interpreter.source( filename, interpreter.globalNameSpace );
			} catch ( FileNotFoundException e ) {
				System.out.println("File not found: "+e);
			} catch ( EvalError e ) {
				System.out.println("Evaluation Error: "+e);
			} catch ( IOException e ) {
				System.out.println("I/O Error: "+e);
			}
        } else {
			// Workaround for JDK bug 4071281, where system.in.available() 
			// returns too large a value. This bug has been fixed in JDK 1.2.
			InputStream src;
			if ( System.getProperty("os.name").startsWith("Windows") 
				&& System.getProperty("java.version").startsWith("1.1."))
			{
				src = new FilterInputStream(System.in) {
					public int available() throws IOException {
						return 0;
					}
				};
			}
			else
				src = System.in;

            Reader in = new CommandLineReader( new InputStreamReader(src));
            Interpreter interpreter = 
				new Interpreter( in, System.out, System.err, true );
        	interpreter.run();
        }
    }

	/**
		Run interactively.  (printing prompts, etc.)
	*/
    public void run() {
        if(evalOnly)
            throw new RuntimeException("bsh Interpreter: No stream");

        /*
          We'll print our banner using eval(String) in order to
          exercise the parser and get the basic expression classes loaded...
          This ameliorates the delay after typing the first statement.
        */
        if ( interactive )
			try { 
				eval("printBanner();"); 
			} catch ( EvalError e ) {
				println(
					"BeanShell "+VERSION+" - by Pat Niemeyer (pat@pat.net)");
			}

        boolean eof = false;

		// init the callstack.  
		CallStack callstack = new CallStack();
		callstack.push( globalNameSpace );

        while(!eof)
        {
            try
            {
                // try to sync up the console
                System.out.flush();
                System.err.flush();
                Thread.yield();  // this helps a little
                if(interactive)
                    print("bsh % ");

                eof = Line();

                if(get_jjtree().nodeArity() > 0)  // number of child nodes 
                {
                    SimpleNode node = (SimpleNode)(get_jjtree().rootNode());

                    if(DEBUG)
                        node.dump(">");

                    Object ret = node.eval( callstack, this );
				
					// sanity check during development
					if ( callstack.depth() > 1 )
						throw new InterpreterError(
							"Callstack growing: "+callstack);

                    if(ret instanceof ReturnControl)
                        ret = ((ReturnControl)ret).value;
                    if(ret != Primitive.VOID)
                    {
                        setVariable("$_", ret);
                        Object show = getu("bsh.show");
                        if(show instanceof Boolean &&
                            ((Boolean)show).booleanValue() == true)
                            println("<" + ret + ">");
                    }
                }
            }
            catch(ParseException e)
            {
                error("Parser Error: " + e.getMessage(DEBUG));
                if(DEBUG)
                    e.printStackTrace();
                if(!interactive)
                    eof = true;

                parser.reInitInput(in);
            }
            catch(InterpreterError e)
            {
                error("Internal Error: " + e.getMessage());
                e.printStackTrace();
                if(!interactive)
                    eof = true;
            }
            catch(TargetError e)
            {
                error("// Uncaught Exception: " + e );
                if(DEBUG)
                    e.printStackTrace();
                if(!interactive)
                    eof = true;
            }
            catch (EvalError e)
            {
				if ( interactive )
					error( e.toString() );
				else
					error( e.getMessage() );
                if(DEBUG)
                    e.printStackTrace();
                if(!interactive)
                    eof = true;
            }
            catch(Exception e)
            {
                error("Unknown error: " + e);
                e.printStackTrace();
                if(!interactive)
                    eof = true;
            }
            catch(TokenMgrError e)
            {
				error("Error parsing input: " + e);

				/*
					We get stuck in infinite loops here when unicode escapes
					fail.  Must re-init the char stream reader 
					(ASCII_UCodeESC_CharStream.java)
				*/
				parser.reInitTokenInput( in );

                if(!interactive)
                    eof = true;
            }
            finally
            {
                get_jjtree().reset();
				// reinit the callstack
				if ( callstack.depth() > 1 ) {
					callstack.clear();
					callstack.push( globalNameSpace );
				}
            }
        }

		if ( interactive && !noExitOnEOF ) 
			System.exit(0);
    }

	// begin source and eval

	/**
		Read text from fileName and eval it.
	*/
    public Object source( String filename, NameSpace nameSpace ) 
		throws FileNotFoundException, IOException, EvalError 
	{
		File file = pathToFile( filename );
		debug("Sourcing file: "+file);
		Reader in = new BufferedReader( new FileReader(file) );
		return eval( in, nameSpace, filename );
	}

	/**
		Read text from fileName and eval it.
		Convenience method.  Use the global namespace.
	*/
    public Object source( String filename ) 
		throws FileNotFoundException, IOException, EvalError 
	{
		return source( filename, globalNameSpace );
	}

    /**
        Spawn a non-interactive local interpreter to evaluate text in the 
		specified namespace.  

		Return value is the evaluated object (or corresponding primitive 
		wrapper).

		@param sourceFileInfo is for information purposes only.  It is used to
		display error messages (and in the future may be made available to
		the script).
		@throws EvalError on script problems
		@throws TargetError on unhandled exceptions from the script
    */
	/*
		Note: we need a form of eval that passes the callstack through...
	*/
	/*
	Can't this be combined with run() ?
	run seems to have stuff in it for interactive vs. non-interactive...
	compare them side by side and see what they do differently, aside from the
	exception handling.
	*/

    public Object eval( 
		Reader in, NameSpace nameSpace, String sourceFileInfo ) 
		throws EvalError 
	{
		Object retVal = null;
		debug("eval: nameSpace = "+nameSpace);

		/* 
			Create non-interactive local interpreter for this namespace
			with source from the input stream and out/err same as 
			this interpreter.
		*/
        Interpreter localInterpreter = 
			new Interpreter( 
				in, out, err, false, nameSpace, this, sourceFileInfo  );

		CallStack callstack = new CallStack();
		callstack.push( nameSpace );

        boolean eof = false;
        while(!eof)
        {
			SimpleNode node = null;
            try
            {
                eof = localInterpreter.Line();
                if (localInterpreter.get_jjtree().nodeArity() > 0)
                {
                    node = (SimpleNode)localInterpreter.get_jjtree().rootNode();
					// nodes remember from where they were sourced
					node.setSourceFile( sourceFileInfo );

					if ( TRACE )
						println( "// " +node.getText() );

                    retVal = node.eval( callstack, localInterpreter );

					// sanity check during development
					if ( callstack.depth() > 1 )
						throw new InterpreterError(
							"Callstack growing: "+callstack);

                    if ( retVal instanceof ReturnControl ) {
                        retVal = ((ReturnControl)retVal).value;
						break; // non-interactive, return control now
					}
                }
            } catch(ParseException e) {
                throw new EvalError(
					"Sourced file: "+sourceFileInfo+" parser Error: " 
					+ e.getMessage( DEBUG ), node );
            } catch(InterpreterError e) {
                e.printStackTrace();
                throw new EvalError(
					"Sourced file: "+sourceFileInfo+" internal Error: " 
					+ e.getMessage(), node);
            } catch( TargetError e ) {
                if(DEBUG)
                    e.printStackTrace();
				// failsafe, set the Line as the origin of the error.
				if ( e.getNode()==null )
					e.setNode( node );
				e.reThrow("Sourced file: "+sourceFileInfo);
            } catch(EvalError e) {
                if(DEBUG)
                    e.printStackTrace();
				// failsafe, set the Line as the origin of the error.
				if ( e.getNode()==null )
					e.setNode( node );
				e.reThrow( "Sourced file: "+sourceFileInfo );
            } catch(Exception e) {
                e.printStackTrace();
                throw new EvalError(
					"Sourced file: "+sourceFileInfo+" unknown error: " 
					+ e.getMessage(), node);
            } catch(TokenMgrError e) {
                throw new EvalError(
					"Sourced file: "+sourceFileInfo+" Token Parsing Error: " 
					+ e.getMessage(), node );
            } finally {
                localInterpreter.get_jjtree().reset();

				// reinit the callstack
				if ( callstack.depth() > 1 ) {
					callstack.clear();
					callstack.push( nameSpace );
				}
            }
        }
		return Primitive.unwrap( retVal );
    }

	/**
		Evaluate the inputstream in this interpreter's global namespace.
	*/
    public Object eval( Reader in ) throws EvalError 
	{
		return eval( in, globalNameSpace, "eval stream" );
	}

	/**
		Evaluate the string in this interpreter's global namespace.
	*/
    public Object eval( String statement ) throws EvalError {
		return eval(statement, globalNameSpace);
	}

	/**
		Evaluate the string in the specified namespace.
	*/
    public Object eval( String statement, NameSpace nameSpace ) 
		throws EvalError {

		String s = ( statement.endsWith(";") ? statement : statement+";" );
        return eval( 
			new StringReader(s), nameSpace, "<Inline eval of: "+s+" >" );
    }

	// end source and eval

	/**
		Print an error message in a standard format on the output stream
		associated with this interpreter. On the GUI console this will appear 
		in red, etc.
	*/
    public final void error(String s) {
		if ( console != null )
				console.error( "// Error: " + s +"\n" );
		else {
			err.println("// Error: " + s);
			err.flush();
		}
    }

	// ConsoleInterface
	// The interpreter reflexively implements the console interface that it 
	// uses.  Should clean this up by using an inner class to implement the
	// console for us.

	/** 
		Get the input stream associated with this interpreter.
		This may be be stdin or the GUI console.
	*/
	public Reader getIn() { return in; }

	/** 
		Get the outptut stream associated with this interpreter.
		This may be be stdout or the GUI console.
	*/
	public PrintStream getOut() { return out; }

	/** 
		Get the error output stream associated with this interpreter.
		This may be be stderr or the GUI console.
	*/
	public PrintStream getErr() { return err; }

    public final void println(String s)
    {
        print(s + "\n");
    }

    public final void print(String s)
    {
		if (console != null) {
            console.print(s);
        } else {
            out.print(s);
            out.flush();
        }
    }

	// End ConsoleInterface

	/**
		Print a debug message on debug stream associated with this interpreter
		only if debugging is turned on.
	*/
    public final static void debug(String s)
    {
        if(DEBUG)
            debug.println("// Debug: " + s);
    }

	/* 
		Primary interpreter set and get variable methods
		Note: These are squeltching errors... should they?
	*/

	/**
		Get the value of the name.
		name may be any value. e.g. a variable or field
	*/
    public Object get( String name ) throws EvalError {
		Object ret = globalNameSpace.get( name, this );
		return Primitive.unwrap( ret );
	}

	/**
		Unchecked get for internal use
	*/
    Object getu( String name ) {
		try { 
			return get( name );
		} catch ( EvalError e ) { 
			throw new InterpreterError("set: "+e);
		}
	}

	/**
		Assign the value to the name.	
		name may evaluate to anything assignable. e.g. a variable or field.
	*/
    public void set(String name, Object value) 
		throws EvalError 
	{
		// map null to Primtive.NULL coming in...
		if ( value == null )
			value = Primitive.NULL;

		CallStack callstack = new CallStack();
		LHS lhs = globalNameSpace.getNameResolver( name ).toLHS( 
			callstack, this );
		lhs.assign( value );
	}

	/**
		Unchecked set for internal use
	*/
    void setu(String name, Object value) {
		try { 
			set(name, value);
		} catch ( EvalError e ) { 
			throw new InterpreterError("set: "+e);
		}
	}

    public void set(String name, long value) throws EvalError {
        set(name, new Primitive(value));
	}
    public void set(String name, int value) throws EvalError {
        set(name, new Primitive(value));
	}
    public void set(String name, double value) throws EvalError {
        set(name, new Primitive(value));
	}
    public void set(String name, float value) throws EvalError {
        set(name, new Primitive(value));
	}
    public void set(String name, boolean value) throws EvalError {
        set(name, new Primitive(value));
	}

	/**
		Unassign the variable name.	
		Name should evaluate to a variable.
	*/
    public void unset( String name ) 
		throws EvalError 
	{
		CallStack callstack = new CallStack();
		LHS lhs = globalNameSpace.getNameResolver( name ).toLHS( 
			callstack, this );

		if ( lhs.type != LHS.VARIABLE )
			throw new EvalError("Can't unset, not a variable: "+name);

		// null means remove it
		lhs.assign( null );
	}


	/**
		@deprecated does not properly evaluate compound names
	*/
    public Object getVariable(String name)
    {
        Object obj = globalNameSpace.getVariable(name);
		return Primitive.unwrap( obj );
    }

	/**
		@deprecated does not properly evaluate compound names
	*/
    public void setVariable(String name, Object value)
    {
        try { globalNameSpace.setVariable(name, value); }
        catch(EvalError e) { error(e.toString()); }
    }

	/**
		@deprecated does not properly evaluate compound names
	*/
    public void setVariable(String name, int value)
    {
        try { globalNameSpace.setVariable(name, new Primitive(value)); }
        catch(EvalError e) { error(e.toString()); }
    }

	/**
		@deprecated does not properly evaluate compound names
	*/
    public void setVariable(String name, float value)
    {
        try { globalNameSpace.setVariable(name, new Primitive(value)); }
        catch(EvalError e) { error(e.toString()); }
    }

	/**
		@deprecated does not properly evaluate compound names
	*/
    public void setVariable(String name, boolean value)
    {
        try { globalNameSpace.setVariable(name, new Primitive(value)); }
        catch(EvalError e) { error(e.toString()); }
    }

	// end primary set and get methods

	/*	Methods for interacting with Parser */

	private JJTParserState get_jjtree() {
		return parser.jjtree;
	}

  	private ASCII_UCodeESC_CharStream get_jj_input_stream() {
		return parser.jj_input_stream;
	}

  	private boolean Line() throws ParseException {
		return parser.Line();
	}

	/*	End methods for interacting with Parser */

	void loadRCFiles() {
		try {
			String rcfile = 
				// Default is c:\windows under win98, $HOME under Unix
				System.getProperty("user.home") + File.separator + ".bshrc";
			source( rcfile, globalNameSpace );
		} catch ( Exception e ) { 
			// squeltch security exception, filenotfoundexception
			debug("Could not find rc file: "+e);
		}
	}

	/**
		Localize a path to the file name based on the bsh.cwd interpreter 
		working directory.
	*/
    public File pathToFile( String fileName ) 
		throws IOException
	{
		File file = new File( fileName );

		// if relative, fix up to bsh.cwd
		if ( !file.isAbsolute() ) {
			String cwd = (String)getu("bsh.cwd");
			file = new File( cwd + File.separator + fileName );
		}

		return new File( file.getCanonicalPath() );
	}

	public static void redirectOutputToFile( String filename ) 
	{
		try {
			PrintStream pout = new PrintStream( 
				new FileOutputStream( filename ) );
			System.setOut( pout );
			System.setErr( pout );
		} catch ( IOException e ) {
			System.err.println("Can't redirect output to file: "+filename );
		}
	}

	/**
		Set an external class loader to be used for all basic class loading
		in BeanShell.  
		<p>

		BeanShell will use this at the same point it would otherwise use the 
		plain Class.forName().
		i.e. if no explicit classpath management is done from the script
		(addClassPath(), setClassPath(), reloadClasses()) then BeanShell will
		only use the supplied classloader.  If additional classpath management
		is done then BeanShell will perform that in addition to the supplied
		external classloader.  
		However BeanShell is not currently able to reload
		classes supplied through the external classloader.
		<p>

		@see BshClassManager.setClassLoader()
	*/
	public void setClassLoader( ClassLoader externalCL ) {
		BshClassManager.setClassLoader( externalCL );
	}

	static void staticInit() {
	/* 
		Apparently in some environments you can't catch the security exception
		at all...  e.g. as an applet in IE  ... will probably have to work 
		around 
	*/
		try {
    		debug = System.err;
    		DEBUG = Boolean.getBoolean("debug");
    		TRACE = Boolean.getBoolean("trace");
			String outfilename = System.getProperty("outfile");
			if ( outfilename != null )
				redirectOutputToFile( outfilename );
		} catch ( SecurityException e ) { 
			System.err.println("Could not init static:"+e);
		} catch ( Exception e ) {
			System.err.println("Could not init static(2):"+e);
		} catch ( Throwable e ) { 
			System.err.println("Could not init static(3):"+e);
		}
	}

	/**
		Specify the source of the text from which this interpreter is reading.
		Note: there is a difference between what file the interrpeter is 
		sourcing and from what file a method was originally parsed.  One
		file may call a method sourced from another file.  See SimpleNode
		for origination file info.
		@see SimpleNode.getSourceFile 
	*/
	public String getSourceFileInfo() { 
		if ( sourceFileInfo != null )
			return sourceFileInfo;
		else
			return "<unknown source>";
	}

	public Interpreter getParent() {
		return parent;
	}

}

