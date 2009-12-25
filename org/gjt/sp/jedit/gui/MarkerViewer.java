/*
 * MarkerViewer.java - Dockable view of markers in the current buffer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Nicholas O'Leary
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
//}}}

public class MarkerViewer extends JPanel implements ActionListener
{
	//{{{ MarkerViewer constructor
	public MarkerViewer(View view)
	{
		super(new BorderLayout());
		this.view = view;
		Box toolBar = new Box(BoxLayout.X_AXIS);

		toolBar.add(new JLabel(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("markers.label"))));

		toolBar.add(Box.createGlue());

		RolloverButton addMarker = new RolloverButton(
			GUIUtilities.loadIcon("Plus.png"));
		addMarker.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("add-marker.label")));
		addMarker.addActionListener(this);
		addMarker.setActionCommand("add-marker");
		toolBar.add(addMarker);

		previous = new RolloverButton(GUIUtilities.loadIcon("ArrowL.png"));
		previous.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("prev-marker.label")));
		previous.addActionListener(this);
		previous.setActionCommand("prev-marker");
		toolBar.add(previous);

		next = new RolloverButton(GUIUtilities.loadIcon("ArrowR.png"));
		next.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("next-marker.label")));
		next.addActionListener(this);
		next.setActionCommand("next-marker");
		toolBar.add(next);

		clear = new RolloverButton(GUIUtilities.loadIcon("Clear.png"));
		clear.setToolTipText(GUIUtilities.prettifyMenuLabel(
			jEdit.getProperty("remove-all-markers.label")));
		clear.addActionListener(this);
		clear.setActionCommand("clear");
		toolBar.add(clear);


		add(BorderLayout.NORTH, toolBar);

		markerList = new JList();
		markerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		markerList.setCellRenderer(new Renderer());
		markerList.addMouseListener(new MouseHandler());

		add(BorderLayout.CENTER,new JScrollPane(markerList));

		refreshList();
	} //}}}

	//{{{ requestDefaultFocus() method
	public boolean requestDefaultFocus()
	{
		markerList.requestFocus();
		return true;
	} //}}}

	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent evt)
	{
		String cmd = evt.getActionCommand();
		if (cmd.equals("clear"))
			view.getBuffer().removeAllMarkers();
		else if (cmd.equals("add-marker"))
			view.getEditPane().addMarker();
		else if (cmd.equals("next-marker"))
		{
			view.getEditPane().goToNextMarker(false);
			updateSelection();
		}
		else if (cmd.equals("prev-marker"))
		{
			view.getEditPane().goToPrevMarker(false);
			updateSelection();
		}
	} //}}}

	//{{{ handleEditPaneUpdate() method
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate epu)
	{
		if (epu.getEditPane().getView().equals(view) &&
			epu.getWhat().equals(EditPaneUpdate.BUFFER_CHANGED))
		{
			refreshList();
		}
	} //}}}

	//{{{ handleViewUpdate() method
	@EBHandler
	public void handleViewUpdate(ViewUpdate vu)
	{
		if (vu.getView().equals(view) &&
			vu.getWhat().equals(ViewUpdate.EDIT_PANE_CHANGED))
		{
			refreshList();
		}
	} //}}}

	//{{{ handleBufferUpdate() method
	@EBHandler
	public void handleBufferUpdate(BufferUpdate bu)
	{
		if (view.getBuffer().equals(bu.getBuffer()) &&
			(bu.getWhat().equals(BufferUpdate.MARKERS_CHANGED) || bu.getWhat().equals(BufferUpdate.LOADED)))
		{
			refreshList();
		}
	}//}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JList markerList;
	private View view;
	private RolloverButton previous;
	private RolloverButton next;
	private RolloverButton clear;
	//}}}

	//{{{ refreshList() method
	private void refreshList()
	{
		java.util.Vector markers = view.getBuffer().getMarkers();
		if (markers.size() > 0)
		{
			markerList.setListData(markers);
			markerList.setEnabled(true);
			next.setEnabled(true);
			previous.setEnabled(true);
			clear.setEnabled(true);
		}
		else
		{
			markerList.setListData(new Object[] {
				jEdit.getProperty("no-markers.label") });
			markerList.setEnabled(false);
			next.setEnabled(false);
			previous.setEnabled(false);
			clear.setEnabled(false);
		}

	} //}}}

	//{{{ goToSelectedMarker() method
	private void goToSelectedMarker()
	{
		Object value = markerList.getSelectedValue();
		if(!(value instanceof Marker))
			return;

		Marker mark = (Marker)value;
		view.getTextArea().setCaretPosition(mark.getPosition());
		view.toFront();
		view.requestFocus();
		view.getTextArea().requestFocus();
	} //}}}

	//{{{ updateSelection() method
	private void updateSelection()
	{
		ListModel model = markerList.getModel();
		int currentLine = view.getTextArea().getCaretLine();
		Buffer buffer = view.getBuffer();
		for (int i = 0; i < model.getSize(); i++)
		{
			Object o = model.getElementAt(i);
			if (o instanceof Marker)
			{
				Marker mark = (Marker)model.getElementAt(i);
				if (buffer.getLineOfOffset(mark.getPosition()) == currentLine)
				{
					markerList.setSelectedIndex(i);
					break;
				}
			}
		}

	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Renderer Class
	class Renderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(
			JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,
			index,isSelected,cellHasFocus);

			if(value instanceof Marker)
			{
				Marker mark = (Marker)value;
				JEditTextArea textArea = view.getTextArea();
				int pos = textArea.getLineOfOffset(mark.getPosition());
				String txt = view.getTextArea().getLineText(pos);
				if (txt.equals(""))
					txt = jEdit.getProperty("markers.blank-line");
				char shortcut_char = mark.getShortcut();
				String shortcut = "";
				if (shortcut_char > 0)
					shortcut = "["+shortcut_char+"]";
				setText((pos+1)+" "+shortcut+": "+txt);
			}
			return this;
		}
	} //}}}

	//{{{ MouseHandler Class
	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if(evt.isConsumed())
				return;

			int index = markerList.locationToIndex(evt.getPoint());
			markerList.setSelectedIndex(index);

			goToSelectedMarker();
		}
	} //}}}

	//}}}
}
