/*
 * Mode.java - jEdit editing mode
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
 * Copyright (C) 1999 mike dillon
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
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.gjt.sp.jedit.indent.DeepIndentRule;
import org.gjt.sp.jedit.indent.IndentRule;
import org.gjt.sp.jedit.indent.IndentRuleFactory;
import org.gjt.sp.jedit.indent.WhitespaceRule;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * An edit mode defines specific settings for editing some type of file.
 * One instance of this class is created for each supported edit mode.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Mode
{
	//{{{ Mode constructor
	/**
	 * Creates a new edit mode.
	 *
	 * @param name The name used in mode listings and to query mode
	 * properties
	 * @see #getProperty(String)
	 */
	public Mode(String name)
	{
		this.name = name;
		this.ignoreWhitespace = true;
		props = new Hashtable<String, Object>();
	} //}}}

	//{{{ init() method
	/**
	 * Initializes the edit mode. Should be called after all properties
	 * are loaded and set.
	 */
	public void init()
	{
		try
		{
			String filenameGlob = (String)getProperty("filenameGlob");
			if(filenameGlob != null && filenameGlob.length() != 0)
			{
				filenameRE = Pattern.compile(StandardUtilities.globToRE(filenameGlob),
							     Pattern.CASE_INSENSITIVE);
			}

			String firstlineGlob = (String)getProperty("firstlineGlob");
			if(firstlineGlob != null && firstlineGlob.length() != 0)
			{
				firstlineRE = Pattern.compile(StandardUtilities.globToRE(firstlineGlob),
							      Pattern.CASE_INSENSITIVE);
			}
		}
		catch(PatternSyntaxException re)
		{
			Log.log(Log.ERROR,this,"Invalid filename/firstline"
				+ " globs in mode " + name);
			Log.log(Log.ERROR,this,re);
		}

		// Fix for this bug:
		// -- Put a mode into the user dir with the same name as one
		//    on the system dir.
		// -- Reload edit modes.
		// -- Old mode from system dir still used for highlighting
		//    until jEdit restart.
		marker = null;
	} //}}}

	//{{{ getTokenMarker() method
	/**
	 * Returns the token marker for this mode.
	 */
	public TokenMarker getTokenMarker()
	{
		loadIfNecessary();
		return marker;
	} //}}}

	//{{{ setTokenMarker() method
	/**
	 * Sets the token marker for this mode.
	 * @param marker The new token marker
	 */
	public void setTokenMarker(TokenMarker marker)
	{
		this.marker = marker;
	} //}}}

	//{{{ loadIfNecessary() method
	/**
	 * Loads the mode from disk if it hasn't been loaded already.
	 * @since jEdit 2.5pre3
	 */
	public void loadIfNecessary()
	{
		if(marker == null)
		{
			ModeProvider.instance.loadMode(this);
			if (marker == null)
				Log.log(Log.ERROR, this, "Mode not correctly loaded, token marker is still null");
		}
	} //}}}

	//{{{ getProperty() method
	/**
	 * Returns a mode property.
	 * @param key The property name
	 *
	 * @since jEdit 2.2pre1
	 */
	public Object getProperty(String key)
	{
		Object value = props.get(key);
		if(value != null)
			return value;
		return null;
	} //}}}

	//{{{ getBooleanProperty() method
	/**
	 * Returns the value of a boolean property.
	 * @param key The property name
	 *
	 * @since jEdit 2.5pre3
	 */
	public boolean getBooleanProperty(String key)
	{
		Object value = getProperty(key);
		return StandardUtilities.getBoolean(value, false);
	} //}}}

	//{{{ setProperty() method
	/**
	 * Sets a mode property.
	 * @param key The property name
	 * @param value The property value
	 */
	public void setProperty(String key, Object value)
	{
		props.put(key,value);
	} //}}}

	//{{{ unsetProperty() method
	/**
	 * Unsets a mode property.
	 * @param key The property name
	 * @since jEdit 3.2pre3
	 */
	public void unsetProperty(String key)
	{
		props.remove(key);
	} //}}}

	//{{{ setProperties() method
	/**
	 * Should only be called by <code>XModeHandler</code>.
	 * @since jEdit 4.0pre3
	 */
	public void setProperties(Map props)
	{
		if(props == null)
			props = new Hashtable<String, Object>();

		ignoreWhitespace = !"false".equalsIgnoreCase(
					(String)props.get("ignoreWhitespace"));

		// need to carry over file name and first line globs because they are
		// not given to us by the XMode handler, but instead are filled in by
		// the catalog loader.
		String filenameGlob = (String)this.props.get("filenameGlob");
		String firstlineGlob = (String)this.props.get("firstlineGlob");
		String filename = (String)this.props.get("file");
		this.props = props;
		if(filenameGlob != null)
			props.put("filenameGlob",filenameGlob);
		if(firstlineGlob != null)
			props.put("firstlineGlob",firstlineGlob);
		if(filename != null)
			props.put("file",filename);
	} //}}}

	//{{{ accept() method
	/**
	 * Returns true if the edit mode is suitable for editing the specified
	 * file. The buffer name and first line is checked against the
	 * file name and first line globs, respectively.
	 * @param fileName The buffer's name
	 * @param firstLine The first line of the buffer
	 *
	 * @since jEdit 3.2pre3
	 */
	public boolean accept(String fileName, String firstLine)
	{
		return acceptFilename(fileName) || acceptFirstLine(firstLine);
	} //}}}

	//{{{ acceptFilename() method
	/**
	 * Returns true if the buffer name matches the file name glob.
	 * @param fileName The buffer's name
	 * @return true if the file name matches the file name glob.
	 * @since jEdit 4.3pre18
	 */
	public boolean acceptFilename(String fileName)
	{
		return filenameRE != null && filenameRE.matcher(fileName).matches();
	} //}}}

	//{{{ acceptFirstLine() method
	/**
	 * Returns true if the first line matches the first line glob.
	 * @param firstLine The first line of the buffer
	 * @return true if the first line matches the first line glob.
	 * @since jEdit 4.3pre18
	 */
	public boolean acceptFirstLine(String firstLine)
	{
		return firstlineRE != null && firstlineRE.matcher(firstLine).matches();
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the internal name of this edit mode.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ toString() method
	/**
	 * Returns a string representation of this edit mode.
	 */
	public String toString()
	{
		return name;
	} //}}}

	//{{{ getIgnoreWhitespace() method
	public boolean getIgnoreWhitespace()
	{
		return ignoreWhitespace;
	} //}}}

	//{{{ Indent rules

	public synchronized List<IndentRule> getIndentRules()
	{
		if (indentRules == null)
		{
			initIndentRules();
		}
		return indentRules;
	}

	public synchronized boolean isElectricKey(char ch)
	{
		if (electricKeys == null)
		{
			String[] props = {
				"indentOpenBrackets",
				"indentCloseBrackets",
				"electricKeys"
			};

			StringBuilder buf = new StringBuilder();
			for(int i = 0; i < props.length; i++)
			{
				String prop = (String) getProperty(props[i]);
				if (prop != null)
					buf.append(prop);
			}

			electricKeys = buf.toString();
		}

		return (electricKeys.indexOf(ch) >= 0);
	}

	private void initIndentRules()
	{
		List<IndentRule> rules = new LinkedList<IndentRule>();

		String[] regexpProps = {
			"indentNextLine",
			"indentNextLines"
		};

		for(int i = 0; i < regexpProps.length; i++)
		{
			IndentRule rule = createRegexpIndentRule(regexpProps[i]);
			if(rule != null)
				rules.add(rule);
		}

		String[] bracketProps = {
			"indentOpenBracket",
			"indentCloseBracket",
			"unalignedOpenBracket",
			"unalignedCloseBracket",
		};

		for(int i = 0; i < bracketProps.length; i++)
		{
			createBracketIndentRules(bracketProps[i], rules);
		}

		String[] finalProps = {
			"unindentThisLine",
			"unindentNextLines"
		};

		for(int i = 0; i < finalProps.length; i++)
		{
			IndentRule rule = createRegexpIndentRule(finalProps[i]);
			if(rule != null)
				rules.add(rule);
		}

		if (getBooleanProperty("deepIndent"))
		{
			String unalignedOpenBrackets = (String) getProperty("unalignedOpenBrackets");
			if (unalignedOpenBrackets != null)
			{
				for (int i = 0 ; i < unalignedOpenBrackets.length();i++)
				{
					char openChar = unalignedOpenBrackets.charAt(i);
					char closeChar = TextUtilities.getComplementaryBracket(openChar, null);
					if (closeChar != '\0')
						rules.add(new DeepIndentRule(openChar, closeChar));
				}
			}
		}

		if (!getIgnoreWhitespace())
			rules.add(new WhitespaceRule());

		indentRules = Collections.unmodifiableList(rules);
	}

	private IndentRule createRegexpIndentRule(String prop)
	{
		String value = (String) getProperty(prop);

		try
		{
			if(value != null)
			{
				Method m = IndentRuleFactory.class.getMethod(
					prop,new Class[] { String.class });
				return (IndentRule)m.invoke(null, value);
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Bad indent rule " + prop
				+ '=' + value + ':');
			Log.log(Log.ERROR,this,e);
		}

		return null;
	}

	private void createBracketIndentRules(String prop,
						List<IndentRule> rules)
	{
		String value = (String) getProperty(prop + 's');

		try
		{
			if(value != null)
			{
				for(int i = 0; i < value.length(); i++)
				{
					char ch = value.charAt(i);

					Method m = IndentRuleFactory.class.getMethod(
						prop,new Class[] { char.class });
					rules.add((IndentRule) m.invoke(null, ch));
				}
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Bad indent rule " + prop
				+ '=' + value + ':');
			Log.log(Log.ERROR,this,e);
		}
	}

	//}}}

	//{{{ Private members
	protected String name;
	protected Map<String, Object> props;
	private Pattern firstlineRE;
	private Pattern filenameRE;
	protected TokenMarker marker;
	private List<IndentRule> indentRules;
	private String electricKeys;
	private boolean ignoreWhitespace;
	//}}}
}
