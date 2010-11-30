/*
 * PluginManagerProgress.java - Plugin download progress meter
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.pluginmgr;

//{{{ Imports
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.ProgressObserver;
//}}}

class PluginManagerProgress extends JDialog implements ProgressObserver
{
	//{{{ PluginManagerProgress constructor
	PluginManagerProgress(PluginManager dialog, Roster roster)
	{
		super(dialog,jEdit.getProperty("plugin-manager.progress"),true);

		this.roster = roster;

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		progress = new JProgressBar();
		progress.setStringPainted(true);
		progress.setString(jEdit.getProperty("plugin-manager.progress"));

		count = roster.getOperationCount();
		int maximum = 0;
		for(int i = 0; i < count; i++)
		{
			maximum += roster.getOperation(i).getMaximum();
		}

		progress.setMaximum(maximum);
		content.add(BorderLayout.NORTH,progress);

		stop = new JButton(jEdit.getProperty("plugin-manager.progress.stop"));
		stop.addActionListener(new ActionHandler());
		JPanel panel = new JPanel(new FlowLayout(
			FlowLayout.CENTER,0,0));
		panel.add(stop);
		content.add(BorderLayout.CENTER,panel);

		addWindowListener(new WindowHandler());

		pack();
		setLocationRelativeTo(dialog);
		setVisible(true);
	} //}}}

	//{{{ setValue() method

	/**
	 * @param value the new value
	 * @deprecated Use {@link #setValue(long)}
	 */
	@Deprecated
	public void setValue(final int value)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progress.setValue(valueSoFar + value);
			}
		});
	} //}}}

	//{{{ setValue() method
	/**
	 * Update the progress value.
	 *
	 * @param value the new value
	 * @since jEdit 4.3pre3
	 */
	public void setValue(final long value)
	{
		SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					progress.setValue(valueSoFar + (int) value);
				}
			});
	} //}}}

	//{{{ setMaximum() method
	/**
	 * This method is unused with the plugin manager.
	 *
	 * @param value the new max value (it will be ignored)
	 * @since jEdit 4.3pre3
	 */
	public void setMaximum(long value) 
	{
	} //}}}

	//{{{ setStatus() method
	/**
	 * This method is unused with the plugin manager.
	 *
	 * @param status the new status (it will be ignored)
	 * @since jEdit 4.3pre3
	 */
	 public void setStatus(String status) 
	 {
		 setTitle(status);
		 progress.setString(status);
	} //}}}

	//{{{ done() method
	public void done()
	{
		try
		{
			if(done == count)
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						dispose();
					}
				});
			}
			else
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						valueSoFar += roster.getOperation(done - 1)
							.getMaximum();
						progress.setValue(valueSoFar);
						done++;
					}
				});
			}
		}
		catch(Exception e)
		{
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Thread thread;

	private final JProgressBar progress;
	private final JButton stop;
	private final int count;
	private int done = 1;

	// progress value as of start of current task
	private int valueSoFar;

	private final Roster roster;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == stop)
			{
				thread.stop();
				dispose();
			}
		}
	} //}}}

	//{{{ WindowHandler class
	class WindowHandler extends WindowAdapter
	{
		boolean done;

		@Override
		public void windowOpened(WindowEvent evt)
		{
			if(done)
				return;

			done = true;
			thread = new RosterThread();
			thread.start();
		}

		@Override
		public void windowClosing(WindowEvent evt)
		{
			thread.stop();
			dispose();
		}
	} //}}}

	//{{{ RosterThread class
	class RosterThread extends Thread
	{
		RosterThread()
		{
			super("Plugin manager thread");
		}

		@Override
		public void run()
		{
			roster.performOperationsInWorkThread(PluginManagerProgress.this);
		}
	} //}}}

	//}}}
}
