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
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import org.gjt.sp.jedit.buffer.FoldHandler;
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
	private static final Icon RED_DISK = GUIUtilities.loadIcon("dirty.gif");
	private static final Icon BLUE_DISK = GUIUtilities.loadIcon("blue_disk.gif");

	//{{{ StatusBar constructor
	public StatusBar(View view)
	{
		super(new BorderLayout());
		setBorder(new CompoundBorder(new EmptyBorder(10,0,0,0),
			UIManager.getBorder("TextField.border")));

		this.view = view;

		panel = new JPanel(new BorderLayout());

		MouseHandler mouseHandler = new MouseHandler();

		caretStatus = new VICaretStatus();
		caretStatus.setToolTipText(jEdit.getProperty("view.status.caret-tooltip"));
		caretStatus.addMouseListener(mouseHandler);
		panel.add(BorderLayout.WEST,caretStatus);

		message = new JLabel();
		setMessageComponent(message);

		Box box = new Box(BoxLayout.X_AXIS);
		mode = new JLabel();
		mode.setToolTipText(jEdit.getProperty("view.status.mode-tooltip"));
		mode.addMouseListener(mouseHandler);
		box.add(mode);
		box.add(Box.createHorizontalStrut(4));

		multiSelect = new JLabel();
		multiSelect.setHorizontalAlignment(SwingConstants.CENTER);
		multiSelect.setToolTipText(jEdit.getProperty("view.status.multi-tooltip"));
		multiSelect.addMouseListener(mouseHandler);

		box.add(multiSelect);

		overwrite = new JLabel();
		overwrite.setHorizontalAlignment(SwingConstants.CENTER);
		overwrite.setToolTipText(jEdit.getProperty("view.status.overwrite-tooltip"));
		overwrite.addMouseListener(mouseHandler);

		box.add(overwrite);
		box.add(Box.createHorizontalStrut(4));

		memory = new MemoryStatus();
		memory.addMouseListener(mouseHandler);
		box.add(memory);

		panel.add(BorderLayout.EAST,box);

		add(BorderLayout.CENTER,panel);
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		TextAreaPainter painter = view.getTextArea().getPainter();
		panel.setBackground(painter.getBackground());
		panel.setForeground(painter.getForeground());

		Font font = /* painter. */getFont();
		caretStatus.setFont(font);
		message.setFont(font);
		mode.setFont(font);
		multiSelect.setFont(font);
		overwrite.setFont(font);
		memory.setFont(font);

		FontMetrics fm = getFontMetrics(font);

		FontRenderContext frc = new FontRenderContext(null,false,false);

		Rectangle2D bounds = font.getStringBounds(caretTestStr,frc);
		caretStatus.lm = font.getLineMetrics(caretTestStr,frc);

		Dimension dim = new Dimension((int)bounds.getWidth(),
			(int)bounds.getHeight());
		caretStatus.setPreferredSize(dim);

		dim = multiSelect.getPreferredSize();
		multiSelect.setPreferredSize(new Dimension(Math.max(
			(int)font.getStringBounds("-",frc).getWidth(),
			(int)font.getStringBounds("M",frc).getWidth()),
			dim.height));

		dim = overwrite.getPreferredSize();
		overwrite.setPreferredSize(new Dimension(Math.max(
			(int)font.getStringBounds("-",frc).getWidth(),
			(int)font.getStringBounds("O",frc).getWidth()),
			dim.height));

		// UI hack because BoxLayout does not give all components the
		// same height
		dim = memory.getPreferredSize();
		memory.setPreferredSize(new Dimension(
			(int)font.getStringBounds(memoryTestStr,frc).getWidth(),
			multiSelect.getPreferredSize().height));
		memory.lm = font.getLineMetrics(memoryTestStr,frc);
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
					message.setIcon(BLUE_DISK);
				}
				else if(requestCount == 1)
				{
					setMessage(jEdit.getProperty(
						"view.status.io-1"));
					message.setIcon(RED_DISK);
				}
				else
				{
					Object[] args = { new Integer(requestCount) };
					setMessage(jEdit.getProperty(
						"view.status.io",args));
					message.setIcon(RED_DISK);
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
	public void setMessage(String message)
	{
		this.message.setIcon(null);

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
		panel.add(BorderLayout.CENTER, messageComp);
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
		mode.setText("(" + buffer.getMode().getName() + ","
			+ (String)view.getBuffer().getProperty("folding") + ","
			+ buffer.getStringProperty("encoding") + ")");
	} //}}}

	//{{{ updateMiscStatus() method
	public void updateMiscStatus()
	{
		JEditTextArea textArea = view.getTextArea();

		multiSelect.setText(textArea.isMultipleSelectionEnabled()
			? "M" : "-");
		overwrite.setText(textArea.isOverwriteEnabled()
			? "O" : "-");
	} //}}}

	//{{{ Private members
	private View view;
	private JPanel panel;
	private VICaretStatus caretStatus;
	private Component messageComp;
	private JLabel message;
	private JLabel mode;
	private JLabel multiSelect;
	private JLabel overwrite;
	private MemoryStatus memory;
	/* package-private for speed */ StringBuffer buf = new StringBuffer();
	private Timer tempTimer;
	//}}}

	static final String caretTestStr = "9999,999-999 99%";
	static final String memoryTestStr = "999/999Mb";

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			Object source = evt.getSource();
			if(source == caretStatus)
			{
				if(evt.getClickCount() == 2)
					view.getTextArea().showGoToLineDialog();
			}
			else if(source == mode)
			{
				if(evt.getClickCount() == 2)
					new BufferOptions(view,view.getBuffer());
			}
			else if(source == multiSelect)
				view.getTextArea().toggleMultipleSelectionEnabled();
			else if(source == overwrite)
				view.getTextArea().toggleOverwriteEnabled();
			else if(source == memory)
			{
				if(evt.getClickCount() == 2)
				{
					jEdit.showMemoryDialog(view);
					memory.repaint();
				}
			}
		}
	} //}}}

	//{{{ VICaretStatus class
	class VICaretStatus extends JComponent
	{
		//{{{ VICaretStatus constructor
		public VICaretStatus()
		{
		} //}}}

		//{{{ paintComponent() method
		public void paintComponent(Graphics g)
		{
			Buffer buffer = view.getBuffer();

			if(!buffer.isLoaded())
				return;

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
				/*VICaretStatus.this.getBorder().getBorderInsets(this).left +*/ 1,
				(int)((VICaretStatus.this.getHeight() - lm.getHeight()) / 2
				+ lm.getAscent()));
		} //}}}

		/* package-private */ LineMetrics lm;

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

		//}}}
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
			ToolTipManager.sharedInstance().registerComponent(this);
		} //}}}

		//{{{ removeNotify() method
		public void removeNotify()
		{
			timer.stop();
			ToolTipManager.sharedInstance().unregisterComponent(this);
		} //}}}

		//{{{ getToolTipText() method
		public String getToolTipText()
		{
			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int)(runtime.freeMemory() / 1024);
			int totalMemory = (int)(runtime.totalMemory() / 1024);
			int usedMemory = (totalMemory - freeMemory);
			Integer[] args = { new Integer(usedMemory),
				new Integer(totalMemory) };
			return jEdit.getProperty("view.status.memory-tooltip",args);
		} //}}}

		//{{{ getToolTipLocation() method
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			MemoryStatus.this.repaint();
		} //}}}

		/* package-private */ LineMetrics lm;

		//{{{ paintComponent() method
		public void paintComponent(Graphics g)
		{
			Insets insets = new Insets(0,0,0,0);//MemoryStatus.this.getBorder().getBorderInsets(this);

			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int)(runtime.freeMemory() / 1024);
			int totalMemory = (int)(runtime.totalMemory() / 1024);
			int usedMemory = (totalMemory - freeMemory);

			int width = MemoryStatus.this.getWidth()
				- insets.left - insets.right;
			int height = MemoryStatus.this.getHeight()
				- insets.top - insets.bottom - 1;

			float fraction = ((float)usedMemory) / totalMemory;

			g.setColor(UIManager.getColor("ProgressBar.selectionBackground"));

			g.fillRect(insets.left,insets.top,
				(int)(width * fraction),
				height);

			String str = (usedMemory / 1024) + "/"
				+ (totalMemory / 1024) + "Mb";

			FontRenderContext frc = new FontRenderContext(null,false,false);

			Rectangle2D bounds = g.getFont().getStringBounds(str,frc);
		
			Graphics g2 = g.create();
			g2.setClip(insets.left,insets.top,
				(int)(width * fraction),
				height);

			g2.setColor(UIManager.getColor("ProgressBar.selectionForeground"));

			g2.drawString(str,
				insets.left + (int)(width - bounds.getWidth()) / 2,
				(int)(insets.top + lm.getAscent()));

			g2.dispose();

			g2 = g.create();

			g2.setClip(insets.left + (int)(width * fraction),
				insets.top,MemoryStatus.this.getWidth()
				- insets.left - (int)(width * fraction),
				height);

			g2.setColor(MemoryStatus.this.getForeground());

			g2.drawString(str,
				insets.left + (int)(width - bounds.getWidth()) / 2,
				(int)(insets.top + lm.getAscent()));

			g2.dispose();
		} //}}}

		private Timer timer;
	} //}}}
}
