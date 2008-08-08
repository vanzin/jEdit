package org.gjt.sp.jedit.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.helpers.DefaultHandler;

public class DockingLayoutManager
{

	private static ActionSet actions;
	
	public static void saveAs(View view)
	{
		JFileChooser fc = new JFileChooser(getPerspectiveDirectory());
		if (fc.showSaveDialog(view) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();
		if (f == null)
			return;
		String lineSep = System.getProperty("line.separator");
		SettingsXML xml = new SettingsXML(f.getParent(), f.getName());
		SettingsXML.Saver out = null;
		try {
			out = xml.openSaver();
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + lineSep);
			out.write("<perspective>" + lineSep);
			view.getViewConfig().docking.savePerspective(f, out, lineSep);
			out.write("</perspective>" + lineSep);
			out.finish();
			addAction(f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void load(View view, File f)
	{
		DockingLayout docking = View.getDockingFrameworkProvider().createDockingLayout();
		DefaultHandler handler = docking.getPerspectiveHandler();
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
	
	public static void createActions()
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
}
