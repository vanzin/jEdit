package org.gjt.sp.util;

import javax.swing.SwingWorker;

public abstract class SwingWorkerBase<T, V> extends SwingWorker<T, V> implements ProgressObserver {

	private String status;
	private Runnable awtTask = null;
	private long maxProgress = 100;
	
	public SwingWorkerBase()
	{
	}
	
	public SwingWorkerBase(Runnable awtTask)
	{
		this.awtTask = awtTask;
	}
	
	public void setMaximum(long maxProgress)
	{
		this.maxProgress = maxProgress;
	}
	
	public void setValue(long value)
	{
		setProgress((int) (((float)value / maxProgress) * 100));
	}
	
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
	
	public void done()
	{
		if (awtTask != null)
			awtTask.run();
	}
}
