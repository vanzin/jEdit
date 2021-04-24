/*
 * Abbrevs.java - Abbreviation manager
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.gjt.sp.jedit.gui.AddAbbrevDialog;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Abbreviation manager.
 * @author Slava Pestov
 * @version $Id$
 */
public class Abbrevs
{
	//{{{ getExpandOnInput() method
	/**
	 * @return if abbreviations should be expanded after the
	 * user finishes typing a word.
	 */
	public static boolean getExpandOnInput()
	{
		return expandOnInput;
	} //}}}

	//{{{ setExpandOnInput() method
	/**
	 * Sets if abbreviations should be expanded after the
	 * user finishes typing a word.
	 * @param expandOnInput If true, typing a non-alphanumeric character
	 * will automatically attempt to expand the current abbrev
	 */
	public static void setExpandOnInput(boolean expandOnInput)
	{
		Abbrevs.expandOnInput = expandOnInput;
	} //}}}

	//{{{ expandAbbrev() method
	/**
	 * Expands the abbrev at the caret position in the specified
	 * view.
	 * @param view The view
	 * @param add If true and abbrev not found, will ask user if
	 * it should be added
	 * @return if expanded
	 * @since jEdit 2.6pre4
	 */
	public static boolean expandAbbrev(View view, boolean add)
	{
		//{{{ Figure out some minor things
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();
		if(!buffer.isEditable())
		{
			javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
			return false;
		}

		int line = textArea.getCaretLine();
		int lineStart = buffer.getLineStartOffset(line);
		int caret = textArea.getCaretPosition();

		String lineText = buffer.getLineText(line);
		if(lineText.isEmpty())
		{
			if(add)
				javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
			return false;
		}

		int pos = caret - lineStart;
		if(pos == 0)
		{
			if(add)
				javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
			return false;
		} //}}}

		// we reuse the 'pp' vector to save time
		m_pp.removeAllElements();

		int wordStart;
		String abbrev;

		//{{{ Handle abbrevs of the form abbrev#pos1#pos2#pos3#...
		if(lineText.charAt(pos-1) == '#')
		{
			wordStart = lineText.indexOf('#');
			wordStart = TextUtilities.findWordStart(lineText,wordStart,
				buffer.getStringProperty("noWordSep") + '#');

			abbrev = lineText.substring(wordStart,pos - 1);

			// positional parameters will be inserted where $1, $2, $3, ...
			// occurs in the expansion

			int lastIndex = 0;
			for(int i = 0; i < abbrev.length(); i++)
			{
				if(abbrev.charAt(i) == '#')
				{
					m_pp.addElement(abbrev.substring(lastIndex,i));
					lastIndex = i + 1;
				}
			}

			m_pp.addElement(abbrev.substring(lastIndex));

			// the first element of pp is the abbrev itself
			abbrev = m_pp.elementAt(0);
			m_pp.removeElementAt(0);
		} //}}}
		//{{{ Handle ordinary abbrevs
		else
		{
			wordStart = TextUtilities.findWordStart(lineText,pos - 1,
				buffer.getStringProperty("noWordSep"));

			abbrev = lineText.substring(wordStart,pos);
		} //}}}

		Expansion expand = expandAbbrev(buffer.getMode().getName(),
			abbrev,(buffer.getBooleanProperty("noTabs") ?
			buffer.getTabSize() : 0),m_pp);

		//{{{ Maybe show add abbrev dialog
		if(expand == null)
		{
			if(add)
				new AddAbbrevDialog(view,abbrev);

			return false;
		} //}}}
		//{{{ Insert the expansion
		else
		{
			buffer.remove(lineStart + wordStart,
				pos - wordStart);

			int whitespace = buffer.insertIndented(
				lineStart + wordStart,
				expand.text);

			int newlines = countNewlines(expand.text,
				expand.caretPosition);

			if(expand.caretPosition != -1)
			{
				textArea.setCaretPosition(lineStart + wordStart
					+ expand.caretPosition
					+ newlines * whitespace);
			}
			if(expand.posParamCount != m_pp.size())
			{
				view.getStatus().setMessageAndClear(
					jEdit.getProperty(
					"view.status.incomplete-abbrev",
					new Integer[] {m_pp.size(), expand.posParamCount}));
			}

			return true;
		} //}}}
	} //}}}

