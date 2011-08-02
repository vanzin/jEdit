/*
 * VFSFileChooserDialog.java - VFS file chooser
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.gui.notification.NotificationService;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.*;
//}}}

/**
 * Wraps the VFS browser in a modal dialog.
 * Shows up when "File-Open" is used. 
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSFileChooserDialog extends EnhancedDialog
{

	//{{{ VFSFileChooserDialog constructor
	public VFSFileChooserDialog(View view, String path,
		int mode, boolean multipleSelection)
	{
		this(view,path,mode,multipleSelection,true);
	} //}}}

	//{{{ VFSFileChooserDialog constructor
	/**
	 * Constructs a new VFSFileChooserDialog. If <code>authoshow</code>
	 * is true, the dialog will be show automatically and the call
	 * will only return after the user disposes of the dialog.
	 *
	 * @since jEdit 4.3pre7
	 */
	public VFSFileChooserDialog(View view, String path,
		int mode, boolean multipleSelection, boolean autoshow)
	{
		super(view,getDefaultTitle(),true);
		setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
		_init(view,path,mode,multipleSelection,autoshow);
	} //}}}

	//{{{ VFSFileChooserDialog constructor
	/**
	 * Constructs a new VFSFileChooserDialog.
	 * This version can specify a dialog as the parent instead
	 * of the view.
	 * @since jEdit 4.3pre10
	 */
	public VFSFileChooserDialog(Dialog parent, View view, String path,
		int mode, boolean multipleSelection, boolean autoshow)
	{
		super(parent,getDefaultTitle(),true);
		setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());		
		_init(view,path,mode,multipleSelection,autoshow);
	} //}}}

	/**
	 * Constructs a new VFSFileChooserDialog.
	 * This version can specify a Frame as the parent instead
	 * of the view.
	 * @since jEdit 4.3pre10
	 */
	public VFSFileChooserDialog(Frame parent, View view, String path,
		int mode, boolean multipleSelection, boolean autoshow)
	{
		super(parent, getDefaultTitle(),true);
		setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());		
		_init(view,path,mode,multipleSelection,autoshow);
	} //}}}


	//{{{ getBrowser() method
	/**
	 * Returns the VFSBrowser instance used internally.
	 * @return the VFS browser used in the dialog
	 * @since jEdit 4.3pre7
	 */
	public VFSBrowser getBrowser()
	{
		return browser;
	} //}}}

	//{{{ dispose() method
	@Override
	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"vfs.browser.dialog");
		VFSManager.getIOThreadPool().removeProgressListener(workThreadHandler);
		super.dispose();
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		VFSFile[] files = browser.getSelectedFiles();
		filename = filenameField.getText();
		boolean choosingDir = (browser.getMode() ==
			VFSBrowser.CHOOSE_DIRECTORY_DIALOG);

		if(files.length != 0)
		{
			if(choosingDir)
			{
				isOK = true;
				dispose();
			}
			else
				browser.filesActivated(VFSBrowser.M_OPEN,false);
			return;
		}
		else if(choosingDir && (filename == null || filename.length() == 0))
		{
			isOK = true;
			dispose();
			return;
		}
		else if(filename == null || filename.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		String bufferDir = browser.getView().getBuffer()
			.getDirectory();
		if(filename.equals("-"))
			filename = bufferDir;
		else if(filename.startsWith("-/")
			|| filename.startsWith('-' + File.separator))
		{
			filename = MiscUtilities.constructPath(
				bufferDir,filename.substring(2));
		}

		final int[] type = { -1 };
		filename = MiscUtilities.expandVariables(filename);
		final String path = MiscUtilities.constructPath(
			browser.getDirectory(),filename);
		final VFS vfs = VFSManager.getVFSForPath(path);
		Object session = vfs.createVFSSession(path,this);
		if(session == null)
			return;

		VFSManager.runInWorkThread(new GetFileTypeRequest(
			vfs,session,path,type));
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				switch(type[0])
				{
				case VFSFile.FILE:
					if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
						break;

					if(vfs instanceof FileVFS)
					{
						if(doFileExistsWarning(path))
							break;
					}
					isOK = true;
					if(browser.getMode() == VFSBrowser.BROWSER_DIALOG)
					{
						Hashtable props = new Hashtable();
						if(browser.currentEncoding != null)
						{
							props.put(JEditBuffer.ENCODING,browser.currentEncoding);
						}
						jEdit.openFile(browser.getView(),
							browser.getDirectory(),
							path,false,props);
					}
					dispose();
					break;
				case VFSFile.DIRECTORY:
				case VFSFile.FILESYSTEM:
					browser.setDirectory(path);
					break;
				}
			}
		});
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getSelectedFiles() method
	public String[] getSelectedFiles()
	{
		if(!isOK)
			return null;

		if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
		{
			if(browser.getSelectedFiles().length > 0)
			{
				return getSelectedFiles(VFSFile.DIRECTORY,
					VFSFile.FILESYSTEM);
			}
			else
				return new String[] { browser.getDirectory() };
		}
		else if(filename != null && filename.length() != 0)
		{
			String path = browser.getDirectory();
			return new String[] { MiscUtilities.constructPath(
				path,filename) };
		}
		else
			return getSelectedFiles(VFSFile.FILE,VFSFile.FILE);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private VFSBrowser browser;
	private VFSFileNameField filenameField;
	private String filename;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;
	private WorkThreadHandler workThreadHandler;
	//}}}

	//{{{ getDefaultTitle() method
	private static String getDefaultTitle()
	{
		return jEdit.getProperty("vfs.browser.title");
	}// }}}

	//{{{ _init method
	private void _init(View view, String path,
		int mode, boolean multipleSelection, boolean autoshow)
	{
		JPanel content = new JPanel(new BorderLayout());
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
			if ((vfs.getCapabilities() & VFS.BROWSE_CAP) == 0)
			{
				path = null;
			}
		}

		browser = new VFSBrowser(view, path, mode, multipleSelection, null);
		
		browser.addBrowserListener(new BrowserHandler());
		content.add(BorderLayout.CENTER,browser);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,12,12,12));
		
		filenameField = new VFSFileNameField(browser,null);
		filenameField.setText(name);
		filenameField.selectAll();
		filenameField.setName("filename");
		browser.setDefaultFocusComponent(filenameField);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(filenameField);
		box.add(Box.createGlue());

		JLabel label = new JLabel(jEdit.getProperty("vfs.browser.dialog.filename"));
		label.setDisplayedMnemonic(jEdit.getProperty(
			"vfs.browser.dialog.filename.mnemonic").charAt(0));
		label.setLabelFor(filenameField);
		panel.add(label);
		panel.add(Box.createHorizontalStrut(12));

		panel.add(box);

		panel.add(Box.createHorizontalStrut(12));

		ok = new JButton();
		ok.setName("ok");
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
			Dimension dim = ok.getPreferredSize();
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
		cancel.setName("cancel");
		cancel.addActionListener(new ActionHandler());
		panel.add(cancel);

		content.add(BorderLayout.SOUTH,panel);

		VFSManager.getIOThreadPool().addProgressListener(
			workThreadHandler = new WorkThreadHandler());

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser.dialog");
		GUIUtilities.requestFocus(this,filenameField);
		if (autoshow)
			setVisible(true);
	} //}}}

	
	
	//{{{ doFileExistsWarning() method
	private boolean doFileExistsWarning(String filename)
	{
		if(browser.getMode() == VFSBrowser.SAVE_DIALOG
			&& new File(filename).exists())
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

	//{{{ getSelectedFiles() method
	private String[] getSelectedFiles(int type1, int type2)
	{
		List<String> l = new ArrayList<String>();
		VFSFile[] selectedFiles = browser.getSelectedFiles();
		for(int i = 0; i < selectedFiles.length; i++)
		{
			VFSFile file = selectedFiles[i];
			if(file.getType() == type1 || file.getType() == type2)
				l.add(file.getPath());
		}
		return l.toArray(new String[l.size()]);
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	private class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else if(evt.getSource() == cancel)
				cancel();
		}
	} //}}}

	//{{{ BrowserHandler class
	private class BrowserHandler implements BrowserListener
	{
		//{{{ filesSelected() method
		public void filesSelected(VFSBrowser browser, VFSFile[] files)
		{
			boolean choosingDir = (browser.getMode()
				== VFSBrowser.CHOOSE_DIRECTORY_DIALOG);

			if(files.length == 0)
			{
				if(choosingDir)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.choose-dir"));
				}
			}
			else if(files.length == 1)
			{
				if(choosingDir)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.choose-dir"));
				}

				VFSFile file = files[0];
				if(file.getType() == VFSFile.FILE)
				{
					String path = file.getPath();
					String directory = browser.getDirectory();
					String parent = MiscUtilities
						.getParentOfPath(path);
					if(MiscUtilities.pathsEqual(parent,directory))
						path = file.getName();

					filenameField.setText(path);
					filenameField.selectAll();
				}
			}
			else
			{
				if(choosingDir)
				{
					ok.setText(jEdit.getProperty(
						"vfs.browser.dialog.choose-dir"));
				}

				filenameField.setText(null);
			}
		} //}}}

		//{{{ filesActivated() method
		public void filesActivated(VFSBrowser browser, VFSFile[] files)
		{
			filenameField.selectAll();

			if(files.length == 0)
			{
				// user pressed enter when the vfs table or
				// file name field has focus, with nothing
				// selected.
				ok();
				return;
			}

			for(int i = 0; i < files.length; i++)
			{
				if(files[i].getType() == VFSFile.FILE)
				{
					String path = files[i].getPath();
					VFS vfs = VFSManager.getVFSForPath(path);
					if(browser.getMode() == VFSBrowser.SAVE_DIALOG
						&& vfs instanceof FileVFS)
					{
						if(doFileExistsWarning(path))
							return;
					}

					isOK = true;
					filenameField.setText(null);
					if(browser.getMode() != VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
					{
						dispose();
					}
					return;
				}
				else
					return;
			}
		} //}}}
	} //}}}

	//{{{ WorkThreadListener class
	private class WorkThreadHandler implements WorkThreadProgressListener
	{
		//{{{ statusUpdate() method
		public void statusUpdate(final WorkThreadPool threadPool,
			int threadIndex)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
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
			});
		} //}}}

		//{{{ progressUpdate() method
		public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
		{
		} //}}}
	} //}}}

	//{{{ GetFileTypeRequest class
	private class GetFileTypeRequest implements Runnable
	{
		VFS    vfs;
		Object session;
		String path;
		int[]  type;

		GetFileTypeRequest(VFS vfs, Object session,
			String path, int[] type)
		{
			this.vfs     = vfs;
			this.session = session;
			this.path    = path;
			this.type    = type;
		}

		public void run()
		{
			try
			{
				VFSFile entry = vfs._getFile(
						session,
						path,
						browser);
				if(entry == null)
				{
					// non-existent file
					type[0] = VFSFile.FILE;
				}
				else
					type[0] = entry.getType();
			}
			catch(IOException e)
			{
				NotificationService.ioerror(e,path,browser);
			}
			finally
			{
				try
				{
					vfs._endVFSSession(
						session,
						browser);
				}
				catch(IOException e)
				{
					NotificationService.ioerror(e,path,browser);
				}
			}
		}
	} //}}}

	//}}}
}
