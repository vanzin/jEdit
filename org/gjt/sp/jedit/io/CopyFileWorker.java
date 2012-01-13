/*
 * CopyFileWorker.java - a worker that will copy a file
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008, 2012 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */

package org.gjt.sp.jedit.io;

//{{{ Imports
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * This worker will copy a file. Be careful it override files if the target
 * already exists
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre13
 */
public class CopyFileWorker extends Task
{
	/**
	 * The behavior if the target already exists
	 * @since jEdit 5.0
	 */
	enum Behavior { SKIP, OVERWRITE, RENAME }

	private String source;

	private final Component comp;

	private VFSFile[] vfsFiles;
	private File[] files;
	
	/**
	 * The behavior if the target already exists.
	 */
	private final Behavior behavior;

	private final String target;

	/**
	 * A latch to tell when the copy is finished
	 */
	private CountDownLatch latch;

	//{{{ CopyFileWorker constructors
	/**
	 * Copy a file. Careful, it will <b>overwrite</b> the target.
	 * @param comp   the component that will be used as parent in case of error
	 * @param source the source path
	 * @param target the target path (it is the file path, not a parent directory)
	 */
	public CopyFileWorker(Component comp, String source, String target)
	{
		this(comp, source, target, null);
	}

	/**
	 * Copy a file. Careful, it will <b>overwrite</b> the target.
	 * @param comp   the component that will be used as parent in case of error
	 * @param source the source path
	 * @param target the target path (it is the file path, not a parent directory)
	 */
	private CopyFileWorker(Component comp, String source, String target, CountDownLatch latch)
	{
		if (source == null || target == null)
			throw new NullPointerException("The source and target cannot be null");
		if (source.equals(target))
			throw new IllegalArgumentException("The source and target must not be the same");
		this.comp = comp;
		this.source = source;
		this.target = target;
		behavior = Behavior.OVERWRITE;
		this.latch = latch;
		setLabel("Copy " + source + " to " + target);
	}