	//{{{ getGlobalAbbrevs() method
	/**
	 * @return the global abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable<String,String> getGlobalAbbrevs()
	{
		if(!loaded)
			load();

		return globalAbbrevs;
	} //}}}

	//{{{ setGlobalAbbrevs() method
	/**
	 * Sets the global abbreviation set.
	 * @param globalAbbrevs The new global abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setGlobalAbbrevs(Hashtable<String,String> globalAbbrevs)
	{
		abbrevsChanged = true;
		Abbrevs.globalAbbrevs = globalAbbrevs;
	} //}}}

	//{{{ getModeAbbrevs() method
	/**
	 * @return the mode-specific abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable<String,Hashtable<String,String>> getModeAbbrevs()
	{
		if(!loaded)
			load();

		return modes;
	} //}}}

	//{{{ setModeAbbrevs() method
	/**
	 * Sets the mode-specific abbreviation set.
	 * @param modes The new mode abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setModeAbbrevs(Hashtable<String,Hashtable<String,String>> modes)
	{
		abbrevsChanged = true;
		Abbrevs.modes = modes;
	} //}}}

	//{{{ addGlobalAbbrev() method
	/**
	 * Adds an abbreviation to the global abbreviation list.
	 * @param abbrev The abbreviation
	 * @param expansion The expansion
	 * @since jEdit 3.1pre1
	 */
	public static void addGlobalAbbrev(String abbrev, String expansion)
	{
		if(!loaded)
			load();

		globalAbbrevs.put(abbrev,expansion);
		abbrevsChanged = true;
	} //}}}

	//{{{ addModeAbbrev() method
	/**
	 * Adds a mode-specific abbrev.
	 * @param mode The edit mode
	 * @param abbrev The abbrev
	 * @param expansion The expansion
	 * @since jEdit 3.1pre1
	 */
	public static void addModeAbbrev(String mode, String abbrev, String expansion)
	{
		if(!loaded)
			load();

		Map<String, String> modeAbbrevs = modes.computeIfAbsent(mode, k -> new Hashtable<>());
		modeAbbrevs.put(abbrev,expansion);
		abbrevsChanged = true;
	} //}}}

	//{{{ save() method
	static void save()
	{
		jEdit.setBooleanProperty("view.expandOnInput",expandOnInput);

		String settings = jEdit.getSettingsDirectory();
		if(abbrevsChanged && settings != null)
		{
			File tempFile = new File(MiscUtilities.constructPath(settings,"#abbrevs#save#"));
			File targetFile = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			if(targetFile.exists() && targetFile.lastModified() != abbrevsModTime)
			{
				Log.log(Log.WARNING,Abbrevs.class,targetFile + " changed on disk;"
					+ " will not save abbrevs");
			}
			else
			{
				jEdit.backupSettingsFile(targetFile);

				try
				{
					saveAbbrevs(tempFile);
					targetFile.delete();
					tempFile.renameTo(targetFile);
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,Abbrevs.class,"Error while saving " + tempFile);
					Log.log(Log.ERROR,Abbrevs.class,e);
				}
				abbrevsModTime = targetFile.lastModified();
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private static boolean loaded;
	private static boolean abbrevsChanged;
	private static long abbrevsModTime;
	private static boolean expandOnInput;
	private static Hashtable<String,String> globalAbbrevs;
	private static Hashtable<String,Hashtable<String,String>> modes;
	
	/**  Vector of Positional Parameters */
	private static Vector<String> m_pp = new Vector<>();
	//}}}

	private Abbrevs() {}

	static
	{
		expandOnInput = jEdit.getBooleanProperty("view.expandOnInput");
	}

	//{{{ load() method
	private static void load()
	{
		globalAbbrevs = new Hashtable<>();
		modes = new Hashtable<>();

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			File file = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			abbrevsModTime = file.lastModified();

			try
			{
				loadAbbrevs(new InputStreamReader(
					new FileInputStream(file), StandardCharsets.UTF_8));
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
					.getResourceAsStream("default.abbrevs"),
					StandardCharsets.UTF_8));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading default.abbrevs");
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
			loaded = true;
		}
	} //}}}

