/*
 * VFSFileChooserDialog.java - VFS file chooser
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;

/**
 * Wraps the VFS browser in a modal dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSFileChooserDialog extends EnhancedDialog
{
	public VFSFileChooserDialog(View view, String path,
		int mode, boolean multipleSelection)
	{
		super(view,jEdit.getProperty("vfs.browser.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		String name;
		if(path == null || path.endsWith(File.separator) || path.endsWith("/"))
			name = null;
		else
		{
			VFS vfs = VFSManager.getVFSForPath(path);
			name = vfs.getFileName(path);
			path = vfs.getParentOfPath(path);
		}

		browser = new VFSBrowser(view,path,mode,multipleSelection);
		browser.addBrowserListener(new BrowserHandler());
		content.add(BorderLayout.CENTER,browser);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));

		panel.add(new JLabel(jEdit.getProperty("vfs.browser.dialog.filename")));
		panel.add(Box.createHorizontalStrut(12));

		filenameField = new JTextField(20);
		filenameField.setText(name);
		filenameField.addKeyListener(new KeyHandler());
		filenameField.selectAll();
		Dimension dim = filenameField.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		filenameField.setMaximumSize(dim);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(filenameField);
		box.add(Box.createGlue());
		panel.add(box);

		panel.add(Box.createHorizontalStrut(12));

		if(mode == VFSBrowser.SAVE_DIALOG)
		{
			GUIUtilities.requestFocus(this,filenameField);
		}
		else
		{
			GUIUtilities.requestFocus(this,browser.getBrowserView()
				.getDefaultFocusComponent());
		}

		ok = new JButton(jEdit.getProperty("vfs.browser.dialog."
			+ (mode == VFSBrowser.OPEN_DIALOG ? "open" : "save")));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		panel.add(cancel);

		if(mode != VFSBrowser.SAVE_DIALOG)
			panel.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,panel);

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser.dialog");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"vfs.browser.dialog");
		super.dispose();
	}

	public void ok()
	{
		VFS.DirectoryEntry[] files = browser.getSelectedFiles();

		String directory = browser.getDirectory();

		if(files.length == 0)
		{
			filename = filenameField.getText();

			if(filename.length() == 0)
			{
				getToolkit().beep();
				return;
			}
		}
		else
		{
			for(int i = 0; i < files.length; i++)
			{
				VFS.DirectoryEntry file = files[i];
				if(file.type == VFS.DirectoryEntry.FILESYSTEM
					|| file.type == VFS.DirectoryEntry.DIRECTORY)
				{
					browser.setDirectory(file.path);
					return;
				}
				else if(browser.getMode() == VFSBrowser.SAVE_DIALOG)
					filename = file.path;
			}
		}

		if(browser.getMode() == VFSBrowser.SAVE_DIALOG)
		{
			VFS vfs = VFSManager.getVFSForPath(directory);
			filename = vfs.constructPath(directory,filename);

			if(vfs instanceof FileVFS && doFileExistsWarning(filename))
				return;
		}

		isOK = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	public String[] getSelectedFiles()
	{
		if(!isOK)
			return null;

		if(filename != null)
			return new String[] { filename };
		else
		{
			Vector vector = new Vector();
			VFS.DirectoryEntry[] selectedFiles = browser.getSelectedFiles();
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFS.DirectoryEntry file =  selectedFiles[i];
				if(file.type == VFS.DirectoryEntry.FILE)
					vector.addElement(file.path);
			}
			String[] retVal = new String[vector.size()];
			vector.copyInto(retVal);
			return retVal;
		}
	}

	// private members
	private VFSBrowser browser;
	private JTextField filenameField;
	private String filename;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;

	private boolean doFileExistsWarning(String filename)
	{
		if(new File(filename).exists())
		{
			String[] args = { MiscUtilities.getFileName(filename) };
			int result = GUIUtilities.confirm(browser,
				"fileexists",args,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return true;
		}

		return false;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else if(evt.getSource() == cancel)
				cancel();
		}
	}

	class BrowserHandler implements BrowserListener
	{
		public void filesSelected(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			if(files.length == 0)
				return;
			else if(files.length == 1)
			{
				VFS.DirectoryEntry file = files[0];
				if(file.type == VFS.DirectoryEntry.FILE)
				{
					String path = file.path;
					String directory = browser.getDirectory();
					VFS vfs = VFSManager.getVFSForPath(directory);
					String parent = vfs.getParentOfPath(path);
					if(parent.endsWith("/") || parent.endsWith(File.separator))
						parent = parent.substring(0,parent.length() - 1);
					if(parent.equals(directory))
						path = file.name;

					filenameField.setText(path);
				}
			}
			else
			{
				filenameField.setText(null);
			}
		}

		public void filesActivated(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			for(int i = 0; i < files.length; i++)
			{
				VFS.DirectoryEntry file = files[i];
				if(file.type == VFS.DirectoryEntry.FILESYSTEM
					|| file.type == VFS.DirectoryEntry.DIRECTORY)
				{
					// the browser will list the directory
					// in question, so just return
					return;
				}
			}

			ok();
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			browser.getBrowserView().selectNone();
		}
	}
}
