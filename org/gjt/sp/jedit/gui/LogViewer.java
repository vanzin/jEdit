/*
 * LogViewer.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ThreadUtilities;
import org.gjt.sp.util.swing.event.UniqueActionDocumentListener;
//}}}

/** Activity Log Viewer
 * @version $Id$
 */
public class LogViewer extends JPanel implements DefaultFocusComponent
{
	private final ColorizerCellRenderer cellRenderer;

	//{{{ LogViewer constructor
	@SuppressWarnings({"unchecked"})	// The FilteredListModel needs work
	public LogViewer()
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
		JPanel caption = new JPanel();
		caption.setLayout(new BoxLayout(caption,BoxLayout.X_AXIS));
		caption.setBorder(new EmptyBorder(6, 0, 6, 0));

		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			String[] args = { MiscUtilities.constructPath(settingsDirectory, "activity.log") };
			JLabel label = new JLabel(jEdit.getProperty("log-viewer.caption",args));
			caption.add(label);
		}

		caption.add(Box.createHorizontalGlue());

		tailIsOn = jEdit.getBooleanProperty("log-viewer.tail", false);
		tail = new JCheckBox(jEdit.getProperty("log-viewer.tail.label"),tailIsOn);
		tail.addActionListener(new ActionHandler());


		filter = new JTextField();
		filter.getDocument().addDocumentListener(new UniqueActionDocumentListener(e -> setFilter()));
		caption.add(filter);
		caption.add(tail);

		caption.add(Box.createHorizontalStrut(12));

		copy = new JButton(jEdit.getProperty("log-viewer.copy"));
		copy.addActionListener(new ActionHandler());
		caption.add(copy);

		caption.add(Box.createHorizontalStrut(6));

		JButton settings = new JButton(jEdit.getProperty("log-viewer.settings.label"));
		settings.addActionListener(e -> new LogSettings());
		caption.add(settings);

		Log.setMaxLines(jEdit.getIntegerProperty("log-viewer.maxlines", 500));
		ListModel<String> model = Log.getLogListModel();
		listModel = new MyFilteredListModel(model);
		// without this, listModel is held permanently in model.
		// See addNotify() and removeNotify(), and constructor of
		// FilteredListModel.
		model.removeListDataListener(listModel);

		list = new LogList(listModel);
		listModel.setList(list);
		cellRenderer = new ColorizerCellRenderer(list);
		list.setCellRenderer(cellRenderer);
		setFilter();
                                                                    
		add(BorderLayout.NORTH,caption);
		JScrollPane scroller = new JScrollPane(list);
		Dimension dim = scroller.getPreferredSize();
		dim.width = Math.min(600,dim.width);
		scroller.setPreferredSize(dim);
		add(BorderLayout.CENTER,scroller);

		propertiesChanged();
	} //}}}

	//{{{ setBounds() method
	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		scrollLaterIfRequired();
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		propertiesChanged();
	} //}}}

	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		super.addNotify();
		cellRenderer.updateColors(list);
		ListModel<String> model = Log.getLogListModel();
		model.addListDataListener(listModel);
		model.addListDataListener(listHandler = new ListHandler());
		if(tailIsOn)
			scrollToTail();

		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		ListModel<String> model = Log.getLogListModel();
		model.removeListDataListener(listModel);
		model.removeListDataListener(listHandler);
		listHandler = null;
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ focusOnDefaultComponent() method
	@Override
	public void focusOnDefaultComponent()
	{
		list.requestFocus();
	} //}}}

	//{{{ Private members
	private ListHandler listHandler;
	private FilteredListModel<ListModel<String>> listModel;
	private final JList<String> list;
	private final JButton copy;
	private final JCheckBox tail;
	private final JTextField filter;
	private boolean tailIsOn;
	private static boolean showDebug = jEdit.getBooleanProperty("log-viewer.message.debug", true);
	private static boolean showMessage = jEdit.getBooleanProperty("log-viewer.message.message", true);
	private static boolean showNotice = jEdit.getBooleanProperty("log-viewer.message.notice", true);
	private static boolean showWarning = jEdit.getBooleanProperty("log-viewer.message.warning", true);
	private static boolean showError = jEdit.getBooleanProperty("log-viewer.message.error", true);

	//{{{ setFilter() method
	private void setFilter()
	{
		String toFilter = filter.getText();
		listModel.setFilter(toFilter.isEmpty() ? " " : toFilter);
		scrollLaterIfRequired();
	} //}}}

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		cellRenderer.updateColors(list);
		list.setFont(jEdit.getFontProperty("view.font"));
		list.setFixedCellHeight(list.getFontMetrics(list.getFont())
					.getHeight());
	} //}}}

	//{{{ scrollToTail() method
	/** Scroll to the tail of the logs. */
	private void scrollToTail()
	{
		int index = list.getModel().getSize();
		if(index != 0)
			list.ensureIndexIsVisible(index - 1);
	} //}}}

	//{{{ scrollLaterIfRequired() method
	private void scrollLaterIfRequired()
	{
		if (tailIsOn)
			ThreadUtilities.runInDispatchThread(this::scrollToTail);
	} //}}}

	//}}}

	//{{{ ActionHandler class
	@SuppressWarnings({"deprecation"})	// see note below
	private class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			if(src == tail)
			{
				tailIsOn = !tailIsOn;
				jEdit.setBooleanProperty("log-viewer.tail",tailIsOn);
				if(tailIsOn)
				{
					scrollToTail();
				}
			}
			else if(src == copy)
			{
				StringBuilder buf = new StringBuilder();
				// TODO: list.getSelectedValues is deprecated. Need to finish the
				// conversion to generics for this class at some point.
				
				Object[] selected = list.getSelectedValues();
				if(selected != null && selected.length != 0)
				{
					for (Object sel : selected)
					{
						buf.append(sel);
						buf.append('\n');
					}
				}
				else
				{
					ListModel<String> model = list.getModel();
					for(int i = 0; i < model.getSize(); i++)
					{
						buf.append(model.getElementAt(i));
						buf.append('\n');
					}
				}
				Registers.setRegister('$',buf.toString());
			}
		}
	} //}}}

	//{{{ ListHandler class
	private class ListHandler implements ListDataListener
	{
		@Override
		public void intervalAdded(ListDataEvent e)
		{
			contentsChanged(e);
		}

		@Override
		public void intervalRemoved(ListDataEvent e)
		{
			contentsChanged(e);
		}

		@Override
		public void contentsChanged(ListDataEvent e)
		{
			scrollLaterIfRequired();
		}
	} //}}}

	//{{{ LogList class
	private class LogList extends JList<String>
	{
		LogList(ListModel<String> model)
		{
			super(model);
			setVisibleRowCount(24);
			getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			setAutoscrolls(true);
		}

		@Override
		public void processMouseEvent(MouseEvent evt)
		{
			if(evt.getID() == MouseEvent.MOUSE_PRESSED)
			{
				startIndex = list.locationToIndex(evt.getPoint());
			}
			super.processMouseEvent(evt);
		}

		@Override
		public void processMouseMotionEvent(MouseEvent evt)
		{
			if(evt.getID() == MouseEvent.MOUSE_DRAGGED)
			{
				int row = list.locationToIndex(evt.getPoint());
				if(row != -1)
				{
					if(startIndex == -1)
					{
						list.setSelectionInterval(row,row);
						startIndex = row;
					}
					else
						list.setSelectionInterval(startIndex,row);
					list.ensureIndexIsVisible(row);
					evt.consume();
				}
			}
			else
				super.processMouseMotionEvent(evt);
		}

		private int startIndex;
	} //}}}

	//{{{ ColorizerCellRenderer class
	private static class ColorizerCellRenderer extends JPanel implements ListCellRenderer<String>
	{
		private static Color debugColor;
		private static Color messageColor;
		private static Color noticeColor;
		private static Color warningColor;
		private static Color errorColor;
		
		private String text;
		private final int borderWidth = 1;
		private int baseline;
		private int width;
		private int height;
		
		private JList<String> list;

		private ColorizerCellRenderer(JList<String> list)
		{
			this.list = list;
			updateColors(list);
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(width, height);	
		}
		
		@Override
		public void paint(Graphics g)
		{
			int currentWidth = (int)list.getFontMetrics(list.getFont()).getStringBounds(text, g).getWidth();
			width = Math.max(width, currentWidth);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(getForeground());
			g.drawString(text, borderWidth, baseline);
		}

		// This is the only method defined by ListCellRenderer.
		@Override
		public Component getListCellRendererComponent(
							      JList<? extends String> list,
							      String value,              // value to display
							      int index,                 // cell index
							      boolean isSelected,        // is the cell selected
							      boolean cellHasFocus )     // the list and the cell have the focus
		{
			if (value == null)
				value = "";
			text = value;
			if (isSelected)
			{
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else
			{
				setBackground(list.getBackground());
				Color color = list.getForeground();
				if (text.contains("[debug]"))
				{
					color = debugColor;
				}
				else if (text.contains("[message]"))
				{
					color = messageColor;
				}
				else if (text.contains("[notice]"))
				{
					color = noticeColor;
				}
				else if (text.contains("[warning]"))
				{
					color = warningColor;
				}
				else if (text.contains("[error]"))
				{
					color = errorColor;
				}
				setForeground(color);
			}
			return this;
		}

		public void updateColors(JList<String> list)
		{
			this.list = list;
			debugColor = jEdit.getColorProperty("log-viewer.message.debug.color", Color.BLUE);
			messageColor = jEdit.getColorProperty("log-viewer.message.message.color", Color.BLACK);
			noticeColor = jEdit.getColorProperty("log-viewer.message.notice.color", Color.GREEN);
			warningColor = jEdit.getColorProperty("log-viewer.message.warning.color", Color.ORANGE);
			errorColor = jEdit.getColorProperty("log-viewer.message.error.color", Color.RED);
			setFont(list.getFont());
			FontMetrics fm = list.getFontMetrics(list.getFont());
			baseline = fm.getAscent() + borderWidth;
			width = list.getWidth();
			height = fm.getHeight() + (2 * borderWidth);
		}
	} //}}}

	//{{{ MyFilteredListModel
	private static class MyFilteredListModel extends FilteredListModel<ListModel<String>>
	{
		MyFilteredListModel(ListModel<String> model)
		{
			super(model);
		}

		@Override
		public String prepareFilter(String filter)
		{
			return filter.toLowerCase();
		}

		@Override
		public boolean passFilter(int row, @Nullable String filter)
		{
			if (filter == null || filter.isEmpty())
				return true;
			Object item = delegated.getElementAt(row);
			if (item == null)
				return true;
			String text = item.toString().toLowerCase();
			if (!showDebug && text.contains("[debug]"))
				return false;
			if (!showMessage && text.contains("[message]"))
				return false;
			if (!showNotice && text.contains("[notice]"))
				return false;
			if (!showWarning && text.contains("[warning]"))
				return false;
			if (!showError && text.contains("[error]"))
				return false;
			return text.contains(filter);
		}
	} //}}}

	//{{{ LogSettings dialog
	@SuppressWarnings({"unchecked"})	// The FilteredListModel needs work
	private class LogSettings extends JDialog
	{
		LogSettings()
		{
			super(jEdit.getActiveView(), jEdit.getProperty("log-viewer.dialog.title"));
			AbstractOptionPane pane = new AbstractOptionPane(jEdit.getProperty("log-viewer.settings.label"))
			{
				@Override
				protected void _init()
				{
					setBorder(BorderFactory.createEmptyBorder(11, 11, 12, 12));
					maxLines = new JSpinner(new SpinnerNumberModel(jEdit.getIntegerProperty("log-viewer.maxlines", 500), 500, Integer.MAX_VALUE, 1));
					addComponent(jEdit.getProperty("log-viewer.maxlines.label", "Max lines"),
						maxLines,
						GridBagConstraints.REMAINDER);
					addComponent(Box.createVerticalStrut(11));
					debug = new JCheckBox(jEdit.getProperty("log-viewer.message.debug.label", "Debug"),
						jEdit.getBooleanProperty("log-viewer.message.debug", true));
					message = new JCheckBox(jEdit.getProperty("log-viewer.message.message.label", "Message"),
						jEdit.getBooleanProperty("log-viewer.message.message", true));
					notice = new JCheckBox(jEdit.getProperty("log-viewer.message.notice.label", "Notice"),
						jEdit.getBooleanProperty("log-viewer.message.notice", true));
					warning = new JCheckBox(jEdit.getProperty("log-viewer.message.warning.label", "Warning"),
						jEdit.getBooleanProperty("log-viewer.message.warning", true));
					error = new JCheckBox(jEdit.getProperty("log-viewer.message.error.label", "Error"),
						jEdit.getBooleanProperty("log-viewer.message.error", true));

					addComponent(new JLabel(jEdit.getProperty("log-viewer.message.label", "Message Display:")));
					addComponent(debug,
						debugColor = new ColorWellButton(
						jEdit.getColorProperty("log-viewer.message.debug.color", Color.BLUE)),
						GridBagConstraints.REMAINDER);
					addComponent(message,
						messageColor = new ColorWellButton(
						jEdit.getColorProperty("log-viewer.message.message.color", Color.GREEN)),
						GridBagConstraints.REMAINDER);
					addComponent(notice,
						noticeColor = new ColorWellButton(
						jEdit.getColorProperty("log-viewer.message.notice.color", Color.GREEN)),
						GridBagConstraints.REMAINDER);
					addComponent(warning,
						warningColor = new ColorWellButton(
						jEdit.getColorProperty("log-viewer.message.warning.color", Color.ORANGE)),
						GridBagConstraints.REMAINDER);
					addComponent(error,
						errorColor = new ColorWellButton(
						jEdit.getColorProperty("log-viewer.message.error.color", Color.RED)),
						GridBagConstraints.REMAINDER);

					addComponent(Box.createVerticalStrut(11));
					beep = new JCheckBox(jEdit.getProperty("debug.beepOnOutput.label"),
						jEdit.getBooleanProperty("debug.beepOnOutput", false));
					addComponent(beep);

					addComponent(Box.createVerticalStrut(11));

					JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					JButton okButton = new JButton(jEdit.getProperty("common.ok"));
					okButton.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent ae)
						{
							save();
							LogSettings.this.setVisible(false);
							LogSettings.this.dispose();
						}
					});
					JButton cancelButton = new JButton(jEdit.getProperty("common.cancel"));
					cancelButton.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent ae)
						{
							LogSettings.this.setVisible(false);
							LogSettings.this.dispose();
						}
					});
					buttonPanel.add(okButton);
					buttonPanel.add(cancelButton);
					addComponent(buttonPanel, GridBagConstraints.HORIZONTAL);
				}

				@Override
				protected void _save()
				{
					jEdit.setIntegerProperty("log-viewer.maxlines", ((SpinnerNumberModel)maxLines.getModel()).getNumber().intValue());
					Log.setMaxLines(jEdit.getIntegerProperty("log-viewer.maxlines", 500));
					ListModel<String> model = Log.getLogListModel();
					listModel = new MyFilteredListModel(model);
					list.setModel(listModel);
					listModel.setList(list);

					showDebug = debug.isSelected();
					jEdit.setBooleanProperty("log-viewer.message.debug", showDebug);
					showMessage = message.isSelected();
					jEdit.setBooleanProperty("log-viewer.message.message", showMessage);
					showNotice = notice.isSelected();
					jEdit.setBooleanProperty("log-viewer.message.notice", showNotice);
					showWarning = warning.isSelected();
					jEdit.setBooleanProperty("log-viewer.message.warning", showWarning);
					showError = error.isSelected();
					jEdit.setBooleanProperty("log-viewer.message.error", showError);

					jEdit.setColorProperty("log-viewer.message.debug.color", debugColor.getSelectedColor());
					jEdit.setColorProperty("log-viewer.message.message.color", messageColor.getSelectedColor());
					jEdit.setColorProperty("log-viewer.message.notice.color", noticeColor.getSelectedColor());
					jEdit.setColorProperty("log-viewer.message.warning.color", warningColor.getSelectedColor());
					jEdit.setColorProperty("log-viewer.message.error.color", errorColor.getSelectedColor());

					jEdit.setBooleanProperty("debug.beepOnOutput", beep.isSelected());

					// it would be most clean to call jEdit.propertiesChanged() now
					// which is needed since global debug.beepOnOutput flag is attached to this pane;
					// but to avoid extra log entries, we workaround it by direct Log access
					Log.setBeepOnOutput(beep.isSelected());
					// jEdit.propertiesChanged();
				}
			};
			setContentPane(pane);
			pane.init();
			pack();
			setLocationRelativeTo(LogViewer.this);
			setVisible(true);
		}

		private JSpinner maxLines;
		private JCheckBox debug;
		private JCheckBox message;
		private JCheckBox notice;
		private JCheckBox warning;
		private JCheckBox error;
		private ColorWellButton debugColor;
		private ColorWellButton messageColor;
		private ColorWellButton noticeColor;
		private ColorWellButton warningColor;
		private ColorWellButton errorColor;
		private JCheckBox beep;

	} //}}}
}

