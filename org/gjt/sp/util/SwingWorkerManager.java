package org.gjt.sp.util;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

public class SwingWorkerManager {
	private static HashMap<String, SwingWorkerManager> managers =
		new HashMap<String, SwingWorkerManager>();
	private String name;
	private ExecutorService pool;
	
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
		pool = Executors.newFixedThreadPool(numThreads);
		synchronized (managers)
		{
			managers.put(name, this);
		}
	}
	
	public void addWorker(SwingWorker worker)
	{
		pool.execute(worker);
	}
}
