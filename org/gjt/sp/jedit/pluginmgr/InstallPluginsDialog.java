/*
 * InstallPluginsDialog.java - Plugin install dialog box
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

class InstallPluginsDialog extends EnhancedDialog
{
	static final int INSTALL = 0;
	static final int UPDATE = 1;

	InstallPluginsDialog(JDialog dialog, Vector model, int mode)
	{
		super(JOptionPane.getFrameForComponent(dialog),
			(mode == INSTALL
			? jEdit.getProperty("install-plugins.title")
			: jEdit.getProperty("update-plugins.title")),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty("install-plugins.caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		content.add(BorderLayout.NORTH,label);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(0,0,12,0));

		plugins = new JCheckBoxList(model);
		plugins.getSelectionModel().addListSelectionListener(new ListHandler());
		JScrollPane scroller = new JScrollPane(plugins);
		Dimension dim = scroller.getPreferredSize();
		dim.height = 120;
		scroller.setPreferredSize(dim);
		panel.add(BorderLayout.CENTER,scroller);

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(new EmptyBorder(6,0,0,0));
		JPanel labelBox = new JPanel(new GridLayout(
			(mode == UPDATE ? 6 : 5),1,0,3));
		labelBox.setBorder(new EmptyBorder(0,0,3,12));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.name"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.author"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.latest-version"),SwingConstants.RIGHT));
		if(mode == UPDATE)
		{
			labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
				+ ".info.installed-version"),SwingConstants.RIGHT));
		}
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.updated"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.description"),SwingConstants.RIGHT));
		panel2.add(BorderLayout.WEST,labelBox);

		JPanel valueBox = new JPanel(new GridLayout(
			(mode == UPDATE ? 6 : 5),1,0,3));
		valueBox.setBorder(new EmptyBorder(0,0,3,0));
		valueBox.add(name = new JLabel());
		valueBox.add(author = new JLabel());
		valueBox.add(latestVersion = new JLabel());
		if(mode == UPDATE)
		{
			valueBox.add(installedVersion = new JLabel());
		}
		valueBox.add(updated = new JLabel());
		valueBox.add(Box.createGlue());
		panel2.add(BorderLayout.CENTER,valueBox);

		JPanel panel3 = new JPanel(new BorderLayout(0,3));
		description = new JTextArea(6,30);
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		panel3.add(BorderLayout.NORTH,new JScrollPane(description));
		if(mode == INSTALL)
		{
			JPanel panel4 = new JPanel(new BorderLayout(0,3));

			ButtonGroup grp = new ButtonGroup();
			installUser = new JRadioButton();
			String settings = jEdit.getSettingsDirectory();
			if(settings == null)
			{
				settings = jEdit.getProperty("install-plugins.none");
				installUser.setEnabled(false);
			}
			else
			{
				settings = MiscUtilities.constructPath(settings,"jars");
				installUser.setEnabled(true);
			}
			String[] args = { settings };
			installUser.setText(jEdit.getProperty("install-plugins.user",args));
			grp.add(installUser);
			panel4.add(BorderLayout.CENTER,installUser);

			installSystem = new JRadioButton();
			String jEditHome = jEdit.getJEditHome();
			if(jEditHome == null)
			{
				jEditHome = jEdit.getProperty("install-plugins.none");
				installSystem.setEnabled(false);
			}
			else
			{
				jEditHome = MiscUtilities.constructPath(jEditHome,"jars");
				installSystem.setEnabled(true);
			}
			args[0] = jEditHome;
			installSystem.setText(jEdit.getProperty("install-plugins.system",args));
			grp.add(installSystem);
			panel4.add(BorderLayout.SOUTH,installSystem);

			if(installUser.isEnabled())
				installUser.setSelected(true);
			else
				installSystem.setSelected(true);

			panel3.add(BorderLayout.CENTER,panel4);
		}

		panel3.add(BorderLayout.SOUTH,downloadSource = new JCheckBox(
			jEdit.getProperty("install-plugins.downloadSource")));
		downloadSource.setSelected(jEdit.getBooleanProperty("install-plugins"
			+ ".downloadSource.value"));

		panel2.add(BorderLayout.SOUTH,panel3);

		panel.add(BorderLayout.SOUTH,panel2);

		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);

		box.add(Box.createGlue());
		install = new JButton(jEdit.getProperty("install-plugins.install"));
		install.setEnabled(false);
		getRootPane().setDefaultButton(install);
		install.addActionListener(new ActionHandler());
		box.add(install);
		box.add(Box.createHorizontalStrut(6));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		box.add(cancel);
		box.add(Box.createHorizontalStrut(6));
		box.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,box);

		pack();
		setLocationRelativeTo(dialog);
		show();
	}

	public void ok()
	{
		jEdit.setBooleanProperty("install-plugins.downloadSource.value",
			downloadSource.isSelected());
		dispose();
	}

	public void cancel()
	{
		cancelled = true;

		dispose();
	}

	void installPlugins(Roster roster)
	{
		if(cancelled)
			return;

		String installDirectory;
		if(installUser == null || installUser.isSelected())
		{
			installDirectory = MiscUtilities.constructPath(
				jEdit.getSettingsDirectory(),"jars");
		}
		else
		{
			installDirectory = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");
		}

		Object[] selected = plugins.getCheckedValues();
		for(int i = 0; i < selected.length; i++)
		{
			PluginList.Plugin plugin = (PluginList.Plugin)selected[i];
			plugin.install(roster,installDirectory,downloadSource.isSelected());
		}
	}

	// private members
	private JCheckBoxList plugins;
	private JLabel name;
	private JLabel author;
	private JLabel latestVersion;
	private JLabel installedVersion;
	private JLabel updated;
	private JTextArea description;
	private JRadioButton installUser;
	private JRadioButton installSystem;
	private JCheckBox downloadSource;

	private JButton install;
	private JButton cancel;

	private boolean cancelled;
	private Thread thread;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == install)
				ok();
			else
				cancel();
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			Object selected = plugins.getSelectedValue();
			if(selected instanceof PluginList.Plugin)
			{
				install.setEnabled(true);

				PluginList.Plugin plugin = (PluginList.Plugin)selected;
				PluginList.Branch branch = plugin.getCompatibleBranch();
				name.setText(plugin.name);
				author.setText(plugin.author);
				if(branch.obsolete)
					latestVersion.setText(jEdit.getProperty(
						"install-plugins.info.obsolete"));
				else
					latestVersion.setText(branch.version);
				if(installedVersion != null)
					installedVersion.setText(plugin.installedVersion);
				updated.setText(branch.date);

				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < branch.deps.size(); i++)
				{
					PluginList.Dependency dep = (PluginList.Dependency)
						branch.deps.elementAt(i);
					if(dep.what.equals("plugin")
						&& !dep.isSatisfied())
					{
						if(buf.length() != 0)
							buf.append(", ");

						buf.append(dep.plugin);
					}
				}

				description.setText(plugin.description
					+ (buf.length() == 0 ? ""
					: jEdit.getProperty("install-plugins.info"
					+ ".also-install") + buf.toString()
					+ (branch.obsolete ? jEdit.getProperty(
					"install-plugins.info.obsolete-text") : "")));
			}
			else
			{
				install.setEnabled(false);

				name.setText(null);
				author.setText(null);
				latestVersion.setText(null);
				if(installedVersion != null)
					installedVersion.setText(null);
				updated.setText(null);
				description.setText(null);
			}
		}
	}
}
