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

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Manages persistence of open buffers and views across jEdit sessions.
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
public class PerspectiveManager
{
	private static final String PERSPECTIVE_FILENAME = "perspective";

	//{{{ isPerspectiveDirty() method
	/**
	 * We only autosave the perspective if it has changed, to avoid spinning
	 * up the disk on laptops.
	 * @since jEdit 4.2pre13
	 */
	public static boolean isPerspectiveDirty()
	{
		return dirty;
	} //}}}

	//{{{ setPerspectiveDirty() method
	/**
	 * We only autosave the perspective if it has changed, to avoid spinning
	 * up the disk on laptops.
	 * @since jEdit 4.2pre13
	 */
	public static void setPerspectiveDirty(boolean dirty)
	{
		PerspectiveManager.dirty = dirty;
	} //}}}

	//{{{ isPerspectiveEnabled() method
	/**
	 * We disable saving of the perspective while the 'close all' dialog is
	 * showing.
	 * @since jEdit 4.3pre2
	 */
	public static boolean isPerspectiveEnabled()
	{
		return enabled;
	} //}}}

	//{{{ setPerspectiveEnabled() method
	/**
	 * We disable saving of the perspective while the 'close all' dialog is
	 * showing.
	 * @since jEdit 4.3pre2
	 */
	public static void setPerspectiveEnabled(boolean enabled)
	{
		PerspectiveManager.enabled = enabled;
	} //}}}

