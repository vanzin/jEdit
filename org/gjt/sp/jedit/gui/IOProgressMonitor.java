/*
 * IOProgressMonitor.java - I/O progress monitor
 * Copyright (C) 2000 Slava Pestov
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
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

public class IOProgressMonitor extends JFrame
{
	public IOProgressMonitor()
	{
		super(jEdit.getProperty("io-progress-monitor.title"));

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		caption = new JLabel();
		updateCaption();
		content.add(BorderLayout.NORTH,caption);

		Box threadBox = new Box(BoxLayout.Y_AXIS);
		threads = new ThreadProgress[VFSManager.getIOThreadPool()
			.getThreadCount()];
		for(int i = 0; i < threads.length; i++)
		{
			threadBox.add(Box.createVerticalStrut(6));

			threads[i] = new ThreadProgress(i);
			threadBox.add(threads[i]);
		}

		content.add(BorderLayout.CENTER,threadBox);

		workThreadHandler = new WorkThreadHandler();
		VFSManager.getIOThreadPool().addProgressListener(workThreadHandler);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		GUIUtilities.loadGeometry(this,"io-progress-monitor");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"io-progress-monitor");
		VFSManager.getIOThreadPool().removeProgressListener(workThreadHandler);
		super.dispose();
	}

	// private members
	private JLabel caption;
	private ThreadProgress[] threads;
	private WorkThreadHandler workThreadHandler;

	private void updateCaption()
	{
		String[] args = { String.valueOf(VFSManager.getIOThreadPool()
			.getRequestCount()) };
		caption.setText(jEdit.getProperty("io-progress-monitor.caption",args));
	}

	class WorkThreadHandler implements WorkThreadProgressListener
	{
		public void statusUpdate(final WorkThreadPool pool, final int index)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateCaption();
					threads[index].update();
				}
			});
		}

		public void progressUpdate(final WorkThreadPool pool, final int index)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateCaption();
					threads[index].update();
				}
			});
		}
	}

	class ThreadProgress extends JPanel
	{
		public ThreadProgress(int index)
		{
			super(new BorderLayout());

			this.index = index;

			JPanel box = new JPanel();
			box.setBorder(new EmptyBorder(0,0,0,12));
			box.setLayout(new BoxLayout(box,BoxLayout.Y_AXIS));
			box.add(Box.createGlue());
			box.add(progress = new JProgressBar());
			progress.setStringPainted(true);
			box.add(Box.createGlue());
			ThreadProgress.this.add(BorderLayout.CENTER,box);

			abort = new JButton(jEdit.getProperty("io-progress-monitor.abort"));
			abort.addActionListener(new ActionHandler());
			ThreadProgress.this.add(BorderLayout.EAST,abort);

			update();
		}

		public void update()
		{
			WorkThread thread = VFSManager.getIOThreadPool().getThread(index);
			if(thread.isRequestRunning())
			{
				abort.setEnabled(true);
				progress.setString(thread.getStatus());
				progress.setMaximum(thread.getProgressMaximum());
				progress.setValue(thread.getProgressValue());
			}
			else
			{
				abort.setEnabled(false);
				progress.setString(jEdit.getProperty("io-progress-monitor"
					+ ".idle"));
				progress.setValue(0);
			}
		}

		// private members
		private int index;
		private JProgressBar progress;
		private JButton abort;

		class ActionHandler implements ActionListener
		{
			public void actionPerformed(ActionEvent evt)
			{
				if(evt.getSource() == abort)
				{
					int result = GUIUtilities.confirm(
						IOProgressMonitor.this,"abort",null,
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
					if(result == JOptionPane.YES_OPTION)
					{
						VFSManager.getIOThreadPool().getThread(index)
							.abortCurrentRequest();
					}
				}
			}
		}
	}
}
