/*
 * RegisterChanged.java - Register changed message
 * Copyright (C) 2004 Nicholas O'Leary
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.Registers;

/**
 * Message sent when a register is updated.
 * @author Nicholas O'Leary
 * @version $Id$
 *
 * @since jEdit 4.3pre1
 */
public class RegisterChanged extends EBMessage
{
	 private char registerName;

	 /**
	 * Creates a new registers changed message.
	 * @param source The message source
	 */
	 public RegisterChanged(EBComponent source, char name)
	 {
		 super(source);
		 registerName = name;
	 }
	 
	 public char getRegisterName()
	 {
		 return registerName;
	 }
	 
	 public String getRegisterValue()
	 {
		 return Registers.getRegister(registerName).toString();
	 }
	 
	 public String paramString()
	 {
		 return "register=" + registerName + "," + super.paramString();
	 }
}
