/*
 * ServiceManager.java - Handles services.xml files in plugins
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
import java.net.URL;
import java.util.Stack;
import org.gjt.sp.util.Log;

/**
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
class ServiceListHandler extends HandlerBase
{
	//{{{ ServiceListHandler constructor
	ServiceListHandler(URL uri, EditPlugin.JAR plugin)
	{
		this.uri = uri;
		this.plugin = plugin;
		stateStack = new Stack();
	} //}}}

	//{{{ resolveEntity() method
	public Object resolveEntity(String publicId, String systemId)
	{
		if("services.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");

			/* try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream
					("/org/gjt/sp/jedit/services.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " dockables.dtd:");
				Log.log(Log.ERROR,this,e);
			} */
		}

		return null;
	} //}}}

	//{{{ attribute() method
	public void attribute(String aname, String value, boolean isSpecified)
	{
		if(aname.equals("NAME"))
			serviceName = value;
		else if(aname.equals("CLASS"))
			serviceClass = value;
	} //}}}

	//{{{ doctypeDecl() method
	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("SERVICES".equals(name))
			return;

		Log.log(Log.ERROR,this,uri + ": DOCTYPE must be SERVICES");
	} //}}}

	//{{{ charData() method
	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if (tag == "SERVICE")
		{
			code = text;
		}
	} //}}}

	//{{{ startElement() method
	public void startElement(String tag)
	{
		tag = pushElement(tag);
	} //}}}

	//{{{ endElement() method
	public void endElement(String name)
	{
		if(name == null)
			return;

		String tag = peekElement();

		if(name.equals(tag))
		{
			if(tag == "SERVICE")
			{
				ServiceManager.registerService(serviceClass,
					serviceName,code,plugin);
			}

			popElement();
		}
		else
		{
			// can't happen
			throw new InternalError();
		}
	} //}}}

	//{{{ startDocument() method
	public void startDocument()
	{
		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private URL uri;
	private EditPlugin.JAR plugin;

	private String serviceName;
	private String serviceClass;
	private String code;

	private Stack stateStack;
	//}}}

	//{{{ pushElement() method
	private String pushElement(String name)
	{
		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	} //}}}

	//{{{ peekElement() method
	private String peekElement()
	{
		return (String) stateStack.peek();
	} //}}}

	//{{{ popElement() method
	private String popElement()
	{
		return (String) stateStack.pop();
	} //}}}

	//}}}
}
