/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * Copyright (C) 2001 Slava Pestov
 * Portions copyright (C) 2001 mike dillon
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

import javax.swing.border.*;
import javax.swing.text.Segment;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

/**
 * The status bar, used for the following:
 * <ul>
 * <li>Displaying caret position information
 * <li>Displaying readNextChar() prompts
 * <li>Displaying the 'macro recording' message
 * <li>Displaying the status of the overwrite, multi select flags
 * <li>I/O progress
 * <li>And so on
 * </ul>
 *
 * @version $Id$
 * @author Slava Pestov
 * @since jEdit 3.2pre2
 */
public class StatusBar extends JPanel
{
	public StatusBar(View view)
	{
		super(new BorderLayout(3,3));
		setBorder(BorderFactory.createEmptyBorder(3,0,0,0));

		this.view = view;

		Border border = BorderFactory.createLoweredBevelBorder();

		caretStatus = new VICaretStatus();
		caretStatus.setBorder(border);
		add(BorderLayout.WEST,caretStatus);

		messagePanel = new JPanel();
		messagePanel.setLayout(new BorderLayout(0,0));
		messagePanel.setBorder(border);
		messagePanel.setPreferredSize(caretStatus.getPreferredSize());
		add(BorderLayout.CENTER,messagePanel);

		message = new JLabel();
		message.setForeground(Color.black);
		message.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		setMessageComponent(message);

		MouseHandler mouseHandler = new MouseHandler();

		Box box = new Box(BoxLayout.X_AXIS);
		mode = new JLabel();
		mode.setForeground(Color.black);
		mode.setBorder(border);
		mode.setToolTipText(jEdit.getProperty("view.status.mode-tooltip"));
		mode.addMouseListener(mouseHandler);
		box.add(mode);
		box.add(Box.createHorizontalStrut(3));

		encoding = new JLabel();
		encoding.setForeground(Color.black);
		encoding.setBorder(border);
		encoding.setToolTipText(jEdit.getProperty("view.status.encoding-tooltip"));
		encoding.addMouseListener(mouseHandler);
		box.add(encoding);
		box.add(Box.createHorizontalStrut(3));

		multiSelect = new JLabel("multi");
		multiSelect.setBorder(border);
		multiSelect.addMouseListener(mouseHandler);
		box.add(multiSelect);
		box.add(Box.createHorizontalStrut(3));

		overwrite = new JLabel("over");
		overwrite.setBorder(border);
		overwrite.addMouseListener(mouseHandler);
		box.add(overwrite);
		box.add(Box.createHorizontalStrut(3));

		fold = new JLabel("fold");
		fold.setBorder(border);
		box.add(fold);

		updateBufferStatus();
		updateMiscStatus();
		updateFoldStatus();

		box.add(Box.createHorizontalStrut(3));
		ioProgress = new MiniIOProgress();
		ioProgress.setBorder(border);
		ioProgress.addMouseListener(mouseHandler);
		box.add(ioProgress);


		// UI hack because BoxLayout does not give all components the
		// same height
		Dimension dim = multiSelect.getPreferredSize();
		dim.width = 40;
		// dim.height = <same as all other components>
		ioProgress.setPreferredSize(dim);

		add(BorderLayout.EAST,box);
	}

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
				setMessage(null);
			}
		});

		tempTimer.setInitialDelay(10000);
		tempTimer.setRepeats(false);
		tempTimer.start();
	}

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
			InputHandler inputHandler = view.getInputHandler();
			if(inputHandler.isRepeatEnabled())
			{
				int repeatCount = inputHandler.getRepeatCount();

				this.message.setText(jEdit.getProperty("view.status.repeat",
					new Object[] { repeatCount == 1 ? "" : String.valueOf(repeatCount) }));
			}
			else if(view.getMacroRecorder() != null)
				this.message.setText(jEdit.getProperty("view.status.recording"));
			else
				this.message.setText(null);
		}
		else
			this.message.setText(message);
	}

	public void setMessageComponent(Component comp)
	{
		if (comp == null || messageComp == comp)
		{
			return;
		}

		messageComp = comp;
		messagePanel.add(BorderLayout.CENTER, messageComp);
	}

	public void repaintCaretStatus()
	{
		caretStatus.repaint();
	}

	public void updateBufferStatus()
	{
		Buffer buffer = view.getBuffer();
		mode.setText(buffer.getMode().getName());
		encoding.setText(buffer.getProperty("encoding").toString());
	}

	public void updateMiscStatus()
	{
		JEditTextArea textArea = view.getTextArea();

		if(textArea.isMultipleSelectionEnabled())
			multiSelect.setForeground(Color.black);
		else
		{
			if(textArea.getSelectionCount() > 1)
			{
				multiSelect.setForeground(UIManager.getColor(
					"Label.foreground"));
			}
			else
				multiSelect.setForeground(gray);
		}

		if(textArea.isOverwriteEnabled())
			overwrite.setForeground(Color.black);
		else
			overwrite.setForeground(gray);
	}

	public void updateFoldStatus()
	{
		Buffer buffer = view.getBuffer();
		if(buffer.getLineCount() != buffer.getVirtualLineCount())
			fold.setForeground(Color.black);
		else
			fold.setForeground(gray);
	}

	// private members
	private View view;
	private VICaretStatus caretStatus;
	private JPanel messagePanel;
	private Component messageComp;
	private JLabel message;
	private JLabel mode;
	private JLabel encoding;
	private JLabel multiSelect;
	private JLabel overwrite;
	private JLabel fold;
	private MiniIOProgress ioProgress;
	private Color gray = new Color(142,142,142);
	/* package-private for speed */ StringBuffer buf = new StringBuffer();
	private Timer tempTimer;

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			Object source = evt.getSource();
			if(source == mode || source == encoding)
				new BufferOptions(view,view.getBuffer());
			else if(source == multiSelect)
				view.getTextArea().toggleMultipleSelectionEnabled();
			else if(source == overwrite)
				view.getTextArea().toggleOverwriteEnabled();
			else if(source == ioProgress)
				new IOProgressMonitor(view);
		}
	}

	class VICaretStatus extends JComponent
	{
		public VICaretStatus()
		{
			VICaretStatus.this.setForeground(UIManager.getColor("Button.foreground"));
			VICaretStatus.this.setBackground(UIManager.getColor("Label.background"));
			VICaretStatus.this.setFont(UIManager.getFont("Label.font"));

			Dimension size = new Dimension(
				VICaretStatus.this.getFontMetrics(
				VICaretStatus.this.getFont())
				.stringWidth(testStr),0);
			VICaretStatus.this.setPreferredSize(size);
		}

		public void paintComponent(Graphics g)
		{
			Buffer buffer = view.getBuffer();

			if(!buffer.isLoaded())
				return;

			FontMetrics fm = g.getFontMetrics();

			JEditTextArea textArea = view.getTextArea();

			int currLine = textArea.getCaretLine();
			int dot = textArea.getCaretPosition()
				- textArea.getLineStartOffset(currLine);
			int virtualPosition = getVirtualPosition(dot,buffer,textArea);

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
			int lineCount = textArea.getVirtualLineCount();

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

			g.drawString(buf.toString(),
				VICaretStatus.this.getBorder().getBorderInsets(this).left + 1,
				(VICaretStatus.this.getHeight() + fm.getAscent()) / 2 - 1);
		}

		// private members
		private static final String testStr = "9999,999-999 99%";

		private Segment seg = new Segment();

		private int getVirtualPosition(int dot, Buffer buffer, JEditTextArea textArea)
		{
			int line = textArea.getCaretLine();

			textArea.getLineText(line, seg);

			int virtualPosition = 0;
			int tabSize = buffer.getTabSize();

			for (int i = 0; i < seg.count && i < dot; ++i)
			{
				char ch = seg.array[seg.offset + i];

				if (ch == '\t')
				{
					virtualPosition += tabSize
						- (virtualPosition % tabSize);
				}
				else
				{
					++virtualPosition;
				}
			}

			return virtualPosition;
		}
	}

	class MiniIOProgress extends JComponent
		implements WorkThreadProgressListener
	{
		public MiniIOProgress()
		{
			MiniIOProgress.this.setDoubleBuffered(true);
			MiniIOProgress.this.setForeground(UIManager.getColor("Button.foreground"));
			MiniIOProgress.this.setBackground(UIManager.getColor("Button.background"));

			icon = GUIUtilities.loadIcon("io.gif");
		}

		public void addNotify()
		{
			super.addNotify();
			VFSManager.getIOThreadPool().addProgressListener(this);
		}

		public void removeNotify()
		{
			super.removeNotify();
			VFSManager.getIOThreadPool().removeProgressListener(this);
		}

		public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
		{
			MiniIOProgress.this.repaint();
		}

		public void paintComponent(Graphics g)
		{
			WorkThreadPool ioThreadPool = VFSManager.getIOThreadPool();
			if(ioThreadPool.getThreadCount() == 0)
				return;

			FontMetrics fm = g.getFontMetrics();

			if(ioThreadPool.getRequestCount() == 0)
				return;
			else
			{
				icon.paintIcon(this,g,MiniIOProgress.this.getWidth()
					- icon.getIconWidth() - 3,
					(MiniIOProgress.this.getHeight()
					- icon.getIconHeight()) / 2);
			}

			Insets insets = MiniIOProgress.this.getBorder().getBorderInsets(this);

			int progressHeight = (MiniIOProgress.this.getHeight() - insets.top - insets.bottom)
				/ ioThreadPool.getThreadCount();
			int progressWidth = MiniIOProgress.this.getWidth()
				- icon.getIconWidth() - insets.left - insets.right - 2;

			for(int i = 0; i < ioThreadPool.getThreadCount(); i++)
			{
				WorkThread thread = ioThreadPool.getThread(i);
				int max = thread.getProgressMaximum();
				if(!thread.isRequestRunning() || max == 0)
					continue;

				int value = thread.getProgressValue();
				double progressRatio = ((double)value / max);

				// when loading gzip files, for example,
				// progressValue (data read) can be larger
				// than progressMaximum (file size)
				progressRatio = Math.min(progressRatio,1.0);

				g.fillRect(insets.left,insets.top + i * progressHeight,
					(int)(progressRatio * progressWidth),progressHeight);
			}
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(40,icon.getIconHeight());
		}

		// private members
		private Icon icon;
	}
}
