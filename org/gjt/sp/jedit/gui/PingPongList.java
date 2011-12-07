/*
 * EncodingsOptionPane.java - Encodings options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010, 2011 Matthieu Casanova
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
package org.gjt.sp.jedit.gui;

//{{{ Imports
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
//}}}

/**
 * @author Matthieu Casanova
 * @since jEdit 4.4pre1
 */
public class PingPongList<E> extends JSplitPane
{
	private final MyListModel<E> leftModel;
	private final MyListModel<E> rightModel;
	private JList left;
	private JList right;
	private JLabel leftLabel;
	private JLabel rightLabel;
	private JPanel leftPanel;
	private JPanel rightPanel;

	//{{{ PingPongList constructors
	public PingPongList(List<E> leftData, List<E> rightData)
	{
		this(JSplitPane.HORIZONTAL_SPLIT, leftData, rightData);
	}

	public PingPongList(int newOrientation, List<E> leftData, List<E> rightData)
	{
		super(newOrientation);
		leftModel = new MyListModel<E>(leftData);
		left = new JList(leftModel);
		rightModel = new MyListModel<E>(rightData);
		right = new JList(rightModel);
		leftPanel = new JPanel(new BorderLayout());
		rightPanel = new JPanel(new BorderLayout());
		JScrollPane leftScroll = new JScrollPane(left);
		JScrollPane rightScroll = new JScrollPane(right);
		leftPanel.add(leftScroll);
		rightPanel.add(rightScroll);
		setLeftComponent(leftPanel);
		setRightComponent(rightPanel);
		left.setDragEnabled(true);
		right.setDragEnabled(true);

		MyTransferHandler myTransferHandler = new MyTransferHandler();
		left.setTransferHandler(myTransferHandler);
		right.setTransferHandler(myTransferHandler);
		setDividerLocation(0.5);
	} //}}}

	//{{{ setLeftTooltip() method
	public void setLeftTooltip(String leftTooltip)
	{
		left.setToolTipText(leftTooltip);
	} //}}}

	//{{{ setRightTooltip() method
	public void setRightTooltip(String rightTooltip)
	{
		right.setToolTipText(rightTooltip);
	} //}}}

	//{{{ setLeftTitle() method
	public void setLeftTitle(String leftTitle)
	{
		if (leftTitle == null)
		{
			removeLeftTitle();
			return;
		}
		if (leftLabel == null)
		{
			leftLabel = new JLabel();
		}
		leftLabel.setText(leftTitle);
		leftPanel.add(leftLabel, BorderLayout.NORTH);
	} //}}}

	//{{{ setRightTitle() method
	public void setRightTitle(String rightTitle)
	{
		if (rightTitle == null)
		{
			removeRightTitle();
			return;
		}
		if (rightLabel == null)
		{
			rightLabel = new JLabel();
		}
		rightLabel.setText(rightTitle);
		rightPanel.add(rightLabel, BorderLayout.NORTH);
	} //}}}

	//{{{ removeLeftTitle() method
	public void removeLeftTitle()
	{
		if (leftLabel != null)
		{
			leftPanel.remove(leftLabel);
			leftLabel = null;
		}
	} //}}}

	//{{{ removeRightTitle() method
	public void removeRightTitle()
	{
		if (rightLabel != null)
		{
			rightPanel.remove(rightLabel);
			rightLabel = null;
		}
	} //}}}

	//{{{ getLeftSize() method
	public int getLeftSize()
	{
		return leftModel.getSize();
	} //}}}

	//{{{ getRightSize() method
	public int getRightSize()
	{
		return rightModel.getSize();
	} //}}}

	//{{{ getLeftDataIterator() method
	public Iterator<E> getLeftDataIterator()
	{
		return leftModel.iterator();
	} //}}}

	//{{{ getRightDataIterator() method
	public Iterator<E> getRightDataIterator()
	{
		return rightModel.iterator();
	} //}}}

	//{{{ moveAllToLeft() method
	public void moveAllToLeft()
	{
		leftModel.addAll(rightModel.data);
		rightModel.clear();
	} //}}}

