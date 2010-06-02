/*
 * ManagePanel.java - Manages plugins
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
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

//{{{ Imports
import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.net.URL;

import java.util.*;
import java.util.List;
import java.util.zip.ZipFile;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;

import javax.swing.border.EmptyBorder;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.browser.VFSFileChooserDialog;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.help.*;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.StandardUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//}}}

/**
 */
public class PackagePanel extends JPanel
{
	//{{{ Private members
	protected final PluginManager window;
	private final String extension = "jar"; // QUESTION: change this?
	private final String packageDir = jEdit.getSettingsDirectory()+
		File.separator+"packages"+File.separator;
	private PackageTableModel packageModel;
	private PackageInfoBox infoBox;
	private JTable table;
	private JComboBox remoteLocalBox = new JComboBox(new String[] { "Remote", "Local" });
	//}}}

	//{{{ PackagePanel constructor
	public PackagePanel(PluginManager window)
	{
		super(new BorderLayout(12,12));

		this.window = window;

		setBorder(new EmptyBorder(12,12,12,12));

		final JSplitPane split = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT, jEdit.getBooleanProperty("appearance.contnuousLayout"));

		split.setResizeWeight(0.75);
		// Set up the table
		table = new JTable(packageModel = new PackageTableModel());
		table.setShowGrid(false);
		table.setRowHeight(table.getRowHeight() + 2);
		table.getTableHeader().setReorderingAllowed(false);
		table.addMouseListener(new PackageTableMouseListener(table));
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.getViewport().setBackground(table.getBackground());
		split.setTopComponent(scrollpane);

		JScrollPane infoPane = new JScrollPane(
				infoBox = new PackageInfoBox());
		
		infoPane.setPreferredSize(new Dimension(500, 100));
		split.setBottomComponent(infoPane);
		
		// TableColumn col1 = table.getColumnModel().getColumn(0);
		// TableColumn col2 = table.getColumnModel().getColumn(1);
		/*
		col1.setPreferredWidth(30);
		col1.setMinWidth(30);
		col1.setMaxWidth(30);
		col1.setResizable(false);
		*/
		//infoBox.setText(jEdit.getProperty("plugin-manager.list-download"));
		
