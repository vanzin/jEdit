/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import javax.swing.border.*;
import javax.swing.text.Segment;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * The status bar, used for the following:
 * <ul>
 * <li>Displaying caret position information
 * <li>Displaying readNextChar() prompts
 * <li>Displaying the status of the overwrite, multi select flags
 * <li>I/O progress
 * <li>Memory status
 * <li>And so on
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
		super(new BorderLayout(3,3));
		setBorder(BorderFactory.createEmptyBorder(3,0,0,0));

		this.view = view;

		Border border = BorderFactory.createLoweredBevelBorder();

		MouseHandler mouseHandler = new MouseHandler();

		caretStatus = new VICaretStatus();
		caretStatus.setBorder(border);
		caretStatus.setToolTipText(jEdit.getProperty("view.status.caret-tooltip"));
		caretStatus.addMouseListener(mouseHandler);
		add(BorderLayout.WEST,caretStatus);

		messagePanel = new JPanel();
		messagePanel.setLayout(new BorderLayout(0,0));
		messagePanel.setBorder(border);
		messagePanel.setPreferredSize(caretStatus.getPreferredSize());
		add(BorderLayout.CENTER,messagePanel);

		message = new JLabel();
		message.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		setMessageComponent(message);

		Box box = new Box(BoxLayout.X_AXIS);
		mode = new JLabel();
		mode.setBorder(border);
		mode.setToolTipText(jEdit.getProperty("view.status.mode-tooltip"));
		mode.addMouseListener(mouseHandler);
		box.add(mode);
		box.add(Box.createHorizontalStrut(3));

		encoding = new JLabel();
		encoding.setBorder(border);
		encoding.setToolTipText(jEdit.getProperty("view.status.encoding-tooltip"));
		encoding.addMouseListener(mouseHandler);
		box.add(encoding);
		box.add(Box.createHorizontalStrut(3));

		FontMetrics fm = getToolkit().getFontMetrics(
			UIManager.getFont("Label.font"));

		foldMode = new JLabel();
		foldMode.setHorizontalAlignment(SwingConstants.CENTER);
		foldMode.setBorder(border);
		foldMode.setToolTipText(jEdit.getProperty("view.status.fold-tooltip"));
		foldMode.addMouseListener(mouseHandler);

		Dimension dim = foldMode.getPreferredSize();
		dim.width += Math.max(fm.stringWidth("none"),
			Math.max(
			fm.stringWidth("indent"),
			fm.stringWidth("explicit")));
		foldMode.setPreferredSize(dim);

		box.add(foldMode);
		box.add(Box.createHorizontalStrut(3));

		multiSelect = new JLabel();
		multiSelect.setHorizontalAlignment(SwingConstants.CENTER);
		multiSelect.setBorder(border);
		multiSelect.setToolTipText(jEdit.getProperty("view.status.multi-tooltip"));
		multiSelect.addMouseListener(mouseHandler);

		dim = multiSelect.getPreferredSize();
		dim.width += Math.max(fm.stringWidth("single"),
			fm.stringWidth("multi"));
		multiSelect.setPreferredSize(dim);

		box.add(multiSelect);
		box.add(Box.createHorizontalStrut(3));

		overwrite = new JLabel();
		overwrite.setHorizontalAlignment(SwingConstants.CENTER);
		overwrite.setBorder(border);
		overwrite.setToolTipText(jEdit.getProperty("view.status.overwrite-tooltip"));
		overwrite.addMouseListener(mouseHandler);

		dim = overwrite.getPreferredSize();
		dim.width += Math.max(fm.stringWidth("ovr"),
			fm.stringWidth("ins"));
		overwrite.setPreferredSize(dim);

		box.add(overwrite);

		updateBufferStatus();
		updateMiscStatus();

		box.add(Box.createHorizontalStrut(3));
		memory = new MemoryStatus();
		memory.setBorder(border);
		memory.setToolTipText(jEdit.getProperty("view.status.memory-tooltip"));
		memory.addMouseListener(mouseHandler);
		box.add(memory);

		// UI hack because BoxLayout does not give all components the
		// same height
		dim = memory.getPreferredSize();
		dim.width += fm.stringWidth("99Mb/999Mb");
		dim.height = multiSelect.getPreferredSize().height;
		memory.setPreferredSize(dim);

		add(BorderLayout.EAST,box);
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		VFSManager.getIOThreadPool().addProgressListener(this);
	} //}}}

	//{{{ removeNotify() method
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
				int requestCount = threadPool.getRequestCount();
				if(requestCount == 0)
				{
					setMessageAndClear(jEdit.getProperty(
						"view.status.io.done"));
				}
				else if(requestCount == 1)
				{
					setMessage(jEdit.getProperty(
						"view.status.io-1"));
				}
				else
				{
					Object[] args = { new Integer(requestCount) };
					setMessage(jEdit.getProperty(
						"view.status.io",args));
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
				setMessage(null);
			}
		});

		tempTimer.setInitialDelay(10000);
		tempTimer.setRepeats(false);
		tempTimer.start();
	} //}}}

	//{{{ setMessage() method
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
	} //}}}

	//{{{ setMessageComponent() method
	public void setMessageComponent(Component comp)
	{
		if (comp == null || messageComp == comp)
		{
			return;
		}

		messageComp = comp;
		messagePanel.add(BorderLayout.CENTER, messageComp);
	} //}}}

	//{{{ repaintCaretStatus() method
	public void repaintCaretStatus()
	{
		caretStatus.repaint();
	} //}}}

	//{{{ updateBufferStatus() method
	public void updateBufferStatus()
	{
		Buffer buffer = view.getBuffer();
		mode.setText(buffer.getMode().getName());
		encoding.setText(buffer.getStringProperty("encoding"));
	} //}}}

	//{{{ updateMiscStatus() method
	public void updateMiscStatus()
	{
		JEditTextArea textArea = view.getTextArea();

		multiSelect.setText(textArea.isMultipleSelectionEnabled()
			? "multi" : "single");
		overwrite.setText(textArea.isOverwriteEnabled()
			? "ovr" : "ins");
		foldMode.setText((String)textArea.getBuffer().getProperty("folding"));
	} //}}}

	//{{{ Private members
	private View view;
	private VICaretStatus caretStatus;
	private JPanel messagePanel;
	private Component messageComp;
	private JLabel message;
	private JLabel mode;
	private JLabel encoding;
	private JLabel foldMode;
	private JLabel multiSelect;
	private JLabel overwrite;
	private MemoryStatus memory;
	/* package-private for speed */ StringBuffer buf = new StringBuffer();
	private Timer tempTimer;
	//}}}

	static final String testStr = "9999,999-999 99%";

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		//{{{ mouseClicked() method
		public void mouseClicked(MouseEvent evt)
		{
			Object source = evt.getSource();
			if(source == caretStatus)
			{
				if(evt.getClickCount() == 2)
					view.getTextArea().showGoToLineDialog();
			}
			else if(source == mode || source == encoding)
			{
				if(evt.getClickCount() == 2)
					new BufferOptions(view,view.getBuffer());
			}
			else if(source == foldMode)
			{
				String text = foldMode.getText();
				if(text.equals("none"))
					text = "indent";
				else if(text.equals("indent"))
					text = "explicit";
				else if(text.equals("explicit"))
					text = "none";

				JEditTextArea textArea = view.getTextArea();
				Buffer buffer = view.getBuffer();
				buffer.putProperty("folding",text);
				buffer.propertiesChanged();

				Integer collapseFolds = (Integer)buffer.getProperty(
					"collapseFolds");
				if(collapseFolds != null && collapseFolds.intValue() != 0)
				{
					textArea.getFoldVisibilityManager()
						.expandFolds(collapseFolds.intValue());
				}
				else
				{
					textArea.getFoldVisibilityManager()
						.expandAllFolds();
				}
			}
			else if(source == multiSelect)
				view.getTextArea().toggleMultipleSelectionEnabled();
			else if(source == overwrite)
				view.getTextArea().toggleOverwriteEnabled();
			else if(source == memory)
			{
				if(evt.getClickCount() == 2)
					jEdit.showMemoryDialog(view);
			}
		} //}}}
	} //}}}

	//{{{ VICaretStatus class
	class VICaretStatus extends JComponent
	{
		//{{{ VICaretStatus constructor
		public VICaretStatus()
		{
			VICaretStatus.this.setForeground(UIManager.getColor("Label.foreground"));
			VICaretStatus.this.setBackground(UIManager.getColor("Label.background"));
			VICaretStatus.this.setFont(UIManager.getFont("Label.font"));

			Dimension size = new Dimension(
				VICaretStatus.this.getFontMetrics(
				VICaretStatus.this.getFont())
				.stringWidth(testStr),0);
			VICaretStatus.this.setPreferredSize(size);
		} //}}}

		//{{{ paintComponent() method
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
		} //}}}

		//{{{ Private members
		private Segment seg = new Segment();

		//{{{ getVirtualPosition() method
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
		} //}}}
	} //}}}

	//{{{ MemoryStatus class
	class MemoryStatus extends JComponent implements ActionListener
	{
		//{{{ MemoryStatus constructor
		public MemoryStatus()
		{
			MemoryStatus.this.setDoubleBuffered(true);
			MemoryStatus.this.setForeground(UIManager.getColor("Label.foreground"));
			MemoryStatus.this.setBackground(UIManager.getColor("Label.background"));
			MemoryStatus.this.setFont(UIManager.getFont("Label.font"));
		} //}}}

		//{{{ addNotify() method
		public void addNotify()
		{
			super.addNotify();
			timer = new Timer(2000,this);
			timer.start();
		} //}}}

		//{{{ removeNotify() method
		public void removeNotify()
		{
			timer.stop();
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			MemoryStatus.this.repaint();
		} //}}}

		//{{{ paintComponent() method
		public void paintComponent(Graphics g)
		{
			Insets insets = MemoryStatus.this.getBorder().getBorderInsets(this);

			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int)(runtime.freeMemory() / 1024 / 1024);
			int totalMemory = (int)(runtime.totalMemory() / 1024 / 1024);
			int usedMemory = (totalMemory - freeMemory);

			int width = MemoryStatus.this.getWidth()
				- insets.left - insets.right;

			Color text = MemoryStatus.this.getForeground();
			Color status = UIManager.getColor("ProgressBar.foreground");
			if(status.equals(text))
				g.setXORMode(MemoryStatus.this.getBackground());
			else
				g.setColor(status);

			float fraction = ((float)usedMemory) / totalMemory;

			g.fillRect(insets.left,insets.top,
				(int)(width * fraction),
				MemoryStatus.this.getHeight()
				- insets.top - insets.bottom);

			g.setPaintMode();

			g.setColor(text);

			String str = "" + usedMemory + "Mb/"
				+ totalMemory + "Mb";
			FontMetrics fm = g.getFontMetrics();

			g.drawString(str,
				insets.left + (width - fm.stringWidth(str)) / 2,
				insets.top + fm.getAscent());
		} //}}}

		private Timer timer;
	} //}}}
}
