/*
 * SearchAndReplace.java - Search and replace
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001, 2002 Slava Pestov
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

//{{{ Imports
import bsh.BshMethod;
import javax.swing.text.Segment;
import javax.swing.JOptionPane;
import java.awt.Component;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.SearchSettingsChanged;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Class that implements regular expression and literal search within
 * jEdit buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class SearchAndReplace
{
	//{{{ Getters and setters

	//{{{ setSearchString() method
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
	} //}}}

	//{{{ getSearchString() method
	/**
	 * Returns the current search string.
	 */
	public static String getSearchString()
	{
		return search;
	} //}}}

	//{{{ setReplaceString() method
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
	} //}}}

	//{{{ getRepalceString() method
	/**
	 * Returns the current replacement string.
	 */
	public static String getReplaceString()
	{
		return replace;
	} //}}}

	//{{{ setIgnoreCase() method
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
	} //}}}

	//{{{ getIgnoreCase() method
	/**
	 * Returns the state of the ignore case flag.
	 * @return True if searches should be case insensitive,
	 * false otherwise
	 */
	public static boolean getIgnoreCase()
	{
		return ignoreCase;
	} //}}}

	//{{{ setRegexp() method
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
		if(regexp && reverse)
			reverse = false;

		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ getRegexp() method
	/**
	 * Returns the state of the regular expression flag.
	 * @return True if regular expression searches should be performed
	 */
	public static boolean getRegexp()
	{
		return regexp;
	} //}}}

	//{{{ setReverseSearch() method
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

		SearchAndReplace.reverse = (regexp ? false : reverse);

		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ getReverseSearch() method
	/**
	 * Returns the state of the reverse search flag.
	 * @return True if searches should go backwards,
	 * false otherwise
	 */
	public static boolean getReverseSearch()
	{
		return reverse;
	} //}}}

	//{{{ setBeanShellReplace() method
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
	} //}}}

	//{{{ getBeanShellReplace() method
	/**
	 * Returns the state of the BeanShell replace flag.
	 * @return True if the replace string is a BeanShell expression
	 * @since jEdit 3.2pre2
	 */
	public static boolean getBeanShellReplace()
	{
		return beanshell;
	} //}}}

	//{{{ setAutoWrap() method
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
	} //}}}

	//{{{ getAutoWrap() method
	/**
	 * Returns the state of the auto wrap around flag.
	 * @param wrap If true, the 'continue search from start' dialog
	 * will not be displayed
	 * @since jEdit 3.2pre2
	 */
	public static boolean getAutoWrapAround()
	{
		return wrap;
	} //}}}

	//{{{ setSearchMatcher() method
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
	} //}}}

	//{{{ getSearchMatcher() method
	/**
	 * Returns the current search string matcher.
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static SearchMatcher getSearchMatcher()
		throws Exception
	{
		return getSearchMatcher(true);
	} //}}}

	//{{{ getSearchMatcher() method
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

		BshMethod replaceMethod;
		if(beanshell && replace.length() != 0)
		{
			replaceMethod = BeanShell.cacheBlock("replace","return ("
				+ replace + ");",true);
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
	} //}}}

	//{{{ setSearchFileSet() method
	/**
	 * Sets the current search file set.
	 * @param fileset The file set to perform searches in
	 */
	public static void setSearchFileSet(SearchFileSet fileset)
	{
		SearchAndReplace.fileset = fileset;

		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ getSearchFileSet() method
	/**
	 * Returns the current search file set.
	 */
	public static SearchFileSet getSearchFileSet()
	{
		return fileset;
	} //}}}

	//}}}

	//{{{ Actions

	//{{{ hyperSearch() method
	/**
	 * Performs a HyperSearch.
	 * @param view The view
	 * @since jEdit 2.7pre3
	 */
	public static boolean hyperSearch(View view)
	{
		return hyperSearch(view,false);
	} //}}}

	//{{{ hyperSearch() method
	/**
	 * Performs a HyperSearch.
	 * @param view The view
	 * @param selection If true, will only search in the current selection.
	 * Note that the file set must be the current buffer file set for this
	 * to work.
	 * @since jEdit 4.0pre1
	 */
	public static boolean hyperSearch(View view, boolean selection)
	{
		record(view,"hyperSearch(view," + selection + ")",false,
			!selection);

		view.getDockableWindowManager().addDockableWindow(
			HyperSearchResults.NAME);
		final HyperSearchResults results = (HyperSearchResults)
			view.getDockableWindowManager()
			.getDockable(HyperSearchResults.NAME);
		results.searchStarted();

		try
		{
			SearchMatcher matcher = getSearchMatcher(false);
			if(matcher == null)
			{
				view.getToolkit().beep();
				results.searchFailed();
				return false;
			}

			Selection[] s;
			if(selection)
			{
				s = view.getTextArea().getSelection();
				if(s == null)
				{
					results.searchFailed();
					return false;
				}
			}
			else
				s = null;
			VFSManager.runInWorkThread(new HyperSearchRequest(view,
				matcher,results,s));
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
	} //}}}

	//{{{ find() method
	/**
	 * Finds the next occurance of the search string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean find(View view)
	{
		boolean repeat = false;
		String path = fileset.getNextFile(view,null);
		if(path == null)
		{
			GUIUtilities.error(view,"empty-fileset",null);
			return false;
		}

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
				while(path != null)
				{
					Buffer buffer = jEdit.openTemporary(
						view,null,path,false);

					/* this is stupid and misleading.
					 * but 'path' is not used anywhere except
					 * the above line, and if this is done
					 * after the 'continue', then we will
					 * either hang, or be forced to duplicate
					 * it inside the buffer == null, or add
					 * a 'finally' clause. you decide which one's
					 * worse. */
					path = fileset.getNextFile(view,path);

					if(buffer == null)
						continue loop;

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

					if(find(view,buffer,start,repeat))
						return true;
				}

				if(repeat)
				{
					if(!BeanShell.isScriptRunning())
					{
						view.getStatus().setMessageAndClear(
							jEdit.getProperty("view.status.search-not-found"));

						view.getToolkit().beep();
					}
					return false;
				}

				boolean restart;

				if(BeanShell.isScriptRunning())
				{
					restart = true;
				}
				else if(wrap)
				{
					view.getStatus().setMessageAndClear(
						jEdit.getProperty("view.status.auto-wrap"));
					// beep if beep property set
					if(jEdit.getBooleanProperty("search.beepOnSearchAutoWrap"))
					{
						view.getToolkit().beep();
					}
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
					path = fileset.getFirstFile(view);
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
	} //}}}

	//{{{ find() method
	/**
	 * Finds the next instance of the search string in the specified
	 * buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 */
	public static boolean find(View view, Buffer buffer, int start)
		throws Exception
	{
		return find(view,buffer,start,false);
	} //}}}

	//{{{ find() method
	/**
	 * Finds the next instance of the search string in the specified
	 * buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 * @param firstTime See <code>SearchMatcher.nextMatch()</code>
	 * @since jEdit 4.0pre7
	 */
	public static boolean find(View view, Buffer buffer, int start,
		boolean firstTime) throws Exception
	{
		SearchMatcher matcher = getSearchMatcher(true);
		if(matcher == null)
		{
			view.getToolkit().beep();
			return false;
		}

		Segment text = new Segment();
		if(reverse)
			buffer.getText(0,start,text);
		else
			buffer.getText(start,buffer.getLength() - start,text);

		// the start and end flags will be wrong with reverse search enabled,
		// but they are only used by the regexp matcher, which doesn't
		// support reverse search yet.
		//
		// REMIND: fix flags when adding reverse regexp search.
		int[] match = matcher.nextMatch(new CharIndexedSegment(text,reverse),
			start == 0,true,firstTime);

		if(match != null)
		{
			jEdit.commitTemporary(buffer);
			view.setBuffer(buffer);
			JEditTextArea textArea = view.getTextArea();

			if(reverse)
			{
				textArea.setSelection(new Selection.Range(
					start - match[1],
					start - match[0]));
				textArea.moveCaretPosition(start - match[1]);
			}
			else
			{
				textArea.setSelection(new Selection.Range(
					start + match[0],
					start + match[1]));
				textArea.moveCaretPosition(start + match[1]);
			}

			return true;
		}
		else
			return false;
	} //}}}

	//{{{ replace() method
	/**
	 * Replaces the current selection with the replacement string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean replace(View view)
	{
		JEditTextArea textArea = view.getTextArea();

		Buffer buffer = view.getBuffer();
		if(!buffer.isEditable())
			return false;

		boolean smartCaseReplace = (replace != null
			&& TextUtilities.getStringCase(replace)
			== TextUtilities.LOWER_CASE);

		Selection[] selection = textArea.getSelection();
		if(selection.length == 0)
		{
			view.getToolkit().beep();
			return false;
		}

		record(view,"replace(view)",true,false);

		// a little hack for reverse replace and find
		int caret = textArea.getCaretPosition();
		Selection s = textArea.getSelectionAtOffset(caret);
		if(s != null)
			caret = s.getStart();

		try
		{
			buffer.beginCompoundEdit();

			SearchMatcher matcher = getSearchMatcher(false);
			if(matcher == null)
				return false;

			int retVal = 0;

			for(int i = 0; i < selection.length; i++)
			{
				s = selection[i];

				/* if an occurence occurs at the
				beginning of the selection, the
				selection start will get moved.
				this sucks, so we hack to avoid it. */
				int start = s.getStart();

				if(s instanceof Selection.Range)
				{
					retVal += _replace(view,buffer,matcher,
						s.getStart(),s.getEnd(),
						smartCaseReplace);

					textArea.removeFromSelection(s);
					textArea.addToSelection(new Selection.Range(
						start,s.getEnd()));
				}
				else if(s instanceof Selection.Rect)
				{
					for(int j = s.getStartLine(); j <= s.getEndLine(); j++)
					{
						retVal += _replace(view,buffer,matcher,
							s.getStart(buffer,j),s.getEnd(buffer,j),
							smartCaseReplace);
					}
					textArea.addToSelection(new Selection.Rect(
						start,s.getEnd()));
				}
			}

			if(reverse)
			{
				// so that Replace and Find continues from
				// the right location
				textArea.moveCaretPosition(caret);
			}
			else
			{
				s = textArea.getSelectionAtOffset(
					textArea.getCaretPosition());
				if(s != null)
					textArea.moveCaretPosition(s.getEnd());
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
	} //}}}

	//{{{ replace() method
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
		if(!buffer.isEditable())
			return false;

		boolean smartCaseReplace = (replace != null
			&& TextUtilities.getStringCase(replace)
			== TextUtilities.LOWER_CASE);

		JEditTextArea textArea = view.getTextArea();

		try
		{
			buffer.beginCompoundEdit();

			SearchMatcher matcher = getSearchMatcher(false);
			if(matcher == null)
				return false;

			int retVal = 0;

			retVal += _replace(view,buffer,matcher,start,end,
				smartCaseReplace);

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
	} //}}}

	//{{{ replaceAll() method
	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 */
	public static boolean replaceAll(View view)
	{
		int fileCount = 0;
		int occurCount = 0;

		if(fileset.getFileCount(view) == 0)
		{
			GUIUtilities.error(view,"empty-fileset",null);
			return false;
		}

		record(view,"replaceAll(view)",true,true);

		view.showWaitCursor();

		boolean smartCaseReplace = (replace != null
			&& TextUtilities.getStringCase(replace)
			== TextUtilities.LOWER_CASE);

		try
		{
			SearchMatcher matcher = getSearchMatcher(false);
			if(matcher == null)
				return false;

			String path = fileset.getFirstFile(view);
loop:			while(path != null)
			{
				Buffer buffer = jEdit.openTemporary(
					view,null,path,false);

				/* this is stupid and misleading.
				 * but 'path' is not used anywhere except
				 * the above line, and if this is done
				 * after the 'continue', then we will
				 * either hang, or be forced to duplicate
				 * it inside the buffer == null, or add
				 * a 'finally' clause. you decide which one's
				 * worse. */
				path = fileset.getNextFile(view,path);

				if(buffer == null)
					continue loop;

				// Wait for buffer to finish loading
				if(buffer.isPerformingIO())
					VFSManager.waitForRequests();

				if(!buffer.isEditable())
					continue loop;

				// Leave buffer in a consistent state if
				// an error occurs
				int retVal = 0;

				try
				{
					buffer.beginCompoundEdit();
					retVal = _replace(view,buffer,matcher,
						0,buffer.getLength(),
						smartCaseReplace);
				}
				finally
				{
					buffer.endCompoundEdit();
				}

				if(retVal != 0)
				{
					fileCount++;
					occurCount += retVal;
					jEdit.commitTemporary(buffer);
				}
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

		/* Don't do this when playing a macro, cos it's annoying */
		if(!BeanShell.isScriptRunning())
		{
			Object[] args = { new Integer(occurCount),
				new Integer(fileCount) };
			view.getStatus().setMessageAndClear(jEdit.getProperty(
				"view.status.replace-all",args));
			if(occurCount == 0)
				view.getToolkit().beep();
		}

		return (fileCount != 0);
	} //}}}

	//}}}

	//{{{ load() method
	/**
	 * Loads search and replace state from the properties.
	 */
	public static void load()
	{
		search = jEdit.getProperty("search.find.value");
		replace = jEdit.getProperty("search.replace.value");
		ignoreCase = jEdit.getBooleanProperty("search.ignoreCase.toggle");
		regexp = jEdit.getBooleanProperty("search.regexp.toggle");
		beanshell = jEdit.getBooleanProperty("search.beanshell.toggle");
		wrap = jEdit.getBooleanProperty("search.wrap.toggle");

		fileset = new CurrentBufferSet();

		// Tags plugin likes to call this method at times other than
		// startup; so we need to fire a SearchSettingsChanged to
		// notify the search bar and so on.
		matcher = null;
		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ save() method
	/**
	 * Saves search and replace state to the properties.
	 */
	public static void save()
	{
		jEdit.setProperty("search.find.value",search);
		jEdit.setProperty("search.replace.value",replace);
		jEdit.setBooleanProperty("search.ignoreCase.toggle",ignoreCase);
		jEdit.setBooleanProperty("search.regexp.toggle",regexp);
		jEdit.setBooleanProperty("search.beanshell.toggle",beanshell);
		jEdit.setBooleanProperty("search.wrap.toggle",wrap);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private static String search;
	private static String replace;
	private static boolean regexp;
	private static boolean ignoreCase;
	private static boolean reverse;
	private static boolean beanshell;
	private static boolean wrap;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;
	//}}}

	//{{{ record() method
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
	} //}}}

	//{{{ _replace() method
	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start The start offset
	 * @param end The end offset
	 * @param matcher The search matcher to use
	 * @param smartCaseReplace See user's guide
	 * @return The number of occurrences replaced
	 */
	private static int _replace(View view, Buffer buffer,
		SearchMatcher matcher, int start, int end,
		boolean smartCaseReplace)
		throws Exception
	{
		int occurCount = 0;

		boolean endOfLine = (buffer.getLineEndOffset(
			buffer.getLineOfOffset(end)) - 1 == end);

		Segment text = new Segment();
		int offset = start;
loop:		for(int counter = 0; ; counter++)
		{
			buffer.getText(offset,end - offset,text);

			boolean startOfLine = (buffer.getLineStartOffset(
				buffer.getLineOfOffset(offset)) == offset);

			int[] occur = matcher.nextMatch(
				new CharIndexedSegment(text,false),
				startOfLine,endOfLine,counter == 0);
			if(occur == null)
				break loop;
			int _start = occur[0];
			int _length = occur[1] - occur[0];

			String found = new String(text.array,text.offset + _start,_length);
			String subst = matcher.substitute(found);
			if(smartCaseReplace && ignoreCase)
			{
				int strCase = TextUtilities.getStringCase(found);
				if(strCase == TextUtilities.LOWER_CASE)
					subst = subst.toLowerCase();
				else if(strCase == TextUtilities.UPPER_CASE)
					subst = subst.toUpperCase();
				else if(strCase == TextUtilities.TITLE_CASE)
					subst = TextUtilities.toTitleCase(subst);
			}

			if(subst != null)
			{
				buffer.remove(offset + _start,_length);
				buffer.insert(offset + _start,subst);
				occurCount++;
				offset += _start + subst.length();

				end += (subst.length() - found.length());
			}
			else
				offset += _start + _length;
		}

		return occurCount;
	} //}}}

	//}}}
}
