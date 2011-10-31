/*
 * AppearanceOptionPane.java - Appearance options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
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

//{{{ Imports
import javax.swing.*;

import java.awt.Font;
import java.awt.event.*;
import java.io.*;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.gui.NumericTextField;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;
//}}}

public class AppearanceOptionPane extends AbstractOptionPane
{
	/**
	 * List of icon themes that are supported in jEdit core.
	 * Possible values of the jedit property 'icon-theme'
	 */
	public static final String[] builtInIconThemes = {"tango", "old"};
	
	//{{{ AppearanceOptionPane constructor
	public AppearanceOptionPane()
	{
		super("appearance");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		/* Look and feel */
		addComponent(new JLabel(jEdit.getProperty("options.appearance.lf.note")));

		lfs = UIManager.getInstalledLookAndFeels();
		String[] names = new String[lfs.length];
		String lf = UIManager.getLookAndFeel().getClass().getName();
		int index = 0;
		for(int i = 0; i < names.length; i++)
		{
			names[i] = lfs[i].getName();
			if(lf.equals(lfs[i].getClassName()))
				index = i;
		}

		lookAndFeel = new JComboBox(names);
		lookAndFeel.setSelectedIndex(index);
		lookAndFeel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				updateEnabled();
			}
		});

		
		addComponent(jEdit.getProperty("options.appearance.lf"),
			lookAndFeel);
		addDockingFrameworkChooser();

		/* Icon Theme */
		String[] themes = IconTheme.builtInNames();
		iconThemes = new JComboBox(themes);
		addComponent(jEdit.getProperty("options.appearance.iconTheme"), iconThemes);
		oldTheme = IconTheme.get();
		for (int i=0; i<themes.length; ++i)
		{
			if (themes[i].equals(oldTheme))
			{
				iconThemes.setSelectedIndex(i);
				break;
			}
		}
		
		/* Primary Metal L&F font */
		Font pf = jEdit.getFontProperty("metal.primary.font");
		primaryFont = new FontSelector(pf);
		addComponent(jEdit.getProperty("options.appearance.primaryFont"),
			primaryFont);

		/* Secondary Metal L&F font */
		secondaryFont = new FontSelector(jEdit.getFontProperty(
			"metal.secondary.font"));
		addComponent(jEdit.getProperty("options.appearance.secondaryFont"),
			secondaryFont);
		
		/* HelpViewer font */
		helpViewerFont = new FontSelector(jEdit.getFontProperty(
			"helpviewer.font", pf));
		addComponent(jEdit.getProperty("options.appearance.helpViewerFont"),
			helpViewerFont);
		
		/*
		antiAliasExtras = new JComboBox(AntiAlias.comboChoices);
		antiAliasExtras.setSelectedIndex(AntiAlias.appearance().val());
		antiAliasExtras.setToolTipText(jEdit.getProperty("options.textarea.antiAlias.tooltip"));
		addComponent(jEdit.getProperty("options.appearance.fonts.antialias"), antiAliasExtras);
		*/
		updateEnabled();

		/* History count */
		history = new NumericTextField(jEdit.getProperty("history"), true);
		addComponent(jEdit.getProperty("options.appearance.history"),history);

		/* Menu spillover count */
		menuSpillover = new NumericTextField(jEdit.getProperty("menu.spillover"), true);
		addComponent(jEdit.getProperty("options.appearance.menuSpillover"),menuSpillover);

		continuousLayout = new JCheckBox(jEdit.getProperty(
			"options.appearance.continuousLayout.label"));
		continuousLayout.setSelected(jEdit.getBooleanProperty("appearance.continuousLayout"));
		addComponent(continuousLayout);

		systemTrayIcon = new JCheckBox(jEdit.getProperty(
					"options.general.systrayicon", "Show the systray icon"));
		systemTrayIcon.setSelected(jEdit.getBooleanProperty("systrayicon", true));
		addComponent(systemTrayIcon);
		
		if (OperatingSystem.isMacOS())
		{
			String settingsDirectory = jEdit.getSettingsDirectory();
			useQuartz = new JCheckBox(jEdit.getProperty("options.appearance.useQuartz"));
			useQuartz.setSelected(!new File(settingsDirectory, "noquartz").exists());
			addComponent(useQuartz);
		}

		addSeparator("options.appearance.startup.label");

		/* Show splash screen */
		showSplash = new JCheckBox(jEdit.getProperty(
			"options.appearance.showSplash"));
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			showSplash.setSelected(true);
		else
			showSplash.setSelected(!new File(settingsDirectory,"nosplash").exists());
		addComponent(showSplash);

		/* Show tip of the day */
		showTips = new JCheckBox(jEdit.getProperty(
			"options.appearance.showTips"));
		showTips.setSelected(jEdit.getBooleanProperty("tip.show"));
		addComponent(showTips);

		addSeparator("options.appearance.experimental.label");
		addComponent(GUIUtilities.createMultilineLabel(
			jEdit.getProperty("options.appearance.experimental.caption")));

		/* Use jEdit colors in all text components */
		textColors = new JCheckBox(jEdit.getProperty(
			"options.appearance.textColors"));
		textColors.setSelected(jEdit.getBooleanProperty("textColors"));
		addComponent(textColors);

		/* Decorate frames with look and feel (JDK 1.4 only) */
		decorateFrames = new JCheckBox(jEdit.getProperty(
			"options.appearance.decorateFrames"));
		decorateFrames.setSelected(jEdit.getBooleanProperty("decorate.frames"));
		addComponent(decorateFrames);

		/* Decorate dialogs with look and feel (JDK 1.4 only) */
		decorateDialogs = new JCheckBox(jEdit.getProperty(
			"options.appearance.decorateDialogs"));
		decorateDialogs.setSelected(jEdit.getBooleanProperty("decorate.dialogs"));
		addComponent(decorateDialogs);
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		String lf = lfs[lookAndFeel.getSelectedIndex()].getClassName();
		jEdit.setProperty("lookAndFeel",lf);
		jEdit.setFontProperty("metal.primary.font",primaryFont.getFont());
		jEdit.setFontProperty("metal.secondary.font",secondaryFont.getFont());
		jEdit.setFontProperty("helpviewer.font", helpViewerFont.getFont());
		jEdit.setProperty("history",history.getText());
		jEdit.setProperty("menu.spillover",menuSpillover.getText());
		jEdit.setBooleanProperty("tip.show",showTips.isSelected());
		jEdit.setBooleanProperty("appearance.continuousLayout",continuousLayout.isSelected());
		jEdit.setBooleanProperty("systrayicon", systemTrayIcon.isSelected());
		IconTheme.set(iconThemes.getSelectedItem().toString());

		jEdit.setProperty(View.VIEW_DOCKING_FRAMEWORK_PROPERTY,
			(String) dockingFramework.getSelectedItem());

		/* AntiAlias nv = AntiAlias.appearance();
		 int idx = antiAliasExtras.getSelectedIndex();
		nv.set(idx);
		primaryFont.setAntiAliasEnabled(idx > 0);
		secondaryFont.setAntiAliasEnabled(idx > 0);
		primaryFont.repaint();
		secondaryFont.repaint(); */

		// These are handled a little differently from other jEdit settings
		// as these flags need to be known very early in the
		// startup sequence, before the user properties have been loaded
		setFileFlag("nosplash", !showSplash.isSelected());
		if (OperatingSystem.isMacOS())
		{
			setFileFlag("noquartz", !useQuartz.isSelected());
		}
		
		jEdit.setBooleanProperty("textColors",textColors.isSelected());
		jEdit.setBooleanProperty("decorate.frames",decorateFrames.isSelected());
		jEdit.setBooleanProperty("decorate.dialogs",decorateDialogs.isSelected());
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private String oldTheme;
	private UIManager.LookAndFeelInfo[] lfs;
	private JComboBox lookAndFeel;
	private FontSelector primaryFont;
	private FontSelector secondaryFont;
	private FontSelector helpViewerFont;
	private JComboBox dockingFramework;
	private JTextField history;
	private JTextField menuSpillover;
	private JCheckBox showTips;
	private JCheckBox continuousLayout;
	private JCheckBox showSplash;
	private JCheckBox textColors;
	private JCheckBox decorateFrames;
	private JCheckBox decorateDialogs;
	private JComboBox antiAliasExtras;
	private JComboBox iconThemes;
	private JCheckBox systemTrayIcon;
	private JCheckBox useQuartz;
	//}}}

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		String className = lfs[lookAndFeel.getSelectedIndex()]
			.getClassName();

		if(className.equals("javax.swing.plaf.metal.MetalLookAndFeel")
			|| className.equals("com.incors.plaf.kunststoff.KunststoffLookAndFeel"))
		{
			primaryFont.setEnabled(true);
			secondaryFont.setEnabled(true);
		}
		else
		{
			primaryFont.setEnabled(false);
			secondaryFont.setEnabled(false);
		}
	} //}}}
	private void addDockingFrameworkChooser()
	{	
		String [] frameworks =
			ServiceManager.getServiceNames(View.DOCKING_FRAMEWORK_PROVIDER_SERVICE);
		dockingFramework = new JComboBox(frameworks);
		String framework = View.getDockingFrameworkName();
		for (int i = 0; i < frameworks.length; i++)
		{
			if (frameworks[i].equals(framework))
			{
				dockingFramework.setSelectedIndex(i);
				break;
			}
		}
		addComponent(new JLabel(jEdit.getProperty("options.appearance.selectFramework.label")), dockingFramework);
	}

	//}}}
	
	//{{{ setFileFlag() method
	private void setFileFlag(String fileName, boolean present)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			File file = new File(settingsDirectory, fileName);
			if (!present)
			{
				file.delete();
			}
			else
			{
				FileOutputStream out = null;
				try
				{
					out = new FileOutputStream(file);
					out.write('\n');
					out.close();
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,this,io);
				}
				finally
				{
					IOUtilities.closeQuietly(out);
				}
			}
		}
	} //}}}
}
