/*
 * SearchAndReplace.java - Search and replace
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 2001 Tom Locke
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

package org.gjt.sp.jedit.search;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.JOptionPane;
import java.awt.Component;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.SearchSettingsChanged;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Class that implements regular expression and literal search within
 * jEdit buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class SearchAndReplace
{
	/**
	 * Sets the current search string.
	 * @param search The new search string
	 */
	public static void setSearchString(String search)
	{
		if(search.equals(SearchAndReplace.search))
			return;

		SearchAndReplace.search = search;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search string.
	 */
	public static String getSearchString()
	{
		return search;
	}

	/**
	 * Sets the current replacement string.
	 * @param search The new replacement string
	 */
	public static void setReplaceString(String replace)
	{
		if(replace.equals(SearchAndReplace.replace))
			return;

		SearchAndReplace.replace = replace;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current replacement string.
	 */
	public static String getReplaceString()
	{
		return replace;
	}

	/**
	 * Sets the ignore case flag.
	 * @param ignoreCase True if searches should be case insensitive,
	 * false otherwise
	 */
	public static void setIgnoreCase(boolean ignoreCase)
	{
		if(ignoreCase == SearchAndReplace.ignoreCase)
			return;

		SearchAndReplace.ignoreCase = ignoreCase;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the ignore case flag.
	 * @return True if searches should be case insensitive,
	 * false otherwise
	 */
	public static boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	/**
	 * Sets the state of the regular expression flag.
	 * @param regexp True if regular expression searches should be
	 * performed
	 */
	public static void setRegexp(boolean regexp)
	{
		if(regexp == SearchAndReplace.regexp)
			return;

		SearchAndReplace.regexp = regexp;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the regular expression flag.
	 * @return True if regular expression searches should be performed
	 */
	public static boolean getRegexp()
	{
		return regexp;
	}

	/**
	 * Sets the reverse search flag. Note that currently, only literal
	 * reverse searches are supported.
	 * @param reverse True if searches should go backwards,
	 * false otherwise
	 */
	public static void setReverseSearch(boolean reverse)
	{
		if(reverse == SearchAndReplace.reverse)
			return;

		SearchAndReplace.reverse = reverse;

		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the reverse search flag.
	 * @return True if searches should go backwards,
	 * false otherwise
	 */
	public static boolean getReverseSearch()
	{
		return reverse;
	}

	/**
	 * Sets the state of the BeanShell replace flag.
	 * @param regexp True if the replace string is a BeanShell expression
	 * @since jEdit 3.2pre2
	 */
	public static void setBeanShellReplace(boolean beanshell)
	{
		if(beanshell == SearchAndReplace.beanshell)
			return;

		SearchAndReplace.beanshell = beanshell;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the BeanShell replace flag.
	 * @return True if the replace string is a BeanShell expression
	 * @since jEdit 3.2pre2
	 */
	public static boolean getBeanShellReplace()
	{
		return beanshell;
	}

	/**
	 * Sets the state of the auto wrap around flag.
	 * @param wrap If true, the 'continue search from start' dialog
	 * will not be displayed
	 * @since jEdit 3.2pre2
	 */
	public static void setAutoWrapAround(boolean wrap)
	{
		if(wrap == SearchAndReplace.wrap)
			return;

		SearchAndReplace.wrap = wrap;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the auto wrap around flag.
	 * @param wrap If true, the 'continue search from start' dialog
	 * will not be displayed
	 * @since jEdit 3.2pre2
	 */
	public static boolean getAutoWrapAround()
	{
		return wrap;
	}

	/**
	 * Sets the current search string matcher. Note that calling
	 * <code>setSearchString</code>, <code>setReplaceString</code>,
	 * <code>setIgnoreCase</code> or <code>setRegExp</code> will
	 * reset the matcher to the default.
	 */
	public static void setSearchMatcher(SearchMatcher matcher)
	{
		SearchAndReplace.matcher = matcher;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search string matcher.
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static SearchMatcher getSearchMatcher()
		throws Exception
	{
		return getSearchMatcher(true);
	}

	/**
	 * Returns the current search string matcher.
	 * @param reverseOK Replacement commands need a non-reversed matcher,
	 * so they set this to false
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static SearchMatcher getSearchMatcher(boolean reverseOK)
		throws Exception
	{
		reverseOK &= (fileset instanceof CurrentBufferSet);

		if(matcher != null && (reverseOK || !reverse))
			return matcher;

		if(search == null || "".equals(search))
			return null;

		// replace must not be null
		String replace = (SearchAndReplace.replace == null ? "" : SearchAndReplace.replace);

		String replaceMethod;
		if(beanshell && replace.length() != 0)
		{
			replaceMethod = BeanShell.cacheBlock("replace","return ("
				+ replace + ");",false);
		}
		else
			replaceMethod = null;

		if(regexp)
			matcher = new RESearchMatcher(search,replace,ignoreCase,
				beanshell,replaceMethod);
		else
		{
			matcher = new BoyerMooreSearchMatcher(search,replace,
				ignoreCase,reverse && reverseOK,beanshell,
				replaceMethod);
		}

		return matcher;
	}

	/**
	 * Sets the current search file set.
	 * @param fileset The file set to perform searches in
	 */
	public static void setSearchFileSet(SearchFileSet fileset)
	{
		SearchAndReplace.fileset = fileset;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search file set.
	 */
	public static SearchFileSet getSearchFileSet()
	{
		return fileset;
	}

	/**
	 * Performs a HyperSearch.
	 * @param view The view
	 * @since jEdit 2.7pre3
	 */
	public static boolean hyperSearch(View view)
	{
		record(view,"hyperSearch(view)",false,true);

		view.getDockableWindowManager().addDockableWindow(
			HyperSearchResults.NAME);
		final HyperSearchResults results = (HyperSearchResults)
			view.getDockableWindowManager()
			.getDockable(HyperSearchResults.NAME);
		results.searchStarted();

		try
		{
			VFSManager.runInWorkThread(new HyperSearchRequest(view,
				getSearchMatcher(false),results));
			return true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
			return false;
		}
	}

	/**
	 * Finds the next occurance of the search string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean find(View view)
	{
		boolean repeat = false;
		Buffer buffer = fileset.getNextBuffer(view,null);

		try
		{
			SearchMatcher matcher = getSearchMatcher(true);
			if(matcher == null)
			{
				view.getToolkit().beep();
				return false;
			}

			record(view,"find(view)",false,true);

			view.showWaitCursor();

loop:			for(;;)
			{
				while(buffer != null)
				{
					// Wait for the buffer to load
					if(!buffer.isLoaded())
						VFSManager.waitForRequests();

					int start;

					if(view.getBuffer() == buffer && !repeat)
					{
						JEditTextArea textArea = view.getTextArea();
						Selection s = textArea.getSelectionAtOffset(
							textArea.getCaretPosition());
						if(s == null)
							start = textArea.getCaretPosition();
						else if(reverse)
							start = s.getStart();
						else 
							start = s.getEnd();
					}
					else if(reverse)
						start = buffer.getLength();
					else
						start = 0;

					if(find(view,buffer,start))
						return true;

					buffer = fileset.getNextBuffer(view,buffer);
				}

				if(repeat)
				{
					// no point showing this dialog box twice
					view.getToolkit().beep();
					return false;
				}

				/* Don't do this when playing a macro */
				if(BeanShell.isScriptRunning())
					break loop;

				boolean restart;
				if(wrap)
				{
					view.getStatus().setMessageAndClear(
						jEdit.getProperty("view.status.auto-wrap"));
					restart = true;
				}
				else
				{
					Integer[] args = { new Integer(reverse ? 1 : 0) };
					int result = GUIUtilities.confirm(view,
						"keepsearching",args,
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
					restart = (result == JOptionPane.YES_OPTION);
				}

				if(restart)
				{
					// start search from beginning
					buffer = fileset.getFirstBuffer(view);
					repeat = true;
				}
				else
					break loop;
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			view.hideWaitCursor();
		}

		return false;
	}

	/**
	 * Finds the next instance of the search string in the specified
	 * buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 */
	public static boolean find(final View view, final Buffer buffer, final int start)
		throws Exception
	{
		SearchMatcher matcher = getSearchMatcher(true);

		Segment text = new Segment();
		if(reverse)
			buffer.getText(0,start,text);
		else
			buffer.getText(start,buffer.getLength() - start,text);

		int[] match = matcher.nextMatch(text);
		if(match != null)
		{
			fileset.matchFound(buffer);
			view.setBuffer(buffer);
			JEditTextArea textArea = view.getTextArea();
			int matchStart = (reverse ? 0 : start);
			textArea.setSelection(new Selection.Range(
				matchStart + match[0],
				matchStart + match[1]));
			textArea.moveCaretPosition(matchStart + match[1]);
			return true;
		}
		else
			return false;
	}

	/**
	 * Replaces the current selection with the replacement string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean replace(View view)
	{
		JEditTextArea textArea = view.getTextArea();

		Selection[] selection = textArea.getSelection();
		if(selection.length == 0)
		{
			view.getToolkit().beep();
			return false;
		}

		record(view,"replace(view)",true,false);

		Buffer buffer = view.getBuffer();

		try
		{
			buffer.beginCompoundEdit();

			int retVal = 0;

			for(int i = 0; i < selection.length; i++)
			{
				Selection s = selection[i];

				/* if an occurence occurs at the
				beginning of the selection, the
				selection start will get moved.
				this sucks, so we hack to avoid it. */
				int start = s.getStart();

				retVal += _replace(view,buffer,
					s.getStart(),s.getEnd());

				// this has no effect if the selection
				// no longer exists
				textArea.removeFromSelection(s);
				if(s instanceof Selection.Range)
				{
					textArea.addToSelection(new Selection.Range(
						start,s.getEnd()));
				}
				else if(s instanceof Selection.Rect)
				{
					textArea.addToSelection(new Selection.Rect(
						start,s.getEnd()));
				}
			}

			if(retVal == 0)
			{
				view.getToolkit().beep();
				return false;
			}

			return true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		return false;
	}

	/**
	 * Replaces text in the specified range with the replacement string.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start The start offset
	 * @param end The end offset
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean replace(View view, Buffer buffer, int start, int end)
	{
		JEditTextArea textArea = view.getTextArea();

		try
		{
			int retVal = 0;

			buffer.beginCompoundEdit();

			retVal += _replace(view,buffer,start,end);

			if(retVal != 0)
				return true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		return false;
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 */
	public static boolean replaceAll(View view)
	{
		int fileCount = 0;
		int occurCount = 0;

		record(view,"replaceAll(view)",true,true);

		view.showWaitCursor();

		try
		{
			Buffer buffer = fileset.getFirstBuffer(view);
			do
			{
				// Wait for buffer to finish loading
				if(buffer.isPerformingIO())
					VFSManager.waitForRequests();

				// Leave buffer in a consistent state if
				// an error occurs
				try
				{
					buffer.beginCompoundEdit();
					int retVal = _replace(view,buffer,
						0,buffer.getLength());
					if(retVal != 0)
					{
						fileCount++;
						occurCount += retVal;
						fileset.matchFound(buffer);
					}
				}
				finally
				{
					buffer.endCompoundEdit();
				}
			}
			while((buffer = fileset.getNextBuffer(view,buffer)) != null);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			view.hideWaitCursor();
		}

		/* Don't do this when playing a macro, cos it's annoying */
		if(!BeanShell.isScriptRunning())
		{
			Object[] args = { new Integer(occurCount),
				new Integer(fileCount) };
			view.getStatus().setMessageAndClear(jEdit.getProperty(
				"view.status.replace-all",args));
		}

		return (fileCount != 0);
	}

	/**
	 * Loads search and replace state from the properties.
	 */
	public static void load()
	{
		search = jEdit.getProperty("search.find.value");
		replace = jEdit.getProperty("search.replace.value");
		ignoreCase = jEdit.getBooleanProperty("search.ignoreCase.toggle");
		regexp = jEdit.getBooleanProperty("search.regexp.toggle");
		reverse = jEdit.getBooleanProperty("search.reverse.toggle");
		beanshell = jEdit.getBooleanProperty("search.beanshell.toggle");
		wrap = jEdit.getBooleanProperty("search.wrap.toggle");

		String filesetCode = jEdit.getProperty("search.fileset.value");
		if(filesetCode != null)
		{
			fileset = (SearchFileSet)BeanShell.eval(null,filesetCode,true);
		}

		if(fileset == null)
			fileset = new CurrentBufferSet();
	}

	/**
	 * Saves search and replace state to the properties.
	 */
	public static void save()
	{
		jEdit.setProperty("search.find.value",search);
		jEdit.setProperty("search.replace.value",replace);
		jEdit.setBooleanProperty("search.ignoreCase.toggle",ignoreCase);
		jEdit.setBooleanProperty("search.regexp.toggle",regexp);
		jEdit.setBooleanProperty("search.reverse.toggle",reverse);
		jEdit.setBooleanProperty("search.beanshell.toggle",beanshell);
		jEdit.setBooleanProperty("search.wrap.toggle",wrap);

		String code = fileset.getCode();
		if(code != null)
			jEdit.setProperty("search.fileset.value",code);
		else
			jEdit.unsetProperty("search.fileset.value");
	}

	// private members
	private static String search;
	private static String replace;
	private static boolean regexp;
	private static boolean ignoreCase;
	private static boolean reverse;
	private static boolean beanshell;
	private static boolean wrap;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;

	private static void record(View view, String action,
		boolean replaceAction, boolean recordFileSet)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null)
		{
			recorder.record("SearchAndReplace.setSearchString(\""
				+ MiscUtilities.charsToEscapes(search) + "\");");

			if(replaceAction)
			{
				recorder.record("SearchAndReplace.setReplaceString(\""
					+ MiscUtilities.charsToEscapes(replace) + "\");");
				recorder.record("SearchAndReplace.setBeanShellReplace("
					+ beanshell + ");");
			}
			else
			{
				// only record this if doing a find next
				recorder.record("SearchAndReplace.setAutoWrapAround("
					+ wrap + ");");
				recorder.record("SearchAndReplace.setReverseSearch("
					+ reverse + ");");
			}

			recorder.record("SearchAndReplace.setIgnoreCase("
				+ ignoreCase + ");");
			recorder.record("SearchAndReplace.setRegexp("
				+ regexp + ");");

			if(recordFileSet)
			{
				recorder.record("SearchAndReplace.setSearchFileSet("
					+ fileset.getCode() + ");");
			}

			recorder.record("SearchAndReplace." + action + ";");
		}
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start The start offset
	 * @param end The end offset
	 * @return True if the replace operation was successful, false
	 * if no matches were found
	 */
	private static int _replace(View view, Buffer buffer,
		int start, int end) throws Exception
	{
		if(!buffer.isEditable())
			return 0;

		SearchMatcher matcher = getSearchMatcher(false);
		if(matcher == null)
			return 0;

		int occurCount = 0;

		Segment text = new Segment();
		int offset = start;
loop:		for(;;)
		{
			buffer.getText(offset,end - offset,text);
			int[] occur = matcher.nextMatch(text);
			if(occur == null)
				break loop;
			int _start = occur[0];
			int _length = occur[1] - occur[0];

			String found = new String(text.array,text.offset + _start,_length);
			String subst = matcher.substitute(found);

			if(subst != null)
			{
				buffer.remove(offset + _start,_length);
				buffer.insertString(offset + _start,subst,null);
				occurCount++;
				offset += _start + subst.length();
				end += (subst.length() - found.length());
			}
			else
				offset += _start + _length;
		}

		return occurCount;
	}
}
