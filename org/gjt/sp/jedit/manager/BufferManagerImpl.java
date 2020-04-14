/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.manager;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.FilesChangedDialog;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.textarea.DisplayManager;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.util.AwtRunnableQueue;
import org.gjt.sp.util.StandardUtilities;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Integer.parseInt;

/**
 * This class contains buffer management code, those methods are not public and must only be used by jEdit.
 * The public interface is {@link BufferManager}
 *
 * @author Matthieu Casanova
 * @since 5.6pre1
 * @version $Id: jEdit.java 25120 2020-04-03 14:58:39Z kpouer $
 */
public class BufferManagerImpl implements BufferManager
{
	public static final Buffer[] EMPTY_BUFFER_ARRAY = new Buffer[0];

	// makes openTemporary() thread-safe
	private final Object bufferListLock = new Object();

	private final Map<String, Buffer> bufferHash;
	private boolean sortBuffers;
	private boolean sortByName;
	private int bufferCount;
	private Buffer buffersFirst;
	private Buffer buffersLast;

	public BufferManagerImpl()
	{
		bufferHash = new HashMap<>();
	}

	public Object getBufferListLock()
	{
		return bufferListLock;
	}

	//{{{ getBuffers() methods
	/**
	 * Returns an array of all open buffers from any View.
	 * This method is synchronized on the bufferList, if you expect your action might be slow
	 * @return a list of all open buffers
	 * @see View#getBuffers()
	 */
	@Override
	@GuardedBy("bufferListLock")
	public List<Buffer> getBuffers()
	{
		List<Buffer> buffers = new ArrayList<>(bufferCount);
		forEach(buffers::add);
		return buffers;
	}

	@GuardedBy("bufferListLock")
	public List<Buffer> getBuffers(Predicate<Buffer> predicate)
	{
		List<Buffer> buffers = new ArrayList<>(bufferCount);
		forEach(buffer -> { if (predicate.test(buffer)) buffers.add(buffer); });
		return buffers;
	} //}}}

	@Override
	public List<Buffer> getTitledBuffers()
	{
		return getBuffers(Buffer::isTitled);
	}

	@Override
	public List<Buffer> getUntitledBuffers()
	{
		return getBuffers(Buffer::isUntitled);
	}

	@Override
	public List<Buffer> getNonUntitledDirtyBuffers()
	{
		Predicate<Buffer> isTitled = Buffer::isTitled;
		Predicate<Buffer> isDirty = Buffer::isDirty;
		return getBuffers(isTitled.and(isDirty));
	}

	@Override
	public List<Buffer> getDirtyBuffers()
	{
		return getBuffers(Buffer::isDirty);
	}

	//{{{ forEach() method
	/**
	 * Performs the given action for each buffer.
	 * This method is synchronized on the bufferList, if you expect your action might be slow,
	 * you can retrieve an array of the buffers.
	 *
	 * @param action The action to be performed for each element
	 * @throws NullPointerException if the specified action is null
	 */
	@Override
	@GuardedBy("bufferListLock")
	public void forEach(Consumer<? super Buffer> action)
	{
		synchronized(bufferListLock)
		{
			Buffer buffer = buffersFirst;
			for(int i = 0; i < bufferCount; i++)
			{
				action.accept(buffer);
				buffer = buffer.getNext();
			}
		}
	} //}}}

	//{{{ size() method
	/**
	 * Returns the number of open buffers.
	 */
	@Override
	public int size()
	{
		return bufferCount;
	} //}}}

	//{{{ getFirstBuffer() method
	@Override
	public Buffer getFirst()
	{
		return buffersFirst;
	} //}}}

	//{{{ getLast() method
	public Buffer getLast()
	{
		return buffersLast;
	} //}}}

	//{{{ checkBufferStatus() method
	/**
	 * Checks buffer status on disk and shows the dialog box
	 * informing the user that buffers changed on disk, if necessary.
	 * @param view The view
	 * @param currentBuffer indicates whether to check only the current buffer
	 * @param autoReload true if we autoreload
	 * @since jEdit 4.2pre1
	 */
	public void checkBufferStatus(View view, boolean currentBuffer, boolean autoReload)
	{
		// still need to call the status check even if the option is
		// off, so that the write protection is updated if it changes
		// on disk

		// auto reload changed buffers?

		// the problem with this is that if we have two edit panes
		// looking at the same buffer and the file is reloaded both
		// will jump to the same location

		Buffer buffer = buffersFirst;

		int[] states = new int[bufferCount];
		int i = 0;
		boolean notifyFileChanged = false;
		while(buffer != null)
		{
			if(currentBuffer && buffer != view.getBuffer())
			{
				buffer = buffer.getNext();
				i++;
				continue;
			}

			states[i] = buffer.checkFileStatus(view);

			switch(states[i])
			{
				case Buffer.FILE_CHANGED:
					if(buffer.getAutoReload())
					{
						if(buffer.isDirty())
							notifyFileChanged = true;
						else
						{
							buffer.load(view,true);
							// File can be changed into link on disk or vice versa, so update
							// file-path,buffer key value pair in bufferHash
							final Buffer b = buffer;
							AwtRunnableQueue.INSTANCE.runAfterIoTasks(() -> updateBufferHash(b));
						}
					}
					else	// no automatic reload even if general setting is true
						autoReload = false;
					// don't notify user if "do nothing" was chosen
					if(buffer.getAutoReloadDialog())
						notifyFileChanged = true;
					break;
				case Buffer.FILE_DELETED:
					notifyFileChanged = true;
					break;
			}

			buffer = buffer.getNext();
			i++;
		}

		if(notifyFileChanged)
			new FilesChangedDialog(view,states, autoReload);
	} //}}}

