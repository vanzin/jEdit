package org.gjt.sp.jedit.notification;

import java.awt.Component;
import java.io.IOException;

public interface NotificationService
{
	//{{{ errorOccurred() method
	/**
	 * Returns if the last request caused an error.
	 */
	boolean errorOccurred();

	//{{{ error() method
	/**
	 * Handle an I/O error.
	 * @since jEdit 4.3pre3
	 */
	void error(IOException e, String path, Component comp);

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
	void error(Component comp, final String path, String messageProp, Object[] args);
}
