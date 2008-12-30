
package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.io.File;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.IOUtilities;
//}}}

/**
 * File's Properties dialog. This class create and show a window from the selected file or files.
 */
public class FilePropertiesDialog extends EnhancedDialog
{
	private View view;	
	private VFSBrowser browser;
	private VFSFile[] selectedFiles = null;
	private LocalFile local;
	
	//{{{ FilePropertiesDialog(View view, VFSBrowser browser) constructor
	/**
	 * The FilePropertiesDialog's constructor
	 * @param view The view
	 * @param browser The VFSBrowser
	 */	
	public FilePropertiesDialog(View view, VFSBrowser browser)
	{
		super(view,jEdit.getProperty("vfs.browser.properties.title"),true);
		GUIUtilities.loadGeometry(this,"propdialog");
		
		this.browser = browser;
		this.view = view;
			
		this.selectedFiles = browser.getSelectedFiles();
		this.local = (LocalFile)selectedFiles[0];
		createAndShowGUI();		
	} //}}}	
	
	//{{{ createAndShowGUI() method
	private void createAndShowGUI()
	{
		addComponentsToPane();
		this.pack();		

		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);		
		this.setFocusable(true);
		this.toFront();
		this.requestFocus();				
		this.setResizable(false);		
		this.setVisible(true);				
	} //}}}	
	
	private JButton okButton;
	private JButton cancelButton;
	private JTextField nameTextField;
	private JLabel infoIcon;
	private JCheckBox readable;
	private JCheckBox write;
		
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
    	int filesCounter=0, directoriesCounter=0;    	
    	
		JPanel northPanel = new JPanel(new BorderLayout());
		
		infoIcon = new JLabel();
		infoIcon.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		northPanel.add(BorderLayout.WEST, infoIcon);
						
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
    	long filesSize=0;
    	File ioFile;
		JPanel centerPanel = new JPanel(new BorderLayout());

		for(int i=0;i<selectedFiles.length;i++){
			if(selectedFiles[i].getType() == VFSFile.DIRECTORY){
				ioFile = new File(selectedFiles[i].getPath());
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
		propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+MiscUtilities.formatFileSize(filesSize)));
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
    	java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm"); 
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		
		JPanel propField = new JPanel();
		propField.setLayout(new GridLayout(4, 1));
		propField.add(new JLabel(jEdit.getProperty("fileprop.name")+": "+local.getName()));
		propField.add(new JLabel(jEdit.getProperty("fileprop.path")+": "+local.getPath()));
		propField.add(new JLabel(jEdit.getProperty("fileprop.lastmod")+": "+sdf.format((new java.util.Date(local.getModified())))));
		if(local.getType() == VFSFile.DIRECTORY)
		{
	    	 File ioFile = new File(local.getPath());
	    	 propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+MiscUtilities.formatFileSize(IOUtilities.fileLength(ioFile))));
	    }	
		else
		{
			propField.add(new JLabel(jEdit.getProperty("fileprop.size")+": "+MiscUtilities.formatFileSize(local.getLength())));
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
	public void ok()
	{		
		if(this.nameTextField != null)
		{
			browser.rename(browser.getSelectedFiles()[0].getPath(), nameTextField.getText());
		}		
			 
		GUIUtilities.saveGeometry(this,"propdialog");
		setVisible(false);
	} //}}}	
	
	//{{{ cancel() method
	public void cancel()
	{
		GUIUtilities.saveGeometry(this,"propdialog");
		setVisible(false);
	} //}}}	
	
	//{{{ ButtonActionHandler class
	class ButtonActionHandler implements ActionListener
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
}
