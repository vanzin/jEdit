/*
 * VFSFileChooserDialog.java - VFS file chooser
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

//{{{ Imports
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * Wraps the VFS browser in a modal dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSFileChooserDialog extends EnhancedDialog
{
	//{{{ VFSFileChooserDialog constructor
	public VFSFileChooserDialog(View view, String path,
		int mode, boolean multipleSelection)
	{
		super(view,jEdit.getProperty("vfs.browser.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		String name;
		if(mode == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
			name = null;
		else if(path == null || path.endsWith(File.separator)
			|| path.endsWith("/"))
		{
			name = null;
		}
		else
		{
			VFS vfs = VFSManager.getVFSForPath(path);
			name = vfs.getFileName(path);
			path = vfs.getParentOfPath(path);
		}

		browser = new VFSBrowser(view,path,mode,multipleSelection,true);
		browser.addBrowserListener(new BrowserHandler());
		content.add(BorderLayout.CENTER,browser);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));

		filenameField = new JTextField();
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

		if(mode != VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
		{
			JLabel label = new JLabel(jEdit.getProperty("vfs.browser.dialog.filename"));
			label.setDisplayedMnemonic(jEdit.getProperty(
				"vfs.browser.dialog.filename.mnemonic").charAt(0));
			label.setLabelFor(filenameField);
			panel.add(label);
			panel.add(Box.createHorizontalStrut(12));

			panel.add(box);

			panel.add(Box.createHorizontalStrut(12));
		}
		else
			panel.add(Box.createGlue());

		if(mode == VFSBrowser.BROWSER || mode == VFSBrowser.OPEN_DIALOG
			|| mode == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
		{
			GUIUtilities.requestFocus(this,browser.getBrowserView()
				.getDefaultFocusComponent());
		}
		else
		{
			GUIUtilities.requestFocus(this,filenameField);
		}

		ok = new JButton();
		getRootPane().setDefaultButton(ok);

		switch(mode)
		{
		case VFSBrowser.OPEN_DIALOG:
		case VFSBrowser.BROWSER_DIALOG:
			ok.setText(jEdit.getProperty("vfs.browser.dialog.open"));
			break;
		case VFSBrowser.CHOOSE_DIRECTORY_DIALOG:
			ok.setText(jEdit.getProperty("vfs.browser.dialog.choose-dir"));
			// so that it doesn't resize...
			dim = ok.getPreferredSize();
			ok.setPreferredSize(dim);
			break;
		case VFSBrowser.SAVE_DIALOG:
			ok.setText(jEdit.getProperty("vfs.browser.dialog.save"));
			break;
		}

		ok.addActionListener(new ActionHandler());
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		panel.add(cancel);

		content.add(BorderLayout.SOUTH,panel);

		VFSManager.getIOThreadPool().addProgressListener(
			workThreadHandler = new WorkThreadHandler());

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser.dialog");
		show();
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"vfs.browser.dialog");
		VFSManager.getIOThreadPool().removeProgressListener(workThreadHandler);
		super.dispose();
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		VFS.DirectoryEntry[] files = browser.getSelectedFiles();

		String directory = browser.getDirectory();

		if(files.length == 0)
		{
			if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
			{
				filename = browser.getDirectory();
			}
			else
			{
				filename = filenameField.getText();

				if(filename.length() == 0)
				{
					getToolkit().beep();
					return;
				}
				else if(browser.getMode() == VFSBrowser.BROWSER_DIALOG)
				{
					Hashtable props = new Hashtable();
					props.put(Buffer.ENCODING,browser.currentEncoding);
					jEdit.openFile(browser.getView(),
						browser.getDirectory(),
						filename,false,props);
					dispose();
					return;
				}
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
					if(file.name.equals(filenameField.getText()))
						filenameField.setText(null);
					return;
				}
				else if(browser.getMode() == VFSBrowser.SAVE_DIALOG)
					filename = file.path;
			}
		}

		if(browser.getMode() == VFSBrowser.SAVE_DIALOG)
		{
			if(!MiscUtilities.isURL(directory)
				&& !MiscUtilities.isURL(filename))
			{
				filename = MiscUtilities.constructPath(directory,
					MiscUtilities.canonPath(filename));

				if(doFileExistsWarning(filename))
					return;
			}
		}
		else if(browser.getMode() == VFSBrowser.BROWSER_DIALOG)
		{
			browser.filesActivated(VFSBrowser.M_OPEN,false);
		}

		isOK = true;
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getSelectedFiles() method
	public String[] getSelectedFiles()
	{
		if(!isOK)
			return null;

		if(filename != null && filename.length() != 0)
		{
			String path = browser.getDirectory();
			return new String[] { MiscUtilities.constructPath(
				path,filename) };
		}
		else
		{
			Vector vector = new Vector();
			VFS.DirectoryEntry[] selectedFiles = browser.getSelectedFiles();
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFS.DirectoryEntry file =  selectedFiles[i];
				if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
				{
					if(file.type != VFS.DirectoryEntry.FILE)
						vector.addElement(file.path);
				}
				else
				{
					if(file.type == VFS.DirectoryEntry.FILE)
						vector.addElement(file.path);
				}
			}
			String[] retVal = new String[vector.size()];
			vector.copyInto(retVal);
			return retVal;
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private VFSBrowser browser;
	private JTextField filenameField;
	private String filename;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;
	private WorkThreadHandler workThreadHandler;
	//}}}

	//{{{ doFileExistsWarning() method
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
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
			{
				if(!browser.getDirectory().equals(
					browser.getDirectoryField().getText()))
				{
					browser.setDirectory(browser.getDirectoryField().getText());
				}
				else
					ok();
			}
			else if(evt.getSource() == cancel)
				cancel();
		}
	} //}}}

	//{{{ BrowserHandler class
	class BrowserHandler implements BrowserListener
	{
		//{{{ filesSelected() method
		public void filesSelected(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			if(files.length == 0)
			{
				if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.choose-dir"));
				}
				return;
			}
			else if(files.length == 1)
			{
				if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.open"));
				}

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
					filenameField.selectAll();
				}
			}
			else
			{
				if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.open"));
				}

				filenameField.setText(null);
			}
		} //}}}

		//{{{ filesActivated() method
		public void filesActivated(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			for(int i = 0; i < files.length; i++)
			{
				VFS.DirectoryEntry file = files[i];
				if(file.type == VFS.DirectoryEntry.FILESYSTEM
					|| file.type == VFS.DirectoryEntry.DIRECTORY)
				{
					if(file.name.equals(filenameField.getText()))
						filenameField.setText(null);

					// the browser will list the directory
					// in question, so just return
					return;
				}
			}

			if(browser.getMode() == VFSBrowser.BROWSER_DIALOG)
				dispose();
			else
				ok();
		} //}}}
	} //}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		//{{{ keyPressed() method
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_LEFT:
				if(filenameField.getCaretPosition() == 0)
					browser.getBrowserView().getTree().processKeyEvent(evt);
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				browser.getBrowserView().getTree().processKeyEvent(evt);
				break;
			}
		} //}}}

		//{{{ keyTyped() method
		public void keyTyped(KeyEvent evt)
		{
			char ch = evt.getKeyChar();
			if(ch < 0x20 || ch == 0x7f || ch == 0xff)
				return;

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					BrowserView view = browser.getBrowserView();
					view.selectNone();
					view.getTree().doTypeSelect(
						filenameField.getText(),
						false);
					VFS.DirectoryEntry[] files =
						view.getSelectedFiles();
					if(files.length != 0)
					{
						int caret = filenameField
							.getCaretPosition();
						filenameField.setText(files[0].name);
						filenameField.setCaretPosition(
							files[0].name.length());
						filenameField.moveCaretPosition(caret);
					}
				}
			});
		} //}}}
	} //}}}

	//{{{ WorkThreadListener implementation
	class WorkThreadHandler implements WorkThreadProgressListener
	{
		//{{{ statusUpdate() method
		public void statusUpdate(WorkThreadPool threadPool, int threadIndex)
		{
			// synchronize with hide/showWaitCursor()
			synchronized(VFSFileChooserDialog.this)
			{
				int requestCount = threadPool.getRequestCount();
				if(requestCount == 0)
				{
					getContentPane().setCursor(
						Cursor.getDefaultCursor());
				}
				else if(requestCount >= 1)
				{
					getContentPane().setCursor(
						Cursor.getPredefinedCursor(
						Cursor.WAIT_CURSOR));
				}
			}
		} //}}}

		//{{{ progressUpdate() method
		public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
		{
		} //}}}
	} //}}}

	//}}}
}
