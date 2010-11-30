/*
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
package org.gjt.sp.jedit;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.IOUtilities;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;

import java.io.IOException;

/**
 * The concrete RegisterSaver for jEdit.
 * @author Matthieu Casanova
 * @version $Id: FoldHandler.java 5568 2006-07-10 20:52:23Z kpouer $
 */
class JEditRegisterSaver implements RegisterSaver
{
	//{{{ Constructor
	JEditRegisterSaver()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			registersXML = new SettingsXML(settingsDirectory, "registers");
		}
	} //}}}

	//{{{ loadRegisters() method
	public void loadRegisters()
	{
		if(registersXML == null)
			return;

		if(!registersXML.fileExists())
			return;

		Log.log(Log.MESSAGE,jEdit.class,"Loading " + registersXML);

		RegistersHandler handler = new RegistersHandler();
		try
		{
			Registers.setLoading(true);
			registersXML.load(handler);
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, Registers.class, ioe);
		}
		finally
		{
			Registers.setLoading(false);
		}
	} //}}}

	//{{{ saveRegisters() method
	public void saveRegisters()
	{
		if(registersXML == null)
			return;

		if(registersXML.hasChangedOnDisk())
		{
			Log.log(Log.WARNING,Registers.class,registersXML
				+ " changed on disk; will not save registers");
			return;
		}

		Log.log(Log.MESSAGE,Registers.class,"Saving " + registersXML);

		String lineSep = System.getProperty("line.separator");

		SettingsXML.Saver out = null;

		try
		{
			out = registersXML.openSaver();
			out.writeXMLDeclaration();

			out.write("<!DOCTYPE REGISTERS SYSTEM \"registers.dtd\">");
			out.write(lineSep);
			out.write("<REGISTERS>");
			out.write(lineSep);

			Registers.Register[] registers = Registers.getRegisters();
			for(int i = 0; i < registers.length; i++)
			{
				Registers.Register register = registers[i];
				if(register == null ||
				   i == '$' ||
				   i == '%' ||
				   register.toString().length() == 0)
					continue;

				out.write("<REGISTER NAME=\"");
				if(i == '"')
					out.write("&quot;");
				else
					out.write((char)i);
				out.write("\">");

				out.write(XMLUtilities.charsToEntities(
					register.toString(), false));

				out.write("</REGISTER>");
				out.write(lineSep);
			}

			out.write("</REGISTERS>");
			out.write(lineSep);

			out.finish();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,Registers.class,e);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
		}
	} //}}}

	//{{{ Private members
	private SettingsXML registersXML;

	//{{{ RegistersHandler class
	private static class RegistersHandler extends DefaultHandler
	{
		//{{{ resolveEntity() method
		@Override
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "registers.dtd", getClass());
		} //}}}

		//{{{ startElement() method
		@Override
		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			registerName = attrs.getValue("NAME");
			inRegister = "REGISTER".equals(qName);
		} //}}}

		//{{{ endElement() method
		@Override
		public void endElement(String uri, String localName, String name)
		{
			if("REGISTER".equals(name))
			{
				if(registerName == null || registerName.length() != 1)
					Log.log(Log.ERROR,this,"Malformed NAME: " + registerName);
				else
					Registers.setRegister(registerName.charAt(0),charData.toString());
				inRegister = false;
				charData.setLength(0);
			}
		} //}}}

		//{{{ characters() method
		@Override
		public void characters(char[] ch, int start, int length)
		{
			if (inRegister)
				charData.append(ch, start, length);
		} //}}}

		//{{{ Private members
		private String registerName;
		private final StringBuilder charData = new StringBuilder();
		private boolean inRegister;
		//}}}
	} //}}}
	//}}}
}
