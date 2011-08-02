package org.gjt.sp.jedit.gui.notification;

import java.awt.Component;
import java.io.IOException;

import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.util.Log;

public abstract class NotificationService
{
	abstract public boolean unnotifiedErrors();

	//{{{ notifyError() method
	/**
	 * Reports an I/O error.
	 *
	 * @param comp The component
	 * @param path The path name that caused the error
	 * @param messageProp The error message property name
	 * @param args Positional parameters
	 * @since jEdit 4.0pre3
	 */
	abstract public void notifyError(Component comp, final String path,
		String messageProp, Object[] args);

	//{{{ errorOccurred() method
	/**
	 * Returns if there exist errors that were not notified yet.
	 */
	public static boolean errorOccurred()
	{
		return getService().unnotifiedErrors();
	}

	//{{{ error() method
	/**
	 * Handle an I/O error.
	 * @since jEdit 4.3pre3
	 */
	public static void ioerror(IOException e, String path, Component comp)
	{
		Log.log(Log.ERROR,VFSManager.class,e);
		error(comp, path, "ioerror", new String[] { e.toString() });
	}

	//{{{ error() method
	/**
	 * Reports an I/O error.
	 *
	 * @param comp The component
	 * @param path The path name that caused the error
	 * @param messageProp The error message property name
	 * @param args Positional parameters
	 * @since jEdit 4.0pre3
	 */
	public static void error(Component comp, final String path, String messageProp, Object[] args)
	{
		getService().notifyError(comp, path, messageProp, args);
	}

	private static NotificationService getService()
	{
		String name = jEdit.getProperty("notification.service");
		if (name != null)
		{
			NotificationService service = (NotificationService)
				ServiceManager.getService(NotificationService.class.getCanonicalName(), name);
			if (service != null)
				return service;
		}
		return DefaultNotificationService.instance();
	}
}
