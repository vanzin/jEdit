/*
 * ListVFSFileTransferable.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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
package org.gjt.sp.jedit.datatransfer;

import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.io.VFSFile;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.4.x
 */
public class ListVFSFileTransferable implements Transferable
{
	public static final DataFlavor jEditFileList = new DataFlavor(List.class, "application/x-java-jEdit-list-vfsfile");
	public static final DataFlavor[] supported = {jEditFileList, DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor};
	
	private final List<VFSFile> files;

	public ListVFSFileTransferable(VFSFile[] files)
	{
		this.files = Collections.unmodifiableList(Arrays.asList(files));
	}

	public DataFlavor[] getTransferDataFlavors()
	{
		return supported;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return jEditFileList.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		if (jEditFileList.equals(flavor))
		{
			return files;
		}
		else if (DataFlavor.stringFlavor.equals(flavor))
		{
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < files.size(); i++)
			{
				VFSFile vfsFile = files.get(i);
				if (i != 0)
					builder.append('\n');
				builder.append(vfsFile);
			}
			return builder.toString();
		}
		else if (DataFlavor.javaFileListFlavor.equals(flavor))
		{
			List<File> files = new ArrayList<File>(this.files.size());
			for (VFSFile file : this.files)
			{
				if (file.getVFS() instanceof FileVFS)
					files.add(new File(file.getPath()));
			}
			return files;
		}
		throw new UnsupportedFlavorException(flavor);
	}
}
