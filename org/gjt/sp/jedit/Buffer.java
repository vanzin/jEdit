/*
 * Buffer.java - jEdit buffer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2005 Slava Pestov
 * Portions copyright (C) 1999, 2000 mike dillon
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
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.text.Segment;

import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.buffer.BufferUndoListener;
import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.bufferio.BufferAutosaveRequest;
import org.gjt.sp.jedit.bufferio.BufferIORequest;
import org.gjt.sp.jedit.bufferio.MarkersSaveRequest;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.options.GeneralOptionPane;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.jedit.syntax.ParserRuleSet;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.jedit.visitors.SaveCaretInfoVisitor;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * A <code>Buffer</code> represents the contents of an open text
 * file as it is maintained in the computer's memory (as opposed to
 * how it may be stored on a disk).<p>
 *
 * In a BeanShell script, you can obtain the current buffer instance from the
 * <code>buffer</code> variable.<p>
 *
 * This class does not have a public constructor.
 * Buffers can be opened and closed using methods in the <code>jEdit</code>
 * class.<p>
 *
 * This class is partially thread-safe, however you must pay attention to two
 * very important guidelines:
 * <ul>
 * <li>Operations such as insert() and remove(),
 * undo(), change Buffer data in a writeLock(), and must
 * be called from the AWT thread.
 * <li>When accessing the buffer from another thread, you must
 * call readLock() before and readUnLock() after, if you plan on performing
 * more than one read, to ensure that  the buffer contents are not changed by
 * the AWT thread for the duration of the lock. Only methods whose descriptions
 * specify thread safety can be invoked from other threads.
 * </ul>

 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Buffer extends JEditBuffer
{
	//{{{ Some constants
	/**
	 * Backed up property.
	 * @since jEdit 3.2pre2
	 */
	public static final String BACKED_UP = "Buffer__backedUp";

	/**
	 * Caret info properties.
	 * @since jEdit 3.2pre1
	 */
	public static final String CARET = "Buffer__caret";
	public static final String CARET_POSITIONED = "Buffer__caretPositioned";

	/**
	 * Stores a List of {@link org.gjt.sp.jedit.textarea.Selection} instances.
	 */
	public static final String SELECTION = "Buffer__selection";

	/**
	 * This should be a physical line number, so that the scroll
	 * position is preserved correctly across reloads (which will
	 * affect virtual line numbers, due to fold being reset)
	 */
	public static final String SCROLL_VERT = "Buffer__scrollVert";
	public static final String SCROLL_HORIZ = "Buffer__scrollHoriz";

	/**
	 * Should jEdit try to set the encoding based on a UTF8, UTF16 or
	 * XML signature at the beginning of the file?
	 */
	public static final String ENCODING_AUTODETECT = "encodingAutodetect";

	/**
	 * This property is set to 'true' if the file has a trailing newline.
	 * @since jEdit 4.0pre1
	 */
	public static final String TRAILING_EOL = "trailingEOL";

	/**
	 * This property is set to 'true' if the file should be GZipped.
	 * @since jEdit 4.0pre4
	 */
	public static final String GZIPPED = "gzipped";
	//}}}

	//{{{ Input/output methods

	//{{{ reload() method
	/**
	 * Reloads the buffer from disk, asking for confirmation if the buffer
	 * has unsaved changes.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void reload(View view)
	{
		if (getFlag(UNTITLED))
			return;
		if(isDirty())
		{
			String[] args = { path };
			int result = GUIUtilities.confirm(view,"changedreload",
				args,JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}
		view.visit(new SaveCaretInfoVisitor());
		load(view,true);
	} //}}}

	//{{{ load() method
	/**
	 * Loads the buffer from disk.
	 * @param view The view
	 * @param reload If true, user will not be asked to recover autosave
	 * file, if any
	 *
	 * @since 2.5pre1
	 */
	public boolean load(final View view, final boolean reload)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		setBooleanProperty(BufferIORequest.ERROR_OCCURRED,false);

		setLoading(true);

		// view text areas temporarily blank out while a buffer is
		// being loaded, to indicate to the user that there is no
		// data available yet.
		if(!getFlag(TEMPORARY))
			EditBus.send(new BufferUpdate(this,view,BufferUpdate.LOAD_STARTED));

		final boolean loadAutosave;

		if(reload || !getFlag(NEW_FILE))
		{
			if(file != null)
				modTime = file.lastModified();

			// Only on initial load
			if(!reload && autosaveFile != null && autosaveFile.exists())
				loadAutosave = recoverAutosave(view);
			else
			{
				if(autosaveFile != null)
					autosaveFile.delete();
				loadAutosave = false;
			}

			if(!loadAutosave)
			{
				VFS vfs = VFSManager.getVFSForPath(path);

				if(!checkFileForLoad(view,vfs,path))
				{
					setLoading(false);
					return false;
				}

				// have to check again since above might set
				// NEW_FILE flag
				if(reload || !getFlag(NEW_FILE))
				{
					if(!vfs.load(view,this,path))
					{
						setLoading(false);
						return false;
					}
				}
			}
		}
		else
			loadAutosave = false;

		//{{{ Do some stuff once loading is finished
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				String newPath = getStringProperty(
					BufferIORequest.NEW_PATH);
				Segment seg = (Segment)getProperty(
					BufferIORequest.LOAD_DATA);
				IntegerArray endOffsets = (IntegerArray)
					getProperty(BufferIORequest.END_OFFSETS);

				loadText(seg,endOffsets);

				unsetProperty(BufferIORequest.LOAD_DATA);
				unsetProperty(BufferIORequest.END_OFFSETS);
				unsetProperty(BufferIORequest.NEW_PATH);

				undoMgr.clear();
				undoMgr.setLimit(jEdit.getIntegerProperty(
					"buffer.undoCount",100));

				// If the buffer is temporary, we don't need to
				// call finishLoading() because it sets the FoldHandler
				// and reload markers. But we always need to set the edit
				// mode that is necessary for HyperSearch on directories
				if (getFlag(TEMPORARY))
					setMode();
				else
					finishLoading();

				setLoading(false);

				// if reloading a file, clear dirty flag
				if(reload)
					setDirty(false);

				if(!loadAutosave && newPath != null)
					setPath(newPath);

				// if loadAutosave is false, we loaded an
				// autosave file, so we set 'dirty' to true

				// note that we don't use setDirty(),
				// because a) that would send an unnecessary
				// message, b) it would also set the
				// AUTOSAVE_DIRTY flag, which will make
				// the autosave thread write out a
				// redundant autosave file
				if(loadAutosave)
					Buffer.super.setDirty(true);

				// send some EditBus messages
				if(!getFlag(TEMPORARY))
				{
					fireBufferLoaded();
					EditBus.send(new BufferUpdate(Buffer.this,
						view,BufferUpdate.LOADED));
					//EditBus.send(new BufferUpdate(Buffer.this,
					//	view,BufferUpdate.MARKERS_CHANGED));
				}
			}
		}; //}}}

		if(getFlag(TEMPORARY))
			runnable.run();
		else
			VFSManager.runInAWTThread(runnable);

		return true;
	} //}}}

	//{{{ insertFile() method
	/**
	 * Loads a file from disk, and inserts it into this buffer.
	 * @param view The view
	 * @param path the path of the file to insert
	 *
	 * @since 4.0pre1
	 */
	public boolean insertFile(View view, String path)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		setBooleanProperty(BufferIORequest.ERROR_OCCURRED,false);

		path = MiscUtilities.constructPath(this.path,path);

		Buffer buffer = jEdit.getBuffer(path);
		if(buffer != null)
		{
			view.getTextArea().setSelectedText(
				buffer.getText(0,buffer.getLength()));
			return true;
		}

		VFS vfs = VFSManager.getVFSForPath(path);

		// this returns false if initial sanity
		// checks (if the file is a directory, etc)
		// fail
		return vfs.insert(view,this,path);
	} //}}}

	//{{{ autosave() method
	/**
	 * Autosaves this buffer.
	 */
	public void autosave()
	{
		if(autosaveFile == null || !getFlag(AUTOSAVE_DIRTY)
			|| !isDirty() || isPerformingIO() ||
			!autosaveFile.getParentFile().exists())
			return;

		setFlag(AUTOSAVE_DIRTY,false);

		VFSManager.runInWorkThread(new BufferAutosaveRequest(
			null,this,null,VFSManager.getFileVFS(),
			autosaveFile.getPath()));
	} //}}}

	//{{{ saveAs() method
	/**
	 * Prompts the user for a file to save this buffer to.
	 * @param view The view
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @return true if the buffer was successfully saved
	 * @since jEdit 2.6pre5
	 */
	public boolean saveAs(View view, boolean rename)
	{
		String[] files = GUIUtilities.showVFSFileDialog(view,path,
			VFSBrowser.SAVE_DIALOG,false);

		// files[] should have length 1, since the dialog type is
		// SAVE_DIALOG
		if(files == null)
			return false;

		boolean saved = save(view, files[0], rename);
		if (saved)
			setReadOnly(false);
		return saved;
	} //}}}

	//{{{ save() method
	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * @return true if the buffer was successfully saved
	 * the existing path
	 */
	public boolean save(View view, String path)
	{
		return save(view,path,true,false);
	} //}}}

	//{{{ save() method
	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @return true if the buffer was successfully saved
	 * @since jEdit 2.6pre5
	 */
	public boolean save(View view, String path, boolean rename)
	{
		return save(view,path,rename,false);
	} //}}}

	//{{{ save() method
	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @param disableFileStatusCheck  Disables file status checking
	 * regardless of the state of the checkFileStatus property
	 * @return true if the buffer was successfully saved
	 */
	public boolean save(final View view, String path, final boolean rename, boolean disableFileStatusCheck)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		setBooleanProperty(BufferIORequest.ERROR_OCCURRED,false);

		if(path == null && getFlag(NEW_FILE))
			return saveAs(view,rename);

		if(path == null && file != null)
		{
			long newModTime = file.lastModified();

			if(newModTime != modTime
				&& jEdit.getBooleanProperty("view.checkModStatus"))
			{
				Object[] args = { this.path };
				int result = GUIUtilities.confirm(view,
					"filechanged-save",args,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if(result != JOptionPane.YES_OPTION)
					return false;
			}
		}

		EditBus.send(new BufferUpdate(this,view,BufferUpdate.SAVING));

		setPerformingIO(true);

		final String oldPath = this.path;
		final String oldSymlinkPath = symlinkPath;
		final String newPath = path == null ? this.path : path;

		VFS vfs = VFSManager.getVFSForPath(newPath);

		if(!checkFileForSave(view,vfs,newPath))
		{
			setPerformingIO(false);
			return false;
		}

		Object session = vfs.createVFSSession(newPath,view);
		if (session == null)
		{
			setPerformingIO(false);
			return false;
		}

		unsetProperty("overwriteReadonly");
		unsetProperty("forbidTwoStageSave");
		try
		{
			VFSFile file = vfs._getFile(session,newPath,view);
			if (file != null)
			{
				boolean vfsRenameCap = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;
				if (!file.isWriteable())
				{
					Log.log(Log.WARNING, this, "Buffer saving : File " + file + " is readOnly");
					if (vfsRenameCap)
					{
						Log.log(Log.DEBUG, this, "Buffer saving : VFS can rename files");
						String savePath = vfs._canonPath(session,newPath,view);
						if(!MiscUtilities.isURL(savePath))
							savePath = MiscUtilities.resolveSymlinks(savePath);
						savePath = vfs.getTwoStageSaveName(savePath);
						if (savePath == null)
						{
							Log.log(Log.DEBUG, this, "Buffer saving : two stage save impossible because path is null");
							VFSManager.error(view,
								newPath,
								"ioerror.save-readonly-twostagefail",
								null);
							setPerformingIO(false);
							return false;
						}
						else
						{
							int result = GUIUtilities.confirm(
								view, "vfs.overwrite-readonly",
								new Object[]{newPath},
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE);
							if (result == JOptionPane.YES_OPTION)
							{
								Log.log(Log.WARNING, this, "Buffer saving : two stage save will be used to save buffer");
								setBooleanProperty("overwriteReadonly",true);
							}
							else
							{
								Log.log(Log.DEBUG,this, "Buffer not saved");
								setPerformingIO(false);
								return false;
							}
						}
					}
					else
					{
						Log.log(Log.WARNING, this, "Buffer saving : file is readonly and vfs cannot do two stage save");
						VFSManager.error(view,
							newPath,
							"ioerror.write-error-readonly",
							null);
						setPerformingIO(false);
						return false;
					}
				}
				else
				{
					String savePath = vfs._canonPath(session,newPath,view);
					if(!MiscUtilities.isURL(savePath))
						savePath = MiscUtilities.resolveSymlinks(savePath);
					savePath = vfs.getTwoStageSaveName(savePath);
					if (jEdit.getBooleanProperty("twoStageSave") && (!vfsRenameCap || savePath == null))
					{
						// the file is writeable but the vfs cannot do two stage. We must overwrite
						// readonly flag


						int result = GUIUtilities.confirm(
								view, "vfs.twostageimpossible",
								new Object[]{newPath},
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE);
						if (result == JOptionPane.YES_OPTION)
						{
							Log.log(Log.WARNING, this, "Buffer saving : two stage save cannot be used");
							setBooleanProperty("forbidTwoStageSave",true);
						}
						else
						{
							Log.log(Log.DEBUG,this, "Buffer not saved");
							setPerformingIO(false);
							return false;
						}

					}
				}
			}
		}
		catch(IOException io)
		{
			VFSManager.error(view,newPath,"ioerror",
				new String[] { io.toString() });
			setPerformingIO(false);
			return false;
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,view);
			}
			catch(IOException io)
			{
				VFSManager.error(view,newPath,"ioerror",
					new String[] { io.toString() });
				setPerformingIO(false);
				return false;
			}
		}

		if(!vfs.save(view,this,newPath))
		{
			setPerformingIO(false);
			return false;
		}

		// Once save is complete, do a few other things
		VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					setPerformingIO(false);
					setProperty("overwriteReadonly",null);
					finishSaving(view,oldPath,oldSymlinkPath,
						newPath,rename,getBooleanProperty(
							BufferIORequest.ERROR_OCCURRED));
					updateMarkersFile(view);
				}
			});

		int check = jEdit.getIntegerProperty("checkFileStatus");
		if(!disableFileStatusCheck && (check == GeneralOptionPane.checkFileStatus_all ||
					       check == GeneralOptionPane.checkFileStatus_operations))
			jEdit.checkBufferStatus(view,false);

		return true;
	} //}}}

	//{{{ checkFileStatus() method
	public static final int FILE_NOT_CHANGED = 0;
	public static final int FILE_CHANGED = 1;
	public static final int FILE_DELETED = 2;
	/**
	 * Check if the buffer has changed on disk.
	 * @return One of <code>NOT_CHANGED</code>, <code>CHANGED</code>, or
	 * <code>DELETED</code>.
	 *
	 * @since jEdit 4.2pre1
	 */
	public int checkFileStatus(View view)
	{
		// - don't do these checks while a save is in progress,
		// because for a moment newModTime will be greater than
		// oldModTime, due to the multithreading
		// - only supported on local file system
		if(!isPerformingIO() && file != null && !getFlag(NEW_FILE))
		{
			boolean newReadOnly = file.exists() && !file.canWrite();
			if(newReadOnly != isFileReadOnly())
			{
				setFileReadOnly(newReadOnly);
				EditBus.send(new BufferUpdate(this,null,
					BufferUpdate.DIRTY_CHANGED));
			}

			long oldModTime = modTime;
			long newModTime = file.lastModified();

			if(newModTime != oldModTime)
			{
				modTime = newModTime;

				if(!file.exists())
				{
					setFlag(NEW_FILE,true);
					setDirty(true);
					return FILE_DELETED;
				}
				else
				{
					return FILE_CHANGED;
				}
			}
		}

		return FILE_NOT_CHANGED;
	} //}}}

	//}}}

	//{{{ Getters/setter methods for various buffer meta-data

	//{{{ getLastModified() method
	/**
	 * Returns the last time jEdit modified the file on disk.
	 * This method is thread-safe.
	 */
	public long getLastModified()
	{
		return modTime;
	} //}}}

	//{{{ setLastModified() method
	/**
	 * Sets the last time jEdit modified the file on disk.
	 * @param modTime The new modification time
	 */
	public void setLastModified(long modTime)
	{
		this.modTime = modTime;
	} //}}}

	//{{{ getAutoReload() method
	/**
	 * Returns the status of the AUTORELOAD flag
	 * If true, reload changed files automatically
	 */
	public boolean getAutoReload()
	{
		return getFlag(AUTORELOAD);
	} //}}}

	//{{{ setAutoReload() method
	/**
	 * Sets the status of the AUTORELOAD flag
	 * @param value # If true, reload changed files automatically
	 */
	public void setAutoReload(boolean value)
	{
		setFlag(AUTORELOAD, value);
		autoreloadOverridden = isAutoreloadPropertyOverriden();
	} //}}}

	//{{{ getAutoReloadDialog() method
	/**
	 * Returns the status of the AUTORELOAD_DIALOG flag
	 * If true, prompt for reloading or notify user
	 * when the file has changed on disk
	 */
	public boolean getAutoReloadDialog()
	{
		return getFlag(AUTORELOAD_DIALOG);
	} //}}}

	//{{{ setAutoReloadDialog() method
	/**
	 * Sets the status of the AUTORELOAD_DIALOG flag
	 * @param value # If true, prompt for reloading or notify user
	 * when the file has changed on disk

	 */
	public void setAutoReloadDialog(boolean value)
	{
		setFlag(AUTORELOAD_DIALOG, value);
		autoreloadOverridden = isAutoreloadPropertyOverriden();
	} //}}}

	//{{{ getVFS() method
	/**
	 * Returns the virtual filesystem responsible for loading and
	 * saving this buffer. This method is thread-safe.
	 */
	public VFS getVFS()
	{
		return VFSManager.getVFSForPath(path);
	} //}}}

	//{{{ getAutosaveFile() method
	/**
	 * Returns the autosave file for this buffer. This may be null if
	 * the file is non-local.
	 */
	public File getAutosaveFile()
	{
		return autosaveFile;
	} //}}}

	//{{{ removeAutosaveFile() method
	/**
	 * Remove the autosave file.
	 * @since jEdit 4.3pre12
	 */
	public void removeAutosaveFile()
	{
		if (autosaveFile != null)
		{
			autosaveFile.delete();
			setFlag(AUTOSAVE_DIRTY,true);
		}
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the name of this buffer. This method is thread-safe.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getPath() method
	/**
	 * Returns the path name of this buffer. This method is thread-safe.
	 */
	public String getPath()
	{
		return path;
	} //}}}

	//{{{ getPath() method
	/**
	  * @param shortVersion if true, replaces home path with ~/ on unix
	  */
	public String getPath(Boolean shortVersion)
	{
		return shortVersion ? MiscUtilities.abbreviate(path) : getPath();
	} //}}}


	//{{{ getSymlinkPath() method
	/**
	 * If this file is a symbolic link, returns the link destination.
	 * Otherwise returns the file's path. This method is thread-safe.
	 * @since jEdit 4.2pre1
	 */
	public String getSymlinkPath()
	{
		return symlinkPath;
	} //}}}

	//{{{ getDirectory() method
	/**
	 * Returns the directory containing this buffer.
	 * @since jEdit 4.1pre11
	 */
	public String getDirectory()
	{
		return directory;
	} //}}}

	//{{{ isClosed() method
	/**
	 * Returns true if this buffer has been closed with
	 * {@link org.gjt.sp.jedit.jEdit#closeBuffer(View,Buffer)}.
	 * This method is thread-safe.
	 */
	public boolean isClosed()
	{
		return getFlag(CLOSED);
	} //}}}

	//{{{ isLoaded() method
	/**
	 * Returns true if the buffer is loaded. This method is thread-safe.
	 */
	public boolean isLoaded()
	{
		return !isLoading();
	} //}}}

	//{{{ isNewFile() method
	/**
	 * Returns whether this buffer lacks a corresponding version on disk.
	 * This method is thread-safe.
	 */
	public boolean isNewFile()
	{
		return getFlag(NEW_FILE);
	} //}}}

	//{{{ setNewFile() method
	/**
	 * Sets the new file flag.
	 * @param newFile The new file flag
	 */
	public void setNewFile(boolean newFile)
	{
		setFlag(NEW_FILE,newFile);
		if(!newFile)
			setFlag(UNTITLED,false);
	} //}}}

	//{{{ isUntitled() method
	/**
	 * Returns true if this file is 'untitled'. This method is thread-safe.
	 */
	public boolean isUntitled()
	{
		return getFlag(UNTITLED);
	} //}}}

	//{{{ setDirty() method
	/**
	 * Sets the 'dirty' (changed since last save) flag of this buffer.
	 */
	@Override
	public void setDirty(boolean d)
	{
		boolean old_d = isDirty();
		if (isUntitled() && jEdit.getBooleanProperty("suppressNotSavedConfirmUntitled"))
			d = false;
		if (d && getLength() == initialLength)
		{
			if (jEdit.getBooleanProperty("useMD5forDirtyCalculation"))
				d = !Arrays.equals(calculateHash(), md5hash);
		}
		super.setDirty(d);
		boolean editable = isEditable();

		if(d)
		{
			if(editable)
				setFlag(AUTOSAVE_DIRTY,true);
		}
		else
		{
			setFlag(AUTOSAVE_DIRTY,false);

			if(autosaveFile != null)
				autosaveFile.delete();
		}

		if(d != old_d && editable)
		{
			EditBus.send(new BufferUpdate(this,null,
				BufferUpdate.DIRTY_CHANGED));
		}
	} //}}}

	//{{{ isTemporary() method
	/**
	 * Returns if this is a temporary buffer. This method is thread-safe.
	 * @see jEdit#openTemporary(View,String,String,boolean)
	 * @see jEdit#commitTemporary(Buffer)
	 * @since jEdit 2.2pre7
	 */
	public boolean isTemporary()
	{
		return getFlag(TEMPORARY);
	} //}}}

	//{{{ getIcon() method
	/**
	 * Returns this buffer's icon.
	 * @since jEdit 2.6pre6
	 */
	public Icon getIcon()
	{
		if(isDirty())
			return GUIUtilities.loadIcon("dirty.gif");
		else if(isReadOnly())
			return GUIUtilities.loadIcon("readonly.gif");
		else if(getFlag(NEW_FILE))
			return GUIUtilities.loadIcon("new.gif");
		else
			return GUIUtilities.loadIcon("normal.gif");
	} //}}}

	//}}}

	//{{{ Property methods

	//{{{ propertiesChanged() method
	/**
	 * Reloads settings from the properties. This should be called
	 * after the <code>syntax</code> or <code>folding</code>
	 * buffer-local properties are changed.
	 */
	@Override
	public void propertiesChanged()
	{
		super.propertiesChanged();
		if (!autoreloadOverridden)
		{
			setAutoReloadDialog(jEdit.getBooleanProperty("autoReloadDialog"));
			setAutoReload(jEdit.getBooleanProperty("autoReload"));
		}
		if (!isTemporary())
			EditBus.send(new BufferUpdate(this,null,BufferUpdate.PROPERTIES_CHANGED));
	} //}}}

	//{{{ getDefaultProperty() method
	@Override
	public Object getDefaultProperty(String name)
	{
		Object retVal;

		if(mode != null)
		{
			retVal = mode.getProperty(name);
			if(retVal == null)
				return null;

			setDefaultProperty(name,retVal);
			return retVal;
		}
		// Now try buffer.<property>
		String value = jEdit.getProperty("buffer." + name);
		if(value == null)
			return null;

		// Try returning it as an integer first
		try
		{
			retVal = new Integer(value);
		}
		catch(NumberFormatException nf)
		{
			retVal = value;
		}

		return retVal;
	} //}}}

	//{{{ toggleWordWrap() method
	/**
	 * Toggles word wrap between the three available modes. This is used
	 * by the status bar.
	 * @param view We show a message in the view's status bar
	 * @since jEdit 4.1pre3
	 */
	public void toggleWordWrap(View view)
	{
		String wrap = getStringProperty("wrap");
		if(wrap.equals("none"))
			wrap = "soft";
		else if(wrap.equals("soft"))
			wrap = "hard";
		else if(wrap.equals("hard"))
			wrap = "none";
		view.getStatus().setMessageAndClear(jEdit.getProperty(
			"view.status.wrap-changed",new String[] {
			wrap }));
		setProperty("wrap",wrap);
		propertiesChanged();
	} //}}}

	//{{{ toggleLineSeparator() method
	/**
	 * Toggles the line separator between the three available settings.
	 * This is used by the status bar.
	 * @param view We show a message in the view's status bar
	 * @since jEdit 4.1pre3
	 */
	public void toggleLineSeparator(View view)
	{
		String status = null;
		String lineSep = getStringProperty(LINESEP);
		if("\n".equals(lineSep))
		{
			status = "windows";
			lineSep = "\r\n";
		}
		else if("\r\n".equals(lineSep))
		{
			status = "mac";
			lineSep = "\r";
		}
		else if("\r".equals(lineSep))
		{
			status = "unix";
			lineSep = "\n";
		}
		view.getStatus().setMessageAndClear(jEdit.getProperty(
			"view.status.linesep-changed",new String[] {
			jEdit.getProperty("lineSep." + status) }));
		setProperty(LINESEP, lineSep);
		setDirty(true);
		propertiesChanged();
	} //}}}

	//{{{ getContextSensitiveProperty() method
	/**
	 * Some settings, like comment start and end strings, can
	 * vary between different parts of a buffer (HTML text and inline
	 * JavaScript, for example).
	 * @param offset The offset
	 * @param name The property name
	 * @since jEdit 4.0pre3
	 */
	@Override
	public String getContextSensitiveProperty(int offset, String name)
	{
		Object value = super.getContextSensitiveProperty(offset,name);

		if(value == null)
		{
			ParserRuleSet rules = getRuleSetAtOffset(offset);

			value = jEdit.getMode(rules.getModeName())
				.getProperty(name);

			if(value == null)
				value = mode.getProperty(name);
		}

		if(value == null)
			return null;
		else
			return String.valueOf(value);
	} //}}}

	//}}}

	//{{{ Edit modes, syntax highlighting

	//{{{ setMode() method
	/**
	 * Sets this buffer's edit mode by calling the accept() method
	 * of each registered edit mode.
	 */
	public void setMode()
	{
		Mode mode = null;
		String userMode = getStringProperty("mode");
		if(userMode != null)
		{
			unsetProperty("mode");
			mode = ModeProvider.instance.getMode(userMode);
		}
		String firstLine = getLineText(0);
		if (mode == null)
		{
			mode = ModeProvider.instance.getModeForFile(name, firstLine);
		}
		if (mode != null)
		{
			boolean forceInsensitive = false;
			if (!getFlag(TEMPORARY) && getLength() > jEdit.getIntegerProperty("largeBufferSize", 4000000))
			{
				boolean contextInsensitive = mode.getBooleanProperty("contextInsensitive");
				if (!contextInsensitive)
				{
					JTextPane tp = new JTextPane();
					tp.setEditable(false);
					tp.setText(jEdit.getProperty("largeBufferDialog.message"));
					int i = JOptionPane.showOptionDialog(jEdit.getActiveView(),
						tp,
						jEdit.getProperty("largeBufferDialog.title", new String[]{name}),
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						new String[]{
							jEdit.getProperty("largeBufferDialog.fullSyntax"),
							jEdit.getProperty("largeBufferDialog.contextInsensitive"),
							jEdit.getProperty("largeBufferDialog.defaultMode")},
						jEdit.getProperty("largeBufferDialog.fullSyntax"));
					switch (i)
					{
						case 0:
							// do nothing
							break;
						case 1:
							forceInsensitive = true;
							break;
						case 2:
							mode =  getDefaultMode();
							break;
					}
				}
			}
			setMode(mode, forceInsensitive);
			return;
		}

		Mode defaultMode = getDefaultMode();

		if (defaultMode != null)
			setMode(defaultMode);
	} //}}}

	private static Mode getDefaultMode()
	{
		Mode defaultMode = jEdit.getMode(jEdit.getProperty("buffer.defaultMode"));
		if(defaultMode == null)
			defaultMode = jEdit.getMode("text");
		return defaultMode;
	}

	//}}}

	//{{{ Deprecated methods

	//{{{ getFile() method
	/**
	 * @deprecated Do not call this method, use {@link #getPath()}
	 * instead.
	 */
	@Deprecated
	public File getFile()
	{
		return file;
	} //}}}

	//}}}

	//{{{ Marker methods

	//{{{ getMarkers() method
	/**
	 * Returns a vector of markers.
	 * @since jEdit 3.2pre1
	 */
	public Vector<Marker> getMarkers()
	{
		return markers;
	} //}}}

	//{{{ getMarkerStatusPrompt() method
	/**
	 * Returns the status prompt for the given marker action. Only
	 * intended to be called from <code>actions.xml</code>.
	 * @since jEdit 4.2pre2
	 */
	public String getMarkerStatusPrompt(String action)
	{
		return jEdit.getProperty("view.status." + action,
			new String[] { getMarkerNameString() });
	} //}}}

	//{{{ getMarkerNameString() method
	/**
	 * Returns a string of all set markers, used by the status bar
	 * (eg, "a b $ % ^").
	 * @since jEdit 4.2pre2
	 */
	public String getMarkerNameString()
	{
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = markers.get(i);
			if(marker.getShortcut() != '\0')
			{
				if(buf.length() != 0)
					buf.append(' ');
				buf.append(marker.getShortcut());
			}
		}

		if(buf.length() == 0)
			return jEdit.getProperty("view.status.no-markers");
		else
			return buf.toString();
	} //}}}

	//{{{ addOrRemoveMarker() method
	/**
	 * If a marker is set on the line of the position, it is removed. Otherwise
	 * a new marker with the specified shortcut is added.
	 * @param pos The position of the marker
	 * @param shortcut The shortcut ('\0' if none)
	 * @since jEdit 3.2pre5
	 */
	public void addOrRemoveMarker(char shortcut, int pos)
	{
		int line = getLineOfOffset(pos);
		if(getMarkerAtLine(line) != null)
			removeMarker(line);
		else
			addMarker(shortcut,pos);
	} //}}}

	//{{{ addMarker() method
	/**
	 * Adds a marker to this buffer.
	 * @param pos The position of the marker
	 * @param shortcut The shortcut ('\0' if none)
	 * @since jEdit 3.2pre1
	 */
	public void addMarker(char shortcut, int pos)
	{
		Marker markerN = new Marker(this,shortcut,pos);
		boolean added = false;

		// don't sort markers while buffer is being loaded
		if(isLoaded())
		{
			setFlag(MARKERS_CHANGED,true);

			markerN.createPosition();

			for(int i = 0; i < markers.size(); i++)
			{
				Marker marker = markers.get(i);
				if(shortcut != '\0' && marker.getShortcut() == shortcut)
					marker.setShortcut('\0');

				if(marker.getPosition() == pos)
				{
					markers.removeElementAt(i);
					i--;
				}
			}

			for(int i = 0; i < markers.size(); i++)
			{
				Marker marker = markers.get(i);
				if(marker.getPosition() > pos)
				{
					markers.insertElementAt(markerN,i);
					added = true;
					break;
				}
			}
		}

		if(!added)
			markers.addElement(markerN);

		if(isLoaded() && !getFlag(TEMPORARY))
		{
			EditBus.send(new BufferUpdate(this,null,
				BufferUpdate.MARKERS_CHANGED));
		}
	} //}}}

	//{{{ getMarkerInRange() method
	/**
	 * Returns the first marker within the specified range.
	 * @param start The start offset
	 * @param end The end offset
	 * @since jEdit 4.0pre4
	 */
	public Marker getMarkerInRange(int start, int end)
	{
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = markers.get(i);
			int pos = marker.getPosition();
			if(pos >= start && pos < end)
				return marker;
		}

		return null;
	} //}}}

	//{{{ getMarkerAtLine() method
	/**
	 * Returns the first marker at the specified line, or <code>null</code>
	 * if there is none.
	 * @param line The line number
	 * @since jEdit 3.2pre2
	 */
	public Marker getMarkerAtLine(int line)
	{
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = markers.get(i);
			if(getLineOfOffset(marker.getPosition()) == line)
				return marker;
		}

		return null;
	} //}}}

	//{{{ removeMarker() method
	/**
	 * Removes all markers at the specified line.
	 * @param line The line number
	 * @since jEdit 3.2pre2
	 */
	public void removeMarker(int line)
	{
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = markers.get(i);
			if(getLineOfOffset(marker.getPosition()) == line)
			{
				setFlag(MARKERS_CHANGED,true);
				marker.removePosition();
				markers.removeElementAt(i);
				i--;
			}
		}

		EditBus.send(new BufferUpdate(this,null,
			BufferUpdate.MARKERS_CHANGED));
	} //}}}

	//{{{ removeAllMarkers() method
	/**
	 * Removes all defined markers.
	 * @since jEdit 2.6pre1
	 */
	public void removeAllMarkers()
	{
		setFlag(MARKERS_CHANGED,true);

		for(int i = 0; i < markers.size(); i++)
			markers.get(i).removePosition();

		markers.removeAllElements();

		if(isLoaded())
		{
			EditBus.send(new BufferUpdate(this,null,
				BufferUpdate.MARKERS_CHANGED));
		}
	} //}}}

	//{{{ getMarker() method
	/**
	 * Returns the marker with the specified shortcut.
	 * @param shortcut The shortcut
	 * @since jEdit 3.2pre2
	 */
	public Marker getMarker(char shortcut)
	{
		for (Marker marker : markers)
		{
			if(marker.getShortcut() == shortcut)
				return marker;
		}
		return null;
	} //}}}

	//{{{ getMarkersPath() method
	/**
	 * Returns the path for this buffer's markers file
	 * @param vfs The appropriate VFS
	 * @param path the path of the buffer, it can be different from the field
	 * when using save-as
	 * @since jEdit 4.3pre10
	 */
	public static String getMarkersPath(VFS vfs, String path)
	{
		return vfs.getParentOfPath(path)
			+ '.' + vfs.getFileName(path)
			+ ".marks";
	} //}}}

	//{{{ updateMarkersFile() method
	/**
	 * Save the markers file, or delete it when there are mo markers left
	 * Handling markers is now independent from saving the buffer.
	 * Changing markers will not set the buffer dirty any longer.
	 * @param view The current view
	 * @since jEdit 4.3pre7
	 */
	public boolean updateMarkersFile(View view)
	{
		if(!markersChanged())
			return true;
		// adapted from VFS.save
		VFS vfs = VFSManager.getVFSForPath(getPath());
		if (((vfs.getCapabilities() & VFS.WRITE_CAP) == 0) ||
		    !vfs.isMarkersFileSupported())
		{
			VFSManager.error(view, path, "vfs.not-supported.save",
				new String[] { "markers file" });
			return false;
		}
		Object session = vfs.createVFSSession(path, view);
		if(session == null)
			return false;
		ThreadUtilities.runInBackground(
			new MarkersSaveRequest(
				view, this, session, vfs, path));
		return true;
	} //}}}

	//{{{ markersChanged() method
	/**
	 * Return true when markers have changed and the markers file needs
	 * to be updated
	 * @since jEdit 4.3pre7
	 */
	public boolean markersChanged()
	{
		return getFlag(MARKERS_CHANGED);
	} //}}}

	//{{{ setMarkersChanged() method
	/**
	 * Sets/unsets the MARKERS_CHANGED flag
	 * @since jEdit 4.3pre7
	 */
	public void setMarkersChanged(boolean changed)
	{
		setFlag(MARKERS_CHANGED, changed);
	} //}}}

	//}}}

	//{{{ Miscellaneous methods

	//{{{ setWaitSocket() method
	/**
	 * This socket is closed when the buffer is closed.
	 */
	public void setWaitSocket(Socket waitSocket)
	{
		this.waitSocket = waitSocket;
	} //}}}

	//{{{ getNext() method
	/**
	 * Returns the next buffer in the list.
	 */
	public Buffer getNext()
	{
		return next;
	} //}}}

	//{{{ getPrev() method
	/**
	 * Returns the previous buffer in the list.
	 */
	public Buffer getPrev()
	{
		return prev;
	} //}}}

	//{{{ getIndex() method
	/**
	 * Returns the position of this buffer in the buffer list.
	 */
	public int getIndex()
	{
		int count = 0;
		Buffer buffer = prev;
		while (true)
		{
			if(buffer == null)
				break;
			count++;
			buffer = buffer.prev;
		}
		return count;
	} //}}}

	//{{{ toString() method
	/**
	 * Returns a string representation of this buffer.
	 * This simply returns the path name.
	 */
	@Override
	public String toString()
	{
		return name + " (" + MiscUtilities.abbreviate(directory) + ')';
	} //}}}

	//{{{ addBufferUndoListener() method
	/**
	 * Adds a buffer undo listener.
	 * @param listener The listener
	 * @since jEdit 4.3pre18
	 */
	public void addBufferUndoListener(BufferUndoListener listener)
	{
		undoListeners.add(listener);
	} //}}}

	//{{{ removeBufferUndoListener() method
	/**
	 * Removes a buffer undo listener.
	 * @param listener The listener
	 * @since jEdit 4.3pre18
	 */
	public void removeBufferUndoListener(BufferUndoListener listener)
	{
		undoListeners.remove(listener);
	} //}}}

	//}}}

	//{{{ Package-private members
	/** The previous buffer in the list. */
	Buffer prev;
	/** The next buffer in the list. */
	Buffer next;

	//{{{ Buffer constructor
	Buffer(String path, boolean newFile, boolean temp, Map props)
	{
		super(props);

		markers = new Vector<Marker>();

		setFlag(TEMPORARY,temp);

		// this must be called before any EditBus messages are sent
		setPath(path);

		/* Magic: UNTITLED is only set if newFile param to
		 * constructor is set, NEW_FILE is also set if file
		 * doesn't exist on disk.
		 *
		 * This is so that we can tell apart files created
		 * with jEdit.newFile(), and those that just don't
		 * exist on disk.
		 *
		 * Why do we need to tell the difference between the
		 * two? jEdit.addBufferToList() checks if the only
		 * opened buffer is an untitled buffer, and if so,
		 * replaces it with the buffer to add. We don't want
		 * this behavior to occur with files that don't
		 * exist on disk; only untitled ones.
		 */
		setFlag(UNTITLED,newFile);
		setFlag(NEW_FILE,newFile);
		setFlag(AUTORELOAD,jEdit.getBooleanProperty("autoReload"));
		setFlag(AUTORELOAD_DIALOG,jEdit.getBooleanProperty("autoReloadDialog"));

		undoListeners = new Vector<BufferUndoListener>();
	} //}}}

	//{{{ commitTemporary() method
	void commitTemporary()
	{
		setFlag(TEMPORARY,false);

		finishLoading();
	} //}}}

	//{{{ close() method
	void close()
	{
		setFlag(CLOSED,true);

		if(autosaveFile != null)
			autosaveFile.delete();

		// notify clients with -wait
		if(waitSocket != null)
		{
			try
			{
				waitSocket.getOutputStream().write('\0');
				waitSocket.getOutputStream().flush();
				waitSocket.getInputStream().close();
				waitSocket.getOutputStream().close();
				waitSocket.close();
			}
			catch(IOException io)
			{
				//Log.log(Log.ERROR,this,io);
			}
		}
	} //}}}

	//}}}

	//{{{ Protected members

	//{{{ fireBeginUndo() method
	protected void fireBeginUndo()
	{
		for (BufferUndoListener listener: undoListeners)
		{
			try
			{
				listener.beginUndo(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer undo event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireEndUndo() method
	protected void fireEndUndo()
	{
		for (BufferUndoListener listener: undoListeners)
		{
			try
			{
				listener.endUndo(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer undo event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireBeginRedo() method
	protected void fireBeginRedo()
	{
		for (BufferUndoListener listener: undoListeners)
		{
			try
			{
				listener.beginRedo(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer begin redo event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}

	//{{{ fireEndRedo() method
	protected void fireEndRedo()
	{
		for (BufferUndoListener listener: undoListeners)
		{
			try
			{
				listener.endRedo(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception while sending buffer end redo event to "+ listener +" :");
				Log.log(Log.ERROR,this,t);
			}
		}
	} //}}}
	//}}}

	//{{{ Private members

	//{{{ Flags

	//{{{ setFlag() method
	private void setFlag(int flag, boolean value)
	{
		if(value)
			flags |= 1 << flag;
		else
			flags &= ~(1 << flag);
	} //}}}

	//{{{ getFlag() method
	private boolean getFlag(int flag)
	{
		int mask = 1 << flag;
		return (flags & mask) == mask;
	} //}}}

	//{{{ getFlag() method
	private boolean isAutoreloadPropertyOverriden()
	{
		return getFlag(AUTORELOAD) != jEdit.getBooleanProperty("autoReload") ||
			getFlag(AUTORELOAD_DIALOG) != jEdit.getBooleanProperty("autoReloadDialog");
	} //}}}

	//{{{ Flag values
	private static final int CLOSED = 0;
	private static final int NEW_FILE = 3;
	private static final int UNTITLED = 4;
	private static final int AUTOSAVE_DIRTY = 5;
	private static final int AUTORELOAD = 6;
	private static final int AUTORELOAD_DIALOG = 7;
	private static final int TEMPORARY = 10;
	private static final int MARKERS_CHANGED = 12;
	//}}}

	private int flags;

	//}}}

	//{{{ Instance variables
	/** Indicate if the autoreload property was overridden */
	private boolean autoreloadOverridden;
	private String path;
	private String symlinkPath;
	private String name;
	private String directory;
	private File file;
	private File autosaveFile;
	private long modTime;
	private byte[] md5hash;
	private int initialLength;
	/**
	 * The longBufferMode is an option
	 */
	private int longBufferMode;

	private final Vector<Marker> markers;

	private Socket waitSocket;
	private List<BufferUndoListener> undoListeners;
	//}}}

	//{{{ setPath() method
	private void setPath(final String path)
	{
		jEdit.visit(new JEditVisitorAdapter()
		{
			@Override
			public void visit(EditPane editPane)
			{
				editPane.bufferRenamed(Buffer.this.path, path);
			}
		});

		this.path = path;
		VFS vfs = VFSManager.getVFSForPath(path);
		if((vfs.getCapabilities() & VFS.WRITE_CAP) == 0)
			setFileReadOnly(true);
		name = vfs.getFileName(path);
		directory = vfs.getParentOfPath(path);

		if(vfs instanceof FileVFS)
		{
			file = new File(path);
			symlinkPath = MiscUtilities.resolveSymlinks(path);

			// if we don't do this, the autosave file won't be
			// deleted after a save as
			if(autosaveFile != null)
				autosaveFile.delete();
			autosaveFile = new File(file.getParent(),'#' + name + '#');
		}
		else
		{
			// I wonder if the lack of this broke anything in the
			// past?
			file = null;
			autosaveFile = null;
			symlinkPath = path;
		}
	} //}}}


	//{{{ recoverAutosave() method
	private boolean recoverAutosave(final View view)
	{
		if(!autosaveFile.canRead())
			return false;

		// this method might get called at startup
		GUIUtilities.hideSplashScreen();

		final Object[] args = { autosaveFile.getPath() };
		int result = GUIUtilities.confirm(view,"autosave-found",args,
			JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);

		if(result == JOptionPane.YES_OPTION)
		{
			VFSManager.getFileVFS().load(view,this,autosaveFile.getPath());

			// show this message when all I/O requests are
			// complete
			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					GUIUtilities.message(view,"autosave-loaded",args);
				}
			});

			return true;
		}
		else
			return false;
	} //}}}

	//{{{ checkFileForLoad() method
	private boolean checkFileForLoad(View view, VFS vfs, String path)
	{
		if((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) != 0)
		{
			Object session = vfs.createVFSSession(path, view);
			if(session == null)
				return false;

			try
			{
				VFSFile file = vfs._getFile(session,path,view);
				if(file == null)
				{
					setNewFile(true);
					return true;
				}

				if(!file.isReadable())
				{
					VFSManager.error(view,path,"ioerror.no-read",null);
					setNewFile(false);
					return false;
				}

				setFileReadOnly(!file.isWriteable());

				if(file.getType() != VFSFile.FILE)
				{
					VFSManager.error(view,path,
						"ioerror.open-directory",null);
					setNewFile(false);
					return false;
				}
			}
			catch(IOException io)
			{
				VFSManager.error(view,path,"ioerror",
					new String[] { io.toString() });
				return false;
			}
			finally
			{
				try
				{
					vfs._endVFSSession(session,view);
				}
				catch(IOException io)
				{
					VFSManager.error(view,path,"ioerror",
						new String[] { io.toString() });
					return false;
				}
			}
		}

		return true;
	} //}}}

	//{{{ checkFileForSave() method
	private static boolean checkFileForSave(View view, VFS vfs, String path)
	{
		if((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) != 0)
		{
			Object session = vfs.createVFSSession(path,view);
			if(session == null)
				return false;

			try
			{
				VFSFile file = vfs._getFile(session,path,view);
				if(file == null)
					return true;

				if(file.getType() != VFSFile.FILE)
				{
					VFSManager.error(view,path,
						"ioerror.save-directory",null);
					return false;
				}
			}
			catch(IOException io)
			{
				VFSManager.error(view,path,"ioerror",
					new String[] { io.toString() });
				return false;
			}
			finally
			{
				try
				{
					vfs._endVFSSession(session,view);
				}
				catch(IOException io)
				{
					VFSManager.error(view,path,"ioerror",
						new String[] { io.toString() });
					return false;
				}
			}
		}

		return true;
	} //}}}

	/** @return an MD5 hash of the contents of the buffer */
	private byte[] calculateHash()
	{
		final byte[] dummy = new byte[1];
		if (!jEdit.getBooleanProperty("useMD5forDirtyCalculation"))
			return dummy;
		return StandardUtilities.md5(getText());
	}

	/** Update the buffer's members with the current hash and length,
	 *  for later comparison.
	 */
	private void updateHash()
	{
		initialLength = getLength();
		md5hash = calculateHash();
	}

	//{{{ finishLoading() method
	private void finishLoading()
	{
		updateHash();

		parseBufferLocalProperties();
		// AHA!
		// this is probably the only way to fix this
		FoldHandler oldFoldHandler = getFoldHandler();
		setMode();

		if(getFoldHandler() == oldFoldHandler)
		{
			// on a reload, the fold handler doesn't change, but
			// we still need to re-collapse folds.
			// don't do this on initial fold handler creation
			invalidateFoldLevels();

			fireFoldHandlerChanged();
		}

		// Create marker positions
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = markers.get(i);
			marker.removePosition();
			int pos = marker.getPosition();
			if(pos > getLength())
				marker.setPosition(getLength());
			else if(pos < 0)
				marker.setPosition(0);
			marker.createPosition();
		}
	} //}}}

	//{{{ finishSaving() method
	private void finishSaving(View view, String oldPath,
		String oldSymlinkPath, String path,
		boolean rename, boolean error)
	{

		//{{{ Set the buffer's path
		// Caveat: won't work if save() called with a relative path.
		// But I don't think anyone calls it like that anyway.
		if(!error && !path.equals(oldPath))
		{
			Buffer buffer = jEdit.getBuffer(path);

			if(rename)
			{
				/* if we save a file with the same name as one
				 * that's already open, we presume that we can
				 * close the existing file, since the user
				 * would have confirmed the overwrite in the
				 * 'save as' dialog box anyway */
				if(buffer != null && /* can't happen? */
					!buffer.getPath().equals(oldPath))
				{
					buffer.setDirty(false);
					jEdit.closeBuffer(view,buffer);
				}

				setPath(path);
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane)
					{
						BufferSet bufferSet = editPane.getBufferSet();
						if (bufferSet.indexOf(Buffer.this) != -1)
						{
							bufferSet.sort();
						}
					}
				});
			}
			else
			{
				/* if we saved over an already open file using
				 * 'save a copy as', then reload the existing
				 * buffer */
				if(buffer != null && /* can't happen? */
					!buffer.getPath().equals(oldPath))
				{
					buffer.load(view,true);
				}
			}
		} //}}}

		//{{{ Update this buffer for the new path
		if(rename)
		{
			if(file != null)
				modTime = file.lastModified();

			if(!error)
			{
				// we do a write lock so that the
				// autosave, which grabs a read lock,
				// is not executed between the
				// deletion of the autosave file
				// and clearing of the dirty flag
				try
				{
					writeLock();

					if(autosaveFile != null)
						autosaveFile.delete();

					setFlag(AUTOSAVE_DIRTY,false);
					setFileReadOnly(false);
					setFlag(NEW_FILE,false);
					setFlag(UNTITLED,false);
					super.setDirty(false);
					if(jEdit.getBooleanProperty("resetUndoOnSave"))
					{
						undoMgr.clear();
					}
				}
				finally
				{
					writeUnlock();
				}

				parseBufferLocalProperties();

				if(!getPath().equals(oldPath))
				{
					if (!isTemporary())
						jEdit.updatePosition(oldSymlinkPath,this);
					setMode();
				}
				else
				{
					// if user adds mode buffer-local property
					String newMode = getStringProperty("mode");
					if(newMode != null &&
						!newMode.equals(getMode()
						.getName()))
						setMode();
					else
						propertiesChanged();
				}

				updateHash();

				if (!isTemporary())
				{
					EditBus.send(new BufferUpdate(this,
								      view,BufferUpdate.DIRTY_CHANGED));

					// new message type introduced in 4.0pre4
					EditBus.send(new BufferUpdate(this,
								      view,BufferUpdate.SAVED));
				}
			}
		} //}}}
	} //}}}

	//}}}
}
