/*
 * HelpIndex.java - Index for help searching feature
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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

package org.gjt.sp.jedit.help;

//{{{ Imports
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

class HelpIndex
{
	//{{{ HelpIndex constructor
	public HelpIndex()
	{
		words = new HashMap<String, Object>();
		files = new ArrayList<HelpFile>();

		ignoreWord("a");
		ignoreWord("an");
		ignoreWord("and");
		ignoreWord("are");
		ignoreWord("as");
		ignoreWord("be");
		ignoreWord("by");
		ignoreWord("can");
		ignoreWord("do");
		ignoreWord("for");
		ignoreWord("from");
		ignoreWord("how");
		ignoreWord("i");
		ignoreWord("if");
		ignoreWord("in");
		ignoreWord("is");
		ignoreWord("it");
		ignoreWord("not");
		ignoreWord("of");
		ignoreWord("on");
		ignoreWord("or");
		ignoreWord("s");
		ignoreWord("that");
		ignoreWord("the");
		ignoreWord("this");
		ignoreWord("to");
		ignoreWord("will");
		ignoreWord("with");
		ignoreWord("you");
	} //}}}

	/* //{{{ HelpIndex constructor
	public HelpIndex(String fileListPath, String wordIndexPath)
	{
		this();
	} //}}} */

	//{{{ indexEditorHelp() method
	/**
	 * Indexes all available help, including the jEdit user's guide, FAQ,]
	 * and plugin documentation.
	 */
	public void indexEditorHelp()
	{
		try
		{
			String jEditHome = jEdit.getJEditHome();
			if(jEditHome != null)
			{
				indexDirectory(MiscUtilities.constructPath(jEditHome,"doc","users-guide"));
				indexDirectory(MiscUtilities.constructPath(jEditHome,"doc","FAQ"));
				indexDirectory(MiscUtilities.constructPath(jEditHome,"doc","whatsnew"));
			}
		}
		catch(Throwable e)
		{
			Log.log(Log.ERROR,this,"Error indexing editor help");
			Log.log(Log.ERROR,this,e);
		}

		PluginJAR[] jars = jEdit.getPluginJARs();
		for (PluginJAR jar : jars)
		{
			try
			{
				indexJAR(jar.getZipFile());
			}
			catch (Throwable e)
			{
				Log.log(Log.ERROR, this, "Error indexing JAR: " + jar.getPath());
				Log.log(Log.ERROR, this, e);
			}
		}

		Log.log(Log.DEBUG,this,"Indexed " + words.size() + " words");
	} //}}}

	//{{{ indexDirectory() method
	/**
	 * Indexes all HTML and text files in the specified directory.
	 * @param dir The directory
	 */
	public void indexDirectory(String dir) throws Exception
	{
		String[] files = VFSManager.getFileVFS()
			._listDirectory(null,dir,"*.{html,txt}",true,null);

		for (String file : files)
			indexURL(file);
	} //}}}

	//{{{ indexJAR() method
	/**
	 * Indexes all HTML and text files in the specified JAR file.
	 * @param jar The JAR file
	 */
	public void indexJAR(ZipFile jar) throws Exception
	{
		Enumeration e = jar.entries();
		while(e.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)e.nextElement();
			String name = entry.getName();
			String lname = name.toLowerCase();
			if(lname.endsWith(".html") || lname.endsWith(".txt") )
			{
				// only works for jEdit plugins
				String url = "jeditresource:/" +
					MiscUtilities.getFileName(jar.getName())
					+ "!/" + name;
				Log.log(Log.DEBUG,this,url);
				indexStream(jar.getInputStream(entry),url);
			}
		}
	} //}}}

	//{{{ indexURL() method
	/**
	 * Reads the specified HTML file and adds all words defined therein to the
	 * index.
	 * @param url The HTML file's URL
	 */
	public void indexURL(String path) throws Exception
	{
		URL url;		
		if (MiscUtilities.isURL(path))
			url = new URL(path);
		else 
		{
			File f = new File(path);
			url = f.toURI().toURL();
		}
		InputStream _in;
		_in =  url.openStream();
		indexStream(_in, url.toString());
	} //}}}

	//{{{ lookupWord() method
	public Word lookupWord(String word)
	{
		Object o = words.get(word);
		if(o == IGNORE)
			return null;
		else
			return (Word)o;
	} //}}}

	//{{{ getFile() method
	public HelpFile getFile(int index)
	{
		return files.get(index);
	} //}}}

	//{{{ Private members
	// used to mark words to ignore (see constructor for the list)
	private static Object IGNORE = new Object();
	private Map<String, Object> words;
	private List<HelpFile> files;

	//{{{ ignoreWord() method
	private void ignoreWord(String word)
	{
		words.put(word,IGNORE);
	} //}}}

	//{{{ indexStream() method
	/**
	 * Reads the specified HTML file and adds all words defined therein to the
	 * index.
	 * @param _in The input stream
	 * @param fileName The file
	 */
	private void indexStream(InputStream _in, String fileName)
		throws Exception
	{
		HelpFile file = new HelpFile(fileName);
		files.add(file);
		int index = files.size() - 1;

		StringBuilder titleText = new StringBuilder();

		BufferedReader in = new BufferedReader(
			new InputStreamReader(_in));

		try
		{
			StringBuilder word = new StringBuilder();
			boolean insideTag = false;
			boolean insideEntity = false;

			boolean title = false;

			int c;
			while((c = in.read()) != -1)
			{
				char ch = (char)c;
				if(insideTag)
				{
					if(ch == '>')
					{
						if(word.toString().equals("title"))
							title = true;
						insideTag = false;
						word.setLength(0);
					}
					else
						word.append(ch);
				}
				else if(insideEntity)
				{
					if(ch == ';')
						insideEntity = false;
				}
				else if(ch == '<')
				{
					if(title)
						title = false;

					if(word.length() != 0)
					{
						addWord(word.toString(),index,title);
						word.setLength(0);
					}

					insideTag = true;
				}
				else if(ch == '&')
					insideEntity = true;
				else if(title)
					titleText.append(ch);
				else if(!Character.isLetterOrDigit(ch))
				{
					if(word.length() != 0)
					{
						addWord(word.toString(),index,title);
						word.setLength(0);
					}
				}
				else
					word.append(ch);
			}
		}
		finally
		{
			in.close();
		}

		if(titleText.length() == 0)
			file.title = fileName;
		else
			file.title = titleText.toString();
	} //}}}

	//{{{ addWord() method
	private void addWord(String word, int file, boolean title)
	{
		word = word.toLowerCase();

		Object o = words.get(word);
		if(o == IGNORE)
			return;

		if(o == null)
			words.put(word,new Word(word,file,title));
		else
			((Word)o).addOccurrence(file,title);
	} //}}}

	//}}}

	//{{{ Word class
	static class Word
	{
		// how much an occurrence in the title is worth
		static final int TITLE_OCCUR = 10;

		// the word
		String word;

		// files it occurs in
		int occurCount = 0;
		Occurrence[] occurrences;

		Word(String word, int file, boolean title)
		{
			this.word = word;
			occurrences = new Occurrence[5];
			addOccurrence(file,title);
		}

		void addOccurrence(int file, boolean title)
		{
			for(int i = 0; i < occurCount; i++)
			{
				if(occurrences[i].file == file)
				{
					occurrences[i].count += (title ? TITLE_OCCUR : 1);
					return;
				}
			}

			if(occurCount >= occurrences.length)
			{
				Occurrence[] newOccur = new Occurrence[occurrences.length * 2];
				System.arraycopy(occurrences,0,newOccur,0,occurCount);
				occurrences = newOccur;
			}

			occurrences[occurCount++] = new Occurrence(file,title);
		}

		static class Occurrence
		{
			int file;
			int count;

			Occurrence(int file, boolean title)
			{
				this.file = file;
				this.count = (title ? TITLE_OCCUR : 1);
			}
		}
	} //}}}

	//{{{ HelpFile class
	static class HelpFile
	{
		String file;
		String title;

		HelpFile(String file)
		{
			this.file = file;
		}

		public String toString()
		{
			return title;
		}

		public boolean equals(Object o)
		{
			if(o instanceof HelpFile)
				return ((HelpFile)o).file.equals(file);
			else
				return false;
		}
	} //}}}
}