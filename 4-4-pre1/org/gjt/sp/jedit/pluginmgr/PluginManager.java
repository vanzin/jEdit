/*
 * PluginManager.java - Plugin manager window
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
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
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.options.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * @version $Id$
 */
public class PluginManager extends JFrame
{
	
	//{{{ getInstance() method
	/**
	 * Returns the currently visible plugin manager window, or null.
	 * @since jEdit 4.2pre2
	 */
	public static PluginManager getInstance()
	{
		return instance;
	} //}}}

	//{{{ dispose() method
	@Override
	public void dispose()
	{
		instance = null;
		EditBus.removeFromBus(this);
		EditBus.removeFromBus(installer);
		super.dispose();
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged message)
	{
		if (shouldUpdatePluginList())
		{
			pluginList = null;
			updatePluginList();
		}
	} //}}}

	//{{{ handlePluginUpdate() method
	@EBHandler
	public void handlePluginUpdate(PluginUpdate msg)
	{
		if(!queuedUpdate)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					queuedUpdate = false;
					manager.update();
				}
			});
			queuedUpdate = true;
		}
	} //}}}

	//{{{ showPluginManager() method
	public static void showPluginManager(Frame parent)
	{
		if (instance == null)
			instance = new PluginManager(parent);
		else
			instance.toFront();
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getPluginList() method
	PluginList getPluginList()
	{
		return pluginList;
	} //}}}

	//{{{ Private members
	private static PluginManager instance;

	//{{{ Instance variables
	private JTabbedPane tabPane;
	private JButton done;
	private JButton mgrOptions;
	private JButton pluginOptions;
	private InstallPanel installer;
	private InstallPanel updater;
	private ManagePanel manager;
	private PluginList pluginList;
	private boolean queuedUpdate;
	private boolean downloadingPluginList;
	private final Frame parent;
	//}}}

	public static final String PROPERTY_PLUGINSET = "plugin-manager.pluginset.path";

	//{{{ PluginManager constructor
	private PluginManager(Frame parent)
	{
		super(jEdit.getProperty("plugin-manager.title"));
		this.parent = parent;
		init();
	} //}}}

	//{{{ init() method
	private void init()
	{
		EditBus.addToBus(this);
		

		/* Setup panes */
		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		tabPane = new JTabbedPane();
		tabPane.addTab(jEdit.getProperty("manage-plugins.title"),
			manager = new ManagePanel(this));
		tabPane.addTab(jEdit.getProperty("update-plugins.title"),
			updater = new InstallPanel(this,true));
		tabPane.addTab(jEdit.getProperty("install-plugins.title"),
			installer = new InstallPanel(this,false));
		EditBus.addToBus(installer);
		content.add(BorderLayout.CENTER,tabPane);

		tabPane.addChangeListener(new ListUpdater());

		/* Create the buttons */
		Box buttons = new Box(BoxLayout.X_AXIS);

		ActionListener al = new ActionHandler();
		mgrOptions = new JButton(jEdit.getProperty("plugin-manager.mgr-options"));
		mgrOptions.addActionListener(al);
		pluginOptions = new JButton(jEdit.getProperty("plugin-manager.plugin-options"));
		pluginOptions.addActionListener(al);
		done = new JButton(jEdit.getProperty("plugin-manager.done"));
		done.addActionListener(al);

		buttons.add(Box.createGlue());
		buttons.add(mgrOptions);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(pluginOptions);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(done);
		buttons.add(Box.createGlue());

		getRootPane().setDefaultButton(done);

		content.add(BorderLayout.SOUTH,buttons);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setIconImage(GUIUtilities.getPluginIcon());

		pack();
		GUIUtilities.loadGeometry(this, parent, "plugin-manager");
		GUIUtilities.addSizeSaver(this, parent, "plugin-manager");
		setVisible(true);
	} //}}}

	//{{{ shouldUpdatePluginList()
	/**
	* Check if the plugin list should be updated.
	* It will return <code>true</code> if the pluginList is <code>null</code>
	* or if the mirror id of the current plugin list is not the current preffered mirror id
	* and will return always false if the plugin list is currently downloading
	*
	* @return true if the plugin list should be updated
	*/
	private boolean shouldUpdatePluginList()
	{
		return (pluginList == null ||
			!pluginList.getMirrorId().equals(jEdit.getProperty("plugin-manager.mirror.id"))) &&
			!downloadingPluginList;
	} //}}}

	//{{{ updatePluginList() method
	private void updatePluginList()
	{
		if(jEdit.getSettingsDirectory() == null
			&& jEdit.getJEditHome() == null)
		{
			GUIUtilities.error(this,"no-settings",null);
			return;
		}
		if (!shouldUpdatePluginList())
		{
			return;
		}

		ThreadUtilities.runInBackground(new Task()
		{
			@Override
			public void _run()
			{
				try
				{
					downloadingPluginList = true;
					setStatus(jEdit.getProperty(
						"plugin-manager.list-download-connect"));
					pluginList = new PluginList(this);
				}
				finally
				{
					downloadingPluginList = false;
				}
				ThreadUtilities.runInDispatchThread(new Runnable()
				{
					public void run()
					{
						pluginListUpdated();
					}
				});
			}
		});
	} //}}}

	//{{{ processKeyEvent() method
	private void pluginListUpdated()
	{
		Component selected = tabPane.getSelectedComponent();
		if(selected == installer || selected == updater)
		{
			installer.updateModel();
			updater.updateModel();
		}
	} //}}}

	//{{{ processKeyEvent() method
	public void processKeyEvents(KeyEvent ke)
	{
		if ((ke.getID() == KeyEvent.KEY_PRESSED) &&
		    (ke.getKeyCode() == KeyEvent.VK_ESCAPE))
		{
			cancel();
			ke.consume();
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == done)
				ok();
			else if (source == mgrOptions)
				new GlobalOptions(PluginManager.this,"plugin-manager");
			else if (source == pluginOptions)
				new PluginOptions(PluginManager.this);
		}
	} //}}}

	//{{{ ListUpdater class
	class ListUpdater implements ChangeListener
	{
		public void stateChanged(ChangeEvent e)
		{
			Component selected = tabPane.getSelectedComponent();
			if(selected == installer || selected == updater)
			{
				updatePluginList();
			}
			else if(selected == manager)
				manager.update();
		}
	} //}}}

	//}}}
}
