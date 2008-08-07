package org.gjt.sp.jedit.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.gjt.sp.jedit.SettingsXML;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager.DockingLayout;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.helpers.DefaultHandler;

public class PerspectiveManager
{

	private static PerspectiveManager instance;

	public static PerspectiveManager get()
	{
		if (instance == null)
			instance = new PerspectiveManager();
		return instance;
	}
	
	private PerspectiveManager()
	{
	}

	public void saveAs(View view)
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void load(View view)
	{
		JFileChooser fc = new JFileChooser(getPerspectiveDirectory());
		if (fc.showOpenDialog(view) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fc.getSelectedFile();
		if ((f == null) || (! f.canRead()))
			return;
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

	private String getPerspectiveDirectory()
	{
		String dir = jEdit.getSettingsDirectory() + File.separator + "perspectives";
		File f = new File(dir);
		if (! f.exists())
			f.mkdir();
		return dir;
	}
	
}