	//{{{ countNewlines() method
	private static int countNewlines(String s, int end)
	{
		int counter = 0;

		for(int i = 0; i < end; i++)
		{
			if(s.charAt(i) == '\n')
				counter++;
		}

		return counter;
	} //}}}

	//{{{ expandAbbrev() method
	private static Expansion expandAbbrev(String mode, String abbrev,
		int softTabSize, Vector<String> pp)
	{
		m_pp = pp;
		if(!loaded)
			load();

		// try mode-specific abbrevs first
		String expand = null;
		Map<String,String> modeAbbrevs = modes.get(mode);
		if(modeAbbrevs != null)
			expand = modeAbbrevs.get(abbrev);

		if(expand == null)
			expand = globalAbbrevs.get(abbrev);

		if(expand == null)
			return null;
		else
			return new Expansion(expand,softTabSize,m_pp);
	} //}}}

	//{{{ loadAbbrevs() method
	private static void loadAbbrevs(Reader _in) throws IOException
	{
		try (BufferedReader in = new BufferedReader(_in))
		{
			Map<String, String> currentAbbrevs = globalAbbrevs;

			String line;
			while((line = in.readLine()) != null)
			{
				int index = line.indexOf('|');

				if(line.isEmpty())
					continue;
				else if(line.startsWith("[") && index == -1)
				{
					if(line.equals("[global]"))
						currentAbbrevs = globalAbbrevs;
					else
					{
						String mode = line.substring(1,
							line.length() - 1);
						currentAbbrevs = modes.computeIfAbsent(mode, k -> new Hashtable<>());
					}
				}
				else if(index != -1)
				{
					currentAbbrevs.put(line.substring(0,index),
						line.substring(index + 1));
				}
			}
		}
	} //}}}

	//{{{ saveAbbrevs() methods
	private static void saveAbbrevs(File file) throws IOException
	{
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8)))
		{
		String lineSep = System.getProperty("line.separator");

		// write global abbrevs
		out.write("[global]");
		out.write(lineSep);

		saveAbbrevs(out,globalAbbrevs);

		// write mode abbrevs
			for (Map.Entry<String, Hashtable<String, String>> entry : modes.entrySet())
		{
			out.write('[');
				out.write(entry.getKey());
			out.write(']');
			out.write(lineSep);
				saveAbbrevs(out,entry.getValue());
			}
		}
		}

	private static void saveAbbrevs(Writer out, Map<String,String> abbrevs) throws IOException
	{
		String lineSep = System.getProperty("line.separator");

		for (Map.Entry<String, String> entry : abbrevs.entrySet())
		{
			out.write(entry.getKey());
			out.write('|');
			out.write(entry.getValue());
			out.write(lineSep);
		}
	} //}}}

	//}}}

	//{{{ Expansion class
	static class Expansion
	{
		String text;
		int caretPosition = -1;
		int lineCount;

		// number of positional parameters in abbreviation expansion
		int posParamCount;

		//{{{ Expansion constructor
		Expansion(String text, int softTabSize, List<String> pp)
		{
			StringBuilder buf = new StringBuilder();
			boolean backslash = false;

			for(int i = 0; i < text.length(); i++)
			{
				char ch = text.charAt(i);
				//{{{ Handle backslash
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
				//}}}
				//{{{ Handle $
				else if(ch == '$')
				{
					if(i != text.length() - 1)
					{
						ch = text.charAt(i + 1);
						if(Character.isDigit(ch) && ch != '0')
						{
							i++;

							int pos = ch - '0';
							posParamCount = Math.max(pos,posParamCount);
							// $n is 1-indexed, but vector
							// contents is zero indexed
							if(pos <= pp.size())
								buf.append(pp.get(pos - 1));
						}
						else
						{
							// $key will be $key, for
							// example
							buf.append('$');
						}
					}
					else
						buf.append('$'); // $ at end is literal
				} //}}}
				else
					buf.append(ch);
			}

			this.text = buf.toString();
		} //}}}
	} //}}}
}
