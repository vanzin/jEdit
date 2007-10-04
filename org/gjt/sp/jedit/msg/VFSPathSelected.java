package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.View;

/** Message sent when a file system tree node,
 * or a ProjectViewer tree node, is selected.
 * @since jEdit 4.3pre11
 */
public class VFSPathSelected extends EBMessage
{
	/**
	 * @param source the View that is considered the "source" of this event
	 * @param path The selected path.
	 */
	public VFSPathSelected(View source, String path)
	{
		super(source);
		this.path = path;
	}

	public View getView()
	{
		return (View) getSource();
	}

	/**
	 *  @return The selected URL (or file path).
	 */
	public String getPath()
	{
		return path;
	}

	private final String path;

}

