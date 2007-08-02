package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.io.VFSFile;

/** Message sent when a file system tree node,
 * or a ProjectViewer tree node, is selected. 
 * @since jEdit 4.3pre11
 */
public class VFSPathSelected extends EBMessage
{
	/** The underlying node in the tree model that was selected */
	protected Object treeNode;
	protected String path = "NULL";
	/**
	 * 
	 * @param source the View that is considered the "source" of this event
	 * @param node if an instance of VFSFile, will extract
	 * the path automatically
	 */
	public VFSPathSelected(View source, Object node) {
		super(source);
		this.treeNode=node;
		if (node instanceof VFSFile) {
			VFSFile f = (VFSFile) node;
			path = f.getPath();
		}
		
	}
	
	/** @return A URL or file path to the file represented by the
	 * selected node.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return The associated node object (may be VFSFile or VPTNode)
	 */
	public Object getNode() {
		return treeNode;
	}
}
