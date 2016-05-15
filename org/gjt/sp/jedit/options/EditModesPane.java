
/*
 * EditModesPane.java - Mode-specific options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2002 Slava Pestov
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


// {{{ Imports
import java.awt.*;
import static java.awt.GridBagConstraints.BOTH;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.PingPongList;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.util.StandardUtilities;


// }}}

/**
 * Option pane to set available edit modes and to add modes from a file.
 * @author Dale Anson
 * @version $Id: EditModesPane.java 24012 2015-08-12 08:48:07Z kpouer $
 */
public class EditModesPane extends AbstractOptionPane

{

	// {{{ Instance variables
	private JComboBox<Mode> defaultMode;
	private PingPongList<Mode> pingPongList;
	private JTextField modeName;
	private JTextField modeFile;
	private JButton browse;
	private JTextField filenameGlob;
	private JTextField firstLineGlob;
	private JButton ok;
	private JButton deleteSelectedButton;


	// }}}
	// {{{ EditModesPane constructor
	public EditModesPane()
	{
		super( "editmodes" );
	}	//}}}


	// {{{ _init() method
	@Override
	protected void _init()
	{
		Mode[] modes = loadAllModes();
		Mode[] userSelectedModes = loadSelectedModes();
		defaultMode = new JComboBox<Mode>( userSelectedModes );
		defaultMode.setSelectedItem( jEdit.getMode( jEdit.getProperty( "buffer.defaultMode" ) ) );
		JPanel topPanel = new JPanel( new BorderLayout() );
		topPanel.add( new JLabel( jEdit.getProperty( "options.editing.defaultMode" ) ), BorderLayout.WEST );
		topPanel.add( defaultMode );
		addComponent( topPanel );
		addSeparator();
		List<Mode> availableModes = new ArrayList<Mode>();
		List<Mode> selectedModes = new ArrayList<Mode>();
		for ( Mode mode : modes ) {
			String modeName = mode.getName();
			boolean selected = !jEdit.getBooleanProperty( "mode.opt-out." + modeName, false );
			if ( selected )
			{
				selectedModes.add( mode );
			}
			else
			{
				availableModes.add( mode );
			}
		}
		pingPongList = new PingPongList<Mode>( availableModes, selectedModes );
		pingPongList.setLeftTitle( jEdit.getProperty( "options.editing.modes.available" ) );
		pingPongList.setRightTitle( jEdit.getProperty( "options.editing.modes.selected" ) );
		pingPongList.setLeftTooltip( jEdit.getProperty( "options.editing.modes.available.tooltip" ) );
		pingPongList.setRightTooltip( jEdit.getProperty( "options.editing.modes.selected.tooltip" ) );
		pingPongList.setRightCellRenderer(new MyCellRenderer());
		pingPongList.addRightListSelectionListener(new MyListSelectionListener());
		deleteSelectedButton = new JButton( jEdit.getProperty("options.editing.modes.deleteSelected", "Delete Selected") );	
		deleteSelectedButton.setEnabled(false);
		pingPongList.addButton( deleteSelectedButton );
		deleteSelectedButton.addActionListener( new ActionListener(){

				public void actionPerformed( ActionEvent ae )
				{
					List<Mode> modes = pingPongList.getRightSelectedValues();
					StringBuilder sb = new StringBuilder();
					sb.append(jEdit.getProperty("options.editing.modes.Delete_these_modes?", "Delete these modes?")).append('\n');
					for (Mode m : modes) 
					{
						if (m.isUserMode())
							sb.append(m.getName()).append('\n');	
					}
					int answer = JOptionPane.showConfirmDialog(jEdit.getActiveView(), sb.toString(), jEdit.getProperty("options.editing.deleteMode.dialog.title", "Confirm Mode Delete"), JOptionPane.WARNING_MESSAGE );
					if (answer == JOptionPane.YES_OPTION) {
						for (Mode m : modes)
						{
							if (m.isUserMode())
							{
								try
								{
									ModeProvider.instance.removeMode(m.getName());
								}
								catch (IOException e)
								{
									JOptionPane.showMessageDialog(jEdit.getActiveView(),
											jEdit.getProperty("options.editing.deleteMode.dialog.message1") + ' ' + m.getProperty("file") +
													'\n' + jEdit.getProperty("options.editing.deleteMode.dialog.message2") + ' ' + m.getName());
								}
							}
						}
						reloadLists(null);
					}
				}
			}
		);
		addComponent( pingPongList, BOTH );
		addSeparator();
		// add mode panel
		addComponent( new JLabel( jEdit.getProperty( "options.editing.addMode.dialog.title", "Add Mode" ) ) );
		JPanel content = new JPanel( new BorderLayout() );
		content.setBorder( BorderFactory.createEmptyBorder( 12, 12, 11, 11 ) );
		addComponent( content );
		// main content
		AbstractOptionPane mainContent = new AbstractOptionPane( "addmode" );
		mainContent.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0 ) );
		modeName = new JTextField( 36 );
		mainContent.addComponent( jEdit.getProperty( "options.editing.addMode.dialog.modeName" ), modeName );
		modeFile = new JTextField();
		browse = new JButton( "..." );
		browse.addActionListener( new ActionHandler() );
		JPanel browsePanel = new JPanel( new BorderLayout() );
		browsePanel.add( modeFile, BorderLayout.CENTER );
		browsePanel.add( browse, BorderLayout.EAST );
		mainContent.addComponent( jEdit.getProperty( "options.editing.addMode.dialog.modeFile" ), browsePanel );
		filenameGlob = new JTextField( 36 );
		mainContent.addComponent( jEdit.getProperty( "options.editing.addMode.dialog.filenameGlob" ), filenameGlob );
		firstLineGlob = new JTextField( 36 );
		mainContent.addComponent( jEdit.getProperty( "options.editing.addMode.dialog.firstLineGlob" ), firstLineGlob );
		content.add( mainContent );
		// buttons
		JPanel buttons = new JPanel();
		buttons.setLayout( new BoxLayout( buttons, BoxLayout.X_AXIS ) );
		buttons.setBorder( BorderFactory.createEmptyBorder( 17, 0, 0, 6 ) );
		ok = new JButton( jEdit.getProperty("options.editing.addMode", "Add Mode" ));
		ok.addActionListener( new ActionHandler() );
		buttons.add( Box.createGlue() );
		buttons.add( ok );
		content.add( BorderLayout.SOUTH, buttons );
		addComponent( content );
	}	//}}}


	private Mode[] loadSelectedModes()
	{
		Mode[] modes = jEdit.getModes();
		Arrays.sort( modes, new StandardUtilities.StringCompare<Mode>( true ) );
		return modes;
	}


	// returns all modes
	private Mode[] loadAllModes()
	{
		Mode[] modes = jEdit.getAllModes();
		Arrays.sort( modes, new StandardUtilities.StringCompare<Mode>( true ) );
		return modes;
	}


	// {{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setProperty( "buffer.defaultMode",
		( ( Mode )defaultMode.getSelectedItem() ).getName() );
		Iterator<Mode> available = pingPongList.getLeftDataIterator();
		while ( available.hasNext() ) {
			Mode mode = available.next();
			jEdit.setBooleanProperty( "mode.opt-out." + mode.getName(), true );
		}
		Iterator<Mode> selected = pingPongList.getRightDataIterator();
		while ( selected.hasNext() ) {
			Mode mode = selected.next();
			jEdit.unsetProperty( "mode.opt-out." + mode.getName() );
		}
	}	//}}}



	private class ActionHandler implements ActionListener
	{
		public void actionPerformed( ActionEvent evt )
		{
			Object source = evt.getSource();
			if ( source == browse )
			{
				View view = jEdit.getActiveView();
				String path = jEdit.getSettingsDirectory();
				int type = VFSBrowser.OPEN_DIALOG;
				boolean multiSelect = false;
				String[] filename = GUIUtilities.showVFSFileDialog( view, path, type, multiSelect );
				if ( filename != null && filename.length > 0 )
				{
					modeFile.setText( filename[0] );
				}
				else
				{
					modeFile.setText( "" );
				}
			}
			else
			if ( source == ok )
			{
				ok();
			}
		}
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


	public void ok()
	{
		// check values
		String modeName = getModeName();
		if ( modeName == null || modeName.isEmpty() )
		{
			JOptionPane.showMessageDialog( jEdit.getActiveView(), jEdit.getProperty( "options.editing.addMode.dialog.Mode_name_may_not_be_empty.", "Mode name may not be empty." ), jEdit.getProperty( "options.editing.addMode.dialog.errorTitle", "Error" ), JOptionPane.ERROR_MESSAGE );
			return;
		}

		String modeFile = getModeFile();
		if ( modeFile == null || modeFile.isEmpty() )
		{
			JOptionPane.showMessageDialog( jEdit.getActiveView(), jEdit.getProperty( "options.editing.addMode.dialog.Mode_file_may_not_be_empty.", "Mode file may not be empty." ), jEdit.getProperty( "options.editing.addMode.dialog.errorTitle", "Error" ), JOptionPane.ERROR_MESSAGE );
			return;
		}

		String filenameGlob = getFilenameGlob();
		String firstLineGlob = getFirstLineGlob();
		if ( ( filenameGlob == null || filenameGlob.isEmpty() ) && ( firstLineGlob == null || firstLineGlob.isEmpty() ) )
		{
			JOptionPane.showMessageDialog( jEdit.getActiveView(), jEdit.getProperty( "options.editing.addMode.dialog.Either_file_name_glob_or_first_line_glob_or_both_must_be_filled_in.", "Either file name glob or first line glob or both must be filled in." ), jEdit.getProperty( "options.editing.addMode.dialog.errorTitle", "Error" ), JOptionPane.ERROR_MESSAGE );
			return;
		}

		boolean exists = jEdit.getMode( modeName ) != null;
		if ( exists )
		{
			int answer = JOptionPane.showConfirmDialog( EditModesPane.this, jEdit.getProperty( "options.editing.addMode.dialog.warning.message" ), jEdit.getProperty( "options.editing.addMode.dialog.warning.title" ) + " " + modeName, JOptionPane.YES_NO_OPTION );
			if ( JOptionPane.YES_OPTION != answer )
			{
				return;
			}
		}

		// create mode and set properties from dialog values
		Mode newMode = new Mode( modeName );
		newMode.setProperty( "file", modeFile );
		newMode.setProperty( "filenameGlob", filenameGlob );
		newMode.setProperty( "firstlineGlob", firstLineGlob );
		File file = new File( modeFile );
		Path target = FileSystems.getDefault().getPath( jEdit.getSettingsDirectory(), "modes", file.getName() );
		try

		{
			ModeProvider.instance.addUserMode( newMode, target );
		}
		catch ( IOException e )

		{
			JOptionPane.showMessageDialog( jEdit.getActiveView(), jEdit.getProperty( "options.editing.addMode.dialog.warning.message1" ) + " " + modeFile + "\n--> " + target );
		}
		// refresh the mode dropdown so the new mode is in the list
		jEdit.reloadModes();
		// add the new mode to the selected list
		reloadLists( newMode );
	}


	private void reloadLists( Mode newMode )
	{
		// load the ping pong lists
		List<Mode> selectedModes = new ArrayList<Mode>();
		List<Mode> availableModes = new ArrayList<Mode>();
		Mode[] modes = loadAllModes();
		for ( Mode mode : modes ) {
			boolean selected = !jEdit.getBooleanProperty( "mode.opt-out." + mode.getName(), false );
			if ( selected )
			{
				selectedModes.add( mode );
			}
			else
			{
				availableModes.add( mode );
			}
		}
		pingPongList.setLeftData( availableModes );
		pingPongList.setRightData( selectedModes );
		if ( newMode != null )
		{
			pingPongList.setRightSelected( newMode );
		}

		// reload the default mode combo box
		defaultMode.setModel( new DefaultComboBoxModel<Mode>( loadSelectedModes() ) );
		defaultMode.setSelectedItem( jEdit.getMode( jEdit.getProperty( "buffer.defaultMode" ) ) );
	}



	// show user modes in green
	class MyCellRenderer extends JLabel implements ListCellRenderer<Mode>
	{

		public Component getListCellRendererComponent(
			JList<? extends Mode> list,	
			Mode value,	
			int index,	
			boolean isSelected,	
			boolean cellHasFocus )	
		{
			String s = value.toString();
			setText( s );
			setForeground(value.isUserMode() ? Color.GREEN : isSelected ? list.getSelectionForeground() : list.getForeground());
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			setOpaque( true );
			return this;
		}
	}
	
	// enable/disable the delete button based on selection
	class MyListSelectionListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent e) 
		{
			List<Mode> modes = pingPongList.getRightSelectedValues();
			boolean enabled = false;
			for (Mode m : modes)
			{
				if (m.isUserMode())
				{
					enabled = true;
					break;
				}
			}
			deleteSelectedButton.setEnabled(enabled);
		}
	}
}
