/*
 * TransferHandler.java
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

import org.gjt.sp.jedit.textarea.TextArea;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.4.x
 */
public class TransferHandler
{
	private static final TransferHandler instance = new TransferHandler();

	private final List<JEditTransferableService> services;

	private TransferHandler()
	{
		services = new ArrayList<JEditTransferableService>();
	}

	public static TransferHandler getInstance()
	{
		return instance;
	}

	public void registerTransferableService(JEditTransferableService transferableService)
	{
		if (!services.contains(transferableService))
			services.add(transferableService);
	}

	public Transferable getTransferable(TextArea textArea, String text)
	{
		Map<DataFlavor, Transferable> flavors = new HashMap<DataFlavor, Transferable>();
		for (JEditTransferableService service : services)
		{
			if (service.accept(textArea, text))
			{
				Transferable t = service.getTransferable(textArea, text);
				DataFlavor[] supportedDataFlavor = t.getTransferDataFlavors();
				for (DataFlavor dataFlavor : supportedDataFlavor)
				{
					flavors.put(dataFlavor, t);
				}
			}
		}
		 Transferable transferable = new JEditTransferable(flavors);

		return transferable;
	}
}