		// Combo box action listener
		remoteLocalBox.setEnabled(false);
		remoteLocalBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				packageModel.update();
			}
		});
		
		// Controls panel
		JPanel controlsPanel = new JPanel();
		
		add(BorderLayout.NORTH, remoteLocalBox);
		add(BorderLayout.CENTER, split);
	} //}}}
	
	//{{{ readPackage() method
	protected void readPackage(String path)
	{
		Log.log(Log.DEBUG,this,"Parsing "+path);
		try {
			/*
			VFS vfs = VFSManager.getVFSForPath(path);
			Object session = vfs.createVFSSession(path, PackagePanel.this);
			InputStream is = vfs._createInputStream(session, path, false, PackagePanel.this);
			*/
			ZipFile pkg = new ZipFile(path);
			InputStream is = pkg.getInputStream(pkg.getEntry("config.xml"));
			XMLUtilities.parseXML(is, new PackageParser(pkg));
		} catch (Exception e) {
			e.printStackTrace();
		}
	} //}}}
	
	//{{{ updateModel() method
	public void updateModel()
	{
		infoBox.setText(jEdit.getProperty("plugin-manager.list-download"));

		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				infoBox.setText(null);
				packageModel.update();
			}
		});
	} //}}}

	//{{{ PackageParser class
	private class PackageParser extends DefaultHandler
	{
		private Entry entry;
		private String context;
		public PackageParser(ZipFile pkg)
		{
			entry = new Entry(pkg);
			//entry.layouts = new ArrayList<String>();
			entry.plugins = new HashMap<String, String>();
			context = "";
		}
		
		public void characters(char[] ch, int start, int length)
		{
			String str = new String(ch, start, length).trim();
			if (str.length() == 0) return;
			
			if (context.equals("package:config:name")) entry.name = str;
			else if (context.equals("package:config:description")) entry.description = str;
			//else if (context.equals("package:docking:layout")) entry.layouts.add(str);
		}
		
		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
		{
			if (context.length() > 0) context += ":";
			context += localName;
			Log.log(Log.DEBUG,this,"uri="+uri+", localName="+localName+", qName="+qName);
			if (context.equals("package:plugin-list:plugin")) {
				String plugin_name = attrs.getValue("name");
				String plugin_jar = attrs.getValue("jar");
				if (plugin_name != null && plugin_jar != null) {
					entry.plugins.put(plugin_name, plugin_jar);
				}
			}
		}
		
		public void endElement(String uri, String localName, String qName)
		{
			context = context.substring(0, context.lastIndexOf(localName));
			if (context.endsWith(":")) context = context.substring(0, context.length()-1);
		}
		
		public void endDocument()
		{
			// Add the new entry
			packageModel.add(entry);
		}
	} //}}}

	//{{{ Entry class
	private class Entry
	{
		public String name;
		public String description;
		//public ArrayList<String> layouts;
		public HashMap<String, String> plugins; // maps a name to a jar file
		public ZipFile pkg;

		public Entry(ZipFile pkg) {
			this.pkg = pkg;
		}
	} //}}}
	
	//{{{ PackageInfoBox class
	private class PackageInfoBox extends JTextPane implements ListSelectionListener
	{
		private final String[] params;
		PackageInfoBox()
		{
			setBackground(jEdit.getColorProperty("view.bgColor"));
			setForeground(jEdit.getColorProperty("view.fgColor"));
			putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			setEditable(false);
			setEditorKit(new HTMLEditorKit());
//			setLineWrap(true);
//			setWrapStyleWord(true);
			params = new String[3];
			table.getSelectionModel().addListSelectionListener(this);
		}


		public void valueChanged(ListSelectionEvent e)
		{
			String text = "";
			if (table.getSelectedRowCount() == 1)
			{
				Entry entry = packageModel.getEntry(table.getSelectedRow());
				/*
				params[0] = entry.author;
				params[1] = entry.date;
				params[2] = entry.description;
				text = jEdit.getProperty("install-plugins.info", params);
				text = text.replace("\n", "<br>");
				*/
				text = "<html>" + entry.description + "</html>";
			}
			setText(text);
			setCaretPosition(0);
		}
	} //}}}

	//{{{ PackageTableModel class
	private class PackageTableModel extends AbstractTableModel
	{
		
		private List entries = new ArrayList();

		//{{{ getColumnCount() method
		public int getColumnCount()
		{
			return 1;
		} //}}}

		//{{{ getColumnName() method
		@Override
		public String getColumnName(int column)
		{
			switch (column) {
				case 0:
					return "Name";
				default:
					return "ERROR";
			}
		} //}}}

		//{{{ getRowCount() method
		public int getRowCount()
		{
			return entries.size();
		} //}}}

		//{{{ getValueAt() method
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Entry e = (Entry) entries.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return e.name;
				default:
					return null;
			}
		} //}}}
		
		//{{{ getEntry() method
		public Entry getEntry(int i) {
			return (Entry) entries.get(i);
		} //}}}
		
		//{{{ getColumnClass() method
		public Class getColumnClass(int columnIndex)
		{
			/*
			if (columnIndex == 0) return Boolean.class;
			else return Object.class;
			*/
			return Object.class;
		} //}}}

		//{{{ isCellEditable() method
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		} //}}}

		//{{{ sort() method
		public void sort(int type)
		{
			/*
			Collections.sort(entries);
			fireTableChanged(new TableModelEvent(this));
			restoreSelection(savedChecked,savedSelection);
			table.getTableHeader().repaint();
			*/
		}
		//}}}

		//{{{ clear() method
		public void clear()
		{
			entries = new ArrayList();
			fireTableChanged(new TableModelEvent(this));
		} //}}}
		
		//{{{ update() method
		public void update()
		{
			entries.clear();
			if (!remoteLocalBox.isEnabled())
				remoteLocalBox.setEnabled(true);
			if (remoteLocalBox.getSelectedItem().equals("Local")) {
				// Local update (~/.jedit/packages)
				infoBox.setText("<html>Click on a package to read its description, "+
						"or double-click it to get detailed information and the option to install it.</html>");
				try {
					File fdir = new File(packageDir);
					Log.log(Log.DEBUG,this,"Package dir: "+packageDir);
					String[] list = fdir.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith("."+extension);
						}
					});
					for (String pkg : list) {
						// pkg is the filename, parse its XML and store an Entry object
						readPackage(packageDir+pkg);
					}
				} catch (Exception e) {
					// Unable to update
				}
			} else {
				// Remote update
				infoBox.setText("<html>Remote plugin packages are not yet supported.</html>");
			}
			fireTableChanged(new TableModelEvent(this));
		} // }}}
		
		//{{{ add() method
		public void add(Entry e)
		{
			entries.add(e);
		} //}}}
		
	} //}}}
	
	//{{{ PackageTableMouseListener class
	private class PackageTableMouseListener extends MouseAdapter
	{
		private JTable table;
		public PackageTableMouseListener(JTable table) {
			this.table = table;
		}
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				int index = table.rowAtPoint(e.getPoint());
				Entry entry = ((PackageTableModel) table.getModel()).getEntry(index);
				new PackageDialog(entry).setVisible(true);
			}
		}
	} //}}}
	
	//{{{ PackageDialog class
	public class PackageDialog extends EnhancedDialog implements ActionListener
	{
		private Entry entry;
		private JButton install;
		private JButton cancel;
		private JCheckBox installLayout;
		private JCheckBox installShortcuts;
		private Properties shortcuts;
		private DockableWindowManager.DockingLayout docking;
		public PackageDialog(Entry entry) {
			super(window, "Plugin Package: "+entry.name, true);
			this.entry = entry;
			JPanel content = new JPanel(new BorderLayout());
			content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
			setPreferredSize(new Dimension(400, 500));

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
			header.add(Box.createHorizontalGlue());
			if (entry.description != null && entry.description.length() > 0)
				header.add(new JLabel("<html><b>"+entry.name+" - </b>"+entry.description+"</html>"));
			else
				header.add(new JLabel("<html><b>"+entry.name+"</b></html>"));
			header.add(Box.createHorizontalGlue());
			content.add(BorderLayout.NORTH, header);

			JPanel body = new JPanel();
			body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
			body.add(Box.createRigidArea(new Dimension(0, 20)));
			//body.add(Box.createVerticalGlue());
			JTextPane packageDesc = new JTextPane();
			packageDesc.setBackground(getBackground());
			packageDesc.setForeground(getForeground());
			packageDesc.setEditable(false);
			packageDesc.setEditorKit(new HTMLEditorKit());
			packageDesc.setFont(getFont());
			JScrollPane packageDescPane = new JScrollPane(packageDesc);
			packageDescPane.setPreferredSize(new Dimension(200, 200));
			body.add(packageDescPane);
			docking = jEdit.getActiveView().getViewConfig().docking;
			if (entry.pkg.getEntry("layout/"+docking.getName()+".xml") != null) {
				body.add(installLayout = new JCheckBox("Install docking layout", true));
			}
			if (entry.pkg.getEntry("shortcuts.props") != null) {
				body.add(installShortcuts = new JCheckBox("Set shortcuts", true));
				shortcuts = new Properties();
				try {
					shortcuts.load(entry.pkg.getInputStream(entry.pkg.getEntry("shortcuts.props")));
				} catch (Exception e) {
					installShortcuts.setSelected(false);
					installShortcuts.setEnabled(false);
					shortcuts = null;
				}
			}
			packageDesc.setText(getPackageDesc());
			//body.add(Box.createVerticalGlue());
			content.add(BorderLayout.CENTER, body);

			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
			buttons.add(Box.createHorizontalGlue());
			cancel = new JButton("Cancel");
			install = new JButton("Install");
			cancel.addActionListener(this);
			install.addActionListener(this);
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(10, 0)));
			buttons.add(install);
			content.add(BorderLayout.SOUTH, buttons);

			setContentPane(content);
			pack();
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setLocationRelativeTo(window);
		}
		
		private String getPackageDesc() {
			StringBuilder str = new StringBuilder();
			str.append("This package will attempt to install the following plugins, downloading them if necessary:<br /><ul>");
			for (String key : entry.plugins.keySet()) {
				str.append("<li>"+key+"</li>");
			}
			str.append("</ul>");
			if (shortcuts != null) {
				str.append("<br />Suggested shortcuts:<br /><ul>");
				for (String prop : shortcuts.stringPropertyNames()) {
					String val = shortcuts.getProperty(prop);
					str.append("<li>"+prop+" = "+val+"</li>");
				}
				str.append("</ul>");
			}
			return str.toString();
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == cancel) {
				dispose();
			} else if (e.getSource() == install) {
				// Load files locally
				String jeditJars = MiscUtilities.constructPath(jEdit.getJEditHome(), "jars");
				String userJars = null;
				String userSettingsDir = null;
				if ((userSettingsDir = jEdit.getSettingsDirectory()) != null)
					userJars = MiscUtilities.constructPath(userSettingsDir, "jars");

				// Probably use a hashset for this instead of an arraylist
				ArrayList<String> localInstalls = new ArrayList<String>();
				Roster roster = new Roster();

				for (String plugin : entry.plugins.keySet()) {
					String jar = entry.plugins.get(plugin);
					PluginJAR[] installedPlugins = jEdit.getPluginJARs();
					if (userJars != null) {
						String j = MiscUtilities.constructPath(userJars, jar);
						File f = new File(j);
						if (f.exists()) {
							try {
								boolean installed = false;
								for (int i = 0; i<installedPlugins.length; i++) {
									PluginJAR p = installedPlugins[i];
									if (p.getFile().equals(f)) {
										installed = true;
										break;
									}
								}
								if (!installed) {
									localInstalls.add(j);
								}
							} catch (NullPointerException ex) {
								// Print the path that caused the NPE in addition to the stack trace
								Log.log(Log.DEBUG, this, "Null pathname: "+j);
								ex.printStackTrace();
							} finally {
								continue;
							}
						}
					}
					// Wasn't found in settings directory, check jEdit's home
					String j = MiscUtilities.constructPath(jeditJars, jar);
					File f = new File(j);
					if (f.exists()) {
						try {
							boolean installed = false;
							for (int i = 0; i<installedPlugins.length; i++) {
								PluginJAR p = installedPlugins[i];
								if (p.getFile().equals(f)) {
									installed = true;
									break;
								}
							}
							if (!installed) {
								localInstalls.add(j);
							}
						} catch (NullPointerException ex) {
							// Print the path that caused the NPE in addition to the stack trace
							Log.log(Log.DEBUG, this, "Null pathname: "+j);
							ex.printStackTrace();
						} finally {
							continue;
						}
					}
					// Plugin was not found locally, attempt to install from internet
					// TODO: Add support for options such as choose where to save downloaded plugins
					boolean found = false;
					String installDirectory = MiscUtilities.constructPath(jEdit.getSettingsDirectory(), "jars");
					PluginList pluginList = window.getPluginList();
					for (int i = 0; i<pluginList.pluginSets.size(); i++) {
						PluginList.PluginSet set = pluginList.pluginSets.get(i);
						for(int k = 0; k < set.plugins.size(); k++) {
							PluginList.Plugin p = pluginList.pluginHash.get(set.plugins.get(k));
							if (jar.equals(p.jar)) {
								// Found it, so download it
								p.install(roster, installDirectory, false);
								found = true;
								break;
							}
						} if (found)
							break;
					}
				}
				int confirm = GUIUtilities.confirm(window, "plugin-manager.installPackage",
						new String[] { String.valueOf(roster.getOperationCount()) },
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (confirm == JOptionPane.OK_OPTION) {
					if (roster.getOperationCount() > 0) {
						new PluginManagerProgress(window, roster);
						roster.performOperationsInAWTThread(window);
					}
					for (String localJar : localInstalls) {
						PluginJAR.load(localJar, false);
					}
				}
				// Install layout, if necessary
				if (installLayout != null && installLayout.isSelected()) {
					// Copy the docking config file to its correct location, then load it
					String src = "layout/"+docking.getName()+".xml";
					String dest = docking.getLayoutFilename(entry.name,
							DockableWindowManager.DockingLayout.NO_VIEW_INDEX);
					BufferedInputStream is = null;
					BufferedOutputStream os = null;
					boolean failed = false;
					try {
						is = new BufferedInputStream(entry.pkg.getInputStream(
									entry.pkg.getEntry(src)));
						os = new BufferedOutputStream(new FileOutputStream(dest));
						IOUtilities.copyStream(4096, null, is, os, false);
						os.flush();
						DockableWindowManager.DockingLayout new_layout =
							jEdit.getActiveView().getDockingFrameworkProvider().createDockingLayout();
						if (new_layout.loadLayout(entry.name, DockableWindowManager.DockingLayout.NO_VIEW_INDEX)) {
							jEdit.getActiveView().getDockableWindowManager().setDockingLayout(new_layout);
						}
					} catch (Exception ex) {
						failed = true;
						ex.printStackTrace();
					} finally {
						IOUtilities.closeQuietly(is);
						IOUtilities.closeQuietly(os);
					}
				}
				// Install shortcuts if necessary
				if (shortcuts != null && installShortcuts != null && installShortcuts.isSelected()) {
					for (String prop : shortcuts.stringPropertyNames()) {
						jEdit.setProperty(prop+".shortcut", shortcuts.getProperty(prop));
					}
					jEdit.propertiesChanged();
				}
				dispose();
			}
		}
		
		public void ok() { dispose(); }
		public void cancel() { dispose(); }
	} //}}}
	
}
