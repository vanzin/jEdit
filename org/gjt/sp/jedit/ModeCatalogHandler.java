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

import com.microstar.xml.*;
import java.io.*;
import org.gjt.sp.util.Log;

class ModeCatalogHandler extends HandlerBase
{
	ModeCatalogHandler(String directory, boolean resource)
	{
		this.directory = directory;
		this.resource = resource;
	}

	public Object resolveEntity(String publicId, String systemId)
	{
		if("catalog.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");

			/* try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("catalog.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " catalog.dtd:");
				Log.log(Log.ERROR,this,e);
			} */
		}

		return null;
	}

	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();

		if(aname == "NAME")
			modeName = value;
		else if(aname == "FILE")
		{
			if(value == null)
			{
				Log.log(Log.ERROR,this,directory + "catalog:"
					+ " mode " + modeName + " doesn't have"
					+ " a FILE attribute");
			}
			else
				file = value;
		}
		else if(aname == "FILE_NAME_GLOB")
			filenameGlob = value;
		else if(aname == "FIRST_LINE_GLOB")
			firstlineGlob = value;
	}

	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		// older jEdit versions used a DOCTYPE of CATALOG, which
		// is incorrect since the DOCTYPE must be the name of the
		// root element, which is MODES.

		// so you the avid code reader should use MODES as the
		// DOCTYPE instead, but we still let old catalogs through
		// to avoid annoying users.
		if("CATALOG".equals(name) || "MODES".equals(name))
			return;

		Log.log(Log.ERROR,this,directory + "catalog: DOCTYPE must be CATALOG");
	}

	public void endElement(String name)
	{
		if(name.equals("MODE"))
		{
			Mode mode = jEdit.getMode(modeName);
			if(mode == null)
			{
				mode = new Mode(modeName);
				jEdit.addMode(mode);
			}

			Object path;
			if(resource)
				path = jEdit.class.getResource(directory + file);
			else
				path = MiscUtilities.constructPath(directory,file);
			mode.setProperty("file",path);

			if(filenameGlob != null)
				mode.setProperty("filenameGlob",filenameGlob);
			else
				mode.unsetProperty("filenameGlob");

			if(firstlineGlob != null)
				mode.setProperty("firstlineGlob",firstlineGlob);
			else
				mode.unsetProperty("firstlineGlob");

			mode.init();

			modeName = file = filenameGlob = firstlineGlob = null;
		}
	}

	// end HandlerBase implementation

	// private members
	private String directory;
	private boolean resource;

	private String modeName;
	private String file;
	private String filenameGlob;
	private String firstlineGlob;
}
