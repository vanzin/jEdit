/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
 * Portions copyright (C) 2001 Mike Dillon
 * Portions copyright (C) 2008 Matthieu Casanova
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
import javax.swing.border.*;
import javax.swing.text.Segment;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.gui.statusbar.ToolTipLabel;
import org.gjt.sp.util.*;
//}}}

/**
 * The status bar used to display various information to the user.<p>
 *
 * Currently, it is used for the following:
 * <ul>
 * <li>Displaying caret position information
 * <li>Displaying {@link InputHandler#readNextChar(String,String)} prompts
 * <li>Displaying {@link #setMessage(String)} messages
 * <li>Displaying I/O progress
 * <li>Displaying various editor settings
 * <li>Displaying memory status
 * </ul>
 *
 * @version $Id$
 * @author Slava Pestov
 * @since jEdit 3.2pre2
 */
public class StatusBar extends JPanel implements WorkThreadProgressListener
{
	//{{{ StatusBar constructor
	public StatusBar(View view)
	{
		super(new BorderLayout());
		setName("StatusBar");
		setBorder(new CompoundBorder(new EmptyBorder(4,0,0,
			(OperatingSystem.isMacOS() ? 18 : 0)),
			UIManager.getBorder("TextField.border")));

		this.view = view;

		panel = new JPanel(new BorderLayout());
		box = new Box(BoxLayout.X_AXIS);
		panel.add(BorderLayout.EAST,box);
		add(BorderLayout.CENTER,panel);

		MouseHandler mouseHandler = new MouseHandler();

		caretStatus = new ToolTipLabel();
		caretStatus.setName("caretStatus");
		caretStatus.setToolTipText(jEdit.getProperty("view.status.caret-tooltip"));
		caretStatus.addMouseListener(mouseHandler);

		message = new JLabel(" ");
		setMessageComponent(message);

		modeWidget = _getWidget("mode");
		foldWidget = _getWidget("fold");
		encodingWidget = _getWidget("encoding");
		wrapWidget = _getWidget("wrap");
		multiSelectWidget = _getWidget("multiSelect");
		rectSelectWidget = _getWidget("rectSelect");
		overwriteWidget = _getWidget("overwrite");
		lineSepWidget = _getWidget("lineSep");
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		Color fg = jEdit.getColorProperty("view.status.foreground");
		Color bg = jEdit.getColorProperty("view.status.background");

		showCaretStatus = jEdit.getBooleanProperty("view.status.show-caret-status");

		panel.setBackground(bg);
		panel.setForeground(fg);
		caretStatus.setBackground(bg);
		caretStatus.setForeground(fg);
		message.setBackground(bg);
		message.setForeground(fg);

		// retarded GTK look and feel!
		Font font = new JLabel().getFont();
		//UIManager.getFont("Label.font");
		FontMetrics fm = getFontMetrics(font);

		if (showCaretStatus)
		{
			panel.add(BorderLayout.WEST,caretStatus);

			caretStatus.setFont(font);

			Dimension dim = new Dimension(fm.stringWidth(caretTestStr),
					fm.getHeight());
			caretStatus.setPreferredSize(dim);
			updateCaretStatus();
		}
		else
			panel.remove(caretStatus);

		String statusBar = jEdit.getProperty("view.status");
		if (!StandardUtilities.objectsEqual(currentBar, statusBar))
		{
			box.removeAll();
			StringTokenizer tokenizer = new StringTokenizer(statusBar);
			while (tokenizer.hasMoreTokens())
			{
				String token = tokenizer.nextToken();
				if (Character.isLetter(token.charAt(0)))
				{
					Widget widget = getWidget(token);
					if (widget == null)
					{
						JLabel label = new JLabel(token);
						label.setBackground(bg);
						label.setForeground(fg);
						box.add(label);
						continue;
					}
					Component c = widget.getComponent();
					c.setBackground(bg);
					c.setForeground(fg);
					box.add(c);
					widget.update();
					widget.propertiesChanged();
				}
				else
				{
					JLabel label = new JLabel(token);
					label.setBackground(bg);
					label.setForeground(fg);
					box.add(label);
				}
			}
			currentBar = statusBar;
		}
		updateBufferStatus();
		updateMiscStatus();
	} //}}}

	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		super.addNotify();
		VFSManager.getIOThreadPool().addProgressListener(this);
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		VFSManager.getIOThreadPool().removeProgressListener(this);
	} //}}}

	//{{{ WorkThreadListener implementation

	//{{{ statusUpdate() method
	public void statusUpdate(final WorkThreadPool threadPool, int threadIndex)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// don't obscure existing message
				if(message != null && !"".equals(message.getText().trim())
					&& !currentMessageIsIO)
					return;

				int requestCount = threadPool.getRequestCount();
				if(requestCount == 0)
				{
					setMessageAndClear(jEdit.getProperty(
						"view.status.io.done"));
					currentMessageIsIO = true;
				}
				else if(requestCount == 1)
				{
					setMessage(jEdit.getProperty(
						"view.status.io-1"));
					currentMessageIsIO = true;
				}
				else
				{
					Object[] args = {requestCount};
					setMessage(jEdit.getProperty(
						"view.status.io",args));
					currentMessageIsIO = true;
				}
			}
		});
	} //}}}

	//{{{ progressUpdate() method
	public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
	{
	} //}}}

	//}}}

	//{{{ getMessage() method
	/**
	 * Returns the current message.
	 *
	 * @return the current message
	 * @since jEdit 4.4pre1
	 */
	public String getMessage()
	{
		return message.getText();
	} //}}}

	//{{{ setMessageAndClear() method
	/**
	 * Show a message for a short period of time.
	 * @param message The message
	 * @since jEdit 3.2pre5
	 */
	public void setMessageAndClear(String message)
	{
		setMessage(message);

		tempTimer = new Timer(0,new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				// so if view is closed in the meantime...
				if(isShowing())
					setMessage(null);
			}
		});

		tempTimer.setInitialDelay(10000);
		tempTimer.setRepeats(false);
		tempTimer.start();
	} //}}}

	//{{{ setMessage() method
	/**
	 * Displays a status message.
	 * @param message the message to display, it can be null
	 */
	public void setMessage(String message)
	{
		if(tempTimer != null)
		{
			tempTimer.stop();
			tempTimer = null;
		}

		setMessageComponent(this.message);

		if(message == null)
		{
			if(view.getMacroRecorder() != null)
				this.message.setText(jEdit.getProperty("view.status.recording"));
			else
				this.message.setText(" ");
		}
		else
			this.message.setText(message);
	} //}}}

	//{{{ setMessageComponent() method
	public void setMessageComponent(Component comp)
	{
		currentMessageIsIO = false;

		if (comp == null || messageComp == comp)
		{
			return;
		}

		messageComp = comp;
		panel.add(BorderLayout.CENTER, messageComp);
	} //}}}

	//{{{ updateCaretStatus() method
	public void updateCaretStatus()
	{
		if (showCaretStatus)
		{
			Buffer buffer = view.getBuffer();

			if(!buffer.isLoaded() ||
				/* can happen when switching buffers sometimes */
				buffer != view.getTextArea().getBuffer())
			{
				caretStatus.setText(" ");
				return;
			}

			JEditTextArea textArea = view.getTextArea();

			int caretPosition = textArea.getCaretPosition();
			int currLine = textArea.getCaretLine();

			// there must be a better way of fixing this...
			// the problem is that this method can sometimes
			// be called as a result of a text area scroll
			// event, in which case the caret position has
			// not been updated yet.
			if(currLine >= buffer.getLineCount())
				return; // hopefully another caret update will come?

			int start = textArea.getLineStartOffset(currLine);
			int dot = caretPosition - start;

			if(dot < 0)
				return;

			int bufferLength = buffer.getLength();

			buffer.getText(start,dot,seg);
			int virtualPosition = StandardUtilities.getVirtualWidth(seg,
				buffer.getTabSize());
			// for GC
			seg.array = null;
			seg.count = 0;

			if (jEdit.getBooleanProperty("view.status.show-caret-linenumber", true))
			{
				buf.append(currLine + 1);
				buf.append(',');
			}
			if (jEdit.getBooleanProperty("view.status.show-caret-dot", true))
			{
				buf.append(dot + 1);
			}
			if (jEdit.getBooleanProperty("view.status.show-caret-virtual", true) &&
				virtualPosition != dot)
			{
				buf.append('-');
				buf.append(virtualPosition + 1);
			}
			if (buf.length() > 0)
			{
				buf.append(' ');
			}
			if (jEdit.getBooleanProperty("view.status.show-caret-offset", true) &&
				jEdit.getBooleanProperty("view.status.show-caret-bufferlength", true))
			{
				buf.append('(');
				buf.append(caretPosition);
				buf.append('/');
				buf.append(bufferLength);
				buf.append(')');
			}
			else if (jEdit.getBooleanProperty("view.status.show-caret-offset", true))
			{
				buf.append('(');
				buf.append(caretPosition);
				buf.append(')');
			}
			else if (jEdit.getBooleanProperty("view.status.show-caret-bufferlength", true))
			{
				buf.append('(');
				buf.append(bufferLength);
				buf.append(')');
			}

			caretStatus.setText(buf.toString());
			buf.setLength(0);
		}
	} //}}}

	//{{{ updateBufferStatus() method
	public void updateBufferStatus()
	{
		wrapWidget.update();
		lineSepWidget.update();
		modeWidget.update();
		foldWidget.update();
		encodingWidget.update();
	} //}}}

	//{{{ updateMiscStatus() method
	public void updateMiscStatus()
	{
		multiSelectWidget.update();
		rectSelectWidget.update();
		overwriteWidget.update();
	} //}}}

	//{{{ Private members
	private String currentBar;
	private final View view;
	private final JPanel panel;
	private final Box box;
	private final ToolTipLabel caretStatus;
	private Component messageComp;
	private final JLabel message;
	private final Widget modeWidget;
	private final Widget foldWidget;
	private final Widget encodingWidget;
	private final Widget wrapWidget;
	private final Widget multiSelectWidget;
	private final Widget rectSelectWidget;
	private final Widget overwriteWidget;
	private final Widget lineSepWidget;
	/* package-private for speed */ StringBuilder buf = new StringBuilder();
	private Timer tempTimer;
	private boolean currentMessageIsIO;

	private final Segment seg = new Segment();

	private boolean showCaretStatus;
	//}}}

	static final String caretTestStr = "9999,999-999 (99999999/99999999)";

	//{{{ getWidget() method
	private Widget getWidget(String name)
	{
		if ("mode".equals(name))
			return modeWidget;
		if ("fold".equals(name))
			return foldWidget;
		if ("encoding".equals(name))
			return encodingWidget;
		if ("wrap".equals(name))
			return wrapWidget;
		if ("multiSelect".equals(name))
			return multiSelectWidget;
		if ("rectSelect".equals(name))
			return rectSelectWidget;
		if ("overwrite".equals(name))
			return overwriteWidget;
		if ("lineSep".equals(name))
			return lineSepWidget;

		return _getWidget(name);
	} //}}}

	//{{{ _getWidget() method
	private Widget _getWidget(String name)
	{
		StatusWidgetFactory widgetFactory =
		(StatusWidgetFactory) ServiceManager.getService("org.gjt.sp.jedit.gui.statusbar.StatusWidget", name);
		if (widgetFactory == null)
		{
			return null;
		}
		return widgetFactory.getWidget(view);
	} //}}}

	//{{{ MouseHandler class
	private class MouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			Object source = evt.getSource();
			if(source == caretStatus && evt.getClickCount() == 2)
			{
				view.getTextArea().showGoToLineDialog();
			}
		}
	} //}}}
}
