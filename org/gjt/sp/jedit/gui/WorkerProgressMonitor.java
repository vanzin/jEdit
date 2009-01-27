package org.gjt.sp.jedit.gui;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.IOProgressMonitor.ThreadProgress.ActionHandler;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.util.SwingWorkerBase;

public class WorkerProgressMonitor extends JPanel implements ActionListener {

	private WorkerProgress[] workers;
	
	public WorkerProgressMonitor()
	{
		super(new BorderLayout());
		add(BorderLayout.NORTH,new Label("Progress monitor"));

		int n = VFSManager.getWorkerManager().getWorkerCount();
		workers = new WorkerProgress[n];

		Box box = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < n; i++)
		{
			if(i != 0)
				box.add(Box.createVerticalStrut(6));

			workers[i] = new WorkerProgress(i);
			box.add(workers[i]);
		}

		JPanel threadPanel = new JPanel(new BorderLayout());
		threadPanel.setBorder(new EmptyBorder(6,6,6,6));
		threadPanel.add(BorderLayout.NORTH,box);
		
		Timer timer = new Timer(500, this);
		timer.setRepeats(true);
		timer.start();
		add(BorderLayout.CENTER,new JScrollPane(threadPanel));
	} //}}}
	
	private class WorkerProgress extends JPanel
	{
		private JProgressBar progress;
		private JButton abort;
		int index;
		
		public WorkerProgress(int index)
		{
			super(new BorderLayout(12,12));
			this.index = index;
			Box box = new Box(BoxLayout.Y_AXIS);
			box.add(Box.createGlue());
			box.add(progress = new JProgressBar());
			progress.setStringPainted(true);
			box.add(Box.createGlue());
			add(BorderLayout.CENTER,box);

			abort = new JButton(jEdit.getProperty("io-progress-monitor.abort"));
			//abort.addActionListener(new ActionHandler());
			add(BorderLayout.EAST,abort);

			update();
		} //}}}

		//{{{ update() method
		public void update()
		{
			SwingWorkerBase worker = VFSManager.getWorkerManager().getWorker(index);
			if (worker != null)
			{
				abort.setEnabled(true);
				String status = worker.getStatus();
				if(status == null)
					status = "";
				progress.setString(status);
				progress.setValue(worker.getProgress());
			}
			else
			{
				abort.setEnabled(false);
				progress.setString(jEdit.getProperty("io-progress-monitor"
					+ ".idle"));
				progress.setIndeterminate(false);
				progress.setValue(0);
			}
		} //}}}

	}

	public void actionPerformed(ActionEvent e) {
		int n = VFSManager.getWorkerManager().getWorkerCount();
		for(int i = 0; i < n; i++)
			workers[i].update();
		
	}
}
