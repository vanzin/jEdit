/*
 * StatusBarOptionPane.java - Status Bar options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 * Copyright (C) 2002 Kenrick Drew
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
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.*;
//}}}

public class StatusBarOptionPane extends AbstractOptionPane
{
	//{{{ StatusBarOptionPane constructor
	public StatusBarOptionPane()
	{
		super("status");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		/* Status bar visible */
		statusVisible = new JCheckBox(jEdit.getProperty(
			"options.status.visible"));
		statusVisible.setSelected(jEdit.getBooleanProperty(
			"view.status.visible"));
		statusVisible.addActionListener(new ActionHandler());
		addComponent(statusVisible);

		/* Caret status */
		showCaretStatus = new JCheckBox(jEdit.getProperty(
			"options.status.show-caret-status"));
		showCaretStatus.setSelected(jEdit.getBooleanProperty(
			"view.status.show-caret-status"));
		addComponent(showCaretStatus);

		/* Edit mode */
		showEditMode = new JCheckBox(jEdit.getProperty(
			"options.status.show-edit-mode"));
		showEditMode.setSelected(jEdit.getBooleanProperty(
			"view.status.show-edit-mode"));
		addComponent(showEditMode);

		/* Fold mode */
		showFoldMode = new JCheckBox(jEdit.getProperty(
			"options.status.show-fold-mode"));
		showFoldMode.setSelected(jEdit.getBooleanProperty(
			"view.status.show-fold-mode"));
		addComponent(showFoldMode);

		/* Encoding */
		showEncoding = new JCheckBox(jEdit.getProperty(
			"options.status.show-encoding"));
		showEncoding.setSelected(jEdit.getBooleanProperty(
			"view.status.show-encoding"));
		addComponent(showEncoding);

		/* Wrap */
		showWrap = new JCheckBox(jEdit.getProperty(
			"options.status.show-wrap"));
		showWrap.setSelected(jEdit.getBooleanProperty(
			"view.status.show-wrap"));
		addComponent(showWrap);

		/* Multi select */
		showMultiSelect = new JCheckBox(jEdit.getProperty(
			"options.status.show-multi-select"));
		showMultiSelect.setSelected(jEdit.getBooleanProperty(
			"view.status.show-multi-select"));
		addComponent(showMultiSelect);

		/* Rect select */
		showRectSelect = new JCheckBox(jEdit.getProperty(
			"options.status.show-rect-select"));
		showRectSelect.setSelected(jEdit.getBooleanProperty(
			"view.status.show-rect-select"));
		addComponent(showRectSelect);

		/* Overwrite */
		showOverwrite = new JCheckBox(jEdit.getProperty(
			"options.status.show-overwrite"));
		showOverwrite.setSelected(jEdit.getBooleanProperty(
			"view.status.show-overwrite"));
		addComponent(showOverwrite);

		/* Line seperator */
		showLineSeperator = new JCheckBox(jEdit.getProperty(
			"options.status.show-line-seperator"));
		showLineSeperator.setSelected(jEdit.getBooleanProperty(
			"view.status.show-line-seperator"));
		addComponent(showLineSeperator);

		/* Memory status */
		showMemory = new JCheckBox(jEdit.getProperty(
			"options.status.show-memory"));
		showMemory.setSelected(jEdit.getBooleanProperty(
			"view.status.show-memory"));
		showMemory.addActionListener(new ActionHandler());
		addComponent(showMemory);

		/* Memory foreground color */
		addComponent(jEdit.getProperty("options.status.memory.foreground"),
			memForegroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.memory.foreground")),
			GridBagConstraints.VERTICAL);

		/* Memory background color */
		addComponent(jEdit.getProperty("options.status.memory.background"),
			memBackgroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.memory.background")),
			GridBagConstraints.VERTICAL);

		updateEnabled();
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		jEdit.setBooleanProperty("view.status.visible",
			statusVisible.isSelected());
		jEdit.setBooleanProperty("view.status.show-caret-status",
			showCaretStatus.isSelected());
		jEdit.setBooleanProperty("view.status.show-edit-mode",
			showEditMode.isSelected());
		jEdit.setBooleanProperty("view.status.show-fold-mode",
			showFoldMode.isSelected());
		jEdit.setBooleanProperty("view.status.show-encoding",
			showEncoding.isSelected());
		jEdit.setBooleanProperty("view.status.show-wrap",
			showWrap.isSelected());
		jEdit.setBooleanProperty("view.status.show-multi-select",
			showMultiSelect.isSelected());
		jEdit.setBooleanProperty("view.status.show-rect-select",
			showRectSelect.isSelected());
		jEdit.setBooleanProperty("view.status.show-overwrite",
			showOverwrite.isSelected());
		jEdit.setBooleanProperty("view.status.show-line-seperator",
			showLineSeperator.isSelected());
		jEdit.setBooleanProperty("view.status.show-memory",
			showMemory.isSelected());
		jEdit.setColorProperty("view.status.memory.foreground",memForegroundColor
			.getSelectedColor());
		jEdit.setColorProperty("view.status.memory.background",memBackgroundColor
			.getSelectedColor());
	} //}}}

	//{{{ Private members
	private JCheckBox statusVisible;
	private JCheckBox showCaretStatus;
	private JCheckBox showEditMode;
	private JCheckBox showFoldMode;
	private JCheckBox showEncoding;
	private JCheckBox showWrap;
	private JCheckBox showMultiSelect;
	private JCheckBox showRectSelect;
	private JCheckBox showOverwrite;
	private JCheckBox showLineSeperator;
	private JCheckBox showMemory;
	private ColorWellButton memForegroundColor;
	private ColorWellButton memBackgroundColor;

	private void updateEnabled()
	{
		boolean enabled = statusVisible.isSelected();
		showCaretStatus.setEnabled(enabled);
		showEditMode.setEnabled(enabled);
		showFoldMode.setEnabled(enabled);
		showEncoding.setEnabled(enabled);
		showWrap.setEnabled(enabled);
		showMultiSelect.setEnabled(enabled);
		showRectSelect.setEnabled(enabled);
		showOverwrite.setEnabled(enabled);
		showLineSeperator.setEnabled(enabled);
		showMemory.setEnabled(enabled);
		memForegroundColor.setEnabled(enabled && showMemory.isSelected());
		memBackgroundColor.setEnabled(enabled && showMemory.isSelected());
	}
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			updateEnabled();
		}
	} //}}}
}
