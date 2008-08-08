/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Kazutoshi Satoda
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
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.util.XMLUtilities;
//}}}

/**
 * A XML file in the settings directory.
 * This class provides some common operations to load/save settings
 * from/into a XML file.
 *   - Proper encoding and XML declaration.
 *   - Two stage save.
 *   - Making backup on each save.
 *   - Detection of change on disk.
 */
public class SettingsXML
{
	//{{{ Saver class
	/**
	 * A Writer to write XML for saving.
	 * The real settings file is not changed until the finish()
	 * method succeeds, in which case the previous settings file is
	 * backuped.
	 */
	public class Saver extends BufferedWriter
	{
		//{{{ writeXMLDeclaration() method
		/**
		 * Write the XML 1.0 declaration.
		 * This should be the first output.
		 */
		public void writeXMLDeclaration() throws IOException
		{
			writeXMLDeclaration("1.0");
		} //}}}

		//{{{ writeXMLDeclaration() method
		/**
		 * Write the XML declaration of a specific version.
		 * This should be the first output.
		 */
		public void writeXMLDeclaration(String version)
			throws IOException
		{
			write("<?xml"
				+ " version=\"" + version + "\""
				+ " encoding=\"" + encoding + "\""
				+ " ?>");
			newLine();
		} //}}}

		//{{{ finish() method
		/**
		 * Perform the final step of saving.
		 */
		public void finish() throws IOException
		{
			close();
			jEdit.backupSettingsFile(file);
			file.delete();
			twoStageSaveFile.renameTo(file);
			knownLastModified = file.lastModified();
		} //}}}

		//{{{ Private members
		private File twoStageSaveFile;
		private static final String encoding = "UTF-8";

		// Only used by SettingsXML#opneSaver().
		Saver() throws IOException
		{
			this(new File(file.getParentFile(),
				"#" + file.getName() + "#save#"));
		}

		// A workaround for a restriction of super().
		private Saver(File twoStageSaveFile) throws IOException
		{
			super(new OutputStreamWriter(
				new FileOutputStream(twoStageSaveFile)
				, encoding));
			this.twoStageSaveFile = twoStageSaveFile;
		}

		//}}}
	} //}}}

	//{{{ Constructor
	/**
	 * Construct a SettingsXML with specific location and name.
	 * @param settingsDirectory
	 * 	The settings directory of jedit
	 * @param name
	 * 	The file name will be (name + ".xml")
	 */
	public SettingsXML(String settingsDirectory, String name)
	{
		String filename = name + ".xml";
		file = new File(MiscUtilities.constructPath(
			settingsDirectory, filename));
	} //}}}
	
	public SettingsXML(File f)
	{
		file = f;
	}

	//{{{ fileExits() method
	/**
	 * Returns true if the file exists.
	 */
	public boolean fileExists()
	{
		return file.exists();
	} //}}}

	//{{{ load() method
	/**
	 * Parse the XML file to load.
	 * @param handler
	 * 	The handler to receive SAX notifications.
	 */
	public void load(DefaultHandler handler) throws IOException
	{
		XMLUtilities.parseXML(new FileInputStream(file), handler);
		knownLastModified = file.lastModified();
	} //}}}

	//{{{ openSaver() method
	/**
	 * Open the file to save in XML.
	 */
	public Saver openSaver() throws IOException
	{
		return new Saver();
	} //}}}

	//{{{ hasChangedOnDisk() method
	/**
	 * Returns true if the file has been changed on disk.
	 * This is based on the last modified time at the last saving
	 * or loading.
	 */
	public boolean hasChangedOnDisk()
	{
		return file.exists()
			&& (file.lastModified() != knownLastModified);
	} //}}}

	//{{{ toString() method
	/**
	 * Returns the file's path.
	 */
	public String toString()
	{
		return file.toString();
	} //}}}

	//{{{ Private members
	private File file;
	private long knownLastModified;
	//}}}
}
