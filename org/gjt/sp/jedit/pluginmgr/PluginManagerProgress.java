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
//}}}

public class PluginManagerProgress extends JDialog
{
	//{{{ PluginManagerProgress constructor
	public PluginManagerProgress(JDialog dialog, String type, Roster roster)
	{
		super(JOptionPane.getFrameForComponent(dialog),
			jEdit.getProperty("plugin-manager.progress."
			+ type + "-task"),true);

		this.dialog = dialog;
		this.roster = roster;
		this.type = type;

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		globalProgress = new JProgressBar();
		globalProgress.setStringPainted(true);
		globalProgress.setString(jEdit.getProperty("plugin-manager.progress."
			+ type + "-task"));

		count = roster.getOperationCount();

		globalProgress.setMaximum(count);
		content.add(BorderLayout.NORTH,globalProgress);

		localProgress = new JProgressBar();
		localProgress.setStringPainted(true);
		content.add(BorderLayout.CENTER,localProgress);

		stop = new JButton(jEdit.getProperty("plugin-manager.progress.stop"));
		stop.addActionListener(new ActionHandler());
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.add(Box.createGlue());
		panel.add(stop);
		panel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,panel);

		addWindowListener(new WindowHandler());

		pack();

		Dimension screen = getToolkit().getScreenSize();
		Dimension size = getSize();
		size.width = Math.max(size.width,500);
		setSize(size);
		setLocationRelativeTo(dialog);

		show();
	} //}}}

	//{{{ removing() method
	public void removing(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.removing",args));
		stop.setEnabled(true);
	} //}}}

	//{{{ downloading() method
	public void downloading(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.downloading",args));
		stop.setEnabled(true);
	} //}}}

	//{{{ installing() method
	public void installing(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.installing",args));
		stop.setEnabled(false);
	} //}}}

	//{{{ setMaximum() method
	public void setMaximum(final int total)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				localProgress.setMaximum(total);
			}
		});
	} //}}}

	//{{{ setValue() method
	public void setValue(final int value)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				localProgress.setValue(value);
			}
		});
	} //}}}

	//{{{ done() method
	public void done(final boolean ok)
	{
		this.ok |= ok;

		try
		{
			if(!ok || done == count)
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						dispose();
						if(ok)
						{
							GUIUtilities.message(dialog,
								"plugin-manager." + type
								+ "-done",null);
						}
						else
						{
							// user will see an error in any case

							//GUIUtilities.message(PluginManagerProgress.this,
							//	"plugin-manager.failed",null);
						}
					}
				});
			}
			else
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						globalProgress.setValue(done++);
						localProgress.setValue(0);
					}
				});
			}
		}
		catch(Exception e)
		{
		}
	} //}}}

	//{{{ isOK() method
	public boolean isOK()
	{
		return ok;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JDialog dialog;

	private Thread thread;

	private String type;

	private JProgressBar globalProgress, localProgress;
	private JButton stop;
	private int count;
	private int done = 1;

	private boolean ok;

	private Roster roster;
	//}}}

	//{{{ showMessage() method
	private void showMessage(final String msg)
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					localProgress.setString(msg);
				}
			});
		}
		catch(Exception e)
		{
		}

		Thread.yield();
	} //}}}

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

		public void windowOpened(WindowEvent evt)
		{
			if(done)
				return;

			done = true;
			thread = new RosterThread();
			thread.start();
		}

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

		public void run()
		{
			roster.performOperations(PluginManagerProgress.this);
		}
	} //}}}
}
