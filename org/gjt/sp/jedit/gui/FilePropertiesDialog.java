/*
 * FilePropertiesDialog.java - A File property dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 VladimirR
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.io.File;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.FileVFS.LocalFile;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * File's Properties dialog. This class create and show a window from the selected file or files.
 */
public class FilePropertiesDialog extends EnhancedDialog
{
	private final VFSBrowser browser;
	private final VFSFile[] selectedFiles;
	private final LocalFile local;

	//{{{ FilePropertiesDialog(View view, VFSBrowser browser) constructor
	/**
	 * The FilePropertiesDialog's constructor
	 * @param view The view
	 * @param browser The VFSBrowser
	 */
	public FilePropertiesDialog(View view, VFSBrowser browser, VFSFile[] files)
	{
		super(view,jEdit.getProperty("vfs.browser.properties.title"),true);
		GUIUtilities.loadGeometry(this,"propdialog");

		this.browser = browser;

		if (files.length > 0)
			selectedFiles = files;
		else
			selectedFiles = browser.getSelectedFiles();
		local = (LocalFile) selectedFiles[0];
		createAndShowGUI();
	} //}}}

	//{{{ addComponentsToPane() method
	public void addComponentsToPane()
	{
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,5,0,5));
		setContentPane(content);

		if (selectedFiles.length == 1)
		{
			content.add(BorderLayout.NORTH, createNorthPanel());
			content.add(BorderLayout.CENTER, createCenterPanel());
			content.add(BorderLayout.SOUTH, createSouthPanel());
		}
		else if(selectedFiles.length > 1)
		{
			content.add(BorderLayout.NORTH, createNorthPanelAll());
			content.add(BorderLayout.CENTER, createCenterPanelAll());
			content.add(BorderLayout.SOUTH, createSouthPanelAll());
		}
	} //}}}

	//{{{createNorthPanelAll() method
	public JPanel createNorthPanelAll()
	{
		JPanel northPanel = new JPanel(new BorderLayout());

		infoIcon = new JLabel();
		infoIcon.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		northPanel.add(BorderLayout.WEST, infoIcon);

		int filesCounter = 0;
		int directoriesCounter = 0;
		for(int i=0;i<selectedFiles.length;i++)
		{
			if(selectedFiles[i].getType() == VFSFile.DIRECTORY)
			{
				directoriesCounter++;
			}
			else if(selectedFiles[i].getType() == VFSFile.FILE)
			{
				filesCounter++;
			}
		}
		JPanel nameField = new JPanel();
		nameField.add(new JLabel(jEdit.getProperty("fileprop.selectedFiles")+": "+filesCounter+", "+
							jEdit.getProperty("fileprop.selectedDirectories")+": "+directoriesCounter));

		northPanel.add(BorderLayout.CENTER, nameField);
		northPanel.add(BorderLayout.SOUTH, new JPanel());

		return northPanel;
	} //}}}

	//{{{createCenterPanelAll() method
	public JPanel createCenterPanelAll()
	{
		long filesSize = 0L;
		JPanel centerPanel = new JPanel(new BorderLayout());

		for (int i=0;i<selectedFiles.length;i++)
		{
			if(selectedFiles[i].getType() == VFSFile.DIRECTORY)
			{
				File ioFile = new File(selectedFiles[i].getPath());
				filesSize += IOUtilities.fileLength(ioFile);
			}
			else if(selectedFiles[i].getType() == VFSFile.FILE)
			{
				filesSize += selectedFiles[i].getLength();
			}
		}

		JPanel propField = new JPanel();
		propField.setLayout(new GridLayout(2, 1));
		String path = local.getPath();
		if(OperatingSystem.isWindows() || OperatingSystem.isWindows9x() || OperatingSystem.isWindowsNT())
		{
			path = path.substring(0, path.lastIndexOf(92)); // 92 = '\'
		}
		else
		{
			path = path.substring(0, path.lastIndexOf('/'));
		}
		propField.add(new JLabel(jEdit.getProperty("fileprop.path")+": "+path));
		propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+
			StandardUtilities.formatFileSize(filesSize)));
		Border etch = BorderFactory.createEtchedBorder();
		propField.setBorder(BorderFactory.createTitledBorder(etch, jEdit.getProperty("fileprop.properties")));
		centerPanel.add(BorderLayout.CENTER, propField);

		return centerPanel;
	} //}}}

	//{{{ createSouthPanelAll() method
	public JPanel createSouthPanelAll()
	{
		ButtonActionHandler actionHandler = new ButtonActionHandler();
		JPanel southPanel = new JPanel(new BorderLayout());

		JPanel buttonsField = new JPanel();
		okButton = new JButton(jEdit.getProperty("fileprop.okBtn"));
		buttonsField.add(okButton);
		okButton.addActionListener(actionHandler);
		cancelButton = new JButton(jEdit.getProperty("fileprop.cancelBtn"));
		buttonsField.add(cancelButton);
		cancelButton.addActionListener(actionHandler);

		southPanel.add(BorderLayout.EAST, buttonsField);

		return southPanel;
	} //}}}

	//{{{ createNorthPanel() method
	public JPanel createNorthPanel()
	{
		JPanel northPanel = new JPanel(new BorderLayout());

		infoIcon = new JLabel();
		infoIcon.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		northPanel.add(BorderLayout.WEST, infoIcon);

		JPanel nameField = new JPanel();
		nameField.add(new JLabel(jEdit.getProperty("fileprop.name")+": "));
		nameTextField = new JTextField(local.getName(), 20);
		nameField.add(nameTextField);
		northPanel.add(BorderLayout.CENTER, nameField);
		northPanel.add(BorderLayout.SOUTH, new JPanel());

		return northPanel;
	} //}}}

	//{{{ createCenterPanel() method
	public JPanel createCenterPanel()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");

		JPanel centerPanel = new JPanel(new BorderLayout());

		JPanel propField = new JPanel();
		propField.setLayout(new GridLayout(4, 1));
		propField.add(new JLabel(jEdit.getProperty("fileprop.name")+": "+local.getName()));
		propField.add(new JLabel(jEdit.getProperty("fileprop.path")+": "+local.getPath()));
		propField.add(new JLabel(jEdit.getProperty("fileprop.lastmod")+": "+sdf.format(new Date(local.getModified()))));
		if(local.getType() == VFSFile.DIRECTORY)
		{
			File ioFile = new File(local.getPath());
			propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+
				StandardUtilities.formatFileSize(IOUtilities.fileLength(ioFile))));
		}
		else
		{
			propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+
				StandardUtilities.formatFileSize(local.getLength())));
		}
		Border etch = BorderFactory.createEtchedBorder();
		propField.setBorder(BorderFactory.createTitledBorder(etch, jEdit.getProperty("fileprop.properties")));
		centerPanel.add(BorderLayout.CENTER, propField);

		JPanel attributeField = new JPanel();
		attributeField.setLayout(new GridLayout(1, 2));
		readable = new JCheckBox(jEdit.getProperty("fileprop.readable"));
		readable.setSelected(local.isReadable());
		readable.setEnabled(false);
		attributeField.add(readable);

		write = new JCheckBox(jEdit.getProperty("fileprop.writeable"));
		write.setSelected(local.isWriteable());
		write.setEnabled(false);
		attributeField.add(write);
		attributeField.setBorder(BorderFactory.createTitledBorder(etch, jEdit.getProperty("fileprop.attribute")));
		centerPanel.add(BorderLayout.SOUTH, attributeField);

		return centerPanel;
	} //}}}

	//{{{ createSouthPanel() method
	public JPanel createSouthPanel()
	{
		ButtonActionHandler actionHandler = new ButtonActionHandler();
		JPanel southPanel = new JPanel(new BorderLayout());

		JPanel buttonsField = new JPanel();
		okButton = new JButton(jEdit.getProperty("fileprop.okBtn"));
		buttonsField.add(okButton);
		okButton.addActionListener(actionHandler);
		cancelButton = new JButton(jEdit.getProperty("fileprop.cancelBtn"));
		buttonsField.add(cancelButton);
		cancelButton.addActionListener(actionHandler);

		southPanel.add(BorderLayout.EAST, buttonsField);

		return southPanel;
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		if(nameTextField != null)
		{
			browser.rename(browser.getSelectedFiles()[0].getPath(), nameTextField.getText());
		}

		GUIUtilities.saveGeometry(this,"propdialog");
		setVisible(false);
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		GUIUtilities.saveGeometry(this,"propdialog");
		setVisible(false);
	} //}}}

	//{{{ Private members
	private JButton okButton;
	private JButton cancelButton;
	private JTextField nameTextField;
	private JLabel infoIcon;
	private JCheckBox readable;
	private JCheckBox write;

	//{{{ createAndShowGUI() method
	private void createAndShowGUI()
	{
		addComponentsToPane();
		pack();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setFocusable(true);
		toFront();
		requestFocus();
		setResizable(false);
		setVisible(true);
	} //}}}

	//{{{ ButtonActionHandler class
	private class ButtonActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();


			if(source == okButton)
			{
				ok();
			}
			else if(source == cancelButton)
			{
				cancel();
			}
		}
	} //}}}
	//}}}
}
