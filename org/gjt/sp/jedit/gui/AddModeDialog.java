/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2015 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.util.GenericGUIUtilities;

import java.awt.*;
import javax.swing.*;

/** Add Mode dialog.
 * @author Dale Anson
 */
public class AddModeDialog extends EnhancedDialog
{
	private final JTextField modeName;
	private final JTextField modeFile;
	private final JTextField filenameGlob;
	private final JTextField firstLineGlob;
	private boolean canceled;

	public AddModeDialog(View view)
	{
		super(view, jEdit.getProperty("options.editing.addMode.dialog.title"), true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
		setContentPane(content);

		// main content
		AbstractOptionPane mainContent = new AbstractOptionPane("addmode");
		mainContent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		modeName = new JTextField(16);
		mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.modeName"), modeName);

		modeFile = new JTextField();
		JButton browse = new JButton("...");
		browse.addActionListener(e -> browse());
		JPanel browsePanel = new JPanel(new BorderLayout());
		browsePanel.add(modeFile, BorderLayout.CENTER);
		browsePanel.add(browse, BorderLayout.EAST);
		mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.modeFile"), browsePanel);

		filenameGlob = new JTextField(16);
		mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.filenameGlob"), filenameGlob);
		firstLineGlob = new JTextField();
		mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.firstLineGlob"), firstLineGlob);

		content.add(mainContent);

		// buttons
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(BorderFactory.createEmptyBorder(17, 0, 0, 6));

		JButton ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(e -> ok());
		getRootPane().setDefaultButton(ok);

		JButton cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(e -> cancel());
		GenericGUIUtilities.makeSameSize(ok, cancel);

		buttons.add(Box.createGlue());
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(cancel);

		content.add(BorderLayout.SOUTH, buttons);

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	}

	public String getModeName()
	{
		return modeName.getText();
	}

	public String getModeFile()
	{
		return modeFile.getText();
	}

	public String getFilenameGlob()
	{
		return filenameGlob.getText();
	}

	public String getFirstLineGlob()
	{
		return firstLineGlob.getText();
	}

	public boolean isCanceled()
	{
		return canceled;
	}

	@Override
	public void ok()
	{
		// check values
		String modeName = getModeName();
		if (modeName == null || modeName.isEmpty())
		{
			JOptionPane.showMessageDialog(jEdit.getActiveView(), jEdit.getProperty("options.editing.addMode.dialog.Mode_name_may_not_be_empty.", "Mode name may not be empty."), jEdit.getProperty("options.editing.addMode.dialog.errorTitle", "Error"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		String modeFile = getModeFile();
		if (modeFile == null || modeFile.isEmpty())
		{
			JOptionPane.showMessageDialog(jEdit.getActiveView(), jEdit.getProperty("options.editing.addMode.dialog.Mode_file_may_not_be_empty.", "Mode file may not be empty."), jEdit.getProperty("options.editing.addMode.dialog.errorTitle", "Error"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		String filenameGlob = getFilenameGlob();
		String firstLineGlob = getFirstLineGlob();
		if ((filenameGlob == null || filenameGlob.isEmpty()) && (firstLineGlob == null || firstLineGlob.isEmpty()))
		{
			JOptionPane.showMessageDialog(jEdit.getActiveView(),jEdit.getProperty("options.editing.addMode.dialog.Either_file_name_glob_or_first_line_glob_or_both_must_be_filled_in.", "Either file name glob or first line glob or both must be filled in."), jEdit.getProperty("options.editing.addMode.dialog.errorTitle", "Error"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		canceled = false;
		dispose();
	}

	@Override
	public void cancel()
	{
		canceled = true;
		dispose();
	}

	private void browse()
	{
		View view = jEdit.getActiveView();
		String path = jEdit.getSettingsDirectory();
		int type = VFSBrowser.OPEN_DIALOG;
		boolean multiSelect = false;
		String[] filename = GUIUtilities.showVFSFileDialog(view, path, type, multiSelect);
		modeFile.setText(filename.length > 0 ? filename[0] : "");
	}
}
