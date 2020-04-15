package org.gjt.sp.jedit.help;

import java.awt.Component;
import java.beans.PropertyChangeListener;

/**
 * Interface supported by all HelpViewer classes.
 * Currently used by @ref infoviewer.InfoViewerPlugin and @ref HelpViewer 
 * 
 * @since Jedit 4.2pre5
 * @version $Id$
 */
public interface HelpViewerInterface 
{
	/**
	 * 
	 * @param url
	 * @param addToHistory 
	 * @param scrollPos desired vertical scrollbar position after we go there
	 */
	void gotoURL(String url, boolean addToHistory, int scrollPos);
	
	String getBaseURL();
	
	void addPropertyChangeListener(PropertyChangeListener l);
	
	/**
	 * Called when the helpviewer is closed
	 */
	void dispose();

	/**
	 * @return the root pane of this help viewer
	 */
	Component getComponent();
	String getShortURL();
	
	void queueTOCReload();
	void setTitle(String newTitle);
}
