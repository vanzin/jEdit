/*
 * Abbrevs.java - Abbreviation manager
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.gui.AddAbbrevDialog;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * Abbreviation manager.
 * @author Slava Pestov
 * @version $Id$
 */
public class Abbrevs
{
	/**
	 * Returns if abbreviations should be expanded after the
	 * user finishes typing a word.
	 */
	public static boolean getExpandOnInput()
	{
		return expandOnInput;
	}

	/**
	 * Sets if abbreviations should be expanded after the
	 * user finishes typing a word.
	 * @param true If true, typing a non-alphanumeric characater will
	 * automatically attempt to expand the current abbrev
	 */
	public static void setExpandOnInput(boolean expandOnInput)
	{
		Abbrevs.expandOnInput = expandOnInput;
	}

	/**
	 * Expands the abbrev at the caret position in the specified
	 * view.
	 * @param view The view
	 * @param add If true and abbrev not found, will ask user if
	 * it should be added
	 * @since jEdit 2.6pre4
	 */
	public static boolean expandAbbrev(View view, boolean add)
	{
		JEditTextArea textArea = view.getTextArea();
		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return false;
		}

		Buffer buffer = view.getBuffer();

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = textArea.getCaretPosition();

		String lineText = textArea.getLineText(line);
		if(lineText.length() == 0)
		{
			if(add)
				view.getToolkit().beep();
			return false;
		}

		int pos = caret - lineStart;
		if(pos == 0)
		{
			if(add)
				view.getToolkit().beep();
			return false;
		}

		// we reuse the 'pp' vector to save time
		pp.removeAllElements();

		int wordStart;
		String abbrev;

		// handle abbrevs of the form abbrev#pos1#pos2#pos3#...
		if(lineText.charAt(pos-1) == '#')
		{
			wordStart = lineText.indexOf('#');
			wordStart = TextUtilities.findWordStart(lineText,wordStart,
				(String)buffer.getProperty("noWordSep") + '#');

			abbrev = lineText.substring(wordStart,pos - 1);

			// positional parameters will be inserted where $1, $2, $3, ...
			// occurs in the expansion

			int lastIndex = 0;
			for(int i = 0; i < abbrev.length(); i++)
			{
				if(abbrev.charAt(i) == '#')
				{
					pp.addElement(abbrev.substring(lastIndex,i));
					lastIndex = i + 1;
				}
			}

			pp.addElement(abbrev.substring(lastIndex));

			// the first element of pp is the abbrev itself
			abbrev = (String)pp.elementAt(0);
		}
		else
		{
			wordStart = TextUtilities.findWordStart(lineText,pos - 1,
				(String)buffer.getProperty("noWordSep"));

			abbrev = lineText.substring(wordStart,pos);
		}

		Expansion expand = expandAbbrev(buffer.getMode().getName(),
			abbrev,(buffer.getBooleanProperty("noTabs") ?
			buffer.getTabSize() : 0),pp);

		if(expand == null)
		{
			if(add)
				new AddAbbrevDialog(view,abbrev);

			return false;
		}
		else
		{
			buffer.beginCompoundEdit();
			try
			{
				// obtain the leading indent for later use
				lineText = buffer.getText(lineStart,wordStart);
				int leadingIndent = MiscUtilities.getLeadingWhiteSpaceWidth(
					lineText,buffer.getTabSize());

				buffer.remove(lineStart + wordStart,pos - wordStart);
				buffer.insertString(lineStart + wordStart,expand.text,null);
				if(expand.caretPosition != -1)
				{
					textArea.setCaretPosition(lineStart + wordStart
						+ expand.caretPosition);
				}

				String whiteSpace = MiscUtilities.createWhiteSpace(
					leadingIndent,buffer.getBooleanProperty("noTabs")
					? 0 : buffer.getTabSize());

				Element map = buffer.getDefaultRootElement();

				// note that if expand.lineCount is 0, we
				// don't do any indentation at all
				for(int i = line + 1; i <= line + expand.lineCount; i++)
				{
					Element elem = map.getElement(i);
					buffer.insertString(elem.getStartOffset(),
						whiteSpace,null);
				}
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,Abbrevs.class,bl);
			}
			buffer.endCompoundEdit();

