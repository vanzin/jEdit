/*
 * AppearanceOptionPane.java - Appearance options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.*;
//}}}

public class AppearanceOptionPane extends AbstractOptionPane
{
	//{{{ AppearanceOptionPane constructor
	public AppearanceOptionPane()
	{
		super("appearance");
	} //}}}

	//{{{ _init() method
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
			public void actionPerformed(ActionEvent evt)
			{
				updateEnabled();
			}
		});

		addComponent(jEdit.getProperty("options.appearance.lf"),
			lookAndFeel);

		/* Primary Metal L&F font */
		primaryFont = new FontSelector(jEdit.getFontProperty(
			"metal.primary.font"));
		addComponent(jEdit.getProperty("options.appearance.primaryFont"),
			primaryFont);

		/* Secondary Metal L&F font */
		secondaryFont = new FontSelector(jEdit.getFontProperty(
			"metal.secondary.font"));
		addComponent(jEdit.getProperty("options.appearance.secondaryFont"),
			secondaryFont);

		updateEnabled();

		/* Menu spillover count */
		menuSpillover = new JTextField(jEdit.getProperty("menu.spillover"));
		addComponent(jEdit.getProperty("options.appearance.menuSpillover"),menuSpillover);

		/* View dock layout */
		addSeparator("options.appearance.viewLayout");

		layoutIcon1 = GUIUtilities.loadIcon("dock_layout1.png");
		layoutIcon2 = GUIUtilities.loadIcon("dock_layout2.png");
		layoutIcon3 = GUIUtilities.loadIcon("dock_layout3.png");
		layoutIcon4 = GUIUtilities.loadIcon("dock_layout4.png");

		JPanel layoutPanel = new JPanel(new BorderLayout(12,12));

		if(jEdit.getBooleanProperty("view.docking.alternateLayout"))
		{
			layout = new JLabel(jEdit.getBooleanProperty(
				"view.toolbar.alternateLayout")
				? layoutIcon4 : layoutIcon2);
		}
		else
		{
			layout = new JLabel(jEdit.getBooleanProperty(
				"view.toolbar.alternateLayout")
				? layoutIcon3 : layoutIcon1);
		}

		layoutPanel.add(BorderLayout.WEST,layout);

		Box buttons = new Box(BoxLayout.Y_AXIS);
		buttons.add(Box.createGlue());
		buttons.add(alternateDockingLayout = new JButton(jEdit.getProperty(
			"options.appearance.alternateDockingLayout")));
		alternateDockingLayout.addActionListener(new ActionHandler());
		buttons.add(Box.createVerticalStrut(12));
		buttons.add(alternateToolBarLayout = new JButton(jEdit.getProperty(
			"options.appearance.alternateToolBarLayout")));
		alternateToolBarLayout.addActionListener(new ActionHandler());
		buttons.add(Box.createGlue());
		layoutPanel.add(BorderLayout.CENTER,buttons);

		addComponent(layoutPanel);

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

		/* Decorate dialogs with look and feel (JDK 1.4 only) */
		decorateDialogs = new JCheckBox(jEdit.getProperty(
			"options.appearance.decorateDialogs"));
		decorateDialogs.setSelected(jEdit.getBooleanProperty("decorate.dialogs"));

		if(OperatingSystem.hasJava14())
		{
			addComponent(decorateFrames);
			addComponent(decorateDialogs);
		}
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		String lf = lfs[lookAndFeel.getSelectedIndex()].getClassName();
		jEdit.setProperty("lookAndFeel",lf);
		jEdit.setFontProperty("metal.primary.font",primaryFont.getFont());
		jEdit.setFontProperty("metal.secondary.font",secondaryFont.getFont());
		jEdit.setProperty("menu.spillover",menuSpillover.getText());
		jEdit.setBooleanProperty("view.docking.alternateLayout",
			layout.getIcon() == layoutIcon2
			|| layout.getIcon() == layoutIcon4);
		jEdit.setBooleanProperty("view.toolbar.alternateLayout",
			layout.getIcon() == layoutIcon3
			|| layout.getIcon() == layoutIcon4);
		jEdit.setBooleanProperty("textColors",textColors.isSelected());
		jEdit.setBooleanProperty("decorate.frames",decorateFrames.isSelected());
		jEdit.setBooleanProperty("decorate.dialogs",decorateDialogs.isSelected());
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private UIManager.LookAndFeelInfo[] lfs;
	private JComboBox lookAndFeel;
	private FontSelector primaryFont;
	private FontSelector secondaryFont;
	private JTextField menuSpillover;
	private JLabel layout;
	private Icon layoutIcon1, layoutIcon2, layoutIcon3, layoutIcon4;
	private JButton alternateDockingLayout, alternateToolBarLayout;
	private JCheckBox textColors;
	private JCheckBox decorateFrames;
	private JCheckBox decorateDialogs;
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

	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == alternateDockingLayout)
			{
				if(layout.getIcon() == layoutIcon1)
					layout.setIcon(layoutIcon2);
				else if(layout.getIcon() == layoutIcon2)
					layout.setIcon(layoutIcon1);
				else if(layout.getIcon() == layoutIcon3)
					layout.setIcon(layoutIcon4);
				else if(layout.getIcon() == layoutIcon4)
					layout.setIcon(layoutIcon3);
			}
			else if(evt.getSource() == alternateToolBarLayout)
			{
				if(layout.getIcon() == layoutIcon1)
					layout.setIcon(layoutIcon3);
				else if(layout.getIcon() == layoutIcon3)
					layout.setIcon(layoutIcon1);
				else if(layout.getIcon() == layoutIcon2)
					layout.setIcon(layoutIcon4);
				else if(layout.getIcon() == layoutIcon4)
					layout.setIcon(layoutIcon2);
			}
		}
	} //}}}
}
