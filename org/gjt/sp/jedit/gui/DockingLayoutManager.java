package org.gjt.sp.jedit.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.options.DockingOptionPane;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.helpers.DefaultHandler;

public class DockingLayoutManager implements EBComponent
{

	private static ActionSet actions;
	private static DockingLayoutManager instance;
	private Map<View, String> currentMode;
	
	private DockingLayoutManager()
	{
		currentMode = new HashMap<View, String>();
	}
	private static void save(View view, File f)
	{
		String lineSep = System.getProperty("line.separator");
		SettingsXML xml = new SettingsXML(f);
		SettingsXML.Saver out = null;
		try {
			out = xml.openSaver();
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + lineSep);
			out.write("<perspective>" + lineSep);
			DockingLayout docking = view.getViewConfig().docking; 
			if (docking != null)
				docking.savePerspective(f, out, lineSep);
			out.write("</perspective>" + lineSep);
			out.finish();
			addAction(f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveAs(View view)
	{
		JFileChooser fc = new JFileChooser(getPerspectiveDirectory());
		if (fc.showSaveDialog(view) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();
		if (f == null)
			return;
		save(view, f);
	}
	
	private static void load(View view, File f)
	{
		DockingLayout docking = View.getDockingFrameworkProvider().createDockingLayout();
		DefaultHandler handler = docking.getPerspectiveHandler();
		if (handler == null)
			return;
		try {
			XMLUtilities.parseXML(new FileInputStream(f), handler);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		view.getDockableWindowManager().setDockingLayout(docking);
	}
	
	public static void load(View view)
	{
		JFileChooser fc = new JFileChooser(getPerspectiveDirectory());
		if (fc.showOpenDialog(view) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();
		if ((f == null) || (! f.canRead()))
			return;
		load(view, f);
	}

	private static String getPerspectiveDirectory()
	{
		String dir = jEdit.getSettingsDirectory() + File.separator + "perspectives";
		File f = new File(dir);
		if (! f.exists())
			f.mkdir();
		return dir;
	}

	private static String[] getSavedPerspectiveFiles()
	{
		File dir = new File(getPerspectiveDirectory());
		if (! dir.canRead())
			return null;
		File[] files = dir.listFiles();
		String[] perspectives = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			perspectives[i] = files[i].getAbsolutePath();
		}
		return perspectives;
	}
	
	private static String getPerspectiveName(String perspectiveFile)
	{
		File f = new File(perspectiveFile);
		String name = f.getName();
		if (name.toLowerCase().endsWith(".xml"))
			name = name.substring(0, name.length() - 4);
		return name;
	}
	
	private static void addAction(String perspectiveFile)
	{
		String name = getPerspectiveName(perspectiveFile);
		if ((actions != null) && (! actions.contains(name)))
			actions.addAction(new LoadPerspectiveAction(name, perspectiveFile));
	}
	
	public static void init()
	{
		createActions();
		instance = new DockingLayoutManager();
		EditBus.addToBus(instance);
	}
	
	private static void createActions()
	{
		actions = new ActionSet("Docking Layouts");
		String[] perspectives = getSavedPerspectiveFiles();
		for (String perspective: perspectives)
			addAction(perspective);
		jEdit.addActionSet(actions);
		actions.initKeyBindings();
	}
	
	public static void removeActions()
	{
		jEdit.removeActionSet(actions);
	}

	private static class LoadPerspectiveAction extends EditAction
	{
		private static final String LOAD_PREFIX = "load-";

		public LoadPerspectiveAction(String name, String file)
		{
			super(LOAD_PREFIX + name, new String[] { file });
			jEdit.setTemporaryProperty(LOAD_PREFIX + name + ".label", LOAD_PREFIX + name);
		}
		
		@Override
		public void invoke(View view)
		{
			File f = new File((String) args[0]);
			if (! f.canRead())
			{
				JOptionPane.showMessageDialog(view,
					"The selected perspective file cannot be read.",
					"Load Perspective Error",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
			DockingLayoutManager.load(view, f);
		}
	}

	public void handleMessage(EBMessage message)
	{
		boolean autoLoadModeLayout = jEdit.getBooleanProperty(
			DockingOptionPane.AUTO_LOAD_MODE_LAYOUT_PROP, false);
		if (! autoLoadModeLayout)
			return;
		if (message instanceof ViewUpdate)
		{
			ViewUpdate vu = (ViewUpdate) message;
			if (vu.getWhat() == ViewUpdate.CLOSED)
			{
				View view = jEdit.getActiveView();
				String mode = currentMode.get(view);
				saveModeLayout(view, mode);
				return;
			}
		}
		// Check for a change in the edit mode
		View view = jEdit.getActiveView();
		if (view == null)
			return;
		String newMode = getCurrentEditMode(view);
		String mode = currentMode.get(view);
		boolean sameMode =
			(mode == null && newMode == null) ||
			(mode != null && mode.equals(newMode));
		if (! sameMode)
		{
			boolean autoSaveModeLayout = jEdit.getBooleanProperty(
				DockingOptionPane.AUTO_SAVE_MODE_LAYOUT_PROP, false);
			if (autoSaveModeLayout)
				saveModeLayout(view, mode);
			currentMode.put(view, newMode);
			loadModeLayout(view, newMode);
		}
	}
	
	private String getCurrentEditMode(View view) {
		Buffer buffer = view.getBuffer();
		if (buffer == null)
			return null;
		Mode bufferMode = buffer.getMode();
		if (bufferMode == null)
			return null;
		return bufferMode.getName();
	}

	private static final String GLOBAL_MODE = "DEFAULT";
	
	private void saveModeLayout(View view, String mode)
	{
		File f = new File(getModePerspective(mode));
		save(view, f);
	}
	
	private void loadModeLayout(View view, String mode)
	{
		File f = new File(getModePerspective(mode));
		if (! f.canRead())	// Try global default
			f = new File(getModePerspective(null));
		if (! f.canRead())
			return;
		load(view, f);
	}

	public static void loadCurrentModeLayout(View view)
	{
		if (view == null)
			return;
		String mode = instance.getCurrentEditMode(view);
		instance.loadModeLayout(view, mode);
	}
	
	public static void saveCurrentModeLayout(View view)
	{
		if (view == null)
			return;
		String mode = instance.getCurrentEditMode(view);
		instance.saveModeLayout(view, mode);
	}
	
	private String getModePerspective(String mode)
	{
		if (mode == null)
			mode = GLOBAL_MODE;
		return getPerspectiveDirectory() + File.separator + "mode-" + mode + ".xml";
	}
}
