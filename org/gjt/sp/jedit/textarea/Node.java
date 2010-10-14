package org.gjt.sp.jedit.textarea;

import java.util.Vector;

public interface Node 
{
	public void addChild(Node node);
	public Vector  getChildren();
	public Node getParent();
}
