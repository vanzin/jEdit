/*
 * IOProgressMonitor.java - I/O progress monitor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2002 Slava Pestov
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
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

/**
 * The IO progressMonitor is the panel that will show JProgressBar for
 * IO threads.
 *
 * @version $Id$
 */
public class IOProgressMonitor extends JPanel
{
	//{{{ IOProgressMonitor constructor
	public IOProgressMonitor()
	{
		super(new BorderLayout());
		caption = new JLabel();
		updateCaption();
		add(BorderLayout.NORTH,caption);

		threads = new ThreadProgress[VFSManager.getIOThreadPool()
			.getThreadCount()];

		Box box = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < threads.length; i++)
		{
			if(i != 0)
				box.add(Box.createVerticalStrut(6));

			threads[i] = new ThreadProgress(i);
			box.add(threads[i]);
		}

		JPanel threadPanel = new JPanel(new BorderLayout());
		threadPanel.setBorder(new EmptyBorder(6,6,6,6));
		threadPanel.add(BorderLayout.NORTH,box);

		add(BorderLayout.CENTER,new JScrollPane(threadPanel));

		workThreadHandler = new WorkThreadHandler();
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		VFSManager.getIOThreadPool().addProgressListener(workThreadHandler);
		super.addNotify();
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		VFSManager.getIOThreadPool().removeProgressListener(workThreadHandler);
		super.removeNotify();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JLabel caption;
	private ThreadProgress[] threads;
	private WorkThreadHandler workThreadHandler;
	//}}}

	//{{{ updateCaption() method
	private void updateCaption()
	{
		String[] args = { String.valueOf(VFSManager.getIOThreadPool()
			.getRequestCount()) };
		caption.setText(jEdit.getProperty("io-progress-monitor.caption",args));
	} //}}}

	//}}}

	//{{{ WorkThreadHandler class
	class WorkThreadHandler implements WorkThreadProgressListener
	{
		public void statusUpdate(final WorkThreadPool threadPool, final int threadIndex)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateCaption();
					threads[threadIndex].update();
				}
			});
		}

		public void progressUpdate(final WorkThreadPool threadPool, final int threadIndex)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateCaption();
					threads[threadIndex].update();
				}
			});
		}
	} //}}}

	//{{{ ThreadProgress class
	class ThreadProgress extends JPanel
	{
		//{{{ ThreadProgress constructor
		public ThreadProgress(int index)
		{
			super(new BorderLayout(12,12));

			this.index = index;

			Box box = new Box(BoxLayout.Y_AXIS);
			box.add(Box.createGlue());
			box.add(progress = new JProgressBar());
			progress.setStringPainted(true);
			box.add(Box.createGlue());
			ThreadProgress.this.add(BorderLayout.CENTER,box);

			abort = new JButton(jEdit.getProperty("io-progress-monitor.abort"));
			abort.addActionListener(new ActionHandler());
			ThreadProgress.this.add(BorderLayout.EAST,abort);

			update();
		} //}}}

		//{{{ update() method
		public void update()
		{
			WorkThread thread = VFSManager.getIOThreadPool().getThread(index);
			if(thread.isRequestRunning())
			{
				if (progress.isIndeterminate())
				{
					if (thread.getProgressMaximum() != 0)
						progress.setIndeterminate(false);
				}
				else if (thread.getProgressMaximum() == 0)
					progress.setIndeterminate(true);
				
				abort.setEnabled(true);
				String status = thread.getStatus();
				if(status == null)
					status = "";
				progress.setString(status);
				progress.setMaximum(thread.getProgressMaximum());
				//System.err.println("value: " + thread.getProgressValue());
				progress.setValue(thread.getProgressValue());
			}
			else
			{
				abort.setEnabled(false);
				progress.setString(jEdit.getProperty("io-progress-monitor"
					+ ".idle"));
				progress.setIndeterminate(false);
				progress.setValue(0);
			}
		} //}}}

		//{{{ Private members
		private int index;
		private JProgressBar progress;
		private JButton abort;
		//}}}

		//{{{ ActionHandler class
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
		} //}}}
	} //}}}
}
