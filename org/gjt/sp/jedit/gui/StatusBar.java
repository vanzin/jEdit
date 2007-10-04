/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
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
import java.text.*;
import java.util.Date;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
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

		mode = new ToolTipLabel();
		mode.setToolTipText(jEdit.getProperty("view.status.mode-tooltip"));
		mode.addMouseListener(mouseHandler);

		wrap = new ToolTipLabel();
		wrap.setHorizontalAlignment(SwingConstants.CENTER);
		wrap.setToolTipText(jEdit.getProperty("view.status.wrap-tooltip"));
		wrap.addMouseListener(mouseHandler);

		multiSelect = new ToolTipLabel();
		multiSelect.setHorizontalAlignment(SwingConstants.CENTER);
		multiSelect.setToolTipText(jEdit.getProperty("view.status.multi-tooltip"));
		multiSelect.addMouseListener(mouseHandler);

		rectSelect = new ToolTipLabel();
		rectSelect.setHorizontalAlignment(SwingConstants.CENTER);
		rectSelect.setToolTipText(jEdit.getProperty("view.status.rect-tooltip"));
		rectSelect.addMouseListener(mouseHandler);

		overwrite = new ToolTipLabel();
		overwrite.setHorizontalAlignment(SwingConstants.CENTER);
		overwrite.setToolTipText(jEdit.getProperty("view.status.overwrite-tooltip"));
		overwrite.addMouseListener(mouseHandler);

		lineSep = new ToolTipLabel();
		lineSep.setHorizontalAlignment(SwingConstants.CENTER);
		lineSep.setToolTipText(jEdit.getProperty("view.status.linesep-tooltip"));
		lineSep.addMouseListener(mouseHandler);
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		Color fg = jEdit.getColorProperty("view.status.foreground");
		Color bg = jEdit.getColorProperty("view.status.background");

		showCaretStatus = jEdit.getBooleanProperty("view.status.show-caret-status");
		showEditMode = jEdit.getBooleanProperty("view.status.show-edit-mode");
		showFoldMode = jEdit.getBooleanProperty("view.status.show-fold-mode");
		showEncoding = jEdit.getBooleanProperty("view.status.show-encoding");
		showWrap = jEdit.getBooleanProperty("view.status.show-wrap");
		showMultiSelect = jEdit.getBooleanProperty("view.status.show-multi-select");
		showRectSelect = jEdit.getBooleanProperty("view.status.show-rect-select");
		showOverwrite = jEdit.getBooleanProperty("view.status.show-overwrite");
		showLineSeperator = jEdit.getBooleanProperty("view.status.show-line-seperator");
		boolean showMemory = jEdit.getBooleanProperty("view.status.show-memory");
		boolean showClock = jEdit.getBooleanProperty("view.status.show-clock");

		panel.setBackground(bg);
		panel.setForeground(fg);
		caretStatus.setBackground(bg);
		caretStatus.setForeground(fg);
		message.setBackground(bg);
		message.setForeground(fg);
		mode.setBackground(bg);
		mode.setForeground(fg);
		wrap.setBackground(bg);
		wrap.setForeground(fg);
		multiSelect.setBackground(bg);
		multiSelect.setForeground(fg);
		rectSelect.setBackground(bg);
		rectSelect.setForeground(fg);
		overwrite.setBackground(bg);
		overwrite.setForeground(fg);
		lineSep.setBackground(bg);
		lineSep.setForeground(fg);

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

		box.removeAll();

		if (showEncoding || showEditMode || showFoldMode)
			box.add(mode);

		if (showWrap)
		{
			dim = new Dimension(Math.max(
				Math.max(fm.charWidth('-'),fm.charWidth('H')),
				fm.charWidth('S')) + 1,fm.getHeight());
			wrap.setPreferredSize(dim);
			wrap.setMaximumSize(dim);
			box.add(wrap);
		}

		if (showMultiSelect)
		{
			dim = new Dimension(
				Math.max(fm.charWidth('-'),fm.charWidth('M')) + 1,
				fm.getHeight());
			multiSelect.setPreferredSize(dim);
			multiSelect.setMaximumSize(dim);
			box.add(multiSelect);
		}

		if (showRectSelect)
		{
			dim = new Dimension(
				Math.max(fm.charWidth('-'),fm.charWidth('R')) + 1,
				fm.getHeight());
			rectSelect.setPreferredSize(dim);
			rectSelect.setMaximumSize(dim);
			box.add(rectSelect);
		}

		if (showOverwrite)
		{
			dim = new Dimension(
				Math.max(fm.charWidth('-'),fm.charWidth('O')) + 1,
				fm.getHeight());
			overwrite.setPreferredSize(dim);
			overwrite.setMaximumSize(dim);
			box.add(overwrite);
		}

		if (showLineSeperator)
		{
			dim = new Dimension(Math.max(
				Math.max(fm.charWidth('U'),
				fm.charWidth('W')),
				fm.charWidth('M')) + 1,
				fm.getHeight());
			lineSep.setPreferredSize(dim);
			lineSep.setMaximumSize(dim);
			box.add(lineSep);
		}

		if (showMemory)
			box.add(new MemoryStatus());

		if (showClock)
			box.add(new Clock());

		updateBufferStatus();
		updateMiscStatus();
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
		//if(!isShowing())
		//	return;

		Buffer buffer = view.getBuffer();

		if (showWrap)
		{
			String wrap = buffer.getStringProperty("wrap");
			if(wrap.equals("none"))
				this.wrap.setText("-");
			else if(wrap.equals("hard"))
				this.wrap.setText("H");
			else if(wrap.equals("soft"))
				this.wrap.setText("S");
		}

		if (showLineSeperator)
		{
			String lineSep = buffer.getStringProperty("lineSeparator");
			if("\n".equals(lineSep))
				this.lineSep.setText("U");
			else if("\r\n".equals(lineSep))
				this.lineSep.setText("W");
			else if("\r".equals(lineSep))
				this.lineSep.setText("M");
		}

		if (showEditMode || showFoldMode || showEncoding)
		{
			/* This doesn't look pretty and mode line should
			 * probably be split up into seperate
			 * components/strings
			 */
			buf.setLength(0);

			if (buffer.isLoaded())
			{
				if (showEditMode)
					buf.append(buffer.getMode().getName());
				if (showFoldMode)
				{
					if (showEditMode)
						buf.append(',');
					buf.append((String)view.getBuffer().getProperty("folding"));
				}
				if (showEncoding)
				{
					if (showEditMode || showFoldMode)
						buf.append(',');
					buf.append(buffer.getStringProperty("encoding"));
				}
			}

			mode.setText('(' + buf.toString() + ')');
		}
	} //}}}

	//{{{ updateMiscStatus() method
	public void updateMiscStatus()
	{
		//if(!isShowing())
		//	return;

		JEditTextArea textArea = view.getTextArea();

		if (showMultiSelect)
			multiSelect.setText(textArea.isMultipleSelectionEnabled()
				? "M" : "-");

		if (showRectSelect)
			rectSelect.setText(textArea.isRectangularSelectionEnabled()
				? "R" : "-");

		if (showOverwrite)
			overwrite.setText(textArea.isOverwriteEnabled()
				? "O" : "-");
	} //}}}

	//{{{ Private members
	private View view;
	private JPanel panel;
	private Box box;
	private ToolTipLabel caretStatus;
	private Component messageComp;
	private JLabel message;
	private JLabel mode;
	private JLabel wrap;
	private JLabel multiSelect;
	private JLabel rectSelect;
	private JLabel overwrite;
	private JLabel lineSep;
	/* package-private for speed */ StringBuilder buf = new StringBuilder();
	private Timer tempTimer;
	private boolean currentMessageIsIO;

	private Segment seg = new Segment();

	private boolean showCaretStatus;
	private boolean showEditMode;
	private boolean showFoldMode;
	private boolean showEncoding;
	private boolean showWrap;
	private boolean showMultiSelect;
	private boolean showRectSelect;
	private boolean showOverwrite;
	private boolean showLineSeperator;
	//}}}

	static final String caretTestStr = "9999,999-999 99%";

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			Buffer buffer = view.getBuffer();

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
			else if(source == wrap)
				buffer.toggleWordWrap(view);
			else if(source == multiSelect)
				view.getTextArea().toggleMultipleSelectionEnabled();
			else if(source == rectSelect)
				view.getTextArea().toggleRectangularSelectionEnabled();
			else if(source == overwrite)
				view.getTextArea().toggleOverwriteEnabled();
			else if(source == lineSep)
				buffer.toggleLineSeparator(view);
		}
	} //}}}

	//{{{ ToolTipLabel class
	static class ToolTipLabel extends JLabel
	{
		//{{{ getToolTipLocation() method
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}
	} //}}}

	//{{{ MemoryStatus class
	class MemoryStatus extends JComponent implements ActionListener
	{
		//{{{ MemoryStatus constructor
		MemoryStatus()
		{
			// fucking GTK look and feel
			Font font = new JLabel().getFont();
			//Font font = UIManager.getFont("Label.font");
			MemoryStatus.this.setFont(font);

			FontRenderContext frc = new FontRenderContext(
				null,false,false);
			Rectangle2D bounds = font.getStringBounds(
				memoryTestStr,frc);
			Dimension dim = new Dimension((int)bounds.getWidth(),
				(int)bounds.getHeight());
			setPreferredSize(dim);
			setMaximumSize(dim);
			lm = font.getLineMetrics(memoryTestStr,frc);

			setForeground(jEdit.getColorProperty("view.status.foreground"));
			setBackground(jEdit.getColorProperty("view.status.background"));

			progressForeground = jEdit.getColorProperty(
				"view.status.memory.foreground");
			progressBackground = jEdit.getColorProperty(
				"view.status.memory.background");

			addMouseListener(new MouseHandler());
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
			super.removeNotify();
		} //}}}

		//{{{ getToolTipText() method
		public String getToolTipText()
		{
			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int)(runtime.freeMemory() >> 10);
			int totalMemory = (int)(runtime.totalMemory() >> 10);
			int usedMemory = totalMemory - freeMemory;
			args[0] = new Integer(usedMemory);
			args[1] = new Integer(totalMemory);
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

		//{{{ paintComponent() method
		public void paintComponent(Graphics g)
		{
			Insets insets = new Insets(0,0,0,0);//MemoryStatus.this.getBorder().getBorderInsets(this);

			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int)(runtime.freeMemory() >> 10);
			int totalMemory = (int)(runtime.totalMemory() >> 10);
			int usedMemory = totalMemory - freeMemory;

			int width = MemoryStatus.this.getWidth()
				- insets.left - insets.right;
			int height = MemoryStatus.this.getHeight()
				- insets.top - insets.bottom - 1;

			float fraction = ((float)usedMemory) / totalMemory;

			g.setColor(progressBackground);

			g.fillRect(insets.left,insets.top,
				(int)(width * fraction),
				height);

			String str = (usedMemory >> 10) + "/"
				+ (totalMemory >> 10) + "Mb";

			FontRenderContext frc = new FontRenderContext(null,false,false);

			Rectangle2D bounds = g.getFont().getStringBounds(str,frc);

			Graphics g2 = g.create();
			g2.setClip(insets.left,insets.top,
				(int)(width * fraction),
				height);

			g2.setColor(progressForeground);

			g2.drawString(str,
				insets.left + ((int) (width - bounds.getWidth()) >> 1),
				(int)(insets.top + lm.getAscent()));

			g2.dispose();

			g2 = g.create();

			g2.setClip(insets.left + (int)(width * fraction),
				insets.top,MemoryStatus.this.getWidth()
				- insets.left - (int)(width * fraction),
				height);

			g2.setColor(MemoryStatus.this.getForeground());

			g2.drawString(str,
				insets.left + ((int) (width - bounds.getWidth()) >> 1),
				(int)(insets.top + lm.getAscent()));

			g2.dispose();
		} //}}}

		//{{{ Private members
		private static final String memoryTestStr = "999/999Mb";

		private final LineMetrics lm;
		private final Color progressForeground;
		private final Color progressBackground;

		private final Integer[] args = new Integer[2];


		private Timer timer;
		//}}}

		//{{{ MouseHandler class
		class MouseHandler extends MouseAdapter
		{
			public void mousePressed(MouseEvent evt)
			{
				if(evt.getClickCount() == 2)
				{
					jEdit.showMemoryDialog(view);
					repaint();
				}
			}
		} //}}}
	} //}}}

	//{{{ Clock class
	static class Clock extends JLabel implements ActionListener
	{
		//{{{ Clock constructor
		Clock()
		{
			/* FontRenderContext frc = new FontRenderContext(
				null,false,false);
			Rectangle2D bounds = getFont()
				.getStringBounds(getTime(),frc);
			Dimension dim = new Dimension((int)bounds.getWidth(),
				(int)bounds.getHeight());
			setPreferredSize(dim);
			setMaximumSize(dim); */

			setForeground(jEdit.getColorProperty("view.status.foreground"));
			setBackground(jEdit.getColorProperty("view.status.background"));
		} //}}}

		//{{{ addNotify() method
		public void addNotify()
		{
			super.addNotify();
			update();

			int millisecondsPerMinute = 1000 * 60;

			timer = new Timer(millisecondsPerMinute,this);
			timer.setInitialDelay((int)(
				millisecondsPerMinute
				- System.currentTimeMillis()
				% millisecondsPerMinute) + 500);
			timer.start();
			ToolTipManager.sharedInstance().registerComponent(this);
		} //}}}

		//{{{ removeNotify() method
		public void removeNotify()
		{
			timer.stop();
			ToolTipManager.sharedInstance().unregisterComponent(this);
			super.removeNotify();
		} //}}}

		//{{{ getToolTipText() method
		public String getToolTipText()
		{
			return new Date().toString();
		} //}}}

		//{{{ getToolTipLocation() method
		public Point getToolTipLocation(MouseEvent event)
		{
			return new Point(event.getX(),-20);
		} //}}}

		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			update();
		} //}}}

		//{{{ Private members
		private Timer timer;

		//{{{ getTime() method
		private static String getTime()
		{
			return DateFormat.getTimeInstance(
				DateFormat.SHORT).format(new Date());
		} //}}}

		//{{{ update() method
		private void update()
		{
			setText(getTime());
		} //}}}

		//}}}
	} //}}}
}
