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
import com.microstar.xml.*;
import java.io.*;
import java.util.Stack;
import org.gjt.sp.util.Log;
//}}}

class ActionListHandler extends HandlerBase
{
	//{{{ ActionListHandler constructor
	ActionListHandler(String path, ActionSet actionSet)
	{
		this.path = path;
		this.actionSet = actionSet;
		stateStack = new Stack();
	} //}}}

	//{{{ resolveEntity() method
	public Object resolveEntity(String publicId, String systemId)
	{
		if("actions.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");

			/* try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("actions.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " actions.dtd:");
				Log.log(Log.ERROR,this,e);
			} */
		}

		return null;
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

	//{{{ doctypeDecl() method
	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("ACTIONS".equals(name))
			return;

		Log.log(Log.ERROR,this,path + ": DOCTYPE must be ACTIONS");
	} //}}}

	//{{{ charData() method
	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if (tag == "CODE")
		{
			code = text;
		}
		else if (tag == "IS_SELECTED")
		{
			isSelected = text;
		}
	} //}}}

	//{{{ startElement() method
	public void startElement(String tag)
	{
		tag = pushElement(tag);

		if (tag == "ACTION")
		{
			code = null;
			isSelected = null;
		}
	} //}}}

	//{{{ endElement() method
	public void endElement(String name)
	{
		if(name == null)
			return;

		String tag = peekElement();

		if(name.equals(tag))
		{
			if(tag == "ACTION")
			{
				actionSet.addAction(new BeanShellAction(actionName,
					code,isSelected,noRepeat,noRecord,
					noRememberLast));
				noRepeat = noRecord = noRememberLast = false;
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
	private String code;
	private String isSelected;

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