	//{{{ loadPerspective() method
	public static View loadPerspective(boolean restoreFiles)
	{
		if(perspectiveXML == null)
			return null;

		if(!perspectiveXML.fileExists())
			return null;

		Log.log(Log.MESSAGE,PerspectiveManager.class,"Loading " + perspectiveXML);

		PerspectiveHandler handler = new PerspectiveHandler(restoreFiles);
		try
		{
			perspectiveXML.load(handler);
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,PerspectiveManager.class,e);
		}
		return handler.view;
	} //}}}

	//{{{ savePerspective() method
	public static void savePerspective(boolean autosave)
	{
		if(!isPerspectiveEnabled() || !jEdit.isStartupDone())
			return;

		if(perspectiveXML == null)
			return;
		
		// backgrounded
		if(jEdit.getBufferCount() == 0)
			return;

		Buffer[] buffers = jEdit.getBuffers();
		Collection<Buffer> savedBuffers = new LinkedList<Buffer>();
		for (Buffer buffer: buffers)
		{
			if (!buffer.isNewFile())
			{
				savedBuffers.add(buffer);
			}
		}

		if(!autosave)
			Log.log(Log.MESSAGE,PerspectiveManager.class,"Saving " + perspectiveXML);

		String lineSep = System.getProperty("line.separator");

		SettingsXML.Saver out = null;

		try
		{
			out = perspectiveXML.openSaver();
			out.writeXMLDeclaration();

			out.write("<!DOCTYPE PERSPECTIVE SYSTEM \"perspective.dtd\">");
			out.write(lineSep);
			out.write("<PERSPECTIVE>");
			out.write(lineSep);

			for (Buffer buffer: savedBuffers)
			{
				out.write("<BUFFER AUTORELOAD=\"");
				out.write(buffer.getAutoReload() ? "TRUE" : "FALSE");
				out.write("\" AUTORELOAD_DIALOG=\"");
				out.write(buffer.getAutoReloadDialog() ? "TRUE" : "FALSE");
				out.write("\">");
				out.write(XMLUtilities.charsToEntities(buffer.getPath(), false));
				out.write("</BUFFER>");
				out.write(lineSep);
			}

			View[] views = jEdit.getViews();
			for(int i = 0; i < views.length; i++)
			{
				View view = views[i];
				// ensures that active view is saved last,
				// ie created last on next load, ie in front
				// on next load
				if(view == jEdit.getActiveView()
					&& i != views.length - 1)
				{
					View last = views[views.length - 1];
					views[i] = last;
					views[views.length - 1] = view;
					view = last;
				}

				View.ViewConfig config = views[i].getViewConfig();
				out.write("<VIEW PLAIN=\"");
				out.write(config.plainView ? "TRUE" : "FALSE");
				out.write("\">");
				out.write(lineSep);
				
				if (config.title != null)
				{
					out.write(lineSep);
					out.write("<TITLE>");
					out.write(XMLUtilities.charsToEntities(config.title,false));
					out.write("</TITLE>");
					out.write(lineSep);
				}

				out.write("<PANES>");
				out.write(lineSep);
				out.write(XMLUtilities.charsToEntities(
					config.splitConfig,false));
				out.write(lineSep);
				out.write("</PANES>");
				out.write(lineSep);

				out.write("<GEOMETRY X=\"");
				out.write(String.valueOf(config.x));
				out.write("\" Y=\"");
				out.write(String.valueOf(config.y));
				out.write("\" WIDTH=\"");
				out.write(String.valueOf(config.width));
				out.write("\" HEIGHT=\"");
				out.write(String.valueOf(config.height));
				out.write("\" EXT_STATE=\"");
				out.write(String.valueOf(config.extState));
				out.write("\" />");
				out.write(lineSep);

				if (config.docking != null)
					config.docking.saveLayout(PERSPECTIVE_FILENAME, i);
				
				out.write("</VIEW>");
				out.write(lineSep);
			}

			out.write("</PERSPECTIVE>");
			out.write(lineSep);

			out.finish();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,PerspectiveManager.class,"Error saving " + perspectiveXML);
			Log.log(Log.ERROR,PerspectiveManager.class,io);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}
	} //}}}

	//{{{ Private members
	private static boolean dirty, enabled = true;
	private static SettingsXML perspectiveXML;

	//{{{ Class initializer
	static
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			perspectiveXML = new SettingsXML(settingsDirectory, PERSPECTIVE_FILENAME);
		}
	} //}}}

	//{{{ PerspectiveHandler class
	private static class PerspectiveHandler extends DefaultHandler
	{
		View view;
		private StringBuilder charData;
		View.ViewConfig config;
		boolean restoreFiles;
		boolean restoreSplits;
		String autoReload, autoReloadDialog;
		
		PerspectiveHandler(boolean restoreFiles)
		{
			this.restoreFiles = restoreFiles;
			restoreSplits = jEdit.getBooleanProperty("restore.splits", true);
			config = new View.ViewConfig();
			charData = new StringBuilder();
			config.docking = View.getDockingFrameworkProvider().createDockingLayout();
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "perspective.dtd", getClass());
		}

		@Override
		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			charData.setLength(0);
			for (int i = 0; i < attrs.getLength(); i++)
			{
				String name = attrs.getQName(i);
				String value = attrs.getValue(i);
				attribute(name, value);
			}
		}

		private void attribute(String aname, String value)
		{
			if(aname.equals("X"))
				config.x = Integer.parseInt(value);
			else if(aname.equals("Y"))
				config.y = Integer.parseInt(value);
			else if(aname.equals("WIDTH"))
				config.width = Integer.parseInt(value);
			else if(aname.equals("HEIGHT"))
				config.height = Integer.parseInt(value);
			else if(aname.equals("EXT_STATE"))
				config.extState = Integer.parseInt(value);
			else if(aname.equals("PLAIN"))
				config.plainView = ("TRUE".equals(value));
			else if(aname.equals("AUTORELOAD"))
				autoReload = value;
			else if(aname.equals("AUTORELOAD_DIALOG"))
				autoReloadDialog = value;
		}

		/**
		 * @return true if the uri points to a remote file
		 */
		public static boolean skipRemote(String uri)
		{
			if (jEdit.getBooleanProperty("restore.remote"))
				return false;
			if(MiscUtilities.isURL(uri))
			{
				String protocol = MiscUtilities.getProtocolOfURL(uri);
				if (!protocol.equals("file")) return true;
			}
			return false;
		}

		@Override
		public void endElement(String uri, String localName, String name)
		{
			if(name.equals("BUFFER"))
			{
				if (restoreFiles && !skipRemote(charData.toString()))
				{
					Buffer restored = jEdit.openTemporary(null,null, charData.toString(), false);
					// if the autoReload attributes are not present, don't set anything
					// it's sufficient to check whether they are present on the first BUFFER element
					if (restored != null)
					{
						if(autoReload != null)
							restored.setAutoReload("TRUE".equals(autoReload));
						if(autoReloadDialog != null)
							restored.setAutoReloadDialog("TRUE".equals(autoReloadDialog));
						jEdit.commitTemporary(restored);
					}
				}
			}
			else if(name.equals("PANES"))
			{
				if (restoreFiles && restoreSplits)
				{
					config.splitConfig = charData.toString();
				}
				else if (restoreFiles)
				{
					config.splitConfig = removeSplits(charData.toString());	
				}
				else if (restoreSplits)
				{
					config.splitConfig = removeFiles(charData.toString());
				}
				else
				{
					config.splitConfig = null;	
				}
				if (config.splitConfig != null && !jEdit.getBooleanProperty("restore.remote"))
				{
					config.splitConfig = removeRemotes(config.splitConfig);	
				}
			}
			else if(name.equals("VIEW"))
			{
				if (config.docking != null)
					config.docking.loadLayout(PERSPECTIVE_FILENAME, jEdit.getViewCount());
				view = jEdit.newView(view,null,config);
				config = new View.ViewConfig();
				config.docking = View.getDockingFrameworkProvider().createDockingLayout();
			}
			else if(name.equals("TITLE"))
				config.title = charData.toString();
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			charData.append(ch,start,length);
		}
		
		//{{{ removeFiles
		// removes filenames from the split config string.  This is used
		// when the user has options set to not restore files but do 
		// restore the split configuration. This includes removing remote
		// files since there is no option setting to allow only restoring
		// remote files.
		private String removeFiles(String splitConfig) 
		{
			StringBuilder sb = new StringBuilder();
			Stack<Object> stack = new Stack<Object>();
	
			try {
				StreamTokenizer st = new StreamTokenizer(new StringReader(splitConfig));
				st.whitespaceChars(0,' ');
				st.wordChars('#','~');
				st.commentChar('!');
				st.quoteChar('"');
				st.eolIsSignificant(false);
				
				int token = st.nextToken();
				
				while (token != StreamTokenizer.TT_EOF)
				{
					switch(token)
					{
						case StreamTokenizer.TT_WORD:
							if(st.sval.equals("vertical") || st.sval.equals("horizontal"))
							{
								Object number = stack.pop();
								sb.append(number.toString());
								sb.append(' ');
								sb.append(st.sval);
								sb.append(' ');
							}
							else if(st.sval.equals("buffer"))
							{
								stack.pop();	// pop filename and discard it
								sb.append("\"Untitled-1\" ");
								sb.append(st.sval);
								sb.append(' ');
							}
							else if(st.sval.equals("buff"))
							{
								stack.pop();	// pop filename and discard it
							}
							else if (st.sval.equals("bufferset"))
							{
								Object scope = stack.pop();
								sb.append('\"');
								sb.append(scope.toString());
								sb.append("\" ");
								sb.append(st.sval);
								sb.append(' ');
							}
							break;
						case StreamTokenizer.TT_NUMBER:
							stack.push((int)st.nval);
							break;
						case '"':
							stack.push(st.sval);
							break;
					}
					token = st.nextToken();
				}
				return sb.toString();
		    }
		    catch(Exception e) {	// NOPMD
		    }
		    return splitConfig;
		}
		//}}}
		
		//{{{ removeSplits
		// removes split configuration from the split configuration string.
		// This is used when the user has options set to restore files but
		// to not restore the split configuration.
		private String removeSplits(String splitConfig)
		{
			StringBuilder sb = new StringBuilder();
			Stack<Object> stack = new Stack<Object>();
	
			try {
				StreamTokenizer st = new StreamTokenizer(new StringReader(splitConfig));
				st.whitespaceChars(0,' ');
				st.wordChars('#','~');
				st.commentChar('!');
				st.quoteChar('"');
				st.eolIsSignificant(false);
				
				int token = st.nextToken();
				boolean haveBuffer = false;
				boolean haveBufferSet = false;
				String bufferSetScope = "";
				
				while (token != StreamTokenizer.TT_EOF)
				{
					switch(token)
					{
						case StreamTokenizer.TT_WORD:
							if(st.sval.equals("vertical") || st.sval.equals("horizontal"))
							{
								stack.pop();     // pop divider location and discard it
							}
							else if(st.sval.equals("buffer"))
							{
								Object filename = stack.pop();
								sb.append('\"');
								sb.append(filename.toString());
								sb.append("\" ");
								sb.append(!haveBuffer ? "buffer" : "buff");
								sb.append(' ');
								haveBuffer = true;
							}
							else if(st.sval.equals("buff"))
							{
								Object filename = stack.pop();
								sb.append('\"');
								sb.append(filename.toString());
								sb.append("\" ");
								sb.append(st.sval);
								sb.append(' ');
							}
							else if (st.sval.equals("bufferset"))
							{
								Object scope = stack.pop();
								bufferSetScope = scope.toString();
								haveBufferSet = true;
							}
							break;
						case StreamTokenizer.TT_NUMBER:
							stack.push((int)st.nval);
							break;
						case '"':
							stack.push(st.sval);
							break;
					}
					token = st.nextToken();
				}
				if (haveBufferSet) 
				{
				     sb.append('"');
				     sb.append(bufferSetScope);
				     sb.append("\" bufferset");
				}
				return sb.toString();
		    }
		    catch(Exception e) {	// NOPMD 
		    }
		    return splitConfig;
		}
		//}}}
		
		//{{{ removeRemotes
		// removes file entries that point to a remote file.
		private String removeRemotes(String splitConfig) 
		{
			StringBuilder sb = new StringBuilder();
			Stack<Object> stack = new Stack<Object>();
	
			try {
				StreamTokenizer st = new StreamTokenizer(new StringReader(splitConfig));
				st.whitespaceChars(0,' ');
				st.wordChars('#','~');
				st.commentChar('!');
				st.quoteChar('"');
				st.eolIsSignificant(false);
				
				boolean haveBuffer = false;
				int token = st.nextToken();
				
				while (token != StreamTokenizer.TT_EOF)
				{
					switch(token)
					{
						case StreamTokenizer.TT_WORD:
							if(st.sval.equals("vertical") || st.sval.equals("horizontal"))
							{
								Object number = stack.pop();
								sb.append(number.toString());
								sb.append(' ');
								sb.append(st.sval);
								sb.append(' ');
							}
							else if(st.sval.equals("buffer") || st.sval.equals("buff"))
							{
								Object filename = stack.pop();
								if (!skipRemote(filename.toString()))
								{
									sb.append('"').append(filename.toString()).append("\" ");
									if (!haveBuffer)
									{
										sb.append("buffer");
										haveBuffer = true;
									}
									else {
										sb.append("buff");	
									}
									sb.append(' ');
								}
							}
							else if (st.sval.equals("bufferset"))
							{
								Object scope = stack.pop();
								sb.append('\"');
								sb.append(scope.toString());
								sb.append("\" ");
								sb.append(st.sval);
								sb.append(' ');
								haveBuffer = false;
							}
							break;
						case StreamTokenizer.TT_NUMBER:
							stack.push((int)st.nval);
							break;
						case '"':
							stack.push(st.sval);
							break;
					}
					token = st.nextToken();
				}
				return sb.toString();
		    }
		    catch(Exception e) {	// NOPMD
		    	    e.printStackTrace();
		    }
		    return splitConfig;
		}
		//}}}
	} //}}}

	//}}}
}
