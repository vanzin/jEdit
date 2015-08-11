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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Add Mode dialog.
 * @author Dale Anson
 * 
 */
public class AddModeDialog extends EnhancedDialog
{
        private JTextField modeName;
        private JTextField modeFile;
        private JButton browse;
        private JTextField filenameGlob;
        private JTextField firstLineGlob;
		private final JButton ok;
		private final JButton cancel;
		private boolean canceled = false;

		public AddModeDialog(View view)
		{
				super(view, jEdit.getProperty("options.editing.addMode.dialog.title"), true);
				
				JPanel content = new JPanel(new BorderLayout());
				content.setBorder(new EmptyBorder(12,12,12,12));
				setContentPane(content);

				// main content
				AbstractOptionPane mainContent = new AbstractOptionPane("addmode");
				
				modeName = new JTextField();
				mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.modeName"), modeName);
				
				JLabel label = new JLabel(jEdit.getProperty("options.editing.addMode.dialog.modeFile"));
				mainContent.addComponent(label);
				modeFile = new JTextField();
				browse = new JButton(jEdit.getProperty("options.editing.addMode.dialog.browse"));
				browse.addActionListener(new ActionHandler());
				mainContent.addComponent(modeFile, browse);

				filenameGlob = new JTextField();
				mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.filenameGlob"), filenameGlob);
				firstLineGlob = new JTextField();
				mainContent.addComponent(jEdit.getProperty("options.editing.addMode.dialog.firstLineGlob"), firstLineGlob);
				
				getContentPane().add(mainContent);

				// buttons
				JPanel buttons = new JPanel();
				buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
				buttons.setBorder(new EmptyBorder(12,0,0,0));
				buttons.add(Box.createGlue());

				ok = new JButton(jEdit.getProperty("common.ok"));
				ok.addActionListener(new ActionHandler());
				getRootPane().setDefaultButton(ok);
				buttons.add(ok);

				buttons.add(Box.createHorizontalStrut(6));

				cancel = new JButton(jEdit.getProperty("common.cancel"));
				cancel.addActionListener(new ActionHandler());
				buttons.add(cancel);

				buttons.add(Box.createGlue());
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
			if (getModeName() == null) 
			{
				JOptionPane.showMessageDialog(jEdit.getActiveView(), "Mode name may not be empty.");
				return;
			}
			if (getModeFile() == null) 
			{
				JOptionPane.showMessageDialog(jEdit.getActiveView(), "Mode name may not be empty.");
				return;
			}
			if (getFilenameGlob() == null && getFirstLineGlob() == null) 
			{
				JOptionPane.showMessageDialog(jEdit.getActiveView(), "Either file name glob or first line glob or both must be filled in.");
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
		
	private class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == browse)
			{
				View view = (View)AddModeDialog.this.getParent();
				String path = jEdit.getSettingsDirectory();
				int type = VFSBrowser.OPEN_DIALOG;
				boolean multiSelect = false;
				String[] filename = GUIUtilities.showVFSFileDialog(view, path, type, multiSelect);
				if (filename.length > 0)
				{
					modeFile.setText(filename[0]);
				}
				else
				{
					modeFile.setText("");	
				}
			}
			else if (source == ok)
			{
				ok();	
			}
			else if (source == cancel)
			{
				cancel();
			}
		}
	} 
		
}
