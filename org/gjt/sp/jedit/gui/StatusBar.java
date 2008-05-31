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
		Dimension dim;

		if (showCaretStatus)
		{
			panel.add(BorderLayout.WEST,caretStatus);

			caretStatus.setFont(font);

			dim = new Dimension(fm.stringWidth(caretTestStr),
				fm.getHeight());
			caretStatus.setPreferredSize(dim);
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
						Log.log(Log.WARNING, this, "Widget " + token + " doesn't exists");
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
					box.add(new JLabel(token));
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
					Object[] args = {Integer.valueOf(requestCount)};
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
		//if(!isShowing())
		//	return;

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

			int currLine = textArea.getCaretLine();

			// there must be a better way of fixing this...
			// the problem is that this method can sometimes
			// be called as a result of a text area scroll
			// event, in which case the caret position has
			// not been updated yet.
			if(currLine >= buffer.getLineCount())
				return; // hopefully another caret update will come?

			int start = textArea.getLineStartOffset(currLine);
			int dot = textArea.getCaretPosition() - start;

			// see above
			if(dot < 0)
				return;

			buffer.getText(start,dot,seg);
			int virtualPosition = StandardUtilities.getVirtualWidth(seg,
				buffer.getTabSize());

			buf.setLength(0);
			buf.append(Integer.toString(currLine + 1));
			buf.append(',');
			buf.append(Integer.toString(dot + 1));

			if (virtualPosition != dot)
			{
				buf.append('-');
				buf.append(Integer.toString(virtualPosition + 1));
			}

			buf.append(' ');

			int firstLine = textArea.getFirstLine();
			int visible = textArea.getVisibleLines();
			int lineCount = textArea.getDisplayManager().getScrollLineCount();

			if (visible >= lineCount)
			{
				buf.append("All");
			}
			else if (firstLine == 0)
			{
				buf.append("Top");
			}
			else if (firstLine + visible >= lineCount)
			{
				buf.append("Bot");
			}
			else
			{
				float percent = (float)firstLine / (float)lineCount
					* 100.0f;
				buf.append(Integer.toString((int)percent));
				buf.append('%');
			}

			caretStatus.setText(buf.toString());
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
	private View view;
	private JPanel panel;
	private Box box;
	private ToolTipLabel caretStatus;
	private Component messageComp;
	private JLabel message;
	private Widget modeWidget;
	private Widget foldWidget;
	private Widget encodingWidget;
	private Widget wrapWidget;
	private Widget multiSelectWidget;
	private Widget rectSelectWidget;
	private Widget overwriteWidget;
	private Widget lineSepWidget;
	/* package-private for speed */ StringBuilder buf = new StringBuilder();
	private Timer tempTimer;
	private boolean currentMessageIsIO;

	private Segment seg = new Segment();

	private boolean showCaretStatus;
	//}}}

	static final String caretTestStr = "9999,999-999 99%";

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
			Log.log(Log.ERROR, this, "Widget " + name + " doesn't exists");
			return null;
		}
		return widgetFactory.getWidget(view);
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
	@Override
		public void mouseClicked(MouseEvent evt)
		{
			Object source = evt.getSource();
			if(source == caretStatus)
			{
				if(evt.getClickCount() == 2)
					view.getTextArea().showGoToLineDialog();
			}
		}
	} //}}}

	//{{{ ToolTipLabel class
	static class ToolTipLabel extends JLabel
	{
		//{{{ getToolTipLocation() method
	@Override
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}
	} //}}}
}
