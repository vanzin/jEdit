package org.gjt.sp.jedit.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionsDialog.OptionTreeModel;
import org.gjt.sp.jedit.gui.OptionsDialog.PaneNameRenderer;
import org.gjt.sp.util.Log;

/**
 * An option pane for displaying groups of options.
 * There is a lot of code here which was taken from OptionsDialog,
 * but this class is a component which can be embedded in other Dialogs.
 * It is intended to be added to an OptionDialog (note the missing "s" there).
 * 
 * 
 * @author ezust
 * 
 */

public class OptionGroupPane extends AbstractOptionPane implements TreeSelectionListener
{
	
	public OptionGroupPane(OptionGroup group)
	{
		super(group.getName());
		optionGroup = group;
		init();
	}

	// {{{ valueChanged() method
	public void valueChanged(TreeSelectionEvent evt)
	{
		TreePath path = evt.getPath();

		if (path == null)
			return;

		Object lastPathComponent = path.getLastPathComponent();
		if (!(lastPathComponent instanceof String || lastPathComponent instanceof OptionPane))
		{
			return;
		}

		Object[] nodes = path.getPath();

		StringBuffer buf = new StringBuffer();

		OptionPane optionPane = null;

		int lastIdx = nodes.length - 1;

		for (int i = paneTree.isRootVisible() ? 0 : 1; i <= lastIdx; i++)
		{
			String label;
			Object node = nodes[i];
			if (node instanceof OptionPane)
			{
				optionPane = (OptionPane) node;
				label = jEdit.getProperty("options." + optionPane.getName()
					+ ".label");
			}
			else if (node instanceof OptionGroup)
			{
				label = ((OptionGroup) node).getLabel();
			}
			else if (node instanceof String)
			{
				label = jEdit.getProperty("options." + node + ".label");
				optionPane = (OptionPane) deferredOptionPanes.get((String) node);
				if (optionPane == null)
				{
					String propName = "options." + node + ".code";
					String code = jEdit.getProperty(propName);
					if (code != null)
					{
						optionPane = (OptionPane) BeanShell.eval(jEdit
							.getActiveView(), BeanShell.getNameSpace(),
							code);

						if (optionPane != null)
						{
							deferredOptionPanes.put(node, optionPane);
						}
						else
							continue;
					}
					else
					{
						Log.log(Log.ERROR, this, propName + " not defined");
						continue;
					}
				}
			}
			else
			{
				continue;
			}

			buf.append(label);

			if (i != lastIdx)
				buf.append(": ");
		}

		if (optionPane == null)
			return;

		String title = jEdit.getProperty("options.title-template", new Object[] {
			jEdit.getProperty(this.getName() + ".title"), buf.toString() });

		try
		{
			optionPane.init();
		}
		catch (Throwable t)
		{
			Log.log(Log.ERROR, this, "Error initializing options:");
			Log.log(Log.ERROR, this, t);
		}

		if (currentPane != null)
			stage.remove(currentPane.getComponent());
		currentPane = optionPane;
		stage.add(BorderLayout.CENTER, currentPane.getComponent());
		stage.revalidate();
		stage.repaint();

		if (!isShowing())
			addNotify();

		currentPane = optionPane;
	} // }}}

	private boolean selectPane(OptionGroup node, String name)
	{
		return selectPane(node, name, new ArrayList());
	} // }}}

	// {{{ selectPane() method
	private boolean selectPane(OptionGroup node, String name, ArrayList path)
	{
		path.add(node);

		Enumeration e = node.getMembers();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof OptionGroup)
			{
				OptionGroup grp = (OptionGroup) obj;
				if (grp.getName().equals(name))
				{
					path.add(grp);
					path.add(grp.getMember(0));
					TreePath treePath = new TreePath(path.toArray());
					if (treePath != null) {
						paneTree.scrollPathToVisible(treePath);
						paneTree.setSelectionPath(treePath);
						return true;
					}
				}
				else if (selectPane((OptionGroup) obj, name, path))
					return true;
			}
			else if (obj instanceof OptionPane)
			{
				OptionPane pane = (OptionPane) obj;
				if (pane.getName().equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);
					return true;
				}
			}
			else if (obj instanceof String)
			{
				String pane = (String) obj;
				if (pane.equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);
					return true;
				}
			}
		}

		path.remove(node);

		return false;
	} // }}}

	protected void _init()
	{

		setLayout(new BorderLayout());
		deferredOptionPanes = new HashMap();
		optionTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) optionTreeModel.getRoot();
		rootGroup.addOptionGroup(optionGroup);
		paneTree = new JTree(optionTreeModel);
		paneTree.setMinimumSize(new Dimension(100, 0));
		paneTree.setVisibleRowCount(1);
		paneTree.setCellRenderer(new PaneNameRenderer());

		JPanel content = new JPanel(new BorderLayout(12, 12));
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		add(content, BorderLayout.CENTER);

		stage = new JPanel(new BorderLayout());

		// looks bad with the OS X L&F, apparently...
		if (!OperatingSystem.isMacOSLF())
			paneTree.putClientProperty("JTree.lineStyle", "Angled");

		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);

		JScrollPane scroller = new JScrollPane(paneTree,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroller, stage);
		content.add(splitter, BorderLayout.CENTER);

		// register the Options dialog as a TreeSelectionListener.
		// this is done before the initial selection to ensure that the
		// first selected OptionPane is displayed on startup.
		paneTree.getSelectionModel().addTreeSelectionListener(this);

		OptionGroup rootNode = (OptionGroup) paneTree.getModel().getRoot();
		for (int i = 0; i < rootNode.getMemberCount(); i++)
		{
			paneTree.expandPath(new TreePath(new Object[] { rootNode,
				rootNode.getMember(i) }));
		}

		// returns false if no such pane exists; calling with null
		// param selects first option pane found
		String name = optionGroup.getName();
		selectPane(rootNode, null);
/*		if ((defaultPaneName != null) && (!selectPane(rootNode, defaultPaneName)))
			selectPane(rootNode, null); */

		splitter.setDividerLocation(paneTree.getPreferredSize().width
			+ scroller.getVerticalScrollBar().getPreferredSize().width);

		String pane = jEdit.getProperty(name + ".last");
		selectPane(rootNode,pane);
		

		
		int dividerLocation = jEdit.getIntegerProperty(name + ".splitter", -1);
		if (dividerLocation != -1)
			splitter.setDividerLocation(dividerLocation);

	}

	protected void _save()
	{
		if(currentPane != null)
			jEdit.setProperty(getName() + ".last",currentPane.getName());

		save(optionGroup);
	}

	private void save(Object obj)
	{
		
		if (obj instanceof OptionGroup)
		{
			OptionGroup grp = (OptionGroup) obj;
			Enumeration members = grp.getMembers();
			while (members.hasMoreElements())
			{
				save(members.nextElement());
			}
		}
		else if (obj instanceof OptionPane)
		{
			try
			{
				((OptionPane) obj).save();
			}
			catch (Throwable t)
			{
				Log.log(Log.ERROR, this, "Error saving options:");
				Log.log(Log.ERROR, this, t);
			}
		}
		else if (obj instanceof String)
		{
			save(deferredOptionPanes.get(obj));
		}
	}
	// {{{ Members
	OptionGroup optionGroup;

	JSplitPane splitter;

	JTree paneTree;

	OptionPane currentPane;

	OptionTreeModel optionTreeModel;

	HashMap deferredOptionPanes;

	JPanel stage;
	// }}}

}
