/*
 * PerspectiveManager.java - Saves view configuration
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

import com.microstar.xml.*;
import java.io.*;
import org.gjt.sp.util.Log;

class PerspectiveManager
{
	//{{{ loadPerspective() method
	static View loadPerspective(boolean restoreFiles)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return null;

		File perspective = new File(MiscUtilities.constructPath(
			settingsDirectory,"perspective.xml"));

		if(!perspective.exists())
			return null;

		Log.log(Log.MESSAGE,PerspectiveManager.class,"Loading " + perspective);

		PerspectiveHandler handler = new PerspectiveHandler(restoreFiles);
		XmlParser parser = new XmlParser();
		parser.setHandler(handler);
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(perspective));
			parser.parse(null, null, in);
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,PerspectiveManager.class,perspective
				+ ":" + line + ": " + message);
		}
		catch(FileNotFoundException fnf)
		{
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,PerspectiveManager.class,e);
		}

		return handler.view;
	} //}}}

	//{{{ savePerspective() method
	static void savePerspective()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		// backgrounded
		if(jEdit.getBufferCount() == 0)
			return;

		File perspective = new File(MiscUtilities.constructPath(
			settingsDirectory,"perspective.xml"));

		Log.log(Log.MESSAGE,PerspectiveManager.class,"Saving " + perspective);

		try
		{
			String lineSep = System.getProperty("line.separator");

			BufferedWriter out = new BufferedWriter(new FileWriter(
				perspective));

			out.write("<?xml version=\"1.0\"?>");
			out.write(lineSep);
			out.write("<!DOCTYPE PERSPECTIVE SYSTEM \"perspective.dtd\">");
			out.write(lineSep);
			out.write("<PERSPECTIVE>");
			out.write(lineSep);

			Buffer[] buffers = jEdit.getBuffers();
			for(int i = 0; i < buffers.length; i++)
			{
				Buffer buffer = buffers[i];
				out.write("<BUFFER>");
				out.write(MiscUtilities.charsToEntities(buffer.getPath()));
				out.write("</BUFFER>");
				out.write(lineSep);
			}

			View[] views = jEdit.getViews();
			for(int i = 0; i < views.length; i++)
			{
				View.ViewConfig config = views[i].getViewConfig();
				out.write("<VIEW PLAIN=\"");
				out.write(config.plainView ? "TRUE" : "FALSE");
				out.write("\" X=\"");
				out.write(String.valueOf(config.x));
				out.write("\" Y=\"");
				out.write(String.valueOf(config.y));
				out.write("\" WIDTH=\"");
				out.write(String.valueOf(config.width));
				out.write("\" HEIGHT=\"");
				out.write(String.valueOf(config.height));
				out.write("\" EXT_STATE=\"");
				out.write(String.valueOf(config.extState));
				out.write("\">");

				out.write("<PANES>");
				out.write(lineSep);
				out.write(config.splitConfig);
				out.write(lineSep);
				out.write("</PANES>");
				out.write(lineSep);

				out.write("</VIEW>");
				out.write(lineSep);
			}

			out.write("</PERSPECTIVE>");
			out.write(lineSep);
			out.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,PerspectiveManager.class,"Error saving " + perspective);
			Log.log(Log.ERROR,PerspectiveManager.class,io);
		}
	} //}}}

	//{{{ PerspectiveHandler class
	static class PerspectiveHandler extends HandlerBase
	{
		View view;
		String charData;
		View.ViewConfig config;
		boolean restoreFiles;

		PerspectiveHandler(boolean restoreFiles)
		{
			this.restoreFiles = restoreFiles;
			config = new View.ViewConfig();
		}

		public Object resolveEntity(String publicId, String systemId)
		{
			if("perspective.dtd".equals(systemId))
			{
				// this will result in a slight speed up, since we
				// don't need to read the DTD anyway, as AElfred is
				// non-validating
				return new StringReader("<!-- -->");

				/* try
				{
					return new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("recent.dtd")));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,"Error while opening"
						+ " recent.dtd:");
					Log.log(Log.ERROR,this,e);
				} */
			}

			return null;
		}

		public void doctypeDecl(String name, String publicId,
			String systemId) throws Exception
		{
			if("PERSPECTIVE".equals(name))
				return;

			Log.log(Log.ERROR,this,"perspective.xml: DOCTYPE must be PERSPECTIVE");
		}

		public void attribute(String aname, String value, boolean specified)
		{
			if(!specified)
				return;

			if(aname.equals("X"))
				config.x = Integer.parseInt(value);
			if(aname.equals("Y"))
				config.y = Integer.parseInt(value);
			if(aname.equals("WIDTH"))
				config.width = Integer.parseInt(value);
			if(aname.equals("HEIGHT"))
				config.height = Integer.parseInt(value);
			if(aname.equals("EXT_STATE"))
				config.extState = Integer.parseInt(value);
			if(aname.equals("PLAIN"))
				config.plainView = ("TRUE".equals(value));
		}

		public void endElement(String name)
		{
			if(name.equals("BUFFER"))
			{
				if(restoreFiles)
					jEdit.openFile(null,charData);
			}
			else if(name.equals("PANES"))
				config.splitConfig = charData;
			else if(name.equals("VIEW"))
			{
				if(jEdit.getBufferCount() == 0)
					jEdit.newFile(null);
				view = jEdit.newView(view,config);
				config = new View.ViewConfig();
			}
		}

		public void charData(char[] ch, int start, int length)
		{
			charData = new String(ch,start,length);
		}
	} //}}}
}
