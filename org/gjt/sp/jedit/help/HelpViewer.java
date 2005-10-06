package org.gjt.sp.jedit.help;

import java.awt.Component;
import java.beans.PropertyChangeListener;

import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Interface supported by all HelpViewer classes.  
 * 
 * To create an instance, @see HelpViewerFactory.create()
 * 
 * @author ezust
 *
 */
public interface HelpViewer 
{
	public void gotoURL(String url, boolean addToHistory);
	public String getShortURL();
	public String getBaseURL();
	public void setTitle(String newTitle);
	public Component getComponent();
	public void queueTOCReload();
	public void dispose();
	public void addPropertyChangeListener(PropertyChangeListener l);
}
