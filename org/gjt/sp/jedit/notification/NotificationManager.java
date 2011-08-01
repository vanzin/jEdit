package org.gjt.sp.jedit.notification;

import java.awt.Component;
import java.io.IOException;

import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;

public class NotificationManager
{
	//{{{ errorOccurred() method
	/**
	 * Returns if the last request caused an error.
	 */
	public static boolean errorOccurred()
	{
		return getService().errorOccurred();
	} //}}}

	//{{{ error() method
	/**
	 * Handle an I/O error.
	 * @since jEdit 4.3pre3
	 */
	public static void error(IOException e, String path, Component comp)
	{
		getService().error(e, path, comp);
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
	public static void error(Component comp, final String path, String messageProp,
		Object[] args)
	{
		getService().error(comp, path, messageProp, args);
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
