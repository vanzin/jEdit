/*
 * ActionListHandler.java - XML handler for action files
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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
import java.io.*;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.util.Log;
//}}}

class ActionListHandler extends DefaultHandler
{
	//{{{ ActionListHandler constructor
	ActionListHandler(String path, ActionSet actionSet)
	{
		this.path = path;
		this.actionSet = actionSet;
		stateStack = new Stack();
		code = new StringBuffer();
		isSelected = new StringBuffer();
	} //}}}

	//{{{ resolveEntity() method
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return MiscUtilities.findEntity(systemId, "actions.dtd", getClass());
	} //}}}

	//{{{ attribute() method
	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();
		value = (value == null) ? null : value.intern();

		if(aname == "NAME")
			actionName = value;
		else if(aname == "NO_REPEAT")
			noRepeat = (value == "TRUE");
		else if(aname == "NO_RECORD")
			noRecord = (value == "TRUE");
		else if(aname == "NO_REMEMBER_LAST")
			noRememberLast = (value == "TRUE");
	} //}}}

	//{{{ characters() method
	public void characters(char[] c, int off, int len)
	{
		String tag = peekElement();
		if (tag.equals("CODE"))
		{
			code.append(c, off, len);
		}
		else if (tag.equals("IS_SELECTED"))
		{
			isSelected.append(c, off, len);
		}
	} //}}}

	//{{{ startElement() method
	public void startElement(String uri, String localName,
				 String qName, Attributes attrs)
	{
		String tag = pushElement(qName);

		if (tag.equals("ACTION"))
		{
			actionName = attrs.getValue("NAME");
			noRepeat = "TRUE".equals(attrs.getValue("NO_REPEAT"));
			noRecord = "TRUE".equals(attrs.getValue("NO_RECORD"));
			noRememberLast = "TRUE".equals(attrs.getValue("NO_REMEMBER_LAST"));
			code.setLength(0);
			isSelected.setLength(0);
		}
	} //}}}

	//{{{ endElement() method
	public void endElement(String uri, String localName, String qName)
	{
		String tag = peekElement();

		if (qName.equals(tag))
		{
			if (tag.equals("ACTION"))
			{
				String selected = (isSelected.length() > 0) ?
					isSelected.toString() : null;
				actionSet.addAction(new BeanShellAction(actionName,
					code.toString(),selected,
					noRepeat,noRecord,noRememberLast));
				noRepeat = noRecord = noRememberLast = false;
				code.setLength(0);
				isSelected.setLength(0);
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
	private String path;
	private ActionSet actionSet;

	private String actionName;
	private StringBuffer code;
	private StringBuffer isSelected;

	private boolean noRepeat;
	private boolean noRecord;
	private boolean noRememberLast;

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
