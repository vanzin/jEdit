package org.gjt.sp.jedit.gui.notification;

import java.awt.Component;
import java.awt.Frame;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.ErrorEntry;
import org.gjt.sp.jedit.gui.ErrorListDialog;
import org.gjt.sp.jedit.io.VFSManager;

public class DefaultNotificationService extends NotificationService
{
	private static final DefaultNotificationService instance =
		new DefaultNotificationService();
	private static final Object errorLock = new Object();
	private static final Vector<ErrorEntry> errors;
	private static boolean error;

	static
	{
		errors = new Vector<ErrorEntry>();
	}

	public static DefaultNotificationService instance()
	{
		return instance;
	}

	@Override
	public void notifyError(Component comp, String path, String messageProp, Object[] args)
	{
		final Frame frame = JOptionPane.getFrameForComponent(comp);

		synchronized(errorLock)
		{
			error = true;

			errors.add(new ErrorEntry(
				path,messageProp,args));

			if(errors.size() == 1)
			{
				VFSManager.runInAWTThread(new Runnable()
				{
					public void run()
					{
						String caption = jEdit.getProperty(
							"ioerror.caption" + (errors.size() == 1
							? "-1" : ""),new Integer[] {
							Integer.valueOf(errors.size())});
						new ErrorListDialog(
							frame.isShowing()
							? frame
							: jEdit.getFirstView(),
							jEdit.getProperty("ioerror.title"),
							caption,errors,false);
						errors.clear();
						error = false;
					}
				});
			}
		}
	}

	@Override
	public boolean unnotifiedErrors()
	{
		return error;
	}

}
