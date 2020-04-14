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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The buffer manager is here to manage the {@link Buffer} linked list.
 *
 * @author Matthieu Casanova
 * @since jEdit 5.6pre1
 * @version $Id: jEdit.java 25120 2020-04-03 14:58:39Z kpouer $
 */
public interface BufferManager
{
	List<Buffer> getBuffers();

	List<Buffer> getTitledBuffers();

	List<Buffer> getUntitledBuffers();

	List<Buffer> getNonUntitledDirtyBuffers();

	List<Buffer> getDirtyBuffers();

	void forEach(Consumer<? super Buffer> action);

	int size();

	Optional<Buffer> getBuffer(String path);

	int getNextUntitledBufferId();

	Buffer getFirst();
}