	public void setSortBuffers(boolean sortBuffers)
	{
		this.sortBuffers = sortBuffers;
	}

	public void setSortByName(boolean sortByName)
	{
		this.sortByName = sortByName;
	}

	//{{{ getBuffer() method
	/**
	 * Returns the buffer with the specified path name. The path name
	 * must be an absolute path. This method automatically resolves
	 * symbolic links. If performance is critical, cache the canonical
	 * path and call {@link #_getBuffer(String)} instead.
	 *
	 * @param path The path name
	 *
	 * @return the searched buffer, or null if it is not already open
	 *
	 * @see MiscUtilities#constructPath(String,String)
	 * @see MiscUtilities#resolveSymlinks(String)
	 */
	@Override
	public Optional<Buffer> getBuffer(String path)
	{
		return _getBuffer(MiscUtilities.resolveSymlinks(path));
	} //}}}

	//{{{ _getBuffer() method
	/**
	 * Returns the buffer with the specified path name. The path name
	 * must be an absolute, canonical, path.
	 *
	 * @param path The path name
	 *
	 * @return the searched buffer, or null if it is not already open
	 *
	 * @see MiscUtilities#constructPath(String,String)
	 * @see MiscUtilities#resolveSymlinks(String)
	 * @see #getBuffer(String)
	 */
	public Optional<Buffer> _getBuffer(String path)
	{
		// paths on case-insensitive filesystems are stored as lower
		// case in the hash.
		if((VFSManager.getVFSForPath(path).getCapabilities() & VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			path = path.toLowerCase();
		}

		// TODO: danson, this causes ProjectViewer to block, not sure why yet
		synchronized(bufferListLock)
		{
			return Optional.ofNullable(bufferHash.get(path));
		}
	} //}}}

	//{{{ getNextUntitledBufferId() method
	@Override
	public int getNextUntitledBufferId()
	{
		int untitledCount = 0;
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.getName().startsWith("Untitled-"))
			{
				try
				{
					untitledCount = Math.max(untitledCount,
						parseInt(buffer.getName()
							.substring(9)));
				}
				catch(NumberFormatException nf)
				{
				}
			}
			buffer = buffer.getNext();
		}
		return untitledCount + 1;
	} //}}}

	//{{{ updateBufferHash() method
	/**
	 * To be used by jEdit class only
	 */
	public void updateBufferHash(Buffer buffer)
	{
		// Remove path,buffer key,value pair from bufferHash. We use iterator over values
		// to find our buffer i.s.o. removing it with bufferHash.remove(oldPath), because
		// path can be changed (e.g. file changed on disk into link.
		bufferHash.values().removeIf(b -> buffer == b);

		String path = getPathForBufferHash(buffer);

		bufferHash.put(path,buffer);
	} //}}}

	//{{{ removeBufferFromList() method
	public void removeBufferFromList(Buffer buffer)
	{
		synchronized(bufferListLock)
		{
			bufferCount--;

			String path = buffer.getPath();
			if(OperatingSystem.isCaseInsensitiveFS())
				path = path.toLowerCase();

			bufferHash.remove(path);

			if(buffer == buffersFirst && buffer == buffersLast)
			{
				buffersFirst = buffersLast = null;
				return;
			}

			if(buffer == buffersFirst)
			{
				buffersFirst = buffer.getNext();
				buffer.getNext().setPrev(null);
			}
			else
			{
				if (buffer.getPrev() != null)
					buffer.getPrev().setNext(buffer.getNext());
			}

			if(buffer == buffersLast)
			{
				buffersLast = buffersLast.getPrev();
				buffer.getPrev().setNext(null);
			}
			else
			{
				if (buffer.getNext() != null)
					buffer.getNext().setPrev(buffer.getPrev());
			}

			// fixes the hang that can occur if we 'save as' to a new
			// filename which requires re-sorting
			buffer.setNext(null);
			buffer.setPrev(null);
		}
	} //}}}

	//{{{ addBufferToList() method
	public void addBufferToList(Buffer buffer)
	{
		synchronized(bufferListLock)
		{
			String symlinkPath = getPathForBufferHash(buffer);

			bufferCount++;

			bufferHash.put(symlinkPath,buffer);

			if(buffersFirst == null)
			{
				buffersFirst = buffersLast = buffer;
				return;
			}
			//{{{ Sort buffer list
			else if(sortBuffers)
			{
				String str11, str12;
				if(sortByName)
				{
					str11 = buffer.getName();
					str12 = buffer.getDirectory();
				}
				else
				{
					str11 = buffer.getDirectory();
					str12 = buffer.getName();
				}

				Buffer _buffer = buffersFirst;
				while(_buffer != null)
				{
					String str21, str22;
					if(sortByName)
					{
						str21 = _buffer.getName();
						str22 = _buffer.getDirectory();
					}
					else
					{
						str21 = _buffer.getDirectory();
						str22 = _buffer.getName();
					}

					int comp = StandardUtilities.compareStrings(str11,str21,true);
					if(comp < 0 || (comp == 0 && StandardUtilities.compareStrings(str12,str22,true) < 0))
					{
						buffer.setNext(_buffer);
						buffer.setPrev(_buffer.getPrev());
						_buffer.setPrev(buffer);
						if(_buffer != buffersFirst)
							buffer.getPrev().setNext(buffer);
						else
							buffersFirst = buffer;
						return;
					}

					_buffer = _buffer.getNext();
				}
			} //}}}

			buffer.setPrev(buffersLast);
			// fixes the hang that can occur if we 'save as' to a
			// new filename which requires re-sorting
			buffer.setNext(null);
			buffersLast.setNext(buffer);
			buffersLast = buffer;
		}
	} //}}}

	//{{{ updatePosition() method
	/**
	 * If buffer sorting is enabled, this repositions the buffer.
	 */
	public void updatePosition(String oldPath, Buffer buffer)
	{
		if((VFSManager.getVFSForPath(oldPath).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			oldPath = oldPath.toLowerCase();
		}

		bufferHash.remove(oldPath);

		String path = getPathForBufferHash(buffer);

		bufferHash.put(path,buffer);

		if(sortBuffers)
		{
			removeBufferFromList(buffer);
			addBufferToList(buffer);
		}
	} //}}}

	//{{{ getPathForBufferHash() method
	public void removeBuffer(Buffer buffer)
	{
		String path = getPathForBufferHash(buffer);
		bufferHash.remove(path);
		removeBufferFromList(buffer);
	} //}}}

	//{{{ getPathForBufferHash() method
	private static String getPathForBufferHash(Buffer buffer)
	{
		String path = buffer.getSymlinkPath();
		if ((VFSManager.getVFSForPath(path).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			path = path.toLowerCase();
		}
		return path;
	} //}}}

	//{{{ closeAllBuffers() method
	public void closeAllBuffers(View view, boolean isExiting, boolean autosaveUntitled, boolean saveRecent, boolean persistentMarkers)
	{
		// close remaining buffers (the close dialog only deals with
		// dirty ones)

		Buffer buffer = buffersFirst;

		// zero it here so that BufferTabs doesn't have any problems
		buffersFirst = buffersLast = null;
		bufferHash.clear();
		bufferCount = 0;

		while(buffer != null)
		{
			if((!buffer.isNewFile() || (buffer.isUntitled() && autosaveUntitled)) && saveRecent)
			{
				Integer _caret = (Integer)buffer.getProperty(Buffer.CARET);
				int caret = _caret == null ? 0 : _caret;
				BufferHistory.setEntry(buffer.getPath(),caret,
					(Selection[])buffer.getProperty(Buffer.SELECTION),
					buffer.getStringProperty(JEditBuffer.ENCODING),
					buffer.getMode().getName());
			}

			// do not delete untitled buffer when started with background
			if(!isExiting && !(buffer.isUntitled() && autosaveUntitled))
			{
				EditBus.send(new BufferUpdate(buffer,view,BufferUpdate.CLOSING));
			}

			buffer.close();
			DisplayManager.bufferClosed(buffer);
			// do not delete untitled buffer when started with background
			if(!isExiting && !(buffer.isUntitled() && autosaveUntitled))
			{
				jEdit.getBufferSetManager().removeBuffer(buffer);
				EditBus.send(new BufferUpdate(buffer,view, BufferUpdate.CLOSED));
			}
			if(jEdit.getBooleanProperty("persistentMarkers"))
				buffer.updateMarkersFile(view);
			buffer = buffer.getNext();
		}
	} //}}}
}
