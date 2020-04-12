/*
 * CopyFileWorker.java - a worker that will copy a file
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
	public enum Behavior
	{ 
		/** Do not copy file. */
		SKIP, 
	
		/** Overwrite existing file. */
		OVERWRITE, 
	
		/** Rename existing file. */
		RENAME 
	}

	private String source;

	private final Component comp;

	private List<String> sources;

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
	 * @param latch a latch so the caller knows when the copy is done
	 */
	private CopyFileWorker(Component comp, @Nonnull String source, @Nonnull String target, @Nullable CountDownLatch latch)
	{
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
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
	 * If some files already exist in the target directory the files will 
	 * be skipped.
	 * @param comp   the component that will be used as parent in case of error
	 * @param sources the sources path to copy
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, List<String> sources, String target)
	{
		this(comp, sources, target, Behavior.SKIP);
	}

	/**
	 * Copy all files from the list to the target directory.
	 * If some files already exist in the target directory the <code>Behavior</code> will decide what
	 * to do.
	 * @param comp   the component that will be used as parent in case of error
	 * @param sources the sources path to copy
	 * @param target the target path (it must be a directory otherwise nothing will be copied)
	 * @param behavior the behavior if the target file already exists
	 * @since jEdit 5.0
	 */
	public CopyFileWorker(Component comp, @Nonnull List<String> sources, @Nonnull String target, Behavior behavior)
	{
		Objects.requireNonNull(sources);
		Objects.requireNonNull(target);
		this.comp = comp;
		this.sources = sources;
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
		Object targetSession = null;
		try
		{
			targetSession = vfs.createVFSSession(target, comp);
			if (targetSession == null)
			{
				Log.log(Log.ERROR, this, "Target VFS path cannot be reached");
				return;
			}
			VFSFile targetFile = vfs._getFile(targetSession, target, comp);
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
			if (sources != null)
			{
				setMaximum(sources.size());
				for (int i = 0; i < sources.size(); i++)
				{
					setValue(i);
					String sourcePath = sources.get(i);
					String sourceName = MiscUtilities.getFileName(sourcePath);
					setLabel(sourceName);
					copy(targetSession, vfs, sourcePath, sourceName, target);
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
				if (targetSession != null)
					vfs._endVFSSession(targetSession, comp);
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
	@Nullable
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
		String nameNoExtension = MiscUtilities.getBaseName(baseName);
		for (int i = 1;i<1000;i++)
		{
			String name = nameNoExtension + "-copy-" + i + extension;
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
