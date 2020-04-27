/*
 * ManagePanel.java - Manages plugins
 * :tabSize=4:indentSize=4:noTabs=false:
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

import java.awt.event.*;

import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import javax.swing.*;

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
import org.gjt.sp.jedit.help.*;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.StandardUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//}}}

/**
 * The ManagePanel is the JPanel that shows the installed plugins.
 */
public class ManagePanel extends JPanel
{
	//{{{ Private members
	private final JCheckBox hideLibraries;
	private final JTable table;
	private final JScrollPane scrollpane;
	private final PluginDetailPanel pluginDetailPanel;
	private final PluginTableModel pluginModel;
	private final PluginManager window;
	private JPopupMenu popup;
	private Set<String> selectedPlugins;
	private Set<String> jarNames;
	//}}}

	//{{{ ManagePanel constructor
	public ManagePanel(PluginManager window)
	{
		super(new BorderLayout(12,12));

		this.window = window;

		setBorder(new EmptyBorder(12,12,12,12));

		Box topBox = new Box(BoxLayout.X_AXIS);
		topBox.add(hideLibraries = new HideLibrariesButton());
		add(BorderLayout.NORTH,topBox);

		/* Create the plugin table */
		table = new JTable(pluginModel = new PluginTableModel());
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setRowHeight(GenericGUIUtilities.defaultRowHeight() + 2);
		table.setPreferredScrollableViewportSize(new Dimension(500,300));
		table.setDefaultRenderer(Object.class, new TextRenderer(
			(DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)));
		table.addFocusListener(new TableFocusHandler());
		table.getSelectionModel().addListSelectionListener(new TableSelectionListener());
		InputMap tableInputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap tableActionMap = table.getActionMap();
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),"tabOutForward");
		tableActionMap.put("tabOutForward",new KeyboardAction(KeyboardCommand.TAB_OUT_FORWARD));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,InputEvent.SHIFT_DOWN_MASK),"tabOutBack");
		tableActionMap.put("tabOutBack",new KeyboardAction(KeyboardCommand.TAB_OUT_BACK));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),"editPlugin");
		tableActionMap.put("editPlugin",new KeyboardAction(KeyboardCommand.EDIT_PLUGIN));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"closePluginManager");
		tableActionMap.put("closePluginManager",new KeyboardAction(KeyboardCommand.CLOSE_PLUGIN_MANAGER));

		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);
		TableColumn col3 = table.getColumnModel().getColumn(2);
		TableColumn col4 = table.getColumnModel().getColumn(3);

		col1.setPreferredWidth(30);
		col1.setMinWidth(30);
		col1.setMaxWidth(30);
		col1.setResizable(false);

		col2.setPreferredWidth(300);
		col3.setPreferredWidth(100);
		col4.setPreferredWidth(100);

		JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setDefaultRenderer(new HeaderRenderer(
				(DefaultTableCellRenderer)header.getDefaultRenderer()));
		MouseListener mouseHandler = new HeaderMouseHandler();
		header.addMouseListener(mouseHandler);
		table.addMouseListener(mouseHandler);
		scrollpane = new JScrollPane(table);
		scrollpane.getViewport().setBackground(table.getBackground());
		pluginDetailPanel = new PluginDetailPanel();
		scrollpane.setPreferredSize(new Dimension(400,400));
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
			true, scrollpane, pluginDetailPanel);
		add(BorderLayout.CENTER, split);
		split.setResizeWeight(0.75);
		/* Create button panel */
		Box buttons = new Box(BoxLayout.X_AXIS);

		buttons.add(new RemoveButton());
		buttons.add(new SaveButton());
		buttons.add(new RestoreButton());
		buttons.add(new FindOrphan());
		buttons.add(Box.createGlue());
		buttons.add(new HelpButton());

		add(BorderLayout.SOUTH,buttons);

		pluginModel.update();
	} //}}}

	//{{{ update() method
	public void update()
	{
		pluginModel.update();
	} //}}}

	// {{{ class ManagePanelRestoreHandler
	/**
	 * For handling the XML parse events of a plugin set.
	 * Selects the same plugins that are in that set.
	 * @since jEdit 4.3pre10
	 */
	private class ManagePanelRestoreHandler extends DefaultHandler
	{
		ManagePanelRestoreHandler()
		{
			selectedPlugins = new HashSet<>();
			jarNames = new HashSet<>();
		}

		@Override
		public void startElement(String uri, String localName,
							String qName, Attributes attrs) throws SAXException
		{
			if (localName.equals("plugin"))
			{
				String jarName = attrs.getValue("jar");
				String name = attrs.getValue("name");
				selectedPlugins.add(name);
				jarNames.add(jarName);
			}
		}
	}//}}}

	//{{{ loadPluginSet() method
	boolean loadPluginSet(String path)
	{
		VFS vfs = VFSManager.getVFSForPath(path);
		Object session = vfs.createVFSSession(path, this);
		try
		{
			InputStream is = vfs._createInputStream(session, path, false, this);
			XMLUtilities.parseXML(is, new ManagePanelRestoreHandler());
			is.close();
			int rowCount = pluginModel.getRowCount();
			for (int i=0 ; i<rowCount ; i++)
			{
				Entry ent = pluginModel.getEntry(i);
				String name = ent.name;
				if (name != null)
				{
					pluginModel.setValueAt(selectedPlugins.contains(name), i, 0);
				}
				else
				{
					String jarPath = ent.jar;
					String jarName = jarPath.substring(1 + jarPath.lastIndexOf(File.separatorChar));
					try
					{
						pluginModel.setValueAt(jarNames.contains(jarName), i, 0);
					}
					catch (Exception e)
					{
						Log.log(Log.WARNING, this, "Exception thrown loading: " + jarName, e);
					}
				}
			}
		}
		catch (Exception e)
		{
			Log.log(Log.ERROR, this, "Loading Pluginset Error", e);
			return false;
		}
		pluginModel.update();
		return true;
	}//}}}

	//{{{ getDeclaredJars() method
	/**
	 * Returns a collection of declared jars in the plugin.
	 * If the plugin is loaded use {@link org.gjt.sp.jedit.PluginJAR#getRequiredJars()}
	 * instead
	 *
	 * @param jarPath the path to the jar of the plugin
	 * @return a collection containing jars path
	 * @throws IOException if jEdit cannot generate cache
	 * @since jEdit 4.3pre12
	 */
	private static Collection<String> getDeclaredJars(String jarPath) throws IOException
	{
		Collection<String> jarList = new ArrayList<>();
		PluginJAR pluginJAR = new PluginJAR(new File(jarPath));
		PluginJAR.PluginCacheEntry pluginCacheEntry = PluginJAR.getPluginCacheEntry(jarPath);
		if(pluginCacheEntry != null)
		{
			Properties cachedProperties = pluginCacheEntry.cachedProperties;

			String jars = cachedProperties.getProperty("plugin." + pluginCacheEntry.pluginClass + ".jars");

			if (jars != null)
			{
				Collection<String> jarsPaths = PluginJAR.parseJarsFilesString(pluginJAR.getPath(), jars);
				for(String _jarPath: jarsPaths)
				{
					if (new File(_jarPath).exists())
						jarList.add(_jarPath);
				}
			}
		}
		jarList.add(jarPath);
		return jarList;
	}//}}}

	//{{{ Inner classes

	//{{{ Entry class
	class Entry
	{
		static final String LOADED = "loaded";
		static final String NOT_LOADED = "not-loaded";
		/** Partially loaded, and marked as "error" due to unsatisfied depends. */
		static final String ERROR = "error";
		/** Not loaded, marked Unsupported in plugin manager. */
		static final String DISABLED = "disabled";

		final String status;
		/** The jar path. */
		final String jar;

		String clazz, name, version, author, docs;
		/** The description property of the plugin. */
		String description;
		/** The dependencies of the plugin. */
		Set<String> depends;
		
		EditPlugin plugin;
		/**
		 * The jars referenced in the props file of the plugin.
		 * plugin.clazz.jars property and
		 * plugin.clazz.files property
		 */
		final List<String> jars;

		/** The data size. */
		String dataSize;

		/**
		 * Constructor used for jars that aren't loaded.
		 *
		 * @param jar jar file name
		 */
		Entry(String jar)
		{
			jars = new LinkedList<>();
			this.jar = jar;
			jars.add(this.jar);
			if (jEdit.getBooleanProperty("plugin." + MiscUtilities.getFileName(jar) + ".disabled"))
				status = DISABLED;
			else status = NOT_LOADED;

			PluginJAR.PluginCacheEntry cacheEntry;
			try
			{
				cacheEntry = PluginJAR.getPluginCacheEntry(jar);
				if(cacheEntry != null)
				{
					clazz = cacheEntry.pluginClass;
					Properties props = cacheEntry.cachedProperties;
					name = props.getProperty("plugin."+clazz+".name");
					version = props.getProperty("plugin."+clazz+".version");
					author = props.getProperty("plugin."+clazz+".author");
					docs = props.getProperty("plugin."+clazz+".docs");
					description = props.getProperty("plugin."+clazz+".description");
				}
			}
			catch (IOException e)
			{
				Log.log(Log.WARNING, "Unable to load cache for "+jar, e);
			}
		}

		/**
		 * Constructor used for loaded jars.
		 *
		 * @param jar the pluginJar
		 */
		Entry(PluginJAR jar)
		{
			jars = new LinkedList<String>();
			this.jar = jar.getPath();
			jars.add(this.jar);

			plugin = jar.getPlugin();
			if(plugin != null)
			{
				status = plugin instanceof EditPlugin.Broken
					? ERROR : LOADED;
				clazz = plugin.getClassName();
				name = jEdit.getProperty("plugin."+clazz+".name");
				version = jEdit.getProperty("plugin."+clazz+".version");
				author = jEdit.getProperty("plugin."+clazz+".author");
				docs = jEdit.getProperty("plugin."+clazz+".docs");
				description = jEdit.getProperty("plugin."+clazz+".description");
				jars.addAll(jar.getJars());
				jars.addAll(jar.getFiles());
			}
			else
			{
				status = LOADED;
			}
		}
		
		/**
		 * @return A list of the names of the dependencies, e.g. ErrorList or ProjectViewer.
		 */
		public Set<String> getDependencies() 
		{
			if (plugin == null)
				return null;
			Set<String> depends = null;
			String cn = plugin.getClassName();
			Set<String> requiredJars = PluginJAR.getDependencies(cn);
			if (!requiredJars.isEmpty())
			{
				depends = new HashSet<>();
				for (String dep : requiredJars)
				{
					Entry e = pluginModel.getEntry(dep);
					if (e != null)
						depends.add(e.name);
				}
			}
			return depends;
		}
	} //}}}

	//{{{ PluginTableModel class
	private class PluginTableModel extends AbstractTableModel
	{
		private final List<Entry> entries;
		private int sortType = EntryCompare.NAME;
		private Map<String, Object> unloaded;
		private int sortDirection = 1;

		//{{{ Constructor
		PluginTableModel()
		{
			entries = new ArrayList<>();
		} //}}}

		//{{{ getColumnCount() method
		@Override
		public int getColumnCount()
		{
			return 5;
		} //}}}

		//{{{ getColumnClass() method
		@Override
		public Class getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return Boolean.class;
				default: return Object.class;
			}
		} //}}}

		//{{{ getColumnName() method
		@Override
		public String getColumnName(int column)
		{
			switch (column)
			{
				case 0:
					return " ";
				case 1:
					return jEdit.getProperty("manage-plugins.info.name");
				case 2:
					return jEdit.getProperty("manage-plugins.info.version");
				case 3:
					return jEdit.getProperty("manage-plugins.info.status");
				case 4:
					return jEdit.getProperty("manage-plugins.info.data");
				default:
					throw new Error("Column out of range");
			}
		} //}}}

		//{{{ getEntry() method
		public Entry getEntry(int rowIndex)
		{
			return entries.get(rowIndex);
		} //}}}

		//{{{ getEntry() method
		public Entry getEntry(String classname)
		{
			if (classname == null || classname.isEmpty())
				return null;
			for (Entry entry : entries)
			{
				if (classname.equals(entry.clazz))
					return entry;
			}
			return null;
		} //}}}

		//{{{ getRowCount() method
		@Override
		public int getRowCount()
		{
			return entries.size();
		} //}}}

		//{{{ getValueAt() method
		@Override
		public Object getValueAt(int rowIndex,int columnIndex)
		{
			Entry entry = entries.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return !entry.status.equals(Entry.NOT_LOADED) &&
						!entry.status.equals(Entry.DISABLED);
				case 1:
					if(entry.name == null)
					{
						return MiscUtilities.getFileName(entry.jar);
					}
					else
					{
						return entry.name;
					}
				case 2:
					return entry.version;
				case 3:
					return jEdit.getProperty("plugin-manager.status." + entry.status);
				case 4:
					if (entry.dataSize == null && entry.plugin != null)
					{
						File pluginDirectory = entry.plugin.getPluginHome();
						if (null == pluginDirectory)
						{
							return null;
						}
						if (pluginDirectory.exists())
						{
							entry.dataSize = StandardUtilities.formatFileSize(IOUtilities.fileLength(pluginDirectory));
						}
						else
						{
							if (jEdit.getBooleanProperty("plugin." + entry.clazz + ".usePluginHome"))
							{
								entry.dataSize = StandardUtilities.formatFileSize(0);
							}
							else
							{
								entry.dataSize = jEdit.getProperty("manage-plugins.data-size.unknown");
							}

						}
					}
					return entry.dataSize;
				default:
					throw new Error("Column out of range");
			}
		} //}}}

		//{{{ isCellEditable() method
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 0;
		} //}}}

		//{{{ setValueAt() method
		@Override
		public void setValueAt(final Object value, int rowIndex,
			final int columnIndex)
		{
			final Entry entry = entries.get(rowIndex);
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if(columnIndex == 0)
					{
						PluginJAR jar = jEdit.getPluginJAR(entry.jar);
						if(jar == null)
						{
							if(value.equals(Boolean.FALSE))
								return;

							PluginJAR load = PluginJAR.load(entry.jar, true);
							if (load == null)
							{
								GUIUtilities.error(ManagePanel.this, "plugin-load-error", null);
							}
						}
						else
						{
							if(value.equals(Boolean.TRUE))
								return;

							unloadPluginJARWithDialog(jar);
						}
					}

					update();
				}
			});
		} //}}}

		//{{{ sort() method
		public void sort(int type)
		{
			List<String> savedSelection = new ArrayList<>();
			saveSelection(savedSelection);

			if (sortType != type)
			{
				sortDirection = 1;
			}
			sortType = type;

			entries.sort(new EntryCompare(type, sortDirection));
			fireTableChanged(new TableModelEvent(this));
			restoreSelection(savedSelection);
			table.getTableHeader().repaint();
		}
		//}}}

		//{{{ update() method
		public void update()
		{
			List<String> savedSelection = new ArrayList<>();
			saveSelection(savedSelection);
			entries.clear();

			String systemJarDir = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");
			String userJarDir;
			String settingsDirectory = jEdit.getSettingsDirectory();
			if(settingsDirectory == null)
				userJarDir = null;
			else
			{
				userJarDir = MiscUtilities.constructPath(
					settingsDirectory,"jars");
			}

			PluginJAR[] plugins = jEdit.getPluginJARs();
			for(int i = 0; i < plugins.length; i++)
			{
				String path = plugins[i].getPath();
				if(path.startsWith(systemJarDir)
					|| (userJarDir != null
					&& path.startsWith(userJarDir)))
				{
					Entry e = new Entry(plugins[i]);
					if(!hideLibraries.isSelected()
						|| e.clazz != null)
					{
						entries.add(e);
					}
				}
			}

			String[] newPlugins = jEdit.getNotLoadedPluginJARs();
			for(int i = 0; i < newPlugins.length; i++)
			{
				Entry e = new Entry(newPlugins[i]);
				entries.add(e);
			}

			sort(sortType);
			restoreSelection(savedSelection);
		} //}}}

		//{{{ unloadPluginJARWithDialog() method
		// Perhaps this should also be moved to PluginJAR class?
		private void unloadPluginJARWithDialog(PluginJAR jar)
		{
			// unloaded = new HashSet<String>();
			unloaded = new ConcurrentHashMap<>();
			String[] dependents = jar.getAllDependentPlugins();
			if(dependents.length == 0)
			{
				unloadPluginJAR(jar);
			}
			else
			{
				List<String> closureSet = new LinkedList<>();
				dependents = jar.getDependentPlugins();
				PluginJAR.transitiveClosure(dependents, closureSet);
				List<String> listModel = new ArrayList<>(new HashSet<>(closureSet));	// remove dupes
				boolean confirm = true;
				if (!listModel.isEmpty())
				{
					// show confirmation dialog listing dependencies to be unloaded
					listModel.sort(new StandardUtilities.StringCompare<>(true));
					int button = GUIUtilities.listConfirm(window,"plugin-manager.dependency",
						new String[] { jar.getFile().getName() }, listModel.toArray());
					confirm = button == JOptionPane.YES_OPTION;
				}
				if (confirm)
				{
					String[] optionals = jar.getOptionallyDependentPlugins();
					unloadPluginJAR(jar);
					// reload the optionally dependent plugins since they can run 
					// without this plugin
					for (String opt : optionals) 
					{
						PluginJAR.load(opt, true);	
					}
				}
			}
		} //}}}

		//{{{ unloadPluginJAR() method
		private void unloadPluginJAR(PluginJAR jar)
		{
			String[] dependents = jar.getAllDependentPlugins();
			for (String dependent : dependents)
			{
				if (!unloaded.containsKey(dependent))
				{
					unloaded.put(dependent, Boolean.TRUE);
					PluginJAR _jar = jEdit.getPluginJAR(dependent);
					if(_jar != null)
						unloadPluginJAR(_jar);
				}
			}
			jEdit.removePluginJAR(jar,false);
			jEdit.setBooleanProperty("plugin-blacklist."+MiscUtilities.getFileName(jar.getPath()),true);
			jEdit.propertiesChanged();
		} //}}}

		//{{{ saveSelection() method
		/**
		 * Save the selection in the given list.
		 * The list will be filled with the jar names of the selected entries
		 *
		 * @param savedSelection the list where to save the selection
		 */
		public void saveSelection(List<String> savedSelection)
		{
			if (table != null)
			{
				int[] rows = table.getSelectedRows();
				for (int row : rows)
					savedSelection.add(entries.get(row).jar);
			}
		} //}}}

		//{{{ restoreSelection() method
		/**
		 * Restore the selection.
		 *
		 * @param savedSelection the selection list that contains the jar names of the selected items
		 */
		public void restoreSelection(List<String> savedSelection)
		{
			if (null != table)
			{
				table.setColumnSelectionInterval(0,0);
				if (!savedSelection.isEmpty())
				{
					int i = 0;
					int rowCount = getRowCount();
					for ( ; i<rowCount ; i++)
					{
						if (savedSelection.contains(entries.get(i).jar))
						{
							table.setRowSelectionInterval(i,i);
							break;
						}
					}
					ListSelectionModel lsm = table.getSelectionModel();
					for ( ; i<rowCount ; i++)
					{
						if (savedSelection.contains(entries.get(i).jar))
						{
							lsm.addSelectionInterval(i,i);
						}
					}
				}
				else
				{
					if (table.getRowCount() != 0)
						table.setRowSelectionInterval(0,0);
					JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
					scrollbar.setValue(scrollbar.getMinimum());
				}
			}
		} //}}}
	} //}}}

	//{{{ TextRenderer class
	private class TextRenderer extends DefaultTableCellRenderer
	{
		private final DefaultTableCellRenderer tcr;

		TextRenderer(DefaultTableCellRenderer tcr)
		{
			this.tcr = tcr;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			Entry entry = pluginModel.getEntry(row);
			if (entry.status.equals(Entry.ERROR) || entry.status.equals(Entry.DISABLED))
				tcr.setForeground(Color.red);
			else
				tcr.setForeground(UIManager.getColor("Table.foreground"));
			return tcr.getTableCellRendererComponent(table,value,isSelected,false,row,column);
		}
	} //}}}

	//{{{ HideLibrariesButton class
	private class HideLibrariesButton extends JCheckBox
	{
		HideLibrariesButton()
		{
			super(jEdit.getProperty("plugin-manager.hide-libraries"));
			setSelected(jEdit.getBooleanProperty(
				"plugin-manager.hide-libraries.toggle"));
			addActionListener(e ->
			{
				jEdit.setBooleanProperty("plugin-manager.hide-libraries.toggle", isSelected());
				ManagePanel.this.update();
			});
		}
	} //}}}

	//{{{ RestoreButton class
	/**
	 * Permits the user to restore the state of the ManagePanel
	 * based on a PluginSet.
	 *
	 * Selects all loaded plugins that appear in an .XML file, and deselects
	 * all others, and also sets the pluginset to that .XML file. Does not install any plugins
	 * that were not previously installed.
	 *
	 * @since jEdit 4.3pre10
	 * @author Alan Ezust
	 */
	private class RestoreButton extends RolloverButton implements ActionListener
	{
		RestoreButton()
		{
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty("manage-plugins.restore.icon")));
			addActionListener(this);
			setToolTipText("Choose a PluginSet, select/deselect plugins based on set.");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String path = jEdit.getProperty(PluginManager.PROPERTY_PLUGINSET,
				jEdit.getSettingsDirectory() + File.separator);
			String[] selectedFiles = GUIUtilities.showVFSFileDialog(window,
				jEdit.getActiveView(), path, VFSBrowser.OPEN_DIALOG, false);
			if (selectedFiles.length != 1)
				return;
			path = selectedFiles[0];
			boolean success = loadPluginSet(path);
			if (success)
			{
				jEdit.setProperty(PluginManager.PROPERTY_PLUGINSET, path);
				EditBus.send(new PropertiesChanged(PluginManager.getInstance()));
			}

		}
	}//}}}

	//{{{ SaveButton class
	/**
	 * Permits the user to save the state of the ManagePanel,
	 * which in this case, is nothing more than a list of
	 * all plugins currently loaded.
	 * @since jEdit 4.3pre10
	 * @author Alan Ezust
	 */
	private class SaveButton extends RolloverButton implements ActionListener
	{
		SaveButton()
		{
			setIcon(GUIUtilities.loadIcon(jEdit.getProperty("manage-plugins.save.icon")));
			setToolTipText("Save Currently Checked Plugins Set");
			addActionListener(this);
			setEnabled(true);
		}

		void saveState(String vfsURL, List<Entry> pluginList)
		{
			StringBuilder sb = new StringBuilder("<pluginset>\n ");

			for (Entry entry: pluginList)
			{
				String jarName = entry.jar.substring(1+entry.jar.lastIndexOf(File.separatorChar));
				sb.append("   <plugin name=\"").append(entry.name).append("\" jar=\"");
				sb.append(jarName).append("\" />\n ");
			}
			sb.append("</pluginset>\n");

			VFS vfs = VFSManager.getVFSForPath(vfsURL);
			Object session = vfs.createVFSSession(vfsURL, ManagePanel.this);
			try (OutputStream os = vfs._createOutputStream(session, vfsURL, ManagePanel.this);
			     Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)))
			{
				writer.write(sb.toString());
			}
			catch (Exception e)
			{
				Log.log(Log.ERROR, this, "Saving State Error", e);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String path = jEdit.getProperty("plugin-manager.pluginset.path", jEdit.getSettingsDirectory() + File.separator);
			VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(window, jEdit.getActiveView(),
				path, VFSBrowser.SAVE_DIALOG, false , true);
			String[] fileselections = fileChooser.getSelectedFiles();
			List<Entry> pluginSelections = new ArrayList<>();
			if (fileselections.length != 1)
				return;

			PluginJAR[] jars = jEdit.getPluginJARs();
			for (PluginJAR jar : jars)
			{
				if (jar.getPlugin() != null)
				{
					Entry entry = new Entry (jar);
					pluginSelections.add(entry);
				}
			}
			saveState(fileselections[0], pluginSelections);
			jEdit.setProperty("plugin-manager.pluginset.path", fileselections[0]);
			EditBus.send(new PropertiesChanged(PluginManager.getInstance()));
		}
	}//}}}

	//{{{ RemoveButton class
	/**
	 * The Remove button is the button pressed to remove the selected
	 * plugin.
	 */
	private class RemoveButton extends JButton implements ListSelectionListener, ActionListener
	{
		RemoveButton()
		{
			super(jEdit.getProperty("manage-plugins.remove"));
			table.getSelectionModel().addListSelectionListener(this);
			addActionListener(this);
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent evt)
		{
			int[] selected = table.getSelectedRows();

			Collection<String> listModel = new LinkedList<>();
			Roster roster = new Roster();
			Collection<String> jarsToRemove = new HashSet<>();
			// this one will contains the loaded jars to remove. They
			// are the only one we need to check to unload plugins
			// that depends on them
			Set<String> loadedJarsToRemove = new HashSet<>();
			for(int i = 0; i < selected.length; i++)
			{
				Entry entry = pluginModel.getEntry(selected[i]);
				if (entry.status.equals(Entry.NOT_LOADED) || entry.status.equals(Entry.DISABLED))
				{
					if (entry.jar != null)
					{
						try
						{
							Collection<String> jarList = getDeclaredJars(entry.jar);
							jarsToRemove.addAll(jarList);
						}
						catch (IOException e)
						{
							Log.log(Log.ERROR, this, e);
						}
					}
				}
				else
				{
					jarsToRemove.addAll(entry.jars);
					loadedJarsToRemove.addAll(entry.jars);
				}
				table.getSelectionModel().removeSelectionInterval(selected[i], selected[i]);
			}

			for (String jar : jarsToRemove)
			{
				if(new File(jar).exists())
				{
					listModel.add(jar);
					roster.addRemove(jar);
				}
			}

			Object[] sortedConfirm = listModel.toArray();
			Arrays.sort(sortedConfirm);

			int button = GUIUtilities.listConfirm(window,
				"plugin-manager.remove-confirm",
				null, sortedConfirm);
			if(button == JOptionPane.YES_OPTION)
			{

				List<String> closureSet = new ArrayList<>();
				PluginJAR.transitiveClosure(loadedJarsToRemove.toArray(StandardUtilities.EMPTY_STRING_ARRAY), closureSet);
				closureSet.removeAll(listModel);
				if (closureSet.isEmpty())
				{
					button = JOptionPane.YES_OPTION;
				}
				else
				{
					button = GUIUtilities.listConfirm(window,"plugin-manager.remove-dependencies",
						null, closureSet.toArray());
					closureSet.sort(new StandardUtilities.StringCompare<>(true));
				}
				if(button == JOptionPane.YES_OPTION)
				{
					for (String jarName:closureSet)
					{
						PluginJAR pluginJAR = jEdit.getPluginJAR(jarName);
						jEdit.removePluginJAR(pluginJAR, false);
					}
					roster.performOperationsInAWTThread(window);
					pluginModel.update();
					if (table.getRowCount() != 0)
					{
						table.setRowSelectionInterval(0,0);
					}
					table.setColumnSelectionInterval(0,0);
					JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
					scrollbar.setValue(scrollbar.getMinimum());
				}
			}
			PluginManager.getInstance().pluginRemoved();
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			if (table.getSelectedRowCount() == 0)
				setEnabled(false);
			else
				setEnabled(true);
		}
	} //}}}

	//{{{ FindOrphanActionListener class
	private class FindOrphan extends JButton implements ActionListener
	{
		private FindOrphan()
		{
			super(jEdit.getProperty("plugin-manager.findOrphan.label"));
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			PluginJAR[] pluginJARs = jEdit.getPluginJARs();
			Collection<String> neededJars = new HashSet<>();

			Map<String, String> jarlibs = new HashMap<>();
			for (PluginJAR pluginJAR : pluginJARs)
			{
				EditPlugin plugin = pluginJAR.getPlugin();
				if (plugin == null)
				{
					jarlibs.put(new File(pluginJAR.getPath()).getName(), pluginJAR.getPath());
				}
				else
				{
					Set<String> strings = plugin.getPluginJAR().getRequiredJars();
					for (String string : strings)
					{
						neededJars.add(new File(string).getName());
					}
				}
			}

			String[] notLoadedJars = jEdit.getNotLoadedPluginJARs();
			for (int i = 0; i < notLoadedJars.length; i++)
			{
				PluginJAR pluginJAR = new PluginJAR(new File(notLoadedJars[i]));
				PluginJAR.PluginCacheEntry pluginCacheEntry = PluginJAR.getPluginCache(pluginJAR);
				try
				{
					if (pluginCacheEntry == null)
					{
						pluginCacheEntry = pluginJAR.generateCache();
					}
					if(pluginCacheEntry == null)
					{
						// this happens when, for some reason, two versions
						// of a plugin are installed, e.g when XSLT.jar and
						// xslt.jar are both in $JEDIT_HOME/jars on Linux.
						Log.log(Log.WARNING, ManagePanel.class,
								"couldn't load plugin "+pluginJAR.getPath()
								+" (most likely other version exists)");
					}
					if (pluginCacheEntry == null || pluginCacheEntry.pluginClass == null)
					{
						// Not a plugin
						jarlibs.put(new File(notLoadedJars[i]).getName(), notLoadedJars[i]);
						continue;
					}


					Properties cachedProperties = pluginCacheEntry.cachedProperties;

					String jars = cachedProperties.getProperty("plugin." + pluginCacheEntry.pluginClass + ".jars");

					if (jars != null)
					{
						neededJars.addAll(PluginJAR.parseJarsFilesStringNames(jars));
					}
				}
				catch (IOException e1)
				{
					Log.log(Log.ERROR, this, e);
				}
			}

			List<String> removingJars = new ArrayList<>();
			Set<String> jarlibsKeys = jarlibs.keySet();
			for (String jar : jarlibsKeys)
			{
				if (!neededJars.contains(jar))
				{
					removingJars.add(jar);
					Log.log(Log.MESSAGE, this, "It seems that this jar do not belong to any plugin " +jar);
				}
			}
			if(removingJars.isEmpty())
			{
				GUIUtilities.message(ManagePanel.this, "plugin-manager.noOrphan", null);
				return;
			}

			String[] strings = removingJars.toArray(StandardUtilities.EMPTY_STRING_ARRAY);
			List<String> mustRemove = new ArrayList<>();
			int ret = GUIUtilities.listConfirm(ManagePanel.this,
							   "plugin-manager.findOrphan",
							   null,
							   strings,
							   mustRemove);
			if (ret != JOptionPane.OK_OPTION || mustRemove.isEmpty())
				return;

			Roster roster = new Roster();
			for (String entry : mustRemove)
				roster.addRemove(jarlibs.get(entry));

			roster.performOperationsInAWTThread(window);
			pluginModel.update();
			if (table.getRowCount() != 0)
			{
				table.setRowSelectionInterval(0,0);
			}
			table.setColumnSelectionInterval(0,0);
			JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
			scrollbar.setValue(scrollbar.getMinimum());
			table.repaint();
		}
	} //}}}

	//{{{ HelpButton class
	private class HelpButton extends JButton implements ListSelectionListener
	{
		private URL docURL;

		HelpButton()
		{
			super(jEdit.getProperty("manage-plugins.help"));
			table.getSelectionModel().addListSelectionListener(this);
			addActionListener(e -> new HelpViewer(docURL));
			setEnabled(false);
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			if (table.getSelectedRowCount() == 1)
			{
				try
				{
					Entry entry = pluginModel.getEntry(table.getSelectedRow());
					String label = entry.clazz;
					String docs = entry.docs;
					if (label != null)
					{
						EditPlugin plug = jEdit.getPlugin(label, false);
						PluginJAR jar = null;
						if (plug != null) jar = plug.getPluginJAR();
						if(jar != null && docs != null)
						{
							URL url = jar.getClassLoader().getResource(docs);
							if(url != null)
							{
								docURL = url;
								setEnabled(true);
								return;
							}
						}
					}
				}
				catch (Exception ex)
				{
					Log.log(Log.ERROR, this, "ManagePanel HelpButton Update", ex);
				}
			}
			setEnabled(false);
		}
	} //}}}

	//{{{ EntryCompare class
	private static class EntryCompare implements Comparator<Entry>
	{
		public static final int NAME = 1;
		public static final int VERSION = 2;
		public static final int STATUS = 3;
		public static final int DATA = 4;

		private final int type;
		private final int direction;

		EntryCompare(int type, int direction)
		{
			this.type = type;
			this.direction = direction;
		}

		@Override
		public int compare(Entry e1, Entry e2)
		{
			int result;
			switch(type)
			{
			case NAME:
				result = compareNames(e1,e2);
				break;
			case VERSION:
				result = StandardUtilities.compareStrings(e1.version, e2.version, true);
				break;
			case STATUS:
				result = e1.status.compareToIgnoreCase(e2.status);
				break;
			case DATA:
				result = StandardUtilities.compareStrings(e1.dataSize,e2.dataSize, false);
				break;
			default:
				throw new IllegalStateException("Invalid sort type "+type);
			}
			return result * direction;
		}

		private static int compareNames(Entry e1, Entry e2)
		{
			String s1;
			if(e1.name == null)
				s1 = MiscUtilities.getFileName(e1.jar);
			else
				s1 = e1.name;
			String s2;
			if(e2.name == null)
				s2 = MiscUtilities.getFileName(e2.jar);
			else
				s2 = e2.name;

			return s1.compareToIgnoreCase(s2);
		}

	} //}}}

	//{{{ HeaderMouseHandler class
	private class HeaderMouseHandler extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			if (evt.getSource() == table.getTableHeader())
			{
				int column = table.getTableHeader().columnAtPoint(evt.getPoint());
				pluginModel.sortDirection *= -1;
				pluginModel.sort(column);
			}
			else
			{
				if (GenericGUIUtilities.isPopupTrigger(evt))
				{
					int row = table.rowAtPoint(evt.getPoint());
					if (row != -1 &&
					    !table.isRowSelected(row))
					{
						table.setRowSelectionInterval(row,row);
					}
					if (popup == null)
					{
						popup = new JPopupMenu();
						JMenuItem item = GUIUtilities.loadMenuItem("plugin-manager.cleanup");
						item.addActionListener(new CleanupActionListener());
						popup.add(item);
					}
					GenericGUIUtilities.showPopupMenu(popup, table, evt.getX(), evt.getY());
				}
			}
		}

		//{{{ CleanupActionListener class
		private class CleanupActionListener implements ActionListener
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{

				int[] ints = table.getSelectedRows();
				List<String> list = new ArrayList<>(ints.length);
				List<Entry> entries = new ArrayList<>(ints.length);
				for (int i = 0; i < ints.length; i++)
				{
					Entry entry = pluginModel.getEntry(ints[i]);
					if (entry.plugin != null)
					{
						list.add(entry.name);
						entries.add(entry);
					}
				}

				String[] strings = list.toArray(StandardUtilities.EMPTY_STRING_ARRAY);
				int ret = GUIUtilities.listConfirm(ManagePanel.this,
								   "plugin-manager.cleanup",
								   null,
								   strings);
				if (ret != JOptionPane.OK_OPTION)
					return;

				for (int i = 0; i < entries.size(); i++)
				{
					Entry entry = entries.get(i);
					File path = entry.plugin.getPluginHome();
					Log.log(Log.NOTICE, this, "Removing data of plugin " + entry.name + " home="+path);
					FileVFS.recursiveDelete(path);
					entry.dataSize = null;
				}
				table.repaint();
			}
		} //}}}
	} //}}}

	//{{{ KeyboardAction class
	private class KeyboardAction extends AbstractAction
	{
		private final KeyboardCommand command;

		KeyboardAction(KeyboardCommand command)
		{
			this.command = command;
		}

		@Override
		public void actionPerformed(ActionEvent evt)
		{
			switch (command)
			{
			case TAB_OUT_FORWARD:
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				break;
			case TAB_OUT_BACK:
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				break;
			case EDIT_PLUGIN:
				int[] rows = table.getSelectedRows();
				for (int row : rows)
				{
					Object st = pluginModel.getValueAt(row, 0);
					pluginModel.setValueAt(
						st.equals(Boolean.FALSE), row, 0);
				}
				break;
			case CLOSE_PLUGIN_MANAGER:
				window.ok();
				break;
			default:
				throw new InternalError();
			}
		}
	} //}}}

	//{{{ TableFocusHandler class
	private class TableFocusHandler extends FocusAdapter
	{
		@Override
		public void focusGained(FocusEvent fe)
		{
			if (table.getSelectedRow() == -1)
			{
				table.setRowSelectionInterval(0,0);
				JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
				scrollbar.setValue(scrollbar.getMinimum());
			}
			if (table.getSelectedColumn() == -1)
			{
				table.setColumnSelectionInterval(0,0);
			}
		}
	} //}}}

	//{{{ HeaderRenderer
	private static class HeaderRenderer extends DefaultTableCellRenderer
	{
		private final DefaultTableCellRenderer tcr;

		HeaderRenderer(DefaultTableCellRenderer tcr)
		{
			this.tcr = tcr;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
							       boolean isSelected, boolean hasFocus,
							       int row, int column)
		{
			JLabel l = (JLabel)tcr.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			PluginTableModel model = (PluginTableModel) table.getModel();
			Icon icon = (column == model.sortType)
				? (model.sortDirection == 1) ? InstallPanel.ASC_ICON : InstallPanel.DESC_ICON
				: null;
			l.setIcon(icon);
			return l;
		}
	} //}}}

	//{{{ TableSelectionListener class
	private class TableSelectionListener implements ListSelectionListener
	{
		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			int row = table.getSelectedRow();
			if (row != -1)
			{
				Entry entry = pluginModel.getEntry(row);
				pluginDetailPanel.setPlugin(entry);
			}
		}
	} //}}}
	//}}}
}
