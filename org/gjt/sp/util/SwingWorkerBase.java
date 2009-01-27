package org.gjt.sp.util;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public abstract class SwingWorkerBase<T, V> extends SwingWorker<T, V> {

	private String status;
	private Runnable awtTask;
	
	public void setStatus(String status)
	{
		this.status = status;
	}
	
	public String getStatus()
	{
		return status;
	}
	
	public void setAbortable(boolean abortable)
	{
	}
	
	public void setAwtTask(Runnable awtTask)
	{
		synchronized (this)
		{
			if (isDone())
			{
				if (SwingUtilities.isEventDispatchThread())
					awtTask.run();
				else
					SwingUtilities.invokeLater(awtTask);
			}
			else
				this.awtTask = awtTask;
		}
	}
	
	public void done()
	{
		synchronized (this)
		{
			if (awtTask != null)
				awtTask.run();
		}
	}
}
