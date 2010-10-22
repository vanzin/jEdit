/*
 * SearchAndReplace.java - Search and replace
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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
import org.gjt.sp.jedit.bsh.*;
import java.awt.*;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.TextAreaDialog;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.PositionChanging;
import org.gjt.sp.jedit.msg.SearchSettingsChanged;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.*;
//}}}

/**
 * Class that implements regular expression and literal search within
 * jEdit buffers.<p>
 *
 * There are two main groups of methods in this class:
 * <ul>
 * <li>Property accessors - for changing search and replace settings.</li>
 * <li>Actions - for performing search and replace.</li>
 * </ul>
 *
 * The "HyperSearch" and "Keep dialog" features, as reflected in
 * checkbox options in the search dialog, are not handled from within
 * this class. If you wish to have these options set before the search dialog
 * appears, make a prior call to either or both of the following:
 *
 * <pre> jEdit.setBooleanProperty("search.hypersearch.toggle",true);
 * jEdit.setBooleanProperty("search.keepDialog.toggle",true);</pre>
 *
 * If you are not using the dialog to undertake a search or replace, you may
 * call any of the search and replace methods (including
 * {@link #hyperSearch(View)}) without concern for the value of these
 * properties.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
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
	 * @param replace The new replacement string
	 */
	public static void setReplaceString(String replace)
	{
		if(replace.equals(SearchAndReplace.replace))
			return;

		SearchAndReplace.replace = replace;

		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ getReplaceString() method
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
	 * Determines whether a reverse search will conducted from the current
	 * position to the beginning of a buffer. Note that reverse search and
	 * regular expression search is mutually exclusive; enabling one will
	 * disable the other.
	 * @param reverse True if searches should go backwards,
	 * false otherwise
	 */
	public static void setReverseSearch(boolean reverse)
	{
		if(reverse == SearchAndReplace.reverse)
			return;

		SearchAndReplace.reverse = reverse;

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
	 * @param beanshell True if the replace string is a BeanShell expression
	 * @since jEdit 3.2pre2
	 */
	public static void setBeanShellReplace(boolean beanshell)
	{
		if(beanshell == SearchAndReplace.beanshell)
			return;

		SearchAndReplace.beanshell = beanshell;

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
	 * @since jEdit 3.2pre2
	 */
	public static boolean getAutoWrapAround()
	{
		return wrap;
	} //}}}

	//{{{ setSearchMatcher() method
	/**
	 * Sets a custom search string matcher. Note that calling
	 * {@link #setSearchString(String)},
	 * {@link #setIgnoreCase(boolean)}, or {@link #setRegexp(boolean)}
	 * will reset the matcher to the default.
	 */
	public static void setSearchMatcher(SearchMatcher matcher)
	{
		SearchAndReplace.matcher = matcher;

		EditBus.send(new SearchSettingsChanged(null));
	} //}}}

	//{{{ getSearchMatcher() method
	/**
	 * Returns the current search string matcher.
	 * @return a SearchMatcher or null if there is no search or if the matcher can match empty String
	 *
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 * @since jEdit 4.1pre7
	 */
	public static SearchMatcher getSearchMatcher()
		throws Exception {
		if (matcher != null)
			return matcher;

		if (search == null || "".equals(search))
			return null;

		if (regexp)
		{
			Pattern re = Pattern.compile(search, 
				PatternSearchMatcher.getFlag(ignoreCase));
			matcher = new PatternSearchMatcher(re, ignoreCase);
		}
		else
			matcher = new BoyerMooreSearchMatcher(search, ignoreCase);

		return matcher;
	} //}}}

	//{{{ setSearchFileSet() method
	/**
	 * Sets the current search file set.
	 * @param fileset The file set to perform searches in
	 * @see AllBufferSet
	 * @see CurrentBufferSet
	 * @see DirectoryListSet
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

	//{{{ getSmartCaseReplace() method
	/**
	 * Returns if the replacement string will assume the same case as
	 * each specific occurrence of the search string.
	 * @since jEdit 4.2pre10
	 */
	public static boolean getSmartCaseReplace()
	{
		return (replace != null
			&& TextUtilities.getStringCase(replace)
			== TextUtilities.LOWER_CASE);
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
		// component that will parent any dialog boxes
		Component comp = SearchDialog.getSearchDialog(view);
		if(comp == null)
			comp = view;

		record(view,"hyperSearch(view," + selection + ')',false,
			!selection);

		view.getDockableWindowManager().addDockableWindow(
			HyperSearchResults.NAME);
		HyperSearchResults results = (HyperSearchResults)
			view.getDockableWindowManager()
			.getDockable(HyperSearchResults.NAME);
		results.searchStarted();

		try
		{
			SearchMatcher matcher = getSearchMatcher();
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
			ThreadUtilities.runInBackground(
				new HyperSearchRequest(view,
					matcher,results,s));
			return true;
		}
		catch(Exception e)
		{
			results.searchFailed();
			handleError(comp,e);
			return false;
		}
	} //}}}

	//{{{ find() method
	/**
	 * Finds the next occurrence of the search string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean find(View view)
	{
		// component that will parent any dialog boxes
		Component comp = SearchDialog.getSearchDialog(view);
		if(comp == null || !comp.isShowing())
			comp = view;

		String path = fileset.getNextFile(view,null);
		if(path == null)
		{
			GUIUtilities.error(comp,"empty-fileset",null);
			return false;
		}

		boolean _reverse = reverse && fileset instanceof CurrentBufferSet;

		try
		{
			view.showWaitCursor();

			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
			{
				view.getToolkit().beep();
				return false;
			}

			record(view,"find(view)",false,true);

			boolean repeat = false;
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
						else if(_reverse)
							start = s.getStart();
						else
							start = s.getEnd();
					}
					else if(_reverse)
						start = buffer.getLength();
					else
						start = 0;

					if(find(view,buffer,start,repeat,_reverse))
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

				// if auto wrap is on, always restart search.
				// if auto wrap is off, and we're called from
				// a macro, stop search. If we're called
				// interactively, ask the user what to do.
				if(wrap)
				{
					if(!BeanShell.isScriptRunning())
					{
						view.getStatus().setMessageAndClear(
							jEdit.getProperty("view.status.auto-wrap"));
						// beep if beep property set
						if(jEdit.getBooleanProperty("search.beepOnSearchAutoWrap"))
						{
							view.getToolkit().beep();
						}
					}
					restart = true;
				}
				else if(BeanShell.isScriptRunning())
				{
					restart = false;
				}
				else
				{
					Integer[] args = {Integer.valueOf(_reverse ? 1 : 0)};
					int result = GUIUtilities.confirm(comp,
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
			handleError(comp,e);
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
		return find(view,buffer,start,false,false);
	} //}}}

	//{{{ find() method
	/**
	 * Finds the next instance of the search string in the specified
	 * buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 * @param firstTime See {@link SearchMatcher#nextMatch(CharSequence,boolean,boolean,boolean,boolean)}.
	 * @since jEdit 4.1pre7
	 */
	public static boolean find(View view, Buffer buffer, int start,
		boolean firstTime, boolean reverse) throws Exception
	{
		
		EditBus.send(new PositionChanging(view.getEditPane()));
		
		SearchMatcher matcher = getSearchMatcher();
		if(matcher == null)
		{
			view.getToolkit().beep();
			return false;
		}

		CharSequence text;
		boolean startOfBuffer;
		boolean endOfBuffer;
		if(reverse)
		{
			text = new ReverseCharSequence(buffer.getSegment(0,start));
			startOfBuffer = true;
			endOfBuffer = (start == buffer.getLength());
		}
		else
		{
			text = buffer.getSegment(start,buffer.getLength() - start);
			startOfBuffer = (start == 0);
			endOfBuffer = true;
		}
		SearchMatcher.Match match = matcher.nextMatch(text,
			startOfBuffer,endOfBuffer,firstTime,reverse);
		if(match != null)
		{
			jEdit.commitTemporary(buffer);
			view.setBuffer(buffer,true);
			JEditTextArea textArea = view.getTextArea();

			if(reverse)
			{
				textArea.setSelection(new Selection.Range(
					start - match.end,
					start - match.start));
				// make sure end of match is visible
				textArea.scrollTo(start - match.start,false);
				textArea.moveCaretPosition(start - match.end);
			}
			else
			{
				textArea.setSelection(new Selection.Range(
					start + match.start,
					start + match.end));
				textArea.moveCaretPosition(start + match.end);
				// make sure start of match is visible
				textArea.scrollTo(start + match.start,false);
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
		// component that will parent any dialog boxes
		Component comp = SearchDialog.getSearchDialog(view);
		if(comp == null)
			comp = view;

		JEditTextArea textArea = view.getTextArea();

		Buffer buffer = view.getBuffer();
		if(!buffer.isEditable())
			return false;

		boolean smartCaseReplace = getSmartCaseReplace();

		Selection[] selection = textArea.getSelection();
		if (selection.length == 0)
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

			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
				return false;

			initReplace();

			int retVal = 0;

			for(int i = 0; i < selection.length; i++)
			{
				s = selection[i];

				retVal += replaceInSelection(view,textArea,
					buffer,matcher,smartCaseReplace,s);
			}

			boolean _reverse = !regexp && reverse && fileset instanceof CurrentBufferSet;
			if(_reverse)
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

			if(!BeanShell.isScriptRunning())
			{
				Object[] args = {Integer.valueOf(retVal),
				                 Integer.valueOf(1)};
				view.getStatus().setMessageAndClear(jEdit.getProperty(
					"view.status.replace-all",args));
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
			handleError(comp,e);
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

		// component that will parent any dialog boxes
		Component comp = SearchDialog.getSearchDialog(view);
		if(comp == null)
			comp = view;

		boolean smartCaseReplace = getSmartCaseReplace();

		try
		{
			buffer.beginCompoundEdit();

			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
				return false;

			initReplace();

			int retVal = 0;

			retVal += _replace(view,buffer,matcher,start,end,
				smartCaseReplace);

			if(retVal != 0)
				return true;
		}
		catch(Exception e)
		{
			handleError(comp,e);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		return false;
	} //}}}

	//{{{ replaceAll() method
	/**
	 * Replaces all occurrences of the search string with the replacement
	 * string.
	 * @param view The view
	 * @return the number of modified files
	 */
	public static boolean replaceAll(View view)
	{
		return replaceAll(view,false);
	} //}}}
	
	//{{{ replaceAll() method
	/**
	 * Replaces all occurrences of the search string with the replacement
	 * string.
	 * @param view The view
	 * @param dontOpenChangedFiles Whether to open changed files or to autosave them quietly
	 * @return the number of modified files
	 */
	public static boolean replaceAll(View view, boolean dontOpenChangedFiles)
	{
		// component that will parent any dialog boxes
		Component comp = SearchDialog.getSearchDialog(view);
		if(comp == null)
			comp = view;

		if(fileset.getFileCount(view) == 0)
		{
			GUIUtilities.error(comp,"empty-fileset",null);
			return false;
		}

		record(view,"replaceAll(view)",true,true);

		view.showWaitCursor();

		boolean smartCaseReplace = (replace != null
			&& TextUtilities.getStringCase(replace)
			== TextUtilities.LOWER_CASE);

		int fileCount = 0;
		int occurCount = 0;
		try
		{
			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
				return false;

			initReplace();

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
					if (dontOpenChangedFiles)
					{
						buffer.save(null,null);
					}
					else
					{
						jEdit.commitTemporary(buffer);
						jEdit.getBufferSetManager().addBuffer(view, buffer);
					}
				}
			}
		}
		catch(Exception e)
		{
			handleError(comp,e);
		}
		finally
		{
			view.hideWaitCursor();
		}

		/* Don't do this when playing a macro, cos it's annoying */
		if(!BeanShell.isScriptRunning())
		{
			Object[] args = {Integer.valueOf(occurCount),
			                 Integer.valueOf(fileCount)};
			view.getStatus().setMessageAndClear(jEdit.getProperty(
				"view.status.replace-all",args));
			if(occurCount == 0)
				view.getToolkit().beep();
		}

		return (fileCount != 0);
	} //}}}

	//}}}

	//{{{ escapeRegexp() method
	/**
	 * Escapes characters with special meaning in a regexp.
	 * @param str the string to escape
	 * @param multiline Should \n be escaped?
	 * @return the string with escaped characters
	 * @since jEdit 4.3pre1
	 */
	public static String escapeRegexp(String str, boolean multiline)
	{
		return StandardUtilities.charsToEscapes(str,
			"\r\t\\()[]{}$^*+?|."
			+ (multiline ? "" : "\n"));
	} //}}}

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

	//{{{ handleError() method
	static void handleError(Component comp, Exception e)
	{
		Log.log(Log.ERROR,SearchAndReplace.class,e);
		if(comp instanceof Dialog)
		{
			new TextAreaDialog((Dialog)comp,
				beanshell ? "searcherror-bsh"
				: "searcherror",e);
		}
		else
		{
			new TextAreaDialog((Frame)comp,
				beanshell ? "searcherror-bsh"
				: "searcherror",e);
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private static String search;
	private static String replace;
	private static BshMethod replaceMethod;
	private static NameSpace replaceNS = new NameSpace(
		BeanShell.getNameSpace(),
		BeanShell.getNameSpace().getClassManager(),
		"search and replace");
	private static boolean regexp;
	private static boolean ignoreCase;
	private static boolean reverse;
	private static boolean beanshell;
	private static boolean wrap;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;
	//}}}

	//{{{ initReplace() method
	/**
	 * Set up BeanShell replace if necessary.
	 */
	private static void initReplace() throws Exception
	{
		if(beanshell && replace.length() != 0)
		{
			String text;
			if( replace.trim().startsWith( "{" ) )
				text = replace;
			else
				text = "return (" + replace + ");";	
			replaceMethod = BeanShell.cacheBlock("replace",
				text,true);
		}
		else
			replaceMethod = null;
	} //}}}

	//{{{ record() method
	private static void record(View view, String action,
		boolean replaceAction, boolean recordFileSet)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null)
		{
			recorder.record("SearchAndReplace.setSearchString(\""
				+ StandardUtilities.charsToEscapes(search) + "\");");

			if(replaceAction)
			{
				recorder.record("SearchAndReplace.setReplaceString(\""
					+ StandardUtilities.charsToEscapes(replace) + "\");");
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

			recorder.record("SearchAndReplace." + action + ';');
		}
	} //}}}

	//{{{ replaceInSelection() method
	private static int replaceInSelection(View view, TextArea textArea,
		Buffer buffer, SearchMatcher matcher, boolean smartCaseReplace,
		Selection s) throws Exception
	{
		/* if an occurence occurs at the
		beginning of the selection, the
		selection start will get moved.
		this sucks, so we hack to avoid it. */
		int start = s.getStart();

		int returnValue;

		if(s instanceof Selection.Range)
		{
			returnValue = _replace(view,buffer,matcher,
				s.getStart(),s.getEnd(),
				smartCaseReplace);

			textArea.removeFromSelection(s);
			textArea.addToSelection(new Selection.Range(
				start,s.getEnd()));
		}
		else if(s instanceof Selection.Rect)
		{
			Selection.Rect rect = (Selection.Rect)s;
			int startCol = rect.getStartColumn(
				buffer);
			int endCol = rect.getEndColumn(
				buffer);

			returnValue = 0;
			for(int j = s.getStartLine(); j <= s.getEndLine(); j++)
			{
				returnValue += _replace(view,buffer,matcher,
					getColumnOnOtherLine(buffer,j,startCol),
					getColumnOnOtherLine(buffer,j,endCol),
					smartCaseReplace);
			}
			textArea.addToSelection(new Selection.Rect(
				start,s.getEnd()));
		}
		else
			throw new RuntimeException("Unsupported: " + s);

		return returnValue;
	} //}}}

	//{{{ _replace() method
	/**
	 * Replaces all occurrences of the search string with the replacement
	 * string.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start The start offset
	 * @param end The end offset
	 * @param matcher The search matcher to use
	 * @param smartCaseReplace See user's guide
	 * @return The number of occurrences replaced
	 */
	private static int _replace(View view, JEditBuffer buffer,
		SearchMatcher matcher, int start, int end,
		boolean smartCaseReplace)
		throws Exception
	{
		int occurCount = 0;

		boolean endOfLine = (buffer.getLineEndOffset(
			buffer.getLineOfOffset(end)) - 1 == end);

		int offset = start;
loop:		for(int counter = 0; ; counter++)
		{
			boolean startOfLine = (buffer.getLineStartOffset(
				buffer.getLineOfOffset(offset)) == offset);

			CharSequence text = buffer.getSegment(offset,end - offset);
			SearchMatcher.Match occur = matcher.nextMatch(
				text,startOfLine,endOfLine,counter == 0,false);
			if(occur == null)
				break loop;

			CharSequence found = text.subSequence(
				occur.start, occur.end);

			int length = replaceOne(view,buffer,occur,offset,
				found,smartCaseReplace);
			if(length == -1)
				offset += occur.end;
			else
			{
				offset += occur.start + length;
				end += (length - found.length());
				occurCount++;
			}
		}

		return occurCount;
	} //}}}

	//{{{ replaceOne() method
	/**
	 * Replace one occurrence of the search string with the
	 * replacement string.
	 */
	private static int replaceOne(View view, JEditBuffer buffer,
		SearchMatcher.Match occur, int offset, CharSequence found,
		boolean smartCaseReplace)
		throws Exception
	{
		String subst = replaceOne(view,buffer,occur,found);
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
			int start = offset + occur.start;
			int end = offset + occur.end;

			if (end - start > 0)
				buffer.remove(start,end - start);
			buffer.insert(start,subst);
			return subst.length();
		}
		else
			return -1;
	} //}}}

	//{{{ replaceOne() method
	private static String replaceOne(View view, JEditBuffer buffer,
		SearchMatcher.Match occur, CharSequence found)
		throws Exception
	{
		if(regexp)
		{
			if(replaceMethod != null)
				return regexpBeanShellReplace(view,buffer,occur);
			else
				return regexpReplace(occur,found);
		}
		else
		{
			if(replaceMethod != null)
				return literalBeanShellReplace(view,buffer,found);
			else
				return replace;
		}
	} //}}}

	//{{{ regexpBeanShellReplace() method
	private static String regexpBeanShellReplace(View view,
		JEditBuffer buffer, SearchMatcher.Match occur) throws Exception
	{
		replaceNS.setVariable("buffer", buffer, false);
		for(int i = 0; i < occur.substitutions.length; i++)
		{
			replaceNS.setVariable("_" + i,
				occur.substitutions[i]);
		}

		Object obj = BeanShell.runCachedBlock(
			replaceMethod,view,replaceNS);

		for(int i = 0; i < occur.substitutions.length; i++)
		{
			replaceNS.setVariable("_" + i,
				null, false);
		}
		// Not really necessary because it is already cleared in the end of
		// BeanShell.runCachedBlock()
		replaceNS.setVariable("buffer", null, false);

		if(obj == null)
			return "";
		else
			return obj.toString();
	} //}}}

	//{{{ regexpReplace() method
	private static String regexpReplace(SearchMatcher.Match occur,
		CharSequence found) throws Exception
	{
		StringBuilder buf = new StringBuilder();

		for(int i = 0; i < replace.length(); i++)
		{
			char ch = replace.charAt(i);
			switch(ch)
			{
			case '$':
				if(i == replace.length() - 1)
				{
					// last character of the replace string, 
					// it is not a capturing group
					buf.append(ch);
					break;
				}

				ch = replace.charAt(++i);
				if(ch == '$')
				{
					// It was $$, so it is an escaped $
					buf.append('$');
				}
				else if(ch == '0')
				{
					// $0 meaning the first capturing group :
					// the found value
					buf.append(found);
				}
				else if(Character.isDigit(ch))
				{
					int n = ch - '0';
					while (i < replace.length() - 1)
					{
						ch = replace.charAt(++i);
						if (Character.isDigit(ch))
						{
							n = n * 10 + (ch - '0');
						}
						else
						{
							// The character is not 
							// a digit, going back and
							// end loop
							i--;
							break;
						}
					}
					if(n < occur
						.substitutions
						.length)
					{
						String subs = occur.substitutions[n];
						if (subs != null)
							buf.append(subs);
					}
				}
				break;
			case '\\':
				if(i == replace.length() - 1)
				{
					buf.append('\\');
					break;
				}
				ch = replace.charAt(++i);
				switch(ch)
				{
				case 'n':
					buf.append('\n');
					break;
				case 't':
					buf.append('\t');
					break;
				default:
					buf.append(ch);
					break;
				}
				break;
			default:
				buf.append(ch);
				break;
			}
		}

		return buf.toString();
	} //}}}

	//{{{ literalBeanShellReplace() method
	private static String literalBeanShellReplace(View view,
		JEditBuffer buffer, CharSequence found)
		throws Exception
	{
		replaceNS.setVariable("buffer",buffer);
		replaceNS.setVariable("_0",found);
		Object obj = BeanShell.runCachedBlock(
			replaceMethod,
			view,replaceNS);

		replaceNS.setVariable("_0", null, false);
		// Not really necessary because it is already cleared in the end of
		// BeanShell.runCachedBlock()
		replaceNS.setVariable("buffer", null, false);

		if(obj == null)
			return "";
		else
			return obj.toString();
	} //}}}

	//{{{ getColumnOnOtherLine() method
	/**
	 * Should be somewhere else...
	 */
	private static int getColumnOnOtherLine(Buffer buffer, int line,
		int col)
	{
		int returnValue = buffer.getOffsetOfVirtualColumn(
			line,col,null);
		if(returnValue == -1)
			return buffer.getLineEndOffset(line) - 1;
		else
			return buffer.getLineStartOffset(line) + returnValue;
	} //}}}

	//}}}
}
