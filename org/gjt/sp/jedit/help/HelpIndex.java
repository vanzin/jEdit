/*
 * HelpIndex.java - Index for help searching feature
 * :tabSize=8:indentSize=8:noTabs=false:
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

public class HelpIndex
{
	//{{{ HelpIndex constructor
	public HelpIndex()
	{
		words = new HashMap();
		files = new ArrayList();
	} //}}}

	/* //{{{ HelpIndex constructor
	public HelpIndex(String fileListPath, String wordIndexPath)
	{
		this();
	} //}}} */

	//{{{ indexEditorHelp() method
	/**
	 * Indexes all available help, including the jEdit user's guide, FAQ, and
	 * plugin documentation.
	 */
	public void indexEditorHelp() throws Exception
	{
		String jEditHome = jEdit.getJEditHome();
		if(jEditHome != null)
			indexDirectory(MiscUtilities.constructPath(jEditHome,"doc"));

		EditPlugin.JAR[] jars = jEdit.getPluginJARs();
		for(int i = 0; i < jars.length; i++)
		{
			indexJAR(jars[i].getZipFile());
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

		for(int i = 0; i < files.length; i++)
		{
			indexURL(files[i]);
		}
	} //}}}

	//{{{ indexJAR() method
	/**
	 * Indexes all HTML and text files in the specified JAR file.
	 * @param jar The JAR file
	 */
	public void indexJAR(ZipFile jar) throws Exception
	{
		Enumeration enum = jar.entries();
		while(enum.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)enum.nextElement();
			String name = entry.getName();
			String lname = name.toLowerCase();
			if(lname.endsWith(".html") || lname.endsWith(".txt"))
			{
				// only works for jEdit plugins
				String url = "jeditresource:/" +
					MiscUtilities.getFileName(jar.getName())
					+ "!" + name;
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
	public void indexURL(String url) throws Exception
	{
		InputStream _in;

		if(MiscUtilities.isURL(url))
			_in =  new URL(url).openStream();
		else
		{
			_in = new FileInputStream(url);
			// hack since HelpViewer needs a URL...
			url = "file:" + url;
		}

		indexStream(_in,url);
	} //}}}

	//{{{ lookupWord() method
	public int[] lookupWord(String word)
	{
		Word w = (Word)words.get(word);
		if(w == null)
			return EMPTY_ARRAY;
		else
			return w.files;
	} //}}}

	//{{{ getFile() method
	public HelpFile getFile(int index)
	{
		return (HelpFile)files.get(index);
	} //}}}

	//{{{ Private members
	private static int[] EMPTY_ARRAY = new int[0];
	private HashMap words;
	private ArrayList files;

	//{{{ indexStream() method
	/**
	 * Reads the specified HTML file and adds all words defined therein to the
	 * index.
	 * @param _in The input stream
	 * @param file The file
	 */
	private void indexStream(InputStream _in, String fileName) throws Exception
	{
		HelpFile file = new HelpFile(fileName);
		files.add(file);
		int index = files.size() - 1;

		BufferedReader in = new BufferedReader(new InputStreamReader(_in));

		StringBuffer titleText = new StringBuffer();

		try
		{
			StringBuffer word = new StringBuffer();
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

					word.setLength(0);
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
						addWord(word.toString(),index);
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
	private void addWord(String word, int file)
	{
		word = word.toLowerCase();

		Word w = (Word)words.get(word);
		if(w == null)
			words.put(word,new Word(word,file));
		else
			w.addOccurrence(file);
	} //}}}

	//}}}

	//{{{ Word class
	public static class Word
	{
		// the word
		String word;

		// files it occurs in
		int fileCount = 0;
		int[] files;

		Word(String word, int file)
		{
			this.word = word;
			files = new int[5];
			addOccurrence(file);
		}

		void addOccurrence(int file)
		{
			for(int i = 0; i < fileCount; i++)
			{
				if(files[i] == file)
					return;
			}

			if(fileCount >= files.length)
			{
				int[] newFiles = new int[files.length * 2];
				System.arraycopy(files,0,newFiles,0,fileCount);
				files = newFiles;
			}

			files[fileCount++] = file;
		}

		public String toString()
		{
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < fileCount; i++)
			{
				if(i != 0)
					buf.append(",");
				buf.append(files[i]);
			}
			return buf.toString();
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
			this.title = title;
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