	/**
	 * Copy all files from the list to the target directory.
	 * If some files already exist in the target directory the {@link Behavior} will decide what
	 * to do.
	 * If a target filename already exists it will be skipped.
	 * @param comp   the component that will be used as parent in case of error
	 * @param vfsFiles the source files
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, VFSFile[] vfsFiles, String target)
	{
		this(comp, vfsFiles, target, Behavior.SKIP);
	}

	/**
	 * Copy all files from the list to the target directory.
	 * If some files already exist in the target directory the {@link Behavior} will decide what
	 * to do.
	 * @param comp   the component that will be used as parent in case of error
	 * @param vfsFiles the source files
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @param behavior the behavior if the target file already exists
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, VFSFile[] vfsFiles, String target, Behavior behavior)
	{
		if (vfsFiles == null || target == null)
			throw new NullPointerException("The source and target cannot be null");
		this.comp = comp;
		this.vfsFiles = vfsFiles;
		this.target = target;
		this.behavior = behavior;
	}

	/**
	 * Copy all files from the list to the target directory.
	 * If a target filename already exists it will be skipped.
	 * @param comp   the component that will be used as parent in case of error
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, File[] files, String target)
	{
		this(comp, files, target, Behavior.OVERWRITE);
	}

	/**
	 * Copy all files from the list to the target directory.
	 * If some files already exist in the target directory the {@link Behavior} will decide what
	 * to do.
	 * @param comp   the component that will be used as parent in case of error
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @param behavior the behavior if the target file already exists
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, File[] files, String target, Behavior behavior)
	{
		if (files == null || target == null)
			throw new NullPointerException("The source and target cannot be null");
		this.comp = comp;
		this.files = files;
		this.target = target;
		this.behavior = behavior;
	} //}}}

	//{{{ _run() method
	@Override
	public void _run()
	{
		Log.log(Log.DEBUG, this, this + ".run()");
		if (source != null)
		{
			// single file copy
			try
			{
				VFS.copy(this, source, target, comp, false, false);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, e, e);
			}
			finally
			{
				if (latch != null)
					latch.countDown();
			}
		}
		else
		{
			// List file copy
			copyFileList();
		}
	} //}}}

	//{{{ copyFileList() method
	private void copyFileList()
	{
		VFS vfs = VFSManager.getVFSForPath(target);
		Object session = null;
		try
		{
			session = vfs.createVFSSession(target, comp);
			if (session == null)
			{
				Log.log(Log.ERROR, this, "Target VFS path cannot be reached");
				return;
			}
			VFSFile targetFile = vfs._getFile(session, target, comp);
			if (targetFile == null)
			{
				Log.log(Log.ERROR, this, "Target is unreachable or do not exist");
				return;
			}
			
			if (targetFile.getType() != VFSFile.DIRECTORY)
			{
				Log.log(Log.ERROR, this, "Target is not a directory");
				return;
			}

			if (vfsFiles != null)
			{
				setMaximum(vfsFiles.length);
				for (int i = 0, vfsFilesLength = vfsFiles.length; i < vfsFilesLength; i++)
				{
					setValue(i);
					VFSFile f = vfsFiles[i];
					if (f.getType() == VFSFile.FILE)
					{
						String sourcePath = f.getPath();
						String sourceName = f.getName();
						setLabel(sourceName);
						copy(session, vfs, sourcePath, sourceName, target);
					}
				}
			}
			if (files != null)
			{
				setMaximum(files.length);

				for (int i = 0, filesLength = files.length; i < filesLength; i++)
				{
					setValue(i);
					File f = files[i];
					if (f.isFile())
					{
						String sourcePath = f.getAbsolutePath();
						String sourceName = f.getName();
						setLabel(sourceName);
						copy(session, vfs, sourcePath, sourceName, target);
					}
				}
			}
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, e);
		}
		catch (InterruptedException e)
		{
			Log.log(Log.WARNING, this, "Copy was interrupted");
		}
		finally
		{
			VFSManager.sendVFSUpdate(vfs, target, true);
			try
			{
				if (session != null)
					vfs._endVFSSession(session, comp);
			}
			catch (IOException e)
			{
			}
		}
	} //}}}

	//{{{ copy() method
	private void copy(Object vfsSession, VFS vfs, String sourcePath, String sourceName, String targetPath)
		throws  IOException, InterruptedException
	{
		String name = getTargetName(vfsSession, vfs, targetPath, sourceName);
		if (name == null)
		{
			return;
		}
		String targetName = MiscUtilities.constructPath(targetPath, name);
		CountDownLatch latch = new CountDownLatch(1);
		ThreadUtilities.runInBackground(new CopyFileWorker(comp, sourcePath, targetName, latch));
		latch.await();
	} //}}}

	//{{{ getTargetName() method
	private String getTargetName(Object session, VFS vfs, String path, String baseName) throws IOException
	{
		if (behavior == Behavior.OVERWRITE)
		{
			// We want to overwrite, no need to check anything
			return baseName;
		}

		String s = MiscUtilities.constructPath(target, baseName);
		VFSFile file = vfs._getFile(session, s, comp);
		if (file == null)
		{
			// The target file do not exist, perfect
			return baseName;
		}
		if (behavior == Behavior.SKIP)
			return null;


		String extension = MiscUtilities.getFileExtension(baseName);
		String nameNoExtension = MiscUtilities.getFileNameNoExtension(baseName);
		for (int i = 1;i<1000;i++)
		{
			String name = nameNoExtension + "-copy-" + i;
			if (extension != null)
				name += extension;
			s = MiscUtilities.constructPath(path, name);
			file = vfs._getFile(session, s, comp);
			if (file == null)
				return name;
		}
		return null;
	} //}}}

	@Override
	public String toString()
	{
		return "CopyFileWorker[" + source + ',' + target + ']';
	}
}
