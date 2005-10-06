package org.gjt.sp.jedit.help;

import java.net.URL;

import javax.swing.JFrame;

import org.gjt.sp.util.Log;

public class HelpViewerFactory
{

	/**
	 * An alternate class to create instead of HelpViewer
	 */
	static protected Class helpClass = HelpViewerFrame.class;
	
	/**
	 * Change the class that is used to create HelpViewer instances.
	 * 
	 * @since Jedit 4.3pre3
	 * @param clazz The class to create  instead of
	 * HelpViewer - must be derived from HelpViewer to work.
	 */
	public static void setHelpViewerClass(Class clazz) 
	{
		helpClass = clazz;
	}
	
	/**
	 * 
	 * @param url A place to view help, specified as a resource
	 *     in a JAR file (usually).
	 * @return a new HelpViewer pointing at the URL.
	 */
	public static HelpViewer create(URL url) 
	{
		return create(url.getPath());
	}
	
	public static HelpViewer create(String url) 
	{
		HelpViewer hv = create();
		if (url != null) hv.gotoURL(url, true);
		return hv;
	}
	
	/**
	 * @return a new HelpViewer instance pointing at welcome.html
	 */
	public static HelpViewer create () 
	{
		HelpViewer hv = null;
		HelpViewerFrame frame = null;
		if (helpClass != null ) try 
		{
		     hv = (HelpViewer) helpClass.newInstance();
		     if (!(hv instanceof JFrame))
		     {
			     hv = new HelpViewerFrame(hv);
		     }
		}
		catch (InstantiationException ie) {
			Throwable t = ie.getCause();
			Log.log(Log.WARNING, t, "Can't create custom HelpClass");
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
			Log.log(Log.WARNING, e, "Unexpected Exception");
		}
		if (hv == null) hv = new HelpViewerFrame();
		hv.gotoURL("welcome.html", true);
		return hv;
	}

	
}
