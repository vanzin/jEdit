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
	} //}}}

	//{{{ HelpIndex constructor
	public HelpIndex(String fileListPath, String wordIndexPath)
	{
		this();
	} //}}}

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
	public String[] lookupWord(String word)
	{
		Word w = (Word)words.get(word);
		if(w == null)
			return EMPTY_ARRAY;
		else
			return w.files;
	} //}}}

	//{{{ Private members
	private static String[] EMPTY_ARRAY = new String[0];
	private HashMap words;

	//{{{ indexStream() method
	/**
	 * Reads the specified HTML file and adds all words defined therein to the
	 * index.
	 * @param _in The input stream
	 * @param file The file
	 */
	private void indexStream(InputStream _in, String file) throws Exception
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(_in));

		try
		{
			StringBuffer word = new StringBuffer();
			boolean insideTag = false;
			boolean insideEntity = false;

			int c;
			while((c = in.read()) != -1)
			{
				char ch = (char)c;
				if(insideTag)
				{
					if(ch == '>')
						insideTag = false;
				}
				else if(insideEntity)
				{
					if(ch == ';')
						insideEntity = false;
				}
				else if(ch == '<')
					insideTag = true;
				else if(ch == '&')
					insideEntity = true;
				else if(!Character.isLetterOrDigit(ch))
				{
					if(word.length() != 0)
					{
						addWord(word.toString(),file);
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
	} //}}}

	//{{{ addWord() method
	private void addWord(String word, String file)
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
		String[] files;

		Word(String word, String file)
		{
			this.word = word;
			files = new String[5];
			addOccurrence(file);
		}

		void addOccurrence(String file)
		{
			if(file == null)
				throw new NullPointerException();

			for(int i = 0; i < fileCount; i++)
			{
				if(files[i] == file)
					return;
			}

			if(fileCount >= files.length)
			{
				String[] newFiles = new String[files.length * 2];
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
}
