/*
 * VFSFileNameField.java - File name field with completion
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2002 Slava Pestov
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

package org.gjt.sp.jedit.browser;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.MiscUtilities;

class VFSFileNameField extends JTextField
{
	//{{{ VFSFileNameField constructor
	VFSFileNameField(VFSBrowser browser)
	{
		this.browser = browser;
		Dimension dim = getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		setMaximumSize(dim);
	} //}}}

	//{{{ isManagingFocus() method
	public boolean isManagingFocus()
	{
		return false;
	} //}}}
	
	//{{{ getFocusTraversalKeysEnabled() method
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	} //}}}

	//{{{ processKeyEvent() method
	public void processKeyEvent(KeyEvent evt)
	{
		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_TAB:
				doComplete();
				return;
			case KeyEvent.VK_LEFT:
				if(getCaretPosition() == 0)
					browser.getBrowserView().getTree().processKeyEvent(evt);
				return;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
				browser.getBrowserView().getTree().processKeyEvent(evt);
				return;
			}
		}
		else if(evt.getID() == KeyEvent.KEY_TYPED)
		{
			char ch = evt.getKeyChar();
			if(ch > 0x20 && ch != 0x7f && ch != 0xff)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						BrowserView view = browser.getBrowserView();
						view.selectNone();
						view.getTree().doTypeSelect(getText(),false);
					}
				});
			}
		}

		super.processKeyEvent(evt);
	} //}}}

	//{{{ Private members
	private VFSBrowser browser;

	//{{{ doComplete() method
	private void doComplete()
	{
		String currentText = getText();

		BrowserView view = browser.getBrowserView();

		VFS.DirectoryEntry[] files = view.getSelectedFiles();
		if(files.length == 0)
			return;

		String path = files[0].path;
		String name = files[0].name;
		String parent = MiscUtilities.getParentOfPath(path);

		String newText;
		if(MiscUtilities.isAbsolutePath(currentText)
			&& !currentText.startsWith(browser.getDirectory()))
		{
			newText = path;
		}
		else
		{
			if(pathsEqual(parent,browser.getDirectory()))
				newText = name;
			else
				newText = path;
		}

		setText(newText);
	} //}}}

	//{{{ pathsEqual() method
	private boolean pathsEqual(String p1, String p2)
	{
		if(p1.endsWith("/") || p1.endsWith(File.separator))
			p1 = p1.substring(0,p1.length() - 1);
		if(p2.endsWith("/") || p2.endsWith(File.separator))
			p2 = p2.substring(0,p2.length() - 2);
		return p1.equals(p2);
	} //}}}

	//}}}
}
