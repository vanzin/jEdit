/*
 * ModeCatalogHandler.java - XML handler for mode catalog files
 * Copyright (C) 2000, 2001 Slava Pestov
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
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
//}}}

/**
 * @author Slava Pestov
 */
class ModeCatalogHandler extends DefaultHandler
{
	//{{{ ModeCatalogHandler constructor
	ModeCatalogHandler(String directory, boolean resource)
	{
		this.directory = directory;
		this.resource = resource;
	} //}}}

	//{{{ resolveEntity() method
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return XMLUtilities.findEntity(systemId, "catalog.dtd", getClass());
	} //}}}

	//{{{ startElement() method
	public void startElement(String uri, String localName,
							 String qName, Attributes attrs)
	{
		if (qName.equals("MODE"))
		{
			String modeName = attrs.getValue("NAME");

			String file = attrs.getValue("FILE");
			if(file == null)
			{
				Log.log(Log.ERROR,this,directory + "catalog:"
					+ " mode " + modeName + " doesn't have"
					+ " a FILE attribute");
			}

			String filenameGlob = attrs.getValue("FILE_NAME_GLOB");
			String firstlineGlob = attrs.getValue("FIRST_LINE_GLOB");


			Mode mode = ModeProvider.instance.getMode(modeName);
			if (mode == null)
			{
			    mode = instantiateMode(modeName);
			}
			ModeProvider.instance.addMode(mode);
			
			Object path;
			if(resource)
				path = jEdit.class.getResource(directory + file);
			else
				path = MiscUtilities.constructPath(directory,file);
			mode.setProperty("file",path);

			mode.unsetProperty("filenameGlob");
			if(filenameGlob != null)
			    mode.setProperty("filenameGlob",filenameGlob);
			    
			mode.unsetProperty("firstlineGlob");
			if(firstlineGlob != null)
			    mode.setProperty("firstlineGlob",firstlineGlob);
			    
			mode.init();
		}
	} //}}}

	protected Mode instantiateMode(String modeName)
	{
		return new Mode(modeName);
	}

	private String directory;
	private boolean resource;

}

