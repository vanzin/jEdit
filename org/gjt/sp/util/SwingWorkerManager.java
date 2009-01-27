package org.gjt.sp.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker.StateValue;

public class SwingWorkerManager implements PropertyChangeListener {
	
	private static HashMap<String, SwingWorkerManager> managers =
		new HashMap<String, SwingWorkerManager>();
	private String name;
	private int numThreads;
	private ExecutorService pool;
	private SwingWorkerBase[] running;
	private int next;
	
	public static SwingWorkerManager get(String name)
	{
		if (name == null)
			return null;
		synchronized (managers)
		{
			return managers.get(name);
		}
	}
	
	public SwingWorkerManager(String name, int numThreads)
	{
		this.name = name;
		this.numThreads = numThreads;
		pool = Executors.newFixedThreadPool(numThreads);
		running = new SwingWorkerBase[numThreads];
		next = 0;
		synchronized (managers)
		{
			managers.put(name, this);
		}
	}
	
	public void addWorker(SwingWorkerBase worker)
	{
		worker.addPropertyChangeListener(this);
		pool.execute(worker);
	}
	
	public int getWorkerCount()
	{
		return numThreads;
	}
	
	public SwingWorkerBase getWorker(int index)
	{
		return running[index];
	}
	
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (! evt.getPropertyName().equals("state"))
			return;
		Object obj = evt.getSource();
		if (! (obj instanceof SwingWorkerBase))
			return;
		SwingWorkerBase worker = (SwingWorkerBase) obj;
		if (evt.getNewValue() == StateValue.STARTED)
		{
			running[next] = worker;
			next++;
		}
		else if (evt.getNewValue() == StateValue.DONE)
		{
			next = -1;
			for (int i = 0; i < numThreads; i++)
			{
				if (running[i] == worker)
					running[i] = null;
				if (running[i] == null && next < 0)
					next = i;
			}
		}
	}
}
