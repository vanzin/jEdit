/*
 * VFSFileChooserDialog.java - VFS file chooser
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.annotation.Nonnull;
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.bufferio.IoTask;
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
		super(view,getTitle(mode),true);
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
		super(parent,getTitle(mode),true);
		setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());		
		_init(view,path,mode,multipleSelection,autoshow);
	} //}}}

	//{{{ VFSFileChooserDialog constructor
	/**
	 * Constructs a new VFSFileChooserDialog.
	 * This version can specify a Frame as the parent instead
	 * of the view.
	 * @since jEdit 4.3pre10
	 */
	public VFSFileChooserDialog(Frame parent, View view, String path,
		int mode, boolean multipleSelection, boolean autoshow)
	{
		super(parent,getTitle(mode),true);
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
		TaskManager.instance.removeTaskListener(ioTaskHandler);
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
		else if(choosingDir && (filename == null || filename.isEmpty()))
		{
			isOK = true;
			dispose();
			return;
		}
		else if(filename == null || filename.isEmpty())
		{
			javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
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

		int[] type = { -1 };
		filename = MiscUtilities.expandVariables(filename);
		String path = MiscUtilities.constructPath(browser.getDirectory(),filename);
		VFS vfs = VFSManager.getVFSForPath(path);
		Object session = vfs.createVFSSession(path,this);
		if(session == null)
			return;

		ThreadUtilities.runInBackground(new GetFileTypeRequest(
			vfs,session,path,type));
		AwtRunnableQueue.INSTANCE.runAfterIoTasks(() ->
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
					Hashtable<String, Object> props = new Hashtable<>();
					if(browser.currentEncoding != null)
					{
						props.put(JEditBuffer.ENCODING, browser.currentEncoding);
					}
					jEdit.openFile(browser.getView(),
						browser.getDirectory(),
						path, false, props);
				}
				dispose();
				break;
			case VFSFile.DIRECTORY:
			case VFSFile.FILESYSTEM:
				browser.setDirectory(path);
				break;
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

	/**
	 * Returns the selected files.
	 * If the browser is in {@link VFSBrowser#OPEN_DIALOG} mode, the file will have to be readable
	 *
	 * @return a String array containing paths, since jEdit 5.6pre1 it is never null (might be an empty array)
	 */
	@Nonnull
	public String[] getSelectedFiles()
	{
		if(!isOK)
			return StandardUtilities.EMPTY_STRING_ARRAY;

		if(browser.getMode() == VFSBrowser.CHOOSE_DIRECTORY_DIALOG)
		{
			if(browser.getSelectedFiles().length > 0)
				return getSelectedFiles(VFSFile.DIRECTORY, VFSFile.FILESYSTEM);
			else
				return new String[] { browser.getDirectory() };
		}
		else if(filename != null && !filename.isEmpty())
		{
			String path = browser.getDirectory();
			String absolutePath = MiscUtilities.constructPath(path, filename);
			if (browser.getMode() == VFSBrowser.OPEN_DIALOG)
			{
				if (VFSManager.canReadFile(absolutePath))
					return new String[] {absolutePath};
				return StandardUtilities.EMPTY_STRING_ARRAY;
			}
			return new String[]{absolutePath};
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
	private TaskListener ioTaskHandler;
	//}}}

	//{{{ getDefaultTitle() method
	private static String getDefaultTitle()
	{
		return jEdit.getProperty("vfs.browser.title");
	}// }}}

	//{{{ getTitle() method
	private static String getTitle(int mode)
	{
		switch(mode)
		{
		case VFSBrowser.OPEN_DIALOG:
			return jEdit.getProperty("vfs.browser.title.open");
		case VFSBrowser.SAVE_DIALOG:
			return jEdit.getProperty("vfs.browser.title.save");
		case VFSBrowser.BROWSER:
			return jEdit.getProperty("vfs.browser.title");
		case VFSBrowser.CHOOSE_DIRECTORY_DIALOG:
			return jEdit.getProperty("vfs.browser.title");
		case VFSBrowser.BROWSER_DIALOG:
			return jEdit.getProperty("vfs.browser.title.dialog");
		default:
			return jEdit.getProperty("vfs.browser.title");
		}
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
		panel.setBorder(new EmptyBorder(12, 12, 12, 12));
		
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

		ok.addActionListener(e -> ok());
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.setName("cancel");
		cancel.addActionListener(e -> cancel());
		GenericGUIUtilities.makeSameSize(ok, cancel);
		
		panel.add(Box.createHorizontalStrut(6));
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		panel.add(cancel);
		
		content.add(BorderLayout.SOUTH,panel);

		TaskManager.instance.addTaskListener(ioTaskHandler = new IoTaskHandler());

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser.dialog");
		GenericGUIUtilities.requestFocus(this,filenameField);
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
			return result != JOptionPane.YES_OPTION;
		}

		return false;
	} //}}}

	//{{{ getSelectedFiles() method
	@Nonnull
	private String[] getSelectedFiles(int type1, int type2)
	{
		List<String> l = new ArrayList<>();
		VFSFile[] selectedFiles = browser.getSelectedFiles();
		for (VFSFile file : selectedFiles)
		{
			if (file.getType() == type1 || file.getType() == type2)
			{
				if (file.getType() == VFSFile.FILE && browser.getMode() == VFSBrowser.OPEN_DIALOG)
				{
					if (file.isReadable())
						l.add(file.getPath());
				}
				else
					l.add(file.getPath());
			}
		}
		return l.toArray(StandardUtilities.EMPTY_STRING_ARRAY);
	} //}}}

	//}}}

	//{{{ Inner classes
	//{{{ BrowserHandler class
	private class BrowserHandler implements BrowserListener
	{
		//{{{ filesSelected() method
		@Override
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
		@Override
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

			for(int i = 0, n = files.length; i < n; i++)
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

	//{{{ IoTaskListener class
	private class IoTaskHandler extends TaskAdapter
	{
		private final Runnable cursorStatus = () ->
		{
			int requestCount = TaskManager.instance.countIoTasks();
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
		};

		//{{{ waiting() method
		@Override
		public void waiting(Task task)
		{
			SwingUtilities.invokeLater(cursorStatus);
		} //}}}
	
		//{{{ running() method
		@Override
		public void running(Task task)
		{
			SwingUtilities.invokeLater(cursorStatus);
		} //}}}
	
		//{{{ done() method
		@Override
		public void done(Task task)
		{
			SwingUtilities.invokeLater(cursorStatus);
		} //}}}

		//{{{ progressUpdate() method
		@Override
		public void valueUpdated(Task task)
		{
			SwingUtilities.invokeLater(cursorStatus);
		} //}}}
	} //}}}

	//{{{ GetFileTypeRequest class
	private class GetFileTypeRequest extends IoTask
	{
		private final VFS    vfs;
		private final Object session;
		private final String path;
		private final int[]  type;

		GetFileTypeRequest(VFS vfs, Object session, String path, int[] type)
		{
			this.vfs     = vfs;
			this.session = session;
			this.path    = path;
			this.type    = type;
		}

		@Override
		public void _run()
		{
			try
			{
				VFSFile entry = vfs._getFile(session, path, browser);
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
				VFSManager.error(e,path,browser);
			}
			finally
			{
				try
				{
					vfs._endVFSSession(session, browser);
				}
				catch(IOException e)
				{
					VFSManager.error(e,path,browser);
				}
			}
		}
	} //}}}

	//}}}
}
