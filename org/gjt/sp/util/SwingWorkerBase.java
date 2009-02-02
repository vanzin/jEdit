package org.gjt.sp.util;

import javax.swing.SwingWorker;

public abstract class SwingWorkerBase extends SwingWorker<Void, String> implements ProgressObserver {

	private long max;
	
	abstract public void background();
	public void foreground() {}
	
	public void setStatus(String status) {
		publish(status);
	}
	
	public void setAbortable(boolean abortable) {
		
	}
	
	protected Void doInBackground() {
		background();
		return null;
	}
	
	public void done() {
		foreground();
	}

	public void setMaximum(long max) {
		this.max = max;
	}
	
	public void setValue(long value) {
		setProgress((int) (((float)value / max) * 100));
	}
}