			return true;
		}
	}

	/**
	 * Returns the global abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable getGlobalAbbrevs()
	{
		return globalAbbrevs;
	}

	/**
	 * Sets the global abbreviation set.
	 * @param globalAbbrevs The new global abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setGlobalAbbrevs(Hashtable globalAbbrevs)
	{
		abbrevsChanged = true;
		Abbrevs.globalAbbrevs = globalAbbrevs;
	}

	/**
	 * Returns the mode-specific abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable getModeAbbrevs()
	{
		return modes;
	}

	/**
	 * Sets the mode-specific abbreviation set.
	 * @param globalAbbrevs The new global abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setModeAbbrevs(Hashtable modes)
	{
		abbrevsChanged = true;
		Abbrevs.modes = modes;
	}

	/**
	 * Adds an abbreviation to the global abbreviation list.
	 * @param abbrev The abbreviation
	 * @param expansion The expansion
	 * @since jEdit 3.1pre1
	 */
	public static void addGlobalAbbrev(String abbrev, String expansion)
	{
		globalAbbrevs.put(abbrev,expansion);
		abbrevsChanged = true;
	}

	/**
	 * Adds a mode-specific abbrev.
	 * @param mode The edit mode
	 * @param abbrev The abbrev
	 * @param expansion The expansion
	 * @since jEdit 3.1pre1
	 */
	public static void addModeAbbrev(String mode, String abbrev, String expansion)
	{
		Hashtable modeAbbrevs = (Hashtable)modes.get(mode);
		if(modeAbbrevs == null)
		{
			modeAbbrevs = new Hashtable();
			modes.put(mode,modeAbbrevs);
		}
		modeAbbrevs.put(abbrev,expansion);
		abbrevsChanged = true;
	}

	// package-private members
	static void load()
	{
		expandOnInput = jEdit.getBooleanProperty("view.expandOnInput");

		globalAbbrevs = new Hashtable();
		modes = new Hashtable();

		boolean loaded = false;

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			File file = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			abbrevsModTime = file.lastModified();

			try
			{
				loadAbbrevs(new FileReader(file));
				loaded = true;
			}
			catch(FileNotFoundException fnf)
			{
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading " + file);
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}

		// only load global abbrevs if user abbrevs file could not be loaded
		if(!loaded)
		{
			try
			{
				loadAbbrevs(new InputStreamReader(Abbrevs.class
					.getResourceAsStream("default.abbrevs")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading default.abbrevs");
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}
	}

	static void save()
	{
		jEdit.setBooleanProperty("view.expandOnInput",expandOnInput);

		String settings = jEdit.getSettingsDirectory();
		if(abbrevsChanged && settings != null)
		{
			File file = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			if(file.exists() && file.lastModified() != abbrevsModTime)
			{
				Log.log(Log.WARNING,Abbrevs.class,file + " changed on disk;"
					+ " will not save abbrevs");
			}
			else
			{
				try
				{
					saveAbbrevs(new FileWriter(file));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,Abbrevs.class,"Error while saving " + file);
					Log.log(Log.ERROR,Abbrevs.class,e);
				}
				abbrevsModTime = file.lastModified();
			}
		}
	}

	// private members
	private static boolean abbrevsChanged;
	private static long abbrevsModTime;
	private static boolean expandOnInput;
	private static Hashtable globalAbbrevs;
	private static Hashtable modes;
	private static Vector pp = new Vector();

	private Abbrevs() {}

	private static Expansion expandAbbrev(String mode, String abbrev,
		int softTabSize, Vector pp)
	{
		// try mode-specific abbrevs first
		String expand = null;
		Hashtable modeAbbrevs = (Hashtable)modes.get(mode);
		if(modeAbbrevs != null)
			expand = (String)modeAbbrevs.get(abbrev);

		if(expand == null)
			expand = (String)globalAbbrevs.get(abbrev);

		if(expand == null)
			return null;
		else
			return new Expansion(expand,softTabSize,pp);
	}

	private static void loadAbbrevs(Reader _in) throws Exception
	{
		BufferedReader in = new BufferedReader(_in);

		Hashtable currentAbbrevs = null;

		String line;
		while((line = in.readLine()) != null)
		{
			if(line.length() == 0)
				continue;
			else if(line.startsWith("[") && line.indexOf('|') == -1)
			{
				if(line.equals("[global]"))
					currentAbbrevs = globalAbbrevs;
				else
				{
					String mode = line.substring(1,
						line.length() - 1);
					currentAbbrevs = (Hashtable)modes.get(mode);
					if(currentAbbrevs == null)
					{
						currentAbbrevs = new Hashtable();
						modes.put(mode,currentAbbrevs);
					}
				}
			}
			else
			{
				int index = line.indexOf('|');
				currentAbbrevs.put(line.substring(0,index),
					line.substring(index + 1));
			}
		}

		in.close();
	}

	private static void saveAbbrevs(Writer _out) throws Exception
	{
		BufferedWriter out = new BufferedWriter(_out);
		String lineSep = System.getProperty("line.separator");

		// write global abbrevs
		out.write("[global]");
		out.write(lineSep);

		saveAbbrevs(out,globalAbbrevs);

		// write mode abbrevs
		Enumeration keys = modes.keys();
		Enumeration values = modes.elements();
		while(keys.hasMoreElements())
		{
			out.write('[');
			out.write((String)keys.nextElement());
			out.write(']');
			out.write(lineSep);
			saveAbbrevs(out,(Hashtable)values.nextElement());
		}

		out.close();
	}

	private static void saveAbbrevs(Writer out, Hashtable abbrevs)
		throws Exception
	{
		String lineSep = System.getProperty("line.separator");

		Enumeration keys = abbrevs.keys();
		Enumeration values = abbrevs.elements();
		while(keys.hasMoreElements())
		{
			String abbrev = (String)keys.nextElement();
			out.write(abbrev);
			out.write('|');
			out.write(values.nextElement().toString());
			out.write(lineSep);
		}
	}

	static class Expansion
	{
		String text;
		int caretPosition = -1;
		int lineCount;

		Expansion(String text, int softTabSize, Vector pp)
		{
			StringBuffer buf = new StringBuffer();
			boolean backslash = false;

			for(int i = 0; i < text.length(); i++)
			{
				char ch = text.charAt(i);
				if(backslash)
				{
					backslash = false;

					if(ch == '|')
						caretPosition = buf.length();
					else if(ch == 'n')
					{
						buf.append('\n');
						lineCount++;
					}
					else if(ch == 't')
					{
						if(softTabSize == 0)
							buf.append('\t');
						else
						{
							for(int j = 0; j < softTabSize; j++)
								buf.append(' ');
						}
					}
					else
						buf.append(ch);
				}
				else if(ch == '\\')
					backslash = true;
				else if(ch == '$')
				{
					if(i != text.length() - 1)
					{
						ch = text.charAt(i + 1);
						if(Character.isDigit(ch) && ch != '0')
						{
							i++;

							int pos = ch - '0';
							if(pos < pp.size())
								buf.append(pp.elementAt(pos));
							else
							{
								// so the user knows
								// a positional is
								// expected
								buf.append('$');
								buf.append(ch);
							}
						}
						else
						{
							// $key will be $key, for
							// example
							buf.append('$');
						}
					}
				}
				else
					buf.append(ch);
			}

			this.text = buf.toString();
		}
	}
}
