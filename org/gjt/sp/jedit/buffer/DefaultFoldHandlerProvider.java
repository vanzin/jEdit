package org.gjt.sp.jedit.buffer;

import java.util.Map;
import java.util.HashMap;

/**
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 */
public class DefaultFoldHandlerProvider implements FoldHandlerProvider
{
	private final Map<String, FoldHandler> folds = new HashMap<String, FoldHandler>();
	/**
	 * Returns the fold handler with the specified name, or null if
	 * there is no registered handler with that name.
	 *
	 * @param name The name of the desired fold handler
	 * @return the FoldHandler or null if it doesn't exists
	 * @since jEdit 4.3pre10
	 */
	public FoldHandler getFoldHandler(String name)
	{
		return folds.get(name);
	}

	/**
	 * Returns an array containing the names of all registered fold
	 * handlers.
	 *
	 * @since jEdit 4.0pre6
	 */
	public String[] getFoldModes()
	{
		return folds.keySet().toArray(new String[folds.size()]); 
	}
}
