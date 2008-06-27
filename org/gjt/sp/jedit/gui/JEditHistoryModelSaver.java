/*
 * JEditHistoryModelSaver.java - Handles services.xml files in plugins
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Matthieu Casanova
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
package org.gjt.sp.jedit.gui;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.jEdit;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.util.*;

/**
 * @author Matthieu Casanova
 * @version $Id: FoldHandler.java 5568 2006-07-10 20:52:23Z kpouer $
 */
public class JEditHistoryModelSaver implements HistoryModelSaver
{
	//{{{ load() method
	public Map<String, HistoryModel> load(Map<String, HistoryModel> models)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return models;

		history = new File(MiscUtilities.constructPath(
			settingsDirectory,"history"));
		if(!history.exists())
			return models;

		historyModTime = history.lastModified();

		Log.log(Log.MESSAGE,HistoryModel.class,"Loading history");

		if(models == null)
			models = Collections.synchronizedMap(new HashMap<String, HistoryModel>());

		BufferedReader in = null;
		try
		{
			// Try loading with UTF-8 and fallback to the system
			// default encoding to load a history which was made by
			// an old version as well.
			try
			{
				// Pass the decoder explicitly to report a decode error
				// as an exception instead of replacing with \xFFFD.
				in = new BufferedReader(new InputStreamReader(
					new FileInputStream(history),
					Charset.forName("UTF-8").newDecoder()));
				models.putAll(loadFromReader(in));
			}
			catch(CharacterCodingException e)
			{
				// It seems to be made by an old version of jEdit.
				in.close();
				Log.log(Log.MESSAGE,HistoryModel.class,
					"Failed to load history with UTF-8." +
					" Fallbacking to the system default encoding.");

				in = new BufferedReader(new FileReader(history));
				models.putAll(loadFromReader(in));
			}
		}
		catch(FileNotFoundException fnf)
		{
			//Log.log(Log.DEBUG,HistoryModel.class,fnf);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
		finally
		{
			IOUtilities.closeQuietly(in);
		}
		return models;
	} //}}}

	//{{{ save() method
	public boolean save(Map<String, HistoryModel> models)
	{
		Log.log(Log.MESSAGE,HistoryModel.class,"Saving history");
		File file1 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "#history#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "history"));
		if(file2.exists() && file2.lastModified() != historyModTime)
		{
			Log.log(Log.WARNING,HistoryModel.class,file2
				+ " changed on disk; will not save history");
			return false;
		}

		jEdit.backupSettingsFile(file2);

		String lineSep = System.getProperty("line.separator");

		BufferedWriter out = null;

		try
		{
			out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file1), "UTF-8"));

			if(models != null)
			{
				Collection<HistoryModel> values = models.values();
				for (HistoryModel model : values)
				{
					if(model.getSize() == 0)
						continue;

					out.write('[');
					out.write(StandardUtilities.charsToEscapes(
						model.getName(),TO_ESCAPE));
					out.write(']');
					out.write(lineSep);

					for(int i = 0; i < model.getSize(); i++)
					{
						out.write(StandardUtilities.charsToEscapes(
							model.getItem(i),
							TO_ESCAPE));
						out.write(lineSep);
					}
				}
			}

			out.close();

			/* to avoid data loss, only do this if the above
			 * completed successfully */
			file2.delete();
			file1.renameTo(file2);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}

		historyModTime = file2.lastModified();
		return true;
	} //}}}

	//{{{ Private members
	private static final String TO_ESCAPE = "\r\n\t\\\"'[]";
	private static File history;
	private static long historyModTime;

	//{{{ loadFromReader() method
	private static Map<String, HistoryModel> loadFromReader(BufferedReader in)
		throws IOException
	{
		Map<String, HistoryModel> result = new HashMap<String, HistoryModel>();

		HistoryModel currentModel = null;
		String line;

		while((line = in.readLine()) != null)
		{
			if(line.length() > 0 && line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']')
			{
				if(currentModel != null)
				{
					result.put(currentModel.getName(),
						currentModel);
				}

				String modelName = MiscUtilities
					.escapesToChars(line.substring(
					1,line.length() - 1));
				currentModel = new HistoryModel(
					modelName);
			}
			else if(currentModel == null)
			{
				throw new IOException("History data starts"
					+ " before model name");
			}
			else
			{
				currentModel.addElement(MiscUtilities
					.escapesToChars(line));
			}
		}

		if(currentModel != null)
		{
			result.put(currentModel.getName(),currentModel);
		}

		return result;
	} //}}}

	//}}}

}
