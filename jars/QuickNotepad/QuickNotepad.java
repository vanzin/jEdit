/*
 * QuickNotepad.java
 * part of the QuickNotepad plugin for the jEdit text editor
 * Copyright (C) 2001 John Gellene
 * jgellene@nyc.rr.com
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
 *
 * $Id$
 */

// from Java:
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

// from Swing:
import javax.swing.*;
import javax.swing.event.*;

// from jEdit:
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.util.Log;


public class QuickNotepad extends JPanel implements EBComponent, QuickNotepadActions, DefaultFocusComponent
{
	private String filename;
	private String defaultFilename;
	private View view;
	private boolean floating;

	private QuickNotepadTextArea textArea;
	private QuickNotepadToolPanel toolPanel;

	//
	// Constructor
	//

	public QuickNotepad(View view, String position)
	{
		super(new BorderLayout());

		this.view = view;
		this.floating  = position.equals(DockableWindowManager.FLOATING);

		if(jEdit.getSettingsDirectory() != null)
		{
			this.filename = jEdit.getProperty(
				QuickNotepadPlugin.OPTION_PREFIX + "filepath");
			if(this.filename == null || this.filename.length() == 0)
			{
				this.filename = new String(jEdit.getSettingsDirectory()
					+ File.separator + "qn.txt");
				jEdit.setProperty(
					QuickNotepadPlugin.OPTION_PREFIX + "filepath",
					this.filename);
			}
			this.defaultFilename = this.filename;
		}

		this.toolPanel = new QuickNotepadToolPanel(this);
		add(BorderLayout.NORTH, this.toolPanel);

		if(floating)
			this.setPreferredSize(new Dimension(500, 250));

		textArea = new QuickNotepadTextArea();
		textArea.setFont(QuickNotepadOptionPane.makeFont());
		textArea.addKeyListener(new KeyHandler());

		JScrollPane pane = new JScrollPane(textArea);
		add(BorderLayout.CENTER, pane);

		readFile();
	}

	public void focusOnDefaultComponent()
	{
		textArea.requestFocus();
	}

	//
	// Attribute methods
	//

	// for toolbar display
	public String getFilename()
	{
		return filename;
	}

	//
	// EBComponent implementation
	//

	public void handleMessage(EBMessage message)
	{
		if (message instanceof PropertiesChanged)
		{
			propertiesChanged();
		}
	}


	private void propertiesChanged()
	{
		String propertyFilename = jEdit.getProperty(
			QuickNotepadPlugin.OPTION_PREFIX + "filepath");
		if(!MiscUtilities.objectsEqual(defaultFilename,propertyFilename))
		{
			saveFile();
			toolPanel.propertiesChanged();
			defaultFilename = propertyFilename;
			filename = defaultFilename;
			readFile();
		}
		Font newFont = QuickNotepadOptionPane.makeFont();
		if(!newFont.equals(textArea.getFont()))
		{
			textArea.setFont(newFont);
		}
	}

	// These JComponent methods provide the appropriate points
	// to subscribe and unsubscribe this object to the EditBus

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}


	public void removeNotify()
	{
		saveFile();
		super.removeNotify();
		EditBus.removeFromBus(this);
	}


	//
	// QuickNotepadActions implementation
	//

	public void saveFile()
	{
		if(filename == null || filename.length() == 0) return;
		try
		{
			FileWriter out = new FileWriter(filename);
			out.write(textArea.getText());
			out.close();
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, QuickNotepad.class,
				"Could not write notepad text to " + filename);
		}
	}

	public void chooseFile()
	{
		String[] paths = GUIUtilities.showVFSFileDialog(view,
			null,JFileChooser.OPEN_DIALOG,false);
		if(paths != null && !paths[0].equals(filename))
		{
			saveFile();
			filename = paths[0];
			toolPanel.propertiesChanged();
			readFile();
		}
	}


	public void copyToBuffer()
	{
		jEdit.newFile(view);
		view.getEditPane().getTextArea().setText(textArea.getText());
	}

	//
	// helper methods
	//

	private void readFile()
	{
		if(filename == null || filename.length() == 0) return;

		BufferedReader bf = null;
		try
		{
			bf = new BufferedReader(new FileReader(filename));
			StringBuffer sb = new StringBuffer(2048);
			String str;
			while((str = bf.readLine()) != null)
			{
				sb.append(str).append('\n');
			}
			bf.close();
			textArea.setText(sb.toString());
		}
		catch (FileNotFoundException fnf)
		{
			Log.log(Log.ERROR, QuickNotepad.class,
				"notepad file " + filename + " does not exist");
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, QuickNotepad.class,
				"could not read notepad file " + filename);
		}
	}

	//
	// Listener objects
	//

	// <Esc> closes a floating window
	private class KeyHandler extends KeyAdapter {
		public void keyPressed(KeyEvent evt) {
			if(QuickNotepad.this.floating &&
				evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
				evt.consume();
				DockableWindowManager wm =
					QuickNotepad.this.view.getDockableWindowManager();
				wm.removeDockableWindow(QuickNotepadPlugin.NAME);
			}
		}
	}

}