	//{{{ moveAllToRight() method
	public void moveAllToRight()
	{
		rightModel.addAll(leftModel.data);
		leftModel.clear();
	} //}}}

	//{{{ MyListModel class
	private static class MyListModel<E> extends AbstractListModel implements Iterable<E>
	{
		private List<E> data;

		private MyListModel(List<E> data)
		{
			this.data = data;
		}

		@Override
		public int getSize()
		{
			return data.size();
		}

		@Override
		public Object getElementAt(int index)
		{
			return data.get(index);
		}

		@Override
		public Iterator<E> iterator()
		{
			return data.iterator();
		}

		public void clear()
		{
			if (data.isEmpty())
				return;
			int i = data.size();
			data.clear();
			fireIntervalRemoved(this, 0, i - 1);
		}

		public void addAll(Collection<E> newData)
		{
			int i = data.size();
			data.addAll(newData);
			fireIntervalAdded(this, i, i + newData.size() - 1);
		}

		public void remove(int index)
		{
			data.remove(index);

			fireContentsChanged(this, index, index);
		}

		public void add(int pos, E[] addedDatas)
		{
			for (int i = addedDatas.length - 1; i >= 0; i--)
				data.add(pos, addedDatas[i]);

			fireContentsChanged(this, pos, pos + addedDatas.length - 1);
		}
	} //}}}

	//{{{ MyTransferHandler class
	private class MyTransferHandler extends TransferHandler
	{
		private JList sourceList;
		private int[]indices;

		@Override
		public int getSourceActions(JComponent c)
		{
			return MOVE;
		}

		@Override
		public boolean importData(JComponent comp, Transferable t)
		{
			try
			{
				@SuppressWarnings({"unchecked"})
				E[] transferData = (E[]) t.getTransferData(MyTransferable.javaListFlavor);
				JList targetList = (JList) comp;
				@SuppressWarnings({"unchecked"})
				MyListModel<E> targetModel = (MyListModel<E>) targetList.getModel();
				@SuppressWarnings({"unchecked"})
				MyListModel<E> sourceModel = (MyListModel<E>) sourceList.getModel();
				int dropLocation = targetList.getSelectedIndex();
				if(dropLocation == -1)dropLocation=0;
				targetModel.add(dropLocation, transferData);
				int dropStart = dropLocation;
				if (targetList == sourceList)
				{
					// we are moving inside the same list
					for (int i = indices.length - 1; i >= 0; i--)
					{
						int index = indices[i];
						if (indices[i] >= dropLocation)
						{
							index += transferData.length;
						}
						else
						{
							dropStart--;
						}
						sourceModel.remove(index);
					}
					for (int i = indices.length - 1; i >= 0; i--)
					{
						indices[i] = dropStart + i;
					}
				}
				else
				{
					// we are moving to another list
					sourceList.clearSelection();
					for (int i = indices.length - 1; i >= 0; i--)
					{
						sourceModel.remove(indices[i]);
						indices[i] = dropLocation + i;
					}
				}
				targetList.setSelectedIndices(indices);
				return true;
			}
			catch (UnsupportedFlavorException e)
			{
				Log.log(Log.ERROR, this, e);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, e);
			}
			return false;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			sourceList = (JList) c;
			indices = sourceList.getSelectedIndices();

			@SuppressWarnings("unchecked")
			E[] objects = (E[]) sourceList.getSelectedValues();
			return new MyTransferable<E>(objects);
		}

		@Override
		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
		{
			return comp == left || comp == right;
		}
	} //}}}

	//{{{ MyTransferable class
	private static class MyTransferable<E> implements Transferable
	{
		public static final DataFlavor javaListFlavor = new DataFlavor(Collection.class, "java.util.Collection");

		private final E[] data;

		private MyTransferable(E[] data)
		{
			this.data = data;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors()
		{
			return new DataFlavor[]{javaListFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return flavor.equals(javaListFlavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
		{
			return data;
		}
	} //}}}
}
