/*
 * PluginManagerOptionPane.java - Plugin options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Kris Kopicki
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

package org.gjt.sp.jedit.options;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.pluginmgr.*;
import org.gjt.sp.util.WorkRequest;

class PluginManagerOptionPane extends AbstractOptionPane
{
	//{{{ Constructor
	public PluginManagerOptionPane()
	{
		super("plugin-manager");
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JLabel locationLabel;
	private JLabel mirrorLabel;

	private ButtonGroup locGrp;
	private JRadioButton settingsDir;
	private JRadioButton appDir;
	private JCheckBox downloadSource;

	private MirrorModel miraModel;
	private JList miraList;
	//}}}

	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());

		locationLabel = new JLabel(jEdit.getProperty(
			"options.plugin-manager.location"));
		mirrorLabel = new JLabel(jEdit.getProperty(
			"options.plugin-manager.mirror"));
		settingsDir = new JRadioButton(jEdit.getProperty(
			"options.plugin-manager.settings-dir"));
		appDir = new JRadioButton(jEdit.getProperty(
			"options.plugin-manager.app-dir"));
		settingsDir.setToolTipText(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"jars"));
		appDir.setToolTipText(MiscUtilities.constructPath(
			jEdit.getJEditHome(),"jars"));

		// Start downloading the list nice and early
		miraList = new JList(miraModel = new MirrorModel());
		miraList.setSelectionModel(new SingleSelectionModel());
		miraList.setSelectedIndex(0);

		/* Download mirror */
		add(BorderLayout.NORTH,mirrorLabel);
		add(BorderLayout.CENTER,new JScrollPane(miraList));

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.Y_AXIS));

		buttonPanel.add(Box.createVerticalStrut(10));

		/* Download source */
		downloadSource = new JCheckBox(jEdit.getProperty(
			"options.plugin-manager.downloadSource"));
		downloadSource.setSelected(jEdit.getBooleanProperty("plugin-manager.downloadSource"));
		buttonPanel.add(downloadSource);

		buttonPanel.add(Box.createVerticalStrut(10));

		/* Install location */
		locGrp = new ButtonGroup();
		locGrp.add(settingsDir);
		locGrp.add(appDir);
		JPanel locPanel = new JPanel();
		locPanel.setBorder(new EmptyBorder(3,12,0,0));
		locPanel.setLayout(new BoxLayout(locPanel,BoxLayout.Y_AXIS));
		locPanel.add(settingsDir);
		locPanel.add(Box.createVerticalStrut(3));
		locPanel.add(appDir);
		buttonPanel.add(locationLabel);
		buttonPanel.add(locPanel);

		buttonPanel.add(Box.createGlue());
		add(BorderLayout.SOUTH,buttonPanel);

		if (jEdit.getBooleanProperty("plugin-manager.installUser"))
			settingsDir.setSelected(true);
		else
			appDir.setSelected(true);
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		jEdit.setBooleanProperty("plugin-manager.installUser",settingsDir.isSelected());
		jEdit.setBooleanProperty("plugin-manager.downloadSource",downloadSource.isSelected());

		if(miraList.getSelectedIndex() != -1)
		{
			String currentMirror = miraModel.getID(miraList.getSelectedIndex());
			String previousMirror = jEdit.getProperty("plugin-manager.mirror.id");

			if (!previousMirror.equals(currentMirror))
			{
				jEdit.setProperty("plugin-manager.mirror.id",currentMirror);
				// Insert code to update the plugin managers list here later
			}
		}
	} //}}}

	//}}}

	//{{{ MirrorModel class
	class MirrorModel extends AbstractListModel
	{
		private ArrayList mirrors;

		public MirrorModel()
		{
			super();
			mirrors = new ArrayList();
			VFSManager.runInWorkThread(new DownloadMirrorsThread());
		}

		public String getID(int index)
		{
			return ((MirrorList.Mirror)mirrors.get(index)).id;
		}

		public int getSize()
		{
			return mirrors.size();
		}

		public Object getElementAt(int index)
		{
			// Default will always be at 0
			if (index == 0)
				return jEdit.getProperty("options.plugin-manager.none");
			MirrorList.Mirror mirror = (MirrorList.Mirror)mirrors.get(index);
			return new String(mirror.continent+": "+mirror.description+" ("+mirror.location+")");
		}

		public void addToList(ArrayList more)
		{
			int size1 = getSize();
			int size2 = more.size();
			mirrors.addAll(more);
			if (size2 > 0)
				fireIntervalAdded(this,size1, size1 + size2 - 1);
		}
	} //}}}

	//{{{ SingleSelectionModel class
	class SingleSelectionModel extends DefaultListSelectionModel
	{
		public SingleSelectionModel()
		{
			super();
			setSelectionMode(SINGLE_SELECTION);
		}

		public void removeSelectionInterval(int index0, int index1) {}
	} //}}}

	//{{{ DownloadMirrorsThread class
	class DownloadMirrorsThread extends WorkRequest
	{
		public void run()
		{
			setStatus(jEdit.getProperty("options.plugin-manager.workthread"));
			setProgressMaximum(1);
			setProgressValue(0);

			ArrayList mirrors = new ArrayList();
			try
			{
				mirrors = new MirrorList().mirrors;
			}
			catch (Exception ex)
			{
			}
			miraModel.addToList(mirrors);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					String id = jEdit.getProperty("plugin-manager.mirror.id");
					int size = miraModel.getSize();
					for (int i=0; i < size; i++)
					{
						if (size == 1 || miraModel.getID(i).equals(id))
						{
							miraList.setSelectedIndex(i);
							break;
						}
					}
				}
			});

			setProgressValue(1);
		}
	} //}}}
}
